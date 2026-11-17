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
