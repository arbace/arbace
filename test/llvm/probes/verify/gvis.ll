; verdict: invalid symbol with local linkage must have default visibility
; rule: llvm/lib/AsmParser/LLParser.cpp:1430 internal + hidden rejected at the top of parseGlobal.
@g = internal hidden global i32 0
