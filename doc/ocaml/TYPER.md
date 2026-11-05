# oi's native front end: OCaml's typer and Translcore ported

Status: the first slice (T1-T6) on the branch `ocaml-typer`, then T7 (Simplif) and T8 (the module system, the stdlib
typed natively unit by unit) on `ocaml-typer2`, 2026-11-05 (§9, "As built: T7 and T8"). From `ocaml-native`, whose
native reader gives the Parsetree as Arbace data. The route of OC4 (b) (doc/ocaml/OCAML-SURVEY.md §6.4): port OCaml 5.5.0's `typing/` and
`lambda/`'s Translcore with Matching, so that the native Parsetree becomes Lambda in Arbace and oi stops needing the
host's `ocamlc` for a program. Light rules (the day's): no gate, no Arbace build, no re-seed; every run on main's stage 0
(`ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`), one process under 1.5 GB, samples only. Nothing here is
TY1-TY6 (§8) were decided by the user (below §8); the proposals of T7-T8 are TY7-TY10 (§10).

## 1. What needs ocamlc today, and what the slice removes

oi (survey "As built: O5") interprets Lambda; four things come from the host's compiler through `tools/ocdump` on
compiler-libs:

| use | today | after this slice |
|---|---|---|
| a program's Lambda (`bin/oi run`, `check`) | `ocdump --mode lambda --simplify` | `bin/oi run-native`: Arbace (the core language and the module system; Simplif's output since T7) |
| the stdlib's Lambda (`bin/oi stdlib`) | ocamlc builds the `.cmi`, ocdump dumps each unit | still ocamlc's (TY5); `bin/oi stdlib-check` types 48 of the 71 units natively, equal (T8) |
| the toplevel's phrases (`bin/oi repl`, `bin/lib/oi/phrase`) | ocdump types and lowers each phrase | unchanged (T11) |
| the judge (types, Lambda, runs) | ocdump `--mode types`, `--mode lambda`, the toplevel | unchanged: the oracle stays the judge |

The stdlib's *interfaces* are read natively already: the typer reads `stdlib/*.mli` of the pinned tarball with the native
reader and translates their signatures itself (no `.cmi`).

## 2. The port, module by module

The port follows OCaml's own modules and data, not a re-design: the Typedtree it builds has OCaml's shape (the
expressions' descriptions, the patterns, the parameters with their `fp_param` and partiality, the applications'
argument lists in the order of the function's type with omitted arguments), so that Translcore and Matching can be
ported as they are and their output compared node for node. All of it is under `arbace/repr/ocaml/typer/`
(`arbace.repr.ocaml.typer.*`), evaluated from the checkout like the rest of repr/ocaml (OC30 (a)): out of `gate_key`
and of every embedded set by the existing exclusion of `arbace/repr/ocaml/`.

| Arbace namespace | OCaml 5.5.0 sources ported | the slice's part |
|---|---|---|
| `typer.types` | `typing/types.ml`, `btype.ml`, `ctype.ml`, `predef.ml`, `printtyp.ml` (with `out_type.ml`, `oprint.ml`), `typeopt.ml` | type nodes with levels (atoms: desc, level, id), identifiers with stamps (Predef's stamps in Predef's order: `Match_failure/22`, `Assert_failure/32`), unification with the occur check and level updates, abbreviation expansion, generalization, instances, the relaxed value restriction (`lower_contravariant`, variances: records with mutable fields and abstract types invariant), immediacy, `value_kind`, `array_type_kind`, `classify`; the printer: variables named in order of appearance, `'_weakN`, labels and optional arguments, the opened Stdlib left out, `Stdlib__List.t` as `List.t` |
| `typer.env` | `typing/env.ml`, `typetexp.ml`, `typedecl.ml`, `datarepr.ml`, `predef.ml`'s environment | environments as maps; Predef's types, constructors and exceptions; a unit's interface (`stdlib.mli`, `list.mli`, ...) read by the native reader on first use and translated: values with their block positions (vals, exceptions, non-alias modules, type extensions and classes take fields; externals not), externals with their primitive and arity, types, constructors (tags numbered among constant and block constructors), labels (`Record_regular`, `Record_float`), module aliases (`module List = List` in Stdlib is `Stdlib__List`, as `expand_module_aliases.awk` makes it); type declarations (variants, records, abbreviations, abstract) |
| `typer.parmatch` | `typing/patterns.ml`, `typing/parmatch.ml` | heads (`deconstruct`, arity, `to_omega_pattern`, `set_args`), `le_pat`, `lub`, `compat` (with `may_equal_constr`), `get_mins`, `complete_constrs`, bound identifiers, `alpha_pat`, and the exhaustiveness check (`satisfiable`, Maranget's usefulness) behind Typecore's partiality |
| `typer.typecore` | `typing/typecore.ml`, `typemod.ml`'s structure items | expressions and patterns of the core language (§3), `type_application` as `collect_apply_args` orders it (labels commuted, optional arguments wrapped in `Some`, eliminated as `None`, out-of-order omissions), `type_function` with `Pparam_val` parameters and defaults (`*opt*`), `name_pattern`, `type_let` with generalization, constructor and label disambiguation by the expected type, interval patterns as Typecore's right-nested or-patterns of chars, computation patterns (`exception`) |
| `typer.lambda` | `lambda/lambda.ml` (bind, `make_key`, `is_guarded`, `patch_guarded`, `next_raise_count`), `lambda/switch.ml` (stores, `opt_count`'s cost heuristics, clusters, jump tables, interval tests, `zyva`), `lambda/printlambda.ml`'s names | Lambda as vectors; the dump's text (tools/ocdump's `--mode lambda`, SPEC.md §11.2); the judge's canonical form |
| `typer.matching` | `lambda/matching.ml` | Half_simple and Simple, `explode_or_pat`, Context, Default_environment, Jumps, `split_or` with the or-matrix, `split_no_or`, `precompile_var`, `precompile_or`, the divisions by constants, constructors, tuples, records and variables, `mk_failaction_neg`/`_pos`, `combine_constant` (int and char switches through the Switcher, string switches, float and boxed-integer test sequences), `combine_constructor` (regular: option-like tests, switches, `isint`, shared actions, `reintroduce_fail`; extension constructors), `compile_orhandlers`, `comp_match_handlers`, `lower_bind`, `for_function`, `for_trywith`, `for_let` (`assign_pat`'s tuple optimization), `for_multiple_match` (flattened tuples), `for_optional_arg_default` |
| `typer.translcore` | `lambda/translcore.ml`, `translprim.ml`, `translmod.ml` (bytecode `transl_implementation`) | identifiers, constants, let and let rec, curried functions, applications (`build_apply`: right-to-left, closures for omitted arguments), primitives (the `%` table, `specialize_primitive` by type: fields' immediacy, `setfield_imm`, block shapes, comparisons on ints/floats/strings/bytes, array kinds; externals as C calls; `raise` of a handler's variable as `reraise`), match with exception cases (`static_catch`), try, tuples, constructors, records (with `init`), fields, arrays, if, sequences, loops, assert with `Assert_failure`, Match_failure's handler; the unit's block (its fields in the signature's order), exceptions as `makeblock 248`, the required globals |
| `typer.simplif` (T7) | `lambda/simplif.ml`, `lambda/lambda.ml`'s traversals (free_variables, subst, rename, shallow_map, shallow_iter, find_exact_application) | simplify_local_functions, simplify_exits, simplify_lets (local references as `mutlet` through eliminate_ref, aliases substituted, beta-reduction, arity fusion when both functions may fuse), the pipeline of simplify_lambda |
| `typer.tmc` (T8) | `lambda/tmc.ml`, `lambda.ml`'s duplicate_function | Constr, Dps, Choice, choice/traverse, make_dps_variant: `[@tail_mod_cons]` functions and their `_dps` variants (Stdlib's `@`, List's map, init...) |
| `typer.typemod` (T8) | `typing/typemod.ml`, `typing/includemod.ml`, `typing/mtype.ml`'s nondep_supertype | structures, signatures, module types (`with type`, `with module`, `:=`, `module type of`, include), module expressions (paths with aliases and strengthening, structures, functors, applications with applicative paths, constraints, unpacking), recursive modules, first-class modules (package types, packing), inclusion with its coercions (Includemod.signatures, functors, compunit), the stdlib's interfaces translated as signatures (lenient: what the slice does not type becomes opaque or a hole) |
| `typer.env` (T8 part) | `typing/env.ml`'s module components and addresses, `typing/subst.ml`, `typing/mtype.ml` (scrape, strengthen) | module entries {:path :mty :addr}, components prefixed by the module's path (types registered under it), aliases (absent, normalized; a submodule through an alias is an alias), required globals (an alias's unit when a C external is reached through it), substitutions of paths in types, declarations and module types, renaming of bound identifiers |
| `typer.translmod` (T8) | `lambda/translmod.ml` | apply_coercion, compose_coercions, compile_functor (merged curried functors, `{:may-fuse-arity true}`), transl_module, transl_structure, transl_struct_item (modules, include, open, exceptions with rebinding, recursive modules with init_shape and CamlinternalMod), transl_implementation coerced to an interface |
| `typer.format`, `typer.printtyp` (T8) | `stdlib/format.ml`'s engine, `typing/oprint.ml` | Format's queue and scan stack, boxes and breaks at margin 78; Oprint's types, declarations, constructors (inline records), values, externals, modules, module types and signatures as documents |
| `typer` (driver) | Arbace's own | `types`, `lambda [--simplify]`, `check` (§4), `stdlib-check` (§9) |

Size: the port is 4,000 lines of Arbace (the driver and bin/lib/oi/typer.bash included) for the parts of about 45,000
lines of OCaml (the 22 source files it draws on) that the slice needs.

**The raw and the simplified translation.** The output is Translcore's raw Lambda (what `ocamlc -drawlambda` prints,
ocdump's `--mode lambda`), then Simplif's (what `-dlambda` prints, `--simplify`), each judged against its own stage of
the compiler (TY1). `split_default_wrapper` is not part of bytecode's pipeline in 5.5.0; Tmc.rewrite is (`typer.tmc`).
oi runs the simplified Lambda.

## 3. The first slice

The core language: let and let rec, functions with labelled and optional parameters (defaults), application with
labels in any order and omitted or eliminated optional arguments, constructors (variants, options, lists, unit, bools),
records (creation, `with`, fields, mutable fields), tuples, match and function with Matching's compilation (constants,
chars with intervals, strings, or-patterns with variables, guards, nested patterns, record patterns, tuples of
scrutinees), exceptions (declarations, raise, try, `exception` cases in match), references and the stdlib's
primitives, arrays, `for` and `while`, `assert`; types inferred with levels and generalization under the relaxed value
restriction, type declarations (variants, records, abbreviations, polymorphic ones), the stdlib's values through
`Stdlib` and its units (`List.map`, `String.concat`, `Array.make`...). Printed types are Printtyp's, the signature
item by item (`val`, `type` with Format's breaking of long variants and records, `exception`).

Out of the slice, refused with a reason ("... outside the slice"), each a class of §5: modules (`module`, `open`,
`include`, functors, local opens, first-class modules), Printf's formats (typing string literals as `format6`, the
GADT `CamlinternalFormatBasics`), polymorphic variants, objects and classes, GADTs and locally abstract types, inline
records, lazy, array and effect patterns, effect handlers (Translcore's `transl_handler`), `let` operators, labelled
tuples' reordering, the elimination of optional arguments in an argument's type (`type_argument`'s eta-expansion),
type annotations with explicit polymorphism, error messages (a program the slice refuses is refused; one OCaml
rejects is not reported in OCaml's words).

## 4. How it is judged

`bin/oi typer-check [--record] [NAME...]` (and `make oi-typer-check`; `bin/lib/oi/typer.bash`): for each sample of
`test/ocaml/typer/`:

The oracle's dumps (types, raw and simplified Lambda) are committed in `test/ocaml/typer/oracle/` (TY2): with ocdump
present they are dumped afresh and must equal the committed ones (a stale dump fails the check), without it (or with
`OI_TYPER_NO_OCDUMP=1`) the committed ones judge alone and the runs on oi, which need ocamlc's stdlib, are skipped.

1. **Types**: the signature Arbace prints against ocdump `--mode types`'s (Printtyp's `signature` of the inferred
   interface, the toplevel's `val` lines), as text, equal.
2. **Lambda**: Arbace's raw Lambda against ocdump `--mode lambda`'s, and Simplif's against `--simplify`'s (`--prefix`
   so file names are the sample's), both read as forms and compared in a canonical form: every identifier renamed in order of appearance
   with its name kept (`x_276` and `x_1203` both `x#k`), and every static exit's number likewise. Stamps and exit
   numbers are counters of the whole compiler process (reading `.cmi` files makes identifiers), so equality up to them
   is the right equality; names, value kinds, primitives, constants, shapes and the structure are compared exactly.
3. **Runs**: the native simplified Lambda run on oi (`.tmp/oi/bin/oi run` with the stdlib's Lambda) against the sample's `.out`,
   the ocaml toplevel's run of the file (`--record` rewrites them).

`bin/oi run-native FILE.ml [ARG...]` runs a program through the native path alone: no ocamlc for the program.

**Results (2026-11-05)**:

| set | files | types equal | Lambda equal | oi's run equal |
|---|---:|---:|---:|---:|
| `test/ocaml/typer/` (36 samples: 30 written first, 6 more written after the port and passing on first run) | 36 | 36 | 36 | 36 |
| probe: `testsuite/tests/basic/`'s first 40 files, the 36 ocamlc accepts (not committed, not run on oi) | 36 | 15 | 15 | - |

The probe's 21 others: 20 refused by the slice (7 Printf formats, 5 modules, 3 `open`, 2 GADT constructors, 1 locally
abstract type, 1 local open, 1 optional-argument elimination) and 1 printer difference (`tailcalls`: Format's breaking
of a `val` line longer than the margin). Speed on stage 0: about 3 s to read the stdlib's interfaces that a program
uses, then about 0.1 s a sample; `bin/oi typer-check` takes 30 s (the oracle's dumps and the oi runs included).

## 5. Classes for what is not equal (for a corpus check, T11)

When the native path runs over the testsuite (`bin/oi check --native`, T11), every program not passing would be in one
of these, in `test/ocaml/divergences.edn`'s format:

- `typer-outside-slice`: refused with "outside the slice" (until its milestone lands; the reason names it);
- `typer-format-breaking`: the signature differs only in Format's line breaks (`val` lines over the margin);
- `typer-recursive-values`: `let rec` of values that are not all functions (Value_rec_compiler, Rec_check), refused;
- `typer-error-message`: a program OCaml rejects, refused with another message (until T12).

## 6. Milestones

| milestone | content | exit criteria | days |
|---|---|---|---:|
| T1-T6 (this) | types, env, parmatch, typecore, lambda (Switch), matching, translcore; driver and check | 36 of 36 samples: types, Lambda, oi runs | done |
| T7 Simplif | `lambda/simplif.ml`: `simplify_exits`, `simplify_lets` (`mutlet`), `simplify_local_functions`; the judge against `-dlambda` too | the samples equal to `ocdump --simplify` | done (§9) |
| T8 the module system | Typemod (structures, signatures, inclusion and coercions, strengthening, applicative paths), functors (`Map.Make`, `Set.Make`, `Hashtbl.Make`), `open`/`include`/local opens, recursive modules; Translmod complete; then the stdlib's own `.ml` typed and translated natively | `bin/oi stdlib --native`: every unit's Lambda equal to the oracle's (canonical), oi's check on it unchanged | the module system done, the stdlib 48 of 71 (§9); the rest wait on T9 and T10 |
| T9 formats, variants, objects | `format6` typing of literals (Typecore's `type_format`, CamlinternalFormatBasics), polymorphic variants (rows, `Tvariant`, Matching's `divide_variant`), objects and classes (Typeclass, Translobj, Translclass) | the testsuite's `basic/` and `typing-objects/` run programs through the native path | 6-10 |
| T10 the rest of the core | GADTs and locally abstract types, existentials, first-class modules, effect handlers (`transl_handler`), lazy and array patterns, inline records, labelled tuples, `let` operators, optional-argument elimination | the probe's refusals gone | 6-10 |
| T11 the corpus and the toplevel | `bin/oi check --native` over the run corpus with the classes of §5; `bin/oi repl --native` (phrases typed here, Genprintval's value printing from types) | none failing, every non-pass classed; repl-check without ocamlc | 4-6 |
| T12 errors | typing errors as ocamlc reports them (Location's report, Printtyp's explanations), the testsuite's expect tests' type errors | the error tests' messages equal | 5-8 |

Total beyond this slice: about 31 to 49 days, within the survey's 15-30 for a checker plus the stdlib and objects.

## 7. Notes on the port

- **Evaluation order.** OCaml evaluates function arguments right to left, so the compiler creates identifiers and exit
  numbers in an order Arbace's left-to-right evaluation does not reproduce; the canonical form makes that invisible,
  and nothing else in the output depends on it.
- **Labels' `lbl_all`.** OCaml's labels share one array of all the record's labels; here a record type's labels are in a
  registry (`types/label-all`) so that a label taken from `lbl_all` still knows its siblings, as Matching's record
  expansion needs.
- **`Patterns.omega`'s type.** A variable turned into an alias of omega binds with `Ctype.none`'s value kind (generic),
  which is why a guarded case's `let :alias "" n ...` prints no `[int]`; the port keeps omega untyped.
- **Format's margin.** Long variant and record declarations break as Printtyp breaks them (margin 78: a one-line form
  of 78 characters breaks, 76 does not); long `val` lines do not yet (`typer-format-breaking`).

## 8. Proposed (TY1-TY6; options with costs, a recommendation each, none decided)

- **TY1 The Lambda judged.** (a) the raw translation until T7, then both the raw and `-dlambda`'s (Simplif's port
  checked by itself); (b) `-dlambda` only, from T7 on (oi's stdlib is the simplified one); (c) raw only. *Recommended:
  (a)*: each port is checked against its own stage of the compiler.
- **TY2 The oracle's dumps for the check.** (a) record the samples' oracle dumps (types and raw Lambda, about 200 KB
  for 36 samples) under `test/ocaml/typer/oracle/`, with `--record` remaking them, so `typer-check` needs no ocamlc and
  can be a light gate step; (b) dump them at each run with ocdump, as built (needs compiler-libs on the host); (c) both,
  the recorded ones checked against a fresh dump when ocdump is present. *Recommended: (c)*.
- **TY3 A gate step.** (a) with OCaml's steps (OC120 (b)), a light essential step `bin/oi typer-check` on the recorded
  dumps (about 30 s, one process under 1.5 GB) once TY2 lands; (b) `--full` only; (c) none until T8. *Recommended:
  (a)*.
- **TY4 Where the typer lives.** (a) `arbace/repr/ocaml/typer/` as built (evaluated, out of `gate_key` and every
  embedded set by repr/ocaml's exclusion); (b) `arbace/ocaml/` beside `arbace/c/` and `arbace/scheme/`, with its own
  exclusion in `arbace/j2g/embed.ae` and `bin/lib/version.bash` (a key change, no re-seed); (c) embedded and compiled
  with the program at a re-seed (the speed: stage 0 evaluates it today). *Recommended: (a)* now, (b) when oi's REPL uses
  it (T11), (c) with OC30's revisit.
- **TY5 The stdlib's Lambda until T8.** (a) keep `bin/oi stdlib` (ocamlc, then ocdump) as it is; (b) record the stdlib's
  Lambda dumps as data in the repository (the 71 units, about 5.4 MB of forms; regenerated by `bin/oi stdlib --record`),
  so oi runs without ocamlc at all before T8; (c) start T8 with the stdlib's units in dependency order, each judged as
  the samples are. *Recommended: (c)*, with (a) meanwhile; (b) costs 5.4 MB of history for a stopgap.
- **TY6 The order after this slice.** (a) T7, T8, T11, then T9, T10, T12 (standalone first: the stdlib and the run
  corpus without ocamlc); (b) T7, T9, T10 first (the core complete before modules); (c) T8 first. *Recommended: (a)*.

## Decided (2026-11-05, the user)

TY1-TY6, each as recommended: the raw Lambda judged until T7, then both; the samples' oracle dumps committed and verified fresh when ocdump is present; bin/oi typer-check an essential step once they land; the typer in arbace/repr/ocaml/typer/, out of gate_key; T8 unit by unit, bin/oi stdlib meanwhile; the order T7, T8, T11, T9, T10, T12.

## 9. As built: T7 and T8 (2026-11-05, branch `ocaml-typer2`)

**T7, Simplif** (`typer.simplif`, TY1). simplify_local_functions, simplify_exits and simplify_lets ported as they are
(bytecode without `-g`: Simplif's `optimize` true), over lambda.ml's traversals; Tmc.rewrite is the pipeline's last step
(`typer.tmc`, T8). A function's attributes are the Lfunction's optional sixth element (`{:may-fuse-arity :local
:inline :tmc}`): Translcore's functions do not fuse their arity, Translmod's functors do. `bin/oi typer-check` judges
the raw Lambda against `-drawlambda`'s and Simplif's against `-dlambda`'s, and runs the simplified one on oi.

**TY2, the oracle's dumps committed** (`test/ocaml/typer/oracle/`, 48 samples' types, raw and simplified Lambda, 516
KB). With ocdump present they are dumped afresh and compared (a deliberately altered dump fails the check: tried);
`OI_TYPER_NO_OCDUMP=1` (or no compiler-libs) judges on the committed dumps alone, the oi runs skipped. TY3's gate step
is not added here (a light essential step `bin/oi typer-check` with `OI_TYPER_NO_OCDUMP=1`, about 25 s: for main's
gate table, after the merge).

**T8, the module system** (`typer.typemod`, `typer.translmod`, `typer.env`'s module part): §2's table. What it covers:
structures and signatures, `module type` and its paths, `with type` / `with module` / destructive `:=`, `module type of`,
`include` in structures and signatures, `open` and local opens (`let open`, `M.(e)`), `let module`, `let exception`,
functors (several parameters, merged into one Lfunction as Translmod does; generative `()` functors), applications with
applicative paths (`Set.Make(String).t`) and Mtype.nondep_supertype for anonymous arguments, strengthening, aliases
(absent, normalized, a submodule through an alias an alias with Tcoerce_alias), constraints with Includemod's
coercions (fields reordered, dropped, primitives eta-expanded, nested modules and functors), recursive modules
(CamlinternalMod's init_mod/update_mod with init_shape), first-class modules (package types with constraints, `(module
M : S)`, `(val e : S)`, `(module M : S)` patterns, an escaping local type expanded). The stdlib's interfaces are read
as signatures by the same Typemod (the unit as a module: `Set.Make (String)` works), leniently (a declaration or module
type outside the slice becomes abstract or a hole that keeps the fields' positions). Along the way: Format's engine and
Oprint's printers ported (`typer.format`, `typer.printtyp`: `module M :` with Format's breaking, long `val` lines too,
which fixed the probe's `tailcalls` signature), named type variables kept (`'acc`), `let x : t = e` and `fun x : t ->`
constraints, inline records (Datarepr's `t.C` record types, Record_inlined, Matching's inlined arguments), lazy values
and `%lazy_force` (Matching.inline_lazy_force), exception rebinding, `[@@immediate]`, `nonrec`, boxed-integer
comparisons, more of Translprim's table (loads, stores, byte swaps, conversions, atomics, raise_with_backtrace),
Typecore.build_as_type for aliases, constructors equal by tag, Env's required globals as Translprim and Env make them,
`-nolabels` and `-no-alias-deps` for the units that ocamlc compiles so.

**The stdlib typed natively** (TY5 (c); `bin/oi stdlib-check`, `make oi-stdlib-check`, the verb `stdlib-check` of the
driver). Each unit of bin/oi stdlib's build directory (the awk-expanded aliases, the per-unit flags of
stdlib/Compflags) is typed and translated by Arbace, coerced to its own interface (Includemod.compunit), and its
simplified Lambda compared with bin/oi stdlib's in the canonical form; the units that are equal replace the oracle's in
`.tmp/oi/lib-native`, and the typer's samples run again on oi with them. `bin/oi stdlib` stays the library oi runs on.

**Results (2026-11-05, stage 0 of main, one process under 1.5 GB)**:

| set | files | types equal | raw Lambda equal | simplified Lambda equal | oi's run equal |
|---|---:|---:|---:|---:|---:|
| `test/ocaml/typer/` (48 samples: 36 of the first slice, 3 for Simplif, 9 for the module system and the stdlib's needs) | 48 | 48 | 48 | 48 | 48 |
| the stdlib's units (`bin/oi stdlib-check`) | 71 | - | - | 48 | the 48 samples on the native units: 48 |
| probe: `testsuite/tests/basic/`'s first 40 files, the 36 ocamlc accepts | 36 | 18 | 17 | 17 | - |

The 12 new samples were written after the code they test and passed on their first or second run but two
(`t45_stdlib_features` found the missing inline-record flag on constructors; `t46_first_class_modules` the 5.5
Parsetree's package type inside `Ppat_unpack` and an escaping local type). The stdlib's units equal natively (48): Stdlib, CamlinternalLazy and Stdlib's Either, Obj, Lazy, Seq, Option, Pair,
Result, Bool, Char, Uchar, List, Int, Array, Bytes, String, Unit, Marshal, Float, Int32, Int64, Nativeint, Lexing,
Parsing, Repr, Set, Map, Stack, Queue, Buffer, Mutex, Condition, Semaphore, Fun, Digest, Hashtbl, Weak, Callback,
Pqueue, Ephemeron, Complex and the six `*Labels` units with StdLabels. The others, each with the slice's reason: Printf's formats (`format6` typing of literals,
T9: Arg, Gc, Printexc, Scanf, Filename, Printf; and the GADTs they rest on), GADTs (T10: CamlinternalFormatBasics,
CamlinternalFormat, Bigarray, Domain, Sys, Type, Random through Bigarray's kinds, In_channel and Out_channel through
Bigarray), objects (T9: Oo, CamlinternalOO, Dynarray's object type), type extensions and effects (T10: Effect, Format's
`type stag += ...`), `[%atomic.loc]` (Atomic), Iarray's `iarray` primitives, recursive values (CamlinternalMod's `let
rec l = lazy ...`, class `typer-recursive-values`). The probe's 19 others: 12 Printf formats, 2 GADT constructors, 1
locally abstract type, 1 class, 1 object, 1 optional-argument elimination, 1 recursive values (`tailcalls`: its
signature is now equal).

Speed: a sample about 0.1 to 0.5 s once the stdlib's interfaces are read (about 3 s); a stdlib unit 0.1 to 4 s;
`bin/oi typer-check` about 45 s with the fresh dumps and the oi runs, `bin/oi stdlib-check` about 4 minutes.

Size: 6,300 lines of Arbace under `arbace/repr/ocaml/typer/` (and the driver, 270) for what they port.

## 10. Proposed (TY7-TY10; options with costs, a recommendation each, none decided)

- **TY7 The stdlib's native units in oi's library.** (a) keep `bin/oi stdlib` (ocamlc) as oi's library, the native
  units only checked (as built); (b) a `bin/oi stdlib --native` that takes each equal native unit and the oracle's for
  the rest, so that a host without ocamlc runs the samples once the remaining units land (needs the oracle's dumps of
  those units as data, TY5 (b)'s cost for the remaining 22 units, about 2 MB); (c) wait until every unit is native
  (T9, T10). *Recommended: (c)*, with (a) meanwhile: (b) commits dumps that T9-T10 make useless.
- **TY8 The stdlib check in the gate.** (a) `bin/oi stdlib-check` in `--full` with OCaml's steps, judged against a
  recorded list of the units expected equal (a unit that regresses fails, a new one passes); (b) in the essential gate
  (4 minutes, ocamlc needed for the oracle's library); (c) none until T8 is complete. *Recommended: (a)*.
- **TY9 What comes next.** (a) T9's formats with T10's GADTs first (CamlinternalFormatBasics, then type_format: 12 of
  the probe's 19 and 9 stdlib units), then the decided T11; (b) T11 now (the run corpus through the native path, the
  classes of §5), formats after; (c) GADTs alone first (6 units). *Recommended: (a)*: the probe and the stdlib show
  formats as the one blocker that matters, and T11's corpus would class most of its programs `typer-outside-slice`
  for them.
- **TY10 Recursive values.** (a) port Rec_check's classification and Value_rec_compiler with T10; (b) now, as part of
  T8's stdlib (CamlinternalMod and the probe's `tailcalls`). *Recommended: (a)*.
