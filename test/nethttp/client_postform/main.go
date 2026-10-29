package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"sort"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if err := r.ParseForm(); err != nil {
			http.Error(w, err.Error(), 400)
			return
		}
		keys := make([]string, 0)
		for k := range r.PostForm {
			keys = append(keys, k)
		}
		sort.Strings(keys)
		for _, k := range keys {
			fmt.Fprintf(w, "%s=%q\n", k, r.PostForm[k])
		}
		fmt.Fprintf(w, "ct=%s query-a=%s\n", r.Header.Get("Content-Type"), r.URL.Query().Get("a"))
	}))
	defer ts.Close()
	res, err := http.PostForm(ts.URL+"/?a=q", url.Values{"name": {"Ann Lee"}, "tags": {"x", "y&z"}, "empty": {""}})
	if err != nil {
		fmt.Println("error:", err)
		return
	}
	defer res.Body.Close()
	buf := make([]byte, 1024)
	n, _ := res.Body.Read(buf)
	fmt.Print(string(buf[:n]))
}
