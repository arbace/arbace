# The key of javac's tool executable (bin/arbace-go --build-java; doc/classes/JAVA-FRONTEND-SURVEY.md,
# JFS30 (b), "As built: JN5"): what the tool is built from, so that the provenance files repr/java
# writes on it (jdk/provenance.edn, jdk/javac/provenance.edn, test/java-fixtures/provenance.edn)
# name it alike for two builds of one commit, which its bytes (the version map: the commit) do
# not. Sourced, after setting root (the checkout). Defines:
#
#   arbace_javac_key ROOT  prints the SHA-256 of the sources the tool is built from: those of
#                          bin/arbace-go's gate_key (Arbace's sources, go/, overlay/ with javac's
#                          variants and jrt's own Java, jdk/ with javac's forms and edge and the
#                          platform's pin, x509roots/, the build's scripts, the seed's and Go's
#                          pins, Go's version), without what repr/java writes (the provenance
#                          files above: the tool's output, which names the key; the suite's
#                          fixtures, the test build's input) and the check scripts of
#                          bin/lib/repr-* and bin/lib/frozen.bash (they run the tool, they do not
#                          build it)
#   arbace_javac_fresh WHO EXE
#                          prints EXE's key, recorded beside it by the tool build (EXE.key), when
#                          it is the checkout's; else stops (status 1) with a message naming the
#                          build, or with REPR_JAVA_STALE_OK=1 (experiments) warns and prints it

arbace_javac_key() {
  (cd "$1" && {
    find arbace go overlay jdk x509roots bin/lib -type f ! -path jdk/provenance.edn ! -path jdk/javac/provenance.edn \
         ! -path 'bin/lib/repr-java/*' ! -path 'bin/lib/repr-go/*' ! -path 'bin/lib/repr-clojure/*' \
         ! -path 'bin/lib/repr-haskell/*' ! -path 'bin/lib/repr-rust/*' ! -path bin/lib/frozen.bash | LC_ALL=C sort
    printf '%s\n' seed/arbace-seed-go.tar.xz.sha256 seed/goroot.sha256 bin/arbace-go bin/j2g bin/repr-go bin/build-go bin/jrt bin/seed bin/goi
  } | xargs -d '\n' sha256sum
    cat "${ARBACE_GOROOT:-/root/tamago-go}/VERSION") | sha256sum | cut -c1-64
}

arbace_javac_fresh() {
  local who=$1 e=$2 k want
  k=$([ ! -f "$e.key" ] || cat "$e.key")
  want=$(arbace_javac_key "$root")
  if [ -z "$k" ] || [ "$k" != "$want" ]; then
    if [ "${REPR_JAVA_STALE_OK:-}" = 1 ]; then
      echo "$who: warning: $e is not built from the checkout's sources (key ${k:-none}, the checkout's $want); used anyway (REPR_JAVA_STALE_OK=1)" >&2
      [ -n "$k" ] || k=unknown
    else
      echo "$who: $e is not built from the checkout's sources (its key ${k:-none, no $e.key}, the checkout's $want)" >&2
      echo "$who: rebuild it: bin/javac-vendor build (or REPR_JAVA_STALE_OK=1 to use it anyway)" >&2
      return 1
    fi
  fi
  echo "$k"
}
