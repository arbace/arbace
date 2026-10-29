package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"time"
)

func main() {
	mod := time.Date(2026, 1, 2, 3, 4, 5, 0, time.UTC)
	body := strings.NewReader("the quick brown fox")
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("ETag", `"v1"`)
		http.ServeContent(w, r, "fox.txt", mod, body)
	}))
	defer ts.Close()
	do := func(label string, hdr map[string]string) {
		req, _ := http.NewRequest("GET", ts.URL, nil)
		for k, v := range hdr {
			req.Header.Set(k, v)
		}
		res, err := http.DefaultClient.Do(req)
		if err != nil {
			fmt.Println(label, err)
			return
		}
		buf := make([]byte, 64)
		n, _ := res.Body.Read(buf)
		res.Body.Close()
		fmt.Printf("%-14s %d lm=%q ct=%q body=%q\n", label, res.StatusCode, res.Header.Get("Last-Modified"), res.Header.Get("Content-Type"), buf[:n])
	}
	do("plain", nil)
	do("if-mod-since", map[string]string{"If-Modified-Since": mod.Format(http.TimeFormat)})
	do("if-mod-old", map[string]string{"If-Modified-Since": mod.Add(-time.Hour).Format(http.TimeFormat)})
	do("if-none-match", map[string]string{"If-None-Match": `"v1"`})
	do("if-none-other", map[string]string{"If-None-Match": `"v2"`})
	do("if-match-fail", map[string]string{"If-Match": `"v9"`})
	do("range", map[string]string{"Range": "bytes=4-8"})
	do("if-range-ok", map[string]string{"Range": "bytes=0-2", "If-Range": `"v1"`})
	do("if-range-bad", map[string]string{"Range": "bytes=0-2", "If-Range": `"zzz"`})
}
