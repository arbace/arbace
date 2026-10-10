/*
 * Arbace (overlay/jdk, doc/go/JRT-NOTES.md "Resources by URL"): java.net.URLClassLoader of the
 * Go build, which loads no classes (there are no class files: a class is found by name in the
 * closed world, through the parent) but finds resources as the JDK's does: the parent first,
 * then the loader's URLs in order, a file: URL ending with / a directory (as the JDK's
 * URLClassPath.FileLoader: jdk.internal.jrt.ClassPathResources.inDirectory). Deviations from jdk26u's
 * (src/java.base/share/classes/java/net/URLClassLoader.java, whose public API this keeps): the
 * superclass is ClassLoader (SecureClassLoader and its permissions are outside the world); jar
 * URLs and other protocols find nothing (java.util.jar is outside the world); getResource and
 * getResources are declared here, the Go build's ClassLoader having none of its own.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import jdk.internal.jrt.ClassPathResources;

/** A class loader of URLs: resources only in the Go build. */
public class URLClassLoader extends ClassLoader implements Closeable {

    private final List<URL> urls = new ArrayList<>();
    private final String name;
    private boolean closed;

    public URLClassLoader(URL[] urls, ClassLoader parent) {
        this(null, urls, parent);
    }

    public URLClassLoader(URL[] urls) {
        this(null, urls, ClassLoader.getSystemClassLoader());
    }

    public URLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        this(null, urls, parent);
    }

    public URLClassLoader(String name, URL[] urls, ClassLoader parent) {
        super(parent);
        this.name = name;
        for (URL u : urls) {
            if (u == null)
                throw new NullPointerException();
            this.urls.add(u);
        }
    }

    public URLClassLoader(String name, URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        this(name, urls, parent);
    }

    public static URLClassLoader newInstance(URL[] urls, ClassLoader parent) {
        return new URLClassLoader(urls, parent);
    }

    public static URLClassLoader newInstance(URL[] urls) {
        return new URLClassLoader(urls);
    }

    /** The loader's name, or null. */
    @Override
    public String getName() {
        return name;
    }

    /** The URLs, in search order. */
    public URL[] getURLs() {
        synchronized (urls) {
            return urls.toArray(new URL[0]);
        }
    }

    /** Appends url to the URLs searched (ignored once closed, as the JDK's). */
    protected void addURL(URL url) {
        synchronized (urls) {
            if (url != null && !closed)
                urls.add(url);
        }
    }

    /** Closes the loader: its URLs are no longer searched. */
    @Override
    public void close() throws IOException {
        synchronized (urls) {
            closed = true;
        }
    }

    /** The resource in this loader's URLs, or null. */
    public URL findResource(String name) {
        for (URL base : getURLs()) {
            URL u = inURL(base, name);
            if (u != null)
                return u;
        }
        return null;
    }

    /** The resources in this loader's URLs. */
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> found = new ArrayList<>();
        for (URL base : getURLs()) {
            URL u = inURL(base, name);
            if (u != null)
                found.add(u);
        }
        return Collections.enumeration(found);
    }

    private URL inURL(URL base, String name) {
        synchronized (urls) {
            if (closed)
                return null;
        }
        if ("file".equals(base.getProtocol()) && base.getFile().endsWith("/"))
            return ClassPathResources.inDirectory(base, name);
        return null;
    }

    /** ClassLoader.getResource: the parent's (or the class path's), else findResource. */
    public URL getResource(String name) {
        ClassLoader parent = getParent();
        URL url = parent == null ? null : ClassPathResources.getResource(parent, name);
        if (url == null)
            url = findResource(name);
        return url;
    }

    /** ClassLoader.getResources: the parent's, then findResources'. */
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> all = new ArrayList<>();
        ClassLoader parent = getParent();
        if (parent != null)
            all.addAll(Collections.list(ClassPathResources.getResources(parent, name)));
        all.addAll(Collections.list(findResources(name)));
        return Collections.enumeration(all);
    }

    /** The resource's stream, as getResource finds it, or null. */
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        try {
            return url == null ? null : url.openStream();
        } catch (IOException e) {
            return null;
        }
    }
}
