#!/bin/bash
# bin/corpus-dedup on a made-up tree (queue 171): a manifest of five corpora over fabricated files, its tables
# checked: byte copies within and across corpora, a repeat (one file in two units), the normalized level by
# each language's reader (C's lexer, Go's parser, Java's lexer, the Clojure reader), a file the reader refuses
# normalized by white space, and the per-check savings with the first copy in corpora order kept.
# needs: exe (the tools' host: bin/gate-tests passes it as ARBACE_HOST_EXE)
# Usage: test/gate/corpus-dedup-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
mkdir -p "$TMPDIR"
T=$here/.tmp/corpus-dedup-test
rm -rf "$T"; mkdir -p "$T"/{c1,c2,go,java,clj}
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }

printf 'int x;\nint main(void) { return x; }\n' > "$T/c1/a.c"
cp "$T/c1/a.c" "$T/c1/b.c"                                                         # a byte copy of a.c
printf '/* the same tokens */\nint  x;\nint main(void)\n{ return x; }\n' > "$T/c1/c.c"  # a.c normalized
printf 'int y;\n' > "$T/c1/d.c"
cp "$T/c1/a.c" "$T/c2/a.c"                                                         # a copy in another corpus
printf '#define A 1\nint z = A;\n' > "$T/c2/e.c"
printf '#define A 1 int z = A;\n' > "$T/c2/f.c"                                    # not e.c: the directive's end kept
printf 'package p\n\nfunc F() int { return 1 }\n' > "$T/go/x.go"
printf '// a comment\npackage p\n\nfunc F() int {\n\treturn 1\n}\n' > "$T/go/y.go" # x.go normalized
printf 'package p\n\nfunc F( {\n' > "$T/go/bad.go"                                  # go/parser refuses it
printf 'class A { int f() { return 1; } }\n' > "$T/java/A.java"
printf '/** doc */\nclass A {\n  int f() { return 1; } // c\n}\n' > "$T/java/B.java"  # A.java normalized
printf '(ns n)\n(defn f [x] (#(inc %%) x))\n' > "$T/clj/f.clj"
printf ';; c\n(ns n)\n\n(defn f\n  [x]\n  (#(inc %%) x))\n' > "$T/clj/g.clj"           # f.clj normalized (gensyms)

u() { local n=$1; shift; printf '{:name "%s" :files [' "$n"; for f; do printf '["%s" "%s"]' "$(basename "$f")" "$f"; done; printf ']}'; }
cat > "$T/manifest.edn" <<EOF
{:corpora
 [{:crop "c" :id "c1" :lang :c :dir "$T/c1" :checks ["chk-a" "chk-b.c1"]
   :units [$(u a.c "$T/c1/a.c") $(u b.c "$T/c1/b.c") $(u c.c "$T/c1/c.c") $(u d.c "$T/c1/d.c")]}
  {:crop "c" :id "c2" :lang :c :dir "$T/c2" :checks ["chk-b.c2"]
   :units [$(u a.c "$T/c2/a.c") $(u e.c "$T/c2/e.c") $(u f.c "$T/c2/f.c")]}
  {:crop "go" :id "go" :lang :go :dir "$T/go" :checks ["chk-go"]
   :units [$(u x.go "$T/go/x.go") $(u y.go "$T/go/y.go") $(u bad.go "$T/go/bad.go")]}
  {:crop "java" :id "java" :lang :java :dir "$T/java" :checks ["chk-java"]
   :units [$(u t1 "$T/java/A.java") $(u t2 "$T/java/A.java") $(u t3 "$T/java/B.java")]}
  {:crop "clojure" :id "clj" :lang :clojure :dir "$T/clj" :checks ["chk-clj"]
   :units [$(u f.clj "$T/clj/f.clj") $(u g.clj "$T/clj/g.clj")]}]}
EOF

"$here/bin/corpus-dedup" --manifest "$T/manifest.edn" --normalize --groups > "$T/out.txt" 2>&1
rc=$?
[ $rc = 0 ] && ok "exit 0" || bad "exit $rc ($(tail -3 "$T/out.txt"))"
has() { grep -qF -- "$2" "$T/out.txt" && ok "$1" || bad "$1: no line with: $2"; }
# per corpus: | crop | corpus | units | files | repeats | byte dups | byte groups | norm dups | norm groups | normalizer | ws |
has "c1: one byte copy (b.c), two normalized (b.c, c.c)" "| c | c1 | 4 | 4 | 0 | 1 (25.0%) | 1 | 2 (50.0%) | 1 | arbace.repr.c.lex's tokens | 0 |"
has "c2: no copy within, e.c and f.c differ (the directive's end)" "| c | c2 | 3 | 3 | 0 | 0 (0.0%) | 0 | 0 (0.0%) | 0 |"
has "go: y.go is x.go normalized, bad.go by white space" "| go | go | 3 | 3 | 0 | 0 (0.0%) | 0 | 1 (33.3%) | 1 | go/parser's tree"
has "go: the fallback counted" "positions and comments dropped | 1 |"
has "go: the refused file named" "- go/go: 1 normalized by white space (the reader refused them): bad.go"
has "java: t2 a repeat of t1 (the same file), B.java normalized" "| java | java | 3 | 2 | 1 | 0 (0.0%) | 0 | 1 (33.3%) | 1 | arbace.repr.java.lex's tokens"
has "clojure: g.clj is f.clj read" "| clojure | clj | 2 | 2 | 0 | 0 (0.0%) | 0 | 1 (50.0%) | 1 | the Clojure reader's forms | 0 |"
# across corpora: c2's a.c a copy of c1's a.c, chk-b reads both
has "across: c2's a.c a copy in c1, chk-b reads both" "| c | c1 | c2 | 3 | 0 | 1 | 1 | chk-b |"
# per check: chk-b (parts c1, c2) reads both corpora: b.c and c2's a.c are copies of c1's a.c
has "per check: chk-b's two copies" "| chk-b | c1, c2 | 7 | 0 (0.0%) | 2 (28.6%) | 3 (42.9%) |"
has "per check: chk-a reads c1 alone" "| chk-a | c1 | 4 | 0 (0.0%) | 1 (25.0%) | 2 (50.0%) |"
has "per check: chk-java's repeat apart from its copies" "| chk-java | java | 3 | 1 (33.3%) | 0 (0.0%) | 1 (33.3%) |"
# the groups: the first in corpora order kept
has "the kept copy: c1's a.c, the first" "- keep c1:a.c; drop 2: c1:b.c, c2:a.c"
has "normalized group keeps a.c, drops b.c, c.c and c2's a.c" "- keep c1:a.c; drop 3: c1:b.c, c1:c.c, c2:a.c"
has "no group across crops" "## Across crops: 0 byte-identical groups"
[ $fail = 0 ] && echo "corpus-dedup-test: PASS" || { echo "corpus-dedup-test: FAIL (output $T/out.txt)"; exit 1; }
