/*
 * Copyright 2023 Ivan Bessonov
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
package com.github.ibessonov.finally4j.ex;

import com.github.ibessonov.finally4j.Finally;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author ibessonov
 */
class SuccessfulTest {
    @Test
    void throwInTryUnconditional() {
        try {
            try {
                Thread.currentThread();

                throw new Exception();
            } finally {
                assertTrue(Finally.hasThrownException());

                Throwable e = Finally.thrownException();

                assertNotNull(e);
                assertEquals(Exception.class, e.getClass());

                assertEquals(Optional.of(e), Finally.thrownExceptionOptional());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    void throwInTryConditional() {
        try {
            try {
                if (System.currentTimeMillis() != 0) {
                    throw new Exception();
                }
            } finally {
                assertTrue(Finally.hasThrownException());

                Throwable e = Finally.thrownException();

                assertNotNull(e);
                assertEquals(Exception.class, e.getClass());

                assertEquals(Optional.of(e), Finally.thrownExceptionOptional());
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    void noException() {
        try {
            System.out.println(1);
        } catch (RuntimeException e) {
            System.out.println("re");
        } catch (Exception e) {
            System.out.println("e");
        } finally {
            assertFalse(Finally.hasThrownException());
        }
    }

    @Test
    void veryBig() {
        try {
            long time = System.currentTimeMillis();
            if (time < 0) return;
            try {
                System.out.println("Time is not negative");
                if (time > 0) return;
                System.out.println("Time is zero");
            } catch (RuntimeException e) {
                System.out.println("Runtime Exception");
            } finally {
                System.out.println("Finally 1");
            }
        } catch (Exception e) {
            System.out.println("Exception");
        } finally {
            System.out.println("Finally 2");
        }
    }
}
