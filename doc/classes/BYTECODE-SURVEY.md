# Classes defined from class-file bytes — design and stages

Status: design, 2026-11-22, for the user's decision JD3 of 2026-11-22 (doc/JOURNAL.md, "the user's decisions on
java-design's questions (JD1-JD9)"): "Java classes defined from class-file bytes are built in full: a class-file loader
with a bytecode interpreter and verifier, so that `ClassLoader.defineClass`, `Lookup.defineClass` and their kin work on
Arbace." It reverses JCS302 (a) (doc/classes/JAVA-DEFERRED-TRIAGE.md, "JCS302") and J2G-SPEC V9's "nothing can be
defined at run time but ..." for class files. Branch `java-bytecode-1122`. The stages' "As built" sections follow at
the end as they land.

## 1. The rows

java-check's records at mac/next 195fafabb (`test/java/check-{interp,compiled}.edn`), each test at both levels:

| test | file | what it needs |
|---|---|---|
| t04537 | jdk/java/lang/Class/getSimpleName/GetSimpleNameTest.java | `findClass` overridden and called by `loadClass`; InnerClasses and EnclosingMethod of a defined class (`getSimpleName`), two classes defined by name in one loader, the same names again in other loaders |
| t04712-t04715 | jdk/java/lang/annotation/AnnotationTypeMismatchException/*.java | a defined class's RuntimeVisibleAnnotations whose values mismatch the element types (AnnotationTypeMismatchException); compiled: `Class.describeConstable` |
| t04865 | jdk/java/lang/reflect/Method/invoke/TestPrivateInterfaceMethodReflect.java | an interface with a private instance method and a class implementing it, defined in `findClass`; `Method.invoke` of the private method runs its code (`ldc`, `ireturn`) |
| t04868 | jdk/java/lang/reflect/Parameter/BadClassFiles.java | MethodParameters read; `getParameters` throws MalformedParametersException for the bad ones |
| t04870 | jdk/java/lang/reflect/Parameter/NoName.java | MethodParameters with an absent name (`getParameters` synthesizes it) |
| t07735 | hotspot/jtreg/runtime/verifier/VerifyProtectedConstructor.java | the verifier (version 49 and 50): `new A.A()` of a protected constructor from another package is a VerifyError |
| t07736 | hotspot/jtreg/runtime/verifier/VerifyStackForExceptionHandlers.java | the verifier (version 49): a handler with max_stack 0 is a VerifyError; trailing bytes past the class file's end ignored by the length |
| t07737 | hotspot/jtreg/runtime/verifier/defaultMethods/DefaultMethodRegressionTests.java | an interface with a default method (version 52) and a class implementing it (version 49, the inference verifier), loaded by `Class.forName(name, true, loader)` |

They are in `test/java/divergences.edn`'s `:define-bytes` (t04537, t04712-t04715, t04865), `:classloading`'s match
(defineClass: t04868, t04870, t07735, t07736) and `:design-also` (t07737): 11 tests, 22 rows.

## 2. Where it goes

A class defined from bytes is an **interpreted class of the Java forms' interpreter** (`Compiler$CF`, doc/go/CLASSFORMS-REPL.md):
a `CF$Klass` made through the same host (`CF$Rt/HOST`, `Compiler$CFGo$GoHost` on Go: J2G-SPEC §5.12, §10.4), whose
methods are `CF$Meth`s whose body runs the method's bytecode. So it mixes with the Java forms' classes and the world's
through the one class model the interpreter already has: its objects are `Dyn` (or `DynSub_C` for a superclass of the
world on m/class-supers' list), its methods fill Dyn's slots and the member tables (reflection, interface calls from
translated code), its fields are the object's slots and the Klass's statics, its class initialization is
`CF$Klass.init` (JLS 12.4.2), its frames show in stack traces (queue 416's shadow stack). A bytecode method calls an
interpreted method (Java forms or bytecode) by its `Meth`, a method of the world by reflection (`java.lang.reflect`,
jrt's member tables: public members) or by the host's super calls (`superCall`, `superCtor`, `superField`: protected
members of a superclass of the world), as the Java forms' interpreter does (`CF$CallV`, `CF$CallR`, `CF$SuperCall`).

The loader, the interpreter and the verifier are **Java forms** in `arbace/lang/Bytecode.ae` (package `arbace.lang`,
translated by j2g into the executable and into every program, as `Compiler.ae`'s interpreter is): the class model they
use (`CF$Klass`, `CF$Meth`, `CF$Frame`, `CF$Host`) is Java forms, and so is everything a program reaches. One node class
in `Compiler.ae`, `CF$Bytecode`, is a `CF$Node` whose evaluation runs its method's bytecode.

jrt (Go) gives `java.lang.ClassLoader` its loading model (JLS 12.2, JVMS 5.3): `loadClass` by delegation
(`findLoadedClass`, the parent's `loadClass`, `findClass`), the defining loader of a class (`getClassLoader`), the
classes a loader defined by name (`findLoadedClass`), `Class.forName(name, init, loader)` through the loader, and the
`defineClass` family (`defineClass(byte[],int,int)`, `(String,byte[],int,int)`, `(...,ProtectionDomain)`,
`(String,ByteBuffer,ProtectionDomain)`), which hands the bytes to `arbace.lang.Bytecode.define`, found by name in the
registry (jrt cannot name a translated class: no import cycle, and a program without the interpreter answers as now).
A class a loader defines is not entered in the closed world's registry by name (`Class.forName(name)` of another loader
does not see it); it is kept by its loader (J2G-SPEC V9 amended).

## 3. The loader (JVMS chapter 4 and 5.3.5)

`Bytecode$ClassFile` parses the bytes (JVMS 4.1-4.7): the constant pool (all 17 tags), access flags, this, super,
interfaces, fields, methods, attributes, with the format checks of JVMS 4.8 that raise `ClassFormatError`
(truncation, extra bytes, constant pool indices and tags, names, descriptors, flags' combinations, duplicate members,
the attributes' lengths) and `UnsupportedClassVersionError` (major beyond 70, the platform's: Java 26). The attributes
read: Code (max_stack, max_locals, code, exception table, StackMapTable, LineNumberTable), Exceptions, ConstantValue,
InnerClasses, EnclosingMethod, Signature, SourceFile, BootstrapMethods, MethodParameters, RuntimeVisibleAnnotations,
RuntimeVisibleParameterAnnotations, AnnotationDefault, Record, NestHost, NestMembers, PermittedSubclasses; the rest
are skipped as JVMS 4.7.1 allows.

Defining (JVMS 5.3.5): the name must match (`NoClassDefFoundError` "... (wrong name: ...)"), a class of the loader's
own of that name is `LinkageError` "duplicate class definition"; `java.*` names are refused (`SecurityException`
"Prohibited package name"); the superclass and interfaces are loaded through the defining loader (`loadClass`), checked
(`IncompatibleClassChangeError`: an interface as superclass, a class as interface; `ClassCircularityError`; `VerifyError`
"Cannot inherit from final class"), and the Klass is laid out as `arbace.classes.interp/layout!` lays out a Java forms
class: instance fields after the superclass's, statics with their ConstantValue, methods and constructors by key, the
vtable (superclass's, interfaces' defaults, own), published to the host with the reflection data: modifiers, members'
Class types (loaded through the defining loader when first published), the annotations and attributes as the data jrt
decodes (`T`, `M`, `C`, `F`, `P`, `D`, `K`, `Q`, `S`, `G`, `E`, `R` records: go/arbace/jrt/annotation.ae,
classdata.ae), and the MethodParameters records (`W`, new) for `getParameters`.

Linking (JVMS 5.4) happens before the class's initialization, its first instance, or its first method call: the
verifier runs over every method (§5); preparation is the layout above; resolution is lazy, per constant pool entry,
cached in the method's interpreter state, with `NoClassDefFoundError`, `NoSuchFieldError`, `NoSuchMethodError`,
`IllegalAccessError` and `IncompatibleClassChangeError` as JVMS 5.4.3 raises them.

## 4. The interpreter (JVMS chapter 6)

`Bytecode$Code` interprets one method: an operand stack of two arrays (`long[]` for int, long, float and double values,
as their bits; `Object[]` for references), the JVM's slot accounting (a long or double takes two slots), the locals in
the `CF$Frame`'s slots by local index. Every instruction of JVMS 6.5 (202 opcodes), with:
- arithmetic as Java's (int overflow, the shifts' masks, `idiv` by zero's ArithmeticException, `fcmpl`/`fcmpg`, NaN's
  conversions, `f2i` saturation, strictfp everywhere);
- `new` of an interpreted class allocates its object (`Host.alloc`), of a world class a placeholder replaced by the
  object `Constructor.newInstance` makes at its `invokespecial <init>`; a constructor's super call to a world class
  `Host.superCtor`;
- `invokevirtual`, `invokeinterface`: an interpreted receiver's method by its Klass's vtable, else reflection;
  `invokespecial` (private, super, constructors) and `invokestatic` without dispatch; JVMS 5.4.6's selection rules,
  `AbstractMethodError`, `IncompatibleClassChangeError`;
- `invokedynamic` and dynamic constants by their bootstrap methods: `LambdaMetafactory.metafactory` and
  `altMetafactory` (a class made at run time implementing the interface, its method calling the implementation method,
  serializable ones too), `StringConcatFactory.makeConcatWithConstants` and `makeConcat`, `ObjectMethods.bootstrap`
  (records), `SwitchBootstraps.typeSwitch` and `enumSwitch`, and `ConstantBootstraps`' constants; another bootstrap
  method is `BootstrapMethodError` (no `MethodHandle` call machinery in the Go build: J2G-SPEC §12);
- exceptions by the exception table (the first handler whose range holds the pc and whose class the exception is an
  instance of), `athrow`, exceptions of the world caught alike; `monitorenter`/`monitorexit` on the world's monitors
  (`Evaluator.monitorEnter`), a synchronized method's monitor held around its body;
- `jsr`/`ret` (class files before version 51), `wide`, `tableswitch`, `lookupswitch`, `multianewarray`.

## 5. The verifier (JVMS 4.10)

`Bytecode$Verifier` checks every method's code before the class is initialized, raising `VerifyError` with HotSpot's
reason text where a test reads it:
- version 50 and later: **verification by type checking** (JVMS 4.10.1), its Prolog rules ported clause by clause: the
  type hierarchy (top, oneWord, twoWord, int, float, long, double, reference, uninitialized, uninitializedThis,
  uninitialized(Offset), class(N, L), arrayOf(T), null), `isAssignable` (classes loaded through the defining loader,
  interfaces as Object), the StackMapTable decoded into frames (JVMS 4.7.4), each instruction's rule
  (`instructionIsTypeSafe`), the exception handlers' rule, `protected` access (4.10.1.8), `<init>`'s rules; version 50
  falls back to inference when type checking fails, as HotSpot's `FailOverToOldVerifier`;
- before 50: **verification by type inference** (JVMS 4.10.2), the data-flow analyzer over the same per-instruction
  rules, merging frames at join points (references to their first common superclass, interfaces as Object), with
  `jsr`/`ret` subroutines.
Not ported from HotSpot (a reference only, no code copied): its error message formats beyond the reason's text.

## 6. Stages

1. **Loading and linking**: jrt's ClassLoader model and `defineClass` family; the parser with its format checks; the
   Klass, its members and reflection data; `Class.describeConstable`. Tests: a REPL harness over the gate executable
   (the Java forms of `Bytecode.ae` interpreted, `.tmp/dev/`) for the parser and the class model; the jrt unit tests.
2. **The interpreter**: the instruction set, invocation, exceptions, monitors, invokedynamic. Tests: class files built by
   `java.lang.classfile` and by javac (jdk26u's, its output committed as test data with its source), run on the REPL
   harness and the built executable against their expected output.
3. **The verifier**: type checking and inference. Tests: the rows' class files, good and bad, and mutants.
4. **The rows**: one build, `bin/java-check --levels interp,compiled --only LIST --judge --partial --record` with the
   controls; the divergence classes updated (`:define-bytes` left, `:classloading` narrowed), REAP and the scores.

## 7. What it does not do

- Class files of the platform's own classes (`java.*` defined by a user loader is refused, as the JVM refuses it).
- `MethodHandle` invocation beyond the bootstraps of §4 (J2G-SPEC §12: no `MethodHandle` machinery).
- Bytecode calling a world method that the program's reachability left out (a compiled program has the methods its
  code reaches, J2G-SPEC §4.1): `NoSuchMethodError`, as a closed world answers.
- JIT, class unloading, CDS, class redefinition (`java.lang.instrument`).
