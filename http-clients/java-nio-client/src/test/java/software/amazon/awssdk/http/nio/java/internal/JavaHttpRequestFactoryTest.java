/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java.internal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.junit.Test;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

public class JavaHttpRequestFactoryTest {

    @Test
    public void returnFullDuplexPublisherWithoutContentLengthTest() {
        SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

            }
        };
        AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
                .requestContentPublisher(publisher)
                .fullDuplex(true)
                .build();

        HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(asyncExecuteRequest);

        // Check whether the BodyPublisher is the same as the one generated with zero content length
        assertThat(bodyPublisher.equals(HttpRequest.BodyPublishers.fromPublisher(FlowAdapters.toFlowPublisher(publisher))));

    }

    @Test
    public void returnFullDuplexPublisherWithContentLengthTest() {
        SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(100L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

            }
        };
        AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
                .requestContentPublisher(publisher)
                .fullDuplex(true)
                .build();

        HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(asyncExecuteRequest);

        // Check whether the BodyPublisher is the same as the one generated with non-zero content length
        assertThat(bodyPublisher.equals(HttpRequest.BodyPublishers.fromPublisher(FlowAdapters.toFlowPublisher(publisher))));
    }

    @Test
    public void returnNonFullDuplexPublisherWithoutContentLengthTest() {
        SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.empty();
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

            }
        };
        AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
                .requestContentPublisher(publisher)
                .fullDuplex(false)
                .build();

        HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(asyncExecuteRequest);

        // Check whether the BodyPublisher is the same as the one generated with zero content length
        assertThat(bodyPublisher.equals(HttpRequest.BodyPublishers.noBody()));

    }

    @Test
    public void returnNonFullDuplexPublisherWithContentLengthTest() {
        SdkHttpContentPublisher publisher = new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.of(100L);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {

            }
        };
        AsyncExecuteRequest asyncExecuteRequest = AsyncExecuteRequest.builder()
                .requestContentPublisher(publisher)
                .fullDuplex(false)
                .build();

        HttpRequest.BodyPublisher bodyPublisher = JavaHttpRequestFactory.createBodyPublisher(asyncExecuteRequest);

        // Check whether the BodyPublisher is the same as the one generated with non-zero content length
        assertThat(bodyPublisher.equals(HttpRequest.BodyPublishers.fromPublisher(FlowAdapters.toFlowPublisher(publisher))));
    }

}