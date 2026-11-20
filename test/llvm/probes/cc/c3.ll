; verdict: invalid Calling convention does not support varargs or perfect forwarding!
; rule: llvm/lib/IR/Verifier.cpp:3126 fastcc is in the varargs-restricted group.
define fastcc void @f(...) { ret void }
