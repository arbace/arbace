/*
 * Arbace (JCS191 (a), the user's decision of 2026-11-08; doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS191"):
 * the streams the JDK writes for jrt's locks, Semaphore and ForkJoinTask's adapters, whose serial forms name the
 * JDK's hidden classes (ReentrantLock$Sync, $NonfairSync, $FairSync, AbstractQueuedSynchronizer's state,
 * AbstractOwnableSynchronizer, ReentrantReadWriteLock's three and its AbstractQueuedLongSynchronizer, Semaphore's
 * three, ForkJoinTask's status and its writeObject's exception): the oracle of go/arbace/jrt/serial_locks_test.ae
 * (TestSerialLocks). Recorded by hand once with jdk26u's JDK, the lines pasted into the test's jdkLockStreams:
 *   JDK=/root/jdk26u/build/linux-x86_64-server-release/images/jdk
 *   $JDK/bin/javac -d .tmp/serial-record test/jrt/serialization/SerialRecordLocks.java
 *   $JDK/bin/java --add-opens java.base/java.util.concurrent=ALL-UNNAMED -cp .tmp/serial-record SerialRecordLocks
 * Each line: a case's name and the hex of one ObjectOutputStream's bytes holding the case's object alone.
 */
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class SerialRecordLocks {
    static void out(String name, Object o) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (ObjectOutputStream s = new ObjectOutputStream(b)) {
            s.writeObject(o);
        }
        StringBuilder h = new StringBuilder();
        for (byte x : b.toByteArray()) h.append(String.format("%02x", x & 0xff));
        System.out.println(name + " " + h);
    }

    // an adapter made by its constructor (ForkJoinTask.adapt requires a non-null task, which would not be
    // serializable here): the adapted task null
    static ForkJoinTask<?> adapter(String simple, Class<?>[] params, Object... args) throws Exception {
        Constructor<?> k = Class.forName("java.util.concurrent.ForkJoinTask$" + simple).getDeclaredConstructor(params);
        k.setAccessible(true);
        return (ForkJoinTask<?>) k.newInstance(args);
    }

    public static void main(String[] args) throws Exception {
        out("reentrantlock", new ReentrantLock());
        out("reentrantlock-fair", new ReentrantLock(true));
        // held twice by this thread: the state 2 written (a lock read is unlocked: Sync.readObject's setState(0))
        ReentrantLock held = new ReentrantLock();
        held.lock(); held.lock();
        out("reentrantlock-held", held);
        held.unlock(); held.unlock();
        out("rwlock", new ReentrantReadWriteLock());
        out("rwlock-fair", new ReentrantReadWriteLock(true));
        // the write lock held, then the read lock taken by the writer: the state (1 << 16) | 1
        ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
        rw.writeLock().lock(); rw.readLock().lock();
        out("rwlock-held", rw);
        rw.readLock().unlock(); rw.writeLock().unlock();
        out("rwlock-readlock", new ReentrantReadWriteLock().readLock());
        out("semaphore", new Semaphore(3));
        out("semaphore-fair", new Semaphore(-2, true));
        // ForkJoinTask: status (DONE 1 << 31, ABNORMAL 1 << 16, THROWN 1 << 17, the tag's 16 low bits), then the
        // writeObject's exception (aux's, null unless thrown)
        out("fjt-runnable", adapter("AdaptedRunnable", new Class<?>[] {Runnable.class, Object.class}, null, "r"));
        ForkJoinTask<?> done = adapter("AdaptedCallable", new Class<?>[] {Callable.class}, (Object) null);
        @SuppressWarnings("unchecked") ForkJoinTask<Object> d = (ForkJoinTask<Object>) done;
        d.complete("v");
        out("fjt-callable-done", done);
        ForkJoinTask<?> cancelled = adapter("AdaptedRunnableAction", new Class<?>[] {Runnable.class}, (Object) null);
        cancelled.cancel(false);
        out("fjt-action-cancelled", cancelled);
        ForkJoinTask<?> thrown = adapter("AdaptedRunnableAction", new Class<?>[] {Runnable.class}, (Object) null);
        Throwable x = new Throwable("x");
        x.setStackTrace(new StackTraceElement[0]);
        Method m = ForkJoinTask.class.getDeclaredMethod("trySetThrown", Throwable.class);
        m.setAccessible(true);
        m.invoke(thrown, x);
        out("fjt-action-thrown", thrown);
        ForkJoinTask<?> tagged = adapter("AdaptedRunnableAction", new Class<?>[] {Runnable.class}, (Object) null);
        tagged.setForkJoinTaskTag((short) 7);
        out("fjt-action-tagged", tagged);
        // JCS192: the version jrt reports (Runtime.version, the java.base frames' module version)
        System.out.println("# Runtime.version " + Runtime.version() + " feature " + Runtime.version().feature()
            + " interim " + Runtime.version().interim() + " update " + Runtime.version().update()
            + " patch " + Runtime.version().patch() + " pre " + Runtime.version().pre() + " build "
            + Runtime.version().build() + " optional " + Runtime.version().optional() + " hash "
            + Runtime.version().hashCode());
        System.out.println("# java.base " + Object.class.getModule().getDescriptor().rawVersion().orElse("-")
            + " frame " + new Throwable().getStackTrace().length);
        StackTraceElement f = StackTraceElement.class.getName().isEmpty() ? null : java.util.List.of(1).stream()
            .map(i -> new Throwable().getStackTrace()).findFirst().get()[1];
        System.out.println("# frame " + f.getModuleName() + " " + f.getModuleVersion() + " " + f);
        for (String p : new String[] {"java.version", "java.runtime.version", "java.vm.version", "java.version.date",
                                      "java.specification.version", "java.vm.specification.version", "java.class.version",
                                      "java.vendor.version", "java.runtime.name", "java.vm.name"})
            System.out.println("# " + p + " " + System.getProperty(p));
    }
}
