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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

public class MapperUserAgentTest {

    private static final String SDK_METRICS = "sdk-metrics";

    @Test
    public void applyUserAgent_requestBuilder_addsOnlyMapperV2SdkMetricsApiName() {
        GetItemRequest request = MapperUserAgent.applyUserAgent(GetItemRequest.builder()).build();

        List<String> metricVersions = sdkMetricsVersions(request);
        assertThat(metricVersions, contains("Ac"));
        assertThat(metricVersions, contains(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
        assertThat(metricVersions, not(hasItem(BusinessMetricFeatureId.DDB_MAPPER.value())));
    }

    @Test
    public void applyUserAgent_requestBuilder_returnsSameBuilder() {
        GetItemRequest.Builder builder = GetItemRequest.builder();

        assertThat(MapperUserAgent.applyUserAgent(builder), is(sameInstance(builder)));
    }

    @Test
    public void applyUserAgent_requestRebuiltWithToBuilder_keepsTag() {
        ScanRequest tagged = MapperUserAgent.applyUserAgent(ScanRequest.builder()).tableName("t").build();

        ScanRequest nextPage = tagged.toBuilder().segment(1).build();

        assertThat(sdkMetricsVersions(nextPage), contains(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
    }

    @Test
    public void applyUserAgent_builderWithCallerOverride_keepsCallerOverrideAndAddsTag() {
        ApiName callerApiName = ApiName.builder().name("caller-lib").version("1.0").build();
        Duration timeout = Duration.ofSeconds(42);
        QueryRequest.Builder builder = QueryRequest.builder()
                                                   .overrideConfiguration(o -> o.apiCallTimeout(timeout)
                                                                                .addApiName(callerApiName)
                                                                                .putHeader("x-caller", "kept"));

        QueryRequest request = MapperUserAgent.applyUserAgent(builder).build();

        AwsRequestOverrideConfiguration override = request.overrideConfiguration().get();
        assertThat(override.apiCallTimeout(), is(Optional.of(timeout)));
        assertThat(override.headers().get("x-caller"), contains("kept"));
        assertThat(override.apiNames(), hasItem(callerApiName));
        assertThat(sdkMetricsVersions(request), contains(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
    }

    @Test
    public void applyUserAgent_appliedOnEveryDerivedPage_tagAppearsOnce() {
        ApiName callerApiName = ApiName.builder().name("caller-lib").version("1.0").build();
        ScanRequest page = ScanRequest.builder().overrideConfiguration(o -> o.addApiName(callerApiName)).build();

        for (int i = 0; i < 3; i++) {
            page = MapperUserAgent.applyUserAgent(page.toBuilder()).segment(i).build();
        }

        assertThat(sdkMetricsVersions(page), contains(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value()));
        assertThat(page.overrideConfiguration().get().apiNames(), hasItem(callerApiName));
    }

    @Test
    public void applyUserAgent_alreadyTaggedBuilder_leavesOverrideUnchanged() {
        GetItemRequest tagged = MapperUserAgent.applyUserAgent(GetItemRequest.builder()).build();
        AwsRequestOverrideConfiguration before = tagged.overrideConfiguration().get();

        GetItemRequest.Builder builder = MapperUserAgent.applyUserAgent(tagged.toBuilder());

        assertThat(builder.overrideConfiguration(), is(sameInstance(before)));
    }

    private static List<String> sdkMetricsVersions(DynamoDbRequest request) {
        return request.overrideConfiguration()
                      .map(RequestOverrideConfiguration::apiNames)
                      .orElse(Collections.emptyList())
                      .stream()
                      .filter(a -> SDK_METRICS.equals(a.name()))
                      .map(ApiName::version)
                      .collect(Collectors.toList());
    }
}
