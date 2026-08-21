package com.chagui68.multiversenets.craft;

import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CraftingSupport {

    private CraftingSupport() {
    }

    public static Recipe find(String key) {
        NamespacedKey nk = NamespacedKey.fromString(key);
        return nk == null ? null : Bukkit.getRecipe(nk);
    }

    public static List<Map.Entry<RecipeChoice, Integer>> requirements(Recipe recipe) {
        List<Map.Entry<RecipeChoice, Integer>> reqs = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shaped) {
            Map<RecipeChoice, Integer> merged = new LinkedHashMap<>();
            for (RecipeChoice choice : shaped.getChoiceMap().values()) {
                if (choice != null) {
                    merged.merge(choice, 1, Integer::sum);
                }
            }
            merged.forEach((k, v) -> reqs.add(Map.entry(k, v)));
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            Map<RecipeChoice, Integer> merged = new LinkedHashMap<>();
            for (RecipeChoice choice : shapeless.getChoiceList()) {
                if (choice != null) {
                    merged.merge(choice, 1, Integer::sum);
                }
            }
            merged.forEach((k, v) -> reqs.add(Map.entry(k, v)));
        } else if (recipe instanceof CookingRecipe<?> cooking) {
            RecipeChoice choice = cooking.getInputChoice();
            if (choice != null) {
                reqs.add(Map.entry(choice, 1));
            }
        }
        return reqs;
    }

    public static boolean tryCraftAll(Network net, NodeBlob blob) {
        boolean crafted = false;
        for (String key : new ArrayList<>(blob.recipes)) {
            Recipe recipe = find(key);
            if (recipe == null || recipe instanceof ComplexRecipe) {
                continue;
            }
            if (tryCraftOnce(net, recipe)) {
                crafted = true;
                break;
            }
        }
        return crafted;
    }

    public static boolean tryCraftOnce(Network net, Recipe recipe) {
        var reqs = requirements(recipe);
        if (reqs.isEmpty()) {
            return false;
        }
        for (var entry : reqs) {
            RecipeChoice choice = entry.getKey();
            int needed = entry.getValue();
            if (net.storage().count(choice::test) < needed) {
                return false;
            }
        }
        List<ItemStack> taken = new ArrayList<>();
        for (var entry : reqs) {
            ItemStack got = net.storage().withdraw(entry.getKey()::test, entry.getValue());
            if (got == null || got.getAmount() < entry.getValue()) {
                net.storage().depositAll(taken);
                return false;
            }
            taken.add(got);
        }
        net.storage().deposit(recipe.getResult().clone());
        return true;
    }
}
