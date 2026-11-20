; verdict: invalid Range types must match instruction type!
; rule: llvm/lib/IR/Verifier.cpp:4492 Both bounds are i64 (the pair types match) but the load yields i32.
define i32 @f(ptr %p) {
 %v = load i32, ptr %p, !range !0
 ret i32 %v
}
!0 = !{i64 0, i64 5}
