package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/err", func(w http.ResponseWriter, r *http.Request) { http.Error(w, "custom failure", 503) })
	mux.HandleFunc("/nf", http.NotFound)
	mux.HandleFunc("/redir", func(w http.ResponseWriter, r *http.Request) { http.Redirect(w, r, "../up?x=1", 302) })
	mux.HandleFunc("/hdr", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Header().Set("X-Trace", "t")
		w.WriteHeader(http.StatusCreated)
		fmt.Fprint(w, `{"made":true}`)
	})
	mux.HandleFunc("/sniff", func(w http.ResponseWriter, r *http.Request) { io.WriteString(w, "<html><body>hi</body></html>") })
	mux.HandleFunc("/nobody", func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(http.StatusNoContent) })
	mux.HandleFunc("/1xx", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Link", "</style.css>; rel=preload")
		w.WriteHeader(http.StatusEarlyHints)
		fmt.Fprint(w, "final")
	})
	ts := httptest.NewServer(mux)
	defer ts.Close()
	c := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	for _, p := range []string{"/err", "/nf", "/redir", "/hdr", "/sniff", "/nobody", "/1xx"} {
		res, err := c.Get(ts.URL + p)
		if err != nil {
			fmt.Println(p, err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		loc := res.Header.Get("Location")
		if strings.HasPrefix(loc, ts.URL) {
			loc = "<base>" + strings.TrimPrefix(loc, ts.URL)
		}
		fmt.Printf("%-7s %d ct=%q xct=%q loc=%q body=%q\n", p, res.StatusCode, res.Header.Get("Content-Type"), res.Header.Get("X-Content-Type-Options"), loc, strings.TrimSpace(string(b)))
	}
	for _, code := range []int{200, 301, 404, 418, 500, 999} {
		fmt.Printf("%d=%q ", code, http.StatusText(code))
	}
	fmt.Println()
}
