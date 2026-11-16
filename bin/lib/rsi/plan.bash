# A program of a run-program plan (bin/lib/repr-rust/run-program's PLAN: id, file, edition, compile flags, run flags, the run's
# environment, the compiler's environment, 0x1f-separated, items space-separated with \x20 for a space), parsed as run-program
# parses it, for rsi's ci leg (bin/lib/rsi/program): plan_line PLAN N sets id file ed and the arrays fl (rustc's flags), rf (the
# run's arguments, --test-threads=1 added for a test harness), xe (the run's environment), re (the compiler's).
plan_words() { local w out=(); for w in $1; do out+=("$(printf '%b' "$w")"); done; printf '%s\0' "${out[@]}"; }
plan_into() { local wf; wf=$(mktemp); plan_words "$2" > "$wf"; mapfile -d '' "$1" < "$wf"; rm -f "$wf"; }
plan_line() {
  local flags rflags xenv renv
  IFS=$'\x1f' read -r id file ed flags rflags xenv renv <<< "$(sed -n "${2}p" "$1")"
  fl=(); rf=(); xe=(); re=()
  [ -n "$flags" ] && plan_into fl "$flags"
  [ -n "$rflags" ] && plan_into rf "$rflags"
  case " $flags " in *" --test "*) case " $rflags " in *--test-threads*) ;; *) rf+=(--test-threads=1) ;; esac ;; esac
  [ -n "$xenv" ] && plan_into xe "$xenv"
  [ -n "$renv" ] && plan_into re "$renv"
  return 0
}
# the id as a directory name (/ and # written %)
plan_key() { printf '%s' "$1" | tr '/#' '%%'; }
# rustc's command line of a side, as run-program's build (the source root SRC, the sysroot SYS, a WRAP): in SRC
plan_rustc() { # plan_rustc SRC SYS OUT [WRAP]
  (cd "$1" && env RUSTC_BOOTSTRAP=1 "${re[@]}" timeout 300 ${4:+"$4"} rustc ${2:+--sysroot "$2"} --edition "$ed" "$file" -o "$3" \
     -A unused -A internal_features -Cdebuginfo=0 --remap-path-prefix "$(cd "$1" && pwd -P)=/src" ${REPR_RUST_NATIVE:+-L "native=$REPR_RUST_NATIVE"} "${fl[@]}")
}
# a run's outputs normalized as run-program's (thread ids, a harness's time)
plan_norm() { sed -i -E -e "s/^(thread '[^']*' )\([0-9]+\)/\1(\$TID)/" -e 's/(; finished in )[0-9.]+s$/\1$TIME/' "$@"; }
