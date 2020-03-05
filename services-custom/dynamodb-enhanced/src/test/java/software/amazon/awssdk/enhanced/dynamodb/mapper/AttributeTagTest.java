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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.internal.mapper.StaticTableMetadata;

@RunWith(MockitoJUnitRunner.class)
public class AttributeTagTest {
    private static final Map<String, Object> CUSTOM_METADATA = IntStream.range(0, 2).mapToObj(Integer::toString)
        .collect(Collectors.toMap(Function.identity(), Function.identity()));
    private static final String ATTRIBUTE_NAME = "test-attribute";

    @Mock
    private StaticTableMetadata.Builder mockTableMetadata;

    private static class KeyAttributeTag extends AttributeTag {
        @Override
        protected Map<String, Object> customMetadataForAttribute(String attributeName,
                                                                 AttributeValueType attributeValueType) {
            return CUSTOM_METADATA;
        }

        @Override
        protected boolean isKeyAttribute() {
            return true;
        }
    }

    private static class NonKeyAttributeTag extends AttributeTag {

        @Override
        protected Map<String, Object> customMetadataForAttribute(String attributeName,
                                                                 AttributeValueType attributeValueType) {
            return CUSTOM_METADATA;
        }

        @Override
        protected boolean isKeyAttribute() {
            return false;
        }
    }

    @Test
    public void keyAttribute_setsTableMetadataCorrectly() {
        AttributeTag attributeTag = new KeyAttributeTag();
        attributeTag.setTableMetadataForAttribute(ATTRIBUTE_NAME, AttributeValueType.S, mockTableMetadata);
        verify(mockTableMetadata).markAttributeAsKey(ATTRIBUTE_NAME, AttributeValueType.S);
        verify(mockTableMetadata).addCustomMetadataObject("0", "0");
        verify(mockTableMetadata).addCustomMetadataObject("1", "1");
    }

    @Test
    public void nonKeyAttribute_setsTableMetadataCorrectly() {
        AttributeTag attributeTag = new NonKeyAttributeTag();
        attributeTag.setTableMetadataForAttribute(ATTRIBUTE_NAME, AttributeValueType.S, mockTableMetadata);
        verify(mockTableMetadata, never()).markAttributeAsKey(ATTRIBUTE_NAME, AttributeValueType.S);
        verify(mockTableMetadata).addCustomMetadataObject("0", "0");
        verify(mockTableMetadata).addCustomMetadataObject("1", "1");
    }

}
