package main

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"mime"
	"mime/multipart"
	"mime/quotedprintable"
	"net/textproto"
	"strings"
)

func main() {
	mt, params, err := mime.ParseMediaType(`text/html; charset="utf-8"; format=flowed`)
	fmt.Println(mt, params["charset"], params["format"], err)
	fmt.Println(mime.FormatMediaType("multipart/form-data", map[string]string{"boundary": "a b"}))
	fmt.Println(mime.TypeByExtension(".json"), mime.TypeByExtension(".html") != "")
	_, _, err = mime.ParseMediaType("bad;;")
	fmt.Println(err)
	fmt.Println(mime.QEncoding.Encode("utf-8", "héllo wörld"))
	dec := new(mime.WordDecoder)
	s, err := dec.DecodeHeader("=?utf-8?q?h=C3=A9llo?= =?utf-8?b?V29ybGQ=?=")
	fmt.Println(s, err)

	var qb bytes.Buffer
	qw := quotedprintable.NewWriter(&qb)
	qw.Write([]byte("caf\xc3\xa9 = good\tline with trailing space \nnext"))
	qw.Close()
	fmt.Printf("%q\n", qb.String())
	back, _ := io.ReadAll(quotedprintable.NewReader(&qb))
	fmt.Printf("%q\n", back)

	raw := "Subject: hi\r\nX-Multi: a\r\nX-Multi: b\r\nContinued: one\r\n two\r\n\r\nbody text\r\n"
	r := textproto.NewReader(bufio.NewReader(strings.NewReader(raw)))
	h, err := r.ReadMIMEHeader()
	fmt.Println(h["Subject"], h["X-Multi"], h.Get("continued"), err)
	rest, _ := r.ReadLine()
	fmt.Println(rest)
	fmt.Println(textproto.CanonicalMIMEHeaderKey("x-forwarded-for"), textproto.TrimString("  pad  "))

	body := "--BOUND\r\nContent-Disposition: form-data; name=\"x\"\r\n\r\nvalue-x\r\n--BOUND\r\nContent-Disposition: form-data; name=\"y\"; filename=\"y.txt\"\r\nContent-Type: text/plain\r\n\r\nvalue-y\r\n--BOUND--\r\n"
	mr := multipart.NewReader(strings.NewReader(body), "BOUND")
	for {
		p, err := mr.NextPart()
		if err != nil {
			fmt.Println(err)
			break
		}
		b, _ := io.ReadAll(p)
		fmt.Println(p.FormName(), p.FileName(), string(b))
	}
	var out bytes.Buffer
	mw := multipart.NewWriter(&out)
	mw.SetBoundary("B2")
	pw, _ := mw.CreatePart(textproto.MIMEHeader{"Content-Type": {"text/plain"}, "X-Id": {"7"}})
	pw.Write([]byte("hello"))
	mw.Close()
	fmt.Printf("%q\n", out.String())
}
