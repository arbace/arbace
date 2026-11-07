# C++'s run corpora's reference (doc/cpp/CPP-SURVEY.md, "As built: cpp-90"; the user's decision of 2026-11-07), sourced by
# bin/lib/cppi/check and check-aot after $root, bin/lib/tmpdir.bash, bin/lib/tools.bash and bin/lib/cppi/lists.bash:
#   cppi_run_ref  prints the reference file the judges read: by default .tmp/cppi-selfref/KEY.edn, test/cpp/ref-gxx15.edn's cases
#                 with a verdict of their own (g++.dg's and libstdc++'s dg-do run tests: exit 0) judged by it (test/cpp/selfref.ae,
#                 its exceptions test/cpp/selfref-exceptions.edn) and the others (samples, runtime, dg-output files) by g++'s run;
#                 CPPI_JUDGE=gxx: test/cpp/ref-gxx15.edn itself (the oracle tier's judge, by hand)
cppi_run_ref() {
  if [ "${CPPI_JUDGE:-self}" = gxx ]; then echo test/cpp/ref-gxx15.edn; return 0; fi
  local k out ex
  ex=test/cpp/selfref-exceptions.edn; [ -f "$root/$ex" ] || ex=
  k=$( (cd "$root" && sha256sum test/cpp/ref-gxx15.edn test/cpp/selfref.ae $ex test/cpp/corpora.edn) | sha256sum | cut -c1-16)
  out=$root/.tmp/cppi-selfref/$k.edn
  if [ ! -s "$out" ]; then
    mkdir -p "$root/.tmp/cppi-selfref"
    cppi_extract || return 1
    (cd "$root" && arbace_tool -p "$root/test" -m cpp.selfref ref "{:gxx-dg \"$cppi_gxxdg\" :libstdcxx \"$cppi_ts\"}" \
       test/cpp/ref-gxx15.edn "$out.$$") > "$root/.tmp/cppi-selfref/$k.log" 2>&1 && mv "$out.$$" "$out" \
      || { echo "cppi: the run corpora's own verdicts could not be read ($root/.tmp/cppi-selfref/$k.log)" >&2; rm -f "$out.$$"; return 1; }
    grep '^selfref:' "$root/.tmp/cppi-selfref/$k.log" >&2
  fi
  echo "$out"
}
