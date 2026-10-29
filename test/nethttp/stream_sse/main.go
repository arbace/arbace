package main

import (
	"bufio"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/event-stream")
		w.Header().Set("Cache-Control", "no-cache")
		rc := http.NewResponseController(w)
		for i := 0; i < 4; i++ {
			fmt.Fprintf(w, "id: %d\nevent: tick\ndata: value %d\n\n", i, i*i)
			if err := rc.Flush(); err != nil {
				return
			}
		}
		fmt.Fprint(w, "event: end\ndata: bye\n\n")
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	defer res.Body.Close()
	fmt.Println(res.Header.Get("Content-Type"), res.Header.Get("Cache-Control"))
	sc := bufio.NewScanner(res.Body)
	var id, ev, data string
	for sc.Scan() {
		line := sc.Text()
		switch {
		case line == "":
			fmt.Printf("event=%s id=%s data=%s\n", ev, id, data)
			id, ev, data = "", "", ""
		case strings.HasPrefix(line, "id: "):
			id = line[4:]
		case strings.HasPrefix(line, "event: "):
			ev = line[7:]
		case strings.HasPrefix(line, "data: "):
			data = line[6:]
		}
	}
}
