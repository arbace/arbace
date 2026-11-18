# The own platform (doc/classes/JAVA-FRONTEND-SURVEY.md, "Own platform", JOP1-JOP8 (a), the user 2026-11-17; JCS359 (a)
# stages 4-5): jdk26u's 68 modules compiled by javac on Arbace from jdk26u's sources and the own generated sources, with
# the own descriptors, no class file of the JDK build read. Sourced by bin/javac-vendor (its pb_*, module_hash, gensrc's
# descriptor_args and the variables root, out, work, jdk, pinned, quiet); out of every key and of the Java steps' inputs,
# as test/javac-platform/'s other files: the platform it makes is pinned by its content, OUT/platform.sha256.
#
# The pin's kind (JOP2 (a)): OUT/platform.sha256 holds "# kind: own" for the own platform (each module's line the SHA-256 of
# its class files' and descriptor's sha256sum listing, as module_hash; each generated module's line "gensrc/MODULE" the
# SHA-256 of its .java files' listing), else it is the build tree's (the JFS15 (b) pin, no kind line). The night of the
# switch (JOP4 (a), .tmp/goal/java-platform-night.sh) rewrites the pin: that is the one setting everything below follows
# (bin/lib/repr-java/platform.bash's repr_java_platform_kind reads the same line).
#
#   own_build OUT [GENSRC]   the platform made into OUT (JOP3 (a)): OUT/gensrc the generated sources (GENSRC's, or the
#                            committed pack OUT/../platform-gensrc.tar.xz's, own_gensrc_pack), OUT/classes/MODULE each
#                            module compiled as the build compiles it (platform-build.edn's options and sources,
#                            --module-source-path over OUT/gensrc and jdk26u's src, --system none: no class file read),
#                            OUT/descriptors/MODULE its descriptor extended as the image holds it (modinfo.ae), OUT/modules
#                            the platform (MODULE/ its class files and module-info.class), OUT/listing.tsv a line a file
#                            (MODULE CLASS SHA-256), OUT/pin.sha256 its pin; --jobs N (own_jobs) modules at once, a module
#                            of more than JAVAC_VENDOR_ALONE files (1000) alone; each javac under GOMEMLIMIT (default 2GiB:
#                            every module at most 2.07 GB measured, 2026-11-17)
#   own_check DIR PIN        DIR/pin.sha256's module and gensrc lines against PIN's: status 1 naming each that differs
#   own_cache                the platform of the committed own pin: ARBACE_GATE_EXES/platform-PIN20 (JCS365 (a)), built on
#                            a miss (own_build, then own_check against the pin, then moved in place under a lock), printed
#   own_gensrc_pack SRC FILE the generated sources' .java files of SRC (MODULE/...) packed into FILE, deterministic (sorted
#                            names, no owners, mtime 0, xz -9e on one thread)

# platform_kind: own when OUT/platform.sha256 says "# kind: own", else build
platform_kind() { if grep -qx '# kind: own' "$out/platform.sha256" 2>"$quiet"; then echo own; else echo build; fi; }
# pin_modules [PIN]: the pin's modules (its lines but the gensrc ones), in its order
pin_modules() { sed -n 's/^[0-9a-f]\{64\}  \([^/ ]*\)$/\1/p' "${1:-$out/platform.sha256}"; }
# pin_line PIN NAME: the pin's SHA-256 of NAME (MODULE or gensrc/MODULE)
pin_line() { awk -v n="$2" '$2 == n && $1 ~ /^[0-9a-f]+$/ && length($1) == 64 {print $1; exit}' "$1"; }
# gensrc_hash DIR: the SHA-256 of DIR's .java files' sha256sum listing (sorted, relative)
gensrc_hash() { (cd "$1" && find . -name '*.java' | LC_ALL=C sort | xargs -d '\n' -r sha256sum | sha256sum | cut -c1-64); }
# own_pack: the committed generated sources (JOP9 (a), proposed: the bootstrap a cold build compiles from, Arbace's own output)
own_pack=$out/platform-gensrc.tar.xz

own_gensrc_pack() {
  local s=$1 f=$2
  [ -d "$s" ] || { echo "javac-vendor: no generated sources in $s" >&2; return 1; }
  (cd "$s" && find . -name '*.java' | sed 's|^\./||' | LC_ALL=C sort > "$TMPDIR/javac-vendor.pack-list" &&
   tar --no-recursion --owner=0 --group=0 --numeric-owner --mode=0644 --mtime=@0 --format=gnu \
       -cf - -T "$TMPDIR/javac-vendor.pack-list") | xz -9e -T1 > "$f.tmp" && mv "$f.tmp" "$f"
}

# own_compile MODULE OUT: one module by javac on Arbace into OUT/classes/MODULE (javac's -d of a multi-module compilation:
# OUT/classes/MODULE/MODULE/...), its log, peak and time in OUT/log/MODULE.*
own_compile() {
  local m=$1 o=$2 w=$2/classes/$1 o2=() t0 r=0
  # the data's placeholders: {gensrc} the own generated sources, {build} none (no option or source names the build tree)
  local PB_B={build} PB_GENSRC=$2/gensrc
  rm -rf "$w"; mkdir -p "$w" "$o/log"
  pb_get "$m" options > "$o/log/$m.options.raw"
  if grep -q '{build}' "$o/log/$m.options.raw"; then echo "javac-vendor: $m's options name the build tree" > "$o/log/$m.log"; return 1; fi
  pb_concrete "{build}" < "$o/log/$m.options.raw" > "$o/log/$m.options"
  mapfile -t o2 < "$o/log/$m.options"
  pb_files_rel "$m" | pb_concrete "{build}" > "$o/log/$m.files"
  if [ "$(wc -l < "$o/log/$m.files")" != "$(pb_scalar "$m" files)" ] ||
     [ "$(pb_files_rel "$m" | sha256sum | cut -c1-64)" != "$(pb_scalar "$m" files-sha256)" ]; then
    echo "javac-vendor: $m's sources are not the data's (jdk26u's checkout moved?)" > "$o/log/$m.log"; return 1
  fi
  t0=$(date +%s%N)
  (cd "$w" && TMPDIR=$o/tmp GOMEMLIMIT=${GOMEMLIMIT:-2GiB} busybox time -v "$javac_exe" "$o/javac.ae" "${o2[@]}" -d "$w" "@$o/log/$m.files") \
    > "$o/log/$m.log" 2> "$o/log/$m.err" || r=1
  echo $(( ($(date +%s%N) - t0) / 1000000 )) > "$o/log/$m.ms"
  sed -n 's/.*Maximum resident set size (kbytes): *//p' "$o/log/$m.err" | tail -1 > "$o/log/$m.kb"
  [ $r = 0 ] && [ -f "$w/$m/module-info.class" ] || { echo "javac-vendor: javac on Arbace failed on $m ($o/log/$m.log)" >&2; return 1; }
}

# own_descriptor MODULE OUT: the module's descriptor extended as the build does (AddPackagesAttribute's ModulePackages, then
# jmod create's version, target, main class, resolution), as descriptors' steps 1 and 2 make it, into OUT/descriptors/MODULE
own_descriptor() {
  local m=$1 o=$2 w=$2/descriptors/$1 c=$2/classes/$1/$1 ja=()
  rm -rf "$w"; mkdir -p "$w"
  { (cd "$c" && find . -name '*.class' ! -path ./module-info.class | sed 's|^\./||; s|/[^/]*$||' | LC_ALL=C sort -u | tr / .)
    pg_records | awk -F'\037' -v m="$m" '$1 == "resource-packages" && $2 == m { for (i = 3; i <= NF; i++) print $i }'; } |
    LC_ALL=C sort -u > "$w/packages.txt"
  descriptor_args "$m" "$w/modinfo.args"
  mapfile -t ja < "$w/modinfo.args"
  "$mex" "$root/test/javac-platform/modinfo.ae" "$c/module-info.class" "$w/exploded.class" --packages-file "$w/packages.txt" > "$w/log" 2>&1 &&
    "$mex" "$root/test/javac-platform/modinfo.ae" "$w/exploded.class" "$w/jmod.class" --packages-file "$w/packages.txt" "${ja[@]}" >> "$w/log" 2>&1 ||
    { echo "javac-vendor: $m's descriptor ($w/log)" >&2; return 1; }
}

# own_pin_lines DIR: the pin's lines of the platform in DIR (DIR/modules, DIR/gensrc)
own_pin_lines() {
  local d=$1 m
  echo "# The platform of javac on Arbace, Arbace's own (doc/classes/JAVA-FRONTEND-SURVEY.md, \"Own platform\", JOP1-JOP8 (a)):"
  echo "# jdk26u at $pinned, its 68 modules compiled by javac on Arbace from its sources and the own generated sources"
  echo "# (jdk/javac/platform-gensrc.tar.xz; bin/javac-vendor platform-own), each with its descriptor as the image holds it;"
  echo "# per module the SHA-256 of its files' sha256sum listing, per generated module (gensrc/MODULE) of its .java files'"
  echo "# kind: own"
  for m in $(own_modules); do echo "$(module_hash "$d/modules/$m")  $m"; done
  for m in $(cd "$d/gensrc" && ls | LC_ALL=C sort); do echo "$(gensrc_hash "$d/gensrc/$m")  gensrc/$m"; done
}

own_check() {
  local d=$1 pin=$2 n st=0 a b
  for n in $(pin_modules "$pin") $(sed -n 's/^[0-9a-f]\{64\}  \(gensrc\/[^ ]*\)$/\1/p' "$pin"); do
    a=$(pin_line "$d/pin.sha256" "$n"); b=$(pin_line "$pin" "$n")
    [ "$a" = "$b" ] || { echo "javac-vendor: the own platform's $n: ${a:-none}, pinned ${b:-none} ($pin)" >&2; st=1; }
  done
  for n in $(pin_modules "$d/pin.sha256"); do [ -n "$(pin_line "$pin" "$n")" ] || { echo "javac-vendor: $n is not pinned ($pin)" >&2; st=1; }; done
  return $st
}

own_jobs=1
# own_modules: the modules made, platform-build.edn's (JAVAC_VENDOR_OWN_MODULES: some, a partial platform for tests by hand)
own_modules() { if [ -n "${JAVAC_VENDOR_OWN_MODULES:-}" ]; then printf '%s\n' $JAVAC_VENDOR_OWN_MODULES; else pb_modules; fi; }
own_build() {
  local o=$1 g=${2:-} m n big=${JAVAC_VENDOR_ALONE:-1000} st=0 t0
  t0=$(date +%s)
  javac_exe=${JAVAC_EXE:-$root/target/arbace-go/amd64/arbace-javac}
  [ -x "$javac_exe" ] || { echo "javac-vendor: no $javac_exe (JAVAC_EXE)" >&2; return 1; }
  mex=$(. "$root/bin/lib/host.bash" && arbace_host_exe) || { echo "javac-vendor: no tools' host" >&2; return 1; }
  [ "$(sed -n 's/^{:jdk "\([0-9a-f]*\)"$/\1/p' "$pbuild")" = "$pinned" ] || { echo "javac-vendor: $pbuild is not of jdk26u at $pinned" >&2; return 1; }
  rm -rf "$o"; mkdir -p "$o/gensrc" "$o/classes" "$o/modules" "$o/descriptors" "$o/log" "$o/tmp"
  printf '(System/exit (jdk.internal.jrt.javac.Main/run (into-array String *command-line-args*)))\n' > "$o/javac.ae"
  if [ -n "$g" ]; then cp -r "$g/." "$o/gensrc/" || return 1
  else
    [ -f "$own_pack" ] || { echo "javac-vendor: no $own_pack (the own generated sources; bin/javac-vendor platform-own --gensrc DIR)" >&2; return 1; }
    xz -dc "$own_pack" | tar -x -C "$o/gensrc" || return 1
  fi
  echo "   own platform: $(own_modules | wc -l) modules, $own_jobs at once (javac on Arbace $javac_exe)" >&2
  for m in $(own_modules); do
    n=$(pb_scalar "$m" files)
    if [ "$n" -gt "$big" ] || [ "$own_jobs" -le 1 ]; then
      wait; own_compile "$m" "$o" || echo "$m" >> "$o/failed.txt"
    else
      while [ "$(jobs -rp | wc -l)" -ge "$own_jobs" ]; do wait -n; done
      (own_compile "$m" "$o" || echo "$m" >> "$o/failed.txt") &
    fi
  done
  wait
  [ ! -s "$o/failed.txt" ] || { echo "javac-vendor: own platform: $(wc -l < "$o/failed.txt") modules failed: $(tr '\n' ' ' < "$o/failed.txt")" >&2; return 1; }
  for m in $(own_modules); do
    own_descriptor "$m" "$o" || st=1
    rm -rf "$o/modules/$m"; mkdir -p "$o/modules/$m"
    (cd "$o/classes/$m/$m" && find . -name '*.class' ! -path ./module-info.class | LC_ALL=C sort | cpio -pdm --quiet "$o/modules/$m") || st=1
    cp "$o/descriptors/$m/jmod.class" "$o/modules/$m/module-info.class" || st=1
  done
  [ $st = 0 ] || return 1
  (cd "$o/modules" && find . -name '*.class' | sed 's|^\./||' | LC_ALL=C sort | xargs -d '\n' sha256sum |
     awk '{h = $1; sub(/^[0-9a-f]+  /, ""); m = $0; sub(/\/.*/, "", m); c = $0; sub(/^[^\/]*\//, "", c); print m "\t" c "\t" h}') > "$o/listing.tsv"
  own_pin_lines "$o" > "$o/pin.sha256"
  awk '{if ($1 ~ /^[0-9]+$/ && $1 > p) {p = $1; f = FILENAME}} END {if (p) printf "   own platform: peak %d MB (%s)\n", p / 1024, f}' "$o"/log/*.kb >&2
  echo "   own platform: $(wc -l < "$o/listing.tsv") class files in $(own_modules | wc -l) modules ($(( $(date +%s) - t0 )) s)" >&2
}

own_cache() {
  local pin=$out/platform.sha256 c
  c=$(. "$root/bin/lib/gate-cache.bash" && arbace_gate_exes "$root")/platform-$(sha256sum < "$pin" | cut -c1-20)
  if [ -f "$c/ok" ]; then echo "$c"; return 0; fi
  [ "${JAVAC_VENDOR_PLATFORM_BUILD:-1}" = 1 ] || { echo "javac-vendor: no own platform at $c (bin/javac-vendor platform-cache builds it)" >&2; return 1; }
  mkdir -p "$(dirname "$c")"
  # built beside it as PLATFORM.tmp.PID (bin/gate --prune removes such a leftover when an hour old) under the hidden lock
  # .PLATFORM.lock (kept, as the executables' locks), then moved in place
  (
    flock 9
    [ -f "$c/ok" ] && exit 0
    echo "javac-vendor: the own platform of the pin, not in $c: built (about 35-40 minutes serial, 15-20 at --jobs 3)" >&2
    own_build "$c.tmp.$$" || exit 1
    own_check "$c.tmp.$$" "$pin" || { echo "javac-vendor: the own platform built ($c.tmp.$$) is not the pin's" >&2; exit 1; }
    cp "$pin" "$c.tmp.$$/pin.committed"
    rm -rf "$c"; mv "$c.tmp.$$" "$c" && rm -rf "$c/classes" "$c/tmp" && touch "$c/ok"
  ) 9> "$(dirname "$c")/.$(basename "$c").lock" || return 1
  echo "$c"
}

# platform_oracle: the oracle tier's javac-platform-oracle (JOP6 (a)): the own pin's platform (own_cache) against the JDK build
# tree, the reference it was proven against: each module's class files but its descriptor against the build's jdk/modules
# (judged by OUT/platform-own-known.edn, as classes modules judges: known, differ, stale), each descriptor against the jmod's
# (OUT/platform-descriptors-known.edn), each generated module's .java files against the build's support/gensrc
# (OUT/platform-gensrc-known.edn); then classes modules' two negative controls on these results. WORK/oracle/summary.tsv
platform_oracle() {
  local c b m st=0 k u s r d=$work/classes/modules gk dk n
  [ "$(platform_kind)" = own ] || { echo "javac-vendor platform-oracle: the pin is the build tree's (nothing to compare)"; return 0; }
  c=$(own_cache) || return 1
  b=$(conf); [ -n "$b" ] || { echo "javac-vendor platform-oracle: no JDK build tree in $jdk/build (the oracle's reference)" >&2; return 1; }
  b=$(dirname "$b")
  rm -rf "$d"; mkdir -p "$d" "$work/oracle"
  known_tsv "$pknown" > "$d/known.tsv"; gknown_tsv "$gknown" > "$work/oracle/gknown.tsv"; gknown_tsv "$dknown" > "$work/oracle/dknown.tsv"
  : > "$d/summary.tsv"; printf 'KIND\tMODULE\tRESULT\tKNOWN\tUNEXPLAINED\tSTALE\n' > "$work/oracle/summary.tsv"
  for m in $(pin_modules); do
    mkdir -p "$d/$m"
    module_sums "$b/jdk/modules/$m" > "$d/$m/build.sums"; module_sums "$c/modules/$m" > "$d/$m/own.sums"
    module_diffs "$d/$m/build.sums" "$d/$m/own.sums" > "$d/$m/diffs.tsv"
    module_judge "$m" "$d/$m/diffs.tsv" "$d/known.tsv" > "$d/$m/judgment.txt"; read -r k u s < "$d/$m/judgment.txt"
    r=$(module_result "$k" "$u" "$s")
    printf '%s\t-\t%s\t%s\t%s\t-\t-\t%s\t%s\t%s\n' "$m" "$(wc -l < "$d/$m/build.sums")" "$(wc -l < "$d/$m/own.sums")" "$r" "$k" "$u" "$s" >> "$d/summary.tsv"
    printf 'classes\t%s\t%s\t%s\t%s\t%s\n' "$m" "$r" "$k" "$u" "$s" >> "$work/oracle/summary.tsv"
    case $r in equal|known) ;; *) st=1; tail -n +2 "$d/$m/judgment.txt" | sed "s/^/   $m: /" >&2 ;; esac
    # the descriptor against the jmod's
    unzip -p "$b/images/jmods/$m.jmod" classes/module-info.class > "$d/$m/jmod.class" 2>"$quiet"
    if cmp -s "$d/$m/jmod.class" "$c/modules/$m/module-info.class"; then r=equal
    else
      printf 'module-info.class\t%s\t%s\n' "$(sha256sum < "$d/$m/jmod.class" | cut -c1-64)" "$(sha256sum < "$c/modules/$m/module-info.class" | cut -c1-64)" > "$d/$m/ddiffs.tsv"
      read -r k u s < <(module_judge "$m" "$d/$m/ddiffs.tsv" "$work/oracle/dknown.tsv"); r=$(module_result "$k" "$u" "$s")
    fi
    printf 'descriptor\t%s\t%s\t-\t-\t-\n' "$m" "$r" >> "$work/oracle/summary.tsv"
    case $r in equal|known) ;; *) st=1; echo "   $m: the descriptor differs from the jmod's ($r)" >&2 ;; esac
  done
  for m in $(cd "$c/gensrc" && ls | LC_ALL=C sort); do
    gensrc_sums "$b/support/gensrc/$m" > "$work/oracle/gbuild-$m.sums"; gensrc_sums "$c/gensrc/$m" > "$work/oracle/gown-$m.sums"
    module_diffs "$work/oracle/gbuild-$m.sums" "$work/oracle/gown-$m.sums" > "$work/oracle/gdiffs-$m.tsv"
    read -r k u s < <(module_judge "$m" "$work/oracle/gdiffs-$m.tsv" "$work/oracle/gknown.tsv"); r=$(module_result "$k" "$u" "$s")
    printf 'gensrc\t%s\t%s\t%s\t%s\t%s\n' "$m" "$r" "$k" "$u" "$s" >> "$work/oracle/summary.tsv"
    case $r in equal|known) ;; *) st=1; echo "   gensrc/$m: $r" >&2 ;; esac
  done
  modules_controls || st=1
  n=$(awk -F'\t' 'NR > 1 {n[$1 " " $3]++} END {for (x in n) printf "%s %d, ", x, n[x]}' "$work/oracle/summary.tsv")
  echo "   oracle: ${n%, }"
  return $st
}
