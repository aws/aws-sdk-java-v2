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

package software.amazon.awssdk.services.cloudfront.cookie;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Base interface class for CloudFront signed cookies
 */
@Immutable
@ThreadSafe
@SdkPublicApi
public interface SignedCookie {

    String COOKIE = "Cookie";

    /**
     * Returns the resource URL
     */
    String resourceUrl();

    /**
     * Generates an HTTP GET request that can be executed by an HTTP client to access the resource
     */
    SdkHttpRequest createHttpGetRequest();

    /**
     * Returns the cookie signature header value that can be appended to an HTTP GET request
     * i.e., "CloudFront-Signature=[SIGNATURE_VALUE]"
     */
    String signatureHeaderValue();

    /**
     * Returns the cookie key-pair-Id header value that can be appended to an HTTP GET request
     * i.e., "CloudFront-Key-Pair-Id=[KEY_PAIR_ID_VALUE]"
     */
    String keyPairIdHeaderValue();

}
