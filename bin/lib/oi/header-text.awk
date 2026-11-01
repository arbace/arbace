# The text of an ocamltest header (the first "(* TEST" that is not "(* TEST_BELOW", to its balanced "*)", comments
# inside it left out), on one line: bin/oi suite roles uses it to find the files a header names. Nothing when
# the file has no header. (The scan is actions.awk's.)
{ text = text $0 " " }
END {
  n = length(text); start = 0; pos = 1
  while (1) {
    i = index(substr(text, pos), "(* TEST")
    if (i == 0) break
    i += pos - 1
    if (substr(text, i, 13) == "(* TEST_BELOW") { pos = i + 13; continue }
    start = i; break
  }
  if (start == 0) exit
  depth = 0; body = ""
  for (k = start; k <= n; k++) {
    two = substr(text, k, 2)
    if (two == "(*") { depth++; k++; if (depth > 1) body = body " "; continue }
    if (two == "*)") { depth--; k++; if (depth == 0) break; continue }
    if (depth == 1) body = body substr(text, k, 1)
  }
  print body
}
