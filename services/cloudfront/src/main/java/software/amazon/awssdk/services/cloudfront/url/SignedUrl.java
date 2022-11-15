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

package software.amazon.awssdk.services.cloudfront.url;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Base interface class for CloudFront signed URLs
 */
@SdkPublicApi
public interface SignedUrl {

    /**
     * Returns the protocol, i.e., HTTPS / HTTP
     */
    String protocol();

    /**
     * Returns the CloudFront domain, e.g., d1npcfkc2mojrf.cloudfront.net
     */
    String domain();

    /**
     * Returns the encoded path of the signed URL
     */
    String encodedPath();

    /**
     * Returns the signed URL that can be provided to users to access your private content
     */
    String url();

    /**
     * Generates an HTTP GET request that can be executed by an HTTP client to access the resource
     */
    SdkHttpRequest createHttpGetRequest();

}
