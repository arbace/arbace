; verdict: invalid expected integer argument to function_entry_count
; rule: llvm/lib/IR/Verifier.cpp:2663 The second operand of the function's !prof is the string "x", not ConstantAsMetadata.
define i32 @f(i32 %x) !prof !0 {
 ret i32 %x
}
!0 = !{!"function_entry_count", !"x"}
