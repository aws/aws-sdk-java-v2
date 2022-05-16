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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.utils.DateUtils;

class TransferManagerJsonMarshallerTest {

    @ParameterizedTest
    @MethodSource("marshallingValues")
    void serialize_ShouldWorkForAllSupportedTypes(Object o, TransferManagerJsonMarshaller<Object> marshaller, String expected)  {
        JsonWriter writer = JsonWriter.create();
        writer.writeStartObject();
        marshaller.marshall(o, writer, "param");
        writer.writeEndObject();
        String serializedResult = new String(writer.getBytes(), StandardCharsets.UTF_8);
        assertThat(serializedResult).contains(expected);
    }

    private static Stream<Arguments> marshallingValues() {
        return Stream.of(Arguments.of("String", TransferManagerJsonMarshaller.STRING, "String"),
                         Arguments.of((short) 10, TransferManagerJsonMarshaller.SHORT, Short.toString((short) 10)),
                         Arguments.of(100, TransferManagerJsonMarshaller.INTEGER, Integer.toString(100)),
                         Arguments.of(100L, TransferManagerJsonMarshaller.LONG, Long.toString(100L)),
                         Arguments.of(DateUtils.parseIso8601Date("2022-03-08T10:15:30Z"), TransferManagerJsonMarshaller.INSTANT,
                                      "1646734530.000"),
                         Arguments.of(null, TransferManagerJsonMarshaller.NULL, "{}"),
                         Arguments.of(12.34f, TransferManagerJsonMarshaller.FLOAT, Float.toString(12.34f)),
                         Arguments.of(12.34d, TransferManagerJsonMarshaller.DOUBLE, Double.toString(12.34d)),
                         Arguments.of(new BigDecimal(34), TransferManagerJsonMarshaller.BIG_DECIMAL, (new BigDecimal(34)).toString()),
                         Arguments.of(true, TransferManagerJsonMarshaller.BOOLEAN, "true"),
                         Arguments.of(SdkBytes.fromString("String", StandardCharsets.UTF_8),
                                      TransferManagerJsonMarshaller.SDK_BYTES, "U3RyaW5n"),
                         Arguments.of(Arrays.asList(100, 45), TransferManagerJsonMarshaller.LIST, "[100,45]"),
                         Arguments.of(Arrays.asList("100", "45"), TransferManagerJsonMarshaller.LIST, "[\"100\",\"45\"]"),
                         Arguments.of(Collections.singletonMap("key", "value"), TransferManagerJsonMarshaller.MAP,
                                      "{\"key\":\"value\"}"),
                         Arguments.of(new HashMap<String, Long>() {{
                             put("key1", 100L);
                             put("key2", 200L);
                         }}, TransferManagerJsonMarshaller.MAP, "{\"key1\":100,\"key2\":200}")
        );
    }

    private static String serializedValue(String paramValue) {
        return String.format("{\"param\":%s}", paramValue);
    }

}
