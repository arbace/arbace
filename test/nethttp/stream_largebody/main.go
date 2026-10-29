package main

import (
	"crypto/sha256"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

type gen struct{ n, i int }

func (g *gen) Read(p []byte) (int, error) {
	if g.i >= g.n {
		return 0, io.EOF
	}
	k := 0
	for k < len(p) && g.i < g.n {
		p[k] = byte('a' + (g.i*7+g.i/13)%26)
		k++
		g.i++
	}
	return k, nil
}

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		sum := sha256.Sum256(b)
		w.Header().Set("X-Sum", fmt.Sprintf("%x", sum[:8]))
		w.Header().Set("X-N", fmt.Sprint(len(b)))
		w.Write(b)
	}))
	defer ts.Close()
	const size = 300 << 10
	h := sha256.New()
	io.Copy(h, &gen{n: size})
	res, err := http.Post(ts.URL, "application/octet-stream", &gen{n: size})
	if err != nil {
		fmt.Println(err)
		return
	}
	hr := sha256.New()
	rn, _ := io.Copy(hr, res.Body)
	res.Body.Close()
	fmt.Println("sent", size, "echoed", rn, "match:", fmt.Sprintf("%x", h.Sum(nil)) == fmt.Sprintf("%x", hr.Sum(nil)))
	fmt.Println(strings.Repeat("-", 3), res.ContentLength, res.Header.Get("X-N"), res.Header.Get("X-Sum"))
}
