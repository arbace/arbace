# wai's js/wasm corpus on the headless world (doc/wasm/WAI.md §10; doc/wasm/WEB.md; WA11, WA12 begun; WAS13, WAS15-WAS17 (a)),
# sourced by bin/wai: Go programs built for GOOS=js GOARCH=wasm by TamaGo's go (not a host toolchain) and run by wai's light
# driver on gojs's headless world (`waidriver gojs`), each judged by its own statement:
#   - syscall/js's tests (`go test -c syscall/js`, run with -test.v as go_js_wasm_exec runs a test binary): status 0, the final
#     PASS, and a row a test by its --- PASS/FAIL/SKIP line;
#   - Go's own programs of test/wasm/js-run.txt (test/wasm/js/NAME/main.go): standard output against want.out byte for byte, the
#     status against want.status (0 when none), standard error empty or its first line want.err; NAME/args the driver's options
#     and the program's arguments (SCRATCH a fresh directory);
#   - the demo scene (wasm/demo, built for js/wasm): its frames on the page's world (--frames, --input)
#     against test/wasm/js/scene/expected/frames.edn (the authors' statements) and digests.edn (a run's), test/wasm/frames.ae;
#   - the shim's checks: wasm/web/arbace.js's gojs import names equal to the pinned Go's //go:wasmimport gojs directives (with
#     wasm_exec.js's debug), to wasm_exec.js's and to the headless world's (WAS15 (a)); its WebGPU bridge's operation table equal to
#     the world's and to the Go side's constants (doc/wasm/WEB.md §2).
# Rows [js/ID RESULT REASON] against test/wasm/js-results.edn (judge.ae, kind js), classes test/wasm/divergences-js.edn, the
# negative controls of test/wasm/controls-js.txt. Expects bin/wai's root, main, goroot, wai_exe, light, judge.

jsrun_dir=$main/.tmp/wai-go/js
jsrun_results=$root/test/wasm/js-results.edn
jsrun_classes=$root/test/wasm/divergences-js.edn
jsrun_controls=$root/test/wasm/controls-js.txt

jsrun_env() {
  echo GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 GOOS=js GOARCH=wasm \
    GOCACHE="$main/.tmp/wai-go/gocache-js" GOMAXPROCS=${GOMAXPROCS:-4} GOMEMLIMIT=${JS_GO_GOMEMLIMIT:-1200MiB}
}

jsrun_key() { { head -1 "$goroot/VERSION"; cat "$@"; } | sha256sum | cut -c1-24; }

# jsrun_build_own NAME: test/wasm/js/NAME/main.go built in GOPATH mode, cached by its source's and the toolchain's hash; echoes the
# module's path
jsrun_build_own() {
  local d=$root/test/wasm/js/$1 key out
  key=$(jsrun_key "$d/main.go")
  out=$jsrun_dir/own/$1-$key.wasm
  if [ ! -f "$out" ]; then
    mkdir -p "$jsrun_dir/own"
    (cd "$d" && env $(jsrun_env) GO111MODULE=off "$goroot/bin/go" build -trimpath -o "$out.tmp" main.go) > "$jsrun_dir/own/$1.log" 2>&1 \
      || { echo "build failed: $(head -1 "$jsrun_dir/own/$1.log" | cut -c1-200)"; return 1; }
    mv -f "$out.tmp" "$out"
  fi
  echo "$out"
}

# jsrun_build_test: syscall/js's test binary, cached by the package's files' and the toolchain's hash
jsrun_build_test() {
  local key out
  key=$(jsrun_key "$goroot"/src/syscall/js/*.go "$goroot"/src/syscall/js/*.s)
  out=$jsrun_dir/syscall_js-$key.wasm
  if [ ! -f "$out" ]; then
    mkdir -p "$jsrun_dir"
    (cd "$goroot/src" && env $(jsrun_env) "$goroot/bin/go" test -c -trimpath -o "$out.tmp" syscall/js) > "$jsrun_dir/syscall_js.log" 2>&1 \
      || { echo "build failed: $(head -1 "$jsrun_dir/syscall_js.log" | cut -c1-200)"; return 1; }
    mv -f "$out.tmp" "$out"
  fi
  echo "$out"
}

# jsrun_build_scene: the demo (wasm/demo: the module arbace, packages arbace/webgpu and arbace/scene; Go text until repr/go reads
# js/wasm, WAS46) built by TamaGo's go for js/wasm; cached by its files' and the toolchain's hash
jsrun_build_scene() {
  local key out
  key=$( (cd "$root" && find wasm/demo -type f | LC_ALL=C sort | xargs sha256sum; head -1 "$goroot/VERSION") | sha256sum | cut -c1-24)
  out=$jsrun_dir/scene-$key.wasm
  if [ ! -f "$out" ]; then
    mkdir -p "$jsrun_dir"
    (cd "$root/wasm/demo" && env $(jsrun_env) GO111MODULE=on GOWORK=off "$goroot/bin/go" build -trimpath -o "$out.tmp" ./scene/) \
      > "$jsrun_dir/scene-build.log" 2>&1 || { echo "build failed: $(head -1 "$jsrun_dir/scene-build.log" | cut -c1-200)"; return 1; }
    mv -f "$out.tmp" "$out"
  fi
  echo "$out"
}

# jsrun_gojs OUTDIR MODULE OPTIONS-AND-ARGS...: one run on the headless world: OUTDIR/{out,err,status}
jsrun_gojs() {
  local o=$1; shift
  local st=0
  GOMEMLIMIT=${WAI_GOMEMLIMIT:-1200MiB} timeout "${WAI_JS_TIMEOUT:-600}" "$wai_exe" gojs "$@" > "$o/out" 2> "$o/err" < "$jsrun_dir/empty" || st=$?
  echo $st > "$o/status"
}

# jsrun_own NAME [WANT-DIR]: a program's run judged against its statement (WANT-DIR: another program's, for the controls); a row
jsrun_own() {
  local n=$1 wd=$root/test/wasm/js/${2:-$1} exe o opts=() a st want
  exe=$(jsrun_build_own "$n") || { printf 'js/%s\tfail\t%s\n' "$n" "$exe"; return; }
  o=$jsrun_dir/run/$n; rm -rf "$o"; mkdir -p "$o/scratch"
  if [ -f "$root/test/wasm/js/$n/args" ]; then
    for a in $(cat "$root/test/wasm/js/$n/args"); do opts+=("${a//SCRATCH/$o/scratch}"); done
  fi
  local pre=() post=()
  while [ ${#opts[@]} -gt 0 ] && [ "${opts[0]#--}" != "${opts[0]}" ]; do pre+=("${opts[0]}" "${opts[1]}"); opts=("${opts[@]:2}"); done
  post=(${opts[@]+"${opts[@]}"})
  jsrun_gojs "$o" ${pre[@]+"${pre[@]}"} "$exe" ${post[@]+"${post[@]}"}
  st=$(cat "$o/status")
  want=0; [ -f "$wd/want.status" ] && want=$(cat "$wd/want.status")
  if [ "$st" != "$want" ]; then
    printf 'js/%s\tfail\tstatus %s, stated %s: %s\n' "$n" "$st" "$want" "$(grep -m1 . "$o/err" | cut -c1-160 | tr '\t"\\' '  /')"; return
  fi
  if ! cmp -s "$o/out" "$wd/want.out"; then
    printf 'js/%s\tfail\tstandard output differs from want.out at line %s\n' "$n" "$(cmp "$o/out" "$wd/want.out" 2>&1 | sed -n 's/.*line \([0-9]*\).*/\1/p')"; return
  fi
  if [ -f "$wd/want.err" ]; then
    [ "$(head -1 "$o/err")" = "$(head -1 "$wd/want.err")" ] || { printf 'js/%s\tfail\tstandard error'"'"'s first line differs from want.err\n' "$n"; return; }
  elif [ -s "$o/err" ]; then
    printf 'js/%s\tfail\tstandard error not empty: %s\n' "$n" "$(head -1 "$o/err" | cut -c1-160 | tr '\t"\\' '  /')"; return
  fi
  printf 'js/%s\tpass\t\n' "$n"
}

# jsrun_test: syscall/js's tests, a row each and one for the binary
jsrun_test() {
  local exe o st
  exe=$(jsrun_build_test) || { printf 'js/syscall_js\tfail\t%s\n' "$exe"; return; }
  o=$jsrun_dir/run/syscall_js; rm -rf "$o"; mkdir -p "$o"
  jsrun_gojs "$o" "$exe" -test.v -test.count=1
  st=$(cat "$o/status")
  awk '/^(=== RUN|--- (PASS|FAIL|SKIP)): Test/ { }
       /^--- PASS: Test/ { printf "js/syscall_js/%s\tpass\t\n", $3 }
       /^--- SKIP: Test/ { printf "js/syscall_js/%s\tskip\tskipped by the test\n", $3 }
       /^--- FAIL: Test/ { printf "js/syscall_js/%s\tfail\tthe test failed\n", $3 }' "$o/out"
  if [ "$st" = 0 ] && [ "$(tail -1 "$o/out")" = PASS ]; then
    printf 'js/syscall_js\tpass\t\n'
  else
    printf 'js/syscall_js\tfail\tstatus %s, last line %s; %s\n' "$st" "$(tail -1 "$o/out" | cut -c1-80 | tr '\t"\\' '  /')" "$(grep -m1 . "$o/err" | cut -c1-120 | tr '\t"\\' '  /')"
  fi
}

# jsrun_scene [INPUT [SHIFT [RECORD]]]: the demo's frames, judged by test/wasm/frames.ae; a row (and the judge's lines on stderr)
jsrun_scene() {
  local input=${1:-input.txt} shift=${2:-0} record=${3:-} d=$root/test/wasm/js/scene exe o frames st
  exe=$(jsrun_build_scene) || { printf 'js/scene\tfail\t%s\n' "$exe"; return; }
  o=$jsrun_dir/run/scene-${input%.txt}-$shift; rm -rf "$o"; mkdir -p "$o"
  frames=$(sed -n 's/^{:frames \([0-9]*\).*/\1/p' "$d/expected/frames.edn")
  jsrun_gojs "$o" --frames "$frames" --input "$d/$input" --record "$o/frames.edn" "$exe"
  st=$(cat "$o/status")
  [ "$st" = 0 ] || { printf 'js/scene\tfail\tstatus %s: %s\n' "$st" "$(grep -m1 . "$o/err" | cut -c1-160 | tr '\t"\\' '  /')"; return; }
  cmp -s "$o/out" "$d/want.out" || { printf 'js/scene\tfail\tstandard output differs from want.out\n'; return; }
  if arbace_tool -e "(do (load-file \"$root/test/wasm/frames.ae\") nil)" \
       -e "(wasm.frames/main \"$o/frames.edn\" \"$d/expected/frames.edn\" \"$d/expected/digests.edn\" \"$shift\" \"$record\")" > "$o/judge" 2>&1; then
    printf 'js/scene\tpass\t\n'
  else
    printf 'js/scene\tfail\tframes: %s\n' "$(grep -m1 'frames: ' "$o/judge" | cut -c1-200 | tr '\t"\\' '  /')"
  fi
  sed 's/^/  /' "$o/judge" >&2
}

# the shim's import names (SHIM: arbace.js or a copy): the keys of its gojs object
# (the keys spelled as quoted strings with a function of the stack pointer: only the gojs object's, in both files)
jsrun_shim_names() { sed -n 's/^[[:space:]]*"\([^"]*\)": (.*/\1/p' "$1" | LC_ALL=C sort -u; }

# jsrun_shim_check [SHIM]: a row: the four lists of gojs's import names equal
jsrun_shim_check() {
  local shim=${1:-$root/wasm/web/arbace.js} t=$jsrun_dir/shim
  mkdir -p "$t"
  { grep -rh -A1 '^//go:wasmimport gojs ' "$goroot/src" --include='*.go' | sed -n 's|^//go:wasmimport gojs ||p'; echo debug; } | LC_ALL=C sort -u > "$t/go.txt"
  jsrun_shim_names "$goroot/lib/wasm/wasm_exec.js" > "$t/wasm_exec.txt"
  jsrun_shim_names "$shim" > "$t/shim.txt"
  "$wai_exe" gojs-imports | LC_ALL=C sort -u > "$t/wai.txt"
  local n
  n=$(wc -l < "$t/go.txt")
  if [ "$n" -lt 20 ]; then printf 'js/shim/imports\tfail\tonly %s names read from Go\n' "$n"; return; fi
  for x in wasm_exec shim wai; do
    cmp -s "$t/go.txt" "$t/$x.txt" || { printf 'js/shim/imports\tfail\t%s names differ from Go'"'"'s: %s\n' "$x" "$(diff "$t/go.txt" "$t/$x.txt" | grep '^[<>]' | head -3 | tr '\n' ' ')"; return; }
  done
  printf 'js/shim/imports\tpass\t\n'
}

# jsrun_ops_check [SHIM]: a row: the bridge's operation table (arbace.js's nargs, the world's GPUOps, the Go side's constants)
jsrun_ops_check() {
  local shim=${1:-$root/wasm/web/arbace.js} t=$jsrun_dir/shim a b
  mkdir -p "$t"
  a=$(sed -n 's/^const nargs = \[\(.*\)\];$/\1/p' "$shim" | tr -d ' ')
  b=$("$wai_exe" gpu-ops | awk '{print $2}' | paste -sd, -)
  if [ -z "$a" ] || [ "$a" != "$b" ]; then printf 'js/shim/gpu-ops\tfail\tarbace.js'"'"'s nargs %s, the world'"'"'s %s\n' "$a" "$b"; return; fi
  printf 'js/shim/gpu-ops\tpass\t\n'
}

jsrun_check() {
  local record=$1 list=${2:-$root/test/wasm/js-run.txt} work=$jsrun_dir/check fails=0 n
  light
  mkdir -p "$jsrun_dir/run" "$work"; : > "$jsrun_dir/empty"
  : > "$work/rows.tsv"
  while read -r n _; do
    case $n in ''|'#'*) continue ;; esac
    case $n in
      syscall_js) jsrun_test >> "$work/rows.tsv" ;;
      scene) jsrun_scene input.txt 0 "${record:+1}" >> "$work/rows.tsv" ;;
      shim) jsrun_shim_check >> "$work/rows.tsv"; jsrun_ops_check >> "$work/rows.tsv" ;;
      *) jsrun_own "$n" >> "$work/rows.tsv" ;;
    esac
  done < "$list"
  awk -F'\t' '{ r = ($2 == "pass") ? ":pass" : ($2 == "skip" ? ":skip" : ":fail"); gsub(/"/, "'"'"'", $3); printf "[\"%s\" %s \"%s\"]\n", $1, r, $3 }' \
    "$work/rows.tsv" > "$work/rows.edn"
  grep -v '	pass	' "$work/rows.tsv" | sed 's/^/wai js: /' || true
  # the negative controls (GTS81 (a)), each judged by the corpus's own judge: it must fail; a _check/ line must pass
  local cid subj mode other v
  while read -r cid subj mode other; do
    case $cid in ''|'#'*) continue ;; esac
    case $mode in
      vs) v=$(jsrun_own "$subj" "$other") ;;
      self) v=$(jsrun_own "$subj") ;;
      shift) v=$(jsrun_scene input.txt "$other" 2>>"$work/controls.log") ;;
      input) v=$(jsrun_scene "$other" 0 2>>"$work/controls.log") ;;
      scene-self) v=$(jsrun_scene input.txt 0 2>>"$work/controls.log") ;;
      shim-drop)
        sed "\#\"$other\": #d" "$root/wasm/web/arbace.js" > "$work/arbace-drop.js"; v=$(jsrun_shim_check "$work/arbace-drop.js") ;;
      ops-change)
        sed 's/^const nargs = \[0, 1, 7,/const nargs = [0, 1, 6,/' "$root/wasm/web/arbace.js" > "$work/arbace-ops.js"; v=$(jsrun_ops_check "$work/arbace-ops.js") ;;
      *) echo "FAIL: control $cid: unknown mode $mode"; fails=$((fails + 1)); continue ;;
    esac
    case $cid in
      _check/*) case $v in *"	pass	"*) echo "check $cid: passes, as it must" ;; *) echo "FAIL: check $cid: $v"; fails=$((fails + 1)) ;; esac ;;
      *) case $v in *"	fail	"*) echo "control $cid: fails as it must" ;; *) echo "FAIL: control $cid: $v"; fails=$((fails + 1)) ;; esac ;;
    esac
  done < "$jsrun_controls"
  if [ "$list" = "$root/test/wasm/js-run.txt" ]; then
    judge "$jsrun_results" "$work/rows.edn" "$jsrun_classes" "$record" "js" js || fails=$((fails + 1))
  else
    arbace_tool -e "(let [run (set (map first (read-string (str \"[\" (slurp \"$work/rows.edn\") \"]\")))) old (if (.exists (java.io.File. \"$jsrun_results\")) (read-string (slurp \"$jsrun_results\")) [])] (spit \"$work/old.edn\" (pr-str (vec (filter #(run (first %)) old)))))"
    judge "$work/old.edn" "$work/rows.edn" "$jsrun_classes" "" "" js || fails=$((fails + 1))
  fi
  [ $fails = 0 ]
}
