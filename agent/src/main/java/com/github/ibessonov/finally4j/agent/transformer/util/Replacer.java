/*
 * Copyright 2024 Ivan Bessonov
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
package com.github.ibessonov.finally4j.agent.transformer.util;

import com.github.ibessonov.finally4j.agent.transformer.code.Block;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static com.github.ibessonov.finally4j.agent.transformer.util.Util.loadOpcode;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.toBoxedInternalName;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.toPrimitiveName;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

public class Replacer {
    private final InsnList instructions;
    private final Runnable methodTransformedClosure;

    private final char returnType;

    public Replacer(InsnList instructions, Runnable methodTransformedClosure, char returnType) {
        this.instructions = instructions;
        this.methodTransformedClosure = methodTransformedClosure;
        this.returnType = returnType;
    }

    public AbstractInsnNode replaceInstruction(AbstractInsnNode from, AbstractInsnNode to) {
        instructions.insertBefore(from, to);
        instructions.remove(from);

        methodTransformedClosure.run();

        return to;
    }

    public void replaceReturnedValueInstructions(VarInsnNode storeInstruction, Block finallyBlock) {
        for (AbstractInsnNode node = finallyBlock.start; node != finallyBlock.end; node = node.getNext()) {
            if (node.getType() == METHOD_INSN && node.getOpcode() == INVOKESTATIC) {
                assert node instanceof MethodInsnNode;

                MethodInsnNode methodInstruction = (MethodInsnNode) node;
                if (methodInstruction.owner.equals(Constants.FINALLY_CLASS_INTERNAL_NAME)) {
                    switch (methodInstruction.name) {
                        case Constants.FINALLY_HAS_RETURNED_VALUE_METHOD_NAME:
                            node = replaceInstruction(methodInstruction, new InsnNode(ICONST_1));
                            break;

                        case Constants.FINALLY_HAS_THROWN_EXCEPTION_METHOD_NAME:
                            node = replaceInstruction(methodInstruction, new InsnNode(ICONST_0));
                            break;

                        default:
                            node = replaceReturnedValueInstruction(methodInstruction, storeInstruction.var);
                    }
                }
            }
        }
    }

    public void replaceExceptionInstructions(VarInsnNode storeInstruction, Block finallyBlock) {
        for (AbstractInsnNode node = finallyBlock.start; node != finallyBlock.end; node = node.getNext()) {
            if (node.getType() == METHOD_INSN && node.getOpcode() == INVOKESTATIC) {
                assert node instanceof MethodInsnNode;

                MethodInsnNode methodInstruction = (MethodInsnNode) node;
                if (methodInstruction.owner.equals(Constants.FINALLY_CLASS_INTERNAL_NAME)) {
                    switch (methodInstruction.name) {
                        case Constants.FINALLY_HAS_RETURNED_VALUE_METHOD_NAME:
                            node = replaceInstruction(methodInstruction, new InsnNode(ICONST_0));
                            break;

                        case Constants.FINALLY_HAS_THROWN_EXCEPTION_METHOD_NAME:
                            node = replaceInstruction(methodInstruction, new InsnNode(ICONST_1));
                            break;

                        case Constants.FINALLY_GET_THROWN_EXCEPTION_OPTIONAL_METHOD_NAME:
                            instructions.insert(node, Util.optionalOfNullable());

                            //noinspection fallthrough
                        case Constants.FINALLY_GET_THROWN_EXCEPTION_METHOD_NAME:
                            node = replaceInstruction(methodInstruction, new VarInsnNode(loadOpcode(';'), storeInstruction.var));
                    }
                }
            }
        }
    }

    /**
     * Replaces the {@code Finally.returnedValue*()} call with the actual returned value.
     *
     * @param methodInstruction Invoke instruction node.
     * @param var Variable index of the return value.
     * @return New instruction node that replaced the invoke instruction, or the original instruction if nothing happened.
     */
    private AbstractInsnNode replaceReturnedValueInstruction(MethodInsnNode methodInstruction, int var) {
        if (!methodInstruction.name.startsWith(Constants.FINALLY_GET_RETURNED_VALUE_METHOD_PREFIX)) {
            return methodInstruction;
        }

        assert returnType != 'V' : "Returning of void passed somehow";

        // Replace INVOKE* with *LOAD.
        AbstractInsnNode node = replaceInstruction(methodInstruction, new VarInsnNode(loadOpcode(returnType), var));

        if (returnType == ';') { // Method returns "Object", essentially.
            switch (methodInstruction.name) {
                case Constants.FINALLY_GET_RETURNED_VALUE_OPTIONAL_METHOD_NAME:
                    // Wrap the head of the stack into a "Optional.ofNullable".
                    instructions.insert(node, Util.optionalOfNullable());

                    //noinspection fallthrough
                case Constants.FINALLY_GET_RETURNED_VALUE_METHOD_NAME:
                    break;

                default:
                    char currentType = methodInstruction.desc.charAt(methodInstruction.desc.length() - 1);
                    String boxedInternalName = toBoxedInternalName(currentType);

                    // Insert instructions in reverse order.
                    // In reality the CHECKCAST is first, and INVOKEVIRTUAL is second.
                    instructions.insert(node, Util.primitiveValue(currentType));
                    instructions.insert(node, new TypeInsnNode(CHECKCAST, boxedInternalName));
            }
        } else { // Method returns primitive type.
            switch (methodInstruction.name) {
                case Constants.FINALLY_GET_RETURNED_VALUE_OPTIONAL_METHOD_NAME:
                    // Insert instructions in reverse order. "Optional.of" goes last.
                    instructions.insert(node, Util.optionalOf());

                    //noinspection fallthrough
                case Constants.FINALLY_GET_RETURNED_VALUE_METHOD_NAME:
                    instructions.insert(node, Util.valueOf(returnType));
                    break;

                default:
                    char currentType = methodInstruction.desc.charAt(methodInstruction.desc.length() - 1);
                    if (currentType != returnType) {
                        // Old method invocation left untouched to preserve bytecode consistency, just in case.
                        node = replaceInstruction(node, methodInstruction);

                        String message = toPrimitiveName(currentType) + " cannot be cast to " + toPrimitiveName(returnType);

                        // Bytecode for "throw new ClassCastException(message);".
                        instructions.insertBefore(node, new TypeInsnNode(NEW, "java/lang/ClassCastException"));
                        instructions.insertBefore(node, new InsnNode(DUP));
                        instructions.insertBefore(node, new LdcInsnNode(message));
                        instructions.insertBefore(node, new MethodInsnNode(INVOKESPECIAL, "java/lang/ClassCastException", "<init>", "(Ljava/lang/String;)V", false));
                        instructions.insertBefore(node, new InsnNode(ATHROW));
                    }
            }
        }

        return node;
    }
}
