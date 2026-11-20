; verdict: invalid LandingPadInst needs to be in a function with a personality.
; rule: llvm/lib/IR/Verifier.cpp:4880 Function has no personality; reached in visitLandingPadInst after the EH-pad predecessor checks pass.
declare void @t()
define void @f() {
  invoke void @t() to label %ok unwind label %lp
ok:
  ret void
lp:
  %l = landingpad { ptr, i32 } cleanup
  resume { ptr, i32 } %l
}
