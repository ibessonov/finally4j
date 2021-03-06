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
package com.github.ibessonov.finally4j.ex;

import com.github.ibessonov.finally4j.Finally;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author ibessonov
 */
class SuccessfulTest {

    @Test
    void throwInTry() {
        try {
            try {
                throw new Exception();
            } finally {
                Assertions.assertTrue(Finally.hasThrownException());
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
