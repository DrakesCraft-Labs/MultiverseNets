package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * [EN] GUI Menu for Quantum Fluid Storage Cells (MVN_FLUID_CELL).
 * Displays stored fluid, volume gauge, and provides quick bucket filling/emptying controls.
 *
 * [ES] Menú GUI para Celdas Cuánticas de Fluidos.
 */
public class FluidCellMenu extends MenuHolder {

    public static final int TANK_SLOT = 13;
    public static final int INTERACT_SLOT = 10;
    public static final int EXTRACT_BUCKET_SLOT = 15;
    public static final int CLEAR_SLOT = 16;

    private static final int[] BG_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 11, 12, 14, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final Block block;

    public FluidCellMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(27, Component.text("Quantum Fluid Cell", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of();
    }

    private NodeBlob blob() {
        NodeBlob b = NodeStore.get(block);
        if (b == null) {
            b = NodeBlob.create("MVN_FLUID_CELL");
        }
        return b;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();
        long capacity = Settings.fluidCellCapacity();

        ItemStack bg = panel(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int s : BG_SLOTS) {
            inv.setItem(s, bg);
        }

        // 1. Tank Center Gauge
        inv.setItem(TANK_SLOT, tankIcon(blob, capacity));

        // 2. Bucket Input / Container slot
        ItemStack interactBtn = new ItemStack(Material.BUCKET);
        var metaIn = interactBtn.getItemMeta();
        metaIn.displayName(Component.text("Container Interaction", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        metaIn.lore(List.of(
                Component.text("Click with a filled bucket to DEPOSIT fluid.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Click with an empty bucket to EXTRACT fluid.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        interactBtn.setItemMeta(metaIn);
        inv.setItem(INTERACT_SLOT, interactBtn);

        // 3. Extract 1 Bucket Quick Button
        ItemStack extractBtn = new ItemStack(Material.WATER_BUCKET);
        var metaExt = extractBtn.getItemMeta();
        metaExt.displayName(Component.text("Quick Extract 1 Bucket (1,000 mB)", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        metaExt.lore(List.of(
                Component.text("Extracts 1 bucket of stored liquid into your inventory.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Requires an empty bucket in your inventory.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        extractBtn.setItemMeta(metaExt);
        inv.setItem(EXTRACT_BUCKET_SLOT, extractBtn);

        // 4. Void / Purge Cell Button (Right Click confirm)
        ItemStack clearBtn = new ItemStack(Material.BARRIER);
        var metaClr = clearBtn.getItemMeta();
        metaClr.displayName(Component.text("Void Fluid Tank", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        metaClr.lore(List.of(
                Component.text("Permanently dumps all fluid stored in this cell.", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Shift + Right Click to confirm purge.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        clearBtn.setItemMeta(metaClr);
        inv.setItem(CLEAR_SLOT, clearBtn);
    }

    private ItemStack tankIcon(NodeBlob blob, long capacity) {
        String fluid = blob.fluidType != null ? blob.fluidType.toUpperCase(Locale.ROOT) : "EMPTY";
        long amount = blob.fluidAmount;

        Material iconMat = switch (fluid) {
            case "WATER" -> Material.WATER_BUCKET;
            case "LAVA" -> Material.LAVA_BUCKET;
            case "MILK" -> Material.MILK_BUCKET;
            case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
            case "HONEY" -> Material.HONEY_BOTTLE;
            default -> Material.GLASS_BOTTLE;
        };

        ItemStack icon = new ItemStack(iconMat);
        var meta = icon.getItemMeta();
        NamedTextColor color = switch (fluid) {
            case "WATER" -> NamedTextColor.AQUA;
            case "LAVA" -> NamedTextColor.GOLD;
            case "MILK" -> NamedTextColor.WHITE;
            case "POWDER_SNOW" -> NamedTextColor.GRAY;
            case "HONEY" -> NamedTextColor.YELLOW;
            default -> NamedTextColor.DARK_GRAY;
        };

        meta.displayName(Component.text("Fluid Tank: " + fluid, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Volume: " + Items.formatAmount(amount) + " / " + Items.formatAmount(capacity) + " mB",
                NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Buckets: " + (amount / 1000) + " / " + (capacity / 1000),
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

        double pct = capacity > 0 ? (double) amount / capacity * 100.0 : 0.0;
        lore.add(Component.text("Fill Level: " + Math.round(pct * 10.0) / 10.0 + "%",
                pct > 90 ? NamedTextColor.GREEN : (pct > 20 ? NamedTextColor.YELLOW : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();
        long capacity = Settings.fluidCellCapacity();

        if (raw == INTERACT_SLOT || raw == TANK_SLOT) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                handleCursorInteract(cursor, blob, capacity);
                refresh();
            }
            return;
        }

        if (raw == EXTRACT_BUCKET_SLOT) {
            handleQuickExtract(blob);
            refresh();
            return;
        }

        if (raw == CLEAR_SLOT && event.getClick() == ClickType.SHIFT_RIGHT) {
            blob.fluidType = null;
            blob.fluidAmount = 0;
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Fluid cell drained and purged.", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1f, 1f);
            refresh();
        }
    }

    private void handleCursorInteract(ItemStack cursor, NodeBlob blob, long capacity) {
        Material mat = cursor.getType();

        // 1. Depositing filled container
        String depositingFluid = null;
        int mbPerItem = 1000;
        Material returnMat = Material.BUCKET;

        if (mat == Material.WATER_BUCKET) {
            depositingFluid = "WATER";
        } else if (mat == Material.LAVA_BUCKET) {
            depositingFluid = "LAVA";
        } else if (mat == Material.MILK_BUCKET) {
            depositingFluid = "MILK";
        } else if (mat == Material.POWDER_SNOW_BUCKET) {
            depositingFluid = "POWDER_SNOW";
        } else if (mat == Material.HONEY_BOTTLE) {
            depositingFluid = "HONEY";
            mbPerItem = 250;
            returnMat = Material.GLASS_BOTTLE;
        }

        if (depositingFluid != null) {
            if (blob.fluidType != null && !blob.fluidType.equalsIgnoreCase(depositingFluid) && blob.fluidAmount > 0) {
                player.sendMessage(Text.msg("Tank already contains " + blob.fluidType + "!", NamedTextColor.RED));
                return;
            }
            if (blob.fluidAmount + mbPerItem > capacity) {
                player.sendMessage(Text.msg("Fluid cell is full!", NamedTextColor.RED));
                return;
            }
            blob.fluidType = depositingFluid;
            blob.fluidAmount += mbPerItem;
            NodeStore.put(block, blob);

            cursor.setAmount(cursor.getAmount() - 1);
            if (cursor.getAmount() <= 0) {
                player.setItemOnCursor(new ItemStack(returnMat));
            } else {
                giveOrDrop(new ItemStack(returnMat));
            }
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
            return;
        }

        // 2. Extracting into empty bucket
        if (mat == Material.BUCKET) {
            if (blob.fluidAmount < 1000 || blob.fluidType == null) {
                player.sendMessage(Text.msg("Not enough liquid (need at least 1,000 mB).", NamedTextColor.RED));
                return;
            }
            Material filledBucket = switch (blob.fluidType.toUpperCase(Locale.ROOT)) {
                case "WATER" -> Material.WATER_BUCKET;
                case "LAVA" -> Material.LAVA_BUCKET;
                case "MILK" -> Material.MILK_BUCKET;
                case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
                default -> null;
            };
            if (filledBucket == null) {
                player.sendMessage(Text.msg("Fluid " + blob.fluidType + " cannot be held in standard bucket.", NamedTextColor.RED));
                return;
            }
            blob.fluidAmount -= 1000;
            if (blob.fluidAmount <= 0) {
                blob.fluidAmount = 0;
                blob.fluidType = null;
            }
            NodeStore.put(block, blob);

            cursor.setAmount(cursor.getAmount() - 1);
            if (cursor.getAmount() <= 0) {
                player.setItemOnCursor(new ItemStack(filledBucket));
            } else {
                giveOrDrop(new ItemStack(filledBucket));
            }
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1f, 1f);
        }
    }

    private void handleQuickExtract(NodeBlob blob) {
        if (blob.fluidAmount < 1000 || blob.fluidType == null) {
            player.sendMessage(Text.msg("Not enough liquid in tank (need at least 1,000 mB).", NamedTextColor.RED));
            return;
        }
        Material filledBucket = switch (blob.fluidType.toUpperCase(Locale.ROOT)) {
            case "WATER" -> Material.WATER_BUCKET;
            case "LAVA" -> Material.LAVA_BUCKET;
            case "MILK" -> Material.MILK_BUCKET;
            case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
            default -> null;
        };
        if (filledBucket == null) {
            player.sendMessage(Text.msg("Stored fluid cannot be placed into bucket.", NamedTextColor.RED));
            return;
        }

        // Consume 1 empty bucket from player inventory
        boolean foundBucket = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.BUCKET) {
                stack.setAmount(stack.getAmount() - 1);
                if (stack.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
                foundBucket = true;
                break;
            }
        }

        if (!foundBucket) {
            player.sendMessage(Text.msg("You need an empty bucket in your inventory.", NamedTextColor.RED));
            return;
        }

        blob.fluidAmount -= 1000;
        if (blob.fluidAmount <= 0) {
            blob.fluidAmount = 0;
            blob.fluidType = null;
        }
        NodeStore.put(block, blob);

        giveOrDrop(new ItemStack(filledBucket));
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1f, 1f);
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
