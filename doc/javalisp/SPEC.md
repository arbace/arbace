# javalisp: Java written as Clojure data

javalisp is a transcription of Java 26 source into s-expressions. Its text reads as plain
data with `clojure.edn/read` and with `clojure.core/read`, and it maps back to Java exactly.
The tool lives in `arbace/javalisp/` (namespaces `arbace.javalisp.*`). It follows the
example of go-lisp (`../go-lisp/golisp/DESIGN.md`, `SPEC.md`), which does the same for Go.

The long-term aim is a second parser inside a vendored javac that reads javalisp and
compiles it to the same class files as the Java it came from, byte for byte. So the
transcription keeps everything javac's tree holds, including the line of every position
javac records.

## 1. Principles

1. **Java words, Lisp shape.** Java keywords and operators head the forms (`class`, `if`,
   `for`, `new`, `+`, `==`). Forms that have no Java keyword use EDN keywords (`:var`,
   `:method`, `:cast`). An EDN keyword is never a Java identifier, so every identifier stays
   writable as itself. Identifiers are verbatim.
2. **Java order.** A form lists its parts in Java's source order. Only the operator moves
   in front.
3. **Same tree.** Java → javalisp → Java gives back the identical javac tree:
   - every node and field javac's parser produces, parentheses (`JCParens`) included
   - the line of every node position, block-closing `}` position and `var` type position
4. **Same lines.** Every token is printed on the line of the Java token it stands for. The
   javalisp file therefore has the same number of lines as the Java file, and diffs line by
   line against it. Its indentation is the Java line's indentation.
5. **Readable by Clojure.** Only EDN-compatible tokens are used, with no `^`, `@`, `~`, `'`
   or `#tag`. The data view loses only what EDN cannot hold: lines and comments, which are
   layout.

## 2. Lexical choices forced by the Clojure readers

| Java | javalisp | why |
|---|---|---|
| `a ^ b`, `^=`, `~a` | `(bit-xor a b)`, `bit-xor=`, `(bit-not a)` | `^` and `~` are reader macros |
| `/=` | `div=` | `/=` is not a valid symbol |
| `@A(x)` | `(:ann A x)` | `@` is a reader macro, `:@` is invalid |
| `T::m` | `(:ref T m)` | `::` is invalid inside symbols |
| `5L`, `1.5f` | `(long 5)`, `(float 1.5)` | the readers have no long or float suffixes |
| `'a'`, `'\n'` | `\a`, `\newline` | Clojure character literals |
| `'\uD800'` (a surrogate) | `(char 0xD800)` | the readers reject surrogate characters |
| `0b101`, `1_000`, `.5`, `1d`, hex floats | `2r101`, `1000`, `0.5`, `1.0`, decimal | value kept, spelling normalized |
| `"..."`, text blocks | Clojure strings with Clojure escapes | value kept, spelling normalized |
| `null`, `true`, `false` | `null`, `true`, `false` | |

Commas are whitespace. They are printed where Java has them in parameter lists and
declarator lists, purely for reading.

## 3. Names and selection

- `x`, `a.b.c`, `this`, `Outer.this`, `Foo.class`, `java.util.*` are symbols. A dotted
  symbol is a chain of field accesses (`JCFieldAccess`) on a name, exactly as javac reads it.
- `(m args...)` calls `m`, and `(a.b.m args...)` calls `m` on `a.b`. Explicit type arguments
  go in a vector right after the name: `(Collections.emptyList [String])`.
- `(.. base link...)` selects and calls on any other expression, in Java order:
  `foo().bar().baz` is `(.. (foo) (bar) baz)`. A link is one of:
  - `name`: a field
  - `(name args...)` or `([targs] name args...)`: a method call
  - `(new ...)`: an inner-class creation (`outer.new Inner()`)
  - `(:annotated anns... name)`: a type annotation in a qualified type (`Outer. @A Inner`)

  It is also used for name chains that are split across lines.

## 4. Forms

### Compilation unit

| Java | javalisp |
|---|---|
| `package a.b;` | `(package anns... a.b)` |
| `import a.B;`, `import static a.B.m;`, `import module m;` | `(import a.B)`, `(import static a.B.m)`, `(import :module m)` |
| `open module m { requires transitive static n; exports p to q; opens p; uses S; provides S with I; }` | `(:module anns... :open m (:requires :transitive static n) (:exports p :to q) (:opens p) (:uses S) (:provides S :with I))` |
| a compact source file's top-level methods and fields | written at top level |

### Declarations

| Java | javalisp |
|---|---|
| `public final class C<T> extends B implements I, J permits D { ... }` | `(class public final C [T] (extends B) (implements I J) (:permits D) members...)` |
| `interface I<T> extends J { ... }` | `(interface I [T] (extends J) members...)` |
| `enum E implements I { A, B(1) { ... }; ... }` | `(enum E (implements I) A (B 1 (class members...)) members...)` |
| `record R<T>(int x, T y) implements I { ... }` | `(:record R [T] [int x, T y] (implements I) members...)` |
| `@interface A { int v() default 1; }` | `(:annotation A (:method int v [] default 1))` |
| `private static final int a = 1, b[];` | `(:var private static final int a = 1, (b []))` |
| `<T> T f(final int x, String... r) throws E { ... }` | `(:method [T] T f [final int x, (String ...) r] (throws E) (do ...))` |
| constructor, compact canonical constructor | `(:method public C [int x] (do ...))`, `(:method public R (do ...))` |
| `abstract void g();`, `int h()[] { ... }` | `(:method abstract void g [])`, `(:method int h [] [] (do ...))` |
| `{ ... }`, `static { ... }` (initializers) | `(do ...)`, `(do static ...)` |

- Modifiers are the Java keywords, with `:sealed` and `:non-sealed`, and annotations
  `(:ann T args...)`. An annotation argument `x = v` is `(= x v)`, and `{a, b}` is `[a b]`.
- Type parameters go in a vector: `[T (U extends A B) ((:ann X) V)]`.
- Parameters go in a vector of `mods... type name` groups: `[final int x, (String ...) rest]`.
  A receiver parameter is `[C this]`.
- An empty vector after the parameters is a legacy array bracket: `int h()[]`.
- Method annotations written after the type parameters (`<T> @A R f()`) follow the vector.

### Statements

| Java | javalisp |
|---|---|
| `{ s1; s2; }`, `;` | `(do s1 s2)`, `()` |
| `if (c) a; else b;` | `(if c a b)` |
| `while (c) s`, `do s while (c);` | `(while c s)`, `(:do-while s c)` |
| `for (int i = 0, j; i < n; i++, j--) s` | `(for [(:var int i = 0, j)] (< i n) [(:++ i) (:-- j)] s)` |
| `for (;;) s`, `for (var x : xs) s` | `(for [] [] s)`, `(for [var x xs] s)` |
| `L: s`, `break L;`, `continue;` | `(:label L s)`, `(break L)`, `(continue)` |
| `switch (x) { case 1, 2: s; default: t; }` | `(switch x (case [1 2] s) (default t))` |
| `case A, B -> e;`, `case T t when g ->` | `(case [A B] -> e)`, `(case [(:var T t)] (:when g) -> ...)` |
| `synchronized (l) { ... }` | `(synchronized l (do ...))` |
| `try (R r = o()) { ... } catch (A \| B e) { ... } finally { ... }` | `(try [(:var R r = (o))] (do ...) (catch (\| A B) e (do ...)) (finally (do ...)))` |
| `return e;`, `throw e;`, `yield e;`, `assert c : m;` | `(return e)`, `(throw e)`, `(:yield e)`, `(assert c m)` |

A statement is either a single form or a block `(do ...)`, as in Java. The two branches of
`if` are its 2nd and 3rd forms. `else if` is a nested `if`.

### Expressions

| Java | javalisp |
|---|---|
| `a + b + c`, `a - b`, `a * b` | `(+ a b c)`, `(- a b)`, `(* a b)` (n-ary operators fold to the left) |
| `a == b`, `a < b` | `(== a b)`, `(< a b)` (comparisons take exactly two operands) |
| `-a`, `!a`, `~a`, `++a`, `a++` | `(- a)`, `(! a)`, `(bit-not a)`, `(++ a)`, `(:++ a)` |
| `a = b`, `a += b`, `a /= b` | `(= a b)`, `(+= a b)`, `(div= a b)` |
| `c ? a : b` | `(? c a b)` |
| `(T) e`, `e instanceof T` | `(:cast T e)`, `(instanceof e T)` |
| `e instanceof P(var x, T t)` | `(instanceof e (:record P (:var var x) (:var T t)))` |
| `a[i][j]` | `(:aget a i j)` |
| `new C<>(x) { ... }`, `new <T>C()` | `(new (C) x (class members...))`, `(new [T] C)` |
| `new int[n][]`, `new int[] {1, 2}`, `{1, 2}` | `(new (int [n] []))`, `(new (int []) [1 2])`, `[1 2]` |
| `x -> e`, `(x, y) -> { ... }`, `(int x) -> e`, `() -> e` | `(-> x e)`, `(-> (x y) (do ...))`, `(-> [int x] e)`, `(-> [] e)` |
| `String::valueOf`, `List<T>::new`, `int[]::new` | `(:ref String valueOf)`, `(:ref (:type (List T)) new)`, `(:ref (int []) new)` |
| `switch (x) { ... }` as an expression | `(switch x ...)` |
| `(e)` where Java does not need the parentheses | `(:paren e)` |

**Parentheses.** javac keeps parentheses in its tree, and they can change the generated
code: `(new C())::m` emits a null check that `new C()::m` does not. So javalisp keeps them,
but writes only the ones Java does not need. Where precedence requires them, Java printing
puts them back, by one rule shared by both directions (`arbace.javalisp.prec`). That rule
covers precedence and javac's parser folding:

- a `-` before a decimal integer becomes a negative literal
- adjacent string literals in a `+` chain become one string

### Types

| Java | javalisp |
|---|---|
| `int`, `a.b.C`, `C<T, U>`, `C<>` | `int`, `a.b.C`, `(C T U)`, `(C)` |
| `?`, `? extends T`, `? super T` | `?`, `(? extends T)`, `(? super T)` |
| `int[][]`, `String @A [] @B []`, `T...` | `(int [] [])`, `(String (:ann A) [] (:ann B) [])`, `(T ...)` |
| `A \| B`, `A & B` | `(\| A B)`, `(& A B)` |
| `@A T`, `Outer. @A Inner`, `C<T>.Inner` | `(:annotated (:ann A) T)`, `(.. Outer (:annotated (:ann A) Inner))`, `(.. (C T) Inner)` |

Patterns are `(:var mods type name)` (binding), `(:record T patterns...)` (record) and `_`.

## 5. Lines

Every token javalisp prints comes from a Java token, and it is printed on that token's line.
An opening bracket goes on the line of the first such token inside it. That alone puts
almost every position javac records on its line. The rest follows from a few defaults, each
with an explicit marker for the cases where the default is wrong:

| javac position | default | marker when it differs |
|---|---|---|
| a binary operator, `=`, `?`, `instanceof` | the line where the previous operand ends | the operator repeated before the operand: `(+ "a" \n + b)` |
| the `.` of a chain link | the link's line | a `.` before the link: `(.. (f) . \n (g))` |
| the `(` of a call | the method name's line | a `.` before the arguments: `(f \n . x)` |
| the `<` of a type application | where the type name ends | a `.` before the arguments: `(C . T)` |
| the `[` of an array access | where the previous item ends | a `.` before the index |
| a postfix `++`/`--` | where the operand ends | a `.` after it: `(:++ i .)` |
| the `(` of an enum constant's arguments or body | the constant's name | a `.` after the name |
| the `(` around an `if`/`while`/`switch`/`synchronized` condition | the keyword's line | a `.` before the condition |
| a block's or switch's `}` | the line after the last statement, or the opening line when the block opens and ends on one line | a **detached** closing paren: whitespace before it, on the `}`'s line: `(do (f) )` or a dangling `)` |

Neither the `.` nor the repeated operator is a valid operand, so a marker is never mistaken
for one.

## 6. Comments

- Each Java line's comments are kept, verbatim, at the end of the javalisp line:
  `;` followed by the comment text. A line whose comment text starts with `//` shows it as
  `;;`, so `// x` becomes `;; x`.
- A doc comment followed by code on its own line (`/** @deprecated */ void f()`) affects the
  class file: javac sets the deprecated flag from it. It is written inline, in front of the
  declaration, as a discarded string `#_"/** @deprecated */"`. Both Clojure readers skip it,
  and javalisp's own reader keeps it.

## 7. The tool

```sh
bin/javalisp -m arbace.javalisp.main java2clj X.java       # Java -> javalisp, to stdout
bin/javalisp -m arbace.javalisp.main clj2java X.clj        # javalisp -> Java, to stdout
bin/javalisp -m arbace.javalisp.main transcribe DIR OUT    # every .java under DIR -> OUT/**.clj
bin/javalisp -m arbace.javalisp.main check [-v] [-o] PATH... # round-trip check
bin/javalisp -m arbace.javalisp.main classes PATH...       # compile together, compare class bytes
bin/javalisp -m arbace.javalisp.main classes-each PATH...  # compile file by file, compare class bytes
```

`bin/javalisp` runs the vendored `clojure/` with access to the JDK's javac internals. javac
parses the Java (`arbace.javalisp.javac`), and `j2l` turns its tree into layout nodes, which
`layout` prints. In the other direction, `reader` reads javalisp with every token's line, and
`l2j` prints Java. `check` verifies each file with these steps:

1. parse with javac
2. transcribe, and read the result with `clojure.edn` and `clojure.core`
3. print back to Java, and parse that with javac
4. compare the two trees field by field, lines included
5. transcribe the printed Java again, and require the identical javalisp text

## 8. Known limits

- Within a line, Java's spacing and the place of comments are not kept. Comments move to the
  end of the line. Numeric and string literals keep their value, not their spelling.
- Comment delimiters written as unicode escapes (`*/`) are not recognized
  (`test/langtools/tools/javac/unicode/UnicodeCommentDelimiter.java`).
- Stray `;` between members are not in javac's tree, so they are lost. javac's doc-comment
  handling around them can differ
  (`test/langtools/tools/javac/depDocComment/DeprecatedDocComment3.java`).
