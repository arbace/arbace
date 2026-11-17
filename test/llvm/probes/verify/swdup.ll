define void @f(i32 %x) {
  switch i32 %x, label %a [ i32 1, label %a
    i32 1, label %a ]
a:
  ret void
}
