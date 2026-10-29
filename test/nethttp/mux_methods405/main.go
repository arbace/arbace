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
	mux.HandleFunc("GET /thing", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "got") })
	mux.HandleFunc("POST /thing", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "posted") })
	mux.HandleFunc("DELETE /thing/{id}", func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "deleted ", r.PathValue("id")) })
	ts := httptest.NewServer(mux)
	defer ts.Close()
	for _, c := range [][2]string{{"GET", "/thing"}, {"POST", "/thing"}, {"PUT", "/thing"}, {"PATCH", "/thing"}, {"DELETE", "/thing/9"}, {"GET", "/thing/9"}, {"GET", "/nothing"}} {
		req, _ := http.NewRequest(c[0], ts.URL+c[1], strings.NewReader(""))
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%-6s %-9s %d allow=%q %q\n", c[0], c[1], res.StatusCode, res.Header.Get("Allow"), strings.TrimSpace(string(b)))
	}
}
