;; jrt: java.lang.ClassLoader, minimal (JRT-SOURCES.md, "rework"; C2G-SPEC §10.3, §12): there is
;; no class loading, only the registry of the closed world. The application and platform
;; loaders exist as objects so that Class.getClassLoader, ClassLoader.getSystemClassLoader
;; (ForkJoinWorkerThread) and Arbace's RT.baseLoader have values; loadClass looks names up in
;; the registry. Also jdk.internal.foreign.Utils.checkNonNegativeArgument (List.of's check).
(in-ns 'go.arbace.jrt)

(go/file "classloader.go")

(go/type ClassLoader_I
  "ClassLoader_I is java.lang.ClassLoader's class interface.\n"
  (interface Object_I
    (Self_ClassLoader ^{:tag (* ClassLoader)} [])
    (LoadClass_String__Class ^{:tag (* Class)} [^{:tag (* String)} name])
    (LoadClass_String_Z__Class ^{:tag (* Class)} [^{:tag (* String)} name ^bool resolve])
    (GetParent__ClassLoader ^ClassLoader_I [])
    (GetName__String ^{:tag (* String)} [])))

(go/type ClassLoader
  "ClassLoader is java.lang.ClassLoader (abstract): a parent and a name.\n"
  (struct Object ^ClassLoader_I parent ^{:tag (* String)} name))

(go/var ClassLoader_class
  (Define (addr (lit ClassInfo :Name "java.lang.ClassLoader" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccAbstract) :Super Object_class
                     :Go "arbace/jrt.ClassLoader"))))

(go/method Ctor "Ctor is ClassLoader(): the system class loader as parent.\n"
  [^{:tag (* ClassLoader)} t ^ClassLoader_I this]
  (set! (.-parent t) appLoader))
(go/method Ctor_ClassLoader "Ctor_ClassLoader is ClassLoader(ClassLoader parent).\n"
  [^{:tag (* ClassLoader)} t ^ClassLoader_I this ^ClassLoader_I parent]
  (set! (.-parent t) parent))

(go/method Self_ClassLoader ^{:tag (* ClassLoader)} [^{:tag (* ClassLoader)} t] t)
(go/method GetParent__ClassLoader ^ClassLoader_I [^{:tag (* ClassLoader)} t] (.-parent t))
(go/method GetName__String ^{:tag (* String)} [^{:tag (* ClassLoader)} t] (.-name t))

(go/method LoadClass_String__Class
  "LoadClass_String__Class is ClassLoader.loadClass: the registry's class, not initialized;
ClassNotFoundException otherwise.\n"
  ^{:tag (* Class)} [^{:tag (* ClassLoader)} t ^{:tag (* String)} name]
  (Class_ForName_String_Z_ClassLoader__Class name false nil))

(go/method LoadClass_String_Z__Class ^{:tag (* Class)}
  [^{:tag (* ClassLoader)} t ^{:tag (* String)} name ^bool resolve]
  (Class_ForName_String_Z_ClassLoader__Class name false nil))

(go/func ClassLoader_InstanceOf ^bool [^any x] (let [(values _ ok) (assert ClassLoader_I x)] ok))
(go/func ClassLoader_Cast ^ClassLoader_I [^any x]
  (when (== x nil) (return nil))
  (let [(values v ok) (assert ClassLoader_I x)]
    (when (not ok) (panic (ClassCast x ClassLoader_class)))
    v))

;; the two built-in loaders (jdk.internal.loader.ClassLoaders' classes)

(go/type builtinLoader (struct ClassLoader ^{:tag (* Class)} cls))
(go/method Ref ^any [^{:tag (* builtinLoader)} t] (when (== t nil) (return nil)) t)
(go/method GetClass__Class ^{:tag (* Class)} [^{:tag (* builtinLoader)} t] (.-cls t))
(go/method ToString__String ^{:tag (* String)} [^{:tag (* builtinLoader)} t] (Object_toString t))
(go/method Clone__O ^any [^{:tag (* builtinLoader)} t] (panic (CloneNotSupported t)))

(go/var
  [builtinLoader_class
   (Define (addr (lit ClassInfo :Name "jdk.internal.loader.BuiltinClassLoader" :Kind KindClass
                      :Modifiers AccPublic :Super ClassLoader_class :Go "arbace/jrt.builtinLoader")))]
  [platformLoader_class
   (Define (addr (lit ClassInfo :Name "jdk.internal.loader.ClassLoaders$PlatformClassLoader" :Kind KindClass
                      :Modifiers (bit-or AccPrivate AccStatic) :Super builtinLoader_class)))]
  [appLoader_class
   (Define (addr (lit ClassInfo :Name "jdk.internal.loader.ClassLoaders$AppClassLoader" :Kind KindClass
                      :Modifiers (bit-or AccPrivate AccStatic) :Super builtinLoader_class)))])

(go/var
  [^{:tag (* builtinLoader)} platformLoader
   (addr (lit builtinLoader :ClassLoader (lit ClassLoader :name (Intern "platform")) :cls platformLoader_class))]
  [^{:tag (* builtinLoader)} appLoader
   (addr (lit builtinLoader :ClassLoader (lit ClassLoader :parent platformLoader :name (Intern "app"))
              :cls appLoader_class))])

(go/func ClassLoader_GetSystemClassLoader__ClassLoader
  "ClassLoader_GetSystemClassLoader__ClassLoader is ClassLoader.getSystemClassLoader: the
application loader.\n"
  ^ClassLoader_I []
  appLoader)

(go/func ClassLoader_GetPlatformClassLoader__ClassLoader ^ClassLoader_I []
  platformLoader)

;; ---------------------------------------------------------------------------------------
;; jdk.internal.foreign.Utils

(go/func Utils_CheckNonNegativeArgument_J_String__V
  "Utils_CheckNonNegativeArgument_J_String__V is jdk.internal.foreign.Utils.
checkNonNegativeArgument: IllegalArgumentException(\"The provided NAME is negative: V\").\n"
  [^int64 value ^{:tag (* String)} name]
  (when (< value 0)
    (panic (IllegalArgumentException_New_String
             (Concat (Str "The provided ") name (Str " is negative: ") (StrOfLong value))))))
