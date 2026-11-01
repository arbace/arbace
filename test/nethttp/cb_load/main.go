// NH3 (doc/go/NET-HTTP-SURVEY.md, "As built: NH2 (write-ahead)", §3.2's table): a handler of the program's
// own type under load from compiled net/http's goroutines, each of which enters the handler through its
// stub (a goi thread adopted per goroutine): 2,000 sequential requests on one kept-alive connection,
// 400 concurrent requests on their own connections, and 200 concurrent HTTP/2 streams on one TLS
// connection. Only counts and sums are printed (no timing); NH3 measures threads and memory beside it.
package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strconv"
	"sync"
	"sync/atomic"
)

type counter struct {
	served atomic.Int64
	sum    atomic.Int64
}

func (c *counter) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	n, _ := strconv.Atoi(r.URL.Query().Get("n"))
	c.served.Add(1)
	c.sum.Add(int64(n))
	fmt.Fprintf(w, "%d %d", n, r.ProtoMajor)
}

func fetch(c *http.Client, url string) (string, error) {
	res, err := c.Get(url)
	if err != nil {
		return "", err
	}
	defer res.Body.Close()
	b, err := io.ReadAll(res.Body)
	return string(b), err
}

func main() {
	h := &counter{}
	ts := httptest.NewServer(h)
	defer ts.Close()

	// sequential, kept alive
	bad := 0
	for i := 0; i < 2000; i++ {
		got, err := fetch(ts.Client(), fmt.Sprintf("%s/?n=%d", ts.URL, i))
		if err != nil || got != fmt.Sprintf("%d 1", i) {
			bad++
		}
	}
	fmt.Println("sequential: served", h.served.Load(), "sum", h.sum.Load(), "bad", bad)

	// concurrent, a connection each
	h.served.Store(0)
	h.sum.Store(0)
	tr := &http.Transport{DisableKeepAlives: true}
	c := &http.Client{Transport: tr}
	var wg sync.WaitGroup
	var errs atomic.Int64
	for i := 0; i < 400; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			got, err := fetch(c, fmt.Sprintf("%s/?n=%d", ts.URL, i))
			if err != nil || got != fmt.Sprintf("%d 1", i) {
				errs.Add(1)
			}
		}(i)
	}
	wg.Wait()
	fmt.Println("concurrent: served", h.served.Load(), "sum", h.sum.Load(), "bad", errs.Load())

	// HTTP/2 streams on one TLS connection
	h2 := &counter{}
	ts2 := httptest.NewUnstartedServer(h2)
	ts2.EnableHTTP2 = true
	ts2.StartTLS()
	defer ts2.Close()
	c2 := ts2.Client()
	// one request first: the connection exists before the streams, which share it (200, under the
	// server's 250 concurrent streams), so no extra connection is dialed and dropped
	if _, err := fetch(c2, ts2.URL+"/?n=0"); err != nil {
		fmt.Println("http2 warm-up:", err)
	}
	h2.served.Store(0)
	h2.sum.Store(0)
	errs.Store(0)
	for i := 0; i < 200; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			got, err := fetch(c2, fmt.Sprintf("%s/?n=%d", ts2.URL, i))
			if err != nil || got != fmt.Sprintf("%d 2", i) {
				errs.Add(1)
			}
		}(i)
	}
	wg.Wait()
	fmt.Println("http2 streams: served", h2.served.Load(), "sum", h2.sum.Load(), "bad", errs.Load())
}
