; verdict: invalid Only PHI nodes may reference their own value!
; rule: llvm/lib/IR/Verifier.cpp:5670 %x = add i32 %x, 1 in a reachable block: forward ref resolves to the instruction itself; parser accepts it.
define i32 @f() {
  %x = add i32 %x, 1
  ret i32 %x
}
