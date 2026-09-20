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

package software.amazon.awssdk.services.cloudfront.utils;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The hash algorithm to use when generating CloudFront signed URLs or signed cookies.
 * <p>
 * CloudFront supports SHA-1 (default) and SHA-256 for signed URLs and signed cookies.
 * When using SHA-256, the signed URL will include a {@code Hash-Algorithm=SHA256} query parameter,
 * and signed cookies will include a {@code CloudFront-Hash-Algorithm=SHA256} cookie.
 */
@SdkPublicApi
public enum SigningHashAlgorithm {
    SHA1("SHA1"),
    SHA256("SHA256");

    private final String id;

    SigningHashAlgorithm(String id) {
        this.id = id;
    }

    /**
     * The algorithm identifier used in CloudFront's {@code Hash-Algorithm} parameter
     * and in Java Security algorithm name construction (e.g., {@code "SHA256withRSA"}).
     */
    public String id() {
        return id;
    }
}
