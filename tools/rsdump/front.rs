// RF4-RF5 (doc/rust/FRONTEND-SURVEY.md, "As built: RF4-RF5 start"): the oracles of the front half's first
// stages, written anew for Arbace on rustc_driver's callbacks (rustc 1.99.1, MIT or Apache-2.0; the
// queries and types read: compiler/rustc_middle/src/metadata.rs (ModChild), rustc_hir/src/intravisit.rs,
// rustc_metadata/src/creader.rs (LoadedMacro), rustc_interface's after_expansion):
//
//   --expanded FILE   the crate after expansion (`tcx.resolver_for_lowering()`'s crate), through the same
//                     Debug transcoding as `dump` (spans of other files as `(span "FILE" l1 c1 l2 c2 ctxt)`)
//   --resolve FILE    every HIR path written in the root file (not from an expansion) with its `Res`:
//                     `(res [l1 c1 l2 c2] [RES...])`, a use path's resolutions in each namespace; RES
//                     `(def KIND "crate::path")` for another crate's item, `(local-def KIND [ident span])` for
//                     the crate's own, `(local [binding's ident span])`, `(prim "u8")`, `(self-ty-alias [l c])`
//                     (the impl's start), `(self-ty-param [l c])` (the trait's start), `(self-ctor [l c])`,
//                     `(attr KIND)`, `(tool-mod)`, `(err)`
//   --std-names FILE  std's, core's and alloc's module trees as rustc's metadata gives them (`module_children`,
//                     RFS4): `(names/mod "crate::path" [(NS "name" KIND "crate::path") ...])` per module
//                     reached (each once), public children only; `(names/macro "crate::path" {:builtin B
//                     :edition E :attrs [...] :macro-rules B} BODY)` per public bang macro, BODY its
//                     `MacroDef` transcoded (`load_macro_untracked`)

use rustc_hir::def::{DefKind, Res};
use rustc_hir::intravisit::{self, Visitor};
use rustc_middle::ty::TyCtxt;
use rustc_span::def_id::DefId;
use rustc_span::Span;
use std::fmt::Write;

fn q(s: &str) -> String {
    crate::clj_str(s)
}

/// `[l1 c1 l2 c2]` of a span in the root file, None elsewhere
fn span_vec(tcx: TyCtxt<'_>, root: &str, sp: Span) -> Option<String> {
    let s = tcx.sess.source_map().span_to_diagnostic_string(sp);
    let rest = s.strip_prefix(root)?.strip_prefix(':')?;
    let nums: Vec<&str> = rest.split(|c: char| c == ':' || c == ' ').filter(|x| !x.is_empty()).collect();
    if nums.len() < 4 {
        return None;
    }
    Some(format!("[{} {} {} {}]", nums[0], nums[1], nums[2], nums[3]))
}

fn span_lo(tcx: TyCtxt<'_>, root: &str, sp: Span) -> String {
    match span_vec(tcx, root, sp) {
        Some(v) => {
            let ns: Vec<&str> = v.trim_matches(|c| c == '[' || c == ']').split(' ').collect();
            format!("[{} {}]", ns[0], ns[1])
        }
        None => "nil".into(),
    }
}

fn root_name_at(tcx: TyCtxt<'_>, sp: Span) -> String {
    let s = tcx.sess.source_map().span_to_diagnostic_string(sp);
    s.rsplitn(5, ':').last().unwrap_or("").to_string()
}

pub fn def_path(tcx: TyCtxt<'_>, d: DefId) -> String {
    format!("{}{}", tcx.crate_name(d.krate), tcx.def_path(d).to_string_no_crate_verbose())
}

fn kind(k: DefKind) -> String {
    q(&format!("{k:?}"))
}

fn res_form<'tcx>(tcx: TyCtxt<'tcx>, root: &str, r: &Res) -> String {
    match *r {
        Res::Def(k, d) => {
            if let Some(l) = d.as_local() {
                let id = match k {
                    DefKind::Ctor(..) => tcx.def_ident_span(tcx.parent(d)),
                    _ => tcx.def_ident_span(d),
                };
                let sp = id.and_then(|s| span_vec(tcx, root, s)).unwrap_or_else(|| span_lo(tcx, root, tcx.def_span(l)));
                format!("(local-def {} {})", kind(k), sp)
            } else {
                format!("(def {} {})", kind(k), q(&def_path(tcx, d)))
            }
        }
        Res::PrimTy(p) => format!("(prim {})", q(p.name_str())),
        Res::SelfTyParam { trait_ } => format!("(self-ty-param {})", span_lo(tcx, root, tcx.def_span(trait_))),
        Res::SelfTyAlias { alias_to, .. } => format!("(self-ty-alias {})", span_lo(tcx, root, tcx.def_span(alias_to))),
        Res::SelfCtor(d) => format!("(self-ctor {})", span_lo(tcx, root, tcx.def_span(d))),
        Res::Local(h) => {
            let sp = match tcx.hir_node(h) {
                rustc_hir::Node::Pat(p) => match p.kind {
                    rustc_hir::PatKind::Binding(_, _, ident, _) => ident.span,
                    _ => p.span,
                },
                _ => tcx.hir_span(h),
            };
            format!("(local {})", span_vec(tcx, root, sp).unwrap_or_else(|| "nil".into()))
        }
        Res::ToolMod => "(tool-mod)".into(),
        Res::NonMacroAttr(k) => format!("(attr {})", q(&format!("{k:?}"))),
        Res::OpenMod(s) => format!("(open-mod {})", q(s.as_str())),
        Res::Err => "(err)".into(),
    }
}

struct Paths<'tcx> {
    tcx: TyCtxt<'tcx>,
    root: String,
    out: std::collections::BTreeMap<String, Vec<String>>,
}

impl<'tcx> Visitor<'tcx> for Paths<'tcx> {
    type NestedFilter = rustc_middle::hir::nested_filter::All;
    fn maybe_tcx(&mut self) -> Self::MaybeTyCtxt {
        self.tcx
    }
    fn visit_path(&mut self, path: &rustc_hir::Path<'tcx>, _id: rustc_hir::HirId) {
        if !path.span.from_expansion() {
            if let Some(sv) = span_vec(self.tcx, &self.root, path.span) {
                let r = res_form(self.tcx, &self.root, &path.res);
                let v = self.out.entry(sv).or_default();
                if !v.contains(&r) {
                    v.push(r);
                }
            }
        }
        intravisit::walk_path(self, path)
    }
}

/// `--resolve FILE`
pub fn resolve<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    let root = root_name_at(tcx, tcx.hir_span(rustc_hir::CRATE_HIR_ID));
    let mut p = Paths { tcx, root, out: Default::default() };
    tcx.hir_walk_toplevel_module(&mut p);
    for id in tcx.hir_crate_items(()).free_items() {
        let _ = id;
    }
    let mut o = String::new();
    // positions in source order: [l1 c1 l2 c2] sorted numerically
    let mut keys: Vec<(Vec<u64>, String)> = p
        .out
        .keys()
        .map(|k| (k.trim_matches(|c| c == '[' || c == ']').split(' ').map(|n| n.parse().unwrap_or(0)).collect(), k.clone()))
        .collect();
    keys.sort();
    for (_, k) in keys {
        let mut v = p.out[&k].clone();
        v.sort();
        let _ = writeln!(o, "(res {} [{}])", k, v.join(" "));
    }
    o
}

/// `--expanded FILE`
pub fn expanded<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    let krate = tcx.resolver_for_lowering().1.borrow();
    let root = root_name_at(tcx, krate.spans.inner_span);
    let text = format!("{:?}", &*krate);
    crate::transcode_any(&text, &root)
}

fn ns_name(k: DefKind) -> &'static str {
    match k.ns() {
        Some(rustc_hir::def::Namespace::TypeNS) => "type",
        Some(rustc_hir::def::Namespace::ValueNS) => "value",
        Some(rustc_hir::def::Namespace::MacroNS) => "macro",
        None => "none",
    }
}

/// `--std-names FILE`
pub fn std_names<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    let mut o = String::new();
    let mut seen = std::collections::HashSet::new();
    let mut macros: Vec<DefId> = Vec::new();
    let mut gen_seen = std::collections::HashSet::new();
    let mut gens: Vec<String> = Vec::new();
    let mut todo: Vec<DefId> = Vec::new();
    for &cnum in tcx.crates(()) {
        let n = tcx.crate_name(cnum);
        if matches!(n.as_str(), "std" | "core" | "alloc") {
            todo.push(cnum.as_def_id());
        }
    }
    todo.sort_by_key(|d| def_path(tcx, *d));
    todo.reverse();
    while let Some(m) = todo.pop() {
        if !seen.insert(m) {
            continue;
        }
        let mut kids = Vec::new();
        let mut next = Vec::new();
        for c in tcx.module_children(m) {
            if !c.vis.is_public() {
                continue;
            }
            let Res::Def(k, d) = c.res else {
                continue;
            };
            kids.push(format!("({} {} {} {})", ns_name(k), q(c.ident.name.as_str()), kind(k), q(&def_path(tcx, d))));
            // RFP10: each type's generic parameters (kind and name), for elided lifetimes in signatures (`Formatter<'_>`)
            if matches!(k, DefKind::Struct | DefKind::Enum | DefKind::Union | DefKind::TyAlias | DefKind::Trait) && gen_seen.insert(d) {
                let ps: Vec<String> = tcx
                    .generics_of(d)
                    .own_params
                    .iter()
                    .map(|p| {
                        let pk = match p.kind {
                            rustc_middle::ty::GenericParamDefKind::Lifetime => "lifetime",
                            rustc_middle::ty::GenericParamDefKind::Type { .. } => "type",
                            rustc_middle::ty::GenericParamDefKind::Const { .. } => "const",
                        };
                        format!("({} {})", pk, q(p.name.as_str()))
                    })
                    .collect();
                gens.push(format!("(names/generics {} [{}])", q(&def_path(tcx, d)), ps.join(" ")));
            }
            match k {
                DefKind::Mod | DefKind::Enum | DefKind::Trait => next.push(d),
                DefKind::Macro(_) => {
                    if !macros.contains(&d) { macros.push(d); }
                }
                _ => {}
            }
        }
        let _ = writeln!(o, "(names/mod {} {} [{}])", q(&def_path(tcx, m)), kind(tcx.def_kind(m)), kids.join(" "));
        next.reverse();
        todo.extend(next);
    }
    gens.sort();
    for g in gens {
        let _ = writeln!(o, "{g}");
    }
    let cstore = rustc_metadata::creader::CStore::from_tcx(tcx);
    for d in macros {
        if !matches!(tcx.def_kind(d), DefKind::Macro(k) if k.contains(rustc_hir::def::MacroKinds::BANG)) {
            continue;
        }
        if let rustc_metadata::creader::LoadedMacro::MacroDef { def, attrs, edition, .. } = cstore.load_macro_untracked(tcx, d) {
            let at = format!("{attrs:?}");
            let builtin = at.contains("rustc_builtin_macro") || at.contains("RustcBuiltinMacro");
            let transp = if at.contains("semiopaque") || at.contains("SemiOpaque") || at.contains("semitransparent") || at.contains("SemiTransparent") {
                "semiopaque"
            } else if at.contains("\"transparent\"") || at.contains("Transparent") {
                "transparent"
            } else if def.macro_rules {
                "semiopaque"
            } else {
                "opaque"
            };
            let body = crate::transcode_any(&format!("{:?}", def), "");
            let _ = writeln!(
                o,
                "(names/macro {} {{:builtin {} :edition {} :macro-rules {} :transparency :{}}} {})",
                q(&def_path(tcx, d)),
                builtin,
                q(&format!("{edition}")),
                def.macro_rules,
                transp,
                body
            );
        }
    }
    o
}
