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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.util.Objects;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * An {@link SdkPublisher} that publishes response body content and also contains a reference to the {@link SdkResponse} returned
 * by the service.
 *
 * @param <ResponseT> Pojo response type.
 * @see AsyncResponseTransformer#toPublisher()
 */
@SdkPublicApi
public final class ResponsePublisher<ResponseT extends SdkResponse> implements SdkPublisher<ByteBuffer> {

    private final ResponseT response;
    private final SdkPublisher<ByteBuffer> publisher;

    public ResponsePublisher(ResponseT response, SdkPublisher<ByteBuffer> publisher) {
        this.response = Validate.paramNotNull(response, "response");
        this.publisher = Validate.paramNotNull(publisher, "publisher");
    }

    /**
     * @return the unmarshalled response object from the service.
     */
    public ResponseT response() {
        return response;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        publisher.subscribe(subscriber);
    }

    @Override
    public String toString() {
        return ToString.builder("ResponsePublisher")
                       .add("response", response)
                       .add("publisher", publisher)
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResponsePublisher<?> that = (ResponsePublisher<?>) o;

        if (!Objects.equals(response, that.response)) {
            return false;
        }
        return Objects.equals(publisher, that.publisher);
    }

    @Override
    public int hashCode() {
        int result = response != null ? response.hashCode() : 0;
        result = 31 * result + (publisher != null ? publisher.hashCode() : 0);
        return result;
    }
}
