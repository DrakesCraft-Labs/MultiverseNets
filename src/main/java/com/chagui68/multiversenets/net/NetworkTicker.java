package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.compat.ProtectionBridge;
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
import java.util.List;
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

    // Adaptive backoff: counts consecutive empty/idle cycles per node to skip redundant container lookups
    private final java.util.Map<Long, Integer> backoffCycles = new java.util.concurrent.ConcurrentHashMap<>();

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

    public void tick() {
        run();
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
            NetworkHologramManager.updateHologram(net);
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
        net.forEach(DeviceType.MVN_LIQUID_PUMP, (pos, type) -> pumpTick(net, pos));
    }

    private NodeBlob blobOf(Network net, long pos) {
        return NodeStore.get(net.block(pos));
    }

    private BlockFace[] facesFor(NodeBlob blob) {
        if (blob != null && blob.targetFace != null) {
            if ("NONE".equalsIgnoreCase(blob.targetFace)) {
                return new BlockFace[0];
            }
            if (!blob.targetFace.equalsIgnoreCase("ALL")) {
                try {
                    BlockFace single = BlockFace.valueOf(blob.targetFace.toUpperCase(java.util.Locale.ROOT));
                    return new BlockFace[]{single};
                } catch (IllegalArgumentException ignored) {
                }
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
    private ItemStack streamToPushers(Network net, ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) return null;
        for (DeviceType pusherType : List.of(DeviceType.MVN_PUSHER_HT, DeviceType.MVN_PUSHER)) {
            final ItemStack currentStack = stack;
            net.forEach(pusherType, (pos, type) -> {
                if (currentStack.getAmount() <= 0) return;
                NodeBlob pBlob = blobOf(net, pos);
                if (pBlob == null) return;
                Predicate<ItemStack> pPred = NetworkManager.filterPredicate(pBlob);
                if (!pPred.test(currentStack)) return;
                Block pBlock = net.block(pos);
                for (BlockFace face : facesFor(pBlob)) {
                    Block target = pBlock.getRelative(face);
                    if (Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.isMachine(target)) {
                        int unhoused = SlimefunBridge.insert(target, currentStack);
                        currentStack.setAmount(unhoused);
                        if (unhoused <= 0) return;
                    } else if (isPotentialContainer(target.getType()) && target.getState() instanceof InventoryHolder holder) {
                        int unhoused = NetworkManager.insertInto(holder.getInventory(), currentStack);
                        currentStack.setAmount(unhoused);
                        if (unhoused <= 0) return;
                    }
                }
            });
            if (stack.getAmount() <= 0) return null;
        }
        return stack.getAmount() > 0 ? stack : null;
    }

    /**
     * [EN] Land protection gate for every block the network loop is about to touch.
     * <p>
     * A network is an anonymous actor: it carries no player identity, so it cannot be judged
     * "trusted" the way a ProtectionStones member or a GriefPrevention trusted player would be.
     * Any claimed land is therefore off limits in both directions, which is what stops two
     * players from robbing each other through their own devices. Deliberately silent: this runs
     * thousands of times per second, and a denied transfer is the expected outcome, not an event.
     *
     * [ES] Puerta de protección de tierras para cada bloque que va a tocar el bucle de red.
     * <p>
     * Una red es un actor anónimo: no lleva identidad de jugador, así que no se puede juzgar
     * "de confianza" como a un miembro de ProtectionStones o a un jugador de confianza de
     * GriefPrevention. Por eso cualquier tierra reclamada queda intocable en ambos sentidos, que
     * es lo que evita que dos jugadores se roben entre sí a través de sus dispositivos.
     */
    private static boolean denied(Block block) {
        return block != null && ProtectionBridge.isProtected(block);
    }

    private static boolean denied(org.bukkit.entity.Entity entity) {
        return entity != null && ProtectionBridge.isProtected(entity.getLocation());
    }

    private void grabOnce(Network net, long pos, int rate) {
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) {
            return;
        }
        // Zero-drop: process any transit buffer leftovers first
        if (blob.transitBuffer != null && blob.transitBuffer.getAmount() > 0) {
            int leftover = net.storage().deposit(blob.transitBuffer);
            if (leftover <= 0) {
                blob.transitBuffer = null;
                NodeStore.put(net.block(pos), blob);
            } else {
                blob.transitBuffer.setAmount(leftover);
                ItemStack unrouted = streamToPushers(net, blob.transitBuffer);
                blob.transitBuffer = unrouted;
                NodeStore.put(net.block(pos), blob);
                return; // Wait until buffer clears before grabbing more
            }
        }
        int backoff = backoffCycles.getOrDefault(pos, 0);
        if (backoff > 0 && (backoff % 3 != 0)) {
            backoffCycles.put(pos, backoff + 1);
            return;
        }

        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        Block self = net.block(pos);
        boolean foundAny = false;
        for (BlockFace face : facesFor(blob)) {
            Block target = self.getRelative(face);
            Material mat = target.getType();
            // Tierra ajena=intocable: la red es un actor sin identidad de jugador, asi que si un
            // cofre esta dentro de una region protegida aqui no se toca. Se sigue con la siguiente
            // cara, igual que con un bloque que no sea contenedor.
            if (denied(target)) {
                continue;
            }

            // 1. Slimefun machine compatibility FIRST (machines like Dispensers must not be hijacked by raw container logic)
            if (Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.isMachine(target)) {
                ItemStack extracted = SlimefunBridge.extract(target, pred, rate);
                if (extracted != null) {
                    foundAny = true;
                    backoffCycles.remove(pos);
                    int moved = extracted.getAmount();
                    int leftover = net.storage().deposit(extracted);
                    if (leftover > 0) {
                        extracted.setAmount(leftover);
                        ItemStack unrouted = streamToPushers(net, extracted);
                        if (unrouted != null && unrouted.getAmount() > 0) {
                            int unhoused = SlimefunBridge.insert(target, unrouted);
                            if (unhoused > 0) {
                                unrouted.setAmount(unhoused);
                                blob.transitBuffer = unrouted;
                                NodeStore.put(self, blob);
                                moved -= unhoused;
                            }
                        }
                    }
                    if (moved > 0) {
                        net.throughput().recordFlow(pos, moved);
                    }
                    return;
                }
                continue;
            }

            // 2. Vanilla container fallback
            if (isPotentialContainer(mat) && target.getState() instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                ItemStack extracted = NetworkManager.extractMatching(inv, pred, rate);
                if (extracted == null) {
                    // Inventario vacio para este filtro: se mira la siguiente cara.
                    continue;
                }
                foundAny = true;
                backoffCycles.remove(pos);
                int moved = extracted.getAmount();
                int leftover = net.storage().deposit(extracted);
                if (leftover > 0) {
                    extracted.setAmount(leftover);
                    ItemStack unrouted = streamToPushers(net, extracted);
                    if (unrouted != null && unrouted.getAmount() > 0) {
                        int sinCasa = NetworkManager.insertInto(inv, unrouted);
                        if (sinCasa > 0) {
                            unrouted.setAmount(sinCasa);
                            blob.transitBuffer = unrouted;
                            NodeStore.put(self, blob);
                            moved -= sinCasa;
                        }
                    }
                }
                if (moved > 0) {
                    net.throughput().recordFlow(pos, moved);
                }
                return;
            }
        }
        if (!foundAny) {
            backoffCycles.put(pos, Math.min(30, backoff + 1));
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

        int backoff = backoffCycles.getOrDefault(pos, 0);
        if (backoff > 0 && (backoff % 3 != 0)) {
            backoffCycles.put(pos, backoff + 1);
            return;
        }

        Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
        ItemStack stack = net.storage().withdraw(pred, rate, -1L, false);
        if (stack == null) {
            backoffCycles.put(pos, Math.min(30, backoff + 1));
            return;
        }
        int initialAmount = stack.getAmount();
        Block self = net.block(pos);
        for (BlockFace face : facesFor(blob)) {
            Block target = self.getRelative(face);
            Material mat = target.getType();
            // Tambien a la inversa: meter items dentro de una region ajena es el mismo robo con
            // el signo cambiado, y si el destino es una maquina con salida puede duplicar.
            if (denied(target)) {
                continue;
            }

            // 1. Slimefun machine compatibility FIRST (ensures items go to BlockMenu input slots instead of raw dispenser inventory)
            if (Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.isMachine(target)) {
                int before = stack.getAmount();
                int unhoused = SlimefunBridge.insert(target, stack);
                stack.setAmount(Math.max(0, Math.min(unhoused, before)));
                if (stack.getAmount() <= 0) {
                    break;
                }
                continue;
            }

            // 2. Vanilla container fallback
            if (isPotentialContainer(mat) && target.getState() instanceof InventoryHolder holder) {
                int leftover = NetworkManager.insertInto(holder.getInventory(), stack);
                stack.setAmount(leftover);
                if (leftover <= 0) {
                    break;
                }
                continue;
            }
        }
        int delivered = initialAmount - stack.getAmount();
        if (delivered > 0) {
            backoffCycles.remove(pos);
            net.throughput().recordFlow(pos, delivered);
        } else {
            backoffCycles.put(pos, Math.min(30, backoff + 1));
        }
        if (stack.getAmount() > 0) {
            int leftover = net.storage().deposit(stack);
            if (leftover > 0) {
                stack.setAmount(leftover);
                dropAt(self, stack);
            }
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
        ItemStack purged = net.storage().withdraw(pred, rate, -1L, false);
        if (purged == null) {
            return;
        }
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
        boolean changed = false;

        // 1. Suction: pull matching items from network into greedy storage up to shared cap
        if (currentTotal < cap) {
            boolean hasFilter = (blob.filterMaterials != null && !blob.filterMaterials.isEmpty())
                    || (blob.filterItems != null && !blob.filterItems.isEmpty());
            if (hasFilter) {
                Predicate<ItemStack> pred = NetworkManager.filterPredicate(blob);
                long space = cap - currentTotal;
                int want = (int) Math.min(space, (long) Settings.itemsPerOp() * 4);
                ItemStack got = net.storage().withdraw(pred, want, pos, false);
                if (got != null && got.getAmount() > 0) {
                    blob.addGreedyItem(got, got.getAmount());
                    changed = true;
                }
            }
        }

        // 2. Distribution: push stored items into adjacent containers (NOT other network nodes!)
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
                    // Critical: Do NOT dump into other network nodes (e.g. Infinity Barrel, Crafter, etc.)
                    if (NodeStore.hasNode(target)) {
                        continue;
                    }
                    if (denied(target)) {
                        continue;
                    }
                    Material targetMat = target.getType();
                    if (Settings.compatSlimefun() && SlimefunBridge.isAvailable() && SlimefunBridge.isMachine(target)) {
                        ItemStack out = sample.clone();
                        out.setAmount(want);
                        int unhoused = SlimefunBridge.insert(target, out);
                        int moved = want - unhoused;
                        if (moved > 0) {
                            roundMoved += moved;
                            want = unhoused;
                        }
                    } else if (isPotentialContainer(targetMat) && target.getState() instanceof InventoryHolder holder) {
                        ItemStack out = sample.clone();
                        out.setAmount(want);
                        int leftover = NetworkManager.insertInto(holder.getInventory(), out);
                        int moved = want - leftover;
                        if (moved > 0) {
                            roundMoved += moved;
                            want = leftover;
                            if (target.getState() instanceof org.bukkit.block.TileState ts) {
                                ts.update();
                            }
                        }
                    }
                    if (want <= 0) {
                        break;
                    }
                }
                if (roundMoved > 0) {
                    blob.removeGreedyItem(i, roundMoved);
                    movedTotal += roundMoved;
                    changed = true;
                    if (roundMoved >= amount) {
                        i--;
                    }
                }
            }
        }

        if (changed) {
            NodeStore.put(block, blob);
        }
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
        // El enlace es el unico punto donde dos redes se tocan aunque no sean la misma, asi que
        // se comprueban los dos extremos.
        if (denied(txBlock) || denied(net.block(pos))) {
            return;
        }
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
                // Suctionar el suelo de una region ajena tambien es robar: los drops de un
                // segundo no salen de su base.
                if (denied(item)) {
                    continue;
                }
                int leftover = net.storage().deposit(stack);
                if (leftover <= 0) {
                    item.remove();
                } else if (leftover < stack.getAmount()) {
                    stack.setAmount(leftover);
                    item.setItemStack(stack);
                }
            }
        });
    }

    /**
     * EN: Executes auto-crafting attempts for installed blueprints and recipes.
 *
     * ES: Ejecuta intentos de autocrafteo para los blueprints y recetas instaladas.
     */
    public void doCrafting(Network net) {
        java.util.function.Consumer<Long> ticker = pos -> {
            NodeBlob blob = blobOf(net, pos);
            if (blob == null) {
                return;
            }
            for (String b64 : new ArrayList<>(blob.blueprintData)) {
                RecipeData data = Blueprints.decode(b64);
                if (data == null) {
                    continue;
                }
                CraftingSupport.tryCraftBlueprint(net, data);
            }
            if (!blob.recipes.isEmpty()) {
                CraftingSupport.tryCraftAll(net, blob);
            }
        };

        net.forEach(DeviceType.MVN_CRAFTER, (pos, type) -> ticker.accept(pos));
        net.forEach(DeviceType.MVN_SF_CRAFTER, (pos, type) -> ticker.accept(pos));
    }

    /**
     * EN: Executes fluid pumping operations (DRAIN from world/cauldrons or FILL to cauldrons/containers).
     * ES: Ejecuta operaciones de bombeo de fluidos (drenar o llenar).
     */
    /**
     * Ticks a Liquid Pump node.
     * Extracts water or lava source blocks directly from the block underneath the pump.
     */
    private void pumpTick(Network net, long pos) {
        Block pumpBlock = net.block(pos);
        if (pumpBlock == null) return;
        NodeBlob blob = blobOf(net, pos);
        if (blob == null) return;

        Block target = pumpBlock.getRelative(BlockFace.DOWN);
        if (target == null) return;
        // Drenar la lava o el agua de otro es lo mismo que vaciarle la base: ademas deja el
        // bloque en aire, asi que el grief es visible y no recuperable.
        if (denied(target)) return;

        String filter = blob.pumpFluid != null ? blob.pumpFluid.toUpperCase(java.util.Locale.ROOT) : null;

        // 1. Water source
        if (target.getType() == Material.WATER && (filter == null || "ANY".equals(filter) || "WATER".equals(filter))) {
            if (target.getBlockData() instanceof org.bukkit.block.data.Levelled l && l.getLevel() == 0) {
                if (net.fluidStorage().deposit("WATER", 1000) == 0) {
                    target.setType(Material.AIR);
                }
            }
        }
        // 2. Lava source
        else if (target.getType() == Material.LAVA && (filter == null || "ANY".equals(filter) || "LAVA".equals(filter))) {
            if (target.getBlockData() instanceof org.bukkit.block.data.Levelled l && l.getLevel() == 0) {
                if (net.fluidStorage().deposit("LAVA", 1000) == 0) {
                    target.setType(Material.AIR);
                }
            }
        }
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
