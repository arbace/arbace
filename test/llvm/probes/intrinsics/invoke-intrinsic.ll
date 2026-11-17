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
