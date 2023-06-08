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

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4HttpSigner;
import software.amazon.awssdk.http.auth.internal.checksums.Algorithm;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials {@link AwsCredentialsIdentity}).
 */
@SdkPublicApi
public interface AwsV4HttpSigner extends HttpSigner<AwsCredentialsIdentity> {

    /**
     * The datetime, in milliseconds, for the request.
     */
    SignerProperty<Instant> REQUEST_SIGNING_INSTANT =
        SignerProperty.create(Instant.class, "requestSigningInstant");
    /**
     * The AWS region to be used for computing the signature.
     */
    SignerProperty<String> REGION_NAME =
        SignerProperty.create(String.class, "regionName");
    /**
     * The name of the AWS service.
     */
    SignerProperty<String> SERVICE_SIGNING_NAME =
        SignerProperty.create(String.class, "serviceSigningName");
    /**
     * The name of the header for the checksum.
     */
    SignerProperty<String> CHECKSUM_HEADER_NAME =
        SignerProperty.create(String.class, "checksumHeaderName");
    /**
     * The Algorithm used to compute the checksum.
     */
    SignerProperty<Algorithm> CHECKSUM_ALGORITHM =
        SignerProperty.create(Algorithm.class, "checksumAlgorithm");
    /**
     * A boolean to indicate whether to double url-encode the resource path
     * when constructing the canonical request.
     */
    SignerProperty<Boolean> DOUBLE_URL_ENCODE =
        SignerProperty.create(Boolean.class, "doubleUrlEncode");
    /**
     * A boolean to indicate Whether the resource path should be "normalized"
     * according to RFC3986 when constructing the canonical request.
     */
    SignerProperty<Boolean> NORMALIZE_PATH =
        SignerProperty.create(Boolean.class, "normalizePath");

    /**
     * Get a default implementation of a {@link AwsV4HttpSigner}
     *
     * @return AwsV4HttpSigner
     */
    static AwsV4HttpSigner create() {
        return new DefaultAwsV4HttpSigner();
    }
}
