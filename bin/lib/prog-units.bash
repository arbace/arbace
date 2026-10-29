# A program's units (doc/GATE-SURVEY.md, GT5, GTS4 (b), (c)): the digest of the Go forms j2g made of a
# program (a --out's prog/: no version stamp, bin/arbace-go links it in), split by namespace, so that a
# step of bin/gate that never loads some namespaces is keyed by the rest. Sourced. Defines:
#
#   prog_units TREE:NAME...
#       prints units.tsv: UNIT<TAB>SHA, one line a unit in order, UNIT a namespace of the trees'
#       namespaces.txt (its compiled package go/arbace/aot_MUNGED, dir and file, and its embedded
#       source go/arbace/cmd/arbace/res/PATH.{ae,clj,cljc}.z) or -core (every other file: the runtime's
#       classes, jrt, goi's table, the main package, the other resources). SHA: the SHA-256 of
#       sha256sum's lines (HASH, two spaces, NAME/PATH) of the unit's files in order. NAME tells the
#       trees apart (prog, tests: the executable's and the test build's)

prog_units() {
  local tmp spec d n
  tmp=$(mktemp -d "${TMPDIR:-/tmp}/prog-units.XXXXXX") || return 1
  : > "$tmp/files"; : > "$tmp/ns"
  for spec in "$@"; do
    d=${spec%:*} n=${spec##*:}
    [ -d "$d" ] || { echo "prog_units: no tree $d" >&2; rm -rf "$tmp"; return 1; }
    (cd "$d" && find . -type f | sed 's|^\./||' | LC_ALL=C sort | xargs -r -d '\n' -n 256 -P 8 sha256sum) \
      | sed "s|^\([0-9a-f]*\)  |\1  $n/|" >> "$tmp/files" || { rm -rf "$tmp"; return 1; }
    [ ! -f "$d/namespaces.txt" ] || cat "$d/namespaces.txt" >> "$tmp/ns"
  done
  mkdir -p "$tmp/u"
  # each file's unit: UNIT<TAB>HASH  NAME/PATH
  awk -v dir="$tmp/u" '
    FILENAME ~ /\/ns$/ { if ($1 == "") next
      m = $1; gsub(/[.-]/, "_", m); pkg["aot_" m] = $1
      r = $1; gsub(/\./, "/", r); gsub(/-/, "_", r); res[r] = $1; next }
    { h = $1; p = substr($0, 67); i = index(p, "/"); rel = substr(p, i + 1); u = "-core"
      if (match(rel, /^go\/arbace\/aot_[A-Za-z0-9_]+/)) {
        k = substr(rel, 11, RLENGTH - 10); rest = substr(rel, RLENGTH + 1)
        if ((rest == "" || rest ~ /^\// || rest == ".ae") && (k in pkg)) u = pkg[k]
      } else if (match(rel, /^go\/arbace\/cmd\/arbace\/res\/.*\.(ae|clj|cljc)\.z$/)) {
        r = substr(rel, 26); sub(/\.(ae|clj|cljc)\.z$/, "", r)
        if (r in res) u = res[r]
      }
      print h "  " p > (dir "/" u) }
  ' "$tmp/ns" "$tmp/files" || { rm -rf "$tmp"; return 1; }
  for f in $(ls "$tmp/u" | LC_ALL=C sort); do
    printf '%s\t%s\n' "$f" "$(LC_ALL=C sort -k2 "$tmp/u/$f" | sha256sum | cut -c1-64)"
  done
  rm -rf "$tmp"
}
