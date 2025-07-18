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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@RunWith(MockitoJUnitRunner.class)
public class DeleteItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        Key key = Key.builder().partitionValue("key").build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;
        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder()
                                                                         .key(key)
                                                                         .conditionExpression(conditionExpression)
                                                                         .returnConsumedCapacity(returnConsumedCapacity)
                                                                         .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                         .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                         .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnConsumedCapacity(), is(returnConsumedCapacity));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(returnConsumedCapacity.toString()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(returnItemCollectionMetrics));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(returnItemCollectionMetrics.toString()));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValuesOnConditionCheckFailure.toString()));
    }

    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;
        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        DeleteItemEnhancedRequest builtObject = DeleteItemEnhancedRequest.builder()
                                                                         .key(key)
                                                                         .conditionExpression(conditionExpression)
                                                                         .returnConsumedCapacity(returnConsumedCapacity)
                                                                         .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                         .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                         .build();

        DeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void equals_keyNotEqual() {
        Key key1 = Key.builder().partitionValue("key1").build();
        Key key2 = Key.builder().partitionValue("key2").build();

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder().key(key1).build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder().key(key2).build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_conditionExpressionNotEqual() {
        Expression conditionExpression1 = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        Expression conditionExpression2 = Expression.builder()
                                                   .expression("#key = :value AND #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .conditionExpression(conditionExpression1)
                                                                          .build();

        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .conditionExpression(conditionExpression2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnConsumedCapacityNotEqual() {
        ReturnConsumedCapacity returnConsumedCapacity1 = ReturnConsumedCapacity.TOTAL;
        ReturnConsumedCapacity returnConsumedCapacity2 = ReturnConsumedCapacity.NONE;

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .returnConsumedCapacity(returnConsumedCapacity1)
                                                                          .build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .returnConsumedCapacity(returnConsumedCapacity2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnItemCollectionMetricsNotEqual() {
        ReturnItemCollectionMetrics returnItemCollectionMetrics1 = ReturnItemCollectionMetrics.SIZE;
        ReturnItemCollectionMetrics returnItemCollectionMetrics2 = ReturnItemCollectionMetrics.NONE;

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .returnItemCollectionMetrics(returnItemCollectionMetrics1)
                                                                          .build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .returnItemCollectionMetrics(returnItemCollectionMetrics2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnValuesOnConditionCheckFailureNotEqual() {
        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure1 = ReturnValuesOnConditionCheckFailure.NONE;
        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure2 = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure1)
                                                                          .build();
        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure2)
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_minimal() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        assertThat(emptyRequest.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesKey() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        Key key = Key.builder().partitionValue("key").build();

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder().key(key).build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesConditionExpression() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .conditionExpression(conditionExpression)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnConsumedCapacity() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.TOTAL;

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .returnConsumedCapacity(returnConsumedCapacity)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnItemCollectionMetrics() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_returnValuesOnConditionCheckFailure() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();

        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        DeleteItemEnhancedRequest containsKey = DeleteItemEnhancedRequest.builder()
                                                                         .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                         .build();

        assertThat(containsKey.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void test_hashCode_includes_overrideConfiguration() {
        DeleteItemEnhancedRequest emptyRequest = DeleteItemEnhancedRequest.builder().build();
        DeleteItemEnhancedRequest requestWithOverrideConfig = DeleteItemEnhancedRequest.builder()
                                                                                       .overrideConfiguration(AwsRequestOverrideConfiguration.builder().build())
                                                                                       .build();

        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithOverrideConfig.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isSame() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                .addMetricPublisher(mockMetricPublisher)
                                                                                                                                .build())
                                                                          .build();

        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                .addMetricPublisher(mockMetricPublisher)
                                                                                                                                .build())
                                                                          .build();

        assertThat(builtObject1, equalTo(builtObject2));
        assertThat(builtObject1.hashCode(), equalTo(builtObject2.hashCode()));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isDifferent() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        DeleteItemEnhancedRequest builtObject1 = DeleteItemEnhancedRequest.builder()
                                                                          .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                .addMetricPublisher(mockMetricPublisher)
                                                                                                                                .build())
                                                                          .build();

        DeleteItemEnhancedRequest builtObject2 = DeleteItemEnhancedRequest.builder()
                                                                          .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                .build())
                                                                          .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
        assertThat(builtObject1.hashCode(), not(equalTo(builtObject2.hashCode())));
    }

    @Test
    public void builder_withOverrideConfigurationAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi"
                                                                                               ).version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder()
                                                                     .overrideConfiguration(overrideConfiguration)
                                                                     .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void builder_withoutOverrideConfiguration() {
        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder().build();

        assertThat(request.overrideConfiguration(), is(nullValue()));
    }

    @Test
    public void builder_withOverrideConfigurationConsumerAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi"
                                                                                               ).version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        DeleteItemEnhancedRequest request = DeleteItemEnhancedRequest.builder()
                                                                     .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                         "TestApi").version("1.0"))
                                                                                                  .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                     .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void toBuilder_withOverrideConfigurationAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi"
                                                                                               ).version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        DeleteItemEnhancedRequest originalRequest = DeleteItemEnhancedRequest.builder()
                                                                             .overrideConfiguration(overrideConfiguration)
                                                                             .build();

        DeleteItemEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void toBuilder_withOverrideConfigurationConsumerAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi"
                                                                                               ).version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        DeleteItemEnhancedRequest originalRequest = DeleteItemEnhancedRequest.builder()
                                                                             .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                                                              "TestApi").version("1.0"))
                                                                                                          .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                             .build();

        DeleteItemEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }
}
