//go:build linux

// +build linux

//go:debug panicnil=1

package vp

import "log/slog"

// Log misses a value.
func Log() { slog.Info("msg", "key") }
