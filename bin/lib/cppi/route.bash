# The programs' IR route of bin/cppi check and check --aot (doc/cpp/CPP-SURVEY.md, "As built: cpp-90"; CP240), sourced after
# bin/lib/tools.bash, with ir1 (the oracle's IR of one program) defined and exported and work set:
#   cppi_ir_route TODO JOBS   each program of TODO (ID STD ARGS PATH) its IR in $work/files/ID/x.ll and ir.status, by
#                             CPPI_ROUTE: clang (the default: clang++ -O0 -S -emit-llvm, ir1), ours (Arbace's front end and code
#                             generator, arbace.cpp.codegen gen-batch, no clang: a program it refuses is ir.status 98, its class in
#                             ir.log), auto (ours for a program with no options of its dg-options, clang++ for the rest and for
#                             those ours refuses). $work/files/ID/route says which made the IR.
# The default route (CP240 (b), decided 2026-11-07): clang until the night's comparison (bin/cppi route-compare over the samples, the
# runtime and gxx-dg-run's sampling, eval and AOT) holds every pass; then this one word becomes auto, the records are written again
# (--record) and bin/lib/cppi/check's header names it (doc/cpp/CPP-SURVEY.md, "As built: cpp-target")
cppi_route_default=clang
cppi_ir_route() {
  local todo=$1 jobs=$2 route=${CPPI_ROUTE:-$cppi_route_default} rest
  case $route in clang|ours|auto) ;; *) echo "cppi: CPPI_ROUTE is clang, ours or auto" >&2; return 2 ;; esac
  rest=$todo
  if [ "$route" != clang ]; then
    rest=$todo.clang; : > "$rest"
    awk -F'\t' -v w="$work" -v r="$route" '{ if (r == "auto" && $3 != "") next; print $4 "\t" $2 "\t" w "/files/" $1 }' "$todo" > "$todo.ours"
    awk -F'\t' '{ print $3 }' "$todo.ours" | while IFS= read -r d; do rm -rf "$d"; mkdir -p "$d"; done
    if [ -s "$todo.ours" ]; then
      # libstdc++'s headers from the tree made without g++ (CP241 (b); REPR_CPP_HEADERS=host: the host's installed tree)
      . "$root/bin/lib/repr-cpp/sysroot.bash"; cpp_sysroot || return 1
      GOMEMLIMIT=${GOMEMLIMIT:-1500MiB} arbace_tool -m arbace.cpp.codegen gen-batch "$todo.ours" > "$todo.ours.log" 2>&1 || { tail -5 "$todo.ours.log" >&2; return 1; }
    fi
    # the fields read with a separator that is no whitespace (cpp-target): IFS=tab collapsed an empty options field, so a program ours
    # refused that has no options fell back to clang with its path as its options (its pack's key then STALE)
    tr '\t' '\037' < "$todo" > "$todo.us"
    while IFS=$'\037' read -r i s a p; do
      local d=$work/files/$i
      if [ -f "$d/gen.ll" ] && [ ! -f "$d/gen.refused" ]; then
        cp "$d/gen.ll" "$d/x.ll"; echo 0 > "$d/ir.status"; echo ours > "$d/route"; echo 0 > "$d/ir.secs"
      elif [ "$route" = ours ]; then
        mkdir -p "$d"; echo 98 > "$d/ir.status"; echo ours > "$d/route"
        { printf 'our route refused: '; cat "$d/gen.refused" 2>"$TMPDIR/cppi-route.err" || echo "not generated"; } > "$d/ir.log"
      else
        # CP263 (a): our refusal kept apart (ir1 makes the directory afresh) and put back beside clang's IR or rejection
        if [ -f "$d/gen.refused" ]; then mkdir -p "$(dirname "$work/ours-refused/$i")"; cp "$d/gen.refused" "$work/ours-refused/$i"; fi
        printf '%s\t%s\t%s\t%s\n' "$i" "$s" "$a" "$p" >> "$rest"
      fi
    done < "$todo.us"
  fi
  # shellcheck disable=SC2016
  tr '\t\n' '\037\0' < "$rest" | xargs -0 -r -P "$jobs" -I{} nice bash -c 'IFS=$'"'"'\037'"'"' read -r i s a p <<< "$1"; ir1 "$i" "$s" "$a" "$p"; echo clang > "$work/files/$i/route"' _ {}
  # CP263 (a): a program our route refused, its IR then clang's or clang's rejection: arbace.cpp.check's ir-row reads gen.refused
  if [ -d "$work/ours-refused" ]; then
    (cd "$work/ours-refused" && find . -type f) | sed 's|^\./||' | while IFS= read -r i; do
      [ -d "$work/files/$i" ] && cp "$work/ours-refused/$i" "$work/files/$i/gen.refused"
    done
    rm -rf "$work/ours-refused"
  fi
  echo "cppi: the IR of $(wc -l < "$todo") programs by route: $(find "$work/files" -name route -newer "$todo" -exec cat {} + | sort | uniq -c | awk '{printf "%s%s %s", (n++ ? ", " : ""), $2, $1}') (CPPI_ROUTE=$route)" >&2
}
