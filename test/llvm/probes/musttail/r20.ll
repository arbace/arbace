define void @f(i32 %x) {
 musttail call void asm sideeffect "", ""()
 ret void
}
