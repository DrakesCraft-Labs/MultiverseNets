package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mesa de trabajo cuántica (Quantum Workbench de Networks):
 * Permite mejorar Celdas Cuánticas (T1 -> T2 -> ... -> T6) transfiriendo
 * y preservando atómicamente todos los ítems guardados en su interior.
 */
public class QuantumWorkbenchMenu extends MenuHolder {

    public static final int[] RECIPE_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };
    public static final int CRAFT_SLOT = 23;
    public static final int OUTPUT_SLOT = 25;
    public static final int CENTER_SLOT = 20;

    private static final int[] BACKGROUND_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 13, 14, 15, 16, 17, 18, 22, 24, 26, 27,
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private final Block block;

    public QuantumWorkbenchMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(45, Component.text("Quantum Workbench", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected Set<Integer> vanillaSlots() {
        return Set.of(10, 11, 12, 19, 20, 21, 28, 29, 30, OUTPUT_SLOT);
    }

    @Override
    protected void draw() {
        ItemStack fondo = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BACKGROUND_SLOTS) {
            inv.setItem(slot, fondo);
        }

        ItemStack craftBtn = new ItemStack(Material.CRAFTING_TABLE);
        var meta = craftBtn.getItemMeta();
        meta.displayName(Component.text("Entangle & Upgrade", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click to entangle and upgrade", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("the central Quantum Cell.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("All stored items are preserved!", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)));
        craftBtn.setItemMeta(meta);
        inv.setItem(CRAFT_SLOT, craftBtn);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        if (raw == CRAFT_SLOT) {
            tryCraft();
            return;
        }

        // Si es shift-click desde inventario de jugador
        if (raw >= inv.getSize() && (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT
                || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT)) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            int slotJugador = slotInventarioJugador(event);
            // Intentar meter en alguno de los slots de receta vacíos o compatibles
            for (int rSlot : RECIPE_SLOTS) {
                ItemStack inSlot = inv.getItem(rSlot);
                if (inSlot == null || inSlot.getType().isAir()) {
                    inv.setItem(rSlot, mover.clone());
                    player.getInventory().setItem(slotJugador, null);
                    return;
                } else if (inSlot.isSimilar(mover) && inSlot.getAmount() < inSlot.getMaxStackSize()) {
                    int space = inSlot.getMaxStackSize() - inSlot.getAmount();
                    int toAdd = Math.min(space, mover.getAmount());
                    inSlot.setAmount(inSlot.getAmount() + toAdd);
                    mover.setAmount(mover.getAmount() - toAdd);
                    if (mover.getAmount() <= 0) {
                        player.getInventory().setItem(slotJugador, null);
                    } else {
                        player.getInventory().setItem(slotJugador, mover);
                    }
                    return;
                }
            }
        }
    }

    private void tryCraft() {
        ItemStack outputCurrent = inv.getItem(OUTPUT_SLOT);
        if (outputCurrent != null && !outputCurrent.getType().isAir()) {
            player.sendMessage(Text.msg("Please take the previous crafted item from the output slot first.", NamedTextColor.RED));
            return;
        }

        ItemStack center = inv.getItem(CENTER_SLOT);
        DeviceType centerType = Items.typeOf(center);
        if (centerType == null || !centerType.isCell() || centerType.cellTier() >= 6) {
            player.sendMessage(Text.msg("Place a Quantum Storage Cell (T1-T5) in the center slot.", NamedTextColor.YELLOW));
            return;
        }

        // Verificar que los 8 slots restantes tengan diamantes
        for (int slot : RECIPE_SLOTS) {
            if (slot == CENTER_SLOT) {
                continue;
            }
            ItemStack inSlot = inv.getItem(slot);
            if (inSlot == null || inSlot.getType() != Material.DIAMOND || inSlot.getAmount() < 1) {
                player.sendMessage(Text.msg("Surround the center Quantum Cell with 8 Diamonds.", NamedTextColor.YELLOW));
                return;
            }
        }

        // Calcular siguiente tier
        int nextTier = centerType.cellTier() + 1;
        DeviceType nextType = DeviceType.parse("CELL_T" + nextTier);
        if (nextType == null) {
            return;
        }

        ItemStack resultado = Items.create(nextType);
        // Transferir la carga almacenada (CELL_CARGO) del tier anterior al nuevo
        if (center.hasItemMeta()) {
            String cargo = center.getItemMeta().getPersistentDataContainer()
                    .get(Keys.CELL_CARGO, PersistentDataType.STRING);
            if (cargo != null) {
                NodeBlob blob = NodeStore.decode(cargo);
                if (blob != null) {
                    var resMeta = resultado.getItemMeta();
                    resMeta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, cargo);
                    List<Component> lore = new ArrayList<>();
                    if (resMeta.hasLore()) {
                        lore.addAll(resMeta.lore());
                    }
                    if (blob.cellSample != null && blob.cellAmount > 0) {
                        lore.add(Component.text("Cargo: " + Items.formatAmount(blob.cellAmount) + " x "
                                + blob.cellSample.getType().name(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                    }
                    resMeta.lore(lore);
                    resultado.setItemMeta(resMeta);
                }
            }
        }

        // Consumir 1 de cada slot de receta
        for (int slot : RECIPE_SLOTS) {
            ItemStack inSlot = inv.getItem(slot);
            if (inSlot != null) {
                if (inSlot.getAmount() <= 1) {
                    inv.setItem(slot, null);
                } else {
                    inSlot.setAmount(inSlot.getAmount() - 1);
                }
            }
        }

        inv.setItem(OUTPUT_SLOT, resultado);
        player.sendMessage(Text.msg("Quantum Storage upgraded to " + nextType.display() + "!", NamedTextColor.GREEN));
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        for (int slot : RECIPE_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                inv.setItem(slot, null);
                devolverAlJugador(item);
            }
        }
        ItemStack output = inv.getItem(OUTPUT_SLOT);
        if (output != null && !output.getType().isAir()) {
            inv.setItem(OUTPUT_SLOT, null);
            devolverAlJugador(output);
        }
    }

    private ItemStack panel(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
