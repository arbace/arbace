#!/bin/bash
# The scripts that look for the gate's build by hand default to ARBACE_GATE_EXES/KEY20 (bin/lib/
# gate-cache.bash, doc/GATE-SURVEY.md, GTS120), not to the .tmp/arbace-go-gate nothing populates since
# GTS24 (a); and a miss of the gate's build never removes a directory of another key.
# Fabricated trees under .tmp/gate-cache-test, no build. Usage: test/gate/gate-cache-test.bash
set -uo pipefail
here=$(cd "$(dirname "$0")/../.." && pwd)
export TMPDIR=${TMPDIR:-$here/.tmp/tmp}
mkdir -p "$TMPDIR"
T=$here/.tmp/gate-cache-test
rm -rf "$T"; mkdir -p "$T"
fail=0
ok() { echo "ok   $*"; }
bad() { echo "FAIL $*"; fail=1; }
KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
K20=${KEY:0:20}
OTHER=ffffffffffffffffffff

# a fabricated checkout: the scripts under test, the libraries, a fake bin/arbace-go
mk_tree() {  # mk_tree DIR
  local r=$1
  mkdir -p "$r/bin/lib" "$r/.tmp" "$r/exes/$K20/amd64"
  cp "$here"/bin/lib/*.bash "$r/bin/lib/"
  cp "$here/bin/diffcheck" "$here/bin/generated-check" "$r/bin/"
  cat > "$r/bin/arbace-go" <<EOS
#!/bin/bash
case \$1 in --gate-key) echo $KEY ;; --gate-build) exit 0 ;; esac
EOS
  chmod +x "$r/bin/arbace-go"
  # the executable at the key records that it ran, and fails
  cat > "$r/exes/$K20/amd64/arbace" <<EOS
#!/bin/bash
echo "\$0" >> "$r/ran"
exit 1
EOS
  chmod +x "$r/exes/$K20/amd64/arbace"
}
unset ARBACE_GATE_CACHE

# 1. the shared function
r=$T/lib; mk_tree "$r"
if [ -f "$here/bin/lib/gate-cache.bash" ]; then
  got=$(ARBACE_GATE_EXES=$r/exes bash -c ". '$here/bin/lib/gate-cache.bash'; arbace_gate_cache '$r'")
  [ "$got" = "$r/exes/$K20" ] && ok "arbace_gate_cache: EXES/KEY20" || bad "arbace_gate_cache gave '$got'"
  got=$(ARBACE_GATE_CACHE=/x/y ARBACE_GATE_EXES=$r/exes bash -c ". '$here/bin/lib/gate-cache.bash'; arbace_gate_cache '$r'")
  [ "$got" = /x/y ] && ok "arbace_gate_cache: ARBACE_GATE_CACHE overrides" || bad "override gave '$got'"
  git init -q "$T/main" && git -C "$T/main" commit -q --allow-empty -m x -c user.name=t -c user.email=t@t 2>/dev/null
  got=$(bash -c ". '$here/bin/lib/gate-cache.bash'; arbace_main_checkout '$T/main'")
  [ "$got" = "$T/main" ] && ok "arbace_main_checkout: a plain checkout" || bad "main checkout gave '$got'"
else
  bad "bin/lib/gate-cache.bash is missing"
fi

# 2. bin/diffcheck, 3. bin/generated-check: the executable they run is the gate's
for s in diffcheck generated-check; do
  r=$T/$s; mk_tree "$r"; rm -f "$r/ran"
  (cd "$r" && ARBACE_GATE_EXES=$r/exes "$r/bin/$s" > "$r/out" 2>&1)
  if [ -f "$r/ran" ] && grep -qx "$r/exes/$K20/amd64/arbace" "$r/ran"; then ok "$s runs EXES/KEY20/amd64/arbace"
  else bad "$s did not run the gate's executable (ran: $(cat "$r/ran" 2>&1 | head -1))"; fi
done

# function extraction: the text of NAME() { ... } of a script
fn() { awk -v n="$2" '$0 ~ "^"n"\\(\\) \\{" {p=1} p {print} p && /^}/ {exit}' "$1"; }

# 4. java-check's run_exe
r=$T/jc; mk_tree "$r"
got=$(root=$r ARBACE_GATE_EXES=$r/exes JAVA_CHECK_EXE= bash -c "
  . '$r/bin/lib/gate-cache.bash' 2>/dev/null
  $(fn "$here/bin/java-check" run_exe)
  run_exe" 2>&1)
[ "$got" = "$r/exes/$K20/amd64/arbace" ] && ok "java-check run_exe: EXES/KEY20" || bad "run_exe gave '$got'"

# 5. program_base
r=$T/pb; mk_tree "$r"
mkdir -p "$r/exes/$K20/amd64/aot"; touch "$r/exes/$K20/amd64/aot/packages.edn"
cat > "$r/exes/$K20/amd64/arbace" <<'EOS'
#!/bin/bash
printf rk
EOS
got=$(root=$r dir=$r/none programs=$r/programs ARBACE_GATE_EXES=$r/exes bash -c "
  . '$r/bin/lib/gate-cache.bash' 2>/dev/null
  aot_key() { echo k; }; arbace_runtime_key() { echo rk; }; host_arch() { echo amd64; }
  program_aot_current() { [ -f \"\$1/packages.edn\" ]; }
  $(fn "$here/bin/lib/program.bash" program_base)
  program_base 2>&1 >/dev/null | head -3" 2>&1)
echo "$got" | grep -q "compiled namespaces: $r/exes/$K20/amd64/aot " && ok "program_base: EXES/KEY20/amd64" || bad "program_base gave: $got"

# 6. clojure-tests --aot without CLOJURE_TESTS_AOT_EXES
r=$T/ct; mk_tree "$r"
got=$(root=$r work=$r/w ARBACE_GATE_EXES=$r/exes CLOJURE_TESTS_AOT_EXES= bash -c "
  . '$r/bin/lib/gate-cache.bash' 2>/dev/null
  program_aot_current() { echo \"aot-dir=\$1\"; return 1; }
  $(fn "$here/bin/clojure-tests" aot_build)
  aot_build" 2>&1)
echo "$got" | grep -q "aot-dir=$r/exes/$K20/amd64/aot" && ok "clojure-tests --aot: EXES/KEY20" || bad "aot_build gave: $got"

# 7. a miss never removes a directory of another key
r=$T/gb; mk_tree "$r"
mkdir -p "$r/exes/$OTHER/amd64"; touch "$r/exes/$OTHER/amd64/arbace" "$r/exes/$OTHER/amd64/arbace-tests"; echo "$OTHER" > "$r/exes/$OTHER/key"
got=$(root=$r discard=$r/discard ARBACE_GATE_CACHE=$r/exes/$OTHER bash -c "
  gate_key() { echo $KEY; }; build() { return 1; }; since() { echo 0; }
  $(fn "$here/bin/arbace-go" gate_build)
  gate_build" 2>&1)
[ -f "$r/exes/$OTHER/amd64/arbace" ] && ok "gate_build: another key's directory kept" || bad "gate_build removed another key's directory"

exit $fail
