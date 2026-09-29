package com.chagui68.multiversenets.persist;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * [EN] Persistent Node State Data Object (Blob)
 * Serialized into Base64 within the chunk's PersistentDataContainer (PDC).
 *
 * Note: {@code serialVersionUID = 1L} must remain unchanged. Java accommodates new fields
 * during deserialization of legacy blobs (defaulting to null/false/0) only when UID matches.
 *
 * [ES] Objeto de Estado Persistente de Nodo (Blob)
 * Serializado en Base64 dentro del PersistentDataContainer (PDC) del chunk.
 *
 * Nota: {@code serialVersionUID = 1L} se mantiene intacto para garantizar retrocompatibilidad.
 */
public class NodeBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    /** EN: DeviceType name / ES: Nombre del DeviceType. */
    public String typeName;
    /** EN: Sample item template stored in cell / ES: Muestra de ítem almacenado en la celda. */
    public ItemStack cellSample;
    /** EN: Total quantity stored in cell / ES: Cantidad total almacenada en la celda. */
    public long cellAmount;
    /** EN: Material names/IDs for filtering / ES: Nombres/IDs de materiales para filtro. */
    public List<String> filterMaterials = new ArrayList<>();
    /** EN: Item templates for exact meta/custom filtering / ES: Plantillas de ítems para filtro exacto. */
    public List<ItemStack> filterItems = new ArrayList<>();
    /** EN: True if filter acts as blacklist; false for whitelist / ES: True para blacklist, false para whitelist. */
    public boolean filterBlacklist;
    /** EN: Legacy recipe string keys / ES: Claves de recetas antiguas. */
    public List<String> recipes = new ArrayList<>();
    /** EN: Encoded blueprints installed in auto-crafter / ES: Blueprints codificados en el autocrafteador. */
    public List<String> blueprintData = new ArrayList<>();
    /** EN: Persistent 3x3 crafting matrix template / ES: Matriz plantilla 3x3 persistente. */
    public ItemStack[] craftingMatrix = new ItemStack[9];
    /** EN: Particle visual marker flag / ES: Marca visual de partículas con crayon. */
    public boolean crayon;
    /** EN: Transmitter linked world UUID string / ES: UUID del mundo del transmisor vinculado. */
    public String txWorld;
    /** EN: Transmitter linked block X coordinate / ES: Coordenada X del transmisor vinculado. */
    public int txX;
    /** EN: Transmitter linked block Y coordinate / ES: Coordenada Y del transmisor vinculado. */
    public int txY;
    /** EN: Transmitter linked block Z coordinate / ES: Coordenada Z del transmisor vinculado. */
    public int txZ;
    /** EN: Selected target face direction / ES: Dirección seleccionada (NORTH, SOUTH, etc. o ALL). */
    public String targetFace;
    /** EN: Multi-item storage templates for Greedy Cell / ES: Plantillas de almacenamiento multi-ítem para Greedy Cell. */
    public List<ItemStack> greedySamples = new ArrayList<>();
    /** EN: Multi-item storage quantities for Greedy Cell / ES: Cantidades de almacenamiento multi-ítem para Greedy Cell. */
    public List<Long> greedyAmounts = new ArrayList<>();
    /** EN: CPU Virtual Cache Tier (1: L1, 2: L2, 3: L3, 4: DRAM, 5: Quantum) / ES: Nivel de Caché Virtual de CPU. */
    public int virtualCacheTier;
    /** EN: Multi-item storage templates for Virtual Cache / ES: Plantillas de ítems en la caché virtual. */
    public List<ItemStack> virtualSamples = new ArrayList<>();
    /** EN: Multi-item storage quantities for Virtual Cache / ES: Cantidades de ítems en la caché virtual. */
    public List<Long> virtualAmounts = new ArrayList<>();
    /** EN: Transit buffer holding items during backpressure / ES: Búfer de tránsito ante contrapresión. */
    public ItemStack transitBuffer;
    /** EN: Target item sample for stock quota limit / ES: Muestra de ítem objetivo para el delimitador de cuota. */
    public ItemStack quotaSample;
    /** EN: Maximum stock quota allowed in the network / ES: Cuota máxima de stock permitida en la red. */
    public long quotaLimit;
    /** EN: Whether quota limiter is actively enforced / ES: Si el delimitador de cuota está activo. */
    public boolean quotaActive = true;
    /** EN: Fluid type name stored in fluid cell (e.g. WATER, LAVA, MILK, HONEY) / ES: Tipo de fluido en la celda. */
    public String fluidType;
    /** EN: Quantity of fluid stored in millibuckets (mB) / ES: Cantidad de fluido almacenada en mB. */
    public long fluidAmount;
    /** EN: Liquid pump mode (DRAIN or FILL) / ES: Modo de bomba de líquidos (DRAIN o FILL). */
    public String pumpMode;
    /** EN: Liquid pump fluid filter (e.g. WATER, LAVA, etc. or null for ANY) / ES: Filtro de fluido para bomba. */
    public String pumpFluid;
    /**
     * EN: UUID of the player who placed this Controller, i.e. the network's owner. Only the
     * Controller carries it. It is what lets a network operate inside the claim of the player who
     * built it, instead of being blocked by land it cannot prove it belongs to.
     * ES: UUID del jugador que colocó este Controlador, es decir, el dueño de la red. Solo lo
     * lleva el Controlador. Es lo que permite a una red operar dentro del reclamo del jugador que
     * la construyó, en vez de quedar bloqueada por tierra que no puede demostrar que es suya.
     */
    public String ownerUuid;

    /**
     * EN: Returns the combined sum of all items stored in the Greedy Cell.
     *
     * ES: Devuelve la suma combinada de todos los ítems almacenados en la Greedy Cell.
     */
    public long totalGreedyAmount() {
        long total = 0;
        if (greedyAmounts != null) {
            for (Long amt : greedyAmounts) {
                if (amt != null) {
                    total += amt;
                }
            }
        }
        return total;
    }

    /**
     * EN: Finds the index of a matching item sample in greedy storage.
     *
     * ES: Encuentra el índice de una muestra coincidente en el almacenamiento greedy.
     */
    public int indexOfGreedySample(ItemStack item) {
        if (item == null || greedySamples == null) {
            return -1;
        }
        for (int i = 0; i < greedySamples.size(); i++) {
            if (com.chagui68.multiversenets.util.StackUtils.itemsMatch(greedySamples.get(i), item)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * EN: Adds an item amount to the multi-item greedy storage.
     *
     * ES: Añade una cantidad de ítem al almacenamiento greedy multi-ítem.
     */
    public void addGreedyItem(ItemStack item, long amount) {
        if (item == null || amount <= 0) {
            return;
        }
        if (greedySamples == null) {
            greedySamples = new ArrayList<>();
        }
        if (greedyAmounts == null) {
            greedyAmounts = new ArrayList<>();
        }
        int idx = indexOfGreedySample(item);
        if (idx >= 0) {
            greedyAmounts.set(idx, greedyAmounts.get(idx) + amount);
        } else {
            greedySamples.add(com.chagui68.multiversenets.util.StackUtils.getAsQuantity(item, 1));
            greedyAmounts.add(amount);
        }
    }

    /**
     * EN: Removes up to {@code amount} of an item at the given index.
     *
     * ES: Retira hasta {@code amount} del ítem en el índice especificado.
     */
    public long removeGreedyItem(int index, long amount) {
        if (greedyAmounts == null || index < 0 || index >= greedyAmounts.size() || amount <= 0) {
            return 0;
        }
        long current = greedyAmounts.get(index);
        long take = Math.min(current, amount);
        long remaining = current - take;
        if (remaining <= 0) {
            greedySamples.remove(index);
            greedyAmounts.remove(index);
        } else {
            greedyAmounts.set(index, remaining);
        }
        return take;
    }

    public long totalVirtualAmount() {
        long total = 0;
        if (virtualAmounts != null) {
            for (Long amt : virtualAmounts) {
                if (amt != null) {
                    total += amt;
                }
            }
        }
        return total;
    }

    public int indexOfVirtualSample(ItemStack item) {
        if (item == null || virtualSamples == null) {
            return -1;
        }
        for (int i = 0; i < virtualSamples.size(); i++) {
            if (com.chagui68.multiversenets.util.StackUtils.itemsMatch(virtualSamples.get(i), item)) {
                return i;
            }
        }
        return -1;
    }

    public void addVirtualItem(ItemStack item, long amount) {
        if (item == null || amount <= 0) {
            return;
        }
        if (virtualSamples == null) {
            virtualSamples = new ArrayList<>();
        }
        if (virtualAmounts == null) {
            virtualAmounts = new ArrayList<>();
        }
        int idx = indexOfVirtualSample(item);
        if (idx >= 0) {
            virtualAmounts.set(idx, virtualAmounts.get(idx) + amount);
        } else {
            virtualSamples.add(com.chagui68.multiversenets.util.StackUtils.getAsQuantity(item, 1));
            virtualAmounts.add(amount);
        }
    }

    public long removeVirtualItem(int index, long amount) {
        if (virtualAmounts == null || index < 0 || index >= virtualAmounts.size() || amount <= 0) {
            return 0;
        }
        long current = virtualAmounts.get(index);
        long take = Math.min(current, amount);
        long remaining = current - take;
        if (remaining <= 0) {
            virtualSamples.remove(index);
            virtualAmounts.remove(index);
        } else {
            virtualAmounts.set(index, remaining);
        }
        return take;
    }

    /**
     * EN: Parses {@link #ownerUuid} back into a UUID, or null when absent/corrupt.
     *
     * ES: Convierte {@link #ownerUuid} en UUID, o null si falta o está corrupto.
     */
    public java.util.UUID owner() {
        if (ownerUuid == null || ownerUuid.isBlank()) {
            return null;
        }
        try {
            return java.util.UUID.fromString(ownerUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * EN: Factory method to create a new NodeBlob for a given device type.
     *
     * ES: Método factoría para crear un nuevo NodeBlob con el tipo especificado.
     */
    public static NodeBlob create(String typeName) {
        NodeBlob blob = new NodeBlob();
        blob.typeName = typeName;
        return blob;
    }
}
