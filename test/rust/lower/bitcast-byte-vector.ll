; Bitcasts from vectors of byte-sized elements (the Mac's review 190, finding 2): <16 x i8> to <2 x i64>, <4 x i8> to i32,
; <2 x i32> to i64, <16 x i8> to i128. Judged against lli (bin/rsi lower-unit).
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-musl"

@fmt = private constant [6 x i8] c"%llx\0A\00"

declare i32 @printf(ptr, ...)

define i32 @main() {
  %a = add <16 x i8> <i8 1, i8 2, i8 3, i8 4, i8 5, i8 6, i8 7, i8 8, i8 9, i8 10, i8 11, i8 12, i8 13, i8 14, i8 15, i8 16>, splat (i8 16)
  %b = bitcast <16 x i8> %a to <2 x i64>
  %b0 = extractelement <2 x i64> %b, i64 0
  %b1 = extractelement <2 x i64> %b, i64 1
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %b0)
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %b1)
  %c = add <4 x i8> <i8 1, i8 2, i8 3, i8 4>, splat (i8 64)
  %ci = bitcast <4 x i8> %c to i32
  %cz = zext i32 %ci to i64
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %cz)
  %d = add <2 x i32> <i32 7, i32 9>, splat (i32 256)
  %di = bitcast <2 x i32> %d to i64
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %di)
  %e = bitcast <16 x i8> %a to i128
  %eh = lshr i128 %e, 64
  %el = trunc i128 %e to i64
  %ehh = trunc i128 %eh to i64
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %el)
  call i32 (ptr, ...) @printf(ptr @fmt, i64 %ehh)
  ret i32 0
}
