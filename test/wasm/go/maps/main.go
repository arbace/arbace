// Maps: insertion, deletion, growth past a few thousand keys, sorted iteration, struct and string keys (WA5's own programs).
package main

import (
	"fmt"
	"sort"
	"strconv"
)

type key struct {
	a int
	b string
}

func main() {
	m := map[string]int{}
	for i := 0; i < 5000; i++ {
		m["k"+strconv.Itoa(i)] = i
	}
	for i := 0; i < 5000; i += 2 {
		delete(m, "k"+strconv.Itoa(i))
	}
	sum := 0
	for _, v := range m {
		sum += v
	}
	fmt.Println(len(m), sum, m["k4999"], m["k4998"])
	s := map[key][]int{}
	for i := 0; i < 30; i++ {
		k := key{i % 3, strconv.Itoa(i % 5)}
		s[k] = append(s[k], i)
	}
	var ks []string
	for k, v := range s {
		ks = append(ks, fmt.Sprintf("%d/%s:%v", k.a, k.b, v))
	}
	sort.Strings(ks)
	for _, k := range ks {
		fmt.Println(k)
	}
	f := map[float64]bool{1.5: true, -0.0: true}
	fmt.Println(len(f), f[0], f[1.5])
}
