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
	mux.HandleFunc("/tree/", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "tree:", r.URL.Path) })
	mux.HandleFunc("/tree/leaf", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "leaf") })
	mux.HandleFunc("/pub/{x}/more", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "wild:", r.PathValue("x")) })
	mux.HandleFunc("/pub/fixed/more", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "fixed") })
	mux.HandleFunc("example.test/host", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "host-specific") })
	mux.HandleFunc("/host", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "any-host") })
	func() {
		defer func() { fmt.Println("conflict:", strings.SplitN(fmt.Sprint(recover()), "\n", 2)[0][:40]) }()
		mux.HandleFunc("/pub/{y}/{z}", func(http.ResponseWriter, *http.Request) {})
		mux.HandleFunc("/pub/{a}/more", func(http.ResponseWriter, *http.Request) {})
	}()
	ts := httptest.NewServer(mux)
	defer ts.Close()
	c := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	for _, p := range []string{"/tree", "/tree/", "/tree/leaf", "/tree/leaf/x", "/pub/fixed/more", "/pub/other/more", "/host", "/a/../tree/leaf", "//tree/leaf"} {
		res, err := c.Get(ts.URL + p)
		if err != nil {
			fmt.Println(p, err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%-18s %d loc=%q %s\n", p, res.StatusCode, res.Header.Get("Location"), strings.TrimSpace(string(b)))
	}
	req, _ := http.NewRequest("GET", ts.URL+"/host", nil)
	req.Host = "example.test"
	res, _ := c.Do(req)
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("Host: example.test ->", string(b))
	r, _ := http.NewRequest("GET", "http://example.test/tree/leaf", nil)
	h, pat := mux.Handler(r)
	fmt.Println(pat, h != nil)
}
