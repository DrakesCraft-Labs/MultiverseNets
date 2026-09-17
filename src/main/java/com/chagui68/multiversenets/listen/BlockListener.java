package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.gui.CellMenu;
import com.chagui68.multiversenets.gui.CrafterMenu;
import com.chagui68.multiversenets.gui.CraftingGridMenu;
import com.chagui68.multiversenets.gui.EncoderMenu;
import com.chagui68.multiversenets.gui.FilterMenu;
import com.chagui68.multiversenets.gui.MonitorMenu;
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
        NodeStore.put(event.getBlockPlaced(), NodeBlob.create(type.name()));

        restaurarCarga(event);

        if (type == DeviceType.RECEIVER) {
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

        if (type == DeviceType.CONTROLLER) {
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
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), suelto(type, blob));

        NodeStore.remove(block);
        if (type == DeviceType.CONTROLLER) {
            manager.removeController(block);
        } else {
            manager.invalidateNear(block);
        }
    }

    /**
     * Lo que suelta romper un nodo. Todo el estado viaja DENTRO del item: el contenido de las
     * celdas y la greedy, los filtros, las recetas y los blueprints del crafter, la matriz de
     * la parrilla y el encoder, y el enlace del receptor. Al recolocarlo, sigue como estaba.
     * (Es el mismo gesto que NetworkQuantumStorage.onBreak en NetworksV6, pero completo.)
     */
    private ItemStack suelto(DeviceType type, NodeBlob blob) {
        ItemStack item = Items.create(type);
        if (type == DeviceType.CONTROLLER || type == DeviceType.CABLE || sinDatos(blob)) {
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
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean sinDatos(NodeBlob blob) {
        boolean matrizVacia = true;
        for (ItemStack s : blob.craftingMatrix) {
            if (s != null && !s.getType().isAir()) {
                matrizVacia = false;
                break;
            }
        }
        return blob.cellAmount <= 0
                && blob.filterMaterials.isEmpty()
                && blob.recipes.isEmpty()
                && blob.blueprintData.isEmpty()
                && matrizVacia
                && blob.txWorld == null;
    }

    /** Contraparte de {@link #suelto}: si el nodo colocado trae estado embebido, se restaura. */
    private void restaurarCarga(BlockPlaceEvent event) {
        var meta = event.getItemInHand().getItemMeta();
        if (meta == null) {
            return;
        }
        String data = meta.getPersistentDataContainer().get(Keys.CELL_CARGO, PersistentDataType.STRING);
        if (data == null) {
            return;
        }
        NodeBlob cargada = NodeStore.decode(data);
        if (cargada == null) {
            return;
        }
        NodeBlob actual = NodeStore.get(event.getBlockPlaced());
        if (actual == null) {
            return;
        }
        actual.cellSample = cargada.cellSample;
        actual.cellAmount = cargada.cellAmount;
        actual.filterMaterials = cargada.filterMaterials;
        actual.filterBlacklist = cargada.filterBlacklist;
        actual.recipes = cargada.recipes;
        actual.blueprintData = cargada.blueprintData;
        actual.craftingMatrix = cargada.craftingMatrix;
        if (cargada.txWorld != null) {
            actual.txWorld = cargada.txWorld;
            actual.txX = cargada.txX;
            actual.txY = cargada.txY;
            actual.txZ = cargada.txZ;
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
            if (heldType == DeviceType.WIRELESS_TERMINAL) {
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

        // Herramientas de mano: van por delante de todo lo demas.
        if (heldType == DeviceType.PROBE) {
            event.setCancelled(true);
            sondear(event.getPlayer(), block);
            return;
        }
        if (heldType == DeviceType.RAKE) {
            usarRake(event, block, blob);
            return;
        }
        if (heldType == DeviceType.CONFIGURATOR) {
            usarWrench(event, block, blob);
            return;
        }
        if (heldType == DeviceType.CRAYON) {
            usarCrayon(event, block, blob);
            return;
        }

        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null) {
            return;
        }
        Player player = event.getPlayer();

        // Vinculos con shift.
        if (type == DeviceType.CONTROLLER && heldType == DeviceType.WIRELESS_TERMINAL && player.isSneaking()) {
            event.setCancelled(true);
            Items.bindWireless(held, block.getLocation());
            player.sendMessage(Text.msg("Wireless terminal bound to this network.", NamedTextColor.GREEN));
            return;
        }
        if (type == DeviceType.TRANSMITTER && heldType == DeviceType.RECEIVER && player.isSneaking()) {
            event.setCancelled(true);
            Items.linkReceiver(held, block.getLocation());
            player.sendMessage(Text.msg("Receiver linked to this transmitter.", NamedTextColor.GREEN));
            return;
        }

        // Agachado con la mano vacia o una herramienta tampoco abre menus (convencion Slimefun).
        if (player.isSneaking()) {
            return;
        }

        // Si el jugador sostiene un bloque y el nodo no tiene un menu principal (p.ej. un cable),
        // deja que vanilla coloque el bloque contra la cara del nodo. Si es un menu (Terminal,
        // Celda, etc.) y NO esta agachado, la apertura del menu tiene prioridad.
        if (held != null && held.getType().isBlock() && (type == DeviceType.CABLE || type == DeviceType.PURGER)) {
            return;
        }

        event.setCancelled(true);
        switch (type) {
            case CONTROLLER, TERMINAL, TRANSMITTER -> openTerminal(player, block);
            case MONITOR -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This monitor is not part of a network.", NamedTextColor.RED));
                    return;
                }
                new MonitorMenu(plugin, player, net, block).openMenu();
            }
            case RECEIVER -> openReceiver(player, block);
            case CELL_T1, CELL_T2, CELL_T3, CELL_T4, CELL_T5, CELL_T6 ->
                    new CellMenu(plugin, player, block, type).openMenu();
            case ENCODER -> new EncoderMenu(plugin, player, block).openMenu();
            case CRAFTER -> new CrafterMenu(plugin, player, block).openMenu();
            case CRAFTING_GRID -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This grid is not part of a network.", NamedTextColor.RED));
                    return;
                }
                new CraftingGridMenu(plugin, player, net, block).openMenu();
            }
            default -> {
                if (type.filterable()) {
                    new FilterMenu(plugin, player, block, type).openMenu();
                }
            }
        }
    }

    // ------------------------------------------------------------------ herramientas

    /**
     * Network Rake (de NetworksV6): quita nodos de la red al instante, con usos. No rompe el
     * controlador (sin el la red muere) ni celdas con carga (el contenido se perderia).
     */
    private void usarRake(PlayerInteractEvent event, Block block, NodeBlob blob) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null) {
            return;
        }
        if (type == DeviceType.CONTROLLER) {
            player.sendMessage(Text.msg("The rake cannot remove a controller.", NamedTextColor.RED));
            return;
        }
        if ((type.isCell() || type == DeviceType.GREEDY_CELL) && blob.cellAmount > 0) {
            player.sendMessage(Text.msg("The cell has cargo; empty it before raking.", NamedTextColor.RED));
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
     * Configuration Wrench (el NetworkConfigurator de NetworksV6): shift+click copia el filtro
     * del dispositivo, click normal lo pega (y consume nada: los filtros aqui son materiales).
     */
    private void usarWrench(PlayerInteractEvent event, Block block, NodeBlob blob) {
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
     * Network Crayon (NetworksV6): marca el controlador para que la red eche particulas cuando
     * sus maquinas trabajan. Se guarda en el blob del controlador y el scan lo propaga.
     */
    private void usarCrayon(PlayerInteractEvent event, Block block, NodeBlob blob) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type != DeviceType.CONTROLLER) {
            player.sendMessage(Text.msg("The crayon only works on a Network Controller.", NamedTextColor.RED));
            return;
        }
        blob.crayon = !blob.crayon;
        NodeStore.put(block, blob);
        manager.invalidateNear(block);
        player.sendMessage(Text.msg(blob.crayon
                ? "Network particles enabled." : "Network particles disabled.", NamedTextColor.GREEN));
    }

    // ------------------------------------------------------------------ menus y enlaces

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
     * Dice de que red es un bloque, o que no es nada.
     */
    private void sondear(Player player, Block block) {
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            player.sendMessage(Text.msg("Aqui no hay ningun dispositivo de red.", NamedTextColor.GRAY));
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        String nombre = type == null ? blob.typeName : type.display();

        Network red = plugin.networks().networkAt(block);
        if (red == null) {
            player.sendMessage(Text.msg(nombre + ": SIN RED. No lo alcanza ningun controlador.",
                    NamedTextColor.RED));
            player.sendMessage(Text.msg("Revisa que haya cables continuos hasta el controlador.",
                    NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Text.msg(nombre + " · red de " + red.size() + " nodo(s)",
                NamedTextColor.GREEN));
        player.sendMessage(Text.msg("Controlador en " + PosUtil.unpackX(red.controllerPos()) + ", "
                + PosUtil.unpackY(red.controllerPos()) + ", " + PosUtil.unpackZ(red.controllerPos()),
                NamedTextColor.GRAY));
        if (red.error != null && !red.error.isBlank()) {
            player.sendMessage(Text.msg("Aviso: " + red.error, NamedTextColor.YELLOW));
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
            if (NodeStore.get(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (NodeStore.get(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> NodeStore.get(block) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> NodeStore.get(block) != null);
    }
}
