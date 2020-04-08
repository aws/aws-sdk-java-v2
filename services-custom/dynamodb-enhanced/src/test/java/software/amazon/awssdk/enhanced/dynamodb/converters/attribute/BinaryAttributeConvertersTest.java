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

package software.amazon.awssdk.enhanced.dynamodb.converters.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformFrom;
import static software.amazon.awssdk.enhanced.dynamodb.converters.attribute.ConverterTestUtils.transformTo;

import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.ByteArrayAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SdkBytesAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.SetAttributeConverter;

public class BinaryAttributeConvertersTest {
    @Test
    public void byteArrayAttributeConverterBehaves() {
        ByteArrayAttributeConverter converter = ByteArrayAttributeConverter.create();

        byte[] emptyBytes = new byte[0];
        byte[] bytes = "foo".getBytes();

        assertThat(transformFrom(converter, bytes).b().asByteArray()).isEqualTo(bytes);
        assertThat(transformFrom(converter, emptyBytes).b().asByteArray()).isEqualTo(emptyBytes);

        assertThat(transformTo(converter, EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("foo")).toAttributeValue())).isEqualTo(bytes);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("")).toAttributeValue())).isEqualTo(emptyBytes);
    }

    @Test
    public void sdkBytesAttributeConverterBehaves() {
        SdkBytesAttributeConverter converter = SdkBytesAttributeConverter.create();
        SdkBytes bytes = SdkBytes.fromUtf8String("");
        assertThat(transformFrom(converter, bytes).b()).isSameAs(bytes);
        assertThat(transformTo(converter, EnhancedAttributeValue.fromBytes(bytes).toAttributeValue())).isSameAs(bytes);
    }

    @Test
    public void sdkBytesSetAttributeConverter_ReturnsBSType() {
        SetAttributeConverter<Set<SdkBytes>> bytesSet = SetAttributeConverter.setConverter(SdkBytesAttributeConverter.create());
        assertThat(bytesSet.attributeValueType()).isEqualTo(AttributeValueType.BS);
    }
}
