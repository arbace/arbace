# The run corpora's reference (doc/C23-SURVEY.md, "Decided (the user, 2026-11-07): the judge"), sourced by bin/lib/ci/check
# and check-aot after $root, bin/lib/tmpdir.bash and bin/lib/tools.bash:
#   c_run_ref   prints the reference file the judges read: by default .tmp/ci-selfref/KEY.edn, test/c/ref-gcc15.edn's cases with
#               a verdict of their own judged by it (test/c/selfref.ae, its exceptions test/c/selfref-exceptions.edn) and the
#               others by gcc's run; ARBACE_C_JUDGE=gcc: test/c/ref-gcc15.edn itself (the oracle tier's judge)
c_run_ref() {
  if [ "${ARBACE_C_JUDGE:-self}" = gcc ]; then echo test/c/ref-gcc15.edn; return 0; fi
  local k out modcache gccroot
  k=$( (cd "$root" && sha256sum test/c/ref-gcc15.edn test/c/selfref.ae test/c/selfref-exceptions.edn test/c/corpora.edn) | sha256sum | cut -c1-16)
  out=$root/.tmp/ci-selfref/$k.edn
  if [ ! -s "$out" ]; then
    mkdir -p "$root/.tmp/ci-selfref"
    modcache=$(GOFLAGS= "${ARBACE_GOROOT:-/root/tamago-go}/bin/go" env GOMODCACHE)
    gccroot=$("$root/bin/repr-c" gcc-tests) || return 1
    (cd "$root" && arbace_tool -p "$root/test" -m c.selfref ref "{:gomodcache \"$modcache\" :gcc-15.2.0 \"$gccroot\" :checkout \"$root\"}" \
       test/c/ref-gcc15.edn "$out.$$") > "$root/.tmp/ci-selfref/$k.log" 2>&1 && mv "$out.$$" "$out" \
      || { echo "ci: the run corpora's own verdicts could not be read ($root/.tmp/ci-selfref/$k.log)" >&2; rm -f "$out.$$"; return 1; }
    grep '^selfref:' "$root/.tmp/ci-selfref/$k.log" >&2
  fi
  echo "$out"
}
