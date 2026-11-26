# arbace/pytls: a vendored crypto/tls with four OpenSSL features

A copy of Go's `crypto/tls`, importable as `arbace/pytls`, with its dependencies
inside GOROOT replaced, plus four TLS features Python's `ssl` (OpenSSL 3.5) has
and Go lacks. The module root is `arbace/` (`arbace/go.mod`: `module arbace`,
`go 1.27`), so that `arbace/pytls` sits where the repository's `go/arbace/` tree
puts its packages (`go/arbace/jrt` is imported as `arbace/jrt`).

`pytls-vendor.diff` (`diff -ruN upstream vendored`, the upstream files as copied, their import paths the originals) is every change against the
originals: the pristine side is the files copied unchanged, import paths
included; new files show as additions.

## Origin

All from https://github.com/usbarmory/tamago-go, tag `go1.27.1` (TamaGo's
go1.27.1), the GOROOT at `/root/tamago-go`, whose pin is SHA-256
`5745170fd3c83cb77ec88bc50aee8606f0436e718c222b48376b5943ac0cebe5`.
License: the Go license, `LICENSE-go` (GOROOT's `LICENSE`), and for
golang.org/x/crypto `LICENSE-x-crypto` (`src/vendor/golang.org/x/crypto/LICENSE`,
the same text).

| Here | From (GOROOT `src/`) | Files |
|---|---|---|
| `arbace/pytls` | `crypto/tls` | every non-test `.go` file but `defaults_boring.go` (`//go:build boringcrypto`) and `generate_cert.go` (`//go:build ignore`) |
| `arbace/pytls/internal/cryptobyte` (+ `asn1`) | `vendor/golang.org/x/crypto/cryptobyte` (x/crypto v0.52.1-0.20260526024921-9beb694f9766, per `vendor/modules.txt`) | all |
| `arbace/pytls/internal/chacha20poly1305` | `vendor/golang.org/x/crypto/chacha20poly1305` | `chacha20poly1305.go`, `chacha20poly1305_generic.go`, `chacha20poly1305_noasm.go`, `xchacha20poly1305.go`, `fips140only_go1.26.go` (no assembly, no `fips140only_compat.go`: `//go:build !go1.26`) |
| `arbace/pytls/internal/chacha20` | `vendor/golang.org/x/crypto/chacha20` | `chacha_generic.go`, `chacha_noasm.go`, `xor.go` |
| `arbace/pytls/internal/poly1305` | `vendor/golang.org/x/crypto/internal/poly1305` | `poly1305.go`, `sum_generic.go`, `mac_noasm.go` |
| `arbace/pytls/internal/alias` | `vendor/golang.org/x/crypto/internal/alias` | `alias.go`, `alias_purego.go` |
| `arbace/pytls/internal/tls13` | `crypto/internal/fips140/tls13` | `tls13.go` (not `cast.go`) |
| `arbace/pytls/internal/tls12` | `crypto/internal/fips140/tls12` | `tls12.go` (not `cast.go`) |
| `arbace/pytls/internal/fips140tls` | `crypto/tls/internal/fips140tls` | `fipstls.go` |
| `arbace/pytls/internal/boring`, `godebug`, `cpu` | new stubs | `boring.go`, `godebug.go`, `cpu.go` |

The copyright headers are kept. `defaults_fips140.go` is kept (unlike the
brief): it is `//go:build !boringcrypto`, so it is part of every normal build
and defines the FIPS filter lists the rest of the package refers to.

## (1) Import replacements and stubs

Imports (every file that has them; gofmt then re-sorted the import blocks, which
is most of the import lines in the diff):

- `golang.org/x/crypto/{cryptobyte,cryptobyte/asn1,chacha20poly1305,chacha20,internal/poly1305,internal/alias}`
  -> `arbace/pytls/internal/{cryptobyte,cryptobyte/asn1,chacha20poly1305,chacha20,poly1305,alias}`;
- `crypto/internal/fips140/tls13`, `.../tls12` -> `arbace/pytls/internal/tls13`, `.../tls12`;
- `crypto/tls/internal/fips140tls` -> `arbace/pytls/internal/fips140tls`;
- `crypto/internal/boring` -> `arbace/pytls/internal/boring`;
- `internal/godebug` -> `arbace/pytls/internal/godebug`; `internal/cpu` -> `arbace/pytls/internal/cpu`.

Edits:

- `chacha20/chacha_noasm.go`, `chacha20poly1305/chacha20poly1305_noasm.go`,
  `poly1305/mac_noasm.go`: the `//go:build` line removed (the assembly files are
  not copied, so the generic paths are used on every architecture).
- `internal/tls13/tls13.go`: over public `crypto/hkdf` (`Extract`, `Expand`
  return an error now: it panics, as the length limits it could report are
  checked by the callers) and `encoding/binary` (for `byteorder.BEAppendUint16`).
  Same API otherwise. Plus `ExternalBinderKey` (feature 3).
- `internal/tls12/tls12.go`: over public `crypto/hmac` (`newHMAC` adapts the
  generic hash constructor); the FIPS service-indicator recording in
  `MasterSecret` (`fips140.RecordNonApproved`) removed. Same API.
- `internal/fips140tls/fipstls.go`: no `init` forcing FIPS mode from
  `crypto/fips140.Enabled()`, so `Required()` is false unless `Force` is called;
  this copy's AES-GCM and KDFs are not the FIPS module's.
- `internal/boring/boring.go` (new): `Enabled = false`, `Unreachable()`, and
  `NewGCMTLS`/`NewGCMTLS13` that panic (never called).
- `internal/godebug/godebug.go` (new): `New(name)`, `Value()` always `""`,
  `IncNonDefault()` a no-op, `Name()`. `""` is Go 1.27's default of every
  setting crypto/tls reads (`tlsmlkem`, `tlssecpmlkem`, `tlssha1`, `fips140ems`,
  `tlsmaxrsasize`); `$GODEBUG` is not read.
- `internal/cpu/cpu.go` (new): the `X86`, `ARM64`, `S390X` fields
  `cipher_suites.go` reads, true on their own `runtime.GOARCH` (AES-GCM is
  assumed to have hardware support there; it only orders cipher preferences).
- `cipher_suites.go`: `aeadAESGCM` and `aeadAESGCMTLS13` use
  `crypto/cipher.NewGCM` instead of `gcm.NewGCMForTLS12`/`NewGCMForTLS13`
  (the nonce construction stays the existing `prefixNonceAEAD` (TLS 1.2) and
  `xorNonceAEAD` (TLS 1.3) wrappers; what is lost is the FIPS module's check
  that TLS 1.2 explicit nonces increase).
- `handshake_server_tls13.go`: `byteorder.LEUint32` -> `binary.LittleEndian.Uint32`.

## (2) The features

New files: `arbace_keylog_alerts.go`, `arbace_pha.go`, `arbace_psk.go`, and the
tests `arbace_features_test.go`. Edits to upstream files are marked `// arbace`.

### 1. Key log: EXPORTER_SECRET

- `common.go`: `keyLogLabelExporterSecret = "EXPORTER_SECRET"`.
- `handshake_client_tls13.go` (`readServerFinished`), `handshake_server_tls13.go`
  (`sendServerFinished`): before `c.ekm` is set, `c.writeKeyLogExporterSecret`
  (in `arbace_keylog_alerts.go`) logs the exporter_master_secret, computed only
  when `KeyLogWriter` is set. A TLS 1.3 connection logs 5 lines on each side.

### 2. Post-handshake client authentication

- `common.go`: `Config.PostHandshakeAuth` (client: offer PHA) and
  `Config.PostHandshakeClientAuth` (server: the deferred policy, as OpenSSL's
  SSL_VERIFY_POST_HANDSHAKE) (+ `Clone`).
- `handshake_messages.go`: `clientHelloMsg.postHandshakeAuth` (marshal: empty
  extension 49 before pre_shared_key; unmarshal; clone);
  `certificateRequestMsgTLS13.certificateRequestContext` and
  `certificateMsgTLS13.requestContext`, marshalled and parsed (they were always
  empty and a non-empty one was a parse error; an empty one parses as nil).
- `handshake_client_tls13.go`: in the handshake, a non-empty context in the
  server's CertificateRequest or Certificate is still a decode_error;
  `sendClientFinished` keeps the transcript through the client Finished when PHA
  was offered. `handshake_server_tls13.go`: the same check for the client's
  Certificate; the transcript kept after `readClientFinished` when the client
  offered PHA; `requestClientCert` is false when `PostHandshakeClientAuth` is not
  NoClientCert and the client offered PHA (the TLS 1.2 handshake is unchanged).
- `handshake_server.go`: `processCertsFromClient` is a wrapper of
  `processCertsFromClientAuth(certificate, clientAuth)`, whose two reads of
  `c.config.ClientAuth` read the parameter; the post-handshake exchange passes
  `PostHandshakeClientAuth`, or `ClientAuth` when that is NoClientCert.
- `handshake_client.go` (`makeClientHello`): the extension is sent when
  `Config.PostHandshakeAuth`, the maximum version is TLS 1.3, and not QUIC.
- `conn.go`: `Conn.pha`; `handlePostHandshakeMessage` reads the message's raw
  bytes (`rawHandshakeMessage` instead of nil) and dispatches a
  CertificateRequest (client that offered PHA) and a Certificate (server with a
  pending request); anything else stays unexpected_message.
- `arbace_pha.go`: `Conn.RequestClientCertificate`, the server's processing,
  the client's answer, the errors.

### 3. Pre-shared keys

- `common.go`: `Config.PSKClientCallback`, `PSKServerCallback`,
  `PSKIdentityHint` (+ `Clone`).
- `internal/tls13/tls13.go`: `EarlySecret.ExternalBinderKey` ("ext binder").
- `ticket.go`: `SessionState.externalPSK`, marking the session that stands for
  an external PSK in the client's TLS 1.3 handshake state.
- `handshake_client.go`: `clientHandshake` calls `loadExternalPSK` when no ticket
  was loaded, skips the ticket-cache cleanup for it, and drops it if TLS 1.2 is
  negotiated; `loadSession` ignores a cached session without peer certificates
  (one from a PSK handshake, which it would otherwise index out of range);
  `makeClientHello` drops PSK suites when there is no `PSKClientCallback`; the
  TLS 1.2 full handshake calls `hs.fullHandshake()` (PSK or not).
- `handshake_client_tls13.go` (`processHelloRetryRequest`): an external PSK's
  obfuscated_ticket_age stays 0.
- `handshake_server_tls13.go` (`checkForResumption`): `checkForExternalPSK`
  first (before the `SessionTicketsDisabled` check).
- `handshake_server.go`: `processClientHello` accepts having no certificate when
  `PSKServerCallback` is set (an empty `Certificate`: only PSK suites qualify);
  `cipherSuiteOk` accepts a PSK suite only with `PSKServerCallback`; the full
  handshake calls `hs.fullHandshake()`.
- `cipher_suites.go`: flag `suitePSK`; the five suites in `cipherSuites`, at the
  end of both preference orders, in `disabledCipherSuites` (used only when
  listed in `Config.CipherSuites`), and in `InsecureCipherSuites()` (in ID
  order), which is where Go lists every suite disabled by default.
- `arbace_psk.go`: the suite IDs, `loadExternalPSK`, `checkForExternalPSK`,
  `pskKeyAgreement` (RFC 4279), and the TLS 1.2 PSK full handshakes
  (`doFullHandshakePSK`, client and server: no Certificate, ServerKeyExchange
  only with a hint, ClientKeyExchange with the identity).

### 4. Alerts from the verification callbacks

- `handshake_client.go`, `handshake_server.go`, `handshake_client_tls13.go`,
  `handshake_server_tls13.go`: each `VerifyPeerCertificate`/`VerifyConnection`
  failure sends `callbackAlert(err)`: the `AlertError` the error is or wraps,
  else bad_certificate as before (9 sites).
- `handshake_client.go` (`verifyServerCertificate`): a server chain that does
  not verify sends `verifyErrorAlert(err)`: unknown_ca for an unknown issuer,
  certificate_expired for an expired certificate, else bad_certificate (the
  server side already did this for client chains; OpenSSL does it on both).
