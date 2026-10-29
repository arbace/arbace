package main

import (
	"bytes"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"sort"
	"strings"
)

func main() {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if err := r.ParseMultipartForm(1 << 20); err != nil {
			http.Error(w, err.Error(), 400)
			return
		}
		var ks []string
		for k, v := range r.MultipartForm.Value {
			ks = append(ks, fmt.Sprintf("%s=%q", k, v))
		}
		sort.Strings(ks)
		fmt.Fprintln(w, "values:", strings.Join(ks, " "))
		for _, fh := range r.MultipartForm.File["upload"] {
			f, _ := fh.Open()
			b, _ := io.ReadAll(f)
			f.Close()
			fmt.Fprintf(w, "file %q size=%d ct=%s content=%q\n", fh.Filename, fh.Size, fh.Header.Get("Content-Type"), b)
		}
		f, h, err := r.FormFile("single")
		if err == nil {
			b, _ := io.ReadAll(f)
			fmt.Fprintf(w, "single %s %q\n", h.Filename, b)
		}
		fmt.Fprintln(w, "FormValue title:", r.FormValue("title"))
	}))
	defer ts.Close()
	var body bytes.Buffer
	mw := multipart.NewWriter(&body)
	mw.SetBoundary("corpusboundary123")
	mw.WriteField("title", "Report")
	mw.WriteField("tags", "a")
	mw.WriteField("tags", "b")
	fw, _ := mw.CreateFormFile("upload", "one.txt")
	fw.Write([]byte("first file"))
	fw, _ = mw.CreateFormFile("upload", "two.txt")
	fw.Write([]byte("second file\nwith lines"))
	fw, _ = mw.CreateFormFile("single", "solo.bin")
	fw.Write([]byte{0, 1, 2, 3})
	mw.Close()
	res, err := http.Post(ts.URL, mw.FormDataContentType(), &body)
	if err != nil {
		fmt.Println(err)
		return
	}
	out, _ := io.ReadAll(res.Body)
	res.Body.Close()
	fmt.Print(string(out))
}
