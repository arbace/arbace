; verdict: invalid The unwind destination does not have an exception handling instruction!
; rule: llvm/lib/IR/Verifier.cpp:4253 The invoke unwinds to block a whose first instruction is ret, not an EH pad.
declare void @g(i32)
define void @f(i32 %x) personality ptr null {
 invoke void @g(i32 %x) to label %a unwind label %a
a:
 ret void
}
