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

/**
 * Utility helper for network autocrafting operations (both recipe keys and blueprints).
 *
 * Clase utilitaria para operaciones de autocrafteo en la red (tanto claves de receta como planos).
 */
public final class CraftingSupport {

    private CraftingSupport() {
    }

    /**
     * Finds a registered Bukkit recipe from its NamespacedKey string.
 *
     * Busca una receta registrada de Bukkit a partir de su cadena de texto NamespacedKey.
     *
     * @param key String representation of NamespacedKey / Representación en texto de NamespacedKey
     * @return Bukkit Recipe or null / Receta de Bukkit o null
     */
    public static Recipe find(String key) {
        NamespacedKey nk = NamespacedKey.fromString(key);
        return nk == null ? null : Bukkit.getRecipe(nk);
    }

    /**
     * Extracts ingredient choices and their required counts from a Bukkit recipe.
 *
     * Extrae las opciones de ingredientes y sus cantidades requeridas de una receta Bukkit.
     *
     * @param recipe Target recipe / Receta objetivo
     * @return List of RecipeChoice requirements / Lista de requisitos RecipeChoice
     */
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

    /**
     * Attempts to craft the first viable recipe configured in the node blob.
 *
     * Intenta craftear la primera receta viable configurada en el blob del nodo.
     *
     * @param net Target network / Red objetivo
     * @param blob Node configuration blob / Blob de configuración del nodo
     * @return true if crafted / true si se crafteó
     */
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

    /**
     * Attempts a single craft of a given Bukkit recipe using network storage items.
 *
     * Intenta un único crafteo de una receta Bukkit usando ítems del almacenamiento de red.
     *
     * @param net Target network / Red objetivo
     * @param recipe Bukkit recipe / Receta Bukkit
     * @return true if crafted and deposited / true si se crafteó y depositó
     */
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
     * Crafts once from an installed Blueprint (NetworksV6 auto-crafter logic):
     * *   - Resolves the vanilla recipe from the matrix and asserts its output matches the blueprint.
     *   - Aggregates ingredient requirements and checks all-or-nothing availability.
     *   - Extracts ingredients item-by-item; rolls back if anything fails midway.
     *   - Deposits the result; rolls back ingredients if output did not fit.
     * * 
     * Craftea una vez desde un Blueprint instalado (lógica de NetworkAutoCrafter):
     * *   - Resuelve la receta de vanilla de la matriz y exige que su salida coincida con la del blueprint.
     *   - Cuenta los ingredientes agrupados y comprueba disponibilidad antes de tocar nada (all-or-nothing).
     *   - Extrae ingrediente a ingrediente; si alguno falla a mitad, devuelve lo sacado.
     *   - Entrega el resultado a la red; si no cupo entero, revierte los ingredientes extraídos.
     * *
     * @param net Target network / Red objetivo
     * @param data Blueprint recipe data / Datos de la receta del plano
     * @return true if successfully crafted and deposited / true si se crafteó y depositó con éxito
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
            net.storage().depositAll(taken);
            return false;
        }
        return true;
    }
}
