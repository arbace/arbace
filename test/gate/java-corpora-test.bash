#!/bin/bash
# bin/gate-tests' registration of test/java/corpora-test.bash (GT8, the tools' lists): bin/java-check's runs of whole
# corpora (the gate's parts, DIR/corpus) and of lists (--list, partial) judged by --judge over fabricated results. Seconds.
# needs: exe (stage 0, the judgment's host)
root=$(cd "$(dirname "$0")/../.." && pwd)
exec bash "$root/test/java/corpora-test.bash"
