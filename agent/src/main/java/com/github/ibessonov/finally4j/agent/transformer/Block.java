package com.github.ibessonov.finally4j.agent.transformer;

import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.List;
import java.util.Objects;

/**
 * This class represents a code located between two labels.
 *
 * @author ibessonov
 */
final class Block {
    static boolean startsWith(List<Block> blocks, List<Block> prefix) {
        if (blocks.size() < prefix.size()) {
            return false;
        }

        return blocks.subList(0, prefix.size()).equals(prefix);
    }

    /** Method that has the block. */
    private final FinallyMethodNode methodNode;

    /**
     * Start label, inclusive, non-null.
     */
    final LabelNode start;

    /**
     * End label, exclusive, nullable.
     */
    final LabelNode end;

    Block(FinallyMethodNode methodNode, LabelNode start, LabelNode end) {
        this.methodNode = methodNode;
        this.start = start;
        this.end = end;
    }

    Block(FinallyMethodNode methodNode, TryCatchBlockNode node) {
        this(methodNode, node.start, node.end);
    }

    int startIndex() {
        return methodNode.labelIdx.get(start);
    }

    int endIndex() {
        return methodNode.labelIdx.get(end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Block block = (Block) o;

        return start.equals(block.start) && Objects.equals(end, block.end);
    }

    @Override
    public int hashCode() {
        return start.hashCode() ^ Objects.hashCode(end);
    }

    @Override
    public String toString() {
        int startIndex = methodNode.labelIdx.get(start);
        Object endIndex = end == null ? "end" : methodNode.labelIdx.get(end);

        return "[" + startIndex + ", " + endIndex + ")";
    }
}
