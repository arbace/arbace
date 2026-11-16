# review 1111 (1): an error inside a `.rept` whose body has no backslash, met at the parse (parseDirectiveLoc's unassigned file
# number, at the number's token): each at its copy's line in the `<instantiation>` buffer, followed by printMacroInstantiations'
# note at the `.rept`. The expected diagnostics, rept-notes.err, are derived by hand from LLVM 23.1.5's rules, not recorded.
	.file 1 "a.c"
.rept 2
.loc 3 1
.endr
