package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"net/http/httputil"
	"net/url"
	"strings"
)

func main() {
	backend := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("X-Backend", "b1")
		w.Header().Set("Server-Timing", "internal")
		fmt.Fprintf(w, "backend saw %s %s fwd=%v xtag=%q body=%q", r.Method, r.URL.RequestURI(), r.Header.Get("X-Forwarded-For") != "", r.Header.Get("X-Tag"), readAll(r.Body))
	}))
	defer backend.Close()
	target, _ := url.Parse(backend.URL)
	rp := &httputil.ReverseProxy{
		Rewrite: func(pr *httputil.ProxyRequest) {
			pr.SetURL(target)
			pr.Out.URL.Path = strings.TrimPrefix(pr.In.URL.Path, "/proxy")
			pr.SetXForwarded()
			pr.Out.Header.Set("X-Tag", "rewritten")
		},
		ModifyResponse: func(res *http.Response) error {
			res.Header.Del("Server-Timing")
			res.Header.Set("X-Via", "proxy")
			return nil
		},
	}
	front := httptest.NewServer(rp)
	defer front.Close()
	res, err := http.Post(front.URL+"/proxy/items?id=5", "text/plain", strings.NewReader("data"))
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println(res.StatusCode, res.Header.Get("X-Backend"), res.Header.Get("X-Via"), res.Header.Get("Server-Timing") == "")
	fmt.Println(readAll(res.Body))
}

func readAll(r io.Reader) string {
	b, _ := io.ReadAll(r)
	return string(b)
}
