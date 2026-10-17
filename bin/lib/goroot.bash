# The Go toolchain's variable, ARBACE_GOROOT: TamaGo's Go tree (go1.27.1, default
# /root/tamago-go), the only Go any build uses. Until 2026-10-17 it was G2A_GOROOT (named after
# g2a, since renamed repr/go: doc/NAMING-SURVEY.md, "Decided"); no script reads that name any
# more, so a shell that still sets it, and not ARBACE_GOROOT, would build with the default tree
# without a word. Sourced by the scripts that read ARBACE_GOROOT, which stop here in that case.
if [ -n "${G2A_GOROOT:-}" ] && [ -z "${ARBACE_GOROOT:-}" ]; then
  echo "G2A_GOROOT is set ($G2A_GOROOT) and ARBACE_GOROOT is not: G2A_GOROOT was renamed ARBACE_GOROOT on 2026-10-17; set ARBACE_GOROOT (and unset G2A_GOROOT)" >&2
  exit 2
fi
