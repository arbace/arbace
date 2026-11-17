package cp.plugin;

import cp.core.Names;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;

/** Splits java.class.path and resolves each entry's name to the resources it has. */
public class Main {
    static String read(URL u) throws Exception {
        if (u == null)
            return "-";
        try (InputStream in = u.openStream();
             BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            return r.readLine();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("module " + Names.tag());
        String[] entries = System.getProperty("java.class.path").split(File.pathSeparator);
        System.out.println("entries " + entries.length);
        for (String e : entries) {
            // a directory entry's URL ends with a slash (a URLClassLoader reads one without as a jar)
            URL base = new URL(new File(e).toURI().toURL().toString() + "/");
            try (URLClassLoader cl = new URLClassLoader(new URL[] {base}, null)) {
                System.out.println(e + " app: " + read(cl.getResource("conf/app.txt")));
                System.out.println(e + " core-only: " + read(cl.getResource("conf/core-only.txt")));
                System.out.println(e + " plugin-only: " + read(cl.getResource("conf/plugin-only.txt")));
                System.out.println(e + " protocol: " + cl.getResource("conf/app.txt").getProtocol());
            }
        }
        // the name of no entry finds nothing; the class path's own loader finds the first entry's copy
        URL none = new URLClassLoader(new URL[] {new URL("file:/no/such/entry/")}, null).getResource("conf/app.txt");
        System.out.println("no entry: " + (none == null));
        System.out.println("loader: " + read(Main.class.getClassLoader().getResource("conf/app.txt")));
    }
}
