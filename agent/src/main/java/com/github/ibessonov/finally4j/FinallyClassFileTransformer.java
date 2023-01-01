/*
 * Copyright 2018 Ivan Bessonov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ibessonov.finally4j;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static com.github.ibessonov.finally4j.Util.ASM_V;
import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;

/**
 * @author ibessonov
 */
class FinallyClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        // Lambdas and other classes defined with "Unsafe#defineAnonymousClass".
        if (className == null) return null;

        // "Finally" class requires special treatment, because Finally#isSupported should return true when transformed.
        if (className.equals(Constants.FINALLY_CLASS_INTERNAL_NAME)) {
            return transformFinallyClass(classfileBuffer);
        }

        var cr = new ClassReader0(classfileBuffer);
        var cw = new ClassWriter(cr, COMPUTE_MAXS | COMPUTE_FRAMES);

        if (!cr.hasFinallyReferenced) return null;

        var cv = new ClassVisitor0(cw);
        cr.accept(cv, 0);

        return cv.classWasTransformed ? cw.toByteArray() : null;
    }

    private static byte[] transformFinallyClass(byte[] classfileBuffer) {
        var cr = new ClassReader(classfileBuffer);
        var cw = new ClassWriter(cr, COMPUTE_MAXS | COMPUTE_FRAMES);
        var cv = new ClassVisitor(ASM_V, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor outerMv = super.visitMethod(access, name, desc, signature, exceptions);

                if (name.equals(Constants.FINALLY_IS_SUPPORTED_METHOD_NAME)) {
                    return new MethodVisitor(ASM_V, outerMv) {

                        @Override
                        public void visitInsn(int opcode) {
                            // replace false with true
                            super.visitInsn(opcode == ICONST_0 ? ICONST_1 : opcode);
                        }
                    };
                } else {
                    return outerMv;
                }
            }
        };

        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    /**
     * Class reader implementation that scans the symbol table for the presence of "Finally" class. Classes that don't
     * have it present should not be transformed.
     */
    private static class ClassReader0 extends ClassReader {
        boolean hasFinallyReferenced;

        public ClassReader0(byte[] classFile) {
            super(classFile);
        }

        @Override
        public String readUTF8(int index, char[] buf) {
            String utf8str = super.readUTF8(index, buf);
            if (!hasFinallyReferenced && utf8str != null) {
                hasFinallyReferenced = utf8str.equals(Constants.FINALLY_CLASS_INTERNAL_NAME);
            }
            return utf8str;
        }
    }

    /**
     * Class visitor implementation that transforms all methods that call methods of "Finally" class.
     */
    private static class ClassVisitor0 extends ClassVisitor {
        boolean classWasTransformed;

        private final Runnable callback;

        public ClassVisitor0(ClassWriter cw) {
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
}
