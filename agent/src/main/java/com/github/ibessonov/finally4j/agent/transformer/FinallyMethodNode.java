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
import com.github.ibessonov.finally4j.agent.transformer.code.TryList;
import com.github.ibessonov.finally4j.agent.transformer.util.Replacer;
import com.github.ibessonov.finally4j.agent.transformer.util.Util;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.ibessonov.finally4j.agent.transformer.util.Util.ASM_V;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.DEBUG;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.findNextInstruction;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.findPreviousInstruction;
import static com.github.ibessonov.finally4j.agent.transformer.util.Util.isStore;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

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

        List<Try> tryList = TryList.init(this);

        // Avoid logs and return if there are no finally blocks in the method.
        if (tryList.isEmpty()) {
            super.accept(outerMv);

            return;
        }

        if (DEBUG) {
            logTransformation(tryList);
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

    private void logTransformation(List<Try> tryList) {
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
                    LabelNode theEndOfFinally = Util.findTheEndOfFinally(labelIdx, block.end, false);

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
                LabelNode theEndOfFinally = Util.findTheEndOfFinally(labelIdx, lastBlock.end, false);

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
}
