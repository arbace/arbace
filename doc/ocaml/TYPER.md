# oi's native front end: OCaml's typer and Translcore ported

Status: design and first slice, 2026-11-05, on the branch `ocaml-typer` (from `ocaml-native`, whose native reader gives
the Parsetree as Arbace data). The route of OC4 (b) (doc/ocaml/OCAML-SURVEY.md §6.4): port OCaml 5.5.0's `typing/` and
`lambda/`'s Translcore with Matching, so that the native Parsetree becomes Lambda in Arbace and oi stops needing the
host's `ocamlc` for a program. Light rules (the day's): no gate, no Arbace build, no re-seed; every run on main's stage 0
(`ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`), one process under 1.5 GB, samples only. Nothing here is
decided: the proposals are TY1-TY6 (§8).

## 1. What needs ocamlc today, and what the slice removes

oi (survey "As built: O5") interprets Lambda; four things come from the host's compiler through `tools/ocdump` on
compiler-libs:

| use | today | after this slice |
|---|---|---|
| a program's Lambda (`bin/oi run`, `check`) | `ocdump --mode lambda --simplify` | `bin/oi run-native`: Arbace (core-language programs) |
| the stdlib's Lambda (`bin/oi stdlib`) | ocamlc builds the `.cmi`, ocdump dumps each unit | unchanged (T8) |
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
| `typer` (driver) | Arbace's own | `types`, `lambda`, `check` (§4) |

Size: the port is 4,000 lines of Arbace (the driver and bin/lib/oi/typer.bash included) for the parts of about 45,000
lines of OCaml (the 22 source files it draws on) that the slice needs.

**The raw translation.** The output is Translcore's raw Lambda (what `ocamlc -drawlambda` prints, ocdump's `--mode
lambda` without `--simplify`), not `-dlambda`'s: Simplif (`lambda/simplif.ml`: exits, lets, `mutlet` for local refs,
the optional-argument wrappers of `split_default_wrapper`) is T7. oi runs the raw translation as it runs the simplified
one (SPEC.md §11.2: the two differ in nothing oi needs), and the 36 samples' runs show it.

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

1. **Types**: the signature Arbace prints against ocdump `--mode types`'s (Printtyp's `signature` of the inferred
   interface, the toplevel's `val` lines), as text, equal.
2. **Lambda**: Arbace's Lambda against ocdump `--mode lambda`'s (the raw translation, `--prefix` so file names are the
   sample's), both read as forms and compared in a canonical form: every identifier renamed in order of appearance
   with its name kept (`x_276` and `x_1203` both `x#k`), and every static exit's number likewise. Stamps and exit
   numbers are counters of the whole compiler process (reading `.cmi` files makes identifiers), so equality up to them
   is the right equality; names, value kinds, primitives, constants, shapes and the structure are compared exactly.
3. **Runs**: the native Lambda run on oi (`.tmp/oi/bin/oi run` with the stdlib's Lambda) against the sample's `.out`,
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
- `typer-simplif`: compared against `-dlambda` before T7;
- `typer-error-message`: a program OCaml rejects, refused with another message (until T12).

## 6. Milestones

| milestone | content | exit criteria | days |
|---|---|---|---:|
| T1-T6 (this) | types, env, parmatch, typecore, lambda (Switch), matching, translcore; driver and check | 36 of 36 samples: types, Lambda, oi runs | done |
| T7 Simplif | `lambda/simplif.ml`: `simplify_exits`, `simplify_lets` (`mutlet`), `split_default_wrapper`, `simplify_local_functions`; the judge against `-dlambda` too | the samples equal to `ocdump --simplify` | 2-3 |
| T8 the module system | Typemod (structures, signatures, inclusion and coercions, strengthening, applicative paths), functors (`Map.Make`, `Set.Make`, `Hashtbl.Make`), `open`/`include`/local opens, recursive modules; Translmod complete; then the stdlib's own `.ml` typed and translated natively | `bin/oi stdlib --native`: every unit's Lambda equal to the oracle's (canonical), oi's check on it unchanged | 8-12 |
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
