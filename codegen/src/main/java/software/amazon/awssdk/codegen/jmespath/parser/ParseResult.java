/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.jmespath.parser;

import java.util.function.Function;
import software.amazon.awssdk.utils.Validate;

/**
 * The result of a {@link Parser#parse(int, int)} call. This is either successful ({@link #success}) or an error
 * ({@link #error()}).
 */
public final class ParseResult<T> {
    private final T result;

    private ParseResult(T result) {
        this.result = result;
    }

    /**
     * Create a successful result with the provided value.
     */
    public static <T> ParseResult<T> success(T result) {
        Validate.notNull(result, "result");
        return new ParseResult<>(result);
    }

    /**
     * Create an error result.
     */
    public static <T> ParseResult<T> error() {
        return new ParseResult<>(null);
    }

    /**
     * Convert the value in this parse result (if successful) using the provided function.
     */
    public <U> ParseResult<U> mapResult(Function<T, U> mapper) {
        if (hasResult()) {
            return ParseResult.success(mapper.apply(result));
        } else {
            return ParseResult.error();
        }
    }

    /**
     * Returns true if the parse result was successful.
     */
    public boolean hasResult() {
        return result != null;
    }

    /**
     * Returns the result of parsing.
     */
    public T result() {
        Validate.validState(hasResult(), "Result not available");
        return result;
    }
}
