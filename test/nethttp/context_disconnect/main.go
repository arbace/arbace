package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"time"
)

func main() {
	up := make(chan struct{})
	gone := make(chan string, 1)
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprint(w, "start\n")
		w.(http.Flusher).Flush()
		close(up)
		select {
		case <-r.Context().Done():
			gone <- fmt.Sprint("handler context done: ", r.Context().Err())
		case <-time.After(10 * time.Second):
			gone <- "timed out waiting for disconnect"
		}
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	buf := make([]byte, 16)
	n, _ := res.Body.Read(buf)
	fmt.Printf("client read %q\n", buf[:n])
	<-up
	res.Body.Close()
	http.DefaultTransport.(*http.Transport).CloseIdleConnections()
	fmt.Println(<-gone)
}
