#!/bin/bash
# CLAUDE.md's sentences the review 045 corrected stay corrected (the gate's step claude-md since GD1). Usage: test/docs/claude-md-test.bash
# CLAUDE_MD (queue 421): another file read as CLAUDE.md, the negative control's (below)
here=$(cd "$(dirname "$0")/../.." && pwd)
claude=${CLAUDE_MD:-$here/CLAUDE.md}
fail=0
at() { [ "$1" = CLAUDE.md ] && echo "$claude" || echo "$here/$1"; }
has() { grep -qF -- "$2" "$(at "$1")" && echo "ok   $1 has: $2" || { echo "FAIL $1 lacks: $2"; fail=1; }; }
lacks() { grep -qF -- "$2" "$(at "$1")" && { echo "FAIL $1 still has: $2"; fail=1; } || echo "ok   $1 lacks: $2"; }
# 7. repr/c
has CLAUDE.md '`arbace/repr/c/` (`arbace.repr.c.*`'
lacks CLAUDE.md 'arbace/repr/c{.ae,/}'
lacks CLAUDE.md 'CS1-CS51'
has CLAUDE.md "against gcc"
# 8. toolSource
has CLAUDE.md 'a namespace of a tool or of goi (`arbace/go/`;'
lacks CLAUDE.md '(and every namespace of `arbace/go/`'
# 9. no heavy lock; heavy-lock by absolute path
has CLAUDE.md 'under no heavy lock'
lacks CLAUDE.md 'Go build, under no lock'
has CLAUDE.md 'ARBACE_HEAVY_CMD="$PWD/bin/heavy-lock"'
# 10. the survey names the queue task, not a scratch path; GTS121 says what the refusal covers; GTS126
lacks doc/GATE-SURVEY.md '.tmp/mac-queue/reports/040'
has doc/GATE-SURVEY.md 'GTS126 how a miss fills the directory'
has doc/GATE-SURVEY.md 'replaces only a directory it'
# 048: the miss's text
has CLAUDE.md 'a miss replaces only a directory of its own making'
lacks CLAUDE.md 'a miss never removes the directory of another key'
has doc/GATE-SURVEY.md '**Built: (a) with (b)**'
# queue 118, finding 7: repr/c and arbace/c embedded and in gate_key since C9, ci's Go forms too
lacks CLAUDE.md 'evaluated from the checkout, not embedded, out of `gate_key`; C'"'"'s essential'
lacks CLAUDE.md 'repr/rust, repr/c, `arbace/c`, repr/ocaml'
lacks CLAUDE.md '`go/arbace/{ci,cinterp,crt,oi,scm}`'
has CLAUDE.md 'embedded and compiled with the program since C9'
lacks arbace/j2g/embed.ae "repr/c's, which are not embedded"
grep -q '^;; repr/c' "$here/arbace/j2g/embed.ae" && { echo "FAIL arbace/j2g/embed.ae: a column-0 comment inside the exclusions"; fail=1; } || echo "ok   arbace/j2g/embed.ae: its comments indented"
# 1119: since W2 repr/scheme, repr/cpp, repr/llvm, repr/python and the Go forms oi, scm, scminterp, py, libm are embedded, out of arbace_crop_paths
lacks CLAUDE.md "their namespaces (\`arbace/repr/{ocaml,scheme,cpp,llvm,python}\`"
lacks CLAUDE.md '`go/arbace/{oi,scm,scminterp,py,'
has CLAUDE.md "out of \`arbace_runtime_key\` alone (\`bin/lib/version.bash\`'s"
# queue 421: CLAUDE.md's key lists, step names and counts against the code they name (each FAIL says what the code holds)
flat=$(tr '\n' ' ' < "$claude" | tr -s ' ')
same() {  # same WHAT DOC-LIST CODE-LIST: the two lists (one name a line) equal as sets
  local d c; d=$(printf '%s\n' "$2" | sed '/^$/d' | LC_ALL=C sort -u); c=$(printf '%s\n' "$3" | sed '/^$/d' | LC_ALL=C sort -u)
  if [ -n "$c" ] && [ "$d" = "$c" ]; then echo "ok   CLAUDE.md: $1 as the code's"; else
    echo "FAIL CLAUDE.md: $1: CLAUDE.md's not the code's (CLAUDE.md only: $(comm -23 <(echo "$d") <(echo "$c") | paste -sd' '); the code only: $(comm -13 <(echo "$d") <(echo "$c") | paste -sd' '))"; fail=1; fi; }
v=$here/bin/lib/version.bash steps=$here/test/gate/steps.edn
crop_list=$(sed -n '/^arbace_crop_paths=(/,/^)/p' "$v" | grep -v '^ *#')
# arbace_crop_paths' script libraries and Go forms
same "arbace_crop_paths' script libraries" \
  "$(printf '%s' "$flat" | grep -o "the crops' script libraries \`bin/lib/{[^}]*}\`" | sed 's/.*{//; s/}.*//; s/ //g' | tr ',' '\n')" \
  "$(printf '%s\n' "$crop_list" | grep -o "'bin/lib/[^/']*/\*'" | sed "s|'bin/lib/||; s|/\*'||")"
same "arbace_crop_paths' Go forms" \
  "$(printf '%s' "$flat" | grep -o "the Go forms \`go/arbace/{[^}]*}\`, pyi's lazy modules \`go/arbace/{[^}]*}\`" | grep -o '{[^}]*}' | tr -d '{} ' | tr ',' '\n')" \
  "$(printf '%s\n' "$crop_list" | grep -o 'go/arbace/[a-z0-9]*\.ae' | sed 's|go/arbace/||; s|\.ae||')"
# arbace_runtime_key's exclusions (but j2g's edge, named as such) in its sentence
rk=$(printf '%s' "$flat" | grep -o "\`arbace_runtime_key\`: \`arbace/lang\`[^)]*)")
for x in $(sed -n '/^arbace_runtime_key() {/,/^}/p' "$v" | grep -o "! -path '\?[^ ']*'\?" | sed "s/! -path //; s/'//g; s/\*$//" | grep -v '^jdk/edge.edn$'); do
  case $rk in *"\`$x\`"*) echo "ok   CLAUDE.md: arbace_runtime_key's sentence names $x" ;; *) echo "FAIL CLAUDE.md: arbace_runtime_key leaves out $x (bin/lib/version.bash), its sentence does not say so"; fail=1 ;; esac
done
# the essential gate's core: test/gate/steps.edn's :pre, :build and :essential steps but the crops' (:essential-when-changed)
# and cpp-runtime (C++'s build, run when a C++ check needs it), pyi-aot-runtime likewise (pyi's AOT runtime, when a Python check
# needs it: python.edn spliced, queue 428)
same "the essential gate's core" \
  "$(printf '%s' "$flat" | grep -o 'the essential gate is the core ([^)]*)' | sed 's/.*core (//; s/)$//' | tr -d ' ' | tr ',' '\n')" \
  "$(grep -E '^  \{:name ' "$steps" | grep -E ':phase :(pre|build|essential)\b' | grep -v ':essential-when-changed' | awk '{print $2}' | grep -vxE 'cpp-runtime|pyi-aot-runtime')"
# the crops tier's steps it names are crops' essential steps
for n in $(printf '%s' "$flat" | grep -o 'where their keys agree ([^;]*;' | grep -o '`[a-z0-9-]*`' | tr -d '`'); do
  grep -qE "^  \{:name $n .*:phase :essential .*:essential-when-changed" "$steps" && echo "ok   CLAUDE.md: $n a crop's essential step" \
    || { echo "FAIL CLAUDE.md names $n among the crops tier's steps: not an essential step :essential-when-changed in test/gate/steps.edn"; fail=1; }
done
# every :phase :weekly step named
for n in $(grep -E '^  \{:name [^ ]+ .*:phase :weekly\b' "$steps" | awk '{print $2}'); do
  printf '%s' "$flat" | grep -qw -- "$n" && echo "ok   CLAUDE.md names the weekly step $n" || { echo "FAIL CLAUDE.md does not name the weekly step $n (test/gate/steps.edn)"; fail=1; }
done
# the REAP matrix's rows and the crops, counted
words=(zero one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen twenty)
nl=$(sed -n '/^{:languages \[/,/\]/p' "$here/test/reap/cells.edn" | grep -o ':[a-z0-9-]\+' | grep -vxc ':languages')
has CLAUDE.md "${words[$nl]} rows, $((nl * 4)) cells since"
nc=$(grep -c '{:id :[a-z0-9-]* :name' "$here/test/crops/crops.edn")
has CLAUDE.md "each of the ${words[$nc]} crops'"
# stale sentences corrected (queue 421)
lacks CLAUDE.md "stay out of \`--full\` until their"
lacks CLAUDE.md "\`roundtrip --cmi samples stdlib\`"
lacks CLAUDE.md "\`ocaml-dumps\`, \`oi-check\`"
lacks CLAUDE.md "Chez 10.3.0 run live"
lacks CLAUDE.md "\`--full\` all of \`test/programs/\`"
lacks CLAUDE.md "the live parts wait in"
lacks CLAUDE.md "The fragments of later batches (haskell,"
has CLAUDE.md "\`gate-run.bash\` and \`cover.bash\`"
has CLAUDE.md "\`arbace_embedded_resources\` (si's \`scheme/psyntax/psyntax.pp.ae\`)"

# the negative control (queue 421): each wrong claim below, put into a copy of CLAUDE.md, must fail this test
if [ -z "${CLAUDE_MD_CONTROL:-}" ]; then
  ctl=${TMPDIR:-$here/.tmp/tmp}/claude-md-control.$$
  mkdir -p "${ctl%/*}"
  while IFS= read -r e; do
    [ -n "$e" ] || continue
    sed "$e" "$claude" > "$ctl"
    if cmp -s "$ctl" "$claude"; then echo "FAIL control: $e changed nothing"; fail=1
    elif CLAUDE_MD=$ctl CLAUDE_MD_CONTROL=1 "$0" > "$ctl.log" 2>&1; then echo "FAIL control: $e passed"; fail=1
    else echo "ok   control: $e fails ($(grep -c '^FAIL' "$ctl.log") FAIL)"; fi
  done <<'CONTROLS'
s/,repr-wasm,wai}`)/,wai}`)/
s/a64i,wai}`, pyi's/a64i}`, pyi's/
s/`jdk\/provenance.edn`, `jdk\/platform\/`, /`jdk\/provenance.edn`, /
s/claude-md,$/claude-md, gate-tests,/
s/`wasm-essential`, `llvm-essential`,/`wasm-check-essential`, `llvm-essential`,/
s/ si-ecraven-aot, / /
s/fourteen rows, 56 cells since/thirteen rows, 52 cells since/
s/each of the fourteen crops'/each of the thirteen crops'/
CONTROLS
  rm -f "$ctl" "$ctl.log"
fi
exit $fail
