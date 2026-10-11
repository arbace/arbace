;; arbace.lang.Murmur3 and the hashes built on it (Util.hasheq of the value kinds).

(each [i [0 1 -1 42 2147483647 -2147483648 (int 1)]]
  (Murmur3/hashInt i))
(each [l [0 1 -1 42 4294967296 9223372036854775807 -9223372036854775808 -4294967296]]
  (Murmur3/hashLong l))
(each [s ["" "a" "ab" "abc" "abcd" "abcde" "hello world" "é" "😀" "a😀b" "\ud800" "\udfff\ud800" "Aa" "BB" "\u0000"]]
  (Murmur3/hashUnencodedChars s)
  (Util/hasheq s)
  (Util/hash s))
(def sb (new StringBuilder "abc"))
(Murmur3/hashUnencodedChars sb)
(each [h [0 1 -1 123456789]
       n [0 1 2 1000]]
  (Murmur3/mixCollHash h n))

;; ordered and unordered collection hashes match the persistent collections'
(def v ^{:sig ["Object[]"]} (PersistentVector/create (array Object 1 2 3)))
(Murmur3/hashOrdered v)
(.hasheq v)
(def l (PersistentList/create v))
(Murmur3/hashOrdered l)
(.hasheq l)
(def s ^{:sig ["Object[]"]} (PersistentHashSet/create (array Object 1 2 3)))
(Murmur3/hashUnordered s)
(.hasheq s)
(def jl (new java.util.ArrayList))
(.add jl 1)
(.add jl 2)
(.add jl 3)
(Murmur3/hashOrdered jl)
(Murmur3/hashUnordered jl)
(Util/hasheq jl)
(def e PersistentVector/EMPTY)
(Murmur3/hashOrdered e)
(Murmur3/hashUnordered e)
(def m (RT/map (array Object 1 2)))
(Murmur3/hashUnordered m)
(.hasheq m)
(def me (new MapEntry 1 2))
(.hasheq me)
(Murmur3/hashOrdered me)

;; Util.hasheq and Util.hash of the value kinds
(def ka (Keyword/intern "a"))
(def kns (Keyword/intern "n" "a"))
(def sa (Symbol/intern "a"))
(def sns (Symbol/intern "n" "a"))
(def r (new Ratio (biginteger "1") (biginteger "3")))
(def bn (BigInt/fromLong 5))
(def bnx (BigInt/fromBigInteger (biginteger "18446744073709551616")))
(each [x [nil true false 0 1 -1 (int 1) (short 1) (byte 1) 1.0 -0.0 0.0 1.5 ##NaN ##Inf (float 1.5) (float -0.0) \a (char 233) (char 55357) ka kns sa sns r bn bnx (biginteger "5") (biginteger "18446744073709551616") (bigdecimal "1.50") (bigdecimal "1.5") (bigdecimal "0") v l s m me jl]]
  (Util/hasheq x)
  (Util/hash x))
(Util/hashCombine 1 2)
(Util/hashCombine -1 0)
(Util/hashCombine 0 0)
