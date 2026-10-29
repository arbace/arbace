package main

import (
	"bufio"
	"fmt"
	"net/http"
	"net/http/httptest"
)

func main() {
	step := make(chan struct{})
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fl := w.(http.Flusher)
		for i := 1; i <= 3; i++ {
			fmt.Fprintf(w, "chunk %d\n", i)
			fl.Flush()
			<-step
		}
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	defer res.Body.Close()
	fmt.Println(res.ContentLength, res.TransferEncoding, res.Header.Get("Content-Length") == "")
	sc := bufio.NewScanner(res.Body)
	for sc.Scan() {
		fmt.Println("client got:", sc.Text())
		step <- struct{}{}
	}
	fmt.Println("eof:", sc.Err())
}
