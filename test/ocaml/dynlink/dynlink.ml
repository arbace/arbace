(* Dynlink's platform on oi (OC220 (a), doc/ocaml/OCAML-SURVEY.md, "As built: OC220's first part"): Arbace's own, in the place of
   OCaml 5.5.0's otherlibs/dynlink/byte/dynlink.ml (LGPL-2.1 with the linking exception, read for its shape: the same functor
   application and re-exports, written anew). The library's other units are the tarball's (dynlink_types, dynlink_platform_intf,
   dynlink_common), its interface the tarball's dynlink.mli.

   The main program's units are oi's own unit table, the units its link took in their order (go/arbace/oi's Program.Units, the
   external caml_oi_dynlink_units), each an implementation already initialised, as the bytecode runtime's global map gives them; their
   interfaces' digests are the table bin/oi writes for a program that loads plugins (OI_DYNLINK_IMPORTS: "NAME DIGEST" a line, the
   interfaces the main program's units imported, as the executable's CRC table), a unit not there known by its name (None). A unit's
   module block is caml_oi_dynlink_global's (unsafe_get_global_value).

   A plugin (OC220 (a)'s loader, doc/ocaml/OCAML-SURVEY.md, "As built: OC220's loader") is the file bin/oi writes where ocamlc writes
   the unit's .cmo: the line "OI-CMO 1", the unit's interface imports ("import NAME DIGEST", the typer's --imports-out: ocamlc's
   cu_imports), the line "lambda", then the unit's Lambda as the typer translated it. load reads the header and gives the Lambda to oi
   (caml_oi_dynlink_parse: the unit's name and its implementation imports, its required units and the globals its code names, as
   bytecode's cu_required_compunits and reloc); dynlink_common checks them; run links the unit under the lock as bytecode's run patches
   it (caml_oi_dynlink_link: a global no unit defines Undefined_global, one not initialised Uninitialized_global; then compiled), then
   runs its initialisation outside it (caml_oi_dynlink_init), an exception wrapped as Library's_module_initializers_failed; finish hides
   a private load's units (caml_oi_dynlink_hide, Symtable.hide_additions). A file that does not start with the header is not a bytecode
   file. The unit's own primitives (cu_primitives, unsafe_module) are not known from its Lambda: a plugin is never unsafe here.
   A library (queue 576, doc/ocaml/OCAML-SURVEY.md "As built: queue 576"): ocamlmklib's NAME.cma as bin/oi writes it ("OI-CMA 1",
   split_cma), its shared libraries opened before its units are read, as byte/dynlink.ml's load opens lib_dllibs. *)

module DC = Dynlink_common
module DT = Dynlink_types

external program_units : unit -> string array = "caml_oi_dynlink_units"
external global_value : string -> Obj.t option = "caml_oi_dynlink_global"
external parse_unit : string -> int * string * string array = "caml_oi_dynlink_parse"
external link_unit : int -> DT.linking_error option = "caml_oi_dynlink_link"
external init_unit : int -> unit = "caml_oi_dynlink_init"
external hide_unit : int -> unit = "caml_oi_dynlink_hide"

let magic = "OI-CMO 1"
let cma_magic = "OI-CMA 1"
let dll_magic = "OI-DLL 1"

(* the main program's interface digests (OI_DYNLINK_IMPORTS), NAME DIGEST a line *)
let program_imports () =
  match Sys.getenv_opt "OI_DYNLINK_IMPORTS" with
  | None | Some "" -> []
  | Some file ->
    match open_in_bin file with
    | exception Sys_error _ -> []
    | ic ->
      let rec go acc =
        match input_line ic with
        | exception End_of_file -> close_in ic; List.rev acc
        | l ->
          match String.index_opt l ' ' with
          | None -> go acc
          | Some i ->
            let n = String.sub l 0 i and c = String.sub l (i + 1) (String.length l - i - 1) in
            if List.mem_assoc n acc then go acc else go ((n, c) :: acc)
      in
      go []

module Oi = struct
  type handle = bool * int list

  module Unit_header = struct
    type t = {
      name : string;
      file : string;
      imports : (string * string option) list;
      deps : string list;
      unit : int;
    }

    let name (t : t) = t.name
    let crc _t = None
    let interface_imports (t : t) = t.imports
    let implementation_imports (t : t) = List.map (fun n -> n, None) t.deps
    let defined_symbols (t : t) = [t.name]
    let unsafe_module _t = false
  end

  let init () =
    if !Sys.interactive then begin (* PR#6802 *)
      invalid_arg "The dynlink.cma library cannot be used \
        inside the OCaml toplevel"
    end

  let is_native = false
  let adapt_filename f = f

  let num_globals_inited () =
    failwith "Should never be called for bytecode dynlink"

  let fold_initial_units ~init ~f =
    let units = Array.to_list (program_units ()) in
    let imports = program_imports () in
    let names =
      units @ List.filter (fun n -> not (List.mem n units)) (List.map fst imports)
    in
    List.fold_left (fun acc compunit ->
        let defined = List.mem compunit units in
        f acc ~compunit ~interface:(List.assoc_opt compunit imports)
          ~implementation:(if defined then Some (None, DT.Loaded) else None)
          ~defined_symbols:(if defined then [compunit] else []))
      init
      names

  let with_lock lock f =
    Mutex.lock lock;
    Fun.protect f
      ~finally:(fun () -> Mutex.unlock lock)

  let run lock (_ : handle) ~(unit_header : Unit_header.t) ~priv:_ =
    with_lock lock (fun () ->
        match link_unit unit_header.unit with
        | None -> ()
        | Some e -> raise (DT.Error (Linking_error (unit_header.file, e))));
    (* the lock released for the initialisation: the unit may load plugins itself *)
    try init_unit unit_header.unit
    with exn ->
      Printexc.raise_with_backtrace
        (DT.Error (Library's_module_initializers_failed exn))
        (Printexc.get_raw_backtrace ())

  (* a .cmo's text: its interface imports and its Lambda *)
  let split_cmo file_name text =
    let n = String.length text in
    let bad () = raise (DT.Error (Not_a_bytecode_file file_name)) in
    let rec go pos first acc =
      match String.index_from_opt text pos '\n' with
      | None -> bad ()
      | Some j ->
        let l = String.sub text pos (j - pos) in
        if first then (if l <> magic then bad () else go (j + 1) false acc)
        else if l = "lambda" then List.rev acc, String.sub text (j + 1) (n - j - 1)
        else match String.split_on_char ' ' l with
          | ["import"; n; "-"] -> go (j + 1) false ((n, None) :: acc)
          | ["import"; n; c] -> go (j + 1) false ((n, Some c) :: acc)
          | _ -> bad ()
    in
    go 0 true []

  (* (queue 576) a library, the file bin/oi writes where ocamlmklib writes NAME.cma: the line "OI-CMA 1", its shared libraries
     ("dllib -lNAME", lib_dllibs), then each unit ("unit LENGTH" and the unit's .cmo text of that length), as bytecode's library
     holds its units' code and their names *)
  let split_cma file_name text =
    let n = String.length text in
    let bad () = raise (DT.Error (Not_a_bytecode_file file_name)) in
    let rec go pos dlls units =
      if pos >= n then List.rev dlls, List.rev units
      else match String.index_from_opt text pos '\n' with
        | None -> bad ()
        | Some j ->
          match String.split_on_char ' ' (String.sub text pos (j - pos)) with
          | ["dllib"; d] -> go (j + 1) (d :: dlls) units
          | ["unit"; len] ->
            let len = match int_of_string_opt len with Some l when l >= 0 && j + 1 + l <= n -> l | _ -> bad () in
            go (j + 1 + len) dlls (String.sub text (j + 1) len :: units)
          | _ -> bad ()
    in
    let start = String.length cma_magic + 1 in
    if n < start then bad () else go start [] []

  (* a library's shared library (Dll.open_dlls For_execution): -lNAME is dllNAME.so, found in CAML_LD_LIBRARY_PATH's directories
     (ocamltest's ld_library_path), else the name as given; the file bin/oi writes where ocamlmklib writes it (the line "OI-DLL 1",
     its C files). Its primitives are the run's C stubs (OI_CSTUBS, every library's C files on ci), each found by its name when first
     called, as oi finds any stub's *)
  let open_dll d =
    let name =
      if String.length d > 2 && String.sub d 0 2 = "-l"
      then "dll" ^ String.sub d 2 (String.length d - 2) ^ ".so" else d
    in
    let dirs =
      match Sys.getenv_opt "CAML_LD_LIBRARY_PATH" with
      | None | Some "" -> []
      | Some p -> String.split_on_char ':' p
    in
    let full =
      if not (Filename.is_implicit name) then name
      else match List.find_opt (fun dir -> Sys.file_exists (Filename.concat dir name)) dirs with
        | Some dir -> Filename.concat dir name
        | None -> name
    in
    let fail () = failwith ("Cannot load shared library " ^ name) in
    match open_in_bin full with
    | exception Sys_error _ -> fail ()
    | ic ->
      let l = try input_line ic with End_of_file -> "" in
      close_in ic;
      if l <> dll_magic then fail ()

  let load ~filename:file_name ~priv =
    let text =
      match open_in_bin file_name with
      | exception exc -> raise (DT.Error (Cannot_open_dynamic_library exc))
      | ic ->
        match really_input_string ic (in_channel_length ic) with
        | s -> close_in ic; s
        | exception exc -> close_in_noerr ic; raise (DT.Error (Cannot_open_dynamic_library exc))
    in
    let first = match String.index_opt text '\n' with Some i -> String.sub text 0 i | None -> text in
    let cmos =
      if first = magic then [text]
      else if first = cma_magic then begin
        let dlls, cmos = split_cma file_name text in
        (try List.iter open_dll dlls
         with exn -> raise (DT.Error (Cannot_open_dynamic_library exn)));
        cmos
      end else raise (DT.Error (Not_a_bytecode_file file_name))
    in
    try
      let uis = List.map (fun t ->
          let imports, lambda = split_cmo file_name t in
          let unit, name, deps = parse_unit lambda in
          { Unit_header.name; file = file_name; imports; deps = Array.to_list deps; unit }) cmos
      in
      (priv, List.map (fun (u : Unit_header.t) -> u.unit) uis), uis
    with
    | DT.Error _ as exc -> raise exc
    | exc -> raise (DT.Error (Cannot_open_dynamic_library exc))

  let unsafe_get_global_value ~bytecode_or_asm_symbol =
    global_value bytecode_or_asm_symbol

  let finish ((priv, units) : handle) =
    if priv then List.iter hide_unit units
end

include DC.Make (Oi)

type linking_error = DT.linking_error =
  | Undefined_global of string
  | Unavailable_primitive of string
  | Uninitialized_global of string

type error = DT.error =
  | Not_a_bytecode_file of string
  | Inconsistent_import of string
  | Unavailable_unit of string
  | Unsafe_file
  | Linking_error of string * linking_error
  | Corrupted_interface of string
  | Cannot_open_dynamic_library of exn
  | Library's_module_initializers_failed of exn
  | Inconsistent_implementation of string
  | Module_already_loaded of string
  | Private_library_cannot_implement_interface of string

exception Error = DT.Error
let error_message = DT.error_message
