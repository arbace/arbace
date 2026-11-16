#!/bin/bash
# GTS80 (a) (doc/GATE-SURVEY.md, "As built: GTS72, GTS79-GTS82 (write-ahead)", review 001's findings 3, 5,
# 6): record_pass (bin/lib/gate-run.bash) writes a pass's files through temporary names, checked whole, the
# record last, so that a full disk leaves the store without a cut pass or a record beside another run's log;
# bin/gate --clean's publication (bin/gate's publish_passes) replaces a pass that is empty or does not read
# and publishes none that does not read. On a fabricated store under .tmp/pass-write-test/: the full disk
# is a file size limit (ulimit -f with SIGXFSZ ignored: a real EFBIG from cp and printf), a record whose
# write fails partway (printf overridden: the first 100 bytes, status 1), a rename that fails (mv
# overridden). Usage: test/gate/pass-write-test.bash [REV]: REV's bin/lib/gate-run.bash and bin/gate
# instead of the checkout's (before GTS80: a93329c0^, whose publication, inline in clean_launch, is
# copied below as its publish_passes)
set -uo pipefail
# the gate's own run (bin/gate --clean's audit_step, GATE_CLEAN_INNER, audit_all) is not the fabricated runner's
unset audit_step audit_all GATE_CLEAN_INNER
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=$here/.tmp/tmp; mkdir -p "$TMPDIR"
T=$here/.tmp/pass-write-test
rm -rf "$T"; mkdir -p "$T/src"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
if [ -n "${1:-}" ]; then
  git -C "$here" show "$1:bin/lib/gate-run.bash" > "$T/src/gate-run.bash" || exit 2
  git -C "$here" show "$1:bin/gate" > "$T/src/gate" || exit 2
  echo "testing $1's bin/lib/gate-run.bash and bin/gate"
else
  cp "$here/bin/lib/gate-run.bash" "$T/src/gate-run.bash"; cp "$here/bin/gate" "$T/src/gate"
fi
sed -n '/^publish_passes() {/,/^}/p' "$T/src/gate" > "$T/src/publish.bash"
if [ ! -s "$T/src/publish.bash" ]; then
  # before GTS80: clean_launch's publication (a93329c0^'s bin/gate), FROM's store directories into TO by cp -an
  cat > "$T/src/publish.bash" <<'EOF'
publish_passes() {
  local d n=0 store_to=$2
  for d in "$1"/*/; do
    [ -d "$d" ] || continue
    mkdir -p "$store_to/$(basename "$d")" && cp -an "$d". "$store_to/$(basename "$d")/" && n=$((n + $(ls "$d" | grep -c '\.edn$')))
  done
  echo "   $n passes (:clean) published into $store_to"
}
EOF
fi

hex() { printf '%s' "$1" | sha256sum | cut -c1-64; }
# whole DIR KEY: KEY's record in DIR reads (not empty, closed, its :key KEY) and its :log-sha256 is its log's
whole() {
  local e=$1/$2.edn
  [ -s "$e" ] && [ "$(tail -c 2 "$e")" = "}" ] && grep -qF ":key \"$2\"" "$e" && [ -f "$1/$2.log" ] \
    && [ "$(sed -n 's/.*:log-sha256 "\([0-9a-f]\{64\}\)".*/\1/p' "$e")" = "$(sha256sum < "$1/$2.log" | cut -c1-64)" ]
}
# consistent DIR KEY: KEY has no record, or a whole one (never an empty or cut record, never one beside another log)
consistent() { [ ! -e "$1/$2.edn" ] || whole "$1" "$2"; }
temps() { find "$1" -maxdepth 1 -name '.*' ! -name . -printf '%f ' 2>/dev/null; }

# the gate's state record_pass reads: one step, its log, plan inputs (no file line: nothing to hash again),
# its time and memory
S=$T/run
setup() {  # setup LOGBYTES: a fresh log of that size for step s1
  rm -rf "$S/logs"; mkdir -p "$S/logs/plan" "$S/store"
  head -c "$1" /dev/zero | tr '\0' 'x' > "$S/logs/s1.log"; echo >> "$S/logs/s1.log"
  echo "host gate 0123456789ab" > "$S/logs/plan/s1.inputs"
  echo 12 > "$S/logs/s1.secs"
  echo '{:step "s1" :wall-s 12 :cpu-s 10.5 :maxrss-kib 2048 :tree-peak-kib 4096 :wait-s 0 :inner-wait-s 0}' > "$S/logs/s1.mem"
}
K=$(hex k1)
# rec [LIMIT-KIB] [--printf-fails|--mv-fails]: record_pass s1 in a subshell, under a file size limit when given;
# its output through a pipe (the limit is the subshell's alone); the status in $S/st
rec() {
  local lim=${1:-} how=${2:-}
  (
    root=$S logs=$S/logs store=$S/store commit=0000000 dirty=false
    . "$T/src/gate-run.bash"
    declare -A key outputs; key[s1]=$K; outputs[s1]=no
    case $how in
      --printf-fails) printf() { builtin printf "$@" | head -c 100; return 1; } ;;
      --mv-fails) mv() { case "${@: -1}" in *.edn) return 1 ;; esac; command mv "$@"; } ;;
    esac
    trap '' XFSZ
    [ -z "$lim" ] || ulimit -f "$lim"
    record_pass s1
    echo "status $?" > "$S/st"
  ) 2>&1 | cat > "$S/out"
}
D=$S/store/s1

# 1. a first pass at a key on a full disk (a 20 KB log, an 8 KiB limit): no file of the key, no temporary
setup 20000; rec 8
if [ -z "$(ls -A "$D" 2>/dev/null)" ] && grep -q 'status [1-9]' "$S/st"; then ok "a full disk at a first pass leaves no file of its key and no temporary"
else bad "a full disk at a first pass: $(cat "$S/st"); left: $(ls -A "$D" | tr '\n' ' ')"; fi

# 2. a rerun at a key that has a pass (--rerun, the canary), the disk full at its log: the old pass whole
setup 100; rec; cp "$D/$K.edn" "$S/old.edn"
setup 20000; rec 8
if whole "$D" "$K" && cmp -s "$D/$K.edn" "$S/old.edn" && [ -z "$(temps "$D")" ] && grep -q 'status [1-9]' "$S/st"; then
  ok "a rerun on a full disk leaves the old pass whole (its record with its own log), no temporary"
else bad "a rerun on a full disk: record $(wc -c < "$D/$K.edn" 2>/dev/null) bytes, log $(wc -c < "$D/$K.log" 2>/dev/null) bytes, its digest $(whole "$D" "$K" && echo matches || echo 'does not match'); temporaries: $(temps "$D")"; fi

# 3. a rerun whose record's write fails partway (the disk full at the record): the old pass whole
setup 100; rm -rf "$S/store"; rec; cp "$D/$K.edn" "$S/old.edn"
setup 300; rec "" --printf-fails
if whole "$D" "$K" && cmp -s "$D/$K.edn" "$S/old.edn" && [ -z "$(temps "$D")" ] && grep -q 'status [1-9]' "$S/st"; then
  ok "a record whose write fails: record_pass fails, the old pass whole, no temporary"
else bad "a record whose write fails: $(cat "$S/st" 2>/dev/null); the old record $(cmp -s "$D/$K.edn" "$S/old.edn" && echo kept || echo 'not kept'), $(consistent "$D" "$K" && echo consistent || echo 'beside another log or cut'); temporaries: $(temps "$D")"; fi

# 4. the record's rename failing at a rerun: no record beside another run's log, no temporary
setup 100; rm -rf "$S/store"; rec
setup 300; rec "" --mv-fails
if consistent "$D" "$K" && [ -z "$(temps "$D")" ] && grep -q 'status [1-9]' "$S/st"; then
  ok "the record's rename failing: record_pass fails, no record beside another log, no temporary"
else bad "the record's rename failing: $(cat "$S/st" 2>/dev/null); record $( [ -e "$D/$K.edn" ] && echo present || echo absent), $(consistent "$D" "$K" && echo consistent || echo 'beside another log'); temporaries: $(temps "$D")"; fi

# 5. (a guard, as before GTS80) a pass written whole: the record reads, plain and :clean
setup 100; rm -rf "$S/store"; rec
whole "$D" "$K" && ! grep -q ':clean' "$D/$K.edn" && grep -q '^ :secs 12 :cpu-s 10.5 :maxrss-kib 2048 :tree-peak-kib 4096$' "$D/$K.edn" \
  && ok "a pass written whole reads, its numbers in it" || bad "a pass written whole: $(cat "$D/$K.edn" 2>/dev/null)"
rm -rf "$S/store"; GATE_CLEAN_INNER=1 rec
whole "$D" "$K" && [ "$(tail -1 "$D/$K.edn")" = " :log-sha256 \"$(sha256sum < "$S/logs/s1.log" | cut -c1-64)\" :clean true}" ] \
  && ok "a pass of the clean run reads and ends :clean true}" || bad "a pass of the clean run: $(tail -1 "$D/$K.edn" 2>/dev/null)"

# 6. the clean run's publication over GT7's rerun 4 (an empty pass and log, a cut pass, a good one, an absent
# one) from a clone's store whose five passes are whole but one that does not read
mkpass() {  # mkpass STORE NAME KEY WORKTREE
  mkdir -p "$1/$2"; printf 'log of %s at %s\n' "$2" "$4" > "$1/$2/$3.log"; echo "host gate 0" > "$1/$2/$3.inputs"
  printf '{:step "%s" :key "%s" :commit "0000000" :dirty false :worktree "%s" :date "2026-10-31T01:35:00Z"\n :secs 1 :cpu-s 1 :maxrss-kib 1 :tree-peak-kib 1\n :log-sha256 "%s" :clean true}\n' \
    "$2" "$3" "$4" "$(sha256sum < "$1/$2/$3.log" | cut -c1-64)" > "$1/$2/$3.edn"
}
P=$T/pub; F=$P/from; G=$P/to
for n in empty cut good absent bad; do mkpass "$F" "$n" "$(hex "$n")" clone; done
: > "$F/bad/$(hex bad).edn"
mkdir -p "$G/empty" "$G/cut"; : > "$G/empty/$(hex empty).edn"; : > "$G/empty/$(hex empty).log"
mkpass "$G" cut "$(hex cut)" store; head -c 60 "$G/cut/$(hex cut).edn" > "$P/c"; mv "$P/c" "$G/cut/$(hex cut).edn"
mkpass "$G" good "$(hex good)" store
pubrun() { ( . "$T/src/gate-run.bash"; . "$T/src/publish.bash"; publish_passes "$F" "$G" ) > "$P/out$1" 2>&1; }
pubrun 1
for n in empty cut absent; do
  whole "$G/$n" "$(hex "$n")" && grep -q ':worktree "clone"' "$G/$n/$(hex "$n").edn" \
    && ok "published: the $n pass replaced by the clone's whole one" || bad "published: the $n pass $( [ -s "$G/$n/$(hex "$n").edn" ] && echo 'cut or beside another log' || echo 'empty or absent')"
done
whole "$G/good" "$(hex good)" && grep -q ':worktree "store"' "$G/good/$(hex good).edn" \
  && ok "published: a pass the store holds whole kept" || bad "published: the store's whole pass replaced"
[ ! -e "$G/bad/$(hex bad).edn" ] && ok "published: a clone's pass that does not read not published" \
  || bad "published: a clone's pass that does not read published ($(wc -c < "$G/bad/$(hex bad).edn") bytes)"
[ -z "$(for n in empty cut good absent bad; do temps "$G/$n"; done)" ] && ok "published: no temporary left" || bad "published: temporaries left"
pubrun 2
for n in empty cut good absent; do whole "$G/$n" "$(hex "$n")" || bad "published again: $n no longer whole"; done
grep -q '^   0 passes' "$P/out2" && ok "published again: nothing published ($(sed 's/^ *//' "$P/out2" | tail -1))" \
  || bad "published again: $(tail -1 "$P/out2")"
exit $fail
