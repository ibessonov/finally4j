package com.github.ibessonov.finally4j.ret;

import com.github.ibessonov.finally4j.Finally;
import com.github.ibessonov.finally4j.Finally.NoReturnValueException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class VoidReturnTest {
    @Test
    void terminalFinallyBlockInTry() {
        try {
            Thread.currentThread();
        } finally {
            assertFalse(Finally.hasReturnedValue());
            assertEquals(Optional.empty(), Finally.returnedValueOptional());

            try { Finally.returnedValue(); fail(""); } catch (NoReturnValueException ignored) {}
        }
    }

    @Test
    void nonTerminalFinallyBlockInTry() {
        try {
            if (System.currentTimeMillis() != 0) return;
        } finally {
            assertFalse(Finally.hasReturnedValue());
            assertEquals(Optional.empty(), Finally.returnedValueOptional());

            try { Finally.returnedValue(); fail(""); } catch (NoReturnValueException ignored) {}
        }
    }
}
