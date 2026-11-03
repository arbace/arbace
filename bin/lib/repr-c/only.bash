# GT8 (doc/GATE-SURVEY.md, "As built: GT8 (C's read lists)"): --only LIST for C's read checks (bin/repr-c check --tokens,
# parse-check, roundtrip, bin/ci annotations): LIST a file of ids CORPUS/NAME (blank and # lines apart), as the records'
# rows name them with a slash for the space. Sourced by the drivers.
#   c_only_set FILE CORPORA WHO   FILE made absolute; an id outside CORPORA (comma-separated) refused (exit 2); exported as
#                                 REPR_C_ONLY, which arbace.repr.c.ppcheck/lists reads: a corpus keeps its listed files
#                                 alone and writes WORK/CORPUS/only.txt, the judges' mark of a partial run (a name the
#                                 corpus does not hold fails the lists)
#   c_only_refuse WHO WHAT        exit 2 when REPR_C_ONLY is set: WHAT (--record, ...) needs whole corpora
#   c_only_marked WORK CORPORA    exit 0 when a corpus's last run under WORK was partial (CORPUS/only.txt)
c_only_set() {
  local f=$1 corpora=$2 who=$3 bad
  [ -f "$f" ] || { echo "$who: --only $f is not a file" >&2; exit 2; }
  f=$(cd "$(dirname "$f")" && pwd)/$(basename "$f")
  bad=$(grep -v '^[[:space:]]*\(#\|$\)' "$f" | awk -F/ -v a=",$corpora," 'index(a, "," $1 ",") == 0 || NF < 2' | head -5 | tr '\n' ' ')
  [ -z "$bad" ] || { echo "$who: --only lists files outside the corpora ($corpora), or not CORPUS/NAME: $bad" >&2; exit 2; }
  export REPR_C_ONLY=$f
}
c_only_refuse() {
  [ -z "${REPR_C_ONLY:-}" ] || { echo "$1: $2 records whole corpora, not --only's list" >&2; exit 2; }
}
c_only_marked() {
  local w=$1 id
  for id in ${2//,/ }; do [ -f "$w/$id/only.txt" ] && return 0; done
  return 1
}
