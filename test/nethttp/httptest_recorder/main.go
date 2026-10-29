package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func handler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("X-Method", r.Method)
	if r.URL.Query().Get("fail") != "" {
		w.WriteHeader(http.StatusBadRequest)
	}
	fmt.Fprintf(w, `{"path":%q,"body":%q}`, r.URL.Path, readBody(r))
}

func readBody(r *http.Request) string {
	if r.Body == nil {
		return ""
	}
	b, _ := io.ReadAll(r.Body)
	return string(b)
}

func main() {
	for _, tc := range []struct{ method, target, body string }{
		{"GET", "/a", ""}, {"POST", "/b?x=1", "payload"}, {"GET", "/c?fail=1", ""},
	} {
		req := httptest.NewRequest(tc.method, tc.target, strings.NewReader(tc.body))
		rec := httptest.NewRecorder()
		handler(rec, req)
		res := rec.Result()
		b, _ := io.ReadAll(res.Body)
		fmt.Println(rec.Code, res.Status, res.Header.Get("X-Method"), rec.Header().Get("Content-Type"), rec.Body.Len(), string(b))
	}
	req := httptest.NewRequest("GET", "http://example.com:8080/x", nil)
	fmt.Println(req.Host, req.RequestURI, req.URL.Port(), req.Proto, req.RemoteAddr)
	rec := httptest.NewRecorder()
	rec.WriteString("abc")
	rec.WriteHeader(404)
	rec.WriteHeader(500)
	fmt.Println(rec.Code, rec.Body.String(), rec.Flushed)
}
