package main

import (
	"crypto/tls"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
)

func main() {
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "proto=%s major=%d tls=%v alpn=%q host-set=%v", r.Proto, r.ProtoMajor, r.TLS != nil, r.TLS.NegotiatedProtocol, r.Host != "")
	}))
	ts.EnableHTTP2 = true
	ts.Config.ErrorLog = log.New(io.Discard, "", 0)
	ts.StartTLS()
	defer ts.Close()
	res, err := ts.Client().Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(res.Proto, res.StatusCode, string(b))
	fmt.Println(res.TLS.Version == tls.VersionTLS13, res.TLS.NegotiatedProtocol, res.TLS.HandshakeComplete)
	// a client that does not offer h2 falls back to HTTP/1.1
	cfg := ts.Client().Transport.(*http.Transport).TLSClientConfig.Clone()
	cfg.NextProtos = []string{"http/1.1"}
	c1 := &http.Client{Transport: &http.Transport{TLSClientConfig: cfg}}
	res, err = c1.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(res.Proto, string(b))
	// plain TLS server without h2
	ts2 := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, r.Proto) }))
	ts2.Config.ErrorLog = log.New(io.Discard, "", 0)
	ts2.StartTLS()
	defer ts2.Close()
	res, _ = ts2.Client().Get(ts2.URL)
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("NewTLSServer:", res.Proto, string(b))
}
