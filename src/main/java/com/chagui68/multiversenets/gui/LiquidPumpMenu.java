package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [EN] GUI Menu for Liquid Pump Nodes (MVN_LIQUID_PUMP).
 * Configures pump mode (Drain/Fill), directional face, and fluid type filter.
 *
 * [ES] Menú GUI para Bombas de Líquidos.
 */
public class LiquidPumpMenu extends MenuHolder {

    public static final int MODE_SLOT = 10;
    public static final int FLUID_FILTER_SLOT = 13;
    public static final int DIRECTION_SLOT = 16;
    public static final int INFO_SLOT = 22;

    private static final int[] BG_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 11, 12, 14, 15, 17,
            18, 19, 20, 21, 23, 24, 25, 26
    };

    private static final String[] FLUID_CHOICES = {
            "ANY", "WATER", "LAVA"
    };

    private final Block block;

    public LiquidPumpMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(27, Component.text("Liquid Pump Configuration", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of();
    }

    private NodeBlob blob() {
        NodeBlob b = NodeStore.get(block);
        if (b == null) {
            b = NodeBlob.create("MVN_LIQUID_PUMP");
        }
        return b;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();

        ItemStack bg = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int s : BG_SLOTS) {
            inv.setItem(s, bg);
        }

        // 1. Pump Mode Button (Fixed to DRAIN below)
        ItemStack modeItem = new ItemStack(Material.HOPPER);
        var metaMode = modeItem.getItemMeta();
        metaMode.displayName(Component.text("Pump Mode: DRAIN (Extract Below)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        metaMode.lore(List.of(
                Component.text("Drains liquid sources directly from the block below.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Only accepts Water and Lava source blocks.", NamedTextColor.DARK_AQUA)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        modeItem.setItemMeta(metaMode);
        inv.setItem(MODE_SLOT, modeItem);

        // 2. Fluid Type Filter Button (ANY / WATER / LAVA)
        String currentFluid = blob.pumpFluid != null ? blob.pumpFluid.toUpperCase(java.util.Locale.ROOT) : "ANY";
        Material filterMat = switch (currentFluid) {
            case "WATER" -> Material.WATER_BUCKET;
            case "LAVA" -> Material.LAVA_BUCKET;
            default -> Material.BUCKET;
        };
        ItemStack filterItem = new ItemStack(filterMat);
        var metaFilter = filterItem.getItemMeta();
        metaFilter.displayName(Component.text("Fluid Filter: " + currentFluid, NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        metaFilter.lore(List.of(
                Component.text("Restricts pumping to this fluid only (Water / Lava).", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to cycle filter (ANY / WATER / LAVA)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
        filterItem.setItemMeta(metaFilter);
        inv.setItem(FLUID_FILTER_SLOT, filterItem);

        // 3. Direction Button (Fixed to DOWN)
        ItemStack dirItem = new ItemStack(Material.COMPASS);
        var metaDir = dirItem.getItemMeta();
        metaDir.displayName(Component.text("Target Side: DOWN (Below)", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        metaDir.lore(List.of(
                Component.text("Pumps exclusively from the block directly underneath.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Position pump directly above Water or Lava.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        dirItem.setItemMeta(metaDir);
        inv.setItem(DIRECTION_SLOT, dirItem);

        // 4. Network Fluid Summary Info
        Network net = plugin.networks().networkAt(block);
        ItemStack infoItem = new ItemStack(Material.PRISMARINE_CRYSTALS);
        var metaInfo = infoItem.getItemMeta();
        metaInfo.displayName(Component.text("Network Fluids", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        List<Component> infoLore = new ArrayList<>();
        if (net != null) {
            Map<String, Long> fluids = net.fluidStorage().getFluids();
            if (fluids.isEmpty()) {
                infoLore.add(Component.text("No fluids currently stored in network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            } else {
                for (var entry : fluids.entrySet()) {
                    infoLore.add(Component.text(" • " + entry.getKey() + ": " + Items.formatAmount(entry.getValue()) + " mB ("
                            + (entry.getValue() / 1000) + " Buckets)", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                }
            }
            infoLore.add(Component.empty());
            infoLore.add(Component.text("Total Capacity: " + Items.formatAmount(net.fluidStorage().totalCapacity()) + " mB",
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else {
            infoLore.add(Component.text("Not connected to an active network.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        metaInfo.lore(infoLore);
        infoItem.setItemMeta(metaInfo);
        inv.setItem(INFO_SLOT, infoItem);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();

        if (raw == MODE_SLOT) {
            player.sendMessage(Text.msg("Pump mode is fixed to DRAIN (Extract Below).", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            return;
        }

        if (raw == FLUID_FILTER_SLOT) {
            String current = blob.pumpFluid != null ? blob.pumpFluid.toUpperCase(java.util.Locale.ROOT) : "ANY";
            int idx = 0;
            for (int i = 0; i < FLUID_CHOICES.length; i++) {
                if (FLUID_CHOICES[i].equalsIgnoreCase(current)) {
                    idx = (i + 1) % FLUID_CHOICES.length;
                    break;
                }
            }
            blob.pumpFluid = "ANY".equalsIgnoreCase(FLUID_CHOICES[idx]) ? null : FLUID_CHOICES[idx];
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Fluid filter set to: " + (blob.pumpFluid == null ? "ANY" : blob.pumpFluid), NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            refresh();
            return;
        }

        if (raw == DIRECTION_SLOT) {
            player.sendMessage(Text.msg("Pumping direction is fixed to DOWN (Block Below).", NamedTextColor.YELLOW));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
        }
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
