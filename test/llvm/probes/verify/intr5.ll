declare i64 @llvm.smax.i32(i32, i32)
define i64 @f() {
  %x = call i64 @llvm.smax.i32(i32 1, i32 2)
  ret i64 %x
}
