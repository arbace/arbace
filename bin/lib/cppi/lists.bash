# The run corpora of bin/cppi's run checks (doc/cpp/CPP-SURVEY.md, "As built: CX8 (write-ahead)"), sourced by bin/cppi and
# bin/lib/cppi/{record-ref,check,check-aot}: run_list CORPUS prints the corpus's programs, one a line, tab-separated:
#   ID STD ARGS PATH
# ID is CORPUS/NAME (NAME the path under the corpus's root), STD the -std the program is compiled at by g++ (the reference)
# and by clang++ (the oracle) alike, ARGS the options both are given (the modelled ones: -I, -D, -U and the language
# switches below), PATH the source. The corpora:
#   samples     test/cpp/samples (CX4-CX6), c++23
#   runtime     test/cpp/runtime/samples (CX7's), c++23
#   libstdcxx   libstdc++-v3/testsuite's run tests as bin/cppi list libstdcxx selects them (every one: no compile, link,
#               preprocess or error directive, no threads, shared libraries, named locales, file I/O, debug mode or xfail),
#               -I util; linked with the testsuite's util/ library
#   gxx-dg-run  gcc/testsuite/g++.dg's `dg-do run` tests (the survey's §8), one file each: not modules/ (CX9's) or contracts/
#               (-fcontracts, which clang does not have), no
#               dg-additional-sources, -flto or xfail, no target selector naming only another architecture or OS; the
#               standard from the selector (target c++20 -> gnu++20, c++14_down -> gnu++14 ...; default gnu++17, g++'s; coroutines/
#               gnu++20, as coroutines.exp runs them) or dg-options' -std
# Needs root (the checkout) and the extracted trees (cppi_extract).
cppi_gcc=$root/.tmp/cppi/gcc/gcc-15.2.0
cppi_ts=$cppi_gcc/libstdc++-v3/testsuite
cppi_gxxdg=$cppi_gcc/gcc/testsuite/g++.dg
cppi_corpora_all=samples,runtime,libstdcxx,gxx-dg-run

# the pinned tarball extracted (the testsuite and g++.dg), its digest checked once per extraction
cppi_extract() {
  local t want main=$root
  case $root in */.tmp/worktrees/*) main=${root%%/.tmp/worktrees/*} ;; esac
  [ -d "$cppi_ts/util" ] && [ -d "$cppi_gxxdg" ] && return 0
  t=${REPR_CPP_GCC_TARBALL:-$main/.tmp/c1/gcc/gcc-15.2.0.tar.xz}
  want=$(sed -n '/^ {:gcc-15.2.0$/,/:sha256/s/^ *:sha256 "\([0-9a-f]*\)".*/\1/p' "$root/test/cpp/corpora.edn" | head -1)
  [ -n "$want" ] && [ "$(sha256sum "$t" | cut -d' ' -f1)" = "$want" ] || { echo "cppi: $t is not the pinned tarball" >&2; return 1; }
  mkdir -p "$root/.tmp/cppi/gcc"
  tar -xJf "$t" -C "$root/.tmp/cppi/gcc" gcc-15.2.0/libstdc++-v3/testsuite gcc-15.2.0/gcc/testsuite/g++.dg
}

# the options a test's dg-options give both compilers (the modelled ones)
cppi_opts() {
  tr ' ' '\n' | grep -E '^-(D|U|f(no-)?(exceptions|rtti|char8_t|sized-deallocation|aligned-new|elide-constructors|threadsafe-statics|new-ttp-matching|strict-enums|operator-names|access-control|implicit-templates))' | tr '\n' ' ' || true
}

run_list_libstdcxx() {
  local f std opts s
  ( cd "$cppi_ts" && find . -name '*.cc' -path './[0-9][0-9]_*' | LC_ALL=C sort ) | sed 's|^\./||' > "$TMPDIR/cppi-all.list"
  ( cd "$cppi_ts" && xargs grep -L -E "dg-do (compile|link|preprocess)|dg-error|dg-additional-sources|dg-require-(thread|effective-target pthread)|-pthread|dg-require-sharedlib|dg-require-namedlocale|dg-require-fileio|dg-require-gthreads|dg-require-debug-mode|xfail" < "$TMPDIR/cppi-all.list" ) \
    | grep -v '^30_threads' > "$TMPDIR/cppi-run.list" || true
  while read -r f; do
    std=gnu++17
    if grep -q -E 'target c\+\+26' "$cppi_ts/$f"; then std=gnu++26
    elif grep -q -E 'target c\+\+23' "$cppi_ts/$f"; then std=gnu++23
    elif grep -q -E 'target c\+\+2[0a]' "$cppi_ts/$f"; then std=gnu++20; fi
    opts=$(grep -o 'dg-options "[^"]*"' "$cppi_ts/$f" | head -1 | sed 's/dg-options "//; s/"$//' || true)
    s=$(printf '%s' "$opts" | grep -o -- '-std=[a-z+0-9]*' | tail -1 | sed 's/-std=//' || true)
    [ -n "$s" ] && std=$s
    printf 'libstdcxx/%s\t%s\t%s\t%s\n' "$f" "$std" "-I$cppi_ts/util $(printf '%s' "$opts" | cppi_opts)" "$cppi_ts/$f"
  done < "$TMPDIR/cppi-run.list"
}

run_list_gxxdg() {
  local f sel std opts s
  ( cd "$cppi_gxxdg" && grep -rl --include='*.C' --include='*.cc' -E 'dg-do run' . | LC_ALL=C sort ) | sed 's|^\./||' \
    | grep -v -E '^(modules|contracts)/' > "$TMPDIR/cppi-gxx.list" || true
  while read -r f; do
    grep -q -E 'dg-additional-sources|-flto|dg-module' "$cppi_gxxdg/$f" && continue
    sel=$(grep -o -m1 'dg-do run.*' "$cppi_gxxdg/$f")
    case $sel in *xfail*) continue ;; esac
    # a selector naming targets: kept when one is x86_64's, i?86's or any Linux's (native, c++NN and the ABI's names kept)
    if printf '%s' "$sel" | grep -q -E '[a-z0-9?*]+-[*a-z0-9]+-[*a-z0-9]+'; then
      printf '%s' "$sel" | grep -q -E 'x86_64-|i\?86-|\*-\*-linux|\*-\*-gnu' || continue
      printf '%s' "$sel" | grep -q -E '!|mingw|darwin|ia64|hpux' && continue
    fi
    case $sel in *arm*-*|*ia64*|*mips*|*powerpc*|*sparc*|*aarch64*) continue ;; esac
    std=gnu++17
    case $sel in
      *c++98_only*|*c++98_down*) std=gnu++98 ;; *c++11_down*) std=gnu++11 ;; *c++14_down*) std=gnu++14 ;; *c++17_down*) std=gnu++17 ;; *c++11_only*) std=gnu++11 ;; *c++14_only*) std=gnu++14 ;; *c++17_only*) std=gnu++17 ;;
      *c++20_only*) std=gnu++20 ;; *c++26*) std=gnu++26 ;; *c++23*) std=gnu++23 ;; *c++20*|*c++2a*) std=gnu++20 ;;
    esac
    # coroutines.exp runs its tests at C++20 (and -fcoroutines, which C++20 implies)
    case $f in coroutines/*) [ "$std" = gnu++17 ] && std=gnu++20 ;; esac
    opts=$(grep -o 'dg-options "[^"]*"' "$cppi_gxxdg/$f" | head -1 | sed 's/dg-options "//; s/"$//' || true)
    s=$(printf '%s' "$opts" | grep -o -- '-std=[a-z+0-9]*' | tail -1 | sed 's/-std=//' || true)
    [ -n "$s" ] && std=$s
    printf 'gxx-dg-run/%s\t%s\t%s\t%s\n' "$f" "$std" "$(printf '%s' "$opts" | cppi_opts)" "$cppi_gxxdg/$f"
  done < "$TMPDIR/cppi-gxx.list"
}

run_list() { # run_list CORPUS
  local f
  case $1 in
    samples) for f in "$root"/test/cpp/samples/*.cpp; do printf 'samples/%s\tc++23\t\t%s\n' "$(basename "$f")" "$f"; done ;;
    runtime) for f in "$root"/test/cpp/runtime/samples/*.cpp; do printf 'runtime/%s\tc++23\t\t%s\n' "$(basename "$f")" "$f"; done ;;
    libstdcxx) cppi_extract && run_list_libstdcxx ;;
    gxx-dg-run) cppi_extract && run_list_gxxdg ;;
    *) echo "cppi: no run corpus $1 ($cppi_corpora_all)" >&2; return 2 ;;
  esac
}

# the libraries a corpus's programs link natively (the reference's and the original's build): libstdcxx the testsuite's util/
# library as its testsuite links it (libtestc++, built by g++ once a run into DIR) and libstdc++exp
cppi_native_libs() { # CORPUS DIR
  local f
  case $1 in
    libstdcxx)
      if [ ! -f "$2/libtestc++.a" ]; then
        mkdir -p "$2/libtestc++"
        for f in testsuite_abi testsuite_allocator testsuite_character testsuite_hooks; do
          g++ -std=gnu++17 -O0 -I"$cppi_ts/util" -c "$cppi_ts/util/$f.cc" -o "$2/libtestc++/$f.o" || return 1
        done
        ar rcs "$2/libtestc++.a" "$2"/libtestc++/*.o
      fi
      echo "$2/libtestc++.a -lstdc++exp -lstdc++ -lm -latomic" ;;
    *) echo "-lstdc++ -lm -latomic" ;;
  esac
}
