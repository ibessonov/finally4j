package com.github.ibessonov.finally4j.agent.transformer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/**
 * Class visitor implementation that transforms all methods that call methods of "Finally" class.
 */
class FinallyClassVisitor extends ClassVisitor {
    boolean classWasTransformed;

    private final Runnable callback;

    FinallyClassVisitor(ClassWriter cw) {
        super(Util.ASM_V, cw);

        // Make this a field for mostly aesthetic purposes.
        callback = () -> classWasTransformed = true;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor outerMv = super.visitMethod(access, name, desc, signature, exceptions);

        return new FinallyMethodNode(callback, outerMv, access, name, desc, signature, exceptions);
    }
}
