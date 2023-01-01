/*
 * Copyright 2018 Ivan Bessonov
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author ibessonov
 */
class UnboxingTest {

    @Test
    void booleanReturnType() {
        assertEquals(Boolean.TRUE, booleanReturnType0());
    }

    @Test
    void byteReturnType() {
        assertEquals(Byte.valueOf((byte) 1), byteReturnType0());
    }

    @Test
    void charReturnType() {
        assertEquals(Character.valueOf('a'), charReturnType0());
    }

    @Test
    void shortReturnType() {
        assertEquals(Short.valueOf((short) 1), shortReturnType0());
    }

    @Test
    void intReturnType() {
        assertEquals(Integer.valueOf(1), intReturnType0());
    }

    @Test
    void longReturnType() {
        assertEquals(Long.valueOf(1L), longReturnType0());
    }

    @Test
    void floatReturnType() {
        assertEquals(Float.valueOf(1f), floatReturnType0());
    }

    @Test
    void doubleReturnType() {
        assertEquals(Double.valueOf(1d), doubleReturnType0());
    }

    private static Boolean booleanReturnType0() {
        try {
            return true;
        } finally {
            Assertions.assertTrue(Finally.getReturnValueBoolean());

            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Byte byteReturnType0() {
        try {
            return 1;
        } finally {
            assertEquals(1, Finally.getReturnValueByte());

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Character charReturnType0() {
        try {
            return 'a';
        } finally {
            assertEquals('a', Finally.getReturnValueChar());

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Short shortReturnType0() {
        try {
            return 1;
        } finally {
            assertEquals(1, Finally.getReturnValueShort());

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Integer intReturnType0() {
        try {
            return 1;
        } finally {
            assertEquals(1, Finally.getReturnValueInt());

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Long longReturnType0() {
        try {
            return 1L;
        } finally {
            assertEquals(1L, Finally.getReturnValueLong());

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Float floatReturnType0() {
        try {
            return 1f;
        } finally {
            assertEquals(1f, Finally.getReturnValueFloat(), Float.MIN_VALUE);

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static Double doubleReturnType0() {
        try {
            return 1d;
        } finally {
            assertEquals(1d, Finally.getReturnValueDouble(), Double.MIN_VALUE);

            try { Finally.getReturnValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.getReturnValueFloat();   fail(""); } catch (ClassCastException ignored) {}
        }
    }
}
