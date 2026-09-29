package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.ProtectionBridge;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.gui.BarrelMenu;
import com.chagui68.multiversenets.gui.CellMenu;
import com.chagui68.multiversenets.gui.CrafterMenu;
import com.chagui68.multiversenets.gui.CraftingGridMenu;
import com.chagui68.multiversenets.gui.EncoderMenu;
import com.chagui68.multiversenets.gui.FilterMenu;
import com.chagui68.multiversenets.gui.GreedyMenu;
import com.chagui68.multiversenets.gui.FluidCellMenu;
import com.chagui68.multiversenets.gui.LiquidPumpMenu;
import com.chagui68.multiversenets.gui.MonitorMenu;
import com.chagui68.multiversenets.gui.QuotaLimiterMenu;
import com.chagui68.multiversenets.gui.QuantumWorkbenchMenu;
import com.chagui68.multiversenets.gui.RequestTerminalMenu;
import com.chagui68.multiversenets.gui.SfEncoderMenu;
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
import com.chagui68.multiversenets.gui.ControllerMenu;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlockListener implements Listener {

    private static final java.util.Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();

    private final MultiverseNets plugin;
    private final NetworkManager manager;

    public BlockListener(MultiverseNets plugin, NetworkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
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
        if (type.isCell()) {
            if (block.getState() instanceof org.bukkit.block.Container container) {
                container.getInventory().clear();
            }
            if (Settings.compatSlimefun() && SlimefunBridge.isAvailable()) {
                SlimefunBridge.unregisterCell(block);
            }
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
        } else if (type == DeviceType.MVN_LIQUID_PUMP && blob.pumpMode != null) {
            lore.add(Component.text("Mode: " + blob.pumpMode + (blob.pumpFluid != null ? " (" + blob.pumpFluid + ")" : ""),
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
                && blob.recipes.isEmpty()
                && blob.blueprintData.isEmpty()
                && matrixEmpty
                && blob.quotaSample == null
                && blob.quotaLimit <= 0
                && blob.txWorld == null
                && (blob.fluidType == null || blob.fluidAmount <= 0)
                && blob.pumpMode == null;
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
        actual.virtualCacheTier = loaded.virtualCacheTier;
        actual.virtualSamples = loaded.virtualSamples != null ? new ArrayList<>(loaded.virtualSamples) : new ArrayList<>();
        actual.virtualAmounts = loaded.virtualAmounts != null ? new ArrayList<>(loaded.virtualAmounts) : new ArrayList<>();
        actual.transitBuffer = loaded.transitBuffer;
        actual.quotaSample = loaded.quotaSample;
        actual.quotaLimit = loaded.quotaLimit;
        actual.quotaActive = loaded.quotaActive;
        actual.fluidType = loaded.fluidType;
        actual.fluidAmount = loaded.fluidAmount;
        actual.pumpMode = loaded.pumpMode;
        actual.pumpFluid = loaded.pumpFluid;
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

        if (blob != null && !canAccessNetwork(event.getPlayer(), block.getLocation())) {
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

        if (type == DeviceType.MVN_CONTROLLER && heldType != null && heldType.isCacheModule()) {
            event.setCancelled(true);
            installCacheModule(player, block, blob, heldType, held);
            return;
        }

        if (type == DeviceType.MVN_FLUID_CELL && held != null) {
            if (handleFluidCellQuickInteract(player, block, blob, held)) {
                event.setCancelled(true);
                return;
            }
        }

        if (openDeviceMenu(player, block, blob, type)) {
            event.setCancelled(true);
        }
    }

    /**
     * EN: Opens the GUI interface of an adjacent/target block (MultiverseNets device, Slimefun BlockMenu, or Vanilla container).
     * ES: Abre la interfaz gráfica de un bloque objetivo o adyacente (nodo MultiverseNets, máquina Slimefun o contenedor Vanilla).
     */
    public boolean openTargetBlockInterface(Player player, Block candidate) {
        if (candidate == null || player == null) {
            return false;
        }
        if (!canAccessNetwork(player, candidate.getLocation())) {
            player.sendMessage(Text.msg("You do not have permission to access devices in this protected area.", NamedTextColor.RED));
            return false;
        }

        // A. Check MultiverseNets device
        NodeBlob candidateBlob = NodeStore.get(candidate);
        if (candidateBlob != null) {
            DeviceType candType = DeviceType.parse(candidateBlob.typeName);
            if (candType != null && openDeviceMenu(player, candidate, candidateBlob, candType)) {
                return true;
            }
        }

        // B. Check Slimefun machine BlockMenu
        if (Settings.compatSlimefun() && SlimefunBridge.isAvailable()) {
            if (SlimefunBridge.openSlimefunMenu(candidate, player)) {
                return true;
            }
        }

        // C. Check Vanilla Container or interactive block
        if (openVanillaInterface(candidate, player)) {
            return true;
        }
        return false;
    }

    private boolean openVanillaInterface(Block block, Player player) {
        if (block == null || player == null) {
            return false;
        }
        // Vanilla Containers
        if (block.getState() instanceof org.bukkit.block.Container container) {
            player.openInventory(container.getInventory());
            return true;
        }
        // Vanilla Interactive blocks
        Material mat = block.getType();
        Location loc = block.getLocation();
        switch (mat) {
            case CRAFTING_TABLE -> {
                player.openWorkbench(loc, true);
                return true;
            }
            case ENCHANTING_TABLE -> {
                player.openEnchanting(loc, true);
                return true;
            }
            case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> {
                player.openAnvil(loc, true);
                return true;
            }
            case SMITHING_TABLE -> {
                player.openSmithingTable(loc, true);
                return true;
            }
            case GRINDSTONE -> {
                player.openGrindstone(loc, true);
                return true;
            }
            case STONECUTTER -> {
                player.openStonecutter(loc, true);
                return true;
            }
            case LOOM -> {
                player.openLoom(loc, true);
                return true;
            }
            case CARTOGRAPHY_TABLE -> {
                player.openCartographyTable(loc, true);
                return true;
            }
            case ENDER_CHEST -> {
                player.openInventory(player.getEnderChest());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean openDeviceMenu(Player player, Block block, NodeBlob blob, DeviceType type) {
        switch (type) {
            case MVN_CONTROLLER -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This controller is not active.", NamedTextColor.RED));
                    return true;
                }
                new ControllerMenu(plugin, player, net, block).openMenu();
                return true;
            }
            case MVN_TERMINAL, MVN_TRANSMITTER -> {
                openTerminal(player, block);
                return true;
            }
            case MVN_MONITOR -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This monitor is not part of a network.", NamedTextColor.RED));
                    return true;
                }
                new MonitorMenu(plugin, player, net, block).openMenu();
                return true;
            }
            case MVN_RECEIVER -> {
                openReceiver(player, block);
                return true;
            }
            case MVN_CELL_T1, MVN_CELL_T2, MVN_CELL_T3, MVN_CELL_T4, MVN_CELL_T5, MVN_CELL_T6 -> {
                new CellMenu(plugin, player, block, type).openMenu();
                return true;
            }
            case MVN_GREEDY_CELL -> {
                new GreedyMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_INFINITY_BARREL -> {
                new BarrelMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_QUANTUM_WORKBENCH -> {
                new QuantumWorkbenchMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_LIMITER -> {
                new QuotaLimiterMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_ENCODER -> {
                new EncoderMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_SF_ENCODER -> {
                if (!Settings.sfEncoderEnabled()) {
                    player.sendMessage(Text.msg("Slimefun Recipe Encoder is disabled on this server.", NamedTextColor.RED));
                    return true;
                }
                new SfEncoderMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_CRAFTER, MVN_REQUEST_CRAFTER, MVN_SF_CRAFTER, MVN_SF_REQUEST_CRAFTER -> {
                if ((type == DeviceType.MVN_SF_CRAFTER || type == DeviceType.MVN_SF_REQUEST_CRAFTER) && !Settings.sfCrafterEnabled()) {
                    player.sendMessage(Text.msg("Slimefun Crafters are disabled on this server.", NamedTextColor.RED));
                    return true;
                }
                new CrafterMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_CRAFTING_GRID -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This grid is not part of a network.", NamedTextColor.RED));
                    return true;
                }
                new CraftingGridMenu(plugin, player, net, block).openMenu();
                return true;
            }
            case MVN_FLUID_CELL -> {
                new FluidCellMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_LIQUID_PUMP -> {
                new LiquidPumpMenu(plugin, player, block).openMenu();
                return true;
            }
            case MVN_REQUEST_TERMINAL -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This request terminal is not connected to a network.", NamedTextColor.RED));
                    return true;
                }
                new RequestTerminalMenu(plugin, player, net, block).openMenu();
                return true;
            }
            default -> {
                if (type.filterable()) {
                    new FilterMenu(plugin, player, block, type).openMenu();
                    return true;
                }
                return false;
            }
        }
    }

    private boolean handleFluidCellQuickInteract(Player player, Block block, NodeBlob blob, ItemStack held) {
        Material mat = held.getType();
        long capacity = Settings.fluidCellCapacity();

        // 1. Filled containers -> Deposit
        String depositingFluid = null;
        int mbPerItem = 1000;
        Material returnMat = Material.BUCKET;

        if (mat == Material.WATER_BUCKET) {
            depositingFluid = "WATER";
        } else if (mat == Material.LAVA_BUCKET) {
            depositingFluid = "LAVA";
        } else if (mat == Material.MILK_BUCKET) {
            depositingFluid = "MILK";
        } else if (mat == Material.POWDER_SNOW_BUCKET) {
            depositingFluid = "POWDER_SNOW";
        } else if (mat == Material.HONEY_BOTTLE) {
            depositingFluid = "HONEY";
            mbPerItem = 250;
            returnMat = Material.GLASS_BOTTLE;
        }

        if (depositingFluid != null) {
            if (blob.fluidType != null && !blob.fluidType.equalsIgnoreCase(depositingFluid) && blob.fluidAmount > 0) {
                player.sendMessage(Text.msg("Fluid cell already contains " + blob.fluidType + "!", NamedTextColor.RED));
                return true;
            }
            if (blob.fluidAmount + mbPerItem > capacity) {
                player.sendMessage(Text.msg("Fluid cell is full!", NamedTextColor.RED));
                return true;
            }
            blob.fluidType = depositingFluid;
            blob.fluidAmount += mbPerItem;
            NodeStore.put(block, blob);

            held.setAmount(held.getAmount() - 1);
            ItemStack ret = new ItemStack(returnMat);
            if (held.getAmount() <= 0) {
                player.getInventory().setItemInMainHand(ret);
            } else {
                giveOrDrop(player, ret);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BUCKET_EMPTY, 1f, 1f);
            player.sendActionBar(Component.text("Fluid Deposited: " + depositingFluid + " (" + Items.formatAmount(blob.fluidAmount) + " mB)", NamedTextColor.AQUA));
            return true;
        }

        // 2. Empty bucket -> Extract
        if (mat == Material.BUCKET) {
            if (blob.fluidAmount < 1000 || blob.fluidType == null) {
                return false;
            }
            Material filledBucket = switch (blob.fluidType.toUpperCase(java.util.Locale.ROOT)) {
                case "WATER" -> Material.WATER_BUCKET;
                case "LAVA" -> Material.LAVA_BUCKET;
                case "MILK" -> Material.MILK_BUCKET;
                case "POWDER_SNOW" -> Material.POWDER_SNOW_BUCKET;
                default -> null;
            };
            if (filledBucket == null) {
                return false;
            }
            blob.fluidAmount -= 1000;
            String takenFluid = blob.fluidType;
            if (blob.fluidAmount <= 0) {
                blob.fluidAmount = 0;
                blob.fluidType = null;
            }
            NodeStore.put(block, blob);

            held.setAmount(held.getAmount() - 1);
            ItemStack ret = new ItemStack(filledBucket);
            if (held.getAmount() <= 0) {
                player.getInventory().setItemInMainHand(ret);
            } else {
                giveOrDrop(player, ret);
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BUCKET_FILL, 1f, 1f);
            player.sendActionBar(Component.text("Fluid Extracted: " + takenFluid + " (" + Items.formatAmount(blob.fluidAmount) + " mB remaining)", NamedTextColor.GREEN));
            return true;
        }

        return false;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        var leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
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

    private void installCacheModule(Player player, Block block, NodeBlob blob, DeviceType cacheType, ItemStack held) {
        int tier = cacheType.cacheTier();
        if (blob == null) {
            blob = NodeStore.get(block);
        }
        if (blob == null) return;
        if (blob.virtualCacheTier >= tier) {
            player.sendMessage(Text.msg("This Controller already has " + cacheType.display() + " or higher installed.", NamedTextColor.RED));
            return;
        }
        if (DeviceType.parse(blob.typeName) != DeviceType.MVN_CONTROLLER) {
            return;
        }
        blob.virtualCacheTier = tier;
        NodeStore.put(block, blob);
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            held.subtract(1);
        }
        player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
        player.sendMessage(Text.msg("Installed " + cacheType.display() + "! Virtual Cache capacity: "
                + Items.formatAmount(Settings.virtualCacheCapacity(tier)) + " items.", NamedTextColor.GREEN));
        Network net = manager.networkAt(block);
        if (net != null) {
            net.storage().invalidate();
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

        // 2. Modality restriction: Cross-world boundary
        if (!player.getWorld().equals(bind.getWorld())) {
            player.sendMessage(Text.msg("Wireless terminal out of range: Network is in world '" + bind.getWorld().getName() + "'.", NamedTextColor.RED));
            return;
        }

        // 3. Combat restriction
        long lastDmg = LAST_COMBAT.getOrDefault(player.getUniqueId(), 0L);
        long combatCooldownMs = Settings.wirelessCombatCooldownSeconds() * 1000L;
        if (System.currentTimeMillis() - lastDmg < combatCooldownMs) {
            player.sendMessage(Text.msg("Cannot access wireless terminal while in active combat!", NamedTextColor.RED));
            return;
        }

        // 4. Modality restriction: BentoBox / Skyblock Island ownership check
        if (!canAccessNetwork(player, bind)) {
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

        // 5. Router Antenna check
        boolean hasRouter = net.count(DeviceType.MVN_ROUTER) > 0;
        int maxLocalDist = Settings.wirelessLocalRange();
        double distSq = player.getLocation().distanceSquared(bind);
        if (!hasRouter && distSq > ((double) maxLocalDist * maxLocalDist)) {
            player.sendMessage(Text.msg("Signal lost! Install a Network Router antenna to access globally.", NamedTextColor.RED));
            return;
        }

        event.setCancelled(true);
        new TerminalMenu(plugin, player, net).openMenu();
    }

        private boolean canAccessNetwork(Player player, Location loc) {
        if (player.hasPermission("multiversenets.admin")) {
            return true;
        }
        // Tierra protegida: el mismo criterio que aplica al bucle de red, mas el permiso de
        // bypass. Esta comprobacion es la que evita abrir el GUI de un dispositivo de otra
        // region para reconfigurarlo a mano.
        if (Settings.protectionBlocksPlayerInteraction() && !ProtectionBridge.mayPlayerAccess(player, loc)) {
            return false;
        }
        try {
            if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
                Class<?> cBentoBox = Class.forName("world.bentobox.bentobox.BentoBox");
                Object bbox = cBentoBox.getMethod("getInstance").invoke(null);
                if (bbox != null) {
                    Object islands = bbox.getClass().getMethod("getIslands").invoke(bbox);
                    if (islands != null) {
                        java.util.Optional<?> opt = (java.util.Optional<?>) islands.getClass()
                                .getMethod("getIslandAt", Location.class).invoke(islands, loc);
                        if (opt.isPresent()) {
                            Object island = opt.get();
                            java.util.Set<?> members = (java.util.Set<?>) island.getClass().getMethod("getMemberSet").invoke(island);
                            return members != null && members.contains(player.getUniqueId());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
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
