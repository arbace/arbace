# bin/oi's reading of an ocamltest header (testsuite/tests' "(* TEST ... *)", or the block after a
# "(* TEST_BELOW" placeholder): one line, "run<TAB>FLAGS<TAB>INCLUDES<TAB>ARGS<TAB>ENV" for a test oi
# runs as one program (doc/ocaml/OCAML-SURVEY.md, "As built: O5 (write-ahead)", OC61), else
# "class<TAB>REASON", REASON the first statement outside that set.
function trim(s) { gsub(/^[ \t\n]+|[ \t\n]+$/, "", s); return s }
# a class: REASON, or in helper mode (below) past its readonly_files statement the reason the test had before that mode, that statement
function cls(r) { if (helper && roseen) r = roclass; print "class\theader: " r; exit }
{ text = text $0 " " }
END {
  # the header: the first "(* TEST" that is not "(* TEST_BELOW", to its balanced "*)"
  n = length(text); start = 0; pos = 1
  while (1) {
    i = index(substr(text, pos), "(* TEST")
    if (i == 0) break
    i += pos - 1
    if (substr(text, i, 13) == "(* TEST_BELOW") { pos = i + 13; continue }
    start = i; break
  }
  if (start == 0) { print "class\theader: none"; exit }
  depth = 0; body = ""
  for (k = start; k <= n; k++) {
    two = substr(text, k, 2)
    if (two == "(*") { depth++; k++; if (depth > 1) body = body " "; continue }
    if (two == "*)") { depth--; k++; if (depth == 0) break; continue }
    if (depth == 1) body = body substr(text, k, 1)
  }
  sub(/^ *TEST/, "", body)
  # ocamltest's ${ocamlsrcdir}, the compiler's source tree (OT21 (b)): the pinned tarball's, bin/oi corpus puts its path for @OCAMLSRCDIR@
  gsub(/\$\{ocamlsrcdir\}/, "@OCAMLSRCDIR@", body)
  gsub(/\$\{test_source_directory\}/, "@TSD@", body)
  gsub(/\$\{[a-z_]*\}/, "$VAR", body)
  gsub(/[{}]/, ";", body)
  m = split(body, st, ";")
  flags = ""; incs = ""; args = ""; env = ""
  # helper mode (OC201 (a), doc/ocaml/OCAML-SURVEY.md "As built: OC199-OC201"): a test whose bytecode alternative builds a helper
  # program of C (a C program, or an OCaml program with C stubs: readonly_files naming a .c file) before the test's own program
  # (setup-ocamlc.byte-build-env, program = NAME, all_modules = FILES, ocamlc.byte), then runs the test's program once or more
  # (arguments = ARGS, run, check-program-output). The run's environment names each helper (@helper=NAME:FILE,...; bin/oi's oi_one
  # builds it: a C program by ci, test/ocaml/capi/cnodes.ae --program, run by oi cmain on ci; an OCaml program by oi with its stubs on
  # ci), the runs beyond the first (@runs=ARGS,...) and the test's run hook (@hook=run: ocamltest runs TEST.run, the source
  # directory's, in place of the run action, its main.ml's install_hook). The first alternative is the run: the second
  # (setup-ocamlopt.byte-build-env) ends it. The AOT levels (aot=1) keep the class (their runtime has no C); a helper of OCaml
  # alone (OC195, undecided) stays its class too. Anything else in the alternative: the class it had (cls's roclass)
  helper = 0; roclass = ""; roseen = 0
  if (aot != 1) {
    hasbuild = 0; rofiles = ""
    for (j = 1; j <= m; j++) {
      s = trim(st[j])
      if (s == "setup-ocamlc.byte-build-env") hasbuild = 1
      if (s ~ /^readonly_files *= *"/) { rofiles = s; sub(/^[^"]*"/, "", rofiles); sub(/"[^"]*$/, "", rofiles); rofiles = trim(rofiles) }
    }
    if (hasbuild && rofiles ~ /^[A-Za-z0-9_]+\.(c|ml)( +[A-Za-z0-9_]+\.(c|ml))*$/ && rofiles ~ /\.c( |$)/) { helper = 1; roclass = "readonly_files = " rofiles }
  }
  hprog = ""; hmods = ""; hsetup = 0; hmain = 0; hargs = ""; nruns = 0; runs = ""; hbase = FILENAME; sub(/^.*\//, "", hbase); sub(/\.ml$/, "", hbase)
  for (j = 1; j <= m; j++) {
    s = trim(st[j]); if (s == "") continue
    if (helper) {
      if (s ~ /^readonly_files *= *"/) { roseen = 1; continue }
      if (s == "check-ocamlc.byte-output" || s == "check-program-output") continue
      if (s == "setup-ocamlc.byte-build-env") { if (hsetup) break; hsetup = 1; continue }
      if (s ~ /^setup-ocamlopt\.(byte|opt)-build-env$/) { if (hsetup) break; cls("") }
      if (s ~ /^(program|all_modules|arguments) *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v)
        if (s ~ /^program/) { hprog = v; sub(/^\$VAR\//, "", hprog) }
        else if (s ~ /^all_modules/) hmods = v
        else hargs = v
        continue
      }
      if (s == "ocamlc.byte") {
        if (!hsetup) cls("")
        c = split(hmods, hm, " "); cfiles = ""; mls = ""
        for (q = 1; q <= c; q++) { if (hm[q] ~ /\.c$/) cfiles = cfiles (cfiles == "" ? "" : ",") hm[q]; else if (hm[q] ~ /\.ml$/) mls = mls (mls == "" ? "" : " ") hm[q]; else cls("") }
        if (hprog == hbase ".byte") {
          # the test's own program: its own .ml alone, and its C stubs
          if (mls != hbase ".ml") cls("")
          if (cfiles != "") env = env " @cstubs=" cfiles
          hmain = 1
        } else {
          # a helper: C files and at most one .ml (an OCaml helper needs a C stub here: OC201's, not OC195's)
          if (hprog !~ /^[A-Za-z0-9_.]+$/ || cfiles == "" || mls ~ / /) cls("")
          env = env " @helper=" hprog ":" cfiles (mls == "" ? "" : "," mls)
        }
        continue
      }
      if (s == "run") {
        if (!hmain) cls("")
        nruns++
        if (nruns == 1) args = hargs
        else { if (hargs == "" || hargs ~ /[ ,]/) cls(""); runs = runs (runs == "" ? "" : ",") hargs }
        continue
      }
    }
    if (match(s, /^[a-z_]+ *\+?= *"/)) {
      key = s; sub(/ *\+?=.*/, "", key)
      val = s; sub(/^[^"]*"/, "", val); sub(/"[^"]*$/, "", val)
      if (key == "flags") {
        c = split(val, fl, " ")
        for (q = 1; q <= c; q++) {
          f = fl[q]
          if (f == "-w" || f == "-warn-error") { q++; continue }
          if (f ~ /^-w[+-]/ || f == "-g" || f == "-bin-annot" || f == "-bin-annot-occurrences" || f == "-S" || f == "-function-sections") continue
          if (f == "-inline") { q++; continue }
          # -I of the compiler's source directories (parsing/, toplevel/: their .cmi files): compiler-libs' units are on the path (OT21 (b))
          if (f == "-I" && fl[q + 1] ~ /^@OCAMLSRCDIR@\//) { q++; continue }
          if (f == "-unsafe" || f == "-rectypes" || f == "-no-strict-formats" || f == "-strict-formats" || f == "-principal" || f == "-strict-sequence" || f == "-nolabels" || f == "-nopervasives" || f == "-no-alias-deps" || f == "-safer-matching") { flags = flags " " f; continue }
          cls("flags " val)
        }
        continue
      }
      # ocamltest's precondition scripts that only check the platform, decided as ocamltest's run here decides them: the runtime does
      # not clean up at exit (no OCAMLRUNPARAM c=1) and the host is Linux, so both pass; has-execvpe skips where the C library has
      # execvpe (musl, glibc)
      if (key == "script" && val ~ /^sh @TSD@\/(test-runtime-cleanup|is-linux)\.sh$/) { prescript = 1; continue }
      # has-afunix.sh: Windows's AF_UNIX (lib-unix/win-socketpair, target-windows below it)
      if (key == "script" && val ~ /^sh @TSD@\/has-afunix\.sh$/) { cls("target-windows (has-afunix.sh)") }
      if (key == "script" && val ~ /^sh @TSD@\/has-execvpe\.sh$/) { cls("skip (has-execvpe.sh: the C library has execvpe)") }
      # the program's output into a file then compared, ocamltest's default made explicit (lib-threads/sieve)
      if (key == "output" && val == "$VAR/program-output") continue
      if (key == "stdout" && val == "$VAR") continue
      # the standard input empty and the standard error discarded (bin/oi's @stdin=null, @stderr=null in the run's environment field)
      if (key == "stdin" && val == "/dev/null") { env = env " @stdin=null"; continue }
      if (key == "stderr" && val == "/dev/null") { env = env " @stderr=null"; continue }
      if (key == "ocamlopt_flags" || key == "reason" || key == "exit_status" || key == "binary_modules") continue
      if (key == "arguments") { args = val; continue }
      # the program's own C stubs (the C API on ci, doc/ocaml/OCAML-SURVEY.md "As built: the C API on ci"): modules that are all C
      # files of the test's directory, built by ci beside the program (bin/oi's cstubs_nodes, the run's @cstubs=FILE,...); at the AOT
      # levels (-v aot=1) the class it was
      if (key == "modules" && aot != 1 && val ~ /^[A-Za-z0-9_.]+\.c( +[A-Za-z0-9_.]+\.c)*$/) { cs = val; csclass = key " = " val; gsub(/ +/, ",", cs); env = env " @cstubs=" cs; continue }
      if (key == "ocamlrunparam") { cls("ocamlrunparam " val) }
      cls(key " = " val)
    }
    if (s ~ /^include /) {
      lib = trim(substr(s, 9))
      if (lib == "testing") { incs = incs " testing"; continue }
      # the Unix library (OT9's second fix): its unit loaded with the program (bin/oi's inc_units)
      if (lib == "unix") { incs = incs " unix"; continue }
      # the threads library (OT9's fifth fix): thread.ml and event.ml loaded with unix's units
      if (lib == "systhreads") { incs = incs " unix systhreads"; continue }
      # compiler-libs (OT9's eighth fix): the units of utils/ bin/oi builds (COMPLIB_UNITS); a program that needs more (the parser, the typer)
      # fails at its link, in a class
      # the Str library (DEFERRED-TRIAGE §3 item 10): str.ml loaded with the program, its matcher oi's ext_str_re
      if (lib == "str") { incs = incs " str"; continue }
      if (lib == "config" || lib == "ocamlcommon") { if (incs !~ / complib/) incs = incs " complib"; continue }
      cls("include " lib)
    }
    if (s ~ /^set [A-Za-z_0-9]+ *= *"/) { e = s; sub(/^set /, "", e); gsub(/ *= */, "=", e); gsub(/"/, "", e); env = env " " e; continue }
    if (s ~ /^unset /) continue
    if (s == "script" && prescript) continue
    # a later alternative that skips (lib-threads' "{ include systhreads; hassysthreads; }{ reason = ...; skip; ... }"): the run is the
    # first alternative's, read up to here (OT9's fifth fix); a test whose only branch skips stays a class
    if (s == "skip" && incs != "") break
    if (s == "bytecode") sawbyte = 1
    if (s == "native") sawnative = 1
    if (s == "bytecode" || s == "native" || s == "no-tsan" || s == "not-target-windows" || s == "multicore" || s == "native-compiler" || s == "arch64" || s == "not-macos" || s == "no-flambda" || s == "linux" || s == "not-windows" || s == "not-bsd" || s == "not_macos_amd64_tsan" || s == "hasunix" || s == "hassysthreads" || s == "hasstr") continue
    cls(s)
  }
  if (helper) {
    if (!hmain || nruns == 0) cls("")
    if (runs != "") env = env " @runs=" runs
    hook = FILENAME; sub(/\.ml$/, ".run", hook)
    if ((getline hl < hook) >= 0) { close(hook); env = env " @hook=run" }
  }
  if (args ~ /\$VAR/) { cls("arguments = " args) }
  # a test of C stubs that runs only native (int64-unboxing: its externals' bytecode names are "", the unboxed native ones the C's)
  # stays its class: oi calls an external by its bytecode name
  if (csclass != "" && sawnative && !sawbyte) { cls(csclass) }
  print "run\t" trim(flags) "\t" trim(incs) "\t" args "\t" trim(env)
}
