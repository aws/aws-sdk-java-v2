/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.custom.s3.transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Base class for all transfer requests.
 */
@SdkPublicApi
public abstract class TransferRequest implements ToCopyableBuilder<TransferRequest.Builder, TransferRequest> {
    private TransferOverrideConfiguration overrideConfiguration;
    private List<TransferProgressListener> progressListeners;

    protected TransferRequest(BuilderImpl builder) {
        this.overrideConfiguration = builder.overrideConfiguration;
        this.progressListeners = builder.progressListeners;
    }

    /**
     * @return The optional override configuration for this request.
     */
    public Optional<TransferOverrideConfiguration>  overrideConfiguration() {
        return Optional.ofNullable(overrideConfiguration);
    }

    /**
     * @return The optional progress listeners for this request.
     */
    public List<TransferProgressListener> progressListeners() {
        return progressListeners;
    }

    public interface Builder extends CopyableBuilder<Builder, TransferRequest> {
        /**
         * Set the optional override configuration for this request. Override
         * configurations take precedence those set on {@link
         * S3TransferManager}.
         *
         * @param overrideConfiguration The override configuration.
         * @return This object for method chaining.
         */
        Builder overrideConfiguration(TransferOverrideConfiguration overrideConfiguration);

        /**
         * Set the optional list of progress listeners for this request. This
         * list overwrites any previously added progress listeners.
         * @param progressListeners The progress listeners.
         * @return This object for method chaining.
         */
        Builder progressListeners(Collection<TransferProgressListener> progressListeners);

        /**
         * Add an additional progress listener to the current configured list of listeners.
         * @param progressListener The progress listener.
         * @return This object for method chaining.
         */
        Builder addProgressListener(TransferProgressListener progressListener);
    }

    protected abstract static class BuilderImpl implements Builder {
        private TransferOverrideConfiguration overrideConfiguration;
        private List<TransferProgressListener> progressListeners;

        protected BuilderImpl(TransferRequest other) {
            this.overrideConfiguration = other.overrideConfiguration;
            if (other.progressListeners != null) {
                this.progressListeners = new ArrayList<>(other.progressListeners);
            }
        }

        protected BuilderImpl() {
        }

        @Override
        public Builder overrideConfiguration(TransferOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public Builder progressListeners(Collection<TransferProgressListener> progressListeners) {
            this.progressListeners = new ArrayList<>(progressListeners);
            return this;
        }

        @Override
        public Builder addProgressListener(TransferProgressListener progressListener) {
            if (this.progressListeners == null) {
                this.progressListeners = new ArrayList<>();
            }
            this.progressListeners.add(progressListener);
            return this;
        }
    }
}
