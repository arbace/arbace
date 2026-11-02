package sample;

import static sample.BindingImport.Util.f;

// Review 117, finding 2: a conditional's arm calling, unqualified, a method of a static import with
// the conditional's pattern binding. Speculatively (Attr.isBooleanOrNumeric) s is unresolved, both
// imported f apply and f(Integer) is chosen, returning int: javac makes the conditional standalone.
// The real attribution calls f(Object). The native provider declines the arm (it answered false, poly,
// as no enclosing class has an f, between 9946ce53 and the fix).
class BindingImport {
  static class Util {
    static int f(Integer x) { return 0; }
    static String f(Object x) { return ""; }
  }
  Object m(Object o) { return o instanceof String s ? f(s) : 0; }
}
