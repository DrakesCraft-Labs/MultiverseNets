package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * [EN] Memory modules and the DRAM Bay. A module is the storage itself: it is installed in a DRAM
 * Bay, and taking it out turns its whole stock into data on the module item. Installing that item
 * in a bay of another network makes the stock appear there; the network it left loses it, because
 * the stock only ever lives in one place (the bay or the item). Item modules are the five cache
 * tiers; the Fluid DRAM holds several fluids at once.
 *
 * [ES] Módulos de memoria y el DRAM Bay. El módulo es el almacenamiento: se instala en un DRAM
 * Bay, y al sacarlo todo su stock pasa a ser datos del ítem del módulo. Instalar ese ítem en el
 * bay de otra red hace que el stock aparezca allí; la red de la que salió lo pierde, porque el
 * stock solo vive en un sitio (el bay o el ítem). Los módulos de ítems son los cinco niveles de
 * caché; el Fluid DRAM guarda varios fluidos a la vez.
 */
public final class MemoryModules {

    private MemoryModules() {
    }

    /** Item capacity of a cache module, 0 for anything else. */
    public static long itemCapacity(DeviceType module) {
        return module != null && module.isCacheModule() ? Settings.virtualCacheCapacity(module.cacheTier()) : 0L;
    }

    /** The module a bay holds, or null. */
    public static DeviceType installed(NodeBlob bay) {
        if (bay == null || bay.installedModule == null) {
            return null;
        }
        DeviceType type = DeviceType.parse(bay.installedModule);
        return type != null && type.isMemoryModule() ? type : null;
    }

    /**
     * EN: Puts {@code moduleItem} (one unit) into an empty bay, restoring the stock it carries.
     * False when the bay is taken or the item is not a module; the caller then keeps the item.
     *
     * ES: Mete {@code moduleItem} (una unidad) en un bay vacío y restaura el stock que lleva. False
     * si el bay está ocupado o el ítem no es un módulo; el llamante conserva el ítem.
     */
    public static boolean install(NodeBlob bay, ItemStack moduleItem) {
        DeviceType type = Items.typeOf(moduleItem);
        if (bay == null || type == null || !type.isMemoryModule() || installed(bay) != null) {
            return false;
        }
        NodeBlob cargo = cargoOf(moduleItem);
        bay.installedModule = type.name();
        bay.virtualSamples = new ArrayList<>();
        bay.virtualAmounts = new ArrayList<>();
        bay.dramFluids = new ArrayList<>();
        bay.dramFluidAmounts = new ArrayList<>();
        if (type.isCacheModule()) {
            bay.virtualCacheTier = type.cacheTier();
            if (cargo != null && cargo.virtualSamples != null && cargo.virtualAmounts != null) {
                for (int i = 0; i < Math.min(cargo.virtualSamples.size(), cargo.virtualAmounts.size()); i++) {
                    ItemStack sample = cargo.virtualSamples.get(i);
                    Long amount = cargo.virtualAmounts.get(i);
                    if (sample != null && amount != null && amount > 0) {
                        bay.addVirtualItem(sample, amount);
                    }
                }
            }
        } else {
            bay.virtualCacheTier = 0;
            if (cargo != null && cargo.dramFluids != null && cargo.dramFluidAmounts != null) {
                for (int i = 0; i < Math.min(cargo.dramFluids.size(), cargo.dramFluidAmounts.size()); i++) {
                    Long amount = cargo.dramFluidAmounts.get(i);
                    if (amount != null && amount > 0) {
                        bay.addDramFluid(cargo.dramFluids.get(i), amount);
                    }
                }
            }
        }
        return true;
    }

    /**
     * EN: Takes the module out of a bay and returns it as an item carrying the whole stock. The
     * bay is left empty. Null when the bay held nothing.
     *
     * ES: Saca el módulo del bay y lo devuelve como ítem con todo el stock. El bay queda vacío.
     * Null si el bay no tenía nada.
     */
    public static ItemStack eject(NodeBlob bay) {
        DeviceType type = installed(bay);
        if (type == null) {
            return null;
        }
        ItemStack item = type.isCacheModule()
                ? moduleItem(type, bay.virtualSamples, bay.virtualAmounts, null, null)
                : moduleItem(type, null, null, bay.dramFluids, bay.dramFluidAmounts);
        clear(bay);
        return item;
    }

    /**
     * EN: Controllers from before the DRAM Bay held their module inside. This turns that cache
     * into a module item with its stock, so it can be moved to a bay.
     *
     * ES: Los Controladores anteriores al DRAM Bay tenían el módulo dentro. Esto convierte esa
     * caché en un ítem de módulo con su stock, para llevarlo a un bay.
     */
    public static ItemStack ejectControllerCache(NodeBlob controller) {
        if (controller == null || controller.virtualCacheTier <= 0) {
            return null;
        }
        DeviceType type = switch (controller.virtualCacheTier) {
            case 1 -> DeviceType.MVN_CACHE_L1;
            case 2 -> DeviceType.MVN_CACHE_L2;
            case 3 -> DeviceType.MVN_CACHE_L3;
            case 4 -> DeviceType.MVN_CACHE_DRAM;
            default -> DeviceType.MVN_CACHE_QUANTUM;
        };
        ItemStack item = moduleItem(type, controller.virtualSamples, controller.virtualAmounts, null, null);
        clear(controller);
        return item;
    }

    /**
     * EN: Moves a module still installed inside a Controller (from before the DRAM Bay) into the
     * Controller's recovered-module list, with its whole stock, so it shows up in the Terminal as
     * a temporary item to collect. True when something moved.
     *
     * ES: Pasa un módulo que sigue dentro de un Controlador (de antes del DRAM Bay) a la lista de
     * módulos recuperados del Controlador, con todo su stock, para que aparezca en el Terminal como
     * ítem temporal que recoger. True si se movió algo.
     */
    public static boolean migrateControllerCache(NodeBlob controller) {
        ItemStack module = ejectControllerCache(controller);
        if (module == null) {
            return false;
        }
        if (controller.recoveredModules == null) {
            controller.recoveredModules = new ArrayList<>();
        }
        controller.recoveredModules.add(module);
        return true;
    }

    private static void clear(NodeBlob blob) {
        blob.installedModule = null;
        blob.virtualCacheTier = 0;
        blob.virtualSamples = new ArrayList<>();
        blob.virtualAmounts = new ArrayList<>();
        blob.dramFluids = new ArrayList<>();
        blob.dramFluidAmounts = new ArrayList<>();
    }

    /** Stock carried by a module item, or null when it is empty. */
    public static NodeBlob cargoOf(ItemStack moduleItem) {
        if (moduleItem == null || !moduleItem.hasItemMeta()) {
            return null;
        }
        String data = moduleItem.getItemMeta().getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING);
        return data == null ? null : NodeStore.decode(data);
    }

    /** A module item of {@code type} carrying the stock stored in {@code cargo}. */
    public static ItemStack moduleItem(DeviceType type, NodeBlob cargo) {
        if (cargo == null) {
            return Items.create(type);
        }
        return type.isCacheModule()
                ? moduleItem(type, cargo.virtualSamples, cargo.virtualAmounts, null, null)
                : moduleItem(type, null, null, cargo.dramFluids, cargo.dramFluidAmounts);
    }

    private static ItemStack moduleItem(DeviceType type, List<ItemStack> samples, List<Long> amounts,
                                        List<String> fluids, List<Long> fluidAmounts) {
        ItemStack item = Items.create(type);
        NodeBlob carrier = NodeBlob.create(type.name());
        long items = 0;
        int itemTypes = 0;
        if (samples != null && amounts != null) {
            for (int i = 0; i < Math.min(samples.size(), amounts.size()); i++) {
                Long amount = amounts.get(i);
                if (samples.get(i) != null && amount != null && amount > 0) {
                    carrier.addVirtualItem(samples.get(i), amount);
                    items += amount;
                    itemTypes++;
                }
            }
        }
        long mb = 0;
        List<Component> fluidLines = new ArrayList<>();
        if (fluids != null && fluidAmounts != null) {
            for (int i = 0; i < Math.min(fluids.size(), fluidAmounts.size()); i++) {
                Long amount = fluidAmounts.get(i);
                if (fluids.get(i) != null && amount != null && amount > 0) {
                    carrier.addDramFluid(fluids.get(i), amount);
                    mb += amount;
                    fluidLines.add(Component.text(" • " + fluids.get(i) + ": " + Items.formatAmount(amount) + " mB",
                            NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                }
            }
        }
        if (items <= 0 && mb <= 0) {
            return item;
        }
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING, NodeStore.encode(carrier));
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        if (items > 0) {
            lore.add(Component.text("Stored: " + Items.formatAmount(items) + " items (" + itemTypes + " types)",
                    NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Stored fluids: " + Items.formatAmount(mb) + " mB", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            lore.addAll(fluidLines);
        }
        lore.add(Component.text("Install it in a DRAM Bay to bring this stock into that network.",
                NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
