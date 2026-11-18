/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: n-java-f13"): jrt's own, what HotSpot's
 * management interface (jmm) answers for jdk26u's sun.management.ClassLoadingImpl, MemoryImpl, MemoryPoolImpl,
 * MemoryManagerImpl and GarbageCollectorImpl and jdk.management's GcInfoBuilder and GarbageCollectorExtImpl, whose
 * natives their derivations (test/jrt/derive.ae) call here: the platform's memory pools and managers, made once, and
 * their usages over jdk.internal.jrt.HostManagement (go/arbace/jrt/memory.ae). A Go program has two pools, Go's heap
 * (heap: the collector's) and the runtime's own memory (non-heap: goroutine stacks, spans, caches, the collector's
 * metadata), one collector (com.sun.management.internal.GarbageCollectorExtImpl, as jdk26u's VM makes it when
 * jdk.management is present) and a manager of the runtime's pool; neither pool supports usage thresholds yet (they
 * are made with -1, as HotSpot makes a pool without them: the threshold methods throw UnsupportedOperationException).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package sun.management;

import java.lang.management.MemoryManagerMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
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
            new MemoryPoolImpl(HEAP_POOL, true, -1, -1),
            new MemoryPoolImpl(RUNTIME_POOL, false, -1, -1)
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
}
