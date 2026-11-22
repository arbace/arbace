; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4165 musttail call returning ptr and ret of the call result, prototypes equal.
declare ptr @g()
define ptr @f() {
 %r = musttail call ptr @g()
 ret ptr %r
}
