# needs-host
# queue 324: the code generator's Sema read a header declaration's text (text-of, located-file) by its absolute name from the
# host, while the preprocessor had read the header from the target tree (ARBACE_C_SYSROOT): on amd64 the host's installed
# libstdc++ held the same bytes, on an arm64 host x86_64-alpine-linux-musl/bits/c++config.h is absent and the text fell back to
# the main file's (every program with a header crashed, StringIndexOutOfBounds in qualifier-before). Now a name under the tree's
# roots is the tree's file; any other name the host's, as arbace.repr.c.pp's sysroot-file. The control: a name outside the roots.
. "$(dirname "$0")/lib.bash"
tree=$t/tree; mkdir -p "$tree/usr/include/c++/15.2.0/bits"
printf '/usr/include/c++/15.2.0\n' > "$tree/roots.txt"
echo '// the tree' > "$tree/usr/include/c++/15.2.0/bits/q324.h"
r=$(ARBACE_C_SYSROOT=$tree arbace_tool -e "(require 'arbace.cpp.sema) (println (.getPath (#'arbace.cpp.sema/located-file \"/usr/include/c++/15.2.0/bits/q324.h\"))) (println (.getPath (#'arbace.cpp.sema/located-file \"/etc/os-release\")))" 2>"$t/err.log")
[ "$(echo "$r" | sed -n 1p)" = "$tree/usr/include/c++/15.2.0/bits/q324.h" ] && ok "a header under the tree's roots: the tree's file" || bad "under the roots: $r"
[ "$(echo "$r" | sed -n 2p)" = /etc/os-release ] && ok "control: a name outside the roots stays the host's" || bad "outside: $r"
r=$(ARBACE_C_SYSROOT=/ arbace_tool -e "(require 'arbace.cpp.sema) (println (.getPath (#'arbace.cpp.sema/located-file \"/usr/include/c++/15.2.0/bits/q324.h\")))" 2>"$t/err.log")
[ "$r" = /usr/include/c++/15.2.0/bits/q324.h ] && ok "no tree (ARBACE_C_SYSROOT=/): the name as written" || bad "no tree: $r"
finish
