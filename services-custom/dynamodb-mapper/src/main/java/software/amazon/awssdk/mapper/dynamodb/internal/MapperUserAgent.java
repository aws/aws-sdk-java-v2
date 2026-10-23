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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest;

/**
 * Tags the DynamoDB requests that the mapper builds with the {@link BusinessMetricFeatureId#DYNAMODB_MAPPER_V2}
 * business metric, so the metric is only reported for requests the mapper itself sends. This is the v2 port of v1's
 * {@code DynamoDBMapper.applyUserAgent}.
 * <p>
 * v1 used separate user agent strings for batch and transaction operations. Only one feature ID is reserved for the
 * v2 mapper, so every mapper request, including batch and transaction requests, carries the same metric.
 */
@SdkInternalApi
public final class MapperUserAgent {

    private static final ApiName API_NAME = ApiName.builder()
                                                   .name("sdk-metrics")
                                                   .version(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value())
                                                   .build();

    /**
     * Shared, immutable override configuration carrying only the mapper's {@code sdk-metrics} API name. It is used
     * as-is for builders that have no override configuration, which covers every request the mapper builds itself.
     */
    private static final AwsRequestOverrideConfiguration USER_AGENT_OVERRIDE =
        AwsRequestOverrideConfiguration.builder()
                                       .addApiName(API_NAME)
                                       .build();

    private MapperUserAgent() {
    }

    /**
     * Adds the mapper's {@code sdk-metrics} API name to a request builder's override configuration, before it is
     * built.
     * <p>
     * Any override configuration already on the builder is kept, so this is safe on requests the mapper did not build
     * (for example, a caller-built request passed to a public paginated list). The cases are:
     * <ul>
     *     <li>No override configuration: the shared {@code USER_AGENT_OVERRIDE} is set, so the common path copies
     *     nothing.</li>
     *     <li>The override configuration already has the API name: the builder is left unchanged. This keeps requests
     *     derived page by page from accumulating one API name per page.</li>
     *     <li>Otherwise: the API name is added to a copy of the existing override configuration.</li>
     * </ul>
     * Requests later derived from the built request with {@code toBuilder()} keep the tag.
     *
     * @param builder the request builder to tag
     * @param <T> the request builder type
     * @return the same builder, for method chaining
     */
    public static <T extends DynamoDbRequest.Builder> T applyUserAgent(T builder) {
        AwsRequestOverrideConfiguration existing = builder.overrideConfiguration();
        if (existing == null) {
            builder.overrideConfiguration(USER_AGENT_OVERRIDE);
        } else if (!existing.apiNames().contains(API_NAME)) {
            builder.overrideConfiguration(existing.toBuilder().addApiName(API_NAME).build());
        }
        return builder;
    }
}
