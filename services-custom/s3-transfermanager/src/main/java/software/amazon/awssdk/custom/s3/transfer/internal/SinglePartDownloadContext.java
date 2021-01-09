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

package software.amazon.awssdk.custom.s3.transfer.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.custom.s3.transfer.DownloadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * The context object for a single part download.
 */
@SdkInternalApi
public final class SinglePartDownloadContext {
    private final DownloadRequest downloadRequest;
    private final GetObjectRequest getObjectRequest;

    private SinglePartDownloadContext(BuilderImpl builder) {
        this.downloadRequest = builder.downloadRequest;
        this.getObjectRequest = builder.getObjectRequest;
    }

    /**
     * @return The original download request given to the transfer manager.
     */
    public DownloadRequest downloadRequest() {
        return downloadRequest;
    }

    public GetObjectRequest getObjectRequest() {
        return getObjectRequest;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder {
        /**
         * Set the original download request given to the transfer manager.
         *
         * @param downloadRequest The original download request.
         * @return This object for method chaining.
         */
        Builder downloadRequest(DownloadRequest downloadRequest);

        Builder getObjectRequest(GetObjectRequest getObjectRequest);

        /**
         * @return The build context object.
         */
        SinglePartDownloadContext build();
    }

    private static final class BuilderImpl implements Builder {
        private DownloadRequest downloadRequest;
        private GetObjectRequest getObjectRequest;

        @Override
        public Builder downloadRequest(DownloadRequest downloadRequest) {
            this.downloadRequest = downloadRequest;
            return this;
        }

        @Override
        public Builder getObjectRequest(GetObjectRequest getObjectRequest) {
            this.getObjectRequest = getObjectRequest;
            return this;
        }

        @Override
        public SinglePartDownloadContext build() {
            return new SinglePartDownloadContext(this);
        }
    }
}
