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
 * Recipe Encoder GUI: encodes 3x3 crafting patterns and recipes onto blank Blueprint items.
 *
 * Menú del Codificador de Recetas: codifica patrones de crafteo 3x3 y recetas en planos en blanco.
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
        return blob == null ? NodeBlob.create(DeviceType.MVN_ENCODER.name()) : blob;
    }

    @Override
    protected void draw() {
        ItemStack background = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == BLANK_SLOT || i == OUTPUT_SLOT) {
                continue;
            }
            inv.setItem(i, background);
        }
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

        RecipeData current = currentRecipe();
        if (current != null) {
            ItemStack preview = current.output.clone();
            var pm = preview.getItemMeta();
            pm.lore(List.of(Component.text("Result: " + Blueprints.readableName(current.output),
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            preview.setItemMeta(pm);
            inv.setItem(PREVIEW_SLOT, preview);
        } else {
            inv.setItem(PREVIEW_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "No matching recipe"));
        }
        inv.setItem(OUTPUT_SLOT + 9, panel(Material.ORANGE_STAINED_GLASS_PANE, "Output above"));
    }

    /**
     * Resolves the crafting recipe for the current matrix pattern, or null if none matches.
 *
     * Resuelve la receta de crafteo para la matriz actual, o null si ninguna coincide.
     */
    private RecipeData currentRecipe() {
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
                editMatrix(i, event);
                return;
            }
        }
        if (raw == ENCODE_SLOT) {
            encodeBlueprint();
            return;
        }
        if (raw >= inv.getSize()) {
            ItemStack moving = event.getCurrentItem();
            if (moving == null || Items.typeOf(moving) != DeviceType.MVN_BLUEPRINT || Blueprints.read(moving) != null) {
                return;
            }
            int playerSlot = playerInventorySlot(event);
            ItemStack current = inv.getItem(BLANK_SLOT);
            if (current == null || current.getType().isAir()) {
                inv.setItem(BLANK_SLOT, moving);
                player.getInventory().setItem(playerSlot, null);
                player.updateInventory();
            } else if (Items.typeOf(current) == DeviceType.MVN_BLUEPRINT && Blueprints.read(current) == null) {
                int transferred = Math.min(current.getMaxStackSize() - current.getAmount(), moving.getAmount());
                if (transferred <= 0) {
                    return;
                }
                current.setAmount(current.getAmount() + transferred);
                moving.setAmount(moving.getAmount() - transferred);
                if (moving.getAmount() <= 0) {
                    player.getInventory().setItem(playerSlot, null);
                } else {
                    player.getInventory().setItem(playerSlot, moving);
                }
                player.updateInventory();
            }
            return;
        }
        refresh();
    }

    private void editMatrix(int index, InventoryClickEvent event) {
        NodeBlob blob = blob();
        ItemStack cursor = event.getView().getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            blob.craftingMatrix[index] = null;
        } else {
            blob.craftingMatrix[index] = StackUtils.getAsQuantity(cursor, 1);
        }
        NodeStore.put(block, blob);
        refresh();
    }

    private void encodeBlueprint() {
        RecipeData data = currentRecipe();
        if (data == null) {
            player.sendMessage(Text.msg("That arrangement does not match any vanilla recipe.",
                    NamedTextColor.RED));
            return;
        }
        ItemStack blank = inv.getItem(BLANK_SLOT);
        if (blank == null || Items.typeOf(blank) != DeviceType.MVN_BLUEPRINT
                || Blueprints.read(blank) != null) {
            player.sendMessage(Text.msg("Put a Blank Blueprint in the blue slot first.",
                    NamedTextColor.RED));
            return;
        }
        ItemStack encoded = Blueprints.toItem(data);
        ItemStack outputItem = inv.getItem(OUTPUT_SLOT);
        if (outputItem != null && !outputItem.getType().isAir()) {
            if (!StackUtils.itemsMatch(outputItem, encoded) || outputItem.getAmount() >= outputItem.getMaxStackSize()) {
                player.sendMessage(Text.msg("The output slot is full.", NamedTextColor.RED));
                return;
            }
            outputItem.setAmount(outputItem.getAmount() + 1);
        } else {
            inv.setItem(OUTPUT_SLOT, encoded);
        }
        blank.setAmount(blank.getAmount() - 1);
        if (blank.getAmount() <= 0) {
            inv.setItem(BLANK_SLOT, null);
        }
        player.sendMessage(Text.msg("Blueprint encoded: " + Blueprints.readableName(data.output),
                NamedTextColor.GREEN));
        refresh();
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        for (int slot : new int[]{BLANK_SLOT, OUTPUT_SLOT}) {
            ItemStack content = inv.getItem(slot);
            if (content != null && !content.getType().isAir()) {
                inv.setItem(slot, null);
                giveOrDrop(content);
            }
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
