;; // ASM: a very small and fast Java bytecode manipulation framework
;; // Copyright (c) 2000-2011 INRIA, France Telecom
;; // All rights reserved.
;; //
;; // Redistribution and use in source and binary forms, with or without
;; // modification, are permitted provided that the following conditions
;; // are met:
;; // 1. Redistributions of source code must retain the above copyright
;; //    notice, this list of conditions and the following disclaimer.
;; // 2. Redistributions in binary form must reproduce the above copyright
;; //    notice, this list of conditions and the following disclaimer in the
;; //    documentation and/or other materials provided with the distribution.
;; // 3. Neither the name of the copyright holders nor the names of its
;; //    contributors may be used to endorse or promote products derived from
;; //    this software without specific prior written permission.
;; //
;; // THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
;; // AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
;; // IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
;; // ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
;; // LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
;; // CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
;; // SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
;; // INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
;; // CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
;; // ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
;; // THE POSSIBILITY OF SUCH DAMAGE.
;;
;; Converted from clojure/asm/Handle.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ^:final Handle
  (field ^:private ^:final ^int tag)

  (field ^:private ^:final ^String owner)

  (field ^:private ^:final ^String name)

  (field ^:private ^:final ^String descriptor)

  (field ^:private ^:final ^boolean isInterface)

  (constructor ^:public ^{Deprecated {:forRemoval false}} [this ^:final ^int tag
                                                           ^:final ^String owner
                                                           ^:final ^String name
                                                           ^:final ^String descriptor]
    (this. tag owner name descriptor (== tag Opcodes/H_INVOKEINTERFACE)))

  (constructor ^:public [this ^:final ^int tag ^:final ^String owner ^:final ^String name
                         ^:final ^String descriptor ^:final ^boolean isInterface]
    (set! (.-tag this) tag)
    (set! (.-owner this) owner)
    (set! (.-name this) name)
    (set! (.-descriptor this) descriptor)
    (set! (.-isInterface this) isInterface))

  (method ^:public getTag ^int [this] tag)

  (method ^:public getOwner ^String [this] owner)

  (method ^:public getName ^String [this] name)

  (method ^:public getDesc ^String [this] descriptor)

  (method ^:public isInterface ^boolean [this] isInterface)

  (method ^:public equals ^boolean [this ^:final object]
    (cond
      (identical? object this) true
      (not (instance? Handle object)) false
      :else
        (let [handle (cast Handle object)]
          (and (and (and (and (== tag (.-tag handle)) (= isInterface (.-isInterface handle)))
                         (.equals owner (.-owner handle)))
                    (.equals name (.-name handle)))
               (.equals descriptor (.-descriptor handle))))))

  (method ^:public hashCode ^int [this]
    (unchecked-add-int (unchecked-add-int tag (if isInterface 64 0))
                       (unchecked-multiply-int
                         (unchecked-multiply-int (.hashCode owner) (.hashCode name))
                         (.hashCode descriptor))))

  (method ^:public toString ^String [this]
    (java-str owner \. name descriptor " (" tag (if isInterface " itf" "") \))))
