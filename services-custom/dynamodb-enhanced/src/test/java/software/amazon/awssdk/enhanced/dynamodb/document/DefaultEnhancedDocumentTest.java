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

package software.amazon.awssdk.enhanced.dynamodb.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.defaultDocBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
class DefaultEnhancedDocumentTest {

    @Test
    void copyCreatedFromToBuilder() {
        DefaultEnhancedDocument originalDoc = (DefaultEnhancedDocument) defaultDocBuilder()
            .putString("stringKey", "stringValue")
            .build();
        DefaultEnhancedDocument copiedDoc = (DefaultEnhancedDocument) originalDoc.toBuilder().build();
        DefaultEnhancedDocument copyAndAlter =
            (DefaultEnhancedDocument) originalDoc.toBuilder().putString("keyOne", "valueOne").build();
        assertThat(originalDoc.toMap()).isEqualTo(copiedDoc.toMap());
        assertThat(originalDoc.toMap().keySet()).hasSize(1);
        assertThat(copyAndAlter.toMap().keySet()).hasSize(2);
        assertThat(copyAndAlter.getString("stringKey")).isEqualTo("stringValue");
        assertThat(copyAndAlter.getString("keyOne")).isEqualTo("valueOne");
        assertThat(originalDoc.toMap()).isEqualTo(copiedDoc.toMap());
    }

    @Test
    void isNull_inDocumentGet() {
        DefaultEnhancedDocument nullDocument = (DefaultEnhancedDocument) DefaultEnhancedDocument.builder()
                                                                                                .attributeConverterProviders(defaultProvider())
                                                                                                .putNull("nullDocument")
                                                                                                .putString("nonNull",
                                                                                                           "stringValue")
                                                                                                .build();
        assertThat(nullDocument.isNull("nullDocument")).isTrue();
        assertThat(nullDocument.isNull("nonNull")).isFalse();
        assertThat(nullDocument.toMap()).containsEntry("nullDocument", AttributeValue.fromNul(true));

    }

    @Test
    void isNull_when_putObjectWithNullAttribute() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder().attributeConverterProviders(defaultProvider());
        builder.putObject("nullAttribute", AttributeValue.fromNul(true));
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) builder.build();
        assertThat(document.isNull("nullAttribute")).isTrue();
    }

    @Test
    void getListOfUnknownType_forUnknownAttributeName_returnsNull() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putNull("nullAttributeName")
            .putString("attributeName", "attributeValue")
            .build();

        List<AttributeValue> result = document.getListOfUnknownType("unknownAttributeName");
        assertThat(result).isNull();
    }

    @Test
    void getListOfUnknownType_forListAttributeName_returnsCorrectValue() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putList(
                "listAttributeName",
                Arrays.asList("listAttributeValue1", "listAttributeValue2"),
                EnhancedType.of(String.class))
            .build();

        List<AttributeValue> result = document.getListOfUnknownType("listAttributeName");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result)
            .containsExactlyInAnyOrder(
                AttributeValue.builder().s("listAttributeValue1").build(),
                AttributeValue.builder().s("listAttributeValue2").build());
    }

    @Test
    void getListOfUnknownType_forNullAttributeName_throwsException() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putNull("nullAttributeName")
            .build();

        assertThatIllegalStateException()
            .isThrownBy(() -> document.getListOfUnknownType("nullAttributeName"))
            .withMessageContaining("Cannot get a List from attribute value of Type NUL");
    }

    @Test
    void getListOfUnknownType_forStringAttributeName_throwsException() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putString("stringAttributeName", "stringAttributeValue")
            .build();

        assertThatIllegalStateException()
            .isThrownBy(() -> document.getListOfUnknownType("stringAttributeName"))
            .withMessageContaining("Cannot get a List from attribute value of Type S");
    }

    @Test
    void getMapOfUnknownType_forUnknownAttributeName_returnsNull() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putNull("nullAttributeName")
            .putString("attributeName", "attributeValue")
            .build();

        Map<String, AttributeValue> result = document.getMapOfUnknownType("unknownAttributeName");
        assertThat(result).isNull();
    }

    @Test
    void getMapOfUnknownType_forMapAttributeName_returnsCorrectValue() {
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("innerMapKey1", "innerMapValue1");
        innerMap.put("innerMapKey2", "innerMapValue2");

        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putMap(
                "mapAttributeName",
                innerMap,
                EnhancedType.of(String.class),
                EnhancedType.of(String.class))
            .build();

        Map<String, AttributeValue> result = document.getMapOfUnknownType("mapAttributeName");

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get("innerMapKey1").s()).isEqualTo("innerMapValue1");
        assertThat(result.get("innerMapKey2").s()).isEqualTo("innerMapValue2");
    }

    @Test
    void getMapOfUnknownType_forNullAttributeName_throwsException() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putNull("nullAttributeName")
            .build();

        assertThatIllegalStateException()
            .isThrownBy(() -> document.getMapOfUnknownType("nullAttributeName"))
            .withMessageContaining("Cannot get a Map from attribute value of Type NUL");
    }

    @Test
    void getMapOfUnknownType_forStringAttributeName_throwsException() {
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) defaultDocBuilder()
            .attributeConverterProviders(defaultProvider())
            .putString("stringAttributeName", "stringAttributeValue")
            .build();

        assertThatIllegalStateException()
            .isThrownBy(() -> document.getMapOfUnknownType("stringAttributeName"))
            .withMessageContaining("Cannot get a Map from attribute value of Type S");
    }
}
