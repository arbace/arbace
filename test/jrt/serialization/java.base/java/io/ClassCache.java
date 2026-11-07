/*
 * Arbace (CDR10, doc/CLOJURE-DESIGN-REVIEW.md, the user's decision of 2026-11-06; staged in
 * test/jrt/serialization until serialization joins the world, doc/go/JRT-NOTES.md "Serialization"): jrt's
 * java.io.ClassCache, in place of jdk26u's (openjdk/jdk26u at baf63fb,
 * src/java.base/share/classes/java/io/ClassCache.java), whose ClassValue and soft references the Go
 * build's world has not: the closed world unloads no class, so a class's value is computed once and
 * kept, in a ConcurrentHashMap (computeIfAbsent: one computation per class wins, as ClassValue's).
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
        return map.computeIfAbsent(cl, c -> Objects.requireNonNull(computeValue(c)));
    }
}
