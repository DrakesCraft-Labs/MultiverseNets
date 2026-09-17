package com.chagui68.multiversenets.persist;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado persistido de un nodo, serializado a Base64 en el PDC del chunk.
 *
 * OJO: el serialVersionUID NO se toca jamas. Java tolera campos nuevos al deserializar blobs
 * antiguos (reciben su valor por defecto), pero solo si el UID coincide; cambiarlo invalidaria
 * todo lo que haya guardado en los mundos.
 */
public class NodeBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    public String typeName;
    public ItemStack cellSample;
    public long cellAmount;
    public List<String> filterMaterials = new ArrayList<>();
    public List<ItemStack> filterItems = new ArrayList<>();
    /** true = el filtro actua como blacklist; false (defecto) = whitelist. */
    public boolean filterBlacklist;
    public List<String> recipes = new ArrayList<>();
    /** Blueprints instalados en un autocrafteador (RecipeData serializado en Base64). */
    public List<String> blueprintData = new ArrayList<>();
    /** Matriz plantilla persistente del Crafting Grid / Encoder (9 huecos, items de a 1). */
    public ItemStack[] craftingMatrix = new ItemStack[9];
    /** Controlador: true si el crayon marco esta red para mostrar particulas. */
    public boolean crayon;
    public String txWorld;
    public int txX;
    public int txY;
    public int txZ;

    public static NodeBlob create(String typeName) {
        NodeBlob blob = new NodeBlob();
        blob.typeName = typeName;
        return blob;
    }
}
