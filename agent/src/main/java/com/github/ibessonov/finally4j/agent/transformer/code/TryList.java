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
package com.github.ibessonov.finally4j.agent.transformer.code;

import com.github.ibessonov.finally4j.agent.transformer.FinallyMethodNode;
import com.github.ibessonov.finally4j.agent.transformer.util.Util;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.ibessonov.finally4j.agent.transformer.code.Block.startsWith;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

public class TryList {
    public static List<Try> init(FinallyMethodNode methodNode) {
        var labelIdx = methodNode.labelIdx;

        // For some reason, there might be an intersection between start/end scope and the handler.
        // Here I normalize such blocks by moving end to the handler position.
        var tryCatchBlocks = methodNode.tryCatchBlocks.stream()
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
                .flatMap(block -> splitTryCatchBlockNode(mergedTryCatchBlocks, block, labelIdx))
                .collect(groupingBy(block -> block.handler));

        // Sort values in "blocksGroupedByHandler" and "blocksGroupedByDefaultHandler" to simplify matching them.
        concat(blocksGroupedByHandler.values().stream(), blocksGroupedByDefaultHandler.values().stream())
                .forEach(list -> list.sort(comparingInt(node -> labelIdx.get(node.start))));

        // Maps "try" sections to lists of corresponding "catch" sections. Without finally blocks.
        var catchBlocksMap = blocksGroupedByHandler.entrySet().stream().collect(
                groupingBy(entry -> entry.getValue().stream().map(node -> new Block(methodNode, node)).collect(toList()),
                        mapping(Map.Entry::getKey, toList()))
        );

        List<Try> tempTryList = new ArrayList<>();

        for (Map.Entry<LabelNode, List<TryCatchBlockNode>> entry : blocksGroupedByDefaultHandler.entrySet()) {
            List<TryCatchBlockNode> list = entry.getValue();

            List<Block> blocks = list.stream().map(node -> new Block(methodNode, node.start, node.end)).collect(toList());

            LabelNode nextLabel = Util.findTheEndOfFinally(labelIdx, entry.getKey(), true);

            boolean found = false;

            for (Map.Entry<List<Block>, List<LabelNode>> catchBlocksMapEntry : catchBlocksMap.entrySet()) {
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

                    newTry.finallyScope.blocks.add(new Block(methodNode, entry.getKey(), nextLabel));

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
                newTry.finallyScope.blocks.add(new Block(methodNode, entry.getKey(), nextLabel));

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
    private static Stream<TryCatchBlockNode> splitTryCatchBlockNode(
            List<TryCatchBlockNode> mergedTryCatchBlocks,
            TryCatchBlockNode block,
            Map<LabelNode, Integer> labelIdx
    ) {
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
}
