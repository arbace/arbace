package main

import (
	"fmt"
	"net/http"
	"net/http/cookiejar"
	"net/http/httptest"
	"net/url"
	"sort"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/login":
			http.SetCookie(w, &http.Cookie{Name: "sid", Value: "s-1", Path: "/"})
			http.SetCookie(w, &http.Cookie{Name: "admin", Value: "yes", Path: "/admin"})
			fmt.Fprint(w, "logged in")
		default:
			var ns []string
			for _, c := range r.Cookies() {
				ns = append(ns, c.Name+"="+c.Value)
			}
			sort.Strings(ns)
			fmt.Fprintf(w, "%s sees %v", r.URL.Path, ns)
		}
	}))
	defer ts.Close()
	jar, _ := cookiejar.New(nil)
	c := &http.Client{Jar: jar}
	get := func(p string) {
		res, err := c.Get(ts.URL + p)
		if err != nil {
			fmt.Println(err)
			return
		}
		b := make([]byte, 128)
		n, _ := res.Body.Read(b)
		res.Body.Close()
		fmt.Println(string(b[:n]))
	}
	get("/before")
	get("/login")
	get("/after")
	get("/admin/panel")
	get("/administrator")
	u, _ := url.Parse(ts.URL)
	cs := jar.Cookies(u)
	fmt.Println(len(cs))
	jar.SetCookies(u, []*http.Cookie{{Name: "extra", Value: "e", Path: "/"}})
	get("/again")
	jar.SetCookies(u, []*http.Cookie{{Name: "sid", Value: "", Path: "/", MaxAge: -1}})
	get("/removed")
}
