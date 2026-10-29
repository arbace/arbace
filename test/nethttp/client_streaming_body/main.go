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
		fmt.Fprintf(w, "len=%d cl=%d te=%v data=%q", len(b), r.ContentLength, r.TransferEncoding, strings.ToUpper(string(b)))
	}))
	defer ts.Close()
	pr, pw := io.Pipe()
	go func() {
		for i := 0; i < 3; i++ {
			fmt.Fprintf(pw, "part%d;", i)
		}
		pw.Close()
	}()
	req, _ := http.NewRequest("POST", ts.URL, pr)
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(string(b))
	req, _ = http.NewRequest("POST", ts.URL, strings.NewReader("known length"))
	res, _ = http.DefaultClient.Do(req)
	b, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(string(b))
}
