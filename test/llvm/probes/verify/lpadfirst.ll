; verdict: invalid The unwind destination does not have an exception handling instruction!
; rule: llvm/lib/IR/Verifier.cpp:4252 lp starts with an add so BasicBlock::isEHPad is false; visitInvokeInst in the entry block fails before lp is visited.
declare i32 @p(...)
declare void @t()
define void @f() personality ptr @p {
  invoke void @t() to label %ok unwind label %lp
ok:
  ret void
lp:
  %z = add i32 1, 2
  %l = landingpad { ptr, i32 } cleanup
  resume { ptr, i32 } %l
}
