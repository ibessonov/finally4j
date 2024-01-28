package com.github.ibessonov.finally4j.agent.transformer;

import java.util.ArrayList;
import java.util.List;

/**
 * A single full try-catch-finally block.
 *
 * @author ibessonov
 */
class Try {
    /**
     * Scope of the {@code try} code blocks.
     */
    final Scope tryScope = new Scope();

    /**
     * Scopes of {@code catch} code blocks.
     */
    final List<Scope> catchScopes = new ArrayList<>();

    /**
     * Scope of the {@code finally} code blocks.
     */
    final Scope finallyScope = new Scope();

    /**
     * Pretty string representation of the block.
     *
     * @param padding Padding for every new string in the representation.
     */
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
