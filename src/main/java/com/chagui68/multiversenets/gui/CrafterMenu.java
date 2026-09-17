package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Auto-Crafter: guarda blueprints (matriz real 3x3, como en el NetworkAutoCrafter de
 * NetworksV6) y claves de receta antiguas. En cada pasada del ticker intenta fabricar cada una
 * con extraccion atomica desde la red: todo o nada, sin consumos parciales.
 *
 * Click con un Blueprint en el cursor: se instala una COPIA de sus datos (el item no se traga;
 * el de Networks se quedaba fisicamente dentro del bloque, aqui basta con leerlo). Click sin
 * cursor sobre una entrada: la desinstala.
 */
public class CrafterMenu extends MenuHolder {

    private static final int HINT_SLOT = 26;

    private final Block block;

    public CrafterMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        open(27, Component.text(DeviceType.CRAFTER.display() + " (" + block.getX() + "," + block.getY()
                + "," + block.getZ() + ")", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        return blob == null ? NodeBlob.create(DeviceType.CRAFTER.name()) : blob;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();
        int slot = 0;
        for (String b64 : blob.blueprintData) {
            if (slot >= HINT_SLOT) {
                break;
            }
            RecipeData data = Blueprints.decode(b64);
            ItemStack icon = data == null || data.output == null
                    ? new ItemStack(Material.BARRIER) : data.output.clone();
            var meta = icon.getItemMeta();
            meta.displayName(Component.text(
                            data == null ? "Blueprint ilegible" : "Blueprint: " + Blueprints.readableName(data.output),
                            NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to uninstall", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }
        for (String key : blob.recipes) {
            if (slot >= HINT_SLOT) {
                break;
            }
            Recipe recipe = CraftingSupport.find(key);
            ItemStack icon = recipe == null ? new ItemStack(Material.BARRIER) : recipe.getResult().clone();
            var meta = icon.getItemMeta();
            meta.displayName(Component.text(key, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to remove", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }
        ItemStack hint = new ItemStack(Material.PAPER);
        var meta = hint.getItemMeta();
        meta.displayName(Component.text("Recipes (" + (blob.blueprintData.size() + blob.recipes.size())
                + "/" + Settings.maxBlueprints() + ")", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click with a Blueprint in cursor: install", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Click on an entry with empty cursor: remove", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        hint.setItemMeta(meta);
        inv.setItem(HINT_SLOT, hint);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        // Shift sobre un blueprint del inventario propio: lo instala sin tocar el item.
        if (event.getClickedInventory() != inv) {
            ItemStack mover = event.getCurrentItem();
            RecipeData data = mover == null ? null : Blueprints.read(mover);
            if (data == null) {
                return;
            }
            NodeBlob blob = blob();
            if (blob.blueprintData.size() + blob.recipes.size() >= Settings.maxBlueprints()) {
                player.sendMessage(Text.msg("Recipe limit reached.", NamedTextColor.RED));
                return;
            }
            String encoded = Blueprints.encode(data);
            if (!blob.blueprintData.contains(encoded)) {
                blob.blueprintData.add(encoded);
                NodeStore.put(block, blob);
                player.sendMessage(Text.msg("Blueprint installed: "
                        + Blueprints.readableName(data.output), NamedTextColor.GREEN));
            }
            refresh();
            return;
        }
        int raw = event.getRawSlot();
        if (raw == HINT_SLOT) {
            refresh();
            return;
        }
        NodeBlob blob = blob();
        ItemStack cursor = event.getCursor();

        if (cursor != null && !cursor.getType().isAir()) {
            RecipeData data = Blueprints.read(cursor);
            String legacyKey = data == null ? Items.readBlueprint(cursor) : null;
            if (data == null && legacyKey == null) {
                legacyKey = findRecipeKeyByResult(cursor);
            }
            if (data == null && legacyKey == null) {
                player.sendMessage(Text.msg("That item is not a valid blueprint or recipe result.",
                        NamedTextColor.RED));
                return;
            }
            int total = blob.blueprintData.size() + blob.recipes.size();
            if (total >= Settings.maxBlueprints()) {
                player.sendMessage(Text.msg("Recipe limit reached.", NamedTextColor.RED));
                return;
            }
            if (data != null) {
                String encoded = Blueprints.encode(data);
                if (!blob.blueprintData.contains(encoded)) {
                    blob.blueprintData.add(encoded);
                    player.sendMessage(Text.msg("Blueprint installed: "
                            + Blueprints.readableName(data.output), NamedTextColor.GREEN));
                }
            } else if (!blob.recipes.contains(legacyKey)) {
                blob.recipes.add(legacyKey);
                player.sendMessage(Text.msg("Recipe added.", NamedTextColor.GREEN));
            }
            NodeStore.put(block, blob);
            refresh();
            return;
        }

        // Sin cursor: click sobre una entrada la retira.
        if (raw < 0 || raw >= HINT_SLOT) {
            return;
        }
        int blueCount = blob.blueprintData.size();
        if (raw < blueCount) {
            blob.blueprintData.remove(raw);
        } else if (raw - blueCount < blob.recipes.size()) {
            blob.recipes.remove(raw - blueCount);
        }
        NodeStore.put(block, blob);
        refresh();
    }

    private String findRecipeKeyByResult(ItemStack sample) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof ComplexRecipe || recipe.getResult().getType().isAir()) {
                continue;
            }
            if (recipe.getResult().isSimilar(sample) && recipe instanceof org.bukkit.Keyed keyed) {
                return keyed.getKey().toString();
            }
        }
        return null;
    }
}
