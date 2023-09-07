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

package software.amazon.awssdk.http.auth.aws;

import static software.amazon.awssdk.http.auth.aws.internal.util.LoaderUtil.getAwsV4aHttpSigner;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials {@link AwsCredentialsIdentity}).
 * <p>
 * The process for signing requests to send to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkPublicApi
public interface AwsV4aHttpSigner extends AwsV4FamilyHttpSigner, HttpSigner<AwsCredentialsIdentity> {
    /**
     * The AWS region name to be used for computing the signature. This property is required.
     * TODO(sra-identity-and-auth): Should this be a list or rename to SIGNING_SCOPE?
     */
    SignerProperty<String> REGION_NAME = SignerProperty.create(String.class, "SigningScope");

    /**
     * Whether to indicate that a payload is chunk-encoded or not. This property defaults to false. This can be set true to
     * enable the `aws-chunk` content-encoding
     */
    SignerProperty<Boolean> CHUNK_ENCODING_ENABLED =
        SignerProperty.create(Boolean.class, "ChunkEncodingEnabled");

    /**
     * Get a default implementation of a {@link AwsV4aHttpSigner}
     */
    static AwsV4aHttpSigner create() {
        return getAwsV4aHttpSigner();
    }
}
