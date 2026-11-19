# The programs' IR route of bin/cppi check and check --aot (doc/cpp/CPP-SURVEY.md, "As built: cpp-90"; CP240), sourced after
# bin/lib/tools.bash, with ir1 (the oracle's IR of one program) defined and exported and work set:
#   cppi_ir_route TODO JOBS   each program of TODO (ID STD ARGS PATH) its IR in $work/files/ID/x.ll and ir.status, by
#                             CPPI_ROUTE: clang (the default: clang++ -O0 -S -emit-llvm, ir1), ours (Arbace's front end and code
#                             generator, arbace.cpp.codegen gen-batch, no clang: a program it refuses is ir.status 98, its class in
#                             ir.log), auto (ours for a program whose options are all ones our route models, clang++ for the rest
#                             and for those ours refuses). $work/files/ID/route says which made the IR.
# The options our route models (CP350 (a), decided 2026-11-10): the preprocessor's -D, -U and -I, and -fexceptions and -fno-exceptions
# (the target's predefined macros follow them, cpp/target/dialect.edn's option deltas; the code generator's landing pads are
# behaviour-neutral where g++ accepts the program); -std= is the list's STD, and -O*, -w and -pedantic never reach ARGS (cppi_opts:
# the -O0 shape is kept, a program's behaviour is its verdict). Every program by ours is given its ARGS. Queue 439 (DECISION 439-1):
# -fstrict-enums too: clang's -O0 IR is the same with and without it (its !range metadata on enum loads is written from -O1 only,
# CodeGenFunction::EmitLoadOfScalar), no macro follows it, and our route ignores it (gxx-dg-run's torture/pr50189.C).
cppi_route_modelled() { # cppi_route_modelled ARGS: 0 when every option of ARGS is one our route models
  local o
  for o in $1; do
    case $o in -D*|-U*|-I*|-fexceptions|-fno-exceptions|-fstrict-enums) ;; *) return 1 ;; esac
  done
  return 0
}
# The default route (CP240 (b), decided 2026-11-07): clang until the night's comparison (bin/cppi route-compare over the samples, the
# runtime and gxx-dg-run's sampling, eval and AOT) holds every pass; then this one word becomes auto, the records are written again
# (--record) and bin/lib/cppi/check's header names it (doc/cpp/CPP-SURVEY.md, "As built: cpp-target")
cppi_route_default=auto
cppi_ir_route() {
  local todo=$1 jobs=$2 route=${CPPI_ROUTE:-$cppi_route_default} rest
  case $route in clang|ours|auto) ;; *) echo "cppi: CPPI_ROUTE is clang, ours or auto" >&2; return 2 ;; esac
  rest=$todo
  if [ "$route" != clang ]; then
    rest=$todo.clang; : > "$rest"
    tr '\t' '\037' < "$todo" | while IFS=$'\037' read -r i s a p; do
      if [ "$route" = auto ] && ! cppi_route_modelled "$a"; then continue; fi
      printf '%s\t%s\t%s\t%s\n' "$p" "$s" "$work/files/$i" "$a"
    done > "$todo.ours"
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
