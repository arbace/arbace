/*
 * Arbace (JCS167's second slice, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 slices 2-3
 * (write-ahead)"): the streams the JDK writes for jrt's hand-written serializable classes, the oracle of
 * go/arbace/jrt/serial_test.ae (TestSerialForms). Recorded by hand once with jdk26u's JDK, the lines pasted
 * into the test's jdkStreams:
 *   JDK=/root/jdk26u/build/linux-x86_64-server-release/images/jdk
 *   $JDK/bin/javac -d .tmp/serial-record test/jrt/serialization/SerialRecord.java
 *   $JDK/bin/java --add-opens java.base/java.lang=ALL-UNNAMED -cp .tmp/serial-record SerialRecord
 * Each line: a case's name and the hex of one ObjectOutputStream's bytes holding the case's object alone.
 */
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class SerialRecord {
    static void out(String name, Object o) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (ObjectOutputStream s = new ObjectOutputStream(b)) {
            s.writeObject(o);
        }
        StringBuilder h = new StringBuilder();
        for (byte x : b.toByteArray()) h.append(String.format("%02x", x & 0xff));
        System.out.println(name + " " + h);
    }

    static StackTraceElement[] trace(String... frames) {
        StackTraceElement[] a = new StackTraceElement[frames.length];
        for (int i = 0; i < frames.length; i++) {
            String[] p = frames[i].split(" ");
            a[i] = new StackTraceElement(p[0], p[1], p[2].equals("-") ? null : p[2], Integer.parseInt(p[3]));
        }
        return a;
    }

    public static void main(String[] args) throws Exception {
        // a Throwable: no cause (the field holds the exception itself), no suppressed (Collections.emptyList())
        Throwable t1 = new Throwable("boom");
        t1.setStackTrace(trace("a.B m B.java 7", "a.C n - -1"));
        out("throwable", t1);
        // a cause and a suppressed exception (an ArrayList)
        Throwable inner = new Throwable("inner");
        inner.setStackTrace(trace("a.D p D.java 3"));
        Throwable t2 = new Throwable("outer", inner);
        t2.setStackTrace(trace("a.B m B.java 8"));
        Throwable sup = new Throwable((String) null);
        sup.setStackTrace(new StackTraceElement[0]);
        t2.addSuppressed(sup);
        out("throwable-cause-suppressed", t2);
        // suppression disabled, stack trace not writable: null suppressed, the sentinel stack trace
        Constructor<Throwable> k = Throwable.class.getDeclaredConstructor(String.class, Throwable.class, boolean.class, boolean.class);
        k.setAccessible(true);
        out("throwable-immutable", k.newInstance("fixed", null, false, false));
        // a null cause given to the constructor
        Throwable t4 = new Throwable("nc", null);
        t4.setStackTrace(new StackTraceElement[0]);
        out("throwable-null-cause", t4);
        out("ste", new StackTraceElement("a.B", "m", "B.java", 12));
        out("ste-module", new StackTraceElement("app", "m.mod", "1.0", "a.B", "m", null, -2));
        out("date", new Date(1234567890123L));
        out("date-negative", new Date(-1L));
        StringBuilder sb = new StringBuilder("hello");
        out("stringbuilder", sb);
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < 20; i++) sb2.append((char) ('a' + i));
        sb2.append('€');
        out("stringbuilder-grown", sb2);
        out("stringbuffer", new StringBuffer("abc"));
        out("atomicinteger", new AtomicInteger(42));
        out("atomiclong", new AtomicLong(-5L));
        out("atomicboolean", new AtomicBoolean(true));
        out("atomicreference", new AtomicReference<Object>("x"));
        out("atomicreference-null", new AtomicReference<Object>());
    }
}
