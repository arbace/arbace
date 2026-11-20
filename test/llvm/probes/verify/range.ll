; verdict: invalid Ranges are only for loads, calls and invokes!
; rule: llvm/lib/IR/Verifier.cpp:5797 !range on a store, checked in visitInstruction.
define void @f(ptr %p) {
  store i32 1, ptr %p, !range !0
  ret void
}
!0 = !{i32 0, i32 4}
