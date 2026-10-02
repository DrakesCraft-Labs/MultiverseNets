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

import com.chagui68.multiversenets.net.NetworkStorage;

import java.util.ArrayList;
import java.util.HashSet;
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
        network.forEach(DeviceType.MVN_REQUEST_CRAFTER, (pos, type) -> scanCrafter(pos));
        network.forEach(DeviceType.MVN_SF_REQUEST_CRAFTER, (pos, type) -> scanCrafter(pos));
    }

    private void scanCrafter(long pos) {
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
                    if (StackUtils.itemsMatch(n.sample(), in, false)) {
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

    public record CraftingJobStep(CraftableOption option, int batches) {
    }

    private static final class SimulatedStock {
        private final List<StockEntry> entries = new ArrayList<>();

        static final class StockEntry {
            ItemStack sample;
            long amount;
            StockEntry(ItemStack sample, long amount) {
                this.sample = sample;
                this.amount = amount;
            }
        }

        SimulatedStock(List<NetworkStorage.View> initialViews) {
            if (initialViews != null) {
                for (NetworkStorage.View v : initialViews) {
                    if (v.sample() != null && v.amount() > 0) {
                        entries.add(new StockEntry(v.sample().clone(), v.amount()));
                    }
                }
            }
        }

        long available(ItemStack item) {
            long total = 0;
            for (StockEntry e : entries) {
                if (StackUtils.itemsMatch(e.sample, item, false)) {
                    total += e.amount;
                }
            }
            return total;
        }

        long take(ItemStack item, long needed) {
            long taken = 0;
            for (StockEntry e : entries) {
                if (StackUtils.itemsMatch(e.sample, item, false)) {
                    long toTake = Math.min(e.amount, needed - taken);
                    e.amount -= toTake;
                    taken += toTake;
                    if (taken >= needed) {
                        break;
                    }
                }
            }
            return taken;
        }

        void add(ItemStack item, long count) {
            if (item == null || count <= 0) return;
            for (StockEntry e : entries) {
                if (StackUtils.itemsMatch(e.sample, item, false)) {
                    e.amount += count;
                    return;
                }
            }
            entries.add(new StockEntry(StackUtils.getAsQuantity(item, 1), count));
        }
    }

    private CraftableOption findProducer(ItemStack needed, Set<CraftableOption> branch, SimulatedStock stock) {
        CraftableOption bestCandidate = null;
        for (CraftableOption opt : options) {
            if (branch.contains(opt)) {
                continue;
            }
            if (StackUtils.itemsMatch(opt.output, needed, false)) {
                if (stock != null) {
                    boolean allInStock = true;
                    for (IngredientNeed in : opt.ingredients) {
                        if (stock.available(in.sample()) < in.amount()) {
                            allInStock = false;
                            break;
                        }
                    }
                    if (allInStock) {
                        return opt;
                    }
                }
                if (bestCandidate == null) {
                    bestCandidate = opt;
                }
            }
        }
        return bestCandidate;
    }

    private List<CraftingJobStep> planCraft(CraftableOption target, int batches, SimulatedStock stock, Set<CraftableOption> branch, int depth) {
        if (batches <= 0) {
            return List.of();
        }
        if (depth > 12 || branch.contains(target)) {
            return null;
        }
        branch.add(target);
        List<CraftingJobStep> steps = new ArrayList<>();

        for (IngredientNeed need : target.ingredients) {
            long totalNeeded = (long) need.amount() * batches;
            long takenFromStock = stock.take(need.sample(), totalNeeded);
            long missing = totalNeeded - takenFromStock;

            if (missing > 0) {
                CraftableOption producer = findProducer(need.sample(), branch, stock);
                if (producer == null) {
                    return null;
                }
                int yieldPerBatch = producer.output.getAmount() > 0 ? producer.output.getAmount() : 1;
                int subBatches = (int) Math.ceil((double) missing / yieldPerBatch);

                List<CraftingJobStep> subSteps = planCraft(producer, subBatches, stock, new HashSet<>(branch), depth + 1);
                if (subSteps == null) {
                    return null;
                }
                steps.addAll(subSteps);

                long totalProduced = (long) subBatches * yieldPerBatch;
                stock.add(producer.output, totalProduced);
                long takenMissing = stock.take(need.sample(), missing);
                if (takenMissing < missing) {
                    return null;
                }
            }
        }

        steps.add(new CraftingJobStep(target, batches));
        return steps;
    }

    private int calculateMaxBatches(CraftableOption opt) {
        SimulatedStock s1 = new SimulatedStock(network.storage().view());
        if (planCraft(opt, 1, s1, new HashSet<>(), 0) == null) {
            return 0;
        }

        int low = 1;
        int high = 64;
        while (high <= 10_000) {
            SimulatedStock s = new SimulatedStock(network.storage().view());
            if (planCraft(opt, high, s, new HashSet<>(), 0) != null) {
                low = high;
                high *= 2;
            } else {
                break;
            }
        }

        int best = low;
        int l = low;
        int r = Math.min(high, 10_000);
        while (l <= r) {
            int mid = (l + r) >>> 1;
            SimulatedStock s = new SimulatedStock(network.storage().view());
            if (planCraft(opt, mid, s, new HashSet<>(), 0) != null) {
                best = mid;
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        return best;
    }

    private ItemStack buildOptionIcon(CraftableOption opt) {
        ItemStack icon = opt.output.clone();
        var meta = icon.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }

        DeviceType crafterType = NodeStore.getType(network.block(opt.crafterPos));
        String crafterTitle = crafterType != null ? crafterType.display() : "Request Crafter";
        lore.add(Component.empty());
        lore.add(Component.text(crafterTitle + " at: " + PosUtil.unpackX(opt.crafterPos) + ", "
                + PosUtil.unpackY(opt.crafterPos) + ", " + PosUtil.unpackZ(opt.crafterPos), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Required Ingredients:", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

        for (IngredientNeed need : opt.ingredients) {
            long count = network.storage().count(item -> StackUtils.itemsMatch(item, need.sample(), false));
            String ingName = Blueprints.readableName(need.sample());
            if (count >= need.amount()) {
                lore.add(Component.text(" • " + ingName + ": " + count + " / " + need.amount(), NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                CraftableOption sub = findProducer(need.sample(), Set.of(opt), null);
                if (sub != null) {
                    lore.add(Component.text(" • " + ingName + ": " + count + " / " + need.amount() + " (Auto-Craftable via " + sub.name + ")", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text(" • " + ingName + ": " + count + " / " + need.amount(), NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
        }

        int maxBatches = calculateMaxBatches(opt);
        lore.add(Component.empty());
        lore.add(Component.text("Max Craftable Batches: " + maxBatches,
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
                        int maxBatches = calculateMaxBatches(opt);
                        if (batches > maxBatches) {
                            batches = maxBatches;
                        }
                        if (batches <= 0) {
                            player.sendMessage(Text.msg("Cannot craft: missing raw materials in network.", NamedTextColor.RED));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return;
                        }
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
                player.sendMessage(Text.msg("Cannot craft: missing raw materials in network.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            executeOrder(opt, batches);
            refresh();
        }
    }

    private int calculateOrderAmount(ClickType click, CraftableOption opt) {
        int maxBatches = calculateMaxBatches(opt);
        if (maxBatches <= 0) {
            return 0;
        }

        int requested = switch (click) {
            case SHIFT_LEFT -> 10;
            case RIGHT -> 64;
            default -> 1;
        };

        return Math.min(requested, maxBatches);
    }

    private void executeOrder(CraftableOption opt, int requestedBatches) {
        SimulatedStock checkStock = new SimulatedStock(network.storage().view());
        List<CraftingJobStep> plan = planCraft(opt, requestedBatches, checkStock, new HashSet<>(), 0);
        if (plan == null || plan.isEmpty()) {
            player.sendMessage(Text.msg("Crafting Job Failed: Insufficient raw materials in network.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        List<ItemStack> intermediateBuffer = new ArrayList<>();
        int unitsPerCraft = opt.output.getAmount() > 0 ? opt.output.getAmount() : 1;
        int totalTargetCrafted = 0;
        boolean failedMidway = false;

        for (int stepIdx = 0; stepIdx < plan.size(); stepIdx++) {
            CraftingJobStep step = plan.get(stepIdx);
            boolean isLastTargetStep = (stepIdx == plan.size() - 1) && (step.option() == opt);

            for (int b = 0; b < step.batches(); b++) {
                List<ItemStack> outputSink = isLastTargetStep ? new ArrayList<>() : intermediateBuffer;
                boolean success = executeSingleCraftStep(step.option(), intermediateBuffer, outputSink);
                if (!success) {
                    failedMidway = true;
                    break;
                }
                if (isLastTargetStep) {
                    for (ItemStack out : outputSink) {
                        if (deliverToInventory) {
                            giveOrDrop(out);
                        } else {
                            int left = network.storage().deposit(out);
                            if (left > 0) {
                                giveOrDrop(StackUtils.getAsQuantity(out, left));
                            }
                        }
                    }
                    totalTargetCrafted++;
                }
            }
            if (failedMidway) {
                break;
            }
        }

        // Return any remaining intermediate items to network storage (or give to player if full)
        for (ItemStack leftover : intermediateBuffer) {
            if (leftover != null && leftover.getAmount() > 0) {
                int left = network.storage().deposit(leftover);
                if (left > 0) {
                    giveOrDrop(StackUtils.getAsQuantity(leftover, left));
                }
            }
        }
        intermediateBuffer.clear();

        if (failedMidway && totalTargetCrafted <= 0) {
            player.sendMessage(Text.msg("Crafting Job Failed: Materials exhausted during multi-step execution.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int totalItemsCrafted = totalTargetCrafted * unitsPerCraft;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        if (totalTargetCrafted == requestedBatches) {
            player.sendMessage(Text.msg("Crafting Job Complete: Ordered " + totalItemsCrafted + "x " + opt.name
                    + (deliverToInventory ? " (delivered to inventory)." : " (deposited in network)."), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Text.msg("Crafting Job Partial: Ordered " + (requestedBatches * unitsPerCraft)
                    + "x " + opt.name + ", crafted " + totalItemsCrafted + " (materials exhausted).", NamedTextColor.YELLOW));
        }
    }

    private boolean executeSingleCraftStep(CraftableOption stepOpt, List<ItemStack> intermediateBuffer, List<ItemStack> outputSink) {
        List<IngredientNeed> needs = stepOpt.ingredients;

        // Check availability
        for (IngredientNeed need : needs) {
            long inBuf = countInBuffer(intermediateBuffer, need.sample());
            long inStore = network.storage().count(item -> StackUtils.itemsMatch(item, need.sample(), false));
            if (inBuf + inStore < need.amount()) {
                return false;
            }
        }

        // Extract ingredients
        List<ItemStack> extracted = new ArrayList<>();
        for (IngredientNeed need : needs) {
            int remaining = need.amount();
            remaining -= takeFromBuffer(intermediateBuffer, need.sample(), remaining, extracted);
            if (remaining > 0) {
                ItemStack pulled = network.storage().withdraw(item -> StackUtils.itemsMatch(item, need.sample(), false), remaining);
                if (pulled != null && pulled.getAmount() > 0) {
                    extracted.add(pulled);
                    remaining -= pulled.getAmount();
                }
            }
            if (remaining > 0) {
                // Rollback
                for (ItemStack ext : extracted) {
                    addToBuffer(intermediateBuffer, ext);
                }
                return false;
            }
        }

        // Resolve result
        ItemStack result = null;
        if (stepOpt.blueprintData != null) {
            Recipe rec = Blueprints.resolve(stepOpt.blueprintData.inputs, network.world());
            if (Blueprints.matchesOutput(rec, stepOpt.blueprintData.output)) {
                result = rec.getResult().clone();
            } else if (SlimefunBridge.isAvailable()) {
                ItemStack sf = SlimefunBridge.findSlimefunRecipe(stepOpt.blueprintData.inputs);
                if (sf != null) {
                    String expectedId = SlimefunBridge.getId(stepOpt.blueprintData.output);
                    String actualId = SlimefunBridge.getId(sf);
                    if (expectedId != null && expectedId.equalsIgnoreCase(actualId)) {
                        result = sf.clone();
                    } else if (StackUtils.itemsMatch(sf, stepOpt.blueprintData.output, false)) {
                        result = sf.clone();
                    }
                }
            }
            // Misma regla que el Auto-Crafter (CraftingSupport.tryCraftBlueprint): fiarse de la
            // salida guardada en el plano solo vale para recetas de Slimefun. Un plano vanilla cuya
            // receta ya no resuelve no fabrica nada en vez de entregar su salida a ciegas.
            if (result == null && stepOpt.blueprintData.output != null
                    && (SlimefunBridge.isSlimefunItem(stepOpt.blueprintData.output)
                    || SlimefunBridge.getId(stepOpt.blueprintData.output) != null)) {
                result = stepOpt.blueprintData.output.clone();
            }
        } else if (stepOpt.vanillaRecipe != null) {
            result = stepOpt.vanillaRecipe.getResult().clone();
        }

        if (result == null) {
            for (ItemStack ext : extracted) {
                addToBuffer(intermediateBuffer, ext);
            }
            return false;
        }

        addToBuffer(outputSink, result);
        return true;
    }

    private long countInBuffer(List<ItemStack> buffer, ItemStack sample) {
        long total = 0;
        for (ItemStack is : buffer) {
            if (is != null && StackUtils.itemsMatch(is, sample, false)) {
                total += is.getAmount();
            }
        }
        return total;
    }

    private int takeFromBuffer(List<ItemStack> buffer, ItemStack sample, int needed, List<ItemStack> extracted) {
        int taken = 0;
        var it = buffer.iterator();
        while (it.hasNext() && taken < needed) {
            ItemStack is = it.next();
            if (is != null && StackUtils.itemsMatch(is, sample, false)) {
                int take = Math.min(is.getAmount(), needed - taken);
                is.setAmount(is.getAmount() - take);
                taken += take;
                if (extracted != null) {
                    extracted.add(StackUtils.getAsQuantity(is, take));
                }
                if (is.getAmount() <= 0) {
                    it.remove();
                }
            }
        }
        return taken;
    }

    private void addToBuffer(List<ItemStack> buffer, ItemStack item) {
        if (item == null || item.getAmount() <= 0) return;
        int remaining = item.getAmount();
        for (ItemStack is : buffer) {
            if (is != null && StackUtils.itemsMatch(is, item, false)) {
                int space = is.getMaxStackSize() - is.getAmount();
                if (space > 0) {
                    int add = Math.min(space, remaining);
                    is.setAmount(is.getAmount() + add);
                    remaining -= add;
                    if (remaining <= 0) break;
                }
            }
        }
        while (remaining > 0) {
            int batch = Math.min(remaining, item.getMaxStackSize());
            buffer.add(StackUtils.getAsQuantity(item, batch));
            remaining -= batch;
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
