; verdict: invalid expected '...' at end of argument list for musttail call in varargs function
; rule: llvm/lib/AsmParser/LLParser.cpp:3364 Caller f(...) is varargs and the musttail call has an empty argument list without ...; parse error.
declare void @llvm.trap()
define void @f(...) {
 musttail call void @llvm.trap()
 ret void
}
