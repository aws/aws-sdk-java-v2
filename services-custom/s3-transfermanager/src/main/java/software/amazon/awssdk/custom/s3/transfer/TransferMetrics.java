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

package software.amazon.awssdk.custom.s3.transfer;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Metrics for a completed transfer.
 */
@SdkPublicApi
public final class TransferMetrics {
    private final Duration elapsedTime;
    private final long bytesTransferred;

    private TransferMetrics(BuilderImpl builder) {
        this.elapsedTime = Validate.notNull(builder.elapsedTimed, "elapsedTime must not be null");
        this.bytesTransferred = builder.bytesTransferred;
    }

    public Duration elapsedTime() {
        return elapsedTime;
    }

    public long bytesTransferred() {
        return bytesTransferred;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        Builder elapsedTime(Duration elapsedTime);

        Builder bytesTransferred(long bytesTransferred);

        TransferMetrics build();
    }

    private static final class BuilderImpl implements Builder {
        private Duration elapsedTimed;
        private long bytesTransferred;

        @Override
        public Builder elapsedTime(Duration elapsedTimed) {
            this.elapsedTimed = elapsedTimed;
            return this;
        }

        @Override
        public Builder bytesTransferred(long bytesTransferred) {
            this.bytesTransferred = bytesTransferred;
            return this;
        }

        @Override
        public TransferMetrics build() {
            return new TransferMetrics(this);
        }
    }
}
