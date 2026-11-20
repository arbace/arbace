; verdict: invalid PHI nodes not grouped at top of basic block!
; rule: llvm/lib/IR/Verifier.cpp:3804 add precedes the phi in block b; visitBasicBlock skips the PHI-count check since front() is not a phi, visitPHINode reports it.
define i32 @f(i1 %c) {
e:
  br i1 %c, label %a, label %b
a:
  br label %b
b:
  %z = add i32 1, 2
  %p = phi i32 [ 1, %a ], [ 2, %e ]
  ret i32 %p
}
