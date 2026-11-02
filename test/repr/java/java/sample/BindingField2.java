package sample;

// Review 133, finding A: a conditional's arm naming the conditional's pattern binding, which hides an
// int field of the same name. Speculatively (Attr.isBooleanOrNumeric) the binding is not in scope, but
// Resolve.findVar finds the field, an int: the arm is numeric and javac makes the conditional standalone
// (true). The native provider declines a binding whose name resolves to anything in the conditional's
// own environment (before, it answered false: the name taken as erroneous).
class BindingField2 {
  int s;
  Object m(Object o) { return o instanceof Integer s ? s : 0; }
}
