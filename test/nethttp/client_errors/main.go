package main

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/http"
	"net/url"
	"syscall"
)

func main() {
	l, _ := net.Listen("tcp", "127.0.0.1:0")
	addr := l.Addr().String()
	l.Close()
	_, err := http.Get("http://" + addr + "/refused")
	var ue *url.Error
	fmt.Println(errors.As(err, &ue))
	fmt.Println(ue.Op, ue.URL == "http://"+addr+"/refused", ue.Timeout(), ue.Temporary())
	var oe *net.OpError
	fmt.Println(errors.As(err, &oe), oe.Op, oe.Net)
	fmt.Println(errors.Is(err, syscall.ECONNREFUSED))
	_, err = http.Get("://bad")
	fmt.Println(err)
	_, err = http.Get("ftp://example.invalid/x")
	fmt.Println(err)
	_, err = http.NewRequest("BAD METHOD", "http://x/", nil)
	fmt.Println(err)
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	req, _ := http.NewRequestWithContext(ctx, "GET", "http://"+addr, nil)
	_, err = http.DefaultClient.Do(req)
	fmt.Println(errors.Is(err, context.Canceled))
}
