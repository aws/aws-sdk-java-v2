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

package software.amazon.awssdk.services.cloudfront.internal.url;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkInternalApi
public interface SignedUrl extends ToCopyableBuilder<SignedUrl.Builder, SignedUrl> {

    /**
     * Returns the signed URL
     */
    String url();

    /**
     * Generates an HTTP request that can be executed by an HTTP client to access the resource
     */
    SdkHttpRequest generateHttpRequest();

    @NotThreadSafe
    @SdkPublicApi
    interface Builder extends CopyableBuilder<Builder, SignedUrl> {

        /**
         * Configure the signed URL
         */
        Builder url(String url);
    }

}
