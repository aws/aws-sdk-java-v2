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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Holds the Appendix A rows and the strategy parameters shared by the naming tests.
 */
public final class AttributeNamingCases {
    static final String[] SOURCES = {
        "HTTPRequest",
        "HttpRequest",
        "httpRequest",
        "HTTPREQUEST",
        "httpREQUEST",
        "2HttpRequest",
        "HTTP_REQUEST",
        "Http-Request",
        "Http.Request",
        "_HttpRequest",
        "httprequest",
        "Httprequest",
        "HTTPrequest",
        "HttpRequest_2",
        "Http_Request",
        "-HttpRequest",
        "HttpRequest2",
        " HttpRequest",
        "HTTP Request",
        "Http Request",
        "http Request",
        " HTTP Request"
    };

    static final String[] LOWER_SNAKE = {
        "http_request",
        "http_request",
        "http_request",
        "httprequest",
        "http_request",
        "2_http_request",
        "http_request",
        "http_request",
        "http_request",
        "http_request",
        "httprequest",
        "httprequest",
        "htt_prequest",
        "http_request_2",
        "http_request",
        "http_request",
        "http_request2",
        "http_request",
        "http_request",
        "http_request",
        "http_request",
        "http_request"
    };

    static final String[] UPPER_SNAKE = {
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTPREQUEST",
        "HTTP_REQUEST",
        "2_HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTPREQUEST",
        "HTTPREQUEST",
        "HTT_PREQUEST",
        "HTTP_REQUEST_2",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST2",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST",
        "HTTP_REQUEST"
    };

    static final String[] LOWER_CAMEL = {
        "httpRequest",
        "httpRequest",
        "httpRequest",
        "httprequest",
        "httpRequest",
        "2HttpRequest",
        "httpRequest",
        "httpRequest",
        "httpRequest",
        "httpRequest",
        "httprequest",
        "httprequest",
        "httPrequest",
        "httpRequest2",
        "httpRequest",
        "httpRequest",
        "httpRequest2",
        "httpRequest",
        "httpRequest",
        "httpRequest",
        "httpRequest",
        "httpRequest"
    };

    static final String[] UPPER_CAMEL = {
        "HttpRequest",
        "HttpRequest",
        "HttpRequest",
        "Httprequest",
        "HttpRequest",
        "2HttpRequest",
        "HttpRequest",
        "HttpRequest",
        "HttpRequest",
        "HttpRequest",
        "Httprequest",
        "Httprequest",
        "HttPrequest",
        "HttpRequest2",
        "HttpRequest",
        "HttpRequest",
        "HttpRequest2",
        "HttpRequest",
        "HttpRequest",
        "HttpRequest",
        "HttpRequest",
        "HttpRequest"
    };

    private static final AttributeNamingStrategy CUSTOM = String::toLowerCase;

    private AttributeNamingCases() {
    }

    /**
     * Returns one row for each Appendix A source name and its four stored names.
     */
    static Stream<Arguments> appendixRows() {
        List<Arguments> rows = new ArrayList<>();
        for (int i = 0; i < SOURCES.length; i++) {
            rows.add(Arguments.of(SOURCES[i], LOWER_SNAKE[i], UPPER_SNAKE[i], LOWER_CAMEL[i], UPPER_CAMEL[i]));
        }
        return rows.stream();
    }

    /**
     * Returns source names that contain no word.
     */
    static Stream<String> rejectedSources() {
        return Stream.of("___", "---", "...", "   ", "", "\t", " . ");
    }

    /**
     * Returns the omitted strategy, the four built-in strategies, and the custom strategy.
     */
    static Stream<Arguments> schemaStrategies() {
        return Stream.concat(Stream.of(Arguments.of("OMITTED", null)), builtInAndCustom());
    }

    /**
     * Returns the four built-in strategies and the custom strategy.
     */
    static Stream<Arguments> builtInAndCustom() {
        return Stream.of(
            Arguments.of("LOWER_SNAKE_CASE", AttributeNamingStrategies.LOWER_SNAKE_CASE),
            Arguments.of("UPPER_SNAKE_CASE", AttributeNamingStrategies.UPPER_SNAKE_CASE),
            Arguments.of("LOWER_CAMEL_CASE", AttributeNamingStrategies.LOWER_CAMEL_CASE),
            Arguments.of("UPPER_CAMEL_CASE", AttributeNamingStrategies.UPPER_CAMEL_CASE),
            Arguments.of("CUSTOM", CUSTOM));
    }

    /**
     * Returns the four built-in strategies.
     */
    static Stream<Arguments> builtInOnly() {
        return Stream.of(
            Arguments.of("LOWER_SNAKE_CASE", AttributeNamingStrategies.LOWER_SNAKE_CASE),
            Arguments.of("UPPER_SNAKE_CASE", AttributeNamingStrategies.UPPER_SNAKE_CASE),
            Arguments.of("LOWER_CAMEL_CASE", AttributeNamingStrategies.LOWER_CAMEL_CASE),
            Arguments.of("UPPER_CAMEL_CASE", AttributeNamingStrategies.UPPER_CAMEL_CASE));
    }

    /**
     * Returns the JUnit 4 parameters for the functional tests.
     */
    public static Collection<Object[]> functionalParameters() {
        List<Object[]> rows = new ArrayList<>();
        builtInAndCustom().forEach(arguments -> rows.add(arguments.get()));
        return rows;
    }

    /**
     * Returns the stored name for a source name.
     * A null strategy keeps the source name.
     */
    public static String storedName(AttributeNamingStrategy strategy, String sourceName) {
        if (strategy == null) {
            return sourceName;
        }
        return strategy.transform(sourceName);
    }

    /**
     * Counts calls to a delegate strategy.
     */
    static final class CountingStrategy implements AttributeNamingStrategy {
        private final AttributeNamingStrategy delegate;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> sources = new ArrayList<>();

        /**
         * Counts every call and then applies the delegate.
         */
        CountingStrategy(AttributeNamingStrategy delegate) {
            this.delegate = delegate;
        }

        /**
         * Returns how many times {@link #transform(String)} was called.
         */
        int calls() {
            return calls.get();
        }

        /**
         * Returns the source names received, in call order.
         */
        List<String> sources() {
            return new ArrayList<>(sources);
        }

        @Override
        public String transform(String sourceName) {
            calls.incrementAndGet();
            sources.add(sourceName);
            return delegate.transform(sourceName);
        }
    }

    /**
     * Returns the built-in strategies from both public types, in matching pairs.
     */
    static List<AttributeNamingStrategy> bothPublicTypes() {
        return Arrays.asList(
            AttributeNamingStrategies.LOWER_SNAKE_CASE,
            AttributeNamingStrategies.UPPER_SNAKE_CASE,
            AttributeNamingStrategies.LOWER_CAMEL_CASE,
            AttributeNamingStrategies.UPPER_CAMEL_CASE,
            BuiltInAttributeNamingStrategy.LOWER_SNAKE_CASE,
            BuiltInAttributeNamingStrategy.UPPER_SNAKE_CASE,
            BuiltInAttributeNamingStrategy.LOWER_CAMEL_CASE,
            BuiltInAttributeNamingStrategy.UPPER_CAMEL_CASE);
    }
}
