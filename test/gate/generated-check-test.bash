#!/bin/bash
# bin/generated-check on a fabricated checkout and a fake executable (audit 041, findings 2 and 6):
# each misbehaviour of a generator, or a stale committed file, must fail the check; a faithful
# one passes. Seconds, no executable of the build: bin/gate-independent (run by hand:
#   test/gate/generated-check-test.bash [SCRATCH])
set -uo pipefail
src=$(cd "$(dirname "$0")/../.." && pwd)
scratch=${1:-$src/.tmp/generated-check-test}
rm -rf "$scratch"; mkdir -p "$scratch"
fails=0
export TMPDIR=$scratch/tmp; mkdir -p "$TMPDIR"

# the fake executable: MODE says how it misbehaves; ARBACE_PATH is recorded
cat > "$scratch/fake" <<'F'
#!/bin/bash
echo "${ARBACE_PATH-unset}" >> "$FAKE_LOG"
M=${MODE:-}
case $1 in
  test/c2j/gen_nodes.ae) [ "$M" = c2j-empty ] || echo N ;;
  test/c2j/gen_fns.ae) echo F ;;
  test/goi/gen_nodes.ae)
    [ "$M" = goi-empty ] && exit 0
    echo "ops$([ "$M" = goi-stale ] && echo X)" > "$2/ops.ae"; echo registry > "$2/registry.ae" ;;
  -m) out=$4; [ "$M" = nothing ] && exit 0
      mkdir -p "$out/go/arbace/repr/go/godump" "$out/go/arbace/repr/go/internal/syntax"
      for f in godump.ae godump/dump.ae internal/syntax.ae internal/syntax/a.ae; do echo "$f" > "$out/go/arbace/repr/go/$f"; done
      [ "$M" = outside-go ] && { mkdir -p "$out/arbace"; echo x > "$out/arbace/new.ae"; }
      exit 0 ;;
esac
F
chmod +x "$scratch/fake"

# mk DIR: a fabricated checkout whose committed files are what the fake writes
mk() {
  local d=$1
  mkdir -p "$d/bin/lib" "$d/arbace/lang/go" "$d/go/arbace/goi" "$d/go/arbace/jrt" "$d/test/goi" "$d/test/c2j" \
    "$d/go/arbace/repr/go/godump" "$d/go/arbace/repr/go/internal/syntax"
  cp "$src/bin/generated-check" "$d/bin/"; cp "$src/bin/lib/tmpdir.bash" "$d/bin/lib/"
  echo N > "$d/arbace/lang/go/CompilerNodes.ae"; echo F > "$d/arbace/lang/go/CompilerAOTFn.ae"
  echo ops > "$d/go/arbace/goi/ops.ae"; echo registry > "$d/go/arbace/goi/registry.ae"
  local r=$d/go/arbace/repr/go
  for f in godump.ae godump/dump.ae internal/syntax.ae internal/syntax/a.ae; do echo "$f" > "$r/$f"; done
  printf '#!/bin/bash\n[ "${JRT_STALE:-}" = 1 ] && exit 1\nexit 0\n' > "$d/bin/jrt"; chmod +x "$d/bin/jrt"
}

# expect NAME WANT_STATUS [VAR=VALUE...]: generated-check on a fresh tree with the fake
expect() {
  local name=$1 want=$2 d=$scratch/$1; shift 2
  mk "$d"
  env FAKE_LOG="$scratch/$name.log" "$@" "$d/bin/generated-check" "$scratch/fake" > "$scratch/$name.out" 2>&1
  local got=$?
  if [ "$got" = "$want" ]; then echo "ok:   $name (status $got)"
  else echo "FAIL: $name: status $got, wanted $want"; sed 's/^/        /' "$scratch/$name.out"; fails=$((fails + 1)); fi
}

expect faithful 0
expect goi-stale 1 MODE=goi-stale          # finding 2: goi's pair compared
expect goi-nothing 1 MODE=goi-empty        # finding 2 and 6: a generator that writes nothing
expect modules-stale 1 JRT_STALE=1         # finding 2: bin/jrt modules --check run
expect outside-go 1 MODE=outside-go        # finding 6: the whole tree of repr/go compared
expect nothing 1 MODE=nothing              # a generator that writes nothing

# finding 6: the c2j generators run with ARBACE_PATH=$root, whatever the caller's
expect arbace-path 0 ARBACE_PATH=/elsewhere
if grep -qx '/elsewhere' "$scratch/arbace-path.log"; then
  echo "FAIL: arbace-path: a generator ran with the caller's ARBACE_PATH"; fails=$((fails + 1))
else echo "ok:   arbace-path (generators ran with ARBACE_PATH=checkout)"; fi

# finding 6: --update with a generator that writes nothing keeps the committed trees, and fails
d=$scratch/update-nothing; mk "$d"
MODE=nothing FAKE_LOG=$scratch/un.log "$d/bin/generated-check" --update "$scratch/fake" > "$scratch/un.out" 2>&1
st=$?
if [ "$st" != 0 ] && [ -f "$d/go/arbace/repr/go/godump.ae" ] && [ -f "$d/go/arbace/repr/go/internal/syntax/a.ae" ]; then
  echo "ok:   update-nothing (status $st, trees kept)"
else echo "FAIL: update-nothing: status $st, trees $(ls "$d/go/arbace/repr/go" 2>&1 | tr '\n' ' ')"; fails=$((fails + 1)); fi

# report 046, finding 2: --update refuses a file outside the four roots, copying nothing
d=$scratch/update-outside; mk "$d"; echo old > "$d/go/arbace/repr/go/godump.ae"
MODE=outside-go FAKE_LOG=$scratch/uo.log "$d/bin/generated-check" --update "$scratch/fake" > "$scratch/uo.out" 2>&1
st=$?
if [ "$st" != 0 ] && [ ! -e "$d/arbace/new.ae" ] && [ "$(cat "$d/go/arbace/repr/go/godump.ae")" = old ]; then echo "ok:   update-outside (status $st, nothing copied)"
else echo "FAIL: update-outside: status $st, $(cat "$scratch/uo.out")"; fails=$((fails + 1)); fi
# and a c2j generator that prints nothing is not copied over the committed file
d=$scratch/update-c2j-empty; mk "$d"
MODE=c2j-empty FAKE_LOG=$scratch/ce.log "$d/bin/generated-check" --update "$scratch/fake" > "$scratch/ce.out" 2>&1
if [ -s "$d/arbace/lang/go/CompilerNodes.ae" ]; then echo "ok:   update-c2j-empty (committed file kept)"
else echo "FAIL: update-c2j-empty: CompilerNodes.ae emptied"; fails=$((fails + 1)); fi
# --update writes a stale goi file
d=$scratch/update-goi; mk "$d"; echo old > "$d/go/arbace/goi/ops.ae"
FAKE_LOG=$scratch/ug.log "$d/bin/generated-check" --update "$scratch/fake" > "$scratch/ug.out" 2>&1
if [ "$(cat "$d/go/arbace/goi/ops.ae")" = ops ]; then echo "ok:   update-goi"
else echo "FAIL: update-goi: ops.ae not rewritten"; fails=$((fails + 1)); fi

[ "$fails" = 0 ] && echo "generated-check-test: passed" || echo "generated-check-test: $fails failed"
exit $((fails > 0))
