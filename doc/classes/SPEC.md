# Class forms: Java's class files written in Clojure

Status: proposal for review (2026-10-06). No code implements it yet.

This spec defines the **class forms**: how every construct javac can put into a class file is
written as idiomatic Clojure, in ordinary `.clj` sources, and compiled by Arbace's own compiler
to classes equivalent to javac's. It covers the forms, what they mean, what the compiler
derives by itself (as javac does), and what the Java → Clojure converter must spell out.

Words used throughout:

- **class form**: `defclass` and the other forms that define classes (`anon`, `letclass`,
  `defmodule`, `defpackage`).
- **class body**: the member forms inside a class form.
- **the compiler**: Arbace's compiler. At stage 0 it is a bootstrap library running on the
  frozen `clojure/` and emitting bytecode with `clojure.asm`. Later it is Arbace's self-hosted
  compiler. Both implement this spec.
- **the converter**: the Java → Clojure converter (agenda step 3), working on javac's
  attributed trees.
- **Java subset**: the part of Clojure that compiles to plain JVM code without the Clojure
  runtime (§5.13). Converted code uses only this subset.

Contents: 1 Principles · 2 A first example · 3 Equivalence · 4 Declarations · 5 Code ·
6 What the compiler derives · 7 What the converter makes explicit · 8 Coverage ·
9 Compilation model · 10 The REPL · 11 Worked examples · 12 Open questions · 13 Sources.

## 1. Principles

1. **Clojure keeps its meaning.** Every form that works today means what it meant. New meaning
   goes only to new names and to forms that are errors today: `^int` on a local with a primitive
   initializer, `set!` of a local, a list as `:tag`, `recur` out of tail position, `new` of an
   array class. The new names are vars in `arbace.core` (macros over a few new starred special
   forms, like `let` over `let*`), so a namespace can exclude or shadow them like any other core
   var.
2. **Declarations mirror Java.** One class form per Java type declaration, one member form per
   member, in Java's order. Modifiers and types are metadata, as in Clojure. Java's defaults and
   implicit modifiers apply: no modifier means package access, as in Java.
3. **javac's derivations are the compiler's job.** Whatever javac derives from declarations
   (default constructors, enum and record members, bridges, outer-instance fields, accessors,
   lambda methods, switch maps...) the compiler derives the same way, with javac's names (§6).
4. **What javac infers from types is explicit.** Arbace does not type-check generics or infer
   types the way javac does. Generic types appear only in declarations, for `Signature`
   attributes. Erasure casts, narrowing conversions and the overloads Arbace would choose
   differently are written out (§7).
5. **Plain Clojure syntax.** Everything reads with the frozen Clojure 1.12 reader: `^meta`,
   `^[param-tags]`, array class symbols like `String/1`, qualified symbols like `Outer/this`.
   There are no reader changes.
6. **The Java subset needs no runtime.** A class body that uses only the Java subset compiles to
   bytecode that references nothing of Clojure: no vars, no `clojure.lang.RT`. That is how
   `arbace.lang` itself can be written with these forms.

## 2. A first example

`clojure/lang/Reduced.java`:

```java
public final class Reduced implements IDeref{
Object val;

public Reduced(Object val){
	this.val = val;
}

public Object deref(){
	return val;
}
}
```

As a class form:

```clojure
(defclass ^:public ^:final Reduced
  :implements [IDeref]

  (field val)

  (constructor ^:public [this val]
    (set! (.-val this) val))

  (method ^:public deref [this]
    val))
```

- Modifiers are metadata keywords. `val` has none, so it is a package-private field, as in Java.
- Untyped means `Object`. A type is a tag: `(field ^int n)`, `(method f ^String [this])`.
- Instance methods and constructors name their receiver as the first parameter, like `deftype`
  and `reify`. Own fields are in scope by name, like `deftype` fields.
- The method body's value is the return value. `return` exists for early exits.

## 3. Equivalence

The goal is classes *equivalent* to javac's. Comments, line numbers, local variable tables and
byte identity do not matter. Two sets of classes are equivalent when:

1. They hold the same classes, by binary name, synthetic ones included (`Outer$1`, the
   `$SwitchMap$` holders).
2. Each class has the same version, access flags, superclass, interfaces and these attributes:
   `Signature`, `InnerClasses`, `EnclosingMethod`, `NestHost`, `NestMembers`,
   `PermittedSubclasses`, `Record`, `Deprecated`, `Runtime(In)Visible(Type)Annotations`,
   `Module`. `SourceFile`, `SourceDebugExtension` and the `Code` sub-attributes for debugging
   (`LineNumberTable`, `LocalVariable(Type)Table`, `CharacterRangeTable`) are ignored.
   `StackMapTable`, `max_stack` and `max_locals` follow from the code.
3. Fields match by name and descriptor, with the same flags, `ConstantValue`, `Signature` and
   annotations.
4. Methods match by name and descriptor, with the same flags, `Exceptions`, `Signature`,
   annotations, parameter annotations, `AnnotationDefault` and `MethodParameters`.
5. Code behaves the same. The check is the same symbolic content per method: the member
   references with their owner, name, descriptor and invocation kind; the class references of
   `new`, `checkcast`, `instanceof` and `anewarray`; the `invokedynamic` call sites with their
   bootstrap methods and static arguments; the constants. Order and instruction choice may
   differ. Behaviour is checked by tests (Clojure's test suite for the converted baseline).
6. Member order does not matter. javac's few non-deterministic names (for example
   identity-hash lambda names like `$1522236047`, seen during the javalisp work) are exempt.

The class file version is the target release, by default the running JDK's (major 70 for JDK 26),
and configurable. The comparison compiles with javac at the same `--release`. Preview features
(class files with minor version `0xFFFF`) are out of scope.

## 4. Declarations

### 4.1 `defclass`

```
(defclass name record-components? option* member*)
```

- **Name.** A simple symbol names a class in the package of the current namespace, munged as
  `deftype` does (`my-app.core` gives package `my_app.core`). A dotted symbol is a full class
  name: `(defclass clojure.lang.Keyword ...)`. A class form nested in another class body names
  a member class, `Outer$Name`. The class name itself is not munged; characters the JVM forbids
  (`. ; [ / < >`) are an error.
- **Kind.** Metadata on the name selects the kind, which in the class file is just flags and
  attributes:

  | Java | metadata | class file |
  |---|---|---|
  | `class` | none | |
  | `interface` | `^:interface` | `ACC_INTERFACE`, `ACC_ABSTRACT` |
  | `enum` | `^:enum` | `ACC_ENUM`, superclass `java.lang.Enum<E>` |
  | `record` | `^:record` | superclass `java.lang.Record`, `Record` attribute |
  | `@interface` | `^:annotation` | `ACC_ANNOTATION`, `ACC_INTERFACE`, `ACC_ABSTRACT` |

- **Record components** follow the name as a vector, as in `defrecord`:
  `(defclass ^:record Point [^int x ^int y] ...)`.
- **Options** are keyword/value pairs before the first member:

  | option | Java | value |
  |---|---|---|
  | `:extends` | `extends` | a type; for an interface a vector of types |
  | `:implements` | `implements` | a vector of types |
  | `:permits` | `permits` | a vector of types |
  | `:type-params` | `<T, U extends B>` | a vector of type parameters (§4.3) |
  | `:package` | `package` | `""` for the unnamed package (dotted names give all others) |

- **Value.** The form defines the class (and everything nested in it), imports it into the
  namespace under its simple name, and returns the `Class`.

Member forms:

| member | form |
|---|---|
| field | `(field name init?)` |
| method | `(method name option* params body*)` |
| constructor | `(constructor option* params body*)` |
| instance initializer | `(initializer body*)` |
| static initializer | `(static-initializer body*)` |
| member class | `(defclass ...)` |
| enum constants | `(constants ...)`, in an enum only (§4.10) |
| several members | `(do member*)`, or a macro that expands to members (§4.15) |

Member heads are recognized by name inside a class body only. They are not vars, as method
specs in `deftype` are not. A method named `field` is `(method field ...)`.

### 4.2 Modifiers

Modifiers are keyword metadata on the declared name (class, field, method, parameter, local).
Constructors have no name, so theirs go on the parameter vector (§4.7).

| keyword | Java | applies to | flag |
|---|---|---|---|
| `:public` `:protected` `:private` | access | classes, members | `ACC_PUBLIC` etc.; none means package access |
| `:static` | `static` | members, member classes | `ACC_STATIC` |
| `:final` | `final` | classes, fields, methods, params, locals | `ACC_FINAL` (params: only in `MethodParameters`) |
| `:abstract` | `abstract` | classes, methods | `ACC_ABSTRACT` |
| `:synchronized` | `synchronized` | methods | `ACC_SYNCHRONIZED` |
| `:native` | `native` | methods | `ACC_NATIVE` |
| `:transient` `:volatile` | same | fields | `ACC_TRANSIENT`, `ACC_VOLATILE` |
| `:default` | `default` | interface methods | none; optional, a body already makes it a default method |
| `:sealed` `:non-sealed` | same | classes, interfaces | `PermittedSubclasses`; `non-sealed` has no trace |
| `:strictfp` | `strictfp` | | none, as javac since release 17 |
| `:deprecated` | javadoc `@deprecated` | all declarations | `Deprecated` attribute |
| `:synthetic` `:bridge` | none | members, classes | `ACC_SYNTHETIC`, `ACC_BRIDGE` (normally derived, §6) |
| `:interface` `:enum` `:record` `:annotation` | kind | classes | see §4.1 |

Further metadata used on locals and parameters: `:mutable` (assignable, §5.3), `:const`
(constant variable, §5.3) and `:compact` (compact canonical constructor, §4.11).

Java's implicit modifiers apply unchanged: interface methods are `public` (and `abstract` without
a body), interface fields `public static final`, member types of interfaces `public static`,
nested enums, records and interfaces `static`, enum constructors `private`, record classes and
enums without constant bodies `final`, record fields `private final`, and so on. `ACC_SUPER` is
always set on classes. A default constructor gets the class's access, as in Java.

Annotations are metadata too (§4.4). `^{Deprecated true}` is the annotation, `^:deprecated` the
javadoc tag. javac emits the `Deprecated` attribute for both.

### 4.3 Types

A type is written as data. In declaration positions it is the declared type: it fixes descriptors
and, when generic, the `Signature` attribute.

| Java | type form |
|---|---|
| `int`, `String`, `java.util.List` | `int`, `String`, `java.util.List` (symbols, resolved like class names) |
| `Map.Entry` | `Map$Entry` (binary name, as in Clojure) |
| `int[]`, `String[][]` | `int/1`, `String/2` (Clojure 1.12 array class symbols) |
| `T` (type variable) | `T`; `T/1` is `T[]` |
| `List<String>`, `Map<K, List<V>>` | `(List String)`, `(Map K (List V))` |
| `List<?>`, `? extends T`, `? super T` | `(List ?)`, `(? extends T)`, `(? super T)` |
| `List<String>[]` | `(array (List String))` |
| `Outer<T>.Inner<U>` | `(.. (Outer T) (Inner U))` |
| `A & B` | `(& A B)` (casts and bounds) |
| `@A String`, `String @A []` | `^{A true} String`, `^{A true} String/1` (§4.4) |

Type parameters are a vector: `[T (U extends Number) (V extends Object Comparable)]`, the last
being `<V extends Object & Comparable>`. A parameter symbol can carry annotations as metadata.

Where types are written:

| position | written as |
|---|---|
| field type | tag on the field name: `(field ^long n)` |
| parameter type | tag on the parameter: `[this ^int i]` |
| return type | tag on the parameter vector, as in `defn`: `(method f ^String [this])` |
| local type | tag on the binding: `(let [^int i 0] ...)` |
| supertypes, bounds, throws | `:extends`, `:implements`, `:type-params`, `:throws` |
| casts, `instanceof`, `new`, `catch` | the type operand of `cast`, `instance?`, `new`, `catch` |

A symbol tag is written the usual way, `^String x`. A generic type needs the map form,
`^{:tag (List String)} xs`, because `^` takes no list. A list tag is an error in Clojure today,
so this is a new meaning (§1.1).

A type is **erased** as in Java: type variables to their first bound, parameterized types to
their class. Code generation uses erased types only. A `Signature` attribute is emitted where
javac emits one: when a declared type, a supertype or a throws clause involves type arguments or
type variables, or a class or method declares type parameters.

In expression positions a tag stays a Clojure hint: `^String (f)` tells the compiler the static
type and checks nothing. A checked conversion is a `cast` (§5.5).

### 4.4 Annotations

Clojure's annotation syntax, which `deftype` and `gen-class` already accept, extended to every
place Java allows annotations:

| Java | metadata |
|---|---|
| `@Override`, `@Deprecated` | `^{Override true}`, `^{Deprecated true}` (marker: `true`) |
| `@SuppressWarnings("x")` | `^{SuppressWarnings "x"}` (the `value` element) |
| `@Target({TYPE, METHOD})` | `^{Target [ElementType/TYPE ElementType/METHOD]}` |
| `@A(x = 1, y = "s")` | `^{A {:x 1 :y "s"}}` |
| element values | constants, `String`, a class symbol (`String`, `int/1`), an enum constant (`ElementType/TYPE`), a nested annotation `(B {:k v})`, a vector for arrays |

- Keys are annotation class symbols, values the elements, so annotations mix freely with
  modifier keywords: `^:public ^{Deprecated true} name`.
- Retention follows the annotation type's `@Retention`: `SOURCE` is dropped, `CLASS` goes to
  `RuntimeInvisibleAnnotations`, `RUNTIME` to `RuntimeVisibleAnnotations`. (Clojure today emits
  `SOURCE` annotations as invisible ones. The converter can keep `@Override`; it is dropped and,
  as with javac, checked.)
- Element constants take the element's type: `^{A {:b 1}}` stores a `byte` if `b()` returns
  `byte`. A single value for an array element is wrapped, as in Java.
- **Declaration annotations** sit on the declared name: class, field, method, constructor (its
  metadata, §4.7), parameter, record component, enum constant, type parameter, module, package.
- **Type annotations** sit on type nodes (§4.3). On a declared name an annotation whose
  `@Target` includes `TYPE_USE` also annotates the declared type, as in Java. Type annotations
  inside a type go on the node they qualify: `^{:tag (List ^{NonNull true} String)}`. Those in
  code (local types, casts, `instance?`, `new`, catch parameters, method references) go to
  the `Code` attribute's type annotations, as javac's do.
- Annotations on record components propagate to the field, accessor and canonical constructor
  parameter by `@Target`, as javac does.

### 4.5 Fields

```clojure
(field ^:private ^:static ^:final ^long serialVersionUID -7896022351281214157)
(field ^:public ^:final ^{:tag (AtomicReference Thread)} edit)
(field ^:volatile ^int cnt)
```

- `(field name init?)`. One form per declarator: `int a, b;` is two forms.
- Static initializers run in textual order with the `static-initializer` forms, as `<clinit>`.
  Instance field initializers and `initializer` forms run in textual order in every constructor
  that does not delegate with `this.`, right after the superclass constructor call (§4.7).
- A final field whose initializer is a constant expression of primitive or `String` type gets a
  `ConstantValue` attribute. Reads of static constant fields are inlined, as in javac (§5.6).
- In a class body the class's own fields, and those of enclosing class bodies, are in scope by
  name, like `deftype` fields. Instance fields are in scope in instance code only. Inherited
  fields are not; they are read as `(.-f this)` or `Super/F`. Locals and parameters shadow fields.
- `(set! f v)` assigns an own field by name, `(set! (.-f x) v)` any instance field and
  `(set! C/f v)` a static one. Final fields can be assigned only in constructors and
  initializers (static ones in static initializers).

### 4.6 Methods

```
(method name option* params body*)
```

```clojure
(method ^:public ^:static intern ^Keyword [^Symbol sym] ...)
(method ^:public ^:final invoke [this obj notFound] ...)
(method ^:public ^:static ^{SafeVarargs true} of
  :type-params [E]
  ^{:tag (List E)} [& ^E/1 xs] ...)
(method ^:private readResolve :throws [ObjectStreamException] [this] ...)
```

- **Parameters.** A vector of symbols with tags and modifiers. An instance method's first
  parameter is its receiver, conventionally `this`, as in `deftype`; a static method has none.
  `& ^T/1 xs` makes the method variable-arity (`ACC_VARARGS`); its type must be an array.
  A receiver parameter's annotations (`void f(@A C this)`) go on the receiver symbol.
- **Return type** is the tag on the parameter vector; `^void` for none.
- **Untyped** parameters and return mean `Object`, with one exception taken from `deftype`: if a
  method has no type at all and its supertypes declare exactly one method with its name and
  arity, it takes that method's erased signature. So `(method run [this] ...)` implements
  `Runnable.run()`. A single tag anywhere turns the inference off.
- **Options**: `:type-params [T ...]`, `:throws [E ...]` (the `Exceptions` attribute; no checked
  exception analysis is done), `:default v` (an annotation element's default, §4.12).
- **Body.** The value of the last form is returned, converted to the return type as by
  assignment (§5.5). In a `^void` method it is discarded. A method without body forms is
  abstract if marked `^:abstract` (or in an interface without `^:static`, `^:private` or
  `^:default`), native if `^:native`, and otherwise has an empty body.
- **Overloads** are separate `method` forms with the same name. Names are used verbatim, not
  munged.

### 4.7 Constructors and initializers

```clojure
(constructor ^:public [this ^int cnt ^int shift ^Node root ^Object/1 tail]
  (super.)                      ; optional: the implicit call, as in Java
  (set! (.-cnt this) cnt) ...)

(constructor [this ^PersistentVector v]
  (this. (.-cnt v) (.-shift v) (TransientVector/editableRoot (.-root v))
         (TransientVector/editableTail (.-tail v)) (.-_meta v)))
```

- Modifiers and annotations are metadata on the parameter vector, which has no return type to
  carry: `(constructor ^:private [this ^Symbol sym] ...)`. Options (`:throws`, `:type-params`)
  come before the vector.
- `(super. args)` calls a superclass constructor, `(this. args)` another constructor of the
  class, mirroring `(Foo. args)`. Without either, `(super.)` is implied. As in Java 25
  (flexible constructor bodies), forms before the call may compute arguments and assign
  fields, but not read `this`.
- `(.super o args)` is Java's `o.super(args)`: a superclass constructor call with an explicit
  outer instance, for a superclass that is an inner class.
- Field initializers and `initializer` bodies are folded into each constructor that calls
  `super.`, after the call, in textual order. `static-initializer` bodies and static field
  initializers make up `<clinit>`.
- Without constructors, the class gets Java's default constructor. An anonymous class's
  constructor passes its arguments to the superclass constructor (§4.8).
- Enum constructors get the name and ordinal parameters prepended, record and inner class
  constructors what §4.10, §4.11 and §4.8 describe. The forms show only the declared
  parameters.

### 4.8 Nested, inner, local and anonymous classes

**Member classes** are class forms in a class body. Without `^:static` a member class is an
inner class, as in Java.

```clojure
(defclass ^:public PersistentVector ...
  (defclass ^:public ^:static Node ...)               ; PersistentVector$Node
  (defclass ^:static ^:final TransientVector ...))    ; PersistentVector$TransientVector
```

Member classes of the class and of enclosing classes are in scope by simple name (`Node`).
Member types inherited from supertypes are not; they are written by binary name or imported.

**The enclosing instance.** Inside an inner, local or anonymous class, `Outer/this` is Java's
`Outer.this` and `Outer/super` Java's `Outer.super` (or `Iface.super` for a direct
superinterface's default method). Fields of enclosing classes are in scope by name and read
through the enclosing instance. A local or anonymous class in an instance method can also
simply use the method's receiver parameter if it has another name, since it is a captured local.

```clojure
(.arrayFor PersistentVector/this i)    ; arrayFor(i) called from an anonymous Iterator
(.toString Outer/super)                ; Outer.super.toString()
(.m Iface/super x)                     ; Iface.super.m(x)
```

**Creating inner class instances.** `(Inner. args)` in an instance context of the outer class
passes `this` (or the right enclosing instance) implicitly. `(.new o Inner args)` is Java's
`o.new Inner(args)`.

**Anonymous classes** are expressions:

```
(anon Super [ctor-args*] member*)
```

```clojure
(anon AFn []
  (method ^:public invoke [this coll val]
    (.conj (cast ITransientVector coll) val)))
```

`Super` is one class or interface, possibly generic: `(anon (Comparator String) [] ...)`. The
arguments go to the superclass constructor. Members are any members but constructors.

**Local classes** are bound like local functions in `letfn`:

```
(letclass [(name record-components? option* member*) ...] body*)
```

```clojure
(letclass [(^:record Pair [a b])
           (Walker :implements [Runnable] (method ^:public run ^void [this] ...))]
  ...)
```

The classes are in scope in the body and in each other's members (Java allows a local class to
refer to itself and earlier ones; the converter puts each Java local class into a `letclass`
that covers the rest of its block).

**Captures.** Local and anonymous classes, and lambdas, capture the locals they use, which must
not be `^:mutable` (Java's effectively final). As javac does, the compiler stores captured
locals in `val$x` fields and the enclosing instance in `this$0`, passes them to the constructor
(storing them before the superclass constructor call), and leaves out `this$0` when the class
does not use it (in static contexts there is none).

**Names and attributes** follow javac: `Outer$Inner`, `Outer$1` for anonymous classes and
`Outer$1Local` for local ones, numbered in javac's order. The compiler emits `InnerClasses`
(for every member, local or anonymous class the class file refers to, also those of other
classes, such as `java/util/Map$Entry`), `EnclosingMethod` for local and anonymous classes,
and `NestHost` and `NestMembers` (the top-level class lists every class nested in it at any
depth). Access to private members of nestmates is direct. The inner class flags in
`InnerClasses` keep `private`, `protected` and `static`; the class's own flags have them
mapped as javac does (`protected` to `ACC_PUBLIC`, `private` to package access).

### 4.9 Interfaces

```clojure
(defclass ^:public ^:interface Associative
  :extends [IPersistentCollection ILookup]
  (method containsKey ^boolean [this key])
  (method entryAt ^IMapEntry [this key])
  (method assoc ^Associative [this key val]))
```

Abstract, default (with a body), static and private methods, and constant fields (implicitly
`public static final`) are written as in classes. A default method calls a superinterface's
default with `(.m Iface/super ...)`.

### 4.10 Enums

```clojure
(defclass ^:public ^:static ^:enum Category
  (constants INTEGER FLOATING DECIMAL RATIO))
```

```
(constants constant*)          constant = NAME | (NAME [args*] member*)
```

- `(constants ...)` is the first member. Each constant is a name, or a list with constructor
  arguments and optionally a body. A body makes an anonymous subclass `E$1` as in Java.
  Annotations go on the name.
- The compiler derives what javac derives: the `public static final` constant fields, the
  private `$VALUES` field and `$values()` method, `values()` and `valueOf(String)` (its parameter
  `MANDATED`, hence a `MethodParameters` attribute), the name and ordinal constructor
  parameters, the `super(name, ordinal)` call, `ACC_ENUM`, `ACC_FINAL` (unless a constant has a
  body; `ACC_ABSTRACT` if it declares abstract methods), and the `Enum<E>` signature.

```clojure
(defclass ^:enum Op
  (constants (PLUS ["+"] (method apply ^int [this ^int a ^int b] (unchecked-add-int a b)))
             (TIMES ["*"] (method apply ^int [this ^int a ^int b] (unchecked-multiply-int a b))))
  (field ^:private ^:final ^String sym)
  (constructor [this ^String sym] (set! (.-sym this) sym))
  (method ^:abstract apply ^int [this ^int a ^int b]))
```

### 4.11 Records

```clojure
(defclass ^:public ^:record Range [^int lo ^int hi]
  :implements [Comparable]
  (constructor ^:public ^:compact [this]
    (when (> lo hi) (throw (IllegalArgumentException. "lo > hi"))))
  (method ^:public compareTo ^int [this ^Object o]
    (Integer/compare lo (.lo (cast Range o)))))
```

- The components vector gives the `Record` attribute and, unless written, the private final
  fields, the accessors and the canonical constructor (whose parameter names give a
  `MethodParameters` attribute, as javac emits for it). `toString`, `hashCode` and `equals`,
  unless written, are derived with an `invokedynamic` to `ObjectMethods.bootstrap`, as javac does.
- A compact canonical constructor is `(constructor ^:compact [this] body*)`: the components are
  in scope as assignable locals, and the fields are assigned from them at the end.
- Generic records take `:type-params`; component types may be generic.

### 4.12 Annotation types

```clojure
(defclass ^:public ^:annotation ^{Retention RetentionPolicy/RUNTIME
                                  Target [ElementType/METHOD]}
  Todo
  (method value ^String [this])
  (method priority :default 1 ^int [this]))
```

Element methods are abstract instance methods. `:default` gives the `AnnotationDefault`
attribute, written like an element value (§4.4).

### 4.13 Sealed classes

`^:sealed` with `:permits [A B]` gives `PermittedSubclasses`. As in Java, without `:permits` the
permitted subclasses are those declared in the same top-level class form or source file.
`^:non-sealed` is accepted and has no trace in the class file.

### 4.14 Modules and packages

```clojure
(defmodule ^:open ^{Deprecated true} com.example.app
  (requires ^:transitive java.logging)
  (requires ^:static java.desktop)
  (exports com.example.api)
  (exports com.example.spi :to [com.example.plugins])
  (opens com.example.model)
  (uses com.example.spi.Plugin)
  (provides com.example.spi.Plugin :with [com.example.impl.DefaultPlugin]))

(defpackage ^{Deprecated true} com.example.old)
```

`defmodule` writes `module-info.class` (`ACC_MODULE`, the `Module` attribute; `java.base` is
required implicitly and marked `MANDATED`, and the versions of required system modules are
recorded as javac does). `defpackage` writes `package-info.class` for package annotations, a
synthetic interface as javac makes it. Both only make sense when compiling to files.

### 4.15 Member macros

A member form whose head is not a member head and resolves to a macro is macroexpanded, and
`(do member*)` splices. So repetitive members can be generated:

```clojure
(defmacro ^:private throw-arity-invokes [from to]
  (cons 'do
        (for [n (range from (inc to))]
          (list 'method (with-meta 'invoke {:public true})
                (into '[this] (map #(symbol (str "arg" %)) (range 1 (inc n))))
                (list '.throwArity 'this n)))))
```

`(throw-arity-invokes 3 20)` in `Keyword`'s body stands for its eighteen `invoke` methods.

The converter writes members one by one; macros are for people.

## 5. Code

Method bodies, initializers and field initializers are Clojure code. This section says how Java's
statements and expressions are written, what is new, and how existing forms behave in class
bodies.

### 5.1 Overview

| Java | Clojure |
|---|---|
| `{ s1; s2; }` | `(do s1 s2)`, or just the forms of a body |
| `T x = e;` and the rest of the block | `(let [^T x e] rest...)` |
| a local assigned later | `(let [^:mutable ^T x e] ...)`, `(set! x v)` |
| `final T x = CONSTANT;` | `(let [^:const ^T x CONSTANT] ...)` |
| `x = e`, `x += e` (int) | `(set! x e)`, `(set! x (unchecked-add-int x e))` |
| `++i`, `i++` as a statement | `(set! i (unchecked-inc-int i))` |
| `if`, `c ? a : b` | `if`, `when`, `cond`, `if-not`, `when-not` |
| `while (c) s` | `(while c s)` |
| `for (int i = 0; i < n; i++) s` | `(loop [^int i 0] (when (< i n) s (recur (unchecked-inc-int i))))` |
| `do s while (c);` | `(loop [] s (when c (recur)))` |
| `for (T x : xs) s` | `(for-each [^T x xs] s)` |
| `L: s`, `break`, `continue`, `return` | `(label :L s)`, `(break)`, `(continue)`, `(return e)` |
| `switch`, `yield`, patterns | `(switch e ...)`, `(break :L v)`, `(if-instance [p e] ...)` |
| `throw`, `try`/`catch`/`finally` | `(throw e)`, `(try ... (catch E e ...) (finally ...))` |
| `catch (A \| B e)` | `(catch [A B] e ...)` |
| `try (R r = e) { ... }` | `(with-resources [^R r e] ...)` |
| `synchronized (x) { ... }` | `(locking x ...)` |
| `assert c : m;` | `(java-assert c m)` |
| a local class | `(letclass [(Local ...)] ...)` |
| `this`, `Outer.this`, `super` | the receiver parameter, `Outer/this`, `super` |
| `x.f`, `C.f`, `super.f`, own field `f` | `(.-f x)`, `C/f`, `(.-f super)`, `f` |
| `x.m(a)`, `C.m(a)`, `super.m(a)` | `(.m x a)`, `(C/m a)`, `(.m super a)` |
| `new C(a)`, `o.new I(a)` | `(C. a)` or `(new C a)`, `(.new o I a)` |
| `new C(a) { ... }` | `(anon C [a] ...)` |
| `new int[n]`, `new int[n][]`, `new int[n][m]` | `(new int/1 n)`, `(new int/2 n)`, `(new int/2 n m)` |
| `new int[] {1, 2}`, `new int[][] {{1}, {2}}` | `(new int/1 [1 2])`, `(new int/2 [[1] [2]])` |
| `a[i]`, `a[i][j]`, `a[i] = v`, `a.length` | `(aget a i)`, `(aget a i j)`, `(aset a i v)`, `(alength a)` |
| `(T) e` | `(cast T e)`; primitive casts `(unchecked-int e)` etc. (§5.5) |
| `e instanceof T` | `(instance? T e)` |
| `a == b` on references, `a == null` | `(identical? a b)`, `(nil? a)`; `!= null`: `(some? a)` |
| `a == b` on numbers, on booleans | `(== a b)`, `(= a b)` |
| `!c`, `a && b`, `a \|\| b` | `(not c)`, `(and a b)`, `(or a b)` |
| arithmetic, bit operations | typed operators (§5.4) |
| `"a" + b` | `(java-str "a" b)` |
| `x -> e`, `C::m` | `(lambda FI [x] e)`, `(method-ref FI C/m)` |
| `T.class`, `int.class`, `T[].class` | `T`, `Integer/TYPE`, `T/1` |
| literals | Clojure literals (§5.4) |

Java's parentheses have no counterpart; the nesting of forms is the grouping.

### 5.2 Names in class bodies

An unqualified symbol in a class body resolves, in order, to:

1. a local or parameter (the receiver parameter included),
2. a field of the class or of an enclosing class body, innermost first (§4.5, §4.8),
3. what the namespace maps it to (vars, imported classes), as Clojure does,
4. a class in the class's own package, as Java sees same-package classes without import
   (a new fallback; today the symbol is an error),
5. `java.lang`, which Clojure imports anyway.

Type names (§4.3) resolve to member classes of the class and of enclosing class bodies first,
then as 3 to 5.

`this` is no keyword: it is the receiver parameter's name. `super` is a reserved name in instance
code: `(.m super args)` calls the superclass's `m` non-virtually (`invokespecial`) and
`(.-f super)` reads a field as a member of the superclass. In a class body `C/this` and `C/super`
are Java's `C.this` and `C.super` (§4.8); a class has no static member named `this` or `super`,
so these symbols mean nothing today. `(super. ...)` and `(this. ...)` are constructor calls
(§4.7).

Methods are never in scope by name. Java's `m(x)` is `(.m this x)` for an instance method,
`(C/m x)` for a static one and `(.m Outer/this x)` for an outer instance's.

### 5.3 Locals

- `let` binds immutable locals as always. Java's effectively final locals are `let` locals,
  declared where Java declares them, scoping over the rest of the block.
- **Mutable locals**: `(let [^:mutable x e] ...)` binds an assignable local; `(set! x v)` assigns
  it and returns `v`. Parameters can be `^:mutable` too. A mutable local cannot be captured by a
  `fn`, `lambda`, local or anonymous class (as Java requires effectively final captures).
  Java's locals declared without initializer get one: the converter restructures
  (`(let [x (if c 1 2)] ...)`) where it can, and otherwise binds a default value
  (`0`, `false`, `nil`) to a mutable local.
- **Exact primitive types.** A primitive tag on a `let` or `loop` binding fixes the local's
  type: `(let [^int i 0] ...)`, `(loop [^byte b (aget bs 0)] ...)`. All of `int`, `short`,
  `byte`, `char`, `float`, `boolean`, `long` and `double` work. Today such a tag is an error
  ("Can't type hint a local with a primitive initializer"), and `loop` widens untagged `int`
  and `float` locals to `long` and `double`. Untagged locals keep Clojure's behaviour; a tagged
  one is never widened. The initializer must convert to the tag's type by an implicit conversion
  (§5.5).
- A mutable local without tag has its initializer's static type, like Java's `var`.
- **Constant variables.** `^:const` marks a local as Java's `final` local with a constant
  initializer: its uses are constant expressions (folded into `java-str`, usable as `switch`
  labels), exactly as for a `final` local in Java. (`^:const` means the same for vars in Clojure.)
- `^:final` on a local or parameter is accepted and has no effect except for parameters in
  `MethodParameters`. The unnamed variable `_` is a plain Clojure symbol.
- In class bodies the compiler does not clear locals (Clojure nulls dead locals in `fn` bodies).
  Java keeps locals alive to the end of their scope, which `WeakReference` code like `Keyword`
  may rely on.

### 5.4 Literals and primitive arithmetic

Java's operators are written with Clojure's unchecked and bit operators, completed where Clojure
lacks them. `+`, `-`, `*`, `/`, `inc`, `quot`, `rem` keep their Clojure meaning and do not appear
in converted code for Java arithmetic.

| Java | `int` (and `byte`, `short`, `char` operands) | `long` | `float` | `double` |
|---|---|---|---|---|
| `a + b` | `unchecked-add-int` | `unchecked-add` | `unchecked-add-float`* | `unchecked-add` |
| `a - b` | `unchecked-subtract-int` | `unchecked-subtract` | `unchecked-subtract-float`* | `unchecked-subtract` |
| `a * b` | `unchecked-multiply-int` | `unchecked-multiply` | `unchecked-multiply-float`* | `unchecked-multiply` |
| `a / b` | `unchecked-divide-int` | `unchecked-divide`* | `unchecked-divide-float`* | `unchecked-divide`* |
| `a % b` | `unchecked-remainder-int` | `unchecked-remainder`* | `unchecked-remainder-float`* | `unchecked-remainder`* |
| `-a` | `unchecked-negate-int` | `unchecked-negate` | `unchecked-negate-float`* | `unchecked-negate` |
| `++`, `--` (the arithmetic) | `unchecked-inc-int`, `unchecked-dec-int` | `unchecked-inc`, `unchecked-dec` | `unchecked-add-float` with 1 | `unchecked-inc`, `unchecked-dec` |
| `a & b`, `a \| b`, `a ^ b` | `bit-and-int`*, `bit-or-int`*, `bit-xor-int`* | `bit-and`, `bit-or`, `bit-xor` | | |
| `~a` | `bit-not-int`* | `bit-not` | | |
| `a << n`, `a >> n`, `a >>> n` | `bit-shift-left-int`*, `bit-shift-right-int`*, `unsigned-bit-shift-right-int`* | `bit-shift-left`, `bit-shift-right`, `unsigned-bit-shift-right` | | |
| `<`, `<=`, `>`, `>=`, `==` | `<`, `<=`, `>`, `>=`, `==` | same | same | same |
| `a != b` | `(not (== a b))` | same | same | same |

\* new in `arbace.core`. Notes:

- Each operator compiles to the JVM instruction (`iadd`, `ladd`, `fadd`, `idiv`, `drem`, `lcmp`
  and so on), in value and in test position, without calls. Clojure already does this for most
  of the existing ones through its intrinsics; the rest (`aset`, comparisons as values, `not`,
  `nil?`, `some?`, the conversions) are added.
- Operands are promoted as Java promotes them: an `int` operand of a `long` operator is widened,
  a `char`, `short` or `byte` operand of an `int` operator is used as `int`. Primitive `char`
  operands count as numbers in these operators and in comparisons (today a `ClassCastException`).
- The comparisons keep their Clojure meaning, which on primitives is Java's, NaN included,
  with one exception: Clojure compares an `int` or `long` with a `float` as `double`, Java as
  `float`. The converter makes that promotion explicit: `(< (unchecked-float i) f)`.
- `unchecked-divide` and `unchecked-remainder` on `long` are `ldiv` and `lrem` (Clojure's `/` on
  longs makes ratios); on `double` they are `ddiv` and `drem` (Clojure's `rem` on doubles is not
  IEEE remainder).
- Java's boolean `&`, `|` and `^` (both operands evaluated) are rare; the converter writes
  `(let [a x b y] (and a b))`, `(or ...)` and `(not (= a b))`.
- Compound assignments and increments are `set!` of the operation, with Java's implicit narrowing
  written out: `b += 1` on a `byte` is `(set! b (unchecked-byte (unchecked-add-int b 1)))`.
  Where Java evaluates a subexpression once (`a[i++] += v`), the converter binds temporaries.

**Literals.** Clojure's literals are used: `5`, `0x1f`, `017`, `2r101`, `1.5`, `1e10`, `\a`,
`é`, `"s"`, `true`, `nil`. Java's `5L` is `5`; `1.5f` is `(float 1.5)`; `'\uD800'` is
`(unchecked-char 0xD800)`. An integer literal is a `long` in Clojure; it is narrowed at compile
time where Java's context wants an `int`, `short`, `byte` or `char` and the value fits (a typed
local, field, parameter, return, array element, operand of an `int` operator, a branch whose
other branch is `int`). Java's hexadecimal, octal and binary `int` literals above `0x7fffffff` denote
negative numbers; in Clojure they are large `long`s, so `0x9e3779b9` is written
`(unchecked-int 0x9e3779b9)`, a constant expression. The same goes for `long` literals above
`0x7fffffffffffffff`.

**Constant expressions.** The compiler folds Java's constant expressions (JLS 15.29) built from
literals, constant variables, constant static fields, the operators above, the primitive casts
(`unchecked-int`, `long`, `float` and so on), `java-str`, `if` with constant test and `and`/`or`.
Folding gives `ConstantValue` attributes, `switch` labels, inlined constant fields, and javac's
removal of code under a constant `false` test (`(when DEBUG ...)`).

### 5.5 Conversions

Java converts implicitly in assignment, invocation, return and numeric contexts. The forms do so
where Arbace converts exactly as javac does, and are explicit elsewhere.

| conversion | written | compiled as |
|---|---|---|
| identity, widening reference | implicit | nothing |
| widening primitive (`int` → `long`, `char` → `int`, `long` → `float` ...) | implicit | `i2l`, `l2f` ... |
| constant narrowing (`byte b = 5`) | implicit | the narrowed constant |
| boxing to the matching wrapper (`int` → `Integer`, `Object`) | implicit | `Integer.valueOf(int)` etc. |
| unboxing from a wrapper's static type (`Integer` → `int`, `long`) | implicit | `Integer.intValue()` (then widening) |
| narrowing primitive | `(unchecked-int x)`, `unchecked-long`, `-short`, `-byte`, `-char`, `-float`, `-double` | `l2i`, `d2l`, `i2b` ... |
| checked reference cast | `(cast T x)` | `checkcast T` |
| unboxing from a non-wrapper type | `(.intValue (cast Integer o))` | `checkcast`, `intValue()` |
| intersection cast | `(cast (& A B) x)` | `checkcast A`, `checkcast B` |

- `cast` with a class literal compiles to `checkcast` and has that class as its static type.
  Clojure's `cast` calls `Class.cast`; the effect is the same but for the exception message.
- Boxing calls `valueOf` of the matching wrapper, as javac does. Clojure today boxes a `long` with
  `Numbers.num`, which returns the same `Long`; only the call changes, not the meaning.
- **Erasure casts.** javac inserts a `checkcast` where an erased generic type is used at a more
  specific type (`String s = list.get(0)`). Arbace does not know generic types in code, so the
  converter writes every such cast: `(cast String (.get list 0))`. Where Java uses the value
  without a cast (`list.get(0).hashCode()` on `Object`'s method), javac inserts none and neither
  does the converter. `for-each` is the exception: its typed binding casts as javac does (§5.7).
- **Conditions.** `if`, `when`, `and`, `or` and `cond` test Clojure truth. On a primitive
  `boolean` that is Java's test. A `Boolean` test is written `(.booleanValue b)`, which also
  gives Java's `NullPointerException`.
- **Branches.** Java types `c ? a : b` by its own rules (numeric promotion, boxing, `lub`). The
  converter writes the conversions so both branches of an `if` (and all arms of a `switch` or
  `cond`) have the type javac gave the whole: `(if c (long i) 2)`, `(if c (Integer/valueOf 1) nil)`.

### 5.6 Members: access, overloads, constants

- **Qualifying type.** A field access or method call names, in the class file, the static type
  of its receiver (or the class written in `C/m`, `C/f`), as JLS 13.1 prescribes, not the class
  that declares the member. An interface type gives `invokeinterface`. Clojure today names the
  declaring class.
- **Accessibility.** In a class body, members are looked up with Java's access rules from the
  class being compiled: package-private members of the same package, protected ones of
  superclasses, private ones of the nest. Clojure today sees public members only. Where javac
  needs an accessor (a protected member of an outer class's superclass in another package,
  reached from an inner class), the compiler makes javac's `access$NNN` method.
- **Overload resolution** is Clojure's: candidates by name and arity, matched against the
  arguments' static types, the most specific one wins; ties are an error in class bodies
  (Clojure would fall back to reflection). A variable-arity method is chosen, with the extra
  arguments packed into an array as Java does, only if no fixed-arity method matches (new;
  today an error). Where Clojure's choice would differ from javac's, the converter pins the
  method with Clojure 1.12 param-tags: `(^[Object] String/valueOf x)`,
  `(^[int] StringBuilder/.append sb c)`. The converter runs the compiler's own resolution to know
  where this is needed.
- **Reflection** is an error in class bodies (§12, question 7).
- **Constant fields.** Reading a static final field with a constant initializer is replaced by
  the constant, as in javac (it does not initialize the field's class). For classes loaded
  from class files the compiler reads their `ConstantValue` attributes, which reflection cannot
  see.
- **Arrays.** `(.clone a)` on an array type is the public array `clone()` returning the array
  type, as in Java (JLS 10.7). `getClass()` has its usual erased type `Class`.
- **Null checks.** Where javac checks a receiver with `Objects.requireNonNull` (a qualified
  `.new`, a bound method reference), so does the compiler.

### 5.7 Control flow

`if`, `when`, `cond`, `case`, `do`, `let`, `loop`/`recur`, `while`, `dotimes` and the rest keep
their Clojure meaning. New:

```
(label :L form*)             ; a named statement; its value is the last form's
(break)  (break :L)  (break :L value)
(continue arg*)  (continue :L arg*)
(return)  (return value)
(for-each [^T x coll] body*)
```

- **Labels are keywords**, so they cannot clash with locals. `label` wraps any forms: a loop it
  wraps becomes a target for `continue`, and any labeled form is a target for `break`.
- `(break)` leaves the innermost `loop` (which includes `while`, `dotimes`, `for-each`) with
  value `nil`. `(break :L v)` leaves the labeled form with value `v`; that is Java's `yield` from
  inside a switch arm, and `break L` from a labeled block.
- `(continue args)` jumps to the head of the innermost loop with new values for its bindings:
  it is `recur` from any position in the loop body, not only the tail, and also across `try`,
  `locking` and inner loops (where `recur` is an error today). `(continue :L args)` does that
  for an outer labeled loop. `while` and `for-each` loops have no bindings to give: `(continue)`. Java's
  `continue` in a `for` loop repeats the update: `(continue (unchecked-inc-int i))`.
- `(return v)` returns from the enclosing method or `lambda` (never across a `fn`).
- Jumps out of `try` run `finally` blocks and leave `locking` monitors, as in Java.
- **Forms that do not complete** (`throw`, `return`, `break`, `continue`, `recur`, a `loop` whose
  every path jumps, `(while true ...)` without `break`, an `if` both of whose branches do not
  complete) have no type, so `(if c (return 1) 2)` and a method ending in an endless loop are
  well typed.
- **Converter style.** The converter prefers restructuring: a `return` at the end of a branch is
  just the branch's value, `if (c) return a; return b;` is `(if c a b)`, a guard
  `if (x) { ...; continue; }` in a loop is an `if` whose branches both `recur`, and a `for` loop
  is `loop`/`recur` (or `let` with mutable locals and `while`, when its variables outlive it).
  `label`, `break`, `continue` and `return` remain for the rest.
- `do`-`while`: `(loop [] body (when c (recur)))`; a `continue` in it goes to the test, so the
  body is wrapped: `(loop [] (label :body ... (break :body) ...) (when c (recur)))`.
- **`for-each`** is Java's enhanced `for`. On an array it loops over the array with an index; on
  an `Iterable` it calls `iterator()`, `hasNext()` and `next()`, casting each element to the
  binding's type if that is not `Object`, as javac does. (`doseq` stays Clojure's seq loop.)
- In class bodies `loop` and `try` in expression position compile inline. (Clojure wraps them in
  a `fn` there; Java code has no such classes.)

### 5.8 `switch` and patterns

```
(switch expr clause* default?)

clause  = label result
label   = constant | (constant+) | [pattern] | [pattern :when guard] | nil | (nil :default)
```

```clojure
(switch (.ordinal day) 0 "Sun" (1 2 3 4 5) "work" "Sat")    ; int
(switch s "a" 1 "b" 2 0)                                    ; String
(switch color RED 1 (GREEN BLUE) 2 0)                       ; enum: bare constant names
(switch shape
  [^Circle c] (.r c)
  [(Square ^double side)] side
  [^Object o :when (some? o)] 0.0
  nil -1.0)
```

- Like `case`: pairs of label and result, multiple constants in a list, a trailing default.
  Unlike `case`, labels are Java's: constant expressions (literals, constant fields like
  `Integer/MAX_VALUE`, `^:const` locals), enum constant names of the selector's enum type, `nil`
  for `case null`, and patterns.
- **Patterns** are vectors: `^T x` is a type pattern, `_` (alone or `^T _`) an unnamed one,
  `(R p*)` a record pattern with nested patterns, and an untyped symbol in a record pattern is
  Java's `var`. `:when` adds a guard.
- Without a default nothing happens when nothing matches, and the value is `nil`. Where Java
  requires exhaustiveness (switch expressions, pattern switches), javac adds a default that
  throws `MatchException`; the converter writes it:
  `(throw (MatchException. nil nil))`.
- No fall-through: each arm is complete. The converter repeats shared code of falling-through
  Java arms (§12, question 9).
- A `switch` is an expression; Java's `yield` from a nested statement is `(break :L v)` with the
  `switch` inside `(label :L ...)`.
- The compiler translates as javac does: `tableswitch` or `lookupswitch` on `int`-like
  selectors; `hashCode` and `equals` on strings; an enum's ordinal, through javac's
  `$SwitchMap$` holder class when the enum is not compiled in the same compilation (§6);
  `SwitchBootstraps.typeSwitch` and `enumSwitch` with restart indexes for pattern and `null`
  switches; record components read through accessors whose exceptions become
  `MatchException`.

`case` keeps Clojure's semantics. It differs from Java's switch on strings (no
`NullPointerException`), on enum constants (symbols are symbols) and in needing the runtime for
hashing, which is why `switch` is a new form.

**Pattern tests.** Java's `x instanceof P` with a pattern, and its flow scoping, become

```
(if-instance [pattern expr] then else?)
(when-instance [pattern expr] body*)
```

```clojure
(if-instance [^String s o] (.length s) 0)
(when-instance [(Point x y) p] (unchecked-add-int x y))
```

The bindings are in scope in `then` only. `if (!(o instanceof String s)) return; ...` becomes
`(if-instance [^String s o] (do ...) (return))`.

### 5.9 Exceptions and monitors

- `throw`, `try`, `catch` and `finally` are Clojure's and behave as Java's.
  `(catch [A B] e ...)` is a multi-catch; `e` has the classes' least upper bound as its type.
- **try-with-resources**:

  ```
  (with-resources [^R r init ...] body*)
  ```

  binds the resources in order and closes them in reverse order with Java's semantics: a
  resource that is `nil` is not closed, an exception from `close` while the body's exception
  propagates is added to it with `addSuppressed`. A Java `try` with resources and `catch` or
  `finally` clauses is a `try` around a `with-resources`, which is javac's desugaring too.
  Java's `try (r)` on an existing variable binds it again: `(with-resources [r r] ...)`.
  (`with-open` stays as it is: it lets `close`'s exception replace the body's.)
- `(locking x ...)` is `synchronized`; the compiler emits javac's `monitorenter`/`monitorexit`
  pattern. `^:synchronized` methods get `ACC_SYNCHRONIZED`.
- `(java-assert c)` and `(java-assert c message)` are Java's `assert`: the compiler adds the
  static `$assertionsDisabled` field, initialized from `desiredAssertionStatus()` of the
  outermost class, and throws `AssertionError` (with the constructor overload matching the
  message's type). Clojure's `assert` stays as it is (checked at compile time against `*assert*`).
- Checked exceptions are not analyzed; `:throws` only writes the `Exceptions` attribute.

### 5.10 Strings

`(java-str a b ...)` is Java's string concatenation: each operand is converted by its static
type as Java does (`null` becomes `"null"`, a `char` its character, an `int` its digits), and the
compiler emits javac's `invokedynamic` to `StringConcatFactory.makeConcatWithConstants`, with
constant operands folded into the recipe. Constant operands only give a constant. `s += x` is
`(set! s (java-str s x))`. (`str` stays Clojure's: `nil` gives `""`.)

### 5.11 Arrays

- Types: `int/1`, `String/2` and so on.
- Creation: `(new T/n dim+)` allocates (`newarray`, `anewarray`, `multianewarray`); fewer
  dimension expressions than `n` leave the inner arrays `null`, like `new int[n][]`. `(new T/n [x
  ...])` creates from an initializer, nested vectors for nested arrays. Today `new` of an array
  class is an error.
- `aget`, `aset` and `alength` on statically typed arrays compile to the array instructions,
  multi-index `aget` included. `aset` takes Java's element types: storing an `int` into a
  `byte[]` needs `(unchecked-byte v)`. Array stores keep the JVM's `ArrayStoreException` check.
- Clojure's `int-array`, `make-array`, `into-array` and so on keep their meaning; the converter
  does not use them.

### 5.12 Lambdas and method references

```
(lambda FI params body*)
(method-ref FI sig? receiver? method)
```

```clojure
(lambda Runnable [] (.run task))
(lambda Function ^Integer [^String s] (Integer/valueOf (.length s)))
(lambda (& Runnable Serializable) [] (.close c))
(method-ref Function [String] Integer/valueOf)          ; Integer::valueOf, a static method
(method-ref ToIntFunction [String] String/.length)      ; String::length, unbound receiver
(method-ref Supplier [] sb StringBuilder/.toString)    ; sb::toString, bound receiver
```

- `FI` is the functional interface type: a class, or `(& I M...)` for an intersection target
  with marker interfaces (`Serializable` gives a serializable lambda).
- **Lambdas.** The parameters' and the vector's tags give the instantiated method type: the
  erased types of Java's instantiated function type (`String`, `Integer` for a
  `Function<String, Integer>`). Untagged, they default to the erased SAM method's types. The body
  is compiled into a private synthetic method `lambda$<method>$<n>` (static unless it uses
  `this`), captures become its leading parameters, and an `invokedynamic` to
  `LambdaMetafactory.metafactory` (or `altMetafactory` for serializable lambdas, marker interfaces
  and bridges, with a `$deserializeLambda$` method for serializable ones) creates the instance.
  Inside, `this` is still the enclosing method's receiver, as in Java, and `(return v)`
  returns from the lambda.
- **Method references.** `sig` is a vector of types giving the instantiated parameter types (its
  tag the return type); omitted, the SAM method's. The method is a qualified method symbol as in
  Clojure 1.12: `C/m` (static), `C/.m` (instance), `C/new` (constructor). With param-tags it picks
  an overload: `^[String] Integer/valueOf`. Without a receiver, an instance method takes its
  receiver from the first argument (`String::length`). A receiver expression before the method
  binds it (`expr::m`); it is evaluated once, null-checked as javac does, and may be `this` or
  `super` (`super::m`). Array constructor references (`int[]::new`) are written as the lambda
  javac makes of them: `(lambda IntFunction [^int n] (new int/1 n))`.
- The compiler makes direct method handles or synthetic lambda methods for references exactly
  where javac does (javac uses a lambda method for `super::m`, varargs adaptation, protected
  members across packages and the like).
- Clojure's `fn` is unchanged, including Clojure 1.12's conversion of a `fn` passed where a
  functional interface is expected. That makes an `IFn`, not a Java lambda.

### 5.13 Clojure inside class bodies

Class bodies are Clojure, so everything else works too: vars, keywords, collection literals,
`fn`, protocols, `str`. Such code has Clojure's semantics and needs the runtime. Its constants
(vars, keywords) live in private static synthetic fields of the class, initialized in
`<clinit>`, as in `deftype`. `fn` classes created in a class body are added to its nest, so they
can reach private members.

The **Java subset** is what converted code uses and what stage 0 must compile:

- the forms of this spec and the class forms;
- `do`, `let`, `loop`, `recur`, `if`, `when`, `when-not`, `if-not`, `cond`, `and`, `or`, `not`,
  `throw`, `try`, `locking`, `set!`, `new`, `.`, `..`, `doto`, the interop shorthands;
- `instance?`, `cast`, `identical?`, `nil?`, `some?`, `==`, `<`, `<=`, `>`, `>=`, `=` on
  primitives, `aget`, `aset`, `alength`, the conversions and operators of §5.4 and §5.5.

Each of these compiles to plain bytecode. In the subset nothing refers to a var at run time.

## 6. What the compiler derives

The forms state the source construct; the compiler derives the class file pattern the way javac
does (javac's `Lower`, `TransTypes`, `TransPatterns`, `LambdaToMethod`, `Gen` and `ClassWriter`),
with javac's names. The converter writes none of this.

| construct | derived as javac does |
|---|---|
| every class | `ACC_SUPER`; superclass `Object` by default; default constructor |
| field initializers, `initializer` | folded into the constructors that call `super.`, after the call |
| static field initializers, `static-initializer` | `<clinit>`, in textual order |
| constant fields | `ConstantValue`; reads elsewhere inlined |
| covariant returns, generic overrides | bridge methods (`ACC_BRIDGE`, `ACC_SYNTHETIC`), from the erased signatures of the overridden methods with the supertypes' type arguments substituted |
| generic declarations | `Signature` attributes |
| inner classes | `this$0` field, outer instance constructor parameter (`MANDATED`, with `MethodParameters`), stored before the `super` call; `Objects.requireNonNull` on an explicit outer instance |
| local and anonymous classes | `val$x` capture fields and constructor parameters, `Outer$1` and `Outer$1Local` names, `EnclosingMethod`, anonymous constructors passing their arguments on |
| nesting | `InnerClasses` (also for referenced classes of other nests), `NestHost`, `NestMembers` |
| protected members across packages from inner classes, `Outer.super.m()` | `access$NNN` accessor methods |
| enums | constant fields, `$VALUES`, `$values()`, `values()`, `valueOf(String)`, name and ordinal constructor parameters and the `super` call, `Enum<E>` signature, `ACC_ENUM`, constant bodies as `E$1` |
| records | fields, accessors, canonical constructor, compact constructor's field assignments, `toString`/`hashCode`/`equals` through `ObjectMethods.bootstrap`, `Record` attribute, `MethodParameters` of the canonical constructor, propagation of component annotations |
| sealed classes | `PermittedSubclasses`, inferred within the same top-level form or file without `:permits` |
| annotation types | `ACC_ANNOTATION`, `AnnotationDefault` |
| annotations | visible, invisible or none by `@Retention`; type annotations with their target and path |
| `java-str` | `invokedynamic` to `StringConcatFactory.makeConcatWithConstants` |
| `lambda`, `method-ref` | `lambda$m$n` methods, `LambdaMetafactory` (`altMetafactory` when serializable or with markers), `$deserializeLambda$`, `BootstrapMethods` |
| `switch` on strings | `hashCode` `lookupswitch` and `equals`, then a second `switch` |
| `switch` on enums | ordinals directly for enums of the same compilation, otherwise the synthetic `Outer$1` class with `$SwitchMap$pkg$Enum` arrays |
| `switch` with patterns or `nil` | `SwitchBootstraps.typeSwitch` or `enumSwitch`, restart indexes for guards, `ConstantBootstraps` dynamic constants for some labels |
| record patterns | accessor calls, `Throwable` from them wrapped in `MatchException` |
| `for-each` | index loop over a copy of the array reference and its length, or `iterator()`/`hasNext()`/`next()` with the element cast |
| `with-resources` | javac's `try`/`catch Throwable`/`addSuppressed`/`close` pattern, without the `null` check for a `new` resource |
| `locking`, `^:synchronized` | `monitorenter`/`monitorexit` with the catch-all handler; `ACC_SYNCHRONIZED` |
| `java-assert` | `$assertionsDisabled`, `desiredAssertionStatus()`, `AssertionError` |
| boxing, unboxing | `valueOf`, `xxxValue` |
| class literals | `ldc`, and `Integer.TYPE` style fields for primitives |
| `break`, `continue`, `return` through `finally` | the `finally` code inlined at each exit |
| constant conditions | dead branches left out |
| calls of variable-arity methods with loose arguments | the argument array |
| `module-info`, `package-info` | `Module` (with `MANDATED` `java.base`), the synthetic `package-info` interface |

Two of these depend on the compilation set rather than on the forms alone, as in javac. Enum
switches use ordinals only for enums compiled together with the switch, and `PermittedSubclasses`
is inferred from the same file. For the compiler, "compiled together" means: classes entered
from source in the same compilation (§9.2), as opposed to classes loaded from class files.

## 7. What the converter makes explicit

The converter works on javac's attributed trees, so it knows every resolved name, chosen member,
implicit conversion and inferred type. It writes out what Arbace would otherwise get differently:

1. **Names.** Every class by simple name with an `:import`, or by binary name; no wildcard or
   static imports. Implicit qualifications become explicit: `(.m this)`, `(C/m)`, `Outer/this`,
   `(.-f this)` for inherited fields. A static member is qualified by the class javac uses as
   its qualifying type (the current class for an inherited static called by simple name).
2. **Types of declarations**, generic ones included, and the tags of locals that javac typed
   differently from their initializer (`Object o = s` is `(let [^Object o s] ...)`).
3. **Overloads.** Param-tags wherever the compiler's resolution (§5.6) would choose differently
   from javac or not at all. The converter calls the compiler's resolution to decide.
4. **Conversions**: narrowing, checked casts, erasure casts, unboxing from non-wrapper types,
   `int`/`long` to `float` promotions in comparisons, branch types (§5.5).
5. **Constants**: `int` literals that look out of range (§5.4); `^:const` on constant local
   variables.
6. **Evaluation order**: temporaries for `i++` in expressions, compound assignments with
   side-effecting targets, and wherever restructuring would reorder side effects.
7. **Arithmetic** with the typed operators of §5.4, chosen by the operation's promoted type.
8. **Lambdas and method references** with their target type and instantiated signature.
9. **Switches** with the implicit `MatchException` default, and repeated code for
   fall-through.
10. **Structure.** Restructured control flow where it is straightforward (§5.7), `let` scopes for
    Java's block-scoped declarations, `letclass` for local classes.
11. **Kept as written:** the declaration order of members, modifiers as Java wrote them (implicit
    ones may be left out, as Java leaves them out), annotations except `SOURCE` ones (which may
    be kept as documentation).

Comments do not count for equivalence. Since the converted Clojure becomes the source (step 4),
the converter carries Java's comments over as `;` comments where that is easy.

## 8. Coverage

These tables check the spec against the authoritative lists: javac's tree kinds
(`com.sun.source.tree.Tree.Kind`), the class file structure (JVMS chapter 4) and the attributes
javac's `ClassWriter` writes.

### 8.1 Tree kinds

| kinds | forms |
|---|---|
| `COMPILATION_UNIT`, `PACKAGE`, `IMPORT` | a file in a namespace; `in-ns`/`ns` and `:import` (§9.1); `defpackage` for package annotations |
| `MODULE`, `REQUIRES`, `EXPORTS`, `OPENS`, `USES`, `PROVIDES` | `defmodule` |
| `CLASS`, `INTERFACE`, `ENUM`, `RECORD`, `ANNOTATION_TYPE` | `defclass` (and `anon`, `letclass`) with the kind as metadata |
| `METHOD` | `method`, `constructor` |
| `VARIABLE` | `field`, parameters, `let`/`loop` bindings, `catch` and resource bindings, pattern bindings |
| `BLOCK` | `do`, a body; `initializer`, `static-initializer` |
| `MODIFIERS`, `ANNOTATION`, `TYPE_ANNOTATION` | metadata |
| `TYPE_PARAMETER` | `:type-params` |
| `PRIMITIVE_TYPE`, `ARRAY_TYPE`, `PARAMETERIZED_TYPE`, `UNION_TYPE`, `INTERSECTION_TYPE`, `UNBOUNDED_WILDCARD`, `EXTENDS_WILDCARD`, `SUPER_WILDCARD`, `ANNOTATED_TYPE` | type forms (§4.3), `(catch [A B] ...)` |
| `EMPTY_STATEMENT`, `EXPRESSION_STATEMENT` | nothing; a form in statement position |
| `IF`, `CONDITIONAL_EXPRESSION` | `if`, `when`, `cond` |
| `WHILE_LOOP`, `DO_WHILE_LOOP`, `FOR_LOOP`, `ENHANCED_FOR_LOOP` | `while`, `loop`/`recur`, `for-each` |
| `LABELED_STATEMENT`, `BREAK`, `CONTINUE`, `RETURN`, `YIELD` | `label`, `break`, `continue`, `return`, `(break :L v)` |
| `SWITCH`, `SWITCH_EXPRESSION`, `CASE`, `CONSTANT_CASE_LABEL`, `PATTERN_CASE_LABEL`, `DEFAULT_CASE_LABEL` | `switch` |
| `ANY_PATTERN`, `BINDING_PATTERN`, `DECONSTRUCTION_PATTERN` | patterns in `switch`, `if-instance`, `when-instance` |
| `THROW`, `TRY`, `CATCH`, `SYNCHRONIZED`, `ASSERT` | `throw`, `try`, `with-resources`, `catch`, `locking`, `java-assert` |
| `IDENTIFIER`, `MEMBER_SELECT` | symbols, `C/f`, `(.-f x)`, `Outer/this` |
| `METHOD_INVOCATION`, `NEW_CLASS`, `NEW_ARRAY` | `(.m x)`, `(C/m)`, `(C.)`, `(.new o C)`, `anon`, `(new T/n ...)` |
| `ARRAY_ACCESS`, `ASSIGNMENT`, the 11 compound assignments | `aget`, `aset`, `set!` |
| `PREFIX_`/`POSTFIX_INCREMENT`/`DECREMENT`, `UNARY_PLUS`, `UNARY_MINUS`, `BITWISE_COMPLEMENT`, `LOGICAL_COMPLEMENT` | `set!` with `unchecked-inc` etc., nothing, `unchecked-negate*`, `bit-not*`, `not` |
| `MULTIPLY` ... `OR` (binary), `CONDITIONAL_AND`, `CONDITIONAL_OR` | operators of §5.4, `identical?`, `and`, `or` |
| `INSTANCE_OF`, `TYPE_CAST` | `instance?`, `if-instance`, `cast`, `unchecked-*` |
| `LAMBDA_EXPRESSION`, `MEMBER_REFERENCE` | `lambda`, `method-ref` |
| `INT_LITERAL` ... `NULL_LITERAL` | Clojure literals (§5.4) |
| `PARENTHESIZED` | none; nesting. (javac skips the null check of `new C()::m` but not of `(new C())::m`; both never fail.) |
| `ERRONEOUS`, `OTHER` | none |

### 8.2 Access flags

| flag | class | field | method | inner class entry | parameter | from |
|---|---|---|---|---|---|---|
| `ACC_PUBLIC` 0x0001 | ✓ | ✓ | ✓ | ✓ | | `^:public`, implicit |
| `ACC_PRIVATE` 0x0002 | | ✓ | ✓ | ✓ | | `^:private` |
| `ACC_PROTECTED` 0x0004 | | ✓ | ✓ | ✓ | | `^:protected` |
| `ACC_STATIC` 0x0008 | | ✓ | ✓ | ✓ | | `^:static`, implicit |
| `ACC_FINAL` 0x0010 | ✓ | ✓ | ✓ | ✓ | ✓ | `^:final`, implicit |
| `ACC_SUPER` 0x0020 | ✓ | | | | | always |
| `ACC_SYNCHRONIZED` 0x0020 | | | ✓ | | | `^:synchronized` |
| `ACC_VOLATILE` 0x0040 | | ✓ | | | | `^:volatile` |
| `ACC_BRIDGE` 0x0040 | | | ✓ | | | derived, `^:bridge` |
| `ACC_TRANSIENT` 0x0080 | | ✓ | | | | `^:transient` |
| `ACC_VARARGS` 0x0080 | | | ✓ | | | `&` in the parameters |
| `ACC_NATIVE` 0x0100 | | | ✓ | | | `^:native` |
| `ACC_INTERFACE` 0x0200 | ✓ | | | ✓ | | `^:interface`, `^:annotation` |
| `ACC_ABSTRACT` 0x0400 | ✓ | | ✓ | ✓ | | `^:abstract`, implicit |
| `ACC_STRICT` 0x0800 | | | | | | never, as javac for release 17 and later |
| `ACC_SYNTHETIC` 0x1000 | ✓ | ✓ | ✓ | ✓ | ✓ | derived, `^:synthetic` |
| `ACC_ANNOTATION` 0x2000 | ✓ | | | ✓ | | `^:annotation` |
| `ACC_ENUM` 0x4000 | ✓ | ✓ | | ✓ | | `^:enum`, enum constants |
| `ACC_MANDATED` 0x8000 | | | | | ✓ | derived |
| `ACC_MODULE` 0x8000 | ✓ | | | | | `defmodule` |

Module flags (`ACC_OPEN`, `ACC_TRANSITIVE`, `ACC_STATIC_PHASE`, `ACC_SYNTHETIC`, `ACC_MANDATED`)
come from `defmodule` metadata or are derived.

### 8.3 Attributes

| attribute | where | source |
|---|---|---|
| `ConstantValue` | field | final field with constant initializer |
| `Code`, `StackMapTable`, `Exceptions` (table) | method | the body |
| `Exceptions` | method | `:throws` |
| `Signature` | class, field, method, record component | generic types (§4.3) |
| `InnerClasses`, `EnclosingMethod`, `NestHost`, `NestMembers` | class | nesting (§4.8) |
| `PermittedSubclasses` | class | `^:sealed`, `:permits` |
| `Record` | class | record components |
| `BootstrapMethods` | class | `invokedynamic` and dynamic constants |
| `RuntimeVisibleAnnotations`, `RuntimeInvisibleAnnotations` | class, field, method, record component | annotation metadata |
| `RuntimeVisibleParameterAnnotations`, `RuntimeInvisibleParameterAnnotations` | method | parameter metadata |
| `RuntimeVisibleTypeAnnotations`, `RuntimeInvisibleTypeAnnotations` | class, field, method, `Code`, record component | type annotations |
| `AnnotationDefault` | method | `:default` |
| `MethodParameters` | method | derived where javac emits it: mandated or synthetic parameters, canonical record constructors (and everywhere with javac's `-parameters`, as a compiler option) |
| `Deprecated` | class, field, method | `^:deprecated`, `^{Deprecated ...}` |
| `Module` | `module-info` | `defmodule` |
| `SourceFile`, `LineNumberTable`, `LocalVariableTable`, `LocalVariableTypeTable`, `CharacterRangeTable` | | debug information: the compiler may emit it; it does not count |
| `SourceID`, `CompilationID` | | javac internals (`-XDsourceid`); not emitted |
| `Synthetic` | | not used by javac for current versions (the flag is) |
| `SourceDebugExtension`, `ModulePackages`, `ModuleMainClass`, `ModuleTarget`, `ModuleResolution`, `ModuleHashes` | | not written by javac (other tools write the module ones) |

### 8.4 Language features without class file traces

`var`, the diamond, explicit type arguments of calls (except their type annotations, which go on
the call's method symbol as `^{:type-args [...]}`), static and module imports, text blocks,
`@Override`, unnamed variables, compact source files (an implicitly declared class is a final
`defclass` with `:package ""` and its `main`), instance main methods, and `strictfp` leave
nothing beyond what their translation shows.

## 9. Compilation model

### 9.1 Namespaces, packages and files

A class form's package is its namespace's (munged), as for `deftype`. For Java packages with many
classes the recommended layout follows `clojure.core`, which loads `core_print.clj` and others
into one namespace:

```
arbace/lang.clj            (ns arbace.lang (:import ...)) and (load "lang/Keyword") ... in order
arbace/lang/Keyword.clj    (in-ns 'arbace.lang) (import ...) (defclass ^:public Keyword ...)
arbace/lang/RT.clj         (in-ns 'arbace.lang) ...
```

One file per Java source file, named after its top-level class, holding that file's class
forms. Same-package classes need no import (§5.2). Java's per-file imports become `import`
calls in the file; when two files import different classes under one simple name, the converter
writes binary names instead. The alternative, one namespace per class in `gen-class` style
(`(ns arbace.lang.Keyword)` with `(defclass arbace.lang.Keyword ...)`), is question 5 in §12.

### 9.2 Declarations before code, and cycles

Java classes refer to each other in cycles (`RT`, `Var`, `Namespace`, `Symbol`, `Keyword` all
do), and javac compiles them together. Clojure compiles one top-level form at a time. To compile
a body the compiler needs the members of every class it mentions, so it keeps a **class
environment**: for each class, its declared members with their types and flags. Entries come
from

1. classes loaded or loadable from the class path (by reflection, plus reading class files for
   `ConstantValue` and generic signatures),
2. the class forms of the current top-level form, entered before any body is compiled (so a
   top-level `do` of mutually dependent class forms works, also at the REPL),
3. **sources**: a class name that resolves to nothing is looked up as `p/C.clj` on the source
   path (javac's `-sourcepath` behaviour); the file's namespace form is evaluated and its
   top-level class forms are entered, declarations only, without compiling their bodies or
   evaluating anything else. The bodies are compiled when the file itself is loaded.

Bytecode refers to classes by name and the JVM resolves names lazily, so a class can be defined
before the classes it refers to exist, as long as it is not used before. With 3, loading
`arbace/lang.clj` (or requiring any one class's file) compiles each file in turn, whatever the
cycles.

### 9.3 AOT

Under `*compile-files*` every class a class form defines is written to `*compile-path*`, nested,
local, anonymous and synthetic ones (`$SwitchMap$` holders) included, as `deftype` classes are.
Loading the namespace's `__init` then defines nothing: the classes load from the class path. A
`defclass` in AOT-compiled code evaluates to its class, loaded by name. Classes written only with
the Java subset have no link to their namespace and can be used without loading it, which is
what `arbace.lang` needs.

`defmodule` and `defpackage` only write files.

### 9.4 Relation to `deftype`, `defrecord`, `reify`, `proxy`, `gen-class`

They all stay as they are; they implement Clojure's data types, not Java's.

| | `deftype` / `reify` | `defclass` / `anon` |
|---|---|---|
| default access | public class, public fields | Java's: package access |
| fields | a vector; final unless `^:volatile-mutable`/`^:unsynchronized-mutable` (then private) | `field` forms with Java's modifiers; non-final unless `^:final` |
| superclass | `Object` only | any |
| methods | only those of the interfaces and `Object` | any, static ones and overloads included |
| constructors | one, from the field vector | any, with `super.`/`this.` |
| statics, initializers, nested types | none | all |
| field and class names | munged | verbatim |
| extras | `__meta`, `__extmap`, `getBasis`, `IObj` for `reify` | none |
| self-reference while compiling | a stub class | the class environment |

`gen-class` generates stub classes delegating to vars, for AOT only. `defclass` covers its uses
directly. Arbace could later define `deftype*` and `reify*` as macros over `class*`; that is not
part of this spec.

### 9.5 Special forms and macros

The new names are vars in `arbace.core`, so code can `:exclude` them. The compiler knows a few
new special forms with starred names, which no namespace can shadow, as `let*` and `fn*` today:
`class*` (one for every kind of class), `label*`, `break*`, `continue*`, `return*`, `switch*`,
`lambda*`, `method-ref*`, `java-str*`, `java-assert*`, `for-each*`. The macros `defclass`, `anon`,
`letclass`, `defmodule`, `defpackage`, `label`, `break`, `continue`, `return`, `switch`, `lambda`,
`method-ref`, `java-str`, `java-assert`, `for-each`, `with-resources`, `if-instance` and
`when-instance` expand to them and to existing forms; `with-resources` and the pattern tests need
no special form of their own. The operators of §5.4 are functions with `:inline` expansions to
`Numbers` methods that the compiler emits as instructions, as Clojure's intrinsics do.
Extensions of existing special forms (`let*`, `loop*`, `set!`, `new`, `.`, `recur`) are listed in
§1.1.

### 9.6 Bootstrap

- **Stage 0**: the frozen `clojure/` with a bootstrap library implementing the class forms. Its
  `defclass` macro runs the library's own compiler on the form (the frozen `Compiler.java` knows
  nothing of `class*`), which compiles the bodies (the Java subset only) with `clojure.asm` and
  defines or writes the classes. The stage-0 driver interns the library's macros into the running
  `clojure.core` namespace (at run time; the frozen sources are not touched), so the same
  `arbace/**/*.clj` sources work at every stage without qualifying the new names.
- **Stage 1**: stage 0 compiles `arbace/**/*.clj`. Its classes are self-contained `arbace.*`
  classes, `arbace.lang.Compiler` (converted, with this spec implemented) among them. Clojure-level
  namespaces such as `arbace.core` must be compiled by Arbace's own compiler, since the frozen one
  emits references to `clojure.lang`; the order of that is a matter for steps 2 and 4.
- **Stage 2** recompiles with stage 1, and must reproduce itself.

## 10. The REPL

```clojure
user=> (defclass ^:public Counter
         (field ^:private ^int n)
         (method ^:public inc ^int [this] (set! n (unchecked-inc-int n))))
user.Counter
user=> (let [c (Counter.)] (.inc c) (.inc c))
2
```

- **Defining.** A class form typed at the REPL is compiled and defined at once, with all classes
  nested in it, and imported into the namespace. Its static initializer runs at first use, as in
  Java. The REPL namespace's package is used (`user.Counter`), so a class meant to be used from
  Clojure code is written `^:public`, with public members (Clojure's functions live in other
  classes and packages).
- **Class loaders.** Clojure defines each top-level form's classes in a fresh
  `DynamicClassLoader`, and a JVM runtime package is a package *and* a loader. Package access
  between classes defined by different forms or files would then fail. So the compiler keeps one
  loader per Java package, a **package loader**, and defines every class of that package in it.
  All classes of one class form share a loader anyway, which nest membership requires.
- **Redefining.** A loader cannot define a name twice. Redefining a class starts a new
  generation: a fresh package loader becomes current for that package and receives the new class
  and every later one. Old instances keep their old class. Classes compiled earlier keep linking
  to the version they first resolved, as for `deftype`; Clojure code compiled later finds the
  newest one through `DynamicClassLoader`'s class cache. Package access between generations fails
  with `IllegalAccessError`, as the JVM demands. Reloading the package's files (or the
  dependent classes) moves everything into the new generation. Nested classes are redefined with
  their top-level class only.
- **Alternatives considered.** Clojure's loader per form: breaks package access between forms.
  `MethodHandles.Lookup.defineClass`: puts classes into an existing class's loader, so no
  redefinition. Hidden classes: cannot be named, so other code cannot refer to them. Widening
  package access to public at the REPL: changes the classes.
- **Cycles at the REPL**: wrap the class forms in one `do`, or let source path lookup (§9.2) find
  the others.
- AOT-compiled classes are loaded by the application class loader; none of this applies to them.
  A REPL-defined class in the package of an AOT-compiled one has no package access to it, being
  in another loader.

## 11. Worked examples

Transcribed by hand from the baseline (`clojure/lang`, before the `arbace.*` renaming), as the
converter should produce them. Comments are the Java's.

### 11.1 `Keyword`

```clojure
(in-ns 'clojure.lang)
(import '(java.io ObjectStreamException Serializable)
        '(java.lang.ref Reference ReferenceQueue WeakReference)
        '(java.util.concurrent ConcurrentHashMap))

(defclass ^:public Keyword
  :implements [IFn Comparable Named Serializable IHashEq]

  (field ^:private ^:static ^:final ^long serialVersionUID -2105088845257724163)

  (field ^:private ^:static ^{:tag (ConcurrentHashMap Symbol (Reference Keyword))} table
    (ConcurrentHashMap.))
  (field ^:static ^:final ^ReferenceQueue rq (ReferenceQueue.))
  (field ^:public ^:final ^Symbol sym)
  (field ^:final ^int hasheq)
  (field ^:transient ^String _str)

  (method ^:public ^:static intern ^Keyword [^:mutable ^Symbol sym]
    (let [^:mutable ^Keyword k nil
          ^:mutable ^{:tag (Reference Keyword)} existingRef (cast Reference (.get table sym))]
      (when (nil? existingRef)
        (Util/clearCache rq table)
        (when (some? (.meta sym))
          (set! sym (cast Symbol (.withMeta sym nil))))
        (set! k (Keyword. sym))
        (set! existingRef (cast Reference (.putIfAbsent table sym (WeakReference. k rq)))))
      (if (nil? existingRef)
        k
        (let [existingk (cast Keyword (.get existingRef))]
          (if (some? existingk)
            existingk
            (do ;; entry died in the interim, do over
              (.remove table sym existingRef)
              (Keyword/intern sym)))))))

  (method ^:public ^:static intern ^Keyword [^String ns ^String name]
    (Keyword/intern (Symbol/intern ns name)))

  (method ^:public ^:static intern ^Keyword [^String nsname]
    (Keyword/intern (Symbol/intern nsname)))

  (constructor ^:private [this ^Symbol sym]
    (set! (.-sym this) sym)
    (set! hasheq (unchecked-add-int (.hasheq sym) (unchecked-int 0x9e3779b9))))

  (method ^:public ^:static find ^Keyword [^Symbol sym]
    (let [^{:tag (Reference Keyword)} ref (cast Reference (.get table sym))]
      (if (some? ref)
        (cast Keyword (.get ref))
        nil)))

  (method ^:public ^:static find ^Keyword [^String ns ^String name]
    (Keyword/find (Symbol/intern ns name)))

  (method ^:public ^:static find ^Keyword [^String nsname]
    (Keyword/find (Symbol/intern nsname)))

  (method ^:public ^:final hashCode ^int [this]
    (unchecked-add-int (.hashCode sym) (unchecked-int 0x9e3779b9)))

  (method ^:public hasheq ^int [this]
    hasheq)

  (method ^:public toString ^String [this]
    (when (nil? _str)
      (set! _str (java-str ":" sym)))
    _str)

  ;; @deprecated CLJ-2350: This function is no longer called, but has not been
  ;; removed to maintain the public interface.
  (method ^:public ^:deprecated throwArity [this]
    (throw (IllegalArgumentException.
             (java-str "Wrong number of args passed to keyword: " (.toString this)))))

  (method throwArity [this ^int n]
    (throw (ArityException. n (.toString this))))

  (method ^:public call [this]
    (.throwArity this 0))

  (method ^:public run ^void [this]
    (throw (UnsupportedOperationException.)))

  (method ^:public invoke [this]
    (.throwArity this 0))

  (method ^:public compareTo ^int [this o]
    (.compareTo sym (.-sym (cast Keyword o))))

  (method ^:public getNamespace ^String [this]
    (.getNamespace sym))

  (method ^:public getName ^String [this]
    (.getName sym))

  (method ^:private readResolve :throws [ObjectStreamException] [this]
    (Keyword/intern sym))

  ;; Indexer implements IFn for attr access
  (method ^:public ^:final invoke [this obj]
    (if (instance? ILookup obj)
      (.valAt (cast ILookup obj) this)
      (RT/get obj this)))

  (method ^:public ^:final invoke [this obj notFound]
    (if (instance? ILookup obj)
      (.valAt (cast ILookup obj) this notFound)
      (RT/get obj this notFound)))

  (method ^:public invoke [this arg1 arg2 arg3]
    (.throwArity this 3))

  ;; ... invoke with 4 to 20 arguments alike ...

  (method ^:public invoke [this arg1 arg2 arg3 arg4 arg5 arg6 arg7 arg8 arg9 arg10 arg11 arg12
                           arg13 arg14 arg15 arg16 arg17 arg18 arg19 arg20 & ^Object/1 args]
    (.throwArity this (unchecked-add-int 20 (alength args))))

  (method ^:public applyTo [this ^ISeq arglist]
    (AFn/applyToHelper this arglist)))
```

Points to note:

- `sym` is reassigned in `intern`, so the parameter is `^:mutable`; `k` and `existingRef` are
  assigned after their declaration.
- `table.get(sym)` returns `V`, erased to `Object`; javac casts to `Reference`, and so does the
  form. `existingRef.get()` is cast to `Keyword` likewise. `sym.withMeta(null)` returns `IObj`,
  so Java's own cast is there too.
- `0x9e3779b9` is a negative `int` in Java; `(unchecked-int 0x9e3779b9)` is the same constant.
- In `hasheq` the method and the field share the name; the body's `hasheq` is the field, since
  methods are never in scope by name.
- `":" + sym` is `java-str` with a `Symbol` operand: `String.valueOf(Object)` semantics through
  `invokedynamic`, as javac compiles it.
- The `invoke`, `call`, `readResolve` and `applyTo` methods are untyped where Java uses `Object`.
  `run` could be untyped too, by the `deftype` rule of §4.6, but the converter writes `^void`.
- Overloads need no param-tags here: `Keyword/intern` with a `Symbol` argument, `RT/get` and
  `.valAt` by arity, `ArityException.` with `(int, String)`.

### 11.2 `PersistentVector` (excerpts)

```clojure
(defclass ^:public PersistentVector
  :extends APersistentVector
  :implements [IObj IEditableCollection IReduce IKVReduce IDrop]

  (field ^:private ^:static ^:final ^long serialVersionUID -7896022351281214157)

  (defclass ^:public ^:static Node
    :implements [Serializable]
    (field ^:transient ^:public ^:final ^{:tag (AtomicReference Thread)} edit)
    (field ^:public ^:final ^Object/1 array)

    (constructor ^:public [this ^{:tag (AtomicReference Thread)} edit ^Object/1 array]
      (set! (.-edit this) edit)
      (set! (.-array this) array))

    (constructor [this ^{:tag (AtomicReference Thread)} edit]
      (set! (.-edit this) edit)
      (set! array (new Object/1 32))))

  (field ^:static ^:final ^{:tag (AtomicReference Thread)} NOEDIT (AtomicReference. nil))
  (field ^:public ^:static ^:final ^Node EMPTY_NODE (Node. NOEDIT (new Object/1 32)))

  (field ^:final ^int cnt)
  (field ^:public ^:final ^int shift)
  (field ^:public ^:final ^Node root)
  (field ^:public ^:final ^Object/1 tail)
  (field ^:final ^IPersistentMap _meta)

  (field ^:public ^:static ^:final ^PersistentVector EMPTY
    (PersistentVector. 0 5 EMPTY_NODE (new Object/1 [])))

  (field ^:private ^:static ^:final ^IFn TRANSIENT_VECTOR_CONJ
    (anon AFn []
      (method ^:public invoke [this coll val]
        (.conj (cast ITransientVector coll) val))
      (method ^:public invoke [this coll]
        coll)))

  (method ^:public ^:static create ^PersistentVector [^:mutable ^ISeq items]
    (let [arr (new Object/1 32)
          ^:mutable ^int i 0]
      (while (and (some? items) (< i 32))
        (aset arr i (.first items))
        (set! i (unchecked-inc-int i))
        (set! items (.next items)))
      (cond
        (some? items)                   ; >32, construct with array directly
        (let [start (PersistentVector. 32 5 EMPTY_NODE arr)
              ^:mutable ret (.asTransient start)]
          (while (some? items)
            (set! ret (.conj ret (.first items)))
            (set! items (.next items)))
          (.persistent ret))

        (== i 32)                       ; exactly 32, skip copy
        (PersistentVector. 32 5 EMPTY_NODE arr)

        :else                           ; <32, copy to minimum array and construct
        (let [arr2 (new Object/1 i)]
          (System/arraycopy arr 0 arr2 0 i)
          (PersistentVector. i 5 EMPTY_NODE arr2)))))

  (method ^:public ^:static create ^PersistentVector [^Iterable items]
    (if (instance? ArrayList items)     ; optimize common case
      (PersistentVector/create (cast ArrayList items))
      (let [iter (.iterator items)
            ^:mutable ret (.asTransient EMPTY)]
        (while (.hasNext iter)
          (set! ret (.conj ret (.next iter))))
        (.persistent ret))))

  (method ^:public ^:static create ^PersistentVector [& ^Object/1 items]
    (let [^:mutable ret (.asTransient EMPTY)]
      (for-each [item items]
        (set! ret (.conj ret item)))
      (.persistent ret)))

  (constructor [this ^int cnt ^int shift ^Node root ^Object/1 tail]
    (set! _meta nil)
    (set! (.-cnt this) cnt)
    (set! (.-shift this) shift)
    (set! (.-root this) root)
    (set! (.-tail this) tail))

  (method ^:final tailoff ^int [this]
    (if (< cnt 32)
      0
      (bit-shift-left-int (unsigned-bit-shift-right-int (unchecked-subtract-int cnt 1) 5) 5)))

  (method ^:public arrayFor ^Object/1 [this ^int i]
    (if (and (>= i 0) (< i cnt))
      (if (>= i (.tailoff this))
        tail
        (loop [^Node node root, ^int level shift]
          (if (> level 0)
            (recur (cast Node (aget (.-array node) (bit-and-int (unsigned-bit-shift-right-int i level) 0x01f)))
                   (unchecked-subtract-int level 5))
            (.-array node))))
      (throw (IndexOutOfBoundsException.))))

  (method rangedIterator ^Iterator [this ^:final ^int start ^:final ^int end]
    (anon Iterator []
      (field ^int i start)
      (field ^int base (unchecked-subtract-int i (unchecked-remainder-int i 32)))
      (field ^Object/1 array
        (if (< start (.count PersistentVector/this)) (.arrayFor PersistentVector/this i) nil))

      (method ^:public hasNext ^boolean [this]
        (< i end))

      (method ^:public next [this]
        (if (< i end)
          (do (when (== (unchecked-subtract-int i base) 32)
                (set! array (.arrayFor PersistentVector/this i))
                (set! base (unchecked-add-int base 32)))
              (let [j i]
                (set! i (unchecked-inc-int i))
                (aget array (bit-and-int j 0x01f))))
          (throw (NoSuchElementException.))))

      (method ^:public remove ^void [this]
        (throw (UnsupportedOperationException.)))))

  (defclass ^:static ^:final TransientVector
    :extends AFn
    :implements [ITransientVector ITransientAssociative2 Counted]
    (field ^:volatile ^int cnt)
    (field ^:volatile ^int shift)
    (field ^:volatile ^Node root)
    (field ^:volatile ^Object/1 tail)
    (field ^:private ^:final ^IPersistentMap _meta)

    (constructor [this ^int cnt ^int shift ^Node root ^Object/1 tail ^IPersistentMap _meta]
      (set! (.-cnt this) cnt)
      (set! (.-shift this) shift)
      (set! (.-root this) root)
      (set! (.-tail this) tail)
      (set! (.-_meta this) _meta))

    (constructor [this ^PersistentVector v]
      (this. (.-cnt v) (.-shift v) (TransientVector/editableRoot (.-root v))
             (TransientVector/editableTail (.-tail v)) (.-_meta v)))

    (method ^:public conj ^TransientVector [this val]
      (.ensureEditable this)
      (let [i cnt]
        (if (< (unchecked-subtract-int i (.tailoff this)) 32)       ; room in tail?
          (do (aset tail (bit-and-int i 0x01f) val)
              (set! cnt (unchecked-inc-int cnt))
              this)
          ;; full tail, push into tree
          (let [tailnode (Node. (.-edit root) tail)]
            (set! tail (new Object/1 32))
            (aset tail 0 val)
            (let [^:mutable ^int newshift shift
                  newroot (if (> (unsigned-bit-shift-right-int cnt 5) (bit-shift-left-int 1 shift))
                            (let [newroot (Node. (.-edit root))]    ; overflow root?
                              (aset (.-array newroot) 0 root)
                              (aset (.-array newroot) 1 (PersistentVector/newPath (.-edit root) shift tailnode))
                              (set! newshift (unchecked-add-int newshift 5))
                              newroot)
                            (.pushTail this shift root tailnode))]
              (set! root newroot)
              (set! shift newshift)
              (set! cnt (unchecked-inc-int cnt))
              this)))))

    ;; ... the other members ...
    ))
```

Points to note:

- `Node` is a static member class, in scope by simple name; `TransientVector` too.
- Generic field and parameter types (`AtomicReference<Thread>`) are written in full because they
  give `Signature` attributes.
- `TRANSIENT_VECTOR_CONJ`'s anonymous class becomes `PersistentVector$1`, with `EnclosingMethod`
  naming only the class (it is created in a field initializer) and no outer instance (a static
  context).
- In `rangedIterator`, the anonymous `Iterator` (`PersistentVector$2`) captures `start` and `end`
  (`val$start`, `val$end`) and the enclosing instance (`this$0`, used through
  `PersistentVector/this`). Its fields `i`, `base` and `array` are in scope by name in its
  initializers and methods; its own receiver shadows the outer method's `this`.
- `array[i++ & 0x01f]` binds the old `i` before incrementing.
- `arr[i++] = items.first()` in `create` became an `aset` then the increment: the right side does
  not read `i`, so the order of effects is unchanged.
- The `for` loops whose variables live on became `let` with mutable locals and `while`; the one
  in `arrayFor` became `loop`/`recur`.
- `create(Object...)` takes `& ^Object/1 items` (`ACC_VARARGS`) and iterates with `for-each`.
- `this(...)` delegation in `TransientVector` is `(this. ...)`; `++cnt` on a volatile field is a
  `set!`; `newPath`, a private static method of the outer class, is called directly (nestmates).
- `withMeta` returning `PersistentVector` while `IObj` declares `IObj withMeta(IPersistentMap)`
  (not shown) gets its bridge method from the compiler.

### 11.3 An enum and an interface

`Numbers.Category` (`static public enum Category {INTEGER, FLOATING, DECIMAL, RATIO};`), inside
`Numbers`' class body:

```clojure
(defclass ^:public ^:static ^:enum Category
  (constants INTEGER FLOATING DECIMAL RATIO))
```

The compiler derives `Numbers$Category` with `ACC_PUBLIC ACC_FINAL ACC_SUPER ACC_ENUM`, superclass
`Enum` with signature `Ljava/lang/Enum<Lclojure/lang/Numbers$Category;>;`, the four
`public static final enum` fields, `$VALUES`, `values()`, `valueOf(String)`, the private
`(String, int)` constructor, `$values()`, `<clinit>`, its `InnerClasses` entry and `NestHost`
`Numbers`.

`Associative` is in §4.9.

### 11.4 Newer Java

Not from the baseline, to show the constructs `clojure/` does not use:

```java
public sealed interface Shape permits Circle, Square {}
public record Circle(double r) implements Shape {}
public record Square(double side) implements Shape {
  public Square { if (side < 0) throw new IllegalArgumentException("side"); }
}
public final class Shapes {
  private Shapes() {}
  public static double area(Shape s) {
    return switch (s) {
      case Circle c -> Math.PI * c.r() * c.r();
      case Square(double side) -> side * side;
    };
  }
  public static <T extends Comparable<? super T>> T max(List<? extends T> xs) {
    T best = null;
    for (T x : xs)
      if (best == null || x.compareTo(best) > 0) best = x;
    return best;
  }
  static long nonBlank(Path p) throws IOException {
    try (var lines = Files.lines(p)) {
      return lines.filter(l -> !l.isBlank()).count();
    }
  }
  static int rowOf(int[][] grid, int v) {
    outer:
    for (int i = 0; i < grid.length; i++)
      for (int j = 0; j < grid[i].length; j++) {
        if (grid[i][j] < 0) continue outer;
        if (grid[i][j] == v) return i;
      }
    return -1;
  }
}
```

```clojure
(defclass ^:public ^:sealed ^:interface Shape
  :permits [Circle Square])

(defclass ^:public ^:record Circle [^double r]
  :implements [Shape])

(defclass ^:public ^:record Square [^double side]
  :implements [Shape]
  (constructor ^:public ^:compact [this]
    (when (< side 0.0)
      (throw (IllegalArgumentException. "side")))))

(defclass ^:public ^:final Shapes
  (constructor ^:private [this])

  (method ^:public ^:static area ^double [^Shape s]
    (switch s
      [^Circle c] (unchecked-multiply (unchecked-multiply Math/PI (.r c)) (.r c))
      [(Square ^double side)] (unchecked-multiply side side)
      (throw (MatchException. nil nil))))

  (method ^:public ^:static max
    :type-params [(T extends (Comparable (? super T)))]
    ^T [^{:tag (List (? extends T))} xs]
    (let [^:mutable ^T best nil]
      (for-each [^T x xs]
        (when (or (nil? best) (> (.compareTo x best) 0))
          (set! best x)))
      best))

  (method ^:static nonBlank :throws [IOException] ^long [^Path p]
    (with-resources [^Stream lines (Files/lines p)]
      (.count (.filter lines (lambda Predicate ^boolean [^String l] (not (.isBlank l)))))))

  (method ^:static rowOf ^int [^int/2 grid ^int v]
    (label :outer
      (loop [^int i 0]
        (when (< i (alength grid))
          (loop [^int j 0]
            (when (< j (alength (aget grid i)))
              (when (< (aget grid i j) 0)
                (continue :outer (unchecked-inc-int i)))
              (when (== (aget grid i j) v)
                (return i))
              (recur (unchecked-inc-int j))))
          (recur (unchecked-inc-int i)))))
    -1))
```

- `area`'s switch is exhaustive over the sealed `Shape`; javac's implicit default is written.
- In `max`, `T` erases to `Comparable`, so `for-each` casts each element to `Comparable` and
  `.compareTo` is `Comparable.compareTo(Object)`. The method's `Signature` carries the bounds.
- `nonBlank`'s lambda becomes `private static boolean lambda$nonBlank$0(String)`, with instantiated
  type `(String)boolean` for the erased `Predicate.test(Object)`.
- `rowOf` keeps Java's labeled `continue`: from the inner loop it restarts the outer one with the
  update `i + 1`.

## 12. Open questions

Each with the recommendation the spec follows.

1. **Receiver parameter.** Explicit `this` as the first parameter, as `deftype`, `reify` and
   `gen-class` have it, or an implicit `this` as in Java and `proxy`? Explicit costs a symbol per
   method (also in abstract and annotation methods) but needs no anaphora and lets the outer
   method's receiver be captured under another name. *Recommendation: explicit.*
2. **Default access.** Java's (no modifier is package access) or Clojure's (public, with a
   `^:package` marker)? Java's keeps the forms a mirror of Java and the converter's output free
   of markers on Java's many package-private members; REPL users must write `^:public`.
   *Recommendation: Java's.*
3. **One `defclass` for all kinds**, the kind as metadata, or a macro per kind? `definterface`
   and `defrecord` are taken by Clojure with other meanings, so separate names would need new
   words (`defenum`, `defannotation`, `defjrecord`...). *Recommendation: one `defclass`.*
4. **Generic types in tags.** `^{:tag (List T)}` is plain Clojure but verbose. Alternatives: Java
   syntax in a string tag (`^"List<T>"`, which Clojure accepts for array names), or a reader
   extension `^(List T)` once Arbace has its own reader (not readable at stage 0).
   *Recommendation: the map form now; revisit the reader extension after self-hosting.*
5. **Layout of converted packages.** One namespace per Java package with one loaded file per Java
   file (§9.1), or one namespace per class in `gen-class` style? The first keeps package = 
   namespace as for `deftype` and shares imports; the second matches Clojure's one-file-one-namespace
   rule. *Recommendation: per package.*
6. **Cycles.** A class environment fed from the current top-level form and from sources on the
   source path (§9.2), or explicit forward declarations (`(declare-class ...)` with member
   signatures), or compiling a package directory as one unit? *Recommendation: the class
   environment with source path lookup.*
7. **Reflection in class bodies.** An error, or a warning as in Clojure? Java code never reflects,
   and an unnoticed reflective call would silently break equivalence. *Recommendation: error,
   with a per-class `^{:reflection :warn}` escape for hand-written code.*
8. **Bridge methods.** Derived by the compiler, which then needs one piece of generic reasoning
   (substituting supertypes' type arguments into inherited signatures to find overrides), or
   written by the converter as `^:bridge ^:synthetic` methods? Covariant-return bridges (all over
   the baseline) need no generics either way. *Recommendation: derived, as javac does.*
9. **Fall-through in `switch`.** Repeat the shared code, or add a `fallthrough` form?
   *Recommendation: repeat; add the form only if the converter meets cases where repeating is
   unreasonable.*
10. **Names of the new forms.** The spec's choices, with alternatives considered:

    | chosen | alternatives |
    |---|---|
    | `defclass` | per-kind macros (question 3) |
    | `field`, `method`, `constructor`, `initializer`, `static-initializer` | `def`/`defn`-like heads; `ctor`, `init`, `clinit` |
    | `constants` | `enum`, `enum-constants` |
    | `anon` | `new-class`, an extended `reify` (whose classes are final and implement `IObj`) |
    | `letclass` | `defclass` in a body, scoped to the rest of it |
    | `lambda`, `method-ref` | `jfn`, `fn-ref`; a `fn` with a target-type tag (changes `fn`) |
    | `label`, `break`, `continue`, `return` | Common Lisp's `block` and `return-from` |
    | `switch` | an extended `case` (changes `case`) |
    | `if-instance`, `when-instance` | `instance?` with a binding |
    | `for-each` | `doiter`, an extended `doseq` (changes `doseq`) |
    | `with-resources` | `java-with-open` |
    | `java-str`, `java-assert` | `str+`, `jstr`; `assert*` |

    The rule used: a `java-` prefix only where Clojure has a like-named form with other
    semantics. *Recommendation: settle the names now, before code exists.*
11. **Boxing and unboxing.** Implicit, compiled to `valueOf`/`xxxValue` as javac does (which
    changes how Clojure boxes `long`, not the result), or always explicit in the forms?
    *Recommendation: implicit.*
12. **REPL class loaders.** Package loaders with generations (§10), or Clojure's loader per form,
    which loses package access between forms? *Recommendation: package loaders.*
13. **`float` arithmetic.** New `-float` operators, or `double` arithmetic narrowed to `float`
    (same results for `+ - * / %`, but other instructions)? *Recommendation: new operators.*
14. **Labels.** Keywords, which cannot be confused with locals, or symbols, as in Java and
    Common Lisp? *Recommendation: keywords.*
15. **Untyped methods.** Infer the signature from a unique inherited method with the same name and
    arity, as `deftype` does, or always `Object`? *Recommendation: infer, as `deftype`.*
16. **`cast`.** Compile `(cast T x)` to `checkcast` (same effect as `Class.cast`, other exception
    message), or add a new form? *Recommendation: reuse `cast`.*
17. **One implementation of the class forms or two.** A throwaway stage-0 library plus a second
    implementation inside Arbace's compiler, or one implementation in plain Clojure that runs on
    stage 0 and is then compiled into stage 1? With one, the source names ASM by one package
    (`arbace.asm`) and the stage-0 driver maps it to `clojure.asm` while loading.
    *Recommendation: one implementation; decide the details in step 2.*

## 13. Sources

Studied for this spec:

- Clojure at `98d735fab02f` (the frozen `clojure/`):
  - `clojure/lang/Compiler.java`: the special forms table; `LocalBinding` (the "Can't type hint a
    local with a primitive initializer" error); `LetExpr` (`loop` widening `int` and `float`, and
    `loop` and `try` wrapped in a `fn` in expression position); `QualifiedMethodExpr` and
    param-tags; array class symbols (`looksLikeArrayClass`); `FISupport` (functional interface
    adapters); `NewInstanceExpr` (`deftype*`, `reify*`).
  - `clojure/lang/Intrinsics.java`: the operators compiled to instructions.
  - `clojure/lang/DynamicClassLoader.java`, `clojure/lang/RT.java` (`makeClassLoader`).
  - `clojure/core.clj` (annotations in `add-annotations`, `locking`, `with-open`, `while`, the
    unchecked and bit operators), `clojure/core_deftype.clj`, `clojure/genclass.clj`,
    `clojure/core_proxy.clj`.
  - `clojure/lang/Keyword.java`, `PersistentVector.java`, `Reduced.java`, `Associative.java`,
    `Numbers.java` (for §11).
- jdk26u at `baf63fbe42b8`, `src/jdk.compiler/share/classes`:
  - `com/sun/source/tree/Tree.java` (`Tree.Kind`).
  - `com/sun/tools/javac/jvm/ClassWriter.java` (the attributes written; `MethodParameters` only
    for `-parameters`, mandated or synthetic parameters and canonical record constructors;
    `EnclosingMethod` for local and anonymous classes only).
  - `com/sun/tools/javac/code/Flags.java`, `jvm/ClassFile.java`.
  - `com/sun/tools/javac/comp/Lower.java` (enum switches: ordinals for enums in the compilation,
    `$SwitchMap$` otherwise; string switches; enums; assertions; inner classes),
    `TransTypes.java` (bridges, erasure casts), `TransPatterns.java` (`typeSwitch`, `enumSwitch`,
    `ConstantBootstraps`), `LambdaToMethod.java`, `jvm/StringConcat.java`.
- The Java Virtual Machine Specification, chapter 4 (class file format, access flags,
  attributes), and the Java Language Specification: 5 (conversions), 13.1 (qualifying types),
  14 (statements), 15.29 (constant expressions), 8.10 and 9.6 (records, annotation types).
