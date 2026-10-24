# repr/java's host as the provenance files name it (doc/classes/JAVA-FRONTEND-SURVEY.md, "As built:
# JN5", "As built: JN6"): javac's tool executable, the only host of repr/java's read since JN6 (until
# then the frozen JVM too, REPR_JAVA_HOST=jvm, with these functions in bin/lib/frozen.bash). Sourced,
# with $root set to the repository root, by the provenance writers (bin/jrt-convert,
# bin/clojure-tests --java-fixtures, bin/javac-vendor). Here, under bin/lib/repr-java, the check
# scripts' directory, so that the tool's key (bin/lib/javac-key.bash) does not hold them. Defines:
#
#   repr_java_host     "arbace-javac KEY": javac's tool executable (REPR_JAVA_EXE, default
#                      target/arbace-go/<arch>/arbace-javac) by the key of its sources (JFS30 (b): two
#                      builds of one commit name it alike), refused when the tool is not built from
#                      the checkout's sources (REPR_JAVA_STALE_OK=1 warns instead)
#   repr_java_platform the platform javac on Arbace attributes against, as the provenance's :jdk
#                      names it (JFS35 (b), JN6; until then `java -version`'s first line): "platform
#                      SHA-256" of its pin, jdk/javac/platform.sha256 (jdk26u's build tree's
#                      exploded modules with the jmods' descriptors, bin/javac-vendor platform)
#   repr_java_provenance_same A B
#                      provenance files A and B the same but for the converter's :host line,
#                      which names the tool that wrote the file (its sources' key), not what the
#                      forms are (JFS32): verify requires the rest byte for byte and reports the host
#   repr_java_reader_hash
#                      the SHA-256 the provenance files give repr/java as converter: of its reader's
#                      sources, what converts Java into forms (arbace/repr/java.ae and convert, its
#                      interfaces tree and attr (JFS66 (b)),
#                      javac, jtypes, resolve, forms, names, layout, full) and bin/repr-java; not the
#                      printer or the round trip's comparator, which change no forms (PRINTER-NOTES.md,
#                      JP1)

repr_java_host() {
  local e k
  e=${REPR_JAVA_EXE:-$root/target/arbace-go/$(case $(uname -m) in aarch64) echo arm64 ;; *) echo amd64 ;; esac)/arbace-javac}
  [ -x "$e" ] || { echo "repr_java_host: no javac tool executable $e (bin/javac-vendor build)" >&2; return 1; }
  . "$root/bin/lib/javac-key.bash"
  k=$(arbace_javac_fresh repr_java_host "$e") || return 1
  echo "arbace-javac $k"
}

repr_java_platform() {
  [ -f "$root/jdk/javac/platform.sha256" ] || { echo "repr_java_platform: no jdk/javac/platform.sha256" >&2; return 1; }
  echo "platform $(sha256sum < "$root/jdk/javac/platform.sha256" | cut -c1-64)"
}

repr_java_provenance_same() {
  local a b st
  a=$(mktemp) && b=$(mktemp) || return 1
  grep -v '^ *:host ' "$1" > "$a"; grep -v '^ *:host ' "$2" > "$b"
  cmp -s "$a" "$b"; st=$?
  rm -f "$a" "$b"
  return $st
}

repr_java_reader_hash() {
  (cd "$root" && for f in arbace/repr/java.ae arbace/repr/java/{attr,convert,forms,full,javac,jtypes,layout,names,resolve,tree}.ae bin/repr-java; do
     echo "$f"; done | LC_ALL=C sort | xargs sha256sum | sha256sum | cut -c1-64)
}
