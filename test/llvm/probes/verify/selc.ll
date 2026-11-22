; verdict: invalid select condition must be i1 or <n x i1>
; rule: llvm/lib/IR/Instructions.cpp:123 select with i32 condition; parseSelect reports SelectInst::areInvalidOperands' string at LLParser.cpp:8592.
define i32 @f(i32 %c) {
  %s = select i32 %c, i32 1, i32 2
  ret i32 %s
}
