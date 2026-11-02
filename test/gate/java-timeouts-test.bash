#!/bin/bash
# bin/gate-tests' registration of test/java/timeouts-test.bash (GTS102 redone, the user 2026-11-02: queue 115's
# test committed): bin/java-check's per-test limits (test/java/timeouts.txt) on fabricated plans. Light, no
# Arbace executable. It reads ROOT as its first argument.
root=$(cd "$(dirname "$0")/../.." && pwd)
exec bash "$root/test/java/timeouts-test.bash" "$root"
