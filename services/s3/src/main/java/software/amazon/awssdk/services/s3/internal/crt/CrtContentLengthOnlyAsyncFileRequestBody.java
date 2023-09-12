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

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;

@SdkInternalApi
public final class CrtContentLengthOnlyAsyncFileRequestBody implements AsyncRequestBody {
    private final AsyncRequestBody asyncRequestBody;

    public CrtContentLengthOnlyAsyncFileRequestBody(Path path) {
        this.asyncRequestBody = AsyncRequestBody.fromFile(path);
    }

    @Override
    public Optional<Long> contentLength() {
        return asyncRequestBody.contentLength();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                subscriber.onError(new IllegalStateException("subscription not supported"));
            }

            @Override
            public void cancel() {

            }
        });

    }
}
