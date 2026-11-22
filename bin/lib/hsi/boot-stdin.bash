# shellcheck shell=bash
# HES102 (hs-speed, 2026-11-22): a nofib program's input its boot.sh makes (nofib's INPUT_FILE, made by `make boot`: k-nucleotide's
# k-nucleotide.faststdin in the fast mode record-ref runs, fasta-c 60000), test/haskell/eval/boot.edn's :stdin entries: the plan's
# stdin (bin/lib/hsi/check, record-ref), made by Arbace (bin/hsi boot: the shipped generator's C on ci, its digest the kept one) and
# copied into the program's run directory. Sourced with root set.

# boot_stdin_name ID: the name (:file) of the program's made input, or nothing (status 0 either way: callers run under set -e)
boot_stdin_name() {
  { grep -F " \"$1\" {" "$root/test/haskell/eval/boot.edn" | grep -F ' :stdin "' | sed -n 's/.* :file "\([^"]*\)".*/\1/p'; } || true
}

# boot_stdin_put ID DIR: the made input copied into DIR under its name (bin/hsi boot makes it when the kept file is not the
# entry's digest; one maker at a time); status 1 when it cannot be made
boot_stdin_put() {
  local id=$1 dir=$2 bl f sha m
  bl=$(grep -F " \"$id\" {" "$root/test/haskell/eval/boot.edn" | grep -F ' :stdin "') || return 1
  f=$(sed -n 's/.* :file "\([^"]*\)".*/\1/p' <<< "$bl"); sha=$(sed -n 's/.* :stdin "\([0-9a-f]*\)".*/\1/p' <<< "$bl")
  m=$root/.tmp/hsi/boot/$id/out.stdout
  mkdir -p "$root/.tmp/hsi"
  (
    flock 9
    if [ ! -f "$m" ] || [ "$(sha256sum < "$m" | cut -c1-64)" != "$sha" ]; then
      "$root/bin/hsi" boot "$id" > "$root/.tmp/hsi/boot-${id//\//_}.log" 2>&1 || exit 1
    fi
  ) 9> "$root/.tmp/hsi/boot.lock" || return 1
  cp "$m" "$dir/$f"
}
