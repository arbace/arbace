package main

import (
	"bufio"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"time"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		rc := http.NewResponseController(w)
		fmt.Fprint(w, "first;")
		fmt.Println("flush:", rc.Flush())
		fmt.Println("write deadline:", rc.SetWriteDeadline(time.Now().Add(time.Minute)))
		fmt.Println("read deadline:", rc.SetReadDeadline(time.Now().Add(time.Minute)))
		fmt.Fprint(w, "second")
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(bufio.NewReader(res.Body))
	res.Body.Close()
	fmt.Println(string(b), res.TransferEncoding)
	rec := httptest.NewRecorder()
	rc := http.NewResponseController(rec)
	err = rc.SetReadDeadline(time.Now())
	fmt.Println("recorder deadline unsupported:", errors.Is(err, http.ErrNotSupported))
	fmt.Println("recorder flush:", rc.Flush(), rec.Flushed)
}
