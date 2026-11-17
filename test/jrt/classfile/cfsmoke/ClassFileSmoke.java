/*
 * F26's run-time smoke of the class-file API (java.lang.classfile, jdk.internal.classfile.impl) in the Go build:
 * builds a class, parses it back element by element, verifies it, transforms it, and asks the String and Class
 * contracts the API relies on (String as a ConstantDesc, Class as a Constable and a TypeDescriptor.OfField).
 * Its output is the JDK's (ClassFileSmoke.out, by the JDK 26 of the jdk26u build). Run by hand on an executable whose
 * world holds F26 (test/jrt/jrt_sources.ae's classfile-sources), its forms read by repr/java:
 *   bin/repr-java read .tmp/cf/forms test/jrt/classfile
 *   (cd .tmp/cf && ARBACE_PATH=forms EXE ../../test/java/run.ae --load files forms cfsmoke.ClassFileSmoke) \
 *     | diff - test/jrt/classfile/cfsmoke/ClassFileSmoke.out
 */
package cfsmoke;

import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.AccessFlag;
import java.util.*;

public class ClassFileSmoke {
    static final ClassDesc CD_Hello = ClassDesc.of("demo.Hello");

    static byte[] build() {
        return ClassFile.of().build(CD_Hello, cb -> cb
            .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL)
            .withField("count", ConstantDescs.CD_int, ClassFile.ACC_PRIVATE | ClassFile.ACC_STATIC)
            .withMethodBody("greet", MethodTypeDesc.of(ConstantDescs.CD_String, ConstantDescs.CD_int),
                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> {
                    Label small = code.newLabel();
                    code.iload(0).bipush(10).if_icmplt(small)
                        .ldc("big").areturn()
                        .labelBinding(small)
                        .ldc("small").areturn();
                })
            .withMethodBody("sum", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int),
                ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> {
                    Label top = code.newLabel(), done = code.newLabel();
                    code.iconst_0().istore(1)
                        .labelBinding(top)
                        .iload(0).ifle(done)
                        .iload(1).iload(0).iadd().istore(1)
                        .iinc(0, -1).goto_(top)
                        .labelBinding(done)
                        .iload(1).ireturn();
                }));
    }

    static void describe(ClassModel cm) {
        System.out.println("class " + cm.thisClass().asInternalName() + " super " + cm.superclass().map(ClassEntry::asInternalName).orElse("-")
                           + " version " + cm.majorVersion() + "." + cm.minorVersion() + " flags " + cm.flags().flags());
        for (FieldModel f : cm.fields())
            System.out.println("  field " + f.fieldName().stringValue() + " " + f.fieldType().stringValue() + " " + f.flags().flags());
        for (MethodModel m : cm.methods()) {
            System.out.println("  method " + m.methodName().stringValue() + m.methodType().stringValue() + " " + m.flags().flags());
            m.code().ifPresent(code -> {
                for (CodeElement e : code) {
                    if (e instanceof Instruction i) {
                        String extra = switch (i) {
                            case ConstantInstruction c -> " " + c.constantValue();
                            case BranchInstruction b -> " ->";
                            case IncrementInstruction n -> " " + n.slot() + " " + n.constant();
                            default -> "";
                        };
                        System.out.println("    " + i.opcode() + extra);
                    }
                }
                code.findAttribute(Attributes.stackMapTable()).ifPresent(smt ->
                    System.out.println("    stack map frames " + smt.entries().size()));
            });
        }
        int n = 0;
        for (PoolEntry pe : cm.constantPool()) {
            if (pe instanceof Utf8Entry) n++;
        }
        System.out.println("  utf8 entries " + n);
    }

    public static void main(String[] args) {
        byte[] bytes = build();
        System.out.println("built " + (bytes.length > 0));
        ClassModel cm = ClassFile.of().parse(bytes);
        describe(cm);
        System.out.println("verify " + ClassFile.of().verify(bytes));

        // a transform: drop the field, replace the constant "big"
        byte[] t = ClassFile.of().transformClass(cm, ClassTransform.dropping(e -> e instanceof FieldModel)
            .andThen(ClassTransform.transformingMethodBodies((cb, e) -> {
                if (e instanceof ConstantInstruction c && "big".equals(c.constantValue())) cb.ldc("large");
                else cb.with(e);
            })));
        describe(ClassFile.of().parse(t));

        // the contracts of String and Class the API relies on
        ConstantDesc s = "a string";
        System.out.println("String as ConstantDesc " + s + " " + (s instanceof Constable));
        Optional<ClassDesc> d = String.class.describeConstable();
        System.out.println("Class.describeConstable " + d.map(ClassDesc::descriptorString).orElse("-"));
        Constable k = int[].class;
        System.out.println("int[] Constable " + k.describeConstable().map(Object::toString).orElse("-"));
        TypeDescriptor.OfField<?> f = int[].class;
        System.out.println("OfField " + f.descriptorString() + " " + f.isArray() + " " + f.componentType().descriptorString()
                           + " " + f.arrayType().descriptorString());
        System.out.println("AccessFlag " + AccessFlag.maskToAccessFlags(0x0019, AccessFlag.Location.FIELD));
    }
}
