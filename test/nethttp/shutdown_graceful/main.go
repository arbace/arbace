package main

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"time"
)

func main() {
	l, _ := net.Listen("tcp", "127.0.0.1:0")
	entered := make(chan struct{})
	finish := make(chan struct{})
	srv := &http.Server{Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/slow" {
			close(entered)
			<-finish
			fmt.Fprint(w, "slow finished")
			return
		}
		fmt.Fprint(w, "quick")
	})}
	served := make(chan error, 1)
	go func() { served <- srv.Serve(l) }()
	base := "http://" + l.Addr().String()
	res, _ := http.Get(base + "/quick")
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println("before:", string(b))
	result := make(chan string, 1)
	go func() {
		res, err := http.Get(base + "/slow")
		if err != nil {
			result <- "error: " + err.Error()
			return
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		result <- string(b)
	}()
	<-entered
	shut := make(chan error, 1)
	go func() { shut <- srv.Shutdown(context.Background()) }()
	time.Sleep(100 * time.Millisecond)
	select {
	case err := <-shut:
		fmt.Println("shutdown returned early:", err)
	default:
		fmt.Println("shutdown waiting for the in-flight request")
	}
	_, err := (&http.Client{Timeout: time.Second}).Get(base + "/new")
	fmt.Println("new request refused:", err != nil)
	close(finish)
	fmt.Println("in-flight:", <-result)
	fmt.Println("shutdown:", <-shut)
	err = <-served
	fmt.Println("serve:", err, errors.Is(err, http.ErrServerClosed))
}
