package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.List;

public class EncoderMenu extends MenuHolder {

    public EncoderMenu(MultiverseNets plugin, Player player) {
        super(plugin, player);
    }

    public void openMenu() {
        open(27, Component.text("Recipe Encoder", NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
    }

    @Override
    protected void draw() {
        ItemStack hint = new ItemStack(Material.BOOK);
        var meta = hint.getItemMeta();
        meta.displayName(Component.text("Encode a recipe", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Click with the RESULT item on your cursor", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("You will receive a Blueprint for Auto-Crafters", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        hint.setItemMeta(meta);
        inv.setItem(13, hint);
    }

    @Override
    protected void click(InventoryClickEvent event) {
        if (event.getClickedInventory() != inv) {
            return;
        }
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return;
        }
        String key = findRecipeKeyByResult(cursor);
        if (key == null) {
            player.sendMessage(Text.msg("No valid recipe found for that result.", NamedTextColor.RED));
            return;
        }
        player.getInventory().addItem(Items.blueprint(key, cursor.getType().name()));
        player.sendMessage(Text.msg("Blueprint created: " + key, NamedTextColor.GREEN));
        refresh();
    }

    private String findRecipeKeyByResult(ItemStack sample) {
        var it = Bukkit.recipeIterator();
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
