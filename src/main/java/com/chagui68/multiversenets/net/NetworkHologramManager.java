package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [EN] Native TextDisplay Floating Hologram Manager for Network Controllers.
 * Lightweight, zero entity accumulation, displays live metrics & throughput without lag.
 *
 * [ES] Gestor de Hologramas Flotantes TextDisplay Nativos para Controladores de Red.
 * Ligero, sin acumulación de entidades, muestra métricas en vivo y flujo en tiempo real sin lag.
 */
public final class NetworkHologramManager {

    private static final Map<String, UUID> HOLOGRAM_ENTITIES = new ConcurrentHashMap<>();
    private static NamespacedKey HOLO_KEY;

    private NetworkHologramManager() {}

    public static void init(MultiverseNets plugin) {
        HOLO_KEY = new NamespacedKey(plugin, "controller_hologram");
    }

    public static void updateHologram(@Nonnull Network net) {
        if (HOLO_KEY == null) return;
        World world = net.world();
        long pos = net.controllerPos();
        int cx = com.chagui68.multiversenets.util.PosUtil.unpackX(pos) >> 4;
        int cz = com.chagui68.multiversenets.util.PosUtil.unpackZ(pos) >> 4;
        if (world == null || !world.isChunkLoaded(cx, cz)) {
            return;
        }

        Location controllerLoc = net.block(pos).getLocation();
        String key = key(world, pos);
        UUID entityId = HOLOGRAM_ENTITIES.get(key);
        TextDisplay textDisplay = null;

        if (entityId != null) {
            Entity entity = world.getEntity(entityId);
            if (entity instanceof TextDisplay td && entity.isValid()) {
                textDisplay = td;
            } else {
                HOLOGRAM_ENTITIES.remove(key);
            }
        }

        if (textDisplay == null) {
            Location spawnLoc = controllerLoc.clone().add(0.5, 1.45, 0.5);
            // Preventive sweep of orphan TextDisplays
            for (Entity nearby : world.getNearbyEntities(spawnLoc, 1.0, 1.0, 1.0)) {
                if (nearby instanceof TextDisplay td && td.getPersistentDataContainer().has(HOLO_KEY, PersistentDataType.BYTE)) {
                    nearby.remove();
                }
            }

            textDisplay = world.spawn(spawnLoc, TextDisplay.class, td -> {
                td.setBillboard(Display.Billboard.CENTER);
                td.setDefaultBackground(true);
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setPersistent(false);
                td.getPersistentDataContainer().set(HOLO_KEY, PersistentDataType.BYTE, (byte) 1);
            });
            HOLOGRAM_ENTITIES.put(key, textDisplay.getUniqueId());
        }

        int nodeCount = net.size();
        int maxNodes = com.chagui68.multiversenets.util.Settings.maxNodes();

        // 1. Header: "MultiverseNets"
        Component header = Component.text("MultiverseNets", NamedTextColor.AQUA, TextDecoration.BOLD);

        // 2. Network Core Status
        Component statusLine;
        if (net.error == null || net.error.isBlank()) {
            statusLine = Component.text("● ", NamedTextColor.GREEN)
                    .append(Component.text("System Online", NamedTextColor.WHITE));
        } else {
            statusLine = Component.text("⚠ ", NamedTextColor.RED)
                    .append(Component.text(net.error, NamedTextColor.RED));
        }

        // 3. Grid Infrastructure Scale
        Component gridLine = Component.text("Grid Scale: ", NamedTextColor.GRAY)
                .append(Component.text(NumberFormat.getInstance(Locale.ROOT).format(nodeCount), NamedTextColor.WHITE))
                .append(Component.text(" / " + NumberFormat.getInstance(Locale.ROOT).format(maxNodes) + " nodes", NamedTextColor.DARK_GRAY));

        // 4. Storage Overview (Items & Fluids)
        List<NetworkStorage.View> view = net.storage().view();
        long totalItems = 0;
        for (NetworkStorage.View v : view) {
            totalItems += v.amount();
        }
        long fluidMb = net.fluidStorage().totalStored();

        Component storageLine;
        if (totalItems > 0 && fluidMb > 0) {
            storageLine = Component.text("Storage: ", NamedTextColor.GRAY)
                    .append(Component.text(com.chagui68.multiversenets.item.Items.formatAmount(totalItems) + " items", NamedTextColor.GOLD))
                    .append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(com.chagui68.multiversenets.item.Items.formatAmount(fluidMb) + " mB", NamedTextColor.AQUA));
        } else if (totalItems > 0) {
            storageLine = Component.text("Storage: ", NamedTextColor.GRAY)
                    .append(Component.text(NumberFormat.getInstance(Locale.ROOT).format(totalItems) + " items", NamedTextColor.GOLD))
                    .append(Component.text(" (" + view.size() + " types)", NamedTextColor.DARK_GRAY));
        } else if (fluidMb > 0) {
            storageLine = Component.text("Storage: ", NamedTextColor.GRAY)
                    .append(Component.text(NumberFormat.getInstance(Locale.ROOT).format(fluidMb) + " mB fluids", NamedTextColor.AQUA));
        } else {
            int storageNodes = 0;
            for (com.chagui68.multiversenets.item.DeviceType dt : com.chagui68.multiversenets.item.DeviceType.values()) {
                if (dt.isCell() || dt.isBarrel() || dt.isFluidCell() || dt == com.chagui68.multiversenets.item.DeviceType.MVN_GREEDY_CELL) {
                    storageNodes += net.count(dt);
                }
            }
            if (storageNodes > 0) {
                storageLine = Component.text("Storage: ", NamedTextColor.GRAY)
                        .append(Component.text("Ready (" + storageNodes + " cells)", NamedTextColor.DARK_GRAY));
            } else {
                storageLine = Component.text("Storage: ", NamedTextColor.GRAY)
                        .append(Component.text("No Cells Connected", NamedTextColor.DARK_GRAY));
            }
        }

        Component text = header
                .append(Component.newline())
                .append(statusLine)
                .append(Component.newline())
                .append(gridLine)
                .append(Component.newline())
                .append(storageLine);

        textDisplay.text(text);
    }

    /** Mundo + posicion: dos controladores en las mismas coordenadas de mundos distintos compartian holograma. */
    private static String key(World world, long pos) {
        return world.getUID() + "|" + pos;
    }

    public static void removeHologram(World world, long pos) {
        UUID entityId = world == null ? null : HOLOGRAM_ENTITIES.remove(key(world, pos));
        if (entityId != null && world != null) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        if (world != null && HOLO_KEY != null) {
            int x = com.chagui68.multiversenets.util.PosUtil.unpackX(pos);
            int y = com.chagui68.multiversenets.util.PosUtil.unpackY(pos);
            int z = com.chagui68.multiversenets.util.PosUtil.unpackZ(pos);
            if (world.isChunkLoaded(x >> 4, z >> 4)) {
                Location spawnLoc = new Location(world, x + 0.5, y + 1.45, z + 0.5);
                for (Entity nearby : world.getNearbyEntities(spawnLoc, 1.0, 1.0, 1.0)) {
                    if (nearby instanceof TextDisplay td && td.getPersistentDataContainer().has(HOLO_KEY, PersistentDataType.BYTE)) {
                        nearby.remove();
                    }
                }
            }
        }
    }

    public static void clearAll(MultiverseNets plugin) {
        for (Map.Entry<String, UUID> entry : HOLOGRAM_ENTITIES.entrySet()) {
            for (World world : plugin.getServer().getWorlds()) {
                Entity entity = world.getEntity(entry.getValue());
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        HOLOGRAM_ENTITIES.clear();
    }
}
