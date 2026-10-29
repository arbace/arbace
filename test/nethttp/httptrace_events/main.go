package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"net/http/httptrace"
	"sync"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "traced") }))
	defer ts.Close()
	tr := &http.Transport{}
	c := &http.Client{Transport: tr}
	for round := 1; round <= 2; round++ {
		var mu sync.Mutex
		var ev []string
		add := func(s string) { mu.Lock(); ev = append(ev, s); mu.Unlock() }
		trace := &httptrace.ClientTrace{
			GetConn:              func(string) { add("GetConn") },
			GotConn:              func(i httptrace.GotConnInfo) { add(fmt.Sprintf("GotConn reused=%v wasidle=%v", i.Reused, i.WasIdle)) },
			PutIdleConn:          func(err error) { add(fmt.Sprintf("PutIdleConn err=%v", err)) },
			ConnectStart:         func(n, a string) { add("ConnectStart " + n) },
			ConnectDone:          func(n, a string, err error) { add(fmt.Sprintf("ConnectDone %s err=%v", n, err)) },
			WroteHeaderField:     func(k string, v []string) {},
			WroteHeaders:         func() { add("WroteHeaders") },
			WroteRequest:         func(i httptrace.WroteRequestInfo) { add(fmt.Sprintf("WroteRequest err=%v", i.Err)) },
			GotFirstResponseByte: func() { add("GotFirstResponseByte") },
		}
		req, _ := http.NewRequest("GET", ts.URL, nil)
		req = req.WithContext(httptrace.WithClientTrace(req.Context(), trace))
		res, err := c.Do(req)
		if err != nil {
			fmt.Println(err)
			return
		}
		io.Copy(io.Discard, res.Body)
		res.Body.Close()
		fmt.Println("round", round)
		mu.Lock()
		for _, e := range ev {
			fmt.Println("  ", e)
		}
		mu.Unlock()
	}
}
