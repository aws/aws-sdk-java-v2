/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.interceptor;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.core.SdkRequestOverrideConfig;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.regions.Region;

/**
 * AWS-specific attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s and
 * {@link Signer}s.
 */
@ReviewBeforeRelease("When we split the AWS/SDK code: "
                     + "1. The public SDK-specific stuff should be moved to the immutable execution context. "
                     + "2. The private SDK stuff should be moved to an internal SDK attributes collection. "
                     + "3. The private AWS stuff should be moved to an internal AWS attributes collection. "
                     + "4. The remaining public AWS stuff should be moved to the AWS module. "
                     + "We should also consider making some of the SDK/AWS-owned set of attributes part of the immutable context "
                     + "if we don't want the interceptors to modify them.")
public final class AwsExecutionAttributes {
    /**
     * The key under which the request credentials are set.
     */
    public static final ExecutionAttribute<AwsCredentials> AWS_CREDENTIALS = new ExecutionAttribute<>("AwsCredentials");

    /**
     * The key under which the request config is stored.
     */
    @ReviewBeforeRelease("RequestConfig feels pretty internal. Can we just expose parts of it?")
    public static final ExecutionAttribute<SdkRequestOverrideConfig> REQUEST_CONFIG = new ExecutionAttribute<>("RequestConfig");

    /**
     * The key under which the service name is stored.
     */
    public static final ExecutionAttribute<String> SERVICE_NAME = new ExecutionAttribute<>("ServiceName");

    /**
     * The key under which the time offset (for clock skew correction) is stored.
     */
    public static final ExecutionAttribute<Integer> TIME_OFFSET = new ExecutionAttribute<>("TimeOffset");

    /**
     * The AWS {@link Region} the client was configured with.
     */
    public static final ExecutionAttribute<Region> AWS_REGION = new ExecutionAttribute<>("AwsRegion");

    /**
     * Handler context key for advanced configuration.
     */
    public static final ExecutionAttribute<ServiceAdvancedConfiguration> SERVICE_ADVANCED_CONFIG =
            new ExecutionAttribute<>("ServiceAdvancedConfig");

    private AwsExecutionAttributes() {
    }
}
