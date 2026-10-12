// Command tlsserver is a TLS test server on the host's loopback for Arbace's TLS checks
// (doc/go/BOX-NOTES.md, "TLS (option 2)"; bin/box-smoke, bin/arbace-go --smoke): it makes a test
// CA and a certificate for localhost and 127.0.0.1 at each start (nothing is kept), writes the
// CA's PEM, a second CA's that signed nothing (the wrong roots), and its two ports into DIR,
// then serves until killed or its standard input ends:
//
//	DIR/ca.pem, DIR/wrong-ca.pem, DIR/https.port, DIR/echo.port
//
// On https.port, HTTP/1.1 (ALPN http/1.1 only):
//
//	GET  /hello           "hello over TLS\n", with Content-Length
//	POST /echo            the request's body, with Content-Length
//	GET  /chunked?n=N     N bytes of the pattern (byte i is i % 251), chunked
//	GET  /big?n=N         the same with Content-Length
//	GET  /eof?n=N         the same, neither: the body ends with the connection
//	GET  /json            401 with a JSON error body, as an API refusing a request
//
// On echo.port, raw TLS: every byte read is written back until the client ends its output
// (close_notify), then the server closes.
package main

import (
	"bufio"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"fmt"
	"io"
	"log"
	"math/big"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"
)

func ca(name string) (*x509.Certificate, *ecdsa.PrivateKey) {
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		log.Fatal(err)
	}
	t := &x509.Certificate{
		SerialNumber:          big.NewInt(1),
		Subject:               pkix.Name{CommonName: name},
		NotBefore:             time.Now().Add(-time.Hour),
		NotAfter:              time.Now().Add(24 * time.Hour),
		IsCA:                  true,
		BasicConstraintsValid: true,
		KeyUsage:              x509.KeyUsageCertSign,
	}
	der, err := x509.CreateCertificate(rand.Reader, t, t, &key.PublicKey, key)
	if err != nil {
		log.Fatal(err)
	}
	c, _ := x509.ParseCertificate(der)
	return c, key
}

func writePEM(path string, der []byte) {
	b := pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: der})
	if err := os.WriteFile(path, b, 0o644); err != nil {
		log.Fatal(err)
	}
}

func pattern(n int) []byte {
	b := make([]byte, n)
	for i := range b {
		b[i] = byte(i % 251)
	}
	return b
}

func size(r *http.Request) int {
	n, _ := strconv.Atoi(r.URL.Query().Get("n"))
	return n
}

func main() {
	if len(os.Args) != 2 {
		fmt.Fprintln(os.Stderr, "usage: tlsserver DIR")
		os.Exit(2)
	}
	dir := os.Args[1]
	caCert, caKey := ca("Arbace test CA")
	wrong, _ := ca("Arbace wrong CA")
	key, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		log.Fatal(err)
	}
	leaf := &x509.Certificate{
		SerialNumber: big.NewInt(2),
		Subject:      pkix.Name{CommonName: "localhost"},
		DNSNames:     []string{"localhost"},
		IPAddresses:  []net.IP{net.ParseIP("127.0.0.1")},
		NotBefore:    time.Now().Add(-time.Hour),
		NotAfter:     time.Now().Add(24 * time.Hour),
		KeyUsage:     x509.KeyUsageDigitalSignature,
		ExtKeyUsage:  []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
	}
	der, err := x509.CreateCertificate(rand.Reader, leaf, caCert, &key.PublicKey, caKey)
	if err != nil {
		log.Fatal(err)
	}
	cert := tls.Certificate{Certificate: [][]byte{der, caCert.Raw}, PrivateKey: key}
	cfg := &tls.Config{Certificates: []tls.Certificate{cert}, NextProtos: []string{"http/1.1"}}

	mux := http.NewServeMux()
	mux.HandleFunc("/hello", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Length", "15")
		io.WriteString(w, "hello over TLS\n")
	})
	mux.HandleFunc("/echo", func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		w.Header().Set("Content-Length", strconv.Itoa(len(b)))
		w.Write(b)
	})
	mux.HandleFunc("/chunked", func(w http.ResponseWriter, r *http.Request) {
		b := pattern(size(r))
		for len(b) > 0 {
			k := min(len(b), 10000)
			w.Write(b[:k])
			w.(http.Flusher).Flush()
			b = b[k:]
		}
	})
	mux.HandleFunc("/big", func(w http.ResponseWriter, r *http.Request) {
		b := pattern(size(r))
		w.Header().Set("Content-Length", strconv.Itoa(len(b)))
		w.Write(b)
	})
	mux.HandleFunc("/eof", func(w http.ResponseWriter, r *http.Request) {
		c, bw, err := w.(http.Hijacker).Hijack()
		if err != nil {
			return
		}
		defer c.Close()
		bw.WriteString("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nConnection: close\r\n\r\n")
		bw.Write(pattern(size(r)))
		bw.Flush()
	})
	mux.HandleFunc("/json", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(401)
		io.WriteString(w, `{"type":"error","error":{"type":"authentication_error","message":"no key"}}`)
	})

	hl, err := tls.Listen("tcp", "127.0.0.1:0", cfg)
	if err != nil {
		log.Fatal(err)
	}
	el, err := tls.Listen("tcp", "127.0.0.1:0", cfg)
	if err != nil {
		log.Fatal(err)
	}
	go (&http.Server{Handler: mux, TLSNextProto: map[string]func(*http.Server, *tls.Conn, http.Handler){}}).Serve(hl)
	go func() {
		for {
			c, err := el.Accept()
			if err != nil {
				return
			}
			go func() {
				defer c.Close()
				io.Copy(c, c)
			}()
		}
	}()
	writePEM(filepath.Join(dir, "ca.pem"), caCert.Raw)
	writePEM(filepath.Join(dir, "wrong-ca.pem"), wrong.Raw)
	os.WriteFile(filepath.Join(dir, "echo.port"), []byte(strconv.Itoa(el.Addr().(*net.TCPAddr).Port)), 0o644)
	os.WriteFile(filepath.Join(dir, "https.port"), []byte(strconv.Itoa(hl.Addr().(*net.TCPAddr).Port)), 0o644)
	io.Copy(io.Discard, bufio.NewReader(os.Stdin))
}
