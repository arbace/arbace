package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"sort"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Add("X-Multi", "one")
		w.Header().Add("X-Multi", "two")
		var ks []string
		for k, v := range r.Header {
			ks = append(ks, k+": "+strings.Join(v, ","))
		}
		sort.Strings(ks)
		for _, k := range ks {
			fmt.Fprintln(w, k)
		}
		fmt.Fprintln(w, "host-set:", r.Host != "")
	}))
	defer ts.Close()
	req, _ := http.NewRequest("GET", ts.URL, nil)
	req.Header.Set("x-custom", "v1")
	req.Header.Add("Accept", "text/html")
	req.Header.Add("Accept", "application/json")
	req.Header.Set("User-Agent", "nethttp-corpus/1")
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer res.Body.Close()
	var sb strings.Builder
	buf := make([]byte, 512)
	for {
		n, err := res.Body.Read(buf)
		sb.Write(buf[:n])
		if err != nil {
			break
		}
	}
	fmt.Print(sb.String())
	fmt.Println(res.Header.Values("X-Multi"), res.Header["X-Multi"])
	fmt.Println(http.CanonicalHeaderKey("content-type"), http.CanonicalHeaderKey("x-forwarded-FOR"))
}
