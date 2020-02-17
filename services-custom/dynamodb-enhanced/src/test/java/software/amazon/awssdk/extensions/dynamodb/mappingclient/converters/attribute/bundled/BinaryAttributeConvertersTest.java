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

package software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.assertFails;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.extensions.dynamodb.mappingclient.converters.attribute.bundled.ConverterTestUtils.transformTo;

import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.ItemAttributeValue;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ByteArrayAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.ByteAttributeConverter;
import software.amazon.awssdk.extensions.dynamodb.mappingclient.converter.attribute.bundled.SdkBytesAttributeConverter;

public class BinaryAttributeConvertersTest {
    @Test
    public void byteArrayAttributeConverterBehaves() {
        ByteArrayAttributeConverter converter = ByteArrayAttributeConverter.create();

        byte[] emptyBytes = new byte[0];
        byte[] bytes = "foo".getBytes();

        assertThat(transformFrom(converter, bytes).b().asByteArray()).isEqualTo(bytes);
        assertThat(transformFrom(converter, emptyBytes).b().asByteArray()).isEqualTo(emptyBytes);

        assertThat(transformTo(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")).toGeneratedAttributeValue())).isEqualTo(bytes);
        assertThat(transformTo(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")).toGeneratedAttributeValue())).isEqualTo(emptyBytes);
    }

    @Test
    public void sdkBytesAttributeConverterBehaves() {
        SdkBytesAttributeConverter converter = SdkBytesAttributeConverter.create();
        SdkBytes bytes = SdkBytes.fromUtf8String("");
        assertThat(transformFrom(converter, bytes).b()).isSameAs(bytes);
        assertThat(transformTo(converter, ItemAttributeValue.fromBytes(bytes).toGeneratedAttributeValue())).isSameAs(bytes);
    }

    @Test
    public void byteAttributeConverterBehaves() {
        ByteAttributeConverter converter = ByteAttributeConverter.create();

        byte aByte = 42;
        byte[] bytes = {aByte};
        SdkBytes sdkBytes = SdkBytes.fromByteArray(bytes);

        assertThat(transformFrom(converter, aByte).b().asByteArray()).isEqualTo(bytes);

        assertFails(() -> transformTo(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")).toGeneratedAttributeValue()));
        assertFails(() -> transformTo(converter, ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("ab")).toGeneratedAttributeValue()));

        assertThat(transformTo(converter, ItemAttributeValue.fromBytes(sdkBytes).toGeneratedAttributeValue())).isEqualTo(aByte);
    }
}
