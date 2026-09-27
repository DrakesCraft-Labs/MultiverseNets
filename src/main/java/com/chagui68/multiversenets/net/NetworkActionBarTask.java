package com.chagui68.multiversenets.net;

import com.chagui68.multiversenets.MultiverseNets;
import com.chagui68.multiversenets.item.DeviceType;
import com.chagui68.multiversenets.item.Items;
import com.chagui68.multiversenets.persist.NodeBlob;
import com.chagui68.multiversenets.persist.NodeStore;
import com.chagui68.multiversenets.util.PosUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * [EN] Lightweight periodic task (runs every 10 ticks = 0.5s) displaying live telemetry
 * in the action bar when a player is holding a network tool or sneaking.
 *
 * [ES] Tarea periódica ligera (cada 10 ticks) que muestra telemetría en vivo en el
 * action bar al sostener una herramienta de red o agacharse mirando un nodo.
 */
public class NetworkActionBarTask extends BukkitRunnable {

    private final MultiverseNets plugin;

    public NetworkActionBarTask(MultiverseNets plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid()) {
                continue;
            }

            boolean isHoldingTool = isHoldingNetworkTool(player);
            boolean isSneaking = player.isSneaking();

            if (!isHoldingTool && !isSneaking) {
                continue;
            }

            RayTraceResult trace = player.rayTraceBlocks(6.0);
            if (trace == null || trace.getHitBlock() == null) {
                continue;
            }

            Block block = trace.getHitBlock();
            NodeBlob blob = NodeStore.get(block);
            if (blob == null) {
                continue;
            }

            DeviceType type = DeviceType.parse(blob.typeName);
            String name = type != null ? type.display() : blob.typeName;

            Network net = plugin.networks().networkAt(block);
            long pos = PosUtil.pack(block.getX(), block.getY(), block.getZ());

            if (net == null) {
                player.sendActionBar(Component.text(name + " · ", NamedTextColor.GRAY)
                        .append(Component.text("NO ACTIVE NETWORK", NamedTextColor.RED)));
                continue;
            }

            double nodeRate = net.throughput().getNodeItemsPerSecond(pos);
            long nodeTotal = net.throughput().getNodeTotalTransferred(pos);
            double netRate = net.throughput().getItemsPerSecond();

            Component msg;
            if (type == DeviceType.MVN_CONTROLLER) {
                msg = Component.text("⚡ Controller ", NamedTextColor.GOLD)
                        .append(Component.text("· Nodos: ", NamedTextColor.GRAY))
                        .append(Component.text(net.size(), NamedTextColor.WHITE))
                        .append(Component.text(" · Flujo: ", NamedTextColor.GRAY))
                        .append(Component.text(String.format(Locale.ROOT, "+%.1f/s", netRate), NamedTextColor.GREEN));
            } else if (type != null && type.isCell()) {
                long cap = Items.capacityOf(type);
                msg = Component.text("📦 " + name + " ", NamedTextColor.AQUA)
                        .append(Component.text("· Carga: ", NamedTextColor.GRAY))
                        .append(Component.text(Items.formatAmount(blob.cellAmount) + "/" + Items.formatAmount(cap), NamedTextColor.YELLOW));
            } else if (type == DeviceType.MVN_INFINITY_BARREL) {
                msg = Component.text("🛢 Infinity Barrel ", NamedTextColor.GOLD)
                        .append(Component.text("· Carga: ", NamedTextColor.GRAY))
                        .append(Component.text(NumberFormat.getInstance().format(blob.cellAmount), NamedTextColor.YELLOW));
            } else {
                msg = Component.text(name + " ", NamedTextColor.AQUA)
                        .append(Component.text("· Flujo: ", NamedTextColor.GRAY))
                        .append(Component.text(String.format(Locale.ROOT, "+%.1f/s", nodeRate), NamedTextColor.GREEN))
                        .append(Component.text(" · Total: ", NamedTextColor.GRAY))
                        .append(Component.text(NumberFormat.getInstance().format(nodeTotal), NamedTextColor.YELLOW));
            }

            player.sendActionBar(msg);
        }
    }

    private static boolean isHoldingNetworkTool(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        DeviceType dtMain = Items.typeOf(main);
        if (dtMain == DeviceType.MVN_CONFIGURATOR || dtMain == DeviceType.MVN_RAKE || dtMain == DeviceType.MVN_PROBE) {
            return true;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        DeviceType dtOff = Items.typeOf(off);
        return dtOff == DeviceType.MVN_CONFIGURATOR || dtOff == DeviceType.MVN_RAKE || dtOff == DeviceType.MVN_PROBE;
    }
}
