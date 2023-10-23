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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class DownloadObjectHelper {

    private final S3AsyncClient s3;
    private final long apiCallBufferSize;

    DownloadObjectHelper(S3AsyncClient s3, long apiCallBufferSize) {
        this.apiCallBufferSize = apiCallBufferSize;
        this.s3 = s3;
    }

    <T> CompletableFuture<T> getObject(GetObjectRequest getObjectRequest,
                                          AsyncResponseTransformer<GetObjectResponse, T> asyncResponseTransformer) {
        // do not use multipart when byte range or part number is manually specified
        if (!StringUtils.isEmpty(getObjectRequest.range()) || Objects.isNull(getObjectRequest.partNumber())) {
            return s3.getObject(getObjectRequest, asyncResponseTransformer);
        }
        PartNumberDownloader<T> downloader = new PartNumberDownloader<>(s3, apiCallBufferSize);
        return downloader.getObject(getObjectRequest, asyncResponseTransformer);
    }
}
