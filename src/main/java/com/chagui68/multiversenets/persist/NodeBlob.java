package com.chagui68.multiversenets.persist;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NodeBlob implements Serializable {

    private static final long serialVersionUID = 1L;

    public String typeName;
    public ItemStack cellSample;
    public long cellAmount;
    public List<String> filterMaterials = new ArrayList<>();
    public List<String> recipes = new ArrayList<>();
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
