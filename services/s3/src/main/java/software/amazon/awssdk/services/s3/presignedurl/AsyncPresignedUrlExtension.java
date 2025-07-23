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

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;

/**
 * Interface for executing S3 operations asynchronously using presigned URLs. This can be accessed using
 * {@link S3AsyncClient#presignedUrlExtension()}.
 */
@SdkPublicApi
public interface AsyncPresignedUrlExtension {
    /**
     * <p>
     * Downloads an S3 object asynchronously using a presigned URL.
     * </p>
     *
     * <p>
     * This operation uses a presigned URL to download an object from Amazon S3. The presigned URL must be valid and not expired.
     * </p>
     *
     * <p>
     * To download a specific byte range of the object, use the range parameter in the request.
     * </p>
     *
     * @param request             The presigned URL request containing the URL and optional parameters
     * @param responseTransformer Transforms the response to the desired return type
     * @param <ReturnT>           The type of the transformed response
     * @return A {@link CompletableFuture} containing the transformed result
     * @throws SdkClientException If any client side error occurs
     * @throws S3Exception        Base class for all S3 service exceptions
     */
    default <ReturnT> CompletableFuture<ReturnT> getObject(PresignedUrlDownloadRequest request,
                                                           AsyncResponseTransformer<GetObjectResponse,
                                                               ReturnT> responseTransformer) {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Downloads an S3 object asynchronously using a presigned URL with a consumer-based request builder.
     * </p>
     *
     * <p>
     * This is a convenience method that creates a {@link PresignedUrlDownloadRequest} using the provided consumer.
     * </p>
     *
     * @param requestConsumer     Consumer that will configure a {@link PresignedUrlDownloadRequest.Builder}
     * @param responseTransformer Transforms the response to the desired return type
     * @param <ReturnT>           The type of the transformed response
     * @return A {@link CompletableFuture} containing the transformed result
     * @throws SdkClientException If any client side error occurs
     * @throws S3Exception        Base class for all S3 service exceptions
     */
    default <ReturnT> CompletableFuture<ReturnT> getObject(Consumer<PresignedUrlDownloadRequest.Builder> requestConsumer,
                                                           AsyncResponseTransformer<GetObjectResponse,
                                                               ReturnT> responseTransformer) {
        return getObject(PresignedUrlDownloadRequest.builder().applyMutation(requestConsumer).build(), responseTransformer);
    }

    /**
     * <p>
     * Downloads an S3 object asynchronously using a presigned URL and saves it to the specified file path.
     * </p>
     *
     * <p>
     * This is a convenience method that uses {@link AsyncResponseTransformer#toFile(Path)} to save the object
     * directly to a file.
     * </p>
     *
     * @param request         The presigned URL request containing the URL and optional parameters
     * @param destinationPath The path where the downloaded object will be saved
     * @return A {@link CompletableFuture} containing the {@link GetObjectResponse}
     * @throws SdkClientException If any client side error occurs
     * @throws S3Exception        Base class for all S3 service exceptions
     */
    default CompletableFuture<GetObjectResponse> getObject(PresignedUrlDownloadRequest request,
                                                           Path destinationPath) {
        return getObject(request, AsyncResponseTransformer.toFile(destinationPath));
    }

    /**
     * <p>
     * Downloads an S3 object asynchronously using a presigned URL with a consumer-based request builder
     * and saves it to the specified file path.
     * </p>
     *
     * <p>
     * This is a convenience method that combines consumer-based request building with file-based response handling.
     * </p>
     *
     * @param requestConsumer Consumer that will configure a {@link PresignedUrlDownloadRequest.Builder}
     * @param destinationPath The path where the downloaded object will be saved
     * @return A {@link CompletableFuture} containing the {@link GetObjectResponse}
     * @throws SdkClientException If any client side error occurs
     * @throws S3Exception        Base class for all S3 service exceptions
     */
    default CompletableFuture<GetObjectResponse> getObject(Consumer<PresignedUrlDownloadRequest.Builder> requestConsumer,
                                                           Path destinationPath) {
        return getObject(requestConsumer, AsyncResponseTransformer.toFile(destinationPath));
    }
}