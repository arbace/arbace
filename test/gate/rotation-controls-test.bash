#!/bin/bash
# Review 181 L4 (GT8.4, GTS81 (a)): every rotated step's :always holds the subjects of its family's negative controls,
# so that the subset and every slice judge them (a control whose subject is not in a run is "left", not failed: a
# :subset or a slice without it would leave the control unexercised, quietly). Read from test/gate/steps.edn and the
# families' control files: C's :run controls (test/c/controls.edn) for c-check and c-aot, goi's interp and aot ones
# (test/goi/controls.txt) for goi-check-full and goi-aot, Java's by level (test/java/controls.edn's :levels) for its
# rotated level steps. A subject outside the step's record (another corpus's) is not required. Seconds.
# Usage: test/gate/rotation-controls-test.bash [STEPS]
# needs: exe (the check on stage 0)
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
steps=${1:-$here/test/gate/steps.edn}
root=$here; . "$here/bin/lib/tools.bash"
cd "$here" || exit 2
arbace_tool -e "
(let [steps (:steps (read-string (slurp \"$steps\")))
      c-run (for [c (read-string (slurp \"test/c/controls.edn\")) :when (= :run (:level c))] (:subject c))
      goi (for [l (arbace.string/split-lines (slurp \"test/goi/controls.txt\"))
                :let [[lv _ s] (arbace.string/split (arbace.string/trim l) #\"\\s+\")]
                :when (and (not (arbace.string/starts-with? l \"#\")) s)] [lv s])
      java (read-string (slurp \"test/java/controls.edn\"))
      jlevel {\"java-interp\" :interp \"java-classes\" :classes \"java-diagnostics\" :diagnostics
              \"java-compiled-full\" :compiled \"java-print\" :print}
      need (fn [nm]
             (cond (#{\"c-check\" \"c-aot\"} nm) c-run
                   (= nm \"goi-check-full\") (for [[lv s] goi :when (= lv \"interp\")] s)
                   (= nm \"goi-aot\") (for [[lv s] goi :when (= lv \"aot\")] s)
                   (jlevel nm) (for [c java :when (some #{(jlevel nm)} (:levels c))] (:subject c))
                   :else nil))
      in-record? (fn [r s] (arbace.string/includes? (slurp (:universe r)) (str \"\\\"\" s \"\\\"\")))
      bad (for [s steps :let [r (:rotate s) nm (str (:name s))] :when r
                x (need nm) :when (and (in-record? r x) (not (some #{x} (map str (:always r)))))]
            (str nm \": the control subject \" x \" is not in :always\"))
      rotated (count (filter :rotate steps))]
  (doseq [b bad] (println \"FAIL\" b))
  (println (str (if (seq bad) \"FAIL\" \"ok  \") \" rotation-controls: \" rotated \" rotated steps, \" (count bad) \" control subjects missing from :always\"))
  (System/exit (if (seq bad) 1 0)))"
