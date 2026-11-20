; verdict: invalid Expected ')' here
; rule: llvm/lib/AsmParser/LLParser.cpp:7601 #dbg_value with 5 operands: after the location (!2) parseDebugRecord expects ')' but finds ','.
define void @f() {
    #dbg_value(i32 0, !1, !DIExpression(), !2, !2)
  ret void
}
