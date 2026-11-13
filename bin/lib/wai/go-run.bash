# wai's Go run corpus (doc/wasm/WAI.md §8; doc/wasm/WASM-SURVEY.md §10.2, WAS11 (a), WA5), sourced by bin/wai: $GOROOT/test's run
# programs (the ids of goi's record test/goi/check-linux-amd64.edn whose action is `run`, as Go's test driver reads it) and Go's own
# small programs of test/wasm/go-run.txt, each built for GOOS=wasip1 GOARCH=wasm by TamaGo's go (GOPATH mode in its directory with
# its action's build flags, as goi's gc reference is built; cached by the source's, the flags' and the toolchain's hashes) and run
# by wai's light driver (`waidriver run`: WASI preview 1, $GOROOT/test preopened read-only at its own path and the working
# directory, /tmp a scratch directory), judged against gc's linux/amd64 build and run of the original as goi's check judges goi:
# exit status, standard output byte for byte, standard error up to the goroutines' traces (goi's gc references, the main
# checkout's .tmp/goi-check/gcref, read by goi's own key; a missing one made the same way into .tmp/wai-go/gcref); a program gc's
# own run does not pass is a skip, as goi's. Rows [go/ID RESULT REASON] against test/wasm/go-results.edn, classes
# test/wasm/divergences-go.edn, the controls of test/wasm/controls-go.txt. Expects bin/wai's root, main, goroot, wai_exe.

. "$root/bin/lib/limit.bash"

gorun_dir=$main/.tmp/wai-go
gorun_results=$root/test/wasm/go-results.edn
gorun_classes=$root/test/wasm/divergences-go.edn
gorun_controls=$root/test/wasm/controls-go.txt

# goi's definitions of stderr_head and run_env, taken from bin/goi's text: they are in goi's reference key (declare -f), so the
# keys made here are goi's
eval "$(sed -n '/^stderr_head() {$/,/^}$/p; /^run_env() {$/,/^}$/p' "$root/bin/goi")"

gorun_action_of() {
  awk 'NF==0{next} /^\/\/go:build/{next} /^\/\/ \+build/{next} {sub(/^\/\/ ?/, ""); print; exit}' "$1"
}

# gorun_parse PROGRAM: sets tmo, bflags, args (the caller's locals) from its action line, or why and returns 1
gorun_parse() {
  local act x w=()
  tmo=10 bflags=() args=()
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
  args=("$@")
}

gorun_gc_env() {
  echo GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= GO111MODULE=off GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 \
    GOOS=linux GOARCH=amd64 GOAMD64=v1 GOCACHE="$gorun_dir/gocache-linux"
}

# gorun_ref PROGRAM: gc's reference (goi's gc_ref: the same key, layout and judgment), sets ref; 1 with a row printed when gc's
# build fails or its run times out
gorun_ref() {
  local p=$1 g=$goroot/test/$1 key rt gst
  key=$( { cat "$goroot/VERSION"; basename "$p"; cat "$g"
           echo "flags:"; printf '%s\n' ${bflags[@]+"${bflags[@]}"}; echo "args:"; printf '%s\n' ${args[@]+"${args[@]}"}
           declare -f stderr_head run_env; } | sha256sum | cut -c1-24)
  ref=$main/.tmp/goi-check/gcref/$key
  [ -f "$ref/exit" ] && return 0
  ref=$gorun_dir/gcref/$key
  [ -f "$ref/exit" ] && return 0
  rt=$ref.tmp.$$; rm -rf "$rt"; mkdir -p "$rt"
  if ! (cd "$(dirname "$g")" && env $(gorun_gc_env) "$goroot/bin/go" build ${bflags[@]+"${bflags[@]}"} -o "$rt/exe" "$(basename "$p")") > "$rt/build.log" 2>&1; then
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

# gorun_build SRCDIR FILE OUT: the program built for wasip1/wasm (cached by key), sets bin; on failure the build's first error line in
# OUT.why
gorun_build() {
  local dir=$1 file=$2 out=$3 key
  key=$( { cat "$goroot/VERSION"; echo "$file"; cat "$dir/$file"; echo "flags:"; printf '%s\n' ${bflags[@]+"${bflags[@]}"}; } | sha256sum | cut -c1-24)
  bin=$gorun_dir/bin/$key.wasm
  [ -f "$bin" ] && return 0
  mkdir -p "$gorun_dir/bin"
  if ! (cd "$dir" && env GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= GO111MODULE=off GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 \
          GOOS=wasip1 GOARCH=wasm GOCACHE="$main/.tmp/wasm/gocache" GOMAXPROCS=2 \
          "$goroot/bin/go" build -p 1 ${bflags[@]+"${bflags[@]}"} -o "$bin.tmp.$$" "$file") > "$out.build.log" 2>&1; then
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
  (cd "$goroot/test" && GOMEMLIMIT=${WAI_GOMEMLIMIT:-1200MiB} ARBACE_MEM_GIB=0 limited "$((tmo * ${WAI_GO_SLOW:-3}))" "$wai_exe" run \
     --dir "$goroot/test=$goroot/test:ro" --dir "/tmp=$scratch" --env "PWD=$goroot/test" --env TMPDIR=/tmp ${env[@]+"${env[@]}"} \
     "$w" "$@" > "$o.out" 2> "$o.err.raw" < "$gorun_dir/empty") || st=$?
  rm -rf "$scratch"
  stderr_head "$o.err.raw" > "$o.err"
  echo "$st" > "$o.exit"
}

# gorun_one ID: one program's row (ID go/PATH for $GOROOT/test's, go/own/NAME for Go's own small programs)
gorun_one() {
  local id=$1 p=${1#go/} tmo=10 bflags=() args=() why ref x o st want
  o=$gorun_work/run/${p//\//__}
  mkdir -p "$gorun_work/run"
  case $p in
    own/*)
      local src=$root/test/wasm/go/${p#own/}
      gorun_build "$src" main.go "$o" || { printf '%s\tfail\tbuild: %s\n' "$id" "$(cat "$o.why")"; return 0; }
      gorun_wai "$o" "$bin"
      st=$(cat "$o.exit")
      if [ "$st" != 0 ]; then printf '%s\tfail\tstatus %s: %s\n' "$id" "$st" "$(head -c 200 "$o.err" | tr '\t\n' '  ')"
      elif ! cmp -s "$o.out" "$src/want.out"; then printf '%s\tfail\tstandard output differs from want.out: %s\n' "$id" "$(head -c 120 "$o.out" | tr '\t\n' '  ')"
      elif [ -s "$o.err" ]; then printf '%s\tfail\tstandard error not empty: %s\n' "$id" "$(head -c 200 "$o.err" | tr '\t\n' '  ')"
      else printf '%s\tpass\t\n' "$id"; fi
      return 0 ;;
  esac
  gorun_parse "$p" || { printf '%s\t%s\n' "$id" "$why"; return 0; }
  gorun_ref "$p" || return 0
  x=$(gorun_ref_fails "$ref")
  [ -z "$x" ] || { printf '%s\tskip\tgc'"'"'s own run does not pass here: %s\n' "$id" "$x"; return 0; }
  gorun_build "$goroot/test/$(dirname "$p")" "$(basename "$p")" "$o" || {
    x=$(cat "$o.why")
    case $x in
      *"build constraints exclude all Go files"*) printf '%s\tskip\tbuild constraints exclude wasip1/wasm: %s\n' "$id" "$x" ;;
      *) printf '%s\tfail\tbuild: %s\n' "$id" "$x" ;;
    esac
    return 0; }
  gorun_wai "$o" "$bin" ${args[@]+"${args[@]}"}
  gorun_judge "$id" "$o" "$ref"
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

# the corpus's ids: Go's own small programs, then $GOROOT/test's run programs (goi's record's ids whose action is run)
gorun_ids() {
  sed -n 's/^[^#;].*/&/p' "$root/test/wasm/go-run.txt" | sed '/^$/d' | sed 's|^|go/own/|'
  awk -F'"' '/^ \["/{print $2}' "$root/test/goi/check-linux-amd64.edn" | while read -r p; do
    [ -f "$goroot/test/$p" ] || continue
    [ "$(gorun_action_of "$goroot/test/$p" | awk '{print $1}')" = run ] && echo "go/$p"
  done
}

gorun_tsv2edn() {
  awk -F'\t' '{ r=$3; gsub(/\\/, "(backslash)", r); gsub(/"/, "'"'"'", r); gsub(/[^ -~]/, "?", r)
                printf "[\"%s\" :%s \"%s\"]\n", $1, $2, r }' "$1"
}

# gorun_check RECORD LIST: the corpus (or LIST's ids, a path under $GOROOT/test or own/NAME a line) run and judged
gorun_check() {
  local record=$1 list=$2 fails=0 jobs=${WAI_GO_JOBS:-3}
  gorun_work=$wai_dir/go; rm -rf "$gorun_work"; mkdir -p "$gorun_work" "$gorun_dir"
  : > "$gorun_dir/empty"
  if [ -n "$list" ]; then grep -v '^#' "$list" | sed '/^$/d; s|^|go/|' > "$gorun_work/ids"; else gorun_ids > "$gorun_work/ids"; fi
  light
  echo "wai go: $(wc -l < "$gorun_work/ids") programs ($jobs at a time)"
  export root main goroot wai_exe wai_dir gorun_work gorun_dir TMPDIR
  xargs -P "$jobs" -I{} bash -c '. "$root/bin/lib/wai/go-run.bash"; gorun_one "$1"' _ {} < "$gorun_work/ids" > "$gorun_work/rows.tsv"
  LC_ALL=C sort -o "$gorun_work/rows.tsv" "$gorun_work/rows.tsv"
  gorun_tsv2edn "$gorun_work/rows.tsv" > "$gorun_work/rows.edn"
  gorun_controls_run || fails=$((fails + 1))
  if [ -n "$list" ]; then
    arbace_tool -e "(let [run (set (map first (read-string (str \"[\" (slurp \"$gorun_work/rows.edn\") \"]\")))) old (read-string (slurp \"$gorun_results\"))] (spit \"$gorun_work/old.edn\" (pr-str (vec (filter #(run (first %)) old)))))"
    judge "$gorun_work/old.edn" "$gorun_work/rows.edn" "$gorun_classes" "" "" go || fails=$((fails + 1))
  else
    judge "$gorun_results" "$gorun_work/rows.edn" "$gorun_classes" "$record" "go" go || fails=$((fails + 1))
  fi
  [ $fails = 0 ]
}

# the negative controls (GTS81 (a)): `vs OTHER`, the subject's run on wai judged against another program's gc reference, must fail;
# `self`, the subject's run against its own, must pass (a _check/ line)
gorun_controls_run() {
  local cid subj mode other v fails=0 tmo bflags args why ref o
  while read -r cid subj mode other; do
    case $cid in ''|'#'*) continue ;; esac
    o=$gorun_work/run/${subj//\//__}
    [ -f "$o.exit" ] || { echo "control $cid: left (its subject was not run)"; continue; }
    local target=${other:-$subj}
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
