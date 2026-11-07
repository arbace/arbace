# C++'s headers without the host's g++ (doc/cpp/CPP-SURVEY.md, "As built: cpp-indep", CP241 (b)): sourced by bin/repr-cpp and
# bin/cppi after $root, bin/lib/tmpdir.bash and bin/lib/tools.bash. REPR_CPP_HEADERS=arbace (unset) reads libstdc++'s headers from
# the tree test/cpp/target.ae makes from the pinned gcc-15.2.0 tarball and the target's facts (cpp/target/libstdcxx.edn; the C
# library's headers it probes are musl's as C's own target lays them out, bin/lib/repr-c/sysroot.bash) and the C library's from
# musl's tarball, no header of the host; REPR_CPP_HEADERS=libstdcxx the tree with the host's C library headers in place;
# REPR_CPP_HEADERS=host the host's installed trees in place, as before (the oracle tier's: `bin/repr-cpp target oracle`).
#   cpp_target_tree       the tree's directory .tmp/cpp-target/KEY (made when missing; KEY: the description, cpp.target, the
#                         tarball's pin and musl's prepared headers): usr/include/c++/15.2.0 and roots.txt
#   cpp_sysroot           exports ARBACE_C_SYSROOT, the tree, which repr/c's preprocessor (C++'s too) reads a name under
#                         roots.txt from (the search lists' three libstdc++ directories), every other name from the host
cpp_target_tree() {
  local main=${root%%/.tmp/worktrees/*} t want k d st cs
  main=${main%%/.tmp/gate-clean/*}
  t=${REPR_CPP_GCC_TARBALL:-$main/.tmp/c1/gcc/gcc-15.2.0.tar.xz}
  want=$(sed -n '/^ {:gcc-15.2.0$/,/:sha256/s/^ *:sha256 "\([0-9a-f]*\)".*/\1/p' "$root/test/cpp/corpora.edn" | head -1)
  # C's sysroot (musl's headers from its tarball, no compiler): the probes' C library
  cs=$( (unset ARBACE_C_SYSROOT ARBACE_C_HOST ARBACE_C_TARGET; . "$root/bin/lib/repr-c/sysroot.bash"; c_sysroot > "$TMPDIR/cpp-target-c.log" 2>&1 && echo "$ARBACE_C_SYSROOT") ) \
    || { cat "$TMPDIR/cpp-target-c.log" >&2; echo "cpp-target: C's sysroot failed" >&2; return 1; }
  [ -n "$cs" ] || { cat "$TMPDIR/cpp-target-c.log" >&2; return 1; }
  k=$( { (cd "$root" && sha256sum cpp/target/libstdcxx.edn test/cpp/target.ae bin/lib/repr-cpp/sysroot.bash); echo "$want"; basename "$cs"; } | sha256sum | cut -c1-16)
  d=$root/.tmp/cpp-target/$k
  if [ ! -f "$d.ok" ]; then
    [ "$(sha256sum < "$t" | cut -c1-64)" = "$want" ] || { echo "cpp-target: $t is not the pinned gcc-15.2.0 tarball (test/cpp/corpora.edn)" >&2; return 1; }
    st=$d.$$; rm -rf "$st"; mkdir -p "$st/src" "$st/usr/include/c++/15.2.0"
    tar -xJf "$t" -C "$st/src" gcc-15.2.0/libstdc++-v3/include gcc-15.2.0/libstdc++-v3/config gcc-15.2.0/libstdc++-v3/libsupc++ \
      gcc-15.2.0/libstdc++-v3/config.h.in gcc-15.2.0/libstdc++-v3/src/c++23 gcc-15.2.0/libgcc/gthr.h gcc-15.2.0/libgcc/gthr-single.h \
      gcc-15.2.0/libgcc/gthr-posix.h gcc-15.2.0/gcc/ginclude gcc-15.2.0/gcc/config/i386 gcc-15.2.0/gcc/gsyslimits.h gcc-15.2.0/gcc/limitx.h \
      gcc-15.2.0/gcc/glimits.h gcc-15.2.0/gcc/limity.h gcc-15.2.0/libgcc/unwind-generic.h gcc-15.2.0/libgcc/gcov.h \
      gcc-15.2.0/libgfortran/ISO_Fortran_binding.h gcc-15.2.0/libquadmath/quadmath.h gcc-15.2.0/libquadmath/quadmath_weak.h \
      gcc-15.2.0/libgomp/acc_prof.h gcc-15.2.0/libgomp/openacc.h gcc-15.2.0/libgomp/omp.h.in gcc-15.2.0/libsanitizer/include/sanitizer \
      || { rm -rf "$st"; return 1; }
    GOMEMLIMIT=1GiB arbace_tool -p "$root/test" -m cpp.target plan "$st/src/gcc-15.2.0" "$st" > "$st/plan.log" 2>&1 || { cat "$st/plan.log" >&2; rm -rf "$st"; return 1; }
    GOMEMLIMIT=1GiB arbace_tool -p "$root/test" -m cpp.target derive "$st/src/gcc-15.2.0" "$cs" "$st/usr/include/c++/15.2.0" > "$st/derive.log" 2>&1 \
      || { cat "$st/derive.log" >&2; rm -rf "$st"; return 1; }
    mv "$st/usr/include/c++/15.2.0/answers.tsv" "$st/answers.tsv"
    while IFS=$'\t' read -r i s; do
      mkdir -p "$st/usr/include/c++/15.2.0/$(dirname "$i")"; cp "$st/src/gcc-15.2.0/$s" "$st/usr/include/c++/15.2.0/$i"
    done < "$st/copy.tsv"
    # the compiler's own include directory (gcc's; :compiler-include)
    local gi=$st/usr/lib/gcc/x86_64-alpine-linux-musl/15.2.0/include
    mkdir -p "$gi"
    GOMEMLIMIT=1GiB arbace_tool -p "$root/test" -m cpp.target plan-compiler "$st/src/gcc-15.2.0" "$st" >> "$st/plan.log" 2>&1 \
      && GOMEMLIMIT=1GiB arbace_tool -p "$root/test" -m cpp.target derive-compiler "$st/src/gcc-15.2.0" "$gi" >> "$st/derive.log" 2>&1 \
      || { cat "$st/plan.log" "$st/derive.log" >&2; rm -rf "$st"; return 1; }
    while IFS=$'\t' read -r i s; do mkdir -p "$gi/$(dirname "$i")"; cp "$st/src/gcc-15.2.0/$s" "$gi/$i"; done < "$st/copy-compiler.tsv"
    (cd "$gi" && find . -type f | LC_ALL=C sort | xargs sha256sum) > "$st/compiler-tree.sha256"
    rm -rf "$st/src"
    # the roots: the libstdc++ directories of g++'s and clang's search lists (test/cpp/host/search-*.txt), their dot-dots resolved
    # lexically as the preprocessor names an opened file (/usr/include/c++/15.2.0, its target and backward directories)
    cat "$root"/test/cpp/host/search-gxx.txt "$root"/test/cpp/host/search-clang.txt | grep '/include/c++/' | while read -r r; do realpath -m -s "$r"; done \
      | awk '!s[$0]++' > "$st/roots.txt"
    (cd "$st/usr/include/c++/15.2.0" && find . -type f | LC_ALL=C sort | xargs sha256sum) > "$st/tree.sha256"
    echo "$cs" > "$st/c-sysroot"
    rm -rf "$d"; mv "$st" "$d"; : > "$d.ok"
  fi
  echo "$d"
}

# cpp_full_sysroot: the libstdc++ tree with the C library's headers of C's own target beside it (musl 1.2.6's from its tarball, its
# usr/include), the compiler's own include directory made from the tarball too, and the libraries' headers of
# cpp/target/libraries.sha256 (fortify-headers', a library's package at its recorded digests, as C's c/target/libraries.txt takes
# gmp's and pcre2's): every directory of g++'s and clang's search lists is a root, so no other header is read from the host
cpp_full_sysroot() {
  local d f cs
  d=$(cpp_target_tree) || return 1
  cs=$(cat "$d/c-sysroot")
  f=$d-full-$(sha256sum < "$root/cpp/target/libraries.sha256" | cut -c1-8)
  if [ ! -f "$f.ok" ]; then
    rm -rf "$f.$$"; mkdir -p "$f.$$"; cp -al "$d/usr" "$f.$$/usr"
    (cd "$cs/usr/include" && find . -type f) | while read -r h; do mkdir -p "$f.$$/usr/include/$(dirname "$h")"; cp "$cs/usr/include/$h" "$f.$$/usr/include/$h"; done
    # the libraries' headers (cpp/target/libraries.sha256: fortify-headers'), the host's package files at their recorded digests
    local want id
    grep -v '^#' "$root/cpp/target/libraries.sha256" | while read -r want id; do
      if [ -f "$id" ] && [ "$(sha256sum < "$id" | cut -c1-64)" = "$want" ]; then mkdir -p "$f.$$$(dirname "$id")"; cp "$id" "$f.$$$id"
      else echo "C++: $id is not the host's at cpp/target/libraries.sha256's digest: left out of the sysroot" >&2; fi
    done
    cat "$root"/test/cpp/host/search-gxx.txt "$root"/test/cpp/host/search-clang.txt | while read -r r; do realpath -m -s "$r"; done | awk 'NF && !s[$0]++' > "$f.$$/roots.txt"
    cp "$d/tree.sha256" "$f.$$/tree.sha256"
    rm -rf "$f"; mv "$f.$$" "$f"; : > "$f.ok"
  fi
  echo "$f"
}

cpp_sysroot() {
  [ -n "${ARBACE_C_SYSROOT:-}" ] && [ -f "$ARBACE_C_SYSROOT/tree.sha256" ] && return 0
  local d
  case ${REPR_CPP_HEADERS:-arbace} in
    host) return 0 ;;
    arbace) d=$(cpp_full_sysroot) || return 1 ;;
    libstdcxx) d=$(cpp_target_tree) || return 1 ;;
    *) echo "C++: REPR_CPP_HEADERS is arbace, libstdcxx or host, not $REPR_CPP_HEADERS" >&2; return 1 ;;
  esac
  export ARBACE_C_SYSROOT=$d
}
