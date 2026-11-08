package identity

type (
	Ptr      *byte
	Ptr16    *uint16
	ENCODING struct {
		utf8Convert  func(enc *ENCODING, fromP *Ptr, fromLim Ptr, toP *Ptr, toLim Ptr) int
		utf16Convert func(enc *ENCODING, fromP *Ptr, fromLim Ptr, toP *Ptr16, toLim Ptr16) int
		same1, same2 func(x int) int
		r1           func(x int) (n int)
		r2           func(x int) (n string)
		r3, r4       func(x int) (n int)
		t1           struct {
			x int `a`
		}
		t2 struct {
			x int `b`
		}
		t3, t4 struct {
			x int `a`
		}
	}
)

func Params(f func(x int), g func(x string), h, k func(x int)) (a func() (n int), b func() (n bool)) {
	return nil, nil
}
