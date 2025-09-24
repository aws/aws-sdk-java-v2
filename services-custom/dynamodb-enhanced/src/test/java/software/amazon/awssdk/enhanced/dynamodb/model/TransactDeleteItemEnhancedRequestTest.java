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
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

@RunWith(MockitoJUnitRunner.class)
public class TransactDeleteItemEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject.key(), is(nullValue()));
        assertThat(builtObject.conditionExpression(), is(nullValue()));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
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

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .key(key)
                                                                                         .conditionExpression(conditionExpression)
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        assertThat(builtObject.key(), is(key));
        assertThat(builtObject.conditionExpression(), is(conditionExpression));
        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(returnValues));
    }

    @Test
    public void equals_maximal() {
        Key key = Key.builder().partitionValue("key").build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .key(key)
                                                                                         .conditionExpression(conditionExpression)
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        TransactDeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(builtObject, equalTo(copiedObject));
    }

    @Test
    public void equals_minimal() {
        TransactDeleteItemEnhancedRequest builtObject1 = TransactDeleteItemEnhancedRequest.builder().build();
        TransactDeleteItemEnhancedRequest builtObject2 = TransactDeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject1, equalTo(builtObject2));
    }

    @Test
    public void equals_differentType() {
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder().build();
        DeleteItemEnhancedRequest differentType = DeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject, not(equalTo(differentType)));
    }

    @Test
    public void equals_self() {
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder().build();
        assertThat(builtObject, equalTo(builtObject));
    }

    @Test
    public void equals_keyNotEqual() {
        Key key1 = Key.builder().partitionValue("key1").build();
        Key key2 = Key.builder().partitionValue("key2").build();

        TransactDeleteItemEnhancedRequest builtObject1 = TransactDeleteItemEnhancedRequest.builder().key(key1).build();
        TransactDeleteItemEnhancedRequest builtObject2 = TransactDeleteItemEnhancedRequest.builder().key(key2).build();

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

        TransactDeleteItemEnhancedRequest builtObject1 =
            TransactDeleteItemEnhancedRequest.builder().conditionExpression(conditionExpression1).build();
        TransactDeleteItemEnhancedRequest builtObject2 =
            TransactDeleteItemEnhancedRequest.builder().conditionExpression(conditionExpression2).build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void equal_returnValuesNotEqual() {
        ReturnValuesOnConditionCheckFailure returnValues1 = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        ReturnValuesOnConditionCheckFailure returnValues2 = ReturnValuesOnConditionCheckFailure.NONE;

        TransactDeleteItemEnhancedRequest builtObject1 =
            TransactDeleteItemEnhancedRequest.builder().returnValuesOnConditionCheckFailure(returnValues1).build();
        TransactDeleteItemEnhancedRequest builtObject2 =
            TransactDeleteItemEnhancedRequest.builder().returnValuesOnConditionCheckFailure(returnValues2).build();

        assertThat(builtObject1, not(equalTo(builtObject2)));
    }

    @Test
    public void hashCode_maximal() {
        Key key = Key.builder().partitionValue("key").build();

        Expression conditionExpression = Expression.builder()
                                                   .expression("#key = :value OR #key1 = :value1")
                                                   .putExpressionName("#key", "attribute")
                                                   .putExpressionName("#key1", "attribute3")
                                                   .putExpressionValue(":value", stringValue("wrong"))
                                                   .putExpressionValue(":value1", stringValue("three"))
                                                   .build();

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .key(key)
                                                                                         .conditionExpression(conditionExpression)
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        TransactDeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(builtObject.hashCode(), equalTo(copiedObject.hashCode()));
    }

    @Test
    public void hashCode_minimal() {
        TransactDeleteItemEnhancedRequest builtObject1 = TransactDeleteItemEnhancedRequest.builder().build();
        TransactDeleteItemEnhancedRequest builtObject2 = TransactDeleteItemEnhancedRequest.builder().build();

        assertThat(builtObject1.hashCode(), equalTo(builtObject2.hashCode()));
    }

    @Test
    public void toBuilder() {
        Key key = Key.builder().partitionValue("key").build();

        ReturnValuesOnConditionCheckFailure returnValues = ReturnValuesOnConditionCheckFailure.ALL_OLD;
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .key(key)
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        TransactDeleteItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureSetterNull_noNpe() {
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .returnValuesOnConditionCheckFailure((ReturnValuesOnConditionCheckFailure) null)
                                                                                         .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailure(), is(nullValue()));
    }

    @Test
    public void builder_returnValuesOnConditionCheckFailureSetterString() {
        String returnValues = "new-value";
        TransactDeleteItemEnhancedRequest builtObject = TransactDeleteItemEnhancedRequest.builder()
                                                                                         .returnValuesOnConditionCheckFailure(returnValues)
                                                                                         .build();

        assertThat(builtObject.returnValuesOnConditionCheckFailureAsString(), is(returnValues));
    }

    @Test
    public void test_hashCode_includes_overrideConfiguration() {
        TransactDeleteItemEnhancedRequest emptyRequest = TransactDeleteItemEnhancedRequest.builder().build();
        TransactDeleteItemEnhancedRequest requestWithOverrideConfig = TransactDeleteItemEnhancedRequest.builder()
                                                                             .overrideConfiguration(AwsRequestOverrideConfiguration.builder().build())
                                                                             .build();

        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithOverrideConfig.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isSame() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        TransactDeleteItemEnhancedRequest builtObject1 = TransactDeleteItemEnhancedRequest.builder()
                                                                .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                      .addMetricPublisher(mockMetricPublisher)
                                                                                                                      .build())
                                                                .build();

        TransactDeleteItemEnhancedRequest builtObject2 = TransactDeleteItemEnhancedRequest.builder()
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
        TransactDeleteItemEnhancedRequest builtObject1 = TransactDeleteItemEnhancedRequest.builder()
                                                                                          .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                                .addMetricPublisher(mockMetricPublisher)
                                                                                                                                                .build())
                                                                                          .build();

        TransactDeleteItemEnhancedRequest builtObject2 = TransactDeleteItemEnhancedRequest.builder()
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
                                                                                               .addApiName(b -> b.name("TestApi").version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        TransactDeleteItemEnhancedRequest request = TransactDeleteItemEnhancedRequest.builder()
                                                           .overrideConfiguration(overrideConfiguration)
                                                           .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void builder_withoutOverrideConfiguration() {
        TransactDeleteItemEnhancedRequest request = TransactDeleteItemEnhancedRequest.builder().build();

        assertThat(request.overrideConfiguration(), is(nullValue()));
    }

    @Test
    public void builder_withOverrideConfigurationConsumerAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi").version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        TransactDeleteItemEnhancedRequest request = TransactDeleteItemEnhancedRequest.builder()
                                                           .overrideConfiguration(b -> b.addApiName(api -> api.name("TestApi").version("1.0"))
                                                                                        .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                           .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void toBuilder_withOverrideConfigurationAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi").version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        TransactDeleteItemEnhancedRequest originalRequest = TransactDeleteItemEnhancedRequest.builder()
                                                                   .overrideConfiguration(overrideConfiguration)
                                                                   .build();

        TransactDeleteItemEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void toBuilder_withOverrideConfigurationConsumerAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi").version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        TransactDeleteItemEnhancedRequest originalRequest = TransactDeleteItemEnhancedRequest.builder()
                                                                   .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                                                    "TestApi").version("1.0"))
                                                                                                .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                   .build();

        TransactDeleteItemEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }

}
