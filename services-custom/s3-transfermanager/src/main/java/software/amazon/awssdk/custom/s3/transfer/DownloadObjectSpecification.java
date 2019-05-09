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

package software.amazon.awssdk.custom.s3.transfer;

import java.net.URL;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Union of the various ways to specify how to download an object from S3.
 */
@SdkPublicApi
public abstract class DownloadObjectSpecification {

    DownloadObjectSpecification() {
    }

    /**
     * @return {@code true} if this is a presigned URL, {@code false} otherwise.
     */
    public boolean isPresignedUrl() {
        return false;
    }

    /**
     * @return {@code true} if this is an API request, {@code false} otherwise.
     */
    public boolean isApiRequest() {
        return false;
    }

    /**
     * @return This specification as a presigned URL.
     * @throws IllegalStateException If this is not a presigned URL.
     */
    public URL asPresignedUrl() {
        throw new IllegalStateException("Not a presigned URL");
    }

    /**
     * @return This specification as an API request.
     * @throws IllegalStateException If this is not an API request.
     */
    public GetObjectRequest asApiRequest() {
        throw new IllegalStateException("Not an API Request");
    }

    /**
     * Create a specification from a {@link GetObjectRequest}.
     *
     * @param getObjectRequest The request.
     * @return The new API request specification.
     */
    public static DownloadObjectSpecification fromApiRequest(GetObjectRequest getObjectRequest) {
        return new ApiRequestDownloadObjectSpecification(getObjectRequest);
    }
}
