// Names spelled like the literals true, false and nil (SPEC §4.4): declared in the package,
// written (go/id "...") wherever they stand, :init-order and :go/via included (GOI-SPEC GC3).
package names

type nil struct{ x int }

type T struct {
	nil
}

var true = false == false
var false = 1 == 2
var y = T{}.x

func f() int {
	var t T
	return t.x
}
