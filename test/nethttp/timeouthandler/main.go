package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"time"
)

func main() {
	release := make(chan struct{})
	slow := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/slow" {
			select {
			case <-r.Context().Done():
				return
			case <-release:
			}
		}
		fmt.Fprint(w, "in time")
	})
	ts := httptest.NewServer(http.TimeoutHandler(slow, 100*time.Millisecond, "timed out!"))
	defer ts.Close()
	defer close(release)
	for _, p := range []string{"/fast", "/slow"} {
		res, err := http.Get(ts.URL + p)
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Println(p, res.StatusCode, strings.TrimSpace(string(b)))
	}
}
