package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
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

    /**
     * EN: Memory modules no longer go in the Controller. This only explains where they go and,
     * for networks built before the DRAM Bay, how many old modules wait in the Terminal.
     * ES: Los módulos de memoria ya no van en el Controlador. Esto solo explica dónde van y, en
     * redes anteriores al DRAM Bay, cuántos módulos antiguos esperan en el Terminal.
     */
    private ItemStack virtualCacheIcon(NodeBlob blob) {
        int recovered = blob == null || blob.recoveredModules == null ? 0 : blob.recoveredModules.size();
        int bays = network.count(DeviceType.MVN_DRAM_BAY);
        ItemStack item = new ItemStack(Material.WAXED_COPPER_BULB);
        var meta = item.getItemMeta();
        meta.displayName(Component.text("Network Memory", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Memory modules go in a DRAM Bay", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("connected to this network, not here.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("DRAM Bays in this network: " + bays, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        if (recovered > 0) {
            lore.add(Component.empty());
            lore.add(Component.text(recovered + " old module(s) were inside this Controller.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("They wait in the Terminal as temporary", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("items, with all their items inside.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
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
        if (slot == 15) {
            player.closeInventory();
            new TerminalMenu(plugin, player, network).openMenu();
        } else if (slot == 22) {
            network.scan();
            refresh();
            player.sendMessage(Text.msg("Controller refreshed.", NamedTextColor.GRAY));
        }
    }
}
