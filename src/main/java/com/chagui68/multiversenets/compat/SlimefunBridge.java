package com.chagui68.multiversenets.compat;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deja que la red hable con las maquinas de Slimefun sin depender de Slimefun.
 *
 * POR QUE HACE FALTA
 *   Los grabbers y pushers buscan un InventoryHolder en el bloque vecino. Una maquina de
 *   Slimefun no lo es: su inventario no vive en el BlockState sino en un BlockMenu aparte, en
 *   el registro del propio Slimefun. Sin esto, MultiverseNets ve una fundidora electrica como
 *   un bloque decorativo y no puede meterle ni sacarle nada, que es justo lo que hace falta
 *   para ser alternativa a Networks.
 *
 * POR QUE POR REFLEXION
 *   El plugin se declara standalone y esa es su gracia: funciona en un servidor sin Slimefun.
 *   Enlazar contra sus clases obligaria a tenerlo. Aqui se resuelve todo en tiempo de ejecucion
 *   y, si Slimefun no esta, el puente se queda inactivo y el resto sigue igual.
 *
 * POR QUE DOS PAQUETES
 *   El fork de DrakesCraft repaqueto Slimefun a com.github.drakescraft_labs; el original vive en
 *   io.github.thebusybiscuit. Se prueban ambos, asi que el mismo jar sirve en los dos sitios sin
 *   compilar dos veces.
 *
 * QUE SLOTS SE USAN
 *   Los que la propia maquina declara en getSlotsAccessedByItemTransport, no todos los del menu.
 *   Eso importa: meter carbon en el hueco de salida de una fundidora la atasca, y sacar de la
 *   entrada le roba lo que estaba procesando. Preguntandole a la maquina se respeta su diseno.
 */
public final class SlimefunBridge {

    private static final String[] RAICES = {
            "com.github.drakescraft_labs.slimefun4.legacy",
            "io.github.thebusybiscuit.slimefun4.legacy",
    };

    private static boolean disponible;
    private static Method mGetInventory;
    private static Method mCheckId;
    private static Method mGetPreset;
    private static Method mSlotsParaTransporte;
    private static Method mGetItemInSlot;
    private static Method mPushItem;
    private static Method mReplaceExistingItem;
    private static Object flujoInsertar;
    private static Object flujoRetirar;

    private SlimefunBridge() {
    }

    /** Resuelve la API de Slimefun una sola vez. Sin el plugin instalado no hace nada. */
    public static void init(Logger log) {
        if (!com.chagui68.multiversenets.util.Settings.compatSlimefun()) {
            log.info("[Compat] Integracion con Slimefun desactivada en el config (compat.slimefun).");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("Slimefun") == null) {
            log.info("[Compat] Slimefun no esta instalado; la red trabajara solo con contenedores de vanilla.");
            return;
        }
        for (String raiz : RAICES) {
            if (intentar(raiz)) {
                disponible = true;
                log.info("[Compat] Slimefun detectado (" + raiz + "). Grabbers, pushers y "
                        + "autocrafteadores pueden usar sus maquinas.");
                return;
            }
        }
        log.warning("[Compat] Slimefun esta instalado pero su API no encaja con ninguna variante "
                + "conocida. La integracion queda desactivada; el resto del plugin no se ve afectado.");
    }

    private static boolean intentar(String raiz) {
        try {
            Class<?> blockStorage = Class.forName(raiz + ".Slimefun.api.BlockStorage");
            Class<?> dirtyMenu = Class.forName(raiz + ".Slimefun.api.inventory.DirtyChestMenu");
            Class<?> preset = Class.forName(raiz + ".Slimefun.api.inventory.BlockMenuPreset");
            Class<?> flujo = Class.forName(raiz + ".Slimefun.api.item_transport.ItemTransportFlow");

            mGetInventory = blockStorage.getMethod("getInventory", Block.class);
            mCheckId = blockStorage.getMethod("checkID", Block.class);
            mGetPreset = dirtyMenu.getMethod("getPreset");
            mSlotsParaTransporte = preset.getMethod(
                    "getSlotsAccessedByItemTransport", dirtyMenu, flujo, ItemStack.class);
            mGetItemInSlot = dirtyMenu.getMethod("getItemInSlot", int.class);
            mPushItem = dirtyMenu.getMethod("pushItem", ItemStack.class, int[].class);
            mReplaceExistingItem = dirtyMenu.getMethod("replaceExistingItem", int.class, ItemStack.class);

            Object[] valores = flujo.getEnumConstants();
            for (Object valor : valores) {
                String nombre = ((Enum<?>) valor).name();
                if ("INSERT".equals(nombre)) flujoInsertar = valor;
                if ("WITHDRAW".equals(nombre)) flujoRetirar = valor;
            }
            return flujoInsertar != null && flujoRetirar != null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    public static boolean disponible() {
        return disponible;
    }

    /** True si el bloque es una maquina de Slimefun con inventario propio. */
    public static boolean esMaquina(Block block) {
        return menuDe(block) != null;
    }

    /** El id de Slimefun del bloque, o null. Util para diagnostico. */
    public static String idDe(Block block) {
        if (!disponible) return null;
        try {
            Object id = mCheckId.invoke(null, block);
            return id == null ? null : id.toString();
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }

    private static Object menuDe(Block block) {
        if (!disponible || block == null) return null;
        try {
            return mGetInventory.invoke(null, block);
        } catch (ReflectiveOperationException | RuntimeException error) {
            return null;
        }
    }

    private static int[] slots(Object menu, Object flujo, ItemStack referencia) {
        try {
            Object preset = mGetPreset.invoke(menu);
            if (preset == null) return new int[0];
            Object resultado = mSlotsParaTransporte.invoke(preset, menu, flujo, referencia);
            return resultado instanceof int[] array ? array : new int[0];
        } catch (ReflectiveOperationException | RuntimeException error) {
            return new int[0];
        }
    }

    /**
     * Saca hasta {@code maximo} unidades que cumplan el filtro, de los huecos que la maquina
     * declara como salida. Devuelve null si no habia nada que sacar.
     */
    public static ItemStack extraer(Block block, Predicate<ItemStack> filtro, int maximo) {
        Object menu = menuDe(block);
        if (menu == null || maximo <= 0) return null;
        try {
            for (int slot : slots(menu, flujoRetirar, null)) {
                Object crudo = mGetItemInSlot.invoke(menu, slot);
                if (!(crudo instanceof ItemStack actual) || actual.getType().isAir()) continue;
                if (filtro != null && !filtro.test(actual)) continue;

                int cuantos = Math.min(maximo, actual.getAmount());
                ItemStack sacado = actual.clone();
                sacado.setAmount(cuantos);

                int quedan = actual.getAmount() - cuantos;
                // replaceExistingItem y no mutar el ItemStack: el menu guarda la referencia viva
                // y Slimefun necesita enterarse del cambio para marcar el bloque como sucio.
                ItemStack resto = null;
                if (quedan > 0) {
                    resto = actual.clone();
                    resto.setAmount(quedan);
                }
                mReplaceExistingItem.invoke(menu, slot, resto);
                return sacado;
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            registrar(block, error);
        }
        return null;
    }

    /**
     * Mete el stack en los huecos de entrada que la maquina declara.
     *
     * @return cuantas unidades NO cupieron; 0 si entro todo
     */
    public static int insertar(Block block, ItemStack stack) {
        Object menu = menuDe(block);
        if (menu == null || stack == null || stack.getAmount() <= 0) {
            return stack == null ? 0 : stack.getAmount();
        }
        try {
            int[] huecos = slots(menu, flujoInsertar, stack);
            if (huecos.length == 0) return stack.getAmount();

            Object sobra = mPushItem.invoke(menu, stack.clone(), huecos);
            if (sobra == null) return 0;
            return sobra instanceof ItemStack resto ? resto.getAmount() : 0;
        } catch (ReflectiveOperationException | RuntimeException error) {
            registrar(block, error);
            return stack.getAmount();
        }
    }

    private static void registrar(Block block, Throwable error) {
        Logger.getLogger("MultiverseNets").log(Level.FINE,
                "Fallo hablando con la maquina de Slimefun en " + block.getX() + "," + block.getY()
                        + "," + block.getZ(), error);
    }
}
