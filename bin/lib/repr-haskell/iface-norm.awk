# bin/repr-haskell lib-check --verify-pack's normal form of an interface line (bin/lib/repr-haskell/lib-check's norm): GHC's uniques,
# stable within one session only, renumbered in their order of first appearance on the line (a thing's line is its own scope):
# a type variable's "occ#UNIQUE", an erased Core binder's (lv "occ" UNIQUE), a pretty-printed local name's occ_aXXXX suffix
function renum(s, re, pre, post,    out, u) {
  out = ""
  while (match(s, re)) {
    u = substr(s, RSTART + pre, RLENGTH - pre - post)
    if (!((re, u) in id)) id[re, u] = ++k[re]
    out = out substr(s, 1, RSTART - 1 + pre) "~" id[re, u] substr(s, RSTART + RLENGTH - post, post)
    s = substr(s, RSTART + RLENGTH)
  }
  return out s
}
{ delete id; delete k
  s = renum($0, "#[0-9]+\"", 1, 1)
  s = renum(s, "[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]+", 0, 0)
  s = renum(s, "_a[0-9A-Za-z][0-9A-Za-z][0-9A-Za-z]?[0-9A-Za-z]?[^0-9A-Za-z_']", 2, 1)
  print s }
