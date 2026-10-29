package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
)

type API struct {
	name  string
	calls map[string]int
}

func (a *API) Hello(w http.ResponseWriter, r *http.Request) {
	a.calls["hello"]++
	fmt.Fprintf(w, "%s: hello #%d", a.name, a.calls["hello"])
}

func (a API) Name(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "name=%s", a.name)
}

func Plain(w http.ResponseWriter, r *http.Request) { io.WriteString(w, "plain func") }

func main() {
	api := &API{name: "svc", calls: map[string]int{}}
	mux := http.NewServeMux()
	mux.HandleFunc("/hello", api.Hello)
	mux.HandleFunc("/name", api.Name)
	mux.HandleFunc("/plain", Plain)
	mux.HandleFunc("/expr", api.Hello2())
	mux.Handle("/hf", http.HandlerFunc(api.Hello))
	ts := httptest.NewServer(mux)
	defer ts.Close()
	api.name = "renamed"
	for _, p := range []string{"/hello", "/hello", "/name", "/plain", "/expr", "/hf"} {
		res, _ := http.Get(ts.URL + p)
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Println(p, string(b))
	}
}

func (a *API) Hello2() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) { fmt.Fprintf(w, "closure of %s", a.name) }
}
