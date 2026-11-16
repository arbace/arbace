// Command serve is the shim's development server (doc/wasm/WEB.md §4; doc/wasm/WASM-SURVEY.md §9.2): DIR's files over HTTP on
// localhost alone (WebGPU's secure context), .wasm as application/wasm for instantiateStreaming. A tool run by hand
// (`bin/wasm serve DIR [PORT]`), never by a check. Built by TamaGo's go.
package main

import (
	"fmt"
	"mime"
	"net/http"
	"os"
)

func main() {
	if len(os.Args) < 2 {
		fmt.Fprintln(os.Stderr, "serve DIR [PORT]")
		os.Exit(2)
	}
	port := "8080"
	if len(os.Args) > 2 {
		port = os.Args[2]
	}
	mime.AddExtensionType(".wasm", "application/wasm")
	mime.AddExtensionType(".js", "text/javascript")
	h := http.FileServer(http.Dir(os.Args[1]))
	addr := "127.0.0.1:" + port
	fmt.Println("serving", os.Args[1], "on http://"+addr+"/")
	if err := http.ListenAndServe(addr, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		h.ServeHTTP(w, r)
	})); err != nil {
		fmt.Fprintln(os.Stderr, "serve:", err)
		os.Exit(1)
	}
}
