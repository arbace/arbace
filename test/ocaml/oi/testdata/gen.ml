(* generates golden.txt: hashes and marshalled strings of values the Go tests rebuild *)
type r = { a : int; b : string; c : (int * int) list }
type fr = { x : float; y : float }
type t = A | B of int | C of int * string
let p name s = Printf.printf "%s\t%s\n" name s
let h name v = p name (string_of_int (Hashtbl.hash v))
let m name ?(f=[]) v = p name (String.escaped (Marshal.to_string v f) |> fun s -> s)
let hx name s = p name (String.concat "" (List.init (String.length s) (fun i -> Printf.sprintf "%02x" (Char.code s.[i]))))
let mx name ?(f=[]) v = hx name (Marshal.to_string v f)
let () =
  h "hash_0" 0; h "hash_1" 1; h "hash_neg1" (-1); h "hash_neg5" (-5); h "hash_big" 1234567890123;
  h "hash_min" min_int; h "hash_max" max_int;
  h "hash_str" "hello"; h "hash_empty" ""; h "hash_str7" "abcdefg"; h "hash_str4" "abcd";
  h "hash_f0" 0.0; h "hash_fm0" (-0.0); h "hash_nan" nan; h "hash_f15" 1.5;
  h "hash_tuple" (1, "a", 2.5); h "hash_list" [1;2;3]; h "hash_list20" (List.init 20 (fun i -> i));
  h "hash_rec" { a = 3; b = "xy"; c = [(1,2);(3,4)] };
  h "hash_frec" { x = 1.0; y = 2.0 };
  h "hash_farr" [|1.0; 2.0; 3.0|];
  h "hash_i64" 42L; h "hash_i64neg" (-42L); h "hash_i64big" 0x123456789abcdefL;
  h "hash_i32" 42l; h "hash_i32neg" (-7l); h "hash_ni" 42n; h "hash_nibig" 0x123456789abn;
  h "hash_variants" [A; B 3; C (4, "z")];
  h "hash_some" (Some 3); h "hash_none" None; h "hash_unit" ();
  h "hash_true" true; h "hash_char" 'a';
  h "hash_nested" [[1;2];[3];[]];
  h "hash_deep" (List.init 100 (fun i -> (i, string_of_int i)));
  p "seeded_x" (string_of_int (Hashtbl.seeded_hash 17 "x"));
  p "seeded_int" (string_of_int (Hashtbl.seeded_hash 99 12345));
  p "param_10_100_list" (string_of_int (Hashtbl.hash_param 10 100 (List.init 30 (fun i -> i))));
  p "param_3_5_tuple" (string_of_int (Hashtbl.hash_param 3 5 (1,(2,3),(4,(5,6)),7)));
  p "param_0" (string_of_int (Hashtbl.hash_param 0 100 5));
  p "param_1_1" (string_of_int (Hashtbl.hash_param 1 1 [1;2]));
  p "param_5_0" (string_of_int (Hashtbl.hash_param 5 0 [1;2]));
  p "param_100_300" (string_of_int (Hashtbl.hash_param 100 300 (List.init 400 (fun i -> i))));
  p "strhash" (string_of_int (Hashtbl.seeded_hash 0 "hello world, long string here!"));
  (* marshal *)
  mx "m_0" 0; mx "m_63" 63; mx "m_64" 64; mx "m_neg1" (-1); mx "m_127" 127; mx "m_neg128" (-128);
  mx "m_128" 128; mx "m_32767" 32767; mx "m_32768" 32768; mx "m_neg32769" (-32769);
  mx "m_2p30m1" (1 lsl 30 - 1); mx "m_2p30" (1 lsl 30); mx "m_min" min_int; mx "m_max" max_int;
  mx "m_neg2p30" (-(1 lsl 30)); mx "m_neg2p30m1" (-(1 lsl 30) - 1);
  mx "m_str" "hello"; mx "m_str31" (String.make 31 'a'); mx "m_str32" (String.make 32 'b');
  mx "m_str255" (String.make 255 'c'); mx "m_str256" (String.make 256 'd'); mx "m_empty" "";
  mx "m_f" 1.5; mx "m_fnan" nan; mx "m_fm0" (-0.0);
  mx "m_farr" [|1.0; 2.5|]; mx "m_farr300" (Array.init 300 float_of_int);
  mx "m_empty_arr" [||];
  mx "m_tuple" (1, "a", 2.5); mx "m_list" [1;2;3]; mx "m_list20" (List.init 20 (fun i -> i));
  mx "m_rec" { a = 3; b = "xy"; c = [(1,2);(3,4)] };
  mx "m_frec" { x = 1.0; y = 2.0 };
  mx "m_i64" 42L; mx "m_i64neg" (-42L); mx "m_i32" 42l; mx "m_ni" 42n; mx "m_nibig" 0x123456789abn;
  mx "m_ni_neg" (-5n);
  mx "m_variants" [A; B 3; C (4, "z")];
  mx "m_some" (Some 3); mx "m_none" None;
  mx "m_big_block" (Array.make 10 7); mx "m_block16" (Array.init 8 (fun i -> i));
  let s = "shared" in
  mx "m_shared" (s, s, s);
  mx "m_noshare" ~f:[Marshal.No_sharing] (s, s, s);
  let l = [1;2] in
  mx "m_shared_list" (l, l, [l]);
  let big = Array.init 300 (fun i -> string_of_int i) in
  mx "m_shared_big" (big, big.(0), big.(299), big.(0));
  mx "m_compat32" ~f:[Marshal.Compat_32] (1, "x");
  mx "m_deep_nested" (List.init 5 (fun i -> (i, [i; i+1])));
  let ar = [| "a"; "b" |] in
  mx "m_arr_shared" [| ar; ar |];
  mx "m_cyc" (let rec x = 1 :: 2 :: x in x);
  mx "m_exn_ok" (Some (Failure "boom"));
  mx "m_lazy_forced" (let z = lazy (1+1) in ignore (Lazy.force z); z);
  hx "m_size" (Marshal.to_string (List.init 1000 (fun i -> i)) []);
  (try ignore (Marshal.to_string (fun x -> x) []) with e -> p "err_fun" (Printexc.to_string e));
  (try ignore (Marshal.to_string (Marshal.to_string 1 [], Lazy.from_fun (fun () -> 1)) []); p "lazy_ok" "ok" with e -> p "err_lazy" (Printexc.to_string e));
  (try ignore (Marshal.to_string (1 lsl 40) [Marshal.Compat_32]) with e -> p "err_c32" (Printexc.to_string e));
  (try ignore (Marshal.from_string "garbage garbage garbage garbage" 0) with e -> p "err_bad" (Printexc.to_string e));
  (try ignore (Marshal.from_string (String.sub (Marshal.to_string "abcdef" []) 0 25) 0) with e -> p "err_trunc" (Printexc.to_string e));
  (let b = Bytes.create 30 in
   (try ignore (Marshal.to_buffer b 0 25 "abcdefghijkl" []) with e -> p "err_buf" (Printexc.to_string e));
   p "buf_ok" (string_of_int (Marshal.to_buffer b 0 30 "abcde" [])))
