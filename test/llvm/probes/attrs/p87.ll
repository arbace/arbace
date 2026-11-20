; verdict: invalid expected memory location (argmem, inaccessiblemem, errnomem) or access kind (none, read, write, readwrite)
; rule: llvm/lib/AsmParser/LLParser.cpp:2755 bogus is not a location keyword.
define void @f() memory(bogus: read) { ret void }
