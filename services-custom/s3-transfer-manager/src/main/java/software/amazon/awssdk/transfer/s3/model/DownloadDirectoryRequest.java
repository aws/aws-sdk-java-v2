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

package software.amazon.awssdk.transfer.s3.model;


import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.config.DownloadFilter;
import software.amazon.awssdk.transfer.s3.config.TransferRequestOverrideConfiguration;
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
public final class DownloadDirectoryRequest
    implements TransferDirectoryRequest, ToCopyableBuilder<DownloadDirectoryRequest.Builder, DownloadDirectoryRequest> {

    private final Path destination;
    private final String bucket;
    private final DownloadFilter filter;
    private final Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer;
    private final Consumer<ListObjectsV2Request.Builder> listObjectsRequestTransformer;

    public DownloadDirectoryRequest(DefaultBuilder builder) {
        this.destination = Validate.paramNotNull(builder.destination, "destination");
        this.bucket = Validate.paramNotNull(builder.bucket, "bucket");
        this.filter = builder.filter;
        this.downloadFileRequestTransformer = builder.downloadFileRequestTransformer;
        this.listObjectsRequestTransformer = builder.listObjectsRequestTransformer;
    }

    /**
     * The destination directory to which files should be downloaded.
     *
     * @return the destination directory
     * @see Builder#destination(Path)
     */
    public Path destination() {
        return destination;
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

        if (!Objects.equals(destination, that.destination)) {
            return false;
        }
        if (!Objects.equals(bucket, that.bucket)) {
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
        int result = destination != null ? destination.hashCode() : 0;
        result = 31 * result + (bucket != null ? bucket.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (downloadFileRequestTransformer != null ? downloadFileRequestTransformer.hashCode() : 0);
        result = 31 * result + (listObjectsRequestTransformer != null ? listObjectsRequestTransformer.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DownloadDirectoryRequest")
                       .add("destination", destination)
                       .add("bucket", bucket)
                       .add("filter", filter)
                       .add("downloadFileRequestTransformer", downloadFileRequestTransformer)
                       .add("listObjectsRequestTransformer", listObjectsRequestTransformer)
                       .build();
    }

    public interface Builder extends CopyableBuilder<Builder, DownloadDirectoryRequest> {
        /**
         * Specifies the destination directory to which files should be downloaded.
         *
         * @param destination the destination directorÏy
         * @return This builder for method chaining.
         */
        Builder destination(Path destination);

        /**
         * The name of the bucket to download objects from.
         *
         * @param bucket the bucket name
         * @return This builder for method chaining.
         */
        Builder bucket(String bucket);

        /**
         * Specifies a filter that will be used to evaluate which objects should be downloaded from the target directory.
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
         * Specifies a function used to transform the {@link DownloadFileRequest}s generated by this
         * {@link DownloadDirectoryRequest}. The provided function is called once for each file that is downloaded, allowing
         * you to modify the paths resolved by TransferManager on a per-file basis, modify the created {@link GetObjectRequest}
         * before it is passed to S3, or configure a {@link TransferRequestOverrideConfiguration}.
         *
         * <p>The factory receives the {@link DownloadFileRequest}s created by Transfer Manager for each S3 Object in the
         * S3 bucket being downloaded and returns a (potentially modified) {@code DownloadFileRequest}.
         *
         * <p>
         * <b>Usage Example:</b>
         * {@snippet :
         * // Add a LoggingTransferListener to every transfer within the download directory request
         *
         * DownloadDirectoryRequest request =
         *     DownloadDirectoryRequest.builder()
         *         .destination(Paths.get("."))
         *         .bucket("bucket")
         *         .downloadFileRequestTransformer(request -> request.addTransferListener(LoggingTransferListener.create()))
         *         .build();
         *
         * DownloadDirectoryTransfer downloadDirectory = transferManager.downloadDirectory(request);
         *
         * // Wait for the transfer to complete
         * CompletedDownloadDirectory completedDownloadDirectory = downloadDirectory.completionFuture().join();
         *
         * // Print out the failed downloads
         * completedDownloadDirectory.failedDownloads().forEach(System.out::println);
         * }
         *
         * @param downloadFileRequestTransformer A transformer to use for modifying the file-level download requests
         *                                       before execution
         * @return This builder for method chaining
         */
        Builder downloadFileRequestTransformer(Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer);

        /**
         * Specifies a function used to transform the {@link ListObjectsV2Request}s generated by this
         * {@link DownloadDirectoryRequest}. The provided function is called once, allowing you to modify
         * {@link ListObjectsV2Request} before it is passed to S3.
         *
         * <p>The factory receives the {@link ListObjectsV2Request}s created by Transfer Manager and
         * returns a (potentially modified) {@code ListObjectsV2Request}.
         *
         * <p>
         * <b>Usage Example:</b>
         * {@snippet :
         *
         * DownloadDirectoryRequest request =
         *     DownloadDirectoryRequest.builder()
         *         .destination(Paths.get("."))
         *         .bucket("bucket")
         *         .listObjectsV2RequestTransformer(request -> request.encodingType(newEncodingType))
         *         .build();
         *
         * DownloadDirectoryTransfer downloadDirectory = transferManager.downloadDirectory(request);
         *
         * // Wait for the transfer to complete
         * CompletedDownloadDirectory completedDownloadDirectory = downloadDirectory.completionFuture().join();
         *
         * // Print out the failed downloads
         * completedDownloadDirectory.failedDownloads().forEach(System.out::println);
         * }
         *
         * <p>
         * <b>Prefix:</b>
         * {@code ListObjectsV2Request}'s {@code prefix} specifies the key prefix for the virtual directory. If not provided,
         * all subdirectories will be downloaded recursively
         * <p>
         * See <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-prefixes.html">Organizing objects using
         * prefixes</a>
         *
         * <p>
         * When a non-empty prefix is provided, Transfer Manager strips it from each key to compute the file's path under the
         * destination directory. The delimiter used for this is the {@code delimiter} of the {@code ListObjectsV2Request}, or
         * {@code "/"} if none is set. The prefix is stripped as follows:
         * <ul>
         *     <li>A key that doesn't contain the delimiter anywhere keeps its full name, including the prefix.</li>
         *     <li>Any other key has the prefix removed from its start. If the prefix doesn't end with the delimiter and the
         *     rest of the key starts with one, that one delimiter is removed too.</li>
         * </ul>
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
         * Given a request to download the bucket to a destination with a prefix of "photos/" and destination path of "test", the
         * downloaded directory would look like this
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
         *
         * <p>
         * A prefix that doesn't end with the delimiter is stripped only from keys that contain the delimiter, and it can cut
         * into the first path segment. For example, with the prefix {@code "top-"} and the default delimiter:
         * <ul>
         *     <li>{@code top-level/sub/1.txt} is downloaded to {@code level/sub/1.txt}</li>
         *     <li>{@code top-/1.txt} is downloaded to {@code 1.txt}</li>
         *     <li>{@code top-file.txt} is downloaded to {@code top-file.txt}, because the key doesn't contain the delimiter</li>
         * </ul>
         * <p>
         * The delimiter check covers the whole key, including the prefix, so a prefix that contains the delimiter is stripped
         * from every key. With the prefix {@code "data/top-"}, {@code data/top-file.txt} is downloaded to {@code file.txt}.
         * <p>
         * With a prefix that doesn't end with the delimiter, two keys can map to the same local file. For example, with the
         * prefix {@code "abc"}, both {@code abc/def/x.txt} and {@code abcdef/x.txt} map to {@code def/x.txt}, and both
         * {@code abc/abcd} and {@code abcd} map to {@code abcd}. We recommend using a prefix that ends with the delimiter, such
         * as {@code "photos/"}. Every key listed under such a prefix contains the delimiter, so the prefix is stripped from all
         * of them in the same way.
         * <p>
         * A prefix that ends with the delimiter doesn't prevent every conflict. Under any prefix, or with no prefix:
         * <ul>
         *     <li>Keys that differ only in empty or dot segments, such as {@code photos/a//b.jpg}, {@code photos/a/./b.jpg} and
         *     {@code photos/a/b.jpg}, map to the same local file.</li>
         *     <li>A key whose local path resolves outside the destination directory is not downloaded. For example, with the
         *     prefix {@code "photos/"}, the key {@code photos//x.jpg} maps to {@code /x.jpg}.</li>
         * </ul>
         * <p>
         * When two keys map to the same local file, the downloads overwrite each other, and the conflict isn't reported in
         * {@link CompletedDirectoryDownload#failedTransfers()}.
         * <p>
         * To choose different local paths, use {@link #downloadFileRequestTransformer(Consumer)}. Transfer Manager works with
         * the path it computed, not the destination set by the transformer: it refuses a computed path outside the destination
         * directory, and it creates only the computed path's parent directories. Directories for the transformer's destination
         * must already exist, and keeping destinations distinct is up to the transformer.
         *
         * <p>
         * <b>Delimiter:</b>
         * {@code ListObjectsV2Request}'s {@code delimiter} specifies the delimiter that will be used to retrieve the objects
         * within the provided bucket. A delimiter causes a list operation to roll up all the keys that share a common prefix
         * into a single summary list result. It's null by default, in which case Transfer Manager uses {@code "/"} when it
         * strips the prefix and maps keys to local paths. Each occurrence of the delimiter in the stripped key is replaced
         * with the file system's separator. Only the delimiter is replaced, so with a delimiter other than {@code "/"}, a
         * {@code "/"} in a key still creates a subdirectory on Unix-like systems.
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
         * Given a request to download the bucket to a destination with delimiter of "-", the downloaded directory would look
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
         * @param listObjectsV2RequestTransformer A transformer to use for modifying ListObjectsV2Request before execution
         * @return This builder for method chaining
         */
        Builder listObjectsV2RequestTransformer(Consumer<ListObjectsV2Request.Builder> listObjectsV2RequestTransformer);

    }

    private static final class DefaultBuilder implements Builder {

        private Path destination;
        private String bucket;
        private DownloadFilter filter;
        private Consumer<DownloadFileRequest.Builder> downloadFileRequestTransformer;
        private Consumer<ListObjectsV2Request.Builder> listObjectsRequestTransformer;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DownloadDirectoryRequest request) {
            this.destination = request.destination;
            this.bucket = request.bucket;
            this.filter = request.filter;
            this.downloadFileRequestTransformer = request.downloadFileRequestTransformer;
            this.listObjectsRequestTransformer = request.listObjectsRequestTransformer;
        }

        @Override
        public Builder destination(Path destination) {
            this.destination = destination;
            return this;
        }

        public void setDestination(Path destination) {
            destination(destination);
        }

        public Path getDestination() {
            return destination;
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
