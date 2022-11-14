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

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Base interface class for CloudFront cookies with custom policies
 */
@SdkPublicApi
public interface CookiesForCustomPolicy extends SignedCookie,
                                                ToCopyableBuilder<CookiesForCustomPolicy.Builder, CookiesForCustomPolicy> {

    String POLICY_KEY = "CloudFront-Policy";

    /**
     * Returns the cookie policy header value
     */
    String policyHeaderValue();

    @NotThreadSafe
    interface Builder extends CopyableBuilder<CookiesForCustomPolicy.Builder, CookiesForCustomPolicy> {

        /**
         * Configure the key pair ID value
         */
        Builder keyPairId(String keyPairId);

        /**
         * Configure the signature value
         */
        Builder signature(String signature);

        /**
         * Configure the resource URL
         */
        Builder resourceUrl(String resourceUrl);

        /**
         * Configure the policy value
         */
        Builder policy(String policy);
    }

}
