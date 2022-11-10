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

@SdkPublicApi
public interface CookiesForCannedPolicy extends SignedCookie,
                                                ToCopyableBuilder<CookiesForCannedPolicy.Builder, CookiesForCannedPolicy> {

    String EXPIRES_KEY = "CloudFront-Expires";

    /**
     * Returns the expires key
     */
    String expiresKey();

    /**
     * Returns the expires value
     */
    String expiresValue();

    @NotThreadSafe
    interface Builder extends CopyableBuilder<CookiesForCannedPolicy.Builder, CookiesForCannedPolicy> {

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
         * Configure the expiration value
         */
        Builder expires(String expires);
    }

}
