#!/bin/bash
# Regenerates the tables of doc/FULL-SUPPORT-SURVEY.md's section "REAP v2's divergences" from the
# classes files bin/reap judges (test/reap/cells.edn), between the markers
#   <!-- v2-record:begin --> ... <!-- v2-record:end -->
# Usage: doc/v2-record/refresh.sh [DOC]      (default: doc/FULL-SUPPORT-SURVEY.md of this checkout)
# Environment: ARBACE_HOST_EXE (the tools' host for bin/reap; default stage 0), REAP_OUT (a saved
# `bin/reap` output to use instead of running it), V1_TAG (default arbace-reaps-3-crops-v1).
# Hand-kept input beside it: changes.tsv (cell, class, what changed and where it was decided; the cell
# `Java *` is any Java level). Plain shell and awk; no builds, seconds.
# Fails (nothing written) on: a missing or unreadable REAP_OUT, changes.tsv or DOC; a DOC without
# exactly one begin and one end marker, in that order; V1_TAG not a commit here; a bin/reap output
# that is not 16 cells or whose last line counts problems. Warns (stderr, work/warnings.txt) on a
# changes.tsv row no table row uses (a class renamed or gone) and on a note whose leading status
# word contradicts the computed one.
# Frozen since 2026-11-03 (the user): the section is the record of the tag arbace-reaps-3-crops-v2, its tables as at
# the tag; the matrix since then is "REAP v3's divergences", doc/v3-record/refresh.sh. V2_RECORD_FORCE=1 runs it anyway.
if [ -z "${V2_RECORD_FORCE:-}" ]; then
  echo "doc/v2-record/refresh.sh: v2's section is frozen at its tag; use doc/v3-record/refresh.sh (V2_RECORD_FORCE=1 to run it)" >&2
  exit 2
fi
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
cd "$root" || exit 1
die() { echo "refresh.sh: $*" >&2; exit 1; }
export LC_ALL=C # bytes, as busybox awk counts them: the same truncations under gawk and busybox awk
doc=${1:-$root/doc/FULL-SUPPORT-SURVEY.md}
tag=${V1_TAG:-arbace-reaps-3-crops-v1}
changes=$here/changes.tsv
[ -f "$doc" ] && [ -r "$doc" ] || die "no document $doc"
[ -f "$changes" ] && [ -r "$changes" ] || die "no $changes (the hand-kept notes)"
nb=$(grep -c '^<!-- v2-record:begin' "$doc"); ne=$(grep -c '^<!-- v2-record:end -->' "$doc")
[ "$nb" = 1 ] && [ "$ne" = 1 ] || die "$doc: $nb begin and $ne end markers (want 1 and 1)"
[ "$(grep -n '^<!-- v2-record:begin' "$doc" | cut -d: -f1)" -lt "$(grep -n '^<!-- v2-record:end -->' "$doc" | cut -d: -f1)" ] || die "$doc: the end marker before the begin marker"
w=$root/.tmp/v2-record/work; rm -rf "$w"; mkdir -p "$w" || die "cannot make $w"
git rev-parse -q --verify "$tag^{commit}" > "$w/tag.sha" 2>&1 || die "V1_TAG $tag is not a commit here (fetch the tag or set V1_TAG)"
if [ -n "${REAP_OUT:-}" ]; then
  [ -f "$REAP_OUT" ] && [ -r "$REAP_OUT" ] || die "no REAP_OUT $REAP_OUT"
  cp "$REAP_OUT" "$w/reap.out" || die "cannot copy $REAP_OUT"
  when=$(date -r "$REAP_OUT" +%F)
else
  bin/reap > "$w/reap.out" 2>&1 || die "bin/reap failed (see $w/reap.out)"
  when=$(date +%F)
fi
ncell=$(grep -cE '^(Clojure|Java|Go|C) (read|eval|AOT|print): .*; classes ' "$w/reap.out")
[ "$ncell" = 16 ] || die "$w/reap.out: $ncell cell lines, not 16 (not bin/reap's output?)"
grep -qE '^reap: [0-9]+ of [0-9]+ cells checked, [0-9]+ with no check yet; 0 problems$' "$w/reap.out" || die "$w/reap.out: its last line does not say 0 problems"
: > "$w/warnings.txt"
grep -qE '^(Clojure|Java|Go|C) (read|eval|AOT|print): no check yet' "$w/reap.out" && echo "warning: a cell has no check yet" >> "$w/warnings.txt"
grep -q 'decision pending' "$w/reap.out" && echo "warning: a class's decision is pending" >> "$w/warnings.txt"

# current classes per cell: cell TAB id TAB kind TAB cases TAB title TAB pending TAB beside (a class
# only beside sources name: counted in the table, not in the matrix, RPS16)
awk '
 /^(Clojure|Java|Go|C) (read|eval|AOT|print): /{ cell=$1" "$2; sub(/:$/,"",cell); bs=0; next }
 cell!="" && /^   [^ ]/{ bs=($0 ~ / \(beside\), /); next }
 cell!="" && /^ +[0-9]+ [^ :]+: .* \((design|deferred)(, decision pending)?\)$/ {
   n=$1; id=$2; sub(/:$/,"",id)
   line=$0; sub(/^ +[0-9]+ [^ ]+ /,"",line)
   pend=(line ~ /decision pending\)$/)?"pending":""
   kind=(line ~ /\(design(, decision pending)?\)$/)?"by design":"deferred"
   sub(/ \((design|deferred)(, decision pending)?\)$/,"",line)
   k=cell SUBSEP id; cnt[k]+=n; kd[k]=kind; ti[k]=line; pd[k]=pend; if(!bs) chk[k]=1
   if(!(k in seen)){seen[k]=1; order[++no]=k}
 }
 END{ for(i=1;i<=no;i++){k=order[i]; split(k,a,SUBSEP); print a[1] "\t" a[2] "\t" kd[k] "\t" cnt[k] "\t" ti[k] "\t" pd[k] "\t" ((k in chk)?"":"beside")} }
' "$w/reap.out" > "$w/cur.tsv"

# v1: the classes of the v1 section (cell, id, kind, cases or -)
git show "$tag:doc/FULL-SUPPORT-SURVEY.md" > "$w/v1-doc.md" || die "git show $tag:doc/FULL-SUPPORT-SURVEY.md failed"
awk '
 /^## REAP v1.s divergences/{s=1; next} s && /^## /{exit}
 !s{next}
 /^- \*\*(Clojure|Java|Go) (read|eval|AOT|print)\*\*:/{
   cell=$2" "$3; gsub(/\*|:/,"",cell)
   d=$0; sub(/^[^:]*\*\*: by design [0-9]+ cases in [0-9]+ classes \(/,"",d); sub(/\); deferred.*$/,"",d)
   m=split(d,names,/`, `/); for(i=1;i<=m;i++){x=names[i]; gsub(/`/,"",x); if(x!="") print cell "\t" x "\tby design\t-"}
   next }
 cell!="" && /^  - `[^`]+` \([0-9]+\):/{ id=$2; gsub(/`/,"",id); c=$3; gsub(/[():]/,"",c); t=$0; sub(/^  - `[^`]+` \([0-9]+\): /,"",t); print cell "\t" id "\tdeferred\t" c "\t" t }
' "$w/v1-doc.md" > "$w/v1.tsv"
[ -s "$w/v1.tsv" ] || die "no class read from $tag's section \"REAP v1's divergences\""
# v1's matrix, the first table of that section, relabelled
awk '/^## REAP v1.s divergences/{s=1; next} s && /^## /{exit} s && /^\| \| read \|/{t=1; sub(/^\| \|/,"| v1 (the tag) |")} t && !/^\|/{exit} t' "$w/v1-doc.md" > "$w/v1-matrix.md"
[ "$(wc -l < "$w/v1-matrix.md")" = 5 ] || die "v1's matrix not found in $tag's section"

# each cell'"'"'s classes file and the decisions
# (a cell of two classes files, C AOT's, prints them as a vector: ["A" "B"], kept here as "A B")
awk '/^(Clojure|Java|Go|C) (read|eval|AOT|print): /{ c=$1" "$2; sub(/:$/,"",c); f=$0; sub(/^.*; classes /,"",f); gsub(/[]["]/,"",f); print c "\t" f }' "$w/reap.out" > "$w/cellfile.tsv"
: > "$w/dec.tsv"
# the cells' files, then the sources' own (:classes in test/reap/cells.edn: diffcheck's, the oracle's)
srcfiles=$(sed -n 's/.*:classes "\([^"]*\)".*/\1/p' test/reap/cells.edn | sort -u)
for f in $( { cut -f2 "$w/cellfile.tsv"; echo "$srcfiles"; } | sort -u); do
  [ -f "$f" ] || die "$f (a classes file bin/reap names) is missing"
  awk -v F="$f" '
   /^ *\{:id /{ id=$2; sub(/^:/,"",id); sub(/\}.*/,"",id) }
   /^ *:decided "/{ t=$0; sub(/^ *:decided "/,"",t); sub(/"[ ]*$/,"",t); gsub(/\\"/,"\"",t); print F "\t" id "\t" t }
  ' "$f" >> "$w/dec.tsv"
done

# the matrix as printed now
sed -n '/^| |/,/^$/p' "$w/reap.out" | sed '/^$/d' > "$w/matrix.md"
problems=$(sed -n "s/^reap: \(.*\)$/\1/p" "$w/reap.out" | tail -1)

awk -F'\t' -v W="$w" -v CHF="$changes" -v WF="$w/warnings.txt" '
 function short(s,n,  t){ if(length(s)<=n) return s; t=substr(s,1,n-3); if(substr(s,n-2,1) ~ /^[\200-\277]$/){ sub(/[\200-\277]*$/,"",t); sub(/[\300-\377]$/,"",t) } return t "..." } # never half a UTF-8 character
 function dec(fs,id,  k,t,ff,nf,i){ nf=split(fs,ff," "); t=""; for(i=1;i<=nf && t=="";i++){ k=ff[i] SUBSEP id; if(k in D) t=D[k] } if(t=="" && (id in DX)) t=DX[id]; if(t=="") return "-"; sub(/ \(doc\/.*$/,"",t); sub(/, as .*$/,"",t); return short(t,110) }
 function word(s){ sub(/[:,; (].*$/,"",s); return s }
 BEGIN{
  while((getline l < (W "/cellfile.tsv"))>0){ split(l,a,"\t"); CF[a[1]]=a[2]; cells[++nc]=a[1] }
  while((getline l < (W "/cur.tsv"))>0){ split(l,a,"\t"); k=a[1] SUBSEP a[2]; CK[k]=a[3]; CN[k]=a[4]; CT[k]=a[5]; CP[k]=a[6]; CB[k]=a[7]; if(!(k in inorder)){inorder[k]=1; ids[a[1]]=ids[a[1]] "\n" a[2]} }
  while((getline l < (W "/v1.tsv"))>0){ split(l,a,"\t"); k=a[1] SUBSEP a[2]; VK[k]=a[3]; VN[k]=a[4]; VT[k]=a[5]; if(!(k in inorder) ){ if(!(k in vin)){ids[a[1]]=ids[a[1]] "\n" a[2]} } vin[k]=1 }
  for(c in CF){ nf=split(CF[c],ff," "); for(i=1;i<=nf;i++) ISCF[ff[i]]=1 }
  while((getline l < (W "/dec.tsv"))>0){ split(l,a,"\t"); if(!((a[1] SUBSEP a[2]) in D)) D[a[1] SUBSEP a[2]]=a[3]; if(!(a[1] in ISCF) && !(a[2] in DX)) DX[a[2]]=a[3] }
  nch=0
  while((getline l < CHF)>0){ nch++; if(l ~ /^[ \t]*$/) continue; if(split(l,a,"\t")!=3){ printf "warning: changes.tsv line %d: not 3 tab-separated fields\n", nch >> WF; continue }
   if((a[1] SUBSEP a[2]) in CH) printf "warning: changes.tsv line %d: %s %s again (the later row wins)\n", nch, a[1], a[2] >> WF
   CH[a[1] SUBSEP a[2]]=a[3]; CHL[a[1] SUBSEP a[2]]=nch }
  nl=split("Clojure read,Clojure eval,Clojure AOT,Clojure print,Java read,Java eval,Java AOT,Java print,Go read,Go eval,Go AOT,Go print,C read,C eval,C AOT,C print",L,",")
  for(ci=1;ci<=nl;ci++){
   c=L[ci]; split(c,p," "); lang=p[1]
   cfs=CF[c]; nf=gsub(/ /,"`, `",cfs)
   printf "\n**%s** (classes file%s `%s`)\n\n", c, (nf>0?"s":""), cfs
   n=split(ids[c],arr,"\n"); m=0; for(i=1;i<=n;i++) if(arr[i]!="") u[++m]=arr[i]
   if(m==0){ print "No class: nothing to record at this cell.\n"; continue }
   # sort: by design first then deferred, by cases desc (simple insertion sort)
   for(i=1;i<=m;i++){ k=c SUBSEP u[i]; key[i]=((k in CK)?(CK[k]=="by design"?0:1):(VK[k]=="by design"?0:1)) * 1000000 - ((k in CN)?CN[k]:0) }
   for(i=2;i<=m;i++){ x=u[i]; kx=key[i]; j=i-1; while(j>=1 && key[j]>kx){u[j+1]=u[j]; key[j+1]=key[j]; j--} u[j+1]=x; key[j+1]=kx }
   print "| class | kind | cases | v1 | since v1 | decision |"
   print "|---|---|---:|---:|---|---|"
   for(i=1;i<=m;i++){
    id=u[i]; k=c SUBSEP id; w2="Java *" ; ck=c SUBSEP id; wk=(lang=="Java")?("Java *" SUBSEP id):ck
    now=(k in CK); was=(k in vin)
    if(now && was){ st=(CK[k]!=VK[k])?"reclassified":((VN[k]!="-" && CN[k]<VN[k]+0)?"shrunk":((VN[k]!="-" && CN[k]>VN[k]+0)?"grew":"unchanged")) }
    else if(now){ st="new" } else { st="removed" }
    note=""; wild=0; if(ck in CH){ note=CH[ck]; USED[ck]=1 } else if(wk in CH){ note=CH[wk]; USED[wk]=1; wild=1 }
    nw=word(note)
    if(note=="" && (st=="grew" || st=="shrunk")){ d=CN[k]-VN[k]; note=st " by " (d<0?-d:d) " (UNREVIEWED: the levels were re-recorded; check the check files)" }
    else if(note=="") note=st
    else if(nw ~ /^(removed|new|shrunk|grew|unchanged)$/ && nw!=st){
     # a cell'"'"'s own note is kept as written (it may know what the counts cannot: a v1 class named
     # only, a test counted under another class); a `Java *` note is not this level'"'"'s: flagged
     printf "warning: %s %s: computed %s, changes.tsv%s says \"%s\"\n", c, id, st, (wild?" (Java *)":""), short(note,70) >> WF
     if(wild) note=st " (CHECK, changes.tsv for every Java level: " note ")" }
    else if(note !~ /^(removed|new|shrunk|grew|unchanged|reclassified|retitled|in neither)/) note=st ": " note
    kind=now?CK[k]:"(" VK[k] " in v1)"
    if(now && CB[k]=="beside") kind=kind " (beside)"
    cases=now?CN[k]:0
    v1=was?VN[k]:"-"
    title=now?CT[k]:((k in VT && VT[k]!="")?VT[k]:id)
    p2=(now && CP[k]=="pending")?" (decision pending)":""
    printf "| `%s`: %s | %s%s | %s | %s | %s | %s |\n", id, short(title,80), kind, p2, cases, v1, note, now?dec(CF[c],id):"(removed)"
   }
   print ""
   delete u; delete key
  }
  for(x in CH) if(!(x in USED)){ split(x,a,SUBSEP); printf "warning: changes.tsv line %d: %s %s names no row of the tables (a class renamed, gone or beside the matrix?)\n", CHL[x], a[1], a[2] >> WF }
 }' > "$w/tables.md" || die "the tables' awk failed"
cat "$w/warnings.txt" >&2

{
 echo "<!-- v2-record:begin (generated by doc/v2-record/refresh.sh from bin/reap's output of $when; do not edit between the markers) -->"
 echo
 echo "Now (\`bin/reap\`; pass / by design / deferred), with v1's beside it:"
 echo
 cat "$w/matrix.md"
 echo
 cat "$w/v1-matrix.md"
 echo
 echo "Its last line: \`reap: ${problems}\`."
 cat "$w/tables.md"
 echo "<!-- v2-record:end -->"
} > "$w/block.md" || die "cannot write $w/block.md"

awk -v B="$w/block.md" '
 /^<!-- v2-record:begin/{ while((getline l < B)>0) print l; skip=1; next }
 /^<!-- v2-record:end -->/{ skip=0; next }
 !skip{ print }' "$doc" > "$w/doc.new" || die "rewriting $doc failed"
cp "$w/doc.new" "$doc" || die "cannot write $doc"
echo "refreshed $doc ($(wc -l < "$w/block.md") lines between the markers)"
