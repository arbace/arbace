#!/bin/sh
# /usr/bin/arb: the Arbace REPL with line editing, as clj is to clojure in Alpine's clojure
# package: rlwrap (history, completion of words seen, Clojure's delimiters as word breaks)
# around /usr/bin/arbace. Arguments as for arbace. Use arbace itself for scripts and pipes.
if command -v rlwrap >/dev/null 2>&1; then
	exec rlwrap -m -r -q '\"' -b "(){}[],^%#@\";:'" /usr/bin/arbace "$@"
fi
echo 'Please install rlwrap for command editing or use "arbace" instead.' >&2
exec /usr/bin/arbace "$@"
