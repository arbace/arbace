; verdict: invalid 'allockind()' can't be both zeroed and uninitialized
; rule: llvm/lib/IR/Verifier.cpp:2470 uninitialized and zeroed together; kind alloc itself is fine.
define void @f() allockind("alloc,uninitialized,zeroed") "alloc-family"="x" { ret void }
