(* ocdump: OCaml's Parsetree (and, for compilable files, the typed annotations and Lambda) as OCaml
   forms (doc/ocaml/SPEC.md), written for Arbace's reader.  The helper of repr/ocaml (OC3 (a)), the
   counterpart of tools/hsdump and tools/rsdump: it decides nothing about the forms beyond writing the
   spec's vocabulary (SPEC §2-§7), so the converter of O2 is near the identity (OC8 (a)).

   Built by `bin/repr-ocaml ocdump-build` with the host's ocamlopt against compiler-libs
   (`-I +compiler-libs ocamlcommon.cmxa`), pinned by tools/ocdump/OCAML_VERSION.

   Usage: ocdump [--loc] [--intf] [--use-file] [--mode parse|types|lambda|tokens] [--file NAME] [FILE]
   FILE is read (stdin when absent).  Output: one form on stdout; exit 0 even for a syntax error
   (the form is then (ocaml/error ...), SPEC §8). *)

open Parsetree
open Asttypes

(* ---- the output tree ---- *)

type s =
  | A of string            (* an atom, written as is *)
  | Str of string          (* a string literal *)
  | L of s list            (* (a b c) *)
  | V of s list            (* [a b c] *)
  | M of (string * s) list (* {:k v ...} *)

let nil = A "nil"
let kw k = A (":" ^ k)
let int n = A (string_of_int n)
let bool b = A (if b then "true" else "false")

let with_loc = ref false

(* a string literal's body: valid UTF-8 passes through, controls are escaped *)
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
        (* overlongs and surrogates *)
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
  | M kvs ->
      Buffer.add_char b '{';
      List.iteri
        (fun i (k, v) ->
          if i > 0 then Buffer.add_char b ' ';
          Buffer.add_string b (":" ^ k); Buffer.add_char b ' '; flat b v)
        kvs;
      Buffer.add_char b '}'
and seq b l =
  List.iteri (fun i x -> if i > 0 then Buffer.add_char b ' '; flat b x) l

let flat_string x = let b = Buffer.create 64 in flat b x; Buffer.contents b

(* pretty: a list that does not fit in the width breaks after its head, one child a line *)
(* flat_within x n: Some (the flat text) when it is at most n bytes, else None, without flattening the rest
   (a deep tree would cost its size at every level: O4's editor.ml) *)
exception Too_long
let flat_within x n =
  let b = Buffer.create 64 in
  let rec go = function
    | A a -> Buffer.add_string b a; chk ()
    | Str x -> Buffer.add_char b '"'; escape b x; Buffer.add_char b '"'; chk ()
    | L l -> Buffer.add_char b '('; sq l; Buffer.add_char b ')'
    | V l -> Buffer.add_char b '['; sq l; Buffer.add_char b ']'
    | M kvs ->
        Buffer.add_char b '{';
        List.iteri (fun i (k, v) ->
            if i > 0 then Buffer.add_char b ' ';
            Buffer.add_string b (":" ^ k); Buffer.add_char b ' '; go v) kvs;
        Buffer.add_char b '}'; chk ()
  and sq l = List.iteri (fun i x -> if i > 0 then Buffer.add_char b ' '; go x) l
  and chk () = if Buffer.length b > n then raise Too_long in
  try go x; Some (Buffer.contents b) with Too_long -> None

(* the indentation stops growing at [max_indent]: a chain of 10,000 nested lets would otherwise cost 10,000 spaces a line *)
let max_indent = 60

let rec pretty b ind width x =
  let ind = min ind max_indent in
  match (match x with A _ | Str _ -> Some (flat_string x) | _ -> flat_within x (width - ind)) with
  | Some f -> Buffer.add_string b f
  | None ->
    let f = "" in
    match x with
    | L (h :: rest) ->
        Buffer.add_char b '(';
        flat b h;
        List.iter
          (fun c ->
            Buffer.add_char b '\n';
            Buffer.add_string b (String.make (ind + 1) ' ');
            pretty b (ind + 1) width c)
          rest;
        Buffer.add_char b ')'
    | V l | L l ->
        Buffer.add_char b (match x with V _ -> '[' | _ -> '(');
        List.iteri
          (fun i c ->
            if i > 0 then begin
              Buffer.add_char b '\n'; Buffer.add_string b (String.make (ind + 1) ' ')
            end;
            pretty b (ind + 1) width c)
          l;
        Buffer.add_char b (match x with V _ -> ']' | _ -> ')')
    | M kvs ->
        Buffer.add_char b '{';
        List.iteri
          (fun i (k, v) ->
            if i > 0 then begin
              Buffer.add_char b '\n'; Buffer.add_string b (String.make (ind + 1) ' ')
            end;
            Buffer.add_string b (":" ^ k ^ " ");
            pretty b (ind + 2 + String.length k) width v)
          kvs;
        Buffer.add_char b '}'
    | _ -> Buffer.add_string b f

(* ---- locations ---- *)

let pos_vec (l : Location.t) =
  let s = l.loc_start and e = l.loc_end in
  V [ int s.pos_lnum; int (s.pos_cnum - s.pos_bol);
      int e.pos_lnum; int (e.pos_cnum - e.pos_bol);
      int s.pos_cnum; int e.pos_cnum ]

(* the metadata of a node: attributes first, then positions *)
let meta ?(attrs = []) ?(stack = []) (loc : Location.t option) =
  let m = [] in
  let m = if attrs <> [] then m @ [ ("attrs", V attrs) ] else m in
  let m =
    match loc with
    | Some l when !with_loc ->
        m @ [ ("loc", pos_vec l) ] @ (if l.loc_ghost then [ ("ghost", bool true) ] else [])
    | _ -> m
  in
  let m =
    if !with_loc && stack <> [] then m @ [ ("stack", V (List.map pos_vec stack)) ] else m
  in
  m

let node ?(attrs = []) ?(stack = []) ?loc head fields =
  let m = meta ~attrs ~stack loc in
  L (A head :: fields @ (if m = [] then [] else [ M m ]))

let at (l : Location.t) x = if !with_loc then L [ A "at"; x; pos_vec l ] else x

(* ---- names ---- *)

let is_alpha c = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c = '_'
let is_alnum c = is_alpha c || (c >= '0' && c <= '9') || c = '\''

let bare_component c =
  c <> "" && is_alpha c.[0]
  && (let ok = ref true in String.iter (fun ch -> if not (is_alnum ch) then ok := false) c; !ok)

let bare_name c = bare_component c && c <> "true" && c <> "false" && c <> "nil"

let name c = if bare_name c then A c else L [ A "name"; Str c ]

(* a name whose text is valid UTF-8 only (identifiers are) *)
let name_loc (n : string loc) = at n.loc (name n.txt)

let lident_special = function
  | "true" -> Some (kw "true") | "false" -> Some (kw "false")
  | "()" -> Some (kw "unit") | "[]" -> Some (kw "nil") | "::" -> Some (kw "cons")
  | _ -> None

let rec has_apply = function
  | Longident.Lident _ -> false
  | Ldot (l, _) -> has_apply l.txt
  | Lapply _ -> true

let rec lid_text = function
  | Longident.Lident s -> s
  | Ldot (l, s) -> lid_text l.txt ^ "." ^ s.txt
  | Lapply _ -> assert false

let rec lid_form (l : Longident.t) =
  match l with
  | Lident s -> (match lident_special s with Some k -> k | None -> name s)
  | Ldot _ when not (has_apply l) ->
      let rec comps = function
        | Longident.Lident s -> [ s ]
        | Ldot (p, s) -> comps p.txt @ [ s.txt ]
        | Lapply _ -> assert false
      in
      let cs = comps l in
      if List.for_all bare_component cs && bare_name (List.nth cs (List.length cs - 1))
      then A (lid_text l)
      else L [ A "name"; Str (lid_text l) ]
  | Ldot (p, s) -> L [ A "ldot"; lid_form p.txt; name s.txt ]
  | Lapply (a, b) -> L [ A "lapply"; lid_form a.txt; lid_form b.txt ]

let lid (l : Longident.t loc) = at l.loc (lid_form l.txt)

(* ---- small helpers ---- *)

let vec f l = V (List.map f l)
let opt f = function None -> nil | Some x -> f x
let opt_tail f = function None -> [] | Some x -> [ f x ]

let rec_flag = function Nonrecursive -> kw "nonrec" | Recursive -> kw "rec"
let dir_flag = function Upto -> kw "to" | Downto -> kw "downto"
let private_flag = function Private -> kw "private" | Public -> kw "public"
let mutable_flag = function Mutable -> kw "mutable" | Immutable -> kw "immutable"
let virtual_flag = function Virtual -> kw "virtual" | Concrete -> kw "concrete"
let override_flag = function Override -> kw "override" | Fresh -> kw "fresh"
let closed_flag = function Closed -> kw "closed" | Open -> kw "open"

let arg_label = function
  | Nolabel -> nil
  | Labelled l -> kw l
  | Optional l -> kw ("?" ^ l)

let variance = function
  | Covariant -> kw "+" | Contravariant -> kw "-" | Bivariant -> kw "+-" | NoVariance -> nil

let injectivity = function Injective -> kw "!" | NoInjectivity -> nil

(* bytes of a string constant: a string when valid UTF-8, else the vector of its bytes *)
let bytes_or_string str =
  if valid_utf8 str then Str str
  else V (List.init (String.length str) (fun i -> int (Char.code str.[i])))

(* ---- the Parsetree ---- *)

let rec constant (c : Parsetree.constant) =
  match c.pconst_desc with
  | Pconst_integer (t, sfx) ->
      node ~loc:c.pconst_loc "int" (Str t :: opt_tail (fun ch -> int (Char.code ch)) sfx)
  | Pconst_char ch -> node ~loc:c.pconst_loc "char" [ int (Char.code ch) ]
  | Pconst_string (str, l, delim) ->
      let loc_part = if !with_loc then [ ("content-loc", pos_vec l) ] else [] in
      let m = meta (Some c.pconst_loc) @ loc_part in
      L ([ A "string"; bytes_or_string str ] @ (match delim with None -> [] | Some d -> [ Str d ])
         @ (if m = [] then [] else [ M m ]))
  | Pconst_float (t, sfx) ->
      node ~loc:c.pconst_loc "float" (Str t :: opt_tail (fun ch -> int (Char.code ch)) sfx)

and attributes l = List.map attribute l

and attribute (a : attribute) =
  node ~loc:a.attr_loc "attr" [ name_loc a.attr_name; payload a.attr_payload ]

and extension ((n, p) : extension) = L [ A "ext"; name_loc n; payload p ]

and payload = function
  | PStr s -> L [ A "str"; structure s ]
  | PSig s -> L [ A "sig"; signature s ]
  | PTyp t -> L [ A "typ"; core_type t ]
  | PPat (p, g) -> L ([ A "pat"; pattern p ] @ opt_tail expression g)

and label_opt_list : 'a. ('a -> s) -> (string option * 'a) list -> s = fun f l ->
  (* tuple elements: an unlabelled element is the form itself, a labelled one [:l form] *)
  V (List.map (fun (lbl, x) -> match lbl with None -> f x | Some s -> V [ kw s; f x ]) l)

and core_type (t : core_type) =
  let n h f = node ~attrs:(attributes t.ptyp_attributes) ~stack:t.ptyp_loc_stack ~loc:t.ptyp_loc h f in
  match t.ptyp_desc with
  | Ptyp_any -> n "any" []
  | Ptyp_var v -> n "var" [ name v ]
  | Ptyp_arrow (l, a, b) -> n "arrow" [ arg_label l; core_type a; core_type b ]
  | Ptyp_tuple l -> n "tuple" [ label_opt_list core_type l ]
  | Ptyp_constr (l, args) -> n "constr" [ lid l; vec core_type args ]
  | Ptyp_object (fs, cl) -> n "object" [ vec object_field fs; closed_flag cl ]
  | Ptyp_class (l, args) -> n "class" [ lid l; vec core_type args ]
  | Ptyp_alias (t, v) -> n "alias" [ core_type t; name_loc v ]
  | Ptyp_variant (rows, cl, labels) ->
      n "variant" [ vec row_field rows; closed_flag cl; opt (vec name) labels ]
  | Ptyp_poly (vs, t) -> n "poly" [ vec name_loc vs; core_type t ]
  | Ptyp_package p -> n "package" [ package_type p ]
  | Ptyp_open (l, t) -> n "open" [ lid l; core_type t ]
  | Ptyp_extension e -> n "extension" [ extension e ]
  | Ptyp_functor (l, v, p, t) -> n "functor" [ arg_label l; name_loc v; package_type p; core_type t ]

and package_type (p : package_type) =
  node ~attrs:(attributes p.ppt_attrs) ~loc:p.ppt_loc "pkg"
    [ lid p.ppt_path; vec (fun (l, t) -> V [ lid l; core_type t ]) p.ppt_constraints ]

and row_field (r : row_field) =
  let n h f = node ~attrs:(attributes r.prf_attributes) ~loc:r.prf_loc h f in
  match r.prf_desc with
  | Rtag (l, b, ts) -> n "rtag" [ name_loc l; bool b; vec core_type ts ]
  | Rinherit t -> n "rinherit" [ core_type t ]

and object_field (o : object_field) =
  let n h f = node ~attrs:(attributes o.pof_attributes) ~loc:o.pof_loc h f in
  match o.pof_desc with
  | Otag (l, t) -> n "tag" [ name_loc l; core_type t ]
  | Oinherit t -> n "inherit" [ core_type t ]

and pattern (p : pattern) =
  let n h f = node ~attrs:(attributes p.ppat_attributes) ~stack:p.ppat_loc_stack ~loc:p.ppat_loc h f in
  match p.ppat_desc with
  | Ppat_any -> n "any" []
  | Ppat_var v -> n "var" [ name_loc v ]
  | Ppat_alias (p, v) -> n "alias" [ pattern p; name_loc v ]
  | Ppat_constant c -> n "constant" [ constant c ]
  | Ppat_interval (a, b) -> n "interval" [ constant a; constant b ]
  | Ppat_tuple (l, cl) -> n "tuple" [ label_opt_list pattern l; closed_flag cl ]
  | Ppat_construct (l, a) ->
      n "construct"
        (lid l :: (match a with None -> [] | Some (vs, p) -> [ vec name_loc vs; pattern p ]))
  | Ppat_variant (l, p) -> n "variant" (name l :: opt_tail pattern p)
  | Ppat_record (fs, cl) ->
      n "record" [ vec (fun (l, p) -> V [ lid l; pattern p ]) fs; closed_flag cl ]
  | Ppat_array l -> n "array" [ vec pattern l ]
  | Ppat_or (a, b) -> n "or" [ pattern a; pattern b ]
  | Ppat_constraint (p, t) -> n "constraint" [ pattern p; core_type t ]
  | Ppat_type l -> n "type" [ lid l ]
  | Ppat_lazy p -> n "lazy" [ pattern p ]
  | Ppat_unpack (nm, pt) ->
      n "unpack" (at nm.loc (opt name nm.txt) :: opt_tail package_type pt)
  | Ppat_exception p -> n "exception" [ pattern p ]
  | Ppat_effect (a, b) -> n "effect" [ pattern a; pattern b ]
  | Ppat_extension e -> n "extension" [ extension e ]
  | Ppat_open (l, p) -> n "open" [ lid l; pattern p ]

and expression (e : expression) =
  let n h f = node ~attrs:(attributes e.pexp_attributes) ~stack:e.pexp_loc_stack ~loc:e.pexp_loc h f in
  match e.pexp_desc with
  | Pexp_ident l -> n "ident" [ lid l ]
  | Pexp_constant c -> n "constant" [ constant c ]
  | Pexp_let (r, vbs, b) -> n "let" [ rec_flag r; vec value_binding vbs; expression b ]
  | Pexp_function (ps, c, b) ->
      n "function" [ vec function_param ps; opt type_constraint c; function_body b ]
  | Pexp_apply (f, args) ->
      n "apply" [ expression f; vec (fun (l, a) -> V [ arg_label l; expression a ]) args ]
  | Pexp_match (e, cs) -> n "match" [ expression e; vec case cs ]
  | Pexp_try (e, cs) -> n "try" [ expression e; vec case cs ]
  | Pexp_tuple l -> n "tuple" [ label_opt_list expression l ]
  | Pexp_construct (l, a) -> n "construct" (lid l :: opt_tail expression a)
  | Pexp_variant (l, a) -> n "variant" (name l :: opt_tail expression a)
  | Pexp_record (fs, b) ->
      n "record" (vec (fun (l, e) -> V [ lid l; expression e ]) fs :: opt_tail expression b)
  | Pexp_field (e, l) -> n "field" [ expression e; lid l ]
  | Pexp_setfield (a, l, b) -> n "setfield" [ expression a; lid l; expression b ]
  | Pexp_array l -> n "array" [ vec expression l ]
  | Pexp_ifthenelse (c, t, f) -> n "ifthenelse" ([ expression c; expression t ] @ opt_tail expression f)
  | Pexp_sequence (a, b) -> n "sequence" [ expression a; expression b ]
  | Pexp_while (c, b) -> n "while" [ expression c; expression b ]
  | Pexp_for (p, a, b, d, body) ->
      n "for" [ pattern p; expression a; expression b; dir_flag d; expression body ]
  | Pexp_constraint (e, t) -> n "constraint" [ expression e; core_type t ]
  | Pexp_coerce (e, f, t) -> n "coerce" [ expression e; opt core_type f; core_type t ]
  | Pexp_send (e, m) -> n "send" [ expression e; name_loc m ]
  | Pexp_new l -> n "new" [ lid l ]
  | Pexp_setinstvar (v, e) -> n "setinstvar" [ name_loc v; expression e ]
  | Pexp_override fs ->
      n "override" [ vec (fun (l, e) -> V [ name_loc l; expression e ]) fs ]
  | Pexp_struct_item (si, e) -> n "struct-item" [ structure_item si; expression e ]
  | Pexp_assert e -> n "assert" [ expression e ]
  | Pexp_lazy e -> n "lazy" [ expression e ]
  | Pexp_poly (e, t) -> n "poly" (expression e :: opt_tail core_type t)
  | Pexp_object cs -> n "object" [ class_structure cs ]
  | Pexp_newtype (v, e) -> n "newtype" [ name_loc v; expression e ]
  | Pexp_pack (m, pt) -> n "pack" (module_expr m :: opt_tail package_type pt)
  | Pexp_letop l -> n "letop" [ binding_op l.let_; vec binding_op l.ands; expression l.body ]
  | Pexp_extension x -> n "extension" [ extension x ]
  | Pexp_unreachable -> n "unreachable" []

and binding_op (b : binding_op) =
  node ~loc:b.pbop_loc "bop" [ name_loc b.pbop_op; pattern b.pbop_pat; expression b.pbop_exp ]

and case (c : case) =
  L [ A "case"; pattern c.pc_lhs; opt expression c.pc_guard; expression c.pc_rhs ]

and function_param (p : function_param) =
  match p.pparam_desc with
  | Pparam_val (l, d, pat) ->
      node ~loc:p.pparam_loc "param" [ arg_label l; opt expression d; pattern pat ]
  | Pparam_newtype v -> node ~loc:p.pparam_loc "newtype" [ name_loc v ]

and function_body = function
  | Pfunction_body e -> L [ A "body"; expression e ]
  | Pfunction_cases (cs, loc, attrs) ->
      node ~attrs:(attributes attrs) ~loc "cases" [ vec case cs ]

and type_constraint = function
  | Pconstraint t -> L [ A "constraint"; core_type t ]
  | Pcoerce (f, t) -> L [ A "coerce"; opt core_type f; core_type t ]

and value_description (v : value_description) head =
  node ~attrs:(attributes v.pval_attributes) ~loc:v.pval_loc head
    [ name_loc v.pval_name; core_type v.pval_type; vec (fun s -> Str s) v.pval_prim ]

and type_param (t, (v, i)) = V [ core_type t; variance v; injectivity i ]

and type_declaration (d : type_declaration) =
  node ~attrs:(attributes d.ptype_attributes) ~loc:d.ptype_loc "decl"
    ([ name_loc d.ptype_name;
       vec type_param d.ptype_params;
       vec (fun (a, b, l) -> node ~loc:l "constraint" [ core_type a; core_type b ]) d.ptype_constraints;
       type_kind d.ptype_kind;
       private_flag d.ptype_private ]
     @ opt_tail core_type d.ptype_manifest)

and type_kind = function
  | Ptype_abstract -> kw "abstract"
  | Ptype_variant cs -> L [ A "variant"; vec constructor_declaration cs ]
  | Ptype_record ls -> L [ A "record"; vec label_declaration ls ]
  | Ptype_open -> kw "open"
  | Ptype_external s -> L [ A "external"; Str s ]

and label_declaration (l : label_declaration) =
  node ~attrs:(attributes l.pld_attributes) ~loc:l.pld_loc "ldecl"
    [ name_loc l.pld_name; mutable_flag l.pld_mutable; core_type l.pld_type ]

and constructor_arguments = function
  | Pcstr_tuple ts -> vec core_type ts
  | Pcstr_record ls -> L [ A "record"; vec label_declaration ls ]

and constructor_declaration (c : constructor_declaration) =
  node ~attrs:(attributes c.pcd_attributes) ~loc:c.pcd_loc "cdecl"
    ([ name_loc c.pcd_name; vec name_loc c.pcd_vars; constructor_arguments c.pcd_args ]
     @ opt_tail core_type c.pcd_res)

and type_extension (t : type_extension) =
  node ~attrs:(attributes t.ptyext_attributes) ~loc:t.ptyext_loc "typext"
    [ lid t.ptyext_path; vec type_param t.ptyext_params;
      vec extension_constructor t.ptyext_constructors; private_flag t.ptyext_private ]

and extension_constructor (c : extension_constructor) =
  let kind =
    match c.pext_kind with
    | Pext_decl (vs, args, res) ->
        L ([ A "decl"; vec name_loc vs; constructor_arguments args ] @ opt_tail core_type res)
    | Pext_rebind l -> L [ A "rebind"; lid l ]
  in
  node ~attrs:(attributes c.pext_attributes) ~loc:c.pext_loc "econstr" [ name_loc c.pext_name; kind ]

and type_exception (t : type_exception) =
  node ~attrs:(attributes t.ptyexn_attributes) ~loc:t.ptyexn_loc "exception"
    [ extension_constructor t.ptyexn_constructor ]

and class_type (c : class_type) =
  let n h f = node ~attrs:(attributes c.pcty_attributes) ~loc:c.pcty_loc h f in
  match c.pcty_desc with
  | Pcty_constr (l, ts) -> n "constr" [ lid l; vec core_type ts ]
  | Pcty_signature s -> n "signature" [ class_signature s ]
  | Pcty_arrow (l, t, c) -> n "arrow" [ arg_label l; core_type t; class_type c ]
  | Pcty_extension e -> n "extension" [ extension e ]
  | Pcty_open (o, c) -> n "open" [ open_description o; class_type c ]

and class_signature (s : class_signature) =
  L [ A "csig"; core_type s.pcsig_self; vec class_type_field s.pcsig_fields ]

and class_type_field (f : class_type_field) =
  let n h fl = node ~attrs:(attributes f.pctf_attributes) ~loc:f.pctf_loc h fl in
  match f.pctf_desc with
  | Pctf_inherit c -> n "inherit" [ class_type c ]
  | Pctf_val (l, m, v, t) -> n "val" [ name_loc l; mutable_flag m; virtual_flag v; core_type t ]
  | Pctf_method (l, p, v, t) -> n "method" [ name_loc l; private_flag p; virtual_flag v; core_type t ]
  | Pctf_constraint (a, b) -> n "constraint" [ core_type a; core_type b ]
  | Pctf_attribute a -> n "attribute" [ attribute a ]
  | Pctf_extension e -> n "extension" [ extension e ]

and class_infos : 'a. ('a -> s) -> 'a class_infos -> s =
  fun f c ->
  node ~attrs:(attributes c.pci_attributes) ~loc:c.pci_loc "class-infos"
    [ virtual_flag c.pci_virt; vec type_param c.pci_params; name_loc c.pci_name; f c.pci_expr ]

and class_expr (c : class_expr) =
  let n h f = node ~attrs:(attributes c.pcl_attributes) ~loc:c.pcl_loc h f in
  match c.pcl_desc with
  | Pcl_constr (l, ts) -> n "constr" [ lid l; vec core_type ts ]
  | Pcl_structure s -> n "structure" [ class_structure s ]
  | Pcl_fun (l, d, p, c) -> n "fun" [ arg_label l; opt expression d; pattern p; class_expr c ]
  | Pcl_apply (c, args) ->
      n "apply" [ class_expr c; vec (fun (l, a) -> V [ arg_label l; expression a ]) args ]
  | Pcl_let (r, vbs, c) -> n "let" [ rec_flag r; vec value_binding vbs; class_expr c ]
  | Pcl_constraint (c, t) -> n "constraint" [ class_expr c; class_type t ]
  | Pcl_extension e -> n "extension" [ extension e ]
  | Pcl_open (o, c) -> n "open" [ open_description o; class_expr c ]

and class_structure (c : class_structure) =
  L [ A "cstruct"; pattern c.pcstr_self; vec class_field c.pcstr_fields ]

and class_field (f : class_field) =
  let n h fl = node ~attrs:(attributes f.pcf_attributes) ~loc:f.pcf_loc h fl in
  match f.pcf_desc with
  | Pcf_inherit (o, c, nm) -> n "inherit" ([ override_flag o; class_expr c ] @ opt_tail name_loc nm)
  | Pcf_val (l, m, k) -> n "val" [ name_loc l; mutable_flag m; class_field_kind k ]
  | Pcf_method (l, p, k) -> n "method" [ name_loc l; private_flag p; class_field_kind k ]
  | Pcf_constraint (a, b) -> n "constraint" [ core_type a; core_type b ]
  | Pcf_initializer e -> n "initializer" [ expression e ]
  | Pcf_attribute a -> n "attribute" [ attribute a ]
  | Pcf_extension e -> n "extension" [ extension e ]

and class_field_kind = function
  | Cfk_virtual t -> L [ A "virtual"; core_type t ]
  | Cfk_concrete (o, e) -> L [ A "concrete"; override_flag o; expression e ]

and module_type (m : module_type) =
  let n h f = node ~attrs:(attributes m.pmty_attributes) ~loc:m.pmty_loc h f in
  match m.pmty_desc with
  | Pmty_ident l -> n "ident" [ lid l ]
  | Pmty_signature s -> n "signature" [ signature s ]
  | Pmty_functor (p, t) -> n "functor" [ functor_parameter p; module_type t ]
  | Pmty_with (t, cs) -> n "with" [ module_type t; vec with_constraint cs ]
  | Pmty_typeof m -> n "typeof" [ module_expr m ]
  | Pmty_extension e -> n "extension" [ extension e ]
  | Pmty_alias l -> n "alias" [ lid l ]

and functor_parameter = function
  | Unit -> kw "unit"
  | Named (nm, t) -> L [ A "named"; at nm.loc (opt name nm.txt); module_type t ]

and signature s = vec signature_item s

and signature_item (i : signature_item) =
  let n h f = node ~loc:i.psig_loc h f in
  match i.psig_desc with
  | Psig_value v -> value_description v "value"
  | Psig_type (r, ds) -> n "type" [ rec_flag r; vec type_declaration ds ]
  | Psig_typesubst ds -> n "typesubst" [ vec type_declaration ds ]
  | Psig_typext t -> typext_item t
  | Psig_exception t -> type_exception t
  | Psig_module d ->
      node ~attrs:(attributes d.pmd_attributes) ~loc:d.pmd_loc "module"
        [ at d.pmd_name.loc (opt name d.pmd_name.txt); module_type d.pmd_type ]
  | Psig_modsubst d ->
      node ~attrs:(attributes d.pms_attributes) ~loc:d.pms_loc "modsubst"
        [ name_loc d.pms_name; lid d.pms_manifest ]
  | Psig_recmodule ds ->
      n "recmodule"
        [ vec (fun d ->
              node ~attrs:(attributes d.pmd_attributes) ~loc:d.pmd_loc "moddecl"
                [ at d.pmd_name.loc (opt name d.pmd_name.txt); module_type d.pmd_type ]) ds ]
  | Psig_modtype d -> modtype_decl "modtype" d
  | Psig_modtypesubst d -> modtype_decl "modtypesubst" d
  | Psig_open o -> open_description o
  | Psig_include i ->
      node ~attrs:(attributes i.pincl_attributes) ~loc:i.pincl_loc "include" [ module_type i.pincl_mod ]
  | Psig_class cs -> n "class" [ vec (class_infos class_type) cs ]
  | Psig_class_type cs -> n "class-type" [ vec (class_infos class_type) cs ]
  | Psig_attribute a -> n "attribute" [ attribute a ]
  | Psig_extension (e, attrs) -> node ~attrs:(attributes attrs) ~loc:i.psig_loc "extension" [ extension e ]

and typext_item t = type_extension t

and modtype_decl head (d : module_type_declaration) =
  node ~attrs:(attributes d.pmtd_attributes) ~loc:d.pmtd_loc head
    (name_loc d.pmtd_name :: opt_tail module_type d.pmtd_type)

and open_description (o : open_description) =
  node ~attrs:(attributes o.popen_attributes) ~loc:o.popen_loc "open"
    [ lid o.popen_expr; override_flag o.popen_override ]

and with_constraint = function
  | Pwith_type (l, d) -> L [ A "type"; lid l; type_declaration d ]
  | Pwith_module (a, b) -> L [ A "module"; lid a; lid b ]
  | Pwith_modtype (l, t) -> L [ A "modtype"; lid l; module_type t ]
  | Pwith_modtypesubst (l, t) -> L [ A "modtypesubst"; lid l; module_type t ]
  | Pwith_typesubst (l, d) -> L [ A "typesubst"; lid l; type_declaration d ]
  | Pwith_modsubst (a, b) -> L [ A "modsubst"; lid a; lid b ]

and module_expr (m : module_expr) =
  let n h f = node ~attrs:(attributes m.pmod_attributes) ~loc:m.pmod_loc h f in
  match m.pmod_desc with
  | Pmod_ident l -> n "ident" [ lid l ]
  | Pmod_structure s -> n "structure" [ structure s ]
  | Pmod_functor (p, m) -> n "functor" [ functor_parameter p; module_expr m ]
  | Pmod_apply (a, b) -> n "apply" [ module_expr a; module_expr b ]
  | Pmod_apply_unit a -> n "apply-unit" [ module_expr a ]
  | Pmod_constraint (m, t) -> n "constraint" [ module_expr m; module_type t ]
  | Pmod_unpack e -> n "unpack" [ expression e ]
  | Pmod_extension e -> n "extension" [ extension e ]

and structure s = vec structure_item s

and value_binding (b : value_binding) =
  node ~attrs:(attributes b.pvb_attributes) ~loc:b.pvb_loc "binding"
    ([ pattern b.pvb_pat; expression b.pvb_expr ]
     @ opt_tail
         (function
           | Pvc_constraint { locally_abstract_univars; typ } ->
               L [ A "constraint"; vec name_loc locally_abstract_univars; core_type typ ]
           | Pvc_coercion { ground; coercion } ->
               L [ A "coercion"; opt core_type ground; core_type coercion ])
         b.pvb_constraint)

and structure_item (i : structure_item) =
  let n h f = node ~loc:i.pstr_loc h f in
  match i.pstr_desc with
  | Pstr_eval (e, attrs) -> node ~attrs:(attributes attrs) ~loc:i.pstr_loc "eval" [ expression e ]
  | Pstr_value (r, vbs) -> n "value" [ rec_flag r; vec value_binding vbs ]
  | Pstr_primitive v -> value_description v "primitive"
  | Pstr_type (r, ds) -> n "type" [ rec_flag r; vec type_declaration ds ]
  | Pstr_typext t -> type_extension t
  | Pstr_exception t -> type_exception t
  | Pstr_module b ->
      node ~attrs:(attributes b.pmb_attributes) ~loc:b.pmb_loc "module"
        [ at b.pmb_name.loc (opt name b.pmb_name.txt); module_expr b.pmb_expr ]
  | Pstr_recmodule bs ->
      n "recmodule"
        [ vec (fun b ->
              node ~attrs:(attributes b.pmb_attributes) ~loc:b.pmb_loc "modbind"
                [ at b.pmb_name.loc (opt name b.pmb_name.txt); module_expr b.pmb_expr ]) bs ]
  | Pstr_modtype d -> modtype_decl "modtype" d
  | Pstr_open o ->
      node ~attrs:(attributes o.popen_attributes) ~loc:o.popen_loc "open"
        [ module_expr o.popen_expr; override_flag o.popen_override ]
  | Pstr_class cs -> n "class" [ vec (class_infos class_expr) cs ]
  | Pstr_class_type cs -> n "class-type" [ vec (class_infos class_type) cs ]
  | Pstr_include i ->
      node ~attrs:(attributes i.pincl_attributes) ~loc:i.pincl_loc "include" [ module_expr i.pincl_mod ]
  | Pstr_attribute a -> n "attribute" [ attribute a ]
  | Pstr_extension (e, attrs) -> node ~attrs:(attributes attrs) ~loc:i.pstr_loc "extension" [ extension e ]

let directive (d : toplevel_directive) =
  node ~loc:d.pdir_loc "directive"
    (name_loc d.pdir_name
     :: opt_tail
          (fun a ->
            match a.pdira_desc with
            | Pdir_string s -> L [ A "string"; bytes_or_string s ]
            | Pdir_int (t, sfx) ->
                L (A "int" :: Str t :: opt_tail (fun ch -> int (Char.code ch)) sfx)
            | Pdir_ident l -> L [ A "ident"; lid_form l ]
            | Pdir_bool b -> L [ A "bool"; bool b ])
          d.pdir_arg)

let phrase = function
  | Ptop_def s -> L [ A "def"; structure s ]
  | Ptop_dir d -> directive d

(* ---- Typedtree annotations and Lambda (OC4 (a)) ---- *)

let type_string env ty =
  Format.asprintf "%a" (fun ppf t -> Printtyp.wrap_printing_env ~error:false env (fun () -> Printtyp.type_scheme ppf t)) ty

let typed_annotations (impl : Typedtree.implementation) =
  let acc = ref [] in
  let open Tast_iterator in
  let add head (loc : Location.t) env ty =
    if not loc.loc_ghost then
      acc := node ~loc head [ Str (type_string env ty) ] :: !acc
  in
  let it =
    { default_iterator with
      expr = (fun self e ->
        add "expr" e.exp_loc e.exp_env e.exp_type;
        default_iterator.expr self e);
      pat = (fun (type k) self (p : k Typedtree.general_pattern) ->
        add "pat" p.pat_loc p.pat_env p.pat_type;
        default_iterator.pat self p) }
  in
  it.structure it impl.structure;
  List.rev !acc

let signature_text (impl : Typedtree.implementation) =
  Format.asprintf "%a" Printtyp.signature impl.signature

let ident id = Str (Ident.unique_name id)
let prim_text p = Format.asprintf "%a" Printlambda.primitive p
let vk_text vk = Format.asprintf "%a" Printlambda.value_kind vk
let sloc l = Debuginfo.Scoped_location.to_location l

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
  | Lapply a -> node ~loc:(sloc a.ap_loc) "apply" [ lam a.ap_func; vec lam a.ap_args ]
  | Lfunction f ->
      node ~loc:(sloc f.loc) "function"
        [ (match f.kind with Curried -> kw "curried" | Tupled -> kw "tupled");
          vec (fun (id, vk) -> V [ ident id; Str (vk_text vk) ]) f.params;
          Str (vk_text f.return); lam f.body ]
  | Llet (k, vk, id, d, b) ->
      L [ A "let"; kw (match k with Strict -> "strict" | Alias -> "alias" | StrictOpt -> "strictopt");
          Str (vk_text vk); ident id; lam d; lam b ]
  | Lmutlet (vk, id, d, b) -> L [ A "mutlet"; Str (vk_text vk); ident id; lam d; lam b ]
  | Lletrec (bs, b) ->
      L [ A "letrec"; vec (fun (r : Lambda.rec_binding) -> V [ ident r.id; lam (Lfunction r.def) ]) bs; lam b ]
  | Lprim (p, args, loc) -> node ~loc:(sloc loc) "prim" [ Str (prim_text p); vec lam args ]
  | Lswitch (e, sw, loc) ->
      node ~loc:(sloc loc) "switch"
        [ lam e; int sw.sw_numconsts;
          vec (fun (i, x) -> V [ int i; lam x ]) sw.sw_consts;
          int sw.sw_numblocks;
          vec (fun (i, x) -> V [ int i; lam x ]) sw.sw_blocks;
          opt lam sw.sw_failaction ]
  | Lstringswitch (e, cs, d, loc) ->
      node ~loc:(sloc loc) "stringswitch"
        [ lam e; vec (fun (s, x) -> V [ bytes_or_string s; lam x ]) cs; opt lam d ]
  | Lstaticraise (n, args) -> L [ A "staticraise"; int n; vec lam args ]
  | Lstaticcatch (b, (n, vars), h) ->
      L [ A "staticcatch"; lam b; V [ int n; vec (fun (id, vk) -> V [ ident id; Str (vk_text vk) ]) vars ]; lam h ]
  | Ltrywith (b, id, h) -> L [ A "trywith"; lam b; ident id; lam h ]
  | Lifthenelse (c, t, f) -> L [ A "ifthenelse"; lam c; lam t; lam f ]
  | Lsequence (a, b) -> L [ A "sequence"; lam a; lam b ]
  | Lwhile (c, b) -> L [ A "while"; lam c; lam b ]
  | Lfor (id, a, b, d, body) -> L [ A "for"; ident id; lam a; lam b; dir_flag d; lam body ]
  | Lassign (id, e) -> L [ A "assign"; ident id; lam e ]
  | Lsend (k, m, o, args, loc) ->
      node ~loc:(sloc loc) "send"
        [ kw (match k with Self -> "self" | Public -> "public" | Cached -> "cached");
          lam m; lam o; vec lam args ]
  | Levent (e, ev) -> node ~loc:(sloc ev.lev_loc) "event" [ lam e ]
  | Lifused (id, e) -> L [ A "ifused"; ident id; lam e ]

let lambda_program (p : Lambda.program) =
  L [ A "lambda-program"; ident p.module_ident; int p.main_module_block_size;
      vec (fun id -> Str (Ident.name id)) (Ident.Set.elements p.required_globals);
      lam p.code ]

(* ---- errors ---- *)

let error_form exn =
  match Location.error_of_exn exn with
  | Some (`Ok (err : Location.error)) ->
      let kind =
        match exn with
        | Syntaxerr.Error _ -> "syntax"
        | Lexer.Error _ -> "lexer"
        | _ -> "other"
      in
      let msg = Format.asprintf "%t" (fun ppf -> Format_doc.Doc.format ppf err.main.txt) in
      let msg = String.trim msg in
      let m = meta (Some err.main.loc) in
      L ([ A "ocaml/error"; kw kind; Str msg ] @ (if m = [] then [] else [ M m ]))
  | _ -> L [ A "ocaml/error"; kw "exception"; Str (Printexc.to_string exn) ]

(* ---- the tokens (SPEC §9; O3: the layout of mode :full) ---- *)

(* The whole text as alternating gaps and items: gap0 item1 gap2 ... itemN gapN, a gap the text between two items
   (white space; line directives), an item a token's text, [(comment "text")] or [(doc "text")] (a docstring).
   Every item's text is the source between its offsets, so the concatenation is the file byte for byte. *)
let tokens_form src lexbuf =
  Lexer.init ();
  let items = ref [] in
  let prev = ref 0 in
  let n = String.length src in
  let rec go () =
    let t = Lexer.token_with_comments lexbuf in
    if t = Parser.EOF then ()
    else if t = Parser.EOL then go ()
    else begin
      let s, e, kind =
        match t with
        | Parser.COMMENT (_, loc) -> (loc.Location.loc_start.Lexing.pos_cnum, loc.Location.loc_end.Lexing.pos_cnum, `Comment)
        | Parser.DOCSTRING d ->
            let loc = Docstrings.docstring_loc d in
            (loc.Location.loc_start.Lexing.pos_cnum, loc.Location.loc_end.Lexing.pos_cnum, `Doc)
        | _ -> (lexbuf.Lexing.lex_start_p.Lexing.pos_cnum, lexbuf.Lexing.lex_curr_p.Lexing.pos_cnum, `Tok)
      in
      if s < !prev || e < s || e > n then failwith (Printf.sprintf "token offsets %d-%d after %d" s e !prev);
      let gap = String.sub src !prev (s - !prev) and text = String.sub src s (e - s) in
      items :=
        (match kind with
         | `Tok -> Str text
         | `Comment -> L [ A "comment"; Str text ]
         | `Doc -> L [ A "doc"; Str text ])
        :: Str gap :: !items;
      prev := e;
      go ()
    end
  in
  go ();
  items := Str (String.sub src !prev (n - !prev)) :: !items;
  V (List.rev !items)

(* ---- main ---- *)

let read_all ic =
  let b = Buffer.create 4096 in
  (try
     while true do Buffer.add_channel b ic 1 done
   with End_of_file -> ());
  Buffer.contents b

let () =
  let intf = ref false and use_file = ref false and mode = ref "parse" in
  let file = ref None and fname = ref None and width = ref 100 in
  let includes = ref [] and simplify = ref false and prefix_opt = ref None in
  Arg.parse
    [ ("--loc", Arg.Set with_loc, " positions: :loc metadata and (at ...) names");
      ("--intf", Arg.Set intf, " read an interface (.mli)");
      ("--use-file", Arg.Set use_file, " read toplevel phrases (#use files)");
      ("--mode", Arg.Set_string mode, "M parse | types | lambda | tokens");
      ("--file", Arg.String (fun s -> fname := Some s), "NAME the file name to record");
      ("--width", Arg.Set_int width, "N pretty-printing width");
      ("-I", Arg.String (fun d -> includes := d :: !includes), "DIR where the compiled interfaces of other units are (types, lambda; as ocamlc -I)");
      ("--prefix", Arg.String (fun d -> prefix_opt := Some d), "P types, lambda: type the file in place (FILE, its .mli beside it found) with output prefix P, where ocamlc -o P.cmo put the unit's .cmi (a weak type is checked against it)");
      ("--simplify", Arg.Set simplify, " lambda: after Simplif.simplify_lambda, what ocamlc -dlambda prints");
      (* the compiler's flags the stdlib's own build passes (stdlib/Makefile, Compflags; O5, OC51 (c)) *)
      ("-nostdlib", Arg.Set Clflags.no_std_include, " types, lambda: as ocamlc -nostdlib");
      ("-nopervasives", Arg.Set Clflags.nopervasives, " types, lambda: as ocamlc -nopervasives");
      ("-no-alias-deps", Arg.Set Clflags.no_alias_deps, " types, lambda: as ocamlc -no-alias-deps");
      ("-nolabels", Arg.Set Clflags.classic, " types, lambda: as ocamlc -nolabels");
      ("-principal", Arg.Set Clflags.principal, " types, lambda: as ocamlc -principal");
      ("-no-principal", Arg.Clear Clflags.principal, " types, lambda: as ocamlc -no-principal");
      ("-strict-sequence", Arg.Set Clflags.strict_sequence, " types, lambda: as ocamlc -strict-sequence");
      ("-open", Arg.String (fun m -> Clflags.open_modules := m :: !Clflags.open_modules), "M types, lambda: as ocamlc -open M");
      (* the flags of the testsuite's run programs (bin/oi's corpus, O5) *)
      ("-unsafe", Arg.Set Clflags.unsafe, " types, lambda: as ocamlc -unsafe");
      ("-rectypes", Arg.Set Clflags.recursive_types, " types, lambda: as ocamlc -rectypes");
      ("-no-strict-formats", Arg.Clear Clflags.strict_formats, " types, lambda: as ocamlc -no-strict-formats");
      ("-strict-formats", Arg.Set Clflags.strict_formats, " types, lambda: as ocamlc -strict-formats");
      ("-g", Arg.Set Clflags.debug, " types, lambda: as ocamlc -g") ]
    (fun f -> file := Some f)
    "ocdump [options] [FILE]";
  let src, path =
    match !file with
    | Some f -> let ic = open_in_bin f in let s = read_all ic in close_in ic; (s, f)
    | None -> (read_all stdin, "stdin.ml")
  in
  let name = match !fname with Some n -> n | None -> path in
  ignore (Warnings.parse_options false "-a");
  let lexbuf = Lexing.from_string src in
  Location.init lexbuf name;
  let top_meta = M ([ ("file", Str name); ("ocaml", Str Sys.ocaml_version) ]) in
  let out =
    try
      match !mode with
      | "tokens" ->
          if not (valid_utf8 src) then
            L [ A "ocaml/error"; kw "encoding"; Str "the file is not valid UTF-8" ]
          else L [ A "ocaml/layout"; tokens_form src lexbuf; top_meta ]
      | "parse" ->
          if !use_file then
            L [ A "ocaml/toplevel"; vec phrase (Parse.use_file lexbuf); top_meta ]
          else if !intf then L [ A "ocaml/intf"; signature (Parse.interface lexbuf); top_meta ]
          else L [ A "ocaml/impl"; structure (Parse.implementation lexbuf); top_meta ]
      | "types" | "lambda" ->
          (* the compiler's own pipeline for one file (Compile_common), no output file *)
          Clflags.native_code := false;
          if !mode = "types" then with_loc := true;
          Clflags.dont_write_files := true;
          Clflags.include_dirs := List.rev !includes;
          ignore (Warnings.parse_options false "-a");
          let tmpdir = Filename.temp_dir "ocdump" "" in
          let base = Filename.basename name in
          let tmp = Filename.concat tmpdir (if Filename.check_suffix name ".ml" || Filename.check_suffix name ".mli" then base else if !intf then "stdin.mli" else "stdin.ml") in
          let in_place = !prefix_opt <> None && !file <> None in
          if not in_place then (let oc = open_out_bin tmp in output_string oc src; close_out oc);
          let prefix = match !prefix_opt with Some p -> p | None -> Filename.remove_extension tmp in
          let kind = if Filename.check_suffix tmp ".mli" then Unit_info.Intf else Unit_info.Impl in
          let source_file = if in_place then (match !file with Some f -> f | None -> tmp) else tmp in
          let ui = Unit_info.make ~source_file kind prefix in
          let result =
            Compile_common.with_info ~native:false ~tool_name:"ocdump" ~dump_ext:"" ui (fun info ->
                if kind = Unit_info.Intf then begin
                  if !mode = "lambda" then failwith "lambda of an interface";
                  let ast = Compile_common.parse_intf info in
                  let (_, sg) = Compile_common.typecheck_intf info ast in
                  L [ A "ocaml/typed"; Str (Format.asprintf "%a" Printtyp.signature sg.Typedtree.sig_type); V []; top_meta ]
                end else begin
                let ast = Compile_common.parse_impl info in
                let impl = Compile_common.typecheck_impl info ast in
                if !mode = "types" then
                  L [ A "ocaml/typed"; Str (signature_text impl); vec (fun x -> x) (typed_annotations impl); top_meta ]
                else
                  let prog =
                    Translmod.transl_implementation (Unit_info.modname info.target)
                      (impl.structure, impl.coercion)
                  in
                  let prog = if !simplify then { prog with Lambda.code = Simplif.simplify_lambda prog.Lambda.code } else prog in
                  L [ A "ocaml/lambda"; lambda_program prog; top_meta ]
                end)
          in
          (try Sys.remove tmp with _ -> ()); (try Sys.rmdir tmpdir with _ -> ());
          result
      | m -> prerr_endline ("ocdump: unknown mode " ^ m); exit 2
    with exn -> error_form exn
  in
  let b = Buffer.create 4096 in
  pretty b 0 !width out;
  Buffer.add_char b '\n';
  print_string (Buffer.contents b)
