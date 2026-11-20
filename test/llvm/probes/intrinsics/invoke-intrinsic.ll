; verdict: invalid Cannot invoke an intrinsic other than donothing, patchpoint, statepoint, coro_resume, coro_destroy, clang.arc.attachedcall or wasm.(re)throw
; rule: llvm/lib/IR/Verifier.cpp:5733 invoke of llvm.trap: visitCallBase ends with visitInstruction, whose operand loop rejects invoking an intrinsic other than the listed ones.
declare void @llvm.trap()
declare i32 @__gxx_personality_v0(...)
define void @f() personality ptr @__gxx_personality_v0 {
  invoke void @llvm.trap() to label %n unwind label %l
n:
  ret void
l:
  %x = landingpad { ptr, i32 } cleanup
  resume { ptr, i32 } %x
}
