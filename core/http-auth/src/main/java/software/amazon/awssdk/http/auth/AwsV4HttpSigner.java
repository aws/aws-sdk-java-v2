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

package software.amazon.awssdk.http.auth;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request
 * using an AWS credentials ({@link AwsCredentialsIdentity}).
 */
@SdkPublicApi
public interface AwsV4HttpSigner extends HttpSigner<AwsCredentialsIdentity> {

    /**
     * Get a default implementation of a {@link AwsV4HttpSigner}
     *
     * @return AwsV4HttpSigner
     */
    static AwsV4HttpSigner create() {
        return new DefaultAwsV4HttpSigner();
    }
}
