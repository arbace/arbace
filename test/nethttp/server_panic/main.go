package main

import (
	"bytes"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	var buf bytes.Buffer
	ts := httptest.NewUnstartedServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/panic" {
			panic("handler failure")
		}
		fmt.Fprint(w, "alive")
	}))
	ts.Config.ErrorLog = log.New(&buf, "srv: ", 0)
	ts.Start()
	defer ts.Close()
	_, err := http.Get(ts.URL + "/panic")
	fmt.Println(err != nil, errors.Is(err, io.EOF), strings.HasSuffix(err.Error(), "EOF"))
	res, err := http.Get(ts.URL + "/after")
	if err != nil {
		fmt.Println(err)
		return
	}
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(res.StatusCode, string(b))
	first := strings.SplitN(buf.String(), "\n", 2)[0]
	fmt.Println(strings.HasPrefix(first, "srv: http: panic serving "), strings.HasSuffix(first, ": handler failure"))
}
