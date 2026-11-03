//go:build arbacecover

// The cover build's third hook file (doc/GATE-SURVEY.md, GT8.3; bin/lib/cover.bash's cover_go_flags): added to Go's
// runtime by an -overlay entry of the cover builds alone, built with -tags arbacecover, never part of a shipped
// build. It pushes the one fact of the scheduler tools/covsubset/hook/cover.go's deadlock detector needs (review
// 182 L2), which the linker lets no other package pull: whether a timer is pending.
package runtime

import _ "unsafe" // go:linkname

// arbaceCoverTimeSleepUntil: the time the next timer of any P fires (timeSleepUntil), maxWhen when none is pending
//
//go:linkname arbaceCoverTimeSleepUntil
func arbaceCoverTimeSleepUntil() int64 { return timeSleepUntil() }
