package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
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

public class CrafterMenu extends MenuHolder {

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
        for (String key : blob.recipes) {
            if (slot >= 26) {
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
        meta.displayName(Component.text("Recipes", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Click with the result in hand: add recipe",
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        hint.setItemMeta(meta);
        inv.setItem(26, hint);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        if (event.getClickedInventory() != inv) {
            return;
        }
        NodeBlob blob = blob();
        List<String> recipes = new ArrayList<>(blob.recipes);

        ItemStack cursor = event.getCursor();
        int raw = event.getRawSlot();
        if (raw == 26) {
            refresh();
            return;
        }
        if (cursor != null && !cursor.getType().isAir()) {
            String blueprintKey = Items.readBlueprint(cursor);
            String key;
            if (blueprintKey != null) {
                key = blueprintKey;
            } else {
                key = findRecipeKeyByResult(cursor);
            }
            if (key == null) {
                player.sendMessage(Text.msg("No valid recipe for that result.", NamedTextColor.RED));
            } else if (!recipes.contains(key)) {
                if (recipes.size() >= 26) {
                    player.sendMessage(Text.msg("Recipe limit reached.", NamedTextColor.RED));
                } else {
                    recipes.add(key);
                    player.sendMessage(Text.msg(blueprintKey != null ? "Blueprint installed." : "Recipe added.", NamedTextColor.GREEN));
                }
            }
        } else if (raw >= 0 && raw < recipes.size()) {
            recipes.remove(raw);
        }
        blob.recipes.clear();
        blob.recipes.addAll(recipes);
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
