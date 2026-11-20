; verdict: invalid Instruction does not dominate all uses!
; rule: llvm/lib/IR/Verifier.cpp:5211 Invoke result %r used in the landing pad (unwind) block: only the normal edge dominates, lp is visited before ok.
declare i32 @g()
declare i32 @p(...)
define i32 @f() personality ptr @p {
  %r = invoke i32 @g() to label %ok unwind label %lp
lp:
  %l = landingpad { ptr, i32 } cleanup
  ret i32 %r
ok:
  ret i32 %r
}
