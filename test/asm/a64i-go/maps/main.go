// queue 288: maps (growth, deletion, iteration sorted), strings, strconv, sort, a struct key, crypto/sha256 and encoding
package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"sort"
	"strconv"
	"strings"
)

type key struct {
	a int
	b string
}

func main() {
	m := map[string]int{}
	for i := 0; i < 10000; i++ {
		m["k"+strconv.Itoa(i)] = i * i
	}
	for i := 0; i < 10000; i += 3 {
		delete(m, "k"+strconv.Itoa(i))
	}
	keys := make([]string, 0, len(m))
	sum := 0
	for k, v := range m {
		keys = append(keys, k)
		sum += v
	}
	sort.Strings(keys)
	fmt.Println(len(m), sum, keys[0], keys[len(keys)-1])

	sk := map[key][]int{}
	for i := 0; i < 100; i++ {
		k := key{i % 7, strings.Repeat("x", i%3)}
		sk[k] = append(sk[k], i)
	}
	fmt.Println(len(sk), sk[key{3, "x"}])

	h := sha256.Sum256([]byte("abc"))
	fmt.Println(hex.EncodeToString(h[:]))
	b, _ := json.Marshal(map[string]any{"b": []int{1, 2}, "a": 1.5, "c": "s"})
	fmt.Println(string(b))
	fmt.Println(strconv.FormatFloat(3.14159, 'e', 3, 64), strings.ToUpper("arbace"), fmt.Sprintf("%08.3f|%x|%q", 2.5, 255, "q"))
}
