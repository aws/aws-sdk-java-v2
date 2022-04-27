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

import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Creates a copy of an object that is already stored in S3.
 *
 * @see S3TransferManager#copy(CopyRequest)
 */
@SdkPublicApi
@SdkPreviewApi
public final class CopyRequest
    implements TransferObjectRequest,
               ToCopyableBuilder<CopyRequest.Builder, CopyRequest> {

    private final CopyObjectRequest copyObjectRequest;
    private final TransferRequestOverrideConfiguration configuration;

    private CopyRequest(DefaultBuilder builder) {
        this.copyObjectRequest = paramNotNull(builder.copyObjectRequest, "copyObjectRequest");
        this.configuration = builder.configuration;
    }

    /**
     * @return The {@link CopyObjectRequest} request that should be used for the copy
     */
    public CopyObjectRequest copyObjectRequest() {
        return copyObjectRequest;
    }

    /**
     * @return the optional override configuration
     * @see Builder#overrideConfiguration(TransferRequestOverrideConfiguration)
     */
    @Override
    public Optional<TransferRequestOverrideConfiguration> overrideConfiguration() {
        return Optional.ofNullable(configuration);
    }

    /**
     * Create a builder that can be used to create a {@link CopyRequest}.
     *
     * @see S3TransferManager#copy(CopyRequest)
     */
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

        CopyRequest that = (CopyRequest) o;

        if (!Objects.equals(copyObjectRequest, that.copyObjectRequest)) {
            return false;
        }
        return Objects.equals(configuration, that.configuration);
    }

    @Override
    public int hashCode() {
        int result = copyObjectRequest != null ? copyObjectRequest.hashCode() : 0;
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("CopyRequest")
                       .add("copyRequest", copyObjectRequest)
                       .add("configuration", configuration)
                       .build();
    }

    /**
     * A builder for a {@link CopyRequest}, created with {@link #builder()}
     */
    @SdkPublicApi
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, CopyRequest> {

        /**
         * Configure the {@link CopyRequest} that should be used for the copy
         *
         * @param copyRequest the copyRequest
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #copyObjectRequest(Consumer)
         */
        Builder copyObjectRequest(CopyObjectRequest copyRequest);

        /**
         * Configure the {@link CopyRequest} that should be used for the copy
         *
         * <p>
         * This is a convenience method that creates an instance of the {@link CopyRequest} builder avoiding the need to create
         * one manually via {@link CopyRequest#builder()}.
         *
         * @param copyRequestBuilder the copyRequest consumer builder
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see #copyRequest(CopyRequest)
         */
        default Builder copyObjectRequest(Consumer<CopyObjectRequest.Builder> copyRequestBuilder) {
            return copyObjectRequest(CopyObjectRequest.builder()
                                                      .applyMutation(copyRequestBuilder)
                                                      .build());
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
         * {@link TransferRequestOverrideConfiguration.Builder}. This removes the need to call {@link
         * TransferRequestOverrideConfiguration#builder()} and {@link TransferRequestOverrideConfiguration.Builder#build()}.
         *
         * @param configurationBuilder the copy configuration
         * @return this builder for method chaining.
         * @see #overrideConfiguration(TransferRequestOverrideConfiguration)
         */
        default Builder overrideConfiguration(Consumer<TransferRequestOverrideConfiguration.Builder> configurationBuilder) {
            Validate.paramNotNull(configurationBuilder, "configurationBuilder");
            return overrideConfiguration(TransferRequestOverrideConfiguration.builder()
                                                                             .applyMutation(configurationBuilder)
                                                                             .build());
        }

        /**
         * @return The built request.
         */
        @Override
        CopyRequest build();
    }

    private static class DefaultBuilder implements Builder {
        private CopyObjectRequest copyObjectRequest;
        private TransferRequestOverrideConfiguration configuration;

        private DefaultBuilder() {
        }

        private DefaultBuilder(CopyRequest copyRequest) {
            this.copyObjectRequest = copyRequest.copyObjectRequest;
            this.configuration = copyRequest.configuration;
        }

        @Override
        public Builder copyObjectRequest(CopyObjectRequest copyRequest) {
            this.copyObjectRequest = copyRequest;
            return this;
        }

        public CopyObjectRequest getCopyObjectRequest() {
            return copyObjectRequest;
        }

        public void setCopyObjectRequest(CopyObjectRequest copyObjectRequest) {
            copyObjectRequest(copyObjectRequest);
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
        public CopyRequest build() {
            return new CopyRequest(this);
        }
    }
}
