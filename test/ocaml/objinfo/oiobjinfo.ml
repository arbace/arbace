(**************************************************************************)
(*                                                                        *)
(*                                 OCaml                                  *)
(*                                                                        *)
(*             Xavier Leroy, projet Cristal, INRIA Rocquencourt           *)
(*         Mehdi Dogguy, PPS laboratory, University Paris Diderot         *)
(*                                                                        *)
(*   Copyright 1996 Institut National de Recherche en Informatique et     *)
(*     en Automatique.                                                    *)
(*   Copyright 2010 Mehdi Dogguy                                          *)
(*                                                                        *)
(*   All rights reserved.  This file is distributed under the terms of    *)
(*   the GNU Lesser General Public License version 2.1, with the          *)
(*   special exception on linking described in the file LICENSE.          *)
(*                                                                        *)
(**************************************************************************)

(* oiobjinfo (Arbace, queue 511): ocamltest's `ocamlc -c FLAGS FILE...` then `ocamlobjinfo ARGS` on oi, over compiler-libs' units.
   Modified from OCaml 5.5.0's tools/objinfo.ml (its .cmi and .cmt printing kept verbatim: print_name_crc, print_cmi_infos,
   print_cmt_infos; the .cmo, .cma, .cmx, .cmxa, .cmxs and executable parts left out, since oi writes none of them) and
   driver/compile_common.ml (the interface and implementation steps up to typing, which writes the .cmi, .cmt and .cmti: oi has no
   bytecode back end, so no .cmo is written).

   oiobjinfo [-c] [-bin-annot] [-bin-annot-occurrences] [-I DIR]... [-nostdlib] FILE.ml|FILE.mli... -objinfo [OBJINFO-ARG]...
   The compiler's messages (warnings and errors, as ocamlc prints them) go to ocamlc.out in the current directory, ocamltest's
   compiler output (check-ocamlc.byte-output); ocamlobjinfo's output to the standard output (check-program-output). A compile
   error: status 2 after the message, before ocamlobjinfo. *)

open Printf

let quiet = ref false
let no_crc = ref false
let shape = ref false
let index = ref false
let decls = ref false
let uid_deps = ref false

module Magic_number = Misc.Magic_number

let dummy_crc = String.make 32 '-'
let null_crc = String.make 32 '0'

let string_of_crc crc = if !no_crc then null_crc else Digest.BLAKE128.to_hex crc

let print_name_crc (name, crco) =
  let crc =
    match crco with
      None -> dummy_crc
    | Some crc -> string_of_crc crc
  in
    printf "\t%s\t%s\n" crc name

let print_spaced_string s =
  printf " %s" s

let print_cmi_infos name crcs =
  if not !quiet then begin
    printf "Unit name: %s\n" name;
    printf "Interfaces imported:\n";
    List.iter print_name_crc crcs
  end

let print_cmt_infos cmt =
  let open Cmt_format in
  if not !quiet then begin
    printf "Cmt unit name: %s\n" cmt.cmt_modname;
    print_string "Cmt interfaces imported:\n";
    List.iter print_name_crc cmt.cmt_imports;
    printf "Source file: %s\n"
          (match cmt.cmt_sourcefile with None -> "(none)" | Some f -> f);
    printf "Compilation flags:";
    Array.iter print_spaced_string cmt.cmt_args;
    printf "\nLoad path:\n  Visible:";
    List.iter print_spaced_string cmt.cmt_loadpath.visible;
    printf "\n  Hidden:";
    List.iter print_spaced_string cmt.cmt_loadpath.hidden;
    printf "\n";
    printf "cmt interface digest: %s\n"
      (match cmt.cmt_interface_digest with
      | None -> ""
      | Some crc -> string_of_crc crc);
  end;
  if !shape then begin
    printf "Implementation shape: ";
    (match cmt.cmt_impl_shape with
    | None -> printf "(none)\n"
    | Some shape -> Format.printf "\n%a" Shape.print shape)
  end;
  if !index then begin
    printf "Indexed shapes:\n";
    List.iter (fun (loc, item) ->
      let pp_loc fmt { Location.txt; loc } =
        Format.fprintf fmt "%a (%a)"
          Pprintast.longident txt Location.print_loc loc
      in
      Format.printf "@[<hov 2>%a:@ %a@]@;"
        Shape_reduce.print_result item pp_loc loc)
      cmt.cmt_ident_occurrences;
    Format.print_flush ()
  end;
  if !uid_deps then begin
    printf "\nUid dependencies:\n";
    let arr = Array.of_list cmt.cmt_declaration_dependencies in
    let () =
      Array.sort (fun (_tr, u1, u2) (_tr', u1', u2') ->
                    match Shape.Uid.compare u1 u1' with
                    | 0 -> Shape.Uid.compare u2 u2'
                    | n -> n) arr
    in
    Format.printf "@[<v>";
    Array.iter (fun (rk, u1, u2) ->
      let rk = match (rk : Shape.Uid.Deps.kind) with
        | Definition_to_declaration -> "<-"
        | Declaration_to_declaration -> "<->"
      in
      Format.printf "@[<h>%a %s %a@]@;"
        Shape.Uid.print u1
        rk
        Shape.Uid.print u2) arr;
    Format.printf "@]";
    Format.print_flush ()
  end;
  if !decls then begin
    printf "\nUid of decls:\n";
    let decls = Array.of_list (Shape.Uid.Tbl.to_list cmt.cmt_uid_to_decl) in
    Array.sort (fun (uid, _) (uid', _) -> Shape.Uid.compare uid uid') decls;
    Array.iter (fun (uid, item) ->
      let loc = match (item : Typedtree.item_declaration) with
        | Value vd -> vd.val_name
        | Value_binding vb ->
          let (_, name, _, _) =
            List.hd (Typedtree.let_bound_idents_full [vb])
          in
          name
        | Type td -> td.typ_name
        | Constructor cd -> cd.cd_name
        | Extension_constructor ec -> ec.ext_name
        | Label ld -> ld.ld_name
        | Module md ->
          { md.md_name with
            txt = Option.value md.md_name.txt ~default:"_" }
        | Module_substitution ms -> ms.ms_name
        | Module_binding mb ->
          { mb.mb_name with
            txt = Option.value mb.mb_name.txt ~default:"_" }
        | Module_type mtd -> mtd.mtd_name
        | Class cd -> cd.ci_id_name
        | Class_type ctd -> ctd.ci_id_name
      in
      let pp_loc fmt { Location.txt; loc } =
        Format.fprintf fmt "%s (%a)"
           txt Location.print_loc loc
      in
      Format.printf "@[<hov 2>%a:@ %a@]@;"
        Shape.Uid.print uid
        pp_loc loc)
      decls;
    Format.print_flush ()
  end

let exit_err msg = print_endline msg; exit 2
let exit_errf fmt = Printf.ksprintf exit_err fmt

(* objinfo's dump_obj for the kinds oi writes (.cmi, .cmt, .cmti); another kind is refused *)
let dump_obj filename =
  let open Magic_number in
  if not !quiet then printf "File %s\n" filename;
  let ic = open_in_bin filename in
  match read_current_info ~expected_kind:None ic with
  | Ok { kind = (Cmi | Cmt); version = _ } ->
     close_in ic;
     let cmi, cmt = Cmt_format.read filename in
     begin match cmi with
       | None -> ()
       | Some cmi ->
          print_cmi_infos cmi.Cmi_format.cmi_name cmi.Cmi_format.cmi_crcs
     end;
     begin match cmt with
       | None -> ()
       | Some cmt -> print_cmt_infos cmt
     end
  | Ok { kind; _ } ->
     close_in ic;
     exit_errf "oiobjinfo: %s: the kind %S is not one oi writes" filename (human_name_of_kind kind)
  | Error _ ->
     close_in ic;
     exit_errf "oiobjinfo: %s: not an object file of OCaml %s" filename Sys.ocaml_version

let objinfo_args = [
  "-quiet", Arg.Set quiet, "";
  "-shape", Arg.Set shape, "";
  "-index", Arg.Set index, "";
  "-decls", Arg.Set decls, "";
  "-uid-deps", Arg.Set uid_deps, "";
  "-null-crc", Arg.Set no_crc, "";
]

(* compile_common.ml's steps for one source file, as Compenv.process_action and Compile's with_info run them *)
let compile_file file =
  let kind = if Filename.check_suffix file ".mli" then Unit_info.Intf else Unit_info.Impl in
  let target = Unit_info.make ~source_file:file kind (Filename.remove_extension file) in
  Compmisc.init_path ();
  Env.set_current_unit target;
  let env = Compmisc.initial_env () in
  match kind with
  | Unit_info.Intf ->
      let ast = Pparse.parse_interface ~tool_name:"ocamlc" file in
      let tsg = Typemod.type_interface env ast in
      let alerts = Builtin_attributes.alerts_of_sig ~mark:true ast in
      let sg = tsg.Typedtree.sig_type in
      ignore (Includemod.signatures env ~mark:true sg sg);
      Typecore.force_delayed_checks ();
      Builtin_attributes.warn_unused ();
      Warnings.check_fatal ();
      let sg = Env.save_signature ~alerts tsg.Typedtree.sig_type (Unit_info.cmi target) in
      Typemod.save_signature target tsg env sg
  | Unit_info.Impl ->
      let ast = Pparse.parse_implementation ~tool_name:"ocamlc" file in
      ignore (Typemod.type_implementation target env ast);
      Builtin_attributes.warn_unused ();
      Warnings.check_fatal ()

let () =
  let argv = Sys.argv in
  let n = Array.length argv in
  let rec split i = if i >= n then n else if argv.(i) = "-objinfo" then i else split (i + 1) in
  let k = split 1 in
  let files = ref [] and incs = ref [] in
  let compile_args = [
    "-c", Arg.Unit ignore, "";
    "-bin-annot", Arg.Set Clflags.binary_annotations, "";
    "-bin-annot-occurrences", Arg.Set Clflags.store_occurrences, "";
    "-I", Arg.String (fun d -> incs := d :: !incs), "DIR";
    "-nostdlib", Arg.Set Clflags.no_std_include, "";
    "-principal", Arg.Set Clflags.principal, "";
    "-strict-sequence", Arg.Set Clflags.strict_sequence, "";
  ] in
  let usage = "oiobjinfo [OPTION]... FILE... -objinfo [OBJINFO-ARG]..." in
  (try Arg.parse_argv ~current:(ref 0) (Array.sub argv 0 k) compile_args (fun f -> files := f :: !files) usage
   with Arg.Bad m | Arg.Help m -> prerr_string m; exit 2);
  Clflags.include_dirs := List.rev !incs;
  let oc = open_out "ocamlc.out" in
  let ppf = Format.formatter_of_out_channel oc in
  Location.formatter_for_warnings := ppf;
  let ok =
    try List.iter compile_file (List.rev !files); true
    with e -> Location.report_exception ppf e; false
  in
  Format.pp_print_flush ppf (); close_out oc;
  if not ok then exit 2;
  let objs = ref [] in
  (try Arg.parse_argv ~current:(ref 0) (Array.append [| "ocamlobjinfo" |] (Array.sub argv (k + 1) (max 0 (n - k - 1))))
         objinfo_args (fun f -> objs := f :: !objs) "ocamlobjinfo [OPTIONS] FILES"
   with Arg.Bad m | Arg.Help m -> prerr_string m; exit 2);
  List.iter dump_obj (List.rev !objs);
  exit 0
