package software.amazon.awssdk.services.s3.transfer;

/**
 * The S3 Transfer Manager is a library that allows users to easily and
 * optimally upload and downloads to and from S3.
 * <p>
 * The list of features includes:
 * <ul>
 *   <li>Parallel uploads and downloads</li>
 *   <li>Bandwidth limiting</li>
 *   <li>Pause and resume of transfers</li>
 * </ul>
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * // Create using all default configuration values
 * S3TransferManager tm = S3TranferManager.create();
 *
 * // Using custom configuration values to set max upload speed to avoid
 * // saturating the network interface.
 * S3TransferManager tm = S3TransferManager.builder()
 *             .maxUploadBytesSecond(32 * 1024 * 1024) // 32 MiB
 *             .build()
 *         .build();
 * }
 * </pre>
 */
public interface S3TransferManager {
    /**
     * Download an object identified by the bucket and key from S3 to the given
     * file.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate transfer
     * Download myFileDownload = tm.download(BUCKET, KEY, Paths.get("/tmp/myFile.txt");
     * // Wait for transfer to complete
     * myFileDownload().completionFuture().join();
     * }
     * </pre>
     *
     */
    default Download download(String bucket, String key, Path file) {
        return download(DownloadObjectRequest.builder()
                        .downloadSpecification(DownloadObjectSpecification.fromApiRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()))
                        .build(),
                file);
    }

    /**
     * Download an object using an S3 presigned URL to the given file.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate transfer
     * Download myFileDownload = tm.download(myPresignedUrl, Paths.get("/tmp/myFile.txt");
     * // Wait for transfer to complete
     * myFileDownload()completionFuture().join();
     * }
     * </pre>
     */
    default Download download(URL presignedUrl, Path file) {
        return download(DownloadObjectRequest.builder()
                        .downloadSpecification(DownloadObjectSpecification.fromPresignedUrl(presignedUrl))
                        .build(),
                file);
    }
    /**
     * Download an object in S3 to the given file.
     *
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * // Initiate the transfer
     * Download myDownload = tm.download(DownloadObjectRequest.builder()
     *         .downloadObjectSpecification(DownloadObjectSpecification.fromApiRequest(
     *             GetObjectRequest.builder()
     *                 .bucket(BUCKET)
     *                 .key(KEY)
     *                 .build()))
     *          // Set the known length of the object to avoid a HeadObject call
     *         .size(1024 * 1024 * 5)
     *         .build(),
     *         Paths.get("/tmp/myFile.txt"));
     * // Wait for the transfer to complete
     * myDownload.completionFuture().join();
     * }
     * </pre>
     */
    Download download(DownloadObjectRequest request, Path file);

    /**
     * Resume a previously paused object download.
     */
    Download resumeDownloadObject(DownloadObjectState downloadObjectState);

    /**
     * Download the set of objects from the bucket with the given prefix to a directory.
     * <p>
     * The transfer manager will use '/' as the path delimiter.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * DownloadDirectory myDownload = tm.downloadDirectory(myBucket, myPrefix, Paths.get("/tmp");
     * myDowload.completionFuture().join();
     * }
     * </pre>
     *
     * @param bucket The bucket.
     * @param prefix The prefix.
     * @param destinationDirectory The directory where the objects will be
     * downloaded to.
     */
    DownloadDirectory downloadDirectory(String bucket, String prefix, Path destinationDirectory);

    /**
     * Upload a directory of files to the given S3 bucket and under the given
     * prefix.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * UploadDirectory myUpload = tm.uploadDirectory(myBucket, myPrefix, Paths.get("/path/to/my/directory));
     * myUpload.completionFuture().join();
     * }
     * </pre>
     */
    UploadDirectory uploadDirectory(String bucket, String prefix, Path direcrtory);

    /**
     * Upload a file to S3.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * UploadObject myUpload = tm.uploadObject(myBucket, myKey, Paths.get("myFile.txt"));
     * myUpload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(String bucket, String key, Path file) {
        return upload(UploadObjectRequest.builder()
                        .uploadSpecification(UploadObjectSpecification.fromApiRequest(PutObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()))
                        .build(),
                file);
    }

    /**
     * Upload a file to S3 using the given presigned URL.
     * <p>
     * <b>Usage Example:</b>
     * <pre>
     * {@code
     * Upload myUpload = tm.upload(myPresignedUrl, Paths.get("myFile.txt"));
     * myUpload.completionFuture().join();
     * }
     * </pre>
     */
    default Upload upload(URL presignedUrl, Path file) {
        return upload(UploadObjectRequest.builder()
                        .uploadSpecification(UploadObjectSpecification.fromPresignedUrl(presignedUrl))
                        .build(),
                file);
    }

    /**
     * Upload a file to S3.
     */
    Upload upload(UploadObjectRequest request, Path file);

    /**
     * Resume a previously paused object upload.
     */
    Upload resumeUploadObject(UploadObjectState uploadObjectState);

    /**
     * Create an {@code S3TransferManager} using the default values.
     */
    static S3TransferManager create() {
        return builder().build();
    }

    static S3TransferManager.Builder builder() {
        return ...;
    }

    interface Builder {
        /**
         * The custom S3AsyncClient this transfer manager will use to make calls
         * to S3.
         */
        Builder s3client(S3AsyncClient s3Client);

        /**
         * The max number of requests the Transfer Manager will have at any
         * point in time. This must be less than or equal to the max concurrent
         * setting on the S3 client.
         */
        Builder maxConcurrency(Integer maxConcurrency);

        /**
         * The aggregate max upload rate in bytes per second over all active
         * upload transfers. The default is unlimited.
         */
        Builder maxUploadBytesPerSecond(Long maxUploadBytesPerSecond);

        /**
         * The aggregate max download rate in bytes per second over all active
         * download transfers. The default value is unlimited.
         */
        Builder maxDownloadBytesPerSecond(Long maxDownloadBytesPerSecond);

        /**
         * Add a progress listener to the currently configured list of
         * listeners.
         */
        Builder addProgressListener(TransferProgressListener progressListener);

        /**
         * Set the list of progress listeners, overwriting any currently
         * configured list.
         */
        Builder progressListeners(Collection<? extends TransferProgressListener> progressListeners);

        S3TransferManager build();
    }
}

/**
 * Configuration object for multipart downloads.
 */
public interface MultipartDownloadConfiguration {
    /**
     * Whether multipart downloads are enabled.
     */
    public Boolean enableMultipartDownloads();

    /**
     * The minimum size for an object to be downloaded in multiple parts.
     */
    public Long multipartDownloadThreshold();

    /**
     * The maximum number of parts objects are to be downloaded in.
     */
    public Integer maxDownloadPartCount();

    /**
     * The minimum size for each part.
     */
    public Long minDownloadPartSize();
}

/**
 * Configuration object for multipart uploads.
 */
public final class MultipartUploadConfiguration {
    /**
     * Whether multipart uploads should be enabled.
     */
    public Boolean enableMultipartUploads();

    /**
     * The minimum size for an object to be uploaded in multipe parts.
     */
    public Long multipartUploadThreshold();

    /**
     * The maximum number of perts to upload an object in.
     */
    public Integer maxUploadPartCount();

    /**
     * The minimum size for each uploaded part.
     */
    public Long minUploadPartSize();
}

/**
 * Override configuration for a single transfer.
 */
public interface TransferOverrideConfiguration {
    /**
     * The maximum rate for this transfer in bytes per second.
     */
    Long maxTransferBytesPerSecond();

    /**
     * Override configuration for multipart downloads.
     */
    MultipartDownloadConfiguration multipartDownloadConfiguration();

    /**
     * Override configuration for multipart uploads.
     */
    MultipartUploadConfiguration multipartUploadConfiguration();
}


/**
 * A factory capable of creating the streams for individual parts of a given
 * object to be uploaded to S3.
 * <p>
 * There is no ordering guaranatee for when {@link
 * #streamForPart(PartUploadContext)} is called.
 */
public interface TransferRequestBody {
    /**
     * Return the stream for the object part described by given {@link
     * PartUploadContext}.
     *
     * @param context The context describing the part to be uploaded.
     * @return The part stream.
     */
    Publisher<ByteBuffer> requestBodyForPart(PartUploadContext context);

    /**
     * Return the stream for a entire object to be uploaded as a single part.
     */
    Publisher<ByteBuffer> requestBodyForObject(SinglePartUploadContext context);

    /**
     * Create a factory that creates streams for individual parts of the given
     * file.
     *
     * @param file The file whose parts the factory will create streams for.
     * @return The stream factory.
     */
    static ObjectPartStreamCreator forFile(Path file) {
        return ...;
    }
}

/**
 * A factory capable of creating the {@link AsyncResponseTransformer} to handle
 * each downloaded object part.
 * <p>
 * There is no ordering guarantee for when {@link
 * #transformerForPart(PartDownloadContext)} invocations. It is invoked when the
 * response from S3 is received for the given part.
 */
public interface TransferResponseTransformer {
    /**
     * Return a transformer for downloading a single part of an object.
     */
    AsyncResponseTransformer<GetObjectResponse, ?> transformerForPart(MultipartDownloadContext context);

    /**
     * Return a transformer for downloading an entire object as a single part.
     */
    AsyncResponseTransformer<GetObjectResponse, ?> transformerForObject(SinglePartDownloadContext context)

    /**
     * Return a factory capable of creating transformers that will recombine the
     * object parts to a single file on disk.
     */
    static TransferResponseTransformer forFile(Path file) {
        return ...;
    }
}

/**
 * The context object for the upload of an object part to S3.
 */
public interface MultipartUploadContext {
    /**
     * The original upload request given to the transfer manager.
     */
    UploadObjectRequest uploadRequest();

    /**
     * The request sent to S3 to initiate the multipart upload.
     */
    CreateMultipartUploadRequest createMultipartRequest();

    /**
     * The upload request to be sent to S3 for this part.
     */
    UploadPartRequest uploadPartRequest();

    /**
     * The offset from the beginning of the object where this part begins.
     */
    long partOffset();
}

public interface SinglePartUploadContext {
    /**
     * The original upload request given to the transfer manager.
     */
    UploadObjectRequest uploadRequest();

    /**
     * The request to be sent to S3 to upload the object as a single part.
     */
    PutObjectRequest objectRequest();
}

/**
 * Context object for an individual object part for a multipart download.
 */
public interface MultipartDownloadContext {
    /**
     * The original download request given to the Transfer Manager.
     */
    DownloadObjectRequest downloadRequest();

    /**
     * The part number.
     */
    int partNumber();

    /**
     * The offset from the beginning of the object where this part begins.
     */
    long partOffset();

    /**
     * The size of the part requested in bytes.
     */
    long size();

    /**
     * Whether this is the last part of the object.
     */
    boolean isLastPart();
}

/**
 * Context object for a single part download of an object.
 */
public interface SinglePartDownloadContext {
    /**
     * The original download request given to the Transfer Manager.
     */
    DownloadObjectRequest downloadRequest();

    /**
     * The request sent to S3 for this object. This is empty if downloading a presigned URL.
     */
    GetObjectRequest objectRequest();
}

/**
 * Progress listener for a Transfer.
 * <p>
 * The SDK guarantees that calls to {@link #transferProgressEvent(EventContext)}
 * are externally synchronized.
 */
public interface TransferProgressListener {
    /**
     * Called when a new progress event is available for a Transfer.
     *
     * @param ctx The context object for the given transfer event.
     */
    void transferProgressEvent(EventContext ctx);

    interface EventContext {
        /**
         * The transfer this listener associated with.
         */
        Transfer transfer();
    }

    interface Initiated extends EventContext {
        /**
         * The amount of time that has elapsed since the transfer was
         * initiated.
         */
        Duration elapsedTime();
    }

    interface BytesTransferred extends Initiated {
        /**
         * The transfer request for the object whose bytes were transferred.
         */
        TransferObjectRequest objectRequest();

        /**
         * The number of bytes transferred for this event.
         */
        long bytes();

        /**
         * The total size of the object.
         */
        long size();

        /**
         * If the transfer of the given object is complete.
         */
        boolean complete();
    }

    interface Completed extends Initiated {
    }

    interface Cancelled extends Initiated {
    }

    interface Failed extends Initiated {
        /**
         * The error.
         */
        Throwable error();
    }
}


/**
 * A download transfer of a single object from S3.
 */
public interface Download extends Transfer {
    @Override
    DownloadObjectState pause();
}

/**
 * An upload transfer of a single object to S3.
 */
public interface Upload extends Transfer {
    @Override
    UploadObjectState pause();
}

/**
 * The state of an object download.
 */
public interface DownloadState extends TransferState {
    /**
     * Persist this state so it can later be resumed.
     */
    void persistTo(OutputStream os);

    /**
     * Load a persisted transfer which can then be resumed.
     */
    static DownloadState loadFrom(Inputstream is) {
        ...
    }
}

/**
 * The state of an object upload.
 */
public interface UploadState extends TransferState {
    /**
     * Persist this state so it can later be resumed.
     */
    void persistTo(OutputStream os);

    /**
     * Load a persisted transfer which can then be resumed.
     */
    static UploadState loadFrom(Inputstream is) {
        ...
    }
}

/**
 * Represents the transfer of one or more objects to or from S3.
 */
public interface Transfer {

    CompletableFuture<? extends CompletedTransfer> completionFuture();

    /**
     * Pause this transfer, cancelling any requests in progress.
     * <p>
     * The returned state object can be used to resume this transfer at a later
     * time.
     *
     * @throws IllegalStateException If this transfer is completed or cancelled.
     * @throws UnsupportedOperationException If the transfer does not support
     * pause and resume.
     */
    TransferState pause();
}

public interface CompletedTransfer {
    /**
     * The metrics for this transfer.
     */
    TransferMetrics metrics();
}

/**
 * Metrics for a completed transfer.
 */
public interface TransferMetrics {
    /**
     * The number of milliseconds that elapsed before this transfer completed.
     */
    long elapsedMillis();

    /**
     * The total number of bytes transferred.
     */
    long bytesTransferred();
}


/**
 * A request to download an object. The object to download is specified using
 * the {@link DownloadObjectSpecification} union type.
 */
public class DownloadObjectRequest extends AbstractTransferRequest {
    /**
     * The specification for how to download the object.
     */
    DownloadObjectSpecification downloadSpecification();

    /**
     * The size of the object to be downloaded.
     */
    public Long size();

    public static DownloadObjectRequest forPresignedUrl(URL presignedUrl) {
        ...
    }

    public static DownloadObjectRequest forBucketAndKey(String bucket, String key) {
        ...
    }

    public interface Builder extends AbstractTransferRequest.Builder {
        /**
         * The specification for how to download the object.
         */
        Builder downloadSpecification(DownloadObjectSpecification downloadSpecification);

       /**
         * Set the override configuration for this request.
         */
        @Override
        Builder overrideConfiguration(TransferOverrideConfiguration config);

        /**
         * Set the progress listeners for this request.
         */
        @Override
        Builder progressListeners(Collection<TransferProgressListener> progressListeners);

        /**
         * Add an additional progress listener for this request, appending it to
         * the list of currently configured listeners on this request.
         */
        @Override
        Builder addProgressListener(TransferProgressListener progressListener);

        /**
         * Set the optional size of the object to be downloaded.
         */
        Builder size(Long size);

        DownloadObjectRequest build();
    }
}

/**
 * Union type to contain the different ways to express an object download from
 * S3.
 */
public class DownloadObjectSpecification {
    /**
     * Return this specification as a presigned URL.
     *
     * @throws IllegalStateException If this specifier is not a presigned URL.
     */
    URL asPresignedUrl();

    /**
     * Return this specification as a {@link GetObjectRequest API request}.
     *
     * @throws IllegalStateException If this specifier is not an API request.
     */
    GetObjectRequest asApiRequest();

    /**
     * Returns {@code true} if this is a presigned URL, {@code false} otherwise.
     */
    boolean isPresignedUrl();

    /**
     * Returns {@code true} if this is an API request, {@code false} otherwise.
     */
    boolean isApiRequest();

    /**
     * Create an instance from a presigned URL.
     */
    static DownloadObjectSpecification fromPresignedUrl(URL presignedUrl) {
        ...
    }

    /**
     * Create an instance from an API request.
     */
    static DownloadObjectSpecification fromApiRequest(GetObjectRequest apiRequest) {
        ...
    }
}

/**
 * A request to upload an object. The object to upload is specified using the
 * {@link UploadObjectSpecification} union type.
 */
public class UploadObjectRequest extends AbstractTransferRequest {
    /**
     * The specification for how to upload the object.
     */
    UploadbjectSpecification uploadSpecification();

    /**
     * The size of the object to be uploaded.
     */
    long size();

    public static UploadObjectRequest forPresignedUrl(URL presignedUrl) {
        ...
    }

    public static UploadObjectRequest forBucketAndKey(String bucket, String key) {
        ...
    }

    public interface Builder extends AbstractTransferRequest.Builder {
        /**
         * The specification for how to upload the object.
         */
        Builder uploadSpecification(UploadObjectSpecification uploadSpecification);

       /**
         * Set the override configuration for this request.
         */
        @Override
        Builder overrideConfiguration(TransferOverrideConfiguration config);

        /**
         * Set the progress listeners for this request.
         */
        @Override
        Builder progressListeners(Collection<TransferProgressListener> progressListeners);

        /**
         * Add an additional progress listener for this request, appending it to
         * the list of currently configured listeners on this request.
         */
        @Override
        Builder addProgressListener(TransferProgressListener progressListener);

        UploadObjectRequest build();
    }
}

/**
 * Union type to contain the different ways to express an object upload from
 * S3.
 */
public class UploadObjectSpecification {
    /**
     * Return this specification as a presigned URL.
     *
     * @throws IllegalStateException If this specifier is not a presigned URL.
     */
    URL asPresignedUrl();

    /**
     * Return this specification as a {@link PutObjectRequest API request}.
     *
     * @throws IllegalStateException If this specifier is not an API request.
     */
    PutObjectRequest asApiRequest();

    /**
     * Returns {@code true} if this is a presigned URL, {@code false} otherwise.
     */
    boolean isPresignedUrl();

    /**
     * Returns {@code true} if this is an API request, {@code false} otherwise.
     */
    boolean isApiRequest();

    /**
     * Create an instance from a presigned URL.
     */
    static UploadObjectSpecification fromPresignedUrl(URL presignedUrl) {
        ...
    }

    /**
     * Create an instance from an API request.
     */
    static UploadObjectSpecification fromApiRequest(PutObjectRequest apiRequest) {
        ...
    }
}
