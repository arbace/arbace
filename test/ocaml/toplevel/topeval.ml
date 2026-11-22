(**************************************************************************)
(*                                                                        *)
(*                                 OCaml                                  *)
(*                                                                        *)
(*             Xavier Leroy, projet Cristal, INRIA Rocquencourt           *)
(*                                                                        *)
(*   Copyright 1996 Institut National de Recherche en Informatique et     *)
(*     en Automatique.                                                    *)
(*                                                                        *)
(*   All rights reserved.  This file is distributed under the terms of    *)
(*   the GNU Lesser General Public License version 2.1, with the          *)
(*   special exception on linking described in the file LICENSE.          *)
(*                                                                        *)
(**************************************************************************)

(* The toplevel's evaluator on oi (queue 529, DECISION 528-7 (a); doc/ocaml/OCAML-SURVEY.md, "As built: queue 529"): modified from
   OCaml 5.5.0's toplevel/byte/topeval.ml (the release tarball, Copyright 1996 INRIA, Xavier Leroy; GNU Lesser General Public
   License version 2.1 with the OCaml LGPL linking exception, LICENSE.md). It takes that file's place in compiler-libs' ocamltoplevel,
   against the tarball's toplevel/topeval.mli: the phrases are typed and translated by compiler-libs on oi as the bytecode toplevel
   types and translates them, and only their loading differs.

   Bytecode's load_lambda compiles a phrase's Lambda to bytecode (Bytegen, Emitcode), patches it against the executable's global
   table (Symtable) and runs it (Meta.reify_bytecode). oi has no bytecode: a phrase's Lambda, simplified (Simplif), is written as the
   unit oi's loader reads (the forms of tools/ocdump's Lambda, the native typer's: (ocaml/lambda (lambda-program ...))), its code
   storing the phrase's value as the only field of a fresh global's module block (OitopN), then given to oi through Dynlink's
   externals on oi (test/ocaml/dynlink/dynlink.ml's: caml_oi_dynlink_parse reads it, caml_oi_dynlink_link links it against the
   program's globals, Symtable.patch_object's and check_global_initialized's errors, caml_oi_dynlink_init runs it on the calling
   thread); the value is that block's field, caml_oi_dynlink_global's. A phrase's globals are the program's units (oi's unit table,
   which a toplevel program links: Toploop's getvalue and setvalue among them, Translmod's toploop_ident) and the predefined
   exceptions; eval_ident reads a unit's module block the same way and a predefined exception by a phrase of its own.

   The directives that load bytecode (#load, ocaml FILE.cmo): an oi plugin file (OI-CMO 1, bin/oi's: dynlink.ml's loader) is loaded
   as Dynlink loads it, anything else is not a bytecode object file. *)

open Format
open Misc
open Parsetree
open Types
open Typedtree
open Outcometree
open Topcommon
module String = Misc.Stdlib.String

(* oi's loader (go/arbace/oi/ext.ae, test/ocaml/dynlink/dynlink.ml) *)
type oi_link_error =
  | Oi_undefined_global of string
  | Oi_unavailable_primitive of string
  | Oi_uninitialized_global of string

external oi_global : string -> Obj.t option = "caml_oi_dynlink_global"
external oi_parse : string -> int * string * string array = "caml_oi_dynlink_parse"
external oi_link : int -> oi_link_error option = "caml_oi_dynlink_link"
external oi_init : int -> unit = "caml_oi_dynlink_init"

(* The table of toplevel value bindings and its accessors *)

let toplevel_value_bindings : Obj.t String.Map.t ref = ref String.Map.empty

let getvalue name =
  try
    String.Map.find name !toplevel_value_bindings
  with Not_found ->
    fatal_error (name ^ " unbound at toplevel")

let setvalue name v =
  toplevel_value_bindings := String.Map.add name v !toplevel_value_bindings

let implementation_label = ""

(* A unit's Lambda as oi's loader reads it (go/arbace/oi/program.ae's ParseUnit): the forms tools/ocdump writes (its lam and
   lambda_program, without positions) *)
module Forms = struct
  type s =
    | A of string
    | Str of string
    | L of s list
    | V of s list

  let int n = A (string_of_int n)
  let kw k = A (":" ^ k)
  let nil = A "nil"
  let vec f l = V (List.map f l)
  let opt f = function None -> nil | Some x -> f x

  let valid_utf8 str =
    let n = String.length str in
    let rec go i =
      if i >= n then true
      else
        let c = Char.code str.[i] in
        let k =
          if c < 0x80 then 1
          else if c >= 0xC2 && c <= 0xDF then 2
          else if c >= 0xE0 && c <= 0xEF then 3
          else if c >= 0xF0 && c <= 0xF4 then 4
          else 0
        in
        if k = 0 || i + k > n then false
        else begin
          let ok = ref true in
          for j = 1 to k - 1 do
            if Char.code str.[i + j] land 0xC0 <> 0x80 then ok := false
          done;
          if !ok && k = 3 then begin
            let c1 = Char.code str.[i + 1] in
            if (c = 0xE0 && c1 < 0xA0) || (c = 0xED && c1 >= 0xA0) then ok := false
          end;
          if !ok && k = 4 then begin
            let c1 = Char.code str.[i + 1] in
            if (c = 0xF0 && c1 < 0x90) || (c = 0xF4 && c1 >= 0x90) then ok := false
          end;
          !ok && go (i + k)
        end
    in
    go 0

  let escape b str =
    String.iter
      (fun c ->
        match c with
        | '"' -> Buffer.add_string b "\\\""
        | '\\' -> Buffer.add_string b "\\\\"
        | '\n' -> Buffer.add_string b "\\n"
        | '\t' -> Buffer.add_string b "\\t"
        | '\r' -> Buffer.add_string b "\\r"
        | c when Char.code c < 0x20 || Char.code c = 0x7f ->
            Buffer.add_string b (Printf.sprintf "\\u%04X" (Char.code c))
        | c -> Buffer.add_char b c)
      str

  let rec flat b = function
    | A a -> Buffer.add_string b a
    | Str x -> Buffer.add_char b '"'; escape b x; Buffer.add_char b '"'
    | L l -> Buffer.add_char b '('; seq b l; Buffer.add_char b ')'
    | V l -> Buffer.add_char b '['; seq b l; Buffer.add_char b ']'
  and seq b l =
    List.iteri (fun i x -> if i > 0 then Buffer.add_char b ' '; flat b x) l

  let bytes_or_string str =
    if valid_utf8 str then Str str
    else V (List.init (String.length str) (fun i -> int (Char.code str.[i])))

  let ident id = Str (Ident.unique_name id)
  let prim_text p = Format.asprintf "%a" Printlambda.primitive p
  let vk_text vk = Format.asprintf "%a" Printlambda.value_kind vk
  let dir_flag = function Asttypes.Upto -> kw "to" | Asttypes.Downto -> kw "downto"

  let rec struct_const (c : Lambda.structured_constant) =
    match c with
    | Const_int n -> L [ A "int"; int n ]
    | Const_char ch -> L [ A "char"; int (Char.code ch) ]
    | Const_float t -> L [ A "float"; Str t ]
    | Const_int32 n -> L [ A "int32"; Str (Int32.to_string n) ]
    | Const_int64 n -> L [ A "int64"; Str (Int64.to_string n) ]
    | Const_nativeint n -> L [ A "nativeint"; Str (Nativeint.to_string n) ]
    | Const_block (tag, l) -> L [ A "block"; int tag; vec struct_const l ]
    | Const_float_array l -> L [ A "float-array"; vec (fun x -> Str x) l ]
    | Const_immstring s -> L [ A "immstring"; bytes_or_string s ]

  let rec lam (l : Lambda.lambda) =
    match l with
    | Lvar id -> L [ A "var"; ident id ]
    | Lmutvar id -> L [ A "mutvar"; ident id ]
    | Lconst c -> L [ A "const"; struct_const c ]
    | Lapply a -> L [ A "apply"; lam a.ap_func; vec lam a.ap_args ]
    | Lfunction f ->
        L [ A "function";
            (match f.kind with Curried -> kw "curried" | Tupled -> kw "tupled");
            vec (fun (id, vk) -> V [ ident id; Str (vk_text vk) ]) f.params;
            Str (vk_text f.return); lam f.body ]
    | Llet (k, vk, id, d, b) ->
        L [ A "let"; kw (match k with Strict -> "strict" | Alias -> "alias" | StrictOpt -> "strictopt");
            Str (vk_text vk); ident id; lam d; lam b ]
    | Lmutlet (vk, id, d, b) -> L [ A "mutlet"; Str (vk_text vk); ident id; lam d; lam b ]
    | Lletrec (bs, b) ->
        L [ A "letrec"; vec (fun (r : Lambda.rec_binding) -> V [ ident r.id; lam (Lfunction r.def) ]) bs; lam b ]
    | Lprim (p, args, _) -> L [ A "prim"; Str (prim_text p); vec lam args ]
    | Lswitch (e, sw, _) ->
        L [ A "switch"; lam e; int sw.sw_numconsts;
            vec (fun (i, x) -> V [ int i; lam x ]) sw.sw_consts;
            int sw.sw_numblocks;
            vec (fun (i, x) -> V [ int i; lam x ]) sw.sw_blocks;
            opt lam sw.sw_failaction ]
    | Lstringswitch (e, cs, d, _) ->
        L [ A "stringswitch"; lam e; vec (fun (s, x) -> V [ bytes_or_string s; lam x ]) cs; opt lam d ]
    | Lstaticraise (n, args) -> L [ A "staticraise"; int n; vec lam args ]
    | Lstaticcatch (b, (n, vars), h) ->
        L [ A "staticcatch"; lam b; V [ int n; vec (fun (id, vk) -> V [ ident id; Str (vk_text vk) ]) vars ]; lam h ]
    | Ltrywith (b, id, h) -> L [ A "trywith"; lam b; ident id; lam h ]
    | Lifthenelse (c, t, f) -> L [ A "ifthenelse"; lam c; lam t; lam f ]
    | Lsequence (a, b) -> L [ A "sequence"; lam a; lam b ]
    | Lwhile (c, b) -> L [ A "while"; lam c; lam b ]
    | Lfor (id, a, b, d, body) -> L [ A "for"; ident id; lam a; lam b; dir_flag d; lam body ]
    | Lassign (id, e) -> L [ A "assign"; ident id; lam e ]
    | Lsend (k, m, o, args, _) ->
        L [ A "send"; kw (match k with Self -> "self" | Public -> "public" | Cached -> "cached");
            lam m; lam o; vec lam args ]
    | Levent (e, _) -> lam e
    | Lifused (id, e) -> L [ A "ifused"; ident id; lam e ]

  let unit_text (p : Lambda.program) =
    let b = Buffer.create 1024 in
    flat b (L [ A "ocaml/lambda";
                L [ A "lambda-program"; ident p.module_ident; int p.main_module_block_size;
                    vec (fun id -> Str (Ident.name id)) (Ident.Set.elements p.required_globals);
                    lam p.code ] ]);
    Buffer.contents b
end

(* A Lambda term run on oi: its value, or the exception it raised (raised again) *)
let phrases = ref 0

(* oi's Undefined_global names "compilation unit `X'" (Dynlink's message), Symtable's error the unit *)
let unit_of_global n =
  let pre = "compilation unit `" in
  let lp = String.length pre and ln = String.length n in
  if ln > lp + 1 && String.sub n 0 lp = pre && n.[ln - 1] = '\''
  then String.sub n lp (ln - lp - 1) else n

let link_error name = function
  | Oi_undefined_global n | Oi_uninitialized_global n
    when unit_of_global n = name -> ()
  | Oi_undefined_global n ->
      let n = unit_of_global n in
      raise (Symtable.Error (Symtable.Undefined_global
                               (Symtable.Global.Glob_compunit (Cmo_format.Compunit n))))
  | Oi_unavailable_primitive p -> raise (Symtable.Error (Symtable.Unavailable_primitive p))
  | Oi_uninitialized_global n ->
      raise (Symtable.Error (Symtable.Uninitialized_global
                               (Symtable.Global.Glob_compunit (Cmo_format.Compunit n))))

let run_lambda lam =
  incr phrases;
  let name = "Oitop" ^ string_of_int !phrases in
  let id = Ident.create_persistent name in
  let code =
    Lambda.Lprim (Psetglobal id,
                  [Lambda.Lprim (Pmakeblock (0, Immutable, None), [lam], Loc_unknown)],
                  Loc_unknown) in
  let text =
    Forms.unit_text { Lambda.module_ident = id; main_module_block_size = 1;
                      required_globals = Ident.Set.empty; code } in
  let h, _, _ = oi_parse text in
  Option.iter (link_error name) (oi_link h);
  oi_init h;
  match oi_global name with
  | Some b -> Obj.field b 0
  | None -> fatal_error ("Topeval: " ^ name ^ " not initialised")

(* To print values *)

module EvalBase = struct

  let eval_ident id =
    if Ident.global id then begin
      let name = Ident.name id in
      if Ident.persistent id then
        match oi_global name with
        | Some v -> v
        | None -> raise (Undefined_global name)
      else
        (* a predefined exception: oi's own (go/arbace/oi/exn.ae's Predefs) *)
        try run_lambda (Lambda.Lprim (Pgetglobal id, [], Loc_unknown))
        with _ -> raise (Undefined_global name)
    end else begin
      let name = Translmod.toplevel_name id in
      try
        String.Map.find name !toplevel_value_bindings
      with Not_found ->
        raise (Undefined_global name)
    end

end

include Topcommon.MakeEvalPrinter(EvalBase)

(* Load in-core and execute a lambda term *)

let may_trace = ref false (* Global lock on tracing *)

let load_lambda ppf lam =
  if !Clflags.dump_rawlambda then fprintf ppf "%a@." Printlambda.lambda lam;
  let slam = Simplif.simplify_lambda lam in
  if !Clflags.dump_lambda then fprintf ppf "%a@." Printlambda.lambda slam;
  let initial_bindings = !toplevel_value_bindings in
  match
    may_trace := true;
    run_lambda slam
  with
  | retval ->
    may_trace := false;
    Result retval
  | exception (Symtable.Error _ as x) ->
    may_trace := false;
    toplevel_value_bindings := initial_bindings;
    raise x
  | exception x ->
    may_trace := false;
    record_backtrace ();
    toplevel_value_bindings := initial_bindings; (* PR#6211 *)
    Exception x

(* Print the outcome of an evaluation *)

let pr_item =
  Out_type.print_items
    (fun env -> function
      | Sig_value(id, {val_kind = Val_reg; val_type}, _) ->
          Some (outval_of_value env (getvalue (Translmod.toplevel_name id))
                  val_type)
      | _ -> None
    )

(* Execute a toplevel phrase *)

let execute_phrase print_outcome ppf phr =
  match phr with
  | Ptop_def sstr ->
      let oldenv = !toplevel_env in
      let (str, sg', newenv) = typecheck_phrase ppf oldenv sstr in
      let lam = Translmod.transl_toplevel_definition str in
      Warnings.check_fatal ();
      begin try
        toplevel_env := newenv;
        let res = load_lambda ppf lam in
        let out_phr =
          match res with
          | Result v ->
              if print_outcome then
                Printtyp.wrap_printing_env ~error:false oldenv (fun () ->
                  match str.str_items with
                  | [] -> Ophr_signature []
                  | _ ->
                      match find_eval_phrase str with
                      | Some (exp, _, _) ->
                        let outv = outval_of_value newenv v exp.exp_type in
                        let ty =
                          Out_type.prepare_for_printing [exp.exp_type];
                          Out_type.tree_of_typexp Type_scheme exp.exp_type
                        in
                        Ophr_eval (outv, ty)
                      | None -> Ophr_signature (pr_item oldenv sg'))
              else Ophr_signature []
          | Exception exn ->
              toplevel_env := oldenv;
              if exn = Out_of_memory then Gc.full_major();
              let outv =
                outval_of_value !toplevel_env (Obj.repr exn) Predef.type_exn
              in
              Ophr_exception (exn, outv)
        in
        begin match out_phr with
        | Ophr_signature [] -> ()
        | _ ->
            Location.separate_new_message ppf;
            !print_out_phrase ppf out_phr;
        end;
        if Printexc.backtrace_status ()
        then begin
          match !backtrace with
            | None -> ()
            | Some b ->
                Location.separate_new_message ppf;
                pp_print_string ppf b;
                pp_print_flush ppf ();
                backtrace := None;
        end;
        begin match out_phr with
        | Ophr_eval (_, _) | Ophr_signature _ -> true
        | Ophr_exception _ -> false
        end
      with x ->
        toplevel_env := oldenv; raise x
      end
  | Ptop_dir {pdir_name = {Location.txt = dir_name}; pdir_arg } ->
      try_run_directive ppf dir_name pdir_arg

let execute_phrase print_outcome ppf phr =
  try execute_phrase print_outcome ppf phr
  with exn ->
    Warnings.reset_fatal ();
    raise exn

(* Loading files: oi's plugin files (OI-CMO 1, test/ocaml/dynlink/dynlink.ml's format), linked and run as Dynlink runs a plugin *)

let oi_magic = "OI-CMO 1"

let load_file _recursive ppf name =
  let filename =
    try Some (Load_path.find name) with Not_found -> None
  in
  match filename with
  | None -> fprintf ppf "Cannot find file %s.@." name; false
  | Some filename ->
      let ic = open_in_bin filename in
      Misc.try_finally
        ~always:(fun () -> close_in ic)
        (fun () ->
           match input_line ic with
           | exception End_of_file ->
               fprintf ppf "File %s is not a bytecode object file.@." name; false
           | l when l <> oi_magic ->
               fprintf ppf "File %s is not a bytecode object file.@." name; false
           | _ ->
               let rec skip () = if input_line ic <> "lambda" then skip () in
               (try skip () with End_of_file -> ());
               let text = really_input_string ic (in_channel_length ic - pos_in ic) in
               let h, unit_name, _ = oi_parse text in
               match
                 may_trace := true;
                 Option.iter (link_error unit_name) (oi_link h);
                 oi_init h
               with
               | () -> may_trace := false; true
               | exception exn ->
                   record_backtrace ();
                   may_trace := false;
                   print_exception_outcome ppf exn;
                   false)

let init () =
  Compmisc.init_path ();
  ()
