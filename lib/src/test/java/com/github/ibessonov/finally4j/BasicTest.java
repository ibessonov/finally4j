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
package com.github.ibessonov.finally4j;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests methods without try blocks.
 *
 * @author ibessonov
 */
class BasicTest {

    @Test
    void finallyIsSupported() {
        assertTrue(Finally.isSupported());
    }

    @Test
    void noReturnValue() {
        assertFalse(Finally.hasReturnedValue());
        assertEquals(Optional.empty(), Finally.returnedValueOptional());
    }

    @Test
    void noThrownException() {
        assertFalse(Finally.hasThrownException());
        assertEquals(Optional.empty(), Finally.thrownExceptionOptional());
    }
}
