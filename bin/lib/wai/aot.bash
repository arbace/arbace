# w2g's AOT programs (doc/wasm/WAI.md §9; doc/wasm/WASM-SURVEY.md §7, WAS10 (a), WA7), sourced by bin/wai: modules translated by
# w2g (`waidriver w2g`, wai's light driver) into one Go file of package wai, put beside wai's printed sources in a copy of the light
# driver's module and built by TamaGo's go alone into an AOT program, the light driver with every translated function compiled
# (its verbs the driver's: wast, control, run). Kept by key (the light driver's sources' digest and the generated file's) in
# $wai_dir/aot-bin (the testsuite's batches) and the main checkout's .tmp/wai-go/aot (the Go corpus's programs). Expects bin/wai's
# root, main, goroot, wai_dir, wai_exe and light.

# aot_exe OUT: the AOT program of w2g's packages OUT (waidriver w2g's OUTDIR: OUT/pN, OUT/imports.go), built when its key has
# none, sets aexe; its build's log OUT.build.log. The packages are put under the module's w2gout/, their imports into the driver's
# package main; go build compiles them one at a time (-p 1: a package's compilation the memory's measure)
aot_exe() {
  local gen=$1 key dir out
  key=$( { if [ -n "${WAI_SRCS_KEY:-}" ]; then echo "$WAI_SRCS_KEY"; else srcs_key; fi; head -1 "$goroot/VERSION"
           (cd "$gen" && find . -type f | LC_ALL=C sort | xargs sha256sum); } | sha256sum | cut -c1-32)
  dir=${AOT_BIN_DIR:-$wai_dir/aot-bin}
  aexe=$dir/$key/waiaot
  [ -x "$aexe" ] && return 0
  out=$dir/$key.tmp.$$
  mkdir -p "$out"
  cp -R "$wai_dir/print/mod" "$out/mod"
  mkdir -p "$out/mod/w2gout"
  cp -R "$gen"/. "$out/mod/w2gout/"
  mv "$out/mod/w2gout/imports.go" "$out/mod/waidriver/zz_w2g_imports.go"
  if ! (cd "$out/mod" && export GOROOT="$goroot" GOTOOLCHAIN=local GOFLAGS= GOWORK=off GO111MODULE=on GOPROXY=off CGO_ENABLED=0 \
          GOCACHE="${WAI_GOCACHE:-$wai_dir/gocache}" GOMAXPROCS=${GOMAXPROCS:-4} GOMEMLIMIT=${WAI_AOT_GOMEMLIMIT:-1500MiB} GOGC=${WAI_AOT_GOGC:-50} &&
          "$goroot/bin/go" build -p 1 -gcflags="arbace/w2gout/...=-c=${WAI_AOT_GC_C:-2}" -trimpath -o "$out/waiaot" ./waidriver/) > "$gen.build.log" 2>&1; then
    rm -rf "$out"
    return 1
  fi
  mkdir -p "$dir/$key"
  mv -f "$out/waiaot" "$aexe"
  rm -rf "$out"
}

# aot_run_batches WORK: the testsuite's plans (WORK/plans.list) run as AOT programs, WAI_AOT_BATCH scripts a program (default 24):
# each batch's modules (WORK/mods/SLUG) translated, the program built and run; the rows into WORK/rows.raw, each batch's w2g
# summary into WORK/aot/bN/stats.tsv, a script's program named in WORK/aot/exe.tsv (PLAN<tab>EXE, the controls')
aot_run_batches() {
  local work=$1 n=${WAI_AOT_BATCH:-24} b=0 i=0 bd line slug
  mkdir -p "$work/aot"
  : > "$work/rows.raw"; : > "$work/aot/exe.tsv"
  split -l "$n" -d -a 3 "$work/plans.list" "$work/aot/batch."
  for line in "$work"/aot/batch.*; do
    bd=$work/aot/b${line##*.}
    mkdir -p "$bd"
    : > "$bd/mods.list"
    while IFS=$'\t' read -r _ _ mdir; do
      [ -d "$mdir" ] && find "$mdir" -name '*.wasm' | LC_ALL=C sort >> "$bd/mods.list"
    done < "$line"
    rm -rf "$bd/gen"
    GOMEMLIMIT=${WAI_GOMEMLIMIT:-1500MiB} "$wai_exe" w2g "$bd/gen" --stats "$bd/stats.tsv" "@$bd/mods.list"
    if ! aot_exe "$bd/gen"; then
      echo "FAIL: wai check --aot: batch ${line##*.}'s program does not build: $(grep -v '^#' "$bd/gen.build.log" | head -2 | tr '\n' ' ' | cut -c1-300)"
      return 1
    fi
    echo "wai check --aot: batch ${line##*.}: $(wc -l < "$line") scripts, $(wc -l < "$bd/stats.tsv") modules, $(awk -F'\t' '{f+=$2; t+=$3} END {print t " of " f}' "$bd/stats.tsv") functions translated"
    GOMEMLIMIT=${WAI_GOMEMLIMIT:-1500MiB} timeout "${WAI_TIMEOUT:-1800}" "$aexe" wast "$line" "$bd/rows.raw"
    cat "$bd/rows.raw" >> "$work/rows.raw"
    while IFS=$'\t' read -r _ plan _; do printf '%s\t%s\n' "$plan" "$aexe"; done < "$line" >> "$work/aot/exe.tsv"
  done
  cat "$work"/aot/b*/stats.tsv > "$work/aot/stats.tsv"
  awk -F'\t' '{f+=$2; t+=$3; if ($4 != "") { n = split($4, xs, ","); for (i = 1; i <= n; i++) { split(xs[i], kv, "="); left[kv[1]] += kv[2] } } }
              END { printf "wai check --aot: %d modules, %d of %d functions translated by w2g", NR, t, f
                    for (k in left) printf ", %d left (%s)", left[k], k; printf "\n" }' "$work/aot/stats.tsv"
}

# aot_go_exe WASM OUT: a Go program's AOT program (the corpus's, bin/wai go --aot): with WAI_AOT_SHARED_EXE (WAS50 (a), the
# default) that program, the run's modules translated by shape into one (aot_go_shared); else (WAI_AOT_SHARED=0, WA7's) its module
# translated alone (OUT.gen, the summary OUT.w2g.tsv) and built, kept by key in the main checkout's .tmp/wai-go/aot; sets aexe, or 1
# with OUT.why
aot_go_exe() {
  local w=$1 o=$2
  # WAS50 (a): the run's first pass collects the modules (OUT.mod); the second runs them all on the one program of their shapes
  if [ -n "${gorun_collect:-}" ]; then echo "$w" > "$o.mod"; echo "collected" > "$o.why"; return 1; fi
  if [ -n "${WAI_AOT_SHARED_EXE:-}" ]; then aexe=$WAI_AOT_SHARED_EXE; return 0; fi
  rm -rf "$o.gen"
  if ! GOMEMLIMIT=${WAI_GOMEMLIMIT:-1500MiB} "$wai_exe" w2g "$o.gen" --stats "$o.w2g.tsv" "$w" 2> "$o.w2g.err"; then
    echo "w2g: $(head -1 "$o.w2g.err" | cut -c1-200)" > "$o.why"; return 1
  fi
  AOT_BIN_DIR=$gorun_dir/aot aot_exe "$o.gen" || {
    echo "w2g: the AOT program does not build: $(grep -v '^#' "$o.gen.build.log" | head -1 | cut -c1-200)" > "$o.why"; return 1; }
  rm -rf "$o.gen"
}

# aot_go_verdict ID OUT: an AOT run's row (gorun_judge's or gorun_dir_one's, on standard input) made a fail when the interpreter
# ran any of the program's functions (OUT.aot, the driver's WAI_AOT_STATS)
aot_go_verdict() {
  local id=$1 o=$2 r n
  r=$(cat)
  case $r in
    *"	pass	"*)
      n=$(sed -n 's/^interpreted //p' "$o.aot" 2>"$TMPDIR/aot-stats.err")
      if [ "${n:-x}" = 0 ]; then printf '%s\n' "$r"
      else printf '%s\tfail\tw2g: %s calls on the interpreter (%s)\n' "$id" "${n:-no stats}" "$(sed -n 's/^why //p' "$o.aot" 2>"$TMPDIR/aot-stats.err")"; fi ;;
    *) printf '%s\n' "$r" ;;
  esac
}

# aot_go_shared WORK: WAS40 (c), WAS50 (a): the modules the run's first pass collected (WORK/run/*.mod) translated by shape (w2g
# --shared: each shape of the programs' functions once, their runtime and libraries shared) into one AOT program, kept by key in
# the main checkout's .tmp/wai-go/aot-shared (the newest WAI_AOT_SHARED_KEEP, default 3, kept); exports WAI_AOT_SHARED_EXE, or 1
aot_go_shared() {
  local work=$1 gen=$1/shared.gen t0 d n
  find "$work/run" -name '*.mod' -exec cat {} + | LC_ALL=C sort -u > "$work/mods.list"
  echo "wai go --aot: $(wc -l < "$work/mods.list") modules translated by shape into one program"
  t0=$(date +%s)
  rm -rf "$gen"
  if ! GOMEMLIMIT=${WAI_GOMEMLIMIT:-1500MiB} "$wai_exe" w2g "$gen" --shared --stats "$work/shared.tsv" "@$work/mods.list" 2> "$work/shared.err"; then
    echo "FAIL: wai go --aot: w2g --shared: $(head -1 "$work/shared.err" | cut -c1-200)"; return 1
  fi
  awk -F'\t' '{f+=$2; t+=$3; a+=$5} END {printf "wai go --aot: %d of %d functions translated, %d shapes\n", t, f, a}' "$work/shared.tsv"
  mkdir -p "$gorun_dir/aot-shared"
  if ! AOT_BIN_DIR=$gorun_dir/aot-shared aot_exe "$gen"; then
    echo "FAIL: wai go --aot: the shared program does not build: $(grep -v '^#' "$gen.build.log" | head -2 | tr '\n' ' ' | cut -c1-300)"; return 1
  fi
  echo "wai go --aot: the shared program $(du -m "$aexe" | cut -f1) MB, translated and built in $(( $(date +%s) - t0 )) s"
  rm -rf "$gen"
  touch "$(dirname "$aexe")"
  n=0
  for d in $(ls -t "$gorun_dir/aot-shared"); do
    case $d in *.tmp.*) continue ;; esac
    n=$((n + 1)); [ $n -le "${WAI_AOT_SHARED_KEEP:-3}" ] || rm -rf "${gorun_dir:?}/aot-shared/$d"
  done
  export WAI_AOT_SHARED_EXE=$aexe
}
