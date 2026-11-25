/*
 * The JDK's side of test/bytecode/check.ae (doc/classes/BYTECODE-SURVEY.md), run by bin/bytecode-check --record-jdk on
 * jdk26u's own build with -Xverify:all:
 *   verdicts NAME MUTANTS DEPS  each mutant class file of directory MUTANTS (sorted) defined as class NAME by a loader of
 *                               its own (the other classes from directory DEPS) and initialized: ok, VerifyError,
 *                               ClassFormatError or another error's name, one a line
 *   run DIR CLASS METHOD [INT]  the static method's result (its one int argument, if any), the classes from DIR
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;

public class HotSpotVerdicts {
    public static void main(String[] a) throws Exception {
        if (a[0].equals("run")) {
            ClassLoader l = new URLClassLoader(new URL[] { new File(a[1]).toURI().toURL() }, null);
            Class<?> c = Class.forName(a[2], true, l);
            Object r;
            if (a.length > 4) {
                Method m = c.getDeclaredMethod(a[3], int.class);
                m.setAccessible(true);
                r = m.invoke(null, Integer.parseInt(a[4]));
            } else {
                Method m = c.getDeclaredMethods()[0];
                for (Method x : c.getDeclaredMethods()) if (x.getName().equals(a[3])) m = x;
                m.setAccessible(true);
                r = m.getParameterCount() == 0 ? m.invoke(null) : m.invoke(null, (Object) new String[0]);
            }
            System.out.println(r);
            return;
        }
        String name = a[1];
        URL[] cp = { new File(a[3]).toURI().toURL() };
        File[] fs = new File(a[2]).listFiles();
        java.util.Arrays.sort(fs);
        for (File f : fs) {
            byte[] b = Files.readAllBytes(f.toPath());
            ClassLoader l = new ClassLoader(new URLClassLoader(cp, null)) {
                protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {
                    if (n.equals(name)) {
                        synchronized (this) {
                            Class<?> c = findLoadedClass(n);
                            return c != null ? c : defineClass(n, b, 0, b.length);
                        }
                    }
                    return super.loadClass(n, r);
                }
            };
            String v;
            try { Class.forName(name, true, l); v = "ok"; }
            catch (ExceptionInInitializerError e) { v = "ok"; }
            catch (VerifyError e) { v = "VerifyError"; }
            catch (ClassFormatError e) { v = "ClassFormatError"; }
            catch (Throwable e) { v = "other: " + e.getClass().getName(); }
            System.out.println(v);
        }
    }
}
