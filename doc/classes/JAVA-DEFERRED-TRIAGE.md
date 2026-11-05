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

**jrt's hand-written Go, the day of the night** (designed, not edited: no Go is built by day; about half a day,
checked by `bin/jrt build` and `bin/jrt test`, jrt's own build, before the night's build):
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
# 0. the day's Go (above): standin_charset.ae, charset.ae's String overloads, codec.ae, access.ae, the manifest;
#    bin/jrt build && bin/jrt test pass
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
and none of its files is java.nio's. **Its Go natives are not written** (no Go can be built by day): the night
writes `go/arbace/jrt/udp.ae` to the design below before the build, or the build fails at j2g's natives check
on `HostDatagram`'s six natives.

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

**The Go side (the night's, before the build)**: a datagram socket is a handle of `HostNet`'s table (`netSock`
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

## Proposed

JCS143-JCS154 are JC9b's (branch `java-jc9b`): JCS143-JCS145 F16's, JCS146-JCS148 F8's, JCS149-JCS152 F7's,
JCS153-JCS154 F11's. JCS140-JCS142 are F4's and F2's ("As built: F4 (write-ahead)", "As built: F2 (write-ahead)", branch
`java-jc9-pki`); JCS136-JCS137 are F1's (`java-jc9-jca`), JCS138-JCS139 F5's (`java-jc9-logging`).

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

## Decided (2026-11-05, the user)

JCS128-JCS135, each as recommended: :management split (:jmx by design, :mxbeans deferred); the generated by-design class; the harness's classes out of the deferred count (JCS99 (c) applied); TLS by translating SunJSSE after the JCA and PKI; RMI by design; Kerberos and GSS-API by design, SASL deferred; JC9 (F1, F5, F4, F2) then JC9b (F16, F8, F7, F11), JCS128-JCS130 first; the closure shipped, the executable's size and build time measured at each step.

## Decided (2026-11-05, the user: JCS140-JCS142)

JCS140 (a): jdk26u's cacerts as data from the build tree; JCS141 (b): jar verification a later small fix, outside F2; JCS142 (a): F2's internal packages visible at the REPL.

## Decided (2026-11-05, the user: JCS143-JCS154)

JCS143-JCS154 as recommended: 143 (b) F16b after JC9b's nights; 144 (a) version 26, the version-printing tests to the harness class; 145 (a) field updaters as built, the exact check with F20; 146 (b) jdk.charsets later; 147 (a) jdk26u's readers and writers, jrt's as fallback; 148 (a) tables generated on javac's tool; 149 (c) positional I/O and locks over today's natives; 150 (a) mappings as heap copies written back; 151 (b) DirectByteBuffer generated; 152 (a) isDirect false; 153 (c) the legacy datagram delegate, the adaptor with F10; 154 (a) IPv6 scope names with F11.
