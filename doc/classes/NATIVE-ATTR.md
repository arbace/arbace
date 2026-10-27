# The native attribution provider

A reference to repr/java's native attribution: what it answers, after which part of javac, how it
declines, and how its two checks judge it (JFS118 (b), doc/classes/JAVA-FRONTEND-SURVEY.md, "As built:
JN9e"). The history of how it was built is in the survey's "As built" sections JN9a to JN9e; this file
is the state.

## What it is

repr/java's converter (`arbace.repr.java.convert`) turns a Java compilation unit into Java forms. It
reads the unit's tree through `arbace.repr.java.tree` (javac's JCTrees or the native parser's trees as
data, `arbace.repr.java.parse`) and asks every fact that is not syntax through
`arbace.repr.java.attr`: a symbol, a type, a chosen method, an instantiated signature, a jump
target, a flag, an annotation's value. Its reads of the answers (a symbol's flags, a type's
erasure, a class's members) go through `arbace.repr.java.model`, over either of two models: javac's
objects (Symbol, Type, Attribute) or the native ones (`arbace.repr.java.sym`'s symbols, types as
data).

The native provider (`arbace.repr.java.nattr`, `provider`) answers those facts for a native tree
without javac. It is made per unit: `arbace.repr.java.scope`'s walk of the unit records each node's
answers (its environment, its local, its declaration), and the higher tiers compute the rest on
demand, memoized in the provider. The units of a run are entered in one registry
(`arbace.repr.java.decls/registry`): the run's sources, the other sources of its source path or
patched module (parsed when named), and the platform's class files (`arbace.repr.java.classfile`),
the JDK build's exploded modules that javac on Arbace reads too (`jdk/javac/platform.sha256`).

Three ways to read:

| read | trees | attribution | javac runs |
|---|---|---|---|
| `read` (`--front javac`) | javac's | javac's | yes: the reference |
| `read --front native --attr native` | native | the provider, each answer checked against javac's (by position: `attr/index` pairs each native node with javac's attributed node of its tag and position); javac's answer where the provider declines | yes, beside it |
| `read --no-javac` | native | the provider alone; a file that meets what only javac answers is read again by javac (the fallback) | only for the fallback's files |

## The tiers

The facts are classed in tiers, cheapest first (`arbace.repr.java.split/tiers`); each tier's
namespace and the javac code it follows (jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259`,
`src/jdk.compiler/share/classes/com/sun/tools/javac/`):

| tier | what | namespace | javac |
|---|---|---|---|
| syntax | a literal's type | `attr` | `comp/Attr.java` (visitLiteral) |
| scope | locals, parameters, pattern bindings, captures, jump targets, liveness | `arbace.repr.java.scope` | `comp/Attr.java` (visitIdent, findJumpTarget), `comp/MemberEnter.java`, `comp/Flow.java` (AliveAnalyzer) |
| declarations | classes, members (TypeEnter's generated ones), flags, declared types, names in types and qualifiers, annotation values, constants | `arbace.repr.java.decls`, `arbace.repr.java.classfile` | `comp/Enter.java`, `TypeEnter.java`, `MemberEnter.java`, `Annotate.java`, `Check.java` (checkFlags, validateAnnotation), `ConstFold.java`, `jvm/ClassReader.java`, `code/ClassFinder.java` |
| flow | a case that completes normally, an unconditional pattern, exhaustiveness for compiling code | `arbace.repr.java.scope`, `arbace.repr.java.expr` | `comp/Flow.java`, `comp/Attr.java` (handleSwitch) |
| members | names in code: fields, classes, packages, inherited and hidden members | `arbace.repr.java.expr`, `arbace.repr.java.nattr` | `comp/Resolve.java` (findIdent, findField), `code/Symbol.java` (isMemberOf, isInheritedIn) |
| types | every expression's type, operators, constants, member types at a site, captures | `arbace.repr.java.expr`, `arbace.repr.java.types` | `comp/Attr.java`, `comp/Operators.java`, `code/Types.java` |
| overload | a call's method or constructor, its phase and arity, method references' lookups and kinds | `arbace.repr.java.overload` | `comp/Resolve.java` (the three phases, mostSpecific, resolveMemberReference) |
| inference | generic methods and constructors, diamonds, poly arguments, lambdas, method references' targets, local classes' numbering | `arbace.repr.java.infer`, `arbace.repr.java.expr` | `comp/Infer.java`, `InferenceContext.java`, `DeferredAttr.java`, `ArgumentAttr.java`, `Attr.java` (visitLambda, visitReference, checkMethod), `Check.java` (localClassName), `Flow.java` (analyzeLambdaThrownTypes) |

## The facts

Each fact the converter asks has an id (`:class/site`); its class is its namespace. The catalogue
(`arbace.repr.java.split/catalogue`) gives each its tier, javac's source and what the converter takes
from it:

| fact | tier | javac's source | what the converter takes |
|---|---|---|---|
| `types/literal` | syntax | Attr.visitLiteral | a literal's type (its typetag: int, long, String, ...) |
| `scope/assigned` | scope | Attr.visitIdent | an assigned local (the converter's ^:mutable) |
| `scope/binding` | scope | Attr.visitBindingPattern | a pattern variable's symbol |
| `scope/capture` | scope | Attr.visitIdent | a local a class or lambda captures |
| `scope/hoist` | scope | Attr.visitIdent | the names in an expression with postfix increments (their uses) |
| `scope/ident` | scope | Attr.visitIdent, Resolve.findVar | a name's local variable or parameter |
| `scope/jump-target` | scope | Attr.findJumpTarget | the statement a break, continue or yield leaves |
| `scope/label` | scope | Attr.visitIdent | a case label naming a local constant |
| `scope/lambda-param` | scope | Attr.visitLambda | a lambda parameter's symbol (identity, type: inferred for implicit ones) |
| `scope/local-decl` | scope | MemberEnter (local VarSymbol) | a local declaration's symbol (identity, declared type, flags: final, effectively final) |
| `scope/local-names` | scope | MemberEnter | the unit's locals' names |
| `scope/param` | scope | MemberEnter.visitMethodDef | a parameter's symbol (identity, type, flags) |
| `scope/refers` | scope | Attr.visitIdent | whether a tree mentions a local |
| `scope/resource` | scope | Attr.visitIdent | a resource named by a local |
| `scope/select` | scope | Attr.visitSelect | a selection that is a local |
| `scope/type-expr` | scope | Attr.visitIdent | a qualifier that is a local, not a type |
| `annotations/attribute` | declarations | Annotate | an annotation's values (constants, classes, enums, nested), its retention |
| `annotations/dropped` | declarations | Check.validateAnnotation | a record component's annotation taken off its field's modifiers (JFS110 (b), JN9e) |
| `enter/class-decl` | declarations | Enter.visitClassDef, TypeEnter | a class's symbol: flags, members, supertypes, type parameters, record components |
| `enter/field-decl` | declarations | MemberEnter.visitVarDef | a field's symbol: flags (enum, record, generated), type, constant value |
| `enter/flags` | declarations | Enter.visitClassDef | the modifiers as Enter completes them (public static of an interface's member class) |
| `enter/local-names` | declarations | MemberEnter | a field among the unit's variable declarations |
| `enter/members` | declarations | TypeEnter, MemberEnter | a class's members by name, flags and erased signature, TypeEnter's generated ones included (checked only, JFS79) |
| `enter/method-decl` | declarations | MemberEnter.visitMethodDef | a method's symbol: flags (varargs, generated, compact), type, default value |
| `enter/platform-members` | declarations | ClassReader | a platform class's members as its class file gives them, against javac's ClassSymbol (checked only, JFS106 (b)) |
| `names/package` | declarations | Enter.visitTopLevel | the unit's package symbol and its classes |
| `names/record-pattern` | declarations | Attr.visitRecordPattern | a record pattern's record type |
| `names/type-expr` | declarations | Attr.visitIdent, visitSelect (KindSelector) | whether a qualifier names a type or package (JLS 6.5) |
| `names/type-tree` | declarations | Attr.attribType, TypeEnter | a type tree's type (names resolved, type arguments, annotations) |
| `types/class-literal` | declarations | Attr.visitSelect | a class literal's type |
| `types/this` | declarations | Attr.visitIdent, visitSelect | the type of this, super, C.this |
| `flow/case-completes` | flow | Flow.AliveAnalyzer | whether a case's statements complete normally (fall-through) |
| `flow/exhaustive` | flow | Flow, ExhaustivenessComputer | whether a switch statement is exhaustive |
| `flow/unconditional-pattern` | flow | Attr.handleSwitch | whether a switch has an unconditional pattern |
| `names/assigned` | members | Attr.visitIdent | an assigned name's symbol (a field) |
| `names/capture` | members | Attr.visitIdent | a name's symbol in a capturing class or lambda (a field or class) |
| `names/hoist` | members | Attr.visitIdent, visitSelect | the names' symbols in an expression with postfix increments |
| `names/ident` | members | Attr.visitIdent, Resolve.findIdent | a simple name's field or class (inherited, outer, static imports) |
| `names/ident-sym` | members | Attr.visitIdent | a name's symbol |
| `names/label` | members | Attr.visitIdent, visitSelect | a case label's constant field |
| `names/local-names` | members | MemberEnter | the unit's variable declarations' symbols |
| `names/method` | members | Attr.visitApply | a call's method symbol where it is not a method |
| `names/package-classes` | members | ClassFinder.fillIn, includeClassFile | the top-level class names of the unit's package, on every location (JFS112 (b), JN9e) |
| `names/refers` | members | Attr.visitIdent | a name's symbol (a field or class), compared with a local's |
| `names/resource` | members | Attr.visitIdent | a resource named by a field |
| `names/select` | members | Attr.visitSelect, Resolve.findIdentInType | a selection's field, class or package |
| `types/constant` | types | Attr, ConstFold | an expression's constant value (folded labels, while(true), concatenation operands) |
| `types/expr` | types | Attr (attribExpr, check) | an expression's type (erased by the converter) |
| `types/ident` | types | Attr.visitIdent | an odd name's type |
| `types/member-constructor-type` | types | Types.memberType | a constructor's type at the class's type arguments |
| `types/member-method-type` | types | Types.memberType | a call's method type at the site's type arguments |
| `types/member-varargs-element` | types | Types.memberType | a variable arity call's array element at the site's type arguments |
| `types/new` | types | Attr.visitNewClass | a new instance's class type |
| `types/new-array` | types | Attr.visitNewArray | a new array's type |
| `types/operator` | types | Attr.visitBinary, Operators.resolveBinary | the operator resolved by the operand types (its parameter types) |
| `types/record-components` | types | TransTypes.visitRecordPattern, TransPatterns | a record pattern's components' erased member types at its type, and declared (a nested type pattern unconditional by the type arguments, JCS114 (a), JN9f) |
| `types/pattern-switch` | types | Attr.handleSwitch | whether a switch is a pattern switch (labels and selector type) |
| `types/qualifier` | types | Attr.visitSelect | a qualifier's type (the site of a member) |
| `types/result` | types | Attr (the operands' types) | a conversion decided by a converted operand's type (coerce, test-form) |
| `types/var-constant` | types | Attr.visitVarDef, attribLazyConstantValue | a final `var` local's constant value at its inferred type (JFS112 (b), JN9e) |
| `overload/anon-super-ctor` | overload | TypeEnter (anonymous constructor), Resolve | the superclass constructor of an anonymous class |
| `overload/arity` | overload | Resolve (phase 3) | whether a call is of variable arity |
| `overload/assigned` | overload | Resolve | a method named where assignments are sought |
| `overload/capture` | overload | Resolve | a method named in a capturing class or lambda |
| `overload/constructor` | overload | Resolve.resolveConstructor | a new instance's constructor |
| `overload/declared-constructor-type` | overload | MemberEnter (the symbol's type) | a constructor's type, the symbol's own |
| `overload/declared-method-type` | overload | MemberEnter (the symbol's type) | a call's method type, the symbol's own (not generic) |
| `overload/declared-varargs-element` | overload | Resolve (phase 3) | a variable arity call's array element, the declared one |
| `overload/hoist` | overload | Resolve | a method named in an expression with postfix increments |
| `overload/ident` | overload | Resolve | a name that is a method |
| `overload/local-names` | overload | Resolve | - |
| `overload/method` | overload | Resolve.resolveQualifiedMethod, findFun | a call's method symbol |
| `overload/method-ref` | overload | Attr.visitReference, Resolve | a method reference's method |
| `overload/ref-kind` | overload | Attr.visitReference | a method reference's kind (static, bound, unbound, constructor) |
| `overload/refers` | overload | Resolve | a method named where a local is sought |
| `overload/select` | overload | Resolve | a selection that is a method |
| `infer/constructor-type` | inference | Attr.visitNewClass, Infer | a generic or diamond constructor's instantiated type |
| `infer/method-type` | inference | Attr.checkMethod, Infer | a generic method's instantiated type (argument targets, call site types) |
| `infer/poly-result` | inference | Attr.visitApply | a signature polymorphic call's result type |
| `infer/target` | inference | Attr.visitLambda, visitReference, DeferredAttr | a lambda's or method reference's functional interface type |
| `infer/varargs-element` | inference | Attr.checkMethod, Infer | a generic method's variable arity array element type, inferred |
| `names/local-class` | inference | Check.localClassName, Attr's order | a local or anonymous class's flat name, numbered as Attr enters them (checked once per class, JFS91 (b)) |

A fact asked of a symbol (`attr/sym`) takes its class from the symbol: `scope` a local, `enter` a
declaration's own, `names` a field, class or package named, `overload` a method; so `scope/ident`,
`names/ident` and `overload/ident` are one site's three classes.

## The model's reads

The converter reads the answers through `arbace.repr.java.model` (`kind`, `flags`, `owner`, `stype`,
`erasure`, `members`, `pkg-class-names`, ...); each read is counted by name and by how it was
answered: `:native` (the native model), `:javac` (javac's object, which a fact javac answered gave),
`:bridge` (a native object whose read asked javac's symbol of the same declaration). attr-check fails
on a bridge not listed in `test/repr/java/attr-check-known.edn`'s `:bridges`; a read without javac
cannot make a `:javac` or `:bridge` read at all (it falls back).

## Declines

Where the port does not follow javac, the provider declines: `arbace.repr.java.decls/decline!` with a
reason, counted under `:why/<fact>` by its reason's key (`nattr/decline-reason`: the message without
the names it gives). With javac beside it the fact takes javac's answer; without javac the file falls
back. The reasons by tier:

- scope, declarations: a name or a declaration the walk does not reach (`no symbol`, `no field`, `no
  environment for`), a local without a declaration;
- members, types: an expression of a kind the types tier does not type (`an expression`), a
  qualified creation's class or qualifier, a class literal of an unknown class;
- overload: a call's site that is not a class, an array or an intersection; the call's phase;
- inference: a cycle in the inference tier's computations (a memoized computation, `:checked`,
  `:args-checked`, re-entered more than three times), a generic method's call in an argument context
  the tier does not reach, an implicit lambda attributed speculatively, a generic functional
  descriptor in most specific; the port's size limits (`an inference beyond the port's limits`: more
  than 64 bounds on a variable, 2,000 incorporation rounds, 1,000 deferred nodes; JFS113 (c): a check
  fails when one fires, as it is a runaway, not a decline).

The current counts are the survey's last "As built" section's tables (`attr.md`'s "The declines by
fact and reason", `nojavac.md`'s "The fallbacks by reason").

## How attr-check judges

`bin/repr-java attr-check [--corpus C,...]` (`bin/lib/repr-java/front-check --attr native`): each
corpus is read with `read --front both --split --attr native`: javac on Arbace attributes the files
once; the converter converts each unit from javac's trees and from the native trees with the native
provider; for every fact asked of a native tree the provider's answer is computed and compared, as
data, with javac's answer for the twin node (`attr/native-fact`): `:equal`, `:differ` (the first 400
kept, with both answers), `:declined` (with its reason), `:leaned` (the answer took javac's choice of
a higher tier). The run fails when

- a corpus's forms from the native trees differ from those from javac's trees, byte for byte (but the
  known file's `:forms`),
- a fact differs beyond the known file's `:facts` counts,
- a model read bridges that is not among `:bridges`,
- an inference size limit fired (JFS113 (c)).

The outcomes are summed by corpus, tier and fact into `WORK/attr.md` and `attr.edn`; `split.md` gives
the share of the output's lists the provider wrote alone (no javac answer in the facts its frame
asked).

## How nojavac-check judges

`bin/repr-java nojavac-check [--corpus C,...]` (`front-check --no-javac`): each corpus is read twice,
by javac's trees (`read --front javac`, the reference) and without javac (`read --no-javac`), and the
forms must be equal file for file, byte for byte (but `test/repr/java/nojavac-check-known.edn`'s
files). In a read without javac (`arbace.repr.java/convert-units-nojavac`) each file is parsed
natively and entered in the run's registry; each unit is converted with `decls/*no-javac*` bound, under
which whatever would ask javac stops the unit (`decls/javac-needed!`): a node's javac twin, a declined
fact, a lean on javac's choice, a `:javac` or `:bridge` model read, a parse error, any failure. Those
files are then read by javac in one run (`convert-units` on them, javac's trees), so the output is
always javac's where the provider cannot answer. Each read writes `OUT/repr-java-nojavac.edn`: its
files, the native ones, the fallbacks with their reasons (`decls/javac-needed-reason`), its time and
its process's peak memory; the check sums them by corpus (`WORK/nojavac.md`, `nojavac.edn`) and fails
when an inference size limit fired.

A read without javac is not checked fact by fact (there is no javac answer to compare with): its check
is the forms. attr-check is what checks the facts; the two are run on the same tool.

## What it assumes

The provider answers as javac does for code that compiles: javac's errors are not ported (JFS105 (a),
JFS114 (a), JFS127). On code javac rejects, a read beside javac shows it (javac's errors in the run's
report); a read without javac may convert it without a word, unless the parse fails (a fallback) or an
answer it needs is not defined (a decline, hence a fallback). The vendoring tools' sources compile.

## Tracing

`REPR_JAVA_INFER_TRACE=1` prints the inference's steps (bounds, errors), a cycle's stack and a failed
fact's stack to stderr (JFS117 (a)); `REPR_JAVA_ATTR_DECLINED=1` keeps every decline's data in
`repr-java-attr.edn`'s `:diffs`; `REPR_JAVA_NOJAVAC_TRACE=1` prints each fallback's reason and stack.
