# oi's native front end: OCaml's typer and Translcore ported

Status: the first slice (T1-T6) on the branch `ocaml-typer`, then T7 (Simplif) and T8 (the module system, the stdlib
typed natively unit by unit, with GADTs, formats and type extensions pulled forward from T9-T10 for it) on
`ocaml-typer2`, 2026-11-05 (§9, "As built: T7 and T8"), then T9's polymorphic variants on `ocaml-polyvar` (TY9 (b);
§11, "As built: T9's polymorphic variants"), then the rest of T9, objects and classes, on `ocaml-objects` (§13, "As
built: T9's objects and classes"), then T10's two stdlib blockers (TY15 (a)), GADT-aware exhaustiveness and recursive
values, on `ocaml-t10` (§15, "As built: T10's stdlib blockers"): the stdlib 71 of 71 natively; then TY18 (a), oi's library made
natively, on `ocaml-native-lib` (§17, "As built: TY18"). From `ocaml-native`, whose
native reader gives the Parsetree as Arbace data. The route of OC4 (b) (doc/ocaml/OCAML-SURVEY.md §6.4): port OCaml 5.5.0's `typing/` and
`lambda/`'s Translcore with Matching, so that the native Parsetree becomes Lambda in Arbace and oi stops needing the
host's `ocamlc` for a program. Light rules (the day's): no gate, no Arbace build, no re-seed; every run on main's stage 0
(`ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`), one process under 1.5 GB, samples only. Nothing here is
TY1-TY6 (§8) were decided by the user (below §8); the proposals of T7-T8 are TY7-TY10 (§10), decided; those of T9's
polymorphic variants TY11-TY12 (§12), decided; those of T9's objects and classes TY13-TY15 (§14), decided; those of
T10's stdlib blockers TY16-TY18 (§16).

## 1. What needs ocamlc today, and what the slice removes

oi (survey "As built: O5") interprets Lambda; four things come from the host's compiler through `tools/ocdump` on
compiler-libs:

| use | today | after this slice |
|---|---|---|
| a program's Lambda (`bin/oi run`, `check`) | `ocdump --mode lambda --simplify` | `bin/oi run-native`: Arbace (the core language and the module system; Simplif's output since T7); `bin/oi check --native` over the run corpus (T11, §18) |
| the stdlib's Lambda (`bin/oi stdlib`) | ocamlc builds the `.cmi`, ocdump dumps each unit | still ocamlc's (TY5, TY7 (c)); `bin/oi stdlib-check` types the 71 units natively, all equal (T8, T9, T10's blockers, §15) |
| the toplevel's phrases (`bin/oi repl`, `bin/lib/oi/phrase`) | ocdump types and lowers each phrase | `bin/oi repl --native` (`bin/lib/oi/phrase-native`): Arbace types and translates each phrase (T11, §18) |
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
- `typer-recursive-values`: `let rec` of values that are not all functions (Value_rec_compiler, Rec_check), refused
  (until §15; since then a program OCaml rejects for an invalid recursive definition is accepted, TY16);
- `typer-error-message`: a program OCaml rejects, refused with another message (until T12).

## 6. Milestones

| milestone | content | exit criteria | days |
|---|---|---|---:|
| T1-T6 (this) | types, env, parmatch, typecore, lambda (Switch), matching, translcore; driver and check | 36 of 36 samples: types, Lambda, oi runs | done |
| T7 Simplif | `lambda/simplif.ml`: `simplify_exits`, `simplify_lets` (`mutlet`), `simplify_local_functions`; the judge against `-dlambda` too | the samples equal to `ocdump --simplify` | done (§9) |
| T8 the module system | Typemod (structures, signatures, inclusion and coercions, strengthening, applicative paths), functors (`Map.Make`, `Set.Make`, `Hashtbl.Make`), `open`/`include`/local opens, recursive modules; Translmod complete; then the stdlib's own `.ml` typed and translated natively | `bin/oi stdlib --native`: every unit's Lambda equal to the oracle's (canonical), oi's check on it unchanged | the module system done, the stdlib 64 of 71 (§9); the rest wait on T9's objects and variants and T10 |
| T9 formats, variants, objects | `format6` typing of literals (Typecore's `type_format`, CamlinternalFormatBasics), polymorphic variants (rows, `Tvariant`, Matching's `divide_variant`), objects and classes (Typeclass, Translobj, Translclass) | the testsuite's `basic/` and `typing-objects/` run programs through the native path | formats done (§9), polymorphic variants done (§11); objects and classes done (§13) |
| T10 the rest of the core | GADTs and locally abstract types, existentials, first-class modules, effect handlers (`transl_handler`), lazy and array patterns, inline records, labelled tuples, `let` operators, optional-argument elimination | the probe's refusals gone | 6-10 |
| T11 the corpus and the toplevel | `bin/oi check --native` over the run corpus with the classes of §5; `bin/oi repl --native` (phrases typed here, Genprintval's value printing from types) | none failing, every non-pass classed; repl-check without ocamlc | done (§18): 244 of 309 natively, ocamlc's path 273, the 29 others classed; the session 24 of 24 |
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

**TY2, the oracle's dumps committed** (`test/ocaml/typer/oracle/`, 52 samples' types, raw and simplified Lambda, 590
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

**Pulled forward from T9 and T10, for the stdlib.** The stdlib's remaining units after the module system were blocked
by Printf's formats and the GADTs they rest on, so these parts of T9 and T10 were ported too (the rest of T9 and T10
stays to do): GADTs (constructors with their own result types, existentials made abstract in patterns, local equations
for locally abstract types in cases, parameters and `let K = e in`, `fun (type a)`, `let f : type a. ...`, explicit
polymorphism with polymorphic recursion), type extensions (`type t += ...`, Typedecl.transl_type_extension, Translmod's
extension constructors), Printf's formats (`typer.fmt`: CamlinternalFormat's parser and Typecore.type_format, a format
literal typed as CamlinternalFormatBasics' constructors), coercions `(e :> t)` without the subtyping check (the program
is one ocamlc accepts), optional arguments eliminated in arguments (Typecore.type_argument), lazy and array patterns
(Matching's divide_lazy, divide_array, combine_array), immutable arrays, the effect primitives, atomic record fields
with `[%atomic.loc]`, `[%extension_constructor]`, Bigarray's primitives specialized by kind and layout, `_ t` for a
constructor of any arity, type paths compared after normalization (through aliases). Samples t49-t52 cover them.

**Results (2026-11-05, stage 0 of main, one process under 1.5 GB)**:

| set | files | types equal | raw Lambda equal | simplified Lambda equal | oi's run equal |
|---|---:|---:|---:|---:|---:|
| `test/ocaml/typer/` (52 samples: 36 of the first slice, 3 for Simplif, 9 for the module system and the stdlib's needs, 4 for GADTs, extensible variants, formats, eliminated optional arguments) | 52 | 52 | 52 | 52 | 52 |
| the stdlib's units (`bin/oi stdlib-check`) | 71 | - | - | 64 | the samples on the native units: all equal |
| probe: `testsuite/tests/basic/`'s first 40 files, the 36 ocamlc accepts | 36 | 31 | 31 | 31 | - |

The 16 new samples were written after the code they test and passed on their first or second run but two
(`t45_stdlib_features` found the missing inline-record flag on constructors; `t46_first_class_modules` the 5.5
Parsetree's package type inside `Ppat_unpack` and an escaping local type). The stdlib's 7 units not equal, each with
its reason: CamlinternalFormatBasics (a polymorphic variant in one annotation, T9), Format (a polymorphic variant
pattern, T9), CamlinternalOO, Oo and Dynarray (objects, T9), CamlinternalFormat (types and Lambda but for one match:
GADT-aware exhaustiveness, Parmatch's check of counter-examples against the GADT equations, is not ported, so a match
OCaml finds total gets a Match_failure handler; T10), CamlinternalMod (`let rec l = lazy ...`, recursive values:
Value_rec_compiler and Rec_check, TY10). The probe's 5 others: a class, an object, polymorphic variants, two recursive
values (`recvalues`, `tailcalls`); and `patmatch`'s Lambda is equal but its signature prints a polymorphic record field
(`'a. 'a cell -> ...`, T10).

Speed: a sample about 0.1 to 0.5 s once the stdlib's interfaces are read (about 3 s); a stdlib unit 0.1 to 4 s
(CamlinternalFormat 13 s); `bin/oi typer-check` about a minute with the fresh dumps and the oi runs, `bin/oi
stdlib-check` about 4 minutes.

Size: about 7,200 lines of Arbace under `arbace/repr/ocaml/typer/` (and the driver, 270) for what they port.

## 10. Proposed (TY7-TY10; options with costs, a recommendation each, none decided)

- **TY7 The stdlib's native units in oi's library.** (a) keep `bin/oi stdlib` (ocamlc) as oi's library, the native
  units only checked (as built); (b) a `bin/oi stdlib --native` that takes each equal native unit and the oracle's for
  the rest, so that a host without ocamlc runs the samples once the remaining units land (needs the oracle's dumps of
  those units as data, TY5 (b)'s cost for the remaining 22 units, about 2 MB); (c) wait until every unit is native
  (T9, T10). *Recommended: (c)*, with (a) meanwhile: (b) commits dumps that T9-T10 make useless.
- **TY8 The stdlib check in the gate.** (a) `bin/oi stdlib-check` in `--full` with OCaml's steps, judged against a
  recorded list of the units expected equal (a unit that regresses fails, a new one passes); (b) in the essential gate
  (4 minutes, ocamlc needed for the oracle's library); (c) none until T8 is complete. *Recommended: (a)*.
- **TY9 What comes next.** Formats and GADTs were pulled forward today (§9) because the stdlib needed them. (a) the
  decided T11 next (the run corpus through the native path, the classes of §5, `bin/oi repl --native`), then what is
  left of T9 (polymorphic variants, objects and classes) and T10; (b) polymorphic variants first (2 stdlib units, a
  common feature of the opam corpus), then T11; (c) finish the stdlib first (variants, objects, GADT exhaustiveness,
  recursive values: the last 7 units). *Recommended: (b)*: T11's corpus would class many of its programs
  `typer-outside-slice` for polymorphic variants, which a day or two removes.
- **TY10 Recursive values.** (a) port Rec_check's classification and Value_rec_compiler with T10; (b) now, as part of
  T8's stdlib (CamlinternalMod and the probe's `tailcalls`). *Recommended: (a)*.

## Decided (2026-11-05, the user: TY7-TY10)

TY7 (c): ocamlc's stdlib Lambda stays oi's library until every unit is native; TY8 (a): bin/oi stdlib-check in --full against a recorded list of the units expected equal; TY9 (b): polymorphic variants next, then T11 (amending TY6's order); TY10 (a): recursive values with T10.

## 11. As built: T9's polymorphic variants (2026-11-05, branch `ocaml-polyvar`)

TY9 (b): polymorphic variants before T11. Ported from the same tarball (SHA-256 fcc6ae66...f015ba), into the existing
namespaces (§2's table), each port following OCaml's functions:

| namespace | OCaml 5.5.0 sources | what |
|---|---|---|
| `typer.types` | `types.ml` (row_desc, row_field with its `ext` cells, row_fields, row_repr, get_row_field, rf_either, link_row_field_ext), `btype.ml` (hash_variant, static_row, fixed_explanation, proxy), `ctype.ml` (merge_row_fields, filter_row_fields, unify_row, unify_row_field, copy_row and copy's sharing of a row by its more, occur_rec through rows and abbreviations, unify3's link of a row before its unification, memorized abbreviations, build_subtype and enlarge_type, subtype_rec and subtype_row (without objects, functors and universal variables), immediacy of rows, lower_contravariant's visited table), `out_type.ml` (nameable_row, printer_iter_type_expr, mark_loops with alias_nongen_row) | a row `[:Tvariant {:fields :more :closed :fixed :name}]`, a field `[:RFpresent t]` `[:RFabsent]` `[:RFeither {:no-arg :arg-type :matched :ext}]`; Btype's snapshots and backtrack (`*trail*`, `try-unify`) for unifications that may fail |
| `typer.env` | `typetexp.ml`'s Ptyp_variant (tags by hash, inheritance of static rows, `[< ... > ...]`) and Ptyp_alias, `btype.ml`'s set_static_row_name, `typedecl.ml`'s constraints (`as 'a`, `constraint`), checked at a constrained type's uses; a declaration whose manifest the lenient reading of an interface cannot translate (MoreLabels' `with type 'a t = 'a Map.Make(Ord).t`) made abstract rather than an opaque abbreviation that loses its parameters | |
| `typer.typecore` | `typecore.ml`'s Pexp_variant, solve_Ppat_variant, build_or_pat (`#t` patterns), finalize_variant, build_as_type of variants, the polymorphic variant processing of type_cases, type_function's parameters and type_let, type_coerce (`(e : t1 :> t2)` checked by Ctype.subtype; a ground coercion tried as one unification with the enlarged type, else made by subtype's constraints; else the argument unified with the enlarged type), the application operators `|>` and `@@` typed as applications (Pexp_apply's `%revapply`/`%apply`), a function's cases with a type constraint, `let p : 'a. t = e` | |
| `typer.parmatch` | `parmatch.ml`'s variant heads, full_match (closing or not), close_variant, pressure_variants; GADT constructors whose result type cannot be the matched one left out of a signature's completeness | |
| `typer.matching`, `typer.lambda` | `matching.ml`'s divide_variant, combine_variant (test sequences, switches on the hashes, isint splits), `switch.ml`'s test_sequence | |
| `typer.translcore` | `translcore.ml`'s Texp_variant (constant blocks), `translprim.ml`'s `%loc_LINE`, `%loc_FILE`, `%loc_LOC`, `%loc_POS`, the comparisons with a constant tag as `==` | |
| `typer.printtyp` | `out_type.ml` and `oprint.ml`'s Otyp_variant (`[ `A ]`, `[> ...]`, `[< ... > ...]`, a row printed by its name, `[< t ]`), aliases (`as 'a`) of open rows and recursive types, Ctype.normalize_type's sorted fields and conjunctions, Printtyp's hidden own name of a variant declaration, type declarations' constraints | |

**Results (2026-11-05, stage 0 of main, one process under 1.5 GB)**:

| set | files | types equal | raw Lambda equal | simplified Lambda equal | oi's run equal |
|---|---:|---:|---:|---:|---:|
| `test/ocaml/typer/` (60 samples; t53-t60 new: inferred rows, abbreviations and `#t`, coercions, Matching on variants, modules and recursive rows, recursive types through abbreviations and constraints, mixed forms) | 60 | 60 | 60 | 60 | 60 |
| the stdlib's units (`bin/oi stdlib-check`) | 71 | - | - | 66 | the samples on the native units: 60 of 60 |
| probe: `testsuite/tests/basic/`'s first 40 files, the 36 ocamlc accepts as programs | 36 | 32 | 32 | 32 | - |
| probe: opam's uutf (`src/uutf.ml`, one unit over the stdlib, a heavy user of polymorphic variants) | 1 | 0 | 1 | 1 | - |
| probe: other run tests with polymorphic variants (basic-more/robustmatch, typing-labels/mixin, typing-modules-bugs/pr5914_ok, pr6985_ok, pr7601_ok, pr7601a_ok, typing-polyvariants-bugs/pr4933_ok, pr7199_ok, privrowsabate_ok, translprim/ref_spec) | 10 | 5 | 6 | 6 | - |

The stdlib's new units: CamlinternalFormatBasics and Format (Format also needed `|>`'s typing). The basic/ probe's
polymorphic variant file, `switch_opts`, is equal (it also needed `%loc_LINE` and the GADT constructors' filter of its
last match). The stdlib's 5 units left: CamlinternalOO, Oo, Dynarray (objects, T9), CamlinternalFormat (GADT-aware
exhaustiveness over a tuple of GADTs: Typecore's check of Parmatch's counter-examples, T10), CamlinternalMod (recursive
values, TY10 (a): T10). The basic/ probe's 4 others: a class, an object, `recvalues` and `tailcalls` (recursive values);
`patmatch`'s and `sets`' signatures differ (a polymorphic record field, T10; a functor application's printed path,
`type t = Set.Make(SetOrd(BaseSet)).t`, T8's printer), their Lambda equal. The other probe's not equal: robustmatch
(GADT tuples' exhaustiveness, T10), mixin (MoreLabels' `Map.Make` result, `type +!'a t`, printed without its
variance, and printed types sharing differently: TY11), pr5914_ok and privrowsabate_ok (constraints recursive through a row, private rows in a
functor's parameter: refused), pr6985_ok, pr4933_ok (private abbreviations' and module types' printing).

Faithfulness notes: unification links a row to the other before unifying their fields (Ctype.unify3) but links no
other type (TY11); a row built by build_subtype or build_as_type carries no name, as OCaml's loses it when
update_level lowers it below its type's scope (scopes are not ported: TY12).

Size: about 8,100 lines of Arbace under `arbace/repr/ocaml/typer/` (and the driver, 177), about 940 more than T8 left (T9's variants and their fixes).

## 12. Proposed (TY11-TY12; options with costs, a recommendation each, none decided)

- **TY11 Sharing in unification.** OCaml's unify3 links every non-variable type to the other before unifying their
  parts; the port does it for rows only, so a printed type can show the other node of an equal pair: uutf's
  `val src : decoder -> Bytes.t -> ...` prints `bytes` here (its Lambda equal), mixin prints `string` where OCaml
  prints `Subst.key` and `('a expr as 'a) expr` for `('a expr as 'a)`. Tried on this branch and taken back: with
  unify2_expand and unify3 ported as they are (both heads expanded, the first linked to the second, no link in a GADT
  pattern nor under a GADT equation) uutf's signature became equal and the 60 samples stayed equal, but basic/'s
  `boxedints` printed `Int32.t` where OCaml prints `int32` (`let f (a : int32) = Int32.unsigned_compare a a`): OCaml's
  levels keep a constrained pattern's type apart from its uses (map_half_typed_cases' generalization of the patterns),
  which the port's levels do not reproduce. (a) port unify3's links with that part of Typecore's levels (about a day:
  type_cases' and type_function's levels, every sample and the stdlib re-judged); (b) with T11, its corpus's classes
  showing how often it matters; (c) not at all. *Recommended: (b)*.
- **TY12 Scopes.** OCaml drops a row's name when update_level lowers it below its type's scope, and checks escapes by
  scopes; the port approximates the first (rows built by enlarge_type and build_as_type unnamed) and has no escape
  check. (a) port Ident's scopes and update_scope with T11 if its corpus shows printed names differing; (b) now (about
  a day, every type node gaining a scope). *Recommended: (a)*.

## Decided (2026-11-05, the user: TY11, TY12)

TY11 (b): sharing in unification waits for T11's corpus; TY12 (a): scopes ported with T11 if the corpus shows differing names.

## 13. As built: T9's objects and classes (2026-11-05, branch `ocaml-objects`)

The rest of T9, from `ocaml-polyvar`. Ported from the same tarball (SHA-256 fcc6ae66...f015ba), each port following
OCaml's functions; two new namespaces, the rest into the existing ones:

| namespace | OCaml 5.5.0 sources | what |
|---|---|---|
| `typer.types` | `types.ml`'s Tobject, Tfield, Tnil, field kinds (shared cells, linked), Tpoly and Tunivar; `ctype.ml`'s flatten_fields, associate_fields, build_fields, object_row, opened_object, make_rowvar, unify_fields, unify_kind, the Tfield/Tnil cases of unify3, instance_poly, subtype_fields, build_subtype on objects (a class abbreviation's #-type, named when its methods are unchanged), unify2's expansion of an abbreviation that drops a parameter; `out_type.ml`'s object proxies and loops | object types and their unification; an object unified with an abbreviation linked to the abbreviation, as unify3 links |
| `typer.typeclass` (new) | `typing/typeclass.ml` (class_structure's two passes with make_method, type_object, class_expr, class_type, class_declarations with initial_env's temporary abbreviations, class_infos' object and #-type abbreviations and constructor type, class_type_declarations, class descriptions), `ctype.ml`'s class signatures (new_class_signature, filter_method_row, add_method, add_instance_variable, inherit_class_signature, update_class_signature, hide_private_methods, close_class_signature, add_dummy_method, instance_class, limited_generalize, set_object_name), `typecore.ml`'s type_send and filter_method, type_class_arg_pattern | classes, inheritance (`as super`), virtual and private methods and instance variables, initializers, class parameters, applications and lets, class constraints, recursive classes (`and`), class types and their abbreviations, polymorphic methods |
| `typer.typecore` | `typecore.ml`'s Pexp_send, Pexp_object, Pexp_new, instance variables and self (Val_ivar, Val_self through `self-N`), Pexp_setinstvar, Pexp_override, Pexp_poly (a method's polytype instantiated) | |
| `typer.env` | `typetexp.ml`'s Ptyp_object (fields folded, inherited object types), Ptyp_class (`#c`), transl_poly_type (`'a. t` as a polytype); classes and class types as module components; `[@@unboxed]` one-constructor variants (Dynarray's `Pack`) | |
| `typer.translclass` (new) | `lambda/translobj.ml` (share, oo_prim, transl_label_init, oo_wrap_gen and the classes' tables), `lambda/translclass.ml` (build_object_init, build_class_init, bind_methods, output_methods, builtin_meths, transl_class with its cached-table case under a function, transl_class_bindings with Value_rec_compiler's dummy blocks for static classes) | the object and class initialisers as calls of CamlinternalOO, bytecode's (no method caches) |
| `typer.translcore`, `typer.translmod`, `typer.simplif`, `typer.lambda`, `typer.tmc` | `translcore.ml`'s Texp_send, Texp_new, Texp_instvar, Texp_setinstvar, Texp_override, Texp_object, transl_exp1's oo_wrap and fuse_method_arity; `translprim.ml`'s %send, %sendself, %sendcache; `translmod.ml`'s Tstr_class and its order (a structure's let bindings and classes translated before the rest); `simplif.ml`'s Lifused; Lambda's Lsend | |
| `typer.printtyp` | `out_type.ml`'s tree_of_typobject, tree_of_class_type, tree_of_class_declaration and tree_of_cltype_declaration, `oprint.ml`'s Otyp_object, Otyp_class, Otyp_poly, Osig_class, Osig_class_type | `< m : t; .. >`, `#c`, `'a. t`, `class ['a] c : 'a -> object ('b) ... end`, `class type ... = ...`, `and` |

**Results (2026-11-05, stage 0 of main, one process under 1.5 GB)**:

| set | files | types equal | raw Lambda equal | simplified Lambda equal | oi's run equal |
|---|---:|---:|---:|---:|---:|
| `test/ocaml/typer/` (71 samples; t61-t71 new: immediate objects, objects under functions, classes, inheritance, class types, self types, polymorphic methods, class type abbreviations, classes that re-export a class, generalized objects, optional class parameters) | 71 | 71 | 71 | 71 | 71 |
| the stdlib's units (`bin/oi stdlib-check`) | 71 | - | - | 69 | the samples on the native units: 71 of 71 |
| probe: `testsuite/tests/basic/`'s first 40 files, the 36 ocamlc accepts | 36 | 34 | 34 | 34 | - |
| probe: run tests with objects and classes (typing-objects-bugs' `*_ok`, typing-poly-bugs' pr5322 pr5673 pr6922, letrec-compilation's class_1 class_2, translprim's sendcache and locs, tool-ocaml/t301-object, parsing/anonymous_class_parameter, typing-recmod's t13ok t16ok, backtrace's methods and names, typing-labels' mixin2 mixin3, typing-extensions/cast, typing-typeparam/newtype, typing-polyvariants-bugs/pr10664, generalized-open/shadowing, regression/pr7426, misc/nucleic, exotic-syntax/exotic, typing-modules-bugs/pr7036_ok) | 29 | 16 | 18 | 18 | - |

The stdlib's new units: CamlinternalOO and Oo (object types, `%sendself`), Dynarray (an immediate object under a
function, a class whose table is cached; also `[@@unboxed]` and unify2's expansion of the phantom abbreviation `'stamp
dummy = < >`). The 2 left: CamlinternalFormat (GADT-aware exhaustiveness, T10) and CamlinternalMod (recursive values,
TY10 (a): T10). The basic/ probe's objects and includestruct are equal now (includestruct also needed an include to
rename an exception's constructor); its 2 others are recursive values (recvalues, tailcalls), and patmatch's and sets'
signatures differ as before (§11). The objects probe's 11 whose Lambda is not equal: class_2 (recursive values, T10);
mixin2, mixin3 and exotic (a functor application's path, `MoreLabels.Map.Make (...)` bound by Translmod, T8's slice);
shadowing (`open struct ... end`, T8's slice); locs (`%loc_FUNCTION`, which needs the scoped locations Translcore
keeps: not ported); newtype (a toplevel test whose phrases OCaml rejects); pr5156_ok (a class's self constrained
through an abbreviation `'a obj = (< .. > as 'a)`); pr6922_ok (class parameters constrained through `#`-types and
rows); pr7036_ok (a polymorphic method defined by `fun (type a) ...` over a first-class module); t16ok (a class whose
abbreviation must match a recursive module's private object type: Includemod on object types). Of the 18 whose Lambda
is equal, 3 signatures differ: cast (a toplevel phrase OCaml rejects), pr5673_ok (polymorphic record fields, T10) and
yamagata021012_ok (an annotation's abbreviation `uchar` printed expanded, TY11, and a coercion's #-type).

Faithfulness notes. Method types are polytypes only when written `'a. t` (OCaml makes every method type `Tpoly (t, [])`
and its printer and generalization see through it): no accepted program's output was found to depend on it. Two
polytypes unify by pairing their universal variables as fresh variables (Ctype's univar pairs and its escape checks
are not ported). Includeclass (the matching of a class against a class type in a signature or a constraint) is not
ported: the class type given wins, as for a program OCaml accepts. Typeclass's type_approx (the first pass's
approximation of method bodies) is not ported: methods are typed in the second pass against the declared or fresh
types. A dummy method hidden by complete_class_signature is skipped when an object is closed and when a row meets a
closed one. The class tables' and builtin methods' Lambda is bytecode's with OCaml's `-g` off for builtin methods
(Translclass uses them unless `Clflags.debug`), as the oracle compiles.

Size: about 9,900 lines of Arbace under `arbace/repr/ocaml/typer/` (and the driver, 179), about 1,800 more than T9's
variants left (typeclass.ae 738, translclass.ae 495, the rest in the existing namespaces).

## 14. Proposed (TY13-TY15; options with costs, a recommendation each, none decided)

- **TY13 Includeclass.** The class matching of a signature's class specification or a class constraint is not
  checked (§13). (a) port `typing/includeclass.ml` and Ctype.match_class_types with T12, where its errors are reported;
  (b) now (about half a day: the class type comparison and its moregeneral); (c) never, the oracle stays the judge of
  rejected programs. *Recommended: (a)*: only rejected programs differ.
- **TY14 Every method type a polytype.** (a) represent a monomorphic method's type as `Tpoly (t, [])` as OCaml does,
  with T11 if its corpus shows a printed type or a generalization that differs; (b) now (about half a day: every
  method type's producer and consumer, the samples and the stdlib re-judged); (c) not at all. *Recommended: (a)*.
- **TY15 What comes next.** The stdlib is 69 of 71 natively; its last 2 units need T10's GADT-aware exhaustiveness
  (CamlinternalFormat) and recursive values (CamlinternalMod, TY10 (a)), which would make oi's library native (TY7
  (c)) and let a host without ocamlc run oi. (a) those two parts of T10 next (about a day), then T11 as decided;
  (b) T11 next as decided (TY6, TY9), T10 after it; (c) the class parameters' constraints and the objects probe's
  rest first. *Recommended: (a)*: it completes the stdlib, the condition TY7 (c) set for dropping ocamlc's library.

## Decided (2026-11-05, the user: TY13-TY15)

TY13 (a): Includeclass with T12; TY14 (a): Tpoly (t, []) for every method type with T11 if its corpus shows a difference; TY15 (a): T10's two stdlib blockers next, then T11 (amending TY9's order).

## 15. As built: T10's stdlib blockers (2026-11-05, branch `ocaml-t10`)

TY15 (a): the two parts of T10 the stdlib's last units needed, from `ocaml-objects`, ported from the same tarball
(SHA-256 fcc6ae66...f015ba):

| namespace | OCaml 5.5.0 sources | what |
|---|---|---|
| `typer.valuerec` (new) | `typing/value_rec_check.ml`'s classify_expression (and is_valid_recursive_expression's fast path for functions), `lambda/value_rec_compiler.ml` whole (compute_static_size with its delayed sizes of let-bound names, split_static_function, compile_alloc, compile_update with CamlinternalLazy.indirect, compile_letrec) | a recursive binding's kind (`:static`, `:dynamic`), computed by `type_let` for `let rec` (the Typedtree's vb_rec_kind); `transl_let`'s recursive case through compile_letrec: dynamic bindings let-bound first, static blocks pre-allocated by caml_alloc_dummy, caml_alloc_dummy_float or caml_alloc_dummy_lazy and backpatched after the functions by caml_update_dummy(_lazy), constants with the recursive names replaced by the dummy constant, non-syntactic functions lifted with their context's block (`letrec_function_context`) or eta-expanded |
| `typer.parmatch` | `parmatch.ml`'s exhaust (exhaust_single_row, specialize_and_exhaust), full_match false, build_other for constructors (pat_of_constrs with orify_many) | the counter-examples of a match, lazily, or-patterns at the choice points |
| `typer.typecore` | `typecore.ml`'s check_counter_example_pat and partial_pred, `parmatch.ml`'s do_check_partial | each counter-example typed against the scrutinee's type with the GADT equations its constructors add (the type checker's pattern mode), an or-pattern's alternatives tried in turn with the rest of the witness (find_valid_alternative), every change to the types (Btype's snapshot) and to the equations undone; a match is total when no counter-example types, so Translcore adds no Match_failure handler |

The exhaustiveness check runs as before (Maranget's usefulness with the constructors a GADT's type excludes left
out); only when it finds the match partial and a GADT constructor is among the patterns are the counter-examples
built and checked, as do_check_partial does for every match (the same answer: a counter-example without a GADT
constructor always types). Two parts of check_counter_example_pat are not ported: the Refine_or strategy (or-patterns
are always split, Backtrack_or, which explores more but answers the same for the stdlib and the samples) and the
explosion of wildcards (`explode` 5 for a one-case match: a wildcard of an empty type is not refuted here); TY17.
Value_rec_check's validity check (its modes and environments: Dereference, Guard, Delay, the rejection of `let rec x =
x + 1`) is not ported: the kind is computed for programs OCaml accepts, and one it rejects for an invalid recursive
definition is compiled here (TY16).

**Results (2026-11-05, stage 0 of main, one process under 1.5 GB)**:

| set | files | types equal | raw Lambda equal | simplified Lambda equal | oi's run equal |
|---|---:|---:|---:|---:|---:|
| `test/ocaml/typer/` (73 samples; t72: GADT matches total only by their indices, over tuples and an option; t73: recursive values of every kind) | 73 | 73 | 73 | 73 | 73 |
| the stdlib's units (`bin/oi stdlib-check`) | 71 | - | - | 71 | the samples on the native units: 71 of 71 |
| probe: `testsuite/tests/basic/`'s first 40 files, the 36 ocamlc accepts | 36 | 34 | 36 | 36 | - |
| probe: run tests with objects and classes (§13's 29) | 29 | 16 | 19 | 19 | - |

The stdlib's new units: CamlinternalFormat (`trans`'s match over a pair of `fmtty_rel`, whose impossible pairs are
refuted by their shared indices) and CamlinternalMod (`let rec l = lazy ...`, a lazy block pre-allocated and
backpatched). With them every unit of the stdlib is typed and translated natively, its simplified Lambda equal to
ocamlc's, and the samples run on oi on that library alone, as on ocamlc's. The basic/ probe's recvalues and tailcalls
are equal now (36 of 36 in Lambda; patmatch's and sets' signatures differ as before, §11), and the objects probe's
class_2 (19 of 29). A sample written with `Lazy.is_val l` inside `let rec l = lazy ...` printed `int lazy_t` where
OCaml prints `int Lazy.t`: unify3's link of a type to the abbreviation it meets, TY11 (b); t73 avoids it.

Speed: CamlinternalFormat 25 s natively (21 s before, its 30-odd GADT matches' counter-examples checked), `bin/oi
stdlib-check` 1 min 34 s, `bin/oi typer-check` 25 s. Size: valuerec.ae 289 lines, the counter-examples about 165 lines
in parmatch.ae and typecore.ae.

## 16. Proposed (TY16-TY18; options with costs, a recommendation each, none decided)

- **TY16 Value_rec_check's validity check.** A `let rec` OCaml rejects ("This kind of expression is not allowed as
  right-hand side of let rec") is compiled here. (a) port its modes and environments (about 800 lines of
  value_rec_check.ml: expression, value_bindings, modexp, class_expr) with T12, where rejected programs are judged;
  (b) now (about half a day); (c) never, the oracle judging rejected programs. *Recommended: (a)*.
- **TY17 check_counter_example_pat's strategies.** (a) port Refine_or and the explosion of wildcards (Parmatch's
  pats_of_type) with T11 if its corpus shows a match whose partiality differs (a one-case match total by an empty
  type, or a search too slow); (b) now (about half a day), also running the counter-examples' check for every partial
  match as OCaml does; (c) not at all. *Recommended: (a)*.
- **TY18 oi's library native.** TY7 (c)'s condition holds: every stdlib unit is native and equal. (a) make the native
  units oi's library (`bin/oi stdlib` typing and translating the tarball's stdlib in its link order with Arbace, ocamlc
  and ocdump kept as the oracle and for `stdlib-check`), so that `bin/oi run-native` and the typer's samples need no
  ocamlc (about half a day: the link order from stdlib/Makefile without ocamlc, the testsuite's lib/testing too, the
  check that the two libraries stay equal); (b) with T11, whose corpus runs would use it; (c) keep ocamlc's library.
  *Recommended: (a)*, before T11: it is what T8-T10's stdlib work was for.

## Decided (2026-11-05, the user: TY16-TY18)

TY16 (a): the validity check with T12; TY17 (a): Refine_or and wildcard expansion with T11 if its corpus needs them; TY18 (a): the native units become oi's library now, before T11 (ocamlc stays the oracle). Built: §17.

## 17. As built: TY18, oi's library native (2026-11-05, branch `ocaml-native-lib`, from `ocaml-t10`)

`bin/oi stdlib` now types and translates the tarball's stdlib and the testsuite's lib/testing with Arbace (the verb
`stdlib-build` of `arbace.repr.ocaml.typer`, on stage 0), with no ocamlc and no ocdump; the oracle's path stays behind a flag.

- **The sources** are prepared as stdlib/Makefile does, by `stdlib_prepare` (sys.ml from sys.ml.in, `expand_module_aliases.awk`
  over stdlib.ml, stdlib.mli and the *Labels.mli: sed and awk, shared with the oracle's path), into `.tmp/oi/stdlib-native-build`,
  with testing.ml and testing.mli beside them (`Testing` is found as the stdlib's interfaces are). The set of units is
  stdlib/StdlibModules' STDLIB_MODULE_BASENAMES (71); its order is not used.
- **The link order is computed.** Typing reads interfaces by name, so every unit is typed in any order; the order comes from each
  Lambda's required globals (the header of its program, the modules whose values the unit uses: what ocamlc links before it), plus
  Stdlib for every unit but Stdlib and CamlinternalFormatBasics (the opened default), by Kahn's algorithm, the smallest ready unit
  first (`link-order`, no cycle). A first try scanned the sources for module names as ocamldep does and was dropped: ocamldep
  counts type-only uses (`Int32.t` in Obj, `Complex.t` in Bigarray), a nested `Obj.Ephemeron`, a local `module Mutex` in Lazy, and
  Stdlib's aliases (-no-alias-deps), so the order StdlibModules lists violated its "dependencies". Against the computed ones
  StdlibModules' order satisfies every dependency (the build prints it), and the computed order differs from it (an alphabetical
  tie-break); the 71 samples and oi's essential subset run the same on it.
- **Flags** as before: -nopervasives for Stdlib and CamlinternalFormatBasics, classic labels for the *Labels and Float, the
  transparent aliases of Stdlib (T8); lib/testing typed against its own interface.
- **The directories and the key.** The native library is `.tmp/oi/lib` and `.tmp/oi/testlib` (Lambda `Testing.ae`, and
  testing.ml and .mli), its key (`native_key`: the sources, StdlibModules, the typer's and the reader's forms, stage 0's bytes, the
  script) in `lib/key`, up to date or rebuilt as before, so `aot-lib`, `oi_key` and the suite's keys read it as they read the oracle's.
  `bin/oi stdlib --ocamlc` (or `OI_STDLIB=ocamlc` for any command) is the oracle's path, ocamlc in StdlibModules' order and ocdump,
  into `lib-ocamlc` and `testlib-ocamlc`. The oracle's commands (`corpus`, `check`, the suite's, which dump programs with ocdump and
  run references with ocamlc) get `testing.cmi` and `testing.cmo` in the native `testlib` through `oracle_testlib` (in `need_tree`).
- **`bin/oi stdlib-check`** compares the two libraries: every unit typed afresh and equal to ocamlc's (canonical form), the library's
  files byte for byte a fresh run's, the same set of units, lib/testing's Lambda equal up to stamps, the typer's samples run on it.
  `typer-check` and `run-native` take their interfaces from the native build directory and the library from `lib`.

**Results (2026-11-05, stage 0 of main; processes under 1.5 GB)**:

| command | result |
|---|---|
| `bin/oi stdlib` with ocamlc off PATH (the 1189 other tools of /usr/bin linked into a directory, no `ocaml*`) | 71 units and lib/testing built, 1 min 23 s |
| `bin/oi run-native` of a program using List, String and Printf, ocamlc off PATH | runs, 10 s |
| `OI_TYPER_NO_OCDUMP=1 bin/oi typer-check`, ocamlc off PATH | 73 of 73 types, raw and simplified Lambda equal; 73 of 73 runs on oi equal the toplevel's |
| `bin/oi typer-check` | the same, the committed dumps verified |
| `bin/oi stdlib-check` | 71 of 71 units equal to ocamlc's; 71 of 71 files equal a fresh run; lib/testing equal; the samples 73 of 73 on the library; 2 min 9 s |
| oi's essential subset (the 59 ids of `test/ocaml/essential-check.txt` on `ocaml-o8`, by `--only`: the branch has no `--only-list`) on the native library | 58 pass, none failing, the negative control differs as it must |
| the same subset on the oracle's library (`OI_STDLIB=ocamlc`) | 58 pass: no regression |

What is left: the AOT path (`aot-lib`) over the native library and the gates' keys (`ocaml-o8` declares oi's steps' inputs: they must
list `arbace/repr/ocaml` and `arbace/repr/ocaml/typer` for the library) were not run here (light day, no AOT); the oracle's
`ocdump` still types the programs of `check`, `suite` and `repl` (T11 and T12 are what retire it).

## 18. As built: T11, the run corpus and the toplevel on the native path (2026-11-05, branch `ocaml-t11`, from `ocaml-t10`, `ocaml-native-lib` merged)

TY18's library merged (conflicts in `arbace/repr/ocaml/typer.ae`, both new verbs kept, and in `bin/lib/oi/typer.bash`'s header);
every run below is on the native library (`bin/oi stdlib`'s), so neither the programs nor the library come from ocamlc.

- **`bin/oi check --native [--record] [--jobs N] [--only RE | --only-list FILE]`**: each program of the run corpus typed and
  translated by Arbace (`arbace.repr.ocaml.typer lambda --simplify`, one stage-0 process a program, one Go thread and 1.2 GB each,
  three at once), the programs that use lib/testing against its interface (`--include`), run on oi exactly as `bin/oi check` runs
  ocdump's Lambda, and judged by `test/ocaml/oi_check.ae` against ocamlc's recorded runs (`ref-ocaml55.edn`), twice: on its own
  record, `test/ocaml/check-oi-native.edn` (`--record` writes it; with `--only`/`--only-list` the rows merge), with the native
  classes `test/ocaml/divergences-native.edn` before `divergences.edn`'s and the native known programs
  `test/ocaml/oi-known-native.edn` after `oi-known.edn`'s (merged in `.tmp/oi/native-judge/`, where the judge looks for them), and
  the negative control of `check-oi.edn` repeated for it; then against ocamlc's path, `check-oi.edn`, every row that differs a
  `CHANGED` line. A row's reason is the typer's message alone (Arbace's "Execution error ... at" and report path left out).
  `--only-list FILE` (one id a line) is new for both paths. Flags: `-nolabels` is the typer's `--nolabels` (classic mode),
  `-unsafe` its `--unsafe` (parser.mly's Clflags.unsafe: the indexing operators read as `Array.unsafe_get` and the like, a dynamic
  var of `arbace.repr.ocaml.actions`), `-no-strict-formats` nothing (the format parser's legacy behaviour is the port's only one);
  any other flag is a row of the class `typer-flag`.
- **`bin/oi repl --native`, `bin/oi repl-check --native`**: each phrase typed and translated by the verb `phrase` (via
  `bin/lib/oi/phrase-native`, the protocol of `bin/lib/oi/phrase`: first as `let it__ = (PHRASE)`, else as structure items, the unit
  Top_N with the earlier units opened), its signature written as `top_N.mli` beside its Lambda, so the next phrase finds Top_N as it
  finds the stdlib's units (`--include` of the work directory, read natively). The toplevel's printer (oi's `printSig`, values from
  the signature's types, OC12 (a)) is unchanged. A type extension's path is now printed as Printtyp.tree_of_type_extension prints it,
  not shortened (`type _ Stdlib.Effect.t += Ask : int Effect.t`), the one line the session differed in.
- **`bin/oi typer-probe`** (for TY11, TY12, TY14, TY17's observations): the run corpus's programs without flags copied flat
  (`DIR__BASE.ml`) with the oracle's three dumps beside them, and the typer's `check` over them in one process (`.tmp/oi/probe/check.txt`).
- **The typer's fixes the corpus found** (each ported from the same tarball, SHA-256 fcc6ae66...f015ba): string constants printed as
  ocdump prints them (the bytes as UTF-8 text when valid, `String.is_valid_utf_8`, else a vector of the bytes: "é", `"\xff"`;
  `lib-string`, `lib-buffer`, `lib-uchar`, `lib-bytes-utf`, `lib-scanf` failed on it); refutation cases (`| p -> .`,
  Pexp_unreachable: typed with the expected type, counted in the match's partiality, left out by Translcore's four filters; its
  refutation check is not ported, TY17's kin); an empty variant's value kind `[int]` (Typeopt); transl_apply's protected arguments
  wrapped right to left (`List.fold_right` over `!defs`: `labels_evaluation_order`); Parmatch.orify_many of no constructors (a
  wildcard: `robustmatch`); an assert's location the innermost of its location stack (Typecore's innermost_location:
  `assert_location`); the typer's options `--include`, `--nolabels`, `--unsafe`, `--intf`.

**Results (2026-11-05, stage 0 of main, the native library; 3 programs at once, each process under 1.5 GB)**:

| list | programs | native path passes | ocamlc's path passes (check-oi.edn) | native non-passes that pass on ocamlc's path, by class |
|---|---:|---:|---:|---|
| oi's essential directories, basic/ and effects/ (OC67) | 56 | 56 | 56 | none |
| slice 1 of the rest (`.tmp/t11/slice00`) | 81 | 58 | 75 | typer-effect-syntax 12, typer-letrec-lazy 2, typer-outside-slice 2, typer-known 1 |
| slice 2 | 95 | 73, then 75 with the flags | 86 | typer-flag 10 (then none: `-unsafe` and `-no-strict-formats` passed), typer-outside-slice 2, typer-format-typing 1, typer-type-error 2 |
| slice 3 | 77 | 48, then 50 | 56 | typer-outside-slice 4, typer-type-error 3, assert's location (then fixed) |
| **the run corpus, all 309 run programs (`test/ocaml/check-oi-native.edn`)** | **309** | **244** | **273** | **29: typer-effect-syntax 12, typer-outside-slice 8 (let operators 1, open of a structure 2, functor applications in paths 1, a class's local open 1, `%loc_FUNCTION` 2, recursive modules 1), typer-type-error 5, typer-letrec-lazy 2, typer-format-typing 1, typer-known 1 (recursive_module_init)** |

None failing: every native non-pass is in a class (the judgment of `check-oi-native.edn`), and no program passes natively that fails on
ocamlc's path. The type errors are gaps of the port: constructor disambiguation by the expected type (`Unbound constructor Normal`,
`Marshal`, `Custom` in statmemprof's 5, already classed `gc-and-memory` on ocamlc's path), a format's `'_weak1 list` against
`Format.Args.t`/`Printf.Args.t` (the two tlprintf), a first-class module's `Set.elt`, a modular explicit's signature, and
`prim-revapply`'s "applied to too many arguments" (`@@`/`|>` over an unknown arity). The rows were run in four lists and the
non-passes again after the fixes (`--record --only-list`, merged into one record). The toplevel: `bin/oi repl-check --native`
24 of 24 phrases equal to the ocaml toplevel's (3 min: one process a phrase), `bin/oi repl-check` (ocamlc's phrases on the native
library) equal too. `bin/oi typer-check` 73 of 73 at every level after the fixes; `bin/oi stdlib-check` 71 of 71 units equal to ocamlc's, lib/testing
equal, the samples 73 of 73 on the library.

**The corpus's observations (TY11, TY12, TY14, TY17; `bin/oi typer-probe`, 272 programs without flags that ocamlc accepts)**:
types equal 226, raw Lambda equal 217, simplified Lambda equal 218; of the signatures that differ, 10 with their Lambda equal:

- *TY11, sharing*: `misc/hamming` prints `'b lcons lazy_t` where OCaml prints `'b lcons Lazy.t` (as §15's sample did) and
  `typing-labels/mixin` `string` for `Subst.key`: unify3's links. Both run equal.
- *TY12, names*: no scope difference (no row named or unnamed differently); the names that differ are Printtyp's choice of a path
  among equal ones: `statmemprof/*` print `Gc.Memprof.t` where OCaml prints the local alias `MP.t` (Printtyp's best_type_path
  through module aliases), `lib-hashtbl/htbl` `H.key` for `M.key` (the strengthened path of the other functor argument),
  `lib-queue`, `lib-stack` `Queue.t` for `Stdlib__Queue.t` (an `include` of the unit's own type), `basic/sets` an abstract `t` for
  `Set.Make(SetOrd(BaseSet)).t` (applicative paths in a printed signature). TY21.
- *TY14, Tpoly*: no method type differs; `basic/patmatch` prints a record field's explicit polytype `'a. 'a cell -> 'b -> 'b` as
  `'_ cell -> 'b -> 'b` (a Tpoly field's variables printed after unification; TY22).
- *TY17, partiality*: no match's partiality differs in the corpus (no Match_failure handler added or missing). The Lambda that
  differs with equal signatures is: the required globals listing `Stdlib` when only its submodules are used (12 programs, harmless:
  oi links the library), Matching's names and exits (`misc/gpr1370`, `match-exception/nested_handlers`, both run equal), an assert's
  location one column off (`parsing/assert_location`), and recursive modules' shapes (`basic-modules/recursive_module_init`, the
  one that fails: `oi-known-native.edn`). Refine_or and the explosion of wildcards stay unported (TY17 (a)'s condition does not hold).
- Also: the externals' `[@@unboxed] [@@noalloc]` are not printed in a signature (`weak-ephe-final/ephetest3`), a module type's
  `+!'a t` (variance and injectivity) prints `'a t` (`typing-labels/mixin*`).

**The night's whole-corpus command**: `bin/oi check --native --record` (all 309 run programs, about 22 minutes at three jobs on a
quiet host; light: one Go thread and 1.2 GB a process), then `bin/oi typer-probe` (2 minutes). Speed: a program costs about 8 s of
which the typer's load is most (stage 0 evaluates the typer's namespaces each time); the REPL about 8 s a phrase (TY20).

## 19. Proposed (TY19-TY22; options with costs, a recommendation each, none decided)

- **TY19 The default of `bin/oi check`.** (a) make the native path the default now, `--ocdump` the oracle's, `check-oi.edn`
  re-recorded from the native path (its CHANGED rows become classed non-passes: the effect syntax, the flags, ...); (b) keep the two
  side by side, `check-oi.edn` ocamlc's and `check-oi-native.edn` the native path's, until the native rows equal ocamlc's but for the
  classes owned by milestones already decided (T10's effect syntax, let operators); then (a). *Recommended: (b)*: the oracle's record
  stays the reference the native one is compared with; the gate's ocaml steps (OC120 (b)) can carry both.
- **TY20 The toplevel's speed.** One stage-0 process a phrase costs about 8 s (the typer's load). (a) one typer process a session
  (the phrase verb reading phrases from a pipe, the units' signatures kept in memory: about half a day); (b) keep a process a phrase.
  *Recommended: (a)*, with T12 (errors at the toplevel are T12's).
- **TY21 Printtyp's paths.** The corpus's printed names differ by path choice (best_type_path through aliases, strengthened and
  applicative paths), not scopes. (a) port Printtyp's printing environment and short paths with T12, where printed types are judged;
  (b) now (about a day). *Recommended: (a)*; TY12 (a)'s scopes stay unneeded.
- **TY22 Polytypes in record fields and sharing.** (a) TY11 (a)'s unify3 links with Typecore's levels, and a Tpoly field's printing,
  with T12; (b) now (about a day); (c) not at all (the runs are equal). *Recommended: (a)*.

## Decided (2026-11-05, the user: TY19-TY22)

TY19 (b): ocamlc's and the native records side by side until the native non-passes are decided milestones' classes; TY20-TY22 (a) with T12 (one typer process per REPL session, Printtyp's short paths, unify3 sharing and record-field polytypes).
