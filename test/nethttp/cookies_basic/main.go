package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"sort"
	"time"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/set" {
			http.SetCookie(w, &http.Cookie{Name: "session", Value: "abc123", Path: "/", HttpOnly: true, SameSite: http.SameSiteLaxMode})
			http.SetCookie(w, &http.Cookie{Name: "pref", Value: "dark mode", Path: "/app", MaxAge: 3600})
			http.SetCookie(w, &http.Cookie{Name: "gone", Value: "x", Expires: time.Unix(0, 0), Path: "/"})
			return
		}
		var names []string
		for _, c := range r.Cookies() {
			names = append(names, c.Name+"="+c.Value)
		}
		sort.Strings(names)
		fmt.Fprint(w, names)
	}))
	defer ts.Close()
	res, _ := http.Get(ts.URL + "/set")
	for _, c := range res.Cookies() {
		fmt.Printf("%s=%q path=%s httponly=%v maxage=%d samesite=%d expired=%v\n", c.Name, c.Value, c.Path, c.HttpOnly, c.MaxAge, c.SameSite, !c.Expires.IsZero() && c.Expires.Before(time.Now()))
	}
	res.Body.Close()
	req, _ := http.NewRequest("GET", ts.URL+"/read", nil)
	req.AddCookie(&http.Cookie{Name: "a", Value: "1"})
	req.AddCookie(&http.Cookie{Name: "b", Value: "2"})
	fmt.Println(req.Header.Get("Cookie"))
	res, _ = http.DefaultClient.Do(req)
	buf := make([]byte, 64)
	n, _ := res.Body.Read(buf)
	res.Body.Close()
	fmt.Println(string(buf[:n]))
	c, err := http.ParseSetCookie("id=7; Path=/x; Secure; Max-Age=10")
	fmt.Println(c.Name, c.Value, c.Path, c.Secure, c.MaxAge, err)
	fmt.Println((&http.Cookie{Name: "n", Value: "v w", Path: "/p", Domain: "example.com", Secure: true}).String())
}
