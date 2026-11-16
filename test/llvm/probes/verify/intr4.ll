declare i32 @llvm.smax.i64(i32, i32)
define i32 @f() {
  %x = call i32 @llvm.smax.i64(i32 1, i32 2)
  ret i32 %x
}
