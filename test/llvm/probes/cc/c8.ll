; verdict: out
; rule: - undefined: `cc 99999` breaks Function::setCallingConv's assertion that a calling convention id is below 1024 (llvm/include/llvm/IR/Function.h:278); a release build shifts the id into the 16-bit subclass data, truncating it and setting flag bits outside the convention's field: an assertions build aborts, no verdict of the module's own
define cc 99999 void @f() { ret void }
