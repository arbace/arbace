# LLVM IR's records (doc/ORACLE-RECORDS-SURVEY.md, ORS12, "As built: C++ and LLVM IR"): what bin/repr-llvm asks of clang, llvm-as and opt at run time,
# sourced by bin/repr-llvm after test/oracle-records/oracle.bash and cpp.bash. Out of bin/lib for oracle.bash's reason.
#
#   test/llvm/records/roundtrip-orig.tsv   (class A, committed) the round trip's originals: NAME<TAB>KEY<TAB>OBJECT<TAB>BITCODE, the SHA-256 of `clang -c` of the
#                                          module for its own triple and of llvm-as's bitcode, `none` when the original does not build there; KEY the module's
#                                          bytes, its triple and the recipe. The print's half (clang -c and llvm-as on Arbace's output, class D) is the oracle tier's.
#   llvm-opt-verify (class B, a pack)      opt -passes=verify -disable-output on each module: NAME/rc and NAME/log (opt's whole standard error), the same key
# The lists: bin/repr-llvm list hw|probes|runtime|all: NAME<TAB>PATH lines (runtime: the IR of C++'s lowered runtime, the pack cpp-runtime-ir's members).
# Records are read unless REPR_LLVM_LIVE=1 (the recording and the oracle tier). An entry not recorded is UNRECORDED, one whose key moved STALE (ORS10 (a)).

lrr_recipe=llvm-rec-1
lrr_live() { [ -n "${REPR_LLVM_LIVE:-}" ]; }
lrr_orig=$root/test/llvm/records/roundtrip-orig.tsv
lrr_opt_pin=$root/test/llvm/records/llvm-opt-verify.sha256
lrr_triple() { sed -n '/^target triple = /{s/^target triple = "\(.*\)"$/\1/p;q;}' "$1"; }
lrr_key() { # FILE
  { printf '%s\n%s\n' "$lrr_recipe" "$(lrr_triple "$1")"; sha256sum < "$1" | cut -c1-64; } | sha256sum | cut -c1-64
}
lrr_orig_digests() { # FILE: "OBJECT<TAB>BITCODE" by the toolchain
  local f=$1 tr o b t=$TMPDIR/lrr.$$
  tr=$(lrr_triple "$f")
  if clang -c -mno-incremental-linker-compatible ${tr:+-target "$tr"} "$f" -o "$t.o" 2>"$TMPDIR/lrr.err"; then o=$(sha256sum < "$t.o" | cut -c1-64); else o=none; fi
  if "${REPR_LLVM_AS:-/usr/lib/llvm23/bin/llvm-as}" "$f" -o "$t.bc" 2>"$TMPDIR/lrr.err"; then b=$(sha256sum < "$t.bc" | cut -c1-64); else b=none; fi
  rm -f "$t.o" "$t.bc"
  printf '%s\t%s\n' "$o" "$b"
}
lrr_orig_get() { # NAME FILE: the recorded "OBJECT<TAB>BITCODE"; exit 3 UNRECORDED, 4 STALE
  local line want
  [ -f "$lrr_orig" ] || { echo "UNRECORDED: no $lrr_orig" >&2; return 3; }
  line=$(awk -F'\t' -v n="$1" '$1 == n {print; exit}' "$lrr_orig")
  [ -n "$line" ] || { echo "UNRECORDED: $1 is not in $lrr_orig (bin/repr-llvm record-orig, by hand on a host with clang and llvm-as)" >&2; return 3; }
  want=$(printf '%s' "$line" | cut -f2)
  [ "$want" = "$(lrr_key "$2")" ] || { echo "STALE: $1's key moved (the module or the recipe changed): record again by hand" >&2; return 4; }
  printf '%s\n' "$line" | cut -f3,4
}
lrr_opt_get() { # NAME FILE DIR: NAME/rc and NAME/log of the pack into DIR/rc, DIR/log; exit 3, 4, as above
  local want f key
  want=$(awk -F'\t' -v i="$1/rc" '!/^#/ && $1 == i {print $2; exit}' "$(oracle_manifest "$lrr_opt_pin")" 2>"$TMPDIR/lrr.err")
  [ -n "$want" ] || { echo "UNRECORDED: $1 is not in the pack llvm-opt-verify (bin/repr-llvm record-opt, by hand on a host with opt)" >&2; return 3; }
  [ "$want" = "$(lrr_key "$2")" ] || { echo "STALE: $1's key moved: record again by hand" >&2; return 4; }
  for f in rc log; do cp "$(oracle_get "$lrr_opt_pin" "$1/$f")" "$3/$f" || return $?; done
  lrr_unnorm "$3/log"
}
lrr_norm() { sed "s|$root/|@ROOT@/|g; s|$(oracle_store)/unpacked/|@STORE@/|g; s|<0x[0-9a-f]\\{6,\\}>|<0xADDR>|g" "$1" > "$1.n" && mv "$1.n" "$1"; }  # opt's messages name the module's path as given
lrr_unnorm() { sed "s|@ROOT@/|$root/|g; s|@STORE@/|$(oracle_store)/unpacked/|g" "$1" > "$1.n" && mv "$1.n" "$1"; }
lrr_list() { # hw|probes|runtime|all
  case $1 in
    hw) cat "$root/test/llvm/hw.list" ;;
    probes) cat "$root/test/llvm/probes.list" ;;
    runtime) local pin=$root/test/cpp/records/cpp-runtime-ir.sha256 u d
      d=$(oracle_unpack "$pin") || return $?
      awk -F'\t' '!/^#/ && $1 ~ /\/x\.ll$/ {print $1}' "$(oracle_manifest "$pin")" | while read -r u; do printf 'rt/%s\t%s/%s/%s\n' "${u%/x.ll}" "$d" "$(oracle_pin_name "$pin")" "$u"; done ;;
    all) lrr_list hw; lrr_list probes; lrr_list runtime ;;
    valid) lrr_list hw; lrr_list runtime ;;  # the modules every judge takes: hand-written ones and clang's IR
    *) echo "list: hw, probes, runtime, valid or all" >&2; return 2 ;;
  esac | sed "s|\t\(test/\)|\t$root/\1|"
}
lrr_listfile() { # LIST: a file, or hw|probes|runtime|all made into one
  if [ -f "$1" ]; then echo "$1"; return; fi
  lrr_list "$1" > "$TMPDIR/lrr-list.$1" && echo "$TMPDIR/lrr-list.$1"
}
