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

package software.amazon.awssdk.services.s3.internal.crt;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;

/**
 * S3CrtProgressListener delegates events to the underlying delegateListener if defined, avoiding null checks for API when using
 * S3MetaRequestProgress for GET calls.
 */
@SdkInternalApi
public final class S3CrtProgressListener implements PublisherListener<S3MetaRequestProgress> {

    PublisherListener<S3MetaRequestProgress> delegateListener;

    private S3CrtProgressListener(Builder builder) {
        this.delegateListener = builder.delegateListener;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public void subscriberOnNext(S3MetaRequestProgress s3MetaRequestProgress) {
        if (delegateListener != null) {
            delegateListener.subscriberOnNext(s3MetaRequestProgress);
        }
    }

    @Override
    public void subscriberOnComplete() {
        if (delegateListener != null) {
            delegateListener.subscriberOnComplete();
        }
    }

    @Override
    public void subscriberOnError(Throwable t) {
        if (delegateListener != null) {
            delegateListener.subscriberOnError(t);
        }
    }

    @Override
    public void subscriptionCancel() {
        if (delegateListener != null) {
            delegateListener.subscriptionCancel();
        }
    }

    static class Builder {

        PublisherListener<S3MetaRequestProgress> delegateListener;

        Builder delegateListener(PublisherListener<S3MetaRequestProgress> delegateListener) {
            this.delegateListener = delegateListener;
            return this;
        }

        public S3CrtProgressListener build() {
            return new S3CrtProgressListener(this);
        }
    }
}