//go:build !purego

//go:debug panicnil=1

// Package main tests the file header (SPEC §4.3): build lines, header directives, the
// package doc and the import specs.
package main

import (
	"errors"
	_ "embed"
	. "math"
	r "math/rand"
	str "strings"
)

import "fmt"

var _ = errors.New
var _ = Pi
var _ = r.Int
var _ = str.ToUpper

func main() { fmt.Println() }
