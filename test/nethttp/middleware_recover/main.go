package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
)

func recoverer(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if v := recover(); v != nil {
				if v == http.ErrAbortHandler {
					panic(v)
				}
				http.Error(w, fmt.Sprint("recovered: ", v), 500)
			}
		}()
		next.ServeHTTP(w, r)
	})
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/ok", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "fine") })
	mux.HandleFunc("/boom", func(w http.ResponseWriter, r *http.Request) { panic("kaboom") })
	mux.HandleFunc("/nilmap", func(w http.ResponseWriter, r *http.Request) {
		var m map[string]int
		m["x"] = 1
	})
	mux.HandleFunc("/index", func(w http.ResponseWriter, r *http.Request) {
		var s []int
		_ = s[len(r.URL.Path)]
	})
	mux.HandleFunc("/abort", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("partial"))
		panic(http.ErrAbortHandler)
	})
	ts := httptest.NewServer(recoverer(mux))
	defer ts.Close()
	for _, p := range []string{"/ok", "/boom", "/nilmap", "/index", "/abort"} {
		res, err := http.Get(ts.URL + p)
		if err != nil {
			fmt.Println(p, "error:", err != nil)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		fmt.Printf("%s %d %q\n", p, res.StatusCode, b)
		res.Body.Close()
	}
}
