package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.MemoryModules;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
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
 * [EN] DRAM Bay menu: shows the installed memory module and its stock, installs a module from the
 * cursor or the inventory, and ejects it as an item that keeps everything it stored.
 *
 * [ES] Menú del DRAM Bay: muestra el módulo instalado y su stock, instala un módulo desde el cursor
 * o el inventario, y lo expulsa como ítem que conserva todo lo que guardaba.
 */
public class DramBayMenu extends MenuHolder {

    public static final int STATS_SLOT = 11;
    public static final int MODULE_SLOT = 13;
    public static final int EJECT_SLOT = 15;

    private final Block block;

    public DramBayMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(27, Component.text("DRAM Bay", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        ItemStack background = icon(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }
        NodeBlob blob = NodeStore.get(block);
        DeviceType module = MemoryModules.installed(blob);
        inv.setItem(STATS_SLOT, statsIcon(blob, module));
        if (module == null) {
            inv.setItem(MODULE_SLOT, icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "Empty Module Slot", NamedTextColor.GRAY, List.of(
                    "Click here with a memory module on the cursor,",
                    "or Shift-Click one in your inventory.",
                    "Item modules: L1, L2, L3, DRAM, Quantum.",
                    "Fluid DRAM Module: fluids only.")));
            inv.setItem(EJECT_SLOT, background);
        } else {
            ItemStack shown = Items.create(module);
            var meta = shown.getItemMeta();
            meta.lore(List.of(Component.text("Installed", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            shown.setItemMeta(meta);
            inv.setItem(MODULE_SLOT, shown);
            inv.setItem(EJECT_SLOT, icon(Material.ORANGE_STAINED_GLASS_PANE, "Eject Module", NamedTextColor.GOLD, List.of(
                    "Takes the module out WITH everything it stores.",
                    "Its stock leaves this network and appears in",
                    "the network whose DRAM Bay you install it in.")));
        }
    }

    private ItemStack statsIcon(NodeBlob blob, DeviceType module) {
        List<String> lines = new ArrayList<>();
        if (module == null) {
            lines.add("No module installed.");
            return icon(Material.BOOK, "Memory", NamedTextColor.AQUA, lines);
        }
        if (module.isCacheModule()) {
            long cap = MemoryModules.itemCapacity(module);
            long stored = blob.totalVirtualAmount();
            lines.add("Module: " + module.display());
            lines.add("Stored: " + Items.formatAmount(stored) + " / " + Items.formatAmount(cap) + " items");
            lines.add("Item types: " + (blob.virtualSamples == null ? 0 : blob.virtualSamples.size()));
        } else {
            long cap = Settings.fluidDramCapacity();
            lines.add("Module: " + module.display());
            lines.add("Stored: " + Items.formatAmount(blob.totalDramFluid()) + " / " + Items.formatAmount(cap) + " mB");
            for (int i = 0; i < blob.dramFluids.size(); i++) {
                lines.add(" • " + blob.dramFluids.get(i) + ": " + Items.formatAmount(blob.dramFluidAmounts.get(i)) + " mB");
            }
        }
        return icon(Material.BOOK, "Memory", NamedTextColor.AQUA, lines);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            player.closeInventory();
            return;
        }
        if (raw >= inv.getSize()) {
            ItemStack mover = event.getCurrentItem();
            DeviceType moverType = Items.typeOf(mover);
            if (moverType == null || !moverType.isMemoryModule()) {
                return;
            }
            int playerSlot = playerInventorySlot(event);
            if (install(blob, mover)) {
                if (mover.getAmount() > 1) {
                    mover.setAmount(mover.getAmount() - 1);
                    player.getInventory().setItem(playerSlot, mover);
                } else {
                    player.getInventory().setItem(playerSlot, null);
                }
                player.updateInventory();
            }
            return;
        }
        if (raw == MODULE_SLOT) {
            ItemStack cursor = event.getView().getCursor();
            DeviceType cursorType = Items.typeOf(cursor);
            if (cursorType != null && cursorType.isMemoryModule()) {
                if (install(blob, cursor)) {
                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        event.getView().setCursor(cursor);
                    } else {
                        event.getView().setCursor(null);
                    }
                }
                return;
            }
        }
        if ((raw == EJECT_SLOT || raw == MODULE_SLOT) && MemoryModules.installed(blob) != null) {
            ItemStack module = MemoryModules.eject(blob);
            NodeStore.put(block, blob);
            invalidate();
            giveOrDrop(module);
            player.sendMessage(Text.msg("Module ejected with its stock.", NamedTextColor.YELLOW));
            draw();
        }
    }

    private boolean install(NodeBlob blob, ItemStack moduleItem) {
        if (MemoryModules.installed(blob) != null) {
            player.sendMessage(Text.msg("This DRAM Bay already holds a module. Eject it first.", NamedTextColor.RED));
            return false;
        }
        if (!MemoryModules.install(blob, moduleItem)) {
            return false;
        }
        NodeStore.put(block, blob);
        invalidate();
        player.sendMessage(Text.msg("Installed " + Items.typeOf(moduleItem).display() + ".", NamedTextColor.GREEN));
        draw();
        return true;
    }

    private void invalidate() {
        Network net = plugin.networks().networkAt(block);
        if (net != null) {
            net.storage().invalidate();
        }
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }
}
