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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author ibessonov
 */
class FinallyClassFileTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {

        // lambdas and other classes defined with "Unsafe#defineAnonymousClass"
        if (className == null) return null;

        // "Finally" class requires special treatment
        if (className.equals(Constants.FINALLY_CLASS_INTERNAL_NAME)) {
            return transformFinallyClass(classfileBuffer);
        }

        boolean[] returnNull = { true };
        ClassReader cr = new ClassReader(classfileBuffer) {

            //TODO not sure if this will work in ASM 6, requires testing
            @Override
            public String readUTF8(int index, char[] buf) {
                String utf8str = super.readUTF8(index, buf);
                if (utf8str != null && returnNull[0]) {
                    returnNull[0] = !utf8str.equals(Constants.FINALLY_CLASS_INTERNAL_NAME);
                }
                return utf8str;
            }
        };
        ClassWriter cw = new ClassWriter(cr, COMPUTE_MAXS | COMPUTE_FRAMES);

        // do nothing if there are no references of "Finally" class
        if (returnNull[0]) return null;

        AtomicBoolean classWasTransformed = new AtomicBoolean(false);
        ClassVisitor cv = new ClassVisitor(ASM5, cw) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor outerMv = super.visitMethod(access, name, desc, signature, exceptions);
                return new FinallyMethodNode(classWasTransformed, outerMv, access, name, desc, signature, exceptions);
            }
        };

        try {
            System.out.println("----------------------------------------");
            System.out.println(className);
            System.out.println("----------------------------------------");
            cr.accept(cv, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classWasTransformed.get() ? cw.toByteArray() : null;
    }

    private static byte[] transformFinallyClass(byte[] classfileBuffer) {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, COMPUTE_MAXS | COMPUTE_FRAMES);

        ClassVisitor cv = new ClassVisitor(ASM5, cw) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor outerMv = super.visitMethod(access, name, desc, signature, exceptions);
                if (name.equals(Constants.FINALLY_IS_SUPPORTED_METHOD_NAME)) {
                    return new MethodVisitor(ASM5, outerMv) {

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
}
