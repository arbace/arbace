package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"sync"
)

func counter() http.HandlerFunc {
	var mu sync.Mutex
	n := 0
	return func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		n++
		c := n
		mu.Unlock()
		fmt.Fprintf(w, "call %d", c)
	}
}

func get(url string) string {
	res, err := http.Get(url)
	if err != nil {
		return err.Error()
	}
	defer res.Body.Close()
	b, _ := io.ReadAll(res.Body)
	return string(b)
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/a", counter())
	mux.HandleFunc("/b", counter())
	greeting := "hello"
	mux.HandleFunc("/g", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "%s, %s", greeting, r.URL.Query().Get("who"))
	})
	ts := httptest.NewServer(mux)
	defer ts.Close()
	fmt.Println(get(ts.URL + "/a"))
	fmt.Println(get(ts.URL + "/a"))
	fmt.Println(get(ts.URL + "/b"))
	greeting = "hi"
	fmt.Println(get(ts.URL + "/g?who=go"))
}
