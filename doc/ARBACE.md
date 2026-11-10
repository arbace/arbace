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
  `repr/java/classfile.ae`, `nattr.ae` are Arbace's own), `repr/haskell.ae` and `repr/haskell/`
  (repr/haskell, Haskell's bridge, `arbace.repr.haskell.*`; doc/haskell/SPEC.md, HASKELL-NOTES.md: the
  converter, the printer and the checks are Arbace's own; its native reader, doc/haskell/NATIVE.md,
  ports GHC 9.14.1, https://downloads.haskell.org/ghc/9.14.1/ghc-9.14.1-src.tar.xz (SHA-256
  `2a83779c...101e77`, the corpus's pin): `repr/haskell/lex.ae` ports `compiler/GHC/Parser/Lexer.x` with
  `Lexer/String.x`, `Lexer/Interface.hs`, `String.hs`, `CharClass.hs`; `header.ae` `GHC/Parser/Header.hs` and
  the extension flags of `GHC/Driver/{DynFlags,Flags,Session}.hs`; `rules_module.ae`, `rules_decl.ae`
  and `rules_expr.ae` the actions of `GHC/Parser.y`; `post.ae` and `post_decl.ae`
  `GHC/Parser/PostProcess.hs` with the builders of `GHC/Hs/{Utils,Type}.hs`; `haddock.ae` and
  `haddocklex.ae` `GHC/Parser/PostProcess/Haddock.hs` and `GHC/Parser/HaddockLex.x`; ported by hand on
  2026-11-04 (the tarball names the Alex and Happy sources `*.x.source`, `Parser.y.source`), the files
  carrying GHC's notice; `tables.ae` is generated from the tarball's `Parser.y.source` by
  `happy.ae`, Arbace's own port of Happy's LALR(1) construction, checked equal to the tables of the
  tarball's `Parser.hs` (Happy 1.20.1.1's output); `doc.ae` ports `GHC/Utils/Ppr.hs` and Outputable's
  combinators, `errors.ae` the parser's messages (`GHC/Parser/Errors/Ppr.hs`, `GHC/Types/Hint/Ppr.hs`, the
  codes of `GHC/Types/Error/Codes.hs`, the printing of `GHC/Types/Error.hs`, `GHC/Utils/Error.hs`,
  `GHC/Driver/Errors.hs`, `GHC/Utils/Logger.hs`), `pprhs.ae` the Outputable instances of the parsed tree
  (`GHC/Hs/{Expr,Pat,Type,Lit,Binds,Decls}.hs`, `GHC/Parser/Types.hs`), the same day; `fileargs.ae` the driver's
  header phase (`GHC/Parser/Header.hs`, `GHC/Driver/{CmdLine,Session}.hs`, `GHC/Driver/Pipeline/Execute.hs`'s
  getFileArgs), with `flags.ae`, the names and kinds of `flagsDynamic` as the helper's `flags` verb prints them
  (2026-11-05); `unlit.ae` ports the tarball's `utils/unlit/unlit.c`
  (2026-11-04); `cpp.ae`, GCC's traditional preprocessor as GHC runs it, is a clean-room implementation of
  Arbace's own (2026-11-04): written from the documented behaviour of `-traditional` and judged against Alpine's
  gcc 15.2.0 `cpp -traditional` as a black box (its output and line markers on probe inputs), no GCC code read
  or kept; `ast.ae`, `parse.ae`, `native.ae`, `mutate.ae`, `driver.ae` are Arbace's own;
  `target.ae`, `configure.ae` and `hsc2hs.ae` (the native reads' inputs derived, HFS-I2 (a), 2026-11-08) port hadrian's
  `src/Rules/Generate.hs` and `bindist/Makefile` (the settings file, the package records' patch), `rts/configure.ac`'s
  ghcplatform.h and ghcautoconf.h steps, `rts/gen_event_types.py`, the packages' `configure.ac` defines with
  `aclocal.m4`'s FPTOOLS_CHECK_HTYPE and FP_CHECK_CONST, and `utils/hsc2hs/src/{HSCParser,C,DirectCodegen,UtilsCodegen}.hs`
  from the same tarball; `cabal.ae` ports Cabal-syntax 3.16.0.0's field parser, conditions, free text and
  showInstalledPackageInfo (`libraries/Cabal/Cabal-syntax`, Cabal's BSD 3-Clause License);
  `target.ae`'s `libffi-vars` and `libffi-headers` (HFS-I11 (a), 2026-11-08) port libffi 3.8.0's `configure.ac` and
  `configure.host` substitution rules for ffi.h.in and the ffitarget.h copy (https://github.com/libffi/libffi/releases/download/v3.8.0/libffi-3.8.0.tar.gz,
  SHA-256 `7da3e2d9...0c0db4`, `test/haskell/target/libffi.edn`'s pin; libffi's MIT License);
  `rename.ae`, the renamer of the front end (doc/haskell/FRONTEND-SURVEY.md, HF2), ports `mkOpAppRn`, `mkNegAppRn`,
  `mkConOpPatRn`, `mkHsOpTyRn` of `compiler/GHC/Rename/HsType.hs` and `compareFixity` of `compiler/GHC/Types/Fixity.hs`
  from the same tarball on 2026-11-05, reading the rest of `compiler/GHC/Rename/` as the specification, the file
  carrying GHC's notice; the helper's `rn`, `iface` and `tc` verbs, `tools/hsdump/HsRn.hs`, are Arbace's own;
  `typecheck.ae`, the front end's elaborator (HF3's first slice, 2026-11-05), is Arbace's own, written anew with GHC's
  `compiler/GHC/Tc/` and Jones's "Typing Haskell in Haskell" (1999) read as the specification, no code ported (its HF9 additions,
  2026-11-08, likewise: kinds erased from the interfaces' types, GADT constructors and their given equalities after
  `GHC.Tc.TyCl`'s rejigConRes and `GHC.Tc.Solver.Equality`'s canonicalisation, ScopedTypeVariables, the implicit parameters, the
  type applications, the data family instances; doc/haskell/FRONTEND-SURVEY.md, "As built: HF9 (first slice)"; its second
  slice, 2026-11-08, likewise: checking mode and tcApp's argument order, implications' givens and wanteds, pattern synonyms after
  `GHC.Tc.TyCl.PatSyn` and `GHC.Tc.Gen.Pat`'s tcPatSynPat, matchDataToTag and matchWithDict, simplifyTop's implicit-parameter
  defaulting, naughty record selectors; "As built: HF9 (second slice)"); `deriv.ae`'s tables of unlifted comparisons and boxings
  (2026-11-08) are `compiler/GHC/Tc/Deriv/Generate.hs`'s primOrdOps (ordOpTbl), boxConTbl and postfixModTbl, under its notice;
  `famenv.ae`
  (2026-11-08) ports `compiler/GHC/Core/FamInstEnv.hs`'s reduction of a type family application (reduceTyFamApp_maybe,
  chooseBranch, findBranch, apartnessCheck, compatibleBranches, computeAxiomIncomps) with `compiler/GHC/Core/Unify.hs`'s
  tcMatchTys and tcUnifyTysFG from the same tarball, carrying GHC's notice; `libcheck.ae` (HF9's measure) is Arbace's own;
  `desugar.ae` (HF5, the match compiler) and `tostg.ae` (HF6, CorePrep, CoreToStg, Unarise) follow GHC 9.14.1's
  `compiler/GHC/HsToCore/Match*.hs`, `GuardedRHSs.hs`, `ListComp.hs`, `compiler/GHC/CoreToStg/Prep.hs`,
  `CoreToStg.hs`, `compiler/GHC/Stg/Unarise.hs`, `Stg/FVs.hs` from the same tarball (2026-11-05, written after their
  algorithms, carrying GHC's notice), `stgcmp.ae` is Arbace's own; since HF10's first slice (2026-11-08) `tostg.ae` also follows `compiler/GHC/CoreToStg/Prep.hs`'s
  speculative evaluation (mkNonRecFloat, decideFloatInfo, cpeArgArity) with `compiler/GHC/Core/Utils.hs`'s exprOkForSpecEval and
  `compiler/GHC/CoreToStg.hs`'s mkStgAltType, and `desugar.ae` `compiler/GHC/HsToCore/Match.hs`'s tidy_bang_pat and matchBangs and
  `compiler/GHC/Core/Make.hs`'s bindNonRec, from the same tarball; since HF10's second slice (2026-11-09) `core.ae` also follows
  `compiler/GHC/Core/Utils.hs`'s exprType and `Core/DataCon.hs`'s dataConInstArgTys (the binders' types), `simplify.ae`
  `compiler/GHC/HsToCore/Binds.hs`'s makeCorePair (default methods' compulsory unfoldings), `Core/Opt/Simplify/Iteration.hs`'s
  simplStableUnfolding (Note [Eta-expand stable unfoldings]) and prepareRhs, `Core/Utils.hs`'s combineIdenticalAlts,
  `Core/Opt/Simplify/Inline.hs`'s is_more_evald and `Types/Id/Make.hs`'s data constructor wrappers (mkDataConRep's
  unboxers), `constfold.ae` `Core/Opt/ConstantFold.hs`'s litEq, `tostg.ae` `Stg/Unarise.hs`'s unariseFunArgBinder (voidArgId),
  `Core/Opt/Arity.hs`'s mkEtaWW (CorePrep's eta binders) and CorePrep's maybeSaturate, `typecheck.ae` the solver's EvCsPushCall and
  `HsToCore/Binds.hs`'s dsEvBinds (superclass evidence bound once), `deriv.ae` `Tc/Deriv/Generate.hs`'s untag_Expr,
  genPrimOpApp and eq_expr_with_tag_check as written; since HF10-P5, P8 and P10 (2026-11-09) `typecheck.ae` also follows
  `compiler/GHC/HsToCore/Foreign/{Decl,C,Prim,Call}.hs` (dsFImport, dsCImport, dsFCall with mkWrapperName's capi wrappers, dsPrimCall,
  dsFLabel, unboxArg, boxResult, resultWrapper, mk_alt), `tostg.ae` `compiler/GHC/Types/RepType.hs`'s unboxed sums' slots and
  `Stg/Unarise.hs`'s mkUbxSum, unariseSumAlts and mapSumIdBinders, `simplify.ae` `Core/Opt/Simplify/Env.hs`'s lazy getSubst and
  `Core/Opt/Simplify.hs`'s shortOutIndirections for top-level binders, `constfold.ae` ConstantFold's binary rules' two literals, from
  the same tarball; `libcheck.ae`'s HF10 verbs (`stg`, `merge`, `judge-stg`, `record-stg`) are Arbace's own; since HF10-P3
  (2026-11-09) `ownface.ae` writes the front end's own interfaces after `compiler/GHC/Iface/Make.hs`'s mkIface_ and
  `Iface/Tidy.hs`'s addExternal and trimId at -O0, `Types/Id/Make.hs`'s dataConSrcToImplBang, `Builtin/Types.hs`'s wired-in
  definitions and `Builtin/Types/Literals.hs`'s built-in families, from the same tarball, carrying GHC's notice (its judge and
  `libcheck.ae`'s `own-order`, `own`, `rank-own` Arbace's own), and `rename.ae` and `typecheck.ae` take an hs-boot file's
  signatures as its values (`Rename/Module.hs`'s rnSrcDecls, `Tc/Module.hs`'s tcHsBootSigs); `simpleopt.ae` (2026-11-06) ports
  `compiler/GHC/Core/SimpleOpt.hs` with the occurrence information of `Core/Opt/OccurAnal.hs` and the compulsory
  unfoldings of `Types/Id/Make.hs`, `constfold.ae` (2026-11-06) the `Float#`/`Double#` rules of
  `compiler/GHC/Core/Opt/ConstantFold.hs` with `Types/Literal.hs`'s conversions, both from the same tarball and carrying
  GHC's notice; `front.ae`, the front end's driver, is Arbace's own; `simplify.ae`, `occanal.ae` and `core.ae`
  (2026-11-06, the -O0 Simplifier) port `compiler/GHC/Core/Opt/Simplify.hs` with `Simplify/Iteration.hs`, `Utils.hs`,
  `Env.hs`, `Inline.hs`, `compiler/GHC/Core/Opt/OccurAnal.hs`, `Opt/Arity.hs`, `compiler/GHC/Core/Unfold.hs` with
  `Unfold/Make.hs`, `compiler/GHC/Core/Utils.hs` and `SimpleOpt.hs`'s `exprIsConApp_maybe` from the same tarball, carrying
  GHC's notice; `primops.ae` is generated from `compiler/GHC/Builtin/primops.txt.pp` by `tools/hsprimops/primops.awk`;
  `simplcmp.ae`, the simplified Core's judge, is Arbace's own),
  `deriv.ae` (HF4, 2026-11-05) ports the generators of `compiler/GHC/Tc/Deriv/Generate.hs` (Eq, Ord, Enum, Bounded, Ix, Read,
  Show) and `Deriv/Functor.hs` (Functor, Foldable, Traversable) from the same tarball, the file carrying GHC's notice;
  `Deriv/Infer.hs` and `Deriv.hs` read as the specification of the contexts and strategies; outside `arbace/` but recorded
  here with the crop, hsi's `go/arbace/hs/alloclimit.ae` follows `rts/Schedule.c`'s schedulePostRunThread and
  `rts/PrimOps.cmm`'s allocation counter primops, and its driver's `test/haskell/hsi/cmd/rtsflags.ae` ports
  `rts/RtsFlags.c`'s procRtsOpts with `rtsusage.ae` its `usage_text[]` transcribed, from the same tarball, HE9 on
  `haskell-eval9`'s 3f57a1d3, ported onto main 2026-11-08, doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE9 on main"),
  `repr/c/` (repr/c, C's bridge,
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
  from the checkout, not embedded, until the program carries goi), `go/escape.ae` (`arbace.go.escape`: gc's
  escape analysis and inliner over goi's checked trees, for the errorcheck programs' `-m`; doc/go/GOI-SPEC.md,
  "Since v1: gc's escape analysis and inliner"): a port to Arbace, by hand on 2026-11-06, of TamaGo's go1.27.1
  (`VERSION` "go1.27.1", the tree pinned by `seed/goroot.sha256`) `src/cmd/compile/internal/escape/*.go`,
  `inline/inl.go`'s `CanInline`, `hairyVisitor` and `mkinlcall`, `inline/interleaved/interleaved.go`,
  `devirtualize/devirtualize.go`'s `StaticCall`, `ir/scc.go`, `ir/reassignment.go`, `loopvar/loopvar.go`'s
  `ForCapture`, `typecheck/stmt.go`'s `normalizeGoDeferCall`, `walk/expr.go`'s `walkIndex` and `bounded`,
  `noder/writer.go`'s dead code and constant switches, `ir/fmt.go`'s `exprFmt` and `types/fmt.go`'s `tconv`,
  and the amd64 entries of `ssagen/intrinsics.go`'s table (LICENSE.md: Go's BSD 3-Clause License), `python.ae` and `python/` (pyi, the evaluator of
  Python forms, `arbace.python` and `arbace.python.*`; doc/python/PYI-SPEC.md, doc/python/PYTHON-SURVEY.md "As built:
  PY5a"; evaluated from the checkout, not embedded, PYS18 (a)): its scope analysis, `python/scope.ae`, ports CPython
  3.14.10's `Python/symtable.c` and `Python/future.c`'s future parse (the release tarball
  https://www.python.org/ftp/python/3.14.10/Python-3.14.10.tar.xz, SHA-256 `de414a57...b72032`), by hand on
  2026-11-03, the file carrying its notice (LICENSE.md: the PSF License Agreement); the rest is Arbace's own. repr/python
  (`repr/python.ae`, `repr/python/`, `arbace.repr.python.*`; doc/python/SPEC.md): its converter and printer are Arbace's
  own; since PY9a (doc/python/PYTHON-SURVEY.md, "As built: PY9a"; queue 199, 2026-11-03) `repr/python/lex.ae` ports
  CPython 3.14.10's C tokenizer (`Parser/lexer/lexer.c`, `state.c`, `Parser/tokenizer/helpers.c`'s errors and
  `readline_tokenizer.c`, `Python/Python-tokenize.c`'s iterator, `Lib/tokenize.py`'s `detect_encoding`; the same tarball),
  by hand, the file carrying its notice; `repr/python/lexcheck.ae` (its check against CPython's) is Arbace's own. Since
  PY9b ("As built: PY9b"; queue 199b) `repr/python/pegen.ae` ports `Tools/peg_generator/pegen` (the metagrammar as a
  hand-written parser, `grammar.py`, `parser_generator.py`'s analyses, `sccutils.py`, the code shapes of `c_generator.py`
  retargeted to Arbace), `repr/python/peg.ae` ports `Parser/pegen.c`'s runtime and `Parser/action_helpers.c`'s helpers,
  and `repr/python/parser.ae` is generated by the first from `Grammar/python.gram` (its actions translated from C), each
  carrying CPython's notice; since 202c (queue 202, 2026-11-04) `repr/python/ucn.ae` ports `Modules/unicodedata.c`'s
  `_getcode` (the \N{NAME} lookup) over pyi's own copy of the tables, carrying CPython's notice; `repr/python/native.ae` ports `tools/pydump/pydump.py`'s Writer (Arbace's own) and
  `repr/python/parsecheck.ae` is Arbace's own.
  pyi's module `pyexpat` (`go/arbace/pyexpat.ae` and `go/arbace/pyexpat/`, package `arbace/pyexpat`, queue 202e, 2026-11-04) ports the same tarball's
  `Modules/pyexpat.c`, and the Go forms it imports, `go/arbace/expat.ae` and `go/arbace/expat/` (package `arbace/expat`,
  outside `arbace/` but recorded here with the binding), port the tarball's vendored Expat 2.9.0 (`Modules/expat/`:
  `xmlparse.c`, `xmltok.c`, `xmltok_impl.c`, `xmltok_ns.c`, `xmlrole.c`, `xcs.c`, `siphash.h` and their headers; Expat's
  commit `e3ca8454...`, "2.9.0+", as `xmlparse.c`'s first line names it) statement for statement, each file carrying
  Expat's notice (LICENSE.md: Expat's MIT License; the binding under the PSF License Agreement).
- `llvm/` (`arbace.llvm.*`: llvmi, LLVM IR's evaluator, `arbace.llvm.run` and its judge `arbace.llvm.check`, and the C forms of the
  personality-free layer `llvm/rt/none.c.ae`; doc/llvm/LLVM-CROP-SURVEY.md, "As built: llvmi") is Arbace's own.
- `repr/cpp/diag.ae` (`arbace.repr.cpp.diag`, CX12: the C++ front end's diagnostics over the native parser's tree, doc/cpp/CPP-SURVEY.md,
  "As built: CX12 (first slice)") is Arbace's own code whose diagnostics' texts are clang's (`clang/include/clang/Basic/DiagnosticSemaKinds.td`)
  and whose rules follow clang's Sema (`SemaExpr.cpp`'s `DiagnoseEmptyLookup`, `SemaDecl.cpp`'s redeclaration checks), the LLVM Project
  from `llvm-project-23.1.5.src.tar.xz` (the pin of `test/cpp/corpora.edn`), written by hand on 2026-11-08; the file carries clang's notice.
  Its second slice (doc/cpp/CPP-SURVEY.md, "As built: CX12 (second slice)") reads `cpp/target/clang-builtins.edn`, clang's identifier
  table made by `test/cpp/builtins.ae` from the same tarball's `Builtins*.td` and `TokenKinds.def` (CP270 (a); the reader carries clang's
  notice), and its checker's rules in `cpp/sema.ae` (Arbace's own, after the standard's rules) and `cpp/template.ae` (the port of
  SemaTemplateDeduction's MemberPointer case and method qualifiers), on only as a checker (`arbace.cpp.sema/checking?`), 2026-11-08.
  Since CP290-CP294 (doc/cpp/CPP-SURVEY.md, "As built: CP290-CP294") also the rules of GNU attributes clang does not know, from the
  native parser's `:_gattrs` data (strict_flex_array first), Arbace's own, clean-room from the GCC 15.2 manual's descriptions.
  Since its fifth slice (doc/cpp/CPP-SURVEY.md, "As built: CX12 (fifth slice)") the constexpr checks `constexpr-bodies` and
  `constexpr-redecls` after `SemaDeclCXX.cpp`'s CheckConstexprFunctionBody, CheckConstexprDeclStmt, CheckConstexprMissingReturn and
  MergeCXXFunctionDecl (llvm-project-23.1.5), 2026-11-08.
- `cpp/` (`arbace.cpp.*`, the C++ crop's eval: the lowering of LLVM forms to C forms, the code generator and its
  semantic analysis; doc/llvm/LOWER.md, doc/cpp/CODEGEN.md) is Arbace's own but for `cpp/template.ae`
  (`arbace.cpp.template`, templates, CODEGEN.md §G3), whose deduction ports parts of clang's
  `clang/lib/Sema/SemaTemplateDeduction.cpp`, https://github.com/llvm/llvm-project at `llvmorg-23.1.4` (commit
  `7a941dfb2877a0b8cf30323fefa85ad019e0b0f5`), and the template parts of `cpp/abi.ae`'s mangling, which
  reimplement parts of `clang/lib/AST/ItaniumMangle.cpp` at the same commit, by hand on 2026-11-05 (LICENSE.md
  names the functions; the files carry clang's notice), and `cpp/inline.ae` (`arbace.cpp.inline`, CODEGEN.md G5),
  which ports LLVM's AlwaysInliner (`llvm/lib/Transforms/IPO/AlwaysInliner.cpp`, the parts of
  `llvm/lib/Transforms/Utils/InlineFunction.cpp` and `llvm/lib/Analysis/InlineCost.cpp` it uses) at the same commit,
  by hand on 2026-11-06.
  Since CP311 (a) (doc/cpp/CPP-SURVEY.md, "As built: CP311 (a) and the rest of CP313 (a)", 2026-11-09) `cpp/template.ae`'s class template
  argument deduction ports `clang/lib/Sema/SemaTemplateDeductionGuide.cpp` (DeclareImplicitDeductionGuides, transformConstructor,
  buildSimpleDeductionGuide, DeclareAggregateDeductionGuideFromInitList) and `clang/lib/Sema/SemaInit.cpp`'s
  DeduceTemplateSpecializationFromInitializer at `llvmorg-23.1.5` (the pinned tarball).
- `repr/ocaml.ae` and `repr/ocaml/` (repr/ocaml, OCaml's bridge, `arbace.repr.ocaml` and `arbace.repr.ocaml.*`;
  doc/ocaml/SPEC.md, doc/ocaml/OCAML-SURVEY.md; evaluated from the checkout, not embedded, OC30 (a)): its native
  reader (2026-11-04, "As built: the native reader") ports OCaml 5.5.0's parsing/lexer.mll (`repr/ocaml/lex.ae`, with
  stdlib/lexing.ml's position updates and utils/misc.ml's Utf8_lexeme), parsing/docstrings.ml (`docstrings.ae`),
  parsing/ast_helper.ml (`ast.ae`'s constructors), parsing/parser.mly's header and semantic actions
  (`actions.ae`) and parsing/location.ml's report printer with utils/format_doc.ml's two columns and Format's break
  rule (`report.ae`, OC172 (a)), from the release tarball https://caml.inria.fr/pub/distrib/ocaml-5.5/ocaml-5.5.0.tar.xz (SHA-256
  `fcc6ae66...f015ba`), by hand, the files carrying OCaml's notice; `menhir.ae` builds parser.mly's LALR(1) automaton
  as Menhir 20250912 does and writes it as the generated `tables.ae`, `parser.ae` drives it as MenhirLib's engine
  (boot/menhir/menhirLib.ml of the same tarball) does (LICENSE.md: the OCaml Core System; Menhir and MenhirLib); the
  rest (the converter, the printer, the checks, `native.ae`) is Arbace's own.
  ocamllex (2026-11-08, OC186 (a), "As built: ocamllex ported (OC186)"): `repr/ocaml/ocamllex.ae`
  (`arbace.repr.ocaml.ocamllex`) ports the same tarball's lex/ (lexer.mll, parser.mly, cset.ml, table.ml, lexgen.ml, compact.ml,
  common.ml, output.ml, main.ml; outputbis.ml's -ml mode not, Proposed OC191) with stdlib/set.ml's AVL trees, by hand, the file
  carrying OCaml's notice (LICENSE.md: the OCaml Core System); its `check` verb is Arbace's own.
  ocamlyacc (2026-11-09, OC194 (a), "As built: OC194 (a)"): `repr/ocaml/ocamlyacc.ae` (`arbace.repr.ocaml.ocamlyacc`) ports the
  same tarball's yacc/ (main.c, reader.c, symtab.c, error.c, lr0.c, closure.c, warshall.c, lalr.c, mkpar.c, output.c,
  skeleton.c, verbose.c; wstr.c, Windows' wide strings, not), Berkeley yacc as OCaml adapted it, by hand, the file carrying
  their notice (INRIA's, LGPL 2.1 with the linking exception, "Based on public-domain code from Berkeley Yacc"; LICENSE.md:
  the OCaml Core System); its `check` verb is Arbace's own.
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
  lookup_apply and normalize_modtype_path, ctype.ml's update_level scope expansion and eq_package_path, and (TY23,
  TYPER.md §22) the modular explicits' Tfunctor of types.mli, ctype.ml, typetexp.ml, typecore.ml, typedtree.ml and oprint.ml, by hand, the
  files carrying OCaml's notice (LICENSE.md: the OCaml Core System); the driver and its judge are Arbace's own, and so is
  `typer/cmi.ae` (2026-11-07, OC184 (a): a unit's typed signature written as forms and read back, after the role of
  typing/cmi_format.ml's read_cmi, not its code).
- `repr/rust.ae` and `repr/rust/` (repr/rust, Rust's bridge, `arbace.repr.rust` and `arbace.repr.rust.*`;
  doc/rust/SPEC.md; evaluated from the checkout, not embedded): its native reader's lexer and parser,
  `repr/rust/lex.ae`, `parse.ae` and `parse/{expr,pat,item}.ae`, with the meta items, literals and
  identifiers' names of `repr/rust/native.ae`, port rustc 1.99.1, https://github.com/rust-lang/rust at
  `58cf8f9c3` (the sources of Alpine's `rust-src-1.99.1-r0`, pinned by `tools/rsdump/apks.sha256`):
  `compiler/rustc_lexer`, `rustc_parse`'s `lexer/` and `parser/`, parts of `rustc_ast` and `rustc_span`
  and the escapes' checks of the crate rustc-literal-escaper 0.0.8 (LICENSE.md names the files; MIT OR
  Apache-2.0), by hand on 2026-11-04, each file's header naming its sources (doc/rust/NATIVE-READER.md);
  `repr/rust/unicode.ae` is generated (`bin/rust-unicode`, `test/rust/unicode_gen.ae`) from the tables of the
  crates unicode-ident 1.0.24 (`src/tables.rs`: XID_Start, XID_Continue; MIT OR Apache-2.0 and the Unicode
  License V3) and unicode-properties 0.1.4 (`src/tables.rs`: `EMOJI_STATUS`; MIT OR Apache-2.0), both from
  https://crates.io, the versions and checksums of rustc 1.99.1's `Cargo.lock` (SHA-256
  `e6e4313c...47da75` and `7df058c7...f8d71d`, `tools/rsdump/unicode-crates.sha256`), on 2026-11-04
  (LICENSE.md); the rest of `repr/rust/` is Arbace's own.
  `arbace/rust/front/mir.ae` (MIR forms' reader and printer, after `rustc_middle/src/mir/pretty.rs`), `arbace/rust/back/codegen.ae`
  (MIR forms to LLVM forms, after `rustc_codegen_ssa/src/mir/` and `rustc_codegen_llvm/src/intrinsic.rs`), `arbace/rust/back/collect.ae`
  (the collector's traversal, after `rustc_monomorphize/src/collector.rs`) and `tools/rsdump/mirdata.rs` (the helper's `mir`, `census` and
  `iface` modes, walking rustc_middle's `Body` as `pretty.rs` does) port rustc 1.99.1, the same revision and sources as above, by
  hand on 2026-11-05 (RF1-RF3, doc/rust/FRONTEND-SURVEY.md, "As built: RF1-RF3 (trial)"; LICENSE.md names the files; MIT OR
  Apache-2.0); evaluated from the checkout, not embedded, out of `gate_key` (RFS13). On 2026-11-08 (RFP23 (b)) `codegen.ae` gained
  `intrinsic.rs`' `volatile_load`/`volatile_store` (and the unaligned ones) and `rustc_codegen_llvm/src/consts.rs`' `codegen_static` for a
  static whose bytes the allocation carries, the same revision.
  `arbace/rust/front/expand.ae` with `expand_format.ae` and `expand_derive.ae` (macro expansion: macro_rules! after
  `rustc_expand/src/mbe/`, the builtins after `rustc_builtin_macros/src/{format,assert,edition_panic,source_util,concat,cfg}.rs` and
  `rustc_parse_format`, the derives after `rustc_builtin_macros/src/deriving/`, cfg after `rustc_expand/src/config.rs`),
  `arbace/rust/front/resolve.ae` (name resolution after `rustc_resolve/src/{build_reduced_graph,imports,ident,late}.rs`) and
  `tools/rsdump/front.rs` (the helper's `expanded`, `resolve` and `std-names` modes) port rustc 1.99.1, the same revision and sources,
  by hand on 2026-11-05 (RF4-RF5, doc/rust/FRONTEND-SURVEY.md, "As built: RF4-RF5 start"; LICENSE.md names the files; MIT OR
  Apache-2.0); evaluated from the checkout, not embedded, out of `gate_key` (RFS13).
  `arbace/rust/front/pp.ae` (the pretty printer engine after `rustc_ast_pretty/src/pp.rs` and `pp/ring.rs`, the comments after
  `rustc_ast_pretty/src/pprust/state.rs`'s `gather_comments`), `arbace/rust/front/lower.ae` (AST to HIR after
  `rustc_ast_lowering/src/{lib,item,expr,pat,block,format}.rs`) and `arbace/rust/front/hir_pretty.ae` (the HIR printer after
  `rustc_hir_pretty/src/lib.rs`, with `pprust/state.rs`'s PrintState, token streams and macro definitions, and
  `rustc_ast/src/util/literal.rs`'s literal display) port rustc 1.99.1, the same revision and sources, by hand on 2026-11-05 (RF6,
  doc/rust/FRONTEND-SURVEY.md, "As built: RF6"; LICENSE.md names the files; MIT OR Apache-2.0); evaluated from the checkout, not
  embedded, out of `gate_key` (RFS13).
  `arbace/rust/front/collect.ae` (collection: the def collector after `rustc_resolve/src/def_collector.rs`, generics, types,
  signatures and clauses after `rustc_hir_analysis/src/{collect/{generics_of,resolve_bound_vars,type_of,clauses_of},
  constrained_generic_params,hir_ty_lowering/{mod,bounds,dyn_trait}}.rs`, visible paths after `rustc_metadata`'s visible_parent_map,
  the type printer after `rustc_middle/src/ty/print/{mod,pretty}.rs`) ports rustc 1.99.1, the same revision and sources, by hand on
  2026-11-06 (RF7, doc/rust/FRONTEND-SURVEY.md, "As built: RF7"; LICENSE.md names the files; MIT OR Apache-2.0); evaluated from the
  checkout, not embedded, out of `gate_key` (RFS13).
  `arbace/rust/front/typeck.ae` (type checking's first slice after `rustc_hir_typeck/src/` and `method/`, with the trait solver's
  selection, normalization and pending obligations after `rustc_trait_selection/src/traits/`, over std's interface from the helper's
  `--std-iface`) ports rustc 1.99.1, the same revision and sources, by hand on 2026-11-06 (RF9a, doc/rust/FRONTEND-SURVEY.md, "As built:
  RF9a"; RF9b's second slice, the same day: ambiguity and where-clause candidates, normalization through the parameter environment and
  item bounds, CoerceMany, closure kinds, writeback's fix-ups, "As built: RF9b"; LICENSE.md names the files; MIT OR Apache-2.0);
  evaluated from the checkout, out of `gate_key` (RFS13).
  `arbace/rust/front/mono.ae` (RFP23 (c), 2026-11-09, doc/rust/FRONTEND-SURVEY.md, "As built: RFP23 (c)": the run's facts natively)
  ports rustc 1.99.1, the same revision and sources: `rustc_symbol_mangling/src/{lib,v0}.rs`, `rustc_span/src/def_id.rs`'s
  StableCrateId, `rustc_middle/src/{mono.rs,ty/print/mod.rs,ty/instance.rs,ty/layout.rs}`, `rustc_mir_transform/src/cross_crate_inline.rs`,
  `rustc_metadata/src/rmeta/{mod,encoder}.rs` (the rmeta's header and CrateRoot's first fields), `rustc_monomorphize/src/collector.rs`,
  `rustc_abi/src/{layout.rs,layout/simple.rs,lib.rs,wrapping_range.rs}` and `rustc_ty_utils/src/layout.rs`; with rustc-stable-hash
  0.1.2's `src/{sip128,stable_hasher}.rs` (https://github.com/rust-lang/rustc-stable-hash, as rustc's Cargo.lock pins it); LICENSE.md
  names the files (MIT OR Apache-2.0); `mono_check.ae` is Arbace's own; evaluated from the checkout, out of `gate_key` (RFS13).
  RFP28-RFP41 (2026-11-09, doc/rust/FRONTEND-SURVEY.md, "As built: the rest of RFP28-RFP41") port into `build.ae`, `passes.ae`,
  `collect.ae`, `lower.ae` and `expand.ae`, from rustc 1.99.1, the same revision and sources: `rustc_middle/src/ty/print/pretty.rs`'
  `pretty_print_const_scalar_ptr`, `rustc_mir_build/src/builder/expr/as_rvalue.rs`' closure fake reads, `rustc_middle/src/mir/consts.rs`'
  `span_as_caller_location` with `rustc_span/src/hygiene.rs`' `expansion_cause`, `rustc_mir_transform/src/post_analysis_normalize.rs`
  (opaque types by their definition), `rustc_expand/src/mbe/transcribe.rs`' `mk_delimited` with `rustc_span/src/lib.rs`' `Span::to`;
  LICENSE.md names them (MIT OR Apache-2.0); `ci_batch.ae` (RFP36 (c): the essential step's ci session) is Arbace's own.
  RF11's first slice (2026-11-09, doc/rust/FRONTEND-SURVEY.md, "As built: RF11 (first slice)") ports into `mono.ae`
  `rustc_abi/src/layout.rs`' `layout_of_union` and into `arbace/rust/back/codegen.ae` `rustc_codegen_ssa/src/mir/place.rs`'
  `project_subslice` and `rvalue.rs`' exposed-provenance casts, rustc 1.99.1, the same revision and sources; LICENSE.md names them
  (MIT OR Apache-2.0); `build_run.ae`'s promoted constants' evaluation and std's integer constants are Arbace's own.
  `arbace/rust/front/build.ae` (THIR and MIR building after `rustc_mir_build/src/{thir,builder}/`, the region scope tree after
  `rustc_hir_analysis/src/check/region.rs`) and `build_run.ae` (CleanupPostBorrowck's changes, the layouts of scalars and scalar pairs
  after `rustc_abi`) port rustc 1.99.1, the same revision and sources, by hand on 2026-11-08 (RF10's first slice,
  doc/rust/FRONTEND-SURVEY.md, "As built: RF10 (first slice)"; LICENSE.md names the files; MIT OR Apache-2.0); evaluated from the
  checkout, out of `gate_key` (RFS13). RF10's second slice (2026-11-08, "As built: RF10 (second slice)") adds to `build.ae` the capture
  analysis after `rustc_hir_typeck/src/{upvar,expr_use_visitor}.rs` and `rustc_passes/src/upvars.rs`, closure bodies, overloaded
  operators, derefs and indexing, constants and allocations, canonical user type annotations (with `typeck.ae`'s part, after
  `fn_ctxt/_impl.rs` and `method/confirm.rs`) and lint scopes, the same revision and sources; `capture_check.ae` is Arbace's own.
  RF10's third slice (2026-11-08, "As built: RF10 (third slice)") ports into `build.ae` the rest of the match lowering after
  `rustc_mir_build/src/builder/matches/{mod,match_pair,buckets,test,util}.rs` (or-patterns' subcandidates, guards and their fake
  borrows, range, slice, string and scalar tests, let-else and `if let` after `builder/block.rs` and `lower_let_expr`), the
  patterns of `thir/pattern/{mod,const_to_pat}.rs` (ranges, slices, `enumerate_and_adjust`), `construct_const` and statics'
  references after `builder/mod.rs` and `thir/cx/expr.rs`, functional update and `write_box_via_move` after `builder/expr/into.rs`;
  and `arbace/rust/front/passes.ae`, mir_built's passes after `rustc_mir_transform/src/{lib,lint_and_remove_uninhabited,simplify}.rs`,
  the same revision and sources (LICENSE.md; MIT OR Apache-2.0).
  RF10's fourth slice (2026-11-08, "As built: RF10 (fourth slice)") ports into `passes.ae` the passes from mir_promoted to runtime
  MIR's cleanup after `rustc_mir_transform/src/{promote_consts,cleanup_post_borrowck,remove_noop_landing_pads,deref_separator,
  add_call_guards,post_analysis_normalize,elaborate_drops,elaborate_drop,abort_unwinding_calls,erase_deref_temps,
  elaborate_box_derefs,lower_intrinsics,remove_place_mention,patch}.rs`, `rustc_const_eval/src/check_consts/qualifs.rs`,
  `rustc_mir_dataflow/src/{move_paths/{mod,builder}.rs,un_derefer.rs,drop_flag_effects.rs,impls/initialized.rs,framework/direction.rs}`
  and `rustc_middle/src/ty/layout.rs`' `fn_can_unwind`, the same revision and sources; and into `build.ae` the float printing of
  `rustc_apfloat` 0.2.3 (`src/ieee.rs`, `impl Display for IeeeFloat`; Apache-2.0 WITH LLVM-exception, from the cargo registry's
  `rustc_apfloat-0.2.3+llvm-462a31f5a5ab`) (LICENSE.md).
  `arbace/rust/check.ae`'s own-verdict judge (`own-verdict`, `run-class`, `verdict-met?`) ports rustc 1.99.1's compiletest
  (`src/tools/compiletest/src/runtest/ui.rs`'s run checks, `directives.rs`'s directive lines; the pinned `rustc-1.99.1-src.tar.xz`),
  by hand on 2026-11-07 (rust-90, doc/rust/EVAL.md §13; LICENSE.md; MIT OR Apache-2.0); evaluated from the checkout.
- `repr/asm.ae` and `repr/asm/` (repr/asm, GNU as's text, AT&T syntax for amd64, as assembly forms and back; `arbace.repr.asm`
  and `arbace.repr.asm.*`; doc/asm/SPEC.md, doc/asm/AMD64-SURVEY.md, AX1): written by hand on 2026-11-09/10 after
  llvm-project at `llvmorg-23.1.5` (the release tarball `llvm-project-23.1.5.src.tar.xz`, SHA-256
  `fb50b1eac93ddeb3aa8dab4cfe324dff74f4a9c2bdee6160c9e36188f8d06a6e`, pinned in `test/cpp/corpora.edn`): `lex.ae` takes
  `llvm/lib/MC/MCParser/AsmLexer.cpp`'s token rules for an x86 ELF target (`LexToken`, `LexDigit`, `LexIdentifier`,
  `LexSlash`, `isAtStartOfComment`; `X86MCAsmInfo.cpp`'s comment and separator strings), `gnu.ae` `AsmParser.cpp`'s
  `getGNUBinOpPrecedence` and the shapes of `parseStatement` (labels, assignments, directives, macro instantiation) and
  `parseMacroLikeBody` (a block's body as text, nested blocks counted), `amd64/att.ae` `X86AsmParser.cpp`'s AT&T operand
  grammar (`ParseOperand`, `ParseMemOperand`, the AVX-512 decorations); no code is copied (LICENSE.md); evaluated from
  the checkout.
- `asm/tablegen.ae` and `asm/amd64/` (`arbace.asm.tablegen`, `arbace.asm.amd64.gen`, `.asm`, `.check`; AX2, doc/asm/AMD64.md,
  doc/asm/AMD64-SURVEY.md, "As built: AX2 (first slice)"): written by hand on 2026-11-10 after the same llvm-project tarball:
  `tablegen.ae` TableGen's language (`llvm/lib/TableGen/TGLexer.cpp`, `TGParser.cpp`, `Record.cpp`, `llvm/docs/TableGen/ProgRef.rst`),
  `amd64/gen.ae` `llvm/utils/TableGen/AsmMatcherEmitter.cpp` (`tokenizeAsmString`, `buildRegisterClasses`, `getOperandClass`,
  `ClassInfo::operator<`, `shouldBeMatchedBefore`, `validate`, the mnemonic aliases) with `Common/CodeGenInstruction.cpp`'s
  `FlattenAsmStringVariants` and `Common/CodeGenRegisters.cpp`'s `TopoOrderRC`, `amd64/asm.ae` `X86AsmParser.cpp`
  (`parseInstruction`, `matchAndEmitATTInstruction`, `processInstruction`, `checkTargetMatchPredicate`,
  `CheckBaseRegAndIndexRegAndScale`, `VerifyAndAdjustOperands`), `X86Operand.h`, `X86MCCodeEmitter.cpp`, `X86BaseInfo.h`,
  `X86MCTargetDesc.cpp` and `X86EncodingOptimization.cpp`; no code is copied (LICENSE.md); evaluated from the checkout. Its
  table, `asm/amd64/table.edn` (outside `arbace/`: data), is generated by `gen.ae` from the tarball's X86 `.td` files. AX2's third
  slice ("As built: AX2 (third slice)"): `amd64/print.ae` (`arbace.asm.amd64.print`) after `X86ATTInstPrinter.cpp`,
  `X86InstPrinterCommon.cpp`, `X86MCAsmInfo.cpp`'s specifier names, `llvm/lib/MC/MCInstPrinter.cpp`, `MCExpr.cpp`, `MCSymbol.cpp`
  and `MCAsmInfo.cpp`, and `gen.ae`'s printer fields after `llvm/utils/TableGen/AsmWriterEmitter.cpp` and
  `Common/AsmWriterInst.cpp`; no code is copied. AX4's first slice (2026-11-10, doc/asm/AMD64-SURVEY.md, "As built: AX4 (first
  slice)"): `asm/elf.ae` (`arbace.asm.elf`: the ELF64 writer after `llvm/lib/MC/ELFObjectWriter.cpp`'s ELFWriter and
  `StringTableBuilder.cpp`, and a reader of Arbace's own) and `asm/amd64/obj.ae` (`arbace.asm.amd64.obj`: the object assembler after
  `MCObjectStreamer.cpp`, `MCELFStreamer.cpp`, `MCAssembler.cpp`'s layout and relaxation, `MCDwarf.cpp`'s FrameEmitterImpl,
  `MCParser/ELFAsmParser.cpp`, `MCParser/AsmParser.cpp`'s data and alignment directives, `X86AsmBackend.cpp`,
  `X86ELFObjectWriter.cpp`, `X86EncodingOptimizationForImmediate.def`), written by hand after the same tarball; no code is copied. AX4's
  second slice (2026-11-10, "As built: AX4 (second slice)"): `asm/link.ae` (`arbace.asm.link`: the static linker after llvm-project
  23.1.5's `lld/ELF/`: `Driver.cpp`, `InputFiles.cpp`'s initializeSymbols and COMDAT rule, `SymbolTable.cpp`, `Symbols.cpp`'s resolve and
  getSymVA, `LinkerScript.cpp`'s getOutputSectionName and assignAddresses, `Writer.cpp`'s getSectionRank, createPhdrs,
  fixSectionAlignments, assignFileOffsets and setPhdrs, `OutputSections.cpp`, `SyntheticSections.cpp`'s .eh_frame, .got, .got.plt and
  symbol tables, `InputSection.cpp`'s getRelocTargetVA and getTlsTpOffset, `Relocations.cpp`'s RelocScan::process,
  `Arch/X86_64.cpp`'s relocate, adjustGotPcExpr, relaxGot and relaxTlsIeToLe), written by hand; no code is copied. AX4's third
  slice (2026-11-10, "As built: AX4 (third slice)"): `asm/amd64/decode.ae` (`arbace.asm.amd64.decode`: the decoder after the same
  tarball's `llvm/lib/Target/X86/Disassembler/X86Disassembler.cpp` and `X86DisassemblerDecoder.h` (readPrefixes, readOpcode,
  getInstructionID, readOperands, translateInstruction, getInstruction's flags), `include/llvm/Support/X86DisassemblerDecoderCommon.h`
  (the contexts and their ranks, the encodings and types), and the tables TableGen makes after `llvm/utils/TableGen/X86RecognizableInstr.cpp`
  (insnContext, emitInstructionSpecifier, emitDecodePath, typeFromString and the encodings), `X86DisassemblerTables.cpp` (inheritsFrom,
  outranks, emitContextTable, setTableFields) and `X86ModRMFilters.h`, built from `asm/amd64/table.edn` at their first use;
  `X86MCTargetDesc.cpp`'s needsAddressSizeOverride) and `asm/amd64/decode_check.ae` (`arbace.asm.amd64.decode-check`: the check,
  with llvm-mc's byte stream after `llvm/tools/llvm-mc/Disassembler.cpp` and FileCheck's matching), written by hand; no code is
  copied. Go's `golang.org/x/arch/x86/x86asm` (TamaGo's GOROOT, `src/cmd/vendor/`) was weighed and not used (doc/asm/AMD64-SURVEY.md,
  "As built: AX4 (third slice)").
- `repr/asm/arm64/gnu.ae` and `repr/asm/plan9.ae` (`arbace.repr.asm.arm64.gnu`, `arbace.repr.asm.plan9`; AR1,
  doc/asm/ARM64-SURVEY.md, doc/asm/SPEC.md §10-§11), and the AArch64 dialects of `lex.ae` and `gnu.ae`: written by hand on
  2026-11-10 after the same llvm-project tarball's `llvm/lib/Target/AArch64/AsmParser/AArch64AsmParser.cpp` (the operand
  shapes: registers with their kinds, `#` immediates, `:spec:` specifiers, `[...]` and `!`, `{...}` lists and ranges,
  indexes, shifts and extends, `=` literals) and `MCTargetDesc/AArch64MCAsmInfo.cpp` (ELF's and COFF's `//`, Darwin's `;` and
  `%%`; `AsmLexer.cpp`'s `#` at a statement's start), and after Go 1.27.1's `src/cmd/asm/internal/asm/parse.go` and
  `cmd/asm/internal/lex` (TamaGo's GOROOT, `seed/goroot.sha256`) for Plan 9's operand shapes; no code is copied
  (LICENSE.md); evaluated from the checkout.
- `asm/arm64/gen.ae` and `repr/asm/arm64/lenc.ae` (`arbace.asm.arm64.gen`, `arbace.repr.asm.arm64.lenc`; AR2's second slice,
  2026-11-10, doc/asm/ARM64-SURVEY.md, "As built: AR2 (second slice)"): the generator of `asm/arm64/table.edn` (outside `arbace/`:
  data) from the same llvm-project tarball's `llvm/lib/Target/AArch64/*.td` (read by `asm/tablegen.ae`, to which `!switch`, `!match`
  and `!sort` were added after `llvm/docs/TableGen/ProgRef.rst`), reading `Inst` as `llvm/utils/TableGen/CodeEmitterGen.cpp` does and
  `AsmParser/AArch64AsmParser.cpp`'s showMatchError and ExtensionDefs as text; the matcher and encoder after `AArch64AsmParser.cpp`
  (parseSysAlias, parseSyspAlias, setRequiredFeatureString, MatchAndEmitInstruction's twiddles and messages, validateInstruction's
  MOPS rules), `MCTargetDesc/AArch64MCCodeEmitter.cpp` (the encoder methods), `MCTargetDesc/AArch64InstPrinter.cpp`
  (printLogicalImm, printSIMDType10Operand), `llvm/utils/TableGen/AsmMatcherEmitter.cpp` (MatchInstructionImpl's diagnostics) and
  `llvm/lib/MC/MCSubtargetInfo.cpp` (the features' implications); written by hand; no code is copied (LICENSE.md); evaluated from
  the checkout.
- `repr/asm/arm64/tablegen.ae`, `table.ae`, `bits.ae`, `enc.ae`, `encheck.ae` (`arbace.repr.asm.arm64.*`; AR2, doc/asm/SPEC.md
  §12, doc/asm/ARM64-SURVEY.md "As built: AR2 (first slice)"), 2026-11-10: `table.ae` generated by `tablegen.ae` (`bin/repr-asm
  --arch arm64 table gen|check`) from Go 1.27.1's GOROOT (TamaGo's, `seed/goroot.sha256`): golang.org/x/arch's
  `arm64/arm64asm` as vendored at `src/cmd/vendor/golang.org/x/arch` (`v0.27.1-0.20260521044007-9c1a596a2c97`, `modules.txt`):
  `tables.go`'s `instFormats`, `condition.go`'s conditions (Go expressions translated), `inst.go`'s `sysInstsAttrs`, `inst.json`'s
  `(1)` bits; and `src/cmd/internal/obj/arm64/sysRegEnc.go`'s `SystemReg`; `bits.ae` a port of arm64asm's `condition_util.go`;
  `enc.ae` written by hand as the inverse of arm64asm's `decode.go` (`decodeArg`, `handle_*`), with LLVM's assembler conventions
  where its encodings differ (`AArch64MCCodeEmitter`'s `fixMOVZ`, `AArch64AsmParser`'s `validateInstruction`, MOV's immediate
  preference; llvm-project 23.1.5's tarball above); `encheck.ae` the check over `llvm/test/MC/AArch64`'s stated encodings
  (LICENSE.md, Go's BSD 3-Clause License); evaluated from the checkout.
- `repr/llvm.ae` and `repr/llvm/` (repr/llvm, LLVM IR to LLVM forms and back and its verifier, `arbace.repr.llvm` and
  `arbace.repr.llvm.*`; doc/llvm/SPEC.md, doc/llvm/LLVM-CROP-SURVEY.md): the verifier `repr/llvm/verify.ae` and the
  reader's LLParser errors in `repr/llvm/read.ae` reimplement checks of llvm-project at `llvmorg-23.1.4` (the release
  tarball `llvm-project-23.1.4.src.tar.xz` pinned in `test/cpp/corpora.edn`): `llvm/lib/IR/Verifier.cpp`,
  `llvm/lib/AsmParser/LLParser.cpp` and `LLLexer.cpp`, with the parts of `lib/IR/{AutoUpgrade,Type,Attributes,AsmWriter,DebugInfoMetadata}.cpp`
  their rules call (the functions SPEC §6.2 names), by hand on 2026-11-04 and 2026-11-05 (LL4); `repr/llvm/intr.ae` ports
  `llvm/lib/IR/Intrinsics.cpp`'s IIT decoding, `lookupIntrinsicID`, `matchIntrinsicType`, `isSignatureValid` and
  `getMangledTypeStr` (2026-11-05); `repr/llvm/intr_table.ae` and `di_table.ae` are generated from the same release by
  `bin/llvm-corpus intrinsics` (llvm-tblgen `--dump-json` over `llvm/include/llvm/IR/Intrinsics*.td`) and `bin/llvm-corpus
  di-table` (`LLParser.cpp`'s `VISIT_MD_FIELDS`, `AsmWriter.cpp`'s field order, `BinaryFormat/Dwarf.def` and `Dwarf.h`,
  `IR/DebugInfoFlags.def`); `repr/llvm/expect.ae` (`arbace.repr.llvm.expect`, the mutants' own verdicts, LLS33 (b), 2026-11-08)
  ports LLParser's naming (`PerFunctionState`: `getVal`, `setInstName`, `defineBB`, `checkValueID`, `checkValidVariableType`,
  `finishFunction`) and carries the texts and orders of the LLParser and Verifier rules the mutator's 41 kinds break
  (`LLParser.cpp`, `Verifier.cpp`, `AutoUpgrade.cpp`, `Attributes.cpp`'s `typeIncompatible`, `Attributes.td`'s placements,
  `CallingConv.h`'s `isCallableCC`, `DiagnosticInfo.cpp`'s version message) of llvm-project 23.1.5 (the release tarball
  `llvm-project-23.1.5.src.tar.xz`, `:llvmorg-23.1.5` in `test/cpp/corpora.edn`), each with its file and line; since LLS36 (a)
  (2026-11-09) also `AutoUpgrade.cpp`'s `upgradeIntrinsicFunction1` name rules, `UpgradeIntrinsicFunction` and
  `UpgradeIntrinsicCall`'s cases, `Intrinsics.cpp`'s `remangleIntrinsicFunction`, the Verifier's visitInstruction attachment checks
  (`verifyRangeLikeMetadata`, `visitProfMetadata`, `visitDereferenceableMetadata`, the alias scope lists, TBAA's first), its
  visitIntrinsicCall cases of ptrmask and is.fpclass, its visitDI* field rules and visitFunction's declaration `!dbg` rule, and
  `DebugInfo.cpp`'s StripDebugInfo reachability, of the same release; since LLS38 and LLS39 (2026-11-09) also the Verifier's
  `visitModuleFlags`, `visitModuleFlag` and `visitModuleIdents`, `Module.cpp`'s `isValidModFlagBehavior`, `AutoUpgrade.cpp`'s
  `UpgradeDebugInfo` (its version read and order) and `UpgradeModuleFlags` (the behaviour rewrites), `DebugInfo.cpp`'s
  `StripDebugInfo` (whether it changes a module), visitCallBase's immarg checks with `UpgradeIntrinsicFunction`'s attributes, and
  visitIntrinsicCall's switch case by case (the cases of assume, ucmp/scmp, coro.begin, is.fpclass, init.trampoline,
  stackprotector, localescape, localrecover, eh.exceptioncode/pointer, get.active.lane.mask, get.vector.length, va_start,
  get.dynamic.area.offset, the fixed-point family, lrint/lround, bswap, invariant.start, stepvector, preserve.*.access.index,
  ptrmask, thread.pointer, threadlocal.address, lifetime, sponentry; the rest named), of the same release; since LLS40 and LLS42
  (2026-11-09) also the Verifier's getSubprogram, visitFunction's walk of the locations (VisitDebugLoc) and its `!dbg` attachment
  checks, visit(DbgVariableRecord)'s scope agreement and verifyFnArgs, visitDISubprogram's retained nodes, visitDILocalVariable,
  visitDIDerivedType's set type, visitDICompileUnit's lists, visitGlobalVariable's `!dbg`, LLParser's DICompileUnit language rule and
  instruction `!dbg` rule, and `AutoUpgrade.cpp`'s memory intrinsics with their alignment and lifetime of two parameters
  (upgradeIntrinsicFunction1, UpgradeIntrinsicCall's cases and its default case), into expect.ae and, for the fixes, verify.ae, of the
  same release; the unit tests
  `test/repr/llvm/expect_test.ae` are Arbace's own; LICENSE.md: Apache 2.0 with LLVM Exceptions; the rest of repr/llvm is Arbace's own.
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
whose `doc/ARBACE.md` records ASM's origin (below). Since 2026-11-10 `asm/` holds another thing, the assembly crop's assembler
(`arbace.asm.tablegen`, `arbace.asm.amd64.*`, `arbace.asm.elf`, `arbace.asm.link`, above, under LLVM's licence), nothing of ASM. The namespaces `genclass.ae`,
`core_proxy.ae`, `reflect/java.ae`, `main.ae` and `core/server.ae` took the Go build's variants
then (they were under `lang/go/ns/`, since merged). Since 2026-11-06 (CDR3) `genclass.ae` holds again Clojure's
`generate-class` (clojure/clojure `src/clj/clojure/genclass.clj` at `98d735fab02f`, the same origin
as below), ported to write a Java form in place of ASM's bytecode (doc/VENDOR-NOTES.md, hand change 31).

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

Generated from data (SCS9 (c), 2026-11-08): `repr/scheme/category.ae` (the general categories) and
`repr/scheme/unicode.ae` (the identifier ranges; since SCS13 (b) the whitespace too), since SCS12 (c) `repr/scheme/casefold.ae`
(full case folding), and beside them si's `go/arbace/scm/unicode.ae` (all of these for si's runtime, the simple folding too), are
made by `arbace.repr.scheme.ucd` (`bin/repr-scheme ucd gen`, `check`) from the Unicode Character Database 16.0.0
(https://www.unicode.org/Public/16.0.0/ucd/ `UnicodeData.txt`, `extracted/DerivedGeneralCategory.txt`, `CaseFolding.txt`,
and since SCS16 (b), 2026-11-09, `SpecialCasing.txt`, `DerivedCoreProperties.txt` and `PropList.txt`: si's `(rnrs unicode)`, its
categories, properties and simple and full case mappings, in place of Go's `unicode` tables and Chez's answers; since SCS17 (c) the
printers' escape ranges by doc/scheme/SPEC.md 7's rules; and since SCS21 (b), 2026-11-09, `auxiliary/WordBreakProperty.txt` (112,730 bytes,
SHA-256 `476464e7...03779`, header `# WordBreakProperty-16.0.0.txt`, `# Date: 2024-07-29, 16:27:36 GMT`): UAX #29's Word_Break classes for
si's `string-titlecase`, in place of a hand-written list; since SCS23 (b) and SCS24 (b), 2026-11-09, `emoji/emoji-data.txt` (113,024 bytes,
SHA-256 `f1365a51...2696a`, header `# emoji-data.txt`, `# Date: 2024-05-01, 21:25:24 GMT`): its Extended_Pictographic property for UAX #29's
WB3c, and `auxiliary/WordBreakTest.txt` (298,862 bytes, SHA-256 `ad985d57...38e8`, header `# WordBreakTest-16.0.0.txt`, `# Date: 2024-04-30,
21:48:43 GMT`): UAX #29's conformance cases, read (not copied) by si's unit test `go/arbace/scm/wordbreak_test.ae`; pinned by SHA-256 in
`test/scheme/ucd.edn`); each file says so in its docstring or header.

The converted files keep their Java file's notice as a comment; which license covers which file
is stated only in [LICENSE.md](../LICENSE.md). Details, the renaming rules and how to build and
run it: [VENDOR-NOTES.md](VENDOR-NOTES.md).
