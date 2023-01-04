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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.ibessonov.finally4j.FinallyAgentPreMain.DEBUG;
import static com.github.ibessonov.finally4j.Util.ASM_V;
import static com.github.ibessonov.finally4j.Util.findNextInstruction;
import static com.github.ibessonov.finally4j.Util.findNextLabel;
import static com.github.ibessonov.finally4j.Util.findPreviousInstruction;
import static com.github.ibessonov.finally4j.Util.findPreviousLabel;
import static com.github.ibessonov.finally4j.Util.getMethodDescriptorForValueOf;
import static com.github.ibessonov.finally4j.Util.isLoad;
import static com.github.ibessonov.finally4j.Util.isReturn;
import static com.github.ibessonov.finally4j.Util.isStore;
import static com.github.ibessonov.finally4j.Util.isThrow;
import static com.github.ibessonov.finally4j.Util.loadOpcode;
import static com.github.ibessonov.finally4j.Util.toBoxedInternalName;
import static com.github.ibessonov.finally4j.Util.toPrimitiveName;
import static java.util.Comparator.comparingInt;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GOTO;
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

    FinallyMethodNode(Runnable methodChanged, MethodVisitor outerMv,
                      int access, String name, String desc, String signature, String[] exceptions) {
        super(ASM_V, access, name, desc, signature, exceptions);
        this.methodChanged = methodChanged;
        this.outerMv       = outerMv;
    }

    @Override
    public void visitEnd() {
        initLabelsCache();

        List<Try> tryList;

        try {
            tryList = inferTheStructure();
        } catch (Throwable e) {
            e.printStackTrace();

            throw e;
        }

        if (tryList.isEmpty()) {
            super.accept(outerMv);

            return;
        }

        if (DEBUG) {
            System.out.println("  Transforming method '" + super.name + super.desc + "':");

            tryList.forEach(aTry -> aTry.print("    "));
        }

        for (Try aTry : tryList) {
            Scope tryScope = aTry.tryScope;

            for (int i = 0; i < tryScope.blocks.size(); i++) {
                Block finallyBlock; {
                    Block tryBlock = tryScope.blocks.get(i);

                    Block nextBlock;

                    if (tryBlock == tryScope.last()) {
                        /*
                         * You can't just read the next catch or finally block, that would be too easy, right?
                         * That's because 2 last finally blocks can be slapped together, for example, here:
                         * try {
                         *     if (foo()) return bar();
                         * } finally {
                         *     baz();
                         * }
                         * It means that the "last" finally block must be split manually.
                         */
                        //TODO Here we may have a false-positive detection of a "return" finally block.
                        // nextBlock = aTry.catchScopes.isEmpty() ? aTry.finallyScope.first() : aTry.catchScopes.get(0).first();
                        //TODO Of there's no return in "try" at all, we may fail on assertion inside of the next line.
                        // How to avoid it: if default finally doesn't end with "return", we can derive that the
                        // "load" and "return" at the end mean that the block is in fact a return block.
                        // Checking the first "store" is DEFINITELY NOT ENOUGH.
                        LabelNode theEndOfFinally = findTheEndOfFinally(tryBlock.end, false);

                        nextBlock = new Block(theEndOfFinally, null);
                    } else {
                        nextBlock = tryScope.blocks.get(i + 1);
                    }

                    finallyBlock = new Block(tryBlock.end, nextBlock.start);
                }

                if (finallyBlock.startIndex() >= finallyBlock.endIndex()) {
                    continue;
                }

                //TODO This code is bad. It doesn't cover nested stuff at all.
                System.out.println("  Finally block in try " + finallyBlock);
                AbstractInsnNode previousInstruction = findPreviousInstruction(finallyBlock.start);
                //TODO Not the best way to find "return" finally blocks.
                // May be false-positive if there's a simple assignment at the end.
                // One way to avoid that would be checking the end of "finally" block for corresponding "load" instruction.
                // But, it only works when the finally block itself doesn't have a return or throw at the end, that is
                // the worst case scenario. There's no way to tell the difference then. I guess I just document it and that's it.
                if (isStore(previousInstruction)) {
                    VarInsnNode storeInstruction = (VarInsnNode) previousInstruction;

                    for (AbstractInsnNode node = finallyBlock.start; node != finallyBlock.end; node = node.getNext()) {
                        if (node.getType() == METHOD_INSN && node.getOpcode() == INVOKESTATIC) {
                            assert node instanceof MethodInsnNode;

                            MethodInsnNode methodInstruction = (MethodInsnNode) node;
                            if (methodInstruction.owner.equals(Constants.FINALLY_CLASS_INTERNAL_NAME)) {
                                switch (methodInstruction.name) {
                                    case Constants.FINALLY_HAS_RETURN_VALUE_METHOD_NAME: {
                                        node = replaceInstruction(methodInstruction, new InsnNode(ICONST_1));
                                        break;
                                    }
                                    case Constants.FINALLY_HAS_THROWN_EXCEPTION_METHOD_NAME: {
                                        node = replaceInstruction(methodInstruction, new InsnNode(ICONST_0));
                                        break;
                                    }
                                    default:
                                        node = processFinallyMethodWithReturn(node, methodInstruction, storeInstruction.var);
                                }
                            }
                        }
                    }
                }
            }
        }

        if (DEBUG) {
            System.out.println();
        }

        super.accept(outerMv);
    }

    static class Try {
        final Scope tryScope = new Scope();

        final List<Scope> catchScopes = new ArrayList<>();

        final Scope finallyScope = new Scope();

        void print(String padding) {
            String nestedPadding = padding + "  ";

            System.out.println(padding + "try " + tryScope.blocks + " {");
            for (Try nestedTry : tryScope.nested) {
                nestedTry.print(nestedPadding);
            }

            for (Scope catchScope : catchScopes) {
                System.out.println(padding + "} catch " + catchScope.blocks + " {");
                for (Try nestedTry : catchScope.nested) {
                    nestedTry.print(nestedPadding);
                }
            }

            System.out.println(padding + "} finally " + finallyScope.first() + " {");
            for (Try nestedTry : finallyScope.nested) {
                nestedTry.print(nestedPadding);
            }

            System.out.println(padding + "}");
        }
    }

    static class Scope {
        final List<Block> blocks = new ArrayList<>();

        final List<Try> nested = new ArrayList<>();

        boolean surrounds(Try aTry) {
            Block first = first();
            Block last = last();

            Block tryFirst = aTry.tryScope.first();
            Block tryLast = aTry.finallyScope.last();

            return first.startIndex() <= tryFirst.startIndex() && tryLast.endIndex() <= last.endIndex();
        }

        private Block first() {
            return blocks.get(0);
        }

        private Block last() {
            return blocks.get(blocks.size() - 1);
        }
    }

    private List<Try> inferTheStructure() {
//        for (TryCatchBlockNode block : super.tryCatchBlocks) {
//            System.out.println("try-catch-block: " + getLabelIndex(block.start) + " " + getLabelIndex(block.end) + " " + getLabelIndex(block.handler) + " (" + block.type + ")");
//
//            if (skipBlock(block)/* || getLabelIndex(block.end) > getLabelIndex(block.handler)*/) {
//                AbstractInsnNode instruction = findPreviousInstruction(findNextLabel(block.end));
//
//                System.out.println("Handler opcode = " + instruction.getOpcode());
//            }
//        }

        var blocksGroupedByHandler = tryCatchBlocks.stream()
                .filter(not(FinallyMethodNode::skipBlock))
                .filter(not(Util::defaultFinallyBlock))
                .collect(Collectors.groupingBy(block -> block.handler));

        var blocksGroupedByDefaultHandler = tryCatchBlocks.stream()
                .filter(not(FinallyMethodNode::skipBlock))
                .filter(Util::defaultFinallyBlock)
                .collect(Collectors.groupingBy(block -> block.handler));

        concat(blocksGroupedByHandler.values().stream(), blocksGroupedByDefaultHandler.values().stream())
                .forEach(list -> list.sort(comparingInt(node -> getLabelIndex(node.start))));

        class CatchBlockX {
            final String type;
            final LabelNode start;

            CatchBlockX(String type, LabelNode start) {
                this.type = type;
                this.start = start;
            }
        }

        Map<List<Block>, List<CatchBlockX>> catchBlocksMap = new HashMap<>();
        for (Map.Entry<LabelNode, List<TryCatchBlockNode>> entry : blocksGroupedByHandler.entrySet()) {
            List<TryCatchBlockNode> list = entry.getValue();

            List<Block> blocks = list.stream().map(node -> new Block(node.start, node.end)).collect(toList());

            catchBlocksMap.computeIfAbsent(blocks, b -> new ArrayList<>())
                    .add(new CatchBlockX(list.get(0).type, entry.getKey()));
        }

        List<Try> tempTryList = new ArrayList<>();

        for (Map.Entry<LabelNode, List<TryCatchBlockNode>> entry : blocksGroupedByDefaultHandler.entrySet()) {
            List<TryCatchBlockNode> list = entry.getValue();

            List<Block> blocks = list.stream().map(node -> new Block(node.start, node.end)).collect(toList());

            LabelNode nextLabel = findTheEndOfFinally(entry.getKey(), true);

            boolean found = false;

            for (Map.Entry<List<Block>, List<CatchBlockX>> catchBlocksMapEntry : catchBlocksMap.entrySet()) {
                List<Block> prefix = catchBlocksMapEntry.getKey();

                if (startsWith(blocks, prefix)) {
                    List<CatchBlockX> value = catchBlocksMapEntry.getValue();
                    value.sort(comparingInt(cb -> getLabelIndex(cb.start)));

                    Try newTry = new Try();

                    newTry.tryScope.blocks.addAll(blocks.subList(0, prefix.size()));

                    List<Block> allCatchSegments = blocks.subList(prefix.size(), blocks.size());

                    assert value.get(0).start == allCatchSegments.get(0).start;
                    Iterator<CatchBlockX> valueIterator = value.iterator();
                    valueIterator.next();

                    CatchBlockX nextCatch = valueIterator.hasNext() ? valueIterator.next() : null;
                    Scope cur = new Scope();
                    for (Block smallCatchSegment : allCatchSegments) {
                        if (nextCatch != null && smallCatchSegment.start == nextCatch.start) {
                            newTry.catchScopes.add(cur);

                            nextCatch = valueIterator.hasNext() ? valueIterator.next() : null;
                            cur = new Scope();
                        }

                        cur.blocks.add(smallCatchSegment);
                    }
                    newTry.catchScopes.add(cur);

                    newTry.finallyScope.blocks.add(new Block(entry.getKey(), nextLabel));

                    catchBlocksMap.remove(prefix);

                    tempTryList.add(newTry);

                    found = true;
                    break;
                }
            }

            if (!found) {
                Try newTry = new Try();

                // No catch blocks.
                newTry.tryScope.blocks.addAll(blocks);
                newTry.finallyScope.blocks.add(new Block(entry.getKey(), nextLabel));

                tempTryList.add(newTry);

//                catchBlocksMap.put(blocks, List.of(finallyCatchBlockX));
            }
        }

//        // catchBlocksMap now has all try-catch blocks without finally. They are irrelevant.
//        // Or are they? What if there's a try-finally inside of catch block? That would be bad.
//        for (Map.Entry<List<Block>, List<CatchBlockX>> e : catchBlocksMap.entrySet()) {
//            System.out.println("Unaccounted try-catch: " + e.getKey() + " -> " + e.getValue().stream().map(c -> getLabelIndex(c.start)).collect(toList()));
//            for (Block tryBlock : e.getKey()) {
//                AbstractInsnNode nextInstruction = findNextInstruction(tryBlock.end);
//                if (nextInstruction.getOpcode() == GOTO) {
//                    System.out.println("End label is most likely " + getLabelIndex(((JumpInsnNode) nextInstruction).label));
//                }
//            }
//        }

        tempTryList.sort(comparingInt((Try t) -> t.finallyScope.blocks.get(0).endIndex() - t.tryScope.blocks.get(0).startIndex())
                .thenComparingInt(t -> t.tryScope.blocks.get(0).startIndex())
        );

        List<Try> tryList = new ArrayList<>();

        tempTryListLoop:
        while (!tempTryList.isEmpty()) {
            Try first = tempTryList.remove(0);

            for (Try nextTry : tempTryList) {
                if (nextTry.tryScope.surrounds(first)) {
                    nextTry.tryScope.nested.add(first);

                    continue tempTryListLoop;
                }

                for (Scope catchScope : nextTry.catchScopes) {
                    if (catchScope.surrounds(first)) {
                        catchScope.nested.add(first);

                        continue tempTryListLoop;
                    }
                }

                if (nextTry.finallyScope.surrounds(first)) {
                    nextTry.finallyScope.nested.add(first);

                    continue tempTryListLoop;
                }
            }

            tryList.add(first);
        }

        return tryList;
    }

    private LabelNode findTheEndOfFinally(LabelNode startLabel, boolean defaultBlock) {
        AbstractInsnNode instruction = defaultBlock
                ? findNextInstruction(startLabel)
                : findPreviousInstruction(startLabel);
        assert isStore(instruction) : "No store instruction at the beginning of default finally block.";

        var store = (VarInsnNode) instruction;

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
                    System.out.println("  Jump to " + getLabelIndex(label) + ". Opcode = " + findNextInstruction(label).getOpcode());
                }

                continue;
            }

            //noinspection DataFlowIssue
            if (isLoad(instruction) && ((VarInsnNode) instruction).var == store.var) {
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

    private boolean startsWith(List<Block> blocks, List<Block> prefix) {
        if (blocks.size() < prefix.size()) {
            return false;
        }

        return blocks.subList(0, prefix.size()).equals(prefix);
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
                "valueOf", getMethodDescriptorForValueOf(returnTypeDescriptor),false);
    }

    private MethodInsnNode optionalOf() {
        return new MethodInsnNode(INVOKESTATIC, "java/util/Optional",
                "of", "(Ljava/lang/Object;)Ljava/util/Optional;",false);
    }

    private static boolean skipBlock(TryCatchBlockNode block) {
        // Nasty bug in compiler?
        return block.start == block.handler;
    }

    private AbstractInsnNode replaceInstruction(AbstractInsnNode from, AbstractInsnNode to) {
        super.instructions.insertBefore(from, to);
        super.instructions.remove(from);

        methodChanged.run();

        return to;
    }

    private final class Block {
        final LabelNode start;
        final LabelNode end;

        Block(LabelNode start, LabelNode end) {
            this.start = start;
            this.end = end;
        }

        int startIndex() {
            return getLabelIndex(start);
        }

        int endIndex() {
            return getLabelIndex(end);
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
            return start.hashCode() ^ end.hashCode();
        }

        @Override
        public String toString() {
            return "[" + getLabelIndex(start) + ", " + (end == null ? "end" : getLabelIndex(end)) + ")";
        }
    }

    private enum ReturnOrThrow {
        RETURN, THROW, NOOP
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
}
