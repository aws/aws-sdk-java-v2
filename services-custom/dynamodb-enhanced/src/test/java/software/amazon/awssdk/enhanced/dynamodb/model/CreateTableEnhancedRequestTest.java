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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

@RunWith(MockitoJUnitRunner.class)
public class CreateTableEnhancedRequestTest {

    @Test
    public void builder_minimal() {
        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder().build();

        assertThat(builtObject.globalSecondaryIndices(), is(nullValue()));
        assertThat(builtObject.localSecondaryIndices(), is(nullValue()));
        assertThat(builtObject.provisionedThroughput(), is(nullValue()));
    }

    @Test
    public void builder_maximal() {
        EnhancedGlobalSecondaryIndex globalSecondaryIndex =
                EnhancedGlobalSecondaryIndex.builder()
                        .indexName("gsi_1")
                        .projection(p -> p.projectionType(ProjectionType.ALL))
                        .provisionedThroughput(getDefaultProvisionedThroughput())
                        .build();

        EnhancedLocalSecondaryIndex localSecondaryIndex = EnhancedLocalSecondaryIndex.create(
            "lsi", Projection.builder().projectionType(ProjectionType.ALL).build());

        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder()
                                                                           .globalSecondaryIndices(globalSecondaryIndex)
                                                                           .localSecondaryIndices(localSecondaryIndex)
                                                                           .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                           .build();

        assertThat(builtObject.globalSecondaryIndices(), is(Collections.singletonList(globalSecondaryIndex)));
        assertThat(builtObject.localSecondaryIndices(), is(Collections.singletonList(localSecondaryIndex)));
        assertThat(builtObject.provisionedThroughput(), is(getDefaultProvisionedThroughput()));
    }

    @Test
    public void builder_consumerBuilder() {
        CreateTableEnhancedRequest builtObject =
            CreateTableEnhancedRequest.builder()
                                      .globalSecondaryIndices(gsi -> gsi.indexName("x"),
                                                              gsi -> gsi.indexName("y"))
                                      .localSecondaryIndices(lsi -> lsi.indexName("x"),
                                                             lsi -> lsi.indexName("y"))
                                      .provisionedThroughput(p -> p.readCapacityUnits(10L))
                                      .build();

        assertThat(builtObject.globalSecondaryIndices(),
                   equalTo(asList(EnhancedGlobalSecondaryIndex.builder().indexName("x").build(),
                                  EnhancedGlobalSecondaryIndex.builder().indexName("y").build())));
        assertThat(builtObject.localSecondaryIndices(),
                   equalTo(asList(EnhancedLocalSecondaryIndex.builder().indexName("x").build(),
                                  EnhancedLocalSecondaryIndex.builder().indexName("y").build())));
        assertThat(builtObject.provisionedThroughput(),
                   equalTo(ProvisionedThroughput.builder().readCapacityUnits(10L).build()));
    }

    @Test
    public void test_hashCode_includes_overrideConfiguration() {
        CreateTableEnhancedRequest emptyRequest = CreateTableEnhancedRequest.builder().build();
        CreateTableEnhancedRequest requestWithOverrideConfig = CreateTableEnhancedRequest.builder()
                                                                                         .overrideConfiguration(AwsRequestOverrideConfiguration.builder().build())
                                                                                         .build();

        assertThat(emptyRequest.hashCode(), not(equalTo(requestWithOverrideConfig.hashCode())));
    }

    @Test
    public void test_equalsAndHashCode_when_overrideConfiguration_isSame() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        CreateTableEnhancedRequest builtObject1 = CreateTableEnhancedRequest.builder()
                                                                            .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                  .addMetricPublisher(mockMetricPublisher)
                                                                                                                                  .build())
                                                                            .build();

        CreateTableEnhancedRequest builtObject2 = CreateTableEnhancedRequest.builder()
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
        CreateTableEnhancedRequest builtObject1 = CreateTableEnhancedRequest.builder()
                                                                            .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                                                                  .addMetricPublisher(mockMetricPublisher)
                                                                                                                                  .build())
                                                                            .build();

        CreateTableEnhancedRequest builtObject2 = CreateTableEnhancedRequest.builder()
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

        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                       .overrideConfiguration(overrideConfiguration)
                                                                       .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void builder_withoutOverrideConfiguration() {
        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder().build();

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

        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                       .overrideConfiguration(b -> b.addApiName(api -> api.name("TestApi").version("1.0"))
                                                                                                    .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                       .build();

        assertThat(request.overrideConfiguration(), is(overrideConfiguration));
    }

    @Test
    public void toBuilder() {
        CreateTableEnhancedRequest builtObject = CreateTableEnhancedRequest.builder()
                                                                           .provisionedThroughput(getDefaultProvisionedThroughput())
                                                                           .build();

        CreateTableEnhancedRequest copiedObject = builtObject.toBuilder().build();

        assertThat(copiedObject, is(builtObject));
    }

    @Test
    public void toBuilder_withOverrideConfigurationAndMetricPublisher() {
        MetricPublisher mockMetricPublisher = mock(MetricPublisher.class);
        AwsRequestOverrideConfiguration overrideConfiguration = AwsRequestOverrideConfiguration.builder()
                                                                                               .addApiName(b -> b.name("TestApi"
                                                                                               ).version("1.0"))
                                                                                               .addMetricPublisher(mockMetricPublisher)
                                                                                               .build();

        CreateTableEnhancedRequest originalRequest = CreateTableEnhancedRequest.builder()
                                                                               .overrideConfiguration(overrideConfiguration)
                                                                               .build();

        CreateTableEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

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

        CreateTableEnhancedRequest originalRequest = CreateTableEnhancedRequest.builder()
                                                                               .overrideConfiguration(b -> b.addApiName(api -> api.name(
                                                                                                                "TestApi").version("1.0"))
                                                                                                            .metricPublishers(Collections.singletonList(mockMetricPublisher)))
                                                                               .build();

        CreateTableEnhancedRequest copiedRequest = originalRequest.toBuilder().build();

        assertThat(copiedRequest.overrideConfiguration(), is(overrideConfiguration));
    }

    private ProvisionedThroughput getDefaultProvisionedThroughput() {
        return ProvisionedThroughput.builder()
                                    .writeCapacityUnits(1L)
                                    .readCapacityUnits(2L)
                                    .build();
    }

}
