; verdict: invalid wchar_size metadata requires constant integer argument
; rule: llvm/lib/IR/Verifier.cpp:1992 Error behavior accepts any value and the ID is unique; then the wchar_size rule requires a ConstantInt but finds !"x".
!llvm.module.flags = !{!0}
!0 = !{i32 1, !"wchar_size", !"x"}
