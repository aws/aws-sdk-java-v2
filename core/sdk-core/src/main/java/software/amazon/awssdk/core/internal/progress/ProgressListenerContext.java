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

import java.util.Optional;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
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
 * An instance of this class can be used by ProgressListener methods to capture and store request progress
 * @see ProgressListenerFailedContext for failed transaction context definitions
 */
@SdkInternalApi
@Immutable
public final class ProgressListenerContext
    implements ProgressListener.Context.ExecutionSuccess,
               ToCopyableBuilder<ProgressListenerContext.Builder, ProgressListenerContext> {

    private final SdkRequest request;
    private final SdkHttpRequest httpRequest;
    private final ProgressSnapshot uploadProgressSnapshot;
    private final ProgressSnapshot downloadProgressSnapshot;
    private final SdkHttpResponse httpResponse;
    private final SdkResponse response;

    private ProgressListenerContext(Builder builder) {
        this.request = builder.request;
        this.httpRequest = builder.httpRequest;
        this.uploadProgressSnapshot = builder.uploadProgressSnapshot;
        this.downloadProgressSnapshot = builder.downloadProgressSnapshot;
        this.httpResponse = builder.httpResponse;
        this.response = builder.response;
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
        return request;
    }

    @Override
    public SdkHttpRequest httpRequest() {
        return httpRequest;
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
        return httpResponse;
    }

    @Override
    public Optional<SdkResponse> response() {
        return Optional.of(response);
    }

    @Override
    public String toString() {
        return ToString.builder("ProgressListenerContext")
                       .add("request", request)
                       .add("uploadProgressSnapshot", uploadProgressSnapshot)
                       .add("downloadProgressSnapshot", downloadProgressSnapshot)
                       .add("response", response)
                       .build();
    }

    public static final class Builder implements CopyableBuilder<Builder, ProgressListenerContext> {
        private SdkRequest request;
        private SdkHttpRequest httpRequest;
        private ProgressSnapshot uploadProgressSnapshot;
        private ProgressSnapshot downloadProgressSnapshot;
        private SdkHttpResponse httpResponse;
        private SdkResponse response;

        private Builder() {
        }

        private Builder(ProgressListenerContext context) {
            this.request = context.request;
            this.httpRequest = context.httpRequest;
            this.uploadProgressSnapshot = context.uploadProgressSnapshot;
            this.downloadProgressSnapshot = context.downloadProgressSnapshot;
            this.httpResponse = context.httpResponse;
            this.response = context.response;
        }

        public Builder request(SdkRequest request) {
            this.request = request;
            return this;
        }

        public Builder httpRequest(SdkHttpRequest httpRequest) {
            this.httpRequest = httpRequest;
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

        public Builder httpResponse(SdkHttpResponse httpResponse) {
            this.httpResponse = httpResponse;
            return this;
        }

        public Builder response(SdkResponse response) {
            this.response = response;
            return this;
        }

        @Override
        public ProgressListenerContext build() {
            return new ProgressListenerContext(this);
        }
    }
}

