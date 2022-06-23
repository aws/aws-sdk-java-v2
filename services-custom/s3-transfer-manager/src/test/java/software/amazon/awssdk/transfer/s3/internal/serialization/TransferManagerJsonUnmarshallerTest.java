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
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NullJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.NumberJsonNode;
import software.amazon.awssdk.protocols.jsoncore.internal.StringJsonNode;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.DateUtils;

class TransferManagerJsonUnmarshallerTest {

    @ParameterizedTest
    @MethodSource("unmarshallingValues")
    void deserialize_ShouldWorkForAllSupportedTypes(JsonNode node, Object o, TransferManagerJsonUnmarshaller<?> unmarshaller)  {
        Object param = unmarshaller.unmarshall(node);
        assertThat(param).isEqualTo(o);
    }

    private static Stream<Arguments> unmarshallingValues() {
        return Stream.of(Arguments.of(new StringJsonNode("String"), "String", TransferManagerJsonUnmarshaller.STRING),
                         Arguments.of(new NumberJsonNode("100"), (short) 100, TransferManagerJsonUnmarshaller.SHORT),
                         Arguments.of(new NumberJsonNode("100"), 100, TransferManagerJsonUnmarshaller.INTEGER),
                         Arguments.of(new NumberJsonNode("100"), 100L, TransferManagerJsonUnmarshaller.LONG),
                         Arguments.of(new NumberJsonNode("12.34"), 12.34f, TransferManagerJsonUnmarshaller.FLOAT),
                         Arguments.of(new NumberJsonNode("12.34"), 12.34d, TransferManagerJsonUnmarshaller.DOUBLE),
                         Arguments.of(new NumberJsonNode("2.3"),
                                      new BigDecimal("2.3"),
                                      TransferManagerJsonUnmarshaller.BIG_DECIMAL),
                         Arguments.of(new NumberJsonNode("1646734530.000"),
                                      DateUtils.parseIso8601Date("2022-03-08T10:15:30Z"),
                                      TransferManagerJsonUnmarshaller.INSTANT),
                         Arguments.of(NullJsonNode.instance(), null, TransferManagerJsonUnmarshaller.NULL),
                         Arguments.of(new StringJsonNode(BinaryUtils.toBase64(SdkBytes.fromString("100", StandardCharsets.UTF_8)
                                                                                      .asByteArray())),
                                      SdkBytes.fromString("100", StandardCharsets.UTF_8),
                                      TransferManagerJsonUnmarshaller.SDK_BYTES)
        );
    }

}
