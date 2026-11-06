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

/// RF7: a type as data, for std's defaults, aliases and supertraits: `(param I "N")`, `(prim "u8")`, `(adt "PATH" [ARGS])`,
/// `(tuple [..])`, `(ref "'a" MUT T)`, `(ptr MUT T)`, `(slice T)`, `(array T "LEN")`, `(projection "PATH" [ARGS])`, `(never)`,
/// else `(text "...")`; arguments as types, `(lt "'a")` or `(const "TEXT")`
fn sty<'tcx>(tcx: TyCtxt<'tcx>, t: rustc_middle::ty::Ty<'tcx>) -> String {
    use rustc_middle::ty;
    let args = |a: ty::GenericArgsRef<'tcx>| a.iter().map(|x| sarg(tcx, x)).collect::<Vec<_>>().join(" ");
    match *t.kind() {
        ty::Bool => "(prim \"bool\")".into(),
        ty::Char => "(prim \"char\")".into(),
        ty::Str => "(prim \"str\")".into(),
        ty::Int(i) => format!("(prim {})", q(i.name_str())),
        ty::Uint(i) => format!("(prim {})", q(i.name_str())),
        ty::Float(i) => format!("(prim {})", q(i.name_str())),
        ty::Never => "(never)".into(),
        ty::Param(p) => format!("(param {} {})", p.index, q(p.name.as_str())),
        ty::Adt(d, a) => format!("(adt {} [{}])", q(&def_path(tcx, d.did())), args(a)),
        ty::Tuple(ts) => format!("(tuple [{}])", ts.iter().map(|x| sty(tcx, x)).collect::<Vec<_>>().join(" ")),
        ty::Ref(r, e, m) => format!("(ref {} {} {})", q(&region_text(r)), m.is_mut(), sty(tcx, e)),
        ty::RawPtr(e, m) => format!("(ptr {} {})", m.is_mut(), sty(tcx, e)),
        ty::Slice(e) => format!("(slice {})", sty(tcx, e)),
        ty::Array(e, n) => match n.kind() {
            ty::ConstKind::Param(p) => format!("(array {} (cparam {} {}))", sty(tcx, e), p.index, q(p.name.as_str())),
            _ => format!("(array {} {})", sty(tcx, e), q(&n.to_string())),
        },
        ty::Alias(_, ty::AliasTy { kind: ty::Projection { def_id }, args: aa, .. }) => format!("(projection {} [{}])", q(&def_path(tcx, def_id)), args(aa)),
        // RF9's: opaque types, fn pointers, trait objects
        ty::Alias(_, ty::AliasTy { kind: ty::Opaque { def_id }, args: aa, .. }) => format!("(opaque {} [{}])", q(&def_path(tcx, def_id)), args(aa)),
        ty::FnPtr(sig_tys, hdr) => {
            let s = sig_tys.with(hdr).skip_binder();
            format!("(fnptr [{}] {})", s.inputs().iter().map(|x| sty(tcx, *x)).collect::<Vec<_>>().join(" "), sty(tcx, s.output()))
        }
        ty::Dynamic(preds, r) => {
            let pr = preds.principal().map(|p| {
                let p = p.skip_binder();
                format!("(trait {} [{}])", q(&def_path(tcx, p.def_id)), args(p.args))
            }).unwrap_or_else(|| "nil".into());
            let projs: Vec<String> = preds.projection_bounds().map(|p| {
                let p = p.skip_binder();
                format!("(proj {} [{}] {})", q(&def_path(tcx, p.def_id)), args(p.args), p.term.as_type().map(|t| sty(tcx, t)).unwrap_or_else(|| "(text \"const\")".into()))
            }).collect();
            let autos: Vec<String> = preds.auto_traits().map(|d| q(&def_path(tcx, d))).collect();
            format!("(dyn {} [{}] [{}] {})", pr, projs.join(" "), autos.join(" "), q(&r.to_string()))
        }
        _ => format!("(text {})", q(&t.to_string())),
    }
}

/// A region's text, a bound (late-bound) one marked `'^` (then its name when it has one)
fn region_text(r: rustc_middle::ty::Region<'_>) -> String {
    if let rustc_middle::ty::ReBound(..) = r.kind() {
        let t = r.to_string();
        if t == "'_" || t.is_empty() { "'^".into() } else { format!("'^{t}") }
    } else {
        r.to_string()
    }
}

fn sarg<'tcx>(tcx: TyCtxt<'tcx>, a: rustc_middle::ty::GenericArg<'tcx>) -> String {
    use rustc_middle::ty::GenericArgKind;
    match a.kind() {
        GenericArgKind::Type(t) => sty(tcx, t),
        GenericArgKind::Lifetime(r) => format!("(lt {})", q(&region_text(r))),
        GenericArgKind::Const(c) => format!("(const {})", q(&c.to_string())),
    }
}

/// `--std-names FILE`
pub fn std_names<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    let mut o = String::new();
    let mut seen = std::collections::HashSet::new();
    let mut macros: Vec<DefId> = Vec::new();
    let mut gen_seen = std::collections::HashSet::new();
    let mut gens: Vec<String> = Vec::new();
    let mut extra: Vec<String> = Vec::new();
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
            // RF7: a re-export that is itself #[doc(hidden)] (visible_parent_map's fallback)
            if c.reexport_chain.first().and_then(|r| r.id()).is_some_and(|id| tcx.is_doc_hidden(id)) {
                extra.push(format!("(names/hidden-reexport {} {})", q(&def_path(tcx, m)), q(c.ident.name.as_str())));
            }
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
                        // RF7: a parameter's default, as a structural type (own_args_no_defaults, the defaults HIR type lowering fills)
                        match p.default_value(tcx) {
                            Some(dv) => format!("({} {} {})", pk, q(p.name.as_str()), sarg(tcx, dv.skip_binder())),
                            None => format!("({} {})", pk, q(p.name.as_str())),
                        }
                    })
                    .collect();
                gens.push(format!("(names/generics {} [{}])", q(&def_path(tcx, d)), ps.join(" ")));
                // RF7: a type alias's type (free aliases are expanded by HIR type lowering), a trait's supertraits and flags
                if k == DefKind::TyAlias {
                    gens.push(format!("(names/alias {} {})", q(&def_path(tcx, d)), sty(tcx, tcx.type_of(d).skip_binder())));
                }
                if k == DefKind::Trait {
                    let sups: Vec<String> = tcx
                        .explicit_super_clauses_of(d)
                        .skip_binder()
                        .iter()
                        .filter_map(|(c, _)| c.as_trait_clause())
                        .map(|t| t.skip_binder())
                        .filter(|t| t.polarity == rustc_middle::ty::PredicatePolarity::Positive)
                        .map(|t| format!("(trait {} [{}])", q(&def_path(tcx, t.trait_ref.def_id)), t.trait_ref.args.iter().map(|a| sarg(tcx, a)).collect::<Vec<_>>().join(" ")))
                        .collect();
                    gens.push(format!(
                        "(names/trait {} {{:supertraits [{}] :paren-sugar {} :auto {}}})",
                        q(&def_path(tcx, d)), sups.join(" "), tcx.trait_def(d).paren_sugar, tcx.trait_is_auto(d)
                    ));
                }
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
        if tcx.is_doc_hidden(m) {
            extra.push(format!("(names/hidden {})", q(&def_path(tcx, m))));
        }
        next.reverse();
        todo.extend(next);
    }
    gens.sort();
    // RF7: the lang items (the sizedness traits HIR type lowering adds, the Fn family, ...)
    for (l, d) in tcx.lang_items().iter() {
        gens.push(format!("(names/lang {} {})", q(l.name().as_str()), q(&def_path(tcx, d))));
    }
    gens.extend(extra);
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

/// RF9's oracle, `--typeck FILE`: each typeck root's TypeckResults over the root file's HIR (not an expansion's):
/// `(tc/expr OWNER [l1 c1 l2 c2] KIND "TYPE" {:adj [...] :method "PATH" :args "ARGS"})` per expression (its node type
/// before adjustments, the adjustments' kinds and targets, a type-dependent definition with its arguments) and
/// `(tc/pat OWNER [span] "TYPE" MODE)` per binding (its type and binding mode)
pub fn typeck<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    use rustc_middle::ty::print::with_no_trimmed_paths;
    struct Tc<'a, 'tcx> {
        tcx: TyCtxt<'tcx>,
        root: String,
        owner: String,
        res: &'a rustc_middle::ty::TypeckResults<'tcx>,
        out: String,
    }
    impl<'a, 'tcx> Visitor<'tcx> for Tc<'a, 'tcx> {
        type NestedFilter = rustc_middle::hir::nested_filter::OnlyBodies;
        fn maybe_tcx(&mut self) -> Self::MaybeTyCtxt {
            self.tcx
        }
        fn visit_expr(&mut self, e: &'tcx rustc_hir::Expr<'tcx>) {
            if !e.span.from_expansion() {
                if let Some(sv) = span_vec(self.tcx, &self.root, e.span) {
                    let kd = format!("{:?}", e.kind);
                    let kind: String = kd.chars().take_while(|c| c.is_alphanumeric()).collect();
                    let ty = self.res.node_type_opt(e.hir_id).map(|t| with_no_trimmed_paths!(t.to_string())).unwrap_or_else(|| "?".into());
                    let adj = self
                        .res
                        .expr_adjustments(e)
                        .iter()
                        .map(|a| {
                            let k = format!("{:?}", a.kind);
                            let k: String = k.chars().take_while(|c| c.is_alphanumeric()).collect();
                            format!("[{} {}]", q(&k), q(&with_no_trimmed_paths!(a.target.to_string())))
                        })
                        .collect::<Vec<_>>()
                        .join(" ");
                    let mut extra = format!("{{:adj [{adj}]");
                    if let Some(Ok((_, d))) = self.res.type_dependent_defs().get(e.hir_id) {
                        let args = self.res.node_args(e.hir_id);
                        let _ = write!(extra, " :method {} :args {}", q(&with_no_trimmed_paths!(self.tcx.def_path_str(*d))), q(&with_no_trimmed_paths!(format!("{:?}", args))));
                    }
                    extra.push('}');
                    let _ = writeln!(self.out, "(tc/expr {} {} {} {} {})", q(&self.owner), sv, q(&kind), q(&ty), extra);
                }
            }
            intravisit::walk_expr(self, e)
        }
        fn visit_pat(&mut self, p: &'tcx rustc_hir::Pat<'tcx>) {
            if let rustc_hir::PatKind::Binding(..) = p.kind {
                if !p.span.from_expansion() {
                    if let Some(sv) = span_vec(self.tcx, &self.root, p.span) {
                        let ty = self.res.node_type_opt(p.hir_id).map(|t| with_no_trimmed_paths!(t.to_string())).unwrap_or_else(|| "?".into());
                        let mode = self.res.pat_binding_modes().get(p.hir_id).map(|m| format!("{m:?}")).unwrap_or_default();
                        let _ = writeln!(self.out, "(tc/pat {} {} {} {})", q(&self.owner), sv, q(&ty), q(&mode));
                    }
                }
            }
            intravisit::walk_pat(self, p)
        }
    }
    let root = root_name_at(tcx, tcx.hir_span(rustc_hir::CRATE_HIR_ID));
    let mut out = String::new();
    for def in tcx.hir_body_owners() {
        if tcx.is_typeck_child(def.to_def_id()) {
            continue;
        }
        let res = tcx.typeck(def);
        let body = tcx.hir_body_owned_by(def);
        let owner = with_no_trimmed_paths!(tcx.def_path_str(def.to_def_id()));
        let mut v = Tc { tcx, root: root.clone(), owner, res, out: String::new() };
        v.visit_body(body);
        out.push_str(&v.out);
    }
    out
}

/// RF9's std interface (RFS4, Tier U's data), `--std-iface FILE`: what type checking asks of std, core and alloc, as
/// structural types (`sty`): each reachable ADT's variants and fields `(si/adt PATH {..})`, trait `(si/trait PATH {..})`
/// with its items, impl (inherent ones of the ADTs and the primitives, the traits' impls in these crates)
/// `(si/impl PATH {:trait TR :self TY :generics [..] :predicates [..] :items [..]})` and function or method
/// `(si/fn PATH {:generics [..] :parent-count N :inputs [..] :output TY :predicates [..]})`; core::arch and the SIMD
/// modules are left out
pub fn std_iface<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    use rustc_middle::ty::{self, fast_reject::SimplifiedType};
    let std_crate = |d: DefId| matches!(tcx.crate_name(d.krate).as_str(), "std" | "core" | "alloc");
    let skip = |p: &str| p.contains("::arch") || p.contains("simd") || p.contains("core_arch");
    let clause = |c: ty::Clause<'tcx>| -> String {
        match c.kind().skip_binder() {
            ty::ClauseKind::Trait(t) => format!("(trait {} [{}])", q(&def_path(tcx, t.def_id())), t.trait_ref.args.iter().map(|a| sarg(tcx, a)).collect::<Vec<_>>().join(" ")),
            ty::ClauseKind::Projection(p) => format!(
                "(proj {} [{}] {})",
                q(&def_path(tcx, p.def_id())),
                p.projection_term.args.iter().map(|a| sarg(tcx, a)).collect::<Vec<_>>().join(" "),
                p.term.as_type().map(|t| sty(tcx, t)).unwrap_or_else(|| "(text \"const\")".into())
            ),
            ty::ClauseKind::TypeOutlives(o) => format!("(type-outlives {} {})", sty(tcx, o.0), q(&o.1.to_string())),
            _ => format!("(other {})", q(&c.to_string())),
        }
    };
    let generics = |d: DefId| -> String {
        let g = tcx.generics_of(d);
        let ps: Vec<String> = g
            .own_params
            .iter()
            .map(|p| {
                let pk = match p.kind {
                    ty::GenericParamDefKind::Lifetime => "lifetime",
                    ty::GenericParamDefKind::Type { .. } => "type",
                    ty::GenericParamDefKind::Const { .. } => "const",
                };
                match p.default_value(tcx) {
                    Some(dv) => format!("({} {} {})", pk, q(p.name.as_str()), sarg(tcx, dv.skip_binder())),
                    None => format!("({} {})", pk, q(p.name.as_str())),
                }
            })
            .collect();
        format!(":generics [{}] :parent-count {} :has-self {}", ps.join(" "), g.parent_count, g.has_self)
    };
    let preds = |d: DefId| -> String {
        let cs = tcx.explicit_clauses_of(d).clauses;
        format!(":predicates [{}]", cs.iter().map(|(c, _)| clause(*c)).collect::<Vec<_>>().join(" "))
    };
    let mut out = String::new();
    let mut fns: Vec<DefId> = Vec::new();
    let mut impls: Vec<DefId> = Vec::new();
    let mut seen = std::collections::HashSet::new();
    let mut todo: Vec<DefId> = Vec::new();
    for &cnum in tcx.crates(()) {
        if matches!(tcx.crate_name(cnum).as_str(), "std" | "core" | "alloc") {
            todo.push(cnum.as_def_id());
        }
    }
    let mut items: Vec<(DefKind, DefId)> = Vec::new();
    while let Some(m) = todo.pop() {
        if !seen.insert(m) {
            continue;
        }
        for c in tcx.module_children(m) {
            if !c.vis.is_public() {
                continue;
            }
            let Res::Def(k, d) = c.res else { continue };
            if !std_crate(d) || skip(&def_path(tcx, d)) {
                continue;
            }
            match k {
                DefKind::Mod => todo.push(d),
                DefKind::Struct | DefKind::Enum | DefKind::Union | DefKind::Trait | DefKind::Fn => items.push((k, d)),
                _ => {}
            }
        }
    }
    items.sort_by_key(|(_, d)| def_path(tcx, *d));
    items.dedup_by_key(|(_, d)| *d);
    for (k, d) in &items {
        let p = def_path(tcx, *d);
        match k {
            DefKind::Struct | DefKind::Enum | DefKind::Union => {
                let a = tcx.adt_def(*d);
                let vs: Vec<String> = a
                    .variants()
                    .iter()
                    .map(|v| {
                        let ctor = match v.ctor_kind() {
                            Some(rustc_hir::def::CtorKind::Fn) => "\"Fn\"",
                            Some(rustc_hir::def::CtorKind::Const) => "\"Const\"",
                            None => "nil",
                        };
                        let fs: Vec<String> = v.fields.iter().map(|f| format!("[{} {}]", q(f.name.as_str()), sty(tcx, tcx.type_of(f.did).skip_binder()))).collect();
                        format!("{{:name {} :path {} :ctor {} :fields [{}]}}", q(v.name.as_str()), q(&def_path(tcx, v.def_id)), ctor, fs.join(" "))
                    })
                    .collect();
                let _ = writeln!(out, "(si/adt {} {{:kind {} {} :variants [{}]}})", q(&p), kind(*k), generics(*d), vs.join(" "));
                impls.extend(tcx.inherent_impls(*d).iter().copied().filter(|i| std_crate(*i)));
            }
            DefKind::Trait => {
                let its: Vec<String> = tcx
                    .associated_items(*d)
                    .in_definition_order()
                    .filter(|i| i.opt_name().is_some())
                    .map(|i| {
                        if matches!(i.kind, ty::AssocKind::Fn { .. }) {
                            fns.push(i.def_id);
                        }
                        format!("[{} {} {}]", q(i.name().as_str()), kind(tcx.def_kind(i.def_id)), q(&def_path(tcx, i.def_id)))
                    })
                    .collect();
                let ti = tcx.trait_impls_of(*d);
                impls.extend(ti.blanket_impls().iter().copied().filter(|i| std_crate(*i)));
                for v in ti.non_blanket_impls().values() {
                    impls.extend(v.iter().copied().filter(|i| std_crate(*i)));
                }
                let _ = writeln!(out, "(si/trait {} {{{} {} :items [{}]}})", q(&p), generics(*d), preds(*d), its.join(" "));
            }
            DefKind::Fn => fns.push(*d),
            _ => {}
        }
    }
    use rustc_ast::{FloatTy, IntTy, UintTy};
    let mut prims = vec![SimplifiedType::Bool, SimplifiedType::Char, SimplifiedType::Str, SimplifiedType::Slice, SimplifiedType::Array, SimplifiedType::Never];
    for i in [IntTy::Isize, IntTy::I8, IntTy::I16, IntTy::I32, IntTy::I64, IntTy::I128] {
        prims.push(SimplifiedType::Int(i));
    }
    for u in [UintTy::Usize, UintTy::U8, UintTy::U16, UintTy::U32, UintTy::U64, UintTy::U128] {
        prims.push(SimplifiedType::Uint(u));
    }
    for f in [FloatTy::F32, FloatTy::F64] {
        prims.push(SimplifiedType::Float(f));
    }
    prims.push(SimplifiedType::Ptr(rustc_ast::Mutability::Not));
    prims.push(SimplifiedType::Ptr(rustc_ast::Mutability::Mut));
    for s in prims {
        impls.extend(tcx.incoherent_impls(s).iter().copied().filter(|i| std_crate(*i)));
    }
    impls.sort_by_key(|d| def_path(tcx, *d));
    impls.dedup();
    for i in impls {
        let p = def_path(tcx, i);
        if skip(&p) {
            continue;
        }
        let tr = tcx
            .impl_opt_trait_ref(i)
            .map(|t| {
                let t = t.skip_binder();
                format!("(trait {} [{}])", q(&def_path(tcx, t.def_id)), t.args.iter().map(|a| sarg(tcx, a)).collect::<Vec<_>>().join(" "))
            })
            .unwrap_or_else(|| "nil".into());
        let its: Vec<String> = tcx
            .associated_items(i)
            .in_definition_order()
            .filter(|it| it.opt_name().is_some())
            .map(|it| {
                let k = tcx.def_kind(it.def_id);
                if matches!(it.kind, ty::AssocKind::Fn { .. }) {
                    fns.push(it.def_id);
                }
                // an associated type's value, an associated constant's type
                let val = if matches!(k, DefKind::AssocTy | DefKind::AssocConst { .. }) { sty(tcx, tcx.type_of(it.def_id).skip_binder()) } else { "nil".into() };
                format!("[{} {} {} {}]", q(it.name().as_str()), kind(k), q(&def_path(tcx, it.def_id)), val)
            })
            .collect();
        let _ = writeln!(
            out,
            "(si/impl {} {{:trait {} :negative {} :self {} {} {} :items [{}]}})",
            q(&p),
            tr,
            tcx.impl_is_of_trait(i) && matches!(tcx.impl_polarity(i), ty::ImplPolarity::Negative),
            sty(tcx, tcx.type_of(i).skip_binder()),
            generics(i),
            preds(i),
            its.join(" ")
        );
    }
    fns.sort_by_key(|d| def_path(tcx, *d));
    fns.dedup();
    for f in fns {
        let p = def_path(tcx, f);
        if skip(&p) {
            continue;
        }
        let sig = tcx.fn_sig(f).skip_binder().skip_binder();
        let ins: Vec<String> = sig.inputs().iter().map(|t| sty(tcx, *t)).collect();
        let _ = writeln!(
            out,
            "(si/fn {} {{{} {} :inputs [{}] :output {} :variadic {} :safety {} :abi {}}})",
            q(&p),
            generics(f),
            preds(f),
            ins.join(" "),
            sty(tcx, sig.output()),
            sig.c_variadic(),
            q(&format!("{:?}", sig.safety())),
            q(&format!("{}", sig.abi()))
        );
    }
    out
}
