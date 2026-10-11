;; /**
;;  *   Copyright (c) Rich Hickey. All rights reserved.
;;  *   The use and distribution terms for this software are covered by the
;;  *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  *   which can be found in the file epl-v10.html at the root of this distribution.
;;  *   By using this software in any fashion, you are agreeing to be bound by
;;  *     the terms of this license.
;;  *   You must not remove this notice, or any other, from this software.
;;  **/
;;
;; Converted from clojure/lang/EdnReader.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException PushbackReader Reader)
        '(java.util ArrayList List)
        '(java.util.regex Matcher Pattern))

(defclass ^:public EdnReader
  (field ^:static ^IFn/1 macros (new IFn/1 256))

  (field ^:static ^IFn/1 dispatchMacros (new IFn/1 256))

  (field ^:static ^Pattern symbolPat (Pattern/compile "[:]?([\\D&&[^/]].*/)?(/|[\\D&&[^/]][^/]*)"))

  (field ^:static ^Pattern intPat
    (Pattern/compile
      "([-+]?)(?:(0)|([1-9][0-9]*)|0[xX]([0-9A-Fa-f]+)|0([0-7]+)|([1-9][0-9]?)[rR]([0-9A-Za-z]+)|0[0-9]+)(N)?"))

  (field ^:static ^Pattern ratioPat (Pattern/compile "([-+]?[0-9]+)/([0-9]+)"))

  (field ^:static ^Pattern floatPat
    (Pattern/compile "([-+]?[0-9]+(\\.[0-9]*)?([eE][-+]?[0-9]+)?)(M)?"))

  (field ^:static ^IFn taggedReader (TaggedReader.))

  (static-initializer
    (aset macros \" (StringReader.))
    (aset macros \; (CommentReader.))
    (aset macros \^ (MetaReader.))
    (aset macros \( (ListReader.))
    (aset macros \) (UnmatchedDelimiterReader.))
    (aset macros \[ (VectorReader.))
    (aset macros \] (UnmatchedDelimiterReader.))
    (aset macros \{ (MapReader.))
    (aset macros \} (UnmatchedDelimiterReader.))
    (aset macros \\ (CharacterReader.))
    (aset macros \# (DispatchReader.))
    (aset dispatchMacros \# (SymbolicValueReader.))
    (aset dispatchMacros \^ (MetaReader.))
    (aset dispatchMacros \{ (SetReader.))
    (aset dispatchMacros \< (UnreadableReader.))
    (aset dispatchMacros \_ (DiscardReader.))
    (aset dispatchMacros \: (NamespaceMapReader.)))

  (method ^:static nonConstituent ^boolean [^int ch]
    (or (or (== ch \@) (== ch \`)) (== ch \~)))

  (method ^:public ^:static readString [^String s ^IPersistentMap opts]
    (let [r (PushbackReader. (java.io.StringReader. s))] (EdnReader/read r opts)))

  (method ^:static isWhitespace ^boolean [^int ch]
    (or (Character/isWhitespace ch) (== ch \,)))

  (method ^:static unread ^void [^PushbackReader r ^int ch]
    (when-not (== ch -1) (try (.unread r ch) (catch IOException e (throw (Util/sneakyThrow e))))))

  (defclass ^:public ^:static ReaderException
    :extends RuntimeException

    (field ^:final ^int line)

    (field ^:final ^int column)

    (constructor ^:public [this ^int line ^int column ^Throwable cause]
      (super. cause)
      (set! (.-line this) line)
      (set! (.-column this) column)))

  (method ^:public ^:static read1 ^int [^Reader r]
    (try (.read r) (catch IOException e (throw (Util/sneakyThrow e)))))

  (field ^:static ^:final ^Keyword EOF (Keyword/intern nil "eof"))

  (method ^:public ^:static read [^PushbackReader r ^IPersistentMap opts]
    (EdnReader/read r (not (.containsKey opts EOF)) (.valAt opts EOF) false opts))

  (method ^:public ^:static read [^PushbackReader r ^boolean eofIsError eofValue
                                  ^boolean isRecursive opts]
    (try
      (while true
        (let [^:mutable ch (EdnReader/read1 r)]
          (while (EdnReader/isWhitespace ch) (set! ch (EdnReader/read1 r)))
          (when (== ch -1)
            (when eofIsError (throw (Util/runtimeException "EOF while reading")))
            (return eofValue))
          (when (Character/isDigit ch)
            (let [n (EdnReader/readNumber r (unchecked-char ch))]
              (when (RT/suppressRead) (return nil))
              (return n)))
          (let [macroFn (EdnReader/getMacro ch)]
            (when (some? macroFn)
              (let [ret (.invoke macroFn r (unchecked-char ch) opts)]
                (when (RT/suppressRead) (return nil))
                (when (identical? ret r) (continue))
                (return ret)))
            (when (or (== ch \+) (== ch \-))
              (let [ch2 (EdnReader/read1 r)]
                (when (Character/isDigit ch2)
                  (EdnReader/unread r ch2)
                  (let [n (EdnReader/readNumber r (unchecked-char ch))]
                    (when (RT/suppressRead) (return nil))
                    (return n)))
                (EdnReader/unread r ch2)))
            (let [token (EdnReader/readToken r (unchecked-char ch) true)]
              (when (RT/suppressRead) (return nil))
              (return (EdnReader/interpretToken token))))))
      (catch Exception e
        (when (or isRecursive (not (instance? LineNumberingPushbackReader r)))
          (throw (Util/sneakyThrow e)))
        (let [rdr (cast LineNumberingPushbackReader r)]
          (throw (ReaderException. (.getLineNumber rdr) (.getColumnNumber rdr) e))))))

  (method ^:private ^:static readToken ^String [^PushbackReader r ^char initch
                                                ^boolean leadConstituent]
    (let [sb (StringBuilder.)]
      (when (and leadConstituent (EdnReader/nonConstituent initch))
        (throw (Util/runtimeException (java-str "Invalid leading character: " initch))))
      (^[char] StringBuilder/.append sb initch)
      (while true
        (let [ch (EdnReader/read1 r)]
          (cond
            (or (or (== ch -1) (EdnReader/isWhitespace ch)) (EdnReader/isTerminatingMacro ch))
              (do (EdnReader/unread r ch) (return (.toString sb)))
            (EdnReader/nonConstituent ch)
              (throw (Util/runtimeException
                       (java-str "Invalid constituent character: " (unchecked-char ch)))))
          (^[char] StringBuilder/.append sb (unchecked-char ch))))))

  (method ^:private ^:static readNumber [^PushbackReader r ^char initch]
    (let [sb (StringBuilder.)]
      (^[char] StringBuilder/.append sb initch)
      (while true
        (let [ch (EdnReader/read1 r)]
          (when (or (or (== ch -1) (EdnReader/isWhitespace ch)) (EdnReader/isMacro ch))
            (EdnReader/unread r ch)
            (break))
          (^[char] StringBuilder/.append sb (unchecked-char ch))))
      (let [s (.toString sb)
            n (EdnReader/matchNumber s)]
        (when (nil? n) (throw (NumberFormatException. (java-str "Invalid number: " s))))
        n)))

  (method ^:private ^:static readUnicodeChar ^int [^String token ^int offset ^int length ^int base]
    (when-not (== (.length token) (unchecked-add-int offset length))
      (throw (IllegalArgumentException. (java-str "Invalid unicode character: \\" token))))
    (let [^:mutable ^int uc 0]
      (loop [^int i offset]
        (when (< i (unchecked-add-int offset length))
          (let [d (Character/digit (.charAt token i) base)]
            (when (== d -1)
              (throw (IllegalArgumentException. (java-str "Invalid digit: " (.charAt token i)))))
            (set! uc (unchecked-add-int (unchecked-multiply-int uc base) d))
            (recur (unchecked-inc-int i)))))
      (unchecked-char uc)))

  (method ^:private ^:static readUnicodeChar ^int [^PushbackReader r ^int initch ^int base
                                                   ^int length ^boolean exact]
    (let [^:mutable uc (Character/digit initch base)]
      (when (== uc -1)
        (throw (IllegalArgumentException. (java-str "Invalid digit: " (unchecked-char initch)))))
      (let [^:mutable ^int i 1]
        (while (< i length)
          (let [ch (EdnReader/read1 r)]
            (when (or (or (== ch -1) (EdnReader/isWhitespace ch)) (EdnReader/isMacro ch))
              (EdnReader/unread r ch)
              (break))
            (let [d (Character/digit ch base)]
              (when (== d -1)
                (throw (IllegalArgumentException. (java-str "Invalid digit: " (unchecked-char ch)))))
              (set! uc (unchecked-add-int (unchecked-multiply-int uc base) d))))
          (set! i (unchecked-inc-int i)))
        (when (and (not (== i length)) exact)
          (throw (IllegalArgumentException.
                   (java-str "Invalid character length: " i ", should be: " length))))
        uc)))

  (method ^:private ^:static interpretToken [^String s]
    (when-not (.equals s "nil")
      (cond
        (.equals s "true") RT/T
        (.equals s "false") RT/F
        :else
          (let [^:mutable ^Object ret nil]
            (set! ret (EdnReader/matchSymbol s))
            (if (some? ret) ret (throw (Util/runtimeException (java-str "Invalid token: " s))))))))

  (method ^:private ^:static matchSymbol [^String s]
    (let [m (.matcher symbolPat s)]
      (when (.matches m)
        (let [gc (.groupCount m)
              ns (^[int] Matcher/.group m 1)
              name (^[int] Matcher/.group m 2)]
          (when-not (or (or (and (some? ns) (.endsWith ns ":/")) (.endsWith name ":"))
                        (not (== (^[String int] String/.indexOf s "::" 1) -1)))
            (when-not (.startsWith s "::")
              (let [isKeyword (== (.charAt s 0) \:)
                    sym (Symbol/intern (.substring s (if isKeyword 1 0)))]
                (if isKeyword (Keyword/intern sym) sym))))))))

  (method ^:private ^:static matchNumber [^String s]
    (let [^:mutable m (.matcher intPat s)]
      (if (.matches m)
          (if (some? (^[int] Matcher/.group m 2))
              (if (some? (^[int] Matcher/.group m 8)) BigInt/ZERO (^[long] Numbers/num 0))
              (let [negate (.equals (^[int] Matcher/.group m 1) "-")
                    ^:mutable ^String n nil
                    ^:mutable ^int radix 10]
                (cond
                  (some? (set! n (^[int] Matcher/.group m 3))) (set! radix 10)
                  (some? (set! n (^[int] Matcher/.group m 4))) (set! radix 16)
                  (some? (set! n (^[int] Matcher/.group m 5))) (set! radix 8)
                  (some? (set! n (^[int] Matcher/.group m 7)))
                    (set! radix (Integer/parseInt (^[int] Matcher/.group m 6))))
                (when (some? n)
                  (let [^:mutable bn (BigInteger. n radix)]
                    (when negate (set! bn (.negate bn)))
                    (cond
                      (some? (^[int] Matcher/.group m 8)) (BigInt/fromBigInteger bn)
                      (< (.bitLength bn) 64) ^Object (^[long] Numbers/num (.longValue bn))
                      :else ^Object (BigInt/fromBigInteger bn))))))
          (do
            (set! m (.matcher floatPat s))
            (if (.matches m)
                (if (some? (^[int] Matcher/.group m 4))
                    (BigDecimal. (^[int] Matcher/.group m 1))
                    (Double/parseDouble s))
                (do
                  (set! m (.matcher ratioPat s))
                  (when (.matches m)
                    (let [^:mutable numerator (^[int] Matcher/.group m 1)]
                      (when (.startsWith numerator "+") (set! numerator (.substring numerator 1)))
                      (Numbers/divide
                        (Numbers/reduceBigInt (BigInt/fromBigInteger (BigInteger. numerator)))
                        (Numbers/reduceBigInt
                          (BigInt/fromBigInteger (BigInteger. (^[int] Matcher/.group m 2)))))))))))))

  (method ^:private ^:static getMacro ^IFn [^int ch]
    (when (< ch (alength macros)) (aget macros ch)))

  (method ^:private ^:static isMacro ^boolean [^int ch]
    (and (< ch (alength macros)) (some? (aget macros ch))))

  (method ^:private ^:static isTerminatingMacro ^boolean [^int ch]
    (and (and (not (== ch \#)) (not (== ch \'))) (EdnReader/isMacro ch)))

  (defclass ^:public ^:static StringReader
    :extends AFn

    (method ^:public invoke [this reader doublequote opts]
      (let [sb (StringBuilder.)
            r (cast Reader reader)]
        (let [^:mutable ch (EdnReader/read1 r)]
          (while (not (== ch \"))
            (when (== ch -1) (throw (Util/runtimeException "EOF while reading string")))
            (when (== ch \\)
              (set! ch (EdnReader/read1 r))
              (when (== ch -1) (throw (Util/runtimeException "EOF while reading string")))
              (switch ch
                \t (set! ch \tab)
                \r (set! ch \return)
                \n (set! ch \newline)
                \\ nil
                \" nil
                \b (set! ch \backspace)
                \f (set! ch \formfeed)
                \u
                  (do
                    (set! ch (EdnReader/read1 r))
                    (when (== (^[int int] Character/digit ch 16) -1)
                      (throw (Util/runtimeException
                               (java-str "Invalid unicode escape: \\u" (unchecked-char ch)))))
                    (set! ch (EdnReader/readUnicodeChar (cast PushbackReader r) ch 16 4 true)))
                (if (Character/isDigit ch)
                    (do
                      (set! ch (EdnReader/readUnicodeChar (cast PushbackReader r) ch 8 3 false))
                      (when (> ch 0377)
                        (throw (Util/runtimeException
                                 "Octal escape sequence must be in range [0, 377]."))))
                    (throw (Util/runtimeException
                             (java-str "Unsupported escape character: \\" (unchecked-char ch)))))))
            (^[char] StringBuilder/.append sb (unchecked-char ch))
            (set! ch (EdnReader/read1 r))))
        (.toString sb))))

  (defclass ^:public ^:static CommentReader
    :extends AFn

    (method ^:public invoke [this reader semicolon opts]
      (let [r (cast Reader reader)
            ^:mutable ^int ch 0]
        (loop []
          (set! ch (EdnReader/read1 r))
          (when (and (and (not (== ch -1)) (not (== ch \newline))) (not (== ch \return))) (recur)))
        r)))

  (defclass ^:public ^:static DiscardReader
    :extends AFn

    (method ^:public invoke [this reader underscore opts]
      (let [r (cast PushbackReader reader)] (EdnReader/read r true nil true opts) r)))

  (defclass ^:public ^:static NamespaceMapReader
    :extends AFn

    (method ^:public invoke [this reader colon opts]
      (let [r (cast PushbackReader reader)
            sym (EdnReader/read r true nil false opts)]
        (when (or (not (instance? Symbol sym)) (some? (.getNamespace (cast Symbol sym))))
          (throw (RuntimeException.
                   (java-str "Namespaced map must specify a valid namespace: " sym))))
        (let [ns (.getName (cast Symbol sym))
              ^:mutable nextChar (EdnReader/read1 r)]
          (while (EdnReader/isWhitespace nextChar) (set! nextChar (EdnReader/read1 r)))
          (when-not (== \{ nextChar)
            (throw (RuntimeException. "Namespaced map must specify a map")))
          (let [kvs (EdnReader/readDelimitedList \} r true opts)]
            (when (== (bit-and-int (.size kvs) 1) 1)
              (throw (Util/runtimeException
                       "Namespaced map literal must contain an even number of forms")))
            (let [a (new Object/1 (.size kvs))
                  iter (.iterator kvs)]
              (loop [^int i 0]
                (when (.hasNext iter)
                  (let [^:mutable key (.next iter)
                        val (.next iter)]
                    (cond
                      (instance? Keyword key)
                        (let [kw (cast Keyword key)]
                          (cond
                            (nil? (.getNamespace kw)) (set! key (Keyword/intern ns (.getName kw)))
                            (.equals (.getNamespace kw) "_")
                              (set! key (Keyword/intern nil (.getName kw)))))
                      (instance? Symbol key)
                        (let [s (cast Symbol key)]
                          (cond
                            (nil? (.getNamespace s)) (set! key (Symbol/intern ns (.getName s)))
                            (.equals (.getNamespace s) "_")
                              (set! key (Symbol/intern nil (.getName s))))))
                    (aset a i key)
                    (aset a (unchecked-add-int i 1) val)
                    (recur (unchecked-add-int i 2)))))
              (RT/map a)))))))

  (defclass ^:public ^:static DispatchReader
    :extends AFn

    (method ^:public invoke [this reader hash opts]
      (let [ch (EdnReader/read1 (cast Reader reader))]
        (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
        (let [fn (aget dispatchMacros ch)]
          (when (nil? fn)
            (when (Character/isLetter ch)
              (EdnReader/unread (cast PushbackReader reader) ch)
              (return (.invoke taggedReader reader ch opts)))
            (throw (Util/runtimeException
                     (String/format "No dispatch macro for: %c"
                                    (new Object/1 [(unchecked-char ch)])))))
          (.invoke fn reader ch opts)))))

  (defclass ^:public ^:static MetaReader
    :extends AFn

    (method ^:public invoke [this reader caret opts]
      (let [r (cast PushbackReader reader)
            ^:mutable ^int line -1
            ^:mutable ^int column -1]
        (when (instance? LineNumberingPushbackReader r)
          (set! line (.getLineNumber (cast LineNumberingPushbackReader r)))
          (set! column
                (unchecked-subtract-int (.getColumnNumber (cast LineNumberingPushbackReader r)) 1)))
        (let [^:mutable meta (EdnReader/read r true nil true opts)]
          (cond
            (or (instance? Symbol meta) (instance? String meta))
              (set! meta (^[Object/1] RT/map RT/TAG_KEY meta))
            (instance? Keyword meta) (set! meta (^[Object/1] RT/map meta RT/T))
            :else
              (when-not (instance? IPersistentMap meta)
                (throw (IllegalArgumentException. "Metadata must be Symbol,Keyword,String or Map"))))
          (let [o (EdnReader/read r true nil true opts)]
            (if (instance? IMeta o)
                (do
                  (when (and (not (== line -1)) (instance? ISeq o))
                    (set! meta
                          (.assoc (.assoc (cast IPersistentMap meta) RT/LINE_KEY line)
                                  RT/COLUMN_KEY
                                  column)))
                  (if (instance? IReference o)
                      (do (.resetMeta (cast IReference o) (cast IPersistentMap meta)) o)
                      (let [^:mutable ^Object ometa (RT/meta o)]
                        (loop [s (RT/seq meta)]
                          (when (some? s)
                            (let [kv (cast IMapEntry (.first s))]
                              (set! ometa (RT/assoc ometa (.getKey kv) (.getValue kv)))
                              (recur (.next s)))))
                        (.withMeta (cast IObj o) (cast IPersistentMap ometa)))))
                (throw (IllegalArgumentException. "Metadata can only be applied to IMetas"))))))))

  (defclass ^:public ^:static CharacterReader
    :extends AFn

    (method ^:public invoke [this reader backslash opts]
      (let [r (cast PushbackReader reader)
            ch (EdnReader/read1 r)]
        (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
        (let [token (EdnReader/readToken r (unchecked-char ch) false)]
          (cond
            (== (.length token) 1) (Character/valueOf (.charAt token 0))
            (.equals token "newline") \newline
            (.equals token "space") \space
            (.equals token "tab") \tab
            (.equals token "backspace") \backspace
            (.equals token "formfeed") \formfeed
            (.equals token "return") \return
            (.startsWith token "u")
              (let [c (unchecked-char (EdnReader/readUnicodeChar token 1 4 16))]
                (when (and (>= c (unchecked-char 0xD800)) (<= c (unchecked-char 0xDFFF)))
                  (throw (Util/runtimeException
                           (java-str "Invalid character constant: \\u" (Integer/toString c 16)))))
                c)
            (.startsWith token "o")
              (let [len (unchecked-subtract-int (.length token) 1)]
                (when (> len 3)
                  (throw (Util/runtimeException
                           (java-str "Invalid octal escape sequence length: " len))))
                (let [uc (EdnReader/readUnicodeChar token 1 len 8)]
                  (when (> uc 0377)
                    (throw (Util/runtimeException
                             "Octal escape sequence must be in range [0, 377].")))
                  (unchecked-char uc)))
            :else (throw (Util/runtimeException (java-str "Unsupported character: \\" token))))))))

  (defclass ^:public ^:static ListReader
    :extends AFn

    (method ^:public invoke [this reader leftparen opts]
      (let [r (cast PushbackReader reader)
            ^:mutable ^int line -1
            ^:mutable ^int column -1]
        (when (instance? LineNumberingPushbackReader r)
          (set! line (.getLineNumber (cast LineNumberingPushbackReader r)))
          (set! column
                (unchecked-subtract-int (.getColumnNumber (cast LineNumberingPushbackReader r)) 1)))
        (let [list (EdnReader/readDelimitedList \) r true opts)]
          (if (.isEmpty list)
              PersistentList/EMPTY
              (let [s (cast IObj (PersistentList/create list))] s))))))

  (defclass ^:public ^:static VectorReader
    :extends AFn

    (method ^:public invoke [this reader leftparen opts]
      (let [r (cast PushbackReader reader)]
        (LazilyPersistentVector/create (EdnReader/readDelimitedList \] r true opts)))))

  (defclass ^:public ^:static MapReader
    :extends AFn

    (method ^:public invoke [this reader leftparen opts]
      (let [r (cast PushbackReader reader)
            a (.toArray (EdnReader/readDelimitedList \} r true opts))]
        (when (== (bit-and-int (alength a) 1) 1)
          (throw (Util/runtimeException "Map literal must contain an even number of forms")))
        (RT/map a))))

  (defclass ^:public ^:static SetReader
    :extends AFn

    (method ^:public invoke [this reader leftbracket opts]
      (let [r (cast PushbackReader reader)]
        (PersistentHashSet/createWithCheck (EdnReader/readDelimitedList \} r true opts)))))

  (defclass ^:public ^:static UnmatchedDelimiterReader
    :extends AFn

    (method ^:public invoke [this reader rightdelim opts]
      (throw (Util/runtimeException (java-str "Unmatched delimiter: " rightdelim)))))

  (defclass ^:public ^:static UnreadableReader
    :extends AFn

    (method ^:public invoke [this reader leftangle opts]
      (throw (Util/runtimeException "Unreadable form"))))

  (defclass ^:public ^:static SymbolicValueReader
    :extends AFn

    (field ^:static ^IPersistentMap specials
      (PersistentHashMap/create
        (new Object/1
             [(Symbol/intern "Inf")
              Double/POSITIVE_INFINITY
              (Symbol/intern "-Inf")
              Double/NEGATIVE_INFINITY
              (Symbol/intern "NaN")
              Double/NaN])))

    (method ^:public invoke [this reader quote opts]
      (let [r (cast PushbackReader reader)
            o (EdnReader/read r true nil true opts)]
        (when-not (instance? Symbol o)
          (throw (Util/runtimeException (java-str "Invalid token: ##" o))))
        (when-not (.containsKey specials o)
          (throw (Util/runtimeException (java-str "Unknown symbolic value: ##" o))))
        (.valAt specials o))))

  (method ^:public ^:static readDelimitedList ^List [^char delim ^PushbackReader r
                                                     ^boolean isRecursive opts]
    (let [firstline (if (instance? LineNumberingPushbackReader r)
                        (.getLineNumber (cast LineNumberingPushbackReader r))
                        -1)
          a (ArrayList.)]
      (while true
        (let [^:mutable ch (EdnReader/read1 r)]
          (while (EdnReader/isWhitespace ch) (set! ch (EdnReader/read1 r)))
          (when (== ch -1)
            (if (< firstline 0)
                (throw (Util/runtimeException "EOF while reading"))
                (throw (Util/runtimeException
                         (java-str "EOF while reading, starting at line " firstline)))))
          (when (== ch delim) (break))
          (let [macroFn (EdnReader/getMacro ch)]
            (if (some? macroFn)
                (let [mret (.invoke macroFn r (unchecked-char ch) opts)]
                  (when-not (identical? mret r) (.add a mret)))
                (do
                  (EdnReader/unread r ch)
                  (let [o (EdnReader/read r true nil isRecursive opts)]
                    (when-not (identical? o r) (.add a o))))))))
      a))

  (defclass ^:public ^:static TaggedReader
    :extends AFn

    (method ^:public invoke [this reader firstChar opts]
      (let [r (cast PushbackReader reader)
            name (EdnReader/read r true nil false opts)]
        (when-not (instance? Symbol name) (throw (RuntimeException. "Reader tag must be a symbol")))
        (let [sym (cast Symbol name)] (.readTagged this r sym (cast IPersistentMap opts)))))

    (field ^:static ^Keyword READERS (Keyword/intern nil "readers"))

    (field ^:static ^Keyword DEFAULT (Keyword/intern nil "default"))

    (method ^:private readTagged [this ^PushbackReader reader ^Symbol tag ^IPersistentMap opts]
      (let [o (EdnReader/read reader true nil true opts)
            readers (cast ILookup (RT/get opts READERS))
            ^:mutable dataReader (cast IFn (RT/get readers tag))]
        (when (nil? dataReader)
          (set! dataReader (cast IFn (RT/get (.deref RT/DEFAULT_DATA_READERS) tag))))
        (if (nil? dataReader)
            (let [defaultReader (cast IFn (RT/get opts DEFAULT))]
              (if (some? defaultReader)
                  (.invoke defaultReader tag o)
                  (throw (RuntimeException.
                           (java-str "No reader function for tag " (.toString tag))))))
            (.invoke dataReader o))))))
