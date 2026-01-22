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

package software.amazon.awssdk.protocols.rpcv2.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

class SdkRpcV2CborGeneratorTest {
    private static final byte[] HEX_ARRAY = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("testCases")
    public void runTestCases(TestCase testCase) {
        StructuredJsonGenerator generator = getGenerator();
        testCase.setup.accept(generator);
        assertThat(toHexEncoded(generator.getBytes())).isEqualTo(annotatedToHex(testCase.expected));
    }

    public static Collection<TestCase> testCases() {
        return Arrays.asList(
            // Numbers, minimal encoding
            builder("Minimally encodes longs when they fit as a simple value")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(23L)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=8117"
                    , "81    # array(1)"
                    , "   17 #   unsigned(23)"
                )
                .build()
            , builder("Minimally encodes longs when they fit in one byte")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(255L)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=8118ff"
                    , "81       # array(1)"
                    , "   18 ff #   unsigned(255)"
                )
                .build()
            , builder("Minimally encodes longs when they fit in two byte")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(256L)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=81190100"
                    , "81         # array(1)"
                    , "   19 0100 #   unsigned(256)"
                )
                .build()
            , builder("Minimally encodes floats when are equivalent to ints")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(23.0F)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=8117"
                    , "81    # array(1)"
                    , "   17 #   unsigned(23)"
                )
                .build()
            , builder("Minimally encodes doubles when are equivalent to ints")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(23.0)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=8117"
                    , "81    # array(1)"
                    , "   17 #   unsigned(23)"
                )
                .build()
            , builder("Minimally encodes doubles that fit in a float")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(1.5)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=81fa3fc00000"
                    , "81             # array(1)"
                    , "   fa 3fc00000 #   float(1.5)"
                )
                .build()

            , builder("Encodes doubles as doubles when needed")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(3.1415927)
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=81fb400921fb5a7ed197"
                    , "81                     # array(1)"
                    , "   fb 400921fb5a7ed197 #   float(3.1415927)"
                )
                .build()
            // Timestamp as epoch seconds with milliseconds decimal part
            , builder("Encodes timestamp as epoch seconds with millisecond decimal part")
                .setup(
                    g -> g.writeStartArray(1)
                          .writeValue(Instant.parse("2024-08-09T18:13:18.426482Z"))
                          .writeEndArray()
                )
                .expected(
                    "# https://cbor.nemo157.com/#type=hex&value=81C1FB41D9AD970F9B4396"
                    , "81                        # array(1)"
                    , "   c1                     #   epoch datetime value, tag(1)"
                    , "      fb 41d9ad970f9b4396 #     float(1,723,227,198.426)"
                    , "                          #     datetime(2024-08-09T18:13:18.426000118Z)"
                )
                .build()
        );
    }

    static TestCaseBuilder builder(String name) {
        return new TestCaseBuilder()
            .name(name);
    }

    private static StructuredJsonGenerator getGenerator() {
        return SdkStructuredRpcV2CborFactory.SDK_CBOR_FACTORY.createWriter("application/cbor");
    }

    private String annotatedToHex(String... args) {
        StringBuilder buf = new StringBuilder();
        for (String arg : args) {
            String cleaned = arg.replaceFirst("#.*$", "")
                                .replaceAll("\\s+", "");
            buf.append(cleaned);
        }
        return buf.toString();
    }

    public static String toHexEncoded(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    static class TestCase {
        private final String name;
        private final Consumer<StructuredJsonGenerator> setup;
        private final String[] expected;

        public TestCase(TestCaseBuilder builder) {
            this.name = Objects.requireNonNull(builder.name, "name");
            this.setup = Objects.requireNonNull(builder.setup, "setup");
            this.expected = Objects.requireNonNull(builder.expected, "expected");
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class TestCaseBuilder {
        private String name;
        private Consumer<StructuredJsonGenerator> setup;
        private String[] expected;

        public TestCaseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TestCaseBuilder setup(Consumer<StructuredJsonGenerator> setup) {
            this.setup = setup;
            return this;
        }

        public TestCaseBuilder expected(String... expected) {
            this.expected = expected;
            return this;
        }

        public TestCase build() {
            return new TestCase(this);
        }
    }
}
