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
  gsub(/\$\{[a-z_]*\}/, "$VAR", body)
  gsub(/[{}]/, ";", body)
  m = split(body, st, ";")
  flags = ""; incs = ""; args = ""; env = ""
  for (j = 1; j <= m; j++) {
    s = trim(st[j]); if (s == "") continue
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
      # ocamltest's precondition scripts that only check the platform, decided as ocamltest's run here decides them: the runtime does
      # not clean up at exit (no OCAMLRUNPARAM c=1) and the host is Linux, so both pass; has-execvpe skips where the C library has
      # execvpe (musl, glibc)
      if (key == "script" && val ~ /^sh @TSD@\/(test-runtime-cleanup|is-linux)\.sh$/) { prescript = 1; continue }
      # has-afunix.sh: Windows's AF_UNIX (lib-unix/win-socketpair, target-windows below it)
      if (key == "script" && val ~ /^sh @TSD@\/has-afunix\.sh$/) { print "class\theader: target-windows (has-afunix.sh)"; exit }
      if (key == "script" && val ~ /^sh @TSD@\/has-execvpe\.sh$/) { print "class\theader: skip (has-execvpe.sh: the C library has execvpe)"; exit }
      # the program's output into a file then compared, ocamltest's default made explicit (lib-threads/sieve)
      if (key == "output" && val == "$VAR/program-output") continue
      if (key == "stdout" && val == "$VAR") continue
      # the standard input empty and the standard error discarded (bin/oi's @stdin=null, @stderr=null in the run's environment field)
      if (key == "stdin" && val == "/dev/null") { env = env " @stdin=null"; continue }
      if (key == "stderr" && val == "/dev/null") { env = env " @stderr=null"; continue }
      if (key == "ocamlopt_flags" || key == "reason" || key == "exit_status" || key == "binary_modules") continue
      if (key == "arguments") { args = val; continue }
      if (key == "ocamlrunparam") { print "class\theader: ocamlrunparam " val; exit }
      print "class\theader: " key " = " val; exit
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
  if (args ~ /\$VAR/) { print "class\theader: arguments = " args; exit }
  print "run\t" trim(flags) "\t" trim(incs) "\t" args "\t" trim(env)
}
