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
import software.amazon.awssdk.http.auth.internal.DefaultAwsV4QueryHttpSigner;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * An {@link HttpSigner} that will sign a request using an AWS credentials ({@link AwsCredentialsIdentity}),
 * specifically for query.
 * <p>
 * The process for signing requests to AWS services is documented
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_aws-signing.html">here</a>.
 */
@SdkPublicApi
public interface AwsV4QueryHttpSigner extends HttpSigner<AwsCredentialsIdentity> {

    /**
     * The datetime, in milliseconds, for the request.
     * This property is required.
     */
    SignerProperty<Instant> REQUEST_SIGNING_INSTANT =
        SignerProperty.create(Instant.class, "RequestSigningInstant");

    /**
     * The datetime, in milliseconds, for the request.
     * This property defaults to the max valid duration (7 days) from the signing instant.
     */
    SignerProperty<Instant> EXPIRATION_INSTANT =
        SignerProperty.create(Instant.class, "ExpirationInstant");

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
     * A boolean to indicate whether to double url-encode the resource path
     * when constructing the canonical request.
     * This property defaults to true.
     */
    SignerProperty<Boolean> DOUBLE_URL_ENCODE =
        SignerProperty.create(Boolean.class, "DoubleUrlEncode");

    /**
     * A boolean to indicate Whether the resource path should be "normalized"
     * according to RFC3986 when constructing the canonical request.
     * This property defaults to true.
     */
    SignerProperty<Boolean> NORMALIZE_PATH =
        SignerProperty.create(Boolean.class, "NormalizePath");

    /**
     * Get a default implementation of a {@link AwsV4QueryHttpSigner}
     *
     * @return AwsV4QueryHttpSigner
     */
    static AwsV4QueryHttpSigner create() {
        return new DefaultAwsV4QueryHttpSigner();
    }
}
