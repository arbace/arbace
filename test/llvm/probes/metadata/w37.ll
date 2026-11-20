; verdict: invalid first operand should be 'function_entry_count' or 'synthetic_function_entry_count'
; rule: llvm/lib/IR/Verifier.cpp:2654 The function !prof name is "bogus" (not "unknown"): a string, but not an entry-count name.
define i32 @f(i32 %x) !prof !0 {
 ret i32 %x
}
!0 = !{!"bogus", i64 3}
