; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3077 cfguard_checkcc has no restriction in visitFunction's switch.
define cfguard_checkcc void @f() { ret void }
