; verdict: invalid Entry block to function must not have predecessors!
; rule: llvm/lib/IR/Verifier.cpp:3221 Parser silently reuses the existing block for the second 'a:' (getBB finds it in the symbol table), so the block has br then ret and branches to itself; visitFunction's entry check comes first.
define void @f() {
a:
  br label %a
a:
  ret void
}
