// JCS353 (a): the differential check of jrt's StringConcatFactory against the JDK's (test/jrt/scf-diff/run.bash).
package scf;
import java.lang.invoke.*;
import java.util.*;
public class Diff {
  static int n = 0, bad = 0;
  static String run(boolean ours, MethodType t, String recipe, Object[] consts, Object[] args) {
    try {
      CallSite cs = ours ? scf.StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "x", t, recipe, consts)
                         : java.lang.invoke.StringConcatFactory.makeConcatWithConstants(MethodHandles.lookup(), "x", t, recipe, consts);
      return "= " + cs.getTarget().type() + " " + cs.getTarget().invokeWithArguments(args);
    } catch (Throwable e) { return "! " + e.getClass().getName() + ": " + e.getMessage(); }
  }
  static void c(MethodType t, String recipe, Object[] consts, Object... args) {
    n++;
    String a = run(false, t, recipe, consts, args), b = run(true, t, recipe, consts, args);
    if (!a.equals(b)) { bad++; System.out.println("DIFF " + t + " " + recipe.replace("\1","<1>").replace("\2","<2>") + "\n  jdk:  " + a + "\n  ours: " + b); }
  }
  static Object nullStr = new Object() { public String toString() { return null; } };
  public static void main(String[] x) throws Throwable {
    Object[] none = {};
    c(MethodType.methodType(String.class), "", none);
    c(MethodType.methodType(String.class), "abc", none);
    c(MethodType.methodType(String.class), "a\2b", new Object[]{42});
    c(MethodType.methodType(String.class, int.class), "\1", none, 7);
    c(MethodType.methodType(String.class, short.class), "\1", none, (short)7);
    c(MethodType.methodType(String.class, byte.class), "\1", none, (byte)-3);
    c(MethodType.methodType(String.class, char.class), "\1", none, 'q');
    c(MethodType.methodType(String.class, boolean.class), "\1", none, true);
    c(MethodType.methodType(String.class, float.class), "\1", none, 1.5f);
    c(MethodType.methodType(String.class, double.class), "\1", none, 1e100);
    c(MethodType.methodType(String.class, long.class), "\1", none, Long.MIN_VALUE);
    c(MethodType.methodType(String.class, Object.class), "\1", none, (Object) null);
    c(MethodType.methodType(String.class, Object.class), "\1", none, nullStr);
    c(MethodType.methodType(String.class, Object.class), "p\1", none, "v");
    c(MethodType.methodType(String.class, Object.class), "\1s", none, (Object) null);
    c(MethodType.methodType(String.class, Object.class, String.class), "\1\1", none, 1, null);
    c(MethodType.methodType(String.class, int.class), "p\1s", none, 3);
    c(MethodType.methodType(Object.class, int.class, char.class, Object.class), "[\1|\2|\1|\2\1]", new Object[]{"K", 9L}, 1, 'c', nullStr);
    c(MethodType.methodType(CharSequence.class, double.class, float.class), "\1\1", none, -0.0, Float.NaN);
    // the checks
    c(MethodType.methodType(Integer.class, int.class), "\1", none, 1);
    c(MethodType.methodType(String.class, int.class), "\1\1", none, 1);
    c(MethodType.methodType(String.class, int.class, int.class), "\1", none, 1, 2);
    c(MethodType.methodType(String.class), "\2", none);
    c(MethodType.methodType(String.class), "x", new Object[]{"unused"});
    c(MethodType.methodType(String.class), "x\2", new Object[]{null});
    Class<?>[] ts = new Class<?>[201]; Arrays.fill(ts, int.class);
    c(MethodType.methodType(String.class, ts), "\1".repeat(201), none, new Object[201]);
    ts = new Class<?>[100]; Arrays.fill(ts, long.class); Object[] as = new Object[100]; Arrays.fill(as, 5L);
    c(MethodType.methodType(String.class, ts), "\1".repeat(100), none, as);
    try { scf.StringConcatFactory.makeConcat(MethodHandles.publicLookup(), "x", MethodType.methodType(String.class)); System.out.println("no exception"); bad++; }
    catch (StringConcatException e) { if (!e.getMessage().startsWith("Invalid caller: ")) { bad++; System.out.println(e); } }
    n++;
    System.out.println("diff: " + n + " cases, " + bad + " differ");
    System.exit(bad == 0 ? 0 : 1);
  }
}
