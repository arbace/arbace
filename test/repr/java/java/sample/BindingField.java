package sample;

// Review 133, finding A: a conditional's arm casting the conditional's pattern binding, which hides a
// field of the same name. Speculatively (Attr.isBooleanOrNumeric) the binding is not in scope, but
// Resolve.findVar finds the field, a String: the cast (Integer) of a String fails (Check.checkCastable),
// the arm's type is an error, and javac makes the conditional a poly expression (false). The native
// provider declines a binding whose name resolves to anything in the conditional's own environment.
class BindingField {
  String s = "";
  Object m(Object o) { return o instanceof Integer s ? (Integer) s : 0; }
}
