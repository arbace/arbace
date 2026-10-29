package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"sort"
	"strings"
)

func main() {
	mux := http.NewServeMux()
	h := func(name string) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			var kv []string
			for _, k := range []string{"id", "name", "rest"} {
				if v := r.PathValue(k); v != "" {
					kv = append(kv, k+"="+v)
				}
			}
			sort.Strings(kv)
			fmt.Fprintf(w, "%s [%s] pattern=%q", name, strings.Join(kv, " "), r.Pattern)
		}
	}
	mux.HandleFunc("GET /items/{id}", h("get-item"))
	mux.HandleFunc("POST /items/{id}", h("post-item"))
	mux.HandleFunc("GET /items/{id}/tags/{name}", h("tag"))
	mux.HandleFunc("GET /files/{rest...}", h("files"))
	mux.HandleFunc("/exact/{$}", h("exact"))
	mux.HandleFunc("GET /items/new", h("new-item"))
	mux.HandleFunc("/", h("root"))
	ts := httptest.NewServer(mux)
	defer ts.Close()
	for _, c := range [][2]string{
		{"GET", "/items/42"}, {"POST", "/items/42"}, {"PUT", "/items/42"}, {"GET", "/items/new"},
		{"GET", "/items/7/tags/blue"}, {"GET", "/files/a/b/c.txt"}, {"GET", "/files/"},
		{"GET", "/exact/"}, {"GET", "/exact/more"}, {"HEAD", "/items/9"}, {"GET", "/other"},
	} {
		req, _ := http.NewRequest(c[0], ts.URL+c[1], nil)
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%-4s %-18s %d allow=%q %s\n", c[0], c[1], res.StatusCode, res.Header.Get("Allow"), strings.TrimSpace(string(b)))
	}
}
