/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3;

import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.GetUrlResponse;

/**
 * S3 client that has custom operations not included in service model.
 * This interface should not be used directly. Please use {@link S3AsyncClient} instead.
 */
@SdkProtectedApi
interface S3EnhancementAsyncClient {

    /**
     * Returns a response with the URL for an object stored in Amazon S3.
     *
     * If the object identified by the given bucket and key has public read permissions,
     * then this URL can be directly accessed to retrieve the object's data.
     *
     * @param getUrlRequest request to construct url
     * @return A Java Future containing the response of the operation
     * @throws MalformedURLException Generated Url is malformed
     */
    default CompletableFuture<GetUrlResponse> getUrl(GetUrlRequest getUrlRequest) throws MalformedURLException {
        throw new UnsupportedOperationException();
    }
}
