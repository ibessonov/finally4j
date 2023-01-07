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
package com.github.ibessonov.finally4j;

import java.util.Optional;

/**
 * Utility class that provides special functionality inside of {@code finally} code blocks. Only works if corresponding
 * java agent library is set in JVM parameters.
 * @author ibessonov
 */
public interface Finally {

    class NoReturnValueException extends RuntimeException {}

    class NoThrownExceptionException extends RuntimeException {}

    /**
     * @return true if {@code finally4j-agent.jar} agent is configured properly
     */
    static boolean isSupported() {
        return false;
    }

    /**
     * @return true if invoked in {@code finally} block that is executed due to return statement in
     * corresponding {@code try/catch} block
     */
    static boolean hasReturnValue() {
        return false;
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code boolean} or {@code Boolean}
     * @throws NullPointerException if type of actual return value is {@code Boolean} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static boolean getReturnValueBoolean() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code byte} or {@code Byte}
     * @throws NullPointerException if type of actual return value is {@code Byte} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static byte getReturnValueByte() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code char} or {@code Character}
     * @throws NullPointerException if type of actual return value is {@code Character} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static char getReturnValueChar() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code short} or {@code Short}
     * @throws NullPointerException if type of actual return value is {@code Short} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static short getReturnValueShort() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code int} or {@code Integer}
     * @throws NullPointerException if type of actual return value is {@code Integer} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static int getReturnValueInt() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code long} or {@code Long}
     * @throws NullPointerException if type of actual return value is {@code Long} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static long getReturnValueLong() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code float} or {@code Float}
     * @throws NullPointerException if type of actual return value is {@code Float} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static float getReturnValueFloat() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value is not {@code double} or {@code Double}
     * @throws NullPointerException if type of actual return value is {@code Double} and it equals {@code null}
     * @see Finally#hasReturnValue()
     */
    static double getReturnValueDouble() {
        throw new NoReturnValueException();
    }

    /**
     * @return value that was returned in corresponding {@code try/catch} block. Value is automatically boxed if
     * type of actual return value is primitive
     * @throws NoReturnValueException outside of {@code finally} block or if no value was actually returned
     * @throws ClassCastException if type of actual returned value differs from expected type
     * @see Finally#hasReturnValue()
     */
    static <T> T getReturnValue() {
        throw new NoReturnValueException();
    }

    /**
     * @return Optional for value that was returned in corresponding {@code try/catch} block. Returns empty optional outside
     * of {@code finally} block or if no value was actually returned. Value is automatically boxed if
     * type of actual return value is primitive
     * @see Finally#hasReturnValue()
     * @see Optional
     */
    static <T> Optional<T> getReturnValueOptional() {
        return Optional.empty();
    }

    /**
     * @return true if invoked in {@code finally} block that is executed due to thrown exception in
     * corresponding {@code try/catch} block
     */
    static boolean hasThrownException() {
        return false;
    }

    /**
     * @return exception that was thrown in corresponding {@code try/catch} block
     * @throws NoReturnValueException outside of {@code finally} block or if no exception was actually thrown
     * @throws ClassCastException if type of actual exception differs from expected type
     * @see Finally#hasThrownException()
     */
    static <T extends Throwable> T getThrownException() {
        throw new NoThrownExceptionException();
    }

    /**
     * @return Optional for exception that was thrown in corresponding {@code try/catch} block. Returns empty optional
     * outside of {@code finally} block or if no exception was actually thrown
     * @see Finally#hasThrownException()
     */
    static <T extends Throwable> Optional<T> getThrownExceptionOptional() {
        return Optional.empty();
    }
}
