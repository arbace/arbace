declare i32 @llvm.smax.i32(i32, i64)
define i32 @f() {
  %x = call i32 @llvm.smax.i32(i32 1, i64 2)
  ret i32 %x
}
