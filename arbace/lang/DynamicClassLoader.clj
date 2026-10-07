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
;; /* rich Aug 21, 2007 */
;;
;; Converted from clojure/lang/DynamicClassLoader.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(java.lang.ref Reference ReferenceQueue SoftReference)
        '(java.net URL URLClassLoader)
        '(java.util HashMap)
        '(java.util.concurrent ConcurrentHashMap))

(defclass ^:public DynamicClassLoader
  :extends URLClassLoader

  (field ^{:tag (HashMap Integer Object/1)} constantVals (HashMap.))

  (field ^:static ^{:tag (ConcurrentHashMap String (Reference Class))} classCache
    (ConcurrentHashMap.))

  (field ^:static ^:final ^URL/1 EMPTY_URLS (new URL/1 []))

  (field ^:static ^:final ^ReferenceQueue rq (ReferenceQueue.))

  (constructor ^:public [this]
    (super. EMPTY_URLS
            (if (or (nil? (.getContextClassLoader (Thread/currentThread)))
                    (identical? (.getContextClassLoader (Thread/currentThread))
                                (ClassLoader/getSystemClassLoader)))
                (.getClassLoader arbace.lang.Compiler)
                (.getContextClassLoader (Thread/currentThread)))))

  (constructor ^:public [this ^ClassLoader parent]
    (super. EMPTY_URLS parent))

  (method ^:public defineClass ^Class [this ^String name ^byte/1 bytes srcForm]
    (Util/clearCache rq classCache)
    (let [c (.defineClass this name bytes 0 (alength bytes))]
      (.put classCache name (SoftReference. c rq))
      c))

  (method ^:static findInMemoryClass ^{:tag (Class ?)} [^String name]
    (let [^{:tag (Reference Class)} cr (cast Reference (.get classCache name))]
      (when (some? cr)
        (let [c (cast Class (.get cr))] (if (some? c) (return c) (.remove classCache name cr))))
      nil))

  (method ^:protected findClass :throws [ClassNotFoundException] ^{:tag (Class ?)} [this
                                                                                    ^String name]
    (let [c (DynamicClassLoader/findInMemoryClass name)] (if (some? c) c (.findClass super name))))

  (method ^:protected ^:synchronized loadClass :throws [ClassNotFoundException] ^{:tag (Class ?)} [this
                                                                                                   ^String name
                                                                                                   ^boolean resolve]
    (let [^:mutable c (.findLoadedClass this name)]
      (when (nil? c)
        (set! c (DynamicClassLoader/findInMemoryClass name))
        (when (nil? c) (set! c (.loadClass super name false))))
      (when resolve (.resolveClass this c))
      c))

  (method ^:public registerConstants ^void [this ^int id ^Object/1 val]
    (.put constantVals id val))

  (method ^:public getConstants ^Object/1 [this ^int id]
    (cast Object/1 (.get constantVals id)))

  (method ^:public addURL ^void [this ^URL url] (.addURL super url)))
