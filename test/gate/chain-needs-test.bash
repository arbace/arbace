#!/bin/bash
# Gate 13c (2026-11-14): under bin/gate --full --concurrent the :full chains run at once, and a step's :needs is read,
# never waited on (bin/lib/gate-run.bash's run_one, ok_status): hsi-check-aot (its own chain) found hsi-check, of another
# chain, not yet started and was "not run: hsi-check did not pass" though hsi-check passed nine minutes later. The rule held
# here: a :full step that needs a :full step of another :chain is in a chain whose first step waits for that need
# (:start-after the need, or a step declared after it in the need's chain). plan.ae's lint holds the essential phase's
# rule (one chain); this one is the full phase's, kept out of the planner (in every step's key). The rule on steps.edn,
# then on a made-up table that breaks it (the negative control) and one that keeps it. Seconds, no build, no gate run.
# needs: exe (an Arbace executable: bin/gate-tests passes it as ARBACE_HOST_EXE)
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
exe=${ARBACE_HOST_EXE:-$src/target/seed/amd64/arbace}
[ -x "$exe" ] || { echo "chain-needs-test: no executable $exe" >&2; exit 2; }
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/chain-needs-test.XXXXXX")
trap 'rm -rf "$w"' EXIT
fail=0
ok() { echo "ok   $1"; }
bad() { echo "FAIL $1"; fail=1; }
# check TABLE: one line a :full step whose cross-chain need its chain's first step does not wait for
cat > "$w/check.ae" <<'F'
(let [steps (vec (:steps (read-string (slurp (first *command-line-args*)))))
      full (filterv #(= :full (:phase %)) steps)
      nm #(str (:name %))
      ch #(str (or (:chain %) (:name %)))
      ix (into {} (map-indexed (fn [i s] [(nm s) i]) full))
      by (into {} (map (fn [s] [(nm s) s]) full))
      first-of (reduce (fn [m s] (if (m (ch s)) m (assoc m (ch s) s))) {} full)]
  (doseq [s full, x (:needs s)
          :let [d (by (str x))]
          :when (and d (not= (ch d) (ch s)))
          :let [a (some-> (first-of (ch s)) :start-after str by)]
          :when (not (and a (= (ch a) (ch d)) (>= (ix (nm a)) (ix (nm d)))))]
    (println (str (nm s) " (chain " (ch s) ") needs " (nm d) " (chain " (ch d) "); its chain starts after "
                  (or (some-> (first-of (ch s)) :start-after) "nothing")))))
F
check() { "$exe" "$w/check.ae" "$1"; }
check "$src/test/gate/steps.edn" > "$w/steps.out" 2>&1; st=$?
[ $st = 0 ] && [ ! -s "$w/steps.out" ] && ok "steps.edn: every :full step's cross-chain need is waited for by its chain's first step" \
  || bad "steps.edn (status $st): $(cat "$w/steps.out")"
cat > "$w/bad.edn" <<'F'
{:steps [{:name a :phase :full :cmd "true"}
         {:name b :phase :full :needs [a] :cmd "true"}]}
F
check "$w/bad.edn" > "$w/bad.out" 2>&1
grep -q '^b (chain b) needs a (chain a)' "$w/bad.out" && ok "control: a cross-chain need with no :start-after is flagged" \
  || bad "control not flagged: $(cat "$w/bad.out")"
cat > "$w/good.edn" <<'F'
{:steps [{:name a :phase :full :cmd "true"}
         {:name a2 :phase :full :chain a :needs [a] :cmd "true"}
         {:name c :phase :full :chain j :start-after a2 :cmd "true"}
         {:name b :phase :full :chain j :needs [a] :cmd "true"}]}
F
check "$w/good.edn" > "$w/good.out" 2>&1; st=$?
[ $st = 0 ] && [ ! -s "$w/good.out" ] && ok "a chain started after its need (or a later step of the need's chain) passes" \
  || bad "good table flagged (status $st): $(cat "$w/good.out")"
exit $fail
