; verdict: invalid Function has metadata parameter but isn't an intrinsic
; rule: llvm/lib/IR/Verifier.cpp:4013 Indirect call whose function type has a metadata parameter; visitCallBase reaches it before the instruction checks.
define void @f(ptr %x) {
 call void %x(metadata !0)
 ret void
}
!0 = !{}
