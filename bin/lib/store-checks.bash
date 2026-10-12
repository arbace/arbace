# Sourced by bin/arbace-go and bin/box-smoke: the store's checks (doc/go/BOX-NOTES.md, S3).

# store_checks RUN T: the store (doc/go/BOX-NOTES.md, S3) through arbace.box, for the program the
# function RUN runs (the executable, or the box's launcher), its directory T/store (ARBACE_BOX_STORE):
# no store, blobs and refs, compare-and-set, the refusals (names, hashes, blobs, sizes), a
# script by ref (ref:NAME, -i ref:NAME), namespaces from ARBACE_PATH's store entries. Uses the
# caller's check
store_checks() {
  local run=$1 s=$2/store h st
  unset ARBACE_BOX_STORE ARBACE_BOX_STORE_MAX ARBACE_BOX_STORE_CACHE ARBACE_BOX_FS
  rm -rf "$s"
  local e='(do (require (quote [arbace.box :as box])) (defn e [f] (try (f) :ok (catch Exception x (or (:arbace.box/error (ex-data x)) (.getName (class x)))))) nil)'
  check "the store: none configured, arbace.box refuses" "[false :no-store :no-store]" \
    "$($run -e "$e" -e '(prn [(box/store?) (e #(box/put "x")) (e #(box/ref "x"))])' 2>&1)"
  h=$(printf hello | sha256sum | cut -c1-64)
  check "the store: put, get, size, ref!, ref" "[\"$h\" \"hello\" 5 true \"$h\" nil nil true]" \
    "$(ARBACE_BOX_STORE=$s $run -e "$e" -e '(let [h (box/put "hello")] (prn [h (box/get h) (box/size h) (box/ref! "greeting" h) (box/ref "greeting") (box/ref "none") (box/get (apply str (repeat 64 "0"))) (box/store?)]))' 2>&1)"
  check "the store: a ref's compare-and-set" "[true false true false \"b\"]" \
    "$(ARBACE_BOX_STORE=$s $run -e "$e" -e '(let [a (box/put "a") b (box/put "b")] (prn [(box/ref! "c" a nil) (box/ref! "c" b nil) (box/ref! "c" b a) (box/ref! "c" a a) (box/load "c")]))' 2>&1)"
  check "the store refuses names, hashes, refs to no blob" "[:bad-name :bad-name :bad-name :ok :bad-hash :no-blob :no-ref]" \
    "$(ARBACE_BOX_STORE=$s $run -e "$e" -e '(let [h (box/put "x")] (prn [(e #(box/ref ".x")) (e #(box/ref! "a/b" h)) (e #(box/ref (apply str (repeat 65 "a")))) (e #(box/ref (apply str (repeat 64 "a")))) (e #(box/get "ABC")) (e #(box/ref! "r" (apply str (repeat 64 "0")))) (e #(box/load-ref "none"))]))' 2>&1)"
  check "the store refuses a blob beyond ARBACE_BOX_STORE_MAX" "[:ok :too-large]" \
    "$(ARBACE_BOX_STORE=$s ARBACE_BOX_STORE_MAX=16 $run -e "$e" -e '(prn [(e #(box/put (apply str (repeat 16 "x")))) (e #(box/put (apply str (repeat 17 "x"))))])' 2>&1)"
  ARBACE_BOX_STORE=$s $run -e "$e" -e '(box/put! "hello.ae" "(prn [:from-ref *command-line-args*])")' > /dev/null 2>&1
  check "a script run from a ref (ref:NAME)" '[:from-ref ("a" "b")]' "$(ARBACE_BOX_STORE=$s $run ref:hello.ae a b 2>&1)"
  check "an -i file from a ref" "$(printf '[:from-ref nil]\n:after')" "$(ARBACE_BOX_STORE=$s $run -i ref:hello.ae -e '(prn :after)' 2>&1)"
  ARBACE_BOX_STORE=$s $run ref:nope > "$s.err" 2>&1; st=$?
  check "a script from a ref that is not: status 1, the reason" "1 no ref nope" "$st $(grep -o 'no ref nope' "$s.err" | head -1)"
  ARBACE_BOX_STORE=$s $run -e "$e" -e '(box/put! "st.lib.ae" "(ns st.lib) (defn f [] :lib)") (box/put! "p.st.two.ae" "(ns st.two (:require [st.lib])) (defn g [] [:two (st.lib/f)])")' > /dev/null 2>&1
  check "namespaces loaded from the store (ARBACE_PATH store:p.:store:)" "[:two :lib]" \
    "$(ARBACE_BOX_STORE=$s ARBACE_PATH=/none:store:p.:store: $run -e '(require (quote st.two)) (prn ((resolve (quote st.two/g))))' 2>&1)"
}
