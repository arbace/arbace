# bin/oi's run corpus and its selection, sourced by bin/oi (outside it, as ocamldoc.bash and tools.bash, so their edits leave
# native_key alone; in oi_key with the rest of bin/lib/oi/).
#
# (2026-11-24, ocaml-aot) Two bin/oi processes of one checkout (a check and a suite check at once: the suite's corpus reads the run
# corpus, so both write it) appended the same rows to one .tmp/oi/corpus.tsv, which then listed programs twice; two jobs of one run
# then ran the same program in one run directory (RUN_DIR/ID) and removed each other's output (parallel/fib_threads's empty output,
# ocaml-eval's journal entry of 2026-11-24). Now each writer builds its corpus in a file of its own and renames it into place, and the
# selection takes a program once (its first row).

cmd_corpus() {
  # the headers alone: the tarball, no ocamlc and no ocdump (the oracle's paths call need_tree themselves)
  mkdir -p "$OI_DIR"; need_source
  local out=$OI_DIR/corpus.tsv r b h
  local tmp=$out.$BASHPID
  # header.awk's level: 0 oi's, 1 the light AOT level, 2 the compiled level (OC215 (a): its main runs a test's C stubs)
  local aotv=${AOT:-0}; [ "$aotv" = 1 ] && [ "${LEVEL:-light}" = compiled ] && aotv=2
  : > "$tmp"
  # the tests with a .reference, and (OC203 (a)) those whose check is their check-program-output hook (ocamltest's check of the
  # output then the hook's, not the reference's: lib-threads/signal)
  # and (OC197 (a)) the tests whose main file is a .mll (a TEST header in it), with or without a .reference (none: an empty output)
  for r in $({ cd "$TESTS" && find . \( -name '*.reference' -o -name '*.check-program-output' \) | sed 's/\.[a-z-]*$//'; grep -l '^(\* TEST' $(find . -name '*.mll') | sed 's/\.mll$//'; } | LC_ALL=C sort -u); do
    b=${r#./}
    [ -f "$TESTS/$b.ml" ] || [ -f "$TESTS/$b.mll" ] || continue
    # (the AOT levels read a test with C stubs as its class: the C API is oi's alone, the light and compiled levels' runtime has none)
    h=$(awk -v aot="$aotv" -f "$root/bin/lib/oi/header.awk" "$(test_src "$b")")
    # a program header.awk classes that the suite's reading runs (modules, readonly data files) is the suite corpus's alone (OT9's third
    # fix): one row a program across the two corpora, the REAP cell counting it once
    case $h in class*) case $(awk -f "$root/bin/lib/oi/actions.awk" "$(test_src "$b")") in run*) continue ;; esac ;; esac
    printf '%s\t%s\n' "$b" "$h" >> "$tmp"
  done
  mv -f "$tmp" "$out"
  echo "oi: corpus: $(wc -l < "$out") programs with a .reference, a check hook or a main .mll, $(cut -f2 "$out" | grep -c '^run$' || true) run, $(cut -f2 "$out" | grep -c '^class$' || true) classed by their header ($out)"
}

# select_from CORPUS: the rows of CORPUS (ID<TAB>...) the selection takes: --only-list's ids, --only's regexp, else all, each program
# once (its first row: a corpus written twice over must not run a program in two jobs at once)
# (the header's ${ocamlsrcdir}, @OCAMLSRCDIR@ in the corpus files, is the pinned tree's path in the rows the runs read: OT21 (b))
select_from() {
  if [ -n "${ONLY_LIST:-}" ]; then awk -F'\t' 'FNR == NR { sub(/[ \t]*#.*/, ""); if ($0 != "") want[$0] = 1; next } $1 in want' "$ONLY_LIST" "$1"
  elif [ -n "$ONLY" ]; then grep -E "^[^	]*($ONLY)" "$1" || true; else cat "$1"; fi | awk -F'\t' '!seen[$1]++' | sed "s|@OCAMLSRCDIR@|$TOP|g"
}
selected() { select_from "$OI_DIR/corpus.tsv"; }
