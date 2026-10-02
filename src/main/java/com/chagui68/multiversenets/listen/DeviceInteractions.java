package com.chagui68.multiversenets.listen;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.ProtectionBridge;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.gui.BarrelMenu;
import com.chagui68.multiversenets.gui.CellMenu;
import com.chagui68.multiversenets.gui.ControllerMenu;
import com.chagui68.multiversenets.gui.CrafterMenu;
import com.chagui68.multiversenets.gui.CraftingGridMenu;
import com.chagui68.multiversenets.gui.EncoderMenu;
import com.chagui68.multiversenets.gui.FilterMenu;
import com.chagui68.multiversenets.gui.FluidCellMenu;
import com.chagui68.multiversenets.gui.GreedyMenu;
import com.chagui68.multiversenets.gui.LiquidPumpMenu;
import com.chagui68.multiversenets.gui.MonitorMenu;
import com.chagui68.multiversenets.gui.QuantumWorkbenchMenu;
import com.chagui68.multiversenets.gui.QuotaLimiterMenu;
import com.chagui68.multiversenets.gui.RequestTerminalMenu;
import com.chagui68.multiversenets.gui.SfEncoderMenu;
import com.chagui68.multiversenets.gui.TerminalMenu;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.net.Network;
import com.chagui68.multiversenets.net.NetworkManager;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * [EN] What happens when a player uses a device: which menu opens, and whether they are allowed to
 * open it at all.
 * <p>
 * This used to be the middle third of {@code BlockListener}, a class that also owned block
 * placement, breaking, hopper carts, pistons and explosions. Splitting the <em>event wiring</em> from
 * the <em>device behaviour</em> keeps each side readable: this class knows the mapping from a device
 * type to its GUI, and the listener knows which event carried the player here.
 * <p>
 * No behaviour moved with the code — the menu dispatch, the protection gate and the fluid-cell
 * quick-interact are byte-for-byte the same decisions, just in a place where they can be found.
 *
 * [ES] Qué pasa cuando un jugador usa un dispositivo: qué menú se abre y si se le permite abrirlo.
 * <p>
 * Esto era el tercio central de {@code BlockListener}, una clase que además llevaba colocación,
 * rotura, vagonetas con tolva, pistones y explosiones. Separar el <em>cableado de eventos</em> del
 * <em>comportamiento de dispositivos</em> deja cada lado legible: esta clase conoce el mapeo de tipo
 * de dispositivo a su GUI, y el listener sabe qué evento trajo al jugador hasta aquí.
 * <p>
 * No se movió ningún comportamiento: el despacho de menús, la puerta de protección y la interacción
 * rápida con la celda de fluidos son las mismas decisiones, solo en un sitio donde se encuentran.
 */
public class DeviceInteractions {

    private final MultiverseNets plugin;
    private final NetworkManager manager;

    public DeviceInteractions(MultiverseNets plugin, NetworkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
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

    /**
     * EN: A controller placed before networks had an owner has none, and a network without an owner
     * is a stranger inside every claim — its own included. The first player who is allowed to open
     * the controller (which means a protection provider already vouched for them) becomes its owner,
     * once, so worlds built before this field existed heal on first use instead of needing every
     * controller broken and replaced.
     *
     * ES: Un controlador colocado antes de que las redes tuvieran dueño no tiene ninguno, y una red
     * sin dueño es un extraño en cada reclamo, incluido el suyo. El primer jugador al que se le
     * permite abrir el controlador (lo que significa que un plugin de protección ya lo avaló) pasa a
     * ser su dueño, una sola vez, así que los mundos construidos antes de que existiera este campo se
     * curan al primer uso en vez de exigir romper y recolocar cada controlador.
     */
    private void adoptControllerOwner(Player player, Block block, NodeBlob blob, Network net) {
        if (blob == null || (blob.ownerUuid != null && !blob.ownerUuid.isBlank())) {
            return;
        }
        blob.ownerUuid = player.getUniqueId().toString();
        NodeStore.put(block, blob);
        net.markDirty();
        plugin.getLogger().info("Network at " + block.getX() + "," + block.getY() + ","
                + block.getZ() + " adopted " + player.getName() + " as its owner.");
    }

    public boolean openDeviceMenu(Player player, Block block, NodeBlob blob, DeviceType type) {
        switch (type) {
            case MVN_CONTROLLER -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This controller is not active.", NamedTextColor.RED));
                    return true;
                }
                adoptControllerOwner(player, block, blob, net);
                new ControllerMenu(plugin, player, net, block).openMenu();
                return true;
            }
            case MVN_TERMINAL -> {
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
            case MVN_TRANSMITTER, MVN_RECEIVER -> {
                new FilterMenu(plugin, player, block, type).openMenu();
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

    public boolean handleFluidCellQuickInteract(Player player, Block block, NodeBlob blob, ItemStack held) {
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
            player.sendActionBar(net.kyori.adventure.text.Component.text("Fluid Deposited: " + depositingFluid + " (" + Items.formatAmount(blob.fluidAmount) + " mB)", NamedTextColor.AQUA));
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
            player.sendActionBar(net.kyori.adventure.text.Component.text("Fluid Extracted: " + takenFluid + " (" + Items.formatAmount(blob.fluidAmount) + " mB remaining)", NamedTextColor.GREEN));
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

    private void openTerminal(Player player, Block nodeBlock) {
        Network net = manager.networkAt(nodeBlock);
        if (net == null) {
            player.sendMessage(Text.msg("No network found for this node.", NamedTextColor.RED));
            return;
        }
        new TerminalMenu(plugin, player, net).openMenu();
    }

    public void installCacheModule(Player player, Block block, NodeBlob blob, DeviceType cacheType, ItemStack held) {
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

    /**
     * [EN] May this player open a device at all? Admins and the bypass permission pass; everybody
     * else meets the same land-protection rule the network loop uses, with the provider given the
     * chance to certify the player as the land's owner or a member.
     *
     * [ES] ¿Puede este jugador abrir un dispositivo? Administradores y el permiso de bypass pasan; el
     * resto se topa con la misma regla de protección de tierras que usa el bucle de red, con el
     * provider pudiendo certificar al jugador como dueño o miembro de la tierra.
     */
    public static boolean canAccessNetwork(Player player, Location loc) {
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
}
