package main

import (
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
)

func main() {
	var conns atomic.Int32
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprint(w, "ok")
	}))
	ts.Config.ConnState = func(c net.Conn, s http.ConnState) {
		if s == http.StateNew {
			conns.Add(1)
		}
	}
	ts.Start()
	defer ts.Close()
	run := func(name string, tr *http.Transport) {
		conns.Store(0)
		c := &http.Client{Transport: tr}
		for i := 0; i < 5; i++ {
			res, err := c.Get(ts.URL)
			if err != nil {
				fmt.Println(err)
				return
			}
			io.Copy(io.Discard, res.Body)
			res.Body.Close()
		}
		tr.CloseIdleConnections()
		fmt.Println(name, "connections:", conns.Load())
	}
	run("keepalive", &http.Transport{MaxIdleConns: 2})
	run("disabled", &http.Transport{DisableKeepAlives: true})
	run("zero-idle", &http.Transport{MaxIdleConnsPerHost: -1})
}
