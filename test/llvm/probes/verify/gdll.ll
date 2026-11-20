; verdict: invalid symbol with local linkage cannot have a DLL storage class
; rule: llvm/lib/AsmParser/LLParser.cpp:1434 internal + dllexport rejected at the top of parseGlobal.
@g = internal dllexport global i32 0
