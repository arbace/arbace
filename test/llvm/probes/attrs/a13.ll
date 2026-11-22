; verdict: invalid Attributes 'readonly and writeonly' are incompatible!
; rule: llvm/lib/IR/Verifier.cpp:2134 readonly and writeonly together on a ptr parameter; the earlier readnone pairs do not match.
define void @f(ptr readonly writeonly %p) { ret void }
