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
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlGetObjectRequest;

/**
 * Interface for executing S3 operations using presigned URLs. This can be accessed using
 * {@link S3Client#presignedUrlManager()}.
 */
@SdkPublicApi
public interface PresignedUrlManager {
    /**
     * <p>
     * Downloads an S3 object using a presigned URL.
     * </p>
     * <p>
     * This operation uses a presigned URL that contains all necessary authentication information,
     * eliminating the need for AWS credentials at request time. The presigned URL must be valid
     * and not expired.
     * </p>
     * <dl>
     * <dt>Range Requests</dt>
     * <dd>
     * <p>
     * Supports partial object downloads using HTTP Range headers. Specify the range parameter
     * in the request to download only a portion of the object (e.g., "bytes=0-1023").
     * </p>
     * </dd>
     * <dt>Storage Classes</dt>
     * <dd>
     * <p>
     * If the object is stored in S3 Glacier Flexible Retrieval, S3 Glacier Deep Archive,
     * or S3 Intelligent-Tiering Archive tiers, the object must be restored before retrieval.
     * Otherwise, this operation returns an <code>InvalidObjectState</code> error.
     * </p>
     * </dd>
     * </dl>
     *
     * @param request
     *        The presigned URL request containing the URL and optional range parameters
     * @param responseTransformer
     *        Transforms the response to the desired return type. See
     *        {@link software.amazon.awssdk.core.sync.ResponseTransformer} for pre-built implementations
     *        like downloading to a file or converting to bytes.
     * @param <ReturnT>
     *        The type of the transformed response
     * @return The transformed result of the ResponseTransformer
     * @throws software.amazon.awssdk.services.s3.model.NoSuchKeyException
     *         The specified object does not exist
     * @throws software.amazon.awssdk.services.s3.model.InvalidObjectStateException
     *         Object is archived and must be restored before retrieval
     * @throws software.amazon.awssdk.core.exception.SdkClientException
     *         If any client side error occurs such as network failures or invalid presigned URL
     * @throws S3Exception
     *         Base class for all S3 service exceptions. Unknown exceptions will be thrown as an instance of this type.
     */
    default <ReturnT> ReturnT getObject(PresignedUrlGetObjectRequest request,
                                ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) throws NoSuchKeyException,
                                                                                                            InvalidObjectStateException, SdkClientException, S3Exception {
        throw new UnsupportedOperationException();
    }

    //TODO: Add other getObject method flavors
}