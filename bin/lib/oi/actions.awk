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
  gsub(/[{}]/, ";", body)
  m = split(body, st, ";")
  flags = ""; incs = ""; args = ""; env = ""; mods = ""
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
      if (key == "ocamlopt_flags" || key == "reason" || key == "exit_status") continue
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
      print "class\theader: include " lib; exit
    }
    if (s ~ /^set [A-Za-z_0-9]+ *= *"/) { e = s; sub(/^set /, "", e); gsub(/ *= */, "=", e); gsub(/"/, "", e); env = env " " e; continue }
    if (s ~ /^unset /) continue
    # a later alternative that skips (lib-threads' "{ include systhreads; hassysthreads; }{ reason = ...; skip; ... }"): the run is the
    # first alternative's, read up to here (OT9's fifth fix); a test whose only branch skips stays a class
    if (s == "skip" && incs != "") break
    if (s == "bytecode" || s == "native" || s == "no-tsan" || s == "not-target-windows" || s == "multicore" || s == "native-compiler" || s == "arch64" || s == "not-macos" || s == "no-flambda" || s == "linux" || s == "not-windows" || s == "not-bsd" || s == "hasunix" || s == "hassysthreads") continue
    print "class\theader: " s; exit
  }
  print "run\t" trim(flags) "\t" trim(incs) "\t" args "\t" trim(env) "\t" mods
}
