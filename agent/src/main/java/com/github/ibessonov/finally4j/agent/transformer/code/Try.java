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
 * A single full try-catch-finally block.
 *
 * @author ibessonov
 */
public class Try {
    /**
     * Scope of the {@code try} code blocks.
     */
    public final Scope tryScope = new Scope();

    /**
     * Scopes of {@code catch} code blocks.
     */
    public final List<Scope> catchScopes = new ArrayList<>();

    /**
     * Scope of the {@code finally} code blocks.
     */
    public final Scope finallyScope = new Scope();

    /**
     * Pretty string representation of the block.
     *
     * @param padding Padding for every new string in the representation.
     */
    public void print(String padding) {
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
