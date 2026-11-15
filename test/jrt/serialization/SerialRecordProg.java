/*
 * Arbace (JCS193 (b), the user's decision of 2026-11-08; doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS193"): the
 * streams the JDK writes for translated JDK classes without serialization hooks, the oracle of
 * go/arbace/jrt/serial_prog_test.ae (TestSerialProg: jrt's test stream over j2g's member tables, bin/jrt test --prog).
 * Recorded by hand once with jdk26u's JDK, the lines pasted into the test's jdkProgStreams:
 *   JDK=/root/jdk26u/build/linux-x86_64-server-release/images/jdk
 *   $JDK/bin/javac -d .tmp/serial-record test/jrt/serialization/SerialRecordProg.java
 *   $JDK/bin/java -cp .tmp/serial-record SerialRecordProg
 * Each line: a case's name and the hex of one ObjectOutputStream's bytes holding the case's object alone.
 */
import java.io.*;
import java.util.*;

public class SerialRecordProg {
    static void out(String name, Object o) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (ObjectOutputStream s = new ObjectOutputStream(b)) {
            s.writeObject(o);
        }
        StringBuilder h = new StringBuilder();
        for (byte x : b.toByteArray()) h.append(String.format("%02x", x & 0xff));
        System.out.println(name + " " + h);
    }

    public static void main(String[] args) throws Exception {
        out("integer", Integer.valueOf(42));
        out("long", Long.valueOf(-5L));
        out("boolean", Boolean.TRUE);
        out("character", Character.valueOf('x'));
        out("double", Double.valueOf(1.5));
        out("uuid", new UUID(0x0123456789abcdefL, 0xfedcba9876543210L));
        out("entry", new AbstractMap.SimpleEntry<String, Integer>("k", 1));
        out("immutable-entry", new AbstractMap.SimpleImmutableEntry<String, String>("k", "v"));
        out("singleton-list", Collections.singletonList("x"));
        out("as-list", Arrays.asList("a", "b"));
    }
}
