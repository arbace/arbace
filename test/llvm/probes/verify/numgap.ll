; verdict: invalid instruction expected to be numbered '%6' or greater
; rule: llvm/lib/AsmParser/LLParser.cpp:3598 %5 is accepted, then %3 < next ID 6 (setInstName -> checkValueID at 4066).
define void @f() {
  %5 = add i32 1, 2
  %3 = add i32 1, 2
  ret void
}
