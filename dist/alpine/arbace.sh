#!/bin/sh
# /usr/bin/arbace: Arbace for Java 26, the self-contained runtime image installed in
# /usr/lib/arbace-java26 (its own JDK, jar and AOT cache). Arguments as for arbace.main:
# none for the REPL, -e EXPR, -m NS, a script, - for a script on stdin.
exec /usr/lib/arbace-java26/bin/arbace "$@"
