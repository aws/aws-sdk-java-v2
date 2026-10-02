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

package software.amazon.awssdk.enhanced.dynamodb.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SearchSchemaElementTest {

    @Test
    public void builder_setsFields() {
        SearchSchemaElement element = SearchSchemaElement.builder()
                                                         .attributeName("pk")
                                                         .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                         .build();

        assertThat(element.attributeName()).isEqualTo("pk");
        assertThat(element.searchSchemaElementType()).isEqualTo(SearchSchemaElementType.HASH);
    }

    @Test
    public void toBuilder_roundTrip() {
        SearchSchemaElement original = SearchSchemaElement.builder()
                                                          .attributeName("category")
                                                          .searchSchemaElementType(SearchSchemaElementType.INLINE_FILTER)
                                                          .build();

        SearchSchemaElement rebuilt = original.toBuilder().build();

        assertThat(rebuilt).isEqualTo(original);
    }

    @Test
    public void equals_sameObject() {
        SearchSchemaElement element = SearchSchemaElement.builder()
                                                         .attributeName("pk")
                                                         .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                         .build();

        assertThat(element).isSameAs(element);
        assertThat(element.equals(element)).isTrue();
    }

    @Test
    public void equals_equalObjects() {
        SearchSchemaElement element1 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        SearchSchemaElement element2 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        assertThat(element1).isEqualTo(element2);
    }

    @Test
    public void equals_null_returnsFalse() {
        SearchSchemaElement element = SearchSchemaElement.builder()
                                                         .attributeName("pk")
                                                         .build();

        assertThat(element.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        SearchSchemaElement element = SearchSchemaElement.builder()
                                                         .attributeName("pk")
                                                         .build();

        assertThat(element.equals("not-an-element")).isFalse();
    }

    @Test
    public void equals_differentAttributeName_returnsFalse() {
        SearchSchemaElement element1 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        SearchSchemaElement element2 = SearchSchemaElement.builder()
                                                          .attributeName("sk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        assertThat(element1).isNotEqualTo(element2);
    }

    @Test
    public void equals_differentType_returnsFalse() {
        SearchSchemaElement element1 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        SearchSchemaElement element2 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.INLINE_FILTER)
                                                          .build();

        assertThat(element1).isNotEqualTo(element2);
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        SearchSchemaElement element1 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        SearchSchemaElement element2 = SearchSchemaElement.builder()
                                                          .attributeName("pk")
                                                          .searchSchemaElementType(SearchSchemaElementType.HASH)
                                                          .build();

        assertThat(element1.hashCode()).isEqualTo(element2.hashCode());
    }
}
