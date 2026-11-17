# The own platform's recorded inputs

JOP10 (proposed; doc/classes/JAVA-FRONTEND-SURVEY.md, "Own platform: what the switch needs"): two files `bin/jrt-convert`
takes from jdk26u's build tree (at `baf63fbe42b8758448fee570a9d9bb914272d259`, `build/linux-x86_64-server-release`) and Arbace
does not make yet. With the own pin (`jdk/javac/platform.sha256`'s `# kind: own`) the conversion reads these copies instead of
the build tree; with the build tree's pin, the build tree as before.

- `java.base/jdk/internal/lang/CaseFolding.java`: `support/gensrc/java.base/jdk/internal/lang/CaseFolding.java`, GenerateCaseFolding's
  output on the build's boot JDK (Unicode 16): javac's tool on Arbace runs Unicode 17 and keeps 28 entries fewer (JCS363 (a);
  JN7, JFS43). The runtime's copy stays the JDK 26 build's, the one Java's check compares with.
- `java.base/lib/security/cacerts`: `images/jdk/lib/security/cacerts`, GenerateCacerts' output (JCS140 (a), JCS233): the tool needs
  KeyStore and CertificateFactory, which javac's tool's world does not hold. Equal to `jdk/data/lib/security/cacerts`.
