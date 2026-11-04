declare ptr @g()
define ptr @f() {
 %r = musttail call ptr @g()
 ret ptr %r
}
