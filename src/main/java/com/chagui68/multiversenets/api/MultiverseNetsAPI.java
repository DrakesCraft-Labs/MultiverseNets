package com.chagui68.multiversenets.api;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.net.Network;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * [EN] Public API so external plugins (e.g. Slimefun's Networks addon) can read and write
 * a MultiverseNets network's VIRTUAL storage directly, given any block that belongs to it.
 * Thin, null-safe facade over {@code networkAt(block).storage()}; if the block is not part of a
 * MultiverseNets network (or the plugin is not loaded) every method returns a neutral value,
 * so callers can probe safely via reflection without a compile-time dependency.
 *
 * <p>Motivation: MultiverseNets keeps items virtually (Base64 NodeBlobs in the chunk PDC),
 * not in a Bukkit {@code Inventory}, so a network grabber from another plugin cannot see them
 * through {@code InventoryHolder}. This bridge exposes the storage explicitly and symmetrically
 * to the existing {@code SlimefunBridge} (which lets MultiverseNets reach Slimefun machines).
 *
 * [ES] API publica para que plugins externos (p.ej. la red de Slimefun) lean y escriban
 * directamente el almacenamiento VIRTUAL de una red de MultiverseNets, dado cualquier bloque
 * que le pertenezca. Facade delgada y null-safe sobre {@code networkAt(block).storage()}.
 */
public final class MultiverseNetsAPI {

    private MultiverseNetsAPI() {
    }

    /** [ES] true si el bloque pertenece a una red de MultiverseNets. */
    public static boolean isNetworkBlock(Block block) {
        return networkOf(block) != null;
    }

    /**
     * [ES] Extrae hasta {@code amount} items que cumplan {@code matcher} del almacenamiento de la
     * red a la que pertenece {@code block}. Devuelve el stack extraido, o {@code null} si no hay
     * red o no habia items que coincidan.
     */
    public static ItemStack extract(Block block, Predicate<ItemStack> matcher, int amount) {
        final Network net = networkOf(block);
        if (net == null || matcher == null || amount <= 0) {
            return null;
        }
        try {
            return net.storage().withdraw(matcher, amount);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * [ES] Inserta {@code stack} en el almacenamiento de la red de {@code block}. Devuelve la
     * cantidad SOBRANTE que no pudo entrar (0 = todo entro). Si no hay red, devuelve la cantidad
     * completa (nada entro).
     */
    public static int insert(Block block, ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        final Network net = networkOf(block);
        if (net == null) {
            return stack.getAmount();
        }
        try {
            return net.storage().deposit(stack); // deposit() ya devuelve el sobrante (leftover)
        } catch (Throwable ignored) {
            return stack.getAmount();
        }
    }

    /** [ES] Cuenta cuantos items cumplen {@code matcher} en la red de {@code block}. */
    public static long count(Block block, Predicate<ItemStack> matcher) {
        final Network net = networkOf(block);
        if (net == null || matcher == null) {
            return 0L;
        }
        try {
            return net.storage().count(matcher);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static Network networkOf(Block block) {
        if (block == null) {
            return null;
        }
        final MultiverseNets plugin = MultiverseNets.instance();
        if (plugin == null || plugin.networks() == null) {
            return null;
        }
        try {
            return plugin.networks().networkAt(block);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
