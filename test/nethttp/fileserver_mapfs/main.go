package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing/fstest"
)

func main() {
	fsys := fstest.MapFS{
		"index.html":      {Data: []byte("<h1>home</h1>")},
		"docs/a.txt":      {Data: []byte("alpha")},
		"docs/b.txt":      {Data: []byte("bravo bravo")},
		"docs/sub/c.json": {Data: []byte(`{"c":3}`)},
		"empty/.keep":     {Data: nil},
		"img/logo.svg":    {Data: []byte("<svg/>")},
		"no-index/readme": {Data: []byte("r")},
	}
	ts := httptest.NewServer(http.FileServerFS(fsys))
	defer ts.Close()
	c := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	for _, p := range []string{"/", "/index.html", "/docs/a.txt", "/docs/", "/docs", "/docs/sub/c.json", "/img/logo.svg", "/nope", "/no-index/", "/docs/b.txt?x=1", "/../etc/passwd"} {
		res, err := c.Get(ts.URL + p)
		if err != nil {
			fmt.Println(p, err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		body := strings.ReplaceAll(strings.TrimSpace(string(b)), "\n", "|")
		fmt.Printf("%-18s %d ct=%-28s loc=%-8q len=%s %s\n", p, res.StatusCode, res.Header.Get("Content-Type"), res.Header.Get("Location"), res.Header.Get("Content-Length"), body)
	}
}
