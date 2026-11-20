; verdict: invalid this attribute does not apply to functions
; rule: llvm/lib/AsmParser/LLParser.cpp:1832 nonnull has only Param/Ret properties; the parser rejects it among function attributes.
define void @f() nonnull { ret void }
