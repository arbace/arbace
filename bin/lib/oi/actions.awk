# bin/oi suite's reading of an ocamltest header (O7, OC100): header.awk's, plus "modules" of .ml and .mli files
# (compiled and linked before the test file, in the order listed: ocamltest's default for a run test) and
# "readonly_files" (copied, no action of their own), the run line's sixth field the modules.
# (header.awk's text follows.)
# bin/oi's reading of an ocamltest header (testsuite/tests' "(* TEST ... *)", or the block after a
# "(* TEST_BELOW" placeholder): one line, "run<TAB>FLAGS<TAB>INCLUDES<TAB>ARGS<TAB>ENV" for a test oi
# runs as one program (doc/ocaml/OCAML-SURVEY.md, "As built: O5 (write-ahead)", OC61), else
# "class<TAB>REASON", REASON the first statement outside that set.
function trim(s) { gsub(/^[ \t\n]+|[ \t\n]+$/, "", s); return s }
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
  # ocamltest's variables (${test_build_directory}) are not blocks
  gsub(/\$\{[a-z_]*\}/, "$VAR", body)
  gsub(/[{}]/, ";", body)
  m = split(body, st, ";")
  flags = ""; incs = ""; args = ""; env = ""; mods = ""
  build = 0
  for (j = 1; j <= m; j++) {
    s = trim(st[j]); if (s == "") continue
    # ocamltest's explicit build steps (OT9's third fix: badly-ordered-deps/coccinelle, required-external/main): from
    # setup-ocamlc.byte-build-env to its check-program-output, the modules compiled one by one, a library (.cma) made of some, the
    # program linked from all_modules and run; the run's units are the link's in order, a library's expanded to its modules (the test
    # file last, as every run's). The other branches (ocamlopt's) are not read; a pack (-pack) stays a class.
    if (build) {
      if (match(s, /^[a-z_]+ *= *"/)) {
        key = s; sub(/ *=.*/, "", key)
        val = s; sub(/^[^"]*"/, "", val); sub(/"[^"]*$/, "", val)
        if (key == "module") { bmod = val; continue }
        if (key == "program") { bprog = val; continue }
        if (key == "all_modules") { ball = val; continue }
        if (key == "flags") { if (val ~ /(^| )-pack( |$)/) { print "class\theader: flags " val; exit }; continue }
        print "class\theader: " key " = " val; exit
      }
      if (s == "ocamlc.byte") { if (bmod == "") { if (bprog ~ /\.cma$/) cma[bprog] = ball; else { blink = ball; nlink++ } }; continue }
      if (s == "check-ocamlc.byte-output") continue
      if (s == "run") { bran = 1; continue }
      if (s == "check-program-output") {
        # a second executable the test runs (OT3 (a), by design: its subprocess) stays a class
        if (!bran || blink == "" || nlink > 1) { print "class\theader: setup-ocamlc.byte-build-env"; exit }
        self = FILENAME; sub(/.*\//, "", self); sub(/\.ml$/, "", self)
        mods = ""; c = split(blink, lk, " ")
        for (q = 1; q <= c; q++) {
          if (lk[q] in cma) { e = split(cma[lk[q]], ex, " ") } else { e = 1; ex[1] = lk[q] }
          for (r = 1; r <= e; r++) {
            b = ex[r]; sub(/\.(cmo|ml)$/, "", b)
            if (b != self) mods = mods (mods == "" ? "" : " ") b ".ml"
          }
        }
        bdone = 1
        break
      }
      print "class\theader: " s; exit
    }
    if (s == "setup-ocamlc.byte-build-env") { build = 1; bmod = ""; bprog = ""; ball = ""; blink = ""; bran = 0; nlink = 0; continue }
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
          print "class\theader: flags " val; exit
        }
        continue
      }
      if (key == "modules") {
        c = split(val, ml, " ")
        for (q = 1; q <= c; q++) if (ml[q] !~ /^[A-Za-z_0-9]+\.mli?$/) { print "class\theader: modules = " val; exit }
        mods = val; continue
      }
      if (key == "readonly_files") {
        c = split(val, ml, " ")
        # a data file of any plain name (OT9's third fix: basic-io-2's test-file-short-lines); a C file stays a class
        for (q = 1; q <= c; q++) if (ml[q] !~ /^[A-Za-z_0-9.-]+$/ || ml[q] ~ /\.c$/) { print "class\theader: readonly_files = " val; exit }
        continue
      }
      # ocamltest's precondition scripts that only check the platform, decided as ocamltest's run here decides them: the runtime does
      # not clean up at exit (no OCAMLRUNPARAM c=1) and the host is Linux, so both pass; has-execvpe skips where the C library has
      # execvpe (musl, glibc)
      if (key == "script" && val ~ /^sh @TSD@\/(test-runtime-cleanup|is-linux)\.sh$/) { prescript = 1; continue }
      if (key == "script" && val ~ /^sh @TSD@\/has-execvpe\.sh$/) { print "class\theader: skip (has-execvpe.sh: the C library has execvpe)"; exit }
      # the program's output into a file then compared, ocamltest's default made explicit (lib-threads/sieve)
      if (key == "output" && val == "$VAR/program-output") continue
      if (key == "stdout" && val == "$VAR") continue
      # the standard input empty and the standard error discarded (bin/oi's @stdin=null, @stderr=null in the run's environment field)
      if (key == "stdin" && val == "/dev/null") { env = env " @stdin=null"; continue }
      if (key == "stderr" && val == "/dev/null") { env = env " @stderr=null"; continue }
      if (key == "ocamlopt_flags" || key == "reason" || key == "exit_status") continue
      if (key == "program" && trim(st[j + 1]) == "setup-ocamlc.byte-build-env") continue
      if (key == "arguments") { args = val; continue }
      if (key == "ocamlrunparam") { print "class\theader: ocamlrunparam " val; exit }
      print "class\theader: " key " = " val; exit
    }
    if (s ~ /^include /) {
      lib = trim(substr(s, 9))
      if (lib == "testing") { incs = incs " testing"; continue }
      # the Unix and threads libraries (OT9's second and fifth fixes), as header.awk's
      if (lib == "unix") { incs = incs " unix"; continue }
      if (lib == "systhreads") { incs = incs " unix systhreads"; continue }
      # compiler-libs (OT9's eighth fix, OT21 (b)), as header.awk's
      # the Str library (DEFERRED-TRIAGE §3 item 10): str.ml loaded with the program, its matcher oi's ext_str_re
      if (lib == "str") { incs = incs " str"; continue }
      if (lib == "config" || lib == "ocamlcommon") { if (incs !~ / complib/) incs = incs " complib"; continue }
      print "class\theader: include " lib; exit
    }
    if (s ~ /^set [A-Za-z_0-9]+ *= *"/) { e = s; sub(/^set /, "", e); gsub(/ *= */, "=", e); gsub(/"/, "", e); env = env " " e; continue }
    if (s ~ /^unset /) continue
    if (s == "script" && prescript) continue
    # a later alternative that skips (lib-threads' "{ include systhreads; hassysthreads; }{ reason = ...; skip; ... }"): the run is the
    # first alternative's, read up to here (OT9's fifth fix); a test whose only branch skips stays a class
    if (s == "skip" && incs != "") break
    if (s == "bytecode" || s == "native" || s == "no-tsan" || s == "not-target-windows" || s == "multicore" || s == "native-compiler" || s == "arch64" || s == "not-macos" || s == "no-flambda" || s == "linux" || s == "not-windows" || s == "not-bsd" || s == "not_macos_amd64_tsan" || s == "hasunix" || s == "hassysthreads" || s == "hasstr") continue
    print "class\theader: " s; exit
  }
  # explicit build steps that do not end in a run of the program (the compiler's own checks), and arguments naming ocamltest's directories
  if (build && !bdone) { print "class\theader: setup-ocamlc.byte-build-env"; exit }
  if (args ~ /\$VAR/) { print "class\theader: arguments = " args; exit }
  print "run\t" trim(flags) "\t" trim(incs) "\t" args "\t" trim(env) "\t" mods
}
