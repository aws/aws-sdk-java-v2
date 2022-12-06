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

package software.amazon.awssdk.transfer.s3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.transfer.s3.internal.DefaultS3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.model.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.model.Download;
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileDownload;
import software.amazon.awssdk.transfer.s3.model.ResumableFileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * The S3 Transfer Manager is a library that allows users to easily perform accelerated uploads and downloads of objects to and
 * from Amazon S3 and benefit from enhanced throughput and reliability, which is achieved through concurrent transfers of a set of
 * small parts from a single object. The Transfer Manager is built on top of the Java bindings of the AWS Common Runtime S3 client
 * and leverages Amazon S3 multipart upload and byte-range fetches for parallel transfers.
 *
 * <h1>Instantiate Transfer Manager</h1>
 * <b>Create a transfer manager instance with SDK default settings</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = defaultTM}
 * <b>Create a transfer manager instance with custom settings</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = customTM}
 * <h1>Common Usage Patterns</h1>
 * <b>Upload a file to S3</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = uploadFile}
 * <b>Download an S3 object to a local file</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = downloadFile}
 * <b>Upload a local directory to an S3 bucket</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = uploadDirectory}
 * <b>Download S3 objects to a local directory</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = downloadDirectory}
 * <b>Copy an S3 object to a different location in S3</b>
 * {@snippet class = software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region = copy}
 */
@SdkPublicApi
@SdkPreviewApi
public interface S3TransferManager extends SdkAutoCloseable {

    /**
     * Download an object identified by the bucket and key from S3 to a local file. For non-file-based downloads, you may use
     * {@link #download(DownloadRequest)} instead.
     * <p>
     *  The SDK will create a new file if the provided one doesn't exist, otherwise replace the existing file. In the
     *  event of an error, the SDK will NOT attempt to delete the file, leaving it as-is.
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=downloadFile }
     *
     * @see #downloadFile(Consumer)
     * @see #download(DownloadRequest)
     */
    default FileDownload downloadFile(DownloadFileRequest downloadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link DownloadFileRequest} builder, avoiding the need to
     * create one manually via {@link DownloadFileRequest#builder()}.
     *
     * @see #downloadFile(DownloadFileRequest)
     */
    default FileDownload downloadFile(Consumer<DownloadFileRequest.Builder> request) {
        return downloadFile(DownloadFileRequest.builder().applyMutation(request).build());
    }

    /**
     * Resumes a downloadFile operation. This download operation uses the same configuration as the original download. Any data
     * already fetched will be skipped, and only the remaining data is retrieved from Amazon S3. If it is determined that the S3
     * object to download or the file has be modified since the last pause, the SDK will download the object from the beginning
     * as if it is a new {@link DownloadFileRequest} and replace the existing file.
     *
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=resumeDownloadFile }
     * }
     *
     * @param resumableFileDownload the download to resume.
     * @return A new {@code FileDownload} object to use to check the state of the download.
     * @see #downloadFile(DownloadFileRequest)
     * @see FileDownload#pause()
     */
    default FileDownload resumeDownloadFile(ResumableFileDownload resumableFileDownload) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link ResumableFileDownload} builder, avoiding the need to
     * create one manually via {@link ResumableFileDownload#builder()}.
     *
     * @see #resumeDownloadFile(ResumableFileDownload)
     */
    default FileDownload resumeDownloadFile(Consumer<ResumableFileDownload.Builder> resumableFileDownload) {
        return resumeDownloadFile(ResumableFileDownload.builder().applyMutation(resumableFileDownload).build());
    }

    /**
     * Download an object identified by the bucket and key from S3 through the given {@link AsyncResponseTransformer}. For 
     * downloading to a file, you may use {@link #downloadFile(DownloadFileRequest)} instead.
     * <p>
     * <b>Usage Example (this example buffers the entire object in memory and is not suitable for large objects):</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=download }
     *
     * See the static factory methods available in {@link AsyncResponseTransformer} for other use cases.
     *
     * @param downloadRequest the download request, containing a {@link GetObjectRequest} and {@link AsyncResponseTransformer}
     * @param <ResultT>       The type of data the {@link AsyncResponseTransformer} produces
     * @return A {@link Download} that can be used to track the ongoing transfer
     * @see #download(Function)
     * @see #downloadFile(DownloadFileRequest)
     */
    default <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link DownloadRequest} builder, avoiding the need to create
     * one manually via {@link DownloadRequest#builder()}.
     *
     * @see #download(DownloadRequest)
     */
    default <ResultT> Download<ResultT> download(Function<DownloadRequest.UntypedBuilder,
        DownloadRequest.TypedBuilder<ResultT>> request) {
        return download(DownloadRequest.builder().applyMutation(request).build());
    }

    /**
     * Upload a local file to an object in S3. For non-file-based uploads, you may use {@link #upload(UploadRequest)} instead.
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=uploadFile }
     *
     * @see #uploadFile(Consumer) 
     * @see #upload(UploadRequest) 
     */
    default FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link UploadFileRequest} builder, avoiding the need to create
     * one manually via {@link UploadFileRequest#builder()}.
     *
     * @see #uploadFile(UploadFileRequest)
     */
    default FileUpload uploadFile(Consumer<UploadFileRequest.Builder> request) {
        return uploadFile(UploadFileRequest.builder().applyMutation(request).build());
    }

    /**
     * Resumes uploadFile operation. This upload operation uses the same configuration as the original upload. Any data
     * already uploaded will be skipped, and only the remaining data is uploaded to Amazon S3. If it is determined that the file
     * has be modified since the last pause, the SDK will upload the object from the beginning
     * as if it is a new {@link UploadFileRequest}.
     *
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=resumeUploadFile }
     *
     * @param resumableFileUpload the upload to resume.
     * @return A new {@code FileUpload} object to use to check the state of the download.
     * @see #uploadFile(UploadFileRequest)
     * @see FileUpload#pause()
     */
    default FileUpload resumeUploadFile(ResumableFileUpload resumableFileUpload) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link ResumableFileUpload} builder, avoiding the need to
     * create one manually via {@link ResumableFileUpload#builder()}.
     *
     * @see #resumeDownloadFile(ResumableFileDownload)
     */
    default FileUpload resumeUploadFile(Consumer<ResumableFileUpload.Builder> resumableFileUpload) {
        return resumeUploadFile(ResumableFileUpload.builder().applyMutation(resumableFileUpload).build());
    }

    /**
     * Upload the given {@link AsyncRequestBody} to an object in S3. For file-based uploads, you may use
     * {@link #uploadFile(UploadFileRequest)} instead.
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=upload }
     *
     * See the static factory methods available in {@link AsyncRequestBody} for other use cases.
     *
     * @param uploadRequest the upload request, containing a {@link PutObjectRequest} and {@link AsyncRequestBody}
     * @return An {@link Upload} that can be used to track the ongoing transfer
     * @see #upload(Consumer)
     * @see #uploadFile(UploadFileRequest)
     */
    default Upload upload(UploadRequest uploadRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link UploadRequest} builder, avoiding the need to create one
     * manually via {@link UploadRequest#builder()}.
     *
     * @see #upload(UploadRequest)
     */
    default Upload upload(Consumer<UploadRequest.Builder> request) {
        return upload(UploadRequest.builder().applyMutation(request).build());
    }

    /**
     * Upload all files under the given directory to the provided S3 bucket. The key name transformation depends on the optional
     * prefix and delimiter provided in the {@link UploadDirectoryRequest}. By default, all subdirectories will be uploaded
     * recursively, and symbolic links are not followed automatically.
     * This behavior can be configured in at request level via
     * {@link UploadDirectoryRequest.Builder#followSymbolicLinks(Boolean)} } or
     * client level via {@link S3TransferManager.Builder#uploadDirectoryFollowSymbolicLinks(Boolean)}}
     * Note that request-level configuration takes precedence over client-level configuration.
     * <p>
     * By default, the prefix is an empty string and the delimiter is {@code "/"}. Assume you have a local
     * directory "/test" with the following structure:
     * <pre>
     *   {@code
     *      |- test
     *         |- sample.jpg
     *         |- photos
     *             |- 2022
     *                 |- January
     *                     |- sample.jpg
     *                 |- February
     *                     |- sample1.jpg
     *                     |- sample2.jpg
     *                     |- sample3.jpg
     *   }
     * </pre>
     * Give a request to upload directory "/test" to an S3 bucket, the target bucket will have the following
     * S3 objects:
     * <ul>
     *     <li>sample.jpg</li>
     *     <li>photos/2022/January/sample.jpg</li>
     *     <li>photos/2022/February/sample1.jpg</li>
     *     <li>photos/2022/February/sample2.jpg</li>
     *     <li>photos/2022/February/sample3.jpg</li>
     * </ul>
     * <p>
     * The returned {@link CompletableFuture} only completes exceptionally if the request cannot be attempted as a whole (the
     * source directory provided does not exist for example). The future completes successfully for partial successful
     * requests, i.e., there might be failed uploads in the successfully completed response. As a result,
     * you should check for errors in the response via {@link CompletedDirectoryUpload#failedTransfers()}
     * even when the future completes successfully.
     *
     * <p>
     * The current user must have read access to all directories and files.
     *
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=uploadDirectory }
     *
     * @param uploadDirectoryRequest the upload directory request
     * @see #uploadDirectory(Consumer)
     */
    default DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link UploadDirectoryRequest} builder, avoiding the need to
     * create one manually via {@link UploadDirectoryRequest#builder()}.
     *
     * @see #uploadDirectory(UploadDirectoryRequest)
     */
    default DirectoryUpload uploadDirectory(Consumer<UploadDirectoryRequest.Builder> requestBuilder) {
        Validate.paramNotNull(requestBuilder, "requestBuilder");
        return uploadDirectory(UploadDirectoryRequest.builder().applyMutation(requestBuilder).build());
    }

    /**
     * Download all objects under a specific prefix and bucket to the provided directory. By default, all objects in the entire
     * bucket will be downloaded.
     *
     * <p>
     * The downloaded directory structure will match with the provided S3 virtual bucket.
     * For example, assume that you have the following keys in your bucket:
     * <ul>
     *     <li>sample.jpg</li>
     *     <li>photos/2022/January/sample.jpg</li>
     *     <li>photos/2022/February/sample1.jpg</li>
     *     <li>photos/2022/February/sample2.jpg</li>
     *     <li>photos/2022/February/sample3.jpg</li>
     * </ul>
     * Give a request to download the bucket to a destination with path of "/test", the downloaded directory would look like this
     *
     * <pre>
     *   {@code
     *      |- test
     *         |- sample.jpg
     *         |- photos
     *             |- 2022
     *                 |- January
     *                     |- sample.jpg
     *                 |- February
     *                     |- sample1.jpg
     *                     |- sample2.jpg
     *                     |- sample3.jpg
     *   }
     * </pre>
     * <p>
     * The returned {@link CompletableFuture} only completes exceptionally if the request cannot be attempted as a whole (the
     * downloadDirectoryRequest is invalid for example). The future completes successfully for partial successful
     * requests, i.e., there might be failed downloads in a successfully completed response. As a result, you should check for
     * errors in the response via {@link CompletedDirectoryDownload#failedTransfers()} even when the future completes
     * successfully.
     *
     * <p>
     * The SDK will create the destination directory if it does not already exist. If a specific file
     * already exists, the existing content will be replaced with the corresponding S3 object content.
     *
     * <p>
     * The current user must have write access to all directories and files
     *
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=downloadDirectory }
     *
     * @param downloadDirectoryRequest the download directory request
     * @see #downloadDirectory(Consumer)
     */
    default DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link DownloadDirectoryRequest} builder, avoiding the need to
     * create one manually via {@link DownloadDirectoryRequest#builder()}.
     *
     * @see #downloadDirectory(DownloadDirectoryRequest)
     */
    default DirectoryDownload downloadDirectory(Consumer<DownloadDirectoryRequest.Builder> requestBuilder) {
        Validate.paramNotNull(requestBuilder, "requestBuilder");
        return downloadDirectory(DownloadDirectoryRequest.builder().applyMutation(requestBuilder).build());
    }

    /**
     * Creates a copy of an object that is already stored in S3 in the same region.
     * <p>
     * Under the hood, {@link S3TransferManager} will intelligently use plain {@link CopyObjectRequest}s for smaller objects, or
     * multiple parallel {@link UploadPartCopyRequest}s for larger objects. This behavior can be configured via
     * {@link S3CrtAsyncClientBuilder#minimumPartSizeInBytes(Long)}. Note that for multipart copy request, existing metadata
     * stored in the source object is NOT copied to the destination object; if required, you can retrieve the metadata
     * from the source object and set it explicitly in the {@link CopyObjectRequest.Builder#metadata(Map)}.
     *
     * <p>
     * While this API supports {@link TransferListener}s, they will not receive {@code bytesTransferred} callback-updates due to
     * the way the {@link CopyObjectRequest} API behaves. When copying an object, S3 performs the byte copying on your behalf
     * while keeping the connection alive. The progress of the copy is not known until it fully completes and S3 sends a response
     * describing the outcome.
     * <p>
     * <b>Usage Example:</b>
     * {@snippet class=software.amazon.awssdk.transfer.s3.samples.S3TransferManagerSamples region=copy }
     *
     * @param copyRequest the copy request, containing a {@link CopyObjectRequest}
     * @return A {@link Copy} that can be used to track the ongoing transfer
     * @see #copy(Consumer)
     */
    default Copy copy(CopyRequest copyRequest) {
        throw new UnsupportedOperationException();
    }

    /**
     * This is a convenience method that creates an instance of the {@link CopyRequest} builder, avoiding the need to create one
     * manually via {@link CopyRequest#builder()}.
     *
     * @see #copy(CopyRequest)
     */
    default Copy copy(Consumer<CopyRequest.Builder> copyRequestBuilder) {
        return copy(CopyRequest.builder().applyMutation(copyRequestBuilder).build());
    }

    /**
     * Create an {@code S3TransferManager} using the default values.
     */
    static S3TransferManager create() {
        return builder().build();
    }

    /**
     * Creates a default builder for {@link S3TransferManager}.
     */
    static S3TransferManager.Builder builder() {
        return DefaultS3TransferManager.builder();
    }

    /**
     * The builder definition for a {@link S3TransferManager}.
     */
    interface Builder {

        /**
         * Specify the low level {@link S3AsyncClient} that will be used to send requests to S3. The SDK will create a default
         * {@link S3AsyncClient} if not provided.
         *
         * <p>
         * It's highly recommended using {@link S3AsyncClient#crtBuilder()} to create an {@link S3AsyncClient} instance to benefit
         * from multipart upload/download feature and maximum throughput.
         *
         * <p>
         * Note: the provided {@link S3AsyncClient} will not be closed when the transfer manager is closed; it must be closed by
         * the caller when it is ready to be disposed.
         *
         * @param s3AsyncClient the S3 async client
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see S3AsyncClient#crtBuilder()
         */
        Builder s3Client(S3AsyncClient s3AsyncClient);

        /**
         * Specify the executor that {@link S3TransferManager} will use to execute background tasks before handing them off to the
         * underlying S3 async client, such as visiting file tree in a
         * {@link S3TransferManager#uploadDirectory(UploadDirectoryRequest)}
         * operation
         *
         * <p>
         * The SDK will create an executor if not provided
         *
         * <p>
         * <b>This executor must be shut down by the user when it is ready to be disposed. The SDK will not close the executor
         * when the s3 transfer manager is closed.</b>
         *
         * @param executor the executor to use
         * @return this builder for method chaining.
         */
        Builder executor(Executor executor);

        /**
         * Specify whether to follow symbolic links when traversing the file tree in
         * {@link S3TransferManager#uploadDirectory} operation
         * <p>
         * Default to false
         *
         * @param uploadDirectoryFollowSymbolicLinks whether to follow symbolic links
         * @return This builder for method chaining.
         */
        Builder uploadDirectoryFollowSymbolicLinks(Boolean uploadDirectoryFollowSymbolicLinks);

        /**
         * Specify the maximum number of levels of directories to visit in {@link S3TransferManager#uploadDirectory} operation.
         * Must be positive. 1 means only the files directly within
         * the provided source directory are visited.
         *
         * <p>
         * Default to {@code Integer.MAX_VALUE}
         *
         * @param uploadDirectoryMaxDepth the maximum number of directory levels to visit
         * @return This builder for method chaining.
         */
        Builder uploadDirectoryMaxDepth(Integer uploadDirectoryMaxDepth);

        /**
         * Build an instance of {@link S3TransferManager} based on the settings supplied to this builder
         *
         * @return an instance of {@link S3TransferManager}
         */
        S3TransferManager build();

    }
}
