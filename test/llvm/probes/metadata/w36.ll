; verdict: invalid !prof annotations should have no less than 2 operands
; rule: llvm/lib/IR/Verifier.cpp:2637 verifyFunctionMetadata, called from visitFunction, first requires the function's !prof to have at least 2 operands; it has one.
define i32 @f(i32 %x) !prof !0 {
 ret i32 %x
}
!0 = !{!"function_entry_count"}
