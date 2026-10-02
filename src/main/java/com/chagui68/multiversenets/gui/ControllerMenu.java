package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller Management Menu: displays network status, CPU Virtual Cache tiers & upgrades,
 * and router antenna broadcasting state.
 */
public class ControllerMenu extends MenuHolder {

    private final Network network;
    private final Block block;

    public ControllerMenu(MultiverseNets plugin, Player player, Network network, Block block) {
        super(plugin, player);
        this.network = network;
        this.block = block;
    }

    public void openMenu() {
        open(27, Component.text("Network Controller", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        NodeBlob blob = NodeStore.get(block);
        inv.setItem(4, controllerIcon());
        inv.setItem(11, virtualCacheIcon(blob));
        inv.setItem(13, routerIcon());
        inv.setItem(15, button(Material.BEACON, "Open Terminal Grid"));
        inv.setItem(22, button(Material.SUNFLOWER, "Rescan Network"));
    }

    private ItemStack controllerIcon() {
        long pos = network.controllerPos();
        boolean ok = network.error == null || network.error.isBlank();
        ItemStack item = new ItemStack(Material.LODESTONE);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("Controller Core", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Position: " + PosUtil.unpackX(pos) + ", " + PosUtil.unpackY(pos) + ", " + PosUtil.unpackZ(pos), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Total Nodes: " + network.size(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Status: " + (ok ? "Operational" : network.error), ok ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack virtualCacheIcon(NodeBlob blob) {
        int tier = blob != null ? blob.virtualCacheTier : 0;
        long total = blob != null ? blob.totalVirtualAmount() : 0;
        long cap = Settings.virtualCacheCapacity(tier);
        String tierName = switch (tier) {
            case 1 -> "L1 CPU Cache";
            case 2 -> "L2 CPU Cache";
            case 3 -> "L3 CPU Cache";
            case 4 -> "System DRAM";
            case 5 -> "Quantum Cache";
            default -> "None (Uninstalled)";
        };
        Material mat = switch (tier) {
            case 1 -> Material.COPPER_INGOT;
            case 2 -> Material.GOLD_INGOT;
            case 3 -> Material.DIAMOND;
            case 4 -> Material.NETHERITE_INGOT;
            case 5 -> Material.NETHER_STAR;
            default -> Material.GRAY_DYE;
        };
        ItemStack item = new ItemStack(mat);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("Legacy Memory Module", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Tier: " + tierName + (tier > 0 ? " (T" + tier + ")" : ""), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        if (tier > 0) {
            double pct = cap > 0 ? (double) total / cap * 100.0 : 0;
            lore.add(Component.text("Capacity: " + Items.formatAmount(cap) + " items", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Stored: " + Items.formatAmount(total) + " (" + String.format("%.1f", pct) + "%)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Item Types: " + (blob.virtualSamples != null ? blob.virtualSamples.size() : 0), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("Click: take the module out WITH its items", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("and install it in a DRAM Bay.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Memory modules now go in a DRAM Bay", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("connected to this network.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack routerIcon() {
        int routers = network.count(DeviceType.MVN_ROUTER);
        boolean active = routers > 0;
        ItemStack item = new ItemStack(active ? Material.LIGHTNING_ROD : Material.IRON_BARS);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("Router Antenna", active ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Status: " + (active ? "ONLINE (" + routers + " active)" : "OFFLINE"), active ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                Component.text(active ? "Broadcasting global wireless signal across chunks and dimensions." : "Wireless Terminal restricted to 64 blocks in local world.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 11) {
            // Controladores anteriores al DRAM Bay: el modulo sale con sus items para llevarlo a un bay.
            NodeBlob blob = NodeStore.get(block);
            ItemStack module = MemoryModules.ejectControllerCache(blob);
            if (module != null) {
                NodeStore.put(block, blob);
                network.storage().invalidate();
                giveOrDrop(module);
                player.sendMessage(Text.msg("Module taken out with its items. Install it in a DRAM Bay.", NamedTextColor.GREEN));
                draw();
            }
        } else if (slot == 15) {
            player.closeInventory();
            new TerminalMenu(plugin, player, network).openMenu();
        } else if (slot == 22) {
            network.scan();
            refresh();
            player.sendMessage(Text.msg("Controller refreshed.", NamedTextColor.GRAY));
        }
    }
}
