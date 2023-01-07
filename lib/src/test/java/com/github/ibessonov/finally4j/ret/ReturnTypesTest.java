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
package com.github.ibessonov.finally4j.ret;

import com.github.ibessonov.finally4j.Finally;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author ibessonov
 */
class ReturnTypesTest {
    @Test
    void booleanReturnType() {
        assertTrue(booleanReturnType0());
    }

    @Test
    void byteReturnType() {
        assertEquals(1, byteReturnType0());
    }

    @Test
    void charReturnType() {
        assertEquals('a', charReturnType0());
    }

    @Test
    void shortReturnType() {
        assertEquals(1, shortReturnType0());
    }

    @Test
    void intReturnType() {
        assertEquals(1, intReturnType0());
    }

    @Test
    void longReturnType() {
        assertEquals(1L, longReturnType0());
    }

    @Test
    void floatReturnType() {
        assertEquals(1f, floatReturnType0(), Float.MIN_VALUE);
    }

    @Test
    void doubleReturnType() {
        assertEquals(1d, doubleReturnType0(), Double.MIN_VALUE);
    }

    @Test
    void objectReturnType() {
        assertEquals("1", objectReturnType0());
    }

    private static boolean booleanReturnType0() {
        try {
            return true;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertTrue(Finally.getReturnValueBoolean());
            assertEquals(Boolean.TRUE, Finally.getReturnValue());
            assertEquals(Optional.of(true), Finally.getReturnValueOptional());
        }
    }

    private static byte byteReturnType0() {
        try {
            return 1;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals(1, Finally.getReturnValueByte());
            assertEquals(Byte.valueOf((byte) 1), Finally.getReturnValue());
            assertEquals(Optional.of((byte) 1), Finally.getReturnValueOptional());
        }
    }

    private static char charReturnType0() {
        try {
            return 'a';
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals('a', Finally.getReturnValueChar());
            assertEquals(Character.valueOf('a'), Finally.getReturnValue());
            assertEquals(Optional.of('a'), Finally.getReturnValueOptional());
        }
    }

    private static short shortReturnType0() {
        try {
            return 1;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals(1, Finally.getReturnValueShort());
            assertEquals(Short.valueOf((short) 1), Finally.getReturnValue());
            assertEquals(Optional.of((short) 1), Finally.getReturnValueOptional());
        }
    }

    private static int intReturnType0() {
        try {
            return 1;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals(1, Finally.getReturnValueInt());
            assertEquals(Integer.valueOf(1), Finally.getReturnValue());
            assertEquals(Optional.of(1), Finally.getReturnValueOptional());
        }
    }

    private static long longReturnType0() {
        try {
            return 1L;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals(1L, Finally.getReturnValueLong());
            assertEquals(Long.valueOf(1L), Finally.getReturnValue());
            assertEquals(Optional.of(1L), Finally.getReturnValueOptional());
        }
    }

    private static float floatReturnType0() {
        try {
            return 1f;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals(1f, Finally.getReturnValueFloat(), Float.MIN_VALUE);
            assertEquals(Float.valueOf(1f), Finally.getReturnValue());
            assertEquals(Optional.of(1f), Finally.getReturnValueOptional());
        }
    }

    private static double doubleReturnType0() {
        try {
            return 1d;
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals(1d, Finally.getReturnValueDouble(), Double.MIN_VALUE);
            assertEquals(Double.valueOf(1d), Finally.getReturnValue());
            assertEquals(Optional.of(1d), Finally.getReturnValueOptional());
        }
    }

    private static String objectReturnType0() {
        try {
            return "1";
        } finally {
            assertTrue(Finally.hasReturnValue());
            assertEquals("1", Finally.getReturnValue());
            assertEquals(Optional.of("1"), Finally.getReturnValueOptional());
        }
    }
}
