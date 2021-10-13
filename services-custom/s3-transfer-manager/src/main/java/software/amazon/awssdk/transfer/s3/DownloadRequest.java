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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
 * Request object to download an object from S3 using the Transfer Manager.
 */
@SdkPublicApi
@SdkPreviewApi
public final class DownloadRequest implements TransferRequest, ToCopyableBuilder<DownloadRequest.Builder, DownloadRequest> {
    private final Path destination;
    private final GetObjectRequest getObjectRequest;
    private final List<TransferListener> listeners;

    private DownloadRequest(BuilderImpl builder) {
        this.destination = Validate.paramNotNull(builder.destination, "destination");
        this.getObjectRequest = Validate.paramNotNull(builder.getObjectRequest, "getObjectRequest");
        this.listeners = builder.listeners != null ? builder.listeners : Collections.emptyList();
    }

    /**
     * Create a builder that can be used to create a {@link DownloadRequest}.
     *
     * @see S3TransferManager#download(DownloadRequest)
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl();
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
     * @return The {@link TransferListener}s associated with this download
     */
    public List<TransferListener> listeners() {
        return listeners;
    }

    @Override
    public String toString() {
        return ToString.builder("DownloadRequest")
                       .add("destination", destination)
                       .add("getObjectRequest", getObjectRequest)
                       .add("listeners", listeners)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DownloadRequest that = (DownloadRequest) o;

        if (!Objects.equals(destination, that.destination)) {
            return false;
        }
        if (!Objects.equals(getObjectRequest, that.getObjectRequest)) {
            return false;
        }
        return Objects.equals(listeners, that.listeners);
    }

    @Override
    public int hashCode() {
        int result = destination != null ? destination.hashCode() : 0;
        result = 31 * result + (getObjectRequest != null ? getObjectRequest.hashCode() : 0);
        result = 31 * result + (listeners != null ? listeners.hashCode() : 0);
        return result;
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return BuilderImpl.class;
    }

    /**
     * A builder for a {@link DownloadRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends TransferRequest.Builder<DownloadRequest, Builder>, CopyableBuilder<Builder,
        DownloadRequest> {

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
         * The {@link TransferListener}s that will be notified as part of this request.
         * See the {@link TransferListener} documentation for usage instructions.
         *
         * @param listeners the collection of listeners
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listeners(Collection<TransferListener> listeners);

        /**
         * The {@link TransferListener}s that will be notified as part of this request.
         * See the {@link TransferListener} documentation for usage instructions.
         *
         * @param listeners the variable amount of listeners
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        Builder listeners(TransferListener... listeners);

        /**
         * @return The built request.
         */
        @Override
        DownloadRequest build();
    }

    private static final class BuilderImpl implements Builder {
        private Path destination;
        private GetObjectRequest getObjectRequest;
        private List<TransferListener> listeners;

        private BuilderImpl() {
        }

        @Override
        public Builder destination(Path destination) {
            this.destination = destination;
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
        public Builder listeners(Collection<TransferListener> listeners) {
            this.listeners = Collections.unmodifiableList(new ArrayList<>(listeners));
            return this;
        }

        @Override
        public Builder listeners(TransferListener... listeners) {
            this.listeners = Collections.unmodifiableList(Arrays.asList(listeners));
            return this;
        }

        @Override
        public DownloadRequest build() {
            return new DownloadRequest(this);
        }
    }
}
