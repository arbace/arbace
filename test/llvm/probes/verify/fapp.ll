; verdict: invalid invalid function linkage type
; rule: llvm/lib/AsmParser/LLParser.cpp:7103 appending linkage on a function header is rejected in parseFunctionHeader.
declare appending void @f()
