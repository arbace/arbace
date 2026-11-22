; verdict: valid
; rule: llvm/lib/IR/Function.cpp:924 Unknown llvm.* declaration only sets HasLLVMReservedName; neither parser nor Verifier rejects an unknown intrinsic name on a declaration.
declare void @llvm.nosuchthing()
