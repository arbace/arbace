#!/bin/bash
# bin/ci musl's forms cache after night 10 (doc/C23-SURVEY.md, "musl's index and its lock"): a partial or missing index.edn is
# not fresh and `ensure`/`read` repair it (rereading nothing), and two readers serialize on the lock.
# Needs a complete forms cache to copy (CI_MUSL_TEST_SEED, default the checkout's, else the main checkout's): the test works on a
# copy in .tmp/musl-lock-test through CI_MUSL_FORMS, writing its key as the checkout's (the forms are what a complete read made;
# the index is what is under test).
# By hand or `make musl-lock-test`: wired to no gate step.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
cmain=${root%%/.tmp/gate-clean/*}; cmain=${cmain%%/.tmp/worktrees/*}
seed=${CI_MUSL_TEST_SEED:-}
if [ -z "$seed" ]; then for c in "$root/.tmp/ci-musl/forms" "$cmain/.tmp/ci-musl/forms"; do [ -f "$c/index.edn" ] && seed=$c && break; done; fi
[ -n "$seed" ] || { echo "musl-lock-test: no complete forms cache to copy (CI_MUSL_TEST_SEED)" >&2; exit 1; }
w=$root/.tmp/musl-lock-test; rm -rf "$w"; mkdir -p "$w"
export CI_MUSL_FORMS=$w/forms
# (GD4: the main checkout's shared copies by key are section 4's, in a store of its own; sections 1-3 read here alone)
export CI_MUSL_NO_SHARED=1 CI_MUSL_SHARED=$w/shared
cp -R "$seed" "$CI_MUSL_FORMS"
# through bin/ci: it evaluates the checkout's arbace/c when the host embeds another (RT.toolSource, CS281 (a))
musl() { "$root/bin/ci" musl "$@"; }
musl prepare
musl key > "$CI_MUSL_FORMS/key"
units() { grep -o ' {:defs ' "$CI_MUSL_FORMS/index.edn" | wc -l; }
cached=$(find "$CI_MUSL_FORMS/src" -name '*.c.ae' | wc -l)
fail() { echo "musl-lock-test: FAIL: $*" >&2; exit 1; }

# 1. a partial index (a quarter of the units, night 10's shape) under a fresh key is not fresh; ensure repairs it
musl ensure > "$w/out.log" 2>&1
musl fresh || fail "a complete cache is not fresh"
[ "$(units)" -eq "$cached" ] || fail "index lists $(units) units, cache holds $cached"
printf '{"src/stdio/printf.c" {:defs {} :refs #{}}}' > "$CI_MUSL_FORMS/index.edn"
if musl fresh; then fail "a partial index is fresh"; fi
musl ensure > "$w/out.log" 2>&1
musl fresh || fail "ensure left the partial index"
[ "$(units)" -eq "$cached" ] || fail "repaired index lists $(units) units, cache holds $cached"
grep -q '"__libc_start_main"' "$CI_MUSL_FORMS/index.edn" || fail "repaired index has no __libc_start_main"
echo "musl-lock-test: partial index repaired ($cached units)"

# 2. a missing index
rm "$CI_MUSL_FORMS/index.edn"
if musl fresh; then fail "a cache without an index is fresh"; fi
musl read > "$w/out.log" 2>&1
musl fresh || fail "read left no index"
echo "musl-lock-test: missing index repaired"

# 3. two readers serialize: the first holds the lock for 6 s, the second starts after 2 s and must finish after the first
rm "$CI_MUSL_FORMS/index.edn"
( CI_MUSL_HOLD=6 musl read > "$w/out.log" 2>&1; date +%s > "$w/first.end" ) &
p1=$!
sleep 2
( s=$(date +%s); musl read > "$w/out.log" 2>&1; e=$(date +%s); echo "$s $e" > "$w/second" ) &
p2=$!
sleep 1
[ ! -f "$w/second" ] || fail "the second reader finished while the first held the lock"
wait $p1 $p2
read s e < "$w/second"
[ "$e" -ge "$(cat "$w/first.end")" ] || fail "the second reader ended before the first"
[ $((e - s)) -ge 3 ] || fail "the second reader did not wait ($((e - s)) s)"
musl fresh || fail "not fresh after two readers"
echo "musl-lock-test: two readers serialized (second waited $((e - s)) s)"

# 4. GD4 (doc/GATE-DIET-SURVEY.md, "As built: GD4"): a shared copy at the key is taken whole, nothing read; a read publishes one
unset CI_MUSL_NO_SHARED
k=$(musl key); mkdir -p "$CI_MUSL_SHARED"; cp -R "$CI_MUSL_FORMS" "$CI_MUSL_SHARED/$k"
rm -rf "$CI_MUSL_FORMS"
musl read > "$w/out.log" 2>&1
grep -q 'taken from' "$w/out.log" || fail "the shared copy at the key was not taken ($(tail -2 "$w/out.log" | tr '\n' ' '))"
musl fresh || fail "not fresh after taking the shared copy"
rm -rf "$CI_MUSL_SHARED"; rm "$CI_MUSL_FORMS/index.edn"
musl read > "$w/out.log" 2>&1
[ -f "$CI_MUSL_SHARED/$k/index.edn" ] && [ "$(cat "$CI_MUSL_SHARED/$k/key")" = "$k" ] || fail "a read published no shared copy"
echo "musl-lock-test: a shared copy taken, a read's published"

# 5. review 300's finding 2: a holder's shared lock (bin/lib/crops/musl-hold.bash's ci_musl_hold, every reader of the cache) keeps a
# reader's reread out until it ends: the holder holds 6 s, the index is removed under it (stale), the reader starts after 2 s and ends
# after the holder
. "$root/bin/lib/crops/musl-hold.bash"
rm -f "$w/held" "$w/holder.end" "$w/reader"
( ci_musl_hold > "$w/hold.log" 2>&1 || exit 1; date +%s > "$w/held"; sleep 6; date +%s > "$w/holder.end" ) &
p1=$!
for _ in 1 2 3 4 5 6 7 8 9 10; do [ -f "$w/held" ] && break; sleep 1; done
[ -f "$w/held" ] || fail "ci_musl_hold took no lock ($(cat "$w/hold.log"))"
rm "$CI_MUSL_FORMS/index.edn"
( s=$(date +%s); musl read > "$w/out.log" 2>&1; e=$(date +%s); echo "$s $e" > "$w/reader" ) &
p2=$!
sleep 2
[ ! -f "$w/reader" ] || fail "a reader reread the cache while a holder held it"
wait $p1 $p2
read s e < "$w/reader"
[ "$e" -ge "$(cat "$w/holder.end")" ] || fail "the reader ended before the holder"
musl fresh || fail "not fresh after the holder and the reader"
echo "musl-lock-test: a reader waited for the shared holder ($((e - s)) s)"

# 6. a holder's child that runs `bin/ci musl read` (or ensure) does not wait on its own parent's lock: a fresh cache is read as it
# is, a stale one fails at once
( ci_musl_hold > "$w/hold.log" 2>&1 || exit 1
  s=$(date +%s); musl read > "$w/child1.log" 2>&1 || exit 2
  rm "$CI_MUSL_FORMS/index.edn"; st=0; musl read > "$w/child2.log" 2>&1 || st=$?
  e=$(date +%s); [ "$st" = 1 ] || exit 3; [ $((e - s)) -lt 20 ] || exit 4 ) &
p3=$!
c=0; while [ -d "/proc/$p3" ] && [ $c -lt 60 ]; do sleep 1; c=$((c + 1)); done
[ -d "/proc/$p3" ] && { kill "$p3"; fail "a holder's child's read waited on its parent's lock (deadlock)"; }
st=0; wait $p3 || st=$?
[ "$st" = 0 ] || fail "a holder's child: status $st (2: a fresh read failed, 3: a stale read did not fail, 4: it waited)"
grep -q 'held' "$w/child1.log" || fail "a holder's child read the fresh cache again ($(cat "$w/child1.log"))"
musl ensure > "$w/out.log" 2>&1; musl fresh || fail "not fresh after case 5"
echo "musl-lock-test: a holder's child reads nothing and never waits on its parent"
rm -rf "$w"
echo "musl-lock-test: ok"
