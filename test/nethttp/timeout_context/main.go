package main

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"time"
)

func main() {
	started := make(chan struct{}, 4)
	serverSawCancel := make(chan error, 4)
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		started <- struct{}{}
		select {
		case <-r.Context().Done():
			serverSawCancel <- r.Context().Err()
		case <-time.After(5 * time.Second):
			fmt.Fprint(w, "late")
		}
	}))
	defer ts.Close()
	ctx, cancel := context.WithTimeout(context.Background(), 80*time.Millisecond)
	defer cancel()
	req, _ := http.NewRequestWithContext(ctx, "GET", ts.URL, nil)
	_, err := http.DefaultClient.Do(req)
	fmt.Println(errors.Is(err, context.DeadlineExceeded), ctx.Err())
	<-started
	fmt.Println("server saw:", <-serverSawCancel)
	ctx2, cancel2 := context.WithCancel(context.Background())
	req, _ = http.NewRequestWithContext(ctx2, "GET", ts.URL, nil)
	go func() {
		<-started
		cancel2()
	}()
	_, err = http.DefaultClient.Do(req)
	fmt.Println(errors.Is(err, context.Canceled))
	fmt.Println("server saw:", <-serverSawCancel)
}
