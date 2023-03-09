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
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.defaultDocBuilder;

import org.junit.jupiter.api.Test;
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
}
