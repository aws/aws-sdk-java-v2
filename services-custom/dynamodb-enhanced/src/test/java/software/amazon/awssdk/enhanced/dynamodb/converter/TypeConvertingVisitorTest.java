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

package software.amazon.awssdk.enhanced.dynamodb.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.converter.attribute.ItemAttributeValue;

public class TypeConvertingVisitorTest {
    @Test
    public void defaultConvertersThrowExceptions() {
        assertThat(DefaultVisitor.INSTANCE.convert(ItemAttributeValue.nullValue())).isEqualTo(null);

        assertDefaultConversionFails(ItemAttributeValue.fromString("foo"));
        assertDefaultConversionFails(ItemAttributeValue.fromNumber("1"));
        assertDefaultConversionFails(ItemAttributeValue.fromBoolean(true));
        assertDefaultConversionFails(ItemAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));
        assertDefaultConversionFails(ItemAttributeValue.fromSetOfStrings(Collections.emptyList()));
        assertDefaultConversionFails(ItemAttributeValue.fromSetOfNumbers(Collections.emptyList()));
        assertDefaultConversionFails(ItemAttributeValue.fromSetOfBytes(Collections.emptyList()));
        assertDefaultConversionFails(ItemAttributeValue.fromListOfAttributeValues(Collections.emptyList()));
        assertDefaultConversionFails(ItemAttributeValue.fromMap(Collections.emptyMap()));
    }

    private void assertDefaultConversionFails(ItemAttributeValue attributeValue) {
        assertThatThrownBy(() -> DefaultVisitor.INSTANCE.convert(attributeValue)).isInstanceOf(IllegalStateException.class);
    }


    private static class DefaultVisitor extends TypeConvertingVisitor<Void> {
        private static final DefaultVisitor INSTANCE = new DefaultVisitor();

        protected DefaultVisitor() {
            super(Void.class);
        }
    }

}