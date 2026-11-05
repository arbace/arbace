define i32 @f(i1 %c) {
e:
  br i1 %c, label %a, label %b
a:
  br label %b
b:
  %p = phi i32 [ 1, %a ]
  ret i32 %p
}
