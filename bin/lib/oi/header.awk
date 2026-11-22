# bin/oi's reading of an ocamltest header (testsuite/tests' "(* TEST ... *)", or the block after a
# "(* TEST_BELOW" placeholder): one line, "run<TAB>FLAGS<TAB>INCLUDES<TAB>ARGS<TAB>ENV" for a test oi
# runs as one program (doc/ocaml/OCAML-SURVEY.md, "As built: O5 (write-ahead)", OC61), else
# "class<TAB>REASON", REASON the first statement outside that set.
function trim(s) { gsub(/^[ \t\n]+|[ \t\n]+$/, "", s); return s }
# a class: REASON, or in helper mode (below) past its readonly_files statement the reason the test had before that mode, that statement
# a header that includes dynlink (OC220 (a)'s first part: Dynlink's unit at oi's level, no plugin loaded yet) runs only when the rest of its
# header runs too; a later class keeps the include's, the class of the plugins the test builds (:dynlink-decided) and of the debugger's
function cls(r) { if (helper && roseen) r = roclass; if (dlseen) r = "include dynlink"; print "class\theader: " r; exit }
# OC210 (a) (the user, 2026-11-10): ocamltest's expansion of a variable's value (environments.ml's expand, Buffer.add_substitute):
# $NAME, ${NAME} or $(NAME) replaced by the variable's value, itself expanded, an unset one by nothing; \$ a literal $. The
# variables are the header's `set NAME = "VALUE"` (setv); ${ocamlsrcdir} and the other builtins were replaced before (@OCAMLSRCDIR@,
# $VAR, which classes the arguments)
function expand(s, depth,   out, n, i, c, prev, j, ch, cl, k, name) {
  if (depth > 32) cls("arguments: a recursive variable definition")
  out = ""; prev = " "; n = length(s); i = 1
  while (i <= n) {
    c = substr(s, i, 1)
    if (c == "$" && prev == "\\") { out = out "$"; prev = " "; i++; continue }
    if (c == "$") {
      j = i + 1; ch = substr(s, j, 1)
      if (ch == "(" || ch == "{") {
        cl = (ch == "(") ? ")" : "}"; k = index(substr(s, j + 1), cl)
        if (k == 0) { out = out "$"; prev = " "; i = j; continue }
        name = substr(s, j + 1, k - 1); i = j + k + 1
      } else {
        k = j; while (k <= n && substr(s, k, 1) ~ /[A-Za-z0-9_]/) k++
        if (k == j) { out = out "$"; prev = " "; i = j; continue }
        name = substr(s, j, k - j); i = k
      }
      if (name in setv) out = out expand(setv[name], depth + 1)
      prev = " "; continue
    }
    if (prev == "\\") out = out "\\"
    if (c != "\\") out = out c
    prev = c; i++
  }
  if (prev == "\\") out = out "\\"
  return out
}
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
  # a header of one branch with ocamltest's skip action (builtin_actions.ml's skip, "Always skip the test": ephe-c-api/test) is
  # ocamltest's own skip at every level, whatever else the branch holds (round 54)
  if (body !~ /[{}]/) { nb = split(body, sk, ";"); for (q = 1; q <= nb; q++) if (trim(sk[q]) == "skip") cls("skip") }
  # ocamltest's ${ocamlsrcdir}, the compiler's source tree (OT21 (b)): the pinned tarball's, bin/oi corpus puts its path for @OCAMLSRCDIR@
  gsub(/\$\{ocamlsrcdir\}/, "@OCAMLSRCDIR@", body)
  gsub(/\$\{test_source_directory\}/, "@TSD@", body)
  gsub(/\$\{[a-z_]*\}/, "$VAR", body)
  # an alternative that requires flambda (its `flambda` action) and names its own reference is one ocamltest skips on the pinned
  # configuration (no flambda, test/ocaml/ocamltest-config.edn): dropped before the statements are read, so its reference
  # (backtrace/lazy's and backtrace_effects_nested's .flambda.reference) does not class the test, whose other alternatives run with
  # the plain one (OC217 (a); queue 461); a flambda alternative without a reference keeps its class (flambda/gpr2239: :config-skipped)
  nbody = ""; rest = body
  while (match(rest, /\{[^{}]*\}/)) {
    blk = substr(rest, RSTART, RLENGTH)
    nbody = nbody substr(rest, 1, RSTART - 1)
    if (blk !~ /[{; ]flambda *;/ || blk !~ /reference *=/) nbody = nbody blk
    rest = substr(rest, RSTART + RLENGTH)
  }
  body = nbody rest
  gsub(/[{}]/, ";", body)
  m = split(body, st, ";")
  flags = ""; incs = ""; args = ""; env = ""
  # helper mode (OC201 (a), doc/ocaml/OCAML-SURVEY.md "As built: OC199-OC201"): a test whose bytecode alternative builds a helper
  # program of C (a C program, or an OCaml program with C stubs: readonly_files naming a .c file) before the test's own program
  # (setup-ocamlc.byte-build-env, program = NAME, all_modules = FILES, ocamlc.byte), then runs the test's program once or more
  # (arguments = ARGS, run, check-program-output). The run's environment names each helper (@helper=NAME:FILE,...; bin/oi's oi_one
  # builds it: a C program by ci, test/ocaml/capi/cnodes.ae --program, run by oi cmain on ci; an OCaml program by oi, with its stubs on
  # ci when it has C files) and the runs beyond the first (@runs=ARGS,...). The first alternative is the run: the second
  # (setup-ocamlopt.byte-build-env) ends it. The AOT levels (aot=1) keep the class (their runtime has no C, and a helper is oi's).
  # OC204 (a) (OC195 (a), the user 2026-11-09): a helper of OCaml alone (myecho.ml, reflector.ml, cmdline_prog.ml) is one too.
  # OC206 (a) (the user 2026-11-09): at the AOT levels (aot=1) too when the helpers are OCaml alone (readonly .ml files, no C): bin/oi's
  # light level translates the helper as the test's program and runs it at that level, the compiled level builds it into the test's shard;
  # a C helper stays oi's (its runtime is oi cmain's), the class it had.
  # Anything else in the alternative: the class it had (cls's roclass)
  helper = 0; roclass = ""; roseen = 0
  hasbuild = 0; rofiles = ""
  for (j = 1; j <= m; j++) {
    s = trim(st[j])
    if (s == "setup-ocamlc.byte-build-env") hasbuild = 1
    if (s ~ /^readonly_files *= *"/) { rofiles = s; sub(/^[^"]*"/, "", rofiles); sub(/"[^"]*$/, "", rofiles); rofiles = trim(rofiles) }
  }
  if (aot == 0 && hasbuild && rofiles ~ /^[A-Za-z0-9_]+\.(c|ml)( +[A-Za-z0-9_]+\.(c|ml))*$/) { helper = 1; roclass = "readonly_files = " rofiles }
  if (aot >= 1 && hasbuild && rofiles ~ /^[A-Za-z0-9_]+\.ml( +[A-Za-z0-9_]+\.ml)*$/) { helper = 1; roclass = "readonly_files = " rofiles }
  # dynlink mode (OC220 (a)'s loader, doc/ocaml/OCAML-SURVEY.md "As built: OC220's loader"): a test at oi's level that includes dynlink and
  # whose bytecode alternative builds plugins: in its build environment (setup-ocamlc.byte-build-env) each `module = FILE; ocamlc.byte` a
  # unit compiled in the current directory (cwd = DIR; cd), one link `all_modules = "A.cmo ... MAIN.cmo"; ocamlc.byte` (module = "") the
  # main program, its last unit the test's own, libraries "" or "dynlink", then one run (exit_status the own verdict's), the native
  # alternative (native-dynlink, setup-ocamlopt.*) ending it. The run's environment: @dyn=DIR/FILE,... the units in order (bin/oi's
  # oi_dynlink types each .ml into Lambda and writes its .cmo, a plugin oi's Dynlink loads), @dynmain=DIR/UNIT,... the main program's,
  # @dyncopy=FILE,... the readonly files and subdirectories. Anything else: the class (include dynlink). The AOT levels too (queue 373:
  # the same units, the main program's translated by o2j; a plugin by the runtime's loader when Dynlink reads it at the light level, ahead
  # into the program's registry at the compiled level, 373-1)
  dlmode = 0
  if (hasbuild) for (j = 1; j <= m; j++) if (trim(st[j]) == "include dynlink") dlmode = 1
  # (a plugin test's readonly .ml files are its units, not helper programs)
  if (dlmode) helper = 0
  # depend mode (queue 478, DECISION 476-1 (a): ocamltest's tools as Arbace's own, judged by the test's own .reference): a test whose
  # bytecode alternative runs `ocamlc -depend ARGS` (commandline = "-depend ARGS"; ocamlc.byte) in its build environment, its readonly
  # files and subdirectories copied there, and compares the compiler's output with its own .reference (compiler_reference =
  # "${test_source_directory}/BASE.reference"; check-ocamlc.byte-output), the native alternative ending it. The run is bin/oi's
  # depend program (test/ocaml/depend/ocamldep.ml on compiler-libs' units, `bin/oi depend`) with ARGS in that directory: the row's
  # arguments, its libraries compiler-libs', its environment @depend=1 and @dyncopy=FILE,... (the light AOT level too; not the
  # compiled level, whose library has no compiler-libs run yet). Anything else in the alternative: the class
  dpmode = 0
  if (aot != 2 && hasbuild && !dlmode) for (j = 1; j <= m; j++) if (trim(st[j]) ~ /^commandline *= *"-depend /) dpmode = 1
  # (only a header whose statements are all of that set, to the native alternative: another keeps the class it had)
  if (dpmode) for (j = 1; j <= m; j++) {
    s = trim(st[j]); if (s == "") continue
    if (s ~ /^setup-ocamlopt\.(byte|opt)-build-env$/) break
    if (s !~ /^((readonly_files|subdirectories|commandline|compiler_reference) *= *"|(setup-ocamlc\.byte-build-env|ocamlc\.byte|check-ocamlc\.byte-output)$)/) { dpmode = 0; break }
  }
  if (dpmode) helper = 0
  # ocamldoc mode (queue 480, DECISION 478-2 (a): ocamltest's tools as Arbace's own, judged by the test's own .reference): a test whose
  # header is ocamltest's ocamldoc test alone (`ocamldoc;`, no backend: the custom generators of `plugins = "FILE.ml"`, built by ocamlc
  # against ocamldoc's interfaces, `flags = "-I ${ocamlsrcdir}/ocamldoc"` their compiler's) runs `ocamldoc -nostdlib -I STDLIB -g
  # FILE.cmo TEST.ml -o TEST.result` and compares TEST.result with TEST.reference and ocamldoc's output with TEST.ocamldoc.reference
  # (none: no output). The run is bin/oi's ocamldoc (OCaml 5.5.0's ocamldoc/ on compiler-libs' units, its plugins by oi's Dynlink): the
  # row's environment @ocamldoc=1 and @plugins=FILE,...; bin/oi's oi_one gives the row TEST.result as its
  # output, ocamldoc's own output against TEST.ocamldoc.reference its note; no libraries of the header's (the program's units are
  # ocamldoc's own list, bin/oi's ocamldoc_build). At oi's level alone: at the light AOT level (bin/oi's o2j_dump and translate loop have
  # the mode) t01 did not finish in 600 s of CPU with 7 GB resident (queue 480; DECISION 480-1), so the AOT levels keep the class
  odmode = 0; odplugins = ""
  if (aot == 0) {
    odmode = 1
    for (j = 1; j <= m; j++) {
      s = trim(st[j]); if (s == "") continue
      if (s == "ocamldoc") { odmode = (odmode == 1) ? 2 : 0; continue }
      if (s ~ /^plugins *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v); gsub(/[ \t]+/, " ", v)
        c = split(v, hm, " "); for (q = 1; q <= c; q++) { if (hm[q] !~ /^[A-Za-z0-9_]+\.ml$/) odmode = 0; odplugins = odplugins (odplugins == "" ? "" : ",") hm[q] }
        continue
      }
      if (s ~ /^flags *= *"-I @OCAMLSRCDIR@\/ocamldoc"$/) continue
      odmode = 0
    }
    odmode = (odmode == 2 && odplugins != "")
  }
  if (odmode) { helper = 0; env = env " @ocamldoc=1 @plugins=" odplugins; m = 0 }
  # objinfo mode (queue 511: ocamltest's tools as Arbace's own, judged by the test's own .reference): a test at oi's level whose header
  # is ocamltest's compile-only test of a unit's .cmt (compile_only = "true"; setup-ocamlc.byte-build-env; all_modules = "FILES";
  # ocamlc.byte; check-ocamlc.byte-output with flags = "FLAGS", readonly_files and modules copied) followed by `program = "ARGS";
  # output = "out_objinfo"; ocamlobjinfo; check-program-output`: the shape-index, uid-deps and uids tests. The run is bin/oi's oiobjinfo
  # (test/ocaml/objinfo/oiobjinfo.ml on compiler-libs' units, bin/lib/oi/tools.bash): `ocamlc -c FLAGS FILES` up to typing (the .cmi,
  # .cmt and .cmti written by Typemod), then ocamlobjinfo's .cmt printing with ARGS; the row's environment @tool=objinfo and
  # @dyncopy=FILE,..., its libraries compiler-libs'; the compiler's own output (ocamlc.out) judged beside it by bin/oi's tool_output. At
  # oi's level alone: the light AOT level does not load compiler-libs' typing units in its limit (queue 480's DECISION 480-1)
  oimode = 0
  if (aot == 0 && hasbuild && !dlmode && !dpmode && !odmode) {
    oimode = 1; oiflags = ""; oimods = ""; oicopy = ""; oiprog = ""; oicomp = 0; oiobj = 0; oico = 0
    for (j = 1; j <= m && oimode; j++) {
      s = trim(st[j]); if (s == "") continue
      v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v); gsub(/[ \t\\]+/, " ", v)
      if (s ~ /^flags *= *"/) { oiflags = v; continue }
      if (s ~ /^compile_only *= *"true"$/) { oico = 1; continue }
      if (s ~ /^(readonly_files|modules) *= *"/) { oicopy = oicopy " " v; continue }
      if (s ~ /^all_modules *= *"/) { oimods = v; oicopy = oicopy " " v; continue }
      if (s == "setup-ocamlc.byte-build-env" || s == "check-ocamlc.byte-output" || s == "check-program-output") continue
      if (s ~ /^output *= *"out_objinfo"$/) continue
      if (s == "ocamlc.byte") { if (oimods == "" || oiobj) oimode = 0; oicomp = 1; continue }
      if (s ~ /^program *= *"/) { if (!oicomp) oimode = 0; oiprog = v; continue }
      if (s == "ocamlobjinfo") { if (oiprog == "") oimode = 0; oiobj = 1; continue }
      oimode = 0
    }
    if (!(oico && oicomp && oiobj)) oimode = 0
    if (oimode) {
      c = split(oiflags " " oimods " " oiprog " " oicopy, hm, " ")
      for (q = 1; q <= c; q++) if (hm[q] !~ /^[A-Za-z0-9_.+-]+$/) oimode = 0
    }
  }
  if (oimode) {
    c = split(oicopy, hm, " "); oicp = ""; delete oiseen
    for (q = 1; q <= c; q++) if (!(hm[q] in oiseen)) { oiseen[hm[q]] = 1; oicp = oicp (oicp == "" ? "" : ",") hm[q] }
    helper = 0; m = 0; incs = " complib"
    args = trim("-c " oiflags " -nostdlib -I _stdlib " oimods " -objinfo " oiprog); gsub(/ +/, " ", args)
    env = env " @tool=objinfo @dyncopy=" oicp
  }
  psetup = 0; pargs = ""; pdone = 0; pchk = 0; pcopy = ""
  dsetup = 0; dcwd = ""; dnext = ""; dmod = ""; dmods = ""; dlibs = ""; dsteps = ""; dmain = ""; dlinked = 0; drun = 0; dcopy = ""
  hprog = ""; hmods = ""; hsetup = 0; hmain = 0; hargs = ""; nruns = 0; runs = ""; hbase = FILENAME; sub(/^.*\//, "", hbase); sub(/\.mll?$/, "", hbase)
  for (j = 1; j <= m; j++) {
    s = trim(st[j]); if (s == "") continue
    if (dpmode) {
      if (s ~ /^(readonly_files|subdirectories) *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v); gsub(/[ \t\\]+/, " ", v)
        c = split(v, hm, " "); for (q = 1; q <= c; q++) { if (hm[q] !~ /^[A-Za-z0-9_.]+$/) cls(""); pcopy = pcopy (pcopy == "" ? "" : ",") hm[q] }
        continue
      }
      if (s == "setup-ocamlc.byte-build-env") { if (psetup) break; psetup = 1; continue }
      if (s ~ /^setup-ocamlopt\.(byte|opt)-build-env$/) { if (pchk) break; cls(s) }
      if (s ~ /^commandline *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v)
        if (!psetup || pdone || v !~ /^-depend( |$)/) cls("commandline = " v)
        sub(/^-depend */, "", v); gsub(/[ \t]+/, " ", v)
        c = split(v, hm, " "); for (q = 1; q <= c; q++) if (hm[q] !~ /^[A-Za-z0-9_.\/+-]+$/) cls("commandline = -depend " v)
        pargs = v; continue
      }
      if (s == "ocamlc.byte") { if (!psetup || pargs == "" || pdone) cls(s); pdone = 1; continue }
      if (s ~ /^compiler_reference *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v)
        if (v != "@TSD@/" hbase ".reference") cls("compiler_reference = " v)
        continue
      }
      if (s == "check-ocamlc.byte-output") { if (!pdone) cls(s); pchk = 1; continue }
      cls(s)
    }
    if (dlmode) {
      if (s ~ /^(readonly_files|subdirectories) *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v); gsub(/[ \t\\]+/, " ", v)
        c = split(v, hm, " "); for (q = 1; q <= c; q++) { if (hm[q] !~ /^[A-Za-z0-9_.]+$/) cls(""); dcopy = dcopy (dcopy == "" ? "" : ",") hm[q] }
        continue
      }
      if (s == "shared-libraries" || s == "check-program-output") continue
      if (s == "setup-ocamlc.byte-build-env") { if (dsetup) break; dsetup = 1; dcwd = ""; continue }
      if (s == "native-dynlink" || s ~ /^setup-ocamlopt\.(byte|opt)-build-env$/) { if (dsetup && drun) break; cls(s) }
      if (s ~ /^(cwd|module|program|all_modules|libraries) *= *"/) {
        v = s; sub(/^[^"]*"/, "", v); sub(/"[^"]*$/, "", v); v = trim(v)
        if (s ~ /^cwd/) dnext = v
        else if (s ~ /^module/) dmod = v
        else if (s ~ /^all_modules/) dmods = v
        else if (s ~ /^libraries/) { if (v != "" && v != "dynlink") cls("libraries = " v); dlibs = v }
        continue
      }
      if (s == "cd") {
        if (dnext == "..") { if (dcwd == "") cls(""); sub(/\/?[^\/]*$/, "", dcwd) }
        else if (dnext != "." && dnext != "") { if (dnext !~ /^[A-Za-z0-9_]+$/) cls(""); dcwd = dcwd (dcwd == "" ? "" : "/") dnext }
        continue
      }
      if (s == "ocamlc.byte") {
        if (!dsetup || dlinked) cls("")
        if (dmod != "") {
          if (dmod !~ /^[A-Za-z0-9_]+\.mli?$/) cls("")
          dsteps = dsteps (dsteps == "" ? "" : ",") (dcwd == "" ? "" : dcwd "/") dmod
        } else {
          c = split(dmods, hm, " "); if (c == 0) cls("")
          for (q = 1; q <= c; q++) {
            if (hm[q] !~ /^[A-Za-z0-9_]+\.cmo$/) cls("")
            x = hm[q]; sub(/\.cmo$/, "", x); dmain = dmain (dmain == "" ? "" : ",") (dcwd == "" ? "" : dcwd "/") x
          }
          if (dcwd != "" || x != hbase) cls("")
          dlinked = 1
        }
        continue
      }
      if (s == "run") { if (!dlinked || drun) cls(""); drun = 1; continue }
    }
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
          # a helper: C files and at most one .ml, or one .ml alone (OC204 (a))
          if (hprog !~ /^[A-Za-z0-9_.]+$/ || (cfiles == "" && mls == "") || mls ~ / /) cls("")
          env = env " @helper=" hprog ":" cfiles (mls == "" ? "" : (cfiles == "" ? "" : ",") mls)
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
          # -g (OC217 (a)): oi's level keeps it, the typer's debugging events its backtraces read; the AOT levels have no
          # backtraces yet (their class)
          # -g at oi's level and at the light level (queue 370: o2j's shadow stack, Bt), not yet the compiled level
          if (f == "-g") { if (aot != 2) flags = flags " -g"; continue }
          if (f ~ /^-w[+-]/ || f == "-bin-annot" || f == "-bin-annot-occurrences" || f == "-S" || f == "-function-sections") continue
          if (f == "-inline") { q++; continue }
          # -I of the compiler's source directories (parsing/, toplevel/: their .cmi files): compiler-libs' units are on the path (OT21 (b))
          if (f == "-I" && fl[q + 1] ~ /^@OCAMLSRCDIR@\//) { q++; continue }
          # -runtime-variant=i (queue 516, DECISION 511-2 (a)): the instrumented runtime, whose allocation buckets oi's ring emits
          # (ext_runtime_events.go's reAlloc) at oi's level; the AOT levels keep the class
          if (f == "-runtime-variant=i" && aot == 0) continue
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
      # a main .mll's ocamllex action (OC197 (a)): bin/oi's mll_gen runs the native ocamllex with -q, the flags every such test sets
      if (key == "ocamllex_flags" && FILENAME ~ /\.mll$/ && trim(val) == "-q") continue
      # compiler_directory_suffix (OC217 (a)): a native variant's build directory (inline_test's -O3 block), not the bytecode run's
      if (key == "compiler_directory_suffix") continue
      if (key == "ocamlopt_flags" || key == "reason" || key == "exit_status" || key == "binary_modules") continue
      if (key == "arguments") { args = val; continue }
      # the program's own C stubs (the C API on ci, doc/ocaml/OCAML-SURVEY.md "As built: the C API on ci"): modules that are all C
      # files of the test's directory, built by ci beside the program (bin/oi's cstubs_nodes, the run's @cstubs=FILE,...); at the
      # compiled level (-v aot=2, OC215 (a)) the C API of its main (test/ocaml/o2j/capi); at the light level (-v aot=1, queue 413) the
      # driver's `oi capi`, which the runtime spawns (OI_CAPI_SERVER)
      if (key == "modules" && val ~ /^[A-Za-z0-9_.]+\.c( +[A-Za-z0-9_.]+\.c)*$/) { cs = val; csclass = key " = " val; gsub(/ +/, ",", cs); env = env " @cstubs=" cs; continue }
      # (queue 523) C files and OCaml units of the test's directory (gc-roots: globrootsprim.c globroots.ml): at oi's level the units
      # typed first, in their order, each before the program (bin/oi's oi_one, @mods=FILE,...), the C files its stubs; the AOT levels keep
      # the class
      if (key == "modules" && aot == 0 && val ~ /^[A-Za-z0-9_.]+\.(c|ml)( +[A-Za-z0-9_.]+\.(c|ml))*$/ && val ~ /\.c( |$)/ && val ~ /\.ml( |$)/) {
        c = split(val, hm, " "); cs = ""; mls = ""
        for (q = 1; q <= c; q++) { if (hm[q] ~ /\.c$/) cs = cs (cs == "" ? "" : ",") hm[q]; else mls = mls (mls == "" ? "" : ",") hm[q] }
        csclass = key " = " val; env = env " @cstubs=" cs " @mods=" mls; continue
      }
      # OCAMLRUNPARAM's d (OC193 (a), the user 2026-11-09): oi and the AOT runtime read the domains' limit, so a header that sets d
      # alone (parallel/max_domains1, max_domains2) runs with it in the environment (ocamltest's ocamlrunparam variable, OCAMLRUNPARAM's
      # value: its leading comma an ignored option); l, the stack's limit, likewise (OC216 (a), the user 2026-11-10: oi's and the AOT
      # runtime's call depth); the other parameters (b=, ...) keep the class
      if (key == "ocamlrunparam" && val ~ /^,?[dl]=[0-9]+$/) { env = env " OCAMLRUNPARAM=" val; continue }
      # b, the backtraces' recording (OC217 (a)): oi's level records them (its backtrace stack over the typer's debugging events)
      # (queue 370: at the light level too, Bt's backtraces; not yet the compiled level)
      if (key == "ocamlrunparam" && val ~ /^,?b=[0-9]+$/ && aot != 2) { env = env " OCAMLRUNPARAM=" val; continue }
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
      # the Runtime_events library (OC219 (a)): runtime_events.ml loaded with the program, its externals oi's ext_runtime_events
      if (lib == "runtime_events") { incs = incs " runtime_events"; continue }
      # the Dynlink library (OC220 (a)'s first part): its units loaded with the program at oi's level (the platform test/ocaml/dynlink's,
      # the main program's units oi's unit table); the AOT levels keep the class until the runtime has the unit table
      # (queue 355: the light level's too, the runtime's Main/units; queue 373-1: the compiled level's, its library in o2j clib)
      if (lib == "dynlink") { incs = incs " dynlink"; dlseen = 1; continue }
      if (lib == "config" || lib == "ocamlcommon") { if (incs !~ / complib/) incs = incs " complib"; continue }
      cls("include " lib)
    }
    if (s ~ /^set [A-Za-z_0-9]+ *= *"/) {
      sv = s; sub(/^set /, "", sv); sn = sv; sub(/ *=.*/, "", sn); sub(/^[^"]*"/, "", sv); sub(/"[^"]*$/, "", sv); setv[sn] = sv
      # its value in the environment expanded as ocamltest exports it (to_system_env), at the end (a later set counts): a token here
      if (sv ~ /[$\\]/ && sv !~ /\$VAR/) { nexp++; expname[nexp] = sn; env = env " @@SET" nexp "@@"; continue }
      e = s; sub(/^set /, "", e); gsub(/ *= */, "=", e); gsub(/"/, "", e); env = env " " e; continue
    }
    if (s ~ /^unset /) continue
    if (s == "script" && prescript) continue
    # a later alternative that skips (lib-threads' "{ include systhreads; hassysthreads; }{ reason = ...; skip; ... }"): the run is the
    # first alternative's, read up to here (OT9's fifth fix); a test whose only branch skips stays a class
    if (s == "skip" && incs != "") break
    # instrumented-runtime (queue 516): the pinned host has libcamlruni.a, so ocamltest runs the test; oi's level takes it
    if (s == "instrumented-runtime" && aot == 0) continue
    if (s == "bytecode") sawbyte = 1
    if (s == "native") sawnative = 1
    if (s == "bytecode" || s == "native" || s == "no-tsan" || s == "not-target-windows" || s == "multicore" || s == "native-compiler" || s == "arch64" || s == "not-macos" || s == "no-flambda" || s == "linux" || s == "not-windows" || s == "not-bsd" || s == "not_macos_amd64_tsan" || s == "hasunix" || s == "hassysthreads" || s == "hasstr") continue
    cls(s)
  }
  if (dlmode) {
    if (!drun) cls("")
    env = env " @dyn=" dsteps " @dynmain=" dmain (dcopy == "" ? "" : " @dyncopy=" dcopy)
  }
  if (dpmode) {
    if (!pchk) cls("")
    args = pargs; incs = " complib"; env = env " @depend=1" (pcopy == "" ? "" : " @dyncopy=" pcopy)
  }
  if (helper) {
    if (!hmain || nruns == 0) cls("")
    if (runs != "") env = env " @runs=" runs
  }
  # ocamltest's hooks (its main.ml's install_hook; OC203 (a), the user 2026-11-09): for each action the test uses, the source
  # directory's TEST.ACTION, when it exists, runs in place of the action's body (actions.ml's run). A run test uses run and
  # check-program-output, so TEST.run (@hook=run: swapchan's and tls's sort, delayintr's SIGINT) and TEST.check-program-output
  # (@check=check-program-output: signal's grep) are its hooks; bin/oi's runners run them as actions_helpers.ml's run_hook does
  hook = FILENAME; sub(/\.mll?$/, ".run", hook)
  if ((getline hl < hook) >= 0) { close(hook); env = env " @hook=run" }
  hook = FILENAME; sub(/\.mll?$/, ".check-program-output", hook)
  if ((getline hl < hook) >= 0) { close(hook); env = env " @check=check-program-output" }
  # ocamltest exports its variables to the program's environment (environments.ml's to_system_env; queue 515): a program that reads
  # ocamlsrcdir or ocamlrun (warnings/mnemonics, which runs `${ocamlrun} ${ocamlsrcdir}/ocamlc -warn-help`) gets ocamlsrcdir as the
  # pinned tarball's root and ocamlrun as bin/lib/oi/ocamlrun (Arbace's ocamlc -warn-help on oi), at every level
  if (index(text, "Sys.getenv \"ocamlsrcdir\"")) env = env " ocamlsrcdir=@OCAMLSRCDIR@"
  if (index(text, "Sys.getenv \"ocamlrun\"")) env = env " ocamlrun=@OCAMLSRCDIR@/../../../bin/lib/oi/ocamlrun"
  if (args ~ /\$VAR/) { cls("arguments = " args) }
  for (q = 1; q <= nexp; q++) { ev = expand(setv[expname[q]], 0); gsub(/[ \t]/, "", ev); sub("@@SET" q "@@", expname[q] "=" ev, env) }
  # the arguments as ocamltest gives them (OC210 (a)): expanded, then split into words (actions_helpers.ml's String.words)
  if (index(args, "$") || index(args, "\\")) { args = expand(args, 0); gsub(/[ \t]+/, " ", args); args = trim(args) }
  # a test of C stubs that runs only native (int64-unboxing: its externals' bytecode names are "", the unboxed native ones the C's)
  # stays its class: oi calls an external by its bytecode name
  if (csclass != "" && sawnative && !sawbyte) { cls(csclass) }
  print "run\t" trim(flags) "\t" trim(incs) "\t" args "\t" trim(env)
}
