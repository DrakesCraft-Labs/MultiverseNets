package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.gui.BarrelMenu;
import com.chagui68.multiversenets.gui.CellMenu;
import com.chagui68.multiversenets.gui.CrafterMenu;
import com.chagui68.multiversenets.gui.CraftingGridMenu;
import com.chagui68.multiversenets.gui.EncoderMenu;
import com.chagui68.multiversenets.gui.FilterMenu;
import com.chagui68.multiversenets.gui.GreedyMenu;
import com.chagui68.multiversenets.gui.MonitorMenu;
import com.chagui68.multiversenets.gui.QuantumWorkbenchMenu;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Event listener responsible for block placement, breakage, explosions, piston movements,
 * tool usage, and network device interactions.
 *
 * Listener de eventos responsable de la colocación, rotura, explosiones, pistones,
 * uso de herramientas e interacción con dispositivos de red.
 */
public class BlockListener implements Listener {

    private final MultiverseNets plugin;
    private final NetworkManager manager;

    public BlockListener(MultiverseNets plugin, NetworkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        DeviceType type = Items.typeOf(event.getItemInHand());
        if (type == null) {
            return;
        }
        if (!type.placeable()) {
            event.setCancelled(true);
            return;
        }
        if (com.chagui68.multiversenets.util.Settings.blockedWorld(event.getBlockPlaced().getWorld())) {
            // Clasico y otros mundos vainilla: la red no existe ahi (config blocked-worlds).
            event.setCancelled(true);
            event.getPlayer().sendMessage(Text.msg("Network devices cannot be used in this world.", NamedTextColor.RED));
            return;
        }
        NodeStore.put(event.getBlockPlaced(), NodeBlob.create(type.name()));

        restoreCargo(event);

        if (type == DeviceType.MVN_RECEIVER) {
            NodeBlob blob = NodeStore.get(event.getBlockPlaced());
            Location bind = Items.readReceiverBind(event.getItemInHand());
            if (bind != null) {
                blob.txWorld = bind.getWorld().getUID().toString();
                blob.txX = bind.getBlockX();
                blob.txY = bind.getBlockY();
                blob.txZ = bind.getBlockZ();
                NodeStore.put(event.getBlockPlaced(), blob);
            }
        }

        if (type == DeviceType.MVN_CONTROLLER) {
            manager.registerController(event.getBlockPlaced());
            event.getPlayer().sendMessage(Text.msg("Controller registered. Connect nodes with cables.", NamedTextColor.GREEN));
        } else {
            manager.invalidateNear(event.getBlockPlaced());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null) {
            return;
        }
        event.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), createDropItem(type, blob));

        NodeStore.remove(block);
        if (type == DeviceType.MVN_CONTROLLER) {
            manager.removeController(block);
        } else {
            manager.invalidateNear(block);
        }
    }

    /**
     * Builds the dropped ItemStack when a network node is broken, preserving its internal state in PDC.
 *
     * Construye el ItemStack soltado al romper un nodo de red, preservando su estado interno en PDC.
     */
    private ItemStack createDropItem(DeviceType type, NodeBlob blob) {
        ItemStack item = Items.create(type);
        if (type == DeviceType.MVN_CONTROLLER || type == DeviceType.MVN_CABLE || isEmptyState(blob)) {
            return item;
        }
        var meta = item.getItemMeta();
        try {
            meta.getPersistentDataContainer().set(Keys.CELL_CARGO, PersistentDataType.STRING,
                    NodeStore.encode(blob));
        } catch (IllegalStateException error) {
            plugin.getLogger().warning("Could not embed node data: " + error.getMessage());
            return item;
        }
        List<Component> lore = new ArrayList<>();
        if (meta.hasLore()) {
            lore.addAll(meta.lore());
        }
        if (blob.cellSample != null && blob.cellAmount > 0) {
            lore.add(Component.text("Cargo: " + Items.formatAmount(blob.cellAmount) + " x "
                    + blob.cellSample.getType().name(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        } else if (blob.totalGreedyAmount() > 0) {
            lore.add(Component.text("Cargo: " + Items.formatAmount(blob.totalGreedyAmount()) + " items ("
                    + (blob.greedySamples != null ? blob.greedySamples.size() : 0) + " types)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isEmptyState(NodeBlob blob) {
        boolean matrixEmpty = true;
        for (ItemStack s : blob.craftingMatrix) {
            if (s != null && !s.getType().isAir()) {
                matrixEmpty = false;
                break;
            }
        }
        return blob.cellAmount <= 0
                && blob.totalGreedyAmount() <= 0
                && blob.filterMaterials.isEmpty()
                && blob.recipes.isEmpty()
                && blob.blueprintData.isEmpty()
                && matrixEmpty
                && blob.txWorld == null;
    }

    /**
     * Counterpart of createDropItem: restores embedded state when a preserved node is placed back into the world.
 *
     * Contraparte de createDropItem: restaura el estado embebido cuando un nodo preservado se vuelve a colocar en el mundo.
     */
    private void restoreCargo(BlockPlaceEvent event) {
        var meta = event.getItemInHand().getItemMeta();
        if (meta == null) {
            return;
        }
        String data = meta.getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING);
        if (data == null) {
            return;
        }
        NodeBlob loaded = NodeStore.decode(data);
        if (loaded == null) {
            return;
        }
        NodeBlob actual = NodeStore.get(event.getBlockPlaced());
        if (actual == null) {
            return;
        }
        actual.cellSample = loaded.cellSample;
        actual.cellAmount = loaded.cellAmount;
        actual.greedySamples = loaded.greedySamples;
        actual.greedyAmounts = loaded.greedyAmounts;
        actual.filterMaterials = loaded.filterMaterials;
        actual.filterBlacklist = loaded.filterBlacklist;
        actual.recipes = loaded.recipes;
        actual.blueprintData = loaded.blueprintData;
        actual.craftingMatrix = loaded.craftingMatrix;
        if (loaded.txWorld != null) {
            actual.txWorld = loaded.txWorld;
            actual.txX = loaded.txX;
            actual.txY = loaded.txY;
            actual.txZ = loaded.txZ;
        }
        NodeStore.put(event.getBlockPlaced(), actual);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        DeviceType heldType = Items.typeOf(held);

        // La terminal inalambrica se usa AL AIRE: antes solo se procesaban clics a bloque y el
        // aparato nunca abria nada.
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (heldType == DeviceType.MVN_WIRELESS_TERMINAL) {
                useWirelessInAir(event);
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        NodeBlob blob = NodeStore.get(block);

        // Handheld tools: handled before general menus
        if (heldType == DeviceType.MVN_PROBE) {
            event.setCancelled(true);
            probeNode(event.getPlayer(), block);
            return;
        }
        if (heldType == DeviceType.MVN_RAKE) {
            useRake(event, block, blob);
            return;
        }
        if (heldType == DeviceType.MVN_CONFIGURATOR) {
            useWrench(event, block, blob);
            return;
        }
        if (heldType == DeviceType.MVN_CRAYON) {
            useCrayon(event, block, blob);
            return;
        }

        if (blob == null) {
            if (heldType == DeviceType.MVN_WIRELESS_TERMINAL && !event.getPlayer().isSneaking()) {
                useWirelessInAir(event);
            }
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null) {
            return;
        }
        Player player = event.getPlayer();

        // Shift-click binding actions
        if ((type == DeviceType.MVN_CONTROLLER || type == DeviceType.MVN_TERMINAL) && heldType == DeviceType.MVN_WIRELESS_TERMINAL && player.isSneaking()) {
            event.setCancelled(true);
            Location targetLoc = block.getLocation();
            if (type == DeviceType.MVN_TERMINAL) {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This terminal is not connected to a network.", NamedTextColor.RED));
                    return;
                }
                targetLoc = new Location(net.world(), PosUtil.unpackX(net.controllerPos()), PosUtil.unpackY(net.controllerPos()), PosUtil.unpackZ(net.controllerPos()));
            }
            Items.bindWireless(held, targetLoc);
            player.sendMessage(Text.msg("Wireless terminal bound to this network.", NamedTextColor.GREEN));
            return;
        }
        if (type == DeviceType.MVN_TRANSMITTER && heldType == DeviceType.MVN_RECEIVER && player.isSneaking()) {
            event.setCancelled(true);
            Items.linkReceiver(held, block.getLocation());
            player.sendMessage(Text.msg("Receiver linked to this transmitter.", NamedTextColor.GREEN));
            return;
        }

        if (player.isSneaking()) {
            return;
        }

        if (held != null && held.getType().isBlock() && (type == DeviceType.MVN_CABLE || type == DeviceType.MVN_CONTROLLER)) {
            return;
        }

        switch (type) {
            case MVN_CONTROLLER -> {
                // The controller is the brain/heart of the network; no inventory GUI.
            }
            case MVN_TERMINAL, MVN_TRANSMITTER -> {
                event.setCancelled(true);
                openTerminal(player, block);
            }
            case MVN_MONITOR -> {
                event.setCancelled(true);
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This monitor is not part of a network.", NamedTextColor.RED));
                    return;
                }
                new MonitorMenu(plugin, player, net, block).openMenu();
            }
            case MVN_RECEIVER -> {
                event.setCancelled(true);
                openReceiver(player, block);
            }
            case MVN_CELL_T1, MVN_CELL_T2, MVN_CELL_T3, MVN_CELL_T4, MVN_CELL_T5, MVN_CELL_T6 -> {
                event.setCancelled(true);
                new CellMenu(plugin, player, block, type).openMenu();
            }
            case MVN_GREEDY_CELL -> {
                event.setCancelled(true);
                new GreedyMenu(plugin, player, block).openMenu();
            }
            case MVN_INFINITY_BARREL -> {
                event.setCancelled(true);
                new BarrelMenu(plugin, player, block).openMenu();
            }
            case MVN_QUANTUM_WORKBENCH -> {
                event.setCancelled(true);
                new QuantumWorkbenchMenu(plugin, player, block).openMenu();
            }
            case MVN_ENCODER -> {
                event.setCancelled(true);
                new EncoderMenu(plugin, player, block).openMenu();
            }
            case MVN_CRAFTER -> {
                event.setCancelled(true);
                new CrafterMenu(plugin, player, block).openMenu();
            }
            case MVN_CRAFTING_GRID -> {
                event.setCancelled(true);
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This grid is not part of a network.", NamedTextColor.RED));
                    return;
                }
                new CraftingGridMenu(plugin, player, net, block).openMenu();
            }
            default -> {
                if (type.filterable()) {
                    event.setCancelled(true);
                    new FilterMenu(plugin, player, block, type).openMenu();
                }
            }
        }
    }

    // ------------------------------------------------------------------ Tools / Herramientas

    /**
     * Handles Network Rake tool action: instantly removes network nodes without destroying controllers or loaded storage.
 *
     * Gestiona la acción del Rastrillo de Red: retira nodos al instante sin romper controladores ni almacenamiento con carga.
     */
    private void useRake(PlayerInteractEvent event, Block block, NodeBlob blob) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null) {
            return;
        }
        if (type == DeviceType.MVN_CONTROLLER) {
            player.sendMessage(Text.msg("The rake cannot remove a controller.", NamedTextColor.RED));
            return;
        }
        if ((type.isCell() || type == DeviceType.MVN_GREEDY_CELL || type == DeviceType.MVN_INFINITY_BARREL)
                && (blob.cellAmount > 0 || blob.totalGreedyAmount() > 0)) {
            player.sendMessage(Text.msg("The storage has cargo; empty it before raking.", NamedTextColor.RED));
            return;
        }
        block.setType(Material.AIR);
        NodeStore.remove(block);
        manager.invalidateNear(block);
        player.sendMessage(Text.msg(type.display() + " removed.", NamedTextColor.YELLOW));
        ItemStack rake = event.getItem();
        if (!Items.spendRakeUse(rake)) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(Text.msg("Your rake broke.", NamedTextColor.RED));
        }
    }

    /**
     * Handles Configuration Wrench: shift-click copies filter settings, regular click pastes.
 *
     * Gestiona la Llave de Configuración: shift-click copia la configuración de filtros, click normal la pega.
     */
    private void useWrench(PlayerInteractEvent event, Block block, NodeBlob blob) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        ItemStack wrench = event.getItem();
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null || !type.filterable()) {
            player.sendMessage(Text.msg("That device has no filter to configure.", NamedTextColor.RED));
            return;
        }
        if (player.isSneaking()) {
            Items.saveConfig(wrench, blob.filterMaterials, blob.filterBlacklist);
            player.sendMessage(Text.msg("Configuration copied (" + blob.filterMaterials.size()
                    + " materials, " + (blob.filterBlacklist ? "blacklist" : "whitelist") + ").",
                    NamedTextColor.GREEN));
            return;
        }
        String[] data = Items.readConfig(wrench);
        if (data == null) {
            player.sendMessage(Text.msg("The wrench holds no configuration (shift+click a device first).",
                    NamedTextColor.RED));
            return;
        }
        blob.filterMaterials.clear();
        String mode = data[data.length - 1];
        for (int i = 0; i < data.length - 1; i++) {
            if (Material.matchMaterial(data[i]) != null) {
                blob.filterMaterials.add(data[i]);
            }
        }
        blob.filterBlacklist = "bl".equals(mode);
        NodeStore.put(block, blob);
        player.sendMessage(Text.msg("Configuration applied.", NamedTextColor.GREEN));
    }

    /**
     * Handles Network Crayon: toggles working particle effects on the network controller.
 *
     * Gestiona el Crayón de Red: alterna los efectos visuales de partículas en el controlador.
     */
    private void useCrayon(PlayerInteractEvent event, Block block, NodeBlob blob) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type != DeviceType.MVN_CONTROLLER) {
            player.sendMessage(Text.msg("The crayon only works on a Network Controller.", NamedTextColor.RED));
            return;
        }
        blob.crayon = !blob.crayon;
        NodeStore.put(block, blob);
        manager.invalidateNear(block);
        player.sendMessage(Text.msg(blob.crayon
                ? "Network particles enabled." : "Network particles disabled.", NamedTextColor.GREEN));
    }

    // ------------------------------------------------------------------ Menus & Connections / Menús y Conexiones

    private void openReceiver(Player player, Block receiverBlock) {
        NodeBlob blob = NodeStore.get(receiverBlock);
        if (blob == null || blob.txWorld == null) {
            player.sendMessage(Text.msg("Unlinked: shift+click this item on a Transmitter first.", NamedTextColor.YELLOW));
            return;
        }
        org.bukkit.World world = plugin.getServer().getWorld(java.util.UUID.fromString(blob.txWorld));
        if (world == null) {
            player.sendMessage(Text.msg("The transmitter's world is not loaded.", NamedTextColor.RED));
            return;
        }
        Network net = manager.networkAt(world.getBlockAt(blob.txX, blob.txY, blob.txZ));
        if (net == null) {
            player.sendMessage(Text.msg("The linked transmitter has no active network.", NamedTextColor.RED));
            return;
        }
        new TerminalMenu(plugin, player, net).openMenu();
    }

    /**
     * Diagnostic probe helper that displays the network owner and status of a clicked block.
 *
     * Función auxiliar de la sonda de diagnóstico que muestra el estado y red del bloque seleccionado.
     */
    private void probeNode(Player player, Block block) {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            player.sendMessage(Text.msg("No network device found at this location.", NamedTextColor.GRAY));
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        String name = type == null ? blob.typeName : type.display();

        Network net = plugin.networks().networkAt(block);
        if (net == null) {
            player.sendMessage(Text.msg(name + ": NO NETWORK. No controller reached.",
                    NamedTextColor.RED));
            player.sendMessage(Text.msg("Check that cables are continuously connected to the controller.",
                    NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Text.msg(name + " · network of " + net.size() + " node(s)",
                NamedTextColor.GREEN));
        player.sendMessage(Text.msg("Controller at " + PosUtil.unpackX(net.controllerPos()) + ", "
                + PosUtil.unpackY(net.controllerPos()) + ", " + PosUtil.unpackZ(net.controllerPos()),
                NamedTextColor.GRAY));
        if (net.error != null && !net.error.isBlank()) {
            player.sendMessage(Text.msg("Notice: " + net.error, NamedTextColor.YELLOW));
        }
    }

    private void useWirelessInAir(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        Location bind = Items.readWirelessBind(held);
        if (bind == null) {
            event.getPlayer().sendMessage(Text.msg("Unbound: shift+click a controller.", NamedTextColor.YELLOW));
            return;
        }
        // Requisito de alcance del remote de NetworksV6: el chunk del controlador debe estar
        // cargado y la red viva; sin eso no hay nada que abrir.
        if (!bind.getWorld().isChunkLoaded(bind.getBlockX() >> 4, bind.getBlockZ() >> 4)) {
            event.getPlayer().sendMessage(Text.msg("The bound network is not loaded.", NamedTextColor.RED));
            return;
        }
        Network net = manager.networkByController(bind);
        if (net == null) {
            event.getPlayer().sendMessage(Text.msg("That network no longer exists.", NamedTextColor.RED));
            return;
        }
        event.setCancelled(true);
        new TerminalMenu(plugin, event.getPlayer(), net).openMenu();
    }

    private void openTerminal(Player player, Block nodeBlock) {
        Network net = manager.networkAt(nodeBlock);
        if (net == null) {
            player.sendMessage(Text.msg("No network found for this node.", NamedTextColor.RED));
            return;
        }
        new TerminalMenu(plugin, player, net).openMenu();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (NodeStore.chunkHasNodes(block.getChunk()) && NodeStore.hasNode(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (NodeStore.chunkHasNodes(block.getChunk()) && NodeStore.hasNode(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> NodeStore.chunkHasNodes(block.getChunk()) && NodeStore.hasNode(block));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> NodeStore.chunkHasNodes(block.getChunk()) && NodeStore.hasNode(block));
    }
}
