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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Factory and registry for plugin items, custom tools, PDC metadata, and crafting recipes.
 *
 * Fábrica y registro de ítems del plugin, herramientas personalizadas, metadatos PDC y recetas de crafteo.
 */
public final class Items {

    private Items() {
    }

    /**
     * Creates a newly instantiated ItemStack for a device type with standard metadata and lore.
 *
     * Crea un nuevo ItemStack para un tipo de dispositivo con metadatos y lore estándar.
     *
     * @param type Device type / Tipo de dispositivo
     * @return Prepared ItemStack / ItemStack preparado
     */
    public static ItemStack create(DeviceType type) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.display(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("MultiverseNets", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (type.isCell() || type == DeviceType.MVN_INFINITY_BARREL || type == DeviceType.MVN_GREEDY_CELL) {
            lore.add(Component.text("Capacity: " + formatAmount(capacityOf(type)), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (type == DeviceType.MVN_WIRELESS_TERMINAL) {
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                    .append(Component.text("Unbound", NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Shift+Right Click a Controller or Terminal", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("to bind to a network.", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Resolves the DeviceType of an ItemStack from its PersistentDataContainer.
 *
     * Resuelve el DeviceType de un ItemStack desde su PersistentDataContainer.
     *
     * @param item Target ItemStack / ItemStack objetivo
     * @return Resolved DeviceType or null / DeviceType resuelto o null
     */
    public static DeviceType typeOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String name = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.DEVICE_TYPE, PersistentDataType.STRING);
        return name == null ? null : DeviceType.parse(name);
    }

    // ---------------------------------------------------------------- Tools / Herramientas

    /**
     * Creates a fresh network rake tool with configured durability uses.
 *
     * Crea una herramienta de rastrillo de red nueva con los usos de durabilidad configurados.
     *
     * @return Rake ItemStack / ItemStack del rastrillo
     */
    public static ItemStack rake() {
        ItemStack item = create(DeviceType.MVN_RAKE);
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

    /**
     * Retrieves remaining uses on a rake tool item.
 *
     * Obtiene los usos restantes de un rastrillo.
     *
     * @param item Rake ItemStack / ItemStack del rastrillo
     * @return Remaining uses / Usos restantes
     */
    public static int rakeUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 0;
        }
        Integer uses = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.RAKE_USES, PersistentDataType.INTEGER);
        return uses == null ? 0 : uses;
    }

    /**
     * Decrements a use from a rake tool. Returns false if durability is depleted and the item should break.
 *
     * Gasta un uso de un rastrillo. Devuelve false cuando la herramienta se agota y debe romperse.
     *
     * @param item Rake ItemStack / ItemStack del rastrillo
     * @return true if uses remain, false if broken / true si quedan usos, false si se rompió
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

    /**
     * Saves copied filter settings onto a configurator wrench item ("WL:MAT1,MAT2" or "BL:MAT1,MAT2").
 *
     * Guarda la configuración de filtros copiada en una llave de configuración ("WL:MAT1,MAT2" o "BL:MAT1,MAT2").
     *
     * @param item Configurator ItemStack / ItemStack de la llave
     * @param mats List of material names / Lista de nombres de materiales
     * @param blacklist true if blacklist mode, false if whitelist / true si es lista negra, false si es blanca
     */
    public static void saveConfig(ItemStack item, java.util.List<String> mats, boolean blacklist) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CONFIG_DATA, PersistentDataType.STRING,
                (blacklist ? "BL:" : "WL:") + String.join(",", mats));
        meta.lore(java.util.List.of(
                Component.text((blacklist ? "Blacklist: " : "Whitelist: ")
                                + (mats.isEmpty() ? "(empty)" : String.join(", ", mats)),
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
    }

    /**
     * Reads copied filter configuration from a configurator wrench item.
     * Returns array [mats..., "bl"/"wl"] or null if empty.
 *
     * Lee la configuración de filtros de una llave.
     * Devuelve el array [mats..., "bl"/"wl"] o null si está vacía.
     *
     * @param item Configurator ItemStack / ItemStack de la llave
     * @return Parsed configuration array or null / Array de configuración parseado o null
     */
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

    /**
     * Creates an encoded recipe blueprint item.
 *
     * Crea un ítem de plano de receta codificado.
     *
     * @param recipeKey Namespaced key string / Clave de receta namespaced
     * @param resultName Display name of recipe output / Nombre legible del resultado de la receta
     * @return Encoded blueprint ItemStack / ItemStack del plano codificado
     */
    public static ItemStack blueprint(String recipeKey, String resultName) {
        ItemStack item = new ItemStack(DeviceType.MVN_BLUEPRINT.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Blueprint: " + resultName, NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Recipe: " + recipeKey, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Click an Auto-Crafter to install", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(Keys.DEVICE_TYPE, PersistentDataType.STRING, DeviceType.MVN_BLUEPRINT.name());
        meta.getPersistentDataContainer().set(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING, recipeKey);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * @param item Target item / Ítem objetivo
     * @return true if item is an encoded blueprint / true si el ítem es un plano codificado
     */
    public static boolean isBlueprint(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
    }

    /**
     * @param item Target item / Ítem objetivo
     * @return Encoded recipe key or null / Clave de receta codificada o null
     */
    public static String readBlueprint(ItemStack item) {
        if (!isBlueprint(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BLUEPRINT_RECIPE, PersistentDataType.STRING);
    }

    /**
     * Links a receiver item to a transmitter block location.
 *
     * Vincula un ítem receptor a la ubicación del bloque transmisor.
     *
     * @param item Receiver ItemStack / ItemStack del receptor
     * @param transmitter Location of target transmitter / Ubicación del transmisor objetivo
     */
    public static void linkReceiver(ItemStack item, Location transmitter) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.RECEIVER_BIND, PersistentDataType.STRING,
                transmitter.getWorld().getUID() + ";" + transmitter.getBlockX() + ";"
                        + transmitter.getBlockY() + ";" + transmitter.getBlockZ());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("MultiverseNets", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("Linked", NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Transmitter: ", NamedTextColor.GRAY)
                .append(Component.text(transmitter.getBlockX() + ", " + transmitter.getBlockY() + ", " + transmitter.getBlockZ(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        if (transmitter.getWorld() != null) {
            lore.add(Component.text("World: ", NamedTextColor.GRAY)
                    .append(Component.text(transmitter.getWorld().getName(), NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Reads bound transmitter location from a receiver item.
 *
     * Lee la ubicación del transmisor vinculado desde un ítem receptor.
     *
     * @param item Receiver ItemStack / ItemStack del receptor
     * @return Bound transmitter Location or null / Ubicación del transmisor vinculado o null
     */
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

    /**
     * Binds a wireless terminal item to a controller/terminal block location.
 *
     * Vincula una terminal inalámbrica a la ubicación de un controlador o terminal.
     *
     * @param item Wireless Terminal ItemStack / ItemStack de la terminal inalámbrica
     * @param loc Target Controller/Terminal Location / Ubicación del controlador/terminal objetivo
     */
    public static void bindWireless(ItemStack item, Location loc) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.WIRELESS_BIND, PersistentDataType.STRING,
                loc.getWorld().getUID() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("MultiverseNets", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                .append(Component.text("Linked", NamedTextColor.GREEN))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Controller: ", NamedTextColor.GRAY)
                .append(Component.text(loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.YELLOW))
                .decoration(TextDecoration.ITALIC, false));
        if (loc.getWorld() != null) {
            lore.add(Component.text("World: ", NamedTextColor.GRAY)
                    .append(Component.text(loc.getWorld().getName(), NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Right click to open terminal", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Reads bound controller location from a wireless terminal item.
 *
     * Lee la ubicación del controlador vinculado desde una terminal inalámbrica.
     *
     * @param item Wireless Terminal ItemStack / ItemStack de la terminal inalámbrica
     * @return Bound Location or null / Ubicación vinculada o null
     */
    public static Location readWirelessBind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String data = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.WIRELESS_BIND, PersistentDataType.STRING);
        return parseLocation(data);
    }

    /**
     * Retrieves storage capacity for a given device type.
 *
     * Obtiene la capacidad de almacenamiento para un tipo de dispositivo dado.
     *
     * @param type Target device type / Tipo de dispositivo objetivo
     * @return Total capacity in items / Capacidad total en ítems
     */
    public static long capacityOf(DeviceType type) {
        if (type == DeviceType.MVN_INFINITY_BARREL) {
            return com.chagui68.multiversenets.util.Settings.barrelCapacity();
        }
        if (type == DeviceType.MVN_GREEDY_CELL) {
            return com.chagui68.multiversenets.util.Settings.greedyCapacity();
        }
        return com.chagui68.multiversenets.util.Settings.cellCapacity(type.cellTier());
    }

    /**
     * Formats large item counts into human-readable shorthand strings (e.g. 1.5k, 2.3M, 1B).
 *
     * Formatea grandes cantidades de ítems en texto abreviado legible (ej. 1.5k, 2.3M, 1B).
     *
     * @param amount Item count / Cantidad de ítems
     * @return Formatted string / Cadena formateada
     */
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

    /**
     * Registers all plugin crafting recipes with the Bukkit server recipe manager.
 *
     * Registra todas las recetas de crafteo del plugin en el gestor de recetas del servidor Bukkit.
     *
     * @param plugin Main plugin instance / Instancia principal del plugin
     */
    public static synchronized void registerRecipes(MultiverseNets plugin) {
        synchronized (RECIPE_KEYS) {
            RECIPE_KEYS.clear();
        }
        shaped(plugin, "controller", create(DeviceType.MVN_CONTROLLER), r -> {
            r.shape("III", "INI", "III");
            r.setIngredient('I', Material.IRON_BLOCK);
            r.setIngredient('N', Material.NETHER_STAR);
        });
        shaped(plugin, "cable", stackOf(create(DeviceType.MVN_CABLE), 16), r -> {
            r.shape("GGG", "GRG", "GGG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('R', Material.REDSTONE);
        });
        shaped(plugin, "terminal", create(DeviceType.MVN_TERMINAL), r -> {
            r.shape("GEG", "EBE", "GEG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('E', Material.ENDER_PEARL);
            r.setIngredient('B', Material.BEACON);
        });
        shaped(plugin, "cell_t1", create(DeviceType.MVN_CELL_T1), r -> {
            r.shape("GGG", "GDG", "GGG");
            r.setIngredient('G', Material.GLASS);
            r.setIngredient('D', Material.DIAMOND);
        });
        for (int tier = 2; tier <= 6; tier++) {
            DeviceType prev = DeviceType.parse("MVN_CELL_T" + (tier - 1));
            DeviceType cur = DeviceType.parse("MVN_CELL_T" + tier);
            final DeviceType prevFinal = prev;
            shaped(plugin, "cell_t" + tier, create(cur), r -> {
                r.shape("DDD", "DPD", "DDD");
                r.setIngredient('D', Material.DIAMOND);
                r.setIngredient('P', new org.bukkit.inventory.RecipeChoice.ExactChoice(create(prevFinal)));
            });
        }
        shaped(plugin, "grabber", create(DeviceType.MVN_GRABBER), r -> {
            r.shape("IOI", "ORO", "IOI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('O', Material.OBSERVER);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
        });
        shaped(plugin, "pusher", create(DeviceType.MVN_PUSHER), r -> {
            r.shape("IDI", "DRD", "IDI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('D', Material.DROPPER);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
        });
        shaped(plugin, "vacuum", create(DeviceType.MVN_VACUUM), r -> {
            r.shape("SRS", "RHR", "SRS");
            r.setIngredient('S', Material.STRING);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('H', Material.HOPPER);
        });
        shaped(plugin, "purger", create(DeviceType.MVN_PURGER), r -> {
            r.shape("ILI", "LHL", "ILI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('L', Material.MAGMA_BLOCK);
            r.setIngredient('H', Material.HOPPER);
        });
        shaped(plugin, "probe", create(DeviceType.MVN_PROBE), r -> {
            r.shape(" A ", "ASA", " A ");
            r.setIngredient('A', Material.AMETHYST_SHARD);
            r.setIngredient('S', Material.SPYGLASS);
        });
        shaped(plugin, "crafter", create(DeviceType.MVN_CRAFTER), r -> {
            r.shape("RCR", "ITI", "RCR");
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('T', Material.TARGET);
        });
        shaped(plugin, "wireless_terminal", create(DeviceType.MVN_WIRELESS_TERMINAL), r -> {
            r.shape(" P ", "PNP", " C ");
            r.setIngredient('P', Material.ENDER_PEARL);
            r.setIngredient('N', Material.NETHER_STAR);
            r.setIngredient('C', Material.COMPASS);
        });
        shaped(plugin, "monitor", create(DeviceType.MVN_MONITOR), r -> {
            r.shape("GGG", "GCG", "GGG");
            r.setIngredient('G', Material.GLASS_PANE);
            r.setIngredient('C', Material.COMPARATOR);
        });
        shaped(plugin, "transmitter", create(DeviceType.MVN_TRANSMITTER), r -> {
            r.shape("IRI", "RCR", "IRI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('R', Material.REDSTONE_BLOCK);
            r.setIngredient('C', Material.CONDUIT);
        });
        shaped(plugin, "receiver", create(DeviceType.MVN_RECEIVER), r -> {
            r.shape("IPI", "PLP", "IPI");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('P', Material.ENDER_PEARL);
            r.setIngredient('L', Material.REDSTONE_LAMP);
        });
        shaped(plugin, "greedy_cell", create(DeviceType.MVN_GREEDY_CELL), r -> {
            r.shape("GHG", "HSH", "GHG");
            r.setIngredient('G', Material.GOLD_INGOT);
            r.setIngredient('H', Material.HOPPER);
            r.setIngredient('S', Material.SLIME_BLOCK);
        });
        shaped(plugin, "grabber_ht", create(DeviceType.MVN_GRABBER_HT), r -> {
            r.shape("OPO");
            r.setIngredient('O', Material.OBSERVER);
            r.setIngredient('P', Material.STICKY_PISTON);
        });
        shaped(plugin, "pusher_ht", create(DeviceType.MVN_PUSHER_HT), r -> {
            r.shape("DPD");
            r.setIngredient('D', Material.DROPPER);
            r.setIngredient('P', Material.PISTON);
        });
        shaped(plugin, "encoder", create(DeviceType.MVN_ENCODER), r -> {
            r.shape("KPK", "PSP", "KPK");
            r.setIngredient('K', Material.INK_SAC);
            r.setIngredient('P', Material.PAPER);
            r.setIngredient('S', Material.SMITHING_TABLE);
        });
        shaped(plugin, "crafting_grid", create(DeviceType.MVN_CRAFTING_GRID), r -> {
            r.shape("CRC", "RGR", "CRC");
            r.setIngredient('C', Material.CRAFTING_TABLE);
            r.setIngredient('R', Material.REDSTONE);
            r.setIngredient('G', Material.CARTOGRAPHY_TABLE);
        });
        // Blueprint en blanco: el Encoder lo rellena con la receta de la matriz.
        shaped(plugin, "blueprint", stackOf(create(DeviceType.MVN_BLUEPRINT), 4), r -> {
            r.shape("PPP", "PBP", "PPP");
            r.setIngredient('P', Material.PAPER);
            r.setIngredient('B', Material.BLUE_DYE);
        });
        shaped(plugin, "configurator", create(DeviceType.MVN_CONFIGURATOR), r -> {
            r.shape("I I", " C ", " I ");
            r.setIngredient('I', Material.IRON_INGOT);
            r.setIngredient('C', Material.COMPARATOR);
        });
        shaped(plugin, "rake", rake(), r -> {
            r.shape("D D", " S ", " S ");
            r.setIngredient('D', Material.DEAD_BUSH);
            r.setIngredient('S', Material.STICK);
        });
        shaped(plugin, "crayon", create(DeviceType.MVN_CRAYON), r -> {
            r.shape("C", "S");
            r.setIngredient('C', Material.CYAN_DYE);
            r.setIngredient('S', Material.STICK);
        });
        shaped(plugin, "quantum_workbench", create(DeviceType.MVN_QUANTUM_WORKBENCH), r -> {
            r.shape("DDD", "DCD", "DDD");
            r.setIngredient('D', Material.DIAMOND);
            r.setIngredient('C', Material.CRAFTING_TABLE);
        });
        shaped(plugin, "infinity_barrel", create(DeviceType.MVN_INFINITY_BARREL), r -> {
            r.shape("NDN", "DBD", "NDN");
            r.setIngredient('N', Material.NETHERITE_INGOT);
            r.setIngredient('D', Material.DIAMOND_BLOCK);
            r.setIngredient('B', Material.BARREL);
        });
        plugin.getLogger().info("Registered " + recipeCount() + " crafting recipes with Bukkit.");
    }

    private static final List<NamespacedKey> RECIPE_KEYS = new ArrayList<>();

    /**
     * @return Total count of registered recipe keys.
     */
    public static int recipeCount() {
        synchronized (RECIPE_KEYS) {
            return RECIPE_KEYS.size();
        }
    }

    /**
     * @return Unmodifiable snapshot of registered recipe keys.
     */
    public static List<NamespacedKey> recipeKeys() {
        synchronized (RECIPE_KEYS) {
            return Collections.unmodifiableList(new ArrayList<>(RECIPE_KEYS));
        }
    }

    /**
     * Unlocks all MultiverseNets recipes in the player's recipe book.
     */
    public static void discoverRecipes(Player player) {
        if (player == null) {
            return;
        }
        List<NamespacedKey> keys = recipeKeys();
        for (NamespacedKey key : keys) {
            try {
                if (!player.hasDiscoveredRecipe(key)) {
                    player.discoverRecipe(key);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private interface RecipeDef {
        void define(ShapedRecipe recipe);
    }

    private static ShapedRecipe shaped(MultiverseNets plugin, String key, ItemStack result, RecipeDef def) {
        NamespacedKey nk = new NamespacedKey(plugin, key);
        try {
            Bukkit.removeRecipe(nk);
        } catch (Throwable ignored) {
        }
        ShapedRecipe recipe = new ShapedRecipe(nk, result);
        def.define(recipe);
        try {
            boolean ok = Bukkit.addRecipe(recipe);
            if (!ok) {
                plugin.getLogger().warning("Bukkit.addRecipe returned false for: " + key);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register recipe " + key + ": " + t.getMessage());
        }
        synchronized (RECIPE_KEYS) {
            if (!RECIPE_KEYS.contains(nk)) {
                RECIPE_KEYS.add(nk);
            }
        }
        return recipe;
    }

    private static ItemStack stackOf(ItemStack item, int amount) {
        item.setAmount(amount);
        return item;
    }
}
