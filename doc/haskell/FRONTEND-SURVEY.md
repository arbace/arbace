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
· ... · As built: HF9 (first slice) · As built: HF9 (second slice) · As built: HF10 (first slice) · As built: HF10 (second slice).

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
