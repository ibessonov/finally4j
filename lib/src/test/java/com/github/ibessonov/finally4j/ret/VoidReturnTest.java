package com.github.ibessonov.finally4j.ret;

import com.github.ibessonov.finally4j.Finally;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class VoidReturnTest {
    @Test
    void terminalFinallyBlockInTry() {
        try {
            //noinspection ResultOfMethodCallIgnored
            Thread.currentThread();
        } finally {
            Assertions.assertFalse(Finally.hasReturnValue());
            assertEquals(Optional.empty(), Finally.getReturnValueOptional());
            try { Finally.getReturnValue(); fail(""); } catch (Finally.NoReturnValueException ignored) {}
        }
    }

    @Test
    void middleFinallyBlockInTry() {
        try {
            if (System.currentTimeMillis() != 0) return;
            System.currentTimeMillis(); // to avoid warnings
        } finally {
            assertFalse(Finally.hasReturnValue());
            assertEquals(Optional.empty(), Finally.getReturnValueOptional());
            try { Finally.getReturnValue(); fail(""); } catch (Finally.NoReturnValueException ignored) {}
        }
    }
}
