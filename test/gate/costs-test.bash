#!/bin/bash
# bin/gate --costs (GT8.1, queue 170; test/gate/costs.ae) on a made-up table and store: the medians and maxima of the
# last N passes, a step the store does not record measured by its NAME.mem, an unmeasured one, a :clean-only part in
# the clean tier alone, the essential tier's measured wall from the runs that ran every essential check (concurrent
# passes; a run with a gap or a missing check is none), the sums per crop and tier against the budgets, the TSV, a
# table whose :crops misses a step refused; and steps.edn's own :crops naming every step once.
# needs: exe (an Arbace executable: bin/gate-tests passes it as ARBACE_HOST_EXE)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "costs-test: no executable $exe" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/costs-test.XXXXXX")
trap 'rm -rf "$w"' EXIT
fail=0
ok() { echo "ok   $1"; }
bad() { echo "FAIL $1"; fail=1; }
has() { if grep -qE -- "$2" "$1"; then ok "$3"; else bad "$3 (no line /$2/ in $1)"; sed 's/^/     /' "$1"; fi; }
hasnt() { if grep -qE -- "$2" "$1"; then bad "$3 (a line /$2/ in $1)"; else ok "$3"; fi; }

cat > "$w/steps.edn" <<'F'
{:groups {}
 :steps
 [{:name pre1 :phase :pre :kind :always :cmd "true"}
  {:name build1 :phase :build :kind :build :cmd "true"}
  {:name a :phase :essential :cmd "true"}
  {:name b :phase :essential :cmd "true"}
  {:name never :phase :essential :cmd "true"}
  {:name f :phase :full :cmd "true" :parts [{:part x} {:part y :clean-only true}]}]
 :crops {:shared [pre1 build1] :clojure [a never] :go [b] :c [f]}
 :budgets {:crops 2 :essential-s 600 :nightly-s 3600 :clean-s 7200}}
F
pass() {  # pass STEP KEY COMMIT DATE SECS CPU RSS
  mkdir -p "$w/store/$1"
  printf '{:step "%s" :key "%s" :commit "%s" :dirty false :worktree "/w" :date "%s"\n :secs %s :cpu-s %s :maxrss-kib %s :tree-peak-kib nil\n :log-sha256 "%064d"}\n' \
    "$1" "$2" "$3" "$4" "$5" "$6" "$7" 0 > "$w/store/$1/$2.edn"
}
# run c1: a 10:00:00-10:01:40, b 10:00:50-10:03:20 (concurrent), wall 200 s
pass a k1 c1 2026-11-01T10:01:40Z 100 150.0 1048576
pass b k1 c1 2026-11-01T10:03:20Z 150 300.0 2097152
# run c2: a 11:00:00-11:01:00, b 11:00:30-11:01:40, wall 100 s
pass a k2 c2 2026-11-01T11:01:00Z 60 90.0 3145728
pass b k2 c2 2026-11-01T11:01:40Z 70 100.0 1048576
# c3: a alone (b skipped): no run of every essential check
pass a k3 c3 2026-11-01T12:00:20Z 20 30.0 1048576
# c4: a and b 10 minutes apart: no run either
pass a k4 c4 2026-11-01T13:00:10Z 10 10.0 1048576
pass b k4 c4 2026-11-01T13:10:10Z 10 10.0 1048576
# a record kept under another key's name is no pass
pass a k5 c5 2026-11-01T14:00:00Z 9999 9999 9999
mv "$w/store/a/k5.edn" "$w/store/a/other.edn"
pass f.x k1 c1 2026-11-01T10:30:00Z 1000 4000.0 1048576
mkdir -p "$w/logs"
echo '{:step "build1" :wall-s 30 :cpu-s 45.0 :maxrss-kib 1048576 :tree-peak-kib 0 :wait-s 0 :inner-wait-s 0}' > "$w/logs/build1.mem"
echo '{:step "outside" :wall-s 2 :cpu-s 1.0 :maxrss-kib 0 :tree-peak-kib 0 :wait-s 0 :inner-wait-s 0}' > "$w/logs/outside.mem"
echo '{:step "a" :wall-s 7777 :cpu-s 1.0 :maxrss-kib 0 :tree-peak-kib 0 :wait-s 0 :inner-wait-s 0}' > "$w/logs/a.mem"

costs() { ARBACE_PATH=$src/test GATE_STEPS=$w/steps.edn "$exe" -m gate.costs costs "$src" "$w/store" "$w/logs" "$@"; }

costs essential 5 text > "$w/ess.txt" 2>&1; st=$?
[ $st = 0 ] && ok "essential: exit 0" || { bad "essential: exit $st"; cat "$w/ess.txt"; }
# a: the passes k4, k3, k2, k1 (k5 under another name not one): walls 10 20 60 100, median 40, max 100; peak max 3 GiB
has "$w/ess.txt" '^   a +essential clojure +4 store +0m40s +1m40s +.* 3\.0G$' "a: median and max of its 4 passes, its .mem not read"
has "$w/ess.txt" '^   build1 +essential shared +1 mem +0m30s +0m30s +0m45s' "build1: no pass, its NAME.mem"
has "$w/ess.txt" '^   never +essential clojure +0 - +unmeasured' "never: unmeasured"
has "$w/ess.txt" '^   outside +essential shared +1 mem +0m02s' "outside: a .mem the table does not hold, crop shared"
hasnt "$w/ess.txt" '^   f\.x' "essential: no full step"
hasnt "$w/ess.txt" '^   pre1 .*store' "pre1: never in the store"
# the essential tier: runs c1 (200 s) and c2 (100 s), median 150 s = 2m30s; the serial sum 40 + 70 + 30 + 2 = 2m22s
has "$w/ess.txt" '^   essential: 2m30s measured \(the median wall of the last 2 runs .* the latest at c2: 1m40s; the serial sum 2m22s\).*budget 10m00s \(25%\) ok$' \
  "essential: measured from the complete runs c2 and c1"
costs essential 1 text > "$w/ess1.txt" 2>&1
has "$w/ess1.txt" '^   essential: 1m40s measured \(the median wall of the last 1 runs' "--last 1: the latest run alone"
has "$w/ess1.txt" '^   a +essential clojure +1 store +0m10s +0m10s' "--last 1: a's latest pass"
# per crop: clojure essential = a's 40 s, its share (600 - shared 32 s) / 2 = 284 s = 4m44s
has "$w/ess.txt" '^   clojure essential +0m40s .*1 unmeasured; share 4m44s \(14%\)$' "clojure's essential sum and share"
has "$w/ess.txt" '^   shared essential +0m32s ' "shared's essential sum (build1 30, outside 2)"

costs full 5 text > "$w/full.txt" 2>&1
has "$w/full.txt" '^   f\.x +full +c +1 store +16m40s' "full: f.x"
has "$w/full.txt" '^   f\.y \(clean\) +full +c +0 - +unmeasured' "full: f.y, clean-only, unmeasured"
# nightly: 142 + 1000 = 1142 s = 19m02s (f.y not in it); clean: every part, the same sum, 1 more unmeasured
has "$w/full.txt" '^   nightly \(essential \+ full, serial\): 19m02s the serial sum.*budget 1h00m00s \(32%\)' "nightly: the serial sum of both tiers"
has "$w/full.txt" '^   clean \(every part.*: 19m02s the serial sum.*3 unmeasured; budget 2h00m00s \(16%\) ok so far \(a lower bound' "clean: every part, a lower bound"
# the full tier's share: (3600 - 142 - 0 shared) / 2 = 1729 s
has "$w/full.txt" '^   c full +16m40s .*share 28m49s \(58%\)$' "c's full share: the nightly budget less the essential sum"

costs full 5 tsv > "$w/full.tsv" 2>&1
has "$w/full.tsv" $'^step\ta\tessential\tclojure\t4\tstore\t40\\.0\t100\t' "tsv: a's row"
has "$w/full.tsv" $'^tier-measured\tessential\t-\t-\t2\tstore\t150\\.0\t200\t' "tsv: the measured essential wall"
has "$w/full.tsv" $'^tier\tnightly \\(essential \\+ full, serial\\)\t-\t-\t[0-9]+\t[0-9]+ unmeasured\t1142(\\.0)?\t' "tsv: nightly"

# a table whose :crops misses a step and names no step: refused (exit 1), the problems said
sed 's/:c \[f\]/:c [g]/' "$w/steps.edn" > "$w/steps2.edn"
ARBACE_PATH=$src/test GATE_STEPS=$w/steps2.edn "$exe" -m gate.costs costs "$src" "$w/store" "$w/logs" essential 5 text > "$w/bad.txt" 2>&1; st=$?
[ $st = 1 ] && ok "a step in no crop: exit 1" || bad "a step in no crop: exit $st"
has "$w/bad.txt" ':crops: f is in no crop' "the problem: f in no crop"
has "$w/bad.txt" ':crops :c: g is no step' "the problem: g no step"

# the checkout's own table: every step in exactly one crop, the budgets there
cat > "$w/table.ae" <<F
(require '[gate.costs :as c] '[arbace.java.io :as io])
(def t (with-open [r (java.io.PushbackReader. (io/reader "$src/test/gate/steps.edn"))] (read r)))
(doseq [p (c/problems t)] (println "problem:" p))
(println "budgets" (:budgets t))
(System/exit (if (and (empty? (c/problems t)) (every? number? (vals (:budgets t)))) 0 1))
F
ARBACE_PATH=$src/test "$exe" "$w/table.ae" > "$w/table.txt" 2>&1 && ok "steps.edn: every step in one crop, the budgets numbers" \
  || { bad "steps.edn's :crops or :budgets"; cat "$w/table.txt"; }

# bin/gate's verb: --costs on the made-up store (the checkout's .tmp/gate as its logs)
ARBACE_HOST_EXE=$exe ARBACE_GATE_STORE=$w/store GATE_STEPS=$w/steps.edn "$src/bin/gate" --costs --last 2 > "$w/gate.txt" 2>&1; st=$?
[ $st = 0 ] && ok "bin/gate --costs: exit 0" || { bad "bin/gate --costs: exit $st"; cat "$w/gate.txt"; }
has "$w/gate.txt" '^   a +essential clojure +2 store +0m15s +0m20s' "bin/gate --costs --last 2: a's two latest passes"
"$src/bin/gate" --last 2 > "$w/usage.txt" 2>&1; st=$?
[ $st = 2 ] && ok "--last without --costs: usage" || bad "--last without --costs: exit $st"

[ $fail = 0 ] && echo "costs-test: passed" || echo "costs-test: failed"
exit $fail
