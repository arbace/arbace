#!/bin/bash
# bin/generated-check on a fabricated checkout and a fake executable (audit 041, finding 2):
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
  test/c2j/gen_nodes.ae) echo N ;;
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
expect goi-nothing 1 MODE=goi-empty        # finding 2: a generator that writes nothing
expect modules-stale 1 JRT_STALE=1         # finding 2: bin/jrt modules --check run
expect nothing 1 MODE=nothing              # a generator that writes nothing

# --update writes a stale goi file
d=$scratch/update-goi; mk "$d"; echo old > "$d/go/arbace/goi/ops.ae"
FAKE_LOG=$scratch/ug.log "$d/bin/generated-check" --update "$scratch/fake" > "$scratch/ug.out" 2>&1
if [ "$(cat "$d/go/arbace/goi/ops.ae")" = ops ]; then echo "ok:   update-goi"
else echo "FAIL: update-goi: ops.ae not rewritten"; fails=$((fails + 1)); fi

[ "$fails" = 0 ] && echo "generated-check-test: passed" || echo "generated-check-test: $fails failed"
exit $((fails > 0))
