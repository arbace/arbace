package sample;

import java.util.function.Supplier;

// Ghidra's G1 read proof (2026-11-10), case 1: a method overloaded on Runnable and Supplier<T>, called
// with a method reference. A reference to a void method is not compatible with Supplier's value
// returning descriptor (Attr.checkReferenceCompatible: the descriptor's return not void, the
// referent's void, an incompatible return type), so only run(Runnable) applies: javac picks it. A
// reference to a value method is compatible with both, and Supplier<T> is the more specific (JLS
// 15.12.2.5: the other descriptor's return is void): the control, javac picks run(Supplier).
class MrefVoid {
  static void run(Runnable r) { r.run(); }
  static <T> T run(Supplier<T> s) { return s.get(); }
  void work() { }
  String value() { return ""; }
  void go() { run(this::work); }
  String control() { return run(this::value); }
}
