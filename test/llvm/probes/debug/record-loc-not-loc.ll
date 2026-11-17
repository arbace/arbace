; debug records and DI metadata, written by hand
source_filename = "dbg.c"
target triple = "x86_64-alpine-linux-musl"

define i32 @main() !dbg !5 {
entry:
  %x = alloca i32, align 4
    #dbg_declare(ptr %x, !9, !DIExpression(), !1)
  store i32 3, ptr %x, align 4, !dbg !11
  %v = load i32, ptr %x, align 4, !dbg !12
    #dbg_value(i32 %v, !9, !DIExpression(DW_OP_plus_uconst, 1), !12)
  ret i32 %v, !dbg !12
}

!llvm.dbg.cu = !{!0}
!llvm.module.flags = !{!3, !4}

!0 = distinct !DICompileUnit(language: DW_LANG_C11, file: !1, producer: "hand", isOptimized: false, runtimeVersion: 0, emissionKind: FullDebug)
!1 = !DIFile(filename: "dbg.c", directory: "/tmp")
!3 = !{i32 7, !"Dwarf Version", i32 5}
!4 = !{i32 2, !"Debug Info Version", i32 3}
!5 = distinct !DISubprogram(name: "main", scope: !1, file: !1, line: 1, type: !6, scopeLine: 1, flags: DIFlagPrototyped, spFlags: DISPFlagDefinition, unit: !0)
!6 = !DISubroutineType(types: !7)
!7 = !{!8}
!8 = !DIBasicType(name: "int", size: 32, encoding: DW_ATE_signed)
!9 = !DILocalVariable(name: "x", scope: !5, file: !1, line: 2, type: !8)
!11 = !DILocation(line: 2, column: 7, scope: !5)
!12 = !DILocation(line: 3,column: 10, scope: !5)   ; no space after a comma
