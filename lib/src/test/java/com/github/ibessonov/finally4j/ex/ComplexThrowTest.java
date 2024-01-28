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
package com.github.ibessonov.finally4j.ex;

import com.github.ibessonov.finally4j.Finally;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author ibessonov
 */
class ComplexThrowTest {
    @Test
    void nested() {
        try {
            Exception e0 = null;

            try {
                throw e0 = new Exception();
            } catch (Exception e) {
                Exception e1 = null;

                //noinspection finally
                try {
                    throw e1 = new Exception();
                } finally {
                    assertSame(e1, Finally.thrownException());

                    throw e;
                }
            } finally {
                assertSame(e0, Finally.thrownException());
            }
        } catch (Exception ignored) {
        }
    }
}
