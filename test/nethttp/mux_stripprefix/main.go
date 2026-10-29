package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	inner := http.NewServeMux()
	inner.HandleFunc("/ping", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "pong ", r.URL.Path) })
	inner.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "inner root ", r.URL.Path) })
	outer := http.NewServeMux()
	outer.Handle("/api/v1/", http.StripPrefix("/api/v1", inner))
	outer.Handle("/static/", http.StripPrefix("/static/", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "static file %q", r.URL.Path)
	})))
	outer.Handle("/gone", http.NotFoundHandler())
	outer.Handle("/old", http.RedirectHandler("/api/v1/ping", http.StatusMovedPermanently))
	outer.Handle("/nocontent", http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(204) }))
	ts := httptest.NewServer(outer)
	defer ts.Close()
	c := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	for _, p := range []string{"/api/v1/ping", "/api/v1/x/y", "/api/v1", "/static/css/a.css", "/gone", "/old", "/nocontent", "/zzz"} {
		res, err := c.Get(ts.URL + p)
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%-18s %d loc=%q %q\n", p, res.StatusCode, res.Header.Get("Location"), strings.TrimSpace(string(b)))
	}
}
