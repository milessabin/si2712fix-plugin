import java.io.{ FileInputStream, FileOutputStream }

import sbt._
import sbt.Compiler.CompileResult

import org.objectweb.asm._

object fixSuperCtorCall {
  def apply(classes: File, classpath: Seq[File]): Unit = {
    val tvsCls = classes / "scala" / "reflect" / "internal" / "TypeVarSub.class"

    val in = new FileInputStream(tvsCls)
    val cr = new ClassReader(in)
    val cw = new ClassWriter(0)

    val cv = new ClassVisitor(Opcodes.ASM5, cw) {
      override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = {
        val mv = super.visitMethod(access, name, desc, signature, exceptions)
        if(name != "<init>") mv
        else
          new MethodVisitor(Opcodes.ASM5, mv) {
            override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean): Unit = {
              val newDesc =
                if(name == "<init>") "(Lscala/reflect/internal/SymbolTable;)V"
                else desc
              super.visitMethodInsn(opcode, owner, name, newDesc, itf)
            }
          }
      }
    }

    cr.accept(cv, 0)
    
    val out = new FileOutputStream(tvsCls)
    out.write(cw.toByteArray)
    out.close
    in.close
  }
}
