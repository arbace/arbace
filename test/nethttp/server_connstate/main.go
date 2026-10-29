package main

import (
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"sort"
	"sync"
	"time"
)

func main() {
	var mu sync.Mutex
	seen := map[http.ConnState]int{}
	idle := make(chan struct{}, 8)
	closed := make(chan struct{}, 8)
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "x") }))
	ts.Config.ConnState = func(c net.Conn, s http.ConnState) {
		mu.Lock()
		seen[s]++
		mu.Unlock()
		switch s {
		case http.StateIdle:
			idle <- struct{}{}
		case http.StateClosed:
			closed <- struct{}{}
		}
	}
	ts.Start()
	defer ts.Close()
	tr := &http.Transport{}
	c := &http.Client{Transport: tr}
	for i := 0; i < 3; i++ {
		res, _ := c.Get(ts.URL)
		io.Copy(io.Discard, res.Body)
		res.Body.Close()
		<-idle
	}
	tr.CloseIdleConnections()
	select {
	case <-closed:
	case <-time.After(5 * time.Second):
		fmt.Println("timeout")
	}
	mu.Lock()
	defer mu.Unlock()
	var ks []string
	for s, n := range seen {
		ks = append(ks, fmt.Sprintf("%s=%d", s, n))
	}
	sort.Strings(ks)
	fmt.Println(ks)
}
