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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

import static com.github.ibessonov.finally4j.Util.ASM_V;
import static com.github.ibessonov.finally4j.Util.findNextInstruction;
import static com.github.ibessonov.finally4j.Util.findNextLabel;
import static com.github.ibessonov.finally4j.Util.findPreviousInstruction;
import static com.github.ibessonov.finally4j.Util.getValueOfMethodDescriptor;
import static com.github.ibessonov.finally4j.Util.isLoadInstruction;
import static com.github.ibessonov.finally4j.Util.isStoreInstruction;
import static com.github.ibessonov.finally4j.Util.loadOpcode;
import static com.github.ibessonov.finally4j.Util.toBoxedInternalName;
import static com.github.ibessonov.finally4j.Util.toPrimitiveName;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.tree.AbstractInsnNode.METHOD_INSN;

/**
 * @author ibessonov
 */
class FinallyMethodNode extends MethodNode {

    private final Runnable methodChanged;
    private final MethodVisitor outerMv;

    private final Map<LabelNode, Integer> labelsMap = new IdentityHashMap<>();
    private LabelNode[] labels = null;
    private VarInsnNode[] storeInstructions;
    private ReturnOrThrow[] returnOrThrows;

    FinallyMethodNode(Runnable methodChanged, MethodVisitor outerMv,
                      int access, String name, String desc, String signature, String[] exceptions) {
        super(ASM_V, access, name, desc, signature, exceptions);
        this.methodChanged = methodChanged;
        this.outerMv       = outerMv;
    }

    @Override
    public void visitEnd() {
        initLabelsCache();
        initStoreInstructions();

        List<VarInsnNode> storeStack = new LinkedList<>();
        List<ReturnOrThrow> rotStack = new LinkedList<>();
        int currentLabelIndex = -1;
        for (AbstractInsnNode node = super.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node.getType() == AbstractInsnNode.LABEL) {
                currentLabelIndex++;
            }
            if (currentLabelIndex >= 0 && storeInstructions[currentLabelIndex] != null) {
                storeStack.add(0, storeInstructions[currentLabelIndex]);
                rotStack.add(0, returnOrThrows[currentLabelIndex]);
            }
            if (!storeStack.isEmpty() && isLoadInstruction(node) && ((VarInsnNode) node).var == storeStack.get(0).var) {
                storeStack.remove(0);
                rotStack.remove(0);
            }
            if (node.getType() == METHOD_INSN && node.getOpcode() == INVOKESTATIC) {
                assert node instanceof MethodInsnNode;

                MethodInsnNode methodInstruction = (MethodInsnNode) node;
                if (methodInstruction.owner.equals(Constants.FINALLY_CLASS_INTERNAL_NAME)) {
                    switch (methodInstruction.name) {
                        case Constants.FINALLY_HAS_RETURN_VALUE_METHOD_NAME: {
                            int c = !rotStack.isEmpty() && rotStack.get(0) == ReturnOrThrow.RETURN ? ICONST_1 : ICONST_0;
                            node = replaceInstruction(methodInstruction, new InsnNode(c));
                            break;
                        }
                        case Constants.FINALLY_HAS_THROWN_EXCEPTION_METHOD_NAME: {
                            int c = !rotStack.isEmpty() && rotStack.get(0) == ReturnOrThrow.THROW ? ICONST_1 : ICONST_0;
                            node = replaceInstruction(methodInstruction, new InsnNode(c));
                            break;
                        }
                    }
                    if (!storeStack.isEmpty()) {
                        if (rotStack.get(0) == ReturnOrThrow.RETURN) {
                            node = processFinallyMethodWithReturn(node, methodInstruction, storeStack.get(0).var);
                        }
                    }
                }
            }
        }

        super.accept(outerMv);
    }

    private AbstractInsnNode processFinallyMethodWithReturn(AbstractInsnNode node, MethodInsnNode methodInstruction, int var) {
        if (methodInstruction.name.startsWith(Constants.FINALLY_GET_RETURN_VALUE_METHOD_PREFIX)) {
            char returnType = super.desc.charAt(super.desc.length() - 1);
            node = replaceInstruction(methodInstruction, new VarInsnNode(loadOpcode(returnType), var));

            assert returnType != 'V' : "Returning of void passed somehow";

            if (returnType == ';') {
                switch (methodInstruction.name) {
                    case Constants.FINALLY_GET_RETURN_VALUE_OPTIONAL_METHOD_NAME:
                        super.instructions.insert(node, optionalOfNullable());
                    case Constants.FINALLY_GET_RETURN_VALUE_METHOD_NAME:
                        break;
                    default:
                        char currentType = methodInstruction.desc.charAt(methodInstruction.desc.length() - 1);
                        String primitiveName = toPrimitiveName(currentType);
                        String boxedInternalName = toBoxedInternalName(currentType);

                        super.instructions.insert(node, new MethodInsnNode(INVOKEVIRTUAL,
                                boxedInternalName, primitiveName + "Value",
                                "()" + currentType, false));
                        super.instructions.insert(node, new TypeInsnNode(CHECKCAST, boxedInternalName));
                }
            } else { // method returns primitive type
                switch (methodInstruction.name) {
                    case Constants.FINALLY_GET_RETURN_VALUE_OPTIONAL_METHOD_NAME:
                        super.instructions.insert(node, optionalOf());
                    case Constants.FINALLY_GET_RETURN_VALUE_METHOD_NAME:
                        super.instructions.insert(node, valueOf(returnType));
                        break;
                    default:
                        char currentType = methodInstruction.desc.charAt(methodInstruction.desc.length() - 1);
                        if (currentType != returnType) {
                            // old method invocation left untouched to preserve bytecode consistency
                            node = replaceInstruction(node, methodInstruction);

                            String message = toPrimitiveName(currentType) + " cannot be cast to " + toPrimitiveName(returnType);

                            // bytecode for "throw new ClassCastException(message);"
                            super.instructions.insertBefore(node, new TypeInsnNode(NEW, "java/lang/ClassCastException"));
                            super.instructions.insertBefore(node, new InsnNode(DUP));
                            super.instructions.insertBefore(node, new LdcInsnNode(message));
                            super.instructions.insertBefore(node, new MethodInsnNode(INVOKESPECIAL, "java/lang/ClassCastException", "<init>", "(Ljava/lang/String;)V", false));
                            super.instructions.insertBefore(node, new InsnNode(ATHROW));
                        }
                }
            }
        }
        return node;
    }

    private MethodInsnNode optionalOfNullable() {
        return new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "ofNullable", "(Ljava/lang/Object;)Ljava/util/Optional;", false);
    }

    private MethodInsnNode valueOf(char returnTypeDescriptor) {
        return new MethodInsnNode(INVOKESTATIC, toBoxedInternalName(returnTypeDescriptor),
                "valueOf", getValueOfMethodDescriptor(returnTypeDescriptor),false);
    }

    private MethodInsnNode optionalOf() {
        return new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "of", "(Ljava/lang/Object;)Ljava/util/Optional;",false);
    }

    //todo not finished
    @SuppressWarnings("unchecked")
    private void initStoreInstructions() {
        boolean[] catches = new boolean[labels.length];
        for (TryCatchBlockNode block : super.tryCatchBlocks) {
            if (block.start == block.handler) {
                continue; // nasty bug in compiler :D
            }
            if (block.type != null) { // not a default finally block
                catches[getLabelIndex(block.handler)] = true;
                if (block.end == block.handler) {
                    catches[getLabelIndex(block.start)] = true;
                }
            }
        }

        Map<Integer, IntBorelSet> tcfbs = new HashMap<>();

        boolean[] finallies = new boolean[labels.length];
        List<Block>[] catchFinallyBlocks = new List[labels.length];
        List<Block>[] tryFinallyBlocks = new List[labels.length];
        for (TryCatchBlockNode block : super.tryCatchBlocks) {
            if (block.start == block.handler) {
                continue; // nasty bug in compiler :D
            }
            if (block.type == null) { // default finally block
                int tryOrCatchIndex = getLabelIndex(block.start);
                int tryOrCatchEndIndex = getLabelIndex(block.end);
                int handlerIndex = getLabelIndex(block.handler);
                finallies[handlerIndex] = true;
                if (catches[tryOrCatchIndex]) {
                    if (catchFinallyBlocks[handlerIndex] == null) {
                        catchFinallyBlocks[handlerIndex] = new ArrayList<>();
                    }
                    catchFinallyBlocks[handlerIndex].add(new Block(block.start, block.end));
                } else {
                    if (tryFinallyBlocks[handlerIndex] == null) {
                        tryFinallyBlocks[handlerIndex] = new ArrayList<>();
                    }
                    tryFinallyBlocks[handlerIndex].add(new Block(block.start, block.end));
                }

                tcfbs.computeIfAbsent(handlerIndex, i -> new IntBorelSet())
                        .add(tryOrCatchIndex, tryOrCatchEndIndex)
                        .add(handlerIndex, handlerIndex); //?
            }
        }
        // detection of finally blocks boundaries
        for (Map.Entry<Integer, IntBorelSet> entry : tcfbs.entrySet()) {
            AbstractInsnNode instruction = findNextInstruction(getLabel(entry.getKey()));
            if (isStoreInstruction(instruction)) {
                VarInsnNode storeInstruction = (VarInsnNode) instruction;
                int index = -1;
                for (AbstractInsnNode node = storeInstruction; node != null; node = node.getNext()) {
                    if (isLoadInstruction(node) && ((VarInsnNode) node).var == storeInstruction.var) {
                        LabelNode label = findNextLabel(node);
                        index = label == null ? labels.length : getLabelIndex(label);
                        break;
                    }
                }
                if (index == -1) {
                    throw new RuntimeException("There's no ALOAD instruction in the end of final finally block");
                }
                entry.getValue().add(index, index);
            } else {
                throw new RuntimeException("There's no ASTORE instruction in the beginning of final finally block");
            }
        }
//        for (TryCatchBlockNode block : (List<TryCatchBlockNode>) super.tryCatchBlocks) {
//            if (block.start == block.handler) {
//                continue; // nasty bug in compiler :D
//            }
//            if (block.type != null) { // not a default finally block
//
////                catches[getLabelIndex(block.handler)] = true;
////                if (block.end == block.handler) {
////                    catches[getLabelIndex(block.start)] = true;
////                }
//            }
//        }
        System.out.println("========================================");
        System.out.println(super.name);
        // tcfbs maps "finally block label index" to the set of intervals that lead to this finally block.
        Map<Integer, Integer> catchBlocksIndexes = new HashMap<>();
        for (Map.Entry<Integer, IntBorelSet> entry : tcfbs.entrySet()) {
            System.out.println(entry.getValue());
            System.out.println("Default finally block at " + entry.getKey());
//            System.out.println(isStoreInstruction(findNextInstruction(getLabel(entry.getKey()))));
            for (TryCatchBlockNode block : super.tryCatchBlocks) {
                if (block.start == block.handler) {
                    continue; // indicates place where exception is stored
                }
                if (block.type != null) { // not a default finally block
//                    if (entry.getValue().convexHaul().l == getLabelIndex(block.start)/* && entry.getValue().convexHaul().r > getLabelIndex(block.end)*/) {
                    int handlerIndex = getLabelIndex(block.handler);
                    int startIndex = (block.end == block.handler) ? getLabelIndex(block.start) : handlerIndex;
                    catchBlocksIndexes.put(startIndex, handlerIndex);
//                        System.out.println("Catch block at " + getLabelIndex(block.handler)); // WRONG??? Maybe not
//                        System.out.println(isStoreInstruction(findNextInstruction(block.handler)));
//                    }
                }
            }
        }
        System.out.println("Catch blocks at " + catchBlocksIndexes);

        class Info {
            public final ReturnOrThrow rot;
            public final int startIndex;

            public Info(ReturnOrThrow rot, int startIndex) {
                this.rot = rot;
                this.startIndex = startIndex;
            }
        }

        Map<IntBorelSet.IntSegment, Info> rotMap = new HashMap<>();
        for (Map.Entry<Integer, IntBorelSet> entry : tcfbs.entrySet()) {
            int finalHandler = entry.getKey();
            IntBorelSet info = entry.getValue();

            IntBorelSet.IntSegment[] segments = info.segments();
            int length = segments.length;

            rotMap.put(new IntBorelSet.IntSegment(segments[length - 2].r, segments[length - 2].l), new Info(ReturnOrThrow.THROW, segments[length - 2].r));
//            for (int i = 0; i < length - 2; i++) {
//
//            }
        }

        storeInstructions = new VarInsnNode[labels.length];
        returnOrThrows = new ReturnOrThrow[labels.length];

        for (int i = 0; i < labels.length; i++) {
            if (finallies[i]) {
                AbstractInsnNode instruction = findNextInstruction(getLabel(i));
                if (isStoreInstruction(instruction)) {
                    VarInsnNode storeInstruction = (VarInsnNode) instruction;
                    storeInstructions[i] = storeInstruction;
                    returnOrThrows[i] = ReturnOrThrow.THROW;
                }
            }
        }
        for (List<Block> catchBlockList : catchFinallyBlocks) {
            if (catchBlockList == null) continue;

            for (Block block : catchBlockList) {
                AbstractInsnNode instruction = findNextInstruction(block.end); // or prev?
                if (isStoreInstruction(instruction)) {
                    VarInsnNode storeInstruction = (VarInsnNode) instruction;

                    int i = getLabelIndex(block.end);
                    storeInstructions[i] = storeInstruction;
                    returnOrThrows[i] = ReturnOrThrow.THROW;
                }
            }
        }

        for (List<Block> tryBlockList : tryFinallyBlocks) {
            if (tryBlockList == null) continue;

            for (Block block : tryBlockList) {
                AbstractInsnNode instruction = findPreviousInstruction(block.end);
                if (isStoreInstruction(instruction)) {
                    VarInsnNode storeInstruction = (VarInsnNode) instruction;

                    int i = getLabelIndex(block.end);
                    storeInstructions[i] = storeInstruction;
                    returnOrThrows[i] = ReturnOrThrow.RETURN;
                }
            }
        }
    }

    private AbstractInsnNode replaceInstruction(AbstractInsnNode from, AbstractInsnNode to) {
        super.instructions.insertBefore(from, to);
        super.instructions.remove(from);

        methodChanged.run();

        return to;
    }

    @Deprecated
    private final class Block {

        final LabelNode start;
        final LabelNode end;

        Block(LabelNode start, LabelNode end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Block block = (Block) o;

            return start.equals(block.start) && end.equals(block.end);
        }

        @Override
        public int hashCode() {
            return 31 * start.hashCode() + end.hashCode();
        }
    }

    private enum ReturnOrThrow {
        RETURN, THROW
    }

    private void initLabelsCache() {
        for (AbstractInsnNode node = super.instructions.getFirst(); node != null; node = node.getNext()) {
            if (node.getType() == AbstractInsnNode.LABEL) {
                labelsMap.put((LabelNode) node, labelsMap.size());
            }
        }

        labels = new LabelNode[labelsMap.size()];
        for (Map.Entry<LabelNode, Integer> entry : labelsMap.entrySet()) {
            labels[entry.getValue()] = entry.getKey();
        }
    }

    private LabelNode getLabel(int index) {
        return labels[index];
    }

    private int getLabelIndex(LabelNode label) {
        return labelsMap.get(label);
    }

    private static class IntBorelSet {

        public static class IntSegment implements Comparable<IntSegment> {

            public final int l;
            public final int r;

            public IntSegment(int l, int r) {
                this.l = l;
                this.r = r;
            }

            public boolean contains(int i) {
                return l <= i && i <= r;
            }

            @Override
            public int compareTo(IntSegment o) {
                int c = Integer.compare(l, o.l);
                if (c == 0) {
                    c = Integer.compare(r, o.r);
                }
                return c;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                IntSegment that = (IntSegment) o;
                return l == that.l && r == that.r;
            }

            @Override
            public int hashCode() {
                return l ^ r;
            }

            @Override
            public String toString() {
                return "[" + l + ", " + r + ")";
            }
        }

        private final NavigableSet<IntSegment> segments = new TreeSet<>();

        public IntBorelSet add(int l, int r) {
            IntSegment c = new IntSegment(l, r);
            if (!segments.contains(c)) {

                IntSegment lower = segments.lower(c);
                if (lower != null && lower.r >= l) {
                    segments.remove(lower);
                    c = new IntSegment(lower.l, r);
                }

                IntSegment higher = segments.higher(c);
                if (higher != null && higher.l <= r) {
                    segments.remove(higher);
                    c = new IntSegment(l, higher.r);
                }

                segments.add(c);
            }
            return this;
        }

        public boolean contains(int i) {
            IntSegment c = new IntSegment(i, Integer.MAX_VALUE);
            IntSegment lower = segments.lower(c);
            return lower != null && i < lower.r;
        }

        public IntSegment convexHaul() {
            if (segments.isEmpty()) {
                throw new UnsupportedOperationException();
            }
            return new IntSegment(segments.first().l, segments.last().r);
        }

        public IntSegment[] segments() {
            return segments.toArray(new IntSegment[] {});
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && segments.equals(((IntBorelSet) o).segments);
        }

        @Override
        public int hashCode() {
            return segments.hashCode();
        }

        @Override
        public String toString() {
            return segments.toString();
        }
    }
}
