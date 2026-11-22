; verdict: invalid Attributes writable and readonly are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2149 writable with readonly; the readnone pairs do not match first.
define void @f(ptr writable readonly %p) { ret void }
