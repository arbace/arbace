/*
 * Arbace (overlay/jdk, doc/classes/JAVA-DEFERRED-TRIAGE.md, F13, "As built: F13 (write-ahead)"; JCS95 (a), JCS161
 * (a)): jrt's javax.management.ObjectName, a skeleton of the type java.lang.management's API names
 * (PlatformManagedObject.getObjectName, sun.management.Util.newObjectName): JMX is not in the Go build, so no
 * object name can be made. Written for JMX's documented API (https://github.com/openjdk/jdk26u at baf63fb,
 * src/java.management/share/classes/javax/management/ObjectName.java's signatures), without its code: Copyright
 * (c) the Arbace authors; Eclipse Public License 1.0.
 */
package javax.management;

/** An MBean's name: none in the Go build, whose constructors and factories throw. */
public class ObjectName {

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("JMX is not in the Go build (javax.management.ObjectName)");
    }

    public static ObjectName getInstance(String name) throws MalformedObjectNameException {
        throw unsupported();
    }

    public ObjectName(String name) throws MalformedObjectNameException {
        throw unsupported();
    }
}
