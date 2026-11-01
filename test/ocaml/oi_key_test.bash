#!/bin/bash
# Re-review 030's finding 3 (doc/ocaml/OCAML-SURVEY.md, "As built: re-review 030's fixes"): the key of oi's rows (bin/oi key, bin/oi suite key)
# covers the limits that change the rows (OI_TIMEOUT, OI_GOMEMLIMIT, OI_GOMAXPROCS) and the corpus (the program corpus's key is not the suite's),
# and is stable otherwise. Needs the tree and stage 0 (ARBACE_HOST_EXE). Run: bash test/ocaml/oi_key_test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
bad=0
k0=$(bin/oi key) || { echo "FAIL bin/oi key"; exit 1; }
chk() { # NAME WANT(same|differ) KEY
  local r=differ; [ "$3" = "$k0" ] && r=same
  if [ "$r" = "$2" ]; then echo "ok   $1"; else echo "FAIL $1: $r, want $2"; bad=1; fi
}
chk "the key is stable" same "$(bin/oi key)"
chk "OI_TIMEOUT is in the key" differ "$(OI_TIMEOUT=30 bin/oi key)"
chk "OI_GOMEMLIMIT is in the key" differ "$(OI_GOMEMLIMIT=300MiB bin/oi key)"
chk "OI_GOMAXPROCS is in the key" differ "$(OI_GOMAXPROCS=2 bin/oi key)"
chk "the suite corpus has a key of its own" differ "$(bin/oi suite key)"

# re-review 042's finding 5: a checkout without .tmp/oi and .tmp/o7 (a clean run): the verbs find their directories (a copy of bin/ in a scratch root)
R=$root/.tmp/w/oi-key-root; rm -rf "$R"; mkdir -p "$R"; cp -r "$root/bin" "$R/bin"
(cd "$R" && bin/oi oi-limits > "$R.log" 2>&1)
[ -d "$R/.tmp/oi" ] && [ -d "$R/.tmp/o7" ] && echo "ok   bin/oi makes .tmp/oi and .tmp/o7" || { echo "FAIL bin/oi left .tmp/oi or .tmp/o7 unmade"; bad=1; }
exit $bad
