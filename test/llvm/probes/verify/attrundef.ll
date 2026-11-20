; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:224 Undefined attribute group #4 is silently skipped in validateEndOfModule (NumberedAttrBuilders.find miss merges nothing); no error is issued.
define void @f() #4 {
  ret void
}
