/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Concurrency"): ThreadLocalRandom's probes for the
 * other packages of java.base in the Go build. jdk26u's Striped64 (java.util.concurrent.atomic)
 * reaches ThreadLocalRandom's package-private getProbe and advanceProbe through
 * SharedSecrets.getJavaUtilConcurrentTLRAccess(), whose holder ThreadLocalRandom$Access
 * registers itself with SharedSecrets, jrt's hand-written shim; Striped64's variant
 * (overlay/jdk/variants/Striped64.clj) calls these instead.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.util.concurrent;

/** ThreadLocalRandom's probe of the current thread, for java.util.concurrent.atomic. */
public final class ThreadLocalRandomProbes {

    private ThreadLocalRandomProbes() {
    }

    /** ThreadLocalRandom.getProbe(): the current thread's probe, 0 until initialized. */
    public static int getProbe() {
        return ThreadLocalRandom.getProbe();
    }

    /** ThreadLocalRandom.advanceProbe(probe). */
    public static int advanceProbe(int probe) {
        return ThreadLocalRandom.advanceProbe(probe);
    }

    /** ThreadLocalRandom.nextSecondarySeed(). */
    public static int nextSecondarySeed() {
        return ThreadLocalRandom.nextSecondarySeed();
    }
}
