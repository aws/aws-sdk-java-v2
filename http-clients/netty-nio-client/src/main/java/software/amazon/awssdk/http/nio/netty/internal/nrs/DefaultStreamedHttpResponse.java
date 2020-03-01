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

package software.amazon.awssdk.http.nio.netty.internal.nrs;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.util.Objects;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A default streamed HTTP response.
 *
 * This class contains source imported from https://github.com/playframework/netty-reactive-streams,
 * licensed under the Apache License 2.0, available at the time of the fork (1/31/2020) here:
 * https://github.com/playframework/netty-reactive-streams/blob/master/LICENSE.txt
 *
 * All original source licensed under the Apache License 2.0 by playframework. All modifications are
 * licensed under the Apache License 2.0 by Amazon Web Services.
 */
@SdkInternalApi
public class DefaultStreamedHttpResponse extends DefaultHttpResponse implements StreamedHttpResponse {

    private final Publisher<HttpContent> stream;

    public DefaultStreamedHttpResponse(HttpVersion version, HttpResponseStatus status, Publisher<HttpContent> stream) {
        super(version, status);
        this.stream = stream;
    }

    public DefaultStreamedHttpResponse(HttpVersion version, HttpResponseStatus status, boolean validateHeaders,
                                       Publisher<HttpContent> stream) {
        super(version, status, validateHeaders);
        this.stream = stream;
    }

    @Override
    public void subscribe(Subscriber<? super HttpContent> subscriber) {
        stream.subscribe(subscriber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        DefaultStreamedHttpResponse that = (DefaultStreamedHttpResponse) o;

        return Objects.equals(stream, that.stream);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (stream != null ? stream.hashCode() : 0);
        return result;
    }
}
