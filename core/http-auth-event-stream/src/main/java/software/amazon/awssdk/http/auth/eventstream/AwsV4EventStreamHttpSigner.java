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

package software.amazon.awssdk.http.auth.eventstream;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.eventstream.internal.DefaultAwsV4EventStreamHttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials ({@link AwsCredentialsIdentity}),
 * specifically for Event Streams.
 */
@SdkPublicApi
public interface AwsV4EventStreamHttpSigner extends AwsV4HttpSigner {

    /**
     * Get a default implementation of a {@link AwsV4EventStreamHttpSigner}
     *
     * @return AwsV4EventStreamHttpSigner
     */
    static AwsV4EventStreamHttpSigner create() {
        return new DefaultAwsV4EventStreamHttpSigner();
    }
}
