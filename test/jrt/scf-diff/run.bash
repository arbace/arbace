#!/bin/bash
# JCS353 (a) (doc/classes/JAVA-DEFERRED-TRIAGE.md, "As built: JCS350, JCS351, JCS353"): jrt's StringConcatFactory
# (overlay/jdk/java.base/java/lang/invoke/StringConcatFactory.java) compiled on the JDK as package scf, its adapters
# by Shim (jdk26u's handles in place of MethodHandleImpl's, parameterSlotCount computed), and Diff's cases (results,
# their types, the linkage checks' exceptions and messages) run against the JDK's StringConcatFactory; --control:
# stringOf's "null" changed, which must differ. The oracle tier's, by hand (bin/lib/java26.bash). Scratch in
# .tmp/scf-diff. Exit 1 when a case differs (or, with --control, when none does).
set -euo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
. "$root/bin/lib/java26.bash"; arbace_use_java26 || exit 2
d=$root/.tmp/scf-diff; rm -rf "$d"; mkdir -p "$d/src/scf" "$d/out"
sed -e 's/^package java.lang.invoke;/package scf; import java.lang.invoke.*;/' -e 's/MethodHandleImpl\.adapter(/Shim.adapter(/g' \
    -e 's/a -> /(Shim.Body) a -> /g' -e 's/concatType\.parameterSlotCount()/Shim.slots(concatType)/g' \
    "$root/overlay/jdk/java.base/java/lang/invoke/StringConcatFactory.java" > "$d/src/scf/StringConcatFactory.java"
[ "${1:-}" = --control ] && sed -i 's/? "null" : s;/? "nul" : s;/' "$d/src/scf/StringConcatFactory.java"
cp "$root/test/jrt/scf-diff/Diff.java" "$root/test/jrt/scf-diff/Shim.java" "$d/src/scf/"
(cd "$d/src" && javac -nowarn -d "$d/out" scf/*.java)
st=0; java -cp "$d/out" scf.Diff || st=$?
if [ "${1:-}" = --control ]; then [ "$st" != 0 ] && echo "scf-diff --control: differs, as it must" && exit 0; echo "scf-diff --control: no case differs"; exit 1; fi
exit $st
