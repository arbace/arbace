# CP134 (b): ARBACE_HOST_EXE=/bin/true is the dry mode of `bin/cppi selftest`: a test that needs the tools' host (its first line
# "# needs-host") is skipped, named in the output and in the summary; a test that does not runs and passes, and the run exits 0.
# Before: the host-less machine ran every test, and each of them failed.
. "$(dirname "$0")/lib.bash"
out=$(ARBACE_HOST_EXE=/bin/true "$root/test/cpp/selftest/run" stale-norm headers-pin 2>&1); st=$?
[ $st = 0 ] && ok "the dry run exits 0" || bad "the dry run: exit $st: $out"
echo "$out" | grep -q '^skip  stale-norm' && ok "the host test is skipped, named" || bad "no skip line for stale-norm: $out"
echo "$out" | grep -q '^pass  headers-pin' && ok "the host-free test runs" || bad "headers-pin not run: $out"
echo "$out" | grep -q '1 skipped (dry mode): stale-norm' && ok "the summary names the skipped" || bad "summary: $out"
# outside the dry mode nothing is skipped
case "$(ARBACE_HOST_EXE=/bin/false "$root/test/cpp/selftest/run" headers-pin 2>&1)" in *skipped*) bad "a skip outside the dry mode" ;; *) ok "no skipped line outside the dry mode" ;; esac
finish
