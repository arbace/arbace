package scan.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import scan.api.Plugin;

/** Scans a package of the class path as log4j's and Spring's scanners do: getResources, then each entry's JarFile. */
public class Main {
    static String text(InputStream in) throws IOException {
        try (InputStream s = in) {
            return new String(s.readAllBytes(), "UTF-8").trim();
        }
    }

    static int scan(ClassLoader loader, String pkg) throws Exception {
        Enumeration<URL> urls = loader.getResources(pkg);
        int n = 0;
        while (urls.hasMoreElements()) {
            URL u = urls.nextElement();
            n++;
            URLConnection c = u.openConnection();
            if (!(c instanceof JarURLConnection)) {
                System.out.println("not a jar: " + u.getProtocol());
                continue;
            }
            JarURLConnection jc = (JarURLConnection) c;
            JarFile jar = jc.getJarFile();
            String prefix = jc.getEntryName().endsWith("/") ? jc.getEntryName() : jc.getEntryName() + "/";
            System.out.println("entry " + jar.getName() + ": " + jc.getEntryName() + " (" + (jc.getJarEntry() != null && jc.getJarEntry().isDirectory() ? "a directory" : "?") + ")");
            List<String> names = new ArrayList<>();
            for (Enumeration<JarEntry> es = jar.entries(); es.hasMoreElements();) {
                JarEntry e = es.nextElement();
                if (e.getName().startsWith(prefix) && !e.isDirectory() && e.getName().indexOf('/', prefix.length()) < 0)
                    names.add(e.getName());
            }
            Collections.sort(names);
            for (String name : names) {
                JarEntry e = jar.getJarEntry(name);
                if (name.endsWith(".class")) {
                    String cn = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                    Class<?> k = Class.forName(cn, false, loader);
                    String what = Plugin.class.isAssignableFrom(k) && !k.isInterface()
                        ? "plugin " + ((Plugin) k.getDeclaredConstructor().newInstance()).name() : "class";
                    boolean refused;
                    try {
                        jar.getInputStream(e).close();
                        refused = false;
                    } catch (IOException x) {
                        refused = true;
                    }
                    System.out.println("  " + name + ": " + what + ", bytes refused " + refused);
                } else {
                    System.out.println("  " + name + ": " + text(jar.getInputStream(e)));
                }
            }
        }
        return n;
    }

    public static void main(String[] args) throws Exception {
        ClassLoader loader = Main.class.getClassLoader();
        System.out.println("urls " + scan(loader, "scan/plug"));
        // the control: an absent package lists nothing
        System.out.println("absent " + scan(loader, "scan/none"));
        System.out.println("first " + (loader.getResource("scan/plug") != null));
    }
}
