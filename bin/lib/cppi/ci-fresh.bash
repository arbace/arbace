# cppi_ci CMD...: CMD (an arbace_tool run of arbace.cpp.run: the C reader, checker, builder and cf2g of ci over the lowered C++)
# under bin/lib/ci/fresh.bash's c_ci_fresh, in a subshell: when the checkout's ci (arbace/c.ae, arbace/c/) differs from the tools'
# host's embedded copy, its namespaces evaluated from the checkout (ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1), as
# bin/ci's own runs are; nothing changes when they are the host's (after a re-seed) or under CI_FRESH=0. Sourced after
# bin/lib/tools.bash with root set.
#
# Why (cpp-threads, 2026-11-15): bin/cppi built every C++ node program with stage 0's embedded ci, the seed d5548e72's (2026-11-05),
# older than ci's threads (de576f46b, 2026-11-06: a _Thread_local object's static `RO 2`, each thread its own bytes). So the lowered
# runtime's _Thread_local pending flag and in-flight exception (arbace/cpp/rt/eh.c.ae's cx$pending, cx$cur, CP103 (b)) were one
# object for every thread: runtime/threads.cpp's threads took each other's exceptions (the old segmentation fault in work(int), then
# std::terminate from this_thread::yield once llvm-eh's cx$exception wrote the adjusted pointer: the same race, test/cpp/selftest/tls.sh).
# Only the arbace.cpp.run steps take it: the C++ front end, the lowering and the judges load no ci namespace, and stay compiled.
. "$root/bin/lib/ci/fresh.bash"
cppi_ci() {
  ( c_ci_fresh || exit 1; "$@" )
}
