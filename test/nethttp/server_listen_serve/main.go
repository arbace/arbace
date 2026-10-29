package main

import (
	"context"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"strings"
)

type store struct {
	items map[string]string
	order []string
}

func (s *store) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	key := strings.TrimPrefix(r.URL.Path, "/kv/")
	switch r.Method {
	case http.MethodPut:
		b, _ := io.ReadAll(r.Body)
		if _, ok := s.items[key]; !ok {
			s.order = append(s.order, key)
		}
		s.items[key] = string(b)
		w.WriteHeader(http.StatusNoContent)
	case http.MethodGet:
		if key == "" {
			fmt.Fprintln(w, strings.Join(s.order, ","))
			return
		}
		v, ok := s.items[key]
		if !ok {
			http.NotFound(w, r)
			return
		}
		fmt.Fprint(w, v)
	case http.MethodDelete:
		delete(s.items, key)
		w.WriteHeader(http.StatusNoContent)
	default:
		w.Header().Set("Allow", "GET, PUT, DELETE")
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
	}
}

func main() {
	l, _ := net.Listen("tcp", "127.0.0.1:0")
	srv := &http.Server{Handler: &store{items: map[string]string{}}}
	go srv.Serve(l)
	defer srv.Shutdown(context.Background())
	base := "http://" + l.Addr().String() + "/kv/"
	do := func(method, key, body string) {
		u := base + url.PathEscape(key)
		req, _ := http.NewRequest(method, u, strings.NewReader(body))
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			fmt.Println(err)
			return
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%-6s %-4s -> %d %q\n", method, key, res.StatusCode, strings.TrimSpace(string(b)))
	}
	do("PUT", "a", "apple")
	do("PUT", "b", "banana")
	do("GET", "a", "")
	do("GET", "", "")
	do("DELETE", "a", "")
	do("GET", "a", "")
	do("POST", "b", "x")
	do("PUT", "a", "avocado")
	do("GET", "", "")
}
