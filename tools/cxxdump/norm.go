package main

// The normal form of a kept tree for the AST level of the round trip (doc/cpp/SPEC.md §3, level 3): the print's
// AST equals the original's modulo positions and the dump's per-run ids. Removed: every node's id, loc and
// range; isReferenced; the keys whose value is another node's id; the ids inside the declaration references; and in strings
// the positions clang writes into the names of unnamed entities ("(lambda at a.cpp:6:20)", "(unnamed struct at
// ...)"), masked as "at *".

import "regexp"

var (
	reAt = regexp.MustCompile(` at [^ ()]*:[0-9]+:[0-9]+`)
	reID = regexp.MustCompile(`^0x[0-9a-f]+$`)
)

var dropKeys = map[string]bool{"id": true, "loc": true, "range": true, "previousDecl": true, "parentDeclContextId": true,
	"referencedMemberDecl": true,
	"isReferenced":         true} // clang's bookkeeping of a name lookup (an injected class name, a typedef used in a text form), not structure: the references themselves are kept (CP60)

func normalize(v any) any {
	switch x := v.(type) {
	case *obj:
		keys, vals := x.keys[:0:0], x.vals[:0:0]
		tparm := x.str("kind") == "TemplateTypeParmDecl"
		for i, k := range x.keys {
			if dropKeys[k] || (tparm && k == "tagUsed") { // class and typename are one (doc/cpp/SPEC.md N8)
				continue
			}
			if s, ok := x.vals[i].(string); ok && reID.MatchString(s) {
				continue
			}
			keys = append(keys, k)
			vals = append(vals, normalize(x.vals[i]))
		}
		x.keys, x.vals = keys, vals
		return x
	case []any:
		for i := range x {
			x[i] = normalize(x[i])
		}
		return x
	case string:
		return reAt.ReplaceAllString(x, " at *")
	}
	return v
}
