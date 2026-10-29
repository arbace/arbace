package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
)

func main() {
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "proto=%s tls=%v", r.Proto, r.TLS != nil)
	}))
	var p http.Protocols
	p.SetHTTP1(true)
	p.SetUnencryptedHTTP2(true)
	ts.Config.Protocols = &p
	ts.Start()
	defer ts.Close()
	tr := &http.Transport{}
	var cp http.Protocols
	cp.SetUnencryptedHTTP2(true)
	tr.Protocols = &cp
	c := &http.Client{Transport: tr}
	res, err := c.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("h2c client:", res.Proto, string(b))
	res, err = http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("http1 client:", res.Proto, string(b))
	fmt.Println(p.HTTP1(), p.HTTP2(), p.UnencryptedHTTP2(), cp.String())
}
