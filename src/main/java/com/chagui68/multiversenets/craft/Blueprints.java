package com.chagui68.multiversenets.craft;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.StackUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [EN] Blueprint Management & Recipe Resolution
 * Blueprints store a full 3x3 crafting matrix and expected output item.
 * Encoded in the Recipe Encoder, installed into Auto-Crafters, and resolved dynamically against Bukkit recipes.
 *
 * [ES] Gestión de Blueprints y Resolución de Recetas
 * Los Blueprints almacenan una matriz de crafteo 3x3 completa y su ítem resultante.
 * Se codifican en el Recipe Encoder, se instalan en Auto-Crafters y se resuelven contra la API de recetas de Bukkit.
 */
public final class Blueprints {

    private static final Map<String, Recipe> RECIPE_CACHE = new ConcurrentHashMap<>();

    private Blueprints() {
    }

    // ---------------------------------------------------------------- serializacion

    public static String encode(RecipeData data) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(data);
            out.flush();
            return java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo serializar la receta del blueprint", e);
        }
    }

    public static RecipeData decode(String data) {
        if (data == null) {
            return null;
        }
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(
                new ByteArrayInputStream(java.util.Base64.getDecoder().decode(data)))) {
            Object o = in.readObject();
            return o instanceof RecipeData rd ? rd : null;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- item <-> receta

    public static ItemStack toItem(RecipeData data) {
        ItemStack item = new ItemStack(DeviceType.MVN_BLUEPRINT.material());
        var meta = item.getItemMeta();
        meta.displayName(Component.text("Blueprint: " + readableName(data.output), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Assigned Recipe", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        for (ItemStack input : data.inputs) {
            if (input != null) {
                lore.add(Component.text("- " + readableName(input), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        lore.add(Component.empty());
        lore.add(Component.text("Output: " + readableName(data.output), NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click an Auto-Crafter to install", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, DeviceType.MVN_BLUEPRINT.name());
        meta.getPersistentDataContainer().set(Keys.BLUEPRINT_DATA, PersistentDataType.STRING, encode(data));
        item.setItemMeta(meta);
        return item;
    }

    /** La receta dentro del item de blueprint, o null si no es un blueprint nuevo. */
    public static RecipeData read(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BLUEPRINT_DATA, PersistentDataType.STRING);
        return decode(data);
    }

    public static boolean isBlueprint(ItemStack item) {
        return read(item) != null;
    }

    // ---------------------------------------------------------------- resolucion

    public static boolean isEmpty(ItemStack[] matrix) {
        for (ItemStack i : matrix) {
            if (i != null && !i.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    /** Matriz normalizada: clones de a 1, aire como null. */
    public static ItemStack[] normalize(ItemStack[] matrix) {
        ItemStack[] out = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack s = i < matrix.length ? matrix[i] : null;
            out[i] = (s == null || s.getType().isAir()) ? null : StackUtils.getAsQuantity(s, 1);
        }
        return out;
    }

    /**
     * Resuelve la receta de vanilla para una matriz (con cache). Devuelve null si no hay ninguna
     * receta de mesa de trabajo que case.
     */
    public static Recipe resolve(ItemStack[] matrix, World world) {
        ItemStack[] norm = normalize(matrix);
        String key = encode(new RecipeData(norm, null));
        return RECIPE_CACHE.computeIfAbsent(key, k -> {
            // Aire real en vez de null: algunas implementaciones del servidor (y MockBukkit en
            // los tests) asumen que la matriz nunca trae huecos a null.
            ItemStack[] paraBukkit = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                paraBukkit[i] = norm[i] == null ? new ItemStack(org.bukkit.Material.AIR) : norm[i];
            }
            return Bukkit.getCraftingRecipe(paraBukkit, world);
        });
    }

    /**
     * Compara una receta resuelta contra la salida esperada del blueprint. Lo pide NetworksV6
     * (matchesBlueprintRecipe) para que un blueprint deje de funcionar si la receta cambia.
     */
    public static boolean matchesOutput(Recipe recipe, ItemStack expected) {
        if (recipe == null || expected == null) {
            return false;
        }
        ItemStack result = recipe.getResult();
        return result != null && StackUtils.itemsMatch(expected, result, false);
    }

    public static String readableName(ItemStack item) {
        if (item == null) {
            return "Nothing";
        }
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        String raw = item.getType().name().toLowerCase(Locale.ROOT);
        StringBuilder pretty = new StringBuilder(raw.length());
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (c == '_') {
                pretty.append(' ');
                upper = true;
            } else if (upper) {
                pretty.append(Character.toUpperCase(c));
                upper = false;
            } else {
                pretty.append(c);
            }
        }
        return pretty.toString();
    }
}
