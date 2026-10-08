package main

import (
	"bufio"
	"go/constant"
	"math/big"
	"strconv"
	"unicode/utf8"
)

// The output is text for the Clojure reader. Strings are written with the reader's escapes
// (\" \\ \n \t \r \b \f \uXXXX); anything else of valid UTF-8 is written as is. A string that
// is not valid UTF-8 (only constant values can be: "\xff") is written as (:bytes [b ...]).

type out struct {
	*bufio.Writer
}

func (w out) str(s string) {
	if !utf8.ValidString(s) {
		w.WriteString("(:bytes [")
		for i := 0; i < len(s); i++ {
			if i > 0 {
				w.WriteByte(' ')
			}
			w.WriteString(strconv.Itoa(int(s[i])))
		}
		w.WriteString("])")
		return
	}
	w.WriteByte('"')
	start := 0
	for i := 0; i < len(s); i++ {
		c := s[i]
		var esc string
		switch c {
		case '"':
			esc = `\"`
		case '\\':
			esc = `\\`
		case '\n':
			esc = `\n`
		case '\t':
			esc = `\t`
		case '\r':
			esc = `\r`
		case '\b':
			esc = `\b`
		case '\f':
			esc = `\f`
		default:
			if c < 0x20 || c == 0x7f {
				esc = `\u00` + string("0123456789abcdef"[c>>4]) + string("0123456789abcdef"[c&15])
			} else {
				continue
			}
		}
		w.WriteString(s[start:i])
		w.WriteString(esc)
		start = i + 1
	}
	w.WriteString(s[start:])
	w.WriteByte('"')
}

func (w out) kw(s string) {
	w.WriteByte(':')
	w.WriteString(s)
}

func (w out) int(i int64) {
	w.WriteString(strconv.FormatInt(i, 10))
}

func (w out) bool(b bool) {
	if b {
		w.WriteString("true")
	} else {
		w.WriteString("false")
	}
}

// maxRatBits bounds the size of a float constant written exactly as an integer or a ratio.
// Larger ones (1e100000) are written as (:float "TEXT") with big.Float's 'p' format.
const maxRatBits = 1 << 14

// val writes a constant's exact value as a reader value: integers as integers (BigInt when
// large), floats as integers or ratios (exact), strings as strings, booleans as booleans,
// complex numbers as (:complex RE IM), and unknown values as nil.
func (w out) val(v constant.Value) {
	switch v.Kind() {
	case constant.Bool:
		w.bool(constant.BoolVal(v))
	case constant.String:
		w.str(constant.StringVal(v))
	case constant.Int:
		w.WriteString(v.ExactString())
	case constant.Float:
		switch x := constant.Val(v).(type) {
		case int64:
			w.int(x)
		case *big.Int:
			w.WriteString(x.String())
		case *big.Rat:
			w.rat(x)
		case *big.Float:
			if x.IsInf() || x.MantExp(nil) > maxRatBits || x.MantExp(nil) < -maxRatBits {
				w.WriteString("(:float ")
				w.str(x.Text('p', 0))
				w.WriteString(")")
			} else {
				r, _ := x.Rat(nil)
				w.rat(r)
			}
		default:
			w.WriteString("(:float ")
			w.str(v.ExactString())
			w.WriteString(")")
		}
	case constant.Complex:
		w.WriteString("(:complex ")
		w.val(constant.Real(v))
		w.WriteByte(' ')
		w.val(constant.Imag(v))
		w.WriteString(")")
	default:
		w.WriteString("nil")
	}
}

func (w out) rat(r *big.Rat) {
	if r.IsInt() {
		w.WriteString(r.Num().String())
	} else {
		w.WriteString(r.String())
	}
}
