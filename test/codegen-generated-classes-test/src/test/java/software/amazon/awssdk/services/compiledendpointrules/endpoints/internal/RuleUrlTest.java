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

package software.amazon.awssdk.services.compiledendpointrules.endpoints.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RuleUrlTest {

    public static Collection<TestIsIpCase> testIsIpCases() {
        return Arrays.asList(
            new TestIsIpCase("0.0.0.0", true)
            , new TestIsIpCase("0.1.1.1", true)
            , new TestIsIpCase("1.0.1.1", true)
            , new TestIsIpCase("1.1.0.1", true)
            , new TestIsIpCase("1.1.1.0", true)
            , new TestIsIpCase("127.0.0.1", true)
            , new TestIsIpCase("132.248.181.171", true)

            // Starts [ and ends with ]
            // No much validation other than that.
            , new TestIsIpCase("[::1]", true)
            , new TestIsIpCase("[no-much-validation-inside]", true)

            // Segment value is > 255
            , new TestIsIpCase("256.1.1.1", false)
            , new TestIsIpCase("1.256.1.1", false)
            , new TestIsIpCase("1.1.256.1", false)
            , new TestIsIpCase("1.1.1.256", false)

            , new TestIsIpCase("399.1.1.1", false)
            , new TestIsIpCase("1.399.1.1", false)
            , new TestIsIpCase("1.1.399.1", false)
            , new TestIsIpCase("1.1.1.399", false)

            // Zero-prefix
            , new TestIsIpCase("010.1.1.1", false)
            , new TestIsIpCase("1.010.1.1", false)
            , new TestIsIpCase("1.1.010.1", false)
            , new TestIsIpCase("1.1.1.010", false)

            , new TestIsIpCase("001.1.1.1", false)
            , new TestIsIpCase("1.001.1.1", false)
            , new TestIsIpCase("1.1.001.1", false)
            , new TestIsIpCase("1.1.1.001", false)

            , new TestIsIpCase("01.1.1.1", false)
            , new TestIsIpCase("1.01.1.1", false)
            , new TestIsIpCase("1.1.01.1", false)
            , new TestIsIpCase("1.1.1.01", false)

            // Not numeric segment
            , new TestIsIpCase("foo.1.1.1", false)
            , new TestIsIpCase("1.foo.1.1", false)
            , new TestIsIpCase("1.1.foo.1", false)
            , new TestIsIpCase("1.1.1.foo", false)
            , new TestIsIpCase("10x.1.1.1", false)
            , new TestIsIpCase("1.10x.1.1", false)
            , new TestIsIpCase("1.1.10x.1", false)
            , new TestIsIpCase("1.1.1.10x", false)

            // More than 4 segments
            , new TestIsIpCase("127.0.0.1.1", false)
            , new TestIsIpCase("127.0.0.1.1mm", false)
            , new TestIsIpCase("1mm.127.0.0.1", false)
            , new TestIsIpCase("127.1mm.0.0.1", false)
            , new TestIsIpCase("127.0.1mm.0.1", false)
            , new TestIsIpCase("127.0.0.1mm.1", false)

            // Less than 4 segments
            , new TestIsIpCase("127.0.0", false)
            , new TestIsIpCase("127.0", false)
            , new TestIsIpCase("127", false)

            // name
            , new TestIsIpCase("amazon.com", false)
            , new TestIsIpCase("localhost", false)
        );
    }

    public static Collection<TestNormalizePathCase> testNormalizePathCases() {
        return Arrays.asList(
            new TestNormalizePathCase(null, "/")
            , new TestNormalizePathCase("", "/")
            , new TestNormalizePathCase("/", "/")
            , new TestNormalizePathCase("foo", "/foo/")
            , new TestNormalizePathCase("/foo", "/foo/")
            , new TestNormalizePathCase("foo/", "/foo/")
            , new TestNormalizePathCase("/foo/", "/foo/")
        );
    }

    @ParameterizedTest
    @MethodSource("testIsIpCases")
    void testIsIp(TestIsIpCase testCase) {
        assertEquals(testCase.isIp, RuleUrl.isIpAddr(testCase.host));
    }

    @ParameterizedTest
    @MethodSource("testNormalizePathCases")
    void testNormalizePath(TestNormalizePathCase testCase) {
        assertEquals(testCase.normalized, RuleUrl.normalizePath(testCase.path));
    }


    static class TestIsIpCase {
        final String host;
        final boolean isIp;

        TestIsIpCase(String host, boolean isIp) {
            this.host = host;
            this.isIp = isIp;
        }

        @Override
        public String toString() {
            return this.host + " -> " + this.isIp;
        }
    }

    static class TestNormalizePathCase {
        final String path;
        final String normalized;

        TestNormalizePathCase(String path, String normalized) {
            this.path = path;
            this.normalized = normalized;
        }

        @Override
        public String toString() {
            return this.path + " -> " + this.normalized;
        }
    }
}
