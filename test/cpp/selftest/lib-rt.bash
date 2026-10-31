# Sourced after lib.bash by the round trip's judge tests: rt_file WORK NAME makes NAME pass levels read to 3 and 5 in WORK
# (its dumps, forms and normalized dumps equal); rt_obj WORK NAME WHO ORIG-STATUS PRINT-STATUS [differ] its level-4 objects.
rt_file() {
  local w=$1 n=$2 d
  for d in orig dump2 forms forms2; do mkdir -p "$w/$d/$(dirname "$n")"; done
  echo 0 > "$w/orig/$n.status"; echo 0 > "$w/dump2/$n.status"
  echo "(norm)" > "$w/orig/$n.norm"; echo "(norm)" > "$w/dump2/$n.norm"
  echo "(forms)" > "$w/forms/$n.ae"; echo "(forms)" > "$w/forms2/$n.ae"
  printf '%s\tok\t0\t{}\tsame\n' "$n" >> "$w/read.tsv"; printf '%s\tok\t0\t{}\t-\n' "$n" >> "$w/read2.tsv"
  printf '%s\tc++23\t\tP\t\n' "$n" >> "$w/list.tsv"
}
rt_obj() {
  local w=$1 n=$2 who=$3 side
  for side in orig print; do mkdir -p "$w/obj/$side/$who/$(dirname "$n")"; done
  echo "$4" > "$w/obj/orig/$who/$n.status"; echo "$5" > "$w/obj/print/$who/$n.status"
  echo obj > "$w/obj/orig/$who/$n.o"; echo "obj${6:+ $6}" > "$w/obj/print/$who/$n.o"
}
