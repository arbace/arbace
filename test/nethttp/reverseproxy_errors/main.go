package main

import (
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"net/http/httputil"
	"net/url"
)

func main() {
	l, _ := net.Listen("tcp", "127.0.0.1:0")
	dead := l.Addr().String()
	l.Close()
	target, _ := url.Parse("http://" + dead)
	rp := httputil.NewSingleHostReverseProxy(target)
	rp.ErrorLog = log.New(io.Discard, "", 0)
	front := httptest.NewServer(rp)
	defer front.Close()
	res, _ := http.Get(front.URL + "/x")
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Printf("default: %d %q\n", res.StatusCode, b)
	rp.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		w.WriteHeader(http.StatusServiceUnavailable)
		fmt.Fprintf(w, "custom handler: upstream down for %s", r.URL.Path)
	}
	res, _ = http.Get(front.URL + "/y")
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Printf("custom: %d %q\n", res.StatusCode, b)
	bad := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Length", "100")
		w.Write([]byte("short"))
	}))
	defer bad.Close()
	bu, _ := url.Parse(bad.URL)
	rp2 := httputil.NewSingleHostReverseProxy(bu)
	rp2.ErrorLog = log.New(io.Discard, "", 0)
	front2 := httptest.NewServer(rp2)
	defer front2.Close()
	res, err := http.Get(front2.URL)
	if err == nil {
		b, err = io.ReadAll(res.Body)
		fmt.Printf("truncated: %d %q err=%v\n", res.StatusCode, b, err)
	} else {
		fmt.Println("truncated: get error")
	}
}
