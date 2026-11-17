package sample;

// Ghidra's G1 read proof (2026-11-10), case 2: the base of StarImports' and StarImportsBase's static
// on-demand imports, a static field and a static method its subclass StarSub inherits.
class StarBase {
  static final int MASK = 1 << 6;
  static int twice(int x) { return 2 * x; }
}
