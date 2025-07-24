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

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.functionaltests.models.FakeItem;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;

@RunWith(MockitoJUnitRunner.class)
public class BatchGetItemEnhancedRequestTest {

    private static final String TABLE_NAME = "table-name";

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<FakeItem> fakeItemMappedTable;


    @Before
    public void setupMappedTables() {
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(mockDynamoDbClient).build();
        fakeItemMappedTable = enhancedClient.table(TABLE_NAME, FakeItem.getTableSchema());
    }

    @Test
    public void builder_minimal() {
        BatchGetItemEnhancedRequest builtObject = BatchGetItemEnhancedRequest.builder().build();

        assertThat(builtObject.readBatches(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacity(), is(nullValue()));
        assertThat(builtObject.returnConsumedCapacityAsString(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        ReadBatch readBatch = ReadBatch.builder(FakeItem.class)
                                       .mappedTableResource(fakeItemMappedTable)
                                       .addGetItem(r -> r.key(k -> k.partitionValue("key")))
                                       .build();

        BatchGetItemEnhancedRequest builtObject = BatchGetItemEnhancedRequest.builder()
                                                                             .readBatches(readBatch)
                                                                             .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                             .build();

        assertThat(builtObject.readBatches(), is(Collections.singletonList(readBatch)));
        assertThat(builtObject.returnConsumedCapacity(), equalTo(ReturnConsumedCapacity.TOTAL));
        assertThat(builtObject.returnConsumedCapacityAsString(), equalTo(ReturnConsumedCapacity.TOTAL.toString()));
    }

    @Test
    public void builder_add_single() {
        ReadBatch readBatch = ReadBatch.builder(FakeItem.class)
                                       .mappedTableResource(fakeItemMappedTable)
                                       .addGetItem(r -> r.key(k -> k.partitionValue("key")))
                                       .build();

        BatchGetItemEnhancedRequest builtObject = BatchGetItemEnhancedRequest.builder()
                                                                             .addReadBatch(readBatch)
                                                                             .build();

        assertThat(builtObject.readBatches(), is(Collections.singletonList(readBatch)));
    }

    @Test
    public void toBuilder() {
        BatchGetItemEnhancedRequest builtObject = BatchGetItemEnhancedRequest.builder().build();

        BatchGetItemEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
        assertThat(copiedObject.hashCode(), equalTo(builtObject.hashCode()));
    }

    @Test
    public void toBuilder_maximal() {
        ReadBatch readBatch = ReadBatch.builder(FakeItem.class)
                                       .mappedTableResource(fakeItemMappedTable)
                                       .addGetItem(r -> r.key(k -> k.partitionValue("key")))
                                       .build();

        BatchGetItemEnhancedRequest builtObject = BatchGetItemEnhancedRequest.builder()
                                                                             .readBatches(readBatch)
                                                                             .returnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                                                                             .build();

        BatchGetItemEnhancedRequest copiedObject = builtObject.toBuilder().build();
        assertThat(copiedObject, is(builtObject));
        assertThat(copiedObject.hashCode(), equalTo(builtObject.hashCode()));
    }

    @Test
    public void hashCode_includes_readBatches() {
        BatchGetItemEnhancedRequest request1 = BatchGetItemEnhancedRequest.builder().build();

        ReadBatch readBatch = ReadBatch.builder(FakeItem.class)
                                       .mappedTableResource(fakeItemMappedTable)
                                       .addGetItem(r -> r.key(k -> k.partitionValue("key")))
                                       .build();
        BatchGetItemEnhancedRequest request2 = BatchGetItemEnhancedRequest.builder()
                                                                          .readBatches(readBatch)
                                                                          .build();

        assertThat(request1, not(equalTo(request2)));
        assertThat(request1.hashCode(), not(equalTo(request2)));
    }

    @Test
    public void hashCode_includes_consumedCapacity() {
        BatchGetItemEnhancedRequest request1 = BatchGetItemEnhancedRequest.builder().build();
        BatchGetItemEnhancedRequest request2 = BatchGetItemEnhancedRequest.builder()
                                                                          .returnConsumedCapacity(ReturnConsumedCapacity.INDEXES)
                                                                          .build();

        assertThat(request1, not(equalTo(request2)));
        assertThat(request1.hashCode(), not(equalTo(request2)));
    }

    @Test
    public void returnConsumedCapacity_unknownToSdkVersion() {
        BatchGetItemEnhancedRequest builtObject = BatchGetItemEnhancedRequest.builder()
                                                                             .returnConsumedCapacity("abcdefg")
                                                                             .build();
        assertThat(builtObject.returnConsumedCapacity(), is(ReturnConsumedCapacity.UNKNOWN_TO_SDK_VERSION));
        assertThat(builtObject.returnConsumedCapacityAsString(), equalTo("abcdefg"));
    }

    @Test
    public void test_hashCode_includes_overrideConfiguration() {
        BatchGetItemEnhancedRequest emptyRequest = BatchGetItemEnhancedRequest.builder().build();
        BatchGetItemEnhancedRequest requestWithOverrideConfig = BatchGetItemEnhancedRequest.builder()
                                                                                           .overrideConfiguration(AwsRequestOverrideConfiguration.builder().build())
                                                                                           .build();

        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithOverrideConfig.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isSame() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        BatchGetItemEnhancedRequest builtObject1 = BatchGetItemEnhancedRequest.builder()
                                                                              .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                    .addMetricPublisher(mockMetricPublisher)
                                                                                                                                    .build())
                                                                              .build();

        BatchGetItemEnhancedRequest builtObject2 = BatchGetItemEnhancedRequest.builder()
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
        BatchGetItemEnhancedRequest builtObject1 = BatchGetItemEnhancedRequest.builder()
                                                                              .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                    .addMetricPublisher(mockMetricPublisher)
                                                                                                                                    .build())
                                                                              .build();

        BatchGetItemEnhancedRequest builtObject2 = BatchGetItemEnhancedRequest.builder()
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

        BatchGetItemEnhancedRequest request = BatchGetItemEnhancedRequest.builder()
                                                                         .overrideConfiguration(overrideConfiguration)
                                                                         .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void builder_withoutOverrideConfiguration() {
        BatchGetItemEnhancedRequest request = BatchGetItemEnhancedRequest.builder().build();

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

        BatchGetItemEnhancedRequest request = BatchGetItemEnhancedRequest.builder()
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

        BatchGetItemEnhancedRequest originalRequest = BatchGetItemEnhancedRequest.builder()
                                                                                 .overrideConfiguration(overrideConfiguration)
                                                                                 .build();

        BatchGetItemEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

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

        BatchGetItemEnhancedRequest originalRequest = BatchGetItemEnhancedRequest.builder()
                                                                                 .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                                                                  "TestApi").version("1.0"))
                                                                                                              .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                                 .build();

        BatchGetItemEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }

}
