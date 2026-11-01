; report 043's F1: clang's size-generic atomic calls (a std::atomic of 3 bytes) are libatomic's __atomic_load/__atomic_store,
; names gcc reads as its own builtins; the lowering gives them a g$ name with the linker's name as an asm label
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
@s3 = global [3 x i8] zeroinitializer, align 1
declare void @__atomic_load(i64, ptr, ptr, i32)
declare void @__atomic_store(i64, ptr, ptr, i32)
declare i8 @__sync_val_compare_and_swap_1(ptr, i8, i8)
declare i32 @__builtin_zz(i32)
define void @f(ptr %o, ptr %v) {
  call void @__atomic_load(i64 noundef 3, ptr noundef nonnull @s3, ptr noundef nonnull %o, i32 noundef 5)
  call void @__atomic_store(i64 noundef 3, ptr noundef nonnull @s3, ptr noundef nonnull %v, i32 noundef 5)
  %z = call i32 @__builtin_zz(i32 1)
  ret void
}
