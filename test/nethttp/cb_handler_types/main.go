// NH3, NH7 (a) (doc/go/NET-HTTP-SURVEY.md, "As built: NH2 (write-ahead)"): 40 handler types of the program's
// own, each with its ServeHTTP, on one ServeMux: compiled net/http calls each through a stub of the
// ServeHTTP family (64 slots, NH7 (a)). Generated: one type per route, value and pointer receivers
// alternating.
package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
)

type h00 struct{ tag string }

func (h h00) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h00 %s %s", h.tag, r.URL.Path)
}

type h01 struct{ hits int }

func (h *h01) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h01 hit %d %s", h.hits, r.URL.Path)
}

type h02 struct{ tag string }

func (h h02) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h02 %s %s", h.tag, r.URL.Path)
}

type h03 struct{ hits int }

func (h *h03) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h03 hit %d %s", h.hits, r.URL.Path)
}

type h04 struct{ tag string }

func (h h04) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h04 %s %s", h.tag, r.URL.Path)
}

type h05 struct{ hits int }

func (h *h05) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h05 hit %d %s", h.hits, r.URL.Path)
}

type h06 struct{ tag string }

func (h h06) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h06 %s %s", h.tag, r.URL.Path)
}

type h07 struct{ hits int }

func (h *h07) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h07 hit %d %s", h.hits, r.URL.Path)
}

type h08 struct{ tag string }

func (h h08) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h08 %s %s", h.tag, r.URL.Path)
}

type h09 struct{ hits int }

func (h *h09) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h09 hit %d %s", h.hits, r.URL.Path)
}

type h10 struct{ tag string }

func (h h10) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h10 %s %s", h.tag, r.URL.Path)
}

type h11 struct{ hits int }

func (h *h11) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h11 hit %d %s", h.hits, r.URL.Path)
}

type h12 struct{ tag string }

func (h h12) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h12 %s %s", h.tag, r.URL.Path)
}

type h13 struct{ hits int }

func (h *h13) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h13 hit %d %s", h.hits, r.URL.Path)
}

type h14 struct{ tag string }

func (h h14) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h14 %s %s", h.tag, r.URL.Path)
}

type h15 struct{ hits int }

func (h *h15) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h15 hit %d %s", h.hits, r.URL.Path)
}

type h16 struct{ tag string }

func (h h16) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h16 %s %s", h.tag, r.URL.Path)
}

type h17 struct{ hits int }

func (h *h17) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h17 hit %d %s", h.hits, r.URL.Path)
}

type h18 struct{ tag string }

func (h h18) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h18 %s %s", h.tag, r.URL.Path)
}

type h19 struct{ hits int }

func (h *h19) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h19 hit %d %s", h.hits, r.URL.Path)
}

type h20 struct{ tag string }

func (h h20) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h20 %s %s", h.tag, r.URL.Path)
}

type h21 struct{ hits int }

func (h *h21) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h21 hit %d %s", h.hits, r.URL.Path)
}

type h22 struct{ tag string }

func (h h22) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h22 %s %s", h.tag, r.URL.Path)
}

type h23 struct{ hits int }

func (h *h23) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h23 hit %d %s", h.hits, r.URL.Path)
}

type h24 struct{ tag string }

func (h h24) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h24 %s %s", h.tag, r.URL.Path)
}

type h25 struct{ hits int }

func (h *h25) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h25 hit %d %s", h.hits, r.URL.Path)
}

type h26 struct{ tag string }

func (h h26) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h26 %s %s", h.tag, r.URL.Path)
}

type h27 struct{ hits int }

func (h *h27) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h27 hit %d %s", h.hits, r.URL.Path)
}

type h28 struct{ tag string }

func (h h28) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h28 %s %s", h.tag, r.URL.Path)
}

type h29 struct{ hits int }

func (h *h29) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h29 hit %d %s", h.hits, r.URL.Path)
}

type h30 struct{ tag string }

func (h h30) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h30 %s %s", h.tag, r.URL.Path)
}

type h31 struct{ hits int }

func (h *h31) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h31 hit %d %s", h.hits, r.URL.Path)
}

type h32 struct{ tag string }

func (h h32) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h32 %s %s", h.tag, r.URL.Path)
}

type h33 struct{ hits int }

func (h *h33) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h33 hit %d %s", h.hits, r.URL.Path)
}

type h34 struct{ tag string }

func (h h34) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h34 %s %s", h.tag, r.URL.Path)
}

type h35 struct{ hits int }

func (h *h35) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h35 hit %d %s", h.hits, r.URL.Path)
}

type h36 struct{ tag string }

func (h h36) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h36 %s %s", h.tag, r.URL.Path)
}

type h37 struct{ hits int }

func (h *h37) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h37 hit %d %s", h.hits, r.URL.Path)
}

type h38 struct{ tag string }

func (h h38) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "h38 %s %s", h.tag, r.URL.Path)
}

type h39 struct{ hits int }

func (h *h39) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	h.hits++
	fmt.Fprintf(w, "h39 hit %d %s", h.hits, r.URL.Path)
}

func main() {
	mux := http.NewServeMux()

	mux.Handle("/r00", h00{"v0"})

	mux.Handle("/r01", &h01{})

	mux.Handle("/r02", h02{"v2"})

	mux.Handle("/r03", &h03{})

	mux.Handle("/r04", h04{"v4"})

	mux.Handle("/r05", &h05{})

	mux.Handle("/r06", h06{"v6"})

	mux.Handle("/r07", &h07{})

	mux.Handle("/r08", h08{"v8"})

	mux.Handle("/r09", &h09{})

	mux.Handle("/r10", h10{"v10"})

	mux.Handle("/r11", &h11{})

	mux.Handle("/r12", h12{"v12"})

	mux.Handle("/r13", &h13{})

	mux.Handle("/r14", h14{"v14"})

	mux.Handle("/r15", &h15{})

	mux.Handle("/r16", h16{"v16"})

	mux.Handle("/r17", &h17{})

	mux.Handle("/r18", h18{"v18"})

	mux.Handle("/r19", &h19{})

	mux.Handle("/r20", h20{"v20"})

	mux.Handle("/r21", &h21{})

	mux.Handle("/r22", h22{"v22"})

	mux.Handle("/r23", &h23{})

	mux.Handle("/r24", h24{"v24"})

	mux.Handle("/r25", &h25{})

	mux.Handle("/r26", h26{"v26"})

	mux.Handle("/r27", &h27{})

	mux.Handle("/r28", h28{"v28"})

	mux.Handle("/r29", &h29{})

	mux.Handle("/r30", h30{"v30"})

	mux.Handle("/r31", &h31{})

	mux.Handle("/r32", h32{"v32"})

	mux.Handle("/r33", &h33{})

	mux.Handle("/r34", h34{"v34"})

	mux.Handle("/r35", &h35{})

	mux.Handle("/r36", h36{"v36"})

	mux.Handle("/r37", &h37{})

	mux.Handle("/r38", h38{"v38"})

	mux.Handle("/r39", &h39{})

	ts := httptest.NewServer(mux)
	defer ts.Close()
	get := func(p string) string {
		res, err := http.Get(ts.URL + p)
		if err != nil {
			return err.Error()
		}
		defer res.Body.Close()
		b, _ := io.ReadAll(res.Body)
		return fmt.Sprintf("%d %s", res.StatusCode, strings.TrimSpace(string(b)))
	}
	var lines []string
	for round := 0; round < 2; round++ {
		for i := 0; i < 40; i++ {
			lines = append(lines, get(fmt.Sprintf("/r%02d", i)))
		}
	}
	fmt.Println(strings.Join(lines, "\n"))
	fmt.Println(get("/none"))
}
