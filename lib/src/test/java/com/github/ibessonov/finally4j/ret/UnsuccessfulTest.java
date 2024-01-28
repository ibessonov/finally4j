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
package com.github.ibessonov.finally4j.ret;

import com.github.ibessonov.finally4j.Finally;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author ibessonov
 */
class UnsuccessfulTest {

    @Test
    void noReturnValue() {
        try { Finally.returnedValue();        fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueBoolean(); fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueByte();    fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueChar();    fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueShort();   fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueInt();     fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueLong();    fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueFloat();   fail(""); } catch (Finally.NoReturnValueException ignored) {}
        try { Finally.returnedValueDouble();  fail(""); } catch (Finally.NoReturnValueException ignored) {}
    }

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
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static byte byteReturnType0() {
        try {
            return 1;
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static char charReturnType0() {
        try {
            return 'a';
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static short shortReturnType0() {
        try {
            return 1;
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static int intReturnType0() {
        try {
            return 1;
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static long longReturnType0() {
        try {
            return 1L;
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static float floatReturnType0() {
        try {
            return 1f;
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static double doubleReturnType0() {
        try {
            return 1d;
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
        }
    }

    private static String objectReturnType0() {
        try {
            return "1";
        } finally {
            try { Finally.returnedValueBoolean(); fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueByte();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueChar();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueShort();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueInt();     fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueLong();    fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueFloat();   fail(""); } catch (ClassCastException ignored) {}
            try { Finally.returnedValueDouble();  fail(""); } catch (ClassCastException ignored) {}
        }
    }
}
