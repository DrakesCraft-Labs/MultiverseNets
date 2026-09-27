package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
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

import java.util.List;

/**
 * Slimefun Recipe Encoder GUI: encodes 3x3 Slimefun crafting patterns and recipes onto blank Blueprint items.
 *
 * Menú del Codificador de Recetas de Slimefun: codifica patrones 3x3 exclusivos de Slimefun en planos en blanco.
 */
public class SfEncoderMenu extends MenuHolder {

    private static final int[] MATRIX_SLOTS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private static final int BLANK_SLOT = 19;
    private static final int ENCODE_SLOT = 16;
    private static final int OUTPUT_SLOT = 34;
    private static final int PREVIEW_SLOT = 25;

    private final Block block;

    public SfEncoderMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(45, Component.text("Slimefun Recipe Encoder", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected java.util.Set<Integer> vanillaSlots() {
        return java.util.Set.of(BLANK_SLOT, OUTPUT_SLOT);
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        return blob == null ? NodeBlob.create(DeviceType.MVN_SF_ENCODER.name()) : blob;
    }

    @Override
    protected void draw() {
        ItemStack background = panel(Material.PURPLE_STAINED_GLASS_PANE, " ");
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
        em.displayName(Component.text("Encode Slimefun Recipe", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        em.lore(List.of(
                Component.text("Fills a Blank Blueprint with the Slimefun recipe on the left", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        encode.setItemMeta(em);
        inv.setItem(ENCODE_SLOT, encode);

        SlimefunBridge.SlimefunRecipeDetails details = currentRecipe();
        if (details != null) {
            ItemStack preview = details.output().clone();
            var pm = preview.getItemMeta();
            pm.lore(List.of(
                    Component.text("Result: " + Blueprints.readableName(details.output()), NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Slimefun ID: " + details.sfId(), NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Type: " + details.recipeType(), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false)));
            preview.setItemMeta(pm);
            inv.setItem(PREVIEW_SLOT, preview);
        } else {
            inv.setItem(PREVIEW_SLOT, panel(Material.RED_STAINED_GLASS_PANE, "No matching Slimefun recipe"));
        }
        inv.setItem(OUTPUT_SLOT + 9, panel(Material.ORANGE_STAINED_GLASS_PANE, "Output above"));
    }

    private SlimefunBridge.SlimefunRecipeDetails currentRecipe() {
        NodeBlob blob = blob();
        if (Blueprints.isEmpty(blob.craftingMatrix)) {
            return null;
        }
        return SlimefunBridge.findSlimefunRecipeDetails(blob.craftingMatrix);
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
        SlimefunBridge.SlimefunRecipeDetails details = currentRecipe();
        if (details == null) {
            player.sendMessage(Text.msg("That arrangement does not match any Slimefun recipe.",
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
        RecipeData data = new RecipeData(Blueprints.normalize(blob().craftingMatrix), details.output().clone());
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
        player.sendMessage(Text.msg("Slimefun blueprint encoded: " + Blueprints.readableName(details.output()),
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
