# wai's Go run corpus (doc/wasm/WAI.md §8; doc/wasm/WASM-SURVEY.md §10.2, WAS11 (a), WA5), sourced by bin/wai: $GOROOT/test's run
# programs (the ids of goi's record test/goi/check-linux-amd64.edn whose action is `run`, as Go's test driver reads it) and Go's own
# small programs of test/wasm/go-run.txt, each built for GOOS=wasip1 GOARCH=wasm by TamaGo's go (GOPATH mode in its directory with
# its action's build flags, as goi's gc reference is built; cached by the source's, the flags' and the toolchain's hashes) and run
# by wai's light driver (`waidriver run`: WASI preview 1, $GOROOT preopened read-only at its own path, $GOROOT/test the working
# directory, /tmp a scratch directory), judged against gc's linux/amd64 build and run of the original as goi's check judges goi:
# exit status, standard output byte for byte, standard error up to the goroutines' traces (goi's gc references, the main
# checkout's .tmp/goi-check/gcref, read by goi's own key; a missing one made the same way into .tmp/wai-go/gcref); a program gc's
# own run does not pass is a skip, as goi's. Rows [go/ID RESULT REASON] against test/wasm/go-results.edn, classes
# test/wasm/divergences-go.edn, the controls of test/wasm/controls-go.txt. Expects bin/wai's root, main, goroot, wai_exe.

. "$root/bin/lib/limit.bash"

gorun_dir=$main/.tmp/wai-go
gorun_results=$root/test/wasm/go-results.edn
gorun_aot_results=$root/test/wasm/go-aot-results.edn
gorun_classes=$root/test/wasm/divergences-go.edn
gorun_controls=$root/test/wasm/controls-go.txt

# goi's definitions of stderr_head and run_env, taken from bin/goi's text: they are in goi's reference key (declare -f), so the
# keys made here are goi's
eval "$(sed -n '/^stderr_head() {$/,/^}$/p; /^run_env() {$/,/^}$/p' "$root/bin/goi")"

gorun_action_of() {
  awk 'NF==0{next} /^\/\/go:build/{next} /^\/\/ \+build/{next} {sub(/^\/\/ ?/, ""); print; exit}' "$1"
}

# gorun_parse PROGRAM: sets tmo, bflags, files, args (the caller's locals) from its action line, or why and returns 1; files the
# program's and the .go files its action names first (`run cmplxdivide1.go`: compiled with it, as goi's forms' :files list them)
gorun_parse() {
  local act x w=()
  tmo=10 bflags=() args=() files=("$(basename "$1")")
  act=$(gorun_action_of "$goroot/test/$1")
  if [[ $act == *[\'\"]* ]]; then
    x=$(printf '%s\n' "$act" | xargs printf '%s\n' 2>&1) || { why="fail	the action cannot be split: $act"; return 1; }
    while IFS= read -r x; do w+=("$x"); done <<< "$x"
    set -- "${w[@]}"
  else
    set -- $act
  fi
  [ "${1:-}" = run ] || { why="skip	not a run test: $act"; return 1; }
  shift
  while [ $# -gt 0 ] && [ "${1#-}" != "$1" ]; do
    case $1 in
      -t) tmo=$2; shift 2 ;;
      -goexperiment|-godebug) why="skip	the action sets ${1#-}: $act"; return 1 ;;
      *) bflags+=("$1"); shift ;;
    esac
  done
  while [ $# -gt 0 ] && [ "${1%.go}" != "$1" ]; do files+=("$1"); shift; done
  args=("$@")
}

gorun_gc_env() {
  echo GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= GO111MODULE=off GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 \
    GOOS=linux GOARCH=amd64 GOAMD64=v1 GOCACHE="$gorun_dir/gocache-linux"
}

# gorun_ref PROGRAM: gc's reference (goi's gc_ref: the same key, layout and judgment), sets ref; 1 with a row printed when gc's
# build fails or its run times out
gorun_ref() {
  local p=$1 g=$goroot/test/$1 key rt gst x
  key=$( { cat "$goroot/VERSION"; for x in "${files[@]}"; do echo "$x"; cat "$(dirname "$g")/$x"; done
           echo "flags:"; printf '%s\n' ${bflags[@]+"${bflags[@]}"}; echo "args:"; printf '%s\n' ${args[@]+"${args[@]}"}
           declare -f stderr_head run_env; } | sha256sum | cut -c1-24)
  ref=$main/.tmp/goi-check/gcref/$key
  [ -f "$ref/exit" ] && return 0
  ref=$gorun_dir/gcref/$key
  [ -f "$ref/exit" ] && return 0
  rt=$ref.tmp.$$; rm -rf "$rt"; mkdir -p "$rt"
  if ! (cd "$(dirname "$g")" && env $(gorun_gc_env) "$goroot/bin/go" build ${bflags[@]+"${bflags[@]}"} -o "$rt/exe" "${files[@]}") > "$rt/build.log" 2>&1; then
    printf '%s\tfail\tgc reference: the original'"'"'s build failed: %s\n' "go/$p" "$(head -1 "$rt/build.log" | cut -c1-200)"; rm -rf "$rt"; return 1
  fi
  local gcwork=$main/.tmp/goi-check
  gst=0; (cd "$goroot/test" && export $(run_env) && limited "$tmo" "$rt/exe" ${args[@]+"${args[@]}"} > "$rt/out" 2> "$rt/err.raw" < "$gorun_dir/empty") || gst=$?
  case $gst in 124|137|152) rm -rf "$rt"; printf '%s\tfail\tgc reference: the original timed out\n' "go/$p"; return 1 ;; esac
  stderr_head "$rt/err.raw" > "$rt/err"
  echo $gst > "$rt/exit"
  if [ -f "$goroot/test/${p%.go}.out" ]; then
    if cat "$rt/out" "$rt/err.raw" | cmp -s - "$goroot/test/${p%.go}.out"; then echo ok; else echo differs; fi > "$rt/golden"
  else
    echo none > "$rt/golden"
  fi
  rm -f "$rt/exe"
  mkdir -p "${ref%/*}"; mv -T "$rt" "$ref" 2>"$rt.mv.err" || rm -rf "$rt"; rm -f "$rt.mv.err"
}

# gorun_ref_fails REF: why gc's own run does not pass the test (goi's gc_ref_fails), or nothing
gorun_ref_fails() {
  local st golden
  st=$(cat "$1/exit"); golden=$(cat "$1/golden" 2>"$TMPDIR/golden.err" || echo none)
  if [ "$st" != 0 ]; then echo "status $st"
  elif [ "$golden" = differs ] && { [ ! -s "$1/out" ] || [ ! -s "$1/err.raw" ]; }; then echo "its output differs from the golden .out"
  elif [ "$golden" = none ] && { [ -s "$1/out" ] || [ -s "$1/err.raw" ]; }; then echo "it writes output and the test has no golden .out"
  fi
}

# gorun_build SRCDIR OUT FILE...: the program built for wasip1/wasm (cached by key), sets bin; on failure the build's first error line in
# OUT.why
gorun_build() {
  local dir=$1 out=$2 key x
  shift 2
  key=$( { cat "$goroot/VERSION"; for x in "$@"; do echo "$x"; cat "$dir/$x"; done; echo "flags:"; printf '%s\n' ${bflags[@]+"${bflags[@]}"}; } | sha256sum | cut -c1-24)
  bin=$gorun_dir/bin/$key.wasm
  [ -f "$bin" ] && return 0
  mkdir -p "$gorun_dir/bin"
  if ! (cd "$dir" && env GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= GO111MODULE=off GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 \
          GOOS=wasip1 GOARCH=wasm GOCACHE="$main/.tmp/wasm/gocache" GOMAXPROCS=2 \
          "$goroot/bin/go" build -p 1 ${bflags[@]+"${bflags[@]}"} -o "$bin.tmp.$$" "$@") > "$out.build.log" 2>&1; then
    rm -f "$bin.tmp.$$"
    grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"
    return 1
  fi
  mv -f "$bin.tmp.$$" "$bin"
}

# gorun_wai OUT WASM ARG...: the module run by wai in $GOROOT/test, OUT.out, OUT.err (stderr_head), OUT.exit
gorun_wai() {
  local o=$1 w=$2 st=0 scratch
  shift 2
  scratch=$o.tmpdir; rm -rf "$scratch"; mkdir -p "$scratch"
  local gcwork=$main/.tmp/goi-check env=() kv
  for kv in $(run_env); do env+=(--env "$kv"); done
  (cd "$goroot/test" && GOMEMLIMIT=${WAI_GOMEMLIMIT:-1200MiB} ARBACE_MEM_GIB=0 WAI_AOT_STATS=${gorun_aot:+$o.aot} limited "$((tmo * ${WAI_GO_SLOW:-3}))" "${aexe:-$wai_exe}" run \
     --dir "$goroot=$goroot:ro" --dir "/tmp=$scratch" --env "PWD=$goroot/test" --env TMPDIR=/tmp ${env[@]+"${env[@]}"} \
     "$w" "$@" > "$o.out" 2> "$o.err.raw" < "$gorun_dir/empty") || st=$?
  rm -rf "$scratch"
  stderr_head "$o.err.raw" > "$o.err"
  echo "$st" > "$o.exit"
}

# gorun_one ID: one program's row (ID go/PATH for $GOROOT/test's, go/own/NAME for Go's own small programs)
gorun_one() {
  local id=$1 p=${1#go/} tmo=10 bflags=() args=() files=() why ref x o st want aexe=""

  o=$gorun_work/run/${p//\//__}
  mkdir -p "$gorun_work/run"
  case $p in
    own/*)
      local src=$root/test/wasm/go/${p#own/}
      gorun_build "$src" "$o" main.go || { printf '%s\tfail\tbuild: %s\n' "$id" "$(cat "$o.why")"; return 0; }
      if [ -n "${gorun_aot:-}" ]; then aot_go_exe "$bin" "$o" || { printf '%s\tfail\t%s\n' "$id" "$(cat "$o.why")"; return 0; }; fi
      gorun_wai "$o" "$bin"
      st=$(cat "$o.exit")
      if [ "$st" != 0 ]; then printf '%s\tfail\tstatus %s: %s\n' "$id" "$st" "$(head -c 200 "$o.err" | tr '\t\n' '  ')"
      elif ! cmp -s "$o.out" "$src/want.out"; then printf '%s\tfail\tstandard output differs from want.out: %s\n' "$id" "$(head -c 120 "$o.out" | tr '\t\n' '  ')"
      elif [ -s "$o.err" ]; then printf '%s\tfail\tstandard error not empty: %s\n' "$id" "$(head -c 200 "$o.err" | tr '\t\n' '  ')"
      else printf '%s\tpass\t\n' "$id"; fi | if [ -n "${gorun_aot:-}" ]; then aot_go_verdict "$id" "$o"; else cat; fi
      return 0 ;;
  esac
  case $(gorun_action_of "$goroot/test/$p" | awk '{print $1}') in rundir|buildrundir|runindir) gorun_dir_one "$id"; return 0 ;; esac
  gorun_parse "$p" || { printf '%s\t%s\n' "$id" "$why"; return 0; }
  # Go's test driver's own build constraints (cmd/internal/testdir's shouldTest) for wasip1/wasm: a program it would not run is a skip
  x=$("$wai_exe" buildok "$goroot/test/$p")
  [ "$x" = ok ] || { printf '%s\tskip\tthe test driver'"'"'s build constraint excludes wasip1/wasm: %s\n' "$id" "$x"; return 0; }
  gorun_ref "$p" || return 0
  x=$(gorun_ref_fails "$ref")
  [ -z "$x" ] || { printf '%s\tskip\tgc'"'"'s own run does not pass here: %s\n' "$id" "$x"; return 0; }
  gorun_build "$goroot/test/$(dirname "$p")" "$o" "${files[@]}" || {
    x=$(cat "$o.why")
    case $x in
      *"build constraints exclude all Go files"*) printf '%s\tskip\tbuild constraints exclude wasip1/wasm: %s\n' "$id" "$x" ;;
      *) printf '%s\tfail\tbuild: %s\n' "$id" "$x" ;;
    esac
    return 0; }
  if [ -n "${gorun_aot:-}" ]; then aot_go_exe "$bin" "$o" || { printf '%s\tfail\t%s\n' "$id" "$(cat "$o.why")"; return 0; }; fi
  gorun_wai "$o" "$bin" ${args[@]+"${args[@]}"}
  gorun_judge "$id" "$o" "$ref" | if [ -n "${gorun_aot:-}" ]; then aot_go_verdict "$id" "$o"; else cat; fi
}

# gorun_dir_parse PROGRAM: a directory test's action (rundir, buildrundir, runindir; WAS34 (a), WA5b): sets kind, tmo, single
# (-s: a file a package), modv (-gomodversion), bflags (rundir's the compiler's), lflags (rundir's after -ldflags, the linker's),
# args (rundir's, the program's), or why and returns 1
gorun_dir_parse() {
  local act x w=()
  kind="" tmo=10 single="" modv=1.14 bflags=() lflags=() args=()
  act=$(gorun_action_of "$goroot/test/$1")
  if [[ $act == *[\'\"]* ]]; then
    x=$(printf '%s\n' "$act" | xargs printf '%s\n' 2>&1) || { why="fail	the action cannot be split: $act"; return 1; }
    while IFS= read -r x; do w+=("$x"); done <<< "$x"
    set -- "${w[@]}"
  else
    set -- $act
  fi
  case ${1:-} in rundir|buildrundir|runindir) kind=$1 ;; *) why="skip	not a directory test: $act"; return 1 ;; esac
  shift
  while [ $# -gt 0 ] && [ "${1#-}" != "$1" ]; do
    case $1 in
      -t) tmo=$2; shift 2 ;;
      -s) single=1; shift ;;
      -gomodversion) modv=$2; shift 2 ;;
      -goexperiment|-godebug) why="skip	the action sets ${1#-}: $act"; return 1 ;;
      -ldflags) shift; lflags=("$@"); set -- ;;
      *) bflags+=("$1"); shift ;;
    esac
  done
  args=("$@")
}

# gorun_std_importcfg: the standard library's export data for wasip1/wasm (Go's test driver's stdlibImportcfg: go list -export std),
# once a check run, into $gorun_work/importcfg.std
gorun_std_importcfg() {
  local cfg=$gorun_work/importcfg.std
  [ -s "$cfg" ] && return 0
  (cd "$goroot/src" && env $(gorun_wasm_env) "$goroot/bin/go" list -export \
     -f '{{if .Export}}packagefile {{.ImportPath}}={{.Export}}{{end}}' std) > "$cfg.tmp.$$" 2> "$gorun_work/importcfg.err" &&
    mv -f "$cfg.tmp.$$" "$cfg"
}

gorun_wasm_env() {
  echo GOROOT="$goroot" GOTOOLCHAIN=local GOENV=off GOFLAGS= GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 \
    GOOS=wasip1 GOARCH=wasm GOCACHE="$main/.tmp/wasm/gocache" GOMAXPROCS=2
}

# gorun_dir_build PROGRAM WD OUT: the directory test built for wasip1/wasm as cmd/internal/testdir builds it (rundir: each package
# of PROGRAM's .dir compiled in the files' order, `go tool compile -e -D test -p test/NAME`, the last linked as main; buildrundir:
# every .go file compiled as main with the .s files assembled, packed and linked; runindir: a shallow copy made a module named after
# the directory, `go build .`), in the work directory WD; the module cached by key (the action, every file of the directory, the
# toolchain), sets bin; the first error line in OUT.why
gorun_dir_build() {
  local p=$1 wd=$2 out=$3 dir key x f name pk last="" mod
  dir=$goroot/test/${p%.go}.dir
  key=$( { cat "$goroot/VERSION"; echo "$kind single=$single modv=$modv"; printf '%s\n' ${bflags[@]+"${bflags[@]}"} -- ${lflags[@]+"${lflags[@]}"}
           (cd "$dir" && find . -type f | LC_ALL=C sort | while read -r f; do echo "$f"; cat "$f"; done); } | sha256sum | cut -c1-24)
  bin=$gorun_dir/bin/dir-$key.wasm
  [ -f "$bin" ] && return 0
  mkdir -p "$gorun_dir/bin" "$wd/test"
  case $kind in
    rundir)
      gorun_std_importcfg || { echo "the standard library's export data (go list -export std) failed" > "$out.why"; return 1; }
      # goDirPackages: the .go files in name order, a package per name (each file one with -s)
      local names=() pkgs=() cfg=$wd/importcfg
      for f in $(cd "$dir" && ls | LC_ALL=C sort); do
        case $f in *.go) ;; *) continue ;; esac
        name=$(sed -n 's/^package \([[:alnum:]_]*\).*/\1/p' "$dir/$f" | head -1)
        [ -n "$name" ] || { echo "cannot find package name in $f" > "$out.why"; return 1; }
        if [ -z "$single" ] && [ ${#pkgs[@]} -gt 0 ] && x=$(printf '%s\n' "${pkgs[@]}" | grep -n "^$name " | head -1 | cut -d: -f1) && [ -n "$x" ]; then
          pkgs[$((x - 1))]="${pkgs[$((x - 1))]} $f"
        else
          pkgs+=("$name $f")
        fi
      done
      { cat "$gorun_work/importcfg.std"; for pk in "${pkgs[@]}"; do set -- $pk; echo "packagefile test/${2%.go}=$wd/test/${2%.go}.a"; done; } > "$cfg"
      for pk in "${pkgs[@]}"; do
        set -- $pk; name=$1; shift
        local fs=() pf=()
        for f in "$@"; do fs+=("$dir/$f"); done
        if [ "$name" = main ]; then pf=(-p=main); last=${1%.go}.o; else pf=(-o "test/${1%.go}.a" -p "test/${1%.go}"); fi
        (cd "$wd" && env $(gorun_wasm_env) "$goroot/bin/go" tool compile -e -D test -importcfg="$cfg" "${pf[@]}" ${bflags[@]+"${bflags[@]}"} "${fs[@]}") \
          >> "$out.build.log" 2>&1 || { grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; }
      done
      # the last package is linked as main (Go's driver links pkgs[len-1].files[0]'s object)
      set -- ${pkgs[$((${#pkgs[@]} - 1))]}
      (cd "$wd" && env $(gorun_wasm_env) "$goroot/bin/go" tool link -s -w -buildid=test -o "$bin.tmp.$$" -importcfg="$cfg" \
         ${lflags[@]+"${lflags[@]}"} "${2%.go}.o") >> "$out.build.log" 2>&1 \
        || { rm -f "$bin.tmp.$$"; grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; } ;;
    buildrundir)
      gorun_std_importcfg || { echo "the standard library's export data (go list -export std) failed" > "$out.why"; return 1; }
      local gos=() asms=() hdr=()
      for f in $(cd "$dir" && ls | LC_ALL=C sort); do
        case $f in *.go) gos+=("$dir/$f") ;; *.s) asms+=("$dir/$f") ;; esac
      done
      if [ ${#asms[@]} -gt 0 ]; then
        : > "$wd/go_asm.h"
        (cd "$wd" && env $(gorun_wasm_env) "$goroot/bin/go" tool asm -p=main -gensymabis -o symabis "${asms[@]}") >> "$out.build.log" 2>&1 \
          || { grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; }
        hdr=(-asmhdr go_asm.h -symabis symabis)
      fi
      local objs=(go.o)
      (cd "$wd" && env $(gorun_wasm_env) "$goroot/bin/go" tool compile -p=main -e -D . -importcfg="$gorun_work/importcfg.std" -o go.o \
         ${hdr[@]+"${hdr[@]}"} "${gos[@]}") >> "$out.build.log" 2>&1 || { grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; }
      if [ ${#asms[@]} -gt 0 ]; then
        (cd "$wd" && env $(gorun_wasm_env) "$goroot/bin/go" tool asm -p=main -e -I . -o asm.o "${asms[@]}") >> "$out.build.log" 2>&1 \
          || { grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; }
        objs+=(asm.o)
      fi
      (cd "$wd" && env $(gorun_wasm_env) "$goroot/bin/go" tool pack c all.a "${objs[@]}" &&
         env $(gorun_wasm_env) "$goroot/bin/go" tool link -s -w -buildid=test -o "$bin.tmp.$$" -importcfg="$gorun_work/importcfg.std" all.a) \
        >> "$out.build.log" 2>&1 || { rm -f "$bin.tmp.$$"; grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; } ;;
    runindir)
      mod=$(basename "$dir")
      gorun_dir_copy "$dir" "$wd"
      (cd "$wd/src/$mod" && env $(gorun_wasm_env) GO111MODULE=on GOPATH="$wd" "$goroot/bin/go" build -p 1 -gcflags=all= \
         ${bflags[@]+"${bflags[@]}"} -o "$bin.tmp.$$" .) >> "$out.build.log" 2>&1 \
        || { rm -f "$bin.tmp.$$"; grep -v '^#' "$out.build.log" | head -1 | cut -c1-200 > "$out.why"; return 1; } ;;
  esac
  mv -f "$bin.tmp.$$" "$bin"
}

# gorun_dir_copy DIR WD: runindir's shallow copy of DIR as the module WD/src/BASENAME (go.mod: module BASENAME, go $modv)
gorun_dir_copy() {
  local mod
  mod=$(basename "$1")
  rm -rf "$2/src/$mod"; mkdir -p "$2/src"
  cp -R "$1" "$2/src/$mod"
  printf 'module %s\ngo %s\n' "$mod" "$modv" > "$2/src/$mod/go.mod"
}

# gorun_dir_one ID: a directory test's row: built (gorun_dir_build), run by wai in Go's test driver's directory (rundir's and
# buildrundir's its work directory, runindir's the module's copy), standard output and error combined, judged as Go's test driver
# and goi's check judge it (dir_verdict): status 0 and the output the golden .out byte for byte (none: no output)
gorun_dir_one() {
  local id=$1 p=${1#go/} kind tmo single modv bflags=() lflags=() args=() why x o wd cwd st want
  o=$gorun_work/run/${p//\//__}
  mkdir -p "$gorun_work/run"
  gorun_dir_parse "$p" || { printf '%s\t%s\n' "$id" "$why"; return 0; }
  x=$("$wai_exe" buildok "$goroot/test/$p")
  [ "$x" = ok ] || { printf '%s\tskip\tthe test driver'"'"'s build constraint excludes wasip1/wasm: %s\n' "$id" "$x"; return 0; }
  wd=$o.wd; rm -rf "$wd" "$o.build.log"; mkdir -p "$wd"
  gorun_dir_build "$p" "$wd" "$o" || { rm -rf "$wd"; printf '%s\tfail\tbuild: %s\n' "$id" "$(cat "$o.why")"; return 0; }
  cwd=$wd
  if [ "$kind" = runindir ]; then
    gorun_dir_copy "$goroot/test/${p%.go}.dir" "$wd"
    cwd=$wd/src/$(basename "${p%.go}.dir")
  fi
  local scratch=$o.tmpdir env=() kv gcwork=$main/.tmp/goi-check
  rm -rf "$scratch"; mkdir -p "$scratch"
  for kv in $(run_env); do env+=(--env "$kv"); done
  st=0
  local aexe=""
  if [ -n "${gorun_aot:-}" ]; then
    aot_go_exe "$bin" "$o" || { rm -rf "$wd"; printf '%s\tfail\t%s\n' "$id" "$(cat "$o.why")"; return 0; }
  fi
  (cd "$cwd" && GOMEMLIMIT=${WAI_GOMEMLIMIT:-1200MiB} ARBACE_MEM_GIB=0 WAI_AOT_STATS=${gorun_aot:+$o.aot} limited "$((tmo * ${WAI_GO_SLOW:-3}))" "${aexe:-$wai_exe}" run \
     --dir "$goroot=$goroot:ro" --dir "$wd=$wd" --dir "/tmp=$scratch" --env "PWD=$cwd" --env TMPDIR=/tmp ${env[@]+"${env[@]}"} \
     "$bin" ${args[@]+"${args[@]}"} > "$o.out" 2>&1 < "$gorun_dir/empty") || st=$?
  rm -rf "$scratch" "$wd"
  echo "$st" > "$o.exit"
  want=$goroot/test/${p%.go}.out
  gorun_dir_verdict "$id" "$o" "$st" "$want" | if [ -n "${gorun_aot:-}" ]; then aot_go_verdict "$id" "$o"; else cat; fi
}

gorun_dir_verdict() {
  local id=$1 o=$2 st=$3 want=$4
  if [ "$st" = 125 ] && grep -q '^wai: ' "$o.out"; then printf '%s\tfail\twai: %s\n' "$id" "$(grep -m1 '^wai: ' "$o.out" | cut -c6-200 | tr '\t' ' ')"
  elif [ "$st" = 126 ] && grep -q '^wai: ' "$o.out"; then printf '%s\tfail\twai: %s\n' "$id" "$(grep -m1 '^wai: ' "$o.out" | cut -c6-240 | tr '\t' ' ')"
  elif [ "$st" = 124 ] || [ "$st" = 137 ] || [ "$st" = 152 ]; then printf '%s\tfail\ttimeout: status %s\n' "$id" "$st"
  elif [ "$st" = 0 ] && { if [ -f "$want" ]; then cmp -s "$o.out" "$want"; else [ ! -s "$o.out" ]; fi; }; then printf '%s\tpass\t\n' "$id"
  else
    printf '%s\tfail\tstatus %s, output differs from the golden %s: %s\n' "$id" "$st" "${want#$goroot/}" \
      "$( { if [ -f "$want" ]; then diff "$want" "$o.out"; else cat "$o.out"; fi; } | grep -v '^[0-9]' | head -2 | tr '\t\n' '  ' | cut -c1-200)"
  fi
}

# gorun_judge ID RUN REF: a run against a gc reference (status, stdout, stderr's head)
gorun_judge() {
  local id=$1 o=$2 ref=$3 st want
  st=$(cat "$o.exit"); want=$(cat "$ref/exit")
  if [ "$st" = 125 ] && grep -q '^wai: ' "$o.err"; then printf '%s\tfail\twai: %s\n' "$id" "$(grep -m1 '^wai: ' "$o.err" | cut -c6-200 | tr '\t' ' ')"
  elif [ "$st" = 126 ] && grep -q '^wai: ' "$o.err"; then printf '%s\tfail\twai: %s\n' "$id" "$(grep -m1 '^wai: ' "$o.err" | cut -c6-240 | tr '\t' ' ')"
  elif [ "$st" = 124 ] || [ "$st" = 137 ] || [ "$st" = 152 ]; then printf '%s\tfail\ttimeout: status %s\n' "$id" "$st"
  elif [ "$st" != "$want" ]; then printf '%s\tfail\tstatus %s, gc %s: %s\n' "$id" "$st" "$want" "$(head -c 200 "$o.err" | tr '\t\n' '  ')"
  elif ! cmp -s "$o.out" "$ref/out"; then printf '%s\tfail\tstandard output differs from gc'"'"'s: %s\n' "$id" "$(cmp "$o.out" "$ref/out" 2>&1 | head -1 | cut -c1-160 | sed "s|$gorun_work/run/||; s|$ref|gc|")"
  elif ! cmp -s "$o.err" "$ref/err"; then printf '%s\tfail\tstandard error differs from gc'"'"'s: %s\n' "$id" "$(diff "$ref/err" "$o.err" | grep '^[<>]' | head -2 | cut -c1-120 | tr '\t\n' '  ')"
  else printf '%s\tpass\t\n' "$id"; fi
}

# the corpus's ids: Go's own small programs, then $GOROOT/test's run programs and directory tests (goi's record's ids whose action is
# run, rundir, buildrundir or runindir; WAS34 (a)) and the directory tests goi's linux record does not hold (test/wasm/go-dirs.txt:
# wasmmemsize.go's //go:build wasm)
gorun_ids() {
  sed -n 's/^[^#;].*/&/p' "$root/test/wasm/go-run.txt" | sed '/^$/d' | sed 's|^|go/own/|'
  { awk -F'"' '/^ \["/{print $2}' "$root/test/goi/check-linux-amd64.edn"; grep -v '^#' "$root/test/wasm/go-dirs.txt" | sed '/^$/d'; } |
    while read -r p; do
      [ -f "$goroot/test/$p" ] || continue
      case $(gorun_action_of "$goroot/test/$p" | awk '{print $1}') in run|rundir|buildrundir|runindir) echo "go/$p" ;; esac
    done
}

gorun_tsv2edn() {
  awk -F'\t' '{ r=$3; gsub(/\\/, "(backslash)", r); gsub(/"/, "'"'"'", r); gsub(/[^ -~]/, "?", r)
                printf "[\"%s\" :%s \"%s\"]\n", $1, $2, r }' "$1"
}

# gorun_check RECORD LIST: the corpus (or LIST's ids, a path under $GOROOT/test or own/NAME a line) run and judged
gorun_check() {
  local record=$1 list=$2 fails=0 jobs=${WAI_GO_JOBS:-3} res=$gorun_results kind=go
  # WA7: --aot, each program's module translated by w2g and built as an AOT program (one at a time: the builds' memory)
  if [ -n "${gorun_aot:-}" ]; then jobs=${WAI_GO_JOBS:-1} res=$gorun_aot_results kind=go-aot; fi
  # WAS50 (a): by shape, one program for the run: the runs WAI_GO_JOBS at a time
  if [ -n "${gorun_aot:-}" ] && [ "${WAI_AOT_SHARED:-1}" != 0 ]; then jobs=${WAI_GO_JOBS:-2}; fi
  gorun_work=$wai_dir/go${gorun_aot:+-aot}; rm -rf "$gorun_work"; mkdir -p "$gorun_work" "$gorun_dir"
  : > "$gorun_dir/empty"
  if [ -n "$list" ]; then grep -v '^#' "$list" | sed '/^$/d; s|^|go/|' > "$gorun_work/ids"; else gorun_ids > "$gorun_work/ids"; fi
  light
  echo "wai go: $(wc -l < "$gorun_work/ids") programs ($jobs at a time)"
  WAI_SRCS_KEY=$(srcs_key)
  export root main goroot wai_exe wai_dir gorun_work gorun_dir TMPDIR gorun_aot WAI_SRCS_KEY gorun_collect
  if [ -n "${gorun_aot:-}" ] && [ "${WAI_AOT_SHARED:-1}" != 0 ]; then
    # WAS40 (c), WAS50 (a): a first pass builds the programs and collects their modules, translated by shape into one AOT program;
    # the second runs every program on it, WAI_GO_JOBS at a time (the per-program translation, WA7's: WAI_AOT_SHARED=0)
    gorun_collect=1 xargs -P "$jobs" -I{} bash -c '. "$root/bin/lib/wai/go-run.bash"; . "$root/bin/lib/wai/aot.bash"; gorun_one "$1"' _ {} < "$gorun_work/ids" > "$gorun_work/collect.tsv"
    # WAI_AOT_SLICE=N: N programs a shared program (the disk's and the build's measure: the whole corpus is about 9,000 shapes,
    # 360 MB of Go), each slice's program built, its programs run, its build's cache and program removed before the next
    local slice=${WAI_AOT_SLICE:-400} sl
    if [ "$slice" -gt 0 ]; then
      split -l "$slice" -d -a 3 "$gorun_work/ids" "$gorun_work/slice."
      : > "$gorun_work/rows.tsv"
      for sl in "$gorun_work"/slice.*; do
        echo "wai go --aot: slice ${sl##*.}: $(wc -l < "$sl") programs"
        WAI_GOCACHE=$gorun_work/aot-gocache WAI_AOT_SHARED_KEEP=1 aot_go_shared "$gorun_work" "$sl" || return 1
        rm -rf "$gorun_work/aot-gocache"
        xargs -P "$jobs" -I{} bash -c '. "$root/bin/lib/wai/go-run.bash"; . "$root/bin/lib/wai/aot.bash"; gorun_one "$1"' _ {} < "$sl" >> "$gorun_work/rows.tsv"
      done
    else
      aot_go_shared "$gorun_work" || return 1
    fi
  fi
  if [ -z "${gorun_aot:-}" ] || [ "${WAI_AOT_SHARED:-1}" = 0 ] || [ "${WAI_AOT_SLICE:-400}" = 0 ]; then
    xargs -P "$jobs" -I{} bash -c '. "$root/bin/lib/wai/go-run.bash"; . "$root/bin/lib/wai/aot.bash"; gorun_one "$1"' _ {} < "$gorun_work/ids" > "$gorun_work/rows.tsv"
  fi
  LC_ALL=C sort -o "$gorun_work/rows.tsv" "$gorun_work/rows.tsv"
  gorun_tsv2edn "$gorun_work/rows.tsv" > "$gorun_work/rows.edn"
  gorun_controls_run || fails=$((fails + 1))
  if [ -n "$list" ] || [ -n "${gorun_vs_eval:-}" ]; then
    # a partial run judged on its programs' rows alone; --aot --vs-eval (WAS40 (a)'s rotation): against eval's record, the AOT
    # level equal to eval's verdicts program for program
    local src=$res
    [ -z "${gorun_vs_eval:-}" ] || src=$gorun_results
    arbace_tool -e "(let [run (set (map first (read-string (str \"[\" (slurp \"$gorun_work/rows.edn\") \"]\")))) old (read-string (slurp \"$src\"))] (spit \"$gorun_work/old.edn\" (pr-str (vec (filter #(run (first %)) old)))))"
    judge "$gorun_work/old.edn" "$gorun_work/rows.edn" "$gorun_classes" "" "" "$kind" || fails=$((fails + 1))
  else
    judge "$res" "$gorun_work/rows.edn" "$gorun_classes" "$record" "go" "$kind" || fails=$((fails + 1))
  fi
  [ $fails = 0 ]
}

# the negative controls (GTS81 (a)): `vs OTHER`, the subject's run on wai judged against another program's gc reference, must fail;
# `self`, the subject's run against its own, must pass (a _check/ line)
gorun_controls_run() {
  local cid subj mode other v fails=0 tmo bflags args files why ref o
  while read -r cid subj mode other; do
    case $cid in ''|'#'*) continue ;; esac
    o=$gorun_work/run/${subj//\//__}
    [ -f "$o.exit" ] || { echo "control $cid: left (its subject was not run)"; continue; }
    local target=${other:-$subj}
    if [ "$mode" = golden ]; then
      # WA5b: a directory test's run judged against another directory test's golden output (or its own, a _check/ line)
      v=$(gorun_dir_verdict "go/$subj" "$o" "$(cat "$o.exit")" "$goroot/test/${target%.go}.out" | cut -f2)
      case $cid in
        _check/*) if [ "$v" = pass ]; then echo "check $cid: passes, as it must"; else echo "FAIL: check $cid: $v"; fails=$((fails + 1)); fi ;;
        *) if [ "$v" != pass ]; then echo "control $cid: fails as it must"; else echo "FAIL: control $cid: passes"; fails=$((fails + 1)); fi ;;
      esac
      continue
    fi
    gorun_parse "$target" || { echo "FAIL: control $cid: $target is no run program"; fails=$((fails + 1)); continue; }
    gorun_ref "$target" > "$TMPDIR/ctl.row" || { echo "FAIL: control $cid: no reference for $target"; fails=$((fails + 1)); continue; }
    v=$(gorun_judge "go/$subj" "$o" "$ref" | cut -f2)
    case $cid in
      _check/*) if [ "$v" = pass ]; then echo "check $cid: passes, as it must"; else echo "FAIL: check $cid: $v"; fails=$((fails + 1)); fi ;;
      *) if [ "$v" != pass ]; then echo "control $cid: fails as it must"; else echo "FAIL: control $cid: passes"; fails=$((fails + 1)); fi ;;
    esac
  done < "$gorun_controls"
  [ $fails = 0 ]
}
