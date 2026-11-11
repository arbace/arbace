# review 1111 (1): an error inside a `.rept` whose body has no backslash, met at the layout (X86ELFObjectWriter's checkIs32,
# reported at the fixup's place). LLVM expands the copies into one `<instantiation>` buffer (parseDirectiveRept,
# instantiateMacroLikeBody): each error at its copy's line there, no note (the layout comes after the parse, ActiveMacros
# empty). The expected diagnostics, rept-place.err, are derived by hand from LLVM 23.1.5's rules, not recorded.
.rept 2
movl $puts@PLTOFF, %eax
.endr
