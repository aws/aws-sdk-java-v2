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

package software.amazon.awssdk.codegen.jmespath.parser.util;

import java.util.function.Function;
import software.amazon.awssdk.codegen.jmespath.parser.ParseResult;
import software.amazon.awssdk.codegen.jmespath.parser.Parser;

/**
 * A {@link Parser} that invokes a list of converters, returning the first converter that was successful. This is created
 * using {@link #firstTry(Parser)} and new converters are added via {@link #thenTry(Parser)}.
 */
public final class CompositeParser<T> implements Parser<T> {
    private final Parser<T> parser;

    private CompositeParser(Parser<T> parser) {
        this.parser = parser;
    }

    /**
     * Create a {@link CompositeParser} that tries the provided parser first.
     */
    public static <T> CompositeParser<T> firstTry(Parser<T> parser) {
        return new CompositeParser<>(parser);
    }

    /**
     * Create a {@link CompositeParser} that tries the provided parser first, converting the result of that parser using the
     * provided function.
     */
    public static <T, U> CompositeParser<U> firstTry(Parser<T> parser, Function<T, U> resultConverter) {
        return firstTry((start, end) -> parser.parse(start, end).mapResult(resultConverter));
    }

    /**
     * Create a new {@link CompositeParser} that tries the provided parser after all previous parsers.
     */
    public CompositeParser<T> thenTry(Parser<T> nextParser) {
        return new CompositeParser<>((start, end) -> {
            ParseResult<T> parse = parser.parse(start, end);
            if (!parse.hasResult()) {
                return nextParser.parse(start, end);
            }

            return parse;
        });
    }

    /**
     * Create a new {@link CompositeParser} that tries the provided parser after all previous parsers, converting the result of
     * that parser using the provided function.
     */
    public <S> CompositeParser<T> thenTry(Parser<S> nextParser, Function<S, T> resultConverter) {
        return thenTry((start, end) -> nextParser.parse(start, end).mapResult(resultConverter));
    }

    @Override
    public ParseResult<T> parse(int startPosition, int endPosition) {
        return parser.parse(startPosition, endPosition);
    }
}