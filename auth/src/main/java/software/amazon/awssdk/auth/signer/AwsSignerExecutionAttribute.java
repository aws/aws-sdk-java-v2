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

package software.amazon.awssdk.auth.signer;

import java.time.Instant;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.regions.Region;

/**
 * AWS-specific signing attributes attached to the execution. This information is available to {@link ExecutionInterceptor}s and
 * {@link Signer}s.
 */
@ReviewBeforeRelease("We should also consider making some of the SDK/AWS-owned set of attributes part of the immutable context"
                     + "if we don't want the interceptors to modify them.")
@SdkProtectedApi
public final class AwsSignerExecutionAttribute extends SdkExecutionAttribute {
    /**
     * The key under which the request credentials are set.
     */
    public static final ExecutionAttribute<AwsCredentials> AWS_CREDENTIALS = new ExecutionAttribute<>("AwsCredentials");

    /**
     * The AWS {@link Region} that is used for signing a request. This is not always same as the region configured on the client
     * for global services like IAM.
     */
    public static final ExecutionAttribute<Region> SIGNING_REGION = new ExecutionAttribute<>("SigningRegion");

    /**
     * The signing name of the service to be using in SigV4 signing
     */
    public static final ExecutionAttribute<String> SERVICE_SIGNING_NAME = new ExecutionAttribute<>("ServiceSigningName");

    /**
     * The key to specify whether to use double url encoding during signing.
     */
    public static final ExecutionAttribute<Boolean> SIGNER_DOUBLE_URL_ENCODE = new ExecutionAttribute<>("DoubleUrlEncode");

    /**
     * The key to specify the expiration time when pre-signing aws requests.
     */
    public static final ExecutionAttribute<Instant> PRESIGNER_EXPIRATION = new ExecutionAttribute<>("PresignerExpiration");

    private AwsSignerExecutionAttribute() {
    }
}
