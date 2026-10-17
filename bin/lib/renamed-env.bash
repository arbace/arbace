# The environment variables renamed on 2026-10-17 (doc/NAMING-SURVEY.md, "Decided" and "The
# rename as built"): G2A_GOROOT is ARBACE_GOROOT (TamaGo's Go tree, go1.27.1, default
# /root/tamago-go, the only Go any build uses), and a2g's A2G_* are j2g's J2G_*. No script reads
# the old names any more, so a shell that still sets one would run with the defaults without a
# word. Sourced by the scripts that read ARBACE_GOROOT or J2G_*, which stop here in that case:
# when G2A_GOROOT is set and ARBACE_GOROOT is not, or when any A2G_* is set.
if [ -n "${G2A_GOROOT:-}" ] && [ -z "${ARBACE_GOROOT:-}" ]; then
  echo "G2A_GOROOT is set ($G2A_GOROOT) and ARBACE_GOROOT is not: G2A_GOROOT was renamed ARBACE_GOROOT on 2026-10-17; set ARBACE_GOROOT (and unset G2A_GOROOT)" >&2
  exit 2
fi
if a2g_vars=$(compgen -e | grep '^A2G_'); then
  echo "set: $(echo $a2g_vars): a2g's variables were renamed J2G_* on 2026-10-17 (A2G_JOBS is J2G_JOBS, ...); unset them (and set the J2G_ ones)" >&2
  exit 2
fi
unset a2g_vars
