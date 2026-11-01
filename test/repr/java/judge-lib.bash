# Shared by test/repr/java/judge-*-test.bash (GT7's audit 061): a fabricated checkout (copies of the judges'
# scripts, a stubbed bin/repr-java, a small jdk26u tree) in which a judge runs without javac's tool or the real
# corpora. Sourced, with $root the real checkout. Defines make_fake_root DIR, ok/bad (counted), finish.

fails=0
ok()  { echo "ok   $1"; }
bad() { echo "FAIL $1"; fails=$((fails + 1)); }
# expect_rc DESC WANTED-ZERO(0|1) RC: 0 wants rc 0, 1 wants rc nonzero
expect_rc() { if { [ "$2" = 0 ] && [ "$3" = 0 ]; } || { [ "$2" = 1 ] && [ "$3" != 0 ]; }; then ok "$1"; else bad "$1 (rc $3)"; fi; }
finish() { [ $fails = 0 ] && { echo "all passed"; exit 0; } || { echo "$fails failed"; exit 1; }; }

# make_fake_root DIR: DIR/bin/lib/repr-java and DIR/test/repr/java copied from the checkout ($root), a stub
# DIR/bin/repr-java, DIR/jdk/src/m1/share/classes/a/A.java (module m1) and m2. The stub's read-jdk writes one
# forms file whose header names the canonical source directory and, with --no-javac, prints the "no javac:"
# line with $STUB_FALLBACKS fallbacks (default 0); main -m ...jtree check/mutate are the callers' to stub.
make_fake_root() {
  local d=$1 m
  mkdir -p "$d/bin/lib" "$d/test/repr/java/expected" "$d/jdk/src"
  cp -r "$root/bin/lib/repr-java" "$d/bin/lib/repr-java"
  cp "$root/test/repr/java/"*.bash "$d/test/repr/java/" 2> /dev/null
  cp "$root"/test/repr/java/*-known.edn "$d/test/repr/java/" 2> /dev/null
  for f in "$root"/test/repr/java/*.edn; do cp "$f" "$d/test/repr/java/" 2> /dev/null; done
  for m in m1 m2; do
    mkdir -p "$d/jdk/src/$m/share/classes/a"
    echo "class A$m {}" > "$d/jdk/src/$m/share/classes/a/A.java"
  done
  cat > "$d/bin/repr-java" <<'STUB'
#!/bin/bash
cmd=$1; shift
case $cmd in
  read-jdk)
    nj=; while [ $# -gt 0 ]; do case $1 in --no-javac) nj=1; shift ;; --*) shift ;; *) break ;; esac; done
    out=$1 mod=$2 src=$3
    canon=$(cd "$src" && pwd -P)
    mkdir -p "$out/a"
    printf ';; Converted from %s/a/A.java by arbace.repr.java. Regenerate rather than edit.\n\n(x %s)\n' "$canon" "$mod" > "$out/a/A.ae"
    if [ -n "$nj" ] && [ -z "${STUB_NOLINE:-}" ]; then
      echo '{:files 3}' > "$out/repr-java-nojavac.edn"
      fb=${STUB_FALLBACKS:-0}
      echo "no javac: 3 files, $((3 - fb)) native, $fb fallbacks to javac; {}"
    fi
    echo "1 Java files, 1 converted, 0 failed, 0 javac errors, 1 read back, 0 unreadable in 0.1s" ;;
  *) echo "stub: $cmd" >&2; exit 2 ;;
esac
STUB
  chmod +x "$d/bin/repr-java"
}

# make_fake_parse_root DIR: make_fake_root, plus what parse-check needs: a stub bin/lib/tools.bash (arbace_tool
# prints nothing: the known files are empty) and a stub jtree: `main -m arbace.repr.java.jtree check OUT ROOT
# PATH...` writes OUT/files.tsv (one equal file), OUT/report.edn with :javac-diagnostics $STUB_DIAG (default 0)
# and a log line; `mutate OUT SRC N K E SEED MODULE...` writes OUT/m1/share/classes/a/A.java holding $STUB_MUT.
make_fake_parse_root() {
  local d=$1
  make_fake_root "$d"
  cp "$root/bin/lib/renamed-env.bash" "$d/bin/lib/"
  printf 'arbace_tool() { :; }\nfrozen_clojure() { echo "$root/test/repr/java"; }\n' > "$d/bin/lib/tools.bash"
  cat > "$d/bin/repr-java" <<'STUB'
#!/bin/bash
cmd=$1; shift
case $cmd in
  main)
    [ "$1" = -m ] && [ "$2" = arbace.repr.java.jtree ] || exit 2
    verb=$3; shift 3
    case $verb in
      check)
        out=$1; mkdir -p "$out"
        printf 'x/A.java\tequal\n' > "$out/files.tsv"
        printf '{:files 1,\n :equal 1,\n :javac-diagnostics %s,\n :nodes 1}\n' "${STUB_DIAG:-0}" > "$out/report.edn"
        echo "1 files, 1 equal, 0 differ, 0 native errors, 0 javac errors" ;;
      mutate)
        out=$1; mkdir -p "$out/m1/share/classes/a"
        printf '%s\n' "${STUB_MUT:-class A {}}" > "$out/m1/share/classes/a/A.java"
        echo "8000 mutants" ;;
      *) exit 2 ;;
    esac ;;
  *) echo "stub: $cmd" >&2; exit 2 ;;
esac
STUB
  chmod +x "$d/bin/repr-java"
  mkdir -p "$d/test/repr/java/java" "$d/test/repr/java/module"
}
