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

package software.amazon.awssdk.auth.signer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.auth.signer.internal.BaseEventStreamAsyncAws4Signer.EVENT_STREAM_DATE;
import static software.amazon.awssdk.auth.signer.internal.BaseEventStreamAsyncAws4Signer.EVENT_STREAM_SIGNATURE;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.subscribers.TestSubscriber;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.internal.SignerTestUtils;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

/**
 * Unit tests for the {@link EventStreamAws4Signer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class Aws4EventStreamSignerTest {

    EventStreamAws4Signer signer = EventStreamAws4Signer.create();


    @Mock
    private Clock signingOverrideClock;

    @Before
    public void setupCase() {
        mockClock();
    }

    private void mockClock() {
        OffsetDateTime time =
            OffsetDateTime.of(1981, 1, 16, 6, 30, 0, 0, ZoneOffset.UTC);
        when(signingOverrideClock.millis()).thenReturn(time.toInstant().toEpochMilli());
    }

    interface TestVector {
        SdkHttpFullRequest.Builder httpFullRequest();

        List<String> requestBody();

        AsyncRequestBody requestBodyPublisher();

        Flowable<Message> expectedMessagePublisher();
    }

    TestVector generatetestVector() {
        return new TestVector() {
            List<String> requestBody = Lists.newArrayList("A", "B", "C");

            @Override
            public List<String> requestBody() {
                return requestBody;
            }

            @Override
            public SdkHttpFullRequest.Builder httpFullRequest() {
                //Header signature: "79f246d8652f08dd3cfaf84cc0d8b4fcce032332c78d43ea1ed6f4f6586ab59d";
                //Signing key: "29dc0a760fed568677d74136ad02d315a07d31b8f321f5c43350f284dac892c";
                return SdkHttpFullRequest.builder()
                                         .method(SdkHttpMethod.POST)
                                         .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                         .putHeader("content-encoding", "application/vnd.amazon.eventstream")
                                         .putHeader("x-amz-content-sha256", "STREAMING-AWS4-HMAC-SHA256-EVENTS")
                                         .encodedPath("/streaming")
                                         .protocol("https")
                                         .host("demo.us-east-1.amazonaws.com");
            }

            @Override
            public AsyncRequestBody requestBodyPublisher() {
                return new AsyncRequestBody() {
                    @Override
                    public void subscribe(Subscriber<? super ByteBuffer> s) {
                        Flowable.fromIterable(requestBody)
                                .map(str -> ByteBuffer.wrap(str.getBytes()))
                                .subscribe(s);
                    }

                    @Override
                    public Optional<Long> contentLength() {
                        return Optional.empty();
                    }
                };
            }

            @Override
            public Flowable<Message> expectedMessagePublisher() {
                Flowable<String> sigsHex = Flowable.just(
                    "2f9960bccd20df6e58d04242ee7854f614e5cee4ffe8ed6dcf12d68da44c7f1b",
                    "6929cce63a306c74ff8f4d00acf21e184e93b58309043f0e8fd81ef8fe6e147c",
                    "de5df3242a48957cc48e9e2d6a379c9fa00c7ba21f9ba058dd3dcdd586da0142",
                    "9388cce58ce5fc6d178984036b99ada22dc1852285289fb5e042b44551b75291");

                // The Last data frame is empty
                Flowable<String> payloads = Flowable.fromIterable(requestBody).concatWith(Flowable.just(""));

                return sigsHex.zipWith(payloads, new BiFunction<String, String, Message>() {
                                           Instant signingInstant = Instant.ofEpochMilli(signingOverrideClock.millis());

                                           @Override
                                           public Message apply(String sig, String payload) throws Exception {
                                               Map<String, HeaderValue> headers = new HashMap<>();
                                               headers.put(EVENT_STREAM_DATE, HeaderValue.fromTimestamp(signingInstant));
                                               headers.put(EVENT_STREAM_SIGNATURE,
                                                           HeaderValue.fromByteArray(BinaryUtils.fromHex(sig)));
                                               return new Message(headers, payload.getBytes());
                                           }
                                       }
                );
            }
        };

    }

    @Test
    public void testEventStreamSigning() {
        TestVector testVector = generatetestVector();
        SdkHttpFullRequest.Builder request = testVector.httpFullRequest();
        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        SdkHttpFullRequest signedRequest =
            SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");

        AsyncRequestBody transformedPublisher =
            SignerTestUtils.signAsyncRequest(signer, signedRequest, testVector.requestBodyPublisher(),
                                             credentials, "demo", signingOverrideClock, "us-east-1");

        TestSubscriber testSubscriber = TestSubscriber.create();

        Flowable.fromPublisher(transformedPublisher)
                .flatMap(new Function<ByteBuffer, Publisher<?>>() {
                    Queue<Message> messages = new LinkedList<>();
                    MessageDecoder decoder = new MessageDecoder(message -> messages.offer(message));

                    @Override
                    public Publisher<?> apply(ByteBuffer byteBuffer) throws Exception {
                        decoder.feed(byteBuffer.array());
                        List<Message> messageList = new ArrayList<>();
                        while (!messages.isEmpty()) {
                            messageList.add(messages.poll());
                        }

                        return Flowable.fromIterable(messageList);
                    }
                })
                .subscribe(testSubscriber);

        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
        testSubscriber.assertValueSequence(testVector.expectedMessagePublisher().blockingIterable());
    }

    /**
     * Test that without demand from subscriber, trailing empty frame is not delivered
     */
    @Test
    public void testBackPressure() {
        TestVector testVector = generatetestVector();
        SdkHttpFullRequest.Builder request = testVector.httpFullRequest();
        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        SdkHttpFullRequest signedRequest =
            SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingOverrideClock, "us-east-1");

        AsyncRequestBody transformedPublisher =
            SignerTestUtils.signAsyncRequest(signer, signedRequest, testVector.requestBodyPublisher(),
                                             credentials, "demo", signingOverrideClock, "us-east-1");


        Subscriber<Object> subscriber = Mockito.spy(new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription s) {
                //Only request the number of request body (excluding trailing empty frame)
                s.request(testVector.requestBody().size());
            }

            @Override
            public void onNext(Object o) {
            }

            @Override
            public void onError(Throwable t) {
                Assert.fail("onError should never been called");

            }

            @Override
            public void onComplete() {
                Assert.fail("onComplete should never been called");

            }
        });

        Flowable.fromPublisher(transformedPublisher)
                .flatMap(new Function<ByteBuffer, Publisher<?>>() {
                    Queue<Message> messages = new LinkedList<>();
                    MessageDecoder decoder = new MessageDecoder(message -> messages.offer(message));

                    @Override
                    public Publisher<?> apply(ByteBuffer byteBuffer) throws Exception {
                        decoder.feed(byteBuffer.array());
                        List<Message> messageList = new ArrayList<>();
                        while (!messages.isEmpty()) {
                            messageList.add(messages.poll());
                        }
                        return Flowable.fromIterable(messageList);
                    }
                })
                .subscribe(subscriber);


        // The number of events equal to the size of request body (excluding trailing empty frame)
        verify(subscriber, times(testVector.requestBody().size())).onNext(any());
        // subscriber is not terminated (no onError/onComplete) since trailing empty frame is not delivered yet
        verify(subscriber, never()).onError(any());
        verify(subscriber, never()).onComplete();
    }
}
