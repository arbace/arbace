;; Go-build variant of sun.security.provider.SeedGenerator (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Security"): read by c2g only. The JDK's seed generator reads securerandom.source (the
;; JDK's file:/dev/random: NativeSeedGenerator, reading the file) or falls back to
;; ThreadedSeedGenerator (thread scheduling jitter). The Go build's seeds come from the host's
;; random source (jdk.internal.jrt.HostRandom), what NativeSeedGenerator would read on Linux
;; without the file; the static initializer that chooses a generator is cut. The system entropy
;; (SHA1PRNG's and DRBG's additional seed material) is the JDK's but for the network adapters
;; (java.net.NetworkInterface is not in the Go build) and lists the temporary directory with
;; File.list (jrt's java.nio.file has no directory streams); its value is not observable. The
;; replacing members keep jdk26u's code where they keep it (baf63fb; Copyright (c) Oracle
;; and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md); the rest Copyright
;; (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.security.provider)

(c2g/variant SeedGenerator
  (c2g/cut (static-initializer 0))

  (method ^:public ^:static generateSeed ^void [^byte/1 result]
    (jdk.internal.jrt.HostRandom/fill result 0 (alength result)))

  (c2g/cut ^:private ^:static addNetworkAdapterInfo ^void [^MessageDigest md])

  (method ^:static getSystemEntropy ^byte/1 []
    (let [^:mutable ^MessageDigest md nil]
      (try
        (set! md (MessageDigest/getInstance "SHA"))
        (catch NoSuchAlgorithmException nsae
          (throw (InternalError. "internal error: SHA-1 not available." nsae))))
      (.update md (unchecked-byte (System/currentTimeMillis)))
      (try
        (let [p (System/getProperties)]
          (for-each [^String s (.stringPropertyNames p)]
            (.update md (.getBytes s))
            (.update md (.getBytes (.getProperty p s))))
          (let [names (.list (File. (.getProperty p "java.io.tmpdir")))]
            (when (some? names)
              (let [r (java.util.Random.)]
                (loop [^int count 0]
                  (when (< count (alength names))
                    (when (or (< count 512) (.nextBoolean r))
                      (.update md (.getBytes (aget names count))))
                    (when (<= count 1024)
                      (recur (unchecked-inc-int count)))))))))
        (catch Exception ex
          (.update md (unchecked-byte (.hashCode ex)))))
      (let [rt (Runtime/getRuntime)
            ^:mutable memBytes (SeedGenerator/longToByteArray (.totalMemory rt))]
        (.update md memBytes 0 (alength memBytes))
        (set! memBytes (SeedGenerator/longToByteArray (.freeMemory rt)))
        (.update md memBytes 0 (alength memBytes)))
      (.digest md))))
