#!/bin/bash
# The own verdicts' unit tests and negative controls of LLS38 and LLS39 (test/repr/llvm/expect_test.ae; doc/llvm/LLVM-CROP-SURVEY.md,
# "As built: LLS38, LLS39"): seconds, on stage 0, no LLVM tool.
set -uo pipefail
src=$(cd "$(dirname "$0")/../../.." && pwd)
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
root=$src; . "$src/bin/lib/tools.bash"
arbace_tool -p "$src/test" -m repr.llvm.expect-test
