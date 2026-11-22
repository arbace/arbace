; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:2654 Function !prof {!"function_entry_count", i64 3}: two operands, an accepted name and a ConstantAsMetadata count (verifyFunctionMetadata).
define i32 @f(i32 %x) !prof !0 {
 ret i32 %x
}
!0 = !{!"function_entry_count", i64 3}
