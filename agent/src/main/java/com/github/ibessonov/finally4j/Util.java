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

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.LABEL;

/**
 * @author ibessonov
 */
class Util {

    static AbstractInsnNode findNextInstruction(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node.getOpcode() == -1);
        return node;
    }

    static LabelNode findNextLabel(AbstractInsnNode node) {
        do {
            node = node.getNext();
        } while (node != null && node.getType() != LABEL);
        return (LabelNode) node;
    }

    static AbstractInsnNode findPreviousInstruction(AbstractInsnNode node) {
        do {
            node = node.getPrevious();
        } while (node.getOpcode() == -1);
        return node;
    }

    static boolean isStoreInstruction(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return ISTORE <= opcode && opcode <= ASTORE;
    }

    static boolean isLoadInstruction(AbstractInsnNode instruction) {
        int opcode = instruction.getOpcode();
        return ILOAD <= opcode && opcode <= ALOAD;
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

    static String getValueOfMethodDescriptor(char type) {
        return "(" + type + ")L" + toBoxedInternalName(type) + ";";
    }

    private static IllegalArgumentException illegalType(char type) {
        return new IllegalArgumentException(Character.toString(type));
    }
}
