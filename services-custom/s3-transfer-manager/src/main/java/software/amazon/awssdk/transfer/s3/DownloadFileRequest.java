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

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Download an object identified by the bucket and key from S3 to a local file. For non-file-based downloads, you may use {@link
 * DownloadRequest} instead.
 *
 * @see S3TransferManager#downloadFile(DownloadFileRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class DownloadFileRequest
    implements TransferObjectRequest, ToCopyableBuilder<DownloadFileRequest.Builder, DownloadFileRequest> {

    private final Path destination;
    private final GetObjectRequest getObjectRequest;
    private final TransferRequestOverrideConfiguration configuration;

    private DownloadFileRequest(DefaultBuilder builder) {
        this.destination = Validate.paramNotNull(builder.destination, "destination");
        this.getObjectRequest = Validate.paramNotNull(builder.getObjectRequest, "getObjectRequest");
        this.configuration = builder.configuration;
    }

    /**
     * Create a builder that can be used to create a {@link DownloadFileRequest}.
     *
     * @see S3TransferManager#downloadFile(DownloadFileRequest)
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }
    
    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    /**
     * The {@link Path} to file that response contents will be written to. The file must not exist or this method
     * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
     *
     * @return the destination path
     */
    public Path destination() {
        return destination;
    }

    /**
     * @return The {@link GetObjectRequest} request that should be used for the download
     */
    public GetObjectRequest getObjectRequest() {
        return getObjectRequest;
    }

    /**
     * @return the optional override configuration
     * @see Builder#overrideConfiguration(TransferRequestOverrideConfiguration)
     */
    @Override
    public Optional<TransferRequestOverrideConfiguration> overrideConfiguration() {
        return Optional.ofNullable(configuration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DownloadFileRequest that = (DownloadFileRequest) o;

        if (!Objects.equals(destination, that.destination)) {
            return false;
        }
        if (!Objects.equals(getObjectRequest, that.getObjectRequest)) {
            return false;
        }
        return Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        int result = destination != null ? destination.hashCode() : 0;
        result = 31 * result + (getObjectRequest != null ? getObjectRequest.hashCode() : 0);
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("DownloadFileRequest")
                       .add("destination", destination)
                       .add("getObjectRequest", getObjectRequest)
                       .add("configuration", configuration)
                       .build();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    /**
     * A builder for a {@link DownloadFileRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, DownloadFileRequest> {

        /**
         * The {@link Path} to file that response contents will be written to. The file must not exist or this method
         * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
         *
         * @param destination the destination path
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder destination(Path destination);

        /**
         * The file that response contents will be written to. The file must not exist or this method
         * will throw an exception. If the file is not writable by the current user then an exception will be thrown.
         *
         * @param destination the destination path
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        default Builder destination(File destination) {
            Validate.paramNotNull(destination, "destination");
            return destination(destination.toPath());
        }

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         *
         * @param getObjectRequest the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(Consumer)
         */
        Builder getObjectRequest(GetObjectRequest getObjectRequest);

        /**
         * The {@link GetObjectRequest} request that should be used for the download
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link GetObjectRequest} builder avoiding the
         * need to create one manually via {@link GetObjectRequest#builder()}.
         *
         * @param getObjectRequestBuilder the getObject request
         * @return a reference to this object so that method calls can be chained together.
         * @see #getObjectRequest(GetObjectRequest)
         */
        default Builder getObjectRequest(Consumer<GetObjectRequest.Builder> getObjectRequestBuilder) {
            GetObjectRequest request = GetObjectRequest.builder()
                                                       .applyMutation(getObjectRequestBuilder)
                                                       .build();
            getObjectRequest(request);
            return this;
        }

        /**
         * Add an optional request override configuration.
         *
         * @param configuration The override configuration.
         * @return This builder for method chaining.
         */
        Builder overrideConfiguration(TransferRequestOverrideConfiguration configuration);

        /**
         * Similar to {@link #overrideConfiguration(TransferRequestOverrideConfiguration)}, but takes a lambda to configure a new
         * {@link TransferRequestOverrideConfiguration.Builder}. This removes the need to call
         * {@link TransferRequestOverrideConfiguration#builder()} and
         * {@link TransferRequestOverrideConfiguration.Builder#build()}.
         *
         * @param configurationBuilder the upload configuration
         * @return this builder for method chaining.
         * @see #overrideConfiguration(TransferRequestOverrideConfiguration)
         */
        default Builder overrideConfiguration(Consumer<TransferRequestOverrideConfiguration.Builder> configurationBuilder) {
            Validate.paramNotNull(configurationBuilder, "configurationBuilder");
            return overrideConfiguration(TransferRequestOverrideConfiguration.builder()
                                                                             .applyMutation(configurationBuilder)
                                                                             .build());
        }
    }

    private static final class DefaultBuilder implements Builder {
        private Path destination;
        private GetObjectRequest getObjectRequest;
        private TransferRequestOverrideConfiguration configuration;

        private DefaultBuilder() {
        }

        private DefaultBuilder(DownloadFileRequest downloadFileRequest) {
            this.destination = downloadFileRequest.destination;
            this.getObjectRequest = downloadFileRequest.getObjectRequest;
            this.configuration = downloadFileRequest.configuration;
        }

        @Override
        public Builder destination(Path destination) {
            this.destination = Validate.paramNotNull(destination, "destination");
            return this;
        }

        public Path getDestination() {
            return destination;
        }

        public void setDestination(Path destination) {
            destination(destination);
        }

        @Override
        public Builder getObjectRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        public GetObjectRequest getGetObjectRequest() {
            return getObjectRequest;
        }

        public void setGetObjectRequest(GetObjectRequest getObjectRequest) {
            getObjectRequest(getObjectRequest);
        }

        @Override
        public Builder overrideConfiguration(TransferRequestOverrideConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public void setOverrideConfiguration(TransferRequestOverrideConfiguration configuration) {
            overrideConfiguration(configuration);
        }

        public TransferRequestOverrideConfiguration getOverrideConfiguration() {
            return configuration;
        }

        @Override
        public DownloadFileRequest build() {
            return new DownloadFileRequest(this);
        }
    }
}
