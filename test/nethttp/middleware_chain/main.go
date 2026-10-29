package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
)

type Middleware func(http.Handler) http.Handler

func Chain(h http.Handler, ms ...Middleware) http.Handler {
	for i := len(ms) - 1; i >= 0; i-- {
		h = ms[i](h)
	}
	return h
}

var (
	mu  sync.Mutex
	log []string
)

func note(s string) { mu.Lock(); log = append(log, s); mu.Unlock() }

func tag(name string) Middleware {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			note("enter " + name)
			w.Header().Add("X-Order", name)
			next.ServeHTTP(w, r)
			note("leave " + name)
		})
	}
}

func auth(token string) Middleware {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.Header.Get("Authorization") != "Bearer "+token {
				note("auth denied")
				http.Error(w, "forbidden", http.StatusForbidden)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

type statusWriter struct {
	http.ResponseWriter
	code  int
	bytes int
}

func (s *statusWriter) WriteHeader(c int) { s.code = c; s.ResponseWriter.WriteHeader(c) }
func (s *statusWriter) Write(b []byte) (int, error) {
	if s.code == 0 {
		s.code = 200
	}
	n, err := s.ResponseWriter.Write(b)
	s.bytes += n
	return n, err
}

func access(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		sw := &statusWriter{ResponseWriter: w}
		next.ServeHTTP(sw, r)
		note(fmt.Sprintf("%s %s -> %d (%d bytes)", r.Method, r.URL.Path, sw.code, sw.bytes))
	})
}

func main() {
	app := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "secret data") })
	ts := httptest.NewServer(Chain(app, access, tag("A"), auth("t0ken"), tag("B")))
	defer ts.Close()
	for _, tok := range []string{"", "t0ken"} {
		req, _ := http.NewRequest("GET", ts.URL+"/data", nil)
		if tok != "" {
			req.Header.Set("Authorization", "Bearer "+tok)
		}
		res, _ := http.DefaultClient.Do(req)
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Println(res.StatusCode, strings.TrimSpace(string(b)), res.Header.Values("X-Order"))
	}
	mu.Lock()
	for _, l := range log {
		fmt.Println(l)
	}
	mu.Unlock()
}
