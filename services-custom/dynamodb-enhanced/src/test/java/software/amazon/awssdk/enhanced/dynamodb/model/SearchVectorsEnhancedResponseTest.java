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

import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.VectorCapacity;

public class SearchVectorsEnhancedResponseTest {

    private static final SearchResultItem<String> RESULT_A =
        SearchResultItem.<String>builder().item("itemA").score(0.9).build();

    private static final SearchResultItem<String> RESULT_B =
        SearchResultItem.<String>builder().item("itemB").score(0.8).build();

    @Test
    public void builder_minimal_nullResultsDefaultsToEmptyList() {
        SearchVectorsEnhancedResponse<String> response = SearchVectorsEnhancedResponse.<String>builder().build();

        assertThat(response.results()).isEmpty();
        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void builder_withResults() {
        SearchVectorsEnhancedResponse<String> response =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .results(Arrays.asList(RESULT_A, RESULT_B))
                                         .build();

        assertThat(response.results()).containsExactly(RESULT_A, RESULT_B);
    }

    @Test
    public void addResult_initializesListWhenNull() {
        SearchVectorsEnhancedResponse<String> response =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .addResult(RESULT_A)
                                         .build();

        assertThat(response.results()).containsExactly(RESULT_A);
    }

    @Test
    public void addResult_appendsToExistingList() {
        SearchVectorsEnhancedResponse<String> response =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .addResult(RESULT_A)
                                         .addResult(RESULT_B)
                                         .build();

        assertThat(response.results()).containsExactly(RESULT_A, RESULT_B);
    }

    @Test
    public void consumedCapacity_returnsSetValue() {
        VectorCapacity capacity = VectorCapacity.builder().build();
        SearchVectorsEnhancedResponse<String> response =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .consumedCapacity(capacity)
                                         .build();

        assertThat(response.consumedCapacity()).isEqualTo(capacity);
    }

    @Test
    public void consumedCapacity_defaultsToNull() {
        SearchVectorsEnhancedResponse<String> response = SearchVectorsEnhancedResponse.<String>builder().build();

        assertThat(response.consumedCapacity()).isNull();
    }

    @Test
    public void equals_sameObject() {
        SearchVectorsEnhancedResponse<String> response =
            SearchVectorsEnhancedResponse.<String>builder().addResult(RESULT_A).build();

        assertThat(response).isSameAs(response);
        assertThat(response.equals(response)).isTrue();
    }

    @Test
    public void equals_equalObjects() {
        SearchVectorsEnhancedResponse<String> response1 =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .results(Arrays.asList(RESULT_A))
                                         .build();

        SearchVectorsEnhancedResponse<String> response2 =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .results(Arrays.asList(RESULT_A))
                                         .build();

        assertThat(response1).isEqualTo(response2);
    }

    @Test
    public void equals_null_returnsFalse() {
        SearchVectorsEnhancedResponse<String> response = SearchVectorsEnhancedResponse.<String>builder().build();

        assertThat(response.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        SearchVectorsEnhancedResponse<String> response = SearchVectorsEnhancedResponse.<String>builder().build();

        assertThat(response.equals("not-a-response")).isFalse();
    }

    @Test
    public void equals_differentResults_returnsFalse() {
        SearchVectorsEnhancedResponse<String> response1 =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .addResult(RESULT_A)
                                         .build();

        SearchVectorsEnhancedResponse<String> response2 =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .addResult(RESULT_B)
                                         .build();

        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        SearchVectorsEnhancedResponse<String> response1 =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .addResult(RESULT_A)
                                         .build();

        SearchVectorsEnhancedResponse<String> response2 =
            SearchVectorsEnhancedResponse.<String>builder()
                                         .addResult(RESULT_A)
                                         .build();

        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
}
