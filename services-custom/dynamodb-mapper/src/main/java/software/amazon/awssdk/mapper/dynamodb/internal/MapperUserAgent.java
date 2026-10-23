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

    /**
     * Shared, immutable override configuration carrying the mapper's {@code sdk-metrics} API name.
     * <p>
     * {@link #applyUserAgent} sets this as the request's whole override configuration, which <em>replaces</em> any
     * override configuration already on the builder. That is safe only because the mapper sets no other request-level
     * override configuration. If the mapper ever needs another override, it must merge the two instead.
     */
    private static final AwsRequestOverrideConfiguration USER_AGENT_OVERRIDE =
        AwsRequestOverrideConfiguration.builder()
                                       .addApiName(ApiName.builder()
                                                          .name("sdk-metrics")
                                                          .version(BusinessMetricFeatureId.DYNAMODB_MAPPER_V2.value())
                                                          .build())
                                       .build();

    private MapperUserAgent() {
    }

    /**
     * Sets the mapper's user agent override configuration on a request builder, before it is built.
     * <p>
     * This <em>replaces</em> any override configuration already on the builder. That is safe only because the mapper
     * sets no other request-level override configuration; if it ever needs one, it must merge the two instead.
     * Requests later derived from the built request with {@code toBuilder()} keep the tag.
     *
     * @param builder the request builder to tag
     * @param <T> the request builder type
     * @return the same builder, for method chaining
     */
    public static <T extends DynamoDbRequest.Builder> T applyUserAgent(T builder) {
        builder.overrideConfiguration(USER_AGENT_OVERRIDE);
        return builder;
    }
}
