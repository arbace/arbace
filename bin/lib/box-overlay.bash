# The box's guests' file system (doc/go/BOX-NOTES.md, S7, BX95): TamaGo's syscall/fs_tamago.go as
# overlay/go/syscall.ae patches it (a seek past the end shrank the file; a write past the end and
# O_TRUNC set wrong sizes; MkLazy for the tool guests), for every GOOS=tamago build: Arbace's
# guest (bin/arbace-go --build --box, bin/tamago-vendor check) and the tool guests (bin/box-tools).
# Sourced, with $root set to the repository root. Defines:
#
#   box_fs_file           prints the patched fs_tamago.go's path, printed from its forms into
#                         target/box-fs/ when missing or older than them (bin/g2a-print)
#   box_overlay IN GOROOT OUT
#                         writes the overlay file OUT: the overlay file IN (bin/jrt overlay's, or
#                         one for another GOROOT) and the patched file replacing
#                         GOROOT/src/syscall/fs_tamago.go
box_fs_file() {
  local d=$root/target/box-fs f
  f=$d/syscall/fs_tamago.go
  if [ ! -f "$f" ] || [ -n "$(find "$root/overlay/go/syscall.ae" "$root/overlay/go/syscall" -newer "$f")" ]; then
    rm -rf "$d"; mkdir -p "$d"
    (cd "$root" && bin/g2a-print --layout gofmt overlay/go/syscall.ae "$d/syscall") > "$d/print.log" 2>&1 \
      || { tail -20 "$d/print.log" >&2; return 1; }
  fi
  echo "$f"
}
box_overlay() {
  local f
  f=$(box_fs_file) || return 1
  { printf '{"Replace": {\n "%s": "%s",' "$2/src/syscall/fs_tamago.go" "$f"
    sed '1s/^{"Replace": {//' "$1"; } > "$3.new" && mv -f "$3.new" "$3"
}
