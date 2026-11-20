; verdict: invalid Attributes writable and readnone are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2145 writable with readnone; readnone/readonly and readnone/writeonly checks do not match, writable/readnone does.
define void @f(ptr writable readnone %p) { ret void }
