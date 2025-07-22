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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.async.FileAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.ContentRangeParser;

public class MultipartFileAsyncResponseTransformer implements AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> {
    private final FileAsyncResponseTransformer<GetObjectResponse> delegate;

    public MultipartFileAsyncResponseTransformer(FileAsyncResponseTransformer<GetObjectResponse> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<GetObjectResponse> prepare() {
        return delegate.prepare();
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        delegate.onResponse(response);
        ContentRangeParser.range(response.contentRange())
                          .ifPresent(pair -> delegate.setOffsetPosition(pair.left()));
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        delegate.onStream(publisher);
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        delegate.exceptionOccurred(error);
    }
}
