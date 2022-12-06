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

package software.amazon.awssdk.services.cloudfront.model;

import java.security.PrivateKey;
import java.time.Instant;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Base interface class for requests to generate a CloudFront signed URL or signed cookie
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public interface CloudFrontSignerRequest {

    /**
     * Returns the resource URL, i.e., the unsigned URL
     */
    String resourceUrl();

    /**
     * Returns the private key used to generate the signature
     */
    PrivateKey privateKey();

    /**
     * Returns the key pair ID, i.e., the public key ID for the CloudFront public key whose corresponding private key you're
     * using to generate the signature
     */
    String keyPairId();

    /**
     * Returns the expiration date, after which users will no longer be able to use the signed URL/cookie to access your
     * private content
     */
    Instant expirationDate();
}
