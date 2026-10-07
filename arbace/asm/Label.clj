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
;; Converted from clojure/asm/Label.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public Label
  (field ^:static ^:final ^int FLAG_DEBUG_ONLY 1)

  (field ^:static ^:final ^int FLAG_JUMP_TARGET 2)

  (field ^:static ^:final ^int FLAG_RESOLVED 4)

  (field ^:static ^:final ^int FLAG_REACHABLE 8)

  (field ^:static ^:final ^int FLAG_SUBROUTINE_CALLER 16)

  (field ^:static ^:final ^int FLAG_SUBROUTINE_START 32)

  (field ^:static ^:final ^int FLAG_SUBROUTINE_END 64)

  (field ^:static ^:final ^int FLAG_LINE_NUMBER 128)

  (field ^:static ^:final ^int LINE_NUMBERS_CAPACITY_INCREMENT 4)

  (field ^:static ^:final ^Label EMPTY_LIST (Label.))

  (field ^:public info)

  (field ^short flags)

  (field ^:private ^short lineNumber)

  (field ^:private ^int/1 otherLineNumbers)

  (field ^int bytecodeOffset)

  (field ^:private ^short lastForwardReference)

  (field ^:private ^short lastWideForwardReference)

  (field ^:private ^short lastStackMapForwardReference)

  (field ^short inputStackSize)

  (field ^short outputStackSize)

  (field ^short outputStackMax)

  (field ^short subroutineId)

  (field ^Frame frame)

  (field ^Label nextBasicBlock)

  (field ^Edge outgoingEdges)

  (field ^Label nextListElement)

  (constructor ^:public [this])

  (method ^:public getOffset ^int [this]
    (when (== (bit-and-int flags FLAG_RESOLVED) 0)
      (throw (IllegalStateException. "Label offset position has not been resolved yet")))
    bytecodeOffset)

  (method ^:final getCanonicalInstance ^Label [this]
    (if (nil? frame) this (.-owner frame)))

  (method ^:final addLineNumber ^void [this ^:final ^int lineNumber]
    (if (== (bit-and-int flags FLAG_LINE_NUMBER) 0)
        (do
          (set! flags (unchecked-short (bit-or-int flags FLAG_LINE_NUMBER)))
          (set! (.-lineNumber this) (unchecked-short lineNumber)))
        (do
          (when (nil? otherLineNumbers)
            (set! otherLineNumbers (new int/1 LINE_NUMBERS_CAPACITY_INCREMENT)))
          (let [otherLineNumberIndex (aset otherLineNumbers
                                           0
                                           (unchecked-inc-int (aget otherLineNumbers 0)))]
            (when (>= otherLineNumberIndex (alength otherLineNumbers))
              (let [newLineNumbers (new int/1
                                        (unchecked-add-int
                                          (alength otherLineNumbers)
                                          LINE_NUMBERS_CAPACITY_INCREMENT))]
                (System/arraycopy otherLineNumbers 0 newLineNumbers 0 (alength otherLineNumbers))
                (set! otherLineNumbers newLineNumbers)))
            (aset otherLineNumbers otherLineNumberIndex lineNumber)))))

  (method ^:final accept ^void [this ^:final ^MethodVisitor methodVisitor
                                ^:final ^boolean visitLineNumbers]
    (.visitLabel methodVisitor this)
    (when (and visitLineNumbers (not (== (bit-and-int flags FLAG_LINE_NUMBER) 0)))
      (.visitLineNumber methodVisitor (bit-and-int lineNumber 0xFFFF) this)
      (when (some? otherLineNumbers)
        (loop [^int i 1]
          (when (<= i (aget otherLineNumbers 0))
            (.visitLineNumber methodVisitor (aget otherLineNumbers i) this)
            (recur (unchecked-inc-int i)))))))

  (method ^:final put ^void [this ^:final ^ByteVector code ^:final ^int sourceInsnBytecodeOffset
                             ^:final ^boolean wideReference]
    (cond
      (== (bit-and-int flags FLAG_RESOLVED) 0)
        (let [newLastForwardReference (unchecked-short (.-length code))]
          (if wideReference
              (do
                (.putShort code (bit-and-int lastWideForwardReference 0xFFFF))
                (.putShort code sourceInsnBytecodeOffset)
                (set! lastWideForwardReference newLastForwardReference))
              (do
                (.putShort code (bit-and-int lastForwardReference 0xFFFF))
                (set! lastForwardReference newLastForwardReference))))
      wideReference (.putInt code (unchecked-subtract-int bytecodeOffset sourceInsnBytecodeOffset))
      :else (.putShort code (unchecked-subtract-int bytecodeOffset sourceInsnBytecodeOffset))))

  (method ^:final put ^void [this ^:final ^ByteVector stackMapTableEntries]
    (if (== (bit-and-int flags FLAG_RESOLVED) 0)
        (let [newLastForwardReference (unchecked-short (.-length stackMapTableEntries))]
          (.putShort stackMapTableEntries (bit-and-int lastStackMapForwardReference 0xFFFF))
          (set! lastStackMapForwardReference newLastForwardReference))
        (.putShort stackMapTableEntries bytecodeOffset)))

  (method ^:final resolve ^boolean [this ^:final ^byte/1 code
                                    ^:final ^ByteVector stackMapTableEntries
                                    ^:final ^int bytecodeOffset]
    (set! (.-flags this) (unchecked-short (bit-or-int (.-flags this) FLAG_RESOLVED)))
    (set! (.-bytecodeOffset this) bytecodeOffset)
    (let [^:mutable hasAsmInstructions false
          ^:mutable offset (bit-and-int lastForwardReference 0xFFFF)]
      (while (not (== offset 0))
        (let [previousOffset (bit-or-int
                               (bit-shift-left-int (bit-and-int (aget code offset) 0xFF) 8)
                               (bit-and-int (aget code (unchecked-add-int offset 1)) 0xFF))
              sourceInsnBytecodeOffset (unchecked-subtract-int offset 1)
              relativeOffset (unchecked-subtract-int bytecodeOffset sourceInsnBytecodeOffset)]
          (when (or (< relativeOffset Short/MIN_VALUE) (> relativeOffset Short/MAX_VALUE))
            (let [opcode (bit-and-int (aget code sourceInsnBytecodeOffset) 0xFF)]
              (if (< opcode Opcodes/IFNULL)
                  (aset code
                        sourceInsnBytecodeOffset
                        (unchecked-byte (unchecked-add-int opcode Constants/ASM_OPCODE_DELTA)))
                  (aset code
                        sourceInsnBytecodeOffset
                        (unchecked-byte
                          (unchecked-add-int opcode Constants/ASM_IFNULL_OPCODE_DELTA))))
              (set! hasAsmInstructions true)))
          (aset code offset (unchecked-byte (unsigned-bit-shift-right-int relativeOffset 8)))
          (set! offset (unchecked-inc-int offset))
          (aset code offset (unchecked-byte relativeOffset))
          (set! offset previousOffset)))
      (set! offset (bit-and-int lastWideForwardReference 0xFFFF))
      (while (not (== offset 0))
        (let [previousOffset (bit-or-int
                               (bit-shift-left-int (bit-and-int (aget code offset) 0xFF) 8)
                               (bit-and-int (aget code (unchecked-add-int offset 1)) 0xFF))
              sourceInsnBytecodeOffset (bit-or-int
                                         (bit-shift-left-int
                                           (bit-and-int
                                             (aget code (unchecked-add-int offset 2))
                                             0xFF)
                                           8)
                                         (bit-and-int (aget code (unchecked-add-int offset 3)) 0xFF))
              relativeOffset (unchecked-subtract-int bytecodeOffset sourceInsnBytecodeOffset)]
          (aset code offset (unchecked-byte (unsigned-bit-shift-right-int relativeOffset 24)))
          (set! offset (unchecked-inc-int offset))
          (aset code offset (unchecked-byte (unsigned-bit-shift-right-int relativeOffset 16)))
          (set! offset (unchecked-inc-int offset))
          (aset code offset (unchecked-byte (unsigned-bit-shift-right-int relativeOffset 8)))
          (set! offset (unchecked-inc-int offset))
          (aset code offset (unchecked-byte relativeOffset))
          (set! offset previousOffset)))
      (set! offset (bit-and-int lastStackMapForwardReference 0xFFFF))
      (while (not (== offset 0))
        (let [data (.-data stackMapTableEntries)
              previousOffset (bit-or-int
                               (bit-shift-left-int (bit-and-int (aget data offset) 0xFF) 8)
                               (bit-and-int (aget data (unchecked-add-int offset 1)) 0xFF))]
          (aset data offset (unchecked-byte (unsigned-bit-shift-right-int bytecodeOffset 8)))
          (set! offset (unchecked-inc-int offset))
          (aset data offset (unchecked-byte bytecodeOffset))
          (set! offset previousOffset)))
      (set! lastForwardReference 0)
      (set! lastWideForwardReference 0)
      (set! lastStackMapForwardReference 0)
      hasAsmInstructions))

  (method ^:final markSubroutine ^void [this ^:final ^short subroutineId
                                        ^:final ^Handler firstHandler ^:final ^ComputeLimits limits]
    (let [^:mutable listOfBlocksToProcess this]
      (set! (.-nextListElement listOfBlocksToProcess) EMPTY_LIST)
      (while (not (identical? listOfBlocksToProcess EMPTY_LIST))
        (let [basicBlock listOfBlocksToProcess]
          (set! listOfBlocksToProcess (.-nextListElement listOfBlocksToProcess))
          (set! (.-nextListElement basicBlock) nil)
          (when (== (.-subroutineId basicBlock) 0)
            (set! (.-subroutineId basicBlock) subroutineId)
            (set! listOfBlocksToProcess
                  (.pushSuccessors basicBlock listOfBlocksToProcess firstHandler limits)))))))

  (method ^:final addSubroutineRetSuccessors ^void [this ^:final ^Label subroutineCaller
                                                    ^:final ^Handler firstHandler
                                                    ^:final ^ComputeLimits limits]
    (let [^:mutable listOfProcessedBlocks EMPTY_LIST
          ^:mutable listOfBlocksToProcess this]
      (set! (.-nextListElement listOfBlocksToProcess) EMPTY_LIST)
      (let [^:mutable retInsnFound false]
        (while (not (identical? listOfBlocksToProcess EMPTY_LIST))
          (let [basicBlock listOfBlocksToProcess]
            (set! listOfBlocksToProcess (.-nextListElement basicBlock))
            (set! (.-nextListElement basicBlock) listOfProcessedBlocks)
            (set! listOfProcessedBlocks basicBlock)
            (when (and (not (== (bit-and-int (.-flags basicBlock) FLAG_SUBROUTINE_END) 0))
                       (not (== (.-subroutineId basicBlock) (.-subroutineId subroutineCaller))))
              (when retInsnFound (throw (RuntimeException. "Multiple rets to single jsr")))
              (set! retInsnFound true)
              (set! (.-outgoingEdges basicBlock)
                    (Edge. (.-outputStackSize basicBlock)
                           (.-successor (.-outgoingEdges subroutineCaller))
                           (.-outgoingEdges basicBlock))))
            (set! listOfBlocksToProcess
                  (.pushSuccessors basicBlock listOfBlocksToProcess firstHandler limits))))
        (while (not (identical? listOfProcessedBlocks EMPTY_LIST))
          (let [newListOfProcessedBlocks (.-nextListElement listOfProcessedBlocks)]
            (set! (.-nextListElement listOfProcessedBlocks) nil)
            (set! listOfProcessedBlocks newListOfProcessedBlocks))))))

  (method ^:private pushSuccessors ^Label [this ^:final ^Label listOfLabelsToProcess
                                           ^:final ^Handler firstHandler
                                           ^:final ^ComputeLimits limits]
    (let [^:mutable newListOfLabelsToProcess listOfLabelsToProcess
          ^:mutable outgoingEdge outgoingEdges
          ^:mutable ^int numOperations 2]
      (while (some? outgoingEdge)
        (let [isJsrTarget (and (not (== (bit-and-int flags Label/FLAG_SUBROUTINE_CALLER) 0))
                               (identical? outgoingEdge (.-nextEdge outgoingEdges)))]
          (when (and (not isJsrTarget) (nil? (.-nextListElement (.-successor outgoingEdge))))
            (set! (.-nextListElement (.-successor outgoingEdge)) newListOfLabelsToProcess)
            (set! newListOfLabelsToProcess (.-successor outgoingEdge)))
          (set! outgoingEdge (.-nextEdge outgoingEdge))
          (set! numOperations (unchecked-add-int numOperations 5))))
      (let [basicBlockOffset bytecodeOffset
            ^:mutable handler firstHandler]
        (while (some? handler)
          (let [startOffset (.-bytecodeOffset (.-startPc handler))
                endOffset (.-bytecodeOffset (.-endPc handler))]
            (when (and (>= basicBlockOffset startOffset) (< basicBlockOffset endOffset))
              (let [successorBlock (.-handlerPc handler)]
                (when (nil? (.-nextListElement successorBlock))
                  (set! (.-nextListElement successorBlock) newListOfLabelsToProcess)
                  (set! newListOfLabelsToProcess successorBlock))))
            (set! handler (.-nextHandler handler))
            (set! numOperations (unchecked-add-int numOperations 5))))
        (.checkNewOperations limits numOperations)
        newListOfLabelsToProcess)))

  (method ^:public toString ^String [this]
    (java-str "L" (System/identityHashCode this))))
