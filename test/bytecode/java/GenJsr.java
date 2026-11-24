import java.lang.classfile.*;
import java.lang.classfile.instruction.*;
import java.lang.constant.*;
import java.nio.file.*;
import static java.lang.constant.ConstantDescs.*;
public class GenJsr {
  public static void main(String[] a) throws Exception {
    // class J (version 49): static int f(int x) { int r; try { r = 100 / x; } finally { counter++; } return r; } with finally as jsr/ret
    byte[] b = ClassFile.of(ClassFile.StackMapsOption.DROP_STACK_MAPS).build(ClassDesc.of("J"), clb -> {
      clb.withVersion(49, 0);
      clb.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_SUPER);
      clb.withSuperclass(CD_Object);
      clb.withField("counter", CD_int, ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC);
      clb.withMethodBody(INIT_NAME, MTD_void, ClassFile.ACC_PUBLIC, cob -> { cob.aload(0); cob.invokespecial(CD_Object, INIT_NAME, MTD_void); cob.return_(); });
      clb.withMethodBody("f", MethodTypeDesc.of(CD_int, CD_int), ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, cob -> {
        Label start = cob.newLabel(), end = cob.newLabel(), handler = cob.newLabel(), sub = cob.newLabel(), after = cob.newLabel();
        cob.labelBinding(start);
        cob.bipush(100); cob.iload(0); cob.idiv(); cob.istore(1);
        cob.labelBinding(end);
        cob.with(DiscontinuedInstruction.JsrInstruction.of(sub));
        cob.goto_(after);
        cob.labelBinding(handler);
        cob.astore(2);
        cob.with(DiscontinuedInstruction.JsrInstruction.of(sub));
        cob.aload(2); cob.athrow();
        cob.labelBinding(sub);
        cob.astore(3);
        cob.getstatic(ClassDesc.of("J"), "counter", CD_int); cob.iconst_1(); cob.iadd(); cob.putstatic(ClassDesc.of("J"), "counter", CD_int);
        cob.with(DiscontinuedInstruction.RetInstruction.of(3));
        cob.labelBinding(after);
        cob.iload(1); cob.ireturn();
        cob.exceptionCatchAll(start, end, handler);
      });
    });
    Files.write(Path.of("J.class"), b);
  }
}
