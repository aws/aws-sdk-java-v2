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

package software.amazon.awssdk.mapper.dynamodb.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class ApplyUserAgentInterceptorTest {

    private static final String SDK_METRICS = "sdk-metrics";

    private final ApplyUserAgentInterceptor interceptor = new ApplyUserAgentInterceptor();

    @Test
    public void modifyRequest_ddbRequest_addsMapperV2SdkMetricsApiName() {
        GetItemRequest getItemRequest = GetItemRequest.builder().build();

        SdkRequest sdkRequest = interceptor.modifyRequest(() -> getItemRequest, new ExecutionAttributes());

        List<String> metricVersions = sdkMetricsVersions(sdkRequest);
        assertThat(metricVersions, hasItem(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
        assertThat(metricVersions, hasItem("Ac"));
        assertThat(metricVersions, not(hasItem(BusinessMetricFeatureId.DDB_MAPPER.value())));
    }

    @Test
    public void modifyRequest_ddbRequestWithExistingOverrideConfig_preservesExistingConfig() {
        ApiName customApiName = ApiName.builder().name("custom").version("1.0").build();
        GetItemRequest getItemRequest =
            GetItemRequest.builder()
                          .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                                                                .addApiName(customApiName)
                                                                                .putHeader("x-test", "v")
                                                                                .build())
                          .build();

        SdkRequest sdkRequest = interceptor.modifyRequest(() -> getItemRequest, new ExecutionAttributes());

        RequestOverrideConfiguration overrideConfiguration = sdkRequest.overrideConfiguration().get();
        assertThat(overrideConfiguration.apiNames(), hasItem(customApiName));
        assertThat(overrideConfiguration.headers().get("x-test"), contains("v"));
        assertThat(sdkMetricsVersions(sdkRequest), contains(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
    }

    @Test
    public void modifyRequest_nonDdbRequest_returnsSameInstance() {
        SdkRequest someOtherRequest = new SdkRequest() {
            @Override
            public List<SdkField<?>> sdkFields() {
                return Collections.emptyList();
            }

            @Override
            public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
                return Optional.empty();
            }

            @Override
            public Builder toBuilder() {
                return null;
            }
        };

        SdkRequest sdkRequest = interceptor.modifyRequest(() -> someOtherRequest, new ExecutionAttributes());

        assertThat(sdkRequest, is(sameInstance(someOtherRequest)));
    }

    private static List<String> sdkMetricsVersions(SdkRequest request) {
        return request.overrideConfiguration()
                      .map(RequestOverrideConfiguration::apiNames)
                      .orElse(Collections.emptyList())
                      .stream()
                      .filter(a -> SDK_METRICS.equals(a.name()))
                      .map(ApiName::version)
                      .collect(Collectors.toList());
    }
}
