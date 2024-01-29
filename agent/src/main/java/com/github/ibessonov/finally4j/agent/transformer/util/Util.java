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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.tree.AbstractInsnNode.LABEL;

/**
 * @author ibessonov
 */
public class Util {
    public static final boolean DEBUG = "true".equals(System.getProperty("finally4j.debug"));

    public static int ASM_V = ASM7;

    public static AbstractInsnNode findPreviousInstruction(AbstractInsnNode node) {
        do {
            node = node.getPrevious();
        } while (node.getOpcode() == -1);
        return node;
    }

    public static AbstractInsnNode findNextInstruction(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node != null && node.getOpcode() == -1);
        return node;
    }

    public static LabelNode findPreviousLabel(AbstractInsnNode node) {
        do {
            node = node.getPrevious();
        } while (node != null && node.getType() != LABEL);
        return (LabelNode) node;
    }

    public static LabelNode findNextLabel(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node != null && node.getType() != LABEL);
        return (LabelNode) node;
    }

    public static boolean isStore(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return ISTORE <= opcode && opcode <= ASTORE;
    }

    public static boolean isLoad(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return ILOAD <= opcode && opcode <= ALOAD;
    }

    public static boolean isReturn(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return IRETURN <= opcode && opcode <= RETURN;
    }

    public static boolean isThrow(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return opcode == ATHROW;
    }

    public static boolean regularCatch(TryCatchBlockNode block) {
        return block.type != null;
    }

    public static boolean defaultCatch(TryCatchBlockNode block) {
        return block.type == null;
    }

    public static boolean validBlock(TryCatchBlockNode block) {
        // Nasty bug in compiler?
        return block.start != block.handler;
    }

    public static String toBoxedInternalName(char type) {
        switch (type) {
            case 'Z': return "java/lang/Boolean";
            case 'B': return "java/lang/Byte";
            case 'C': return "java/lang/Character";
            case 'S': return "java/lang/Short";
            case 'I': return "java/lang/Integer";
            case 'J': return "java/lang/Long";
            case 'F': return "java/lang/Float";
            case 'D': return "java/lang/Double";
            default:  throw illegalType(type);
        }
    }

    public static int loadOpcode(char type) {
        switch (type) {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I': return ILOAD;
            case 'J': return LLOAD;
            case 'F': return FLOAD;
            case 'D': return DLOAD;
            default:  return ALOAD;
        }
    }

    public static String toPrimitiveName(char type) {
        switch (type) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'F': return "float";
            case 'D': return "double";
            default:  throw illegalType(type);
        }
    }

    public static String getMethodDescriptorForValueOf(char type) {
        return "(" + type + ")L" + toBoxedInternalName(type) + ";";
    }

    private static IllegalArgumentException illegalType(char type) {
        return new IllegalArgumentException(Character.toString(type));
    }

    public static MethodInsnNode optionalOfNullable() {
        return new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "ofNullable", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
    }

    public static MethodInsnNode optionalOf() {
        return new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "of", "(Ljava/lang/Object;)Ljava/util/Optional;",false);
    }

    public static MethodInsnNode valueOf(char returnTypeDescriptor) {
        return new MethodInsnNode(INVOKESTATIC, toBoxedInternalName(returnTypeDescriptor),
                "valueOf", getMethodDescriptorForValueOf(returnTypeDescriptor),false);
    }

    public static MethodInsnNode primitiveValue(char returnTypeDescriptor) {
        String primitiveName = toPrimitiveName(returnTypeDescriptor);
        String boxedInternalName = toBoxedInternalName(returnTypeDescriptor);

        return new MethodInsnNode(INVOKEVIRTUAL,
                boxedInternalName, primitiveName + "Value",
                "()" + returnTypeDescriptor, false);
    }

    public static LabelNode findTheEndOfFinally(Map<LabelNode, Integer> labelIdx, LabelNode startLabel, boolean defaultBlock) {
        AbstractInsnNode instruction = defaultBlock
                ? findNextInstruction(startLabel)
                : findPreviousInstruction(startLabel);

        assert isStore(instruction);

        var storeInstruction = (VarInsnNode) instruction;

        Set<LabelNode> jumpLabels = new HashSet<>();

        while (true) {
            instruction = instruction.getNext();

            if (instruction == null) {
                // Should not happen I guess.
                return null;
            }

            if (instruction instanceof LabelNode) {
                jumpLabels.remove((LabelNode) instruction);

                continue;
            }

            if (instruction instanceof JumpInsnNode && instruction.getOpcode() != GOTO) {
                LabelNode label = ((JumpInsnNode) instruction).label;

                jumpLabels.add(label);

                if (DEBUG) {
                    System.out.println("  Jump to " + labelIdx.get(label) + ". Opcode = " + findNextInstruction(label).getOpcode());
                }

                continue;
            }

            if (isLoad(instruction) && ((VarInsnNode) instruction).var == storeInstruction.var) {
                // Throw found.
                return findNextLabel(instruction);
            }

            if (isReturn(instruction) || isThrow(instruction)) {
                if (jumpLabels.isEmpty()) {
                    return findPreviousLabel(instruction);
                }
            }
        }
    }
}
