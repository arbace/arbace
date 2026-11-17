# review 1111 (6): `.reloc` errors met at the layout (MCAssembler::layout's .reloc offsets), each reported at its offset
# expression's place (O.getLoc()), the next directive evaluated still. The expected diagnostics, reloc-place.err, are
# derived by hand from LLVM 23.1.5's rules, not recorded.
nop
.reloc und, R_X86_64_NONE, 0
.reloc 0, R_X86_64_NONE, 0
.reloc und2, R_X86_64_NONE, 0
