; verdict: invalid alignment is not a power of two
; rule: llvm/lib/AsmParser/LLParser.cpp:2525 align 3 in the parameter attribute: parseOptionalAlignment rejects non powers of two.
define void @f(ptr align 3 %p) { ret void }
