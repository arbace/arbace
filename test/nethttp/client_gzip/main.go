package main

import (
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	payload := strings.Repeat("compressible ", 100)
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if strings.Contains(r.Header.Get("Accept-Encoding"), "gzip") {
			w.Header().Set("Content-Encoding", "gzip")
			zw := gzip.NewWriter(w)
			io.WriteString(zw, payload)
			zw.Close()
			return
		}
		io.WriteString(w, "plain:"+payload)
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("transparent:", res.Uncompressed, len(b) == len(payload), res.Header.Get("Content-Encoding") == "", res.ContentLength)
	tr := &http.Transport{DisableCompression: true}
	c := &http.Client{Transport: tr}
	req, _ := http.NewRequest("GET", ts.URL, nil)
	req.Header.Set("Accept-Encoding", "gzip")
	res, _ = c.Do(req)
	raw, _ := io.ReadAll(res.Body)
	res.Body.Close()
	zr, err := gzip.NewReader(bytes.NewReader(raw))
	if err != nil {
		fmt.Println(err)
		return
	}
	un, _ := io.ReadAll(zr)
	fmt.Println("manual:", res.Uncompressed, res.Header.Get("Content-Encoding"), string(un) == payload)
	res, _ = c.Get(ts.URL)
	raw, _ = io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("no accept:", len(raw) == len(payload)+6)
}
