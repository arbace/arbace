# bin/javac-vendor's own ct.sym (JCS50 (b), JCS50-1 (a), the user 2026-11-20; doc/classes/JAVA-CONFORMANCE-SURVEY.md,
# "As built: JCS50 (b), the own ct.sym"): the platform's ct.sym, the releases javac on Arbace's --release N reads
# (jdk.internal.jrt.javac.ReleasePlatforms), made as make/modules/jdk.compiler/Gendata.gmk makes it, by the JDK build's own
# tool build.tools.symbolgenerator.CreateSymbols (jdk26u's make/langtools/src/classes/build/tools/symbolgenerator/
# CreateSymbols.java, read by repr/java on javac's tool executable and run there by the interpreter,
# test/javac-platform/ctsym-run.ae over gensrc-run.ae, as the own platform's interpreted build tools are) over jdk26u's
# src/jdk.compiler/share/data/symbols (the releases' history) and the own platform's modules with their exploded
# descriptors (the build's $(JDK_OUTPUTDIR)/modules/: the current release), with the build's parameters as data
# (OUT/ct-sym-build.edn: SOURCE_DATE_EPOCH, the release, VERSION_PRE and the included modules, derived from the build tree by
# ct-sym-build and re-checked by ct-sym-build --check, the oracle tier's). Sourced by bin/javac-vendor after own.bash.
#
# The pin: OUT/ct-sym.sha256, "# kind: own" (the made file's SHA-256), else a pin of the build tree's
# images/jdk/lib/ct.sym. The made file is kept in the own platform's cache (own_cache's directory, keyed by platform.sha256)
# as ct-sym-KEY20/ct.sym, KEY the digest of what it is made from (ct_sym_key): made once on a miss by the javac-tool step
# (bin/javac-vendor tool), never by a check; ct-sym-rebuild (the weekly step) makes it afresh and requires the pin;
# ct-sym-oracle (the oracle tier's) requires it equal to the build tree's, byte for byte.

cts_pin=$out/ct-sym.sha256
cts_build=$out/ct-sym-build.edn
# the JDK build's tool and data, at the platform's pin of jdk26u
cts_tool=make/langtools/src/classes/build/tools/symbolgenerator/CreateSymbols.java
cts_data=src/jdk.compiler/share/data/symbols
# CreateSymbols' javac options for its read (make/modules/jdk.compiler/Gendata.gmk's COMPILECREATESYMBOLS_ADD_EXPORTS)
cts_exports="com.sun.tools.javac.api com.sun.tools.javac.code com.sun.tools.javac.util com.sun.tools.javac.jvm"

ct_sym_kind() { if grep -qx '# kind: own' "$cts_pin" 2>"$quiet"; then echo own; else echo build; fi; }
# cts_get KEY: ct-sym-build.edn's top-level :KEY "VALUE" (a line each, one space or { before it: not :origins' lines)
cts_get() { sed -n "s/^[ {]:$1 \"\([^\"]*\)\"}\{0,1\}\$/\1/p" "$cts_build" | head -1; }
# ct_sym_key: the SHA-256 of what the own ct.sym is made from: the platform's pin, the build's parameters, the tool's source
# and the data at jdk26u's pin, and the scripts that make it
ct_sym_key() {
  { sha256sum < "$out/platform.sha256"; sha256sum < "$cts_build"; sha256sum < "$jdk/$cts_tool"
    (cd "$jdk/$cts_data" && find . -type f | LC_ALL=C sort | xargs -d '\n' sha256sum)
    for f in ctsym.bash ctsym-run.ae gensrc-run.ae; do sha256sum < "$root/test/javac-platform/$f"; done
  } | sha256sum | cut -c1-64
}

# ct_sym_make DIR: the own ct.sym made into DIR/ct.sym (on javac's tool executable, JAVAC_EXE else gen_exe), with
# DIR/run.log and DIR/time.txt (wall seconds, peak RSS)
ct_sym_make() {
  local d=$1 exe c m t0 a=() p
  t0=$(date +%s)
  exe=${JAVAC_EXE:-$(gen_exe)}
  [ -x "$exe" ] || { echo "javac-vendor ct-sym: no javac's tool executable $exe (JAVAC_EXE or REPR_JAVA_EXE)" >&2; return 1; }
  [ -f "$cts_build" ] || { echo "javac-vendor ct-sym: no $cts_build (bin/javac-vendor ct-sym-build)" >&2; return 1; }
  c=$(JAVAC_VENDOR_PLATFORM_BUILD=${JAVAC_VENDOR_PLATFORM_BUILD:-0} own_cache) || return 1
  rm -rf "$d"; mkdir -p "$d/src/build/tools/symbolgenerator" "$d/exploded" "$d/tmp"
  cp "$jdk/$cts_tool" "$d/src/build/tools/symbolgenerator/" || return 1
  # the read: repr/java on the tool, with the build's --add-exports
  for p in $cts_exports; do a+=(--javac --add-exports --javac "jdk.compiler/$p=ALL-UNNAMED"); done
  (cd "$d" && REPR_JAVA_EXE=$exe "$root/bin/repr-java" read --check "${a[@]}" "$d/forms" "$d/src") > "$d/read.log" 2>&1 ||
    { tail -5 "$d/read.log" >&2; return 1; }
  grep -q '^1 Java files, 1 converted, 0 failed, 0 javac errors, ' "$d/read.log" || { tail -3 "$d/read.log" >&2; return 1; }
  # the current release: the own platform's modules as the build's exploded image holds them (the exploded descriptors,
  # descriptors/MODULE/exploded.class: the jmods' carry ModuleTarget, which the build's $(JDK_OUTPUTDIR)/modules/ has not)
  for m in $(ls "$c/modules"); do
    [ -f "$c/descriptors/$m/exploded.class" ] || { echo "javac-vendor ct-sym: no exploded descriptor of $m in $c/descriptors" >&2; return 1; }
    cp -rs "$c/modules/$m" "$d/exploded/$m" && rm -f "$d/exploded/$m/module-info.class" &&
      ln -s "$c/descriptors/$m/exploded.class" "$d/exploded/$m/module-info.class" || return 1
  done
  printf '%s\n' "$(cts_get included-modules)" > "$d/included-modules"
  (cd "$d" && env TMPDIR="$d/tmp" GOMEMLIMIT="${JAVAC_VENDOR_CT_SYM_MEM:-4GiB}" /usr/bin/time -f '%e %M' -o "$d/time.txt" \
     "$exe" "$root/test/javac-platform/ctsym-run.ae" "$d/forms" build.tools.symbolgenerator.CreateSymbols build-ctsym \
     "$jdk/$cts_data/symbols" "$d/ct.sym" "$(cts_get source-date-epoch)" "$(cts_get release)" "$(cts_get version-pre)" \
     "$d/exploded/" "$d/included-modules") > "$d/run.log" 2>&1 || { tail -5 "$d/run.log" >&2; return 1; }
  [ -s "$d/ct.sym" ] || { echo "javac-vendor ct-sym: CreateSymbols wrote no $d/ct.sym" >&2; return 1; }
  local w k; read -r w k < "$d/time.txt"
  echo "   ct.sym made: $(stat -c %s "$d/ct.sym") bytes, $(unzip -Z1 "$d/ct.sym" 2>"$quiet" | wc -l) entries, SHA-256 $(sha256sum < "$d/ct.sym" | cut -c1-12) (CreateSymbols ${w%.*} s, peak $((k / 1024)) MiB; $(since "$t0") with the read)" >&2
}

# ct_sym_own [--check]: the own ct.sym of the pin, ARBACE_GATE_EXES/platform-PIN20/ct-sym-KEY20/ct.sym, made on a miss and
# required to be the pin's (JAVAC_VENDOR_CT_SYM_BUILD=0 or --check: none made, a miss fails); prints the file
ct_sym_own() {
  local c k f want
  c=$(JAVAC_VENDOR_PLATFORM_BUILD=0 own_cache) || return 1
  k=$(ct_sym_key) || return 1
  f=$c/ct-sym-${k:0:20}
  if [ -f "$f/ok" ]; then echo "$f/ct.sym"; return 0; fi
  if [ "${1:-}" = --check ] || [ "${JAVAC_VENDOR_CT_SYM_BUILD:-1}" = 0 ]; then
    echo "javac-vendor: no own ct.sym at $f (bin/javac-vendor ct-sym-own makes it)" >&2; return 1
  fi
  want=$(pin_line "$cts_pin" ct.sym)
  (
    flock 9
    [ -f "$f/ok" ] && exit 0
    echo "javac-vendor: the own ct.sym of the pin, not in $f: made (CreateSymbols interpreted on javac's tool, about a minute)" >&2
    ct_sym_make "$f.tmp.$$" || exit 1
    h=$(sha256sum < "$f.tmp.$$/ct.sym" | cut -c1-64)
    [ "$h" = "$want" ] || { echo "javac-vendor: the own ct.sym made is $h, pinned ${want:-none} ($cts_pin; kept in $f.tmp.$$)" >&2; exit 1; }
    rm -rf "$f.tmp.$$/forms" "$f.tmp.$$/exploded" "$f.tmp.$$/src" "$f.tmp.$$/tmp" && rm -rf "$f" && mv "$f.tmp.$$" "$f" && touch "$f/ok"
    # the newest 3 kept (other checkouts' keys), the rest and leftovers of an interrupted making removed
    ls -dt "$c"/ct-sym-* 2>"$quiet" | grep -v '\.tmp\.' | tail -n +4 | xargs -r rm -rf
    find "$c" -maxdepth 1 -name 'ct-sym-*.tmp.*' -mmin +60 -exec rm -rf {} + 2>"$quiet"
  ) 9> "$c/.ct-sym.lock" || return 1
  echo "$f/ct.sym"
}

# ct_sym DIR [--make]: the pin's ct.sym linked into DIR as DIR/ct.sym, beside the modules (ReleasePlatforms finds it there):
# with the own pin the made file of the cache (--make: made on a miss; else a miss leaves DIR without one, warned: javac on
# Arbace then refuses --release N but its own release), else the build tree's images/jdk/lib/ct.sym checked against the pin;
# none without a pin
ct_sym() {
  local d=$1 c f h want
  [ -f "$cts_pin" ] || return 0
  want=$(pin_line "$cts_pin" ct.sym)
  if [ "$(ct_sym_kind)" = own ]; then
    if [ "${2:-}" = --make ]; then f=$(ct_sym_own) || return 1
    else f=$(JAVAC_VENDOR_CT_SYM_BUILD=0 ct_sym_own 2>"$quiet") || { echo "javac-vendor: warning: no own ct.sym made yet, $d has none (bin/javac-vendor ct-sym-own)" >&2; rm -f "$d/ct.sym"; return 0; }
    fi
  else
    c=$(conf); [ -n "$c" ] || { echo "javac-vendor: no JDK build tree in $jdk/build (its images/jdk/lib/ct.sym)" >&2; return 1; }
    f=$(dirname "$c")/images/jdk/lib/ct.sym
    [ -f "$f" ] || { echo "javac-vendor: no $f (the ct.sym $cts_pin pins)" >&2; return 1; }
  fi
  h=$(sha256sum < "$f" | cut -c1-64)
  [ "$h" = "$want" ] || { echo "javac-vendor: $f: $h, pinned ${want:-none} ($cts_pin)" >&2; return 1; }
  ln -sfn "$f" "$d/ct.sym"
}

# ct_sym_build_make: OUT/ct-sym-build.edn from the build tree (spec.gmk's SOURCE_DATE, JDK_SOURCE_TARGET_VERSION and
# VERSION_PRE; support/symbols/included-modules, which Gendata.gmk writes from CT_MODULES, CT_TRANSITIVE_MODULES and
# CT_EXTRA_MODULES), into FILE
ct_sym_build_make() {
  local o=$1 c b spec v
  c=$(conf); [ -n "$c" ] || fail "no JDK build tree in $jdk/build (its spec.gmk)"
  b=$(dirname "$c"); spec=$b/spec.gmk
  v() { sed -n "s/^$1 := \(.*\)\$/\1/p" "$spec" | head -1; }
  [ -f "$b/support/symbols/included-modules" ] || fail "no $b/support/symbols/included-modules"
  {
    echo ";; The JDK build's parameters of ct.sym (JCS50 (b), doc/classes/JAVA-CONFORMANCE-SURVEY.md, \"As built: JCS50 (b), the"
    echo ";; own ct.sym\"): derived by bin/javac-vendor ct-sym-build from jdk26u's build tree at $pinned, re-checked against it by"
    echo ";; ct-sym-build --check (the oracle tier's) and never read from it otherwise; the arguments make/modules/jdk.compiler/"
    echo ";; Gendata.gmk gives CreateSymbols build-ctsym. Generated; do not edit by hand. A line a key (bin/javac-vendor's cts_get)."
    echo "{:jdk \"$pinned\""
    echo " :origins {:source-date-epoch \"spec.gmk's SOURCE_DATE (SOURCE_DATE_EPOCH)\" :release \"spec.gmk's JDK_SOURCE_TARGET_VERSION\""
    echo "           :version-pre \"spec.gmk's VERSION_PRE\" :included-modules \"support/symbols/included-modules (Gendata.gmk)\""
    echo "           :description \"src/jdk.compiler/share/data/symbols/symbols, at the pin\"}"
    echo " :source-date-epoch \"$(v SOURCE_DATE)\""
    echo " :release \"$(v JDK_SOURCE_TARGET_VERSION)\""
    echo " :version-pre \"$(v VERSION_PRE)\""
    echo " :included-modules \"$(head -1 "$b/support/symbols/included-modules")\"}"
  } > "$o"
}
