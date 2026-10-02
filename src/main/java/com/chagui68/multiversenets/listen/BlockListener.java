package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Keys;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockListener implements Listener {

    private static final java.util.Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();

    private final MultiverseNets plugin;
    private final NetworkManager manager;
    /** Menu dispatch and the player-facing protection gate. See {@link DeviceInteractions}. */
    private final DeviceInteractions interactions;

    public BlockListener(MultiverseNets plugin, NetworkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.interactions = new DeviceInteractions(plugin, manager);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player p) {
            LAST_COMBAT.put(p.getUniqueId(), System.currentTimeMillis());
        }
        if (event.getDamager() instanceof Player attacker) {
            LAST_COMBAT.put(attacker.getUniqueId(), System.currentTimeMillis());
        }
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
        if (NodeStore.countNodesInChunk(event.getBlockPlaced().getChunk()) >= Settings.maxNodesPerChunk()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Text.msg("Chunk device limit reached! Maximum "
                    + Settings.maxNodesPerChunk() + " network devices per chunk.", NamedTextColor.RED));
            return;
        }
        NodeStore.put(event.getBlockPlaced(), NodeBlob.create(type.name()));

        restoreCargo(event);

        if (type == DeviceType.MVN_RECEIVER || type == DeviceType.MVN_TRANSMITTER) {
            NodeBlob blob = NodeStore.get(event.getBlockPlaced());
            Location bind = Items.readReceiverBind(event.getItemInHand());
            if (bind != null && blob != null) {
                blob.txWorld = bind.getWorld().getUID().toString();
                blob.txX = bind.getBlockX();
                blob.txY = bind.getBlockY();
                blob.txZ = bind.getBlockZ();
                NodeStore.put(event.getBlockPlaced(), blob);
            }
        }

        if (type == DeviceType.MVN_CONTROLLER) {
            // El controlador recuerda quien lo coloco: es la identidad de la red. Sin ella, la red
            // no puede demostrar que el claim donde la construiste es tuyo y el escaneo la corta en
            // la frontera, dejando el controlador solo en una red de 1 nodo.
            NodeBlob ctrlBlob = NodeStore.get(event.getBlockPlaced());
            if (ctrlBlob != null) {
                ctrlBlob.ownerUuid = event.getPlayer().getUniqueId().toString();
                NodeStore.put(event.getBlockPlaced(), ctrlBlob);
            }
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
        if (type.isCell()) {
            if (block.getState() instanceof org.bukkit.block.Container container) {
                container.getInventory().clear();
            }
            if (Settings.compatSlimefun() && SlimefunBridge.isAvailable()) {
                SlimefunBridge.unregisterCell(block);
            }
        }
        // El modulo de un DRAM Bay sale como item con todo su stock, tambien en creativo: el
        // stock es del jugador, no del bloque.
        ItemStack module = type == DeviceType.MVN_DRAM_BAY
                ? com.chagui68.multiversenets.net.MemoryModules.eject(blob) : null;
        if (module != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), module);
        }
        // Modulos recuperados de un Controlador antiguo que nadie recogio del Terminal: salen al
        // suelo con su stock en vez de perderse con el bloque.
        if (blob.recoveredModules != null) {
            for (ItemStack recovered : blob.recoveredModules) {
                if (recovered != null && !recovered.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), recovered);
                }
            }
            blob.recoveredModules = new ArrayList<>();
        }
        if (blob.encoderBlank != null && !blob.encoderBlank.getType().isAir()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), blob.encoderBlank);
            blob.encoderBlank = null;
        }
        if (blob.encoderOutput != null && !blob.encoderOutput.getType().isAir()) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), blob.encoderOutput);
            blob.encoderOutput = null;
        }
        if (event.getPlayer().getGameMode() != org.bukkit.GameMode.CREATIVE) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), createDropItem(type, blob));
        }

        NodeStore.remove(block);
        if (type == DeviceType.MVN_CONTROLLER) {
            long pos = PosUtil.pack(block.getX(), block.getY(), block.getZ());
            com.chagui68.multiversenets.net.NetworkHologramManager.removeHologram(block.getWorld(), pos);
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
        if (type == DeviceType.MVN_CABLE || isEmptyState(blob)) {
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
        } else if (blob.virtualCacheTier > 0) {
            String tierName = switch (blob.virtualCacheTier) {
                case 1 -> "L1 CPU Cache";
                case 2 -> "L2 CPU Cache";
                case 3 -> "L3 CPU Cache";
                case 4 -> "System DRAM";
                case 5 -> "Quantum Cache";
                default -> "T" + blob.virtualCacheTier;
            };
            lore.add(Component.text("CPU Cache: " + tierName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            if (blob.totalVirtualAmount() > 0) {
                lore.add(Component.text("Virtual Cargo: " + Items.formatAmount(blob.totalVirtualAmount()) + " items ("
                        + (blob.virtualSamples != null ? blob.virtualSamples.size() : 0) + " types)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            }
        } else if (type == DeviceType.MVN_LIMITER && blob.quotaSample != null) {
            lore.add(Component.text("Target: " + blob.quotaSample.getType().name(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Limit: " + Items.formatAmount(blob.quotaLimit) + (blob.quotaActive ? " (Active)" : " (Disabled)"), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        } else if (type == DeviceType.MVN_FLUID_CELL && blob.fluidType != null && blob.fluidAmount > 0) {
            lore.add(Component.text("Fluid: " + blob.fluidType + " (" + Items.formatAmount(blob.fluidAmount) + " mB / "
                    + (blob.fluidAmount / 1000) + " Buckets)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        } else if (type == DeviceType.MVN_LIQUID_PUMP && blob.pumpFluid != null) {
            lore.add(Component.text("Fluid filter: " + blob.pumpFluid,
                    NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
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
                && blob.virtualCacheTier <= 0
                && blob.totalVirtualAmount() <= 0
                && blob.filterMaterials.isEmpty()
                && (blob.filterItems == null || blob.filterItems.isEmpty())
                && blob.targetFace == null
                && !blob.hasTransit()
                && blob.recipes.isEmpty()
                && blob.blueprintData.isEmpty()
                && matrixEmpty
                && blob.quotaSample == null
                && blob.quotaLimit <= 0
                && blob.txWorld == null
                && (blob.fluidType == null || blob.fluidAmount <= 0)
                && blob.pumpMode == null
                && blob.pumpFluid == null
                && blob.installedModule == null
                && !blob.chickenActive
                && !blob.chickenPull
                && (blob.chickenProducts == null || blob.chickenProducts.isEmpty())
                && blob.chickenMinTier == null
                && blob.chickenMaxTier == null
                && blob.chickenKnown == null
                && blob.chickenAge == null
                && blob.chickenMinStrength <= 0
                && !blob.chickenPureOnly;
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
        // filterItems manda sobre filterMaterials en NetworkManager.filterPredicate: sin
        // restaurarlo, un filtro de un item de Slimefun o con nombre volvia como "solo material".
        actual.filterItems = loaded.filterItems != null ? new ArrayList<>(loaded.filterItems) : new ArrayList<>();
        actual.filterBlacklist = loaded.filterBlacklist;
        actual.targetFace = loaded.targetFace;
        actual.recipes = loaded.recipes;
        actual.blueprintData = loaded.blueprintData;
        actual.craftingMatrix = loaded.craftingMatrix;
        actual.virtualCacheTier = loaded.virtualCacheTier;
        actual.virtualSamples = loaded.virtualSamples != null ? new ArrayList<>(loaded.virtualSamples) : new ArrayList<>();
        actual.virtualAmounts = loaded.virtualAmounts != null ? new ArrayList<>(loaded.virtualAmounts) : new ArrayList<>();
        actual.setTransit(loaded.transitStack());
        actual.quotaSample = loaded.quotaSample;
        actual.quotaLimit = loaded.quotaLimit;
        actual.quotaActive = loaded.quotaActive;
        actual.fluidType = loaded.fluidType;
        actual.fluidAmount = loaded.fluidAmount;
        actual.pumpMode = loaded.pumpMode;
        actual.pumpFluid = loaded.pumpFluid;
        actual.chickenActive = loaded.chickenActive;
        actual.chickenPull = loaded.chickenPull;
        actual.chickenProducts = loaded.chickenProducts != null ? new ArrayList<>(loaded.chickenProducts) : new ArrayList<>();
        actual.chickenMinTier = loaded.chickenMinTier;
        actual.chickenMaxTier = loaded.chickenMaxTier;
        actual.chickenKnown = loaded.chickenKnown;
        actual.chickenAge = loaded.chickenAge;
        actual.chickenMinStrength = loaded.chickenMinStrength;
        actual.chickenPureOnly = loaded.chickenPureOnly;
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

        if (blob != null && !interactions.canAccessNetwork(event.getPlayer(), block.getLocation())) {
            event.getPlayer().sendMessage(Text.msg("You do not have permission to access network devices in this protected area.", NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

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

        if (blob == null) {
            // Bloque que no es un dispositivo registrado. Agachado no se cancela nada: en vanilla
            // agachado + click derecho con un bloque en la mano lo coloca contra la cara, y con la
            // mano vacia simplemente no pasa nada. Aqui no hay interfaz que abrir en ningun caso.
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
        if (type == DeviceType.MVN_RECEIVER && heldType == DeviceType.MVN_TRANSMITTER && player.isSneaking()) {
            event.setCancelled(true);
            Items.linkReceiver(held, block.getLocation());
            player.sendMessage(Text.msg("Transmitter linked to this receiver.", NamedTextColor.GREEN));
            return;
        }

        // Agachado + click derecho no abre ninguna interfaz, igual que en vanilla: se devuelve
        // antes de tocar nada mas, asi que ningun camino de aqui abajo puede abrir un GUI.
        if (player.isSneaking()) {
            return;
        }

        // Con un bloque en la mano no se abre el menu de un cable ni de un controlador, para que
        // el clic placement-place en vez de entrar al GUI. Un cable no tiene menu, pero el
        // controlador si, y ahi este corte es el que evita el conflicto.
        if (held != null && held.getType().isBlock() && (type == DeviceType.MVN_CABLE || type == DeviceType.MVN_CONTROLLER)) {
            return;
        }

        if (type == DeviceType.MVN_CABLE) {
            Network net = manager.networkAt(block);
            if (net == null) {
                player.sendActionBar(Component.text("⚠ MultiverseNets: Disconnected (No Controller reached)", NamedTextColor.RED));
                player.sendMessage(Text.msg("MultiverseNets Cable: Disconnected! No controller reached. Ensure continuous connection to an active Controller.", NamedTextColor.RED));
            } else {
                player.sendActionBar(Component.text("✔ MultiverseNets: Connected (" + net.size() + " nodes)", NamedTextColor.GREEN));
            }
            return;
        }

        if (type == DeviceType.MVN_CONTROLLER && heldType != null && heldType.isMemoryModule()) {
            event.setCancelled(true);
            player.sendMessage(Text.msg("Memory modules go in a DRAM Bay connected to the network.", NamedTextColor.YELLOW));
            return;
        }

        if (type == DeviceType.MVN_DRAM_BAY && heldType != null && heldType.isMemoryModule()
                && com.chagui68.multiversenets.net.MemoryModules.installed(blob) == null) {
            event.setCancelled(true);
            interactions.installMemoryModule(player, block, blob, held);
            return;
        }

        if (type == DeviceType.MVN_FLUID_CELL && held != null) {
            if (interactions.handleFluidCellQuickInteract(player, block, blob, held)) {
                event.setCancelled(true);
                return;
            }
        }

        if (interactions.openDeviceMenu(player, block, blob, type)) {
            event.setCancelled(true);
        }
    }

    /**
     * EN: Opens the GUI interface of an adjacent/target block (MultiverseNets device, Slimefun BlockMenu, or Vanilla container).
     * ES: Abre la interfaz gráfica de un bloque objetivo o adyacente (nodo MultiverseNets, máquina Slimefun o contenedor Vanilla).
     */
    public boolean openTargetBlockInterface(Player player, Block candidate) {
        // La logica vive en DeviceInteractions; esto queda como la costura publica del listener
        // para que FilterMenu no tenga que saber adonde se movio el enrutado de menus.
        return interactions.openTargetBlockInterface(player, candidate);
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
        if ((type.isCell() || type == DeviceType.MVN_GREEDY_CELL || type == DeviceType.MVN_INFINITY_BARREL
                || type == DeviceType.MVN_FLUID_CELL)
                && (blob.cellAmount > 0 || blob.totalGreedyAmount() > 0 || blob.fluidAmount > 0)) {
            player.sendMessage(Text.msg("The storage has cargo; empty it before raking.", NamedTextColor.RED));
            return;
        }
        // El rastrillo desmonta y RECUPERA el nodo, como en Networks: antes ponia el bloque en aire
        // y el dispositivo (con su filtro o sus planos) se perdia sin dejar nada.
        ItemStack module = type == DeviceType.MVN_DRAM_BAY
                ? com.chagui68.multiversenets.net.MemoryModules.eject(blob) : null;
        ItemStack recovered = createDropItem(type, blob);
        if (type.isCell() && Settings.compatSlimefun() && SlimefunBridge.isAvailable()) {
            SlimefunBridge.unregisterCell(block);
        }
        block.setType(Material.AIR);
        NodeStore.remove(block);
        manager.invalidateNear(block);
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            for (ItemStack overflow : player.getInventory().addItem(recovered).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
        if (module != null) {
            for (ItemStack overflow : player.getInventory().addItem(module).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
        }
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
            Items.saveConfigItems(wrench, blob.filterItems);
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
        // filterItems tiene prioridad sobre filterMaterials en el predicado de filtro: si no se
        // sustituye, el destino conservaba su filtro anterior y el pegado no tenia efecto.
        blob.filterItems = Items.readConfigItems(wrench);
        blob.filterBlacklist = "bl".equals(mode);
        NodeStore.put(block, blob);
        player.sendMessage(Text.msg("Configuration applied.", NamedTextColor.GREEN));
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
        if (net.linksBlockedByProtection() > 0) {
            player.sendMessage(Text.msg("This network was cut at protected land: "
                    + net.linksBlockedByProtection() + " link(s) were not indexed.", NamedTextColor.YELLOW));
            player.sendMessage(Text.msg("Place the Controller as the land's owner, or list the spot "
                    + "under protection.exempt-locations, so the network can reach its own machines.",
                    NamedTextColor.GRAY));
        }
    }

    private void useWirelessInAir(PlayerInteractEvent event) {
        ItemStack held = event.getItem();
        Player player = event.getPlayer();
        Location bind = Items.readWirelessBind(held);
        if (bind == null) {
            player.sendMessage(Text.msg("Unbound: shift+click a controller.", NamedTextColor.YELLOW));
            return;
        }

        // 1. Modality restriction: Blocked worlds
        if (Settings.blockedWorld(player.getWorld())) {
            player.sendMessage(Text.msg("Wireless network devices cannot be used in this world.", NamedTextColor.RED));
            return;
        }

        // 2. Combat restriction
        long lastDmg = LAST_COMBAT.getOrDefault(player.getUniqueId(), 0L);
        long combatCooldownMs = Settings.wirelessCombatCooldownSeconds() * 1000L;
        if (System.currentTimeMillis() - lastDmg < combatCooldownMs) {
            player.sendMessage(Text.msg("Cannot access wireless terminal while in active combat!", NamedTextColor.RED));
            return;
        }

        // 3. Modality restriction: BentoBox / Skyblock Island ownership check
        if (!interactions.canAccessNetwork(player, bind)) {
            player.sendMessage(Text.msg("You do not have permission to access network devices in this protected area.", NamedTextColor.RED));
            return;
        }

        Network net = manager.networkByController(bind);
        if (net == null) {
            net = manager.networkAt(bind.getBlock());
            if (net == null) {
                player.sendMessage(Text.msg("The bound network is not loaded or no longer exists.", NamedTextColor.RED));
                return;
            }
        }

        // 4. Router Antenna check: Router allows global and cross-dimension access
        boolean hasRouter = net.count(DeviceType.MVN_ROUTER) > 0;
        if (!hasRouter) {
            if (!player.getWorld().equals(bind.getWorld())) {
                player.sendMessage(Text.msg("Wireless terminal out of range: Network is in world '" + bind.getWorld().getName() + "'. Install a Network Router antenna to access across dimensions.", NamedTextColor.RED));
                return;
            }
            int maxLocalDist = Settings.wirelessLocalRange();
            double distSq = player.getLocation().distanceSquared(bind);
            if (distSq > ((double) maxLocalDist * maxLocalDist)) {
                player.sendMessage(Text.msg("Signal lost! Install a Network Router antenna to access globally.", NamedTextColor.RED));
                return;
            }
        }

        event.setCancelled(true);
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

    /**
     * EN: Hoppers (and hopper minecarts) never interact with a network device, in either
     * direction. The event is only cancelled, never used to move items by hand: Paper shrinks the
     * hopper's slot to the moved amount while the event runs, so the old code that called
     * {@code removeItem} on the hopper here wiped the whole stack and stored only one item.
     *
     * ES: Las tolvas (y las vagonetas con tolva) nunca interactúan con un dispositivo de la red, en
     * ningún sentido. El evento solo se cancela, nunca se usa para mover ítems a mano: Paper reduce
     * la ranura de la tolva a la cantidad movida mientras dura el evento, así que el código antiguo
     * que llamaba a {@code removeItem} aquí borraba el stack entero y guardaba un solo ítem.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isNodeInventory(event.getDestination()) || isNodeInventory(event.getSource())) {
            event.setCancelled(true);
        }
    }

    private static boolean isNodeInventory(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        // getHolder(false) evita copiar el estado del bloque en cada movimiento de tolva.
        org.bukkit.inventory.InventoryHolder holder;
        try {
            holder = inventory.getHolder(false);
        } catch (RuntimeException unsupported) {
            holder = inventory.getHolder();
        }
        if (!(holder instanceof org.bukkit.block.BlockState state)) {
            // Vagonetas, cofres dobles y demas: ningun nodo de la red es uno de ellos.
            return false;
        }
        Block block = state.getBlock();
        return NodeStore.chunkHasNodes(block.getChunk()) && NodeStore.hasNode(block);
    }
}
