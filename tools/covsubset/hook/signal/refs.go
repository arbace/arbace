//go:build arbacecover

// The cover build's second hook file (doc/GATE-SURVEY.md, GT8.3; bin/lib/cover.bash's cover_go_flags): added to Go's
// os/signal by an -overlay entry of the cover builds alone, built with -tags arbacecover, never part of a shipped
// build. It tells tools/covsubset/hook/cover.go (internal/coverage/cfile's hook) whether the program has signal
// handlers of its own (review 182 L3): os/signal keeps no other account of them that another package can read.
package signal

import "os"

// ArbaceCoverRefs: the number of channels that receive sig (signal.Notify's registrations not stopped, the hook's
// own among them); with sig nil, the number of registrations of every signal summed.
func ArbaceCoverRefs(sig os.Signal) int64 {
	handlers.Lock()
	defer handlers.Unlock()
	if sig == nil {
		var n int64
		for _, r := range handlers.ref {
			n += r
		}
		return n
	}
	n := signum(sig)
	if n < 0 {
		return 0
	}
	return handlers.ref[n]
}
