package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/http/httputil"
	"strings"
)

func main() {
	req, _ := http.NewRequest("POST", "http://example.test/submit?a=1", strings.NewReader("name=go&v=1"))
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Set("X-Custom", "1")
	d, err := httputil.DumpRequestOut(req, true)
	fmt.Printf("out err=%v\n%s\n", err, strings.ReplaceAll(string(d), "\r\n", "|\n"))
	var seen string
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		d, _ := httputil.DumpRequest(r, true)
		seen = strings.ReplaceAll(string(d), r.Host, "HOST")
		w.Header().Set("X-Reply", "yes")
		w.Header().Set("Date", "Mon, 01 Jan 2026 00:00:00 GMT")
		fmt.Fprint(w, "reply body")
	}))
	defer ts.Close()
	r2, _ := http.NewRequest("PUT", ts.URL+"/thing", strings.NewReader("payload"))
	r2.Header.Set("Content-Type", "text/plain")
	res, _ := http.DefaultClient.Do(r2)
	fmt.Printf("in:\n%s\n", strings.ReplaceAll(seen, "\r\n", "|\n"))
	rd, _ := httputil.DumpResponse(res, true)
	fmt.Printf("response:\n%s\n", strings.ReplaceAll(string(rd), "\r\n", "|\n"))
	res.Body.Close()
	hd, _ := httputil.DumpRequest(r2, false)
	fmt.Println(strings.Contains(string(hd), "payload"))
}
