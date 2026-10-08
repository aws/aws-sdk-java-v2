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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.defaultDocBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
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

    @Test
    void putStringSet_onNullValue_throwsException() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder()
                                                                            .attributeConverterProviders(defaultProvider());
        Set<String> values = new LinkedHashSet<>(Arrays.asList("a", null, "b"));

        assertThatIllegalStateException()
            .isThrownBy(() -> builder.putStringSet("stringSet", values))
            .withMessage("Set must not have null values.");
    }

    @Test
    void putNumberSet_onNullValue_throwsException() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder()
                                                                            .attributeConverterProviders(defaultProvider());
        Set<Number> values = new LinkedHashSet<>(Arrays.asList(1, null, 2));

        assertThatIllegalStateException()
            .isThrownBy(() -> builder.putNumberSet("numberSet", values))
            .withMessage("Set must not have null values.");
    }

    @Test
    void putBytesSet_onNullValue_throwsException() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder()
                                                                            .attributeConverterProviders(defaultProvider());
        Set<SdkBytes> values = new LinkedHashSet<>(Arrays.asList(SdkBytes.fromUtf8String("a"), null));

        assertThatIllegalStateException()
            .isThrownBy(() -> builder.putBytesSet("bytesSet", values))
            .withMessage("Set must not have null values.");
    }

    @Test
    void toJson_onEmptyDocument_returnsEmptyJson() {
        DefaultEnhancedDocument doc = (DefaultEnhancedDocument) DefaultEnhancedDocument.builder().build();
        assertThat(doc.toJson()).isEqualTo("{}");
    }

    @Test
    void toJson_onNonEmptyDocument_returnsJsonWithKeyAndValue() {
        DefaultEnhancedDocument doc = (DefaultEnhancedDocument)
            DefaultEnhancedDocument.builder()
                                   .putString("key", "value")
                                   .attributeConverterProviders(defaultProvider())
                                   .build();
        assertThat(doc.toJson()).contains("key");
        assertThat(doc.toJson()).contains("value");
    }

    @Test
    void putStringSet_onValidSet_addsStringSet() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder()
                                                                            .attributeConverterProviders(defaultProvider());
        Set<String> values = new LinkedHashSet<>(Arrays.asList("a", "b"));

        builder.putStringSet("stringSet", values);

        DefaultEnhancedDocument doc = (DefaultEnhancedDocument) builder.build();
        assertThat(doc.toMap().get("stringSet").ss()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void putNumberSet_onValidSet_addsNumberSet() {
        DefaultEnhancedDocument.DefaultBuilder builder = (DefaultEnhancedDocument.DefaultBuilder)
            DefaultEnhancedDocument.builder().attributeConverterProviders(defaultProvider());
        Set<Number> values = new LinkedHashSet<>(Arrays.asList(1, 2));

        builder.putNumberSet("numberSet", values);

        DefaultEnhancedDocument doc = (DefaultEnhancedDocument) builder.build();
        assertThat(doc.toMap().get("numberSet").ns()).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void putBytesSet_onValidSet_addsBytesSet() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder()
                                                                            .attributeConverterProviders(defaultProvider());
        Set<SdkBytes> values = new LinkedHashSet<>(Arrays.asList(SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b")));

        builder.putBytesSet("bytesSet", values);

        DefaultEnhancedDocument doc = (DefaultEnhancedDocument) builder.build();
        assertThat(doc.toMap().get("bytesSet").bs()).hasSize(2);
    }

    @Test
    void json_onValidJson_setsAttributeValueMap() {
        String json = "{\"foo\":{\"S\":\"bar\"}}";
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder();

        builder.json(json);

        DefaultEnhancedDocument doc = (DefaultEnhancedDocument) builder.build();
        assertThat(doc.toMap()).containsKey("foo");
        assertThat(doc.toMap().get("foo").m().get("S").s()).isEqualTo("bar");
    }

    @Test
    void json_onInvalidJson_throwsUncheckedIOException() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder();

        assertThatThrownBy(() -> builder.json("not a json"))
            .isInstanceOf(java.io.UncheckedIOException.class)
            .hasMessageContaining("Unrecognized token");
    }

    @Test
    void json_onJsonParsingToNull_throwsIllegalArgumentException() {
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder();

        assertThatThrownBy(() -> builder.json(""))
            .isInstanceOf(java.lang.IllegalArgumentException.class)
            .hasMessageContaining("Could not parse argument json");
        assertThatThrownBy(() -> builder.json("   "))
            .isInstanceOf(java.lang.IllegalArgumentException.class)
            .hasMessageContaining("Could not parse argument json");
    }
}
