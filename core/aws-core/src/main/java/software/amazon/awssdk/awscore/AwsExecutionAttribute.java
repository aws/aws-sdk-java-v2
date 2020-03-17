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

package software.amazon.awssdk.awscore;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.regions.Region;

/**
 * AWS-specific attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s.
 */
@SdkPublicApi
public final class AwsExecutionAttribute extends SdkExecutionAttribute {
    /**
     * The AWS {@link Region} the client was configured with. This is not always same as the
     * {@link AwsSignerExecutionAttribute#SIGNING_REGION} for global services like IAM.
     */
    public static final ExecutionAttribute<Region> AWS_REGION = new ExecutionAttribute<>("AwsRegion");

    /**
     * The {@link AwsClientOption#ENDPOINT_PREFIX} for the client.
     */
    public static final ExecutionAttribute<String> ENDPOINT_PREFIX = new ExecutionAttribute<>("AwsEndpointPrefix");

    private AwsExecutionAttribute() {
    }
}
