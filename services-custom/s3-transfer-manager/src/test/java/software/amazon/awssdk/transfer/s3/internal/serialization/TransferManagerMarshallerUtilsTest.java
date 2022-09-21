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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.protocol.MarshallingType;

class TransferManagerMarshallerUtilsTest {

    @ParameterizedTest
    @MethodSource("marshallingValues")
    void getMarshallerByType(Object o, MarshallingType<Object> type, TransferManagerJsonMarshaller<?> expectedMarshaller)  {
        TransferManagerJsonMarshaller<Object> marshaller = TransferManagerMarshallingUtils.getMarshaller(type, o);
        assertThat(marshaller).isNotNull()
                              .isEqualTo(expectedMarshaller);
    }

    @ParameterizedTest
    @MethodSource("marshallingValues")
    void findMarshallerByValue(Object o, MarshallingType<Object> type, TransferManagerJsonMarshaller<?> expectedMarshaller)  {
        TransferManagerJsonMarshaller<Object> marshaller = TransferManagerMarshallingUtils.getMarshaller(o);
        assertThat(marshaller).isEqualTo(expectedMarshaller);
    }

    @ParameterizedTest
    @MethodSource("unmarshallingValues")
    void getUnmarshaller(MarshallingType<Object> type, TransferManagerJsonUnmarshaller<?> expectedUnmarshaller)  {
        TransferManagerJsonUnmarshaller<Object> marshaller = (TransferManagerJsonUnmarshaller<Object>) TransferManagerMarshallingUtils.getUnmarshaller(type);
        assertThat(marshaller).isEqualTo(expectedUnmarshaller);
    }

    @Test
    void whenNoMarshaller_shouldThrowException()  {
        assertThatThrownBy(() -> TransferManagerMarshallingUtils.getMarshaller(MarshallingType.DOCUMENT, Document.fromNull()))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot find a marshaller");
    }

    @Test
    void whenNoUnmarshaller_shouldThrowException()  {
        assertThatThrownBy(() -> TransferManagerMarshallingUtils.getUnmarshaller(MarshallingType.DOCUMENT))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot find an unmarshaller");
    }

    private static Stream<Arguments> marshallingValues() {
        return Stream.of(Arguments.of("String", MarshallingType.STRING, TransferManagerJsonMarshaller.STRING),
                         Arguments.of((short) 10, MarshallingType.SHORT, TransferManagerJsonMarshaller.SHORT),
                         Arguments.of(100, MarshallingType.INTEGER, TransferManagerJsonMarshaller.INTEGER),
                         Arguments.of(100L, MarshallingType.LONG, TransferManagerJsonMarshaller.LONG),
                         Arguments.of(Instant.now(), MarshallingType.INSTANT, TransferManagerJsonMarshaller.INSTANT),
                         Arguments.of(null, MarshallingType.NULL, TransferManagerJsonMarshaller.NULL),
                         Arguments.of(12.34f, MarshallingType.FLOAT, TransferManagerJsonMarshaller.FLOAT),
                         Arguments.of(12.34d, MarshallingType.DOUBLE, TransferManagerJsonMarshaller.DOUBLE),
                         Arguments.of(new BigDecimal(34), MarshallingType.BIG_DECIMAL, TransferManagerJsonMarshaller.BIG_DECIMAL),
                         Arguments.of(true, MarshallingType.BOOLEAN, TransferManagerJsonMarshaller.BOOLEAN),
                         Arguments.of(SdkBytes.fromString("String", StandardCharsets.UTF_8),
                                      MarshallingType.SDK_BYTES, TransferManagerJsonMarshaller.SDK_BYTES),
                         Arguments.of(Arrays.asList(100, 45), MarshallingType.LIST, TransferManagerJsonMarshaller.LIST),
                         Arguments.of(Collections.singletonMap("key", "value"), MarshallingType.MAP,
                                      TransferManagerJsonMarshaller.MAP)
        );
    }

    private static Stream<Arguments> unmarshallingValues() {
        return Stream.of(Arguments.of(MarshallingType.STRING, TransferManagerJsonUnmarshaller.STRING),
                         Arguments.of(MarshallingType.SHORT, TransferManagerJsonUnmarshaller.SHORT),
                         Arguments.of(MarshallingType.INTEGER, TransferManagerJsonUnmarshaller.INTEGER),
                         Arguments.of(MarshallingType.LONG, TransferManagerJsonUnmarshaller.LONG),
                         Arguments.of(MarshallingType.INSTANT, TransferManagerJsonUnmarshaller.INSTANT),
                         Arguments.of(MarshallingType.NULL, TransferManagerJsonUnmarshaller.NULL),
                         Arguments.of(MarshallingType.FLOAT, TransferManagerJsonUnmarshaller.FLOAT),
                         Arguments.of(MarshallingType.DOUBLE, TransferManagerJsonUnmarshaller.DOUBLE),
                         Arguments.of(MarshallingType.BIG_DECIMAL, TransferManagerJsonUnmarshaller.BIG_DECIMAL),
                         Arguments.of(MarshallingType.BOOLEAN, TransferManagerJsonUnmarshaller.BOOLEAN),
                         Arguments.of(MarshallingType.SDK_BYTES, TransferManagerJsonUnmarshaller.SDK_BYTES)
        );
    }

}
