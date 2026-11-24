# bin/oi's ppx verbs (queue 546, the user's 539b-1, T1: ppxlib's bootstrap on oi; doc/ocaml/OCAML-SURVEY.md, "As built: ppxlib's
# bootstrap"). ppxlib 0.39.0's build run on oi from the pinned opam set (test/ocaml/corpora.edn's :opam-ppxlib and its closure), no
# ppx of the host's: the programs of test/ocaml/ppx-programs.tsv (opam-programs.tsv's format; not rows of the suite corpus, no record),
# each staged, typed by Arbace's typer and run on oi as the suite's programs are (suite_oi_one), their units' build texts by
# arbace.repr.ocaml.prep (astlib's and ast's pp, ocaml-compiler-libs' ocaml_common and ocaml_shadow, src/'s generated units).
# Sourced by bin/oi after suite.bash.
#
#   bin/oi ppx gen     src/gen's gen_ast_pattern and gen_ast_builder run on oi over ast/ast.ml: the units ppxlib's src/dune makes,
#                      ast_pattern_generated.ml and ast_builder_generated.ml, kept in .tmp/oi/ppx/gen (OI_PPXLIB_GEN to the staging)
#   bin/oi ppx check   the metaquot driver (test/metaquot/driver.ml: Ppxlib.Driver.standalone with ppxlib.metaquot linked) run on
#                      oi over the cram cases of ppxlib's test/metaquot/run.t (each `cat > F << EOF` input, then `./driver.exe F`),
#                      its output against the expected output the cram file holds, line for line; its negative control: a case's
#                      output judged against another case's expected output must differ
#   bin/oi ppx run ID  (queue 547) the program ID of ppx-programs.tsv staged, typed and run on oi as the suite runs one; its result line
#   bin/oi ppx pp [DRIVER...]  (queue 547) the units of test/ocaml/ppx-pp.tsv run through their ppx driver on oi as dune runs it, the
#                      texts kept in .tmp/oi/ppx/pp/PKG/FILE (OI_PPX_PP), which the staging gives a unit its dune file preprocesses
#                      by `(pps ...)`
#   bin/oi ppx jst-config  (queue 547) jst-config's config.h for oi's C target (discover.ml ported), .tmp/oi/ppx/jst-config
#   bin/oi ppx expect  (queue 547, S3) re's lib_test/expect run on oi, judged by its own [%expect] blocks, with its negative control

PPX=$OI_DIR/ppx
PPX_ROWS=$root/test/ocaml/ppx-programs.tsv

# ppx_row ID: the corpus row (suite_oi_one's fields, tab-separated) of the program ID of ppx-programs.tsv, @PPXIN@ in its ARGS the
# directory of the extracted cram inputs, @PPX@ anywhere .tmp/oi/ppx (the generated files: jst-config's config.h, the pp texts)
ppx_row() {
  grep -v '^#' "$PPX_ROWS" | awk -F'\t' -v id="$1" -v opam="$OPAM" '$2 == id { printf "opam/%s/%s\t%s\t%s\t%s\t%s\t%s\t%s\n", $1, $2, $3, $7, $4, $6, opam "/" $1, $5 }' |
    sed "s|@OCAMLSRCDIR@|$TOP|g; s|@PPXIN@|$PPX/in|g; s|@PPX@|$PPX|g"
}

# ppx_prepare ID (queue 547): the program ID typed once (suite_dump) and its C stubs' node program built (cstubs_nodes), for runs of
# its own: PD (its run directory), PD_TL (the libraries' units), PD_ENV (the row's environment with OI_CSTUBS); dies on a failure
ppx_prepare() {
  local row id flags incs args env base units
  row=$(ppx_row "$1"); [ -n "$row" ] || die "ppx: no program $1 in $PPX_ROWS"
  IFS=$'\037' read -r id flags incs args env base units <<< "$(tr '\t' '\037' <<< "$row")"
  suite_dump "$id" "$flags" "$incs" "$base" "$units"
  PD=$S/run/$id
  [ -f "$PD/error" ] && die "ppx: $1: $(cat "$PD/error")"
  PD_TL=$(inc_units "$incs" $(cat "$PD/units.list") | tr '\n' ' ')
  local miss; miss=$(lib_missing $PD_TL); [ -z "$miss" ] || die "ppx: $1: typer: $miss"
  local cs="" kv nodes
  for kv in $env; do case $kv in @cstubs=*) cs=${kv#@cstubs=} ;; esac; done
  if [ -n "$cs" ]; then
    nodes=$(cstubs_nodes "$base" "$cs" 2> "$PD/cnodes.err") || die "ppx: $1: cstubs: $(grep -v '^Reflection\|^Auto-boxing\|recur arg' "$PD/cnodes.err" | tail -1)"
    env="$env OI_CSTUBS=$nodes"
  fi
  PD_ENV=$env
}

# ppx_pp [DRIVER...] (queue 547): the units dune preprocesses by ppx rewriters (`(preprocess (pps ...))`), each run through its
# driver on oi as dune runs it (`DRIVER --cookie library-name="LIB" [ARGS] -o OUT --impl|--intf FILE`, in the package's directory,
# FILE relative to it), the text written as OI_PPX_PP/PKG/FILE (prep's build-unit gives it to the suite's staging); the rows of
# test/ocaml/ppx-pp.tsv (DRIVER PKG LIB ARGS FILES), those of the DRIVERs named
cmd_ppx_pp() {
  local only=" $* " drv pkg lib pargs files f st n=0 fail=0 last=""
  while IFS=$'\t' read -r drv pkg lib pargs files; do
    case $drv in '#'*|'') continue ;; esac
    [ "$only" = "  " ] || case $only in *" $drv "*) ;; *) continue ;; esac
    [ "$drv" = "$last" ] || { ppx_prepare "$drv"; last=$drv; }
    [ "$pargs" = - ] && pargs=""; pargs=${pargs//@PPX@/$PPX}
    # the driver's directory: the package's files at their paths (dune's _build/default), with the rule-made files the row names
    # (@with=DEST=SRC, SRC under .tmp/oi/ppx as @PPX@: jst-config's config.h for time_now), so a unit's locations stay FILE's own
    local w=$PPX/work/$pkg a ra=""; rm -rf "$w"; mkdir -p "$w"; cp -a "$OPAM/$pkg/." "$w/"
    for a in $pargs; do case $a in @with=*) a=${a#@with=}; cp "${a#*=}" "$w/${a%%=*}" || die "ppx pp: $drv: no ${a#*=}" ;; *) ra="$ra $a" ;; esac; done
    for f in $files; do
      local out=$OI_PPX_PP/$pkg/$f k=--impl; case $f in *.mli) k=--intf ;; esac
      mkdir -p "$(dirname "$out")"; rm -f "$out" "$out.log"
      st=0
      (cd "$w" && run_redirected "$PD_ENV" "$out.log" limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$LIB" $PD_TL $(cat "$PD/units.list") -- \
        --cookie "library-name=\"$lib\"" $ra -o "$out" $k "$f") || st=$?
      n=$((n + 1))
      if [ "$st" = 0 ] && [ -s "$out" ]; then echo "pp $pkg/$f ($drv)"
      else echo "FAIL $pkg/$f ($drv): status $st: $(head -c 400 "$out.log" | tr '\n' ' ')"; rm -f "$out"; fail=$((fail + 1)); fi
    done
  done < "$root/test/ocaml/ppx-pp.tsv"
  echo "oi: ppx pp: $n files, $fail failures ($OI_PPX_PP)"
  [ $fail = 0 ]
}

# ppx_jst_config (queue 547): jst-config's config.h as its src/dune rule makes it, by discover/discover.ml (held to its SHA-256; dune-
# configurator's c_test and C_define.import ported): each of discover's C programs read, checked and linked with musl for oi's own C
# target by test/ocaml/capi/cnodes.ae (--program; the stubs' target and caml/*.h), success its switch; the header written as
# C.C_define.gen_header_file writes it (the guard CONFIG_H_CONFIG_H, `#define N`, `#undef N`, `#define N (V)`), in
# .tmp/oi/ppx/jst-config/config.h, kept by the key of discover.ml, thread_id.h and the C API's sources
JST_DISCOVER_SHA=15a3e1b8e3c24d4740e204d848c57b8f9ead212cb2d52028cb015a83dba657ed
cmd_ppx_jst_config() {
  local src=$OPAM/jst-config d=$PPX/jst-config k
  [ "$(sha256sum < "$src/discover/discover.ml" | cut -d' ' -f1)" = "$JST_DISCOVER_SHA" ] || die "ppx jst-config: discover.ml is not the ported one"
  k=$( { sha256sum < "$src/discover/discover.ml"; sha256sum < "$src/src/thread_id.h"; (cd "$root" && find test/ocaml/capi -type f | LC_ALL=C sort | xargs sha256sum); echo "${ARBACE_C_TARGET:-arbace}"; } | sha256sum | cut -c1-20)
  [ -s "$d/config.h" ] && [ "$(cat "$d/key" 2> /dev/null)" = "$k" ] && { echo "oi: ppx jst-config: up to date ($d/config.h)"; return 0; }
  rm -rf "$d"; mkdir -p "$d/t"
  # discover's programs, its {|...|} texts by name
  awk -v d="$d/t" '/^let [a-z_]+ .*=$/ { n = $2; next } /^let [a-z_]+ =.*\{\|$/ { n = $2; inb = 1; f = d "/" n ".c"; printf "" > f; next }
    n != "" && /^ *\{\|$/ { inb = 1; f = d "/" n ".c"; printf "" > f; next } inb && /^\|\}/ { inb = 0; close(f); n = ""; next } inb { print >> f }' "$src/discover/discover.ml"
  local exe; exe=$(arbace_exe)
  ctest() { # NAME FILE [FLAG...]: 1 when FILE builds for the target
    ( . "$root/bin/lib/repr-c/sysroot.bash"; c_sysroot > /dev/null || exit 1
      cd "$d/t" && ARBACE_PATH=$root "$exe" "$root/test/ocaml/capi/cnodes.ae" "$root" "$d/t/$1.nodes" --program "${@:3}" "$2" ) > "$d/t/$1.log" 2>&1 && echo 1 || echo 0
  }
  . "$root/bin/lib/crops/musl-hold.bash"; ci_musl_hold > "$d/musl.log" 2>&1 || die "ppx jst-config: musl's forms: $(tail -1 "$d/musl.log")"
  local v=() n c fl
  # rlimit_vars: <sys/resource.h> builds, then C_define.import of the two switches (each defined or not)
  printf '#include <sys/resource.h>\nint main() { return 0; }\n' > "$d/t/resource.c"
  if [ "$(ctest resource resource.c)" = 1 ]; then
    for n in RLIMIT_AS RLIMIT_NICE; do printf '#include <sys/resource.h>\n#ifndef %s\n#error undefined\n#endif\nint main() { return 0; }\n' $n > "$d/t/imp_$n.c"; v+=("$n $(ctest imp_$n imp_$n.c)"); done
  else v+=("RLIMIT_AS 0" "RLIMIT_NICE 0"); fi
  for n in ARCH_BIG_ENDIAN ARCH_SIXTYFOUR; do printf '#include <caml/config.h>\n#ifndef %s\n#error undefined\n#endif\nint main() { return 0; }\n' $n > "$d/t/imp_$n.c"; v+=("$n $(ctest imp_$n imp_$n.c)"); done
  for c in EVENTFD:eventfd_code TIMESPEC:timespec_code:-std=c11 TIMERFD:timerfd_code WORDEXP:wordexp_code MEMFD:memfd_code \
           MSG_NOSIGNAL:msg_nosignal_code SO_NOSIGPIPE:so_nosigpipe_code FDATASYNC:fdatasync_code RECVMMSG:recvmmsg_code \
           THREAD_CPUTIME:thread_cputime_code PTHREAD_NP:pthread_np MKOSTEMP:mkostemp_code READDIR_DTYPE:readdir_dtype_code; do
    IFS=: read -r n c fl <<< "$c"; [ -s "$d/t/$c.c" ] || die "ppx jst-config: no $c in discover.ml"
    v+=("$n $(ctest "$c" "$c.c" $fl)")
  done
  v+=("POSIX_TIMERS $(ctest posix_timers_code posix_timers_code.c)" "CLOCK_GETCPUCLOCKID $(ctest clock_getcpuclockid_code clock_getcpuclockid_code.c)")
  # thread_id_code's sprintf: the first method of 1 and 2 that builds, else -1
  local m tid=-1
  # (discover names thread_id.h by its absolute path, Sys.getcwd's; repr/c's preprocessor joins an absolute quoted name to each
  # directory of the search, so the header is copied beside the programs and named by its file name, the same text included)
  cp "$src/src/thread_id.h" "$d/t/thread_id.h"
  for m in 1 2; do sed "s|%d|$m|; s|%s|thread_id.h|" "$d/t/thread_id_code.c" > "$d/t/thread_id_$m.c"; [ "$(ctest thread_id_$m thread_id_$m.c)" = 1 ] && { tid=$m; break; }; done
  v+=("THREAD_ID_METHOD =$tid")
  # LINUX_EXT: ocaml_config_var system, oi's "linux" (Sys.os_type Unix, the C API's target)
  v+=("LINUX_EXT 1")
  { echo "#ifndef CONFIG_H_CONFIG_H"; echo "#define CONFIG_H_CONFIG_H"
    for c in "${v[@]}"; do n=${c%% *}; c=${c#* }
      case $c in =*) echo "#define JSC_$n (${c#=})" ;; 1) echo "#define JSC_$n" ;; *) echo "#undef JSC_$n" ;; esac; done
    echo "#endif"; } > "$d/config.h.new"
  mv "$d/config.h.new" "$d/config.h"; echo "$k" > "$d/key"
  echo "oi: ppx jst-config: $d/config.h"; cat "$d/config.h"
}

# ppx_expect_judge DIR RESULT: re's expect tests judged by their own [%expect] blocks, as dune's runtest does: the runner's status 0
# and no .corrected file under the source tree DIR (ppx_expect writes FILE.corrected beside a test whose output differs); "pass" or
# the reason on stdout
ppx_expect_judge() {
  local tree=$1 res=$2 st c
  st=$(cut -f2 <<< "$res"); c=$(cd "$tree" && find . -name '*.corrected' | LC_ALL=C sort | tr '\n' ' ')
  if [ "$st" = 0 ] && [ -z "$c" ]; then echo pass; else echo "fail: status $st, corrected: ${c:-none}"; fi
}

# cmd_ppx_expect (queue 547, S3): re's lib_test/expect on oi (the program expect_tests of ppx-programs.tsv: its 27 files rewritten by
# the ppx_expect driver on oi, bin/oi ppx pp expect_driver), judged by the tests' own expected output (ppx_expect_judge); its negative
# control: the same program with one [%expect] of test_re.ml altered (a line added to its first block, the file rewritten again by
# the driver into a pp tree of its own) must fail
cmd_ppx_expect() {
  local tree=$PPX/work/re res v
  [ -s "$OI_PPX_PP/re/lib_test/expect/test_re.ml" ] || die "ppx expect: no rewritten tests (bin/oi ppx pp expect_driver)"
  find "$tree" -name '*.corrected' -delete 2> /dev/null || true
  res=$(ppx_run expect_tests); echo "$res"; v=$(ppx_expect_judge "$tree" "$res"); echo "expect_tests: $v"
  # the control
  local cpp=$PPX/pp-control ctree=$PPX/work/re-control f=lib_test/expect/test_re.ml cv
  rm -rf "$cpp" "$ctree"; mkdir -p "$cpp"; cp -a "$OI_PPX_PP/." "$cpp/"; mkdir -p "$ctree"; cp -a "$OPAM/re/." "$ctree/"
  awk '!done && /\[%expect \{\|/ { sub(/\[%expect \{\|/, "[%expect {| CONTROL"); done = 1 } { print }' "$OPAM/re/$f" > "$ctree/$f"
  cmp -s "$OPAM/re/$f" "$ctree/$f" && die "ppx expect: the control's alteration changed nothing"
  ppx_prepare expect_driver
  (cd "$ctree" && run_redirected "$PD_ENV" "$cpp/control.log" limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$LIB" $PD_TL $(cat "$PD/units.list") -- \
    --cookie 'library-name="re_tests"' --cookie 'inline_tests="enabled"' -corrected-suffix .ppx-corrected -diff-cmd - -o "$cpp/re/$f" --impl "$f") \
    || die "ppx expect: the control's rewrite failed: $(head -c 300 "$cpp/control.log")"
  local cres; cres=$(OI_PPX_PP=$cpp ppx_run_tree expect_tests "$ctree"); echo "$cres"
  cv=$(ppx_expect_judge "$ctree" "$cres")
  if [ "$cv" = pass ]; then echo "control (test_re.ml's first [%expect] altered): passes: the judgment passes what differs"; v="fail: the control passed"
  else echo "control (test_re.ml's first [%expect] altered): $cv, as a control must"; fi
  echo "oi: ppx expect: re/expect $v"
  [ "$v" = pass ]
}

# ppx_run_tree ID TREE: ppx_run with the row's source tree (@PPX@/work/re in its ARGS) replaced by TREE
ppx_run_tree() {
  local row id flags incs args env base units
  row=$(ppx_row "$1" | sed "s|$PPX/work/re|$2|g"); [ -n "$row" ] || die "ppx: no program $1 in $PPX_ROWS"
  IFS=$'\037' read -r id flags incs args env base units <<< "$(tr '\t' '\037' <<< "$row")"
  suite_oi_one "$id" "$flags" "$incs" "$args" "$env" "$base" "$units"
}

# ppx_run ID: the program run on oi as the suite runs one (its run directory $S/run/opam/PKG/ID); the result line on stdout
ppx_run() {
  local row; row=$(ppx_row "$1"); [ -n "$row" ] || die "ppx: no program $1 in $PPX_ROWS"
  local id flags incs args env base units
  IFS=$'\037' read -r id flags incs args env base units <<< "$(tr '\t' '\037' <<< "$row")"
  suite_oi_one "$id" "$flags" "$incs" "$args" "$env" "$base" "$units"
}

ppx_env() {
  opts "$@"
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build; NATIVE=1; suite_native_env
  mkdir -p "$PPX" "$S/run"
  export OI_PPXLIB_GEN=$PPX/gen OI_OCAML_TOP=$TOP
  # (queue 547) the programs share hundreds of units: their typed outputs kept by chained key (suite_dump_native's OI_DUMP_CACHE)
  export OI_DUMP_CACHE=${OI_DUMP_CACHE:-$PPX/dump-cache} OI_PPX_PP=${OI_PPX_PP:-$PPX/pp}
  export -f limited_cpu oi_wall oi_status oi_cpu_secs oi_limit_kill suite_oi_one suite_dump suite_dump_native suite_stage suite_gen suite_names suite_cap suite_unit_file suite_wrap_one suite_wrap_path suite_needs_build suite_alias
  export S OI_DIR TESTS TESTLIB TIMEOUT EXE LIB OCD GOMEMLIMIT root
}

# the generated units: each generator's run, the file it writes in its run directory kept in $PPX/gen
cmd_ppx_gen() {
  ppx_env "$@"
  rm -rf "$PPX/gen.new"; mkdir -p "$PPX/gen.new"
  local g f st=0
  for g in gen_ast_pattern:ast_pattern_generated.ml gen_ast_builder:ast_builder_generated.ml; do
    f=${g#*:}; g=${g%%:*}
    ppx_run "$g" | tee "$PPX/$g.result"
    if [ "$(cut -f2 "$PPX/$g.result")" = 0 ] && [ -s "$S/run/opam/ppxlib/$g/$f" ]; then cp "$S/run/opam/ppxlib/$g/$f" "$PPX/gen.new/"
    else echo "oi: ppx gen: $g made no $f ($S/run/opam/ppxlib/$g)" >&2; st=1; fi
  done
  [ $st = 0 ] || die "ppx gen failed"
  rm -rf "$PPX/gen"; mv "$PPX/gen.new" "$PPX/gen"
  echo "oi: ppx gen: $(ls "$PPX/gen" | tr '\n' ' ')in $PPX/gen"
}

# ppx_cram RUN.T DIR: the cram file's cases into DIR: each `cat > F << EOF` input as DIR/in/F, each `./driver.exe F`'s expected output
# (the indented lines that follow it, their two spaces taken off; cram's `[N]`, the exit status, as DIR/exp/F.status) as DIR/exp/F.out;
# the cases' names, one a line, in DIR/cases
ppx_cram() {
  local t=$1 d=$2
  rm -rf "$d/in" "$d/exp"; mkdir -p "$d/in" "$d/exp"; : > "$d/cases"
  awk -v d="$d" '
    function flush() { if (cur != "") { close(outf) }; cur = "" }
    /^  \$ cat > [^ ]+ << EOF$/ { flush(); mode = "in"; f = $4; inf = d "/in/" f; printf "" > inf; next }
    mode == "in" && /^  > EOF$/ { close(inf); mode = ""; next }
    mode == "in" && /^  > / { print substr($0, 5) >> inf; next }
    mode == "in" && /^  >$/ { print "" >> inf; next }
    /^  \$ \.\/driver\.exe [^ ]+$/ { flush(); mode = "out"; cur = $3; outf = d "/exp/" cur ".out"; printf "" > outf; print cur >> (d "/cases"); next }
    mode == "out" && /^  \[[0-9]+\]$/ { s = $1; gsub(/[^0-9]/, "", s); print s > (d "/exp/" cur ".status"); close(d "/exp/" cur ".status"); next }
    mode == "out" && /^  / { print substr($0, 3) >> outf; next }
    mode == "out" { flush(); mode = ""; next }
  ' "$t"
}

# the metaquot driver on oi over test/metaquot/run.t's cases: typed once (suite_dump), run once a case in the inputs' directory
cmd_ppx_check() {
  ppx_env "$@"
  [ -s "$PPX/gen/ast_pattern_generated.ml" ] && [ -s "$PPX/gen/ast_builder_generated.ml" ] || die "ppx check: no generated units (bin/oi ppx gen)"
  local cram=$OPAM/ppxlib/test/metaquot/run.t c
  ppx_cram "$cram" "$PPX"
  [ -s "$PPX/cases" ] || die "ppx check: no case in $cram"
  local row id flags incs args env base units
  row=$(ppx_row metaquot_driver); [ -n "$row" ] || die "ppx: no program metaquot_driver in $PPX_ROWS"
  IFS=$'\037' read -r id flags incs args env base units <<< "$(tr '\t' '\037' <<< "$row")"
  suite_dump "$id" "$flags" "$incs" "$base" "$units"
  local d=$S/run/$id
  [ -f "$d/error" ] && die "ppx check: the driver: $(cat "$d/error")"
  local tl; tl=$(inc_units "$incs" $(cat "$d/units.list") | tr '\n' ' ')
  local miss; miss=$(lib_missing $tl); [ -z "$miss" ] || die "ppx check: the driver: typer: $miss"
  rm -rf "$PPX/out"; mkdir -p "$PPX/out"
  local pass=0 fail=0 st want
  while read -r c; do
    st=0
    (cd "$PPX/in" && run_redirected "$env" "$PPX/out/$c.out" limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$LIB" $tl $(cat "$d/units.list") -- "$c") || st=$?
    want=0; [ -f "$PPX/exp/$c.status" ] && want=$(cat "$PPX/exp/$c.status")
    if [ "$st" = "$want" ] && cmp -s "$PPX/out/$c.out" "$PPX/exp/$c.out"; then echo "pass $c"; pass=$((pass + 1))
    else echo "FAIL $c: status $st (cram's $want); diff:"; diff "$PPX/exp/$c.out" "$PPX/out/$c.out" | head -20; fail=$((fail + 1)); fi
  done < "$PPX/cases"
  # the negative control (GTS81 (a)): the first case's output against the second's expected output must differ
  local c1 c2; c1=$(sed -n 1p "$PPX/cases"); c2=$(sed -n 2p "$PPX/cases")
  if [ -n "$c2" ]; then
    if cmp -s "$PPX/out/$c1.out" "$PPX/exp/$c2.out"; then echo "control $c1-vs-$c2: the same: the comparison passes what differs"; fail=$((fail + 1))
    else echo "control $c1-vs-$c2: differs, as a control must"; fi
  fi
  echo "oi: ppx check: $pass of $(wc -l < "$PPX/cases") cases of $cram pass, $fail failures ($PPX/out)"
  [ $fail = 0 ]
}

cmd_ppx() {
  local verb=${1:-}; [ -n "$verb" ] || die "usage: bin/oi ppx gen|check|run ID|pp [DRIVER...]|jst-config|expect"
  shift
  case $verb in
    gen) cmd_ppx_gen "$@" ;;
    pp) ppx_env; cmd_ppx_pp "$@" ;;
    jst-config) ppx_env; cmd_ppx_jst_config ;;
    expect) ppx_env; cmd_ppx_expect ;;
    run) local id=${1:-}; [ -n "$id" ] || die "usage: bin/oi ppx run ID"; shift; ppx_env "$@"; ppx_run "$id" ;;
    check) cmd_ppx_check "$@" ;;
    *) die "unknown ppx verb $verb" ;;
  esac
}
