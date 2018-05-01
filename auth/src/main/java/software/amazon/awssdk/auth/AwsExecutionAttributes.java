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

package software.amazon.awssdk.auth;

import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttributes;
import software.amazon.awssdk.core.runtime.auth.Signer;
import software.amazon.awssdk.regions.Region;

/**
 * AWS-specific attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s and
 * {@link Signer}s.
 */
@ReviewBeforeRelease("We should also consider making some of the SDK/AWS-owned set of attributes part of the immutable context"
                     + "if we don't want the interceptors to modify them.")
public final class AwsExecutionAttributes extends SdkExecutionAttributes {
    /**
     * The key under which the request credentials are set.
     */
    public static final ExecutionAttribute<AwsCredentials> AWS_CREDENTIALS = new ExecutionAttribute<>("AwsCredentials");

    /**
     * The AWS {@link Region} the client was configured with.
     */
    public static final ExecutionAttribute<Region> AWS_REGION = new ExecutionAttribute<>("AwsRegion");

    private AwsExecutionAttributes() {
    }
}
