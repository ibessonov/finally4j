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
package com.github.ibessonov.finally4j.agent.transformer;

import com.github.ibessonov.finally4j.agent.transformer.code.Block;
import com.github.ibessonov.finally4j.agent.transformer.code.Scope;
import com.github.ibessonov.finally4j.agent.transformer.code.Try;
import com.github.ibessonov.finally4j.agent.transformer.util.Replacer;
import com.github.ibessonov.finally4j.agent.transformer.util.Util;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.ibessonov.finally4j.agent.transformer.code.Block.startsWith;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.ASM_V;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.DEBUG;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.findNextInstruction;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.findNextLabel;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.findPreviousInstruction;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.findPreviousLabel;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.isLoad;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.isReturn;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.isStore;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.isThrow;
import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static org.objectweb.asm.Opcodes.GOTO;

/**
 * @author ibessonov
 */
public class FinallyMethodNode extends MethodNode {
    /**
     * Closure that must be invoked when the method is transformed by this class. Used to notify external class visitor.
     */
    private final Runnable methodTransformedClosure;

    /**
     * Delegate method visitor to generate bytecode.
     */
    private final MethodVisitor outerMv;

    /**
     * Mapping of all labels into their sequence number.
     */
    public final Map<LabelNode, Integer> labelIdx = new IdentityHashMap<>();


    private final Replacer replacer;

    FinallyMethodNode(MethodVisitor outerMv, Runnable methodTransformedClosure,
                      int access, String name, String desc, String signature, String[] exceptions) {
        super(ASM_V, access, name, desc, signature, exceptions);
        this.outerMv = outerMv;
        this.methodTransformedClosure = methodTransformedClosure;

        this.replacer = new Replacer(instructions, methodTransformedClosure, desc.charAt(desc.length() - 1));
    }

    @Override
    public void visitEnd() {
        // Calculate indexes for all labels in the method.
        Stream.iterate(super.instructions.getFirst(), Objects::nonNull, AbstractInsnNode::getNext)
                .filter(node -> node.getType() == AbstractInsnNode.LABEL)
                .forEach(node -> labelIdx.put((LabelNode) node, labelIdx.size()));

        List<Try> tryList = initTryList();

        // Avoid logs and return if there are no finally blocks in the method.
        if (tryList.isEmpty()) {
            super.accept(outerMv);

            return;
        }

        if (DEBUG) {
            System.out.println("  Transforming method '" + super.name + super.desc + "':");

            for (TryCatchBlockNode node : tryCatchBlocks) {
                System.out.println("    raw block: [s=" + labelIdx.get(node.start) + ", e=" + labelIdx.get(node.end) + ", h=" + labelIdx.get(node.handler) + ", t=" + node.type + "]");
            }
            System.out.println("   ---");

            var invalidBlocks = tryCatchBlocks.stream()
                    .filter(block -> !Util.validBlock(block)).map(block -> new Block(this, block).toString())
                    .collect(toList());
            if (!invalidBlocks.isEmpty()) {
                System.out.println("    Invalid blocks: " + invalidBlocks);
            }

            tryList.forEach(aTry -> aTry.print("    "));
        }

        for (Try aTry : tryList) {
            replaceInstructionsInTryBlock(aTry);
        }

        if (DEBUG) {
            // Don't remove it, it's an actual empty line, not a mistake.
            System.out.println();
        }

        super.accept(outerMv);
    }

    private void replaceInstructionsInTryBlock(Try aTry) {
        Stream<Block> returnFinallyBlocks = concat(Stream.of(aTry.tryScope), aTry.catchScopes.stream()).flatMap(scope -> {
            return IntStream.range(0, scope.blocks.size()).mapToObj(i -> {
                Block block = scope.blocks.get(i);

                Block nextBlock;

                if (block == scope.last()) {
                    if (!isStore(findPreviousInstruction(block.end))) { //ATHROW
                        return null; // This one should end with exceptional finally block, so I ignore it.
                    }

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
                    // How to avoid it: if default finally doesn't end with "return", we can derive that the
                    // "load" and "return" at the end mean that the block is in fact a return block.
                    // Checking the first "store" is DEFINITELY NOT ENOUGH.
                    LabelNode theEndOfFinally = findTheEndOfFinally(block.end, false);

                    nextBlock = new Block(this, theEndOfFinally, null);
                } else {
                    nextBlock = scope.blocks.get(i + 1);
                }

                return new Block(this, block.end, nextBlock.start);
            });
        }).filter(Objects::nonNull).filter(b -> b.startIndex() < b.endIndex());

        returnFinallyBlocks.forEach(finallyBlock -> {
            //TODO This code is bad. It doesn't cover nested stuff at all.
            if (DEBUG) {
                System.out.println("  Finally block in try " + finallyBlock);
            }
            AbstractInsnNode previousInstruction = findPreviousInstruction(finallyBlock.start);
            //TODO Not the best way to find "return" finally blocks.
            // May be false-positive if there's a simple assignment at the end.
            // One way to avoid that would be checking the end of "finally" block for corresponding "load" instruction.
            // But, it only works when the finally block itself doesn't have a return or throw at the end, that is
            // the worst case scenario. There's no way to tell the difference then. I guess I just document it and that's it.
            if (isStore(previousInstruction)) {
                VarInsnNode storeInstruction = (VarInsnNode) previousInstruction;

                replacer.replaceReturnedValueInstructions(storeInstruction, finallyBlock);
            }
        });

        // No exceptions.
        IntStream.range(0, aTry.catchScopes.size()).forEach(i -> {
            Scope catchScope = aTry.catchScopes.get(i);

            Block lastBlock = catchScope.last();

            LabelNode startLabel = lastBlock.end;

            LabelNode endLabel = i == aTry.catchScopes.size() - 1
                    ? aTry.finallyScope.first().start
                    : aTry.catchScopes.get(i + 1).first().start;

            if (isStore(findPreviousInstruction(lastBlock.end))) {
                LabelNode theEndOfFinally = findTheEndOfFinally(lastBlock.end, false);

                if (theEndOfFinally == endLabel) {
                    return;
                } else {
                    startLabel = theEndOfFinally;
                }
            }

            AbstractInsnNode firstCatchInstruction = findNextInstruction(catchScope.first().start);

            assert isStore(firstCatchInstruction);

            var storeInstruction = (VarInsnNode) firstCatchInstruction;
            Block finallyBlock = new Block(this, startLabel, endLabel);

            replacer.replaceExceptionInstructions(storeInstruction, finallyBlock);
        });

        replacer.replaceExceptionInstructions((VarInsnNode) findNextInstruction(aTry.finallyScope.first().start), aTry.finallyScope.first());

        // Recursion!
        Stream<Try> nestedInTry = aTry.tryScope.nested.stream();
        Stream<Try> nestedInCatch = aTry.catchScopes.stream().map(scope -> scope.nested.stream()).flatMap(identity());
        Stream<Try> nestedInFinally = aTry.finallyScope.nested.stream();

        Stream.of(nestedInTry, nestedInCatch, nestedInFinally)
                .flatMap(identity())
                .forEach(this::replaceInstructionsInTryBlock);
    }

    /**
     * Splits "merged" default catch blocks apart. For example, in this case: <pre>
     * try {
     *     throw new Exception();
     * } catch (Exception e) {
     *     foo();
     * } finally {
     *     bar();
     * }
     * </pre>
     * the block for {@code finally} handler would include both {@code try} and {@code catch} sections, but we want to
     * process them separately.
     */
    private Stream<TryCatchBlockNode> splitTryCatchBlockNode(List<TryCatchBlockNode> mergedTryCatchBlocks, TryCatchBlockNode block) {
        return mergedTryCatchBlocks.stream()
                .filter(b -> b.start == block.start && labelIdx.get(b.end) < labelIdx.get(block.end))
                //TODO I need a good comment about why there cannot be two blocks that satisfy the condition.
                // Seems arbitrary, you know.
                .findAny()
                .map(b -> concat(
                        Stream.of(new TryCatchBlockNode(block.start, b.end, block.handler, block.type)),
                        //TODO Check if I need a recursion here. It depends on the exception variable index in catch
                        // blocks that belong to the same try. It's probably the same for all of them, but who knows.
                        Stream.of(new TryCatchBlockNode(b.end, block.end, block.handler, block.type))
                )).orElse(
                        Stream.of(block)
                );
    }

    private List<Try> initTryList() {
        // For some reason, there might be an intersection between start/end scope and the handler.
        // Here I normalize such blocks by moving end to the handler position.
        List<TryCatchBlockNode> tryCatchBlocks = this.tryCatchBlocks.stream()
                .filter(Util::validBlock)
                .map(b -> labelIdx.get(b.end) <= labelIdx.get(b.handler) ? b
                        : new TryCatchBlockNode(b.start, b.handler, b.handler, b.type)
                ).collect(toList());

        // All "TryCatchBlockNode" instances that represent catch blocks, grouped by handler labels.
        var blocksGroupedByHandler = tryCatchBlocks.stream()
                .filter(Util::regularCatch)
                .collect(groupingBy(block -> block.handler));

        // List of all "TryCatchBlockNode" instances, for which the end label of the block matches the handler label.
        var mergedTryCatchBlocks = tryCatchBlocks.stream()
                .filter(Util::regularCatch)
                .filter(b -> b.end == b.handler)
                .collect(toList());

        // All "TryCatchBlockNode" instances that represent finally blocks, grouped by handler labels.
        var blocksGroupedByDefaultHandler = tryCatchBlocks.stream()
                .filter(Util::defaultCatch)
                .flatMap(block -> splitTryCatchBlockNode(mergedTryCatchBlocks, block))
                .collect(groupingBy(block -> block.handler));

        // Sort values in "blocksGroupedByHandler" and "blocksGroupedByDefaultHandler" to simplify matching them.
        concat(blocksGroupedByHandler.values().stream(), blocksGroupedByDefaultHandler.values().stream())
                .forEach(list -> list.sort(comparingInt(node -> labelIdx.get(node.start))));

        // Maps "try" sections to lists of corresponding "catch" sections. Without finally blocks.
        var catchBlocksMap = blocksGroupedByHandler.entrySet().stream().collect(
                groupingBy(entry -> entry.getValue().stream().map(node -> new Block(this, node)).collect(toList()),
                mapping(Entry::getKey, toList()))
        );

        List<Try> tempTryList = new ArrayList<>();

        for (Entry<LabelNode, List<TryCatchBlockNode>> entry : blocksGroupedByDefaultHandler.entrySet()) {
            List<TryCatchBlockNode> list = entry.getValue();

            List<Block> blocks = list.stream().map(node -> new Block(this, node.start, node.end)).collect(toList());

            LabelNode nextLabel = findTheEndOfFinally(entry.getKey(), true);

            boolean found = false;

            for (Entry<List<Block>, List<LabelNode>> catchBlocksMapEntry : catchBlocksMap.entrySet()) {
                List<Block> prefix = catchBlocksMapEntry.getKey();

                if (startsWith(blocks, prefix)) {
                    List<LabelNode> value = catchBlocksMapEntry.getValue();
                    value.sort(comparingInt(label -> labelIdx.get(label)));

                    Try newTry = new Try();

                    newTry.tryScope.blocks.addAll(blocks.subList(0, prefix.size()));

                    List<Block> allCatchSegments = blocks.subList(prefix.size(), blocks.size());

                    assert value.get(0) == allCatchSegments.get(0).start;
                    Iterator<LabelNode> valueIterator = value.iterator();
                    valueIterator.next();

                    LabelNode nextCatch = valueIterator.hasNext() ? valueIterator.next() : null;
                    Scope cur = new Scope();
                    for (Block smallCatchSegment : allCatchSegments) {
                        if (smallCatchSegment.start == nextCatch) {
                            newTry.catchScopes.add(cur);

                            nextCatch = valueIterator.hasNext() ? valueIterator.next() : null;
                            cur = new Scope();
                        }

                        cur.blocks.add(smallCatchSegment);
                    }
                    newTry.catchScopes.add(cur);

                    newTry.finallyScope.blocks.add(new Block(this, entry.getKey(), nextLabel));

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
                newTry.finallyScope.blocks.add(new Block(this, entry.getKey(), nextLabel));

                tempTryList.add(newTry);

//                catchBlocksMap.put(blocks, List.of(finallyCatchBlockX));
            }
        }

//        // catchBlocksMap now has all try-catch blocks without finally. They are irrelevant.
//        // Or are they? What if there's a try-finally inside of catch block? That would be bad. Or would it?
//        for (Map.Entry<List<Block>, List<LabelNode>> e : catchBlocksMap.entrySet()) {
//            System.out.println("  Unaccounted try-catch: " + e.getKey() + " -> " + e.getValue().stream().map(c -> getLabelIndex(c.start)).collect(toList()));
//            for (Block tryBlock : e.getKey()) {
//                AbstractInsnNode nextInstruction = findNextInstruction(tryBlock.end);
//                if (nextInstruction.getOpcode() == GOTO) {
//                    System.out.println("  End label is most likely " + getLabelIndex(((JumpInsnNode) nextInstruction).label));
//                }
//            }
//        }

        tempTryList.sort(
                comparingInt((Try t) -> t.finallyScope.first().endIndex() - t.tryScope.first().startIndex())
                .thenComparingInt(t -> t.tryScope.first().startIndex())
        );

        return IntStream.range(0, tempTryList.size()).mapToObj(i -> {
            Try first = tempTryList.get(i);

            for (Try nextTry : tempTryList.subList(i + 1, tempTryList.size())) {
                if (nextTry.tryScope.surrounds(first)) {
                    nextTry.tryScope.nested.add(first);

                    return Optional.<Try>empty();
                }

                for (Scope catchScope : nextTry.catchScopes) {
                    if (catchScope.surrounds(first)) {
                        catchScope.nested.add(first);

                        return Optional.<Try>empty();
                    }
                }

                if (nextTry.finallyScope.surrounds(first)) {
                    nextTry.finallyScope.nested.add(first);

                    return Optional.<Try>empty();
                }
            }

            return Optional.of(first);
        }).flatMap(Optional::stream).collect(toList());
    }

    private LabelNode findTheEndOfFinally(LabelNode startLabel, boolean defaultBlock) {
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
