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

import java.time.Clock;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.aws.checksum.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.aws.crt.internal.signer.DefaultAwsCrtV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials ({@link AwsCredentialsIdentity}),
 * specifically for CRT.
 */
@SdkPublicApi
public interface AwsCrtV4aHttpSigner extends HttpSigner<AwsCredentialsIdentity> {

    /**
     * The AWS region name to be used for computing the signature.
     * This property is required.
     */
    SignerProperty<String> REGION_NAME =
        SignerProperty.create(String.class, "RegionName");

    /**
     * The name of the AWS service.
     * This property is required.
     */
    SignerProperty<String> SERVICE_SIGNING_NAME =
        SignerProperty.create(String.class, "ServiceSigningName");

    /**
     * A {@link Clock} to be used at the time of signing.
     * This property defaults to the time at which signing occurs.
     */
    SignerProperty<Clock> SIGNING_CLOCK =
        SignerProperty.create(Clock.class, "SigningClock");

    /**
     * The name of the header for the checksum.
     * This property is optional.
     */
    SignerProperty<String> CHECKSUM_HEADER_NAME =
        SignerProperty.create(String.class, "ChecksumHeaderName");

    /**
     * The {@link ChecksumAlgorithm} used to compute the checksum.
     * This property is required *if* a checksum-header name is given.
     */
    SignerProperty<ChecksumAlgorithm> CHECKSUM_ALGORITHM =
        SignerProperty.create(ChecksumAlgorithm.class, "ChecksumAlgorithm");

    /**
     * A boolean to indicate whether to double url-encode the resource path
     * when constructing the canonical request.
     * This property defaults to true.
     */
    SignerProperty<Boolean> DOUBLE_URL_ENCODE =
        SignerProperty.create(Boolean.class, "DoubleUrlEncode");

    /**
     * A boolean to indicate whether the resource path should be "normalized"
     * according to RFC3986 when constructing the canonical request.
     * This property defaults to true.
     */
    SignerProperty<Boolean> NORMALIZE_PATH =
        SignerProperty.create(Boolean.class, "NormalizePath");

    /**
     * Whether to enable chunked encoding or not.
     * This property defaults to false.
     * This is only used in the case of an implementation that supports chunked-encoding.
     */
    SignerProperty<Boolean> CHUNKED_ENCODING =
        SignerProperty.create(Boolean.class, "ChunkedEncoding");

    /**
     * Whether to indicate that a payload is signed or not.
     * This property defaults to false.
     */
    SignerProperty<Boolean> PAYLOAD_SIGNING =
        SignerProperty.create(Boolean.class, "PayloadSigning");


    /**
     * Get a default implementation of a {@link AwsCrtV4aHttpSigner}
     *
     * @return AwsCrtV4aHttpSigner
     */
    static AwsCrtV4aHttpSigner create() {
        return new DefaultAwsCrtV4aHttpSigner();
    }
}
