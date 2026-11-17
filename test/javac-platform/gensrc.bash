# The own platform's generated sources (JCS359 (a)(2), doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS359 stages
# 2-3 (day)"): the 16 modules' support/gensrc made as the JDK build makes them, by the build's own tools on javac's tool
# executable, and judged against the build tree's. Sourced by bin/javac-vendor (its root, jdk, work, out, pinned, conf,
# pb_* and gen_exe); out of every key and of the Java steps' inputs (test/javac-platform/: it runs the tool, it does
# not build it).
#
# Two parts, as make has two kinds of rule:
#
# - The recorded runs. The build records the command line of most tool runs it makes (make's ExecuteWithLog and
#   SetupExecute: support/gensrc/MODULE/**/_*.cmdline): Spp's 122 runs, CompileProperties' 17, the CLDR converter's two,
#   FieldGen, the X11 wrappers, the langtools tools. platform-gensrc derives them once into OUT/platform-gensrc.edn
#   (JCS362 (a): configuration as data, the paths with bin/javac-vendor's placeholders, each run's moves after it), with
#   the values make substitutes into text (its vardeps: VersionProps, PlatformProps, JceSecurity, release.txt), the module
#   lists of GenModuleLoaderMap and GenModuleInfoSource, and spec.gmk's values the other rules use; platform-gensrc --check
#   (the oracle tier's) derives it afresh and compares. gensrc replays every run on javac's tool, {gensrc} its own tree.
# - The rules make runs without recording them, ported here from make/modules/*/Gensrc.gmk and gensrc/*.gmk, each at
#   its function: CharacterData, CaseFolding, IndicConjunctBreak, the charset mappings (java.base's stdcs, jdk.charsets'
#   extcs, hkscs, euctw, sjis0213), LocaleEquivalentMaps, ModuleLoaderMap, VarHandleGuards, module-info.java (three
#   modules), the buffers' and ScopedMemoryAccess's snippets put together, the text substitutions, version.properties,
#   jdwpgen's JDWP.java, the AWT icons, the Nimbus look and feel, the zh_HK copies of the zh_TW bundles, sa.properties.
#
# A tool javac's tool executable holds compiled (jdk/javac/forms' module buildtools: bin/javac-vendor's tool_dirs and
# langtools_dirs) runs compiled; any other is read by repr/java from make/jdk/src/classes/build/tools/DIR (with its
# tool-world variants, overlay/jdk/javac/buildtools) into WORK/gensrc/tools/DIR and run by the interpreter
# (test/javac-platform/gensrc-run.ae): jdwpgen, x11wrappergen, icondata, methodhandle, module, generatenimbus.

pgensrc=$out/platform-gensrc.edn
gknown=${JAVAC_VENDOR_GENSRC_KNOWN:-$out/platform-gensrc-known.edn}
# the 16 modules the build generates sources for
gensrc_modules="java.base java.desktop java.logging jdk.charsets jdk.compiler jdk.hotspot.agent jdk.httpserver jdk.jartool
  jdk.javadoc jdk.jdeps jdk.jdi jdk.jlink jdk.jpackage jdk.jshell jdk.localedata jdk.management.agent"

# pg_records [FILE]: each record of the data, its fields separated by \x1f, a line a record
pg_records() {
  awk '
    /^  \["/ {
      s = $0; ins = 0; buf = ""; out = ""; n = 0
      for (j = 1; j <= length(s); j++) {
        c = substr(s, j, 1)
        if (ins) {
          if (c == "\\") { j++; d = substr(s, j, 1); buf = buf (d == "n" ? "\n" : d) }
          else if (c == "\"") { out = out (n++ ? "\037" : "") buf; ins = 0; buf = "" }
          else buf = buf c
        } else if (c == "\"") ins = 1
        else if (c == "]") break
      }
      print out
    }' "${1:-$pgensrc}"
}
# pg_value NAME: a "value" record's value
pg_value() { pg_records | awk -F'\037' -v n="$1" '$1 == "value" && $2 == n { print $3; exit }'; }
# pg_list NAME [SEP]: a "list" record's items joined by SEP (default ",")
pg_list() { pg_records | awk -F'\037' -v n="$1" -v s="${2:-,}" '$1 == "list" && $2 == n { o = $3; for (i = 4; i <= NF; i++) o = o s $i; print o; exit }'; }

# platform_gensrc_make FILE: the derivation from the build tree
platform_gensrc_make() {
  local o=$1 c b g bb f rel m id dir rest a s v w
  c=$(conf); [ -n "$c" ] || fail "no JDK build tree in $jdk/build"
  b=$(dirname "$c"); g=$b/support/gensrc; bb=build/$(basename "$b")
  local vd=$b/make-support/vardeps/make
  {
    echo ";; The JDK build's generated sources' configuration as data (JCS362 (a), JCS359 (a)(2), doc/classes/JAVA-CONFORMANCE-SURVEY.md,"
    echo ";; \"As built: JCS359 stages 2-3 (day)\"): derived by bin/javac-vendor platform-gensrc from jdk26u's build tree at"
    echo ";; $pinned, re-checked against it by platform-gensrc --check (the oracle tier's) and never read from it otherwise."
    echo ";; Each record one line, a vector of strings (test/javac-platform/gensrc.bash's pg_records): value NAME VALUE (spec.gmk's);"
    echo ";; list NAME ITEM... (a module list make computes); subst MODULE OUTPUT TEMPLATE FROM TO... (make's text"
    echo ";; substitution, its vardeps); resource-packages MODULE PACKAGE... (the module's packages that hold resources alone,"
    echo ";; stage 3's); java MODULE ID DIR TOOL ARG... (a tool run the build recorded, its @-files expanded, DIR \"-\" for none);"
    echo ";; mv MODULE ID FROM TO (the run's move after it). Paths with bin/javac-vendor's placeholders."
    echo ";; Generated; do not edit by hand."
    echo "{:jdk $(pb_str "$pinned")"
    echo " :build $(pb_str "$bb")"
    echo " :origins"
    echo " {:value \"$bb/spec.gmk's NAME := VALUE\""
    echo "  :list \"$bb/make-support/vardeps/make/ModuleWrapper.gmk/java.base/VARDEPS_VALUE.vardeps (GensrcModuleLoaderMap.gmk's BOOT_MODULES, PLATFORM_MODULES, NATIVE_ACCESS_MODULES), .../common/modules/GensrcModuleInfo.gmk/MODULE/ALL_MODULES.vardeps\""
    echo "  :subst \"$bb/make-support/vardeps/make/ModuleWrapper.gmk/java.base/BUILD_*_VARDEPS.vardeps (SetupTextFileProcessing's sed)\""
    echo "  :java \"$bb/support/gensrc/MODULE/**/_*.cmdline: the recorded java runs (cd DIR && java ... -cp CP TOOL ARGS)\""
    echo "  :mv \"$bb/support/gensrc/MODULE/**/_*_post.cmdline\""
    echo "  :resource-packages \"$bb/jdk/modules/MODULE/module-info.class's ModulePackages less the packages of MODULE's class files\"}"
    echo " :records ["
    for v in COPYRIGHT_YEAR OPENJDK_TARGET_OS OPENJDK_TARGET_OS_TYPE OPENJDK_TARGET_CPU OPENJDK_TARGET_CPU_BITS \
             OPENJDK_TARGET_CPU_ENDIAN VERSION_NUMBER VERSION_STRING VERSION_SHORT; do
      a=$(sed -n "s/^$v := *//p" "$c" | head -1)
      [ -n "$a" ] || { echo "javac-vendor: no $v in $c" >&2; return 1; }
      echo "  $(pb_vec value "$v" "$a")"
    done
    # the module lists: the loader map's three (VARDEPS_VALUE := BOOT PLATFORM NATIVE, comma lists), each module's
    # ALL_MODULES for GenModuleInfoSource
    s=$(sed -n 's/^VARDEPS_VALUE_old := //p' "$vd/ModuleWrapper.gmk/java.base/VARDEPS_VALUE.vardeps")
    set -f; set -- $s; set +f
    [ $# = 3 ] || { echo "javac-vendor: VARDEPS_VALUE is not three lists" >&2; return 1; }
    echo "  $(IFS=,; pb_vec list BOOT_MODULES $1)"
    echo "  $(IFS=,; pb_vec list PLATFORM_MODULES $2)"
    echo "  $(IFS=,; pb_vec list NATIVE_ACCESS_MODULES $3)"
    for f in "$vd"/common/modules/GensrcModuleInfo.gmk/*/ALL_MODULES.vardeps; do
      m=$(basename "$(dirname "$f")")
      s=$(sed -n 's/^ALL_MODULES_old := //p' "$f")
      set -f; set -- $s; set +f
      echo "  $(pb_vec list "ALL_MODULES:$m" "$@")"
    done
    # the text substitutions: each vardeps' sed -e 's|FROM|TO|g' pairs, the template and output make names
    for a in "BUILD_VERSION_JAVA java/lang/VersionProps.java {src}/java.base/share/classes/java/lang/VersionProps.java.template" \
             "BUILD_PLATFORMPROPERTIES_JAVA jdk/internal/util/PlatformProps.java {src}/java.base/share/classes/jdk/internal/util/PlatformProps.java.template" \
             "BUILD_JCESECURITY_JAVA javax/crypto/JceSecurity.java {src}/java.base/share/classes/javax/crypto/JceSecurity.java.template" \
             "BUILD_RELEASE_FILE jdk/internal/misc/resources/release.txt {src}/java.base/share/classes/jdk/internal/misc/resources/release.txt.template"; do
      set -f; set -- $a; set +f
      f=$vd/ModuleWrapper.gmk/java.base/$1_VARDEPS.vardeps
      s=$(sed -n "s/^$1_VARDEPS_old := //p" "$f")
      [ -n "$s" ] || { echo "javac-vendor: no $f" >&2; return 1; }
      local out2=$2 tpl=$3 pairs=()
      set -f; eval "set -- $s"; set +f
      while [ $# -gt 0 ]; do
        if [ "$1" = -e ]; then
          w=$2; local sep=${w:1:1} body=${w:2}; body=${body%"${sep}g"}
          pairs+=("${body%%"$sep"*}" "${body#*"$sep"}"); shift 2
        else shift; fi
      done
      echo "  $(pb_vec subst java.base "$out2" "$tpl" "${pairs[@]}")"
    done
    # the resource-only packages of every module (stage 3's ModulePackages: the image's packages, a module's class
    # files' and its resources'; the platform holds class files alone): the exploded descriptor's packages that hold
    # no class file
    local mex
    mex=$(. "$root/bin/lib/host.bash" && arbace_host_exe) || return 1
    for m in $(platform_modules); do
      "$mex" "$root/test/javac-platform/modinfo.ae" --packages "$b/jdk/modules/$m/module-info.class" > "$TMPDIR/javac-vendor.pg-p" || return 1
      local rp=()
      while IFS= read -r a; do
        ls "$b/jdk/modules/$m/${a//.//}"/*.class > "$quiet" 2>&1 || rp+=("$a")
      done < "$TMPDIR/javac-vendor.pg-p"
      [ ${#rp[@]} = 0 ] || echo "  $(pb_vec resource-packages "$m" $(printf '%s\n' "${rp[@]}" | LC_ALL=C sort))"
    done
    # the recorded java runs, by module and path; each run's _post move after it
    for m in $gensrc_modules; do
      [ -d "$g/$m" ] || continue
      (cd "$g/$m" && find . -name '_*.cmdline' ! -name '*_pre.cmdline' ! -name '*_post.cmdline' | sed 's|^\./||' | LC_ALL=C sort) |
      while IFS= read -r rel; do
        f=$g/$m/$rel
        grep -q '/bin/java ' "$f" || continue
        id=${rel%.cmdline}
        dir=$(sed -n 's/^cd \([^ ]*\) && .*/\1/p' "$f"); [ -n "$dir" ] || dir=-
        rest=$(sed 's/^.* -cp [^ ]* //' "$f")
        set -f; eval "set -- $rest"; set +f
        local args=() x
        for x in "$@"; do
          if [ "${x:0:1}" = @ ] && [ -f "${x:1}" ]; then mapfile -t -O "${#args[@]}" args < "${x:1}"; else args+=("$x"); fi
        done
        local pa=()
        [ "$dir" = - ] || dir=$(pb_abs "$b" "$dir")
        pb_abs "$b" "${args[@]}" > "$TMPDIR/javac-vendor.pg-a"; mapfile -t pa < "$TMPDIR/javac-vendor.pg-a"
        echo "  $(pb_vec java "$m" "$id" "$dir" "${pa[@]}")"
        local post=${f%_exec.cmdline}_post.cmdline
        if [ "$post" != "$f" ] && [ -f "$post" ]; then
          s=$(sed -n 's|^cd [^ ]* && /bin/mv ||p' "$post")
          set -f; eval "set -- $s"; set +f
          [ $# = 2 ] || { echo "javac-vendor: $post is not one mv" >&2; exit 1; }
          echo "  $(pb_vec mv "$m" "$id" "$(pb_abs "$b" "$1")" "$(pb_abs "$b" "$2")")"
        fi
      done || return 1
    done
    echo " ]}"
  } > "$o.tmp" || { rm -f "$o.tmp"; return 1; }
  grep -q '^  \["java" "java.base" ' "$o.tmp" || { echo "javac-vendor: no recorded run of java.base" >&2; rm -f "$o.tmp"; return 1; }
  mv "$o.tmp" "$o"
}

# The tools: those javac's tool executable holds compiled (bin/javac-vendor's tool_dirs, langtools_dirs), by package
gensrc_compiled() {
  local p=${1%.*} d
  for d in $tool_dirs; do [ "$p" = "build.tools.$d" ] && return 0; done
  for d in $langtools_dirs; do [ "$p" = "$d" ] && return 0; done
  return 1
}
# gensrc_forms DIR: the tool package make/jdk/src/classes/build/tools/DIR (and its variants) as Java forms in
# WORK/gensrc/tools/DIR/forms, read once a run by repr/java on the tool; prints the forms' root
gensrc_forms() {
  local d=$1 t=$work/gensrc/tools/$1 v=$root/overlay/jdk/javac/buildtools/build/tools/$1
  if [ ! -f "$t/ok" ]; then
    rm -rf "$t"; mkdir -p "$t/src/build/tools"
    cp -r "$jdk/make/jdk/src/classes/build/tools/$d" "$t/src/build/tools/" || return 1
    # the tool-world variants replace the build's files of the same name
    [ ! -d "$v" ] || cp -r "$v/." "$t/src/build/tools/$d/" || return 1
    # a tool whose uses of what javac's tool lacks are renamed to its variants' classes (gensrc_patch_DIR)
    if declare -F "gensrc_patch_$d" > "$quiet"; then "gensrc_patch_$d" "$t/src/build/tools/$d" || return 1; fi
    REPR_JAVA_EXE=$gexe "$root/bin/repr-java" read --check "$t/forms" "$t/src" > "$t/read.log" 2>&1 ||
      { tail -5 "$t/read.log" >&2; return 1; }
    grep -q ' 0 failed, 0 javac errors, ' "$t/read.log" || { tail -3 "$t/read.log" >&2; return 1; }
    # the tool's world variants (overlay/jdk/javac/buildtools-world/DIR/MODULE: the API it calls that javac's tool lacks),
    # each module's read as a patch of that module against the platform, defined beside the tool
    local w=$root/overlay/jdk/javac/buildtools-world/$d m
    if [ -d "$w" ]; then
      for m in $(ls "$w"); do
        (cd "$t" && REPR_JAVA_EXE=$gexe "$root/bin/repr-java" read-jdk --check "$t/world/$m" "$m" "$w/$m") > "$t/read-$m.log" 2>&1 ||
          { tail -5 "$t/read-$m.log" >&2; return 1; }
        grep -q ' 0 failed, 0 javac errors, ' "$t/read-$m.log" || { tail -3 "$t/read-$m.log" >&2; return 1; }
        rm -f "$t/world/$m/repr-java-report.edn"
        cp -r "$t/world/$m/." "$t/forms/" || return 1
      done
    fi
    touch "$t/ok"
  fi
  echo "$t/forms"
}
# gensrc_patch_methodhandle DIR: VarHandleGuardMethodGenerator's MethodType, VarHandle.class and TypeKind as its
# variants' GuardMethodType, the name className gives VarHandle ("VarHandle": java.lang.invoke's package dropped) and
# GuardTypeKind, and its loop over a method's Parameters (java.lang.reflect.Parameter, not in the tool's world) over
# its parameter types, as many (overlay/jdk/javac/buildtools/build/tools/methodhandle), as make/CopyInterimTZDB.gmk
# renames a package: the build's file is read, never kept changed; each substitution must apply once at least
gensrc_patch_methodhandle() {
  local f=$1/VarHandleGuardMethodGenerator.java x
  for x in '^import java\.lang\.classfile\.TypeKind;$' '^import java\.lang\.invoke\.MethodType;$' '^import java\.lang\.invoke\.VarHandle;$' \
           'className(VarHandle\.class)' 'TypeKind\.from(pt)\.upperBound()\.descriptorString()' '\bMethodType\b' \
           'for (var p : m\.getParameters())'; do
    grep -q "$x" "$f" || { echo "javac-vendor gensrc: $f has no $x" >&2; return 1; }
  done
  sed -i -e '/^import java\.lang\.classfile\.TypeKind;$/d' -e '/^import java\.lang\.invoke\.MethodType;$/d' \
    -e '/^import java\.lang\.invoke\.VarHandle;$/d' -e 's/className(VarHandle\.class)/"VarHandle"/' \
    -e 's/TypeKind\.from(pt)\.upperBound()\.descriptorString()/GuardTypeKind.upperBoundDescriptor(pt)/' \
    -e 's/\bMethodType\b/GuardMethodType/g' -e 's/for (var p : m\.getParameters())/for (Class<?> p : m.getParameterTypes())/' "$f"
}
# gensrc_patch_generatenimbus DIR: Generator's templates, its class resources (Class.getResourceAsStream("resources/..."),
# which the Go build provides but does not list among Class's members, so that the interpreter cannot call it), read as
# Class.getResourceAsStream resolves a relative name: by the class's package's path, from the system class loader (the
# build's tools' class path; here ARBACE_PATH, where gensrc_post_java_desktop puts them as make/CompileToolsJdk.gmk does)
gensrc_patch_generatenimbus() {
  local f=$1/Generator.java x='getClass()\.getResourceAsStream('
  [ "$(grep -c "$x" "$f")" = 1 ] || { echo "javac-vendor gensrc: $f has not one $x" >&2; return 1; }
  sed -i 's|getClass()\.getResourceAsStream(|ClassLoader.getSystemClassLoader().getResourceAsStream("build/tools/generatenimbus/" +|' "$f"
}
# gensrc_tool CWD CLASS ARG...: the tool's main on javac's tool executable, in CWD (- the module's tree), its output
# to the run's log
# (the interpreted tools defined with --reflect: none since gensrc_patch_generatenimbus)
gensrc_reflect_tools=""
gensrc_tool() {
  local cwd=$1 cls=$2 forms=- d r=(); shift 2
  if ! gensrc_compiled "$cls"; then
    d=${cls#build.tools.}; d=${d%%.*}
    forms=$(gensrc_forms "$d") || { echo "javac-vendor gensrc: the tool $cls was not read" >&2; return 1; }
    case " $gensrc_reflect_tools " in *" $d "*) r=(--reflect) ;; esac
  fi
  [ "$cwd" != - ] || cwd=$gout
  mkdir -p "$cwd"
  (cd "$cwd" && env "TMPDIR=$work/tmp" \
     "ARBACE_PATH=$work/gensrc/tools/resources:$jdk/make/jdk/src/classes:$jdk/src/java.xml/share/classes:$jdk/make/langtools/tools" \
     "$gexe" "$root/test/javac-platform/gensrc-run.ae" "-Djdk.internal.jrt.javac.platform=$work/platform" \
     "${r[@]}" "$forms" "$cls" "$@")
}
# pg_concrete: stdin's placeholders, {gensrc} the own tree
pg_concrete() { PB_GENSRC=$gout pb_concrete "$gbuild"; }

# gensrc_replay MODULE: the module's recorded runs and moves
gensrc_replay() {
  local m=$1 kind mm id dir tool x
  pg_records | awk -F'\037' -v m="$m" '($1 == "java" || $1 == "mv") && $2 == m' | pg_concrete > "$work/gensrc/runs-$m.txt"
  while IFS=$'\037' read -r -a r; do
    kind=${r[0]} id=${r[2]}
    case $kind in
      java)
        dir=${r[3]} tool=${r[4]}
        # make makes the outputs' directories before a run
        for x in "${r[@]:5}"; do x=${x#-o}; case $x in "$gout"/*) mkdir -p "$(dirname "$x")" ;; esac; done
        gensrc_tool "$dir" "$tool" "${r[@]:5}" >> "$work/gensrc/log/$m.log" 2>&1 ||
          { echo "javac-vendor gensrc: $m: $id ($tool) failed ($work/gensrc/log/$m.log)" >&2; return 1; } ;;
      mv) mv "${r[3]}" "${r[4]}" || return 1 ;;
    esac
  done < "$work/gensrc/runs-$m.txt"
}

# The rules make does not record, a function a module: gensrc_pre_MODULE (before the recorded runs: their inputs),
# gensrc_post_MODULE (after them)
gensrc_tool_logged() { gensrc_tool "$@" >> "$work/gensrc/log/$gm.log" 2>&1 || { echo "javac-vendor gensrc: $gm: $2 failed ($work/gensrc/log/$gm.log)" >&2; return 1; }; }
# GensrcCommon.gmk's SetupVersionProperties MODULE REL
gensrc_version_properties() {
  mkdir -p "$gout/$1/$(dirname "$2")"
  printf 'jdk=%s\nfull=%s\nrelease=%s\n' "$(pg_value VERSION_NUMBER)" "$(pg_value VERSION_STRING)" "$(pg_value VERSION_SHORT)" > "$gout/$1/$2"
}
# GensrcProperties.gmk's SetupCopy-zh_HK: each generated *_zh_TW.java copied as *_zh_HK.java, the class line renamed
gensrc_zh_hk() {
  local f
  find "$gout/$1" -name '*_zh_TW.java' | while IFS= read -r f; do
    sed -e '/class/s/_zh_TW/_zh_HK/' < "$f" > "${f%_zh_TW.java}_zh_HK.java"
  done
}
# GensrcModuleInfo.gmk: module-info.java from the module's own and its .extra files (the module's source roots, the
# build's order: OS, OS type, share), by build.tools.module.GenModuleInfoSource
gensrc_module_info() {
  local m=$1 r extras=() src=
  for r in linux unix share; do
    [ -z "$src" ] && [ -f "$jdk/src/$m/$r/classes/module-info.java" ] && src=$jdk/src/$m/$r/classes/module-info.java
    [ -f "$jdk/src/$m/$r/classes/module-info.java.extra" ] && extras+=("$jdk/src/$m/$r/classes/module-info.java.extra")
  done
  [ ${#extras[@]} -gt 0 ] || return 0
  mkdir -p "$gout/$m"
  gensrc_tool_logged - build.tools.module.GenModuleInfoSource -o "$gout/$m/module-info.java.tmp" --source-file "$src" \
    --modules "$(pg_list "ALL_MODULES:$m")" "${extras[@]}" &&
  mv "$gout/$m/module-info.java.tmp" "$gout/$m/module-info.java"
}

gensrc_pre_java_base() { :; }
gensrc_post_java_base() {
  local gfail=0 o=$gout/java.base u=$jdk/src/java.base/share/data/unicodedata c=$jdk/src/java.base/share/classes x s f
  # GensrcCharacterData.gmk: SetupCharacterData NAME, OPTIONS, -usecharforbyte ARGS; CaseFolding
  mkdir -p "$o/java/lang" "$o/jdk/internal/lang" "$o/jdk/internal/util/regex"
  for x in "CharacterDataLatin1||-latin1 8" "CharacterData00|-string -plane 0|11 4 1" "CharacterData01|-string -plane 1|11 4 1" \
           "CharacterData02|-string -plane 2|11 4 1" "CharacterData03|-string -plane 3|11 4 1" "CharacterData0E|-string -plane 14|11 4 1"; do
    IFS='|' read -r f s a <<< "$x"
    # shellcheck disable=SC2086
    gensrc_tool_logged - build.tools.generatecharacter.GenerateCharacter $s -template "$c/java/lang/$f.java.template" \
      -spec "$u/UnicodeData.txt" -specialcasing "$u/SpecialCasing.txt" -proplist "$u/PropList.txt" \
      -derivedprops "$u/DerivedCoreProperties.txt" -emojidata "$u/emoji/emoji-data.txt" \
      -o "$o/java/lang/$f.java" -usecharforbyte $a || gfail=1
  done
  gensrc_tool_logged - build.tools.generatecharacter.GenerateCaseFolding "$c/jdk/internal/lang/CaseFolding.java.template" \
    "$u/CaseFolding.txt" "$o/jdk/internal/lang/CaseFolding.java" || gfail=1
  # GensrcRegex.gmk
  gensrc_tool_logged - build.tools.generateextraproperties.GenerateExtraProperties \
    "$c/jdk/internal/util/regex/IndicConjunctBreak.java.template" "$u/DerivedCoreProperties.txt" \
    "$o/jdk/internal/util/regex/IndicConjunctBreak.java" InCB=Linker InCB=Extend InCB=Consonant || gfail=1
  # GensrcCharsetMapping.gmk: stdcs
  mkdir -p "$o/sun/nio/cs"
  gensrc_tool_logged - build.tools.charsetmapping.Main "$jdk/make/data/charsetmapping" "$o/sun/nio/cs" stdcs charsets \
    "stdcs-$(pg_value OPENJDK_TARGET_OS)" "$c/sun/nio/cs/StandardCharsets.java.template" \
    "$jdk/src/jdk.charsets/share/classes/sun/nio/cs/ext" "$jdk/make/jdk/src/classes/build/tools/charsetmapping" || gfail=1
  # Gensrc.gmk's GENSRC_LSREQUIVMAPS
  mkdir -p "$o/sun/util/locale"
  gensrc_tool_logged - build.tools.generatelsrequivmaps.EquivMapsGenerator "$jdk/src/java.base/share/data/lsrdata/language-subtag-registry.txt" \
    "$o/sun/util/locale/LocaleEquivalentMaps.java" "$(pg_value COPYRIGHT_YEAR)" || gfail=1
  # GensrcModuleLoaderMap.gmk
  mkdir -p "$o/jdk/internal/module"
  gensrc_tool_logged - build.tools.module.GenModuleLoaderMap -boot "$(pg_list BOOT_MODULES)" -platform "$(pg_list PLATFORM_MODULES)" \
    -native-access "$(pg_list NATIVE_ACCESS_MODULES)" -o "$o/jdk/internal/module/ModuleLoaderMap.java.tmp" \
    "$c/jdk/internal/module/ModuleLoaderMap.java" &&
  mv "$o/jdk/internal/module/ModuleLoaderMap.java.tmp" "$o/jdk/internal/module/ModuleLoaderMap.java" || gfail=1
  # GensrcVarHandles.gmk's GENSRC_VARHANDLEGUARDS
  mkdir -p "$o/java/lang/invoke"
  gensrc_tool_logged - build.tools.methodhandle.VarHandleGuardMethodGenerator "$o/java/lang/invoke/VarHandleGuards.java" || gfail=1
  # GensrcBuffer.gmk's GENERATE_BIN: the lines from #BIN on replaced by each non-byte type's snippet
  for f in "$o"/java/nio/*.java.bin-snippet.tmp; do
    [ -f "$f" ] || continue
    x=${f%.bin-snippet.tmp}
    { sed -e '/#BIN/,$d' < "$f"; for s in char short int long float double; do cat "$x.bin-snippet.$s"; done; echo "}"; } > "$x.tmp" &&
      mv "$x.tmp" "$x" || gfail=1
  done
  # GensrcScopedMemoryAccess.gmk: the template, each type's snippet (SCOPED_TYPES' order), a brace
  f=$o/jdk/internal/misc/ScopedMemoryAccess.java
  { cat "$c/jdk/internal/misc/X-ScopedMemoryAccess.java.template"
    for s in boolean byte short char int long float double; do cat "$f.snippet.$s"; done; echo "}"; } > "$f.tmp" && mv "$f.tmp" "$f" || gfail=1
  # SetupTextFileProcessing's substitutions (GensrcMisc.gmk, Gensrc.gmk's release.txt)
  gensrc_substs java.base || gfail=1
  # GensrcMisc.gmk's generate-preproc-src: UnixConstants, SocketOptionRegistry (JCS369 (a))
  gensrc_preproc "$jdk/src/java.base/unix/classes/sun/nio/fs/UnixConstants.java.template" "$o/sun/nio/fs/UnixConstants.java" || gfail=1
  gensrc_preproc "$jdk/src/java.base/share/classes/sun/nio/ch/SocketOptionRegistry.java.template" "$o/sun/nio/ch/SocketOptionRegistry.java" || gfail=1
  gensrc_module_info java.base || gfail=1
  gensrc_zh_hk java.base || gfail=1
  return $gfail
}
# gensrc_preproc TEMPLATE OUT: GensrcMisc.gmk's generate-preproc-src (JCS369 (a)): the template's lines before
# @@END_COPYRIGHT@@, then its C-preprocessed text from @@START_HERE@@ on, that line renamed, PREFIX_ dropped and a line
# marker made empty, as make's awk and sed do; the preprocessing repr/c's on C's own target (musl's headers, c/target),
# gcc -E's layout by test/javac-platform/cppout.ae, with the build's -D, -U, -I and -std (platform-build.edn's :cpp)
gensrc_preproc() {
  local t=$1 o=$2 opts=()
  mkdir -p "$(dirname "$o")"
  pb_get_top cpp | grep -e '^-[DUI]' -e '^-std' > "$work/gensrc/cpp.opts"
  mapfile -t opts < "$work/gensrc/cpp.opts"
  ( . "$root/bin/lib/repr-c/sysroot.bash" && c_sysroot &&
    mex=$(. "$root/bin/lib/host.bash" && arbace_host_exe) &&
    "$mex" "$root/test/javac-platform/cppout.ae" "$root/test/c/host" "$t" "${opts[@]}" ) > "$o.E" 2>> "$work/gensrc/log/$gm.log" ||
    { echo "javac-vendor gensrc: $gm: preprocessing $t failed ($work/gensrc/log/$gm.log)" >&2; return 1; }
  { awk '/@@END_COPYRIGHT@@/{exit}1' "$t"
    awk '/@@START_HERE@@/,0' "$o.E" | sed -e 's/@@START_HERE@@/\/\/ AUTOMATICALLY GENERATED FILE - DO NOT EDIT/' -e 's/PREFIX_//' -e 's/^#.*//'
  } > "$o" && rm -f "$o.E"
}
# pb_get_top FIELD: the strings of platform-build.edn's top-level vector FIELD, a line each
pb_get_top() {
  awk -v field=" :$1 [" 'index($0, field) == 1 {
      s = substr($0, length(field) + 1); ins = 0; buf = ""
      for (j = 1; j <= length(s); j++) {
        c = substr(s, j, 1)
        if (ins) { if (c == "\\") { j++; buf = buf substr(s, j, 1) } else if (c == "\"") { print buf; ins = 0; buf = "" } else buf = buf c }
        else if (c == "\"") ins = 1
        else if (c == "]") exit
      }
      exit }' "$pbuild"
}
# gensrc_substs MODULE: each subst record of MODULE: TEMPLATE through sed's s|FROM|TO|g in order
gensrc_substs() {
  local r i
  pg_records | awk -F'\037' -v m="$1" '$1 == "subst" && $2 == m' | pg_concrete > "$work/gensrc/subst-$1.txt"
  while IFS=$'\037' read -r -a r; do
    local t; t=$(cat "${r[3]}") || return 1
    for ((i = 4; i + 1 < ${#r[@]}; i += 2)); do t=${t//"${r[i]}"/"${r[i+1]}"}; done
    mkdir -p "$gout/$1/$(dirname "${r[2]}")"
    # make's sed keeps the template's last newline (or its absence)
    if [ -n "$(tail -c 1 "${r[3]}")" ]; then printf '%s' "$t"; else printf '%s\n' "$t"; fi > "$gout/$1/${r[2]}"
  done < "$work/gensrc/subst-$1.txt"
}
gensrc_post_java_desktop() {
  local gfail=0 o=$gout/java.desktop p n s
  # GensrcIcons.gmk: each X11 icon's int (32) and long (64) array by ToBin (its tool-world variant)
  mkdir -p "$o/sun/awt"
  for p in java-icon16.png java-icon24.png java-icon32.png java-icon48.png; do
    s=${p//[.-]/_}
    gensrc_tool - build.tools.icondata.awt.ToBin < "$jdk/src/java.desktop/unix/classes/sun/awt/X11/$p" > "$o/sun/awt/$s.bin" \
      2>> "$work/gensrc/log/$gm.log" || { echo "javac-vendor gensrc: java.desktop: ToBin $p failed" >&2; return 1; }
    for n in 32 64; do
      { echo "package sun.awt;"; echo "public class AWTIcon${n}_$s {"
        if [ $n = 32 ]; then echo "public static int[] $s = { "; else echo "public static long[] $s = { "; fi
        cat "$o/sun/awt/$s.bin"; echo "}; }"; } > "$o/sun/awt/AWTIcon${n}_$s.java"
    done
    rm -f "$o/sun/awt/$s.bin"
  done
  # GensrcSwing.gmk: Nimbus (JCS368 (b)): its templates are the tool's class resources, as make/CompileToolsJdk.gmk copies
  # them (src/java.desktop/share/classes/javax/swing/plaf/nimbus/*.template into build/tools/generatenimbus/resources)
  local res=$work/gensrc/tools/resources/build/tools/generatenimbus/resources
  rm -rf "$res"; mkdir -p "$res"
  cp "$jdk"/src/java.desktop/share/classes/javax/swing/plaf/nimbus/*.template "$res/" || gfail=1
  gensrc_tool_logged - build.tools.generatenimbus.Generator -skinFile "$jdk/src/java.desktop/share/classes/javax/swing/plaf/nimbus/skin.laf" \
    -buildDir "$o" -packagePrefix javax.swing.plaf.nimbus -lafName Nimbus || gfail=1
  gensrc_zh_hk java.desktop || gfail=1
  return $gfail
}
gensrc_post_java_logging() { gensrc_zh_hk java.logging; }
gensrc_post_jdk_charsets() {
  local d=$jdk/make/data/charsetmapping o=$gout/jdk.charsets/sun/nio/cs/ext h=$jdk/make/jdk/src/classes/build/tools/charsetmapping
  mkdir -p "$o"
  gensrc_tool_logged - build.tools.charsetmapping.Main "$d" "$o" extcs charsets "stdcs-$(pg_value OPENJDK_TARGET_OS)" \
    "$jdk/src/jdk.charsets/share/classes/sun/nio/cs/ext/ExtendedCharsets.java.template" \
    "$jdk/src/jdk.charsets/share/classes/sun/nio/cs/ext" "$h" &&
  gensrc_tool_logged - build.tools.charsetmapping.Main "$d" "$o" hkscs "$h/HKSCS.java" &&
  gensrc_tool_logged - build.tools.charsetmapping.Main "$d" "$o" euctw "$h/EUC_TW.java" &&
  gensrc_tool_logged - build.tools.charsetmapping.Main "$d/sjis0213.map" "$o/sjis0213.dat" sjis0213
}
gensrc_pre_jdk_compiler() { gensrc_version_properties jdk.compiler com/sun/tools/javac/resources/version.properties; }
gensrc_post_jdk_compiler() { gensrc_zh_hk jdk.compiler; }
gensrc_post_jdk_hotspot_agent() {
  mkdir -p "$gout/jdk.hotspot.agent"
  echo "sun.jvm.hotspot.runtime.VM.saBuildVersion=$(pg_value VERSION_STRING)" > "$gout/jdk.hotspot.agent/sa.properties"
}
gensrc_post_jdk_httpserver() { gensrc_zh_hk jdk.httpserver; }
gensrc_post_jdk_jartool() { gensrc_zh_hk jdk.jartool; }
gensrc_pre_jdk_javadoc() { gensrc_version_properties jdk.javadoc jdk/javadoc/internal/tool/resources/version.properties; }
gensrc_post_jdk_javadoc() { gensrc_zh_hk jdk.javadoc; }
gensrc_pre_jdk_jdeps() {
  gensrc_version_properties jdk.jdeps com/sun/tools/javap/resources/version.properties &&
  gensrc_version_properties jdk.jdeps com/sun/tools/jdeps/resources/version.properties
}
gensrc_post_jdk_jdeps() { gensrc_zh_hk jdk.jdeps; }
gensrc_post_jdk_jdi() {
  # Gensrc.gmk's JAVA_FILE: jdwpgen's JDWP.java (its -include header and -doc page are no platform input)
  mkdir -p "$gout/jdk.jdi/com/sun/tools/jdi"
  gensrc_tool_logged - build.tools.jdwpgen.Main "$jdk/src/java.se/share/data/jdwp/jdwp.spec" -jdi "$gout/jdk.jdi/com/sun/tools/jdi/JDWP.java" &&
  gensrc_zh_hk jdk.jdi
}
gensrc_post_jdk_jlink() { gensrc_zh_hk jdk.jlink && gensrc_module_info jdk.jlink; }
gensrc_post_jdk_jpackage() { gensrc_module_info jdk.jpackage; }
gensrc_pre_jdk_jshell() { gensrc_version_properties jdk.jshell jdk/internal/jshell/tool/resources/version.properties; }
gensrc_post_jdk_jshell() { gensrc_zh_hk jdk.jshell; }
gensrc_post_jdk_management_agent() { gensrc_zh_hk jdk.management.agent; }

# gensrc_module MODULE: the module's tree made afresh, its time and peak in WORK/gensrc/log/MODULE.time
gensrc_module() {
  local m=$1 fn=${1//./_} t0
  gm=$m
  t0=$(date +%s%N)
  rm -rf "${gout:?}/$m"; mkdir -p "$gout/$m"
  : > "$work/gensrc/log/$m.log"
  if declare -F "gensrc_pre_$fn" > "$quiet"; then "gensrc_pre_$fn" || return 1; fi
  gensrc_replay "$m" || return 1
  if declare -F "gensrc_post_$fn" > "$quiet"; then "gensrc_post_$fn" || return 1; fi
  echo $(( ($(date +%s%N) - t0) / 1000000 )) > "$work/gensrc/log/$m.ms"
}

# gknown_tsv: MODULE FILE BUILD OWN WHY, a line a known entry
gknown_tsv() {
  [ -f "$1" ] || return 0
  sed -n 's/^.*{:module "\([^"]*\)" :file "\([^"]*\)" :build "\([^"]*\)" :own "\([^"]*\)" :why :\([^ }]*\)}.*$/\1\t\2\t\3\t\4\t\5/p' "$1"
}
# gensrc_sums DIR: FILE<TAB>SHA-256 of every .java under DIR, sorted
gensrc_sums() { (cd "$1" && find . -name '*.java' | sed 's|^\./||' | LC_ALL=C sort | xargs -d '\n' -r sha256sum | awk '{h = $1; sub(/^[0-9a-f]+  /, ""); print $0 "\t" h}'); }

# gensrc_judge: each module's .java files against the build's (module_diffs, module_judge over the known file), into
# WORK/gensrc/summary.tsv and generated.edn; then the two negative controls; status 1 on any unexplained or stale
gensrc_judge() {
  local m st=0 k u s r n kt=$work/gensrc/known.tsv
  gknown_tsv "$gknown" > "$kt"
  printf 'MODULE\tFILES\tEQUAL\tKNOWN\tUNEXPLAINED\tSTALE\tRESULT\tMS\n' > "$work/gensrc/summary.tsv"
  : > "$work/gensrc/explain.txt"
  for m in $gmods; do
    gensrc_sums "$gbuild/support/gensrc/$m" > "$work/gensrc/build-$m.sums"
    gensrc_sums "$gout/$m" > "$work/gensrc/own-$m.sums"
    module_diffs "$work/gensrc/build-$m.sums" "$work/gensrc/own-$m.sums" > "$work/gensrc/diffs-$m.tsv"
    module_judge "$m" "$work/gensrc/diffs-$m.tsv" "$kt" > "$work/gensrc/judge-$m.txt"
    read -r k u s < "$work/gensrc/judge-$m.txt"
    sed -n "2,\$s/^/$m\t/p" "$work/gensrc/judge-$m.txt" >> "$work/gensrc/explain.txt"
    r=$(module_result "$k" "$u" "$s")
    n=$(wc -l < "$work/gensrc/build-$m.sums")
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$m" "$n" "$((n - $(awk -F'\t' '$2 != "-"' "$work/gensrc/diffs-$m.tsv" | wc -l)))" \
      "$k" "$u" "$s" "$r" "$(cat "$work/gensrc/log/$m.ms" 2>"$quiet")" >> "$work/gensrc/summary.tsv"
    case $r in differ|stale) st=1 ;; esac
  done
  {
    echo ";; bin/javac-vendor gensrc (JCS359 (a)(2)): each generated .java against the build tree's support/gensrc"
    echo "{:generators \"the JDK build's tools on javac's tool executable $(basename "$(dirname "$gexe")")\""
    echo " :files {"
    for m in $gmods; do
      awk -F'\t' -v m="$m" 'FILENAME == ARGV[1] { d[$1] = ($2 == "-") ? ":no-reference" : (($3 == "-") ? ":not-generated" : ":different"); next }
        !($1 in d) { d[$1] = ":same" } END { for (f in d) print "  \"" m "/" f "\" " d[f] }' \
        "$work/gensrc/diffs-$m.tsv" "$work/gensrc/build-$m.sums"
    done | LC_ALL=C sort
    echo "}}"
  } > "$work/gensrc/generated.edn"
  # the negative controls (GTS81 (a)), on the run's own results: (1) a module with a known entry, that entry taken away,
  # must be differ; (2) an entry added for an equal file must be stale
  local cm cf ok=1
  cm=$(awk -F'\t' '{print $1}' "$kt" | while read -r m; do case " $gmods " in *" $m "*) echo "$m"; break ;; esac; done)
  if [ -n "$cm" ]; then
    awk -F'\t' -v m="$cm" 'BEGIN { d = 0 } $1 == m && !d { d = 1; next } { print }' "$kt" > "$work/gensrc/control1.tsv"
    read -r k u s < <(module_judge "$cm" "$work/gensrc/diffs-$cm.tsv" "$work/gensrc/control1.tsv")
    [ "$(module_result "$k" "$u" "$s")" = differ ] || { echo "javac-vendor gensrc: control 1 ($cm less a known entry) did not fail" >&2; ok=0; }
  fi
  m=${gmods%% *}
  cf=$(awk -F'\t' 'NR == 1 { print $1 "\t" $2 }' "$work/gensrc/own-$m.sums")
  if [ -n "$cf" ] && ! grep -q "^${cf%%$'\t'*}"$'\t' "$work/gensrc/diffs-$m.tsv"; then
    { cat "$kt"; printf '%s\t%s\t%s\t%s\tcontrol\n' "$m" "${cf%%$'\t'*}" "${cf#*$'\t'}" "${cf#*$'\t'}"; } > "$work/gensrc/control2.tsv"
    read -r k u s < <(module_judge "$m" "$work/gensrc/diffs-$m.tsv" "$work/gensrc/control2.tsv")
    [ "$(module_result "$k" "$u" "$s")" = stale ] || { echo "javac-vendor gensrc: control 2 ($m, an entry for an equal file) did not fail" >&2; ok=0; }
  fi
  [ $ok = 1 ] || st=1
  return $st
}

# gensrc_judge_pin: each module's .java files' digest (own.bash's gensrc_hash) against the own pin's gensrc/MODULE line, into
# WORK/gensrc/summary.tsv; a negative control (a module's tree less one file must not match); status 1 on any difference
gensrc_judge_pin() {
  local m st=0 a b f
  printf 'MODULE\tFILES\tPIN\tRESULT\tMS\n' > "$work/gensrc/summary.tsv"
  : > "$work/gensrc/explain.txt"
  for m in $gmods; do
    a=$(gensrc_hash "$gout/$m"); b=$(pin_line "$out/platform.sha256" "gensrc/$m")
    printf '%s\t%s\t%s\t%s\t%s\n' "$m" "$(find "$gout/$m" -name '*.java' | wc -l)" "${b:0:12}" "$([ "$a" = "$b" ] && echo equal || echo differ)" \
      "$(cat "$work/gensrc/log/$m.ms" 2>"$quiet")" >> "$work/gensrc/summary.tsv"
    [ "$a" = "$b" ] || { echo "$m	differs from the pin: $a, pinned ${b:-none}" >> "$work/gensrc/explain.txt"; st=1; }
  done
  m=${gmods%% *}
  f=$(cd "$gout/$m" && find . -name '*.java' | LC_ALL=C sort | head -1)
  if [ -n "$f" ]; then
    rm -rf "$work/gensrc/control"; cp -r "$gout/$m" "$work/gensrc/control"; rm -f "$work/gensrc/control/$f"
    if [ "$(gensrc_hash "$work/gensrc/control")" = "$(pin_line "$out/platform.sha256" "gensrc/$m")" ]; then
      echo "javac-vendor gensrc: the control ($m less $f) matches the pin" >&2; st=1
    else echo "   control ($m less ${f#./}): differs from the pin, as it must" >&2; fi
  fi
  return $st
}

# gensrc MODULE...: the modules' generated sources made into WORK/gensrc/out (the platform for the tools that compile,
# flagsgenerator's, made first), then judged
gensrc() {
  local m t0 st=0 c
  t0=$(date +%s)
  # with the own pin (JOP3 (a)'s weekly rebuild) each module is judged by the pin's gensrc/MODULE line, no build tree read;
  # JAVAC_VENDOR_ORACLE=1 (the oracle tier's javac-platform-oracle, JOP6 (a)) against the build tree's as before
  local ref=build
  [ "$(platform_kind)" = own ] && [ -z "${JAVAC_VENDOR_ORACLE:-}" ] && ref=pin
  if [ $ref = pin ]; then gbuild={build}
  else
    c=$(conf); [ -n "$c" ] || fail "no JDK build tree in $jdk/build (its support/gensrc, the reference)"
    gbuild=$(dirname "$c")
  fi
  gexe=${JAVAC_EXE:-$(gen_exe)}
  [ -x "$gexe" ] || fail "no $gexe (bin/javac-vendor build, JAVAC_EXE or REPR_JAVA_EXE)"
  [ -f "$pgensrc" ] || fail "no $pgensrc (bin/javac-vendor platform-gensrc)"
  gmods=${*:-$gensrc_modules}; gmods=$(echo $gmods)
  gout=$work/gensrc/out
  mkdir -p "$work/gensrc/log" "$work/tmp" "$gout"
  rm -f "$work/gensrc"/tools/*/ok
  case " $gmods " in *" jdk.compiler "*) platform "$work/platform" || fail "the platform differs from its pin (flagsgenerator's)" ;; esac
  for m in $gmods; do
    echo "   $m" >&2
    gensrc_module "$m" || { st=1; echo "$m" >> "$work/gensrc/failed.txt"; }
  done
  if [ $ref = pin ]; then gensrc_judge_pin || st=1; else gensrc_judge || st=1; fi
  cat "$work/gensrc/summary.tsv" >&2
  [ ! -s "$work/gensrc/explain.txt" ] || { echo "   differences not explained by ${gknown#$root/}:" >&2; head -30 "$work/gensrc/explain.txt" >&2; }
  echo "   ($(since "$t0"))" >&2
  return $st
}

# The own platform's module descriptors (JCS359 (a)(3)): each module's javac descriptor extended as the build extends it,
# by test/javac-platform/modinfo.ae on the tools' host: (1) AddPackagesAttribute's ModulePackages (the packages of the
# module's class files and its resource-only packages, OUT/platform-gensrc.edn's resource-packages), against the build's
# exploded descriptor; (2) jmod create's version, main class, target platform and resolution flags (OUT/platform-build.edn's
# :jmod), against the jmod's; java.base's ModuleHashes not written (JCS364 (a): OUT/platform-descriptors-known.edn). The
# input is stage 1's own descriptor and class packages (WORK/classes/modules/MODULE/module-info.class, packages.txt) when
# classes modules has run for the module (from own), else the build's exploded descriptor and classes (from exploded:
# step 2 alone proves the writer). Into WORK/descriptors/MODULE/{exploded,jmod}.class and WORK/descriptors/summary.tsv
dknown=${JAVAC_VENDOR_DESCRIPTORS_KNOWN:-$out/platform-descriptors-known.edn}
# descriptor_args MODULE FILE: modinfo.ae's options for jmod create's step, from platform-build.edn's :jmod (its version,
# target platform, main class and resolution flags), a line each into FILE (descriptors' and the own platform's, own.bash)
descriptor_args() {
  local ja=() resn=0 jm=()
  pb_get "$1" jmod > "$2.jmod"
  mapfile -t jm < "$2.jmod"
  while [ ${#jm[@]} -gt 0 ]; do
    case ${jm[0]} in
      --module-version) ja+=(--version "${jm[1]}"); jm=("${jm[@]:2}") ;;
      --target-platform) ja+=(--target "${jm[1]}"); jm=("${jm[@]:2}") ;;
      --main-class) ja+=(--main-class "${jm[1]}"); jm=("${jm[@]:2}") ;;
      --do-not-resolve-by-default) resn=$((resn | 1)); jm=("${jm[@]:1}") ;;
      --warn-if-resolved=deprecated) resn=$((resn | 2)); jm=("${jm[@]:1}") ;;
      --warn-if-resolved=deprecated-for-removal) resn=$((resn | 4)); jm=("${jm[@]:1}") ;;
      --warn-if-resolved=incubating) resn=$((resn | 8)); jm=("${jm[@]:1}") ;;
      *) jm=("${jm[@]:1}") ;;
    esac
  done
  [ $resn = 0 ] || ja+=(--resolution "$resn")
  : > "$2"; [ ${#ja[@]} = 0 ] || printf '%s\n' "${ja[@]}" > "$2"
}
descriptors() {
  local c b m w from st=0 t0 mex x r1 r2 jm k u s kt
  t0=$(date +%s)
  c=$(conf); [ -n "$c" ] || fail "no JDK build tree in $jdk/build (the reference)"
  b=$(dirname "$c")
  mex=$(. "$root/bin/lib/host.bash" && arbace_host_exe) || fail "no tools' host"
  kt=$work/descriptors/known.tsv
  mkdir -p "$work/descriptors"
  gknown_tsv "$dknown" > "$kt"
  printf 'MODULE\tFROM\tEXPLODED\tJMOD\n' > "$work/descriptors/summary.tsv"
  : > "$work/descriptors/explain.txt"
  for m in ${*:-$(platform_modules)}; do
    w=$work/descriptors/$m; rm -rf "$w"; mkdir -p "$w"
    if [ -f "$work/classes/modules/$m/module-info.class" ] && [ -f "$work/classes/modules/$m/packages.txt" ]; then
      from=own
      cp "$work/classes/modules/$m/packages.txt" "$w/classes.txt"
      x=$work/classes/modules/$m/module-info.class
    else
      from=exploded
      (cd "$b/jdk/modules/$m" && find . -name '*.class' ! -path ./module-info.class | sed 's|^\./||; s|/[^/]*$||' | LC_ALL=C sort -u | tr / .) > "$w/classes.txt"
      x=$b/jdk/modules/$m/module-info.class
    fi
    { cat "$w/classes.txt"; pg_records | awk -F'\037' -v m="$m" '$1 == "resource-packages" && $2 == m { for (i = 3; i <= NF; i++) print $i }'; } |
      LC_ALL=C sort -u > "$w/packages.txt"
    # step 1 (own only): the exploded descriptor
    r1=-
    if [ $from = own ]; then
      "$mex" "$root/test/javac-platform/modinfo.ae" "$x" "$w/exploded.class" --packages-file "$w/packages.txt" > "$w/log" 2>&1 || r1=failed
      if [ $r1 != failed ]; then cmp -s "$w/exploded.class" "$b/jdk/modules/$m/module-info.class" && r1=equal || r1=differ; x=$w/exploded.class; fi
    fi
    # step 2: jmod create's
    descriptor_args "$m" "$w/modinfo.args"
    local ja=(); mapfile -t ja < "$w/modinfo.args"
    unzip -p "$b/images/jmods/$m.jmod" classes/module-info.class > "$w/build-jmod.class" 2> "$w/unzip.log"
    r2=failed
    if [ "$r1" != failed ] && "$mex" "$root/test/javac-platform/modinfo.ae" "$x" "$w/jmod.class" --packages-file "$w/packages.txt" "${ja[@]}" >> "$w/log" 2>&1; then
      if cmp -s "$w/jmod.class" "$w/build-jmod.class"; then r2=equal
      else
        printf 'module-info.class\t%s\t%s\n' "$(sha256sum < "$w/build-jmod.class" | cut -c1-64)" "$(sha256sum < "$w/jmod.class" | cut -c1-64)" > "$w/diffs.tsv"
        read -r k u s < <(module_judge "$m" "$w/diffs.tsv" "$kt")
        r2=$(module_result "$k" "$u" "$s")
        [ "$r2" = known ] || printf '%s\tunexplained\tmodule-info.class\t%s\n' "$m" "$(cut -f2- "$w/diffs.tsv")" >> "$work/descriptors/explain.txt"
      fi
    fi
    printf '%s\t%s\t%s\t%s\n' "$m" "$from" "$r1" "$r2" >> "$work/descriptors/summary.tsv"
    case "$r1 $r2" in *failed*|*differ*|*stale*) st=1 ;; esac
  done
  # a known entry of a module not run, or whose descriptor is equal, is stale
  awk -F'\t' '{print $1}' "$kt" | while read -r m; do
    r2=$(awk -F'\t' -v m="$m" '$1 == m {print $4}' "$work/descriptors/summary.tsv")
    [ "$r2" != equal ] || { echo "$m	stale	module-info.class" >> "$work/descriptors/explain.txt"; }
  done
  grep -q 'stale' "$work/descriptors/explain.txt" && st=1
  # the negative control (GTS81 (a)): the first module's step 2 again with its last package left out must differ
  m=$(awk -F'\t' 'NR == 2 {print $1}' "$work/descriptors/summary.tsv")
  if [ -n "$m" ] && [ -f "$work/descriptors/$m/jmod.class" ]; then
    w=$work/descriptors/$m
    sed '$d' "$w/packages.txt" > "$w/control-packages.txt"
    x=$b/jdk/modules/$m/module-info.class; [ ! -f "$w/exploded.class" ] || x=$w/exploded.class
    local ca=(); mapfile -t ca < "$w/modinfo.args"
    "$mex" "$root/test/javac-platform/modinfo.ae" "$x" "$w/control.class" --packages-file "$w/control-packages.txt" "${ca[@]}" > "$w/control.log" 2>&1
    if cmp -s "$w/control.class" "$w/build-jmod.class"; then
      echo "javac-vendor descriptors: the control ($m less a package) equals the build's descriptor" >&2; st=1
    else
      echo "   control ($m less its last package): differs, as it must" >&2
    fi
  fi
  cat "$work/descriptors/summary.tsv" | awk -F'\t' 'NR > 1 { n[$2 " " $3 " " $4]++ } END { for (k in n) print "   " n[k] " modules from " k }' >&2
  [ ! -s "$work/descriptors/explain.txt" ] || cat "$work/descriptors/explain.txt" >&2
  echo "   ($(since "$t0"))" >&2
  return $st
}
