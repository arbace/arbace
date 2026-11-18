/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: n-java-f13"): jrt's own, what HotSpot's
 * management interface (jmm) answers for jdk26u's sun.management.ClassLoadingImpl, MemoryImpl, MemoryPoolImpl,
 * MemoryManagerImpl and GarbageCollectorImpl and jdk.management's GcInfoBuilder and GarbageCollectorExtImpl, whose
 * natives their derivations (test/jrt/derive.ae) call here: the platform's memory pools and managers, made once, and
 * their usages over jdk.internal.jrt.HostManagement (go/arbace/jrt/memory.ae). A Go program has two pools, Go's heap
 * (heap: the collector's) and the runtime's own memory (non-heap: goroutine stacks, spans, caches, the collector's
 * metadata), one collector (com.sun.management.internal.GarbageCollectorExtImpl, as jdk26u's VM makes it when
 * jdk.management is present) and a manager of the runtime's pool. Since n-java-thresh ("As built: n-java-thresh") both
 * pools support a usage threshold and the heap a collection usage threshold (made with 0, disabled, as HotSpot makes a
 * collected pool; the runtime's pool is not collected: -1), and the collector sends its notifications: HotSpot's
 * LowMemoryDetector, SensorInfo and GCNotifier here, their events Go's (memory.ae's sampler, the watcher's queue of
 * cycles and the notifier goroutine, HotSpot's Notification Thread).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.management;

import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import jdk.internal.jrt.HostManagement;

/** The platform's memory pools and managers and the natives of their beans. */
public final class JrtManagement {

    private JrtManagement() {
    }

    /** The heap pool's name: Go's heap objects. */
    public static final String HEAP_POOL = "Go heap";

    /** The non-heap pool's name: the runtime's own memory. */
    public static final String RUNTIME_POOL = "Go runtime";

    /** The collector's name: Go's concurrent mark and sweep collector. */
    public static final String COLLECTOR = "Go GC";

    /** The runtime pool's manager. */
    public static final String RUNTIME_MANAGER = "Go runtime manager";

    private static MemoryPoolMXBean[] pools;
    private static MemoryManagerMXBean[] managers;

    private static synchronized void init() {
        if (pools != null) {
            return;
        }
        HostManagement.watchGc0();
        pools = new MemoryPoolMXBean[] {
            new MemoryPoolImpl(HEAP_POOL, true, 0, 0),
            new MemoryPoolImpl(RUNTIME_POOL, false, 0, -1)
        };
        managers = new MemoryManagerMXBean[] {
            new com.sun.management.internal.GarbageCollectorExtImpl(COLLECTOR),
            new MemoryManagerImpl(RUNTIME_MANAGER)
        };
    }

    /** MemoryImpl's getMemoryPools0: the pools, the heap's first. */
    static MemoryPoolMXBean[] pools() {
        init();
        return pools.clone();
    }

    /** MemoryImpl's getMemoryManagers0: the managers, the collector first. */
    static MemoryManagerMXBean[] managers() {
        init();
        return managers.clone();
    }

    /** MemoryManagerImpl's getMemoryPools0: the collector's the heap, the runtime manager's the runtime's pool. */
    static MemoryPoolMXBean[] poolsOf(MemoryManagerMXBean m) {
        init();
        return new MemoryPoolMXBean[] { m == managers[0] ? pools[0] : pools[1] };
    }

    /** MemoryPoolImpl's getMemoryManagers0. */
    static MemoryManagerMXBean[] managersOf(MemoryPoolMXBean p) {
        init();
        return new MemoryManagerMXBean[] { p == pools[0] ? managers[0] : managers[1] };
    }

    private static MemoryUsage usage(long[] u) {
        return new MemoryUsage(u[0], u[1], u[2], u[3]);
    }

    /** The usage u with used bytes used: committed at least used, max (when defined) at least committed. */
    private static MemoryUsage usage(long[] u, long used) {
        long committed = Math.max(u[2], used);
        long max = u[3] == -1 ? -1 : Math.max(u[3], committed);
        return new MemoryUsage(u[0], used, committed, max);
    }

    /** The heap's (MemoryImpl: all of it) or the runtime pool's usage now. */
    static MemoryUsage usage(boolean heap) {
        return usage(HostManagement.memory0(heap ? 0 : 1));
    }

    /** The pool's peak usage. */
    static MemoryUsage peak(boolean heap) {
        return usage(HostManagement.peak0(heap ? 0 : 1));
    }

    /** Makes the pool's peak its usage now. */
    static void resetPeak(boolean heap) {
        HostManagement.resetPeak0(heap ? 0 : 1);
    }

    /**
     * The pool's usage after the latest cycle: the heap's the live heap the cycle left (0 used before the first),
     * the runtime pool's none (no collector manages it: null, as HotSpot's for a pool not collected).
     */
    static MemoryUsage collectionUsage(boolean heap) {
        if (!heap) {
            return null;
        }
        long[] u = HostManagement.memory0(0);
        long[] g = HostManagement.lastGc0();
        return usage(u, g == null ? 0 : g[4]);
    }

    /** ClassLoadingImpl's and MemoryImpl's verbose settings (jrt prints nothing for them yet). */
    private static volatile boolean verboseClass;
    private static volatile boolean verboseGC;

    static void setVerboseClass(boolean value) {
        verboseClass = value;
    }

    static boolean verboseClass() {
        return verboseClass;
    }

    static void setVerboseGC(boolean value) {
        verboseGC = value;
    }

    static boolean verboseGC() {
        return verboseGC;
    }

    /**
     * GcInfoBuilder's getLastGcInfo0: the latest cycle's GcInfo (through GcInfo's factory), null before the first;
     * before and after are filled in poolNames' order (the heap's from the cycle, the runtime pool's its usage now).
     */
    public static Object lastGcInfo(Object builder, String[] poolNames, MemoryUsage[] before, MemoryUsage[] after,
                                    Object[] extAttValues) {
        long[] g = HostManagement.lastGc0();
        if (g == null) {
            return null;
        }
        for (int i = 0; i < poolNames.length; i++) {
            if (HEAP_POOL.equals(poolNames[i])) {
                long[] u = HostManagement.memory0(0);
                before[i] = usage(u, g[3]);
                after[i] = usage(u, g[4]);
            } else {
                MemoryUsage u = usage(false);
                before[i] = u;
                after[i] = u;
            }
        }
        return HostManagement.gcInfoFactory().newGcInfo(builder, g[0], g[1], g[2], before, after, extAttValues);
    }

    // ---------------------------------------------------------------------------------------
    // n-java-thresh (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: n-java-thresh"): HotSpot's LowMemoryDetector,
    // SensorInfo and GCNotifier (src/hotspot/share/services/lowMemoryDetector.cpp, gcNotifier.cpp)

    /** A usage threshold (USAGE) or a collection usage threshold (COLLECTION). */
    static final int USAGE = 0;
    static final int COLLECTION = 1;

    /**
     * One sensor of a pool: HotSpot's SensorInfo with the pool's ThresholdSupport of its kind (high and low, set to
     * the same value, as MemoryPoolImpl's natives set them; 0 disabled). Guarded by lock, Notification_lock.
     */
    private static final class SensorInfo {
        Sensor sensor;
        long high;
        long low;
        boolean on;
        long count;
        int pendingTrigger;
        int pendingClear;
        MemoryUsage usage;

        boolean enabled() {
            return sensor != null && high != 0;
        }

        boolean pending() {
            return pendingTrigger > 0 || pendingClear > 0;
        }

        boolean overHigh(MemoryUsage u) {
            return high > 0 && u.getUsed() >= high;
        }

        boolean belowLow(MemoryUsage u) {
            return low > 0 && u.getUsed() < low;
        }

        /** set_gauge_sensor_level: triggered at the first crossing above high, cleared at the first below low. */
        void gauge(MemoryUsage u) {
            boolean over = overHigh(u);
            boolean below = belowLow(u);
            if (over && ((!on && pendingTrigger == 0) || pendingClear > 0)) {
                pendingTrigger++;
                usage = u;
                pendingClear = 0;
            } else if (below && ((on && pendingClear == 0) || (pendingTrigger > 0 && pendingClear == 0))) {
                pendingClear++;
            }
        }

        /** set_counter_sensor_level: triggered at every collection that leaves the pool above high. */
        void counter(MemoryUsage u) {
            boolean over = overHigh(u);
            boolean below = belowLow(u);
            if (over) {
                pendingTrigger++;
                usage = u;
                pendingClear = 0;
            } else if (below && (on || pendingTrigger > 0)) {
                pendingClear++;
            }
        }
    }

    private static final Object lock = new Object();

    /** The pools' sensors, [pool][kind]: the heap's and the runtime pool's (whose collection sensor is never set). */
    private static final SensorInfo[][] sensors = {
        { new SensorInfo(), new SensorInfo() },
        { new SensorInfo(), new SensorInfo() }
    };

    private static int index(MemoryPoolMXBean p) {
        init();
        return p == pools[0] ? 0 : 1;
    }

    /** MemoryPoolImpl's setPoolUsageSensor (kind USAGE) and setPoolCollectionSensor (COLLECTION). */
    static void setSensor(MemoryPoolMXBean p, int kind, Sensor s) {
        int i = index(p);
        synchronized (lock) {
            sensors[i][kind].sensor = s;
        }
        startNotifier();
    }

    /**
     * MemoryPoolImpl's setUsageThreshold0 (kind USAGE) and setCollectionThreshold0 (COLLECTION), jmm's
     * SetPoolThreshold: both thresholds set; a usage threshold's pool checked at once (the collection's at the next
     * cycle), the sampler on while a usage threshold is set, the cycles queued while a collection threshold is.
     */
    static void setThreshold(MemoryPoolMXBean p, int kind, long threshold) {
        int i = index(p);
        SensorInfo s = sensors[i][kind];
        long prev;
        synchronized (lock) {
            prev = s.high;
            s.high = threshold;
            s.low = threshold;
        }
        if (kind == COLLECTION) {
            gcEvents();
            return;
        }
        if (prev != threshold) {
            boolean any;
            synchronized (lock) {
                any = sensors[0][USAGE].high != 0 || sensors[1][USAGE].high != 0;
            }
            HostManagement.sampleUsage0(any);
            if (detectUsage(i)) {
                HostManagement.notify0();
            }
        }
    }

    /** detect_low_memory(pool): pool i's usage against its usage threshold; whether a request is pending. */
    private static boolean detectUsage(int i) {
        SensorInfo s = sensors[i][USAGE];
        synchronized (lock) {
            if (!s.enabled()) {
                return false;
            }
        }
        MemoryUsage u = usage(i == 0);
        synchronized (lock) {
            if (!s.enabled()) {
                return false;
            }
            s.gauge(u);
            return s.pending();
        }
    }

    /** The collector's notifications' sender, GarbageCollectorExtImpl's while it has listeners, else null. */
    public interface GcNotifier {
        void notifyGc(long timestamp, String gcAction, String gcCause, long index, long startTime, long endTime,
                      MemoryUsage[] before, MemoryUsage[] after);
    }

    private static volatile GcNotifier gcNotifier;

    /** GarbageCollectorExtImpl's setNotificationEnabled: its sender, or null when disabled. */
    public static void setGcNotifier(GcNotifier n) {
        init();
        gcNotifier = n;
        if (n != null) {
            startNotifier();
        }
        gcEvents();
    }

    /** The watcher queues the cycles while a collection threshold is set or the notifications are enabled. */
    private static void gcEvents() {
        boolean on;
        synchronized (lock) {
            on = sensors[0][COLLECTION].high != 0 || gcNotifier != null;
        }
        HostManagement.gcEvents0(on);
    }

    private static boolean notifierStarted;

    private static synchronized void startNotifier() {
        if (!notifierStarted) {
            notifierStarted = true;
            HostManagement.setNotifier0(new Notifier());
        }
    }

    /** GCNotifier's action: Go's collector marks the whole heap at every cycle. */
    static final String GC_ACTION = "end of major GC";

    /** GCCause's for a forced cycle (System.gc, runtime.GC). */
    static final String GC_CAUSE_FORCED = "System.gc()";

    /** The cause of a cycle Go's pacer started: the heap reached its goal. */
    static final String GC_CAUSE_GOAL = "Go heap goal";

    /**
     * HotSpot's Notification Thread, run by memory.ae's notifier goroutine at each event: the usages checked against
     * their thresholds, the queued cycles' collection usage against the heap's collection threshold, the sensors'
     * pending requests processed (LowMemoryDetector::process_sensor_changes), then each cycle's notification sent.
     */
    private static final class Notifier implements Runnable {
        @Override
        public void run() {
            for (int i = 0; i < 2; i++) {
                detectUsage(i);
            }
            List<long[]> cycles = new ArrayList<>();
            long[] g;
            while ((g = HostManagement.nextGc0()) != null) {
                cycles.add(g);
                SensorInfo s = sensors[0][COLLECTION];
                MemoryUsage after = usage(HostManagement.memory0(0), g[4]);
                synchronized (lock) {
                    if (s.enabled()) {
                        s.counter(after);
                    }
                }
            }
            for (SensorInfo[] ss : sensors) {
                for (SensorInfo s : ss) {
                    try {
                        process(s);
                    } catch (Throwable t) {
                        // dropped, as the Notification Thread clears a pending exception
                    }
                }
            }
            GcNotifier n = gcNotifier;
            if (n == null) {
                return;
            }
            for (long[] c : cycles) {
                long[] u = HostManagement.memory0(0);
                MemoryUsage r = usage(false);
                MemoryUsage[] before = { usage(u, c[3]), r };
                MemoryUsage[] after = { usage(u, c[4]), r };
                try {
                    n.notifyGc(c[5], GC_ACTION, c[6] != 0 ? GC_CAUSE_FORCED : GC_CAUSE_GOAL, c[0], c[1], c[2], before,
                               after);
                } catch (Throwable t) {
                    // dropped
                }
            }
        }

        /** SensorInfo::process_pending_requests: a pending clear, else the pending triggers. */
        private static void process(SensorInfo s) {
            int n;
            boolean clear;
            MemoryUsage u;
            Sensor sensor;
            synchronized (lock) {
                if (!s.pending() || s.sensor == null) {
                    return;
                }
                n = s.pendingTrigger;
                clear = s.pendingClear > 0;
                u = s.usage;
                sensor = s.sensor;
                if (clear) {
                    s.on = false;
                    s.count += n;
                    s.pendingClear = 0;
                    s.pendingTrigger -= n;
                }
            }
            if (clear) {
                sensor.clear(n);
                return;
            }
            try {
                sensor.trigger(n, u);
            } finally {
                synchronized (lock) {
                    s.on = true;
                    s.count += n;
                    s.pendingTrigger -= n;
                }
            }
        }
    }
}
