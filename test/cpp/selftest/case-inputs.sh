# needs-host
# queue 324 (CP356 (a)): bin/cppi check --record writes each program's IR source (test/cpp/eval-inputs-linux-amd64.edn), read by
# crops-score's :case-inputs (CP333 (b)'s programs' half). A fabricated run of the samples corpus in a copy of test/cpp: a program
# whose route file says ours with ir.status 0 is :own; one our route refused and clang made is the pack's (its refusal's class named);
# one ours refused under CPPI_ROUTE=ours is none; one with no route file none. The control: the same run with every route file
# `clang` counts no program own. Also runtime-inputs: every unit the pack's while the route is clang.
. "$(dirname "$0")/lib.bash"
mkdir -p "$t/tree/test" && cp -r "$root/test/cpp" "$t/tree/test/" && rm -f "$t/tree/test/cpp/eval-inputs-linux-amd64.edn"
mkrun() { # mkrun WORK ROUTE-OF-auto.cpp
  local w=$1 i
  mkdir -p "$w/samples"
  for i in auto bitfields classes coroutine; do printf 'samples/%s.cpp\tc++23\t\tx\n' "$i"; mkdir -p "$w/files/samples/$i.cpp"; done > "$w/samples/list.tsv"
  echo "$2" > "$w/files/samples/auto.cpp/route"; echo 0 > "$w/files/samples/auto.cpp/ir.status"
  echo clang > "$w/files/samples/bitfields.cpp/route"; echo 0 > "$w/files/samples/bitfields.cpp/ir.status"
  printf 'expr\ta message\n' > "$w/files/samples/bitfields.cpp/gen.refused"
  echo ours > "$w/files/samples/classes.cpp/route"; echo 98 > "$w/files/samples/classes.cpp/ir.status"
  printf 'coroutine\tCoyieldExpr\n' > "$w/files/samples/classes.cpp/gen.refused"
  : > "$w/samples/nodes.tsv"; : > "$w/samples/run.tsv"
}
inputs=$t/tree/test/cpp/eval-inputs-linux-amd64.edn
mkrun "$t/w1" ours
(cd "$t/tree" && arbace_tool -m arbace.cpp.check run-judge "$t/w1" eval --record samples) > "$t/w1.log" 2>&1
grep -q '^\["samples/auto.cpp" {:ir :own}\]$' "$inputs" && ok "a program our route made: :own" || bad "ours: $(head -12 "$inputs" 2>&1 | tail -6)"
grep -q '^\["samples/bitfields.cpp" {:ir "pack: cpp-ir-samples (.*); our route refused it (expr)"}\]$' "$inputs" && ok "refused, clang's: the pack, the class named" || bad "pack: $(grep bitfields "$inputs")"
grep -q '^\["samples/classes.cpp" {:ir "none: our route refused it (coroutine)"}\]$' "$inputs" && ok "refused under ours: none" || bad "none: $(grep classes.cpp "$inputs")"
grep -q '^\["samples/coroutine.cpp" {:ir "none: no IR made (not built)"}\]$' "$inputs" && ok "no route: none" || bad "unbuilt: $(grep coroutine "$inputs")"
grep -q '^\["runtime/' "$inputs" && bad "the samples' run wrote another corpus's rows" || ok "only the run's corpus written"
# the control: auto.cpp's route clang: no program own
mkrun "$t/w2" clang
(cd "$t/tree" && arbace_tool -m arbace.cpp.check run-judge "$t/w2" eval --record samples) > "$t/w2.log" 2>&1
if grep -q '{:ir :own}' "$inputs"; then bad "control: a run with no route ours counted one own"; else ok "control: no route ours, none own (as a control must)"; fi
# runtime-inputs while the route is clang: 198 units, each the pack's
(cd "$t/tree" && arbace_tool -m arbace.cpp.check runtime-inputs - clang) > "$t/rt.edn" 2>&1
n=$(grep -c '^\["' "$t/rt.edn"); p=$(grep -c '{:ir "pack: cpp-runtime-ir' "$t/rt.edn")
[ "$n" = "$(grep -c . "$root/test/cpp/runtime/ir.sha256")" ] && [ "$n" = "$p" ] && ok "runtime-inputs - clang: $n units, every one the pack's" || bad "runtime-inputs: $n rows, $p pack"
(cd "$t/tree" && arbace_tool -m arbace.cpp.check runtime-inputs - auto) > "$t/rt2.log" 2>&1; st=$?
[ $st = 2 ] && ok "runtime-inputs - auto refused (a build is needed)" || bad "runtime-inputs - auto: exit $st"
finish
