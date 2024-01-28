package com.github.ibessonov.finally4j.agent.transformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Code scope is a list of code blocks that could represent either a try block (without finally blocks included),
 * a catch block (without finally block included) or a finally block. Used as a part of {@link Try} implementation.
 *
 * @author ibessonov
 */
class Scope {
    /**
     * A list of blocks that represent the scope.
     */
    final List<Block> blocks = new ArrayList<>();

    /**
     * All try blocks contained within this scope.
     */
    final List<Try> nested = new ArrayList<>();

    /**
     * Returns {@code true} if {@code aTry} should belong to {@link #nested} list, meaning that the entirity of
     * {@code aTry} is located within current scope.
     *
     * @param aTry Try block.
     * @return {@code true} or {@code false}.
     */
    boolean surrounds(Try aTry) {
        Block first = first();
        Block last = last();

        Block tryFirst = aTry.tryScope.first();
        Block tryLast = aTry.finallyScope.last();

        return first.startIndex() <= tryFirst.startIndex() && tryLast.endIndex() <= last.endIndex();
    }

    /**
     * @return First block in the scope.
     */
    Block first() {
        return blocks.get(0);
    }

    /**
     * @return Last block in the scope.
     */
    Block last() {
        return blocks.get(blocks.size() - 1);
    }
}
