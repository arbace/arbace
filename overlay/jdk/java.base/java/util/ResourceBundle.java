/*
 * jrt's own java.util.ResourceBundle for the Go build (doc/go/JRT-NOTES.md, "Time"). Its
 * lookups (getObject, containsKey, keySet, handleKeySet and the key set's computation) are
 * transcribed from openjdk/jdk26u at baf63fb, src/java.base/share/classes/java/util/ResourceBundle.java,
 * Copyright (c) Oracle and/or its affiliates, under the GNU General Public License version 2
 * with the Classpath Exception (LICENSE.md); the rest is Arbace's own.
 */
package java.util;

/**
 * java.util.ResourceBundle for the Go build: a bundle's lookups through its parent chain, as
 * the JDK's, for the bundles the Go build has as classes (the CLDR locale data of java.base,
 * which jrt's locale providers chain themselves). Not here: getBundle and its loading,
 * caching and control (there are no class loaders or modules to load bundles from), and
 * clearCache.
 */
public abstract class ResourceBundle {
    /** The parent bundle, consulted by getObject when this bundle has no value for a key. */
    protected ResourceBundle parent = null;

    private volatile Set<String> handleKeys;

    /** Sole constructor. */
    public ResourceBundle() {
    }

    /** The base name of the bundle: null, as for a bundle not loaded by getBundle. */
    public String getBaseBundleName() {
        return null;
    }

    public final String getString(String key) {
        return (String) getObject(key);
    }

    public final String[] getStringArray(String key) {
        return (String[]) getObject(key);
    }

    public final Object getObject(String key) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            if (parent != null) {
                obj = parent.getObject(key);
            }
            if (obj == null) {
                throw new MissingResourceException("Can't find resource for bundle "
                                                   + this.getClass().getName()
                                                   + ", key " + key,
                                                   this.getClass().getName(),
                                                   key);
            }
        }
        return obj;
    }

    /** The locale of the bundle: null, as for a bundle not loaded by getBundle. */
    public Locale getLocale() {
        return null;
    }

    protected void setParent(ResourceBundle parent) {
        this.parent = parent;
    }

    protected abstract Object handleGetObject(String key);

    public abstract Enumeration<String> getKeys();

    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            if (rb.handleKeySet().contains(key)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        for (ResourceBundle rb = this; rb != null; rb = rb.parent) {
            keys.addAll(rb.handleKeySet());
        }
        return keys;
    }

    protected Set<String> handleKeySet() {
        Set<String> keys = handleKeys;
        if (keys == null) {
            keys = new HashSet<>();
            Enumeration<String> enumKeys = getKeys();
            while (enumKeys.hasMoreElements()) {
                String key = enumKeys.nextElement();
                if (handleGetObject(key) != null) {
                    keys.add(key);
                }
            }
            handleKeys = keys;
        }
        return keys;
    }
}
