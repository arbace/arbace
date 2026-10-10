;; jrt: B1a's OSHost's random source (Host.RandomBytes; doc/go/JRT-NOTES.md, "Security"): Go's
;; crypto/rand, getrandom(2) on Linux. The entropy of SecureRandom (NativePRNG, the seeds of
;; SHA1PRNG and DRBG, jdk.internal.jrt.HostRandom) reaches the OS only through it; other systems
;; (TamaGo's, B1b) get their own host.
(in-ns 'go.arbace.jrt)

(go/file "hostrand_linux.go"
  :build ["//go:build linux"]
  :imports [[rand "crypto/rand"]])

(go/method RandomBytes [^OSHost h ^{:tag (slice byte)} b] (rand/Read b))
