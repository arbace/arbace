(* Dynlink's platform on oi (OC220 (a), doc/ocaml/OCAML-SURVEY.md, "As built: OC220's first part"): Arbace's own, in the place of
   OCaml 5.5.0's otherlibs/dynlink/byte/dynlink.ml (LGPL-2.1 with the linking exception, read for its shape: the same functor
   application and re-exports, written anew). The library's other units are the tarball's (dynlink_types, dynlink_platform_intf,
   dynlink_common), its interface the tarball's dynlink.mli.

   The main program's units are oi's own unit table, the units its link took in their order (go/arbace/oi's Program.Units, the
   external caml_oi_dynlink_units), each an implementation already initialised, as the bytecode runtime's global map gives them; the
   interfaces' CRCs are not kept (None: a unit's interface is known by its name). A unit's module block is caml_oi_dynlink_global's
   (unsafe_get_global_value). Loading a plugin is not built yet: loadfile opens the file as the bytecode one does (a missing file is
   Cannot_open_dynamic_library of its Sys_error) and then refuses it as Cannot_open_dynamic_library (Failure ...). *)

module DC = Dynlink_common
module DT = Dynlink_types

external program_units : unit -> string array = "caml_oi_dynlink_units"
external global_value : string -> Obj.t option = "caml_oi_dynlink_global"

module Oi = struct
  type handle = unit

  module Unit_header = struct
    type t = string

    let name (t : t) = t
    let crc _t = None
    let interface_imports _t = []
    let implementation_imports _t = []
    let defined_symbols (t : t) = [t]
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
    Array.fold_left (fun acc compunit ->
        f acc ~compunit ~interface:None
          ~implementation:(Some (None, DT.Loaded))
          ~defined_symbols:[compunit])
      init
      (program_units ())

  let run _lock () ~unit_header:_ ~priv:_ = ()

  let load ~filename ~priv:_ =
    let ic =
      try open_in_bin filename
      with exc -> raise (DT.Error (Cannot_open_dynamic_library exc))
    in
    close_in_noerr ic;
    raise (DT.Error (Cannot_open_dynamic_library
      (Failure ("oi: loading " ^ filename ^ " is not built (OC220)"))))

  let unsafe_get_global_value ~bytecode_or_asm_symbol =
    global_value bytecode_or_asm_symbol

  let finish () = ()
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
