package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
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
import java.util.Map;
import java.util.TreeMap;

/**
 * GUI menu for the Network Monitor displaying controller info, device breakdowns, storage metrics, and network health.
 *
 * Menú del Monitor de Red que muestra información del controlador, desglose de dispositivos, métricas de almacenamiento y salud de la red.
 */
public class MonitorMenu extends MenuHolder {

    private final Network network;
    private final Block block;

    public MonitorMenu(MultiverseNets plugin, Player player, Network network, Block block) {
        super(plugin, player);
        this.network = network;
        this.block = block;
    }

    public void openMenu() {
        open(27, Component.text("Network Monitor", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        inv.setItem(4, controllerIcon());
        inv.setItem(11, breakdownIcon());
        inv.setItem(13, storageIcon());
        inv.setItem(15, statusIcon());
        inv.setItem(22, button(Material.SUNFLOWER, "Refresh"));
    }

    private ItemStack icon(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
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

    private ItemStack controllerIcon() {
        long pos = network.controllerPos();
        return icon(Material.LODESTONE, "Controller",
                List.of("Position: " + PosUtil.unpackX(pos) + "," + PosUtil.unpackY(pos) + "," + PosUtil.unpackZ(pos),
                        "Nodes: " + network.size()));
    }

    private ItemStack breakdownIcon() {
        Map<String, Integer> counts = new TreeMap<>();
        synchronized (network.nodes()) {
            for (DeviceType type : network.nodes().values()) {
                counts.merge(type.display(), 1, Integer::sum);
            }
        }
        List<String> lines = new ArrayList<>();
        counts.forEach((name, count) -> lines.add(name + ": " + count));
        if (lines.isEmpty()) {
            lines.add("No nodes");
        }
        return icon(Material.CHEST, "Node breakdown", lines);
    }

    private ItemStack storageIcon() {
        var view = network.storage().view();
        long total = 0;
        for (var entry : view) {
            total += entry.amount();
        }
        return icon(Material.SHULKER_SHELL, "Storage",
                List.of("Item types: " + view.size(),
                        "Total stored: " + Items.formatAmount(total)));
    }

    private ItemStack statusIcon() {
        boolean ok = network.error == null || network.error.isBlank();
        return icon(ok ? Material.LIME_DYE : Material.RED_DYE, "Status",
                List.of(ok ? "All systems operational" : network.error), ok);
    }

    private ItemStack icon(Material material, String name, List<String> loreLines, boolean ok) {
        ItemStack item = icon(material, name, loreLines);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, ok ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        if (event.getRawSlot() == 22) {
            network.scan();
            refresh();
            player.sendMessage(Text.msg("Monitor refreshed.", NamedTextColor.GRAY));
        }
    }
}
