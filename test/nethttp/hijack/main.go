package main

import (
	"bufio"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		hj, ok := w.(http.Hijacker)
		if !ok {
			http.Error(w, "no hijack", 500)
			return
		}
		conn, rw, err := hj.Hijack()
		if err != nil {
			return
		}
		defer conn.Close()
		rw.WriteString("HTTP/1.1 101 Switching Protocols\r\nUpgrade: echo\r\nConnection: Upgrade\r\n\r\n")
		rw.Flush()
		sc := bufio.NewScanner(rw)
		for sc.Scan() {
			t := sc.Text()
			if t == "quit" {
				rw.WriteString("bye\n")
				rw.Flush()
				return
			}
			rw.WriteString("echo: " + strings.ToUpper(t) + "\n")
			rw.Flush()
		}
	}))
	defer ts.Close()
	req, _ := http.NewRequest("GET", ts.URL, nil)
	req.Header.Set("Connection", "Upgrade")
	req.Header.Set("Upgrade", "echo")
	res, err := http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println(res.StatusCode, res.Header.Get("Upgrade"))
	rwc, ok := res.Body.(io.ReadWriteCloser)
	if !ok {
		fmt.Println("body is not writable")
		return
	}
	defer rwc.Close()
	br := bufio.NewReader(rwc)
	for _, m := range []string{"hello", "world", "quit"} {
		fmt.Fprintf(rwc, "%s\n", m)
		line, _ := br.ReadString('\n')
		fmt.Print(line)
	}
}
