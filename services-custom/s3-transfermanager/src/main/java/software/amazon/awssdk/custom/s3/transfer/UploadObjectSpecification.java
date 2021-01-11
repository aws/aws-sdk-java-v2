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

package software.amazon.awssdk.custom.s3.transfer;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.custom.s3.transfer.internal.ApiRequestUploadObjectSpecification;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Union of the various ways to specify how to upload an object from S3.
 */
@SdkProtectedApi
public abstract class UploadObjectSpecification implements TransferSpecification {

    /**
     * @return This specification as an API request.
     * @throws IllegalStateException If this is not an API request.
     */
    public PutObjectRequest asApiRequest() {
        throw new IllegalStateException("Not an API request");
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
