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

package software.amazon.awssdk.core.internal.progress;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.core.progress.snapshot.ProgressSnapshot;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An SDK-internal implementation of {@link ProgressListener.Context.ExecutionSuccess} and its parent interfaces.
 *
 * @see ProgressListenerFailedContext
 */
@SdkProtectedApi
@Immutable
public final class ProgressListenerContext
    implements ProgressListener.Context.ExecutionSuccess,
               ToCopyableBuilder<ProgressListenerContext.Builder, ProgressListenerContext> {

    private final SdkRequest sdkRequest;
    private final SdkHttpRequest sdkHttpRequest;
    private final ProgressSnapshot uploadProgressSnapshot;
    private final ProgressSnapshot downloadProgressSnapshot;
    private final SdkHttpResponse sdkHttpResponse;
    private final SdkResponse executionSuccessfulSdkResponse;

    private ProgressListenerContext(Builder builder) {
        this.sdkRequest = builder.sdkRequest;
        this.sdkHttpRequest = builder.sdkHttpRequest;
        this.uploadProgressSnapshot = builder.uploadProgressSnapshot;
        this.downloadProgressSnapshot = builder.downloadProgressSnapshot;
        this.sdkHttpResponse = builder.sdkHttpResponse;
        this.executionSuccessfulSdkResponse = builder.executionSuccessfulSdkResponse;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public SdkRequest request() {
        return sdkRequest;
    }

    @Override
    public SdkHttpRequest httpRequest() {
        return sdkHttpRequest;
    }

    @Override
    public ProgressSnapshot uploadProgressSnapshot() {
        return uploadProgressSnapshot;
    }

    @Override
    public ProgressSnapshot downloadProgressSnapshot() {
        return downloadProgressSnapshot;
    }

    @Override
    public SdkHttpResponse httpResponse() {
        return sdkHttpResponse;
    }

    @Override
    public SdkResponse response() {
        return executionSuccessfulSdkResponse;
    }

    @Override
    public String toString() {
        return ToString.builder("ProgressListenerContext")
                       .add("sdkRequest", sdkRequest)
                       .add("uploadProgressSnapshot", uploadProgressSnapshot)
                       .add("downloadProgressSnapshot", downloadProgressSnapshot)
                       .add("executionSuccessfulSdkResponse", executionSuccessfulSdkResponse)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, ProgressListenerContext> {
        private SdkRequest sdkRequest;
        private SdkHttpRequest sdkHttpRequest;
        private ProgressSnapshot uploadProgressSnapshot;
        private ProgressSnapshot downloadProgressSnapshot;
        private SdkHttpResponse sdkHttpResponse;
        private SdkResponse executionSuccessfulSdkResponse;

        private Builder() {
        }

        private Builder(ProgressListenerContext context) {
            this.sdkRequest = context.sdkRequest;
            this.sdkHttpRequest = context.sdkHttpRequest;
            this.uploadProgressSnapshot = context.uploadProgressSnapshot;
            this.downloadProgressSnapshot = context.downloadProgressSnapshot;
            this.sdkHttpResponse = context.sdkHttpResponse;
            this.executionSuccessfulSdkResponse = context.executionSuccessfulSdkResponse;
        }

        public Builder request(SdkRequest sdkRequest) {
            this.sdkRequest = sdkRequest;
            return this;
        }

        public Builder httpRequest(SdkHttpRequest sdkHttpRequest) {
            this.sdkHttpRequest = sdkHttpRequest;
            return this;
        }

        public Builder uploadProgressSnapshot(ProgressSnapshot uploadProgressSnapshot) {
            this.uploadProgressSnapshot = uploadProgressSnapshot;
            return this;
        }

        public Builder downloadProgressSnapshot(ProgressSnapshot downloadProgressSnapshot) {
            this.downloadProgressSnapshot = downloadProgressSnapshot;
            return this;
        }

        public Builder httpResponse(SdkHttpResponse sdkHttpResponse) {
            this.sdkHttpResponse = sdkHttpResponse;
            return this;
        }

        public Builder response(SdkResponse executionSuccessfulSdkResponse) {
            this.executionSuccessfulSdkResponse = executionSuccessfulSdkResponse;
            return this;
        }

        @Override
        public ProgressListenerContext build() {
            return new ProgressListenerContext(this);
        }
    }
}

