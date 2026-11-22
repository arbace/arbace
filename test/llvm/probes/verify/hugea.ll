; verdict: invalid huge alignments are not supported yet
; rule: llvm/lib/AsmParser/LLParser.cpp:2527 align 2^33 is a power of two but exceeds Value::MaximumAlignment (2^32).
@g = global i32 0, align 8589934592
