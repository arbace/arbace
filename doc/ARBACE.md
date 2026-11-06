# arbace/

What `arbace/` holds (all `.ae`, Arbace's source extension since 2026-10-11: Clojure plus the
Java forms, which Clojure cannot evaluate; VENDOR-NOTES.md, hand change 19): Arbace's Clojure
code. Three kinds of things live here:

- Arbace's own tools: `classes/` (the Java forms' analysis and the Go build's interpreter of
  them, `arbace.classes.*`; the bytecode back end is on the branch `arbace-for-java-26`), `repr/clojure.ae`
  and `repr/clojure/` (repr/clojure, Clojure source to Arbace's, `arbace.repr.clojure` and
  `arbace.repr.clojure.*`; doc/REPR-CLOJURE.md; c2a, `arbace.c2a.*`, until 2026-10-17), `repr/java.ae`
  and `repr/java/` (repr/java, the Java to Java forms converter, `arbace.repr.java` and
  `arbace.repr.java.*`; its native lexer and parser, `repr/java/lex.ae` and `parse.ae`, are ports of
  jdk26u's javac: https://github.com/openjdk/jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/parser/{UnicodeReader,JavaTokenizer,JavadocTokenizer,Tokens,JavacParser}.java`
  and `tree/{TreeInfo,TreeMaker}.java`, ported by hand on 2026-10-23, the files carrying their
  notice; doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN8"; and `repr/java/tree.ae`'s helpers
  `skip-parens`, `tree-name`, `full-name`, `diamond?` and `class-kind` reimplement, for the native
  trees, `tree/TreeInfo.java`'s `skipParens`, `name`, `fullName` and `isDiamond` and
  `tree/JCTree.java`'s `JCClassDecl.getKind`, at the same commit, by hand on 2026-10-23, the file
  carrying their notice; "As built: JFS66 (b)", "As built: JFS70"; and the native attribution's
  declarations and scope tiers, `repr/java/decls.ae` and `scope.ae`, reimplement for the native trees
  parts of `comp/{Check,Resolve,TypeEnter,MemberEnter,Enter,ConstFold,Annotate,Attr,MatchBindingsComputer,Flow}.java`
  and `code/{Types,Symbol,Flags}.java` at the same commit (the functions each file's header and
  LICENSE.md name), by hand on 2026-10-24, the files carrying their notice; "As built: JN9a"; and its
  members and types tiers, `repr/java/expr.ae` and `types.ae`, with `decls.ae`'s generated members and
  `nattr.ae`'s methods' `isMemberOf`, reimplement parts of `comp/{Attr,Operators,TypeEnter,Resolve}.java`
  and `code/{Types,Symbol,Symtab}.java` at the same commit (LICENSE.md names the functions), by hand on
  2026-10-24, the files carrying their notice; "As built: JN9b"; and its overload tier,
  `repr/java/overload.ae`, with `expr.ae`'s choice and `decls.ae`'s ClassReader method flags, reimplements
  parts of `comp/{Resolve,Attr,ArgumentAttr,Check,TypeEnter}.java`, `code/{Types,Symbol,Scope}.java` and
  `jvm/ClassReader.java` at the same commit (LICENSE.md names the functions), by hand on 2026-10-25, the
  files carrying their notice; "As built: JN9c"; and its inference tier, `repr/java/infer.ae`, with
  `expr.ae`'s poly expressions, lambdas, method references, record patterns and local class names,
  `overload.ae`'s diamonds, method references and functional descriptors and `decls.ae`'s ClassReader
  members, reimplements parts of `comp/{Infer,InferenceContext,DeferredAttr,ArgumentAttr,Attr,Resolve,Check,TypeEnter}.java`,
  `code/{Type,Types,Symbol}.java`, `jvm/ClassReader.java`, `tree/TreeInfo.java` and `util/GraphUtils.java`
  at the same commit (LICENSE.md names the functions), by hand on 2026-10-25, the files carrying their
  notice; "As built: JN9d" (`repr/java/sym.ae`, the native symbols' type, JFS107 (b), is Arbace's own);
  and its read without javac and last declines, in `expr.ae`, `overload.ae`, `decls.ae`, `attr.ae` and
  `model.ae`, reimplement parts of `comp/{Flow,Check,Attr,TypeEnter,MemberEnter}.java` and
  `code/{ClassFinder,Types,Symbol}.java` at the same commit (LICENSE.md names the functions), by hand on
  2026-10-27, the files carrying their notice; "As built: JN9e" (the read without javac itself,
  `repr/java.ae`'s `convert-units-nojavac`, `resolve.ae`'s native class infos and `split.ae`'s summary,
  is Arbace's own);
  `repr/java/classfile.ae`, `nattr.ae` are Arbace's own), `repr/c/` (repr/c, C's bridge,
  `arbace.repr.c.*`; doc/C23-SURVEY.md, doc/c/SPEC.md: its lexer and preprocessor, `repr/c/lex.ae` and
  `pp.ae`, are ports of modernc.org/cc/v4 v4.29.7 as go-whim carries it, https://github.com/arbace/go-whim
  at `80c49bf53cd6340487ac623f3d6d135b77ac1db6`, `crefactor/cc/scanner.go` and `cpp.go` with the parts of
  `etc.go` and `cc.go` they use, ported by hand on 2026-10-28, the files carrying cc's notice, go-whim's
  delta and Arbace's changes marked; "As built: C2"; its parser `repr/c/parse.ae` (C3, 2026-10-29) ports
  `crefactor/cc/parser.go` with go-whim's `c23.go` and the parts of `ast.go`, `ast2.go`, `cc.go` and `etc.go`
  the parser uses, `repr/c/tree.ae` the node table of cc's generated `ast.go`, "As built: C3";
  `repr/c/host.ae`, `ppcheck.ae` and `parsecheck.ae` are Arbace's own;
  the oracle is the helper `tools/ccdump/`, which imports cc; since C3b (2026-10-29, "As built: C3b")
  `repr/c.ae` and `repr/c/convert.ae`, `print.ae`, `layout.ae`, `rtcheck.ae`, `c23check.ae`, the converter, the
  printer and the round trip, whose converting and printing rules port those of go-whim's `crefactor/cemit` and
  `crefactor/clisp` at the same commit (read and ported, nothing copied; EPL-1.0 by CS37 (a)), the rest Arbace's;
  since C4 (2026-10-30, "As built: C4") `repr/c/annot.ae` and the checker over C forms, Arbace's own, cc's checker only its
  oracle, `c/check.ae` (`arbace.c.check`) since C5 (CS90 (a)); since C5 ("As built: C5 (write-ahead)") `c.ae` and `c/`, ci's
  macros, REPL, builder, linker and musl's reading (`arbace.c`, `arbace.c.*`), and since C6 ("As built: C6 (write-ahead)")
  `c/cf2g.ae` (`arbace.c.cf2g`, the AOT translator of node programs into Go forms), Arbace's own, the C side of
  `go/arbace/crt` and `go/arbace/ci`, Arbace's own Go forms), `repr/go.ae` and `repr/go/` (repr/go, Go to Go forms and back,
  `arbace.repr.go` and `arbace.repr.go.*`, with its Go helpers under `tools/godump/` and
  `tools/gocmp/`; doc/go/), `build/go.ae` (the build of a program held as Go forms,
  `arbace.build.go`; doc/go/BUILD.md; until 2026-10-17 j2a, g2a and `arbace.g2a.build`) and
  `j2g/` (the Java forms to Go forms translator of the Go build, `arbace.j2g.*`;
  doc/go/J2G-SPEC.md; `j2g/start.ae`, the classes initialized at start, generated by
  `test/j2g/start_classes.ae`), `c2j/` (c2j, the back end compiling namespaces to Java forms, `arbace.c2j.*`;
  `arbace.aot.*` until 2026-10-17; doc/STANDALONE-NOTES.md, "S6: Arbace compiled to Go"),
  `go/check.ae` and `go/check/` (goi's checker, a type checker of Go forms written anew after
  go/types, `arbace.go.check` and `arbace.go.check.*`; doc/go/GOI-SPEC.md §3, GR2; evaluated
  from the checkout, not embedded, until the program carries goi), `python.ae` and `python/` (pyi, the evaluator of
  Python forms, `arbace.python` and `arbace.python.*`; doc/python/PYI-SPEC.md, doc/python/PYTHON-SURVEY.md "As built:
  PY5a"; evaluated from the checkout, not embedded, PYS18 (a)): its scope analysis, `python/scope.ae`, ports CPython
  3.14.10's `Python/symtable.c` and `Python/future.c`'s future parse (the release tarball
  https://www.python.org/ftp/python/3.14.10/Python-3.14.10.tar.xz, SHA-256 `de414a57...b72032`), by hand on
  2026-11-03, the file carrying its notice (LICENSE.md: the PSF License Agreement); the rest is Arbace's own;
  `repr/ocaml.ae` and `repr/ocaml/` (repr/ocaml, OCaml's bridge, `arbace.repr.ocaml` and `arbace.repr.ocaml.*`;
  doc/ocaml/SPEC.md, doc/ocaml/OCAML-SURVEY.md; evaluated from the checkout, not embedded, OC30 (a)): its native
  reader (2026-11-04, "As built: the native reader") ports OCaml 5.5.0's parsing/lexer.mll (`repr/ocaml/lex.ae`, with
  stdlib/lexing.ml's position updates and utils/misc.ml's Utf8_lexeme), parsing/docstrings.ml (`docstrings.ae`),
  parsing/ast_helper.ml (`ast.ae`'s constructors) and parsing/parser.mly's header and semantic actions
  (`actions.ae`), from the release tarball https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz (SHA-256
  `fcc6ae66...f015ba`), by hand, the files carrying OCaml's notice; `menhir.ae` builds parser.mly's LALR(1) automaton
  as Menhir 20250912 does and writes it as the generated `tables.ae`, `parser.ae` drives it as MenhirLib's engine
  (boot/menhir/menhirLib.ml of the same tarball) does (LICENSE.md: the OCaml Core System; Menhir and MenhirLib); the
  rest (the converter, the printer, the checks, `native.ae`) is Arbace's own.
  Its native front end for oi (2026-11-05, doc/ocaml/TYPER.md), `repr/ocaml/typer.ae` and `repr/ocaml/typer/`
  (`arbace.repr.ocaml.typer.*`), ports from the same tarball typing/'s types.ml, btype.ml, ctype.ml, predef.ml,
  printtyp.ml, env.ml, typetexp.ml, typedecl.ml, datarepr.ml, patterns.ml, parmatch.ml, typecore.ml and typemod.ml's
  structure items, and lambda/'s lambda.ml helpers, switch.ml, printlambda.ml's names, matching.ml, translcore.ml,
  translprim.ml and translmod.ml's bytecode structure, then (T7) simplif.ml with lambda.ml's traversals and (T8)
  typemod.ml, includemod.ml, mtype.ml, subst.ml, env.ml's module components, translmod.ml, tmc.ml, stdlib/format.ml's
  engine and oprint.ml's printers, for the core language and the module system (the slice TYPER.md names), then (T9's
  polymorphic variants, TYPER.md §11) types.ml's rows, btype.ml's row helpers and hash_variant, ctype.ml's
  unify_row, unify_row_field, copy_row, build_subtype and enlarge_type, typetexp.ml's Ptyp_variant, typecore.ml's
  variant expressions and patterns, build_or_pat, finalize_variant and type_coerce, parmatch.ml's full_match,
  close_variant and pressure_variants, out_type.ml's rows and aliases, matching.ml's divide_variant and
  combine_variant, switch.ml's test_sequence, translcore.ml's Texp_variant and translprim.ml's %loc_ primitives, then (T9's
  objects and classes, TYPER.md §13) typeclass.ml (`typer/typeclass.ae`: class_structure, type_object, class_expr,
  class_type, class_declarations, class_type_declarations) with ctype.ml's class signatures, object fields,
  unify_fields, filter_method, limited_generalize, subtype_fields and build_subtype on objects, typetexp.ml's
  Ptyp_object, Ptyp_class and transl_poly_type, typecore.ml's object expressions (type_send, instance variables,
  overrides, Pexp_poly), out_type.ml's and oprint.ml's object types and class declarations, translobj.ml and
  translclass.ml (`typer/translclass.ae`: shared constants, oo_wrap, the object and class initialisers, builtin
  methods, cached classes), translcore.ml's object expressions and fuse_method_arity, translprim.ml's %send primitives
  and simplif.ml's Lifused, then (T10's stdlib blockers, TYPER.md §15) typing/value_rec_check.ml's classify_expression
  and lambda/value_rec_compiler.ml (`typer/valuerec.ae`: recursive values' kinds, sizes, function lifting and
  compile_letrec), parmatch.ml's exhaust and build_other (`typer/parmatch.ae`) and typecore.ml's
  check_counter_example_pat and partial_pred (`typer/typecore.ae`: GADT-aware exhaustiveness), then (T10's rest,
  TYPER.md §20, 2026-11-06) typecore.ml's effect cases, let operators, check_apply_prim_type and constructor
  disambiguation, translcore.ml's transl_handler and transl_letop, matching.ml's for_handler, lambda/debuginfo.ml's
  Scoped_location (`typer/translcore.ae`: %loc_FUNCTION's scopes), typemod.ml's open of a module expression,
  approx_modtype and transl_recmodule_modtypes, translmod.ml's Tstr_open and init_shape's classes, env.ml's
  lookup_apply and normalize_modtype_path, ctype.ml's update_level scope expansion and eq_package_path, by hand, the
  files carrying OCaml's notice (LICENSE.md: the OCaml Core System); the driver and its judge are Arbace's own.
- Arbace's own namespaces of the program, not derived from Clojure: `box.ae` and `box/`
  (`arbace.box`, the store, over its host layer `arbace.box.host`; the box's build driver
  `arbace.box.build`, its plan `arbace.box.plan` (since GP2 also repr/go's native loader's go
  list, doc/go/GO-PARSER-SURVEY.md), the rebuild in the box `arbace.box.rebuild`
  with its printing steps `arbace.box.print`, the symbol table cut `arbace.box.elf`, the
  conversion of `tamago/goroot` in the box `arbace.box.convert` (GP8, doc/go/GO-PARSER-SURVEY.md,
  also `bin/tamago-vendor`'s assembling and merging);
  doc/go/BOX-NOTES.md, S3, S7, S8), `tls.ae` and `tls/` (`arbace.tls`, TLS client connections,
  over `arbace.tls.host`) and `http.ae` (`arbace.http`, a small HTTP/1.1 client; BOX-NOTES.md,
  "TLS (option 2)"), `go.ae` and `go/` (`arbace.go`, Go forms at the REPL, goi; since GR1 the
  namespaces of the table of compiled Go packages, and `arbace.go.table`, the table's generator
  on the tools' host; doc/go/GOI-SPEC.md).
- The vendored Clojure, renamed `clojure.*` → `arbace.*`, everything else: the namespaces
  `arbace.core`, `arbace.main`, `arbace.pprint`, ... (`*.ae`), and the Java classes as class
  forms, one namespace per Java package (`lang.ae`, `java/api.ae`) with one file per
  Java file (`lang/RT.ae`, `lang/Compiler.ae`, ...). Clojure's main class `clojure.main` is
  `arbace.lang.Main` (`lang/Main.ae`). A few files among them are Arbace's own, not derived from
  Clojure, and say so in their header (`lang/KeywordInvokeSite.ae`,
  `lang/ReflectorCallSite.ae`, `lang/GoInterp.ae`: goi's classes, doc/go/GOI-SPEC.md). `lang/go/` holds only the Go build's generated variants of `Compiler`,
  `CompilerOps.ae` (by `test/j2g/eval_ops.ae`) and `CompilerAOTFn.ae` (by `test/c2j/gen_fns.ae`);
  the hand-written variants it held of `RT`, `Main` and `Compiler` were merged into their bases
  (SA150).

ASM (`asm.ae`, `asm/`, Clojure's `clojure.asm` renamed `arbace.asm`), the JVM's bytecode back end
of `lang/Compiler.ae` and of the Java forms compiler (`classes/emit.ae` ...), `lang/Compile.ae`,
`lang/DynamicClassLoader.ae` and `lang/Intrinsics.ae` left main on 2026-10-12 with the JVM build
(doc/STANDALONE-NOTES.md, S4: the JVM removed); they remain on the branch `arbace-for-java-26`,
whose `doc/ARBACE.md` records ASM's origin (below). The namespaces `genclass.ae`,
`core_proxy.ae`, `reflect/java.ae`, `main.ae` and `core/server.ae` took the Go build's variants
then (they were under `lang/go/ns/`, since merged).

Origin of the vendored Clojure: the frozen baseline `clojure/` this repository held until the
freeze (it remains on the branch `arbace-for-java-26`), that is
https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a` with ASM
(https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`, repackaged), with
the two seed fixes recorded in the journal on that branch
([JOURNAL.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
2026-10-06). It was derived on 2026-10-07 by `bin/vendor-arbace` (on that branch; converter
`arbace.j2c`, renaming `arbace.j2c.rename`; since renamed `arbace.j2a` (2026-10-11), then `arbace.repr.java` (2026-10-17), and `arbace.repr.clojure.rename`) and is maintained by hand since.

Also vendored, renamed alike: clojure.spec, the two libraries upstream Clojure at that revision
depends on (`pom.xml`), as `arbace.spec.alpha`, `arbace.spec.gen.alpha`, `arbace.spec.test.alpha`
(`spec/`, from https://github.com/clojure/spec.alpha at `v0.6.249`,
`3d1efb353b8a95c699b4051ba8273568c21f874e`) and `arbace.core.specs.alpha` (`core/specs/alpha.ae`,
from https://github.com/clojure/core.specs.alpha at `v0.6.133-alpha10`,
`75875946b7e827a1cec91ce645d5e1dcfc475e49`), derived on 2026-10-07 by `bin/vendor-spec` (on that branch) and
maintained by hand since; each file names its origin in a comment.

The converted files keep their Java file's notice as a comment; which license covers which file
is stated only in [LICENSE.md](../LICENSE.md). Details, the renaming rules and how to build and
run it: [VENDOR-NOTES.md](VENDOR-NOTES.md).
