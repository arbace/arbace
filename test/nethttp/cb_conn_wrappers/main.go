// NH3 (doc/go/NET-HTTP-SURVEY.md, "As built: NH2 (write-ahead)"): connections of the program's own types
// under compiled net/http: an http.Server serving a net.Listener wrapper (Accept, Close, Addr) whose
// net.Conn wrapper counts bytes (Read, Write, Close, LocalAddr, RemoteAddr, SetDeadline and its read
// and write halves called by the server), a Transport whose DialContext returns such a wrapper, the
// Server's ConnState hook and an ErrorLog over a Writer of the program's.
package main

import (
	"context"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"strings"
	"sync"
	"sync/atomic"
)

type counted struct {
	net.Conn
	r, w *atomic.Int64
}

func (c counted) Read(p []byte) (int, error) {
	n, err := c.Conn.Read(p)
	c.r.Add(int64(n))
	return n, err
}

func (c counted) Write(p []byte) (int, error) {
	n, err := c.Conn.Write(p)
	c.w.Add(int64(n))
	return n, err
}

type listener struct {
	net.Listener
	accepted *atomic.Int64
	r, w     *atomic.Int64
}

func (l listener) Accept() (net.Conn, error) {
	c, err := l.Listener.Accept()
	if err != nil {
		return nil, err
	}
	l.accepted.Add(1)
	return counted{c, l.r, l.w}, nil
}

type lines struct {
	mu sync.Mutex
	n  int
}

func (l *lines) Write(p []byte) (int, error) {
	l.mu.Lock()
	l.n += strings.Count(string(p), "\n")
	l.mu.Unlock()
	return len(p), nil
}

func main() {
	inner, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		fmt.Println("unsupported: no loopback:", err)
		return
	}
	var accepted, sr, sw, cr, cw atomic.Int64
	ln := listener{inner, &accepted, &sr, &sw}
	var mu sync.Mutex
	states := map[http.ConnState]int{}
	errs := &lines{}
	srv := &http.Server{
		Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.URL.Path == "/panic" {
				panic("handler panics")
			}
			fmt.Fprintf(w, "hello %s", strings.Repeat("x", 100))
		}),
		ConnState: func(c net.Conn, s http.ConnState) {
			mu.Lock()
			states[s]++
			mu.Unlock()
		},
		ErrorLog: log.New(errs, "", 0),
	}
	done := make(chan error, 1)
	go func() { done <- srv.Serve(ln) }()
	tr := &http.Transport{
		// one connection a request: the counts do not depend on keep-alive or a retry
		DisableKeepAlives: true,
		DialContext: func(ctx context.Context, network, addr string) (net.Conn, error) {
			var d net.Dialer
			c, err := d.DialContext(ctx, network, addr)
			if err != nil {
				return nil, err
			}
			return counted{c, &cr, &cw}, nil
		},
	}
	client := &http.Client{Transport: tr}
	base := "http://" + ln.Addr().String()
	for i := 0; i < 3; i++ {
		res, err := client.Get(base + "/hello")
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Println(res.StatusCode, len(b))
	}
	_, err = client.Get(base + "/panic")
	fmt.Println("panic seen by the client:", err != nil)
	tr.CloseIdleConnections()
	srv.Shutdown(context.Background())
	fmt.Println("serve:", <-done)
	fmt.Println("accepted:", accepted.Load())
	fmt.Println("server read and wrote:", sr.Load() > 0, sw.Load() > 0, "client read and wrote:", cr.Load() > 0, cw.Load() > 0)
	mu.Lock()
	fmt.Println("states: new", states[http.StateNew], "active", states[http.StateActive])
	mu.Unlock()
	fmt.Println("error log lines:", errs.n > 0)
}
