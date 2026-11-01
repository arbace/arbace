#!/bin/bash
# CLAUDE.md's sentences the review 045 corrected stay corrected. Usage: test/gate/claude-md-test.bash
here=$(cd "$(dirname "$0")/../.." && pwd)
fail=0
has() { grep -qF -- "$2" "$here/$1" && echo "ok   $1 has: $2" || { echo "FAIL $1 lacks: $2"; fail=1; }; }
lacks() { grep -qF -- "$2" "$here/$1" && { echo "FAIL $1 still has: $2"; fail=1; } || echo "ok   $1 lacks: $2"; }
# 7. repr/c
has CLAUDE.md '`arbace/repr/c/` (`arbace.repr.c.*`'
lacks CLAUDE.md 'arbace/repr/c{.ae,/}'
lacks CLAUDE.md 'CS1-CS51'
has CLAUDE.md "against gcc"
# 8. toolSource
has CLAUDE.md 'a namespace of a tool or of goi (`arbace/go/`;'
lacks CLAUDE.md '(and every namespace of `arbace/go/`'
# 9. no heavy lock; heavy-lock by absolute path
has CLAUDE.md 'under no heavy lock'
lacks CLAUDE.md 'Go build, under no lock'
has CLAUDE.md 'ARBACE_HEAVY_CMD="$PWD/bin/heavy-lock"'
exit $fail
