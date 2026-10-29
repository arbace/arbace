package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"sort"
	"strings"
)

type echo struct{}

func (echo) ServeHTTP(w http.ResponseWriter, r *http.Request) { io.Copy(w, r.Body) }

type upper struct{ next http.Handler }

func (u upper) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	rec := httptest.NewRecorder()
	u.next.ServeHTTP(rec, r)
	w.WriteHeader(rec.Code)
	io.WriteString(w, strings.ToUpper(rec.Body.String()))
}

type stats struct{ hits map[string]int }

func (s *stats) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	s.hits[r.URL.Path]++
	var ks []string
	for k, v := range s.hits {
		ks = append(ks, fmt.Sprintf("%s:%d", k, v))
	}
	sort.Strings(ks)
	fmt.Fprint(w, strings.Join(ks, ","))
}

type status int

func (s status) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(int(s))
	fmt.Fprintf(w, "status %d %s", int(s), http.StatusText(int(s)))
}

func main() {
	mux := http.NewServeMux()
	mux.Handle("/echo", echo{})
	mux.Handle("/up", upper{echo{}})
	mux.Handle("/stats/", &stats{hits: map[string]int{}})
	mux.Handle("/teapot", status(418))
	mux.Handle("/nope", status(http.StatusNotFound))
	ts := httptest.NewServer(mux)
	defer ts.Close()
	do := func(method, p, body string) {
		req, _ := http.NewRequest(method, ts.URL+p, strings.NewReader(body))
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			fmt.Println(err)
			return
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%s %s -> %d %q\n", method, p, res.StatusCode, b)
	}
	do("POST", "/echo", "echo me")
	do("POST", "/up", "shout")
	do("GET", "/stats/a", "")
	do("GET", "/stats/a", "")
	do("GET", "/stats/b", "")
	do("GET", "/teapot", "")
	do("GET", "/nope", "")
	do("GET", "/missing", "")
}
