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

import java.time.Duration;
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
public interface AwsV4aHttpSigner extends HttpSigner<AwsCredentialsIdentity> {
    /**
     * The AWS region name to be used for computing the signature. This property is required.
     */
    SignerProperty<String> REGION_NAME =
        SignerProperty.create(String.class, "RegionName");

    /**
     * The name of the AWS service. This property is required.
     */
    SignerProperty<String> SERVICE_SIGNING_NAME =
        SignerProperty.create(String.class, "ServiceSigningName");

    /**
     * A boolean to indicate whether to double url-encode the resource path when constructing the canonical request. This property
     * defaults to true.
     */
    SignerProperty<Boolean> DOUBLE_URL_ENCODE =
        SignerProperty.create(Boolean.class, "DoubleUrlEncode");

    /**
     * A boolean to indicate whether the resource path should be "normalized" according to RFC3986 when constructing the canonical
     * request. This property defaults to true.
     */
    SignerProperty<Boolean> NORMALIZE_PATH =
        SignerProperty.create(Boolean.class, "NormalizePath");

    /**
     * The location where auth-related data is inserted, as a result of signing. This property defaults to HEADER.
     */
    SignerProperty<AuthLocation> AUTH_LOCATION =
        SignerProperty.create(AuthLocation.class, "AuthLocation");

    /**
     * The duration for the request to be valid. This property defaults to the max valid duration (7 days). This is only used in
     * the case of a pre-signing implementation.
     */
    SignerProperty<Duration> EXPIRATION_DURATION =
        SignerProperty.create(Duration.class, "ExpirationDuration");

    /**
     * Whether to indicate that a payload is signed or not. This property defaults to true. This can be set false to disable
     * payload signing.
     */
    SignerProperty<Boolean> PAYLOAD_SIGNING_ENABLED =
        SignerProperty.create(Boolean.class, "PayloadSigningEnabled");


    /**
     * Get a default implementation of a {@link AwsV4aHttpSigner}
     */
    static AwsV4aHttpSigner create() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /**
     * This enum represents where auth-related data is inserted, as a result of signing.
     */
    enum AuthLocation {
        /**
         * Indicates auth-related data is inserted in HTTP headers.
         */
        HEADER,

        /**
         * Indicates auth-related data is inserted in HTTP query-parameters.
         */
        QUERY_STRING
    }
}
