package com.chagui68.multiversenets.craft;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;

/**
 * Una receta embebida en un Blueprint: los 9 ingredientes de la matriz (de a 1) y el resultado.
 *
 * Es el equivalente del BlueprintInstance de NetworksV6 (PersistenContainer bajo la key
 * "ntw_blueprint"), solo que en vez de PDC anidado se serializa entero a Base64 dentro del PDC
 * del item, igual que NodeStore hace con los blobs. Serializable via BukkitObject streams.
 */
public class RecipeData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Exactamente 9 posiciones; los huecos libres son null. Cada item va con amount 1. */
    public final ItemStack[] inputs;
    public final ItemStack output;

    public RecipeData(ItemStack[] inputs, ItemStack output) {
        if (inputs == null || inputs.length != 9) {
            throw new IllegalArgumentException("La matriz de receta tiene que tener 9 huecos");
        }
        this.inputs = inputs;
        this.output = output;
    }
}
