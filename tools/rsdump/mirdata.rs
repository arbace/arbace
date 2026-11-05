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

fn join<T>(xs: impl IntoIterator<Item = T>, mut f: impl FnMut(T) -> String) -> String {
    xs.into_iter().map(|x| f(x)).collect::<Vec<_>>().join(" ")
}

pub struct W<'tcx> {
    pub tcx: TyCtxt<'tcx>,
    /// the allocations the constants of the body being written reach: id -> form (see `alloc_form`)
    pub allocs: std::cell::RefCell<std::collections::BTreeMap<String, String>>,
    /// bodies of monomorphic instances: a call's callee is resolved (rustc's `Instance::try_resolve`) and its symbol given
    pub mono: bool,
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
            ConstValue::Scalar(s) => {
                if let rustc_middle::mir::interpret::Scalar::Ptr(p, _) = s {
                    self.note_alloc(p.provenance.alloc_id());
                }
                self.scalar(s)
            }
            ConstValue::ZeroSized => "(zst)".to_string(),
            ConstValue::Slice { alloc_id, meta } => {
                self.note_alloc(alloc_id);
                format!("(slice {:?} {})", alloc_id, meta)
            }
            ConstValue::Indirect { alloc_id, offset } => {
                self.note_alloc(alloc_id);
                format!("(indirect {:?} {})", alloc_id, offset.bytes())
            }
        }
    }

    /// Records an allocation (and those its pointers reach) for the body's `:alloc-data`: `{:kind :memory :align A :mutable M
    /// :bytes "HEX" :init "HEX" :relocs [[OFFSET "allocN"]...]}`, a function (`:kind :fn :symbol`), a static (`:kind :static
    /// :symbol`), a vtable (its allocation, `:kind :memory :vtable "TEXT"`)
    fn note_alloc(&self, id: rustc_middle::mir::interpret::AllocId) {
        use rustc_middle::mir::interpret::GlobalAlloc;
        let key = format!("{id:?}");
        if self.allocs.borrow().contains_key(&key) {
            return;
        }
        self.allocs.borrow_mut().insert(key.clone(), "(pending)".into());
        let tcx = self.tcx;
        let mem = |a: rustc_middle::mir::interpret::ConstAllocation<'tcx>, extra: String| -> (String, Vec<rustc_middle::mir::interpret::AllocId>) {
            let a = a.inner();
            let n = a.size().bytes_usize();
            let bytes = a.inspect_with_uninit_and_ptr_outside_interpreter(0..n);
            let hex: String = bytes.iter().map(|b| format!("{b:02x}")).collect();
            let mut init = String::new();
            for i in 0..n {
                init.push(if a.init_mask().is_range_initialized(rustc_middle::mir::interpret::alloc_range(rustc_abi::Size::from_bytes(i), rustc_abi::Size::from_bytes(1))).is_ok() { '1' } else { '0' });
            }
            let mut ids = Vec::new();
            let relocs = join(a.provenance().ptrs().iter(), |(off, p)| {
                ids.push(p.alloc_id());
                format!("[{} {}]", off.bytes(), q(&format!("{:?}", p.alloc_id())))
            });
            (
                format!("{{:kind :memory :align {} :mutable {} :bytes {} :init {} :relocs [{}]{}}}", a.align.bytes(), a.mutability.is_mut(), q(&hex), q(&init), relocs, extra),
                ids,
            )
        };
        let (form, ids) = match tcx.try_get_global_alloc(id) {
            Some(GlobalAlloc::Memory(a)) => mem(a, String::new()),
            Some(GlobalAlloc::Function { instance }) => (format!("{{:kind :fn :symbol {}}}", q(&tcx.symbol_name(instance).name.to_string())), vec![]),
            Some(GlobalAlloc::Static(d)) => (format!("{{:kind :static :symbol {} :def {}}}", q(&tcx.symbol_name(ty::Instance::mono(tcx, d)).name.to_string()), q(&format!("{d:?}"))), vec![]),
            Some(GlobalAlloc::VTable(t, d)) => {
                let vid = tcx.vtable_allocation((t, d.principal().map(|p| tcx.instantiate_bound_regions_with_erased(p))));
                match tcx.try_get_global_alloc(vid) {
                    Some(GlobalAlloc::Memory(a)) => mem(a, format!(" :vtable {}", q(&format!("{t} as {d:?}")))),
                    _ => ("{:kind :other}".to_string(), vec![]),
                }
            }
            _ => ("{:kind :other}".to_string(), vec![]),
        };
        self.allocs.borrow_mut().insert(key, form);
        for i in ids {
            self.note_alloc(i);
        }
    }

    fn constant(&self, c: &ConstOperand<'tcx>) -> String {
        let text = c.to_string();
        let v = match c.const_ {
            Const::Ty(_, k) => match c.const_.try_to_scalar_int() {
                Some(i) => format!("(scalar {} {})", i.to_bits_unchecked(), i.size().bytes()),
                None => format!("(tyconst {})", q(&k.to_string())),
            },
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

    /// The instance a call resolves to, in a monomorphic body: `(callee {:kind K :symbol S})`, or nil
    fn callee(&self, func: &Operand<'tcx>) -> String {
        if !self.mono {
            return "nil".into();
        }
        let tcx = self.tcx;
        let Some((def, args)) = func.const_fn_def() else { return "nil".into() };
        let env = ty::TypingEnv::fully_monomorphized();
        let args = tcx.erase_and_anonymize_regions(args);
        match ty::Instance::try_resolve(tcx, env, def, args) {
            Ok(Some(i)) => {
                let kind = match i.def {
                    ty::InstanceKind::Item(_) => "item",
                    ty::InstanceKind::Intrinsic(_) => "intrinsic",
                    ty::InstanceKind::Virtual(..) => "virtual",
                    ty::InstanceKind::Shim(k) => match k {
                        ty::ShimKind::VTable(_) => "vtable-shim",
                        ty::ShimKind::Reify(..) => "reify-shim",
                        ty::ShimKind::FnPtr(..) => "fnptr-shim",
                        ty::ShimKind::ClosureOnce { .. } => "closure-once-shim",
                        ty::ShimKind::DropGlue(..) => "drop-glue",
                        ty::ShimKind::Clone(..) => "clone-shim",
                        _ => "shim",
                    },
                    _ => "other",
                };
                let sym = if matches!(i.def, ty::InstanceKind::Intrinsic(_) | ty::InstanceKind::Virtual(..)) { String::new() } else { tcx.symbol_name(i).name.to_string() };
                let intr = if let ty::InstanceKind::Intrinsic(d) = i.def { tcx.intrinsic(d).map(|x| x.name.as_str().to_string()) } else { None };
                let can_unwind = match i.def {
                    ty::InstanceKind::Item(d) if matches!(tcx.def_kind(d), rustc_hir::def::DefKind::Fn | rustc_hir::def::DefKind::AssocFn) => {
                        let never = tcx.codegen_fn_attrs(d).flags.contains(rustc_middle::middle::codegen_fn_attrs::CodegenFnAttrFlags::NEVER_UNWIND);
                        let abi = tcx.fn_sig(d).skip_binder().skip_binder().abi();
                        let abi_unwinds = match abi {
                            rustc_abi::ExternAbi::Rust | rustc_abi::ExternAbi::RustCall | rustc_abi::ExternAbi::RustCold => true,
                            a => a.is_rustic_abi() || format!("{a:?}").contains("Unwind"),
                        };
                        !never && abi_unwinds
                    }
                    ty::InstanceKind::Intrinsic(_) => false,
                    _ => true,
                };
                format!("(callee {{:kind {} :symbol {} :def {} :can-unwind {} :track-caller {}{}{}}})", q(kind), q(&sym), q(&self.path(i.def_id())), can_unwind, i.def.requires_caller_location(tcx),
                    intr.map(|n| format!(" :intrinsic {}", q(&n))).unwrap_or_default(),
                    if tcx.is_mir_available(i.def_id()) || !matches!(i.def, ty::InstanceKind::Item(_)) { "" } else { " :external true" })
            }
            _ => "(callee {:kind \"unresolved\"})".into(),
        }
    }

    /// The drop glue a `Drop` terminator calls in a monomorphic body: the symbol of `drop_in_place::<T>`, or nil
    fn drop_glue(&self, place: &Place<'tcx>, b_: &Body<'tcx>) -> String {
        if !self.mono {
            return "nil".into();
        }
        let tcx = self.tcx;
        let ty = place.ty(&b_.local_decls, tcx).ty;
        let i = ty::Instance::resolve_drop_glue(tcx, ty);
        if matches!(i.def, ty::InstanceKind::Shim(ty::ShimKind::DropGlue(_, None))) {
            return "nil".into();
        }
        format!("(glue {{:symbol {}}})", q(&tcx.symbol_name(i).name.to_string()))
    }

    /// `(loc "FILE" LINE COL)`: what `span_as_caller_location` gives the panic location of an assertion
    fn caller_location(&self, span: rustc_span::Span) -> String {
        let topmost = span.ctxt().outer_expn().expansion_cause().unwrap_or(span);
        let caller = self.tcx.sess.source_map().lookup_char_pos(topmost.lo());
        let name = caller.file.name.display(rustc_span::RemapPathScopeComponents::MACRO).to_string_lossy().to_string();
        format!("(loc {} {} {})", q(&name), caller.line, caller.col_display + 1)
    }

    fn terminator(&self, t: &Terminator<'tcx>, b_: &Body<'tcx>) -> String {
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
                "(drop {} {} {} {} {} {})",
                self.place(place), b(target), self.unwind(unwind), replace, ob(drop), self.drop_glue(place, b_)
            ),
            Call { func, args: a, destination, target, unwind, fn_span, .. } => format!(
                "(call {} {} {} {} {} {} {} {})",
                self.operand(func), args(a), self.place(destination), ob(target), self.unwind(unwind), self.callee(func),
                self.caller_location(*fn_span), t.source_info.scope.index()
            ),
            TailCall { func, args: a, .. } => format!("(tailcall {} {})", self.operand(func), args(a)),
            Assert { cond, expected, msg, target, unwind } => format!(
                "(assert {} {} {} {} {} {} {})",
                self.operand(cond), expected, self.assert_msg(msg), b(target), self.unwind(unwind), self.caller_location(t.source_info.span), t.source_info.scope.index()
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
        self.body_sym(b, phase, ctfe, None)
    }

    pub fn body_sym(&self, b: &Body<'tcx>, phase: &str, ctfe: bool, symbol: Option<&str>) -> String {
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
        if b.source.instance.requires_caller_location(tcx) {
            o.push_str(" :track-caller true");
        }
        if let Some(sa) = b.spread_arg {
            let _ = write!(o, " :spread-arg {}", sa.index());
        }
        if let Some(sy) = symbol {
            let _ = write!(o, " :symbol {}", q(sy));
        }
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
            let (inl, site, tc) = match s.inlined {
                Some((callee, sp)) => {
                    let tc = callee.def.requires_caller_location(tcx);
                    let rt = if tc { "#[track_caller] " } else { "" };
                    (q(&format!(" (inlined {rt}{callee})")), self.caller_location(sp), tc.to_string())
                }
                None => ("nil".into(), "nil".into(), "false".into()),
            };
            let ip = s.inlined_parent_scope.map(|p| p.index().to_string()).unwrap_or("nil".into());
            format!("[{p} {inl} {ip} {site} {tc}]")
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
                        .strip_prefix("(place 0 ").and_then(|x| x.strip_suffix(')')).unwrap_or("").to_string())
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
                bb.terminator.as_ref().map(|t| self.terminator(t, b)).unwrap_or("nil".into())
            );
        }
        o.push(']');
        let mut al = Vec::new();
        let _ = rustc_middle::mir::pretty::write_allocations(tcx, b, &mut al);
        let _ = write!(o, "\n :allocs {}", q(&String::from_utf8_lossy(&al)));
        let data = std::mem::take(&mut *self.allocs.borrow_mut());
        let _ = write!(o, "\n :alloc-data [{}]}})", join(data.iter(), |(k, v)| format!("[{} {}]", q(k), v)));
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
    let w = W { tcx, mono: false, allocs: Default::default() };
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

/// The layouts of the types the bodies' locals have (RF1's `layout` oracle, doc/rust/FRONTEND-SURVEY.md §3.11): one
/// `(mir/layouts [[ "TEXT" {...}] ...])` form, the monomorphic types of every body's locals, sizes, alignments, the backend
/// representation and the fields' offsets as `rustc_abi` decided them
pub fn layouts<'tcx>(tcx: TyCtxt<'tcx>, tys: &std::collections::BTreeMap<String, Ty<'tcx>>) -> String {
    use rustc_abi::{BackendRepr, FieldsShape, Primitive, Scalar};
    let prim = |s: Scalar| -> String {
        let p = match s {
            Scalar::Initialized { value, .. } | Scalar::Union { value } => value,
        };
        let r = match s {
            Scalar::Initialized { valid_range, .. } => format!(" [{} {}]", valid_range.start, valid_range.end),
            Scalar::Union { .. } => " nil".to_string(),
        };
        match p {
            Primitive::Int(i, sg) => format!("(int {} {}{})", i.size().bits(), sg, r),
            Primitive::Float(f) => format!("(float {}{})", f.size().bits(), r),
            Primitive::Pointer(_) => format!("(ptr{})", r),
        }
    };
    let mut out = String::from("(mir/layouts [");
    for (text, ty) in tys {
        let Ok(l) = tcx.layout_of(ty::TypingEnv::fully_monomorphized().as_query_input(*ty)) else { continue };
        let abi = match l.backend_repr {
            BackendRepr::Scalar(s) => format!("(scalar {})", prim(s)),
            BackendRepr::ScalarPair { a, b, b_offset } => format!("(pair {} {} {})", prim(a), prim(b), b_offset.bytes()),
            BackendRepr::Memory { sized } => format!("(memory {sized})"),
            _ => "(other)".to_string(),
        };
        let fields = match &l.fields {
            FieldsShape::Primitive => "(primitive)".to_string(),
            FieldsShape::Union(n) => format!("(union {n})"),
            FieldsShape::Array { stride, count } => format!("(array {} {})", stride.bytes(), count),
            FieldsShape::Arbitrary { offsets, .. } => format!("[{}]", join(offsets.iter(), |o| o.bytes().to_string())),
        };
        let variants = match &l.variants {
            rustc_abi::Variants::Empty => "(empty)".to_string(),
            rustc_abi::Variants::Single { index } => format!("(single {})", index.index()),
            rustc_abi::Variants::Multiple { tag, tag_encoding, tag_field, variants } => {
                let enc = match tag_encoding {
                    rustc_abi::TagEncoding::Direct => "(direct)".to_string(),
                    rustc_abi::TagEncoding::Niche { untagged_variant, niche_variants, niche_start } => format!(
                        "(niche {} {} {} {})",
                        untagged_variant.index(), niche_variants.start.index(), niche_variants.last.index(), niche_start
                    ),
                };
                let vs = join(variants.iter_enumerated(), |(vi, v)| {
                    let offs = format!("[{}]", join(v.field_offsets.iter(), |o| o.bytes().to_string()));
                    format!("{{:offsets {} :uninhabited {}}}", offs, l.is_variant_uninhabited(vi))
                });
                format!("(multiple {{:tag {} :tag-field {} :encoding {} :variants [{}]}})", prim(*tag), tag_field.index(), enc, vs)
            }
        };
        let discrs = match l.ty.kind() {
            ty::Adt(d, _) if d.is_enum() => format!(
                " :discrs [{}]",
                join(d.discriminants(tcx), |(_, dv)| dv.val.to_string())
            ),
            _ => String::new(),
        };
        let _ = write!(
            out,
            "\n [{} {{:size {} :align {} :abi {} :fields {} :variants {}{} :uninhabited {}}}]",
            q(text), l.size.bytes(), l.align.abi.bytes(), abi, fields, variants, discrs, l.is_uninhabited()
        );
    }
    out.push_str("])\n");
    out
}
