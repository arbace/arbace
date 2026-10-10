;; Go-build variant of sun.security.provider.NativePRNG (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Security"): read by c2g only. The JDK's NativePRNG reads its seeds from /dev/random (or the
;; file of java.security.egd or securerandom.source) and its random bytes from /dev/urandom (the
;; blocking kind both from /dev/random, the non-blocking kind both from /dev/urandom), mixed with
;; a SHA1PRNG seeded from them. The Go build reads both from the host's random source
;; (jdk.internal.jrt.HostRandom: Host.RandomBytes, getrandom(2) through Go's crypto/rand in B1a),
;; which has no blocking and non-blocking kinds (getrandom(2) blocks only until the kernel's pool
;; is initialized) and no file: the box (B1b) has no /dev. So the three kinds are always
;; available, and setSeed only mixes the seed into the SHA1PRNG (the JDK also writes it to
;; /dev/random, which only adds to the kernel's pool; here the seed file is null and the write
;; is skipped as when the file cannot be opened). The rest is jdk26u's (RandomIO's buffering
;; and mixing unchanged). The replacing members keep jdk26u's code where they keep it (baf63fb;
;; Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md);
;; the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.provider)

(c2g/variant NativePRNG
  (method ^:private ^:static initIO ^NativePRNG$RandomIO [^NativePRNG$Variant v]
    (when (some? debug)
      (.println debug (java-str "NativePRNG." v " seedFile: host nextFile: host")))
    (NativePRNG$RandomIO. nil nil)))

(c2g/variant NativePRNG$RandomIO
  (constructor ^:private [this ^File seedFile ^File nextFile]
    (set! (.-seedFile this) seedFile)
    (set! (.-seedIn this) jdk.internal.jrt.HostRandom/INSTANCE)
    (set! (.-nextIn this) jdk.internal.jrt.HostRandom/INSTANCE)
    (set! (.-nextBuffer this) (new byte/1 bufferSize))))
