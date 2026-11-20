; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:2527 align 2^32 equals Value::MaximumAlignment (1<<32); parser and verifier only reject values greater than it.
define void @f(ptr align 4294967296 %p) { ret void }
