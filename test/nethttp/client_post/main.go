package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		fmt.Fprintf(w, "%s %s ct=%q len=%d body=%q", r.Method, r.URL.Path, r.Header.Get("Content-Type"), r.ContentLength, b)
	}))
	defer ts.Close()
	res, err := http.Post(ts.URL+"/p", "application/json", strings.NewReader(`{"a":1}`))
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(string(b))
	req, _ := http.NewRequest(http.MethodPut, ts.URL+"/put", strings.NewReader("hello"))
	req.Header.Set("Content-Type", "text/plain")
	res, err = http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(string(b))
	req, _ = http.NewRequest(http.MethodDelete, ts.URL+"/del", nil)
	res, _ = http.DefaultClient.Do(req)
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(string(b))
}
