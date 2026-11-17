package sample;

import static sample.StarSub.*;
import static sample.StarBase.*;

// Ghidra's G1 read proof (2026-11-10), case 2's control: StarImports' imports the other way round. The
// last import that includes MASK and twice is StarBase's, their owner: javac names StarBase/MASK and
// StarBase/twice (no clone), OWN StarSub's.
class StarImportsBase {
  static int m() { return MASK | OWN; }
  static int n() { return twice(MASK); }
}
