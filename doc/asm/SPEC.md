# Assembly forms: the spec (draft, AX1)

Status: draft of AX1 (doc/asm/AMD64-SURVEY.md §4.2, §12), 2026-11-09/10. It settles the forms' frame both assembly crops share
(`:asm-amd64` and `:asm-arm64`, the user's decision of 2026-11-09: AXS1, ARS1/SH1) and the amd64 part AX1 reads: GNU as's
directive language and AT&T syntax. What AX1 leaves to later milestones is marked (AX2: the instruction table and the machine level;
AX3: Intel syntax; AX9: Plan 9). The namespaces are `arbace.repr.asm.*` (`arbace/repr/asm{.ae,/}`), the command `bin/repr-asm`.

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

The header is `asm/unit`, not the survey's `asm/file`: `.file` is a directive, `(asm/file ...)` its item (AXS18). The items are not
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

`bin/repr-asm read [--full] FILE [-o OUT.ae]`, `print [--full] FILE.ae [-o OUT]`, `corpus extract|lists`, `roundtrip [--record]
[CORPUS...]`, `divergences`; the Makefile's `repr-asm-*`.

## 8. The judges of AX1 (own verdicts only)

Each file of `test/asm/corpora.edn`'s corpora is read in `:full`; its `.ae` text read back gives the same forms and layer; the
forms printed with the layer give the original byte for byte; the canonical print read again gives the same forms and an empty
layer. A file whose four hold is `:pass` in `test/asm/roundtrip-results-amd64.edn`; every other is in a class of
`test/asm/divergences-read.edn` (`:intel`, `:error-test`, `:error-input`, `:lexer-option`, `:other-arch`, `:not-input`), each
named from the test's own RUN lines (`not llvm-mc`, `-x86-asm-syntax=intel`, a triple, the file given to llvm-mc or not).
split-file tests are read as their parts (`NAME.PART`), as llvm-mc reads them. The negative controls (`test/asm/controls.txt`):
a file's `.ae` printed back must differ from another file's original. No assembler runs; gas and llvm-mc are the oracle tier's,
from AX2 on (the stated encodings and CHECK texts are AX2's own verdicts).

## 9. Later

AX2: the table from LLVM's `.td` (AXS4 (a)), the matcher's acceptance and messages, the machine-level forms; the canonical
print against `MC/X86`'s CHECK lines. AX3: Intel syntax, the cross-syntax judge. arm64 (AR1): the same frame, its operands under
`arbace.repr.asm.arm64`. AX9: Plan 9's `p9/` forms.
