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
;; /* rich 9/5/11 */
;;
;; Converted from clojure/lang/Intrinsics.java of Clojure 98d735fab02f by arbace.j2c
;; (convert --rename clojure=arbace) and arbace.j2c.rename; see doc/VENDOR-NOTES.md.

(in-ns 'arbace.lang)

(import '(arbace.asm Opcodes))

(defclass ^:public Intrinsics
  :implements [Opcodes]

  (method ^:private ^:static oa ^Object/1 [& ^Object/1 arr] arr)

  (field ^:static ^IPersistentMap ops
    (^[Object/1] RT/map
      "public static double arbace.lang.Numbers.add(double,double)"
      Intrinsics/DADD
      "public static long arbace.lang.Numbers.and(long,long)"
      Intrinsics/LAND
      "public static long arbace.lang.Numbers.or(long,long)"
      Intrinsics/LOR
      "public static long arbace.lang.Numbers.xor(long,long)"
      Intrinsics/LXOR
      "public static double arbace.lang.Numbers.multiply(double,double)"
      Intrinsics/DMUL
      "public static double arbace.lang.Numbers.divide(double,double)"
      Intrinsics/DDIV
      "public static long arbace.lang.Numbers.remainder(long,long)"
      Intrinsics/LREM
      "public static long arbace.lang.Numbers.shiftLeft(long,long)"
      (^[Object/1] Intrinsics/oa Intrinsics/L2I Intrinsics/LSHL)
      "public static long arbace.lang.Numbers.shiftRight(long,long)"
      (^[Object/1] Intrinsics/oa Intrinsics/L2I Intrinsics/LSHR)
      "public static long arbace.lang.Numbers.unsignedShiftRight(long,long)"
      (^[Object/1] Intrinsics/oa Intrinsics/L2I Intrinsics/LUSHR)
      "public static double arbace.lang.Numbers.minus(double)"
      Intrinsics/DNEG
      "public static double arbace.lang.Numbers.minus(double,double)"
      Intrinsics/DSUB
      "public static double arbace.lang.Numbers.inc(double)"
      (^[Object/1] Intrinsics/oa Intrinsics/DCONST_1 Intrinsics/DADD)
      "public static double arbace.lang.Numbers.dec(double)"
      (^[Object/1] Intrinsics/oa Intrinsics/DCONST_1 Intrinsics/DSUB)
      "public static long arbace.lang.Numbers.quotient(long,long)"
      Intrinsics/LDIV
      "public static int arbace.lang.Numbers.shiftLeftInt(int,int)"
      Intrinsics/ISHL
      "public static int arbace.lang.Numbers.shiftRightInt(int,int)"
      Intrinsics/ISHR
      "public static int arbace.lang.Numbers.unsignedShiftRightInt(int,int)"
      Intrinsics/IUSHR
      "public static int arbace.lang.Numbers.unchecked_int_add(int,int)"
      Intrinsics/IADD
      "public static int arbace.lang.Numbers.unchecked_int_subtract(int,int)"
      Intrinsics/ISUB
      "public static int arbace.lang.Numbers.unchecked_int_negate(int)"
      Intrinsics/INEG
      "public static int arbace.lang.Numbers.unchecked_int_inc(int)"
      (^[Object/1] Intrinsics/oa Intrinsics/ICONST_1 Intrinsics/IADD)
      "public static int arbace.lang.Numbers.unchecked_int_dec(int)"
      (^[Object/1] Intrinsics/oa Intrinsics/ICONST_1 Intrinsics/ISUB)
      "public static int arbace.lang.Numbers.unchecked_int_multiply(int,int)"
      Intrinsics/IMUL
      "public static int arbace.lang.Numbers.unchecked_int_divide(int,int)"
      Intrinsics/IDIV
      "public static int arbace.lang.Numbers.unchecked_int_remainder(int,int)"
      Intrinsics/IREM
      "public static int arbace.lang.Numbers.andInt(int,int)"
      Intrinsics/IAND
      "public static int arbace.lang.Numbers.orInt(int,int)"
      Intrinsics/IOR
      "public static int arbace.lang.Numbers.xorInt(int,int)"
      Intrinsics/IXOR
      "public static int arbace.lang.Numbers.notInt(int)"
      (^[Object/1] Intrinsics/oa Intrinsics/ICONST_M1 Intrinsics/IXOR)
      "public static float arbace.lang.Numbers.unchecked_float_add(float,float)"
      Intrinsics/FADD
      "public static float arbace.lang.Numbers.unchecked_float_subtract(float,float)"
      Intrinsics/FSUB
      "public static float arbace.lang.Numbers.unchecked_float_multiply(float,float)"
      Intrinsics/FMUL
      "public static float arbace.lang.Numbers.unchecked_float_divide(float,float)"
      Intrinsics/FDIV
      "public static float arbace.lang.Numbers.unchecked_float_remainder(float,float)"
      Intrinsics/FREM
      "public static float arbace.lang.Numbers.unchecked_float_negate(float)"
      Intrinsics/FNEG
      "public static long arbace.lang.Numbers.unchecked_divide(long,long)"
      Intrinsics/LDIV
      "public static double arbace.lang.Numbers.unchecked_divide(double,double)"
      Intrinsics/DDIV
      "public static long arbace.lang.Numbers.unchecked_remainder(long,long)"
      Intrinsics/LREM
      "public static double arbace.lang.Numbers.unchecked_remainder(double,double)"
      Intrinsics/DREM
      "public static long arbace.lang.Numbers.unchecked_add(long,long)"
      Intrinsics/LADD
      "public static double arbace.lang.Numbers.unchecked_add(double,double)"
      Intrinsics/DADD
      "public static long arbace.lang.Numbers.unchecked_minus(long)"
      Intrinsics/LNEG
      "public static double arbace.lang.Numbers.unchecked_minus(double)"
      Intrinsics/DNEG
      "public static double arbace.lang.Numbers.unchecked_minus(double,double)"
      Intrinsics/DSUB
      "public static long arbace.lang.Numbers.unchecked_minus(long,long)"
      Intrinsics/LSUB
      "public static long arbace.lang.Numbers.unchecked_multiply(long,long)"
      Intrinsics/LMUL
      "public static double arbace.lang.Numbers.unchecked_multiply(double,double)"
      Intrinsics/DMUL
      "public static double arbace.lang.Numbers.unchecked_inc(double)"
      (^[Object/1] Intrinsics/oa Intrinsics/DCONST_1 Intrinsics/DADD)
      "public static long arbace.lang.Numbers.unchecked_inc(long)"
      (^[Object/1] Intrinsics/oa Intrinsics/LCONST_1 Intrinsics/LADD)
      "public static double arbace.lang.Numbers.unchecked_dec(double)"
      (^[Object/1] Intrinsics/oa Intrinsics/DCONST_1 Intrinsics/DSUB)
      "public static long arbace.lang.Numbers.unchecked_dec(long)"
      (^[Object/1] Intrinsics/oa Intrinsics/LCONST_1 Intrinsics/LSUB)
      "public static short arbace.lang.RT.aget(short[],int)"
      Intrinsics/SALOAD
      "public static float arbace.lang.RT.aget(float[],int)"
      Intrinsics/FALOAD
      "public static double arbace.lang.RT.aget(double[],int)"
      Intrinsics/DALOAD
      "public static int arbace.lang.RT.aget(int[],int)"
      Intrinsics/IALOAD
      "public static long arbace.lang.RT.aget(long[],int)"
      Intrinsics/LALOAD
      "public static char arbace.lang.RT.aget(char[],int)"
      Intrinsics/CALOAD
      "public static byte arbace.lang.RT.aget(byte[],int)"
      Intrinsics/BALOAD
      "public static boolean arbace.lang.RT.aget(boolean[],int)"
      Intrinsics/BALOAD
      "public static java.lang.Object arbace.lang.RT.aget(java.lang.Object[],int)"
      Intrinsics/AALOAD
      "public static int arbace.lang.RT.alength(int[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(long[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(char[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(java.lang.Object[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(byte[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(float[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(short[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(boolean[])"
      Intrinsics/ARRAYLENGTH
      "public static int arbace.lang.RT.alength(double[])"
      Intrinsics/ARRAYLENGTH
      "public static double arbace.lang.RT.doubleCast(long)"
      Intrinsics/L2D
      "public static double arbace.lang.RT.doubleCast(double)"
      Intrinsics/NOP
      "public static double arbace.lang.RT.doubleCast(float)"
      Intrinsics/F2D
      "public static double arbace.lang.RT.doubleCast(int)"
      Intrinsics/I2D
      "public static double arbace.lang.RT.doubleCast(short)"
      Intrinsics/I2D
      "public static double arbace.lang.RT.doubleCast(byte)"
      Intrinsics/I2D
      "public static double arbace.lang.RT.uncheckedDoubleCast(double)"
      Intrinsics/NOP
      "public static double arbace.lang.RT.uncheckedDoubleCast(float)"
      Intrinsics/F2D
      "public static double arbace.lang.RT.uncheckedDoubleCast(long)"
      Intrinsics/L2D
      "public static double arbace.lang.RT.uncheckedDoubleCast(int)"
      Intrinsics/I2D
      "public static double arbace.lang.RT.uncheckedDoubleCast(short)"
      Intrinsics/I2D
      "public static double arbace.lang.RT.uncheckedDoubleCast(byte)"
      Intrinsics/I2D
      "public static long arbace.lang.RT.longCast(long)"
      Intrinsics/NOP
      "public static long arbace.lang.RT.longCast(short)"
      Intrinsics/I2L
      "public static long arbace.lang.RT.longCast(byte)"
      Intrinsics/I2L
      "public static long arbace.lang.RT.longCast(int)"
      Intrinsics/I2L
      "public static int arbace.lang.RT.uncheckedIntCast(long)"
      Intrinsics/L2I
      "public static int arbace.lang.RT.uncheckedIntCast(double)"
      Intrinsics/D2I
      "public static int arbace.lang.RT.uncheckedIntCast(byte)"
      Intrinsics/NOP
      "public static int arbace.lang.RT.uncheckedIntCast(short)"
      Intrinsics/NOP
      "public static int arbace.lang.RT.uncheckedIntCast(char)"
      Intrinsics/NOP
      "public static int arbace.lang.RT.uncheckedIntCast(int)"
      Intrinsics/NOP
      "public static int arbace.lang.RT.uncheckedIntCast(float)"
      Intrinsics/F2I
      "public static long arbace.lang.RT.uncheckedLongCast(short)"
      Intrinsics/I2L
      "public static long arbace.lang.RT.uncheckedLongCast(float)"
      Intrinsics/F2L
      "public static long arbace.lang.RT.uncheckedLongCast(double)"
      Intrinsics/D2L
      "public static long arbace.lang.RT.uncheckedLongCast(byte)"
      Intrinsics/I2L
      "public static long arbace.lang.RT.uncheckedLongCast(long)"
      Intrinsics/NOP
      "public static long arbace.lang.RT.uncheckedLongCast(int)"
      Intrinsics/I2L))

  (field ^:static ^IPersistentMap preds
    (^[Object/1] RT/map "public static boolean arbace.lang.Numbers.lt(double,double)"
                        (^[Object/1] Intrinsics/oa Intrinsics/DCMPG Intrinsics/IFGE)
                        "public static boolean arbace.lang.Numbers.lt(long,long)"
                        (^[Object/1] Intrinsics/oa Intrinsics/LCMP Intrinsics/IFGE)
                        "public static boolean arbace.lang.Numbers.equiv(double,double)"
                        (^[Object/1] Intrinsics/oa Intrinsics/DCMPL Intrinsics/IFNE)
                        "public static boolean arbace.lang.Numbers.equiv(long,long)"
                        (^[Object/1] Intrinsics/oa Intrinsics/LCMP Intrinsics/IFNE)
                        "public static boolean arbace.lang.Numbers.lte(double,double)"
                        (^[Object/1] Intrinsics/oa Intrinsics/DCMPG Intrinsics/IFGT)
                        "public static boolean arbace.lang.Numbers.lte(long,long)"
                        (^[Object/1] Intrinsics/oa Intrinsics/LCMP Intrinsics/IFGT)
                        "public static boolean arbace.lang.Numbers.gt(long,long)"
                        (^[Object/1] Intrinsics/oa Intrinsics/LCMP Intrinsics/IFLE)
                        "public static boolean arbace.lang.Numbers.gt(double,double)"
                        (^[Object/1] Intrinsics/oa Intrinsics/DCMPL Intrinsics/IFLE)
                        "public static boolean arbace.lang.Numbers.gte(long,long)"
                        (^[Object/1] Intrinsics/oa Intrinsics/LCMP Intrinsics/IFLT)
                        "public static boolean arbace.lang.Numbers.gte(double,double)"
                        (^[Object/1] Intrinsics/oa Intrinsics/DCMPL Intrinsics/IFLT)
                        "public static boolean arbace.lang.Util.equiv(long,long)"
                        (^[Object/1] Intrinsics/oa Intrinsics/LCMP Intrinsics/IFNE)
                        "public static boolean arbace.lang.Util.equiv(boolean,boolean)"
                        (Intrinsics/oa Intrinsics/IF_ICMPNE)
                        "public static boolean arbace.lang.Util.equiv(double,double)"
                        (^[Object/1] Intrinsics/oa Intrinsics/DCMPL Intrinsics/IFNE)
                        "public static boolean arbace.lang.Numbers.isZero(double)"
                        (^[Object/1] Intrinsics/oa
                          Intrinsics/DCONST_0
                          Intrinsics/DCMPL
                          Intrinsics/IFNE)
                        "public static boolean arbace.lang.Numbers.isZero(long)"
                        (^[Object/1] Intrinsics/oa
                          Intrinsics/LCONST_0
                          Intrinsics/LCMP
                          Intrinsics/IFNE)
                        "public static boolean arbace.lang.Numbers.isPos(long)"
                        (^[Object/1] Intrinsics/oa
                          Intrinsics/LCONST_0
                          Intrinsics/LCMP
                          Intrinsics/IFLE)
                        "public static boolean arbace.lang.Numbers.isPos(double)"
                        (^[Object/1] Intrinsics/oa
                          Intrinsics/DCONST_0
                          Intrinsics/DCMPL
                          Intrinsics/IFLE)
                        "public static boolean arbace.lang.Numbers.isNeg(long)"
                        (^[Object/1] Intrinsics/oa
                          Intrinsics/LCONST_0
                          Intrinsics/LCMP
                          Intrinsics/IFGE)
                        "public static boolean arbace.lang.Numbers.isNeg(double)"
                        (^[Object/1] Intrinsics/oa
                          Intrinsics/DCONST_0
                          Intrinsics/DCMPG
                          Intrinsics/IFGE))))
