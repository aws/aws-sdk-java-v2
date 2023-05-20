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

package software.amazon.awssdk.http.auth.spi.internal;

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.AsyncSignedHttpRequest;
import software.amazon.awssdk.utils.ToString;

@SdkInternalApi
public final class DefaultAsyncSignedHttpRequest
    extends DefaultSignedHttpRequest<Publisher<ByteBuffer>> implements AsyncSignedHttpRequest {

    private DefaultAsyncSignedHttpRequest(BuilderImpl builder) {
        super(builder);
    }

    @Override
    public String toString() {
        return ToString.builder("AsyncSignedHttpRequest")
                       .add("request", request)
                       .build();
    }

    @SdkInternalApi
    public static final class BuilderImpl
        extends DefaultSignedHttpRequest.BuilderImpl<AsyncSignedHttpRequest.Builder, Publisher<ByteBuffer>>
        implements AsyncSignedHttpRequest.Builder {

        @Override
        public AsyncSignedHttpRequest build() {
            return new DefaultAsyncSignedHttpRequest(this);
        }
    }
}
