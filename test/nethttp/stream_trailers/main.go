package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Trailer", "X-Checksum, X-Count")
		w.Header().Add("Trailer", "X-Late")
		fmt.Fprint(w, "alpha ")
		w.(http.Flusher).Flush()
		fmt.Fprint(w, "beta")
		w.Header().Set("X-Checksum", "abc123")
		w.Header().Set("X-Count", "2")
		w.Header().Set(http.TrailerPrefix+"X-Surprise", "unannounced")
	}))
	defer ts.Close()
	res, err := http.Get(ts.URL)
	if err != nil {
		fmt.Println(err)
		return
	}
	fmt.Println("announced:", res.Trailer)
	b, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Println(strings.TrimSpace(string(b)))
	fmt.Println(res.Trailer.Get("X-Checksum"), res.Trailer.Get("X-Count"), res.Trailer.Get("X-Surprise"), len(res.Trailer))
	fmt.Println(res.TransferEncoding)
}
