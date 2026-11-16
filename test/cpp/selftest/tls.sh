# needs-host
# cpp-threads (2026-11-15; doc/cpp/CPP-SURVEY.md, "As built: cpp-threads"): bin/cppi builds a C++ node program with the checkout's
# ci when it differs from the tools' host's embedded copy (bin/lib/cppi/ci-fresh.bash's cppi_ci, bin/lib/ci/fresh.bash's c_ci_fresh),
# so a _Thread_local of the lowered runtime is a thread-local static of the node program (`(static N SIZE ALIGN 2 NAME)`, ci's
# threads) and each thread has its own pending flag and in-flight exception. tls.cpp (our route's IR, no clang) built as bin/cppi
# check builds it: cx$pending, cx$cur and the program's thread_local are thread-local statics, and the program passes (four threads
# throwing and catching at once, 1500 each). Controls: the judge of the statics rejects the same node text with cx$pending's RO 0
# (always), and while the host's embedded ci is older than the checkout's, the build under CI_FRESH=0 (the host's ci on purpose,
# the old bin/cppi's) has cx$pending not thread-local.
. "$(dirname "$0")/lib.bash"
. "$root/bin/lib/cppi/ci-fresh.bash"
rt=$("$root/bin/cppi" runtime-ci 2> "$t/rt.log") || die "no lowered runtime for ci: $(tail -2 "$t/rt.log")"
. "$root/bin/lib/repr-cpp/sysroot.bash"; cpp_sysroot > "$t/sysroot.log" 2>&1 || die "no sysroot: $(tail -2 "$t/sysroot.log")"
mkdir -p "$t/g"
printf '%s\tc++23\t%s\t\n' "$root/test/cpp/selftest/tls.cpp" "$t/g" > "$t/todo.tsv"
(cd "$root" && arbace_tool -m arbace.cpp.codegen gen-batch "$t/todo.tsv") > "$t/gen.log" 2>&1
[ -f "$t/g/gen.ll" ] && [ ! -f "$t/g/gen.refused" ] || die "our route did not generate tls.cpp: $(cat "$t/g/gen.refused" 2>&1 | head -2)"
build() { # OUT [ENV...]: the node program as bin/cppi check builds it
  local o=$1; shift
  (cd "$root" && env "$@" root="$root" GOMEMLIMIT=${CPPI_CI_MEMLIMIT:-6GiB} bash -c '. bin/lib/tools.bash; . bin/lib/cppi/ci-fresh.bash; cppi_ci arbace_tool -m arbace.cpp.run nodes "$0" "" "$1" "$2"' "$rt" "$o" "$t/g/gen.ll") > "$o.log" 2>&1
}
tl_statics() { # NODES: 0 when cx$pending, cx$cur and the program's thread_local are thread-local statics
  grep -Eq '^\(static [0-9]+ 4 4 2 "cx\$pending"\)' "$1" && grep -Eq '^\(static [0-9]+ 8 8 2 "\[[0-9]+ \\"cx\$cur\\"\]"\)' "$1" &&
    grep -Eq '^\(static [0-9]+ 4 4 2 "\[[0-9]+ \\"_ZL4mine\\"\]"\)' "$1"
}
build "$t/good.nodes"
[ -s "$t/good.nodes" ] || die "the build failed: $(tail -3 "$t/good.nodes.log")"
tl_statics "$t/good.nodes" && ok "cx\$pending, cx\$cur and mine are thread-local statics" \
  || bad "not thread-local: $(grep -E 'cx\$pending"|cx\$cur|_ZL4mine' "$t/good.nodes" | head -3 | tr '\n' ' ')"
cirun=${CIRUN:-$root/.tmp/ci/cirun}
[ -x "$cirun" ] || "$root/bin/ci" cirun-build --ensure > "$t/cirun.log" 2>&1 || die "no cirun: $(tail -2 "$t/cirun.log")"
: > "$t/empty"
for i in 1 2 3; do
  "$cirun" "$t/good.nodes" < "$t/empty" > "$t/run$i.out" 2> "$t/run$i.err"; st=$?
  [ $st = 0 ] && grep -qx '1500 1500 1500 1500' "$t/run$i.out" && ok "run $i passes: $(cat "$t/run$i.out")" \
    || bad "run $i: status $st, $(cat "$t/run$i.out") $(head -c 300 "$t/run$i.err")"
done
# the judge's control: the same node text with the pending flag one object for every thread
sed -E 's/^\(static ([0-9]+) 4 4 2 "cx\$pending"\)/(static \1 4 4 0 "cx$pending")/' "$t/good.nodes" > "$t/bad.nodes"
tl_statics "$t/bad.nodes" && bad "control: the judge accepts cx\$pending not thread-local" || ok "control: the judge rejects cx\$pending not thread-local"
# the build's control, while the host's ci is older than the checkout's (until the next re-seed)
if (c_ci_fresh 2> "$t/fresh.err"; [ -n "${ARBACE_NO_AOT:-}" ]); then
  build "$t/old.nodes" CI_FRESH=0
  [ -s "$t/old.nodes" ] || die "the control's build failed: $(tail -3 "$t/old.nodes.log")"
  tl_statics "$t/old.nodes" && bad "control: the host's own ci (CI_FRESH=0) made them thread-local: the fix is not what makes them so" \
    || ok "control: under CI_FRESH=0 (the host's embedded ci) cx\$pending is not thread-local"
else
  ok "the host's ci is the checkout's: no build control (after a re-seed)"
fi
finish
