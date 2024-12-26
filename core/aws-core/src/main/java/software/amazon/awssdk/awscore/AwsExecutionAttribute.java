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

import java.util.Set;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.endpoints.AccountIdEndpointMode;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.regions.Region;

/**
 * AWS-specific attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s.
 */
@SdkPublicApi
public final class  AwsExecutionAttribute extends SdkExecutionAttribute {
    /**
     * The AWS {@link Region} the client was configured with. This is not always same as the
     * {@link AwsSignerExecutionAttribute#SIGNING_REGION} for global services like IAM.
     */
    public static final ExecutionAttribute<Region> AWS_REGION = new ExecutionAttribute<>("AwsRegion");

    /**
     * The {@link AwsClientOption#ENDPOINT_PREFIX} for the client.
     */
    public static final ExecutionAttribute<String> ENDPOINT_PREFIX = new ExecutionAttribute<>("AwsEndpointPrefix");

    /**
     * Whether dualstack endpoints were enabled for this request.
     */
    public static final ExecutionAttribute<Boolean> DUALSTACK_ENDPOINT_ENABLED =
        new ExecutionAttribute<>("DualstackEndpointsEnabled");


    /**
     * AWS Sigv4a  signing region set. This is used to compute the signing region for Sigv4a requests.
     */
    public static final ExecutionAttribute<Set<String>> AWS_SIGV4A_SIGNING_REGION_SET =
        new ExecutionAttribute<>("AwsSigv4aSigningRegionSet");

    /**
     * Whether fips endpoints were enabled for this request.
     */
    public static final ExecutionAttribute<Boolean> FIPS_ENDPOINT_ENABLED =
        new ExecutionAttribute<>("FipsEndpointsEnabled");

    /**
     * Whether the client was configured to use the service's global endpoint. This is used as part of endpoint computation by
     * the endpoint providers.
     */
    public static final ExecutionAttribute<Boolean> USE_GLOBAL_ENDPOINT =
        new ExecutionAttribute<>("UseGlobalEndpoint");

    /**
     * The AWS account ID associated with the identity resolved for this request.
     */
    public static final ExecutionAttribute<String> AWS_AUTH_ACCOUNT_ID =
        new ExecutionAttribute<>("AwsAuthAccountId");

    /**
     * The mode for an AWS account ID that's resolved for this request. See {@link AccountIdEndpointMode} for values.
     */
    public static final ExecutionAttribute<AccountIdEndpointMode> AWS_AUTH_ACCOUNT_ID_ENDPOINT_MODE =
        new ExecutionAttribute<>("AwsAuthAccountIdEndpointMode");

    private AwsExecutionAttribute() {
    }
}
