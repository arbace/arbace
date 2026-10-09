(ns jrt.testdata-reflect
  "jrt's differential test data for phase 2b (doc/go/JRT-NOTES.md, \"Phase 2b\"): reflection
  on jrt's own classes (through their member tables), java.lang.reflect.Array, charsets,
  locales and dates, as the JVM computes them. Written by jrt.testdata into DIR: reflect.txt,
  charsets.txt, locales.txt, dates.txt (the format of jrt.testdata).

  Values in reflect.txt are tokens: n (null), s:TEXT (escaped), i: j: h: b: (int, long, short,
  byte), c: (a char's code), z: (boolean), d: (a double, Java's toString), C:NAME (the Class
  of a JVM name), L:ROOT|US|ENGLISH (a Locale), cs:NAME (a Charset), ia:1,2 (an int[]), ca:TEXT
  (a char[]). A result is null or CLASS:TEXT (String.valueOf of the value; an array's elements
  joined by commas in brackets), or the exception (!CLASS: MESSAGE, then / and the cause for an
  InvocationTargetException)."
  (:require [arbace.string :as str]
            [arbace.instant :as inst]
            [jrt.testdata-util :refer [esc ex-text line write]]
            [g2c.jrt-sources :as jrt-sources])
  (:import [java.lang.reflect Array Field Method Modifier InvocationTargetException]
           [java.nio.charset Charset]
           [java.util Date Locale Random TimeZone]))

;; ---------------------------------------------------------------------------------------
;; Tokens

(defn class-of [^String n]
  (case n
    "int" Integer/TYPE "long" Long/TYPE "short" Short/TYPE "byte" Byte/TYPE "char" Character/TYPE
    "boolean" Boolean/TYPE "float" Float/TYPE "double" Double/TYPE "void" Void/TYPE
    (Class/forName n false (ClassLoader/getPlatformClassLoader))))

(defn value [^String tok]
  (let [[k v] (str/split tok #":" 2)]
    (case k
      "n" nil
      "s" v
      "i" (Integer/valueOf (Integer/parseInt v))
      "j" (Long/valueOf (Long/parseLong v))
      "h" (Short/valueOf (Short/parseShort v))
      "b" (Byte/valueOf (Byte/parseByte v))
      "c" (Character/valueOf (char (Integer/parseInt v)))
      "z" (Boolean/valueOf (Boolean/parseBoolean v))
      "d" (Double/valueOf (Double/parseDouble v))
      "C" (class-of v)
      "L" ({"ROOT" Locale/ROOT "US" Locale/US "ENGLISH" Locale/ENGLISH} v)
      "cs" (Charset/forName v)
      "ia" (int-array (map #(Integer/parseInt %) (str/split v #",")))
      "ca" (char-array v))))

(defn text-of [v]
  (cond
    (nil? v) "null"
    (.isArray (class v)) (str (.getName (class v)) ":["
                              (str/join "," (for [i (range (Array/getLength v))] (esc (str (Array/get v i))))) "]")
    :else (str (.getName (class v)) ":" (esc (str v)))))

(defn outcome [f]
  (try
    (text-of (f))
    (catch InvocationTargetException e
      (str (ex-text e) " / " (ex-text (.getCause e))))
    (catch Throwable e (ex-text e))))

(defn params-of [^String ps]
  (into-array Class (if (= ps "") [] (map class-of (str/split ps #",")))))

(defn args-of [^String as]
  (object-array (if (= as "") [] (map value (str/split as #" ")))))

;; ---------------------------------------------------------------------------------------
;; Reflection: which members jrt's tables hold (the manifest's public members)

(def manifest (binding [*read-eval* false] (read-string (slurp "go/arbace/jrt/manifest.edn"))))

(defn desc [^Method m]
  (.toMethodDescriptorString (java.lang.invoke.MethodType/methodType (.getReturnType m) (.getParameterTypes m))))

(defn table-sigs
  "The public name+descriptor signatures the member tables of c and its supertypes hold
  (Object's public methods included)."
  [^Class c]
  (let [supers (loop [todo [c] seen #{}]
                 (if (empty? todo)
                   seen
                   (let [x (first todo)]
                     (recur (into (rest todo) (remove nil? (cons (.getSuperclass ^Class x) (.getInterfaces ^Class x))))
                            (conj seen x)))))]
    (into (set (for [^Method m (.getMethods Object)]
                 (str (.getName m) (desc m))))
          (for [^Class s supers
                [kind n d flags] (get-in manifest [(symbol (.getName s)) :members])
                :when (and (= kind :method) (contains? flags :public))]
            (str n d)))))

(defn sig [^Method m]
  (str (.getName m) (desc m) (if (Modifier/isStatic (.getModifiers m)) " static" "")))

(def method-classes
  ["java.lang.Object" "java.lang.String" "java.lang.Math" "java.lang.StringBuilder" "java.lang.Class"
   "java.lang.Throwable" "java.util.Locale" "java.util.Locale$Category" "java.util.Date"
   "java.nio.charset.Charset" "sun.nio.cs.UTF_8" "java.lang.reflect.Method" "java.lang.reflect.Field"
   "java.lang.reflect.Member" "java.lang.ClassLoader" "java.text.DecimalFormatSymbols" "java.lang.Enum"])

(def translated-classes
  "The top-level classes c2g translates from the JDK closure (g2c.jrt-sources: the measured
  closure, the files added to it, jrt's own Java), by binary name."
  (delay
    (set (concat
           (for [s (jrt-sources/sources "doc/go/java-surface.edn")]
             (-> s (str/replace #"^src/java\.base/share/classes/|^build/[^/]+/support/gensrc/java\.base/" "")
                 (str/replace #"\.java$" "") (str/replace "/" ".")))
           (for [s (jrt-sources/overlay-sources)]
             (-> s (str/replace #"^java\.base/" "") (str/replace #"\.java$" "") (str/replace "/" ".")))))))

(defn translated? [^Class c]
  (let [n (.getName c) i (.indexOf n "$")]
    (and (not (contains? manifest (symbol n)))
         (contains? @translated-classes (if (neg? i) n (subs n 0 i))))))

(defn in-world?
  "Is type c in the Go program (a member naming another type has no table entry)?"
  [^Class c]
  (cond (.isArray c) (in-world? (.getComponentType c))
        (.isPrimitive c) true
        :else (or (= c Object) (contains? manifest (symbol (.getName c))) (translated? c))))

(defn table-sigs-c2g
  "table-sigs with c2g's translation of the JDK closure in the program: the public methods
  that the translated supertypes of c declare (their member tables, C2G-SPEC §5.11) too."
  [^Class c]
  (let [supers (loop [todo [c] seen #{}]
                 (if (empty? todo)
                   seen
                   (let [x (first todo)]
                     (recur (into (rest todo) (remove nil? (cons (.getSuperclass ^Class x) (.getInterfaces ^Class x))))
                            (conj seen x)))))]
    (into (table-sigs c)
          (for [^Class s supers
                :when (translated? s)
                ^Method m (.getDeclaredMethods s)
                :when (and (Modifier/isPublic (.getModifiers m))
                           (every? in-world? (cons (.getReturnType m) (.getParameterTypes m))))]
            (str (.getName m) (desc m))))))

(defn method-lines
  "methods: getMethods of jrt's own build (stand-ins, the hand-written tables); methods-c2g:
  the same with c2g's translated JDK classes (bin/jrt test --prog), whose tables add the
  translated supertypes' methods (bridges such as compareTo(Object) through Comparable)."
  []
  (for [[kind sigs-of] [["methods" table-sigs] ["methods-c2g" table-sigs-c2g]]
        n method-classes
        :let [c (class-of n)
              ok (sigs-of c)
              ms (sort (distinct (for [^Method m (.getMethods c)
                                       :let [s (sig m)]
                                       :when (ok (first (str/split s #" ")))]
                                   s)))]]
    (line kind n (str/join "|" ms))))

(def lookups
  ;; [class name params]
  [["java.lang.String" "charAt" "int"] ["java.lang.String" "valueOf" "java.lang.Object"]
   ["java.lang.String" "valueOf" "[C"] ["java.lang.String" "valueOf" "int"] ["java.lang.String" "compareTo" "java.lang.String"]
   ["java.lang.String" "equals" "java.lang.Object"] ["java.lang.String" "hashCode" ""]
   ["java.lang.String" "getClass" ""] ["java.lang.String" "wait" "long"] ["java.lang.String" "notify" ""]
   ["java.lang.String" "isEmpty" ""] ["java.lang.String" "nope" "int,java.lang.String"]
   ["java.lang.String" "charAt" "long"] ["java.lang.String" "join" "java.lang.CharSequence,[Ljava.lang.CharSequence;"]
   ["java.lang.Math" "abs" "int"] ["java.lang.Math" "abs" "long"] ["java.lang.Math" "max" "double,double"]
   ["java.lang.Math" "toString" ""] ["java.lang.Class" "getName" ""] ["java.lang.Class" "forName" "java.lang.String"]
   ["java.lang.Class" "getMethods" ""] ["java.lang.StringBuilder" "append" "java.lang.String"]
   ["java.lang.StringBuilder" "append" "char"] ["java.lang.StringBuilder" "length" ""]
   ["java.lang.Throwable" "getMessage" ""] ["java.lang.Throwable" "printStackTrace" ""]
   ["java.util.Locale" "getDefault" ""] ["java.util.Locale$Category" "values" ""]
   ["java.util.Locale$Category" "name" ""] ["java.util.Locale$Category" "compareTo" "java.lang.Enum"]
   ["java.util.Date" "getTime" ""] ["java.util.Date" "compareTo" "java.util.Date"]
   ["java.util.Date" "UTC" "int,int,int,int,int,int"]
   ["java.nio.charset.Charset" "forName" "java.lang.String"] ["java.nio.charset.Charset" "name" ""]
   ["sun.nio.cs.UTF_8" "name" ""] ["java.lang.reflect.Method" "invoke" "java.lang.Object,[Ljava.lang.Object;"]
   ["java.lang.reflect.Method" "getParameterTypes" ""] ["java.lang.reflect.Member" "getName" ""]])

(defn lookup-lines []
  (concat
    (for [[c n ps] lookups]
      (line "getMethod" c n ps (outcome #(str (.getMethod (class-of c) n (params-of ps))))))
    (for [[c ps] [["java.lang.String" "java.lang.String"] ["java.lang.String" "[C"] ["java.lang.String" "int"]
                  ["java.lang.StringBuilder" "int"] ["java.lang.StringBuilder" ""] ["java.util.Date" "long"]
                  ["java.lang.Object" ""] ["java.lang.Math" ""] ["java.lang.Throwable" "java.lang.String"]]]
      (line "getConstructor" c ps (outcome #(str (.getConstructor (class-of c) (params-of ps))))))
    (for [[c f] [["java.lang.Math" "PI"] ["java.lang.Math" "E"] ["java.lang.Math" "nope"]
                 ["java.util.Locale" "US"] ["java.util.Locale$Category" "FORMAT"] ["sun.nio.cs.UTF_8" "INSTANCE"]]]
      (line "getField" c f (outcome #(str (.getField (class-of c) f)))))
    (for [c ["java.lang.Math" "java.util.Locale" "java.lang.String" "java.nio.charset.StandardCharsets" "java.lang.Object"]]
      (line "fields" c (str/join "|" (sort (for [^Field f (.getFields (class-of c))
                                                 :when (contains? (set (for [[k n] (get-in manifest [(symbol (.getName (.getDeclaringClass f))) :members])
                                                                             :when (= k :field)] n))
                                                                  (.getName f))]
                                             (str f))))))
    (for [c ["java.lang.String" "java.lang.StringBuilder" "java.util.Date" "java.lang.Math" "java.lang.Object"]]
      (line "constructors" c
            (str/join "|" (sort (for [k (.getConstructors (class-of c))
                                      :when (or (= c "java.lang.Object")
                                                (contains? (set (for [[kind d] (get-in manifest [(symbol c) :members]) :when (= kind :ctor)] d))
                                                           (.toMethodDescriptorString (java.lang.invoke.MethodType/methodType Void/TYPE (.getParameterTypes ^java.lang.reflect.Constructor k)))))]
                                  (str k))))))))

(def invokes
  ;; [class name params receiver args]
  [["java.lang.String" "charAt" "int" "s:abc" "i:1"]
   ["java.lang.String" "charAt" "int" "s:abc" "j:1"]
   ["java.lang.String" "charAt" "int" "s:abc" "h:1"]
   ["java.lang.String" "charAt" "int" "s:abc" "b:2"]
   ["java.lang.String" "charAt" "int" "s:abc" "c:1"]
   ["java.lang.String" "charAt" "int" "s:abc" "z:true"]
   ["java.lang.String" "charAt" "int" "s:abc" ""]
   ["java.lang.String" "charAt" "int" "s:abc" "i:1 i:2"]
   ["java.lang.String" "charAt" "int" "j:42" "i:1"]
   ["java.lang.String" "charAt" "int" "s:abc" "i:7"]
   ["java.lang.String" "valueOf" "java.lang.Object" "n" "i:42"]
   ["java.lang.String" "valueOf" "java.lang.Object" "j:42" "n"]
   ["java.lang.String" "valueOf" "[C" "n" "ca:xyz"]
   ["java.lang.String" "valueOf" "double" "n" "d:2.5"]
   ["java.lang.String" "valueOf" "double" "n" "i:3"]
   ["java.lang.String" "valueOf" "char" "n" "i:65"]
   ["java.lang.String" "valueOf" "boolean" "n" "z:true"]
   ["java.lang.String" "indexOf" "java.lang.String" "s:abc" "i:42"]
   ["java.lang.String" "indexOf" "java.lang.String" "s:abc" "s:c"]
   ["java.lang.String" "isEmpty" "" "s:" ""]
   ["java.lang.String" "toCharArray" "" "s:hey" ""]
   ["java.lang.String" "hashCode" "" "s:abc" ""]
   ["java.lang.String" "equals" "java.lang.Object" "s:abc" "s:abc"]
   ["java.lang.String" "compareTo" "java.lang.String" "s:abc" "s:abd"]
   ["java.lang.Object" "toString" "" "s:abc" ""]
   ["java.lang.Object" "getClass" "" "s:abc" ""]
   ["java.lang.Object" "hashCode" "" "s:abc" ""]
   ["java.lang.Object" "equals" "java.lang.Object" "L:US" "L:US"]
   ["java.lang.Object" "notify" "" "s:abc" ""]
   ["java.lang.Math" "abs" "long" "n" "i:-3"]
   ["java.lang.Math" "abs" "long" "n" "b:-3"]
   ["java.lang.Math" "abs" "long" "n" "c:3"]
   ["java.lang.Math" "abs" "long" "n" "d:3.0"]
   ["java.lang.Math" "abs" "int" "n" "j:-3"]
   ["java.lang.Math" "max" "double,double" "n" "i:1 d:2.5"]
   ["java.lang.Math" "max" "double,double" "s:ignored" "j:7 h:2"]
   ["java.lang.Math" "floorMod" "int,int" "n" "i:-7 i:3"]
   ["java.lang.Math" "addExact" "int,int" "n" "i:2147483647 i:1"]
   ["java.lang.Math" "sqrt" "double" "n" "d:2.0"]
   ["java.lang.Class" "getName" "" "C:java.lang.String" ""]
   ["java.lang.Class" "getName" "" "C:[I" ""]
   ["java.lang.Class" "getSimpleName" "" "C:java.util.Locale$Category" ""]
   ["java.lang.Class" "isInstance" "java.lang.Object" "C:java.lang.CharSequence" "s:x"]
   ["java.lang.Class" "forName" "java.lang.String" "n" "s:java.util.Date"]
   ["java.lang.Class" "forName" "java.lang.String" "n" "s:no.Such"]
   ["java.lang.Class" "getSuperclass" "" "C:sun.nio.cs.UTF_8" ""]
   ["java.lang.StringBuilder" "append" "java.lang.String" "n" "s:x"]
   ["java.lang.Throwable" "getMessage" "" "s:abc" ""]
   ["java.util.Locale" "getDefault" "" "n" ""]
   ["java.util.Locale" "toString" "" "L:US" ""]
   ["java.util.Locale" "getLanguage" "" "L:ENGLISH" ""]
   ["java.util.Locale" "toLanguageTag" "" "L:ROOT" ""]
   ["java.util.Date" "getTime" "" "n" ""]
   ["java.util.Date" "UTC" "int,int,int,int,int,int" "n" "i:70 i:0 i:1 i:0 i:0 i:0"]
   ["java.nio.charset.Charset" "forName" "java.lang.String" "n" "s:latin1"]
   ["java.nio.charset.Charset" "forName" "java.lang.String" "n" "s:EBCDIC"]
   ["java.nio.charset.Charset" "name" "" "cs:UTF8" ""]
   ["java.nio.charset.Charset" "compareTo" "java.nio.charset.Charset" "cs:UTF-8" "cs:US-ASCII"]
   ["java.lang.reflect.Array" "getLength" "java.lang.Object" "n" "ia:1,2,3"]
   ["java.lang.reflect.Array" "get" "java.lang.Object,int" "n" "ia:1,2,3 i:1"]
   ["java.lang.reflect.Array" "get" "java.lang.Object,int" "n" "ia:1,2,3 i:3"]])

(defn invoke-lines []
  (for [[c n ps recv as] invokes]
    (line "invoke" c n ps recv as
          (outcome #(.invoke (.getMethod (class-of c) n (params-of ps)) (value recv) (args-of as))))))

(def news
  [["java.lang.String" "java.lang.String" "s:x"] ["java.lang.String" "java.lang.String" "i:42"]
   ["java.lang.String" "java.lang.String" ""] ["java.lang.String" "[C" "ca:abc"]
   ["java.lang.StringBuilder" "int" "i:-1"] ["java.lang.StringBuilder" "java.lang.String" "s:q"]
   ["java.util.Date" "long" "j:0"] ["java.util.Date" "long" "i:1000"] ["java.lang.Object" "" ""]
   ["java.lang.Throwable" "java.lang.String" "s:boom"]])

(defn new-lines []
  (for [[c ps as] news]
    (line "newInstance" c ps as
          (outcome #(let [v (.newInstance (.getConstructor (class-of c) (params-of ps)) (args-of as))]
                      (if (= c "java.lang.Object") (.getName (class v)) v))))))

(defn field-lines []
  (concat
    (for [[c f recv] [["java.lang.Math" "PI" "n"] ["java.lang.Math" "E" "s:ignored"] ["java.util.Locale" "ROOT" "n"]
                      ["java.nio.charset.StandardCharsets" "UTF_8" "n"] ["java.util.Locale$Category" "DISPLAY" "n"]]]
      (line "fieldGet" c f recv (outcome #(.get (.getField (class-of c) f) (value recv)))))
    (for [[c f v] [["java.lang.Math" "PI" "d:3.0"] ["java.lang.Math" "PI" "n"] ["java.util.Locale" "US" "L:ROOT"]]]
      (line "fieldSet" c f v (outcome #(.set (.getField (class-of c) f) nil (value v)))))))

(defn class-lines []
  (for [n ["java.lang.String" "[I" "[[Ljava.lang.String;" "int" "java.util.Locale$Category" "sun.nio.cs.UTF_8"
           "java.lang.reflect.Method" "[Ljava.util.Locale$Category;" "java.lang.Object"]
        :let [c (class-of n)]
        [op f] [["typeName" #(.getTypeName c)] ["canonicalName" #(.getCanonicalName c)]
                ["packageName" #(.getPackageName c)] ["simpleName" #(.getSimpleName c)]
                ["declaringClass" #(some-> (.getDeclaringClass c) .getName)]
                ["modifiers" #(.getModifiers c)] ["superclass" #(some-> (.getSuperclass c) .getName)]
                ["classLoaderNull" #(nil? (.getClassLoader c))]
                ["enumConstants" #(some->> (.getEnumConstants c) (map str) (str/join ","))]]]
    (line op n (outcome f))))

(defn array-lines []
  (let [cases [["get int[] 1" #(Array/get (int-array [5 6 7]) 1)]
               ["get char[] 0" #(Array/get (char-array "q") 0)]
               ["get oob" #(Array/get (int-array 3) 5)]
               ["get neg" #(Array/get (object-array 3) -1)]
               ["get notarray" #(Array/get "x" 0)]
               ["get null" #(Array/get nil 0)]
               ["set int null" #(Array/set (int-array 3) 0 nil)]
               ["set int short" #(let [a (int-array 3)] (Array/set a 0 (short 5)) (str/join "," a))]
               ["set int long" #(Array/set (int-array 3) 0 (long 5))]
               ["set int oob mismatch" #(Array/set (int-array 3) 5 "x")]
               ["set str oob mismatch" #(Array/set (make-array String 3) 5 1)]
               ["set str mismatch" #(Array/set (make-array String 3) 0 1)]
               ["set obj short" #(let [a (object-array 3)] (Array/set a 0 (short 5)) (.getName (class (aget a 0))))]
               ["set notarray" #(Array/set "x" 0 1)]
               ["setInt long[]" #(let [a (long-array 3)] (Array/setInt a 0 5) (str/join "," a))]
               ["setInt notarray" #(Array/setInt "x" 0 1)]
               ["setInt objarray" #(Array/setInt (object-array 3) 0 1)]
               ["setLong int[]" #(Array/setLong (int-array 3) 0 5)]
               ["setChar int[]" #(let [a (int-array 3)] (Array/setChar a 1 \a) (str/join "," a))]
               ["setByte char[]" #(Array/setByte (char-array 3) 0 (byte 1))]
               ["setBoolean int[]" #(Array/setBoolean (int-array 3) 0 true)]
               ["setDouble float[]" #(Array/setDouble (float-array 3) 0 1.0)]
               ["setFloat double[]" #(let [a (double-array 1)] (Array/setFloat a 0 (float 0.1)) (str/join "," a))]
               ["getInt long[]" #(Array/getInt (long-array 3) 0)]
               ["getLong int[]" #(Array/getLong (int-array [9]) 0)]
               ["getDouble char[]" #(Array/getDouble (char-array "A") 0)]
               ["getInt obj[]" #(Array/getInt (object-array 3) 0)]
               ["getLength int[]" #(Array/getLength (int-array 4))]
               ["getLength null" #(Array/getLength nil)]
               ["getLength notarray" #(Array/getLength "x")]
               ["newInstance void" #(Array/newInstance Void/TYPE 3)]
               ["newInstance -1" #(Array/newInstance String -1)]
               ["newInstance dims empty" #(Array/newInstance Integer/TYPE (int-array []))]
               ["newInstance dims void" #(Array/newInstance Void/TYPE (int-array [1]))]
               ["newInstance dims neg" #(Array/newInstance Integer/TYPE (int-array [2 -1]))]
               ["newInstance dims 256" #(Array/newInstance Integer/TYPE (int-array 256 1))]
               ["newInstance dims 2x3" #(let [a (Array/newInstance Integer/TYPE (int-array [2 3]))]
                                          (str (.getName (class a)) " " (Array/getLength a) " " (Array/getLength (Array/get a 0))))]
               ["newInstance dims String 2" #(.getName (class (Array/newInstance String (int-array [2]))))]]]
    (for [[n f] cases]
      (line "array" n (outcome f)))))

;; ---------------------------------------------------------------------------------------
;; Charsets

(def charset-names
  ["UTF-8" "utf8" "UTF8" "unicode-1-1-utf-8" "ISO-8859-1" "latin1" "L1" "iso-ir-100" "cp819" "8859_1"
   "US-ASCII" "ascii" "us" "646" "iso_646.irv:1983" "default" "UTF-16" "EBCDIC" "" "-utf8" "utf 8" "a.b:c+d_e"
   ;; step 5 phase 2B
   "UTF-16BE" "UTF-16LE" "utf16" "UnicodeBigUnmarked" "X-UTF-16LE" "unicode"])

(def charsets
  "jrt's charsets, by canonical name."
  ["UTF-8" "ISO-8859-1" "US-ASCII" "UTF-16" "UTF-16BE" "UTF-16LE"])

(def utf16-byte-cases
  [[0xFE 0xFF 0 0x61] [0xFF 0xFE 0x61 0] [0 0x61 0] [0xD8 0 0 0x61] [0xDC 0 0 0x61] [0xFF 0xFE] [0x61]
   [0xD8 0x3D 0xDE 0] [0xFF 0xFE 0xFF 0xFE] [0 0x61 0xFE 0xFF] [0 0x61 0xFF 0xFE] [0xD8 0x3D] [0xD8 0x3D 0xDE]
   [0xDE 0 0xD8 0x3D] [0 0x61 0xD8 0x3D 0xDE 0 0 0x62]])

(def byte-cases
  [[] [0x41 0x42] [0xC3 0xA9] [0xE2 0x82 0xAC] [0xF0 0x9F 0x98 0x80] [0xFF] [0x80 0x41] [0xC3]
   [0xE2 0x82] [0xED 0xA0 0x80] [0xF4 0x90 0x80 0x80] [0xC0 0x80] [0x7F 0x80 0xFF]])

(defn charset-lines []
  (concat
    (for [n charset-names
          [op f] [["forName" #(.name (Charset/forName n))] ["isSupported" #(Charset/isSupported n)]]]
      (line op (esc n) (outcome f)))
    (line "forNameNull" (outcome #(Charset/forName nil)))
    (for [n charsets
          :let [cs (Charset/forName n)]
          [op f] [["toString" #(str cs)] ["hashCode" #(.hashCode cs)] ["aliases" #(str/join "," (sort (.aliases cs)))]
                  ["canEncode" #(.canEncode cs)] ["isRegistered" #(.isRegistered cs)]
                  ["containsUTF8" #(.contains cs (Charset/forName "UTF-8"))]
                  ["containsASCII" #(.contains cs (Charset/forName "US-ASCII"))]
                  ["containsLatin1" #(.contains cs (Charset/forName "ISO-8859-1"))]
                  ["containsUTF16" #(.contains cs (Charset/forName "UTF-16"))]
                  ["compareToUTF8" #(.compareTo cs (Charset/forName "UTF-8"))]
                  ["equalsUTF8" #(.equals cs (Charset/forName "UTF8"))]
                  ["class" #(.getName (class cs))]]]
      (line op n (outcome f)))
    (for [n charsets
          s ["" "abc" "café" "€uro" "😀" "lone \ud800 x" "\udc00" "ÿĀ" "x\u0000y" "\ud800\ud800" "a\udc00b" "\ud800"]]
      (line "encode" n (esc s) (outcome #(str/join "," (map (fn [b] (bit-and b 0xff)) (.getBytes ^String s (Charset/forName n)))))))
    (for [n charsets
          bs (concat byte-cases utf16-byte-cases)]
      (line "decode" n (str/join "," bs) (outcome #(String. (byte-array (map unchecked-byte bs)) (Charset/forName n)))))
    (for [n ["UTF-8" "ISO-8859-1"]]
      (line "decodeRange" n (outcome #(String. (byte-array (map unchecked-byte [0x61 0xC3 0xA9 0x62])) 1 2 (Charset/forName n)))))
    [(line "decodeRangeBad" (outcome #(String. (byte-array 3) 2 5 (Charset/forName "UTF-8"))))
     (line "defaultCharset" (outcome #(.name (Charset/defaultCharset))))]))

;; ---------------------------------------------------------------------------------------
;; Locales

(defn locale-lines []
  (concat
    (for [[n l] [["ROOT" Locale/ROOT] ["US" Locale/US] ["ENGLISH" Locale/ENGLISH] ["UK" Locale/UK]
                 ["of:fr" (Locale/of "fr")] ["of:EN,us" (Locale/of "EN" "us")] ["of:en,US,POSIX" (Locale/of "en" "US" "POSIX")]
                 ["of:,US" (Locale/of "" "US")] ["of:,,V" (Locale/of "" "" "V")] ["of:de,,V" (Locale/of "de" "" "V")]]
          [op f] [["toString" #(str l)] ["hashCode" #(.hashCode l)] ["toLanguageTag" #(.toLanguageTag l)]
                  ["language" #(.getLanguage l)] ["country" #(.getCountry l)] ["variant" #(.getVariant l)]
                  ["equalsUS" #(.equals l Locale/US)]]]
      (line op n (outcome f)))
    [(line "default" (outcome #(str (Locale/getDefault))))
     (line "defaultFormat" (outcome #(str (Locale/getDefault java.util.Locale$Category/FORMAT))))
     (line "categories" (outcome #(str/join "," (map str (java.util.Locale$Category/values)))))]
    (for [[n l] [["ROOT" Locale/ROOT] ["US" Locale/US]]
          :let [d (java.text.DecimalFormatSymbols/getInstance l)]
          [op f] [["zero" #(int (.getZeroDigit d))] ["grouping" #(int (.getGroupingSeparator d))]
                  ["decimal" #(int (.getDecimalSeparator d))] ["minus" #(int (.getMinusSign d))]
                  ["percent" #(int (.getPercent d))] ["perMill" #(int (.getPerMill d))]
                  ["infinity" #(.getInfinity d)] ["nan" #(.getNaN d)] ["exponent" #(.getExponentSeparator d)]
                  ["currency" #(.getCurrencySymbol d)] ["intlCurrency" #(.getInternationalCurrencySymbol d)]
                  ["locale" #(str (.getLocale d))]]]
      (line (str "dfs." op) n (outcome f)))
    (for [s ["TITLE" "title" "İstanbul" "ıi"]
          [n l] [["ROOT" Locale/ROOT] ["US" Locale/US]]
          [op f] [["lower" #(.toLowerCase ^String s ^Locale l)] ["upper" #(.toUpperCase ^String s ^Locale l)]]]
      (line op n (esc s) (outcome f)))))

;; ---------------------------------------------------------------------------------------
;; Dates (the JVM's default time zone set to GMT, as jrt's)

(def cutover -12219292800000)

(defn instants []
  (let [r (Random. 20261008)
        fixed [0 -1 1 999 1000 -1000 86399999 86400000 -86400000 1234567890123 -1234567890123
               cutover (dec cutover) (- cutover 86400000) (+ cutover 86400000) (- cutover (* 10 86400000))
               -62135596800000 -62135596800001 -62167219200000 -62198755200000 -62198755200001
               -77914137600000 253402300799999 253402300800000 32503680000000 -30610224000000
               951782400000 951868800000 4102444800000 -2208988800000 -12219292800001 -12220156800000
               -8640000000000000 8640000000000000 -9223372036854775808 9223372036854775807]]
    (concat fixed
            (repeatedly 400 #(- (long (* (.nextDouble r) 2.6e14)) 130000000000000))
            (repeatedly 100 #(.nextLong r)))))

(defn date-lines []
  (concat
    (for [ms (instants)
          :let [d (Date. (long ms))]]
      (line "date" (str ms)
            (outcome #(str d)) (outcome #(.toGMTString d))
            (outcome #(str/join "," [(.getYear d) (.getMonth d) (.getDate d) (.getDay d) (.getHours d) (.getMinutes d) (.getSeconds d)]))
            (outcome #(.hashCode d))
            (outcome #(pr-str d))))
    (let [r (Random. 1582)]
      (for [[y m d h mi s] (concat
                             ;; years as written; Date.UTC takes year - 1900
                             [[1970 0 1 0 0 0] [0 0 1 0 0 0] [-1 0 1 0 0 0] [1582 9 15 0 0 0]
                              [1582 9 14 0 0 0] [1582 9 4 0 0 0] [1582 9 5 0 0 0] [1582 9 10 12 0 0]
                              [1582 0 1 0 0 0] [1582 11 31 23 59 59] [1581 11 31 0 0 0] [1581 11 400 0 0 0]
                              [1583 0 1 0 0 0] [1583 0 -100 0 0 0] [2000 13 1 0 0 0] [2000 -1 1 0 0 0]
                              [2000 0 0 0 0 0] [2000 1 30 25 61 61] [0 1 29 0 0 0] [100 1 29 0 0 0]
                              [9999 11 31 23 59 59] [-2000 0 1 0 0 0] [1999 1 29 0 0 0] [2000 1 29 0 0 0]
                              [1000 1 29 0 0 0] [1700 1 29 0 0 0] [1970 0 1 -1 0 0] [1970 0 1 0 0 -1]
                              [1970 0 1 0 -60 0] [1582 9 5 -24 0 0] [1582 9 15 0 0 -1] [1581 11 31 0 0 86400]]
                             (for [_ (range 300)]
                               [(- (.nextInt r 4000) 1500) (- (.nextInt r 30) 6) (- (.nextInt r 70) 15)
                                (- (.nextInt r 40) 8) (- (.nextInt r 120) 30) (- (.nextInt r 120) 30)]))
            :let [y (- y 1900)]]
        (line "utc" (str/join "," [y m d h mi s]) (outcome #(Date/UTC y m d h mi s)))))
    (for [s ["1970-01-01T00:00:00Z" "2026-10-08T12:34:56.789Z" "1582-10-15" "1582-10-14" "1582-10-04"
             "1582-10-05T12:00" "0001-01-01" "0000-01-01T00:00:00.000-00:00" "1000-02-29" "1900-02-28T23:59:60"
             "9999-12-31T23:59:59.999999999Z" "2000-01-01T00:00:00+14:00" "2000-01-01T00:00:00-12:30"
             "1500-06-15T06:07:08.5+01:00" "2016-12-31T23:59:60Z"]]
      (line "readInst" (esc s) (outcome #(.getTime ^Date (inst/read-instant-date s)))))))

(defn write-all [dir]
  (let [tz (TimeZone/getDefault)]
    (TimeZone/setDefault (TimeZone/getTimeZone "GMT"))
    (try
      (write dir "reflect.txt" (concat (method-lines) (lookup-lines) (invoke-lines) (new-lines) (field-lines)
                                       (class-lines) (array-lines)))
      (write dir "charsets.txt" (charset-lines))
      (write dir "locales.txt" (locale-lines))
      (write dir "dates.txt" (date-lines))
      (finally (TimeZone/setDefault tz)))))
