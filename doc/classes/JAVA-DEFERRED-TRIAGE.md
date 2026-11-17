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
- (F10a's day) JC9b's F5 fault (`Logger` and `LogRecord` calling `ResourceBundle.getBundle(String, Locale,
  ClassLoader)`, which jrt's overlay had not) is fixed on batch 1 (`b8a1951ce`, the overload in jrt's overlay):
  the joint javac check of batch 1's whole list (2,215 files) has 0 errors, and with the overload removed it fails
  with exactly F5's two errors. The check is now `.tmp/all9-run.sh CHECKOUT` (JCS165).
- (F10a's day) `bin/jrt build` on batch 1 fails at `go build ./...` in `arbace/expat` (Python's crop: `Ptr16`
  passed where `Ptr` is declared, xmltok.go:224 and :869-876): the module bin/jrt prints holds every package of
  `go/`, the crops' among them; jrt's own package builds, vets and tests (`go build ./jrt` in `.tmp/jrt-go/mod`).
  Either the crops' packages leave bin/jrt's module (they are out of every key, `arbace_crop_paths`) or expat is
  fixed on its branch; until then `bin/jrt build` and `bin/jrt test` report failure for a reason not jrt's.
- (F10a's day) `bin/jrt build`'s print on stage 0 peaked at 3.9 GB without a limit; with `GOMEMLIMIT=1400MiB`
  1.45 GB and the same output: the light rules' runs of it should set the limit.


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

JCS240-JCS243 are serialization's slice 4 and JCS169-JCS170's (at the end, "Proposed (serialization slice 4, JCS169-JCS170)",
branch `java-serial4`; JCS168-JCS171 decided by the user 2026-11-08, "Decided (the user, 2026-11-08: JCS168-JCS171)");
JCS190-JCS193 are JCS167's slices 2 and 3 (at the end, "Proposed (JCS167 slices 2-3)", branch `java-serial2`; JCS190
the DECISION on JCS168's switch);
JCS168-JCS171 are JCS167's first slice's (at the end, "Proposed (JCS167)", branch `java-serial`);
JCS162-JCS167 are F10a's and the crop's 90% (at the end, "Proposed (F10a)", branch `java-complete`);
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

## As built: F10a (write-ahead)

Branch `java-complete` (from `batch1`, `0edacad62`), 2026-11-07, by day under the light rules: F10's first part,
the triage's rank 13 and the first not built after F13 (F16, F8, F7 and F11 are JC9b's, F12, F6, F13 and F19
JC9c's, F3 JC9's TLS): **selectors, socket and server socket channels, pipes and the sockets' adaptors**. The
closure's list, jrt's own Java, the variants, the Go natives with their tests, light checks only; nothing
converted, translated or built into an executable. DatagramChannel, the asynchronous channels and the UNIX family
are F10b (JCS163, JCS164).

**The shape.** jdk26u's channels are plain Java (`SocketChannelImpl`, `ServerSocketChannelImpl`, `SelectorImpl`,
`SelectionKeyImpl`, `EPollSelectorImpl`, the pipes' `SourceChannelImpl` and `SinkChannelImpl`, the adaptors) over
a thin native layer in `sun.nio.ch`: `Net`, `IOUtil`, `SocketDispatcher`, `UnixDispatcher`, `NativeThread`, `EPoll`,
`EventFD`, `PollSelectorImpl`'s poll. F10a keeps the Java **unchanged** and gives jrt that layer, rule for rule:
the overlays keep jdk26u's classes, signatures and Java (Net is jdk26u's file with bodies for its natives), and
their natives are `jdk.internal.jrt.HostChannels`' on the descriptor's number, Go ports of libnio's C
(`src/java.base/unix/native/libnio/ch/{Net,IOUtil,SocketDispatcher,UnixDispatcher,PollSelectorImpl}.c`,
`linux/native/libnio/ch/{EPoll,EventFD}.c`, libnet's `NET_SetSockOpt`, `NET_GetSockOpt`, `NET_Bind`): the same
system calls with the same flags, options, conversions, results (IOStatus's) and exceptions (Net.c's
`handleSocketError` choosing ConnectException, BindException, NoRouteToHostException, ProtocolException or
SocketException by the error number; SocketDispatcher.c's ConnectionResetException; convertReturnVal's
IOException). This is unlike jrt's sockets (`HostSocketImpl` over Go's `net`, JC6) and F7's file channel (jrt's
own `FileChannelImpl`): a channel's state machine, its locks, its blocking and non-blocking paths and its close
protocol are jdk26u's own code.

**Two departures, below the natives' interface** (doc/go/JRT-NOTES.md's "Channels", to be written with the
night's records): every descriptor is non-blocking in the kernel and registered with Go's poller (an `os.File`); a
descriptor the channel configured blocking (`IOUtil.configureBlocking`) waits there for what the kernel would
have waited for (a read for data, a write for every byte, a connect for its outcome, an accept for a
connection), so that `UnixDispatcher.preClose` can wake a thread blocked in it: jdk26u dup2's a closed socket over
the descriptor and signals the thread (`NativeThread.signal`, SIGIO's EINTR); jrt marks the descriptor (it then
reads its end, fails a write with EPIPE and polls POLLHUP, as the dup2'd socket) and moves its deadlines to the
past, which wakes the waiter. `NativeThread.current()` names every thread (never 0, never the virtual marker), so
the channels' close always takes the pre-close path; `Poller` (virtual threads' parking on epoll in jdk26u) waits
in Go's poller as a platform thread does, a virtual thread being a goroutine like any other. And jrt has no native
memory: `IOUtil` reads into and writes from a buffer's array (a temporary one for a read-only buffer), a scatter
or gather as one read or write of a temporary array (one system call, as readv/writev), `NativeDispatcher` the
byte[] methods, `EPoll`'s and `PollSelectorImpl`'s event arrays int arrays.

**What comes in, and how** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `selector-sources`, one line in
`sources`):

| part | how |
|---|---|
| java.nio.channels (22) | jdk26u's: `SelectableChannel`, `SelectionKey`, `Selector`, `SocketChannel`, `ServerSocketChannel`, `Pipe`, `NetworkChannel`, the eleven exceptions (`AlreadyBound`, `AlreadyConnected`, `CancelledKey`, `ClosedSelector`, `ConnectionPending`, `IllegalSelector`, `NoConnectionPending`, `NotYetBound`, `NotYetConnected`, `UnresolvedAddress`, `UnsupportedAddressType`), `spi/AbstractSelectableChannel`, `AbstractSelectionKey`, `AbstractSelector`, `SelectorProvider` (variant) |
| sun.nio.ch (18 + 3 linux) | jdk26u's: `SelectorImpl`, `SelectionKeyImpl`, `SelChImpl`, `SelectorProviderImpl`, `SocketChannelImpl` (variant), `ServerSocketChannelImpl`, `SocketAdaptor`, `ServerSocketAdaptor`, `SocketInputStream`, `SocketOutputStream`, `DummySocketImpl`, `ExtendedSocketOption`, `OptionKey`, `IOStatus`; KIND unix `PipeImpl`, `SourceChannelImpl`, `SinkChannelImpl`, `PollSelectorProvider` (variant); **KIND linux** (new: `print-sources`, `bin/jrt-convert`'s tree and origins, the originals' check, the round trip's origins) `DefaultSelectorProvider`, `EPollSelectorImpl`, `EPollSelectorProvider` (variant): Linux's default selector is jdk26u's epoll selector, the poll selector beside it as on Linux (`-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider`, which tests use) |
| beside them (4) | `sun.net.ConnectionResetException`, `sun.net.ext.ExtendedSocketOptions` (jdk.net absent: `NoExtendedSocketOptions`, as the JDK without the module), `jdk.internal.event.SocketReadEvent`, `SocketWriteEvent` (JFR's no-op events) |
| the native layer (15 overlays) | `jdk.internal.jrt.HostChannels` (the natives, 31); `Net` (derived: jdk26u's file, each native's declaration kept with a body over HostChannels; multicast, DatagramChannelImpl's, refuses until F10b; `discardOOB` Windows' only, as on Linux); `IOUtil`, `NativeDispatcher`, `SocketDispatcher`, `UnixDispatcher`, `NativeThread`, `Poller`, `Util` (ungrowableSet; heap temporary buffers), `UnixDomainSockets` (`isSupported()` false: F9's FileSystems, JCS164), `EventFD`, `FileDispatcherImpl` (closeIntFD alone; jrt's file channel is its own, F7), `EPoll` (an event array's "address" a token naming int arrays, so EPollSelectorImpl stays jdk26u's), `PollSelectorImpl` (derived: the pollfd array as int arrays), `SocketOptionRegistry` (derived from jdk26u's template with Linux's constants, below) |
| variants (4) | `SelectorProvider$Holder` (no ServiceLoader: the property's class, else `DefaultSelectorProvider`; 1 replaced 1 cut), `SocketChannelImpl` (tryRead, tryWrite: the adaptor's streams through the byte[] dispatcher, not a direct buffer's address; 2 replaced), `PollSelectorProvider` and `EPollSelectorProvider` (inheritedChannel null: jrt's standard input is never an inetd socket, InheritedChannel's answer then; 1 replaced each) |
| j2g | the VarHandle table (`arbace.j2g.vh`) takes `ConstantBootstraps.fieldVarHandle(lookup, name, VarHandle.class, C.class, T.class)` (SelectionKeyImpl's INTERESTOPS, beside MhUtil.findVarHandle's ATTACHMENT, INVALID, CLOSED); rename `sun/nio/ch/Util` → `Ch_Util` (F13's `sun.management.Util`) |
| Go | `go/arbace/jrt/channels_linux.ae` (the natives, about 800 lines of forms), `channels_other.ae` (every native "Channels not available" off Linux: TamaGo's, the box's), `channels_test.ae` (TestChannelSockets, TestChannelPipeAndPoll, TestChannelEpoll) |

**The derivations** (scratch scripts in the worktree's `.tmp/f10/`, to be kept with its scratch; JCS166 proposes
them as committed Arbace): `gennet.py` (jdk26u's `Net.java`: every `native` declaration given the body of a table,
asserting each native has one and none is left; the helpers `scope`, `inetAddress`, `sockAddress`, `sockPort`,
`multicast` before `shouldNotReachHere`; `import jdk.internal.jrt.HostChannels`), `genpoll.py` (jdk26u's unix
`PollSelectorImpl.java`: 14 exact replacements, each asserted once: the AllocatedNativeObject and its Unsafe copy
as three int arrays and `Arrays.copyOf`, Blocker's begin and end dropped, `poll` and `closeIntFD` over
HostChannels), `genopts.py` (GensrcMisc.gmk's `generate-preproc-src` without the C preprocessor: the copyright, the
text after `@@START_HERE@@`, `PREFIX_` removed, `#` lines dropped, the 18 C constants by Linux's generic UAPI
values; checked against the build's gensrc: the same 17 (option, family, level, name) tuples).

**Light checks done** (stage 0 main's `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-a3300f566f6c87d40d06`,
`REPR_JAVA_STALE_OK=1`, `GOMEMLIMIT=1400MiB`, each process under 1.5 GiB; scratch `.tmp/f10/`):
- the closure (F1's `closure.py`, the world being batch 1's whole `print-sources` list, JC9's and JC9b's
  included; scope `java.nio.channels`, `.spi`, `sun.nio.ch`, `sun.net.ext`, `jdk.internal.event`): the 44 jdk26u
  files and 3 linux ones above, 10,333 lines; outside the world and the list, each accounted for: the VarHandle
  machinery (compiled by j2g), DatagramChannel and DatagramChannelImpl (cut: SelectorProviderImpl's
  openDatagramChannel answers by an operation-level stub), InheritedChannel (the variants), DirectBuffer (the
  variant), UnixDomainSocketAddress (a cut class: the UNIX family is refused before it is reached), ServiceLoader
  (the variant), the native-memory classes the overlays replace;
- `print-sources` of the branch: 2,277 lines, batch 1's 2,215 plus 62 (40 share, 4 unix, 3 linux, 15 overlay), no
  line twice;
- **the joint javac check** (JC9b's `all9/run.sh`, made general: `.tmp/all9-run.sh CHECKOUT`, below): all 2,277
  files compiled by the JDK's javac as java.base's patch against jrt's overlays: **0 errors**, 4,942 classes (13.6 s,
  1.0 GB); run on batch 1 alone first: 2,215 files, 0 errors (F5's `getBundle` fault, JC9b's finding, is fixed on
  batch 1: `ResourceBundle.getBundle(String, Locale, ClassLoader)` in jrt's overlay since `b8a1951ce`); and the
  check's negative control: the overlay's overload renamed, the check fails with F5's two errors (`Logger.java:2191`,
  `LogRecord.java:716`) and exits 1;
- the read: `bin/repr-java read-jdk --check` of the 62 files with HostFiles and FileDescriptor (which they use) as a
  patch of java.base: 64 converted, 0 failed, 0 javac errors, 0 unreadable (4.0 s, 0.6 GB); `--no-javac`: 64
  native, 0 fallbacks, the forms identical;
- the variants applied and parsed (F4's `vet.ae`): SocketChannelImpl 2/0/0, PollSelectorProvider 1/0/0,
  EPollSelectorProvider 1/0/0, SelectorProvider$Holder 1/1/0;
- Go names: the 73 classes of the new forms (nested ones included) against the gate executable's generated types,
  jrt's hand-written ones and batch 1's list: one collision, `Util`, renamed;
- the natives' Go names: the 31 natives of HostChannels mangled as j2g names them, each defined in both Go files;
- jrt's own build (`bin/jrt build --arch amd64` prints the module; its `go build ./...` stops at `arbace/expat`, a
  crop's package and not jrt's: Findings), then `go build` and `go vet` of `./jrt` for linux/amd64 and arm64, and
  `go test ./jrt` on amd64: every test passes, the three new ones among them (a blocking connection through
  connect, accept, write and read with the peer's address; a non-blocking read's UNAVAILABLE; poll's timeout and
  readiness; SO_LINGER and SO_RCVBUF with Linux's conversion; a read blocked in Go's poller woken by preClose to its
  end of stream, a write after it failing; a refused non-blocking connect reported by pollConnect with
  ECONNREFUSED; a pipe's poll(2) in the selector's shape, write1, drain1 and drain; an epoll instance with an
  eventfd and a pipe: the timeout, both reported with their descriptors, the eventfd drained, a removal).

**JCS135, the closure's growth**: +13,106 Java lines (10,333 jdk26u's, 2,773 jrt's overlays): **+2.0%** on the
closure after JC9 and JC9b (about 655,000); the forms +248,759 bytes (+2.2% on `jdk/forms`' 11,161,667, before JC9's
growth); by F4's ratio about +2.5 MB of executable.

**Expected yield.** The triage's F10 counted 81 / 82 tests touched, 56 / 55 alone; with F7, F11 and F13 built
(their causes no longer counted), 80 / 80 tests have only built causes besides the selectors, of which 31 / 31 use
DatagramChannel, an asynchronous channel, a UNIX domain address or a multicast channel (F10b) and **49 / 49 are
F10a's** (Pipe 5, SelectionKey 2, Selector 24, ServerSocketChannel 2, SocketChannel 11, etc/Shadow, sun/nio/ch's
Basic, SelProvider, ServerSocketAdaptorTest, TempBuffer, and CompilationMXBean/Basic). At risk: the interrupt
tests (`LotsOfInterrupts`, `PipeInterrupt`: jrt's Thread has no blocker, so `Thread.interrupt` does not wake a
selector or a blocked channel: JCS162), `WakeupSpeed` and the stress loops at the interpreted level (speed),
`OutOfBand`/`SendUrgentData` (urgent data through the kernel's, as the JDK's), the tests reading sun.nio.ch's
internals at the REPL (JCS136's visibility), `TempBuffer` (jdk26u's temporary-buffer cache, which jrt's `Util` has
not). About **34 / 34 expected** (0.7 × 49): Java eval and AOT each about +1.0 point, the crop's completeness about
+0.5. `SelProvider` expects `sun.nio.ch.EPollSelectorProvider` on Linux: it is.

**The night's commands** (after JC9's and JC9b's conversions, in the night's worktree with this branch merged;
heavy ones under `bin/heavy-lock`; about 2 hours with the area's checks, the conversion shared with the night's):

```sh
# 0. the joint javac check of the merged tree (0 errors expected; a light minute)
.tmp/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: expect the sources line +40 share, +4 unix, +3 linux ("N from src/java.base/linux/classes",
#    new in the line), +15 of jrt's own; 0 failed, 0 javac errors, no javac: 0 fallbacks; jdk/forms + 62 files and the
#    package index of sun/nio/ch grown (java/nio/channels, .spi exist since F7)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (DatagramChannelImpl, InheritedChannel, DirectBuffer, UnixDomainSocketAddress as outside
#    references), javac's world's edge
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: SelectorProvider$Holder 1/1/0, SocketChannelImpl 2/0/0, PollSelectorProvider and
#    EPollSelectorProvider 1/0/0; no "VarHandle not constant" (SelectionKeyImpl's INTERESTOPS by fieldVarHandle); no
#    native without its Go function (HostChannels' 31)
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F10a: expect "1 true", "2 hi", "3", "sun.nio.ch.EPollSelectorProvider"
target/arbace-go/amd64/arbace -e '(import (quote (java.nio ByteBuffer)) (quote (java.nio.channels Selector SelectionKey ServerSocketChannel SocketChannel Pipe)) (quote (java.nio.channels.spi SelectorProvider)) (quote (java.net InetSocketAddress InetAddress)))
  (let [sel (Selector/open) ss (ServerSocketChannel/open)]
    (.bind ss (InetSocketAddress. (InetAddress/getLoopbackAddress) 0))
    (.configureBlocking ss false) (.register ss sel SelectionKey/OP_ACCEPT)
    (let [c (SocketChannel/open (.getLocalAddress ss))]
      (println (.select sel 2000) (.isAcceptable (first (.selectedKeys sel))))
      (let [a (.accept ss) b (ByteBuffer/allocate 8)]
        (.write c (ByteBuffer/wrap (.getBytes "hi"))) (.read a b)
        (println (.position b) (String. (.array b) 0 (.position b))) (.close a))
      (.close c))
    (let [p (Pipe/open) b (ByteBuffer/allocate 4)]
      (.write (.sink p) (ByteBuffer/wrap (byte-array [1 2 3]))) (println (.read (.source p) b)))
    (println (.getName (class (SelectorProvider/provider))))
    (.close sel) (.close ss))'
# 5. Java's levels interp and compiled on F10a's area, then the whole corpus with the night's others: saved, not
#    recorded; expect about 34 / 34 of the 49 / 49 to pass, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(java/nio/channels/(Pipe|SelectionKey|Selector|ServerSocketChannel|SocketChannel|etc)|sun/nio/ch|java/lang/management/CompilationMXBean)' \
  --save .tmp/f10a-night/check
```

None of the slice's tests runs at `--only` interp before the night: every one names a class the committed
`jdk/forms` and the gate's executable do not have yet (`Selector`, `SocketChannel`, `Pipe`...), so their first run
is step 5.

**Risks and fallbacks**: (1) the blocking emulation: a descriptor shared by two channels (`SocketChannel.socket()`
and its adaptor's streams) is one entry, one mode, as the kernel's; (2) `os.File`'s finalizer would close a
descriptor whose entry was dropped: entries are dropped only by `close0`, which closes; (3) the selector's
`epoll_wait` and `poll` block an OS thread (Go's syscall), as the JDK's block a platform thread; the wakeup is
jdk26u's (eventfd, the pipe); (4) a test that sets `jdk.net`'s extended options finds none, as on a JDK without
jdk.net; (5) should j2g refuse a member of the new forms, the night's fallback leaves `selector-sources` out
(its line in `sources`, the overlays of sun/nio/ch but FileChannelImpl, the four variants, the rename and
HostChannels): F7, F11 and the rest do not depend on it.

**What is left of F10**: F10b (JCS163: DatagramChannel over Net's multicast natives, then DatagramSocket's NIO
adaptor as jdk26u's default delegate, and the asynchronous channels over EPollPort), the UNIX family with F9
(JCS164), the interrupts (JCS162); the night's run and the day's classification and records.

## Proposed (F10a)

Numbered after JCS161; written here at the end with F10a's section (the main "Proposed" above lists the earlier ones).

### JCS162 Thread.interrupt wakes a blocked channel and a selector (F7, F10a)

jdk26u registers a channel's or a selector's `Interruptible` with the thread (`JavaLangAccess.blockedOn`), and
`Thread.interrupt` calls it: the channel closes (ClosedByInterruptException), the selector wakes. jrt's
hand-written Thread has no blocker (F7's AbstractInterruptibleChannel variant: blockedOn does nothing, end checks
the interrupt status). Tests at stake: `Selector/LotsOfInterrupts`, `Pipe/PipeInterrupt`, F7's `ClosedByInterrupt`,
`InterruptDeadlock`, `InterruptMapDeadlock`, about 6. (a) jrt's Thread gains a blocker: a field set by
`blockedOn` (the variant then calls a jrt native `Thread_SetBlocker`), and `Interrupt` calls its `interrupt(Thread)`
through a function the translated `sun.nio.ch.Interruptible` registers at its first use (no hand-written reference to
a translated type), 1 day with jrt's tests; (b) as now. **Recommended: (a)**, after the night's records show the
interrupt tests are what is left.

### JCS163 F10b: DatagramChannel, then the asynchronous channels

F10a leaves 31 / 31 tests of the selectors' area to DatagramChannel (about 20), the asynchronous channels (8) and
multicast channels. (a) DatagramChannel as jdk26u's `DatagramChannelImpl` over F10a's layer: Net's multicast natives
(`joinOrDrop4/6`, `blockOrUnblock4/6`, `setInterface4/6`, `getInterface4/6`: Net.c's, `IP_ADD_MEMBERSHIP`,
`MCAST_JOIN_SOURCE_GROUP`...), `DatagramDispatcher`'s natives (recvfrom, sendto into IOUtil's arrays), then
`DatagramSocketAdaptor` as `DatagramSocket`'s delegate as in jdk26u (retiring F11's legacy delegate, JCS153 (c)'s
second step), 3 days; the asynchronous channels (`UnixAsynchronousSocketChannelImpl` over `EPollPort`, the default
group's threads) after, 3 days; (b) DatagramChannel only; (c) neither. **Recommended: (a)**, DatagramChannel first
(F11's tests then run on the JDK's own default path too).

### JCS164 UNIX domain sockets with F9

`UnixDomainSockets` answers not supported: `UnixDomainSocketAddress.of(Path)` needs the path's FileSystem
(java.nio.file's FileSystems, F9), and the JDK's natives bind and connect by the path's bytes from the default file
system's provider. (a) with F9: jdk26u's UnixDomainSockets and UnixDomainSocketAddress, the natives
(`socket0(AF_UNIX)`, bind, connect, accept, localAddress) in channels_linux.ae, 1 day after F9; (b) never (a few
tests). **Recommended: (a)**.

### JCS165 The joint javac check as a light gate step

JC9b's finding (the fixes' javac checks compiled against the JDK's java.base, not jrt's overlays) has now a general
script, `.tmp/all9-run.sh CHECKOUT` (print-sources' list, jdk26u's files, the build's gensrc, the overlays; exit 1
on any error; its negative control is F5's fault). (a) `bin/jrt-convert check-javac`, the script as a verb, and a
light essential gate step keyed by the closure's sources and overlays (about 15 s, 1 GB; needs only the JDK
build's javac, the oracle tier's host, as `bin/javac-vendor classes`); (b) a step of the nights' scripts only, as
tonight. **Recommended: (a)**: every conversion night would stop at javac errors the day could have seen.

### JCS166 The overlays' derivations committed and checked

F7's `DirectByteBuffer` (gendirect.py), F10a's `Net`, `PollSelectorImpl` and `SocketOptionRegistry` (gennet.py,
genpoll.py, genopts.py) are derived from jdk26u's files by scratch Python scripts; the overlays are committed, the
scripts not, so a new jdk26u revision is re-derived by hand from the doc's description. (a) the derivations as
Arbace (`test/jrt/derive.ae`: the same replacements, each asserted), run by a `bin/jrt-convert derive --check` that
compares the committed overlays byte for byte (as `bin/generated-check` does for the generated variants), 1 day;
(b) the Python scripts committed under `test/jrt/derive/` (a host tool, Python, in the vendoring path: against
independence); (c) as now. **Recommended: (a)**.

### JCS167 Toward 90% strict: serialization out of by design

Java's completeness on batch 1 is 72.9% strict, 80.4% without by design; after tonight's records (JC9's F1, F5,
F4, F2, JC9b's four, JC9c's F6, F12, F13, F19, F3, F10a; the sections' expected yields, about +530 eval and +500
AOT) about **80.6% strict and 89% without by design** (eval about 2,390 of 3,322, AOT about 2,440; read and print
unchanged). The strict mean cannot pass 90% while eval and AOT hold 335 and 308 tests by design (10% of each
cell): the largest class is **serialization, 168 eval and 117 AOT** (`:serialization`, J2G-SPEC §12; the
compiled level's `:design-also-compiled` 139 is mostly serialization's too), then XML 22, the class-file API 14,
class loading 14 / 19. (a) serialization ported (ObjectOutputStream, ObjectInputStream, ObjectStreamClass over
jrt's reflection; clojure-cdr staged its port, CDR's "serialization's port staged"; j2g's SerialNew), its tests
deferred again then regained, 8-10 days; (b) by design as decided. **Recommended: (a)** when 90% strict is the
goal: no other single change moves the strict mean more than a point or two.

## Decided (the user, 2026-11-07: JCS162-JCS167)

The proposals are F10a's (`java-complete`, merged into `batch1` at 23d640f54, after batch 2's base 0edacad6: their text reaches this
file when batch 1's later head and batch 2 meet on `next`). Decided: **JCS162-JCS164 (a)** (jrt's Thread gains a blocker, interrupts
wake channels and selectors; F10b's DatagramChannel then the asynchronous channels; UNIX domain sockets with F9); **JCS165-JCS166 (a)**
(`bin/jrt-convert check-javac` as a light essential gate step; the overlays' derivations as Arbace, `bin/jrt-convert derive --check`);
**JCS167 (a)**: serialization ported (ObjectOutputStream, ObjectInputStream, ObjectStreamClass over jrt's reflection, j2g's SerialNew;
clojure-cdr's staged port), its tests deferred again then regained, toward 90% strict.

## As built: JCS167 (write-ahead)

Branch `java-serial` (from main `2a72c94aa`), 2026-11-08, by day under the light rules: JCS167 (a)'s **first slice**,
Java serialization in the world end to end in code: jdk26u's object streams, their descriptors and filter, the
allocation of a deserialized object without its constructors, reflection's serial members and final fields, for the
three kinds of class a stream meets (translated JDK classes, a program's translated classes at the compiled level,
interpreted classes at the REPL and the interpreted level). Nothing converted, translated or built into an
executable: the night's commands below. CDR10's staging (`clojure-cdr`, doc/go/JRT-NOTES.md, "Serialization": j2g's
`SerialNew`, `variants.py`, `SerialAlloc`, `ClassCache`) is the precedent, taken further and moved into the world.

### The design: a closed world serializes the classes the program names

jdk26u's serialization is plain Java over five things the JVM gives it: reflection over every member, whatever its
access (the serial fields, the private hooks `writeObject`/`readObject`/`readObjectNoData`, `writeReplace`/`readResolve`
of any access, `serialVersionUID`, `serialPersistentFields`); the writing of final fields; an object made without its
constructors (`ReflectionFactory.newConstructorForSerialization`: the first non-serializable superclass's no-argument
constructor run on an object of the class); method handles (records' canonical constructors); `Unsafe` (field offsets,
the fence). The Go build has no run-time class loading and its reflection is the member tables j2g writes ahead of
time (J2G-SPEC §5.11), public members only. The design keeps jdk26u's streams **unchanged** (ObjectOutputStream,
ObjectStreamField, ObjectInputFilter and the rest are jdk26u's files; ObjectStreamClass and ObjectInputStream are
variants by a replayable script, `test/jrt/serialization/variants.py`, of a few members each) and gives them what the
JVM gives, decided ahead of time where the closed world allows and at run time where the REPL makes classes:

| the JVM's | the Go build's (this slice) |
|---|---|
| reflection over a Serializable class's every member | j2g's member table of a Serializable class (`m/serialization?` and `m/serializable?`) lists, beside the public members, its serialization hooks (`m/serial-hooks`: the three private ones, private and not static; `writeReplace`, `readResolve` of any access) and its serial fields (`m/serial-field?`: every instance field, `serialVersionUID`, `serialPersistentFields`), whatever their access (`out/member-tables`); a program's own classes (the compiled level, `nm/own-class?`) list every member already (JCS117 (a)); an interpreted class publishes every member (`arbace.classes.interp/publish!`) |
| the hooks are called by reflection only | RTA reaches them: an instantiated Serializable class reaches the hooks its superclass chain declares and its first non-serializable superclass's no-argument constructor (`reach/serial-reach!`, in `instantiate-st`); Externalizable's `readExternal`/`writeExternal` are virtual calls of the streams' code, its public no-argument constructor a root of the REPL's world |
| Unsafe's writes of final fields | jrt's `Field.set` follows the JDK's rule (`Field.finalWritable`): a final field is written when `setAccessible(true)` succeeded, it is not static and its class is not a record; j2g gives every final instance field a `Set` (line `F`, the modifiers keep `final`); the variant's field reflector reads and writes by `java.lang.reflect.Field` and jrt's `Field` gains the sixteen typed accessors (`getInt`, `setLong`...: widening as the JDK's, its messages) |
| `newConstructorForSerialization` | `jdk.internal.jrt.SerialAlloc` (jrt's Java) over `ClassInfo.SerialNew` (`serial.ae`'s natives), the rule decided by whoever knows the class: **j2g** for a translated class (`m/serial-super`, jdk26u's rule entire: `superHasAccessibleConstructor` up the chain by Java package, nestmates by their top-level class, the no-argument constructor not private and public, protected or of the package; for an instantiated class only, whose constructor RTA reached; `C_SerialNew` its zero struct with S's constructor body run on it, CDR10's form); **the interpreter** for an interpreted class (`Compiler$CF$Rt.serialPlan`: the same rule over the interpreter's classes, the plan a `Meth` or the nearest class of the world; `serialNew`: the class initialized, allocated with zero fields, the plan run; `arbace.classes.interp/serial-new!` hands it to the host, `CFGo.setSerialNew`), and for the world's part of an interpreted class's chain **j2g again**, per DynSub class (`j2g_cf.go`'s `cfSerialFor`: from another package, the DynSub class public or protected with a public or protected constructor, then its own no-argument constructor when it is not Serializable, else its chain's `serial-chain-super`, run on the DynSub object) |
| method handles of records | the canonical `Constructor` called with each component's stream value (`RecordSupport.Ctr`; its exception unwrapped from `InvocationTargetException`, as `invokeExact` throws it); a record's components are its instance fields in declaration order (`recordComponentFields`: `RecordComponent` is not in the world, JLS 8.10.3); an interpreted record is `KindRecord` (`Klass.record`, the host's kind 3), so `Class.isRecord` holds at the REPL as on the JVM |
| `ClassValue`, soft references | jrt's `ClassCache`: a `ConcurrentHashMap` (the closed world unloads no class) |
| `SharedSecrets`' serialization accessors (`HashMap`, `ArrayList`, `Hashtable`... call `getJavaObjectInputStreamAccess().checkArray` in `readObject`) | written by j2g in the program's support code (`m/serial-accessors`, `m/j2g-static?`): a variable per access interface (translated: jdk26u's three interfaces), its setter, its getter initializing `ObjectInputStream` or `ObjectInputFilter$Config` first (jdk26u's `ensureClassInitialized`): jrt's hand-written `SharedSecrets` cannot name the translated interfaces |
| `JavaLangAccess` | jrt's (`countPositives`, `inflateBytesToChars`, `countNonZeroAscii`): the streams keep jdk26u's calls |
| `Proxy.isProxyClass`, `getProxyClass` | false; `UnsupportedOperationException` as `newProxyInstance` (no class is made at run time) |
| `VM.latestUserDefinedLoader` | the application loader, every class's of the world and the REPL's: `resolveClass`'s `Class.forName` finds a translated class, an interpreted one (`DefineDynamic`'s registry) and an array class |
| JFR's `DeserializationEvent`, `SerializationMisdeclarationEvent`; `Unsafe.storeFence` | not raised; no fence (Go's memory model publishes the graph through the synchronization that hands it over) |
| `ObjectInputFilter` (`jdk.serialFilter`, the factory) | jdk26u's own, unchanged: its `Config` reads the system and security properties as on the JVM |

**What comes in** (`test/jrt/jrt_sources.ae`'s `serial-sources`, one line in `sources`): jdk26u's `java/io/`
`Externalizable`, `ObjectInput`, `ObjectInputFilter`, `ObjectOutput`, `ObjectOutputStream`, `ObjectStreamConstants`,
`ObjectStreamField`, `SerialCallbackContext`, `SerializablePermission` and `jdk/internal/access/`
`JavaObjectInputFilterAccess`, `JavaObjectInputStreamAccess`, `JavaObjectInputStreamReadString` (12 files); jrt's overlays
`java/io/ObjectStreamClass.java`, `ObjectInputStream.java` (the variants), `ClassCache.java` and
`jdk/internal/jrt/SerialAlloc.java`. `java.io.Serial` (an annotation, not kept) and `SerializationMisdeclarationChecker`
(JFR's, cut by the variant) stay out. jrt: `go/arbace/jrt/serial.ae` (moved from CDR10's staging), `ClassInfo.SerialNew`,
`Field`'s accessors and final rule (`reflect.ae`), the manifest and jrt's own tables regenerated (`bin/jrt manifest`).
j2g: `model.ae` (`serialization?`, `serializable?`, `serial-hooks`, `serial-hook?`, `serial-field?`, `serial-chain-super`,
`serial-super`, `foreign-super-accessible?`, `serial-accessors`), `reach.ae` (`serial-reach!`), `out.ae` (the tables, the
accessors), `dyncf.ae` (`cfSerialFor`, three natives, kind 3), `main.ae` (`:inst` in the world). The runtime:
`Compiler.ae`'s `CF$Rt.serialPlan`, `superCtorAccessible`, `packageOf`, `serialNew`, `Host`'s and `GoHost`'s
`setSerialNew`, `serialSuper`, `serialInit`, `Klass.record`; `arbace.classes.interp/serial-new!`. The classes:
`test/java/divergences.edn`'s `:serialization` deferred (`:world`, JCS167) rather than by design, as decided ("its tests
deferred again then regained"): `bin/reap --record` and `bin/crops-score --record` move Java's eval by design 335 -> 167
and AOT 308 -> 191 into deferred (strict 72.85% unchanged; without by design 80.36% -> 78.93% until the nights regain them).

**Decided by the doc's earlier decisions, not proposed**: the variants by a replayable script like F7's and F10a's
derivations (JCS166 (a) ports them all to Arbace, `bin/jrt-convert derive`); records without `RecordComponent`
(`Class.getRecordComponents` is a reflection feature of its own, below); the filter jdk26u's own; proxies refused as
J2G-SPEC §12 refuses them (JCS170 proposes them).

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`, `GOMEMLIMIT=1400MiB`; scratch `.tmp/js/`)
- **the joint javac check** (`.tmp/night7/all9-run.sh`, F10a's general script): 2,293 files (2,277 + 12 share + 4
  overlay), 0 errors, 5,002 classes (16 s, 1.0 GB); its control (the variant's `recordComponentFields` call renamed):
  1 error, exit 1;
- the closure scan (F1's `closure.py`, the world being this branch's `print-sources`): every class the four overlays and
  five of the new sources name is in the world (the scan of jdk26u's own files had found `RecordComponent`, `ClassValue`,
  `ReflectionFactory`, the method handles and JFR's events, which the variants replace); the members they need that jrt
  lacked are added (`Proxy.isProxyClass`, `getProxyClass`, `VM.latestUserDefinedLoader`, `Field`'s typed accessors);
- jrt: `bin/jrt build --arch amd64` (its `go build ./...` stops at `arbace/expat`, the Python crop's, as on F10a's day),
  `go vet ./jrt`, and `go test ./jrt`: every test passes, `TestReflectInvoke`'s six new cases among them (a final field
  written once made accessible, refused otherwise; `getLong` widening an int; `getInt` of a double refused with the JDK's
  message; `setInt` widening to double), `TestReflectAgainstJVM` with `bin/jrt testdata` afresh (the JVM's `Field`
  members); `bin/jrt manifest` (12.9 s, 0.4 GB): 16 + 2 members added, "edge members of these classes jrt does not
  define: 0";
- j2g's namespaces load on stage 0 from the checkout (model, reach, out, dyncf, decls, main);
  `arbace.classes.interp` loads up to its first call of the new `CF$Rt.serialPlan`, which stage 0's runtime has not (the
  night's build has it);
- `bin/reap --record`, `bin/crops-score --record`, then `bin/crops-score --judge --strict`: passes.

Not checked by day (no build): the Java forms of `Compiler.ae`'s new methods (their analysis is the build's), j2g's
output for the new tables, `cfSerialFor` and the accessors (the night's j2g run; `go build` of its output).

### Expected yield

The class's tests (`.tmp/js/ser-interp.txt`: the records' non-passes matching its pattern, 167 interpreted, 136 compiled),
scanned for what this slice leaves out (proxies 11, records 4, serializable lambdas 9, RMI 8, the security manager 14,
filters 3, class loaders 18, JDK-written streams read from files 57, jrt's hand-written serializable classes):
**55 tests need none of it** (`t04245 t04310 t04375 t04377 t04378 t04379 t04380 t04381 t04382 t04383 t04389 t04390
t04393 t04394 t04395 t04396 t04397 t04398 t04399 t04400 t04401 t04402 t04403 t04404 t04407 t04409 t04410 t04412 t04413
t04414 t04416 t04417 t04429 t04431 t04434 t04436 t04437 t04438 t04439 t04441 t04442 t04445 t04446 t04448 t04450 t04452
t04453 t05325 t05543 t05547 t05549 t05550 t05560 t05666 t08306`; 53 of them in the compiled level's list too), and **19
more** serialize jdk26u's collections and numbers through their own hooks (`t04392 t04443 t04451 t04905 t05018 t05435
t05449 t05494 t05495 t05499 t05512 t05514 t05518 t05520 t05521 t05556 t05565 t05671 t05672`: EnumSet's and EnumMap's
proxies, Vector, Collections' singletons, BigDecimal, URI). About **0.6 of the 74, 45 interpreted and 40 compiled**:
Java eval and AOT each about +1.4 points, the crop's strict completeness about +0.7. At risk: tests printing a
descriptor's default `serialVersionUID` of a translated JDK class without one (its table lists only the serial members:
JCS168), `ExpectedStackTrace` (Go's frames), the tests whose classes extend a class outside `class-supers` at the
interpreted level ("a class extending ... is not in the Go build"), a JDK class with a variant that cut its serialization
members (J2G-SPEC §12's operation-level stubs: LogRecord's `serialPersistentFields`, KeepAliveCache, SerialBlob...: slice
4), and every stream holding an exception (Throwable is jrt's hand-written class: slice 2).

### The night's commands

In the night's worktree with this branch merged, after the night's other conversions; heavy ones under
`bin/heavy-lock` (memory as F10a's: the conversion and the build are the night's heavy steps, each under the lock; the
checks below about 1 GB each):

```sh
# 0. the joint javac check of the merged tree (0 errors expected; 30 s, 1 GB)
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: expect the sources line +12 share and +4 of jrt's own (overlay), 0 failed, 0 javac
#    errors, no javac: 0 fallbacks; jdk/forms + 16 files (java/io's 13, jdk/internal/access's 3), jdk/internal/jrt +1
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (VM.latestUserDefinedLoader and Proxy's members as outside references; the new classes'), then
#    javac's world's edge
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: no "native without its Go function" (SerialAlloc's 2, ObjectStreamClass's 2,
#    Compiler$CFGo's setSerialNew, serialSuper, serialInit), no "not in the declared edge" for java.io; the executable's
#    size against the night before's (`ls -l`: serialization's reach brings every instantiated Serializable class's hooks,
#    JCS171)
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of the slice (translated JDK classes through their hooks, an enum, EnumSet's serialization proxy):
#    expect "[1 2 3] {a 1} 42 [x y] SECONDS true RegularEnumSet [SECONDS DAYS]"
target/arbace-go/amd64/arbace -e '(import (quote (java.io ByteArrayOutputStream ObjectOutputStream ObjectInputStream ByteArrayInputStream)) (quote (java.util ArrayList HashMap UUID EnumSet)) (quote (java.util.concurrent TimeUnit)))
  (defn rt [x] (let [b (ByteArrayOutputStream.) o (ObjectOutputStream. b)] (.writeObject o x) (.close o)
                 (.readObject (ObjectInputStream. (ByteArrayInputStream. (.toByteArray b))))))
  (let [s (rt (EnumSet/of TimeUnit/SECONDS TimeUnit/DAYS))]
    (println (rt (ArrayList. [1 2 3])) (rt (doto (HashMap.) (.put "a" 1))) (rt (int 42)) (vec (rt (into-array String ["x" "y"])))
             (rt TimeUnit/SECONDS) (= (rt (UUID. 1 2)) (UUID. 1 2)) (.getSimpleName (class s)) (vec (map str (sort s)))))'
# 5. Java's levels interp and compiled on the class's tests: saved, not recorded; expect about 45 / 40 of the 74 above
#    to pass, no passing test lost; then the whole corpus with the night's others
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/io/(Serializable|Externalizable|ObjectInputStream|InputStream/ReadParams|OutputStream/WriteParams)|jdk/java/util/(HashSet|Hashtable|Vector|EnumMap|EnumSet|LinkedHash|UUID|BitSet/StickySize|Arrays/ArrayObjectMethods|List/LockStep|Collections/(Ser|ReverseOrder))|jdk/java/math/BigDecimal/SerializationTests|jdk/java/net/URI/Test|jdk/java/security/SignedObject|jdk/sun/util/calendar/Bug6653944' \
  --save .tmp/jcs167-night/check
# 6. the day after: the new failures' classes (narrower than :serialization: proxies, jrt's hand-written classes, the
#    default serialVersionUID, ...), the records, bin/reap --record, bin/crops-score --record
```

**Risks and fallbacks**: (1) a JDK class's `readObject` newly translated needs something outside the world: j2g stubs it
(operation-level, reported), its tests fail in a class; (2) the executable's growth (every instantiated Serializable
class's hooks and their callees): measured at step 3, JCS171; (3) `Compiler.ae`'s new Java forms fail the build's
analysis: the night's fallback reverts `arbace/lang/Compiler.ae`, `arbace/classes/interp.ae` and dyncf's three natives
(the interpreted level then has no allocation: `canAllocate` false, "no valid constructor"), the rest stands; (4) should
the conversion refuse a new file, `serial-sources` and the four overlays leave together (j2g's rules are inert without
`java/io/ObjectStreamClass`: `m/serialization?`).

### The remaining slices (JCS167's 8-10 days, this one about 2)

| slice | what | days | tests |
|---|---|---|---|
| 2 | jrt's hand-written serializable classes: `Throwable` (`detailMessage`, `cause`, `stackTrace`, `suppressedExceptions`, its `writeObject`/`readObject`) with `StackTraceElement`, `Date`, `StringBuilder`/`StringBuffer` (`serialPersistentFields`), the atomics, the locks: their serial fields and hooks in jrt's own tables (`test/jrt/tables.ae`), their `SerialNew` | 2 | every stream holding an exception (Throwable/*, the 6 "throwable?" ones, most of RMI's later), Date's |
| 3 | the default `serialVersionUID` exact (JCS168): j2g computes it ahead of time for a translated class without one; interpreted classes' `getDeclaredMethods` without the inherited virtual methods and their synthetic `this$0` and captured fields; then the 57 tests reading JDK-written streams (`.ser` files, `test.src`) | 1.5 | about 25 |
| 4 | the JDK variants that cut serialization members (J2G-SPEC §12's stubs: `LogRecord`, `KeepAliveCache`, `SerialBlob`/`SerialClob`, the security classes' `readObject`s F2-F4 left out) restored | 1 | logging 4, security 6, sql |
| 5 | `Class.getRecordComponents` and `RecordComponent` (jrt's, from j2g's record facts and the interpreter's), records' tests | 0.5 | 4 + reflection's |
| 6 | proxies (JCS170) | 2 | 11 |
| 7 | serializable lambdas (JCS169): `SerializedLambda`, `$deserializeLambda$` | 1.5 | 9 |
| 8 | Clojure's own types (CDR10's remainder: `arbace.lang`'s hooks, a defrecord's `Dyn` allocation) | 1 | the suite's `serialization` 9 |

## Proposed (JCS167)

Numbered after JCS167 (renumbered at the merge should another branch have taken these numbers).

### JCS168 The default serialVersionUID of a translated class

`ObjectStreamClass.computeDefaultSUID` hashes the class's name, modifiers, interfaces, its non-private fields, its static
initializer's presence, its non-private constructors and methods (SHA-1). A translated JDK class's table lists only its
public and serial members, so a class without a declared `serialVersionUID` gets another value than the JDK's: harmless
within one program, wrong against a JDK-written stream (and for the tests that print it). (a) j2g computes it ahead of
time from the declaration (the JDK's algorithm over `a/decl`: flags, members, `<clinit>` as javac emits it) into
`ClassInfo.SerialUID`, which the variant's `computeDefaultSUID` asks first (`SerialAlloc.defaultSUID`), 1 day with a
check against the JDK's values for the world's Serializable classes (recorded on the frozen JVM, `bin/j2g-edge`'s way);
(b) list every member of every Serializable class (the tables grow, about +1 MB); (c) as now. **Recommended: (a)**.

### JCS169 Serializable lambdas

javac compiles a serializable lambda with `altMetafactory`'s `FLAG_SERIALIZABLE` and a `$deserializeLambda$` in the
capturing class; j2g refuses both (`:deser-indy`, J2G-SPEC §12). (a) j2g's lambda adapter of a serializable lambda
writes `writeReplace` returning a `SerializedLambda` (its capturing class, interface method, implementation and captured
args, as `InnerClassLambdaMetafactory` does), and translates `$deserializeLambda$` (javac's own switch over the
`SerializedLambda`'s fields): 1.5 days; (b) by design (a `:design` class of its own). **Recommended: (a)** after slices
2-4 (9 tests, javac's lambda tests among them).

### JCS170 java.lang.reflect.Proxy over Dyn

`Proxy.newProxyInstance` throws in the Go build; a Proxy's class over `Dyn` (its interfaces' slots filled with the
handler's `invoke`, `Proxy.isProxyClass` true for it, `getInvocationHandler`) is J2G-SPEC §5.12's machinery at run time,
at the REPL; at the compiled level j2g would need the proxy's interfaces' adapters ahead of time (all of the world's
public interfaces have `Dyn`'s methods already). (a) Proxy over `Dyn` at both levels, with serialization's proxy class
descriptors (`resolveProxyClass`), 2 days; (b) the REPL only; (c) by design (J2G-SPEC §12 stands). **Recommended: (a)**:
proxies are also reflection's and the JDK's own (annotations' implementations, MXBeans' proxies).

### JCS171 The executable's growth

`serial-reach!` reaches every instantiated Serializable class's hooks and their callees (`HashMap.readObject`,
`ArrayList.writeObject`, the collections' serialization proxies...) and `serial-field?` lists their fields: an estimate
of +1.5 to 3 MB, measured at the night's step 3. (a) accept it, as JCS135 accepted the closure's growth; (b) reach the
hooks only of classes the REPL's world names publicly and the program's own (the shipped program's interpreted level
needs them all); (c) a build option without serialization. **Recommended: (a)**, then (c) only if the box's guest
needs it.

## As built: JCS167 slices 2-3 (write-ahead)

Branch `java-serial2` (from `java-serial` 05504ace6), 2026-11-08, by day under the light rules: the second and third
slices of JCS167 (a) ("The remaining slices" above). Nothing converted, translated or built into an executable: the
night's commands below join slice 1's.

### Slice 2: jrt's hand-written serializable classes

jdk26u's streams find a class's serial form by reflection (slice 1's variant of ObjectStreamClass), and jrt's
hand-written classes listed only their public members: no `serialVersionUID`, no private serial field, no hook, no
`SerialNew`. A JDK class j2g translates gets them from j2g (slice 1); a class jrt writes by hand now gets them from
`go/arbace/jrt/serial_classes.ae`, each with jdk26u's serial form byte for byte. The members are a `MemberTable` of
the class's private serial members, decoded on reflection's first use beside the public members `reflect_tables.ae`
lists (the hooks' descriptors name `java.io.ObjectOutputStream` and `ObjectInputStream`, which j2g translates and
registers after jrt's own classes; `MemberTable.DataFn`, new, makes the lines at the first decoding and drops the
hooks when the world has no streams). The hooks are jdk26u's bodies over jrt's state; they call the streams by
reflection over the translated classes' tables (`serialCall`: jrt cannot name a translated class's Go type), and
their exceptions propagate unwrapped as the JDK's do.

| class (jdk26u's serialVersionUID) | serial form |
|---|---|
| `Throwable` (-3042686055658047285) | fields `detailMessage`, `cause` (the exception itself when no cause was set, jdk26u's `cause = this`), `stackTrace` (`getOurStackTrace`; when not writable the sentinel `[StackTraceElement("", "", null, MIN_VALUE)]` while `writeObject` writes, its two names the one interned `""` as on the JDK), `suppressedExceptions` (`Collections.emptyList()` when none, null when suppression is disabled, else an `ArrayList`); `writeObject` (synchronized), `readObject` with jdk26u's validation (the list's class in java.base, its size, no null and not the exception itself; the sentinel making the trace not writable, a null or empty trace an empty one; a null list disabling suppression, as on the JDK) |
| `StackTraceElement` (6992337162326171013) | jdk26u's eight fields: jrt's element gains `classLoaderName`, `moduleName`, `moduleVersion` and `format`, with jdk26u's `toString`, `equals` and `hashCode`; a frame's element holds what the JVM gives one (`frameElement`: a JDK class's module java.base, version 26, format 3; another's loader `app`, format 1), so a frame prints as before |
| `Date` (7523967970034938905) | no fields; `writeObject` writes the time as a long, `readObject` reads it |
| `StringBuilder` (4383685877147921099) | no fields; `writeObject` the count and a `char[]` of the capacity (jrt grows the capacity as Java does), `readObject` `initBytes` and the count (StreamCorruptedException "count value invalid") |
| `StringBuffer` (3388685877147921107) | `serialPersistentFields` value, count, shared (`ObjectStreamField`s made once by reflection); `putFields`/`writeFields`, `readFields` |
| `AtomicInteger` (6214790243416807050), `AtomicLong` (1927816293512124184), `AtomicBoolean` (4654671469794556979, an int), `AtomicReference` (-1848883965231344442) | `value` (private volatile), no hooks |

The classes' `SerialNew` is the zero object (their first non-serializable superclass is Object). The stand-ins
(`standin_*.ae`: the wrappers, `Number`, `Locale`, the exceptions) are not in the build: j2g translates them with
slice 1's tables. Left for later (JCS191): the locks, `Semaphore`, `ForkJoinTask` and its adapters,
`AbstractQueuedSynchronizer$ConditionObject` (their serial forms name JDK classes jrt has not: `ReentrantLock$Sync`,
`NonfairSync`, AQS's state).

**The oracle**: `test/jrt/serialization/SerialRecord.java` recorded once by hand on jdk26u's JDK the bytes its
ObjectOutputStream writes for 16 cases (`jdk-streams.txt`, also `serial_test.ae`'s `jdkStreams`). jrt's package has
not the object streams, so `serial_test.ae` has a small stream of its own written after jdk26u's protocol
(ObjectOutputStream's grammar, handles, block data; class descriptors made as ObjectStreamClass makes them, by
reflection over jrt's tables: `serialVersionUID`, `serialPersistentFields`, the non-static non-transient fields
sorted primitives first and by name, the private hooks, an array class's default serialVersionUID) and stand-ins
named as the classes the hooks call (the streams, `PutField`, `GetField`, `ObjectStreamField`, `List`, `ArrayList`,
`Collections$EmptyList`, with their serial forms). `TestSerialForms`: each case's object made by jrt is written to
the JDK's bytes, the JDK's bytes are read into a jrt object that shows the same (message, trace, cause, suppressed,
capacity, value) and written again to the same bytes; `TestSerialThrowableRead` (readObject's rules),
`TestSerialFrames`, `TestSerialControl` (the negative controls: another object's bytes differ, a wrong
serialVersionUID is refused). The night's check with the real streams is `test/jrt/serialization/streams_check.ae`.

### Slice 3: the default serialVersionUID exact (JCS168 (a), behind a switch) and the interpreted classes' reflection

`arbace/classes/suid.ae` (`arbace.classes.suid`) ports jdk26u's `ObjectStreamClass.computeDefaultSUID` rule for
rule over the class as javac compiles it rather than over reflection: the declaration arbace.classes.analyze makes
(javac's derived members: default and anonymous constructors with their implicit parameters, `a/ctor-real-desc`;
bridges; records' and enums' members) and javac's synthetic members of a serializable class (Lower: `this$N`, kept in
a serializable class by `shouldEmitOuterThis`, N by `outerThisName`; the captured locals' `val$x`; `$assertionsDisabled`
by `assertFlagTest`, in an interface only `<clinit>`; `<clinit>` from a static initializer, a static field's
non-constant initializer, an assert or an enum's constants). It is used twice:

- **j2g** (`m/serial-uid`, `out.ae`): with `J2G_DEFAULT_SUID=1` each translated Serializable class without a static
  `serialVersionUID`, but enums and records, gets `ClassInfo.SerialUID` (new, with `HasSerialUID`); with
  `J2G_DEFAULT_SUID_OUT=FILE` the values are also appended to FILE for the night's comparison. **Off by default: the
  decision is the user's (JCS190).**
- **the interpreter** (`arbace.classes.interp/serial-uid!`, beside slice 1's `serial-new!`): every interpreted
  Serializable class without one, through the host (`Compiler$CF$Rt` `Host.setSerialUID`, `CFGo.setSerialUID`, j2g's
  native in `dyncf.ae`). Not behind the switch: an interpreted class's reflection is not javac's (below), so the stock
  computation over it is wrong without it.

The variant's `computeDefaultSUID` returns `SerialAlloc.defaultSUID(cl)` first when `SerialAlloc.hasDefaultSUID(cl)`
(`variants.py`, replayed: the regenerated `ObjectStreamClass.java` differs from slice 1's by that insertion alone;
jrt's natives `SerialAlloc_HasDefaultSUID0_Class__Z_native`, `SerialAlloc_DefaultSUID0_Class__J_native`).

**The interpreted classes' reflection** (`publish-members!`): a class publishes the virtual methods it inherits from
interpreted classes and their interfaces' defaults for dispatch only (bit 30, as a lambda's class since JCS116 (a)),
so `getDeclaredMethods` lists its own methods as the JVM's does; and it publishes javac's synthetic instance fields,
`this$0` and the captured locals' (final, synthetic), so `getDeclaredFields` and the default serial fields list them
as the JVM's (jdk26u serializes `this$0` too).

**The oracle**: `test/jrt/serialization/suid/` holds two samples (`src/suid/Sample.java`, `Sample2.java`: inner,
deeper inner and their subclasses, local and anonymous classes capturing locals in instance and static contexts, a
record, interfaces with methods, defaults, statics, a constant and a non-constant field, an assert, an empty one, an
abstract class with a bridge and a native method, generic classes and bridges, covariant returns, varargs,
synchronized, static and instance initializers, lambdas, method references, enum and pattern switches, every field
modifier mix), their forms (`forms/`, `bin/repr-java read --no-javac`) and jdk26u's `computeDefaultSUID` of their 26
Serializable classes (`jdk-suids.txt`, `runner/suid/SuidRecord.java`, recorded by hand). `check.ae` loads the forms at
the REPL and compares each class's value as the interpreter computes it (and, on an executable with the streams,
`ObjectStreamClass.lookupAny`'s). For the JDK's own classes `DefaultSUIDs.java` recorded `default-suids-jdk26.txt`:
the 118 Serializable classes of `jdk/forms` (members and anonymous ones) that declare none, which the night compares
with j2g's values.

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`, `GOMEMLIMIT=1400MiB`; scratch `.tmp/s2/`, `.tmp/s3/`)
- **the joint javac check** (`.tmp/night7/all9-run.sh`): 2,293 files, 0 errors, 5,002 classes (20 s, 1.0 GB);
- **go test ./jrt** (printed by `bin/jrt print`, run as `bin/jrt test` runs it, amd64): every test passes, the four new
  ones among them (TestSerialForms: 16 of 16 cases written, read and written again byte for byte); its control by
  hand: Date's serialVersionUID changed by one fails TestSerialForms ("written ... got/want") and the read
  ("local class incompatible"); `go vet ./jrt` on amd64 and arm64: clean; `bin/jrt manifest`: unchanged, "edge
  members of these classes jrt does not define: 0";
- **the default serialVersionUID**: `check.ae` on stage 0 (the samples' interpreted classes, their declarations
  taken as the interpreter publishes them): **26 of 26** equal to jdk26u's `computeDefaultSUID` (the record's too,
  -7535811012808276925, which ObjectStreamClass gives 0); its control (one expected value changed): 25 of 26, exit 1.
  Of jdk/forms' own classes stage 0's world analyzes two alone (`javax.naming.ldap.Control`, `java.io.Serializable`:
  equal); the 118 are the night's (j2g's world);
- j2g's namespaces (model, out, dyncf, decls, main) load on stage 0 from the checkout with `arbace.classes.suid`.

Not checked by day (no build): `Compiler.ae`'s new host method and the interpreter's `serial-uid!` and
`publish-members!` changes (stage 0 runs its embedded interpreter), j2g's output for `SerialUID`, the real streams
over the new tables.

### The night's commands

With slice 1's (its steps 0-6, "As built: JCS167 (write-ahead)"), in the night's worktree with this branch merged; the
conversion and the build under the heavy lock:

```sh
# 0. the joint javac check (0 errors)
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh (slice 1's step 1): the two overlays ObjectStreamClass.java and SerialAlloc.java change
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. the edges (slice 1's step 2), then the executable with JCS168's switch on for the comparison (write-ahead: the
#    shipped default stays off until JCS190; a build without it differs only in the ClassInfo.SerialUID lines)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
mkdir -p .tmp/jcs168 && rm -f .tmp/jcs168/suids.txt
J2G_DEFAULT_SUID=1 J2G_DEFAULT_SUID_OUT=$PWD/.tmp/jcs168/suids.txt bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 3. j2g's default serialVersionUIDs against the JDK's: expect every class of the record that j2g translated equal
awk 'NR==FNR{j[$1]=$2;next} ($1 in j){n++; if(j[$1]!=$2){b++; print "BAD",$0,j[$1]}} END{print n+0" compared, "b+0" differ"}' \
  .tmp/jcs168/suids.txt test/jrt/serialization/default-suids-jdk26.txt
# 4. the real streams over jrt's classes: expect "streams-check: 16 of 16"; the samples' interpreted classes: "suid-check: 26 of 26"
target/arbace-go/amd64/arbace -e '(load-file "test/jrt/serialization/streams_check.ae")'
ARBACE_PATH=$PWD:$PWD/test/jrt/serialization/suid/forms target/arbace-go/amd64/arbace -e '(load-file "test/jrt/serialization/suid/check.ae")'
# 5. Java's levels on the class's tests (slice 1's step 5, its --only with these added):
#    'jdk/java/lang/Throwable/|jdk/java/io/Serializable/|jdk/java/security/(KeyRep|Permissions|cert/CertPath)|jdk/java/net/Inet6Address/serialize|jdk/java/math/BigInteger/SerializationTests|jdk/java/util/(TimeZone/bug4096952|Calendar/Bug7017458|concurrent/atomic/Serial)|jdk/java/text/Format'
```

**Risks and fallbacks**: (1) a hook's call names a stream method by a signature the translated table lacks
(InternalError "serialization needs ..."): the table's line fixed, or the class's `serialClass` call removed (it is
then as before); (2) the interpreter's dispatch-only inherited methods break a call the interpreted level made through
the subclass's own table: revert `publish-members!`'s first hunk (the SUID stays right through `serial-uid!`); (3) a
j2g value differs from the JDK's at step 3: the night ships with the switch off (as committed) and the class's case
goes to JCS190's record; (4) `Compiler.ae`'s `setSerialUID` fails the build's analysis: revert it with
`interp.ae`'s `serial-uid!` and dyncf's native (interpreted classes then compute it by reflection, as slice 1).

### Expected yield

Slice 2: the streams holding an exception or a date, a string builder or an atomic: the Throwable tests
(`LegacyChainedExceptionSerialization`, `StackTraceSerialization`, `SuppressedExceptions`), the six "throwable?" ones
of slice 1's scan, `RemoteException/chaining`, `DateFormat/bug6271375` and the old tests that write exceptions; about
**8-12** at each of eval and AOT. Slice 3: of the 57 tests reading JDK-written streams, those whose classes declare no
serialVersionUID or are the test's own (`classDescFlagConflict`, `illegalHandle`, `oldTests`' readers, `KeyRep`,
`Permissions`, `Inet6Address`, `CertPath`, `BigInteger`, `TimeZone`, `Calendar`), slice 1's estimate **about 25**,
less those that need proxies, class loaders or the security manager. Together about +1 point of Java's eval and AOT
cells after slice 1's +1.4, the crop's strict completeness about +0.5.

### Remaining slices

Slices 4-8 of the table above (the JDK variants' cut members, `RecordComponent`, proxies JCS170, serializable lambdas
JCS169, Clojure's types), and JCS191 (the locks' serial forms).

## Proposed (JCS167 slices 2-3)

Numbered from JCS190, to keep clear of the numbers other branches may take after JCS171.

### JCS190 DECISION: JCS168's switch (j2g's default serialVersionUID ahead of time)

Built write-ahead (above): `J2G_DEFAULT_SUID=1` has j2g write every translated Serializable class's default
serialVersionUID; off, the variant computes it by reflection over the class's table, which lists only the public and
serial members (another value than the JDK's for each of the 118 classes of `default-suids-jdk26.txt` and for a
program's own classes at the compiled level). (a) on by default (`m/default-suid?` true unless `J2G_DEFAULT_SUID=0`),
after the night's step 3 finds the values equal: the tables unchanged, one `SerialUID` line per such class (about 120
of the world's, a few bytes each), no run-time cost; (b) on only for the compiled level's programs (`bin/j2g
--program`), the shipped world by reflection; (c) off, as now. **Recommended: (a)**: the JDK's value is what a stream
written by a JVM holds, and the computation is the one the interpreted classes use already.

### JCS191 The serial forms of jrt's locks, Semaphore and ForkJoinTask

Their jdk26u serial forms name classes jrt has not (`ReentrantLock$Sync`, `$NonfairSync`, `$FairSync`,
`AbstractQueuedSynchronizer`'s `state`, `AbstractOwnableSynchronizer`, `ReentrantReadWriteLock`'s three, `Semaphore$Sync`,
`ForkJoinTask`'s `status` and its adapters' fields). (a) descriptor classes in jrt for the hidden ones (their
serialVersionUIDs and fields, `SerialNew` making jrt's lock with the state read), ForkJoinTask's hooks: 1 day; (b)
as now (Serializable without serial members: written with an empty descriptor, read as a new unlocked lock, which
`readObject` gives on the JDK too, but the bytes differ). **Recommended: (a)** with slice 4 (few tests: the
`MyOwnSynchronizer` and ConcurrentHashMap ones).

### JCS192 A frame's module version

jrt's frames of JDK classes say `java.base` version "26" (`java.version`), the JVM's the build's full version
(`26.0.2.1` and its build suffix): only a stream holding a JVM-made frame shows the difference, never `toString`
(the format drops the version). (a) the version of `Runtime.version()` jrt reports; (b) as now. **Recommended: (a)**
when jrt's `Runtime.version` is made the JDK build's (an hour).

### JCS193 jrt's serial test stream as a shared harness

`serial_test.ae`'s stream (writer and reader after jdk26u's protocol) proves jrt's serial forms without the
translated streams. (a) keep it to jrt's classes; (b) also run it over j2g's tables of translated classes in `bin/jrt
test --prog` (the program root j2g writes), a check of slice 1's tables by day: half a day. **Recommended: (b)** when
slice 4 restores the variants' members.

## As built: F10b (write-ahead)

(Its proposals were JCS168-JCS170 on `java-f10b`; renumbered JCS220-JCS222 when merged into `java-night`, 2026-11-08,
since `java-serial2` had taken JCS168-JCS171.)

Branch `java-f10b` (from `next` `bd33e27cc`), 2026-11-08, by day under the light rules: JCS163 (a)'s first part (the user,
2026-11-07), **DatagramChannel and multicast through jdk26u's own `DatagramChannelImpl`, then the NIO adaptor as
`DatagramSocket`'s delegate**. The closure's list, jrt's own Java, the variants, the Go natives with their tests, light checks
only; nothing converted, translated or built into an executable. The asynchronous channels did not fit the slice: JCS220 below.

**The shape** is F10a's: jdk26u's `DatagramChannel`, `MulticastChannel`, `MembershipKey`, `DatagramChannelImpl`,
`DatagramSocketAdaptor`, `MembershipKeyImpl` and `MembershipRegistry` are plain Java over sun.nio.ch's native layer, which F10a gave
jrt; F10b adds what that layer lacked, rule for rule from libnio's C (jdk26u at `baf63fb`):
`src/java.base/unix/native/libnio/ch/DatagramChannelImpl.c` (`receive0`: recvfrom of at most MAX_PACKET_LEN bytes, the sender's
struct sockaddr into NativeSocketAddress's, EAGAIN UNAVAILABLE, EINTR INTERRUPTED, ECONNREFUSED retried when not connected and a
PortUnreachableException when connected, else handleSocketError; `send0`: sendto, the same results, ECONNREFUSED a
PortUnreachableException; `disconnect0`: Linux's connect to AF_UNSPEC with sockaddr_in6's or sockaddr_in's size),
`Net.c`'s multicast (`joinOrDrop4`: IP_ADD/DROP_MEMBERSHIP of a struct ip_mreq, IP_ADD/DROP_SOURCE_MEMBERSHIP of an ip_mreq_source,
a join the host does not support, ENOPROTOOPT or EOPNOTSUPP, UNAVAILABLE, else handleSocketErrorWithMessage's "setsockopt failed";
`blockOrUnblock4`: IP_BLOCK/UNBLOCK_SOURCE; `joinOrDrop6`: IPV6_ADD/DROP_MEMBERSHIP of an ipv6_mreq, MCAST_JOIN/LEAVE_SOURCE_GROUP
of a group_source_req as `initGroupSourceReq` fills it; `blockOrUnblock6`: MCAST_BLOCK/UNBLOCK_SOURCE; `setInterface4/6`,
`getInterface4/6`: IP_MULTICAST_IF's in_addr in network order, IPV6_MULTICAST_IF's index) and `DatagramDispatcher.c` (a connected
channel's recv and send with no flags, which are read(2) and write(2), ECONNREFUSED a PortUnreachableException). The natives are
HostChannels' nine new ones in `go/arbace/jrt/channels_linux.ae` (and "not available" in `channels_other.ae`), on F10a's
descriptor table: a descriptor the channel configured blocking waits in Go's poller for a datagram or for room, a pre-closed one
receives 0 (the dup2'd socket's end) and fails a send with EPIPE.

**No native memory, the one departure.** jdk26u's channel passes its natives a direct buffer's address and a
`NativeSocketAddress`'s (a struct sockaddr in memory from Unsafe), and registers the descriptor's close and the addresses' release
with a Cleaner. jrt's `NativeSocketAddress` overlay keeps jdk26u's class, its methods and its layout (Linux's sockaddr_in and
sockaddr_in6, the natives' sizeof and offsetof constants as their values on linux-amd64 and linux-arm64) over a byte array of the
union's 28 bytes, which the Go natives hand the kernel as recvfrom's and sendto's sockaddr: the kernel writes and reads the same
bytes it would in jdk26u. `DatagramChannelImpl`'s variant: the constructors without the Cleaner, `tryClose` closing the
descriptor as the Cleaner's releaser does, `receiveIntoNativeBuffer` and `sendFromNativeBuffer` over the buffer's array and the
address's bytes; jrt's temporary buffers being heap buffers (`Util`'s overlay), `receive` and `send` take jdk26u's
substitute-buffer path unchanged.

**What comes in, and how** (`test/jrt/jrt_sources.ae`'s `datagram-channel-sources`, one line in `sources`):

| part | how |
|---|---|
| java.nio.channels (3) | jdk26u's `DatagramChannel`, `MulticastChannel`, `MembershipKey` |
| sun.nio.ch (4) | jdk26u's `DatagramChannelImpl` (variant), `DatagramSocketAdaptor` (variant), `MembershipKeyImpl`, `MembershipRegistry` |
| overlays (2 new, 2 grown) | `sun.nio.ch.NativeSocketAddress` (above), `sun.nio.ch.DatagramDispatcher` (jdk26u's unix class over HostChannels' read0 and write0, ECONNREFUSED a PortUnreachableException; `dup`, the macOS branch of `repairSocket`, NativeDispatcher's refusal: Linux's disconnect leaves an explicitly bound port bound and resets an ephemeral one to 0, the branch Linux takes); `Net` (the eight multicast natives' bodies, F10a's refusal gone); `HostChannels` (nine natives) |
| variants (3 new, 1 gone, 1 grown) | `DatagramChannelImpl` (6 replaced, 4 cut: NIO_ACCESS, cleaner, receive0, send0; 1 added, `datagramException`); `DatagramSocketAdaptor$NetworkInterfaces` and `$DatagramSockets` (jdk26u reaches `NetworkInterface.getDefault`, its constructor and `DatagramSocket.NO_DELEGATE` through a private lookup's method handles and VarHandle, which the Go build has not: jrt's public accessors instead, 2 replaced 3 cut, 1 replaced); `DatagramSocket` (F11's `createDelegate` and constructor replaced by nothing: jdk26u's, the NIO adaptor the default delegate as in the JDK, JCS153 (c)'s second step; the variant now only adds `jrtNoDelegate`); `NetworkInterface` (+`jrtGetDefault`, `jrtNew`) |
| Go | `channels_linux.ae` +289 lines of forms, `channels_other.ae` +44, `datagrams_test.ae` (TestChannelDatagram, TestChannelMulticast) |

F11's `HostDatagramSocketImpl` stays in the closure, unreached (a program's DatagramSocketImplFactory returns its own impl):
JCS221. `DatagramChannelImpl$AbstractSelectableChannels` (reflection on `AbstractSelectableChannel.forEach`) is reached only by
`repairSocket`'s macOS branch.

**Light checks done** (main's stage 0 as `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-5ad357331dbcd7cfb0fd`,
`REPR_JAVA_STALE_OK=1`, `GOMEMLIMIT=1400MiB`, each process under 1.5 GiB; scratch `.tmp/f10b/`):
- `print-sources` of the branch: 2,286 lines, the base's 2,277 plus 7 jdk26u files and 2 overlays, no line twice;
- **the joint javac check** (`/root/arbace/.tmp/night7/all9-run.sh CHECKOUT`): all 2,286 files compiled by the JDK's javac as
  java.base's patch against jrt's overlays: **0 errors**, 4,959 classes (20.7 s, 0.9 GB; the base 2,277, 0 errors, 4,942);
- the read: `bin/repr-java read-jdk --check` of the 9 new files with the sun.nio.ch overlays, HostChannels, HostFiles and
  FileDescriptor as a patch of java.base: 20 converted, 0 failed, 0 javac errors, 0 unreadable (12.6 s, 0.65 GB);
- the variants applied and parsed (`vet.ae`, JC9b's): DatagramChannelImpl 6/4/1, NetworkInterface 12/1/4, DatagramSocket 0/0/1,
  the nested DatagramSocketAdaptor$NetworkInterfaces 2/3/0 and $DatagramSockets 1/0/0, all parsed;
- jrt's own build (`bin/jrt build --arch amd64`, with `FROZEN_JAR` the main checkout's frozen jar for the test data; its `go
  build ./...` stops at `arbace/expat`, Python's crop, as on F10a's day), then `go build` and `go vet` of `./jrt` for linux/amd64
  and linux/arm64, and `go test ./jrt` on amd64: every test passes, the two new ones among them (`TestChannelDatagram`: a send to
  a NativeSocketAddress's bytes and a receive with the sender's struct sockaddr, family and port; a non-blocking receive's
  UNAVAILABLE; a datagram truncated to the buffer; a connected socket's receive after a refused send THROWN with ECONNREFUSED, a
  disconnect, then an unconnected socket's refusal retried to UNAVAILABLE; a receive blocked in Go's poller woken by preClose to 0,
  a send after it failing; `TestChannelMulticast`: IP_MULTICAST_IF set to 127.0.0.1 and read back in host order, a join of
  239.255.0.1 on the loopback, a second join failing, a datagram to the group received by the member, a source blocked and
  unblocked, the drop, IPV6_MULTICAST_IF by index). One collision found and fixed: `udp_linux.ae` already names
  IP_ADD_SOURCE_MEMBERSHIP and IP_DROP_SOURCE_MEMBERSHIP.

**JCS135, the closure's growth**: +3,969 jdk26u lines and 386 of jrt's overlays (+0.7% on the closure's about 668,000).

**Expected yield.** The records (`test/java/check-interp.edn`, before F11 and F10a) hold 47 tests whose sources name a datagram
class, all passing on the JDK (`test/java/ref-jdk26.edn`): 31 under java/net (F11's: DatagramPacket 4, DatagramSocket 13,
DatagramSocketImpl 1, MulticastSocket 7, PortUnreachableException 3, B6499348, PlainSocketImpl/SetBufferSize, ipv6tests/
BadIPv6Addresses), which since F10b run on jdk26u's own adaptor rather than F11's legacy delegate, and **16 F10b's own**:
t05108 `channels/ConfigureBlocking`, t05109-t05118 `DatagramChannel/{AdaptorConcurrentIO, ChangingAddress, EmptyBuffer, NoSender,
NotBound, Receive, Refused, StressNativeSignal, ThereCanBeOnlyOne, Truncate}`, t05162 `Selector/CloseInvalidatesKeys`, t05165
`Selector/KeySets`, t05198 `SocketChannel/UnboundSocketTests`, t05199 `etc/AdaptorCloseAndInterrupt`, t05200
`etc/NetworkChannelTests`. At risk: `EmptyBuffer` and `AdaptorCloseAndInterrupt` (interrupts: JCS162), `StressNativeSignal`
(signals), `AdaptorConcurrentIO` (speed at the interpreted level). About **12 / 12 expected** of the 16, with F10a's 34 the
selectors' area about 46; the java/net ones hold or gain (the adaptor's behaviour is the JDK's own).

**The night's commands** (in the night's worktree with this branch merged, after F10a's and the night's other conversions;
heavy ones under `bin/heavy-lock`; about 1.5 hours with the areas' checks, the conversion shared with the night's):

```sh
# 0. the joint javac check of the merged tree (0 errors expected; a light minute)
/root/arbace/.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: expect the sources line +7 share and +2 of jrt's own beyond F10a's; 0 failed, 0 javac errors,
#    no javac: 0 fallbacks; jdk/forms + the 9 files (java/nio/channels' three, sun/nio/ch's six)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (DatagramChannelImpl's JavaNioAccess, Cleaner, MethodHandles, VarHandle, Method as outside
#    references), javac's world's edge
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: DatagramChannelImpl 6/4/1, DatagramSocketAdaptor$NetworkInterfaces 2/3/0,
#    $DatagramSockets 1/0/0, DatagramSocket 0/0/1, NetworkInterface 12/1/4; no "VarHandle not constant" (SOCKET by
#    MhUtil.findVarHandle, DatagramPackets' LENGTH and BUF_LENGTH by findVarHandle); no native without its Go function
#    (HostChannels' 40); no access refusal of jrtGetDefault, jrtNew, jrtNoDelegate
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F10b: expect "1 2 hi true", "2 yo", "3 true"
target/arbace-go/amd64/arbace -e '(import (quote (java.nio ByteBuffer)) (quote (java.nio.channels DatagramChannel)) (quote (java.net InetSocketAddress InetAddress DatagramSocket DatagramPacket MulticastSocket)))
  (let [lo (InetAddress/getLoopbackAddress) a (DatagramChannel/open) b (DatagramChannel/open)]
    (.bind a (InetSocketAddress. lo 0)) (.bind b (InetSocketAddress. lo 0))
    (.send a (ByteBuffer/wrap (.getBytes "hi")) (.getLocalAddress b))
    (let [buf (ByteBuffer/allocate 8) from (.receive b buf)]
      (println "1" (.position buf) (String. (.array buf) 0 (.position buf)) (= from (.getLocalAddress a))))
    (let [s (DatagramSocket. 0 lo) p (DatagramPacket. (.getBytes "yo") 2 (.getLocalAddress b)) buf (ByteBuffer/allocate 8)]
      (.send s p) (.receive b buf) (println "2" (String. (.array buf) 0 (.position buf))) (.close s))
    (let [m (MulticastSocket. 0)] (println "3" (.getReuseAddress m)) (.close m))
    (.close a) (.close b))'
# 5. Java's levels interp and compiled on the datagram tests, then the whole corpus with the night's others: saved, not
#    recorded; expect about 12 of F10b's 16 to pass, the 31 java/net ones to hold F11's, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/(nio/channels/(DatagramChannel|ConfigureBlocking|Selector/(CloseInvalidatesKeys|KeySets)|SocketChannel/UnboundSocketTests|etc/(AdaptorCloseAndInterrupt|NetworkChannelTests))|net/(Datagram|Multicast|PortUnreachable|B6499348|PlainSocketImpl/SetBufferSize|ipv6tests/BadIPv6Addresses))' \
  --save .tmp/f10b-night/check
```

**Risks and fallbacks**: (1) j2g's analysis checks access, which the light checks do not reach: the variants call only public
members (the added accessors) or the class's own; (2) the macOS branch of `repairSocket` (`DatagramDispatcher.dup`'s
refusal, `AbstractSelectableChannels`' reflection) is unreached on Linux, but should a test reach it, it fails with
UnsupportedOperationException instead of jdk26u's dup2; (3) should j2g refuse a member of the new forms, the night's fallback
leaves `datagram-channel-sources` out with the two overlays and the three variants, and restores F11's DatagramSocket variant
(`git show 0bc16ca6e^:overlay/jdk/variants/DatagramSocket.ae`): F10a and F11 do not depend on F10b; (4) the multicast tests need
the host's multicast routes (the loopback's join passes here).

**What is left of F10**: the asynchronous channels (JCS220), the UNIX family with F9 (JCS164), the interrupts (JCS162); the
night's run and the day's classification and records.

## Proposed (F10b)

### JCS220 The asynchronous channels: jdk26u's EPollPort group, or a thread per operation

The asynchronous channels hold 8 tests of the corpus (`AsynchronousChannelGroup/{Basic, GroupOfOne, Identity, Restart,
Unbounded}`, `AsynchronousSocketChannel/DieBeforeComplete`, `Channels/Basic2`; `AsynchronousFileChannel/{Basic, LotsOfWrites}`
are F7's world, its file channel jrt's own). (a) jdk26u's own: `AsynchronousChannelGroupImpl`, `Port`, Linux's `EPollPort` and
`LinuxAsynchronousChannelProvider`, `UnixAsynchronousSocketChannelImpl` and `UnixAsynchronousServerSocketChannelImpl`, `Invoker`,
`PendingFuture`, `CompletedFuture`, `Cancellable`, `Groupable`, the spi's `AsynchronousChannelProvider` (no ServiceLoader, as
SelectorProvider's variant), about 4,500 lines, over F10a's EPoll natives plus `UnixAsynchronousSocketChannelImpl.c`'s
`checkConnect` (getsockopt SO_ERROR) and `EPollPort`'s socketpair and interrupt natives: 2 to 3 days with jrt's tests; its
risk is the group's thread pools (ThreadPool, `sun.nio.ch.ThreadPool` over Executors: in the world since F2) and
`AsynchronousFileChannel`'s `SimpleAsynchronousFileChannelImpl` over F7's FileChannelImpl (a variant); (b) a jrt-own provider,
each operation on a goroutine over the blocking channels (less code, not jdk26u's: the groups' semantics, shutdown and
`Restart`'s handler threads differ); (c) not now. **Recommended: (a)**, after the night's records of F10a and F10b show the
selectors and datagram channels pass: it reuses their whole native layer.

### JCS221 F11's HostDatagramSocketImpl, unreached since F10b

Since the NIO adaptor is `DatagramSocket`'s delegate, jrt's `java.net.HostDatagramSocketImpl` (F11) and the natives of
`jdk.internal.jrt.HostDatagram` that only it calls (open, send, receive, peek, connect, join, the multicast options;
`go/arbace/jrt/udp.ae`'s UDPHost) are reached by nothing: a program's `DatagramSocketImplFactory` returns its own impl.
HostDatagram's interfaces (NetworkInterface's natives) stay in use. (a) remove HostDatagramSocketImpl and the socket half of
HostDatagram and udp.ae after the night proves F10b (about 700 lines of Java and Go fewer, jrt's tests of udp_test.ae's sockets
with them); (b) keep them as F10b's fallback for a release, then (a); (c) keep. **Recommended: (b)**: the fallback costs
nothing while it waits, and (a) is a small commit once the records agree.

### JCS222 j2g's constant method handles of private lookups

jdk26u reaches non-public members across packages through a private lookup's method handles and VarHandles made once in a
static initializer (DatagramSocketAdaptor's `NetworkInterfaces` and `DatagramSockets`; the same idiom in about a dozen
classes of java.base that the Go build may want: `Thread`'s, `ClassValue`'s, the HTTP client's). F10b's variants replace them
by jrt's public accessors, one variant per use. (a) j2g's VarHandle table (`arbace.j2g.vh`) extended to method handles:
`findStatic`, `findVirtual`, `findConstructor` and `findStaticVarHandle` of constant arguments in a static initializer,
`invokeExact`/`invoke`/`get` on those fields translated into direct calls and field reads (access lifted as the JVM's lookup
does), half a day with j2g's tests; (b) a variant per use as F10b did. **Recommended: (a)** when a second area needs it; (b)
until then.


## As built: JCS162

Branch `java-f9` (from `next`, `bd33e27cc`), 2026-11-08, by day under the light rules: the user's JCS162 (a). jrt's
hand-written Thread (`go/arbace/jrt/thread.ae`) gains jdk26u's `nioBlocker` and `interruptLock`
(`src/java.base/share/classes/java/lang/Thread.java` at `baf63fb`): the fields `nioBlocker` and `interruptMu`, and
`Thread.interrupt` as jdk26u's, rule for rule: the status set first (the token), then, when the interrupted thread is
not the current one, its blocker read under the interrupt lock and its `interrupt(Thread)` called there, its
`postInterrupt()` after the lock is released. `Thread.blockedOn(Interruptible)` (jdk26u's `JavaLangAccess.blockedOn`,
which only `AbstractInterruptibleChannel.blockedOn` calls; `AbstractSelector`'s begin and end call that) is jrt's
native `jdk.internal.jrt.HostThreads.blockedOn(Object)` (a new overlay, one native), which
`AbstractInterruptibleChannel`'s variant now calls; the variant's `end` is gone (jdk26u's begin and end again: F7's
check of the status at `end` stood in for the blocker).

**No reference to a translated type.** The proposal named a registration by the translated `Interruptible`; none is
needed: jrt declares sun.nio.ch.Interruptible's Go method set as j2g names it (`Interrupt_Thread__V(Thread_I)`,
`PostInterrupt__V()`, read off the gate executable's `sun_nio_ch_Interruptible.ae`) as its own interface,
`nioInterruptible`, and the native asserts the blocker to it (an object without the methods: ClassCastException), so
every translated implementation (AbstractInterruptibleChannel's and AbstractSelector's anonymous classes) is one,
and jrt builds and tests without the translation.

**What it makes work** (jdk26u's code above the blocker, unchanged): a thread blocked in a socket channel's or a
pipe's read, write, connect or accept (F10a: blocked in Go's poller) is woken by the channel's close, which the
interruptor does (`implCloseChannel` → `preClose`), and gets ClosedByInterruptException; a thread blocked in
`Selector.select` is woken by `wakeup()` (the eventfd or the pipe); a file channel's operation (jrt's own
FileChannelImpl) closes the channel on an interrupt from another thread during it, as before at `begin`.

**Checked** (light): `bin/jrt build --arch amd64` then `go build` and `go vet` of `./jrt`; `go test ./jrt`: every test
passes, the new `TestInterruptBlocker` among them (another thread's interrupt calls the blocker once, its target the
thread, then postInterrupt once, the status set; a self-interrupt calls neither; after `blockedOn(null)` an interrupt
calls nothing; a String as blocker is ClassCastException); the variant applied and parsed (F4's `vet.ae`:
AbstractInterruptibleChannel `{:replaced 1, :cut 0, :added 0}`); the joint javac check with F9's slice below.

**Expected tests** (the night's interp and compiled runs, with F7's and F10a's areas): `jdk/java/nio/channels/
Selector/LotsOfInterrupts.java#platform` (t05166), `Pipe/PipeInterrupt.java` (t05153), `FileChannel/
ClosedByInterrupt.java` (t05121), `FileChannel/InterruptDeadlock.java` (t05126), `FileChannel/InterruptMapDeadlock.java`
(t05127), `etc/AdaptorCloseAndInterrupt.java` (t05199, with F11's datagrams), and F10a's interrupt cases inside
`SocketChannel/*` and `Selector/*`: about 6 tests, as JCS162 counted. The nights' commands are F10a's (step 5's
`--only` covers them); the conversion adds one overlay (`jdk/internal/jrt/HostThreads.java`).

## As built: F9 (write-ahead)

Branch `java-f9` (from `next`, `bd33e27cc`), 2026-11-08, by day under the light rules: the user's decision of 2026-11-07,
the triage's rank 14, **java.nio.file from jdk26u's own Java** (FileSystems, Path, Files, the attribute views, directory
streams, walks, file stores, user principals, the watch service) with Linux's default provider, sun.nio.fs's Unix and
Linux classes, **over jrt's natives**: libnio's `UnixNativeDispatcher.c`, `UnixFileSystem.c`, `LinuxNativeDispatcher.c`
and `LinuxWatchService.c` ported into Go. It replaces jrt's own `Path`, `Files`, `HostPath` and `GlobPatterns`
(JRT-NOTES' FS2, reversed) and F7's `FileChannel` overlay (jdk26u's `open` again, through the provider). The closure's
list, the overlays, the Go natives with their tests, light checks only; nothing converted, translated or built into
an executable.

**The shape.** As F10a's channels, jdk26u's Java stays as it is and jrt gives it the native layer, rule for rule; unlike
F10a, the natives are the JDK classes' own `native` methods (j2g calls the Go function of a native by its mangled name,
J2G-SPEC §9.1), so `UnixNativeDispatcher`, `LinuxNativeDispatcher`, `LinuxWatchService` and `UnixFileSystem` keep their
declarations and jrt implements them (`go/arbace/jrt/nio_fs_linux.ae`, 66 functions, each the C function's system
calls with its flags, RESTARTABLE loops, results and errors: statx preferred to stat as on Linux with statx, the birth
time when STATX_BTIME comes back, glibc's makedev; copy_file_range then sendfile with the C's fallbacks and the
cancellation flag; inotify, the socketpair and poll of the watch service). What JNI gives the C, jrt gives in Go:

- **Native memory** (`go/arbace/jrt/nativemem.ae`, new): `Unsafe.allocateMemory`'s blocks are Go byte slices, an
  address a token (the block's number in the high 32 bits, the offset in the low 32), so Java's address arithmetic
  holds within a block; Unsafe's absolute accesses (getByte(long), putInt(long, int), copyMemory(long, long, long),
  setMemory, reallocateMemory, getAddress) and its (Object, long) accesses with a null base (`unsafe.ae`'s arrayBytes,
  intSlot and longSlot: copyMemory(null, address, array, ...), putIntVolatile(null, address, v)) reach the block; an
  address outside every block is an InternalError. So `NativeBuffer`, `NativeBuffers`, `Cancellable` (its polling
  address), the xattr views' buffers and LinuxWatchService's event parsing (`unsafe.getInt(address)`) are jdk26u's
  code, and the natives read a path as the C string at its address (`nativeCString`).
- **Set*Field**: a native that fills `UnixFileAttributes`, `UnixFileStoreAttributes` or `UnixMountEntry` sets the
  translated object's `F_name` field by Go's reflection (jrt cannot name a translated type; the field paths cached).
- **UnixException**: the natives throw it through a factory `UnixNativeDispatcher`'s static initializer registers
  (`jrtExceptions(UnixException::new)`, an `IntFunction` asserted to its Go method set), as the C's
  JNU_NewObjectByName.
- **The C library's databases**: getpwuid_r and the rest are Go's `os/user` over `/etc/passwd` and `/etc/group` (no
  NSS: Arbace has no C library); setmntent's FILE*, rewind and getlinelen a table of buffered files, getmntent_r's
  parse Go's (blank and comment lines skipped, the `\ooo` escapes); opendir's DIR* a table of descriptors read by
  getdents64 (fdopendir owns its descriptor, as glibc's).
- **Descriptors** are the kernel's; `UnixChannelFactory`'s overlay enters the one it opened into jrt's file table
  (`jdk.internal.jrt.HostFiles.adopt`, an `os.File` owning it), whose handle the FileDescriptor names, so the channel is
  jrt's FileChannelImpl (F7), which gains jdk26u's `setUninterruptible` (Files' and the provider's streams).

**What comes in** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `nio-file-sources`, one line in `sources`): 92
files, 70 of `src/java.base/share/classes` (java.nio.file's remaining 31 classes with Path, .attribute's 22, .spi's
FileTypeDetector, sun.nio.fs's 15 shared ones, java.nio.channels.FileChannel), 15 unix (UnixFileSystemProvider,
UnixFileAttributes and the views, the directory streams, file stores, principals, the URI utilities), 6 linux
(DefaultFileSystemProvider, LinuxFileSystem, LinuxFileSystemProvider, LinuxFileStore, the DOS and user-defined views)
and the generated `UnixConstants`; the overlays below; the closure computed by F1's `closure.py` (the linux root added,
the world this branch's print-sources): outside the world and the list only `AsynchronousFileChannel` and its
implementation and thread pool (operation-level stubs: `newAsynchronousFileChannel`), `ObjectInputStream`
(DirectoryIteratorException's readObject, by design) and what the overlays remove.

| overlay (derived: `.tmp/f9/derive.py`, each replacement asserted once) | the change |
|---|---|
| `java/nio/file/Files.java` | readString and writeString code through `jdk.internal.jrt.NioStrings` (the charset's coders with REPORT) where jdk26u asks JavaLangAccess; no installed FileTypeDetectors (no ServiceLoader) |
| `FileSystems.java` | getDefault without `jdk.internal.misc.VM`; a provider named by `java.nio.file.spi.DefaultFileSystemProvider` by Class.forName (no ClassLoaders); newFileSystem through installed providers only |
| `spi/FileSystemProvider.java` | no installed providers beyond the default (jdk.zipfs and jrtfs are not in the Go build) |
| `FileChannelLinesSpliterator.java` | unmap without JavaNioAccess (a mapping is a heap copy, F7) |
| `sun/nio/fs/UnixNativeDispatcher.java`, `LinuxNativeDispatcher.java`, `LinuxWatchService.java` | no library loaded; UnixException's factory registered (the watch service's initializer initializes UnixNativeDispatcher) |
| `UnixFileSystem.java` | copyFile's buffered copy through a NativeBuffer, not a temporary direct buffer's address |
| `UnixChannelFactory.java` | the descriptor adopted by jrt's file table; the append mode to FileChannelImpl's open; no JavaIOFileDescriptorAccess |
| `UnixPath.java` | encode through NioStrings |
| `NativeBuffer.java`, `NativeBuffers.java` | no Cleaner (free() frees once), a plain ThreadLocal (no TerminatingThreadLocal): a dead thread's cached buffers are not freed |
| `UnixUserDefinedFileAttributeView.java` | no JavaNioAccess (jrt's direct buffers are heap buffers: the direct branches are never taken) |

jrt's own beside them: `jdk/internal/jrt/NioStrings.java`, `HostFiles.java`'s `adopt`; removed: `java/nio/file/Path.java`,
`jdk/internal/jrt/HostPath.java`, `sun/nio/fs/GlobPatterns.java`, `java/nio/channels/FileChannel.java` (F7's) and the
variant `FilePermission.ae` (its builtInFS is jdk26u's again), `File.ae`'s toPath. j2g renames (`arbace.j2g.names`):
`sun/nio/fs/UnixFileSystem` → `Fs_UnixFileSystem` (java.io's), `sun/nio/fs/Util` → `Fs_Util` (sun.management's),
`java/nio/file/attribute/UserPrincipal` → `Nio_UserPrincipal` (JAAS's com.sun.security.auth.UserPrincipal); 147 classes
(nested ones included) checked against the gate executable's generated types and jrt's hand-written ones, these the only
collisions. javac's world: its `FileSystems` overlay (`overlay/jdk/javac/java.base/java/nio/file/`) and `bin/javac-vendor`'s
eight `java/nio/file` base files leave it (the program's world has them now). Off Linux (`nio_fs_other.ae`, TamaGo's, the
box) every native throws "java.nio.file's default file system not available" (JCS180). `nio_fs_linux_amd64.ae` and
`_arm64.ae` name statx's and copy_file_range's numbers, which Go's syscall package has not.

**Light checks done** (main's stage 0 as `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-5ad357331dbcd7cfb0fd`,
`REPR_JAVA_STALE_OK=1`, `GOMEMLIMIT=1400MiB`, each process under 1 GB):
- **the joint javac check** (`.tmp/night7/all9-run.sh` on the branch): 2,379 files (2,278 before F9), **0 errors**, 5,097
  classes (18 s, 0.9 GB); its first run found jrt's FileChannelImpl without `setUninterruptible` (3 errors), added;
- the read: `bin/repr-java read-jdk --check` of the 110 files (the list, the overlays, HostFiles, HostFileChannels,
  FileChannelImpl) as a patch of java.base: 110 converted, 0 failed, 0 javac errors (8.5 s, 0.75 GB); `--no-javac`: 110
  native, 0 fallbacks, the forms identical;
- the natives' names: the 77 native methods of the read forms (UnixNativeDispatcher 49 with jrtExceptions,
  LinuxNativeDispatcher 6, LinuxWatchService 8, UnixFileSystem 1, HostFiles' adopt0, HostThreads' blockedOn, ...) mangled as
  j2g names them (the renames applied), each defined in `nio_fs_linux.ae` (or thread.ae, files.ae) and in
  `nio_fs_other.ae`;
- jrt: `bin/jrt manifest` (Unsafe's 18 new members), `bin/jrt print`, `go vet` of `./jrt` for linux/amd64 and arm64, `go test
  ./jrt`: every test passes, the new ones among them: `TestNativeMemory` (the accesses, copies both ways, reallocation,
  an access after free and beyond a block), `TestNioFsNatives` (stat0 against stat(2): ino, dev, nanoseconds; a missing
  file's errno and UnixException; open0, fstat0, read0, close0's EBADF; mkdir0, symlink0, readlink0, realpath0, lstat0,
  fstatat0; the directory's five entries; fdopendir; access0; rename0, chmod0, utimensat0's times; directCopy0 and
  bufferedCopy0 with a cancellation flag; statvfs0; /proc/mounts' `/` through setmntent0, getlinelen, rewind, getmntent0;
  the escapes; getpwuid, getpwnam, getgrgid; strerror; getcwd; an adopted descriptor read through HostFiles),
  `TestNioFsXattr` (fsetxattr0, fgetxattr0's size and value, flistxattr, fremovexattr0's ENODATA), `TestNioFsInotify` (an
  IN_CREATE event with its name, poll of inotify and a socketpair).

**JCS135, the closure's growth**: +23,105 Java lines (15,844 of jdk26u's listed files, 9,935 of the derived overlays and
NioStrings, less jrt's 2,674 removed): about **+3.4%** on the closure; by F4's ratio about +4 MB of executable.

**Expected yield.** The interp record's non-passes naming java.nio.file (the triage's 28 / 30 touched, 13 / 13 alone):
`jdk/java/nio/file/Files/walkFileTree/Nulls` (t05225), `PathMatcher/Basic` (t05227), `WatchService/DeleteInterference`,
`LotsOfCancels`, `LotsOfCloses`, `UpdateInterference` (t05228-t05231), `attribute/AclEntry/EmptySet` (t05232),
`Files/probeContentType/ForceLoad` (t05224), `Path/UriImportExport` (t05226, a failing result today: jrt's own Path's
URI), `jdk/java/io/File/IsHidden`, `ListRoots`, `SymLinks` (t04259, t04261, t04267),
`jdk/sun/net/www/protocol/jar/FileURLConnectionLeak` (t08144, Files.list), `B6449504` (t08142, a NoSuchFileException
today), `jdk/java/util/logging/CheckLockLocationTest` (t05744), `jdk/java/util/zip/TestExtraTime` (t05832): about **12 / 12
expected**; not expected: `Files/CopyToNonDefaultFS` (t05222, zipfs), `jdk/internal/jrtfs/PathOps` (t08098, the jrt file
system), `tools/jlink/CheckExecutable` (t08310) and the eleven `tools/launcher` tests (t08312-t08322: they start a JDK's
`java`). With JCS162's six, about +18 at each level. **At risk: every test and tool that uses java.nio.file** (Clojure's
java.io, arbace.main, the suite, javac's tool's file manager): jrt's Path and Files were their implementation; the
night's whole corpus, the suite and the gate are the proof, not the area alone.

**The night's commands** (in the night's worktree with this branch merged; heavy ones under `bin/heavy-lock`; the
conversion shared with the night's others):

```sh
# 0. the joint javac check of the merged tree (0 errors expected; a light minute)
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: expect the sources line +70 share, +15 unix, +6 linux, +1 gensrc (UnixConstants), the
#    overlays +15 (the 13 derived, NioStrings, HostThreads) -4 (Path, HostPath, GlobPatterns, F7's FileChannel); 0 failed,
#    0 javac errors, no javac: 0 fallbacks; jdk/forms + about 105 files
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. javac's world afresh (its FileSystems overlay and eight java/nio/file base files gone), j2g's edge, javac's edge
bin/heavy-lock bin/javac-vendor convert && bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: no native without its Go function (the 77 above), the renames Fs_UnixFileSystem,
#    Fs_Util, Nio_UserPrincipal, no "a variant for a class not in the inputs" (FilePermission's variant is gone)
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F9: expect "true 5 [a.txt d]", "rw-r--r-- true", "[ENTRY_CREATE n]", "glob true", "sun.nio.fs.LinuxFileSystemProvider"
target/arbace-go/amd64/arbace -e '(import (quote (java.nio.file Files Path FileSystems StandardWatchEventKinds LinkOption)) (quote (java.nio.file.attribute PosixFilePermissions FileAttribute)) (quote (java.util.concurrent TimeUnit)))
  (let [d (Files/createTempDirectory "f9" (make-array FileAttribute 0)) f (.resolve d "a.txt")]
    (Files/writeString f "hello" (make-array java.nio.file.OpenOption 0))
    (Files/createDirectory (.resolve d "d") (make-array FileAttribute 0))
    (println (Files/exists f (make-array LinkOption 0)) (Files/size f)
             (with-open [s (Files/list d)] (vec (sort (map #(str (.getFileName %)) (iterator-seq (.iterator s)))))))
    (println (PosixFilePermissions/toString (Files/getPosixFilePermissions f (make-array LinkOption 0)))
             (.isRegularFile (Files/readAttributes f java.nio.file.attribute.BasicFileAttributes (make-array LinkOption 0))))
    (let [w (.newWatchService (FileSystems/getDefault))]
      (.register d w (into-array java.nio.file.WatchEvent$Kind [StandardWatchEventKinds/ENTRY_CREATE]))
      (Files/createFile (.resolve d "n") (make-array FileAttribute 0))
      (let [k (.poll w 5 TimeUnit/SECONDS)]
        (println (vec (map #(str (.kind %) " " (.context %)) (.pollEvents k)))))
      (.close w))
    (println "glob" (.matches (.getPathMatcher (FileSystems/getDefault) "glob:*.txt") (.getFileName f)))
    (println (.getName (class (.provider (FileSystems/getDefault))))))'
# 5. Java's levels interp and compiled on F9's and JCS162's areas, then the whole corpus with the night's others (F9
#    changes every java.nio.file user): saved, not recorded; expect about 18 / 18 of the tests above to pass, no
#    passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(java/nio/file|java/io/File/(IsHidden|ListRoots|SymLinks)|java/util/logging/CheckLockLocationTest|java/util/zip/TestExtraTime|sun/net/www/protocol/jar|java/nio/channels/(FileChannel|Pipe/PipeInterrupt|Selector/LotsOfInterrupts|etc/AdaptorCloseAndInterrupt))' \
  --save .tmp/f9-night/check
# 6. the gate, essential then --full (Clojure's suite, the oracle, javac's tool and repr/java's checks all read files
#    through java.nio.file now)
bin/gate && bin/gate --full
```

None of the slice's tests runs before the night: the classes they name (`FileSystems`, `FileVisitOption`,
`StandardWatchEventKinds`, ...) are not in the committed `jdk/forms` and the gate's executable.

**Risks and fallbacks**: (1) a behaviour of jrt's own Path that Arbace relied on and jdk26u's has not (the night's suite,
oracle and gate show it; jdk26u's is the JDK's, so the fix is on the user's side, recorded); (2) Go's reflection on a
translated field: a field j2g renames (a Go keyword, a collision) would not be found (InternalError "no field"): the
UnixFileAttributes', UnixFileStoreAttributes' and UnixMountEntry's names are plain; (3) statx under a seccomp filter that
refuses it: the C falls back to stat only when the function is absent, so here too a refusal is the error; (4) the
blocking calls (poll of the watch service, a read of a FIFO) hold an OS thread in a system call, as the JDK's; (5)
should j2g refuse a member of the new forms, the night's fallback is the branch's parent (jrt's own Path and Files): F9
is one commit of the list, the overlays and the natives, revertible alone; JCS162 does not depend on it.

**What is left of F9**: the night's run and the day's classification and records; jdk.zipfs's provider (CopyToNonDefaultFS,
the jar file systems javac's file manager could use) and the jrt file system (JCS181); the box's default file system
(JCS180); UNIX domain sockets over F9's paths (JCS164, decided (a), 1 day); the derivations as Arbace (JCS166, decided (a):
`.tmp/f9/derive.py` joins `gennet.py` and the others).

## Proposed (F9)

Numbered from JCS180 (JCS168-JCS179 left to the branches of the same day: F10b, serialization).

### JCS180 The box's default file system

On TamaGo (the box) jrt has no system calls: `nio_fs_other.ae` throws "java.nio.file's default file system not
available", so Path's operations fail in the box where jrt's own Path worked over java.io.File and the box's store. (a)
the box's natives over its store (the same natives' Go names, `nio_fs_tamago.ae`: stat from the store's file info, the
directory streams from its listings, no inotify: the watch service then sun.nio.fs's PollingWatchService, which is in the
closure), 2 days when the box is unparked; (b) the box keeps jrt's own Path and Files as variants of its build (two
worlds: against one closure); (c) as now until the box is unparked. **Recommended: (c) then (a)**: the box is parked (the
user, 2026-10-26), and (a) keeps one closure.

### JCS181 jdk.zipfs and the jrt file system

`Files/CopyToNonDefaultFS` and the jar file systems (FileSystems.newFileSystem of a jar, javac's file manager's jars,
`jdk/internal/jrtfs/PathOps`) need jdk.zipfs's `ZipFileSystemProvider` (plain Java over SeekableByteChannel and
java.util.zip) and jdk.internal.jrtfs (over the image, which the Go build has not). (a) jdk.zipfs into the world as an
installed provider (FileSystemProvider's overlay lists it in place of ServiceLoader), about 1.5 days with its tests (the
`jdk/nio/zipfs` corpus, not yet in `test/java/corpora.edn`); (b) neither. **Recommended: (a)** after the night's records,
jrtfs never (no image).

## As built: JCS164 (write-ahead)

Branch `java-unix` (from `java-f9`, `1ff91c764`), 2026-11-08, by day under the light rules: the user's JCS164 (a), **UNIX
domain sockets with F9**. jdk26u's UNIX family of the socket channels, rule for rule, replacing F10a's "not supported"
`UnixDomainSockets` overlay. The closure's list, the overlay, the Go natives with their tests, light checks only; nothing
converted, translated or built into an executable.

**The shape.** The channels' UNIX paths were already jdk26u's since F10a (`SocketChannelImpl`, `ServerSocketChannelImpl` and
`SelectorProviderImpl` take `StandardProtocolFamily.UNIX` and call `UnixDomainSockets`): only the class they call answered
`isSupported()` false, and `UnixDomainSocketAddress` stayed out because `of(Path)` needs java.nio.file's default file system,
which F9 brings. So JCS164 is three files and five natives:

| part | how |
|---|---|
| `java.net.UnixDomainSocketAddress` (share), `sun.nio.ch.UnixDomainSocketsUtil` (unix) | jdk26u's, unchanged (`test/jrt/jrt_sources.ae`'s `unix-socket-sources`, one line in `sources`); the address's `of` checks the path's file system against `FileSystems.getDefault()` and its class's module against java.base's (jrt's `Class.getModule`: sun.nio.fs is java.base's); its `readObject` (ObjectInputStream) an operation-level stub, by design as serialization is |
| `sun.nio.ch.UnixDomainSockets` (overlay, derived) | jdk26u's file with its six natives given bodies over `jdk.internal.jrt.HostChannels`, as F10a's Net (`.tmp/unix/derive.py`: four replacements, each asserted once, and no `native` declaration left): `init` true; `socket0`, `bind0`, `connect0` throw `HostChannels.socketException` (Net.c's handleSocketError); `accept0` `HostChannels.ioException(err, "Accept failed")` (JNU_ThrowIOExceptionWithLastError) and sets the new FileDescriptor (`IOUtil.setfdVal`, the C's setfdval); unixSocketAddressToSockaddr's length check (`MAX_UNIX_DOMAIN_PATH_LEN`, 106: "Unix domain path too long") before the call, where the C makes it. The rest stays jdk26u's: the temporary names of `bind(null)` (`jdk.net.unixdomain.tmpdir`, else `java.io.tmpdir`, `socket_` and NativePRNGNonBlocking's random number), `getPathBytes` by the default provider's `getSunPathForSocketFile` (F9's UnixPath bytes), the unnamed address |
| `jdk.internal.jrt.HostChannels` (overlay, jrt's) | five natives `unixSocket0`, `unixBind0`, `unixConnect0`, `unixAccept0`, `unixLocalAddress0` and the constant `MAX_UNIX_DOMAIN_PATH_LEN` |
| Go | `go/arbace/jrt/channels_linux.ae`'s UNIX section, a port of `src/java.base/unix/native/libnio/ch/UnixDomainSockets.c`; `channels_other.ae` (each "Channels not available"); `channels_test.ae`'s `TestChannelUnix` |

Why F10a's way and not F9's (the JDK class's own natives by their mangled names): the C throws java.net's exceptions chosen by
the error number (handleSocketError), which HostChannels' Java already makes for Net; a Go native would need a factory per
exception class, as F9 registers UnixException's. The descriptors are F10a's: entered into the channels' table
(non-blocking in the kernel, on Go's poller), so `IOUtil.configureBlocking`, `Net.poll`, `Net.shutdown`, the dispatchers'
reads and writes, preClose and the selectors work on a UNIX socket unchanged.

**The natives, rule for rule.** The sockaddr_un is built and read as the C does it, with raw system calls on
`syscall.RawSockaddrUnix` (Go's `SockaddrUnix` computes another length for an empty name and spells abstract names with
'@'): bind and connect pass a zeroed sockaddr_un with the name's bytes and length `offsetof(sun_path) + n + 1`; accept
(accept4, non-blocking and close-on-exec as F10a's accept) and getsockname read the name back by
sockaddrToUnixAddressBytes: empty when the length is `offsetof(sun_path)` (Linux's ZERO_PATHLEN, an unnamed socket),
else up to the first NUL. connect answers 1, UNAVAILABLE on EINPROGRESS, INTERRUPTED on EINTR, else THROWN; accept
UNAVAILABLE on EAGAIN, INTERRUPTED on EINTR, else THROWN. **Abstract names** (Linux's: the first byte NUL) bind and
connect through the natives (the test does), and read back empty by the C's strlen, as the JDK's; Java cannot name one
(UnixPath refuses a NUL: InvalidPathException), so the JDK's API has none and neither has jrt's.

**One departure, below the natives' interface** (F10a's): a blocking channel's descriptor is non-blocking in the kernel.
accept waits in Go's poller as F10a's does (preClose wakes it: EBADF). A UNIX stream connect never answers EINPROGRESS on
Linux; with the listener's backlog full a non-blocking one gets EAGAIN (thrown, as the C's) and a blocking one waits in the
kernel, where poll would report the unconnected socket writable at once: so a socket configured blocking tries again after
a pause (1 ms doubling to 32 ms) until it connects, fails otherwise or is pre-closed (EBADF).

**Light checks done** (main's stage 0 as `ARBACE_HOST_EXE`, javac's tool `.tmp/gate-exe/javac-5ad357331dbcd7cfb0fd`,
`REPR_JAVA_STALE_OK=1`, `GOMEMLIMIT=1400MiB`, each process under 1.1 GB; scratch `.tmp/unix/`):
- the closure (F1's `closure2.py`, the world this branch's `print-sources` less jrt's old overlay; seeds the three files,
  scope java.net and sun.nio.ch): the 3 files (434 lines with jdk26u's UnixDomainSockets), and outside the world only `java.io.ObjectInputStream` (the
  address's readObject) and jrt's hand-written `Object`;
- **the joint javac check** (`.tmp/night7/all9-run.sh` on the branch): 2,381 files (F9's 2,379 + 2), **0 errors**, 5,101
  classes (27 s, 1.0 GB);
- the read: `bin/repr-java read-jdk --check` of the 4 files (the two of the list, the overlay, HostChannels) as a patch of
  java.base: 4 converted, 0 failed, 0 javac errors, 0 unreadable; `--no-javac`: the forms identical;
- the natives' names: the five of the read HostChannels mangled as j2g names them (`HostChannels_UnixSocket0_I1__I_native`,
  `..._UnixBind0_I_B1_I1__I_native`, `..._UnixConnect0_I_B1_I1__I_native`, `..._UnixAccept0_I_I1_B2_I1__I_native`,
  `..._UnixLocalAddress0_I_B2_I1__I_native`), each defined in `channels_linux.ae` and `channels_other.ae`; the two new
  classes' simple names collide with none of the world's or jrt's;
- jrt: `bin/jrt test --arch amd64 -run TestChannel` (the printed module), `go vet ./jrt` for linux/amd64 and arm64, `go test
  ./jrt` on amd64 (every test passes) and `-run TestChannel` on arm64 under qemu-aarch64: `TestChannelUnix` passes on both
  (the sockaddr_un of "abc": family 1, the bytes, a NUL, length 6, sun_path 108 and the struct 110 bytes; a 106-byte name
  length 109, a 107-byte one ENAMETOOLONG; a listener bound in a scratch directory, its name read back; an unbound
  client's name empty; connect, accept with the unbound peer's empty name, "hello" one way and "back" the other, the
  peer's shutdown read as the end of the stream; a non-blocking accept with none pending UNAVAILABLE; a connect to a missing
  path ENOENT, a bind to the path in use EADDRINUSE; a blocked accept woken by preClose; an abstract name bound, connected
  and accepted, read back empty).

**JCS135, the closure's growth**: +271 Java lines of the list's two jdk26u files, the derived overlay's 220 in place of the
old one's 73, HostChannels +26: about +440 lines, **+0.1%**.

**Expected tests.** The corpora's tests that name the UNIX family (every test of `test/java/ref-jdk26.edn` searched for
`UnixDomainSocket`, `ProtocolFamily.UNIX` and `StandardProtocolFamily`): `jdk/java/nio/channels/unixdomain/Shutdown.java`
(**t05204**: bind(null)'s temporary name, connect, accept, write and read, shutdownOutput then ClosedChannelException, the end
of the stream, a non-blocking read's 0, shutdownInput's -1, Files.delete of the socket file) at interp and compiled: **1 / 1
expected**. Not expected: `unixdomain/SocketOptions.java` (t05205: jdk.net's `SO_PEERCRED` and `UnixDomainPrincipal`, the
module absent: JCS200). jdk26u's other UNIX tests (`unixdomain/Bind`, `IOExchanges`, `NonBlockingAccept`, `NullTest`,
`FileAttributes`, `java/net/UnixDomainSocketAddress/*`, the inheritedChannel ones) are TestNG or `/test/lib` tests, outside
the corpora. Today t05204 and t05205 fail at loading (`ClassNotFoundException: java.net.StandardProtocolFamily`, the records
before F11's datagrams) and are refused compiled.

**The night's commands** (after F9's, in the night's worktree with this branch merged; heavy ones under `bin/heavy-lock`;
the conversion, the edges and the build shared with F9's steps 1-3):

```sh
# 0. the joint javac check of the merged tree (0 errors expected)
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh (F9's step 1): beyond F9's, the sources line +1 share (java/net/UnixDomainSocketAddress), +1 unix
#    (sun/nio/ch/UnixDomainSocketsUtil); the overlay sun/nio/ch/UnixDomainSockets.java changed; 0 failed, 0 javac errors,
#    no javac: 0 fallbacks
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge (UnixDomainSocketAddress no longer an outside reference), javac's edge (F9's step 2)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable (F9's step 3); j2g's report: no native without its Go function (HostChannels' 36: F10a's 31 and the 5
#    unix ones)
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of JCS164: expect "java.net.UnixDomainSocketAddress true", "hi ", "ok"
target/arbace-go/amd64/arbace -e '(import (quote (java.net StandardProtocolFamily)) (quote (java.nio ByteBuffer)) (quote (java.nio.channels ServerSocketChannel SocketChannel)) (quote (java.nio.file Files)))
  (let [ss (ServerSocketChannel/open StandardProtocolFamily/UNIX)]
    (.bind ss nil)
    (let [a (.getLocalAddress ss) c (SocketChannel/open a) s (.accept ss) b (ByteBuffer/allocate 8)]
      (println (.getName (class a)) (.startsWith (str a) (System/getProperty "java.io.tmpdir")))
      (.write c (ByteBuffer/wrap (.getBytes "hi"))) (.read s b)
      (println (String. (.array b) 0 (.position b)) (str (.getRemoteAddress s)))
      (.close c) (.close s) (.close ss) (Files/delete (.getPath a)) (println "ok")))'
# 5. Java's levels interp and compiled on the UNIX family's tests (with F9's step 5 and F10a's area): saved, not recorded;
#    expect t05204 to pass at both, t05205 to fail on jdk.net (JCS200), no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/nio/channels/(unixdomain|SocketChannel|ServerSocketChannel|Selector|spi)' --save .tmp/jcs164-night/check
```

**Risks and fallbacks**: (1) a temporary name longer than 106 bytes: `bind(null)` names `java.io.tmpdir/socket_N`, so a
`TMPDIR` deeper than about 85 bytes makes "Unix domain path too long", as the JDK's (the checks' scratch paths are short;
`jdk.net.unixdomain.tmpdir` overrides); (2) the blocking connect's pauses when a listener's backlog is full (the kernel's
wait emulated, at most 32 ms late); (3) should j2g refuse a member of the new forms, the fallback is F10a's overlay again
(`git show 1ff91c764:overlay/jdk/java.base/sun/nio/ch/UnixDomainSockets.java`) with the line `unix-socket-sources` out: F9 does
not depend on it.

**What is left**: the night's run and the day's records; jdk.net's extended options for the UNIX family (JCS200); the
inherited channel (JCS201); the derivation as Arbace with F9's and F10a's (JCS166, decided (a): `.tmp/unix/derive.py` joins
them).

## Proposed (JCS164)

Numbered from JCS200 (as the brief asks).

### JCS200 jdk.net's extended socket options

jdk.net is absent from the Go build (F10a: `sun.net.ext.ExtendedSocketOptions` answers `NoExtendedSocketOptions`, as a JDK
without the module), so `jdk.net.ExtendedSocketOptions` (SO_PEERCRED, TCP_KEEPIDLE/INTERVAL/COUNT, TCP_QUICKACK, IP_DONTFRAGMENT,
SO_INCOMING_NAPI_ID) and `UnixDomainPrincipal` are not there: `unixdomain/SocketOptions` (t05205) and jdk26u's `jdk/net`
tests (about 10 in the corpora: `ExtendedSocketOption/*`, `Sockets/*`) fail on them. (a) jdk.net's classes into the world
(`src/jdk.net/share/classes/jdk/net/*` and its Linux platform `LinuxSocketOptions` over `jdk.internal.jrt.HostChannels`' get
and set options, a port of `src/jdk.net/linux/native/libextnet/LinuxSocketOptions.c`: getsockopt SO_PEERCRED's uid and gid
through os/user's names as F9's principals), registered as `sun.net.ext.ExtendedSocketOptions`' platform options in place of
the ServiceLoader-free stub, about 1 day; (b) as now. **Recommended: (a)**: it completes the UNIX family and the TCP options
F10a's channels already pass through.

### JCS201 The inherited channel

`System.inheritedChannel()` (jdk26u: `SelectorProvider.inheritedChannel()`, `sun.nio.ch.InheritedChannel` over
`InheritedChannel.c`: standard input as a socket channel, a server socket channel or a datagram channel, of the INET or UNIX
family, when the process was started by inetd or systemd's socket activation) answers null in jrt (F10a's provider variants).
`spi/SelectorProvider/inheritedChannel/ClosedStreams` (t05202: `System.inheritedChannel` not in jrt's System) and `NullTest`
(t05203) fail on it; the inheritedChannel tests that launch a child with a socket as its standard input are TestNG. (a)
jdk26u's InheritedChannel with its natives (`peerAddressInet`, `unixPeerAddress`, `soType0`, `dup`, `dup2`, `open0`, `close0`)
over HostChannels, `System.inheritedChannel` in jrt's System, the variants removed, 1 day; (b) `System.inheritedChannel`
alone, answering null (t05202, t05203 pass: their standard input is not a socket), 1 hour; (c) as now. **Recommended: (b)
now, (a) with F10b's DatagramChannel** (the inherited datagram channel needs it).

## As built: F14 (first slice, write-ahead)

Branch `java-f14` (from `next` `13895c06e`), 2026-11-08, by day under the light rules: F14, the triage's rank 16 and
the first not built (JC9's plan, decided by the user 2026-11-05), **its first slice: declaration annotations at run time
(J2G-SPEC's R16 reversed for them), with Field's primitive accessors**. jrt's Go, j2g's tables, the interpreter's hand-off,
the closure's list, light checks only; nothing converted, translated as a program or built into an executable. Proxy is
not used (serialization's JCS170, branch `java-serial2`, keeps it): an annotation is no proxy here (JCS210).

**The 75 / 72 measured first** (`test/reap/main.ae`'s classification re-run over `check-interp.edn` and
`check-compiled.edn`, `.tmp/f14/classify.py`, `rows.json`; the class `:reflection` holds 75 interpreted and 72 compiled,
the triage's 74 / 68 since JC9's records), each test's failure and source read, grouped by what it needs (a test under
each need; interp, the compiled level the same tests within a few):

| need | tests | of them only this | days | tests/day |
|---|---:|---:|---:|---:|
| declaration annotations (getAnnotation and the rest on Class, Method, Constructor, Field; java.lang.annotation's types and exceptions; @Inherited, @Repeatable) | 20 | 14 | 1 | 14 |
| Field's primitive accessors (getInt, setByte ...) | 3 | 3 | 0.2 | 15 |
| member classes (getClasses, getDeclaredClasses) | 4 | 3 | 0.5 | 6 |
| AccessFlag (Class/Member.accessFlags, the enum) | 4 | 2 | 1 | 2 |
| Parameter, MethodParameters | 5 | 3 | 1.5 | 2 |
| generic signatures (TypeVariable, ParameterizedType, getGeneric*) | 11 | 7 | 3 | 2.3 |
| type annotations (AnnotatedType and its kinds) | 11 | 9 | 3 (after generics) | 3 |
| Proxy (newProxyInstance, getProxyClass, isProxyClass) | 7 | 6 | 2 | 3 |
| the metadata (local and anonymous classes, getEnclosingMethod/Constructor, InnerClasses modifiers, canonical names, javac's synthetic lambda methods, nestmates' constructors, array dimensions) | 18 | 15 | 3+ | 5 |

So the slice is the first two rows: about 17 of the 75 for 1.2 days, the triage's average for F14 (6 a day) three times.

**The design (the closed world records, jrt decodes).** Annotations are recorded ahead of time as data, the way the
member tables are (amendment SZ3) and CDR4's tables are: no class file, no constant pool, no parser of attributes. One
text format, decoded by jrt (`go/arbace/jrt/annotation.ae`'s header gives it: a line a record, `T` the class's, `M`
`C` `F` a member's by name and descriptor, `P` a parameter's, `D` an annotation type's element default; a value
self-typed: `I5`, `"TEXT"` with `\uXXXX`, `cLjava.lang.String;`, `eLjava.lang.annotation.ElementType;TYPE`, a nested
`@DESC N ...`, `[DESC N ...`), written by one encoder, `arbace.classes.annot`, which both writers call:
- **j2g** (`out/class-registration`): `ClassInfo.Annots` of every class it translates: the class's RUNTIME-visible
  annotations and an annotation type's defaults; a `--program`'s own classes (the conformance check's tests, a user's
  program: `nm/own-class?`, as their member tables list every member, JCS117 (a)) also their members' and parameters'.
  The JDK's members' annotations are not recorded (JCS211). Every annotation type it translates gets a Go type of its
  annotations (`decls/annotation-impl-forms`): `C_ann` embeds `jrt.AnnotationObj` and has C's markers and one method a
  member, returning `AnnotationObj.Member` at the member's Go type; `ClassInfo.AnnNew` makes one. That object
  satisfies C's Go interface, so translated code's cast and member call are the ordinary ones. In place of the JDK's
  Proxy of C (AnnotationParser.annotationForMap).
- **the interpreter** (`arbace.classes.interp`'s define!): the same data of every class it defines, members and
  parameters included, handed to jrt (`Compiler$CF$Host.annotations`, the native `CFGo.setAnnotations` in
  `arbace.j2g.dyncf`, `jrt.SetAnnotations`). An interpreted annotation type's objects are plain `AnnotationObj`s: its
  Class's instance check accepts them (`jrt.AnnotationTypeOf`), its Kind is `KindAnnotation` (isAnnotation), and a call of
  its member (CallV without a reflective method) goes to the receiver's class's public method of that key
  (`Rt.receiverMethod`: the JVM's invokeinterface on an object of the world implementing an interpreted interface).
- Each annotation is resolved alone (`arbace.classes.analyze/*lenient*`): one whose type or values the world cannot
  resolve (a type outside the closed world, an edge fact not recorded) is left out, as AnnotationParser skips an
  annotation of a type it cannot load; a class of the world is never refused for its annotations. A recorded annotation
  whose type jrt's world has not (a cut class) is skipped when decoded.
- **jrt** (`annotation.ae`): `java.lang.annotation.Annotation` and `java.lang.reflect.AnnotatedElement` hand-written
  (Class, AccessibleObject and so Method, Constructor, Field implement it), with AnnotatedElement's default methods as
  functions (a class made at run time, Dyn, implementing it); `AnnotationObj`, jdk26u's AnnotationInvocationHandler rule
  for rule (`equalsImpl` with another implementation's members by its methods, `hashCodeImpl`, `toStringImpl` with
  every `toSourceString`, a member's array cloned); memberValues as AnnotationParser builds it (the defaults in
  `new HashMap<>(0)`'s iteration order, emulated: `hashMapOrder`, checked against JDK 26's, then the explicit members
  in order); Class's `annotationData` with @Inherited from the superclass, `getAnnotationsByType` with
  AnnotationSupport's @Repeatable containers and their order; the members' `getAnnotation`, `getDeclaredAnnotations`,
  `getAnnotationsByType`; `getParameterAnnotations`; `Method.getDefaultValue`. An object's class is jrt's, made once per
  type: `jdk.proxy1.$ProxyN` (the type's package when it is not public), final, extending Proxy, implementing the type,
  its member table the elements and Annotation's four (so `getClass().getMethod("value").invoke(a)` works).
  `isAnnotationPresent(FunctionalInterface)` keeps its answer by FromFn (Compiler.FISupport) beside the recorded one.
- **Field's primitive accessors** (`reflect.ae`): `getBoolean` ... `getDouble`, `setBoolean` ... `setDouble`, after
  jdk26u's FieldAccessorImpl: widening on get and set, `newGetIllegalArgumentException`'s and `getSetMessage`'s messages.
- **The closure** (`test/jrt/jrt_sources.ae`'s `annotation-sources`, 14 files of jdk26u): java.lang.annotation's
  `AnnotationFormatError`, `AnnotationTypeMismatchException`, `Documented`, `ElementType`,
  `IncompleteAnnotationException`, `Inherited`, `Native`, `Repeatable`, `Retention`, `RetentionPolicy`, `Target`, and
  java.lang's `Deprecated`, `FunctionalInterface`, `SafeVarargs`. javac's world gave up its copies of five of them
  (`bin/javac-vendor`'s `base_files`; its forms, source list and provenance's counts by hand: 821 sources, 909 forms).
- The analysis: a class value written `Integer/TYPE` (repr/java's spelling of `int.class`, `Void/TYPE` of `void.class`)
  is the primitive class (it failed: "Unknown type: Byte/TYPE"); an array value keeps its element's descriptor.
- Order: repr/java writes one `^{A ...}` per annotation and the reader assocs each outer map onto the inner one's, so a
  declaration's annotations come in reverse; the encoder reverses them (JCS212).

**Light checks done** (main's stage 0 as `ARBACE_HOST_EXE`, the frozen jar `FROZEN_JAR` of the main checkout, each
process under 1.6 GB):
- `go test ./jrt` on amd64 (`bin/jrt test --arch amd64`): every test passes, the new `TestAnnotationValues`,
  `TestAnnotationAllKinds`, `TestAnnotationHashMapOrder`, `TestFieldPrimitives` among them: an annotation type and an
  annotated class written as j2g writes them, their toString and hashCode equal to JDK 26's for the same program
  (`.tmp/f14/jo/test/Host.java`; Class's identity hash taken out of the comparison), every value kind, the parameters'
  annotation of a type outside the world skipped, the object's class and its member invoked;
- `bin/jrt manifest` (jrt's manifest and member tables regenerated: Class's, Method's, Constructor's, Field's new
  members, Annotation and AnnotatedElement), `bin/jrt testdata` (TestReflectAgainstJVM then passes);
- the joint javac check (`.tmp/night7/all9-run.sh`): **2,291 files, 0 errors**, 4,956 classes (16 s, 1.0 GB);
- the encoder on the frozen JVM (`.tmp/f14/encode.clj`: the conformance check's read forms of t05990, t06338, t07647,
  t04860, t04861, t03578, t04536, t04524, t04525 analyzed, each class's data printed): every annotation the tests read
  is recorded with its values and defaults, in source order;
- j2g on the frozen JVM, a slice of one fixture (`.tmp/f14/fx/f14/Annots.ae`: an annotation type with defaults, an
  annotated class, field, method and parameter; `--slice '^f14/' --jvm-reflection`, 1.4 GB heap): `ClassInfo.Annots`
  of both classes, `AnnNew`, the type's `_ann` Go type and its three members as designed.

**Expected tests** (each needs only what the slice built, as read from its source; interp / compiled):
t05990 (Primitives: class values, void and arrays), t06338 (Pos04), t07647 (TestAnnotatedStringEncoding; passes compiled
already, vacuously), t04860 (IsDefaultTest), t03578 (TestFMA), t04716 (FoundType), t04722
(TestIncompleteAnnotationExceptionNPE), t04720 (RecursiveAnnotation), t04728 (NonInheritableContainee), t04729
(OrderUnitTest), t04727 (InheritedAssociatedAnnotations), t04843 (StaticFieldTest), t07681 and t07691 (the nestmates'
private fields), and possibly t04861 (DefaultMethodModeling), t04536 (TestPrimitiveAndArrayModifiers: the member classes'
modifiers too) and t04841 (TestParameterAnnotations: its constructors' strings): **about 14 / 14 expected** of 17, Java eval
and AOT each about +0.4 points. Still failing for another row of the table: t04718 (Proxy.getInvocationHandler), t04855 and
t04848 (generics), t04524 and t04525 (getEnclosingConstructor/Method), t04730 (Package), t04836 (AnnotatedElement's
defaults on a class of the program, with generics of the edge).

**The night's commands** (heavy ones under `bin/heavy-lock`; in the night's worktree with this branch merged; one
`bin/jrt-convert update` and one build take every area of the night):

```sh
# 0. the joint javac check of the merged tree (0 errors expected)
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: the sources line + 14 share (java/lang/annotation's 11, Deprecated, FunctionalInterface,
#    SafeVarargs), 0 failed, 0 javac errors; jdk/forms + 14 files and the package index java/lang/annotation.ae
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge afresh (the annotation types' :retention and :targets facts the JDK's forms now ask), javac's world's
#    edge and forms (base_files less the five: bin/javac-vendor convert, or verify against the hand edit)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge && bin/heavy-lock bin/javac-vendor verify
# 3. the executable; j2g's report: no new refusal; the natives check finds CFGo.setAnnotations
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke: expect "@java.lang.FunctionalInterface() true RUNTIME"
target/arbace-go/amd64/arbace -e '(println (.getAnnotation Runnable FunctionalInterface)
  (.isAnnotationPresent java.util.function.Function FunctionalInterface)
  (.value (.getAnnotation java.lang.annotation.Retention java.lang.annotation.Retention)))'
# 5. Java's levels on the area, then the corpus with the night's others: saved, not recorded; expect about 14 / 14
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/lang/(annotation|reflect/(Field|Method|Constructor)|Class/getModifiers)|javac/(annotations|multicatch/Pos04)|runtime/(Annotations|Nestmates/private)|compiler/floatingpoint/TestFMA' \
  --save .tmp/f14-night/check
```

**Risks and fallbacks**: (1) a JDK class whose annotation types resolve now (Deprecated, FunctionalInterface in the world)
records its class annotations: more data (one line a class; the JDK's members are not recorded); (2) the interpreter
forces its classes' annotations at define!: an annotation that does not resolve is left out (`*lenient*`), and the whole
encoding is in a try, so no class fails to define for it; (3) should the conversion or j2g refuse a file of
`annotation-sources`, the night drops it from the list: jrt's Annotation and AnnotatedElement stand alone, the annotation
types then resolve as cut classes and are skipped; (4) the change of `Compiler.ae` (Host.annotations, CallV's fallback)
moves the runtime's key: the first build compiles the namespaces anew.

**Left of F14** (the table's rows, by tests a day): member classes (getClasses, getDeclaredClasses: the closed world's
member classes from `ClassInfo.Declaring`, half a day), AccessFlag and Parameter/MethodParameters (the recorded
`MethodParameters` beside the annotations' data; jdk26u's AccessFlag enum translated, Class/Member.accessFlags over the
modifiers), the generic signatures (the analysis's `:sig` recorded as `Signature` strings and jdk26u's
sun.reflect.generics parser, factory and reflective objects translated over them: the real algorithm, no Go port), the
type annotations after them (TypeAnnotationParser over recorded data), Proxy (JCS210, with serialization's JCS170), and
the metadata row (local and anonymous classes, the enclosing method, InnerClasses' modifiers: the analysis knows them).

**Stage 0 and F14's analyzer (night 10's stopgap, retired by W's R).** Night 10 (java-night 33d04049c) met stage 0
(seed `d5548e72`), whose embedded `arbace.classes.analyze` predates F14 (no `*lenient*`, no `element-default`), failing to
load `arbace.j2g.out` through `arbace.classes.annot`. Two stopgaps followed: `arbace.classes.annot` resolved both vars with
`ns-resolve` (`lenient-var`, the `lenient` macro, an `element-default` fallback that records no default), and
`bin/arbace-go`'s `build()` sent the second j2g run to the first program while the host's embedded `arbace/classes/`
differed from the checkout's (`classes_stale`). Both are retired by R (doc/W-WINDOW.md, "As built: R"; branch
`w-retire`), which applies at W2's step 11, after the new seed's verify: with `arbace/classes/` in `RT.toolSource` (W-D3
(a)) and the new seed carrying F14's analyzer, `annot` binds `a/*lenient*` and calls `a/element-default` directly again,
and the second run goes to the first program only for a host without `RT.ID_SPACE` (SA143), as before night 10.

## Proposed (F14)

### JCS210 Annotations as proxies, or jrt's annotation classes

An annotation here is no `java.lang.reflect.Proxy`: `Proxy.isProxyClass(a.getClass())` is false and
`Proxy.getInvocationHandler(a)` throws (t04718, EqualityTest). (a) keep jrt's per-type classes, answer `isProxyClass`
true for them and `getInvocationHandler` with an InvocationHandler over the AnnotationObj (jdk26u's
AnnotationInvocationHandler's invoke), half a day, with the Proxy row; (b) annotations as real proxies once Proxy exists
(F14's Proxy row, JCS170's serialization), the j2g types dropped; (c) as built, the two tests by design. Recommended:
(a), on the Proxy row's day: the JDK's observable behaviour without its class generation.

### JCS211 The JDK's members' annotations

j2g records the annotations of a translated JDK class itself and of an annotation type's elements, not of its members
(@Deprecated, @Stable ... on thousands of methods), so `String.class.getMethod("getBytes", ...).getAnnotations()` is
empty where the JDK has @Deprecated. (a) as built; (b) every member's (more data in every executable, a measure first);
(c) only the java.lang.annotation and java.lang annotation types' (@Deprecated, @FunctionalInterface, @SafeVarargs).
Recommended: (c) if a test asks it; none of the 75 does.

### JCS212 The order of a declaration's annotations

The reader assocs `^{A ...} ^{B ...}`'s outer map onto the inner one's, so the metadata holds B before A; the encoder
reverses a declaration's annotations, which is the source's order for repr/java's forms (one map an annotation) and the
reverse of it for `^{A ... B ...}` written by hand. (a) as built; (b) repr/java writes a declaration's annotations in one
map, in order (every form of `jdk/forms` with two annotations changes: a reconversion and the round trips' references);
(c) the forms record the order (`:annotation-order` metadata). Recommended: (a); getDeclaredAnnotations' order is the
class file's, which nothing in the 75 checks across two annotation types but OrderUnitTest, which compares two lists of
the same order.

### JCS213 IncompleteAnnotationException from a missing member

A member without a value or default (an annotation type changed after its use was compiled: impossible in the closed
world, possible at the REPL) throws UnsupportedOperationException naming IncompleteAnnotationException, jrt's hand-written
Go not reaching the translated class. (a) as built; (b) a hook jrt calls, set by jdk.internal.jrt's Java at start (the
real IncompleteAnnotationException), a few lines. Recommended: (b) with the next slice.

## The Java night (`java-night`, write-ahead)

The user's decision of 2026-11-08: the day's seven Java slices on one branch, converted and tested together in the
night after night 9, each slice revertable on its own. Branch `java-night` from `next` `13895c06e`, one merge commit a
slice (mac/ refs), then `next` again (`7744e70b4`, the day's five crop branches) at `9aa350afb`.

**The merges and their conflicts** (resolutions in each merge commit's message):

- `java-serial` (05504ace6): none.
- `java-serial2` (c62dac3e0, on java-serial): the journal (both kept).
- `java-f10b` (7b4944e36): the journal and this file (both kept; **F10b's proposals JCS168-JCS170 renumbered JCS220-JCS222**:
  JCS167's slices had taken JCS168-JCS171); `go/arbace/jrt.ae` (the test files of both).
- `java-f9` (1ff91c764, F9 and JCS162): the journal, this file, LICENSE.md (both kept); `go/arbace/jrt.ae` (the lists and loads
  united).
- `java-unix` (2e6a1dfdb, JCS164, on java-f9): `go/arbace/jrt/channels_linux.ae`, `channels_other.ae` and
  `overlay/jdk/java.base/jdk/internal/jrt/HostChannels.java` (F10b's datagram natives and JCS164's UNIX natives inserted at the
  same place: both, F10b's first); `test/jrt/jrt_sources.ae` (selector-sources' comment rewritten for both).
- `java-jcv2` (79f4658d5): the journal; the Makefile (java-golden); `test/crops/scores.edn` (recorded again after the merges).
- `java-f14` (5bbf24b7e): the journal and this file (both); `bin/javac-vendor` (F9's java.nio.file and F14's annotation files
  both out of javac's base files); `arbace/classes/interp.ae` (both requires: suid, annot); `arbace/lang/Compiler.ae` (CF's
  natives of both); `go/arbace/jrt/class.ae` (ClassInfo's SerialNew, SerialUID, HasSerialUID and Annots, AnnNew);
  `go/arbace/jrt/reflect.ae` (Field's primitive accessors: F14's port of FieldAccessorImpl in place of JCS167's primGet/primSet);
  the generated `manifest.edn`, `manifest_test.ae`, `reflect_tables.ae` (F14's side, a superset; `bin/jrt manifest` afterwards
  leaves them unchanged). **Interactions fixed**: F14's Field.setX refused every final field; it now allows the ones JCS167's
  `finalWritable` allows, as `Field.set` does (serialization's field reflector writes accessible final fields); JCS167's test of
  setInt on a final field not made accessible expects jdk26u's message, "to (int)1" (FieldAccessorImpl's
  `throwFinalFieldIllegalAccessException(int)`).

**Light checks** (by day, on the merged head): the joint javac check (`.tmp/night7/all9-run.sh`): 2,420 files, **0 errors**, 5,192
classes (16 s, 1.0 GB), the same after next's merge; `go test ./jrt` passes (amd64; go/'s crop packages expat, pyexpat and oi
fail by themselves, as on next); `go vet ./jrt` clean on linux/amd64 and arm64; `bin/jrt manifest` unchanged; `bin/reap` 0
problems; `bin/crops-score --judge --strict` 0 after `--record` (Java's cells as JCS167's matrix, independence 99.72% as
JCV2's); `bin/gate --plan`: 48 steps, 42 to run. Each slice's revert (and the combinations f14+f10b, f14+unix+f9+f10b,
f14+serial2+serial, jcv2+unix+f9+f10b+serial2+serial) made by the night's revert tool on a scratch worktree: every one
resolved, balanced, the joint javac check 0 errors, `go vet ./jrt` clean (JCS167's slice 1 after `bin/jrt manifest`).

**The revert table** (newest first; `.tmp/javanight/run.sh` holds the same):

| slice | branch (head) | merge on java-night | revert first | kept at its revert |
|---|---|---|---|---|
| F14's first slice | `java-f14` (5bbf24b7e) | `386749ca8` | | |
| JCV2 | `java-jcv2` (79f4658d5) | `14a168531` | | |
| JCS164 | `java-unix` (2e6a1dfdb) | `9fa6ceea2` | | |
| F9, JCS162 | `java-f9` (1ff91c764) | `8d526f981` | JCS164 | |
| F10b | `java-f10b` (7b4944e36) | `ad20fb5a3` | | |
| JCS167 slices 2-3 | `java-serial2` (c62dac3e0) | `42efaaad4` | | |
| JCS167 slice 1 | `java-serial` (05504ace6) | `33b6d9b85` | slices 2-3 | `reflect.ae`, `reflect_test.ae` (F14's) |

A revert is `git revert -m 1` of the merge by `.tmp/javanight/revert-slice.sh`: the records (the journal, this file,
LICENSE.md, the scores) keep the slice's text; `go/arbace/jrt.ae` drops the deleted files; any other conflict is each hunk's
current text less the lines the slice added there (its brackets rebalanced); `bin/jrt manifest` afterwards.

**The night's script** (`.tmp/javanight/run.sh`, logs and progress.log in `.tmp/javanight/`, heavy commands under
`.tmp/heavy bin/heavy-lock`): (w) wait for night 9's "night 9 done" (or a stop line); (m) `mac/next` merged in (night 9's
records; the journal's entries both kept, the scores and the matrix next's, anything else stops the night); (p) the pipeline:
the joint check; `bin/javac-vendor convert`; `bin/jrt-convert update` on javac's newest tool (`GOMEMLIMIT=28GiB`); `bin/jrt
modules`; `bin/j2g-edge update`; `bin/javac-vendor edge` and `verify`; `bin/generated-check`; `bin/arbace-go --build` with
`J2G_DEFAULT_SUID=1 J2G_DEFAULT_SUID_OUT` (JCS168, decided (a)), `--smoke`, `bin/jrt test` (arbace/jrt's lines on amd64 and
arm64); the slices' smokes against their expected output (JCS167's round trips, streams-check 16 of 16 and suid-check 26 of 26,
F10b's datagrams, F9's files, JCS164's UNIX socket, F14's annotations); JCS168's comparison with the JDK's 118 values (a
difference goes to JCS190's record, no revert); JCV2's `bin/java-check golden` and `verdicts --verify`; `bin/java-check
--levels interp,compiled --only <the active slices' areas> --save` and `--judge` (saved and judged, not recorded: a regression
or a passing control reverts the slices whose areas it falls in, "in no class" is the day's classification); the conversion
committed, `bin/reap`, `bin/crops-score` (recorded again if it differs); (g) `bin/gate` (one plain rerun); on a pass next and main
fast-forwarded to java-night (`next`'s worktree and mac, origin main, mac main), never forced. A failing step's error lines are
matched against each slice's names (or, for Java's tests, its areas); the newest matching slice is reverted (with the slices
depending on it), the uncommitted outputs dropped and the pipeline run again, at most four reverts; a failure no slice matches
stops the night for the morning, nothing reverted and nothing fast-forwarded.

**The day after**: the new failures' classes, `bin/java-check --record` of the levels (the whole corpus with the gate's
`--full`), `bin/reap --record`, `bin/crops-score --record`; JCS190 (the switch's default) on step p's comparison.

## As built: F23 (write-ahead): the `:security` class sorted again, the JCA's tail, SASL

Branch `java-security` (from `next`, `7744e70b4`), 2026-11-08, by day under the light rules: the brief's "raise Java's
completeness by taking on `:security`, its largest deferred class (359 interpreted, 234 compiled)". Nothing converted,
translated or built; the night's commands below. F15 is the plan's `java.lang.invoke`, so this is F23, the next free number.

**The class sorted by what is missing, measured, not scanned.** The records' 359 / 234 are from before night 7's conversion
(b6a6093af: F1 the JCA, F4, F2, F3 converted into `jdk/forms`, every one of the class's first reasons, `ClassNotFoundException:
javax.crypto.Cipher` 72, `KeyStore` 41, `KeyFactory` 24, ..., names a class that is in the world now); the whole-corpus rerun
that records them is the Java night's. So the class's 361 tests (interp's 359 and compiled's 234, together) were run at the
levels read and interp on the gate executable of night 8 (`.tmp/gate-exe/ad3066cf41ea6faf1566`, `next`'s world), two at a
time, javac's tool `javac-5ad357331dbcd7cfb0fd` for the read (`REPR_JAVA_STALE_OK=1`), saved, not recorded (scratch
`.tmp/sec/` of the worktree: `interp-now.tsv`, `groups.json`, the run directories with each test's stderr):
**261 of 361 pass already**, the rest by the cause their stderr names:

| cause | tests | what | where |
|---|---:|---|---|
| AES/GCM | 30 | `Cipher.getInstance("AES/GCM/NoPadding")`: "Cannot find any provider" / "No such algorithm", the services' class `GaloisCounterMode$AESGCM` "not in the closed world": j2g's analysis refused `GaloisCounterMode` and `GCMDecrypt` (the executable's `report.edn`, `:analysis-failures`: "Unable to resolve symbol: NIO_ACCESS (line 647)"), F1's variant having cut the field but not its two users, so every class of the file is a stub; and SunJCE's `AlgorithmParameters.GCM`, `sun.security.util.GCMParameters`, a class no list holds (`Cipher.getParameters`) | **F23** |
| TLS | 17 | ChaCha20's `xor` and the AEAD tag compare: "VarHandle.get on a handle that is not a constant of its class": `ChaCha20Cipher.asLongView` is `byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder())`, and j2g's constant handles took only `BIG_ENDIAN`/`LITTLE_ENDIAN`; with GCM gone too, SunJSSE's TLS 1.3 suites fail (wrap's "internal_error", "No appropriate protocol", the peers waiting: the six timeouts) | **F23** |
| SASL | 11 | `javax.security.sasl`, `com.sun.security.sasl` (JCS133 (a): "SASL deferred, after F1 and F4") | **F23** |
| MD4 | 1 | `sun.security.provider.MD4` (DigestKAT; NTLM's digest) | **F23** |
| Kerberos | 10 | `javax.security.auth.kerberos`'s permissions, principal and keys, `sun.security.krb5` | JCS133 (a) by design; JCS230 |
| serialization | 6 | KeyStore's JCEKS and `SignedObject`, `SealedObject` through the object streams | JCS167 (a)'s port (`java-serial`) |
| jar signing | 5 | signed jars read as unsigned (`ScanSignedJar` x2, `TurkCert`, `SignedJarFileGetInputStream`, `TestIndexedJarWithBadSignature`) | JCS141 (b), F2b; JCS231 |
| LDAP | 4 | `LdapPrincipal` (1, **F23**), `LdapLoginModule` (2) and the LDAP `CertStore` (1): JNDI's LDAP provider | JCS234 |
| reflection | 4 | `Proxy.getProxyClass`, `Class.getProtectionDomain`, `Field` `isRestricted`, `clone` of an interpreted subclass | F14, F21 |
| extendable classes | 3 | "cannot extend java.util.HashSet yet" (2), `X509EncodedKeySpec` (1) | JCS235 |
| `java.home` and cacerts | 4 | `${java.home}/conf/security/java.security` (2), `/lib/security/cacerts` (2: JCS140 (a), decided, not built) | JCS233 |
| the rest | 5 | `MemorySegment` (FFM, by design), OCSP over the network, `getContextClassLoader`, `StrongSeedReader`'s seed file, DoPrivileged's stdout | as named |

**What came in, and how** (jdk26u at `baf63fb`):

| part | how |
|---|---|
| GCM | `overlay/jdk/variants/GaloisCounterMode.ae`: beside F1's cut of `NIO_ACCESS`, `GCMEngine.overlapDetection(ByteBuffer, ByteBuffer)` (its two-direct-buffers branch copies, as the overlapping case does: a Go build's buffer is never direct, F7's `DirectByteBuffer` answers `isDirect` false) and `GCMDecrypt.doFinal(ByteBuffer, ByteBuffer)` (a tag mismatch zeroes an output without an array through its absolute `put`, where the JDK sets its memory by address under the buffer's session), each jdk26u's method as repr/java converts it but that branch |
| GCM's parameters | `sun/security/util/GCMParameters.java` into the closure (`test/jrt/jrt_sources.ae`'s new `jca-tail-sources`); jrt's `JceServices` constructs it (the one class SunJCE names outside its package) |
| ChaCha20 | `arbace.j2g.vh/byte-order-arg`: `ByteOrder.nativeOrder()` is `"LE"` (both targets, amd64 and arm64, are little-endian; jrt's `Unsafe.isBigEndian` answers false), so `asLongView` is a constant view handle (J2G-SPEC §8.5) |
| the DRBG's CTR mechanism | `sun/security/provider/CtrDrbg.java` (`jca-tail-sources`; DRBG's `createImpl` names it: the report's `:unavailable` "CtrDrbg is not in the closed world") |
| MD4 | `sun/security/provider/MD4.java` (`jca-tail-sources`); its own provider's service constructed by jrt's `JrtServices` |
| SASL | `sasl-sources`: java.security.sasl's 28 files (KIND `share:java.security.sasl`: the API's 10, SunSASL's provider and the CRAM-MD5, DIGEST-MD5, EXTERNAL, PLAIN and NTLM mechanisms' 18) and java.base's `com/sun/security/ntlm` (5), 7,417 + 947 lines; `ProviderConfigSasl.ae` (ProviderConfig's third file: F1's and F3's `getProvider` with `SunSASL` by name and class name; SunSASL's own `Provider.Service` subclass constructs its factories, so no services switch) |
| LDAP's principal | `src/jdk.security.auth/share/classes/com/sun/security/auth/LdapPrincipal.java` (over F19's `LdapName`) |
| j2g's names | `Sasl_Provider` (beside `java.security.Provider`), `Digest_FactoryImpl`, `Ntlm_FactoryImpl` (`arbace.j2g.names/renames`): the 43 new classes' Go names against the night 8 executable's 23,035 translated classes, these three the only collisions |

**Light checks** (main's stage 0 as `ARBACE_HOST_EXE`, javac's tool `javac-5ad357331dbcd7cfb0fd`, `REPR_JAVA_STALE_OK=1`, each process
under 1.1 GB):
- the joint javac check (`.tmp/night7/all9-run.sh` on the branch): 2,314 files (2,277 on `next`, +3 tail, +33 SASL and NTLM, +1
  LdapPrincipal), **0 errors**, 4,988 classes (24 s, 0.96 GB);
- the read: `bin/repr-java read-jdk --check` of the 37 new files, `JceServices`, `JrtServices`, `RsaServices` and F5's java.util.logging
  (the SASL mechanisms log through it) as a patch of java.base: all but `LogManager` converted (its patch needs its own module's
  `sun.util.logging.internal`, as F5's own check found), 0 errors in the new files; `--no-javac`: every file native, the forms
  identical byte for byte;
- the variant read back as forms (`in-ns` and three variants); `arbace.j2g.vh` and `arbace.j2g.names` loaded from the checkout on
  stage 0: `byte-order-arg` of `ByteOrder.nativeOrder()` is `"LE"`, the three renames apply;
- `bin/jrt build --arch amd64` and `bin/jrt test --arch amd64`: `arbace/jrt` vets and its tests pass (6.1 s); jrt's Go is unchanged
  by F23 (no new natives); the module's crop packages fail as on `next` (`arbace/expat`, `arbace/pyexpat` do not build, `arbace/oi`'s
  four golden tests), none of F23's;
- the smoke below on today's executable reproduces the four failures F23 fixes (GCM "Cannot find any provider", ChaCha20's
  VarHandle, MD4 not found, the CTR DRBG not constructed, SASL not found).

**JCS135, the closure's growth**: +9,446 Java lines (SASL and NTLM 8,364, CtrDrbg, MD4, GCMParameters and LdapPrincipal 1,082):
about +1.3%; the executable about +1.5 MB by F4's ratio.

**Expected yield** (interp; the compiled level the same tests where j2g translates them: 22 of the GCM ones are `:security` at
compiled, 8 `:j2g-unresolved`; the TLS ones `:j2g-unresolved` 15; SASL `:j2g-unresolved` 8, `:security` 3):

- AES/GCM, 30: `com/sun/crypto/provider/Cipher/AEAD/` AEADBufferTest, Encrypt, GCMLargeDataKAT, GCMParameterSpecTest,
  GCMShortBuffer, GCMShortInput, KeyWrapper, OverlapByteBuffer, ReadWriteSkip, SealedObjectTest, WrongAAD (t07745-t07755),
  `Cipher/AES/` CTR, Padding, Test4512524, Test4512704, Test4517355, Test4626070, TestAESCipher, TestAESWithDefaultProvider,
  TestAESWithRemoveAddProvider, TestCopySafe, TestNonexpanding (t07757-t07771), `TextLength/SameBufferOverwrite` (t07808),
  `javax/crypto/Cipher/CipherInputStreamExceptions`, `TestCipherMode` (t07886, t07893), `CipherSpi/CipherByteBufferOverwriteTest`,
  `ResetByteBuffer` (t07896, t07898), `javax/net/ssl/SSLEngine/NoAuthClientAuth`, `SSLSession/ResumeTLS13withSNI` (t07987, t08000),
  `sun/security/jca/PreferredProviderTest` (t08193);
- TLS, 17: `javax/net/ssl/SSLSession/SessionCacheSizeTests`, `SessionTimeOutTests`, `SSLSocket/ClientExcOnAlert`,
  `TLSCommon/ConcurrentClientAccessTest`, `TestSessionLocalPrincipal`, `templates/SSLEngineTemplate`, `SSLEngine/ExtendedKeySocket`,
  `FinishedPresent`, `LargePacket`, `CheckTlsEngineResults`, `ConnectionTest`, `SSLSession/HttpsURLConnectionLocalCertificateChain`,
  `sun/security/ssl/SSLContextImpl/MD2InTrustAnchor`, `SSLSocketImpl/ClientModeClientAuth`, `SSLSocketSSLEngineCloseInbound`,
  `X509TrustManagerImpl/SelfIssuedCert`, `X509ExtendedTMEnabled` (t07981-t08019, t08265-t08295);
- SASL, 11: `com/sun/security/sasl/digest/` AuthNoUtf8, AuthOnly, AuthRealmChoices, AuthRealms, CheckNegotiatedQOPs,
  HasInitialResponse, Integrity, Privacy, PrivacyRc4 (t07871-t07880), `ntlm/Conformance` (t07881), `util/CheckAccess` (t07882);
- MD4 and LDAP's principal, 2: `sun/security/provider/MessageDigest/DigestKAT` (t08351), `com/sun/security/auth/CreateLdapPrincipals`
  (t07864).

60 tests touched; by the triage's calibration (0.7 of a test unblocked passes; here the causes are measured, not scanned, so
higher) about **45-55 at interp** and **35-45 at compiled** (the TLS tests' second causes are the risk: sockets in one process,
the interpreted loops' speed). With the 261 the night's rerun records, `:security` falls from 359 to about 50 at interp.

**The night's commands** (in the night's worktree with this branch merged, after the Java night's conversion or folded into
it; heavy ones under `bin/heavy-lock`):

```sh
# 0. the joint javac check of the merged tree (0 errors expected; a light minute)
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: expect the sources line +3 share (CtrDrbg, MD4, GCMParameters), +5 share (com/sun/security/ntlm),
#    +28 share:java.security.sasl, +1 share:jdk.security.auth (LdapPrincipal); 0 failed, 0 javac errors, no javac: 0 fallbacks;
#    jdk/forms + about 37 files
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. the package-to-module table (javax.security.sasl, com.sun.security.sasl and its three packages: java.security.sasl),
#    j2g's edge, javac's world's edge less it
bin/jrt modules && bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: no :analysis-failures for GaloisCounterMode or GCMDecrypt, no :unavailable for
#    GaloisCounterMode's classes, ChaCha20Cipher's xor and doFinal, DRBG.createImpl; the renames Sasl_Provider,
#    Digest_FactoryImpl, Ntlm_FactoryImpl; no "a variant for a class not in the inputs"
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. a smoke of F23 (the file is the branch's .tmp/sec/smoke.clj, copied below): expect
#    "gcm hello GCM", "chacha hello world, chacha", "md4 16", "ctr-drbg CTR_DRBG", "sasl CRAM-MD5 true u"
target/arbace-go/amd64/arbace .tmp/f23-smoke.clj
# 5. Java's levels interp and compiled on :security's 361 tests (then the whole corpus with the night's others): saved, not
#    recorded; expect about 45-55 / 35-45 of the 60 above to pass, and the 261 that pass today, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(com/sun/crypto|com/sun/security|javax/crypto|javax/net/ssl|javax/security|java/security|sun/security)' \
  --save .tmp/f23-night/check
```

The smoke (`.tmp/f23-smoke.clj`, each item caught so that one failure does not hide the others):

```clojure
(import '(javax.crypto Cipher KeyGenerator) '(javax.crypto.spec GCMParameterSpec IvParameterSpec)
        '(java.security SecureRandom)
        '(javax.security.auth.callback CallbackHandler NameCallback PasswordCallback))
(defmacro item [n & body] `(println ~n (try (eval '(do ~@body)) (catch Throwable t# (str "FAILED " t#)))))
(item "gcm" (let [k (.generateKey (doto (KeyGenerator/getInstance "AES") (.init 128))) c (Cipher/getInstance "AES/GCM/NoPadding")]
  (.init c Cipher/ENCRYPT_MODE k (GCMParameterSpec. 128 (byte-array 12)))
  (let [ct (.doFinal c (.getBytes "hello")) ps (.getParameters c)]
    (.init c Cipher/DECRYPT_MODE k ps)
    (str (String. (.doFinal c ct)) " " (.getAlgorithm ps)))))
(item "chacha" (let [k (.generateKey (KeyGenerator/getInstance "ChaCha20")) c (Cipher/getInstance "ChaCha20-Poly1305")]
  (.init c Cipher/ENCRYPT_MODE k (IvParameterSpec. (byte-array 12)))
  (let [ct (.doFinal c (.getBytes "hello world, chacha"))]
    (.init c Cipher/DECRYPT_MODE k (IvParameterSpec. (byte-array 12)))
    (String. (.doFinal c ct)))))
(item "md4" (count (.digest (sun.security.provider.MD4/getInstance) (.getBytes "abc"))))
(item "ctr-drbg" (java.security.Security/setProperty "securerandom.drbg.config" "CTR_DRBG")
  (let [r (SecureRandom/getInstance "DRBG")] (.nextBytes r (byte-array 8)) (first (.split (str r) ","))))
(item "sasl" (let [h (reify CallbackHandler (handle [_ cbs] (doseq [cb cbs] (cond (instance? NameCallback cb) (.setName cb "u") (instance? PasswordCallback cb) (.setPassword cb (.toCharArray "p"))))))
                   s (javax.security.sasl.Sasl/createSaslServer "CRAM-MD5" "ldap" "h" nil h)
                   c (javax.security.sasl.Sasl/createSaslClient (into-array String ["CRAM-MD5"]) nil "ldap" "h" nil h)
                   r (.evaluateResponse s (.evaluateChallenge c (.evaluateResponse s (byte-array 0))))]
               (str (.getMechanismName c) " " (.isComplete s) " " (.getAuthorizationID s))))
```

**Risks and fallbacks**: (1) j2g refusing the GCM variant's methods (the forms are the converter's but two branches; `nil` as a
`cond` branch in statement position is the converter's own spelling, TreeMap's): the report names it, and the night's fallback
is F1's variant (GCM stays out, the 30 and the TLS tests wait); (2) another `nativeOrder` view elsewhere now compiled as a
constant (only ChaCha20Cipher's in the world: the analysis made it a stub before); (3) the TLS tests reaching their second causes
(the time limit, the interpreted handshake loops): `:speed`'s measure after the run; (4) a Go name collision the check above
missed (nested classes' derived names): the build's collision check names it, a rename more.

**What is left of `:security`** after F23 and the night's rerun, about 40 at interp: Kerberos 10 (JCS230), serialization 6
(JCS167 (a)'s port), jar signing 5 (JCS231), LDAP 3 (JCS234), reflection 4 (F14, F21), the extendable classes 3 (JCS235),
`java.home` and cacerts 4 (JCS233), and five single causes.

## Proposed (F23)

Numbered from JCS230 (JCS168-JCS229 left to the branches of the same days).

### JCS230 Kerberos's API classes, or Kerberos by design as JCS133 (a) said

JCS133 (a) put Kerberos and GSS-API by design (no KDC, no keytabs), but no class files them: 10 tests of `:security` need them,
and 5 of those are `javax.security.auth.kerberos`'s permissions alone (`DelegationPermission{Collection,Hash,Init}`,
`ServicePermission{Collection,Test}`), which parse principal names as strings and need no KDC.
- (a) A `:kerberos` class, `:kind :design`, by its regex (`javax\.security\.auth\.kerberos|sun\.security\.krb5|jgss`): the 10
  move from deferred to by design. No work; the completeness unchanged (by design is in the denominator), the strict mean
  lowered by nothing.
- (b) The permissions now (java.security.jgss's `DelegationPermission`, `ServicePermission` and their collections, 4 files,
  half a day: 5 tests), the rest by design as (a).
- (c) `javax.security.auth.kerberos` whole with `sun.security.krb5`'s principal names and configuration (no KDC: the keys from
  passwords, `KerberosPrincipal`'s realm from `krb5.conf`), 3 days: about 8 of the 10.
- *Recommended: (b)*: five tests for half a day, and the class says what is by design.

### JCS231 Jar verification (JCS141 (b), F2b) as the next security slice

JCS141 (b) made jar verification a small fix after F2. Its 5 tests are measured now (`ScanSignedJar` x2, `TurkCert`,
`SignedJarFileGetInputStream`, `TestIndexedJarWithBadSignature`; and `CodeSource/CertsMatch`, `JarEntry/GetMethodsReturnClones`
touched): jdk26u's `JarVerifier`, `ManifestEntryVerifier`, `SignatureFileVerifier`, `ManifestDigester` with jrt's `JarFile`
calling the verifier as the JDK's does.
- (a) Now, as F23b on the next Java day: 1.5 days, about 6 tests, the zip and jar tests rerun.
- (b) After F9's night (java.nio.file's change touches `JarFile`'s neighbours): the same, a night later.
- *Recommended: (b)*, the jar tests' world settled first.

### JCS232 ProviderConfig's three variant files folded

`ProviderConfig.ae` (F1), `ProviderConfigJsse.ae` (F3) and `ProviderConfigSasl.ae` (F23) each replace `getProvider`, the last
file's winning, so that the branches merged without conflict.
- (a) Folded into `ProviderConfig.ae` once F23 is on `next` (minutes; the variant's report counts unchanged).
- (b) Kept: each slice adds a file.
- *Recommended: (a)*.

### JCS233 `java.home` for the security files (with JCS140 (a)'s cacerts)

Four tests read `${java.home}/conf/security/java.security` or `${java.home}/lib/security/cacerts` as files; the Go build's
`java.home` is empty (its `java.security` is resource data jrt reads), and JCS140 (a)'s cacerts was decided as data but is not
built.
- (a) `java.home` a directory the executable writes once per build key (the embedded `jdk/data`, about 1 MB with cacerts) under
  the user's cache (`$XDG_CACHE_HOME/arbace/KEY/`), so that the JDK's readers and the tests find the files; cacerts added to the
  data step (JCS140 (a)). A day; the 4 tests and `VerifyCACerts`' list.
- (b) jrt's file layer serving `${java.home}/...` from the embedded data (a virtual directory: `FileInputStream`, `Files` and
  `File.exists` of those paths), no disk write. Two days; the same tests, and a path that is not a file.
- (c) Neither: the 4 tests deferred, cacerts as JCS140 (a) through `FilePaths`' variant alone.
- *Recommended: (a)*: the JDK's own code reads the files it reads on the JDK.

### JCS234 JNDI's LDAP provider

`LdapLoginModule`'s two tests (`CheckConfigs`, `CheckOptions`) and the LDAP `CertStore` (`java/security/cert/GetInstance`) need
`com.sun.jndi.ldap`'s context factory (java.naming, about 30k lines) behind F19's names; no directory server is needed for the
tests' configurations.
- (a) Deferred until JNDI's providers have tests of their own beyond these three.
- (b) com.sun.jndi.ldap now: 3 days for 3 tests.
- *Recommended: (a)*.

### JCS235 `java.util.HashSet` and the key specs among the extendable classes

Three tests define classes extending `java.util.HashSet` (`Subject/UnreliableContains`, `VerifyCACerts`) and
`X509EncodedKeySpec` (`KeyFactoryGetKeySpecForInvalidSpec`): "The Go build's Java forms cannot extend ... yet". F1 added 18 JCA
classes to `arbace.j2g.model/class-supers`.
- (a) `HashSet`, `X509EncodedKeySpec` and `PKCS8EncodedKeySpec` added (JC4b's mechanism, an hour, each a DynSub more).
- (b) A rule: every public non-final JDK class with an accessible constructor extendable (the executable's size: JC4b measured
  one DynSub at a few KB; about 2,000 classes, some MB).
- *Recommended: (a)* now, (b) as its own measurement.

## As built: F24 (write-ahead): the `:net` class sorted, jdk.httpserver, the variants the night's j2g cut

Branch `java-net` from `java-night` (`639d42ab9`), 2026-11-08, by day under the light rules (no j2g, no build, no
conversion; each process under 2 GiB). The brief: raise Java's completeness through `:net` (227 interpreted, 150 compiled
deferred tests in the committed records), sorted by the missing piece, the biggest group ported from jdk26u's own Java. The
number F24 (F23 is `java-security`'s, the same day).

### The class measured on today's world

The records (`test/java/check-{interp,compiled}.edn`) predate the conversion of F7, F8, F10a, F11, F12 and F13 (night step 0,
`b6a6093af`, 2026-11-07): they say nothing of what is still missing. So the class's 230 tests (227 at interp, 3 at compiled
alone) were run **interpreted on the newest gate executable** (`.tmp/gate-exe/ad3066cf41ea6faf1566`, `next` after night 8: F7,
F8, F10a, F11, F12, F13 in; F10b, F9, JCS164 not), their forms from the read's cache (the newest passing read of each test,
copied into the worktree's own cache: `.tmp/net/scripts/linkforms.py`), two at a time, a watchdog stopping any test above 2 GiB
(ExpandingMap): `bin/java-check --levels interp --only <the 230> --save .tmp/net/s1` with `JAVA_CHECK_EXE`,
`JAVA_CHECK_FORMS_ONLY=1`; 1,627 s. **121 of the 227 pass already** (the records will say so after the night's whole-corpus
run); the 108 that do not, by what they stop at:

| tests | the missing piece | whose |
|---:|---|---|
| 42 | datagrams and multicast: `DatagramSocket` (21), `MulticastSocket` (7), `DatagramChannel` (14): today's `DatagramSocket` is cut (F11's variant failed j2g's analysis: `NetMulticastSocket(Object)`) | F10b, tonight |
| 11 | asynchronous channels (`AsynchronousChannelGroup`, `...ServerSocketChannel`, `...FileChannel`, `...Channel`) | JCS220, waits for the night's records |
| 11 | RMI (`java.rmi.Remote`, `LocateRegistry`, `RemoteServer`, `MarshalledObject`, ...) | JCS132; JCS255 below |
| 8 | the HTTP client `java.net.http` (`HttpClient`, `HttpRequest`, `HttpClient$Version`) | F12b's client half; JCS254 below |
| 8 | **jdk.httpserver** (`com.sun.net.httpserver.HttpServer`, `HttpExchange`, `Authenticator$Failure`) | **F24, below** |
| 6 | **`sun.net.www.http.HttpClient` not in the closed world**: F12's variant failed j2g's analysis, so every http: URL connection is cut (OpenStream, Responses, B8025710, MultiThreadTest, ProxyTest, HttpsProxyStackOverflow) | **F24's fix** |
| 2 | **`jdk.internal.logger.DefaultLoggerFinder` not in the closed world**: LogManager's `LoggingProviderAccess.demandLoggerFor` (`isSystem`), which PlatformLogger's JulBridge calls on its first use (Encode, ProtocolRedirect) | **F24's fix** |
| 2 | **`com.sun.management.internal.HotSpotThreadImpl` not in the closed world**: F13's `ThreadImpl` variant failed j2g's analysis (RegisterDuringSelect, SelectAndClose) | **F24's fix** |
| 1 | `java.net.HttpURLConnection` extended by a test (getResponseCode): not in j2g's `class-supers` | **F24** |
| 2 | UNIX domain sockets (`UnixDomainSocketAddress`) | JCS164, tonight |
| 15 | one each: `FileOutputStream` extended (2), `System.inheritedChannel` (2), `Thread.ofPlatform`, `System.runFinalization`, `InetAddress.isReachable`, urgent data (OutOfBand), SCTP, IDN's ICU normalizer (`UseSTD3ASCIIRules`: an initializer's failure), NetParamsTest's interface flags (5 bytes), cookies (B6644726: the order of a store's cookies), and the timeouts InterruptDeadlock, KeySets, ExpandingMap (interrupts, JCS162; memory) | the tail |

The largest group, datagrams, is F10b's (tonight); the next two wait or need a decision; **jdk.httpserver is the largest group
that is nobody's**, and three of the groups are not missing pieces at all but **variants that j2g's analysis refused on the
night of 2026-11-07** (the gate executable's `j2g/report.edn`, `:analysis-failures`: six classes; their dependants are "not in
the closed world"): F12's `HttpClient`, F5's `SimpleFormatter`, F13's `ThreadImpl` (and `DatagramSocket`, F10b's to replace;
`GaloisCounterMode` and its `GCMDecrypt`, `NIO_ACCESS`, left to `java-security`'s JCA line). F24 is the server and those fixes.

### What comes in, and how

**jdk.httpserver** (jdk26u at `baf63fb`; `test/jrt/jrt_sources.ae`'s `httpserver-sources`, one line in `sources`): 43 files,
8,297 lines, all `share:jdk.httpserver` (translated as java.base's, as java.logging's and java.sql's): `com.sun.net.httpserver`'s
API (15: `HttpServer`, `HttpsServer`, `HttpContext`, `HttpExchange`, `HttpsExchange`, `Headers`, `Filter`, `Authenticator`,
`BasicAuthenticator`, `HttpHandler`, `HttpHandlers`, `HttpPrincipal`, `HttpsConfigurator`, `HttpsParameters`, `Request`), its
`spi/HttpServerProvider`, and `sun.net.httpserver`'s server (27): `ServerImpl` over F10a's `ServerSocketChannel` and `Selector`,
its dispatcher thread, the idle and request/response timers over F16's `java.util.Timer`, the exchange's chunked and fixed-length
streams, HTTPS over F3's `SSLEngine` (`SSLStreams`, `HttpsServerImpl`). jdk26u's code unchanged but for:

| part | how |
|---|---|
| System.Logger | jrt's `java.lang.System` is hand-written Go without System.Logger (JCS155). `jdk.internal.jrt.SystemLoggers` (overlay, new) holds jdk26u's `System.Logger` interface as `SystemLoggers.Logger` (its `Level`, its abstract and default methods, verbatim) and `getLogger(name)`: what the JDK's `System.getLogger` answers a system module when java.logging is present, LazyLoggers' lazy logger over `LoggingProviderImpl.JULWrapper`, here the JULWrapper's System.Logger half (verbatim, `spi2JulLevelMapping`) over the java.util.logging logger demanded on first use through F5's `LoggingProviderImpl` access, as PlatformLogger's JulBridge demands it (JCS155 (b)'s way, F3's SSLLogger's and F12's PlatformLogger's) |
| the derived overlays (5) | `ServerImpl`, `ExchangeImpl`, `HttpConnection`, `HttpContextImpl`, `ServerConfig`: jdk26u's files with `import java.lang.System.Logger[.Level]` as `jdk.internal.jrt.SystemLoggers.Logger[.Level]` and `System.getLogger` as `SystemLoggers.getLogger`; and ServerImpl's dispatcher `new Thread(null, dispatcher, "HTTP-Dispatcher", 0, false)` as `new Thread(dispatcher, "HTTP-Dispatcher")` (jrt has no thread groups and no stack sizes; the inheritable thread locals jdk26u does not pass are inherited: the dispatcher reads none). Made by **`test/jrt/derive.ae`** (below), not by hand |
| the variant (1) | `HttpServerProvider.ae`: `provider()` the property's class, else `DefaultHttpServerProvider`; `loadProviderAsService` cut (no ServiceLoader, as SelectorProvider's variant) |
| j2g | renames (`arbace/j2g/names.ae`, J2G-SPEC §4.4: the Go names are the simple names): `com/sun/net/httpserver/Authenticator` → `Hs_Authenticator` (java.net's, SunJSSE's), `Filter` → `Hs_Filter` (java.util.logging's), `sun/net/httpserver/ChunkedInputStream`, `ChunkedOutputStream` → `Hs_...` (sun.net.www.http's), `Event` → `Hs_Event` (jdk.internal.event's), `Request` → `Hs_Request` (the API's `Request`); checked against the gate executable's 5,736 Go types and the branch's closure, nested classes included: no other collision. `class-supers` + `com/sun/net/httpserver/BasicAuthenticator` (BasicAuthToken extends it) and `java/net/HttpURLConnection` (getResponseCode extends it) |
| left out | the simple file server (`SimpleFileServer`, `sun.net.httpserver.simpleserver`: jwebserver's, with one KIND unix file of jdk.httpserver, which print-sources has no kind for: JCS252); its classes' calls are j2g's operation-level stubs. java.net.http's client (JCS254) |

**The variants' fixes** (the night's `:analysis-failures`; each reproduced and fixed on the gate executable's REPL, below):
- F12's `HttpClient.ae`: the added `jrtServerSocket` read the inherited protected field as a bare `serverSocket` ("Unable to
  resolve symbol: serverSocket"); it is `(.-serverSocket this)`, as repr/java converts the class's own reads of it. With it the
  http: handler's connections (`sun.net.www.protocol.http.HttpURLConnection` and the keep-alive cache) are in the world again.
- F5's `SimpleFormatter.ae`: `Function.apply`'s result into the `String` local without a cast ("Cannot convert value for format
  of type Object to String"): `(cast String ...)`. Without the fix every `ConsoleHandler` and `FileHandler`'s default formatter is
  cut (the logging tests, TaskRejectedTest's handler).
- F13's `ThreadImpl.ae`: `Arrays.copyOf`'s `Object[]` into the `StackTraceElement[]` local: `(cast StackTraceElement/1 ...)`; with
  it `HotSpotThreadImpl` and the thread bean's `getThreadInfo` are in the world again.
- F5's LogManager: `LoggingProviderAccess.demandLoggerFor` asks jdk26u's `DefaultLoggerFinder.isSystem` (jdk.internal.logger is
  out of the Go build, JCS136), so every PlatformLogger's first use failed; `LogManager.ae`'s new nested variant keeps the method
  with isSystem's body, `VM.isSystemDomainLoader(module.getClassLoader())` (jrt's hand-written VM function).

**`test/jrt/derive.ae`, `bin/jrt-convert derive [--check] [PATH...]`** (JCS166 (a), decided 2026-11-07, its first part): a derived
overlay is a table entry (jdk26u's file, the exact replacements each with its count, the header naming the origin, the change
and the licenses), written by `derive`, compared byte for byte by `derive --check` (exit 1 on a difference, the overlay's name
printed DIFFERS or MISSING); on the tools' host, seconds. F24's five overlays are its first entries; F7's, F9's and F10a's
scratch derivations are JCS251.

No Go: the server is plain Java over F10a's natives; no new native, `HostChannels` unchanged.

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`; javac's tool `.tmp/gate-exe/javac-5ad357331dbcd7cfb0fd` with `REPR_JAVA_STALE_OK=1`;
`GOMEMLIMIT=1500MiB`; scratch `.tmp/net/`)
- the closure (F3's `closure.py` against the branch's print-sources and jrt's manifest, scope `com.sun.net.httpserver`, `.spi`,
  `sun.net.httpserver`): the 43 files; outside the world and the list only `ServiceLoader` (the variant) and `System.Logger`
  (SystemLoggers); jrt's hand-written members the files call (System, Thread, Class, ClassLoader, String): all in the manifest but
  Thread's five-argument constructor (the derivation);
- **the joint javac check** (`.tmp/night7/all9-run.sh`): **2,464 files, 0 errors**, 5,269 classes (17 s, 1.0 GB; the branch's
  2,420 + 38 share + 5 derived + SystemLoggers); its negative control (SystemLoggers moved away): 64 errors, exit 1;
- `bin/jrt-convert derive --check`: 5 overlays, 0 not as derived; its control (a line appended to HttpContextImpl): DIFFERS, exit 1;
- the read: `bin/repr-java read-jdk --check` of the 44 with java.util.logging and sun.util.logging as a patch of java.base
  (`REPR_JAVA_PATCH_ALL=1`): 64 converted, 0 failed, 0 javac errors (4 s, 0.7 GB); `--no-javac`: 64 native, 0 fallbacks, the forms
  identical;
- the variants applied and parsed (F4's `vet.ae`): HttpServerProvider 1/1/0 (replaced/cut/added), LogManager 4/0/1 with
  `$Cleaner` 1/0/0 and `$LoggingProviderAccess` 1/0/0, SimpleFormatter 1/0/1, ThreadImpl 17/0/3, HttpClient 1/0/1;
- **the fixes analyzed on the gate executable's REPL** (its analyzer is j2g's): ThreadImpl with the variant applied
  (`.tmp/net/vet/emit.ae`, the class renamed) loads, and without the cast fails with the night's message ("Cannot convert value
  for st of type [Ljava/lang/Object; to [Ljava/lang/StackTraceElement;"); the minimal cases of the other two (a String local from
  `Function.apply`; a NetworkClient subclass reading `serverSocket`) fail with the night's messages without the fix and analyze
  with it;
- j2g's changed namespaces load (`class-supers` 127, `Hs_Request`); `bin/jrt test --arch amd64`: `arbace/jrt` ok (the crops'
  `expat`, `pyexpat`, `oi` fail by themselves, as on `next`); `go vet ./jrt` clean for linux/amd64 and arm64.

Not checked by day: j2g's analysis of the 43 new classes and the derived overlays (the night's; their Go names above), and the
server's behaviour (no executable has it).

### Expected yield

Interpreted, of `:net`'s 227: **121 pass on today's world** (unrecorded: the night's whole-corpus record moves them), then F24:
httpserver 8 (t07855-t07862), the http: connection 6 (t05030, t05061, t08121, t08122, t08123, t08137), the logger 2 (t04313,
t08136), the thread bean 2 (t05172, t05174), HttpURLConnection 1 (t04950): **19 touched, about 15 expected**; F10b's and JCS164's
tonight about 34 of their 44. At risk: TruncatedRequestBody's and B6361557's timing at the interpreted level, TaskRejectedTest's
log line (its handler on java.util.logging's logger, demanded as a system logger here as in the JDK). Beyond `:net`: SimpleFormatter
and the LoggingProviderAccess fix are under every test that logs to a console or file handler or reaches a PlatformLogger
(`:logging`'s and the HTTP tests'; not counted here). Compiled: the same tests where `:net`, `:j2g-unresolved` and `:logging` hold
them now (httpserver 8, the http: connection's 6), about **14 expected**. Java eval and AOT each about +0.5 point beyond the
night's other slices.

### The night's commands

In the night's worktree with this branch merged (after java-night's slices; its own revert is one merge), heavy ones under
`bin/heavy-lock`, about 1 hour with the areas' checks, the conversion shared with the night's:

```sh
# 0. the joint javac check and the derivations (light)
/root/arbace/.tmp/night7/all9-run.sh $PWD          # expect 0 errors
bin/jrt-convert derive --check                     # expect 5 overlays, 0 not as derived
# 1. jdk/ converted afresh: expect the night's count + 38 share:jdk.httpserver + 6 of jrt's own (the 5 derived,
#    SystemLoggers), 0 failed, 0 javac errors, no javac: 0 fallbacks; jdk/forms + com/sun/net/httpserver (16) and
#    sun/net/httpserver (27), jdk/internal/jrt/SystemLoggers; then the module table (bin/jrt modules: + com.sun.net.httpserver,
#    .spi and sun.net.httpserver as jdk.httpserver's)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules
# 2. j2g's edge afresh (com.sun.net.httpserver exported, sun.net.httpserver not: the edge's :unexported), javac's world's edge
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: :analysis-failures without HttpClient, SimpleFormatter, ThreadImpl (and without DatagramSocket
#    after F10b), none of sun/net/httpserver or SystemLoggers; variants HttpServerProvider 1/1/0, LogManager$LoggingProviderAccess
#    1/0/0, HttpClient 1/0/1, SimpleFormatter 1/0/1, ThreadImpl 17/0/3; no "a variant for a class not in the inputs"
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
grep -A8 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn
# 4. a smoke: the server on loopback, an http: connection to it, a system logger through java.util.logging; expect "200 ok"
target/arbace-go/amd64/arbace -e '(import (quote (com.sun.net.httpserver HttpServer HttpHandler)) (quote (java.net InetSocketAddress InetAddress URL)))
  (let [s (HttpServer/create (InetSocketAddress. (InetAddress/getLoopbackAddress) 0) 0)]
    (.createContext s "/" (reify HttpHandler (handle [_ x] (let [b (.getBytes "ok")] (.sendResponseHeaders x 200 (alength b))
                                                            (with-open [o (.getResponseBody x)] (.write o b))))))
    (.start s)
    (let [c (.openConnection (URL. (str "http://127.0.0.1:" (.getPort (.getAddress s)) "/")))]
      (println (.getResponseCode c) (slurp (.getInputStream c))))
    (.stop s 0))'
# 5. Java's levels interp and compiled on the class's areas (then the whole corpus with the night's others): saved, not
#    recorded; expect about 15 / 14 of F24's 19 to pass, the 121 to hold, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/(com/sun/net/httpserver|java/net/|java/nio/channels/|sun/net/|java/io/OutputStreamWriter/Encode|java/util/logging)' \
  --save .tmp/f24-night/check
```

**Risks and fallbacks**: (1) a collision or refusal in j2g on the new classes: the fallback drops `httpserver-sources`' line, the
five derived overlays, SystemLoggers and HttpServerProvider.ae (the variants' fixes and the class-supers stay: they need nothing
of the server); (2) a variant fix analyzed differently in the whole world than at the REPL: that variant's old text again and its
class in its class as before; (3) the server's loopback runs under the interpreted level's limits (the dispatcher's selector loop
wakes at least each second, `selector.select(1000)`).

**The day after**: the class's tests judged and recorded with the whole corpus; the 121 now passing and F24's move from `:net`
to pass; the tail's new classes (the sixteen above); doc/go/JRT-NOTES.md's "Sockets" (the server) and "Logging" (SystemLoggers).

## Proposed (F24)

### JCS250 System.Logger in jrt (JCS155 (a)), or SystemLoggers as the classes' Logger

F24's server logs through `jdk.internal.jrt.SystemLoggers.Logger`, jdk26u's interface under another name, its five files derived
for the import. JCS155 decided (a) "as its own fix": `System.Logger`, `Level` and `LoggerFinder` as jrt's hand-written types, the
default methods as package functions (F14's AnnotatedElement shows the shape: `Iface_Method(this, ...)`, which j2g's forwarders
name), `System.getLogger` as j2g's own static (`m/j2g-static?`, as `System.getProperties`) over `SystemLoggers.getLogger`.
- (a) Now, after F24's night: the derivations dropped (jdk26u's files as they are), SSLLogger's and ObjectInputStream's
  System.Logger paths and LoggerInterfaceTest (`:logging`, 1 test) gained; 1.5 days by day plus a night (j2g's first
  hand-written interface with defaults that translated classes implement).
- (b) SystemLoggers stays the classes' Logger, a derivation for each new user.
- *Recommended: (a)*, after the night proves SystemLoggers' behaviour (its JULWrapper half moves into the hand-written type's
  implementation unchanged).

### JCS251 F7's, F9's and F10a's derivations into `test/jrt/derive.ae`

JCS166 (a) decided the derivations as Arbace with `derive --check`; F24 built the table and the verb with its own five. F7's
DirectByteBuffer (gendirect.py), F10a's Net, PollSelectorImpl and SocketOptionRegistry (gennet.py, genpoll.py, genopts.py) and F9's
thirteen (derive.py) are scratch Python in worktrees' `.tmp/`, some with multi-line rewrites (Net's natives' bodies) the table's
exact replacements express as long OLD/NEW pairs. (a) Moved, each overlay re-derived byte for byte, 1 day; then `derive --check`
in the essential gate (beside `bin/jrt-convert check-javac`, JCS165), a light step; (b) only new derivations in the table.
*Recommended: (a).*

### JCS252 The simple file server (SimpleFileServer, jwebserver)

`sun.net.httpserver.simpleserver` has one file of KIND unix in jdk.httpserver (`URIPathSegment`); print-sources and the
conversion have unix only for java.base. (a) A KIND `unix:MODULE` in print-sources, bin/jrt-convert's tree and the joint check,
the six files in (with FileServerHandler derived for System.Logger); 0.5 day; the corpus has no SimpleFileServer test today; (b) out.
*Recommended: (b)* until a test of the corpus names it.

### JCS253 Variants analyzed by day

Six classes left the night's world on 2026-11-07 because their variants, parsed by day, failed j2g's analysis (F12's, F5's, F13's,
F11's, F1's): their dependants went with them (every http: connection, every console handler's formatter, the thread bean), and
the records cannot show it until a whole-corpus run. F24 analyzed its fixes on a gate executable's REPL. (a) `bin/jrt-convert vet`:
each variant applied to its class's forms and the class analyzed by `arbace.classes.analyze` in the committed world (j2g's world
load without translation; to be measured, it may be heavy: a night step then, before the build); (b) the REPL way, the class
renamed and loaded on the gate executable (light, but a class whose dependants are cut cannot load, and package-private access
differs); (c) as now, the build's report. *Recommended: (a)* if its world load stays light, else (b) for every new variant.

### JCS254 java.net.http's client

8 tests (`HttpClient`, `HttpRequest`, `Version`). The module is 313 files, 101,314 lines: HTTP/1.1 and HTTP/2 over F10a's
selectors and F3's TLS, HTTP/3 over QUIC (DatagramChannel, F10b; QUIC's TLS engine, JCS156 (a)), WebSocket. (a) All of it, after
F10b's records, 4-5 days; (b) HTTP/1.1, HTTP/2 and WebSocket, HTTP/3 cut by variants (`HttpClientImpl`, `ExchangeImpl`,
`MultiExchange`, `Exchange`: 40 references), 3 days; (c) deferred. *Recommended: (b)* after F10b's night, (a) when QUIC's tests
come with it (the corpus has none today).

### JCS255 RMI now that serialization is ported

JCS132 recommended RMI by design beside serialization (the user's JCS128-JCS135 as recommended, 2026-11-05), but no class
applied it (`:net` matches `java\.rmi`: 11 tests), and JCS167 (a) has since ported serialization. (a) Apply JCS132 (a): a `:rmi`
`:design` class before `:net` (11 interpreted, 12 compiled); (b) RMI deferred again, ported after JCS167's slices (java.rmi's
registry, the transport over sockets, stubs over Dyn's proxies: JCS170), 3-4 days. *Recommended: (b)*, as JCS167 reversed the
reason JCS132 gave; (a) only if the strict count must move before then.

## Decided (the user, 2026-11-08: JCS168-JCS171)

**JCS168 (a)**: j2g's default serialVersionUID ahead of time, the night's `J2G_DEFAULT_SUID=1` run (the shipped default
remains JCS190's, on that run's comparison); **JCS169 (a)** after slices 2-4: serializable lambdas (built below);
**JCS170 (a)**: `java.lang.reflect.Proxy` over Dyn at both levels, with serialization's proxy class descriptors (built
below); **JCS171 (a)**: the executable's growth accepted (measured at the night's build, below). JCS191 (the locks'
serial forms), JCS192 and JCS193 are not decided: left for the user.

## As built: serialization slice 4 (write-ahead)

Branch `java-serial4` (from `mac/java-night` 639d42ab9), 2026-11-08, by day under the light rules: JCS167's fourth
slice ("The remaining slices": the JDK variants that cut serialization members, restored). Slice 1 brought
`java.io`'s streams into the world, so the members j2g had stubbed for naming them (`ObjectInputStream`,
`ObjectStreamField`, `Externalizable` outside the world: `KeepAliveCache`'s `writeObject`, `SerialBlob`'s and
`SerialClob`'s hooks, `UnixDomainSocketAddress`'s `readObject`, `DirectoryIteratorException`'s, the `<clinit>`s of the
classes with `serialPersistentFields`) translate as they are. What was left is what the variants and the source list cut
by hand, found by a scan of every world file's serialization members (`.tmp/s4/hookscan.py`: the bodies of
`readObject`, `writeObject`, `readObjectNoData`, `readResolve`, `writeReplace`, `readExternal`, `writeExternal`, the
`serialPersistentFields` initializers and the methods taking a stream, against the world: the joint check's sources and
jrt's classes): 24 classes named 4 classes outside it.

| what | was | now |
|---|---|---|
| `DESKey`, `DESedeKey`, `PBEKey`, `PasswordCallback` | `readObject` cut (`PBEKey`, `PasswordCallback`) or naming `CleanerFactory` (F17's Cleaner is not in the world) | `readObject` jdk26u's in the variants without the Cleaner's registration, as their constructors (the key zeroed by `destroy`, the password by `clearPassword`; `PBEKey` and `PasswordCallback` keep the clearing action as their `cleanable`) |
| `LogRecord`, `URL` | `serialPersistentFields` cut (its initializer named `ObjectStreamField`) | jdk26u's (the variants' cut removed) |
| `SealedObject` | its static initializer cut (`SharedSecrets.setJavaxCryptoSealedObjectAccess`, which jrt's hand-written SharedSecrets has not) | the variant removed: `JavaxCryptoSealedObjectAccess` is one of j2g's serialization accessors (`m/serial-accessors`, slice 1's mechanism: set by SealedObject's initializer, its getter initializing SealedObject first), so JCEKS's secret keys (`SealedObjectForKeyProtector.getKey`) work, "by design" no longer |
| `java.time.Ser`, `java.time.chrono.Ser` | out of `time-sources` (Externalizable outside the world): the `writeReplace` of 16 java.time classes and 4 of java.time.chrono stubbed | in `time-sources`; chrono's cases of the Japanese, Hijrah, Minguo and Thai Buddhist dates (not in the world) are operation-level stubs |
| `ConcurrentSkipListMap.readObject` | names `VarHandle` | its fences are j2g's (`vh.ae`), nothing to do |

`test/jrt/jrt_sources.ae`'s comments that said serialization was outside the build are corrected (SerialBlob, the
java.io exceptions, the JCA's SealedObject, JCEKS, UnixDomainSocketAddress); LICENSE.md's line for `SealedObject.ae`.
Each restored variant applied to its class's committed forms by j2g's own `apply-variant` (`.tmp/s4/vcheck.ae`, stage 0):
DESKey and DESedeKey 2 replaced, PBEKey and PasswordCallback 4 replaced, each with a `readObject` and no
`CleanerFactory`; LogRecord and URL with their `serialPersistentFields`.

## As built: JCS170 (write-ahead)

The user's JCS170 (a): `java.lang.reflect.Proxy` over Dyn, at both levels, with serialization's proxy class descriptors.

- **jrt** (`reflect.ae`, `serial.ae`, `serial_classes.ae`, `access.ae`): `Proxy.newProxyInstance` (the handler required,
  the class's constructor of `InvocationHandler` called), `getProxyClass` (ObjectInputStream's `resolveProxyClass`),
  `isProxyClass` (the classes registered by `RegisterProxyClass`) and the new `getInvocationHandler` ("not a proxy
  instance"), over `ProxyClassFor`, a hook arbace/lang's `j2g_dyn.go` sets (`dynSetProxyHook`); without the evaluator's Dyn
  they throw `UnsupportedOperationException` as before. `Proxy` is Serializable with jdk26u's serial form
  (serialVersionUID -2222568056686623797, the protected field `h` read and written through the proxy object's first field,
  `proxyHandlerField`); `InvocationHandler_Cast`, so that Dyn has the interface's method (an interpreted or deftype handler
  is a Dyn: without it the handler's cast failed, found by the day's check) and its `IsInstance`; `JavaLangAccess.setCause`
  (UndeclaredThrowableException's `readObject`). `bin/jrt manifest`: Proxy gains `getInvocationHandler` and
  `java.io.Serializable`; "edge members ... 0".
- **arbace/lang** (`Compiler.ae`): `Compiler$ProxySupport.proxyClass` follows jdk26u's `Proxy.ProxyBuilder` (the interface
  limit, "is not an interface", hidden, sealed, "repeated interface", null elements; non-public interfaces of one package,
  "cannot have non-public interfaces in different packages"; the name `jdk.proxy1.$ProxyN` for public interfaces, the
  non-public ones' package otherwise; modifiers `public final` or `final`; one class per list of interfaces) and
  `ProxyGenerator`'s methods (Object's `hashCode`, `equals`, `toString` first, each interface's non-static methods in
  order, one method per signature and return type, the first interface's `Method`, the duplicates' exceptions narrowed by
  `collectCompatibleTypes`, `checkReturnTypes`' "methods with same signature ... but incompatible return types"); each
  method a `Compiler$ProxyFn` (a RestFn: the handler's `invoke` with the proxy, the Method and the arguments or null; the
  result checked as the generated code checks it: null for a primitive NullPointerException, another type
  ClassCastException; Errors and RuntimeExceptions propagate, a checked exception the method does not declare is wrapped in
  `UndeclaredThrowableException`). `Compiler$Dyn.defineJavaProxy` (a native, `arbace.j2g.dyn`): a Dyn class whose
  superclass is Proxy, no fields (the handler the object's field 0), registered as a proxy class, `SerialNew` an object
  without a handler (read into it through Proxy's `h`).
- **j2g** (`dyn.ae`): the native, the hook, `proxyClass` as a root, and Dyn over a program's own non-public interfaces too
  (`nm/own-class?`, package lang: a compiled test's package-private interface can be proxied).
- **The world**: jdk26u's `java/lang/reflect/UndeclaredThrowableException.java` (`serial-sources`).

**Divergence, recorded**: jrt's member tables keep no throws clauses (`Method.getExceptionTypes` is empty), so a proxy's
method whose exceptions are unknown lets a checked exception through instead of wrapping it (JCS240 proposes the
clauses). `InvocationHandler.invokeDefault` is not built (JCS241).

**Checked by day**: `go test ./jrt` passes, `TestProxy` among them (`proxy_test.ae`: the hook's absence, a stand-in
ProxyClassFor, isProxyClass, getProxyClass's one class, getInvocationHandler and its refusal, Proxy's serialVersionUID and
`h` read and written by reflection); `go vet ./jrt` on amd64 and arm64. The rules of `ProxySupport` and `ProxyFn`,
interpreted on stage 0 over `defineProxyClass` (a copy, `.tmp/s4/forms/jcs170`, the handler an IFn there: stage 0's Dyn
has not InvocationHandler): Runnable's run, Function's apply, Comparable's compareTo, the three Object methods with Object's
Methods, one class per list (`jdk.proxy1.$Proxy1` twice), "java.lang.String is not an interface", "repeated interface:
java.lang.Runnable", the null element's NullPointerException, a declared exception through, null for an int's
NullPointerException, a String for an int's ClassCastException, "methods with same signature length() but incompatible
return types: int and others" (CharSequence and java.sql.Clob), Supplier and Callable's covariant `get`/`call` accepted.
Not checked by day (no build): the Java forms of `Compiler.ae` in the build's analysis, j2g's output, the real streams.

## As built: JCS169 (write-ahead)

The user's JCS169 (a), after slices 2-4: serializable lambdas and method references. javac compiles one into an
`altMetafactory` call with FLAG_SERIALIZABLE (its class's `writeReplace` answers a `SerializedLambda`) and a
`$deserializeLambda$` in the capturing class (a switch over the SerializedLambda that makes the lambda again from its
captured arguments, called by `SerializedLambda.readResolve`). The Go build has neither invokedynamic nor classes made
for translated lambdas, so the two halves meet in jrt's own Java:

- **`jdk.internal.jrt.SerialLambdas`** (overlay): a site registers what its SerializedLambda holds (the capturing class,
  the functional interface's class, method and signature, the implementation's REF_ kind, class, name and signature, the
  instantiated type) with a `Factory` of the lambda from captured values; a lambda's object holds an `Instance` (its site
  and captured values); `replace` is the SerializedLambda; `deserialize` finds the site by the capturing class and the
  implementation's name and signature and checks the rest as `$deserializeLambda$` does, else jdk26u's
  InvalidObjectException "ReflectiveOperationException during deserialization" around javac's IllegalArgumentException
  "Invalid lambda deserialization".
- **jdk26u's `SerializedLambda`** in the world (`serial-sources`), its variant (`overlay/jdk/variants/SerializedLambda.ae`):
  `readResolve` is `SerialLambdas.deserialize` (no `$deserializeLambda$` to call), `toString` names the kind without
  java.lang.invoke's MethodHandleInfo.
- **j2g** (`code.ae`'s `serial-lambda-val`, `out.ae`'s adapters, `main.ae`'s roots): a serializable lambda's or method
  reference's value is made by a factory closure of its captured values, the site's own adapter code with the captured
  Go names (the receiver `this`, its struct `t` when not a leaf, the captured locals, a bound reference's receiver)
  shadowed by the factory's arguments, so there is one translation of the body; the site registers itself and calls the
  factory with its values; the adapter of a serializable interface (or with the Serializable marker) has a field `Ser`
  and the member `writeReplace` (private final, as InnerClassLambdaMetafactory's) calling `SerialLambdas.replace`.
  The implementation's facts are javac's: the analyzer's serializable lambda name (`lambda$m$hash$N`, LambdaToMethod's),
  REF_invokeStatic or, capturing this, REF_invokeSpecial; a method reference's own method and kind.
- **The interpreter** (`arbace.classes.interp/serial-lambda!`): a serializable site's lambda class is registered when built,
  its factory allocating the class's object with the captured values as its fields, and given `writeReplace` through the
  host (`Compiler$CFGo.setMethod`); and the class of an intersection-cast lambda now implements Serializable
  (`lambda-markers`, as j2g's): `(IntSupplier & Serializable) () -> 1` was not Serializable at the interpreted level, a
  bug found by the day's check.

**Divergences, recorded**: the capturing class has no `$deserializeLambda$` (reflection does not list it); an interpreted
site registers when its method is built, so a stream read before the site ran in this process is refused (a translated
site likewise registers when it runs; JCS242 proposes registering at class initialization); an adapter of a Clojure fn to
a serializable interface (no site) is refused with NotSerializableException.

**Checked by day**: the joint javac check (SerialLambdas, SerializedLambda); the interpreter's sites on stage 0 over a
stand-in of SerialLambdas (`.tmp/s4/sl`, `interp.ae` as a copy without slice 1's runtime calls): `(IntSupplier &
Serializable) () -> x + 1` registered as `[jcs169/T IntSupplier getAsInt ()I 6 jcs169/T lambda$st$97ab7abf$1 (I)I ()I]`
with captured `[5]`, its factory's lambda answering 6; an instance lambda of a Serializable interface `[... 7 ...
lambda$inst$8783a704$1 (I)I ()I]` with this and 3 captured, remade to 103; a plain Runnable without writeReplace. The
night's check with the real streams is `test/jrt/serialization/lambda/check.ae` (its sample in `src/`, forms in `forms/`).

## JCS171 (a): the growth, measured at the night's build

Accepted as decided; the night records the executable's size against the night before's (step 3 below) in this file.

## The night's commands (slice 4, JCS169, JCS170)

After the Java night, in a worktree of `java-night` (or `next`) with `java-serial4` merged; heavy commands under the lock:

```sh
# 0. the joint javac check (0 errors: 2,425 files, 5,200 classes on this branch)
.tmp/night7/all9-run.sh $PWD
# 1. jdk/ converted afresh: the sources line +2 share (java/time/Ser, java/time/chrono/Ser), +2 share (UndeclaredThrowableException,
#    SerializedLambda), +1 overlay (SerialLambdas); 0 failed, 0 javac errors, no javac: 0 fallbacks
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. the edges (SerialLambdas, SerializedLambda, JavaxCryptoSealedObjectAccess's accessor, JavaLangAccess.setCause)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable (JCS168's switch on, as the Java night's); j2g's report: no native without its Go function
#    (defineJavaProxy), the variants' counts (DESKey 2, DESedeKey 2, PBEKey 4, PasswordCallback 4, LogRecord 1+1, URL 1/2 cut,
#    SerializedLambda 2+1); the size against the night before's (JCS171), recorded here
J2G_DEFAULT_SUID=1 bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test && ls -l target/arbace-go/amd64/arbace
# 4. the smokes: "proxy-check: 9 of 9", "lambda-check: 6 of 6", and slice 1's (the Java night's) again
target/arbace-go/amd64/arbace -e '(load-file "test/jrt/serialization/proxy_check.ae")'
ARBACE_PATH=$PWD:$PWD/test/jrt/serialization/lambda/forms target/arbace-go/amd64/arbace -e '(load-file "test/jrt/serialization/lambda/check.ae")'
# 5. Java's levels on the areas: saved, not recorded; no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/lang/reflect/Proxy/|jdk/java/io/Serializable/(proxy|resolveProxyClass)|jdk/java/io/ObjectInputStream/(ResolveProxyClass|TestObjectStreamClass)|jdk/java/lang/annotation/UnitTest|jdk/java/lang/invoke/lambda/[A-Za-z]*Serializ|langtools/tools/javac/lambda/(SerializableObjectMethods|SerializedLambdaIn|LambdaLambdaSerialized|GenericMethodRefImplClass)|jdk/java/util/concurrent/atomic/Serial|jdk/java/util/logging/SerializeLogRecord|jdk/java/net/URL/(Test|RestoreURL)|jdk/com/sun/crypto/provider/Cipher/DES/Sealtest|jdk/java/time/' \
  --save .tmp/serial4-night/check
# 6. the day after: the new failures' classes (narrower than :serialization), bin/reap --record, bin/crops-score --record
```

**Risks and fallbacks**: (1) `Compiler.ae`'s ProxySupport or ProxyFn fails the build's analysis: revert `Compiler.ae`'s
two classes and `defineJavaProxy`, `dyn.ae`'s hook falls back to its `false` form (Proxy throws as before), the rest
stands; (2) j2g's factory closure does not compile (a captured name the shadowing misses: Go's "declared and not used"
or an undefined name): `serial-site?` false (one line, `code.ae`) leaves serializable lambdas as before (not
deserializable), the interpreter's half stands; (3) Dyn over the own non-public interfaces breaks a compiled program:
revert that one `or`; (4) `SerializedLambda`'s variant refuses (its converted shape differs): drop the source line and the
variant with SerialLambdas (j2g's and the interpreter's halves are inert without `jdk/internal/jrt/SerialLambdas`).

### Expected yield

Of the class's tests (slice 1's scan, `.tmp/js/classify.txt` of `java-serial`): the proxies' 11 (`java/io/Serializable/proxy`,
`resolveProxyClass`, `ObjectInputStream/ResolveProxyClass`, `TestObjectStreamClass`, `annotation/UnitTest`,
`ManagementFactory/ThreadMXBeanProxy`, `ResponseCache/getResponseCode`, `URL/RestoreURL`, `URL/Test`) less those needing
class loaders or the security manager, and the 9 lambdas' (`LambdaSerialization`, `RepetitiveLambdaSerialization`,
`atomic/Serial`, javac's `SerializedLambdaIn*`, `LambdaLambdaSerialized`, `SerializableObjectMethods`,
`GenericMethodRefImplClass`; `LambdaClassLoaderSerialization` needs a loader): about **10-14** at each level; java.lang.reflect's
9 Proxy tests in the corpus (Basic1, Boxing, CharType, NullClassLoader, ProxyClashTest, nonJavaNames, nonPublicProxy's two,
returnTypes; outside `:serialization`, in reflection's class) about **5-7**; slice 4's: logging's `SerializeLogRecordTest`, the
DES `Sealtest`, java.time's serialization cases, about **3-6**. Together about +20-25 at eval and +15-20 at AOT: Java's two cells
about +0.6 point each.

## Proposed (serialization slice 4, JCS169-JCS170)

Numbered from JCS240 (JCS220-JCS222 are F10b's, JCS230+ left to the day's other branches).

### JCS240 Throws clauses in the member tables

jrt's `Method.getExceptionTypes` and `Constructor.getExceptionTypes` are empty: the tables keep no throws clauses, though
j2g knows them (`jdk/edge.edn`'s `:throws`, the Java forms' `:throws`) and the interpreter too. A proxy cannot tell a
declared checked exception from an undeclared one (JCS170 lets every one through), and reflection's `getExceptionTypes`,
`toGenericString` and `Executable.toString` print none. (a) j2g writes them in the tables' lines (an `E` suffix of class
names, decoded lazily), the interpreter publishes its methods' and constructors', `ProxyFn` then wraps as the JDK: half a
day, the tables a few hundred KB larger; (b) only for the methods of interfaces (a proxy's), smaller; (c) as now.
**Recommended: (a)**: reflection's answers are the JDK's, and the size is JCS171's kind of growth.

### JCS241 InvocationHandler.invokeDefault

Java 16's `InvocationHandler.invokeDefault(proxy, method, args)` runs an interface's default method on a proxy. (a) jrt's
static over the default method's Go function (j2g's `Iface_m` defaults, by the Method's key) and, for an interpreted
interface, the interpreter's method: half a day; (b) UnsupportedOperationException as now (the method is missing).
**Recommended: (a)** with JCS240.

### JCS242 Serializable lambdas' sites registered at class initialization

A site registers when it runs (the interpreter: when its method is built), so a stream written by another process (or
before the lambda was made in this one) cannot be read: javac's `$deserializeLambda$` exists from the class's loading.
(a) j2g registers each class's serializable sites in its initialization (their factories as package-level functions of the
captured values, the body translated a second time) and the interpreter when the class is defined (its lambdas' classes
made eagerly): a day; (b) as now. **Recommended: (a)** when a test reads lambdas from a file (none of the class's do now).

### JCS243 A deftype's and a record's serialization (JCS167's slice 8 first)

The day's proxy check cannot serialize a proxy whose handler is a deftype: Clojure's own types have no SerialNew and no
serial fields (CDR10's remainder, slice 8). (a) slice 8 next (a deftype's and defrecord's Dyn allocation, their fields as
serial fields, `arbace.lang`'s hooks): a day; (b) after slices 5-7. **Recommended: (a)**: Clojure's suite's `serialization`
9 and every Clojure handler of a serialized proxy.

## Decided (the user, 2026-11-08, about 19:45: F23's, F24's and serialization slice 4's proposals)

To build, and built below ("As built: JCS232, JCS235, JCS251, JCS253"):

- **JCS232 (a):** ProviderConfig's three variant files are folded into one.
- **JCS235 (a):** `HashSet`, `X509EncodedKeySpec` and `PKCS8EncodedKeySpec` become extendable classes.
- **JCS251 (a):** F7's, F9's and F10a's scratch derivations move into `test/jrt/derive.ae`, and `derive --check` becomes a
  light gate step.
- **JCS253:** variants are checked by day with j2g's analyzer. This is a light command: it applies every variant and runs j2g's
  analysis on the variant classes alone, and it fails on a refused one.

Decided as texts only, nothing built:

- **JCS230 (b):** the Kerberos permissions are ported now: java.security.jgss's `DelegationPermission`, `ServicePermission` and
  their collections, 4 files, about half a day for 5 tests. The rest of Kerberos is by design, as option (a) put it: a
  `:kerberos` class, `:kind :design`.
- **JCS231:** jar verification comes after F9's night, which is option (b).
- **JCS233 (a):** `java.home` is a directory the executable writes once per build key, under the user's cache. Cacerts joins the
  data step (JCS140 (a)).
- **JCS234 (a):** JNDI's LDAP provider is deferred until JNDI's providers have tests of their own.
- **JCS240 (a):** j2g writes throws clauses into the member tables, and `ProxyFn` wraps exceptions as the JDK does.
- **JCS241 (a):** `InvocationHandler.invokeDefault` is built, with JCS240.
- **JCS242 (a):** j2g registers serializable lambdas' sites at class initialization, and the interpreter registers them when the
  class is defined.
- **JCS243 (a):** serialization's slice 8 comes next: deftypes' and records' serialization.
- **JCS250:** the triage records it as decided with no option named. Read as recommended, that is (a): `System.Logger` in jrt as
  JCS155 (a) decided, after F24's night, with the derivations' Logger renaming dropped.
- **JCS252 (b):** the simple file server stays out until a test of the corpus names it.
- **JCS254 (b):** java.net.http's client is HTTP/1.1, HTTP/2 and WebSocket, with HTTP/3 cut by variants, after F10b's night.
- **JCS255 (b):** RMI is deferred again and ported after JCS167's slices.

## As built: JCS232, JCS235, JCS251, JCS253 (2026-11-08; branch `java-props`, write-ahead)

Branch `java-props` started from `mac/java-night` (`639d42ab9`) with `mac/java-jcv7`, `mac/java-security`, `mac/java-net` and
`mac/java-serial4` merged into it. It is the Java night's input. All work was done by day under the light rules.

**JCS232 (a).**

- `overlay/jdk/variants/ProviderConfig.ae` is now the one variant of `sun.security.jca.ProviderConfig`. Its `getProvider` is
  F23's: SUN, SunRsaSign, SunJCE, SunEC, SunJSSE and SunSASL are made directly, each found by its name and by its class name.
- `ProviderConfigJsse.ae` (F3) and `ProviderConfigSasl.ae` (F23) are removed.
- Nothing translates differently. Before the fold, j2g applied the three files in order and the last one's `getProvider` won;
  the folded file holds that same text.
- `LICENSE.md`, `test/jrt/jrt_sources.ae`'s comments and doc/go/JRT-SOURCES.md name the fold.

**JCS235 (a).**

- `arbace.j2g.model/class-supers` gains `java/util/HashSet`, `java/security/spec/X509EncodedKeySpec` and
  `java/security/spec/PKCS8EncodedKeySpec`, so the list now has 130 classes. Each one gets its DynSub_C at the next build.
- The tests it is for are t08045 (`Subject/UnreliableContains`), t08202 (`VerifyCACerts`) and t05275
  (`KeyFactoryGetKeySpecForInvalidSpec`). Today they stop at "The Go build's Java forms cannot extend ... yet".
- These tests can only be checked at night: the change needs a whole-program j2g run, and a new executable to interpret them on.

**JCS251 (a).** `test/jrt/derive.ae` holds the scratch derivations as tables taken from the scripts' own data.

| derivation | script it replaces | what the table does |
|---|---|---|
| F7's `DirectByteBuffer` and `DirectByteBufferR` | `gendirect.py` | Starts from the JDK build's generated `HeapByteBuffer(R).java`. Applies its renames and mapping replacements, each with its own count for the plain and the read-only file. Its one regex is `$1, mapping)`. Inserts the constructors before the marker. |
| F10a's `Net` | `gennet.py` | Gives each native declaration its body. These are gennet's 40 bodies, with F10b's eight multicast bodies, which were later edited into the overlay by hand. The helpers follow, without F10b's removed `multicast()`. Verified: no `native` left outside comments. |
| `PollSelectorImpl` | `genpoll.py` | 15 replacements. Verified: `pollArray.`, `Unsafe` and `Blocker` are gone. |
| `SocketOptionRegistry` | `genopts.py` | Starts from the template, as GensrcMisc.gmk does, with Linux's constants. Verified: its option tuples equal the JDK build's gensrc. |
| F9's thirteen | `.tmp/f9/derive.py` | Each replacement once. |
| JCS164's `UnixDomainSockets` | `.tmp/unix/derive.py` | Four replacements. Verified: no native declaration is left. |

- **The engine.** An entry's `:ops` are applied in order. An op is `[OLD NEW COUNT]` (OLD a string or a regex) or a function of
  the text. `:verify` is a function of the overlay that returns its problems.
- **The result.** `bin/jrt-convert derive --check` re-derives 24 overlays and finds 0 not as derived, so all 24 are byte for
  byte the committed files. They are F24's five plus the 19 above.
  - The headers are unchanged, so they still name the scripts that first made them. The overlays did not move, so no conversion
    is needed.
  - The scripts' own outputs had been checked first: each reproduces its overlay, except Net's (F10b's hand edits), which the
    table now includes.
- **The negative control.** It is part of `--check`: the PollSelectorImpl entry, with one count raised by one, must throw, and an
  overlay one character off must differ. A line appended to Net.java gives DIFFERS and exit 1.
- **The gate step.** `jrt-derive` is an essential step on stage 0, keyed by the platform's pin, the overlays, `derive.ae`, `bin/jrt-convert`
  and its libraries. It takes seconds.

**JCS253.** `bin/jrt-convert vet [--forms DIR] [--variants DIR] [--known FILE] [--strict] [--others] [FILE...]`
(`test/jrt/vet.ae`, `jrt.vet`).

- **What it runs.** j2g's own world steps: the committed `jdk/forms` (or `--forms`) is read, and the variants are applied by
  `arbace.j2g.world/apply-variant`. Every class is parsed, declared, header-resolved, member-resolved and bridged. Only the
  variants' classes and their nests then go through `analyze-class!`.
- **What resolves the rest.** The classes outside the forms come from j2g's edge (`jdk/edge.edn`), with `a/*source-lookup*`
  false, as in j2g's run.
- **Why it is light.** The heavy part of j2g's load is analyzing every class, and this skips it. The whole world (4,530 classes)
  is declared in about 20 s, at a peak RSS of 1.48 GiB (`GOMEMLIMIT` defaults to 1500 MiB).
- **What fails it.** A variant's class that fails at any step, or a variant whose class has no forms (MISSING), unless
  `test/jrt/vet-known.edn` names it. The file is empty today. A refusal that follows from another class's failed declaration
  prints that cause under it (`after`).
- **Proved: the negative control.** The three variants F24 fixed were restored to their texts before the fix (`--variants` over a
  copy). Vet refuses them with the night's own messages:
  - SimpleFormatter: "Cannot convert value for format of type Object to String";
  - ThreadImpl: "... [Ljava/lang/Object; to [Ljava/lang/StackTraceElement;";
  - HttpClient: "Unable to resolve symbol: serverSocket".
- **Proved: the fixed texts.** The texts as fixed pass: ThreadImpl 17/0/3, SimpleFormatter 1/0/1, HttpClient 1/0/1.
- **Today's result on the committed `jdk/forms`.** 146 variant files, 9 refused. Each refusal is the committed conversion being
  older than the day's sources:
  - five MISSING, classes not converted yet: SerializedLambda, HttpServerProvider, DatagramSocketAdaptor's two and
    DatagramChannelImpl;
  - `File` (F9's `java.nio.file.FileSystem`);
  - `DatagramSocket` (F10b's `MulticastChannel`);
  - `AbstractInterruptibleChannel` (F9's `HostThreads.blockedOn`);
  - `ProviderConfig` (F23's `com.sun.security.sasl.Provider`).

  Each goes away with the night's conversion. So vet is meant to run on the conversion's output, before j2g:
  `bin/jrt-convert vet --forms .tmp/jrt/conv`, or after `bin/jrt-convert update` on `jdk/forms`.
- **No gate step yet.** On today's committed forms the step would fail. JCS256 proposes it.

## Proposed (java-props)

### JCS256 `bin/jrt-convert vet` as a gate step

- **(a)** An essential light step after `jrt-derive`, on stage 0. It is keyed by `jdk/forms`, `overlay/jdk/variants`,
  `jdk/edge.edn`, `test/jrt/vet*.{ae,edn}` and j2g's world and analyzer namespaces, and takes about 20 s and 1.5 GiB. It could
  first pass once a conversion has made `jdk/forms` current, which happens at the night after the next `bin/jrt-convert update`.
- **(b)** A step of the Java night's script only (`--forms .tmp/jrt/conv`, before the build).
- *Recommended: (a)*, from the morning after the Java night's conversion is committed. Until then, use (b).

### JCS257 The derived overlays' headers name `test/jrt/derive.ae`

The 19 headers JCS251 moved still name their scratch scripts (`.tmp/f10/gennet.py`, ...), which no longer exist in any
checkout.

- **(a)** Rewrite the headers to name `test/jrt/derive.ae`, at the next conversion. This changes the overlays' text but not
  their code, and moves `jdk/provenance.edn`'s digests.
- **(b)** Keep them as the derivations' first record.
- *Recommended: (a)*, folded into the Java night's conversion.

## As built: F25 (write-ahead): `:reflection` and `:nio` sorted, generic signatures and nesting attributes

Branch `java-refl` from `java-night` (`639d42ab9`), 2026-11-08, by day under the light rules (no j2g of the world, no
build, no conversion; each process under 2 GiB, two at a time). The brief: raise Java's completeness through the deferred
classes `:reflection` and `:nio`, measured first on today's world, sorted by the missing piece, the biggest groups ported
from jdk26u onto jrt. F25 is F14's second slice: the rows "generic signatures" and "member classes" of F14's table and the
InnerClasses/EnclosingMethod part of its "metadata" row.

### The two classes measured on today's world

The committed records predate night 7's conversion (F7, F8, F10a, F11, F12, F13). The classes' tests (the classification of
`java-net`'s `cls-interp.tsv`/`cls-compiled.tsv`: 82 tests in `:reflection` and 109 in `:nio` at either level, 76 / 93 at
interp) were run **interpreted on the newest gate executable** (`.tmp/gate-exe/ad3066cf41ea6faf1566`, `next` after night 8:
F7, F8, F10a, F11, F12, F13 in; java-night's F9, F10b, F14, JCS164, JCS167 not), their forms the newest passing read of each
in the main checkout's read cache (`java-net`'s `linkforms.py`; t03491 has none), two at a time under a 2 GiB watchdog:
`bin/java-check --levels interp --list .tmp/refl/run.ids --save .tmp/refl/s1` with `JAVA_CHECK_EXE`,
`JAVA_CHECK_FORMS_ONLY=1`; 334 s for 190 tests, no test above 2 GiB. By what each stops at:

| tests | the missing piece | whose |
|---:|---|---|
| **69 of 109** `:nio` | **pass already** (F7's channels and buffers, F8's charsets and coders, JC9b's direct buffers) | the night's whole-corpus record |
| 28 | `java.nio.file` (FileSystems, Files.walk/list/probeContentType, the attribute views, the watch service, user principals) | **F9, tonight** |
| 3 | jdk.charsets' extended charsets (ISO-2022-JP, x-eucJP-Open, MS950) | JCS146 (b): later; JCS274 |
| 8 | one each: MemorySegment.allocate(int) (FFM), a ClassLoader subclass's defineClass (2), `conf/security/java.security` (2, F23's), a stack trace, a timeout (t08163), a lost assertion (t05226) | the tail |
| **5 of 82** `:reflection` | pass already | |
| 17 | declaration annotations, Field's primitive accessors | **F14, tonight** |
| 7 | Proxy (newProxyInstance, getProxyClass, isProxyClass; annotations as proxies) | JCS170 (`java-serial4`), JCS210 |
| **13** | **type annotations** (AnnotatedType and its kinds, getAnnotatedSuperclass/Interfaces/ReceiverType, a type variable's annotations, Parameter.getAnnotatedType) | JCS271 below, the next slice |
| **9** | **InnerClasses and EnclosingMethod** (getDeclaredClasses, getClasses, isLocalClass, isAnonymousClass, getSimpleName and getCanonicalName of local and anonymous classes, getEnclosingMethod/Constructor, the inner classes' modifiers) | **F25** |
| **8** | **generic signatures** (getTypeParameters, getGenericReturnType/ParameterTypes/ExceptionTypes, getGenericType, toGenericString, TypeVariable, ParameterizedType, WildcardType) | **F25** |
| 4 | AccessFlag (Class/Member.accessFlags, ClassFileFormatVersion) | JCS273 |
| 4 | Parameter (MethodParameters, -parameters; BadClassFiles reads class file bytes) | JCS273 |
| 15 | the tail: getProtectionDomain, Package's annotations (t04730), AnnotatedElement's defaults over generics (t04836), Retention's annotation status (t04508), javac's synthetic lambda methods (t06144, t06157, t06306), an inner class's outer field (t06345), a nested class's constructor with its outer parameter (t07678), nestmates' access errors (t07688), the array dimension limit (t04837), getMethods of a star inheritance (t04531), an http: connection (t08137), Writer.append's IOException (t04464), arrayType's error (t04504) | F14's metadata row, JCS123 |

So `:nio` is nearly done (69 + 28 of 109 with F9); its remainder has no group above three. `:reflection`'s biggest groups not
anyone's were the type annotations (13), the nesting attributes (9) and the generic signatures (8); the last two are F25 (the
type annotations build on the generic types: JCS271).

### The design: the class file attributes reflection reads, recorded beside the annotations

F14 records each class's RUNTIME annotations as text data (`ClassInfo.Annots`, one encoder for j2g and the interpreter's
host). F25 adds the attributes jdk26u's `Class`, `Executable` and `Field` read through the VM, as more records of the same
data (`arbace.classes.annot/attribute-lines`; the format in `go/arbace/jrt/classdata.ae`'s header):

| record | attribute | written for |
|---|---|---|
| `S SIG` | the class's Signature | every class that has one (the JDK's too: `Class.getTypeParameters` of Map, Enum, Class; HashMap.comparableClassFor's `Comparable<C>`) |
| `K FLAGS KIND OUTER NAME` | its InnerClasses entry: the inner flags, member / local / anonymous, the declaring or enclosing class, the simple name | every nested class |
| `Q NAME DESC` | its EnclosingMethod's method | a local or anonymous class in a method or constructor |
| `G M\|C\|F ... SIG` | a member's Signature | a class's own members as the annotations' (a program's, the interpreter's; the JDK's: JCS270) |
| `E M\|C ... T...` | a method's or constructor's Exceptions | the same |

The analysis already computes them as javac does (`:signature`, `:sig`, `:inner-flags`, `:nesting`, `:enclosing-method`,
`:throws`; the frozen JVM back end wrote them into class files): the encoder only prints them. jrt decodes them on first use
(`classdata.ae`, `attrsOf`, dropped by `SetAnnotations` with the annotations); jrt's hand-written generic classes (Class, Enum,
ThreadLocal, the references, AtomicReference, Future, ForkJoinTask, Constructor) have their Signature in a table
(`handSignatures`: jdk26u's, as javap prints them, less the interfaces jrt's classes do not implement).

**Ported rule for rule** (java/lang/Class.java at jdk26u `baf63fb`): `isLocalOrAnonymousClass`, `isAnonymousClass`,
`isLocalClass`, `isMemberClass`, `getSimpleBinaryName`, `getSimpleName0`, `getCanonicalName0`, `getEnclosingClass`,
`getEnclosingMethod`/`getEnclosingConstructor` (EnclosingMethodInfo's descriptor parsed into classes, the candidates
searched by name, parameters and return type), `getClasses`; `getModifiers` of a nested class its InnerClasses flags
(JVM_GetClassModifiers; the interpreter's classes had their class file flags: `private class` read as package-private);
`getDeclaredClasses` (JVM_GetDeclaredClasses) the closed world's member classes of the class, by name (JCS272);
`getExceptionTypes` the Exceptions attribute, so `Method.toString` and `Constructor.toString` print their throws clause as
the JDK's do. The generic members (`getTypeParameters`, `getGenericSuperclass`, `getGenericInterfaces` of Class;
`getTypeParameters`, `getGenericReturnType`, `getGenericParameterTypes`, `getGenericExceptionTypes`, `toGenericString` of
Method and Constructor; `getGenericType`, `toGenericString` of Field) answer as jdk26u's do without a Signature (the erased
types) and with one through **jdk26u's sun.reflect.generics translated unchanged**: the Signature parser, its trees, the
reifying visitor, CoreReflectionFactory, the scopes, the repositories and the reflective objects (50 files), with
java.lang.reflect's `TypeVariable`, `ParameterizedType`, `WildcardType`, `GenericArrayType`, `GenericDeclaration`,
`AnnotatedType`, `MalformedParameterizedTypeException`, `GenericSignatureFormatError` (`test/jrt/jrt_sources.ae`'s
`generics-sources`, 58 files). jrt's Go reaches them through **`jdk.internal.jrt.ReflectSupport`** (overlay, new): static
methods holding the bodies of jdk26u's `getGenericInfo`, `getFactory`, the generic getters and `toGenericString`
(`sharedToGenericString`, `printModifiersIfNonzero`, `typeVarBounds`, `addSealingInfo`, `hasSealedAncestor`), their receiver a
parameter; jrt keeps each class's ClassRepository (Class.genericInfo) and each member's by its class. In jrt's own build
`standin_reflectsupport.ae` stands in for ReflectSupport, `TypeVariable` and `GenericDeclaration` (j2g's translations replace
them; its `standin-roots` make ReflectSupport's methods the program's roots). Class implements GenericDeclaration (and Type,
as the JDK's: `Class_class`'s interfaces, the manifest's), Executable implements GenericDeclaration.

**The variant** `overlay/jdk/variants/TypeVariableImpl.ae` (5 replaced, 1 cut): a type variable's annotations are type
annotations (TYPE_PARAMETER), parsed by jdk26u from the class file's RuntimeVisibleTypeAnnotations through the constant pool,
which the closed world does not record yet: `getAnnotations` asks `ReflectSupport.typeVariableAnnotations` (none until
JCS271), `getAnnotation` and `getAnnotationsByType` look in them (ReflectSupport's helpers, AnnotationType's retention map
cut), `getAnnotatedBounds` refuses (UnsupportedOperationException naming F25), and `getGenericDeclaration` returns the
declaration where jdk26u copies a Method or Constructor through ReflectionFactory (jrt's hold no mutable state but the
accessible flag).

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`, the frozen jar of the main checkout, `GOMEMLIMIT=1500MiB`, scratch `.tmp/refl/`)
- **the joint javac check** (`.tmp/night7/all9-run.sh`): **2,479 files, 0 errors**, 5,251 classes (15.5 s, 0.98 GB): the
  branch's 2,420 + the 58 + ReflectSupport;
- the read: `bin/repr-java read-jdk --check` of the 59 (`REPR_JAVA_PATCH_ALL=1`, javac's tool `javac-5ad357331dbcd7cfb0fd`
  with `REPR_JAVA_STALE_OK=1`): 59 converted, 0 failed, 0 javac errors (1.5 s); the variant applied to the converted
  TypeVariableImpl and parsed (F4's `vet.ae`): 5 replaced, 1 cut, 0 added;
- the closure scan (F3's `closure.py` against the branch's print-sources, the manifest and the gate executable's Go types):
  the 58 reach nothing outside the world but TypeVariableImpl's annotation parsing (AnnotationSupport, AnnotationType,
  TypeAnnotationParser, ReflectionFactory: the variant's) and jrt's Class, Object, ClassFormatError;
- **the encoder** on the frozen JVM (`.tmp/refl/scripts/encode.clj`, F14's, over the read forms of t04524, t04511, t04849,
  t04850, t04532, t04525): every record as javac writes the attribute: `K 2 m t04532.ForInnerClass Inner`, `K 4 m ...
  Protected`, `K 8 m t04850.Test Inner1`, `Q <init> (I)V` for EnclosingConstructorTests' second local class, no `Q` for the
  anonymous class of a static initializer, `S <T:Ljava/lang/Object;>Ljava/lang/Object;`, `G M max (Ljava.util.Collection;)
  Ljava.lang.Object; <S:Ljava/lang/Object;:Ljava/lang/Comparable<-TS;>;>(Ljava/util/Collection<+TS;>;)TS;`, `G M f ...
  (Lt04849/SignatureTest<Ljava/lang/String;>.Inner1.Inner11;)V`, `E M main ([Ljava.lang.String;)V java.lang.Exception`;
- `bin/jrt manifest` (Class's getClasses, getDeclaredClasses, getTypeParameters, toGenericString; Method's, Constructor's
  and Field's generic members; GenericDeclaration among Class's and Executable's interfaces), `bin/jrt testdata`;
  **`bin/jrt test --arch amd64`: `arbace/jrt` ok**, with `classdata_test.ae`'s three tests (a member, a private inner, a
  local and an anonymous class as the encoder writes them: their modifiers, names, enclosing class and method,
  getDeclaredClasses, getClasses, Method.toString with its throws clause, the decoded Signature with the stand-in's erased
  answers) and TestReflectAgainstJVM; the crops' `expat`, `pyexpat`, `oi` fail by themselves, as on `next`; **`go vet ./jrt`
  clean** for linux/amd64 and arm64.

Not checked by day: j2g's analysis of the 58 and of ReflectSupport, the stand-ins' replacement, and the generic answers
themselves (no executable has them).

### Expected yield

Interpreted, of `:reflection`'s 76: F25's 17 (generics t04847, t04848, t04849, t04850, t04852, t04853, t04855, t06420;
nesting t04511, t04514, t04518, t04519, t04520, t04524, t04525, t04532, t04533), **about 13 expected** (t04848, t04855,
t04524 and t04525 need F14's annotations too, tonight; t04849's and t04850's stdout depends on getDeclaredMethods' order
against the JDK's; t04847 prints the type variables' hash codes, the declarations' identity hashes in them), with F14's
about 14 and the 5 passing: about 32 of 76. Of `:nio`'s 93: 69 pass and F9's 28, about 22 expected, tonight. Compiled: the
same tests where the records hold them now, F25's about 11 (a compiled program's classes are `nm/own-class?`, so their
members' Signatures are recorded as the interpreter's are). Java eval and AOT each about +0.4 point for F25 beyond the
night's slices.

### The night's commands

In the night's worktree with this branch merged (after java-night's slices; its own revert is one merge), heavy ones under
`bin/heavy-lock`; one conversion and one build take every area of the night:

```sh
# 0. the joint javac check (expect 0 errors; the sources line + 59 beyond java-night's)
/root/arbace/.tmp/night7/all9-run.sh $PWD
# 1. jdk/ converted afresh: + 50 sun/reflect/generics, 8 java/lang/reflect, 1 of jrt's own (ReflectSupport); 0 failed,
#    0 javac errors, no javac: 0 fallbacks; then the module table (bin/jrt modules: sun.reflect.generics.* java.base's)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules
# 2. j2g's edge afresh, javac's world's edge
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: variant TypeVariableImpl 5/1/0, no "a variant for a class not in the inputs", no
#    :analysis-failures of sun/reflect/generics or ReflectSupport; the stand-ins ReflectSupport, TypeVariable and
#    GenericDeclaration replaced (no duplicate Go names)
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
grep -A8 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn
# 4. a smoke: expect "[K V] java.util.AbstractList<E> java.lang.Comparable<java.lang.Integer> public final class java.lang.Class<T> true"
target/arbace-go/amd64/arbace -e '(println (mapv str (.getTypeParameters java.util.Map)) (str (.getGenericSuperclass java.util.ArrayList))
  (str (first (.getGenericInterfaces Integer))) (.toGenericString Class) (.isMemberClass java.util.Map$Entry))'
# 5. Java's levels interp and compiled on the two classes' areas (then the whole corpus with the night's others): saved,
#    not recorded; expect about 13 / 11 of F25's 17, the passing ones to hold
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/lang/(Class|reflect|annotation)|langtools/tools/javac/types/BadSigTest|jdk/java/nio' \
  --save .tmp/f25-night/check
```

**Risks and fallbacks**: (1) the stand-ins' replacement: should j2g not reach GenericDeclaration or TypeVariable, the
stand-ins stay (they compile alone) and the generic answers keep working through the translated implementations; should a
translated interface's method set differ from what jrt's Class, Method and Constructor have (GenericDeclaration over F14's
AnnotatedElement), Go's compiler says so: the fix is in `classdata.ae` (a missing method), not a revert; (2) `S` records for
every JDK class: more data in every executable (about one line a generic class), and HashMap's tree bins now order
Comparable keys by compareTo as the JDK's do (comparableClassFor finds `Comparable<C>`); (3) a nested class's `getModifiers`
is now its InnerClasses flags at the interpreted level too, as j2g's ClassInfo.Modifiers already were; (4)
`Class.getSimpleName` of a top-level class whose name holds `$` (c2j's compiled fns, `core$map$fn__123`) is the whole name
after the package, the JDK's answer, where jrt split at the last `$`; (5) the change of `arbace/classes/annot.ae` moves the
gate's key (the interpreter embeds it).

### Proposed (F25)

#### JCS270 The JDK's members' Signatures

The encoder writes a JDK class's own Signature but not its members' (as F14's JCS211 for their annotations), so
`List.class.getMethod("get", int.class).getGenericReturnType()` is `Object` where the JDK says `E`. (a) as built; (b) every
member's (a measure of the data first: thousands of lines); (c) the public members of the public API's classes. Cost: (b) or
(c) a line in `annot.ae` and a night's measure. Recommended: (a) until a test asks; none of the 82 does.

#### JCS271 Type annotations: recorded data or recorded bytes

The 13 tests of the type annotations row need `RuntimeVisibleTypeAnnotations` (TypeAnnotationParser, AnnotatedTypeFactory).
(a) as F14 did for declaration annotations: text records of the analysis' type annotations (TypeReference and TypePath as
javac writes them) decoded by a variant of TypeAnnotationParser into TypeAnnotation objects, AnnotatedTypeFactory unchanged;
3 days; (b) record the attribute's bytes and a ConstantPool over the recorded entries (jdk.internal.reflect.ConstantPool's
natives in Go), jdk26u's TypeAnnotationParser and AnnotationParser unchanged: the real algorithm, and F14's text decoder
could move onto it later; 3-4 days; (c) by design. Recommended: (b), the parser rule for rule over the class file's own
format.

#### JCS272 getDeclaredClasses' order

HotSpot returns the InnerClasses entries in the attribute's order, which is javac's pool order; jrt sorts the member classes
by name. (a) as built (the tests compare sets); (b) record the order (`I` records from the analysis' member classes).
Recommended: (a).

#### JCS273 AccessFlag and Parameter

AccessFlag (4 tests) imports java.lang.classfile (by design out of the world, JCS95 (a)): a variant cutting its class file
members, jdk26u's `maskToAccessFlags` and `ClassFileFormatVersion` translated, `Class/Member.accessFlags` over the modifiers
and the InnerClasses flags F25 records (1 day). Parameter (4 tests, one reading class file bytes: by design) needs
MethodParameters, which javac writes only with `-parameters`: the check's `@compile -parameters` handed to the interpreter and
to j2g (the action's flags), the names recorded then (1.5 days). Recommended: AccessFlag first, with JCS271.

#### JCS274 `:nio`'s remainder

After F9 `:nio` keeps 11: jdk.charsets' three (JCS146 (b): later), FFM's MemorySegment (by design with JCS95's
java.lang.foreign), F23's java.security file, and singles. Recommended: reclassify them by their pieces after the night's
record (`:charsets-locales`, `:security`, `:classloader`), closing `:nio` as a class of its own.

## Decided (the user, 2026-11-08, about 21:00): JCS270-JCS274, and JCS263

- **JCS270 (a)**: the JDK's members' Signatures stay unrecorded until a test asks.
- **JCS271 (b)**: type annotations as the class file's bytes and a ConstantPool over the recorded entries, jdk26u's
  TypeAnnotationParser unchanged: built below ("As built: JCS271 (b) (write-ahead)").
- **JCS272 (a)**: getDeclaredClasses keeps its name order.
- **JCS273**: AccessFlag first (a variant cutting java.lang.classfile), then Parameter (`-parameters` passed through):
  AccessFlag built below ("As built: JCS273 (write-ahead)"); Parameter is JCS280's.
- **JCS274**: `:nio`'s rest reclassified by its pieces after the night's record.
- **JCS263** (doc/classes/JAVA-CONFORMANCE-SURVEY.md): world slices for javax.management (built below, "As built: JMX-1")
  and javax.security.sasl (F23's slice already holds every class its 8 tests name).

## As built: JMX-1 (write-ahead): javax.management's core (JCS263; branch `java-more`, 2026-11-08)

By day under the light rules: nothing converted into jdk/, nothing translated or built; the slice's files read and analyzed
by day as below, the night converts them.

**The slice** (`test/jrt/jrt_sources.ae`'s `jmx-sources`, jdk26u `baf63fb`'s `src/java.management/share/classes` translated as
java.base's: 166 files of java.management beyond F13's 25, and JavaBeansAccess): javax.management's API whole (86 with F13's
three exceptions: the MBean server and its factory and builder, standard and dynamic MBeans, MXBeans, notifications,
queries, descriptors, the permissions), javax.management.openmbean (25), loading (3), timer (5), com.sun.jmx.mbeanserver (31: the introspectors, the MXBean mappings over F25's generics, the repository, the
instantiator), com.sun.jmx.interceptor (2), com.sun.jmx.defaults (2), com.sun.jmx.remote.util's EnvHelp and ClassLogger,
com.sun.jmx.remote.security's NotificationAccessController; sun.management's LazyCompositeData, MappedMXBeanType and the
composite data of MemoryUsage, MemoryNotificationInfo, LockInfo, MonitorInfo, ThreadInfo and StackTraceElement;
java.lang.management's MemoryUsage, LockInfo, MonitorInfo (F13's overlays without their open data removed) and
MemoryNotificationInfo; jdk.internal.access.JavaBeansAccess. F13's skeletons javax.management.ObjectName and
openmbean.CompositeData are gone (jdk26u's in their place). Not in it: the connectors (javax.management.remote, com.sun.jmx.remote's
rest: RMI, JCS132), the model, monitor and relation services, com.sun.management's HotSpot beans.

**Derived** (`test/jrt/derive.ae`, JCS166 (a)'s mechanism; `bin/jrt-convert derive --check`: 50 overlays, 0 not as derived,
the control failing as it must): System.Logger as `jdk.internal.jrt.SystemLoggers.Logger` (F24's rule) in JmxProperties,
ClassLogger and the 12 files that import `System.Logger.Level`; EnvHelp without the connectors' factories (their two attribute
names written as their values); `sun.reflect.misc.MethodUtil.invoke(m, obj, args)` as `m.invoke(obj, args)` (MethodUtil's
trampoline defines a class from bytes: no class loader in the Go build) in ConvertingMethod, DefaultMXBeanMappingFactory,
Introspector, StandardMBeanIntrospector and OpenMBeanAttributeInfoSupport; JavaBeansAccessor's access none (java.beans is not in
the Go build: isAvailable false, as on a JDK without java.desktop); ThreadInfo, F13's overlay, now jdk26u's text with F13's
factory for jrt's ThreadImpl inserted (its open data back).

**jrt's ManagementFactory** (overlay) gains jdk26u's `getPlatformMBeanServer` over its table of beans (the threads', the
runtime's, the operating system's, each registered by jdk26u's `addMXBean` under its own `getObjectName()`) and jdk26u's
`newPlatformMXBeanProxy` (JMX.newMXBeanProxy over a connection). The platform server's other components (memory, class
loading, compilation, the collectors and pools, logging, buffer pools) are F13's later slices, as before.

**Checked by day.** The joint javac check (`.tmp/night7/all9-run.sh`): 2,749 files with this branch's other slices, 0
errors. `bin/repr-java read-jdk --check` of the slice with its context (java.util.logging, sun.management's overlays, jrt's
SystemLoggers and HostManagement; `REPR_JAVA_PATCH_ALL=1`, one chunk): 228 converted, 0 failed, 0 javac errors; `--no-javac`
the same forms byte for byte. j2g's analysis of the slice by `bin/j2g --analyze-only` over jdk/forms with the converted
slice laid over it (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS265 (b)"): the first run named MethodUtil (6
classes), JavaBeansAccess and newPlatformMXBeanProxy, fixed above; the second, none of the slice's classes but Introspector's
UndeclaredThrowableException (JCS170's, converted tonight). Of the 19 javax.management and java.lang.management tests
refused, 11 analyze; the 8 others need the connectors (2), HotSpot's beans (4), getPlatformMXBean over a connection (1) or
the edge (1).

**Expected**: of the 11 that analyze, about 7 pass interpreted and compiled (DuplicateGetterTest, AttributeListTypeSafeTest,
MBeanExceptionTest, ImmutableDescriptorSerialHashCodeTest and MemoryUsageCompositeData plainly; CustomQueryTest and
PreRegisterNameTest over the platform server; PostExceptionTest's URLClassLoader and the deadlock tests' timing are the
risks); the interpreted level's count comes from the same world.

## As built: JCS273 (write-ahead): AccessFlag

`java.lang.reflect.AccessFlag` derived (test/jrt/derive.ae): the imports of java.lang.classfile's models and of
ModuleDescriptor, which only its documentation names, dropped, ClassFile's access flags imported from jrt's own
`jdk.internal.jrt.ClassFileAccess` (the same names and values, JVMS 4.1-4.7); jdk26u's `ClassFileFormatVersion` in the
closure (`accessflag-sources`). `jdk.internal.jrt.AccessFlagSupport` (overlay) holds jdk26u's `Class.accessFlags`,
`Executable.accessFlags` and `Field.accessFlags` over `ReflectionFactory.parseAccessFlags`, the closed world's facts in place
of the VM's: a class's class file version is javac 26's (major 70, minor 0: `ClassFileFormatVersion.latest()`), and its class
file access flags (`getClassFileAccessFlags`) are its modifiers with ACC_SUPER on every class that is not an interface, as javac
writes them. j2g writes `AccessFlags__Set` on jrt's Class, Method, Constructor and Field (`arbace.j2g.out/support-forms`) and
their member table entries once AccessFlag and AccessFlagSupport are translated, AccessFlagSupport's three methods roots
(main's scan and roots). Checked: the joint javac check; `read-jdk --check` of the four files, 0 failed, `--no-javac` the same;
bin/j2g-unit's tables; on the predicted world t04830, t04831 and t04835 analyze, t04832 names Parameter (JCS280). Expected: 3
pass (ClassAccessFlagTest's nested classes rest on F25's InnerClasses flags; FieldAccessFlagTest; VersionedLocationsTest is
AccessFlag's own tables).

## As built: JCS271 (b) (write-ahead): type annotations as the class file holds them

**Slice 1, the data** (`arbace.classes.annot/type-annotation-lines`, in `class-data` beside F14's and F25's records; format in
`go/arbace/jrt/classdata.ae`'s header): `Y T HEX`, `Y M NAME DESC HEX`, `Y C DESC HEX`, `Y F NAME HEX`, each a
RuntimeVisibleTypeAnnotations attribute's info (JVMS 4.7.20: target_type and target_info from the analysis' ASM type
reference value, the type_path from its path string, the annotation and its element values JVMS 4.7.16's), and `N` records,
the constant pool entries they index (Utf8, Integer, Long, Float and Double as raw bits), in index order from 1, a Long or
Double taking two indices, each constant once. Only RUNTIME annotations (reflection reads no RuntimeInvisibleTypeAnnotations),
each resolved alone (`*lenient*`). Both writers carry them: j2g (ClassInfo.Annots) and the interpreter's host. **Checked by
day against javac**: a sample with every declaration target (class and method type parameters and their bounds, the
superclass and interfaces, fields, returns, receivers, formal parameters, throws), every path step (arrays, nested types,
wildcard bounds, type arguments) and every element value kind, compiled by the JDK's javac and read by repr/java; the class
files' attributes and Arbace's records both resolved through their pools (a scratch reader, `.tmp/ta/scripts/norm.py`) and
compared: **28 of 29 annotations equal**; the 29th, `@B(e = ElementType.METHOD)`, encodes as a string on stage 0, whose world
does not know java.lang.annotation.ElementType as an enum (F14's closure, tonight; the sample's own enum, `Color.GREEN`,
encodes as javac's `e`).

**Slice 2, the reading.** jdk26u's `sun.reflect.annotation` in the closure (`type-annotation-sources`: AnnotationParser,
AnnotationInvocationHandler, AnnotatedTypeFactory, TypeAnnotation, the exception proxies) with java.lang.reflect's
AnnotatedArrayType, AnnotatedParameterizedType, AnnotatedTypeVariable and AnnotatedWildcardType; three derived where jdk26u asks
the VM through JavaLangAccess: TypeAnnotationParser (the raw bytes and the pool from `jdk.internal.jrt.TypeAnnotationData`),
AnnotationType (its per-class slot a map; an annotation type's @Retention and @Inherited from the recorded declaration
annotations, where jdk26u parses the raw ones) and AnnotationSupport (a superclass's declared annotations from
getDeclaredAnnotations). jrt's own: `jdk.internal.reflect.ConstantPool` (getSize, getIntAt, getLongAt, getFloatAt,
getDoubleAt, getUTF8At over the entries), `jdk.internal.jrt.TypeAnnotationData` (natives `bytes0`, `pool0` in classdata.ae
decoding the Y and N records), `jdk.internal.jrt.TypeAnnotationSupport` (jdk26u's getAnnotatedSuperclass,
getAnnotatedInterfaces, getAnnotatedReturnType(0), getAnnotatedReceiverType with Constructor's, parameterize,
getAnnotatedParameterTypes with getAllGenericParameterTypes' branch without MethodParameters, getAnnotatedExceptionTypes,
Field.getAnnotatedType). j2g writes the 11 methods of jrt's Class (2), Field (1), Method and Constructor (4 each) and their
member table entries once TypeAnnotationParser and TypeAnnotationSupport are translated, the 7 support methods roots.
TypeVariableImpl's variant is down to getGenericDeclaration: its getAnnotations, getAnnotation, getAnnotationsByType and
getAnnotatedBounds are jdk26u's again (F25's placeholders in ReflectSupport removed). The annotations a type annotation gives
are jdk26u's Proxy objects (AnnotationParser.annotationForMap, JCS170's Proxy over Dyn); declaration annotations stay F14's
`C_ann` objects.

**Checked by day**: the joint javac check, 2,749 files, 0 errors; `read-jdk --check` of the three slices with their
context (306 files), 0 failed, 0 javac errors, `--no-javac` identical; `bin/jrt test --arch amd64`: arbace/jrt ok (the
natives compiled and vetted; the crops' expat, pyexpat and oi fail by themselves, as on next); `go vet ./jrt` clean for
amd64 and arm64; bin/j2g-unit 20 of 20. On the predicted world: TypeVariableImpl's variant applies (1 replaced, 0 cut, 0
added), AnnotationParser, TypeAnnotationParser, AnnotationType, AnnotationInvocationHandler and TypeAnnotationSupport analyze;
the annotated types' bridges need AnnotatedElement's generics in the edge (`bin/j2g-edge update`, tonight, as F25's) and
AnnotationSupport F14's AnnotationFormatError (tonight's conversion). Of the 9 type annotation tests, 8 analyze (t04723 names
Parameter). **Expected**: about 5 of the 9 pass at interp and compiled (the Annotated*Type tests compare toString and equals of
the annotated types and annotations: AnnotatedTypeFactory's and AnnotationInvocationHandler's own code, translated).
Not checked by day: the natives' decoding against the encoder (no executable has both; the night's smoke below), the Proxy
annotations at the compiled level.

### The night's commands (java-more)

In the Java night's worktree with `java-more` as its input (it holds java-props, java-j2g and java-refl), heavy ones under
`bin/heavy-lock`, after the other branches' steps (one conversion and one build take every area):

```sh
# 0. the joint javac check (expect 2,749 files, 0 errors) and the derivations (50, 0 not as derived)
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check
# 1. jdk/ converted afresh: + JMX-1's 167 (jmx-sources) and derived overlays, AccessFlag's 4, the type annotations' 18 with
#    jrt's TypeAnnotationData, TypeAnnotationSupport, ConstantPool; 0 failed, 0 javac errors; the module table again
#    (javax.management.openmbean, .loading, .timer, com.sun.jmx.* java.management's)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules
# 2. the edges (AnnotatedElement's generics, JMX's JDK classes)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; report.edn: no :analysis-failures of javax/management, com/sun/jmx, sun/management,
#    sun/reflect/annotation, jdk/internal/jrt/(TypeAnnotation|AccessFlag)Support, java/lang/reflect/AccessFlag;
#    variant TypeVariableImpl 1/0/0
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
grep -A12 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn
# 4. smokes: expect "[PUBLIC, FINAL, SUPER]", a JVM name, and the annotated superclass's type
target/arbace-go/amd64/arbace -e '(println (str (.accessFlags String))
  (.getAttribute (java.lang.management.ManagementFactory/getPlatformMBeanServer) (javax.management.ObjectName. "java.lang:type=Runtime") "VmName")
  (str (.getType (.getAnnotatedSuperclass java.util.ArrayList))))'
# 5. the levels on the areas, saved, not recorded: expect about 7 of JMX's 11, 3 AccessFlag, 5 type annotations, F23's SASL
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/javax/management|jdk/java/lang/management|jdk/com/sun/management|javax/security/sasl|jdk/java/lang/reflect/(AccessFlag|Parameter|TypeAnnotation)|jdk/java/lang/annotation/typeAnnotations' \
  --save .tmp/more-night/check
# 6. the 120 analyzed again on the night's world, light (JCS265 (b)): what each still lacks
bin/java-check --levels read,compiled --analyze-only --list .tmp/an/still.txt --shard-size 30 --jobs 2 --save .tmp/more-night/an120
```

**Risks and fallbacks.** (1) JMX's introspection leans on reflection jrt answers from tables (getMethods' order, generic
returns through F25, Proxy through JCS170): a wrong answer fails a test, not the build. (2) A JMX class j2g refuses tonight
(report.edn) takes its tests to `:j2g-unresolved`'s successor class; the slice's revert is one commit (jrt_sources' two lines,
the overlays and derivations) and gives F13's skeletons back. (3) The type annotation natives decode what the encoder writes;
should a test's annotations come out empty, the night's step 4 shows whether the records reached ClassInfo.Annots (`Y`
lines) before the parsers are looked at.

### Proposed (java-more): JCS280-JCS285

#### JCS280 Parameter

`java.lang.reflect.Parameter` blocks t04723 and t04832 beside its own four tests (t04717, t04868-t04870, t05910). Without
MethodParameters an executable's parameters are synthesized (`arg0`..., modifiers 0: jdk26u's synthesizeAllParams); with
`@compile -parameters` (CaptureTest, t05910) javac writes them. (a) Parameter in the world with synthesized parameters only
(Executable.getParameters over the parameter count; Parameter.getAnnotatedType through getAnnotatedParameterTypes) — half a
day, but a translated Parameter calls them through Executable, whose Go interface is jrt's hand-written Executable_I: j2g's
support forms must add the methods there (a jrt edit beside them); (b) (a) and MethodParameters recorded (`R` records of the
analysis' parameter names and flags when the test's action passes `-parameters` to the read and j2g) — 1.5 days. Recommended:
(a) now, (b) with JCS273's decision's second half.

#### JCS281 The platform MBean server's other beans

getPlatformMBeanServer registers the threads', runtime's and operating system's beans; the PlatformLoggingMXBean tests (t04792,
t04793) and most of `jdk/java/lang/management` need logging, memory, class loading and the collectors registered too.
(a) F13's later slices as planned; (b) the logging bean now (jdk26u's ManagementFactoryHelper.PlatformLoggingImpl over F5's
LogManager), and getPlatformMXBean(s) over a connection. Cost: (b) half a day. Recommended: (b), after the night's record of
JMX-1.

#### JCS282 A committed check of the type annotation records

The encoder was compared with javac's class files by hand (`.tmp/ta`). (a) as now; (b) the sample and its javac attributes,
normalized as data (recorded by hand with the JDK, as test/java's ref-jdk26.edn is), committed under test/jrt/, and a light
check on stage 0 comparing the encoder's records with them (no javac at check time), in bin/j2g-unit or a gate step. Cost:
(b) half a day. Recommended: (b).

#### JCS283 A prediction verb

The predicted world (the changed closure files converted and laid over jdk/forms, then `--analyze-only`) found three gaps
before the night. (a) by hand, as now; (b) `bin/jrt-convert predict [PATH...]` (the files changed since a commit, their
context from the closure, a scratch world, and `bin/java-check --analyze-only` over the tests of a list). Cost: (b) half a
day. Recommended: (b).

#### JCS284 JMX's remote connectors and HotSpot's beans

t07843, t07954 (javax.management.remote's RMI connector), t07844, t07845, t07848, t07854 (com.sun.management's GcInfo,
HotSpotDiagnosticMXBean, VMOption). (a) by design: RMI is JCS132's, the HotSpot beans describe HotSpot; (b) deferred with
JCS255 (RMI). Recommended: the connectors deferred with RMI, HotSpot's beans by design.

#### JCS285 `bin/jrt test` within the day's budget

`bin/jrt test --arch amd64` tests every package of the module: 4.9 GB peak over its processes (go's compiles), above the day's
2 GiB. (a) as now (by night, or accepted by day); (b) `--package P` (go test of one package, `-p 1`), the day's form. Cost:
(b) an hour. Recommended: (b).


## As built: F27 (write-ahead): java.rmi, and JMX's remote connectors with it (JCS255 (b), JCS284; branch `java-rmi`)

The user's JCS255 (b) (2026-11-08): RMI ported after serialization instead of JCS132's by-design class. Branch `java-rmi`
from `mac/java-more` (`3e876ad36`), 2026-11-08, by day under the light rules (nothing converted into jdk/, nothing
translated or built; each process under about 2 GiB). F27 is the next free number (F26 is `java-classfile`'s). JMX's RMI
connectors, which JCS284 recommended deferring with RMI, come in as F27's second slice, JMX-2.

### The tests measured

Of the corpus's tests (`test/java/ref-jdk26.edn`), those whose source names `java.rmi`, `javax.rmi` or
`javax.management.remote` and that pass on the JDK: **18 of `jdk/java/rmi`** (t05236, t05237, t05241-t05256; the 5 others
of the directory, t05234, t05235, t05238-t05240, fail on the JDK: they need the jtreg test library's `TestLibrary`),
**t08020** (`javax/rmi/ssl`), and JMX's connector tests **t07952** (DaemonRMIExporterTest), **t07953** (JMXServiceURLProtocol),
**t07954** (ConnectorStopDeadlockTest), **t07956** (ConcurrentModificationTest), with the three DiagnosticCommandMBean tests
(t07841-t07843: HotSpot's diagnostic commands over a connector). The records hold them at `:net` (`java\.rmi`) and at the
compiled level's refusals (MarshalledObject, Remote, LocateRegistry...: the classes were not in the world). What the tests
use: the API's exceptions and MarshalledObject (serialization of Throwables and Class objects, JCS167's slices),
UnicastRemoteObject's export and unexport over the TCP transport on loopback, the rmic stubs the tests carry (`*_Stub`,
`Impl1_Skel`: RemoteStub, the 1.1 skeleton protocol) and the dynamic stubs (`java.rmi.server.ignoreStubClasses`: JCS170's
Proxy over RemoteObjectInvocationHandler), the registry (LocateRegistry.createRegistry: ExportException on a port in
use), the DGC's Unreferenced after a collection (MarshalledObjectGet, `sun.rmi.dgc.client.gcInterval`), the socket
factories and the failure handler (RMISocketFactory, CloseServerSocketOnTermination), the server's stack traces in the
client's exception (ServerStackTrace, SuppressStackTraces), `java.rmi.server.hostname` (ChangeHostName), the context
class loader (RuntimeThreadInheritanceLeak), and SunJSSE's socket factories' equality (t08020).

### What comes in, and how

**java.rmi** (`test/jrt/jrt_sources.ae`'s `rmi-sources`: jdk26u at `baf63fb`, 89 files of `src/java.rmi/share/classes`
translated as java.base's, KIND `share:java.rmi`): java.rmi (20 with Naming and MarshalledObject), java.rmi.server (27 with
RMIClassLoaderSpi, the socket factories, RemoteObjectInvocationHandler, the deprecated LoaderHandler, LogStream, Skeleton and
Operation), java.rmi.registry (3), java.rmi.dgc (3), javax.rmi.ssl (2), and sun.rmi's runtime: sun.rmi.server (11: the unicast
references, the marshal streams over jdk26u's ObjectInputStream and ObjectOutputStream, LoaderHandler, Util,
DeserializationChecker), sun.rmi.transport (17: LiveRef, the object table, the targets, DGCImpl with its stub and skeleton,
DGCAckHandler, the stream calls and the connections) and its tcp transport (5: TCPEndpoint, TCPTransport's accept loop and
connection threads, TCPChannel's connection reuse over F10a's sockets), sun.rmi.registry (RegistryImpl with its stub and
skeleton); and java.base's `java/lang/NoSuchMethodError.java` (the rmic stubs' static initializers throw it when a remote
method is missing). Not in it: sun.rmi.log (activation's log, gone with activation in JDK 17: nothing else uses it) and
RMISecurityManager (a SecurityManager).

| part | how |
|---|---|
| the derived overlays (5, `test/jrt/derive.ae`'s `rmi-derivations`) | `RMIClassLoader` (the provider the `java.rmi.server.RMIClassLoaderSpi` property's class or the default, no ServiceLoader lookup of META-INF/services); `sun.rmi.transport.GC` (librmi not loaded: no native libraries in the Go build); `DGCClient` (a remote reference's entry a WeakReference to its LiveRef where jdk26u's is a PhantomReference: jrt has no PhantomReference and no finalization, so the two are cleared and enqueued together; DGCClient never asks the referent); `sun.rmi.runtime.Log` (the logging caller's frame the fourth of a Throwable's stack trace, as jdk26u's StackWalker.walk skips three: no StackWalker in the Go build); `RuntimeUtil` (the RMI runtime's threads made without thread groups: jrt has none) |
| the native (1) | `GC.maxObjectInspectionAge` (librmi's GC.c, JVM_MaxObjectInspectionAge: the milliseconds since the last whole-heap collection) is jrt's `GC_MaxObjectInspectionAge__J_native` (`go/arbace/jrt/rmi.ae`): the milliseconds since Go's last collection (runtime/debug's LastGC), since the runtime's start before the first; its test `rmi_test.ae` |
| jrt's Thread | `getContextClassLoader` and `setContextClassLoader` (the RMI transport sets the target's loader around each call; RuntimeThreadInheritanceLeak sets its own): jdk26u's semantics, the creating thread's loader at construction, the system class loader for an adopted (the main) thread; `bin/jrt manifest` regenerated the manifest and the member tables (Thread's two methods, their test references) |
| JCS240 (a), j2g's half | every class j2g translates writes its methods' and constructors' throws clauses (the `E` records F25 wrote for a program's own classes: `arbace.classes.annot/throws-lines`, `class-data`'s `:throws`, `arbace.j2g.out/class-registration`), the JDK's too: `sun.rmi.server.Util.checkMethod` refuses a remote interface whose method does not declare `RemoteException` (the JDK's are Registry, DGC and JMX's RMIServer and RMIConnection, whose dynamic stubs it checks), and RemoteObjectInvocationHandler rethrows what the method declares, an UnexpectedException otherwise. About 7,000 methods of the world declare exceptions: of the order of 400 KB of data, decoded on first use. ProxyFn's leniency (a method without known exceptions lets every exception through) stays, JCS323 |
| j2g's renames (`arbace/j2g/names.ae`) | java.rmi's `AlreadyBoundException`, `ConnectException`, `UnknownHostException` (java.nio.channels', java.net's) → `Rmi_...`, java.rmi.server's `LoaderHandler` (sun.rmi.server's) → `Rmi_LoaderHandler`, sun.rmi's `Util`, `Channel`, `Connection`, `Target` (sun.nio's, java.nio.channels', java.sql's, java.lang.annotation's) → `Rmi_...`; found by a scan of the predicted world's simple names, which also found **JMX-1's `com.sun.jmx.mbeanserver.Util` beside F13's `sun.management.Util`** (→ `Jmx_Util`: the night would have met it) |

**JMX-2, the connectors** (`jmx-remote-sources`: 41 files of `src/java.management/share/classes` and
`src/java.management.rmi/share/classes`): javax.management.remote's API (the connector and connector server interfaces and
their MBean, JMXServiceURL, the connection notifications, JMXPrincipal, the subject delegation permission, the provider
interfaces), com.sun.jmx.remote.internal (the notification buffers, ArrayQueue, the client's and server's notification
forwarders), com.sun.jmx.remote.security (the access controllers, the file login module, the hashed password manager),
com.sun.jmx.remote.util's ClassLoaderWithRepository and OrderClassLoaders; java.management.rmi's RMIConnectorServer,
RMIJRMPServerImpl, RMIServerImpl and RMIConnectionImpl with their rmic stubs (`RMIServerImpl_Stub`, `RMIConnectionImpl_Stub`:
jdk26u's sources), RMIConnection, RMIServer, ProxyRef, RMIExporter and the two providers. Derived (`jmx2-derivations`):
`JMXConnectorFactory` and `JMXConnectorServerFactory` ask **`jdk.internal.jrt.JmxProviders`** (overlay, new) for the providers
ServiceLoader would stream: java.management.rmi's ClientProvider and ServerProvider, which its module-info provides, behind
ServiceLoader.Provider's interface (`type`, `get`), so the factories' filters, `isSystemProvider` among them (the provider's
module named java.management.rmi: `bin/jrt modules` after the night's conversion), are jdk26u's; `ClientCommunicatorAdmin`,
`ServerCommunicatorAdmin` and `ClientNotifForwarder` make their threads without groups and stack sizes (F24's rule);
`RMIConnector`'s static initializer does not fabricate jdk26u's PRef class from bytes in a module of its own
(NoCallStackClassLoader, jdk.internal.module.Modules: what keeps RMI's stack-based class loading from seeing the stub), the
Go build defining no class from bytes and reading no call stack for class loading: it takes jdk26u's own failure branch (no
proxy reference constructor) and getConnection returns the connection stub unwrapped, as jdk26u does when it cannot wrap it.
NoCallStackClassLoader stays out. JMX-1's EnvHelp derivation (the factories' two attribute names written as their values)
stays: the values are the factories'.

**`bin/j2g --analyze-only`'s `J2G_MISSING`** (`arbace/j2g/main.ae`): with a regex of internal names, the report's `:missing`
lists what the translation of those classes would stub at the operation level, `arbace.j2g.code/missing-reason` over every
node of their analyzed bodies (`jrt lacks java.lang.Thread.getContextClassLoader()...`, `class java.lang.ThreadGroup is not in
the closed world`), with the translation's own scan of jrt's classes (run*'s support-code adjustments factored out unchanged
into `world-scan`). The analysis alone accepts a body whose call j2g then stubs: the scan is what found the context class
loader, the thread groups, StackWalker, ServiceLoader and `System.loadLibrary` before the night (JCS283's prediction verb
would run it).

**Found on the way, fixed** (java-more's, under this branch): `jrt.jrt-sources/print-sources` printed 24 files twice, JMX-1's
21 and JCS271 (b)'s 3 derived overlays whose jdk26u sources their lists also name; bin/jrt-convert's origins map, which the
Clojure reader reads, would have held 24 duplicate keys (`Duplicate key`: the night's conversion stops). A file an overlay
replaces is now printed once, as the overlay.

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`; javac's tool `.tmp/gate-exe/javac-5ad357331dbcd7cfb0fd` with `REPR_JAVA_STALE_OK=1`;
scratch `.tmp/an`, `.tmp/conv`, `.tmp/jmxr` in the worktree)
- **the joint javac check** (`.tmp/night7/all9-run.sh`): **2,892 files, 0 errors**, 5,843 classes (20 s, 1.0 GB; java-more's
  2,749 + 89 java.rmi + 5 derived + NoSuchMethodError + 41 JMX-2 + 6 derived + JmxProviders, less nothing); its negative control
  (JmxProviders moved away): 12 errors, exit 1; no line of sources.txt twice;
- `bin/jrt-convert derive --check`: **61 overlays, 0 not as derived** (java-more's 50 + 5 + 6), the control failing as it must;
- **the read**: `bin/repr-java read-jdk --check` of the two slices with their context (the closure's java.management,
  java.management.rmi, java.rmi, java.logging, java.naming and jdk.management files and every overlay, `REPR_JAVA_PATCH_ALL=1`,
  `GOMEMLIMIT=1500MiB`): 541 files, 533 converted, the 159 forms of RMI and JMX-2 (and JmxProviders, NoSuchMethodError) all of
  them, 8 context files of other slices failed for their own context's absence (httpserver, java.sql, the locale data); 1.6 GB;
  `--no-javac`: the same 159 forms byte for byte;
- **the prediction** (JCS265 (b)'s way: the converted files, and the 568 closure files jdk/forms lacks tonight, the serialization
  slices among them, laid over a copy of jdk/forms; `bin/java-check --levels read,compiled --analyze-only` with `J2G_MISSING`;
  2.1 GB peak, a shard of 24 tests): **23 of the 24 tests analyze** (the 18 of java/rmi, t08020, t07843, t07952, t07953, t07954);
  t07956 is refused by the edge's generics of Thread.UncaughtExceptionHandler (JCS262, tonight's `bin/j2g-edge update`). No
  class of java.rmi, sun.rmi, javax.rmi, javax.management.remote or com.sun.jmx.remote fails its analysis, and none would be
  stubbed (`:missing []`) after the five derivations, the native and the context class loader (the first scan named 22 stubs in 14
  classes: the context class loader 10, StackWalker 4, `System.err` 4 and `String.split` 1 (these two the scan's own before
  `world-scan`), ServiceLoader, ThreadGroup, `System.loadLibrary`). Two tests' own code would be stubbed: t05256's `Thread.getAllStackTraces`
  (JCS320) and t07952's ThreadGroup (JCS322);
- bin/j2g-unit: 23 of 23 (the new `throws` checks: an interface's and a class's E records with `:throws` alone, none without,
  once with `:members`); the changed j2g namespaces load on stage 0;
- `bin/jrt test --arch amd64 -p 1 -run '...'` (one package at a time, 1.5 GB): `arbace/jrt` ok (TestGCMaxObjectInspectionAge,
  TestProxy, TestWeakReferences, TestLiveThreads; the crops' expat, pyexpat, cinterp, goi fail by themselves, as on next); `go
  vet ./jrt` clean for linux/amd64 and arm64;
- `bin/gate --plan`: no table error; jrt-derive and the light steps the shared files' changes move.

Not checked by day: j2g's translation and Go of the new classes, the transport's behaviour (no executable has it: the night's
smoke below), JCS240's records' size in the build.

### Expected yield

Interpreted and compiled, of the 23 that analyze: **likely** NullReference, Chaining, NotExtending, ToStub, SetLogNull, AddrInUse,
MarshalAfterUnexport, MarshalAfterUnexport2, UseDynamicProxies, PrimitiveClasses, CloseServerSocketOnTermination, ChangeHostName,
GcDuringExport, SocketFactoryTest and JMXServiceURLProtocol (15); **at risk** UnrecognizedRefType (Class.newInstance of a class
without a no-argument constructor must throw InstantiationException), ServerStackTrace and SuppressStackTraces (the server's frames
in the exception as jrt's stack traces give them), MarshalledObjectGet (Unreferenced after System.gc: the DGC's weak references
cleared by Go's collector within the test's 120 s) and ConnectorStopDeadlockTest (ThreadInfo.getLockOwnerId over F13's records);
**not expected**: RuntimeThreadInheritanceLeak (JCS320), DaemonRMIExporterTest (JCS322), ConcurrentModificationTest (JCS262) and
the three DiagnosticCommandMBean tests (HotSpot's commands: JCS284's by design). About **15 to 20 expected** at each level, from
`:net` and `:serialization` (java.rmi's classes, MarshalledObject) and the compiled refusals; Java eval and AOT each about +0.5 point.

### The night's commands

In the Java night's worktree with this branch merged (after java-more's steps: it holds them), heavy ones under
`bin/heavy-lock`; the conversion and the build shared with the night's other slices:

```sh
# 0. the joint javac check (expect 2,892 files, 0 errors, no duplicate line) and the derivations (61, 0 not as derived)
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check
awk '{print $3}' .tmp/jc9-check/sources.txt | sort | uniq -d   # expect nothing
# 1. jdk/ converted afresh: + 89 share:java.rmi, NoSuchMethodError, 41 of JMX-2, 11 derived, JmxProviders; 0 failed, 0 javac
#    errors; the module table (java.rmi's packages java.rmi's, javax.management.remote.rmi and com.sun.jmx.remote.protocol.rmi
#    java.management.rmi's: JMXConnectorFactory.isSystemProvider reads it)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules
grep -n "java.rmi\|management.rmi" go/arbace/jrt/module_table.ae | head
# 2. the edges (java.rmi's JDK classes; Thread.UncaughtExceptionHandler's generics for t07956)
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; report.edn: no :analysis-failures of java/rmi, sun/rmi, javax/rmi, javax/management/remote, com/sun/jmx/remote;
#    no "a variant for a class not in the inputs"; the binary's growth (JCS240's records) beside JCS171's measure
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test --arch amd64 -p 1
grep -A12 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn
# 4. smokes on loopback: JCS240's records of a JDK remote interface, a registry exported and unexported;
#    expect "3" (Registry.lookup's throws: RemoteException, NotBoundException, AccessException) and sun.rmi.registry.RegistryImpl
target/arbace-go/amd64/arbace -e '(import (quote (java.rmi.registry LocateRegistry)) (quote (java.rmi.server UnicastRemoteObject)) (quote (java.lang.reflect Proxy)))
  (println (count (.getExceptionTypes (.getMethod java.rmi.registry.Registry "lookup" (into-array Class [String])))))
  (let [r (LocateRegistry/createRegistry 0)] (println (class r)) (UnicastRemoteObject/unexportObject r true))'
# 5. the levels on the areas, saved, not recorded: expect about 15-20 of the 23, no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 4 \
  --only 'jdk/java/rmi/|jdk/javax/rmi/|jdk/javax/management/remote/|jdk/com/sun/management/DiagnosticCommandMBean' \
  --save .tmp/rmi-night/check
```

**Risks and fallbacks.** (1) A j2g refusal of a new class (report.edn): the class's slice's revert is one commit each (F27's
java.rmi, `6f934fca0` with the NoSuchMethodError line; JMX-2, `cc53c80f8`), JCS240's and the context class loader's commits stand
alone. (2) JCS240's records for every class: a class whose data was nil until now has its E lines; should the build grow past
JCS171's budget, `:throws` is one keyword in `arbace/j2g/out.ae` (false: only the program's own classes, as F25). (3) The RMI
threads' and the DGC's timing at the interpreted level (the GC daemon waits an hour by default; MarshalledObjectGet asks one
second). (4) The rmic stubs' method hashes (Util.computeMethodHash over SHA-1 of the name and descriptor) must equal the
server's for the static stubs to dispatch: the descriptor is built from getParameterTypes and getReturnType, jrt's Method's.

**The day after**: the tests judged with the whole corpus; those still failing classed by their pieces (JCS325); doc/go/JRT-NOTES.md's
"Sockets" (RMI's transport) after the run.

### Proposed (F27): JCS320-JCS326

#### JCS320 Thread.getAllStackTraces

RuntimeThreadInheritanceLeak (t05256) and other tests of the corpus call it; jrt's Thread has not, and it returns a translated type
(Map). (a) j2g's support code writes it (as System.getProperties) over HostManagement's `threads0` and `stackTrace0` (another
thread's frames when it waits, F13's records; empty otherwise): an hour; (b) as now (stubbed). Recommended: (a).

#### JCS321 PhantomReference in jrt

DGCClient is derived to hold weak references (jrt has no PhantomReference). (a) jrt's PhantomReference beside Weak and Soft (a
weak pointer whose `get` answers null, enqueued by the same cleanup; the manifest and the member tables regenerated), DGCClient's
derivation dropped, java/lang/ref's phantom tests in reach: half a day; (b) as now. Recommended: (a), when java/lang/ref's tests are
next measured.

#### JCS322 Thread groups

RuntimeUtil and JMX's three thread makers are derived without groups; DaemonRMIExporterTest (t07952) enumerates its thread group,
and `Thread.getThreadGroup` is cut (V10). (a) a minimal ThreadGroup in jrt (the system and main groups, a thread's group at
construction, `getThreadGroup`, `enumerate`, `activeCount`, `getParent`, `getName`; no priorities or daemon status of groups,
deprecated since JDK 16) and the derivations dropped: a day; (b) as now. Recommended: (b) until a second test asks; (a) then.

#### JCS323 ProxyFn's exceptions as the JDK's

ProxyFn lets every exception through when a method declares none it knows (JCS170's leniency, from when no throws clause was
recorded). With JCS240's records the JDK's and the programs' classes have theirs; jrt's hand-written classes and Clojure's
interfaces do not. (a) the leniency kept for jrt's hand-written interfaces alone (their tables get no throws), the rest wrapped as
the JDK wraps (UndeclaredThrowableException): an hour after the night's build shows the records; (b) jrt's member tables carry
throws too (bin/jrt manifest from the JVM's reflection), then no leniency: half a day; (c) as now. Recommended: (a) after the
night, (b) with JCS241.

#### JCS324 A light check of the predicted world

The predicted world (the day's conversion laid over jdk/forms) is a scratch procedure in each branch's `.tmp`; it found, today,
the context class loader, the thread groups and JMX-1's Util collision before the night. (a) JCS283's `bin/jrt-convert predict`
with `J2G_MISSING` as its second half (a list of class prefixes, the analysis and the stubs both reported); (b) by hand, as now.
Recommended: (a), with JCS283.

#### JCS325 RMI's own class

After the night, the RMI tests that still fail are held by `:net`'s `java\.rmi` match. (a) a `:rmi` class before `:net`,
`:kind :deferred`, by piece (stack traces, the DGC's timing, thread groups), reclassified from the night's record; (b) left in
`:net`. Recommended: (a).

#### JCS326 The DiagnosticCommandMBean tests

t07841-t07843 need HotSpot's diagnostic commands (`com.sun.management.DiagnosticCommandMBean`, the VM's `jcmd` set) over JMX's
connectors, which F27 brings. (a) by design, a `:hotspot-diagnostic` class (JCS284's recommendation for HotSpot's beans); (b) the
commands the tests list ported over jrt (VM.version, Thread.print, GC.run, ...). Recommended: (a).

## Decided (the user, 2026-11-08, about 22:00: the older proposals JCS190-JCS213)

**JCS190 (a)**: j2g's default serialVersionUID on by default, once the night's comparison of the 118 values of
`default-suids-jdk26.txt` finds them equal (the flip is a night step, below; not applied by day). **JCS191 (a)**, **JCS192
(a)**, **JCS193 (b)**, **JCS200 (a)**, **JCS201 (a)**, **JCS210 (a)**, **JCS213 (b)**: built below. **JCS211 (c)** if a
test asks (the java.lang and java.lang.annotation annotation types' members' annotations only; no test asks today: not
built). **JCS212 (a)**: as built (the encoder's reversal; the class file's order for repr/java's forms).

## As built: Java's older proposals (JCS191-JCS213) (2026-11-08; branch `java-older`, write-ahead)

Branch `java-older` from `mac/java-props` `a9aebc62f`, by day under the light rules (agent-rules-1108): jrt's Go, the
derived overlays, jrt's own Java, j2g's support function, light checks only; nothing converted, translated as a program or
built into an executable. Shared files were extended, not rewritten (jrt.ae's lists, `jrt_sources.ae`'s `sources`,
`derive.ae`'s `derivations`, this section), so the branch merges onto `java-more` after it.

### JCS191 (a): the locks', Semaphore's and ForkJoinTask's serial forms

`go/arbace/jrt/serial_locks.ae`. jdk26u's ReentrantLock, ReentrantReadWriteLock and Semaphore keep their state in a hidden
Sync object, which their serial forms hold; jrt keeps the state in Go. So the Sync is a descriptor object, `lockSync`. Its
class is the JDK's NonfairSync or FairSync. Its state is the JDK's AQS state: the hold count, the permits, or, for a read-write
lock, the read holds << 32 | the write holds (SHARED_SHIFT is 32 over AbstractQueuedLongSynchronizer's long state; the
recorded stream showed it). The lock's `sync` setter reads it back into jrt's lock: the fairness, and the permits. A lock read
from a stream is unlocked, as Sync.readObject's `setState(0)` makes it.

| class | serial form |
|---|---|
| `ReentrantLock` (7373984872572414699) | `sync` (its Sync) |
| `ReentrantLock$Sync` (-5179523762034025860), `$NonfairSync` (7316153563782823691), `$FairSync` (-3000897897090466540) | Sync's `readObject`: defaultReadObject, state 0 |
| `ReentrantReadWriteLock` (-6992448646407690164) | `readerLock`, `sync`, `writerLock`; one Sync shared with its `$ReadLock` (-5992448646407690164) and `$WriteLock` (-4992448646407690164), so the stream holds it once |
| `ReentrantReadWriteLock$Sync` (6317671515068378041), `$NonfairSync` (-8159625535654395037), `$FairSync` (-2274990926593161451) | Sync's `readObject`: state 0 |
| `Semaphore` (-3222578661600680210), `$Sync` (1192457210091910933), `$NonfairSync` (-2694183684443567898), `$FairSync` (2014338818796000944) | `sync`; no readObject, so the permits are read |
| `AbstractOwnableSynchronizer` (3737899427754241961), `AbstractQueuedSynchronizer` (7373984972572414691), `AbstractQueuedLongSynchronizer` (7373984972572414692) | twins: the owner (transient), `state` int, `state` long |
| `ForkJoinTask` (-7721805057305804111) | `status` (DONE, ABNORMAL, THROWN, the tag's 16 bits, packed as jdk26u packs them); `writeObject` writes the exception the task completed with, `readObject` reads it |
| `ForkJoinTask$AdaptedRunnable`, `$AdaptedRunnableAction` (5232453952276885070), `$AdaptedCallable` (2838392045355241008) | `runnable` or `callable`, `result` |

**The synchronizers are in the world** (AQS, AQLS and AOS are translated from jdk26u; AQS has a variant), so the descriptor
classes' superclasses are **twins**. A twin is a class of the same name, serialVersionUID and serial fields over `lockSync`. It
is not registered, so `Class.forName` still gives the translated class. Reading still works, because ObjectInputStream's
`getClassDataLayout0` matches a stream's class descriptors to the local class's hierarchy by name: the twin's slot reads the
state into the `lockSync`. The Sync classes themselves are registered: they are nested classes of jrt's own locks, which no
translated class names.

**Divergence**: the Sync classes' `getSuperclass` is the twin, not the world's AQS (`AbstractQueuedSynchronizer.class
.isAssignableFrom` is false for them). **Left**: AQS's `ConditionObject` (jrt's condition is `ReentrantLock$ConditionObject`;
the JDK's serial form is AQS's inner class, with `this$0`), proposed as JCS290.

**The oracle**: `test/jrt/serialization/SerialRecordLocks.java`, recorded by hand once on jdk26u's JDK. It gives 14 cases:
locks plain, fair and held; read-write locks plain, fair, held (write, then read) and a read lock alone; semaphores; the
adapters fresh, done, cancelled, thrown and tagged. Their bytes are in `serial_locks_test.ae`'s `jdkLockStreams` and in
`jdk-locks.txt`. The test stream `serial_test.ae` gained jdk26u's `readSerialData` rule: custom data is skipped only after a
class that wrote some (SC_WRITE_METHOD), since the Sync classes have a readObject but no writeObject.

### JCS192 (a): Runtime.version the JDK build's

`system.ae`. `Runtime.version()` is now the platform build's `26.0.2.1-internal-adhoc.root.jdk26u`: numbers 26.0.2.1,
pre-release `internal`, no build, optional part `adhoc.root.jdk26u`. jdk26u's toString, equals, compareTo and hashCode are
ported over these parts; the hash is -526613477, the JDK's. The properties are made from it as VersionProps makes them:
`java.version` `26.0.2.1-internal`, `java.runtime.version` and `java.vm.version` the full string, and `java.version.date`
`2026-08-18`. A java.base frame's module version (`frameElement`) is the short form, as java.base's descriptor's.
`SerialRecordLocks.java` printed the JDK's values. This supersedes F16's choice of "26" for the runtime and VM versions.
**Left**: `pre()`, `build()`, `optional()`, `parse` and `major()` (Version.parse's types are translated), so
RuntimeTests/Version/Basic (t04598) still fails.

### JCS193 (b): the test stream over j2g's tables

**In jrt.** `serialCall` hands a call on one of the tests' streams to that stream (`serialTestStream`). So `serial_test.ae`'s
`serOut` and `serIn` (and their PutField and GetField) stand in for the translated streams in a program root of j2g's as well.

**In the test stream.**
- The stand-ins are made at init: they are the world's classes when it has them (`serProg`).
- The reader matches a stream's descriptor to the object's hierarchy by name, as jdk26u's layout does.
- `serialPersistentFields` is read from the translated ObjectStreamField by reflection.

**The new test.** `serial_prog_test.ae`'s `TestSerialProg` takes ten hookless translated classes: Integer, Long, Boolean,
Character, Double, UUID, SimpleEntry, SimpleImmutableEntry, singletonList and Arrays.asList. Each is made by reflection and
checked against jdk26u's bytes (`SerialRecordProg.java`, `jdk-prog.txt`): written, read, written again, with a control. In
jrt alone it is skipped.

**Left out under `--prog`.** `TestSerialForms`' `throwable-cause-suppressed`, because the translated ArrayList's hooks call
the translated stream's own methods. That case is the real streams' check.

### JCS200 (a): jdk.net's extended socket options

**What came into the world.** jdk26u's jdk.net, as `jdk-net-sources` (KIND `share:jdk.net`): NetworkPermission, Sockets and
UnixDomainPrincipal.

**The derived overlays** (`test/jrt/derive.ae`):
- `jdk/net/ExtendedSocketOptions.java`: a descriptor's number comes from `HostFiles.handle`, and Linux's platform options are
  made directly, since `jdk.internal.util.OperatingSystem` is not in the world.
- `jdk/net/LinuxSocketOptions.java`: LinuxSocketOptions.c's 15 natives are given bodies over four new HostChannels natives.
  These are int getsockopt and setsockopt, socketOptionSupported and SO_PEERCRED. handleError's exceptions are kept:
  UnsupportedOperationException for ENOPROTOOPT, else SocketException with the error's text. No library is loaded.

`sun.net.ext.ExtendedSocketOptions.getInstance` finds the class by name and registers it. TCP_QUICKACK, TCP_KEEP*,
IP_DONTFRAGMENT, SO_INCOMING_NAPI_ID and SO_PEERCRED then join the sockets' supported options, as on the JDK.

### JCS201 (a): the inherited channel

**The overlay.** `sun/nio/ch/InheritedChannel.java` is derived from jdk26u's unix file. InheritedChannel.c's natives get bodies
over nine new HostChannels natives in `channels_linux.ae`: dup, dup2, open, SO_TYPE, the family, connectedness, the inet peer,
the UNIX peer name, and adopt. The FileDescriptor is made by `IOUtil.newFD`, since `sun.nio.ch.Reflect` is not in the world.

**Adoption.** The dup of standard input enters HostChannels' table only once it is known to be a socket. Entering the table
makes a descriptor non-blocking in the kernel, which would change standard input's open file description.

**Variants and support.**
- The Poll and EPoll SelectorProvider variants that answered null were removed; `inheritedChannel` is jdk26u's again.
- `System.inheritedChannel` is a j2g support function, `J2g_SystemInheritedChannel` (`m/support-call-helpers`), which calls
  `SelectorProvider.provider().inheritedChannel()`. It is rooted, and has a reflection entry for the interpreter.

**Divergences.**
- An inherited socket's non-blocking mode is shared with fd 0, so a program reading `System.in` on such a socket may see no
  data.
- On the box (no host descriptors) `System.inheritedChannel` throws "Channels not available" instead of returning null.

### JCS210 (a), JCS213 (b): annotations as proxies; the real IncompleteAnnotationException

`annotation.ae`.
- **The proxy class.** Each annotation type's class is registered as a proxy class, so `Proxy.isProxyClass` is true for it.
- **The handler.** `Proxy.getInvocationHandler` of an annotation gives its `sun.reflect.annotation.AnnotationInvocationHandler`,
  one per annotation. Its invoke is jdk26u's: equals is equalsImpl, then toString, hashCode and annotationType; an element
  gives its cloned value; a member with parameters is an AssertionError.
- **The missing member.** It throws the world's `IncompleteAnnotationException(type, member)`. That class is in F14's
  `annotation-sources`; jrt reaches it by name and its public constructor, as the serial hooks reach the streams.
- **Why no hook set at start.** JCS213 (b)'s hook served to reach a translated class from jrt. The lookup by name does that
  without start-up code: every public member of the world is a program root. Without the class (jrt alone) the exception is the
  UnsupportedOperationException, as before.

**Left**: an annotation's serial form. The JDK writes a proxy with AnnotationInvocationHandler's `type` and `memberValues`.
It is proposed as JCS291.

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`, `GOMEMLIMIT=1400MiB`; scratch `.tmp/older/`)
- **The joint javac check** (`.tmp/night7/all9-run.sh`): 2,512 files, 0 errors, 5,337 classes (15 s, 0.95 GB). The three
  new overlays and jdk.net's five classes are among them.
- **`bin/jrt-convert derive --check`**: 27 overlays, 0 not as derived, the control passing (3 new: InheritedChannel,
  ExtendedSocketOptions, LinuxSocketOptions).
- **`go test ./jrt`** (printed by repr/go, as `bin/jrt test` runs it, amd64): every test passes. The new ones:
  - TestSerialLocks: 14 of 14 written, read and written again byte for byte.
  - TestSerialLocksRead and TestSerialLocksControl pass.
  - TestRuntimeVersion.
  - TestAnnotationProxy.
  - TestChannelInherited: on loopback and a socket pair.
  - TestChannelExtOptions: on loopback.
  - TestSerialProg is skipped in jrt alone.

  The control by hand: one byte of the semaphore's recorded stream changed fails TestSerialLocks.
- **`go vet ./jrt`** on linux/amd64 and arm64: clean. `channels_other.go` is syntax-checked by gofmt; no build of tamago here.
- **j2g's namespaces** (model, out, main) load on stage 0 from the checkout (arbace.classes.analyze loaded from the checkout
  first: stage 0 embeds an older one).

Not checked by day (no conversion, no build): j2g's output for the support function, the derived overlays through repr/java
and j2g, the real streams over the new tables, `TestSerialProg` under `--prog`.

### Expected tests

Each needs its prerequisites' nights (slice 1's streams, F14's annotations, F9's and F11's classes):
- JCS201: `inheritedChannel/ClosedStreams` (t05202) and `NullTest` (t05203), at interp and compiled.
- JCS200: `unixdomain/SocketOptions` (t05205).
- JCS210: `annotation/EqualityTest` (t04718).
- JCS213: `TestIncompleteAnnotationExceptionNPE` (t04722).
- JCS191: `ConcurrentHashMap/MapCheck` (t05680: CHM's Segments are ReentrantLocks).
- About **5-6 at each of interp and compiled**. JCS192 and JCS193 free none directly: JCS193 is a check.

### The night's commands

In the night's worktree with `java-more` and then this branch merged; the heavy steps under the heavy lock:

```sh
# 0. the joint javac check (0 errors) and the derived overlays as committed
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
bin/jrt-convert derive --check
# 1. jdk/ converted afresh: the overlays InheritedChannel, jdk/net/{ExtendedSocketOptions,LinuxSocketOptions} new,
#    HostChannels changed, jdk-net-sources' 3 files new, the variants Poll/EPollSelectorProvider gone
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 2. JCS190 (a)'s comparison then its flip (the slice 2-3 night's steps 2-3, J2G_DEFAULT_SUID=1); only when it reports
#    "N compared, 0 differ" (N about 118):
mkdir -p .tmp/jcs168 && rm -f .tmp/jcs168/suids.txt
J2G_DEFAULT_SUID=1 J2G_DEFAULT_SUID_OUT=$PWD/.tmp/jcs168/suids.txt bin/heavy-lock bin/arbace-go --build
r=$(awk 'NR==FNR{j[$1]=$2;next} ($1 in j){n++; if(j[$1]!=$2){b++}} END{print n+0" compared, "b+0" differ"}' \
      .tmp/jcs168/suids.txt test/jrt/serialization/default-suids-jdk26.txt); echo "$r"
case $r in *" compared, 0 differ") sed -i 's/(= "1" (System\/getenv "J2G_DEFAULT_SUID"))/(not= "0" (System\/getenv "J2G_DEFAULT_SUID"))/' arbace/j2g/model.ae
  git commit -qam "JCS190 (a): j2g's default serialVersionUID on by default ($r)" ;; esac
# 3. the executable (the flip moves gate_key: build again when step 2 committed), the smoke, jrt's tests
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. the real streams: expect "locks-check: 24 of 24" (and slice 2's "streams-check: 16 of 16")
target/arbace-go/amd64/arbace -e '(load-file "test/jrt/serialization/locks_check.ae")'
target/arbace-go/amd64/arbace -e '(load-file "test/jrt/serialization/streams_check.ae")'
# 5. smokes: no inherited channel over a pipe ("nil"); the extended options listed and read; an annotation a proxy
echo | target/arbace-go/amd64/arbace -e '(println (System/inheritedChannel))'
target/arbace-go/amd64/arbace -e '(let [s (java.nio.channels.SocketChannel/open)] (println (sort (map str (.supportedOptions s)))) (println (.getOption s jdk.net.ExtendedSocketOptions/TCP_KEEPIDLE)))'
target/arbace-go/amd64/arbace -e '(let [a (.getAnnotation java.lang.Deprecated java.lang.annotation.Retention)] (println (java.lang.reflect.Proxy/isProxyClass (class a)) (class (java.lang.reflect.Proxy/getInvocationHandler a))))'
# 6. JCS193 (b): the test stream over j2g's tables (a program root with jrt's tests; expect TestSerialProg to pass)
bin/heavy-lock bin/j2g --tests --out .tmp/jcs193/j2g --root java/lang/Integer --root java/lang/Long --root java/lang/Boolean \
  --root java/lang/Character --root java/lang/Double --root java/util/UUID --root 'java/util/AbstractMap$*' \
  --root java/util/Collections --root java/util/Arrays --root java/io/ObjectStreamField
bin/jrt test --arch amd64 --prog .tmp/jcs193/j2g/prog -run 'TestSerial'
# 7. Java's levels on the tests: saved, not recorded; expect t05202, t05203, t05205, t04718, t04722, t05680 to pass, no
#    passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --only 'jdk/java/nio/channels/(spi/SelectorProvider/inheritedChannel|unixdomain|SocketChannel|ServerSocketChannel)|jdk/java/lang/annotation/|jdk/java/util/concurrent/(ConcurrentHashMap|locks|forkjoin)|jdk/java/lang/RuntimeTests/Version|jdk/java/net/Socket' \
  --save .tmp/older-night/check
```

**Risks and fallbacks.**
1. j2g refuses a member of the derived overlays. The fallback per feature is `git revert` of its commit: JCS200 (60487528e)
   and JCS201 (ce0e156f9) are independent.
2. `ExtendedSocketOptions`' extra supported options change a passing test's printed option set. That is now the JDK's set; a
   difference is the test's case to read.
3. The new java.version strings change a passing test's stdout. Again the JDK's; read it.
4. `locks_check.ae` fails on the twins (the real ObjectInputStream's layout). The fallback is to register the twins only when
   the world lacks the classes, as the test does, and record the failure as JCS292's.

## Proposed (java-older)

Numbered from JCS290.

### JCS290 AbstractQueuedSynchronizer$ConditionObject's serial form

jrt's condition is `ReentrantLock$ConditionObject`, a Serializable leaf with no serial members. The JDK's is AQS's inner
`ConditionObject`: serialVersionUID 1173984872572414699, its waiters transient, `this$0` the lock's Sync. Writing one gives
other bytes; reading one names a class jrt has not.
- (a) Rename jrt's class to the JDK's name with a twin AQS descriptor and `this$0` the lock's lockSync. Half a day, and the
  class name changes (`getClass().getName()`).
- (b) As now.

**Recommended: (b)** until a test asks; none of the corpora does.

### JCS291 An annotation's serial form

Since JCS210 an annotation is a proxy class, so ObjectOutputStream writes it as a proxy whose handler field `h` is
`proxyHandlerField`'s. That is a Dyn proxy's field, so writing an annotation fails ("not a proxy instance"); before JCS210 it
wrote a class descriptor of `$ProxyN`.
- (a) Proxy's `h` getter answers `annotationHandlerOf` for an annotation, and the handler gets jdk26u's serial form (`type`,
  `memberValues` as a LinkedHashMap made by reflection; `readObject`'s validation). Reading a stream's annotation proxy then
  makes the annotation (`AnnotationParser.annotationForMap`'s place: newAnnotation). 1 day.
- (b) As now.

**Recommended: (a)** with JCS210's tests' night, if a test serializes an annotation (`annotation/AnnotationTypeDeadlock`
does not; `java/io/Serializable/...` tests may).

### JCS292 Twins of translated classes as a mechanism

JCS191 made unregistered twins of three translated classes so that jrt's descriptor classes extend them. The same need comes
again wherever a jrt class's serial form names a translated superclass: Number for jrt's atomics already, StackTraceElement's
none.
- (a) A small registry of twins in jrt (`TwinOf(name)`): the twin made once per name, its serial table given, and
  documented in J2G-SPEC §5.11.
- (b) Per use, as now.

**Recommended: (b)** until a second user appears.

### JCS293 Runtime.Version's Optional parts and parse

`pre()`, `build()`, `optional()`, `version()`, `parse(String)` and the deprecated `major()`, `minor()`, `security()`: Runtime.Version is jrt's leaf class,
and Optional and List are translated.
- (a) jrt's Runtime$Version keeps its class and gains `pre`, `build`, `optional`, `version` and `parse` as j2g support
  functions over the translated Optional and List (`m/support-call-helpers`, as System.inheritedChannel's). `parse` would be
  a port of jdk26u's VersionPattern rules into the Go, and `major()`, `minor()` and `security()` jdk26u's deprecated ones.
  RuntimeTests/Version/Basic (t04598) is the measure. 1 day.
- (b) As built.

**Recommended: (a)** when the Java triage's next slice takes small reflection gaps.

## As built: F26 (classfile), write-ahead: java.lang.classfile in the world (JCS260 (b))

Branch `java-classfile` from `mac/java-props` (`a9aebc62f`), 2026-11-08, by day under the light rules. Nothing is
converted, translated or built here; the night's commands are below. The brief: JCS260 (b), decided by the user on
2026-11-08. jdk26u's class-file API and its implementation come into the Go build's world, as F9 and F23 did: jdk26u's
own Java converted by repr/java, listed in `test/jrt/jrt_sources.ae`, natives only where needed, variants as few as
possible. F25 is left to the day's other Java branches, so this is F26.

### What uses it: the corpus's 44 tests

`test/java/ref-jdk26.edn`'s source sets were scanned for `java.lang.classfile`, `java.lang.constant`,
`jdk.internal.classfile` and `AccessFlag`. **44 tests** match, and the JDK passes all 44. At the committed records they are
`:load-failed` at interp and `:refused` at compiled. jdk26u's own `test/jdk/jdk/classfile` (52 JUnit tests) is not in the
corpus, whose categories are main runs only.

| family | tests | what they do with the API | interp's host |
|---|---:|---|---|
| javac's typeAnnotations | 17: t05992-t06008 | compile sources with javac in-process, parse the class files, count type annotations | javac's tool (F6) |
| line numbers, `resolve`, `classfiles` | 8: t06042, t06043, t06326-t06329, t06390, t06072 | parse the test's own class files from `test.classes` or `getResourceAsStream` | executable |
| other javac tests | 5: t05847, t05856, t05857, t05915, t05921 | compile in-process or by ToolProvider, then parse | javac's tool |
| `ByteCodeTest` | 1: t06156 | the constant pool by a visitor that names `MethodHandleInfo` | javac's tool |
| build and define | 6: t04537, t04712-t04715, t04865 | `ClassFile.of().build(...)`, then `ClassLoader.defineClass` of the bytes | executable |
| `AccessFlag` | 5: t04830-t04833, t04835 | `Class`/`Field`/`Method`/`ModuleDescriptor` `accessFlags()`, `AccessFlag.maskToAccessFlags` | executable |
| `CallerSensitive` | 2: t08102, t08103 | parse the JDK's own class files through the `jrt:/` file system | executable |

### What came in, and how (jdk26u at `baf63fb`)

`test/jrt/jrt_sources.ae`'s `classfile-sources` lists **260 files and 58,652 lines**, all of KIND `share`, unpatched. They
are:

- `java/lang/classfile` with `attribute`, `constantpool` and `instruction`: 154 files.
- `jdk/internal/classfile/impl` (69), `impl/verifier` (8) and `components` (5): ClassPrinter, ClassRemapper and the code
  transforms, whose implementations are in `impl`.
- `java/lang/constant`'s other nine (Constable and ConstantDesc were added-sources' already) and `jdk/internal/constant`'s nine.
- What those name that the world lacked, found by a constant-pool scan of their class files against the gate executable's
  23,035 translated classes and jrt's manifest (`.tmp/cf/scripts/cpdeps.py`):
  - `java/lang/reflect/AccessFlag` and `ClassFileFormatVersion`;
  - `java/lang/invoke/TypeDescriptor` (ClassDesc's and MethodTypeDesc's interfaces);
  - `sun/invoke/util/Wrapper` (the primitive class descriptors');
  - `java/lang/VerifyError` (`ClassFile.verify`'s);
  - `jdk/internal/access/JavaUtilCollectionAccess`.

The rest of what they name stays outside the world as J2G-SPEC's operation-level stubs: java.lang.invoke's run-time classes
(`resolveConstantDesc(Lookup)` everywhere, `ClassHierarchyResolver.ofClassLoading(Lookup)`, DynamicConstantDesc's enum and
VarHandle canonicalizations, which catch a stub's exception and keep the description), `BootstrapMethodError`, and the JIT's
annotations. The lambdas, records and pattern switches are j2g's own, as everywhere.

| part | how |
|---|---|
| the lists from trusted arrays | `SharedSecrets.getJavaUtilCollectionAccess().listFromTrustedArray(...)`: ClassImpl's interfaces, the annotation reader, `Util`'s lists. No variant: `arbace.j2g.model/serial-accessors` gains `JavaUtilCollectionAccess`, whose setter `ImmutableCollections$Access`'s static initializer calls, as JCS167's accessors work. j2g then writes `SharedSecrets_GetJavaUtilCollectionAccess__JavaUtilCollectionAccess`, which initializes `ImmutableCollections$Access` first. The same accessor serves Gatherers' windows, the four `:unavailable` entries of night 8's report |
| `JavaLangAccess.concat` | jrt's Go (`go/arbace/jrt/access.ae`, with `TestJavaLangAccessConcat`). `ConstantUtils.concat` builds every `ClassDesc`'s and `MethodTypeDesc`'s descriptor with it, so without it the API cannot make a descriptor. It is jdk26u's `StringConcatHelper.concat`: prefix, `String.valueOf(value)`, suffix, and a null prefix or suffix is `"null"`. jrt had the other three JLA methods the API calls (`countPositives`, `inflateBytesToChars`, `countNonZeroAscii`) |
| `Unsafe`'s unaligned reads | RawBytecodeHelper's `getCharUnaligned`, `getShortUnaligned` and `getIntUnaligned` with the big-endian flag: already in jrt's Unsafe |
| Go names | seven renames in `arbace.j2g.names/renames`: `Classfile_Annotation` (jrt's `Annotation`), `Classfile_Attribute` (javax.naming.directory's), `Classfile_Attributes` (java.util.jar's), `Classfile_Signature` (java.security's), `Classfile_MethodInfo` (jrt's member tables' `MethodInfo`), `Classfile_Util` (sun.management's), `Invoke_Wrapper` (java.sql's). They were found by checking the 631 named classes against the world, jrt's manifest and jrt's Go identifiers (`.tmp/cf/scripts/gonames.py`). No package-private method is overridden across Java packages (W4; `pkgpriv.py`: 0 pairs) |
| the interpreter's `Class.getResource` | j2g's support code adds `Class.getResource` over `ClassPathResources.getResource`, the name resolved as `getResourceAsStream` resolves it. Both go into `Class`'s member table, which the interpreter reads: F19's day finding, "No accessible method getResourceAsStream in java.lang.Class". A test helper that only declares a method calling them failed to load, as javac's `ClassfileTestHelper.getClassFile(String)` does |

### Light checks

All ran with main's stage 0 as `ARBACE_HOST_EXE`, javac's tool `javac-5ad357331dbcd7cfb0fd` with `REPR_JAVA_STALE_OK=1`, and
the gate executable `ad3066cf41ea6faf1566`. Each process stayed under 2 GiB under a watchdog. Scratch is in the worktree's
`.tmp/cf/`.

- **The joint javac check.** `.tmp/night7/all9-run.sh` on the branch: 2,766 files (2,506 + 260), **0 errors**, 6,013
  classes, 22 s, 0.97 GB.
- **The read.** `bin/repr-java read-jdk --check` of the 260 files as a patch of java.base, as the convert step reads them:
  260 converted, 0 failed, 0 javac errors, 0 unreadable, 274 files, 1,678,284 bytes, 32 s, 1.39 GB. With `--no-javac`:
  260 native, 0 fallbacks, the forms byte for byte the same (1.93 GB peak at two threads).
- **j2g's world, by day.** This is `bin/jrt-convert vet --forms` on a copy of `jdk/forms` with the 274 new files merged, and
  JCS253's steps:
  - the declaration: **5,161 classes declared**, and no class of the port fails its parse, declaration, header, members or
    bridges (`--others`: only the known `FilePermission`);
  - the analysis, bodies and all, of the 633 classes by a scratch copy of `jrt.vet` (`.tmp/cf/vetns/jrt/cfvet.ae`,
    `analyze-class!` on them): 3 refusals, all the edge's: `DirectMethodHandleDesc` and `BytecodeHelpers` (the
    `MethodHandleInfo.REF_*` constants), and `DynamicConstantDesc` (`Enum$EnumDesc.of`, `VarHandle$VarHandleDesc.of*`).
    `jdk/edge.edn` has none of the three classes, and the night's `bin/j2g-edge update` records them from the JVM. With those
    three entries in a scratch copy of the edge (`.tmp/cf/edge-sim.edn`: MethodHandleInfo's nine constants, EnumDesc's and
    VarHandleDesc's factories), **0 refused** on the 1,024 classes of java.lang, jdk.internal and sun.invoke.util, 27 s,
    1.50 GB.
- **jrt.** The two files F26 changes, printed by `bin/repr-go print --layout gofmt` (`bin/jrt test` prints all of `go/`, at
  4.2 GB), differ from java-older's printed module of today only by the additions. With them in a copy of that module,
  `go vet ./jrt` is clean and `go test ./jrt` reports ok, `TestJavaLangAccessConcat` included (5.5 s, 0.3 GB).
- **j2g's new forms.** The Go method and the table entries, printed: `gofmt -e` clean. j2g's names on stage 0: the
  accessor's bases are `GetJavaUtilCollectionAccess__JavaUtilCollectionAccess` and
  `SetJavaUtilCollectionAccess_JavaUtilCollectionAccess__V`, the calls' own, and concat's is `Concat_String_O_String__String`.
- **`bin/gate --plan`.** Exit 0; every step is keyed (15 s, 0.4 GB).

### The API on Arbace's runtime, by day: the smoke, identical to the JDK

The forms ran interpreted on the gate executable, the same Java as the build will translate. The smoke program
(`.tmp/cf/smoke/src/cfsmoke/Smoke.java`) does the following:

- builds a class with a loop, a field and a branch, whose stack maps are made through the default class hierarchy resolver
  by class loading;
- parses it back, element by element, then verifies it;
- transforms it (drops the field, replaces a constant);
- uses ConstantPoolBuilder and `AccessFlag.maskToAccessFlags`;
- parses javac's class files of a sample with generics, a synchronized block, a string switch, a record, an enum and a
  lambda, and for each: its attributes, line tables, annotations, a round trip by `ACCEPT_ALL`, a new pool and `verify`;
- prints a class with `ClassPrinter.toYaml`.

**Its output is the JDK's byte for byte** (`.tmp/cf/smoke/go.out`, `jdk.out`), in 11 s at 1.0 GB.

The interpreter cannot define the API's own packages as `run.ae` does: they import each other in cycles, and an import names
a class that is not yet defined. So the smoke's loader (`.tmp/cf/smoke/load.ae`, `.tmp/cf/scripts/flatten.py`):

- renames the 14 packages into one;
- enters every class in one top-level `do`;
- leaves out the 17 members that name java.lang.invoke's run-time classes (the interpreter has no edge, so it cannot declare
  them; j2g stubs them);
- answers jrt's `SharedSecrets.getJavaUtilCollectionAccess` and `JavaLangAccess` by a small converted helper
  (`SmokeAccess`), because the interpreter cannot reach jrt's Go `SharedSecrets`.

None of this is in the build.

### The 44 tests on the port, interpreted, by day

The same loader ran each test, its forms from main's read cache (the newest passing read), its main renamed. Tool tests ran
on javac's tool with its platform, as F6 runs them. Each was judged on status and stdout's digest against the JDK's record
(`.tmp/cf/scripts/runtest.sh`, `results2.tsv`, `results3.tsv`). t06072 has no cached read (package `test`).

| result | tests | what stops them |
|---|---:|---|
| **pass** | 3 | t04835 VersionedLocationsTest, t05856 DuplicateConstantPoolEntry, t06042 SyntheticClasses |
| **pass with F26's `Class.getResource`** | 2 + 2 | t05994 and t06004 pass with the helper's unused method left out, as the table entry makes it loadable. t05992 and t05993 reach their two hundred cases, each printing "Pass" (javac on Arbace compiling in-process, the API parsing its class files), until the day's 2 GiB watchdog stops them at 2.36 GB |
| the test's own class files | 18 | t05995-t06003, t06006-t06008 (`getClassFile("X$Test.class")` of a class jtreg's `@compile` made), t06043, t06326-t06329, t06390. The interp level's `test.classes` is empty (bin/java-check compiles the test only on the JDK's side), so these read nothing: **JCS300** |
| jrt's reflection | 4 | t04830 `Class.getDeclaredClasses`, t04831 `Field.getAnnotation`, t04832 `java.lang.reflect.Parameter`, t04833 `ModuleDescriptor`, then each its `accessFlags()`: **JCS301** |
| `defineClass` of built bytes | 6 | t04537, t04712-t04715 (and `AnnotationTypeMismatchException`, not in the world), t04865: **JCS302** |
| javac on Arbace | 4 | t05847 (a jar on the class path: "package j does not exist"), t05857 and t05915 (`--release 16`: "release version 16 not supported"), t05921 ("Cannot find javac") |
| java.lang.invoke | 1 | t06156 (`MethodHandleInfo.referenceKindToString`: `:invoke`, JCS94 (b)) |
| the `jrt:/` file system | 2 | t08102, t08103 (the JDK's own class files: by design) |
| `java.lang.annotation.Repeatable` | 1 | t06005 (the meta-annotation is not in the world) |
| no read | 1 | t06072 |

**Expected after the night.**

- At interp, 5 to 7 of the 44 pass: the three, t05994 and t06004, and t05992 and t05993 under the night's limits. With
  JCS300 (b) or (c), up to 18 more.
- At compiled, the executable's non-tool tests that pass at interp: t04835 and t06042. The tool tests are JCS261's.
- The rest leave `:classfile` for the classes above.
- **JCS135, the growth.** +58,652 Java lines on the closure's 1,017,154 (+5.8%), +1.68 MB of forms on `jdk/forms`' 21.6 MB
  (+7.8%). By F4's ratio the executable grows by about 9 MB. The API is large: most of its classes are records and sealed
  interfaces, which j2g translates as ordinary classes.

### Findings for the interpreter (no test of the corpus needs them)

- **Imports.** The interpreter defines a package's classes one by one, so an import naming a class not yet defined fails.
  This is F19's day finding again, now with fourteen packages in a cycle. Entering every class in one `do` across packages
  works (the smoke's loader).
- **The source lookup's re-entrancy.** Entering `UnboundAttribute.ae` from source, while its header names a class whose own
  file names `UnboundAttribute$AdHocAttribute`, resolves AdHocAttribute's members before its header has set its type
  variables: "Unknown type: T" (`analyze/enter-ctor`). The one-`do` entry avoids it.

### The night's commands

Run them in the night's worktree with this branch merged, folded into the Java night's conversion. Heavy ones go under
`bin/heavy-lock`.

```sh
# 0. the joint javac check of the merged tree: 0 errors, the sources line +260
.tmp/night7/all9-run.sh /root/arbace/.tmp/worktrees/next
# 1. jdk/ converted afresh: +260 `java.base share` lines, 0 failed, 0 javac errors, no javac: 0 fallbacks; jdk/forms +274
#    files (the namespaces java.lang.classfile, .attribute, .constantpool, .instruction, jdk.internal.classfile.impl,
#    .verifier, .components, jdk.internal.constant, sun.invoke.util, java.lang.invoke new; java.lang, java.lang.reflect,
#    java.lang.constant, jdk.internal.access merged)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. the package table (java.base only: no change expected, --check), j2g's edge, javac's world's edge; the edge must then hold
#    java/lang/invoke/MethodHandleInfo with :const REF_getField..REF_invokeInterface 1..9, java/lang/Enum$EnumDesc and
#    java/lang/invoke/VarHandle$VarHandleDesc (the day's vet refused DirectMethodHandleDesc, BytecodeHelpers and
#    DynamicConstantDesc without them)
bin/jrt modules --check && bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
grep -c '"java/lang/invoke/MethodHandleInfo"\|"java/lang/Enum\$EnumDesc"\|"java/lang/invoke/VarHandle\$VarHandleDesc"' jdk/edge.edn
# 3. the variants and the port's classes on j2g's world (JCS253): the known nine refusals go with the conversion
bin/jrt-convert vet --others
# 4. the executable; j2g's report: no :analysis-failures under java/lang/classfile, jdk/internal/classfile,
#    java/lang/constant, jdk/internal/constant; :unavailable for the port only resolveConstantDesc, resolveCallSiteDesc,
#    ofClassLoading(Lookup), the canonicalize* and DynamicConstantDesc's bootstrap's; no collision; the Go
#    SharedSecrets_GetJavaUtilCollectionAccess__JavaUtilCollectionAccess written; Gatherers' four :unavailable gone
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 5. F26's smoke on the new executable, its output against the JDK's (byte for byte on the day's interpreted run; the
#    last line, ClassPrinter's, may say "not translated": jdk.internal.classfile.components is exported to no module the
#    REPL world roots, repl-visible?, and only the API's own calls reach it)
S=/root/arbace/.tmp/worktrees/java-classfile/.tmp/cf/smoke
(cd $S && ARBACE_PATH=$S/forms $OLDPWD/target/arbace-go/amd64/arbace $OLDPWD/test/java/run.ae --load files $S/forms \
   cfsmoke.Smoke -- jdkout/cfsmoke/Sample.class 'jdkout/cfsmoke/Sample$P.class' 'jdkout/cfsmoke/Sample$E.class' > night.out) \
  && diff $S/night.out $S/jdk.out && echo F26-SMOKE-OK
# 6. javac's tool (its world takes jdk/forms: the typeAnnotations tests run there)
bin/heavy-lock bin/javac-vendor tool
# 7. Java's levels interp and compiled on the 44 (the list: their ids, one a line; then the whole corpus with the night's
#    others): saved, not recorded;
#    expect 5-7 to pass at interp (t04835 t05856 t06042 t05994 t06004, t05992 t05993), t04835 and t06042 compiled
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --list /root/arbace/.tmp/worktrees/java-classfile/.tmp/cf/ids.txt
  --save .tmp/f26-night/check
```

**Risks and fallbacks.**

- If the edge update records none of the three classes, the port's code is stubs, because `BytecodeHelpers` is refused. The
  fallback is a variant of `BytecodeHelpers.isConstantDynamicBootstrap` and `DirectMethodHandleDesc$Kind`'s constants that
  writes the nine `REF_*` values as literals (the values of `.tmp/cf/edge-sim.edn`), plus DynamicConstantDesc's three
  canonicalizations cut.
- j2g's translation of the records and sealed hierarchies at this size (about 690 classes) cannot be measured by day. If the
  report refuses some classes, the triage's usual route is a variant for each.
- The accessor's class `ImmutableCollections$Access` is non-trivially initialized. If j2g's `trivial-init?` decides
  otherwise, the getter returns nil and the API fails with an NPE at its first list. The smoke (step 5) would show it.
- `Class.getResource`'s support method calls `ClassPathResources.getResource`, which the REPL world reaches today. If a build
  has no ClassPathResources, the method is not written (`cp-resources?`).

## Proposed (F26)

Numbered from JCS300, with JCS260-JCS299 left to the day's other branches.

### JCS300 The interp level's `test.classes`: the test's own class files

Eighteen of the 44 parse their own class files, from `test.classes` (jtreg's `@compile` output) or by
`Class.getResourceAsStream("X.class")`. bin/java-check compiles a test only on the JDK's side (`record_runs`), so Arbace's
run finds nothing, at interp and at compiled.

- **(a)** As now: the 18 fail on null bytes.
- **(b)** Every test: javac on Arbace (the tool, as the classes level runs it, with the test's javac options and `-g`)
  compiles its sources into `WORK/classes/ID` before its run, and `WORK/classes/ID` joins `ARBACE_PATH`, so a class's
  `getResource(AsStream)` finds its class file. Cost: one tool run per test (2-10 s), about +4 hours of CPU for the 4,853
  at night.
- **(c)** Only the tests whose sources name `java.lang.classfile` or `test.classes` (about 30). Cost: an hour to build; a
  few minutes a night.
- *Recommended: (c)*, the plan's `compile` line from the source scan, as F6's tool tests are chosen.

### JCS301 AccessFlag's reflection in jrt

`Class.accessFlags()`, `Field`, `Method` and `Constructor` `accessFlags()`, `Parameter`, `Class.getDeclaredClasses`, and
`Field.getAnnotation` in the interpreter's tables. This is the family of t04830-t04833. AccessFlag is translated, and jrt's
Go cannot name a translated class, so these are j2g support methods, as `Class.getResource` is
(`AccessFlag.maskToAccessFlags(getModifiers(), Location.X)`, the class-file flags where they differ: `ACC_SUPER`,
synthetic, mandated).

- **(a)** Defer them to F14's reflection.
- **(b)** Build them after the night proves the port.
- Cost of (b): half a day, with a jrt Go test each.
- *Recommended: (b).*

### JCS302 `ClassLoader.defineClass` of class files the API builds

Six tests build a class and define it: t04537, t04712-t04715 and t04865. The Go build defines classes at run time only from
Java forms (J2G-SPEC V9).

- **(a)** By design, a class `:define-bytes` of its own.
- **(b)** A class-file loader: a `ClassModel` decompiled into Java forms (structured control flow from the code's labels
  and stack maps) and then interpreted. Cost: weeks, and new risk to the interpreter.
- **(c)** A bytecode interpreter over the API's `CodeModel`. Cost: weeks, and a second evaluator.
- *Recommended: (a)*, revisited if the box's tool guests need class files.

### JCS303 The `:classfile` class after the night's rerun

`test/java/divergences.edn`'s `:classfile` is `:kind :design` (JCS95 (a)); JCS260 (b) reverses it.

- **(a)** Keep it until the records say what is left.
- **(b)** After the night's whole-corpus rerun, re-sort the 44 into the classes above: JCS300's, `:invoke`, the javac
  tool's, `jrt:/` by design and JCS302's. `:classfile` then keeps only what names the API's own missing pieces, as
  `:world`.
- REAP's cells and `bin/crops-score`'s scores move with it, so the re-sort is recorded with the night's records, not by day.
- *Recommended: (b)*, by the main session with the records.

### JCS304 `Runtime.Version.parse` in jrt

`ClassFileFormatVersion.runtimeVersion()` calls it, and jrt's hand-written `Runtime$Version` has no `parse`, so it is an
operation-level stub.

- **(a)** As now: only `runtimeVersion` fails.
- **(b)** jdk26u's `Runtime.Version.parse` ported rule for rule (its regex, its errors) into jrt's Go, with a test. Cost: an
  hour.
- *Recommended: (b).*

### JCS305 The stream variants that avoided JavaUtilCollectionAccess

`overlay/jdk/variants/ReferencePipeline.ae` (`toList`) and `Collectors.ae` (`toUnmodifiableList`) were written because
SharedSecrets' accessor was outside the world. With F26 it is inside.

- **(a)** Keep both.
- **(b)** Remove them once the night's smoke proves the accessor. jdk26u's own code then runs: `toList` over
  `listFromTrustedArrayNullsAllowed`.
- *Recommended: (b)*, the morning after the night's build.

## As built: JCS233 (a) with JCS140 (a): `java.home`, the crypto policy, cacerts (2026-11-08; branch `java-http`, write-ahead)

Branch `java-http` from `mac/java-props` (`a9aebc62f`), by day under the light rules: nothing converted, translated or built;
the night's commands below. jdk26u at `baf63fb`.

| part | what |
|---|---|
| `java.home` | `go/arbace/jrt/javahome.ae` (new): a directory per key, `$XDG_CACHE_HOME/arbace/java-home/KEY` (else `$HOME/.cache/...`), KEY the first 20 hex digits of the SHA-256 of the embedded files' names and bytes, written at the first read of the property (a temporary directory renamed into place; a loser of the race takes the winner's); `ARBACE_JAVA_HOME` overrides; `""` when none can be had (the box). doc/go/JRT-NOTES.md, "`java.home`". The decision's `$XDG_CACHE_HOME/arbace/KEY/` gained the subdirectory `java-home/`, so that the cache can hold other things later |
| the files | `javaHomeFiles`, at the image's paths: `conf/logging.properties`, `conf/net.properties`, `conf/security/java.security`, `conf/security/policy/` (README.txt; limited's `default_US_export`, `default_local`, `exempt_local`; unlimited's `default_US_export`, `default_local`), `lib/security/cacerts`, `lib/tzdb.dat` |
| the property | `system.ae`'s `initProps` puts the sentinel `javaHomeUnset`; `System_GetProperty_String__String` replaces it by `JavaHome` on the first read; `-Djava.home=...` (JAVA_TOOL_OPTIONS) and `setProperty` win as for any property |
| the data step | `bin/jrt-convert`'s generate: `conf/security/policy/` copied from `src/java.base/share/conf/security/policy/` as `GendataCryptoPolicy.gmk` installs it; `lib/security/cacerts` copied from the JDK build's `images/jdk/lib/security/cacerts` (JCS140 (a): GenerateCacerts needs KeyStore and CertificateFactory, which javac's tool's world lacks), `generated.edn`'s `:data` naming it `:build-image`; the step fails without a build image |
| the crypto policy | jrt's `JceSecurity` (overlay) gains jdk26u's `setupJurisdictionPolicies`, its static initializer, `getDefaultPolicy`, `getExemptPolicy` and `isRestricted` (the template's, `@@JCE_DEFAULT_POLICY@@` as `unlimited`, the build's UNLIMITED_CRYPTO default); without java.home the unlimited policy as before. `JceSecurityManager` (overlay) answers jdk26u's `getDefaultPermission` (no class has a code base in the Go build, so the JDK's walk would return the same). jca-sources + `javax/crypto/CryptoPermissions`, `CryptoPolicyParser` |
| not changed | `Security.ae`'s `loadMaster` (the embedded resource: it works without java.home); `ZoneInfoFile`'s, `NetProperties`' and `LogManager`'s variants; `FilePaths`, `TrustStoreManager`, `KeyStoreUtil`, `AnchorCertificates` are jdk26u's and now find `${java.home}/lib/security/cacerts` |

**Light checks.**
- The joint javac check (`.tmp/night7/all9-run.sh`): 2,508 files (2,506 on `java-props`), **0 errors**, 5,329 classes.
- `bin/repr-java read-jdk --check` of `JceSecurity`, `JceSecurityManager`, `CryptoPermissions`, `CryptoPolicyParser` (with the two
  of JCS230) as a patch of java.base: 6 converted, 0 failed, 0 javac errors; `--no-javac`: 6 native, 0 fallbacks, the forms
  identical byte for byte.
- `bin/jrt build --arch all` (go build and vet, linux/amd64 and arm64): `arbace/jrt` clean; `bin/jrt test --arch amd64`: `arbace/jrt` ok
  (5.1 s; the crops' `expat`, `pyexpat`, `hs`, `oi` fail by themselves, as on `next`). New: `TestJavaHome` (no cache directory,
  `ARBACE_JAVA_HOME`, no embedded file; the key's directory, its files and only the list's, made once, `HOME`'s `.cache`, another
  key for other data, eight concurrent first readers sharing one directory, no temporary left), `TestJavaHomeProperty` (the
  first read, `setProperty`, `-Djava.home`), `BenchmarkJavaHome` (0.26 ms a start once the directory exists). Negative control:
  the key cut to 16 digits fails `TestJavaHome` ("the directory: ...").
- The policy's code on the JDK: the overlay's `JceSecurity` and `JceSecurityManager` with jdk26u's `CryptoPermission`,
  `CryptoAllPermission`, `CryptoPermissions`, `CryptoPolicyParser` in a scratch package (`.tmp/jcs233/`, java.home read as the
  property), run on jdk26u's JDK for TestUnlimited's eleven cases: `use_default`, `unlimited`, `unlimited/` give AES and RC5
  unlimited and not restricted, `limited` 128 and restricted, the seven others "Can not initialize cryptographic mechanism",
  as TestUnlimited expects. The interpreter cannot run the scratch copy (it cannot extend `java.security.Permission` yet), so
  the Go build's run of it is the night's.

**Expected yield** (interp; compiled the same where j2g translates them): `javax/crypto/CryptoPermissions/CryptoPolicyFallback`
(t07905), `TestUnlimited` (t07906, eleven runs), `javax/net/ssl/sanity/CacertsExplorer` (t08012), `sun/security/rsa/TestCACerts`
(t08242): **4**, and `sun/security/lib/cacerts/VerifyCACerts` (t08202) with JCS235 (a)'s HashSet, 5 touched. At risk: TestCACerts'
110 signature checks at the interpreted level's speed; `Files.newDirectoryStream`'s glob (F9's provider) and `CryptoPolicyParser`'s
reflective `RC2ParameterSpec`/`RC5ParameterSpec` constructors (the limited policy) in the Go build, unchecked by day.

**Beyond the tests.** Every `TrustManagerFactory.init((KeyStore) null)` and `SSLContext.getDefault()` now has the JDK's
anchors: PKIX validation against public CAs as on the JDK (no network at check time, so the corpus does not show it).

**The night's commands** (in the night's worktree with this branch merged):

```sh
/root/arbace/.tmp/night7/all9-run.sh $PWD                     # 0 errors
# jdk/ converted afresh: + CryptoPermissions, CryptoPolicyParser (and JCS230's two, JCS254's); jdk/data + 7 files
# (conf/security/policy/*, lib/security/cacerts); generated.edn :data "lib/security/cacerts" :build-image
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules
bin/jrt-convert vet --strict                                  # JceSecurity's users unchanged; no refusal
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# a smoke: expect "home /…/arbace/java-home/<20 hex> true", "aes 2147483647", "limited 128", "anchors <about 110>"
X=target/arbace-go/amd64/arbace
$X -e '(println "home" (System/getProperty "java.home") (.exists (java.io.File. (str (System/getProperty "java.home") "/lib/security/cacerts"))))
       (println "aes" (javax.crypto.Cipher/getMaxAllowedKeyLength "AES"))
       (println "anchors" (count (.getAcceptedIssuers (first (.getTrustManagers (doto (javax.net.ssl.TrustManagerFactory/getInstance "PKIX") (.init nil)))))))'
$X -e '(java.security.Security/setProperty "crypto.policy" "limited") (println "limited" (javax.crypto.Cipher/getMaxAllowedKeyLength "AES"))'
JAVA_CHECK_EXE=$PWD/$X J2G_GO_EXE=$PWD/$X JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 4 \
  --only 'jdk/(javax/crypto/CryptoPermissions|javax/net/ssl/sanity|sun/security/rsa/TestCACerts|sun/security/lib/cacerts)' \
  --save .tmp/jcs233-night/check
```

**Fallbacks.** (1) j2g refusing `CryptoPolicyParser` (its reflection) or `CryptoPermissions` (serialization's fields): the report
names it; the fallback is the old `JceSecurity` and `JceSecurityManager` (the policy unlimited; TestUnlimited fails, the other
three still pass on java.home alone). (2) A start that cannot write its cache (a read-only home): java.home `""`, as before.

## As built: JCS230 (b): Kerberos's permissions, the rest by design (2026-11-08; branch `java-http`, write-ahead)

- **The permissions.** `test/jrt/jrt_sources.ae`'s `kerberos-sources`: jdk26u's (`baf63fb`)
  `src/java.security.jgss/share/classes/javax/security/auth/kerberos/DelegationPermission.java` and `ServicePermission.java`,
  KIND `share:java.security.jgss` (translated into java.base's tree as java.security.sasl's are), 933 lines. Each file holds its
  collection (`KrbDelegationPermissionCollection`, `KrbServicePermissionCollection`), so the proposal's "4 files" are 2. They
  need only `BasicPermission`, `Permission`, `PermissionCollection` (F4) and the object streams (JCS167); no variant, no Go. Go
  names: the four simple names collide with nothing in `jdk/forms`, `overlay/` or jrt.
- **The class.** `test/java/divergences.edn`'s `:kerberos`, `:kind :design`, `:levels [:interp :compiled]`, after
  `:design-also-compiled` and before `:j2g-unresolved` and `:security` (which the same tests' reasons match): `:tests` the five by
  design (t05298 `java/security/Principal/Implies`, which builds a `KerberosPrincipal`; t08051 `KerberosHashEqualsTest`, t08052
  `KerberosNullsAndDestroyTest`, t08199 `krb5/NullStringToKey`, t08201 `krb5/etype/UnsupportedKeyType`), and `:match` the
  Kerberos API's other classes, `sun.security.krb5`, `org.ietf.jgss` and `sun.security.jgss` (not the two permissions: a
  permission test failing for another cause is not hidden). Checked on stage 0: the file reads (89 classes), the regex finds
  "javax.security.auth.kerberos.EncryptionKey" and "Unable to resolve class: KerberosPrincipal", not "DelegationPermission" or
  "ServicePermission".
- **Light checks.** The joint javac check: 2,510 files, 0 errors, 5,333 classes. `read-jdk --check` (with JCS233's four): 0 failed,
  0 javac errors; `--no-javac`: native, identical.
- **Expected yield**: `javax/security/auth/kerberos/` DelegationPermissionCollection (t08048), DelegationPermissionHash (t08049),
  DelegationPermissionInit (t08050), ServicePermissionCollection (t08054), ServicePermissionTest (t08055): **5 pass** at interp
  and compiled; the other 5 move from deferred to by design (completeness's denominator unchanged, the strict count by 5).
- **The night's**: the conversion (`jdk/forms/javax/security/auth/kerberos/` + 2 files), `bin/jrt modules` (javax.security.auth.kerberos
  as java.security.jgss's), the edge (`javax.security.auth.kerberos` exported), the build, then
  `bin/java-check --levels interp,compiled --only 'jdk/(javax/security/auth/kerberos|sun/security/krb5|java/security/Principal)'`.

## As built: JCS254 (b): java.net.http's client without HTTP/3 (2026-11-08; branch `java-http`, write-ahead)

F10b's datagram channels are on `java-props` (this branch's base), as the decision's "after F10b" asks; HTTP/3 stays out all
the same (option (b)), so nothing here needs them. jdk26u at `baf63fb`, `src/java.net.http/share/classes` (313 files, 101,314
lines).

**What comes in.** `test/jrt/jrt_sources.ae`'s `http-client-sources`: the module but `jdk.internal.net.http.http3`, `.quic`,
`.qpack`, the eleven `Http3*` classes, `HttpQuicConnection` and `H3FrameOrderVerifier` (156 files, 51k lines, out): **155 files,
49,719 lines** (`java.net.http`'s API 13 less package-info, `jdk.internal.net.http` 57, `common` 27, `frame` 18, `hpack` 22,
`websocket` 18), KIND `share:java.net.http`; 140 listed, 15 derived overlays (below); **385 classes**. HTTP/1.1, HTTP/2 (frames,
HPACK, ALPN over F3's SSLEngine), WebSocket, the body handlers, publishers and subscribers, authentication, cookies, redirects,
proxies; over F10a's `SocketChannel` and `Selector` and F16's `java.util.concurrent` (`Flow`, `CompletableFuture`).

**The cut, as derivations** (`test/jrt/derive.ae`'s `http-derivations`, JCS166 (a)'s mechanism, `bin/jrt-convert derive
--check`): the decision named variants, but the joint javac check and the read see jdk26u's Java, where a file naming
`Http3ClientImpl` does not compile without it; a derived overlay is the cut at the source level, exact and re-derivable.
The client a JDK has when its `SSLContext` is not QUIC compatible (`HttpClientImpl.client3 == null`) is the model: every
HTTP/3 path is the JDK's own "no HTTP/3" path.

| overlay | change |
|---|---|
| `HttpClientImpl` | `isQuicCompatible` false, `client3` an `Object` that stays null, its stop and abort gone; the selector manager's thread `new Thread(selmgr, name)` + `setDaemon(true)` (jrt has no `Thread.Builder.OfPlatform`, no `inheritInheritableThreadLocals`; the JDK's default is a virtual thread, a daemon) |
| `ExchangeImpl` | `attemptHttp3Exchange` through the HTTP/3 `createExchangeImpl` cut (one block); `get`'s HTTP/3 branch fails with "HTTP3 is not supported" (unreachable: `MultiExchange` downgrades first, `sendAsync` fails an `HTTP_3_URI_ONLY` request first) |
| `Exchange`, `HttpConnection`, `Response` | no `HttpQuicConnection`: the aborter's QUIC connection never set, no HTTP/3 connection, no QUIC TLS session |
| `AltSvcProcessor` | `Http3ClientProperties.ALTSVC_ALLOW_LOCAL_HOST_ORIGIN` read in place (the same property and default) |
| `common/Log` | the QUIC packets' and frames' logging cut (the ten `quic` imports, `isLogging(QuicFrame)` to `quicPacketInLoggable`, `logQuicPacketOut/In`) |
| `common/Log`, `Logger`, `DebugLogger`, `Utils`, `hpack/HPACK`, `Http1Response`, `WindowController`, `RawChannelTube` | `System.Logger` as `jdk.internal.jrt.SystemLoggers.Logger`, `System.getLogger` as `SystemLoggers.getLogger` (F24's, JCS155 (b); dropped with JCS250 (a)) |
| `HttpClientFacade`, `RawChannelTube` | no `Cleaner` (not in the Go build): the selector manager notices a collected facade at its next wake-up (`NODEADLINE`, 3 s, as the JDK's loop does); WebSocket's debug-only `CleanupChecker` not registered |

**j2g's names** (`arbace/j2g/names.ae`): `Hc_HttpClient`, `Hc_ExchangeImpl`, `Hc_HeaderParser`, `Hc_HeaderParser_ParserIterator`,
`Hc_HttpConnection`, `Hc_Stream`, `Hc_Logger`, `Hc_Utils`, `Hc_ISO_8859_1`, `Hc_StringReader`, `Hc_StringWriter`: the 385 classes'
Go names against the closure's 5,718 classes (javac's class files, nested and anonymous ones included) and jrt's 305 Go types,
these eleven the collisions.

**Light checks.**
- The closure (F1's scan, `.tmp/http/closure2.py`, the overlays first): outside the world and the set only `Object`, `Class` (jrt's)
  and `MethodHandles`/`VarHandle` (`Http1Response$ClientRefCountTracker.STATE`, `Stream.DEREGISTERED`: `findVarHandle` constants,
  J2G-SPEC §8.5).
- The joint javac check: **2,665 files, 0 errors**, 5,718 classes (23 s, 1.0 GB). Negative control: `HttpClientImpl`'s overlay moved
  away, 63 errors, exit 1.
- `bin/jrt-convert derive --check`: **39 overlays** (24 + 15), 0 not as derived, its own control passing.
- The read: `bin/repr-java read-jdk --check` of the 155 with SystemLoggers and java.util.logging (175 files): 175 converted, 0 failed,
  0 javac errors (26 s, 1.4 GB); `--no-javac`: 175 native, 0 fallbacks, the forms identical; 1.33 MB of forms for the module.
- **j2g's analysis by day** (`.tmp/http/vetns/jrt/hvet.ae`, `bin/jrt-convert vet`'s `jrt.vet` with the targets a regex): the committed
  `jdk/forms` with the day's read of the 175 (and UndeclaredThrowableException, JCS170's, not converted yet) entered, declared,
  resolved and bridged as j2g does, then every class of `java.net.http` and `jdk.internal.net.http` analyzed: **385 classes, 0
  refused** (4,908 in the world; 24 s, 1.67 GB). Its first run refused `HttpClientImpl` ("Unknown class:
  java.lang.Thread$Builder$OfPlatform"), which the derivation's thread fixed. The same run with JCS233's and JCS230's classes:
  `DelegationPermission`, `ServicePermission`, `CryptoPermissions`, `CryptoPolicyParser`, `JceSecurityManager` analyze;
  `JceSecurity` waits for F9's `DirectoryIteratorException` (in the sources, converted tonight).

**Expected yield**: `:net`'s 8 (`jdk/java/net/httpclient/` HeadersTest t05071, HeadersTest2 t05072, Http1ChunkedTest t05073,
HttpRequestBuilderTest t05074, InterruptedBlockingSend t05075, ShortRequestBody t05076, TimeoutOrdering t05077, `http2/Timeout`
t05078): the builders' tests need no server; the others a `ServerSocket` (or an `SSLServerSocket`, http2/Timeout) on the loopback
address. About **6 at interp** (the timing tests, TimeoutOrdering and InterruptedBlockingSend, at risk at the interpreted speed),
about 6 compiled. Beyond the corpus: `HttpClient` is the JDK's HTTP API, for every program of the Go build.

**JCS135, the closure's growth**: +49,719 Java lines (+6.5% on the closure's ~760k), the executable about +7 MB by F4's ratio.

**The night's commands** (in the night's worktree with this branch merged; heavy ones under `bin/heavy-lock`):

```sh
/root/arbace/.tmp/night7/all9-run.sh $PWD                       # 2,665 files (+ the night's other branches), 0 errors
bin/jrt-convert derive --check                                  # 39 overlays, 0 not as derived
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update     # + 140 share:java.net.http, 15 overlays; 0 failed
bin/jrt modules                                                 # java.net.http, jdk.internal.net.http.* as java.net.http's
bin/jrt-convert vet --strict
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge   # java.net.http exported
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
grep -A12 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn   # none of java/net/http, jdk/internal/net/http
# a smoke on loopback: jdk.httpserver's server (F24) and the client; expect "200 ok HTTP_1_1"
target/arbace-go/amd64/arbace -e '(import (quote (com.sun.net.httpserver HttpServer HttpHandler)) (quote (java.net InetSocketAddress InetAddress URI))
    (quote (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers)))
  (let [s (HttpServer/create (InetSocketAddress. (InetAddress/getLoopbackAddress) 0) 0)]
    (.createContext s "/" (reify HttpHandler (handle [_ x] (let [b (.getBytes "ok")] (.sendResponseHeaders x 200 (alength b))
                                                            (with-open [o (.getResponseBody x)] (.write o b))))))
    (.start s)
    (let [c (HttpClient/newHttpClient)
          r (.send c (.build (HttpRequest/newBuilder (URI. (str "http://127.0.0.1:" (.getPort (.getAddress s)) "/")))) (HttpResponse$BodyHandlers/ofString))]
      (println (.statusCode r) (.body r) (.version r)))
    (.stop s 0))'
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 4 \
  --only 'jdk/java/net/httpclient/' --save .tmp/jcs254-night/check
```

**Risks and fallbacks.** (1) A refusal in the whole world the day's analysis did not see (the night's world is newer): the report
names it; a fix is a derivation more; the fallback drops `http-client-sources`' line and the 15 overlays (the renames stay harmless).
(2) The HTTP/2 tests' TLS (ALPN `h2` over F3's SSLEngine). (3) The selector manager's loop at the interpreted level (its select
wakes at least every 3 s).

**What is left of java.net.http**: HTTP/3 and QUIC (JCS254 (a): 156 files over F10b's `DatagramChannel` and the JDK's QUIC TLS
engine, `sun.security.ssl.QuicTLSEngineImpl`, which F3's forms already hold), when a corpus test needs it. Of
`test/jdk/java/net/httpclient`'s 378 tests the corpus holds these 8: 259 name an `@library` (the test library's HTTP/1.1, HTTP/2
and HTTP/3 servers, `/test/jdk/java/net/httpclient/lib`, and `/test/lib`), and most of the other 119 run as TestNG or JUnit
tests (63 and 30 `@run` lines), which the corpus's categories leave out: JCS311 below.

## Proposed (java-http: JCS233, JCS230, JCS254)

Numbered from JCS310 (JCS258-JCS309 left to the day's other branches).

### JCS310 Where the checks' java.home goes

jrt writes java.home under `$XDG_CACHE_HOME` or `$HOME/.cache` (JCS233 (a)'s "the user's cache"), so every gate step, Java
check and test run that reads `java.home` writes one directory per data key (about 360 KB) into the host user's cache, outside
the checkout and the clean run's fresh clone; RT.doInit's `System.getProperties()` reads it at every start.
- (a) `ARBACE_CACHE_HOME`, read by jrt before `XDG_CACHE_HOME` (Arbace's cache root alone: Go's own build cache, which reads
  `XDG_CACHE_HOME` when `GOCACHE` is unset, is not moved), set by `bin/lib/tmpdir.bash` to the checkout's `.tmp/cache` when unset:
  the checks write only inside the checkout, the clean run inside its clone. An hour; `TestJavaHome` gains the variable.
- (b) As built: the user's cache for everything; a few keys a week (the data changes with the conversion).
- *Recommended: (a)*: the checks' outputs stay in `.tmp/`, as the rest of the scratch does.

### JCS311 java.net.http's jtreg tests that use the test library

`test/jdk/java/net/httpclient` has 378 tests; the corpus holds 8 (JCS254's). 259 name an `@library` (the HTTP/1.1, HTTP/2 and
HTTP/3 test servers of `/test/jdk/java/net/httpclient/lib`, `jdk.test.lib`), and 93 of the rest run under TestNG or JUnit:
the corpus's categories (`:run-plain`, `:run-compile`, `:run-jitopts`, `:compile-fail-ref`) take neither.
- (a) A survey of `@library` and TestNG/JUnit runs for Java's check (JC-level: the libraries' sources read and loaded with the test,
  TestNG's and JUnit's runners as forms or a minimal runner of Arbace's own that reports as jtreg does), its first corpus the
  HTTP client's: 1-2 days of survey, then the runner; hundreds of tests across `jdk/` beyond these.
- (b) Deferred: the 8 stand for the client until the check takes libraries.
- *Recommended: (a)* as its own survey, after the Java night records JCS254's 8: the libraries are the largest part of jtreg the
  check does not reach.

### JCS312 Security's `loadMaster` from java.home

With java.home a directory (JCS233), jdk26u's `Security.SecPropLoader.loadMaster` could read `${java.home}/conf/security/java.security`
as the JDK does, and `Security.ae`'s `loadMaster` variant (the embedded resource) could go.
- (a) The variant dropped: the JDK's code; but a Go build without java.home (the box, a read-only home) then has no security
  properties at all and every provider lookup fails.
- (b) The variant kept: the resource is the same bytes as the file; the JDK's own readers of the file (the tests, `FilePaths`) read
  java.home's.
- *Recommended: (b)*.

### JCS313 `bin/jrt-convert vet --classes RE`

JCS254's day check analyzed a new slice's classes (385) on the committed world plus the day's read, by a scratch copy of
`jrt.vet` whose targets are a regex (`.tmp/http/vetns/jrt/hvet.ae`): 24 s, 1.67 GiB, and it found what the night would have
(`Thread.Builder.OfPlatform`, fixed the same day). Any slice that adds sources has the same need.
- (a) `vet --classes RE [--forms-add DIR...]`: the classes matching RE analyzed beside the variants', DIRs' forms laid over the
  committed ones (the day's `read-jdk --check` output); an hour, documented in the triage's checklist for new slices.
- (b) Left as the scratch script.
- *Recommended: (a)*.

## Decided (the user, 2026-11-09, about 06:15: the Java proposals of 2026-11-08's branches)

All as recommended: JCS280 (a), JCS281 (b), JCS282 (b), JCS283 (b), JCS284 (the remote connectors deferred with RMI, HotSpot's
beans by design), JCS285 (b), JCS290 (b), JCS291 (a) if a test asks, JCS292 (b), JCS293 and JCS304 as one port (JCS304 (b)),
JCS300 (c), JCS301 (b), JCS302 (a), JCS303 (b), JCS305 (b), JCS311 (a), JCS312 (b), JCS313 (a), JCS320 (a), JCS321 (a) when
measured, JCS322 (b), JCS323 (a) after the night, JCS324 (a) folded into JCS283's verb, JCS325 (a), JCS326 (a); JCS256 (a) from
the morning after the conversion is committed, JCS257 (a) with it; JCS213: the lookup as built (the hook not needed). Built on the
branch `java-props2` (from `java-night`), one commit a proposal; the sections below.

## As built: JCS300 (c): the test's own class files at interp and compiled (branch `java-props2`, write-ahead)

`bin/java-check`'s `classes_test` chooses the tests as F6's `tool_test` chooses the tool tests: a source line of the test's that is
no comment line names `java.lang.classfile` or `test.classes` (`cf_re`). At the platform's pin that is 57 tests of the corpora
(36 name the API, 18 `test.classes`, 3 both): the 18 of F26's table that read their class files, the tests that write into
`test.classes` (t05046's and t08142's jars), the launcher tests that start `java -cp ${test.classes}`, and the API's tests.
`check_one` compiles such a test's renamed sources, once the read passed and when the levels hold interp or compiled, by javac on
Arbace (`bin/repr-java javac`, the tool of `REPR_JAVA_EXE`) with the options `record_runs` gives the JDK's javac for the
reference (`-nowarn -XDsuppressNotes -implicit:none` and the test's own) into `WORK/classes/ID`, the directory both sides pass
as `test.classes`. `run_actions` puts that directory on `ARBACE_PATH` after the forms, so `ClassPathResources` and the support
methods' `ResourceOrPath` (`Class.getResource`, `getResourceAsStream`) find `X.class` as the JDK's class path does. A javac
failure is said on stderr (the log `RUN/ID/test-classes.log`) and leaves the directory empty: the test runs as before. A
committed-forms run (`--essential`) has no sources and compiles nothing (none of the 57 is essential).

By day, on the gate's executable (F26's port is not in it yet): t07651 passes as before with its classes; t08142 (`B6449504`,
its jar written under `test.classes`) passes where the record says fail; t05046 gets past its missing directory to
`ClassNotFoundException: Foo` (a URLClassLoader over a jar). The night's whole-corpus rerun takes it with no new command: the
Java night's `bin/java-check --levels interp` and `--levels compiled` runs with `REPR_JAVA_EXE` set compile the 57 (a tool run
each, 2 to 10 s). Expected: F26's 18 can pass at interp once the API is in the world (the night's build); the rows of t08142 and
the launcher tests may move (recorded by the night, not by day).

## As built: JCS285 (b): `bin/jrt test --package P`, the day's form (branch `java-props2`)

`bin/jrt build` and `bin/jrt test` take `--package P` (repeated, or `P,Q`): the go command builds, vets or tests `./P` alone (the
package's directory in the printed module: `jrt` for `arbace/jrt`), one package at a time (`-p 1`), in place of `./...`. The
whole module's peak was not go's compiles alone: the print of `go/` (`bin/build-go --print-only` on stage 0) peaks at 4.6 GB
in 17 s; under `--package` it runs with `GOMEMLIMIT=1500MiB` (unless set), 1.7 GB in 32 s. Measured on `java-props2` (busybox
`time -v`, every process of the run): `bin/jrt test --arch amd64 --package jrt -count=1` 1.59 GB peak, 38 s (`ok arbace/jrt
5.0s`); `bin/jrt build --arch amd64 --package jrt` (go build and go vet) 1.59 GB, 51 s; go's compile and test of `./jrt` alone
from a cold cache 0.94 GB, 30 s. Without `--package` nothing changes (the night's `bin/jrt test`).

## As built: JCS313 (a): `bin/jrt-convert vet --classes RE [--forms-add DIR]...` (branch `java-props2`)

`jrt.vet` (test/jrt/vet.ae) takes the day's check of a new slice that JCS254 ran as a scratch copy (`hvet.ae`):

- `--classes RE`: the classes whose top-level internal name RE finds (`java/net/http/HttpClient`) are analyzed beside the variants'
  (j2g's `analyze-class!`, bodies and all, as the variants' are). Without a variant FILE named, no variant file is: the slice alone.
  Their failures are listed as `REFUSED  --classes: CLASS: MESSAGE`; a known row `["--classes" CLASS PREFIX WHY]` of
  `test/jrt/vet-known.edn` holds one. The summary counts them (`N classes of --classes`).
- `--forms-add DIR` (repeated): DIR's forms laid over the world's (`--forms`, default `jdk/forms`) by their path below the root: a
  file of DIR replaces the world's at the same path, the rest are added. DIR is the day's read of the slice's new and changed
  files (bin/repr-java read of them, or `read-jdk --check`'s output), not a whole conversion.

Proved on `java-props2` against JCS254's day result: the 15 entries in which java-http's day forms
(`.tmp/http/forms`) differ from `jdk/forms` (java.net.http, jdk.internal.net.http, the JCE's and Kerberos's files,
`SystemLoggers`), 173 files, as DIR: `bin/jrt-convert vet --classes '^(java/net/http|jdk/internal/net/http)/' --forms-add DIR
--others` declares 4,918 classes (hvet's own count), analyzes the 373 classes of the two packages and refuses none, in 20 s at
1.45 GB. With only the two packages as DIR it refuses 113 (`Unknown type: SystemLoggers$Logger`): the slice's other files are
part of what DIR must hold. `bin/jrt-convert vet` alone is unchanged (the same files in the same order).

**The checklist for a slice that adds sources** (each step light): (1) the closure's files added (test/jrt/jrt_sources.ae) and the
joint javac check (`all9-run.sh`); (2) the new and changed files read by day into DIR (their forms at their paths below a forms
root); (3) `bin/jrt-convert vet --classes RE --forms-add DIR --others`, RE the slice's packages: no refusal, or each one in the
slice's own triage with its fix; (4) the night's conversion and build as usual.

## As built: JCS257 (a): the derived overlays' headers name `test/jrt/derive.ae` (branch `java-props2`)

The 18 derived overlays whose headers still named their first scratch scripts (`.tmp/f9/derive.py` for F9's thirteen,
`.tmp/unix/derive.py` for UnixDomainSockets, `.tmp/f10/gennet.py`, `genpoll.py`, `genopts.py` for Net, PollSelectorImpl and
SocketOptionRegistry) name `test/jrt/derive.ae` instead, in derive.ae's header texts and in the committed overlays alike (the
comments of derive.ae's table keep the scripts' names, as the derivations' first record). Code unchanged; `bin/jrt-convert derive
--check`: 79 overlays, 0 not as derived, the control failing as it must. The overlays' text moves `jdk/provenance.edn`'s digests:
the night's `bin/jrt-convert update` takes it with the rest (JCS257 (a)'s "at the next conversion"). The F1 services
(`JceServices`, `RsaServices`, "the F1 scratch script gensvc.py/genjava.py") are not derive.ae's: hand-kept, unchanged.

## JCS256 (a): `jrt-vet`, tomorrow's switch (branch `java-props2`, write-ahead)

Decided (a), from the morning after the Java night's conversion is committed: until `jdk/forms` is the night's, `bin/jrt-convert
vet` refuses what the variants already expect of the new world (on `java-props2` today, over the committed `jdk/forms` of
2026-11-07: 145 variant files, 4,530 classes, 10 refused, among them `File`, `DatagramSocket`, `AbstractInterruptibleChannel` and
`ProviderConfig`, and 2 variants whose classes are not yet in it), so the step is not in `test/gate/steps.edn` tonight. Tonight it
is the Java night's (JCS256 (b)): `bin/jrt-convert vet --forms .tmp/jrt/conv --strict` after `bin/jrt-convert update`'s
conversion, before the build. The switch, for the morning after (one commit, after `jrt-derive` in `test/gate/steps.edn`):

```clojure
  ;; JCS256 (a) (the user, 2026-11-09): jrt's Go-build variants vetted on j2g's world (bin/jrt-convert vet, test/jrt/vet.ae,
  ;; JCS253): every variant file's classes entered from jdk/forms with their variants applied and analyzed as j2g's analysis
  ;; does, a refusal not in test/jrt/vet-known.edn failing, a stale known row too (--strict); about 20 s at 1.5 GiB, stage 0
  {:name jrt-vet :phase :essential :mem-gib 2 :cmd "bin/jrt-convert vet --strict"
   :hosts [:stage0]
   :inputs [:lib-tools "bin/lib/heavy.bash" "bin/lib/repr-java/host.bash" "bin/lib/javac-key.bash" "bin/jrt-convert"
            "test/jrt/vet.ae" "test/jrt/vet-known.edn" "overlay/jdk/variants/" "jdk/forms/" "jdk/edge.edn" :tools]
   :summary ["^jrt.vet:|^REFUSED|^MISSING|^stale" 6]}
```

Then `bin/gate --plan` (the table's names and inputs) and `bin/gate --audit jrt-vet` once, whose inputs the audit settles. It
needs no executable (stage 0's j2g namespaces, the checkout's loaded where they differ: `:tools`).

## As built: JCS283 (b), JCS324 (a): `bin/jrt-convert predict`, the night's world by day (branch `java-props2`)

`bin/jrt-convert predict [--since REV] [--list FILE] [--missing RE] [--out DIR] [PATH...]` makes the scratch procedure of JMX-1's,
F27's and JCS265 (b)'s days one light verb (into `$JRT_WORK/predict`, or `--out DIR`):

1. **The closure's sources**, as the sources step makes them (`make_sources`: `DIR/src`, `DIR/sources.txt`).
2. **What changed** against the committed conversion (`DIR/changed.txt`): the lines of the new `sources.txt` that `jdk/sources.txt`
   lacks (a new file, or one whose kind moved), and the overlays of the closure changed since REV (default: the commit that last
   wrote `jdk/forms`; derived overlays among them); with PATHs (below java.base's root), those files alone, the rest of the world
   `jdk/forms`'. The closure's dropped files (`DIR/removed.txt`) leave the world. Generated sources (gensrc, jrtgen, the locales)
   are the night's, not predicted.
3. **The read**, as the convert step reads (`bin/repr-java read-jdk --check`, `--no-javac` by default, `REPR_JAVA_PATCH_ALL`, the
   origins), of the changed files with their context: every overlay of the closure, and the closure's files of the changed files'
   packages and of their other modules (`share:MODULE`); `GOMEMLIMIT=1500MiB`, two threads (`REPR_JAVA_THREADS`). A context file
   that fails for its own context's absence is not the prediction's; a changed file not read is listed (`DIR/unread.txt`, status 1).
4. **The world** (`DIR/world`): a copy of `jdk/forms`, each changed file's forms over it, its package file's loads merged (the
   committed order, then the new ones), the dropped files' forms and loads removed.
5. **On it**: `bin/jrt-convert vet --forms WORLD --classes CHANGED --others` with every variant file (JCS253, JCS313 (a)); j2g's
   analysis of the whole world, `bin/j2g --analyze-only --jdk WORLD` with **`J2G_MISSING`** (JCS324 (a): the changed classes, or
   `--missing RE`'s: what their translation would stub at the operation level; `DIR/j2g-findings.txt`, `DIR/j2g/report.edn`); and
   with `--list FILE` those tests analyzed on the world (`bin/java-check --levels read,compiled --analyze-only`, `JRT_CONV=WORLD`,
   shards of 30, two jobs; `DIR/check/results.tsv`; the executable `JAVA_CHECK_EXE`, else `J2G_GO_EXE`'s).

Measured on `java-props2` (main's stage 0, the gate's executable of 2026-11-08 as `J2G_GO_EXE`, javac's tool of `javac-5ad35733`
with `REPR_JAVA_STALE_OK=1`): `predict java/lang/reflect/AccessFlag.java jdk/internal/jrt/AccessFlagSupport.java` reads 203 files
(201 converted: the locale overlays' two fail for their data, as in java-more's day), and vet refuses AccessFlag's five classes
for `ClassFileFormatVersion` and `ClassFileAccess`, which the slice also holds: with them as PATHs (four files) the slice's 7
classes analyze; j2g's analysis of the world 15 failures, all of today's committed `jdk/forms` (File, DatagramSocket, the variants
of classes the conversion has not yet brought: the night's), no stub in the slice; `--list` t04830, t04835: both analyze, as
java-more's prediction found. 191 s and 2.07 GB peak (j2g's `GOMEMLIMIT` 1800MiB), the java-check part 112 s at 2.05 GB.
Without PATHs on today's branch the change is the night's whole conversion (1,058 files since `b6a6093af`): that read is the
night's (`bin/jrt-convert update`), not the day's; `predict` by day is for a slice's files after a conversion is committed.

### java-props2: tonight's part, the light checks

On `java-props2` (`java-night` at `33d04049c` with JCS300 (c), JCS285 (b), JCS313 (a), JCS257 (a), JCS325 (a), JCS283 (b) with
JCS324 (a), JCS256 (a)'s switch written): the joint javac check (`.tmp/night7/all9-run.sh`) 3,315 files, 0 errors, 6,937
classes (20 s, 1.0 GB); `bin/jrt-convert derive --check` 79 overlays, 0 not as derived; `bin/jrt test --arch amd64 --package jrt`
ok (1.6 GB) and `bin/jrt build --arch amd64 --package jrt` (go vet) clean; `bin/gate --plan` 50 essential steps, no table error.
`bin/reap`: after `java-night`'s own re-record (187a55e8b, merged) and the day's classes, `bin/reap --record` on the branch
(the classes' rows alone move), then 0 problems; `bin/crops-score --judge --strict` 0 problems, the scores unchanged.

## As built: JCS304 (b), JCS293: `Runtime.Version.parse` and its parts, one port (branch `java-props2`)

jrt's hand-written `Runtime$Version` (go/arbace/jrt/system.ae) gains jdk26u's `Version.parse` ported rule for rule
(`Runtime_Version_Parse_String__Runtime_Version`): null a NullPointerException; `isSimpleNumber`'s shortcut (digits, the first not
0; the empty string too, whose `Integer.parseInt` throws); `VersionPattern.VSTR_PATTERN` as a Go regexp, anchored as
`Matcher.matches`; each number by `Integer.parseInt` (`NumberFormatException` "For input string: \"S\"" above `MAX_VALUE`); the
build; the three `IllegalArgumentException`s of `+`. The struct keeps the numbers of any count (`nums`, beside the four), the build
(`compareTo`'s `compareBuild`, `equals`, `hashCode`'s `Optional<Integer>`, `toString`'s `+BUILD`), the deprecated `major`,
`minor`, `security`, and `compareToIgnoreOptional`/`equalsIgnoreOptional` (Basic.java's other two; jdk26u's `compare(obj, true)`). `bin/jrt manifest` (the frozen JVM's reflection) adds `parse`, `major`, `minor`, `security` and the two `IgnoreOptional`s to the manifest and
the member tables, nothing else moving. `version()`, `pre()`, `build()` and `optional()` return translated `List` and `Optional`:
j2g's support methods on `*Runtime_Version` (arbace/j2g/out.ae's support forms, `Optional.of`/`empty`, `Integer.valueOf`,
`List.of`), named in `world-scan` so that j2g translates their calls, their callees rooted in main.ae, and entered into
`Runtime$Version`'s member table for the interpreter (the support table's init), as `String.describeConstable` is.

Checked by day: `TestRuntimeVersionParse` (jrt) holds 11 valid strings (toString, the four numbers, the count, pre, build,
optional, hashCode) and 12 invalid ones (each message), the exceptions' classes and compareTo/equals with builds, against the
platform's jdk26u build's `java` (a probe of 2026-11-09, `.tmp/verref/V.java` on the branch's worktree: its output is the test's
table); `TestRuntimeVersion` unchanged; `bin/jrt test --arch amd64 --package jrt` and `build --package jrt` (vet) clean;
`bin/j2g-unit` passes (the changed j2g namespaces load). The support methods are compiled by the night's build only (no day
build): expected, RuntimeTests' Version/Basic (t04598) passes at interp and compiled, and `ClassFileFormatVersion.runtimeVersion()`
(F26) is no longer a stub.

## As built: JCS320 (a): `Thread.getAllStackTraces` (branch `java-props2`)

j2g writes `Thread_GetAllStackTraces__Map` into the program's support code when HashMap is translated (arbace/j2g/out.ae;
`m/j2g-static?` names it, so that translated calls reach it and reachability counts it; its HashMap constructor and `put` rooted
in main.ae): jdk26u's map of the live threads to their stacks, over F13's facts, `liveThreads` (HostManagement's `threads0`)
and `stackOf` (its `stackTrace0`): the current thread's own frames, a waiting thread's as it began to wait (F13 slice 4's records),
an empty array for a running thread (its goroutine's stack cannot be read from another: jdk26u leaves out only terminated threads,
so every live thread is a key). The interpreter finds it in Thread's member table (the support table's init). Checked by day:
bin/j2g-unit passes (the namespaces load); the code is the night build's. Expected after the night: t04684
(GenerifyStackTraces) gets past `No accessible static method getAllStackTraces` at interp (its own verdict then needs the
waiting threads' stacks, which F13 records once `trackLocks0` ran); t05256 (RuntimeThreadInheritanceLeak) no longer stubs it,
its other gaps RMI's (`:rmi`).

## As built: JCS282 (b): the type annotation records checked against javac's, light (branch `java-props2`)

JCS271 (b)'s day comparison (`.tmp/ta`, a scratch Python normalizer) is now committed and runs on stage 0 with no javac:

- **The sample** `test/j2g/unit/typeann/p/TA.java` (JCS271 (b)'s: every declaration target, path step and element value kind)
  and its forms `p.ae`, `p/TA.ae` (repr/java's read on javac's tool, the same forms byte for byte as JCS271's day read).
- **javac's side**, recorded by hand with the JDK: `javac -d DIR p/TA.java`, then `arbace_tool -p test -m j2g.typeann record DIR
  test/j2g/unit/typeann.edn` (test/j2g/typeann.ae: the class files' constant pools and the fields', methods' and classes'
  RuntimeVisibleTypeAnnotations decoded, JVMS 4.7.20 and 4.7.16, the indices resolved): 29 annotations of 7 class files.
- **The check**, bin/j2g-unit's `test-type-annotations` (5.6 s, 0.6 GB with the rest): the forms declared and analyzed in a unit of
  their own on the tools' host (`j2g.typeann/encoder`, as JCS271's `enc.ae` did), each class's `annot/class-data` Y and N records
  decoded the same way, the two sets compared: **29 of 29 equal but 1 known**, `@B(e = ElementType.METHOD)` (the encoder writes a
  string where the world does not know `java.lang.annotation.ElementType` as an enum: JCS271 (b)'s day finding; the row is held to
  differ, so the night's world that knows the enum shows it as a stale known row); the negative control, the first row with an int
  constant changed, is not javac's.

Not a gate step yet: bin/j2g-unit is in none (a proposal for the essential gate would key it by `test/j2g/` and `arbace/classes/`
and `arbace/j2g/`, seconds).

## As built: JCS280 (a): `java.lang.reflect.Parameter`, synthesized parameters (branch `java-props2`, write-ahead)

- **Parameter in the world**: jdk26u's `java/lang/reflect/Parameter.java` as a derived overlay (test/jrt/derive.ae's
  `parameter-derivation`, five replacements): what it asks of its executable beyond jrt's hand-written Executable goes through
  jrt's own Java: `getAllGenericParameterTypes()[i]` (package-private in jdk26u's Executable) is
  `jdk.internal.jrt.ParameterSupport.genericParameterType` (jdk26u's branch without real parameter data: the generic types when
  as many as the parameter types, else the parameter types), `getSharedParameterTypes()[i]` is `getParameterTypes()[i]`,
  `getAnnotatedParameterTypes()` is `TypeAnnotationSupport.annotatedParameterTypes` (JCS271 (b)'s), `getParameterAnnotations()[i]`
  is `ParameterSupport.parameterAnnotations` (Method's or Constructor's, which jrt's Executable_I does not declare), and
  `accessFlags` is `AccessFlagSupport.parameterAccessFlags` (Location.METHOD_PARAMETER, in place of AccessibleObject's
  ReflectionFactory). Every overlay is in the closure (print-sources), so the night's conversion takes both new files.
- **getParameters**: j2g writes `GetParameters__Parameter1` on jrt's Method and Constructor once Parameter is translated
  (arbace/j2g/out.ae's support forms; main.ae's scan names them, its roots Parameter's constructor), jdk26u's
  `synthesizeAllParams`: `arg0` ... with modifiers 0, a new array each call; the interpreter finds them in the member tables (the
  support table's init). A call through a receiver of static type Executable stays a stub (jrt's Executable_I has no
  getParameters, as it has no accessFlags: JCS273's rule).
- **Not here** (JCS280 (b), with JCS273's decision's second half): MethodParameters records for `@compile -parameters` (names,
  FINAL, SYNTHETIC, MANDATED): MethodAccessFlagTest (t04832) checks a constructor's mandated parameter, so it analyzes but its own
  verdict needs (b).

Checked by day: `bin/jrt-convert derive --check` 80 overlays, 0 not as derived; the joint javac check 3,317 files, 0 errors;
bin/j2g-unit passes; `bin/jrt-convert predict` of the reflection slices with Parameter (78 files and their context; 5 min,
2.07 GB): Parameter is refused only for its context (AnnotatedElement's generics in the edge, tonight's `bin/j2g-edge update`;
AnnotationSupport for F14's AnnotationFormatError, tonight's conversion), ParameterSupport analyzes, and of t04830, t04832,
t04869 and t04870 all four analyze. Expected after the night: t04869 (GetAnnotatedTypeTest) and t04870 (NoName) can pass;
t04723 is no longer refused for Parameter.

## JCS301 (b): built by JCS273, the rest named (branch `java-props2`)

F26's JCS301 listed Class/Method/Constructor/Field `accessFlags`, `Parameter`, `getDeclaredClasses` and `Field.getAnnotation` for
t04830-t04833. JCS273 (java-more, merged on java-night) already writes the four accessFlags as j2g support methods with their
member table entries; `Field.getAnnotation` is F14's (its first slice, merged); Parameter is JCS280 (a) above; getDeclaredClasses
is in jrt's tables since F25 (its order JCS272). What stays: t04833's `ModuleDescriptor` (java.lang.module is not in the closed
world: `:reflection`'s) and the accessFlags of an Executable-typed receiver (jrt's Executable_I declares none). Nothing more to
build for (b); the night's records say which of the four pass.

## The decisions of 2026-11-09 that build nothing by day (branch `java-props2`, recorded)

- **JCS213**: the lookup by name as built (java-older's; no start-up hook: every public member of the world is a root).
- **JCS281 (b)**, after JMX-1's night record: the platform logging bean (jdk26u's ManagementFactoryHelper.PlatformLoggingImpl over
  F5's LogManager) and getPlatformMXBean(s) over an MBeanServerConnection; the record says first which of t04792 and t04793 pass.
- **JCS290 (b)**: AbstractQueuedSynchronizer$ConditionObject's serial form stays jrt's until a test asks.
- **JCS291 (a)** if a test asks: an annotation's serial form (none of the corpus known); until then as now.
- **JCS292 (b)**: twins of translated classes per use until a second user.
- **JCS303 (b)**, by the main session with the night's records: `:classfile` re-sorted into JCS300's (the test's own class files,
  now compiled), `:invoke`, the javac tool's, `jrt:/` by design and `:define-bytes` (JCS302 (a), committed today by name).
- **JCS305 (b)**, the morning after the night's build: the stream variants `overlay/jdk/variants/ReferencePipeline.ae` (`toList`)
  and `Collectors.ae` (`toUnmodifiableList`) removed once F26's smoke proves SharedSecrets' JavaUtilCollectionAccess; the commit is
  the variants' deletion and `bin/jrt-convert vet`.
- **JCS312 (b)**: Security's `loadMaster` variant kept (the embedded resource; a build without java.home keeps its properties).
- **JCS321 (a)**, when java/lang/ref's tests are next measured: jrt's PhantomReference beside Weak and Soft, DGCClient's
  derivation dropped.
- **JCS322 (b)**: no thread groups until a second test asks (t07952 alone today).
- **JCS323 (a)**, after the night's build shows JCS240's throws records: ProxyFn's leniency kept for jrt's hand-written interfaces
  alone, the rest wrapped as the JDK wraps (UndeclaredThrowableException).

## java-props2: the night's commands (for the Java night's script, after the other branches' steps)

In the night's worktree with `java-props2` merged (it holds `java-night` at `33d04049c`), heavy ones under `bin/heavy-lock`:

```sh
# 0. light, first: the joint javac check (3,317 files, 0 errors), the derivations (80, 0 not as derived), j2g's unit tests
#    (all pass, test-type-annotations 29 of 29 but 1 known), jrt's package (TestRuntimeVersionParse)
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check && bin/j2g-unit \
  && bin/jrt test --arch amd64 --package jrt -run 'TestRuntimeVersion' -count=1
# 1. the conversion takes Parameter.java (derived), ParameterSupport.java (new), AccessFlagSupport.java (parameterAccessFlags) and
#    the 18 overlays whose headers JCS257 (a) rewrote (their forms' text unchanged but the comment; jdk/provenance.edn's digests)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. JCS256 (b) tonight (JCS256 (a)'s step from the morning after: the block in "JCS256 (a): jrt-vet, tomorrow's switch"):
#    no refusal but the known rows
bin/jrt-convert vet --strict
# 3. the edges as the other slices need them (AnnotatedElement's generics: Parameter's bridges), then the build
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke
grep -A40 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn | grep -E 'reflect/Parameter|ParameterSupport|Runtime' || echo none
# 4. the smoke of the day's support code: expect "9-ea+20-foo Optional[ea] [26, 0, 2, 1] true (java.lang.String arg0 int arg1)"
target/arbace-go/amd64/arbace -e '(println (str (java.lang.Runtime$Version/parse "9-ea+20-foo"))
  (str (.pre (java.lang.Runtime$Version/parse "9-ea"))) (str (.version (Runtime/version)))
  (contains? (Thread/getAllStackTraces) (Thread/currentThread))
  (map str (.getParameters (.getMethod String "indexOf" (into-array Class [String Integer/TYPE])))))'
# 5. javac's tool, then the levels on the day's tests, saved, not recorded: expect t04598 (Version/Basic) to pass at interp
#    and compiled, t04869 and t04870 (Parameter) to pass or move to their next gap, t04684 past getAllStackTraces, and of
#    JCS300 (c)'s 57 tests the 18 class-file tests to read their own class files (their next gaps the API's, F26's)
bin/heavy-lock bin/javac-vendor tool
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels read,interp,compiled --jobs 8 \
  --only '^(t04598|t04684|t04723|t04830|t04832|t04869|t04870|t05256|t04253|t05046|t07651|t08142|t05995|t05996|t06042|t06326) ' \
  --save .tmp/props2-night/check
# 6. the whole-corpus rerun (night 11's, unchanged commands) compiles JCS300 (c)'s 57 by itself (REPR_JAVA_EXE set); then
#    bin/reap --record and bin/crops-score --record with the night's records (the day's classes :rmi, :rmi-connectors,
#    :hotspot-diagnostic, :hotspot-beans, :define-bytes are in the branch's matrix already)
```

**Risks and fallbacks.** (1) Parameter refused tonight (its bridges need AnnotatedElement's generics in the edge, its
annotations AnnotationSupport, F14's): getParameters is then not written (`parameters?` is false) and nothing else moves; revert
is the JCS280 commit. (2) The support methods are compiled only by the build: a Go compile error names `j2g_support.go`; each is
one commit to revert (JCS304/JCS293, JCS320, JCS280). (3) JCS300 (c)'s compile fails for a test: said on stderr, the test runs as
before.

## As built: P1 (by day): PhantomReference, the Cleaner, thread groups, jar verification (branch `java-p1`, write-ahead)

Branch `java-p1` from `mac/java-dyn-fix` (`379130378`: `java-night` with the frozen JVM's fix of j2g's dyn forms), 2026-11-10,
by day under the light rules (no conversion, edge, build or level run; one light process at a time). The user's decisions of
2026-11-10 (about 10:30): JCS370-JCS378 as recommended (`.tmp/plans/JAVA-90-PLAN.md`), JCS374 (a): P1 on night 15, P2 on
night 17, P3 on night 19; JCV10 (b). P1's brief: the jrt and overlay gaps behind `:security`'s and `:serialization`'s remaining
tests, PhantomReference, thread groups.

### What P1's tests need, measured from the records

The records are three conversions stale (the plan's §0), so the tail was taken from what was measured since: `:security`'s 361
tests run on night 8's world (F23's `interp-now.tsv`, 261 pass), less what F23, JCS230 (b), JCS233 (a) and JCS235 (a) built
(the Java night's slices): what F23 left is Kerberos 10 (by design but its permissions, JCS230 (b)), serialization 6 (JCS167's
slices 1-4, built), **jar signing 5 (JCS231 (b): after F9's night, which `java-night` holds; P1's)**, LDAP 3 (JCS234 (a):
deferred), reflection 4 and five single causes. `:serialization`'s slices 1-4, 6 (JCS170) and 7 (JCS169) are built on
`java-night`; slices 5 (RecordComponent) and 8 (Clojure's types, JCS243 (a)) are not (below, JCS383). And java.lang.ref's
and the thread groups' tests by their first missing piece in `test/java/check-interp.edn` (a scan of the non-passing tests'
sources for `PhantomReference`, `Cleaner`, `ThreadGroup`, `getThreadGroup`, `runFinalization`: 23 tests):

| piece | tests (interp's first reason) |
|---|---|
| PhantomReference (F17, JCS321 (a)) | t04816 Basic, t04818 EnqueueNullRef, t04822 PhantomReferentClearing, t04823 ReferenceClone, t04824 ReferenceEnqueue, t04826 ReferenceRefersTo, t03610 TestReferenceRefersTo (hotspot) |
| System.runFinalization | t04682 FinInterrupt, t04683 FinThreads, t05432, t05472 IteratorMicroBenchmark x2, t05474 RemoveMicroBenchmark, t05683 GCRetention, t05842 ManyEntries, t08134 http Finalizer |
| ThreadGroup (JCS322) | t04686 ITLConstructor, t04821 NullQueue (and the thread "Reference Handler"), t05712 ThrowingTasks, t04815 ThreadLists, t07952 DaemonRMIExporterTest |
| java.lang.ref.Cleaner | t03385 Test7190310 (t05123 ExpandingMap's first gap is FileChannel.MapMode) |
| jar verification (JCS231) | t05732 GetMethodsReturnClones, t05736 and t05741 ScanSignedJar, t05737 SignedJarFileGetInputStream, t05739 TurkCert, t05742 TestIndexedJarWithBadSignature, t05269 CertsMatch |

### What came in, and how (jdk26u at `baf63fb`)

| part | how |
|---|---|
| PhantomReference | `go/arbace/jrt/reference.ae`: jrt's `PhantomReference` beside Weak and Soft (a weak pointer whose `get` answers null; `refersTo` the referent itself, now for every kind, not `get()`'s answer, as jdk26u's `refersTo0`), a non-leaf (`PhantomReference_I`; `arbace.j2g.model/class-supers` lists it: jdk26u's `PhantomCleanable` and the tests' own references extend it), its generic signature in `classdata.ae`, the manifest's class list (`test/jrt/manifest.ae`). Go has nothing between a weak and a phantom referent's collection and jrt runs no finalizers, so both are enqueued alike |
| the thread "Reference Handler" | the cleanups of collected referents no longer enqueue on Go's cleanup goroutine: they put the reference on a pending list that a daemon Java thread "Reference Handler" of the system group, at priority 10, enqueues (jdk26u's `Reference.startReferenceHandlerThread`); `RunMain` starts it before main (NullQueue looks for it), a program without `RunMain` with its first collected referent |
| System.runFinalization | `system.ae`: a collection and a yield; jrt runs no finalizers (a JVM with `--finalization=disabled`, JEP 421), the interrupt status untouched (FinInterrupt). `finalize()` itself: JCS379 |
| thread groups (JCS322 (a), the user's P1 overriding its (b)) | `go/arbace/jrt/threadgroup.ae`, jdk26u's `ThreadGroup.java` transcribed (JDK 19's design: a group tracks no threads; `activeCount`, `enumerate`, `interrupt` walk the live platform threads of management.ae's registry; the system and main groups' subgroups strong, later ones weak; `list()` left out: no test calls it); the groups "system", "main" in it, "VirtualThreads" in it; `Thread`: its group (the creator's, or the constructor's; an adopted goroutine's the main group), `getThreadGroup` (null once terminated), the five constructors taking a group (`inheritInheritableThreadLocals` false: no inheritable values, ITLConstructor), `Thread.activeCount`, `Thread.enumerate`, `setPriority` and construction within the group's maximum, `toString`'s group. An uncaught exception goes as before (the thread's handler, else the default handler, else the message): a group's `uncaughtException` does the same, and no test extends ThreadGroup |
| the Cleaner (F17) | `test/jrt/jrt_sources.ae`'s new `p1-sources`: jdk26u's `java.lang.ref.Cleaner`, `jdk.internal.ref.CleanerImpl`, `PhantomCleanable`, `CleanerFactory` unchanged (the cleaner's thread jrt's own InnocuousThread). The variants that cut a Cleaner (DESKey, DESedeKey, PBEKey, PBKDF2KeyImpl, PasswordCallback, Timer, Executors, LogManager, DatagramChannelImpl; ZipFile's and Inflater's FileCleanable, NativeBuffer) stay as they are: JCS380 |
| DGCClient | its derivation (weak references for jdk26u's phantom ones) dropped from `test/jrt/derive.ae` and its overlay removed: jdk26u's file, listed in `rmi-sources` (JCS321 (a)) |
| jar verification (JCS231) | jdk26u's `JarVerifier`, `ManifestEntryVerifier`, `SignatureFileVerifier`, `ManifestDigester` in `p1-sources`, jrt's verifying-nothing `JarVerifier.java` and `ManifestEntryVerifier.java` overlays removed; `JarFile.java`'s overlay with jdk26u's verifier code restored (`jv`, `maybeInstantiateVerifier`, `initializeVerifier`, the verifier stream, `verifiableEntry`, the entries' certificates and signers, `ensureInitialization`), its other cuts kept (ZipAccess for SharedSecrets, the versions' feature numbers), jdk26u's `ScopedValue IN_VERIFIER_INIT` a `ThreadLocal` (no ScopedValue in the Go build); `overlay/jdk/variants/JarEntry.ae` removed (the copy constructor copies the certificates and signers again: java.security is in the world since F1-F4) |

LICENSE.md: `threadgroup.ae`'s origin, the jar overlays' lines. Not changed: `RuntimeUtil`'s and JMX's three thread makers'
derivations without groups (JCS381).

### Light checks done

Main's stage 0 as `ARBACE_HOST_EXE`, one process at a time, each under 2 GiB but the predicted world's (below, 4.98 GB):

- **jrt's package tests**, `bin/jrt test --arch amd64 --package jrt`: the new `TestPhantomReferences` (get null, refersTo the
  referent, a collected referent's reference enqueued by "Reference Handler", a live system-group daemon; enqueue and clear by
  hand; a null referent's reference enqueues; runFinalization keeps the interrupt status) and `TestThreadGroups` (system > main >
  the user's, a group's threads, activeCount and enumerate with and without recursion, the groups' enumeration, a terminated
  thread's group null, the priority capped by the group's maximum at construction and by setPriority, Thread.toString's group,
  inheritInheritableThreadLocals false) pass, and the whole package passes with the regenerated manifest test (9.9 s; the
  first run caught the test's own unbuffered channel, `(make (chan any) 2)` is repr/go's spelling);
- **the manifest and member tables**: `jrt.tables` and `jrt.manifest` on the frozen jar (`frozen_java tools -Xmx1800m`, as
  `bin/jrt manifest` runs them; `JAVA_TOOL_OPTIONS` breaks `java26.bash`'s version check): Thread 52 of 121 JDK members (+8:
  the five group constructors, getThreadGroup, activeCount, enumerate), System's runFinalization, ThreadGroup 21 of 32 (`list`
  and members jrt does not define left out), PhantomReference 2 of 6 (its own: the
  constructor and get; the rest Reference's), "edge members of these classes jrt does not define: 0";
- **the joint javac check** (`.tmp/night7/all9-run.sh`, the JDK's javac over the closure as java.base's patch): 3,323 files
  (java-night's 3,317, +8 `p1-sources`, +1 DGCClient jdk26u's, -1 its overlay, -2 the stub verifiers), **0 errors**, 6,958
  classes (45 s, 1.0 GB);
- **the derivations**, `bin/jrt-convert derive --check`: 79 overlays (80 less DGCClient's), 0 not as derived.
- **the night's world predicted**, `bin/jrt-convert predict` (JCS283 (b); javac's tool `javac-5ad357331dbcd7cfb0fd`,
  `REPR_JAVA_STALE_OK=1`): 1,071 files changed against the committed conversion (b6a6093af: all of `java-night`'s unconverted
  slices with P1's), 1,769 read with their context in 41 minutes, 1,767 converted, every one natively but 2; the 2 failures and
  46 javac errors are the predicted read's known context gaps (`LocaleProviderAdapter`, `LocaleData`: the generated
  `LocaleDataIndex`; `PlatformLogger`: java.logging's packages, as F5's check found), none in P1's files. The vet (`jrt.vet`
  over the world, the changed classes): 46 refused, 43 of them the night's own gaps (sun.nio.fs's generated `UnixConstants`,
  generics the edge does not declare yet, `MethodHandleInfo`, java.time.chrono's dates); P1's three: `PhantomCleanable` and
  DGCClient's `PhantomLiveRef` ("header: Unknown type: PhantomReference") and `Cleaner` (its `register` returning a
  `PhantomCleanableRef`, a consequence): **`jdk/edge.edn` has no `java/lang/ref/PhantomReference`** (ThreadGroup's entry is
  there), which the night's `bin/j2g-edge update` (step 2) records, the edge being the JVM's answers for every class the
  builds name. Should step 3's report still name them, the fallback is risk (3) below. j2g's analysis of the whole predicted world
  (`bin/j2g --analyze-only`, J2G_MISSING the changed classes): 45 analysis failures, P1's four the same edge gap (Cleaner,
  PhantomCleanable, DGCClient and its PhantomLiveRef); **the jar verifier's classes (JarVerifier, ManifestEntryVerifier,
  SignatureFileVerifier, ManifestDigester), JarFile and JarEntry analyze without a failure or a stub**, and so do the classes
  naming ThreadGroup and Thread's new members. The 52 stubs are the other slices' (java.lang.invoke's, System.Logger, and
  `RecordComponent` in JMX's mapping factory: serialization's slice 5, JCS383). The whole predict ran 73 minutes, **peak 4.98 GB**
  (the analysis of the whole world, beyond the day's 2 GiB rule: a slip of this branch's day, see the journal); its outputs in the
  worktree's `.tmp/jrt/predict/`.

### Expected yield (interp / compiled; the triage's 0.7 where a second cause is unknown)

- PhantomReference: EnqueueNullRef, PhantomReferentClearing, ReferenceClone, ReferenceEnqueue, ReferenceRefersTo,
  TestReferenceRefersTo: **5-6**; Basic waits for finalization (JCS379).
- runFinalization: FinInterrupt; GCRetention, ManyEntries and the three micro benchmarks (their next gaps unknown): **3-5**;
  FinThreads and the http Finalizer need `finalize()` (JCS379).
- thread groups: ITLConstructor, NullQueue, ThrowingTasks, DaemonRMIExporterTest (with F27's RMI), ThreadLists (with JMX-1's
  ManagementFactory): **3-5**.
- the Cleaner: Test7190310: **0-1**.
- jar verification: the 7 above: **4-6**.

About **+15-23 at interp and +12-20 at compiled** (the compiled level's are the same tests where j2g translates them; the
DynSubs of PhantomReference are new, JCS111 (a)'s mechanism). With the plan's S4 estimate (+60-90) the rest of P1's share is
`:security`'s single causes and serialization's slices 5 and 8, not built here (JCS383).

### The night's commands (night 15, after JCS359's measure; in the night's worktree with `java-p1` merged, heavy ones under `bin/heavy-lock`)

```sh
# 0. light: the joint javac check (the closure + 8 files: Cleaner 4, the jar verifier 4; 0 errors), the derivations (DGCClient's
#    gone: one fewer), jrt's package tests (TestPhantomReferences, TestThreadGroups and their neighbours)
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check \
  && bin/jrt test --arch amd64 --package jrt -run 'TestPhantom|TestThreadGroups|TestWeakReferences|TestThread|TestRunMain|TestSystem' -count=1
# 1. jrt's manifest and member tables (the frozen JVM: PhantomReference, ThreadGroup and Thread's new members appear;
#    manifest_test.ae refers to each), then the conversion: + Cleaner, CleanerImpl, PhantomCleanable, CleanerFactory,
#    JarVerifier, ManifestEntryVerifier, SignatureFileVerifier, ManifestDigester, DGCClient (share:java.rmi), JarFile's
#    overlay; - jrt's JarVerifier and ManifestEntryVerifier overlays
JAVA_TOOL_OPTIONS=-Xmx2g bin/jrt manifest
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. the package table, j2g's edge (PhantomReference and ThreadGroup are jrt's now; the Cleaner's and the verifier's classes),
#    javac's world's edge
bin/jrt modules && bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the build; j2g's report: no :analysis-failures for Cleaner, CleanerImpl, PhantomCleanable, JarVerifier,
#    SignatureFileVerifier, JarFile; a DynSub_PhantomReference; no "a variant for a class not in the inputs"
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
# 4. the smoke (below; .tmp/p1-smoke.clj): expect "phantom null true", "enqueued true", "cleaner ran",
#    "groups main system true", "itl 1", "jar-signed true" (TurkCert's signed test.jar, an entry read whole then its signers)
target/arbace-go/amd64/arbace .tmp/p1-smoke.clj
# 5. Java's levels interp and compiled on P1's areas: saved, not recorded; expect the yields above and no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 2 \
  --only 'jdk/java/lang/(ref|Thread|ThreadGroup|System/finalization|management/ThreadMXBean)/|jdk/java/util/(jar|zip)/|jdk/java/security/CodeSource|jdk/java/util/concurrent/(ConcurrentQueues|ThreadPoolExecutor)/|jdk/java/util/(ArrayList|Collection)/|jdk/sun/net/www/protocol/http/Finalizer|jdk/javax/management/remote/mandatory/connection/DaemonRMIExporterTest|jdk/java/rmi/|hotspot/jtreg/compiler/(intrinsics/TestReferenceRefersTo|c2/Test7190310)' \
  --save .tmp/p1-night/check
bin/java-check --judge .tmp/p1-night/check/*.edn
```

The smoke (`.tmp/p1-smoke.clj`):

```clojure
(import '(java.lang.ref PhantomReference ReferenceQueue Cleaner) '(java.util.jar JarFile))
(defmacro item [n & body] `(println ~n (try (eval '(do ~@body)) (catch Throwable t# (str "FAILED " t#)))))
(item "phantom" (let [o (Object.) p (PhantomReference. o (ReferenceQueue.))] (str (.get p) " " (.refersTo p o))))
(item "enqueued" (let [q (ReferenceQueue.) p (PhantomReference. (Object.) q)]
  (loop [i 0] (System/gc) (if-let [r (.remove q 50)] (identical? r p) (if (< i 100) (recur (inc i)) false)))))
(item "cleaner" (let [done (promise)] (.register (Cleaner/create) (Object.) #(deliver done "ran"))
  (loop [i 0] (System/gc) (or (deref done 50 nil) (if (< i 100) (recur (inc i)) "not run")))))
(item "groups" (let [g (.getThreadGroup (Thread/currentThread))] (str (.getName g) " " (.getName (.getParent g)) " "
  (some #(= "Reference Handler" (.getName %)) (let [a (make-array Thread 64)] (take (.enumerate (.getParent g) a) a))))))
(item "itl" (let [l (InheritableThreadLocal.) r (atom nil)] (.set l 1)
  (doto (Thread. nil #(reset! r [(.get l)]) "t" 0 true) .start .join) (first @r)))
(item "jar-signed" (with-open [j (JarFile. "/root/jdk26u/test/jdk/java/util/jar/JarFile/test.jar" true)]
  (some (fn [e] (with-open [in (.getInputStream j e)] (while (pos? (.read in (byte-array 4096)))))
              (when (and (not (.isDirectory e)) (.getCodeSigners e)) true))
        (enumeration-seq (.entries j)))))
```

**Risks and fallbacks**: (1) the thread "Reference Handler" is a live thread: a test that counts the live threads exactly
(ThreadMXBean's counts) sees one more, as on the JDK (which has it and more); the night's `--only` covers
`java/lang/management/ThreadMXBean`; fallback: started lazily (with the first collected referent) instead of by `RunMain`, one
line. (2) the jar verifier's classes refused by j2g (a member the edge lacks): the report names it; fallback the JarFile
overlay and the two stub overlays restored (`git revert` of the jar commit), the Cleaner and the groups kept. (3) a
DynSub of PhantomReference (`PhantomCleanable`'s) not built: JC4c's mechanism, as Weak and Soft; fallback the Cleaner's four
files out of `p1-sources`. (4) Thread's interface gains `GetThreadGroup__ThreadGroup`: every translated subclass of Thread
(ForkJoinWorkerThread, InnocuousThread, the tests') gets it from the embedded `Thread` (final, promoted).

## Proposed (P1): JCS379-JCS383

Numbered after JCS378 (`.tmp/plans/JAVA-90-PLAN.md`).

### JCS379 Finalization (`Object.finalize`)

Basic, FinThreads and the http Finalizer test (3) need an object's `finalize()` run after it becomes unreachable, on a thread
"Finalizer". (a) By design: Arbace runs as a JVM with `--finalization=disabled` (JEP 421; finalization is deprecated for
removal since JDK 18): a `:finalization` class, `:kind :design`; (b) Go's `runtime.SetFinalizer` on every object of a class
that overrides `finalize` (j2g's allocation of such a class and the interpreter's `new` of one), the finalizer queueing it for
a "Finalizer" thread, `runFinalization` waiting for that queue: 1.5 days, a cost on every such allocation, and resurrection
(which Go's finalizers allow once) to match. *Recommended: (a)*: the JDK itself is moving to (a), and 3 tests.

### JCS380 The Cleaner's cut variants restored

With the Cleaner in the world (P1), the variants that left out a Cleaner (DESKey, DESedeKey, PBEKey, PBKDF2KeyImpl,
PasswordCallback: keys zeroed only when destroyed; Timer, Executors' finalizable delegate, LogManager, DatagramChannelImpl,
ZipFile's and Inflater's FileCleanable, NativeBuffer) can be jdk26u's again. (a) In P2, one variant a commit, each with its
area's tests (`bin/jrt-convert vet --classes`); (b) only where a test needs it; (c) never. *Recommended: (a)*: fewer
variants, the JDK's behaviour (a key's bytes zeroed when it is unreachable), no test known to need it now.

### JCS381 RuntimeUtil's and JMX's thread makers with groups again

RMI's `RuntimeUtil` and JMX's three thread makers are derived without thread groups (F27, JMX-1). (a) Their derivations
dropped in P2 (jdk26u's files), DaemonRMIExporterTest's threads then in the "RMI Runtime" group as on the JDK; (b) kept.
*Recommended: (a)*, after night 15 shows the groups.

### JCS382 The thread "Reference Handler" from the start

P1 starts it in `RunMain` (NullQueue looks for it before any reference is made). (a) As built; (b) lazily, with the first
collected referent (no thread for a program that makes no reference with a queue; NullQueue fails). *Recommended: (a)*: the
JVM has it from the start, and a daemon thread blocked on a channel costs nothing.

### JCS383 Serialization's slices 5 and 8 as P2's

`Class.getRecordComponents` and `RecordComponent` (slice 5, records' tests and reflection's) and Clojure's types (slice 8,
JCS243 (a), the suite's `serialization` 9) are not built by P1, which took the security tail and the references. (a) Both in
P2 (night 17), with JCS241 (invokeDefault) and JCS242 if a test reads lambdas from a file; (b) slice 8 alone in P2, slice 5
with F25's reflection records. *Recommended: (a)*: about 1.5 days, and both are measured by the same areas' run.

## As built: P2 (by day): records' reflection, Clojure's types' serialization, the platform logging bean (branch `java-p2`, write-ahead)

Branch `java-p2` from `mac/java-night` (`ffec18089`, which carries `java-p1`), 2026-11-11, by day under the light rules (no
conversion, edge, build or level run). The user's decisions of 2026-11-10: JCS374 (a), P2 on night 17; JCS383 (proposed by P1:
serialization's slices 5 and 8 as P2's) taken as P2's brief with the plan's S5 list (`.tmp/plans/JAVA-90-PLAN.md`). What the
day built: **slice 5** (RecordComponent), **slice 8** (Clojure's own types: the classes made at run time), and of the plan's
S5 list **JCS281 (b)** (decided 2026-11-09: the platform logging bean and getPlatformMXBean(s) over a connection). Not built:
the rest of S5 (below, "What P2 leaves").

**No executable has any of it, and none of the moved tests can run by day**: night 11's Java night stopped at its conversion
(`.tmp/night11/java-retry.log`: p02 `bin/javac-vendor convert`, JCS170's MethodTooLargeException, fixed by `java-dyn-fix`), so
`java-night`'s serialization slices (JCS167's 1-4, 6, 7), JMX-1 and P1 are sources only: `jdk/forms` has no
`java/io/ObjectOutputStream` and no gate executable (the newest, `.tmp/gate-exe/15290d9735ea602053e8`, 11-10) holds
serialization, JMX or RecordComponent. `bin/java-check --only` on the moved tests at the interp level would only measure the
old world; it was not run for them. The light checks are those below.

### What the tests need, measured

- **Slice 5** in the base corpus: no non-passing test names `RecordComponent` or `getRecordComponents` (a scan of the 1,459
  non-passing interp tests' sources); hotspot's `recordReflectionTest` (t07726) is a JDK failure in `ref-jdk26.edn` (its
  `getName() == name` identity checks fail on the renamed package's JDK run too). The slice's tests are the grown corpora's:
  `jdk/java/io/Serializable/records/` (16 JUnit tests, 3 more under `migration/`, `java-io-framework`, JCS337's night 7) and
  `jdk/java/lang/reflect/records/` (3 TestNG tests, `java-lang-framework`, night 3); `javax/management/mxbean/RecordsMXBeanTest`
  (TestNG) is in no corpus; JMX-1's `DefaultMXBeanMappingFactory` names `getRecordComponents` (the predicted world's stub, P1's
  vet: now resolved).
- **Slice 8**: Clojure's suite's `clojure.test-clojure.serialization` (9 tests, skipped in `test/arbace-go-results.edn` and
  `test/arbace-go-aot-results.edn`, the Clojure classes' `:serialization`, 10 at eval and 10 at AOT with java-interop's
  `test-proxy-non-serializable`). Its types: `arbace.lang`'s Serializable classes are translated Java forms, so JCS167's j2g rules
  already give them their serial fields, hooks (`Keyword`, `Symbol`, `Namespace`, `RT$DefaultComparator` `readResolve`, `Var`'s
  `writeReplace` and `Var$Serialized`, `Iterate`'s `readObject`) and `SerialNew`; what JCS167 left out is the classes made at
  run time (`Compiler$Dyn.defineClass`: defrecord, deftype, an Object proxy), which had no `SerialNew` ("no valid
  constructor"), and the evaluated fns (`defineFnClass`, subclasses of `Compiler$EvalFn`).
- **JCS281 (b)**: t04792 `LoggingMXBeanTest` (newPlatformMXBeanProxy of `java.util.logging:type=Logging` on the platform
  server, `getPlatformMXBean(mbs, PlatformLoggingMXBean.class)`) and t04793 `PlatformLoggingMXBeanTest` (the bean registered in
  a server of its own and found in the platform server); both were in `:design-also` and `:design-also-compiled` (JCS129's JMX,
  in the world since JMX-1).

### What came in, and how (jdk26u at `baf63fb`)

| part | how |
|---|---|
| RecordComponent (slice 5) | `go/arbace/jrt/record.ae`: jdk26u's `RecordComponent` transcribed (name interned, `getType`, `getGenericSignature` interned, `getGenericType` through the component's field's FieldRepository or the accessor's generic return type, `getAccessor`, `getDeclaringRecord`, `toString`, `AnnotatedElement`'s seven), and `Class.getRecordComponents` (null for a class that is not a record; a new array of new objects); `getAnnotatedType` left out (jrt's Field has none either: JCS385) |
| the records' data | what HotSpot's `JVM_GetRecordComponents` reads from the Record attribute, as data lines beside the annotations (`ClassInfo.Annots`, classdata.ae's header): `R NAME DESC [SIG]` a component in order, written by `arbace.classes.annot/attribute-lines` for **every** record (j2g's world and the interpreter's: the JVM keeps the attribute for every record), and `A NAME ANN` its annotations with the members' data (those applicable to RECORD_COMPONENT, `analyze/for-target`, as javac keeps them); decoded by classdata.ae's `attrsOf` (R) and annotation.ae's `annotations` (A). A record whose data has no R line (a class translated before them) answers its instance fields in declaration order, as the ObjectStreamClass variant took them |
| records' derived accessors | `annot.ae`'s `reflected-member?`: a record's derived accessor (`:derived :record-accessor`, no `:kind`) now writes its Signature (`G M`) and its METHOD annotations (`M`) as javac does; before, only declared methods did (an accessor's `getGenericReturnType` answered the erased type) |
| the manifest, tables | `test/jrt/manifest.ae`: `java.lang.reflect.RecordComponent` (AnnotatedElement); `bin/jrt manifest` on the frozen JVM (`frozen_java tools -Xmx1800m`, as P1): Class's `getRecordComponents` and RecordComponent's 10 members in `reflect_tables.ae`, `manifest.edn`, `manifest_test.ae` |
| Dyn's SerialNew (slice 8) | `arbace/j2g/dyn.ae`: `Compiler$Dyn.defineClass` gives every class made at run time a `SerialNew`, a Dyn of the class with its slots null (ReflectionFactory's rule for a class whose superclass is Object: Object's constructor; the stream sets every field: a Dyn's fields, hidden ones too, are all `getDeclaredFields`'), in a fn of its own (`dyn-serial-new-form`: native-forms's quoted constants stay within the frozen JVM's 64 KB static initializer, JCS170's MethodTooLargeException); `defineJavaProxy` still sets its own |
| the platform logging bean (JCS281 (b)) | `overlay/jdk/java.base/sun/management/ManagementFactoryHelper.java`: jdk26u's `PlatformLoggingImpl` and `getPlatformLoggingMXBean`, its `LoggingMXBeanAccess` reflection replaced by direct calls on `LogManager.getLoggingMXBean()` (java.logging is in the world as java.base's; jdk26u reflects only because java.management may not read java.logging); `ManagementFactory.java`: the bean among the singletons, registered in `getPlatformMBeanServer`, in `getPlatformManagementInterfaces`, and jdk26u's `getPlatformMXBean(MBeanServerConnection, Class)` and `getPlatformMXBeans(MBeanServerConnection, Class)` over the singleton's object name |
| the classes | `test/java/divergences.edn`: `:reflection`'s pattern without `RecordComponent` (in the world); t04792, t04793 out of `:design-also` and `:design-also-compiled` into `:mxbeans`'s `:tests` (deferred until the night; compiled, `:j2g-unresolved` takes them first by its pattern). `bin/reap --record`: Java eval by design 176 -> 174, deferred 1,283 -> 1,285; AOT 200 -> 198, 1,186 -> 1,188; `bin/crops-score --record`: completeness 77.2 unchanged (strict), 79.2 without by design |

LICENSE.md: `record.ae`'s origin, `ManagementFactoryHelper.java`'s PlatformLoggingImpl.

### Light checks done

Main's stage 0 as `ARBACE_HOST_EXE`, javac's tool `javac-5ad357331dbcd7cfb0fd` with `REPR_JAVA_STALE_OK=1` (its key is not
the checkout's), one process at a time, each under 2 GiB:

- **jrt's package**, `bin/jrt test --arch amd64 --package jrt`: the new `TestRecordComponents` (a record class's R and A
  lines: three components, the array's class, names interned, types, `TT;` interned, `toString`, the accessor, the component's
  annotation and none on the others; a class that is not a record null; a record without R lines its instance fields) and the
  whole package pass (5.0 s of tests, 23 s with the build);
- **the encoder**: a sample record (`.tmp/p2/rec/src/p/Point.java`: `record Point<T>(int x, @RCA String name, @FO T val)`, RCA
  targeting RECORD_COMPONENT and FIELD, FO FIELD only) read by `bin/repr-java read` and its classes' data printed on stage 0
  (`.tmp/p2/rec/tool/p2/recdata.ae`, j2g.typeann's way): `R x I`, `R name Ljava.lang.String;`, `R val Ljava.lang.Object; TT;`,
  `A name @Lp.Point$RCA; 0` and no A line for FO, `G M val ()Ljava.lang.Object; ()TT;` (new), the R lines also without
  members;
- **j2g**: `bin/j2g-unit` on stage 0, 55 of 55 pass (the type annotations' encoder against javac's among them); `arbace.j2g.dyn`,
  `out` and `arbace.classes.annot` load on stage 0 and `arbace.j2g.dyn/native-forms` on the frozen JVM (`frozen_java tools`);
- **the joint javac check** (`.tmp/night7/all9-run.sh`): 3,348 files, **0 errors**, 7,000 classes (18 s, 1.0 GB);
  `bin/jrt-convert derive --check`: 83 overlays, 0 not as derived;
- `bin/reap` 0 problems after `--record`; `bin/crops-score --judge --strict` exit 0; `bin/java-check --essential` on the
  11-10 gate executable (the old world: a check of the records and forms, not of P2).

Not checked by day: the two overlays' read (`bin/repr-java read-jdk --check` needs JMX-1's context, `REPR_JAVA_PATCH_ALL=1`
with the slice: the night's conversion reads them), j2g's analysis of the new world (`bin/jrt-convert predict`: 73 minutes and
4.98 GB for P1, over the day's rule), the Go of `j2g_dyn.go` (the night's build).

### Expected yield (interp / compiled)

- **The base corpus (Java's cells)**: t04792, t04793: **1-2 / 1-2** (both need JMX-1's server and introspection: the triage's
  0.7). Slice 5 moves no base-corpus test. Java's completeness about **+0.02-0.03** (one test 0.0075 in eval and AOT each,
  a quarter of the mean); the JCS129 split of the two (design -> deferred) moves nothing strict.
- **The grown corpora**: of `java/io/Serializable/records`' 19 and `java/lang/reflect/records`' 3, the ones that need neither
  `getAnnotatedType` (RecordReflectionTest's type annotation case) nor the record serialization the variant already has: about
  **10-15** once nights 3 and 7 record their corpora (JUnit and TestNG, JCS331's runner).
- **Clojure's row**: `serialization` unskipped (the night's command below): at eval **7-8 of 9** (function-serialization needs
  an evaluated fn serialized: JCS384; CLJ-2839 needs `Repeat`/`Iterate`/`Cycle`'s hooks, translated), at AOT **8-9** (a compiled
  fn is a translated class with its closed-over fields: JCS167's rules); `test-proxy-non-serializable` stays (arbace.inspector's
  Swing). Clojure's eval and AOT each about +1 point of the row (9 of about 800 tests).

### The night's commands (night 17, P2's Java night, JCS374 (a); in the night's worktree with `java-p2` merged after P1's night, heavy ones under `bin/heavy-lock`)

```sh
# 0. light (1 min, 1 GB): the joint javac check, the derivations, jrt's package tests
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check \
  && bin/jrt test --arch amd64 --package jrt -run 'TestRecord|TestClassData|TestAnnotation|TestReflect|TestManifest' -count=1
# 1. jrt's manifest and tables are committed (check them: no diff), then the conversion (the two overlays changed: heavy,
#    under the lock, ~40-75 min as P1's predict, GOMEMLIMIT as the Java night's)
JAVA_TOOL_OPTIONS= bin/jrt manifest && git diff --exit-code go/arbace/jrt/
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update
# 2. j2g's edge (Class.getRecordComponents and RecordComponent are jrt's now; ManagementFactory's new members), javac's edge
bin/jrt modules && bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the build (two Go-hosted builds, ~8 min; j2g's whole program under the lock, ~20 GiB); j2g's report: no
#    :analysis-failures for DefaultMXBeanMappingFactory, ManagementFactory, ManagementFactoryHelper$PlatformLoggingImpl;
#    j2g_dyn.go compiles (dyn-serial-new-form); then the test build for the suite
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test && bin/heavy-lock bin/arbace-go --build-tests
# 4. the smoke (below, .tmp/p2-smoke.clj): expect "record true", "colls true", "var true", "logging
#    java.util.logging:type=Logging true", "logging-server true"
target/arbace-go/amd64/arbace .tmp/p2-smoke.clj
# 5. Java's levels interp and compiled on P2's areas (interp --jobs 2 under 8 GiB, ~10 min; compiled one shard under the
#    lock): saved, not recorded; expect t04792, t04793 and no passing test lost
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 2 \
  --only 'jdk/java/lang/management/|jdk/java/util/logging/|jdk/javax/management/|jdk/java/io/Serializable/|jdk/java/lang/(Class|reflect|Record)' \
  --save .tmp/p2-night/check
bin/java-check --judge .tmp/p2-night/check/*.edn
# 6. Clojure's serialization namespace on the test build (1 GB, ~1 min): its skip removed from the reference first
sed -i 's/^  clojure.test-clojure.serialization "Java serialization (ObjectOutputStream, cut by D6)"}$/  }/' test/arbace-go-results.edn
CLOJURE_TESTS_GO=$PWD/target/arbace-go/amd64/arbace-tests bin/clojure-tests -j 2 clojure.test-clojure.serialization
# 7. the morning after: its counts into test/arbace-go-results.edn's :namespaces (and a test the eval level fails, such as
#    function-serialization, in :skipped-tests or a narrower Clojure class than :serialization, JCS387), the AOT level in the
#    next --full (bin/clojure-tests --aot: j2g's whole program, ~20 GiB, its own heavy step), bin/reap --record, crops-score
```

The smoke (`.tmp/p2-smoke.clj`):

```clojure
(import '(java.io ByteArrayOutputStream ObjectOutputStream ObjectInputStream ByteArrayInputStream)
        '(java.lang.management ManagementFactory PlatformLoggingMXBean) '(javax.management ObjectName))
(defmacro item [n & body] `(println ~n (try (eval '(do ~@body)) (catch Throwable t# (str "FAILED " t#)))))
(defn rt [x] (let [b (ByteArrayOutputStream.) o (ObjectOutputStream. b)] (.writeObject o x) (.close o)
               (.readObject (ObjectInputStream. (ByteArrayInputStream. (.toByteArray b))))))
(defrecord SR [a b c])
(item "record" (let [r (SR. 0 :foo (range 5)) s (rt r)] (and (= r s) (= (hash r) (hash s)) (instance? SR s))))
(item "colls" (let [v [[1 2] {:a 1} #{'a} :kw 'sym 25/3 (sorted-set 3 1) (seq "s11n")]] (= v (rt v))))
(item "var" (identical? #'arbace.core/conj (rt #'arbace.core/conj)))
(item "logging" (let [b (ManagementFactory/getPlatformMXBean PlatformLoggingMXBean)]
                  (str (.getObjectName b) " " (boolean (some #{""} (.getLoggerNames b))))))
(item "logging-server" (.isRegistered (ManagementFactory/getPlatformMBeanServer) (ObjectName. "java.util.logging:type=Logging")))
```

**Risks and fallbacks**: (1) the R lines change the data of every record of the world (a few JDK records, the tests'):
harmless to a reader that ignores them, but `bin/j2g-unit`'s type annotation sample and the gate's generated checks see the
new lines (checked by day: 55 of 55); fallback: R lines only with `:members`. (2) `j2g_dyn.go`'s SerialNew does not compile:
revert `arbace/j2g/dyn.ae`'s hunk (slice 8 then waits; nothing else depends on it). (3) the logging bean's registration fails
the platform server's creation (`StandardMBean` of PlatformLoggingImpl): every `getPlatformMBeanServer` caller would fail;
fallback: the bean out of the server's array (one line), t04793's second half then fails alone. (4) ManagementFactory's new
overloads change j2g's overload choice in callers (none in the world calls `getPlatformMXBean` with two arguments).

### What P2 leaves (the plan's S5 list)

- **JCS270 (a), JCS272 (a)** (decided 2026-11-08: the JDK members' Signatures unrecorded and getDeclaredClasses' name order
  until a test asks): nothing to build until S1's record names a test.
- **The Proxy tail**: after the night's records show which proxy tests remain (JCS170's).
- **JCS220** (the asynchronous channels, proposed, recommended (a) after F10a/F10b's records): not decided; 2-3 days.
- **JCS160 (a)**: built (SOCKS, F12's); nothing left.
- **F18** (jrt's dates, 16 / 14 tests, 4 days): not started.
- **JCS377 (a)**: a javac-world shard measured in P1's night (night 15); P2 adopts it if it builds.
- **JCS380, JCS381** (proposed by P1 for P2): not decided.

## Proposed (P2): JCS384-JCS388

Numbered after the newest number on `mac/java-night` and `mac/jcs359-s2`: both used **JCS379-JCS382** for different proposals
(P1's on `java-night`, JCS359 stages 2-3's on `jcs359-s2`); one set needs renumbering at their merge (JCS388).

### JCS384 An evaluated fn's serialization

Clojure's `function-serialization` serializes fns made by the evaluator: their classes are made at run time
(`Dyn.defineFnClass`, subclasses of `Compiler$EvalFn`) and their state is the evaluator's code tree and captured frame, which
no other process could read. (a) By design at the eval level (a `:fn-serialization` Clojure class, eval only): the AOT level's
fns are translated classes and serialize as on the JVM; (b) an evaluated fn written as its source form, its namespace and its
closed-over values, and read back by evaluating the form again (`writeReplace` on EvalFn, a `readResolve` holder): 1.5 days,
and a fn whose form is gone (made by `eval` of a generated form) still cannot. *Recommended: (a)*: the JVM's own answer
needs the class file the evaluator does not make (J2G-SPEC §10.4); one test.

### JCS385 RecordComponent.getAnnotatedType and Field.getAnnotatedType

jrt's Field has no `getAnnotatedType`; RecordComponent leaves it out likewise. JCS271 (b)'s Y records hold the fields' type
annotations already. (a) Both, over the Y F records and a new `Y R NAME HEX` (the Record attribute's
RuntimeVisibleTypeAnnotations), through jdk26u's TypeAnnotationParser as Method's are: half a day; (b) when a test of the
corpus needs it (RecordReflectionTest's type annotation case, java-lang-framework's, night 3). *Recommended: (b)*.

### JCS386 ObjectStreamClass's record support from RecordComponent

The variant (`test/jrt/serialization/variants.py`) takes a record's components as its instance fields
(`recordComponentFields`) because RecordComponent was not in the world. (a) jdk26u's own `canonicalRecordCtr` and
`RecordSupport` over `getRecordComponents`, with method handles once F15's java.lang.invoke is in the night's world: the
variant shrinks; (b) as now (the fields are the components, JLS 8.10.3; the same answers). *Recommended: (a)* after F15's
night, one variant fewer.

### JCS387 Clojure's serialization namespace recorded

After P2's night: (a) the namespace's counts into `test/arbace-go-results.edn` (unskipped) and the AOT reference, the tests
that fail into narrower Clojure classes (JCS384's at eval), `:serialization` left with `test-proxy-non-serializable`'s
Swing reason only; (b) keep it skipped until the AOT level passes too. *Recommended: (a)*.

### JCS388 JCS379-JCS382 used twice

`java-p1` (P1's finalization, the Cleaner's variants, thread makers, Reference Handler) and `jcs359-s2` (JCS359 stages 2-3's)
both numbered from JCS379. (a) jcs359-s2's renumbered at its merge (it is the later and not yet merged into `java-night`'s
line); (b) P1's renumbered. *Recommended: (a)*, the main session's at the merge.

## As built: F15 (java.lang.invoke), write-ahead

Branch `java-invoke` from `mac/java-night` (`9e6160bbb`), 2026-11-09, by day under the light rules (no conversion, j2g run,
build or compiled level; each process about 2 GiB, two at once). The brief: the plan's F15, java.lang.invoke, with the
`:invoke` class's tests and those the other slices left on it (F26's ByteCodeTest, JCS222's private lookups, JCS170's Proxy).
Nothing is converted, translated or built here; the night's commands are below.

### The tests measured

`test/java/ref-jdk26.edn`'s source sets were scanned for java.lang.invoke's API (`.tmp/inv/scripts/scan.py`): **43 tests**
the JDK passes use it, the `:invoke` class's 41 among them. At the committed records all 43 are `:load-failed` at interp
(`ClassNotFoundException: java.lang.invoke.MethodHandle`: the class is not in the world), and at compiled 2 pass (their
handles unused at run time), 22 fail ("class java.lang.invoke.MethodHandle is not in the closed world") and 19 are refused.
By the missing piece:

| tests | the missing piece | ids |
|---:|---|---|
| 28 | **direct handles and their calls**: `Lookup.find*`, `unreflect*`, `revealDirect`, `MethodType`, `invokeExact`/`invoke`/`invokeWithArguments`, `bindTo`, `asType`, the combinators (`catchException`, `tryFinally`, `spreadInvoker`), `MutableCallSite`, `WrongMethodTypeException` | t03287 t03306 t03574 t03593 t03630-t03635 t03862 t03895 t03899 t04612 t04675 t04745-t04748 t04751 t04756 t06213 t07677 t07679 t07685 t07686 t07689 t07693 |
| 12 | **`LambdaMetafactory`** called as a method (`metafactory`, `altMetafactory`, `LambdaConversionException`'s rules) | t04761 t04766 t04767 t04768 t04770-t04774 t04776 t04777 t04778 |
| 1 | `StringConcatFactory` | t04758 |
| 2 | another area first: javac's tool and the class-file API (t06156), jdk.dynalink (t08097) | |

### What comes in, and how (jdk26u at `baf63fb`)

jdk26u's java.lang.invoke is LambdaForms, interpreted or spun into hidden classes' bytecode by the VM, and the VM's linkage of
signature polymorphic calls and of `MethodHandles.lookup()`'s caller: none of it can run in the closed world. So the
run-time classes are **jrt's own Java over java.lang.reflect**, with jdk26u's Java wherever it runs as written:

| part | how |
|---|---|
| `MethodHandle` (jrt's own) | a handle's `invokeImpl(Object[])`: the arguments boxed as its type's parameters box them, the result boxed (null for void). `invokeExact` and `invoke` stay `final native` varargs of `Object[]` (JVMS 2.9.3), so that the analyzer recognizes their calls; their natives (`go/arbace/jrt/natives.ae`) throw as reflection's do. `asType` (jdk26u's `isConvertibleTo`, then a pairwise converting adapter), `asSpreader`, `asCollector`, `asVarargsCollector`, `withVarargs`, `bindTo`, `invokeWithArguments`, `toString` ("MethodHandle"+type): jdk26u's checks and messages |
| `DirectMethodHandle` (jrt's own) | a field, method or constructor over reflection, its reference kind and member for `revealDirect`: getters and setters `Field.get`/`set`, statics `Method.invoke(null, ...)`, virtual and interface calls `Method.invoke` on the receiver (reflection's dispatch, the JVM's), constructors `newInstance`; an `InvocationTargetException`'s cause thrown as itself. `invokeSpecial`: a private method directly, Object's three by identity, an inherited method called non-virtually only when the receiver's class does not override it (else UnsupportedOperationException: JCS352) |
| `MethodHandleImpl` (jrt's own) | the adapters (a handle of the new type computing its target's arguments) and the value conversions of asType and `explicitCastArguments`, as jdk26u's ValueConversions do them on boxed values (a reference cast, a primitive widened or cast, boxing then a cast, a dynamic unboxing then widening, "Cannot cast X to Y") |
| `MethodHandles` and `Lookup` (jrt's own) | the lookup modes, `in`, `dropLookupMode`, `toString`, `privateLookupIn`: jdk26u's text (no module boundary); `find*` and `unreflect*` resolve as JVMS 5.4.3 does over the declared members (the class, its superclasses, its superinterfaces, an interface's Object methods; the descriptor exact), check access by `VerifyAccess` (derived below), narrow a protected member's receiver (`restrictProtectedReceiver`), and `findSpecial` requires the special caller as jdk26u does; `findVirtual(MethodHandle.class, "invoke"/"invokeExact")` the invokers; `bind`; `revealDirect` (jdk26u's `InfoFromMemberName`); the combinators `explicitCastArguments`, `permuteArguments`, `constant`, `identity`, `zero`, `empty`, `insertArguments`, `dropArguments`, `dropReturn`, `filterArguments`, `collectArguments`, `filterReturnValue`, `foldArguments`, `guardWithTest`, `catchException`, `throwException`, `tryFinally`, the invokers and the array handles. VarHandle stays outside the world (J2G-SPEC §8.5 compiles its constant handles): `findVarHandle` and its kin are declared, for the JDK code j2g's §8.5 reads, and throw. Not built: the loops, `tableSwitch`, `defineClass`/`defineHiddenClass`, `classData` |
| the call sites (jrt's own) | `CallSite`, `ConstantCallSite`, `MutableCallSite`, `VolatileCallSite`: the target a volatile field, `dynamicInvoker` a handle that reads it; jdk26u's checks |
| `MethodHandleStatics`, `MethodHandleNatives` (jrt's own) | the parts of jdk26u's without the VM: the exceptions' factories, Unsafe; the reference kinds and their predicates (`refKindName` ...) |
| derived (`test/jrt/derive.ae`'s `invoke-derivations`) | `MethodType` (Invokers' cache out: 2 replacements), `MethodTypeForm` (the LambdaForm and interpreter-entry caches out), `LambdaMetafactory` (ProxyLambdaMetafactory in place of InnerClassLambdaMetafactory), `sun.invoke.util.VerifyAccess` (Reflection's two answers from Class, packages compared by equality, no module boundary) |
| unpatched (`invoke-sources`) | `MethodHandleInfo`, `AbstractValidatingLambdaMetafactory` (every check of the metafactory, LambdaConversionException's messages), `WrongMethodTypeException`, `LambdaConversionException`, `StringConcatException`, `sun.invoke.util.BytecodeDescriptor` and `VerifyType`; `Wrapper` and `TypeDescriptor` are F26's |
| `ProxyLambdaMetafactory` (jrt's own) | the lambda's instances are JCS170's proxies over Dyn implementing the interface and the markers; a call of the interface's method (or a bridge's signature) is the implementation's, adapted by asType to the captured types and `dynamicMethodType`; nothing captured, one instance for every call (jdk26u's); Object's methods by identity; another default method `invokeDefault`'s (JCS241: not built) |
| **the call site: `jdk.internal.jrt.Invokers`** | the Java forms' analyzer (`arbace/classes/analyze.ae`, SPEC §5.6 amended) writes a polymorphic call of `MethodHandle.invokeExact` or `invoke` as `Invokers.invokeExactR`/`invokeR` (R the call's return kind, V Z B C S I J F D or L, L's result cast to the call's type as javac's checkcast), the call site's type as class literals and the arguments boxed into an `Object[]`; and `MethodHandles.lookup()` as `Invokers.lookup(C.class)`, C the class of the code: the caller-sensitive lookup resolved where it is called. Only when the class environment has `Invokers` (a world before the conversion keeps the old nodes). The interpreter and j2g then translate ordinary static calls. java.lang.invoke registers its side (`Invokers.Access`) in MethodHandles' static initializer |
| j2g | `code.ae`'s `lookup-call?`: `Invokers.lookup(C.class)` of a JDK class (`pkg-of :jrt`) is still §8.5's nil, so the JDK's static initializers (the VarHandle idiom's lookups) stay exactly as they were; in a program's own class and Arbace's a real lookup (`reach.ae` likewise). J2G-SPEC §12 amended |

**Which analyzer.** The interpreter's is the executable's own (the checkout's, after the night's build), and so is j2g's at
Java's compiled level (hosted on the built executable). The build's own j2g run is on stage 0, whose analysis is the seed's: the
JDK's and Arbace's classes keep the old nodes (lookup() the nil, a polymorphic call an operation-level stub), as today, until a
re-seed. No class of the world calls a MethodHandle polymorphically but arbace/lang's KeywordInvokeSite and ReflectorCallSite
(reworked and unused in the Go build).

### Light checks done

(main's stage 0 as `ARBACE_HOST_EXE`; javac's tool `javac-5ad357331dbcd7cfb0fd` with `REPR_JAVA_STALE_OK=1`; the gate executable
`ad3066cf41ea6faf1566`; scratch `.tmp/inv/`)

- **The joint javac check** (`.tmp/night7/all9-run.sh`): **3,340 files, 0 errors**, 6,973 classes (32 s, 1.0 GB).
- `bin/jrt-convert derive --check`: 84 overlays, 0 not as derived (the control as before).
- **The prediction** (`bin/jrt-convert predict` with the slice's 43 files, F26's java.lang.constant, jdk.internal.constant,
  Wrapper and TypeDescriptor as PATHs, `--list` of the 43 tests; 708 s, 2.09 GB): the read 233 files, 231 converted (the two
  locale overlays' known failures), no javac: 0 fallbacks of the slice's; vet: the slice's classes declared and analyzed, refused
  only F26's known `DynamicConstantDesc` (its `Enum$EnumDesc`, the night's edge); **j2g's analysis of the world: no failure in
  java.lang.invoke, sun.invoke.util or Invokers** (the other 15 the PATH-only world's, the night's conversion); the stubs in the
  slice's classes: MethodType's serialization (the streams are in the night's world) and `Class.describeConstable` (jrt lacks
  it: `MethodType.describeConstable` is a stub); **the 43 tests at compiled, analyzed on the world: 39 analyze, 4 refused**
  (t04675 Thread.Builder.OfPlatform, t04758 StringConcatFactory, t06156 the class-file API's ClassModel, t08097 jdk.dynalink).
- **The 43 interpreted, by day** (`.tmp/inv/day/`): the forms of the read, interpreted on the gate executable with the checkout's
  analyzer evaluated over the executable's (`load.ae`). Today's executable has world stubs of `java.lang.invoke.MethodType` and
  `MethodHandles$Lookup` (edge classes) that a class of the same name cannot replace, and no java.lang.constant (F26 is tonight's),
  no Proxy over Dyn (JCS170 is tonight's), no ObjectStreamField: so the day's runtime is renamed into one package `jinv`
  (`prep.sh`: java.lang.invoke, sun.invoke.util's four and Invokers, F26's day loader's flattening), the members naming
  java.lang.constant or the object streams cut (`strip.py`), the intern table bypassed (ReferencedKeySet.get is not translated in
  today's world), the tests' forms renamed alike. None of this is in the build. Judged by `test/java/verdicts.edn` (status, or
  stdout's digest against the JDK's): **23 of the 43 pass**: t03287 t03306 t03574 t03593 t03630 t03631 t03632 t03633 t03634 t03635
  t03862 t03895 t03899 t04745 t04746 t04747 t04748 t04751 t06213 t07679 t07685 t07689 t07693 (the five stdout-judged ones byte for
  byte the JDK's). The rest:
  - the twelve metafactory tests validate (the validation tests' expected LambdaConversionExceptions among them) and stop at
    today's `Proxy` ("jrt.As: ProxyLambdaMetafactory$Handler": the interpreted handler is no InvocationHandler there, JCS170's
    `InvocationHandler_Cast` fixes it tonight);
  - t07686: a private method's `Method.invoke` on an interpreted subclass's object runs the subclass's private method of the same
    name (the interpreter's Dyn dispatches it by name): **JCS350**;
  - t07677: an interpreted inner class's constructor lacks its outer instance's parameter in reflection
    (`getDeclaredConstructors` of `O$In` gives `[]`): **JCS351**;
  - t04612 (StackWalker's `RETAIN_CLASS_REFERENCE`), t04675 (`System.Logger`, `Thread.Builder.OfPlatform`), t04756 (its p2.T3 not
    loaded by the day's loader), t04758 (StringConcatFactory: **JCS353**), t06156, t08097: other areas.
- `bin/jrt test --arch amd64 --package jrt`: ok (the natives compile; 301 s, 2.55 GB peak in the printing step, over the day's 2
  GiB by 0.5 GB).
- `bin/j2g-unit`: all passed, with the new `test-lookup-call` (a JDK class's `Invokers.lookup(C.class)` the nil, a test's and
  Arbace's real, the control) and `test-invokers` over the fixture `test/j2g/unit/invoke` (the analyzer's four rewrites and the
  control; skipped on a host whose analysis predates F15: stage 0 needs `J2G_UNIT_SOURCES=arbace.classes.analyze`, the gate's
  build at the checkout's key has it).
- `bin/gate --plan`: exit 0, no table error.

### Expected yield

Interpreted, of the 43: the 23, and of the twelve metafactory tests those JCS170's proxies carry (about 10: their checks
passed by day), **about 30-33**; the `:invoke` class's 41 then down to about 10, the rest in JCS350, JCS351, JCS353 and other
classes (StackWalker, Thread.Builder, javac's tool, dynalink). Compiled: of the 39 that analyze, those whose translation j2g
writes and whose run matches: the same ~33 less what the compiled level stubs (about 28-30). F26's ByteCodeTest (t06156) stays
F6's (javac's tool). Java eval and AOT each about +0.7 point.

### The night's commands

In the night's worktree with this branch merged (after the night's conversion of F26, which the slice builds on), heavy ones
under `bin/heavy-lock`:

```sh
# 0. light: the joint javac check (the sources +7 share +16 overlays), the derivations (84 overlays)
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check
# 1. jdk/ converted afresh: + java/lang/invoke (jrt's 11, the 3 derived, MethodHandleInfo, AbstractValidatingLambdaMetafactory,
#    the 3 exceptions), sun/invoke/util/{BytecodeDescriptor,VerifyType,VerifyAccess}, jdk/internal/jrt/Invokers; 0 failed,
#    no javac: 0 fallbacks
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules --check
# 2. j2g's edge (java.lang.invoke's classes leave it for the forms; VarHandle, ConstantBootstraps, StringConcatFactory stay),
#    javac's world's edge
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 3. the executable; j2g's report: no :analysis-failures under java/lang/invoke, sun/invoke/util, jdk/internal/jrt/Invokers;
#    the natives MethodHandle_InvokeExact_O1__O_native and MethodHandle_Invoke_O1__O_native found; then jrt's tests
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
grep -A12 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn | grep -E 'invoke|Invokers' ; echo "(none above: ok)"
# 4. a smoke at the REPL (the reflective path): expect 4, then "MethodHandle(int,int)int"
target/arbace-go/amd64/arbace -e '(let [mh (.findStatic (java.lang.invoke.MethodHandles/publicLookup) Math "max"
    (java.lang.invoke.MethodType/methodType Integer/TYPE Integer/TYPE (into-array Class [Integer/TYPE])))]
  (println (.invokeWithArguments mh (object-array [(int 3) (int 4)]))) (println (str mh)))'
# 5. Java's levels interp and compiled on the 43 (their ids: /root/arbace/.tmp/worktrees/java-invoke/.tmp/inv/ids.txt), then the
#    whole corpus with the night's others: saved, not recorded; expect about 30-33 interp, 28-30 compiled
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --list /root/arbace/.tmp/worktrees/java-invoke/.tmp/inv/ids.txt --save .tmp/f15-night/check
# 6. j2g's unit tests on the new executable (test-invokers runs on it: its analysis is the checkout's)
J2G_UNIT_EXE=$PWD/target/arbace-go/amd64/arbace bin/j2g-unit
```

**Risks and fallbacks.**
- (1) The build: a JDK class that called `MethodHandles.lookup()` and then a real Lookup method beyond §8.5's VarHandle factories
  (none in today's `jdk/forms` but CDS's strings; F26's classfile code takes a Lookup as a parameter) now gets a nil receiver: a
  NullPointerException where an operation-level stub threw UnsupportedOperationException. Pattern: an NPE in a static initializer
  naming `MethodHandles$Lookup`. Fallback: that class's lookup by JCS222's variant.
- (2) `jrt.As`/ClassCastException naming `ProxyLambdaMetafactory$Handler`, or `UnsupportedOperationException` from
  `Proxy.newProxyInstance`: JCS170 not in the build; revert group 2 alone (`118f3956e`).
- (3) A world class's analysis failure naming `MethodHandles$Lookup`, `MethodType` or `VarHandle` members (`No accessible method
  find...`): the old edge stubs had members jrt's own lack; the member to add is the message's. Revert: groups 1-3 (`30dcc3ee8`,
  `118f3956e`, `83a98a659`); jdk/ converted again without `invoke-sources` and the overlays.
- (4) An interpreted test that ran before and now fails at a polymorphic call ("No method invokeExact..." / "...in
  jdk.internal.jrt.Invokers"): the analyzer's rewrite (`invokers-call`); revert `30dcc3ee8`'s analyzer hunk and `83a98a659`.
- Areas to look at in the night's whole-corpus diff: `jdk/java/lang/invoke`, `hotspot/jtreg/compiler/jsr292`,
  `compiler/runtime`, `compiler/c1`, `compiler/inlining`, `compiler/exceptions`, `runtime/Nestmates`, and any test whose
  sources name `MethodHandles` (StackWalker's, StringBuilder's StressSBTest).

## Proposed (F15)

### JCS350 DECISION: a private method's reflection on an interpreted class

`Method.invoke` of an interpreted class's private method, on an object of an interpreted subclass declaring a private method of
the same name, runs the subclass's: the Dyn's member table dispatches it by name, where a private method is not virtual (JVMS
5.4.6). F15's direct handles call through reflection, so `findVirtual` of a nestmate's private method dispatches wrongly at the
interp level (t07686; compiled j2g calls a private method's `Impl_` directly). (a) The Dyn's invoker of a private method calls the
declaring class's Meth directly (`arbace.j2g.dyncf`, the Go of the interpreter's classes), half a day with the interpreter's
tests; (b) a divergence class. **Recommended: (a).**

### JCS351 DECISION: an interpreted inner class's constructor in reflection

`getDeclaredConstructors` of an interpreted inner class lists its constructor without the enclosing instance's parameter (and a
local class's without its captured values), where javac's class has them first (and last); so `findConstructor(Inner.class,
methodType(void.class, Outer.class))` finds nothing (t07677), as `getDeclaredConstructor(Outer.class)` does not. (a) Publish the
constructor with javac's descriptor and have its invoker take the implicit arguments first (F14's reflection metadata, the
`:reflection` class's InnerClasses line), 1 day; (b) a divergence class. **Recommended: (a) with F14's next slice.**

### JCS352 DECISION: invokeSpecial of an overridden method

`findSpecial` (`super::m` in a lambda, a handle's non-virtual call) of a method its receiver's class overrides needs a
non-virtual call, which jrt's reflection has not (the member tables' invokers dispatch virtually): it throws
UnsupportedOperationException naming it. No test of the corpus needs it. (a) j2g writes a second invoker per overridable method
(`MethodInfo.InvokeSpecial`, the declaring class's own Go method) and the Dyn its Meth, 1 day, the executable a little larger;
(b) as built, until a test needs it. **Recommended: (b).**

### JCS353 DECISION: StringConcatFactory

t04758 calls `makeConcat` and `makeConcatWithConstants` as methods (javac's string concatenation is compiled directly, J2G-SPEC
§7.5). (a) jrt's own StringConcatFactory over jrt's handles, its recipe parsing and checks jdk26u's (the StringConcatException
messages), half a day; (b) leave it to the `:invoke` class. **Recommended: (a)**, the next slice of F15.

### JCS354 DECISION: real lookups in the JDK's classes (JCS222 again)

F15 keeps `MethodHandles.lookup()` in a JDK class §8.5's nil, so nothing in the world's static initializers moves tonight.
With java.lang.invoke in the world, JCS222's idiom (a private lookup's method handles made once in a static initializer:
DatagramSocketAdaptor's, Thread's, ClassValue's, the HTTP client's) could run as jdk26u wrote it, retiring F10b's variants per use.
(a) After the night proves F15: the lookup real in every class whose initializer uses it for more than VarHandles (the lookup's
uses read as §8.5 reads the stores), each such class checked by the area's tests, 1 day; (b) a variant per use, as F10b did.
**Recommended: (a)**, after the night's records; it is also JCS222 (a) without j2g's constant method handles.

### JCS355 DECISION: the `:invoke` class after the night

The class's matcher is `java.lang.invoke` in a failure's message; after the night most of its tests pass and the rest fail on
other pieces. (a) The night's rerun sorts them: JCS350's, JCS351's, JCS353's tests into classes of their own (`:interp-private`,
`:interp-inner-ctor`, `:invoke` kept for StringConcatFactory and what names java.lang.invoke still), the class's `:why` rewritten
(F15 built); (b) as is. **Recommended: (a)**, with the night's records.

### JCS356 DECISION: MethodType.describeConstable

jrt's `Class` lacks `describeConstable` (Class is hand-written), so MethodType's is an operation-level stub, and every handle's
`describeConstable` is empty. (a) `Class.describeConstable` in jrt (jdk26u's: a ClassDesc of the descriptor string, F26's
ClassDesc), with `MethodHandle.describeConstable` for direct handles (jdk26u's `DirectMethodHandleDesc.Kind` from the reference
kind), half a day; (b) as built. **Recommended: (a)** when a test needs it (none of the 43 does).

## Decided (the user, 2026-11-09, about 11:10): JCS350-JCS356

As recommended: JCS350 (a), JCS351 (a) with F14's slice, JCS352 (b) (as built, until a test needs a non-virtual call of an
overridden method), JCS353 (a) as F15's next slice, JCS354 (a) after the night proves F15, JCS355 (a) after the night's rerun,
JCS356 (a) when a test needs it. Built on the branch `java-invoke2` (from `java-invoke` d0bf31ea8), one commit a proposal; the
section below. JCS354, JCS355 and JCS356 wait for their conditions: nothing of them is built.

## As built: JCS350, JCS351, JCS353 (2026-11-09; branch `java-invoke2`, write-ahead)

By day under the light rules (no conversion, j2g run, build or compiled level). All three change what the night builds: the
executable (JCS350's Go of the interpreter's classes, JCS351's `Compiler$CF` and the interpreter, its annotation data) and
`jdk/forms` (JCS353's overlay, at the night's `bin/jrt-convert update`).

**JCS350 (a), `1a1be6b58`.** The interpreter's classes publish their methods to jrt by `Compiler$CFGo.setMethod`, whose Go is
`arbace.j2g.dyncf`'s native: a virtual method there took the Dyn's slot of its key, became the class's method by key (`ByKey`),
and its member table entry called *the receiver's class's* method of that key. A private method is selected as itself (JVMS
5.4.6: it overrides nothing and nothing overrides it; the interpreter's own calls already did so, `interp/private-method`), so
the native now gives a private instance method no slot and no by-key entry, and its member table entry calls its own
implementation. `Method.invoke` of a nestmate's private method on a subclass's object, and F15's `findVirtual` over it, no longer
run the interpreted subclass's private method of the same name (t07686, `runtime/Nestmates/privateMethods/
TestMethodHandlesHierarchy`). j2g-unit's `test-private-reflection` checks the native's form as data (the rule's two parts), with
`dyn.ae`'s proxy `setMethod` (no private methods) as the control; the run needs the night's executable.

**JCS351 (a), `83cf8b52f`.** An interpreted class with an outer instance (`this$0`) or captured values publishes its
constructors with javac's descriptor (jdk26u's `Lower`: the enclosing instance's parameter first, the captured values' last;
`interp/implicit-params!`), and the constructor's `Meth` carries the fields those arguments set (`implicit`, a new field);
`Compiler$CF$Rt/construct` (the member table's `New`) sets them from the leading and trailing arguments, then runs the
constructor on the rest, as the interpreter's `new` does (`implicit-fields`). So `getDeclaredConstructors` of an inner class lists
`In(Outer, ...)`, `getDeclaredConstructor(Outer.class, ...)` finds it, and F15's `findConstructor(In.class, methodType(void.class,
Outer.class))` too (t07677, `runtime/Nestmates/privateConstructors/TestMethodHandles`); before, the reflective path left `this$0`
null. Not changed: an anonymous class whose superclass is inner keeps its superclass's outer instance out of the descriptor
(javac's `(this$0, superOuter, ...)`; the interpreter sets it from the `new` site: JCS358). With it F14's slice: the annotation
data (`arbace.classes.annot`) keyed a constructor's C and P records by the declared descriptor, while jrt's `Constructor` (the
compiled classes' member tables already, now the interpreted ones too) has javac's, so an inner class's constructor annotations
were not found at either level; they are now keyed by javac's descriptor (`safe-ctor-desc`, as the G and Y records were), and a
parameter's index follows the leading implicit parameters, as the JDK's `Constructor.getParameterAnnotations` shifts the class
file's (`handleParameterNumberMismatch`: a member class's one, an enum's two; a local or anonymous class's the JDK leaves
unshifted, "can't do reliable parameter counting": JCS357). j2g-unit's `test-inner-ctor-annotations` over the fixture
`test/j2g/unit/innerctor` (`Outer.java`, its forms by repr/java): the inner class's C and P lines name `(Lr.Outer;Ljava.lang.String;)V`,
the parameter's at index 1; the control, a static nested class's, its own descriptor at index 0.

**JCS353 (a), `cc7ba8f28`.** `overlay/jdk/java.base/java/lang/invoke/StringConcatFactory.java`, jrt's own in place of jdk26u's:
jdk26u's text for `makeConcat` (the mocked recipe), `makeConcatWithConstants`' linkage checks in their order with their
`StringConcatException` messages (the null checks, the caller's private access, `parseRecipe`'s argument and constant counts,
the return type, the 200 argument slots, "Generator failed"), `parseRecipe`, and `makeSimpleConcat`'s shapes with their
stringifiers (`String.valueOf` by type; `StringConcatHelper`'s `newStringOf` and `simpleConcat`, whose `stringOf` makes null and
a null `toString` "null"). What jdk26u's hidden class did (`InlineHiddenClassStrategy`, spun by the class-file API) is an
adapter (`MethodHandleImpl.adapter`) that stringifies the arguments left to right, then joins them between the recipe's
constants: the hidden class's order (every argument's string before the result is assembled). The handles are adapters, not
lookups: a JDK class's `MethodHandles.lookup()` is J2G-SPEC §8.5's nil. `print-sources` lists it as the overlay (`overlay`
kind replaces the edge class at the night's conversion and edge). `test/jrt/scf-diff/run.bash` (the oracle tier's, by hand):
the overlay compiled on the JDK as package `scf` (`Shim`: jdk26u's handles for the adapters, `parameterSlotCount` computed) and
28 cases (every primitive and reference shape, constants, nulls, a null `toString`, a non-String return type, the five checks'
exceptions and messages, 201 and 200 slots) against the JDK's `StringConcatFactory`: **0 differ**; `--control` (stringOf's
"null" changed): 5 differ, as it must.

**Light checks** (`ARBACE_HOST_EXE` main's stage 0; scratch `.tmp/`):

- the joint javac check (`/root/arbace/.tmp/night7/all9-run.sh`): **3,341 files, 0 errors**, 6,974 classes (30 s, 1.0 GB);
- `bin/jrt-convert derive --check`: 84 overlays, 0 not as derived;
- `bin/j2g-unit`: all passed (the two new tests; `test-invokers` skipped on stage 0 as before);
- `bin/jrt test --arch amd64 --package jrt`: ok (alone; nothing of jrt's Go changed);
- `bin/reap`: 41 of 44 cells checked, 0 problems; `bin/crops-score --judge --strict`: 0 problems; `bin/gate --plan`: exit 0.

**Expected yield.** t07686 and t07677 at interp (F15's 23 plus these two: about 25 of the 43 by day's measure, 32-35 with the
twelve metafactory tests JCS170 carries), t04758 at interp and, if j2g's analysis of `BasicTest` no longer refuses it
(StringConcatFactory now in the world), compiled. The `:invoke` class's 41 down by about three more. Beyond the corpus: any
test whose interpreted inner classes are constructed reflectively (a framework's runner, a ServiceLoader-like factory) and any
inner class's constructor annotations, at both levels.

**The night's commands** (in the night's worktree with `java-invoke` and this branch merged; heavy ones under
`bin/heavy-lock`; F15's own commands first, `doc` "As built: F15 (java.lang.invoke)"):

```sh
# 0. light: the joint javac check (+1 overlay), the derivations (84), the units
/root/arbace/.tmp/night7/all9-run.sh $PWD && bin/jrt-convert derive --check && bin/j2g-unit
# 1. jdk/ converted afresh (F15's update: + java/lang/invoke/StringConcatFactory as the overlay); j2g's edge
#    (StringConcatFactory leaves it for the forms)
REPR_JAVA_EXE=$(ls -td /root/arbace/.tmp/gate-exe/javac-*/ | head -1)arbace-javac REPR_JAVA_STALE_OK=1 \
  ARBACE_HEAVY_CMD=$PWD/bin/heavy-lock bin/jrt-convert update && bin/jrt modules --check
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge
# 2. the executable; no :analysis-failures naming StringConcatFactory, Compiler$CF$Rt or dyncf's natives
bin/heavy-lock bin/arbace-go --build && bin/arbace-go --smoke && bin/jrt test
grep -A12 ':analysis-failures' target/arbace-go/amd64/j2g/report.edn | grep -E 'invoke|Invokers|StringConcat|Compiler' ; echo "(none above: ok)"
# 3. the three measures, then F15's 43 (/root/arbace/.tmp/worktrees/java-invoke2/.tmp/inv/ids.txt, a copy of java-invoke's):
#    expect t07686, t07677, t04758 pass at interp
JAVA_CHECK_EXE=$PWD/target/arbace-go/amd64/arbace J2G_GO_EXE=$PWD/target/arbace-go/amd64/arbace \
  JAVA_CHECK_HEAVY=$PWD/bin/heavy-lock bin/java-check --levels interp,compiled --jobs 8 \
  --list /root/arbace/.tmp/worktrees/java-invoke2/.tmp/inv/ids.txt --save .tmp/f15b-night/check
grep -E '^(t07686|t07677|t04758)' .tmp/f15b-night/check/results.tsv
# 4. j2g's unit tests on the new executable (test-invokers runs there); the whole corpus with the night's others
J2G_UNIT_EXE=$PWD/target/arbace-go/amd64/arbace bin/j2g-unit
```

**Revert hints** (merge2's style: the commit, the failure's pattern, what to revert):

- `1a1be6b58` (JCS350): a reflective call of an interpreted class's private method failing with "abstract" or calling nothing
  (`dynAbstract` naming a private method), or an interpreted class's private method no longer reached through a world
  interface's slot (none should be: a private method implements nothing). Revert the commit alone (dyncf.ae and its unit test).
- `83cf8b52f` (JCS351): an interpreted inner or local class constructed by reflection failing with an `ArrayIndexOutOfBounds`
  or `IllegalArgumentException: wrong number of arguments` in `Compiler$CF$Rt.construct`, or a test that called
  `getDeclaredConstructor(String.class)` on an inner class and relied on the old (wrong) shape; annotations of a constructor
  missing where they were found before (`annot`'s C/P keys). Revert the commit (Compiler.ae's `implicit` and `construct`,
  interp's `implicit-params!`, annot's keys, the fixture); the annot hunk can be reverted alone (its keys) if only the
  annotations regress.
- `cc7ba8f28` (JCS353): the build's j2g refusing `java/lang/invoke/StringConcatFactory` (a member of jrt's handles it names that
  the world lacks), or an Arbace program's indy concatenation (J2G-SPEC §7.5 compiles it directly, so none expected) behaving
  otherwise. Revert the commit; `bin/jrt-convert update` again (the overlay's forms out).

### Proposed (JCS350-JCS353's follow-ups): JCS357-JCS358

### JCS357 DECISION: a local or anonymous class's constructor parameter annotations

The JDK's `Constructor.getParameterAnnotations` of a local or anonymous class returns the class file's array unshifted (its
`handleParameterNumberMismatch` cannot count them: javac writes the declared parameters' annotations only), so index 0 holds the
first declared parameter's annotations though parameter 0 is the outer instance; JCS351's records shift by the leading implicit
parameters for every class (a member class's and an enum's exact). (a) jrt's `Constructor.getParameterAnnotations` returns the
declared count for a local or anonymous class (the records unshifted for them, `annot`'s `ctor-lead` 0 when the class is local
or anonymous), an hour and a fixture case; (b) as built until a test reads them. **Recommended: (b)**: no test of the corpus is
known to, and (a) is small when one does.

### JCS358 DECISION: an anonymous class's superclass's outer instance in reflection

javac's constructor of an anonymous class whose superclass is an inner class takes the superclass's enclosing instance after
its own (`(this$0, superOuter, args..., captured...)`); the interpreter sets the superclass's `this$0` from the `new` site
(`super-outer-fields`), so JCS351 publishes such a constructor without it, and a reflective construction leaves it null. (a) The
super-outer parameter published and set (`implicit` gains a third group: the superclass's field), half a day with a fixture;
(b) as built. **Recommended: (b)**: an anonymous class is not constructed reflectively in the corpus.

## Decided (the user, 2026-11-09, about 14:20): JCS357, JCS358

JCS357 (b) and JCS358 (b), as recommended: as built until a test reads a local or anonymous class's constructor parameter
annotations, or constructs an anonymous class with an inner superclass reflectively. Nothing built (branch `java-indep`;
doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JCS340-JCS343, JCS357, JCS358").

## As built: h-java-readprint (write-ahead), 2026-11-17

Branch `h-java-readprint` (from next `77630727b`), by day under the host burst's light rules: Java's read cell (7,748 / 0 / 96)
and print cell (3,186 / 32 / 107) triaged by a light run of their non-passing tests on next's gate executable
`f60652fb7fb9568e76b1` and javac's tool `javac-f70d1a9d900eb07a9afc` (`REPR_JAVA_STALE_OK=1`): `--levels read,classes,print`
over the 139 print and read/classes ids (6 jobs, 6 minutes), `--corpus langtools-diagnostics --levels diagnostics` over the
93 diagnostics ids (77 s); nothing built.

**Stale rows.** The read cell's 96 are not stale: read 1, classes 2 and diagnostics 93 fail as recorded (`:read-platform` 3;
`:diag-release` 44, `:diag-processors` 25, `:diag-doclint` 8, `:diag-file-manager` 8, `:diag-closed-world` 4,
`:diag-command-line` 4), each a limit of javac on Arbace's world or command line (JCS50-JCS53: ct.sym for --release, processors
from class files, jdk.javadoc's doclint, javac's own Main and file manager), all needing javac's tool rebuilt with more world.
Three print rows were stale: t05276 and t05749 (by design `:print-classes`, recorded before JCV11 (b) made such a row a pass
with its note) and t04728 (`:print-erasure`, its forms now equal).

**The class files' namer counted.** 44 more of the 72 `:print-classes-unnamed` rows were the named kinds: the namer took the
sets of code lines of the two class files, so an early return, a loop's `goto` or a default value's `aconst_null`/`astore`
also met elsewhere in the method left no line in the difference, and the first branch whose target moved was reported (`code:
main ...: ifeq L51`). `java.print/counted-kinds` (test/java/print.ae), used only where the sets name nothing: the lines counted,
each branch without its target; the kinds named only when every instruction that differs in number belongs to one (control
flow: the branches, goto, the returns, when a goto or a return differs or only conditional branches do, a test's sense and its
arms swapped; an increment: iinc, wide iinc and i = i + n's int arithmetic; a local's initializer: a default's constant and a
local's store); no instruction differing in number, the order of the code. Stricter than the sets' rule (which names a kind on
one matching line), and only additive: a row the sets named keeps its reason. The 47 rows (44 and the 3 stale) re-recorded
alone (`--judge --partial --record`): Java print **3,186 / 32 / 107 → 3,236 / 29 / 60**, Java completeness 87 → **88** (meant 90).
The negative control c00001 still differs at print; t06032 (below) stays unnamed as it must.

**The 25 left in `:print-classes-unnamed`** (all the converter's, none the printer's): a static member through a pure
qualifier, `_instance.vMeth()`, `"foo".copyValueOf(...)`, whose local or literal the converter drops (`pure?`, convert.ae's
static field and call sites; javac's `aload`/`ldc` `pop`) 16 (t03444 t03733 t03807 t03808 t03825 t04629 t04700 t04806 t04807
t05058 t05059 t06111 t06347 t07690 t07692 t07763); `s = s + x` on a String field read as the form `s += x` prints (`aload_0`
against `dup`) 3 (t03356 t03770 t05101); an inherited static called by its simple name in a subclass, `sleep(1)`, printed
`Thread.sleep(1)` (the method ref's owner) 1 (t05179); a local's slot, locals declared in another order 1 (t05125); the
converter's types: `pos[0] != 0L` read as `(== (aget pos 0) 0)` (the long literal lost, i2l/lcmp against if_icmp) t03434, and
`float i = it.next()` read without its float tag (i2f fstore against istore) t06032; two mixed (t03567, t06072). The
`:print-type-arguments` 31 stay JFS75 (b)'s (the printer cannot write type arguments the forms do not carry: a raw
`new Function[]{lambdas}`, a cast to a type variable dropped), `:print-overload-cast` 1 RPS65's, `:print-release` 2 JCS50's.

**The by-design rows audited** (29 after the stale three): `:print-shapes` 18 (when/if, do/if tails, the converter's temporaries
o-30 printed o_30, labels, a folded constant, a mutable local's default initializer dropped: RPS64 (a)) and `:print-erasure` 11
(the casts and tags of type arguments the forms lack: RPS9 (a)) each hold their reason; t04728 left `:print-erasure`'s :tests.

**Proposals.** RP1: the converter's pure qualifier of a static member (16 rows): (a) a named kind, by design (the forms keep
no evaluation without effect: a local's or a literal's), test/java/print.ae naming `aload`/`ldc`/`getstatic` with `pop`;
(b) the converter keeping `(do q (C/m))` for a pure q too (javac's tool rebuilt, jdk/ reconverted). Recommended (a). RP2: the
String compound assignment and the inherited static's owner (4 rows): (a) by design as RP1 (a); (b) the printer writing
`s = s + x` for a String field (the forms do not say which), the simple name for an inherited static when the class extends
the owner. Recommended (a). RP3: t03434 and t06032 are the converter's losses of a type (a long literal in a comparison, an
unused local's float): (a) fix in convert.ae for the night (javac's tool rebuilt; `bin/repr-java roundtrip` and `check --jdk`
records may move); (b) deferred. Recommended (a). arbace/repr/java/roundtrip.ae's class-differences has the same sets' rule
(the round trip's classes level); its counted form waits for a whole round trip's record at night.

### Decided (the user, 2026-11-17, round 51): RP1 (a), RP2 (a), RP3 (a); as built

RP1 (a) and RP2 (a): `java.print/counted-kinds` names three more kinds, in `class-kinds` and in `:print-classes`'s pattern (by
design): *a static member's pure qualifier* (the original's extra `pop`, with the loads, constants and static reads it pops),
*a compound assignment written out or not* (`aload` against `dup` alone) and *a static member's owner* (an invokestatic,
getstatic or putstatic of the same member through another class: the lines compared without their owners). The 20 rows
re-recorded alone: Java print 3,236 / 29 / 60 → **3,256 / 29 / 40**, completeness 88 → 88 (0.8765 → 0.8780; meant 90). Left in
`:print-classes-unnamed`: t05125 (a local's slot), t03567 and t06072 (mixed), t03434 and t06032 (RP3 (a), below). c00001 still
differs at print.

## As built: h-java-deferred (write-ahead), 2026-11-17

Branch `h-java-deferred` (from next `e4a7a30f3`), by day under the host burst's light rules: the eval cell's 611 deferred rows
(test/java/check-interp.edn as gate 13c recorded them, 2026-11-14) triaged by a light run of those tests (581 of them; the 30
`:speed-hang` left out) at interp on next's gate executable `f60652fb7fb9568e76b1` (javac's tool `javac-f70d1a9d900eb07a9afc`,
`REPR_JAVA_STALE_OK=1`, 6 jobs, 25 minutes; nothing built, nothing converted).

**The records were stale.** B2's world (the night conversion of 2026-11-15 and F26's removal of 2026-11-16) is in next's
executable but not yet in the interp record: **168 of the 581 pass** (reflection 51, security 33, nio 18, net 16, serialization
12, references 9, threads 8, invoke 7, stack 4, interp-extends 3, the rest 1 or 2). Those 168 rows alone re-recorded (`bin/java-check
--judge --partial --record` of their rows only); the others keep their rows (the night records them whole). Java's eval cell 2,549 /
165 / 611 → **2,717 / 165 / 443**; completeness 87 → **89** (meant 90 → 91).

**74 of the 413 still failing were in no class** under their new reasons, which would fail the night's judgment: classed in
test/java/divergences.edn (`:invoke` takes `sun.invoke.util` and `jdk.dynalink`, `:serialization` "Recursive update", the
rest by id under `:references`, `:reflection`, `:stack`, `:rmi`, `:net`, `:nio`, `:security`, `:util-classes`); the AOT cell's
classes unchanged (2,588 / 201 / 536: two compiled rows moved between world classes).

### The 413 by root cause (interp, next's executable)

| group | tests | cause | cost |
|---|---|---|---|
| ObjectInputFilter's System.Logger | 87 (+8 tests using System.Logger themselves) | every ObjectInputStream's filter factory calls `Config.traceFilter`, `configLog.log(TRACE, ...)`: System.Logger is not in the world (jrt's System is Go) | a derivation (F24's rule) |
| sun.invoke.util.Wrapper | 33 | MethodType's and MethodTypeForm's; F26 brought it and took it away (2026-11-16) | a derivation without its ClassDescs |
| cannot extend (ObjectOutputStream/InputStream 6, TextCallbackHandler 7, ConcurrentHashMap, CyclicBarrier, FileOutputStream, ...) | 29 | the DynSub list (`arbace.j2g.model/class-supers`, JCS120) | j2g's model, a build per class |
| DefaultLoggerFinder | 18 | java.util.logging's Logger.setupResourceInfo and LogManager ask `DefaultLoggerFinder.isSystem` (jdk.internal.logger is outside) | a derivation |
| protected or missing members (No accessible / No matching) | 48 | jrt's member tables, interpreted subclasses' protected world members (JCS120) | many small |
| ClassNotFoundException of a world class | 37 | SimpleFileVisitor 11, the asynchronous channels 11, sasl/kerberos, prefs, dynalink, ... | per area |
| RMI stubs (RemoteStub, UnicastRemoteObject, RegistryImpl_Stub) | 11 | F27's what-is-left | RMI's slice |
| ClassCache's "Recursive update" | 8 | jrt's ClassCache (overlay) called computeIfAbsent inside its own computation (ObjectStreamClass's superclass lookup) | a few lines |
| the rest | ~140 | behaviour after loading (dates, references under Go's GC, annotations' reflection, datagrams, ...) | per test |

### Built (unproven here: each needs the night's conversion and a build)

- **test/jrt/derive.ae**, `hjd-derivations`, four derived overlays: `java/io/ObjectInputFilter.java` (System.Logger and its Level as
  `jdk.internal.jrt.SystemLoggers.Logger`, System.getLogger as SystemLoggers.getLogger: F24's and JCS155 (b)'s rule),
  `java/util/logging/Logger.java` and `LogManager.java` (`DefaultLoggerFinder.isSystem` as `SystemLoggers.isSystem`),
  `sun/invoke/util/Wrapper.java` (its nominal descriptors left out: java.lang.constant stays outside since F26 left).
- **overlay/jdk/java.base/jdk/internal/jrt/SystemLoggers.java**: `isSystem(Module)`, DefaultLoggerFinder's answer
  (`VM.isSystemDomainLoader(m.getClassLoader())`).
- **overlay/jdk/java.base/java/io/ClassCache.java**: get computes outside the map, the first value put kept (ClassValue's
  semantics), so a computation may ask for another class.
- **arbace/lang/Compiler.ae**, `Compiler$CF$Try.eval` (the interpreter's try): a finally that jumps (return, break, continue) now
  discards the pending exception, JLS 14.20.2 (t03210, the jls corpus's Exceptions: `:interp-jls`; compiled passes already).
- Light checks: the four derived files made by a scratch replica of the ops (each replacement's count asserted) compile with
  SystemLoggers under the JDK's javac as java.base's patch (java.logging's sources beside, 34 classes, no error); the four
  overlays committed as that replica wrote them, header and all (bin/jrt-convert was not run by day): **`bin/jrt-convert derive
  --check` confirms them byte for byte** (the essential `jrt-derive`; `derive` rewrites them should a byte differ).
  The joint javac check (`/root/arbace/.tmp/night7/all9-run.sh`, the closure with the four overlays and the new SystemLoggers):
  3,091 files, 0 errors, 6,318 classes (34 s, 1.0 GB). The negative controls c00001, c00003, c00004 still fail as controls must; bin/reap 0 problems; crops-score --judge --strict
  and records-check pass.

### The night's commands

```sh
bin/jrt-convert derive --check && /root/arbace/.tmp/night7/all9-run.sh $PWD   # the four overlays as derived; the joint javac check
bin/heavy-lock bin/jrt-convert update && bin/jrt modules --check
bin/heavy-lock bin/j2g-edge update && bin/heavy-lock bin/javac-vendor edge    # Wrapper and the logging classes leave the edge
bin/arbace-go --gate-build                                                   # Compiler.ae and jrt moved gate_key
# the groups' tests at interp, then compiled (the same causes: their compiled rows fail on the same classes)
bin/java-check --levels interp,compiled --list IDS --save .tmp/hjd-night   # IDS: the ids below, one a line
```

The groups' ids. ObjectInputFilter: t04245 t04264 t04307 t04375 t04380 t04384 t04390 t04392 t04393 t04395 t04396 t04397 t04399 t04401 t04403 t04404 t04406 t04407 t04408 t04413 t04414 t04415 t04416 t04417 t04429 t04431 t04434 t04438 t04441 t04442 t04445 t04446 t04451 t04453 t04706 t04725 t04763 t04769 t04920 t04959 t05018 t05037 t05038 t05267 t05268 t05277 t05287 t05288 t05297 t05308 t05313 t05324 t05325 t05327 t05338 t05401 t05405 t05435 t05449 t05457 t05494 t05495 t05499 t05512 t05514 t05518 t05521 t05543 t05556 t05560 t05565 t05567 t05595 t05652 t05666 t05672 t05751 t05752 t05753 t05779 t06159 t06200 t06256 t06257 t06258 t07754 t07887 t07919 t07939 t07959 t07965 t07966 t07989 t08037 t08306. Wrapper: t03287 t03306 t03574 t03593 t03630 t03631 t03632 t03635 t03862 t03895 t03899 t04745 t04746 t04747 t04748 t04751 t04758 t04766 t04767 t04768 t04770 t04771 t04772 t04773 t04774 t04776 t04777 t04778 t07677 t07679 t07685 t07689 t07693.
DefaultLoggerFinder: t04313 t04792 t05061 t07855 t07856 t07857 t07859 t07860 t07861 t07862 t07978 t08121 t08122 t08123 t08134 t08136 t08137 t08139. ClassCache: t04381 t04398 t05315 t05520 t05717 t05964 t08036 t08234. Try: t03210.

**Expected yield**: the first failure of 146 of these 154 tests removed (the 8 that use System.Logger themselves stay); of them, those that pass beyond it (F15's estimate for the invoke tests:
23 of 33; the serialization tests stop next at their own pieces). AOT: the same derivations are the compiled level's world (its
rows of these tests fail on the same classes, or on the old ObjectOutputStream reason gate 13c recorded); print is unaffected (its
rows of these tests pass). Interp's other large groups (cannot extend, JCS120's members) need j2g's model and a build per class:
no light fix.
## As built: h-java-members (write-ahead), 2026-11-17

Branch `h-java-members` (from next `55e8c0ae7`), by day under the host burst's light rules: the two groups h-java-deferred left
by root cause ("As built: h-java-deferred", on its branch), the 48 "No accessible" / "No matching" members and the 37
ClassNotFoundExceptions, run again at interp on next's gate executable `f60652fb7fb9568e76b1` (javac's tool
`javac-f70d1a9d900eb07a9afc`, `REPR_JAVA_STALE_OK=1`; 85 tests, 6 jobs, 55 s): the same 85 results as h-java-deferred's run, each
test's cause then read from its full trace (test/java/run.ae with the trace printed) and from the executable's j2g output
(`amd64/j2g/prog`). Next's key has no gate executable yet; nothing built, nothing converted.

**Nothing of the two groups is provable by day**: every cause is in the executable (jrt's member tables, j2g's support tables,
j2g's DynSub list) or outside the world (a conversion). The interpreter's analysis (arbace.classes, loaded from a checkout that
differs) is not among the causes. No row moves by day; the rows stay as recorded (deferred, their classes h-java-deferred's).

### The 48 members by cause

| cause | tests | where | built here |
|---|---|---|---|
| protected members of a translated class a test extends, not in the DynSub list (`class-supers`): the object streams' protected constructors, resolveClass, readClassDescriptor, writeClassDescriptor, enableReplaceObject | 11: t04308 t04309 t04378 t04383 t04394 t04400 t04418 t04436 t04437 t04443 t04444 | arbace/j2g/model.ae | ObjectInputStream, ObjectOutputStream listed (and the 6 "cannot extend" tests of the object streams: t04379 t04385 t04387 t04388 t04432 t04439) |
| the same for SimpleFileVisitor (its constructor not even translated: J2g_NotTranslated), FileTypeDetector, BreakIterator, HttpRequest (protected constructors of anonymous or own subclasses) | 4: t05225 t05224 t05378 t05071 | arbace/j2g/model.ae | listed |
| Executable's own table lacks what Method and Constructor have (accessFlags, getAnnotated*): a call on an Executable does not resolve | 3: t04832 t04723 t04733 | j2g's support tables (out/support-table-forms) | Executable's entries: accessFlags, the four getAnnotated*, getParameters, each the receiver's Method's or Constructor's |
| String.transform missing (jrt's String hand-written, Function translated) | 1: t04649 | j2g's support forms and tables | `Transform_Function__O` (f.apply(this)) and its entry |
| System.setProperties missing (jrt keeps the properties in its own table; getProperties answers a copy) | 1: t04794 | j2g's support forms and tables | `J2g_SystemSetProperties`: jrt's table made the argument's stringPropertyNames (null leaves them) |
| Object's protected finalize missing from Object's table (clone was its only protected method) | 1: t06387 | j2g's support tables | Object.finalize, protected, jdk26u's empty body |
| an inner member class's constructors not in its member table (`member-tables` kept those whose real descriptor has the declared parameters alone) | 1: t05722 (AbstractQueuedLongSynchronizer$ConditionObject) | arbace/j2g/out.ae | listed with the outer instance first, as the JVM's reflection gives them (the interpreter's env drops it, inner-member?) |
| AQS's ConditionObject is jrt's ReentrantLock condition (go/arbace/jrt/locks.ae takes its name), with no constructor and tied to a ReentrantLock | 1: t04809 | jrt | no: an AQS-backed condition in jrt |
| jrt's hand-written classes lack the member: Class.getProtectionDomain (t03296 t04873), Thread.ofPlatform (Thread.Builder.OfPlatform, t04689 t04690 t05166), ForkJoinPool.schedule (the DelayScheduler, t05691) and getStealCount (t05719), ClassLoader.getSystemResourceAsStream (t03295: the test then defines its class from its class file, which the Go build does not have) | 8 | go/arbace/jrt (manifest, tables) | no |
| a protected member of a hand-written class that is not extendable: Semaphore.reducePermits | 1: t05707 | jrt (a class interface, JC4c's way) | no |
| java.lang.Package is a cut class (outside the world): registered with no interfaces (t04730: not an AnnotatedElement), no Class.getPackage (t07858), no ClassLoader.getDefinedPackage (t04539) | 3 | the closure (a conversion) | no |
| AsynchronousFileChannel cut (FileLock's constructor, tryToDeadlock(Channel)) | 2: t05141 t05150 | the closure | no |
| RMI's protected constructors (RemoteStub, RemoteObject, UnicastRemoteObject) and RMIJRMPServerImpl.makeClient | 11: t05241 t05242 t05243 t05246 t05248 t05249 t05250 t05252 t05253 t05256 t07954 | F27's RMI slice | no |

### The 37 ClassNotFoundExceptions by cause

| cause | tests | night |
|---|---|---|
| `java.nio.file.SimpleFileVisitor` not in javac's tool's world: jdk/tools/launcher's TestHelper names java.util.spi.ToolProvider, so the tests run on javac's tool (F6), whose java.base closure (bin/javac-vendor's base files) lacks it (the shipped program has it) | 11: t08312-t08322 | add it to javac's world (`bin/javac-vendor convert`); the tests then launch a JDK (test.jdk is none): expect them to fail further on |
| the asynchronous channels outside the world (AsynchronousChannel, AsynchronousChannelGroup, AsynchronousServerSocketChannel, AsynchronousSocketChannel, CompletionHandler; their sun.nio.ch providers) | 9: t05091-t05096 t05098 t05099 t05101 | a slice of its own (the providers over jrt's poller): not one closure line |
| `java.util.Calendar$Builder`: jrt's own Calendar (overlay) has no Builder | 2: t05460 t05461 | the overlay's Calendar.Builder (jdk26u's, over GregorianCalendar), `bin/jrt-convert update` |
| one class outside the world each: java.lang.reflect.MalformedParametersException (t04868), java.util.prefs (t05784), javax.naming.directory (t07839), com.sun.nio.sctp (t07863), LdapLoginModule (t07869 t07870), javax.security.auth.kerberos (t08053), javax.smartcardio (t08071), javax.xml.xpath (t08082), jdk.dynalink (t08097) | 10 | MalformedParametersException is one file (java.base share): the cheapest line; the others are modules or areas |
| `sun.rmi.transport.DGCImpl_Stub`: converted but not reached (RMI names its stubs by name) | 2: t05244 t05247 | F27's RMI slice (a root) |
| by design, the REPL's model (`:repl-sibling`): two of the test's packages importing each other (p1 imports p2.T3, which extends p1.T2) | 2: t04756 t06388 | none |
| a class defined from bytes (Proxy/nonJavaNames' "Test+Interface") | 1: t04878 | none (no class files in the Go build) |

### Built (unproven here: each needs the night's build; no conversion)

- **arbace/j2g/model.ae** `class-supers`: java.io.ObjectInputStream, java.io.ObjectOutputStream, java.nio.file.SimpleFileVisitor,
  java.nio.file.spi.FileTypeDetector, java.text.BreakIterator, java.net.http.HttpRequest (each translated and not final: a
  DynSub_C each, their protected members in the world's signatures' `:protected`).
- **arbace/j2g/out.ae** `support-table-forms`: Executable's accessFlags, getAnnotatedReturnType, getAnnotatedReceiverType,
  getAnnotatedParameterTypes, getAnnotatedExceptionTypes and getParameters (under the conditions Method's are written by), each
  calling the receiver's (a Method or a Constructor); String.transform; System.setProperties; Object.finalize (protected).
  `support-forms`: `String.Transform_Function__O`, `J2g_SystemSetProperties`. `member-tables`: an inner member class's public
  constructors with the outer instance first.
- **arbace/j2g/main.ae**: the scan's `Transform_Function__O`; the roots Properties.stringPropertyNames and getProperty
  (setProperties' calls).
- Light checks: the changed namespaces load on stage 0; the support forms made in a stand-in world (the conditions' classes
  translated) print as Go that gofmt accepts (`gofmt -e`).

Expected yield at interp: 22 of the 48 lose their first failure (11 object streams, 4 protected constructors, 3 Executable, transform,
setProperties, finalize, the AQLS condition) and 6 "cannot extend" tests of the object streams; how many pass beyond it is the night's
run. A risk to watch: Object's table now lists finalize, so an interpreted class's `super.finalize()` resolves where it failed (34
files of jdk/java and javac's tests declare a finalize; FinalizeOverride calls super's).

### The night's commands

```sh
bin/arbace-go --gate-build                                  # arbace/j2g moved gate_key (and javac's tool's key)
bin/java-check --levels interp,compiled --list IDS --save .tmp/hjm-night   # IDS below, one a line
bin/java-check --judge --partial --record .tmp/hjm-night/...                # the rows that moved alone
```

IDS: t04308 t04309 t04378 t04383 t04394 t04400 t04418 t04436 t04437 t04443 t04444 t04379 t04385 t04387 t04388 t04432 t04439
t05225 t05224 t05378 t05071 t04832 t04723 t04733 t04649 t04794 t06387 t05722, and for the finalize risk the tests of
jdk/java/lang/ref and the javac protectedAccess corpus.

The conversions for the night (not done here): `java/nio/file/SimpleFileVisitor.java` into javac's world's java.base files
(bin/javac-vendor, 11 tests); `java/lang/reflect/MalformedParametersException.java` into the closure (test/jrt/jrt_sources.ae, 1);
Calendar.Builder in overlay/jdk/java.base/java/util/Calendar.java (2); java.lang.Package into the world (3, with Class.getPackage).
The asynchronous channels (11 with the two cut AsynchronousFileChannel uses), RMI (13) and the single-class areas (prefs, JNDI's
directory, SCTP, kerberos, smartcardio, xpath, dynalink) are slices of their own, not closure lines.
