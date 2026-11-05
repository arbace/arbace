# Python's oracle records (queue 213; doc/ORACLE-RECORDS-SURVEY.md ORS12, "As built: Python"; doc/python/PYTHON-SURVEY.md,
# "As built: 213"): what bin/repr-python and bin/pyi ask of CPython (python3 -I -S -B over tools/pydump's scripts) read
# from records, so that no gate step outside the oracle tier needs python3. Sourced by both after $root, TMPDIR and die.
#
# The mode, REPR_PYTHON_ORACLE (both scripts):
#   records  (the default where test/oracle-records/oracle.bash is, the shared tooling of oracle-records-c) the records
#            alone: no python3; an input whose entry is missing is UNRECORDED, one whose input or helper changed STALE,
#            a committed file that differs from its manifest REFUSED: each fails the verb (ORS10 (a))
#   live     python3 runs as before (day work; the oracle tier; the default without the shared tooling)
#   record   python3 runs and its answers are recorded (by hand only, ORS5 (a): the main host, a commit of its own naming
#            :python's identity): the verb runs as live and, at its end, packs what it dumped
#   verify   python3 runs and each answer is compared with its record (the oracle tier's: ORS6 (a), a moved python3
#            reported, not failed; ORS7 (a), the crossed pair must differ): the verb's own judgment as live, then the verdict
#
# Class B, pydump's dumps (pyrec_dump): one pack a corpus, python-pydump-CORPUS (test/python/records/NAME.{sha256,tsv},
# oracle.bash's format), its members by kind: tree/REL.ae and tree/REL.row (pydump.py tree: the dump and its TSV row),
# full/REL.ae, full/REL.row (tree --full), scopes/REL.scopes.ae (scopes), tok/REL.tok, tok/REL.row (tokdump.py); a
# generated corpus's inputs too, src/REL and src.list (syntax-errors from syntax_cases.py; mutants-lex, mutants-parse,
# the checks' mutants, kept for the verify). The rows' times and peak RSS are 0 (the record's bytes do not depend on the
# run). An entry's key: the input's SHA-256 and the helper's (pydump.py, or tokdump.py, or syntax_cases.py with the
# tarball's pin), first 16 characters, "INPUT:HELPER". The essential corpora's members are committed, raw, under
# test/python/records/essential/CORPUS/ (ORS2 (a), ORS19 (a)): the essential steps need no store.
# Class A, outcomes (pyrec_outcome): a command's stdout and status keyed by its inputs, committed as
# test/python/records/outcomes/NAME.out and NAME.meta (key, status, host): spec-check's and samples' verdicts, gen --check's
# coverage, the smoke runs and REPL sessions of bin/pyi runtime check, the wheels' CPython runs.

PYREC_DIR=$root/test/python/records
PYREC_ESSENTIAL="samples synthetic errors lex-errors"
PYREC_LIB=${REPR_PYTHON_ORACLE_LIB:-$root/test/oracle-records/oracle.bash}
if [ -f "$PYREC_LIB" ]; then
  . "$PYREC_LIB"
  PYREC_MODE=${REPR_PYTHON_ORACLE:-records}
  # :python's identity (bin/gate's :host-ids :python recipe) until the shared library knows it (queue 213's patch)
  if [ "$(oracle_host_id python 2>"$TMPDIR/pyrec-id.err")" = none ] && grep -q 'no identity recipe' "$TMPDIR/pyrec-id.err"; then
    eval "pyrec_host_id_base() $(declare -f oracle_host_id | tail -n +2)"
    eval "pyrec_host_version_base() $(declare -f oracle_host_version | tail -n +2)"
    oracle_host_id() { if [ "$1" = python ]; then pyrec_python_id; else pyrec_host_id_base "$@"; fi; }
    oracle_host_version() { if [ "$1" = python ]; then command python3 --version 2>&1 | head -1; else pyrec_host_version_base "$@"; fi; }
  fi
else
  PYREC_MODE=${REPR_PYTHON_ORACLE:-live}
  [ "$PYREC_MODE" = live ] || die "REPR_PYTHON_ORACLE=$PYREC_MODE needs the oracle records' tooling ($PYREC_LIB, oracle-records-c)"
fi
case $PYREC_MODE in records|live|record|verify) ;; *) die "REPR_PYTHON_ORACLE: records, live, record or verify (not $PYREC_MODE)" ;; esac
pyrec_live() { [ "$PYREC_MODE" != records ]; }
# verify mode: python3 runs as live and each answer is compared with its record (ORS6 (a)); the tallies in PYREC_VSTATE,
# judged at the verb's end by pyrec_flush (oracle_verdict, and the crossed pair: one entry's live answer against another
# entry's record, which must differ: ORS7 (a))
PYREC_VSTATE=$root/.tmp/repr-python/records-verify.$$

pyrec_python_id() {  # the :host-ids :python recipe: the version line, python3's bytes, libpython's
  local x p m=0
  x=$(type -P python3) || { echo none; return; }
  { command python3 --version 2>"$TMPDIR/pyrec-id.err" | head -1
    sha256sum < "$(readlink -f "$x")" | cut -c1-64
    for p in /usr/lib/libpython3.*.so.1.0; do [ -f "$p" ] && { sha256sum < "$p" | cut -c1-64; m=1; }; done
    [ $m = 1 ] || echo "missing /usr/lib/libpython3.*.so.1.0"; } | sha256sum | cut -c1-64
}

pyrec_sha16() { sha256sum < "$1" | cut -c1-16; }
pyrec_helper() {  # KIND: the helper's script
  case $1 in tok) echo "$root/tools/pydump/tokdump.py" ;; src) echo "$root/tools/pydump/syntax_cases.py" ;; *) echo "$root/tools/pydump/pydump.py" ;; esac
}
pyrec_pin() { echo "$PYREC_DIR/python-pydump-$1.sha256"; }
pyrec_essential() { case " $PYREC_ESSENTIAL " in *" $1 "*) return 0 ;; esac; return 1; }

# pyrec_list BASE PATH... [--exclude PATH]...: the corpus's files as tools/pydump/corpus.py list selects them (the .py files
# under each PATH, a file PATH itself, less those under an --exclude, sorted by their bytes), without python3
pyrec_list() {
  local base=$1 p e; shift
  local -a paths=() ex=()
  while [ $# -gt 0 ]; do if [ "$1" = --exclude ]; then ex+=("${2%/}"); shift 2; else paths+=("$1"); shift; fi; done
  ( cd "$base" || exit 1
    for p in "${paths[@]}"; do
      p=${p%/}
      if [ -f "$p" ]; then printf '%s\n' "${p#./}"
      else find -H "$p" -name '*.py' \( -type f -o -xtype f \) | sed 's|^\./||'; fi
    done ) | awk -v ex="$(printf '%s\n' "${ex[@]+"${ex[@]}"}")" '
      BEGIN { n = split(ex, E, "\n") }
      { for (i = 1; i <= n; i++) if (E[i] != "" && ($0 == E[i] || index($0, E[i] "/") == 1)) next; print }' | LC_ALL=C sort -u
}

# pyrec_run_helper KIND BASE LIST OUT TSV: the helper live in ${PYREC_JOBS:-1} processes, the TSV sorted
pyrec_run_helper() {
  local kind=$1 base=$2 list=$3 out=$4 tsv=$5 i d jobs=${PYREC_JOBS:-1}
  type -P python3 >/dev/null || die "no python3 (the oracle): REPR_PYTHON_ORACLE=$PYREC_MODE runs it"
  d="$out.split.$$"; rm -rf "$d"; mkdir -p "$d" "$out"
  split -n "r/$jobs" -d "$list" "$d/part."
  for i in "$d"/part.*; do
    [ -s "$i" ] || continue
    case $kind in
      tree) command python3 -I -S -B "$root/tools/pydump/pydump.py" tree --base "$base" --list "$i" --out "$out" --tsv "$i.tsv" & ;;
      full) command python3 -I -S -B "$root/tools/pydump/pydump.py" tree --full --base "$base" --list "$i" --out "$out" --tsv "$i.tsv" & ;;
      scopes) command python3 -I -S -B "$root/tools/pydump/pydump.py" scopes --base "$base" --list "$i" --out "$out" & ;;
      tok) command python3 -I -S -B "$root/tools/pydump/tokdump.py" --base "$base" --list "$i" --out "$out" --tsv "$i.tsv" & ;;
    esac
  done
  wait
  if [ -n "$tsv" ]; then cat "$d"/part.*.tsv 2>"$TMPDIR/pyrec.err" | LC_ALL=C sort > "$tsv"; fi
  rm -rf "$d"
}

pyrec_ext() { case $1 in scopes) echo .scopes.ae ;; tok) echo .tok ;; *) echo .ae ;; esac; }
# the row without its run's figures: tree's SECONDS and RSS, tok's SECONDS, as 0
pyrec_norm_rows() { case $1 in tok) awk -F'\t' -v OFS='\t' '{$NF = 0; print}' ;; *) awk -F'\t' -v OFS='\t' '{$(NF-1) = 0; $NF = 0; print}' ;; esac; }

# pyrec_keys KIND BASE LIST: "REL<TAB>KEY" for each file of LIST
pyrec_keys() {
  local h; h=$(pyrec_sha16 "$(pyrec_helper "$1")")
  (cd "$2" && tr '\n' '\0' < "$3" | xargs -0 -r sha256sum) | awk -v h="$h" '{ s = substr($0, 1, 16); r = substr($0, 67); sub(/^\*/, "", r); print r "\t" s ":" h }'
}

# pyrec_dump KIND CORPUS BASE LIST OUT [TSV]: the helper's KIND (tree, full, scopes, tok) of LIST's files under BASE into
# OUT (OUT/REL.ae, .scopes.ae or .tok) and TSV, live or from the records as the mode says; exit 1 with the reasons
# when a record is missing or stale
pyrec_dump() {
  local kind=$1 corpus=$2 base=$3 list=$4 out=$5 tsv=${6:-}
  if pyrec_live; then
    pyrec_run_helper "$kind" "$base" "$list" "$out" "$tsv"
    case $PYREC_MODE in
      record) pyrec_stage "$kind" "$corpus" "$base" "$list" "$out" "$tsv" ;;
      verify) pyrec_verify_dump "$kind" "$corpus" "$base" "$list" "$out" "$tsv" ;;
    esac
    return 0
  fi
  pyrec_read "$@"
}

# pyrec_read KIND CORPUS BASE LIST OUT [TSV]: the records' dumps (records mode)
pyrec_read() {
  local kind=$1 corpus=$2 base=$3 list=$4 out=$5 tsv=${6:-} ext; ext=$(pyrec_ext "$kind")
  local pin man w=$TMPDIR/pyrec.$$.$RANDOM src
  pin=$(pyrec_pin "$corpus"); man=$(oracle_manifest "$pin")
  [ -f "$man" ] || { echo "UNRECORDED $corpus: no $man (REPR_PYTHON_ORACLE=record, on the host with python3: doc/ORACLE-RECORDS-SURVEY.md, \"As built: Python\")" >&2; return 1; }
  rm -rf "$w"; mkdir -p "$w"
  # the wanted members with their keys, against the manifest
  pyrec_keys "$kind" "$base" "$list" | awk -F'\t' -v k="$kind" -v e="$ext" -v t="${tsv:+1}" '
    { print k "/" $1 e "\t" $2; if (t) print k "/" $1 ".row\t" $2 }' > "$w/want"
  awk -F'\t' 'NR == FNR { if (!/^#/) { key[$1] = $2; sha[$1] = $3 }; next }
              !($1 in key) { print "UNRECORDED " $1 > "/dev/stderr"; bad = 1; next }
              key[$1] != $2 { print "STALE " $1 ": its input or helper changed since the record" > "/dev/stderr"; bad = 1; next }
              { print sha[$1] "  " $1 }
              END { exit bad }' "$man" "$w/want" > "$w/sums" 2> "$w/why" || {
    head -5 "$w/why" >&2; echo "pyrec: $corpus: $(wc -l < "$w/why") entries of $kind missing or stale in $man (REPR_PYTHON_ORACLE=live runs python3; =record records them)" >&2
    rm -rf "$w"; return 1; }
  if pyrec_essential "$corpus" && [ -d "$PYREC_DIR/essential/$corpus" ]; then
    src=$PYREC_DIR/essential/$corpus
    (cd "$src" && sha256sum -c --quiet --strict "$w/sums") > "$w/why" 2>&1 || { head -3 "$w/why" >&2; echo "REFUSED $corpus: $src differs from $man" >&2; rm -rf "$w"; return 1; }
  else
    src=$(oracle_unpack "$pin") || { rm -rf "$w"; return 1; }; src=$src/python-pydump-$corpus
  fi
  mkdir -p "$out"
  awk '{ print substr($0, 67) }' "$w/sums" | grep -v '\.row$' | tr '\n' '\0' > "$w/names"
  (cd "$src" && tar --null -T "$w/names" -cf - --transform "s|^$kind/||") | tar -C "$out" -xf -
  if [ -n "$tsv" ]; then
    awk '{ print substr($0, 67) }' "$w/sums" | grep '\.row$' | tr '\n' '\0' | (cd "$src" && xargs -0 -r cat) | LC_ALL=C sort > "$tsv"
  fi
  rm -rf "$w"
}

# pyrec_verify_dump KIND CORPUS BASE LIST OUT TSV: the live dumps in OUT (TSV) against the records, file by file
pyrec_verify_dump() {
  local kind=$1 corpus=$2 base=$3 list=$4 out=$5 tsv=$6 v=$PYREC_VSTATE ext rel n=0 nd=0; ext=$(pyrec_ext "$kind")
  mkdir -p "$v/rec"; local r=$v/rec/$kind-$corpus.$RANDOM
  if ! pyrec_read "$kind" "$corpus" "$base" "$list" "$r" ${tsv:+"$r.tsv"} 2> "$v/err"; then
    echo "verify: $corpus $kind: $(tail -1 "$v/err")"; echo "$corpus $kind $(wc -l < "$list") $(wc -l < "$list")" >> "$v/tally"; return 0
  fi
  [ -z "$tsv" ] || pyrec_norm_rows "$kind" < "$tsv" > "$r.live.tsv"
  while IFS= read -r rel; do
    n=$((n + 1))
    if ! cmp -s "$out/$rel$ext" "$r/$rel$ext" || { [ -n "$tsv" ] && [ "$(grep -F -- "$rel"$'\t' "$r.live.tsv" | head -1)" != "$(grep -F -- "$rel"$'\t' "$r.tsv" | head -1)" ]; }; then
      nd=$((nd + 1)); echo "verify: $corpus $kind/$rel: DIFFERS"
    fi
  done < "$list"
  echo "$corpus $kind $n $nd" >> "$v/tally"
  # the crossed pair's candidates: this corpus's first file's live dump and its last file's record
  if [ "$n" -ge 2 ] && [ ! -f "$v/pair" ]; then
    printf '%s\t%s\t%s\n' "$out/$(head -1 "$list")$ext" "$r/$(tail -1 "$list")$ext" "$corpus $kind: $(head -1 "$list") against $(tail -1 "$list")'s record" > "$v/pair"
  fi
}

# record mode: what the verb dumped, staged per corpus (PYREC_STAGE), packed by pyrec_flush at its end
# (bin/repr-python records record runs several verbs into one stage, PYREC_DEFER_FLUSH set, and packs once at its end)
PYREC_STAGE=${PYREC_STAGE:-$root/.tmp/repr-python/records-stage.$$}
pyrec_stage() {
  local kind=$1 corpus=$2 base=$3 list=$4 out=$5 tsv=$6 ext s=$PYREC_STAGE/$1-$2.$RANDOM; ext=$(pyrec_ext "$kind")
  mkdir -p "$s/m/$kind"
  echo "$corpus" > "$s/corpus"
  pyrec_keys "$kind" "$base" "$list" > "$s/keys"
  (cd "$out" && sed "s|\$|$ext|" "$list" | tr '\n' '\0' | tar --null -T - -cf -) | tar -C "$s/m/$kind" -xf -
  awk -F'\t' -v k="$kind" -v e="$ext" '{ print k "/" $1 e "\t" $2 }' "$s/keys" > "$s/ids"
  if [ -n "$tsv" ]; then
    # each row beside its dump (the dump's directory exists)
    pyrec_norm_rows "$kind" < "$tsv" | awk -F'\t' -v d="$s/m/$kind" '{ f = d "/" $1 ".row"; print > f; close(f) }'
    awk -F'\t' -v k="$kind" '{ print k "/" $1 ".row\t" $2 }' "$s/keys" >> "$s/ids"
  fi
}
# pyrec_stage_src CORPUS BASE LIST KEY: a generated corpus's inputs (src/REL, src.list) staged beside its dumps
pyrec_stage_src() {
  [ "$PYREC_MODE" = record ] || return 0
  local corpus=$1 base=$2 list=$3 key=$4 s=$PYREC_STAGE/src-$1.$RANDOM
  mkdir -p "$s/m/src"; echo "$corpus" > "$s/corpus"
  (cd "$base" && tr '\n' '\0' < "$list" | tar --null -T - -cf -) | tar -C "$s/m/src" -xf -
  cp "$list" "$s/m/src.list"
  { awk -v k="$key" '{ print "src/" $0 "\t" k }' "$list"; printf 'src.list\t%s\n' "$key"; } > "$s/ids"
}
# pyrec_src CORPUS DIR: a generated corpus's recorded inputs into DIR, its list on stdout (records mode); KEY checked
pyrec_src() {
  local corpus=$1 dir=$2 key=$3 pin man src w=$TMPDIR/pyrec-src.$$
  pin=$(pyrec_pin "$corpus"); man=$(oracle_manifest "$pin")
  [ -f "$man" ] || { echo "UNRECORDED $corpus: no $man" >&2; return 1; }
  [ "$(awk -F'\t' '$1 == "src.list" {print $2}' "$man")" = "$key" ] || { echo "STALE $corpus: its generator or the tarball changed since the record (or UNRECORDED)" >&2; return 1; }
  if pyrec_essential "$corpus" && [ -d "$PYREC_DIR/essential/$corpus" ]; then src=$PYREC_DIR/essential/$corpus
  else src=$(oracle_unpack "$pin") || return 1; src=$src/python-pydump-$corpus; fi
  awk -F'\t' '!/^#/ && ($1 ~ /^src\// || $1 == "src.list") { print $3 "  " $1 }' "$man" > "$w"
  (cd "$src" && sha256sum -c --quiet --strict "$w") >&2 || { echo "REFUSED $corpus: its inputs differ from $man" >&2; rm -f "$w"; return 1; }
  rm -f "$w"; mkdir -p "$dir"; cp -r "$src/src/." "$dir/"; cat "$src/src.list"
}

pyrec_flush() {
  [ "$PYREC_MODE" != verify ] || { pyrec_verdict; return $?; }
  [ "$PYREC_MODE" = record ] && [ -d "$PYREC_STAGE" ] && [ -z "${PYREC_DEFER_FLUSH:-}" ] || return 0
  local corpus pin man name st c s d
  for corpus in $(cat "$PYREC_STAGE"/*/corpus | LC_ALL=C sort -u); do
    pin=$(pyrec_pin "$corpus"); man=$(oracle_manifest "$pin"); name=python-pydump-$corpus; st=$PYREC_STAGE/pack-$corpus
    rm -rf "$st"; mkdir -p "$st/$name"; : > "$st/ids"
    # the members recorded before, kept but those this run replaced
    if [ -f "$man" ] && [ -f "$pin" ] && d=$(oracle_unpack "$pin" 2>"$TMPDIR/pyrec.err"); then
      cp -a "$d/$name/." "$st/$name/"; awk -F'\t' '!/^#/ && NF {print $1 "\t" $2}' "$man" > "$st/ids"
    fi
    for s in "$PYREC_STAGE"/*; do
      [ -f "$s/corpus" ] && [ "$(cat "$s/corpus")" = "$corpus" ] || continue
      cp -a "$s/m/." "$st/$name/"
      awk -F'\t' 'NR == FNR { new[$1] = 1; next } !($1 in new)' "$s/ids" "$st/ids" > "$st/ids.new"; cat "$s/ids" >> "$st/ids.new"; mv "$st/ids.new" "$st/ids"
    done
    oracle_make "$pin" "$st" "$st/ids" "REPR_PYTHON_ORACLE=record ${PYREC_VERB:-bin/repr-python}" python || return 1
    if pyrec_essential "$corpus"; then
      rm -rf "$PYREC_DIR/essential/$corpus"; mkdir -p "$PYREC_DIR/essential/$corpus"; cp -a "$st/$name/." "$PYREC_DIR/essential/$corpus/"
      echo "pyrec: $corpus's members committed in test/python/records/essential/$corpus ($(du -sk "$PYREC_DIR/essential/$corpus" | cut -f1) KB)"
    fi
  done
  rm -rf "$PYREC_STAGE"
}

# pyrec_verdict: verify mode's judgment of the verb's tallies (ORS6 (a): a moved python3 reported, not failed), then the
# crossed pair
pyrec_verdict() {
  local v=$PYREC_VSTATE total nd man rc=0
  [ -f "$v/tally" ] || { rm -rf "$v"; return 0; }
  total=$(awk '{ s += $3 } END { print s + 0 }' "$v/tally"); nd=$(awk '{ s += $4 } END { print s + 0 }' "$v/tally")
  awk '{ printf "verify: %s %s: %d entries, %d differ\n", $1, $2, $3, $4 }' "$v/tally"
  # the identity the records name: the first corpus's manifest, else the first outcome's meta
  local c1; c1=$(awk '$1 != "outcomes" { print $1; exit }' "$v/tally")
  if [ -n "$c1" ] && [ -f "$PYREC_DIR/python-pydump-$c1.tsv" ]; then man=$PYREC_DIR/python-pydump-$c1.tsv
  else man=$PYREC_DIR/outcomes/$(awk '$1 == "outcomes" { print $2; exit }' "$v/tally").meta; fi
  oracle_verdict python "$man" "$nd" "$total" || rc=1
  if [ -f "$v/pair" ]; then
    IFS=$'\t' read -r a b what < "$v/pair"
    if cmp -s "$a" "$b"; then echo "verify: FAILED: the crossed pair ($what) is equal: control not judged"; rc=1
    else echo "verify: the crossed pair differs ($what; control passed)"; fi
  fi
  rm -rf "$v"; return $rc
}

# ---- class A: outcomes ----
# pyrec_outcome NAME KEY CMD...: CMD's stdout and status (stderr passed through), live or recorded as the mode says; the
# key names CMD's inputs (the caller's digest); $root in the output is @ROOT in the record
pyrec_outcome() {
  local name=$1 key=$2 o=$PYREC_DIR/outcomes/$1 rc=0; shift 2
  if pyrec_live; then
    type -P python3 >/dev/null || die "no python3 (the oracle): $name runs it"
    "$@" > "$TMPDIR/pyrec-out.$$" || rc=$?
    if [ "$PYREC_MODE" = verify ]; then
      mkdir -p "$PYREC_VSTATE"; local same=0
      if [ -f "$o.meta" ] && [ "$(sed -n 's/^key //p' "$o.meta")" = "$key" ] && [ "$(sed -n 's/^status //p' "$o.meta")" = "$rc" ] \
         && sed "s|$root|@ROOT|g" "$TMPDIR/pyrec-out.$$" | cmp -s - "$o.out"; then same=1; fi
      [ $same = 1 ] || echo "verify: outcome $name: DIFFERS (or unrecorded, or stale)" >&2
      echo "outcomes $name 1 $((1 - same))" >> "$PYREC_VSTATE/tally"
      if [ ! -f "$PYREC_VSTATE/opair" ]; then echo "$o" > "$PYREC_VSTATE/opair"
      elif [ ! -f "$PYREC_VSTATE/pair" ] && [ -f "$(cat "$PYREC_VSTATE/opair").out" ]; then
        sed "s|$root|@ROOT|g" "$TMPDIR/pyrec-out.$$" > "$PYREC_VSTATE/olive"
        printf '%s\t%s\t%s\n' "$PYREC_VSTATE/olive" "$(cat "$PYREC_VSTATE/opair").out" "outcome $name against $(basename "$(cat "$PYREC_VSTATE/opair")")'s record" > "$PYREC_VSTATE/pair"
      fi
    fi
    if [ "$PYREC_MODE" = record ]; then
      mkdir -p "$(dirname "$o")"
      sed "s|$root|@ROOT|g" "$TMPDIR/pyrec-out.$$" > "$o.out"
      { echo "key $key"; echo "status $rc"; echo "host: :python $(oracle_host_id python) ($(oracle_host_version python))"; echo "date $(date -u +%Y-%m-%d)"; } > "$o.meta"
    fi
    cat "$TMPDIR/pyrec-out.$$"; rm -f "$TMPDIR/pyrec-out.$$"; return $rc
  fi
  [ -f "$o.meta" ] || { echo "UNRECORDED outcome $name: no test/python/records/outcomes/$name.meta (REPR_PYTHON_ORACLE=record)" >&2; return 1; }
  [ "$(sed -n 's/^key //p' "$o.meta")" = "$key" ] || { echo "STALE outcome $name: its inputs changed since the record (REPR_PYTHON_ORACLE=live runs python3)" >&2; return 1; }
  sed "s|@ROOT|$root|g" "$o.out"
  return "$(sed -n 's/^status //p' "$o.meta")"
}
# pyrec_key FILE...: the digest of the files' names (relative to $root) and bytes, 16 characters
pyrec_key() { local f; for f in "$@"; do printf '%s\0' "${f#$root/}"; cat "$f"; printf '\0'; done | sha256sum | cut -c1-16; }
