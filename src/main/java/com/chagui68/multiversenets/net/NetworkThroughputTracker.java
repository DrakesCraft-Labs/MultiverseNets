package com.chagui68.multiversenets.net;

import org.bukkit.Location;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * [EN] Real-time sliding window telemetry engine for network item flow and throughput.
 * [ES] Motor de telemetría de throughput en ventana deslizante en tiempo real para redes y nodos.
 */
public class NetworkThroughputTracker {

    private final AtomicLong totalTransferredItems = new AtomicLong(0);

    // 1-second sliding window
    private volatile long currentSecondEpoch = System.currentTimeMillis() / 1000L;
    private final AtomicInteger currentSecondCount = new AtomicInteger(0);
    private volatile double lastReportedItemsPerSecond = 0.0;

    // Node-level metric map
    private final Map<Long, NodeFlow> nodeFlowMap = new ConcurrentHashMap<>();

    public void recordFlow(long pos, int amount) {
        if (amount <= 0) {
            return;
        }

        totalTransferredItems.addAndGet(amount);

        long nowSec = System.currentTimeMillis() / 1000L;
        if (nowSec != currentSecondEpoch) {
            synchronized (this) {
                if (nowSec != currentSecondEpoch) {
                    long diff = nowSec - currentSecondEpoch;
                    if (diff == 1) {
                        lastReportedItemsPerSecond = currentSecondCount.get();
                    } else {
                        lastReportedItemsPerSecond = 0.0;
                    }
                    currentSecondEpoch = nowSec;
                    currentSecondCount.set(0);
                }
            }
        }
        currentSecondCount.addAndGet(amount);

        if (pos != 0L) {
            nodeFlowMap.computeIfAbsent(pos, k -> new NodeFlow()).record(amount);
        }
    }

    public double getItemsPerSecond() {
        long nowSec = System.currentTimeMillis() / 1000L;
        if (nowSec - currentSecondEpoch > 2) {
            return 0.0;
        }
        if (nowSec == currentSecondEpoch) {
            return Math.max(lastReportedItemsPerSecond, (double) currentSecondCount.get());
        }
        return lastReportedItemsPerSecond;
    }

    public long getTotalTransferredItems() {
        return totalTransferredItems.get();
    }

    public double getNodeItemsPerSecond(long pos) {
        NodeFlow flow = nodeFlowMap.get(pos);
        return flow != null ? flow.getItemsPerSecond() : 0.0;
    }

    public long getNodeTotalTransferred(long pos) {
        NodeFlow flow = nodeFlowMap.get(pos);
        return flow != null ? flow.getTotal() : 0L;
    }

    public void removeNode(long pos) {
        nodeFlowMap.remove(pos);
    }

    public static class NodeFlow {
        private final AtomicLong total = new AtomicLong(0);
        private volatile long currentSecond = System.currentTimeMillis() / 1000L;
        private final AtomicInteger thisSecond = new AtomicInteger(0);
        private volatile double lastRate = 0.0;

        public void record(int count) {
            total.addAndGet(count);
            long nowSec = System.currentTimeMillis() / 1000L;
            if (nowSec != currentSecond) {
                if (nowSec - currentSecond == 1) {
                    lastRate = thisSecond.get();
                } else {
                    lastRate = 0.0;
                }
                currentSecond = nowSec;
                thisSecond.set(0);
            }
            thisSecond.addAndGet(count);
        }

        public double getItemsPerSecond() {
            long nowSec = System.currentTimeMillis() / 1000L;
            if (nowSec - currentSecond > 2) {
                return 0.0;
            }
            if (nowSec == currentSecond) {
                return Math.max(lastRate, (double) thisSecond.get());
            }
            return lastRate;
        }

        public long getTotal() {
            return total.get();
        }
    }
}
