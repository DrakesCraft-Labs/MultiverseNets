package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.SlimefunBridge;
import com.chagui68.multiversenets.craft.Blueprints;
import com.chagui68.multiversenets.craft.CraftingSupport;
import com.chagui68.multiversenets.craft.RecipeData;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import com.chagui68.multiversenets.util.Settings;
import com.chagui68.multiversenets.util.StackUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * [EN] Network Ticking & Processing Loop
 * Central heartbeat running every 5 ticks. Dispatches scheduled tasks based on independent config timers:
 * transfer operations (grabbers/pushers), vacuum pickups, auto-crafting, and topology rescans.
 * - Anti-loss guarantee: Items that cannot be deposited return to source, or drop naturally if full.
 * - Safe purger: Purger nodes without configured filters never discard items.
 * - Chunk safety: Nodes in unloaded chunks are never touched or synchronously loaded.
 *
 * [ES] Bucle de Procesamiento y Ticking de Red
 * Corazón central de todas las redes ejecutado cada 5 ticks. Distribuye el trabajo por temporizadores configurables:
 * transferencias (grabbers/pushers), recolección por vacuum, autocrafteo y reescaneo de topología.
 */
public class NetworkTicker {

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    private final MultiverseNets plugin;
    private final NetworkManager manager;
    private BukkitTask task;

    // Cada familia cuenta sus ticks restantes; al llegar a 0 se ejecuta y se rearma.
    private int scanIn;
    private int transferIn;
    private int vacuumIn;
    private int craftIn;

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
        scanIn -= 5;
        transferIn -= 5;
        vacuumIn -= 5;
        craftIn -= 5;
        for (Network net : manager.all()) {
            if (net.isDirty() || scanIn <= 0) {
                net.scan();
            }
            if (transferIn <= 0) {
                doTransfers(net);
            }
            if (vacuumIn <= 0) {
                doVacuum(net);
            }
            if (craftIn <= 0) {
                doCrafting(net);
            }
        }
        if (scanIn <= 0) {
            scanIn = Settings.scanIntervalTicks();
        }
        if (transferIn <= 0) {
            transferIn = Settings.transferIntervalTicks();
        }
        if (vacuumIn <= 0) {
            vacuumIn = Settings.vacuumIntervalTicks();
        }
        if (craftIn <= 0) {
            craftIn = Settings.craftIntervalTicks();
        }
    }

    private void doTransfers(Network net) {
        int base = Settings.itemsPerOp();
        int ht = base * Settings.htMultiplier();
        net.forEach(DeviceType.MVN_GRABBER, (pos, type) -> grabOnce(net, pos, base));
        net.forEach(DeviceType.MVN_GRABBER_HT, (pos, type) -> grabOnce(net, pos, ht));
        net.forEach(DeviceType.MVN_PUSHER, (pos, type) -> pushOnce(net, pos, base));
        net.forEach(DeviceType.MVN_PUSHER_HT, (pos, type) -> pushOnce(net, pos, ht));
        net.forEach(DeviceType.MVN_GREEDY_CELL, (pos, type) -> greedyTick(net, pos));
        net.forEach(DeviceType.MVN_PURGER, (pos, type) -> purgeOnce(net, pos, base));
        net.forEach(DeviceType.MVN_RECEIVER, (pos, type) -> bridgeOnce(net, pos, base));
    }

    private NodeBlob blobOf(Network net, long pos) {
        return NodeStore.get(net.block(pos));
    }

    private void spark(Network net, long pos) {
        if (!net.crayon()) {
            return;
        }
        Location at = net.block(pos).getLocation().add(0.5, 0.6, 0.5);
        at.getWorld().spawnParticle(Particle.DUST, at, 4, 0.25, 0.25, 0.25, 0,
                new Particle.DustOptions(Color.AQUA, 0.8f));
    }

    private BlockFace[] facesFor(NodeBlob blob) {
        if (blob != null && blob.targetFace != null && !blob.targetFace.equalsIgnoreCase("ALL")) {
            try {
                BlockFace single = BlockFace.valueOf(blob.targetFace.toUpperCase(java.util.Locale.ROOT));
                return new BlockFace[]{single};
            } catch (IllegalArgumentException ignored) {
            }
        }
        return FACES;
    }

    private static boolean isPotentialContainer(Material mat) {
        if (mat == null || mat.isAir()) {
            return false;
        }
        return switch (mat) {
            case CHEST, TRAPPED_CHEST, BARREL, HOPPER, DISPENSER, DROPPER,
                 FURNACE, BLAST_FURNACE, SMOKER, BREWING_STAND, CHISELED_BOOKSHELF,
                 SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX,
                 LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX,
                 PINK_SHULKER_BOX, GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX,
                 CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX, BLUE_SHULKER_BOX,
                 BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX,
                 BLACK_SHULKER_BOX -> true;
            default -> false;
        };
    }

    /**
     * Saca hasta {@code rate} unidades del contenedor adyacente y las mete en la red. Si la red
     * no las admite todas, el sobrante vuelve al origen; si el origen tampoco lo admite (alguien
     * lo lleno en medio), se suelta en el mundo. Al aire no se va nada.
     */
    private void grabOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) {
            return;
        }
        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        Block self = net.block(pos);
        for (BlockFace face : facesFor(blob)) {
            Block target = self.getRelative(face);
            Material mat = target.getType();

            if (isPotentialContainer(mat) && target.getState() instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                ItemStack extracted = NetworkManager.extractFirst(inv, pred, rate);
                if (extracted == null) {
                    // Inventario vacio para este filtro: se mira la siguiente cara.
                    continue;
                }
                int leftover = net.storage().deposit(extracted);
                if (leftover > 0) {
                    extracted.setAmount(leftover);
                    int sinCasa = NetworkManager.insertInto(inv, extracted);
                    if (sinCasa > 0) {
                        extracted.setAmount(sinCasa);
                        dropAt(target, extracted);
                    }
                }
                spark(net, pos);
                return;
            }

            // Slimefun machine compatibility branch
            if (Settings.compatSlimefun() && SlimefunBridge.isAvailable()) {
                ItemStack extracted = SlimefunBridge.extract(target, pred, rate);
                if (extracted == null) {
                    continue;
                }
                int leftover = net.storage().deposit(extracted);
                if (leftover > 0) {
                    extracted.setAmount(leftover);
                    int unhoused = SlimefunBridge.insert(target, extracted);
                    if (unhoused > 0) {
                        extracted.setAmount(unhoused);
                        dropAt(target, extracted);
                    }
                }
                spark(net, pos);
                return;
            }
        }
    }

    /**
     * EN: Exports items from the network into adjacent inventories.
     *
     * ES: Exporta ítems desde la red hacia los contenedores adyacentes.
     */
    private void pushOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) {
            return;
        }
        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        ItemStack stack = net.storage().withdraw(pred, rate);
        if (stack == null) {
            return;
        }
        Block self = net.block(pos);
        for (BlockFace face : facesFor(blob)) {
            Block target = self.getRelative(face);
            Material mat = target.getType();

            if (isPotentialContainer(mat) && target.getState() instanceof InventoryHolder holder) {
                int leftover = NetworkManager.insertInto(holder.getInventory(), stack);
                if (leftover < stack.getAmount()) {
                    spark(net, pos);
                }
                stack.setAmount(leftover);
                if (leftover <= 0) {
                    break;
                }
                continue;
            }

            if (Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.isMachine(target)) {
                int before = stack.getAmount();
                int unhoused = SlimefunBridge.insert(target, stack);
                stack.setAmount(Math.max(0, Math.min(unhoused, before)));
                if (stack.getAmount() < before) {
                    spark(net, pos);
                }
                if (stack.getAmount() <= 0) {
                    break;
                }
            }
        }
        if (stack.getAmount() > 0) {
            net.storage().deposit(stack);
        }
    }

    /**
     * EN: Discards matching items from the network storage. Does nothing if no filters are configured.
 *
     * ES: Descarta ítems coincidentes del almacenamiento de la red. No hace nada si no hay filtros configurados.
     */
    private void purgeOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) {
            return;
        }
        boolean hasItems = blob.filterItems != null && !blob.filterItems.isEmpty();
        boolean hasMats = blob.filterMaterials != null && !blob.filterMaterials.isEmpty();
        if (!hasItems && !hasMats) {
            return;
        }
        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        ItemStack purged = net.storage().withdraw(pred, rate);
        if (purged == null) {
            return;
        }
        spark(net, pos);
        if (Settings.debug()) {
            plugin.getLogger().info("[Purger] Discarded " + purged.getAmount() + "x "
                    + purged.getType() + " at " + PosUtil.unpackX(pos) + ","
                    + PosUtil.unpackY(pos) + "," + PosUtil.unpackZ(pos));
        }
    }

    /**
     * EN: Ticks a Greedy Cell: claims its configured item from the network and distributes it to adjacent containers.
 *
     * ES: Procesa una Greedy Cell: solicita su ítem a la red hasta llenarse y lo sirve a contenedores vecinos.
     */
    private void greedyTick(Network net, long pos) {
        Block block = net.block(pos);
        NodeBlob blob = NodeStore.get(block);
        if (blob == null) {
            return;
        }
        long cap = Settings.greedyCapacity();
        long currentTotal = blob.totalGreedyAmount();

        // 1. Suction: pull matching items from network into greedy storage up to shared cap
        if (currentTotal < cap) {
            boolean hasFilter = (blob.filterMaterials != null && !blob.filterMaterials.isEmpty())
                    || (blob.filterItems != null && !blob.filterItems.isEmpty());
            if (hasFilter) {
                Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
                long space = cap - currentTotal;
                int want = (int) Math.min(space, (long) Settings.itemsPerOp() * 4);
                ItemStack got = net.storage().withdraw(pred, want, pos);
                if (got != null && got.getAmount() > 0) {
                    blob.addGreedyItem(got, got.getAmount());
                    spark(net, pos);
                }
            }
        }

        // 2. Distribution: push stored items into adjacent containers
        if (blob.totalGreedyAmount() > 0 && blob.greedySamples != null && !blob.greedySamples.isEmpty()) {
            int maxTake = (int) Math.min(blob.totalGreedyAmount(), Settings.itemsPerOp() * 2L);
            int movedTotal = 0;
            for (int i = 0; i < blob.greedySamples.size() && movedTotal < maxTake; i++) {
                ItemStack sample = blob.greedySamples.get(i);
                long amount = blob.greedyAmounts.get(i);
                if (sample == null || amount <= 0) {
                    continue;
                }
                int want = (int) Math.min(amount, (long) (maxTake - movedTotal));
                int roundMoved = 0;
                for (BlockFace face : facesFor(blob)) {
                    Block target = block.getRelative(face);
                    Material targetMat = target.getType();
                    if (isPotentialContainer(targetMat) && target.getState() instanceof InventoryHolder holder) {
                        ItemStack out = sample.clone();
                        out.setAmount(want);
                        int leftover = NetworkManager.insertInto(holder.getInventory(), out);
                        int moved = want - leftover;
                        if (moved > 0) {
                            roundMoved += moved;
                            want = leftover;
                        }
                    } else if (Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.isMachine(target)) {
                        ItemStack out = sample.clone();
                        out.setAmount(want);
                        int unhoused = SlimefunBridge.insert(target, out);
                        int moved = want - unhoused;
                        if (moved > 0) {
                            roundMoved += moved;
                            want = unhoused;
                        }
                    }
                    if (want <= 0) {
                        break;
                    }
                }
                if (roundMoved > 0) {
                    blob.removeGreedyItem(i, roundMoved);
                    movedTotal += roundMoved;
                    spark(net, pos);
                    if (roundMoved >= amount) {
                        i--;
                    }
                }
            }
        }

        NodeStore.put(block, blob);
    }

    /**
     * El puente inalambrico: el RECEPTOR tira de la red del TRANSMISOR enlazado hacia la suya.
     *
     * Espejo del transmisor/receptor de NetworksV6, con el enlace guardado en el receptor (aqui
     * el enlace se fija haciendo shift+click con el receptor sobre el transmisor). Solo cruzan
     * items que pasen el filtro DEL RECEPTOR, y con filtro vacio no cruza nada: abrir un puente
     * sin decidir que pasa mezclaria dos redes enteras sin querer.
     */
    /**
     * EN: Wireless Bridge: pulls matching filtered items from the linked Transmitter network into this Receiver's network.
 *
     * ES: Puente inalámbrico: el Receptor extrae ítems filtrados de la red del Transmisor vinculado hacia la suya.
     */
    private void bridgeOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null || blob.txWorld == null || blob.filterMaterials.isEmpty()) {
            return;
        }
        UUID worldId;
        try {
            worldId = UUID.fromString(blob.txWorld);
        } catch (IllegalArgumentException e) {
            return;
        }
        World world = plugin.getServer().getWorld(worldId);
        if (world == null || !world.isChunkLoaded(blob.txX >> 4, blob.txZ >> 4)) {
            return;
        }
        Block txBlock = world.getBlockAt(blob.txX, blob.txY, blob.txZ);
        NodeBlob txBlob = NodeStore.get(txBlock);
        if (txBlob == null || DeviceType.parse(txBlob.typeName) != DeviceType.MVN_TRANSMITTER) {
            return;
        }
        Network remote = manager.networkAt(txBlock);
        if (remote == null || remote == net) {
            return;
        }
        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        ItemStack stack = remote.storage().withdraw(pred, rate);
        if (stack == null) {
            return;
        }
        int leftover = net.storage().deposit(stack);
        if (leftover > 0) {
            stack.setAmount(leftover);
            remote.storage().deposit(stack);
        } else {
            spark(net, pos);
        }
    }

    /**
     * EN: Collects matching dropped items from the ground within the vacuum radius.
 *
     * ES: Recoge ítems del suelo que cumplan el filtro dentro del radio del vacuum.
     */
    private void doVacuum(Network net) {
        double radius = Settings.vacuumRadius();
        net.forEach(DeviceType.MVN_VACUUM, (pos, type) -> {
            NodeBlob blob = blobOf(net, pos);
            if (blob == null) {
                return;
            }
            Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
            Location center = net.block(pos).getLocation().add(0.5, 0.5, 0.5);
            for (org.bukkit.entity.Entity entity : center.getWorld()
                    .getNearbyEntities(center, radius, radius, radius)) {
                if (!(entity instanceof Item item)) {
                    continue;
                }
                if (item.getPickupDelay() > 0) {
                    continue;
                }
                ItemStack stack = item.getItemStack();
                if (!pred.test(stack)) {
                    continue;
                }
                int leftover = net.storage().deposit(stack);
                if (leftover <= 0) {
                    item.remove();
                    spark(net, pos);
                } else if (leftover < stack.getAmount()) {
                    stack.setAmount(leftover);
                    item.setItemStack(stack);
                    spark(net, pos);
                }
            }
        });
    }

    /**
     * EN: Executes auto-crafting attempts for installed blueprints and recipes.
 *
     * ES: Ejecuta intentos de autocrafteo para los blueprints y recetas instaladas.
     */
    private void doCrafting(Network net) {
        net.forEach(DeviceType.MVN_CRAFTER, (pos, type) -> {
            NodeBlob blob = blobOf(net, pos);
            if (blob == null) {
                return;
            }
            boolean worked = false;
            for (String b64 : new ArrayList<>(blob.blueprintData)) {
                RecipeData data = Blueprints.decode(b64);
                if (data == null) {
                    continue;
                }
                if (CraftingSupport.tryCraftBlueprint(net, data)) {
                    worked = true;
                }
            }
            if (!blob.recipes.isEmpty()) {
                worked |= CraftingSupport.tryCraftAll(net, blob);
            }
            if (worked) {
                spark(net, pos);
            }
        });
    }

    /**
     * EN: Safety fallback: drops item safely at the block location if no container can accept it.
 *
     * ES: Red de seguridad: suelta el ítem en el bloque si ningún contenedor puede aceptarlo.
     */
    private static void dropAt(Block block, ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) {
            return;
        }
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), stack);
    }
}
