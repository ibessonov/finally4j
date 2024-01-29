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

import java.util.ArrayList;
import java.util.List;

/**
 * Code scope is a list of code blocks that could represent either a try block (without finally blocks included),
 * a catch block (without finally block included) or a finally block. Used as a part of {@link Try} implementation.
 *
 * @author ibessonov
 */
public class Scope {
    /**
     * A list of blocks that represent the scope.
     */
    public final List<Block> blocks = new ArrayList<>();

    /**
     * All try blocks contained within this scope.
     */
    public final List<Try> nested = new ArrayList<>();

    /**
     * Returns {@code true} if {@code aTry} should belong to {@link #nested} list, meaning that the entirity of
     * {@code aTry} is located within current scope.
     *
     * @param aTry Try block.
     * @return {@code true} or {@code false}.
     */
    public boolean surrounds(Try aTry) {
        Block first = first();
        Block last = last();

        Block tryFirst = aTry.tryScope.first();
        Block tryLast = aTry.finallyScope.last();

        return first.startIndex() <= tryFirst.startIndex() && tryLast.endIndex() <= last.endIndex();
    }

    /**
     * @return First block in the scope.
     */
    public Block first() {
        return blocks.get(0);
    }

    /**
     * @return Last block in the scope.
     */
    public Block last() {
        return blocks.get(blocks.size() - 1);
    }
}
