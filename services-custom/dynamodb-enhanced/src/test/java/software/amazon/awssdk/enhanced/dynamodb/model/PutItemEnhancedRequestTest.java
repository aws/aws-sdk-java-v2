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
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.services.dynamodb.model.ReturnItemCollectionMetrics;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@RunWith(MockitoJUnitRunner.class)
public class PutItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(builtObject.item(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnValues(), is(nullValue()));
        assertThat(builtObject.returnValuesAsString(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetrics(), is(nullValue()));
        assertThat(builtObject.returnItemCollectionMetricsAsString(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(nullValue()));
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

        ReturnValue returnValues = ReturnValue.ALL_OLD;
        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.INDEXES;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;
        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .item(fakeItem)
                                                                             .conditionExpression(conditionExpression)
                                                                             .returnValues(returnValues)
                                                                             .returnConsumedCapacity(returnConsumedCapacity)
                                                                             .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                             .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                             .build();

        assertThat(builtObject.item(), is(fakeItem));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnValues(), is(returnValues));
        assertThat(builtObject.returnValuesAsString(), is(returnValues.toString()));
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

        ReturnValue returnValues = ReturnValue.ALL_OLD;
        ReturnConsumedCapacity returnConsumedCapacity = ReturnConsumedCapacity.INDEXES;
        ReturnItemCollectionMetrics returnItemCollectionMetrics = ReturnItemCollectionMetrics.SIZE;
        ReturnValuesOnConditionCheckFailure returnValuesOnConditionCheckFailure = ReturnValuesOnConditionCheckFailure.ALL_OLD;

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .item(fakeItem)
                                                                             .conditionExpression(conditionExpression)
                                                                             .returnValues(returnValues)
                                                                             .returnConsumedCapacity(returnConsumedCapacity)
                                                                             .returnItemCollectionMetrics(returnItemCollectionMetrics)
                                                                             .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                             .build();

        PutItemEnhancedRequest<FakeItem> copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void equals_itemNotEqual() {
        FakeItem item1 = createUniqueFakeItem();
        FakeItem item2 = createUniqueFakeItem();

        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .item(item1)
                                                                              .build();
        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .item(item2)
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

        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .conditionExpression(conditionExpression1)
                                                                              .build();
        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .conditionExpression(conditionExpression2)
                                                                              .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnValuesNotEqual() {
        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnValues("return1")
                                                                              .build();

        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnValues("return2")
                                                                              .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnConsumedCapacityNotEqual() {
        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnConsumedCapacity("return1")
                                                                              .build();

        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnConsumedCapacity("return2")
                                                                              .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnItemCollectionMetricsNotEqual() {
        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnItemCollectionMetrics("return1")
                                                                              .build();

        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnItemCollectionMetrics("return2")
                                                                              .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equals_returnValuesOnConditionCheckFailureNotEqual() {
        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnValuesOnConditionCheckFailure("return1")
                                                                              .build();

        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnValuesOnConditionCheckFailure("return2")
                                                                              .build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_minimal() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        assertThat(emptyRequest.hashCode(), equalTo(0));
    }

    @Test
    public void hashCode_includesItem() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();
        PutItemEnhancedRequest<FakeItem> containsItem = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .item(createUniqueFakeItem())
                                                                              .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesConditionExpression() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        PutItemEnhancedRequest<FakeItem> containsExpression = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                    .conditionExpression(conditionExpression)
                                                                                    .build();

        assertThat(containsExpression.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnValues() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        PutItemEnhancedRequest<FakeItem> containsReturnValues = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                      .returnValues("return")
                                                                                      .build();

        assertThat(containsReturnValues.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnConsumedCapacity() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        PutItemEnhancedRequest<FakeItem> containsReturnConsumedCapacity = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                .returnConsumedCapacity("return")
                                                                                                .build();

        assertThat(containsReturnConsumedCapacity.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_includesReturnItemCollectionMetrics() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        PutItemEnhancedRequest<FakeItem> cotnainsReturnItemCollectionMetrics = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                                     .returnItemCollectionMetrics("return")
                                                                                                     .build();

        assertThat(cotnainsReturnItemCollectionMetrics.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void hashCode_returnValuesOnConditionCheckFailure() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();

        PutItemEnhancedRequest<FakeItem> containsItem = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .returnValuesOnConditionCheckFailure("return1")
                                                                              .build();

        assertThat(containsItem.hashCode(), not(equalTo(emptyRequest.hashCode())));
    }

    @Test
    public void builder_returnValueEnumSetter_paramNull_NoNpe() {
        PutItemEnhancedRequest.builder(FakeItem.class).returnValues((ReturnValue) null).build();
    }

    @Test
    public void returnValues_newValue_returnsUnknownToSdkVersion() {
        String newReturnValue = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnValues(newReturnValue)
                                                                             .build();

        assertThat(builtObject.returnValues(), equalTo(ReturnValue.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void returnValues_newValue_stringGetter_returnsValue() {
        String newReturnValue = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnValues(newReturnValue)
                                                                             .build();

        assertThat(builtObject.returnValuesAsString(), equalTo(newReturnValue));
    }

    @Test
    public void builder_returnConsumedCapacityEnumSetter_paramNull_NoNpe() {
        PutItemEnhancedRequest.builder(FakeItem.class).returnConsumedCapacity((ReturnConsumedCapacity) null).build();
    }

    @Test
    public void returnConsumedCapacity_newValue_returnsUnknownToSdkVersion() {
        String newReturnCapacity = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnConsumedCapacity(newReturnCapacity)
                                                                             .build();

        assertThat(builtObject.returnConsumedCapacity(), equalTo(ReturnConsumedCapacity.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void returnConsumedCapacity_newValue_stringGetter_returnsValue() {
        String newReturnCapacity = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnConsumedCapacity(newReturnCapacity)
                                                                             .build();

        assertThat(builtObject.returnConsumedCapacityAsString(), equalTo(newReturnCapacity));
    }

    @Test
    public void builder_returnItemCollectionMetricsEnumSetter_paramNull_NoNpe() {
        PutItemEnhancedRequest.builder(FakeItem.class).returnItemCollectionMetrics((ReturnItemCollectionMetrics) null).build();
    }

    @Test
    public void returnItemCollectionMetrics_newValue_returnsUnknownToSdkVersion() {
        String newReturnItemCollectionMetrics = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnItemCollectionMetrics(newReturnItemCollectionMetrics)
                                                                             .build();

        assertThat(builtObject.returnItemCollectionMetrics(), equalTo(ReturnItemCollectionMetrics.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void returnItemCollectionMetrics_newValue_stringGetter_returnsValue() {
        String newReturnItemCollectionMetrics = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnItemCollectionMetrics(newReturnItemCollectionMetrics)
                                                                             .build();

        assertThat(builtObject.returnItemCollectionMetricsAsString(), equalTo(newReturnItemCollectionMetrics));
    }


    @Test
    public void returnValuesOnConditionCheckFailure_newValue_returnsUnknownToSdkVersion() {
        String returnValuesOnConditionCheckFailure = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                             .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailure(), equalTo(ReturnValuesOnConditionCheckFailure.UNKNOWN_TO_SDK_VERSION));
    }

    @Test
    public void returnValuesOnConditionCheckFailure_newValue_stringGetter_returnsValue() {
        String returnValuesOnConditionCheckFailure = UUID.randomUUID().toString();

        PutItemEnhancedRequest<FakeItem> builtObject = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                             .returnValuesOnConditionCheckFailure(returnValuesOnConditionCheckFailure)
                                                                             .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), equalTo(returnValuesOnConditionCheckFailure));
    }

    @Test
    public void test_hashCode_includes_overrideConfiguration() {
        PutItemEnhancedRequest<FakeItem> emptyRequest = PutItemEnhancedRequest.builder(FakeItem.class).build();
        PutItemEnhancedRequest<FakeItem> requestWithOverrideConfig = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                           .overrideConfiguration(AwsRequestOverrideConfiguration.builder().build())
                                                                                           .build();

        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithOverrideConfig.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isSame() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                    .addMetricPublisher(mockMetricPublisher)
                                                                                                                                    .build())
                                                                              .build();

        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
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
        PutItemEnhancedRequest<FakeItem> builtObject1 = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                              .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                    .addMetricPublisher(mockMetricPublisher)
                                                                                                                                    .build())
                                                                              .build();

        PutItemEnhancedRequest<FakeItem> builtObject2 = PutItemEnhancedRequest.builder(FakeItem.class)
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

        PutItemEnhancedRequest<FakeItem> request = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                         .overrideConfiguration(overrideConfiguration)
                                                                         .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void builder_withoutOverrideConfiguration() {
        PutItemEnhancedRequest<FakeItem> request = PutItemEnhancedRequest.builder(FakeItem.class).build();

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

        PutItemEnhancedRequest<FakeItem> request = PutItemEnhancedRequest.builder(FakeItem.class)
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

        PutItemEnhancedRequest<FakeItem> originalRequest = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                 .overrideConfiguration(overrideConfiguration)
                                                                                 .build();

        PutItemEnhancedRequest<FakeItem> copiedRequest = originalRequest.toBuilder().build();

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

        PutItemEnhancedRequest<FakeItem> originalRequest = PutItemEnhancedRequest.builder(FakeItem.class)
                                                                                 .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                                                                  "TestApi").version("1.0"))
                                                                                                              .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                                 .build();

        PutItemEnhancedRequest<FakeItem> copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }
}
