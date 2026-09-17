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
