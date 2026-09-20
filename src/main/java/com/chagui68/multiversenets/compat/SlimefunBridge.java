package com.chagui68.multiversenets.compat;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [EN] Slimefun Compatibility Bridge
 * Allows MultiverseNets to interact with Slimefun machines without compile-time dependencies.
 *
 * Why it's needed: Grabbers and Pushers normally look for an {@code InventoryHolder}
 * in adjacent blocks. Slimefun machines do not store items in {@code BlockState}, but rather
 * in a separate {@code BlockMenu} in Slimefun's registry. Without this bridge, MultiverseNets
 * would treat Slimefun machines as plain decorative blocks.
 *
 * Why via reflection: The plugin remains completely standalone. If Slimefun is absent,
 * the bridge stays dormant and vanilla container operations continue normally.
 *
 * Multi-package support: Supports both legacy upstream packages ({@code io.github.thebusybiscuit})
 * and relocated forks ({@code com.github.drakescraft_labs}).
 *
 * Slot safety: Only accesses slots declared in {@code getSlotsAccessedByItemTransport} to respect
 * machine input/output slot design.
 *
 * [ES] Puente de Compatibilidad con Slimefun
 * Permite que MultiverseNets interactúe con máquinas de Slimefun sin dependencias en tiempo de compilación.
 *
 * Por qué es necesario: Grabbers y Pushers buscan un {@code InventoryHolder} en bloques vecinos.
 * Las máquinas de Slimefun guardan su inventario en un {@code BlockMenu} separado. Este puente permite
 * transferir ítems respetando los slots de entrada/salida de cada máquina.
 *
 * Por reflexión: Mantiene el plugin autónomo (standalone). Si Slimefun no está instalado, queda inactivo.
 */
public final class SlimefunBridge {

    /**
     * Prefijos completos del paquete de la API legacy. El jar vivo de Slimefun-Drake en DrakesCraft
     * (1.2.DEV v11.0-Drake-1.21.11) expone {@code ...slimefun4.legacy.api.BlockStorage}, sin el
     * segmento {@code .Slimefun}; el arbol de fuentes actual del fork lo tiene con el. Se prueban
     * ambos y el upstream original, y el primero que resuelva gana.
     */
    private static final String[] API_ROOTS = {
            "com.github.drakescraft_labs.slimefun4.legacy.api",
            "com.github.drakescraft_labs.slimefun4.legacy.Slimefun.api",
            "io.github.thebusybiscuit.slimefun4.legacy.api",
            "io.github.thebusybiscuit.slimefun4.legacy.Slimefun.api",
            "me.mrCookieSlime.Slimefun.api",
    };

    private static boolean available;
    private static Method mGetInventory;
    private static Method mCheckId;
    private static Method mGetPreset;
    private static Method mSlotsForTransport;
    private static Method mGetItemInSlot;
    private static Method mPushItem;
    private static Method mReplaceExistingItem;
    private static Object flowInsert;
    private static Object flowWithdraw;

    private SlimefunBridge() {
    }

    /**
     * EN: Initializes the Slimefun reflection hooks. If Slimefun is missing, it stays dormant.
 *
     * ES: Inicializa los enlaces por reflexión con Slimefun. Si Slimefun no está instalado, queda inerte.
     *
     * @param log Logger instance for diagnostic notices / ES: Instancia del logger para mensajes.
     */
    public static void init(Logger log) {
        if (!com.chagui68.multiversenets.util.Settings.compatSlimefun()) {
            log.info("[Compat] Slimefun integration disabled in config (compat.slimefun).");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Slimefun") == null) {
            log.info("[Compat] Slimefun is not installed; the network will work only with vanilla containers.");
            return;
        }
        for (String root : API_ROOTS) {
            if (tryHook(root)) {
                available = true;
                log.info("[Compat] Slimefun detected (" + root + "). Grabbers, pushers, and crafters can use machines.");
                return;
            }
        }
        log.warning("[Compat] Slimefun is installed but its API does not match any known package root. Integration disabled.");
    }

    private static boolean tryHook(String root) {
        try {
            Class<?> blockStorage = Class.forName(root + ".BlockStorage");
            Class<?> dirtyMenu = Class.forName(root + ".inventory.DirtyChestMenu");
            Class<?> preset = Class.forName(root + ".inventory.BlockMenuPreset");
            Class<?> flow = Class.forName(root + ".item_transport.ItemTransportFlow");

            mGetInventory = blockStorage.getMethod("getInventory", Block.class);
            mCheckId = blockStorage.getMethod("checkID", Block.class);
            mGetPreset = dirtyMenu.getMethod("getPreset");
            mSlotsForTransport = preset.getMethod(
                    "getSlotsAccessedByItemTransport", dirtyMenu, flow, ItemStack.class);
            mGetItemInSlot = dirtyMenu.getMethod("getItemInSlot", int.class);
            mPushItem = dirtyMenu.getMethod("pushItem", ItemStack.class, int[].class);
            mReplaceExistingItem = dirtyMenu.getMethod("replaceExistingItem", int.class, ItemStack.class);

            Object[] values = flow.getEnumConstants();
            for (Object val : values) {
                String name = ((Enum<?>) val).name();
                if ("INSERT".equals(name)) flowInsert = val;
                if ("WITHDRAW".equals(name)) flowWithdraw = val;
            }
            return flowInsert != null && flowWithdraw != null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    /**
     * EN: Returns true if Slimefun is present and its reflection API was successfully resolved.
 *
     * ES: Devuelve true si Slimefun está presente y su API se resolvió por reflexión.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * EN: Checks if a block is an active Slimefun machine with a custom menu.
 *
     * ES: Comprueba si un bloque es una máquina activa de Slimefun con menú propio.
     */
    public static boolean isMachine(Block block) {
        return menuOf(block) != null;
    }

    /**
     * EN: Returns the Slimefun item ID for a given block, or null if not a Slimefun block.
 *
     * ES: Obtiene el ID de Slimefun de un bloque, o null si no es de Slimefun.
     */
    public static String getId(Block block) {
        if (!available || block == null) return null;
        try {
            Object id = mCheckId.invoke(null, block);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }

    /**
     * EN: Returns the Slimefun ID from an ItemStack's PersistentDataContainer, or null if vanilla.
 *
     * ES: Obtiene el ID de Slimefun del PersistentDataContainer de un ItemStack, o null si es vanilla.
     */
    public static String getId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        var meta = item.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        for (org.bukkit.NamespacedKey key : pdc.getKeys()) {
            if ("slimefun_item".equalsIgnoreCase(key.getKey())) {
                String id = pdc.get(key, org.bukkit.persistence.PersistentDataType.STRING);
                if (id != null && !id.isBlank()) {
                    return id;
                }
            }
        }
        return null;
    }

    /**
     * EN: Returns true if the ItemStack is a registered Slimefun item.
 *
     * ES: Devuelve true si el ItemStack es un ítem registrado de Slimefun.
     */
    public static boolean isSlimefunItem(ItemStack item) {
        return getId(item) != null;
    }

    private static Object menuOf(Block block) {
        if (!available || block == null) return null;
        try {
            return mGetInventory.invoke(null, block);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }

    private static int[] getTransportSlots(Object menu, Object flow, ItemStack reference) {
        try {
            Object preset = mGetPreset.invoke(menu);
            if (preset == null) return new int[0];
            Object result = mSlotsForTransport.invoke(preset, menu, flow, reference);
            return result instanceof int[] array ? array : new int[0];
        } catch (ReflectiveOperationException | RuntimeException error) {
            return new int[0];
        }
    }

    /**
     * EN: Extracts up to {@code max} items matching {@code filter} from the machine's output slots.
 *
     * ES: Extrae hasta {@code max} ítems que cumplan {@code filter} de los huecos de salida de la máquina.
     *
     * @param block  The block containing the Slimefun machine / ES: Bloque de la máquina.
     * @param filter Predicate filtering allowed items / ES: Predicado que filtra los ítems válidos.
     * @param max    Maximum amount to extract / ES: Cantidad máxima a extraer.
     * @return Extracted ItemStack, or null if none found / ES: ItemStack extraído o null si no había nada.
     */
    public static ItemStack extract(Block block, Predicate<ItemStack> filter, int max) {
        Object menu = menuOf(block);
        if (menu == null || max <= 0) return null;
        try {
            for (int slot : getTransportSlots(menu, flowWithdraw, null)) {
                Object raw = mGetItemInSlot.invoke(menu, slot);
                if (!(raw instanceof ItemStack current) || current.getType().isAir()) continue;
                if (filter != null && !filter.test(current)) continue;

                int amount = Math.min(max, current.getAmount());
                ItemStack extracted = current.clone();
                extracted.setAmount(amount);

                int remaining = current.getAmount() - amount;
                ItemStack remainingStack = null;
                if (remaining > 0) {
                    remainingStack = current.clone();
                    remainingStack.setAmount(remaining);
                }
                mReplaceExistingItem.invoke(menu, slot, remainingStack);
                return extracted;
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            logError(block, error);
        }
        return null;
    }

    /**
     * EN: Inserts an ItemStack into the machine's valid input slots.
 *
     * ES: Inserta un ItemStack en los huecos de entrada declarados por la máquina.
     *
     * @param block The block containing the Slimefun machine / ES: Bloque de la máquina.
     * @param stack The items to insert / ES: Ítems a insertar.
     * @return Number of items that did not fit (0 if all inserted) / ES: Cantidad que no cupo (0 si entró todo).
     */
    public static int insert(Block block, ItemStack stack) {
        Object menu = menuOf(block);
        if (menu == null || stack == null || stack.getAmount() <= 0) {
            return stack == null ? 0 : stack.getAmount();
        }
        try {
            int[] slots = getTransportSlots(menu, flowInsert, stack);
            if (slots.length == 0) return stack.getAmount();

            Object excess = mPushItem.invoke(menu, stack.clone(), slots);
            if (excess == null) return 0;
            return excess instanceof ItemStack rem ? rem.getAmount() : 0;
        } catch (ReflectiveOperationException | RuntimeException error) {
            logError(block, error);
            return stack.getAmount();
        }
    }

    // ----------------------------------------------------------------
    // Legacy backward-compatibility aliases / Alias de compatibilidad
    // ----------------------------------------------------------------

    /** @deprecated Use {@link #isAvailable()} */
    @Deprecated
    public static boolean disponible() {
        return isAvailable();
    }

    /** @deprecated Use {@link #isMachine(Block)} */
    @Deprecated
    public static boolean esMaquina(Block block) {
        return isMachine(block);
    }

    /** @deprecated Use {@link #getId(Block)} */
    @Deprecated
    public static String idDe(Block block) {
        return getId(block);
    }

    /** @deprecated Use {@link #getId(ItemStack)} */
    @Deprecated
    public static String idDe(ItemStack item) {
        return getId(item);
    }

    /** @deprecated Use {@link #isSlimefunItem(ItemStack)} */
    @Deprecated
    public static boolean esItemSlimefun(ItemStack item) {
        return isSlimefunItem(item);
    }

    /** @deprecated Use {@link #extract(Block, Predicate, int)} */
    @Deprecated
    public static ItemStack extraer(Block block, Predicate<ItemStack> filter, int max) {
        return extract(block, filter, max);
    }

    /** @deprecated Use {@link #insert(Block, ItemStack)} */
    @Deprecated
    public static int insertar(Block block, ItemStack stack) {
        return insert(block, stack);
    }

    private static void logError(Block block, Throwable error) {
        Logger.getLogger("MultiverseNets").log(Level.FINE,
                "Failed communicating with Slimefun machine at " + block.getX() + "," + block.getY()
                        + "," + block.getZ(), error);
    }
}
