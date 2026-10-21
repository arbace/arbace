# TMPDIR for the scripts of bin/: when unset, the repository's .tmp/tmp (gitignored), so that
# Arbace's error reports (arbace-*.edn, written to $TMPDIR, else /tmp) and mktemp's files stay in the
# checkout (the user, 2026-10-21). Sourced by every script of bin/ right after it sets $root.
if [ -z "${TMPDIR:-}" ]; then
  export TMPDIR=$root/.tmp/tmp
fi
mkdir -p "$TMPDIR"
