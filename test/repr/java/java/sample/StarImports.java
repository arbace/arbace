package sample;

import static sample.StarBase.*;
import static sample.StarSub.*;

// Ghidra's G1 read proof (2026-11-10), case 2: an inherited static member reached through two static
// on-demand imports, the base's and the subclass's. StarImportScope prepends each import's scope
// (Scope.java's importAll), so its getOrigin is the last import that includes the member, StarSub's;
// Resolve.findVar and findFun clone the member with that origin as owner: javac names StarSub/MASK and
// StarSub/twice. StarImportsBase, the imports the other way round, is the control.
class StarImports {
  static int m() { return MASK | OWN; }
  static int n() { return twice(MASK); }
}
