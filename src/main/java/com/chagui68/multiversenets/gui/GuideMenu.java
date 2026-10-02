package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.GuideContent;
import com.chagui68.multiversenets.item.GuideContent.Category;
import com.chagui68.multiversenets.item.GuideContent.Entry;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.util.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [EN] The in-game guide (replaces the old written book). Home → "How networks work" or a category →
 * a device page with its real 3x3 recipe, the result, what it does, how to use it and its numbers
 * from the config. Every page can switch between English and Spanish. Recipes are read from the
 * recipes the server registered, so a recipe disabled in the config shows as disabled.
 *
 * [ES] La guía del juego (sustituye al antiguo libro escrito). Inicio → "Cómo funcionan las redes" o
 * una categoría → la página de un dispositivo con su receta 3x3 real, el resultado, qué hace, cómo se
 * usa y sus números de la config. Cada página cambia entre inglés y español. Las recetas se leen de
 * las registradas en el servidor, así una receta desactivada en la config aparece como desactivada.
 */
public class GuideMenu extends MenuHolder {

    public static final int LANGUAGE_SLOT = 53;
    public static final int BACK_SLOT = 45;
    public static final int HOME_SLOT = 49;
    public static final int CLOSE_SLOT = 49;
    public static final int TOPICS_SLOT = 19;
    public static final int[] CATEGORY_SLOTS = {21, 22, 23, 24, 25, 30, 31, 32};
    public static final int[] LIST_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    public static final int[] TOPIC_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    public static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int TABLE_SLOT = 22;
    public static final int RESULT_SLOT = 24;
    public static final int DEVICE_SLOT = 4;
    public static final int WHAT_SLOT = 15;
    public static final int USE_SLOT = 16;
    public static final int STATS_SLOT = 33;
    public static final int INGREDIENTS_SLOT = 34;
    public static final int PREVIOUS_SLOT = 48;
    public static final int NEXT_SLOT = 50;

    private enum View { HOME, TOPICS, CATEGORY, DEVICE }

    private boolean spanish;
    private View view = View.HOME;
    private Category category;
    private DeviceType device;

    public GuideMenu(MultiverseNets plugin, Player player, boolean spanish) {
        super(plugin, player);
        this.spanish = spanish;
    }

    /** Opens the home page. */
    public void openMenu() {
        show(View.HOME, null, null);
    }

    /** Opens the page of one device directly. */
    public void openDevice(DeviceType type) {
        GuideContent.Entry entry = GuideContent.of(type);
        show(View.DEVICE, entry == null ? null : entry.category(), type);
    }

    public boolean isSpanish() {
        return spanish;
    }

    public DeviceType currentDevice() {
        return view == View.DEVICE ? device : null;
    }

    public Category currentCategory() {
        return view == View.CATEGORY || view == View.DEVICE ? category : null;
    }

    private void show(View next, Category nextCategory, DeviceType nextDevice) {
        view = next;
        category = nextCategory;
        device = nextDevice;
        open(54, Component.text(title(), NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
    }

    /** Page changes reopen the inventory (the title changes) one tick later, outside the click event. */
    private void navigate(View next, Category nextCategory, DeviceType nextDevice) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                show(next, nextCategory, nextDevice);
            }
        });
    }

    private String title() {
        String base = spanish ? "Guía" : "Guide";
        return switch (view) {
            case HOME -> "MultiverseNets · " + base;
            case TOPICS -> base + " · " + (spanish ? "Cómo funciona" : "How it works");
            case CATEGORY -> base + " · " + category.name(spanish);
            case DEVICE -> base + " · " + GuideContent.of(device).name(spanish);
        };
    }

    @Override
    protected void draw() {
        ItemStack background = icon(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }
        inv.setItem(LANGUAGE_SLOT, icon(Material.NAME_TAG, spanish ? "Idioma: Español" : "Language: English",
                NamedTextColor.AQUA, List.of(spanish ? "Clic: cambiar a inglés" : "Click: switch to Spanish")));
        switch (view) {
            case HOME -> drawHome();
            case TOPICS -> drawTopics();
            case CATEGORY -> drawCategory();
            case DEVICE -> drawDevice();
        }
    }

    // ---------------------------------------------------------------- pages

    private void drawHome() {
        inv.setItem(DEVICE_SLOT, icon(Material.KNOWLEDGE_BOOK, "MultiverseNets", NamedTextColor.GOLD, wrap(spanish
                ? "Logística digital y almacenamiento masivo. Elige un tema o una categoría: cada dispositivo "
                + "muestra su receta en la mesa de crafteo, qué hace y cómo se usa."
                : "Digital logistics and mass storage. Pick a topic or a category: every device shows its "
                + "crafting table recipe, what it does and how to use it.")));
        inv.setItem(TOPICS_SLOT, icon(Material.WRITABLE_BOOK, spanish ? "Cómo funcionan las redes" : "How networks work",
                NamedTextColor.YELLOW, List.of(spanish ? "Conceptos básicos, filtros, máquinas," : "Basics, filters, machines,",
                        spanish ? "seguridad y comandos." : "safety and commands.")));
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length && i < CATEGORY_SLOTS.length; i++) {
            Category c = categories[i];
            List<String> lore = new ArrayList<>(wrap(c.blurb(spanish)));
            lore.add("");
            lore.add((spanish ? "Dispositivos: " : "Devices: ") + GuideContent.in(c).size());
            inv.setItem(CATEGORY_SLOTS[i], icon(c.icon(), c.name(spanish), NamedTextColor.AQUA, lore));
        }
        inv.setItem(CLOSE_SLOT, icon(Material.BARRIER, spanish ? "Cerrar" : "Close", NamedTextColor.RED, List.of()));
    }

    private void drawTopics() {
        List<GuideContent.Topic> topics = GuideContent.topics();
        for (int i = 0; i < topics.size() && i < TOPIC_SLOTS.length; i++) {
            GuideContent.Topic topic = topics.get(i);
            inv.setItem(TOPIC_SLOTS[i], icon(topic.icon(), topic.title(spanish), NamedTextColor.YELLOW, wrap(topic.text(spanish))));
        }
        inv.setItem(BACK_SLOT, back());
    }

    private void drawCategory() {
        List<Entry> entries = GuideContent.in(category);
        inv.setItem(DEVICE_SLOT, icon(category.icon(), category.name(spanish), NamedTextColor.GOLD, wrap(category.blurb(spanish))));
        for (int i = 0; i < entries.size() && i < LIST_SLOTS.length; i++) {
            Entry entry = entries.get(i);
            ItemStack item = Items.create(entry.type());
            var meta = item.getItemMeta();
            meta.displayName(Component.text(entry.name(spanish), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            if (spanish) {
                lore.add(line("Ítem: " + entry.type().display(), NamedTextColor.DARK_GRAY));
            }
            for (String l : wrap(entry.what(spanish))) {
                lore.add(line(l, NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(line(spanish ? "Clic: receta y detalles" : "Click: recipe and details", NamedTextColor.YELLOW));
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(LIST_SLOTS[i], item);
        }
        inv.setItem(BACK_SLOT, back());
    }

    private void drawDevice() {
        Entry entry = GuideContent.of(device);
        ItemStack shown = Items.create(device);
        var meta = shown.getItemMeta();
        meta.displayName(Component.text(entry.name(spanish), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> headLore = new ArrayList<>();
        if (spanish) {
            headLore.add(line("Ítem: " + device.display(), NamedTextColor.DARK_GRAY));
        }
        headLore.add(line("id: " + device.id(), NamedTextColor.DARK_GRAY));
        headLore.add(line((spanish ? "Categoría: " : "Category: ") + entry.category().name(spanish), NamedTextColor.GRAY));
        meta.lore(headLore);
        shown.setItemMeta(meta);
        inv.setItem(DEVICE_SLOT, shown);

        drawRecipe();

        inv.setItem(WHAT_SLOT, icon(Material.BOOK, spanish ? "Qué hace" : "What it does", NamedTextColor.YELLOW, wrap(entry.what(spanish))));
        inv.setItem(USE_SLOT, icon(Material.WRITABLE_BOOK, spanish ? "Cómo se usa" : "How to use it", NamedTextColor.YELLOW, wrap(entry.use(spanish))));
        List<String> stats = stats(device);
        if (!stats.isEmpty()) {
            inv.setItem(STATS_SLOT, icon(Material.COMPARATOR, spanish ? "Números (config.yml)" : "Numbers (config.yml)",
                    NamedTextColor.YELLOW, stats));
        }

        List<Entry> siblings = GuideContent.in(category);
        int index = siblings.indexOf(entry);
        if (index > 0) {
            inv.setItem(PREVIOUS_SLOT, icon(Material.ARROW, spanish ? "Anterior" : "Previous", NamedTextColor.WHITE,
                    List.of(siblings.get(index - 1).name(spanish))));
        }
        if (index >= 0 && index < siblings.size() - 1) {
            inv.setItem(NEXT_SLOT, icon(Material.ARROW, spanish ? "Siguiente" : "Next", NamedTextColor.WHITE,
                    List.of(siblings.get(index + 1).name(spanish))));
        }
        inv.setItem(BACK_SLOT, back());
        inv.setItem(HOME_SLOT, icon(Material.KNOWLEDGE_BOOK, spanish ? "Inicio" : "Home", NamedTextColor.WHITE, List.of()));
    }

    private void drawRecipe() {
        NamespacedKey key = recipeKey(device);
        Recipe recipe = key == null ? null : Bukkit.getRecipe(key);
        if (!(recipe instanceof ShapedRecipe shaped)) {
            ItemStack none = icon(Material.BARRIER, spanish ? "Sin receta" : "No recipe", NamedTextColor.RED, wrap(spanish
                    ? "Esta receta está desactivada en config.yml (sf-encoder.enabled / sf-crafter.enabled) o el "
                    + "ítem no se craftea."
                    : "This recipe is disabled in config.yml (sf-encoder.enabled / sf-crafter.enabled) or the item "
                    + "is not crafted."));
            for (int slot : GRID_SLOTS) {
                inv.setItem(slot, none);
            }
            return;
        }
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, Component> names = new LinkedHashMap<>();
        String[] shape = shaped.getShape();
        Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = GRID_SLOTS[row * 3 + col];
                char c = row < shape.length && col < shape[row].length() ? shape[row].charAt(col) : ' ';
                RecipeChoice choice = choices.get(c);
                if (c == ' ' || choice == null) {
                    inv.setItem(slot, icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE, spanish ? "Vacío" : "Empty",
                            NamedTextColor.DARK_GRAY, List.of()));
                    continue;
                }
                DeviceType ingredientDevice = Items.deviceIngredientAt(key, c);
                ItemStack ingredient;
                String idKey;
                Component name;
                if (ingredientDevice != null) {
                    ingredient = Items.create(ingredientDevice);
                    Entry ingredientEntry = GuideContent.of(ingredientDevice);
                    String deviceName = ingredientEntry != null ? ingredientEntry.name(spanish) : ingredientDevice.display();
                    var meta = ingredient.getItemMeta();
                    meta.displayName(Component.text(deviceName, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(line(spanish ? "Tiene que ser este dispositivo, no el material."
                            : "Must be this device, not the plain material.", NamedTextColor.GRAY)));
                    ingredient.setItemMeta(meta);
                    idKey = "device:" + ingredientDevice.name();
                    name = Component.text(deviceName);
                } else if (choice instanceof RecipeChoice.MaterialChoice materials && !materials.getChoices().isEmpty()) {
                    Material material = materials.getChoices().get(0);
                    ingredient = new ItemStack(material);
                    idKey = material.name();
                    name = Component.translatable(material.translationKey());
                } else if (choice instanceof RecipeChoice.ExactChoice exact && !exact.getChoices().isEmpty()) {
                    ingredient = exact.getChoices().get(0).clone();
                    idKey = ingredient.getType().name();
                    name = Component.translatable(ingredient.getType().translationKey());
                } else {
                    continue;
                }
                inv.setItem(slot, ingredient);
                totals.merge(idKey, 1, Integer::sum);
                names.putIfAbsent(idKey, name);
            }
        }
        inv.setItem(TABLE_SLOT, icon(Material.CRAFTING_TABLE, spanish ? "Mesa de crafteo  →" : "Crafting table  →",
                NamedTextColor.WHITE, List.of(spanish ? "Coloca los ingredientes así." : "Place the ingredients like this.")));
        ItemStack result = shaped.getResult().clone();
        inv.setItem(RESULT_SLOT, result);

        ItemStack list = new ItemStack(Material.PAPER);
        var meta = list.getItemMeta();
        meta.displayName(Component.text(spanish ? "Ingredientes" : "Ingredients", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (var e : totals.entrySet()) {
            lore.add(Component.text(e.getValue() + "x ", NamedTextColor.GRAY).append(names.get(e.getKey())
                    .color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(line((spanish ? "Resultado: " : "Result: ") + result.getAmount() + "x", NamedTextColor.GRAY));
        meta.lore(lore);
        list.setItemMeta(meta);
        inv.setItem(INGREDIENTS_SLOT, list);
    }

    /** The registered recipe whose result is {@code type}, or null. */
    public static NamespacedKey recipeKey(DeviceType type) {
        for (NamespacedKey key : Items.recipeKeys()) {
            Recipe recipe = Bukkit.getRecipe(key);
            if (recipe != null && Items.typeOf(recipe.getResult()) == type) {
                return key;
            }
        }
        return null;
    }

    private List<String> stats(DeviceType type) {
        List<String> out = new ArrayList<>();
        String cap = spanish ? "Capacidad: " : "Capacity: ";
        String items = spanish ? " ítems" : " items";
        if (type.isCell() || type == DeviceType.MVN_GREEDY_CELL || type == DeviceType.MVN_INFINITY_BARREL || type.isCacheModule()) {
            out.add(cap + Items.formatAmount(Items.capacityOf(type)) + items);
        }
        switch (type) {
            case MVN_FLUID_CELL -> out.add(cap + Items.formatAmount(Settings.fluidCellCapacity()) + " mB");
            case MVN_FLUID_DRAM -> out.add(cap + Items.formatAmount(Settings.fluidDramCapacity()) + " mB");
            case MVN_GRABBER, MVN_PUSHER, MVN_PURGER, MVN_TRANSMITTER, MVN_RECEIVER ->
                    out.add((spanish ? "Por ciclo: " : "Per cycle: ") + Settings.itemsPerOp() + items);
            case MVN_GRABBER_HT, MVN_PUSHER_HT ->
                    out.add((spanish ? "Por ciclo: " : "Per cycle: ") + Settings.itemsPerOp() * Settings.htMultiplier() + items);
            case MVN_VACUUM -> out.add((spanish ? "Radio: " : "Radius: ") + Settings.vacuumRadius());
            case MVN_CRAFTER, MVN_SF_CRAFTER, MVN_REQUEST_CRAFTER, MVN_SF_REQUEST_CRAFTER ->
                    out.add((spanish ? "Blueprints máx.: " : "Max Blueprints: ") + Settings.maxBlueprints());
            case MVN_RAKE -> out.add((spanish ? "Usos: " : "Uses: ") + Settings.rakeUses());
            case MVN_WIRELESS_TERMINAL -> out.add((spanish ? "Alcance sin Router: " : "Range without Router: ")
                    + Settings.wirelessLocalRange());
            default -> {
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- clicks

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= inv.getSize()) {
            return;
        }
        if (raw == LANGUAGE_SLOT) {
            spanish = !spanish;
            navigate(view, category, device);
            return;
        }
        switch (view) {
            case HOME -> {
                if (raw == CLOSE_SLOT) {
                    player.closeInventory();
                } else if (raw == TOPICS_SLOT) {
                    navigate(View.TOPICS, null, null);
                } else {
                    for (int i = 0; i < CATEGORY_SLOTS.length && i < Category.values().length; i++) {
                        if (CATEGORY_SLOTS[i] == raw) {
                            navigate(View.CATEGORY, Category.values()[i], null);
                        }
                    }
                }
            }
            case TOPICS -> {
                if (raw == BACK_SLOT) {
                    navigate(View.HOME, null, null);
                }
            }
            case CATEGORY -> {
                if (raw == BACK_SLOT) {
                    navigate(View.HOME, null, null);
                    return;
                }
                List<Entry> entries = GuideContent.in(category);
                for (int i = 0; i < entries.size() && i < LIST_SLOTS.length; i++) {
                    if (LIST_SLOTS[i] == raw) {
                        navigate(View.DEVICE, category, entries.get(i).type());
                    }
                }
            }
            case DEVICE -> {
                if (raw == BACK_SLOT) {
                    navigate(View.CATEGORY, category, null);
                } else if (raw == HOME_SLOT) {
                    navigate(View.HOME, null, null);
                } else if (raw == PREVIOUS_SLOT || raw == NEXT_SLOT) {
                    List<Entry> siblings = GuideContent.in(category);
                    int index = siblings.indexOf(GuideContent.of(device)) + (raw == NEXT_SLOT ? 1 : -1);
                    if (index >= 0 && index < siblings.size()) {
                        navigate(View.DEVICE, category, siblings.get(index).type());
                    }
                } else {
                    // Clicking an ingredient that is itself a device opens its page.
                    DeviceType clicked = Items.typeOf(event.getCurrentItem());
                    if (clicked != null && clicked != device && GuideContent.of(clicked) != null) {
                        navigate(View.DEVICE, GuideContent.of(clicked).category(), clicked);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private ItemStack back() {
        return icon(Material.ARROW, spanish ? "Volver" : "Back", NamedTextColor.WHITE, List.of());
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack icon(Material material, String name, NamedTextColor color, List<String> lore) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        List<Component> lines = new ArrayList<>();
        for (String l : lore) {
            lines.add(l.isEmpty() ? Component.empty() : line(l, NamedTextColor.GRAY));
        }
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }

    /** Splits a paragraph into tooltip lines of about 40 characters. */
    static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > 40) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
