/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.fromAttributeValue;
import static software.amazon.awssdk.enhanced.dynamodb.converter.attribute.bundled.ConverterTestUtils.toAttributeValue;

import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.model.ItemAttributeValue;

public class BinaryAttributeConvertersTest {
    @Test
    public void byteArrayAttributeConverterBehaves() {
        ByteArrayAttributeConverter converter = ByteArrayAttributeConverter.create();

        byte[] emptyBytes = new byte[0];
        byte[] bytes = "foo".getBytes();

        assertThat(toAttributeValue(converter, bytes).asBytes().asByteArray()).isEqualTo(bytes);
        assertThat(toAttributeValue(converter, emptyBytes).asBytes().asByteArray()).isEqualTo(emptyBytes);

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")))).isEqualTo(bytes);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")))).isEqualTo(emptyBytes);
    }

    @Test
    public void sdkBytesAttributeConverterBehaves() {
        SdkBytesAttributeConverter converter = SdkBytesAttributeConverter.create();
        SdkBytes bytes = SdkBytes.fromUtf8String("");
        assertThat(toAttributeValue(converter, bytes).asBytes()).isSameAs(bytes);
        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBytes(bytes))).isSameAs(bytes);
    }

    @Test
    public void byteAttributeConverterBehaves() {
        ByteAttributeConverter converter = ByteAttributeConverter.create();

        byte aByte = 42;
        byte[] bytes = {aByte};
        SdkBytes sdkBytes = SdkBytes.fromByteArray(bytes);

        assertThat(toAttributeValue(converter, aByte).asBytes().asByteArray()).isEqualTo(bytes);

        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String(""))));
        assertFails(() -> fromAttributeValue(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("ab"))));

        assertThat(fromAttributeValue(converter, ItemAttributeValue.fromBytes(sdkBytes))).isEqualTo(aByte);
    }
}
