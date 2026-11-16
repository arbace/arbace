/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: F13 (write-ahead)"; JCS95 (a), JCS161
 * (a)): jrt's javax.management.openmbean.CompositeData, the type java.lang.management's from(CompositeData)
 * methods name, without getCompositeType (JMX's open types are not in the Go build): no value of it is made in
 * the Go build, and a program's own implementation is an interface of its keys and values. Written for JMX's
 * documented API (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.management/share/classes/javax/management/openmbean/CompositeData.java's signatures), without its
 * code: Copyright (c) the Arbace authors; Eclipse Public License 1.0.
 */
package javax.management.openmbean;

import java.util.Collection;

/** Composite open data: its items by key. */
public interface CompositeData {

    Object get(String key);

    Object[] getAll(String[] keys);

    boolean containsKey(String key);

    boolean containsValue(Object value);

    Collection<?> values();
}
