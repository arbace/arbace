#!/bin/bash
# bin/ci musl's forms cache after night 10 (doc/C23-SURVEY.md, "musl's index and its lock"): a partial or missing index.edn is
# not fresh and `ensure`/`read` repair it (rereading nothing), and two readers serialize on the lock.
# Needs a complete forms cache to copy (CI_MUSL_TEST_SEED, default the checkout's, else the main checkout's): the test works on a
# copy in .tmp/musl-lock-test through CI_MUSL_FORMS, writing its key as the checkout's (the forms are what a complete read made;
# the index is what is under test).
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
cmain=${root%%/.tmp/gate-clean/*}; cmain=${cmain%%/.tmp/worktrees/*}
seed=${CI_MUSL_TEST_SEED:-}
if [ -z "$seed" ]; then for c in "$root/.tmp/ci-musl/forms" "$cmain/.tmp/ci-musl/forms"; do [ -f "$c/index.edn" ] && seed=$c && break; done; fi
[ -n "$seed" ] || { echo "musl-lock-test: no complete forms cache to copy (CI_MUSL_TEST_SEED)" >&2; exit 1; }
w=$root/.tmp/musl-lock-test; rm -rf "$w"; mkdir -p "$w"
export CI_MUSL_FORMS=$w/forms
cp -R "$seed" "$CI_MUSL_FORMS"
# through bin/ci: it evaluates the checkout's arbace/c when the host embeds another (bin/lib/ci/fresh.bash)
musl() { "$root/bin/ci" musl "$@"; }
musl prepare
musl key > "$CI_MUSL_FORMS/key"
units() { grep -o ' {:defs ' "$CI_MUSL_FORMS/index.edn" | wc -l; }
cached=$(find "$CI_MUSL_FORMS/src" -name '*.c.ae' | wc -l)
fail() { echo "musl-lock-test: FAIL: $*" >&2; exit 1; }

# 1. a partial index (a quarter of the units, night 10's shape) under a fresh key is not fresh; ensure repairs it
musl ensure >/dev/null 2>&1
musl fresh || fail "a complete cache is not fresh"
[ "$(units)" -eq "$cached" ] || fail "index lists $(units) units, cache holds $cached"
printf '{"src/stdio/printf.c" {:defs {} :refs #{}}}' > "$CI_MUSL_FORMS/index.edn"
if musl fresh; then fail "a partial index is fresh"; fi
musl ensure >/dev/null 2>&1
musl fresh || fail "ensure left the partial index"
[ "$(units)" -eq "$cached" ] || fail "repaired index lists $(units) units, cache holds $cached"
grep -q '"__libc_start_main"' "$CI_MUSL_FORMS/index.edn" || fail "repaired index has no __libc_start_main"
echo "musl-lock-test: partial index repaired ($cached units)"

# 2. a missing index
rm "$CI_MUSL_FORMS/index.edn"
if musl fresh; then fail "a cache without an index is fresh"; fi
musl read >/dev/null 2>&1
musl fresh || fail "read left no index"
echo "musl-lock-test: missing index repaired"

# 3. two readers serialize: the first holds the lock for 6 s, the second starts after 2 s and must finish after the first
rm "$CI_MUSL_FORMS/index.edn"
( CI_MUSL_HOLD=6 musl read >/dev/null 2>&1; date +%s > "$w/first.end" ) &
p1=$!
sleep 2
( s=$(date +%s); musl read >/dev/null 2>&1; e=$(date +%s); echo "$s $e" > "$w/second" ) &
p2=$!
sleep 1
[ ! -f "$w/second" ] || fail "the second reader finished while the first held the lock"
wait $p1 $p2
read s e < "$w/second"
[ "$e" -ge "$(cat "$w/first.end")" ] || fail "the second reader ended before the first"
[ $((e - s)) -ge 3 ] || fail "the second reader did not wait ($((e - s)) s)"
musl fresh || fail "not fresh after two readers"
echo "musl-lock-test: two readers serialized (second waited $((e - s)) s)"
rm -rf "$w"
echo "musl-lock-test: ok"
