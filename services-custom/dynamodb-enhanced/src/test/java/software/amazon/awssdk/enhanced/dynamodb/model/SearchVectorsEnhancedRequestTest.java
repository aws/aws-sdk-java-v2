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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

@RunWith(MockitoJUnitRunner.class)
public class SearchVectorsEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder().build();

        assertThat(request.searchVector()).isNull();
        assertThat(request.topK()).isNull();
        assertThat(request.searchConditionExpression()).isNull();
        assertThat(request.attributesToProject()).isNull();
        assertThat(request.nestedAttributesToProject()).isNull();
        assertThat(request.returnConsumedCapacityAsString()).isNull();
    }

    @Test
    public void builder_maximal() {
        Expression condition = Expression.builder()
                                         .expression("#pk = :val")
                                         .putExpressionName("#pk", "partitionKey")
                                         .putExpressionValue(":val", AttributeValue.fromS("abc"))
                                         .build();

        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(new float[] {0.1f, 0.2f, 0.3f})
                                                                           .topK(5)
                                                                           .searchConditionExpression(condition)
                                                                           .attributesToProject("title", "author")
                                                                           .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                           .build();

        assertThat(request.searchVector().length).isEqualTo(3);
        assertThat(request.topK()).isEqualTo(5);
        assertThat(request.searchConditionExpression()).isEqualTo(condition);
        assertThat(request.attributesToProject()).isEqualTo(Arrays.asList("title", "author"));
        assertThat(request.returnConsumedCapacity()).isEqualTo(ReturnConsumedCapacity.TOTAL);
    }

    @Test
    public void toBuilder_roundTrip() {
        Expression condition = Expression.builder()
                                         .expression("x = :x")
                                         .putExpressionValue(":x", AttributeValue.fromS("y"))
                                         .build();

        SearchVectorsEnhancedRequest original = SearchVectorsEnhancedRequest.builder()
                                                                            .searchVector(new float[] {1.0f, 2.0f})
                                                                            .topK(10)
                                                                            .searchConditionExpression(condition)
                                                                            .addNestedAttributeToProject(
                                                                                NestedAttributeName.create("a", "b"))
                                                                            .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                                            .build();

        SearchVectorsEnhancedRequest rebuilt = original.toBuilder().build();

        assertThat(rebuilt).isEqualTo(original);
    }

    @Test
    public void searchVector_floatArray_defensiveCopy() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(vector)
                                                                           .build();

        vector[0] = 99.0f;
        assertThat(request.searchVector()[0]).isEqualTo(1.0f);

        float[] returned = request.searchVector();
        returned[1] = 99.0f;
        assertThat(request.searchVector()[1]).isEqualTo(2.0f);
    }

    @Test
    public void searchVector_listOverload() {
        List<Float> vector = Arrays.asList(0.5f, 1.5f, 2.5f);

        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(vector)
                                                                           .build();

        assertThat(request.searchVector().length).isEqualTo(3);
        assertThat(request.searchVector()[0]).isEqualTo(0.5f);
        assertThat(request.searchVector()[2]).isEqualTo(2.5f);
    }

    @Test
    public void addNestedAttributeToProject() {
        NestedAttributeName nested = NestedAttributeName.create("metadata", "tags", "primary");

        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(new float[] {1.0f})
                                                                           .addNestedAttributeToProject(nested)
                                                                           .build();

        assertThat(request.nestedAttributesToProject()).containsExactly(nested);
        assertThat(request.attributesToProject()).containsExactly("metadata.tags.primary");
    }

    @Test
    public void equals_sameObject_returnsTrue() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(new float[] {1.0f})
                                                                           .topK(5)
                                                                           .build();

        assertThat(request.equals(request)).isTrue();
    }

    @Test
    public void equals_null_returnsFalse() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(new float[] {1.0f})
                                                                           .build();

        assertThat(request.equals(null)).isFalse();
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector(new float[] {1.0f})
                                                                           .build();

        assertThat(request.equals("string")).isFalse();
    }

    @Test
    public void equals_differentSearchVector_returnsFalse() {
        SearchVectorsEnhancedRequest request1 = SearchVectorsEnhancedRequest.builder()
                                                                            .searchVector(new float[] {1.0f, 2.0f})
                                                                            .topK(5)
                                                                            .build();
        SearchVectorsEnhancedRequest request2 = SearchVectorsEnhancedRequest.builder()
                                                                            .searchVector(new float[] {3.0f, 4.0f})
                                                                            .topK(5)
                                                                            .build();

        assertThat(request1.equals(request2)).isFalse();
    }

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        SearchVectorsEnhancedRequest request1 = SearchVectorsEnhancedRequest.builder()
                                                                            .searchVector(new float[] {1.0f, 2.0f})
                                                                            .topK(10)
                                                                            .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                            .build();
        SearchVectorsEnhancedRequest request2 = SearchVectorsEnhancedRequest.builder()
                                                                            .searchVector(new float[] {1.0f, 2.0f})
                                                                            .topK(10)
                                                                            .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                            .build();

        assertThat(request1.equals(request2)).isTrue();
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void searchVector_nullList_returnsNull() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .searchVector((List<Float>) null)
                                                                           .build();

        assertThat(request.searchVector()).isNull();
    }

    @Test
    public void topK_primitiveOverload() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .topK(7)
                                                                           .build();

        assertThat(request.topK()).isEqualTo(7);
    }

    @Test
    public void attributesToProject_collection_clearsExisting() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .attributesToProject("a", "b")
                                                                           .attributesToProject(Arrays.asList("x", "y"))
                                                                           .build();

        assertThat(request.attributesToProject()).isEqualTo(Arrays.asList("x", "y"));
    }

    @Test
    public void addAttributeToProject_null_isNoOp() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .addAttributeToProject("existing")
                                                                           .addAttributeToProject(null)
                                                                           .build();

        assertThat(request.attributesToProject()).isEqualTo(Collections.singletonList("existing"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addNestedAttributesToProject_nullCollection_isNoOp() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .addAttributeToProject("existing")
                                                                           .addNestedAttributesToProject((Collection<NestedAttributeName>) null)
                                                                           .build();

        assertThat(request.attributesToProject()).isEqualTo(Collections.singletonList("existing"));
    }

    @Test
    public void returnConsumedCapacity_stringOverload() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .returnConsumedCapacity("TOTAL")
                                                                           .build();

        assertThat(request.returnConsumedCapacity()).isEqualTo(ReturnConsumedCapacity.TOTAL);
    }

    @Test
    public void returnConsumedCapacityAsString_returnsRawString() {
        SearchVectorsEnhancedRequest request = SearchVectorsEnhancedRequest.builder()
                                                                           .returnConsumedCapacity("INDEXES")
                                                                           .build();

        assertThat(request.returnConsumedCapacityAsString()).isEqualTo("INDEXES");
    }
}
