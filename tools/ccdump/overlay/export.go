// Arbace's exporter, laid into cc's package at build time by `go build -overlay` (bin/repr-c
// ccdump-build): cc's preprocessor and scanner give their tokens only to cc's parser, so the helper
// reaches them through this file, which calls cc's own functions and copies none of cc's code. It is
// Arbace's (EPL-1.0, LICENSE.md); the checkout it is laid into stays as it is (doc/C23-SURVEY.md, C1.3,
// "As built: C2").

package cc

import "fmt"

// ArbaceLex calls f with each preprocessing token of src as cc's scanner gives them (cppScan: the
// line-start, '#' and header-name states, each line's '\n'), and returns the scanner's errors.
func ArbaceLex(name string, src []byte, f func(t Token)) (errs []string, err error) {
	eh := func(msg string, args ...interface{}) { errs = append(errs, fmt.Sprintf(msg, args...)) }
	s, err := newScanner(newFset(), Source{name, src, nil}, eh)
	if err != nil {
		return nil, err
	}
	for {
		t := s.cppScan()
		if t.Ch == eof {
			return errs, nil
		}
		f(t)
	}
}

// ArbacePreprocess calls f with each token of the preprocessed stream, as Preprocess writes them (the
// separators' ' ' tokens and each line's '\n' among them), and the name of the object-like macro whose
// expansion made it ("" for none: cc records only those, Token.m), and returns the preprocessor's
// errors.
func ArbacePreprocess(cfg *Config, sources []Source, f func(t Token, macro string)) (errs []string, err error) {
	c, err := newCPP(cfg, newFset(), sources, nil)
	if err != nil {
		return nil, err
	}
	c.eh = func(msg string, args ...interface{}) { errs = append(errs, fmt.Sprintf(msg, args...)) }
	for {
		if c.rune() == eof {
			return errs, nil
		}
		t := c.shift()
		m := ""
		if t.m != nil {
			m = t.m.Name.SrcStr()
		}
		f(t, m)
	}
}
