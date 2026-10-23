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
import java.util.stream.Collectors;
import org.junit.Test;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
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
