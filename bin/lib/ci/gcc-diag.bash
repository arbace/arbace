# gcc's diagnostics of a dg file, kept (CS201): the lines `FILE:LINE:COL: fatal error|error|warning|note: ...` only, the
# source snippets and notes' carets dropped, and up to 1 MB of them, no longer the first 20,000 bytes of the output (a file
# of hundreds of warnings, c23-ucn-2, lost its later errors). A filter: gcc_diag < gcc's output.
gcc_diag() {
  grep -E '^[^ ][^ ]*:[0-9]+:([0-9]+:)? (fatal error|error|warning|note): |^[^ :][^ :]*: (fatal error|error): ' | head -c 1000000
}
