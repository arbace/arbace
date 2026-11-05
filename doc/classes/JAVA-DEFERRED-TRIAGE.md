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

## Proposed

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

## Decided (2026-11-05, the user)

JCS128-JCS135, each as recommended: :management split (:jmx by design, :mxbeans deferred); the generated by-design class; the harness's classes out of the deferred count (JCS99 (c) applied); TLS by translating SunJSSE after the JCA and PKI; RMI by design; Kerberos and GSS-API by design, SASL deferred; JC9 (F1, F5, F4, F2) then JC9b (F16, F8, F7, F11), JCS128-JCS130 first; the closure shipped, the executable's size and build time measured at each step.

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

JCS136 (a): F1's internal packages made visible at the REPL; JCS137 (a): JCS99 (c)'s 9 status-only tests listed for --record-ref and rerun on the JDK.
