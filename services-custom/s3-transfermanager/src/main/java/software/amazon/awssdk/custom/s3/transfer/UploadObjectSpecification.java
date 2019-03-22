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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Union of the various ways to specify how to upload an object from S3.
 */
@SdkPublicApi
public abstract class UploadObjectSpecification {

    UploadObjectSpecification() {
    }

    /**
     * @return {@code true} if this is an API request, {@code false} otherwise.
     */
    public boolean isApiRequest() {
        return false;
    }

    /**
     * @return {@code true} if this is a presigned URL, {@code false} otherwise.
     */
    public boolean isPresignedUrl() {
        return false;
    }

    /**
     * @return This specification as an API request.
     * @throws IllegalStateException If this is not an API request.
     */
    public PutObjectRequest asApiRequest() {
        throw new IllegalStateException("Not an API request");
    }

    /**
     * @return This specification as a presigned URL.
     * @throws IllegalStateException If this is not a presigned URL.
     */
    public URL asPresignedUrl() {
        throw new IllegalStateException("Not a presigned URL");
    }

    /**
     * Create a specification from a {@link PutObjectRequest}.
     *
     * @param apiRequest The request.
     * @return The new API request specification.
     */
    static UploadObjectSpecification fromApiRequest(PutObjectRequest apiRequest) {
        return new ApiRequestUploadObjectSpecification(apiRequest);
    }
}
