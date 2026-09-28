package com.chagui68.multiversenets.gui;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI menu for the Auto-Crafter: displays, installs, and uninstalls crafting recipes and blueprints.
 *
 * Menú de interfaz para el Auto-Crafter: visualiza, instala y desinstala recetas y planos de crafteo.
 */
public class CrafterMenu extends MenuHolder {

    public static final int MAX_BLUEPRINT_SLOTS = 18;
    public static final int STATUS_SLOT = 24;
    public static final int CLEAR_SLOT = 25;
    public static final int HELP_SLOT = 26;

    private static final int[] BOTTOM_BORDER_SLOTS = {18, 19, 20, 21, 22, 23};

    private final Block block;

    public CrafterMenu(MultiverseNets plugin, Player player, Block block) {
        super(plugin, player);
        this.block = block;
    }

    public void openMenu() {
        DeviceType type = NodeStore.getType(block);
        String title;
        NamedTextColor titleColor = NamedTextColor.DARK_AQUA;
        if (type == DeviceType.MVN_SF_REQUEST_CRAFTER) {
            title = "Slimefun Request Crafter";
            titleColor = NamedTextColor.DARK_PURPLE;
        } else if (type == DeviceType.MVN_SF_CRAFTER) {
            title = "Slimefun Auto-Crafter";
            titleColor = NamedTextColor.DARK_PURPLE;
        } else if (type != null && type.isRequestCrafter()) {
            title = "Request Crafter";
        } else {
            title = "Auto-Crafter";
        }
        open(27, Component.text(title, titleColor)
                .decoration(TextDecoration.ITALIC, false));
    }

    private NodeBlob blob() {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            DeviceType type = NodeStore.getType(block);
            blob = NodeBlob.create(type != null ? type.name() : DeviceType.MVN_CRAFTER.name());
        }
        if (blob.blueprintData == null) {
            blob.blueprintData = new ArrayList<>();
        }
        if (blob.recipes == null) {
            blob.recipes = new ArrayList<>();
        }
        return blob;
    }

    @Override
    protected void draw() {
        NodeBlob blob = blob();
        int totalInstalled = blob.blueprintData.size() + blob.recipes.size();
        DeviceType devType = NodeStore.getType(block);
        boolean sfMachine = devType != null && devType.isSlimefunCrafter();
        String machineName = (devType != null) ? devType.display() : "Auto-Crafter";

        // 1) Dibujar casillas de Blueprints / Recetas (Slots 0..17)
        for (int i = 0; i < MAX_BLUEPRINT_SLOTS; i++) {
            if (i < blob.blueprintData.size()) {
                String b64 = blob.blueprintData.get(i);
                RecipeData data = Blueprints.decode(b64);
                if (data != null && data.output != null) {
                    ItemStack icon = data.output.clone();
                    icon.setAmount(1);
                    var meta = icon.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text("Blueprint: " + Blueprints.readableName(data.output),
                                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
                        List<Component> lore = new ArrayList<>();
                        lore.add(Component.text("Installed Recipe", NamedTextColor.YELLOW)
                                .decoration(TextDecoration.ITALIC, false));
                        lore.addAll(summarizeInputs(data.inputs));
                        lore.add(Component.empty());
                        lore.add(Component.text("Output: " + data.output.getAmount() + "x "
                                + Blueprints.readableName(data.output), NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.empty());
                        lore.add(Component.text("Left / Right Click: Uninstall blueprint", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false));
                        meta.lore(lore);
                        icon.setItemMeta(meta);
                    }
                    inv.setItem(i, icon);
                    continue;
                }
            } else if (i - blob.blueprintData.size() < blob.recipes.size()) {
                String key = blob.recipes.get(i - blob.blueprintData.size());
                Recipe recipe = CraftingSupport.find(key);
                ItemStack icon = recipe == null ? new ItemStack(Material.BARRIER) : recipe.getResult().clone();
                icon.setAmount(1);
                var meta = icon.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("Recipe: " + key, NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
                    meta.lore(List.of(
                            Component.empty(),
                            Component.text("Left / Right Click: Remove recipe", NamedTextColor.RED)
                                    .decoration(TextDecoration.ITALIC, false)));
                    icon.setItemMeta(meta);
                }
                inv.setItem(i, icon);
                continue;
            }

            // Hueco de Blueprint vacío
            ItemStack emptySlot = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
            var metaEmpty = emptySlot.getItemMeta();
            metaEmpty.displayName(Component.text("Empty Blueprint Slot", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            metaEmpty.lore(List.of(
                    Component.text("Click with a Blueprint on cursor or", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Shift-Click a Blueprint from inventory to install.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)));
            emptySlot.setItemMeta(metaEmpty);
            inv.setItem(i, emptySlot);
        }

        // 2) Bordes inferiores (Slots 18..23)
        ItemStack border = panel(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BOTTOM_BORDER_SLOTS) {
            inv.setItem(slot, border);
        }

        // 3) Botón de Estado / Info (Slot 24)
        ItemStack status = new ItemStack(Material.HOPPER);
        var metaStatus = status.getItemMeta();
        metaStatus.displayName(Component.text(machineName + " Status", sfMachine ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        metaStatus.lore(List.of(
                Component.text("Installed Blueprints: " + totalInstalled + " / " + MAX_BLUEPRINT_SLOTS,
                        NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Pulls ingredients automatically from network.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Crafting operations are atomic and safe.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        status.setItemMeta(metaStatus);
        inv.setItem(STATUS_SLOT, status);

        // 4) Botón de Limpiar todo (Slot 25)
        ItemStack clear = new ItemStack(Material.BARRIER);
        var metaClear = clear.getItemMeta();
        metaClear.displayName(Component.text("Clear All Blueprints", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        metaClear.lore(List.of(
                Component.text("Click to remove all installed blueprints.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
        clear.setItemMeta(metaClear);
        inv.setItem(CLEAR_SLOT, clear);

        // 5) Botón de Ayuda (Slot 26)
        ItemStack help = new ItemStack(Material.BOOK);
        var metaHelp = help.getItemMeta();
        metaHelp.displayName(Component.text("How " + machineName + " Works", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        if (sfMachine) {
            metaHelp.lore(List.of(
                    Component.text("• Encode recipes using the Slimefun Recipe Encoder.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("• Shift-Click or place Slimefun Blueprints here to install.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("• Only Slimefun blueprints are accepted by this machine.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("• Click any installed recipe above to uninstall it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        } else {
            metaHelp.lore(List.of(
                    Component.text("• Encode recipes using the Blueprint Encoder.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("• Shift-Click or place Blueprints here to install.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("• The network crafts items automatically and stores output.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("• Click any installed recipe above to uninstall it.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        }
        help.setItemMeta(metaHelp);
        inv.setItem(HELP_SLOT, help);
    }

    private List<Component> summarizeInputs(ItemStack[] inputs) {
        List<Component> list = new ArrayList<>();
        if (inputs == null) {
            return list;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack in : inputs) {
            if (in != null && !in.getType().isAir()) {
                String name = Blueprints.readableName(in);
                counts.put(name, counts.getOrDefault(name, 0) + in.getAmount());
            }
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            list.add(Component.text("- " + entry.getValue() + "x " + entry.getKey(), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        return list;
    }

    @Override
    protected void click(InventoryClickEvent event) {
        int raw = event.getRawSlot();
        NodeBlob blob = blob();

        // 1) Shift-click desde el inventario del jugador hacia el Auto-Crafter
        if (raw >= inv.getSize()) {
            ItemStack mover = event.getCurrentItem();
            if (mover == null || mover.getType().isAir()) {
                return;
            }
            RecipeData data = Blueprints.read(mover);
            if (data == null) {
                player.sendMessage(Text.msg("This item is not a Blueprint. Create one with the Blueprint Encoder.", NamedTextColor.YELLOW));
                return;
            }
            DeviceType devType = NodeStore.getType(block);
            boolean isSfMachine = devType != null && devType.isSlimefunCrafter();
            boolean isSfRec = isSlimefunRecipe(data, mover);
            if (isSfMachine && !isSfRec) {
                player.sendMessage(Text.msg("Slimefun Crafters only accept Slimefun recipes. Use a standard Auto-Crafter for vanilla recipes.", NamedTextColor.RED));
                return;
            }
            if (!isSfMachine && isSfRec) {
                player.sendMessage(Text.msg("Standard Crafters cannot craft Slimefun recipes. Use a Slimefun Auto-Crafter.", NamedTextColor.RED));
                return;
            }
            int total = blob.blueprintData.size() + blob.recipes.size();
            if (total >= MAX_BLUEPRINT_SLOTS) {
                String machineName = (devType != null) ? devType.display() : "Auto-Crafter";
                player.sendMessage(Text.msg(machineName + " is full (max " + MAX_BLUEPRINT_SLOTS + " recipes).", NamedTextColor.RED));
                return;
            }
            String encoded = Blueprints.encode(data);
            if (blob.blueprintData.contains(encoded)) {
                player.sendMessage(Text.msg("This Blueprint is already installed.", NamedTextColor.YELLOW));
                return;
            }
            blob.blueprintData.add(encoded);
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("Blueprint installed: " + Blueprints.readableName(data.output), NamedTextColor.GREEN));
            draw();
            return;
        }

        // 2) Botón de Limpiar Todo (Slot 25)
        if (raw == CLEAR_SLOT) {
            blob.blueprintData.clear();
            blob.recipes.clear();
            NodeStore.put(block, blob);
            player.sendMessage(Text.msg("All blueprints cleared from Auto-Crafter.", NamedTextColor.YELLOW));
            draw();
            return;
        }

        // 3) Botón de Estado o Ayuda (Slots 24, 26)
        if (raw == STATUS_SLOT || raw == HELP_SLOT) {
            draw();
            return;
        }

        // 4) Clic en casillas de Blueprints (0..17)
        if (raw >= 0 && raw < MAX_BLUEPRINT_SLOTS) {
            ItemStack cursor = event.getView().getCursor();
            boolean hasCursor = cursor != null && !cursor.getType().isAir();
            int total = blob.blueprintData.size() + blob.recipes.size();

            if (hasCursor) {
                RecipeData data = Blueprints.read(cursor);
                String legacyKey = data == null ? Items.readBlueprint(cursor) : null;
                if (data == null && legacyKey == null) {
                    legacyKey = findRecipeKeyByResult(cursor);
                }
                if (data == null && legacyKey == null) {
                    player.sendMessage(Text.msg("Please insert a valid Blueprint (created with Blueprint Encoder).", NamedTextColor.RED));
                    return;
                }

                DeviceType devType = NodeStore.getType(block);
                boolean isSfMachine = devType != null && devType.isSlimefunCrafter();
                if (data != null) {
                    boolean isSfRec = isSlimefunRecipe(data, cursor);
                    if (isSfMachine && !isSfRec) {
                        player.sendMessage(Text.msg("Slimefun Crafters only accept Slimefun recipes. Use a standard Auto-Crafter for vanilla recipes.", NamedTextColor.RED));
                        return;
                    }
                    if (!isSfMachine && isSfRec) {
                        player.sendMessage(Text.msg("Standard Crafters cannot craft Slimefun recipes. Use a Slimefun Auto-Crafter.", NamedTextColor.RED));
                        return;
                    }
                } else if (legacyKey != null && isSfMachine) {
                    player.sendMessage(Text.msg("Slimefun Crafters only accept Slimefun blueprints.", NamedTextColor.RED));
                    return;
                }

                if (raw < total) {
                    // Reemplazar la receta existente en este hueco
                    if (data != null) {
                        String encoded = Blueprints.encode(data);
                        if (raw < blob.blueprintData.size()) {
                            blob.blueprintData.set(raw, encoded);
                        } else {
                            blob.blueprintData.add(encoded);
                        }
                        NodeStore.put(block, blob);
                        player.sendMessage(Text.msg("Updated blueprint to: " + Blueprints.readableName(data.output), NamedTextColor.GREEN));
                        draw();
                    }
                } else {
                    // Instalar nueva receta
                    if (total >= MAX_BLUEPRINT_SLOTS) {
                        String machineName = (devType != null) ? devType.display() : "Auto-Crafter";
                        player.sendMessage(Text.msg(machineName + " is full (max " + MAX_BLUEPRINT_SLOTS + " recipes).", NamedTextColor.RED));
                        return;
                    }
                    if (data != null) {
                        String encoded = Blueprints.encode(data);
                        if (blob.blueprintData.contains(encoded)) {
                            player.sendMessage(Text.msg("This Blueprint is already installed.", NamedTextColor.YELLOW));
                            return;
                        }
                        blob.blueprintData.add(encoded);
                        NodeStore.put(block, blob);
                        player.sendMessage(Text.msg("Blueprint installed: " + Blueprints.readableName(data.output), NamedTextColor.GREEN));
                        draw();
                    } else if (legacyKey != null) {
                        if (!blob.recipes.contains(legacyKey)) {
                            blob.recipes.add(legacyKey);
                            NodeStore.put(block, blob);
                            player.sendMessage(Text.msg("Recipe added: " + legacyKey, NamedTextColor.GREEN));
                            draw();
                        }
                    }
                }
            } else {
                // Clic sin ítem en el cursor: desinstalar / retirar la receta en este hueco
                if (raw < total) {
                    if (raw < blob.blueprintData.size()) {
                        String removedB64 = blob.blueprintData.remove(raw);
                        RecipeData data = Blueprints.decode(removedB64);
                        String name = data != null && data.output != null ? Blueprints.readableName(data.output) : "Blueprint";
                        player.sendMessage(Text.msg("Uninstalled blueprint: " + name, NamedTextColor.YELLOW));
                    } else {
                        String removedKey = blob.recipes.remove(raw - blob.blueprintData.size());
                        player.sendMessage(Text.msg("Removed recipe: " + removedKey, NamedTextColor.YELLOW));
                    }
                    NodeStore.put(block, blob);
                    draw();
                }
            }
        }
    }

    private String findRecipeKeyByResult(ItemStack sample) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe instanceof ComplexRecipe || recipe.getResult().getType().isAir()) {
                continue;
            }
            if (recipe.getResult().isSimilar(sample) && recipe instanceof org.bukkit.Keyed keyed) {
                return keyed.getKey().toString();
            }
        }
        return null;
    }

    private boolean isSlimefunRecipe(RecipeData data, ItemStack blueprintItem) {
        if (blueprintItem != null && blueprintItem.hasItemMeta()) {
            Byte sf = blueprintItem.getItemMeta().getPersistentDataContainer()
                    .get(com.chagui68.multiversenets.util.Keys.SF_BLUEPRINT, org.bukkit.persistence.PersistentDataType.BYTE);
            if (sf != null && sf == (byte) 1) {
                return true;
            }
        }
        return isSlimefunRecipe(data);
    }

    private boolean isSlimefunRecipe(RecipeData data) {
        if (data == null) {
            return false;
        }
        if (data.output != null && (SlimefunBridge.isSlimefunItem(data.output) || SlimefunBridge.getId(data.output) != null)) {
            return true;
        }
        if (data.inputs != null) {
            for (ItemStack in : data.inputs) {
                if (in != null && (SlimefunBridge.isSlimefunItem(in) || SlimefunBridge.getId(in) != null)) {
                    return true;
                }
            }
            if (SlimefunBridge.isAvailable() && SlimefunBridge.findSlimefunRecipe(data.inputs) != null) {
                return true;
            }
        }
        return false;
    }

    private ItemStack panel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }
}
