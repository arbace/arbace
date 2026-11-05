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
6 Routes and tiers · 7 Milestones HF1-HF12 · 8 Risks · Proposed (HFS1-HFS9) · Decided · As built: HF1-HF2 · As built: HF3 (start).

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
