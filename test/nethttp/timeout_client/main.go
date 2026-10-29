package main

import (
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"time"
)

func main() {
	release := make(chan struct{})
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/slow" {
			select {
			case <-release:
			case <-r.Context().Done():
			}
		}
		fmt.Fprint(w, "done")
	}))
	defer ts.Close()
	defer close(release)
	c := &http.Client{Timeout: 100 * time.Millisecond}
	_, err := c.Get(ts.URL + "/slow")
	var ue *url.Error
	fmt.Println(errors.As(err, &ue), ue.Timeout(), os.IsTimeout(err))
	var te interface{ Timeout() bool }
	fmt.Println(errors.As(err, &te) && te.Timeout())
	res, err := c.Get(ts.URL + "/fast")
	fmt.Println(res.StatusCode, err)
	res.Body.Close()
}
