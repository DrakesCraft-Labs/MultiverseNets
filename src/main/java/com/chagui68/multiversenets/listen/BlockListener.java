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
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Text;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), Items.create(type));

        if (type.isCell() && blob.cellSample != null && blob.cellAmount > 0) {
            long remaining = blob.cellAmount;
            int stacks = 0;
            while (remaining > 0 && stacks < 8) {
                int amount = (int) Math.min(blob.cellSample.getMaxStackSize(), remaining);
                ItemStack drop = blob.cellSample.clone();
                drop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
                remaining -= amount;
                stacks++;
            }
            if (remaining > 0) {
                plugin.getLogger().warning("Cell broken with " + remaining + " unrecoverable items at "
                        + block.getX() + "," + block.getY() + "," + block.getZ());
                event.getPlayer().sendMessage(Text.msg("Lost "
                        + Items.formatAmount(remaining) + " items from the cell.", NamedTextColor.RED));
            }
        }

        NodeStore.remove(block);
        if (type == DeviceType.CONTROLLER) {
            manager.removeController(block);
        } else {
            manager.invalidateNear(block);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            useWirelessInAir(event);
            return;
        }
        // La sonda se atiende antes de exigir que el bloque sea un nodo: su utilidad esta
        // precisamente en decirte que NO lo es cuando creias que si.
        if (Items.typeOf(event.getItem()) == DeviceType.PROBE) {
            event.setCancelled(true);
            sondear(event.getPlayer(), block);
            return;
        }

        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        DeviceType type = DeviceType.parse(blob.typeName);
        if (type == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = event.getItem();

        event.setCancelled(true);

        if (type == DeviceType.CONTROLLER && held != null && player.isSneaking()
                && Items.typeOf(held) == DeviceType.WIRELESS_TERMINAL) {
            Items.bindWireless(held, block.getLocation());
            player.sendMessage(Text.msg("Wireless terminal bound to this network.", NamedTextColor.GREEN));
            return;
        }

        if (type == DeviceType.TRANSMITTER && held != null && player.isSneaking()
                && Items.typeOf(held) == DeviceType.RECEIVER) {
            Items.linkReceiver(held, block.getLocation());
            player.sendMessage(Text.msg("Receiver linked to this transmitter.", NamedTextColor.GREEN));
            return;
        }

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
            case ENCODER -> new EncoderMenu(plugin, player).openMenu();
            case CRAFTER -> new CrafterMenu(plugin, player, block).openMenu();
            case CRAFTING_GRID -> {
                Network net = manager.networkAt(block);
                if (net == null) {
                    player.sendMessage(Text.msg("This grid is not part of a network.", NamedTextColor.RED));
                    return;
                }
                new CraftingGridMenu(plugin, player, net).openMenu();
            }
            default -> {
                if (type.filterable()) {
                    new FilterMenu(plugin, player, block, type).openMenu();
                }
            }
        }
    }

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
     *
     * /mvnets doctor resume la salud de todas las redes; esto responde la pregunta concreta que
     * uno se hace de pie delante de una maquina parada: "esta esto conectado a algo?". Un nodo
     * que existe pero no pertenece a ninguna red es exactamente el sintoma que en Networks se
     * reportaba como "lo tengo todo conectado y no saca".
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
        if (held == null || Items.typeOf(held) != DeviceType.WIRELESS_TERMINAL) {
            return;
        }
        Location bind = Items.readWirelessBind(held);
        if (bind == null) {
            event.getPlayer().sendMessage(Text.msg("Unbound: shift+click a controller.", NamedTextColor.YELLOW));
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

    private void openTerminal(Player player, Block controllerBlock) {
        Network net = manager.networkAt(controllerBlock);
        if (net == null) {
            player.sendMessage(Text.msg("No network found for this controller.", NamedTextColor.RED));
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
