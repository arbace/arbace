// NH3 (doc/go/NET-HTTP-SURVEY.md, "As built: NH2 (write-ahead)"): a middleware's ResponseWriter wrapper
// of the program's own type, seen by compiled net/http: ResponseController walks it through Unwrap()
// ResponseWriter (net/http's unexported rwUnwrapper) and calls FlushError() error and
// EnableFullDuplex() error on it; w.(http.Flusher) and the other assertions on the wrapper.
package main

import (
	"bufio"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"time"
)

// counting wraps a ResponseWriter and counts the bytes written; it unwraps for ResponseController.
type counting struct {
	http.ResponseWriter
	n int
}

func (c *counting) Write(b []byte) (int, error) {
	n, err := c.ResponseWriter.Write(b)
	c.n += n
	return n, err
}

func (c *counting) Unwrap() http.ResponseWriter { return c.ResponseWriter }

// flushing has its own FlushError, which ResponseController prefers to the inner Flusher.
type flushing struct {
	http.ResponseWriter
	flushes *int
}

func (f flushing) FlushError() error {
	*f.flushes++
	if fl, ok := f.ResponseWriter.(http.Flusher); ok {
		fl.Flush()
	}
	return nil
}

// duplex says no to full duplex itself.
type duplex struct{ http.ResponseWriter }

func (duplex) EnableFullDuplex() error { return errors.New("duplex: refused by the wrapper") }

func main() {
	var flushes int
	mux := http.NewServeMux()
	mux.HandleFunc("/count", func(w http.ResponseWriter, r *http.Request) {
		cw := &counting{ResponseWriter: w}
		rc := http.NewResponseController(cw)
		io.WriteString(cw, "one;")
		fmt.Println("count flush:", rc.Flush())
		fmt.Println("count deadline:", rc.SetWriteDeadline(time.Now().Add(time.Minute)))
		io.WriteString(cw, "two")
		_, isFlusher := interface{}(cw).(http.Flusher)
		_, isHijacker := interface{}(cw).(http.Hijacker)
		fmt.Println("count wrapper: Flusher", isFlusher, "Hijacker", isHijacker, "bytes", cw.n)
	})
	mux.HandleFunc("/flush", func(w http.ResponseWriter, r *http.Request) {
		fw := flushing{w, &flushes}
		rc := http.NewResponseController(fw)
		io.WriteString(fw, "a;")
		fmt.Println("flush:", rc.Flush(), "flushes", flushes)
		io.WriteString(fw, "b")
		fmt.Println("flush again:", rc.Flush(), "flushes", flushes)
	})
	mux.HandleFunc("/duplex", func(w http.ResponseWriter, r *http.Request) {
		rc := http.NewResponseController(duplex{w})
		fmt.Println("duplex:", rc.EnableFullDuplex())
		rc2 := http.NewResponseController(&counting{ResponseWriter: w})
		fmt.Println("duplex through Unwrap:", rc2.EnableFullDuplex())
		io.WriteString(w, "ok")
	})
	mux.HandleFunc("/bare", func(w http.ResponseWriter, r *http.Request) {
		// a wrapper without Unwrap: ResponseController cannot reach the connection
		type plain struct{ http.ResponseWriter }
		rc := http.NewResponseController(plain{w})
		err := rc.Flush()
		fmt.Println("bare flush unsupported:", errors.Is(err, http.ErrNotSupported))
		io.WriteString(w, "bare")
	})
	ts := httptest.NewServer(mux)
	defer ts.Close()
	for _, p := range []string{"/count", "/flush", "/duplex", "/bare"} {
		res, err := http.Get(ts.URL + p)
		if err != nil {
			fmt.Println(err)
			continue
		}
		b, _ := io.ReadAll(bufio.NewReader(res.Body))
		res.Body.Close()
		fmt.Println(p, res.StatusCode, strings.Join(res.TransferEncoding, ","), string(b))
	}
}
