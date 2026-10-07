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
;; Converted from clojure/asm/Opcodes.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.asm)

(defclass ^:public ^:interface Opcodes
  (field ^int ASM4 (bit-or-int (bit-shift-left-int 4 16) (bit-shift-left-int 0 8)))

  (field ^int ASM5 (bit-or-int (bit-shift-left-int 5 16) (bit-shift-left-int 0 8)))

  (field ^int ASM6 (bit-or-int (bit-shift-left-int 6 16) (bit-shift-left-int 0 8)))

  (field ^int ASM7 (bit-or-int (bit-shift-left-int 7 16) (bit-shift-left-int 0 8)))

  (field ^int ASM8 (bit-or-int (bit-shift-left-int 8 16) (bit-shift-left-int 0 8)))

  (field ^int ASM9 (bit-or-int (bit-shift-left-int 9 16) (bit-shift-left-int 0 8)))

  (field ^{Deprecated {:forRemoval false}} ^int ASM10_EXPERIMENTAL
    (bit-or-int (bit-or-int (bit-shift-left-int 1 24) (bit-shift-left-int 10 16))
                (bit-shift-left-int 0 8)))

  (field ^int SOURCE_DEPRECATED 0x100)

  (field ^int SOURCE_MASK SOURCE_DEPRECATED)

  (field ^int V1_1 (bit-or-int (bit-shift-left-int 3 16) 45))

  (field ^int V1_2 (bit-or-int (bit-shift-left-int 0 16) 46))

  (field ^int V1_3 (bit-or-int (bit-shift-left-int 0 16) 47))

  (field ^int V1_4 (bit-or-int (bit-shift-left-int 0 16) 48))

  (field ^int V1_5 (bit-or-int (bit-shift-left-int 0 16) 49))

  (field ^int V1_6 (bit-or-int (bit-shift-left-int 0 16) 50))

  (field ^int V1_7 (bit-or-int (bit-shift-left-int 0 16) 51))

  (field ^int V1_8 (bit-or-int (bit-shift-left-int 0 16) 52))

  (field ^int V9 (bit-or-int (bit-shift-left-int 0 16) 53))

  (field ^int V10 (bit-or-int (bit-shift-left-int 0 16) 54))

  (field ^int V11 (bit-or-int (bit-shift-left-int 0 16) 55))

  (field ^int V12 (bit-or-int (bit-shift-left-int 0 16) 56))

  (field ^int V13 (bit-or-int (bit-shift-left-int 0 16) 57))

  (field ^int V14 (bit-or-int (bit-shift-left-int 0 16) 58))

  (field ^int V15 (bit-or-int (bit-shift-left-int 0 16) 59))

  (field ^int V16 (bit-or-int (bit-shift-left-int 0 16) 60))

  (field ^int V17 (bit-or-int (bit-shift-left-int 0 16) 61))

  (field ^int V18 (bit-or-int (bit-shift-left-int 0 16) 62))

  (field ^int V19 (bit-or-int (bit-shift-left-int 0 16) 63))

  (field ^int V20 (bit-or-int (bit-shift-left-int 0 16) 64))

  (field ^int V21 (bit-or-int (bit-shift-left-int 0 16) 65))

  (field ^int V22 (bit-or-int (bit-shift-left-int 0 16) 66))

  (field ^int V23 (bit-or-int (bit-shift-left-int 0 16) 67))

  (field ^int V24 (bit-or-int (bit-shift-left-int 0 16) 68))

  (field ^int V25 (bit-or-int (bit-shift-left-int 0 16) 69))

  (field ^int V26 (bit-or-int (bit-shift-left-int 0 16) 70))

  (field ^int V27 (bit-or-int (bit-shift-left-int 0 16) 71))

  (field ^int V28 (bit-or-int (bit-shift-left-int 0 16) 72))

  (field ^int V_PREVIEW (unchecked-int 0xFFFF0000))

  (field ^int ACC_PUBLIC 0x0001)

  (field ^int ACC_PRIVATE 0x0002)

  (field ^int ACC_PROTECTED 0x0004)

  (field ^int ACC_STATIC 0x0008)

  (field ^int ACC_FINAL 0x0010)

  (field ^int ACC_SUPER 0x0020)

  (field ^int ACC_SYNCHRONIZED 0x0020)

  (field ^int ACC_OPEN 0x0020)

  (field ^int ACC_TRANSITIVE 0x0020)

  (field ^int ACC_VOLATILE 0x0040)

  (field ^int ACC_BRIDGE 0x0040)

  (field ^int ACC_STATIC_PHASE 0x0040)

  (field ^int ACC_VARARGS 0x0080)

  (field ^int ACC_TRANSIENT 0x0080)

  (field ^int ACC_NATIVE 0x0100)

  (field ^int ACC_INTERFACE 0x0200)

  (field ^int ACC_ABSTRACT 0x0400)

  (field ^int ACC_STRICT 0x0800)

  (field ^int ACC_SYNTHETIC 0x1000)

  (field ^int ACC_ANNOTATION 0x2000)

  (field ^int ACC_ENUM 0x4000)

  (field ^int ACC_MANDATED 0x8000)

  (field ^int ACC_MODULE 0x8000)

  (field ^int ACC_RECORD 0x10000)

  (field ^int ACC_DEPRECATED 0x20000)

  (field ^int T_BOOLEAN 4)

  (field ^int T_CHAR 5)

  (field ^int T_FLOAT 6)

  (field ^int T_DOUBLE 7)

  (field ^int T_BYTE 8)

  (field ^int T_SHORT 9)

  (field ^int T_INT 10)

  (field ^int T_LONG 11)

  (field ^int H_GETFIELD 1)

  (field ^int H_GETSTATIC 2)

  (field ^int H_PUTFIELD 3)

  (field ^int H_PUTSTATIC 4)

  (field ^int H_INVOKEVIRTUAL 5)

  (field ^int H_INVOKESTATIC 6)

  (field ^int H_INVOKESPECIAL 7)

  (field ^int H_NEWINVOKESPECIAL 8)

  (field ^int H_INVOKEINTERFACE 9)

  (field ^int F_NEW -1)

  (field ^int F_FULL 0)

  (field ^int F_APPEND 1)

  (field ^int F_CHOP 2)

  (field ^int F_SAME 3)

  (field ^int F_SAME1 4)

  (field ^Integer TOP Frame/ITEM_TOP)

  (field ^Integer INTEGER Frame/ITEM_INTEGER)

  (field ^Integer FLOAT Frame/ITEM_FLOAT)

  (field ^Integer DOUBLE Frame/ITEM_DOUBLE)

  (field ^Integer LONG Frame/ITEM_LONG)

  (field ^Integer NULL Frame/ITEM_NULL)

  (field ^Integer UNINITIALIZED_THIS Frame/ITEM_UNINITIALIZED_THIS)

  (field ^int NOP 0)

  (field ^int ACONST_NULL 1)

  (field ^int ICONST_M1 2)

  (field ^int ICONST_0 3)

  (field ^int ICONST_1 4)

  (field ^int ICONST_2 5)

  (field ^int ICONST_3 6)

  (field ^int ICONST_4 7)

  (field ^int ICONST_5 8)

  (field ^int LCONST_0 9)

  (field ^int LCONST_1 10)

  (field ^int FCONST_0 11)

  (field ^int FCONST_1 12)

  (field ^int FCONST_2 13)

  (field ^int DCONST_0 14)

  (field ^int DCONST_1 15)

  (field ^int BIPUSH 16)

  (field ^int SIPUSH 17)

  (field ^int LDC 18)

  (field ^int ILOAD 21)

  (field ^int LLOAD 22)

  (field ^int FLOAD 23)

  (field ^int DLOAD 24)

  (field ^int ALOAD 25)

  (field ^int IALOAD 46)

  (field ^int LALOAD 47)

  (field ^int FALOAD 48)

  (field ^int DALOAD 49)

  (field ^int AALOAD 50)

  (field ^int BALOAD 51)

  (field ^int CALOAD 52)

  (field ^int SALOAD 53)

  (field ^int ISTORE 54)

  (field ^int LSTORE 55)

  (field ^int FSTORE 56)

  (field ^int DSTORE 57)

  (field ^int ASTORE 58)

  (field ^int IASTORE 79)

  (field ^int LASTORE 80)

  (field ^int FASTORE 81)

  (field ^int DASTORE 82)

  (field ^int AASTORE 83)

  (field ^int BASTORE 84)

  (field ^int CASTORE 85)

  (field ^int SASTORE 86)

  (field ^int POP 87)

  (field ^int POP2 88)

  (field ^int DUP 89)

  (field ^int DUP_X1 90)

  (field ^int DUP_X2 91)

  (field ^int DUP2 92)

  (field ^int DUP2_X1 93)

  (field ^int DUP2_X2 94)

  (field ^int SWAP 95)

  (field ^int IADD 96)

  (field ^int LADD 97)

  (field ^int FADD 98)

  (field ^int DADD 99)

  (field ^int ISUB 100)

  (field ^int LSUB 101)

  (field ^int FSUB 102)

  (field ^int DSUB 103)

  (field ^int IMUL 104)

  (field ^int LMUL 105)

  (field ^int FMUL 106)

  (field ^int DMUL 107)

  (field ^int IDIV 108)

  (field ^int LDIV 109)

  (field ^int FDIV 110)

  (field ^int DDIV 111)

  (field ^int IREM 112)

  (field ^int LREM 113)

  (field ^int FREM 114)

  (field ^int DREM 115)

  (field ^int INEG 116)

  (field ^int LNEG 117)

  (field ^int FNEG 118)

  (field ^int DNEG 119)

  (field ^int ISHL 120)

  (field ^int LSHL 121)

  (field ^int ISHR 122)

  (field ^int LSHR 123)

  (field ^int IUSHR 124)

  (field ^int LUSHR 125)

  (field ^int IAND 126)

  (field ^int LAND 127)

  (field ^int IOR 128)

  (field ^int LOR 129)

  (field ^int IXOR 130)

  (field ^int LXOR 131)

  (field ^int IINC 132)

  (field ^int I2L 133)

  (field ^int I2F 134)

  (field ^int I2D 135)

  (field ^int L2I 136)

  (field ^int L2F 137)

  (field ^int L2D 138)

  (field ^int F2I 139)

  (field ^int F2L 140)

  (field ^int F2D 141)

  (field ^int D2I 142)

  (field ^int D2L 143)

  (field ^int D2F 144)

  (field ^int I2B 145)

  (field ^int I2C 146)

  (field ^int I2S 147)

  (field ^int LCMP 148)

  (field ^int FCMPL 149)

  (field ^int FCMPG 150)

  (field ^int DCMPL 151)

  (field ^int DCMPG 152)

  (field ^int IFEQ 153)

  (field ^int IFNE 154)

  (field ^int IFLT 155)

  (field ^int IFGE 156)

  (field ^int IFGT 157)

  (field ^int IFLE 158)

  (field ^int IF_ICMPEQ 159)

  (field ^int IF_ICMPNE 160)

  (field ^int IF_ICMPLT 161)

  (field ^int IF_ICMPGE 162)

  (field ^int IF_ICMPGT 163)

  (field ^int IF_ICMPLE 164)

  (field ^int IF_ACMPEQ 165)

  (field ^int IF_ACMPNE 166)

  (field ^int GOTO 167)

  (field ^int JSR 168)

  (field ^int RET 169)

  (field ^int TABLESWITCH 170)

  (field ^int LOOKUPSWITCH 171)

  (field ^int IRETURN 172)

  (field ^int LRETURN 173)

  (field ^int FRETURN 174)

  (field ^int DRETURN 175)

  (field ^int ARETURN 176)

  (field ^int RETURN 177)

  (field ^int GETSTATIC 178)

  (field ^int PUTSTATIC 179)

  (field ^int GETFIELD 180)

  (field ^int PUTFIELD 181)

  (field ^int INVOKEVIRTUAL 182)

  (field ^int INVOKESPECIAL 183)

  (field ^int INVOKESTATIC 184)

  (field ^int INVOKEINTERFACE 185)

  (field ^int INVOKEDYNAMIC 186)

  (field ^int NEW 187)

  (field ^int NEWARRAY 188)

  (field ^int ANEWARRAY 189)

  (field ^int ARRAYLENGTH 190)

  (field ^int ATHROW 191)

  (field ^int CHECKCAST 192)

  (field ^int INSTANCEOF 193)

  (field ^int MONITORENTER 194)

  (field ^int MONITOREXIT 195)

  (field ^int MULTIANEWARRAY 197)

  (field ^int IFNULL 198)

  (field ^int IFNONNULL 199))
