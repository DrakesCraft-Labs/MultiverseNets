package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.StackUtils;
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
import org.bukkit.inventory.Recipe;

import java.util.List;

/**
 * El Encoder de NetworksV6, version standalone: 45 huecos con la misma distribucion.
 *
 *   Matriz 3x3 en 12-14 / 21-23 / 30-32 (plantillas: clic con item lo apunta, clic sin item lo
 *   borra; nunca consume items), blueprint en blanco en 19 (hueco VANILLA: el item va y viene
 *   de verdad), boton de codificar en 16, salida en 34 (vanilla) y vista previa del resultado
 *   en 25.
 *
 * Codificar consume 1 blueprint en blanco y escribe la receta completa (matriz + salida) en un
 * item de Blueprint que cae en la salida. La matriz se guarda en el blob del bloque y sobrevive
 * al cerrar el menu.
 */
public class EncoderMenu extends MenuHolder {

    private static final int[] MATRIX_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private static final int BLANK_SLOT = 19;
    private static final int ENCODE_SLOT = 16;
    private static final int OUTPUT_SLOT = 34;
    private static final int PREVIEW_SLOT = 25;

    private final Block block;

    public EncoderMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(45, Component.text("Recipe Encoder", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of(BLANK_SLOT, OUTPUT_SLOT);
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        return blob == null ? NodeBlob.create(DeviceType.ENCODER.name()) : blob;
    }

    @Override
    protected void draw() {
        ItemStack fondo = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            // Los huecos vanilla (blank/output) NO se rellenan de paneles: son reales.
            if (i == BLANK_SLOT || i == OUTPUT_SLOT) {
                continue;
            }
            inv.setItem(i, fondo);
        }
        // La matriz plantilla guardada en el bloque.
        NodeBlob blob = blob();
        for (int i = 0; i < MATRIX_SLOTS.length; i++) {
            ItemStack tpl = blob.craftingMatrix[i];
            if (tpl != null && !tpl.getType().isAir()) {
                inv.setItem(MATRIX_SLOTS[i], StackUtils.getAsQuantity(tpl, 1));
            } else {
                inv.setItem(MATRIX_SLOTS[i], panel(Material.BLACK_STAINED_GLASS_PANE, "Empty"));
            }
        }

        inv.setItem(BLANK_SLOT - 9, panel(Material.BLUE_STAINED_GLASS_PANE, "Blank Blueprints below"));
        inv.setItem(BLANK_SLOT + 9, panel(Material.BLUE_STAINED_GLASS_PANE, "Blank Blueprints above"));

        ItemStack encode = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        var em = encode.getItemMeta();
        em.displayName(Component.text("Encode Recipe", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        em.lore(List.of(
                Component.text("Fills a Blank Blueprint with the recipe on the left", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        encode.setItemMeta(em);
        inv.setItem(ENCODE_SLOT, encode);

        RecipeData actual = recetaActual();
        if (actual != null) {
            ItemStack preview = actual.output.clone();
            var pm = preview.getItemMeta();
            pm.lore(List.of(Component.text("Result: " + Blueprints.readableName(actual.output),
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            preview.setItemMeta(pm);
            inv.setItem(PREVIEW_SLOT, preview);
        } else {
            inv.setItem(PREVIEW_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "No matching recipe"));
        }
        inv.setItem(OUTPUT_SLOT + 9, panel(Material.ORANGE_STAINED_GLASS_PANE, "Output above"));
    }

    /** La receta que saldria de la matriz actual, o null si no casa con nada de vanilla. */
    private RecipeData recetaActual() {
        NodeBlob blob = blob();
        if (Blueprints.isEmpty(blob.craftingMatrix)) {
            return null;
        }
        Recipe recipe = Blueprints.resolve(blob.craftingMatrix, block.getWorld());
        if (recipe == null) {
            return null;
        }
        return new RecipeData(Blueprints.normalize(blob.craftingMatrix), recipe.getResult().clone());
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        for (int i = 0; i < MATRIX_SLOTS.length; i++) {
            if (MATRIX_SLOTS[i] == raw) {
                editarMatriz(i, event);
                return;
            }
        }
        if (raw == ENCODE_SLOT) {
            codificar();
            return;
        }
        // Shift sobre un blueprint en blanco del propio inventario: va directo a su hueco.
        if (raw >= inv.getSize()) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || Items.typeOf(mover) != DeviceType.BLUEPRINT || Blueprints.read(mover) != null) {
                return;
            }
            int slotJugador = slotInventarioJugador(event);
            ItemStack actual = inv.getItem(BLANK_SLOT);
            if (actual == null || actual.getType().isAir()) {
                inv.setItem(BLANK_SLOT, mover);
                player.getInventory().setItem(slotJugador, null);
            } else if (Items.typeOf(actual) == DeviceType.BLUEPRINT && Blueprints.read(actual) == null) {
                int pasan = Math.min(actual.getMaxStackSize() - actual.getAmount(), mover.getAmount());
                if (pasan <= 0) {
                    return;
                }
                actual.setAmount(actual.getAmount() + pasan);
                mover.setAmount(mover.getAmount() - pasan);
                if (mover.getAmount() <= 0) {
                    player.getInventory().setItem(slotJugador, null);
                }
            }
            return;
        }
        refresh();
    }

    private void editarMatriz(int indice, InventoryClickEvent event) {
        NodeBlob blob = blob();
        ItemStack cursor = event.getView().getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            blob.craftingMatrix[indice] = null;
        } else {
            blob.craftingMatrix[indice] = StackUtils.getAsQuantity(cursor, 1);
        }
        NodeStore.put(block, blob);
        refresh();
    }

    private void codificar() {
        RecipeData data = recetaActual();
        if (data == null) {
            player.sendMessage(Text.msg("That arrangement does not match any vanilla recipe.",
                    NamedTextColor.RED));
            return;
        }
        ItemStack blank = inv.getItem(BLANK_SLOT);
        if (blank == null || Items.typeOf(blank) != DeviceType.BLUEPRINT
                || Blueprints.read(blank) != null) {
            player.sendMessage(Text.msg("Put a Blank Blueprint in the blue slot first.",
                    NamedTextColor.RED));
            return;
        }
        ItemStack codificado = Blueprints.toItem(data);
        ItemStack salida = inv.getItem(OUTPUT_SLOT);
        if (salida != null && !salida.getType().isAir()) {
            if (!StackUtils.itemsMatch(salida, codificado) || salida.getAmount() >= salida.getMaxStackSize()) {
                player.sendMessage(Text.msg("The output slot is full.", NamedTextColor.RED));
                return;
            }
            salida.setAmount(salida.getAmount() + 1);
        } else {
            inv.setItem(OUTPUT_SLOT, codificado);
        }
        blank.setAmount(blank.getAmount() - 1);
        if (blank.getAmount() <= 0) {
            inv.setItem(BLANK_SLOT, null);
        }
        player.sendMessage(Text.msg("Blueprint encoded: " + Blueprints.readableName(data.output),
                NamedTextColor.GREEN));
        refresh();
    }

    /** Al cerrar, lo que quede en los huecos vanilla vuelve al jugador. */
    @Override
    protected void onClose(InventoryCloseEvent event) {
        for (int slot : new int[]{BLANK_SLOT, OUTPUT_SLOT}) {
            ItemStack contenido = inv.getItem(slot);
            if (contenido != null && !contenido.getType().isAir()) {
                inv.setItem(slot, null);
                devolverAlJugador(contenido);
            }
        }
    }

    private ItemStack panel(Material material, String nombre) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(nombre, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
