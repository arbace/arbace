package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"sort"
	"strconv"
	"sync"
	"sync/atomic"
)

func main() {
	var served atomic.Int64
	var inflight, peak atomic.Int64
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		cur := inflight.Add(1)
		for {
			p := peak.Load()
			if cur <= p || peak.CompareAndSwap(p, cur) {
				break
			}
		}
		n, _ := strconv.Atoi(r.URL.Query().Get("n"))
		served.Add(1)
		fmt.Fprint(w, n*n)
		inflight.Add(-1)
	}))
	defer ts.Close()
	const N = 200
	tr := &http.Transport{MaxIdleConnsPerHost: 50}
	c := &http.Client{Transport: tr}
	results := make([]int, N)
	var wg sync.WaitGroup
	sem := make(chan struct{}, 40)
	for i := 0; i < N; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			sem <- struct{}{}
			defer func() { <-sem }()
			res, err := c.Get(fmt.Sprintf("%s/?n=%d", ts.URL, i))
			if err != nil {
				results[i] = -1
				return
			}
			b, _ := io.ReadAll(res.Body)
			res.Body.Close()
			results[i], _ = strconv.Atoi(string(b))
		}(i)
	}
	wg.Wait()
	sum := 0
	bad := 0
	for i, v := range results {
		if v != i*i {
			bad++
		}
		sum += v
	}
	sort.Ints(results)
	fmt.Println("served:", served.Load(), "bad:", bad, "sum:", sum, "max:", results[N-1], "peak>1:", peak.Load() > 1)
}
