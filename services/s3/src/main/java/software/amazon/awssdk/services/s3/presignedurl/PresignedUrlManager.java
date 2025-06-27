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

package software.amazon.awssdk.services.s3.presignedurl;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;

/**
 * Manager for executing S3 sync client operations using presigned URLs.
 * This allows downloading S3 objects without requiring AWS credentials at request time.
 */
@SdkPublicApi
public interface PresignedUrlManager {

    /**
     * Downloads an S3 object using a presigned URL with a custom response transformer.
     *
     * @param request             The presigned URL request containing the URL and optional range parameters
     * @param responseTransformer Transforms the response to the desired return type
     * @param <ReturnT>           The type of the transformed response
     * @return The transformed response
     */
    <ReturnT> ReturnT getObject(PresignedUrlGetObjectRequest request,
                                ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer);

    //TODO: Add other getObject method flavors
}