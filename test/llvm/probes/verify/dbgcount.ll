; verdict: invalid Expected ',' here
; rule: llvm/lib/AsmParser/LLParser.cpp:7569 #dbg_value has only 3 operands; after parsing the DIExpression parseDebugRecord requires a comma before the DILocation but sees ')'.
define void @f() {
    #dbg_value(i32 0, !1, !DIExpression())
  ret void
}
