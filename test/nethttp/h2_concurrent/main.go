package main

import (
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"sync"
)

func main() {
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "stream %s h%d", r.URL.Query().Get("n"), r.ProtoMajor)
	}))
	ts.EnableHTTP2 = true
	ts.Config.ErrorLog = log.New(io.Discard, "", 0)
	ts.StartTLS()
	defer ts.Close()
	client := ts.Client()
	const N = 60
	out := make([]string, N)
	var wg sync.WaitGroup
	for i := 0; i < N; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			res, err := client.Get(fmt.Sprintf("%s/?n=%d", ts.URL, i))
			if err != nil {
				out[i] = err.Error()
				return
			}
			b, _ := io.ReadAll(res.Body)
			res.Body.Close()
			out[i] = fmt.Sprintf("%s %s", res.Proto, b)
		}(i)
	}
	wg.Wait()
	ok := 0
	for i, s := range out {
		if s == fmt.Sprintf("HTTP/2.0 stream %d h2", i) {
			ok++
		}
	}
	fmt.Println("ok streams:", ok, "of", N)
	fmt.Println(out[0], "|", out[N-1])
}
