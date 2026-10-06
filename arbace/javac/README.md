# arbace/javac: the vendored javac

This is javac, the `jdk.compiler` module of OpenJDK, vendored from
https://github.com/openjdk/jdk26u at commit `baf63fbe42b8758448fee570a9d9bb914272d259`
(the source of the JDK 26.0.2.1 this repository runs on). It is licensed under the GNU General
Public License version 2 with the Classpath Exception, as each file's header states; the full
texts are in the repository's `LICENSE.md`.

## What was taken

From `src/jdk.compiler/share/classes`:

| upstream | here |
|---|---|
| `com/sun/tools/javac/**` | `arbace/javac/**` (package `arbace.javac.*`) |
| `com/sun/source/**` | `arbace/javac/source/**` (package `arbace.javac.source.*`) |
| `com/sun/tools/doclint/**` | `arbace/javac/doclint/**` (package `arbace.javac.doclint`) |

Left out: `module-info.java` and `sun/tools/serialver` (a separate tool).

The packages are renamed because the JDK's own `jdk.compiler` module owns the original names, so
copies of them on the class path would be shadowed. The renaming is purely textual: in every
`.java` and `.properties` file, `com.sun.tools.javac` became `arbace.javac`,
`com.sun.tools.doclint` became `arbace.javac.doclint` and `com.sun.source` became
`arbace.javac.source`, in code, strings and comments alike. Nothing else in the upstream files
changed, apart from the hooks listed below.

## Generated sources

The JDK build generates three sources for this module. They were generated with the upstream
tools in `make/langtools/tools` at the same commit, from the unrenamed upstream inputs, and then
renamed like the rest. The results are identical to the JDK build's own
(`build/*/support/gensrc/jdk.compiler`):

- `resources/CompilerProperties.java`, `resources/LauncherProperties.java`:
  `propertiesparser.PropertiesParser -compile compiler.properties ... -compile launcher.properties ...`
- `code/FlagsEnum.java`: `flagsgenerator.FlagsGenerator Flags.java FlagsEnum.java`

The message bundles (`resources/*.properties`) stay resources: the JDK build compiles them into
`ListResourceBundle` classes with the same content, but `ResourceBundle` loads the
`.properties` files from the class path just as well. `bin/build-javac` writes
`resources/version.properties` (from `version.properties-template`) and the
`PlatformProvider` service registration (from `module-info.java`).

## Building

`bin/build-javac` compiles it with the system javac into `target/javac` (gitignored), and
`bin/javac` runs it.
