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
import static software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem.createUniqueFakeItem;
import static software.amazon.awssdk.enhanced.dynamodb.internal.AttributeValues.stringValue;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@RunWith(MockitoJUnitRunner.class)
public class UpdateItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject.item(), is(nullValue()));
        assertThat(builtObject.ignoreNulls(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        FakeItem fakeItem = createUniqueFakeItem();

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

        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                   .item(fakeItem)
                                                                                   .ignoreNulls(true)
                                                                                   .conditionExpression(conditionExpression)
                                                                                   .returnConsumedCapacity(returnConsumedCapacity)
                                                                                   .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                                   .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                                   .build();

        assertThat(builtObject.item(), is(fakeItem));
        assertThat(builtObject.ignoreNulls(), is(true));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnConsumedCapacity(), is(returnConsumedCapacity));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(returnConsumedCapacity.toString()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(returnItemCollectionMetrics));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(returnItemCollectionMetrics.toString()));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValuesOnConditionCheckFailure.toString()));
    }

    @Test
    public void toBuilder() {
        FakeItem fakeItem = createUniqueFakeItem();

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

        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                   .item(fakeItem)
                                                                                   .ignoreNulls(true)
                                                                                   .conditionExpression(conditionExpression)
                                                                                   .returnConsumedCapacity(returnConsumedCapacity)
                                                                                   .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                                   .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                                   .build();

        UpdateItemEnhancedRequest<FakeItem> copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void equals_self() {
        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject, equalTo(builtObject));
    }

    @Test
    public void equals_differentType() {
        UpdateItemEnhancedRequest<FakeItem> builtObject = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject, not(equalTo(new Object())));
    }

    @Test
    public void equals_itemNotEqual() {
        FakeItem fakeItem1 = createUniqueFakeItem();
        FakeItem fakeItem2 = createUniqueFakeItem();

        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .item(fakeItem1)
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .item(fakeItem2)
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_ignoreNullsNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .ignoreNulls(true)
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .ignoreNulls(false)
                                                                                    .build();

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
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .conditionExpression(conditionExpression1)
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .conditionExpression(conditionExpression2)
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnConsumedCapacityNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnConsumedCapacity("return1")
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnConsumedCapacity("return2")
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnItemCollectionMetricsNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnItemCollectionMetrics("return1")
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnItemCollectionMetrics("return2")
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnValuesOnConditionCheckFailureNotEqual() {
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnValuesOnConditionCheckFailure("return1")
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnValuesOnConditionCheckFailure("return2")
                                                                                    .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_minimal() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(emptyRequest.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesItem() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .item(createUniqueFakeItem())
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesIgnoreNulls() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .ignoreNulls(true)
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesConditionExpression() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        UpdateItemEnhancedRequest<FakeItem> containsExpression = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                          .conditionExpression(conditionExpression)
                                                                                          .build();

        assertThat(containsExpression.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnConsumedCapacity() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnConsumedCapacity("return1")
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnItemCollectionMetrics() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnItemCollectionMetrics("return1")
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_returnValuesOnConditionCheckFailure() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

        UpdateItemEnhancedRequest<FakeItem> containsItem = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .returnValuesOnConditionCheckFailure("return1")
                                                                                    .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void test_hashCode_includes_overrideConfiguration() {
        UpdateItemEnhancedRequest<FakeItem> emptyRequest = UpdateItemEnhancedRequest.builder(FakeItem.class).build();
        UpdateItemEnhancedRequest<FakeItem> requestWithOverrideConfig = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                                 .overrideConfiguration(AwsRequestOverrideConfiguration.builder().build())
                                                                                                 .build();

        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithOverrideConfig.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isSame() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                          .addMetricPublisher(mockMetricPublisher)
                                                                                                                                          .build())
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
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
        UpdateItemEnhancedRequest<FakeItem> builtObject1 = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                          .addMetricPublisher(mockMetricPublisher)
                                                                                                                                          .build())
                                                                                    .build();

        UpdateItemEnhancedRequest<FakeItem> builtObject2 = UpdateItemEnhancedRequest.builder(FakeItem.class)
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

        UpdateItemEnhancedRequest<FakeItem> request = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                               .overrideConfiguration(overrideConfiguration)
                                                                               .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void builder_withoutOverrideConfiguration() {
        UpdateItemEnhancedRequest<FakeItem> request = UpdateItemEnhancedRequest.builder(FakeItem.class).build();

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

        UpdateItemEnhancedRequest<FakeItem> request = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                               .overrideConfiguration(b -> b.addApiName(api -> api.name("TestApi").version("1.0"))
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

        UpdateItemEnhancedRequest<FakeItem> originalRequest = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                       .overrideConfiguration(overrideConfiguration)
                                                                                       .build();

        UpdateItemEnhancedRequest<FakeItem> copiedRequest = originalRequest.toBuilder().build();

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

        UpdateItemEnhancedRequest<FakeItem> originalRequest = UpdateItemEnhancedRequest.builder(FakeItem.class)
                                                                                       .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                                                                        "TestApi").version("1.0"))
                                                                                                                    .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                                       .build();

        UpdateItemEnhancedRequest<FakeItem> copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }
}
