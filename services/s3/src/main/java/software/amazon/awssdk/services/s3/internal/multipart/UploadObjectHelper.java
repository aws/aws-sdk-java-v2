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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * An internal helper class that automatically uses multipart upload based on the size of the object.
 */
@SdkInternalApi
public final class UploadObjectHelper {
    private static final Logger log = Logger.loggerFor(UploadObjectHelper.class);

    private final S3AsyncClient s3AsyncClient;
    private final long partSizeInBytes;
    private final GenericMultipartHelper<PutObjectRequest, PutObjectResponse> genericMultipartHelper;

    private final long apiCallBufferSize;
    private final long multipartUploadThresholdInBytes;
    private final UploadWithKnownContentLengthHelper uploadWithKnownContentLength;
    private final UploadWithUnknownContentLengthHelper uploadWithUnknownContentLength;

    public UploadObjectHelper(S3AsyncClient s3AsyncClient,
                              MultipartConfigurationResolver resolver) {
        this.s3AsyncClient = s3AsyncClient;
        this.partSizeInBytes = resolver.minimalPartSizeInBytes();
        this.genericMultipartHelper = new GenericMultipartHelper<>(s3AsyncClient,
                                                                   SdkPojoConversionUtils::toAbortMultipartUploadRequest,
                                                                   SdkPojoConversionUtils::toPutObjectResponse);
        this.apiCallBufferSize = resolver.apiCallBufferSize();
        this.multipartUploadThresholdInBytes = resolver.thresholdInBytes();
        this.uploadWithKnownContentLength = new UploadWithKnownContentLengthHelper(s3AsyncClient,
                                                                                   partSizeInBytes,
                                                                                   multipartUploadThresholdInBytes,
                                                                                   apiCallBufferSize);
        this.uploadWithUnknownContentLength = new UploadWithUnknownContentLengthHelper(s3AsyncClient,
                                                                                       partSizeInBytes,
                                                                                       multipartUploadThresholdInBytes,
                                                                                       apiCallBufferSize);
    }

    public CompletableFuture<PutObjectResponse> uploadObject(PutObjectRequest putObjectRequest,
                                                             AsyncRequestBody asyncRequestBody) {
        Long contentLength = asyncRequestBody.contentLength().orElseGet(putObjectRequest::contentLength);

        if (contentLength == null) {
            return uploadWithUnknownContentLength.uploadObject(putObjectRequest, asyncRequestBody);
        } else {
            return uploadWithKnownContentLength.uploadObject(putObjectRequest, asyncRequestBody, contentLength.longValue());
        }
    }
}
