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

class TransferManagerJsonMarshallerTest {

    @ParameterizedTest
    @MethodSource("marshallingValues")
    void serialize_ShouldWorkForAllSupportedTypes(Object o, TransferManagerJsonMarshaller<Object> marshaller)  {
        JsonWriter writer = JsonWriter.create();
        writer.writeStartObject();
        marshaller.marshall(o, writer, "param");
        writer.writeEndObject();
        String serializedResult = new String(writer.getBytes(), StandardCharsets.UTF_8);
        if (o == null) {
            assertThat(serializedResult).isEqualTo("{}");
        } else {
            assertThat(serializedResult.contains(o.toString()));
        }
    }

    private static Stream<Arguments> marshallingValues() {
        return Stream.of(Arguments.of("String", TransferManagerJsonMarshaller.STRING),
                         Arguments.of((short) 10, TransferManagerJsonMarshaller.SHORT),
                         Arguments.of(100, TransferManagerJsonMarshaller.INTEGER),
                         Arguments.of(100L, TransferManagerJsonMarshaller.LONG),
                         Arguments.of(Instant.now(), TransferManagerJsonMarshaller.INSTANT),
                         Arguments.of(null, TransferManagerJsonMarshaller.NULL),
                         Arguments.of(12.34f, TransferManagerJsonMarshaller.FLOAT),
                         Arguments.of(12.34d, TransferManagerJsonMarshaller.DOUBLE),
                         Arguments.of(new BigDecimal(34), TransferManagerJsonMarshaller.BIG_DECIMAL),
                         Arguments.of(true, TransferManagerJsonMarshaller.BOOLEAN),
                         Arguments.of(SdkBytes.fromString("String", StandardCharsets.UTF_8),
                                      TransferManagerJsonMarshaller.SDK_BYTES),
                         Arguments.of(Arrays.asList(100, 45), TransferManagerJsonMarshaller.LIST),
                         Arguments.of(Arrays.asList("100", "45"), TransferManagerJsonMarshaller.LIST),
                         Arguments.of(Collections.singletonMap("key", "value"), TransferManagerJsonMarshaller.MAP),
                         Arguments.of(new HashMap<String, Long>() {{
                             put("key1", 100L);
                             put("key2", 200L);
                         }}, TransferManagerJsonMarshaller.MAP));
    }

}
