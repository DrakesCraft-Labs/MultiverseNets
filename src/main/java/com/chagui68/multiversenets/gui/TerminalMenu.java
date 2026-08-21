package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkStorage;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TerminalMenu extends MenuHolder {

    private static final int PAGE_SIZE = 45;

    private final Network network;
    private int page = 0;
    private String query = "";

    public TerminalMenu(MultiverseNets plugin, Player player, Network network) {
        super(plugin, player);
        this.network = network;
    }

    public void openMenu() {
        open(54, Component.text("Network Terminal", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    private List<NetworkStorage.View> filtered() {
        List<NetworkStorage.View> all = network.storage().view();
        if (query.isBlank()) {
            return all;
        }
        String q = query.toLowerCase();
        List<NetworkStorage.View> out = new ArrayList<>();
        for (NetworkStorage.View v : all) {
            Material type = v.sample().getType();
            if (type.name().toLowerCase().contains(q) || type.translationKey().toLowerCase().contains(q)) {
                out.add(v);
            }
        }
        return out;
    }

    @Override
    protected void draw() {
        List<NetworkStorage.View> list = filtered();
        int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) {
            page = pages - 1;
        }
        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < list.size(); i++) {
            NetworkStorage.View view = list.get(start + i);
            ItemStack icon = view.sample().clone();
            icon.setAmount(Math.max(1, Math.min(icon.getMaxStackSize(), 64)));
            var meta = icon.getItemMeta();
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Amount: " + Items.formatAmount(view.amount()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Left click: withdraw a stack", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            icon.setItemMeta(meta);
            inv.setItem(i, icon);
        }
        drawControls(pages);
    }

    private void drawControls(int pages) {
        inv.setItem(45, button(Material.ARROW, "Previous page", page > 0));
        inv.setItem(47, button(Material.HOPPER, "Deposit inventory", true));
        inv.setItem(49, button(Material.COMPASS, query.isBlank() ? "Search" : "Search: " + query, true));
        inv.setItem(51, button(Material.BOOK, "Network info", true));
        inv.setItem(53, button(Material.SPECTRAL_ARROW, "Next page", page < pages - 1));
    }

    private ItemStack button(Material material, String label, boolean enabled) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(label, enabled ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        switch (raw) {
            case 45 -> {
                if (page > 0) {
                    page--;
                    refresh();
                }
                return;
            }
            case 47 -> {
                depositAllFromPlayer();
                return;
            }
            case 49 -> {
                ChatPrompts.ask(player, "Type your search term:", text -> {
                    query = text;
                    page = 0;
                    refresh();
                });
                return;
            }
            case 51 -> {
                player.sendMessage(Text.msg("Network @ " + coord() + " | Nodes: " + network.size(), NamedTextColor.AQUA));
                return;
            }
            case 53 -> {
                page++;
                refresh();
                return;
            }
            default -> {
            }
        }

        if (raw >= 0 && raw < PAGE_SIZE) {
            withdrawClicked(event.getCurrentItem());
            return;
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                depositAllFromPlayer();
            } else {
                depositOne(event);
            }
        }
    }

    private String coord() {
        return PosUtil.unpackX(network.controllerPos()) + "," + PosUtil.unpackY(network.controllerPos())
                + "," + PosUtil.unpackZ(network.controllerPos());
    }

    private void withdrawClicked(ItemStack icon) {
        if (icon == null || icon.getType().isAir()) {
            return;
        }
        int want = Math.min(icon.getMaxStackSize(), 64);
        ItemStack got = network.storage().withdraw(icon::isSimilar, want);
        if (got == null) {
            return;
        }
        int leftover = NetworkManager.insertInto(player.getInventory(), got);
        if (leftover > 0) {
            got.setAmount(leftover);
            network.storage().deposit(got);
        }
        refresh();
    }

    private void depositAllFromPlayer() {
        ItemStack[] contents = player.getInventory().getContents();
        long deposited = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir()) {
                continue;
            }
            int leftover = network.storage().deposit(it);
            if (leftover <= 0) {
                player.getInventory().setItem(i, null);
                deposited += it.getAmount();
            } else if (leftover < it.getAmount()) {
                deposited += it.getAmount() - leftover;
                it.setAmount(leftover);
            }
        }
        if (deposited > 0) {
            player.sendMessage(Text.msg("Deposited " + Items.formatAmount(deposited) + " items.", NamedTextColor.GREEN));
        }
        refresh();
    }

    private void depositOne(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()
                || event.getCursor() != null && !event.getCursor().getType().isAir()) {
            return;
        }
        int leftover = network.storage().deposit(current);
        if (leftover <= 0) {
            event.getClickedInventory().setItem(event.getSlot(), null);
        } else {
            current.setAmount(leftover);
        }
        refresh();
    }
}
