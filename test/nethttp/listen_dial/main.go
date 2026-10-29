package main

import (
	"bufio"
	"fmt"
	"net"
	"strings"
)

func main() {
	l, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		fmt.Println("listen:", err)
		return
	}
	defer l.Close()
	go func() {
		for {
			c, err := l.Accept()
			if err != nil {
				return
			}
			go func() {
				defer c.Close()
				sc := bufio.NewScanner(c)
				for sc.Scan() {
					fmt.Fprintf(c, "echo %s\n", strings.ToUpper(sc.Text()))
				}
			}()
		}
	}()
	c, err := net.Dial("tcp", l.Addr().String())
	if err != nil {
		fmt.Println("dial:", err)
		return
	}
	defer c.Close()
	br := bufio.NewReader(c)
	for _, m := range []string{"one", "two", "three"} {
		fmt.Fprintln(c, m)
		line, _ := br.ReadString('\n')
		fmt.Print(line)
	}
	host, _, _ := net.SplitHostPort(l.Addr().String())
	fmt.Println(host, l.Addr().Network(), c.LocalAddr().Network())
}
