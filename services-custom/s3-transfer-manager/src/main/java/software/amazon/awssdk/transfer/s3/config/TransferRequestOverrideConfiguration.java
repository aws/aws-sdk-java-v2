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

package software.amazon.awssdk.transfer.s3.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link UploadFileRequest} and {@link DownloadFileRequest}. All values are optional, and not
 * specifying them will use the SDK default values.
 *
 * <p>Use {@link #builder()} to create a set of options.
 */
@SdkPublicApi
@SdkPreviewApi
public final class TransferRequestOverrideConfiguration
    implements ToCopyableBuilder<TransferRequestOverrideConfiguration.Builder, TransferRequestOverrideConfiguration> {

    private final List<TransferListener> listeners;

    public TransferRequestOverrideConfiguration(DefaultBuilder builder) {
        this.listeners = builder.listeners != null ? Collections.unmodifiableList(builder.listeners) : Collections.emptyList();
    }

    /**
     * @return The {@link TransferListener}s that will be notified as part of this request.
     */
    public List<TransferListener> listeners() {
        return listeners;
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

        TransferRequestOverrideConfiguration that = (TransferRequestOverrideConfiguration) o;

        return Objects.equals(listeners, that.listeners);
    }

    @Override
    public int hashCode() {
        return listeners != null ? listeners.hashCode() : 0;
    }

    @Override
    public String toString() {
        return ToString.builder("TransferRequestOverrideConfiguration")
                       .add("listeners", listeners)
                       .build();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    public static Class<? extends Builder> serializableBuilderClass() {
        return DefaultBuilder.class;
    }

    public interface Builder extends CopyableBuilder<Builder, TransferRequestOverrideConfiguration> {

        /**
         * The {@link TransferListener}s that will be notified as part of this request. This method overrides and replaces any
         * transferListeners that have already been set. Add an optional request override configuration.
         *
         * @param transferListeners     the collection of transferListeners
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        Builder transferListeners(Collection<TransferListener> transferListeners);

        /**
         * Add a {@link TransferListener} that will be notified as part of this request.
         *
         * @param transferListener the transferListener to add
         * @return Returns a reference to this object so that method calls can be chained together.
         * @see TransferListener
         */
        Builder addTransferListener(TransferListener transferListener);

        @Override
        TransferRequestOverrideConfiguration build();
    }

    private static final class DefaultBuilder implements Builder {
        private List<TransferListener> listeners;

        private DefaultBuilder(TransferRequestOverrideConfiguration configuration) {
            this.listeners = configuration.listeners;
        }

        private DefaultBuilder() {
        }

        @Override
        public Builder transferListeners(Collection<TransferListener> transferListeners) {
            this.listeners = transferListeners != null ? new ArrayList<>(transferListeners) : null;
            return this;
        }

        @Override
        public Builder addTransferListener(TransferListener transferListener) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(transferListener);
            return this;
        }

        public List<TransferListener> getListeners() {
            return listeners;
        }

        public void setListeners(Collection<TransferListener> listeners) {
            transferListeners(listeners);
        }

        @Override
        public TransferRequestOverrideConfiguration build() {
            return new TransferRequestOverrideConfiguration(this);
        }
    }
}
