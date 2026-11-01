// CX7 (test/cpp/runtime/units.edn, :shims): libstdc++-v3/src/c++11/assert_fail.cc as the oracle compiles it. The installed
// <bits/c++config.h> declares std::__glibcxx_assert_fail with __attribute__((__noreturn__)) and the file defines it with
// [[__noreturn__]], which clang rejects ("'noreturn' attribute does not appear on the first declaration"; g++ accepts it).
// The headers the file includes are read first, then the C++ attribute's name is spelled as one clang takes on a later
// declaration; the declaration in c++config.h keeps the noreturn. The file itself is unchanged, included from the tarball.
#include <bits/c++config.h>
#include <cstdlib>
#include <cstdio>
#define __noreturn__ __gnu__::__cold__
#include <assert_fail.cc>
