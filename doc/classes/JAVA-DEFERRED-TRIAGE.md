# Java's deferred cases, triaged by root cause (2026-11-05)

The REAP matrix's Java cells at `next` (`d86cb874a`, `test/reap/matrix.edn`): **eval 1,848 pass / 254 by
design / 1,220 deferred**, **AOT 1,921 / 160 / 1,241** (doc/FULL-SUPPORT-SURVEY.md, "REAP v3's divergences").
This triage asks what the 1,220 and 1,241 deferred tests really wait for, groups them by that root cause
rather than by the class `test/java/divergences.edn` files them under, and ranks the fixes by tests
regained per day of work. Nothing was run but the analysis of the committed records and sources (no build,
no corpus run). AGENDA item 4 of 2026-11-05; doc/classes/JAVA-CONFORMANCE-SURVEY.md (JC1-JC8, JCS1-JCS127)
is the background, and its JCS90-JCS99 ("the world decided") are the decisions this plan refines.

## Summary

- The divergence classes name the **first regex that matched a failure's message**, not its cause: `:security`
  (361 interpreted) is 198 tests of `javax.crypto` and key factories, 120 of certificates and keystores, 44 of
  JAAS, 34 of permissions; `:net` (233) holds 51 tests that stop at `FileInputStream.getChannel`; the compiled
  level's largest class, `:j2g-unresolved` (305), is not a translation problem at all: it is crypto 73, PKI 45,
  TLS 38, javac's API 31, the class-file API 26, ... (j2g refuses the test at the first unresolved type).
- Read by root cause, **one fix stands out: the JCA** (javax.crypto, SunJCE's ciphers, RSA, DSA and EC key
  factories and signatures, the key specs). It is the only thing missing for **176 interpreted and 168
  compiled tests**, about 7 days: 49 tests a day, twice the rate of anything after the first five.
- The **first five fixes** (JCA, java.util.logging, permissions with JAAS's core, PKI, the JCS96 small-members
  batch), **20.5 days**, unblock at most **404 interpreted and 386 compiled** tests; the **first eight**
  (adding charsets' coders, FileChannel, datagrams), **32.5 days**, at most **564 / 549**. With the record's
  calibration (below: about 70% of a test unblocked by the class scan passes once its classes exist), expect
  about **280-390** of the eight's 564 at eval: eval from 1,848 to about **2,130-2,240**, AOT from 1,921 to
  about **2,190-2,300**.
- Every fix built (24 of them, about 123 days) would leave **101 interpreted and 173 compiled** deferred tests,
  all of them needing something decided or proposed by design (JMX, the class-file API, serialization, XML,
  RMI, class loading) or the harness's own classes.
- **Reclassifications with no work** (Proposed, JCS128-JCS130): 36 interpreted and 108 compiled deferred tests
  also need a by-design feature (serialization 48 compiled, the class-file API 25/29, XML 21 compiled, ...) and
  can never pass; 18 and 18 need only JMX, which JCS95 (a) decided by design but `:management` still files as
  `:world`; 17 and 17 are the harness's (no JVM launcher, identity hash codes, clocks, thread order, no heap
  ceiling). Applying JCS128-JCS130 moves up to **71 interpreted and 143 compiled** tests from deferred to by
  design (or to pass, for the status-only ones), before any fix.
- **Since then** (2026-11-05, the user's JCS128-JCS135 as recommended): F1 prepared on `java-jc9-jca` for the night's
  conversion and run, and JCS128-JCS130's classes applied (Java eval 1,848 / 332 / 1,142, AOT 1,921 / 307 / 1,094):
  "As built: F1 (write-ahead)" at the end; JCS136, JCS137 proposed.

## 1. Method

**The classification, reproduced.** A Python copy of `test/reap/main.ae`'s `classify` (the first class of
`test/java/divergences.edn` that applies at the level, its `:results`, `:tests` or a `:match` regex finding
"RESULT REASON"; the `:kind-map` of `test/reap/cells.edn`) over `test/java/check-interp.edn` and
`check-compiled.edn` gives the matrix's numbers exactly: interp 1,848 / 254 / 1,220, compiled 1,921 / 160 /
1,241.

**The world.** The classes the executables have: every `ClassInfo :Name` of the gate's executable's
translated `jrt` package (`.tmp/gate-exe/3157cb94cd9b233a7208/amd64/j2g/gen/go/arbace/jrt`, the build of
`d86cb874a`) and of jrt's hand-written Go (`go/arbace/jrt/*.ae`): **2,637 classes**, and 240 more that exist
only as cut stubs (`j2g_cut.ae`: a type, no members).

**The static scan.** For each test, its source set (`test/java/ref-jdk26.edn`'s `:files`, read in place at
`/root/jdk26u/test/`, comments and strings removed): the JDK classes it names, by its imports (single and
on-demand), its fully qualified names, and `java.lang`'s and its on-demand packages' simple names that are
JDK classes (an index of jdk26u's `src/*/share|unix/classes` plus the world, 13,347 classes), less the classes
the test declares. A named class not in the world is **missing**. The scan is sound on the passing tests: of
the 1,848 that pass interpreted, one names a missing class (`java.lang.SafeVarargs`, an annotation; the
annotations are ignored).

**The reasons.** Each recorded non-pass names what it stopped at: `ClassNotFoundException: C`, `Unable to
resolve symbol: S (the import C)`, `Unknown type: S (the import C)`, `class C is not in the closed world`,
`No accessible method m in C`, `jrt lacks C.m(...)`, `C.m is not in the Go build`. Those name classes the scan
already has, and **members** the scan cannot see (`FileInputStream.getChannel`, `Charset.newDecoder`,
`Class.getAnnotation`, `URLEncoder.encode`, `NetworkInterface.getNetworkInterfaces`, `Provider.<init>`...).

**The causes.** Every missing class and member maps to one **root cause** (a cluster: one fix's scope;
`cluster()` in the scripts, by package and class, members by name). A test's causes are the union of its
scan's and its reason's; a test whose failure names no class or member (a wrong output, an exception of the
test's own, a timeout) keeps its divergence class as its cause (`class:jrt-dates`, `class:speed-hang`, ...).
A test needing several causes is counted under each. The columns below:

- **touches**: the deferred tests one of whose causes it is;
- **alone**: the tests it is the only cause of: unblocked if it alone were fixed;
- **+deps**: the tests unblocked by it with its prerequisites (PKI after the JCA, TLS after both);
- **design**: the tests it touches that also need a by-design feature (never regained).

**Calibration.** "Unblocked" means every class the test names exists; it can still fail on a member, a
behaviour or the time limit. Two measures of how far that holds: of the tests whose scan finds nothing
missing today, 83% pass interpreted and 86% compiled (the rest are the behaviour classes); JC6b, the last
widening of the world (java.io and zip, doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC6b"), regained 57
of `:io`'s 110 and 45 of `:zip`'s 68 tests, counted by touches, not by cause. This plan uses **0.7 × alone**
as the expected yield of a new area of the world, and the alone and +deps counts as upper bounds.

**Reproducing.** The scripts and their outputs are scratch, in this worktree's `.tmp/triage/` (to be copied
before the worktree is removed): `edn.py` (a reader for the Clojure data), `classify.py` (the matrix's
classification: `rows.json`), `scan.py` (the static scan: `scan.json`; inputs `jdk-classes.txt`, the
worktree's `.tmp/world-classes.txt`, `.tmp/cut-classes.txt`), `causes.py` (the causes: `causes.json`,
`tables.txt`), `plan.py` (the fixes, the greedy ranking: `plan.txt`), `classtab.txt` (§2's table).

## 2. The divergence classes, read by cause

Deferred tests per class (interp, compiled), with the causes found in them (a test counted under each of its
causes; "design-blocked": also needs a by-design feature).

| class | interp | compiled | the causes inside (interp, else compiled) |
|---|---:|---:|---|
| `:security` | 361 | 242 | crypto 198, PKI 120, JAAS 44, permissions 34, TLS 33, SASL/GSS 22 |
| `:j2g-unresolved` | - | 305 | crypto 73, PKI 45, TLS 38, javac-API 31, class-file API 26, selectors 22; 53 design-blocked |
| `:net` | 233 | 156 | selectors 80, file channels 51, datagrams 47, URL/HTTP 34, HTTP client 16, RMI 11 |
| `:j2g-world` | - | 95 | TLS 50, XML 16, JNDI 8, file channels 7, buffers 5; 22 design-blocked |
| `:nio` | 93 | 88 | charset coders 35, `java.nio.file` 28, file channels 14, buffers 13, javac-API 11 |
| `:reflection` | 80 | 74 | annotations and generics 60, reflection's metadata (canonical names, local classes, InnerClasses flags) 19 |
| `:management` | 65 | 45 | MXBeans 47, JMX 33 |
| `:tls` | 58 | 2 | TLS 58 |
| `:invoke` | 41 | 36 | `java.lang.invoke` 41 |
| `:javac-api` | 39 | 17 | javac's API 39, of which the class-file API too 20 |
| `:speed-hang` | 28 | 10 | interpreter speed (15 HotSpot compiler tests, 7 java.util) |
| `:logging` | 28 | 16 | `java.util.logging` 27 |
| `:threads` | 26 | 21 | Timer, thread groups, FieldUpdaters, FutureTask's and ForkJoinPool's members |
| `:j2g-edge` | - | 22 | serialization 10 (design), edge facts 4, file channels 4 |
| `:charsets-locales` | 20 | 22 | charsets jrt lacks (UTF-16/32 streams, EUC, Big5, KOI8), Lithuanian casing |
| `:interp-protected` | 19 | 8 | crypto 6 (Provider's protected constructor), the interpreter's protected members 6 |
| `:references` | 16 | 9 | PhantomReference, Cleaner, runFinalization |
| `:jrt-dates` | 13 | 13 | jrt's own Calendar and DateFormat |
| `:scripting`, `:naming`, `:random`, `:system-properties`, `:util-classes`, `:sql` | 47 | 37 | one cause each, as named |
| `:stack` | 9 | 8 | line numbers 7, StackWalker |
| the harness's (`:identity`, `:identity-sum`, `:environment`, `:clock`, `:jvm-tools`, `:thread-order`, `:memory-limit`, `:interp-synchronized`, `:flaky`, `:read-failed`) | 17 | 18 | the runner's, not Arbace's (JCS130) |
| the back ends' (`:interp-clone`, `-jls`, `-volatile`, `-extends`, `:repl-sibling`, `:j2g-go`, `:j2g-pp-override`, `:j2g-deep-stack`) | 16 | 5 | each a known finding (JC4b/JC4c follow-ups) |

## 3. Root causes

Interp / compiled in each cell. Days are one agent's, including the converter run (`bin/jrt-convert update`),
the edge (`bin/j2g-edge update`), the build and the corpus's area rerun; risk is that the estimate or the
yield is wrong.

| fix | root cause | touches | alone | +deps | design | days | risk |
|---|---|---:|---:|---:|---:|---:|---|
| F1 | JCA: javax.crypto, SunJCE's ciphers, KDFs, key agreement; RSA, DSA, EC; key specs, interfaces | 205 / 206 | **176 / 168** | 176 / 168 | 0 / 3 | 7 | medium |
| F2 | PKI: X.509, CertificateFactory, CertPath and PKIX, KeyStore (PKCS12, JKS), X500Principal | 122 / 123 | 63 / 63 | 76 / 76 | 0 / 1 | 5 (after F1) | medium |
| F3 | TLS: javax.net.ssl as SunJSSE translated (JCS131) | 95 / 96 | 58 / 58 | 87 / 88 | 0 / 1 | 8 (after F1, F2) | high |
| F4 | permissions (Permission's subclasses, PermissionCollection, AccessController, Policy, ProtectionDomain), JAAS's core (Subject, Principal, callbacks, login) | 86 / 90 | 49 / 45 | 49 / 45 | 0 / 4 | 3 | low |
| F4b | SASL mechanisms, GSS-API, Kerberos | 27 / 28 | 12 / 12 | 24 / 24 | 0 / 1 | 6 (after F1, F4) | high (JCS133) |
| F5 | java.util.logging, System.Logger | 34 / 37 | 27 / 26 | 27 / 26 | 0 / 4 | 1.5 | low |
| F6 | javac's API tests run on `arbace-javac` (JCS95 (c)'s check change) | 52 / 55 | 21 / 21 | 21 / 21 | 20 / 23 | 2 | low |
| F7 | FileChannel's members, FileLock, Channels, the stream's `getChannel`, MappedByteBuffer as a heap copy, buffer views (LongBuffer, read-only, direct as heap) | 78 / 84 | 62 / 66 | 62 / 66 | 0 / 0 | 5 | medium |
| F8 | charsets: CharsetEncoder/Decoder and CoderResult public, `newDecoder`/`newEncoder`/`availableCharsets`, UTF-16/32 stream coding, jdk.charsets' tables | 59 / 59 | 58 / 56 | 58 / 56 | 0 / 1 | 4 | medium |
| F9 | `java.nio.file`: FileSystems, walkFileTree, attribute views, WatchService (jrt's own subset widened) | 28 / 30 | 13 / 13 | 13 / 13 | 0 / 2 | 3 | medium |
| F10 | selectors, socket and datagram channels, Pipe, asynchronous channels over Go's net | 81 / 82 | 56 / 55 | 69 / 69 | 0 / 0 | 8 (after F7) | high |
| F11 | DatagramSocket, DatagramPacket, MulticastSocket, NetworkInterface | 47 / 48 | 38 / 38 | 38 / 38 | 0 / 1 | 3 | medium (no multicast in the sandbox) |
| F12 | URL and HTTP's small classes: URLEncoder/Decoder, Proxy, ProxySelector, HttpURLConnection, cookies, Authenticator, IDN | 41 / 39 | 37 / 35 | 37 / 35 | 0 / 1 | 3 | medium |
| F12b | java.net.http's client, com.sun.net.httpserver | 16 / 16 | 12 / 12 | 14 / 14 | 0 / 0 | 6 (after F3, F10) | high |
| F13 | java.lang.management's MXBeans over Go's runtime metrics | 49 / 52 | 31 / 31 | 31 / 31 | 0 / 3 | 3 | medium |
| F14 | annotations and generic signatures at run time (J2G-SPEC's R16 reversed), Parameter, AccessFlag, Proxy; reflection's metadata | 80 / 75 | 74 / 68 | 74 / 68 | 6 / 7 | 12 | high |
| F15 | `java.lang.invoke` over reflection | 43 / 40 | 41 / 39 | 41 / 39 | 1 / 0 | 7 (after F14) | high |
| F16 | JCS96's small members: Timer, ThreadGroup's members, FieldUpdaters, FutureTask's and ForkJoinPool's, ScopedValue, Scanner, RandomGeneratorFactory, the JVM's properties and Runtime.Version, String.transform, StringBuffer's capacity, `skip(-n)`, Math.pow's edges, rowsets' Serial*, Bidi's data | 63 / 56 | 63 / 54 | 63 / 54 | 0 / 0 | 4 | low |
| F17 | references: PhantomReference, Cleaner, runFinalization over Go's weak pointers and cleanups | 16 / 12 | 16 / 12 | 16 / 12 | 0 / 0 | 3 | medium |
| F18 | jrt's dates, case by case | 16 / 14 | 16 / 14 | 16 / 14 | 0 / 0 | 4 | medium |
| F19 | javax.script's API, JNDI's names (LdapName, Rdn, RefAddr) | 21 / 24 | 20 / 20 | 20 / 20 | 0 / 3 | 2.5 | low |
| F20 | StackWalker, line numbers in traces | 8 / 6 | 7 / 6 | 7 / 6 | 1 / 0 | 3 | medium |
| F21 | the back ends' findings (interp-protected, clone, volatile, extends, the REPL's order, j2g's findings, the edge's facts) | 19 / 18 | 19 / 17 | 19 / 17 | 0 / 0 | 5 | medium |
| F22 | interpreter speed (`:speed-hang`: over 600 s of CPU) | 28 / 8 | 28 / 8 | 28 / 8 | 0 / 0 | 15+ | high |
| (by design) | JMX (JCS95 (a)) | 34 / 37 | 18 / 18 | - | 0 / 3 | - | JCS128 |
| (by design?) | RMI (needs serialization) | 11 / 15 | 9 / 12 | - | 1 / 2 | - | JCS132 |

Not in any fix: the by-design features themselves, met inside deferred tests: the class-file API 25 / 29,
serialization 1 / 48, XML 2 / 21, class loading 8 / 5, JDI 0 / 4, smartcardio 0 / 2, FFM 0 / 1 (JCS129).

## 4. The ranked plan

Greedy by tests per day, each step taking its prerequisites; the gains are the deferred tests the step
unblocks (upper bounds, interp / compiled), the expected yield about 0.7 of them.

| # | fix | days | gain | tests/day | cumulative | at day |
|---:|---|---:|---:|---:|---:|---:|
| 1 | F1 JCA | 7 | 176 / 168 | 49.1 | 176 / 168 | 7 |
| 2 | F5 logging | 1.5 | 27 / 26 | 35.3 | 203 / 194 | 8.5 |
| 3 | F4 permissions, JAAS core | 3 | 49 / 49 | 32.7 | 252 / 243 | 11.5 |
| 4 | F2 PKI | 5 | 89 / 89 | 35.6 | 341 / 332 | 16.5 |
| 5 | F16 small members | 4 | 63 / 54 | 29.2 | 404 / 386 | 20.5 |
| 6 | F8 charsets | 4 | 58 / 56 | 28.5 | 462 / 442 | 24.5 |
| 7 | F7 file channels, buffers | 5 | 63 / 68 | 26.2 | 525 / 510 | 29.5 |
| 8 | F11 datagrams | 3 | 39 / 39 | 26.0 | 564 / 549 | 32.5 |
| 9 | F12 URL and HTTP small | 3 | 37 / 35 | 24.0 | 601 / 584 | 35.5 |
| 10 | F3 TLS | 8 | 94 / 94 | 23.5 | 695 / 678 | 43.5 |
| 11 | F6 javac's API on the tool | 2 | 21 / 21 | 21.0 | 716 / 699 | 45.5 |
| 12 | F13 MXBeans | 3 | 31 / 31 | 20.7 | 747 / 730 | 48.5 |
| 13 | F10 selectors, channels | 8 | 80 / 81 | 20.1 | 827 / 811 | 56.5 |
| 14 | F9 `java.nio.file` | 3 | 28 / 28 | 18.7 | 855 / 839 | 59.5 |
| 15 | F19 script, JNDI | 2.5 | 21 / 21 | 16.8 | 876 / 860 | 62 |
| 16 | F14 annotations, generics | 12 | 74 / 68 | 11.8 | 950 / 928 | 74 |
| 17 | F15 invoke | 7 | 41 / 40 | 11.6 | 991 / 968 | 81 |
| 18 | F17 references | 3 | 16 / 12 | 9.3 | 1,007 / 980 | 84 |
| 19 | F4b SASL, GSS | 6 | 26 / 26 | 8.7 | 1,033 / 1,006 | 90 |
| 20 | F18 dates | 4 | 16 / 14 | 7.5 | 1,049 / 1,020 | 94 |
| 21 | F21 back ends | 5 | 19 / 18 | 7.4 | 1,068 / 1,038 | 99 |
| 22 | F12b HTTP client, server | 6 | 16 / 16 | 5.3 | 1,084 / 1,054 | 105 |
| 23 | F20 stack | 3 | 7 / 6 | 4.3 | 1,091 / 1,060 | 108 |
| 24 | F22 speed | 15+ | 28 / 8 | 2.4 | 1,119 / 1,068 | 123 |

Left after all 24: 101 / 173, each needing JMX (34 / 37), the class-file API (25 / 29), serialization (1 /
48), XML (2 / 21), RMI (11 / 15), class loading, JDI, smartcardio, FFM or the harness. Steps 1-4 are one
area, the security half of JCS90 (16.5 days for up to 341 / 332), and TLS (step 10) closes it; the curve is
flat from step 5 to step 15 (17-29 tests a day), so their order there can follow other needs (F7 and F10 are
one NIO thread; F6 is a check change, not the world). F14, F15 and F22 are design work of their own (R16's
reversal, MethodHandles, the interpreter's speed) at a third of the rate.

**What the plan does not count.** Each widening also makes the read and print levels' tests of those areas
run (unchanged) and will surface the tests' second causes (a member, a behaviour): expect each area's rerun
to leave a tail in new, narrower classes, as JC6b's did.

## 5. The top fixes, designed

All the world fixes are one shape, the way JC6b added java.io and zip: jdk26u's own Java into the closure
(`test/jrt/jrt_sources.ae`: a new `def` of paths and its line in `sources`; a module other than java.base in
`added-module-sources`), jrt's own Java in `overlay/jdk/java.base` where jdk26u's needs what is cut, variants
in `overlay/jdk/variants/` for single members, then `bin/jrt-convert update` (javac's tool, heavy), `bin/j2g-edge
update`, the executables built, the area's tests run interpreted and compiled (`bin/java-check --levels
interp,compiled --only ...`), their records and classes updated, `bin/reap --record`. Judged by the area's
tests regained at both levels against this triage's alone and +deps counts, with no passing test lost
(`levels-known.edn`, the essential gate), and `bin/jrt-convert verify` byte for byte.

### F1 The JCA (7 days, 176 / 168 alone)

- **Scope.** javax.crypto (38 files, 14.5k lines: Cipher, CipherSpi, the cipher streams, KeyGenerator,
  SecretKeyFactory, KeyAgreement, KDF, SealedObject, EncryptedPrivateKeyInfo, NullCipher, the exceptions;
  `spec/` 19, `interfaces/` 5); `com/sun/crypto/provider` (100 files, 33.7k: AES, DES, DESede, Blowfish,
  RC2, ARCFOUR, ChaCha20, the modes (CBC, CTR, GCM, ...), padding, PBE, PBKDF2, DH, the key wrap); the
  java.security engines and SPIs still missing (KeyFactory, KeyPairGenerator, KeyPair, Signature,
  AlgorithmParameterGenerator, AlgorithmParametersSpi, the key interfaces), `java/security/spec` (36 files)
  and `interfaces` (20); `sun/security/rsa` (14: RSA keys, signatures, PSS, OAEP padding),
  `sun/security/provider`'s DSA and its key classes, `sun/security/ec` (17 plus `point/` 6, and the field
  arithmetic jdk26u's build generates, `IntegerPolynomialP256` and kin: a `gensrc` entry, made by
  `bin/jrt-convert`'s generate step with the build's `FieldGen`, as the CLDR data is), the DER and OID
  utilities of `sun/security/util` (DerValue, DerInputStream, DerOutputStream, ObjectIdentifier, KnownOIDs
  are there already) and `sun/security/x509`'s AlgorithmId and X509Key (the key encodings need them; the rest
  of x509 is F2's). About 85k lines, +18% on the closure's 460k.
- **What is cut today and must change.** The provider registry is jrt's own: `overlay/jdk/variants/SunJCE.ae`
  and `SunEntries`' variant register only the services the build implements, and
  `overlay/jdk/java.base/com/sun/crypto/provider/JceServices.java` and `sun/security/provider/JrtServices.java`
  construct them by name (no class loading). F1 extends the three to SunJCE's, SUN's and SunRsaSign's and
  SunEC's full service lists, `ProviderConfig`'s variant making SunRsaSign and SunEC directly beside SUN and
  SunJCE. `JceSecurity` (overlay, from its template) keeps the unlimited policy. DESKey, DESedeKey, PBEKey,
  PBKDF2KeyImpl and GaloisCounterMode register a Cleaner to zero key bytes: variants without it (as ZipFile's,
  JC6b) until F17. ChaCha20, GHASH, GaloisCounterMode use `MethodHandles.byteArrayViewVarHandle`, which j2g
  already compiles directly (ByteArrayAccess, SHA3 are in the closure). Provider's protected constructor
  (`:interp-protected`'s 6 crypto tests, the interpreted subclasses of Provider) needs Provider among the
  extendable classes (`arbace.j2g.model/class-supers`), with Cipher, CipherSpi and Mac (JC4b's mechanism).
- **Risks.** j2g's and repr/java's first contact with 85k lines (JC6b's 10k found 4 j2g findings); EC's
  generated sources; the interpreted KAT loops' speed (the providers are compiled, the tests' loops
  interpreted: some of `com/sun/crypto/provider`'s may join `:speed`); the executable's size (JCS135).
- **Judged by** `jdk/com/sun/crypto`, `jdk/javax/crypto`, `jdk/sun/security/{rsa,ec,provider}`,
  `jdk/java/security/{KeyFactory,KeyPairGenerator,Signature,spec,Provider}` interpreted and compiled: 176 / 168
  alone at most, about 120 expected.

### F5 java.util.logging (1.5 days, 27 / 26)

- **Scope.** java.logging's 19 files (9.9k lines) and `sun/util/logging/internal/LoggingProviderImpl`, an
  `added-module-sources` entry; `conf/logging.properties` into `jdk/data` (as `java.security` is,
  `bin/jrt-convert`'s data step); `System.getLogger`'s LoggerFinder bound to it directly (no ServiceLoader).
- **Cut.** `LogRecord.inferCaller` walks the stack with StackWalker (absent until F20): a variant that walks
  `Throwable.getStackTrace` (jrt's frames), or answers no caller; LogManager's handler classes named in the
  configuration made by a table of the closure's handlers (no class loading by name); `LoggingMXBean` and
  `MemoryHandler`'s management parts left out (F13).
- **Judged by** `jdk/java/util/logging` (26 of the 27 alone).

### F4 Permissions and JAAS's core (3 days, 49 / 45)

- **Scope.** java.security's permission classes (BasicPermission, Permissions, PermissionCollection,
  AllPermission, SecurityPermission, UnresolvedPermission, the cut `Permission` made whole), AccessController
  and AccessControlContext (since JDK 24 they only run the action: no SecurityManager), Policy, ProtectionDomain,
  CodeSource, DomainCombiner, PrivilegedActionException; java.io.FilePermission, java.net.SocketPermission,
  NetPermission, java.util.PropertyPermission, RuntimePermission's members, ReflectPermission;
  javax.security.auth's Subject, PrivateCredentialPermission, AuthPermission, `callback/` (11), `login/` (14),
  `com.sun.security.auth`'s principals and TextCallbackHandler. All plain Java, about 15k lines.
- **Cut.** `Class.getProtectionDomain` (one test) answers a fixed domain; the login modules that reach JNDI or
  Kerberos stay out (F19, F4b).
- **Judged by** `jdk/javax/security/auth`, `jdk/com/sun/security/auth`, `jdk/java/security/BasicPermission`,
  `jdk/java/io/FilePermission`, `jdk/java/net/SocketPermission`.

### F2 PKI (5 days after F1, 89 / 89 with F1)

- **Scope.** `java/security/cert` (51 files), `sun/security/x509` (72), `sun/security/pkcs` (10) and `pkcs12`
  (3), `sun/security/provider/certpath` (37: PKIX validation and building, without the OCSP and LDAP fetchers'
  network paths), KeyStore and its SPI with the JKS, PKCS12 and DKS implementations, X500Principal,
  `sun/security/provider/X509Factory`, `JarEntry.getCertificates` and JarVerifier verifying for real (jrt's
  verifies nothing, JC6b). About 55k lines.
- **Cut.** The cacerts keystore: `lib/security/cacerts` into `jdk/data` from jdk26u's build (as `tzdb.dat`),
  or an empty one (the tests that read it are few); revocation checking's network fetches fail as offline.
- **Judged by** `jdk/java/security/cert`, `jdk/java/security/KeyStore`, `jdk/sun/security/{x509,pkcs,pkcs12}`,
  the signed-jar tests of `jdk/java/util/jar` (`:security`'s t05737, t05742).

### F16 The small members (4 days, 63 / 54)

JCS96 (c)'s batch, which the user deferred (b) on 2026-10-26: Timer and TimerTask (two files), ThreadGroup's
members over jrt's threads, `Thread.ofPlatform`, `getAllStackTraces`, `activeCount`, the
Atomic*FieldUpdaters (over the member tables' fields: the j2g side is reflection by name, as Field.get), FutureTask's
protected `set`, `done`, `runAndReset` (an extendable class's protected members, JC4b), ForkJoinPool's
`schedule` and `getStealCount`, ScopedValue, Scanner, PropertyResourceBundle, RandomGeneratorFactory with a
static list of the algorithms (no ServiceLoader), `java.version`, `java.vm.specification.vendor`,
`java.runtime.version` and Runtime.Version, String.transform, StringBuffer's capacity and an append's bounds,
FileInputStream.skip(-n), Math.pow's last place (StrictMath's fdlibm for Math.pow), javax.sql.rowset.serial's
SerialBlob and SerialClob, Bidi's ICU data. Each a class of jdk26u's or a member of jrt's; judged by the
classes `:threads`, `:util-classes`, `:random`, `:system-properties`, `:jrt-strings`, `:jrt-io`,
`:jrt-math`, `:sql`, `:text-data`.

### F8 Charsets (4 days, 58 / 56)

- **Scope.** jrt's charsets are its own (three plus three, J2G-SPEC §12): CharsetEncoder, CharsetDecoder,
  CoderResult and CodingErrorAction public as jdk26u's (they are cut: `newDecoder`, `newEncoder`, `encode`,
  `decode` and `availableCharsets` are missing members), jdk26u's `sun.nio.cs` coders for UTF-16 (BE, LE,
  with the BOM), UTF-32, the ISO-8859 family, KOI8 and the Windows code pages (table driven, generated by the
  build's `charsetmapping` tool from `make/data/charsetmapping`: a `gensrc` entry), then jdk.charsets' EUC,
  Big5, GB18030 and JIS tables (the same tool), with their aliases; the streams' coding (`no stream coding
  for UTF-16`) through the coders.
- **Risk.** The generated tables' size (jdk.charsets is about 3 MB of source); the order: the Unicode coders
  and the coders' API first (most of `jdk/java/nio/charset`), jdk.charsets second (most of `jdk/sun/nio/cs`,
  34 tests).
- **Judged by** `jdk/sun/nio/cs`, `jdk/java/nio/charset`, `:charsets-locales`.

### F7 File channels and buffers (5 days, 62 / 66)

- **Scope.** jrt's FileChannel exists, cut (no `close`, `isOpen`, `lock`, `map`, `transferTo`): its members
  over jrt's file table (RandomAccessFile's natives, JC6b), `getChannel` of FileInputStream, FileOutputStream
  and RandomAccessFile (24 tests stop there at either level), FileLock (Go's `syscall.Flock`/`fcntl`),
  FileChannel.MapMode with `map` as a heap copy written back on `force` and at unmap (MappedByteBuffer: no
  mmap in the sandbox's model; READ_ONLY maps exact), Channels' stream adapters, the channel exceptions;
  java.nio's buffer views jrt lacks (LongBuffer, ShortBuffer, Float/DoubleBuffer whole, the read-only heap
  buffers HeapByteBufferR/HeapCharBufferR, BufferMismatch), `allocateDirect` as a heap buffer (`isDirect`
  true).
- **Risk.** `map`'s semantics for tests that expect two maps of a file to share; those stay in a class.
- **Judged by** `jdk/java/nio/channels/FileChannel`, `jdk/java/nio/MappedByteBuffer`, `jdk/java/nio/Buffer`,
  `jdk/java/io/{FileDescriptor,FileInputStream,RandomAccessFile}`.

### F11 Datagrams (3 days, 38 / 38)

DatagramSocket and DatagramPacket over Go's `net.UDPConn` (jrt's sockets are TCP), MulticastSocket over
`net.ListenMulticastUDP` with the joins, NetworkInterface over `net.Interfaces` (`getNetworkInterfaces`,
`networkInterfaces`, addresses, flags), PortUnreachableException, StandardProtocolFamily. The tests mostly use
the loopback; those needing a multicast route fail in the check's sandbox the same on the JDK (they pass there:
the JDK's host has one), so a few will stay, in `:environment`. Judged by `jdk/java/net/{DatagramSocket,
DatagramPacket,MulticastSocket,NetworkInterface}`.

## As built: F4 (write-ahead)

Branch `java-jc9-pki` (from `java-deferred-triage`, JCS128-JCS139 decided), 2026-11-05, F4 then F2 prepared
by day under the light rules, as F1 (`java-jc9-jca`) and F5 (`java-jc9-logging`) were: the closure's lists,
jrt's own Java, the variants, j2g's tables, the generate step, and light checks only. Nothing converted,
translated or built. **F4 needs F1** (jca-sources holds `Permission`, `PermissionCollection`, `Guard` and
`SecurityException`, which F4's classes extend and throw) **and comes after F5** (LoggingPermission, which F5
left to F4): its closure was computed against the world plus F1's and F5's lists as their branches hold them
(by reference: `permission-sources` names none of their files). The branch's hunks in the shared files are
apart from F1's and F5's (`git merge-tree` of this branch with each: no conflict; F1 with F5 do conflict, in
`bin/jrt-convert`, `test/jrt/jrt_sources.ae` and this file: "Findings", below). **The branch is not mergeable before its
night's conversion**, as F1's: its variants name classes `jdk/forms` does not hold yet (Subject, X509Factory ...:
"a variant for a class not in the inputs", an analysis failure), and `Provider.ae`'s now name
`Policy.Parameters`, `Configuration.Parameters` and jrt's `PkiServices`, which only the conversion brings.

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `permission-sources`, one
line in `sources`):

| part | how |
|---|---|
| java.security's permission machinery (25) | jdk26u's as it is since the Security Manager left (JDK 24): `AccessController` runs the action and `checkPermission` throws `AccessControlException` always, `AccessControlContext` and `ProtectionDomain` hold what they are given, `Policy.getPolicy` grants nothing, `setPolicy` throws: the unlimited model F1's `JceSecurityManager` takes for the crypto policy, with no variant. `BasicPermission`, `Permissions`, `AllPermission`, `SecurityPermission`, `UnresolvedPermission` (and its collection), `CodeSource`, `DomainCombiner`, `GuardedObject`, `PolicySpi`, `URIParameter`, the privileged actions and `PrivilegedActionException`, `Principal`; the deprecated identities (`Identity`, `IdentityScope`, `Signer`, the old `java.security.Certificate`: `jdk/java/security/IdentityScope`) |
| the other permissions (8) | `java.io.FilePermission` (variant), `java.net.SocketPermission`, `NetPermission`, `URLPermission` with `HostPortrange`, `java.util.PropertyPermission`, `RuntimePermission` (a cut class until now), `ReflectPermission`, and `java.util.logging.LoggingPermission` (KIND `share:java.logging`) |
| JAAS's core (31) | javax.security.auth's `Subject` (variant), `SubjectDomainCombiner`, `AuthPermission`, `PrivateCredentialPermission`, `Refreshable`; `callback/` (10, `PasswordCallback`'s variant); `login/` (13: `LoginContext`, `Configuration`, variants; the exceptions); `spi/LoginModule`; `sun.security.provider.ConfigFile`, the configuration file's parser; `sun.security.util`'s `ResourcesMgr`, `Password` (variant), `ConsoleCallbackHandler`, `PendingException`, `RegisteredDomain` (variant) |
| jdk.security.auth (15, KIND `share:jdk.security.auth`) | the principals (Unix, NT, `UserPrincipal`, `PrincipalComparator`; not `LdapPrincipal`, javax.naming's, F19), `TextCallbackHandler`, the public `ConfigFile`. No login module: `UnixLoginModule`'s `UnixSystem` is native, `KeyStoreLoginModule` is F2's (it needs KeyStore), the JNDI, LDAP and Kerberos ones F19's and F4b's |
| `sun.net.PortConfig` (KIND `unix`) | variant: Linux's ephemeral port range read from `/proc/sys/net/ipv4/ip_local_port_range` as libnet's `PortConfig.c` reads it, no native library (SocketPermission's port 0) |
| the messages (2, KIND `gensrc`) | `sun.security.util.resources.security` and `auth`, which ResourcesMgr reads (Subject's, the principals', LoginContext's and ConfigFile's texts): generated by `bin/jrt-convert`'s generate step with the build's `build.tools.compileproperties.CompileProperties` (`make/modules/java.base/Gensrc.gmk`'s `SECURITY_RESOURCES`), the root locale's alone (JCS139 (b)); javac's tool gains the tool (`bin/javac-vendor`'s `tool_dirs` + `compileproperties`; `gen_tools`) |
| the variants (8) | `Password.ae` (no console: `java.io.Console`, `JdkConsoleImpl`, the charset encoders and `JavaLangAccess.initialSystemIn` are not in the Go build; the stream read as the JDK reads it without a console), `PasswordCallback.ae` (no Cleaner, as F1's keys: cleared when replaced or cleared, F17), `Subject.ae` (`current` and `callAs` over a ThreadLocal, not a ScopedValue), `LoginContext.ae` (the login module by `Class.forName` alone, no ServiceLoader), `Configuration.ae` (`ConfigFile` constructed directly when no other class is configured), `FilePermission.ae` (paths by jrt's `Path.of`, no FileSystem), `PortConfig.ae`, `RegisteredDomain.ae` (no public suffix list, as the JDK without `lib/security/public_suffix_list.dat`; `DomainName` stays out, it logs through `SSLLogger`) |
| j2g | renames `com/sun/security/auth/login/ConfigFile` → `Auth_ConfigFile` (sun.security.provider's namesake), `java/security/Certificate` → `Security_Certificate` (java.security.cert's, F2's); `class-supers` + `BasicPermission`, `Identity`, `IdentityScope` (the classes F4's tests extend; F2's beside them, below) |
| the provider | `Provider.ae` (the world's variant, not F1's): the Policy and Configuration engines take `Policy.Parameters` and `Configuration.Parameters` again (the JDK's); SUN's `JavaLoginConfig` service is registered with F2's (`SunEntriesPki.ae`, below) |

Left out, with their tests: `Class.getProtectionDomain` (jrt's hand-written `Class`: `Proxy/Basic1`,
`compiler/c1/TestConcurrentPatching`; a fixed domain, the JDK's for a boot class, `ProtectionDomain(null,
AllPermission's collection)`, is a small day's change to `go/arbace/jrt/class.ae` once the class is translated:
not done by day, it needs the converted constructor's Go name), the LDAP principal and login module
(`CreateLdapPrincipals`, `LdapLoginModule/CheckOptions`: javax.naming, F19), `UnixLoginModule` (`AllPlatforms`:
a native `UnixSystem`; jrt's own over the host's uid and gids would be a follow-up). What stays outside the world
in members no variant replaces becomes j2g's operation-level stubs, unreachable in the tests: the
serialization members (`readObject`, `writeObject`, `serialPersistentFields` of the permissions, Subject and
the principals: J2G-SPEC §12, by design), `CodeSource`'s and `UnresolvedPermission`'s certificate members until
F2 (same branch), `SocketPermission.checkForIDN` for `xn--` names (`java.net.IDN`, `:net-http`'s).

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 2 GiB; scratch in the worktree's `.tmp/f4/`):
- the closure (F1's `closure.py` with the other modules' roots and the world extended by F1's and F5's lists
  and overlays): 86 files, 20,813 lines (69 `share`, 1 `unix`, 15 `share:jdk.security.auth`, 1
  `share:java.logging`) and the 2 generated bundles, 90 lines; what it names outside the world and the list,
  each accounted for above;
- `jrt.jrt-sources/print-sources`: the committed list plus exactly those 88 lines (907 → 994 with the
  generate step's `jrtgen` line);
- the read: `bin/repr-java read-jdk --check` of the 88 files as a patch of java.base, as `bin/jrt-convert`'s
  convert step reads them: 88 converted, 0 failed, 0 javac errors, 0 unreadable (9 s, 0.6 GB); `--no-javac`:
  88 native, 0 fallbacks, the forms identical;
- the generator: jdk26u's `CompileProperties` run on the JDK with the build's arguments: `security.java` and
  `auth.java` equal to the build's `support/gensrc/java.base/sun/security/util/resources/` byte for byte;
- the variants: `arbace.j2g.world/read-variants` and `apply-variant` on the converted forms, each class then
  parsed by `arbace.classes.parse/parse-class` (`.tmp/f4/vet.ae`): Password 2/0/0 (replaced/cut/added; the
  nested ConsoleHolder emptied), PasswordCallback 3/1/0, Subject 2/1/1, LoginContext 1/1/0, FilePermission
  5/1/0, PortConfig 3/0/1, RegisteredDomain 1/0/0, Configuration 1/0/0, all parsed;
- javac: F1's 335 sources and 10 generated fields, its overlays, F4's and F2's sources and this branch's
  overlays, 661 files, compiled by the JDK's javac with `--patch-module` (java.base, then jdk.security.auth and
  java.logging against it): 0 errors, 1,137 classes;
- Go names: every class of the new forms (with the nested ones, `Outer_Inner`) against the gate executable's
  Go types, jrt's hand-written ones and F1's and F5's new classes: the collisions are the renames above.

**JCS135, the closure's growth**: +20,903 Java lines (**+4.3%** on the closure's 483,461), the forms +340,685
bytes (**+3.1%** on `jdk/forms`' 11,161,667); by JC6b's measure (10k lines, +1.63 MB) about +3.4 MB on the
executable.

## As built: F2 (write-ahead)

On the same branch, after F4. **F2 needs F1 and F4**: its closure was computed against the world plus F1's,
F5's and F4's lists (by reference again: `pki-sources` holds none of their files; its certificates' keys,
signatures and `AlgorithmId` are F1's, its `CodeSource`, `AuthProvider`'s login and principals F4's), and its
variants read F1's `jdk.internal.jrt.CryptoAccess` and follow F1's `Signature.ae` and `SunEntries.ae`, so the
branch merges after `java-jc9-jca`.

**What comes in, and how** (`test/jrt/jrt_sources.ae`'s `pki-sources`, one line in `sources`):

| part | how |
|---|---|
| java.security.cert (49) | the whole package but `CertStoreParameters` (in the world since the security basics): CertificateFactory, X.509's certificates and CRLs, CertPath, PKIX's validator, builder, parameters and revocation checker, trust anchors, cert stores, selectors |
| sun.security.x509 (68), pkcs (8), pkcs12 (3), timestamp (4) | all of them but F1's four (`AlgorithmId`, `AlgIdDSA`, `X509Key`, `NamedX509Key`) and F1's `PKCS8Key`, `NamedPKCS8Key`; `PKCS12KeyStore`'s variant clears its key specs through F1's `CryptoAccess` |
| sun.security.provider.certpath (37) | PKIX's validation and building, the revocation checker with CRLs and OCSP (`OCSP.ae`: `getOCSPBytes` throws the IOException of an unreachable responder: no `HttpURLConnection` in the Go build), the cert stores; not `ssl/` (F3) |
| the key stores | `java.security.KeyStore` and its SPI and entries, SUN's `X509Factory`, `JavaKeyStore` (JKS, CaseExactJKS), `DomainKeyStore` (DKS, with `PolicyParser`), `KeyProtector`; the JCEKS key store F1 left out (`JceKeyStore`, its `KeyProtector`, `JceKeyProtector.ae`, `SealedObjectForKeyProtector`: its secret-key entries are serialized SealedObjects, by design, J2G-SPEC §12; its private keys and certificates are not); `KeyStoreLoginModule` (KIND `share:jdk.security.auth`), F4's login over the key store |
| java.security, javax.security.auth.x500 | `CodeSigner`, `Timestamp`, `PKCS12Attribute`, `DomainLoadStoreParameter`, `AuthProvider`, `KeyStoreException`; `X500Principal` (variant), `X500PrivateCredential`. Not `PEMEncoder`/`PEMDecoder` (a preview API, JDK 25's JEP 470: the tests need `--enable-preview`) |
| sun.security.util (13), validator (11), tools (1) | what PKI uses: `SignatureUtil` (variant), the algorithm constraints, `AnchorCertificates`, `UntrustedCertificates`, `Cache`, `KeyStoreDelegator`, `HostnameChecker` (its TLS members stubs until F3), `FilePaths`, `PolicyUtil`; the validators PKIX's `AlgorithmChecker` names; `KeyStoreUtil` (PKCS12KeyStore's `isSelfSigned`) |
| jdk.internal | `X509CertificateEvent`, `X509ValidationEvent` (never enabled: no JFR), `ThreadTracker` (the builder's guard against fetching AIA recursively), the accesses `JavaxSecurityAccess`, `JavaSecuritySignatureAccess`, `JavaxCryptoSealedObjectAccess` |
| jrt's own (overlay, 3) | `jdk.internal.event.EventHelper` (never logs: System.Logger is not in the Go build, JCS138; jdk26u's signatures, which `JCAUtil.tryCommitCertEvent`, in the world since the security basics, and `PKIXCertPathValidator` call), `jdk.internal.jrt.PkiAccess` (SharedSecrets' JavaxSecurityAccess and JavaSecuritySignatureAccess, as F1's CryptoAccess), `sun.security.provider.PkiServices` (the PKI services and JavaLoginConfig constructed directly, after JrtServices, with the JDK's choice of constructor: CertStoreParameters, Configuration.Parameters) |
| the variants (9) | `SignatureAccess.ae` (Signature's static initializer, which F1's `Signature.ae` cuts, added back giving its access to PkiAccess: the file sorts after `Signature.ae`, and j2g applies a class's variant members file by file in that order), `X500Principal.ae`, `X500Name.ae`, `SignatureUtil.ae` (the accesses through PkiAccess), `JceKeyProtector.ae`, `PKCS12KeyStore.ae` (through F1's CryptoAccess), `OCSP.ae`, `Sun.ae` with `SunEntriesPki.ae` (SUN registers, after F1's SunEntries constructor, cut after the digests, the rest of jdk26u's list in its order: the X.509 certificate factory, PKCS12, JKS, CaseExactJKS and DKS, the Collection and IndexedCollection cert stores, JavaLoginConfig, PKIX's builder and validator) |
| j2g | renames `java/security/cert/Extension` → `Cert_Extension`, `sun/security/x509/Extension` → `X509_Extension` (and sun.util.locale's `Extension`), `java/security/Timestamp` → `Security_Timestamp` (java.sql's), `sun/security/util/Event` → `Util_Event` (jdk.internal.event's), `sun/security/pkcs/EncryptedPrivateKeyInfo` → `Pkcs_EncryptedPrivateKeyInfo` (javax.crypto's), `com/sun/crypto/provider/KeyProtector` → `Jce_KeyProtector` (sun.security.provider's); `class-supers` + `java/security/cert/Certificate`, `X509Certificate`, `CRL`, `java/security/KeyStoreSpi` |

Left out: jar verification (jrt's `JarVerifier` and `ManifestEntryVerifier` verify nothing, JC6b; JCS141), the
trust anchors' keystore `lib/security/cacerts` (JCS140), keytool and jarsigner (no test of the areas runs their
classes in process: the ones that use them start them as tools, `:jvm-tools`, by design). What stays outside the
world in members no variant replaces, operation-level stubs: serialization (J2G-SPEC §12), `URICertStore`'s
`HttpURLConnection` test (reached only when a fetched CRL is fetched again; the fetch itself fails as offline
in the sandbox), TLS's `SSLSession`, `SSLScope`, `SNIHostName` and `SSLLogger` in `Cache`,
`DisabledAlgorithmConstraints` and `HostnameChecker` (F3), `IDN` in `HostnameChecker`, ServiceLoader in
`KeyStoreUtil`'s provider loading (keytool's).

**Light checks done** (as F4's; scratch in `.tmp/f2/`):
- the closure: 219 files, 71,353 lines (218 `share`, 1 `share:jdk.security.auth`), and the 3 overlays, 191
  lines; `print-sources`: the committed list plus F4's 88 and these 222 lines (907 → 1,216 with the `jrtgen`
  line);
- the read of the 219 files and the 3 overlays: 222 converted, 0 failed, 0 javac errors, 0 unreadable (13 s,
  1.1 GB); `--no-javac`: 222 native, 0 fallbacks, the forms identical;
- the variants applied and parsed: Signature 0/1/1 (F1's cut, then the added initializer: F1's `Signature.ae`
  and `SignatureAccess.ae` together, on F1's converted Signature), X500Principal 1/0/0, X500Name 2/0/0,
  SignatureUtil 3/0/0, KeyProtector 1/0/0, PKCS12KeyStore 1/0/0, OCSP 1/0/0, and on the world's forms Sun
  1/0/0, SunEntries 1/0/1 (with the world's `SunEntries.ae` and with F1's), Provider 1/0/0 with
  `Provider$Service` 1/0/0;
- javac and Go names: with F4's (above).

**JCS135, the closure's growth**: +71,544 Java lines (**+14.8%**), the forms +1,479,848 bytes (**+13.3%**);
about +11.7 MB by JC6b's measure. JC9 whole (F1 +96,773, F5 +9,697, F4 +20,903, F2 +71,544): **+198,917 lines,
+41.1%** on 483,461, the forms about **+5.0 MB, +45%**, the executable by the forms' share some +32 MB (15%) on
the gate build's 206.9 MB, and a j2g run some 35-45% longer on the JDK's part: JCS135 (a)'s threshold, a minute
more of build, is to be measured on the night of F1 and again on this one.

**Expected yield** (the triage's counts, the scan of `d86cb874a`): F4 49 interpreted / 45 compiled tests at most
(its causes alone, F1 and F5 assumed), of which 5 stay out by the cuts above (`Proxy/Basic1`,
`TestConcurrentPatching`, `CreateLdapPrincipals`, `CheckOptions`, `AllPlatforms`), about 34 / 31 expected
(0.7); at risk `PasswordCleanup` (the Cleaner), the interpreted tests' own `Permission` subclasses (BasicPermission
listed) and login modules (an interpreted class is not in jrt's registry). F2 89 / 89 at most with F1 and F4, of
which about 7 stay out (`CertsMatch`, `GetMethodsReturnClones`, the two `ScanSignedJar`, `TurkCert`: jar
verification; `VerifyCACerts`, `TestCACerts`: cacerts), the OCSP tests (`AIACheck`, `OcspUnauthorized`) at risk,
about 62 / 62 expected. With F1's and F5's, JC9's 341 / 332 at most and about 240 / 230 expected stand.

**The night's commands**, in the worktree after `next` holds F1 and F5 (merge `next` into `java-jc9-pki`
first: F2's variants and SunEntriesPki follow F1's files), heavy ones under `bin/heavy-lock`; about 3-4 hours
with the check:

```sh
cd /root/arbace/.tmp/worktrees/java-jc9-pki
git merge next      # F1 and F5 (their nights' conversions, jdk/ and jdk/javac/ committed)
# 1. javac's world gains the JDK build's CompileProperties (buildtools, make/jdk/src/classes/build/tools/
#    compileproperties): expect jdk/javac/sources.txt +1 line ("buildtools make build/tools/compileproperties/
#    CompileProperties.java", beside langtools' compileproperties/CompileProperties.java: its classes are named
#    Jv_... by package, no collision), jdk/javac/forms +1 file, edge.edn alike or a few entries more
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  bin/heavy-lock bin/javac-vendor convert
# 2. javac's tool with it
bin/heavy-lock bin/arbace-go --build-java
# 3. jdk/ converted afresh: expect "1582 files (before the locales): 1437 from ...share/classes (F1's 1150 + 287),
#    7 unix, 43 generated (41 + security.java, auth.java), 58 of jrt's own", the generate step's two bundles
#    :same as the build's gensrc (43 generated equal), 0 failed, 0 javac errors, no javac: 0 fallbacks;
#    jdk/forms + 106 (F4) and 240 (F2) files
REPR_JAVA_EXE=$PWD/target/arbace-go/amd64/arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 4. j2g's edge afresh (the new classes' outside references: the object streams, HttpURLConnection, URLEncoder,
#    IDN, SSLSession, SSLScope, SNIHostName, SSLLogger, ScopedValue, Console, ServiceLoader, CleanerFactory)
bin/heavy-lock bin/j2g-edge update
# 5. javac's world's edge less the new jdk/edge.edn
bin/heavy-lock bin/javac-vendor edge
# 6. the executable (JCS135: its size against the F1+F5 night's, `ls -l`; the j2g and Go times); j2g's report:
#    the variants' counts above, "a variant for a class not in the inputs" for none
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 7. a smoke of F4 and F2 on it: true true [UserPrincipal: u] sun.security.provider.ConfigFile, then
#    "OU=isrg, OU=labs, O=sun, C=us", 1 (a PKCS12 round trip), [SUN ... X.509 ...] and a CertPathValidator for PKIX
target/arbace-go/amd64/arbace -e '(import (quote (java.io FilePermission FileInputStream ByteArrayOutputStream ByteArrayInputStream)) (quote (java.net SocketPermission)) (quote (javax.security.auth Subject)) (quote (javax.security.auth.login Configuration)) (quote (com.sun.security.auth UserPrincipal)) (quote (java.security KeyStore Security)) (quote (java.security.cert CertificateFactory CertPathValidator)))
  (println (.implies (FilePermission. "/tmp/-" "read") (FilePermission. "/tmp/a/b" "read"))
           (.implies (SocketPermission. "*.example.com:80" "connect") (SocketPermission. "www.example.com:80" "connect")))
  (let [s (Subject.)] (.add (.getPrincipals s) (UserPrincipal. "u"))
    (println (Subject/callAs s (fn [] (vec (.getPrincipals (Subject/current)))))))
  (println (.getName (.getClass (Configuration/getConfiguration))))
  (let [c (.generateCertificate (CertificateFactory/getInstance "X.509") (FileInputStream. "/root/jdk26u/test/jdk/java/security/cert/CertPathValidator/trustAnchor/labs2isrg2.cer"))
        ks (doto (KeyStore/getInstance "PKCS12") (.load nil nil) (.setCertificateEntry "a" c))
        out (ByteArrayOutputStream.)]
    (println (str (.getSubjectX500Principal c)))
    (.store ks out (.toCharArray "pw"))
    (println (.size (doto (KeyStore/getInstance "PKCS12") (.load (ByteArrayInputStream. (.toByteArray out)) (.toCharArray "pw")))))
    (println (mapv #(.getType %) (.getServices (Security/getProvider "SUN"))) (CertPathValidator/getInstance "PKIX")))'
# 8. Java's levels interp and compiled on F4's and F2's areas (then the whole corpus with F1's and F5's on their
#    night, as JC6b): saved, not recorded; expect about 34 / 31 (F4) and 62 / 62 (F2) of the tests above to pass,
#    no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(java/security|javax/security/auth|com/sun/security/auth|sun/security/(x509|pkcs|pkcs12|provider|util|lib|rsa)|java/io/FilePermission|java/net/(SocketPermission|URL)|java/util/(PropertyPermission|jar)|java/lang/(Class|RuntimePermission|reflect))|hotspot/jtreg/(runtime/JVMDoPrivileged|compiler/c1/TestConcurrentPatching)' \
  --save .tmp/f4f2-night/check
```

Then, the day after (light), as F1's: the saved results judged (`bin/java-check --judge`), the tail classified
(`:security` split by cause: what F4 and F2 leave narrowed to `:security-tls`, `:security-sasl-gss` and the cut
causes named above), `--record` once the whole corpus is rerun, `bin/reap --record`, the docs with the numbers
(JRT-SOURCES.md's closure list, JRT-NOTES.md's "Security", J2G-SPEC §4.1 and §4.4's renames), the commit of
`jdk/` and `jdk/javac/`, the merge to `next`.

**Risks and fallbacks**: (1) `SignatureAccess.ae`'s added static initializer depends on the order of the variant
files (`Signature.ae` first, its cut keyed by position): should j2g report the cut missing or the access unset
(SignatureUtil's `initVerifyWithParam` failing), fold it into F1's `Signature.ae` (replace the cut by the
initializer): a one-file change once F1 is merged; (2) a class of `class-supers` whose DynSub j2g cannot make
(`X509Certificate`'s final members, `KeyStoreSpi`'s): remove it, its tests stay `:interp-protected`; (3)
CompileProperties on the tool (`Files.newBufferedWriter`, `MessageFormat` over jrt's): should it fail, the
step's error names it, and the two bundles can be taken from the build tree as CaseFolding's are, a decision
for the user; (4) the PKIX tests' speed interpreted (big certificate paths: `:speed`'s measure, `bin/java-speed`);
(5) `PKIXCertPathValidator`'s and the builder's `ThreadTracker` and `Cache` over jrt's threads: plain Java,
untried.

**What is left of F4 and F2**: the night's run and the day's classification and records; `Class.getProtectionDomain`
(above); JCS140-JCS142 (Proposed).

## As built: F3 (write-ahead)

Branch `java-jc9-tls` (from `java-jc9-pki`, JCS128-JCS154 decided), 2026-11-05, F3 prepared by day under the
light rules as F1, F5, F4, F2 and JC9b were: the closure's list, jrt's own Java, the variants, j2g's tables, the
data step, and light checks only. Nothing converted, translated or built. JCS131 (a): TLS is jdk26u's own
SunJSSE translated; JCS138 (b): System.Logger was to come with F3, whose `SSLLogger` is its first in-world
user (what was built instead, and why: below and JCS155). **F3 needs F1, F2 and F4** (SunJSSE's ciphers, key
agreements, KDFs, signatures and key factories are F1's; its trust and key managers, certificate messages and
`X509TrustManagerImpl` stand on F2's certificates, PKIX and key stores; `AccessController` and the permissions
are F4's) **and F5** (`SSLLogger`'s path for an empty `javax.net.debug`): its closure was computed against the
world plus F1's, F5's, F4's and F2's lists (by reference: `tls-sources` names none of their files), and
`ProviderConfigJsse.ae` repeats F1's `getProvider` with SunJSSE beside it, so the branch merges after
`java-jc9-jca`, `java-jc9-logging` and `java-jc9-pki`. F3 does not need JC9b (no F7, F8, F10 or F11 class:
the closure names none). The branch's hunks in the shared files are apart from the other branches' (`git
merge-tree` with `java-jc9-jca`, `java-jc9-logging`, `java-jc9b` and `java-jc9b-natives`: a conflict in this
file alone, as `java-jc9-pki` had, once F3's lines in `bin/jrt-convert`, `sources` and LICENSE.md were moved off
JC9b's; the resolution is the union, every branch adding sections of its own).
**Not mergeable before its night**, as JC9's branches: its variants name classes `jdk/forms` does not hold yet
(SSLContext, SNIHostName, SSLSocketImpl: "a variant for a class not in the inputs").

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `tls-sources`, one line in
`sources`):

| part | how |
|---|---|
| sun.security.ssl (137) | the whole package but `SSLLogger` (jrt's copy, below): `SunJSSE`, `SSLContextImpl` and its TLS, DTLS and Default contexts, the handshake (`ClientHello` ... `Finished`, `KeyUpdate`, `NewSessionTicket`, the extensions), the record layer (`SSLSocketInputRecord`, `SSLEngineOutputRecord`, DTLS's records), the key exchanges (RSA, DH, ECDH, XDH, the named groups, `KAKeyDerivation`) and cipher suites (`CipherSuite`, `SSLCipher`), the key and trust managers (`X509KeyManagerImpl`, `SunX509KeyManagerImpl`, `X509TrustManagerImpl`, `TrustStoreManager`), the sessions, `SSLSocketImpl`, `SSLServerSocketImpl`, `SSLEngineImpl`, `StatusResponseManager`; and QUIC's TLS engine (`QuicTLSEngineImpl`, `QuicKeyManager`, `QuicCipher`, `QuicEngineOutputRecord`, `QuicTransportParametersExtension`), which 13 files of the handshake name (JCS156) |
| jdk.internal.net.quic (7) | the QUIC engine's interfaces, versions and errors; not `QuicTLSContext`, HTTP/3's entry (java.net.http), whose `VarHandle` reads `SSLContext`'s private field through `privateLookupIn` |
| javax.net.ssl (40), javax.net (2) | the whole package but `HttpsURLConnection` (java.net's `HttpURLConnection` is not in the Go build: F12); `SocketFactory`, `ServerSocketFactory` |
| javax.security.cert (7) | the deprecated certificate API `SSLSession` and `HandshakeCompletedEvent` name (j2g renames, below) |
| java.net.IDN with ICU's StringPrep (8) | `IDN`, `jdk.internal.icu` `StringPrep`, `Punycode`, `StringPrepDataReader`, `Trie`, `CharTrie`, `UCharacterDirection`, `UCharacterEnums`: `SNIHostName`'s `toASCII` (JCS157); its profile `sun/net/idn/uidna.spp` copied into `jdk/data` by `bin/jrt-convert`'s data step, as the build copies the resource |
| the rest (4) | `sun.security.util`'s `ByteArrays`, `LegacyAlgorithmConstraints`; `jdk.internal.event.TLSHandshakeEvent` (never enabled: no JFR); `jdk.internal.access.JavaNetInetAddressAccess` |
| jrt's own (overlay, 3) | `sun.security.ssl.SSLLogger`, jdk26u's transcribed: implements no `System.Logger` (jrt's hand-written `System` has none), a nested `Level` of the same constants and severities, `javax.net.debug` non-empty: the JDK's private console logger exactly; empty: java.util.logging's logger `javax.net.ssl` (F5) reached by reflection with `PlatformLogger`'s level mapping, what the JDK's `System.getLogger` gives with java.logging present (java.base does not read java.logging); the caller from a Throwable's stack trace (no StackWalker, F20). `sun.security.ssl.JsseServices`: SunJSSE's 14 service classes constructed directly (after JrtServices and PkiServices). `jdk.internal.jrt.NetAccess`: InetAddress's access object, in place of SharedSecrets |
| the variants (5) | `SSLContext.ae` (the default context published under the class's monitor: j2g compiles `findVarHandle`, not `findStaticVarHandle`), `SNIHostName.ae` (the encoded name decoded by String's UTF-8 and checked by encoding it back, no `CharsetDecoder` before F8), `SSLSocketImpl.ae` (`useImplicitHost` through NetAccess), `InetAddressAccess.ae` (InetAddress's second file: a static initializer added, jdk26u's anonymous `JavaNetInetAddressAccess` given to NetAccess), `ProviderConfigJsse.ae` (ProviderConfig's second file: F1's `getProvider` with `SunJSSE` by name and class name); `Provider.ae`'s `newInstanceUtil` asks JsseServices after PkiServices |
| j2g | renames `javax/security/cert/*` (7) → `JavaxCert_*` (java.security.cert's namesakes, F2's), `sun/security/ssl/RSASignature` → `Ssl_RSASignature` (sun.security.rsa's, F1's), `sun/security/ssl/Record` → `Ssl_Record` (jrt's `java.lang.Record`); `class-supers` + `X509ExtendedTrustManager`, `X509ExtendedKeyManager`, `SSLSocketFactory`, `SSLServerSocketFactory`, `SSLSocket`, `SSLEngine`, `SSLContextSpi`, `SNIServerName`, `SNIMatcher`, `KeyManagerFactorySpi` (the classes the area's tests extend: 3, 3, 3, 2, 2, 2, 1, 1, 1, 1 of them) |

The sockets are jrt's: `SSLSocketImpl` is a `java.net.Socket` over jrt's `HostSocketImpl` (the world's `Socket`
and `ServerSocket` variants), its streams the plain socket's, the TLS records SunJSSE's own; jrt's
`TlsSocket` over Go's crypto/tls (arbace.tls's HTTPS) stays as it is, apart. The engine (`SSLEngineImpl`)
needs no socket. Left out, with their tests: `HttpsURLConnection` (6 tests at each level:
`HttpsURLConnection/*`, `sun/net/www/protocol/https/*`, `FixingJavadocs/JavaxURLNulls`,
`SSLSession/HttpsURLConnectionLocalCertificateChain`; F12, F12b), QUIC's `QuicTLSContext` (no test of the
areas), `javax.security.cert.X509Certificate.getInstance`'s provider class loaded by name (the default
`com.sun.security.cert.internal.x509.X509V1CertImpl` is not in the closure; the deprecated chain methods throw
`UnsupportedOperationException` in jdk26u anyway). What stays outside the world in members no variant
replaces becomes j2g's operation-level stubs: `BaseSSLSocketImpl.getChannel` (`SocketChannel`, F10; the
plain socket's answer is null), the sessions' and keys' serialization members (J2G-SPEC §12).

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 2 GiB; scratch in the worktree's `.tmp/f3/`):
- the closure (F1's `closure.py` as JC9b ran it, every module indexed, the world extended by JC9's lists and
  overlays, scope `sun.security.ssl`, `javax.net.ssl`, `javax.net`, `sun.security.util`, `javax.security.cert`,
  `jdk.internal.net.quic`, `java.net`, `jdk.internal.icu.**`, `sun.net.idn`): 205 files, 77,684 lines, all
  `share`, and the 3 overlays, 808 lines; outside the world and the list it names `VarHandle` and
  `MethodHandles` (`SSLContext`: the variant; `QuicTLSEngineImpl`: an instance field's `findVarHandle`, which
  j2g compiles, arbace.j2g.vh), `StackWalker` (SSLLogger: the overlay), `CharsetDecoder`, `CodingErrorAction`
  (SNIHostName: the variant), `SocketChannel` (a stub) and `JavaNetInetAddressAccess` (added);
- `jrt.jrt-sources/print-sources`: the branch's list is `java-jc9-pki`'s 1,216 lines plus exactly these 208
  (205 `share`, 3 `overlay`), 1,424, no line twice; JC9 merged with the branch (`git merge-tree` with
  `java-jc9-jca`, then `java-jc9-logging`, their `jrt_sources.ae` conflict resolved as the union): 1,790;
- the read: `bin/repr-java read-jdk --check` of the 205 files and the 3 overlays as a patch of java.base, as
  `bin/jrt-convert`'s convert step reads them: 208 converted, 0 failed, 0 javac errors, 0 unreadable (23 s); `--no-javac`: 208 native, 0 fallbacks, the forms identical (`diff -r`);
- the variants applied and parsed (`.tmp/f3/vet3.ae`, `vet.ae` given the variant files in j2g's order): SSLContext
  1/2/0 (replaced/cut/added), SNIHostName 1/0/0, SSLSocketImpl 1/0/0 on the converted forms; on the world's
  forms InetAddress 5/2/1 (InetAddress.ae, then InetAddressAccess.ae's added initializer), ProviderConfig 2/0/0
  (F1's ProviderConfig.ae, then ProviderConfigJsse.ae: the second replacement wins), Provider 1/0/0 with
  `Provider$Service` 1/0/0; all parsed;
- the whole closure by the JDK's javac against jrt's overlays (JC9b's `all9/run.sh` with F3's paths,
  `.tmp/f3/run.sh`): JC9 and F3 merged, every line of print-sources from jdk26u, the build's gensrc and the
  merged tree's overlays, `--patch-module java.base`: 1,790 files, **0 errors**, 4,231 classes (18 s, 0.9 GB);
- jrt's hand-written classes: every `System.`, `Thread.` member the new files call is in jrt's manifest
  (`System.getLogger` was the one missing: SSLLogger's overlay);
- Go names: every class of the new forms (with the nested ones) against the gate executable's Go types, jrt's
  hand-written ones and F1's, F2's, F4's and F5's classes: the collisions are the renames above.

**JCS135, the closure's growth**: +78,492 Java lines (77,684 jdk26u's, 808 jrt's: **+16.2%** on the closure's
483,461), the forms +2,119,080 bytes (**+19.0%** on `jdk/forms`' 11,161,667), and 20 KB of data; about
+12.8 MB on the executable by JC6b's measure. JC9 with F3: **+277,409 lines, +57.4%**, the forms about +7.1 MB
(+64%), the executable some +45 MB (22%) on the gate build's 206.9 MB, before JC9b's +22.4%. JCS135 (a)'s
threshold (a minute more of build) is measured at each night.

**Expected yield** (the triage's scan of `d86cb874a`, `causes.json`: the deferred tests whose causes F1, F5,
F4, F2 and F3 together cover, `tls` among them): **91 interpreted / 92 compiled** at most (the plan's 94 / 94
counted F3 after F1-F2 alone), of which 6 / 6 need `HttpsURLConnection` (above) and 2 / 2 a `SocketChannel`
(`SSLEngine/LargePacket`, `SSLSocketImpl/NewSocketMethods`): about **58 / 59 expected** (0.7 of 83 / 84). At
risk besides: the handshake's speed interpreted (the tests' loops interpreted, SunJSSE compiled), the tests
that run a server thread and a client on loopback under the check's CPU limits, the default trust store
(`TrustStoreManager` reads cacerts: JCS140 (a)'s data step is not on `java-jc9-pki` yet), the test library's
`SSLSocketTemplate` and `SSLEngineTemplate` (48 and 17 subclasses: one failure there fails many). `javax/net/ssl/DTLS`'s
tests (over `DatagramSocket`, JC9b's F11) have no row in the scan, so none is counted; F12 adds 3 / 2 and F12b
1 / 1 more. Beside them, `jdk/java/net/IDN`'s two tests (filed under F12's `net-http`: `java.net.IDN` was all
they lacked) can pass with F3's IDN (JCS157), +2 / 2.

**The night's commands**, in the worktree after `next` holds F1, F5, F4 and F2 (their nights' conversions
committed; merge `next` into `java-jc9-tls` first), heavy ones under `bin/heavy-lock`; about 3 hours with the
check. F3 adds no build tool: javac's world and tool are unchanged, so no `bin/javac-vendor convert` and no
`--build-java`.

```sh
cd /root/arbace/.tmp/worktrees/java-jc9-tls
git merge next      # JC9 (F1, F5, F4, F2), their nights' jdk/ and jdk/javac/ committed
# 1. jdk/ converted afresh: expect F2's night's count + 208 files (205 share, 3 of jrt's own), 0 failed,
#    0 javac errors, no javac: 0 fallbacks; jdk/forms + 221 files; jdk/data + sun/net/idn/uidna.spp
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (the new classes' outside references: SocketChannel, the object streams, StackWalker no
#    longer); 3. javac's world's edge less the new jdk/edge.edn
bin/heavy-lock bin/j2g-edge update
bin/heavy-lock bin/javac-vendor edge
# 4. the executable (JCS135: its size against F2's night's, `ls -l`; the j2g and Go times); j2g's report: the
#    variants' counts above, "a variant for a class not in the inputs" for none, QuicTLSEngineImpl's handle
#    compiled (no "VarHandle not constant")
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 5. a smoke of F3: a key store made by the JDK's keytool, a TLS 1.3 handshake on loopback in one process:
#    expect "42 TLSv1.3 TLS_AES_256_GCM_SHA384", then the providers with SunJSSE, then "xn--bcher-kva.example"
mkdir -p .tmp/f3-night && /root/jdk26u/build/linux-x86_64-server-release/images/jdk/bin/keytool -genkeypair \
  -alias k -keyalg EC -groupname secp256r1 -dname CN=localhost -validity 30 -storetype PKCS12 \
  -keystore .tmp/f3-night/ks.p12 -storepass passphrase
target/arbace-go/amd64/arbace -e '(import (quote (javax.net.ssl SSLContext KeyManagerFactory TrustManagerFactory SNIHostName)) (quote (java.security KeyStore Security)) (quote (java.io FileInputStream)))
  (let [pw (.toCharArray "passphrase")
        ks (doto (KeyStore/getInstance "PKCS12") (.load (FileInputStream. ".tmp/f3-night/ks.p12") pw))
        kmf (doto (KeyManagerFactory/getInstance "SunX509") (.init ks pw))
        tmf (doto (TrustManagerFactory/getInstance "PKIX") (.init ks))
        ctx (doto (SSLContext/getInstance "TLSv1.3") (.init (.getKeyManagers kmf) (.getTrustManagers tmf) nil))
        ss (.createServerSocket (.getServerSocketFactory ctx) 0)
        srv (future (with-open [s (.accept ss)] (.read (.getInputStream s))))
        c (.createSocket (.getSocketFactory ctx) "localhost" (.getLocalPort ss))]
    (doto (.getOutputStream c) (.write 42) (.flush))
    (println @srv (.getProtocol (.getSession c)) (.getCipherSuite (.getSession c)))
    (.close c) (.close ss)
    (println (mapv #(.getName %) (Security/getProviders)))
    (println (.getAsciiName (SNIHostName. "bücher.example")))))'
# 6. Java's levels interp and compiled on F3's areas (then the whole corpus with JC9's, as JC6b): saved, not
#    recorded; expect about 58 / 59 of the 91 / 92 tests to pass, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(javax/net|sun/security/ssl|sun/net/www/protocol/https|com/sun/net/ssl|java/net/IDN)' \
  --save .tmp/f3-night/check
```

Then, the day after (light), as F1's and F2's: the saved results judged (`bin/java-check --judge`), the tail
classified (`:tls` split by cause: `:https` for HttpsURLConnection, the channel tests to F10's class),
`--record` once the whole corpus is rerun, `bin/reap --record`, the docs with the numbers (JRT-SOURCES.md's
closure list, JRT-NOTES.md's "Security" and "Sockets", J2G-SPEC §4.1 and §4.4's renames), the commit of `jdk/`,
the merge to `next`.

**Risks and fallbacks**: (1) `SSLLogger`'s reflection on java.util.logging (`Class.forName`, `getMethod`,
`Method.invoke` on a compiled class from compiled code): should jrt's member tables not hold `Logger.log(Level,
String)`, the empty-`javax.net.debug` path logs nothing (the static initializer catches it) and only the
tests of that path fail; JCS155 (a) is the proper fix; (2) `QuicTLSEngineImpl`'s `findVarHandle` on a final
local bound to the class literal: should arbace.j2g.vh not take it, a two-line variant (the handle's
`compareAndSet` under the engine's lock); (3) `ProviderConfigJsse.ae` replaces what `ProviderConfig.ae`
replaced: should j2g's order of variant files not be the file names', fold SunJSSE into F1's file (a one-line
change once F1 is merged); (4) a `class-supers` class whose DynSub j2g cannot make (`SSLSocket` over jrt's
`Socket`, `SSLEngine`'s final members): remove it, its tests stay `:interp-protected`; (5) loopback servers
under the check's CPU-time limits (`bin/lib/limit.bash`): the handshake's key generation interpreted
around compiled code, `:speed`'s measure if they time out.

**What is left of F3**: the night's run and the day's classification and records; JCS140 (a)'s cacerts in
the data step (F2's, decided, not built), which the default `SSLContext`'s trust managers read; JCS155-JCS157
(Proposed).

## As built: F6 (javac's API on javac's tool, JCS95 (c))

Branch `java-jc9c` (from `java-jc9-tls`), 2026-11-06, by day under the light rules. F6 is the check's change JCS95 (c)
proposed and the user deferred on 2026-10-26: the tests that name javac's API run on javac's tool executable,
which holds javac's world, instead of the shipped program, which has none. It needs no JC9 branch.

**The check** (`bin/java-check`, its header): a test whose source files name `com.sun.tools.javac`, `com.sun.source`,
`javax.tools`, `javax.lang.model`, `javax.annotation.processing` or `java.util.spi.ToolProvider` on a line that is no
comment line is interpreted on javac's tool (`REPR_JAVA_EXE`, bin/repr-java's default), with
`-Djdk.internal.jrt.javac.platform=` the platform `bin/repr-java platform-dir` gives (new verb: the platform
javac on Arbace's command line attributes against, made as `bin/repr-java javac` makes it), which
`jdk.internal.jrt.javac.SystemCompiler`, javax.tools' system compiler on the tool, reads (as bin/javac-vendor's
generate step sets it). Without a tool the tests run on the executable as before (a warning). The rule selects
exactly the 56 tests the triage's scan files under javac's API (of the 3,322 the interp level runs; the one
more a plain grep would take, t06039, names javac in a comment). The gate's `java-interp` hosts `:javac-prog`
(it had `REPR_JAVA_EXE` already) and declares `bin/repr-java`, `bin/javac-vendor`, `bin/lib/host.bash`.

**javac's tool world** (built at the night, `bin/javac-vendor tool`):
- `com.sun.tools.javac.Main`'s `compile(String[])` and `compile(String[], PrintWriter)`: javac on Arbace's
  command line (variant `overlay/jdk/javac/variants/JavacMain.ae`, over the added
  `jdk.internal.jrt.javac.Main.runOnPlatform`: the platform property's directories as `--platform`): jdk26u's
  launcher runs over JavacFileManager and java.nio.file, which the tool has not (`BasicFileAttributes is not in
  the closed world`);
- the API's standard file manager (`StandardFileManager`) handles the file manager's options javac hands it
  (`handleOption`, `isSupportedOption`): `-d`, the class and source paths, `-encoding UTF-8`, `--system none`,
  as the command line serves them;
- a class file without `-d` goes where JavacFileManager puts it: beside its source file, else in the current
  directory by its simple name (`FileManager.besideSibling`); before, an `IOException` ("no output location");
- j2g's `class-supers` + `javax/tools/SimpleJavaFileObject`, `com/sun/source/util/TreeScanner`, `TreePathScanner`
  (what the tests extend; absent from the shipped world, which they leave alone);
- `jdk/javac/forms` of the three changed overlay classes, converted as `bin/javac-vendor convert` reads
  jdk.compiler (`read-jdk --check --no-javac`, `REPR_JAVA_PATCH_ALL=1`): the unchanged `SystemCompiler` came
  out byte for byte its committed form; the night's `bin/javac-vendor verify` compares the whole tree.

**Checked by day** (main's stage 0, the gate's tool `.tmp/gate-exe/javac-fee544298f4305f99543`, stale for the
checkout's key: `REPR_JAVA_STALE_OK=1`):
- the 56 at read and interp: before F6 every one load-failed on the shipped executable; with the check's
  change, on today's tool, **6 pass** (t05890, t06381-t06384, t06385), every other one in a class;
- the javac-world changes, before their build: renamed copies of the four overlay classes (package `f6x`,
  `FileManager.Obj` implementing `JavaFileObject` itself, since an interpreted class cannot extend
  `SimpleJavaFileObject` until the build), read and interpreted on today's tool with the tests' javac entries
  pointed at them (`.tmp/jc9c/f6x/proof.sh`): t06044, t06045, t06369, t06378, t06379, t06380 **pass** against the
  JDK's recorded runs (status, stdout); t06346 stops at `--release 11` (no ct.sym, `:diag-release`);
- the classes: `:javac-tool` (new, kind `:javac`, the eval cell maps it to deferred) for what javac on Arbace's own
  Java does not serve (javac's ToolProvider, JCS159; until the night the API's `-d`, the class output and
  `Main.compile`), the jar tool's two (`ToolProvider.findFirst("jar")`) to `:jvm-tools` (JCS103's JDK tools),
  ToolProviderTest's META-INF/services provider and the file manager's `getServiceLoader` to `:classloading`,
  `--release` at the interp level to `:diag-release`.

**Expected, interp** (the 56): after the night about **16 pass** (6 measured, 6 proven, the 4 that extend
`SimpleJavaFileObject` and `TreeScanner` not provable before the build); 4 leave deferred for by-design classes;
the rest: the class-file API 22 (by design, JCS129), the launcher's tests 11 (`tools/launcher`, they need `java`),
`--release` 1, javac's ToolProvider 2. The compiled level is unchanged (JCS158). **Deferred at eval: -18**
(16 passing, 2 by design), AOT unchanged.

**The night's commands** (after the merge; javac's tool is the only build):

```sh
cd /root/arbace/.tmp/worktrees/java-jc9c
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 bin/javac-vendor verify
bin/heavy-lock bin/javac-vendor edge         # javac's world's edge (the new members name nothing new: expect no change)
bin/heavy-lock bin/javac-vendor tool .tmp/f6-night/tool   # the tool at the checkout's arbace_javac_key
# the 56: expect about 16 pass at interp, every other in a class; record their lines
REPR_JAVA_EXE=$PWD/.tmp/f6-night/tool/arbace-javac JAVA_CHECK_EXE=<the night's gate executable> \
  bin/java-check --levels interp,compiled --only "$(paste -sd'|' .tmp/jc9c/javacapi.ids)" --record
```

## As built: F12 (write-ahead)

Branch `java-jc9c`, 2026-11-06, F12 prepared by day under the light rules as JC9's and JC9b's areas were: the
closure's list, jrt's own Java, the variants, j2g's tables, the data step, light checks. Nothing converted,
translated or built. **F12 needs F5** (`PlatformLogger`'s bridge is java.util.logging, F5's `LoggingProviderImpl`
access), **F3** (https: is SunJSSE's), **F4** (the permissions `URLPermission`, `NetPermission` the classes check) and
**F8** (`URLEncoder.encode(String, Charset)`'s `CharsetEncoder`, JC9b's): its closure was computed against the world
plus every JC9 and JC9b list (a scratch merge of `java-jc9-tls`, `java-jc9-jca`, `java-jc9-logging` and
`java-jc9b-natives` with the union for their shared lines, `.tmp/jc9c/mclone`, 1,993 lines of print-sources), by
reference: `http-sources` names none of their files. Not mergeable before its night, as JC9's branches (its
variants name classes `jdk/forms` does not hold yet: "a variant for a class not in the inputs").

**What comes in** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `http-sources`, one line in `sources`): 82 files,
25,542 lines, all `share`:

| part | how |
|---|---|
| java.net (23) | `Proxy`, `ProxySelector`, `SocksSocketImpl`, `SocksConsts`, `HttpConnectSocketImpl`, `DelegatingSocketImpl`; `URLEncoder` (with `jdk.internal.util.ImmutableBitSetPredicate`); `HttpCookie`, `CookieHandler`, `CookieManager`, `CookiePolicy`, `CookieStore`, `InMemoryCookieStore`; `Authenticator`, `PasswordAuthentication`; `ResponseCache`, `CacheRequest`, `CacheResponse`, `SecureCacheResponse`; `HttpURLConnection`, `HttpRetryException`, `ProtocolException`, `ContentHandlerFactory` |
| the http: and https: connections (33) | `sun.net.www.protocol.http` (13: `HttpURLConnection`, the authentication schemes Basic and Digest, the NTLM and Negotiate proxies, which find no implementation by name and answer none, as on a JDK without them), `sun.net.www.http` (12: `HttpClient`, the keep-alive cache and streams, the chunked streams, `PosterOutputStream`, the capture), `sun.net.www.protocol.https` (4: `HttpsURLConnectionImpl` over SunJSSE), `javax.net.ssl.HttpsURLConnection` (F3 left it out), `sun.net.www` `HeaderParser`, `MeteredStream`, `sun.net.util.ProxyUtil` |
| the ftp: and mailto: handlers (17) | `sun.net.www.protocol.ftp` and `mailto` (2 each), `sun.net.ftp` (7) with `impl` (2), `sun.net.smtp` (2), `sun.net` `NetworkClient`, `TransferProtocolClient`, the telnet streams (3) |
| the proxies' configuration (6) | `sun.net.spi.DefaultProxySelector`, `sun.net.NetProperties`, `ApplicationProxy`, `SocksProxy`; `jdk.internal.access.JavaNetHttpCookieAccess` |
| jrt's own (overlay, 2 new, 1 extended) | `sun.util.logging.PlatformLogger`: jdk26u's transcribed without System.Logger (JCS155 (b); its lazy loggers have nothing to bridge to), its bridge the one the JDK ends with when java.logging is present, jdk26u's JULWrapper's PlatformLogger half (`JulBridge`), the java.util.logging logger demanded through F5's `LoggingProviderImpl` access when first used; `jdk.internal.misc.InnocuousThread`, the factories of jdk26u's on jrt's `Thread` (no thread groups, context class loaders or Unsafe on Thread's fields in jrt); `jdk.internal.jrt.NetAccess` holds HttpCookie's access (as InetAddress's since F3) |
| the variants (9) | `ProxySelector.ae` (the default constructed directly, no `Class.forName`), `DefaultProxySelector.ae` (no libnet: `init` answers false, the JDK's answer on a host without the desktop's proxy settings), `NetProperties.ae` (the resource `conf/net.properties`), `HttpCookieAccess.ae` and `HttpURLConnectionImpl.ae` (the cookie access through NetAccess; `jrtTunnelSocket`), `HttpClient.ae` (`System.getProperty`, jrt has no `getProperties` for translated code; `jrtServerSocket`), `HttpConnectSocketImpl.ae` (the tunnel without reflection on protected members); `URL.ae`'s factory + `ftp`, `mailto`; `Socket.ae` without its `createImpl` (jdk26u's: the platform impl in a `SocksSocketImpl`, which asks the proxy selector, JCS160) and `ServerSocket.ae` removed (jdk26u's `implAccept` unwraps the delegating impls) |
| the data | `conf/net.properties` into `jdk/data` by `bin/jrt-convert`'s data step, as `make/modules/java.base/Copy.gmk` makes it (share's then unix's: equal to the image's) |
| j2g | renames `java/net/Authenticator` → `Net_Authenticator` (SunJSSE's, F3's), `sun/net/www/protocol/ftp/Handler` → `Ftp_Handler`, `mailto/Handler` → `Mailto_Handler` (java.util.logging's `Handler`, F5's), `sun/net/ftp/impl/FtpClient` → `FtpImpl_FtpClient`, `sun/net/www/protocol/http/HttpURLConnection` → `Http_HttpURLConnection` (J2G-SPEC §4.4); `class-supers` + `Authenticator`, `ProxySelector`, `Socket`, `ServerSocket`, `URLConnection`, `SecureCacheResponse`, `BufferedReader` (what the area's tests extend: 3, 2, 2, 2, 1, 1, 1) |

What the classes reach outside the world: serialization (`KeepAliveCache`'s `writeObject`, J2G-SPEC §12's stubs), the
NTLM, Negotiate and SPNEGO implementations by name (none: they answer as a JDK without them), `ServiceLoader` (no
FTP provider: the default, as jdk26u's own comment has it).

**Light checks done** (main's stage 0, today's tool, `REPR_JAVA_STALE_OK=1`, each under 1 GB; scratch
`.tmp/jc9c/`): the closure (`closure.py`, F3's with the merged world, scope `java.net`, `sun.net.**`'s packages,
`jdk.internal.util`; seeded with the world's own java.net and sun.net files, whose stubbed references become real,
which added `HttpConnectSocketImpl` and `ContentHandlerFactory`): nothing outside the world and the list but the
overlays' classes and the variants' members; print-sources: the branch's 1,424 lines + exactly these 84 (82 share,
2 overlay), no line twice; `read-jdk --check` of the 82, the overlays and F5's java.util.logging as a patch of
java.base: 104 converted, 0 failed, 0 javac errors; `--no-javac`: 104 native, 0 fallbacks, the forms identical;
the variants applied and parsed on the converted forms (ProxySelector 1/0/0, DefaultProxySelector 3/0/0,
NetProperties 1/0/0, HttpCookie 1/0/0, sun's HttpURLConnection 1/0/1, HttpClient 1/0/1, HttpConnectSocketImpl
1/8/0) and on the world's (URL with its factory, Socket 3/4/4); the whole closure by the JDK's javac against jrt's
overlays (F3's `run.sh` on JC9, JC9b, F3 and F12 merged): 2,077 files, **0 errors**, 4,706 classes (18 s, 0.9 GB);
jrt's hand-written members the new files call (System, Thread, Class...): `System.getProperties` was the one
missing (`HttpClient.ae`). `git merge-tree` with `java-jc9-jca`: conflicts in this file, `test/java/divergences.edn`
(`:jvm-tools`, whose kind JCS130's branch makes `:harness-design`: take it, with F6's two ids) and
`test/reap/cells.edn` (the eval kind-map: the union, `:harness-design :design` and `:javac :deferred`); with
`java-jc9-logging` and `java-jc9b-natives`: this file alone.

**JCS135's measure**: +25,542 Java lines of jdk26u's and about 500 of jrt's (+5.3% on the closure's 483,461; JC9,
JC9b, F3 and F12 together about +85%); about +4 MB on the executable by JC6b's measure.

**Expected yield** (the triage's scan, the deferred tests whose causes JC9, JC9b and F12 together cover and JC9 and
JC9b alone do not): **40 interpreted / 37 compiled** at most (the HttpsURLConnection tests F3 left among them),
about **28 / 26** (0.7). F12b (java.net.http's client, com.sun.net.httpserver) gains with it. At risk: every client
socket now goes through `SocksSocketImpl` and the proxy selector (JCS160: should the sockets' passing tests regress,
the fallback is `Socket.ae`'s former `createImpl`, BadProxySelector and `socksProxyHost` left out); the HTTP tests'
loopback servers under the CPU limits; the keep-alive threads (`InnocuousThread`).

**The night's commands**, after `next` holds JC9 and JC9b (merge `next` into `java-jc9c` first), heavy ones under
`bin/heavy-lock`, about 3 hours with the check:

```sh
cd /root/arbace/.tmp/worktrees/java-jc9c
git merge next
# 1. jdk/ converted afresh: expect the night before's count + 84 files (82 share, 2 overlay), 0 failed, 0 javac
#    errors, no javac: 0 fallbacks; jdk/data + conf/net.properties (:same as the image's)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh; 3. javac's world's edge less it
bin/heavy-lock bin/j2g-edge update
bin/heavy-lock bin/javac-vendor edge
# 4. the executable (its size, the j2g and Go times); j2g's report: the variants above, none "for a class not in
#    the inputs"
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 5. a smoke: a loopback HTTP server thread and HttpURLConnection, a cookie, a proxy selector's answer
target/arbace-go/amd64/arbace -e '(import (quote (java.net ServerSocket URL HttpCookie ProxySelector URI URLEncoder)))
  (let [ss (ServerSocket. 0)
        srv (future (with-open [s (.accept ss)] (let [in (.getInputStream s)] (while (pos? (.available in)) (.read in))
              (.write (.getOutputStream s) (.getBytes "HTTP/1.1 200 OK\r\nSet-Cookie: a=b\r\nContent-Length: 2\r\n\r\nok")))))
        c (.openConnection (URL. (str "http://127.0.0.1:" (.getLocalPort ss) "/")))]
    (println (.getResponseCode c) (slurp (.getInputStream c)) (HttpCookie/parse "Set-Cookie: a=b"))
    (println (.select (ProxySelector/getDefault) (URI. "http://example.com/")) (URLEncoder/encode "a b&c" "UTF-8"))))'
# expect "200 ok [a=b]" then "[DIRECT] a+b%26c"
# 6. Java's levels interp and compiled on F12's areas and the sockets' (then the whole corpus with JC9's): saved,
#    not recorded; expect about 28 / 26 of the 40 / 37 to pass, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(java/net/(Authenticator|CookieHandler|HttpCookie|HttpURLConnection|IDN|ProxySelector|Socks|Socket|ServerSocket|URL|URLConnection|URLDecoder|URLEncoder)|sun/net/www|javax/net/ssl/HttpsURLConnection|java/io/OutputStreamWriter/Encode)' \
  --save .tmp/f12-night/check
```

Then, the day after: the results judged, the tail classified, the records with the whole corpus, `bin/reap --record`,
JRT-SOURCES.md's closure list, JRT-NOTES.md's "Sockets" (the proxies, the http: connection), the commit of `jdk/`.

## As built: JCS159 (write-ahead): javac's ToolProvider on javac's tool

Branch `java-jc9c`, 2026-11-06, after the user's JCS159 (a). Two variants of javac's tool world
(`overlay/jdk/javac/variants/`), built with F6's at the tool's night:
- `JavacToolProvider.ae`: jdk26u's `com.sun.tools.javac.main.JavacToolProvider` with `run` keeping javac's own
  launcher (`com.sun.tools.javac.main.Main`) when an argument is one of javac's information options (Option's INFO
  group that prints and compiles nothing: `--help`/`-help`/`-?`, `--help-extra`/`-X`, `--help-lint`,
  `--version`/`-version`, `--full-version`/`-fullversion`): javac's own text, which needs no file manager (tried on
  today's tool: `-help`'s 4,212 characters, exit 0); any other command line is javac on Arbace's
  (`Main.runOnPlatform`, as F6's `JavacMain.ae`), diagnostics to `err`. `name` and `description` are jdk26u's
  (`description` read through JavacMessages on today's tool: "read Java class and interface definitions and compile
  them into bytecode and class files").
- `ServiceLoader.ae`: over jrt's ServiceLoader of the tool world (JN2, no provider), `java.util.spi.ToolProvider` has
  javac's provider, as on a JDK image (`iterator`, `findFirst`; `stream` stays empty: nothing in the world or the
  tests streams it); jdk26u's `java.util.spi.ToolProvider.findFirst`, unchanged, iterates the loader. Every other
  service still finds none, so javac itself runs as before.

**Checked by day**: both variants applied and parsed on `jdk/javac/forms` (JavacToolProvider 1/0/0, ServiceLoader
2/0/1); Help.java (t05926) with its `ToolProvider.findFirst("javac")` replaced by jdk26u's `JavacToolProvider`
(the variant's `-help` and `-X` path is that one) interpreted on today's tool: **passes** against the JDK's run
(status 0, stdout empty, every option checked). Expected after the night: t05926 and t05786 (ToolProviderDescriptionTest)
pass: F6's 56 at about **18**, eval's deferred -20. No command beyond F6's: the same tool build (`bin/javac-vendor
tool`), then F6's `bin/java-check --levels interp,compiled --only ... --record` covers both tests.

## As built: F19 (write-ahead): javax.script's API, JNDI's names

Branch `java-jc9c`, 2026-11-06, after F12, by the triage's next rate (F13, 10.3 tests a day, waits for JCS161; F19,
8.4, comes next). Prepared as F12 was; nothing converted, translated or built. F19 needs nothing of JC9 or JC9b
but F4's permissions (javax.naming's and javax.script's `checkPermission` paths are gone with the Security Manager);
its closure was computed against the world with JC9, JC9b and F12.

**What comes in** (`test/jrt/jrt_sources.ae`'s `naming-script-sources`, 63 files, 17,814 lines, other modules'
files translated as java.base's, F5's way): javax.script (12: the API; `ScriptEngineManager`, `AbstractScriptEngine`,
`SimpleBindings`, `SimpleScriptContext`...), javax.naming (21: `Name`, `CompositeName`, `CompoundName`'s base
`NameImpl`, `Reference`, `RefAddr` and its two kinds, `InitialContext`, `Context`, the exceptions...),
javax.naming.directory (9: `Attribute(s)`, `BasicAttribute(s)`, `DirContext`...), javax.naming.ldap (7: `LdapName`,
`Rdn`, `Rfc2253Parser`, `LdapContext`, controls), javax.naming.spi (9: `NamingManager` and the factories'
interfaces), com.sun.naming.internal (6: `ResourceManager`, `VersionHelper`, `FactoryEnumeration`,
`NamingManagerHelper`, `ObjectFactoriesFilter`, `NamedWeakReference`). No provider: JNDI's LDAP, DNS and RMI
contexts stay out (JCS133's Kerberos neighbourhood; `LdapLoginModule`'s test with them).

| what | how |
|---|---|
| variants (3) | `ScriptEngineManager.ae`: no ServiceLoader, so no engine (JDK 26's image holds none: the scripting tests pass vacuously on the JDK too), the no-argument constructor on the system class loader (jrt's threads have no context class loader); `VersionHelper.ae`: the system class loader, no class-path resource files (`jndi.properties`: jrt's ClassLoader has no `getResources`), the JNDI properties one by one (no `System.getProperties`); `NamingManager.ae`: the initial context factories cached by class name (jdk26u's per-loader `ClassLoaderValue` is outside the world; one registry of classes, J2G-SPEC §10.3), the factory the named class's, as jdk26u does when no ServiceLoader provider has the name |
| outside the world | serialization (`readObject`, `writeObject`: J2G-SPEC §12's stubs), `ObjectFactoriesFilter`'s `ObjectInputFilter` (serialization's filters, by design): `NamingManager.getObjectInstance` reaches it, no test of the area does |
| j2g | renames `javax/naming/directory/Attributes` → `Naming_Attributes` (java.util.jar's), `javax/naming/Reference` → `Naming_Reference` (jrt's java.lang.ref); `class-supers` + `javax/naming/InitialContext` (EnvClone extends it) |

**Light checks**: print-sources + 63 lines (51 `share:java.naming`, 12 `share:java.scripting`), no line twice;
`read-jdk --check`: 63 converted, 0 failed, 0 javac errors; `--no-javac`: 63 native, 0 fallbacks, the forms
identical; the variants applied and parsed (ScriptEngineManager 2/1/0, VersionHelper 3/0/0, NamingManager
2/1/1); the whole closure with F12's and F19's by the JDK's javac against jrt's overlays: 2,140 files, **0
errors**. **The algorithms run on Arbace by day**: jdk26u's javax.script and javax.naming classes, renamed into
one package each (`aajs`, `aaxjn`: run.ae orders packages by imports, and JNDI's import each other in a cycle), the
variants' changes made in their Java, serialization's members left out, read and interpreted on the gate's
executable with the tests renamed to them (`.tmp/jc9c/f19p/proof.sh`, `jsproof.sh`, `--load do`): **18 of the 21
pass** against the JDK's recorded runs (the 12 javax.script tests; t07958, t07960, t07962 with identity hash codes
masked, t07963, t07967, t07968); t07961 iterates `Locale.getAvailableLocales()`, which jrt's locale selection
shortens (the locales' class, not F19's); t07964's interpreted `NamingException.toString` reaches
`super.toString()` of Exception, which the interpreter cannot call on a compiled superclass (the copy's limit:
compiled, NamingException is jdk26u's); t07869 needs the LDAP provider.

**JCS135's measure**: +17,814 lines (+3.7%). **Expected**: 21 / 21 at most, about **19 / 19** (18 proven, t07964
compiled). **Night**: F12's (the same conversion, edge, build); then `bin/java-check --levels interp,compiled --only
'jdk/javax/(script|naming)|jdk/com/sun/security/auth/module/LdapLoginModule' --save .tmp/f19-night/check`.

## F13 (java.lang.management's MXBeans): not built, JCS161

The next area by rate, 31 / 31 tests (ManagementFactory's ThreadMXBean 17, RuntimeMXBean 6, OperatingSystemMXBean 4,
GarbageCollector and MemoryPool 3, the rest 1). Read on F19's day: java.lang.management is not separable from JMX as
jdk26u writes it. `ManagementFactory` imports 15 javax.management classes (MBeanServer, ObjectName, StandardMBean,
JMX's proxies), every `PlatformManagedObject` answers `getObjectName()` (a javax.management.ObjectName), the platform
beans are found through `sun.management.spi.PlatformMBeanProvider`s by ServiceLoader, and the beans' data come from
49 natives of `sun.management.VMManagementImpl` and `ThreadImpl` (HotSpot's jmm interface: thread contention
counts, monitors and synchronizers owned, deadlocks, CPU times, GC and pool statistics). JCS95 (a) and JCS128 (a)
put JMX by design; F13 needs a line between them (JCS161) and jrt's own runtime facts (its monitors and locks
record no owner chains, its threads no CPU time): days of Go beside the Java, which a light day cannot prove.

## As built: F13 (write-ahead)

Branch `java-jc9c`, 2026-11-06, after JCS161 (a). java.lang.management without JMX, jrt's natives in Go.

**The line JMX draws, found on the day.** A variant is j2g's alone: `bin/jrt-convert`'s convert step compiles every
file of the closure with javac (a patch of java.base), so a jdk26u file naming a javax.management class does not
convert unless that class is in the closure (java.management's packages are not java.base's: "package
javax.management is not visible"). jdk26u's java.lang.management names JMX in its public API
(`PlatformManagedObject.getObjectName(): ObjectName`; `ThreadInfo`, `LockInfo`, `MonitorInfo`, `MemoryUsage`,
`MemoryNotificationInfo`'s `from(CompositeData)`), and its implementations through `sun.management.Util`'s
`newObjectName`, the `*CompositeData` helpers, `NotificationEmitterSupport` and `ManagementFactoryHelper`'s MBean
registration. The smallest JMX that closes over `ObjectName` and `CompositeData` is 54 files, 15,395 lines (the
MBean server's interfaces and the open types: `.tmp/jc9c/outjmx.json`), which JCS161 (a) declines. So F13 is:
- **jrt's own type skeletons** (overlay/jdk/java.base) for the two JMX types the API's signatures name:
  `javax.management.ObjectName` (its constructors and `getInstance` throw `UnsupportedOperationException`, "JMX is not
  in the Go build", JCS95 (a), JCS161 (a)) and `javax.management.openmbean.CompositeData` (the interface without
  `getCompositeType`, whose open types are JMX's);
- **overlays transcribing jdk26u's files** where they reach more of JMX: `ManagementFactory` (the beans by
  interface directly, no MBean server and no PlatformMBeanProvider's ServiceLoader: `getPlatformMBeanServer` and
  `newPlatformMXBeanProxy` absent), `ManagementFactoryHelper` (the bean getters alone), `ThreadInfo`, `LockInfo`,
  `MonitorInfo`, `MemoryUsage` (their `from(CompositeData)` throwing for a non-null argument), `sun.management.Util`
  (`newObjectName` throwing), `VMManagementImpl` (its natives over `jdk.internal.jrt.HostManagement`);
- **jdk26u's files as they are** where they name nothing else of JMX: the MXBean interfaces, `ThreadImpl`,
  `RuntimeImpl` (with variants for `ThreadImpl`'s natives), `ManagementPermission`, `MemoryType`, jdk.management's
  `com.sun.management.ThreadMXBean` and `HotSpotThreadImpl`.

**jrt's natives** (`go/arbace/jrt/management.ae`, `jdk.internal.jrt.HostManagement`), in slices:
1. *(built: `b47333dd5`, `bin/jrt test` passing)* the registry of live threads (started to ended, and main), their counts as
   HotSpot's ThreadService keeps them (live, daemon, peak and its reset, ever started), the runtime's start and
   uptime, the process id: ThreadMXBean's ids and counts, RuntimeMXBean's pid, name, start, uptime;
2. *(built: `0ad3408fd`, `bin/jrt test` passing, TestThreadStatesAndLocks)* a thread's state as the JVM reports it (BLOCKED while it waits to enter a monitor, WAITING and TIMED_WAITING in
   `wait`, `join`, `sleep`, `park`: jrt reports RUNNABLE today, JC8) with the object it blocks on, recorded at the
   monitor's contended path and the waits only (the thin lock's path untouched); then ThreadInfo's lock, lock owner
   and blocked and waited counts and times (contention monitoring);
3. *(built: `a52ff720b`, TestDeadlocks)* the cycles of waits for `findMonitorDeadlockedThreads` and, with jrt's
   ReentrantLock's and a translated AbstractOwnableSynchronizer's owner (LockSupport's blocker, through its
   `getExclusiveOwnerThread`), `findDeadlockedThreads`; 3b *(built: `8d859bf1c`, TestLockedMonitorsAndSynchronizers)*
   the monitors and synchronizers a thread holds: once a ThreadMXBean is made (`HostManagement.trackLocks0`), every
   monitor entry and exit and every jrt ReentrantLock's acquisition and release is recorded per thread (one atomic
   load on those paths before); ThreadInfo's locked monitors (latest first, each once, not the one waited on) and
   synchronizers (jrt's ReentrantLocks; the JDK lists their AQS sync objects, which jrt's lock has not); object
   monitor and synchronizer usage supported, so `findDeadlockedThreads` is reached; a monitor locked before the
   bean existed stays unlisted;
4. *(built: `d7ef6b827`, TestOtherThreadsStacks)* other threads' stacks: once tracked, a thread records its Java
   frames when it begins to wait, and the frame count at each monitor entry; ThreadInfo's stack is the current
   thread's own, a waiting thread's as it began to wait (Thread.sleep, Object.wait, the lock's wait on top, as
   jrt's frames name them), none for a running thread (a goroutine's stack cannot be read from another: such a
   ThreadInfo has an empty stack, `getThreadInfo(id, 0)`'s shape); the locked monitors' frames from the counts.
   While a ThreadMXBean exists each wait and monitor entry costs a stack walk; nothing before;
5. *(built: `9642751d4`, TestOperatingSystemFacts)* the operating system's bean: jdk.management's unix
   `OperatingSystemImpl` (overlay: its natives over jrt's `management_linux.ae`, which reads what
   libmanagement_ext reads on Linux: sysinfo, /proc/self/stat, the process's CPU time, /proc/self/fd and
   RLIMIT_NOFILE, sysfs's CPUs, /proc/stat's ticks for the loads, /proc/loadavg; no cgroup metrics,
   `jdk.internal.platform` being outside: the host's facts, as the JDK's without container limits; the box
   answers -1), made for ManagementFactory's table by `JrtPlatformBeans`; `BaseOperatingSystemImpl`'s load average.

Left for later slices: the memory, class-loading, compilation, collector and pool beans (Go's `runtime/metrics`
for the heap and the collector), the other threads' CPU time (per goroutine: none in Go), the allocated bytes
(per process only), `-XX` VM arguments (none), logging's and buffer pools' beans.

**The Java side** (`22919f139`): `management-sources`, 26 files of java.management and jdk.management (the MXBean
interfaces, `ThreadImpl`, `RuntimeImpl`, `VMManagement`, `Util`, the counters' interface, JMX's three exceptions
`MalformedObjectNameException` stands on, `com.sun.management.ThreadMXBean`, `HotSpotThreadImpl`); 10 overlays (the
two skeletons; `ManagementFactory`, its beans by interface from a table, the threads' (`HotSpotThreadImpl`, also the
`com.sun.management.ThreadMXBean`) and the runtime's, every other platform interface `UnsupportedOperationException`
naming the later slices; `ManagementFactoryHelper` with jmm's state mapping, jdk26u's `VM.toThreadState`'s;
`VMManagementImpl` over HostManagement; `ThreadInfo` (and its factory over the private constructors, handed to
HostManagement as the JVM reaches them by JNI), `LockInfo`, `MonitorInfo`, `MemoryUsage`; `HostManagement`); the
variant `ThreadImpl.ae` (17 natives); j2g's rename `Sun_ThreadMXBean`. Light checks: print-sources + 36;
read-jdk 36/0/0, `--no-javac` identical; the variant applied and parsed; the whole closure with F12, F19 and F13
by the JDK's javac: 2,176 files, 0 errors; `bin/jrt test` (amd64: all of arbace/jrt) and `bin/jrt build --arch
all` (jrt builds and vets clean on amd64 and arm64; vet's findings are scm's and goi's, as before) with slices 1-3.

**Expected**: of the 31, the counts and ids (AllThreadIds, ThreadCounts, ThreadDaemonTest, ThreadLists,
InvalidThreadID), RuntimeMXBean's (ProcessIdTest, UpTime, GetSystemProperties) with slices 1-2, about 9; the
deadlock and locks tests (FindDeadlocks, FindMonitorDeadlock, LockedMonitors, LockedSynchronizers,
SharedSynchronizer, SynchronizationStatistics, ThreadBlockedCount, MaxDepthForThreadInfoTest) with slices 2-4,
about 8 more; OperatingSystemMXBean's 4 with slice 5; GetObjectName, ThreadStartTest (javax.management) and the
VM-option tests (InputArgument's `-XX`) by design or the harness's. With slices 1-5 as built: about 18 of the 31 (the
counts and ids, RuntimeMXBean's, InvalidThreadID, the deadlock tests, the locked monitors and synchronizers where
their frames are of waiting threads, ThreadBlockedCount and SynchronizationStatistics, three of the four
OperatingSystemMXBean tests; MemoryStatusOverflow, ThreadAllocatedMemoryArray, LastGCInfo, ThresholdTest,
GetObjectName, ThreadStartTest and the CPU-time ones stay). The change of `Thread.getState` (BLOCKED, WAITING, TIMED_WAITING
where jrt said RUNNABLE) reaches the whole corpus: the night's run shows what it gains and whether it costs.

**The night's commands** (F13's part, after F12's conversion and build of the same night: one `bin/jrt-convert
update` and one `bin/arbace-go --build` take every area of the branch):

```sh
# the same jdk/ conversion (expect + 36 files, 0 failed, 0 javac errors), edge and build as F12's
# then a smoke of the beans
target/arbace-go/amd64/arbace -e '(let [t (java.lang.management.ManagementFactory/getThreadMXBean)
                                     r (java.lang.management.ManagementFactory/getRuntimeMXBean)]
  (println (pos? (.getThreadCount t)) (pos? (alength (.getAllThreadIds t))) (.getState (.getThreadInfo t (.threadId (Thread/currentThread))))
           (pos? (.getPid r)) (>= (.getUptime r) 0) (.findMonitorDeadlockedThreads t)))'
# expect "true true RUNNABLE true true nil"
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(java/lang/management|com/sun/management|javax/management/mxbean)|jdk/java/lang/Thread' \
  --save .tmp/f13-night/check
```

## 6. Findings on the way

- `:j2g-unresolved` (305 compiled, kind `:world`) is the compiled level's catch-all for "a type the world does
  not have": its tests are the world areas' (crypto, PKI, TLS, ...), 53 of them design-blocked. Since JCS44 (a)
  j2g's refusal names the import; the analysis' `Cannot convert ... of type :null` does not. Proposed: JCS44's
  naming extended to that message (j2g: the unresolved class in the refusal), so these tests fall in their
  areas' classes as the interpreted level's do.
- The interpreted `:javac-api` tests half need the class-file API too (20 of 39): running them on
  `arbace-javac` (F6) regains about 21, not 39.
- `:interp-protected`'s 19 interpreted tests are mostly not the interpreter's: 6 subclass Provider (F1), 3 a
  ClassLoader (by design), the rest single members (F16, F21).
- `:repl-sibling` (9) holds 4 SASL tests whose package order the REPL could not resolve; with SASL out
  (JCS133) they are by design.
- JCS99 (c) (the harness's tests' digests recorded as `"-"`) was decided on 2026-10-26 but the reference has
  not been recorded since (`--record-ref` needs the system's JDK): `:clock`, `:identity-sum` and the identity
  hash code tests still differ.
- (F4's day, on F1's branch) `java-jc9-jca`'s `overlay/jdk/variants/PBEKey.ae` cuts `readObject` as
  `(j2g/cut ^:private readObject ^void [^ObjectInputStream s])`, which matches no member: an instance method's
  cut names `this` first (`arbace.j2g.world`'s `param-key` drops an instance method's first parameter), so
  `apply-variant` throws "j2g: variant cuts no member: readObject" (checked with F4's `vet.ae` on F1's converted
  `PBEKey`), and F1's night would stop at j2g. The fix, on `java-jc9-jca`: `[this ^ObjectInputStream s]`. It is the
  only such cut among F1's and F5's variants.
- (F4's day) `java-jc9-jca` and `java-jc9-logging` conflict with each other (`git merge-tree`): in
  `bin/jrt-convert` (both insert after `MakeJavaSecurity`'s call in the generate step), in
  `test/jrt/jrt_sources.ae` (both insert after `zip-sources` in `sources`, and a `def` before `overlay-sources`)
  and here (both append at the end). The resolution is the union in each place; `java-jc9-pki` merges cleanly
  with either.
- (F3's day) F1's `closure.py` takes jrt's hand-written classes into the world by their top-level name
  (`world-classes.txt` cut at `$`), so a nested type jrt does not have passes as present: `SSLLogger implements
  System.Logger` and its `System.getLogger` were invisible to the scan and showed on reading the file (jrt's
  `System` has no `Logger`, `Logger.Level` or `LoggerFinder`). F3's new files were then checked by hand against
  jrt's manifest for `System`'s, `Thread`'s and `Runtime`'s members (only `getLogger` missing). The scan should
  read `go/arbace/jrt/manifest.edn`'s nested types and members; the next widening's day can add it.
- (F19's day) The interpreter's view of `Class` and `Module` lacks `getResourceAsStream` ("No accessible method
  getResourceAsStream in java.lang.Class"), which the compiled program has (j2g's `out.ae` adds it to every
  class): a Java form calling it works compiled and fails interpreted. Its member tables
  (`test/jrt/tables.ae`) could list it as the compiled side has it; a JC4-style finding for the interpreter.
- (F19's day) `test/java/run.ae` orders a test's packages by their imports and takes the first by name when they
  import each other in a cycle; JDK-style package cycles (javax.naming and its spi, internal) then load in an
  order that fails ("Class ... is not defined yet"). No jtreg test of the corpus has such a cycle (the day's proof
  did); `--load do` with the packages' classes entered together across packages would take it.
- (F12's day) jrt's hand-written `System` has no `getProperties` for translated code (SeedGenerator's overlay says
  so since F1): a JDK class reading its properties as a `Properties` (`sun.net.www.http.HttpClient`'s static
  initializer) needs a variant reading them one by one. The closure scan's member check (F3's finding: the scan
  should read `go/arbace/jrt/manifest.edn`) found it on F12's day: a check of each `System.`, `Thread.`, `Class.`
  static member the new files call against the manifest, `.tmp/jc9c/statics.txt`.
- (F6's day) The interpreter on javac's tool runs Java forms against javac's world as on the shipped program
  (`test/java/run.ae` unchanged): the overlay classes of javac's world can be tried by day, renamed, before a tool
  build (`.tmp/jc9c/f6x/proof.sh`), except where they extend a class not in `class-supers` (then a copy implementing
  the interface itself).
- (F3's day) `arbace.j2g.vh` compiles `findVarHandle` on an instance field, with the class or field type a
  literal or a final local bound to one, but not `findStaticVarHandle` (a static field) nor a handle stored
  through a local (`QuicTLSContext`'s `vh = lookup.findVarHandle(...); CONTEXT_SPI = vh;`). jdk26u's java.base
  has three `findStaticVarHandle` users outside java.lang.invoke: `SSLContext` (F3's variant),
  `DatagramSocketAdaptor` (F10) and `jdk.internal.event.EventHelper` (jrt's overlay since F2). A small j2g
  amendment (static fields in the handle table, a local's initializer followed) would retire such variants;
  not needed by F3.

- (JC9b's day) **F5's `Logger` and `LogRecord` do not compile against jrt's own `ResourceBundle`** (overlay):
  they call `ResourceBundle.getBundle(String, Locale, ClassLoader)`, which jrt's has not ("JC9b's night", the
  joint check of JC9 and JC9b merged); `bin/jrt-convert`'s convert step reads with `REPR_JAVA_PATCH_ALL` against
  the overlays, so F5's night would stop at "2 javac errors". The fix, on `java-jc9-logging`: jrt's
  `ResourceBundle` gains the overload (the loader ignored, as jrt has one: the two-argument lookup), or F5's two
  callers an overlay each; then the joint check again (`.tmp/jc9b/all9/run.sh`, a minute, 1 GB).
- (JC9b's day) The javac light checks of F1, F5, F4 and F2 compiled each fix's files against the JDK's own
  java.base, not against jrt's overlays (Path, Files, FileDescriptor, ResourceBundle ...), which the convert
  step's attribution and javac step use; F7's check over the whole closure found jdk26u's `FileChannel.open`
  calling `Path.getFileSystem()` (hence F7's FileChannel overlay), the joint one F5's `getBundle` and F1's cast
  to `DirectBuffer` (F7's `MappedByteBuffer` made non-sealed). The joint check is cheap (16 s, 1 GB) and belongs
  before every conversion night: the script `.tmp/jc9b/full/run.sh` (print-sources' list, jdk26u's files, the
  build's gensrc, the overlays) could become `bin/jrt-convert check-javac`, a light step (proposed nowhere yet:
  a small follow-up, without the user's decision it stays a scratch script).
- (JC9b's day) `Readable`, `Scanner` and `PropertyResourceBundle` were javac's tool world's
  (`overlay/jdk/javac/closure.txt`, JN7: the JDK build's tools read with Scanner): F16 moves them, with the
  Scanner variant, into the shipped closure; on JC9b's night they stay in javac's world for the first tool build
  (the charsetmapping tool reads with Scanner before `jdk/` has it) and leave it after the conversion ("JC9b's
  night", steps 1 and 2b). Likewise F8's `overlay/jdk/javac/variants/InputStreamReader.ae` (which adds the
  CharsetDecoder constructor jdk26u's reader has) is removed after the conversion, not before.
- (JC9b's day) Seven of the tests the triage counted F8-alone (`EUCJPUnderflowDecodeTest`, `EucJpLinux0212`,
  `FindOneCharEncoderBugs`, `TestUTF8`, `TestUTF8BOM`, `DirectBufferAllocTest`, `BashStreams`) also need F7's
  direct buffers or `Channels`; `Executors/Throws`, counted F16's, is F4's (`PrivilegedAction`); `jdk.sctp`'s
  `MessageInfoTests`, counted F11's, is neither (SCTP). The sections' expected yields count them where they land.
- (JC9b's day) Only 34 of jdk26u's datagram, multicast and interface tests are in the check's corpora (the rest
  of `jdk/java/net/MulticastSocket/` use `/test/lib` or testng: JC1's selection), so F11's code is judged by fewer
  tests than it serves; `InetAddress.isReachable` could fall back to TCP echo once F11's sockets exist (a small
  follow-up).
- (JC9b's day) With F7 in, F5's `LoggingFileHandler` variant's cut of `lockFileChannel` can go (the JDK's lock
  file over `FileChannel.open` and `tryLock`): a follow-up after both nights.


## As built: the REPL's exports (JCS136 (a), JCS142 (a); branch `java-internal-pkgs`, 2026-11-05)

**How `@modules` reached the check.** It did not: `categorize` (`test/java/check.ae`) filed every test with an
`@modules` tag as `:run-modules`, a category no corpus takes, so the tests that export an internal package were not
in the reference at all (`ref-jdk26.edn` has none), whatever the world held. Two things stood between them and a pass:
the tests were not in the corpora, and the REPL's member tables left the packages' classes out
(`arbace.j2g.main/repl-visible?`: the edge's `:unexported` set, `jdk/edge.edn`).

**What was built.**
- `jdk/repl-exports.txt`: the packages, one a line, exact: F1's (JCS136 (a): `com.sun.crypto.provider`,
  `sun.security.{util,x509,pkcs,provider,rsa,ec,ec.ed,ec.point,util.math,util.math.intpoly,internal.spec}`), `sun.security.jca`
  (the JCA's registry, in F1's closure and named by the tests' `@modules`; not on JCS136's list) and F2's (JCS142 (a):
  `sun.security.{pkcs12,provider.certpath,validator,timestamp,tools}`). It lies under `jdk/`, so it is in
  `arbace_runtime_key` and the gate's keys.
- `repl-visible?` takes a class of a listed package as visible (the edge's `:unexported` is not consulted for it):
  the executable's member tables keep its public members and the REPL names them. Checked on stage 0: `AlgorithmDecomposer`,
  `SHA`, `AESCipher`, `X509CertImpl`, `IntegerModuloP`, `PKIX`, `Validator` visible; `jdk.internal.misc.Unsafe`,
  `sun.net.www.URLConnection`, `sun.nio.fs.Util` still not.
- `test/java/check.ae`: a test whose `@modules` entries are all `java.base/PKG` of that list is not `:run-modules` (its
  other categories apply), and its entries become javac options `--add-exports=java.base/PKG=ALL-UNNAMED` in its plan
  (`:javac` in the reference); an entry of another module or package keeps it `:run-modules`. A main class of a
  module (`java.base/PKG.Main`: the test patches the module) is skipped with its reason.
- `bin/java-check`: `--record-ref` passes the options to the JDK's `javac` (the source set, the classes) and to its
  `java`; `read_opts` passes them to repr/java's read (`--javac`: javac's attribution of the unexported package would
  fail without them: AssertionError, ClassCastException, observed); the classes level already passed all `:javac`
  options to javac on Arbace.
- The REPL needs nothing per test (the visibility is the executable's, as option (a) says): the JVM's refusal without
  `--add-exports` has no counterpart in the interpreter.

**Checked (light, the gate's executable of the previous key: its tables predate the change).** `--record-ref --only` of
three tests that need no F1/F2 class (`DecomposeAlgorithms`, `ExpandAndEncode`, `DigestKAT`: t08362, t08376, t08351, the
reference by the JDK 26 build, the 4,850 other lines kept byte for byte), then `--levels read,interp,classes`: read 3
of 3, classes 3 of 3 (javac on Arbace with the exports, byte for byte the JDK's), interpreted `ExpandAndEncode`
passes; `DecomposeAlgorithms` fails with `j2g: not translated: sun.security.util.AlgorithmDecomposer.<clinit>` (the
class is in the world, unrooted: the failure `repl-visible?` removes at the next executable build, to confirm then),
`DigestKAT` stops at `ClassNotFoundException: sun.security.provider.MD4` (an F1 class). A sample of 15 essential tests
at read and interp: 15 of 15 as recorded, 0 regressions (the change touches only tests with an `--add-exports` option).

**The tests to judge when F1 and F2 land.** The scan finds 86 tests (4,936 in the corpora instead of 4,850; 81 not
skipped): by option, `sun.security.util` 51, `x509` 28, `util.math` and `util.math.intpoly` 15 each (one test),
`provider.certpath` 6, `provider` 5, `internal.spec` 4, `pkcs` 3, `validator`, `rsa`, `com.sun.crypto.provider` 1;
by area `sun/security/util` 29, `x509` 19, `com/sun/crypto` 11, `sun/security/provider` 10, `com/sun/security` 4,
`java/security/cert` 3, the rest 1-2. The 68 and 25 of JCS136 and JCS142 were estimates of the same set; the count
is 86 with `sun.security.util` and `x509`, which F1 and F2 share. The night's commands: after F1's classes are in the
executable, `bin/java-check --record-ref --only 'add-exports'` (the new tests' reference and ids, the other lines
kept), then the levels read, interp, compiled and classes with `--only` on those ids, `--record`; `bin/reap --record`.
Not run here: the executable's rebuild (a build), the compiled level (needs the world's classes).

**Left.** The ids t08351, t08362 and t08376 are in `test/java/ref-jdk26.edn` (the other 83 come with the nights'
`--record-ref`, since a full one rewrites the scan's plans); their `check-LEVEL.edn` lines are not recorded (a record
needs the new executable). If F1's night finds a listed package that tests need beyond `repl-exports.txt`, one line
adds it.

## Proposed

JCS161 is F13's ("F13 (java.lang.management's MXBeans): not built, JCS161"); JCS158-JCS159 are F6's ("As built:
F6"), JCS160 F12's ("As built: F12 (write-ahead)"), branch `java-jc9c`;
JCS155-JCS157 are F3's ("As built: F3 (write-ahead)", branch `java-jc9-tls`); JCS143-JCS154 are JC9b's
(`java-jc9b`). JCS140-JCS142 are F4's and F2's ("As built: F4 (write-ahead)", "As built: F2 (write-ahead)", branch
`java-jc9-pki`); JCS136-JCS137 are F1's (`java-jc9-jca`), JCS138-JCS139 F5's (`java-jc9-logging`).

### JCS161 java.lang.management without JMX, or with JMX's core (F13)

JCS95 (a) put JMX (javax.management) by design and JCS128 (a) deferred java.lang.management's MXBeans (31 / 31 tests);
jdk26u's java.lang.management names JMX throughout (ManagementFactory's MBean server and proxies, every bean's
`getObjectName`, the platform beans' composite data for `ThreadInfo` and `MemoryUsage`).
- (a) java.lang.management alone, JMX cut by variants: `ManagementFactory`'s `getXxxMXBean`s and
  `getPlatformMXBean(s)` over jrt's own implementations of the interfaces (jdk26u's `sun.management` impls where
  their Java stands, `ThreadImpl`, `RuntimeImpl`, `MemoryImpl`, with jrt's natives in Go: thread ids and states,
  stack traces, uptime and input arguments, the process id, Go's `runtime/metrics` for memory and GC, and for the
  contention, monitor and deadlock facts what jrt's monitors and locks can record), `getObjectName` and
  `getPlatformMBeanServer` throwing `UnsupportedOperationException`. Cost: about 5 days (2 of Go: owners and waiters
  recorded by jrt's monitors, which also slows every monitor a little); about 22 of the 31 (GetObjectName and the
  MBean-server tests stay by design).
- (b) With JMX's core (ObjectName, the MBean server, StandardMBean, the open types and MXBean mapping: about 250
  files, 70k lines): every java.lang.management test and part of javax.management's own (`:jmx`'s 34 / 37 then
  deferred, not by design). Cost: about 10 days, reversing JCS95 (a) in part, +15% on the closure.
- (c) Deferred: java.lang.management stays out until JMX is decided again.
- *Recommended: (a)*: the MXBeans are what a program asks of its runtime (threads, memory, uptime), and they need
  no MBean server; JMX's remote management is a JVM's, as JCS95 (a) found.

### JCS158 javac's API tests at the compiled level (F6)

F6 runs the 56 tests that name javac's API interpreted on javac's tool; the compiled level still translates them with
the shipped program (`bin/j2g --program` over the shipped build's compiled namespaces), which has none of javac's
world, so j2g refuses each (`:j2g-unresolved` filed `:javac-api`): 21 tests the triage counts as F6's compiled.
- (a) As built: the compiled level of these tests stays deferred under `:javac-api` until (b) is worth its night.
- (b) A shard of its own over javac's tool's program: the tests' Java forms translated with javac's world's forms
  and the tool's compiled namespaces (its `aot-reader` base, `bin/javac-vendor tool`'s), built and run as the other
  shards. Cost: a day of `bin/java-check`'s compiled level (a second program base, its key) and, each night, a j2g
  run of javac's world (about the tool build's: heavy, `:mem-gib` near the tool's); +about 16 compiled.
- (c) By design at the compiled level: javac's API is the tool's, not a user program's. Cost: none; the 21 leave
  the AOT cell's deferred count for good.
- *Recommended: (a)*, (b) after the GT8 budget is known (one more whole-world j2g run a night): the interp level
  already proves the API on javac on Arbace, and (c) would call by design what Arbace can do.

### JCS159 javac's ToolProvider on javac's tool (F6)

javac's tool world has jrt's ServiceLoader, which finds no provider (JN2), so `ToolProvider.findFirst("javac")` and
`ServiceLoader.load(ToolProvider.class)` find none: Help.java (javac's `-help` and `-X` text) and
ToolProviderDescriptionTest (any tool observable) fail, `:javac-tool`. jdk26u's `JavacToolProvider` runs javac's own
launcher over JavacFileManager, which the tool has not.
- (a) A javac ToolProvider of jrt's own (overlay/jdk/javac/jdk.compiler, beside SystemCompiler), `run` over javac on
  Arbace's command line (`Main.runOnPlatform`), found by `ToolProvider.findFirst` and the ServiceLoader's stream for
  `java.util.spi.ToolProvider` (the two variants of the tool world); `-help`, `-X` and `--version` printed by
  javac's own `com.sun.tools.javac.main.Option` help over Arbace's file manager, as `Main` does not today. Cost: a
  day by day, a night's tool build; +2 tests (Help, ToolProviderDescriptionTest), and the tools' tests that ask for
  javac by name later.
- (b) Deferred until a tool test of another corpus asks for it.
- *Recommended: (a)* with F6's follow-ups: javac's command line help is javac's own text, and the provider is the
  API a program uses to run javac in process.

### JCS160 Every client socket in a SocksSocketImpl again (F12)

Until F12 jrt's `Socket.ae` gave a client socket the platform's impl itself, "no proxies in the Go build". jdk26u wraps
it in a `SocksSocketImpl` that asks the proxy selector (and `socksProxyHost`) at each connect; F12 brings the
proxies, and BadProxySelector (t05015) needs the wrapper (its selector is asked for `socket://` URIs).
- (a) As built: jdk26u's `createImpl`; each connect first asks `ProxySelector.getDefault()` (DefaultProxySelector,
  the Java properties, `conf/net.properties`), as on the JDK. Cost: a selector call a connect; a risk on every socket
  test until the night's corpus run shows none lost (the fallback is (b)).
- (b) The platform impl unwrapped as before, `Socket(Proxy)` the only way to a proxy. Cost: BadProxySelector and
  `socksProxyHost`, `socksProxyPort` and `java.net.useSystemProxies` for plain sockets left out, a divergence from
  the JDK to record.
- *Recommended: (a)*: it is the JDK's algorithm, and its cost is one map lookup of properties a connect.

### JCS155 System.Logger: jrt's own now, or SSLLogger without it (F3; JCS138 (b))

JCS138 (b) left `System.getLogger` until F3, whose `SSLLogger` implements `System.Logger`. `java.lang.System`
is jrt's hand-written Go: `System.Logger` (an interface with eight default methods), `System.Logger.Level` (an
enum) and `System.LoggerFinder` (an abstract class) would be new hand-written types, with default methods,
which no jrt interface has had; only a j2g run can try them (the manifest, the translated `SSLLogger`
implementing a hand-written interface, the defaults dispatched). F3 as built does not need them.
- (a) jrt's own: the three types in `go/arbace/jrt/system.ae` and the manifest (`bin/jrt manifest`),
  `System.getLogger(String)` and `getLogger(String, ResourceBundle)` over F5's `LoggingProviderImpl` (its
  wrappers translated, `LoggerFinder.getLoggerFinder` the java.logging one directly, no `BootstrapLogger`),
  jdk26u's `SSLLogger` unchanged but for `formatCaller` (a variant: no StackWalker), F2's `EventHelper` logging
  as the JDK's. About 2 days by day plus a night (a build of jrt and j2g's check); +1 test
  (`LoggerInterfaceTest`) and the JDK classes that log through it (`EventHelper`, `SSLLogger`).
- (b) As built: jrt's `SSLLogger` (overlay) implements no `System.Logger`, java.util.logging reached by
  reflection when `javax.net.debug` is empty; System.Logger deferred until a second user or F20 (StackWalker),
  `:logging` narrowed to `System\$Logger` meanwhile.
- *Recommended: (b)* for F3's night (javac checked it, no Go to try by night), (a) as its own small fix after
  JC9's nights, when a build of jrt can be tried by day.

### JCS156 QUIC's TLS engine in F3, or cut by variants

JCS131 (a) planned SunJSSE "QUIC's engine left out". jdk26u's handshake names it in 13 files (`Finished`,
`ServerHello`, `CertificateMessage`, `KeyUpdate`, `SSLConfiguration`, `TransportContext`, the key managers'
`QuicTLSEngineImpl` overloads, `SSLExtension`'s transport parameters, ...).
- (a) In, as built: `sun.security.ssl`'s five QUIC files and `jdk.internal.net.quic`'s seven interfaces (4.4k
  lines, +5.6% on F3), plain Java over F1's ciphers; never reached without HTTP/3 (`QuicTLSContext`, its only
  entry, stays out). Cost: none by day; +0.7 MB.
- (b) Out: 13 variants, each a member or more with its QUIC branch cut (the extension's producers, the key
  managers' overloads), about a day, and each a divergence to keep in step with jdk26u.
- *Recommended: (a)*: less work and no divergence; the engine is there when F12b's HTTP/3 needs it.

### JCS157 java.net.IDN in F3, or SNIHostName without it

`SNIHostName(String)` and `(byte[])` call `IDN.toASCII(name, USE_STD3_ASCII_RULES)`; `IDN` is not in the world
(F2's `HostnameChecker` and F4's `SocketPermission.checkForIDN` stub it).
- (a) In, as built: `java.net.IDN` and ICU's `StringPrep`, `Punycode`, `StringPrepDataReader`, `Trie`,
  `CharTrie`, `UCharacterDirection`, `UCharacterEnums` (2.9k lines), the profile `sun/net/idn/uidna.spp` (20 KB)
  in `jdk/data`; F2's and F4's stubs become the JDK's code. Cost: half a day (done), the data step's line.
- (b) Out: `SNIHostName`'s variant checks STD3 ASCII rules itself and rejects a non-ASCII name (the JDK accepts
  `bücher.example` as `xn--bcher-kva.example`): `ServerName`'s IDN tests fail, `:tls-idn`.
- *Recommended: (a)*: the JDK's behaviour for little, and `jdk/java/net/IDN`'s own two tests (`IllegalArg`,
  `UseSTD3ASCIIRules`, filed under F12's `net-http` by the scan) can pass with F3.

### JCS140 The trust anchors: jdk26u's cacerts or x509roots' roots (F2)

PKIX's default anchors (`sun.security.util.AnchorCertificates`, `KeyStoreUtil.getCacertsKeyStore`,
`TrustStoreUtil`) and the tests that open `${java.home}/lib/security/cacerts` read the JDK's cacerts keystore,
which the JDK build generates from `src/java.base/share/data/cacerts/` (jdk26u's own CA list, about 110 PEM
files) with `build.tools.generatecacerts.GenerateCacerts` (`make/modules/java.base/Gendata.gmk`). The Go build has
none (F2 as built: the readers find no file, as the JDK without it: no default anchors). The checkout's
`x509roots/` holds Mozilla's NSS roots (120, as DER in `bundle.der`) for jrt's TLS (BX73).
- (a) jdk26u's cacerts as resource data (`jdk/data/lib/security/cacerts`, the image's file, as `tzdb.dat` is
  data): GenerateCacerts cannot run on javac's tool (it needs KeyStore and CertificateFactory, which the tool's
  world does not hold), so the data step copies the build tree's `images/jdk/lib/security/cacerts`, a
  `:boot-jdk`-style exception recorded in `generated.edn`; `FilePaths.cacerts()` reads the resource (a variant).
  The JDK's tests of the list pass (`VerifyCACerts`, `TestCACerts`), the anchors are the JDK's. Cost: half a day;
  134 KB of data.
- (b) x509roots' roots: a variant of the three readers building the cacerts keystore in memory from the bundle
  jrt's TLS already embeds (one root set for Go's TLS and Java's PKIX, no new data). Cost: a day (the bundle's
  DER read through jrt, a PKCS12 keystore made at first use); `VerifyCACerts` and `TestCACerts` fail (another
  list: `:jrt-trust-anchors`, by design), and Java's PKIX and the JDK disagree on which CAs are trusted.
- (c) None: as built; the two tests deferred, and any default-anchor validation fails as the JDK without cacerts.
- *Recommended: (a)*: the check's reference is the JDK, and the anchors are data the JDK ships; (b) when jrt's
  TLS and SunJSSE (F3) are to share one store, with JCS131's TLS.

### JCS141 Jar verification: in F2 or after it

jrt's `JarVerifier` and `ManifestEntryVerifier` (JC6b's overlay) verify nothing: a signed jar reads as unsigned,
`JarEntry.getCertificates` and `getCodeSigners` answer null. With F2's PKCS #7 and signature verification in the
world, jdk26u's `JarVerifier`, `ManifestEntryVerifier`, `sun.security.util.SignatureFileVerifier`,
`JarConstraintsParameters` (in F2's closure) and `ManifestDigester` can replace them; jrt's `JarFile` (overlay)
must then call the verifier as the JDK's does (`getManifestFromReference`, `maybeInstantiateVerifier`, the
`JarFileEntry` certificates). 5 of F2's tests need it (`CodeSource/CertsMatch`, `JarEntry/GetMethodsReturnClones`,
`JarFile/ScanSignedJar`, `JarInputStream/ScanSignedJar`, `JarFile/TurkCert`).
- (a) In F2, before its night: 1.5 days (the overlay's JarFile changed, the jar and zip checks rerun), +2k lines.
- (b) After F2, its own small fix (F2b) with JC9b: the same work, its tests deferred until then.
- *Recommended: (b)*: 5 tests for 1.5 days is a third of F2's rate, and it touches JC6b's JarFile, which the jar
  and zip tests pass on; F2 lands without risking them.

### JCS142 F2's internal packages at the REPL, as JCS136 (a) F1's

JCS136 (a) made F1's internal packages visible at the REPL for the tests that export them with `@modules`.
About 25 tests of PKI's areas export F2's (`sun.security.pkcs12`, `sun.security.provider.certpath`,
`sun.security.validator`, `sun.security.timestamp`, `sun.security.tools`, and `sun.security.x509`, `pkcs`, `util`
for classes F1 did not have) and are outside the 89 counted (the scan counted the tests without them).
- (a) The same list extended (`arbace.j2g.main/repl-visible?`): those packages' public members kept in the
  executable's member tables, perhaps 2-3 MB beyond F2's.
- (b) As JCS136 (b): those tests in `:repl-unexported`, interpreted only.
- *Recommended: (a)*, with JCS136 (a)'s implementation, measured on F2's night.

### JCS128 `:management` split as JCS95 decided

JCS95 (a) put JMX (`javax.management`) by design and left java.lang.management's MXBeans deferred (b); the class
`:management` still files both as `:world`. 34 interpreted and 37 compiled deferred tests name JMX, 18 and 18
only JMX.
- (a) Split: `:jmx` (`:kind :design`, `javax\.management`) before `:mxbeans` (`:world`,
  `java\.lang\.management|com\.sun\.management`). Cost: an hour, `bin/reap --record`. 18 / 18 move to by design.
- (b) Leave it until F13 is built.
- *Recommended: (a)*: it applies a decision already taken, and the matrix should say what was decided.

### JCS129 A deferred test that also needs a by-design feature is by design

36 interpreted and 108 compiled deferred tests name, beside their filed cause, a feature J2G-SPEC §12 or JCS95
(a)/JCS97 (a) put out for good: serialization (48 compiled: j2g refuses at an earlier unresolved type), the
class-file API (25 / 29), XML (2 / 21), class loading (8 / 5), JDI, smartcardio, FFM. No fix regains them.
- (a) A generated `:design` class, `:design-also`, listing them by `:tests`, made by the static scan (a script of
  `bin/java-check`'s, rerun when the corpus or the world changes), placed before the world classes. Cost: 1 day
  (the scan as Arbace code, `test/java/check.ae`, rather than this triage's Python). Moves 36 / 108.
- (b) The same by hand-curated `:tests` once (half a day), stale as the world grows.
- (c) Leave them deferred: the matrix overstates the deferred work by 3% / 9%.
- *Recommended: (a)*: the compiled level's 108 are the largest single correction, and the scan is cheap to keep.

### JCS130 The harness's classes out of the deferred count

17 interpreted and 17 compiled tests are the runner's: no JVM launcher (`:jvm-tools` 3), no heap ceiling
(`:memory-limit` 4), identity hash codes and their sums (`:identity` 3, `:identity-sum` 1), a run's clock
(`:clock` 1), the temporary directory (`:environment` 1), thread order (`:thread-order` 2,
`:interp-synchronized` 1), `:flaky`, `:read-failed`.
- (a) Apply JCS99 (c) at the next `--record-ref` (status only for `:identity*`, `:clock`, `:thread-order`,
  `:interp-synchronized`: they then pass), and map `:jvm-tools`, `:memory-limit` and `:environment` to by design in
  `test/reap/cells.edn`'s `:kind-map` (a `:kind :harness-design`, or those three `:kind :design`). Cost: half a day
  and the JDK's rerun of those tests. 
- (b) Only the kind-map: 17 / 17 to by design, none to pass.
- (c) Leave them.
- *Recommended: (a)*.

### JCS131 TLS: SunJSSE translated, not javax.net.ssl over Go's crypto/tls

JCS90 (b) planned javax.net.ssl "over Go's crypto/tls" after the providers. With F1 and F2 built, jdk26u's own
TLS (`sun/security/ssl`, 138 files, 63k lines, and javax.net.ssl's 42) is plain Java over the JCA, PKI and
sockets, which the world would then have.
- (a) Translate SunJSSE (8 days after F1, F2): the JDK's behaviour exactly (SSLEngine's states, the handshake
  messages, cipher suite order, the properties the tests set), up to 94 / 94 tests with F1-F2. Cost: +16% closure,
  SSLLogger's and the handshake's Cleaner/StackWalker uses as variants; QUIC's engine left out.
- (b) javax.net.ssl over crypto/tls (6-10 days, JCS90's): smaller, but SSLEngine's buffer API over a stream library
  is a design of its own, and the tests (javax/net/ssl, sun/security/ssl: handshake details) judge the JDK's
  implementation, which (b) would not have; jrt's own HTTPS client keeps crypto/tls either way.
- *Recommended: (a)*.

### JCS132 RMI by design

RMI's tests (11 interpreted, 15 compiled; 9 and 12 alone) marshal by serialization, which is by design
(J2G-SPEC §12), and register stubs by class loading.
- (a) By design, beside serialization: J2G-SPEC §12 amended, `java\.rmi` a `:design` class. Moves 9 / 12.
- (b) Deferred as now.
- *Recommended: (a)*.

### JCS133 Kerberos and GSS-API by design, SASL deferred

`sun/security/krb5`, `jgss` and `javax.security.auth.kerberos` need a KDC and keytabs the sandbox has not; the
SASL mechanisms (DIGEST-MD5, CRAM-MD5, PLAIN, NTLM) are plain Java over the JCA.
- (a) Kerberos and GSS-API by design; SASL deferred, after F1 and F4 (2 days).
- (b) All deferred (F4b, 6 days).
- *Recommended: (a)*.

### JCS134 The first wave: JC9, the world's security half and its cheap neighbours

- (a) A milestone JC9 of F1, F5, F4, F2 (16.5 days, up to 341 / 332 tests, about 240 / 230 expected), in that
  order, each its own branch and gate; then F16, F8, F7, F11 as JC9b (16 days, up to 223 / 217 more).
- (b) Smaller first: F5, F4, F16 (8.5 days, up to 139 / 129), the JCA after.
- (c) No widening before the next REAP tag; the reclassifications (JCS128-JCS130, JCS132) alone.
- *Recommended: (a)*, with JCS128-JCS130 applied first (they change the denominator, not the work).

### JCS135 The closure's growth

F1-F3 add about 210k Java lines (+45%) to the shipped executable's closure; all eight first fixes about 250k.
The REPL needs every class at hand (the interpreter calls the world by reflection), so they ship.
- (a) Ship them, measure the executable's size and the gate's build time at each step (JC6b's 10k lines cost
  nothing measurable; 210k may cost a minute of j2g and 30-60 MB).
- (b) A security tier: the JCA, PKI and TLS translated into the shipped executable only when a build option or
  a program (`--build-program`) asks for them; the check's executables with it.
- *Recommended: (a)*, revisited if the build grows past a minute.

### JCS136 The JCA's internal packages visible at the REPL for the tests that export them

Of jdk26u's 576 tests of the JCA's areas (`jdk/com/sun/crypto`, `javax/crypto`, `sun/security/{rsa,ec,provider}`,
`java/security`), 68 export a JCA internal package with jtreg's `@modules` (`java.base/com.sun.crypto.provider`,
`sun.security.util`, `sun.security.x509` ...) and call its classes. The REPL's world roots only the exported
packages' classes (`arbace.j2g.main/repl-visible?`: the edge's `:unexported`, as the JVM refuses them without
`--add-exports`), so these tests stop at "Unable to resolve" interpreted, whatever F1 translates; compiled, j2g
translates them against the world and they may pass.
- (a) F1's internal packages visible at the REPL (`repl-visible?` takes `com.sun.crypto.provider`,
  `sun.security.{util,x509,pkcs,provider,rsa,ec,ec.ed,ec.point,util.math,util.math.intpoly,internal.spec}` as it takes
  the tool's): the tests' `@modules` honoured for them. Cost: half a day; the executable keeps every public member of
  those packages' classes (their member tables), perhaps 2-4 MB beyond F1's.
- (b) As built: those tests in a class of their own (`:repl-unexported`, `:kind :harness-design`: the JVM needs the
  test's `--add-exports` too), interpreted only.
- (c) An export at run time: the interpreter honours a test's `@modules` from a list the runner passes, the tables of
  the listed packages kept apart and loaded on demand. Cost: 1-2 days.
- *Recommended: (a)*, measured with F1's night: `jdk/com/sun/crypto`'s interpreted yield depends on it, and the
  members are translated anyway (the services reach them).

### JCS137 JCS99 (c)'s status-only tests: by the reference or by the classes

JCS99 (c) (with JCS104 (b) and JCS130 (a)) decided that the harness's order- and time-dependent tests (`:identity`,
`:identity-sum`, `:clock`, `:thread-order`, `:interp-synchronized`: 9 tests) be judged on their status alone, their
digests recorded as `"-"`. `bin/java-check --record-ref` writes `"-"` only where the JDK's two runs disagreed (JCS80
(a)); for these tests they agreed, so nothing writes it yet.
- (a) A list `--record-ref` reads (`test/java/status-only.txt`), the listed tests' digests written as `"-"`; then
  `bin/java-check --record-ref --only` of the 9 with the system's JDK (minutes) and `--judge --record`. Cost: an hour
  of `test/java/check.ae` and the JDK's rerun.
- (b) A class flag, `:status-only true`, that the judgment honours (a test of such a class compared by its status),
  no rerun. Cost: an hour; the reference keeps digests the check ignores.
- *Recommended: (a)*: the reference says what is compared, as JCS80 (a)'s `"-"` does, and the classes stay a
  classification.
### JCS138 System.Logger after java.util.logging

F5 brings java.util.logging without `System.getLogger` (its `LoggerFinder`, `jdk.internal.logger`'s bootstrap and
lazy loggers, `sun.util.logging.PlatformLogger`'s bridge): `java.lang.System` is jrt's hand-written Go, whose
nested `System.Logger`, `System.Logger.Level` and `System.LoggerFinder` would be new hand-written types, and the
corpus has one test that names them (`jdk/java/lang/System/Logger/interface/LoggerInterfaceTest`, filed under
`:logging` today, not counted in F5's 27).
- (a) Now, with F5: the three nested types in jrt (`system.ae`, `bin/jrt manifest`), `System.getLogger` and
  `getLogger(String, ResourceBundle)` bound directly to a jrt `LoggerFinder` over jrt's `LoggingProviderImpl`
  (overlay, which already holds `LogManagerAccess`), jdk26u's `LoggingProviderImpl`'s wrappers translated; no
  `BootstrapLogger` (the configuration is read before any logger exists). About 1.5 days; 1 test, and the JDK
  classes that log through `System.Logger` (none in the world yet).
- (b) Deferred until a widening brings a JDK class that logs through it (SunJSSE's `SSLLogger`, JCS131, does: F3)
  or a test area needs it; `:logging` narrowed to `System\$Logger` meanwhile.
- *Recommended: (b)*, taken up with F3, whose `SSLLogger` is the first in-world user.

### JCS139 Localized level names

`Level.getLocalizedName` and the system loggers' bundle read `sun.util.logging.resources.logging`, a
`ListResourceBundle` the JDK build generates from `logging.properties` (and ten translations) with
`build.tools.compileproperties` (make/modules/java.logging/Gensrc.gmk), a build tool javac's tool does not
carry (`bin/javac-vendor`'s `tool_dirs`; langtools' `compileproperties`, which it does carry, writes no `import`
line, so its output differs from the build's).
- (a) The tool added to `tool_dirs`, the root bundle (and the selected locales' translations, `JRT_LOCALES`)
  generated in `bin/jrt-convert`'s generate step as KIND `gensrc:java.logging`, compared with the build's
  `support/gensrc/java.logging`. Cost: half a day, and javac's tool rebuilt (its key changes): best folded
  into the next night that rebuilds the tool anyway.
- (b) No bundle: the lookup fails and `getLocalizedLevelName` answers the level's name, which is what the JDK
  prints in the root and English locales (the bundle's values are the names' capitalized forms, which the JDK
  upper-cases back); a non-English default locale prints English names.
- *Recommended: (b)* for F5, (a) with the next change that rebuilds javac's tool.

JCS143-JCS154 are JC9b's (branch `java-jc9b`): JCS143-JCS145 F16's, JCS146-JCS148 F8's, JCS149-JCS152 F7's,
JCS153-JCS154 F11's. JCS140-JCS142 are F4's and F2's ("As built: F4 (write-ahead)", "As built: F2 (write-ahead)", branch
### JCS143 F16's Go members: on F16's day, or as F16b after JC9b's nights

F16's batch (JCS96 (c)) names members of jrt's hand-written classes, which no light check can try and only a
build proves: Thread's `activeCount`, `getThreadGroup` with a ThreadGroup (jrt's own `java.lang.ThreadGroup`,
overlay Java: the name, the parent, the maximum priority, one `main` group every thread joins unless given
another; jdk26u's is over the VM's thread list), the constructors with a group and a stack size,
`ofPlatform` (jdk26u's `ThreadBuilders.PlatformThreadBuilder` translated over those constructors),
`getAllStackTraces`, `getContextClassLoader` (jrt's one loader); `Runtime.Version`'s members (jdk26u's parsing as
overlay Java, `Runtime.version()` and Version's Go methods delegating); `String.transform`, `contentEquals` of an
empty buffer, StringBuffer's `capacity` (a Go builder's cap, the JDK's 16 + growth rule) and an append's bounds;
Math.pow by StrictMath's fdlibm `pow`; Semaphore given a class interface (`Semaphore_I`, as JC4c did for
WeakReference) and `reducePermits`; ForkJoinPool's `schedule` and `getStealCount`; FileInputStream's
`skip(-n)`. About 20 tests interpreted and compiled (the list in "As built: F16 (write-ahead)").
- (a) On F16's day, before its night (2-3 days of Go with `bin/jrt test` and a build each), F16 then whole.
- (b) F16b, a Go-only fix after JC9b's nights: its own build and corpus rerun (2-3 days), F16's Java part
  landing first with JC9b's conversion.
- *Recommended: (b)*: the Java part needs a conversion night, the Go part a build day; apart, neither waits for
  the other, and the Go part is judged on a world that already holds Timer and the field updaters.

### JCS144 The version properties: the feature release, or the reference JDK's strings

F16 sets `java.runtime.version` and `java.vm.version` to `26`, as `java.version` (jrt is JDK 26's runtime,
`Runtime.version()` 26). Four tests print them and compare stdout with the reference JDK's run
(`compiler/c2/Test7179138_1`, `_2` twice: "Java Version: " + `java.vm.version`; `System/Versions`, which also loads
class files, by design): the reference is the build's `26.0.2.1-internal-adhoc.root.jdk26u`.
- (a) As built: `26`, and the four filed with the harness's classes (JCS130 (a)'s `:environment`, by design: a
  test that prints the reference JVM's own version).
- (b) The reference JDK's strings, `26.0.2.1-internal` and kin, as jrt's: the three pass, but jrt then names a
  JDK build it is not, and the strings change with the platform's pin.
- (c) The check masks the version strings in stdout (as it masks identity hash codes): the three pass with (a)'s
  values. Cost: a mask in `test/java/run.ae` (half a day), a mask that could hide a real difference.
- *Recommended: (a)*.

### JCS145 The field updaters' access check without the caller

jdk26u's `Atomic*FieldUpdater.newUpdater` is caller-sensitive (`Reflection.getCallerClass`): a field must be
accessible to the class that asks. jrt has no caller class; F16's updaters decide without it: a field that is not
public (or of a class that is not) is refused when its class is the platform's (java., javax., jdk., sun.,
com.sun.), accepted otherwise; a protected field's receivers are not narrowed to the caller's subclasses.
- (a) As built: the platform's fields refused, the program's accepted (a program's updaters are on its own
  classes, and the JDK refuses the platform's private fields to it); wrong only for a program's updater on
  another class's private field, which the JDK refuses and jrt allows.
- (b) A caller class from jrt's stack (the frames `Thread.getStackTrace` already gives: the first frame outside
  `java.util.concurrent.atomic`), the JDK's check exactly. Cost: a day, and a stack walk per `newUpdater` (one
  per class, usually in a static initializer).
- *Recommended: (a)* now, (b) with F20's StackWalker, which brings the frames' classes.

### JCS146 jdk.charsets: with F8, later, or not

F8 as built brings java.base's charsets as the JDK has them on Linux (60, the CJK ones included: what
`Charset.availableCharsets` lists in the JDK without jdk.charsets). jdk.charsets (sun.nio.cs.ext, 124 files of
which 111 generated by the same tool: the ISO-2022 family, the EBCDIC and other IBM pages, MS950/MS936/MS949,
x-eucJP-Open, x-windows-5022x, COMPOUND_TEXT; the JDK's 173 charsets in all) is 85,881 lines (+17.8% on the
closure, more than F8 itself) for 3 of F8's tests (`TestMS950`, `Test6392804`, `TestJIS0212Decoder`), and wider
coverage of the tests that iterate `availableCharsets`.
- (a) With F8: a second def (`charset-ext-sources`, KIND share:jdk.charsets and gensrc:jdk.charsets), the
  charsetmapping tool run again for `extcs` (GensrcCharsetMapping.gmk in jdk.charsets), `Charset.ae`'s
  `extendedProviders0` giving `List.of(new sun.nio.cs.ext.ExtendedCharsets())` and a JrtCharsets-like switch for
  its classes (its provider loads them by name too); `JISAutoDetect`'s `OperatingSystem` a variant. Cost: 1 day,
  +17.8% closure, perhaps +8 MB executable.
- (b) Later, as its own small fix when a user or a test area needs it (the tests stay in a `:charsets-ext` class,
  deferred).
- (c) By design: the Go build ships java.base's charsets; jdk.charsets' tests by design.
- *Recommended: (b)*: 3 tests for a sixth of the closure is the plan's worst rate; (c) only if JCS135's
  measure on F8's night shows the build near its threshold.

### JCS147 The readers and writers: jdk26u's, or jrt's with a delegate

jrt's InputStreamReader and OutputStreamWriter (overlay, JC6b's) code UTF-8, ISO-8859-1 and US-ASCII directly
and throw for the rest; every PrintStream, PrintWriter, FileReader and FileWriter goes through them.
- (a) jdk26u's, over StreamDecoder and StreamEncoder (as built): every charset, the CharsetDecoder/Encoder
  constructors (`IOCoders`, javac's file manager), `getEncoding`'s historical names (`String/Encodings`), the
  JDK's exact malformed-input and end-of-input behaviour; each write encodes through CharsetEncoder over heap
  buffers (the UTF-8 encoder's ASCII fast path is JavaLangAccess.uncheckedEncodeASCII). Cost: none beyond F8;
  a speed risk on output-heavy tests, measured on the night.
- (b) jrt's overlays kept for the three charsets and delegating every other to StreamDecoder/StreamEncoder, the
  decoder/encoder constructors added: the fast path stays, two code paths to keep equal to the JDK's (the
  FxStreams fixture proves the three). Cost: half a day; the overlays' 518 lines stay.
- *Recommended: (a)*, with (b) the night's fallback if the suite's or Java check's times grow by more than
  JCS135 (a)'s threshold.

### JCS148 The charset tables: generated on javac's tool, or copied from the build tree

- (a) Generated in `bin/jrt-convert`'s generate step by the JDK build's `build.tools.charsetmapping.Main` (11
  files into javac's tool world, `bin/javac-vendor`'s `tool_dirs`) and Spp for the coders, as built: the
  pattern of every generated source since JN7; on the JDK the outputs equal the build's byte for byte. Cost: as
  built; the tool's run time on the tool (the 2 MB of tables through Formatter) on each conversion.
- (b) Copied from the build tree's `support/gensrc/java.base/sun/nio/cs` (a `:boot-jdk`-style exception in
  generated.edn, as CaseFolding's tool-differing case): no tool in javac's world. Cost: an hour; one more
  generated source not generated.
- *Recommended: (a)*; (b) only should the tool fail on javac's tool on the night (risk 2).

### JCS149 File channels' positional I/O and locks: Java over today's natives, or new natives (F7)

F7 as built adds no native: a positional read or write saves the descriptor's position, seeks, reads or writes
and restores it under the descriptor's monitor, and a lock is checked against the process's own locks only
(the JDK's FileLockTable's check), not taken on the host. The JDK uses pread/pwrite and fcntl locks.
- (a) As built: no Go change; a stream reading the same descriptor in another thread during a positional
  operation can see the moved position (no test of the 84 does), and another process's lock is not seen
  (none of the tests locks across processes: `Lock.java`, which spawns one, is not among them).
- (b) Three natives in jrt (go/arbace/jrt/files.ae, HostFiles): `pread0`/`pwrite0` over Go's `os.File.ReadAt`/
  `WriteAt`, `lock0`/`unlock0` over `syscall.FcntlFlock` (F_SETLK/F_SETLKW, shared and exclusive), with the
  box's `hostfs` and `hostbox_tamago` answering "unsupported" for locks. Cost: half a day of Go, a build to
  check; FileChannelImpl's two call sites change.
- (c) (a) now, (b) when a test or a user's program needs it.
- *Recommended: (c)*: the tests judge the process's own behaviour, and (b) touches jrt's natives and the box,
  which F7's night need not risk.

### JCS150 A mapping: a heap copy written back, written through, or the host's mmap (F7)

`FileChannel.map` copies the region into a byte array; a READ_WRITE mapping is written back by `force` and when
its channel closes, never past the file's end. The JDK maps the file: another reader sees a put at once, and a
put sees another writer's bytes.
- (a) As built: correct for the tests that write through a map and force or close before reading (MapTest,
  Basic, Truncate, ZeroMap, MapOverEnd, ForceException); wrong for a program that shares a file between a map
  and a stream without force.
- (b) Write-through: every put of a READ_WRITE mapping also writes the bytes (a pwrite per put, the copy kept for
  reads): another reader sees them; puts into large maps (ExpandingMap, MapTest's loops) become a system call
  each. Cost: a day (the derived `DirectByteBuffer`'s puts call the mapping).
- (c) The host's mmap (`syscall.Mmap`) backing the Java byte[] (jrt's byte arrays are Go slices): true sharing,
  unmapped at GC (a runtime cleanup); no mmap in the box (TamaGo): (a) there. Cost: 2 days, jrt's array
  representation touched.
- *Recommended: (a)*, revisited by a test or a user's need; (c) is the JDK's semantics but crosses jrt's array
  model, which REAP's work does not need.

### JCS151 `DirectByteBuffer`: a derived overlay or generated by the generate step (F7)

jrt's `DirectByteBuffer` and `DirectByteBufferR` are `HeapByteBuffer(R)`'s generated text rewritten (the
derivation recorded in "As built: F7"), committed as overlays (1,700 lines of the JDK's code).
- (a) As built: overlays, regenerated by hand when jdk26u's template changes (jdk26u is pinned: rarely).
- (b) bin/jrt-convert's generate step derives them (KIND `jrtgen`, as `LocaleDataIndex`): after genbuffer, the
  rewrite as a sed script over its own `HeapByteBuffer{,R}.java`; nothing derived is committed but the forms.
  Cost: half a day (the step, print-sources' `jrtgen` lines, `origins`).
- *Recommended: (b)* before F7's night if the day allows, else (a) and (b) later: the rule "no generated source
  is copied" (JFS48 (b)) is better kept, and (b) keeps the derivation reproducible from the record.

### JCS152 A direct buffer's `isDirect()`: false (F7)

The Go build's direct and mapped buffers are heap buffers. jdk26u's code branches on `isDirect()` to read a
buffer through its address (`NIO_ACCESS.getBufferAddress`, `((DirectBuffer) b).address()`): with `true` those
branches would read address 16 + offset of a null base.
- (a) As built: `false`; `hasArray()` true (the copy's array), which the JDK's direct buffers answer false.
- (b) `true`, every `isDirect()` branch of the closure given a variant (CRC32's and Adler32's are already; GCTR,
  GHASH, GaloisCounterMode, CRC32C, the buffers' own bulk copies: about ten).
- *Recommended: (a)*: none of F7's 84 tests asserts `isDirect()` or `!hasArray()`; (b) is ten variants for a
  flag's spelling.

### JCS153 Datagram sockets: the legacy delegate over the host, or DatagramChannelImpl with F10

F11 as built gives `DatagramSocket` jdk26u's legacy `NetMulticastSocket` over jrt's `HostDatagramSocketImpl`
(the JDK's default delegate until JDK 15). Since JEP 373 the JDK's default is `DatagramSocketAdaptor` over
`DatagramChannelImpl`, which needs java.nio's channels, selectors and the poller (F10).
- (a) As built: the legacy delegate now (F11's 3 days, Go natives included), kept until F10; a test that
  depends on the adaptor's own behaviour (a message, connect's filtering of queued datagrams) fails in a
  narrow class. Cost: none beyond F11.
- (b) Wait for F10 and translate `DatagramChannelImpl` and `DatagramSocketAdaptor` (the JDK's path exactly;
  +3k lines, the channel's natives over the same Go). F11's tests wait 8 more days of F10.
- (c) (a) now, and switch `createDelegate` to the adaptor when F10 lands (the variant's else branch back to the
  JDK's; `HostDatagramSocketImpl` stays for factories' users or goes). Cost: half a day at F10.
- *Recommended: (c)*: the 38 tests now, the JDK's own path once its world exists.

### JCS154 IPv6 scope names now that NetworkInterface is in the world

`Inet6Address`'s variant (JC4/the sockets' days) drops the scope's `NetworkInterface` (it was a cut class): a
link-local address prints `fe80::1%2` where the JDK prints `fe80::1%eth0`, and `Inet6Address.getByAddress(host,
addr, NetworkInterface)` keeps only the index.
- (a) Revise the variant with F11: keep `scope_ifname` and `scope_ifname_set` (the JDK's fields), the
  interfaces' rows giving NetworkInterface-scoped addresses as the JDK's natives do. Cost: half a day;
  `Inet6Address/StringScope`, `B4923906` judged on it.
- (b) Leave it: the two tests in a class `:jrt-scope-name`.
- *Recommended: (a)*, on F11's day after the Go natives: small, and it removes a divergence the variant only had
  for want of NetworkInterface.


## Decided (2026-11-05, the user)

JCS138 (b): System.Logger deferred to F3 (SSLLogger); JCS139 (b) for F5, no localized level-name bundle, (a) at the next rebuild of javac's tool.

## Decided (2026-11-05, the user: JCS140-JCS142)

JCS140 (a): jdk26u's cacerts as data from the build tree; JCS141 (b): jar verification a later small fix, outside F2; JCS142 (a): F2's internal packages visible at the REPL.

## Decided (2026-11-05, the user: JCS155-JCS157)

JCS155: (b) SSLLogger as built for F3's night, then (a) System.Logger in jrt as its own fix; JCS156 (a): QUIC's TLS engine in, amending JCS131; JCS157 (a): java.net.IDN in.

## Decided (2026-11-06, the user: JCS158-JCS160)

JCS158 (a): the javac-API tests stay deferred at the compiled level (`:javac-api`); JCS159 (a): javac's ToolProvider on
javac's tool, built on `java-jc9c` ("As built: JCS159 (write-ahead)"); JCS160 (a): as built, every client socket in a
SocksSocketImpl, as jdk26u.

## Decided (2026-11-06, the user: JCS161)

JCS161 (a): java.lang.management alone, JMX cut by variants (`getObjectName` and the platform MBean server
unsupported), jdk26u's `sun.management` implementations where their Java stands, jrt's natives in Go (monitor
owners recorded for the deadlock tests): F13, on `java-jc9c` ("As built: F13 (write-ahead)").
## As built: F1 (write-ahead, 2026-11-05)

Branch `java-jc9-jca` (from `java-deferred-triage`), JC9's first fix as F1 above designed it, prepared on the light
side: the closure's lists, jrt's own Java, the variants, j2g's renames and the REPL's extendable classes, the
generator of the generated fields; and JCS128-JCS130's classes. Nothing heavy ran: no `bin/jrt-convert update`, no
build. **The branch is not mergeable before the night's conversion**: 16 of its variants name classes `jdk/forms`
does not hold yet (j2g records each as "a variant for a class not in the inputs", an analysis failure, not an
error), and the variants of classes it holds call what it does not (`SecretKeySpec`'s static initializer calls
`CryptoAccess`, an operation-level stub until converted: the class would not initialize, HMAC with it), so a build
before `bin/jrt-convert update` is wrong, though it completes (the night's steps 1-2 build javac's tool so, which
uses none of it); the night's commands below make it whole, then it merges.

**What came in, and how** (jdk26u at `baf63fb`):

| part | how |
|---|---|
| the closure | `test/jrt/jrt_sources.ae`'s `jca-sources`: 335 files of `src/java.base/share/classes`, 90,145 lines: javax.crypto (28: every engine and SPI, the cipher streams, NullCipher, EncryptedPrivateKeyInfo, SealedObject, the exceptions; `CryptoPermission` and `CryptoAllPermission`, which Cipher holds), `spec` (17), `interfaces` (4); `com/sun/crypto/provider` (93: all but the JCEKS key store's three, `JceKeyStore`, `KeyProtector`, `SealedObjectForKeyProtector`, PKI's with KeyStore, F2); java.security (24: the engines and SPIs, the key interfaces, `KeyPair`, `KeyRep`, `SignedObject`, `DEREncodable`, `PEM`, and `Permission`, `PermissionCollection`, `Guard`, CryptoPermission's supertypes, F4's start), `spec` (33), `interfaces` (19); `java/lang/SecurityException` (a cut class until now); `jdk/internal/access`'s two accesses; SUN's DSA, HSS/LMS and ML-DSA with the named keys' framework ML-KEM shares (`sun/security/provider`, 17); SunRsaSign (`sun/security/rsa`, 14); SunEC (`sun/security/ec` 17, `point` 6, `ed` 11); `sun/security/util`'s DER, curves and helpers (27), `util/math` (6), `util/math/intpoly` (4); `internal/spec` (5), `internal/interfaces` (1); `pkcs`' `PKCS8Key`, `NamedPKCS8Key`; `x509`'s `X509Key`, `NamedX509Key`, `AlgorithmId`, `AlgIdDSA` |
| the generated fields | `added-gensrc`'s 10 files of `sun/security/util/math/intpoly` (`IntegerPolynomialP256`, `P384`, `P521`, `25519`, `448`, the five order fields; 6,628 lines), made by `bin/jrt-convert`'s generate step as `make/modules/java.base/Gensrc.gmk`'s `INTPOLY_GEN_DONE` makes them: `build.tools.intpoly.FieldGen` on `header.txt`, on javac's tool executable, whose `buildtools` module gains `make/jdk/src/classes/build/tools/intpoly` (`bin/javac-vendor`'s `tool_dirs`) |
| the providers | `ProviderConfig`'s variant makes SunRsaSign and SunEC as jdk26u's switch does (java.security's order: SUN, SunRsaSign, SunEC, SunJCE); `SunJCE`'s variant removed (jdk26u's `putEntries` whole: 195 services); `SunEntries`' variant is jdk26u's constructor as repr/java converts it, cut after the digests (the signatures, key pair generators, parameter generator, parameters and key factories of DSA, HSS/LMS and ML-DSA added; the certificate factory, key stores, cert stores, login configuration and PKIX left out, F2 and F4) |
| the services' construction | jrt's `JceServices` (overlay) constructs every SunJCE service class but JCEKS's (184) and the HKDF classes with their `KDFParameters`; `RsaServices` (new, `sun/security/rsa`) SunRsaSign's 19; `JrtServices` SUN's 36 new ones (DSA's 20 signatures, `DSAKeyPairGenerator$Current` and `$Legacy`, HSS/LMS, ML-DSA's 12), the KDF engine's parameter, and asks `JceServices` then `RsaServices`. The three switches are generated from jdk26u's provider sources (the scratch scripts `gensvc.py`, `genjava.py`) and compiled by the JDK's javac against the platform (every constructor exists and is accessible). SunEC's own `Provider.Service` subclass constructs its services, as in the JDK |
| the crypto policy | jrt's `JceSecurityManager` (overlay, new): the unlimited policy's `CryptoAllPermission` and every caller of Cipher's protected constructor trusted (no provider jar, as jrt's `JceSecurity`; its `WALKER` keeps Cipher's call without a StackWalker); `JceSecurity` gains `getVerificationResult` (null) |
| SharedSecrets' accesses | jrt's `jdk.internal.jrt.CryptoAccess` (overlay, as JC6b's `ZipAccess`): `SecretKeySpec`'s and `EncodedKeySpec`'s static initializers (variants) set `JavaxCryptoSpecAccess` and `JavaSecuritySpecAccess` there, their eight readers' members (variants of `PBES2Core`, `HmacPKCS12PBECore`, `PBMAC1Core`, `HKDFKeyDerivation`, `ConstructKeys`, `KeyUtil`, `ECUtil`, `PKCS8Key`, cut verbatim from repr/java's conversion, the one call changed) read them there; `Signature`'s and `SealedObject`'s accesses (static initializers) cut: their readers (`SignatureUtil`, the JCEKS key store) are F2's |
| the Cleaner | `DESKey`, `DESedeKey`: the constructor registers no Cleaner; `PBEKey`, `PBKDF2KeyImpl`: the Cleanable field is the zeroing action itself, a `Runnable`, run by `destroy` and `clear` (and `PBEKey.readObject` cut). A key's bytes are zeroed when destroyed or cleared, not when unreachable, until F17 |
| direct buffers | `GaloisCounterMode`: the static `JavaNioAccess` field cut, so that the class initializes (its users are the direct-buffer branches, which a heap buffer never takes) |
| j2g | `arbace.j2g.names/renames`: seven providers' classes named as the API's interfaces get `Jce_` and `Sun_` names (`Jce_DHPrivateKey`, `Jce_DHPublicKey`, `Jce_PBEKey`, `Jce_EncryptedPrivateKeyInfo`, `Sun_DSAKeyPairGenerator`, `Sun_DSAPrivateKey`, `Sun_DSAPublicKey`; J2G-SPEC §4.4); `arbace.j2g.model/class-supers`: the 18 JCA classes jdk26u's security tests extend (`Provider` (55 tests), `Provider$Service`, `SecureRandom`, `SecureRandomSpi`, `MessageDigestSpi`, `Signature`, `SignatureSpi`, `KeyFactorySpi`, `KeyPairGeneratorSpi`, `AlgorithmParametersSpi`, `EncodedKeySpec`, `CipherSpi`, `MacSpi`, `Mac`, `KDFSpi`, `SecretKeyFactorySpi`, `CipherInputStream`, `CipherOutputStream`), so that the Java forms made at the REPL extend them (`:interp-protected`'s six Provider tests) |
| the oracle | the known mismatch of `Security.getProviders()` stays (four providers against the JVM's twelve), its reason now naming SunRsaSign and SunEC; Clojure's `:security-providers` matches both spellings |
| docs | LICENSE.md's Security item and `jdk/forms`' counts, J2G-SPEC §4.4's renames, JRT-SOURCES.md's "The JCA (F1, write-ahead)" |

**What was checked, light** (2026-11-05; scratch in the worktree's `.tmp/f1/`, kept in
`.tmp/keep/java-jc9-jca/` when the worktree goes):

- **The closure** (`closure.py`): the seeds' JDK classes named by imports, qualified names and the package's simple
  names, to a fixpoint over the packages in scope, against the world (`jdk/sources.txt` and jrt's hand-written
  classes, the cut classes not counted as world): 345 files. What they name outside the world and the list, 17
  classes, each accounted for: serialization's streams (`ObjectInputStream` 27 files, `ObjectOutputStream`,
  `ObjectInput`, `ObjectOutput`, `ObjectStreamClass`, `ObjectStreamField`: `readObject`, `writeObject`,
  `SealedObject`, `SignedObject`; J2G-SPEC §12), `CleanerFactory` and `Cleaner` (the variants), `MethodHandles` and
  `VarHandle` (byte-array views only: `GHASH`, `ChaCha20Cipher`, `GaloisCounterMode`, `IntegerPolynomial1305`,
  compiled by j2g, §8.5), `Certificate`, `X509Certificate`, `X509CRL` (Signature's and Cipher's `init(Certificate)`,
  DEREncodable's permits: F2), `DirectBuffer`, `JavaNioAccess` (GCM's variant), `UTF_32BE` (DerValue's
  UniversalString: F8). The converted forms' imports, which name every class a file uses (implicit ones too),
  checked against the world and the list the same way (`formsimports.py`): the same, and `JavaSecuritySpecAccess`,
  which the scan of the sources had missed (named only by a method reference's type), added.
- **repr/java's first contact**: the 345 files read by `bin/repr-java read-jdk` on javac's tool executable
  (`javac-8f24f568…`, 1.2 GB peak, 13 s for the 335, 6 s for the 10 generated): 345 converted, 0 failed; read without
  javac (`--no-javac`, the vendoring tools' default since JN9f): 345 native, 0 fallbacks, the forms equal to javac's
  read byte for byte.
- **javac**: the 335 sources, the 10 generated fields and the overlay's crypto and security files with `HostRandom`
  (354) compiled by the JDK's javac with `--patch-module java.base` (as `bin/jrt-convert`'s javac step compiles the
  closure, the rest from the platform): 0 errors, 695 classes (jdk26u's `Cipher` against jrt's `JceSecurityManager`
  among them).
- **FieldGen**: run on the JDK (`make/jdk/src/classes/build/tools/intpoly/FieldGen.java`), its 10 files equal to the
  JDK build's `support/gensrc/java.base/sun/security/util/math/intpoly` byte for byte.
- **The source list**: `jrt.jrt-sources/print-sources` on stage 0: 1,254 lines (907 now; the locales' index line is
  the generate step's): +335 `share`, +10 `gensrc`, +3 `overlay`.
- **The variants** read as forms (19 files, 2 forms each); the members they replace exist in the converted forms
  (the constructor `[Provider]`, `getProvider`, the static initializers, `NIO_ACCESS`, the methods by name and
  parameters).
- **Go names**: every class of the new forms (685 with the nested ones) against the gate executable's Go types and
  jrt's hand-written ones: 7 collisions among the new classes (the renames above), none with the world's, no nested
  class or static field making a derived name (`C_New`, `C_I` ...); no collision with javac's world's java.base files.
- **JCS128-JCS130** (below): `bin/reap --record`, 16 of 16 cells, 0 problems; `bin/java-check --judge` of the six
  levels' latest results in the gate's store against the new classes: 0 regressions, none in no class; the essential
  judgment: 428 of 428; `bin/repr-java closure-check` passed; `bin/gate --plan` (essential) plans 17 runs.

**JCS135, the closure's growth, measured**: +96,773 Java lines (335 files' 90,145 and the generated 6,628), **+20.0%**
on the closure's 483,461 (the 905 files of `jdk/sources.txt` but the locales); the forms +3,077,686 bytes (the
share files' 356 forms files 2,397,063, the generated 680,623), **+27.6%** on `jdk/forms`' 11,161,667; classes about
+685 (nested and anonymous ones included) on the world's 2,637, +26%. The executable (156.4 MB at JC6b, which added
1.6 MB) and the gate's build time are the night's to measure: by the forms' share, expect +12 to +20 MB (8-13%) and a
j2g run some 20-30% longer on the JDK's part; JCS135 (a)'s threshold, a minute more of build, is then checked.

**JCS128-JCS130, applied** (test/java/divergences.edn, test/reap/cells.edn, test/reap/matrix.edn):
- JCS128 (a): `:management` split into `:jmx` (`:design`, `javax\.management`, first) and `:mxbeans` (`:world`,
  `java\.lang\.management|com\.sun\.management`). `:jmx` holds no test today: every test whose failure names JMX also
  names it in its sources, so `:design-also` takes it first.
- JCS129 (a): `:design-also` (interp, 70 tests) and `:design-also-compiled` (139), `:kind :design`, by `:tests`, before
  the world's classes: the triage's static scan's tests (`causes.json`) that need serialization, the class-file API,
  XML, class loading, JDI, smartcardio, FFM or, since JCS128 (a) put it by design, JMX (34 interpreted and 37 compiled
  tests name JMX; without them the counts are the triage's 36 and 108). Three compiled tests stay in the classes that
  name them for their verdicts (`t05549`, `t05671` in `:speed-hang-compiled`, `t05565` in `:flaky`). The scan as
  Arbace code in `test/java/check.ae` (rerun when the corpus or the world changes), JCS129 (a)'s other half, is not
  written yet: until then the lists are the triage's of `d86cb874a`, stable under F1 (a by-design feature stays out).
- JCS130 (a): `:jvm-tools`, `:memory-limit`, `:environment` are `:kind :harness-design`, by design in the REAP cells'
  `:kind-map` (8 tests at each level). JCS99 (c)'s status-only digests need the reference (JCS137 below): not applied.
- The matrix: Java eval **1,848 / 332 / 1,142** (was 1,848 / 254 / 1,220), AOT **1,921 / 307 / 1,094** (was 1,921 /
  160 / 1,241): 78 and 147 tests from deferred to by design.

**The night's commands** (in the worktree, in this order, each under the heavy lock where it says; `ARBACE_HOST_EXE`
unset, the worktree's own stage 0 or main's linked; about 3-4 hours with the check):

```sh
cd /root/arbace/.tmp/worktrees/java-jc9-jca
# 1. javac's world gains FieldGen (buildtools, make/jdk/src/classes/build/tools/intpoly): read by the current tool,
#    the edge recorded on the frozen JVM. Expect jdk/javac/sources.txt +1 line ("buildtools make
#    build/tools/intpoly/FieldGen.java"), jdk/javac/forms +1 file, edge.edn alike or a few entries more.
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  bin/heavy-lock bin/javac-vendor convert
# 2. javac's tool with FieldGen (target/arbace-go/amd64/arbace-javac); j2g from the checkout (names.ae, model.ae
#    changed: loaded from the checkout by any host)
bin/heavy-lock bin/arbace-go --build-java
# 3. jdk/ converted afresh: expect "1254 files (before the locales): 1150 ... share, 6 unix, 41 generated, 54 of jrt's
#    own", the generate step's intpoly fields :same as the build's gensrc (41 generated equal), every file converted
#    (no javac: 0 fallbacks expected, as the light read), javac's classes, 0 failed; jdk/sources.txt 907 -> 1255 lines
REPR_JAVA_EXE=$PWD/target/arbace-go/amd64/arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 4. j2g's edge recorded afresh (the new classes' outside references: serialization's streams, Cleaner, certificates)
bin/heavy-lock bin/j2g-edge update
# 5. javac's world's edge less the new jdk/edge.edn (bin/javac-vendor verify, --full's, compares it)
bin/heavy-lock bin/javac-vendor edge
# 6. the executable (JCS135: its size against main's, `ls -l`, and the j2g and Go times of target/arbace-go/*.log)
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 7. a smoke of the JCA on it: [SUN SunRsaSign SunEC SunJCE], hello (AES/GCM's round trip), each signature true
#    (RSA, ECDSA, Ed25519, DSA, ML-DSA), 32 (PBKDF2's key bytes)
target/arbace-go/amd64/arbace -e '(import (quote (javax.crypto Cipher KeyGenerator SecretKeyFactory)) (quote (javax.crypto.spec GCMParameterSpec PBEKeySpec)) (quote (java.security KeyPairGenerator Signature Security)))
  (println (mapv #(.getName %) (Security/getProviders)))
  (let [k (.generateKey (doto (KeyGenerator/getInstance "AES") (.init 128))) iv (byte-array 12) c (Cipher/getInstance "AES/GCM/NoPadding")]
    (.init c Cipher/ENCRYPT_MODE k (GCMParameterSpec. 128 iv)) (let [ct (.doFinal c (.getBytes "hello"))]
      (.init c Cipher/DECRYPT_MODE k (GCMParameterSpec. 128 iv)) (println (String. (.doFinal c ct)))))
  (doseq [[kpa sa] [["RSA" "SHA256withRSA"] ["EC" "SHA256withECDSA"] ["Ed25519" "Ed25519"] ["DSA" "SHA256withDSA"] ["ML-DSA-44" "ML-DSA"]]]
    (let [kp (.generateKeyPair (KeyPairGenerator/getInstance kpa)) s (Signature/getInstance sa)]
      (.initSign s (.getPrivate kp)) (.update s (.getBytes "m")) (let [sig (.sign s)] (.initVerify s (.getPublic kp)) (.update s (.getBytes "m")) (println sa (.verify s sig)))))
  (println (count (.getEncoded (.generateSecret (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256") (PBEKeySpec. (.toCharArray "pw") (byte-array 8) 1000 256)))))'
# 8. Java's levels interp and compiled, the whole corpus (as JC6b), saved, not recorded: the new failures' classes
#    are the day's work (a test in no class fails the judgment); expect interp about 1,960-2,020 passing (1,848 now,
#    F1's 176 alone at most), compiled about 2,030-2,090 (1,921, 168), no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 --save .tmp/f1-night/check
```

Then, the day after (light): the saved results judged (`bin/java-check --judge .tmp/f1-night/check/results.tsv`),
the tail classified (JC6b's way: the cause classes take their tests, new narrower classes for what F1 surfaced), then
`bin/java-check --judge --record .tmp/f1-night/check/results.tsv`, `bin/reap --record`, the commit of `jdk/`,
`jdk/javac/` and the records, and the merge to `next` for the night's gate (its `--full` runs `bin/jrt-convert verify`,
`bin/javac-vendor verify` and the rest on the new tree).

**Risks and fallbacks**: (1) a variant j2g refuses (the forms are the converter's, but the variant mechanism's
matching of a cut method whose parameter is outside the world, `PBEKey.readObject`, is untried): drop that cut, the
method's body then references only the field of the same name; (2) the 18 extendable classes: a JCA class whose
DynSub j2g cannot make (a final method it must override, a constructor it cannot reach) fails the build: remove it
from `class-supers`, its tests stay `:interp-protected`; (3) `FieldGen` on the tool (its `Files`, `Path.of`,
`PrintWriter` over jrt's own `Files`): should it fail, the step's error names it, and the fields can be taken from
the build tree as CaseFolding's are (`:boot-jdk`), a decision for the user; (4) the interpreted KAT loops of
`com/sun/crypto/provider`'s tests past the 20 s limit: `:speed`'s measure (`bin/java-speed`) after the run.

**What is left of F1**: the night's run and the day's classification and records; JCS129 (a)'s scan as Arbace code;
JCS136 and JCS137 (Proposed).

## Decided (2026-11-05, the user)

JCS138 (b): System.Logger deferred to F3 (SSLLogger); JCS139 (b) for F5, no localized level-name bundle, (a) at the next rebuild of javac's tool.

## As built: F5 (write-ahead)

Branch `java-jc9-logging` (from `java-deferred-triage`, JCS128-JCS135 decided), 2026-11-05, prepared by day
under the light rules: the closure, the variants, jrt's own class, the data step, j2g's two tables, and light
checks only. Nothing converted, translated or built; the night's commands and their expected outcomes are below.
F1 (the JCA) is prepared beside it on `java-jc9-jca`: this branch touches no crypto class, and its hunks in
the shared files (`test/jrt/jrt_sources.ae`, `arbace/j2g/names.ae`, `arbace/j2g/model.ae`, `LICENSE.md`) are
apart from the lists F1 extends (`security-sources`, the end of `class-supers` and of the rename table).

**What comes in, and how** (jdk26u at `baf63fb`):

| class (or group) | how |
|---|---|
| java.logging's 17 classes: `ConsoleHandler`, `ErrorManager`, `FileHandler`, `Filter`, `Formatter`, `Handler`, `Level`, `LogManager`, `LogRecord`, `Logger`, `Logging`, `LoggingMXBean`, `MemoryHandler`, `SimpleFormatter`, `SocketHandler`, `StreamHandler`, `XMLFormatter` | jdk26u's, translated as java.base's (`test/jrt/jrt_sources.ae`'s new `logging-sources`, KIND `share:java.logging`, as java.sql's two files); 9,697 lines |
| `LogManager` | variant: the default configuration read as the resource `conf/logging.properties` when `java.util.logging.config.file` is unset; the manager's and the configuration class's names through the system loader alone (no context loader); the shutdown hook a `Thread(String)`; no `BootstrapLogger` redirection |
| `Logger` | variant: the caller-sensitive `getLogger(String)`, `getLogger(String, String)`, `getAnonymousLogger(String)` pass no caller (no `Reflection.getCallerClass`; the JDK's null-caller paths); `demandLogger` tests a caller's module only when there is one (`VM.isSystemDomainLoader`); `findResourceBundle` by `ResourceBundle.getBundle(name, locale)` (jrt's one registry); `RbAccess` cut |
| `Level` | variant: `computeLocalizedLevelName` by `ResourceBundle.getBundle(name, locale)`; `KnownLevel`'s custom levels kept in a map by loader (no `ClassLoaderValue`); `RbAccess` cut |
| `LogRecord` | variant: `inferCaller` over `new Throwable().getStackTrace()` with `CallerFinder`'s test and `SimpleConsoleLogger.Formatting.isFilteredFrame`'s names (no `StackWalker`, F20); `serialPersistentFields` cut (serialization by design) |
| `SimpleFormatter` | variant: the format by `SimpleConsoleLogger.Formatting.getSimpleFormat`'s rules, transcribed (no `jdk.internal.logger`) |
| `FileHandler` | variant (`LoggingFileHandler.ae`: `FileHandler.ae` is the file: URL handler's): the lock file held by an output stream, availability by the in-process lock table alone (no `FileChannel`/`FileLock`, F7); `%h` without the set-UID check |
| `sun.util.logging.internal.LoggingProviderImpl` | jrt's own (overlay): only `LogManagerAccess` and its holder, which `LogManager`'s static initializer registers with; no `LoggerFinder` (JCS138) |
| `conf/logging.properties` | resource data: `bin/jrt-convert`'s generate step copies `src/java.logging/share/conf/logging.properties` as the build's Copy.gmk installs it (identical to the build's `jdk/conf/logging.properties`); `jdk/data/conf/logging.properties` after `update` |
| j2g | rename `java/util/logging/Formatter` → `Logging_Formatter` (java.util.Formatter's Go name); `class-supers` gains the 10 logging classes the area's tests extend (11 of the 27 subclass `Handler`, `Logger`, `LogManager`, `Formatter`, ...), so interpreted subclasses work (JC4b's mechanism) |

Left out: `LoggingPermission` (extends `BasicPermission`: F4's), `HttpLogFormatter` (sun.net.www.protocol.http's),
`System.Logger` (JCS138), the localized level names' bundle (JCS139). Corrections to §5's F5 design: `LoggingMXBean`
and `Logging` are plain Java (no java.lang.management), so they come in and `LogManager.getLoggingMXBean` works
(`LoggingMXBeanTest2`); `MemoryHandler` has no management part; no table of handlers is needed: `LogManager`
names classes through `ClassLoader.getSystemClassLoader().loadClass`, which is jrt's registry (the world's and a
compiled program's classes; an interpreted test's classes are not in it, a second cause for configurations that
name a test's own handler). What stays outside the world in members no variant replaces becomes j2g's
operation-level stubs (J2G-SPEC §4.1), unreachable in the Go build: `LogRecord$CallerFinder` (StackWalker,
`SurrogateLogger`), `LogRecord.writeObject`/`readObject` (serialization), `LogManager$LoggingProviderAccess.demandLoggerFor`
and `Logger.setupResourceInfo(String, Module)`'s system-logger branch (`DefaultLoggerFinder.isSystem`; no finder
calls the first, no system logger has a module). `report.edn`'s missing parts should list these and no other.

**The closure's growth** (JCS135): `jdk/sources.txt` 907 → 925 lines (17 `share:java.logging`, 1 `overlay`);
9,697 Java lines on the closure's 483,461 (+2.0%); the converted forms about 145 KB on `jdk/forms`' 12 MB.
JC6b's 10k lines cost +1,634,304 bytes (+1.06%); expect about +1.5 MB on the gate build's 206,876,832-byte
executable (`d86cb874a`), to be measured.

**Light checks done** (stage 0 `ARBACE_HOST_EXE` main's, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 1 GB and 10 s):
- the closure: `jrt.jrt-sources/print-sources` on `doc/go/java-surface.edn`: 924 lines (with the generate step's
  `jrtgen` line, 925), the committed list plus exactly the 17 logging files and the overlay class;
- the read: `bin/repr-java read-jdk --check OUT java.base TREE` of the 17 files and the overlay class as a patch of
  java.base, as `bin/jrt-convert`'s convert step reads them: 18 converted, 0 failed, 0 javac errors, 0 unreadable;
  `--no-javac` (the vendoring default): 18 native, 0 fallbacks, the forms identical;
- the variants: `arbace.j2g.world/read-variants` and `apply-variant` on those forms (`.tmp/f5/vet.ae`), each then
  parsed by `arbace.classes.parse/parse-class`: every replaced, cut and added member found (LogManager 4/0/1,
  Logger 5/0/0, Level 1/0/0, LogRecord 1/1/1, SimpleFormatter 1/0/1, FileHandler 3/1/1; nested
  `Level$KnownLevel` 1/1/1, `Level$RbAccess` 0/1/0, `Logger$RbAccess` 0/1/0, `LogManager$Cleaner` 1/0/0), all parsed;
- `arbace/j2g/names.ae` and `model.ae` load (the rename found, 10 class-supers added); `bash -n bin/jrt-convert`;
  the build's `conf/logging.properties` equal to the source's.
Not checked by day: the analysis and translation (j2g on the whole world), javac's classes of the patch, the
edge, the build, the runs.

**The night's commands**, in order, heavy ones under `bin/heavy-lock` (expected outcomes after each):

1. `bin/jrt-convert update`: sources 925 lines ("18 more"); generate: `conf/logging.properties` `:same` against
   the build's `jdk/conf`; convert: `0 failed, 0 javac errors, ... 0 unreadable`, `no javac: ... 0 fallbacks`;
   check: 18 forms files added (`java/util/logging/*.ae`, `sun/util/logging/internal/LoggingProviderImpl.ae`),
   none different; `jdk/data/conf/logging.properties` new; the edge step's tables (javac's classes of the
   unvaried sources) gain the logging classes' outside references (`StackWalker$StackFrame`, `StackWalker$Option`, `ObjectInputStream`,
   `ObjectOutputStream` and their `GetField`/`PutField`, `ObjectStreamField`, `jdk.internal.logger.DefaultLoggerFinder`,
   `SurrogateLogger`, `BootstrapLogger`, `jdk.internal.loader.ClassLoaderValue`,
   `jdk.internal.access.JavaUtilResourceBundleAccess`, `jdk.internal.reflect.Reflection`, `FileChannel`'s members,
   `OverlappingFileLockException`, `VM.isSetUID`, `Thread`'s five-argument constructor).
2. `bin/j2g-edge update` (the frozen JVM): what j2g asks after the variants, a subset of the above (the
   `StackWalker` family, the object streams, `ObjectStreamField`, `DefaultLoggerFinder`, `SurrogateLogger`,
   `ClassLoaderValue`, `JavaUtilResourceBundleAccess`, which the unreplaced members name); `bin/j2g-edge verify` passes.
3. `bin/arbace-go --gate-build` (stage 0): j2g's report lists the replaced/cut/added counts above and only the
   missing parts named above; the executable's size against 206,876,832 (expect about +1.5 MB); `bin/jrt test`
   passes.
4. `bin/java-check --levels interp,compiled --jobs 8 --only 'jdk/java/util/logging|jdk/java/lang/management/PlatformLoggingMXBean|jdk/java/rmi/transport/runtimeThreadInheritanceLeak|jdk/com/sun/net/httpserver/(TaskRejectedTest|bugs/TruncatedRequestBody)|jdk/com/sun/security/sasl/digest/NoQuoteParams|jdk/java/lang/System/Logger'`:
   the area's 31 run tests (all `:load-failed`/`:refused` now); at most the 27 / 26 of §3 pass (26 of them in
   `jdk/java/util/logging`), about 19 / 18 expected (0.7). Expected to stay out: the 4 serialization tests
   (`HigherResolutionTimeStamps/LogRecordWithNanos*`, `SerializeLogRecord*`, by design, JCS129),
   `CheckLockLocationTest` (`UserPrincipal`, F9), the 6 with other causes (MXBeans, RMI, the HTTP server, SASL);
   at risk: `TestInferCaller` interpreted (an interpreted caller's frames are the evaluator's), `TestLogger` and
   `RootLoggerHandlers` interpreted (the test's own bundle and handlers are not in jrt's registry),
   `ParentLoggerWithHandlerGC` and `TestLoggerNames` (collection of weakly held loggers, F17).
5. `bin/java-check --judge` on the results; `test/java/divergences.edn`: `:logging` narrowed to `System\$Logger`
   (JCS138), the tail filed by cause; `--record` of the levels' files once the whole corpus is rerun (no test
   passing before may fail: `levels-known.edn`); `bin/reap --record`.
6. `bin/gate` (essential) and, with JC9's other parts, `bin/gate --full` (its `jrt-convert verify` byte for
   byte).

Docs left for the night, with the numbers: JRT-SOURCES.md's closure list, JRT-NOTES.md (a "java.util.logging"
section like JC6b's "java.io, zip and jar"), J2G-SPEC §4.1's closure paragraph and §4.4's rename, LICENSE.md's
`jdk/forms` count ("17 of `src/java.logging/share/classes`"; the variants' entry is written).

## Decided (2026-11-05, the user)

JCS138 (b): System.Logger deferred to F3 (SSLLogger); JCS139 (b) for F5, no localized level-name bundle, (a) at the next rebuild of javac's tool.

## As built: F16 (write-ahead)

Branch `java-jc9b` (from `java-jc9-pki`: F4 and F2 prepared, JCS128-JCS142 decided), 2026-11-05, F16 prepared by
day under the light rules as F1, F5, F4 and F2 were (JCS134 (a): JC9b = F16, F8, F7, F11, in that order): the
closure's list, jrt's own Java, the variants, j2g's tables, the data step, light checks only; nothing converted,
translated or built. **F16 needs nothing of JC9** (its closure was computed against the world plus F1's, F5's, F4's
and F2's lists, by reference: `small-members-sources` names none of their files) but sits after it in the file and
on the nights. The batch is two kinds of work (JCS96 (c)'s list): jdk26u classes the world lacks, prepared here, and
members of jrt's hand-written classes (Go: Thread, ThreadGroup, Runtime.Version, StringBuffer, ...), which no light
check can try: those are designed below (JCS143) and left to a day with a build, but for the system properties
(pure data, done).

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `small-members-sources`, one line in
`sources`):

| part | how |
|---|---|
| java.util (4) | `Timer` (variant: no Cleaner), `TimerTask`, `Scanner` (until now converted for javac's tool world alone, `overlay/jdk/javac/closure.txt`, with its variant: both move to the shipped closure, below; its `ReadableByteChannel` constructors, which name `CharsetDecoder` and `Channels`, are operation-level stubs until F8 and F7 bring those), `PropertyResourceBundle` (variant: the InputStream constructor decodes the file as UTF-8, else ISO-8859-1, through `jdk.internal.jrt.Utf8Coder` and a `StringReader`, not through `PropertyResourceBundleCharset`'s decoder; the whole file at once, where the JDK's falls back from the start of the reader's 8 KB buffer) |
| java.lang (2) | `Readable` (a cut class until now: Scanner reads its source through `read(CharBuffer)`; Reader and CharBuffer, translated, implement it), `TypeNotPresentException` |
| random (11) | `java.util.random.RandomGeneratorFactory` (jdk26u's constructs its algorithms from a static table and a switch: no ServiceLoader, no reflection; `isAnnotationPresent(Deprecated.class)` answers false in the Go build, as the JDK for every algorithm) and `jdk.internal.random`'s ten LXM, Xoroshiro and Xoshiro generators, over `RandomSupport` (in the world since JB1) |
| java.sql (5, KIND `share:java.sql`) | `Time` (beside `Date` and `Timestamp`, `added-module-sources`), `Blob`, `Clob`, `SQLException`, `SQLWarning` |
| javax.sql.rowset.serial (3, KIND `share:java.sql.rowset`) | `SerialBlob`, `SerialClob`, `SerialException`; their `readObject`/`writeObject` operation-level stubs (serialization, J2G-SPEC §12) |
| sun.net.www (2) | `MimeTable`, `MimeEntry`: `URLConnection.getFileNameMap`'s table, read from the image's `content-types.properties`; `overlay/jdk/variants/URLConnection.ae`, whose map knew no type, is removed (jdk26u's `getFileNameMap` again) |
| jrt's own (overlay, 5) | `java.util.concurrent.atomic.AtomicIntegerFieldUpdater`, `AtomicLongFieldUpdater`, `AtomicReferenceFieldUpdater`: jdk26u's, their implementations' Unsafe field offset replaced by the reflected `Field` and Unsafe's operations by `jdk.internal.jrt.FieldUpdaters`' (new: `Field.get`/`set` under a striped lock of the receiver and the field; the Go build has no field offsets, a class's fields being Go struct fields read by name), no caller class (`Reflection.getCallerClass` is not jrt's: a field that is not public is refused when its class is the platform's, accepted otherwise; JCS145); `java.sql.DriverManager`: the log writer alone (`getLogWriter`, `setLogWriter`, `println`, which SQLException and SQLWarning call), no driver registry |
| the variants (3) | `Timer.ae` (the ThreadReaper kept in the field and run by `cancel`, as the Cleanable's `clean` runs it: an unreachable timer never cancelled keeps its idle thread), `PropertyResourceBundle.ae`, `Scanner.ae` (moved from `overlay/jdk/javac/variants`: the File constructors through the stream, FileInputStream having no `getChannel` before F7) |
| javac's tool world | `Readable`, `Scanner` and `PropertyResourceBundle` leave `overlay/jdk/javac/closure.txt` (JN7's list of what the JDK build's tools need beyond the program): the program has them now; `bin/javac-vendor convert` drops their forms from `jdk/javac/forms` on the night |
| the data (`bin/jrt-convert`'s data step) | ICU's `uprops.icu` beside `ubidi.icu` (Bidi's supplementary characters load `UCharacterProperty.INSTANCE`: today's `ExceptionInInitializerError` of `BidiSurrogateTest` and `Bug7041232`, a NullPointerException in `ICUBinary.getRequiredData`), and `sun/net/www/content-types.properties` (src/java.base/unix/classes'), copied as the build copies resources |
| j2g | `class-supers` + `TimerTask` (the timer tests' tasks), `FutureTask` (protected `set`, `done`, `runAndReset`), `LocaleServiceProvider` (its protected constructor); no rename (the new classes' Go names collide with none) |
| Go (system.ae) | `java.runtime.version` and `java.vm.version` (the feature release, as `java.version`), `java.specification.name`, `java.specification.vendor`, `java.vm.specification.name`, `java.vm.specification.vendor` (the JDK's values): seven lines of data in `initProps` |

**Left to the Go day (JCS143)**, with their tests: Thread's `activeCount` (`PipedInputStream/WriterLoop`),
`getThreadGroup` and a ThreadGroup (`ITLConstructor`, `ref/NullQueue`, `ThreadPoolExecutor/ThrowingTasks`), the
constructors with a ThreadGroup and a stack size (`TestThreadStackSizes`), `ofPlatform` and its builder
(`NotifiedThenTimedOutWait` twice), `getAllStackTraces` (`GenerifyStackTraces`), `getContextClassLoader`
(`Executors/PrivilegedCallables`); `Runtime.Version`'s members (`RuntimeTests/Version/Basic`);
`String.transform` (`String/Transform`), `contentEquals` of an empty buffer (`StringContentEqualsBug`),
StringBuffer's `capacity` and an append's bounds (`StringBuffer/Capacity`, `AppendCharSequence`,
`TestSynchronization`); Math.pow's last place (`PowLargeExpTests`: StrictMath's fdlibm `pow`, jrt's `fdlibm.ae`
has it for StrictMath); Semaphore given a class interface for `reducePermits` (`Semaphore/PermitOverflow`: jrt's
hand-written leaf, as JC4c did for WeakReference); ForkJoinPool's `schedule` and `getStealCount`
(`DelayScheduler/AscendingOrderAfterReplace`, `forkjoin/NQueensCS`); `FileInputStream.skip(-n)`
(`NegativeAvailable`: jrt's overlay FileInputStream, F7's file; a one-line Java change there). Not F16's after all:
`Executors/Throws` (`Executors.callable(PrivilegedAction)` is F4's: PrivilegedAction comes in with
permission-sources), `ScopedValue/UnboundValueAfterOOME` (ScopedValue needs Thread's bindings, Go, and the test an
OutOfMemoryError from `new int[Integer.MAX_VALUE][]`, which the Go runtime does not raise as one), the three
`compiler/c2/Test7179138` and `System/Versions` (they print `java.vm.version` and the other version properties,
compared with the reference JDK's own, `26.0.2.1-internal-adhoc...`: JCS144), `ClassLoader/deadlock/GetResource`
(prints the properties), `Collator/Bug4848897` (Norwegian's collation: the locale selection, `JRT_LOCALES`),
`AbstractQueuedLongSynchronizer$ConditionObject`'s constructor from an interpreted subclass (`FlakyMutex`, F21's),
`System.inheritedChannel` (F10).

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 0.7 GB; scratch in the worktree's `.tmp/jc9b/F16/`):
- the closure (`.tmp/jc9b/closure.py`, F4's with every module's share and unix classes indexed, the world
  extended by JC9's lists and overlays, scope `java.sql` and `javax.sql.rowset.serial` less JDBC's Connection,
  DriverManager, Statement, PreparedStatement and ResultSet): 27 files, 11,261 lines (19 `share`, 5
  `share:java.sql`, 3 `share:java.sql.rowset`), and the 5 overlays, 1,804 lines; what it names outside the world and
  the list: `ObjectInputStream`/`ObjectOutputStream` (the serial classes' serialization), `Cleaner` and
  `CleanerFactory` (Timer's variant), `PropertyResourceBundleCharset` (the variant), `CharsetDecoder`, `Channels`,
  `ReadableByteChannel` (Scanner's channel constructors);
- `jrt.jrt-sources/print-sources`: the list before plus exactly those 32 lines (27 sources, 5 overlays);
- the read: `bin/repr-java read-jdk --check` of the 32 files (with `java/sql/Date.java`, the world's, for
  `Time`'s references) as a patch of java.base: 33 converted, 0 failed, 0 javac errors, 0 unreadable (3.2 s, 0.6
  GB); `--no-javac`: 33 native, 0 fallbacks, the forms identical;
- the variants applied and parsed (`.tmp/jc9b/vet.ae`): Timer 3/0/0 (replaced/cut/added), PropertyResourceBundle
  1/0/1, Scanner 2/0/0, all parsed;
- javac: the java.base part (23 files and the overlays) with `--patch-module java.base`, the java.sql part with
  `--patch-module java.sql`, the serial classes with `--patch-module java.sql.rowset`, by the JDK's javac: 0 errors,
  45 classes;
- Go names: the 44 classes of the new forms (nested ones `Outer_Inner`) against the gate executable's 2,855 Go
  types (translated and hand-written) and JC9's new classes: no collision (`java.sql.Date`, read for context, is
  `Sql_Date` already).

**JCS135, the closure's growth**: +13,065 Java lines (**+2.7%** on the closure's 483,461), the forms +201,423 bytes
(**+1.8%** on `jdk/forms`' 11,161,667), about +1.6 MB on the executable by JC6b's measure, and 170 KB of data
(`uprops.icu`, `content-types.properties`).

**Expected yield** (the triage's counts, the scan of `d86cb874a`): F16's causes alone, 63 interpreted / 54
compiled at most; this branch's Java part serves 31 of those tests, **29 / 27** at most (Scanner 2, Timer 6,
RandomGeneratorFactory 6, SerialBlob and SerialClob 3, Bidi 2, the MIME table 2, PropertyResourceBundle 1, FutureTask
3 interpreted, LocaleServiceProvider 1 interpreted, the field updaters 2, java.sql.Time and TypeNotPresentException 1
compiled each, `Test6981737` by the properties), about **20 / 19** expected (0.7). At risk: `AtomicUpdaters` (it asks
`getDeclaredField` of AtomicInteger's private `value`, which jrt's member tables may not hold for a translated
class), the RandomGenerator tests' speed interpreted (`RandomTestChiSquared`, `RandomTestMoments`: `:speed`'s
measure), `GetLastModified` (a jar entry's time), Timer's `KillThread` (an uncaught exception in the timer thread).
The Go day (JCS143) serves about 20 more.

**The night's commands**, in the worktree after JC9's nights (`next` holds F1, F5, F4 and F2: merge it first),
heavy ones under `bin/heavy-lock`; F16 alone about 2 hours with the check, or one conversion for JC9b's four (F16,
F8, F7, F11 together, below their sections):

```sh
cd /root/arbace/.tmp/worktrees/java-jc9b
git merge next      # JC9 (its nights' conversions, jdk/ and jdk/javac/ committed)
# 1. jdk/ converted afresh: expect sources.txt +32 lines for F16 (19 share, 5 share:java.sql, 3
#    share:java.sql.rowset, 5 overlay), 0 failed, 0 javac errors, no javac: 0 fallbacks; jdk/forms +34 files
#    (with the packages javax.sql.rowset.serial and jdk.internal.random); jdk/data + uprops.icu (170,864 bytes)
#    and sun/net/www/content-types.properties
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 1b. javac's world without Readable, Scanner and PropertyResourceBundle (closure.txt): expect
#    jdk/javac/sources.txt -3 lines, jdk/javac/forms -3 files (the program's now), then javac's tool again
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  bin/heavy-lock bin/javac-vendor convert && bin/heavy-lock bin/arbace-go --build-java
# 2. j2g's edge afresh (Cleaner, CleanerFactory, PropertyResourceBundleCharset, CharsetDecoder, Channels and the
#    object streams as outside references)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable (JCS135: its size against JC9's last night, `ls -l`); j2g's report: "variant of
#    java/util/Timer: 3 replaced, 0 cut, 0 added", PropertyResourceBundle 1/0/1; URLConnection's line gone
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F16: true, then "a b" and 3 (Scanner), a long (L64X128MixRandom), true
#    (Bidi with a supplementary character), "text/html", "26 Oracle Corporation"
target/arbace-go/amd64/arbace -e '(import (quote (java.util Timer TimerTask Scanner)) (quote (java.util.concurrent CountDownLatch TimeUnit)) (quote (java.util.random RandomGeneratorFactory)) (quote (java.text Bidi)) (quote (java.net URLConnection)))
  (let [l (CountDownLatch. 1) t (Timer. true)] (.schedule t (proxy [TimerTask] [] (run [] (.countDown l))) 10) (println (.await l 5 TimeUnit/SECONDS)) (.cancel t))
  (let [s (Scanner. "a b 3")] (println (.next s) (.next s)) (println (.nextInt s)))
  (println (.nextLong (.create (RandomGeneratorFactory/of "L64X128MixRandom") 42)))
  (println (.isMixed (Bidi. "a𐤀b" 0)))
  (println (URLConnection/guessContentTypeFromName "x.html"))
  (println (System/getProperty "java.vm.version") (System/getProperty "java.vm.specification.vendor")))'
# 5. Java's levels interp and compiled on F16's tests (then the whole corpus with JC9b's others, as JC6b): saved,
#    not recorded; expect about 20 / 19 of the 31 to pass, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/util/(Timer|Scanner|Random|Collections/Shuffle|ResourceBundle/Bug6204853|concurrent/(FutureTask|atomic|Executors)|PluggableLocale/SupportedLocalesTest)|jdk/java/text/Bidi|jdk/java/net/URLConnection/GetContentType|jdk/java/net/URLConnection/GetLastModified|jdk/java/sql|jdk/javax/sql|jdk/java/lang/(RuntimeTests/exec/SleepyCat|reflect/Generics/exceptionCauseTest)|hotspot/jtreg/(compiler/c2/cr6890943|runtime/6981737)' \
  --save .tmp/f16-night/check
```

(The field updaters are judged by the check's `AtomicUpdaters` and `Lazy`, whose classes declare the volatile
fields: no class of the world offers one to the smoke.)

Then, the day after (light), as F4's: the saved results judged, the tail classified (`:threads` and
`:util-classes` narrowed to the Go members above, `:system-properties` to the four that print the reference's
version, JCS144), the records with the whole corpus's rerun, `bin/reap --record`, the docs with the numbers
(JRT-SOURCES.md's closure list, JRT-NOTES.md's "Concurrency" for the field updaters and "Text and locales" for
uprops.icu, J2G-SPEC §12's Cleaner line for Timer).

**Risks and fallbacks**: (1) `Readable` made whole: a hand-written jrt class implementing it would need its
`read(CharBuffer)` (none does: Reader and CharBuffer are translated); should j2g find one, Readable goes back to
the cut list and Scanner's reads go through a variant over `Reader`; (2) the field updaters over `Field.get`/`set`
for the REPL's interpreted classes (their fields are the interpreter's, which jrt's Field reads by name: untried
for volatile fields of a class made at the REPL); (3) `FieldUpdaters`' lock stripes: a program that holds an
object's updater lock while waiting on another stripe's could deadlock only if it nested two updaters' operations,
which the API does not do (each operation takes one lock and returns); (4) jdk26u's `RandomGeneratorFactory`
names `SecureRandom` (the world's since the security basics) and `ThreadLocalRandom` (jrt's): plain references.

**What is left of F16**: the night's run and the day's classification and records; the Go members (JCS143);
JCS144, JCS145 (Proposed).

## As built: F8 (write-ahead)

Branch `java-jc9b`, 2026-11-05, prepared by day under the light rules as F4 and F2 were: the closure's lists,
jrt's own Java, the variants, j2g's tables, the generate step, and light checks only; nothing converted,
translated or built. **F8 replaces jrt's hand-written charsets** (J2G-SPEC §12's V9/R18: UTF-8, ISO-8859-1,
US-ASCII and since phase 2B UTF-16, -BE, -LE, go/arbace/jrt/charset.ae and codec.ae, with jrt's own
InputStreamReader and OutputStreamWriter, overlay/jdk) **by jdk26u's java.nio.charset and sun.nio.cs**, as
"Text and locales" replaced the hand-written Locale: the translated classes in the program, the hand-written
ones kept as stand-ins for jrt's own build. Its closure was computed against the world plus JC9's lists (F1,
F5, F4, F2, by reference) and needs none of them: F8 can land before or after JC9. **Not mergeable before its
night** (as F1's): its variants name classes jdk/forms does not hold yet, and the overlay readers it removes
are jdk/forms' until the conversion.

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `charset-sources`, 42 files of
src/java.base/share/classes, and `charset-gensrc`, 51 generated, one line each in `sources` and print-sources):

| part | how |
|---|---|
| java.nio.charset (6 + 1) | the whole package: `Charset` (variant), `CoderResult`, `CodingErrorAction`, `CoderMalfunctionError`, `StandardCharsets`, `spi/CharsetProvider` (the exceptions were in the world); `CharsetDecoder` and `CharsetEncoder` generated (below). `Charset.ae`: no providers by ServiceLoader (`providers` empty, `lookupViaProviders` null, its ScopedValue guard `IN_LOOKUP` cut), no extended providers until jdk.charsets is in (JCS146), the constructor copying the aliases always (`VM.isSystemDomainLoader` is not in the Go build; the copy of a system charset's array is not observable) |
| sun.nio.cs, the Unicode family and the machinery (31) | UTF-8, UTF-16 (BE, LE, LE with BOM), UTF-32 (BE, LE, with BOM), CESU-8, ISO-8859-1, US-ASCII; `Unicode`, `UnicodeDecoder`/`Encoder`, `UTF_32Coder`, `SingleByte`, `DoubleByte`, `CharsetMapping`, `HKSCS`, `GB18030` (variant `GB18030.ae`: `IS_2000` without `VM.initLevel`), `Surrogate`, `ArrayDecoder`/`Encoder`, `DelegatableDecoder`, `HistoricallyNamedCharset`, `ThreadLocalCoders`, `StringUTF16` (variant `CsStringUTF16.ae`: `getChar` reads jrt's little-endian UTF-16 bytes without Unsafe), `StreamDecoder`, `StreamEncoder` |
| the streams (2) | jdk26u's `InputStreamReader` and `OutputStreamWriter` over StreamDecoder and StreamEncoder: every charset, the CharsetDecoder/Encoder constructors, `getEncoding`'s historical names; jrt's overlays of them (overlay/jdk/java.base/java/io, three charsets, "no stream coding for UTF-16") removed (JCS147) |
| the generated (51, KIND gensrc) | `CharsetDecoder`, `CharsetEncoder` from `Charset-X-Coder.java.template` by `build.tools.spp.Spp` (GensrcCharsetCoder.gmk's keys and replacements); sun.nio.cs's `StandardCharsets` (the provider: aliases, classes and cache as pre-hashed maps) and its 48 table-driven charsets for linux (the ISO-8859 family, KOI8-R/U, the Windows and IBM PC code pages, TIS-620, and the CJK ones the JDK keeps in java.base on Linux: EUC-JP/-KR/-CN/-TW, GBK, Big5, Big5-HKSCS, Johab, SJIS, MS932, JIS X 0201/0208/0212) by `build.tools.charsetmapping.Main` (GensrcCharsetMapping.gmk: make/data/charsetmapping's `charsets` and `stdcs-linux`); both in `bin/jrt-convert`'s generate step on javac's tool executable, which gains the tool (`bin/javac-vendor`'s `tool_dirs` + `charsetmapping`, `gen_tools` + `build.tools.charsetmapping.Main`; JCS148) |
| what they name (3) | `sun.util.PreHashedMap` (StandardCharsets' maps), `java.nio.channels.Channel` and `ReadableByteChannel` (StreamDecoder's channel reader, `Channels.newReader`, F7's) |
| jrt's own (overlay, 2) | `sun.nio.cs.JrtCharsets`: the 60 standard charsets constructed by class name (the generated provider's `Class.forName(...).newInstance()` replaced by variant `CsStandardCharsets.ae`; no class loading by name, as jrt's providers' services); `jdk.internal.jrt.CharsetStrings`: String's Charset overloads for the charsets jrt's String does not code itself, as jdk26u's `String.decode`, `encodeWithEncoder` and `lookupCharset` code them (the ArrayDecoder/ArrayEncoder paths, else the decoder or encoder over heap buffers, malformed and unmappable input replaced; the String's coder Latin1 when every char is at most U+00FF, as COMPACT_STRINGS chooses) |
| j2g | renames `sun/nio/cs/StandardCharsets` → `Cs_StandardCharsets` (java.nio.charset's keeps the Go names `StandardCharsets_UTF_8` ... jrt's code uses), `sun/nio/cs/StringUTF16` → `Cs_StringUTF16` (java.lang.StringUTF16's hand-written functions hold the prefix); `class-supers` + `Charset`, `CharsetDecoder`, `CharsetEncoder` (the tests' own charsets and coders: `CharsetDecoder/AverageMax`, `NaNinCtor`, `CharsetEncoder/NaNinCtor`) |

**jrt's hand-written Go** (written 2026-11-05 on `java-jc9b-natives`, from `java-jc9b`, checked by `bin/jrt build`
(amd64 and arm64: `go build`, `go vet`) and `bin/jrt test` (amd64), jrt's own build, no j2g run: see "As built: the
Go natives of F8 and F11" below for what was written and how it departs from this design):
- `charset.ae` becomes `standin_charset.ae` (Charset, Unicode, the six charsets, StandardCharsets, the lookup,
  `Charset.forName`, `defaultCharset` ...: replaced as a whole in a j2g program, as `standin_locale.ae` is), and
  `test/jrt/manifest.ae` drops their nine entries (regenerated `manifest.edn`), as Locale's were dropped; the
  reflection tables' Charset entries (`reflect_tables.ae`, generated) go with them;
- String's Charset overloads stay hand-written in a new `charset.ae` (`String_New_B1_Charset`,
  `String_New_B1_I_I_Charset`, `GetBytes_Charset__B1`, the name overloads through codec.ae): the JDK's fast paths
  by identity with `UTF_8_INSTANCE`, `ISO_8859_1_INSTANCE`, `US_ASCII_INSTANCE` (phase 1's coding, as now), any
  other charset, the UTF-16 family included, through the translated `CharsetStrings_Decode_B1_I_I_Charset__String`,
  `CharsetStrings_Encode_String_Charset__B1` and `CharsetStrings_Lookup_String__Charset` (the stand-in file declares
  `jdk.internal.jrt.CharsetStrings` and those three functions over phase 1's coding, so jrt's own build links and a
  j2g program drops them for the translated class);
- `codec.ae`'s `charsetOf` keeps jrt's six names in a table of its own (it read the hand-written charsets' `kind`);
- `access.ae`: JavaLangAccess gains `decodeASCII` and `uncheckedEncodeASCII` (the coders' ASCII fast paths; the
  forms in the scratch `go-access.ae.txt`).

Left out, with their tests: **jdk.charsets** (sun.nio.cs.ext: 124 files, 85,881 lines, +17.8%; the ISO-2022
family, the EBCDIC and other IBM pages, MS950, x-eucJP-Open, x-windows-5022x, COMPOUND_TEXT): JCS146;
`TestMS950`, `Test6392804`, `TestJIS0212Decoder` wait for it. Direct buffers and channels (F7):
`ByteBuffer.allocateDirect` names DirectByteBuffer, outside the world, and `Channels` is F7's, so
`EUCJPUnderflowDecodeTest`, `EucJpLinux0212`, `FindOneCharEncoderBugs`, `TestUTF8`, `TestUTF8BOM`,
`DirectBufferAllocTest` and `BashStreams` come with F7, not F8 alone. Not charsets at all though filed under
`:charsets-locales`: `String/ToLowerCase` and `ToUpperCase` (Lithuanian casing, jrt's String) and
`MultipleNumberScriptTest` (locale data). Members no variant replaces become operation-level stubs:
`StreamDecoder`'s `instanceof FileChannel` (a cut class: false) until F7.

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 2 GiB; scratch in the worktree's `.tmp/jc9b/F8/`):
- the closure (`.tmp/jc9b/closure.py` over java.nio.charset, its spi, sun.nio.cs and the two readers, against
  the world and JC9's lists): 82 files beyond the world, and the 11 it replaces (Charset, StandardCharsets, the
  six hand-written charsets' classes with Unicode, the two readers), 93 files, 59,455 lines (42 `share`, 51
  `gensrc`); outside the world and the list: ServiceLoader and ScopedValue (Charset's variant), FileChannel (cut,
  F7);
- `jrt.jrt-sources/print-sources` with `charset-sources` and `charset-gensrc` (a scratch copy of
  jrt_sources.ae): the committed list plus exactly those 93 lines, less the two overlay readers' lines;
- the read of the 93 files and the 2 overlays: 95 converted, 0 failed, 0 javac errors, 0 unreadable (8 s,
  0.8 GB); `--no-javac`: 95 native, 0 fallbacks, the forms identical;
- the generators: jdk26u's `build.tools.charsetmapping.Main` and `build.tools.spp.Spp`, compiled and run on the
  JDK with the build's arguments: the 49 sun/nio/cs files and CharsetDecoder, CharsetEncoder equal to the
  build's `support/gensrc/java.base` byte for byte;
- the variants applied and parsed (`.tmp/jc9b/vet.ae` on the converted forms): Charset 4/1/0
  (replaced/cut/added), StandardCharsets 1/0/0, StringUTF16 1/0/0, GB18030 1/0/0, all parsed;
- javac: the 93 files and the 2 overlays compiled by the JDK's javac with `--patch-module java.base`: 0 errors,
  210 classes;
- Go names: the 193 classes of the new forms against the gate executable's Go types, jrt's hand-written ones and
  JC9's: the collisions are the renames above and the replaced classes.

**JCS135, the closure's growth**: +59,155 Java lines (**+12.2%** on 483,461: the 93 files and jrt's 2 new
overlays, less the 518 lines of the 2 removed), the forms +1,478,904 bytes (**+13.2%** on 11,161,667), of which
0.98 MB are eight files of table literals (GB18030, EUC_TWMapping, GBK, Johab, HKSCSMapping, Big5, MS932,
EUC_KR) that j2g makes Go string constants: the executable by the forms' measure some +11 MB, by the code's
nearer +5 MB; measured on the night.

**Expected yield** (the triage's counts): 58 interpreted / 56 compiled tests at most alone, of which 14 / 13 stay
out by the cuts above (7 with F7, 3 with jdk.charsets, 3 not charsets, `UriImportExport` unexplained), about
**31 / 30 expected** (0.7 of 44 / 43); likely more, the rest being coders' tests of the classes F8 brings whole.
At risk: the readers' speed through the coders (JCS147), the generated tables' first contact with j2g (string
literals of 300 KB).

**The night's commands**, in the worktree, heavy ones under `bin/heavy-lock`; about 3 hours with the check:

```sh
cd /root/arbace/.tmp/worktrees/java-jc9b
# 0. the Go (above) is written and committed on java-jc9b-natives (merge it first): standin_charset.ae, charset.ae's
#    String overloads, codec.ae, access.ae, the manifest; bin/jrt build && bin/jrt test pass (rerun them after the merge)
# 1. javac's world gains the JDK build's charsetmapping tool: expect jdk/javac/sources.txt +11 lines
#    ("buildtools make build/tools/charsetmapping/{Charset,DBCS,EUC_TW,HKSCS,Hasher,JIS0213,Main,SBCS,SPI,SRC,Utils}.java"),
#    jdk/javac/forms +11 files
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  bin/heavy-lock bin/javac-vendor convert
# 2. javac's tool with it
bin/heavy-lock bin/arbace-go --build-java
# 3. jdk/ converted afresh: expect the sources line +42 share, +51 generated, overlay -2 +2; the generate step's
#    CharsetDecoder, CharsetEncoder and 49 sun/nio/cs files :same as the build's gensrc; 0 failed, 0 javac errors,
#    no javac: 0 fallbacks; jdk/forms +93 +2 -2 files
REPR_JAVA_EXE=$PWD/target/arbace-go/amd64/arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 3b. jdk26u's InputStreamReader has the CharsetDecoder constructor javac's world added by a variant
git rm overlay/jdk/javac/variants/InputStreamReader.ae
# 4. j2g's edge afresh (the new classes' outside references), 5. javac's world's edge
bin/heavy-lock bin/j2g-edge update
bin/heavy-lock bin/javac-vendor edge
# 6. the executable (JCS135: its size; the j2g and Go times); j2g's report: variants Charset 4 replaced 1 cut,
#    StandardCharsets 1, StringUTF16 1, GB18030 1; "replaced-standins" the nine charset classes
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 7. a smoke: 60 true UTF-16 [104 0] héllo [-126 -96] Шевченко (the JDK's line but the count, 173 with jdk.charsets)
target/arbace-go/amd64/arbace -e '(println (count (java.nio.charset.Charset/availableCharsets))
  (.canEncode (.newEncoder (java.nio.charset.Charset/forName "EUC-JP")) あ)
  (.getEncoding (java.io.InputStreamReader. (java.io.ByteArrayInputStream. (byte-array 0)) "UTF-16"))
  (vec (.getBytes "h" "UTF-16LE")) (String. (.getBytes "héllo" "EUC-JP") "EUC-JP")
  (vec (.getBytes "あ" "Shift_JIS")) (String. (.getBytes "Шевченко" "KOI8-U") "KOI8-U"))'
# 8. Java's levels on the charsets' areas: saved, not recorded; expect about 31 / 30 of F8's tests to pass, no
#    passing test lost (the readers and writers are under every test: the whole corpus on JC9b's night)
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(sun/nio/cs|java/nio/charset|java/io/(OutputStreamWriter|InputStreamReader|Unicode|charStreams|PrintStream|Reader|Writer)|java/lang/String/Encodings|java/util/(Formatter|jar/TestExtra)|java/net/URLConnection/GetXmlContentType)' \
  --save .tmp/f8-night/check
```

(Step 7's line checked on the JDK, `.tmp/jc9b/F8/Smoke.java`: 173 charsets there, 60 of them java.base's
sun.nio.cs.) Then, the day after: the results
judged, the tail classified (`:charsets-locales` narrowed to `:charsets-ext` and the Lithuanian casing),
`--record` with the whole corpus, `bin/reap --record`, the docs (JRT-NOTES.md "Charsets, locales, dates",
J2G-SPEC §12's V9/R18 and §4.1's renames, JRT-SOURCES.md's list), the commit of `jdk/` and `jdk/javac/`.

**Risks and fallbacks**: (1) every PrintStream and Writer now encodes through StreamEncoder and CharsetEncoder:
should the suite or the essential gate slow measurably, JCS147 (b) (jrt's readers kept for the three
charsets, delegating the rest) is the day's fallback, the removed overlays in git; (2) the charsetmapping tool
on javac's tool (Scanner over a File, through the tool world's Scanner variant; Formatter): should it fail, the
step's error names it, and the 49 files can be copied from the build tree as CaseFolding's are, a decision for
the user (JCS148 (b)); (3) the generated tables' string literals (EUC_TWMapping's 600 KB of Java) through j2g
and Go's compiler: their time and size are the night's measure; (4) javac's tool world holds the shipped world's classes, so after F8 javac on Arbace decodes its sources through
jdk26u's coders: `bin/javac-vendor classes` on the night confirms its class files unchanged; (5) j2g's stand-in replacement for a class jrt's own Go
code names by type (`Charset_I` in reflect_tables and shims_test): the day's Go removes those uses.

**What is left of F8**: the day's Go (above), the night's run and the day's classification and records;
jdk.charsets (JCS146).

## As built: F7 (write-ahead)

Branch `java-jc9b` (from `java-jc9-pki`), 2026-11-05, F7 prepared by day under the light rules as F4 and F2
were: the closure's list, jrt's own Java, the variants, j2g's table, the generate step, light checks only;
nothing converted, translated or built. **F7 needs neither the JCA nor PKI**: its closure was computed against
the world plus JC9's lists and overlays (F1, F5, F4, F2, by reference); of JC9b's, Channels' readers and writers
are F8's (`sun.nio.cs.StreamDecoder`, `StreamEncoder`, the coders) and `Scanner(ReadableByteChannel)` F16's
use of F7's interfaces: F7 lands after F8 (its `Channels.newReader`/`newWriter` operation-level stubs until
then) and keeps the channel interfaces should F16 take them first (the earlier fix keeps a file).

**The shape.** jdk26u's channel API is plain Java over `sun.nio.ch.FileChannelImpl`, whose own machinery is
native (`FileDispatcherImpl`, `NativeThreadSet`, `IOUtil`'s temporary direct buffers, `mmap`, `FileKey`'s inode
for the lock table). F7 takes the API from jdk26u and writes the implementation for jrt, as JC6b wrote
RandomAccessFile's natives: a `FileChannelImpl` of jrt's own over jrt's file table (`HostFiles`), with
jdk26u's checks, exceptions and results; the mapped and direct buffers as heap buffers. No new native: the Go
side is unchanged (JCS149).

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `channel-sources`, one line in
`sources`; `added-gensrc` grown; the overlays by `overlay-sources`):

| part | how |
|---|---|
| java.nio.channels (19) | jdk26u's: `Channel`, `ReadableByteChannel`, `WritableByteChannel`, `ByteChannel`, `ScatteringByteChannel`, `GatheringByteChannel`, `SeekableByteChannel`, `InterruptibleChannel`, `FileLock`, `Channels` (the stream adapters; its readers and writers F8's coders), the exceptions (`ClosedChannelException`, `AsynchronousCloseException`, `ClosedByInterruptException`, `FileLockInterruptionException`, `NonReadable`/`NonWritableChannelException`, `OverlappingFileLockException`, `IllegalBlockingModeException`), `spi/AbstractInterruptibleChannel` (variant) |
| sun.nio.ch (5) | jdk26u's `ChannelInputStream`, `ChannelOutputStream`, `Streams` (j2g rename `Ch_Streams`), `FileLockImpl`, `Interruptible` |
| `java.nio.BufferMismatch` | jdk26u's (ByteBuffer's and CharBuffer's `mismatch`/`compareTo`, `:nio`'s two tests) |
| the buffers (35, KIND `gensrc`) | the generate step's `genbuffer` (bin/jrt-convert, GensrcBuffer.gmk's `SetupGenBuffer`) gains READ_ONLY (`R`: keys `ro`, `RW=R`) and generates `Long`/`Short`/`Float`/`DoubleBuffer` with their heap buffers and `ByteBufferAs*Buffer{B,L}` views, and every type's read-only heap buffer and views (`Heap*BufferR`, `ByteBufferAs*BufferR{B,L}`): jdk26u's whole heap family, the direct ones left to jrt's own `DirectByteBuffer` |
| `FileChannelImpl` (overlay, jrt's own) | the channel of the three streams (jdk26u's `open(FileDescriptor, String, boolean, boolean, boolean, boolean, Closeable)`, which jdk26u's `RandomAccessFile.getChannel` calls unchanged) and of `FileChannel.open` (`open(Path, Set)`: UnixChannelFactory's option checks): reads and writes at the descriptor's position (shared with the stream, as the JDK's), scattering and gathering, positional reads and writes that leave the position alone, `size`, `truncate` (the position moved back when beyond), `force` (nothing to flush: jrt's host writes through), transfers through an 8 KiB buffer, `available` and `isOther` for `ChannelInputStream`, an appending stream's position its size; locks (`lock`, `tryLock`, `release`, released at close) checked against the process's other locks of the file, `OverlappingFileLockException` as the JDK's `FileLockTable`; `map` READ_ONLY, READ_WRITE and PRIVATE (the extended SYNC modes `UnsupportedOperationException`), the file extended when the region passes its end, as jdk26u's; close writes back the read/write mappings, releases the locks and closes the stream through its parent, as jdk26u's |
| `jdk.internal.jrt.HostFileChannels` (overlay, new) | what the channel and the mapped buffers share: `pread`/`pwrite` (save the position, seek, read or write, restore, under the descriptor's monitor), the lock table (keyed by the file's absolute path, or the descriptor of a stream made from one), a `Mapping` (descriptor, path, file position, written back or not) whose `force` writes a region back, never past the file's current end (a truncated file stays truncated; through the path opened again when the descriptor is closed), and the access through which `FileChannelImpl` makes a `DirectByteBuffer` (set by its static initializer, as `HostFiles.DescriptorAccess`) |
| `MappedByteBuffer` (overlay, jrt's own) | non-sealed (jdk26u's is sealed with its DirectByteBuffer, a `sun.nio.ch.DirectBuffer`; F1's GaloisCounterMode casts a direct buffer to one, which a sealed class without such a subclass makes a compile error: the joint check below); jdk26u's public members over a heap copy: `isLoaded` true, `load` nothing, `force()`/`force(index, length)` the mapping's write-back (nothing without a file), the covariant overrides; `unmapper()` null (Buffer's `JavaNioAccess`, cut by Buffer's variant, still compiles) |
| `DirectByteBuffer`, `DirectByteBufferR` (overlay, derived) | a direct buffer is a heap buffer that is a `MappedByteBuffer`, as the JDK's direct buffers are: derived mechanically from the JDK build's generated `HeapByteBuffer.java`/`HeapByteBufferR.java` (the class renamed, `extends MappedByteBuffer`, the mapping passed to every buffer made from it, `slice`/`duplicate`/`compact` returning `MappedByteBuffer`) plus `DirectByteBuffer(int)` (`ByteBuffer.allocateDirect`'s), the JDK's address constructors refusing (`UnsupportedOperationException`: Buffer's cut `JavaNioAccess` names them) and the static initializer; `isDirect()` answers false (JCS152); derivation below |
| `FileChannel` (overlay) | jdk26u's with one change: `open(Path, Set, FileAttribute...)` asks `FileChannelImpl` (jrt's Path has no FileSystem); a file attribute refused. An overlay, not a variant: bin/jrt-convert's javac step compiles the tree with jrt's `Path`, which jdk26u's `path.getFileSystem().provider()` does not compile against (found by the full-closure javac check below) |
| the streams | `FileInputStream`, `FileOutputStream` (overlays): `getChannel` (jdk26u's), close closing the channel first (jdk26u's); `FileOutputStream` remembers `append`; `RandomAccessFile`'s variant: close closes the channel (its `getChannel` is jdk26u's, no longer a stub) |
| `AbstractInterruptibleChannel` (variant) | `blockedOn` does nothing (jrt's hand-written Thread has no blocker); `end` also checks the thread's interrupt status: an interrupt before an operation (begin, jdk26u's) or during it closes the channel, `ClosedByInterruptException`; a thread blocked in a pipe's read is not woken |
| j2g | rename `sun/nio/ch/Streams` → `Ch_Streams` (java.util.stream's); no class-supers needed (FileLockConstructor's `FileLock` subclass optional, that test needs F10 too) |

**The derivation of `DirectByteBuffer(R)`** (to be replayed if jdk26u's template changes; scratch script
`.tmp/jc9b/F7/gendirect.py`, to be copied with the worktree's scratch): read the build's
`support/gensrc/java.base/java/nio/HeapByteBuffer{,R}.java`; drop the generated-file banner (the header names
the source); add `import jdk.internal.jrt.HostFileChannels;`; rename `HeapByteBufferR`→`DirectByteBufferR`,
`HeapByteBuffer`→`DirectByteBuffer`; `extends ByteBuffer`→`extends MappedByteBuffer`; `public ByteBuffer
slice()`, `slice(int index, int length)`, `duplicate()`, `compact()` → `public MappedByteBuffer ...`; the
protected constructor's parameters gain `HostFileChannels.Mapping mapping` and its `super(...)` passes it; the
two package-private constructors' `super(...)` pass `null`; every `new DirectByteBuffer[R](hb, ..., segment)`
passes `mapping`; into `DirectByteBuffer` before its `(int cap, int lim, MemorySegment)` constructor: the
`(int)` constructor, the three address constructors and the static initializer.

Left out, with their tests: the selectable, socket and asynchronous channels (`ChannelInputStream`'s and
`Streams`' `instanceof SelectableChannel`/`SocketChannelImpl` are false, `FileLock(AsynchronousFileChannel, ...)`
a stub: F10; `LongTransferTest`, `SocketChannel/CloseDuringWrite`, `FileLockConstructor`,
`AsynchronousFileChannel/Basic`, `ReleaseOnCloseDeadlock`), `System.inheritedChannel` (`ClosedStreams`, F16's
member), locks against other processes (JCS149), a mapping's sharing with other writers of the file
(JCS150), direct memory (`DirectByteBuffer(long, ...)`, memory segments' buffers, `FileChannel.map(..., Arena)`:
the FFM, by design), `FileChannel.open`'s POSIX attributes. What stays outside the world in members no
overlay or variant replaces becomes an operation-level stub: `Channels.newInputStream(AsynchronousByteChannel)`
and its output twin, `FileLock`'s asynchronous constructor, `MemorySegment`-typed members of the buffers (as
today's heap buffers').

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 1.1 GiB; scratch in the worktree's `.tmp/jc9b/F7/`):
- the closure (`.tmp/jc9b/closure.py`, the world extended by JC9's lists and overlays and F7's own): 25 files
  of `share`, 3,374 lines (FileChannel's 1,346 taken as the overlay instead), and 35 generated buffers, 23,726
  lines; what it names outside the world and the list, each accounted for above (the async and selectable
  channels, `Streams`' socket streams, Channels' charset coders, `FileSystemProvider` and `Arena` in
  FileChannel, and the generated buffers' `AbstractMemorySegmentImpl`, `SegmentFactories`, `Bits` and their
  direct twins, as the world's generated buffers name them today);
- the generator: the generate step's `genbuffer` with the read-only argument, run on the JDK with the build's
  tools (`.tmp/jc9b/F7/genbuf.sh`): 45 buffers (the world's 10 and F7's 35) equal to the build's
  `support/gensrc/java.base/java/nio/` byte for byte;
- the read: `bin/repr-java read-jdk --check` of the 60 files, the 6 new and 2 changed overlays (with
  `HostFiles` and `FileDescriptor`, which they use) as a patch of java.base: 70 converted, 0 failed, 0 javac
  errors, 0 unreadable (4.7 s, 0.6 GB); `--no-javac`: 70 native, 0 fallbacks, the forms identical (13.7 s,
  0.7 GB);
- the variants applied and parsed (`.tmp/jc9b/vet.ae`): AbstractInterruptibleChannel 2/0/0 (replaced/cut/added),
  RandomAccessFile 12/2/0 on the world's forms, all parsed;
- javac, the full closure: the committed world's 907 sources (jdk/sources.txt; the overlays at HEAD but F7's,
  a stand-in for the generated `LocaleDataIndex`), F7's 60 and its overlays, 972 files, compiled by the JDK's
  javac with `--patch-module java.base`: 0 errors, 2,393 classes (8 s, 1.1 GB). This check, unlike F4's and
  F2's, compiles against jrt's own overlays (Path, Files, FileDescriptor ...): it found jdk26u's
  `FileChannel.open` not compiling against jrt's `Path` (hence the overlay; "Findings");
- Go names: every class of the new forms against the gate executable's types, jrt's hand-written ones and
  JC9's: one collision, the rename above.

**JCS135, the closure's growth**: +31,165 Java lines (**+6.4%** on 483,461: 3,374 share, 23,726 generated, the
mostly blank lines of Spp's output among them, 3,982 of jrt's own overlays, +83 in the streams), the forms
+299,781 bytes (**+2.7%** on 11,161,667); by F4's ratio of forms to executable about +3 MB.

**Expected yield** (the triage's counts, the scan of `d86cb874a`): 62 interpreted / 66 compiled tests at most
(its causes alone), of which 3 / 2 stay out by the cuts above (`LongTransferTest`, `CloseDuringWrite`
interpreted, `ClosedStreams` compiled); at risk: the interrupt tests (`ClosedByInterrupt`, `InterruptDeadlock`,
`InterruptMapDeadlock`: a thread interrupted while blocked), `FileDescriptor/Finalize` and `Sharing` (finalization,
GC), `LargeFileAvailable` (a 7.4 GB sparse file), `MapTest`, `ExpandingMap`, `CRCTest` and `TimeChecksum`
(interpreted speed), the Channels reader/writer tests (`Channels/Basic`, `TinyBuffers`,
`CloseWriterOnFailedFlush`) until F8's coders. About **43 / 46** expected (0.7 × alone).

**The night's commands**, in the worktree after `next` holds F16 and F8 (merge `next` first; heavy ones under
`bin/heavy-lock`); about 2.5-3 hours with the check:

```sh
cd /root/arbace/.tmp/worktrees/java-jc9b
git merge next      # JC9 and F16, F8 (their nights' conversions committed)
# 1. jdk/ converted afresh: expect the sources line's counts up by 25 share, 35 generated, 6 of jrt's own
#    (overlay/jdk), the generate step's buffers :same as the build's gensrc (35 more equal), 0 failed,
#    0 javac errors, no javac: 0 fallbacks; jdk/forms + 66 files and 3 package indexes (java/nio/channels,
#    java/nio/channels/spi, sun/nio/ch)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (the new classes' outside references: SelectableChannel, SocketChannelImpl,
#    AsynchronousFileChannel, StreamDecoder/StreamEncoder unless F8 has them, Arena, MemorySegment)
bin/heavy-lock bin/j2g-edge update
# 3. the executable (JCS135: its size against the previous night's, `ls -l`; the j2g and Go times); j2g's
#    report: AbstractInterruptibleChannel 2 replaced, RandomAccessFile 12 replaced 2 cut; no "variant for a
#    class not in the inputs"
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F7 on it: expect the lines "4 [104 105 33 0]", "4 true", "true", "[1 2]", "[104 105 33 9]"
#    (a map extending the file, written back by force; a read-only map; a lock; a direct buffer's view)
target/arbace-go/amd64/arbace -e '(import (quote (java.io File RandomAccessFile)) (quote (java.nio ByteBuffer)) (quote (java.nio.channels FileChannel$MapMode)) (quote (java.nio.file Files)))
  (let [f (File/createTempFile "f7" ".bin") raf (RandomAccessFile. f "rw") fc (.getChannel raf)]
    (.write fc (ByteBuffer/wrap (.getBytes "hi!")))
    (let [m (.map fc FileChannel$MapMode/READ_WRITE 0 4) b (byte-array 4)]
      (.get (.duplicate m) b) (println (.capacity m) (vec b))
      (.put m 3 (byte 9)) (.force m))
    (println (.length raf) (.isLoaded (.map fc FileChannel$MapMode/READ_ONLY 0 1)))
    (let [l (.lock fc)] (println (.isValid l)) (.release l))
    (let [lb (.asLongBuffer (doto (ByteBuffer/allocateDirect 16) (.putLong 0 1) (.putLong 8 2)))]
      (println [(.get lb 0) (.get lb 1)]))
    (.close raf)
    (println (vec (Files/readAllBytes (.toPath f))))))'
# 5. Java's levels interp and compiled on F7's areas (then the whole corpus with JC9b's on its night):
#    saved, not recorded; expect about 43 / 46 of the tests above to pass, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(java/nio/(channels|MappedByteBuffer|Buffer)|java/io/(FileDescriptor|FileInputStream|FileOutputStream|InputStream|RandomAccessFile|Reader|Writer)|java/lang/Appendable|java/security/MessageDigest/ByteBuffers|java/util/(Base64|BitSet|zip)|javax/crypto/Mac/ByteBuffers|sun/nio/c[hs])|hotspot/jtreg/(compiler/codegen/CRCTest|runtime/verifier)' \
  --save .tmp/f7-night/check
```

(The smoke's expected line is the design's; a mismatch there is the night's first finding, not a stop.)

Then, the day after (light), as F4's and F2's: the saved results judged (`bin/java-check --judge`), the tail
classified (`:net` and `:nio` narrowed: what F7 leaves as `:nio-selectors`, `:nio-interrupt`, the cut causes
named above), `--record` once the whole corpus is rerun, `bin/reap --record`, the docs with the numbers
(JRT-SOURCES.md's closure list, JRT-NOTES.md's "Files" with the channels and buffers, J2G-SPEC §4.4's rename,
§12's buffers), the commit of `jdk/`, the merge to `next`.

**Risks and fallbacks**: (1) the derived `DirectByteBuffer`: a member of `HeapByteBuffer`'s that j2g or the
interpreter dispatches by class name (`HeapByteBuffer` special-cased anywhere) would miss it; the read and javac
accept it; (2) `isDirect()` false while the class is named direct: code that branches on `getClass()` names
(none in the closure) or a test asserting `isDirect()` (none of the 84) would see the difference (JCS152);
(3) the position dance of `pread`/`pwrite` races with a stream's own unsynchronized read on the same descriptor
in another thread (none of the tests does both at once); should it show, JCS149 (b); (4) a map's heap copy:
tests that read a file through a stream after writing through a map without `force` see the old bytes until the
channel closes (MapTest, Basic write then force or close; none reads between); (5) the interrupt tests' threads
blocked in a read: `ClosedByInterrupt` reads a file (never blocks long), so the variant's check at `end` should
do; (6) `HostFileChannels.map`'s initialization of `DirectByteBuffer` by `ByteBuffer.allocateDirect(0)`: a
translated static initializer runs at the class's first use in jrt as in the JDK.

**What is left of F7**: the night's run and the day's classification and records; JCS149 to -4 (Proposed);
F5's `LoggingFileHandler` variant can drop its cut of `lockFileChannel` once F7 is in (a follow-up: F5's
handler then locks as the JDK's, through `FileChannel.open` and `tryLock`).

## As built: F11 (write-ahead)

Branch `java-jc9b` (from `java-jc9-pki`), 2026-11-05, prepared by day under the light rules as F4 and F2 were:
the closure's list, jrt's own Java, the variants, light checks only; nothing converted, translated or built.
**F11 needs nothing of JC9 or of F16, F8, F7**: its closure was computed against the world plus F1's, F5's, F4's
and F2's lists (by reference; `datagram-sources` names none of their files, nor any of JC9b's other lists'),
and none of its files is java.nio's. **Its Go natives are written** (`java-jc9b-natives`, 2026-11-05: `go/arbace/jrt/udp.ae`,
`udp_linux.ae`, `udp_test.ae`; "As built: the Go natives of F8 and F11" below), to the design below; without
them the build would fail at j2g's natives check on `HostDatagram`'s six natives.

**The mechanism.** jdk26u's `DatagramSocket` delegates to `sun.nio.ch.DatagramSocketAdaptor` over
`DatagramChannelImpl` (java.nio's channels, selectors and the VM's natives: F10's world, not F11's), or, when a
`DatagramSocketImplFactory` is set, to the legacy `NetMulticastSocket` over the factory's `DatagramSocketImpl`.
The Go build takes the second path always: `DatagramSocket.createDelegate`'s variant wraps jrt's own
`java.net.HostDatagramSocketImpl` in `NetMulticastSocket`, as jrt's `HostSocketImpl` replaces NioSocketImpl for
TCP. `NetMulticastSocket` (jdk26u's, plain Java: the JDK's default delegate until JDK 15, JEP 373) keeps the
state checks and messages; the impl is the host's UDP socket. NetworkInterface's natives read the host's
interfaces (Go's `net.Interfaces`).

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `datagram-sources`, one line in
`sources`):

| part | how |
|---|---|
| java.net's datagrams (8) | `DatagramSocket` (variant), `MulticastSocket`, `DatagramPacket`, `NetMulticastSocket`, `DatagramSocketImpl`, `DatagramSocketImplFactory`, `PortUnreachableException`, and `ProtocolFamily`/`StandardProtocolFamily` (named by tests and, later, by F10's channels) |
| the interfaces (3) | `NetworkInterface` (variant: the natives over `HostDatagram`, no `loadLibrary("net")`, no native init; the name is the display name; no sub-interfaces: Linux's `eth0:1` aliases are not in Go's list), `InterfaceAddress` (variant: an added constructor, the JDK's natives set its private fields by JNI), `DefaultInterface` (KIND `unix`: null, the kernel's choice) |
| jrt's own (overlay, 2) | `java.net.HostDatagramSocketImpl` (the platform's `DatagramSocketImpl`: bind, send, receive, peek, connect over the host's socket, multicast join and leave, the legacy `SocketOptions` and `StandardSocketOptions`' datagram options; options set before the bind kept and applied by it, SO_REUSEADDR/SO_REUSEPORT before the host's bind itself; in `java.net`, as the JDK's legacy `PlainDatagramSocketImpl` was, to set a received packet's `length` without its `bufLength` and to answer the package-private `dataAvailable`), `jdk.internal.jrt.HostDatagram` (the natives; the interfaces as rows, `Row`, with the lookups by name, index and address the variant calls; the error kind `PORT_UNREACHABLE`, `PortUnreachableException`; jrt's option numbers for the multicast TTL, loopback and interface) |
| the variants (3) | `DatagramSocket.ae` (`createDelegate`: the factory's impl or `HostDatagramSocketImpl`, both in `NetMulticastSocket`; the constructor's assert without `DatagramSocketAdaptor`), `NetworkInterface.ae`, `InterfaceAddress.ae` |
| j2g | nothing: no rename (the new classes' Go names collide with none), no `class-supers` (no test of the list extends a datagram class or sets a factory); `NetworkInterface` leaves `j2g_cut.go` (it was a cut class), `DatagramChannel` enters it (`getChannel`'s type) |

**The Go side (written, with the departures listed below)**: a datagram socket is a handle of `HostNet`'s table (`netSock`
with `c` a `*net.UDPConn`), so HostNet's `close0`, `available0` (FIONREAD: the next datagram's size, which
`NetMulticastSocket`'s connect filter asks), `address0`, `port0`, `setOption0` and `getOption0` serve it. In
`go/arbace/jrt/udp.ae` (new; `net.ae`'s helpers):
- `UDPHost`, an optional interface beside `NetHost` (a host without it, the box's, has no datagrams: every native
  `HostNet.UNSUPPORTED`, the box unchanged): `ListenUDP(address string, reuseAddr, reusePort bool) (*net.UDPConn,
  error)` (OSHost: `net.ListenConfig{Control: SO_REUSEADDR, SO_REUSEPORT (15)}.ListenPacket(ctx, "udp", address)`)
  and `Interfaces() ([]net.Interface, error)` (`net.Interfaces`, `Addrs` each).
- `HostDatagram_Open0_String_I_Z_Z_String1__I_native`: `ListenUDP(joinHostPort(host, port), ...)` into the
  table; EADDRINUSE etc. `netBind`.
- `HostDatagram_Send0_I_B1_I_I_String_I_String1__I_native`: `WriteToUDP` to the resolved literal, or `Write` when
  host is null (the kernel-connected peer); ECONNREFUSED `PORT_UNREACHABLE` (-14), EMSGSIZE the JVM's "Message too
  long" (`SendSize`).
- `HostDatagram_Receive0_I_B1_I_I_I_Z_B2_I1_String1__I_native`: `SetReadDeadline` by the timeout (`netTimeout`);
  `ReadFromUDP` into a `len`-byte slice (the rest of a longer datagram discarded, as recvfrom); peek through
  `SyscallConn().Read(func(fd) bool)`, `syscall.Recvfrom(fd, buf, MSG_PEEK|MSG_DONTWAIT)`, EAGAIN false (the
  poller waits, the deadline holds); `ipBytes` of the sender into `from[0]`, its port into `port[0]`;
  ECONNREFUSED `PORT_UNREACHABLE`; a closed handle `netClosed`.
- `HostDatagram_Connect0_I_String_I_String1__I_native`: `syscall.Connect(fd, sockaddr)` through `RawConn.Control`
  (not Go's connected state: `WriteToUDP` stays allowed, the kernel filters what is received and reports the
  ICMP port unreachable as ECONNREFUSED); host null: `SYS_CONNECT` with an `AF_UNSPEC` `RawSockaddr`.
- `HostDatagram_Join0_I_B1_I_B1_Z_String1__I_native`: `SetsockoptIPMreqn(IP_ADD_MEMBERSHIP / IP_DROP_MEMBERSHIP,
  {group, ifindex})` for 4 bytes, `SetsockoptIPv6Mreq(IPV6_JOIN_GROUP / IPV6_LEAVE_GROUP)` for 16; a source:
  `IP_ADD_SOURCE_MEMBERSHIP` (unused until F10's MulticastChannel).
- `HostDatagram_Interfaces0_String1__O1_native`: per interface an `Object[]` (`NewRefArray` of Object) of its name,
  `int[]{index, flags, mtu}` (Go's `net.Flags` bits as `HostDatagram`'s constants), the hardware address (null
  when empty: the JDK answers null for the loopback), the addresses' `ipBytes` (`byte[][]`, as `lookup0`'s), their
  prefix lengths (`Mask.Size`), the IPv4 broadcast (`ip | ^mask` when `FlagBroadcast`, else null).
- `net_linux.ae`'s `sockopt`: SocketOptions' `SO_BROADCAST` (0x20) and jrt's `OPT_MULTICAST_TTL` (`IP_MULTICAST_TTL`
  and, on an IPv6 socket, `IPV6_MULTICAST_HOPS`), `OPT_MULTICAST_LOOP` (`IP_MULTICAST_LOOP`, `IPV6_MULTICAST_LOOP`),
  `OPT_MULTICAST_IF` (`IP_MULTICAST_IF` with an `ip_mreqn` of the index, `IPV6_MULTICAST_IF`); `GetOption`'s boolean
  folding to leave the TTL and the interface as numbers; `net.ae`'s `sockAddr` to take a `*net.UDPAddr` too.
About 250 lines of Go forms, a day with the tests (net_test.ae's loopback cases: send and receive, peek, the
timeout, connect's filter and the unreachable port, a join on the loopback, the interfaces' list against `ip addr`).

Left out, with their tests: `InetAddress.isReachable` (`InetAddress/IsReachable`: ICMP echo, not in the Go build,
Inet6AddressImpl's variant), the IPv6 scope's interface name (`Inet6Address`'s variant keeps no `scope_ifname`: a
link-local address prints `%index`, not `%eth0`: `Inet6Address/StringScope` at risk), `jdk.sctp`
(`com/sun/nio/sctp/MessageInfoTests`, which the triage filed under F11 by its `Association`: SCTP is its own
module, not datagrams), and DatagramChannel (F10: `AddressNotSet`, `GetLocalAddress`, the `DatagramChannel/*`
and `unixdomain/*` tests that also need selectors). What stays outside the world in members no variant replaces:
`DatagramSocket.getChannel`'s type (a cut class; the method answers null, as the JDK's for a socket not made by a
channel), the serialization of `NetworkInterface`'s users (`Inet6Address/serialize`, by design).

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-8f24f568a977b802084a`,
`REPR_JAVA_STALE_OK=1`, each under 0.6 GB; scratch in `.tmp/jc9b/F11/`):
- the closure (`closure.py`, every module indexed, the world extended by JC9's lists and overlays, scope
  `java.net`): 12 files, 4,289 lines (11 `share`, 1 `unix`); outside the world and the list it names
  `DatagramChannel`, `MulticastChannel` (javadoc and `getChannel`), `sun.nio.ch.DatagramSocketAdaptor` (the
  replaced `createDelegate` and the constructor's assert) and `BootLoader` (the replaced static initializer);
- `jrt.jrt-sources/print-sources` with the def in a scratch copy of `test/jrt/jrt_sources.ae`: the branch's list
  plus exactly those 12 lines (the two overlays are listed by `overlay-sources` already);
- the read: `bin/repr-java read-jdk --check` of the 12 files, the 2 overlays and HostNet (which HostDatagram
  names) as a patch of java.base: 15 converted, 0 failed, 0 javac errors, 0 unreadable (1.8 s, 0.5 GB);
  `--no-javac`: 15 native, 0 fallbacks, the forms identical;
- the variants applied and parsed (`vet.ae`): DatagramSocket 2/0/0 (replaced/cut/added), NetworkInterface 12/1/2,
  InterfaceAddress 0/0/1, all parsed;
- javac: the 12 sources, the 2 overlays and HostNet compiled by the JDK's javac with `--patch-module java.base`:
  0 errors, 18 classes;
- Go names: the 14 new classes (with the nested `HostDatagram_Row`) against the gate executable's Go types, jrt's
  hand-written ones and JC9's new classes: no collision.

**JCS135, the closure's growth**: +4,868 Java lines (4,289 jdk26u's, 579 jrt's: **+1.0%** on the closure's
483,461), the forms +71,938 bytes (**+0.6%** on `jdk/forms`' 11,161,667); about +0.7 MB on the executable by JC6b's
measure.

**Expected yield** (the triage's counts, the scan of `d86cb874a`): 38 interpreted / 38 compiled tests alone at
most, of which 2 are not F11's (`IsReachable`, `MessageInfoTests`) and 1 at risk of the scope's name
(`StringScope`): about **25 / 25** expected (0.7 of 35). At risk besides: the multicast joins without an interface
(`B6427403` joins on `getLocalHost()`'s socket: the kernel needs a multicast route, which the check's sandbox may
lack; then `:environment`, as the JDK's own run there decides), `InheritHandle` (a child process while the socket
is open), `OneExceptionOnly` and `Concurrent` (Linux reports one ICMP error per socket error queue entry: the
count of PortUnreachableExceptions is the kernel's, as the JDK's legacy impl's), `SendSize` (EMSGSIZE's message),
the outputs that print an address (`TestDefaults` prints `0.0.0.0/0.0.0.0`, as the JDK's adaptor). The NetworkInterface
tests read the host's interfaces live, as the JDK's run did: their digests hold when the check's host has the
reference run's interfaces (the check runs where the reference was recorded).

**The night's commands**, in the worktree after its earlier fixes (merge `next` first), heavy ones under
`bin/heavy-lock`; F11's own share is the Go natives (written and committed by day before the night: `udp.ae`,
`net_linux.ae`, `net.ae`, tests run with `bin/jrt test`), then JC9b's common conversion:

```sh
cd /root/arbace/.tmp/worktrees/java-jc9b
# 1. jdk/ converted afresh (JC9b's whole conversion; F11's part): expect sources.txt +12 lines (11 share, 1
#    unix) and the 2 overlays, 0 failed, 0 javac errors, no javac: 0 fallbacks; jdk/forms +14 files
REPR_JAVA_EXE=$PWD/target/arbace-go/amd64/arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (DatagramChannel and MulticastChannel as outside references)
bin/heavy-lock bin/j2g-edge update
# 3. the executable; j2g's report: "variant of java/net/DatagramSocket: 2 replaced, 0 cut, 0 added",
#    NetworkInterface 12/1/2, InterfaceAddress 0/0/1; no native without its Go function
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F11: the loopback's name and true, then "hi" from 127.0.0.1, then 1 (the TTL) and 7
target/arbace-go/amd64/arbace -e '(import (quote (java.net DatagramSocket DatagramPacket InetAddress MulticastSocket NetworkInterface)))
  (println (.getName (NetworkInterface/getByInetAddress (InetAddress/getLoopbackAddress))) (.isLoopback (NetworkInterface/getByName "lo")))
  (with-open [a (DatagramSocket. 0 (InetAddress/getLoopbackAddress)) b (DatagramSocket.)]
    (.send b (DatagramPacket. (.getBytes "hi") 2 (InetAddress/getLoopbackAddress) (.getLocalPort a)))
    (let [p (DatagramPacket. (byte-array 16) 16)] (.receive a p) (println (String. (.getData p) 0 (.getLength p)) (.getAddress p))))
  (with-open [m (MulticastSocket.)] (println (.getTimeToLive m)) (.setTimeToLive m 7) (println (.getTimeToLive m)))'
# 5. Java's levels interp and compiled on F11's area: saved, not recorded; expect about 25 / 25 of the 38 to
#    pass, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/net/(B6499348|Datagram|MulticastSocket|NetworkInterface|InterfaceAddress|Inet6Address|InetAddress/IsReachable|PlainSocketImpl|PortUnreachable|Socket/|ServerSocket/)' \
  --save .tmp/f11-night/check
```

Then, the day after (light): the saved results judged, `:net`'s datagram tail classified (`:net-multicast-route`
or `:environment` for the joins the sandbox cannot route, `:jrt-scope-name` for StringScope if it fails), the
records with the whole corpus's rerun, `bin/reap --record`, JRT-NOTES.md's "Datagrams" (new, beside "Sockets"),
J2G-SPEC §4.1's cut list.

**Risks and fallbacks**: (1) `NetMulticastSocket`'s legacy behaviour differs from the adaptor's in a few
messages and in `connect`'s filter (the JDK's tests are written against the adaptor since JDK 15): such a test
fails in a new narrow class, and the impl's message is changed to the adaptor's where the impl throws it; (2) the
Go natives unwritten at the night: the build stops at j2g's missing natives; the fallback leaves F11 out of that
night by moving its parts aside (`datagram-sources`' line in `sources`, the two overlays and `DatagramSocket.ae`, whose
`createDelegate` names the impl; `NetworkInterface.ae` and `InterfaceAddress.ae` name classes then not in the
inputs: out too), the rest of JC9b unaffected; (3)
`InterfaceAddress`'s added constructor and NetworkInterface's private fields set from its variant: plain Java
forms, untried by j2g's analysis by day (only parsed); (4) the IPv6 interface names (`%eth0`): Inet6Address's
variant could keep `scope_ifname` now that NetworkInterface is in the world (a small follow-up, JCS154).

**What is left of F11**: the Go natives (a day), the night's run and the day's classification and records;
JCS153, JCS154 (Proposed).

## JC9b's night: the four together (write-ahead)

The four fixes are one branch (`java-jc9b`) and one conversion, as JC9's were meant to be night by night; their
own sections' commands are the parts. **Order**: JC9b comes after JC9 (F1, F5, F4, F2 on `next`), and within it
F16 < F8 < F7 < F11 in the file and in the dedupe (the earlier fix keeps a file: F8 took `Channel` and
`ReadableByteChannel`, which F7's list named too; F16 took `Readable`, which javac's tool world held). The
branch's hunks in the shared files are apart from JC9's: `git merge-tree` of the branch with `java-jc9-jca` and
`java-jc9-logging` conflicts in this file alone, as `java-jc9-pki` already did (both append to it), and merges
`test/jrt/jrt_sources.ae`, `bin/jrt-convert`, `bin/javac-vendor`, `arbace/j2g/model.ae`, `names.ae` and LICENSE.md
cleanly. **Not mergeable before its night**, as JC9's branches: its variants name classes `jdk/forms` does not
hold yet.

**Joint light checks** (scratch `.tmp/jc9b/full/`, `.tmp/jc9b/all9/`):
- `print-sources` of the branch: 1,419 lines, `java-jc9-pki`'s 1,216 plus 205 (95 `share`, 5 `share:java.sql`, 3
  `share:java.sql.rowset`, 1 `unix`, 86 `gensrc`, 15 `overlay`) less jrt's two readers' overlays; no line twice;
- the full closure by the JDK's javac (F7's check, made general: every line of print-sources from jdk26u, the
  build's gensrc and the worktree's overlays, `LocaleDataIndex` a stand-in, `--patch-module java.base`): 1,419
  files, **0 errors**, 3,105 classes (16 s, 1.0 GB). This is the check F1, F5, F4 and F2 did not run: theirs
  compiled each fix's files against the JDK's own java.base, not against jrt's overlays, which `bin/jrt-convert`'s
  convert step reads them with (`REPR_JAVA_PATCH_ALL`);
- the same over JC9 and JC9b merged (`git merge-tree` of the branch with `java-jc9-jca`, then `java-jc9-logging`,
  `jrt_sources.ae`'s F1/F5 conflict resolved as the union): 1,785 files; it found **two faults**: F1's
  `GaloisCounterMode.overlapDetection` casts a direct buffer to `sun.nio.ch.DirectBuffer`, which F7's sealed
  `MappedByteBuffer` made a compile error (fixed on this branch: `MappedByteBuffer` non-sealed, F7's section), and
  **F5's `Logger` and `LogRecord` call `ResourceBundle.getBundle(String, Locale, ClassLoader)`, which jrt's own
  `ResourceBundle` (overlay) has not**: 2 errors, F5's night's conversion would stop at its javac errors
  (Findings; a fix on `java-jc9-logging`, not here).

**JCS135, JC9b's growth**: F16 +13,065, F8 +59,155, F7 +31,165, F11 +4,868 Java lines: **+108,253, +22.4%** on the
closure's 483,461; the forms +2,052,046 bytes (**+18.4%** on 11,161,667). With JC9's +198,917: **+307,170 lines,
+63.5%**, the forms about +7 MB (+63%), the executable by the forms' share some +45 MB (22%) on the gate build's
206.9 MB. JCS135 (a)'s threshold (a minute more of build) is measured at each night.

**Expected yield**: at most 187 interpreted / 187 compiled of the deferred tests (F16's Java part 29 / 27, F8 58 /
56, F7 62 / 66, F11 38 / 38; the triage's alone counts, F16's Go part out), about **119 / 120 expected** (F16 20 /
19, F8 31 / 30, F7 43 / 46, F11 25 / 25), eval from JC9's expected ~2,090 to about 2,210 (the triage's eight
fixes' 2,130-2,240 band), AOT likewise.

**The night's order** (the sections' commands merged; heavy ones under `bin/heavy-lock`; about 4-5 hours with the
area checks, then the whole corpus as JC6b):

```sh
cd /root/arbace/.tmp/worktrees/java-jc9b
git merge next      # JC9 (F1, F5, F4, F2), its nights' jdk/ and jdk/javac/ committed
# 0. by day before it, the Go (no light check can try it; each with bin/jrt build && bin/jrt test and a build):
#    F8's stand-in (charset.ae -> standin_charset.ae, String's overloads through CharsetStrings, codec.ae's
#    charsetOf, access.ae's decodeASCII and uncheckedEncodeASCII, the manifest) and F11's udp.ae with HostNet's
#    options (their sections); F16's Go members are JCS143's, not this night's
# 1. javac's world gains F8's charsetmapping tool (jdk/javac/sources.txt +11), F16's three classes kept in it for
#    this run (the generate step's tools read with Scanner, which jdk/ has only after step 2), then javac's tool
printf '%s\n' 'java.base java/lang/Readable.java' 'java.base java/util/Scanner.java' \
  'java.base java/util/PropertyResourceBundle.java' >> overlay/jdk/javac/closure.txt
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  bin/heavy-lock bin/javac-vendor convert && bin/heavy-lock bin/arbace-go --build-java
# 2. jdk/ afresh: sources.txt +205 -2 lines, the generate step's 51 charset files and 35 buffers :same as the
#    build's gensrc, 0 failed, 0 javac errors, no javac: 0 fallbacks; jdk/data + uprops.icu, content-types.properties
REPR_JAVA_EXE=$PWD/target/arbace-go/amd64/arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2b. javac's world without F16's three (the program's now: jdk/javac/sources.txt -3) and without F8's
#     InputStreamReader variant (jdk26u's reader has the constructor)
git checkout overlay/jdk/javac/closure.txt && git rm overlay/jdk/javac/variants/InputStreamReader.ae
REPR_JAVA_EXE=$PWD/target/arbace-go/amd64/arbace-javac REPR_JAVA_STALE_OK=1 bin/heavy-lock bin/javac-vendor convert
# 3. the edges, then javac's tool on the new jdk/, the executable, smoke, jrt's tests
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge && bin/heavy-lock bin/arbace-go --build-java
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. the four smokes (their sections), then Java's levels interp and compiled over the four --only patterns
#    joined, saved, not recorded; then the whole corpus (the readers and writers are under every test)
```

Should one fix's part fail, its section's fallback leaves it out of the night (its def's line in `sources`, its
overlays and variants aside), the others unaffected: F11's needs only its Go natives; F7 needs F8 for `Channels`'
readers (operation-level stubs without it); F8 needs F16 only for the charsetmapping tool's `Scanner` (javac's
world's until F16, then the program's, with the variant either way).

## Decided (2026-11-05, the user: JCS143-JCS154)

JCS143-JCS154 as recommended: 143 (b) F16b after JC9b's nights; 144 (a) version 26, the version-printing tests to the harness class; 145 (a) field updaters as built, the exact check with F20; 146 (b) jdk.charsets later; 147 (a) jdk26u's readers and writers, jrt's as fallback; 148 (a) tables generated on javac's tool; 149 (c) positional I/O and locks over today's natives; 150 (a) mappings as heap copies written back; 151 (b) DirectByteBuffer generated; 152 (a) isDirect false; 153 (c) the legacy datagram delegate, the adaptor with F10; 154 (a) IPv6 scope names with F11.

## As built: the Go natives of F8 and F11

Branch `java-jc9b-natives` (from `java-jc9b`), 2026-11-05, by day under the light rules: Go forms written and
checked by jrt's own build alone (`bin/jrt build --arch amd64` and `--arch arm64`: print, `go build`, `go vet` of
the package with TamaGo's go; `bin/jrt test --arch amd64`: jrt passes, with `TestDatagrams`, `TestMulticastJoin`,
`TestInterfaces` new). No j2g run, no conversion, no gate build, so what a j2g program does with them (the natives
check, the stand-in drop, the Go names of the translated classes) is the nights' first check. The branch's merge
into `java-jc9b` precedes both nights. Main's stage 0 hosted the printer (`ARBACE_HOST_EXE`); `bin/jrt build` ran
`jrt.testdata` once on the frozen jar of the worktree (built then, 2.5 minutes) and `bin/jrt manifest` once (16 s).

**F11, `udp.ae` and `udp_linux.ae`** (about 420 lines with the tests' 150; the design said 250: the
Linux-specific half is a file of its own, which the box's build does not compile).
- `UDPHost` (udp.ae), the optional interface beside `NetHost`, is wider than designed: `ListenUDP`, `Peek`,
  `ConnectUDP`, `Membership`, `Interfaces`. The designed split put `syscall.Recvfrom`, `Connect` and the multicast
  structs in the natives themselves, which would have made `udp.go` Linux-only; the natives (portable: Go's
  `net.UDPConn`, `errors`, `syscall.Errno`) stay in `udp.ae` and every descriptor-level call is OSHost's method in
  `udp_linux.ae` (`//go:build linux`, as `net_linux.ae`). A host without `UDPHost` (the box's, TamaGo's OSHost)
  answers `HostNet.UNSUPPORTED` from each native; `HostNet`'s own natives serve a datagram handle unchanged
  (`close0`, `available0`: TIOCINQ is the next datagram's size on Linux, as the design said).
- The six natives, by their `j2g` names: `HostDatagram_Open0_String_I_Z_Z_String1__I_native` (a
  `net.ListenConfig` with `Control` setting SO_REUSEADDR and SO_REUSEPORT before the bind),
  `Send0_I_B1_I_I_String_I_String1__I` (`WriteToUDP`, or `Write` for a null host), `Receive0_I_B1_I_I_I_Z_B2_I1_String1__I`
  (read deadline by the timeout; `ReadFromUDP`; peek through `RawConn.Read` with `Recvfrom(MSG_PEEK|MSG_DONTWAIT)`,
  EAGAIN waits under the deadline), `Connect0_I_String_I_String1__I` (`syscall.Connect` inside `RawConn.Control`,
  the sockaddr built for the socket's family by `SO_DOMAIN`, so an IPv4 peer of a dual-stack socket is v4-mapped; a
  null host: `connect(AF_UNSPEC)`), `Join0_I_B1_I_B1_Z_String1__I` (`IPMreqn`, `IPv6Mreq`, and for a v4 source
  `ip_mreq_source` with the interface's first IPv4 address), `Interfaces0_String1__O1` (the `Object[]` rows of
  `HostDatagram.Row`: name, `int[]{index, flags, mtu}` with Go's `net.Flags` bits, hardware address or none, the
  addresses, prefix lengths, IPv4 broadcast as `ip | ^mask` when the interface broadcasts).
- ECONNREFUSED on a send or a receive is `PORT_UNREACHABLE` (-14, "ICMP Port Unreachable"); EMSGSIZE's text is
  Go's, capitalised ("Message too long", the JVM's).
- **A finding**: Linux's disconnect (`connect(AF_UNSPEC)`) releases a port the bind took by itself (bound to port
  0), so a socket would lose its port. `ConnectUDP` with a nil address reads `getsockname` before and after and
  binds the old address again when the port changed; the test (`TestDatagrams`, "after disconnect") caught it.
- `net_linux.ae`: `SO_BROADCAST` (0x20) joins `sockopt`'s table; `SetOption` and `GetOption` send jrt's option
  numbers 0x2001 to 0x2003 to `mcastSet` and `mcastGet` (udp_linux.ae), before the generic table, so the boolean
  folding (any non-zero value is 1) does not touch the multicast TTL, which is a number: TTL (`IP_MULTICAST_TTL`
  and, on an IPv6 socket, `IPV6_MULTICAST_HOPS` too), loopback (likewise), interface by index (`ip_mreqn`, and
  `IPV6_MULTICAST_IF` on an IPv6 socket). `getOption` of the interface answers an index on an IPv6 socket and 0 on
  an IPv4 one (the kernel gives an address there; `HostDatagramSocketImpl` only sets it). `net.ae`'s `sockAddr`
  takes a `*net.UDPAddr` and answers its IP and port as a `*net.TCPAddr`, so `address0` and `port0` serve it.
- Left as designed or cut: IPv6 source-specific membership (`MCAST_JOIN_SOURCE_GROUP`) answers EOPNOTSUPP (nothing
  uses it before F10's MulticastChannel); the IPv6 scope in a destination is passed to Go's resolver as the zone
  and to the kernel by index; a join with a group of neither 4 nor 16 bytes is EINVAL.
- Light checks: the loopback test sends and receives, checks the sender's address and port, peeks (the datagram
  stays, `available0` sees its size), cuts a 40-byte datagram to 16, times out (50 ms), connects (a stranger's
  datagram filtered, sending without an address, the closed peer's ICMP error as -14, dissolving), the options
  (broadcast, TTL 7 read back, loopback 0 read back), a send on a closed handle (`netClosed`), a join and a leave of
  224.0.0.251 on `lo`, and the interfaces (`lo` with its flags and no hardware address, every row's six parts, the
  addresses', prefixes' and broadcasts' lengths equal). All pass on this host.

**F8, the Go side**:
- `charset.ae` is `standin_charset.ae` (`git mv`; the loader's table and `go/arbace/jrt.ae` name it), with the String
  overloads taken out and `jdk.internal.jrt.CharsetStrings`'s class and its three functions added over phase 1's
  coding: `CharsetStrings_Lookup_String__Charset` (UnsupportedEncodingException for an unknown name),
  `CharsetStrings_Decode_B1_I_I_Charset__String`, `CharsetStrings_Encode_String_Charset__B1`. A j2g program drops the
  file as it drops `standin_locale.ae` and links the translated class.
- A new `charset.ae`: `String_New_B1_Charset`, `String_New_B1_I_I_Charset`, `GetBytes_Charset__B1`, and the
  charset-name overloads `String_New_B1_I_I_String`, `String_New_B1_String`, `GetBytes_String__B1` (moved from
  string.ae: a name no longer reaches the hand-written charsets). Written against `Charset_I`, the identity with
  `UTF_8_INSTANCE`, `ISO_8859_1_INSTANCE` and `US_ASCII_INSTANCE` (phase 1's coding), and the translated
  `CharsetStrings` for any other charset; for a name, jrt's six names (a table of codec.ae's own: `charsetOf` with
  `charsetKinds`, the names and aliases the stand-in had, lower-cased) are coded here and any other name is looked
  up through `CharsetStrings_Lookup_String__Charset`, then the Charset overload. `codec.ae` gains `decodeKind` and
  `encodeKind` (the switch `DecodeBytes` and `EncodeBytes` had, by kind), which they now call.
- `access.ae`: `DecodeASCII_B1_I_C1_I_I__I` and `UncheckedEncodeASCII_C1_I_B1_I_I__I` (the scratch forms of
  `go-access.ae.txt`, unchanged), in the interface and as methods.
- The manifest: the nine entries out of `test/jrt/manifest.ae`; `bin/jrt manifest` regenerated `manifest.edn`
  (-119 lines), `manifest_test.ae` (-30), `reflect_tables.ae` (-93: Charset's and its subclasses' tables); the
  JVM's reflection cases of Charset, UTF_8 and StandardCharsets out of `test/jrt/testdata_reflect.ae`
  (`TestReflectAgainstJVM` compared the stand-in's tables with the JVM's, which no longer exist; the classes stay
  for `Class.forName` and `getSuperclass`). `edge.edn` was absent, so the manifest's coverage line for the edge is
  not printed.
- Not checked (nights): that the program's translated `Charset` has the Go interface `Charset_I` and the instances
  `UTF_8_INSTANCE`, `ISO_8859_1_INSTANCE`, `US_ASCII_INSTANCE` the new `charset.ae` names (the stand-in's shapes
  were made after j2g's, but F8's translation is the proof); if a name differs, `charset.ae` changes, not the
  design. `shims_test.ae`'s `TestCharsetsAgainstJVM` still runs, on the stand-in, and passes.

**The nights' commands, changed**: F8's step 0 and F11's "own share" are done (merge the branch first); after the
merge `bin/jrt build && bin/jrt test` must still pass (about 15 s with the test data made; the first run in a
fresh worktree builds the frozen jar). Nothing else of the nights' lists changes; F11's smoke (step 4) and F8's
(step 7) are the first run of these natives in a program.

## Decided (the user, 2026-11-07: JCS162-JCS167)

The proposals are F10a's (`java-complete`, merged into `batch1` at 23d640f54, after batch 2's base 0edacad6: their text reaches this
file when batch 1's later head and batch 2 meet on `next`). Decided: **JCS162-JCS164 (a)** (jrt's Thread gains a blocker, interrupts
wake channels and selectors; F10b's DatagramChannel then the asynchronous channels; UNIX domain sockets with F9); **JCS165-JCS166 (a)**
(`bin/jrt-convert check-javac` as a light essential gate step; the overlays' derivations as Arbace, `bin/jrt-convert derive --check`);
**JCS167 (a)**: serialization ported (ObjectOutputStream, ObjectInputStream, ObjectStreamClass over jrt's reflection, j2g's SerialNew;
clojure-cdr's staged port), its tests deferred again then regained, toward 90% strict.
