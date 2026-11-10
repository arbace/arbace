# Assembly forms: the spec (draft, AX1, AR1)

Status: draft of AX1 (doc/asm/AMD64-SURVEY.md §4.2, §12), 2026-11-09/10. It settles the forms' frame both assembly crops share
(`:asm-amd64` and `:asm-arm64`, the user's decision of 2026-11-09: AXS1, ARS1/SH1) and the amd64 part AX1 reads: GNU as's
directive language and AT&T syntax. What AX1 leaves to later milestones is marked (AX2: the instruction table and the machine level;
AX3: Intel syntax; AX9: Plan 9). AR1 (doc/asm/ARM64-SURVEY.md §12, 2026-11-10) adds arm64's part: GNU syntax in ELF's and Apple's
spellings (§10) and Plan 9's text lexically (§11), on the same shell. The namespaces are `arbace.repr.asm.*` (`arbace/repr/asm{.ae,/}`),
the command `bin/repr-asm`.

## 1. A file

A file's `.ae` text is a header form, then one item a line, then in mode `:full` the layer's line:

```clojure
(asm/unit {:arch :amd64, :syntax :att})
(asm/global expm1l)
(asm/type expm1l (at function))
(asm/label expm1l)
(fldt (mem {:base rsp, :disp 8}))
(fucomip (reg st) (reg st 1))
(jb (mem {:disp (local 1 :f)}))
(asm/label 1)
#_(asm/layer {:at {0 ["" ".global expm1l" 1861780259], ...}, :n 46})
```

The header is `asm/unit`, not the survey's `asm/file`: `.file` is a directive, `(asm/file ...)` its item (AXS18). Its map is the
text's dialect, which reading the `.ae` back and printing it take from it: `{:arch :amd64 :syntax :att}` (AX1), `{:arch :arm64
:syntax :gnu}` (ELF and COFF), `:syntax :apple` (Darwin's spelling: `;` comments), `:syntax :plan9` (Go's assembler), with `:cpp true`
for a text of the C preprocessor (a `.S` file; Plan 9's always: §4's `asm/cpp-line` and `asm/cpp-invoke`). The items are not
nested in the header, so a file is a sequence of top-level forms like repr/llvm's. A text is read as ISO-8859-1 (a character a
byte) and written back the same way.

## 2. The lexer (`arbace.repr.asm.lex`)

GNU as's tokens as LLVM's AsmLexer makes them for an x86 ELF target (llvm-project 23.1.5, `llvm/lib/MC/MCParser/AsmLexer.cpp`):

- a statement ends at a newline, a carriage return (with its newline) or `;`; `#` starts a line comment anywhere outside a
  string (a cpp line marker `# 1 "f.c"` included), and `//` and `/* */` are comments too (`/* */` may span lines);
- identifiers `[A-Za-z_.][A-Za-z0-9_.$?]*`; `.` alone is the location counter; a `.` and digits a float when no identifier
  character follows (`.5`, but `.2byte` an identifier);
- integers: decimal, `0x` hex, `0b` binary, a leading `0` octal, a character `'a` or `'a'` (`'\n'`), the darwin suffixes
  `U`, `L`, `LL`, `UL`, `ULL` skipped; `1f` and `2b` (digits and `f` or `b` before a non-identifier character) a local
  label's reference; floats decimal (`1.5`, `1e10`, `2.`) and hex (`0x1.8p3`);
- strings `"..."` with gas's escapes (`\b \f \n \r \t \" \\`, `\ooo` octal, `\xhh` hex, any other character itself);
- punctuators `, ( ) [ ] { } $ % * + - / : ~ ! = < > | & ^ @ \` and `<< >> <= >= <> == != && || ->`.

The arm64 dialects (AR1, LLVM's `AArch64MCAsmInfo.cpp`): ELF's and COFF's comment string is `//` and `#` is a line comment only at
a statement's start (`AsmLexer::LexToken`: so a `.S`'s cpp lines and `# 1 "f.c"` markers), elsewhere the punctuator of an immediate;
Darwin's is `;` with `%%` the separator. Plan 9's (Go's `cmd/asm/internal/lex`): `//` and `/* */`, `;` the separator, a byte above
127 an identifier's character (Go's `·` U+00B7 and `∕` U+2215 in UTF-8, which the ISO-8859-1 reading sees as two or three
characters, kept as such in the `.ae`), `->` and `@>` punctuators.

A character no token starts with (a byte outside ASCII outside a comment or string, a `\` outside a macro's body) is a token
`:other` that refuses the statement (unless the statement invokes a macro: its arguments are text).

## 3. Expressions (`arbace.repr.asm.gnu`)

| text | form |
|---|---|
| `42`, `0x2a`, `'*'` | `42` (an integer; above 64 bits a BigInt) |
| `foo`, `.Ltmp0`, `$foo` (in an expression) | `foo` (a symbol when a Clojure symbol holds the name, else `(sym "NAME")`) |
| `"a b"` (a quoted name, in an instruction) | `(sym "a b")` |
| `"text"` (in a directive's arguments) | `"text"` |
| `1f`, `2b` | `(local 1 :f)`, `(local 2 :b)` |
| `1.5e3`, `0x1p-2` | `(float "1.5e3")` (the text is the float's) |
| `.` | `(asm/dot)` |
| `foo@PLT`, `(a+b)@GOTPCREL`, `f@8` | `(at foo PLT)`, `(at (+ a b) GOTPCREL)`, `(at f 8)` |
| `@function`, `%function` (in a directive) | `(at function)`, `(pct function)` |
| `%pltpcrel(f)`, `f@AUTH(ia,42,addr)` (arm64's data directives), `$d:` (arm64's mapping symbol) | `(pct pltpcrel f)`, `(at f AUTH [ia 42 addr])`, `(asm/label $d)` |
| Plan 9's `R1->3`, `R1@>3`, `·f`, `f<>`, `f<ABIInternal>` | `(-> R1 3)`, `(at> R1 3)` (the shifts' precedence), `(id "·f")` (its bytes), `(id "f<>")`, `(id "f<ABIInternal>")` (`id` when no symbol holds the name; printed as written) |
| `-4`, `-x`, `+x`, `~x`, `!x` | `-4` (a negative integer), `(- x)`, `(+ x)`, `(bnot x)`, `(lnot x)` |
| `a OP b` | `(OP a b)`, OP one of `\|\| && == != < <= > >= + - \| ornot xor & * / % << >>` (`<>` is `!=`, `^` is `xor`, binary `!` is `ornot`) |

The precedence is LLVM's GNU precedence (AsmParser.cpp's `getGNUBinOpPrecedence`): `||` 1, `&&` 2, comparisons 3, `+ -` 4,
`| ! ^ &` 5, `* / % << >>` 6, every operator left associative; a unary operator binds tighter. The canonical text puts no space
around a binary operator and parentheses only where the precedence needs them.

## 4. Items

| text | form |
|---|---|
| `foo:`, `1:`, `"a b":`, `_f@8:` | `(asm/label foo)`, `(asm/label 1)`, `(asm/label (sym "a b"))`, `(asm/label (sym "_f@8"))` |
| `x = e`, `x == e`, `. = . + 4` | `(asm/= x E)`, `(asm/== x E)`, `(asm/= (asm/dot) (+ (asm/dot) 4))` (the expression read as a directive's: `v = %ecx` is `(pct ecx)`) |
| `.NAME a, b c, , d` | `(asm/NAME A [B C] nil D)`: comma-separated arguments, an empty one `nil`, space-separated expressions within one a vector |
| `.2byte 1` (a name no symbol holds) | `(asm/directive "2byte" 1)` |
| `.ifc a,b` and the other text directives | `(asm/ifc "a,b")`: `.ifc .ifnc .ifb .ifnb .print .symver .secure_log_unique .abort` keep their arguments as text |
| `.macro m a=1` ... `.endm` | `(asm/macro "m a=1" BODY "endm")`: BODY the text between the header's last token and the end directive; also `.rept`/`.rep`, `.irp`, `.irpc`, `.irpw` (ended by `.endr`), each nesting its own kind |
| `m 1, 2` (a macro defined above in the file) | `(asm/invoke m "1, 2")`: the arguments as text |
| `#include "x.h"`, `# define S b` (`:cpp`: a line whose first character but blanks is `#`, `\`-continued lines with it) | `(asm/cpp-line "include \"x.h\"")`: the text after `#`, printed at the line's start |
| `CAS`, `SYMBOL_TYPE(_init, function)` (`:cpp`: a statement that starts with a name the text `#define`s, or with `NAME(` and no blank) | `(asm/cpp-invoke CAS)`, `(asm/cpp-invoke SYMBOL_TYPE "(_init, function)")`: the rest as text |
| `fred .req x0` (arm64) | `(asm/reg-alias fred x0)` |
| an instruction | the architecture's (§5) |

A directive's name is lower case in the form (`.TEXT` is `(asm/text)`; the spelling is the layer's). Macro bodies and invocations
are text because gas expands them as text (`\arg`, `\()`, altmacro's `%expr` and `<str>`); AX2's assembler expands them as LLVM's
AsmParser does (AXS19). `.intel_syntax` switches the reader to Intel syntax, which AX1 refuses (class `:intel`, AX3) until
`.att_syntax`.

## 5. AT&T instructions (`arbace.repr.asm.amd64.att`)

`(MNEMONIC ATTRS? OPERAND ...)`: the mnemonic as written, lower case (`movl`, `vmovaps.s`, `cmovl`); ATTRS a map, only when not
empty, of `:pseudo` (the `{vex3}`, `{disp32}`, ... written before it, as strings) and `:prefix` (the prefixes written before it on
the same statement: `lock`, `rep`, `repz`, `notrack`, `data16`, `rex64`, a segment, ...; `lock/incl` too); the operands in the
**reverse of the text's order**, the SDM's (destination first) for every instruction but the few gas keeps in text order (`enter`,
`bound`, ...), which AX2's table names.

| text | operand |
|---|---|
| `%rax`, `%st`, `%st(1)` | `(reg rax)`, `(reg st)`, `(reg st 1)` |
| `%zmm3 {%k1} {z}` (`{k1}` too) | `(reg zmm3 {:k k1, :z true})` |
| `$e` | `(imm E)` |
| `disp(%base,%index,scale)`, any part left out | `(mem {:disp E, :base B, :index I, :scale E})`, only the parts written (`(%rax,%rbx)` has no `:scale`, `0(%rax)` has `:disp 0`) |
| `%fs:8`, `%es:(%rdi)`, `v_gs:(,v_eax)` | `:seg fs`; a segment or a register named by an equated symbol: `:segsym v_gs`, `:index (asm/eq v_eax)` |
| `(%rax,)`, `(%rax,%rbx,)` | `:comma 1`, `:comma 2` (the empty parts written) |
| `(%rax){1to16}` | `(mem {:base rax, :bcst 16})` |
| `foo`, `1f`, `foo@GOTPCREL(%rip)` (a bare expression, a branch's target too) | `(mem {:disp E})`, ... (LLVM's absolute memory operand; AX2's matcher makes a branch target of it) |
| `*%rax`, `*8(%rax)`, `%cs:*0x10` | `(ind (reg rax))`, `(ind (mem ...))`, `(ind (mem {:seg cs, :disp 16}))` |
| `{rn-sae}`, `{sae}`, `{dfv=of}` | `(rc "rn-sae")` (a standalone operand; after it the comma is optional) |

So the forms of AX1 are the syntax level in the machine's operand order. AX2 makes them the machine level (AXS2 (a), the user's
decision of 2026-11-09): the table resolves the mnemonic to the opcode and its width (`movl` to `(mov {:w 32} ...)`, `cmovl` to a
condition, not a suffix), and the spelling moves into the layer (AXS17).

## 6. Mode :full (`arbace.repr.asm.full`)

The canonical print writes each item on a line of its own, a label at the line's start and every other item after a tab, a
directive's arguments after a tab separated by `, ` (`,` before an empty one), an instruction's mnemonic then a tab then its operands
separated by `, ` (llvm-mc's layout), integers in decimal, names and directives in lower case, strings with `\"`, `\\`, `\n`, `\t`
and `\ooo` for the other bytes outside 32-126; the text ends with a newline. The layer `{:n COUNT :at {K ENTRY} :end TEXT}` keeps,
for each item K that differs, the gap before it (a string: white space, comments, `;`, line markers) or `[GAP TEXT HASH]` (its own
text when the spelling differs, GAP nil when canonical), HASH the canonical text's `String.hashCode`: an item whose form changed
prints canonically. `:end` is the text after the last item when it is not the canonical newline. The layer is the `.ae`'s last line,
`#_(asm/layer {...})`, which a reader skips.

## 7. Commands

`bin/repr-asm [--arch amd64|arm64] read [--syntax att|gnu|apple|plan9] [--full] FILE [-o OUT.ae]`, `print [--full] FILE.ae [-o OUT]`
(the dialect the header's), `corpus extract|lists`, `roundtrip [--record] [CORPUS...]`, `divergences`; the Makefile's `repr-asm-*`
(`ARCH=`, `repr-asm-roundtrip-arm64`, `repr-asm-record-arm64`). Since AX2 (doc/asm/AMD64.md): `table gen|check|oracle` (amd64's
instruction table) and `encode-check [--record] [CORPUS...]` (the stated encodings by their own CHECK lines); the forms stay this
spec's, the machine level a view the matcher makes (AXS26).

## 8. The judges of AX1 and AR1 (own verdicts only)

Each file of `test/asm/corpora.edn`'s corpora of the architecture is read in `:full`; its `.ae` text read back gives the same header,
forms and layer; the forms printed with the layer give the original byte for byte; the canonical print read again gives the same forms
and an empty layer. A file whose four hold is `:pass` in `test/asm/roundtrip-results-ARCH.edn`; every other is in a class of
`test/asm/divergences-read.edn` (amd64: `:intel`, `:error-test`, `:error-input`, `:lexer-option`, `:other-arch`, `:not-input`) or
`test/asm/divergences-read-arm64.edn` (arm64: `:error-test`, `:other-arch`, `:not-input`), each named from the test's own RUN lines
(`not llvm-mc`, `-x86-asm-syntax=intel`, a triple, the file given to llvm-mc or not; for arm64 a file whose triples are all Darwin's is
read in Apple's spelling). split-file tests are read as their parts (`NAME.PART`), as llvm-mc reads them. The negative controls
(`test/asm/controls.txt`, `controls-arm64.txt`): a file's `.ae` printed back must differ from another file's original. No assembler
runs; gas and llvm-mc are the oracle tier's, from AX2 and AR2 on (the stated encodings and CHECK texts are their own verdicts).

## 9. Later

AX2: the table from LLVM's `.td` (AXS4 (a)), the matcher's acceptance and messages, the machine-level forms; the canonical
print against `MC/X86`'s CHECK lines. AX3: Intel syntax, the cross-syntax judge. AR2: arm64's tables (Go's optab and arm64asm,
ARS5 (a)), the aliases resolved, the machine-level forms and LLVM's InstPrinter spelling against `MC/AArch64`'s CHECK lines. AR8 and
AX9: Plan 9 through Go's own assembler (ARS3 (a), AXS15 (a)), its forms bridged to the machine level.

## 10. arm64's GNU syntax (`arbace.repr.asm.arm64.gnu`, AR1)

`(MNEMONIC OPERAND ...)`: the mnemonic as written, lower case (`add`, `b.eq`, `ld1b`, `smstart`); the operands **in the text's order**,
which for A64 is the Arm ARM's (destination first): unlike AT&T, nothing is reversed. AR1 is the syntax level, as AX1's amd64 forms
are (AXS17 (a), ARS17): `mov` is not yet `orr` or `movz`, a bare `4` is not yet an immediate; AR2's table resolves the aliases and the
operand classes.

| text | operand |
|---|---|
| `x0`, `W1`, `sp`, `xzr`, `fp`, `lr`, `q0`, `z31`, `p7`, `pn8`, `za`, `za1h`, `zt0` | `(reg x0)`, `(reg w1)` (lower case; the spelling is the layer's), ... |
| `v0.4s`, `z0.d`, `za0h.s`, `p0.b` | `(reg v0 :4s)`, `(reg z0 :d)`, `(reg za0h :s)`, `(reg p0 :b)` (the kind after the first `.`) |
| `p0/z`, `p0/m` | `(reg p0 {:pred z})`, `(reg p0 {:pred m})` |
| `#4`, `#-1.25`, `#(1<<12)`, `#:lo12:sym` | `(imm 4)`, `(imm (- (float "1.25")))`, `(imm (<< 1 12))`, `(imm (spec lo12 sym))` |
| `:got:sym`, `:abs_g1_nc:x+4` | `(spec got sym)`, `(spec abs_g1_nc (+ x 4))`: a relocation's specifier over the expression after it |
| `=0x1234`, `=sym` | `(lit 4660)`, `(lit sym)` (the literal pool's pseudo operand) |
| `[x0]`, `[x0, #16]`, `[x1, x2, lsl #3]`, `[x0, #1, mul vl]` | `(mem (reg x0))`, `(mem (reg x0) (imm 16))`, `(mem (reg x1) (reg x2) (shift lsl (imm 3)))`, ... |
| `[x0, #16]!`, `x2!` (MOPS) | `(wb (mem (reg x0) (imm 16)))`, `(wb (reg x2))`; `[x0], #16` is a memory operand and an immediate |
| `{v0.4s, v1.4s}`, `{z0.d - z3.d}`, `{za}` | `(rlist (reg v0 :4s) (reg v1 :4s))`, `(rlist (range (reg z0 :d) (reg z3 :d)))`, `(rlist (reg za))` |
| `v0.s[1]`, `{v0.s, v1.s}[1]`, `za0h.s[w12, 0]`, `za.d[w8, 0:1, vgx2]` | `(elt (reg v0 :s) 1)`, `(elt (rlist ...) 1)`, `(elt (reg za0h :s) (reg w12) 0)`, `(elt (reg za :d) (reg w8) (range 0 1) vgx2)` |
| `lsl #12`, `uxtw`, `sxtw #2`, `msl 8`, `mul vl` | `(shift lsl (imm 12))`, `(shift uxtw)`, `(shift sxtw (imm 2))`, `(shift msl 8)`, `(shift mul vl)` |
| `x(tmp0)` (in a `.S`: a cpp macro's call) | `(cpp-call x "tmp0")` |
| `foo`, `1f`, `eq`, `vmalle1is`, `CNTVCT_EL0`, `c7`, `4` | the expression itself: a label, a condition, an operation's or a system register's name (case kept), a bare immediate |

The canonical text is llvm-mc's layout (a tab after the mnemonic, `, ` between operands, `{ a, b }` with blanks inside the braces as
LLVM's InstPrinter writes lists, `{ a - b }` for a range, integers in decimal); AR2 holds it to the `CHECK:` lines. `.arch`,
`.arch_extension`, `.cpu` and `.fpu` keep their arguments as text (`armv8.2-a+crypto` is not an expression).

## 11. Plan 9's syntax, lexically (`arbace.repr.asm.plan9`, AR1)

`(p9/MNEMONIC OPERAND ...)`: the mnemonic as written (Plan 9's are upper case, `MOVD`, `MOVD.W`, `TEXT`, `DATA`, `GLOBL`), the operands
**in the text's order** (Plan 9's, source first), registers as the names they are (`R1`, `V0.S4`, `ZR`, `RSP`): a lexical reading,
which reads and prints back every text Go's assembler takes without knowing what the operands are; AR8 bridges it to the machine level
through Go's own assembler (ARS3 (a)).

| text | operand |
|---|---|
| `R1`, `V0.S4`, `R1<<3`, `R1->3`, `R1.UXTW<<2`, `NOSPLIT\|NOFRAME` | an expression: `R1`, `V0.S4`, `(<< R1 3)`, `(-> R1 3)`, `(<< R1.UXTW 2)`, `(\| NOSPLIT NOFRAME)` |
| `$1`, `$-8`, `$0-24`, `$(3<<20)`, `$"abc"`, `$f(SB)` | `(imm 1)`, `(imm -8)`, `(imm (- 0 24))`, `(imm (addr [(<< 3 20)]))`, `(imm "abc")`, `(imm (addr f [SB]))` |
| `8(RSP)`, `(R1)(R2<<3)`, `·f(SB)`, `(R6, R7)`, `(VL*4)(RSP)` | `(addr 8 [RSP])`, `(addr [R1] [(<< R2 3)])`, `(addr (id "·f") [SB])`, `(addr [R6 R7])`, `(addr [(* VL 4)] [RSP])` |
| `[V0.B16, V1.B16]`, `[Z4.B-Z7.B]` | `(rlist V0.B16 V1.B16)`, `(rlist (- Z4.B Z7.B))` |
| `V0.D[1]`, `x+0(SB)/8`, `*R1` | `(elt V0.D 1)`, `(size (addr (+ x 0) [SB]) 8)`, `(ind R1)` |

A parenthesized group is always a group, never an expression's parentheses, so `$(1)` and `(R1)` print back as written.
