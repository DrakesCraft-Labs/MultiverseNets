package com.chagui68.multiversenets.craft;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;

/**
 * [EN] Blueprint Recipe Data Structure
 * Encapsulates the 9 input ingredients (each with amount 1) and the expected crafting output.
 * Serialized into Base64 within the Blueprint item's PersistentDataContainer.
 *
 * [ES] Estructura de Datos de Receta para Blueprint
 * Encapsula los 9 ingredientes de la matriz (cada uno con cantidad 1) y el resultado esperado.
 * Serializado en Base64 en el PersistentDataContainer del ítem Blueprint.
 */
public class RecipeData implements Serializable {

    private static final long serialVersionUID = 1L;

    /** EN: Exactly 9 matrix positions (null for empty slots) / ES: Exactamente 9 posiciones (null para huecos vacíos). */
    public final ItemStack[] inputs;
    /** EN: Target output item / ES: Ítem resultante de la receta. */
    public final ItemStack output;

    public RecipeData(ItemStack[] inputs, ItemStack output) {
        if (inputs == null || inputs.length != 9) {
            throw new IllegalArgumentException("Recipe matrix must have exactly 9 slots / La matriz debe tener 9 huecos");
        }
        this.inputs = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            this.inputs[i] = sanitize(inputs[i]);
        }
        this.output = sanitize(output);
    }

    /**
     * Sanitizes an ItemStack to guarantee it is a standard org.bukkit.inventory.ItemStack,
     * stripping any vendor-specific subclasses (e.g. SlimefunItemStack) that fail during
     * BukkitObjectInputStream deserialization while preserving all metadata and PDC tags.
     */
    public static ItemStack sanitize(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }
        if (stack.getClass() == ItemStack.class) {
            return stack;
        }
        ItemStack pure = new ItemStack(stack.getType(), stack.getAmount());
        if (stack.hasItemMeta()) {
            pure.setItemMeta(stack.getItemMeta().clone());
        }
        return pure;
    }
}
