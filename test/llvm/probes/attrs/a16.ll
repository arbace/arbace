; verdict: invalid Cannot have multiple 'sret' parameters!
; rule: llvm/lib/IR/Verifier.cpp:2321 Second sret parameter: first one is at index 0 and passes, the second (index 1) sets SawSRet failure.
define void @f(ptr sret(i32) %p, ptr sret(i32) %q) { ret void }
