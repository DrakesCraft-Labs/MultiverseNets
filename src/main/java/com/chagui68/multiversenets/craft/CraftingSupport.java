package com.chagui68.multiversenets.craft;

import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.util.StackUtils;
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

    /**
     * Craftea una vez desde un Blueprint instalado (logica de NetworkAutoCrafter de NetworksV6):
     *
     *   1. Resuelve la receta de vanilla de la matriz y exige que su salida coincida con la del
     *      blueprint; si el servidor ya no ofrece esa receta, el plano deja de craftear en vez
     *      de producir otra cosa a escondidas.
     *   2. Cuenta los ingredientes agrupados y comprueba que la red los tenga TODOS antes de
     *      tocar nada (all-or-nothing).
     *   3. Extrae ingrediente a ingrediente; si alguno falla a mitad, devuelve lo sacado.
     *   4. Entrega el resultado a la red; si no cupo entero, revierte tambien los ingredientes
     *      (como NetworksV6, que aborta cuando el output no cabe).
     */
    public static boolean tryCraftBlueprint(Network net, RecipeData data) {
        if (data == null || Blueprints.isEmpty(data.inputs) || data.output == null) {
            return false;
        }
        Recipe recipe = Blueprints.resolve(data.inputs, net.world());
        if (!Blueprints.matchesOutput(recipe, data.output)) {
            return false;
        }
        ItemStack result = recipe.getResult().clone();

        // Ingredientes agrupados: un 2x2 de tablas gasta 4 maderas, no 4 extracciones sueltas.
        record Need(ItemStack sample, int amount) {
        }
        List<Need> needs = new ArrayList<>();
        for (ItemStack input : data.inputs) {
            if (input == null || input.getType().isAir()) {
                continue;
            }
            boolean merged = false;
            for (int i = 0; i < needs.size(); i++) {
                Need n = needs.get(i);
                if (StackUtils.itemsMatch(n.sample(), input)) {
                    needs.set(i, new Need(n.sample(), n.amount() + 1));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                needs.add(new Need(StackUtils.getAsQuantity(input, 1), 1));
            }
        }
        for (Need need : needs) {
            if (net.storage().count(item -> StackUtils.itemsMatch(item, need.sample())) < need.amount()) {
                return false;
            }
        }

        List<ItemStack> taken = new ArrayList<>();
        for (Need need : needs) {
            ItemStack got = net.storage().withdraw(item -> StackUtils.itemsMatch(item, need.sample()),
                    need.amount());
            if (got == null || got.getAmount() < need.amount()) {
                if (got != null) {
                    taken.add(got);
                }
                net.storage().depositAll(taken);
                return false;
            }
            taken.add(got);
        }

        int leftover = net.storage().deposit(result);
        if (leftover > 0) {
            // La red no trago el resultado entero: se devuelven los ingredientes y se aborta.
            // Como todo corre en el hilo principal dentro de la misma pasada, la red sigue
            // teniendo el hueco que dejaron las extracciones y el rollback no puede fallar.
            net.storage().depositAll(taken);
            return false;
        }
        return true;
    }
}
