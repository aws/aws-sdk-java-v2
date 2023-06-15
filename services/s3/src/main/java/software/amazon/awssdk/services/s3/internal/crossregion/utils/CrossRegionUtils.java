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

package software.amazon.awssdk.services.s3.internal.crossregion.utils;


import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class CrossRegionUtils {
    private static final ApiName API_NAME = ApiName.builder().version("cross-region").name("hll").build();
    private static final Consumer<AwsRequestOverrideConfiguration.Builder> USER_AGENT_APPLIER = b -> b.addApiName(API_NAME);

    private CrossRegionUtils() {
    }

    public static <T extends S3Request> AwsRequestOverrideConfiguration updateUserAgentInConfig(T request) {
        AwsRequestOverrideConfiguration overrideConfiguration =
            request.overrideConfiguration().map(c -> c.toBuilder()
                                                      .applyMutation(USER_AGENT_APPLIER)
                                                      .build())
                   .orElse(AwsRequestOverrideConfiguration.builder()
                                                          .applyMutation(USER_AGENT_APPLIER)
                                                          .build());
        return overrideConfiguration;
    }
}
