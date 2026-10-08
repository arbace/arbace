package main

import (
	"go/constant"
	"io"
	"math/big"
	"strconv"
	"unicode/utf8"
)

// The output is text for the Clojure reader. Strings are written with the reader's escapes
// (\" \\ \n \t \r \b \f \uXXXX); anything else of valid UTF-8 is written as is. A string that
// is not valid UTF-8 (only constant values can be: "\xff") is written as (:bytes [b ...]).

type out struct {
	sink
}

// sink is what out writes to: the dump's buffered file, or a strings.Builder (type table entries).
type sink interface {
	io.Writer
	io.ByteWriter
	io.StringWriter
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

// maxRatBits bounds the size of a float constant written as an integer or a ratio. Larger
// ones (1e100000; go/constant holds them as big.Float) are written as mantissa and exponent.
const maxRatBits = 1 << 14

// val writes a constant's exact value, its kind told by the representation:
//
//	bool     true, false
//	string   "..." (valid UTF-8), else (:bytes [b ...])
//	int      an integer (BigInt when large); also untyped runes
//	float    (:float R), R the exact value as an integer or a ratio;
//	         (:float M E) for M·2^E, M an odd integer, when |E| is beyond 2^14
//	complex  (:complex (:float RE) (:float IM))
//	unknown  nil
func (w out) val(v constant.Value) {
	switch v.Kind() {
	case constant.Bool:
		w.bool(constant.BoolVal(v))
	case constant.String:
		w.str(constant.StringVal(v))
	case constant.Int:
		w.WriteString(v.ExactString())
	case constant.Float:
		w.float(v)
	case constant.Complex:
		w.WriteString("(:complex ")
		w.float(constant.ToFloat(constant.Real(v)))
		w.WriteByte(' ')
		w.float(constant.ToFloat(constant.Imag(v)))
		w.WriteString(")")
	default:
		w.WriteString("nil")
	}
}

// float writes a float constant: (:float R) or (:float M E).
func (w out) float(v constant.Value) {
	w.WriteString("(:float ")
	switch x := constant.Val(v).(type) {
	case int64:
		w.int(x)
	case *big.Int:
		w.WriteString(x.String())
	case *big.Rat:
		w.rat(x)
	case *big.Float:
		if exp := x.MantExp(nil); exp > maxRatBits || exp < -maxRatBits {
			// x = mant · 2^exp with 0.5 <= |mant| < 1 and at most prec bits: an integer
			// after shifting by prec
			mant := new(big.Float)
			exp = x.MantExp(mant)
			prec := int(x.MinPrec())
			m, _ := mant.SetMantExp(mant, prec).Int(nil)
			e := exp - prec
			if tz := m.TrailingZeroBits(); tz > 0 {
				m.Rsh(m, tz)
				e += int(tz)
			}
			w.WriteString(m.String())
			w.WriteByte(' ')
			w.int(int64(e))
		} else {
			r, _ := x.Rat(nil)
			w.rat(r)
		}
	default:
		w.WriteString("nil")
	}
	w.WriteString(")")
}
func (w out) rat(r *big.Rat) {
	if r.IsInt() {
		w.WriteString(r.Num().String())
	} else {
		w.WriteString(r.String())
	}
}
