package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.StackUtils;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [EN] GUI Menu for Crafting Job Requester & Ordering Terminal (MVN_REQUEST_TERMINAL).
 * Scans all connected Auto-Crafters on the network, calculates material requirements and
 * on-hand network stocks, and dispatches batch crafting orders on-demand.
 *
 * [ES] Menú GUI para la Terminal de Solicitud de Crafteos en Red.
 */
public class RequestTerminalMenu extends MenuHolder {

    public static final int PREV_PAGE_SLOT = 45;
    public static final int PAGE_INFO_SLOT = 47;
    public static final int DELIVERY_SLOT = 49;
    public static final int REFRESH_SLOT = 51;
    public static final int NEXT_PAGE_SLOT = 53;
    public static final int PAGE_SIZE = 45;

    private static final int[] BOTTOM_BG_SLOTS = {
            46, 48, 50, 52
    };

    public record IngredientNeed(ItemStack sample, int amount) {
    }

    public static final class CraftableOption {
        private final ItemStack output;
        private final String name;
        private final RecipeData blueprintData;
        private final Recipe vanillaRecipe;
        private final List<IngredientNeed> ingredients;
        private final long crafterPos;

        public CraftableOption(ItemStack output, String name, RecipeData blueprintData, Recipe vanillaRecipe,
                              List<IngredientNeed> ingredients, long crafterPos) {
            this.output = output;
            this.name = name;
            this.blueprintData = blueprintData;
            this.vanillaRecipe = vanillaRecipe;
            this.ingredients = ingredients;
            this.crafterPos = crafterPos;
        }
    }

    private final Network network;
    private final Block block;
    private int page = 0;
    private boolean deliverToInventory = true;
    private final List<CraftableOption> options = new ArrayList<>();

    public RequestTerminalMenu(MultiverseNets plugin, Player player, Network network, Block block) {
        super(plugin, player);
        this.network = network;
        this.block = block;
    }

    public void openMenu() {
        open(54, Component.text("Crafting Request Terminal", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of();
    }

    private void scanCraftables() {
        options.clear();
        network.forEach(DeviceType.MVN_CRAFTER, (pos, type) -> {
            int cx = PosUtil.unpackX(pos) >> 4;
            int cz = PosUtil.unpackZ(pos) >> 4;
            if (!network.world().isChunkLoaded(cx, cz)) {
                return;
            }
            Block crafterBlock = network.block(pos);
            if (crafterBlock == null) {
                return;
            }
            NodeBlob blob = NodeStore.get(crafterBlock);
            if (blob == null) {
                return;
            }

            // 1. Blueprints
            for (String b64 : blob.blueprintData) {
                RecipeData data = Blueprints.decode(b64);
                if (data == null || data.output == null) {
                    continue;
                }
                List<IngredientNeed> needs = new ArrayList<>();
                for (ItemStack in : data.inputs) {
                    if (in == null || in.getType().isAir()) {
                        continue;
                    }
                    boolean merged = false;
                    for (int i = 0; i < needs.size(); i++) {
                        IngredientNeed n = needs.get(i);
                        if (StackUtils.itemsMatch(n.sample(), in)) {
                            needs.set(i, new IngredientNeed(n.sample(), n.amount() + 1));
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        needs.add(new IngredientNeed(StackUtils.getAsQuantity(in, 1), 1));
                    }
                }
                String name = Blueprints.readableName(data.output);
                options.add(new CraftableOption(data.output.clone(), name, data, null, needs, pos));
            }

            // 2. Legacy / Vanilla registered recipes
            for (String key : blob.recipes) {
                Recipe rec = CraftingSupport.find(key);
                if (rec == null) {
                    continue;
                }
                var reqs = CraftingSupport.requirements(rec);
                List<IngredientNeed> needs = new ArrayList<>();
                for (var r : reqs) {
                    RecipeChoice choice = r.getKey();
                    ItemStack sample = null;
                    if (choice instanceof RecipeChoice.MaterialChoice mc && !mc.getChoices().isEmpty()) {
                        sample = new ItemStack(mc.getChoices().get(0));
                    } else if (choice instanceof RecipeChoice.ExactChoice ec && !ec.getChoices().isEmpty()) {
                        sample = ec.getChoices().get(0).clone();
                    }
                    if (sample != null) {
                        needs.add(new IngredientNeed(sample, r.getValue()));
                    }
                }
                String name = Blueprints.readableName(rec.getResult());
                options.add(new CraftableOption(rec.getResult().clone(), name, null, rec, needs, pos));
            }
        });
    }

    @Override
    protected void draw() {
        scanCraftables();

        int totalPages = Math.max(1, (int) Math.ceil((double) options.size() / PAGE_SIZE));
        if (page >= totalPages) {
            page = totalPages - 1;
        }

        // Draw items for current page
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx < options.size()) {
                CraftableOption opt = options.get(idx);
                inv.setItem(i, buildOptionIcon(opt));
            } else {
                inv.setItem(i, null);
            }
        }

        // Bottom row controls
        ItemStack bg = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int s : BOTTOM_BG_SLOTS) {
            inv.setItem(s, bg);
        }

        // Previous Page
        inv.setItem(PREV_PAGE_SLOT, button(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));

        // Page Info
        ItemStack pageItem = new ItemStack(Material.PAPER);
        var metaPage = pageItem.getItemMeta();
        metaPage.displayName(Component.text("Page " + (page + 1) + " / " + totalPages, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        metaPage.lore(List.of(
                Component.text(options.size() + " craftable items discovered on network.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        pageItem.setItemMeta(metaPage);
        inv.setItem(PAGE_INFO_SLOT, pageItem);

        // Delivery Destination Button
        ItemStack deliveryItem = new ItemStack(deliverToInventory ? Material.PLAYER_HEAD : Material.CHEST);
        var metaDel = deliveryItem.getItemMeta();
        metaDel.displayName(Component.text("Delivery: " + (deliverToInventory ? "Direct to Inventory" : "Deposit to Network"),
                deliverToInventory ? NamedTextColor.GREEN : NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        metaDel.lore(List.of(
                Component.text(deliverToInventory
                        ? "Items will be placed straight into your player inventory."
                        : "Crafted items will be deposited directly into network storage.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to toggle delivery destination", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        deliveryItem.setItemMeta(metaDel);
        inv.setItem(DELIVERY_SLOT, deliveryItem);

        // Refresh Button
        inv.setItem(REFRESH_SLOT, button(Material.SUNFLOWER, "Refresh List", NamedTextColor.YELLOW));

        // Next Page
        inv.setItem(NEXT_PAGE_SLOT, button(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
    }

    private ItemStack buildOptionIcon(CraftableOption opt) {
        ItemStack icon = opt.output.clone();
        var meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }

        lore.add(Component.empty());
        lore.add(Component.text("Crafter at: " + PosUtil.unpackX(opt.crafterPos) + ", "
                + PosUtil.unpackY(opt.crafterPos) + ", " + PosUtil.unpackZ(opt.crafterPos), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Required Ingredients:", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

        long maxBatches = Long.MAX_VALUE;
        for (IngredientNeed need : opt.ingredients) {
            long count = network.storage().count(item -> StackUtils.itemsMatch(item, need.sample()));
            long possible = need.amount() > 0 ? count / need.amount() : 0;
            if (possible < maxBatches) {
                maxBatches = possible;
            }
            String ingName = Blueprints.readableName(need.sample());
            NamedTextColor col = count >= need.amount() ? NamedTextColor.GREEN : NamedTextColor.RED;
            lore.add(Component.text(" • " + ingName + ": " + count + " / " + need.amount(), col)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (opt.ingredients.isEmpty()) {
            maxBatches = 0;
        }

        lore.add(Component.empty());
        lore.add(Component.text("Max Craftable Batches: " + (maxBatches == Long.MAX_VALUE ? 0 : maxBatches),
                maxBatches > 0 ? NamedTextColor.AQUA : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Left Click: Order 1", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift + Left Click: Order 10", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right Click: Order 64", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift + Right Click: Define Custom Amount (Chat)", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();

        if (raw == PREV_PAGE_SLOT) {
            if (page > 0) {
                page--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                refresh();
            }
            return;
        }

        if (raw == NEXT_PAGE_SLOT) {
            int totalPages = Math.max(1, (int) Math.ceil((double) options.size() / PAGE_SIZE));
            if (page < totalPages - 1) {
                page++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                refresh();
            }
            return;
        }

        if (raw == DELIVERY_SLOT) {
            deliverToInventory = !deliverToInventory;
            player.sendMessage(Text.msg("Delivery set to: " + (deliverToInventory ? "Inventory" : "Network Storage"), NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            refresh();
            return;
        }

        if (raw == REFRESH_SLOT) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            refresh();
            return;
        }

        if (raw >= 0 && raw < PAGE_SIZE) {
            int idx = page * PAGE_SIZE + raw;
            if (idx >= options.size()) {
                return;
            }
            CraftableOption opt = options.get(idx);
            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                player.closeInventory();
                ChatPrompts.ask(player, "Enter crafting quantity in chat (numbers only):", input -> {
                    try {
                        int qty = Integer.parseInt(input.trim());
                        if (qty <= 0) {
                            player.sendMessage(Text.msg("Invalid quantity! Must be greater than 0.", NamedTextColor.RED));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return;
                        }
                        int unitsPerCraft = opt.output.getAmount() > 0 ? opt.output.getAmount() : 1;
                        int batches = (int) Math.ceil((double) qty / unitsPerCraft);
                        executeOrder(opt, batches);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Text.msg("Invalid quantity! Crafting cancelled. Only numeric values are allowed.", NamedTextColor.RED));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                });
                return;
            }

            int batches = calculateOrderAmount(event.getClick(), opt);
            if (batches <= 0) {
                player.sendMessage(Text.msg("Cannot craft: missing ingredients in network.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            executeOrder(opt, batches);
            refresh();
        }
    }

    private int calculateOrderAmount(ClickType click, CraftableOption opt) {
        long maxBatches = Long.MAX_VALUE;
        for (IngredientNeed need : opt.ingredients) {
            long count = network.storage().count(item -> StackUtils.itemsMatch(item, need.sample()));
            long possible = need.amount() > 0 ? count / need.amount() : 0;
            if (possible < maxBatches) {
                maxBatches = possible;
            }
        }
        if (maxBatches <= 0 || maxBatches == Long.MAX_VALUE) {
            return 0;
        }

        int requested = switch (click) {
            case SHIFT_LEFT -> 10;
            case RIGHT -> 64;
            default -> 1;
        };

        return (int) Math.min(requested, maxBatches);
    }

    private void executeOrder(CraftableOption opt, int requestedBatches) {
        int crafted = 0;
        for (int i = 0; i < requestedBatches; i++) {
            boolean success;
            if (opt.blueprintData != null) {
                success = CraftingSupport.tryCraftBlueprint(network, opt.blueprintData);
            } else if (opt.vanillaRecipe != null) {
                success = CraftingSupport.tryCraftOnce(network, opt.vanillaRecipe);
            } else {
                break;
            }
            if (!success) {
                break;
            }
            crafted++;
        }

        if (crafted <= 0) {
            player.sendMessage(Text.msg("Crafting Job Failed: Insufficient raw materials or full storage.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int unitsPerCraft = opt.output.getAmount() > 0 ? opt.output.getAmount() : 1;
        int totalItemsCrafted = crafted * unitsPerCraft;

        if (deliverToInventory) {
            // Withdraw from network storage and put directly into player inventory
            int remainingToDeliver = totalItemsCrafted;
            while (remainingToDeliver > 0) {
                int batchTake = Math.min(remainingToDeliver, opt.output.getMaxStackSize());
                ItemStack pulled = network.storage().withdraw(item -> StackUtils.itemsMatch(item, opt.output), batchTake);
                if (pulled == null || pulled.getAmount() <= 0) {
                    break;
                }
                remainingToDeliver -= pulled.getAmount();
                giveOrDrop(pulled);
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        if (crafted == requestedBatches) {
            player.sendMessage(Text.msg("Crafting Job Complete: Ordered " + totalItemsCrafted + "x " + opt.name
                    + (deliverToInventory ? " (delivered to inventory)." : " (deposited in network)."), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("Crafting Job Partial: Ordered " + (requestedBatches * unitsPerCraft)
                    + "x " + opt.name + ", crafted " + totalItemsCrafted + " (materials exhausted).", NamedTextColor.YELLOW));
        }
    }

    private ItemStack button(Material mat, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
