; verdict: invalid this attribute does not apply to functions
; rule: llvm/lib/AsmParser/LLParser.cpp:1832 Call-site attribute list is parsed by parseFnAttributeValuePairs; zeroext has no FnAttr property so the parser errors at the first one.
define i32 @f() { ret i32 0 }
define void @g() { call i32 @f() zeroext signext
 ret void }
