// NH3 (doc/go/NET-HTTP-SURVEY.md, "As built: NH2 (write-ahead)"): a Client of the program's own parts as
// compiled net/http calls them: an http.CookieJar (SetCookies, Cookies), an http.RoundTripper wrapping
// the Transport (RoundTrip), its CloseIdleConnections (net/http's unexported closeIdler, from
// Client.CloseIdleConnections), CheckRedirect, and a request's GetBody replayed on a 307.
package main

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"sort"
	"strings"
	"sync"
)

type jar struct {
	mu      sync.Mutex
	cookies map[string][]*http.Cookie
	log     []string
}

func (j *jar) SetCookies(u *url.URL, cs []*http.Cookie) {
	j.mu.Lock()
	defer j.mu.Unlock()
	for _, c := range cs {
		j.cookies[u.Host] = append(j.cookies[u.Host], &http.Cookie{Name: c.Name, Value: c.Value})
		j.log = append(j.log, "set "+c.Name+"="+c.Value+" for "+u.Path)
	}
}

func (j *jar) Cookies(u *url.URL) []*http.Cookie {
	j.mu.Lock()
	defer j.mu.Unlock()
	j.log = append(j.log, fmt.Sprintf("cookies for %s: %d", u.Path, len(j.cookies[u.Host])))
	return j.cookies[u.Host]
}

type logging struct {
	next   http.RoundTripper
	seen   []string
	closed int
}

func (l *logging) RoundTrip(r *http.Request) (*http.Response, error) {
	l.seen = append(l.seen, r.Method+" "+r.URL.Path)
	r2 := r.Clone(r.Context())
	r2.Header.Set("X-Via", "logging")
	res, err := l.next.RoundTrip(r2)
	if err == nil {
		res.Header.Set("X-Logged", "yes")
	}
	return res, err
}

func (l *logging) CloseIdleConnections() {
	l.closed++
	if c, ok := l.next.(interface{ CloseIdleConnections() }); ok {
		c.CloseIdleConnections()
	}
}

func main() {
	mux := http.NewServeMux()
	mux.HandleFunc("/login", func(w http.ResponseWriter, r *http.Request) {
		http.SetCookie(w, &http.Cookie{Name: "session", Value: "s1"})
		http.Redirect(w, r, "/home", http.StatusFound)
	})
	mux.HandleFunc("/home", func(w http.ResponseWriter, r *http.Request) {
		c, err := r.Cookie("session")
		if err != nil {
			fmt.Fprint(w, "no session")
			return
		}
		fmt.Fprintf(w, "home of %s via %s", c.Value, r.Header.Get("X-Via"))
	})
	mux.HandleFunc("/moved", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/echo", http.StatusTemporaryRedirect)
	})
	mux.HandleFunc("/echo", func(w http.ResponseWriter, r *http.Request) {
		b, _ := io.ReadAll(r.Body)
		fmt.Fprintf(w, "%s %q", r.Method, b)
	})
	ts := httptest.NewServer(mux)
	defer ts.Close()
	j := &jar{cookies: map[string][]*http.Cookie{}}
	lt := &logging{next: http.DefaultTransport.(*http.Transport).Clone()}
	var hops []string
	c := &http.Client{
		Jar:       j,
		Transport: lt,
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			hops = append(hops, fmt.Sprintf("%s after %d", req.URL.Path, len(via)))
			return nil
		},
	}
	show := func(res *http.Response, err error) {
		if err != nil {
			fmt.Println("error:", err)
			return
		}
		b, _ := io.ReadAll(res.Body)
		res.Body.Close()
		fmt.Println(res.StatusCode, res.Header.Get("X-Logged"), string(b))
	}
	show(c.Get(ts.URL + "/login"))
	show(c.Post(ts.URL+"/moved", "text/plain", strings.NewReader("replayed body")))
	c.CloseIdleConnections()
	fmt.Println("hops:", strings.Join(hops, "; "))
	fmt.Println("seen:", strings.Join(lt.seen, "; "))
	fmt.Println("close idle:", lt.closed)
	sort.Strings(j.log)
	fmt.Println("jar:", strings.Join(j.log, "; "))
}
