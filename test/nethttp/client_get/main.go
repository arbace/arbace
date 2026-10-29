package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
		w.Header().Set("X-Seen-Method", r.Method)
		fmt.Fprintf(w, "path=%s query=%s", r.URL.Path, r.URL.RawQuery)
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL + "/a/b?x=1&y=2")
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer res.Body.Close()
	body, _ := io.ReadAll(res.Body)
	fmt.Println(res.Status, res.StatusCode, res.Proto, res.ContentLength)
	fmt.Println(res.Header.Get("Content-Type"), res.Header.Get("X-Seen-Method"))
	fmt.Println(string(body))
	res2, err := http.Head(ts.URL + "/h")
	fmt.Println(res2.StatusCode, res2.ContentLength, err)
}
