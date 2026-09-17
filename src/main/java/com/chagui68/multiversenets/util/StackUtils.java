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

import java.util.Objects;

/**
 * Comparacion profunda de items, puerto del StackUtils de NetworksV6 (sin la parte Slimefun).
 *
 * Hace falta porque ItemStack.isSimilar delega en equals del meta, y para agregar la vista de la
 * red NO se puede usar hashCode de ItemStack: Bukkit calcula mal los hash y dos terracotas del
 * mismo color podian caer en cubos distintos (bug #226 de Networks). La regla es comparar de lo
 * barato a lo caro y solo devolver true si TODO coincide.
 */
public final class StackUtils {

    private StackUtils() {
    }

    public static boolean itemsMatch(ItemStack a, ItemStack b) {
        return itemsMatch(a, b, true);
    }

    public static boolean itemsMatch(ItemStack a, ItemStack b, boolean checkLore) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getType() != b.getType()) {
            return false;
        }
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
        if (!am.getPersistentDataContainer().equals(bm.getPersistentDataContainer())) {
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

    /** Copia del stack con cantidad fija (util para plantillas de a 1). */
    public static ItemStack getAsQuantity(ItemStack stack, int amount) {
        ItemStack clone = stack.clone();
        clone.setAmount(amount);
        return clone;
    }
}
