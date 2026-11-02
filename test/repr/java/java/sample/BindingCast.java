package sample;

// Review 117, finding 1 (not borne out): a conditional's arm casting to a box a call that passes the
// conditional's pattern binding. Speculatively (Attr.isBooleanOrNumeric) s is unresolved, and a call
// with an erroneous argument is typed as an error (Attr.checkIdInternal: its method template is
// erroneous), whichever foo Resolve chose; an error is castable to anything, so the cast's type is
// Integer and javac makes the conditional standalone. The real attribution calls foo(Object). The
// native provider answers the arm by its type tree, true, as javac does.
class BindingCast {
  static String foo(Integer x) { return ""; }
  static Object foo(Object x) { return 1; }
  Object m(Object o) { return o instanceof String s ? (Integer) foo(s) : 0; }
}
