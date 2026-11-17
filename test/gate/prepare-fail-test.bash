#!/bin/bash
# needs: exe
# bin/arbace-go's image prepare and the host's compile (bin/lib/program.bash's host_compile) fail the build when a namespace does
# not load (jcs127's day test, 2026-11-15: a syntax error at arbace/classes/interp.ae:537 left arbace.classes.interp and
# arbace.classes.native out of the image and of the compiled namespaces, "159 sources compiled ... 0 failed", and
# `bin/arbace-go --gate-build` exited 0). Each path run alone (--prepare-check, --host-compile-check) on the fixture's namespaces
# (test/gate/prepare-fixture): prepfix.ok alone passes (the control: the path itself works), prepfix.ok with prepfix.bad (a forward
# reference) fails and names it. On ARBACE_HOST_EXE (stage 0); about a minute.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$root/.tmp/tmp}; mkdir -p "$TMPDIR"
. "$root/bin/lib/host.bash"
exe=${ARBACE_HOST_EXE:-$(arbace_host_exe)}
fx=$root/test/gate/prepare-fixture
W=$root/.tmp/prepare-fail-test; rm -rf "$W"; mkdir -p "$W"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }

# the image prepare (bin/arbace-go's prepare): the executable requires the namespaces from ARBACE_PATH
ARBACE_PATH=$fx "$root/bin/arbace-go" --prepare-check "$exe" "$W/p-ok" prepfix.ok > "$W/p-ok.log" 2>&1; st=$?
[ "$st" = 0 ] && ok "prepare: prepfix.ok alone passes" || { bad "prepare: the control failed (status $st)"; tail -5 "$W/p-ok.log"; }
ARBACE_PATH=$fx "$root/bin/arbace-go" --prepare-check "$exe" "$W/p-bad" prepfix.ok prepfix.bad > "$W/p-bad.log" 2>&1; st=$?
if [ "$st" != 0 ] && grep -q 'image: not prepared: prepfix.bad' "$W/p-bad.log"; then ok "prepare: a forward reference fails it, named"
else bad "prepare: prepfix.bad's forward reference did not fail it (status $st)"; tail -5 "$W/p-bad.log"; fi

# the host's compile (host_compile: the namespaces required, then compiled by c2j)
HOST_COMPILE_PATH=$fx "$root/bin/arbace-go" --host-compile-check "$exe" "$W/h-ok" prepfix.ok > "$W/h-ok.log" 2>&1; st=$?
[ "$st" = 0 ] && ok "host compile: prepfix.ok alone passes" || { bad "host compile: the control failed (status $st)"; tail -5 "$W/h-ok.log"; tail -5 "$W/h-ok/host-compile.log" 2>&1; }
HOST_COMPILE_PATH=$fx "$root/bin/arbace-go" --host-compile-check "$exe" "$W/h-bad" prepfix.ok prepfix.bad > "$W/h-bad.log" 2>&1; st=$?
if [ "$st" != 0 ] && grep -q 'image: not prepared: prepfix.bad' "$W/h-bad/host-compile.log"; then ok "host compile: a forward reference fails it, named"
else bad "host compile: prepfix.bad's forward reference did not fail it (status $st)"; tail -5 "$W/h-bad.log"; fi

[ "$fail" = 0 ] && rm -rf "$W"
exit $fail
