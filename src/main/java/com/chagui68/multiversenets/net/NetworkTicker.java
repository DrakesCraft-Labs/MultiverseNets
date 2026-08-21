package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class NetworkTicker {

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    private final MultiverseNets plugin;
    private final NetworkManager manager;
    private BukkitTask task;
    private long tick = 0;

    public NetworkTicker(MultiverseNets plugin, NetworkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::run, 20L, 5L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void run() {
        tick += 5;
        boolean scanDue = due(Settings.scanIntervalTicks());
        for (Network net : manager.all()) {
            if (scanDue) {
                net.scan();
            }
            doTransfers(net);
            doVacuum(net);
            doCrafting(net);
        }
    }

    private boolean due(int interval) {
        return interval > 0 && tick % interval == 0;
    }

    private void doTransfers(Network net) {
        if (!due(Settings.transferIntervalTicks())) {
            return;
        }
        int base = Settings.itemsPerOp();
        int ht = base * Settings.htMultiplier();
        net.forEach(DeviceType.GRABBER, (pos, type) -> grabOnce(net, pos, base));
        net.forEach(DeviceType.GRABBER_HT, (pos, type) -> grabOnce(net, pos, ht));
        net.forEach(DeviceType.PUSHER, (pos, type) -> pushOnce(net, pos, base));
        net.forEach(DeviceType.PUSHER_HT, (pos, type) -> pushOnce(net, pos, ht));
        net.forEach(DeviceType.GREEDY_CELL, (pos, type) -> greedyTick(net, pos));
        net.forEach(DeviceType.PURGER, (pos, type) -> purgeOnce(net, pos, base));
    }

    private NodeBlob blobOf(Network net, long pos) {
        return NodeStore.get(net.block(pos));
    }

    private void grabOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) {
            return;
        }
        var pred = NetworkManager.filterPredicate(blob);
        for (BlockFace face : FACES) {
            Block target = net.block(pos).getRelative(face);

            if (target.getState() instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                ItemStack extracted = NetworkManager.extractFirst(inv, pred, rate);
                if (extracted == null) {
                    continue;
                }
                int leftover = net.storage().deposit(extracted);
                if (leftover > 0) {
                    extracted.setAmount(leftover);
                    NetworkManager.insertInto(inv, extracted);
                }
                return;
            }

            // Una maquina de Slimefun no es InventoryHolder: su inventario vive en un BlockMenu
            // aparte. Sin esta rama, la red ve una fundidora electrica como un bloque cualquiera.
            ItemStack sacado = SlimefunBridge.extraer(target, pred, rate);
            if (sacado == null) {
                continue;
            }
            int sobra = net.storage().deposit(sacado);
            if (sobra > 0) {
                sacado.setAmount(sobra);
                SlimefunBridge.insertar(target, sacado);
            }
            return;
        }
    }

    private void pushOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) {
            return;
        }
        var pred = NetworkManager.filterPredicate(blob);
        ItemStack stack = net.storage().withdraw(pred, rate);
        if (stack == null) {
            return;
        }
        for (BlockFace face : FACES) {
            Block target = net.block(pos).getRelative(face);

            if (target.getState() instanceof InventoryHolder holder) {
                int leftover = NetworkManager.insertInto(holder.getInventory(), stack);
                if (leftover > 0) {
                    stack.setAmount(leftover);
                    net.storage().deposit(stack);
                }
                return;
            }

            // Slimefun: se le entrega solo a los huecos que la propia maquina declara de entrada,
            // no a cualquiera. Meter carbon en la salida de una fundidora la atasca.
            if (!SlimefunBridge.esMaquina(target)) {
                continue;
            }
            int noCupo = SlimefunBridge.insertar(target, stack);
            if (noCupo >= stack.getAmount()) {
                continue;
            }
            if (noCupo > 0) {
                stack.setAmount(noCupo);
                net.storage().deposit(stack);
            }
            return;
        }
        net.storage().deposit(stack);
    }

    /**
     * Saca de la red lo que case con el filtro del purgador y lo descarta.
     *
     * Sin esto una red se atasca sola: cualquier maquina que genere un residuo acaba llenando las
     * celdas y bloqueando lo que si interesa.
     *
     * Sin filtro configurado NO hace nada, a proposito. Un purgador que por defecto se lo comiera
     * todo seria una trituradora de inventarios esperando a que alguien lo coloque sin mirar.
     */
    private void purgeOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null || blob.filterMaterials.isEmpty()) {
            return;
        }
        var pred = NetworkManager.filterPredicate(blob);
        ItemStack sacado = net.storage().withdraw(pred, rate);
        // withdraw ya lo saco del almacen; no devolverlo es justamente descartarlo.
        if (sacado != null && Settings.debug()) {
            plugin.getLogger().info("[Purger] descartadas " + sacado.getAmount() + " de "
                    + sacado.getType() + " en " + PosUtil.unpackX(pos) + ","
                    + PosUtil.unpackY(pos) + "," + PosUtil.unpackZ(pos));
        }
    }

    private void greedyTick(Network net, long pos) {
        Block block = net.block(pos);
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        long cap = Settings.greedyCapacity();

        if (blob.cellSample == null && !blob.filterMaterials.isEmpty()) {
            for (String matName : blob.filterMaterials) {
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matName);
                if (mat == null) {
                    continue;
                }
                long available = net.storage().count(item -> item.getType() == mat);
                if (available <= 0) {
                    continue;
                }
                int want = (int) Math.min(cap, available);
                ItemStack got = net.storage().withdraw(item -> item.getType() == mat, want);
                if (got != null) {
                    blob.cellAmount += got.getAmount();
                    blob.cellSample = got;
                    blob.cellSample.setAmount(1);
                    break;
                }
            }
        } else if (blob.cellSample != null && blob.cellAmount < cap) {
            int missing = (int) Math.min((long) Integer.MAX_VALUE, cap - blob.cellAmount);
            ItemStack got = net.storage().withdraw(blob.cellSample::isSimilar, missing);
            if (got != null) {
                blob.cellAmount += got.getAmount();
            }
        }

        if (blob.cellAmount > 0) {
            int rate = Settings.itemsPerOp() * 2;
            int take = (int) Math.min(blob.cellAmount, rate);
            for (BlockFace face : FACES) {
                Block target = block.getRelative(face);
                if (!(target.getState() instanceof InventoryHolder holder)) {
                    continue;
                }
                ItemStack out = blob.cellSample.clone();
                out.setAmount(take);
                int leftover = NetworkManager.insertInto(holder.getInventory(), out);
                int moved = take - leftover;
                blob.cellAmount -= moved;
                take = leftover;
                if (take <= 0) {
                    break;
                }
            }
        }

        if (blob.cellAmount <= 0) {
            blob.cellAmount = 0;
            blob.cellSample = null;
        }
        NodeStore.put(block, blob);
    }

    private void doVacuum(Network net) {
        if (!due(Settings.vacuumIntervalTicks())) {
            return;
        }
        double radius = Settings.vacuumRadius();
        net.forEach(DeviceType.VACUUM, (pos, type) -> {
            Location center = net.block(pos).getLocation().add(0.5, 0.5, 0.5);
            for (org.bukkit.entity.Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof Item item)) {
                    continue;
                }
                ItemStack stack = item.getItemStack();
                int leftover = net.storage().deposit(stack);
                if (leftover <= 0) {
                    item.remove();
                } else {
                    stack.setAmount(leftover);
                    item.setItemStack(stack);
                }
            }
        });
    }

    private void doCrafting(Network net) {
        if (!due(Settings.craftIntervalTicks())) {
            return;
        }
        net.forEach(DeviceType.CRAFTER, (pos, type) -> {
            NodeBlob blob = blobOf(net, pos);
            if (blob == null || blob.recipes.isEmpty()) {
                return;
            }
            CraftingSupport.tryCraftAll(net, blob);
        });
    }
}
