package main

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/http"
	"time"
)

func main() {
	l, _ := net.Listen("tcp", "127.0.0.1:0")
	entered := make(chan struct{})
	release := make(chan struct{})
	srv := &http.Server{Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		close(entered)
		select {
		case <-release:
		case <-r.Context().Done():
		}
	})}
	closedHook := make(chan struct{})
	srv.RegisterOnShutdown(func() { close(closedHook) })
	served := make(chan error, 1)
	go func() { served <- srv.Serve(l) }()
	errc := make(chan error, 1)
	go func() {
		_, err := http.Get("http://" + l.Addr().String())
		errc <- err
	}()
	<-entered
	ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancel()
	err := srv.Shutdown(ctx)
	fmt.Println("shutdown:", err, errors.Is(err, context.DeadlineExceeded))
	<-closedHook
	fmt.Println("on-shutdown hook ran")
	fmt.Println("close:", srv.Close())
	err = <-errc
	fmt.Println("client sees failure:", err != nil)
	fmt.Println("serve:", <-served)
	close(release)
	fmt.Println("listen after close:", srv.ListenAndServe())
}
