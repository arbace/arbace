/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Resources by URL"): the Go build's class path as
 * the class loaders' getResource and getResources see it: the resources embedded in the
 * executable (the jar of the JVM build), then the directories of ARBACE_PATH, in RT.load's
 * order (jrt's ResourceOrPath). An embedded resource's URL is a jar: URL naming the executable,
 * "jar:file:/path/to/arbace!/arbace/core.ae", as the JVM's for a resource of arbace.jar,
 * opened by this class's handler (the jar: connection of the JDK is outside the world); a
 * file of ARBACE_PATH has its file: URL, as the JVM's class path directories give it. The
 * natives are jrt's Go (go/arbace/jrt/resources.ae).
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package jdk.internal.jrt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import sun.net.www.ParseUtil;

/** The resources of the Go build's class path, by URL. */
public final class ClassPathResources {

    private ClassPathResources() {
    }

    /** The executable's path (the jar: URLs' file), found once. */
    private static String executable;

    /** The handler of the embedded resources' URLs. */
    private static final EmbeddedHandler handler = new EmbeddedHandler();

    /** Is name an embedded resource? */
    private static native boolean embedded0(String name);

    /** The embedded resource's bytes, or null. */
    private static native byte[] bytes0(String name);

    /** The executable's absolute path ("" when the host cannot tell). */
    private static native String executable0();

    private static String executable() {
        synchronized (ClassPathResources.class) {
            if (executable == null) {
                String p = executable0();
                executable = p.isEmpty() ? "/arbace" : p;
            }
            return executable;
        }
    }

    /**
     * The directories of ARBACE_PATH, in order; not its store entries (HostStore.pathEntries),
     * whose resources have no URL.
     */
    private static List<String> path() {
        List<String> dirs = new ArrayList<>();
        String p = System.getenv("ARBACE_PATH");
        if (p != null) {
            for (String d : HostStore.pathEntries(p)) {
                if (!d.startsWith("store:"))
                    dirs.add(d);
            }
        }
        return dirs;
    }

    /** The URL of an embedded resource. */
    private static URL embeddedURL(String name) {
        try {
            return new URL(null, "jar:" + ParseUtil.fileToEncodedURL(new File(executable())) + "!/" + name, handler);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * The URL of resource name under base, a file: URL of a directory (ending with /), if that
     * file exists, as the JDK's URLClassPath.FileLoader finds it: base resolving the encoded
     * name, a name with ".." only within the directory; null otherwise.
     */
    public static URL inDirectory(URL base, String name) {
        try {
            File dir = new File(ParseUtil.decode(base.getFile().replace('/', File.separatorChar))).getCanonicalFile();
            URL normalizedBase = new URL(base, ".");
            URL url = new URL(base, ParseUtil.encodePath(name, false));
            if (!url.getFile().startsWith(normalizedBase.getFile()))
                return null;
            File file;
            if (name.indexOf("..") != -1) {
                file = new File(dir, name.replace('/', File.separatorChar)).getCanonicalFile();
                if (!file.getPath().startsWith(dir.getPath()))
                    return null;
            } else {
                file = new File(dir, name.replace('/', File.separatorChar));
            }
            return file.exists() ? url : null;
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    /** The file: URL of a directory of ARBACE_PATH, as the JVM's class path makes it, or null. */
    private static URL directoryURL(String d) {
        try {
            File f = new File(d).getAbsoluteFile();
            return f.isDirectory() ? ParseUtil.fileToEncodedURL(f) : null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /** The first URL of resource name on the class path, or null. */
    public static URL find(String name) {
        if (name == null)
            throw new NullPointerException();
        if (embedded0(name))
            return embeddedURL(name);
        for (String d : path()) {
            URL b = directoryURL(d);
            URL u = b == null ? null : inDirectory(b, name);
            if (u != null)
                return u;
        }
        return null;
    }

    /** Every URL of resource name on the class path, in order. */
    public static List<URL> findAll(String name) {
        if (name == null)
            throw new NullPointerException();
        List<URL> urls = new ArrayList<>();
        if (embedded0(name))
            urls.add(embeddedURL(name));
        for (String d : path()) {
            URL b = directoryURL(d);
            URL u = b == null ? null : inDirectory(b, name);
            if (u != null)
                urls.add(u);
        }
        return urls;
    }

    /**
     * ClassLoader.getResource of a loader: a URLClassLoader's own; the application loader's
     * the class path's; the platform loader and the bootstrap loader (null) find none (the
     * JDK's own resources are the embedded ones, found through the application loader).
     */
    public static URL getResource(ClassLoader loader, String name) {
        if (loader instanceof URLClassLoader)
            return ((URLClassLoader) loader).getResource(name);
        if (loader == ClassLoader.getSystemClassLoader())
            return find(name);
        if (name == null)
            throw new NullPointerException();
        return null;
    }

    /** ClassLoader.getResources of a loader, as getResource finds them. */
    public static Enumeration<URL> getResources(ClassLoader loader, String name) throws IOException {
        if (loader instanceof URLClassLoader)
            return ((URLClassLoader) loader).getResources(name);
        if (loader == ClassLoader.getSystemClassLoader())
            return Collections.enumeration(findAll(name));
        if (name == null)
            throw new NullPointerException();
        return Collections.emptyEnumeration();
    }

    /** The handler of the embedded resources' jar: URLs: their connection reads the resource. */
    static final class EmbeddedHandler extends sun.net.www.protocol.jar.Handler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            String s = u.getFile();
            int i = s.indexOf("!/");
            if (i < 0)
                throw new MalformedURLException("no !/ in spec");
            return new EmbeddedConnection(u, ParseUtil.decode(s.substring(i + 2)));
        }
    }

    /** The connection of an embedded resource's URL. */
    static final class EmbeddedConnection extends URLConnection {
        private final String name;
        private byte[] bytes;

        EmbeddedConnection(URL u, String name) {
            super(u);
            this.name = name;
        }

        @Override
        public void connect() throws IOException {
            if (!connected) {
                bytes = bytes0(name);
                if (bytes == null)
                    throw new FileNotFoundException("JAR entry " + name + " not found in " + executable());
                connected = true;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public long getContentLengthLong() {
            try {
                connect();
            } catch (IOException e) {
                return -1;
            }
            return bytes.length;
        }
    }
}
