package main

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"time"
)

func main() {
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { fmt.Fprint(w, "ok") }))
	ts.Config.ReadHeaderTimeout = 150 * time.Millisecond
	ts.Config.IdleTimeout = 150 * time.Millisecond
	ts.Config.ErrorLog = log.New(io.Discard, "", 0)
	ts.Start()
	defer ts.Close()
	addr := strings.TrimPrefix(ts.URL, "http://")
	c, _ := net.Dial("tcp", addr)
	fmt.Fprint(c, "GET / HTTP/1.1\r\nHost: x\r\n")
	c.SetReadDeadline(time.Now().Add(5 * time.Second))
	line, err := bufio.NewReader(c).ReadString('\n')
	fmt.Printf("slow headers: %q %v\n", strings.TrimSpace(line), err)
	c.Close()
	c, _ = net.Dial("tcp", addr)
	fmt.Fprint(c, "GET / HTTP/1.1\r\nHost: x\r\n\r\n")
	br := bufio.NewReader(c)
	res, err := http.ReadResponse(br, nil)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	fmt.Println("first:", res.StatusCode, string(b))
	c.SetReadDeadline(time.Now().Add(5 * time.Second))
	_, err = br.ReadByte()
	fmt.Println("idle connection closed by server:", err == io.EOF)
	c.Close()
}
