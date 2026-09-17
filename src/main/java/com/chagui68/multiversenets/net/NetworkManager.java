package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class NetworkManager {

    private final MultiverseNets plugin;
    private final Map<UUID, Map<Long, Network>> networksByWorld = new HashMap<>();

    public NetworkManager(MultiverseNets plugin) {
        this.plugin = plugin;
    }

    public void load() {
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (long[] ctrl : NodeStore.controllers(world.getUID())) {
                long pos = PosUtil.pack((int) ctrl[0], (int) ctrl[1], (int) ctrl[2]);
                networkFor(world, pos);
            }
        }
    }

    public void saveAll() {
        NodeStore.save();
    }

    public Network networkFor(org.bukkit.World world, long controllerPos) {
        return networksByWorld
                .computeIfAbsent(world.getUID(), k -> new HashMap<>())
                .computeIfAbsent(controllerPos, p -> {
                    Network net = new Network(plugin, world, p);
                    net.scan();
                    return net;
                });
    }

    public void registerController(Block block) {
        networkFor(block.getWorld(), PosUtil.pack(block.getX(), block.getY(), block.getZ()));
        NodeStore.addController(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public void removeController(Block block) {
        UUID worldId = block.getWorld().getUID();
        Map<Long, Network> nets = networksByWorld.get(worldId);
        if (nets != null) {
            nets.remove(PosUtil.pack(block.getX(), block.getY(), block.getZ()));
        }
        NodeStore.removeController(worldId, block.getX(), block.getY(), block.getZ());
    }

    public Network networkAt(Block block) {
        UUID worldId = block.getWorld().getUID();
        Map<Long, Network> nets = networksByWorld.get(worldId);
        if (nets == null) {
            return null;
        }
        long pos = PosUtil.pack(block.getX(), block.getY(), block.getZ());
        for (Network net : nets.values()) {
            if (net.contains(pos)) {
                return net;
            }
        }
        return null;
    }

    public Network networkByController(Location loc) {
        UUID worldId = loc.getWorld().getUID();
        Map<Long, Network> nets = networksByWorld.get(worldId);
        if (nets == null) {
            return null;
        }
        return nets.get(PosUtil.pack(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    /**
     * Rescanea la red que toca al bloque o a CUALQUIERA de sus vecinos.
     *
     * El matiz importa: un nodo recien colocado o recien roto todavia no es miembro de ninguna
     * red, asi que buscar la red en su propia posicion no encuentra nada y la topologia se
     * quedaba obsoleta hasta el scan periodico. Mirando los adyacentes, el cambio se ve al
     * instante (antes: "abro la terminal que acabo de poner y dice que no tengo red").
     */
    public void invalidateNear(Block block) {
        java.util.Set<Network> tocadas = new java.util.HashSet<>();
        Network propia = networkAt(block);
        if (propia != null) {
            tocadas.add(propia);
        }
        for (org.bukkit.block.BlockFace face : new org.bukkit.block.BlockFace[]{
                org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST,
                org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN}) {
            Network vecina = networkAt(block.getRelative(face));
            if (vecina != null) {
                tocadas.add(vecina);
            }
        }
        for (Network net : tocadas) {
            net.scan();
        }
    }

    public List<Network> all() {
        List<Network> all = new ArrayList<>();
        for (Map<Long, Network> nets : networksByWorld.values()) {
            all.addAll(nets.values());
        }
        return all;
    }

    public void rescanAll() {
        for (Network net : all()) {
            net.scan();
        }
    }

    public int totalNodes() {
        int total = 0;
        for (Network net : all()) {
            total += net.size();
        }
        return total;
    }

    /**
     * Filtro de un nodo. Sin materiales configuradas pasa todo; con materiales manda el modo:
     * whitelist (defecto) deja pasar solo lo listado, blacklist lo excluye. Es la version barata
     * (por material) del par filtro+modo de los dispositivos avanzados de NetworksV6.
     */
    public static Predicate<ItemStack> filterPredicate(NodeBlob blob) {
        if (blob.filterMaterials.isEmpty()) {
            return item -> true;
        }
        Set<String> mats = new HashSet<>();
        for (String m : blob.filterMaterials) {
            mats.add(m.toUpperCase(Locale.ROOT));
        }
        return item -> blob.filterBlacklist != mats.contains(item.getType().name());
    }

    public static ItemStack extractFirst(Inventory inv, Predicate<ItemStack> pred, int max) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || !pred.test(it)) {
                continue;
            }
            int take = Math.min(it.getAmount(), max);
            ItemStack out = it.clone();
            out.setAmount(take);
            if (take >= it.getAmount()) {
                inv.setItem(i, null);
            } else {
                it.setAmount(it.getAmount() - take);
            }
            return out;
        }
        return null;
    }

    public static int insertInto(Inventory inv, ItemStack stack) {
        Map<Integer, ItemStack> overflow = inv.addItem(stack);
        int left = 0;
        for (ItemStack over : overflow.values()) {
            left += over.getAmount();
        }
        return left;
    }
}
