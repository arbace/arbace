# Review 007's finding 10: pp-check marked a failed extraction done, and its guard against a shrunken corpus never ran (the
# sample was never falsy). Now the marker is written only when tar succeeds, lists refuses a tree with another number of
# files than the manifest's, and the check names a recorded file that is not in the tree.
. "$(dirname "$0")/lib.bash"
tree=$t/tree; mkdir -p "$tree/test/repr" "$tree/test/cpp"
cp -r "$root/bin" "$root/arbace" "$tree/"; cp -r "$root/test/cpp/." "$tree/test/cpp/"; cp -r "$root/test/repr/cpp" "$tree/test/repr/"
# 1. a tarball that ends early (its digest pinned in the copy): tar fails, nothing is marked extracted
head -c 2000000 /root/arbace/.tmp/c1/gcc/gcc-15.2.0.tar.xz > "$t/short.tar.xz"
sum=$(sha256sum "$t/short.tar.xz" | cut -d' ' -f1)
sed -i "s|:sha256 \"438fd996826b0c82485a29da03a72d71d6e3541a83ec702df4271f6fe025d24e\"|:sha256 \"$sum\"|" "$tree/test/cpp/corpora.edn"
REPR_CPP_GCC_TARBALL=$t/short.tar.xz REPR_CPP_CHECK_WORK=$t/w1 timeout 300 "$tree/bin/repr-cpp" pp-check --corpus cpp-lexical > "$t/1.log" 2>&1; st=$?
[ $st != 0 ] && ok "a failed extraction stops pp-check (exit $st)" || bad "a failed extraction: exit 0"
[ ! -f "$tree/.tmp/cpp-gcc/.extracted" ] && ok "and is not marked extracted" || bad "a failed extraction is marked extracted"
rm -rf "$tree/.tmp/cpp-gcc"
# 2. lists on a shrunken corpus (one file of test/cpp/lexical gone): refused
rm "$tree/test/cpp/lexical/varargs.cpp"
GOMEMLIMIT=1GiB arbace_tool -m arbace.repr.cpp.ppcheck lists "$t/w2" "{:checkout \"$tree\"}" 1 cpp-lexical > "$t/2.log" 2>&1; st=$?
[ $st != 0 ] && grep -q "has 18 files, the manifest counts 19" "$t/2.log" && ok "lists refuses 18 files of 19" || bad "lists on 18 of 19: exit $st: $(grep -v Reflection "$t/2.log" | tail -1)"
# 3. a recorded file renamed (the count kept): the check names it
mv "$tree/test/cpp/lexical/udl.cpp" "$tree/test/cpp/lexical/udl-renamed.cpp"; cp "$root/test/cpp/lexical/varargs.cpp" "$tree/test/cpp/lexical/"
( cd "$tree" && GOMEMLIMIT=1GiB arbace_tool -m arbace.repr.cpp.ppcheck lists "$t/w3" "{:checkout \"$tree\"}" 1 cpp-lexical && \
  GOMEMLIMIT=1GiB arbace_tool -m arbace.repr.cpp.ppcheck check "$t/w3" lex cpp-lexical ) > "$t/3.log" 2>&1; st=$?
[ $st = 1 ] && grep -q "recorded files not in the tree: udl.cpp" "$t/3.log" && ok "the check names the recorded file not in the tree" || bad "the renamed file: exit $st: $(grep -v Reflection "$t/3.log" | tail -1)"
rm -rf "$tree"
finish
