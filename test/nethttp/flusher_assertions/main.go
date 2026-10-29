package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, fl := w.(http.Flusher)
		_, hj := w.(http.Hijacker)
		_, rf := w.(io.ReaderFrom)
		_, pu := w.(http.Pusher)
		_, cn := w.(http.CloseNotifier)
		_, sw := w.(io.StringWriter)
		_, ww := w.(io.Writer)
		fmt.Fprintf(w, "Flusher=%v Hijacker=%v ReaderFrom=%v Pusher=%v CloseNotifier=%v StringWriter=%v Writer=%v proto=%s", fl, hj, rf, pu, cn, sw, ww, r.Proto)
	}))
	defer ts.Close()
	res, _ := http.Get(ts.URL)
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(string(b))
	rec := httptest.NewRecorder()
	var w http.ResponseWriter = rec
	_, fl := w.(http.Flusher)
	_, hj := w.(http.Hijacker)
	fmt.Println("recorder Flusher:", fl, "Hijacker:", hj)
}
