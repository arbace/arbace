(* oicmi (queue 480): `ocamlc -c FILE.mli` on oi, the interface's .cmi written by compiler-libs' units (Arbace's own program, under
   the Eclipse Public License 1.0, written against OCaml 5.5.0's driver/compile_common.ml's interface step: Pparse, Typemod's
   type_interface, Env's save_signature). bin/oi ocamldoc makes with it the standard library's .cmi files ocamldoc types a
   documented file against (ocamltest's `-nostdlib -I stdlib`), so no ocamlc is needed.

   oicmi [-I DIR]... [-nopervasives] [-no-alias-deps] [-principal|-no-principal] [-strict-sequence] [-o PREFIX] FILE.mli
   writes PREFIX.cmi (PREFIX defaults to FILE less its extension); a type error is reported as ocamlc reports it, status 2. *)

let () =
  let incs = ref [] and output = ref "" and file = ref "" in
  let spec = [
    "-I", Arg.String (fun d -> incs := d :: !incs), "DIR";
    "-nopervasives", Arg.Set Clflags.nopervasives, "";
    "-no-alias-deps", Arg.Set Clflags.no_alias_deps, "";
    "-principal", Arg.Set Clflags.principal, "";
    "-no-principal", Arg.Clear Clflags.principal, "";
    "-strict-sequence", Arg.Set Clflags.strict_sequence, "";
    "-o", Arg.Set_string output, "PREFIX";
  ] in
  Arg.parse spec (fun f -> file := f) "oicmi [OPTION]... FILE.mli";
  if !file = "" then (prerr_endline "oicmi: no FILE.mli"; exit 2);
  Clflags.no_std_include := true;
  Clflags.include_dirs := List.rev !incs;
  let prefix = if !output = "" then Filename.remove_extension !file else Filename.remove_extension !output in
  try
    Compmisc.init_path ();
    let info = Unit_info.make ~check_modname:false ~source_file:!file Unit_info.Intf prefix in
    Env.set_current_unit info;
    let env = Compmisc.initial_env () in
    let ast = Pparse.parse_interface ~tool_name:"ocamlc" !file in
    let tsg = Typemod.type_interface env ast in
    let alerts = Builtin_attributes.alerts_of_sig ~mark:true ast in
    ignore (Env.save_signature ~alerts tsg.Typedtree.sig_type (Unit_info.cmi info));
    exit 0
  with e ->
    Location.report_exception Format.err_formatter e;
    exit 2
