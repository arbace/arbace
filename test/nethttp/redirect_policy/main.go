package main

import (
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
)

func main() {
	hits := 0
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		hits++
		if r.URL.Path == "/loop" {
			http.Redirect(w, r, "/loop", http.StatusFound)
			return
		}
		http.Redirect(w, r, "/elsewhere", http.StatusTemporaryRedirect)
	}))
	defer ts.Close()
	_, err := http.Get(ts.URL + "/loop")
	var ue *url.Error
	errors.As(err, &ue)
	fmt.Println(ue != nil, hits)
	fmt.Println(err.Error()[len(err.Error())-30:])
	c := &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	hits = 0
	res, err := c.Get(ts.URL + "/a")
	fmt.Println(res.StatusCode, res.Header.Get("Location"), err, hits)
	res.Body.Close()
	c = &http.Client{CheckRedirect: func(req *http.Request, via []*http.Request) error {
		return fmt.Errorf("stopped after %d", len(via))
	}}
	_, err = c.Get(ts.URL + "/a")
	fmt.Println(errors.Unwrap(err))
}
