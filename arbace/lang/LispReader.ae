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
;; Converted from clojure/lang/LispReader.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.io IOException PushbackReader Reader)
        '(java.lang.reflect Constructor)
        '(java.util ArrayList LinkedList List Map$Entry)
        '(java.util.regex Matcher Pattern))

(defclass ^:public LispReader
  (field ^:static ^:final ^Symbol QUOTE (Symbol/intern "quote"))

  (field ^:static ^:final ^Symbol THE_VAR (Symbol/intern "var"))

  (field ^:static ^Symbol UNQUOTE (Symbol/intern "arbace.core" "unquote"))

  (field ^:static ^Symbol UNQUOTE_SPLICING (Symbol/intern "arbace.core" "unquote-splicing"))

  (field ^:static ^Symbol CONCAT (Symbol/intern "arbace.core" "concat"))

  (field ^:static ^Symbol SEQ (Symbol/intern "arbace.core" "seq"))

  (field ^:static ^Symbol LIST (Symbol/intern "arbace.core" "list"))

  (field ^:static ^Symbol APPLY (Symbol/intern "arbace.core" "apply"))

  (field ^:static ^Symbol HASHMAP (Symbol/intern "arbace.core" "hash-map"))

  (field ^:static ^Symbol HASHSET (Symbol/intern "arbace.core" "hash-set"))

  (field ^:static ^Symbol VECTOR (Symbol/intern "arbace.core" "vector"))

  (field ^:static ^Symbol WITH_META (Symbol/intern "arbace.core" "with-meta"))

  (field ^:static ^Symbol META (Symbol/intern "arbace.core" "meta"))

  (field ^:static ^Symbol DEREF (Symbol/intern "arbace.core" "deref"))

  (field ^:static ^Symbol READ_COND (Symbol/intern "arbace.core" "read-cond"))

  (field ^:static ^Symbol READ_COND_SPLICING (Symbol/intern "arbace.core" "read-cond-splicing"))

  (field ^:static ^Keyword UNKNOWN (Keyword/intern nil "unknown"))

  (field ^:static ^IFn/1 macros (new IFn/1 256))

  (field ^:static ^IFn/1 dispatchMacros (new IFn/1 256))

  (field ^:static ^Pattern symbolPat (Pattern/compile "[:]?([\\D&&[^/]].*/)?(/|[\\D&&[^/]][^/]*)"))

  (field ^:static ^Pattern arraySymbolPat (Pattern/compile "([\\D&&[^/:]].*)/([1-9])"))

  (field ^:static ^Pattern intPat
    (Pattern/compile
      "([-+]?)(?:(0)|([1-9][0-9]*)|0[xX]([0-9A-Fa-f]+)|0([0-7]+)|([1-9][0-9]?)[rR]([0-9A-Za-z]+)|0[0-9]+)(N)?"))

  (field ^:static ^Pattern ratioPat (Pattern/compile "([-+]?[0-9]+)/([0-9]+)"))

  (field ^:static ^Pattern floatPat
    (Pattern/compile "([-+]?[0-9]+(\\.[0-9]*)?([eE][-+]?[0-9]+)?)(M)?"))

  (field ^:static ^Pattern argPat (Pattern/compile "%(?:(&)|([1-9][0-9]*))?"))

  (field ^:static ^Var GENSYM_ENV (.setDynamic (Var/create nil)))

  (field ^:static ^Var ARG_ENV (.setDynamic (Var/create nil)))

  (field ^:static ^IFn ctorReader (CtorReader.))

  (field ^:static ^Var READ_COND_ENV (.setDynamic (Var/create nil)))

  (static-initializer
    (aset macros \" (StringReader.))
    (aset macros \; (CommentReader.))
    (aset macros \' (WrappingReader. QUOTE))
    (aset macros \@ (WrappingReader. DEREF))
    (aset macros \^ (MetaReader.))
    (aset macros \` (SyntaxQuoteReader.))
    (aset macros \~ (UnquoteReader.))
    (aset macros \( (ListReader.))
    (aset macros \) (UnmatchedDelimiterReader.))
    (aset macros \[ (VectorReader.))
    (aset macros \] (UnmatchedDelimiterReader.))
    (aset macros \{ (MapReader.))
    (aset macros \} (UnmatchedDelimiterReader.))
    (aset macros \\ (CharacterReader.))
    (aset macros \% (ArgReader.))
    (aset macros \# (DispatchReader.))
    (aset dispatchMacros \^ (MetaReader.))
    (aset dispatchMacros \# (SymbolicValueReader.))
    (aset dispatchMacros \' (VarReader.))
    (aset dispatchMacros \" (RegexReader.))
    (aset dispatchMacros \( (FnReader.))
    (aset dispatchMacros \{ (SetReader.))
    (aset dispatchMacros \= (EvalReader.))
    (aset dispatchMacros \! (CommentReader.))
    (aset dispatchMacros \< (UnreadableReader.))
    (aset dispatchMacros \_ (DiscardReader.))
    (aset dispatchMacros \? (ConditionalReader.))
    (aset dispatchMacros \: (NamespaceMapReader.)))

  (defclass ^:public ^:static ^:interface Resolver
    (method currentNS ^Symbol [this])

    (method resolveClass ^Symbol [this ^Symbol sym])

    (method resolveAlias ^Symbol [this ^Symbol sym])

    (method resolveVar ^Symbol [this ^Symbol sym]))

  (method ^:static isWhitespace ^boolean [^int ch]
    (or (Character/isWhitespace ch) (== ch \,)))

  (method ^:static unread ^void [^PushbackReader r ^int ch]
    (when-not (== ch -1) (try (.unread r ch) (catch IOException e (throw (Util/sneakyThrow e))))))

  (defclass ^:public ^:static ReaderException
    :extends RuntimeException
    :implements [IExceptionInfo]

    (field ^:public ^:final ^int line)

    (field ^:public ^:final ^int column)

    (field ^:public ^:final data)

    (field ^:public ^:static ^:final ^String ERR_NS "arbace.error")

    (field ^:public ^:static ^:final ^Keyword ERR_LINE (Keyword/intern ERR_NS "line"))

    (field ^:public ^:static ^:final ^Keyword ERR_COLUMN (Keyword/intern ERR_NS "column"))

    (constructor ^:public [this ^int line ^int column ^Throwable cause]
      (super. cause)
      (set! (.-line this) line)
      (set! (.-column this) column)
      (set! (.-data this) (^[Object/1] RT/map ERR_LINE line ERR_COLUMN column)))

    (method ^:public getData ^IPersistentMap [this]
      (cast IPersistentMap data)))

  (method ^:public ^:static read1 ^int [^Reader r]
    (try (.read r) (catch IOException e (throw (Util/sneakyThrow e)))))

  (field ^:public ^:static ^:final ^Keyword OPT_EOF (Keyword/intern nil "eof"))

  (field ^:public ^:static ^:final ^Keyword OPT_FEATURES (Keyword/intern nil "features"))

  (field ^:public ^:static ^:final ^Keyword OPT_READ_COND (Keyword/intern nil "read-cond"))

  (field ^:public ^:static ^:final ^Keyword EOFTHROW (Keyword/intern nil "eofthrow"))

  (field ^:private ^:static ^:final ^Keyword PLATFORM_KEY (Keyword/intern nil "clj"))

  (field ^:private ^:static ^:final PLATFORM_FEATURES
    (PersistentHashSet/create (new Object/1 [PLATFORM_KEY])))

  (field ^:public ^:static ^:final ^Keyword COND_ALLOW (Keyword/intern nil "allow"))

  (field ^:public ^:static ^:final ^Keyword COND_PRESERVE (Keyword/intern nil "preserve"))

  (method ^:public ^:static read [^PushbackReader r opts]
    (let [^:mutable eofIsError true
          ^:mutable ^Object eofValue nil]
      (when (and (some? opts) (instance? IPersistentMap opts))
        (let [eof (.valAt (cast IPersistentMap opts) OPT_EOF EOFTHROW)]
          (when-not (.equals EOFTHROW eof) (set! eofIsError false) (set! eofValue eof))))
      (LispReader/read r eofIsError eofValue false opts)))

  (method ^:public ^:static read [^PushbackReader r ^boolean eofIsError eofValue
                                  ^boolean isRecursive]
    (LispReader/read r eofIsError eofValue isRecursive PersistentHashMap/EMPTY))

  (method ^:public ^:static read [^PushbackReader r ^boolean eofIsError eofValue
                                  ^boolean isRecursive opts]
    (LispReader/read r
                     eofIsError
                     eofValue
                     nil
                     nil
                     isRecursive
                     opts
                     nil
                     (cast Resolver (.deref RT/READER_RESOLVER))))

  (method ^:private ^:static read [^PushbackReader r ^boolean eofIsError eofValue
                                   ^boolean isRecursive opts pendingForms]
    (LispReader/read r
                     eofIsError
                     eofValue
                     nil
                     nil
                     isRecursive
                     opts
                     (LispReader/ensurePending pendingForms)
                     (cast Resolver (.deref RT/READER_RESOLVER))))

  (method ^:private ^:static ensurePending [pendingForms]
    (if (nil? pendingForms) (LinkedList.) pendingForms))

  (method ^:private ^:static installPlatformFeature [opts]
    (if (nil? opts)
        (^[Object/1] RT/mapUniqueKeys LispReader/OPT_FEATURES PLATFORM_FEATURES)
        (let [mopts (cast IPersistentMap opts)
              features (.valAt mopts OPT_FEATURES)]
          (if (nil? features)
              (.assoc mopts LispReader/OPT_FEATURES PLATFORM_FEATURES)
              (.assoc mopts
                      LispReader/OPT_FEATURES
                      (RT/conj (cast IPersistentSet features) PLATFORM_KEY))))))

  (method ^:private ^:static read [^PushbackReader r ^boolean eofIsError eofValue
                                   ^Character returnOn returnOnValue ^boolean isRecursive
                                   ^:mutable opts pendingForms ^Resolver resolver]
    (when (identical? (.deref RT/READEVAL) UNKNOWN)
      (throw (Util/runtimeException "Reading disallowed - *read-eval* bound to :unknown")))
    (set! opts (LispReader/installPlatformFeature opts))
    (try
      (while true
        (when (and (instance? List pendingForms) (not (.isEmpty (cast List pendingForms))))
          (return (^[int] List/.remove (cast List pendingForms) 0)))
        (let [^:mutable ch (LispReader/read1 r)]
          (while (LispReader/isWhitespace ch) (set! ch (LispReader/read1 r)))
          (when (== ch -1)
            (when eofIsError (throw (Util/runtimeException "EOF while reading")))
            (return eofValue))
          (when (and (some? returnOn) (== (.charValue returnOn) ch)) (return returnOnValue))
          (when (Character/isDigit ch)
            (let [n (LispReader/readNumber r (unchecked-char ch))] (return n)))
          (let [macroFn (LispReader/getMacro ch)]
            (when (some? macroFn)
              (let [ret (.invoke macroFn r (unchecked-char ch) opts pendingForms)]
                (when (identical? ret r) (continue))
                (return ret)))
            (when (or (== ch \+) (== ch \-))
              (let [ch2 (LispReader/read1 r)]
                (when (Character/isDigit ch2)
                  (LispReader/unread r ch2)
                  (let [n (LispReader/readNumber r (unchecked-char ch))] (return n)))
                (LispReader/unread r ch2)))
            (let [token (LispReader/readToken r (unchecked-char ch))]
              (return (LispReader/interpretToken token resolver))))))
      (catch Exception e
        (when (or isRecursive (not (instance? LineNumberingPushbackReader r)))
          (throw (Util/sneakyThrow e)))
        (let [rdr (cast LineNumberingPushbackReader r)]
          (throw (ReaderException. (.getLineNumber rdr) (.getColumnNumber rdr) e))))))

  (method ^:private ^:static readToken ^String [^PushbackReader r ^char initch]
    (let [sb (StringBuilder.)]
      (^[char] StringBuilder/.append sb initch)
      (while true
        (let [ch (LispReader/read1 r)]
          (when (or (or (== ch -1) (LispReader/isWhitespace ch)) (LispReader/isTerminatingMacro ch))
            (LispReader/unread r ch)
            (return (.toString sb)))
          (^[char] StringBuilder/.append sb (unchecked-char ch))))))

  (method ^:private ^:static readNumber [^PushbackReader r ^char initch]
    (let [sb (StringBuilder.)]
      (^[char] StringBuilder/.append sb initch)
      (while true
        (let [ch (LispReader/read1 r)]
          (when (or (or (== ch -1) (LispReader/isWhitespace ch)) (LispReader/isMacro ch))
            (LispReader/unread r ch)
            (break))
          (^[char] StringBuilder/.append sb (unchecked-char ch))))
      (let [s (.toString sb)
            n (LispReader/matchNumber s)]
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
          (let [ch (LispReader/read1 r)]
            (when (or (or (== ch -1) (LispReader/isWhitespace ch)) (LispReader/isMacro ch))
              (LispReader/unread r ch)
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

  (method ^:private ^:static interpretToken [^String s ^Resolver resolver]
    (when-not (.equals s "nil")
      (cond
        (.equals s "true") RT/T
        (.equals s "false") RT/F
        :else
          (let [^:mutable ^Object ret nil]
            (set! ret (LispReader/matchSymbol s resolver))
            (if (some? ret) ret (throw (Util/runtimeException (java-str "Invalid token: " s))))))))

  (method ^:private ^:static matchSymbol [^String s ^Resolver resolver]
    (let [m (.matcher symbolPat s)]
      (if (.matches m)
          (let [gc (.groupCount m)
                ns (^[int] Matcher/.group m 1)
                name (^[int] Matcher/.group m 2)]
            (when-not (or (or (and (some? ns) (.endsWith ns ":/")) (.endsWith name ":"))
                          (not (== (^[String int] String/.indexOf s "::" 1) -1)))
              (if (.startsWith s "::")
                  (let [ks (Symbol/intern (.substring s 2))]
                    (if (some? resolver)
                        (let [^Symbol nsym (if (some? (.-ns ks))
                                               (.resolveAlias resolver (Symbol/intern (.-ns ks)))
                                               (.currentNS resolver))]
                          (when (some? nsym) (Keyword/intern (.-name nsym) (.-name ks))))
                        (let [^Namespace kns (if (some? (.-ns ks))
                                                 (.lookupAlias
                                                   (arbace.lang.Compiler/currentNS)
                                                   (Symbol/intern (.-ns ks)))
                                                 (arbace.lang.Compiler/currentNS))]
                          (when (some? kns) (Keyword/intern (.-name (.-name kns)) (.-name ks))))))
                  (let [isKeyword (== (.charAt s 0) \:)
                        sym (Symbol/intern (.substring s (if isKeyword 1 0)))]
                    (if isKeyword (Keyword/intern sym) sym)))))
          (do
            (let [am (.matcher arraySymbolPat s)]
              (when (.matches am)
                (return (Symbol/intern (^[int] Matcher/.group am 1) (^[int] Matcher/.group am 2)))))
            nil))))

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
    (and (and (and (not (== ch \#)) (not (== ch \'))) (not (== ch \%))) (LispReader/isMacro ch)))

  (defclass ^:public ^:static RegexReader
    :extends AFn

    (field ^:static ^StringReader stringrdr (StringReader.))

    (method ^:public invoke [this reader doublequote opts pendingForms]
      (let [sb (StringBuilder.)
            r (cast Reader reader)]
        (let [^:mutable ch (LispReader/read1 r)]
          (while (not (== ch \"))
            (when (== ch -1) (throw (Util/runtimeException "EOF while reading regex")))
            (^[char] StringBuilder/.append sb (unchecked-char ch))
            (when (== ch \\)
              (set! ch (LispReader/read1 r))
              (when (== ch -1) (throw (Util/runtimeException "EOF while reading regex")))
              (^[char] StringBuilder/.append sb (unchecked-char ch)))
            (set! ch (LispReader/read1 r))))
        (Pattern/compile (.toString sb)))))

  (defclass ^:public ^:static StringReader
    :extends AFn

    (method ^:public invoke [this reader doublequote opts pendingForms]
      (let [sb (StringBuilder.)
            r (cast Reader reader)]
        (let [^:mutable ch (LispReader/read1 r)]
          (while (not (== ch \"))
            (when (== ch -1) (throw (Util/runtimeException "EOF while reading string")))
            (when (== ch \\)
              (set! ch (LispReader/read1 r))
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
                    (set! ch (LispReader/read1 r))
                    (when (== (^[int int] Character/digit ch 16) -1)
                      (throw (Util/runtimeException
                               (java-str "Invalid unicode escape: \\u" (unchecked-char ch)))))
                    (set! ch (LispReader/readUnicodeChar (cast PushbackReader r) ch 16 4 true)))
                (if (Character/isDigit ch)
                    (do
                      (set! ch (LispReader/readUnicodeChar (cast PushbackReader r) ch 8 3 false))
                      (when (> ch 0377)
                        (throw (Util/runtimeException
                                 "Octal escape sequence must be in range [0, 377]."))))
                    (throw (Util/runtimeException
                             (java-str "Unsupported escape character: \\" (unchecked-char ch)))))))
            (^[char] StringBuilder/.append sb (unchecked-char ch))
            (set! ch (LispReader/read1 r))))
        (.toString sb))))

  (defclass ^:public ^:static CommentReader
    :extends AFn

    (method ^:public invoke [this reader semicolon opts pendingForms]
      (let [r (cast Reader reader)
            ^:mutable ^int ch 0]
        (loop []
          (set! ch (LispReader/read1 r))
          (when (and (and (not (== ch -1)) (not (== ch \newline))) (not (== ch \return))) (recur)))
        r)))

  (defclass ^:public ^:static DiscardReader
    :extends AFn

    (method ^:public invoke [this reader underscore opts pendingForms]
      (let [r (cast PushbackReader reader)]
        (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))
        r)))

  (defclass ^:public ^:static NamespaceMapReader
    :extends AFn

    (method ^:public invoke [this reader colon opts pendingForms]
      (let [r (cast PushbackReader reader)
            ^:mutable auto false
            autoChar (LispReader/read1 r)]
        (if (== autoChar \:) (set! auto true) (LispReader/unread r autoChar))
        (let [^:mutable ^Object sym nil
              ^:mutable nextChar (LispReader/read1 r)]
          (if (LispReader/isWhitespace nextChar)
              (if auto
                  (do
                    (while (LispReader/isWhitespace nextChar) (set! nextChar (LispReader/read1 r)))
                    (when-not (== nextChar \{)
                      (LispReader/unread r nextChar)
                      (throw (Util/runtimeException "Namespaced map must specify a namespace"))))
                  (do
                    (LispReader/unread r nextChar)
                    (throw (Util/runtimeException "Namespaced map must specify a namespace"))))
              (when-not (== nextChar \{)
                (LispReader/unread r nextChar)
                (set! sym (LispReader/read r true nil false opts pendingForms))
                (set! nextChar (LispReader/read1 r))
                (while (LispReader/isWhitespace nextChar) (set! nextChar (LispReader/read1 r)))))
          (when-not (== nextChar \{)
            (throw (Util/runtimeException "Namespaced map must specify a map")))
          (let [^:mutable ^String ns nil]
            (cond
              auto
                (let [resolver (cast Resolver (.deref RT/READER_RESOLVER))]
                  (cond
                    (nil? sym)
                      (if (some? resolver)
                          (set! ns (.-name (.currentNS resolver)))
                          (set! ns (.getName (.getName (arbace.lang.Compiler/currentNS)))))
                    (or (not (instance? Symbol sym)) (some? (.getNamespace (cast Symbol sym))))
                      (throw (Util/runtimeException
                               (java-str "Namespaced map must specify a valid namespace: " sym)))
                    :else
                      (let [^:mutable ^Symbol resolvedNS nil]
                        (if (some? resolver)
                            (set! resolvedNS (.resolveAlias resolver (cast Symbol sym)))
                            (let [rns (.lookupAlias
                                        (arbace.lang.Compiler/currentNS)
                                        (cast Symbol sym))]
                              (set! resolvedNS (when (some? rns) (.getName rns)))))
                        (if (nil? resolvedNS)
                            (throw (Util/runtimeException
                                     (java-str "Unknown auto-resolved namespace alias: " sym)))
                            (set! ns (.getName resolvedNS))))))
              (or (not (instance? Symbol sym)) (some? (.getNamespace (cast Symbol sym))))
                (throw (Util/runtimeException
                         (java-str "Namespaced map must specify a valid namespace: " sym)))
              :else (set! ns (.getName (cast Symbol sym))))
            (let [kvs (LispReader/readDelimitedList
                        \}
                        r
                        true
                        opts
                        (LispReader/ensurePending pendingForms))]
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
                (RT/map a))))))))

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

    (method ^:public invoke [this reader quote opts pendingForms]
      (let [r (cast PushbackReader reader)
            o (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))]
        (when-not (instance? Symbol o)
          (throw (Util/runtimeException (java-str "Invalid token: ##" o))))
        (when-not (.containsKey specials o)
          (throw (Util/runtimeException (java-str "Unknown symbolic value: ##" o))))
        (.valAt specials o))))

  (defclass ^:public ^:static WrappingReader
    :extends AFn

    (field ^:final ^Symbol sym)

    (constructor ^:public [this ^Symbol sym] (set! (.-sym this) sym))

    (method ^:public invoke [this reader quote opts pendingForms]
      (let [r (cast PushbackReader reader)
            o (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))]
        (RT/list sym o))))

  (defclass ^:public ^:static DeprecatedWrappingReader
    :extends AFn

    (field ^:final ^Symbol sym)

    (field ^:final ^String macro)

    (constructor ^:public [this ^Symbol sym ^String macro]
      (set! (.-sym this) sym)
      (set! (.-macro this) macro))

    (method ^:public invoke [this reader quote opts pendingForms]
      (.println System/out
                (java-str "WARNING: reader macro "
                          macro
                          " is deprecated; use "
                          (.getName sym)
                          " instead"))
      (let [r (cast PushbackReader reader)
            o (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))]
        (RT/list sym o))))

  (defclass ^:public ^:static VarReader
    :extends AFn

    (method ^:public invoke [this reader quote opts pendingForms]
      (let [r (cast PushbackReader reader)
            o (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))]
        (RT/list THE_VAR o))))

  (defclass ^:public ^:static DispatchReader
    :extends AFn

    (method ^:public invoke [this reader hash opts ^:mutable pendingForms]
      (let [ch (LispReader/read1 (cast Reader reader))]
        (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
        (let [fn (when (< ch (alength dispatchMacros)) (aget dispatchMacros ch))]
          (if (nil? fn)
              (do
                (LispReader/unread (cast PushbackReader reader) ch)
                (set! pendingForms (LispReader/ensurePending pendingForms))
                (let [result (.invoke ctorReader reader ch opts pendingForms)]
                  (if (some? result)
                      result
                      (throw (Util/runtimeException
                               (String/format "No dispatch macro for: %c"
                                              (new Object/1 [(unchecked-char ch)])))))))
              (.invoke fn reader ch opts pendingForms))))))

  (method ^:static garg ^Symbol [^int n]
    (Symbol/intern nil (java-str (if (== n -1) "rest" (java-str "p" n)) "__" (RT/nextID) "#")))

  (defclass ^:public ^:static FnReader
    :extends AFn

    (method ^:public invoke [this reader lparen opts pendingForms]
      (let [r (cast PushbackReader reader)]
        (when (some? (.deref ARG_ENV))
          (throw (IllegalStateException. "Nested #()s are not allowed")))
        (try
          (Var/pushThreadBindings (^[Object/1] RT/map ARG_ENV PersistentTreeMap/EMPTY))
          (LispReader/unread r \()
          (let [form (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))
                ^:mutable args PersistentVector/EMPTY
                argsyms (cast PersistentTreeMap (.deref ARG_ENV))
                rargs (.rseq argsyms)]
            (when (some? rargs)
              (let [^int higharg (cast Integer (.getKey (cast Map$Entry (.first rargs))))]
                (when (> higharg 0)
                  (loop [^int i 1]
                    (when (<= i higharg)
                      (let [^:mutable sym (.valAt argsyms i)]
                        (when (nil? sym) (set! sym (LispReader/garg i)))
                        (set! args (.cons args sym))
                        (recur (unchecked-inc-int i))))))
                (let [restsym (.valAt argsyms (Integer/valueOf -1))]
                  (when (some? restsym)
                    (set! args (.cons args arbace.lang.Compiler/_AMP_))
                    (set! args (.cons args restsym))))))
            (RT/list arbace.lang.Compiler/FN args form))
          (finally (Var/popThreadBindings))))))

  (method ^:static registerArg ^Symbol [^int n]
    (let [argsyms (cast PersistentTreeMap (.deref ARG_ENV))]
      (when (nil? argsyms) (throw (IllegalStateException. "arg literal not in #()")))
      (let [^:mutable ret (cast Symbol (.valAt argsyms n))]
        (when (nil? ret) (set! ret (LispReader/garg n)) (.set ARG_ENV (.assoc argsyms n ret)))
        ret)))

  (defclass ^:static ArgReader
    :extends AFn

    (method ^:public invoke [this reader pct opts pendingForms]
      (let [r (cast PushbackReader reader)
            token (LispReader/readToken r \%)]
        (if (nil? (.deref ARG_ENV))
            (LispReader/interpretToken token nil)
            (let [m (.matcher argPat token)]
              (when-not (.matches m)
                (throw (IllegalStateException. "arg literal must be %, %& or %integer")))
              (if (some? (^[int] Matcher/.group m 1))
                  (LispReader/registerArg -1)
                  (LispReader/registerArg
                    (if (nil? (^[int] Matcher/.group m 2))
                        1
                        (Integer/parseInt (^[int] Matcher/.group m 2))))))))))

  (defclass ^:public ^:static MetaReader
    :extends AFn

    (method ^:public invoke [this reader caret opts ^:mutable pendingForms]
      (let [r (cast PushbackReader reader)
            ^:mutable ^int line -1
            ^:mutable ^int column -1]
        (when (instance? LineNumberingPushbackReader r)
          (set! line (.getLineNumber (cast LineNumberingPushbackReader r)))
          (set! column
                (unchecked-subtract-int (.getColumnNumber (cast LineNumberingPushbackReader r)) 1)))
        (set! pendingForms (LispReader/ensurePending pendingForms))
        (let [^:mutable meta (LispReader/read r true nil true opts pendingForms)]
          (cond
            (or (instance? Symbol meta) (instance? String meta))
              (set! meta (^[Object/1] RT/map RT/TAG_KEY meta))
            (instance? Keyword meta) (set! meta (^[Object/1] RT/map meta RT/T))
            (instance? IPersistentVector meta)
              (set! meta (^[Object/1] RT/map RT/PARAM_TAGS_KEY meta))
            :else
              (when-not (instance? IPersistentMap meta)
                (throw (IllegalArgumentException.
                         "Metadata must be Symbol,Keyword,String,Vector or Map"))))
          (let [o (LispReader/read r true nil true opts pendingForms)]
            (if (instance? IMeta o)
                (do
                  (when (and (not (== line -1)) (instance? ISeq o))
                    (set! meta (RT/assoc meta RT/LINE_KEY (RT/get meta RT/LINE_KEY line)))
                    (set! meta (RT/assoc meta RT/COLUMN_KEY (RT/get meta RT/COLUMN_KEY column))))
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

  (defclass ^:public ^:static SyntaxQuoteReader
    :extends AFn

    (method ^:public invoke [this reader backquote opts pendingForms]
      (let [r (cast PushbackReader reader)]
        (try
          (Var/pushThreadBindings (^[Object/1] RT/map GENSYM_ENV PersistentHashMap/EMPTY))
          (let [form (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))]
            (SyntaxQuoteReader/syntaxQuote form))
          (finally (Var/popThreadBindings)))))

    (method ^:static syntaxQuote [form]
      (let [^:mutable ^Object ret nil]
        (cond
          (arbace.lang.Compiler/isSpecial form) (set! ret (RT/list arbace.lang.Compiler/QUOTE form))
          (instance? Symbol form)
            (let [resolver (cast Resolver (.deref RT/READER_RESOLVER))
                  ^:mutable sym (cast Symbol form)]
              (cond
                (and (nil? (.-ns sym)) (.endsWith (.-name sym) "#"))
                  (let [gmap (cast IPersistentMap (.deref GENSYM_ENV))]
                    (when (nil? gmap)
                      (throw (IllegalStateException. "Gensym literal not in syntax-quote")))
                    (let [^:mutable gs (cast Symbol (.valAt gmap sym))]
                      (when (nil? gs)
                        (.set GENSYM_ENV
                              (.assoc gmap
                                      sym
                                      (set! gs
                                            (Symbol/intern
                                              nil
                                              (java-str
                                                (.substring
                                                  (.-name sym)
                                                  0
                                                  (unchecked-subtract-int (.length (.-name sym)) 1))
                                                "__"
                                                (RT/nextID)
                                                "__auto__"))))))
                      (set! sym gs)))
                (and (nil? (.-ns sym)) (.endsWith (.-name sym) "."))
                  (let [^:mutable csym (Symbol/intern
                                         nil
                                         (.substring
                                           (.-name sym)
                                           0
                                           (unchecked-subtract-int (.length (.-name sym)) 1)))]
                    (if (some? resolver)
                        (let [rc (.resolveClass resolver csym)] (when (some? rc) (set! csym rc)))
                        (set! csym (arbace.lang.Compiler/resolveSymbol csym)))
                    (set! sym (Symbol/intern nil (.concat (.-name csym) "."))))
                :else
                  (when-not (and (nil? (.-ns sym)) (.startsWith (.-name sym) "."))
                    (if (some? resolver)
                        (let [^:mutable ^Symbol nsym nil]
                          (when (some? (.-ns sym))
                            (let [alias (Symbol/intern nil (.-ns sym))]
                              (set! nsym (.resolveClass resolver alias))
                              (when (nil? nsym) (set! nsym (.resolveAlias resolver alias)))))
                          (cond
                            (some? nsym) (set! sym (Symbol/intern (.-name nsym) (.-name sym)))
                            (nil? (.-ns sym))
                              (let [^:mutable rsym (.resolveClass resolver sym)]
                                (when (nil? rsym) (set! rsym (.resolveVar resolver sym)))
                                (if (some? rsym)
                                    (set! sym rsym)
                                    (set! sym
                                          (Symbol/intern
                                            (.-name (.currentNS resolver))
                                            (.-name sym)))))))
                        (let [^:mutable ^Object maybeClass nil]
                          (when (some? (.-ns sym))
                            (set! maybeClass
                                  (.getMapping (arbace.lang.Compiler/currentNS)
                                               (Symbol/intern nil (.-ns sym)))))
                          (if (instance? Class maybeClass)
                              (set! sym
                                    (Symbol/intern (.getName (cast Class maybeClass)) (.-name sym)))
                              (set! sym (arbace.lang.Compiler/resolveSymbol sym)))))))
              (set! ret (RT/list arbace.lang.Compiler/QUOTE sym)))
          (LispReader/isUnquote form) (return (RT/second form))
          (LispReader/isUnquoteSplicing form) (throw (IllegalStateException. "splice not in list"))
          (instance? IPersistentCollection form)
            (cond
              (instance? IRecord form) (set! ret form)
              (instance? IPersistentMap form)
                (let [keyvals (SyntaxQuoteReader/flattenMap form)]
                  (set! ret
                        (RT/list APPLY
                                 HASHMAP
                                 (RT/list SEQ
                                          (RT/cons
                                            CONCAT
                                            (SyntaxQuoteReader/sqExpandList (.seq keyvals)))))))
              (instance? IPersistentVector form)
                (set! ret
                      (RT/list APPLY
                               VECTOR
                               (RT/list SEQ
                                        (RT/cons
                                          CONCAT
                                          (SyntaxQuoteReader/sqExpandList
                                            (.seq (cast IPersistentVector form)))))))
              (instance? IPersistentSet form)
                (set! ret
                      (RT/list APPLY
                               HASHSET
                               (RT/list SEQ
                                        (RT/cons
                                          CONCAT
                                          (SyntaxQuoteReader/sqExpandList
                                            (.seq (cast IPersistentSet form)))))))
              (or (instance? ISeq form) (instance? IPersistentList form))
                (let [seq (RT/seq form)]
                  (if (nil? seq)
                      (set! ret (RT/cons LIST nil))
                      (set! ret (RT/list SEQ (RT/cons CONCAT (SyntaxQuoteReader/sqExpandList seq))))))
              :else (throw (UnsupportedOperationException. "Unknown Collection type")))
          (or (or (or (instance? Keyword form) (instance? Number form)) (instance? Character form))
              (instance? String form))
            (set! ret form)
          :else (set! ret (RT/list arbace.lang.Compiler/QUOTE form)))
        (when (and (instance? IObj form) (some? (RT/meta form)))
          (let [newMeta (.without (.without (.meta (cast IObj form)) RT/LINE_KEY) RT/COLUMN_KEY)]
            (when (> (.count newMeta) 0)
              (return (RT/list WITH_META
                               ret
                               (SyntaxQuoteReader/syntaxQuote (.meta (cast IObj form))))))))
        ret))

    (method ^:private ^:static sqExpandList ^ISeq [^:mutable ^ISeq seq]
      (let [^:mutable ret PersistentVector/EMPTY]
        (while (some? seq)
          (let [item (.first seq)]
            (cond
              (LispReader/isUnquote item) (set! ret (.cons ret (RT/list LIST (RT/second item))))
              (LispReader/isUnquoteSplicing item) (set! ret (.cons ret (RT/second item)))
              :else (set! ret (.cons ret (RT/list LIST (SyntaxQuoteReader/syntaxQuote item))))))
          (set! seq (.next seq)))
        (.seq ret)))

    (method ^:private ^:static flattenMap ^IPersistentVector [form]
      (let [^:mutable ^IPersistentVector keyvals PersistentVector/EMPTY]
        (loop [s (RT/seq form)]
          (when (some? s)
            (let [e (cast IMapEntry (.first s))]
              (set! keyvals (.cons keyvals (.key e)))
              (set! keyvals (.cons keyvals (.val e)))
              (recur (.next s)))))
        keyvals)))

  (method ^:static isUnquoteSplicing ^boolean [form]
    (and (instance? ISeq form) (Util/equals (RT/first form) UNQUOTE_SPLICING)))

  (method ^:static isUnquote ^boolean [form]
    (and (instance? ISeq form) (Util/equals (RT/first form) UNQUOTE)))

  (defclass ^:static UnquoteReader
    :extends AFn

    (method ^:public invoke [this reader comma opts ^:mutable pendingForms]
      (let [r (cast PushbackReader reader)
            ch (LispReader/read1 r)]
        (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
        (set! pendingForms (LispReader/ensurePending pendingForms))
        (if (== ch \@)
            (let [o (LispReader/read r true nil true opts pendingForms)]
              (RT/list UNQUOTE_SPLICING o))
            (do
              (LispReader/unread r ch)
              (let [o (LispReader/read r true nil true opts pendingForms)] (RT/list UNQUOTE o)))))))

  (defclass ^:public ^:static CharacterReader
    :extends AFn

    (method ^:public invoke [this reader backslash opts pendingForms]
      (let [r (cast PushbackReader reader)
            ch (LispReader/read1 r)]
        (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
        (let [token (LispReader/readToken r (unchecked-char ch))]
          (cond
            (== (.length token) 1) (Character/valueOf (.charAt token 0))
            (.equals token "newline") \newline
            (.equals token "space") \space
            (.equals token "tab") \tab
            (.equals token "backspace") \backspace
            (.equals token "formfeed") \formfeed
            (.equals token "return") \return
            (.startsWith token "u")
              (let [c (unchecked-char (LispReader/readUnicodeChar token 1 4 16))]
                (when (and (>= c (unchecked-char 0xD800)) (<= c (unchecked-char 0xDFFF)))
                  (throw (Util/runtimeException
                           (java-str "Invalid character constant: \\u" (Integer/toString c 16)))))
                c)
            (.startsWith token "o")
              (let [len (unchecked-subtract-int (.length token) 1)]
                (when (> len 3)
                  (throw (Util/runtimeException
                           (java-str "Invalid octal escape sequence length: " len))))
                (let [uc (LispReader/readUnicodeChar token 1 len 8)]
                  (when (> uc 0377)
                    (throw (Util/runtimeException
                             "Octal escape sequence must be in range [0, 377].")))
                  (unchecked-char uc)))
            :else (throw (Util/runtimeException (java-str "Unsupported character: \\" token))))))))

  (defclass ^:public ^:static ListReader
    :extends AFn

    (method ^:public invoke [this reader leftparen opts pendingForms]
      (let [r (cast PushbackReader reader)
            ^:mutable ^int line -1
            ^:mutable ^int column -1]
        (when (instance? LineNumberingPushbackReader r)
          (set! line (.getLineNumber (cast LineNumberingPushbackReader r)))
          (set! column
                (unchecked-subtract-int (.getColumnNumber (cast LineNumberingPushbackReader r)) 1)))
        (let [list (LispReader/readDelimitedList
                     \)
                     r
                     true
                     opts
                     (LispReader/ensurePending pendingForms))]
          (if (.isEmpty list)
              PersistentList/EMPTY
              (let [s (cast IObj (PersistentList/create list))]
                (if (not (== line -1))
                    (let [^:mutable ^Object meta (RT/meta s)]
                      (set! meta (RT/assoc meta RT/LINE_KEY (RT/get meta RT/LINE_KEY line)))
                      (set! meta (RT/assoc meta RT/COLUMN_KEY (RT/get meta RT/COLUMN_KEY column)))
                      (.withMeta s (cast IPersistentMap meta)))
                    s)))))))

  (defclass ^:public ^:static EvalReader
    :extends AFn

    (method ^:public invoke [this reader eq opts pendingForms]
      (when-not (RT/booleanCast (.deref RT/READEVAL))
        (throw (Util/runtimeException "EvalReader not allowed when *read-eval* is false.")))
      (let [r (cast PushbackReader reader)
            o (LispReader/read r true nil true opts (LispReader/ensurePending pendingForms))]
        (cond
          (instance? Symbol o) (RT/classForName (.toString o))
          (instance? IPersistentList o)
            (let [fs (cast Symbol (RT/first o))]
              (cond
                (.equals fs THE_VAR)
                  (let [vs (cast Symbol (RT/second o))] (RT/var (.-ns vs) (.-name vs)))
                (.endsWith (.-name fs) ".")
                  (let [args (RT/toArray (RT/next o))]
                    (Reflector/invokeConstructor
                      (RT/classForName
                        (.substring (.-name fs) 0 (unchecked-subtract-int (.length (.-name fs)) 1)))
                      args))
                (arbace.lang.Compiler/namesStaticMember fs)
                  (let [args (RT/toArray (RT/next o))]
                    (Reflector/invokeStaticMethod (.-ns fs) (.-name fs) args))
                :else
                  (let [v (arbace.lang.Compiler/maybeResolveIn (arbace.lang.Compiler/currentNS) fs)]
                    (if (instance? Var v)
                        (.applyTo (cast IFn v) (RT/next o))
                        (throw (Util/runtimeException (java-str "Can't resolve " fs)))))))
          :else (throw (IllegalArgumentException. "Unsupported #= form"))))))

  (defclass ^:public ^:static VectorReader
    :extends AFn

    (method ^:public invoke [this reader leftparen opts pendingForms]
      (let [r (cast PushbackReader reader)]
        (LazilyPersistentVector/create
          (LispReader/readDelimitedList \] r true opts (LispReader/ensurePending pendingForms))))))

  (defclass ^:public ^:static MapReader
    :extends AFn

    (method ^:public invoke [this reader leftparen opts pendingForms]
      (let [r (cast PushbackReader reader)
            a (.toArray (LispReader/readDelimitedList
                          \}
                          r
                          true
                          opts
                          (LispReader/ensurePending pendingForms)))]
        (when (== (bit-and-int (alength a) 1) 1)
          (throw (Util/runtimeException "Map literal must contain an even number of forms")))
        (RT/map a))))

  (defclass ^:public ^:static SetReader
    :extends AFn

    (method ^:public invoke [this reader leftbracket opts pendingForms]
      (let [r (cast PushbackReader reader)]
        (PersistentHashSet/createWithCheck
          (LispReader/readDelimitedList \} r true opts (LispReader/ensurePending pendingForms))))))

  (defclass ^:public ^:static UnmatchedDelimiterReader
    :extends AFn

    (method ^:public invoke [this reader rightdelim opts pendingForms]
      (throw (Util/runtimeException (java-str "Unmatched delimiter: " rightdelim)))))

  (defclass ^:public ^:static UnreadableReader
    :extends AFn

    (method ^:public invoke [this reader leftangle opts pendingForms]
      (throw (Util/runtimeException "Unreadable form"))))

  (field ^:private ^:static ^:final READ_EOF (Object.))

  (field ^:private ^:static ^:final READ_FINISHED (Object.))

  (method ^:public ^:static readDelimitedList ^List [^char delim ^PushbackReader r
                                                     ^boolean isRecursive opts pendingForms]
    (let [firstline (if (instance? LineNumberingPushbackReader r)
                        (.getLineNumber (cast LineNumberingPushbackReader r))
                        -1)
          a (ArrayList.)
          resolver (cast Resolver (.deref RT/READER_RESOLVER))]
      (while true
        (let [form (LispReader/read r
                                    false
                                    READ_EOF
                                    delim
                                    READ_FINISHED
                                    isRecursive
                                    opts
                                    pendingForms
                                    resolver)]
          (cond
            (identical? form READ_EOF)
              (if (< firstline 0)
                  (throw (Util/runtimeException "EOF while reading"))
                  (throw (Util/runtimeException
                           (java-str "EOF while reading, starting at line " firstline))))
            (identical? form READ_FINISHED) (return a))
          (.add a form)))))

  (defclass ^:public ^:static CtorReader
    :extends AFn

    (method ^:public invoke [this reader firstChar opts ^:mutable pendingForms]
      (let [r (cast PushbackReader reader)]
        (set! pendingForms (LispReader/ensurePending pendingForms))
        (let [name (LispReader/read r true nil false opts pendingForms)]
          (when-not (instance? Symbol name)
            (throw (RuntimeException. "Reader tag must be a symbol")))
          (let [sym (cast Symbol name)
                form (LispReader/read r true nil true opts pendingForms)]
            (cond
              (or (LispReader/isPreserveReadCond opts) (RT/suppressRead))
                (TaggedLiteral/create sym form)
              (.contains (.getName sym) ".") (.readRecord this form sym opts pendingForms)
              :else (.readTagged this form sym opts pendingForms))))))

    (method ^:private readTagged [this o ^Symbol tag opts pendingForms]
      (let [^:mutable data_readers (cast ILookup (.deref RT/DATA_READERS))
            ^:mutable data_reader (cast IFn (RT/get data_readers tag))]
        (when (nil? data_reader)
          (set! data_readers (cast ILookup (.deref RT/DEFAULT_DATA_READERS)))
          (set! data_reader (cast IFn (RT/get data_readers tag)))
          (when (nil? data_reader)
            (let [default_reader (cast IFn (.deref RT/DEFAULT_DATA_READER_FN))]
              (if (some? default_reader)
                  (return (.invoke default_reader tag o))
                  (throw (RuntimeException.
                           (java-str "No reader function for tag " (.toString tag))))))))
        (.invoke data_reader o)))

    (method ^:private readRecord [this form ^Symbol recordName opts pendingForms]
      (let [readeval (RT/booleanCast (.deref RT/READEVAL))]
        (when-not readeval
          (throw (Util/runtimeException
                   "Record construction syntax can only be used when *read-eval* == true")))
        (let [recordClass (RT/classForNameNonLoading (.toString recordName))
              ^:mutable shortForm true]
          (cond
            (instance? IPersistentMap form) (set! shortForm false)
            (instance? IPersistentVector form) (set! shortForm true)
            :else
              (throw (Util/runtimeException
                       (java-str "Unreadable constructor form starting with \"#" recordName "\""))))
          (let [^:mutable ^Object ret nil
                allctors (.getConstructors recordClass)]
            (if shortForm
                (let [recordEntries (cast IPersistentVector form)
                      ^:mutable ctorFound false]
                  (for-each [^Constructor ctor allctors]
                    (when (== (alength (.getParameterTypes ctor)) (.count recordEntries))
                      (set! ctorFound true)))
                  (when-not ctorFound
                    (throw (Util/runtimeException
                             (java-str "Unexpected number of constructor arguments to "
                                       (.toString recordClass)
                                       ": got "
                                       (.count recordEntries)))))
                  (set! ret (Reflector/invokeConstructor recordClass (RT/toArray recordEntries))))
                (let [vals (cast IPersistentMap form)]
                  (loop [s (RT/keys vals)]
                    (if (some? s)
                        (if (not (instance? Keyword (.first s)))
                            (throw
                              (Util/runtimeException
                                (java-str
                                  "Unreadable defrecord form: key must be of type arbace.lang.Keyword, got "
                                  (.toString (.first s)))))
                            (recur (.next s)))
                        nil))
                  (set! ret
                        (Reflector/invokeStaticMethod recordClass "create" (new Object/1 [vals])))))
            ret)))))

  (method ^:static isPreserveReadCond ^boolean [opts]
    (if (and (RT/booleanCast (.deref READ_COND_ENV)) (instance? IPersistentMap opts))
        (let [readCond (.valAt (cast IPersistentMap opts) OPT_READ_COND)]
          (.equals COND_PRESERVE readCond))
        false))

  (defclass ^:public ^:static ConditionalReader
    :extends AFn

    (field ^:private ^:static ^:final READ_STARTED (Object.))

    (field ^:public ^:static ^:final ^Keyword DEFAULT_FEATURE (Keyword/intern nil "default"))

    (field ^:public ^:static ^:final ^IPersistentSet RESERVED_FEATURES
      (^[Object/1] RT/set (Keyword/intern nil "else") (Keyword/intern nil "none")))

    (method ^:public ^:static hasFeature ^boolean [feature opts]
      (when-not (instance? Keyword feature)
        (throw (Util/runtimeException (java-str "Feature should be a keyword: " feature))))
      (if (.equals DEFAULT_FEATURE feature)
          true
          (let [custom (cast IPersistentSet (.valAt (cast IPersistentMap opts) OPT_FEATURES))]
            (and (some? custom) (.contains custom feature)))))

    (method ^:public ^:static readCondDelimited [^PushbackReader r ^boolean splicing opts
                                                 ^:mutable pendingForms]
      (let [^:mutable result READ_STARTED
            ^:mutable ^Object form nil
            toplevel (nil? pendingForms)]
        (set! pendingForms (LispReader/ensurePending pendingForms))
        (let [firstline (if (instance? LineNumberingPushbackReader r)
                            (.getLineNumber (cast LineNumberingPushbackReader r))
                            -1)]
          (while true
            (when (identical? result READ_STARTED)
              (set! form
                    (LispReader/read r false READ_EOF \) READ_FINISHED true opts pendingForms nil))
              (cond
                (identical? form READ_EOF)
                  (if (< firstline 0)
                      (throw (Util/runtimeException "EOF while reading"))
                      (throw (Util/runtimeException
                               (java-str "EOF while reading, starting at line " firstline))))
                (identical? form READ_FINISHED) (break))
              (when (.contains RESERVED_FEATURES form)
                (throw (Util/runtimeException (java-str "Feature name " form " is reserved."))))
              (when (ConditionalReader/hasFeature form opts)
                (set! form
                      (LispReader/read r
                                       false
                                       READ_EOF
                                       \)
                                       READ_FINISHED
                                       true
                                       opts
                                       pendingForms
                                       (cast Resolver (.deref RT/READER_RESOLVER))))
                (cond
                  (identical? form READ_EOF)
                    (if (< firstline 0)
                        (throw (Util/runtimeException "EOF while reading"))
                        (throw (Util/runtimeException
                                 (java-str "EOF while reading, starting at line " firstline))))
                  (identical? form READ_FINISHED)
                    (if (< firstline 0)
                        (throw
                          (Util/runtimeException "read-cond requires an even number of forms."))
                        (throw (Util/runtimeException
                                 (java-str "read-cond starting on line "
                                           firstline
                                           " requires an even number of forms"))))
                  :else (set! result form))))
            (try
              (Var/pushThreadBindings (^[Object/1] RT/map RT/SUPPRESS_READ RT/T))
              (set! form
                    (LispReader/read r
                                     false
                                     READ_EOF
                                     \)
                                     READ_FINISHED
                                     true
                                     opts
                                     pendingForms
                                     (cast Resolver (.deref RT/READER_RESOLVER))))
              (cond
                (identical? form READ_EOF)
                  (if (< firstline 0)
                      (throw (Util/runtimeException "EOF while reading"))
                      (throw (Util/runtimeException
                               (java-str "EOF while reading, starting at line " firstline))))
                (identical? form READ_FINISHED) (break))
              (finally (Var/popThreadBindings))))
          (cond
            (identical? result READ_STARTED) r
            splicing
              (do
                (when-not (instance? List result)
                  (throw (Util/runtimeException
                           "Spliced form list in read-cond-splicing must implement java.util.List")))
                (when toplevel
                  (throw (Util/runtimeException
                           "Reader conditional splicing not allowed at the top level.")))
                (.addAll (cast List pendingForms) 0 (cast List result))
                r)
            :else result))))

    (method ^:private ^:static checkConditionalAllowed ^void [opts]
      (let [mopts (cast IPersistentMap opts)]
        (when-not (and (some? opts)
                       (or (.equals COND_ALLOW (.valAt mopts OPT_READ_COND))
                           (.equals COND_PRESERVE (.valAt mopts OPT_READ_COND))))
          (throw (Util/runtimeException "Conditional read not allowed")))))

    (method ^:public invoke [this reader mode opts pendingForms]
      (ConditionalReader/checkConditionalAllowed opts)
      (let [r (cast PushbackReader reader)
            ^:mutable ch (LispReader/read1 r)]
        (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
        (let [^:mutable splicing false]
          (when (== ch \@) (set! splicing true) (set! ch (LispReader/read1 r)))
          (while (LispReader/isWhitespace ch) (set! ch (LispReader/read1 r)))
          (when (== ch -1) (throw (Util/runtimeException "EOF while reading character")))
          (when-not (== ch \() (throw (Util/runtimeException "read-cond body must be a list")))
          (try
            (Var/pushThreadBindings (^[Object/1] RT/map READ_COND_ENV RT/T))
            (if (LispReader/isPreserveReadCond opts)
                (let [listReader (LispReader/getMacro ch)
                      form (.invoke listReader r ch opts (LispReader/ensurePending pendingForms))]
                  (ReaderConditional/create form splicing))
                (ConditionalReader/readCondDelimited r splicing opts pendingForms))
            (finally (Var/popThreadBindings))))))))
