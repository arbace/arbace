/*
 * Arbace (JCS168, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 slices 2-3 (write-ahead)"): the default
 * serialVersionUIDs of the samples' Serializable classes as jdk26u's ObjectStreamClass.computeDefaultSUID gives
 * them, the record test/jrt/serialization/suid/check.ae compares arbace.classes.suid's with. Recorded by hand:
 *   JDK=/root/jdk26u/build/linux-x86_64-server-release/images/jdk; D=test/jrt/serialization/suid
 *   $JDK/bin/javac -d .tmp/suid-record $D/src/suid/*.java $D/runner/suid/SuidRecord.java
 *   $JDK/bin/java --add-opens java.base/java.io=ALL-UNNAMED -cp .tmp/suid-record suid.SuidRecord > $D/jdk-suids.txt
 */
package suid;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class SuidRecord {
    public static void main(String[] a) throws Exception {
        Method m = ObjectStreamClass.class.getDeclaredMethod("computeDefaultSUID", Class.class);
        m.setAccessible(true);
        List<Class<?>> cs = new ArrayList<>();
        Sample s = new Sample();
        List<?> ls = (List<?>) s.anon(1, "z");
        for (Object o : new Object[] { s, s.new Inner(), s.new Inner().new Deeper(), ls.get(0), ls.get(1), Sample.anonStatic(2) })
            cs.add(o.getClass());
        Object[] os = Sample2.objs();
        for (Object o : os) cs.add(o.getClass());
        for (Field f : os[3].getClass().getDeclaredFields())
            if (f.getName().equals("p")) { f.setAccessible(true); cs.add(f.get(os[3]).getClass()); }
        for (Class<?> c : new Class<?>[] { Sample.Nested.class, Sample.Holder.class, Sample.Rec.class, Sample.Gen.class,
                                          Sample.GenSub.class, Sample.Marker.class, Sample.Empty.class, Sample.Abs.class,
                                          Sample2.Konst.class, Sample2.WithAssert.class })
            cs.add(c);
        Map<String, Long> out = new TreeMap<>();
        for (Class<?> c : cs) out.put(c.getName(), (Long) m.invoke(null, c));
        for (Map.Entry<String, Long> e : out.entrySet()) System.out.println(e.getKey() + " " + e.getValue());
    }
}
