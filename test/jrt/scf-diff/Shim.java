// JCS353 (a): the differential check of jrt's StringConcatFactory against the JDK's (test/jrt/scf-diff/run.bash).
package scf;
import java.lang.invoke.*;
final class Shim {
  interface Body { Object apply(Object[] a) throws Throwable; }
  static int slots(MethodType t) { int s = 0; for (Class<?> c : t.parameterList()) s += (c == long.class || c == double.class) ? 2 : 1; return s; }
  static MethodHandle adapter(MethodType t, Body b) {
    try {
      MethodHandle h = MethodHandles.lookup().findVirtual(Body.class, "apply", MethodType.methodType(Object.class, Object[].class)).bindTo(b);
      return h.asCollector(Object[].class, t.parameterCount()).asType(t);
    } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
  }
}
