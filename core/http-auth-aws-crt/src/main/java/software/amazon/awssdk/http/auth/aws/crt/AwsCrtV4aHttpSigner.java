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

package software.amazon.awssdk.http.auth.aws.crt;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.crt.internal.signer.DefaultAwsCrtV4aHttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link AwsV4aHttpSigner} that will sign a request using an AWS credentials ({@link AwsCredentialsIdentity}), specifically
 * using CRT.
 * TODO: Remove this once we can remove the auth-scheme.
 */
@SdkPublicApi
public interface AwsCrtV4aHttpSigner extends AwsV4aHttpSigner {

    /**
     * Get a default implementation of a {@link AwsCrtV4aHttpSigner}
     *
     * @return AwsV4aHttpSigner
     */
    static AwsV4aHttpSigner create() {
        return new DefaultAwsCrtV4aHttpSigner();
    }
}
