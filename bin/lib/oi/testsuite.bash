# bin/oi's testsuite verbs (O7, OC100; doc/ocaml/OCAML-SURVEY.md, "As built: O7 (write-ahead)"): every .ml and .mli
# of testsuite/ (tests, lib, tools) a role and an action class, and the rollup of the levels' results.
#
#   bin/oi suite roles      .tmp/o7/roles.tsv: FILE <TAB> ROLE <TAB> RESULT, FILE relative to testsuite/; ROLE test (an
#                           ocamltest header of its own), helper (a header names it, or it sits in a subdirectory a
#                           header names), interface (an .mli beside a test or helper), infrastructure (lib/, tools/), orphan;
#                           RESULT actions.awk's: "run<TAB>..." or "class<TAB>header: ..."
T_ROOT=$TOP/testsuite

cmd_suite_roles() {
  need_tree; mkdir -p "$S"
  local out=$S/roles.tsv f
  # every header's text, by file (under tests/)
  (cd "$T_ROOT/tests" && find . -name '*.ml' | sed 's|^\./||' | LC_ALL=C sort) | while read -r f; do
    printf '%s\t%s\n' "$f" "$(awk -f "$root/bin/lib/oi/header-text.awk" "$T_ROOT/tests/$f")"
  done > "$S/headers.tsv"
  (cd "$T_ROOT" && find tests lib tools -type f \( -name '*.ml' -o -name '*.mli' \) | LC_ALL=C sort) > "$S/files.txt"
  while read -r f; do
    case $f in
      lib/*|tools/*) printf '%s\tclass\theader: infrastructure\n' "$f" ;;
      *.ml) printf '%s\t%s\n' "$f" "$(awk -f "$root/bin/lib/oi/actions.awk" "$T_ROOT/$f")" ;;
      *.mli) printf '%s\tclass\theader: interface\n' "$f" ;;
    esac
  done < "$S/files.txt" > "$S/actions-all.tsv"
  awk -F'\t' -v H="$S/headers.tsv" '
    BEGIN {
      while ((getline line < H) > 0) { split(line, a, "\t"); hdr["tests/" a[1]] = a[2] }
    }
    function dirof(p) { sub(/[^\/]*$/, "", p); sub(/\/$/, "", p); return p }
    function base(p) { sub(/.*\//, "", p); return p }
    function modname(p,  b) { b = base(p); sub(/\.mli?$/, "", b); return b }
    {
      f = $1; rest = $2; for (i = 3; i <= NF; i++) rest = rest "\t" $i
      if (f ~ /^(lib|tools)\//) { print f "\tinfrastructure\t" rest; next }
      if (f ~ /\.mli$/) { print f "\tinterface\t" rest; next }
      if ($2 == "class" && $3 == "header: none") {
        d = dirof(f); named = 0; b = base(f); m = modname(f)
        for (h in hdr) {
          if (h == f) continue
          hd = dirof(h)
          # a header of a test in the file'"'"'s directory, or above it (its subdirectories), names it
          if (index(d "/", hd "/") != 1) continue
          t = " " hdr[h] " "
          if (index(t, b) || t ~ ("[^A-Za-z_0-9]" m "[^A-Za-z_0-9.]") || (hd != d && t ~ /subdirectories/)) { named = 1; break }
        }
        if (named) print f "\thelper\tclass\theader: helper of a test of its directory"
        else print f "\torphan\t" rest
        next
      }
      print f "\ttest\t" rest
    }' "$S/actions-all.tsv" > "$out"
  echo "oi: roles: $(wc -l < "$out") files: $(cut -f2 "$out" | sort | uniq -c | tr '\n' ' ')($out)"
}

# ---- the expect action, run by the host's expect tool (testsuite/tools/expect.ml built against compiler-libs)

EXPECT_DIR=$root/.tmp/o7/exp
EXPECT_RESULTS=$root/test/ocaml/ref-expect.edn

cmd_suite_expect_build() {
  need_tree; mkdir -p "$EXPECT_DIR"
  cmd_stdlib > "$OI_DIR/stdlib.log"
  # the tool: expect.ml against the host's compiler-libs, linked whole (-linkall: ocamltest's build links every unit of the
  # stdlib, so Either, Iarray, ... are in the executable the toplevel's phrases reach); the environment of each file is the tree's
  # (OC105 (b), OC106 (b)): -repo-root names a root whose stdlib/ holds the stdlib of bin/oi stdlib's build (its interfaces,
  # objects, stdlib.cma and std_exit.cmo), as ocamltest passes the built tree
  local key; key=$({ cat "$T_ROOT/tools/expect.ml" "$T_ROOT/tools/expect.mli"; sha256sum "$(ocamlopt -where)/compiler-libs/ocamlcommon.cma"; cat "$LIB/key"; echo linkall-repo-root-1; } | sha256sum | cut -d' ' -f1)
  if [ -x "$EXPECT_DIR/expect.byte" ] && [ "$(cat "$EXPECT_DIR/key" 2>&1)" = "$key" ]; then return 0; fi
  rm -rf "$EXPECT_DIR/repo" "$EXPECT_DIR/expect.byte"; mkdir -p "$EXPECT_DIR/repo/stdlib"
  cp "$OI_DIR"/stdlib-build/*.cmi "$OI_DIR"/stdlib-build/*.cmo "$EXPECT_DIR/repo/stdlib/"
  cp "$OI_DIR/stdlib-build/std_exit.ml" "$OI_DIR/stdlib-build/std_exit.mli" "$EXPECT_DIR/repo/stdlib/"
  local mods; mods=$(awk '{ print $2 ".cmo" }' "$OI_DIR/stdlib-build/order.txt" | tr '\n' ' ')
  (cd "$EXPECT_DIR/repo/stdlib" && ocamlc -nostdlib -w -a -I . -c std_exit.mli && ocamlc -nostdlib -w -a -I . -c std_exit.ml && ocamlc -nostdlib -a -o stdlib.cma $mods) || die "the tree's stdlib.cma"
  cp "$T_ROOT/tools/expect.ml" "$T_ROOT/tools/expect.mli" "$EXPECT_DIR/"
  (cd "$EXPECT_DIR" && ocamlc -linkall -I +compiler-libs ocamlcommon.cma ocamlbytecomp.cma ocamltoplevel.cma expect.mli expect.ml -o expect.byte) || die "expect tool"
  echo "$key" > "$EXPECT_DIR/key"
}

# suite_expect_one FILE (relative to testsuite/): ocamltest's expect action: the tool on the file, then with -principal
# on its correction; pass when both succeed and the final correction equals the file. "FILE<TAB>pass|diff|error<TAB>NOTE"
suite_expect_one() {
  local f=$1 flags=$2
  local d=$EXPECT_DIR/run/$f n; n=$(basename "$f")
  rm -rf "$d"; mkdir -p "$d"
  find "$T_ROOT/$(dirname "$f")" -maxdepth 1 -type f ! -name '*.reference' -exec cp {} "$d/" \;
  local st=0
  case $flags in
    *@SKIP=*) local why=${flags##*@SKIP=}; why=${why%% *}
      printf '%s\tskip\t%s\n' "$f" "$why"; rm -rf "$d"; return ;;
  esac
  case $flags in
    *@ORP=*) local orp=${flags##*@ORP=}; orp=${orp%% *}; export OCAMLRUNPARAM=$orp; flags=${flags//@ORP=$orp/} ;;
  esac
  # a test that reads the compiler's own interfaces (-I +compiler-libs) is run on the installed stdlib: the installed compiler-libs
  # were compiled against it, and its Format, Float, In_channel, Out_channel and Scanf interfaces are not the tree's (CRCs differ)
  local root_flag="-repo-root $EXPECT_DIR/repo"
  case $flags in *+compiler-libs*) root_flag="" ;; esac
  (cd "$d" && timeout 120 ocamlrun "$EXPECT_DIR/expect.byte" $root_flag $flags "$n" > "$d/e1.out" 2>&1) || st=$?
  if [ $st != 0 ]; then printf '%s\terror\texpect exit %s: %s\n' "$f" "$st" "$(head -c 160 "$d/e1.out" | tr '\n\t' '  ')"; rm -rf "$d"; return; fi
  (cd "$d" && timeout 120 ocamlrun "$EXPECT_DIR/expect.byte" $root_flag $flags -principal "$n.corrected" > "$d/e2.out" 2>&1) || st=$?
  if [ $st != 0 ]; then printf '%s\terror\texpect -principal exit %s: %s\n' "$f" "$st" "$(head -c 160 "$d/e2.out" | tr '\n\t' '  ')"; rm -rf "$d"; return; fi
  if cmp -s "$d/$n" "$d/$n.corrected.corrected"; then printf '%s\tpass\t\n' "$f"; else printf '%s\tdiff\t%s\n' "$f" "$(diff "$d/$n" "$d/$n.corrected.corrected" | head -4 | tr '\n\t' '  ' | head -c 200)"; fi
  rm -rf "$d"
}

cmd_suite_expect() {
  cmd_suite_roles > "$S/roles.log"
  cmd_suite_expect_build
  # the files whose header has an expect statement, and their flags
  awk -F'\t' -v FLAT="$(ocamlc -config | awk -F': ' '$1 == "flat_float_array" { print $2 }')" '{
    t = " ; " $2 " ; "; sub(/TEST/, ";", t); gsub(/[{};]/, " ; ", t)
    if (t !~ /; *expect *;/) next
    fl = ""
    # include LIB: ocamltest adds the library'"'"'s directory (the installed ones: the compiler-libs, the threads and unix)
    if (t ~ /include (ocamlcommon|ocamltoplevel|ocamlbytecomp)/) fl = fl " -I +compiler-libs"
    if (t ~ /include systhreads/) fl = fl " -I +threads -I +unix"
    # a configuration the host does not have (ocamlc -config: flat_float_array true), a build sequence before the expect action
    if (t ~ /; *no-flat-float-array *;/ && FLAT == "true") fl = fl " @SKIP=no-flat-float-array"
    else if (t ~ /; *setup-ocamlc\.(byte|opt)-build-env *;/) fl = fl " @SKIP=build-sequence"
    h = $2; gsub(/\$\{ocamlsrcdir\}/, "OCAMLSRCDIR", h)
    n = split(h, st, /[;{}]/)
    for (i = 1; i <= n; i++) {
      sub(/^ *TEST */, "", st[i])
      v = st[i]; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v)
      if (match(st[i], /^ *flags *\+?= *"/)) fl = fl " " v
      # the OCaml source tree of ocamltest: its compiled directories are the installed compiler-libs here
      if (match(st[i], /^ *ocamlrunparam *= *"/)) fl = fl " @ORP=" v
    }
    gsub(/OCAMLSRCDIR\/[A-Za-z_]*/, "+compiler-libs", fl)
    gsub(/[ \t]+/, " ", fl); print "tests/" $1 "\t" fl
  }' "$S/headers.tsv" > "$S/expect.list"
  if [ -n "${ONLY:-}" ]; then grep -E "^tests/($ONLY)" "$S/expect.list" > "$S/expect.list.only" || true; mv "$S/expect.list.only" "$S/expect.list"; fi
  export -f suite_expect_one; export EXPECT_DIR T_ROOT
  local out=$S/expect.tsv
  [ -n "${ONLY:-}" ] && out=$S/expect-only.tsv; EXPECT_RESULTS_ALL=$EXPECT_RESULTS; [ -n "${ONLY:-}" ] && EXPECT_RESULTS=$S/expect-only.edn
  tr '\n' '\0' < "$S/expect.list" | xargs -0 -n 1 -P "${JOBS:-3}" bash -c 't=$(printf "\t"); suite_expect_one "${0%%"$t"*}" "${0#*"$t"}"' > "$out.new"
  LC_ALL=C sort "$out.new" > "$out"; rm -f "$out.new"
  { echo ";; The expect action's host runs (bin/oi suite expect; doc/ocaml/OCAML-SURVEY.md, \"As built: OC102, OC104, OC105 (b), OC106 (b)\"): testsuite/tools/expect.ml"
    echo ";; built against the host's compiler-libs, linked whole, and run as ocamltest runs it (-repo-root the tree's stdlib), on the file, then with -principal on the correction:"
    echo ";; [FILE RESULT NOTE], RESULT pass (the final correction equals the file), diff, error, skip (the header's constraint or build sequence). Read with the Clojure reader."
    echo "["
    sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' "$out" | awk -F'\t' '{ printf " [\"%s\" \"%s\" \"%s\"]\n", $1, $2, $3 }'
    echo "]"; } > "$EXPECT_RESULTS"
  echo "oi: expect: $(wc -l < "$out") tests: $(cut -f2 "$out" | sort | uniq -c | tr '\n' ' ')($EXPECT_RESULTS)"
}

# the rollup: every file of testsuite/ a row of test/ocaml/testsuite-results.edn, judged (no run: the records of the levels)
cmd_suite_rollup() {
  # always from the tree as it is: a roles.tsv kept from an earlier tree, header reading or actions.awk would be judged against
  # the levels' new records (review 008's finding 5); it takes seconds
  cmd_suite_roles > "$S/roles.log"
  local mode=judge; [ "${1:-}" = --record ] && mode=record
  "$(arbace_exe)" "$root/test/ocaml/testsuite_check.ae" "$mode" "$S/roles.tsv" "$root/test/ocaml/testsuite-results.edn"
}
