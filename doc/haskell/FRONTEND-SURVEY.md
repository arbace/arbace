# Haskell without GHC: a survey of the front end (HES21 (b))

Survey of 2026-11-05 (branch `haskell-frontend-survey`, from `haskell-native`), design only. Question: today hsi
(`doc/haskell/HASKELL-EVAL-SURVEY.md`, branch `haskell-he5`) interprets GHC's final STG, dumped by a GHC API driver, so a
Haskell program needs the host's GHC. What is the smallest route from repr/haskell's native parse trees
(`doc/haskell/NATIVE.md`: GHC 9.14.1's `HsModule GhcPs`, no GHC at read time) to STG that hsi already runs, and what
does it cost? HES21 (c) there (an Arbace type checker for Haskell, "years") is re-measured here and found smaller if the
subset is chosen well, and not the same thing as porting GHC.

Sources: the pinned `ghc-9.14.1-src.tar.xz` (SHA-256 `2a83779c...`, `compiler/`, `libraries/`, `nofib/`; the tarball
carries no `testsuite/`), the host's installed GHC 9.14.1 for the probe of §2.3, and the surveys above. Measured by
scripts kept in this worktree's `.tmp/src/` (`files.pl`, `closure.pl`, not committed: scratch). What is from memory (prior
art, licenses, papers) is marked "(memory, verify)".

Contents: Summary · 1 What the front end must produce · 2 Measurements · 3 The pieces · 4 Judging · 5 Prior art ·
6 Routes and tiers · 7 Milestones HF1-HF12 · 8 Risks · Proposed (HFS1-HFS9) · Decided · As built: HF1-HF2 · As built: HF3 (start)
· ... · As built: HF9 (first slice) · As built: HF9 (second slice) · As built: HF10 (first slice) · As built: HF10 (second slice) · As built: HF10-P1, P2, P5, P6, P8, P10 · As built: HF10-P3 (first slice) · As built: HF10-P11 to P14.

## Summary

- **The target is smaller than GHC's Core.** hsi runs untyped, A-normal, unarised STG. Types, kinds, coercions, rewrite
  rules, pragmas and the whole of GHC's simplifier (`Core/Opt`, 43,545 lines) are not needed for a correct run: a probe
  (§2.3) shows GHC's own `-O0` STG for a 14-line program is the naive desugaring, with dictionaries passed and nothing
  specialised. What the front end must still decide *with types*: instance selection (dictionary passing), defaulting,
  deriving, `PrimRep`s of binders (unlifted and unboxed), literals' types, `Typeable`/`HasCallStack` evidence. So a
  **trusting elaborator** (it needs to be right on programs GHC accepts, not to reject the others) is the target, not GHC's
  checker.
- **GHC's front end is 571,824 lines in `compiler/` (820 modules of GHC/), of which the path source to STG** (rename,
  typecheck, deriving, desugar, prep, to STG, unarise) **is about 150,000 lines** before the support data
  (Name/Id/Var/Type/Core data types, FastString, UniqFM, Bag: 60,000 more). A faithful port is out of reach as "smallest";
  a clean elaborator for a measured subset is about 35,000 to 50,000 lines of `.ae`, with GHC's own text ported only
  where the output is checkable text (deriving's generated code, the match compiler, CorePrep, CoreToStg, Unarise: about
  11,000 GHC lines).
- **The subset is not Haskell 2010 alone, if the libraries are compiled from source.** `Prelude`'s transitive closure
  in `base`/`ghc-internal`/`ghc-prim`/`ghc-bignum` is about 266 modules and 98,000 lines, with `MagicHash` in 101,
  `UnboxedTuples` in 74, `PolyKinds` in 33, `DataKinds` in 23, `TypeFamilies` in 17, `GADTs` in 17, `RankNTypes` in 23,
  `ImplicitParams` in 8, `PatternSynonyms` in 7, `GHC.Generics` and `Typeable` machinery all in. The programs that
  matter for the row (nofib: 88% of 657 files use no extension pragma at all; 6% simple extensions; 2% type-level; 4%
  `MagicHash`) are Haskell 2010 plus a handful of common extensions. So there are two tiers: **Tier S**, user programs in
  Haskell 2010 plus the common extensions, **libraries' STG and interfaces taken from the host GHC once, as pinned data**
  (no host GHC at program build time, GHC only at the pin, as the JVM's jar is for the frozen reference): about 65 to 95
  working days. **Tier L**, the libraries compiled from source by Arbace too (type families, GADTs, PolyKinds, levity,
  pattern synonyms, `Generic`, `Typeable`, CPP, hs-boot cycles): about 40 to 70 days more. Neither includes Template
  Haskell, arrows, or `-O1`.
- **Judging is cheap where GHC leaves checkable data**: the renamer's resolved names (a `GhcRn` dump), inferred
  top-level types and instance heads against the installed `.hi` interfaces (the interface level of repr/haskell already
  reads them, 3.7 million lines of `--show-iface` over the corpus), deriving's text against `-ddump-deriv`, desugared Core
  and STG at `-O0` against `-ddump-ds-preopt` and `-ddump-stg-final` after normalisation, and hsi's runs against
  hsi on GHC's STG and against GHC's native runs (the final judge).
- **Recommended** (HFS1-HFS9): the clean trusting elaborator in tiers (HFS1 (b)), the front end's input the `GhcPs` dump
  (HFS2 (a)), interfaces from the host as forms for Tier S (HFS3 (a)), Arbace's own Core without types (HFS4 (a)), faithful
  ports only of deriving, the match compiler, CorePrep, CoreToStg and Unarise (HFS5), `-O0` as the reference and no
  simplifier (HFS6 (a)), the order HF1-HF8 before the type-level tier (HFS7), and a decision on the HES21 (a) route after
  HE5, because it is cheaper in front-end days and different in kind (HFS8).

## 1. What the front end must produce

hsi loads STG forms (`HASKELL-EVAL-SURVEY.md` §4): `(stg/bind ID RHS)`, closures with update flags, `case` with its
alternatives, `let` and `let-no-escape`, constructor and primop applications saturated, each binder's `PrimRep`s and
arity, a per-module table of data constructors (tag, arity, field representations, strictness marks), literals,
foreign calls. It is *final* STG: after CorePrep (ANF, saturation, floating of arguments), CoreToStg (free variables,
update flags, `let-no-escape`, arity), and Unarise (unboxed tuples and sums flattened to their `PrimRep`s; void
arguments dropped, a function whose arguments are all void keeping one). Names are GHC's external names
(`module:occ`), because library STG from GHC (HES6) is linked by name: `$fShowInt`, `$WCon`, `$dmshow`, `$p1Ord`,
class-method selectors, `$trModule`, `$tcT`.

What the front end owes in the other direction (to link with GHC's library STG, Tier S): the dictionary layout of each
class (the `C:Ord` constructor: superclasses first, then methods, strictness marks), the names of the default methods and
selectors, which constructors have wrappers (`$WCon`, for strict fields and unpacking: a wrapper must be called, the
worker assumes evaluated arguments after tag inference), fixities, and an interface per library module with exports,
types, instances and data constructors (§3.2).

## 2. Measurements

### 2.1 GHC's front end by phase (9.14.1, `compiler/GHC/`, lines of `.hs`; `wc -l`)

| phase | directory | lines | what we would need of it |
|---|---|---:|---|
| renamer | `Rename/` (12 modules) | 18,774 | all but `Splice.hs` (1,101): `Env` 2,493, `Names` 2,501, `Expr` 2,911, `Module` 2,877, `HsType` 2,219, `Pat` 1,560, `Bind` 1,547, `Utils` 812, `Unbound` 504, `Fixity` 205 |
| typechecker, expressions | `Tc/Gen/` | 23,584 | `HsType` 4,783, `Splice` 3,123 (TH: no), `App` 2,337, `Pat` 1,951, `Expr` 1,925, `Bind` 1,917, `Sig` 1,692, `Match` 1,311, `Head` 1,224, `Export` 1,153, `Foreign` 790, `Do` 494, `Arrow` 472 (no) |
| typechecker, support | `Tc/Utils/` | 16,442 | `Unify` 4,764, `TcMType` 2,483, `TcType` 2,463, `Monad` 2,461, `Env` 1,256, `Instantiate` 1,183, `Concrete` 809 (`Backpack` 1,023: no) |
| constraint solver | `Tc/Solver/` | 14,899 | `Equality` 3,467, `Monad` 2,458, `Dict` 2,231, `InertSet` 2,142, `Solve` 1,837, `Rewrite` 1,115, `Default` 1,263 |
| declarations | `Tc/TyCl/` + `Tc/TyCl.hs` | 6,174 + 5,922 | kind inference, classes, instances (`Instance` 2,691), pattern synonyms (`PatSyn` 1,263) |
| deriving | `Tc/Deriv/` + `Tc/Deriv.hs` | 8,097 + 2,355 | `Generate` 3,104, `Functor` 1,481, `Utils` 1,279, `Infer` 1,118, `Generics` 1,115 |
| built-in instances | `Tc/Instance/` | 3,759 | `Class` 1,324 (`Coercible`, `KnownNat`, ...), `Family` 944, `Typeable` 789, `FunDeps` 702 |
| types, zonking, errors, top | `Tc/Types/`, `Tc/Zonk/`, `Tc/Errors/`, `Tc/*.hs` | 6,964 + 3,262 + 15,940 + 20,920 | errors (16k) and plugins: not needed; `Module` 3,334, `Validity` 3,025 (a checker's: not needed) |
| desugarer | `HsToCore/` (+ `HsToCore.hs` 787) | 23,969 | `Binds` 1,854, `Match` 1,321 (+ `Match/Constructor` 296, `Match/Literal` 725), `Expr` 1,268, `ListComp` 665, `Utils` 1,012, `Monad` 625, `GuardedRHSs` 162, `Foreign/` (C 598, Call 358, Decl 220); not: `Quote` 3,306 (TH), `Pmc/` 4,5xx (warnings), `Ticks` 1,451, `Arrows` 1,259, `Docs` 652, `Usage` 383 |
| Core: types and data | `Core/` (77 modules) | 91,903 | `Type` 3,438, `TyCon` 3,188, `Coercion` 2,807, `Core/TyCo/` 6,029, `Utils` 3,066, `DataCon` 2,016, `InstEnv` 1,711, `FamInstEnv` 1,607, `Unify` 2,541, `Make` 1,305, `Lint` 4,017 (no) |
| Core to Core | `Core/Opt/` | 43,545 | not needed: `Simplify/Iteration` 4,827, `OccurAnal` 4,075, `Specialise` 3,736, `ConstantFold` 3,634, `Arity` 3,262, `SpecConstr` 2,946, `DmdAnal` 2,751 ... ; `Core/SimpleOpt.hs` 1,612 is `-O0`'s only optimiser |
| to STG | `CoreToStg/Prep` 2,858 + `CoreToStg.hs` 900 + `AddImplicitBinds` 151; `Stg/` 7,597 | 3,909 + 7,597 | `Prep`, `CoreToStg`, `Stg/Unarise` 1,193, `Stg/Syntax` 945, `Stg/FVs` 292, `Stg/EnforceEpt` 750 (tag inference: an optimisation) |
| names and wired-in | `Types/` 31,816, `Builtin/` 11,818, `Data/` 15,197, `Unit/` 11,024, `Utils/` 11,307 | 81,162 | `Builtin/Names` 2,777, `Types` 2,948, `Types/Prim` 1,440, `Types/Id/Make` 2,495 (wrapper/selector/dictionary constructors); the rest is GHC's own data structures, replaced by Arbace's |
| interfaces | `Iface/` | 19,308 | `Syntax` 3,256, `Type` 2,667, `Tidy` 1,489, `Load` 1,251, `Binary` 749 |
| driver | `Driver/` | 25,055 | module graph and flags: ours, small |

Whole `compiler/GHC/*.hs`: 571,824 lines.

### 2.2 The programs: extensions in use

`LANGUAGE` pragmas counted per file (scripts `files.pl`, `closure.pl`; a pragma inside a comment is counted when
it matches, an error of a few per cent; the packages' `default-extensions` and the language edition (`GHC2021` for
five of the corpus's packages) come on top: `NoImplicitPrelude` is the cabal default of `base` and `ghc-internal`).

| set | files | no pragma at all | simple extensions | type-level | prim (`MagicHash`, unboxed) | CPP |
|---|---:|---:|---:|---:|---:|---:|
| `libraries/` (the 41 packages, less tests, Cabal, Win32) | 1,318 | 750 (57%) | 200 (15%) | 96 (7%) | 268 (20%) | 411 (31%) |
| `nofib/` | 657 | 580 (88%) | 38 (6%) | 15 (2%) | 24 (4%) | 74 |
| `libraries/*/tests/` | 595 | 462 (78%) | 80 | 23 | 14 | 96 |
| `compiler/` (GHC itself, HES21 (a)) | 820 | 315 (38%) | 235 | 222 (27%) | 45 | 101 |

("Simple" = bang patterns, lambda case, tuple sections, records' extensions, scoped type variables, flexible
instances and contexts, multi-parameter classes, deriving extensions, existential quantification; "type-level" =
RankNTypes, TypeFamilies, GADTs, DataKinds, PolyKinds, QuantifiedConstraints, role annotations, standalone kinds.)

Per extension, files in `libraries/` (base, ghc-internal, ghc-prim and the rest): CPP 411, MagicHash 219, UnboxedTuples
120, BangPatterns about 200, ScopedTypeVariables 92, RankNTypes 66, PolyKinds 57, StandaloneDeriving 56, TypeFamilies
43, TypeApplications 40, GeneralizedNewtypeDeriving 38, DataKinds 33, PatternSynonyms 32, GADTs 28, MultiParamTypeClasses
22, OverloadedStrings 22, FunctionalDependencies 11, ImplicitParams 9, QuantifiedConstraints 7, DerivingVia 4,
`TemplateHaskell` 0, `Arrows` 0, `RebindableSyntax` 0, `QuasiQuotes` 4 (the testsuite's tests: TH 9, QQ 7). GHC itself:
TypeFamilies 130, GADTs 89, DataKinds 76, PatternSynonyms 56, DerivingVia 27, TH 3.

Declarations in `libraries/` (lines starting so): `instance` 3,281, `data` 1,147, `deriving instance` 449, `pattern` 324,
`newtype` 283, `class` 244, `foreign import` 1,197 (hsi's side), type and data families 48, `{-# RULES` 424 and
`SPECIALISE` 342 (semantic no-ops), `HasCallStack` 285, `deriving ... Generic` 284, `Data` 143, `Lift` 132,
`mdo`/`rec` about 99, implicit parameters 2. Derived classes by clause: Eq 457, Show 382, Ord 290, Generic 240, Lift
219, Data 209, Typeable 97, Read 94, Bounded 91, Generic1 54, Foldable 32, Traversable 31, Enum 30, Functor 15.

`Prelude`'s closure (a Hello program's library): 266 modules, 98,421 lines (plus `GHC.Types` and `GHC.Classes` of
ghc-prim, built in), extensions: NoImplicitPrelude 224, MagicHash 101, CPP 89, UnboxedTuples 74, BangPatterns 64,
ScopedTypeVariables 37, PolyKinds 33, DataKinds 23, RankNTypes 23, DeriveGeneric 21, GND 20, StandaloneDeriving 19,
GADTs 17, TypeFamilies 17, TypeApplications 16, ExistentialQuantification 14, ImplicitParams 8, PatternSynonyms 7,
ViewPatterns 6, UnboxedSums 4, QuantifiedConstraints 2, LinearTypes 1. So every program's libraries need Tier L's
features; a Tier S front end has them only as the *host's output* (interfaces and STG).

### 2.3 Probe: what GHC does at `-O0` and `-O1` to a small program

`P.hs` (14 lines: a tree type deriving `Show` and `Eq`, an overloaded insertion with guards and an as-pattern, a list
comprehension, a `do`), compiled by the host's GHC 9.14.1 with `-ddump-ds-preopt -ddump-ds -ddump-prep -ddump-stg-final
-dsuppress-all -dsuppress-uniques`:

| stage | `-O0` | `-O1` |
|---|---:|---:|
| `-ddump-ds-preopt` (the desugarer's output) | 219 lines, 429 terms, 390 types, 3 coercions | 218 lines |
| `-ddump-ds` (after `simpleOptPgm`) | 171 lines, 318 terms, 269 types, 0 coercions | 167 lines |
| `-ddump-prep` | 244 lines | 355 |
| `-ddump-stg-final` | 271 lines | 378 |

At `-O0` the whole Core-to-Core pipeline is the desugarer's `simpleOptPgm` (trivial-binding inlining, beta, case of known
constructor: `Core/SimpleOpt.hs`, 1,612 lines), and the STG is the naive code: `sum $fFoldableList $fNumInt xs'`,
`even $fIntegralInt`, `enumFromTo $fEnumInt`, the list comprehension a local recursive `ds` over a `case`, constructors
`I#! [10#]`. The desugarer also emits, for a one-line `data T`, the Typeable binds (`$tcT = TyCon <two Word64
fingerprints> $trModule (TrNameS "T"#) 0# krep`, MD5 of the package, module and name: `-dno-typeable-binds` drops them;
a `Show (TypeRep)` needs them to match), `$fEqT = \ @a $dEq -> C:Eq ($c== $dEq) ($c/= $dEq)`, and dictionary bindings
`$dShow = $fShowT $dShow` that `simpleOptPgm` inlines (without it the STG has 42 more `let`s; semantics unchanged). `-O1`
adds worker/wrapper, specialisation and inlining, whose effects are speed and, in imprecise cases, which exception or
trace comes first (`HASKELL-EVAL-SURVEY.md` §1).

## 3. The pieces

For each piece: GHC's modules and size, what a port costs, what a clean reimplementation costs, how it is judged.
Estimates of the clean sizes are in lines of `.ae` (the native reader was 12,000 `.ae` lines beside a 4,300-line table
file for 14,500 lines of GHC plus Happy's construction in one session day; a port has been about 0.8 to 1.0 `.ae` lines
per GHC line for algorithmic code); days are working days of one agent, as the surveys give them, and the type
checker's are the least certain.

### 3.1 Input: the `GhcPs` dump

The parse trees are `HsModule GhcPs` as the helper's dump of mode `:lines` (spans with GHC's buffer positions,
`(L span x)`, nodes, names, operator applications still unresolved, `ECP`/`PatBuilder` already disambiguated by the
reader). Everything the renamer needs is there (SPEC §8.2). The alternative input is repr/haskell's Haskell forms (what
`convert` makes); they are for tools and for print, they drop the structure the renamer needs to match GHC's (fixity
resolution order, source spans for `HasCallStack` strings). Not recommended (HFS2).

Preprocessing is the reader's: CPP is the helper's `cpp` verb today (the host's cpp, for 31% of the libraries' files),
hsc2hs the helper's for 48 modules. A host-free Tier L needs both on Arbace: CPP in traditional mode (repr/c's `pp` is
cc's preprocessor ported; a `-traditional` mode of it is the cheapest route: about 1 to 2 thousand lines, 3 to 5 days),
and hsc2hs's `#{size}`/`#{peek}` templates (it compiles a C program to learn offsets: ci could run it, or the offsets
are tabled per platform: Linux/amd64 and arm64, a risk, §8).

### 3.2 Names, wired-in knowledge, interfaces

GHC: `Builtin/Names` 2,777, `Builtin/Types` 2,948, `Types/Prim` 1,440, `Types/Id/Make` 2,495 (dictionary constructors,
selectors, wrappers, `$W`, record selectors), `Iface/*` 19,308 (binary `.hi`).
Needed: the tuples, unboxed tuples and sums, lists, `->`, `~`, `Coercible`, `TYPE`, the literals' types, the class `Num`
and friends *by name* for literals and defaulting, the primop table (hsi has it from `primops.txt.pp`), and
for every imported module an **interface**: exports, type constructors (arity, constructors, newtype or data, roles
ignored), classes (superclasses, methods and types, fundeps, associated types, default-method names, dictionary
constructor), instances (head, context, dictionary function name, overlap flags, orphans), type family
equations, data constructors (type, field labels, strictness, wrapper, representation), values (type scheme, arity,
details: selector, wrapper, primop), fixities, pattern synonyms, pragma-free. Three sources:
(1) the helper dumps `ModIface` to forms (`--show-iface` text is already compared by the interface level; a forms dump
is a modest GHC API driver: about 600 Haskell lines, 3 to 4 days) — Tier S;
(2) read GHC's binary `.hi` natively: `Iface/Binary` 749, `Iface/Type` 2,667, `Iface/Syntax` 3,256 and the Binary
class and `FastString` table: a format that changes every release, port 5,000 to 7,000 lines, 8 to 12 days: no;
(3) Arbace's own interface forms written by its own compile of a module (Tier L, and for the user's own modules in Tier
S): about 800 lines.
Clean: wired-in tables 1,500 lines, interface forms and environments 1,500, in total 5 to 8 days with (1).
Judged: interface forms against `--show-iface` after normalisation for (3), the name tables by the renamer's check.

### 3.3 Renamer

GHC: `Rename/` 18,774 lines. Scoping of every binder form, import and export lists (hiding, qualified, `as`,
instances flowing in), fixity resolution, dependency analysis of bindings (the SCCs that let-polymorphism needs, and
type declarations', `Rename/Bind`, `GHC.Data.Graph`), record fields (`DuplicateRecordFields`, `NamedFieldPuns`,
`RecordWildCards` expanded to the fields in scope), `ScopedTypeVariables`' binding, class and instance method
binding, `deriving` clauses' names, pattern synonyms, `default` declarations, and the rest of the surface the 3.1
extensions add.
Port: 15,000 `.ae` lines (the TH half out), 12 to 16 days. Clean: 6,000 to 9,000 lines, 7 to 10 days. The clean route is
cheap here because the output is checkable *exactly*.
Judged: the helper's new `rn` mode dumps `HsGroup GhcRn` reduced to the resolved names (each occurrence
span to `module:occ` or a local unique, fixities applied as a tree, the binding groups), compared token for token with
the resolved tree of ours over every corpus module (exact; the 1,226 modules plus nofib). First check with no type
checker at all.

### 3.4 Kinds and types: the elaborating checker

GHC: `Tc/` about 120,000 lines (§2.1); `Errors` and `Validity` and `Splice` and `Arrow` and `Plugin` out, about 85,000
in. The checker is OutsideIn(X) with implication constraints, deferred unification, a lazy `TcM`, knot-tying
declarations (`tcTyClDecls` through `fixM`), and a Core that carries types and coercions all the way.
**What evaluation needs of it**, by the probe and §1: the *elaboration* only — where dictionaries and evidence go —
not the rejection of wrong programs. Hence a trusting checker:

- bidirectional inference with higher-rank types (Peyton Jones, Vytiniotis, Weirich, Shields, "Practical type inference
  for arbitrary-rank types", 2007: about 1,000 lines), `let`-generalisation by dependency groups (without
  `MonoLocalBinds`: Haskell 2010's; with it when GADTs/TypeFamilies are on), class and instance declarations with
  superclasses, default methods, instance contexts, multi-parameter classes, flexible and overlapping instances,
  functional dependencies, defaulting (Haskell 2010 §4.3.4 and the `ExtendedDefaultRules`-free default), overloaded
  literals and strings, `Typeable`/`Coercible`/`KnownNat`/`HasField`/implicit parameters/`HasCallStack` solved built in;
- no kind checking beyond what dictionary and representation choices need (kinds trusted from annotations, `Type` by
  default): *without* `PolyKinds`/`DataKinds` the elaborator needs no kinds at all; `Typeable`'s `krep` data needs them
  for the `$tc` bindings;
- evidence: each wanted becomes a dictionary term built from instance functions and superclass selectors (`EvBinds`
  without the evidence-order machinery), with GHC's names;
- **type-level tier** (Tier L, §6): type families (open, closed, associated, data families) and their reduction in the
  solver (`Tc/Solver/{Equality,Rewrite}` 4,582 and `FamInstEnv` 1,607 in GHC), GADT local equalities and existentials
  (implication constraints, but skolem-escape and ambiguity checks dropped), `DataKinds`/`PolyKinds` kind inference
  (`Tc/Gen/HsType` 4,783, `Tc/TyCl` 12,000: the biggest single piece, trusted where annotations are given: standalone kind
  signatures are in 11 of `Prelude`'s closure), `QuantifiedConstraints`, pattern synonyms (matcher and builder
  functions), `UnboxedTuples`/`UnboxedSums`/levity polymorphism (`($) :: forall r a (b :: TYPE r)`: representations
  need to flow where unarise needs them).

Port (faithful, all but errors): about 85,000 GHC lines; an agent port runs, measured on repr/haskell's, at
10 to 12 thousand lines a day for pure algorithmic code, and far less for `TcM` code that is mutable-reference-heavy and lazy
(`fixM`, `unsafeInterleaveIO`, lazy `Bag`s): 60 to 120 days. Not the smallest route, and its output is no easier to judge
than the clean checker's (the evidence's order differs wherever any detail does).
Clean, Tier S: about 14,000 to 20,000 `.ae` lines, 20 to 30 days. Tier L adds about 9,000 to 14,000 lines, 20 to 35 days.
Judged (§4): inferred types of every top-level binding and every instance head against the interfaces GHC wrote
(alpha-equivalence, constraint order normalised, `forall` order as GHC's ordering rules give it), then runs.

### 3.5 Deriving

GHC: `Tc/Deriv/` 8,097 + `Tc/Deriv.hs` 2,355. GHC *generates source-level bindings* (`GhcPs` trees) for `Eq`, `Ord`,
`Show`, `Read`, `Enum`, `Bounded`, `Ix`, `Functor`, `Foldable`, `Traversable`, `Data`, `Lift`, `Generic`, `Generic1`,
`Typeable`, newtype deriving (by coercion: erased, so a dictionary is reused), `DeriveAnyClass`, `DerivingVia`, and infers the
instance contexts by a fixpoint over the constraints (`Infer` 1,118).
Port: `Generate` 3,104 + `Functor` 1,481 + `Utils` 1,279 + `Infer` 1,118 (inference of contexts needs the solver)
+ `Generics` 1,115: about 7,000 `.ae` lines, 8 to 12 days for all classes, 5 to 7 for the nine classic ones. The output
is text GHC prints (`-ddump-deriv`), so a port is checkable exactly; a clean reimplementation would be shorter and its
output different, so unjudged except by runs (`Show`'s precedences and `Read`'s lexing must be exact: the libraries' and
nofib's `show` outputs are the check). **Port**, the `GhcPs` it generates goes through our renamer and checker as GHC's does.
In the libraries: Eq 457, Show 382, Ord 290, Generic 240, Lift 219, Data 209, Read 94, Bounded 91.

### 3.6 Desugarer

GHC: `HsToCore/` less TH, Pmc, ticks, arrows, docs: about 12,000 lines: `Binds` 1,854 (the evidence and the
`AbsBinds` flattening), `Match` 1,321 + `Constructor` 296 + `Literal` 725 (the pattern-matching compiler, Wadler's chapter in
SPJ's book, with GHC's grouping and its tweaks), `Expr` 1,268, `ListComp` 665, `Utils` 1,012, `Monad` 625,
`GuardedRHSs` 162, `Foreign` (C 598, Call 358, Decl 220), `HsToCore.hs` 787. Covers `do` (with `ApplicativeDo` aside), list
comprehensions (GHC's foldr/build-free `-O0` translation, which the probe shows), records (construction, update, field
selectors, wildcards), guards, view patterns, bang patterns and strict `let`, `where` and local polymorphism with dictionary
abstraction, literals (`fromInteger` at known types folded to `I# 3#`), string literals (`unpackCString#`), `newtype`
patterns, existential dictionaries in constructors, the Typeable binds, pattern synonyms, `foreign import` wrappers.
Port: 12,000 `.ae` lines, 10 to 14 days; **clean**: 5,000 to 7,000 lines, 8 to 12 days (the match compiler has to come out
as GHC's does for the diffs to be small, so a clean desugarer is clean only around it).
Judged: Core at `-ddump-ds-preopt` of GHC (helper `ds` mode, forms) against ours after the **erased normal form** (§4):
types, coercions, casts and ticks dropped, alpha-renamed, trivial atom bindings inlined, independent bindings sorted.

### 3.7 Core, CorePrep, CoreToStg, Unarise

GHC: `CoreToStg/Prep` 2,858 (ANF, saturation of constructors and primops, `$WCon` workers, floating, `-O0` "no `seq` on
unlifted lets" details), `CoreToStg` 900 (free variables, update flags, arity, `let-no-escape`), `Stg/Unarise` 1,193,
`Stg/FVs` 292, `Stg/Syntax` 945, `Core/SimpleOpt` 1,612 (optional), `Core/Tidy` 440 (naming of exported Ids). The types
`Core/Syntax` 1,500 and `Core/Make` 1,305 reduce to Arbace's Core (HFS4). Judged exactly: `-ddump-stg-final` at `-O0`
against ours after renaming, per module, when the desugarer matched; hsi runs after. Port: `Prep`, `CoreToStg`,
`Unarise`, `FVs` with the Core they consume reduced to the untyped one: 5,500 `.ae` lines, 8 to 12 days (the `PrimRep`
of every binder comes from the checker's zonked types: the one coupling between type inference and STG).

### 3.8 Optimisation: what we do not port

`Core/Opt` 43,545 lines: none. `-O0` is the reference (HFS6). A later, optional, measured piece: `SimpleOpt`'s inliner
and case-of-known-constructor, a few hundred lines clean, for the 20 to 30% fewer terms of the probe and for hsi's speed.
A user's program compiled at `-O0` runs slower than the host's `-O1` one and can differ in imprecise-exception and
`trace` order from it (§1 of the eval survey): a recorded class, not a failure.

### 3.9 Template Haskell, arrows

`TemplateHaskell` appears in 0 files of the libraries and 3 of GHC; `TemplateHaskellQuotes` in 19 (for `Lift` and
the `template-haskell` package itself: quotes need `HsToCore/Quote` 3,306, `Rename/Splice` 1,101). Splices need the
front end to *run* Haskell at compile time: hsi inside the front end, over Arbace's own STG of the libraries: only after
Tier L, and for the testsuite's TH tests (9 files in the library tests): out of scope; `Arrows` 0 files; `RebindableSyntax` 0.

## 4. Judging

Three levels, in the cost order of the eval survey's references; each stage has an oracle from the host's GHC at the
pin (never at program build time), cached by key:

| stage | oracle | compare | exact? |
|---|---|---|---|
| reader | the helper's `parse` | trees, as today (`native-check`) | yes, done |
| rename | helper `rn`: resolved names, fixities, binding groups | token for token | yes |
| types | the corpus's `.hi` (`--show-iface`, already compared by repr/haskell's interface level; for Tier S the interface forms of the helper) | top-level types, instance heads, class and data declarations, per module, after alpha and constraint-order normalisation | yes up to the normaliser |
| deriving | `-ddump-deriv` text | the generated bindings | yes (port) |
| desugar | `-ddump-ds-preopt` Core (helper `ds`: forms) | erased normal form (§3.6) | up to the normaliser |
| prep, STG | `-ddump-stg-final` at `-O0` (`stg` mode of HE2 with `-O0`) | renamed, per binding | up to renaming, when ds matched |
| run | hsi on the host's STG; GHC's native run | stdout, stderr, exit status, `trace` order, per program | yes |

The **run** level is the final judge for every piece, as in the eval survey; the stage oracles localise a failure and
make the cheap early milestones possible (the renamer and the types are judged with no STG and no hsi). A stage that
cannot be judged exactly (the evidence's order, dictionary binder names) is judged after a normaliser written once
(`arbace.hs.norm`, about 600 lines: alpha, sorting of independent bindings, inlining of trivial atoms and
`$dX = $fY` bindings, erasure) and the normaliser itself is checked on GHC's own output at `-O0` against
`-O0` with a different unique supply (two GHC runs equal after it).
Negative controls (GTS81 (a)): a module judged against another module's reference at each stage must fail.
Corpora: the 1,226 modules (types and rename, which need no run), nofib's 657 files (about 88% in Tier S's
subset; runs against GHC's), `libraries/*/tests/` (595; 462 with no extension), and the testsuite's `all.T` run tests
(HE3, a separate pin: the tarball carries none); the synthetic corpus of HE3 gets a layer per feature of §2.2.

## 5. Prior art

All from memory, to verify (a fetch of each project's repository would confirm sizes and licenses; none was made):

- **Typing Haskell in Haskell** (Mark P. Jones, 2000): Haskell 98's type inference with type classes, defaulting,
  binding groups, in about 500 lines of Haskell; the clearest specification of what Tier S's checker must compute;
  not a compiler (no elaboration), freely usable. Worth reading as the spec of `HF4`'s core.
- **MicroHs** (Lennart Augustsson; Apache-2.0 (memory, verify)): a Haskell compiler in Haskell, about 15,000 lines in its
  `src/MicroHs/`, `TypeCheck.hs` a few thousand lines: type classes by dictionary passing, multi-parameter classes,
  fundeps, GADT syntax, existentials, higher-rank with annotations, deriving, records, many extensions, its own
  `Prelude`/`base` (not GHC's) and its own back end (combinators, a small C runtime). It shows that a trusting
  dictionary-passing elaborator for most of the *common* extensions is about 5,000 lines. Its libraries are not
  `ghc-internal`, so its output is not hsi's STG nor linkable with GHC's libraries: useful as a design and a source of
  tests for Tier S, not as a part (HFS1 (d)). It supports a subset of type families only, as far as I remember.
- **Hugs 98** (BSD-like, memory, verify) and Mark Jones's THIH: the design Tier S follows (dictionary passing, no
  type-level programming beyond multi-parameter classes with fundeps and rank-2).
- **Frege** (Ingo Wechsung; BSD-3, memory, verify), **UHC/EHC** (Utrecht; BSD-3; the type checker is attribute
  grammars), **nhc98/Yhc**: prior Haskell front ends of other designs; the first's checker is a readable
  small clean one (JVM). **Helium** (Utrecht; GPL: not read for a port) and **jhc** (GPL2: same): clean-room rule.
- **GHC itself** (BSD-3, `LICENSE.md` already lists the reader's ports of it): the only front end whose libraries are
  `ghc-internal`; porting is allowed and recorded (origin, path, revision in `doc/ARBACE.md`, license in `LICENSE.md`).
- Papers (memory, verify): Peterson and Jones, "Implementing type classes" (1993); Jones, "Type classes with
  functional dependencies" (2000); SPJ et al., "Type checking with open type functions" (2008) and "OutsideIn(X)"
  (2011); Eisenberg, "System FC with explicit kind equality" (2013); Wadler's chapter on pattern matching in SPJ's
  "The implementation of functional programming languages" (1987); Marlow and SPJ, "Making a fast curry" (STG).

## 6. Routes and tiers

Four routes to "STG from native parse trees":

- **R1, faithful port of GHC's phases** to Arbace: about 85,000 + 18,000 + 12,000 + 11,000 GHC lines plus the data
  structures; 250 to 320 days; the result is GHC's front end in Arbace. The only route where the stage dumps agree exactly
  by construction. Rejected as the *smallest*; the right shape for deriving, the match compiler, CorePrep, CoreToStg, Unarise.
- **R2, clean trusting elaborator**, GHC's text ported only where it emits checkable text (R1's small parts), types judged
  by the interfaces: the recommendation. Tiers:
  - **Tier S** (HF1-HF8): user programs, Haskell 2010 plus the common extensions of §2.2's "simple" column and rank-N,
    existentials, multi-parameter classes with fundeps, GND, `deriving` of the classic nine, records with their
    extensions, `HasCallStack`/`Typeable` evidence; libraries by the host's STG and interfaces (data, pinned).
  - **Tier L** (HF9-HF11): type families, GADTs, DataKinds, PolyKinds, pattern synonyms, `Generic`/`Data`/`Lift`
    deriving, unboxed tuples and sums with their representations, CPP and hsc2hs, hs-boot cycles: the libraries from source.
- **R3, GHC's own front end as STG on hsi** (HES21 (a)): the `ghc` library compiled by the host's GHC once (as the
  libraries are), run by hsi (or compiled by HE8's AOT) with a driver as `tools/hsdump` is. No front-end days (the days
  go to hsi's completeness: GHC needs the RTS's file, process, `getrusage`, `mmap`, `unsafePerformIO` global state,
  `Foreign`, hundreds of C symbols, and speed: GHC compiled to STG and interpreted is two orders slower than native, so
  AOT is needed, and the 1,000+ modules of `ghc` plus its dependencies (`containers`, `process`, `directory`, `filepath`,
  `ghc-boot`, `ghci` ...) are in the pinned set); the front end is then a *seed*, not derived by hand, and not
  Arbace-readable in the way CLAUDE.md's "derivation done once by hand" wants. Its cost after HE5: 15 to 30 days
  (the GHC API driver, the module graph, the missing RTS pieces, speed) plus HE8; the host's GHC is needed once, at the pin.
- **R4, MicroHs on hsi** (HES21 (b) in the eval survey): its own `base`, so STG is not the judge's and library-linked
  tests do not apply; rejected as the route, kept as prior art.

Tier S's reasoning (smallest): a user's program in Tier S needs a renamer, an elaborating checker for the subset, deriving,
a desugarer, CorePrep, CoreToStg and Unarise, interface forms from the host once, and nothing of the type-level tier.
It runs nofib (88% plain), the libraries' tests (78% plain) and the corpus of `HE3`'s language-core tests that stay in
the subset. What remains host-dependent in Tier S: the libraries' STG and interfaces (pinned data generated by the host's GHC;
a program build needs no GHC, a re-pin does). Tier L removes that last dependency.

## 7. Milestones (HF1-HF12)

Working days; each milestone ends with its check green and its records. Dependencies: HF1-HF3 need no hsi; HF8 needs HE4
(the machine) and the libraries' STG of HE2; the tiers' order is HFS7.

| milestone | content | exit criteria | days |
|---|---|---|---:|
| **HF1** oracles and the normaliser | helper modes `rn`, `iface-forms`, `ds`, `prep` (beside HE2's `stg`, at `-O0`); `arbace.hs.norm`; corpora and the references of §4; the controls | every corpus module dumped at each stage, the normaliser's self-check on two GHC runs | 4-6 |
| **HF2** names and the renamer | wired-in tables, the environments, the `Rename/` surface of Tier S, interface forms read | the 1,226 corpus modules' names and fixities equal GHC's `rn` dump | 12-18 |
| **HF3** types, Tier S | the elaborating checker of §3.4's first list; defaulting; built-in `Typeable`, `HasCallStack`, `Coercible` as erasure; types of top-level binders | every Tier S module's inferred types equal the `.hi`'s; the Haskell 2010 samples elaborate | 20-30 |
| **HF4** deriving | the nine classic classes, GND, standalone, `DeriveAnyClass` as GHC generates | `-ddump-deriv` equal on the corpus's derived instances; `show`/`read` runs | 6-10 |
| **HF5** desugar | the match compiler, binds with evidence, `do`, comprehensions, records, literals, Typeable binds | the Core equal after normalisation on the Tier S corpus, a class per remaining diff | 8-12 |
| **HF6** to STG | CorePrep, CoreToStg, Unarise, the data-constructor table; ABI with the host's library STG (wrappers, dictionary layout, void arguments) | STG equal to `-O0`'s after renaming on the Tier S corpus | 8-12 |
| **HF7** first light | `bin/hsi build FILE.hs --front arbace`: the program's STG linked to the libraries' STG, run | nofib's plain programs and the samples: stdout equal to GHC's, every non-pass classed | 5-8 |
| **HF8** Tier S check | the cells of the row (`read`, `eval`) over the Tier S corpora, controls, essential subset, a gate step with `:parts` | judged by `bin/reap`; the nightly's share | 3-5 |
| **HF9** type-level tier | type families and fundeps, GADTs, DataKinds/PolyKinds kinds, QuantifiedConstraints, pattern synonyms, unboxed tuples and levity, `Generic`/`Data`/`Lift` | the Prelude closure's types equal the `.hi`'s; its modules elaborate | 20-35 |
| **HF10** libraries from source | CPP (traditional) and hsc2hs on Arbace, hs-boot cycles, the module graph, foreign import, `ghc-internal` then `base` ... the 41 packages | the packages' STG at `-O0` equal to the host's `-O0`, then libraries from Arbace run the corpus | 15-30 |
| **HF11** speed | `SimpleOpt`-like inliner, the checker's speed on the compiled build (`ARBACE_AOT=1`) | the 1,226 modules in the nightly's share | 5-10 |
| **HF12** the milestone | the row without host GHC at build time; `bin/gate --full --clean` | the tag | 2-4 |

Totals: **Tier S (HF1-HF8): 66 to 101 days; with Tier L (HF9-HF12): 108 to 180 days.** HF3, HF9 and HF10 are the
long poles and the least certain (HF3 by a factor of 1.5 either way). Sonnet and Opus split (the user's): Opus for HF3
and HF9 (the checker, the solver, GADTs), HF6's ABI, the equality of Core; Sonnet for HF1, the renamer's mechanical
parts, HF4 and HF5's ports from GHC's text, HF8, HF12's records.

## 8. Risks

- **The checker's scope creeps toward GHC's.** Every extension the libraries use pulls in solver features; Tier S
  stops at the boundary by taking the libraries as data. The boundary is a *measured* one (§2.2); drift is watched by a
  corpus of Tier S modules whose extension set is fixed.
- **ABI with GHC's `-O1` library STG** (Tier S only): dictionary layouts and names, wrappers for strict fields (calling
  a worker with an unevaluated argument breaks GHC's tag assumptions: hsi does not use tags, but the desugarer must still
  call `$WCon`), void-argument conventions after unarise, `Typeable` fingerprints (MD5 over package, module and name;
  needed to equal `$tc` bindings), class-method selector names, specialisations (`$s...`) that exist only inside the
  library. Mitigation: HF1 dumps the libraries' interfaces *and* a table of their STG top-level binders' arities and
  representations; HF6 checks every library reference of the corpus against it.
- **Laziness.** GHC's front end relies on lazy knot-tying (`fixM`, lazy interface loading); an Arbace port is strict:
  the clean checker's declarations are processed in dependency order with explicit knots (kind and class environments
  filled in two passes). A reason for R2 over R1.
- **Representation inference** is the one place the type checker must be exact on the *typed* side: a binder of an
  unlifted type has a different `PrimRep` and a wrong guess breaks hsi. Tier L's levity polymorphism and `UnboxedSums`
  sharpen it; Tier S sees them only through library interfaces' types.
- **Speed.** A checker for `ghc-internal` (94,418 lines) running on Arbace: the corpus nights are a budget (GT8). Only the
  compiled build is realistic (`ARBACE_AOT=1`); HF11.
- **CPP and hsc2hs on Arbace** (Tier L): traditional-mode CPP is small; hsc2hs's offsets need a C compiler or a table: ci
  can run `hsc2hs`'s generated C, or the offsets are recorded per platform from the host (data, pinned).
- **The judge's normaliser hides real bugs** if made too liberal: it is checked on GHC's own output, the controls fail
  by design, and the run level is the final word.
- **GHC drift** (9.14.1 is pinned): each stage's tables (wired-in names, deriving text) are per version; a port of
  deriving and CorePrep moves with GHC by diff, the clean checker not at all (HFS1).
- **Sheer length of Tier L** (40 to 70 days beyond Tier S) and of HF9's solver: a stop after Tier S still gives the
  user programs a GHC-free build, with the host GHC at the pin alone; HFS8 (R3) is the fallback for the libraries.

## Proposed (HFS1-HFS9)

For the user's decision; recommendations only.

- **HFS1 The route.**
  - Options: (a) R1, the faithful port of GHC's phases: 250 to 320 days, stage dumps agree by construction, no
    drift to design; (b) R2, the clean trusting elaborator in tiers with faithful ports of the text-emitting parts:
    Tier S 66 to 101 days, Tier L 108 to 180 in all; (c) R3, GHC's front end run on hsi (HES21 (a)): 15 to 30 days after HE5
    plus HE8, a seed not derived by hand; (d) R4, MicroHs on hsi or as a base: its own `base`, not the judge's.
  - *Recommended: (b)*, Tier S first and Tier L after a decision at HF8; (c) is not exclusive and can be measured
    cheaply after HE5 as an *oracle* (GHC's front end on hsi against Arbace's), HFS8.
- **HFS2 The front end's input.**
  - Options: (a) the `GhcPs` dump of the native reader (spans and all; the renamer's natural input); (b) repr/haskell's
    Haskell forms (`convert`'s output); (c) the forms with a new "renamer-ready" layer.
  - *Recommended: (a)*: no loss, the reader's `native-check` already proves it equal to GHC's parse.
- **HFS3 Interfaces of the libraries in Tier S.**
  - Options: (a) the helper dumps each `ModIface` as forms (about 600 Haskell lines, 3 to 4 days), pinned with the
    libraries' STG; (b) a native `.hi` reader (5,000 to 7,000 lines, 8 to 12 days, format changes every release); (c)
    `--show-iface` text parsed (it is lossy for unfoldings and breaks lines by width: the interface level compares it
    by tokens).
  - *Recommended: (a)*; (b) only if Tier S must run with no helper at all, which Tier L's own interfaces will anyway.
- **HFS4 The Core of the front end.**
  - Options: (a) Arbace's own untyped Core (lambda, application, `let`, `letrec`, `case` with its alternatives, literals,
    constructor and primop applications), each binder carrying its `PrimRep` list and its name, types gone at the
    desugarer's input; (b) GHC's typed Core with coercions (`Core/Syntax`, `Type`, `TyCo`, `Coercion`: 18,000 lines more),
    which makes `Lint` and the simplifier possible and the front end 2.5 times larger.
  - *Recommended: (a)*; (b) only with R1.
- **HFS5 What is ported from GHC's text.**
  - Options: (a) deriving's generators (7,000 `.ae` lines), the match compiler (`Match*`: 2,342), CorePrep (2,858),
    CoreToStg (900), Unarise (1,193): the parts whose output is checkable text, 11,000 GHC lines, 12,000 `.ae` lines;
    (b) everything clean; (c) more (`Binds`, `Expr`, `Utils` of the desugarer).
  - *Recommended: (a)*, (c) where HF5's diffs show it.
- **HFS6 The optimisation level.**
  - Options: (a) `-O0` is the reference and the front end has no simplifier; `SimpleOpt`-like inlining later for speed
    (HF11); (b) `-O1`'s STG as the reference (worker/wrapper, specialisation, `Core/Opt` 43,545 lines: no); (c) `-O0`
    reference plus a tiny optional inliner from the start.
  - *Recommended: (a)*; records a class (imprecise exceptions, `trace` order, speed) against the host's `-O1` runs.
- **HFS7 The order.**
  - Options: (a) HF1 and HF2 now (no hsi needed; the renamer is judged exactly against the helper), HF3-HF6 beside HE4, HF7 at HE4's
    first light; (b) HF1-HF8 after HE5 and HE6; (c) Tier L first (the libraries from source) to prove Haskell without GHC for
    the harder code.
  - *Recommended: (a)*: the renamer and the type inference (HF2, HF3) are judged by data and cost no night's
    budget; HE4 sets the order for the rest.
- **HFS8 GHC's front end on hsi as the oracle or the fallback (R3).**
  - Options: (a) after HE5, measure it as an oracle (`ghc` library compiled by the host's GHC, the stage dumps from
    hsi's run of it equal the helper's: a test of hsi and a second oracle) and decide at HF8; (b) adopt it as the
    standalone front end instead of Tier L (HES21 (a)); (c) never.
  - *Recommended: (a)*; the choice between (b) and Tier L is the user's after Tier S exists.
- **HFS9 Reuse and licenses.**
  - Options: (a) ports of GHC's text under BSD-3 recorded as the reader's are (`doc/ARBACE.md`, `LICENSE.md` extended for
    each module ported); MicroHs and Hugs read as design only, THIH as the checker's specification, no copying from
    GPL projects (Helium, jhc); (b) also vendor MicroHs's checker with an Apache-2.0 notice; (c) only GHC.
  - *Recommended: (a)*; (b) revisited if Tier S's checker stalls.

What to decide first: HFS1 (the route) and HFS7 (when); HFS3 and HFS4 follow from them. HF1 and HF2 are a cheap
trial with no commitment (about 16 to 24 days): the renamer is judged exactly, and its size re-estimates the checker's.

## Decided (2026-11-05, the user)

Two decisions of the same day.

**HFS1-HFS9** (the survey's proposals): as recommended: a trusting elaborator in tiers (S, then L), the
`-O0` STG the reference. In full: HFS1-HFS9 as recommended: HFS1 (b), a clean trusting elaborator in two tiers (S, then L) with faithful ports of the text-emitting parts; the GhcPs dump as input; the libraries' interfaces dumped as forms for Tier S; Arbace's own untyped Core; only deriving, the match compiler, CorePrep, CoreToStg and Unarise ported; -O0 the reference; HF1-HF2 now as a trial; GHC's front end on hsi measured after HE5, decided at HF8; GHC's ported text under BSD-3 recorded.

**HF1-HF2's four proposals** (below, "As built: HF1-HF2", "Proposed"): as recommended:
1. the renamer builds the renamed tree inside `rename.ae`, the facts kept as a projection of it that `rename-check`
   judges (option (a));
2. the interface dump gains roles, family equations, fundeps, default methods and strictness, and the renamer writes
   the same for the program's own modules;
3. `rename-check` enters the gate with the type checker's step, not before;
4. the binding groups' order stays unjudged (any topological order serves the checker).

## As built: HF1-HF2 (2026-11-05, branch `haskell-hf12`)

The trial of HFS7 (a): the oracles of the front end and its first stage, the renamer, judged exactly against GHC's.

**HF1, the oracle dumps** (`tools/hsdump/HsRn.hs`, two new verbs of the helper, Arbace's own code against GHC 9.14.1's
API; `bin/repr-haskell rn-dump`, `iface`):

- `rn` loads the modules together (`-i.` finds the ones the others import), type-checks them with no code generation
  and writes, per module, `FILE.rn.edn`: the renamer's output (`tm_renamed_source`: `HsGroup GhcRn`, the imports, the
  export list) reduced to what the front end must reproduce: `(hsrn/occs [[span name]...])` every located name of the
  renamed tree (declarations, imports, export list, binders and occurrences), a name `(ext ns "Module" "occ")` for a
  top-level or imported one (the defining module, GHC's `GHC.Internal.*` names), `(loc ns "occ" l1 c1 l2 c2)` for a local
  one, its binding site (uniques are not stable); `(hsrn/ops [...])` the operator trees as resolved (`opapp` with the
  fixity applied, `neg`, `secl`, `secr`, `conop`, `tyop`, each with the spans of its operands); `(hsrn/groups [...])`
  the binding groups (`vals rec|nonrec [binders]`, `tycl [names] 0` and `tycl [] 1` for each instance) and
  `(hsrn/group-order [...])` GHC's order of them; `(hsrn/imports ...)`, `(hsrn/exports ...)` (the final export
  avails). Sets are sorted and each element once (GHC keeps a binder's name at both its `FunBind` and its `FunRhs`).
  The renamer's expansions (`ExpandedThingRn`: sections as `rightSection`, record updates) are read through to the
  source thing; the names that GHC attaches at no location (`SyntaxExprRn`: `negate`, `>>=`, `>>`, `fail`, `guard`,
  `==` of literal patterns, `fromInteger`/`fromRational`/`fromString` of literals) are in the set with the span `nil`
  (or the literal's): they are what the checker needs evidence for.
- `iface` writes `MODULE.iface.edn` for the modules asked and, with each export's defining module followed, for the
  modules that define the names (the closure of `Prelude` is 50 modules, 389 KB; `hsiface/exports` as avails with the
  defining module of each name, `hsiface/fixities`, `hsiface/things`: every thing the module defines and exports as
  `datacon`, `tycon` (kind, data/newtype/synonym/family, constructors), `class` (superclasses, methods with types), `id`
  (type), the types as GHC prints them). Every type is also a structured form
  (`tv`, `tycon-app`, `app`, `fun`, `forall` with its kind, `lit`), and `hsiface/instances` lists the instances a
  module defines (`getModuleInfo` is "unimplemented" for a package module's instances and `mi_insts` panics on a
  loaded interface, so they are read from the external package state, each by its dictionary function's type).

**HF2, the renamer** (`arbace/repr/haskell/rename.ae`, 1,397 lines with the notice, namespace
`arbace.repr.haskell.rename`; `bin/repr-haskell rename-check`): the native reader's `GhcPs` trees in, the same forms as
the helper's out, so the judge is a comparison of data (`-main`: `rn`, `check`). It reads the interfaces of the
libraries (`--ifaces`), writes the interface of each module it renames (`--local`, for the modules that import it) and
needs no GHC at run time. Ported from GHC: `mkOpAppRn`, `mkNegAppRn`, `mkConOpPatRn`, `mkHsOpTyRn`, `compareFixity`;
the rest follows GHC's behaviour as the dumps show it (the renamer's text was read as the specification:
`Rename/{Env,Names,Expr,HsType,Pat,Bind,Module,Fixity}.hs`). Covers: scopes of every binder form (lambda, case,
function clauses, guards and pattern guards, `where`, `let`, `do` and comprehensions, view patterns, as/lazy/bang
patterns, record patterns with puns and wildcards, record construction with wildcards, updates, `RULES` binders),
type variables (implicit binders at GHC's spans, `forall`, scoped type variables, pattern signatures, class and
instance heads, GADT constructors, associated families, kind variables), imports (qualified, `as`, lists, `hiding`,
the implicit `Prelude`, `NoImplicitPrelude`), exports (items, `T(..)`, `T(a,b)`, `module M`, headerless `Main`),
fixity resolution (operator applications, negation, sections, infix constructor patterns, type operators; local and
class-level fixity declarations), binding groups by SCC (values and type/class declarations, instances as groups of their
own), wired-in names (`[]`, `:`, tuples, `()`, `->`), class methods in instances resolved through the class.

**Results** (2026-11-05, stage 0 as host, the helper built `-O0`): `bin/repr-haskell rename-check` over **101 modules,
all equal**, imports, occurrences, operator trees, binding groups and exports, in 2 min 25 s to 4 min with the helper's runs
(the renamer itself 1 to 1.6 s a module on stage 0's evaluator, at most 430 MB: `Data/OldList.hs`, 2,145 lines, 3,386
occurrences, 1.6 s): 47 samples written for it (`test/haskell/rename/`: fixities, sections, records, classes,
imports and exports across modules, `where`/`let`/guards, GADTs and existentials, type families, type operators,
deriving, literals, RULES...), 33 modules of ghc-internal's source (`Data/{Maybe,Functor,Function,Ord,Monoid,
OldList,Bits,List,String,...}`, `Control/{Monad,Arrow,Category}`, `Arr`, `Ix`, `Numeric`, `Text/Read/Lex`, `CString`,
`Exts`: the Prelude's own closure, renamed against the installed interfaces, 10 to 2,145 lines) and 21 programs of
`nofib/spectral` (Haskell 2010, imports of base, array...). Bugs the corpora found that the samples did not: a hiding item
names both the type and the constructor of its spelling; a method exported alone belongs to its parent's avail; the uses
of a nested `let` flow to the enclosing binding's group; RULES and the spans of implicit binders in `SPECIALISE`, standalone deriving and expression signatures;
`FixitySig` names every namespace the module declares; a GADT constructor does not see the declaration's variables.
The order of the groups (`group-order`) is reported, not judged: GHC's order of independent bindings is reverse source
order and depends on its uniques (equal in about a quarter of the modules with Tarjan's algorithm visiting the nodes last to first).
**Wider sweeps** (run by hand, not committed as checks; each module alone in its own session): every CPP-free
module of ghc-internal (268 files, 138 accepted by GHC standalone with no CPP: all equal once the bugs the sweep
found were fixed: DataKinds constructors in types, unboxed unit, record and bundled pattern synonyms, wildcard
exports, imported constructors' fields for `{..}`, injectivity result variables, `deriving via`), 60 modules of
mtl, transformers' neighbours, array, stm, parsec, xhtml and containers (the rest CPP or not loadable alone), and
36 more nofib programs (`imaginary`, `real`, `shootout`, `gc`...; the 3 differences are the harness's: the
interface of an `Unsafe` module not dumped in that run).

**Not covered yet** (outside Tier S's common subset): arrows, Template Haskell and quasi-quotes,
`RebindableSyntax`'s in-scope names, `OverloadedRecordDot` and labels, `DuplicateRecordFields`' disambiguation by
constructor, `rec` blocks of explicit `RecStmt`, the exact `fail` rule for exotic patterns (single-constructor
types only), role annotations, warnings and errors (a name not in scope is
recorded, not reported in GHC's words), CPP and hsc2hs modules (the reader still needs the helper's `cpp` verb: the
two ghc-internal modules with `#include` were left out), `Data/Foldable.hs` (GHC itself cannot load it standalone:
its derivings need the package's constructors).

**What this says about the survey's estimates.** HF1 and HF2 together took one session, about an hour of the agent's
wall clock (the survey's days are working days of one agent: 4 to 6 for HF1, 12 to 18 for HF2 on the port route, 7 to
10 on the clean one, 16 to 24 for the trial): 1,400 lines against
`Rename/`'s 18,774 (7%), for a subset that renames the Prelude's own sources and 21 real programs equal to GHC's.
The clean route's estimate (6,000 to 9,000 lines) is high by a factor of three for Tier S: a complete Tier S renamer
(with pattern synonyms, `mdo`, the rest of the list above and error messages) is about 2,000 to 3,000 lines. The type
checker (HF3) is not re-estimated here; its judging data is better than the survey assumed (the nil-span names above).

**What HF3 (the type checker) should start from.**

1. The renamer must return the renamed tree, not only the facts: the same walk with `Name`s substituted, operator
   applications rebuilt (`mk-opapp` already builds the shapes; they are thrown away after the facts are written),
   bindings regrouped in their SCCs (nested `let`s as groups), and the implicit-name slots (`negate`, `>>=`, `fromInteger`...
   as the nil-span facts show them) kept on their nodes. The facts stay as a projection of it, so `rename-check` keeps
   judging it. About 400 lines.
2. The interface dump now has structured types and instances; HF3 still needs roles, arity details, type family
   equations, fundeps, default-method names and strictness (a few more fields of the same verb), and the local
   modules' own interfaces (`iface-text` writes exports and fixities only) must carry the same.
3. The `hsrn` facts are the contract of the front end's boundary with GHC's data: each `ext` name of the facts is a
   key of the interface dump (`hsiface/things`), so the checker can be judged name by name against `-ddump-tc` /
   `--show-iface` as the survey's §4 plans.

**Proposed** (the user decides).

1. HF3 starts with step 1 above (the renamed tree). Option (a) rebuild the tree inside `rename.ae` (recommended: the
   walk exists, 400 lines, the facts remain the check); option (b) a second pass over the facts and the `GhcPs` tree
   (no change to the judged code, but the structure is walked twice and the spans become the join key: fragile).
2. Extend the interface dump with the rest of what the checker reads (roles, family equations, fundeps, defaults,
   strictness) and make the renamer write the same for the program's own modules (recommended, about a day).
3. Whether `rename-check` joins the gate: it runs in 2.5 minutes, light (no process above 450 MB, the helper's
   `-O0` build apart); the Haskell crop is out of `--full` until its rows fit GT8's budget, so recommended: not
   yet, add the step with the type checker's.
4. Groups' order: keep it unjudged (recommended: any topological order serves the checker); or port GHC's
   `stronglyConnCompFromEdgedVerticesUniq` with names' uniques (not stable: no).

## As built: HF3 (start) (2026-11-05, branch `haskell-hf3`)

HF3's start as the user's decisions above give it: HF1-HF2's proposals 1 and 2, then the trusting elaborator's first
slice. Light on the host (samples and single-file nofib programs; main's stage 0 as the tools' host, the helper at `-O0`).

**1. The renamed tree** (proposal 1 (a)). `rename.ae`'s walk now records, beside the facts, what a tree needs, keyed by
the parse tree's own objects (identity, not spans): each RdrName object the Names it resolved to, each maximal operator
application's final shape (its leaves carry their subtrees), the implicit names a node carries (literals' `fromInteger`,
`do`'s `>>=`, `NPat`'s `==` and `negate`, with their spans) and each list of value bindings' SCCs. After the walk
`rtree` substitutes them: `(L sp (rn/name NAME))` (`rn/names` where one place names two: a pun), operator applications
rebuilt by their fixities (`(OpApp (rn/fixity P D) L OP R)`, `NegApp`, infix `ConPat`, `HsOpTy`), `(rn/implicit [NAME...]
[SPAN...])` appended to a node, `(ValBindsRn [(rn/group rec|nonrec [BIND...])...] SIGS)`, a record wildcard's fields
and variables as `(rn/dotdot ...)`, and the module as `(rn/module {:module :decls :binds :sigs :export-list :exports
:imports})`. The facts are its projection: `tree-names` collects every located name of the tree (with the implicit
names, the wildcards' and the import items'), and `rename-check` now also requires the tree's names to be among the
facts and reports the facts the tree does not carry. Result: **101 of 101 modules equal** as before (47 samples, 33
ghc-internal modules, 21 nofib programs), every one with its tree's names equal to its located facts (none outside,
none absent); `-o` writes `NAME.rn-tree.edn` beside `NAME.rn.edn`.

**2. Interfaces** (proposal 2). The helper's `iface` verb (`tools/hsdump/HsRn.hs`) gains a trailing map on each thing,
the positional fields unchanged: type constructors' roles, arity, a synonym's `[tvs rhs]`, a family's flavour and a
closed family's equations (`(eqn [tv...] [lhs...] rhs)`); classes' type variables, fundeps, each method's default
method name (`$dm...`, from `classOpItems`), associated types and roles; constructors' source and implementation bangs
and tag; ids' arity and strictness signature; and `(hsiface/fam-instances [...])`, the module's family instances
(the external package state's, as for class instances). A type variable is now written with its unique (`"a#123"`):
GHC's inferred types name many variables `t`, which made the forms ambiguous. A new verb, `tc` (`bin/repr-haskell
tc-dump`), writes per module `FILE.tc.edn`: the module's things with their types in the same forms, its instances, its
family instances and its desugared Core (`desugarModule`, `-O0`: `-ddump-ds`'s program, after the simple optimiser) with
the types erased (`(v NAME) (lv "occ" UNIQUE) (lit "text") (app F [ARG...]) (lam [B...] E) (let ...) (letrec ...)
(case E B [(alt CON [B...] E)...])`, type and coercion arguments and binders dropped, casts and ticks read through).
The renamer's own interface of a module (`--local`) now also carries `(hsiface/things [...])` as far as the renamer
knows them, types left `nil`: type constructors with their constructors, constructors with arity, fields and source
bangs, classes with their methods, fundeps and default methods (`local-things`); the elaborator writes the typed
interface (`typecheck.ae`'s `iface-text`, `--local`): ids, selectors, constructors, classes with method types and
superclasses, synonyms and instances, read back for the modules that import it (samples `T14Lib`, `T15Use`). Proposals 3
and 4: `rename-check` is not in the gate yet (with this step, Proposed below), the groups' order stays unjudged.

**3. The elaborator's first slice** (`arbace/repr/haskell/typecheck.ae`, 1,645 lines, namespace
`arbace.repr.haskell.typecheck`; `bin/repr-haskell typecheck-check`, `make repr-haskell-typecheck-check`). Clean (HFS1
(b)), no GHC code ported (GHC's `Tc/` and Jones's "Typing Haskell in Haskell" read as the specification):

- input: the renamed tree and the interfaces (every `NAME.iface.edn` of the directories: values' types, classes,
  instances, constructors, synonyms; the wired-in `[]`, `(:)`, `()` and tuples added);
- types `[:tc NAME args]`, `[:tv]`, metas with levels (Rémy's generalisation), `[:app]`, `[:forall tvs preds body]`,
  synonyms expanded on unification and in instance heads and wanteds; deep instantiation of prenex types (a class
  method's `forall f. C f => forall a. ...`);
- inference over the renamed tree's Haskell 2010 core: variables, constructors, application, operators by their
  rebuilt trees, negation, sections, lambdas and `case` by their match groups, `if`, `let`/`where`, `do` (`>>=`, `>>`),
  list comprehensions, arithmetic sequences, lists, tuples, literals (overloaded integers and fractions, strings,
  chars), signatures on expressions, records (fields, construction, update, selectors, record and wildcard patterns),
  every Haskell 2010 pattern (variables, wildcards, as, lazy, bang, constructors prefix, infix and record, tuples, lists,
  `NPat`, literals, signatures), guards and pattern guards;
- binding groups by the renamer's SCCs: the unsigned binders inferred together and generalised (the constraints whose
  variables are all quantified become dictionary parameters, a constraint a superclass of another implies dropped
  (GHC's `mkMinimalBySCs`), the others deferred with their variables kept monomorphic, constraints with a known head
  reduced by an instance first); the monomorphism restriction's rule 1 (a pattern binding or an argument-less one
  quantifies no constraint); the signed binders checked against their signature with its givens (closed under
  superclasses, `$pNCls`); a pattern binding binds every variable of its pattern;
- classes and instances as dictionaries with GHC's names: `C:Cls` applied to the superclass dictionaries and the
  methods, `$fClsT` (an instance with a context a function of its dictionaries, each superclass a binding `$cpNCls` of its
  own), `$cop` methods checked at the instance head with the method's own forall skolemised, missing methods from the
  class's `$dmop` applied to the instance's dictionary, default methods `$dmop` over the class's dictionary; derived
  instances entered in the instance table (their bindings are HF4's; constructor classes' heads drop the last parameter);
- defaulting (Haskell 2010 §4.3.4): a meta whose constraints are all standard classes, one numeric, gets the first of
  `Integer`, `Double` satisfying them; `HasCallStack` solved at its call site as GHC's solver builds it (`pushCallStack`
  of the name and `SrcLoc` onto `emptyCallStack`); constraints grouped by class in their first order;
- output: each top-level binder's type and the elaborated program in the `tc` verb's Core vocabulary, dictionaries
  passed (GHC's AbsBinds shape: `\dicts -> letrec mono = ... in mono` for a recursive group), pattern matching left
  as `(match ...)`/`(clause ...)` for HF5's match compiler, overloaded literals folded at `Int`, `Integer` and
  `Double` as GHC's desugarer folds them.

The judge (`compare-with-ghc`, against the `tc` verb): (a) **types**: every binder, constructor, worker, selector and
method of the module, canonical forms compared (synonyms expanded, foralls dropped, variables renamed by first
occurrence, constraints sorted, multiplicities ignored), a binder GHC has and the elaborator lacks a failure; (b)
**evidence**: per top-level binding (an instance method keyed by its dictionary function, `:Main.main` apart), the
sorted applications of a name to dictionary terms (global `$f...`, superclass selections, dictionary parameters by
place), after what GHC's `-O0` desugarer does after the desugaring proper (`simpleOptPgm`), applied to both sides: a
once-used let inlined, a dead one dropped, a lambda applied to trivial arguments beta-reduced, a module's unexported
top-level binding used once (not under a lambda) inlined at its use, GHC's floated top-level dictionaries
(`$dNum = $fNumInt`) and let-bound ones inlined, local binders renamed by place; (c) **Core** equal after the same
normalisation: reported, not judged (the match compiler is HF5's). The **negative control** (`--control true`:
defaulting's order reversed, superclass-implied constraints kept) must differ from GHC on some module, else the check
fails. (A first version of the judge compared only applications to dictionary *parameters*: the global dictionaries
were text after normalisation and escaped the recogniser. Fixed; the wider judge found the default methods' unfoldings
and the four choices below.)

What matching GHC's evidence required beyond the type system, each found by the judge: every default method has a
compulsory unfolding (GHC.Tc.TyCl.Instance, Note [INLINE and default methods]) inlined into an instance's missing
method even at `-O0`, so the helper's `iface` verb now dumps them (`(hsiface/unfoldings [(dm-unfolding NAME CORE)...])`,
the Core erased as `tc`'s) and the elaborator inlines them; a numeric literal pattern at `Int`, `Word`, `Char`,
`Double`, `Float` is matched without `(==)` (tidyNPat); an instance's superclass dictionary that is not a variable,
or any of an instance with a context, is a binding of its own (`$cpNCls`); in an instance's method a constraint the
instances solve to a variable is solved so, else by the instance's own dictionary's superclasses; constraints a
reduction by an instance produces come after the others, and the quantified ones are grouped by class; a pattern
binding's right-hand side is bound once; `MonadFail`'s `fail` for a failable `do` pattern, with its `HasCallStack` at
the pattern ("a do statement"); a dictionary function is named by each argument's outer type constructor, synonyms
expanded (`instance C (State Env)`: `$fCStateT`).

**Results** (2026-11-05): `bin/repr-haskell typecheck-check` over the 17 samples of `test/haskell/typecheck/` (Haskell
2010: basic polymorphism, classes with superclasses and defaults, defaulting and the monomorphism restriction, local
polymorphism, monads, higher-kinded classes, a user `State` monad, constructor classes, local recursion and list
comprehensions, imports of base, records, a `Num` instance and literals at it, a cross-module library and its user, the
restriction's ambiguity case, methods with their own constraints, `MonadFail`, deriving): **17 of 17 equal**, types 194
of 194, evidence 220 of 220 bindings, Core equal in 107 of 220 (the rest is the match compiler's shape); the control
differs on 10 of 17. In about 30 s for the 17 (most of it reading the interfaces once per module), at most 600 MB.
Wider, run by hand (scratch scripts, not committed as checks): the 21 nofib/spectral programs of rename-check, **17 of 21
equal**, types 626 of 626 binders, evidence 597 of 608 bindings, up to 1,124 lines (`simple`, 125 bindings, 6.6 s); and
the 36 single-module programs of nofib's other directories that rename equal (gc, imaginary, real/maillist, shake,
shootout, smp), **31 of 36 equal**, types 352 of 352, evidence 316 of 327. The non-passes fall in recorded classes:
(1) the order of two constraints of one class or of classes GHC's solver orders otherwise (life's `elt`, simple's
`table_search`: GHC's inert set's order; one binding each); (2) superclass bindings of some instances without a
context whose superclass is a variable (exact-reals: GHC binds `$cp1Ord = $fEqCR`, T02 inlines `$fShapeSq`; the rule
is not found yet) and the inner lets of inlined unfoldings (exact-reals, power); (3) extensions outside the slice:
unboxed tuples (k-nucleotide), a newtype-derived instance of a C type (reverse-complement), `printf`'s
variadic instances (smp/threads005); foreign imports (their signature their type), equality constraints `~` (solved by
unification, their evidence erased), pragmas in expressions and higher-rank arguments (`runST`, trusted: no escape
check) were added for the programs that needed them. Bugs the programs found that the samples did
not: a lazy `filter` that read the metas' levels after their adjustment (the ambiguous constraints of a binding were
quantified), NPat's equality before the literal, record patterns, derived instances (and their heads for constructor
classes), synonyms in instance heads, instance methods of the same name in two instances, local recursive groups.

**Not covered yet** (beyond this slice of Tier S): the deriving bindings (HF4: their instances are in the table), the
match compiler, CorePrep, CoreToStg (HF5, HF6: so no program runs on hsi from Arbace's own STG yet, and this branch does
not carry hsi), higher-rank types, multi-parameter classes and fundeps' improvement, flexible and overlapping instances,
GADTs, existentials, type families, kinds, `Typeable`/`Coercible` evidence, implicit parameters but `HasCallStack`,
`mdo`, `ApplicativeDo`, `RebindableSyntax`, a signature's own givens of `HasCallStack`, the class `Read`'s defaulting
cases beyond the standard rule, error messages.

**On the estimates.** The survey gave HF3 20 to 30 working days for 14,000 to 20,000 lines (Tier S). The first slice,
Haskell 2010 with classes, is about 1,650 lines and took this session (the renamed tree and the interfaces included,
about two hours of the agent's wall clock): the checker's core is small; the remaining cost is in the extensions of the list above and in matching
GHC's choices exactly where they leave no trace in the types (the two classes above).

**Proposed** (the user decides).

1. The judge of HF3. Options: (a) types and evidence as built, full Core equality from HF5 (the match compiler) on;
   (b) port GHC's match compiler now (`HsToCore/Match*`, about 2,300 lines) so Core is judged from HF3. *Recommended:
   (a)*, HF5 is where the port belongs (HFS5 (a)).
2. The default methods' unfoldings: done in this branch (the helper dumps every default method's compulsory unfolding,
   the elaborator inlines it), because the evidence judge cannot pass a single instance of a base class without it.
   Options: (a) keep it (the unfoldings are data of the pinned libraries, like their interfaces; Tier L derives them
   from the libraries' own source); (b) leave the inlining to HF5's desugarer and judge instances' missing methods as a
   recorded class until then. *Recommended: (a)*.
3. The order of constraints (class 1 above). Options: (a) a recorded class until the solver keeps GHC's order (its
   inert set's insertion order: the work list's order, not GHC's uniques); (b) compare a binding's dictionary
   parameters as a multiset per class (hides real misplacements). *Recommended: (a)*.
4. The gate step of decision 3: rename-check with the type checker's. Options: (a) one `--full` step `haskell-front`
   running `bin/repr-haskell rename-check` and `typecheck-check` (about 4 minutes, at most 600 MB, the host's GHC at
   its pin for the helper; the crop's other rows stay out until GT8's budget); (b) an essential step (samples only,
   about 1 minute); (c) wait for HF8. *Recommended: (a)*, after the user's night review of this branch.
5. HF3's next steps, in order: classes beyond Haskell 2010 (multi-parameter, flexible and overlapping instances,
   fundeps), higher-rank and scoped type variables (checked, not trusted), existentials and GADTs' local equalities,
   equality constraints, pragmas, then the type-level tier (HF9). Alternative: HF4 (deriving, a port) first, to make the
   derived instances' bindings judged too. *Recommended:* HF4 next (a port, mechanical, Sonnet), then the extensions as
   the corpora's measured needs order them; the nofib sweeps above become a committed list (`test/haskell/typecheck-check.txt`,
   as rename-check's) with HF4.

## Decided (2026-11-05, the user: HF3's proposals)

HF3's five proposals as recommended: the judge types plus evidence, full Core from HF5; the default-method unfoldings kept; constraint order a known class until the solver keeps GHC's; a --full step haskell-front (rename-check and typecheck-check) after review; HF4 (deriving) next, then the extensions in the corpora's order, the nofib sweeps committed as a check list.

## As built: HF5-HF6 (first slice) (2026-11-05, branch `haskell-hf56`, from `haskell-hf3`)

HF5 and HF6's first slice: the elaborated program of HF3 becomes STG in hsi's format and runs on hsi. Light on the host
(main's stage 0 as the tools' host, the helper at `-O0`, hsi's light driver built by `bin/hsi build` in the worktree, the
libraries' STG dumps of `haskell-integrate`'s worktree through `HSI_LIBS`). `haskell-integrate` (7bda6b88b) is merged in
for hsi; conflicts: the Makefile's `.PHONY` line and targets, `bin/repr-haskell`'s helper build line (`-threaded` and the
quoted `-i`), `doc/JOURNAL.md` and `tools/hsdump/HsDump.hs`'s usage and verb dispatch, all resolved by keeping both sides.
Deriving is left to HF4 (`haskell-hf4`): the programs here derive nothing.

**HF5, the match compiler** (`arbace/repr/haskell/desugar.ae`, `arbace.repr.haskell.desugar`, about 300 lines): the
elaborator's `(match ...)`/`(clause ...)`, `(guards ...)`, pattern guards, `(listcomp ...)` and patterns in lambdas become
cases, after GHC 9.14.1's `HsToCore/Match.hs` and its parts: tidy1 (variables and as-patterns bound to the match variable,
list patterns as conses, literals at Int, Word, Char, Double, Float as their boxing constructor around the primitive literal,
string patterns as character lists, newtype constructors transparent), groupEquations by the first pattern's kind
(variables, constructors, primitive literals, overloaded literals by `(==)`), one case per constructor group with the
equations of a constructor gathered, the alternatives in tag order, DEFAULT first and only when a constructor is missing,
and a DEFAULT standing for one constructor made that constructor's alternative (`refineDefaultAlt`, whose `ipv` binders
GHC's `-O0` STG shows); guards as dsGuarded (`otherwise` and `True` drop out); list comprehensions as deListComp (the `-O0`
translation, no foldr/build); lambdas collected (`mkLams`); lazy patterns (`~p`, kept by the elaborator now: each variable bound by a selector, as
mkSelectorBinds); the interfaces' inlined unfoldings normalised to the program's spelling and, applied, beta-reduced (the
simple optimiser's beta); a scrutinised variable replaced by its case binder in the alternatives (what GHC's `-O0` output
shows). Failure is a let-bound handler inlined when used once and dropped when unused; shared, it becomes a join point in
STG (`letne` of a `:j` closure over the void argument, called with `(##)`, as GHC's mkFailurePair and let-no-escape
make it; an ordinary closure when a call is under a closure). The elaborator gained the tables the passes need (`:all-cons` with tags and strict marks,
`:tycons` with each type's flavour and constructors, `:all-classes`), and a fix: the record selectors' case scrutinised
an unbound variable.

**HF6, CorePrep, CoreToStg, Unarise** (`arbace/repr/haskell/tostg.ae`, `arbace.repr.haskell.tostg`, about 450 lines): A-normal
form (every non-trivial argument let-bound, the last argument's binding outermost as cpeArgs makes them; a string literal's
`Addr#` bound by a case, as the spec's worked example), constructors saturated (a saturated application `con`/`rcon`, an
unsaturated one the worker, whose binding the module carries; a strict field's argument evaluated first, `$WCon`'s work;
the library classes' `C:Cls` constructors saturated too), floats (a saturated constructor's arguments' bindings floated out
before it, a case scrutinee's lets out of the case, a top-level CAF's arguments to the top level: wantFloatTop),
right-hand sides (lambda a `:r` closure, a saturated constructor of atoms `rcon`, else an updatable thunk), free variables
(Stg/FVs; top-level binders excluded), representations from the constructors' `stg/data` (the libraries' dumps for theirs:
`I#`'s field `:int`, `C#`'s `:word`, ...), literal alternatives with `(prim REP)`, the module's `stg/data` (data types,
newtypes and the non-newtype classes' `C:Cls`), the classes' selectors (`mkDictSelId`), newtype classes (one method and no
superclass, or one superclass and no method) and newtypes erased, the program's `:Main.main` (`runMainIO main`). Unarise
leaves Haskell 2010's programs as they are (no unboxed tuples or sums at the source level). Names: the program's top-level
binders `main:M/occ`, the libraries' by the unit of their module (read from the dumps' headers), locals `occ_N`.

**The judges** (`bin/repr-haskell stg-check`, `make repr-haskell-stg-check`; about 2 minutes, at most 1 GB a process):

- **Run, the final judge**: the programs of `test/haskell/stg/` (nine Haskell 2010 programs with a `main`: list functions
  and nested patterns, guards and character and numeric literal patterns, data types and classes with defaults, a user
  `State` monad with `Functor`/`Applicative`/`Monad` instances and `Maybe`'s `do`, records (construction, update, field
  patterns), strict fields, a `Num` instance, newtypes, `Integer`, `where` with local recursion, list comprehensions with
  guards and `let`, imports of `Data.List` and `Data.Char`, hsi's own samples `Hello` and `Nfib`, guards falling through
  equations, string and `Integer` literal patterns, `reads`, local mutual recursion, lazy patterns) compiled from source by
  Arbace's front end (native read, rename, elaborate, desugar, STG) and run on hsi: **9 of 9 print GHC's `-O0` build's
  stdout byte for byte**. The negative control
  (each program's run against the next program's reference) fails as it must.
- **Core** (decision 1 of HF3: in full from HF5): the desugared Core against the helper's tc verb after the judge's
  normalisation: **152 of 220 bindings of the 17 typecheck samples equal** (107 of 220 before the match compiler), 48 of 85
  of the nine programs.
- **STG** (`arbace/repr/haskell/stgcmp.ae`): per top-level binding of an external name, against the helper's `stg` dump at
  `-O0`, Typeable bindings dropped, internal names renamed by first occurrence, a closure's free variables as a set (GHC
  orders them by unique): **62 of 83 bindings equal**, the data declarations equal (as sets) in 9 of 9 (S01, S07, S08
  entirely).

The remaining differences, by class (none is a wrong result on the programs): (1) GHC's simple optimiser at `-O0`
(`simpleOptPgm`): local functions used once inlined and beta-reduced, constant dictionaries floated to the top level as
internal bindings (`$dShow_4`), record selectors applied to a matched record (`older`); (2) CorePrep's eta-expansion of
top-level bindings of a function type that are not lambdas (`get = \eta -> get1 eta`, the newtype-wrapped functions,
`total`); (3) the order of a binding's floats relative to a join point (`grade`); (4) AbsBinds of a mutually recursive
group without signatures (GHC's tuple and selectors, T04's `evens`/`odds`), which the elaborator writes as a letrec; (5)
GHC's top-level `(rec ...)` groups (the judge flattens them; hsi takes the separate bindings); (6) the order of the
module's `stg/data` forms. hsi's own `Loop` sample (a 3,000,000 element `foldl`) exhausts the light runs' 3 GiB cap from
GHC's STG as from ours: a memory class, not the compiler's. Not covered yet: `n+k`, view and bang patterns' strictness
in matches, `seq`, overloaded literal patterns at non-built-in types beyond `(==)` chains, string literals with characters
beyond ASCII (`unpackCStringUtf8#`), `fromRational` literals at other than `Double` and `Float`, foreign imports, the
derived instances' bindings (HF4), unboxed tuples (no Unarise work yet), the Typeable bindings (`$trModule`, `$tc`), and
the desugarer's top-level dictionary bindings.

**On HFS5 (port).** The two files follow GHC's algorithms and reproduce their output's shape where the judges show it,
but they were written after the algorithms in this session's time, not translated line by line from the tarball's text;
they carry GHC's notice and are recorded under its license (LICENSE.md) as derived work. Proposal 1 below.

**Proposed** (the user decides).

1. HFS5's port of the match compiler and CorePrep. Options: (a) translate `HsToCore/Match*.hs`, `GuardedRHSs.hs`,
   `CoreToStg/Prep.hs`, `CoreToStg.hs` line by line now (about 3,000 `.ae` lines, 3 to 5 days, Sonnet), replacing these
   files, so the Core and STG judges reach equality by construction; (b) keep these files and close the judges' classes
   one by one, porting a GHC function where a diff needs it (HF3's proposal 1 (a) style). *Recommended: (b)*: 6 of 6
   programs already run, the classes above are few and the simple optimiser (class 1) is a separate piece either way.
2. The simple optimiser (class 1). Options: (a) port `GHC/Core/SimpleOpt.hs` (1,612 lines) into the pipeline so the Core
   and STG judges compare GHC's `-O0` output exactly; (b) normalise both sides in the judges only. *Recommended: (a)*:
   it is what GHC's `-O0` runs, and the STG judge cannot be exact without it.
3. The run judge's corpus. Options: (a) grow `test/haskell/stg/` to hsi's samples and the synthetic corpus' Haskell 2010
   programs as the elaborator covers them, recorded as a check list with classes (`test/haskell/stg-check.edn`); (b) only
   the hand-written programs. *Recommended: (a)*, after HF4 (deriving: `Show` and `Eq` are in most programs).
4. The gate. Options: (a) add `stg-check` to the `--full` step `haskell-front` of HF3's decision 4 (about 2 minutes, light;
   it needs the libraries' STG dumps and hsi's driver: `bin/hsi dumps` and `bin/hsi build`); (b) wait for HF8.
   *Recommended: (a)* once `haskell-integrate` is on main.

## Decided (2026-11-05, the user: HF5-HF6's proposals)

HF5-HF6's four: (1) (b) the match compiler and CorePrep kept as written after GHC's algorithms, GHC functions ported where a diff needs them (amending HFS5's line-by-line port); (2) (a) GHC's SimpleOpt ported into the pipeline; (3) (a) the run corpus a recorded check list after HF4; (4) (a) stg-check in the --full step haskell-front once haskell-integrate is on main.
## As built: HF4 (2026-11-05, branch `haskell-hf4`)

Deriving, `arbace/repr/haskell/deriv.ae` (about 500 lines) with its integration in `typecheck.ae`; ports of GHC 9.14.1's
`compiler/GHC/Tc/Deriv/Generate.hs` and `Deriv/Functor.hs` (HFS5 (a), BSD-3, recorded in `LICENSE.md` and
`doc/ARBACE.md`; `Deriv/Infer.hs`, `Deriv/Utils.hs` and `Deriv.hs` read as the specification of the contexts and the
choice of strategy). Nothing else of GHC was needed beyond the pinned tarball's text.

- **Generators** (the generators' trees, built as the renamed tree's nodes so that the elaborator types them as any
  instance's methods): Eq (`gen_Eq_binds`: one constructor, all nullary by tags, mixed by a tag check then a case per
  non-nullary constructor), Ord (`gen_Ord_binds`: `compare` always and `<`, `<=`, `>`, `>=` for one to three
  constructors or an enumeration, `mkOrdOpRhs`/`mkInnerRhs` with their tag ranges, `mkCompareFields`), Show
  (`showsPrec` with `showParen`, infix constructors with their declared precedence, records, operators in
  parentheses), Read (`readPrec` with `parens`, `prec`, `expectP`, `choose`, `readField`, `+++`; `readList` and
  `readListPrec` the defaults), Enum, Bounded, Ix (enumerations and single constructors), Functor (`fmap`, `<$`),
  Foldable (`foldr`, `foldMap`, `null`), Traversable (`traverse`), by `functorLikeTraverse`'s cases: the type variable,
  a type constructor applied with the variable in its last argument only (nested), no occurrence. Not generated:
  tuple and function fields of Functor-like classes (the class is left to its defaults and the judge shows it),
  `Data`, `Lift`, `Generic`.
- **Contexts** (`Deriv/Infer.hs`): each field's `C ty` reduced by the instances to constraints on type variables, a
  fixpoint over the module's derived instances (the recursive types), duplicates dropped keeping the last occurrence
  (GHC's solver leaves `T a [b] (Maybe a)`'s context as `(Eq b, Eq a)`: found by the judge); Functor-like classes
  need the class at each nested type constructor (`Functor f` for a field `f a`).
- **Strategies** (`Deriv.hs`, `mkNewTypeEqn`): a newtype's Eq, Ord, Ix and Bounded by the newtype strategy as GHC
  chooses it without any extension (`std_class_via_coercible`); with `GeneralizedNewtypeDeriving` every class but Show
  and Read, unless `DeriveAnyClass` is on too (then the stock classes stock, the others anyclass); `DeriveAnyClass`
  gives an empty instance; standalone deriving (`deriving instance ctx => C (T a)`) uses the declaration's own
  context. The newtype strategy is the representation type's methods coerced: `$cm = m dRep` (with the method's own
  constraints still parameters); in an instance with a context whose representation dictionary is one of its
  parameters, GHC's simple optimiser has inlined them into the dictionary function and so does the elaborator. The
  rename tree now carries the module's extensions (`:exts`) for it.
- **Where GHC's primops are replaced**: `dataToTag#`, `tagToEnum#` and the `Int#` comparisons and subtraction have no
  type in the elaborator (no unboxed types yet); the tree has `(DerivTag e)` (an Int), `(DerivTag2Con [CON...] e)` and
  `(DerivPrim op ...)`, typed as GHC's code is at `Int`, the comparisons are `eqInt`, `ltInt`... of `GHC.Classes`,
  and the auxiliary bindings `$tag2con_T` and `$maxtag_T` are inlined at their uses (so they stay among the
  "GHC only" bindings of the judge, the one recorded difference of the samples).
- **The judge** is the evidence judge of HF3, now over the derived instances' bindings as well (`-ddump-deriv` is
  GHC's text of the same bindings, `bin/repr-haskell tc-dump FILE --ghc -XHaskell2010 -ddump-deriv`, kept for reading;
  the tc dump's Core of `$c==@$fEqT`... is what the judge compares, per instance and per method, so a method GHC
  generates that the port lacks is a "GHC only" binding and one with other dictionaries is an evidence difference:
  the set of methods of each derived instance, the dictionaries each one applies, their order and the instance's
  context equal GHC's). Samples `T18Derive` (the seven Haskell 2010 classes over enumerations, records, infix
  constructors, parameterised and recursive types, a newtype), `T19Derive2` (Functor, Foldable, Traversable over a
  tree, a rose tree, a pair, a type with a non-parameter first argument; newtype strategy for `Num`, `Eq`, `Show`,
  `Functor`, `Foldable` of a newtype; standalone deriving with and without contexts), `T20Derive3` (tag-range
  Ord, large enumerations, backquoted and symbolic infix constructors, record syntax with an operator field, an
  empty data type, a unit type, strict fields, newtypes), `T21AnyClass` (`DeriveAnyClass` with the other classes
  together). `typecheck-check` also takes a list (`test/haskell/typecheck-check.txt`, HF3's decision 5): the nofib
  programs of the sweeps, each a session of its own, the non-passes `known` with their classes.
- **Results** (2026-11-05): `bin/repr-haskell typecheck-check` over the 22 samples of `test/haskell/typecheck/` (the 17
  of HF3 and T18-T22): **22 of 22 equal**, the negative control differs on 11; the only "GHC only" bindings are the
  enumerations' `$tag2con_T` and `$maxtag_T`. The list `test/haskell/typecheck-check.txt` (`bin/repr-haskell
  typecheck-check test/haskell/typecheck-check.txt`: the 21 nofib/spectral programs of rename-check and the 36 others
  of HF3's sweep, each in a session of its own, about 13 minutes, light): **48 of 57 equal, the 9 others `known`** in
  HF3's three classes (life and simple: the order of two constraints; exact-reals, power, gc/power, integrate:
  inlining differences of GHC's simple optimiser; k-nucleotide, reverse-complement, threads005: unboxed tuples, a
  newtype-derived instance of a C type, `printf`), none new, and the derived instances of the nofib programs (their
  `Eq`, `Ord`, `Show`, `Read`, `Enum`, `Ix`, `Bounded` instances, which HF3 left as "GHC only") now judged: the
  (spectral/constraints went from 36 to 57 judged evidence bindings). What the judge found while matching GHC: the order of an
  inferred context (last occurrence kept), the newtype strategy for `Eq`, `Ord`, `Ix`, `Bounded` without any
  extension, a coerced method as a binding of its own except when its dictionary is the instance's parameter,
  GHC's choice of strategy with both `GeneralizedNewtypeDeriving` and `DeriveAnyClass` on, `foldMap`/`null` and the
  tuple cases of `functorLikeTraverse`.
- **Not covered**: `Generic`, `Data`, `Lift`, `Typeable` (built in), (HF4b: `DerivingVia` and the strategy keywords now, below)
  strategies written in the clause (the choice is GHC's default), `DeriveAnyClass` contexts inferred from default
  signatures, function fields (contravariance) in Functor-like classes, deriving for data families and GADTs,
  existentials, `-XStandaloneDeriving` of a type of another module, strict-field unboxed types; `show` and `read` runs
  (the exit criterion's second half) wait for hsi's first light (HF7), so the derived code is judged by its types and
  dictionaries only; `-ddump-deriv` is GHC's text and was read, not compared (no pretty-printer of the generated trees:
  the tc dump's Core of each `$c` binding is the stricter comparison, and the sets of methods per instance equal).

**Proposed** (the user decides).

1. The enumerations' auxiliary bindings `$tag2con_T`, `$maxtag_T` (GHC's `DerivTag2Con`/`DerivMaxTag`, top-level
   bindings of the module's Core). Options: (a) keep them inlined, the one recorded difference of the samples, until
   HF5 gives the desugarer primops and unboxed `Int#`; (b) generate them now as top-level bindings over the `DerivTag`
   nodes (about 40 lines, judged equal in evidence, still not in Core). *Recommended: (a)*.
2. The gate step `haskell-front` of HF3's decision 4: now that the list exists. Options: (a) `--full` step with the 22
   samples (under 2 minutes) and the 57-program list (about 13 minutes, at most 1 GiB), as `rename-check` is judged; (b) the samples
   only in the essential gate (2 minutes), the list in `--full`; (c) leave it out of the gate until GT8's budget.
   *Recommended: (b)*, after the user's review of this branch.
3. What the next derivings need. Options: (a) `Generic` (HF9: needs type families and kinds, `Rep` types), `Data` and
   `Lift` then; (b) `DerivingVia` and the strategy keywords first (cheap: the strategy is already the code's choice;
   `via T` needs the representation by coercion like `newtype`); (c) neither until a corpus asks. *Recommended: (b)*
   next, (a) at HF9.
4. Deriving in Tier L (the libraries from source, HF10): the base library derives with `Generic`, `Data`, `Lift`
   in places; those instances become HF9's scope, noted here so the Tier S exit is not read as covering them.

## Decided (2026-11-05, the user: HF4's proposals)

HF4's four as recommended: $tag2con/$maxtag inlined until HF5; a haskell-front step, the samples essential and the nofib list in --full; DerivingVia and the strategy keywords next, Generic/Data/Lift at HF9; Tier S's exit does not cover Generic/Data/Lift.

## As built: HF4b, the strategy keywords and DerivingVia (2026-11-05, branch `haskell-deriving-via`)

Proposal 3 (b) of HF4, decided as recommended. `Deriv.hs`'s choice of strategy (`mkEqnHelp`, `mkNewTypeEqn`, `via`'s
`mkViaEqn`) read as the specification, as in HF4 (no new GHC text ported: BSD-3, origin in `LICENSE.md` and
`doc/ARBACE.md` as for HF4); the changes are in `typecheck.ae` and one line of `rename.ae`.

- **Strategies written** in a clause (`deriving stock (..)`, `deriving newtype (..)`, `deriving anyclass (..)`, `deriving (..) via T`)
  and in a standalone declaration (`deriving stock instance ...`, `deriving via T instance ...`): the request carries
  `:strat`; `stock` is the generators whatever the extensions, `newtype` the representation type's methods coerced for any
  class (Show and Read too), `anyclass` an empty instance, `via T` the methods of `C T` coerced, `T` as written (of the
  class's kind: `deriving Functor via []`), its type variables those of the declaration, the context inferred as for the
  newtype strategy (`C T` reduced by the instances) or the standalone declaration's own. A standalone `newtype` strategy
  substitutes the declaration's parameters for the data type's. Without a keyword the choice is HF4's.
- **What the judge found**: (1) a class of one method and no superclass has a newtype dictionary, GHC's simple optimiser
  makes the coerced method the dictionary itself (no `$cm` binding), with or without a context; (2) a superclass
  dictionary that is a dictionary function applied to the instance's own context parameters alone is inlined into the
  dictionary (no `$cpN` binding; it also changed a hand-written `instance Num a => Monoid (Sum' a)`, which had differed);
  (3) in a standalone declaration with a strategy GHC binds the implicit type variables at the strategy's span
  (renamer, `rename-check`'s `hsrn/occs`).
- **Samples** `T23Strategies` (every keyword in clauses, newtype strategy for Show, stock beside newtype, anyclass on a newtype,
  standalone with each strategy including `deriving newtype instance Functor F`) and `T24Via` (`Semigroup`/`Monoid` via a
  `Sum'`, `Show` via `Wrap Int`, via an applicative wrapper with a parameterised type, via a type application, `Functor via []`,
  a single-method class via `Int`, standalone via with and without a context).
- **Results**: `bin/repr-haskell typecheck-check`: **24 of 24 equal**, the negative control differs on 13 (the only "GHC only"
  bindings the enumerations' `$tag2con`/`$maxtag` as before and T04's `ds`); the list `test/haskell/typecheck-check.txt`: 48 of 57 equal, the 9 known, unchanged.
- **Not covered**: coercibility of the via type is not checked (GHC rejects a via type not representationally equal), nor
  that a `newtype` strategy's class is derivable; `deriving anyclass` contexts from default signatures; `Generic`, `Data`, `Lift`
  (HF9); a via type in a data declaration with more type variables than the declaration binds.

## As built: HF5-HF6's second slice, HF7 (first light), HF3's second slice (2026-11-06, branch `haskell-hf7`)

From `haskell-hf56` (589e0cde7) with `haskell-hf4` (08a4354e7) merged in (HF5-HF6's decision 3 asks for the run corpus
after HF4; conflicts in `typecheck.ae`'s `data-decl!` and the elaborator's bindings, both sides' tables kept, and in the
docs). The decisions of HF5-HF6 as the user took them: (1) (b) the match compiler and CorePrep kept, GHC's functions ported
where a judge needs them; (2) (a) GHC's simple optimiser ported; (3) (a) the run corpus a recorded check list; (4) the gate
step waits for `haskell-integrate` on main. Light on the host: main's stage 0, the helper at `-O0`, hsi's light driver,
the libraries' STG of the oracle pack `haskell-stg-libs` (`.tmp/oracle-packs/unpacked/c2c81126.../`, the same dumps
`haskell-integrate`'s worktree held), at most 2 GiB a process.

**1. GHC's simple optimiser** (decision 2 (a); `arbace/repr/haskell/simpleopt.ae`, a port of
`compiler/GHC/Core/SimpleOpt.hs`): the occurrence information it reads (OccurAnal's: dead, once outside a lambda, many;
a recursive group's binders loop breakers; the top level's one recursive group split into its binding groups in dependency
order, Tarjan's), then `simple_opt_expr` rule for rule on the untyped Core: `preInlineUnconditionally` (a binder dead or
used once outside a lambda inlined unoptimised with its binding's environment, `soe_inl`), `postInlineUnconditionally` (a
trivial optimised right-hand side substituted), `simple_app` (beta with each argument bound as `simple_bind_pair` binds a
let, a let in head position floated over the arguments, compulsory unfoldings), case of known constructor
(`exprIsConApp_maybe`'s constructor case with `simple_out_bind`), `go_lam`'s collected lambdas, exported Ids never inlined
nor dropped (the export list's, and the Ids GHC keeps: `$f`, `$dm`, `$tc`, selectors, methods; a dictionary function's
methods count twice, as its DFunUnfolding mentions them). The compulsory unfoldings of `GHC.Types.Id.Make`: `seq`
(`\a b -> case a of _ -> b`: the run corpus's `t` helper), `oneShot`, `coerce`, the sections, `nullAddr#`, every newtype's
constructor (Note [Compulsory newtype unfolding]: the identity, `IO` among them) and a newtype class's dictionary
constructor. Not ported: eta reduction (`tryEtaReduce` needs every Id's arity), rules (none at `-O0` in a program).
A newtype constructor's case is no case (GHC's match is a cast): the field and the case binder are the scrutinee.

**2. What `-O0` runs after the desugarer** (a correction of §2.3): `GHC.Core.Opt.Pipeline.getCoreToDo` keeps
`simplify "final"` at every level, so `-O0` runs the Simplifier once (inlining, case of case, eta expansion, the built-in
rules), not only `simpleOptPgm`. Its effects on the output: shapes (the STG judge's classes below) and, where the
built-in rules compute differently from the machine, results: GHC folds the floating-point primops on the literals'
Rationals (`0.0## *## -2.5##` is `0.0`, a Rational has no negative zero) and drops identities (`0.0## +## x` is `x`, so
`0.0## +## (negateDouble# 0.0##)` prints `-0.0`). Those rules are ported (`arbace/repr/haskell/constfold.ae`, a port of
`compiler/GHC/Core/Opt/ConstantFold.hs`'s `Float#`/`Double#` rules: `binaryLit` with `floatOp2`/`doubleOp2` and the division
guards, `identity`, `rightIdentity`, `strengthReduction`, `negOp`, `semiInversePrimOp`, the fused multiply-adds' `fmaLit`,
`fmaZero_z`, `fmaOne`, `convFloating`, and `Literal.hs`'s conversions, `double2Int#` wrapping), applied after the simple
optimiser; a literal is a Rational (`-0.0##` is `0.0`). The integral primops' rules compute what the machine computes and
are left to hsi.

**3. CorePrep, CoreToStg and Unarise, GHC's functions where the corpus needed them** (decision 1 (b);
`arbace/repr/haskell/tostg.ae`): representations (`typePrimRep`) from the elaborator's types: every local binder's
zonked type (`:ltypes`), a global's result type after its arguments, a primop's from `GHC.Internal.Prim`'s interface;
`Int#`...`Word64#`, `Char#`, `Double#`, `Float#`, `Addr#`, the unlifted boxed `:U`, `State#` void. Primops saturated as
`(primop "OP" ...)` (`maybeSaturate`'s eta expansion when not), a void argument `void#`; an unlifted argument case-bound
(`cpeArg`'s case float), an unlifted let a case (Core's let-can-float invariant, the desugarer's `bindNonRec`); Unarise's
unboxed tuples: void components dropped, the constructor of the non-void ones' count (`MkSolo#`, `(#,#)`...), `(multi N)`
alternatives, a binder of an unboxed tuple unarised into its components (a case on it renames the alternative's binders).
The module's own constructors' fields their types' representations; the boxed `(~)`'s evidence `Eq#`; `word64`
literals (Typeable's fingerprints), infinite floating literals.

**4. HF3's second slice: the elaborator's extensions the run corpus asked for, in its order** (the next stage of the route,
HF3's decision 5: "the extensions as the corpora's measured needs order them"; `typecheck.ae`): `MagicHash`'s literals
(`3#`, `3##`, `'c'#`, `"s"#`, `1.5##`, `1.5#`, exact) and the primops' types; `UnboxedTuples` (`(# a, b #)` in patterns
and expressions, the unboxed tuples' constructors wired in, their representation arguments dropped from the interfaces'
types); Typeable: the solver's evidence for a type without variables (`mkTrCon $tcT []`, `mkTrApp`), the module's
`$trModule` and `$tcT` bindings with GHC's fingerprints (`GHC.Internal.Fingerprint`: MD5 of each string's characters as four
big-endian bytes, then of the three fingerprints: `$tcBoom` equal to GHC's, `17929780780547902510 8657790650026734023`);
existential constructors (`data T = forall s. C s => T s`: the context's dictionaries the worker's first fields, skolems
and givens in the pattern); functional dependencies' improvement before defaulting (`MonadState s m | m -> s`); a class with
a quantified superclass counted as a field (`MonadTrans`); a newtype class's dictionary the identity; HasCallStack's
`SrcLoc` file the source's path; `default` declarations (§4.3.4); fractional literals at other types `fromRational` of a
reduced `n :% d`; non-ASCII string literals `unpackCStringUtf8#` (modified UTF-8); generated code's `Int` literals boxed;
deriving's tags as GHC's `dataToTag#` (`dataToTagLarge#`, boxed) and tags' constructors a case on the literals (HF4's
`DerivTag` had typed them without a run: the derived `==` of a type with a field compared the values, not their tags).

**5. Interfaces** (Tier S's data, HFS3 (a)): the helper's `iface` closure now also follows the modules of the type
constructors its things' types mention and the orphan modules below each interface (`dep_orphs`: `Num CSize`,
`FormatTime UTCTime`), and a reexported module (`GHC.Prim` is `ghc-internal`'s `GHC.Internal.Prim`) is read through
`reexports.edn` (ghc-pkg's `exposed-modules` reexports), in the renamer.

**6. The driver and the run check** (HF7's first light; decision 3 (a)): `arbace/repr/haskell/front.ae`, the front end as
one process for many programs (native read, rename, elaborate, desugar, the simple optimiser, the constant folder, STG; the
interfaces read once), and `bin/repr-haskell run-check` (`make repr-haskell-run-check`): the samples and synthetic programs
of `test/haskell/eval/` through it in four shards and on hsi, each against GHC 9.14.1's `-O0` run recorded in
`test/haskell/ref-ghc914.edn` (status, stdout, stderr), against the record `test/haskell/run-check.edn` (`--record`;
`test/haskell/run_record.ae` judges, every non-pass in a class), the negative control (each run against the next program's
reference) failing. No GHC at build time but the interfaces' dump. About 2 minutes, at most 1.6 GB.

**Results** (the brief's measure: the run corpus's programs that run from Arbace's own STG):

| corpus | before (HF5-HF6's head, `haskell-hf4` merged) | after |
|---|---:|---:|
| `test/haskell/stg/` (stg-check) | 9 of 9 | 9 of 9 |
| `test/haskell/eval/samples` | 12 of 26 | 23 of 26 |
| `test/haskell/eval/synthetic` | 4 of 37 | 37 of 37 |
| run-check (samples and synthetic) | 16 of 63 | **60 of 63** |

The three others fail on GHC's own STG under the same cap (`hsi-memory`: `Loop`, `runtime-deep`, which
`test/haskell/check-hsi.edn` passes at 16 GiB; `hsi`: `AsyncRerun`, a failure of check-hsi too): every program hsi runs
from GHC's STG now runs from Arbace's. The judges of HF5-HF6 (`bin/repr-haskell stg-check`): desugared Core equal to the
tc dump in 171 of 227 bindings of the typecheck samples (158 before) and 54 of 85 of the programs (48); STG equal to GHC's
`-O0` dumps in 65 of 83 bindings (62; S01, S02, S07, S08 entirely); `bin/repr-haskell typecheck-check` 22 of 22, its control
differing. The STG judge's remaining classes are the Simplifier's shapes (2 above): a newtype-wrapped function's binding
split by its cast and eta-expanded (`get = \eta -> get1 eta`), local functions inlined, floats' order.

**7. Beyond the run corpus: nofib** (the next stage's first slice measured on programs no one wrote for it):
`bin/repr-haskell run-check --nofib` runs the 53 single-module nofib programs of HF3-HF4's check list
(`test/haskell/run-check-nofib.txt`; shake's three-module program and the two `callback` programs with C of their own left
out) from the pinned GHC tarball's `nofib/` in nofib's fast mode (each Makefile's `FAST_OPTS` and `PROG_ARGS` less `+RTS`
options, `NAME.faststdin` or `NAME.stdin`), against GHC's `-O0` runs recorded once with the host's GHC
(`test/haskell/ref-nofib.edn`, `--record-ref`: the tarball's nofib is not hsi's pinned nofib commit, whose runs
`ref-ghc914.edn` holds), record `test/haskell/run-check-nofib.edn`. **40 of 53 run with GHC's output**; the 13 others in two classes: `hsi` (12: hsi
fails on GHC's own `-O0` STG of the program the same way under the light caps, 2 GiB and 120 s of CPU: `cryptarithm1`,
`last-piece`, `kahan`, `x2n1`, `maillist`, `fannkuch-redux`, `k-nucleotide`, `spectral-norm`, `stm001`, `stm002`,
`threads003`, `threads005`), `space` (1, `imaginary/integrate`: GHC's `-O0` Simplifier inlines the once-used recursive
CAFs `rtotals` and `itotals` into `es`, the front end keeps them top-level, so more is retained; GHC's STG of it passes).
About 25 minutes (the references once, about 10 more), at most 1.6 GB a process. The nofib programs found five
front-end bugs the run corpus had not: the derived `==` of a type with a field compared the values where GHC compares
the tags (HF4's `DerivTag` typed without a run), `default (Integer, Rational, Double)` ignored, a top-level pattern
binding's selectors returning themselves (`((kmin, kmax), ...) = grid_size`: `<<loop>>`), `MonadTrans`'s quantified
superclass not counted as a dictionary field, `MonadFail`'s message a bare literal; each fixed with GHC's rule.

**Not covered yet**: multi-module programs in the driver (the renamer and the elaborator read a module's own interface,
`--local`, but `front.ae` compiles one module a program), the `-O0` Simplifier's other effects (2 above), eta reduction,
`n+k` patterns, view patterns, `foreign import` calls' STG (`ffi`), `Typeable` of types with kind variables, GADTs and type
families (HF9), Template Haskell; the libraries stay GHC's STG (Tier S).

**Proposed** (the user decides).

1. The gate (HF5-HF6's decision 4, HF4's decision 2): `run-check` beside `typecheck-check` in the step `haskell-front`.
   Options: (a) the run corpus (63 programs, about 2 minutes, at most 1.6 GB a process, the libraries' STG of the oracle
   pack pinned by its digest) in the essential gate and `--nofib` (53 programs, about 25 minutes) in `--full`; (b) both
   in `--full`; (c) wait for HF8. *Recommended: (a)*, once `haskell-integrate` is on main (decision 4's condition).
2. GHC's `-O0` Simplifier run (the finding of 2). Options: (a) a recorded class for the STG judge's shapes and the
   `space` class, only the rules that change results ported (done); (b) port the Simplifier in `-O0`'s mode
   (`GHC/Core/Opt/Simplify/*`, `OccurAnal`, `Arity`, `Unfold`: about 16,000 lines, most of its decisions typed: it would
   need GHC's typed Core, HFS4 (b)); (c) port the parts the judges and the space class ask for, one at a time: the inlining
   of a once-used recursive top-level group into its use, eta expansion through a newtype's cast (`get = \eta -> get1
   eta`), the local functions' inlining. *Recommended: (a) now, (c) as the nofib corpus or the STG judge asks*.
3. The nofib references. Options: (a) keep the tarball's nofib with its own recorded GHC runs (`ref-nofib.edn`); (b)
   move to hsi's pinned nofib commit and `ref-ghc914.edn` when its checkout is fetched (a network fetch; its programs
   differ from the tarball's in places); (c) both. *Recommended: (a)*: the tarball is repr/haskell's pin already.
4. The light runs' caps (`hsi`, `hsi-memory` classes: hsi on GHC's own STG fails the same way at 2 GiB and 120 s).
   Options: (a) run-check takes `ARBACE_MEM_GIB` and `HSI_TIMEOUT` as `bin/hsi check` does, the gate's night run at 16 GiB
   and 60 s (the classes then empty or smaller); (b) keep the light caps everywhere. *Recommended: (a)*.
5. Next: the multi-module driver (nofib's 100 other programs, the testsuite's), then the elaborator's next extensions as
   those corpora order them (GADTs and type families wait for HF9). *Recommended* as the order of HF3's decision 5.

## Decided (2026-11-06, the user: HF5-HF6's second slice and HF7's proposals)

1. (a): `run-check` (the run corpus, 63 programs) in the essential gate and `run-check --nofib` (53 programs) in `--full`,
   once `haskell-integrate` is on main; the steps are prepared on the branch, not added to main's gate.
2. **The full port of GHC 9.14.1's `-O0` Simplifier now** (`GHC/Core/Opt/Simplify/*`, with OccurAnal, Arity's eta expansion
   and arity analysis, Unfold's inlining decisions and sizes, the rules `-O0` has, the casts and coercions as far as the
   elaborator's typed Core carries them), not piece by piece: behind a flag first, the default when the judges agree;
   measured by stg-check (Core and STG equal to GHC's) and run-check / `--nofib` (runs equal; `imaginary/integrate` to pass).
   The elaborator's output is extended where the Simplifier needs types (part of the port).
3. (a): the tarball's nofib with its own recorded GHC runs (`test/haskell/ref-nofib.edn`).
4. (a): run-check takes hsi's memory and time caps (`ARBACE_MEM_GIB`, `HSI_TIMEOUT`), 16 GiB at night.
5. The multi-module driver (HF9's first part) comes after the Simplifier, by another agent.

## As built: the -O0 Simplifier (2026-11-06, branch `haskell-simpl`, from `haskell-hf7`)

The user's decision 2 above: GHC 9.14.1's Simplifier as `-O0` runs it (`getCoreToDo`'s `simplify "final"`: one
`CoreDoSimplify` of up to 4 iterations, `maxSimplIterations`), ported rule for rule from the pinned tarball's
`compiler/GHC/Core/Opt/` (read in the frontend survey's extraction) into three namespaces, behind
`REPR_HASKELL_SIMPL=1` (`tostg/module-stg` runs it after the desugarer's simple optimiser; `REPR_HASKELL_SIMPL=0`
the earlier pipeline):

- `arbace/repr/haskell/simplify.ae`: `Simplify.hs` (`simplifyPgm`'s iterations to a fixed point, `shortOutIndirections`),
  `Simplify/Iteration.hs` (`simplTopBinds`, `simplRecBind`, `simplLazyBind`, `simplJoinBind`, `completeBind`,
  `prepareBinding`, `prepareRhs`, `makeTrivial`, `tryCastWorkerWrapper`, `simplExprF` and the continuations
  `ApplyToVal`/`CastIt`/`Select`/`StrictBind`/`StrictArg`, `simplLam` and the beta reductions, `simplNonRecE`,
  `completeBindX`, `simplAuxBind`, `simplInId`/`simplOutId`, `rebuildCall` with the demands of a dead-end Id,
  `simplCast` with `addCoerce` and `pushCoValArg`, `rebuild`, `rebuildCase` with case of known constructor
  (`knownCon`), case-to-let and the plain seq, `reallyRebuildCase`, `simplAlts`, `simplAlt` with the alternatives'
  and the binder swap's unfoldings, `mkDupableCaseCont`/`mkDupableCont` with Plan A and B of the strict argument and
  the join points `$j` of `mkDupableAlt`/`mkDupableStrictBind`, `simplLetUnfolding`, `simplStableUnfolding` (DFun
  unfoldings simplified), `wrapJoinCont`, `trimJoinCont`); `Simplify/Utils.hs` (`SimplCont`, `ArgInfo`,
  `mkArgInfo` with the type's strictness, `interestingCallContext`, `interestingArg`, `contArgs`,
  `preInlineUnconditionally`, `postInlineUnconditionally`, `rebuildLam` with eta reduction, eta expansion and the cast
  swizzle, `tryEtaExpandRhs`, `wantEtaExpansion`, `prepareAlts` with `filterAlts` and `refineDefaultAlt`, `mkCase`'s
  identity case); `Simplify/Env.hs` (the substitution `DoneId`/`DoneEx`/`ContEx`, the in-scope set with
  `refineFromInScope`, `substIdBndr` with `uniqAway` and `zapFragileIdInfo`, `SimplFloats` with the float flags and
  `doFloatFromRhs`); `Simplify/Inline.hs` (`callSiteInline`, `tryUnfolding`, `computeDiscount`, the case-depth
  penalty); the built-in rules `-O0` keeps (rewrite rules are off, `matchRule` cannot switch built-in ones off): the
  class-op rule of `GHC.Types.Id.Make.mkDictSelId` against the module's DFun unfoldings, ConstantFold's primop rules
  (`constfold.ae`: the floating-point ones, and now the `Int#`, `Word#` and `Char#` ones `-O0` keeps: `binaryLit`, the
  identities, `zeroElem`, `leftZero`, `equalArgs`, the comparisons of literals and of equal arguments, negation, the
  conversions; the `NumConstantFolding` ones are off at `-O0`) and its `dataToTagRule` on a known constructor. The SimplMode is `-O0`'s
  (`initSimplMode`): inlining on, eta expansion and reduction on, case-of-case on, cast swizzling on, case merging and
  case folding off, let floating to the top level on, the phase final.
- `arbace/repr/haskell/occanal.ae`: `OccurAnal.hs` as the Simplifier runs it before each iteration
  (`occurAnalysePgm` glomming the top level, `occAnalBind`, `occAnalRecBind` with `makeNode`, `tagRecBinders`,
  `loopBreakNodes`/`reOrderNodes`/`chooseLoopBreaker` and `nodeScore`, `occAnal`, `occAnalApp` with the binder swap,
  `occAnalLamTail`, `occAnalUnfolding` (a DFun's arguments and a stable unfolding counted), join points by tail calls
  (`tagNonRecBinder`, `decideRecJoinPointHood`, the join points' zeroed usage at their jumps), UsageDetails with the
  many/in-lambda/non-tail zappings, dead code dropped) and `occurAnalyseExpr` for the unfolding templates.
- `arbace/repr/haskell/core.ae`: `Core/Utils.hs` (`exprIsTrivial`, `exprIsDupable`, `exprIsCheapX` with
  `isWorkFreeApp`/`isCheapApp`/`isExpandableApp`, `exprOkForSpeculation`/`exprOkToDiscard`, `exprIsHNF`/`exprIsConLike`,
  `cheapEqExpr`, `mkCast`), `Opt/Arity.hs` (`typeArity` through newtypes, `ArityType`, `arityType`, `findRhsArity`'s
  fixpoint for recursive bindings, `exprEtaExpandArity`, `etaExpandAT` with `mkEtaWW`, `etaInfoApp` and `etaInfoAbs`
  (the newtypes' coercions pushed through the expansion), `tryEtaReduce`, `exprArity`, `exprIsDeadEnd`,
  `cheapArityType`, `pushCoValArg`), `Unfold.hs` and `Unfold/Make.hs` (`sizeExpr` with GHC's sizes and discounts,
  `calcUnfoldingGuidance`, `uncondInline`, `uncondInlineJoin`, `inlineBoringOk`, `mkUnfolding` with its cache, the
  template and the guidance computed when first asked as GHC's lazy fields), `SimpleOpt.hs`'s `exprIsConApp_maybe`
  (through casts, lambdas, lets, DFun and expandable unfoldings); the primops' attributes from
  `compiler/GHC/Builtin/primops.txt.pp` (`primops.ae`, generated by `tools/hsprimops/primops.awk`).

**The Core it reads** (the elaborator's output extended where the Simplifier needs it, as decision 2 allows): the
newtypes' casts kept (`(cast E {:l T1 :r T2})`, a coercion by its two kinds: a newtype constructor's compulsory
unfolding `\x -> x |> sym co`, its match `x |> co`; `desugar/*casts*`), GHC's failure handlers `\void -> e` applied to
`(##)` (`mkFailurePair`), `dsAbsBinds`' letrec for one self-recursive binder without a signature, the simple
optimiser's dead code (a dead top-level group's occurrences not counted: the once-used letrecs inlined as GHC's
desugarer inlines them), the module's DFun unfoldings, record selectors one a field with `recSelError`'s DEFAULT,
Core's let-can-float invariant (an unlifted let not ok for speculation a case); the binders' IdInfo is their forms'
metadata (`:occ :join :arity :unf :oneshot :type :bot`), the imported Ids' from GHC's implicit Ids (constructor
workers, class-op and superclass selectors, primops, DFuns, the wired-in error Ids with their dead-end arity) and none
else (`-fignore-interface-pragmas`). After it, CorePrep's eta expansion to the binders' arity (`simplify/strip`),
the casts erased, top-level string literals (`StgTopStringLit`), the join points let-no-escape, a strict
constructor's non-atomic arguments case-bound with their fields' types (`tostg.ae`).

**The judges.** New: the simplified Core against GHC's (`arbace/repr/haskell/simplcmp.ae`: the helper's `stg --core`
Core after GHC's Simplifier, both sides in one normal form, types, casts and ticks erased, names alpha-normalised by
binding site; `bin/repr-haskell stg-check` prints it per program and in total). The measures (before = this branch's
start, the simplifier off; after = on):

| judge | before | after |
|---|---:|---:|
| simplified Core = GHC's -O0 Core, test/haskell/stg's programs | 37 of 61 | 53 of 61 |
| simplified Core = GHC's -O0 Core, test/haskell/typecheck's 22 samples | 261 of 321 | 300 of 321 |
| STG = GHC's -O0 STG (stg-check) | 65 of 83 | 80 of 83 |
| stg-check runs | 9 of 9 | 9 of 9 |
| run-check (the run corpus) | 60 of 63 | 60 of 63 (63 of 63 as recorded) |
| run-check --nofib (light caps) | 40 of 53 | 39 of 53 (53 of 53 as recorded) |

The desugared Core judge (stg-check's typecheck samples) went from 171 to 173 of 227 (dsAbsBinds' letrec, sections,
selectors); typecheck-check stays 22 of 22. The one nofib change is `imaginary/queens`, now in the class `hsi`: it runs
in 109 s of CPU from our STG against 119 s from the earlier pipeline's on the loaded host, both against the 120 s cap
(the same output given 600 s). `imaginary/integrate`, the `space` class (now `hsi`, the class retired): the Simplifier's input now inlines `rtotals` and `itotals` into `es` as GHC's
desugarer does; its run's peak memory is GHC's own STG's (804 MB against GHC's 792 MB, from 993 MB), so it fails
under the light 2 GiB cap exactly as GHC's STG does (the class `hsi-memory`) and passes with GHC's output at 4 GiB:
with decision 4's night caps it passes.

**The differences left** are the elaborator's and the desugarer's, not the Simplifier's: GHC's desugarer floats a
module's constant dictionaries to the top level (`$dShow = $fShowList $fShowInt`, shared), binds a section's operator
and operand outside its lambda, generalises local bindings over their constraints (`parity`'s
`\$dEq $dNum -> letrec ...`), matches string literal patterns with `eqString`, derives Ix and Enum on `dataToTag#` and
the tag primops (ours on `I#` and `ltInt`) and makes the Ix instances top-level; CorePrep floats a scrutinee's argument
lets past a join point (`grade_sat_80`).

**Not ported** (no `-O0` program of the corpora needed it, or the Core lacks what it reads): the other built-in rules
(the narrowings', shifts' and sized integers' folding, the string rules `unpackFoldrCString#`/`eqString`/`unpackAppendCString#`, `tagToEnum#`'s
rule, which needs the erased type argument, `seq#`, `inline`), `runRW#`'s special case, `improveSeq`,
`mkStrictFieldSeqs` in the join points, `combineIdenticalAlts`, `abstractFloats` (the types are erased), INLINE and
NOINLINE pragmas and stable unfoldings of the user's, the demands (`-O0` has none but the types' and the dead ends'),
the coercions' optimiser (`optCoercion`: a coercion is its two kinds; reflexive when they are equal), the newtypes'
coercions with their type arguments (left uninstantiated: an arity through a type variable instantiated to a function
is not seen).

**Proposed** (the user decides).

1. The evidence bindings' placement (the remaining Core and STG differences of the judges): GHC's typechecker binds a
   module's constant dictionaries at the top level (shared, the simple optimiser inlining the once-used) and a signed
   binding's at its body's top (`collatz`'s `$dReal = $p1Integral $fIntegralInt`), where the elaborator writes each
   dictionary term at its use. Options: (a) the elaborator's evidence bindings placed as GHC's solver places them
   (top-level implication, signature implications), a slice of HF3; (b) a desugarer post-pass hoisting closed
   dictionary terms (approximate); (c) leave them (the runs are equal). *Recommended: (a)*, with the next elaborator slice.
2. The derived Ix/Enum/Ord code on `dataToTag#` and the tag primops as GHC's `Deriv/Generate.hs` writes them
   (`T20Derive3`'s `cmp`, `weekend`, the Ix instances top-level): HF4's generators. *Recommended* in HF4's next pass.
3. The nofib record at the night caps (decision 4): once a night slot allows, `ARBACE_MEM_GIB=16 HSI_TIMEOUT=60
   bin/repr-haskell run-check --nofib --record` to empty the `hsi` class (integrate and queens pass at 4 GiB / 600 s).

## Decided (2026-11-06, the user: the Simplifier's proposals 1-3)

- **1 (a):** evidence bindings placed where GHC's solver places them (constant dictionaries shared once), in the next elaborator slice.
- **2 (a):** HF4's derived code uses `dataToTag#` and the tag primops, as GHC's generators do.
- **3 (a):** nofib re-recorded at the night caps (16 GiB, 60 s) to empty the `hsi` class.

## Decided (the user, 2026-11-07)

Relayed by the main session with the brief of the branch `haskell-90` (the crop above 90% on `bin/crops-score`'s three
measures, doc/CROPS-SCORE.md on `mac/crops-score`):

- **The judge.** A test's own verdict counts as a judge no toolchain made (score 1): the exit status, expected output shipped
  with the test (GHC's testsuite's `.stdout` and `.stderr` files, nofib's `.stdout`), the round trip's reread forms equal.
- **A mixed stage** scores the case-weighted mean of its cases' 1 (own verdict) and 0.5 (a recorded toolchain reference).
- **A pinned input corpus** in the crop's own language counts 1 when no toolchain output about it is a reference.
- **Toolchain runs** move to the oracle tier (by hand, at a tag).

## As built: the crop's independence (2026-11-07, branch `haskell-90`, from `haskell-simpl`)

Under the day's light rules: main's stage 0, sample checks, every process under 1.5 GB. The stub runs had `ghc ghc-pkg
runghc ghci hsc2hs gcc cc clang` first on `PATH` as scripts logging their call and exiting 99 (doc/CROPS-SCORE.md, "The stub
runs"); after each run the log was empty.

**What came in.** `haskell-eval9` (hsi's HE9: allocation counters, the RTS's options) was not merged: its merge with this line
conflicts in eleven files of hsi's runtime (`go/arbace/hs/{machine,nodes,rts,mem,prim_ctl}.ae`, the driver, `bin/lib/hsi/check`,
the records), both lines having built HES60 and HES64 (a program's own C, the foreign stubs) independently; not small (proposal
HFS-I4). From `mac/oracle-records-cpp2` the shared oracle-records tooling and Haskell's ORS12 commits were cherry-picked
(`c3892e875` `8e5ce6196` `1bf41517c` `54f5e5313` `62e96e5d9` `2b03a6bdd`, `-x`; `oracle.bash` taken whole from that branch's
head; `prog_key` kept as recorded: a program's own C is not in its STG's key). From `mac/haskell-cpp` its net change over
`53794ed6^` was applied (unlit, the driver, the clean-room `cpp.ae`, the native mode `:full`, the helper's version 5): the tree
it gives holds no GPL code (`4b16a392b` replaced the port), and no commit containing `53794ed6` is in this branch's history.

**Read: the native reader as the source of the read stage's forms.** `bin/repr-haskell read --corpus` and `roundtrip` (its tree,
layout and text levels) read with `arbace.repr.haskell.native` (`REPR_HASKELL_READER=helper`: the helper's dumps, the oracle's).
GHC's installation, the configure products and hsc2hs's outputs, which the reads of 528 of the 1,345 modules still consume, are
the pack `haskell-native-inputs-ghc-9.14.1` (doc/haskell/NATIVE.md, "The native reads' inputs"); `cpp.ae` reads absolute paths
under a sysroot (`*sysroot*`, `--sysroot`), the records naming the host's paths as before. The recording (`bin/repr-haskell
record-native-inputs`, a live native read of the whole corpus in both modes): 1,345 modules each mode, none failing, 7 min 39 s
at four jobs, at most 1.4 GB.

**The judge: the round trip's own verdict.** The tree level (and the layout level) reads the printed modules again natively and
compares their forms with the originals' (`arbace.repr.haskell` `compare-forms`; the module form's records of how its text was
made, `:config :cpp :hsc2hs :unlit`, left out: the print is CPP's output), known differences in
`test/haskell/roundtrip-ghc-9.14.1{,-layout}-native-known.edn`; the text level compares the print with the original text (a
CPP module's module view, which the native reader now writes as `FILE.view`) byte for byte, no tree made (`compare-text
--bytes-only`). The interface level (GHC compiling the print) and the helper's trees are the oracle tier's
(`test/gate/crops/haskell.edn`: `haskell-roundtrip-oracle`, `REPR_HASKELL_READER=helper`). Each level has its negative control
(GTS81 (a)): `test/haskell/roundtrip-controls.edn`'s pairs, a module's reread forms (print, interface) judged against another
module's reference, and each judged module against the next; one equal pair, or none judged, fails the level. The printer's
one fix the stricter judge found: `{-# OPTIONS_HADDOCK not-home #-}` printed with its text's own spaces (the reread string was
`"  not-home  "`).

| stub run (samples) | result |
|---|---|
| `bin/repr-haskell test` (the records re-recorded at the helper's version 5, `--record`, 74 files) | 18 ok, 22 GHC checks skipped (class D, `--oracle`), 34 s |
| `roundtrip --text --layout` over array, deepseq, containers, unix, directory (113 modules, 47 through CPP, 39 `.hsc`) | tree 113 of 113, layout 113 of 113, text 113 of 113 byte for byte; the controls 8, 8 and 7 pairs differ; 3 min 11 s at four jobs |

**Eval and AOT: hsi over the packs.** `bin/hsi check` runs without GHC (ORS12's path): the boot libraries' STG and the programs'
`-O0` STG from the packs `haskell-stg-libs` and `haskell-stg-progs`; this branch adds to the latter the programs' foreign stubs
(the C GHC writes for a `foreign export` or a `"wrapper"` import, `ID/stubs/`, `NONE` when GHC wrote none) and takes the RTS's
headers for a program's own C from the native-inputs pack's sysroot, so `stubs-build` (a GHC build of the probe) runs only
live. `haskell-stg-progs` recorded afresh (`bin/hsi check --live --dump-only --record-progs --corpus
nofib,samples,synthetic,testsuite-core --jobs 8`, 4 min 6 s): 908 programs, 849 dumped, 59 GHC's own build errors recorded as
such. `haskell-stg-libs` is STALE on this line (its members name the helper they were made by: the helper changed with HE7's
verbs and version 5): its re-recording is the night's (`bin/hsi dumps --record`); the day's sample ran with `HSI_LIBS` naming
the pinned pack's unpacked members.

| stub run (samples, `HSI_LIBS` the pinned pack's members, 2 GiB) | result |
|---|---|
| `bin/hsi check --only` 12 programs (samples, synthetic, testsuite: the controls' subjects and five of the essential list) | 12 of 12 pass (4 by their own verdict), the 4 controls fail as they must, 5 s |
| the same with three programs with C of their own (`T12622`, `ffi020`, nofib's `callback001`: ci, the recorded stubs) | as recorded (`ffi020` passes; the other two in their classes `:rts` and `:no-reference`); with `.tmp/ci-musl` absent ci's `bin/ci musl ensure` configures musl with gcc (the stub was called): on this line the 87 programs with C need it prepared; `mac/batch1`'s ci (c-indep-b1: musl without a compiler) removes that at the merge |

**The judge: the tests' own verdicts.** `bin/hsi own-verdicts` lists the programs whose shipped expectations (all.T's
`exit_code` and `NAME.stdout`, `NAME.stderr`, a missing one empty, as the testsuite driver takes them; nofib's `NAME.faststdout`
or `NAME.stdout`) are exactly GHC's recorded run: 580 of 908 (527 of the testsuite's, 53 of nofib's), `test/haskell/own-verdicts.tsv`.
`bin/hsi check` judges those by the shipped files alone (`own_ref`; `:judge :own` in the result), the record not read; the others
by GHC's recorded run. Since the list holds only the programs on which both agree, every verdict is the same either way
(`HSI_JUDGE=ref`: all by the record, the oracle tier's comparison; `own-verdicts --check`: the list against the trees and the
record, in `hsi-essential`). The negative controls judge a subject against a reference's own verdict when it has one.

**The gate.** Steps in `test/gate/steps.edn` (`:crops`' `:haskell`), none on `:ghc`: `haskell-read-essential` (`bin/repr-haskell
test`, the round trip of array, deepseq and containers at the tree and text levels) and `hsi-essential` (`own-verdicts --check`,
the 123 programs of `test/haskell/hsi/essential.txt`) in the essential gate; `haskell-roundtrip` (the whole corpus, tree, layout,
text), `hsi-check` and `hsi-check-aot` in `--full`. `test/gate/crops/haskell.edn` keeps the oracle tier's steps alone
(`haskell-test-oracle`, `haskell-roundtrip-oracle`, `haskell-native-oracle` new: `tables --check`, `native-check` in both modes,
`HSI_JUDGE=ref` `own-verdicts --check`; `haskell-records-verify`, with the new kind `native-inputs`). `bin/gate --plan` reads them.

**Scores** (`bin/crops-score --ref REF --crop haskell` from a scratch worktree of `mac/batch1`, with this branch's facts and
control rules, `.tmp/independence.diff` and `.tmp/crops-rules.diff` of its report):

| ref | completeness (without by design) | maturity | independence (read, eval, AOT, print, judge) |
|---|---:|---:|---:|
| `mac/haskell-hf7` (the brief's baseline) | 96.3 (98.7) | 73.3 | 20.0 (0, 0, 0, 1, 0) |
| `haskell-90` | 96.3 (98.7) | 100.0 | 73.8 (0.804, 0.452, 0.493, 1, 0.942) |

Independence stays under 90 while every hsi run consumes the libraries' STG GHC made (eval and AOT 0.5 at most: HFS-I1 with a
native compile of the boot libraries is what makes them 1) and the read stage's 528 preprocessed modules read the pack (HFS-I2).
The 87 programs with C score 0 on this line alone (ci's musl configured by gcc), 0.5 after the merge with `mac/batch1`'s ci.

**Left** (the night's, one check process at a time, at most 1.5 GB a process): `bin/hsi dumps --record` (the libraries' STG at
this helper; then `hsi-essential` and `hsi-check` pass their pins), `bin/repr-haskell roundtrip --text --layout` over the whole
corpus (writes the native known files' first entries, if any differ), `bin/hsi check --corpus nofib,samples,synthetic,testsuite-core
--record` and `--aot --record` on the packs (the records' lines gain `:judge :own`; T16846 alone needs 10 GB: `HSI_MEM_CAPS`), then
`bin/reap --record`. The native front end (HF7's run-check, 60 of 63 programs, and nofib's) is not yet hsi check's source of STG
(HFS-I1).

## Proposed (HFS-I1 to HFS-I4; options with costs, a recommendation each, none decided)

- **HFS-I1 The native front end as hsi check's source of STG.** (a) `bin/hsi check --front native`: the programs the front end
  compiles (single-module, no GADTs or type families yet) run from Arbace's STG, the others from the pack, each result naming its
  source; the front end's interfaces from a pack `haskell-ifaces` (the helper's `iface` closure of every program's imports, about
  11 MB for the run corpus), the default once a night's run shows no pass lost; about a day. (b) Keep the pack's STG until the
  front end compiles the testsuite's multi-module programs (HF9). (c) Both records side by side (OCaml's TY19 (b)). The stage
  stays 0.5 in any case while the libraries' STG is GHC's. *Recommended: (a)*, after the night's records.
- **HFS-I2 The native inputs at 1.** The read stage's 528 preprocessed modules consume the pack (0.5). (a) Derive the configure
  products and the installation's generated headers from the target's facts (sizes, features: a few dozen `#define`s), the package
  database from the corpus's `.cabal` files, and run hsc2hs's C program on ci; about two days; read 80% → 100%. (b) Keep the pack.
  *Recommended: (a)* for the configure products and the database first (they are data), hsc2hs after.
- **HFS-I3 The interface level in the read cell.** `roundtrip --iface` compiles the print with GHC: it is the oracle tier's now, but
  the read cell still counts its 1,345 modules from the known file. (a) Mark the source `:role :beside` (counted apart, as the
  layout's); (b) keep it in the cell, recorded at each tag's oracle run; (c) replace it by the native front end's interfaces of
  both sides (HF2's renamer and HF3's elaborator: types and exports of the original and the print compared) once they cover the
  corpus. *Recommended: (b)* now, (c) as the front end grows.
- **HFS-I4 `haskell-eval9`.** Its merge conflicts in hsi's runtime (two builds of the same C support). (a) A merge by an agent
  of its own: hsi's forms merged as Go (`git merge-file` per printed file, as `haskell-integrate` did), eval9's HE9 kept, this
  line's stubs and packs kept; about half a day; check-hsi 806 of 910 on that line. (b) Rebase HE9's commits alone (allocation
  counters, RTS flags) onto this line. *Recommended: (b)*: HE9 is self-contained, the C support of both lines is the same design.

## Decided (the user, 2026-11-07: HFS-I1 to HFS-I4)

- **HFS-I1 (a):** `bin/hsi check --front native`, the front end's STG for the programs it compiles, the pack's for the others, each
  result naming its source; the default once a night's run shows no pass lost (branch `hfs-i1`, after `res34`'s RES34).
- **HFS-I2 (a):** the configure products and the package database derived (data first), hsc2hs's C program on ci after.
- **HFS-I3 (b):** the interface level stays in the read cell, recorded at each tag's oracle run; (c) as the front end grows.
- **HFS-I4 (b):** HE9's commits (allocation counters, RTS flags) rebased alone onto this line; `haskell-eval9` not merged.

### As merged: batch 2 (2026-11-07, branch `batch2`)

haskell-90 merged on batch 2 (no commit containing 53794ed6; its CPP the clean-room one). The packs `haskell-native-inputs-ghc-9.14.1`
and the re-recorded `haskell-stg-progs` copied into the main checkout's `.tmp/oracle-packs/` (`bin/oracle-records pins`: the seven
Haskell packs in the store). `test/reap/main.ae`'s one `:corpus-known` reader counts `:hs-modules` beside Rust's units; the facts moved
into `:stages`, ci's C cases 0.5 now that batch 1 derives musl without gcc. Scores 96.3 / 100 / 74.9 (batch 2 before: 0 / 8.3 / 20).

## As built: HFS-I1 (a) (2026-11-07, branch `hfs-i1`, from `haskell-90` 0fb4591bf)

By an agent on the main host by day (the light tier: no gate, no build of Arbace, every process under about 2 GiB; stage 0
`/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated with `ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1`;
`HSI_LIBS` the unpacked `haskell-stg-libs`, whose keys are stale at this helper, "As built: the crop's independence": the night's
`bin/hsi dumps --record`).

### What was built

| piece | files | what |
|---|---|---|
| the front end as hsi's source of STG | `bin/lib/hsi/check` (`--front native\|ghc`, `HSI_FRONT`; `default_front`) | before the runs, each program the front end can take compiled by `arbace.repr.haskell.front` in `HSI_FRONT_SHARDS` processes (default 4, Go's heap near `HSI_FRONT_MEM`, 1536MiB); `one` runs it from `front/out/stg/ID`, else from `haskell-stg-progs` as before; each result carries `:stg :native`, or `:stg :ghc` with `:front` the stage that stopped the front end (`:ineligible`: not tried); the summary counts both and the passes from native STG |
| eligibility | `front_eligible` | kinds `plain`, `compile_and_run`, `nofib` (one main file); every module the main file imports a library's (its interface in the pack, or a package's reexport: `GHC.Prim`); no C of its own (`.c`, `.o`, `-optc`), no CPP, no `foreign export`, `"wrapper"` or `capi` import (their stubs are GHC's): 694 of the 908 programs before the two rules below (26 samples, 37 synthetic, 567 testsuite-core, 64 nofib; `HSI_FRONT_DRY=1` lists them) |
| the flags | `arbace/repr/haskell/front.ae` (`build --plan FILE`) | each program with its own flags (its all.T `-X` options; none: GHC2021, the language GHC compiled the pack's STG in), not run-check's one `-XHaskell2010` for all |
| the interfaces pack | `bin/lib/hsi/records.bash` (`hsi_ifaces_dir`, `hsi_record_ifaces`), `test/haskell/records/haskell-ifaces-ghc-9.14.1.{sha256,tsv}` | the helper's `iface` closure of every module the run corpora's main files import (and Prelude), 439 interfaces, with ghc-pkg's reexports (`reexports.edn`): 440 members, 13 MB, 332 KB of xz, keyed by the helper's identity (a new helper: STALE); recorded by hand with GHC (`bin/hsi check --front native --record-ifaces --corpus samples,synthetic,testsuite-core,nofib`, 46 s), the oracle tier |
| the records apart | `--record` | while ghc is the default, `test/haskell/check-hsi-native.edn` (`-aot-native`) beside `check-hsi.edn`; work `.tmp/hsi/check-native` |
| the comparison | `bin/hsi compare-front [--aot] [NATIVE [GHC]]` (`bin/lib/hsi/compare-front`) | the native run against GHC's STG's record program by program: both pass, lost, gained, both fail, the native run's sources; fails on a pass lost from native STG (the switch's condition) |
| knobs | `HSI_FRONT_REUSE=DIR` | an earlier run's front outputs reused, the front end run on the rest |
| the front end's limit | `front_shard` (`HSI_FRONT_TIMEOUT`, default 300 s) | a program that gives no line for that long (a loop) or ends its process is recorded `timeout`, a fresh process takes the shard's rest |
| two front-end fixes (GHC's rules) | `arbace/repr/haskell/simpleopt.ae` (`con-app`), `desugar.ae` (the constructors' `:strict`), `typecheck.ae` (`*strict-binds*`, `HsLet`) | exprIsConApp_maybe does not take a constructor application whose strict field's argument is not a value (the wrapper `$WCon` evaluates it; GHC floats that evaluation, keeping the case does the same); a banged pattern binding (dsStrictBind) forces its whole match before the let's body. `cgrun051` and `T11601` pass from native STG |
| eligibility, two more rules | `front_eligible` | `-fdefer-type-errors` (flags or `OPTIONS_GHC`) and `RULES` pragmas: outside the front end's subset |

### Results (the day's, light: `ARBACE_MEM_GIB=2 HSI_TIMEOUT=120 HSI_JOBS=2 HSI_FRONT_SHARDS=2`)

| run | result |
|---|---|
| `bin/hsi check --front native --corpus samples,synthetic,testsuite-core,nofib --only` 100 programs (the 63 of samples and synthetic, every 20th eligible testsuite-core program, every 8th eligible nofib program), after the three fixes below | 97 eligible, **84 run from Arbace's STG (samples 26 of 26, synthetic 36 of 37, testsuite-core 17 of 29, nofib 5 of 8), 79 of them pass**; 90 of 100 pass in all. Against GHC's STG's record program by program (`bin/hsi compare-front`): **no pass lost but two**, `samples/Loop` and `synthetic/runtime-deep`, which fail at the light 2 GiB cap from GHC's STG too (`bin/hsi check --only` of the two from the pack: status 2, both) and pass in the record's 16 GiB run; no pass gained; the others fail or stop as recorded (AsyncRerun's class, two runs at the 120 s cap, CallConv's primcall in hsi). The front end stops on 13: the reader on 3 nofib programs (`parse error on input of`, `,`, a varsym: digits-of-e1, ray, lift, programs GHC rejects at `-O0` too: no-dump in the pack), the elaborator on 8 (n+k patterns, monad comprehensions, `HsAppType`, three unifications, an undeclared constructor's type), the STG stage on 1 (`primops-word`: a `StackOverflowError`), 1 timeout (`AtomicPrimops`, below); each runs from GHC's STG. 15 minutes, the largest process 1.57 GB, at most two front processes and two runs at a time |
| the earlier slice (before the fixes) | 4 passes lost from native STG, each a front-end bug or a program outside its subset: `cgrun051` (a strict field's argument `error "OK"` dropped by the simple optimiser's case of known constructor), `T11601` (a banged pattern binding `let !(Just (Just y)) = ...` not forced), `T13838` (`-fdefer-type-errors`: GHC's run-time type error, in GHC's words), `tcrun022` (a `RULES` pragma GHC's `-O0` applies); and a front process that never returned on `AtomicPrimops` |

### The night command and the switch

The night (one check process at a time, at most 1.5 GiB a process; `. .tmp/env.sh`: stage 0, the checkout's sources evaluated):

1. `bin/hsi dumps --record` first (the libraries' pack at this helper; "As built: the crop's independence"'s first night item).
2. `HSI_FRONT_SHARDS=1 HSI_FRONT_MEM=1200MiB bin/hsi check --front native --corpus nofib,samples,synthetic,testsuite-core --jobs 1
   --record` (about 690 programs through the front end, 2 to 8 s each, the primops programs up to 50 s: about an hour of one process,
   plus the runs as `hsi-check`'s; the front process at 1.57 GB by day under 1536MiB: 1200MiB holds the night's 1.5 GiB; writes
   `test/haskell/check-hsi-native.edn`), then `bin/hsi compare-front` against `check-hsi.edn` (recorded the same night by
   `hsi-check`'s command at the same caps), and `bin/hsi compare-front --aot` after `bin/hsi check --aot --front native ... --record`.
3. When `compare-front` reports no pass lost, the switch (one commit): in `bin/lib/hsi/check` `default_front=native`; the three
   steps `hsi-essential`, `hsi-check`, `hsi-check-aot` of `test/gate/steps.edn` gain `test/haskell/records/haskell-ifaces-ghc-9.14.1.sha256`
   in `:pins`; `check-hsi-native.edn` becomes `check-hsi.edn` (and the AOT's), the old GHC-STG record kept as `check-hsi-ghc.edn` (what
   `--front ghc --record` writes after the switch); `test/crops/independence.edn`'s eval and AOT cases split by source (below). The patch
   is `.tmp/hfs-i1-switch.diff` in the worktree, ready for `git apply`.
   A pass lost instead is a front-end bug (the stage and message in `front/front.txt`), fixed or classed before the switch.

### The score

Eval and AOT stay 0.5 per case either way: the program's own STG is Arbace's, but every run still executes the boot libraries' STG
GHC made (`haskell-stg-libs`) against interfaces GHC wrote (the new pack): a case that consumes a toolchain's record is 0.5 by the rule
of 2026-11-07. The independence facts (`.tmp/independence.diff` in the worktree, for batch2's `test/crops/independence.edn`) name the
two sources apart (`:native` and `:pack`, both 0.5), so the score does not move: Haskell stays at 73.8 (read 0.804, eval 0.452, AOT 0.493,
print 1, judge 0.942) until the libraries compile natively.

**What eval at 1 needs**: the boot libraries' STG and interfaces from Arbace's front end, i.e. HF9 (the type-level tier: type families,
GADTs, DataKinds and PolyKinds, QuantifiedConstraints, pattern synonyms, unboxed tuples and levity, `Generic`/`Data`/`Lift`: 20 to 35
days) and HF10 (libraries from source: the module graph and hs-boot cycles, a multi-module driver writing its own interfaces in
dependency order, CPP and hsc2hs on Arbace (HFS-I2), `foreign import` to ghc-internal's C on ci, then `ghc-internal`, `base` ... the 41
packages: 15 to 30 days), with HF11's speed (the 1,345 modules in the night's share: 5 to 10 days). Then neither pack is read: the
front end's interfaces are its own (`--local`), the libraries' STG its own, and eval's and AOT's cases score 1.


**The pack's place.** `haskell-ifaces-ghc-9.14.1`'s tar (`ea93d0a1...`, 332 KB) was made in this worktree's store
(`ARBACE_ORACLE_PACKS=.tmp/oracle-packs`, the day's rule: nothing outside the worktree); before a run on main it goes into the main
checkout's store, `.tmp/oracle-packs/haskell-ifaces-ghc-9.14.1/`, and the mirror (`bin/oracle-records`), as the other packs.

### Proposed (HFS-I5 to HFS-I7)

- **HFS-I5 The front end's stops as data.** 13 of 97 eligible programs stop in the front end (above), each run from GHC's STG, so no
  check fails on them, but they are invisible to the record but for `:front`. Options: (a) **a file `test/haskell/front-known.edn`
  (each program's stage and message, as run-check's classes), `compare-front` failing on a stop that is not in it and on a known one
  that now compiles**; (b) leave them in the results only. *Recommended: (a)*, with the night's first whole run.
- **HFS-I6 The fixes' effect on stg-check.** The two front-end fixes change the desugared Core of a strict field's case and of a
  banged pattern binding; `bin/repr-haskell stg-check` and `run-check` (GHC at build time for the interfaces, so not run by day)
  measure Core and STG against GHC's dumps. Options: (a) **rerun both at night (`make repr-haskell-run-check`), a change in the counts
  classed or fixed**; (b) none. *Recommended: (a)*.
- **HFS-I7 The front end's interfaces from its own pack, then its own.** The pack holds the helper's interfaces of 439 modules (the run
  corpora's import closure). A program importing a library module outside it is ineligible (none of the 908 today). Options: (a)
  **re-record the pack whenever a corpus grows (`--record-ifaces`)**, until HF10 makes the front end write the libraries' interfaces
  itself; (b) the closure of every exposed module of the 41 packages (about 2,000 interfaces, about 60 MB). *Recommended: (a)*.

## As built: HFS-I2 (a) (2026-11-08, branch `hs-inputs`, from `next` bd33e27cc)

The user's decision HFS-I2 (a): the read stage's inputs derived, the configure products and the package database first (data), then
hsc2hs's C program on ci. Built in that order, each part compared byte for byte with the pack `haskell-native-inputs-ghc-9.14.1` (the
oracle tier); doc/haskell/NATIVE.md, "The native reads' inputs: derived", has the members and how each is made.

### What was built

- **`arbace.repr.haskell.target`** (`bin/repr-haskell target derive`): the tree in the pack's layout from the tarball, musl's tarball
  and `test/haskell/target/installation.edn` (the installation's names and choices, each with its reason). The settings file in
  hadrian's order; the 47 package records by **`arbace.repr.haskell.cabal`**, a port of Cabal-syntax 3.16's field parser, conditions,
  free text and `showInstalledPackageInfo` (each unit's `.cabal`, `.cabal.in` or `.buildinfo.in` finalized for x86_64 Linux with
  hadrian's flags), then the bindist's `patchpackageconf`; the units' installed headers by their `install-includes`; ghcversion.h,
  ghcplatform.h, the event headers (gen_event_types.py's rules), HsIntegerGmp.h, ghc-gmp.h, musl's stdc-predef.h.
- **`arbace.repr.haskell.configure`** (`target probe [--verify]`): the seven configure headers by config.status's rewrite of each
  template over `test/haskell/target/answers.edn`, which the probe computes on Arbace's C target: each check's kind from its autoheader
  comment or a rule of the configure.ac (`manual-rules`), headers in musl's tree and `c/include`, functions among libc.a's definitions
  (musl's forms index and a scan of its `src/ldso` and assembly sources), 308 compile tests by ci's checker (`bin/ci diagnose`), the
  constants' and htypes' types by `bin/ci annot` (the checker accepts a pointer in an integer constant expression and a double cast to
  a pointer: the type, not the compile test, decides FP_COMPUTE_INT's failure and FPTOOLS_CHECK_HTYPE's CUIntPtr), four value programs
  by `bin/ci run`. The probe takes about an hour, nearly all of it the compile tests.
- **`arbace.repr.haskell.constants`**: deriveConstants. Its `wanteds` are read from the tarball's `utils/deriveConstants/Main.hs` and
  expanded by the DSL's definitions; the C file of `char derivedConstantX[1 + EXPR]` arrays is checked by ci's checker and the arrays'
  sizes read from `bin/ci annot` where Main.hs reads nm's (building the RTS's headers is refused: SpinLock.h's inline assembly).
- **`arbace.repr.haskell.hsc2hs`** (`target hsc`): hsc2hs 0.68.10's direct mode ported (the parser, the C program, the utilities'
  file), the program built and run by `bin/ci run`; with DerivedConstants.h into `.tmp/repr-haskell/target/hsc`, kept by key, about 40
  seconds a module.
- `bin/repr-haskell`: the native reads take the derived tree (`REPR_HASKELL_INPUTS=pack` the pack); a member the derivation has not made
  yet (before `target hsc` has run) is the pack's and named in the tree's `BORROWED`; `target compare` (the oracle tier's step
  `haskell-target-oracle`, `test/gate/crops/haskell.edn`). `haskell-roundtrip` runs `target hsc` first; the two Haskell read steps
  declare C's inputs (musl's pin, `bin/ci`, `c/target/`).

### Results (light: one process, under 2 GiB, `ARBACE_HOST_EXE` main's stage 0)

- **The oracle tier** (`bin/repr-haskell target compare`): 335 of the pack's 337 members byte for byte (an `.hspp.edn` on its
  `:hsc2hs`), none differing; the two left are GHCi/FFI.hsc's (below, HFS-I11). In order of building: the records, settings and
  headers 147; the configure answers 156; DerivedConstants.h and the 89 .hsc modules 335.
- **The reads** (stubs `ghc ghc-pkg runghc ghci hsc2hs gcc cc clang cpp c99 ld` first on PATH, exiting 99; none was called):
  `REPR_HASKELL_ONLY_PACKAGES='unix ghc-heap hpc haskeline'` and `'directory process time'` `bin/repr-haskell read --reader native
  --corpus`: the 166 modules' native dumps byte for byte those read from the pack (`REPR_HASKELL_INPUTS=pack`); `roundtrip --text
  --layout` over the seven packages: 166 of 166 equal at the tree, layout and text levels, 61 of them .hsc modules, 60 CPP module views.
- **Times**: `target derive` 24 s; `target hsc` 46 min (DerivedConstants.h about 3 min, then about 30 s a module); `target probe`
  about 65 min (the compile tests 60).
- **Scores** (`bin/crops-score --crop haskell`): before 96.3 / 100 / 74.9 (read 0.804), after 96.3 / 100 / 78.8 (read 1.000: 1,344
  modules by their own inputs, 1 by the pack, (1344 + 0.5) / 1345 = 0.9996); `test/crops/scores.edn` recorded.

### Proposed (HFS-I8 to HFS-I10)

- **HFS-I8 Where the hsc2hs outputs live for the gate.** `target hsc` takes about an hour of ci on a fresh tree (90 modules, one
  process, under 2 GiB). (a) **As built: made by `haskell-roundtrip`, kept by key in the checkout's `.tmp/`** (a night in the main
  checkout pays it once per change of the tarball, the derivation or hsc2hs.ae; the clean run pays it every time, light); (b) a pack
  of Arbace's own outputs in the store (`bin/oracle-records`'s format, recorded by `target hsc`, verified by a rerun in the oracle
  tier): the clean run free of the hour, a record to keep; (c) the outputs committed (about 0.9 MB) with `target hsc --verify`.
  *Recommended: (a)*, (b) if the clean run's budget (GT8) objects.
- **HFS-I9 The probe's place.** `target probe --verify` (about an hour of ci's checker) is in the oracle tier's `haskell-target-oracle`.
  (a) **As built: by hand at a tag, and whenever `configure.ae`, `answers.edn`, musl's pin or `c/` changes** (the answers are committed;
  no read recomputes them); (b) a `--full` step keyed on those inputs (it runs only when they change). *Recommended: (b)*: no toolchain
  runs in it, and the key keeps it off most nights.
- **HFS-I10 The installation's identities.** The units' ids (Hadrian's 4-digit hashes), the ABI directory's and the 44 ABI fingerprints
  are recorded facts (`installation.edn`): the reads quote the ids and the directory in their `:cpp` records; no read consumes an ABI
  hash, which is only in the records for the byte comparison. (a) **Keep them as facts** (names of the installation, as the corpus's
  -I paths already are); (b) port Hadrian's `Hadrian.Haskell.Hash` (the ids from the packages' sources and flags); (c) leave the
  fingerprints out of the derived records (the comparison then differs in one line of 44 records). *Recommended: (a)*; (b) once the
  front end compiles the boot libraries (HF9, HF10), which needs the ids it would then compute.
- **HFS-I11 GHCi/FFI.hsc's libffi.** Its C includes `<ffi.h>`, libffi's generated header (the installation's system libffi), which
  neither musl nor the tarball holds; the module's hsc2hs output stays the pack's (BORROWED), 1 of the 1,345 modules. (a) libffi 3.x's
  release tarball pinned in the main checkout's `.tmp/` (as musl's) and `ffi.h` and `ffitarget.h` derived for x86_64 as its configure
  makes them (`ffi.h.in`'s substitutions: a few values), then the module made on ci; about half a day; (b) Arbace's own `ffi.h` in
  `c/include` (the types and constants GHCi/FFI.hsc reads: `ffi_cif`, `ffi_type`, `FFI_DEFAULT_ABI`, the ABI enum) from the libffi
  ABI for x86_64 written anew; (c) keep the record. *Recommended: (a)*, the faithful one; the fetch is the main session's.
- Beside them, for C's crop: ci's checker accepts `enum { v = ((void (*)(int))0) }` and `(timer_t)0.2` (a pointer in an integer
  constant expression, a floating value cast to a pointer), which gcc rejects; recorded here for the C line's agenda (the probe works
  around it with the declarations' types).
## As built: HF9 (first slice) (2026-11-08, branch `hs-hf9`, from next 835891150)

By an agent on the main host by day (the light tier: no gate, no build of Arbace, every process under about 1.6 GB, at most two
at once; stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated). The brief: HF9's first slice (the
type-level tier), chosen by what blocks the most boot-library modules, measured first.

### The measure: `bin/repr-haskell lib-check`

`arbace.repr.haskell.libcheck` with its driver `bin/lib/repr-haskell/lib-check` (`bin/repr-haskell lib-check [--package
ghc-internal|base] [--every N] [--only FILE] [--jobs N] [--record FILE]`, `make repr-haskell-lib-check`) takes each module of a
boot package through the front end as `front.ae` does up to the elaborator: its native dump (`bin/repr-haskell read --reader
native --corpus`, the package alone) renamed and elaborated against the interfaces of its imports, and judges its binders' types
against **GHC's interface of the module itself**: the types GHC recorded when it compiled the installed library
(`typecheck/compare-with-iface`, the canonical forms of `compare-with-ghc`: synonyms expanded, variables renamed by place, an inner
forall's too, the constraints sorted, kinds erased on both sides). A module's result is one EDN map: its stage (`:ok`, `:errors`,
`:typecheck` or `:rename` for an exception, `:timeout` past `LIB_CHECK_TIMEOUT`, 900 s by default, the shard going on in a fresh
process), its errors, the feature each error needs (`libcheck/feature-rules`), the binders compared and equal, its time; `rank`
sums a run. GHC's part is the oracle tier's and only data: the helper's `iface` verb dumps the interfaces of each module's imports
and the module's own; the closure is the helper's grown to a fixed point over the modules the interfaces name (GHC loads an
interface on demand when it expands a synonym or looks up an instance: `Integer`'s closure needs `WordArray`'s for `type
WordArray# = ByteArray#`), with MonadFail's (a failable `do` pattern's `fail` is GHC's to find). GHC compiles nothing.

The corpus: **ghc-internal**, 266 modules (ghc-prim 9.14.1 holds no Haskell but `Dummy.hs`: `GHC.Types`, `GHC.Classes`,
`GHC.CString`, `GHC.Magic`, `GHC.Tuple` are ghc-internal's `GHC.Internal.*` since GHC 9.10), 18 of them hidden (GHC-87110: not
exposed, so the helper cannot load their interfaces: judged nowhere, and their importers' names they define unresolved), 248 judged;
and a sample of **base** (every 9th of its 241 modules, 27, one hidden), whose modules mostly re-export ghc-internal's.

### Before and after

The same closures and dumps, the branch's start (next 835891150's `typecheck.ae`, with the judge's two additions alone) against
its end:

| | before | after |
|---|---:|---:|
| ghc-internal: modules elaborated without an error (of 248) | 122 | **200** |
| of them every binder's type equal to GHC's interface | 113 | **198** |
| binders equal (of those compared) | 4,164 of 4,473 (93.1%) | **4,937 of 4,992 (98.9%)** |
| binders GHC has and the elaborator does not | 17 | 0 |
| modules stopped (an exception, a timeout) | 5 | 0 |
| base's sample: modules without an error, all types equal (of 26) | 23 | **26** (99 of 99 binders) |

### The ranked blockers (ghc-internal, modules whose errors need the feature; sole: the module's only one)

The elaborator's generic "cannot unify" is split by what the two types show (`libcheck/feature-rules`' `unify:` rules, committed
after the runs and applied to their errors' texts; each class confirmed on its modules by hand).

| rank | feature (before) | modules | sole | GHC's rule ported | after |
|---:|---|---:|---:|---|---:|
| 1 | unboxed tuples in types (`(# a, b #)` read as a boxed tuple; `Unit#`, `Solo#`) | 30 | 8 | `mkTupleStr`'s names, `tcHsType`'s HsTupleTy sort | 1 |
| 2 | kinds: a poly-kinded tycon's invisible kind arguments (`Proxy k a`, `Typeable k a`, `MutableArray# l s a`, `(~) k a b`) | 29 | 6 | erasure: `tyConBinders`' invisible count, classes' from their kind-position variables | 2 |
| 3 | `HsDocTy` (a documented type; also hid 16 derived classes in a documented `deriving` clause) | 28 | 6 | the type alone | 0 |
| 4 | variables without a type (binders of unhandled patterns, imported pattern synonyms, hidden modules) | 19 | 2 | below | 0 (8 oracle: hidden) |
| 5 | `Coercible` unsolved | 14 | 2 | erased: GHC's evidence is a representational coercion | 0 |
| 6 | GADTs: constructors (`ConDeclGADT`), imported GADT and existential constructors, given equalities in an alternative | 13 | 0 | `rejigConRes`, `tcDataConPat`, `canEqNC` for givens | 12 |
| 7 | superclass dictionaries of derived instances (documented `deriving`, standalone deriving's variables) | 13 | 2 | the clause's classes; `DerivDecl`'s quantified variables | 1 |
| 8 | constructors unknown (GADT syntax, data family instances, pattern synonyms) | 12 | 0 | data instances as `R:` tycons; pattern synonyms open | 7 |
| 9 | visible type application `f @t` | 10 | 0 | `tcApp`'s instantiation of specified quantifiers | 0 |
| 10 | unboxed sums `(# a \| b #)` (type, expression, pattern) | 8 | 0 | `sumTyCon`, `mkSumDataConOcc` | 0 |
| 11 | ScopedTypeVariables and pattern-bound type variables (`sizeOf (undefined :: a)`) | 4 (+ the unsolved `Storable`, `Show`, `Eq` metas they leave: 5 to 9 each) | 1 | `tcPolyCheck`'s skolems in scope, `tcPatSig` | 0 (Storable 1) |
| 12 | type families: closed, open, associated, data; reduction | 4 | 1 | `FamInstEnv`'s `reduceTyFamApp_maybe` (port) | 0 |
| 13 | `(->)` as a type constructor (`instance Category (->)`) | 4 | 0 | `type (->) = FUN 'Many` | 0 |
| 14 | implicit parameters `?x`, `?x :: t` | 4 | 2 | `IP`'s name as a functional dependency | 0 (2: given IP across a binding) |
| 15 | `MultiWayIf` | 4 | 1 | a right-hand side's guards | 0 |
| 16 | rank-n arguments (`runST :: (forall s. ST s a) -> a`) | 2 | 1 | an inner forall quantified apart | 0 |
| 17 | type-level literals, `KnownNat`/`KnownSymbol`/`KnownChar` | 4 | 1 | `matchKnownNat`... on literals | 0 (3 on variables) |

### Built (this slice, in the measure's order)

- `arbace/repr/haskell/famenv.ae`, a **port** of GHC 9.14.1's `compiler/GHC/Core/FamInstEnv.hs` (`reduceTyFamApp_maybe`,
  `lookupFamInstEnv`'s matching, `chooseBranch`, `findBranch`, `apartnessCheck`, `compatibleBranches`, `computeAxiomIncomps`) over
  `compiler/GHC/Core/Unify.hs`'s `tcMatchTys` and `tcUnifyTysFG alwaysBindFam alwaysBindTv` (SurelyApart, MaybeApart, the occurs
  check's MaybeApart), rule for rule on the elaborator's types without coercions; the elaborator (`typecheck.ae`) reads the
  families of the interfaces (`tycon ... family` with a closed family's equations, `hsiface/fam-instances`) and of the module
  (`FamilyDecl` top-level and associated, `TyFamInstDecl` top-level and in instances, an associated default not taken as an
  instance), reduces at the head where it expands synonyms (`expand`), defers an equality on a stuck application and retries the
  deferred ones to a fixed point before defaulting; GHC.Builtin.Types.Literals' families on literals (`+ * ^ - Div Mod CmpNat
  CmpSymbol AppendSymbol`).
- **Kinds erased** (`conv-type`, `kind-arities`): an interface's types apply a poly-kinded tycon to its invisible kind arguments
  first; the elaborator trusts kinds (§3.4) and drops them, as a type it writes never has them: a tycon's count its kind's leading
  foralls (FUN's required multiplicity binder excepted), a promoted constructor's its type's, a class's its leading variables that
  occur in a kind (in its methods' types and superclasses, to a fixed point: `(~)` through `(~~)` and `~#`), the primitive
  equalities wired; a class's own variables and fundeps shifted alike; the judge erases GHC's side by the same table.
- **GADTs**: `ConDeclGADT` (several names, prefix and record forms; the type the signature's, as `dataConWrapperType`),
  `rejigConRes`'s split of a result into universals and equality specs; an imported constructor's split from its interface type
  (`:~:`'s `Refl`, `Coercion`'s, existentials); a pattern's given equalities scoped to its alternative (`add-given!`: GHC's
  canonicalisation of a given, decomposing tycon and application types, a skolem oriented and bound), unification reading a
  skolem through its given, the alternative's wanteds rewritten by them; signatures checked, not inferred (a FunBind's arguments
  take the signature's types before the patterns: `tcPolyCheck`), so a GADT's scrutinee is known.
- **Data family instances**: a `DataFamInstDecl` a data declaration of its representation tycon (`R:FamTys`), its constructors
  building the family applied to the instance's patterns.
- Unboxed tuples (all arities, `Unit#`, `Solo#`, `MkSolo#`) and **unboxed sums** (`Sum<n>#`, `(#_|#)`: types, expressions,
  patterns); `HsDocTy`, `HsKindSig`, `HsAppKindTy`, `HsTyLit`, `HsWildCardTy`, `(->)` and `FUN` as type constructors; rank-n
  `HsForAllTy`; **ScopedTypeVariables** (a signature's, an instance head's, a class's and a method's variables in scope over the
  body, keyed by the renamer's Names) and pattern-bound type variables; **type applications**; **implicit parameters**; view
  patterns; `MultiWayIf`; Coercible erased; Typeable of applications from its parts' and the givens; the literal classes.
- Bugs the measure found in the existing code: a documented `deriving` clause's classes dropped; standalone deriving's quantified
  variables lost (`(:names st)` of a vector); `simplify-pred`'s `first` of a keyword.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check` (ghc-internal, 248 modules, one process, about 75 minutes) | 200 without an error (122 before), 198 all types equal |
| `bin/repr-haskell lib-check --package base --every 9` (26 modules) | 26 of 26 (23 before) |
| `bin/repr-haskell typecheck-check` (the samples, Tier S's regression; needed `--libdir` on its native reads: since the native reader reads GHC's settings it failed at `/settings` on next, before this branch) | 24 of 24 modules equal, the negative control differs (13 of 24) |
| `bin/repr-haskell run-check` (the 63 programs, `REPR_HASKELL_FRONT_SHARDS=1`, `HSI_LIBS` the unpacked `haskell-stg-libs`) | 59 of 63 run from Arbace's STG with GHC's output, the negative control fails; 62 of 63 as recorded: `synthetic/primops-word` stops in the STG stage on a `StackOverflowError`, the same with the branch's start (`.tmp/m/before-root`, run alone: 45 s, the same error) and in HFS-I1's run (above): not this slice's, a known stop for HFS-I5 (a)'s file |

### Not covered (the remaining slices of HF9, estimates)

| slice | what the measure shows after | days |
|---|---|---:|
| 2: GADT and existential solving in full | 12 modules: givens between skolems of an outer scope, implication constraints whose wanteds are solved after the alternative (GHC's `Implication` with its inert givens, not only rewriting at the alternative's end), escape | 3-5 |
| 3: pattern synonyms | 7 modules define them, more use them; the matcher and builder (`$m`, `$b`), `PatSynSig`'s required and provided contexts; the helper's `iface` writes `(patsyn NAME)` without a type (an imported synonym's type needs the helper: a new verb, its records re-recorded) | 2-4 |
| 4: deriving `Data`, `Generic`, `Lift` and the derived contexts of type-level classes | `unsolved: Show/Eq/Data/Quote/Quasi` in TH.Syntax and IO; `Generic`'s `Rep` instances (an associated family) | 3-5 |
| 5: kinds proper (PolyKinds, DataKinds inference) | erased today; Typeable's `$krep` bindings of poly-kinded tycons need them, STG not | 3-6 |
| 6: QuantifiedConstraints, `WithDict`, `DataToTag`, `HasCallStack` through givens, implicit parameters across bindings | 1-4 modules each | 2-3 |
| 7: desugaring and STG of the new forms (sums in Unarise, view patterns, GADT matches' casts erased, data instances' tycons) | needed for HF10's STG, unmeasured here | 3-5 |
| 8: speed | TH.Syntax and System.Posix.Types take 5 to 10 minutes each (derived instances over a linear instance table) | 1-2 |

Remaining HF9: about 17 to 30 days (the survey's 20-35 for all of HF9, of which this slice is about 4 by its size).

### Proposed (HF9-P1 to HF9-P4; options with costs, a recommendation each, none decided)

- **HF9-P1 The measure as a check.** `lib-check` needs GHC for the closures (the helper's interfaces), about 75 minutes for
  ghc-internal in one process. Options: (a) **the interfaces as an oracle pack `haskell-lib-ifaces-ghc-9.14.1` (the pool and the
  closures, about 40 MB unpacked), the results recorded as `test/haskell/lib-check-ghc-internal.edn`, a `--full` step judging
  that no module's stage or equal count gets worse, its negative control a module judged against another's interface**; about half
  a day; (b) the oracle tier only (`bin/gate --oracle`); (c) wait for HF10's own interfaces (the closures from Arbace's front end,
  no pack). *Recommended: (a)*, the pack recorded with the night's other Haskell records.
- **HF9-P2 What the helper cannot give.** 18 hidden modules of ghc-internal (GHC-87110) and the pattern synonyms' types
  (`(patsyn NAME)` alone) are out of the measure. Options: (a) the helper's `iface` verb reads an interface by its file
  (`readIface` on `$libdir`'s `.hi`, hidden or not) and writes a pattern synonym's `patSynSig`: the helper's hash moves, so every
  Haskell pack is STALE and re-recorded (a night); (b) **a second verb under a version of its own (`iface2`), the packs keyed by the
  first untouched**; (c) wait for HF10. *Recommended: (b)*.
- **HF9-P3 The next slice.** Options: (a) **speed first (slice 8: TH.Syntax and System.Posix.Types at 5-10 minutes each make every
  run slow; 1-2 days), then GADT implications (2), pattern synonyms (3), deriving Data/Generic/Lift (4)**; (b) the measure's order
  alone (2, 3, 4), speed with HF11. *Recommended: (a)*.
- **HF9-P4 Kinds.** Options: (a) **erased, as built, until Typeable's `$krep` bindings of poly-kinded type constructors (HF10's
  STG) need them**; (b) PolyKinds/DataKinds inference now (`Tc/Gen/HsType`'s kind checking, 3-6 days). *Recommended: (a)*.

## As built: HF9 (second slice) (2026-11-08, branch `hs-hf9`, from its first slice bfcfd7952)

By an agent on the main host by day, as the first slice (the light tier; every process under about 1.6 GB, at most two at once;
stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated). The brief: the remaining items in HF9-P3
(a)'s order: speed, GADT implications, pattern synonyms, deriving and the derived contexts; kinds stay erased (HF9-P4 (a)).

### Before and after

The same closures, dumps and judge as the first slice's end (its `.tmp/m/final-after2.edn`, 2026-11-08 morning), against this
slice's end (`bin/repr-haskell lib-check`, the whole of ghc-internal in one process):

| | first slice's end | second slice's end |
|---|---:|---:|
| ghc-internal: modules elaborated without an error (of 248) | 200 | **227** |
| of them every binder's type equal to GHC's interface | 198 | **226** |
| binders equal (of those compared) | 4,937 of 4,992 | **4,955 of 4,992** |
| the whole run, one process (the modules' own times) | about 75 minutes (2,782 s) | **about 12 minutes (592 s)** |
| base's sample (every 9th, 26 judged): without an error, all types equal | 26 | **26 of 26 (99 of 99 binders)** |

### The items, in the brief's order (modules from failing to passing, measured on each item's modules by day)

| item | what was built (GHC's rule as the specification) | modules |
|---|---|---|
| 1 speed | the renamer's interface cache keyed by the file (lib-check's closures are links into one pool: the key of the directories parsed every interface again for every module and kept each copy, so the process grew and its later modules ran 5 to 9 times slower: the corpus's last 53 modules 2,124 s → 221 s); the instance table indexed by class (`insts-of`); derived contexts by a simultaneous fixpoint until none changes (GHC.Tc.Deriv's inferInstanceContexts, each round from the previous one's: was five rounds of one instance at a time over the whole table); lib-check's renamed tree kept in memory, without the renamer's projection on the facts | TH.Syntax 570 s (in the run) / 108 s (alone) → 32 s; System.Posix.Types 222 s → 19 s; Types 200 s → 24 s |
| 2 GADTs and implications | checking mode (GHC.Tc.Gen.Expr's Check ExpType: a case's alternatives, if, let, lambda, multi-way if, a list comprehension's result; GHC.Tc.Gen.App's tcApp: the head inferred, the result unified with the expected type before the arguments are checked; tcSkolemise of an expected polytype: `(KnownNat n => r)`, `forall d. Data d => d -> r'`); sub-patterns checked at the pushed type (tcConPat's matchExpectedConTy before the arguments; tuples, lists, sums); equality givens of signatures', instances', methods' and constructors' contexts (`instance (a ~ Char) => IsString [a]`); a pattern's givens scoped over the statements after it (tcDoStmt, tcLcStmt); wanteds made under givens keep them (an implication's ic_given), solved later rewritten by them; a given whose sides are not known yet pending, settled when its binding ends (`fmap (\Refl -> x) (eqT :: Maybe (t :~: t'))`), an equality failing under pending givens deferred with them and retried; type applications on a signed binder of the module; a signature's nested foralls one quantifier; class method signatures' explicit variables over their default methods (renamer); polytypes unified up to alpha | Data.Data, Data.String, Data.Type.Equality, Err, TypeLits, TypeNats, Unsafe.Coerce (7 of the 12); left: Data.Dynamic (an imported pattern synonym, `Fun`), Data.Typeable and Exception.Context (names of hidden modules), Data.Typeable.Internal (kinds), IO.Encoding.Iconv (the helper's missing synonym, below) |
| 3 pattern synonyms | GHC.Tc.TyCl.PatSyn: a signature's pattern type split as tcPatSynSig does (univ and req, then ex and prov; implicitly quantified, a variable the result does not mention existential); the declaration checked against it (tcCheckPatSynDecl: the universals skolems, the existentials metas the pattern's own existentials fill, each argument at its type, prov wanted from what the pattern binds), an explicit builder checked at the builder type, inferred without a signature; a use in a pattern (tcPatSynPat: the result first, req wanted, ex skolems, prov given) and as an expression (the builder's type); record pattern synonyms by field; the core pattern `(psyn NAME [req...] [prov...] [pat...])`, the matcher's desugaring left to slice 7 | Tuple, Natural, TypeLits, TypeNats, Exception, IO.Encoding.Types (6 of 7; Data.Typeable.Internal: kinds) |
| 4 deriving and the derived contexts | standalone deriving of an imported type (`deriving instance Bounded Bool`: its constructors from the interface); derived Eq, Ord and Show on unlifted fields (`compiler/GHC/Tc/Deriv/Generate.hs`'s primOrdOps, boxConTbl and postfixModTbl, under deriv.ae's notice: `Ptr Addr#`, URec's `Char#`, `Double#`); the newtype strategy for a class of type constructors only when the representation eta-reduces (GHC.Tc.Deriv's eta_ok: `Par1 p = Par1 p` is derived stock); Generic's and Generic1's contexts empty; a data instance's derived heads and GADT constructors at the family's application; existential and GADT record patterns (as positional ones: IO's `Handle__ {haDevice = dev}` binds `IODevice dev`); `C {}` on a constructor without fields; RecordWildCards in expressions (`RTSStats{..}`) | Enum, Show, Ptr, Stats, IO.Handle, IO.Handle.Internals, IO.Handle.Text, IO.Handle.FD, IO.Handle.Types (42 of 42 binders); Generics 53 → 9 errors (all `SingKind k`: kinds) |
| beside them | DataToTag solved for a data type (matchDataToTag: `dataToTagSmall#` below 8 constructors); WithDict (matchWithDict); implicit-parameter bindings `let ?x = e` given to their body; `?callStack` and `?exceptionContext` defaulted at the top (simplifyTop); variable constraints (`cls => r`, `c => DictBox c`) kept as `[:cvar [type]]`; data families generative (never stuck); a family applied beyond its arity decomposed (`Rep a x`, `Rep1 f a`); `e :: sig` checked; promoted lists; a tycon's non-leading invisible kind binder erased (`Code m r a`); naughty record selectors typed `()` (mkOneRecordSelector); default signatures kept apart from methods' types (`lift`'s `r ~ BoxedRep Lifted`) | TH.Syntax (392 of 392), Types, TH.Lift (but its quotes), IO.Exception, IO, Exception.Backtrace, Exception.Type, Conc.Sync, Foreign.Marshal.Pool, Magic.Dict |

### What is left (ghc-internal, the end's run)

21 modules with errors, 1 elaborated with unequal types (Event.Internal, 3 of 7: its types name hidden modules' tycons):

| class | modules | why |
|---|---|---|
| names of hidden modules (GHC-87110: the helper cannot load their interfaces; HF9-P2) | Conc.IO, Data.Typeable, Event.IntTable, Event.Manager, Event.Thread, Event.TimerManager, Event.TimeOut (`Unique`'s representation dictionary), Exception.Context, IO.Handle.Lock, Type.Reflection.Unsafe, Unicode (and Event.Internal's types) | oracle: the helper |
| a synonym the defining module does not export (`CodeBuffer#`, `CodingBuffer#`: the helper's interface omits it; HF9-P5) | IO.Encoding, IO.Encoding.Iconv, IO.Encoding.Latin1, UTF8, UTF16, UTF32 | oracle: the helper |
| an imported pattern synonym's type (`Fun`; HF9-P2) | Data.Dynamic | oracle: the helper |
| kinds (HF9-P4, HF9-P6) | Generics (`SingKind k`, 9 errors, its only ones), Data.Typeable.Internal (levity, kind arithmetic of `TypeRep`) | elaborator: slice 5 |
| Template Haskell quotes (HsUntypedBracket: section 3.9, outside HF9) | TH.Lift (2 errors; its 3 binders equal) | out of scope |

So of the 21, 18 wait for the helper (an oracle-side change, HF9-P2 and HF9-P5), 2 for kinds, 1 for Template Haskell.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check` (ghc-internal, 248 modules, one process) | 227 without an error, 226 all types equal (200 and 198 at the first slice's end) |
| `bin/repr-haskell lib-check --package base --every 9` | 26 of 26 (99 of 99 binders) |
| `bin/repr-haskell typecheck-check` | 24 of 24 modules equal, the negative control differs (13 of 24) |
| `bin/repr-haskell run-check` (`REPR_HASKELL_FRONT_SHARDS=1`, `HSI_LIBS` the unpacked `haskell-stg-libs` of today's records) | 59 of 63 pass, the negative control fails; 62 of 63 as recorded, the one differing the first slice's (`synthetic/primops-word`, `:no-stg`: the StackOverflowError in the STG stage, the same at the branch's start) |

### Proposed (HF9-P5 to HF9-P8; options with costs, a recommendation each, none decided)

- **HF9-P5 What the helper's interfaces leave out (extends HF9-P2).** Besides the hidden modules and the pattern synonyms' types,
  the `iface` verb writes only the module's *exported* things: a synonym it does not export but its exported types use
  (`CodeBuffer#`, `CodingBuffer#` of GHC.Internal.IO.Encoding.Types) has no declaration, so its importers' types do not expand
  (IO.Encoding, Latin1, UTF8, UTF16, UTF32, Iconv: 6 modules, about 230 errors). Options: (a) the second verb of HF9-P2 (b) also
  writes every type constructor of the module's type environment (GHC's interface has their declarations), its records
  re-recorded under its own version; (b) the closure takes the missing synonyms from Arbace's own renamed and elaborated
  defining module (HF10's own interfaces, early). *Recommended: (a)*, with HF9-P2 (b): one new verb, the packs of the first
  untouched; it moves 17 to 18 modules (hidden names 11, these 6, Data.Dynamic's `Fun`) out of the oracle's class.
- **HF9-P6 Kinds (revisits HF9-P4).** Erased kinds now block two modules the elaborator otherwise types: GHC.Internal.Generics
  (`SingKind k`, a class over a kind: `fromSing (sing :: Sing nt)`) and Data.Typeable.Internal (`TypeRep (a :: k)`'s kind
  arithmetic, levity); the data family `Sing`'s instances differ only by kind, so their representation tycons collide. Options:
  (a) keep erasure, these two in a recorded class `:kinds`; (b) kinds proper (Tc/Gen/HsType's kind checking, the
  invisible arguments kept as types; 3 to 6 days, the survey's slice 5). *Recommended: (a) until HF10's Typeable bindings
  need (b)*.
- **HF9-P7 Deriving Data, Generic and Lift's code.** The instances' heads and contexts are entered (what the measure
  judges); their methods (gen_Data_binds, GHC.Tc.Deriv.Generics' `Rep` instance with `from` and `to`, gen_Lift_binds) are not
  generated, so the core lacks them. Options: (a) port them with slice 7 (desugaring and STG of the new forms), where STG needs
  them; (b) now, unmeasured by lib-check. *Recommended: (a)*.
- **HF9-P8 The measure as a check (revisits HF9-P1).** The whole run is now about 12 minutes (592 s) instead of 75 minutes. Options as
  HF9-P1's; *recommended: HF9-P1 (a)*, the results recorded (`--record test/haskell/lib-check-ghc-internal.edn`, from this
  slice's end) and the step's budget the night's.

## Decided (the user, 2026-11-08, about 19:45: HF9-P1 to P8)

HF9-P1 (a) with HF9-P8: lib-check a recorded check (`test/haskell/lib-check-ghc-internal.edn`), the interfaces packed as an oracle
pack, a `--full` step that fails when a module gets worse. HF9-P2 (b) with HF9-P5 (a): a second helper verb that dumps hidden
modules' interfaces, every type constructor including the unexported ones and the imported pattern synonyms' types, so that the
packs keyed by the current helper stay valid; built on the host's GHC 9.14.1, pinned. HF9-P6 (a): kinds erased, the two
kind-blocked modules in a recorded class. HF9-P7 (a): Data, Generic and Lift's derived methods with slice 7. (HF9-P3 (a) and
HF9-P4 (a) were decided before the second slice.) Then lib-check again over the 18 modules the helper limited, from the new verb's
dumps.

## As built: HF9-P1, P2, P5, P6, P7, P8 (2026-11-08, branch `hs-props`, from `mac/hs-hf9` 7ed40299a with `mac/hs-hes84`)

By an agent on the main host, the light tier (every process under about 1.5 GiB, at most two at once; stage 0
`/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated); the night's host load made the runs slow, not heavy.

### The second interface verb (HF9-P2 (b), HF9-P5 (a))

- `tools/hsdump/iface2/Iface2.hs`: an executable of its own, `hsdump-iface2 iface2 -o OUTDIR --libdir DIR [--ghc FLAG... --files]
  MODULE...`, under its own version (`iface2Version`, 1). It is compiled with `tools/hsdump/HsRn.hs`, its export list opened in a
  copy (`bin/repr-haskell`'s `build_iface2`, into `.tmp/hsdump/iface2/`, kept by its own key: `hsr_iface2_hash`, the SHA-256 of
  `tools/hsdump/iface2/*.hs`, `HsRn.hs` and `GHC_VERSION`, with the ghc unit), so its forms are the iface verb's own (`thingF`,
  `typeF`, `nameF`, `availF`, the closure's rule) and nothing of `tools/hsdump/*.hs` changed: `hsr_helper_hash` (the glob
  `tools/hsdump/*.hs`, which does not reach `iface2/`) and every pack keyed by it (haskell-ifaces, -stg-progs, -stg-libs, the
  dumps) are as they were. The helper refuses no GHC of its own (it is linked against the host's ghc library, the pin checked by
  the build's `ghc_check`).
- What it adds to the iface verb's output, each GHC's own answer:
  - **hidden modules**: a module `findModule` refuses (GHC-87110) is found among the units' `unitHiddenModules` (ghc-internal's
    first) and its interface read by `getModuleInfo`, which loads a package module's interface by its `Module` whatever its
    exposure (`getPackageModuleInfo`, `hscGetModuleInterface`);
  - **every type constructor of the module**: the interface's declarations are in the external package state once it is loaded
    (`eps_PTE`), every TyCon among them (GHC.Iface.Tidy keeps all of them); the ones the module does not export are added to
    `(hsiface/things ...)`, as `thingF` writes them, but not classes (their methods would enter the judge's comparison), implicit
    things or family instances' representation tycons. So `CodeBuffer#` and `CodingBuffer#` of GHC.Internal.IO.Encoding.Types,
    synonyms its exported types use, now expand in their importers;
  - **pattern synonyms' signatures**: `(patsyn NAME {:univ [TV...] :req [T...] :ex [TV...] :prov [T...] :args [T...] :res T
    :fields [...] :builder T|nil})`, `patSynSig`'s parts (GHC.Core.PatSyn) and the builder's type, where the iface verb writes
    `(patsyn NAME)` alone. GHC's pretty-printed pattern type is left out: it names variables by their uniques, which differ
    between two sessions.
  - the header `(hsiface/module "M" {:unit "U" :verb "iface2" :iface2 1})`.
- The elaborator reads it (`arbace.repr.haskell.typecheck`'s `add-things!`): an imported pattern synonym with a signature enters
  `*patsyns*` (tcPatSynPat's input: the universals instantiated, the required context wanted, the existentials skolems, the
  provided context given) and, with a builder, `*genv*` (the builder type `forall univ ex. (req, prov) => args -> res`, as a local
  synonym's). A `(patsyn NAME)` without one is skipped as before.

### lib-check as a recorded check (HF9-P1 (a), HF9-P8)

- `bin/repr-haskell lib-check` reads its interfaces from the pack **haskell-lib-ifaces-ghc-9.14.1**
  (`test/haskell/records/haskell-lib-ifaces-ghc-9.14.1.sha256` and `.tsv`, the store of `test/oracle-records/oracle.bash`): its members
  `pool/M.iface.edn` (every interface a closure names) and `closure/M.txt` (each module's closure as the helper dumped it), each keyed
  by `hsr_iface2_hash`; a key that is not the current one is STALE, an absent pin UNRECORDED, a member off its digest REFUSED, each a
  failure, and a module of the run whose closure the pack lacks UNRECORDED. No GHC runs. `--live` makes the closures with the
  iface2 verb (the oracle tier), `--live --record-pack` packs them (the other modules' closures of a pinned pack kept; class B,
  `:host ghc`), `--live --verb iface` measures with the first verb as HF9's slices did. `--verify-pack [--sample N]` (the kind
  `lib-ifaces` of `test/oracle-records/kinds.edn`): N modules' closures dumped afresh and compared with the pack file by file in a
  normal form (`bin/lib/repr-haskell/iface-norm.awk`: GHC's uniques, of type variables, Core binders and pretty-printed local
  names, differ between sessions and order the sorted things, so each is renumbered by its first appearance on its line and the
  lines are sorted; the first runs, which compared bytes, found GHC.Internal.Types', Tuple's and Bits' interfaces "different"), a difference reported
  rather than failed when GHC moved (ORS6 (a)), then the crossed pair (one module's fresh interface against another's packed one,
  which must differ). The work directory is `.tmp/repr-haskell/lib-check/VERB/PKG/`.
- `--record FILE` writes the results with a header (`arbace.repr.haskell.libcheck`'s `record`; a partial run, `--only` or
  `--every`, replaces its modules' rows); `--judge FILE` (`judge`) fails when a module is worse than its row (its stage ranks lower:
  `:ok` > `:errors` > `:typecheck` > `:rename` > `:timeout`; or at the same stage fewer binders equal, more GHC's only, or more
  errors), when a whole run lacks a recorded module or has one the record lacks, or when a module that does not pass is in no class
  of `test/haskell/divergences-lib-check.edn`; a better module is reported (re-record it), never a failure. The negative control
  (GTS81 (a)), `test/haskell/lib-check-controls.tsv`: GHC.Internal.Data.Either judged against GHC.Internal.Data.Maybe's interface
  with Maybe's module name put in Either's place (`control`): the judge must not find it equal (it finds 9 binders GHC's only).
- The gate step `haskell-lib-check` (`test/gate/steps.edn`, `:phase :full`, `:mem-gib 2`, after haskell-read-essential, on stage 0
  alone; its pins the corpus, the native inputs and the new pack; its control the controls file): `bin/repr-haskell lib-check
  --judge test/haskell/lib-check-ghc-internal.edn`, about 12 minutes in one process. The Makefile's `repr-haskell-lib-check` runs
  it, `repr-haskell-lib-check-record` records the pack and the measure by hand.
- The record `test/haskell/lib-check-ghc-internal.edn` (266 rows, ghc-internal's every module now: the 248 of HF9's second slice and
  the 18 hidden ones) is provisional until the night: 227 rows are the second slice's end (the first verb, live), the 39 modules
  run today the iface2 verb's. The pack is not recorded yet (it needs every closure of the package: the night's, by hand), so the
  step fails UNRECORDED until then.

### Kinds and derived methods (HF9-P6 (a), HF9-P7 (a))

- `test/haskell/divergences-lib-check.edn`, goi's format: `:kinds` (deferred; HF9-P6 (a)): GHC.Internal.Generics (SingKind k),
  Data.Typeable.Internal (TypeRep's kind arithmetic and levity) and, found today, Data.Dynamic (dynApply's `typeRep @Type` against
  `typeRepKind tr`: a kind instantiated through an invisible binder, under Fun's provided `k ~ Type`; its other errors left with
  the patterns' types); `:default-signatures` (deferred): GHC.Internal.Exception.Context, found once its hidden imports' names
  resolved: the default method of `class ExceptionAnnotation` is checked at the method's type, not at its default signature's
  (`Show a =>`), so its Show is not given (GHC types `$dm` at the default signature, its context's dictionaries parameters an
  instance passes: GHC.Tc.TyCl.Class, GHC.Tc.TyCl.Instance's mkDefMethBind); the instance's side is the desugaring's, so with slice
  7; `:th-quotes` (deferred): TH.Lift's brackets, section 3.9.
- HF9-P7 (a): nothing built; gen_Data_binds, GHC.Tc.Deriv.Generics' `Rep` instances with `from` and `to`, and gen_Lift_binds go
  with slice 7 (desugaring and STG of the new forms), where STG needs them; the measure judges the instances' heads and contexts,
  which are entered.

### lib-check before and after on the 18 modules the helper limited

The second slice's end (its record) against today's run (the iface2 verb, live, 39 modules: the 18, Event.Internal, Either and
Maybe, and the 18 hidden modules):

| module | before (the iface verb) | after (the iface2 verb) |
|---|---|---|
| Conc.IO | 9 errors (unrenamed:qual 9) | ok, 10 of 10 equal |
| Data.Typeable | 44 errors (unrenamed, no-con-type, Show) | ok, all equal |
| Event.IntTable | 20 errors (unrenamed) | ok, all equal |
| Event.Manager | 101 errors (unrenamed, Monoid, Eq) | ok, all equal |
| Event.Thread | 32 errors (unrenamed) | ok, all equal |
| Event.TimerManager | 37 errors | ok, all equal |
| Event.TimeOut | 9 errors (deriving:rep-dict) | ok, all equal |
| Exception.Context | 5 errors (unrenamed, GADT, Show) | 1 error (unsolved Show: `:default-signatures`), 9 of 9 equal |
| IO.Handle.Lock | 3 errors (unrenamed) | ok, all equal |
| Type.Reflection.Unsafe | 1 error (unrenamed) | ok, all equal |
| Unicode | 6 errors (unrenamed) | ok, 52 of 52 equal |
| IO.Encoding | 2 errors (unify: CodeBuffer#) | ok, 21 of 21 equal |
| IO.Encoding.Iconv | 1 error | ok, all equal |
| IO.Encoding.Latin1 | 38 errors | ok, all equal |
| IO.Encoding.UTF8 | 47 errors | ok, all equal |
| IO.Encoding.UTF16 | 50 errors | ok, all equal |
| IO.Encoding.UTF32 | 44 errors | ok, all equal |
| Data.Dynamic | 4 errors (no-con-type, GADT, Typeable 2: the pattern synonym Fun's type missing) | 2 errors (Typeable 2: `:kinds`), 7 of 7 equal |
| (Event.Internal) | ok, 3 of 7 equal | ok, 7 of 7 equal |

So 16 of the 18 pass now (and Event.Internal's types are equal); the two left are the elaborator's, not the helper's. The 18
hidden modules (Event.Arr, Array, EPoll, IntVar, KQueue, PSQ, Poll; IO.Handle.Lock.Flock, LinuxOFD, NoOp, Windows;
StaticPtr.Internal; Unicode.Bits, Char.DerivedCoreProperties and the five UnicodeData tables), out of the measure before, all pass
(76 of 76 binders). ghc-internal as recorded: **266 modules, 261 without an error and every type equal** (was 248 judged, 227 and
226), binders 5,067 of 5,068 equal; the five left in their classes (`:kinds` 3, `:default-signatures` 1, `:th-quotes` 1).
## As built: HF10 (first slice) (2026-11-08, branch `hs-hf10`, from `mac/hs-hf9` 7ed40299a)

By an agent on the main host by day (the light tier: no gate, no build of Arbace, every process under about 1.6 GB, at most two
at once; stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated; GHC only as the oracle, below). The
brief: the native front end's desugaring to Core and STG for the boot libraries, so that hsi can run Arbace's own STG of
ghc-internal instead of the pack's; a design first, then a first slice on the leaves of ghc-internal's import graph, compared
with GHC's STG binding by binding, and programs run on hsi with those modules' own STG.

### Design (HF10-D1 to HF10-D6)

- **HF10-D1 Through Core, the programs' pipeline.** The libraries go through the pipeline the programs already take (HF5-HF7,
  the Simplifier): the elaborator's Core desugared by the match compiler (`desugar.ae`), GHC's simple optimiser (`simpleopt.ae`),
  the `-O0` Simplifier (`simplify.ae`), then CorePrep, CoreToStg and Unarise (`tostg.ae`). Not a direct translation to STG: GHC's
  reference passes through Core, and the Core passes are where most of the differences are decided (the simplifier's inlining,
  case of case, eta expansion); one pipeline for programs and libraries keeps every fix good for both, and the programs' checks
  (`stg-check`, `run-check`) watch the libraries' changes. What a library adds is its unit (`tostg/*unit*`, `typecheck/*unit*`:
  its names are `ghc-internal:M/x`, its `SrcLoc`s and `$trModule` name the package) and its source path as GHC was given it
  (patError's and `HasCallStack`'s text: the reference's `:file`, `libraries/ghc-internal/src/...`).
- **HF10-D2 The reference is GHC's `-O0` STG of the libraries, a new oracle datum.** The pack `haskell-stg-libs` holds the installed
  libraries' STG at `-O2` (HES6 (a)): workers and wrappers, specialisations, cross-module inlining; no binding of a `-O0` front end
  can equal it. So the reference is GHC compiling the same packages from the pinned tarball exactly as `bin/hsi dumps` does (HES6's
  recipe: one-shot in dependency order, the installed unit ids, hadrian's paths), at `-O0` (`HSI_DUMP_OPT=-O0 bin/hsi dumps
  --packages ghc-internal,base,ghc-prim,ghc-bignum`): 267 + 241 + 2 modules, 28 MB of forms, about 80 seconds at two jobs, read
  back by `arbace.repr.haskell.stg check` with 0 errors. Made by hand, the oracle tier's; a pack of its own is HF10-P1.
- **HF10-D3 The judge: each exported binding with its internal closure.** At `-O0` GHC keeps an instance's methods, a floated
  literal, a local helper as internal top-level bindings (`$c==_7`, `lvl_0`); comparing the exported bindings alone (HF6's
  `stgcmp`, built for programs) would call two dictionaries equal whose methods differ. `stgcmp` now compares each exported
  top-level binding together with the internal top-level bindings it reaches, in the order of their first occurrence, all renamed
  together by place (`closure-of`); the Typeable bindings apart; a primop's or a foreign call's name is not renamed. Each differing
  binding records its first difference (`first-diff`, the subterm pair), and `rank-stg` counts them by the heads of the two
  subterms: the classes below are the work list.
- **HF10-D4 Which modules first: the bottom of the import graph.** The modules ordered by their level in ghc-internal's import
  graph (0: importing nothing of the package but `GHC.Internal.Prim`, then one more than the highest import), the first slice the
  levels 0 to 12: 55 modules, all of which HF9's `lib-check` elaborates without an error (`test/haskell/lib-stg.txt`: Types,
  CString, Classes, Tuple, Magic, Maybe, Base, Err, List, Num, Real's prerequisites, the bignum backend, BigNat, Natural,
  Integer...). The rest follow level by level, the same check over `--every 1`.
- **HF10-D5 Running on the installed libraries' ABI.** The programs' front end compiles against the installed interfaces (the
  helper's, `-O2`: their unfoldings name `-O2`'s workers, `sameTypeRep` behind `eqTypeRep`), and every other module of the pack
  calls the replaced module's `-O2` names (`$wfoo`, `$fOrdMaybe_$c<`, floated `lvl`s). Two sets were tried:
  - **a coherent `-O0` set** (GHC's `-O0` STG of the four packages, the others the pack's): 35 of 63 of run-check's programs pass
    on GHC's own `-O0` STG, the others stop at a name only `-O2` defines (`sameTypeRep`), because the programs' STG was compiled
    against `-O2` interfaces. Rejected for this slice: it needs `-O0` interfaces (HF10-D6).
  - **the pack's set, each replaced module merged over its ABI** (`libcheck merge`, `merge-abi`): the module's own STG, and beside
    it the pack's bindings of the names the own STG does not define, with the internal bindings they reach, renamed apart
    (`ghc$lvl_0`), each form as the pack wrote it. The own bindings take every name both define (every source-level binding:
    an importer calling `$fEqMaybe` gets Arbace's); the kept ones are GHC's `-O2` extras, which call the own ones. The measure of
    independence is the count of own bindings against kept (`replaced.txt`); the baseline is the pack's set, run-check's own.
- **HF10-D6 The independence path.** Eval's and AOT's cases are 0.5 while any STG a run executes is GHC's. In order: (1) the
  modules level by level until ghc-internal's 227 that elaborate compile; (2) the front end's own interfaces, written as it compiles
  each module bottom-up (`typecheck/iface-text`, which `tostg compile --local` already writes for programs), so the library set
  and the interfaces are both `-O0`'s and coherent: the programs compiled against them, the libraries' STG all Arbace's, no kept
  bindings, no pack; (3) base and the other packages the run corpora import (containers, bytestring, text, stm, unix, directory,
  filepath, deepseq, array, time); (4) the switch: `bin/hsi check`'s libraries from the front end (HFS-I1's `--front native` for
  the programs), the cases 1. The interfaces pack and `haskell-stg-libs` then leave every key.

### Built

| piece | files | what |
|---|---|---|
| the check | `bin/repr-haskell lib-stg` (`bin/lib/repr-haskell/lib-stg`), `make repr-haskell-lib-stg` | lib-check's plan and closures (the helper's interfaces), each module of `test/haskell/lib-stg.txt` (`--only FILE`, `--every N`) through the front end into `.tmp/repr-haskell/lib-stg/out/stg/ghc-internal/M.stg.ae`, one EDN map a module (stage, errors, the judge's counts, the differing bindings and their first differences), `summary.txt` by `rank-stg`; `--record FILE`; `LIB_STG_REUSE=1` keeps the last STG for `--run` alone; GHC's `-O0` reference made when missing |
| the module driver | `arbace/repr/haskell/libcheck.ae` (`stg`, `rank-stg`, `replaceable`, `merge`, `compare-runs`) | `stg-one`: lib-check's rename and elaboration, then `tostg/module-stg` in the unit, the reference's `:file` as the source path; `LIB_STG_CORE=1` writes the elaborated and simplified Core beside |
| the judge | `arbace/repr/haskell/stgcmp.ae` | HF10-D3: closures, `first-diff`, `:ours-only`, a binding's two closures shown (`stgcmp GHC OURS NAME`) |
| the reference | `bin/hsi` (`HSI_DUMP_OPT`) | HF10-D2 |
| the runs | `lib-stg --run [ID...]` | HF10-D5: run-check twice (`REPR_HASKELL_FRONT_SHARDS=1`, hsi at 2 GiB and 120 s), on the pack and on the merged set, `compare-runs` failing on a pass lost |

The front end's fixes, each GHC's rule, in the measure's order (bindings equal on the 55 modules: 435 of 1,004 at the start of
the day's runs, the judge's closures already in):

| rule (GHC 9.14.1) | files | equal after |
|---|---|---:|
| the judge's renaming leaves primop names alone | `stgcmp.ae` | 501 of 1,085 |
| `typePrimRep` through type synonyms (`BigNat#` = `WordArray#` = `ByteArray#`: `:U`) | `tostg.ae` (`expand-syn`) | |
| CorePrep's speculative evaluation (Note [Speculative evaluation]; `mkNonRecFloat`, `decideFloatInfo`, `exprOkForSpecEval`'s `app_ok` with `-fspec-eval` and `-fspec-eval-dictfun`): a superclass selection from a dictionary (a `ClassOpId` with a terminating result) or a saturated application of a dictionary function of a class that is not unary is case-bound, not a thunk, with the class's `alg` alternative type; never a call of the binding's own group (`cpe_rec_ids`) | `tostg.ae` (`spec-eval`) | |
| GHC.Core.Make's `bindNonRec` (`needsCaseBinding`): a match's unlifted scrutinee that is not ok for speculation (`noDuplicate# s`) is case-bound, not a let the simplifier may drop; a void scrutinee's alternative is Unarise's `(##)` with `(multi 0)` | `desugar.ae`, `tostg.ae` | 525 |
| bang patterns (GHC.HsToCore.Match's `tidy1`/`tidy_bang_pat`: dropped around a strict pattern, kept around a variable, a wildcard, a lazy pattern; `matchBangs`: a case on the variable with one DEFAULT) and strict variable bindings (`let !x = e`, FunRhs's `SrcStrict`, dsStrictBind: the body forces `x`); the elaborator had dropped every bang | `typecheck.ae` (`BangPat`, `strict-var-bind?`), `desugar.ae` (`pbang`, `match-bangs`) | |
| CorePrep's case floats taken out of a case's scrutinee and of an unlifted let's right-hand side (a strict context: StrictContextFloatable), as its lets were | `tostg.ae` (`float-scrut`) | 545 |
| CorePrep's `cpeArgArity` at `-O0` (`exprArity`, `cpeEtaExpand`): a partial application of one of the module's functions in argument position is eta-expanded to the function's arity | `tostg.ae` (`pap-eta`) | |
| CoreToStg's `mkStgAltType` for a case without constructor alternatives: `(alg T)` from the scrutinee's type (a data type, a dictionary); the module's own binders' types for representations (`global-type` falls back on the elaborator's `:types`: `bigNatNe#`'s `Int#`) | `tostg.ae` (`scrut-alg`, `expr-type`) | 575 of 1,085 |
| `getDFunTyKey`: a dictionary function's name takes a type variable's own name (`$fCTuple2c1c2`) | `typecheck.ae` (`head-occs`) | 575 of 1,149 (64 more compared) |
| a strict field of an unlifted type is not evaluated (`IS !Int#`: `$WIS` is `IS`); `case e of x { DEFAULT -> x }` is `e`; `tagToEnum#`'s result type constructor (StgOpApp's tycon: the binding's result for `isTrue#`, else `Bool`); an unboxed tuple's scrutinee stays let-bound for Unarise (`bindNonRec`'s case on `quotRemInt8# a b` lost its components: the programs' `primops-*` regressed until this) | `tostg.ae`, `desugar.ae` | **595 of 1,149** |

Two of the 55 stop in the STG stage (`Bignum.Primitives`, `Bignum.Backend.Native`: a `NullPointerException` in tostg, their
foreign imports' literals, below); `Bignum.Backend.GMP` has no reference (the pack and the reference are the native backend's,
HES7 (a)). Of the 36 modules with exported bindings, 9 have every binding equal (Tuple 66 of 66, Data.Function, Data.Functor,
Data.Tuple, Control.Monad.IO.Class, Data.Bool, TypeError, Integer.Logarithms, Unicode.Version); the large ones Base 117 of 171,
Classes 58 of 173, Types 66 of 80, Integer 41 of 42, Natural 26 of 28, Num 15 of 16, Stack.Types 16 of 17, List 30 of 55, BigNat
34 of 140, Bignum.Integer 24 of 105, Bignum.Natural 26 of 85.

### What is left: the largest classes of first differences (554 bindings of the 55 modules differ)

| class (GHC's / ours) | bindings | the rule | |
|---|---:|---|---|
| a dictionary's or a derived instance's text (the judge's `"" / ""`: a method taken from another binding, the derived code's shape) | 172 | deriving's Eq, Ord, Enum, Ix on tags: GHC generates `dataToTag#` (Small below 8 constructors), `/=#`, `<#` on `Int#` and `case ... of 1# ->` (`Generate.hs`'s `untag_Expr`, `genPrimOpApp`, `eq_expr_with_tag_check`), `deriv.ae` compares boxed tags with `eqInt`/`neInt`; Classes' constraint tuples' instances; the methods GHC keeps as their own bindings (`$cpure = \\x -> Just x`) | HF10-P4 |
| `:r / :u`: a dictionary's method fields are thunks, GHC's functions | 55 | the `-O0` Simplifier's eta expansion of a method through a newtype's cast (IO's `State#`: GHC's `$cstimes` takes the token as a fifth argument), then cpeArgArity at the dictionary | |
| `(alg / (poly)`, `:U`, `:word`, `:int`, `:v` `/ :L` | 130 | binders' representations and alternative types where the elaborator's type of a binder is not in `:ltypes` (the simplifier's clones and eta binders: `bigNatShiftR`'s `BigNat#` arguments, `bindIO`'s `State#` token) | |
| `(primop / (case`, `(app / (case`, `(case / (app`, `(case / (let` | 95 | the `-O0` Simplifier's inlining of `isTrue#` and its case of `tagToEnum#` (ConstantFold's caseRules), a case identity on a call, an unlifted call let-bound where GHC's is case-bound | |
| `(letne / (case`, `(letne / (let` | 25 | join points: GHC's Simplifier makes a join point of a case's shared continuation (mkDupableCont) where ours duplicates or nests | |
| foreign imports | 49 of ghc-internal's 266 modules | `foreign import ccall`, `prim`, `capi` (dsFCall: the arguments unboxed, the call `(ffi ...)` or `(primcall ...)`, the result boxed in IO): not desugared yet, tostg drops the binding (`literal not handled: foreign import ...`) | HF10-P5 |

With every own binding in place (`LIB_STG_REPLACE=all`), not only the equal ones, the run shows what the differences cost:
`samples/Exc` and `synthetic/class-show` stop at an unboxed tuple of the wrong size (Base's `bindIO` matches the `(# s, a #)` of an
IO action with both components where Unarise drops the `State#`: the representations' class above), `samples/Classes` raises a
`divide by zero` GHC's run does not. The default replaces only the bindings the judge found equal, which are GHC's own `-O0`
code by construction.

### Programs on the own STG

`bin/repr-haskell lib-stg --run` (run-check's 63 programs, samples and synthetic, the front end in one shard, hsi at 2 GiB and
120 s): on the pack, **59 of 63 pass** (62 of 63 as `test/haskell/run-check.edn` records, the differing one the known
`synthetic/primops-word`, `:no-stg`); with the 52 compiled modules of ghc-internal replaced (the two STG stops and GMP aside), their
**595 exported bindings equal to GHC's `-O0` Arbace's own STG** and 6,108 of the pack's kept for its ABI: **59 of 63 pass, none lost,
none gained**; run-check's negative control fails on both sets as it must. Every program runs through the replaced modules (Base's
`$`, `.`, `++`, `map`, `foldr`, the Functor, Applicative and Monad instances of lists, Maybe and tuples; Classes' Eq and Ord
methods, Num's instances, Integer's and Natural's arithmetic, Types and Tuple's workers). The coherent `-O0` set of HF10-D5: 35 of
63 (the rest stop at `-O2`-only names the programs' interfaces put in their STG).

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check --live --only` the 39 modules (the iface2 verb) | 37 pass, 2 in a class; `--judge` against the record: 0 worse, the control differs (9 GHC's only) |
| the same from a test pack (`ARBACE_ORACLE_PACKS` a scratch store, `--live --record-pack`, then the pack read with no GHC, `--judge`) | the same results, 0 worse, the control differs |
| `--verify-pack --sample 4` against that pack | 4 modules, 233 interfaces equal in the normal form, the crossed pair differs |
| the judge's own negatives: one binder fewer equal, a module out of its class | worse, unclassed: exit 1 |
| `gate.crops check` | 0 problems |
| `bin/repr-haskell typecheck-check` (the elaborator after `add-things!`'s pattern synonyms) | 24 of 24 modules equal, the negative control differs (13 of 24) |
| `bin/gate --full --plan` | haskell-lib-check planned (a light `:full` chain beside the serial run) |
| `bin/reap --record`, `bin/crops-score --record`, `--judge --strict` | Haskell eval 797 / 64 / 47, AOT 137 / 1 / 1 (HES87 (a)); 0 problems; Haskell 96.6 / 100 / 74.9 to 96.3 / 100 / 74.9 (completeness 0.9661 to 0.9632) |

### Night (the main session's, by hand at the oracle tier: GHC 9.14.1)

1. `bin/repr-haskell lib-check --live --record-pack --record test/haskell/lib-check-ghc-internal.edn` (the iface2 verb over every
   module of ghc-internal: two helper sessions a module, about 4 s each, then the measure, one process, about 12 minutes; the pack
   haskell-lib-ifaces-ghc-9.14.1 pinned and the record rewritten from one run), `bin/repr-haskell lib-check --verify-pack --sample 8`,
   then add `test/haskell/records/haskell-lib-ifaces-ghc-9.14.1.sha256` to haskell-lib-check's `:pins` in `test/gate/steps.edn`; a
   commit of its own naming :ghc's identity; `bin/oracle-records mirror --push`.
2. `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` (the step's command, no GHC): 0 worse, the control
   differs; a module that moved with the whole run's interfaces (the 227 rows recorded with the first verb) is then the record's.
| `bin/repr-haskell lib-stg` (the 55 modules, one process, about 3 minutes) | 53 compiled, 2 STG stops; **595 of 1,149** exported bindings equal to GHC's `-O0` (435 of 1,004 at the slice's first measure); 9 of 36 modules with bindings all equal; the negative control (Base against its rotated reference) 0 of 171 equal |
| `bin/repr-haskell lib-stg --run` | 59 of 63 on the pack, 59 of 63 with the own bindings, 0 lost |
| `bin/repr-haskell typecheck-check` | 24 of 24 modules equal, the control 13 of 24 (after the bang patterns and the dictionary names) |
| `bin/repr-haskell run-check` (lib-stg's GHC side, the same command at the same caps) | 59 of 63, 62 of 63 as recorded (the known `primops-word`); an intermediate state lost nine `primops-*` programs to `bindNonRec`'s case on an unboxed tuple, found by this check and fixed |
| `HSI_DUMP_OPT=-O0 bin/hsi dumps --packages ghc-internal,base,ghc-prim,ghc-bignum` (the reference, GHC, by hand) | 510 dumps read back, 0 errors; 80 s at two jobs |

The record `test/haskell/lib-stg-results.edn` keeps each module's stage and counts (no step reads it yet: HF10-P2).

### The score and the independence path

The score does not move: `bin/hsi check`, eval's and AOT's tool, still runs every program over the pack's STG, and lib-stg's run is
a check beside it. What moves it is HF10-D6: the 227 modules of ghc-internal lib-check elaborates compiled (about 4,950 binders;
today 595 bindings of 52 modules equal), then the front end's own interfaces so that the library set is coherent at `-O0` without
the pack, then base and the other boot packages the corpora import, then `bin/hsi check --front native --libs native`: Haskell's
eval and AOT cases 1, independence from 74.9 (eval 0.452, AOT 0.493) towards 90. The two judges stay the oracle's: lib-stg's
reference is GHC's `-O0` STG (HF10-P1), the runs' references GHC's recorded runs or the tests' own verdicts.

### Night commands (one process at a time, at most 1.5 GiB a process; `. .tmp/env.sh`)

1. `HSI_DUMP_OPT=-O0 HSI_JOBS=2 bin/hsi dumps --packages ghc-internal,base,ghc-prim,ghc-bignum --out .tmp/hsi/dumps-O0` (GHC, the
   oracle: the reference, about 80 s), until HF10-P1's pack.
2. `bin/repr-haskell lib-stg --every 1 --record .tmp/lib-stg-all.edn` (all 248 judged modules of ghc-internal; about 15 minutes
   by the slice's rate; the measure of the next slice).
3. `bin/repr-haskell lib-stg --run` (the slice; about 35 minutes: two run-checks) and `LIB_STG_REPLACE=all bin/repr-haskell lib-stg
   --run` (every own binding: the bugs list).
4. HFS-I6 (a): `make repr-haskell-run-check` and `bin/repr-haskell stg-check` (the programs' Core and STG against GHC's after this
   slice's rules: bang patterns, speculative evaluation, case floats, dictionary names).

### Proposed (HF10-P1 to HF10-P5; options with costs, a recommendation each, none decided)

- **HF10-P1 The `-O0` reference as a pack.** (a) **`haskell-stg-libs-O0-ghc-9.14.1` through `bin/oracle-records` (the four packages
  now, all 41 when HF10 reaches them; about 4 MB of xz, keyed by the helper's identity as `haskell-stg-libs`), `lib-stg` reading it
  by default**; half a day; (b) by hand at each run (80 s of GHC); (c) no reference once the own interfaces come, the runs alone
  (loses the localisation the binding judge gives). *Recommended: (a)*.
- **HF10-P2 lib-stg as a recorded check.** (a) **`test/haskell/lib-stg-results.edn` as the record, a `--full` step (light, about 40
  minutes with `--run`) on the packs of HF10-P1 and HF9-P1 (the interfaces), failing on a module whose equal count drops, a stage
  that gets worse, a pass lost, or the control**; half a day after P1; (b) the oracle tier only. *Recommended: (a)*.
- **HF10-P3 The front end's own interfaces next (HF10-D6 step 2).** (a) **compile ghc-internal bottom-up writing each module's
  interface (`typecheck/iface-text`) and elaborate the next against the own ones, judged against GHC's `-O0` `.hi` (the helper's
  `iface` on the reference build's interfaces); the run on a coherent own `-O0` set without the merge**; 3 to 5 days; it removes the
  helper's interfaces from the libraries' path; (b) keep GHC's `-O2` interfaces and the merge until every module compiles. *Recommended: (a)*,
  after the modules' foreign imports (P5).
- **HF10-P4 The order of the STG equality work.** The classes above: deriving's tags (172 with the dictionaries' text), the methods'
  eta expansion through casts (55), representations of clone binders (130), the `-O0` Simplifier's `isTrue#`/`tagToEnum#` case rules
  (95), join points (25). Options: (a) **the behaviour first: the representations' class (what `LIB_STG_REPLACE=all` shows failing)
  and foreign imports, then by count**; (b) by count alone. *Recommended: (a)*.
- **HF10-P5 Foreign imports.** 49 of ghc-internal's 266 modules declare them; without them those modules' STG lacks bindings and two
  stop. (a) **port GHC.HsToCore.Foreign.Call's dsCCall/dsFCall (argument unboxing, the result boxed, IO's state threading) and
  `foreign import prim` as `primcall`, the `(ffi ...)` form hsi runs**; 2 to 3 days; (b) take those modules' foreign bindings from
  the pack (a kept-bindings class of its own). *Recommended: (a)*.

## As built: HF10 (second slice) (2026-11-08/09, branch `hs-hf10`, from its first slice bd09e1192)

By an agent on the main host at night but in the light tier (no gate, no build of Arbace, every process under about 1.5 GiB, at
most two at once; stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated; GHC only as the oracle: the
`-O0` reference of the first slice, and two-line probes by hand, `ghc -O0 -ddump-ds -ddump-simpl -ddump-verbose-inlinings` on
scratch modules in `.tmp/gt/`, to see which GHC rule makes a shape). The brief: the largest classes of the first slice's
differing bindings, the behaviour-changing ones first (HF10-P4 (a), the order the first slice recommended), then the module set
past level 12 of ghc-internal's import graph as far as it holds. No HF10-P1 to P5 built (they wait for the user); foreign imports
as they were.

### Rules ported (GHC 9.14.1, written from the source, `.tmp/ghc-src/` of the pinned tarball)

| rule | files | what it fixed |
|---|---|---|
| exprType and dataConInstArgTys: every binder the desugarer and the Simplifier make (the match compiler's variables, case binders, the eta binders of `strip`) given its type from its binding site (a let's right-hand side, a case's scrutinee, a constructor's fields at the scrutinee's type arguments, a lambda's expected type: the binding's own, a cast's source, a known function's parameter), before and after the Simplifier (`c/infer-binder-types!`, `REPR_HASKELL_INFER_TYPES=0` off) | `core.ae`, `simplify.ae` | the representations class: Base's `bindIO` (the `State#` token `:v`, `(# s, a #)` unarised to `MkSolo#`), `bigNatShiftR`'s `BigNat#`, every alternative type of a clone; the Simplifier's own unlifted tests (doCaseToLet: `case sz of i -> ...` on an `Int#` variable) |
| mkEtaWW at the binder's type in CorePrep's eta expansion (`strip`'s `eta`), its binders by position (mkBuiltinUnique n: one name a position, `eta_E1`...) | `simplify.ae`, `core.ae`, `tostg.ae` | `bindIO`'s third argument, the dictionaries' eta-expanded fields compared across closures |
| Unarise's unariseFunArgBinder: every void function argument is voidArgId (one name a module); a void variable returned is `(##)` | `tostg.ae` (`void-binders`) | the `State#` arguments of IO's closures; a `State#` returned by name (`mwaFill#`), which hsi ran as a call of an unbound variable |
| makeCorePair's is_default_method: a class's default methods get compulsory unfoldings (Note [INLINE and default methods]) | `simplify.ae` | `$csconcat = $dmsconcat d` inlined as GHC does: the instances' default methods |
| dfunInlinePragma: dictionary functions CONLIKE (isConLikeId); the classop rule then sees a let-bound `$fSemigroupMaybe d` | `simplify.ae` | the instances' method selections on their own dictionaries |
| simplStableUnfolding's eta expansion to the binder's arity (Note [Eta-expand stable unfoldings]) | `simplify.ae` | `$cmconcat`'s `\d eta -> let z = ...` |
| prepareRhs's makeTrivial at the binding's level (an unlifted argument of a top-level constructor is not let-bound at the top) | `simplify.ae` | an unlifted top-level thunk (`bigNatZero`'s `BigNat#`), invalid STG |
| combineIdenticalAlts (prepareAlts), the alternatives with dead binders and cheapEqExpr right-hand sides merged into a DEFAULT | `simplify.ae` | the derived Ord's `case b of _ -> False` |
| is_more_evald / isBetterUnfoldingThan for join points' inlining (Note [Inlining join points]) | `simplify.ae` | (counted with the join points' class) |
| ConstantFold's litEq: `x ==# lit` as `case x of {DEFAULT -> 0#; lit -> 1#}` | `constfold.ae`, `simplify.ae` | `bigNatIsZero#`, the equalities on literals |
| the unboxed tuples' constructors as constructors (their arity: case of a known `(# a, b #)`) | `simplify.ae` | `bigNatGcd`'s `case (# size a, size b #) of` |
| deriving's untagged code (Generate.hs's untag_Expr `case dataToTag# a of a# ->`, dataToTagSmall#/Large# by the family's size, genPrimOpApp `tagToEnum# (a# ==# b#)`, eq_expr_with_tag_check `case a# /=# b# of 1# -> False`, mkTagCmp's unliftedCompare, the Enum and Ix methods' `I# a#`); the derived binders apart per instance (their keys were all zero spans: two instances' `a` shared one type) | `deriv.ae`, `typecheck.ae` | the derived Eq and Ord of the enumerations (Bool, Ordering), the derived instances' case types |
| primitive literal patterns (`1#`) matched as literals (they had become wildcards) | `desugar.ae` | eq_expr_with_tag_check's `1#`; a latent bug of any `case x# of 0# ->` in the libraries |
| the solver's EvCsPushCall: a call of a HasCallStack binding of the module itself pushes its call site onto `?callStack`, a new wanted solved from the enclosing given (it was the empty stack) | `typecheck.ae` | `cycle`, `head`, `tail`: `pushCallStack site d` |
| IP's evidence kept to CorePrep (`C:IP (pushCallStack ...)`, `ip d`), there the identity and an atom (the newtype class's constructor and selector have no binding) | `desugar.ae`, `tostg.ae` | the HasCallStack evidence's thunks, as GHC's |
| a data constructor wrapper's unfolding (mkDataConRep: strict lifted fields evaluated, a field the libraries' constructor holds unpacked unboxed by `case x of I# x#`) | `simplify.ae` | `$WSrcLoc` inlined; against the pack's `-O2` SrcLoc (unpacked `Int` fields) hsi had stopped on `not an integer: I#` |
| Tidy's tidyTopName at `-O0`: a top-level binder the module does not export is internal | `tostg.ae` (`*internal-tops*`) | `lenAcc`, `badHead` and the like: their callers' closures |
| OccurAnal's rule occurrences: the module's values its RULES mention are kept alive (never inlined unconditionally) | `typecheck.ae` (`:rule-names`), `tostg.ae` | `head`'s `badHead` |
| CorePrep's top-level string floats: a CAF `unpackCString# "..."#` has its literal a top-level string | `tostg.ae` | `prel_list_str` |
| CoreToStg's mkStgAltType: PolyAlt for an unlifted pointer (`ByteArray#`), PrimAlt only for a non-pointer | `tostg.ae` (`prim-alt`) | `(poly) / (prim :U)` |
| a nullary join point's binder at its body's representation; recursive join points (OccurAnal's tagRecBinders) let-no-escape | `tostg.ae` | the loops of `WordArray`, `reverse`, `until` |
| CorePrep's cpeArg floats: a case float's own floats float out with it | `tostg.ae` (`wrap-binds`) | `bigNatSize`'s `case (case ...)` |
| dsEvBinds: a binding's superclass evidence bound once after its dictionary lambdas (`$dApplicative = $p1Alternative d`), the unused removed | `typecheck.ae` (`*ev-binds*`) | `$dmsome`, `$dmstimes`, the methods' repeated superclass selections |
| maybeSaturate for data constructors: a constructor of fields as an argument a function closure (`liftA2 d (\\a b -> a : b)`), a dictionary's constructor fields floated at the top level | `tostg.ae` | `$fApplicativeMaybe`'s `pure = Just` |
| a constructor whose strict fields are all unlifted is a constructor right-hand side | `tostg.ae` (`lifted-strict?`) | `integerZero = IS 0#` |
| NOINLINE and OPAQUE pragmas (their inline activation never active: no call-site inlining, no pre- or post-inlining unconditionally) | `typecheck.ae` (`:noinline`), `simplify.ae` | the bignum functions (`integerToNatural`, `integerFromWord#`, `naturalFromBigNat#`...) inlined where GHC calls them |
| an empty case (EmptyCase: matchEmpty's case without alternatives, CorePrep's Note [Empty case alternatives]) | `desugar.ae`, `tostg.ae` | `absurd`: a pattern-match failure where GHC evaluates the scrutinee |
| (the runs) the merge keeps the pack's binding of any own one that names a constructor the pack lays out otherwise (`-O2`'s unpacked fields: Arr's `Array`, its `Int` an `Int#`) | `libcheck.ae` | `samples/Arrays`, lost in an intermediate run on the 112 modules (Arr's equal bindings constructing `Array` boxed) |

### The measure

`bin/repr-haskell lib-stg` (one process, the reference the first slice's `.tmp/hsi/dumps-O0`):

| set | first slice | second slice |
|---|---:|---:|
| the 55 modules of levels 0 to 12 | 595 of 1,149 | **857 of 1,149** (no binding lost against the first slice; modules all equal 9 → 10) |
| levels 13 to 18 (20 modules: Show, Real, Enum, Arr, Ix, Word, Bits, ST, Char...) | - | 409 of 650 |
| levels 19 to 30 (37 modules: Float, Read, ReadP, Lexeme, Numeric, Int, Ptr, Data.Either...) | - | 1,207 of 1,511 |
| **`test/haskell/lib-stg.txt`, 112 modules** | | **2,473 of 3,310**; 109 ok, 1 with elaborator errors (Unicode), the 2 STG stops of the first slice (foreign imports) |
| levels 31 to 45, measured, not listed | - | 40 modules made (10 with elaborator errors) before `GHC.Internal.IO.Encoding.UTF8`, 594 of 1,063; UTF8's `utf8_decode` takes the Simplifier minutes an iteration (126 s, growing), past the 900 s limit |

The first slice's classes on the same 55 modules (the judge's first difference, by binding):

| class | first slice | second slice | |
|---|---:|---:|---|
| binders' representations and alternative types (`(alg / (poly)`, `:U`, `:word`, `:v`, `:int`, `:addr / :L`, `(poly) / (prim`) | 148 | 24 | the rest: unboxed sums (below), a few clones the inference cannot type (a type variable's representation) |
| a dictionary's method field a thunk where GHC's is a function (`:r / :u`) | 55 | 7 | |
| a closure set of another size (`"" / ""`: a method from another binding, the derived code, the dictionaries) | 181 | 146 | of which about 65 the constraint tuples (below) |
| the Simplifier's case shapes (`(primop / (case`, `(case / (app`, `(app / (case`, `(case / (let`) | 88 | 33 | NOINLINE's mostly: `(app / (case` 20 → 3 |
| join points GHC makes and ours not (`(letne / (case`, `(letne / (let`) | 25 | 8 | |
| join points ours makes and GHC not (`(case / (letne`) | (in other classes) | 25 | new at the head: the case-of-case of ours where GHC's simplifier does not duplicate |

### What is left (the classes of the 112 modules' differing bindings)

- **Constraint tuples** (Classes, about 130 bindings with their data): `class (c1, c2) => CTuple2 c1 c2`, superclasses that are
  type variables; the elaborator keeps only class superclasses, GHC names the selectors `$p0CTuple2` (0-based) and the
  constructor `CTuple2`. Nothing runs them (HF10-P9).
- **Unboxed sums** (`(# (# #) | Word# #)`, the `IsPowerOf2#` functions): Unarise's sum layout (ubxSumRepType, the tag and slots,
  the rubbish literals) not ported (HF10-P8).
- **The bignum backend**: lib-check's dumps are read with the corpus's `-DBIGNUM_GMP`, the reference is native's (HES7 (a)):
  `bignat_compare` and the other backend calls resolve to `Backend.GMP` (HF10-P6).
- **The `-O2` ABI against the `-O0` reference**: the pack's SrcLoc holds its `Int` fields unpacked, so every HasCallStack site
  constructs it unboxed where GHC's `-O0` code boxes them (the first slice's HF10-D5; HF10-P7, HF10-P3).
- **The Simplifier's shapes**: case-of-case into a join point where GHC keeps the case (27), the `isTrue#`/`tagToEnum#` shapes
  (33), the Simplifier's time on `utf8_decode` (an iteration of minutes: the cost of case-of-case and the join points, to profile).
- **Foreign imports** (HF10-P5) and the elaborator's errors of 11 modules past level 13 (HF9's classes).

### Programs on the own STG

`bin/repr-haskell lib-stg --run` over the 112 modules (run-check's 63 programs, samples and synthetic, the front end in one shard,
hsi at 2 GiB and 120 s): on the pack, **59 of 63 pass** (62 of 63 as `test/haskell/run-check.edn` records, the known
`synthetic/primops-word`); with the 109 compiled modules replaced, their **2,451 exported bindings equal to GHC's `-O0` Arbace's
own STG** (595 of 52 modules in the first slice) and 11,509 of the pack's kept for its ABI: **59 of 63 pass, none lost, none
gained**. An intermediate run of the 112 lost `samples/Arrays`: Arr's equal bindings construct `Array` with its `Int` boxed, the
pack's `-O2` layout unpacks it; the merge now keeps the pack's binding of any own one naming such a constructor (HF10-P7).

With every own binding (`LIB_STG_REPLACE=all`): 5,944 own bindings of the 109 modules (10,241 of the
pack's kept): **28 of 63 pass, 31 lost** (the first slice's three failures, Base's `bindIO` among them, are gone; the set is now
twice as large). The bugs list, by hsi's first error: `applying int64(0) to 1 arguments` (13 programs: an own binding called
at another arity than the pack's callers assume, the `-O2` ABI's workers), `not an integer: <thunk ... bigNatCompare ...>` (5: an
unlifted value let-bound lazily in an unequal binding), the bignum backend's `Backend.GMP` (5: HF10-P6), `no binding of raise#`
(3: an unsaturated primop as a value), `unbound name` (3: a local lost by an unequal binding's STG), `unboxed tuple of the wrong
size` (2), a segmentation fault (1). The default (`equal`) replaces only GHC's own `-O0` code by construction, and loses none.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-stg` (112 modules, one process, about 8 minutes) | 2,473 of 3,310 bindings equal; the 55 of the first slice 857 of 1,149, none lost against it; the negative control 0 of 171 |
| `bin/repr-haskell typecheck-check` | 24 of 24 modules equal, the control 13 of 24 (after the deriving, HasCallStack and evidence changes) |
| `bin/repr-haskell lib-stg --run` (run-check twice, the front end in one shard, hsi at 2 GiB and 120 s) | on the pack 59 of 63 (62 of 63 as recorded); with 2,451 own bindings of 109 modules 59 of 63, 0 lost |
| `LIB_STG_REPLACE=all` (every own binding of the 109 compiled modules) | 5,944 own bindings: 28 of 63, 31 lost (the bugs list above) |

The record `test/haskell/lib-stg-results.edn` keeps each module's stage and counts (`--record`; no step reads it yet: HF10-P2).

### Night commands (one process at a time, at most 1.5 GiB a process; `. .tmp/env.sh`)

1. `bin/repr-haskell lib-stg --every 1 --record .tmp/lib-stg-all.edn` (every judged module of ghc-internal: the measure past
   level 30, UTF8's timeout recorded as such; about 40 minutes).
2. `bin/repr-haskell lib-stg --run` and `LIB_STG_REUSE=1 LIB_STG_REPLACE=all bin/repr-haskell lib-stg --run` (the 112 modules:
   about 40 minutes each).
3. HFS-I6 (a): `make repr-haskell-run-check` and `bin/repr-haskell stg-check` (the programs' Core and STG against GHC's after this
   slice's rules: the binders' types, voidArgId, the default methods, the evidence bindings, the wrappers).

### Proposed (HF10-P6 to HF10-P10; options with costs, a recommendation each, none decided)

- **HF10-P6 The bignum backend of lib-check's dumps.** (a) **ghc-internal's native dumps read with `-DBIGNUM_NATIVE` and
  without `Backend.GMP`, as `bin/hsi dumps` builds the reference (`test/haskell/eval/dump_plan.ae`'s rule), lib-check's closures
  then Native's interfaces**; half a day, a re-read of the package (light, about 10 minutes); (b) leave it: about a dozen bindings
  call the other backend's names. *Recommended: (a)*, the judge and the runs then name the backend the pack has.
- **HF10-P7 The ABI the runs compile against.** The programs and the replaced modules are compiled against the pack's `-O2`
  data (unpacked strict fields), the judge's reference is `-O0` (boxed): the two disagree on SrcLoc and any `{-# UNPACK #-}`
  field. (a) **keep the pack's data for both (as built: behaviour first) until HF10-P3's own `-O0` interfaces and libraries make
  the set coherent**; (b) two compilations, the judge's against the reference's `-O0` data and the runs' against the pack's (twice
  the time, and an equal binding is then not the one that runs). *Recommended: (a)*.
- **HF10-P8 Unboxed sums in Unarise.** (a) **port ubxSumRepType's slots and layout, the tag, the rubbish literals and the
  alternatives' unpacking (Stg/Unarise.hs, RepType.hs) with an STG form for rubbish (`(rubbish REP)`, hsi's)**; 1 to 2 days with
  hsi's part; (b) the few `IsPowerOf2#` bindings from the pack. *Recommended: (a)*, programs with UnboxedSums need it too.
- **HF10-P9 Constraint tuples.** (a) **the elaborator's variable superclasses (a class context of constraint variables), GHC's
  names for the CTuple classes (`$p<i>CTuple<n>` from 0, the constructor `CTuple<n>`)**; a day; (b) leave them: no program builds
  a constraint tuple's dictionary at `-O0`. *Recommended: (b)* until a corpus needs them.
- **HF10-P10 The list past level 30.** (a) **first the Simplifier's time on `utf8_decode` (profile the iteration, the
  case-of-case and join point duplication against GHC's dump), then the levels 31 to 60 into `test/haskell/lib-stg.txt` as they
  hold**; 1 to 2 days; (b) the levels as they are, UTF8 recorded `:timeout`. *Recommended: (a)*: the same cost would hit a program.

## Decided (the user, 2026-11-08: HFS-I8 to HFS-I11)

- **HFS-I8 (a)**: the hsc2hs outputs made by `haskell-roundtrip` (`target hsc` first), kept by key in the checkout's `.tmp/`; the clean
  run pays the hour (light). As built with HFS-I2 (a); nothing to change.
- **HFS-I9 (b)**: `target probe --verify` a `--full` step keyed on what it reads, out of the oracle tier (below).
- **HFS-I10 (a)**: the units' ids, the ABI directory and the 44 ABI fingerprints stay facts of `installation.edn` (names of the
  installation, as the corpus's -I paths are); Hadrian's hash ported when the front end compiles the boot libraries (HF9, HF10).
- **HFS-I11 (a)**: libffi 3.x's release tarball pinned, `ffi.h` and `ffitarget.h` derived for x86_64 as its configure makes them,
  GHCi/FFI.hsc made on ci (below). The fetch: the user allowed it for this one tarball.

## As built: HFS-I9 (b), HFS-I11 (a) (2026-11-08, branch `hs-hfsi9`, from `next` 5ec43f7b7)

### HFS-I11 (a): the system's libffi

- **The pin**: `test/haskell/target/libffi.edn`: libffi 3.8.0, the latest 3.x release on 2026-11-08 (GitHub's release v3.8.0, published
  2026-08-08), https://github.com/libffi/libffi/releases/download/v3.8.0/libffi-3.8.0.tar.gz, 1,581,626 bytes, SHA-256
  `7da3e2d9a171eb0a038f592ecad3ff2bb2550f3496d87b3b29ad0cf4430c0db4` (equal to the digest GitHub's release API lists for the asset),
  SHA-512 recorded beside; fetched once into the main checkout's `.tmp/repr-haskell/libffi/` (a new, empty directory), the path a
  worktree and the clean clone take it from (`haskell_take`). 3.8.0 is also the host's Alpine package (libffi-dev 3.8.0-r0), the one the
  pack's hsc2hs run read. MIT: LICENSE.md ("libffi: MIT License"), doc/ARBACE.md.
- **The derivation** (`arbace.repr.haskell.target`'s `libffi-vars` and `libffi-headers`): the tarball is data (only `configure.ac`,
  `include/ffi.h.in`, `src/x86/ffitarget.h` and `LICENSE` are extracted, into `.tmp/repr-haskell/target/libffi`; no configure, no make,
  no code of it runs). config.status's AC_CONFIG_FILES substitutions of `ffi.h.in`, each value by its rule: `VERSION` and
  `FFI_VERSION_STRING` AC_INIT's version (3.8.0), `FFI_VERSION_NUMBER` X*10000 + Y*100 + Z (30800), `TARGET` configure.host's
  `x86_64-*-*` rule (X86_64, sizeof(size_t) 8 from `c/target/abi.edn`; TARGETDIR x86), `HAVE_LONG_DOUBLE` 1 (configure.host sets none for
  x86; sizeof(long double) 16 differs from double's 8), `FFI_EXEC_TRAMPOLINE_TABLE` 0 (only `*arm*-apple-*`, `aarch64-apple-*`); an
  `@VAR@` left unsubstituted is an error. `ffitarget.h` is `src/x86/ffitarget.h`, as configure's config command copies it. The host
  `x86_64-alpine-linux-musl` is the pin's `:host`. Both headers are byte for byte the host's `/usr/include/ffi.h` and `ffitarget.h`
  (checked by hand, 2026-11-08: Alpine's configure of the same release).
- **Where they go**: `sysroot/usr/include/ffi.h` and `ffitarget.h` of the derived tree, where libffi-dev installs them; named in the
  tree's new `SYSTEM` file, which `target compare` leaves out (the host system's headers: the pack, GHC's installation, holds none of
  them). `target hsc` gives every module's C program the derived tree's `sysroot/usr/include` last, as gcc's `/usr/include` was for
  hsc2hs on the host; and it now runs `bin/ci musl ensure` first (a fresh tree had no forms cache: its three ghci modules failed with
  "no index.edn", found here).
- **Results** (light, one process, about 1 GiB): `REPR_HASKELL_ONLY_PACKAGES=ghci bin/repr-haskell target hsc`: 3 of 3 modules made on
  ci, 2 min 11 s; GHCi/FFI.hs byte for byte the pack's (SHA-256 `3abe5950...`, the record's `:hsc2hs :output`). The derived tree with
  the 90 modules made on ci (89 from the hs-inputs run, the ghci three from this one) against the pack: `target compare` **337 of 337
  equal, 0 differ, 0 missing, 0 extra, 0 borrowed**, libffi's 2 the system's. Nothing of the read stage is borrowed any more.
- **Score** (`bin/crops-score --crop haskell`): read 0.9996 to 1.000 (`:cases {:self 1345 :recorded 0}`); Haskell 96.6 / 100 / 78.8
  (independence 0.78840 to 0.78847: unchanged at the record's four places, `test/crops/scores.edn` as it was; `--judge --strict` 0
  problems).

### HFS-I9 (b): the probe as a `--full` step

- **`bin/repr-haskell-target`** (new; `src`, `probe [--verify]`): the probe and the tarball's extraction moved out of `bin/repr-haskell`
  so that the step's key is its own (a script in `bin/`, as the planner's `:cmd` check reads `bin/NAME`); `bin/repr-haskell target probe`
  and its `target_src` run it. Make: `make repr-haskell-target-probe`.
- **`haskell-target-probe`** (`test/gate/steps.edn`, `:phase :full`, 2 GiB, hosts `:stage0 :go`): `bin/repr-haskell-target probe
  --verify`, keyed on the script, `inputs.bash`, `arbace/repr/haskell/configure.ae`, `test/haskell/target/answers.edn`, the pins of the
  GHC tarball (`corpus-ghc-9.14.1.edn`) and musl (`test/c/corpora.edn`), C's tools and target (`bin/ci`, `bin/lib/ci/`, `bin/lib/repr-c/`,
  `arbace/c{.ae,/}`, `arbace/repr/c{.ae,/}`, `c/`, `test/c/host/`, `test/c/records/`, `:c-run`, `:c2j-runtime`; 186 files): no Haskell
  change but those reruns it. Its control is waived (the fresh answers against the committed file: a differing answer is its finding).
  `bin/gate --full --plan`: `run haskell-target-probe key 8549e6f01260, 186 files; never passed`, among the light chains beside the
  serial run. `gate.crops check`: 0 problems.
- **The oracle tier**: `haskell-target-oracle` (`test/gate/crops/haskell.edn`) keeps `target hsc && target compare`; spliced into `test/gate/steps.edn`'s oracle tier by night 10's merge stage (branch `night10-fixes`, `gate.crops splice` of that step alone: its step, its `:crops :haskell` entry and its waiver).
- **The probe's run** (the moved script, whole, by day: one process, 0.9 GiB peak): `bin/repr-haskell target probe --verify` (through
  `bin/repr-haskell-target probe --verify`), 308 compile tests, "the answers as committed", 36 min (65 on hs-inputs' first run).

## Decided (the user, 2026-11-08, later: HFS-I12, HFS-I13)

- **HFS-I12 (a)**: the probe gets a negative control of two planted compile tests. Built ("As built: HFS-I12 (a)").
- **HFS-I13 (a)**: keep the clean run's two hours of ci (light, beside the serial run) until GT8's budget for the clean run objects.
  Nothing to build.

## As built: HFS-I12 (a) (2026-11-08, branch `hs-hfsi9`, from 88af58be2)

- **The control**: `test/haskell/target/controls/control-no.c` (`#include <stdlib.h>` and `void unsetenv(const char *);`, a
  conflicting declaration, the shape of the probe's own `compiles` checks) must be answered "no", and `control-yes.c` (stdlib.h
  and a function using `abs`) "yes". `bin/repr-haskell-target controls` runs the two through `bin/ci diagnose` as the probe runs
  the 308 and reads the result as `arbace.repr.haskell.configure/compile-errors` does (a `FILE.c:LINE:` line on the file means
  no); either answer flipped, exit 1 naming it. `probe` runs it first, so a flip fails in about 20 seconds instead of after the
  36 minutes. `REPR_HASKELL_CONTROLS=DIR` names another directory (the flips below).
- **The step**: `haskell-target-probe`'s waiver in `:controls` is replaced by `"test/haskell/target/controls/control-no.c"`, and
  the directory joins its `:inputs` (so the control is in its key: `bin/gate --full --plan` shows 188 files, never passed).
  `gate.crops check`: 0 problems; `test/gate/crops-test.bash`: ok.
- **Checked, light**: the real controls: "control-no.c answered no, control-yes.c answered yes", exit 0. The no-test replaced by the
  yes-test: "control-no.c was answered yes (it must be no)", exit 1. The yes-test replaced by the no-test: "control-yes.c was
  answered no (it must be yes)", exit 1.
- **The whole probe** (by day, `bin/repr-haskell-target probe --verify`): the controls passed first, then the 308 compile tests, types and values: "the answers as committed", 33 min wall (40 min CPU), one process.
- **Why not planted among the 308**: the answers file reads only the 308 by name, so a planted pair would be harmless there too,
  but a failure would show only after the long diagnose run; a separate early run costs one more 20-second ci start.

- **HFS-I12 The probe's negative control.** The step is waived. (a) **A control check in the plan**: one compile test the probe must
  answer "no" (a header that does not exist, a function musl lacks) and one it must answer "yes", planted beside the 308, the probe
  failing when either flips; a few minutes; (b) keep the waiver. *Recommended: (a)*, cheap, and it tests ci's checker as the probe uses it.
- **HFS-I13 The clean run's hour of `target hsc`.** With HFS-I8 (a) and the probe's hour, a clean run of Haskell's steps pays two hours of
  ci (light, beside the serial run). (a) Keep; (b) HFS-I8 (b) (Arbace's own outputs as a pack, verified by a rerun in the oracle tier).
  *Recommended: (a)* until GT8's budget for the clean run objects.

## Decided (the user, 2026-11-09 ~06:15: HF10-P1 to HF10-P10, all as recommended)

HF10-P1 (a), HF10-P2 (a), HF10-P3 (a) (after P5), HF10-P4 (a), HF10-P5 (a), HF10-P6 (a), HF10-P7 (a), HF10-P8 (a), HF10-P9 (b),
HF10-P10 (a). Built the same day on branch `hs-props2` (below); P4, P7 and P9 are orders and leaves, recorded here: P4 (a) the
behaviour-changing classes first (P5's foreign imports and P8's unboxed sums were the two that stopped modules or programs), then by
count; P7 (a) the runs keep the pack's `-O2` data (unpacked fields) until P3's own interfaces make the set coherent; P9 (b) the
constraint tuples' classes stay as they are until a corpus needs their dictionaries. P3 (a) waits for its own slice (3 to 5 days).

## As built: HF10-P1, P2, P5, P6, P8, P10 (2026-11-09, branch `hs-props2`, from `hs-hf10` 5e30516fb with `mac/next` b15d4c73c merged)

By an agent on the main host by day (the light tier: no gate but `--plan`, no build of Arbace, every process under about 1.5 GiB, at
most two at once; stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated). GHC ran by hand only to make
the oracle packs (P1's, and the two P2 needs, below); every check below runs without it. One commit a proposal.

### HF10-P1 (a): GHC's `-O0` STG as the pack `haskell-stg-libs-O0-ghc-9.14.1`

- `HSI_DUMP_OPT=-O0 bin/hsi dumps --record --packages ghc-internal,base,ghc-prim,ghc-bignum`: the four packages compiled from the
  pinned tarball as `bin/hsi dumps` compiles the installed libraries (HES6 (a)), at `-O0`; 510 members, 31.7 MB of forms, **1.9 MB
  of xz**, read back by `arbace.repr.haskell.stg check` with 0 errors, byte for byte the first slice's `.tmp/hsi/dumps-O0`. Pinned by
  `test/haskell/records/haskell-stg-libs-O0-ghc-9.14.1.sha256` with its manifest; the kind `stg-libs-O0` in
  `test/oracle-records/kinds.edn` (verify: `HSI_DUMP_OPT=-O0 bin/hsi verify-libs {sample}`); mirrored (`bin/oracle-records mirror
  --push`).
- Keys: night10-fixes' rule (cherry-picked as d4d589c49): `dump_package`'s text hashed as the `-O2` it was, another level keyed
  apart with the level in the key. `dump_package` is unchanged (its digest keys `haskell-stg-libs`: `bin/hsi libs-pack -O2` takes
  the `-O2` pack as recorded).
- `bin/hsi libs-pack [-O0|-O2]`: a level's pack unpacked, its keys checked, its directory printed (`hsi_libs_pack`, `hsi_libs_pin_of`
  in `bin/lib/hsi/records.bash`); `verify-libs` at `HSI_DUMP_OPT`'s level. `lib-stg` reads its reference from the `-O0` pack and the
  other packages from `haskell-stg-libs` (`LIB_STG_LIVE=1`: GHC live as before, `LIB_STG_REF`, `HSI_LIBS` other directories).

### HF10-P2 (a): lib-stg as a recorded `--full` step

- `bin/repr-haskell lib-stg --judge FILE` (`arbace.repr.haskell.libcheck judge-stg`): each module of the list against its row of
  `test/haskell/lib-stg-results.edn` (`:stage :compared :equal :data-equal`; `--record FILE` writes it, `record-stg`); a module whose
  stage drops (`:ok`, `:errors`, `:stg`, `:typecheck`, `:rename`, `:timeout`) or whose bindings equal to GHC's `-O0` drop fails, and
  in a run of the whole list a module of one not in the other. Better is reported, never a failure.
- The negative control: `test/haskell/lib-stg-controls.txt` names the module (GHC.Internal.Base) judged against its reference with
  the exported bindings' closures rotated; none equal or the step fails (as the first slice's control, now from a file).
- `--run` with `--judge`: run-check's programs on the pack's libraries and on the own set; a pass lost between them fails
  (`compare-runs`), and so does a pass of `test/haskell/lib-stg-runs.txt` (the own set's passes, written by `--run --record`) lost,
  so that both sets failing alike cannot pass.
- No GHC: the plan's closures from `haskell-lib-ifaces-ghc-9.14.1` (HF9-P1's pack, recorded by hand today, below), the reference from
  P1's pack, run-check's interfaces from `haskell-ifaces-ghc-9.14.1` (`REPR_HASKELL_RUN_IFACES=pack`, `bin/hsi ifaces-pack`; run-check
  itself keeps the helper live by default).
- The step `haskell-lib-stg` (`test/gate/steps.edn`, `--full`, needs `haskell-lib-check`, `:mem-gib 2`, `:hosts [:stage0 :go]`, the
  five packs' and the corpus's pins, `:inputs [:lib-tools :tools :haskell :hsi]`, its control file in `:controls`); `bin/gate --full
  --plan`: run, never passed. Its first pass is night 11's.
- Two packs this needed, recorded by hand (GHC 9.14.1, `:ghc b9908216`): **`haskell-lib-ifaces-ghc-9.14.1`** (HF9-P1's write-ahead
  pack: `bin/repr-haskell lib-check --live --record-pack`, 265 closures and 269 interfaces, 277 KB xz; its pin joins
  `haskell-lib-check`'s `:pins`, as that step's comment planned) and **`haskell-ifaces-ghc-9.14.1` re-keyed**: HF9-P2's change to
  `tools/hsdump` (0cdc76cec) had made every member STALE (so `bin/hsi check --front native` refused it on next too); recorded again,
  its 440 members byte for byte the same, the pin unchanged, only the manifest's keys.

### HF10-P10 (a): the Simplifier's time on `utf8_decode`

Profiled by counting calls (a driver wrapping the Simplifier's functions, `.tmp/p10/prof.ae`): in 180 s of the first iteration of
`GHC.Internal.IO.Encoding.UTF8`, `subst-expr-env` was called 427,758 times, `simpl-expr-f` 1,380. The cause: contOutArgs's
substitution (GHC.Core.Opt.Simplify.Env's getSubst) converted every entry of the environment's substitution, and each ContEx entry by
its own saved substitution, transitively; with the guards' nested continuations (case-of-case into join points, each saving the
environment) that is exponential. GHC's `to_expr` is a lazy thunk of `mapVarEnv`: an entry nobody looks up is never computed, one looked
up twice is computed once. Ported so (`simplify.ae`, `subst-expr-env`: only the keys the expression names, memoised per call). The
module: past the 900 s limit, now **9.8 s**.

### HF10-P6 (a): the bignum backend

`REPR_HASKELL_BIGNUM=native` (`bin/repr-haskell`'s `corpus_bignum`, the rule of `test/haskell/eval/dump_plan.ae`): ghc-internal read
with `-DBIGNUM_NATIVE`, `GHC.Internal.Bignum.Backend.GMP` left out, as GHC's reference and the installed libraries were built;
`lib-check` reads so (`nd.bignum` marks the read, a different backend reads again). `test/haskell/lib-check-ghc-internal.edn`
recorded again: 265 modules, 260 pass, the 5 in their classes, none worse.

### HF10-P5 (a): foreign imports

GHC 9.14.1's `GHC/HsToCore/Foreign/` ported into the elaborator's Core (`typecheck.ae`, `ds-fimport`): Decl.hs's dsFImport, C.hs's
dsCImport and dsFCall (the CApiConv wrapper's name: mkWrapperName's `ghc_wrapper:N:UNIT:MODULE:SYM` z-encoded, N the module's counter),
Prim.hs's dsPrimCall, dsFLabel; Call.hs's unboxArg (primitive, newtype, Bool, a product of one field), boxResult (IO's state threading,
a pure call on `realWorld#`), resultWrapper and mk_alt. The call is a global of kind `ffi` (GHC's FCallId: its spec in the elaborator's
`:fcalls`, its type the worker's); the wrapper has an INLINE unfolding of its arity (mkInlineUnfoldingWithArity), which the simple
optimiser leaves alone; `tostg.ae` writes StgOpApp's `(ffi TARGET CONV SAFETY ATOMS REPS)` and `(primcall LABEL UNIT ATOMS REPS)` and the
label `(label SYM :fun|:data)`. Not taken (left as before, the binding dropped): `"wrapper"` and dynamic imports, an argument of a
three-field product (unboxArg's ByteArray# case). With it, the rules it met: Unarise keeps `realWorld#` as an argument (only void
binders are `void#`; it had made every `realWorld#` `void#`), the representations of `Solo#` and `Unit#` (a call returning nothing:
`:v`), shortOutIndirections of a top-level binder of the module (`free = _free`: GHC's isLocalId), and ConstantFold's binary rules on
two literals only (`and# x` unsaturated had thrown the NullPointerException that stopped `Bignum.Primitives` and `Backend.Native`
since the first slice).

### HF10-P8 (a): unboxed sums

`tostg.ae`: RepType's SlotTy, primRepSlot, slotPrimRep, fitsIn, ubxSumRepType and layoutUbxSum; Unarise's mkUbxSum (the tag an `Int#`
literal, the field in its slot, the others ubxSumRubbishArg's: `absentSumFieldError`, `0`), unariseSumAlts and mapSumIdBinders (a case on
a sum a case on its tuple, then on the tag, DEFAULT first). A construction takes the sum's type from the binding's result (its join
points'), a case from its scrutinee's type; a field that needs castArg's cast is left as before. `wordIsPowerOf2#` and
`bigNatIsPowerOf2#` are GHC's `-O0` STG now.

### The measure

`bin/repr-haskell lib-stg` (one process; the reference P1's pack, the closures HF9-P1's):

| set | HF10's second slice | 2026-11-09 |
|---|---:|---:|
| `test/haskell/lib-stg.txt`'s 111 modules (levels 0 to 30, Backend.GMP out by P6) | 2,473 of 3,310; 2 STG stops, 1 with elaborator errors | **2,560 of 3,421**, all 111 `:ok` (14 modules better, none worse: Backend.Native and Primitives made STG, Unicode now without errors, BigNat 78 → 81, Float 150 → 160, Clock, Profiling, Magic, Prim.Ext, Stable, Foreign.Ptr ...) |
| the other 154 modules of the plan (level 31 to the top) | 40 made before UTF8's timeout | 149 `:ok`, 5 with elaborator errors (Data.Dynamic, Data.Typeable.Internal, Exception.Context, Generics, TH.Lift: lib-check's classes), none over the limit (the slowest 84 s); 2,812 of 4,172 |
| **the whole of ghc-internal (265 modules), the list since P10 (a)** | | **5,372 of 7,593**; 260 `:ok`, 5 with elaborator errors; 55 modules with every binding equal (of 239 with bindings) |

The largest classes of first differences over the 265 (`summary.txt`): a closure set of another size (`"" / ""`, 1,193: the
dictionaries' and methods' closures, the constraint tuples HF10-P9 (b) leaves), a constructor GHC makes static where ours is a
closure (`(rcon / (closure`, 191), a case GHC keeps where ours let-binds (`(case / (let`, 149), updatable and re-entrant swapped (`:r /
:u` 112, `:u / :r` 104), join points (`(case / (letne`, 46).

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn` (111 modules, against the second slice's record) | 2,560 of 3,421; 14 better, none worse; the control 0 of 171 |
| `bin/repr-haskell lib-stg --record ... --run` (111 modules) | the runs 59 of 63 on the pack and 59 of 63 with 2,538 own bindings of 111 modules (2,451 of 109 before), 0 lost; `lib-stg-runs.txt` 60 lines |
| `bin/repr-haskell lib-stg --record ... --run` (265 modules, 47 minutes) | 5,372 of 7,593; the runs 59 of 63 on the pack and 59 of 63 with 5,179 own exported bindings of 265 modules (31,190 of the pack's kept for its ABI), 0 lost; the control 0 of 171 |
| `REPR_HASKELL_RUN_IFACES=pack bin/repr-haskell run-check` (after P10, and again after P5 and P8) | 59 of 63, 62 of 63 as recorded (the known `synthetic/primops-word`), the control fails as it must |
| `bin/repr-haskell typecheck-check` | 24 of 24, the control 13 of 24 |
| `bin/repr-haskell lib-check` (the record pack's run, 265 modules) and `libcheck judge` | 260 pass, 5 in their classes; none worse (Backend.GMP out) |
| `bin/hsi libs-pack -O2` | the `-O2` pack's keys hold (`dump_package` unchanged) |
| `bin/gate --full --plan` | `haskell-lib-check` and `haskell-lib-stg` run, never passed; the plan exits 0 |

### What is left

- **HF10-P3 (a), the front end's own interfaces**, the next slice (3 to 5 days): `typecheck/iface-text` writes a module's things and
  instances but not yet what a library's importers read of GHC's interface (the exports, the fixities, the type constructors with
  their data, the families, the DFun unfoldings: `hsiface/exports`, `fixities`, `fam`, `unfoldings`); then lib-check's plan with the
  closures from the own interfaces bottom-up, judged against GHC's `-O0` `.hi` (the helper's `iface` on the reference build), and the
  run on a coherent own `-O0` set without the merge.
- The classes above (P4 (a)'s order): the static constructors and the thunk/function flags first (they change what runs), then the
  closure sets.
- P5's leftovers: `"wrapper"` and dynamic imports (none in the `-O0` reference), unboxArg's three-field ByteArray# product; P8's:
  castArg's casts (a sum of an `Int8#` and a `Word#`), a sum constructed outside its binding's result.

### Night commands (one process at a time, at most 1.5 GiB a process; `. .tmp/env.sh`)

1. `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn --run` (the step `haskell-lib-stg`: 265 modules and the two
   run-checks, about 47 minutes; no GHC).
2. `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` (the step `haskell-lib-check`, now on its pack).
3. `REPR_HASKELL_RUN_IFACES=pack bin/repr-haskell run-check` and `bin/hsi check --front native` (the interfaces pack re-keyed).
4. The oracle tier, by hand: `bin/oracle-records verify haskell stg-libs-O0 --sample 2` (GHC: the reference's two smallest packages
   dumped again and compared).

## As built: HF10-P3 (first slice) (2026-11-09, branch `hs-p3`, from `mac/next` e6214cd13)

By an agent on the main host by day (the light tier: no gate but `--plan`, no build of Arbace, every process under about 1.5 GiB, at
most two at once; stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated; no GHC: GHC's interfaces and
`-O0` STG only as the judges' data, from the packs). The brief: HF10-P3 (a), the user's decision of 2026-11-09: the front end's
own interfaces, written as it compiles ghc-internal bottom-up, so the boot libraries' `-O0` set is coherent without GHC's
interfaces or the pack merge; hs-props2's "What is left" named the gap: our interfaces lacked exports, fixities, type constructors,
families and the default methods' unfoldings. GHC 9.14.1's sources the spec (`compiler/GHC/Iface/{Make,Tidy}.hs`,
`Driver/Config/Tidy.hs`, `IfaceToCore.hs`, `HsToCore/Binds.hs`, `Types/Id/Make.hs`, `Builtin/Types.hs`, `Builtin/Types/Literals.hs`,
`Tc/Utils/TcMType.hs`, `Tc/TyCl/{Class,Instance}.hs`, of the pinned tarball), ported with GHC's notice (LICENSE.md, doc/ARBACE.md).

### Design (HF10-P3-D1 to D6)

- **D1 The format is the helper's iface2 forms.** An own interface is `M.iface.edn` in the forms the renamer's (`load-iface`:
  exports, fixities, the constructors' fields) and the elaborator's (`load-ifaces!`: things, instances, family instances,
  unfoldings) readers already take from GHC's interfaces, so nothing that reads an interface changes and the judge reads both the
  same way. Written by `arbace.repr.haskell.ownface/own-iface-text` from the renamer's result (the avails, the fixities, the source
  bangs) and the elaborator's (its things; the result now also carries its families, its open family instances, its pattern
  synonyms, its values' types and its instances zonked).
- **D2 What a module's interface holds is -O0's** (mkIface_ with GHC.Iface.Tidy at -O0: `opt_expose_unfoldings = ExposeNone`,
  `trimId`): its exports and fixities; every exported thing it defines and every type constructor and class it defines (Tidy keeps
  every TyCon: a boot file's exported Integral names a Real it does not export); ids with their types and no IdInfo (trimmed);
  constructors with dataConSourceArity, field labels, wrapper types, source bangs, -O0's implementation bangs (dataConSrcToImplBang
  with bang_opt_unbox_disable: UNPACK not honoured) and tags; synonyms, families (flavour, a closed family's equations), classes
  (variables, superclasses, methods, functional dependencies by position, default methods), pattern synonyms (patSynSig's parts,
  the builder, a record synonym's field selectors as mkPatSynRecSelBinds types them); instances; family instances (a data
  instance's with its representation tycon); and the only unfoldings -O0 exposes, the compulsory ones: every default method's
  (makeCorePair's is_default_method for IsDefaultMethod, Note [INLINE and default methods]), the elaborator's Core of the `$dm`.
- **D3 Kinds stay erased** (HF9-P4 (a)): an own type constructor is written with a kind of its arity and no invisible binder, so its
  kind arity reads 0 and the types that mention it are written without kind arguments, as the elaborator holds them. A type
  constructor whose reader drops arguments (a kind arity from another interface, an unboxed tuple's or sum's representations,
  FUN's multiplicity and representations) gets placeholders back (`type-form`), so a type round-trips. The one interface that is
  GHC's is GHC.Internal.Prim, the wired-in interface GHC itself makes from `primops.txt.pp` (GHC.Builtin.Utils' ghcPrimIface: no
  source, not compiled), taken from the pack; it names GHC.Internal.Types' wired-in things with their kind arguments, so
  `typecheck/wired-kind-arities` adds GHC.Builtin.Types' (Solo#, MkSolo#, Coercible, ~, ~~, Any).
- **D4 Wired-in definitions win over the source** (GHC.Iface.Tidy's Note [Drop wired-in things]): Multiplicity's constructors in
  multiplicityTyCon's order (One, Many; the source says Many | One), the superclasses of ~~, ~ (~#) and Coercible (~R#) the source
  does not write, GHC.Builtin.Types.Literals' built-in families (+, *, ^, -, Div, Mod, Log2, CmpNat, CmpSymbol, CmpChar,
  AppendSymbol, ConsSymbol, UnconsSymbol, CharToNat, NatToChar) as built in.
- **D5 Bottom-up as --make.** `libcheck own-order`: the plan's modules and the package's 24 hs-boot files in dependency order
  (Kahn's, a SOURCE import an edge to the boot file); each unit's closure the module names of its lib-check closure (GHC's) with its
  transitive imports (the oracle's installed closures name the GMP backend), each member's own interface, a boot file's when the
  module is not compiled yet, Prim the wired one; `libcheck own` compiles them in one process (resumable after a timeout), writing
  `pool/M.iface.edn` (`boot/` for a boot file) and a closure list `ln/M.txt` (no links: the readers take `*iface-files*`, a map of
  module to file, beside their directories, since jrt has no symbolic links). An hs-boot file's signatures are its values
  (`Rename/Module.hs`'s boot binders, `Tc/Module.hs`'s tcHsBootSigs).
- **D6 Two judges, both against GHC's data.** lib-check's own types judge unchanged (each binder against GHC's interface of the
  module), and `ownface/compare-ifaces`: the own interface against GHC's (the iface2 pack, the installed `-O2` build: its
  exports, fixities, things and instances do not depend on the level), both read by the elaborator's readers
  (`typecheck/iface-facts`) and compared fact by fact in the canonical form, instances by their heads (the dictionary functions'
  names apart: GHC's getDFunTyKey spells the invisible kind arguments, `$fCategoryTYPEFUN`), with a negative control (Data.Either's
  own interface against Data.Maybe's GHC interface, renamed: 0 of 19 equal). lib-stg's judge is GHC's `-O0` STG as before.

The front end's rules met on the way (each GHC's): tcShortCutLit (an overloaded literal checked against Int, Word, Integer, Float or
Double is built at its type with no Num wanted: Bignum.Integer's literals need no Num Integer, which GHC.Internal.Num defines above
it); a standalone deriving on an empty data type (`deriving instance Show Void`); a class's functional dependencies kept by
position for its own interface.

### Built

| piece | files | what |
|---|---|---|
| the writer and the judge | `arbace/repr/haskell/ownface.ae` | D1-D4, D6 |
| the readers | `typecheck.ae` (`*iface-files*`, `iface-facts`, `canon`, the result's `:families :fam-insts :patsyns :own-genv :own-insts`, `wired-kind-arities`), `rename.ae` (`*iface-files*`, `*boot*`) | D1, D3, D5 |
| the driver | `libcheck.ae` (`own-order`, `own`, `rank-own`, `own-control`, `closure-files`: a plan's closure `@FILE`) | D5 |
| the checks | `bin/repr-haskell lib-check --ifaces own` (`.tmp/repr-haskell/lib-check/own/PKG/`), `lib-stg --ifaces own [--run]` (`.tmp/repr-haskell/lib-stg-own/`), run-check's `REPR_HASKELL_RUN_IFACES_OVER=DIR` | |
| the records | `test/haskell/lib-check-own-ghc-internal.edn`, `test/haskell/lib-stg-own-results.edn` | `--judge` holds them |

### The measure (ghc-internal, 265 modules and 24 boot files, one process, about 9 minutes)

| | GHC's interfaces (the pack) | own interfaces, bottom-up |
|---|---:|---:|
| lib-check: modules elaborated without an error, every binder's type equal to GHC's | 260 of 265 | **258 of 265** (the first own run of the slice: 237) |
| binders equal | 5,002 of 5,003 | 5,002 of 5,003 |
| interfaces: facts equal to GHC's (D6) | - | **20,681 of 20,891**; 248 of 265 modules every fact; classes 136/136, datacons 3,310/3,310, tycons 647/647, fixities 114/114, patsyns 17/17, types 5,022/5,026, exports 8,541/8,679, instances 2,688/2,747, synonyms 180/182, families 26/33 |
| closures | GHC's | 22,606 own interfaces, 400 boot, 289 Prim (wired), 9 missing (GMP's backend 7, base's GHC.ST, GHC.Stack: the oracle's closures) |
| lib-stg: bindings equal to GHC's `-O0` STG (265 modules) | 5,372 of 7,593 (hs-props2's record) | **5,280 of 7,627** (5,140 before the default methods' unfoldings) |

The 7 modules that do not pass on their own interfaces: the 5 that do not on GHC's (Data.Typeable.Internal, Exception.Context,
Generics, Data.Dynamic, TH.Lift: lib-check's classes), Data.Data (`deriving instance (a ~ b, Data a) => Data (a :~: b)`: Typeable
(a :~: b) from the given equality, not solved on the own interfaces) and none else. The interfaces' differences: the backend's
exports (GHC's installed build is the GMP backend's, 138: an oracle class, HF10-P6), the derived instances on data family instances
(URec's Eq, Ord, Show, Foldable, Traversable, Functor, Generic: the elaborator's deriving does not take a data instance) and
Generic's Rep and Rep1 equations (not derived), the constraint tuples (CTuple0 is CUnit, HF10-P9 (b)), 4 types of
unsafeWithSNatCo's family (an inner constraint).

lib-stg's 92 bindings fewer than on GHC's interfaces, by class: the default methods' unfoldings are the elaborator's Core of the
`$dm`, where GHC's is the `$dm`'s stable unfolding after its own module's `-O0` Simplifier (simplStableUnfolding: `not (x == y)`
with `not` inlined in GHC.Internal.Classes, so GHC's `$fEqProxy`'s `/=` is `False` where ours calls `not True`; `:r / :u` 182 and
`(case / (let` 250 at the head); the dictionary functions' names with kinds (`$fCategoryTYPEFUN`). Both are the next slice's.

### Programs on the coherent own set

`bin/repr-haskell lib-stg --ifaces own --run ID...` (a sample of ten of run-check's programs by day: samples/Arrays, Exc, LibMisc,
Stm, Classes, synthetic/class-bits, class-integral, class-realfrac, primops-int, runtime-integer; the 63 at night), the programs
compiled against the pack's interfaces with ghc-internal's replaced by the own ones (`REPR_HASKELL_RUN_IFACES_OVER`), no merge:

| set | pass |
|---|---:|
| the pack (the `-O2` libraries, GHC's `-O2` interfaces), run-check's record | 10 of 10 |
| ghc0: GHC's `-O0` STG of ghc-internal, base, ghc-prim and ghc-bignum, the programs against the own interfaces | **8 of 10** |
| own, LIB_STG_REPLACE=equal: ghc0 with the 70 ghc-internal modules whose every binding equals GHC's `-O0` replaced whole by Arbace's own STG | **8 of 10, 0 lost** |
| own, all: every one of the 258 modules the front end made STG of (no merge) | 0 of 10 |

HF10-D5's coherent `-O0` set ran 35 of 63 programs because the programs were compiled against `-O2` interfaces (their unfoldings
name `-O2` workers); against the own interfaces the `-O0` set is coherent: the two that fail stop outside them (Exc: SrcLoc's
fields, the programs' constructor layout read from the libraries' STG; LibMisc: `ioException3`, an `-O2` name of a package still the
`-O2` pack's). The whole own set stops at what lib-stg's judge already counts: Typeable's kind representations GHC.Internal.Types
does not define (`krep$*`, GHC's mkKindRepBindings of the wired-in kinds) and a local left unbound by an unequal binding (`w_188`,
the second slice's `LIB_STG_REPLACE=all` class).

### What remains to switch hsi's eval and AOT to own libraries

1. The default methods' unfoldings after the defining module's Simplifier (lib-check then runs the pipeline to the Simplifier, as
   lib-stg does, and writes the stable unfolding of each `$dm`); the dictionary functions' names with their kind arguments
   (getDFunTyKey over every argument: needs the kinds HF9-P4 (a) erased, or a kind arity per class kept from the declaration).
2. base, ghc-prim and ghc-bignum bottom-up on ghc-internal's own interfaces (`lib-check --package base --ifaces own`: the driver
   takes a second package once its closures name the first's pool), then the run corpora's packages (containers, bytestring, text,
   stm, unix, directory, filepath, deepseq, array, time).
3. GHC.Internal.Prim's interface made from `primops.txt.pp` (the pinned tarball's; `primops.ae`'s generator already reads it) instead
   of the pack's: the last GHC interface of the path.
4. Their STG (lib-stg's own set) for every package, then `bin/hsi check --front native --libs own`: the programs' interfaces the
   own ones, the libraries the own STG; the cases 1 and the packs out of the eval and AOT keys.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check --ifaces own` (265 + 24, one process) | 258 of 265, 20,681 of 20,891 facts; the control differs (0 of 19); `--judge test/haskell/lib-check-own-ghc-internal.edn`: 0 worse |
| `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` (GHC's interfaces, after the slice's elaborator rules) | 260 pass, 5 in a class, 0 worse, the control differs |
| `bin/repr-haskell lib-stg --ifaces own` (265 modules) | 5,280 of 7,627 equal; the control 0 of 171 |
| `bin/repr-haskell typecheck-check` | 24 of 24, the control 13 of 24 |
| `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn` (GHC's interfaces, 265 modules) | 5,373 of 7,600 (recorded 5,372); 0 worse, 1 better (Read); the control 0 of 171. The judge had read no row of its record (record-stg writes sorted maps, `{:compared ...`, and `read-results` took only lines starting `{:name`: every module "not in the record", the step failing); fixed, it reads any map line with a `:name` |
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` | 0 worse |
| `REPR_HASKELL_RUN_IFACES=pack bin/repr-haskell run-check` (HSI_LIBS the `-O2` pack, one front shard) | **60 of 63** (59 before), 63 of 63 as recorded, the 3 non-passes in their classes; the control fails as it must |
| `bin/repr-haskell lib-check --ifaces own`'s interface control | Data.Either's own against Data.Maybe's: 0 of 19 equal (and against its own GHC interface every fact equal) |
| `make check-makefile`, `bin/gate --full --plan` | 0; the plan exits 0 (no step added: HF10-P11) |

### Night commands (one process at a time, at most 1.5 GiB a process; `. .tmp/env.sh`)

1. `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` (about 9 minutes; the pool for 2).
2. `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run` (the 265 modules and run-check's 63
   programs on the two coherent sets: about 50 minutes).
3. `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn --run` (the step `haskell-lib-stg` after the slice's elaborator
   rules: about 47 minutes) and `REPR_HASKELL_RUN_IFACES=pack bin/repr-haskell run-check`.

### Proposed (HF10-P11 to HF10-P14; options with costs, a recommendation each, none decided)

- **HF10-P11 The own interfaces as a recorded `--full` step.** (a) **`haskell-lib-check-own`: `lib-check --ifaces own --judge
  test/haskell/lib-check-own-ghc-internal.edn` beside `haskell-lib-check` (light, about 9 minutes, 1.5 GiB, the iface2 pack's pin, no
  GHC), its control the interface judge's**; half a day; (b) by hand until base joins. *Recommended: (a)*: the interfaces are what
  every later step stands on.
- **HF10-P12 The default methods' unfoldings.** (a) **after the defining module's `-O0` Simplifier (lib-check `--ifaces own` runs the
  pipeline to the Simplifier and writes each `$dm`'s stable unfolding, its Core in the helper's spelling), lib-stg then on par
  with GHC's interfaces or better**; 1 to 2 days, lib-check's run about twice as long; (b) the elaborator's Core as built (92
  bindings fewer). *Recommended: (a)*.
- **HF10-P13 Kinds for the dictionary functions' names.** GHC's getDFunTyKey spells the invisible kind arguments (198 of 2,709 names
  differ; an importer of an own interface is coherent with it, GHC's STG is not). (a) **a class's and a type constructor's kind
  arity kept from its declaration (the binders' kind annotations: `(a :: k)`), its instances' kind arguments inferred as far as
  the heads' type constructors give them**; 1 to 2 days, part of HF9-P4's kinds; (b) leave the names (the judge keys by heads).
  *Recommended: (b)* until the eval switch needs the pack's names, then (a).
- **HF10-P14 The next package.** (a) **base on ghc-internal's own pool (the driver takes a package's pool as the next package's
  wired-in set), then ghc-prim and ghc-bignum, then the run corpora's packages, each with its own lib-check and lib-stg records**;
  2 to 4 days for base; (b) GHC.Internal.Prim from primops.txt.pp first. *Recommended: (a)*, (b) after: Prim is the compiler's own
  wired-in interface, as GHC's.

## Decided (the user, 2026-11-09: HF10-P11 to HF10-P14)

HF10-P11 to HF10-P14 accepted as recommended: P11 (a), the own interfaces a recorded `--full` step; P12 (a), the default
methods' unfoldings after the defining module's `-O0` Simplifier; P13 (b), the dictionary functions' names left as they are
(the judge keys instances by their heads) until the eval switch needs the pack's names, then (a); P14 (a), base on
ghc-internal's own pool, then the next packages, each with its own records ((b), Prim from `primops.txt.pp`, after).

## As built: HF10-P11 to P14 (2026-11-09, branch `hs-p11`, from `mac/next` 6c3c87c02)

By an agent on the main host at night's edge (the light lane: no gate but `--plan`, no build of Arbace, every process under
about 1.5 GiB, at most two at once; stage 0 `/root/arbace/target/seed/amd64/arbace`, the checkout's sources evaluated; no GHC:
GHC's interfaces and `-O0` STG only as the judges' data, from the packs). GHC 9.14.1's sources the spec
(`compiler/GHC/HsToCore/Binds.hs`'s makeCorePair, `Core/Opt/Simplify/Iteration.hs`'s simplStableUnfolding,
`Core/Opt/Simplify/Utils.hs`'s updModeForStableUnfoldings, `Rename/Pat.hs`'s generalizeOverLitVal, of the pinned tarball;
BSD-3, LICENSE.md "GHC").

### Where Haskell's independence stands, and why these do not move it yet

`bin/crops-score --crop haskell` on `next` 6c3c87c02: independence **76.0%** = (read 1 + eval 0.5 + AOT 0.5 + print 1 + judge
0.8) / 5. Read and print are Arbace's alone. Eval (908 cases) and AOT (139) are 0.5 because every program runs over the boot
libraries' STG GHC made (the packs `haskell-stg-libs`, `haskell-stg-progs`): no GHC at check time, but a toolchain's product
consumed. The judge is 0.8: 4,676 cases by their own verdict, 3,116 by GHC's records (the interface level of the round trip,
GHC's recorded runs of the programs without a shipped expectation). Above 90% needs eval and AOT at 1 (4.8 / 5 = 96%): the
programs run over libraries Arbace's front end compiled (HFS-I1), which needs every package a run corpus imports compiled by
it, on its own interfaces, its STG good enough to run. HF10-P11 to P14 are that path's next steps (the interfaces coherent,
their unfoldings GHC's, the second package); no case runs on own libraries yet, so no fact of `test/crops/independence.edn`
changes and the score stays 76.0 (below, "Proposed": the steps that move it).

### HF10-P11 (a): `haskell-lib-check-own`

`test/gate/steps.edn`: `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn`, a `--full`
step after `haskell-lib-check` (both read the package's native dumps in the same work directory), 2 GiB, the host `:stage0`,
pins the corpus, the native inputs, `haskell-lib-ifaces` (the judge's and the closures') and `haskell-stg-libs` (P12's
Simplifier reads the other modules' constructors from it, as lib-stg does), its control the interface judge's
(`test/haskell/lib-check-controls.tsv`: Data.Either's own interface against Data.Maybe's GHC interface, renamed: 0 of 19 equal);
in `:crops :haskell` and `:controls`. `bin/gate --full --plan` plans it (`run haskell-lib-check-own ... never passed`);
`test/gate/costs-test.bash`, `crops-test.bash`, `cover-test.bash`, `declared-env-test.bash` pass. About 8.5 minutes.

### HF10-P12 (a): the default methods' unfoldings after the Simplifier

GHC gives each `$dm` a compulsory unfolding of its right-hand side (makeCorePair, `is_default_method`), and the Simplifier of the
defining module simplifies that template (simplStableUnfolding in updModeForStableUnfoldings' mode: inlining on, eta expansion
off inside it, the phase the activation's) before Tidy exposes it: GHC.Internal.Classes' `$dm/=` is `\d x y -> case (==) d x y
of False -> True; True -> False`, `not` inlined, where the elaborator's Core is `not ((==) d x y)`.

- `libcheck/dm-unfoldings`: in `lib-check --ifaces own`, a module that defines a default method goes through lib-stg's pipeline
  (`tostg/module-stg`: desugared, the simple optimiser, the `-O0` Simplifier) and each `$dm`'s compulsory unfolding is taken
  as the Simplifier left it; a template with a free local (a binding the Simplifier floated to the top, which GHC's Tidy would
  externalise) keeps the elaborator's Core. `ownface/helper-core` writes the Simplifier's Core in the helper's spelling (a name
  vector as `(ext NS M OCC)`, `:default` as `default`, casts dropped, as GHC's interfaces carry them).
- `simplify/simpl-stable-unfolding`: the template simplified in updModeForStableUnfoldings' mode (it was the outer mode's;
  the eta expansion to the binder's arity stays the outer mode's, seEtaExpand).
- The source path of a module in `--ifaces own` is lib-stg's (`libraries/UNIT/src/M.hs`, the text patError and SrcLoc carry).
- The driver's `--libs`: HSI_LIBS, else `bin/hsi libs-pack -O2`, as lib-stg's.

| | before (hs-p3) | after |
|---|---:|---:|
| `$dm`s written from the Simplifier | 0 of 156 | **154 of 156** (2 with a floated local keep the elaborator's Core) |
| lib-check --ifaces own: modules, facts | 258 of 265; 20,681 of 20,891 | the same (the judge compares no unfolding); 8 m 17 s |
| lib-stg --ifaces own: bindings equal to GHC's `-O0` | 5,280 of 7,627 | **5,398 of 7,627**: 54 modules better, 0 worse |
| lib-stg on GHC's interfaces (the step `haskell-lib-stg`) | 5,372 of 7,600 | 5,373 of 7,600, 0 worse (the mode change alone) |

The own set is now ahead of GHC's `-O2` interfaces (5,398 against 5,373): the `-O0` interfaces are the ones GHC's `-O0` STG was
compiled against. `test/haskell/lib-stg-own-results.edn` re-recorded.

### HF10-P13 (b): the names left

Nothing built: the interface judge keys instances by their heads and counts the names apart (`:dfun-names`, ghc-internal 2,511
of 2,709 the same); an importer of an own interface is coherent with it. (a) waits for the eval switch.

### HF10-P14 (a): base on ghc-internal's own pool

`bin/repr-haskell lib-check --package base --ifaces own` (`LIB_CHECK_DEP_POOLS`, default ghc-internal's own pool, which
`lib-check --ifaces own` makes first):

- **No GHC closure.** The pack `haskell-lib-ifaces` holds ghc-internal's closures only; base's modules' closures are grown by the
  driver (`libcheck/grow-closure`) as GHC's `--make` loads interfaces on demand: the module's imports, the in-package closures of
  own-order, MonadFail's module, then every module an interface of the set mentions (`(ext NS "M" ...)`), to a fixed point, over
  the own pools (base's, then ghc-internal's: `own-one`'s `:dep` source), GHC.Internal.Prim the wired-in one.
- **ghc-prim's reexported modules.** ghc-prim 9.14's library is a `Dummy.hs` and thirteen `reexported-modules` (`GHC.Internal.Prim
  as GHC.Prim`, `GHC.Internal.Types as GHC.Types` ...); `libcheck/cabal-reexports` reads them from the pinned tarball's
  `ghc-prim.cabal` (Cabal's ModuleReexport items, `arbace.repr.haskell.cabal`), the driver maps an import through them and writes
  `reexports.edn` beside the module's closure for the renamer (its existing alias reader, ghc-pkg's `M from unit:N`). The first run
  without them stopped 5 modules and left GHC.Prim, GHC.Types, GHC.Tuple, GHC.Prim.Ext and GHC.Prim.PtrEq missing (12 times).
- **The unit id** is the installation's (`test/haskell/target/installation.edn`, `base-4.22.0.0-3875`; ghc-internal's is wired
  in): GHC's names and Typeable's module records spell it (the first lib-stg run compared 0 bindings under `base:`).
- **The judge.** Types and interface facts against GHC's interface of the module where the pack `haskell-ifaces-ghc-9.14.1` holds
  one (the programs' closures' interfaces, the helper's `iface` verb: 113 of base's 241 modules), else none; the control
  `test/haskell/lib-check-controls-base.tsv` (Data.Complex's own interface against Data.Functor.Product's GHC one: 0 of 26 equal).
  `lib-stg --package base --ifaces own` judges every module against GHC's `-O0` STG of base (the pack `haskell-stg-libs-O0`
  holds it), its control `test/haskell/lib-stg-controls-base.txt` (Data.Complex rotated: 0 of 23).
- **NumDecimals** (found on base): GHC's renamer makes an integral fractional literal an integral one (generalizeOverLitVal,
  `1e12 :: Integer`); ported in the renamer's literal names and the elaborator's `overlit` (`rename/integral-fl`, exponent
  -100..100, Base10 and Base2): System.CPUTime.Posix.ClockGetTime, RUsage and Times elaborate.

| base (241 modules, no boot file) | |
|---|---:|
| elaborated without an error | **235 of 241** |
| closures | 372 own interfaces of base, 14,048 of ghc-internal's own pool, 241 wired (Prim), **0 missing** |
| binders' types equal to GHC's (the 113 judged) | 224 of 224 |
| interface facts equal to GHC's (the 113 judged) | **6,780 of 6,785**; exports 6,147/6,147, classes 14/14, datacons 23/23, tycons 28/28, types 224/224, synonyms 11/11, instances 330/335 |
| lib-stg: bindings equal to GHC's `-O0` STG of base | **531 of 1,024** (41 modules with bindings; most of base re-exports ghc-internal) |
| time | lib-check about 3 minutes, lib-stg about 3.5 |

The 6 modules with errors, by class: Template Haskell's untyped quotes (Data.Fixed, Data.Array.Byte: `:th-quotes`, as
ghc-internal's TH.Lift); kinds (Data.Functor.Compose: Typeable of a poly-kinded `Compose f g a`, `:kinds`); a quantified or
higher-rank method context (Data.Functor.Classes: `liftEq` at `f`'s application, Eq1's superclass `forall a. Eq a => Eq (f a)`);
deriving through Generic1 (Data.Functor.Contravariant: `no representation dictionary`); GHC.RTS.Flags (a record of functions
unified with FUN). The 5 instances: the derived instances' contexts (`Generic (WrappedMonad m a)`: GHC's empty context, ours
`Generic (m a)`), as ghc-internal's data-family deriving. Records `test/haskell/lib-check-own-base.edn`,
`test/haskell/lib-stg-own-base-results.edn`; the Makefile's `repr-haskell-lib-check-own-base`, `repr-haskell-lib-stg-own-base`.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` | 258 of 265, 20,681 of 20,891 facts; 0 worse; the control differs (0 of 19) |
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` | 5,398 of 7,627; 0 worse, 54 better (re-recorded); the control 0 of 171 |
| `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn` (GHC's interfaces) | 5,373 of 7,600; 0 worse; the control 0 of 171 |
| `bin/repr-haskell lib-check --package base --ifaces own --judge test/haskell/lib-check-own-base.edn` | 235 of 241; 0 worse; the control differs (0 of 26) |
| `bin/repr-haskell lib-stg --package base --ifaces own --judge test/haskell/lib-stg-own-base-results.edn` | 531 of 1,024; 0 worse; the control 0 of 23 |
| hsi-essential's command (`bin/hsi own-verdicts --check && bin/hsi check ... --only @test/haskell/hsi/essential.txt`, one job) | 123 programs, 0 not passing in no class, the four controls fail as they must |
| `make check-makefile`; `bin/gate --full --plan` | 0; the plan exits 0 with `haskell-lib-check-own` |

### Night commands (one process at a time, at most 1.5 GiB a process)

1. `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` (the step, about 9 minutes; the
   pool for 2 and 3).
2. `bin/repr-haskell lib-check --package base --ifaces own --judge test/haskell/lib-check-own-base.edn` and
   `bin/repr-haskell lib-stg --package base --ifaces own --judge test/haskell/lib-stg-own-base-results.edn` (about 7 minutes).
3. `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run` (the 63 programs on the two coherent
   sets, about 28 minutes since HF10-P3's second night: `--ifaces own --run` replaces the `equal` modules by default, `LIB_STG_REPLACE=all`
   explicit for every own binding) and the step `haskell-lib-stg` (`bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn --run`).

Measured 2026-11-10 (`.tmp/r7.log`, no variable): `lib-stg --ifaces own --judge ... --run` exits 0 in 27 min 54 s, 1.7 GiB peak: 5,398 of 7,627
bindings equal, 76 ghc-internal modules own whole and 191 GHC's `-O0`, **50 of 63 programs on each side, 0 lost, 0 gained**. Why 50 and not
the 53 of `run-check.edn` (or the 59 of the `lib-stg --run` slice): the two sides of `--ifaces own --run` are the coherent `-O0` sets, and the
programs are compiled against the `-O2` pack's interfaces (`REPR_HASKELL_RUN_IFACES=pack`), whose names (`ioException3`, `prettySrcLoc`'s
workers) the `-O0` STG does not define: ten programs (samples/Exc, LibBytes, LibIO, LibMisc, LibPosix, LibSystem, LibText, Trace,
synthetic/runtime-exceptions, runtime-stm) stop at "no binding of ghc-internal:GHC.Internal.IO.Exception/ioException3" or a case on an Int64
in `prettySrcLoc`, identically on both sides and in the earlier run (`r3`, under load of another night): deterministic, not load. The same ten
alone on the `-O2` pack: 10 of 10 pass (46 s). The 50 is the baseline of the `-O0` sets; closing it needs the programs compiled against the
own interfaces of the same `-O0` ABI (a later slice), not a change of the checks.

### Proposed (HF10-P15 to HF10-P19; options with costs, a recommendation each, none decided)

- **HF10-P15 base's records as a gate step.** (a) **`haskell-lib-own-base`: lib-check and lib-stg of base on the own pools after
  `haskell-lib-check-own` (light, about 7 minutes, 2 GiB), the ghc-internal pool its producer's `:outputs` (GATE_OUT, put back
  when the producer is skipped), its controls the two above**; half a day; (b) by hand until the run corpora's packages join.
  *Recommended: (a)*: each later package stands on it.
- **HF10-P16 GHC's interfaces of all of base for the judge.** 128 of base's 241 modules have no GHC interface in any pack (the
  `haskell-ifaces` pack holds the programs' closures). (a) **the pack `haskell-lib-ifaces` grown by base's (`lib-check --live
  --record-pack --package base`, the iface2 verb, by hand with GHC at the oracle tier; the driver's closures for base made
  live as for ghc-internal)**: GHC needed once, by the main session; (b) base judged by lib-stg alone where the pack lacks it.
  *Recommended: (a)* at the next GHC window: the interface judge is what caught the reexports and the unit.
- **HF10-P17 The next packages.** (a) **a generic `--package P` over the installation's dependency order
  (`installation.edn`'s units; the own pools of every package below as `--dep-pools`, each package's reexports from its
  .cabal): ghc-bignum (a `Dummy.hs` and reexports alone, from ghc-internal and base), then the run corpora's (containers, bytestring, text, stm,
  unix, directory, filepath, deepseq, array, time, transformers, mtl), each with lib-check and lib-stg records against GHC's
  `-O0` STG (the pack `haskell-stg-libs-O0` grown to them: GHC once, at the oracle tier)**; 3 to 5 days; (b) one package at a
  time as a corpus needs it. *Recommended: (a)*.
- **HF10-P18 GHC.Internal.Prim from primops.txt.pp** (HF10-P14 (b)): the last GHC interface every closure reads (wired-in, 289 +
  241 uses), made by `primops.ae`'s generator from the pinned tarball's `primops.txt.pp` as GHC.Builtin.Utils' ghcPrimIface;
  judged against the pack's fact by fact. 1 day. *Recommended: yes, before HF10-P19*.
- **HF10-P19 The first own cases (the independence path, HFS-I1).** (a) **`bin/hsi check --libs own`: a program whose every
  library module (the closure the run loads) is Arbace's own STG on own interfaces runs as a case of its own, `:own` in
  `test/crops/independence.edn` (1, not 0.5), the rest on the packs; starting with the programs that import only ghc-internal
  and base (samples, synthetic: their closures measured first), the libraries' own STG chosen per module where its bindings
  run (lib-stg's `--run` sets) and GHC's `-O0` STG of a module not yet own still a pack (the case 0.5)**; 2 to 4 days after P17's
  first packages; (b) wait until every package is own, then switch all at once. *Recommended: (a)*: each own case moves eval and
  AOT, and 90% needs both near 1.

## Decided (the user, 2026-11-10: HF10-P15 to HF10-P27)

The plan "Haskell on its own libraries" (HF10-P15 to P19 as one path, staged and costed; its proposals HF10-P20 to P27 with
options and costs) accepted as recommended, (a) each: P15, `haskell-lib-own-base`, base's lib-check and lib-stg records a
`--full` step on ghc-internal's own pool (the producer's `:outputs`); P16, GHC's interfaces of all of base for the judge, at the
same GHC window as P17's packs (one window); P17, the generic `--package P` in dependency order as far as the run corpora import;
P18, GHC.Internal.Prim from `primops.txt.pp` before P19; P19, `bin/hsi check --libs own`, the first own cases, with P20 to P23 its
parts (P13 stays (b)); P20, the whole own set runs (`LIB_STG_REPLACE=all`: the run-breaking classes fixed, `krep$*` and the
floated locals first, judged by runs, run-check's 63 with none lost against `equal`); P21, programs compiled on own interfaces
(`--ifaces own` for run-check and `--front native`, ghc-prim and ghc-bignum as reexport packages); P22, an own case is a record's
`:libs own|pack` with its reason, crops-score counting E and A from the records; P23, one run per case, own when eligible, a case
lost against the pack in `own-fallback.txt` (a list that may only shrink); P24, multi-module programs through the front end
(own-order over the program's modules); P25 (yes), the round trip's interface level judged on own interfaces: the original's own
interface equal to the printed module's fact by fact where both elaborate, **ruled an own verdict of the round trip** (a third
level of the same kind as the reread forms and the `:full` text), GHC's interface level moving to the oracle tier; P26, foreign
stubs by the front end (mkFExportCBits); P27, E, A and J from the records, a corpus whose cases are all own dropping the packs
from its step's pins, the pack runs the oracle tier's comparison.

## As built: HF10-P18 (2026-11-10, branch `hs-p18`, from `mac/hs-p11` 3d55ae343)

By an agent in the light lane (one process at a time, under 2 GiB; stage 0 `/root/arbace/target/seed/amd64/arbace`, the
checkout's sources evaluated; no GHC, no gate run, no network). GHC 9.14.1's sources the specification, from the pinned tarball
(`utils/genprimopcode`, `compiler/GHC/Builtin/{Utils,Types/Prim}.hs`, `compiler/GHC/Types/Id/Make.hs`,
`compiler/GHC/Tc/Instance/Typeable.hs`; BSD-3, LICENSE.md "GHC").

### HF10-P18 (a): GHC.Internal.Prim from primops.txt.pp

`arbace.repr.haskell.primiface` builds the wired-in interface as GHC does without compiling a module (GHC.Iface.Load's
ghcPrimIface): `primops.txt.pp` (the corpus's extract, `compiler/GHC/Builtin/primops.txt.pp`) through Arbace's own C preprocessor
(`arbace.repr.haskell.cpp`, GCC's traditional mode as hadrian runs `cc -E -undef -traditional`), read by a port of genprimopcode's
lexer and parser (Lexer.x, Parser.y; the generated `bytearray_access_ops` and `addr_access_ops` of AccessOps.hs; Main.hs's
desugarVectorSpec for the SIMD entries), each primop's type as primOpInfo builds it (tvsIn, ppTyVarBinders: the inferred levity
and representation binders first, then GHC.Builtin.Types.Prim's template variables, mkTemplateTyVars' names and uniques), its
arity and strictness signature (the `strictness` option's mkClosedDmdSig, the defaults' replicate); GHC.Types.Id.Make's
ghcPrimIds (realWorld#, void#, nullAddr#, seq from their pseudoop entries; coerce, proxy#, leftSection and rightSection as Make.hs
builds them); exposedPrimTyCons with their kinds (pcPrimTyCon0, pcPrimTyCon, pcPrimTyCon_LevPolyLastArg, Proxy#, FUN, TYPE,
CONSTRAINT), roles and arities, and the vector types of `primtype VECTOR`; ghcPrimFixities (seq's infixr 0). The interface is
written in the iface2 verb's format with the own interfaces' header (`{:verb "own" :own 1 :from "primops.txt.pp"}`): the
exports in GHC's order, the things sorted, each type's GHC text and typeF form, the tycons' kinds.

The judge (`primiface check`): against the pack `haskell-lib-ifaces-ghc-9.14.1`'s pool member, fact by fact
(`ownface/compare-ifaces`, each side read with its own kind arities) and every line but the header byte for byte; the negative
control the same build with each primop given the next one's type, which must differ.

| measure | result |
|---|---|
| facts (exports 1,566, fixities 20, types 1,499) | **3,085 of 3,085 equal**, 0 missing, 0 extra |
| lines but the header | **3,155 of 3,155 equal byte for byte** (the type texts, strictness signatures and kinds too) |
| the wired-in kind arities (GHC.Internal.Prim's tycons) | equal |
| the control (types rotated) | 1,840 of 3,085 facts equal: differs, as it must |

`bin/repr-haskell lib-check --ifaces own` builds it first into `OUT/wired/` (seconds), judges it (`OUT/prim-check.txt`, a
difference or a blind control fails the run) and gives it to every closure in place of the pack's (`--wired`): the last GHC
interface every own compilation read (289 closures of ghc-internal, 241 of base) is Arbace's own; GHC's interfaces stay the
judge's (`--ghc-pool`, the control). `LIB_CHECK_PRIM=ghc`: the pack's, as before. GHC's interfaces no longer enter the eval
path of lib-check and lib-stg `--ifaces own` (`lib-stg` reads the closures lib-check lists).

### HF10-P15 (a): `haskell-lib-own-base`

`haskell-lib-check-own` keeps ghc-internal's own pool as its output (`LIB_CHECK_POOL_OUT="$GATE_OUT"`: `ghc-internal-pool.tar`, one
deterministic tar of the 265 interfaces, 6.5 MB, made in 0.01 s: the "~22,000 files" of the plan's risk were the closures' links,
not the pool), and the new `--full` step `haskell-lib-own-base` (after it by `:uses`, the tar's digest in its key) unpacks it
(`LIB_CHECK_DEP_POOL_TAR`), runs `lib-check --package base --ifaces own --judge test/haskell/lib-check-own-base.edn`, then
`lib-stg --package base --ifaces own --judge test/haskell/lib-stg-own-base-results.edn`, and keeps base's own pool as its output
(`base-pool.tar`, HF10-P17's producer). Its control `test/haskell/lib-check-controls-base.tsv` (`:controls`), the stage group
`:haskell`. 6 minutes 8 seconds, 1.36 GiB peak.

### HF10-P20 (a), started: the whole own set's run-breaking classes

`LIB_STG_REPLACE=all` (every module the front end made STG of, 265, in place of GHC's `-O0`) ran 0 of the day's ten programs (8
of 10 on `ghc0`). Each class found by the runs, fixed, and the next one met:

1. **`krep$*` of the wired-in kinds** (Classes, Exc: "no binding of ghc-internal:GHC.Internal.Types/krep$*"). GHC compiles
   GHC.Types with mkPrimTypeableTodos (`compiler/GHC/Tc/Instance/Typeable.hs`): `tr$ModuleGHCPrim`, the built-in KindReps
   (builtInKindReps: `krep$*`, `krep$Constraint`, `krep$*Arr*`, `krep$*->*->*`), a `$tc` for each of GHC.Prim's type constructors
   with its KindRep, and the module's promoted constructors' `$tc'`. Built (`typecheck.ae`'s `prim-typeable-binds`,
   `promoted-typeable-binds`): the KindReps by mkKindRepRhs over the kinds GHC.Internal.Prim's interface writes (the own one,
   HF10-P18: its kinds are what make this possible without GHC), the synonyms expanded as coreView does (Type, Constraint,
   UnliftedType, ZeroBitType, LiftedRep, UnliftedRep, ZeroBitRep), TYPE's shortcut (KindRepTYPE of a nullary rep or of BoxedRep
   of a levity), the built-in ones found by their normal form, the fingerprints as GHC's (`$tcInt#`'s 9887917896653282234 /
   10957577085936226832, `$tc'True`'s, checked against the `-O0` pack); `krep$`/`tr$` names exported (desugar's isExportedId).
   Left out: the unboxed tuples' `$tc'(#,..#)` (their KindRepFun nests, up to 64 deep, written as one nested term each, took the
   module past its 900 s limit; GHC binds each `$krep` apart: a later step), the wired-in classes' `$tc~`, `$tcCoercible`,
   `Eq#`'s and the like (not met by a run yet). GHC.Internal.Types' STG: 66 to 70 of 85 bindings equal.
2. **An unbound local, `w_188`** (eight of ten: "image: unbound name w_188"): runMainIO's `Weak#` passed to the foreign call
   `rts_setMainThread` inside a closure, left out of the closure's free variables: Stg/FVs counted no foreign call's arguments
   (`tostg.ae`'s `fv-expr`: `ffi` and `primcall` atoms are occurrences). Also lifts four modules' equal bindings
   (System.Posix.Internals, Conc.Sync, AllocationLimitHandler, Stack.CloneStack).
3. **`$tcKindBndr`** (all ten, after 1): KindRepVar's field type is the synonym KindBndr; GHC's KindRep expands it (Int).
4. **"unboxed tuple of the wrong size"** (all ten, after 3), bisected (`LIB_STG_REPLACE=@FILE`, a list of modules; the equal set
   plus a prefix of the rest, samples/Arrays, eight probes) to GHC.Internal.Bignum.Primitives: `plusWord12# :: Word# -> (# Word#,
   Word# #) -> (# Word#, Word# #)` took its tuple as one binder where Unarise's unariseFunArgBinder makes two (the callers pass
   two). Built: a lambda's binder of an unboxed tuple type unarised into its components (`binders`, the existing `*multi*` of a
   case on it binding the alternative's), and an argument of an unboxed tuple type unarised at the call (`with-atoms`: an
   unarised local's components, a construction's arguments, else a case taking the value apart); `(# #)` stays one void binder,
   as GHC's.
5. **A primop as a value** (after 4, bisected again to GHC.Internal.Bignum.WordArray: "no binding of
   ghc-internal:GHC.Internal.Prim/writeWordArray#"): `mwaWrite# = writeWordArray#` became a thunk of the primop's name, which
   has no binding; CorePrep's maybeSaturate eta-expands a primop to its arity. Built (`primop-value?`, `primop-eta`: a right-hand
   side, an expression and an argument), `mwaWrite#` now a four-argument closure over `writeWordArray#`, as GHC's.

Where it stands: lib-stg on the own interfaces 5,398 to **5,412 of 7,632** bindings equal to GHC's `-O0` (eight modules better, 0
worse; `test/haskell/lib-stg-own-results.edn` re-recorded), the `equal` set 76 to 78 modules; the whole own set still **0 of 10**:
"unboxed tuple of the wrong size" remains (under withMVar's `takeMVar` thunk in the signal handlers' setup), and the second
bisection met other classes among the first 46 modules of the rest (GHC.Internal.List's `product` path: an unbound `sat_556`, a
floated local of the kind the plan names; the wrong size again before WordArray's fix). The method for the tail:
`LIB_STG_REPLACE=@FILE` with the equal set plus a prefix of the rest (`.tmp/p20/bisect.sh` in the branch's worktree: eight
probes, about a minute each on a quiet host), one class at a time, each fix judged by `lib-stg --ifaces own --judge` (no module
worse) before the next run.

### Checks

| check | result |
|---|---|
| `primiface check` (the pack's Prim) | 3,085 of 3,085 facts, 3,155 of 3,155 lines; the control differs |
| `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` (own Prim) | 265 against 265 recorded, **0 worse**, 0 better (258 of 265, as recorded); the control differs (0 of 19); 7m55s, 1.73 GiB |
| `lib-check --ifaces own --judge ...ghc-internal.edn` again after the P20 fixes | 265 against 265, 0 worse; 19 min (a loaded host) |
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` (after the P20 fixes) | 5,412 of 7,632, **0 worse**, 8 better (re-recorded); the control 0 of 171; 24 min, 1.75 GiB |
| `LIB_STG_REPLACE=all ... lib-stg --ifaces own --run` (the day's ten) | ghc0 8 of 10; all 0 of 10 (before: krep$*, w_188; now: the wrong size, the tail above) |
| hsi-essential's command (`bin/hsi own-verdicts --check`, `check --only @test/haskell/hsi/essential.txt`) | 123 programs, 0 not passing in no class, the four controls fail as they must |
| `bin/reap`; `bin/crops-score --judge --strict` | 41 of 44 cells checked, 0 problems; 0 problems (Haskell independence 76.0: no case runs on own libraries yet) |
| `haskell-lib-own-base`'s command (the pool through the tar) | lib-check 241 against 241, **0 worse** (235 of 241); lib-stg 531 of 1,024 bindings (recorded 531), 0 worse; both controls differ |

## As built: HF10-P20 (a), continued (2026-11-10, branch `hs-p20`, from `mac/hs-p18` 876e2913c)

By an agent in the light lane (one process at a time under 2 GiB; stage 0 `/root/arbace/target/seed/amd64/arbace`, the
checkout's sources evaluated; no GHC, no gate run, no network). GHC 9.14.1's sources the specification, from the pinned
tarball (`compiler/GHC/Stg/Unarise.hs`, `compiler/GHC/CoreToStg/Prep.hs`, `compiler/GHC/HsToCore.hs`'s patchMagicDefns,
`compiler/GHC/HsToCore/Match.hs`'s matchView, `compiler/GHC/Tc/TyCl/PatSyn.hs`'s mkPatSynRecSelBinds,
`compiler/GHC/Types/Id/Make.hs`'s coerceId; BSD-3, LICENSE.md "GHC").

**Where it stands.** The whole own set (`LIB_STG_REPLACE=all`: every ghc-internal module Arbace's own STG, GHC's `-O0` for
none) runs **40 of run-check's 63** programs (it ran 0 of the day's 10); the `equal` set (the modules whose every binding
equals GHC's `-O0`) runs 50 (83 modules own whole, 0 lost against GHC's `-O0` set's 50). The proof of HF10-P20 (≥ the `equal` set's passes, 0 lost) is not
reached: 10 of `equal`'s passes are lost on `all`, in the three classes of the tail below. lib-stg on the own interfaces
**5,412 to 5,473 of 7,637** bindings equal to GHC's `-O0` (25 modules better, 0 worse; re-recorded), the modules
with every binding equal 59 to 64, the `equal` set of the run 78 to 83 modules.

### Method

Each class was found by a run, its first broken module by bisection (`.tmp/p20/bisect.sh` in the worktree:
`LIB_STG_REPLACE=@FILE` with the `equal` modules plus a prefix of the rest, `LIB_STG_SIDES=own` running the own side alone,
`LO`/`HI` resuming a range; about a minute a probe), fixed, judged by `lib-stg --ifaces own --judge` (no module worse), and
the next run made. Static comparisons of the own STG with GHC's `-O0` found classes without runs:
`arbace.repr.haskell.stgcmp --abi GHC OURS` (every exported binding's arguments' representations), `--rets` (the
unboxed tuples' sizes the callers of each function expect), `--builds` (the sizes each binding builds), `GHC OURS --why`
(each differing binding's first difference). hsi's errors now name what they need: "unboxed tuple of the wrong size"
gives the alternative's size, the values returned and the call's function, "case of an algebraic type on" the
scrutinee (`go/arbace/hs/compile.ae`'s `tupleSizeMsg`, `scrutName`, `Atom.String`).

### The classes fixed

| class | where it broke | the fix (GHC's rule) |
|---|---|---|
| a polymorphic identity's result taken lifted | BigNat's `inline bignat_compare a b` (an Int#) thunked | `tostg/app-type`: a result type variable instantiated from the argument whose type ends in it (inline, lazy, oneShot; runRW#'s `(State# RealWorld -> o) -> o`) |
| a void value case-bound by its rep | Native's `mwaTrimZeroes#` result passed on: unbound sat | a void argument a case on `(##)`, the argument `void#` (Unarise) |
| a nullary void join point returned as `(##)` | Native's `bignat_gcd`: the join's body never ran | a jump to a nullary join point is its call (`*njoins*`) |
| synonyms and newtypes of function types | Latin1's `DecodeBuffer#`, ST's `STRep`, `ST`: results unknown, tuples miscounted | `result-type`, `arg-types` expand synonyms and newtypes (instNewTyCon_maybe) |
| unsafeCoerce# applied | Exts' `atomicModifyMutVar#`: the triple unarised into its arguments | unsafeCoerce# x is x (patchMagicDefns' unfolding, CorePrep) |
| match variables untyped | a State# in `(# s, r #)` bound lifted, a 4-tuple returned for 3 | typecheck types the equations' `ds` binders; desugar's match variables take their pattern variables' types (`desugar/fresh-types`); a cast function's type its coercion's target |
| nested unboxed tuples | Native's case on `quotRemWord3#` bound 2 of 3 | a component binder of several representations unarised into one binder each |
| coerce's unfolding | Utils' `(#.) _f = coerce` returned the evidence, Eq# | `\d x -> x` (coerceId takes the Coercible evidence) |
| Unsafe.Coerce's magic | `unsafeEqualityProof = case unsafeEqualityProof of ...` looped; unsafeCoerce returned the proof | the proof's right-hand side UnsafeRefl, a case on the proof its body (patchMagicDefns, CorePrep) |
| existential dictionaries | Handle__ built with 13 of 17 fields; its selectors took the dictionaries for fields | record construction wants the constructor's context, record update and the selectors bind and pass it |
| a CAF of a primop | Real's `divZeroError = raise# e` a call of raise# | the CAF float shortcut leaves primops and foreign calls to `rhs` |
| view patterns | Encoding.Types' `(getEncode -> encode)` a wildcard, its variable unbound | matchView: `x = F v`, then P on x |
| pattern synonyms' record selectors | BufferCodec's `encode` had no binding | mkPatSynRecSelBinds: the synonym's own pattern, the field's variable, else recSelError |
| unboxed tuple case binders | Integer's quotRem's `wild_941` unbound | the case binder stands for its components (unariseAlts) |
| a tuple case of an unknown scrutinee | withNewWordArray2#'s `runRW# io` bound 2 in a case of 1 | the case's size its alternative's binders' |
| primitive byte strings | the Unicode tables' bitmaps the literal 0: segfaults in generalCategory | `(bytes "...")#` literals as their bytes |

Interim, named as such in the code: `tostg/kinded-results` gives unsafeCoerceAddr's and unsafeCoerceUnlifted's results
their representations; the elaborator keeps no type variable's kind (its interfaces write `Type` for `(a :: TYPE
AddrRep)`), the class `kinds` below.

### The tail (stage 4's fallback list until fixed)

The 10 of `equal`'s passes lost on `all`, by class:

1. **case of an algebraic type on void** (LibContainers, MVarOrder, StmBlock, ThrowMasked, class-enum): a function's
   argument slot holds the void token where a data value is expected; the exception paths (TopHandler's `real_handler`,
   the forked threads' `catch`) meet it first. Candidates from `--abi`: bindings GHC eta-expands over State# that ours
   do not (`Arr.adjust`, `done`, `fill`, `ST.unsafeInterleaveST`, the encoders' `[:L :L :v]`), the void fields of
   constructors (`STret`, `Refl`, `UnsafeRefl`: GHC's Unarise drops them).
2. **output differs** (Echo, class-num, runtime-laziness, runtime-textio): runs to the end with a different stdout or
   stderr; not yet looked at.
3. **NonTermination unhandled** (runtime-loop): "no alternative for NonTermination" in TopHandler's handler.

Beyond the 63: pattern synonym patterns are not desugared (`psyn`, HF9's slice 7: the matcher `$mP`); the kinds of
type variables (`kinds`: TYPE-kinded binders written `Type`; Box constructors' fields, unsafePtrEquality#); the ABI
classes `--abi` lists (117 bindings, most of them eta arities hsi's application absorbs).

### Records and tools

- `test/haskell/lib-stg-own-results.edn`: the module rows re-recorded (5,473 of 7,637), and the runs as rows of their
  own, `{:runs SET :passes N :of 63 :ids [...]}` for `all` (40) and `equal` (50): `lib-stg --ifaces own --run
  --record FILE` writes the set's row (`LIB_STG_REPLACE`'s set), `--judge FILE` fails on a recorded pass lost (on `all`,
  on a pass of `equal`'s row lost: the proof of P20, failing today); `libcheck record-runs`, `judge-runs`; `record-stg`
  keeps the run rows, `judge-stg` reads module rows alone.
- `LIB_STG_SIDES=own` (lib-stg `--run`): the own side alone (bisection), the other side's last results kept.
- `LIB_STG_CORE=1`: also the elaborator's local types (`M.ltypes.edn`) beside the Core.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` | 5,473 of 7,637, **0 worse**, 25 better (re-recorded); the control 0 of 171; 11 min, 1.75 GiB |
| `LIB_STG_REPLACE=all ... lib-stg --ifaces own --run` | ghc0 50 of 63 (as before: the programs compiled by the changed front end lose nothing); all **40 of 63**, 10 lost (the tail), recorded as `{:runs :all}`; `judge-runs` fails on it, as P20's unmet proof; 15 min, 1.56 GiB |
| `LIB_STG_REPLACE=equal LIB_STG_SIDES=own ... --run --record` (judge-runs on it: kept; `all`'s results judged as `equal`'s: fails, the control) | 83 modules own, **50 of 63**, 0 lost against GHC's `-O0` set; recorded as `{:runs :equal}`; 7 min, 1.55 GiB |
| `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` | 265 against 265, **0 worse** (258 of 265; 20,681 of 20,891 facts), 1 better (Encoding.Types' binders 13 to 18: the synonym's selectors; not re-recorded); the control differs; 8 min, 1.78 GiB |
| `lib-check --package base --ifaces own --judge test/haskell/lib-check-own-base.edn` | 241 against 241, **0 worse** (235 of 241); the control differs; 3 min, 1.39 GiB; base's lib-stg 531 to **547 of 1,024**, 0 worse (re-recorded), its control 0 of 23 |
| `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` (the step `haskell-lib-check`) | 265 against 265, 0 worse (260 of 265), 1 better (Encoding.Types); the control differs; 6 min |
| `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn --run` (the step `haskell-lib-stg`) | 5,372 to **5,444 of 7,610**, 0 worse, 31 better (re-recorded); the control 0 of 171; runs 60 of 63 on each side, 0 lost, the 59 recorded kept; 24 min, 1.82 GiB |
| hsi-essential's command | `bin/hsi own-verdicts --check`, `check ... --only @test/haskell/hsi/essential.txt`: 123 programs, 0 not passing in no class, the four controls fail as they must |

### Night commands (one at a time, under 2 GiB each)

1. `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` (11 min), then
   `LIB_STG_REUSE=1 LIB_STG_REPLACE=equal bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run`
   (15 min: both sides, `equal`'s 50 kept) and `LIB_STG_REUSE=1 LIB_STG_REPLACE=all ... --run` (15 min; fails on the 10 lost
   until the tail is fixed: the measure, not a gate step).
2. The `--full` steps the front end's change touches, as the gate runs them: `haskell-lib-check`, `haskell-lib-check-own`,
   `haskell-lib-own-base`, `haskell-lib-stg` (the last with `--run`, about 28 minutes).

## As built: HF10-P20 (a), closed (2026-11-11, branch `hs-p21`, from `mac/next` 5d10a92fb)

By an agent in the light lane (one process at a time under 2 GiB; stage 0 `/root/arbace/target/seed/amd64/arbace`, the
checkout's sources evaluated; no GHC, no gate run, no network). GHC 9.14.1's sources the specification, from the pinned
tarball (`compiler/GHC/Tc/Gen/Match.hs`'s tcGRHS, `compiler/GHC/HsToCore/Match/Literal.hs`'s tidyNPat,
`compiler/GHC/Iface/Tidy.hs`'s rule-exposed Ids, `compiler/GHC/Tc/Solver/Dict.hs`'s implicit parameters, `GHC/Types/Id/Make.hs`'s
data constructor workers; BSD-3, LICENSE.md "GHC").

**Where it stands.** HF10-P20's proof is met: the whole own set (`LIB_STG_REPLACE=all`, every ghc-internal module Arbace's own
STG) runs **50 of run-check's 63** (40 before), every one of the `equal` set's 50 passes kept, 0 lost (`lib-stg --ifaces own
--judge test/haskell/lib-stg-own-results.edn --run`: "every pass of the equal set's 50 kept"; the `{:runs :all}` row re-recorded
40 to 50). lib-stg on the own interfaces 5,473 to **5,533 of 7,639** bindings equal to GHC's `-O0` (0 worse); on GHC's
interfaces 5,444 to 5,457 of 7,612 (0 worse, its runs 60 of 63, the 59 recorded kept). The 13 programs neither set runs are not the own libraries' (GHC's own `-O0` set, `ghc0`, runs the same 50): below.

### The classes fixed

Each found by a run, its first broken module by bisection (`.tmp/p20/bisect.sh`, now with `PAT`: only a failure whose stderr
matches counts, another failure of a mixed set a pass, so a masking failure does not mislead it) or by reading the module's
STG against GHC's (`stgcmp GHC OURS --why`, `stgcmp GHC OURS NAME`), fixed, judged by lib-check and lib-stg `--ifaces own
--judge` (0 worse) before the next run.

| class | where it broke | the fix (GHC's rule) |
|---|---|---|
| an implicit parameter's synonym under a `let ?x` | Exception.Type's default `toException e = SomeException e where ?exceptionContext = emptyExceptionContext`: every instance's inlined copy left the context free (the case on void in the forked threads' handlers) | a wanted HasExceptionContext or HasCallStack is the `let`'s binding of its name (`want!`, `ip-synonym-name`) |
| a binder of a qualified type | withFrozenCallStack's `do_this :: HasCallStack => a` returned unapplied: throwIO's continuation cased on a function | its occurrence takes its constraints' evidence, the call site pushed (`var-occ`) |
| a pattern guard's givens | Data.Typeable's `cast`: `Just HRefl <- ta `eqTypeRep` tb` rewrote the where clause's `Typeable b` to `Typeable a`, every cast succeeded (NonTermination taken for an AsyncException) | each GRHS its own implication, the wanteds rewritten from the first pattern that gives equalities on (`infer-grhss`, `*guard-w*`; eqT's `heqT @a @b` too) |
| an overloaded literal pattern at Word | Num Word's `signum 0 = 0` matched nothing (class-num's signum 0) | tidyNPat at wordTy: `W# n##` |
| -O0 constructor layouts | `$WSrcLoc` unboxed `I# 315#` into SrcLoc's fields, boxed in the own STG: the -O2 dumps' UNPACKed layout | a boot library compiled own (`--o0-layout 1`: lib-check `--ifaces own`, lib-stg `--ifaces own`) lays out its own unit's constructors from their types; lib-stg on GHC's interfaces keeps the -O2 pack's layout it merges over, base's judge too (GHC's `-O0` reference compiles each package against the installed -O2 interfaces: base's SrcLoc unpacked) |
| rule-exposed Ids | Enum's `eftInt` (eftIntList's right-hand side) internal, the programs' interfaces name it | a NOINLINE binding a RULE's right-hand side mentions is external, as GHC's `-O0` output has it (`:rule-rhs-names` ∩ `:noinline`: eftInt, efdtInt external; List's lenAcc, lengthList's, without a pragma, internal: all of a rule's names made List's `length` unequal on the GHC-interface lib-stg) |
| an imported constructor's context | Backtrace's `fmap SomeExceptionAnnotation` built the constructor of its dictionary alone (the interface's arity is the source arity) | its arity counts its context's dictionaries (`iface-con-dicts`) |
| an over-applied polymorphic result | listArray's `foldr go (\_ s# -> s#) es 0# s2#`, a State#, bound by a thunk: the writes never ran, `Array.!` read arrEleBottom | a lambda argument of as many binders as the arguments beyond the declared ones gives the result its body's representations; a case's first known alternative |

### The kinds kept (the interim hard-code replaced)

`tostg/kinded-results` is gone. The elaborator keeps the kinds of a value's type variables where they fix a representation
(`*tv-kinds*`, `:tv-kinds` of its result, `{NAME {TV kind}}`): a signature's kinded binders (`(a :: TYPE 'AddrRep)`,
`UnliftedType`, `TYPE ('BoxedRep 'Unlifted)`; `sig-tv-kinds`) and an interface's forall binders (`iface-tv-kinds`, GHC's and
the own ones); the own interfaces write them (`ownface/*kinds*`: `(forall "a_331_29_331_30" specified (tycon-app (ext tc
"GHC.Internal.Prim" "TYPE") [(tycon-app (ext data "GHC.Internal.Types" "AddrRep") [])]) ...)`, GHC's spelling; the judge
reads kinds as before: lib-check 0 worse). tostg gives a type variable of such a kind its representation, a call's result
(`app-type`) and the binding's own binders (`*bkinds*`). Only closed representation kinds are kept (a kind with a variable,
`TYPE r`, stays the elaborator's Type: the classes' kind arities read kinds' variables).

### The 13 run-check programs neither set runs (for HF10-P21 and P19)

GHC's own `-O0` set (`ghc0`) runs the same 50; the 13 others are not the own STG's:

- 7 call names of ghc-internal that only its -O2 build has, from the other packages' -O2 STG (the pack `haskell-stg-libs`):
  `GHC.Internal.IO.Exception/ioException3` from bytestring, directory, file-io (LibBytes, LibIO, LibMisc, LibPosix,
  LibSystem, LibText), `GHC.Internal.List/reverse1` from stm (runtime-stm). The programs' own STG names none of them, so P21
  (the programs on own interfaces) does not move them: the packages' own STG (P17) does.
- 3 differ in the uncaught exception's report on the `-O0` libraries (Exc, Trace, runtime-exceptions: "Uncaught exception
  ...ErrorCall:" where the -O2 pack's base prints GHC's text); 2 run out of hsi's 2 GiB (Loop, runtime-deep, `:hsi-memory` in
  `test/haskell/run-check.edn` already); AsyncRerun (`:hsi`, recorded).

The plan's "13 are the programs' -O2 ABI" (stage 3's proof, run-check ≥ 53 on own interfaces) does not hold: HF10-P28 below.

### Tools

- `bin/repr-haskell lib-stg`: `--o0-layout 1` passed to `libcheck stg` with `--ifaces own` (`tostg/*o0-layout*`; lib-check's
  `own` verb always).
- `.tmp/p20/bisect.sh` (worktree scratch): `PAT=TEXT` (only that failure counts), `LO`/`HI`.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` | 265 against 265, **0 worse** (re-recorded: Encoding.Types 13 to 18 binders, Data.Dynamic errors to ok); the control differs; 12 min, 1.75 GiB |
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` | 5,473 to **5,533 of 7,639**, 0 worse (re-recorded); the control 0 of 171; 16 min, 1.80 GiB |
| `LIB_STG_REUSE=1 LIB_STG_SIDES=own LIB_STG_REPLACE=all ... --judge ... --run` | **50 of 63**, every pass of `equal`'s 50 kept (judge-runs exit 0); `{:runs :all}` 40 to 50; 8 min, 1.55 GiB |
| hsi-essential's command | `bin/hsi own-verdicts --check`, `check --corpus samples,synthetic,testsuite-core --only @test/haskell/hsi/essential.txt`: 123 programs, 0 not passing in no class, the four controls fail as they must; 18 min, 1.75 GiB |
| haskell-read-essential's command | `bin/repr-haskell test`, the round trip of array, deepseq, containers: 51 of 51 byte-identical, the control differs |
| `lib-check --package base --ifaces own --judge test/haskell/lib-check-own-base.edn`, `lib-stg --package base --ifaces own --judge test/haskell/lib-stg-own-base-results.edn` (haskell-lib-own-base's commands) | 241 against 241, 0 worse; 547 of 1,024, 0 worse (the first try, with the -O0 layout for every package's constructors, lost two of Data.List.NonEmpty's: the unit check); both controls differ; 5 min each, 1.45 GiB |
| `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` (haskell-lib-check) | 265 against 265, 0 worse, 2 better (re-recorded); the control differs; 10 min |
| `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn --run` (haskell-lib-stg) | 5,444 to 5,457 of 7,612, 0 worse (re-recorded); runs 60 of 63 on each side, 0 lost, the 59 recorded kept; 28 min, 1.84 GiB (the first try, with the -O0 layout here too, ran 0 of 63 on the merged set: the `--o0-layout` flag) |

### Proposal

- **HF10-P28 Stage 3's proof restated.** The plan's stage 3 (P21) took run-check's 13 non-passes for the programs' -O2 ABI;
  they are not (above). (a) **P21 as planned (the programs' front end over the own pools of every package, ghc-prim and
  ghc-bignum as reexports), its proof "run-check's 63 on own interfaces ≥ the `all` set's 50, 0 lost"; the 7 that call
  ghc-internal's -O2-only names move with their packages to P17 (bytestring, directory, file-io, stm: own -O0 STG), the 3
  exception reports a class of their own (`:o0-report`) to look at in base's -O0 TopHandler path**; 1-2 days as planned;
  (b) keep "≥ 53" by giving the own ghc-internal GHC's -O2 floated names as aliases (`ioException3`, `reverse1`): mixes the
  ABIs, a list to maintain by hand; rejected. *Recommended: (a)*.

## Decided (the user, 2026-11-11: HF10-P28)

HF10-P28 (a): stage 3's proof restated as "run-check's 63 on the own interfaces ≥ the `all` set's 50, 0 lost"; the 7 programs that
call ghc-internal's -O2-only names wait for P17 (own -O0 STG of bytestring, directory, file-io, stm); the 3 exception-report
differences a class of their own, `:o0-report`.

## As built: HF10-P21, P19, P22, P23 (2026-11-11, branch `hs-p22`, from `mac/next` 4b8fc77e3)

By an agent in the light lane (one process at a time under 2 GiB, at most three by day; stage 0 `/root/arbace/target/seed/amd64/arbace`,
the checkout's sources evaluated; no GHC, no gate run, no network). GHC 9.14.1's sources the specification, from the pinned tarball
(`compiler/GHC/Tc/Gen/App.hs`'s tcInstFun, `compiler/GHC/Tc/Gen/Expr.hs`'s tcRecordBinds, `compiler/GHC/Tc/Solver/Dict.hs`'s IP givens,
`libraries/ghc-internal/src/GHC/Internal/Exception/{Type,Backtrace}.hs`; BSD-3, LICENSE.md "GHC").

**Where it stands.** P21's proof (P28 (a)) is met: run-check's 63 compiled on **every package's own interfaces** run 51 on GHC's own
-O0 libraries (`ghc0`, 50 on ghc-internal's pool alone) and **51 on the whole own set** (`LIB_STG_REPLACE=all`; 50 before), every
pass of the `all` set's 50 kept, 0 lost. P19 (a) with P22 (a) and P23 (a) built: `bin/hsi check --libs own` runs a program the front end
can take on the own interfaces alone over the own libraries' STG alone, **the first own cases**: of samples', synthetic's and testsuite-core's 764 programs, **420 run on Arbace's own libraries (412 pass)**, 342 on the pack (74 of them by the fallback list).

### P21 (a): the programs on every package's own interfaces

`bin/repr-haskell run-check`'s `REPR_HASKELL_RUN_IFACES_OVER` takes a list (`DIR:DIR...`; `run-check/over.txt` names each copied
interface's directory); `lib-stg --ifaces own --run` gives it ghc-internal's pool, its wired-in `GHC.Internal.Prim` (primops.txt.pp's,
HF10-P18) and base's pool (`lib-check --package base --ifaces own`), 507 interfaces (`LIB_STG_PROG_IFACES=ghc-internal`: the pool
alone, as before). ghc-prim and ghc-bignum compile nothing of their own (`Dummy`): their modules are reexports, named by the
pack's `reexports.edn` (GHC.Types → GHC.Internal.Types, GHC.Num.Integer is base's). No pack interface of a boot package is read by
run-check's programs any more: the pack `haskell-ifaces-ghc-9.14.1` gives only the other packages' (containers, bytestring, ...).

### The classes fixed (each by a run, judged by lib-check and lib-stg `--ifaces own --judge` before the next run)

| class | where it broke | the fix (GHC's rule) |
|---|---|---|
| a pattern's implicit parameter | `someExceptionContext (SomeException _) = ?exceptionContext` read the empty context: HasExceptionContext, a synonym of `?exceptionContext :: ExceptionContext`, was not matched by name as HasCallStack is, so the wanted was defaulted (`defaultExceptionContext`); no uncaught exception printed its backtrace on the own libraries (runtime-exceptions) | a constructor pattern's IP givens (`HasCallStack`, `HasExceptionContext`, `?x :: t`) are its alternative's (`ip-pat-env` into `::ip`, `with-ips`); the solver matches either synonym by its name; the module's unscoped existential givens only for `?callStack`, as before (an unscoped `?exceptionContext` took another function's dictionary) |
| a field of a qualified type | `ceaCollectExceptionAnnotationMechanism cea` (`:: HasCallStack => IO SomeExceptionAnnotation`) applied without its call stack: the action was a function | an application's result of a qualified type is instantiated after its arguments (tcInstFun's final instantiation), HasCallStack's call site the field's, GHC's `OccurrenceOfRecSel` label "a use of `f'" |
| a record construction's field | `CollectExceptionAnnotationMechanism { ceaCollect... = SomeExceptionAnnotation `fmap` collectBacktraces }` built no abstraction over the call stack | each field checked against its type (tcRecordBinds' checking), a qualified one abstracting its evidence, as the positional constructor already did |
| the innermost implicit parameter | `errorCallWithCallStackException s stk = ... withFrozenCallStack $ toExceptionWithBacktrace (ErrorCall s) where ?callStack = stk`: the argument's HasCallStack took the `where`'s `?callStack` (stk) instead of the frozen stack withFrozenCallStack gives it: `error`'s backtrace showed collectExceptionAnnotation's and toExceptionWithBacktrace's frames (cgrun016, T11572, 15 testsuite programs) | a qualified argument's IP givens (check-poly's) are in scope ahead of an enclosing `let ?x` (`ip-pat-env`, `with-ips`), GHC's innermost given |
| GHC.Internal.Prim's unit | on the own libraries alone (no GHC stub of Prim) `void#` was named `main:GHC.Internal.Prim/void#` (samples/Cont: no STG for main:GHC.Internal.Prim) | GHC.Internal.Prim is ghc-internal's whether or not the libraries hold its stub (`tostg/ext-str`) |

lib-stg on the own interfaces 5,533 → **5,536 of 7,639** (Exception.Type 20 → 23 of 42), 0 worse; lib-check own 265 against 265, 0 worse;
base's 241 and 547 of 1,024, 0 worse; on GHC's interfaces lib-check 265, 0 worse, lib-stg 5,457 → 5,460 of 7,612 (0 worse; its runs 60 of 63, the 59 recorded kept).

### run-check on the own interfaces (P28 (a)'s classes)

| set (`LIB_STG_REPLACE`, the programs on the own interfaces of ghc-internal, Prim and base) | pass of 63 |
|---|---:|
| ghc0: GHC's own -O0 STG of the boot libraries | **51** (50 on ghc-internal's pool alone) |
| all: every ghc-internal module Arbace's own STG | **51**, 0 lost against ghc0 and the recorded `all` 50 (the row re-recorded 50 → 51) |
| all-base (`LIB_STG_BASE=own`): base's 241 modules own too, no GHC-made STG of the four boot packages | **51**, 0 lost (a new row `{:runs :all-base}`) |

The rows of `lib-stg-own-results.edn` carry `:ifaces` (`:own`: every package's pools) and `:classes` (each non-pass's class);
`libcheck judge-runs` fails on a non-pass in no class (`run-classes`): `:o2-names` (the other packages' -O2 STG calls ghc-internal's
-O2-only names: LibBytes, LibIO, LibMisc, LibPosix, LibSystem, LibText, runtime-stm; P17), `:o0-report` (P28 (a)'s class: Exc and
Trace; with the context restored the report is GHC's text up to the backtrace's line, where `prettySrcLoc` of the own (-O0, boxed)
ghc-internal meets containers' -O2 `SrcLoc` with UNPACKed `Int#` fields: `case of an algebraic type on int64(629)`; it moves with P17
too: containers' own -O0 STG), `:hsi-memory` (Loop, runtime-deep at the light runs' 2 GiB), `:hsi` (AsyncRerun). runtime-exceptions,
the third of P28's report programs, passes.

### P19 (a), P22 (a), P23 (a): `bin/hsi check --libs own`

`bin/hsi check --libs own` (`HSI_LIBS_SET=own`): a program is an own case when the native front end can take it on the own interfaces
alone (`front_eligible` over a directory of the own interfaces only: every import ghc-internal's or base's, or a reexport of one; no C,
no foreign stub, no CPP), it is not in `test/haskell/own-fallback.txt` (P23 (a): the list, empty, that may only shrink) and the front
end compiles it; it runs over the own libraries' STG alone (`HSI_OWN_STG`: ghc-internal's 265 and base's 241 modules, no other
package, no GHC-made module: a run that needed one would stop) and its own image: `:libs :own`. Every other program runs as `--front
ghc` runs it (haskell-stg-progs over haskell-stg-libs): `:libs :pack`, `:front` the reason (`:ineligible`, `:fallback`, the stage that
stopped the front end). `--aot` the same (the own libraries' closures compiled by HE8's per-image cache). Work `.tmp/hsi/check-own`
(`check-aot-own`); `--record` writes `test/haskell/check-hsi-own.edn` (`-aot-own`) beside the pack's records until the switch (P29).

| corpus (`--libs own`, ARBACE_MEM_GIB=2: the light cap) | programs | own (`:libs :own`) | own pass | pack | pack pass |
|---|---:|---:|---:|---:|---:|
| samples, synthetic | 63 | 47 | 43 | 16 | 16 |
| samples, synthetic, testsuite-core (`test/haskell/check-hsi-own.edn`) | 764 | **420** | **412** | 342 (74 fallback) | 312 |

The 8 own non-passes fail on the pack as well (AsyncRerun, CallConv's Cmm, T24809, AtomicPrimops, T3279, conc037, T22671 at the light
cap, numsparks001 `:threaded-way`), in their classes of `test/haskell/divergences.edn`. The fallback list's 74, each with its class:
`:front-run` 49 (the program's own STG: Word's negative literal patterns, T9533; patError's text, dsrun005; C:IP left applied, T246,
IPRun; `$tcMany` named, TypeableEq; ...: the front end's), `:o0-report` 16 (the uncaught report on the own libraries: `fail` in IO's
`throwIO` frame, PatternMatchFail's frames), `:light-cap` 7 (2 GiB, 60 s: -O0 libraries are slower and larger; the gate's 16 GiB to
measure), `:own-lib` 2 (class IP's selector `ip` absent from the own GHC.Internal.Classes: tcrun019, tcrun024). 4 programs the pack
passes at 16 GiB fail at the light cap on either side (Loop, runtime-deep, readMVar1, fptr02). `--aot --libs own`: samples/Hello and
synthetic/class-num built and run from the own libraries' STG compiled to Go (`:vs-hsi :same`), the first own AOT cases.

### Independence

Unchanged in the recorded score (76.0): `test/crops/independence.edn`'s eval and AOT facts name the gate's steps `hsi-check` and
`hsi-check-aot`, which run on the packs; an own case counts when a step runs it (P29). With the night's records the projection is
eval 0.5 + 0.5 x 420/908 = 0.731 (nofib's 58 eligible not yet run), AOT as eval's share if the same programs build (~0.73), judge unchanged: **independence about 85** ((2 + 0.731 + 0.73 + 0.8) / 5 = 85.2), against 76.0.

### Checks

| check | result |
|---|---|
| `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` | 265 against 265, **0 worse**; control differs; 13 min, 1.75 GiB |
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` | 5,533 → **5,536 of 7,639**, 0 worse (re-recorded); control 0 of 171; 11 min |
| `lib-check --package base --ifaces own --judge ...-own-base.edn`, `lib-stg --package base --ifaces own --judge ...` | 241 against 241, 0 worse; 547 of 1,024, 0 worse; controls differ; 4 min each |
| `LIB_STG_REUSE=1 LIB_STG_REPLACE=all lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run` | ghc0 51, own **51 of 63**, 0 lost; judge-runs: the 51 recorded kept, equal's 50 kept, every non-pass in a class; 20 min, 1.6 GiB |
| the same with `LIB_STG_BASE=own LIB_STG_SIDES=own` | **51 of 63**, 0 lost (`{:runs :all-base}`); 10 min |
| `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` | 265 against 265, 0 worse (before the last elaborator change: a night command) |
| `bin/repr-haskell lib-stg --judge test/haskell/lib-stg-results.edn --run` | 5,457 → 5,460 of 7,612, 0 worse (re-recorded); runs 60 of 63, the 59 recorded kept; 29 min, 1.9 GiB (before the last elaborator change: a night command) |
| `bin/hsi check --libs own --corpus samples,synthetic,testsuite-core --record` (ARBACE_MEM_GIB=2) | 764 programs: 420 own (412 pass), 342 pack; `check-hsi-own.edn`; 44 min, 2.0 GiB (then the 83 own failures again with the last fix, 9 passing, the 74 left to the fallback list and rerun) |
| hsi-essential's command | own-verdicts current; 123 programs, 122 pass, 0 unclassed; the four controls that ran fail as they must |
| haskell-read-essential's command | `bin/repr-haskell test` as expected, controls differ; array, deepseq, containers 51 of 51 at the tree and text levels |
| `bin/reap`; `bin/crops-score --record`, `--judge --strict` | 0 problems; scores unchanged (Haskell 76.0), exit 0 |
| `bash test/gate/gate-key-crops-test.bash` | PASS; every changed path is a crop path (`arbace/repr/haskell/`, `bin/lib/hsi/`, `bin/lib/repr-haskell/`) or `bin/repr-haskell` and `test/haskell/`, none in `gate_key`, the runtime key or the seed's |

### Night commands (one at a time; `. .tmp/env.sh`)

1. `ARBACE_MEM_GIB=16 bin/hsi check --libs own --corpus nofib,samples,synthetic,testsuite-core --jobs 4 --record` (the whole
   eval corpus at the gate's cap; the front end 4 shards of 1.5 GiB, then the runs: about 10 GiB, 60-75 minutes, the heavy
   lock as hsi-check): the light-cap fallbacks (`:light-cap` 7) and nofib's 58 eligible measured; the list shrunk by hand.
2. `bin/hsi check --aot --libs own --corpus samples,synthetic,testsuite-core --jobs 2 --record` (8 GiB, about 2 hours: the
   own libraries' closures compiled once per image key, then each program's build): A, the own AOT cases.
3. `bin/repr-haskell lib-check --judge test/haskell/lib-check-ghc-internal.edn` (10 min, 1.5 GiB) and `bin/repr-haskell lib-stg
   --judge test/haskell/lib-stg-results.edn --run` (29 min, 1.9 GiB): the GHC-interface judges after the last elaborator change
   (the innermost IP given), which they ran before.

### Proposals

- **HF10-P29 The own cases in the gate.** `bin/hsi check --libs own` needs ghc-internal's and base's own interfaces and STG, which
  today live in a worktree's `.tmp`. (a) **`haskell-lib-check-own` keeps its pool (as now, `ghc-internal-pool.tar`); a new `--full`
  step `haskell-lib-stg-own` (`lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run`, LIB_STG_REPLACE=all: the
  module rows and the classed `{:runs :all}` row, 2 GiB, about 35 minutes) keeps ghc-internal's own STG as its output
  (`ghc-internal-stg.tar`, :uses the pool); `haskell-lib-own-base` keeps base's pool and STG too; `hsi-check` and `hsi-check-aot`
  run `--libs own` with the three tars as `:uses` (`HSI_OWN_IFACES`, `HSI_OWN_STG` taking a tar), recording `:libs` per case in
  `check-hsi{,-aot}.edn` (the switch: `check-hsi-own.edn` retired); crops-score's eval and AOT facts count the `:libs :own` cases
  from the records (`:cases {:own [E 1] :pack [M 0.5] :c [87 0.5]}`, E and A read by `crops.score` from the records, not by hand)**;
  1 day; the night's hsi-check grows by the front end of about 600 programs (4 shards of 1.5 GiB, about 40 minutes); keys: steps
  and crop paths only (bin/lib/hsi, arbace/repr/haskell: `arbace_crop_paths`), gate_key unmoved. (b) keep `--libs own` a check by
  hand, its record `check-hsi-own.edn`, the facts' counts written by hand at each merge (drift; the steps cannot agree with them).
  *Recommended: (a)*.
- **HF10-P30 P17 scoped: the run corpora's packages own.** The programs that stay `:libs :pack` for an import are the other
  packages' (containers, array, bytestring, text, stm, mtl, deepseq, directory, filepath, process, time, unix); and the 9 run-check
  programs of `:o2-names` and `:o0-report` mix those packages' -O2 STG with the -O0 own ghc-internal. (a) **the plan's stage 5 as
  decided (P17 (a)): `--package P` generalised from base to any installation unit, its own pool and STG, dependency order deepseq,
  array, stm, containers, template-haskell's reexports, transformers, mtl, os-string, filepath, bytestring, text, time, unix,
  file-io, directory, process; each with lib-check and lib-stg records and controls; the -O0 references grown at one GHC window
  (P16, P17's pack, the oracle tier); `--libs own` then takes every own package**; 4-8 days, one oracle window; first slice
  without GHC: deepseq, array, stm (no -O0 reference: judged by runs only, lib-stg's rows written at the window). (b) only the
  packages run-check's 9 need (bytestring, directory, file-io, stm, containers and their dependencies): about half the work, the
  testsuite's other imports stay pack. *Recommended: (a), first slice deepseq, array, stm, then containers*.

## As built: review 1111's fixes to HF10-P20/P21, findings 2 and 8 (2026-11-11, branch `review-fix-1`, from mac/next a7916bb2a)

The independent review of 2026-11-11 (`.tmp/reviews/1111/REVIEW-hs-rust-ml-scm-llvm.md`). **Finding 2, a pattern guard's givens.**
HF10-P20's fix (`infer-grhss`, `*guard-w*`) rewrote only the wanteds from the guard's pattern on; but in a clause with givens (a GADT
match, a signature's `a ~ b`) the GRHS's `*given-eqs*` atom is non-empty, the scrutinee's wanteds (eqT's `Typeable b`, `Typeable c`)
carry that atom itself as `:geqs`, and `tc-pat` grows it by HRefl's `b ~~ c`: `under-givens` rewrote `Typeable c` to `Typeable b` and
every comparison held. `infer-guards`' BindStmt now re-points the wanteds made before the pattern under that atom to a snapshot of the
givens they were made under (the review's second fix). Regression case `test/haskell/eval/front/GuardGivens.hs` (both shapes), its
expected output `GuardGivens.stdout` written by hand from the program's meaning (GHC was not run: its reference line is for the oracle
tier's `run-check --record-ref`), run by `REPR_HASKELL_RUN_IFACES=pack HSI_LIBS=$(bin/hsi libs-pack) bin/repr-haskell run-check
front/GuardGivens` (the program is in no corpus, so run-check reports `:no-ref` and exits 1; its stdout is the proof): next's elaborator
prints `True` six times, the fix `False True False False True True`. Not fixed: the same aliasing between successive patterns of one
clause (`infer-matches`, pre-existing, the review's note). **Finding 8, `--o0-layout`'s unknown module.** `con-reps` gave a constructor
of a module `*units*` does not know the -O0 layout from its type, as the unit's own; it is now another unit's (as `ext-str` names
it, "main"), the pack's layout. Checks in the table below.

| check | result |
|---|---|
| `bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn` (finding 2) | 265 against 265, 0 worse, 0 better; the control differs; 21 min, 1.0 GB |
| `bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn` (findings 2 and 8) | 5,533 of 7,639 bindings equal, as recorded, 0 worse or missing, 0 better; the control 0 of 171; 21 min, 1.3 GB |

## As built: review 1111b (2026-11-11, branch `hs-p22b`, from `mac/hs-p22` e9de0d3b7 with `mac/review-fix-1` merged)

The independent review of 2026-11-11 (`.tmp/reviews/1111/REVIEW-pending-hs-rust-fix1.md`, findings #1-#6 on hs-p22 and
review-fix-1). Each finding, what was done:

- **#1 (two equality-giving guards).** `infer-guards` records `[WI GIVENS-BEFORE]` at every pattern guard that gives equalities
  (`*guard-w*` held the first's index alone), and `infer-grhss` rewrites the wanteds stretch by stretch
  (`rewrite-wanteds-by-stretch!`): from WI_k to WI_k+1 by the givens before pattern k+1, the last stretch by the GRHS's final
  givens, as GHC nests one implication per pattern guard. `GuardGivens.hs` gains `two` (no clause givens: `a ~~ Int`, then
  `b ~~ c`) and `twoG` (the same after a GADT match); their six lines of `GuardGivens.stdout` written by hand from GHC's semantics
  (GHC not run). Before the fix (the merged elaborator) lines 7 and 10 printed `True`; after, all twelve lines as expected.
- **#5 (GuardGivens run by a check).** run-check's default plan takes `test/haskell/eval/front/*.hs` beside samples and synthetic;
  a program without a line in `ref-ghc914.edn` is judged by its own verdict, its committed `NAME.stdout` with status 0 and no
  stderr (the negative control takes the same reference); `run-check.edn` records `front/GuardGivens :pass`. So `haskell-lib-stg`'s
  `--run` (both library sets) and every run-check run it; its GHC reference can be recorded at the oracle window
  (`run-check --record-ref`), which the judge then prefers.
- **#3 (own failures hidden by the pack's classes).** `bin/hsi check --libs own` judges a `:libs :own` non-pass by
  `divergences.edn` only when it fails as `check-hsi.edn`'s line of the program does (kind, status, differs); otherwise it is an
  own-only failure, in a class of the new `test/haskell/divergences-own.edn` whose `:outcome` is its own and whose `:message` its
  stderr's first line holds (a failure on the own libraries now records it, `:err`). Classed by what fails (light runs of
  2026-11-11): AtomicPrimops `:own-memory` (Go's "runtime: out of memory" at the 2 GiB cap in 3-5 s; on the pack killed at the CPU
  limit at 46 s: not the pack's speed), conc037 `:front-no-main` ("panic: no binding of main::Main/main", the front end's STG lacks
  main, as conc036's in the fallback list; on the pack only the scheduling order differs), T24809 `:no-reference-own` (GHC's
  reference did not build; the own front end compiles it).
- **#6 (lib-stg's run classes by id).** `record-runs` writes each non-pass's `:outcomes` (result, differs, stage) beside
  `:classes` and keeps a class only for the same outcome; `judge-runs` fails a classed non-pass whose outcome moved. The rows
  recorded before (`:equal`, `:all`, `:all-base`) have no `:outcomes`: the judge says so and matches them by id until the night's
  re-record writes them.
- **#4 (own-fallback.txt).** The header now says what the list is (eligible programs that did not pass on the own libraries in
  the light runs; 8 of them did not pass on the pack either in that run, each passing in `check-hsi.edn` at 16 GiB); `:light-cap`
  became `:untried-cap` (over the cap on both, never tried at 16 GiB), `:o0-report` became `:own-report` (P28 (a)'s class was
  decided for run-check's -O2/-O0 mix, not for own STG alone). "May only shrink" is enforced: `HSI_OWN_FALLBACK=try` runs the
  listed programs on the own libraries (no `--record`) and fails on one that passes; every `--libs own` run without `--only` fails on
  a listed id its corpora lack (`HSI_OWN_FALLBACK_LIST` names another list, the mode's own test).
- **#2 (check-hsi-own.edn).** Not re-recorded by day: its header marks it INTERIM (three light runs over two states merged by id;
  4 `:libs :pack` lines in no class) and names the night commands that replace it. The "420 own, 412 pass" of "As built: HF10-P21,
  P19, P22, P23" is that merge, not a run of e9de0d3b7's.

| check | result |
|---|---|
| `REPR_HASKELL_RUN_IFACES=pack HSI_LIBS=$(bin/hsi libs-pack) bin/repr-haskell run-check front/GuardGivens` | 1 of 1 pass (its own verdict); the record line added; 1m07, 0.75 GB |
| the same with the merged elaborator (`typecheck.ae` of the merge commit) | 0 of 1: `:fail :differs [:stdout]`, lines 7 and 10 `True` |
| `HSI_OWN_FALLBACK=try bin/hsi check --libs own --corpus testsuite-core --only AtomicPrimops,conc037,T9533,T24809` (ARBACE_MEM_GIB=2; hs-p22's own pools) | three in their own classes, T9533 still listed (`:fail :status 0 :differs [:stdout]`), 0 unclassed (exit 1: no negative control in a run of `--only`) |
| the same mode with a test list naming samples/Hello | "passes on the own libraries and is listed in own-fallback.txt", 1 problem, exit 1 |
| `judge-runs` on a crafted row with `:outcomes` | a non-pass whose differs moved: "fails otherwise than its class's recorded outcome", exit 1 |

Not run (night, over the 1.5 GiB of night 12's day lane): lib-check and lib-stg `--ifaces own` of ghc-internal and base with this
elaborator (the multi-guard change may move a binding) and the whole `--libs own` run; the commands are in `check-hsi-own.edn`'s
header. No key moves (every path is a crop path).

## Decided (the user, 2026-11-11: HF10-P29, HF10-P30)

HF10-P29 (a): the own cases in the gate: a step `haskell-lib-stg-own` keeps ghc-internal's own interfaces and STG as its output,
`haskell-lib-own-base` base's too, `hsi-check` and `hsi-check-aot` run `--libs own` on them (`:uses`), and crops-score counts the own
cases from the records. HF10-P30 (a): P17 scoped as stage 5, the run corpora's packages in dependency order (first slice deepseq,
array, stm, then containers), one GHC oracle window for their references: the plan written, no runs.

## As built: HF10-P29 (2026-11-11, branch `hs-p29`, from `mac/hs-p22b` 8516fcf10)

By an agent in night 12's day lane (one run at a time, each process under 1.5 GiB, MemAvailable at least 12 GiB before a run; stage 0
`/root/arbace/target/seed/amd64/arbace`; no GHC, no gate run but `--plan`, no network). The wiring only: the heavy first runs (the own
STG of ghc-internal with its `--run`, hsi's whole check on the own libraries) are a night's, below.

**The steps** (`test/gate/steps.edn`, `--full`, in this order; each `:inputs [:lib-tools :tools :haskell :hsi]`, no GHC host):

| step | `:uses` | command | `:outputs` | `:mem-gib` |
|---|---|---|---|---:|
| `haskell-lib-check-own` (unchanged) | | `lib-check --ifaces own --judge ...-own-ghc-internal.edn` | `ghc-internal-pool.tar` | 2 |
| `haskell-lib-own-base` | `haskell-lib-check-own/ghc-internal-pool.tar` | lib-check and lib-stg `--package base --ifaces own`, now with `LIB_STG_OWN_OUT` | `base-pool.tar`, **`base-own.tar`** | 2 |
| **`haskell-lib-stg-own`** (new) | the two pools | `LIB_STG_REPLACE=all lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run` | **`ghc-internal-own.tar`** | 2 |
| `hsi-check` (`:heavy`) | `ghc-internal-own.tar`, `base-own.tar` | `HSI_OWN_TARS=... bin/hsi check --libs own --corpus nofib,samples,synthetic,testsuite-core --jobs 4` | | 10 |
| `hsi-check-aot` (`:heavy`) | the same | `... check --aot --libs own ... --jobs 2` | | 8 |

`haskell-lib-stg-own` judges ghc-internal's 265 modules' own STG against GHC's -O0 (the module rows of `lib-stg-own-results.edn`, the
control `lib-stg-controls.txt`), then runs run-check's programs compiled on the own interfaces of ghc-internal, its wired-in Prim
and base (base's pool from `haskell-lib-own-base`'s output, `LIB_STG_BASE_POOL_TAR`) on GHC's -O0 libraries and on the whole own
set (`{:runs :all}` judged: a recorded pass lost, a non-pass in no class or of another outcome, fails). Its pins are
`haskell-lib-stg`'s with the native inputs'; `hsi-check`'s gain `haskell-ifaces-ghc-9.14.1` (the reexports `--libs own` reads) and
the native inputs'. `bin/gate --full --plan`: no table error; the planner places `haskell-lib-own-base` and `haskell-lib-stg-own`
"after" (decided once their producers ran, the outputs' digests in their keys: GT5's early cutoff), and `hsi-check`, `hsi-check-aot`
"after haskell-lib-stg-own, haskell-lib-own-base".

**The tools.**
- `bin/repr-haskell lib-stg` (`bin/lib/repr-haskell/lib-stg`): `LIB_STG_OWN_OUT=DIR` (with `--ifaces own`) keeps `DIR/PKG-own.tar`,
  one deterministic tar (sorted, mtime 0, owner 0) of `pool/` (lib-check `--ifaces own`'s interfaces), `wired/` (ghc-internal's
  GHC.Internal.Prim, primops.txt.pp's) and `stg/UNIT/` (the run's own STG of every module); `LIB_STG_OWN_POOL_TAR=TAR` (ghc-internal):
  the closures lib-check left in `.tmp` are used only when their pool, tarred the same way, is TAR byte for byte; else (the producer
  skipped at its key, a `.tmp` of another state) `lib-check --ifaces own` makes them again (no judge, about 15 minutes, 1.8 GiB) and
  must then agree, or the run fails; `LIB_STG_BASE_POOL_TAR=TAR` (`--run`): base's pool unpacked from the producer's tar in place of
  `.tmp/repr-haskell/lib-check/own/base`.
- `bin/hsi check --libs own` (`bin/lib/hsi/check`): `HSI_OWN_TARS=TAR:TAR...` in place of `HSI_OWN_IFACES` and `HSI_OWN_STG`, each tar
  unpacked into `WORK/own-tars/N`, its `pool/` and `wired/` the interfaces (in that order: the wired-in Prim last), each `stg/UNIT/`
  a library directory. The records stay apart from the pack's: `--record` writes `check-hsi-own.edn` (`-aot-own`); `check-hsi.edn`
  stays the pack's (the own judge compares an own failure with its line, review 1111b's finding 3) and the REAP cells' source.

**crops-score** (`test/crops/score.ae`, `crops.edn`, `independence.edn`): RFP53 (a)'s counting takes Haskell's records. crops.edn's
`:own-inputs :haskell` is one input, `:libs` (the program compiled on the own interfaces and run over the own libraries' STG alone);
Haskell's eval fact reads `:case-inputs {:file "test/haskell/check-hsi-own.edn"}`, the AOT fact `check-hsi-aot-own.edn`. The
records of `bin/hsi check` are one map a line: `input-rows` takes a map row's `:id` with the map as its inputs, `cell-ids` reads the
`:hsi-check` sources (`check-hsi.edn`, `-aot`) form by form; a case counts 1 when its line is `:libs :own`, the cell's other cases
0.5. A record whose first line is `;; INTERIM` counts none (`interim?`: review 1111b's mark on `check-hsi-own.edn`, "not to be read as
a result"): the stated `:cases` stand until the night's whole run re-records it (its `--record` drops the mark). The self-test gains
the case (4 of 10 `:libs :own` counted 4 own, 6 at 0.5), its control (every program `:libs :pack`: 0 own) and the mark's reading.

**Projection.** With the mark lifted on the interim record (a copy, not committed): eval 0.500 → **0.731** (420 of 908 cases own,
the rest 0.5), independence **76.0 → 80.6** with AOT still 0.5; with `check-hsi-aot-own.edn` written by the night (the same share, the
programs that build) AOT about 0.73 and independence **about 85** ((1 + 0.731 + 0.73 + 1 + 0.8) / 5 = 85.2). The recorded score is
unchanged (76.0) until the night's records.

**Keys.** Every changed path is a crop path or the gate's tables (`bin/lib/hsi/`, `bin/lib/repr-haskell/`, `test/gate/steps.edn`,
`test/crops/`, `test/haskell/`, the survey): `bin/arbace-go --gate-key` 9accb085d767... on `mac/hs-p22b` and on the branch,
`test/gate/gate-key-crops-test.bash` passes; the runtime and seed keys untouched (no `arbace/lang`, `go/`, `overlay/`, `jdk/`). The
steps' own keys: `haskell-lib-check-own` unchanged (its command and inputs as before); `haskell-lib-own-base`'s command changed (it
reruns once); `haskell-lib-stg-own` new; `hsi-check` and `hsi-check-aot` rekeyed by their commands, pins and `:uses`.

### HF10-P30 (a): P17's plan (written, not run)

**Why.** Of the eval corpora's 908 programs, those that stay `:libs :pack` for an import (not a C file or CPP) import the other
installed packages; and run-check's 9 non-passes of `:o2-names` and `:o0-report` mix those packages' -O2 STG (GHC's) with the -O0 own
ghc-internal. Each package made own (its interfaces and STG by the front end, on the own pools below it) moves programs to `:libs
:own` and those 9 off the mix.

**The generalisation (one change, the base path's).** `lib-check --package P --ifaces own` and `lib-stg --package P --ifaces own`
already take any installation unit above ghc-internal for the unit id (`installation.edn`) and the driver-grown closures; what is
base's alone today: (1) `--dep-pools` names ghc-internal's pool only: it becomes every own pool below P, in the order below
(`LIB_CHECK_DEP_POOL_TAR` a list, each producer's `PKG-pool.tar`); (2) the reexported modules read from ghc-prim's `.cabal` only: every
dependency's `.cabal` (`reexported-modules`: template-haskell's `GHC.Internal.TH.*`, ghc-boot-th's), its `build-depends` giving the
order; (3) the judge's GHC interfaces from `haskell-ifaces-ghc-9.14.1` (the programs' closures, 113 of base's 241): the window below
grows `haskell-lib-ifaces` by every module of P; (4) `--live`'s iface2 runs `-package ghc-internal`: `-package P` (the window's, by
hand); (5) `hsi check --libs own`'s eligibility (`front_eligible`: an import is own when ghc-internal's, base's or a reexport) reads
the own pools' module lists instead of the two names; (6) a step per package or one chained step `haskell-lib-own-P` after its
dependencies' (`:uses` their pools; `LIB_STG_OWN_OUT` its `P-own.tar`, `hsi-check`'s `:uses` growing by one tar a package).

**The order** (the pinned tarball's `.cabal` files, library `build-depends`; ghc-prim and ghc-bignum are reexports, done):

| slice | package | depends on (beyond base) | modules (approx.) | note |
|---|---|---|---:|---|
| 1 | deepseq | — | 4 | NFData's Generic instances: deriving at -O0 |
| 1 | array | — | 11 | `GHC.Arr` reexported from ghc-internal; unboxed arrays' primops |
| 1 | stm | array | 12 | TVar primops (runtime-stm's `:o2-names`) |
| 2 | pretty | deepseq | 4 | ghc-boot-th's dependency |
| 2 | ghc-boot-th, template-haskell | pretty; ghc-internal | ~10 | mostly reexports of `GHC.Internal.TH.*` (`reexported-modules`) |
| 2 | containers | array, deepseq, template-haskell | 40 | -O2's UNPACKed fields in the pack meet -O0 own code today (`:o0-report`'s `SrcLoc`) |
| 3 | transformers, mtl | transformers | 35, 25 | |
| 3 | exceptions | mtl, stm, template-haskell, transformers | 4 | os-string's and filepath's dependency |
| 4 | bytestring | deepseq, template-haskell (ghc-prim) | 30 | C (`cbits/`): its foreign imports' STG only, the C on ci as hsi runs it |
| 4 | os-string, filepath | bytestring, deepseq, exceptions, template-haskell | 25, 20 | |
| 4 | time | deepseq | 50 | C (`cbits/HsTime.c`) |
| 5 | unix | bytestring, filepath, os-string, time | 60 | hsc2hs modules (made on ci, HFS-I2) |
| 5 | file-io, directory, process | unix, filepath, os-string, time (deepseq) | 5, 15, 10 | the run corpora's System.* programs (LibIO, LibSystem, LibPosix) |
| 6 | binary, text | bytestring, containers, array (deepseq, template-haskell) | 30, 70 | text's C (simdutf: C++, out unless cppi takes it) |

First slice without GHC: deepseq, array, stm (judged by lib-check's own rows and the runs: run-check's runtime-stm and the testsuite's
stm programs; lib-stg's rows written at the window). Then containers, after the window (its -O0 reference).

**The one GHC oracle window** (by hand, the main session, on a host with GHC 9.14.1, `tools/hsdump/GHC_VERSION`; one window, P16's and
P17's references together; each a commit of its own naming `:ghc`'s identity; then `bin/oracle-records verify haskell` as the tier does):

```
export ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace TMPDIR=$PWD/.tmp/tmp
P=deepseq,array,stm,pretty,ghc-boot-th,template-haskell,containers,transformers,mtl,exceptions,bytestring,os-string,filepath,time,unix,file-io,directory,process,binary,text
# 1. GHC's -O0 STG of the packages, lib-stg's reference (HF10-P1's pack grown; the four boot packages' entries kept)
HSI_DUMP_OPT=-O0 bin/hsi dumps --record --packages "$P"                       # ~1 h, GHC one module at a time, ~2 GiB
# 2. GHC's interfaces of every module, the judge's (P16: base's 128 missing; P17: each package), the iface2 verb live
for p in base ${P//,/ }; do bin/repr-haskell lib-check --live --record-pack --package "$p"; done   # after change (4); ~2 h in all
# 3. the oracle tier's verify of both packs, and the round trip's GuardGivens reference (review 1111b, #5)
bin/oracle-records verify haskell stg-libs-O0 lib-ifaces --sample 8
bin/repr-haskell run-check --record-ref front/GuardGivens
```

then, without GHC, per package in the order above (night commands, one process at a time, about 1.5-2 GiB, minutes each but
containers, unix and text, 10-20 minutes): `bin/repr-haskell lib-check --package P --ifaces own --record test/haskell/lib-check-own-P.edn`,
`lib-stg --package P --ifaces own --record test/haskell/lib-stg-own-P-results.edn`, each with its control (`lib-check-controls-P.tsv`,
`lib-stg-controls-P.txt`: a module against another's reference), then `bin/hsi check --libs own --record` (the whole corpus at 16 GiB,
the heavy lock): the newly eligible programs measured, the fallback list grown only by programs that fail on the own libraries.
Costs: the generalisation 1-2 days, the first slice 1 day, each later slice 1-2 days (the front end's classes it meets), one window.

### Checks (light, night 12's day lane: one run at a time, under 1.5 GiB)

| check | result |
|---|---|
| `bin/gate --full --plan` | exit 0, no table error; `haskell-lib-stg-own` between `haskell-lib-own-base` and `haskell-lib-stg`, decided "after" its two producers; `hsi-check`, `hsi-check-aot` "after haskell-lib-stg-own, haskell-lib-own-base" |
| `gate.crops check ROOT test/gate/steps.edn` | 0 problems |
| `bash test/gate/crops-test.bash` | 42 ok, exit 0 |
| `bash test/gate/gate-key-crops-test.bash` | PASS; `bin/arbace-go --gate-key` 9accb085d767... before and after |
| `bin/crops-score --self-test` | every test passes, every control fails (the new HF10-P29 case and control among them) |
| `bin/crops-score --judge --strict` | exit 0; Haskell 76.0 unchanged (eval: "check-hsi-own.edn is marked INTERIM, the stated score"; AOT: "no check-hsi-aot-own.edn yet") |
| the projection (`--crop haskell` on a copy without the mark, not committed) | eval 0.731, "420 of 908 cases own", independence 80.6 |
| `LIB_STG_REUSE=1 LIB_STG_OWN_POOL_TAR=... LIB_STG_OWN_OUT=... lib-stg --ifaces own` (hs-p22's own state copied into the worktree's `.tmp`, the pool tar made from it) | "the own closures in .tmp are the producer's pool's"; control 0 of 171; `ghc-internal-own.tar`: 265 interfaces, the wired-in Prim, 265 modules' STG (25.8 MB); 29 s, 0.25 GB |
| the same for `--package base` | `base-own.tar`: 241 interfaces, 241 modules' STG (`stg/base-4.22.0.0-3875/`); control 0 of 23; 14 s, 0.24 GB |
| `HSI_OWN_TARS=ghc-internal-own.tar:base-own.tar bin/hsi check --libs own --corpus samples --only samples/Hello,samples/Classes,samples/Arrays` (ARBACE_MEM_GIB=1, one front-end shard at 1.2 GiB; hs-p22's hsi executable, its `simd.ae` one change behind) | 507 own interfaces, 506 modules' STG from the tars; Hello `:libs :own` pass, Arrays `:libs :pack` pass, Classes `:libs :own` "runtime: out of memory" at the 1 GiB cap (passes at 2 GiB in the light runs): exit 1 for it alone; 1m19, 0.7 GB |
| hsi-essential's command (`own-verdicts --check`, then the 123 programs; ARBACE_MEM_GIB=1, one at a time, hs-p22b's hsi executable, the same sources) | own-verdicts current; 120 pass, 2 fail (synthetic/primops-word, runtime-laziness: "runtime: out of memory" at the 1 GiB cap, the pack path this branch does not change; they pass in the record at 16 GiB), the four controls that ran fail as they must; 24m52, 0.87 GB |
| `bin/reap`; `bin/crops-score --record`, `--judge --strict` | 0 problems; scores unchanged (Haskell 76.0), exit 0 |

### Night commands (not run; one at a time, in the branch's worktree, `export ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace TMPDIR=$PWD/.tmp/tmp`)

The steps' commands by hand, their outputs where the gate would keep them (`O=$PWD/.tmp/p29/outs`); once the branch is in `next`, the
night's `bin/gate --full` runs the same as steps (about 2.5 hours for the five).

1. `mkdir -p $O/haskell-lib-check-own && env LIB_CHECK_POOL_OUT=$O/haskell-lib-check-own bin/repr-haskell lib-check --ifaces own --judge test/haskell/lib-check-own-ghc-internal.edn`
   (13-21 min, 1.0-1.8 GB): ghc-internal's pool with this elaborator (review 1111b's multi-guard change judged too).
2. `mkdir -p $O/haskell-lib-own-base && env LIB_CHECK_DEP_POOL_TAR=$O/haskell-lib-check-own/ghc-internal-pool.tar LIB_CHECK_POOL_OUT=$O/haskell-lib-own-base bin/repr-haskell lib-check --package base --ifaces own --judge test/haskell/lib-check-own-base.edn && env LIB_STG_OWN_OUT=$O/haskell-lib-own-base bin/repr-haskell lib-stg --package base --ifaces own --judge test/haskell/lib-stg-own-base-results.edn`
   (about 10 min, 1.5 GB): `base-pool.tar`, `base-own.tar`.
3. `mkdir -p $O/haskell-lib-stg-own && env LIB_STG_OWN_POOL_TAR=$O/haskell-lib-check-own/ghc-internal-pool.tar LIB_STG_BASE_POOL_TAR=$O/haskell-lib-own-base/base-pool.tar LIB_STG_OWN_OUT=$O/haskell-lib-stg-own LIB_STG_REPLACE=all bin/repr-haskell lib-stg --ifaces own --judge test/haskell/lib-stg-own-results.edn --run`
   (about 35 min: the front end 11-21 min at 1.9 GB, run-check twice with hsi at 2 GiB): the module rows and `{:runs :all}` judged,
   `ghc-internal-own.tar`. If a row moved with the elaborator: the same with `--record` for that file, reviewed before committing.
4. `bin/heavy-lock env ARBACE_MEM_GIB=16 HSI_OWN_TARS=$O/haskell-lib-stg-own/ghc-internal-own.tar:$O/haskell-lib-own-base/base-own.tar bin/hsi check --libs own --corpus nofib,samples,synthetic,testsuite-core --jobs 4 --record`
   (about 10 GiB, 60-75 min; the front end 4 shards of 1.5 GiB first): `check-hsi-own.edn` re-recorded whole (nofib with it; the
   INTERIM mark dropped), so crops-score counts it; `:untried-cap` settled; then the same with `HSI_OWN_FALLBACK=try` and no
   `--record` (about 45 min): a listed program that passes leaves `own-fallback.txt`.
5. `bin/heavy-lock env ARBACE_MEM_GIB=16 HSI_OWN_TARS=... bin/hsi check --aot --libs own --corpus nofib,samples,synthetic,testsuite-core --jobs 2 --record`
   (8 GiB, about 2 hours: the own libraries' closures compiled once per image key, then each program's build): `check-hsi-aot-own.edn`,
   the own AOT cases.
6. `bin/crops-score --record && bin/crops-score --judge --strict && bin/reap`: Haskell's independence moves (projected 80.6 after 4,
   about 85 after 5); the scores' commit names the records' commit.

### Proposal

- **HF10-P31 The eval and AOT cells' source.** `bin/reap`'s Haskell eval and AOT cells read `check-hsi.edn` and `check-hsi-aot.edn`,
  the pack's records (`--libs pack`, by hand); since P29 the gate runs `--libs own`, so the cells' records are no longer what the
  gate's step runs, and crops-score counts the own cases from a second record. (a) **after the night's whole own records, the cells
  read `check-hsi-own.edn` and `-aot-own.edn` (every program of the corpora, `:libs own|pack` a line; the same classes with
  `divergences-own.edn` beside), `check-hsi.edn` kept as the own judge's pack reference (recorded with the pack by hand, as the oracle
  tier's comparison, P27); crops-score's `:case-inputs` then the cell's own source**; half a day, a reap record; keys unmoved (crop
  paths). (b) keep two records, the cell on the pack's (the gate's step and the cell disagree on 420 programs' library set). *Recommended:
  (a)*.

## Decided (the user, 2026-11-11: HF10-P31)

HF10-P31 (a): the cells read the own records once they are whole; `check-hsi.edn` stays the pack's reference, recorded by hand.

## As built: HF10-P31 (2026-11-11, branch `hs-p31`, from `mac/hs-p29` d90827eb0)

- `test/reap/cells.edn`: the Haskell eval and AOT cells' source (`:reader :hsi-check`) gets `:own-file {:file :name :classes}`: eval
  `check-hsi-own.edn`, classes `divergences.edn` and `divergences-own.edn` (an own-only failure is judged there, hs-p22b); AOT
  `check-hsi-aot-own.edn`, classes `divergences-aot.edn` and `divergences-own.edn`. The source's `:file` stays the pack's record.
- `test/reap/main.ae`: `judge-source` takes the source through `hsi-pick`: the own record is read, under its own name and classes,
  only when `hsi-own-state` of its first line is `:whole`; absent (the AOT record today) or first line `;; INTERIM` (the eval record
  today): the source is the pack's, unchanged (no score jump from a mixed record). Same mark crops-score's `interim?` reads (P29).
  `bin/reap --self-test`: the guard's five cases (the states, interim and absent keep the pack's, whole reads the own, a source
  without `:own-file` untouched); it is the reap step's first command now (`bin/reap --self-test && bin/reap`), and the step's inputs
  gain `test/haskell/*.edn` (the Haskell records were in no input of it).
- What switches when: the commit that re-records `check-hsi-own.edn` whole (night command 4, the INTERIM mark dropped) moves the eval
  cell to the own record at once; likewise `check-hsi-aot-own.edn` (command 5) for the AOT cell. Each such commit also needs `bin/reap
  --record` (the cell's source name and counts change: the matrix record differs until then, by design) and `bin/crops-score --record`.
  A whole own record with a program in no class fails `bin/reap` (tried here on the interim record with its mark changed: 764 programs,
  6 in no class, 11 problems), so the night's classes are written before the record is committed. The AOT own record is judged by
  `divergences-own.edn`'s eval outcomes beside `divergences-aot.edn`: an AOT-only failure needs a class of `divergences-aot.edn`.
- Not changed: crops-score's `:case-inputs` (it reads `check-hsi-own.edn` already, P29).
- Checks (one run at a time, under 1.5 GiB): `bin/reap --self-test` ok; `bin/reap` 0 problems, Haskell eval 811 / 63 / 34 and AOT
  137 / 1 / 1 as before (the pack's); `bin/crops-score --judge --strict` 0; `gate-key-crops-test` PASS; `bin/gate --plan` reads the
  table. No key moves (test/ and bin/reap are in no key).

## As built: HF10-P31b (2026-11-12, branch `hs-p31b`, from `mac/hs-p31` cf62b27b4)

The six programs HF10-P31 (a)'s trial of the switch left in no class (samples/Loop, synthetic/runtime-deep, testsuite/codeGen/should_run/
T16846, testsuite/concurrent/should_run/T21969 and readMVar1, testsuite/ffi/should_run/fptr02), run light (`bin/hsi check --libs own
--only ID`, ARBACE_MEM_GIB=2, GOMEMLIMIT 1200MiB, one at a time, peak 0.71-0.78 GB; hs-p22's own pools). What fails, for each: Go's
"runtime: out of memory" in 2 to 9 s (0.82-0.86 GB in use at the `ulimit -v` cap of the light runs), stdout empty, status 2; and none
of the six runs on the own libraries: Loop, runtime-deep, readMVar1 and fptr02 are eligible but in own-fallback.txt (`:untried-cap`,
:front :fallback), T16846 and T21969 are `:ineligible` (:libs :pack). So the failure is the pack's run at the light cap, not the
own libraries'; each passes at 16 GiB in check-hsi.edn.

- Two classes in `test/haskell/divergences-own.edn`, both `:deferred` (the night's whole record at 16 GiB settles them; then they go):
  `:light-cap` (Loop, runtime-deep: `:result :fail :status 2 :differs [:status :stdout :stderr]`) and `:light-cap-stdout-ok` (the
  other four: `:differs [:status :stderr]`). No `:message`: a `:libs :pack` line records no `:err`.
- `bin/lib/hsi/check`'s own judge took only `:libs :own` lines; it takes a `:libs :pack` line too (a line equal to the pack's record in
  outcome falls through to the pack's classes as before; one that differs, as these do against a passing pack line, needs an own
  class). Run: Loop "in own class :light-cap", fptr02 "in own class :light-cap-stdout-ok", 0 in no class.
- The switch tried on a copy of the interim record (first line's mark removed, not committed, restored): `bin/reap` finds no program in
  no class; its 5 problems are matrix.edn's counts (the cell's numbers move with the own record: Haskell eval deferred 35 against 34
  recorded and the rest), which the switch's `bin/reap --record` rewrites. On the branch (record as interim): `bin/reap` 0 problems,
  `bin/crops-score --judge --strict` exit 0.
- Moves no key of the seed or runtime (`bin/lib/hsi` and test/haskell are crop paths); bin/gate-key not run.

## As built: P17 (first slice) (2026-11-12, branch `hs-p17`, from `mac/hs-p31b` 3727f3ce4)

HF10-P30 (a)'s generalisation that needs no GHC, and its first slice deepseq, array, stm, by an agent in night 12 (one run at a time,
each process under 1.5 GiB with GOMEMLIMIT 1200MiB, MemAvailable at least 12 GiB before each run; stage 0; no GHC, no gate run but
`--plan`, no network). The plan's six points, as built:

1. **Dependency pools as a list.** `lib-check --package P --ifaces own` (any installation unit above ghc-internal) takes P's
   dependencies, transitively and in dependency order, from the pinned tarball's `.cabal` files: `arbace.repr.haskell.target`'s new
   `package-deps` (verb `deps PKG --src TREE`: NAME, CABAL, the number of exposed modules; each visited unit's `.cabal` finalized as
   the installation's records are, its library `build-depends`; rts, whose `.cabal` the corpus's tree lacks, has no modules). Each
   dependency that exposes modules needs its own pool: `LIB_CHECK_DEP_POOL_TAR=TAR:TAR...` (each `DEP-pool.tar`, a producer's
   `LIB_CHECK_POOL_OUT`; the name gives the package), else `LIB_CHECK_DEP_POOLS` as given, else `.tmp`'s `lib-check/own/DEP/pool`; a
   missing one is an error naming the command. base's run is the same as before (its dependencies rts, ghc-internal, ghc-prim; the one
   pool ghc-internal's).
2. **Every dependency's reexports.** The `.cabal` of every dependency gives its `reexported-modules` (base: ghc-internal's none and
   ghc-prim's 13, as before; later template-haskell's). lib-check's own run also writes them as `OUT/reexports.edn`, and lib-stg's
   per-module run reads that file beside the plan's own closures (`stg-one`): deepseq's `import GHC.Tuple (Solo (..))`, ghc-prim's
   reexport, failed in lib-stg alone without it ("no constructor type for nil").
3. **Prelude.** A package above base imports Prelude implicitly (the native dump does not name it; the renamer adds the import):
   the driver's closure seeds Prelude when a dependency's pool holds it (not for base, whose pools lack it).
4. **The package's source directory.** The dumps' tree is the package's (`src/` for ghc-internal, base, containers; the package's
   root for deepseq, array, stm): lib-check finds the one lowercase top directory, else the root.
5. **Eligibility from the own pools.** `bin/hsi check --libs own` already decided eligibility by the own interfaces present
   (`front_eligible` over WORK/own/if, the copies of every own pool, and the pack's reexports whose target is own); the tars
   (`HSI_OWN_TARS`) may name any number of packages; `HSI_OWN_PACKAGES=deepseq,array,stm` adds the `.tmp` pools and STG of the
   packages above base to the defaults.
6. **One step a package** (`test/gate/steps.edn`, after `haskell-lib-own-base`): `haskell-lib-own-deepseq`, `-array`, `-stm`, each
   lib-check `--judge test/haskell/lib-check-own-P.edn` with its control `lib-check-controls-P.tsv`, then lib-stg as a measure (no
   `--judge`: no -O0 reference yet), outputs `P-pool.tar` and `P-own.tar`, `:uses` the pools below (stm: array's). Each has the one
   part `own` with `:clean-only true`: it runs only in the clean run (`GATE_ALL_PARTS=1`) until the oracle window records P's
   references. The window's commands are in the steps' comment, as the plan wrote them; after it, the part's `:clean-only` goes,
   lib-stg gains `--judge test/haskell/lib-stg-own-P-results.edn` (recorded with `--record`), and `hsi-check`, `hsi-check-aot` grow by
   `P-own.tar` (`:uses`, `HSI_OWN_TARS`). The lib-stg controls `lib-stg-controls-P.txt` are written; they run once a reference exists.

**One elaborator class** met on the way: DefaultSignatures (deepseq's `default rnf :: (Generic a, GNFData Zero (Rep a)) => a -> ()`).
`class-decl!` keeps the default signatures (`:dsigs`), `default-methods` checks `$dm` at its default signature (its context the
method's own givens: `$dmrnf :: forall a. NFData a => (Generic a, GNFData Zero (Rep a)) => a -> ()`), and an instance that omits the
method applies `$dm` to the context's dictionaries solved at its head (GHC.Tc.TyCl.Instance's GenericDM). An importing module's
instance cannot see the default signature (the interface's class carries only `:defaults` names): a program's `instance NFData T`
without `rnf` over the own deepseq is not handled yet (P17's next item).

**The first slice** (each `lib-check --package P --ifaces own` on the tars of hs-p29's state: ghc-internal's pool, base's pool, and
for stm array's; judged against GHC's interfaces of the modules the pack haskell-ifaces-ghc-9.14.1 holds):

| package | modules | elaborate | types equal (of modules GHC's pack holds) | interface facts equal | lib-stg | peak, time |
|---|---:|---:|---|---|---|---|
| deepseq | 1 | 1 | 11 of 11 binders (1 of 1) | 202 of 211 (8 GHC's only, 12 ours only) | 1 of 1 :ok, no reference | 0.63 GB, 2m13 + 0m28 |
| array | 15 | 15 | 107 of 107 binders (9 of 9) | 388 of 388 | 15 of 15 :ok, no reference | 0.62 GB, 2m00 + 0m47 |
| stm | 10 | 10 | 56 of 56 binders (8 of 8) | 222 of 222 (11 ours only) | 10 of 10 :ok, no reference | 0.59 GB, 1m38 + 0m37 |

deepseq's interface differences are two classes, recorded, not fixed: its data family `RnfArgs` (GHC: an open family whose instances
are eta-reduced, `R:RnfArgsZeroa`; ours: arity 2 with the variable, `R:RnfArgsZero a0`), and five instances over poly-kinded heads
(`NFData1 (Sum f g)`, `NFData (Product f g a)`, `Fixed`, `Sum`, `Compose`: GHC's heads carry the kind arguments, ours not). Each
control differs as it must (deepseq's subject against another package's module, Data.Array: deepseq has one module). The records
`test/haskell/lib-check-own-{deepseq,array,stm}.edn` are whole runs; lib-stg is a measure (every module `:judge :no-reference`): the
slice's STG is not judged until the window.

**The programs** (`bin/hsi check --libs own`, HSI_OWN_TARS ghc-internal-own.tar and base-own.tar of hs-p29's state with the slice's
three `P-own.tar`; ARBACE_MEM_GIB 2, one program at a time; `HSI_FRONT_DRY=1` for the counts): eligible for the own front end **551
→ 582 of 908** (nofib, samples, synthetic, testsuite-core). The 31 new ones run: **24 pass** (23 on the own libraries, among them
samples/Arrays, Stm, StmBlock, Deadlock, synthetic/runtime-stm, nofib's kahan, paraffins, tchan, simple and 14 of testsuite's
arr001-arr019; throwto002 on the pack, the front end's `:typecheck-errors`), **6 fail on the own libraries** where the pack passes, in
no class yet: arr011, arr015 (an uncaught ErrorCall: array's bounds error at -O0 own), arr014, cgrun026 (hsi: "case of an algebraic
type on void"), T3207 (stdout differs), cgrun044 ("no binding of main::Main/main"). They do not touch the gate today: `hsi-check`
takes only ghc-internal-own.tar and base-own.tar, so these programs stay `:ineligible` there. When a package's part is gated (after
the window) they are its prerequisites: each fixed, or classed in `divergences-own.edn` and listed in `own-fallback.txt`.
samples/StmBlock needs 2 GiB (Go's out of memory at the 1 GiB cap, as hs-p31b's light-cap programs).

**No regression** (`--package base --ifaces own` on the same ghc-internal pool, with the generalised script, the reexports file and
the DefaultSignatures change): lib-check 241 modules, 236 elaborate, judge "0 worse, missing or unclassed, 1 better" against
`lib-check-own-base.edn`, control differs (7m45, 0.68 GB); lib-stg 547 of 1,029 bindings equal (recorded 547), "0 worse or missing, 2
better", control 0 of 23 (4m13, 0.67 GB). Not re-recorded: the records are the night's (P29's night commands re-record base with the
current elaborator).

### Checks (light, night 12: one run at a time, under 1.5 GiB)

| check | result |
|---|---|
| lib-check `--package deepseq/array/stm --ifaces own --judge` (records, controls) | exit 0 each; the table above |
| lib-stg `--package deepseq/array/stm --ifaces own` | exit 0; 1, 15, 10 modules `:ok`, `:judge :no-reference` |
| lib-check and lib-stg `--package base --ifaces own --judge` | exit 0; 0 worse (1 and 2 better) |
| `bin/hsi check --libs own --corpus samples,synthetic` on ghc-internal-own.tar and base-own.tar alone (the gate's set; the elaborator change) | exit 0; 45 own (44 pass), 18 pack (16 pass), 0 unclassed, the controls fail as they must: as `check-hsi-own.edn` records them; 2m48, 1.07 GB |
| `bin/hsi check --libs own` on the 31 newly eligible programs | 24 pass, 6 own-only failures (above), arr001's control fails as it must; 3m13, 0.78 GB |
| hsi-essential's command (`own-verdicts --check`, the 123 programs; ARBACE_MEM_GIB=1, HSI_JOBS 3 by default) | own-verdicts current; 120 pass, 2 fail (synthetic/primops-word, runtime-laziness: status 2 at the 1 GiB cap, the pack path, as on hs-p29; they pass at 16 GiB in the record), the four controls that ran fail as they must; 17m33, 0.88 GB |
| `bin/gate --full --plan` | exit 0, no table error; the three steps absent (`:clean-only`); with `GATE_ALL_PARTS=1` they plan "after" their producers (stm after `haskell-lib-own-array.own`) |
| `gate.crops check ROOT test/gate/steps.edn` | 0 problems |
| `bash test/gate/crops-test.bash` | ok (42 ok) |
| `bash test/gate/gate-key-crops-test.bash`; `bin/arbace-go --gate-key` | PASS; 314c8e5b04ab... on `mac/hs-p31b` and on the branch (no key moves: every path a crop path) |
| `bin/reap --self-test`, `bin/reap` | ok; 0 problems |
| `bin/crops-score --record`, `--judge --strict` | exit 0; scores unchanged (Haskell 76.0) |

### Night commands (not run; one at a time, in the branch's worktree, `export ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace TMPDIR=$PWD/.tmp/tmp`)

None is needed for the slice (its runs fit the day lane). Once the branch is in `next`: the clean run (`bin/gate --full --clean`, which
sets `GATE_ALL_PARTS=1`) runs the three steps, about 8 minutes in all under 1 GiB. With P29's night commands 1-2 (ghc-internal's and
base's pools with the current elaborator), the slice again on those pools, `O=$PWD/.tmp/p29/outs`:

```
for p in deepseq array stm; do
  d=$O/haskell-lib-own-$p; mkdir -p $d
  t=$O/haskell-lib-check-own/ghc-internal-pool.tar:$O/haskell-lib-own-base/base-pool.tar; [ $p = stm ] && t=$t:$O/haskell-lib-own-array/array-pool.tar
  env LIB_CHECK_DEP_POOL_TAR=$t LIB_CHECK_POOL_OUT=$d bin/repr-haskell lib-check --package $p --ifaces own --judge test/haskell/lib-check-own-$p.edn &&
  env LIB_STG_OWN_OUT=$d bin/repr-haskell lib-stg --package $p --ifaces own || break
done   # about 8 minutes, 0.7 GB
```

### Proposals

- **HF10-P32 The slice in hsi-check before the window.** The slice's lib-check is judged now (GHC's interfaces of 18 of its 26
  modules in the pack), only its STG waits for the window. (a) **drop the parts' `:clean-only` now (lib-check judged, lib-stg a
  measure), class or fix the 6 own-only failures above, list the unfixed in `own-fallback.txt`, then add `P-own.tar` to `hsi-check`'s
  and `hsi-check-aot`'s `:uses` and `HSI_OWN_TARS`: 31 programs more eligible, 24 own passes**; half a day, keys unmoved (crop paths),
  `--full` grows by about 8 minutes. (b) wait for the window, as the plan wrote (the own STG of the slice is then judged before the
  programs run on it). *Recommended: (a)*: the runs judge the slice's STG already (23 programs over it pass), and the window's date is
  open.
- **HF10-P33 DefaultSignatures across modules.** An instance in another module that omits a method with a default signature (a
  program's `instance NFData T` over Generic) needs the signature's context; GHC's interface carries it (IfaceClassOp's `GenericDM`),
  the helper's iface dump does not. (a) **the own interface writes the class's default signatures (`:dsigs`, the method's type in
  the interface's spelling), the elaborator reads them; the judge compares the class fact without them (GHC's dump lacks them)**;
  half a day. (b) the helper's iface verb dumps GenericDM too (the oracle window, a pack re-recorded). *Recommended: (a)*, then (b) at
  the window for the comparison.

## Decided (the user, 2026-11-12: HF10-P32, HF10-P33)

HF10-P32 (a): the deepseq/array/stm slice gated in `hsi-check` now: the three steps' `:clean-only` dropped, the six programs that
fail only on the own libraries (arr011, arr014, arr015, T3207, cgrun026, cgrun044) fixed if cheap, else classed in
`divergences-own.edn` by outcome with honest reasons, the `P-own.tar` outputs in `hsi-check`'s and `hsi-check-aot`'s `:uses`.
HF10-P33 (a): the classes' default signatures written into the own interfaces, so an instance in another module that omits a method
with a default signature works (a program's `instance NFData T` over Generic).

## As built: HF10-P32, P33 (2026-11-12, branch `hs-p32`, from `mac/hs-p17` fb31a9fe0)

By an agent during night 12's gate (one run at a time, each process under 1.5 GiB with GOMEMLIMIT 1200MiB, MemAvailable at least
12 GiB before each run; stage 0; no GHC, no gate run but `--plan`, no network). **All six programs are fixed; none is classed.** Each
was a front-end or eligibility fault that the slice's programs were the first to reach:

| program | what failed on the own libraries | cause | fix |
|---|---|---|---|
| arr011, arr015 | `(Array.!): undefined array element` after `//` | ghc-internal's `thawSTArray`: `let copy i# s3# = ... in case copy 0# s2# of s3# -> ...`, an unsigned local function whose result is a `State#`; the elaborator noted no type for a let-bound function (only pattern binders), so tostg took `copy 0# s2#` for a lifted value, the desugarer's bindNonRec made it a lazy `let`, unarisation dropped it: the copy loop never ran | `tc-group` notes every local binder's type (unsigned: the generalised type, the abstracted ones' monomorphic binders too; signed: the signature) in `*ltypes*`, tostg's representations read it |
| T3207 | prints `(())` for `()` | derived `Show`'s `wrapOpParens` took any non-letter first character as an operator: `deriving instance Show ()` in GHC.Internal.Show printed `"(" ++ "()" ++ ")"` | `deriv.ae`'s `sym?` is GHC.Utils.Lexeme's isSym: an ASCII symbol of `!#$%&*+./<=>?@\^|~-`, a colon, or a non-ASCII Unicode symbol (math, currency, modifier, other) |
| arr014, cgrun026 | `hsi: case of an algebraic type on void` | Data.Array.Base's `instance MArray (STArray s) e (ST s)` and `... (Lazy.ST s)` both got the dictionary function `$fMArraySTArrayeST`; the lazy one's binding replaced the strict one's, so strict `newArray` ran lazy ST's code | `inst-decl!` names dictionary functions as GHC.Tc.Utils.Monad's chooseUniqueOccTc: a name the module's instances already took gets `0`, `1`, ... appended (GHC's `$fMArraySTArrayeST0`). base had such collisions too: its lib-stg now has 1,046 bindings to compare, not 1,029 |
| cgrun044 | `no binding of main::Main/main` | `{-# OPTIONS -cpp #-}`: a CPP module the eligibility missed (it looked for `LANGUAGE CPP` and the flags), read as an empty module | `front_eligible` also refuses an `OPTIONS`/`OPTIONS_GHC` pragma with `-cpp` or `-XCPP`: cgrun044 runs on the pack (passes), and so do conc036 (leaves `own-fallback.txt`) and conc037 (fails as on the pack, `divergences.edn`'s class; `divergences-own.edn`'s `:front-no-main` goes, empty) |

Also: `bin/lib/hsi/check`'s default own STG path (`ls ... | head -1` under `pipefail`) failed the whole check when `.tmp` had no
lib-stg output of base even with `HSI_OWN_TARS` set: the `ls` may fail now.

**HF10-P33.** `ownface.ae` writes a class's `:dsigs` (`[[METHOD TYPE] ...]`, each default signature's type over the class's
variables in the interface's type forms) when it has any; `typecheck.ae`'s interface reader reads them into the class's `:dsigs` (the
module-level interface of `typecheck.ae`, a program's own modules', too). An instance that omits the method then applies `$dm` to the
dictionaries of the signature's context solved at its head, as in the defining module (P17's GenericDM); a signature with type
variables of its own (NFData1's `liftRnf :: forall a. (Generic1 f, GNFData One (Rep1 f)) => ...`) is taken when its context does not
mention them. The judge's class fact (`libcheck.ae`'s `cmp-map :classes`) selects `:ntvs :supers :methods :fundeps :defaults`, so
`:dsigs` is not compared (GHC's dump lacks it; HF10-P33 (b) at the window). Shown by a program (`.tmp/p32/t/G1.hs` in the worktree,
not committed): a hand-written `instance Generic T` (`type Rep T = D1 ... :+: ...`, `from`, `to`) and `instance NFData T` without
`rnf`: `evaluate (rnf (T 1 [True, error "forced by rnf"]))` raises the error on the new deepseq interface; on hs-p17's it stops
(`hsi: case of an algebraic type on <pap main:Main/$fNFDataT>`). `deriving Generic` itself is not built: the elaborator derives
no Generic (HF10-P34 below), so a program's `deriving (Generic, NFData)` still fails to typecheck (`Rep T` does not reduce).

**The libraries again** (`lib-check`, `lib-stg` as the gate's steps run them, each on the pools made before it; one at a time):

| step | result | before (records) | peak, time |
|---|---|---|---|
| lib-check `--ifaces own` (ghc-internal) | 0 worse, 1 better (GHC.Internal.Exception.Context: errors → ok) | | 0.85 GB, 9m55 |
| lib-stg `--ifaces own --judge` (ghc-internal) | 5,545 of 7,639 bindings equal, 0 worse, 6 better | 5,536 | 1.17 GB, 11m19 |
| lib-check `--package base` | 0 worse, 1 better (Data.Functor.Classes: errors → ok) | | 0.69 GB, 4m02 |
| lib-stg `--package base --judge` | 547 of 1,046 equal, 0 worse, 2 better (Data.Functor.Classes, Data.Foldable1: errors → ok) | 547 of 1,029 | 0.69 GB, 3m47 |
| lib-check `--package deepseq/array/stm` | as recorded: 202/211, 388/388, 222/222 facts; 0 worse each; controls differ | same | 0.56/0.61/0.53 GB |
| lib-stg `--package deepseq/array/stm` | 1, 15, 10 modules `:ok` (no reference) | same | 0.55-0.61 GB |

Not re-recorded: the records are the night's (P29's night commands), the betters stay betters until then.

**The programs** (`bin/hsi check --libs own`, ARBACE_MEM_GIB 2, one program at a time, on the five tars made above):

| run | before (hs-p17) | after |
|---|---|---|
| the 31 programs the slice made eligible | 24 pass, 6 own-only failures in no class | 30 pass (28 own, 2 pack: throwto002, cgrun044), kahan `:speed` as on the pack; 0 unclassed; 2m60, 0.77 GB |
| samples, synthetic | 45 own (44 pass), 18 pack (16 pass) | 50 own (49 pass), 13 pack (11 pass); 0 unclassed; the classes as before (AsyncRerun, Loop, runtime-deep); 2m53, 1.07 GB |

**The gate** (`test/gate/steps.edn`): `haskell-lib-own-{deepseq,array,stm}` lose their `:clean-only` part (stm's dependency path is
`haskell-lib-own-array/array-pool.tar` now, no `.own` part); `hsi-check` and `hsi-check-aot` take the slice's `deepseq-own.tar`,
`array-own.tar`, `stm-own.tar` in `:uses` and `HSI_OWN_TARS` after ghc-internal's and base's. `--full` grows by the three steps,
about 6 minutes in all under 0.7 GB (measured above). lib-stg of the slice stays a measure until the window records its -O0 STG.
Keys: every path a crop path; `bin/arbace-go --gate-key` 314c8e5b04ab... as on `mac/hs-p17`.

### Checks (light, night 12: one run at a time, under 1.5 GiB)

| check | result |
|---|---|
| the libraries' lib-check and lib-stg, as the gate runs them (the table above) | exit 0 each; 0 worse anywhere; controls differ as they must |
| `bin/hsi check --libs own --only` the 31 newly eligible programs | 30 pass, kahan `:speed`; 0 unclassed; arr001's control fails as it must |
| `bin/hsi check --libs own --corpus samples,synthetic` on the five tars | exit 0; 50 own (49 pass), 13 pack (11 pass), 0 unclassed, the controls fail as they must |
| hsi-essential's command (ARBACE_MEM_GIB=1) | own-verdicts current; 120 pass, 2 fail (primops-word, runtime-laziness: status 2 at the 1 GiB cap on the pack path, as on hs-p17 and hs-p29; they pass at 16 GiB); 18m05, 0.87 GB |
| P33's program (a hand-written Generic instance, NFData's default `rnf`) | the error inside the structure is forced by `rnf` (before: hsi stopped) |
| `bin/gate --full --plan` | exit 0, no table error; the three steps plan "after" their producers, `hsi-check` and `hsi-check-aot` after all five |
| `gate.crops check ROOT test/gate/steps.edn`; `bash test/gate/crops-test.bash` | 0 problems; ok |
| `bash test/gate/gate-key-crops-test.bash`; `bin/arbace-go --gate-key` | PASS; 314c8e5b04ab... (unmoved) |
| `bin/reap --self-test`, `bin/reap` | ok (5 cases); 49 of 52 cells checked, 0 problems |
| `bin/crops-score --record`, `--judge --strict` | exit 0; scores unchanged (Haskell 76.0) |

Not run: testsuite-core's whole own run (about 800 programs; the gate's `hsi-check`, `:heavy`, at night) and `hsi-check-aot`.

### Night commands (not run; in the branch's worktree, `export ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace TMPDIR=$PWD/.tmp/tmp`)

None is new. The gate's `hsi-check` and `hsi-check-aot` (heavy, 10 and 8 GiB, about 1.5 h together) are the whole proof over
testsuite-core and nofib once the branch is in `next`; P29's night commands record `check-hsi-own.edn` / `-aot-own.edn` whole (their
lines for cgrun044, conc036, conc037 move to `:front :ineligible :libs :pack`, the slice's programs to `:libs :own`) and re-record
the lib-check and lib-stg records with the betters above (`lib-stg-own-results.edn` 5,536 → 5,545; base's 1,029 → 1,046 bindings).

### Proposals

- **HF10-P34 DeriveGeneric.** `deriving (Generic)` (and `Generic1`) is not derived by the elaborator, so a program's
  `deriving (Generic, NFData)` or `instance NFData T` over a derived Generic does not typecheck (`Rep T` does not reduce): deepseq's
  Generic path works only with a hand-written instance (P33's proof). (a) **derive Generic as GHC.Tc.Deriv.Generics does: the
  `Rep` family instance (`D1`/`C1`/`S1` with `MetaData`, `MetaCons`, `MetaSel` and their promoted constructors and Symbols, `:+:`
  and `:*:` balanced as GHC splits them, `Rec0`, `U1`, `V1`), `from` and `to`; Generic1 after**; about a day, judged by the
  testsuite's `generics/` programs (eligible once derived) and the interfaces' `R:Rep` instances. (b) leave it to a later slice
  (containers and the run corpora need it little). *Recommended: (a)*, after the window: the generics/ tests are run programs.
