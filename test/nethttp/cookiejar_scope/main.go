package main

import (
	"fmt"
	"net/http"
	"net/http/cookiejar"
	"net/url"
	"sort"
	"time"
)

func show(j *cookiejar.Jar, raw string) {
	u, _ := url.Parse(raw)
	var ns []string
	for _, c := range j.Cookies(u) {
		ns = append(ns, c.Name+"="+c.Value)
	}
	sort.Strings(ns)
	fmt.Printf("%-34s %v\n", raw, ns)
}

func main() {
	j, _ := cookiejar.New(nil)
	a, _ := url.Parse("http://www.example.com/dir/page")
	j.SetCookies(a, []*http.Cookie{
		{Name: "host-only", Value: "1"},
		{Name: "domain", Value: "2", Domain: "example.com", Path: "/"},
		{Name: "secure", Value: "3", Secure: true, Path: "/"},
		{Name: "deep", Value: "4", Path: "/dir/sub"},
		{Name: "old", Value: "5", Expires: time.Now().Add(-time.Hour), Path: "/"},
		{Name: "bad", Value: "6", Domain: "other.com", Path: "/"},
	})
	show(j, "http://www.example.com/dir/page")
	show(j, "http://www.example.com/dir/sub/x")
	show(j, "http://www.example.com/")
	show(j, "https://www.example.com/dir/page")
	show(j, "http://api.example.com/dir/page")
	show(j, "http://example.com/")
	show(j, "http://other.com/")
	j2, _ := cookiejar.New(&cookiejar.Options{})
	b, _ := url.Parse("http://127.0.0.1:1234/")
	j2.SetCookies(b, []*http.Cookie{{Name: "ip", Value: "v", Path: "/"}})
	show(j2, "http://127.0.0.1:9999/x")
}
