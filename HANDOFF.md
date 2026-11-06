# Handoff: Rust front end after RF7 and RF9a (branch rust-rf14, 2026-11-06)

Read doc/rust/FRONTEND-SURVEY.md, "As built: RF7", "As built: RF9a" and "Decided (2026-11-06, the user: RFP14-RFP19)" first.

Setup (light, no gate): `export TMPDIR=$PWD/.tmp/tmp ARBACE_HOST_EXE=/root/arbace/target/seed/amd64/arbace`; `bin/repr-rust helper`
(the rustc-dev apk must be extracted whole: delete `.tmp/repr-rust/apks/rustc-dev*` if the build misses crates); `bin/rust-front
front-oracles` (writes the oracles, std-names.edn and std-iface.edn, checked against their digests in test/rust/front/).

Baselines: `bin/rust-front typeck-check` 10,768 of 11,173 facts (54 of 90 whole), `--ours` 10,760; `typeck-check --crates` (after
`bin/rust-front crates` over the registry crates listed by the scan in the survey) 21,922 of 23,722; `collect-check` 1,189 of 1,191.
`TYPECK_VERBOSE=N` lists the differing facts, `TYPECK_TRACE=1` traces calls and failures.

Start RF9's next slice (decided) in arbace/rust/front/typeck.ae on these classes, in this order:
1. closure kinds and captures (upvar.rs: a call's Borrow vs Borrow-mut on the callee; today every closure is taken as Fn);
2. normalization through the parameter environment (`<I as IntoIterator>::Item` from a where clause's projection, item bounds of
   associated types like IntoIter's), e.g. collect1 `sum_all`, generics `group_by_len`;
3. CoerceMany's order for array elements (string literals' reborrow in `vec!` and `for` arrays: iterators, collections);
4. `Box<dyn Fn>` returns' Pointer adjustment placement (closures `boxed`).
Then RF10 (THIR and MIR building) over RF9's facts. Separately (RFP15 b): load `mod m;` files in arbace/rust/front/expand.ae and
resolve.ae, extend the helper's --resolve to every local file.
