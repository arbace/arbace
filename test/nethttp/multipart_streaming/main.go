package main

import (
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mr, err := r.MultipartReader()
		if err != nil {
			http.Error(w, err.Error(), 400)
			return
		}
		for {
			p, err := mr.NextPart()
			if err == io.EOF {
				break
			}
			if err != nil {
				fmt.Fprintln(w, "error:", err)
				return
			}
			b, _ := io.ReadAll(p)
			fmt.Fprintf(w, "part name=%q file=%q ct=%q len=%d\n", p.FormName(), p.FileName(), p.Header.Get("Content-Type"), len(b))
		}
	}))
	defer ts.Close()
	pr, pw := io.Pipe()
	mw := multipart.NewWriter(pw)
	go func() {
		mw.WriteField("a", "1")
		for i := 0; i < 3; i++ {
			fw, _ := mw.CreateFormFile("f", fmt.Sprintf("file%d.dat", i))
			fw.Write(make([]byte, 1000*(i+1)))
		}
		mw.Close()
		pw.Close()
	}()
	res, err := http.Post(ts.URL, mw.FormDataContentType(), pr)
	if err != nil {
		fmt.Println(err)
		return
	}
	out, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Print(string(out))
	res, _ = http.Post(ts.URL, "text/plain", nil)
	out, _ = io.ReadAll(res.Body)
	fmt.Printf("%d %s", res.StatusCode, out)
}
