# The programs' IR route of bin/cppi check and check --aot (doc/cpp/CPP-SURVEY.md, "As built: cpp-90"; CP240), sourced after
# bin/lib/tools.bash, with ir1 (the oracle's IR of one program) defined and exported and work set:
#   cppi_ir_route TODO JOBS   each program of TODO (ID STD ARGS PATH) its IR in $work/files/ID/x.ll and ir.status, by
#                             CPPI_ROUTE: clang (the default: clang++ -O0 -S -emit-llvm, ir1), ours (Arbace's front end and code
#                             generator, arbace.cpp.codegen gen-batch, no clang: a program it refuses is ir.status 98, its class in
#                             ir.log), auto (ours for a program with no options of its dg-options, clang++ for the rest and for
#                             those ours refuses). $work/files/ID/route says which made the IR.
cppi_ir_route() {
  local todo=$1 jobs=$2 route=${CPPI_ROUTE:-clang} rest
  case $route in clang|ours|auto) ;; *) echo "cppi: CPPI_ROUTE is clang, ours or auto" >&2; return 2 ;; esac
  rest=$todo
  if [ "$route" != clang ]; then
    rest=$todo.clang; : > "$rest"
    awk -F'\t' -v w="$work" -v r="$route" '{ if (r == "auto" && $3 != "") next; print $4 "\t" $2 "\t" w "/files/" $1 }' "$todo" > "$todo.ours"
    awk -F'\t' '{ print $3 }' "$todo.ours" | while IFS= read -r d; do rm -rf "$d"; mkdir -p "$d"; done
    if [ -s "$todo.ours" ]; then
      GOMEMLIMIT=${GOMEMLIMIT:-1500MiB} arbace_tool -m arbace.cpp.codegen gen-batch "$todo.ours" > "$todo.ours.log" 2>&1 || { tail -5 "$todo.ours.log" >&2; return 1; }
    fi
    while IFS=$'\t' read -r i s a p; do
      local d=$work/files/$i
      if [ -f "$d/gen.ll" ] && [ ! -f "$d/gen.refused" ]; then
        cp "$d/gen.ll" "$d/x.ll"; echo 0 > "$d/ir.status"; echo ours > "$d/route"; echo 0 > "$d/ir.secs"
      elif [ "$route" = ours ]; then
        mkdir -p "$d"; echo 98 > "$d/ir.status"; echo ours > "$d/route"
        { printf 'our route refused: '; cat "$d/gen.refused" 2>"$TMPDIR/cppi-route.err" || echo "not generated"; } > "$d/ir.log"
      else
        printf '%s\t%s\t%s\t%s\n' "$i" "$s" "$a" "$p" >> "$rest"
      fi
    done < "$todo"
  fi
  # shellcheck disable=SC2016
  tr '\t\n' '\037\0' < "$rest" | xargs -0 -r -P "$jobs" -I{} nice bash -c 'IFS=$'"'"'\037'"'"' read -r i s a p <<< "$1"; ir1 "$i" "$s" "$a" "$p"; echo clang > "$work/files/$i/route"' _ {}
  echo "cppi: the IR of $(wc -l < "$todo") programs by route: $(find "$work/files" -name route -newer "$todo" -exec cat {} + | sort | uniq -c | awk '{printf "%s%s %s", (n++ ? ", " : ""), $2, $1}') (CPPI_ROUTE=$route)" >&2
}
