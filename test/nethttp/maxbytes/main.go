package main

import (
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		r.Body = http.MaxBytesReader(w, r.Body, 16)
		b, err := io.ReadAll(r.Body)
		var mbe *http.MaxBytesError
		if errors.As(err, &mbe) {
			http.Error(w, fmt.Sprintf("too large (limit %d, read %d)", mbe.Limit, len(b)), http.StatusRequestEntityTooLarge)
			return
		}
		fmt.Fprintf(w, "ok %d bytes", len(b))
	}))
	defer ts.Close()
	for _, n := range []int{5, 16, 17, 1000} {
		res, err := http.Post(ts.URL, "text/plain", strings.NewReader(strings.Repeat("x", n)))
		if err != nil {
			fmt.Println(n, "error", err != nil)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Printf("%4d -> %d %s\n", n, res.StatusCode, strings.TrimSpace(string(b)))
	}
}
