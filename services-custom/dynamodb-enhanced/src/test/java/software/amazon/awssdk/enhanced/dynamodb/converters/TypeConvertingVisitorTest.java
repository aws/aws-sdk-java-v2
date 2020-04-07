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

package software.amazon.awssdk.enhanced.dynamodb.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.TypeConvertingVisitor;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.EnhancedAttributeValue;

public class TypeConvertingVisitorTest {
    @Test
    public void defaultConvertersThrowExceptions() {
        assertThat(DefaultVisitor.INSTANCE.convert(EnhancedAttributeValue.nullValue())).isEqualTo(null);

        assertDefaultConversionFails(EnhancedAttributeValue.fromString("foo"));
        assertDefaultConversionFails(EnhancedAttributeValue.fromNumber("1"));
        assertDefaultConversionFails(EnhancedAttributeValue.fromBoolean(true));
        assertDefaultConversionFails(EnhancedAttributeValue.fromBytes(SdkBytes.fromUtf8String("")));
        assertDefaultConversionFails(EnhancedAttributeValue.fromSetOfStrings(Collections.emptyList()));
        assertDefaultConversionFails(EnhancedAttributeValue.fromSetOfNumbers(Collections.emptyList()));
        assertDefaultConversionFails(EnhancedAttributeValue.fromSetOfBytes(Collections.emptyList()));
        assertDefaultConversionFails(EnhancedAttributeValue.fromListOfAttributeValues(Collections.emptyList()));
        assertDefaultConversionFails(EnhancedAttributeValue.fromMap(Collections.emptyMap()));
    }

    private void assertDefaultConversionFails(EnhancedAttributeValue attributeValue) {
        assertThatThrownBy(() -> DefaultVisitor.INSTANCE.convert(attributeValue)).isInstanceOf(IllegalStateException.class);
    }


    private static class DefaultVisitor extends TypeConvertingVisitor<Void> {
        private static final DefaultVisitor INSTANCE = new DefaultVisitor();

        protected DefaultVisitor() {
            super(Void.class);
        }
    }

}
