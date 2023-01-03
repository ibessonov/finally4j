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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.tree.AbstractInsnNode.LABEL;

/**
 * @author ibessonov
 */
class Util {
    static int ASM_V = ASM7;

    static AbstractInsnNode findPreviousInstruction(AbstractInsnNode node) {
        do {
            node = node.getPrevious();
        } while (node.getOpcode() == -1);
        return node;
    }

    static AbstractInsnNode findNextInstruction(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node != null && node.getOpcode() == -1);
        return node;
    }

    static LabelNode findPreviousLabel(AbstractInsnNode node) {
        do {
            node = node.getPrevious();
        } while (node != null && node.getType() != LABEL);
        return (LabelNode) node;
    }

    static LabelNode findNextLabel(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node != null && node.getType() != LABEL);
        return (LabelNode) node;
    }

    static boolean isStore(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return ISTORE <= opcode && opcode <= ASTORE;
    }

    static boolean isLoad(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        return ILOAD <= opcode && opcode <= ALOAD;
    }

    static boolean isReturn(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return IRETURN <= opcode && opcode <= RETURN;
    }

    static boolean isThrow(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return opcode == ATHROW;
    }

    static boolean defaultFinallyBlock(TryCatchBlockNode block) {
        // Default finally block has "null" throwable type instead of "Throwable".
        return block.type == null;
    }

    static String toBoxedInternalName(char type) {
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

    static int loadOpcode(char type) {
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

    static String toPrimitiveName(char type) {
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

    static String getMethodDescriptorForValueOf(char type) {
        return "(" + type + ")L" + toBoxedInternalName(type) + ";";
    }

    private static IllegalArgumentException illegalType(char type) {
        return new IllegalArgumentException(Character.toString(type));
    }
}
