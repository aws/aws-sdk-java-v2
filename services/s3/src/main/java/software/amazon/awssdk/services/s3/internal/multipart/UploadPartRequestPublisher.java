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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.util.concurrent.Executor;
import java.util.function.Function;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.internal.crt.RequestConversionUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * Generates {@link UploadPartRequest} and the associated {@link AsyncRequestBody} pairs.
 * Upon subscription request, it will start to prepare requests asynchronously by loading content from the provided
 * {@link AsyncRequestBody}. The maximum memory consumed is {@code numOfPartsBuffered * partSize} MB.
 */
@SdkInternalApi
public final class UploadPartRequestPublisher implements SdkPublisher<Pair<UploadPartRequest, AsyncRequestBody>> {
    private static final Logger log = Logger.loggerFor(UploadPartRequestPublisher.class);

    private final SdkPublisher<AsyncRequestBody> upstream;

    private final String uploadId;
    private final long optimalPartSize;
    private final PutObjectRequest putObjectRequest;
    private final int numOfPartsBuffered;

    public UploadPartRequestPublisher(Builder builder) {
        this.uploadId = builder.uploadId;
        this.optimalPartSize = builder.partSize;
        this.putObjectRequest = builder.putObjectRequest;
        this.numOfPartsBuffered = builder.numOfPartsBuffered;

        // change numOfPartsBuffered to just configure the maxMemoryUsageInBytes directly?
        this.upstream = builder.asyncRequestBody.split(optimalPartSize, numOfPartsBuffered * optimalPartSize);
    }

    public static Builder builder() {
        return new UploadPartRequestPublisher.Builder();
    }

    @Override
    public void subscribe(Subscriber<? super Pair<UploadPartRequest, AsyncRequestBody>> subscriber) {
        upstream.map(new BodyToRequestConverter()).subscribe(subscriber);
    }

    private class BodyToRequestConverter implements Function<AsyncRequestBody, Pair<UploadPartRequest, AsyncRequestBody>> {
        private int partNumber = 1;

        @Override
        public Pair<UploadPartRequest, AsyncRequestBody> apply(AsyncRequestBody asyncRequestBody) {
            UploadPartRequest uploadRequest =
                RequestConversionUtils.toUploadPartRequest(putObjectRequest,
                                                           partNumber,
                                                           uploadId);
            ++partNumber;
            return Pair.of(uploadRequest, asyncRequestBody);
        }
    }

    public static final class Builder {
        private String uploadId;
        private long partSize;

        private PutObjectRequest putObjectRequest;
        private AsyncRequestBody asyncRequestBody;
        private int numOfPartsBuffered;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder partSize(long partSize) {
            this.partSize = partSize;
            return this;
        }

        public Builder putObjectRequest(PutObjectRequest putObjectRequest) {
            this.putObjectRequest = putObjectRequest;
            return this;
        }

        public Builder asyncRequestBody(AsyncRequestBody asyncRequestBody) {
            this.asyncRequestBody = asyncRequestBody;
            return this;
        }

        public Builder numOfPartsBuffered(int numOfPartsBuffered) {
            this.numOfPartsBuffered = numOfPartsBuffered;
            return this;
        }

        public UploadPartRequestPublisher build() {
            return new UploadPartRequestPublisher(this);
        }
    }
}