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
   print_cmt_infos; since queue 528 print_general_infos and print_cmxs_infos for Arbace's own .cmxs, below; since queue 532 its .cmo
   printing too, print_cmo_infos, for the .cmo files ocamlc on oi writes (bin/oi's bytecomp tool); the .cma, .cmx, .cmxa and
   executable parts left out) and driver/compile_common.ml (the interface and implementation steps up to typing, which writes the
   .cmi, .cmt and .cmti; the .cmo is ocamlc on oi's, not this program's).

   oiobjinfo [-c | -shared -o NAME.cmxs] [-bin-annot] [-bin-annot-occurrences] [-I DIR]... [-nostdlib] FILE.ml|FILE.mli... -objinfo [OBJINFO-ARG]...
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

let yesno_of_bool oc b = output_string oc (if b then "YES" else "no")

let print_line name =
  printf "\t%s\n" name

let print_required_compunit (Cmo_format.Compunit cu_name) =
  printf "\t%s\n" cu_name

let print_cmo_infos cu =
  let open Cmo_format in
  printf "Unit name: %s\n" (Symtable.Compunit.name cu.cu_name);
  print_string "Interfaces imported:\n";
  List.iter print_name_crc cu.cu_imports;
  print_string "Required globals:\n";
  List.iter print_required_compunit cu.cu_required_compunits;
  printf "Uses unsafe features: ";
  (match cu.cu_primitives with
    | [] -> printf "no\n"
    | l  ->
        printf "YES\n";
        printf "Primitives declared in this module:\n";
        List.iter print_line l);
  printf "Force link: %a\n" yesno_of_bool cu.cu_force_link

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

(* objinfo's print_general_infos and print_cmxs_infos, verbatim (its print_line above, with print_cmo_infos); dynunit and dynheader
   are file_formats/cmxs_format.mli's (queue 528: Arbace's .cmxs, below) *)
let print_general_infos name crc defines cmi cmx =
  printf "Name: %s\n" name;
  printf "CRC of implementation: %s\n" (string_of_crc crc);
  printf "Globals defined:\n";
  List.iter print_line defines;
  printf "Interfaces imported:\n";
  List.iter print_name_crc cmi;
  printf "Implementations imported:\n";
  List.iter print_name_crc cmx

type dynunit = {
  dynu_name: string;
  dynu_crc: Digest.BLAKE128.t;
  dynu_imports_cmi: (string * Digest.BLAKE128.t option) list;
  dynu_imports_cmx: (string * Digest.BLAKE128.t option) list;
  dynu_defines: string list;
}

type dynheader = {
  dynu_magic: string;
  dynu_units: dynunit list;
}

let print_cmxs_infos header =
  List.iter
    (fun ui ->
       print_general_infos
         ui.dynu_name
         ui.dynu_crc
         ui.dynu_defines
         ui.dynu_imports_cmi
         ui.dynu_imports_cmx)
    header.dynu_units

let exit_err msg = print_endline msg; exit 2
let exit_errf fmt = Printf.ksprintf exit_err fmt

(* objinfo's dump_obj for the kinds oi writes (.cmi, .cmt, .cmti, and ocamlc on oi's .cmo); another kind is refused *)
let dump_obj filename =
  let open Magic_number in
  if not !quiet then printf "File %s\n" filename;
  let ic = open_in_bin filename in
  (* Arbace's .cmxs (queue 528): the plugin header written by `-shared` below, read as objinfo's dump_cmxs reads the
     caml_plugin_header symbol's value *)
  if Filename.check_suffix filename ".cmxs" then begin
    let m = String.length Config.cmxs_magic_number in
    let magic = try really_input_string ic m with End_of_file -> "" in
    if magic <> Config.cmxs_magic_number then exit_errf "Unable to read info on %s %s." (human_name_of_kind Cmxs) filename;
    let header = (input_value ic : dynheader) in
    close_in ic;
    if header.dynu_magic <> Config.cmxs_magic_number then exit_errf "Unable to read info on %s %s." (human_name_of_kind Cmxs) filename;
    print_cmxs_infos header
  end else
  match read_current_info ~expected_kind:None ic with
  | Ok { kind = Cmo; version = _ } ->
     let cu_pos = input_binary_int ic in
     seek_in ic cu_pos;
     let cu = (input_value ic : Cmo_format.compilation_unit) in
     close_in ic;
     print_cmo_infos cu
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

let last_impl = ref None
let shared = ref false
let output = ref ""

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
      let impl = Typemod.type_implementation target env ast in
      Builtin_attributes.warn_unused ();
      Warnings.check_fatal ();
      last_impl := Some (Unit_info.modname target, file, impl.Typedtree.structure)

(* `ocamlopt -shared FILE.ml -o NAME.cmxs` (queue 528): the plugin header Cmm_helpers.plugin_header gives the unit (dynu_name and
   dynu_defines the unit's module, Compilenv's ui_defines; dynu_imports_cmi Env.imports (), Compilenv.save_unit_info's), written as
   Arbace's .cmxs: Config.cmxs_magic_number then the header as output_value writes it (ocamlopt's is an ELF shared object whose
   caml_plugin_header symbol holds the same externed record). Arbace writes no .cmx: dynu_imports_cmx is the persistent units whose
   values the typed tree names (Closure's global_approx asks their .cmx), each with its interface's digest, and dynu_crc the
   source's digest, where ocamlopt digests the .cmx (ocamltest prints both with -null-crc). *)
let write_cmxs out =
  match !last_impl with
  | None -> exit_err "oiobjinfo: -shared: no implementation compiled"
  | Some (name, file, str) ->
      let used = ref [] in
      let rec head = function Path.Pident id -> Some id | Path.Pdot (p, _) -> head p | Path.Papply (p, _) -> head p | Path.Pextra_ty (p, _) -> head p in
      let it = { Tast_iterator.default_iterator with
                 expr = (fun sub e ->
                   (match e.Typedtree.exp_desc with
                    | Typedtree.Texp_ident (p, _, _) ->
                        (match head p with
                         | Some id when Ident.global id && Ident.name id <> name && not (List.mem (Ident.name id) !used) ->
                             used := Ident.name id :: !used
                         | _ -> ())
                    | _ -> ());
                   Tast_iterator.default_iterator.expr sub e) } in
      it.structure it str;
      let cmi = Env.imports () in
      let crc_of n = try List.assoc n cmi with Not_found -> None in
      let cmx = List.map (fun n -> (n, crc_of n)) (List.sort compare !used) in
      let header = { dynu_magic = Config.cmxs_magic_number;
                     dynu_units = [ { dynu_name = name; dynu_crc = Digest.BLAKE128.file file;
                                      dynu_imports_cmi = cmi; dynu_imports_cmx = cmx; dynu_defines = [name] } ] } in
      let oc = open_out_bin out in
      output_string oc Config.cmxs_magic_number;
      output_value oc header;
      close_out oc

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
    "-shared", Arg.Set shared, "";
    "-o", Arg.Set_string output, "FILE";
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
  if !shared then write_cmxs (if !output = "" then "a.cmxs" else !output);
  let objs = ref [] in
  (try Arg.parse_argv ~current:(ref 0) (Array.append [| "ocamlobjinfo" |] (Array.sub argv (k + 1) (max 0 (n - k - 1))))
         objinfo_args (fun f -> objs := f :: !objs) "ocamlobjinfo [OPTIONS] FILES"
   with Arg.Bad m | Arg.Help m -> prerr_string m; exit 2);
  List.iter dump_obj (List.rev !objs);
  exit 0
