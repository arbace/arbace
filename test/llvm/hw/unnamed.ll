; unnamed values: LLParser numbers an instruction written without `%N =` and a block written without a label
source_filename = "unnamed.ll"
define i32 @u(i32) {
  add i32 %0, 1
  icmp eq i32 %2, 0
  br i1 %3, label %4, label %5
  ret i32 %2          ; block %4, after the terminator, unlabelled
5:
  ret i32 0
}
