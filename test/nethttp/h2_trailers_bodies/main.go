package main

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		w.Header().Set("Trailer", "X-Digest")
		w.Header().Set("X-Seen-Proto", r.Proto)
		w.WriteHeader(http.StatusAccepted)
		w.Write(bytes.ToUpper(b))
		w.(http.Flusher).Flush()
		w.Header().Set("X-Digest", fmt.Sprint(len(b)))
	}))
	ts.EnableHTTP2 = true
	ts.Config.ErrorLog = log.New(io.Discard, "", 0)
	ts.StartTLS()
	defer ts.Close()
	res, err := ts.Client().Post(ts.URL, "text/plain", strings.NewReader(strings.Repeat("h2 body ", 5000)))
	if err != nil {
		fmt.Println(err)
		return
	}
	n, _ := io.Copy(io.Discard, res.Body)
	res.Body.Close()
	fmt.Println(res.Proto, res.StatusCode, res.Header.Get("X-Seen-Proto"), n, res.Trailer.Get("X-Digest"), res.ContentLength)
	var pusher bool
	ts2 := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, pusher = w.(http.Pusher)
	}))
	ts2.EnableHTTP2 = true
	ts2.Config.ErrorLog = log.New(io.Discard, "", 0)
	ts2.StartTLS()
	defer ts2.Close()
	res, _ = ts2.Client().Get(ts2.URL)
	res.Body.Close()
	fmt.Println("Pusher available over h2:", pusher)
}
