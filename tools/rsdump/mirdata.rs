// RF1 (doc/rust/FRONTEND-SURVEY.md, "As built: RF1-RF2"): the helper's `mir` mode, a crate's MIR bodies
// as Clojure-reader forms (spec doc/rust/MIR.md), and its `census` mode. Written anew for Arbace by
// walking rustc_middle's `Body` as `rustc_middle::mir::pretty` does (rustc, MIT or Apache-2.0,
// 1.99.1: compiler/rustc_middle/src/mir/{pretty,syntax,terminator,consts}.rs): the same choices of
// body per item as `write_mir_pretty`, the same texts for types and constants, with the structure
// beside them. `(ty "text" STRUCT)`: a type's rendering as `{}` prints it (the text the printer
// prints back) and its structure for the back half.

use rustc_middle::mir::*;
use rustc_middle::ty::{self, Ty, TyCtxt};
use std::fmt::Write;

pub fn q(s: &str) -> String {
    let mut o = String::from("\"");
    for c in s.chars() {
        match c {
            '"' => o.push_str("\\\""),
            '\\' => o.push_str("\\\\"),
            '\n' => o.push_str("\\n"),
            '\r' => o.push_str("\\r"),
            '\t' => o.push_str("\\t"),
            c if (c as u32) < 0x20 || c as u32 == 0x7f => {
                let _ = write!(o, "\\u{:04X}", c as u32);
            }
            c => o.push(c),
        }
    }
    o.push('"');
    o
}

fn join<T>(xs: impl IntoIterator<Item = T>, f: impl Fn(T) -> String) -> String {
    xs.into_iter().map(f).collect::<Vec<_>>().join(" ")
}

pub struct W<'tcx> {
    pub tcx: TyCtxt<'tcx>,
}

impl<'tcx> W<'tcx> {
    fn path(&self, d: rustc_hir::def_id::DefId) -> String {
        ty::print::with_no_trimmed_paths!(self.tcx.def_path_str(d))
    }

    fn path_args(&self, d: rustc_hir::def_id::DefId, args: ty::GenericArgsRef<'tcx>) -> String {
        ty::print::with_no_trimmed_paths!(self.tcx.def_path_str_with_args(d, args))
    }

    fn args(&self, args: ty::GenericArgsRef<'tcx>) -> String {
        let v = join(args.iter(), |a| match a.kind() {
            ty::GenericArgKind::Type(t) => self.ty(t),
            ty::GenericArgKind::Lifetime(_) => "(lt)".to_string(),
            ty::GenericArgKind::Const(c) => format!("(cst {})", q(&c.to_string())),
        });
        format!("[{v}]")
    }

    pub fn ty(&self, t: Ty<'tcx>) -> String {
        let text = ty::print::with_no_trimmed_paths!(format!("{t}"));
        let st = match *t.kind() {
            ty::Bool => "bool".to_string(),
            ty::Char => "char".to_string(),
            ty::Int(i) => format!("(int {})", q(i.name_str())),
            ty::Uint(u) => format!("(uint {})", q(u.name_str())),
            ty::Float(f) => format!("(float {})", q(f.name_str())),
            ty::Str => "str".to_string(),
            ty::Never => "never".to_string(),
            ty::Tuple(ts) => format!("(tuple {})", join(ts.iter(), |x| self.ty(x))),
            ty::Array(e, n) => format!("(array {} {})", self.ty(e), q(&n.to_string())),
            ty::Slice(e) => format!("(slice {})", self.ty(e)),
            ty::Ref(_, e, m) => format!("(ref {} {})", self.ty(e), m.is_mut()),
            ty::RawPtr(e, m) => format!("(ptr {} {})", self.ty(e), m.is_mut()),
            ty::Adt(d, a) => format!("(adt {} {} {})", q(&self.path(d.did())), q(&format!("{:?}", d.did())), self.args(a)),
            ty::FnDef(d, a) => format!("(fndef {} {})", q(&self.path(d)), self.args(a.skip_binder())),
            ty::Closure(d, a) => format!("(closure {} {})", q(&self.path(d)), self.args(a)),
            ty::Param(p) => format!("(param {})", q(p.name.as_str())),
            ty::FnPtr(..) => "(fnptr)".to_string(),
            ty::Dynamic(..) => "(dyn)".to_string(),
            ty::Alias(..) => "(alias)".to_string(),
            ty::Foreign(_) => "(foreign)".to_string(),
            _ => format!("(other {})", q(&format!("{:?}", t.kind()).chars().take(24).collect::<String>())),
        };
        format!("(ty {} {})", q(&text), st)
    }

    fn local(&self, l: Local) -> String {
        l.index().to_string()
    }

    fn place(&self, p: &Place<'tcx>) -> String {
        let mut o = format!("(place {}", p.local.index());
        for e in p.projection.iter() {
            o.push(' ');
            o.push_str(&match e {
                ProjectionElem::Deref => "(deref)".to_string(),
                ProjectionElem::Field(f, t) => format!("(field {} {})", f.index(), self.ty(t)),
                ProjectionElem::Index(l) => format!("(index {})", l.index()),
                ProjectionElem::ConstantIndex { offset, min_length, from_end } => format!("(cindex {offset} {min_length} {from_end})"),
                ProjectionElem::Subslice { from, to, from_end } => format!("(subslice {from} {to} {from_end})"),
                ProjectionElem::Downcast(n, i) => format!(
                    "(downcast {} {})",
                    n.map(|n| q(n.as_str())).unwrap_or("nil".into()),
                    i.index()
                ),
                ProjectionElem::OpaqueCast(t) => format!("(opaque {})", self.ty(t)),
                ProjectionElem::UnwrapUnsafeBinder(t) => format!("(unwrap-binder {})", self.ty(t)),
            });
        }
        o.push(')');
        o
    }

    fn scalar(&self, s: rustc_middle::mir::interpret::Scalar) -> String {
        use rustc_middle::mir::interpret::Scalar;
        match s {
            Scalar::Int(i) => format!("(scalar {} {})", i.to_bits_unchecked(), i.size().bytes()),
            Scalar::Ptr(ptr, size) => {
                let (prov, off) = ptr.prov_and_relative_offset();
                format!("(ptr {:?} {} {})", prov.alloc_id(), off.bytes(), size)
            }
        }
    }

    fn constval(&self, v: ConstValue) -> String {
        match v {
            ConstValue::Scalar(s) => self.scalar(s),
            ConstValue::ZeroSized => "(zst)".to_string(),
            ConstValue::Slice { alloc_id, meta } => format!("(slice {:?} {})", alloc_id, meta),
            ConstValue::Indirect { alloc_id, offset } => format!("(indirect {:?} {})", alloc_id, offset.bytes()),
        }
    }

    fn constant(&self, c: &ConstOperand<'tcx>) -> String {
        let text = c.to_string();
        let v = match c.const_ {
            Const::Ty(_, k) => format!("(tyconst {})", q(&k.to_string())),
            Const::Unevaluated(u, _) => format!(
                "(unevaluated {} {} {})",
                q(&self.path(u.def)),
                self.args(u.args),
                u.promoted.map(|p| p.index().to_string()).unwrap_or("nil".into())
            ),
            Const::Val(v, _) => self.constval(v),
        };
        format!("(const {} {} {})", q(&text), self.ty(c.const_.ty()), v)
    }

    fn operand(&self, o: &Operand<'tcx>) -> String {
        match o {
            Operand::Copy(p) => format!("(copy {})", self.place(p)),
            Operand::Move(p) => format!("(move {})", self.place(p)),
            Operand::Constant(c) => self.constant(c),
            Operand::RuntimeChecks(c) => format!("(runtime-checks {})", q(&format!("{c:?}"))),
        }
    }

    fn rvalue(&self, r: &Rvalue<'tcx>) -> String {
        match r {
            Rvalue::Use(o, rt) => format!("({} {})", if rt.no() { "use-noretag" } else { "use" }, self.operand(o)),
            Rvalue::Repeat(o, n) => format!("(repeat {} {})", self.operand(o), q(&n.to_string())),
            Rvalue::Ref(_, k, p) => {
                let k = match k {
                    BorrowKind::Shared => "shared",
                    BorrowKind::Fake(FakeBorrowKind::Deep) => "fake-deep",
                    BorrowKind::Fake(FakeBorrowKind::Shallow) => "fake-shallow",
                    BorrowKind::Mut { .. } => "mut",
                };
                format!("(ref :{k} {})", self.place(p))
            }
            Rvalue::ThreadLocalRef(d) => {
                let m = self.tcx.static_mutability(*d).unwrap().prefix_str();
                format!("(tlsref {} {})", q(m), q(&self.path(*d)))
            }
            Rvalue::RawPtr(m, p) => format!("(rawptr {} {})", q(m.ptr_str()), self.place(p)),
            Rvalue::Cast(k, o, t) => format!("(cast {} {} {})", q(&format!("{k:?}")), self.operand(o), self.ty(*t)),
            Rvalue::BinaryOp(op, ab) => format!("(binop {} {} {})", q(&format!("{op:?}")), self.operand(&ab.0), self.operand(&ab.1)),
            Rvalue::UnaryOp(op, a) => format!("(unop {} {})", q(&format!("{op:?}")), self.operand(a)),
            Rvalue::Discriminant(p) => format!("(discriminant {})", self.place(p)),
            Rvalue::CopyForDeref(p) => format!("(copy-for-deref {})", self.place(p)),
            Rvalue::WrapUnsafeBinder(o, t) => format!("(wrap-binder {} {})", self.operand(o), self.ty(*t)),
            Rvalue::Reborrow(t, m, p) => format!("(reborrow {} {} {})", self.ty(*t), m.is_mut(), self.place(p)),
            Rvalue::Aggregate(k, ops) => {
                let ops_s = format!("[{}]", join(ops.iter(), |o| self.operand(o)));
                let kind = match &**k {
                    AggregateKind::Array(t) => format!("(array {})", self.ty(*t)),
                    AggregateKind::Tuple => "(tuple)".to_string(),
                    AggregateKind::Adt(d, v, args, _, _) => {
                        let tcx = self.tcx;
                        let vd = &tcx.adt_def(*d).variant(*v);
                        let name = ty::print::FmtPrinter::print_string(tcx, rustc_hir::def::Namespace::ValueNS, |p| {
                            ty::print::Printer::print_def_path(p, vd.def_id, args)
                        })
                        .unwrap_or_default();
                        let ck = match vd.ctor_kind() {
                            Some(rustc_hir::def::CtorKind::Const) => "const",
                            Some(rustc_hir::def::CtorKind::Fn) => "fn",
                            None => "struct",
                        };
                        let fields = join(vd.fields.iter(), |f| q(f.name.as_str()));
                        format!(
                            "(adt {} {} [{}] {} {} {})",
                            q(&name), q(ck), fields, q(&self.path(*d)), v.index(), self.args(args)
                        )
                    }
                    AggregateKind::Closure(d, args) | AggregateKind::CoroutineClosure(d, args) => {
                        let names = match d.as_local().and_then(|l| self.tcx.upvars_mentioned(l)) {
                            Some(u) => format!("[{}]", join(u.keys(), |v| q(self.tcx.hir_name(*v).as_str()))),
                            None => "nil".to_string(),
                        };
                        let span = self.tcx.def_span(*d);
                        let sp = self.tcx.sess.source_map().span_to_diagnostic_string(span);
                        format!("(closure {} {} {} {})", q(&self.path(*d)), self.args(args), names, q(&sp))
                    }
                    AggregateKind::Coroutine(d, args) => {
                        let names = match d.as_local().and_then(|l| self.tcx.upvars_mentioned(l)) {
                            Some(u) => format!("[{}]", join(u.keys(), |v| q(self.tcx.hir_name(*v).as_str()))),
                            None => "nil".to_string(),
                        };
                        format!("(coroutine {} {} {} {})", q(&self.path(*d)), self.args(args), names, q(&format!("{:?}", self.tcx.def_span(*d))))
                    }
                    AggregateKind::RawPtr(t, m) => format!("(rawptr {} {})", self.ty(*t), m.is_mut()),
                };
                format!("(aggregate {kind} {ops_s})")
            }
        }
    }

    fn unwind(&self, u: &UnwindAction) -> String {
        match u {
            UnwindAction::Continue => "(continue)".into(),
            UnwindAction::Unreachable => "(unreachable)".into(),
            UnwindAction::Terminate(r) => format!("(terminate {})", q(r.as_short_str())),
            UnwindAction::Cleanup(b) => format!("(cleanup {})", b.index()),
        }
    }

    fn assert_msg(&self, m: &AssertMessage<'tcx>) -> String {
        use AssertKind::*;
        let o = |x: &Operand<'tcx>| self.operand(x);
        match m {
            BoundsCheck { len, index } => format!("(bounds-check {} {})", o(len), o(index)),
            Overflow(op, a, b) => format!("(overflow {} {} {})", q(&format!("{op:?}")), o(a), o(b)),
            OverflowNeg(a) => format!("(overflow-neg {})", o(a)),
            DivisionByZero(a) => format!("(div-zero {})", o(a)),
            RemainderByZero(a) => format!("(rem-zero {})", o(a)),
            MisalignedPointerDereference { required, found } => format!("(misaligned {} {})", o(required), o(found)),
            NullPointerDereference => "(null-deref)".into(),
            NullReferenceConstructed => "(null-ref)".into(),
            InvalidEnumConstruction(a) => format!("(invalid-enum {})", o(a)),
            ResumedAfterReturn(_) | ResumedAfterPanic(_) | ResumedAfterDrop(_) => {
                let mut s = String::new();
                let _ = m.fmt_assert_args(&mut s);
                let kind = match m {
                    ResumedAfterReturn(_) => "return",
                    ResumedAfterPanic(_) => "panic",
                    _ => "drop",
                };
                format!("(resumed {} {})", q(kind), q(&s))
            }
        }
    }

    fn terminator(&self, t: &Terminator<'tcx>) -> String {
        use TerminatorKind::*;
        let b = |x: &BasicBlock| x.index().to_string();
        let ob = |x: &Option<BasicBlock>| x.map(|y| y.index().to_string()).unwrap_or("nil".into());
        let args = |a: &[rustc_span::Spanned<Operand<'tcx>>]| format!("[{}]", join(a.iter(), |x| self.operand(&x.node)));
        match &t.kind {
            Goto { target } => format!("(goto {})", b(target)),
            SwitchInt { discr, targets } => {
                let vs = join(targets.iter(), |(v, t)| format!("[{} {}]", v, t.index()));
                format!("(switch {} [{}] {})", self.operand(discr), vs, targets.otherwise().index())
            }
            UnwindResume => "(resume)".into(),
            UnwindTerminate(r) => format!("(terminate {})", q(r.as_short_str())),
            Return => "(return)".into(),
            Unreachable => "(unreachable)".into(),
            Drop { place, target, unwind, replace, drop } => format!(
                "(drop {} {} {} {} {})",
                self.place(place), b(target), self.unwind(unwind), replace, ob(drop)
            ),
            Call { func, args: a, destination, target, unwind, .. } => format!(
                "(call {} {} {} {} {})",
                self.operand(func), args(a), self.place(destination), ob(target), self.unwind(unwind)
            ),
            TailCall { func, args: a, .. } => format!("(tailcall {} {})", self.operand(func), args(a)),
            Assert { cond, expected, msg, target, unwind } => format!(
                "(assert {} {} {} {} {})",
                self.operand(cond), expected, self.assert_msg(msg), b(target), self.unwind(unwind)
            ),
            Yield { value, resume, resume_arg, drop } => format!(
                "(yield {} {} {} {})",
                self.operand(value), b(resume), self.place(resume_arg), ob(drop)
            ),
            CoroutineDrop => "(coroutine-drop)".into(),
            FalseEdge { real_target, imaginary_target } => format!("(false-edge {} {})", b(real_target), b(imaginary_target)),
            FalseUnwind { real_target, unwind } => format!("(false-unwind {} {})", b(real_target), self.unwind(unwind)),
            InlineAsm { targets, unwind, .. } => format!(
                "(asm {} [{}] {})",
                q(&format!("{:?}", t.kind)),
                join(targets.iter(), |x| x.index().to_string()),
                self.unwind(unwind)
            ),
        }
    }

    fn statement(&self, s: &Statement<'tcx>) -> String {
        use StatementKind::*;
        let dbg = if s.debuginfos.is_empty() {
            String::new()
        } else {
            format!(" :dbg [{}]", join(s.debuginfos.iter(), |d| q(&format!("{d:?}"))))
        };
        let body = match &s.kind {
            Assign(b) => format!("(assign {} {})", self.place(&b.0), self.rvalue(&b.1)),
            FakeRead(b) => format!("(fake-read {} {})", q(&format!("{:?}", b.0)), self.place(&b.1)),
            SetDiscriminant { place, variant_index } => format!("(set-discriminant {} {})", self.place(place), variant_index.index()),
            StorageLive(l) => format!("(storage-live {})", self.local(*l)),
            StorageDead(l) => format!("(storage-dead {})", self.local(*l)),
            PlaceMention(p) => format!("(place-mention {})", self.place(p)),
            Intrinsic(i) => match &**i {
                NonDivergingIntrinsic::Assume(o) => format!("(assume {})", self.operand(o)),
                NonDivergingIntrinsic::CopyNonOverlapping(c) => format!(
                    "(copy-nonoverlapping {} {} {})",
                    self.operand(&c.dst), self.operand(&c.src), self.operand(&c.count)
                ),
            },
            ConstEvalCounter => "(const-eval-counter)".into(),
            Nop => "(nop)".into(),
            BackwardIncompatibleDropHint { place, .. } => format!("(bidh {})", self.place(place)),
            AscribeUserType(..) | Coverage(..) => format!("(raw {})", q(&format!("{:?}", s.kind))),
        };
        if dbg.is_empty() { body } else { format!("(dbg-stmt {body}{dbg})") }
    }

    pub fn body(&self, b: &Body<'tcx>, phase: &str, ctfe: bool) -> String {
        let tcx = self.tcx;
        let def = b.source.def_id();
        let mut o = String::new();
        let _ = write!(o, "(mir/body {{:def {} :def-id {} :phase {}", q(&self.path(def)), q(&format!("{def:?}")), q(phase));
        if ctfe {
            o.push_str(" :ctfe true");
        }
        if let Some(p) = b.source.promoted {
            let _ = write!(o, " :promoted {}", p.index());
        }
        let _ = write!(o, " :instance {}", q(&format!("{:?}", b.source.instance)));
        let name = ty::print::with_forced_impl_filename_line!(tcx.def_path_str(def));
        let _ = write!(o, " :name {}", q(&name));
        let kind = {
            use rustc_hir::def::DefKind;
            let k = tcx.def_kind(def);
            let is_fn = match k {
                DefKind::Fn | DefKind::AssocFn | DefKind::Ctor(..) | DefKind::SyntheticCoroutineBody => true,
                _ => tcx.is_closure_like(def),
            };
            match (k, b.source.promoted) {
                (_, Some(_)) => "const",
                (DefKind::Const { .. } | DefKind::AssocConst { .. }, _) => "const",
                (DefKind::Static { mutability: rustc_hir::Mutability::Not, nested: false, .. }, _) => "static",
                (DefKind::Static { mutability: rustc_hir::Mutability::Mut, nested: false, .. }, _) => "static-mut",
                (_, _) if is_fn => "fn",
                (DefKind::AnonConst, _) => "anon-const",
                (DefKind::GlobalAsm, _) => "global-asm",
                _ => "other",
            }
        };
        let _ = write!(o, " :kind :{kind} :args {} :ret {}", b.arg_count, self.ty(b.return_ty()));
        if let Some(y) = b.yield_ty() {
            let _ = write!(o, " :yields {}", self.ty(y));
        }
        let _ = write!(o, "\n :locals [{}]", join(b.local_decls.iter(), |d| {
            let up = match &d.user_ty {
                Some(u) => format!(" :user-ty [{}]", join(u.projections(), |x| q(&format!("{x:?}")))),
                None => String::new(),
            };
            format!("{{:mut {} :scope {} :ty {}{}}}", d.mutability.is_mut(), d.source_info.scope.index(), self.ty(d.ty), up)
        }));
        let _ = write!(o, "\n :scopes [{}]", join(b.source_scopes.iter(), |s| {
            let p = s.parent_scope.map(|p| p.index().to_string()).unwrap_or("nil".into());
            let inl = match s.inlined {
                Some((callee, _)) => {
                    let rt = if callee.def.requires_caller_location(tcx) { "#[track_caller] " } else { "" };
                    q(&format!(" (inlined {rt}{callee})"))
                }
                None => "nil".into(),
            };
            format!("[{p} {inl}]")
        }));
        let _ = write!(o, "\n :debug [{}]", join(b.var_debug_info.iter(), |v| {
            let value = match &v.value {
                VarDebugInfoContents::Place(p) => self.place(p),
                VarDebugInfoContents::Const(c) => self.constant(c),
            };
            let comp = match &v.composite {
                Some(f) => format!(
                    " :composite {{:ty {} :proj [{}]}}",
                    self.ty(f.ty),
                    join(f.projection.iter(), |e| self.place(&Place { local: Local::from_u32(0), projection: tcx.mk_place_elems(&[*e]) })
                        .trim_start_matches("(place 0 ").trim_end_matches(')').to_string())
                ),
                None => String::new(),
            };
            format!("{{:name {} :scope {} :value {}{}}}", q(v.name.as_str()), v.source_info.scope.index(), value, comp)
        }));
        let _ = write!(o, "\n :blocks [");
        for (i, bb) in b.basic_blocks.iter().enumerate() {
            if i > 0 {
                o.push('\n');
                o.push_str("   ");
            }
            let _ = write!(
                o,
                "{{:cleanup {} :stmts [{}] :after-dbg [{}] :term {}}}",
                bb.is_cleanup,
                join(bb.statements.iter(), |s| self.statement(s)),
                join(bb.after_last_stmt_debuginfos.iter(), |d| q(&format!("{d:?}"))),
                bb.terminator.as_ref().map(|t| self.terminator(t)).unwrap_or("nil".into())
            );
        }
        o.push(']');
        let mut al = Vec::new();
        let _ = rustc_middle::mir::pretty::write_allocations(tcx, b, &mut al);
        let _ = write!(o, "\n :allocs {}}})", q(&String::from_utf8_lossy(&al)));
        o
    }
}

/// `--census FILE`: what the crate's bodies use that a back end must know: the enabled features,
/// the callees that are lang items or intrinsics (with how many call sites), the item counts
pub fn census<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    use std::collections::BTreeMap;
    let mut lang: BTreeMap<String, usize> = BTreeMap::new();
    let mut intr: BTreeMap<String, usize> = BTreeMap::new();
    let mut calls = 0usize;
    let mut bodies = 0usize;
    let mut kinds: BTreeMap<&'static str, usize> = BTreeMap::new();
    for &def_id in tcx.mir_keys(()) {
        let b = if tcx.is_const_fn(def_id) { tcx.optimized_mir(def_id) } else { tcx.instance_mir(ty::InstanceKind::Item(def_id.to_def_id())) };
        bodies += 1;
        for bb in b.basic_blocks.iter() {
            for s in &bb.statements {
                if let StatementKind::Assign(a) = &s.kind {
                    let k = match &a.1 {
                        Rvalue::Aggregate(k, _) => match &**k {
                            AggregateKind::Closure(..) => "closure",
                            AggregateKind::Coroutine(..) => "coroutine",
                            _ => "",
                        },
                        Rvalue::Cast(CastKind::PointerCoercion(..), ..) => "ptr-coercion",
                        Rvalue::ThreadLocalRef(_) => "tls",
                        _ => "",
                    };
                    if !k.is_empty() {
                        *kinds.entry(k).or_default() += 1;
                    }
                }
            }
            if let Some(t) = &bb.terminator {
                match &t.kind {
                    TerminatorKind::Call { func, .. } => {
                        calls += 1;
                        if let Some((d, _)) = func.const_fn_def() {
                            if let Some(i) = tcx.intrinsic(d) {
                                *intr.entry(i.name.as_str().to_string()).or_default() += 1;
                            }
                            if let Some(l) = tcx.as_lang_item(d) {
                                *lang.entry(format!("{l:?}")).or_default() += 1;
                            }
                        } else {
                            *kinds.entry("indirect-call").or_default() += 1;
                        }
                    }
                    TerminatorKind::InlineAsm { .. } => *kinds.entry("inline-asm").or_default() += 1,
                    TerminatorKind::Yield { .. } => *kinds.entry("yield").or_default() += 1,
                    TerminatorKind::Drop { .. } => *kinds.entry("drop").or_default() += 1,
                    TerminatorKind::TailCall { .. } => *kinds.entry("tailcall").or_default() += 1,
                    _ => {}
                }
            }
        }
    }
    let feats: Vec<String> = tcx.features().enabled_lang_features().iter().map(|f| q(f.gate_name.as_str())).collect();
    let m = |m: &BTreeMap<String, usize>| join(m.iter(), |(k, v)| format!("[{} {}]", q(k), v));
    let kd = join(kinds.iter(), |(k, v)| format!("[{} {}]", q(k), v));
    format!(
        "(census {{:bodies {bodies} :calls {calls} :features [{}] :lang-items [{}] :intrinsics [{}] :kinds [{}]}})\n",
        feats.join(" "), m(&lang), m(&intr), kd
    )
}

/// `--iface FILE`: the crate's collected items as `(iface/item {...})`: def path, kind, generics,
/// predicates and signature or type as rustc prints them, ADTs' variants and fields, trait impls'
/// trait refs (std's interface format for RF7's judge, doc/rust/FRONTEND-SURVEY.md §3.5)
pub fn iface<'tcx>(tcx: TyCtxt<'tcx>) -> String {
    use rustc_hir::def::DefKind;
    let w = W { tcx };
    let mut out = String::new();
    for local in tcx.iter_local_def_id() {
        let d = local.to_def_id();
        let k = tcx.def_kind(d);
        if !matches!(k, DefKind::Fn | DefKind::AssocFn | DefKind::Ctor(..) | DefKind::Const { .. } | DefKind::AssocConst { .. } | DefKind::Static { .. } | DefKind::TyAlias | DefKind::AssocTy | DefKind::Struct | DefKind::Enum | DefKind::Union | DefKind::Trait | DefKind::Impl { .. } | DefKind::Variant | DefKind::Field | DefKind::Closure) {
            continue;
        }
        let kn = format!("{k:?}");
        let mut f = format!(":def {} :kind {}", q(&w.path(d)), q(&kn));
        let g = tcx.generics_of(d);
        if !g.own_params.is_empty() || g.parent.is_some() {
            let _ = write!(f, " :generics [{}]", join(g.own_params.iter(), |p| q(p.name.as_str())));
        }
        match k {
            DefKind::Fn | DefKind::AssocFn | DefKind::Ctor(..) => {
                let _ = write!(f, " :sig {}", q(&ty::print::with_no_trimmed_paths!(tcx.fn_sig(d).instantiate_identity().skip_binder().to_string())));
            }
            DefKind::Const { .. } | DefKind::AssocConst { .. } | DefKind::Static { .. } | DefKind::TyAlias | DefKind::AssocTy | DefKind::Field => {
                if k == DefKind::AssocTy && !tcx.associated_item(d).defaultness(tcx).has_value() {
                    let _ = writeln!(out, "(iface/item {{{f}}})");
                    continue;
                }
                let _ = write!(f, " :ty {}", q(&ty::print::with_no_trimmed_paths!(tcx.type_of(d).instantiate_identity().skip_norm_wip().to_string())));
            }
            DefKind::Struct | DefKind::Enum | DefKind::Union => {
                let a = tcx.adt_def(d);
                let vs = join(a.variants().iter(), |v| {
                    format!("{{:name {} :fields [{}]}}", q(v.name.as_str()), join(v.fields.iter(), |fl| q(fl.name.as_str())))
                });
                let _ = write!(f, " :variants [{}]", vs);
            }
            DefKind::Impl { .. } => {
                if let Some(t) = tcx.impl_opt_trait_ref(d) {
                    let _ = write!(f, " :trait-ref {}", q(&ty::print::with_no_trimmed_paths!(t.instantiate_identity().skip_norm_wip().to_string())));
                }
                let _ = write!(f, " :self {}", q(&ty::print::with_no_trimmed_paths!(tcx.type_of(d).instantiate_identity().skip_norm_wip().to_string())));
            }
            _ => {}
        }
        if matches!(k, DefKind::Fn | DefKind::AssocFn | DefKind::Struct | DefKind::Enum | DefKind::Union | DefKind::Trait | DefKind::Impl { .. }) {
            let ps = tcx.explicit_clauses_of(d).clauses;
            if !ps.is_empty() {
                let _ = write!(f, " :predicates [{}]", join(ps.iter(), |(c, _)| q(&c.to_string())));
            }
        }
        let _ = writeln!(out, "(iface/item {{{f}}})");
    }
    out
}
