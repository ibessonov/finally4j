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
package com.github.ibessonov.finally4j;

interface Constants {

    String FINALLY_CLASS_INTERNAL_NAME = "com/github/ibessonov/finally4j/Finally";

    String FINALLY_IS_SUPPORTED_METHOD_NAME = "isSupported";

    String FINALLY_HAS_RETURN_VALUE_METHOD_NAME = "hasReturnValue";
    String FINALLY_GET_RETURN_VALUE_METHOD_NAME = "getReturnValue";
    String FINALLY_GET_RETURN_VALUE_METHOD_PREFIX = FINALLY_GET_RETURN_VALUE_METHOD_NAME;
    String FINALLY_GET_RETURN_VALUE_OPTIONAL_METHOD_NAME = "getReturnValueOptional";

    String FINALLY_HAS_THROWN_EXCEPTION_METHOD_NAME = "hasThrownException";
    String FINALLY_GET_THROWN_EXCEPTION_METHOD_NAME = "getThrownException";
}
