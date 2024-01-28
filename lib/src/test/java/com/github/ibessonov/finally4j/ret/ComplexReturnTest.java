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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author ibessonov
 */
class ComplexReturnTest {
    @Test
    void conditionInTryBlock() {
        assertEquals("false", conditionInTryBlock0(false));
        assertEquals("false", conditionInTryBlock1(false));

        assertEquals("true", conditionInTryBlock0(true));
        assertEquals("true", conditionInTryBlock1(true));
    }

    @Test
    void conditionInCatchBlock() {
        assertEquals("false", conditionInCatchBlock0(false));
        assertEquals("false", conditionInCatchBlock1(false));
        assertEquals("false", conditionInCatchBlock2(false));
        assertEquals("false", conditionInCatchBlock3(false));

        assertEquals("true", conditionInCatchBlock0(true));
        assertEquals("true", conditionInCatchBlock1(true));
        assertEquals("true", conditionInCatchBlock2(true));
        assertEquals("true", conditionInCatchBlock3(true));
    }

    private static String conditionInTryBlock0(boolean hasReturnValue) {
        try {
            if (hasReturnValue) {
                return "true";
            }
        } finally {
            assertEquals(hasReturnValue, Finally.hasReturnedValue());

            if (hasReturnValue) {
                assertEquals("true", Finally.returnedValue());
            } else {
                assertEquals(Optional.empty(), Finally.returnedValueOptional());
            }
        }

        return "false";
    }

    private static String conditionInTryBlock1(boolean hasReturnValue) {
        try {
            if (hasReturnValue) {
                return "true";
            }

            return "false";
        } finally {
            assertTrue(Finally.hasReturnedValue());

            if (hasReturnValue) {
                assertEquals("true", Finally.returnedValue());
            } else {
                assertEquals("false", Finally.returnedValue());
            }
        }
    }

    private static String conditionInCatchBlock0(boolean hasReturnValue) {
        try {
            throw new Exception();
        } catch (Exception e) {
            if (hasReturnValue) {
                return "true";
            }
        } finally {
            assertEquals(hasReturnValue, Finally.hasReturnedValue());

            if (hasReturnValue) {
                assertEquals("true", Finally.returnedValue());
            } else {
                assertEquals(Optional.empty(), Finally.returnedValueOptional());
            }
        }

        return "false";
    }

    private static String conditionInCatchBlock1(boolean hasReturnValue) {
        try {
            throw new Exception();
        } catch (Exception e) {
            if (hasReturnValue) {
                return "true";
            }

            return "false";
        } finally {
            assertTrue(Finally.hasReturnedValue());

            if (hasReturnValue) {
                assertEquals("true", Finally.returnedValue());
            } else {
                assertEquals("false", Finally.returnedValue());
            }
        }
    }

    private static String conditionInCatchBlock2(boolean hasReturnValue) {
        try {
            throw new Exception();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (hasReturnValue) {
                return "true";
            }
        } finally {
            assertEquals(hasReturnValue, Finally.hasReturnedValue());

            if (hasReturnValue) {
                assertEquals("true", Finally.returnedValue());
            } else {
                assertEquals(Optional.empty(), Finally.returnedValueOptional());
            }
        }

        return "false";
    }

    private static String conditionInCatchBlock3(boolean hasReturnValue) {
        try {
            throw new Exception();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (hasReturnValue) {
                return "true";
            }

            return "false";
        } finally {
            assertTrue(Finally.hasReturnedValue());

            if (hasReturnValue) {
                assertEquals("true", Finally.returnedValue());
            } else {
                assertEquals("false", Finally.returnedValue());
            }
        }
    }
}
