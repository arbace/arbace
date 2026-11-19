#!/bin/bash
# GD4 (doc/GATE-DIET-SURVEY.md, GDS17 (a), GDS18 (a), "As built: GD4"): the caches' table test/gate/caches.edn and the prune over it
# (bin/lib/gate-run.bash's caches_rows, caches_prune, caches_unlisted, worktrees_list), on a scratch copy: a scratch checkout, main
# checkout, HOME and Go cache under $TMPDIR/caches-test, never the real ones (GATE_CACHES_ROOT, _MAIN, _HOME, _GOCACHE).
#   1. every row of the table reads (caches_rows: one a line), with the Clojure reader too (stage 0), each with :path, :owner and a
#      known :rule;
#   2. every .tmp/NAME a bin/ script names is a row's (its first part matched by a row's glob): a new scratch area joins the table;
#   3. each rule on the scratch tree: [:size G :lru] removes the least recently used first, [:age D] the old files, [:key-gen N]
#      the older generations (a day old), [:age-self D] an old test directory, :keep and :list nothing; --dry-run removes nothing;
#      --cheap only the cheap rows; :free-gib, :min-gib and :idle-only bound a size cap; :max-entries warns; an unlisted entry is listed, never removed;
#   4. worktrees_list names a merged worktree idle for a day, not an unmerged one, and removes nothing.
# Usage: test/gate/caches-test.bash [--coverage]   (in gate-tests: 1, 3 and 4; --coverage: 2 alone, the essential step caches-table,
#        whose inputs are bin/ and the table: gate-tests' key does not hold bin/ whole)
# needs: exe
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}; mkdir -p "$TMPDIR"
T=$TMPDIR/caches-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
tab=$here/test/gate/caches.edn
lib() { ( root=$here logs=$T/logs; mkdir -p "$logs"; . "$here/bin/lib/gate-cache.bash"; . "$here/bin/lib/gate-run.bash"; "$@" ); }

# 1. the table reads
lib caches_rows "$tab" > "$T/rows.tsv"
n=$(wc -l < "$T/rows.tsv"); m=$(grep -c '^ *{:path ' "$tab")
[ "$n" = "$m" ] && [ "$n" -gt 50 ] && ok "caches_rows reads every row ($n)" || bad "caches_rows read $n rows of $m"
bad_rules=$(awk -F'\t' '$3 !~ /^(size|age|age-self|key-gen|count|builtin|keep|list)$/ {print $1 " " $3}' "$T/rows.tsv")
[ -z "$bad_rules" ] && ok "every rule is a known one" || bad "unknown rules: $bad_rules"
awk -F'\t' '$3 ~ /^(size|age|age-self|key-gen|count)$/ && $4 !~ /^[0-9]+$/ {print}' "$T/rows.tsv" > "$T/noarg"
[ -s "$T/noarg" ] && bad "a rule without its number: $(head -2 "$T/noarg")" || ok "every counted rule has its number"
root=$here; . "$here/bin/lib/host.bash"
exe=${ARBACE_HOST_EXE:-$(arbace_host_exe)}
if "$exe" -e "(let [v (read (java.io.PushbackReader. (java.io.FileReader. \"$tab\")))] (doseq [e v] (assert (string? (:path e)) (pr-str e)) (assert (string? (:owner e)) (pr-str e)) (assert (or (keyword? (:rule e)) (vector? (:rule e))) (pr-str e)) (assert (contains? #{:checkout :main :home nil} (:where e)) (pr-str e))) (println (count v)))" > "$T/read.out" 2>&1 \
   && [ "$(tail -1 "$T/read.out")" = "$n" ]; then ok "the Clojure reader reads the table: $n entries, each with :path, :owner, :rule"
else bad "the Clojure reader: $(tail -3 "$T/read.out" | tr '\n' ' ')"; fi

# 2. every .tmp name of bin/ is a row's
if [ "${1:-}" = --coverage ]; then
grep -rhoE '\.tmp/[A-Za-z0-9_][A-Za-z0-9_.-]*' "$here/bin" | sed 's/[.]$//; s|^\.tmp/||' | sort -u > "$T/names"
awk -F'\t' '$1 ~ /^\.tmp\// {p = $1; sub(/^\.tmp\//, "", p); sub(/\/.*/, "", p); print p}' "$T/rows.tsv" | sort -u > "$T/globs"
: > "$T/missing"
while IFS= read -r k; do
  awk -v k="$k" 'BEGIN { st = 1 } { g = $0; gsub(/\./, "\\.", g); gsub(/\*/, ".*", g); if (k ~ ("^" g "$")) st = 0 } END { exit st }' "$T/globs" || echo "$k" >> "$T/missing"
done < "$T/names"
[ ! -s "$T/missing" ] && ok "every .tmp name bin/ writes ($(wc -l < "$T/names")) is a row's" || bad "names no row covers: $(tr '\n' ' ' < "$T/missing")"
[ $fail = 0 ] && rm -rf "$T"
exit $fail
fi

# 3. the rules on a scratch tree
R=$T/root M=$T/main H=$T/home G=$T/gocache
mkdir -p "$R/.tmp/tmp" "$R/.tmp/goi-table" "$R/.tmp/old-test" "$R/.tmp/new-test" "$R/.tmp/scratch-unlisted" "$G" "$M/.tmp/java-check/forms/x" "$M/.tmp/worktrees" "$H"
now=$(date +%s)
# Go's cache: 12 files of 1 MiB, ages 1..12 hours; a cap of 8 MiB (a toy table) removes the 4 oldest
for i in $(seq 1 12); do yes | head -c 1048576 > "$G/f$i"; touch -d "@$(( now - i * 3600 ))" "$G/f$i"; done
touch -d "@$(( now - 5 * 86400 ))" "$R/.tmp/tmp/old"; touch "$R/.tmp/tmp/new"
for k in aaaaaaaaaaaaaaaa bbbbbbbbbbbbbbbb cccccccccccccccc; do mkdir -p "$R/.tmp/goi-table/$k"; done
touch -d "@$(( now - 3 * 86400 ))" "$R/.tmp/goi-table/aaaaaaaaaaaaaaaa"; touch -d "@$(( now - 2 * 86400 ))" "$R/.tmp/goi-table/bbbbbbbbbbbbbbbb"
mkdir -p "$R/.tmp/goi-table/not-a-key"; touch -d "@$(( now - 9 * 86400 ))" "$R/.tmp/goi-table/not-a-key"
touch -d "@$(( now - 3 * 86400 ))" "$R/.tmp/old-test"
for i in $(seq 1 12); do : > "$M/.tmp/java-check/forms/x/e$i"; done
cat > "$T/toy.edn" <<'EOF'
[
 {:path "{gocache}" :where :home :owner "go" :rule [:size 0.0078125 :lru] :cheap true}
 {:path ".tmp/tmp" :owner "tmpdir" :rule [:age 2] :cheap true}
 {:path ".tmp/goi-table" :owner "goi" :rule [:key-gen 2]}
 {:path ".tmp/*-test" :owner "tests" :rule [:age-self 2]}
 {:path ".tmp/java-check" :where :main :owner "java-check" :rule :keep :max-entries 10}
 {:path ".tmp/worktrees" :where :main :owner "main session" :rule :list}
]
EOF
env3=(GATE_CACHES="$T/toy.edn" GATE_CACHES_ROOT="$R" GATE_CACHES_MAIN="$M" GATE_CACHES_HOME="$H" GATE_CACHES_GOCACHE="$G")
snap() { (cd "$T" && find root main gocache | LC_ALL=C sort); }
snap > "$T/before"
env "${env3[@]}" bash -c 'root=$1 logs=$2; mkdir -p "$logs"; . "$1/bin/lib/gate-cache.bash"; . "$1/bin/lib/gate-run.bash"; caches_prune --dry-run all' x "$here" "$T/logs" > "$T/dry.out" 2>&1
snap > "$T/after-dry"
cmp -s "$T/before" "$T/after-dry" && grep -q 'would remove 4 entries of' "$T/dry.out" && ok "--dry-run lists (4 of Go's cache among them) and removes nothing" \
  || bad "--dry-run: $(diff "$T/before" "$T/after-dry" | head -3 | tr '\n' ' ') $(head -3 "$T/dry.out" | tr '\n' ' ')"
env "${env3[@]}" bash -c 'root=$1 logs=$2; . "$1/bin/lib/gate-cache.bash"; . "$1/bin/lib/gate-run.bash"; caches_prune "" cheap' x "$here" "$T/logs" > "$T/cheap.out" 2>&1
[ ! -e "$G/f12" ] && [ ! -e "$G/f9" ] && [ -e "$G/f8" ] && [ -e "$G/f1" ] && ok "[:size :lru]: the 4 least recently used removed, 8 MiB kept" || bad "the size cap: $(ls "$G" | tr '\n' ' ')"
[ ! -e "$R/.tmp/tmp/old" ] && [ -e "$R/.tmp/tmp/new" ] && ok "[:age 2]: TMPDIR's 5-day-old file removed, a new one kept" || bad "the age rule: $(ls "$R/.tmp/tmp" | tr '\n' ' ')"
[ -d "$R/.tmp/goi-table/aaaaaaaaaaaaaaaa" ] && [ -d "$R/.tmp/old-test" ] && ok "--cheap: the rows that are not cheap left alone" || bad "--cheap touched a row that is not cheap"
env "${env3[@]}" bash -c 'root=$1 logs=$2; . "$1/bin/lib/gate-cache.bash"; . "$1/bin/lib/gate-run.bash"; caches_prune "" all' x "$here" "$T/logs" > "$T/all.out" 2>&1
[ ! -e "$R/.tmp/goi-table/aaaaaaaaaaaaaaaa" ] && [ -d "$R/.tmp/goi-table/bbbbbbbbbbbbbbbb" ] && [ -d "$R/.tmp/goi-table/cccccccccccccccc" ] && [ -d "$R/.tmp/goi-table/not-a-key" ] \
  && ok "[:key-gen 2]: the oldest key removed, the newest 2 and a name that is no key kept" || bad "key-gen: $(ls "$R/.tmp/goi-table" | tr '\n' ' ')"
[ ! -e "$R/.tmp/old-test" ] && [ -d "$R/.tmp/new-test" ] && ok "[:age-self 2]: an old test directory removed, a new one kept" || bad "age-self: $(ls "$R/.tmp" | tr '\n' ' ')"
grep -q 'WARNING: .*forms/x holds 12 entries, more than its :max-entries 10' "$T/all.out" && [ -e "$M/.tmp/java-check/forms/x/e1" ] \
  && ok ":max-entries: a directory over it warned of, nothing removed (:keep)" || bad ":max-entries: $(grep -i warn "$T/all.out")"
grep -q 'the table does not name.*scratch-unlisted' "$T/all.out" && [ -d "$R/.tmp/scratch-unlisted" ] && ok "an unlisted entry listed and kept" || bad "unlisted: $(grep 'does not name' "$T/all.out")"
# [:size G :lru] with :free-gib, :min-gib and :idle-only (n-gate-gocache): 12 files of 1 MiB, 4 MiB free (GATE_CACHES_FREE_KIB);
# :free-gib 8 MiB makes the cap 12 + 4 - 8 = 8 MiB (the 4 oldest out), :min-gib 10 MiB holds it at 10 (2 out), a Go build
# running (GATE_CACHES_GO_BUSY=1) none
fcap() {  # fcap MIN BUSY: a fresh G2, the toy row, the cheap prune; prints the files left
  local G2=$T/gocache2 i; rm -rf "$G2"; mkdir -p "$G2"
  for i in $(seq 1 12); do yes | head -c 1048576 > "$G2/f$i"; touch -d "@$(( now - i * 3600 ))" "$G2/f$i"; done
  printf '[\n {:path "{gocache}" :where :home :owner "go" :rule [:size 1 :lru] :free-gib 0.0078125 :min-gib %s :idle-only true :cheap true}\n]\n' "$1" > "$T/toy-free.edn"
  GATE_CACHES="$T/toy-free.edn" GATE_CACHES_GOCACHE="$G2" GATE_CACHES_FREE_KIB=4096 GATE_CACHES_GO_BUSY=$2 \
    bash -c 'root=$1 logs=$2; . "$1/bin/lib/gate-cache.bash"; . "$1/bin/lib/gate-run.bash"; caches_prune "" cheap' x "$here" "$T/logs" > "$T/free.out" 2>&1
  ls "$G2" | wc -l
}
[ "$(fcap 0 0)" = 8 ] && grep -q 'its cap 0.0 GiB (at most 1 GiB' "$T/free.out" && ok ":free-gib: the cap the size plus the free disk less F, the 4 oldest removed" || bad ":free-gib: $(tr '\n' ' ' < "$T/free.out")"
[ "$(fcap 0.009765625 0)" = 10 ] && ok ":min-gib: the cap held at its floor, 2 removed" || bad ":min-gib: $(tr '\n' ' ' < "$T/free.out")"
[ "$(fcap 0 1)" = 12 ] && grep -q 'left alone: a Go build runs' "$T/free.out" && ok ":idle-only: nothing removed while a Go build runs" || bad ":idle-only: $(tr '\n' ' ' < "$T/free.out")"
GATE_CACHES=none bash -c 'root=$1 logs=$2; . "$1/bin/lib/gate-cache.bash"; . "$1/bin/lib/gate-run.bash"; caches_prune "" all' x "$here" "$T/logs" > "$T/none.out" 2>&1
[ ! -s "$T/none.out" ] && ok "GATE_CACHES=none: no table, nothing done" || bad "GATE_CACHES=none printed $(head -2 "$T/none.out")"

# 4. the worktrees: a scratch main checkout with two worktrees, one merged and idle, one not merged
W=$M
( cd "$W" && git init -q -b main . && git -c user.email=t@t -c user.name=t commit -q --allow-empty -m base \
  && git branch merged && git branch -q open && git checkout -q open && git -c user.email=t@t -c user.name=t commit -q --allow-empty -m new && git checkout -q main \
  && git worktree add -q .tmp/worktrees/merged merged && git worktree add -q .tmp/worktrees/open open ) > "$T/git.out" 2>&1 || bad "the scratch repository: $(tail -2 "$T/git.out")"
touch -d "@$(( now - 2 * 86400 ))" "$(git -C "$W/.tmp/worktrees/merged" rev-parse --git-path index)" "$(git -C "$W/.tmp/worktrees/open" rev-parse --git-path index)"
GATE_CACHES_MAIN=$W GATE_PRUNE_MERGED=main bash -c 'root=$1 logs=$2; . "$1/bin/lib/gate-cache.bash"; . "$1/bin/lib/gate-run.bash"; worktrees_list' x "$here" "$T/logs" > "$T/wt.out" 2>&1
grep -q 'worktrees/merged (merged, merged into main, idle' "$T/wt.out" && ! grep -q 'worktrees/open ' "$T/wt.out" && [ -d "$W/.tmp/worktrees/merged" ] \
  && ok "--worktrees: the merged, idle worktree listed, the open one not, nothing removed" || bad "--worktrees: $(tr '\n' ' ' < "$T/wt.out")"
[ $fail = 0 ] && rm -rf "$T"
exit $fail
