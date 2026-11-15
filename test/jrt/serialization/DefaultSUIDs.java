/*
 * Arbace (JCS168, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 slices 2-3 (write-ahead)"): the default
 * serialVersionUIDs jdk26u's ObjectStreamClass.computeDefaultSUID gives the Serializable classes of the world's JDK
 * forms (jdk/forms) that declare none, but enums and records: the record default-suids-jdk26.txt, which the night
 * compares with j2g's (J2G_DEFAULT_SUID=1 J2G_DEFAULT_SUID_OUT=FILE). Recorded by hand:
 *   JDK=/root/jdk26u/build/linux-x86_64-server-release/images/jdk
 *   find jdk/forms -mindepth 2 -name '*.ae' | sed 's|jdk/forms/||; s|\.ae$||; s|/|.|g' > .tmp/jdk-classes.txt
 *   $JDK/bin/javac -d .tmp/suid-record test/jrt/serialization/DefaultSUIDs.java
 *   $JDK/bin/java --add-opens java.base/java.io=ALL-UNNAMED -cp .tmp/suid-record DefaultSUIDs .tmp/jdk-classes.txt \
 *     | sort > test/jrt/serialization/default-suids-jdk26.txt
 * Each class: the top-level classes of the list, their member classes and their anonymous classes $1..$29.
 */
import java.io.*; import java.nio.file.*; import java.util.*;
public class DefaultSUIDs { public static void main(String[] a) throws Exception {
  var m = ObjectStreamClass.class.getDeclaredMethod("computeDefaultSUID", Class.class); m.setAccessible(true);
  for (String top : Files.readAllLines(Path.of(a[0]))) {
    Class<?> t; try { t = Class.forName(top, false, null); } catch (Throwable e) { continue; }
    List<Class<?>> all = new ArrayList<>(); all.add(t);
    for (int i = 0; i < all.size(); i++) for (Class<?> c : all.get(i).getDeclaredClasses()) all.add(c);
    for (int i = 1; i < 30; i++) { try { all.add(Class.forName(top + "$" + i, false, null)); } catch (Throwable e) { break; } }
    for (Class<?> c : all) {
      if (!Serializable.class.isAssignableFrom(c) || c.isEnum() || c.isRecord()) continue;
      boolean d = false; try { var f = c.getDeclaredField("serialVersionUID"); d = java.lang.reflect.Modifier.isStatic(f.getModifiers()); } catch (NoSuchFieldException e) {}
      if (!d) System.out.println(c.getName() + " " + m.invoke(null, c));
    }
  } } }
