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


import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request object to download the objects in the provided S3 bucket to a local directory using the Transfer Manager.
 *
 * @see S3TransferManager#downloadDirectory(DownloadDirectoryRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class DownloadDirectoryRequest
    implements TransferDirectoryRequest, ToCopyableBuilder<DownloadDirectoryRequest.Builder, DownloadDirectoryRequest> {

    private final Path destinationDirectory;
    private final String bucket;
    private final String prefix;
    private final String delimiter;
    private final DownloadFilter filter;
    private final Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer;
    private final Consumer<ListObjectsV2Request.Builder> listObjectsRequestTransformer;

    public DownloadDirectoryRequest(DefaultBuilder builder) {
        this.destinationDirectory = Validate.paramNotNull(builder.destinationDirectory, "destinationDirectory");
        this.bucket = Validate.paramNotNull(builder.bucket, "bucket");
        this.prefix = builder.prefix;
        this.delimiter = builder.delimiter;
        this.filter = builder.filter;
        this.downloadFileRequestTransformer = builder.downloadFileRequestTransformer;
        this.listObjectsRequestTransformer = builder.listObjectsRequestTransformer;
    }

    /**
     * The destination directory to which files should be downloaded.
     *
     * @return the destination directory
     * @see Builder#destinationDirectory(Path)
     */
    public Path destinationDirectory() {
        return destinationDirectory;
    }

    /**
     * The name of the bucket
     *
     * @return bucket name
     * @see Builder#bucket(String)
     */
    public String bucket() {
        return bucket;
    }

    /**
     * @return the optional key prefix
     * @see Builder#prefix(String)
     */
    public Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    /**
     * @return the optional delimiter
     * @see Builder#delimiter(String)
     */
    public Optional<String> delimiter() {
        return Optional.ofNullable(delimiter);
    }

    /**
     * @return the optional filter, or {@link DownloadFilter#allObjects()} if no filter was provided
     * @see Builder#filter(DownloadFilter)
     */
    public DownloadFilter filter() {
        return filter == null ? DownloadFilter.allObjects() : filter;
    }

    /**
     * @return the {@link ListObjectsV2Request} transformer if not null, otherwise no-op
     * @see Builder#listObjectsV2RequestTransformer(Consumer)
     */
    public Consumer<ListObjectsV2Request.Builder> listObjectsRequestTransformer() {
        return listObjectsRequestTransformer == null ? ignore -> { } : listObjectsRequestTransformer;
    }

    /**
     * @return the upload request transformer if not null, otherwise no-op
     * @see Builder#listObjectsV2RequestTransformer(Consumer)
     */
    public Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer() {
        return downloadFileRequestTransformer == null ? ignore -> { } : downloadFileRequestTransformer;
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DownloadDirectoryRequest that = (DownloadDirectoryRequest) o;

        if (!Objects.equals(destinationDirectory, that.destinationDirectory)) {
            return false;
        }
        if (!Objects.equals(bucket, that.bucket)) {
            return false;
        }
        if (!Objects.equals(prefix, that.prefix)) {
            return false;
        }
        if (!Objects.equals(delimiter, that.delimiter)) {
            return false;
        }
        if (!Objects.equals(downloadFileRequestTransformer, that.downloadFileRequestTransformer)) {
            return false;
        }
        if (!Objects.equals(listObjectsRequestTransformer, that.listObjectsRequestTransformer)) {
            return false;
        }
        return Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode() {
        int result = destinationDirectory != null ? destinationDirectory.hashCode() : 0;
        result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (delimiter != null ? delimiter.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (downloadFileRequestTransformer != null ? downloadFileRequestTransformer.hashCode() : 0);
        result = 31 * result + (listObjectsRequestTransformer != null ? listObjectsRequestTransformer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DownloadDirectoryRequest")
                       .add("destinationDirectory", destinationDirectory)
                       .add("bucket", bucket)
                       .add("prefix", prefix)
                       .add("delimiter", delimiter)
                       .add("filter", filter)
                       .add("downloadFileRequestTransformer", downloadFileRequestTransformer)
                       .add("listObjectsRequestTransformer", listObjectsRequestTransformer)
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, DownloadDirectoryRequest> {
        /**
         * Specify the destination directory to which files should be downloaded.
         *
         * @param destinationDirectory the destination directory
         * @return This builder for method chaining.
         */
        Builder destinationDirectory(Path destinationDirectory);

        /**
         * The name of the bucket to download objects from.
         *
         * @param bucket the bucket name
         * @return This builder for method chaining.
         */
        Builder bucket(String bucket);

        /**
         * Specify the key prefix for the virtual directory. If not provided, all subdirectories will be downloaded recursively
         * <p>
         * See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * <p>
         * When a non-empty prefix is provided, the prefix is stripped from the directory structure of the files.
         * <p>
         * For example, assume that you have the following keys in your bucket:
         * <ul>
         *     <li>sample.jpg</li>
         *     <li>photos/2022/January/sample.jpg</li>
         *     <li>photos/2022/February/sample1.jpg</li>
         *     <li>photos/2022/February/sample2.jpg</li>
         *     <li>photos/2022/February/sample3.jpg</li>
         * </ul>
         *
         * Give a request to download the bucket to a destination with a prefix of "/photos" and destination path of "test", the
         * downloaded directory would like this
         *
         * <pre>
         *   {@code
         *      |- test
         *         |- 2022
         *              |- January
         *                 |- sample.jpg
         *              |- February
         *                 |- sample1.jpg
         *                 |- sample2.jpg
         *                 |- sample3.jpg
         *   }
         * </pre>
         * @param prefix the key prefix
         * @return This builder for method chaining.
         */
        Builder prefix(String prefix);

        /**
         * Specify the delimiter that will be used to retrieve the objects within the provided bucket. A delimiter causes a list
         * operation to roll up all the keys that share a common prefix into a single summary list result. It's null by default.
         *
         * For example, assume that you have the following keys in your bucket:
         *
         * <ul>
         *     <li>sample.jpg</li>
         *     <li>photos-2022-January-sample.jpg</li>
         *     <li>photos-2022-February-sample1.jpg</li>
         *     <li>photos-2022-February-sample2.jpg</li>
         *     <li>photos-2022-February-sample3.jpg</li>
         * </ul>
         *
         * Give a request to download the bucket to a destination with delimiter of "-", the downloaded directory would look
         * like this
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
         *
         * <p>
         * See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * @param delimiter the delimiter
         * @return This builder for method chaining.
         * @see #prefix(String)
         */
        Builder delimiter(String delimiter);

        /**
         * Specify a filter that will be used to evaluate which objects should be downloaded from the target directory.
         * <p>
         * You can use a filter, for example, to only download objects of a given size, of a given file extension, of a given
         * last-modified date, etc. See {@link DownloadFilter} for some ready-made implementations. Multiple {@link
         * DownloadFilter}s can be composed together via the {@code and} and {@code or} methods.
         * <p>
         * By default, if no filter is specified, all objects will be downloaded.
         *
         * @param filter the filter
         * @return This builder for method chaining.
         * @see DownloadFilter
         */
        Builder filter(DownloadFilter filter);

        /**
         * Specify a function used to transform the {@link DownloadFileRequest}s generated by this
         * {@link DownloadDirectoryRequest}. The provided function is called once for each file that is downloaded, allowing
         * you to modify the paths resolved by TransferManager on a per-file basis, modify the created {@link GetObjectRequest}
         * before it is passed to S3, or configure a {@link TransferRequestOverrideConfiguration}.
         *
         * <p>The factory receives the {@link DownloadFileRequest}s created by Transfer Manager for each S3 Object in the
         * S3 bucket being downloaded and returns a (potentially modified) {@code DownloadFileRequest}.
         *
         * <p>
         * <b>Usage Example:</b>
         * <pre>
         * {@code
         * // Add a LoggingTransferListener to every transfer within the download directory request
         * TransferRequestOverrideConfiguration fileDownloadConfiguration =
         *     TransferRequestOverrideConfiguration.builder()
         *         .addListener(LoggingTransferListener.create())
         *         .build();
         *
         * DownloadDirectoryRequest request =
         *     DownloadDirectoryRequest.builder()
         *         .destinationDirectory(Paths.get("."))
         *         .bucket("bucket")
         *         .prefix("prefix")
         *         .downloadFileRequestTransformer(request -> request.overrideConfiguration(fileDownloadConfiguration))
         *         .build()
         *
         * DownloadDirectoryTransfer downloadDirectory = transferManager.downloadDirectory(request);
         *
         * // Wait for the transfer to complete
         * CompletedDownloadDirectory completedDownloadDirectory = downloadDirectory.completionFuture().join();
         *
         * // Print out the failed downloads
         * completedDownloadDirectory.failedDownloads().forEach(System.out::println);
         * }
         * </pre>
         *
         * @param downloadFileRequestTransformer A transformer to use for modifying the file-level download requests
         *                                       before execution
         * @return This builder for method chaining
         */
        Builder downloadFileRequestTransformer(Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer);

        /**
         * Specify a function used to transform the {@link ListObjectsV2Request}s generated by this
         * {@link DownloadDirectoryRequest}. The provided function is called once, allowing you to modify
         * {@link ListObjectsV2Request} before it is passed to S3.
         *
         * <p>The factory receives the {@link ListObjectsV2Request}s created by Transfer Manager and
         * returns a (potentially modified) {@code ListObjectsV2Request}.
         *
         * <p>
         * <b>Usage Example:</b>
         * <pre>
         * {@code
         *
         * DownloadDirectoryRequest request =
         *     DownloadDirectoryRequest.builder()
         *         .destinationDirectory(Paths.get("."))
         *         .bucket("bucket")
         *         .prefix("prefix")
         *         .listObjectsRequestTransformer(request -> request.encodingType(newEncodingType))
         *         .build()
         *
         * DownloadDirectoryTransfer downloadDirectory = transferManager.downloadDirectory(request);
         *
         * // Wait for the transfer to complete
         * CompletedDownloadDirectory completedDownloadDirectory = downloadDirectory.completionFuture().join();
         *
         * // Print out the failed downloads
         * completedDownloadDirectory.failedDownloads().forEach(System.out::println);
         * }
         * </pre>
         *
         * @param listObjectsV2RequestTransformer A transformer to use for modifying ListObjectsV2Request before execution
         * @return This builder for method chaining
         */
        Builder listObjectsV2RequestTransformer(Consumer<ListObjectsV2Request.Builder> listObjectsV2RequestTransformer);

    }

    private static final class DefaultBuilder implements Builder {

        private Path destinationDirectory;
        private String bucket;
        private String prefix;
        private String delimiter;
        private DownloadFilter filter;
        private Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer;
        private Consumer<ListObjectsV2Request.Builder> listObjectsRequestTransformer;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DownloadDirectoryRequest request) {
            this.destinationDirectory = request.destinationDirectory;
            this.bucket = request.bucket;
            this.prefix = request.prefix;
            this.filter = request.filter;
            this.downloadFileRequestTransformer = request.downloadFileRequestTransformer;
            this.listObjectsRequestTransformer = request.listObjectsRequestTransformer;
        }

        @Override
        public Builder destinationDirectory(Path destinationDirectory) {
            this.destinationDirectory = destinationDirectory;
            return this;
        }

        public void setDestinationDirectory(Path destinationDirectory) {
            destinationDirectory(destinationDirectory);
        }

        public Path getDestinationDirectory() {
            return destinationDirectory;
        }

        @Override
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public void setBucket(String bucket) {
            bucket(bucket);
        }

        public String getBucket() {
            return bucket;
        }

        @Override
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public void setPrefix(String prefix) {
            prefix(prefix);
        }

        public String getPrefix() {
            return prefix;
        }

        @Override
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public void setDelimiter(String delimiter) {
            delimiter(delimiter);
        }

        public String getDelimiter() {
            return delimiter;
        }

        @Override
        public Builder filter(DownloadFilter filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public Builder downloadFileRequestTransformer(Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer) {
            this.downloadFileRequestTransformer = downloadFileRequestTransformer;
            return this;
        }

        @Override
        public Builder listObjectsV2RequestTransformer(Consumer<ListObjectsV2Request.Builder> listObjectsRequestTransformer) {
            this.listObjectsRequestTransformer = listObjectsRequestTransformer;
            return this;
        }

        public void setFilter(DownloadFilter filter) {
            filter(filter);
        }

        public DownloadFilter getFilter() {
            return filter;
        }

        @Override
        public DownloadDirectoryRequest build() {
            return new DownloadDirectoryRequest(this);
        }
    }
}
