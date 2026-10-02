package com.chagui68.multiversenets.compat;

import com.chagui68.multiversenets.persist.NodeBlob;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * [EN] Reads the genes of a GeneticChickengineering pocket chicken straight from the item's data
 * container, so the Genetic Chicken Sorter works without a compile-time dependency on that addon.
 * The layout mirrors the addon: {@code gce_pocket_chicken_dna} is an int[7] (six gene states, 0 =
 * recessive aa, 1 = mixed Aa, 3 = dominant AA, plus a "DNA known" flag); the product comes from
 * which genes are dominant; {@code gce_expanded_species} marks the special tier 7-9 species.
 *
 * [ES] Lee los genes de un pollo de bolsillo de GeneticChickengineering directamente del
 * contenedor de datos del ítem, así el Genetic Chicken Sorter funciona sin depender del addon al
 * compilar. El formato sigue al addon: {@code gce_pocket_chicken_dna} es un int[7] (seis estados de
 * gen, 0 = recesivo aa, 1 = mixto Aa, 3 = dominante AA, más el indicador "ADN conocido"); el
 * producto sale de qué genes son dominantes; {@code gce_expanded_species} marca las especies
 * especiales de nivel 7-9.
 */
public final class ChickenGenetics {

    private static final String NAMESPACE = "geneticchickengineering";
    private static final NamespacedKey DNA = new NamespacedKey(NAMESPACE, "gce_pocket_chicken_dna");
    private static final NamespacedKey ADAPTER = new NamespacedKey(NAMESPACE, "gce_pocket_chicken_adapter");
    private static final NamespacedKey SPECIES = new NamespacedKey(NAMESPACE, "gce_expanded_species");

    /** Product of each typing (bit i set = gene i dominant, most significant bit = first gene). */
    private static final String[] PRODUCTS = new String[64];
    private static final Map<String, Integer> SPECIES_TIER = new HashMap<>();

    static {
        String[][] table = {
                {"63", "FEATHER"}, {"31", "BONE"}, {"47", "COBBLESTONE"}, {"55", "DIRT"}, {"59", "FLINT"},
                {"61", "SAND"}, {"62", "WATER"}, {"15", "COAL"}, {"23", "STRING"}, {"27", "LEATHER"},
                {"29", "SUGAR"}, {"30", "SPONGE"}, {"39", "DIORITE"}, {"43", "ANDESITE"}, {"45", "GRAVEL"},
                {"46", "ICE"}, {"51", "GRANITE"}, {"53", "CLAY"}, {"54", "OAK_LOG"}, {"57", "GUNPOWDER"},
                {"58", "KELP"}, {"60", "SLIME_BALL"}, {"7", "GOLD"}, {"11", "NETHERRACK"}, {"13", "GLASS"},
                {"14", "LAPIS"}, {"19", "IRON"}, {"21", "IRON_DUST"}, {"22", "GOLD_DUST"}, {"25", "SILVER_DUST"},
                {"26", "ZINC_DUST"}, {"28", "CAKE"}, {"35", "OBSIDIAN"}, {"37", "COPPER_DUST"},
                {"38", "MAGNESIUM_DUST"}, {"41", "LAVA"}, {"42", "TIN_DUST"}, {"44", "SNOWBALL"},
                {"49", "REDSTONE"}, {"50", "CACTUS"}, {"52", "ALUMINUM_DUST"}, {"56", "LEAD_DUST"},
                {"3", "BLACKSTONE"}, {"5", "SOUL_SOIL"}, {"9", "BLAZE_ROD"}, {"17", "GHAST_TEAR"},
                {"33", "SULFATE"}, {"6", "SHROOMLIGHT"}, {"10", "QUARTZ"}, {"18", "BASALT"},
                {"34", "CRYING_OBSIDIAN"}, {"12", "SOUL_SAND"}, {"20", "ENDER_PEARL"}, {"36", "NETHER_WART"},
                {"24", "PHANTOM_MEMBRANE"}, {"40", "MAGMA_CREAM"}, {"48", "GLOWSTONE_DUST"}, {"1", "DIAMOND"},
                {"2", "END_STONE"}, {"4", "PRISMARINE_CRYSTALS"}, {"8", "PRISMARINE_SHARD"},
                {"16", "EXPERIENCE"}, {"32", "EMERALD"}, {"0", "NETHERITE"}};
        for (String[] row : table) {
            PRODUCTS[Integer.parseInt(row[0])] = row[1];
        }
        for (String id : new String[]{"REINFORCED_ALLOY", "CARBONADO", "BLISTERING", "DAMASCUS_STEEL",
                "REDSTONE_ALLOY", "HARDENED_METAL", "CORINTHIAN_BRONZE", "GOLD_24K", "DURALUMIN", "BRONZE",
                "SYNTHETIC_DIAMOND"}) {
            SPECIES_TIER.put(id, 7);
        }
        for (String id : new String[]{"URANIUM", "NEPTUNIUM", "PLUTONIUM", "BOOSTED_URANIUM"}) {
            SPECIES_TIER.put(id, 8);
        }
        for (String id : new String[]{"NETHER_STAR", "DRAGON_BREATH", "ECHO_SHARD", "ANCIENT_DEBRIS", "SHULKER",
                "MAGIC_LUMP"}) {
            SPECIES_TIER.put(id, 9);
        }
    }

    private ChickenGenetics() {
    }

    /**
     * EN: Everything the sorter filters on. {@code product} is {@code TYPE:<typing>} or
     * {@code SPECIES:<id>}.
     * ES: Todo lo que filtra el clasificador. {@code product} es {@code TYPE:<tipo>} o
     * {@code SPECIES:<id>}.
     */
    public record Chicken(String product, int tier, int strength, boolean known, boolean pure, boolean adult) {
    }

    public static boolean isPocketChicken(ItemStack item) {
        return read(item) != null;
    }

    /** Genes of a pocket chicken, or null when the item is not one. */
    public static Chicken read(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        int[] state;
        try {
            state = pdc.get(DNA, PersistentDataType.INTEGER_ARRAY);
        } catch (IllegalArgumentException wrongType) {
            return null;
        }
        if (state == null || state.length < 7) {
            return null;
        }
        boolean adult = readAdult(pdc);
        String species = null;
        try {
            species = pdc.get(SPECIES, PersistentDataType.STRING);
        } catch (IllegalArgumentException ignored) {
        }
        if (species != null && !species.isBlank()) {
            String id = species.toUpperCase(Locale.ROOT);
            return new Chicken("SPECIES:" + id, SPECIES_TIER.getOrDefault(id, 7), 6, state[6] == 1, true, adult);
        }
        int typing = 0;
        int recessive = 0;
        int mixed = 0;
        for (int i = 0; i < 6; i++) {
            if (state[i] > 0) {
                typing += 1 << (5 - i);
            } else {
                recessive++;
            }
            if (state[i] == 1 || state[i] == 2) {
                mixed++;
            }
        }
        int strength = 6 - recessive - mixed;
        return new Chicken("TYPE:" + typing, recessive, strength, state[6] == 1, mixed == 0, adult);
    }

    private static boolean readAdult(PersistentDataContainer pdc) {
        try {
            String json = pdc.get(ADAPTER, PersistentDataType.STRING);
            if (json == null) {
                return true;
            }
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                return !(obj.has("baby") && obj.get("baby").getAsBoolean());
            }
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    /** Readable name of a product key: "Iron Dust", "Uranium (special)". */
    public static String productName(String key) {
        if (key == null) {
            return "?";
        }
        if (key.startsWith("TYPE:")) {
            try {
                int typing = Integer.parseInt(key.substring(5));
                if (typing >= 0 && typing < PRODUCTS.length && PRODUCTS[typing] != null) {
                    return pretty(PRODUCTS[typing]);
                }
            } catch (NumberFormatException ignored) {
            }
            return key;
        }
        if (key.startsWith("SPECIES:")) {
            return pretty(key.substring(8)) + " (special)";
        }
        return key;
    }

    private static String pretty(String id) {
        StringBuilder out = new StringBuilder();
        for (String part : id.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /**
     * EN: Does this chicken pass the sorter's rules? Non-chickens never do. Every rule left at its
     * default lets everything through, the product list included (empty = any product).
     *
     * ES: ¿Pasa este pollo las reglas del clasificador? Lo que no es un pollo nunca pasa. Cada regla
     * en su valor por defecto deja pasar todo, también la lista de productos (vacía = cualquiera).
     */
    public static boolean matches(NodeBlob rules, ItemStack item) {
        Chicken chicken = read(item);
        if (chicken == null || rules == null) {
            return false;
        }
        if (rules.chickenProducts != null && !rules.chickenProducts.isEmpty()
                && !rules.chickenProducts.contains(chicken.product())) {
            return false;
        }
        int min = rules.chickenMinTier == null ? 0 : rules.chickenMinTier;
        int max = rules.chickenMaxTier == null ? Integer.MAX_VALUE : rules.chickenMaxTier;
        if (chicken.tier() < min || chicken.tier() > max) {
            return false;
        }
        if (chicken.strength() < rules.chickenMinStrength) {
            return false;
        }
        if (rules.chickenPureOnly && !chicken.pure()) {
            return false;
        }
        if ("KNOWN".equals(rules.chickenKnown) && !chicken.known()) {
            return false;
        }
        if ("UNKNOWN".equals(rules.chickenKnown) && chicken.known()) {
            return false;
        }
        if ("ADULT".equals(rules.chickenAge) && !chicken.adult()) {
            return false;
        }
        return !"BABY".equals(rules.chickenAge) || !chicken.adult();
    }
}
