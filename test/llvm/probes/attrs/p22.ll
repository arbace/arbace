; verdict: invalid byte count specified must be non-zero
; rule: llvm/lib/AsmParser/LLParser.cpp:2616 dereferenceable(0) rejected by parseOptionalAttrBytes.
define void @f(ptr dereferenceable(0) %p) { ret void }
