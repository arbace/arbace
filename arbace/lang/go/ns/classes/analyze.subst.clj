;; The Go build (doc/go/CLASSFORMS-REPL.md): ASM's TypeReference and the generic signatures of
;; the classes of the world come from arbace.classes.go; no source path lookup (§9.2) yet
["(:import (arbace.asm Opcodes Type MethodVisitor ClassWriter TypeReference)))"
 "(:require [arbace.classes.go :as TypeReference])\n  (:import (arbace.asm Opcodes Type MethodVisitor ClassWriter)))"

 "\"Whether class names that resolve to nothing are looked up as p/C.clj sources (§9.2).\"\n  true)"
 "\"Whether class names that resolve to nothing are looked up as p/C.clj sources (§9.2).\"\n  false)"

 "      (mapv #(symbol (.getName ^java.lang.reflect.TypeVariable %)) (.getTypeParameters c)))))"
 "      (:tparams (TypeReference/generics n c)))))"

 "    (when-let [c ^Class (env/load-class n)]\n      {:tparams (mapv #(symbol (.getName ^java.lang.reflect.TypeVariable %)) (.getTypeParameters c))"
 "    (when-let [c ^Class (env/load-class n)]\n     (if true (TypeReference/generics n c)\n      {:tparams (mapv #(symbol (.getName ^java.lang.reflect.TypeVariable %)) (.getTypeParameters c))"

 "[(symbol (.getName tv)) (mapv t/reflect->tnode (.getBounds tv))]))})})))"
 "[(symbol (.getName tv)) (mapv t/reflect->tnode (.getBounds tv))]))})}))))"]
