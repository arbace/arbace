package main

import (
	"fmt"
	"io"
	"net/http"
	"strings"
)

type RoundTripperFunc func(*http.Request) (*http.Response, error)

func (f RoundTripperFunc) RoundTrip(r *http.Request) (*http.Response, error) { return f(r) }

type logging struct {
	next http.RoundTripper
	log  *[]string
}

func (l logging) RoundTrip(r *http.Request) (*http.Response, error) {
	*l.log = append(*l.log, r.Method+" "+r.URL.String())
	r.Header.Set("X-Injected", "yes")
	res, err := l.next.RoundTrip(r)
	if err == nil {
		res.Header.Set("X-Logged", "true")
	}
	return res, err
}

func main() {
	stub := RoundTripperFunc(func(r *http.Request) (*http.Response, error) {
		body := fmt.Sprintf("stub for %s injected=%s", r.URL.Path, r.Header.Get("X-Injected"))
		return &http.Response{
			StatusCode: 200, Status: "200 OK", Proto: "HTTP/1.1", ProtoMajor: 1, ProtoMinor: 1,
			Header: http.Header{"Content-Type": {"text/plain"}}, Body: io.NopCloser(strings.NewReader(body)),
			ContentLength: int64(len(body)), Request: r,
		}, nil
	})
	var log []string
	c := &http.Client{Transport: logging{next: stub, log: &log}}
	for _, u := range []string{"http://nowhere.invalid/a", "http://nowhere.invalid/b?q=1"} {
		res, err := c.Get(u)
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Println(res.StatusCode, res.Header.Get("X-Logged"), string(b))
	}
	fmt.Println(log)
	failing := &http.Client{Transport: RoundTripperFunc(func(r *http.Request) (*http.Response, error) { return nil, fmt.Errorf("synthetic failure") })}
	_, err := failing.Get("http://nowhere.invalid/")
	fmt.Println(err)
}
