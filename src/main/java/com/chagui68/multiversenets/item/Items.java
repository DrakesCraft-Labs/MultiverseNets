package com.chagui68.multiversenets.item;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class Items {

    private Items() {
    }

    public static ItemStack create(DeviceType type) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.display(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("MultiverseNets", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (type.isCell() || type == DeviceType.INFINITY_BARREL || type == DeviceType.GREEDY_CELL) {
            lore.add(Component.text("Capacity: " + formatAmount(capacityOf(type)), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    public static DeviceType typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String name = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.DEVICE_TYPE, PersistentDataType.STRING);
        return name == null ? null : DeviceType.parse(name);
    }

    // ---------------------------------------------------------------- herramientas

    /** Un rake nuevo con todos sus usos. */
    public static ItemStack rake() {
        ItemStack item = create(DeviceType.RAKE);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RAKE_USES, PersistentDataType.INTEGER, Settings.rakeUses());
        meta.lore(java.util.List.of(
                Component.text("Right click a network node to remove it instantly.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(Settings.rakeUses() + " uses left", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    public static int rakeUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer uses = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.RAKE_USES, PersistentDataType.INTEGER);
        return uses == null ? 0 : uses;
    }

    /**
     * Gasta un uso. Devuelve false cuando la herramienta se ha gastado del todo y hay que romperla.
     */
    public static boolean spendRakeUse(ItemStack item) {
        int uses = rakeUses(item) - 1;
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RAKE_USES, PersistentDataType.INTEGER, uses);
        meta.lore(java.util.List.of(
                Component.text("Right click a network node to remove it instantly.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text(Math.max(0, uses) + " uses left", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return uses > 0;
    }

    /** Configuracion copiada en el wrench: "WL:MAT1,MAT2" o "BL:MAT1,MAT2". */
    public static void saveConfig(ItemStack item, java.util.List<String> mats, boolean blacklist) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CONFIG_DATA, PersistentDataType.STRING,
                (blacklist ? "BL:" : "WL:") + String.join(",", mats));
        meta.lore(java.util.List.of(
                Component.text((blacklist ? "Blacklist: " : "Whitelist: ")
                                + (mats.isEmpty() ? "(vacio)" : String.join(", ", mats)),
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
    }

    /** Devuelve [datos-en-minusculas..., "bl"/"wl"], o null si el wrench no guarda nada. */
    public static String[] readConfig(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.CONFIG_DATA, PersistentDataType.STRING);
        if (data == null || data.length() < 3) {
            return null;
        }
        String mode = data.substring(0, 3);
        String body = data.substring(3);
        if (body.isEmpty()) {
            return new String[]{mode.equals("BL:") ? "bl" : "wl"};
        }
        String[] mats = body.split(",");
        String[] out = new String[mats.length + 1];
        System.arraycopy(mats, 0, out, 0, mats.length);
        out[mats.length] = mode.equals("BL:") ? "bl" : "wl";
        return out;
    }

    public static ItemStack blueprint(String recipeKey, String resultName) {
        ItemStack item = new ItemStack(DeviceType.BLUEPRINT.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blueprint: " + resultName, NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Recipe: " + recipeKey, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Click an Auto-Crafter to install", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, DeviceType.BLUEPRINT.name());
        meta.getPersistentDataContainer().set(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING, recipeKey);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBlueprint(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
    }

    public static String readBlueprint(ItemStack item) {
        if (!isBlueprint(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
    }

    public static void linkReceiver(ItemStack item, Location transmitter) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RECEIVER_BIND, PersistentDataType.STRING,
                transmitter.getWorld().getUID() + ";" + transmitter.getBlockX() + ";"
                        + transmitter.getBlockY() + ";" + transmitter.getBlockZ());
        item.setItemMeta(meta);
    }

    public static Location readReceiverBind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.RECEIVER_BIND, PersistentDataType.STRING);
        return parseLocation(data);
    }

    private static Location parseLocation(String data) {
        if (data == null) {
            return null;
        }
        String[] parts = data.split(";");
        var world = Bukkit.getWorld(java.util.UUID.fromString(parts[0]));
        if (world == null) {
            return null;
        }
        return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    public static void bindWireless(ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.WIRELESS_BIND, PersistentDataType.STRING,
                loc.getWorld().getUID() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ());
        item.setItemMeta(meta);
    }

    public static Location readWirelessBind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.WIRELESS_BIND, PersistentDataType.STRING);
        return parseLocation(data);
    }

    public static long capacityOf(DeviceType type) {
        if (type == DeviceType.INFINITY_BARREL) {
            return 2_000_000_000L;
        }
        if (type == DeviceType.GREEDY_CELL) {
            return com.chagui68.multiversenets.util.Settings.greedyCapacity();
        }
        return com.chagui68.multiversenets.util.Settings.cellCapacity(type.cellTier());
    }

    public static String formatAmount(long amount) {
        if (amount >= 1_000_000_000L) {
            return trim(amount / 1_000_000_000.0) + "B";
        }
        if (amount >= 1_000_000L) {
            return trim(amount / 1_000_000.0) + "M";
        }
        if (amount >= 1_000L) {
            return trim(amount / 1_000.0) + "k";
        }
        return String.valueOf(amount);
    }

    private static String trim(double v) {
        return v >= 100 ? String.valueOf((long) v) : String.valueOf(Math.round(v * 10.0) / 10.0);
    }

    public static void registerRecipes(MultiverseNets plugin) {
        shaped(plugin, "controller", create(DeviceType.CONTROLLER), r -> {
            r.shape("III", "INI", "III");
            r.setIngredient('I', Material.IRON_BLOCK);
            r.setIngredient('N', Material.NETHER_STAR);
        });
        shaped(plugin, "cable", stackOf(create(DeviceType.CABLE), 16), r -> {
            r.shape("GGG", "GRG", "GGG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('R', Material.REDSTONE);
        });
        shaped(plugin, "terminal", create(DeviceType.TERMINAL), r -> {
            r.shape("GEG", "EBE", "GEG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('E', Material.ENDER_PEARL);
            r.setIngredient('B', Material.BEACON);
        });
        shaped(plugin, "cell_t1", create(DeviceType.CELL_T1), r -> {
            r.shape("GGG", "GDG", "GGG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('D', Material.DIAMOND);
        });
        for (int tier = 2; tier <= 6; tier++) {
            DeviceType prev = DeviceType.valueOf("CELL_T" + (tier - 1));
            DeviceType cur = DeviceType.valueOf("CELL_T" + tier);
            final DeviceType prevFinal = prev;
            shaped(plugin, "cell_t" + tier, create(cur), r -> {
                r.shape("DDD", "DPD", "DDD");
                r.setIngredient('D', Material.DIAMOND);
                r.setIngredient('P', new org.bukkit.inventory.RecipeChoice.ExactChoice(create(prevFinal)));
            });
        }
        shaped(plugin, "grabber", create(DeviceType.GRABBER), r -> {
            r.shape("IOI", "ORO", "IOI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('O', Material.OBSERVER);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
        });
        shaped(plugin, "pusher", create(DeviceType.PUSHER), r -> {
            r.shape("IDI", "DRD", "IDI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('D', Material.DROPPER);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
        });
        shaped(plugin, "vacuum", create(DeviceType.VACUUM), r -> {
            r.shape("SRS", "RHR", "SRS");
            r.setIngredient('S', Material.STRING);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('H', Material.HOPPER);
        });
        shaped(plugin, "purger", create(DeviceType.PURGER), r -> {
            r.shape("ILI", "LHL", "ILI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('L', Material.MAGMA_BLOCK);
            r.setIngredient('H', Material.HOPPER);
        });
        shaped(plugin, "probe", create(DeviceType.PROBE), r -> {
            r.shape(" A ", "ASA", " A ");
            r.setIngredient('A', Material.AMETHYST_SHARD);
            r.setIngredient('S', Material.SPYGLASS);
        });
        shaped(plugin, "crafter", create(DeviceType.CRAFTER), r -> {
            r.shape("RCR", "ITI", "RCR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('T', Material.TARGET);
        });
        shaped(plugin, "wireless_terminal", create(DeviceType.WIRELESS_TERMINAL), r -> {
            r.shape(" P ", "PNP", " C ");
            r.setIngredient('P', Material.ENDER_PEARL);
            r.setIngredient('N', Material.NETHER_STAR);
            r.setIngredient('C', Material.COMPASS);
        });
        shaped(plugin, "monitor", create(DeviceType.MONITOR), r -> {
            r.shape("GGG", "GCG", "GGG");
            r.setIngredient('G', Material.GLASS_PANE);
            r.setIngredient('C', Material.COMPARATOR);
        });
        shaped(plugin, "transmitter", create(DeviceType.TRANSMITTER), r -> {
            r.shape("IRI", "RCR", "IRI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
            r.setIngredient('C', Material.CONDUIT);
        });
        shaped(plugin, "receiver", create(DeviceType.RECEIVER), r -> {
            r.shape("IPI", "PLP", "IPI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('P', Material.ENDER_PEARL);
            r.setIngredient('L', Material.REDSTONE_LAMP);
        });
        shaped(plugin, "greedy_cell", create(DeviceType.GREEDY_CELL), r -> {
            r.shape("GHG", "HSH", "GHG");
            r.setIngredient('G', Material.GOLD_INGOT);
            r.setIngredient('H', Material.HOPPER);
            r.setIngredient('S', Material.SLIME_BLOCK);
        });
        shaped(plugin, "grabber_ht", create(DeviceType.GRABBER_HT), r -> {
            r.shape("OPO");
            r.setIngredient('O', Material.OBSERVER);
            r.setIngredient('P', Material.STICKY_PISTON);
        });
        shaped(plugin, "pusher_ht", create(DeviceType.PUSHER_HT), r -> {
            r.shape("DPD");
            r.setIngredient('D', Material.DROPPER);
            r.setIngredient('P', Material.PISTON);
        });
        shaped(plugin, "encoder", create(DeviceType.ENCODER), r -> {
            r.shape("KPK", "PSP", "KPK");
            r.setIngredient('K', Material.INK_SAC);
            r.setIngredient('P', Material.PAPER);
            r.setIngredient('S', Material.SMITHING_TABLE);
        });
        shaped(plugin, "crafting_grid", create(DeviceType.CRAFTING_GRID), r -> {
            r.shape("CRC", "RGR", "CRC");
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('G', Material.CARTOGRAPHY_TABLE);
        });
        // Blueprint en blanco: el Encoder lo rellena con la receta de la matriz.
        shaped(plugin, "blueprint", stackOf(create(DeviceType.BLUEPRINT), 4), r -> {
            r.shape("PPP", "PBP", "PPP");
            r.setIngredient('P', Material.PAPER);
            r.setIngredient('B', Material.BLUE_DYE);
        });
        shaped(plugin, "configurator", create(DeviceType.CONFIGURATOR), r -> {
            r.shape("I I", " C ", " I ");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('C', Material.COMPARATOR);
        });
        shaped(plugin, "rake", rake(), r -> {
            r.shape("D D", " S ", " S ");
            r.setIngredient('D', Material.DEAD_BUSH);
            r.setIngredient('S', Material.STICK);
        });
        shaped(plugin, "crayon", create(DeviceType.CRAYON), r -> {
            r.shape("C", "S");
            r.setIngredient('C', Material.CYAN_DYE);
            r.setIngredient('S', Material.STICK);
        });
        shaped(plugin, "quantum_workbench", create(DeviceType.QUANTUM_WORKBENCH), r -> {
            r.shape("DDD", "DCD", "DDD");
            r.setIngredient('D', Material.DIAMOND);
            r.setIngredient('C', Material.CRAFTING_TABLE);
        });
        shaped(plugin, "infinity_barrel", create(DeviceType.INFINITY_BARREL), r -> {
            r.shape("NDN", "DBD", "NDN");
            r.setIngredient('N', Material.NETHERITE_INGOT);
            r.setIngredient('D', Material.DIAMOND_BLOCK);
            r.setIngredient('B', Material.BARREL);
        });
    }

    private interface RecipeDef {
        void define(ShapedRecipe recipe);
    }

    private static ShapedRecipe shaped(MultiverseNets plugin, String key, ItemStack result, RecipeDef def) {
        NamespacedKey nk = new NamespacedKey(plugin, key);
        ShapedRecipe recipe = new ShapedRecipe(nk, result);
        def.define(recipe);
        Bukkit.addRecipe(recipe);
        return recipe;
    }

    private static ItemStack stackOf(ItemStack item, int amount) {
        item.setAmount(amount);
        return item;
    }
}
