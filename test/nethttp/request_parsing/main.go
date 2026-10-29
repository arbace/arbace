package main

import (
	"bufio"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
)

func main() {
	raw := "POST /search?q=go+lang&lang=en HTTP/1.1\r\nHost: example.test:8080\r\nUser-Agent: raw/1\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 22\r\nCookie: a=1; b=2\r\nX-Forwarded-For: 10.0.0.1, 10.0.0.2\r\n\r\nname=Ann&age=33&q=body"
	req, err := http.ReadRequest(bufio.NewReader(strings.NewReader(raw)))
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println(req.Method, req.URL.Path, req.URL.RawQuery, req.Proto, req.Host, req.ContentLength)
	fmt.Println(req.URL.Query()["q"], req.Header.Get("x-forwarded-for"))
	fmt.Println(req.UserAgent(), len(req.Cookies()), req.ProtoAtLeast(1, 0))
	b, _ := io.ReadAll(req.Body)
	fmt.Printf("%q\n", b)
	req.Body = io.NopCloser(strings.NewReader(string(b)))
	req.ParseForm()
	fmt.Println(req.Form["q"], req.PostForm["q"], req.FormValue("name"), req.FormValue("lang"))
	_, err = http.ReadRequest(bufio.NewReader(strings.NewReader("BROKEN\r\n\r\n")))
	fmt.Println(err)
	u, _ := url.Parse("https://user:pw@host.test:9000/a%20b/c?x=1&x=2#frag")
	fmt.Println(u.Scheme, u.User.Username(), u.Hostname(), u.Port(), u.Path, u.EscapedPath(), u.Query()["x"], u.Fragment)
	r2, _ := http.NewRequest("GET", "http://host.test/p?z=1", nil)
	r2.Header.Add("A", "1")
	r2c := r2.Clone(r2.Context())
	r2c.Header.Add("A", "2")
	fmt.Println(r2.Header["A"], r2c.Header["A"])
	var sb strings.Builder
	r2.Write(&sb)
	fmt.Printf("%q\n", sb.String())
	res := &http.Response{StatusCode: 404, ProtoMajor: 1, ProtoMinor: 1, Header: http.Header{"X": {"y"}}, Body: io.NopCloser(strings.NewReader("gone")), ContentLength: 4}
	sb.Reset()
	res.Write(&sb)
	fmt.Printf("%q\n", sb.String())
}
