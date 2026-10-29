package main

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"net"
	"net/http"
	"sync/atomic"
)

func main() {
	var dials atomic.Int32
	srv := &http.Server{Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "in-process: host=%s path=%s", r.Host, r.URL.Path)
	})}
	ln := &pipeListener{conns: make(chan net.Conn), done: make(chan struct{})}
	go srv.Serve(ln)
	defer srv.Close()
	tr := &http.Transport{DialContext: func(ctx context.Context, network, addr string) (net.Conn, error) {
		dials.Add(1)
		c, s := net.Pipe()
		ln.conns <- s
		return c, nil
	}}
	c := &http.Client{Transport: tr}
	for _, p := range []string{"/one", "/two"} {
		res, err := c.Get("http://virtual.example" + p)
		if err != nil {
			fmt.Println(err)
			return
		}
		b, _ := io.ReadAll(bufio.NewReader(res.Body))
		res.Body.Close()
		fmt.Println(string(b))
	}
	fmt.Println("dials:", dials.Load())
}

type pipeListener struct {
	conns chan net.Conn
	done  chan struct{}
}

func (p *pipeListener) Accept() (net.Conn, error) {

	select {
	case c := <-p.conns:
		return c, nil
	case <-p.done:
		return nil, net.ErrClosed
	}
}
func (p *pipeListener) Close() error {

	select {
	case <-p.done:
	default:
		close(p.done)
	}
	return nil
}
func (p *pipeListener) Addr() net.Addr { return pipeAddr{} }

type pipeAddr struct{}

func (pipeAddr) Network() string { return "pipe" }
func (pipeAddr) String() string  { return "pipe" }
