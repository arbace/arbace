#!/bin/bash
# Re-review 030's finding 3 (doc/ocaml/OCAML-SURVEY.md, "As built: re-review 030's fixes"): the key of oi's rows (bin/oi key, bin/oi suite key)
# covers the limits that change the rows (OI_TIMEOUT, OI_GOMEMLIMIT, OI_GOMAXPROCS) and the corpus (the program corpus's key is not the suite's),
# and is stable otherwise. Needs the tree and stage 0 (ARBACE_HOST_EXE). Run: bash test/ocaml/oi_key_test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
bad=0
# re-review 049's finding 3: the cases write into .tmp/w, which a fresh checkout lacks: a failing redirect made the "fails the key" cases pass
# without bin/oi having run. The directory is made first, each step is checked, and a "fails" case must show bin/oi's own message.
mkdir -p "$root/.tmp/w" || { echo "FAIL cannot make .tmp/w"; exit 1; }
k0=$(bin/oi key) || { echo "FAIL bin/oi key"; exit 1; }
chk() { # NAME WANT(same|differ) KEY [BASE]
  local r=differ; [ "$3" = "${4:-$k0}" ] && r=same
  if [ "$r" = "$2" ]; then echo "ok   $1"; else echo "FAIL $1: $r, want $2"; bad=1; fi
}
chk "the key is stable" same "$(bin/oi key)"
chk "OI_TIMEOUT is in the key" differ "$(OI_TIMEOUT=30 bin/oi key)"
chk "OI_GOMEMLIMIT is in the key" differ "$(OI_GOMEMLIMIT=300MiB bin/oi key)"
chk "OI_GOMAXPROCS is in the key" differ "$(OI_GOMAXPROCS=2 bin/oi key)"
chk "the suite corpus has a key of its own" differ "$(bin/oi suite key)"

# re-review 042's finding 4: the opam pins are in the key; a part of the key that cannot be had fails the key, it does not shorten it
sed 's/0\.17\.0/0.17.1/' "$root/.tmp/repr-ocaml/opam-pins.txt" > "$root/.tmp/w/opam-pins.changed" || { echo "FAIL cannot make the changed pins"; exit 1; }
cmp -s "$root/.tmp/repr-ocaml/opam-pins.txt" "$root/.tmp/w/opam-pins.changed" && { echo "FAIL the changed pins equal the pins (the edit did not apply)"; exit 1; }
chk "the program corpus's key does not hold the opam pins (a pin bump must not rerun its rows)" same "$(OI_OPAM_PINS=$root/.tmp/w/opam-pins.changed bin/oi key)"
chk "the opam pins are in the suite's key" differ "$(OI_OPAM_PINS=$root/.tmp/w/opam-pins.changed bin/oi suite key)" "$(bin/oi suite key)"
# (ocdump's key is the oracle path's part: OI_CHECK_DEFAULT=ocdump; the native path's key has none, TY24 (a))
for v in OI_OPAM_PINS OI_OCDUMP_KEY; do
  dflt=native; [ "$v" = OI_OCDUMP_KEY ] && dflt=ocdump
  OI_CHECK_DEFAULT=$dflt OI_TIMEOUT=30 env "$v=$root/.tmp/w/missing" bin/oi suite key > "$root/.tmp/w/key.out" 2> "$root/.tmp/w/key.err"; st=$?
  if [ "$st" != 0 ] && [ ! -s "$root/.tmp/w/key.out" ] && grep -q '^oi: key: ' "$root/.tmp/w/key.err"; then echo "ok   a missing part ($v) fails the key"; else echo "FAIL a missing part ($v): exit $st, key $(cat "$root/.tmp/w/key.out")"; bad=1; fi
done

# re-review 042's finding 5: a checkout without .tmp/oi and .tmp/o7 (a clean run): the verbs find their directories (a copy of bin/ in a scratch root)
R=$root/.tmp/w/oi-key-root; rm -rf "$R"; mkdir -p "$R"; cp -r "$root/bin" "$R/bin"
(cd "$R" && bin/oi oi-limits > "$R.log" 2>&1)
[ -d "$R/.tmp/oi" ] && [ -d "$R/.tmp/o7" ] && [ -d "$R/.tmp/o6" ] && echo "ok   bin/oi makes .tmp/oi, .tmp/o7 and .tmp/o6 (re-review 049)" || { echo "FAIL bin/oi left .tmp/oi, .tmp/o7 or .tmp/o6 unmade"; bad=1; }
exit $bad
