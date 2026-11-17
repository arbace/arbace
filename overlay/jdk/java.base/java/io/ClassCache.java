/*
 * Arbace (CDR10, doc/CLOJURE-DESIGN-REVIEW.md, the user's decision of 2026-11-06; JCS167 (a), the user's decision of
 * 2026-11-07, doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS167 (write-ahead)"; doc/go/JRT-NOTES.md,
 * "Serialization"): jrt's
 * java.io.ClassCache, in place of jdk26u's (openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/io/ClassCache.java), whose ClassValue and soft references the Go
 * build's world has not: the closed world unloads no class, so a class's value is computed once and
 * kept, in a ConcurrentHashMap (one computation per class wins, as ClassValue's). Not computeIfAbsent: a value's
 * computation may ask the cache for another class (ObjectStreamClass's constructor looks up its superclass's
 * descriptor), which ConcurrentHashMap refuses inside its own computation ("Recursive update"; h-java-deferred,
 * 2026-11-17): the value is computed outside the map, the first one put kept, as ClassValue does.
 * Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package java.io;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Maps Class instances to values of type T, computed once per class. */
abstract class ClassCache<T> {
    private final ConcurrentHashMap<Class<?>, T> map = new ConcurrentHashMap<>();

    protected abstract T computeValue(Class<?> cl);

    protected ClassCache() {
    }

    T get(Class<?> cl) {
        T v = map.get(cl);
        if (v != null) {
            return v;
        }
        T computed = Objects.requireNonNull(computeValue(cl));
        T prev = map.putIfAbsent(cl, computed);
        return prev != null ? prev : computed;
    }
}
