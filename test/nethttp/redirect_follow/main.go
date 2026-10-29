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
	mux.HandleFunc("/start", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/hop1", http.StatusMovedPermanently)
	})
	mux.HandleFunc("/hop1", func(w http.ResponseWriter, r *http.Request) { http.Redirect(w, r, "/hop2?from=hop1", http.StatusFound) })
	mux.HandleFunc("/hop2", func(w http.ResponseWriter, r *http.Request) { http.Redirect(w, r, "/final", http.StatusSeeOther) })
	mux.HandleFunc("/final", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "final via %s referer=%v", r.Method, r.Referer() != "")
	})
	ts := httptest.NewServer(mux)
	defer ts.Close()
	var chain []string
	c := &http.Client{CheckRedirect: func(req *http.Request, via []*http.Request) error {
		chain = append(chain, fmt.Sprintf("%s->%s(%d)", via[len(via)-1].URL.Path, req.URL.Path, req.Response.StatusCode))
		return nil
	}}
	res, err := c.Get(ts.URL + "/start")
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(strings.Join(chain, " "))
	fmt.Println(res.StatusCode, res.Request.URL.Path, res.Request.URL.RawQuery, string(b))
	loc, err := res.Location()
	fmt.Println(loc, err)
}
