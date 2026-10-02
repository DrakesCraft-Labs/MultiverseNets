package com.chagui68.multiversenets.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;

import java.util.List;
import java.util.Objects;

/**
 * [EN] Deep ItemStack Comparison Engine
 * Port of the specialized {@code StackUtils} matching engine from NetworksV6.
 *
 * Standard {@code ItemStack.isSimilar()} delegates to {@code ItemMeta.equals()}, and
 * {@code ItemStack.hashCode()} in Bukkit can be unstable across identical items (Networks bug #226).
 * This utility compares item components in order from cheapest to most expensive (material -> meta presence
 * -> custom model data -> PDC tags -> enchantments -> flags -> lore -> display name -> custom meta types).
 *
 * [ES] Motor de Comparación Profunda de ItemStacks
 * Motor de comparación profunda adaptado de NetworksV6 para evitar fallos de {@code hashCode} de Bukkit.
 * Compara metadatos de menor a mayor coste computacional y garantiza igualdad exacta de ítems.
 */
public final class StackUtils {

    private StackUtils() {
    }

    /**
     * EN: Deeply checks if two ItemStacks are identical (including lore and custom meta).
 *
     * ES: Comprueba si dos ItemStacks son idénticos en material y metadatos (incluyendo lore).
     */
    public static boolean itemsMatch(ItemStack a, ItemStack b) {
        return itemsMatch(a, b, true);
    }

    /**
     * EN: Deeply checks if two ItemStacks match, with optional lore verification.
 *
     * ES: Comprueba si dos ItemStacks coinciden, con verificación opcional de lore.
     */
    public static boolean itemsMatch(ItemStack a, ItemStack b, boolean checkLore) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getType() != b.getType()) {
            return false;
        }
        return strictMatch(a, b, checkLore) || sameCustomItem(a, b, checkLore);
    }

    /**
     * EN: Items from Slimefun and other plugins are identified by their PersistentDataContainer
     * (the item id lives there). Saving one inside a node and loading it back can change how its
     * name and lore are stored (a component with explicit styles instead of legacy text) or drop a
     * hide flag, so the stored sample no longer matched the very item it came from: an Infinity
     * Barrel refused to take back what it had just handed out. Two items with the same non-empty
     * data container, enchantments, damage and visible text are the same item.
     *
     * ES: Los ítems de Slimefun y de otros plugins se identifican por su PersistentDataContainer
     * (ahí vive su id). Guardar uno en un nodo y volver a leerlo puede cambiar cómo se guardan su
     * nombre y su lore (un componente con estilos explícitos en vez de texto legacy) o perder un
     * flag de ocultar, así que la muestra guardada dejaba de coincidir con el propio ítem: un
     * Infinity Barrel rechazaba lo que acababa de entregar. Dos ítems con el mismo contenedor de
     * datos no vacío, encantamientos, daño y texto visible son el mismo ítem.
     */
    private static boolean sameCustomItem(ItemStack a, ItemStack b, boolean checkLore) {
        if (!a.hasItemMeta() || !b.hasItemMeta()) {
            return false;
        }
        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();
        if (am == null || bm == null) {
            return false;
        }
        var apdc = am.getPersistentDataContainer();
        var bpdc = bm.getPersistentDataContainer();
        if (apdc.isEmpty() || bpdc.isEmpty() || !pdcMatches(apdc, bpdc)) {
            return false;
        }
        if (!am.getEnchants().equals(bm.getEnchants())) {
            return false;
        }
        if (am instanceof Damageable ad && bm instanceof Damageable bd && ad.getDamage() != bd.getDamage()) {
            return false;
        }
        if (!plain(am.hasDisplayName() ? am.displayName() : null)
                .equals(plain(bm.hasDisplayName() ? bm.displayName() : null))) {
            return false;
        }
        if (checkLore) {
            List<net.kyori.adventure.text.Component> al = am.hasLore() ? am.lore() : null;
            List<net.kyori.adventure.text.Component> bl = bm.hasLore() ? bm.lore() : null;
            int an = al == null ? 0 : al.size();
            int bn = bl == null ? 0 : bl.size();
            if (an != bn) {
                return false;
            }
            for (int i = 0; i < an; i++) {
                if (!plain(al.get(i)).equals(plain(bl.get(i)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String plain(net.kyori.adventure.text.Component component) {
        if (component == null) {
            return "";
        }
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static boolean strictMatch(ItemStack a, ItemStack b, boolean checkLore) {
        boolean aMeta = a.hasItemMeta();
        boolean bMeta = b.hasItemMeta();
        if (aMeta != bMeta) {
            return false;
        }
        if (!aMeta) {
            return true;
        }
        ItemMeta am = a.getItemMeta();
        ItemMeta bm = b.getItemMeta();
        if (am == null || bm == null) {
            return am == bm;
        }
        if (!am.getClass().equals(bm.getClass())) {
            return false;
        }
        if (!quickSubtypeMatch(am, bm)) {
            return false;
        }
        if (am.hasCustomModelData() != bm.hasCustomModelData()) {
            return false;
        }
        if (am.hasCustomModelData() && am.getCustomModelData() != bm.getCustomModelData()) {
            return false;
        }
        if (!pdcMatches(am.getPersistentDataContainer(), bm.getPersistentDataContainer())) {
            return false;
        }
        if (!am.getEnchants().equals(bm.getEnchants())) {
            return false;
        }
        if (!am.getItemFlags().equals(bm.getItemFlags())) {
            return false;
        }
        if (checkLore && !Objects.equals(am.lore(), bm.lore())) {
            return false;
        }
        return Objects.equals(am.displayName(), bm.displayName());
    }

    /** Comparaciones especificas por subtipo de meta. Cualsquiera que falle descarta. */
    private static boolean quickSubtypeMatch(ItemMeta am, ItemMeta bm) {
        if (am instanceof Damageable ad && bm instanceof Damageable bd
                && ad.getDamage() != bd.getDamage()) {
            return false;
        }
        if (am instanceof PotionMeta ap && bm instanceof PotionMeta bp) {
            if (ap.getBasePotionType() != bp.getBasePotionType()
                    || ap.hasCustomEffects() != bp.hasCustomEffects()
                    || (ap.hasCustomEffects() && !ap.getCustomEffects().equals(bp.getCustomEffects()))
                    || ap.hasColor() != bp.hasColor()
                    || (ap.hasColor() && !Objects.equals(ap.getColor(), bp.getColor()))) {
                return false;
            }
        }
        if (am instanceof SkullMeta as && bm instanceof SkullMeta bs) {
            if (as.hasOwner() != bs.hasOwner()
                    || (as.hasOwner() && !Objects.equals(as.getOwningPlayer(), bs.getOwningPlayer()))) {
                return false;
            }
        }
        if (am instanceof BundleMeta ab && bm instanceof BundleMeta bb) {
            if (ab.hasItems() != bb.hasItems()
                    || (ab.hasItems() && !ab.getItems().equals(bb.getItems()))) {
                return false;
            }
        }
        if (am instanceof EnchantmentStorageMeta ae && bm instanceof EnchantmentStorageMeta be) {
            if (ae.hasStoredEnchants() != be.hasStoredEnchants()
                    || (ae.hasStoredEnchants() && !ae.getStoredEnchants().equals(be.getStoredEnchants()))) {
                return false;
            }
        }
        if (am instanceof BookMeta abk && bm instanceof BookMeta bbk) {
            if (abk.getPageCount() != bbk.getPageCount()
                    || !Objects.equals(abk.getAuthor(), bbk.getAuthor())
                    || !Objects.equals(abk.getTitle(), bbk.getTitle())) {
                return false;
            }
        }
        if (am instanceof FireworkMeta af && bm instanceof FireworkMeta bf) {
            if (af.getPower() != bf.getPower()
                    || !Objects.equals(af.getEffects(), bf.getEffects())) {
                return false;
            }
        }
        if (am instanceof LeatherArmorMeta al && bm instanceof LeatherArmorMeta bl
                && !Objects.equals(al.getColor(), bl.getColor())) {
            return false;
        }
        if (am instanceof CompassMeta ac && bm instanceof CompassMeta bc) {
            if (ac.isLodestoneTracked() != bc.isLodestoneTracked()
                    || !Objects.equals(ac.getLodestone(), bc.getLodestone())) {
                return false;
            }
        }
        if (am instanceof CrossbowMeta ack && bm instanceof CrossbowMeta bck) {
            if (ack.hasChargedProjectiles() != bck.hasChargedProjectiles()
                    || (ack.hasChargedProjectiles()
                    && !ack.getChargedProjectiles().equals(bck.getChargedProjectiles()))) {
                return false;
            }
        }
        if (am instanceof MapMeta amp && bm instanceof MapMeta bmp) {
            if (amp.hasMapView() != bmp.hasMapView()
                    || amp.hasColor() != bmp.hasColor()
                    || (amp.hasColor() && !Objects.equals(amp.getColor(), bmp.getColor()))) {
                return false;
            }
        }
        if (am instanceof AxolotlBucketMeta aa && bm instanceof AxolotlBucketMeta ba) {
            if (!aa.hasVariant() || !ba.hasVariant() || aa.getVariant() != ba.getVariant()) {
                return false;
            }
        }
        if (am instanceof TropicalFishBucketMeta at && bm instanceof TropicalFishBucketMeta bt) {
            if (!at.hasVariant() || !bt.hasVariant()
                    || at.getPattern() != bt.getPattern()
                    || at.getBodyColor() != bt.getBodyColor()
                    || at.getPatternColor() != bt.getPatternColor()) {
                return false;
            }
        }
        if (am instanceof SuspiciousStewMeta as2 && bm instanceof SuspiciousStewMeta bs2
                && !Objects.equals(as2.getCustomEffects(), bs2.getCustomEffects())) {
            return false;
        }
        if (am instanceof BannerMeta abn && bm instanceof BannerMeta bbn
                && !abn.getPatterns().equals(bbn.getPatterns())) {
            return false;
        }
        return true;
    }

    /**
     * EN: Safely compares two PersistentDataContainers, ignoring transient GUI markers.
     *
     * ES: Compara de forma segura dos PersistentDataContainers ignorando marcadores transitorios de GUI.
     */
    public static boolean pdcMatches(org.bukkit.persistence.PersistentDataContainer a,
                                     org.bukkit.persistence.PersistentDataContainer b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        if (a.equals(b)) {
            return true;
        }
        var aKeys = new java.util.HashSet<>(a.getKeys());
        var bKeys = new java.util.HashSet<>(b.getKeys());
        if (Keys.TERMINAL_DISPLAY != null) {
            aKeys.remove(Keys.TERMINAL_DISPLAY);
            bKeys.remove(Keys.TERMINAL_DISPLAY);
        }
        if (!aKeys.equals(bKeys)) {
            return false;
        }
        for (var key : aKeys) {
            if (!pdcTagMatches(a, b, key)) {
                return false;
            }
        }
        return true;
    }

    private static boolean pdcTagMatches(org.bukkit.persistence.PersistentDataContainer a,
                                         org.bukkit.persistence.PersistentDataContainer b,
                                         org.bukkit.NamespacedKey key) {
        try {
            if (a.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.STRING),
                        b.get(key, org.bukkit.persistence.PersistentDataType.STRING));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.INTEGER),
                        b.get(key, org.bukkit.persistence.PersistentDataType.INTEGER));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.BYTE),
                        b.get(key, org.bukkit.persistence.PersistentDataType.BYTE));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.LONG)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.LONG),
                        b.get(key, org.bukkit.persistence.PersistentDataType.LONG));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.DOUBLE)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.DOUBLE),
                        b.get(key, org.bukkit.persistence.PersistentDataType.DOUBLE));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.BYTE_ARRAY)) {
                return java.util.Arrays.equals(a.get(key, org.bukkit.persistence.PersistentDataType.BYTE_ARRAY),
                        b.get(key, org.bukkit.persistence.PersistentDataType.BYTE_ARRAY));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.INTEGER_ARRAY)) {
                return java.util.Arrays.equals(a.get(key, org.bukkit.persistence.PersistentDataType.INTEGER_ARRAY),
                        b.get(key, org.bukkit.persistence.PersistentDataType.INTEGER_ARRAY));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.LONG_ARRAY)) {
                return java.util.Arrays.equals(a.get(key, org.bukkit.persistence.PersistentDataType.LONG_ARRAY),
                        b.get(key, org.bukkit.persistence.PersistentDataType.LONG_ARRAY));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.SHORT)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.SHORT),
                        b.get(key, org.bukkit.persistence.PersistentDataType.SHORT));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.FLOAT)) {
                return Objects.equals(a.get(key, org.bukkit.persistence.PersistentDataType.FLOAT),
                        b.get(key, org.bukkit.persistence.PersistentDataType.FLOAT));
            }
            if (a.has(key, org.bukkit.persistence.PersistentDataType.TAG_CONTAINER)) {
                return b.has(key, org.bukkit.persistence.PersistentDataType.TAG_CONTAINER)
                        && pdcMatches(a.get(key, org.bukkit.persistence.PersistentDataType.TAG_CONTAINER),
                        b.get(key, org.bukkit.persistence.PersistentDataType.TAG_CONTAINER));
            }
        } catch (IllegalArgumentException ignored) {
        }
        // Un tipo que no sabemos comparar no se da por igual: antes devolvia true y dos pollos de
        // GeneticChickengineering con distinto ADN (un int[]) se fundian en un solo stack.
        return false;
    }

    /**
     * EN: Returns a clone of the given ItemStack with the specified stack amount.
 *
     * ES: Devuelve un clon del ItemStack con la cantidad especificada.
     *
     * @param stack  The item to clone / ES: El ítem a clonar.
     * @param amount Target amount / ES: Cantidad deseada.
     * @return Cloned ItemStack / ES: Clon con nueva cantidad.
     */
    public static ItemStack getAsQuantity(ItemStack stack, int amount) {
        ItemStack clone = stack.clone();
        clone.setAmount(amount);
        return clone;
    }
}
