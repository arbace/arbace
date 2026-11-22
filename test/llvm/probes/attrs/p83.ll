; verdict: invalid 'allockind()' requires exactly one of alloc, realloc, and free
; rule: llvm/lib/IR/Verifier.cpp:2462 alloc and free together: the masked kind is not one of the three.
define void @f() allockind("alloc,free") "alloc-family"="x" { ret void }
