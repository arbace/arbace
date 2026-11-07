define i1 @f(float %c) {
  %x = icmp eq float %c, %c
  ret i1 %x
}
