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
# 10. the survey names the queue task, not a scratch path; GTS121 says what the refusal covers; GTS126
lacks doc/GATE-SURVEY.md '.tmp/mac-queue/reports/040'
has doc/GATE-SURVEY.md 'GTS126 how a miss fills the directory'
has doc/GATE-SURVEY.md 'or like javac'"'"'s tool'"'"'s'
# 048: the miss's text
has CLAUDE.md 'a miss replaces only a directory of its own making'
lacks CLAUDE.md 'a miss never removes the directory of another key'
has doc/GATE-SURVEY.md '**Built: (a) with (b)**'
exit $fail
