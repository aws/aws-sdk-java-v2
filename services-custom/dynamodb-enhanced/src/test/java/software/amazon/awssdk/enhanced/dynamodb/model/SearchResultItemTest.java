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

public class SearchResultItemTest {

    @Test
    public void builder_setsItemAndScore() {
        SearchResultItem<String> result = SearchResultItem.<String>builder()
                                                          .item("hello")
                                                          .score(0.95)
                                                          .build();

        assertThat(result.item()).isEqualTo("hello");
        assertThat(result.score()).isEqualTo(0.95);
    }

    @Test
    public void score_defaultsToNull() {
        SearchResultItem<String> result = SearchResultItem.<String>builder()
                                                          .item("hello")
                                                          .build();

        assertThat(result.score()).isNull();
    }

    @Test
    public void equals_sameObject() {
        SearchResultItem<String> result = SearchResultItem.<String>builder()
                                                          .item("hello")
                                                          .score(0.5)
                                                          .build();

        assertThat(result).isSameAs(result);
        assertThat(result.equals(result)).isTrue();
    }

    @Test
    public void equals_equalObjects() {
        SearchResultItem<String> result1 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.5)
                                                           .build();

        SearchResultItem<String> result2 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.5)
                                                           .build();

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    public void equals_null_returnsFalse() {
        SearchResultItem<String> result = SearchResultItem.<String>builder()
                                                          .item("hello")
                                                          .build();

        assertThat(result.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        SearchResultItem<String> result = SearchResultItem.<String>builder()
                                                          .item("hello")
                                                          .build();

        assertThat(result.equals("not-a-result")).isFalse();
    }

    @Test
    public void equals_differentItem_returnsFalse() {
        SearchResultItem<String> result1 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.5)
                                                           .build();

        SearchResultItem<String> result2 = SearchResultItem.<String>builder()
                                                           .item("world")
                                                           .score(0.5)
                                                           .build();

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    public void equals_differentScore_returnsFalse() {
        SearchResultItem<String> result1 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.5)
                                                           .build();

        SearchResultItem<String> result2 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.9)
                                                           .build();

        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        SearchResultItem<String> result1 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.75)
                                                           .build();

        SearchResultItem<String> result2 = SearchResultItem.<String>builder()
                                                           .item("hello")
                                                           .score(0.75)
                                                           .build();

        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }
}
