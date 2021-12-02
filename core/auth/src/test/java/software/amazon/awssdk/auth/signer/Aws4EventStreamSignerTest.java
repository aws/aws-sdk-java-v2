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

package software.amazon.awssdk.auth.signer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.auth.signer.internal.BaseEventStreamAsyncAws4Signer.EVENT_STREAM_DATE;
import static software.amazon.awssdk.auth.signer.internal.BaseEventStreamAsyncAws4Signer.EVENT_STREAM_SIGNATURE;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.subscribers.TestSubscriber;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
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
public class Aws4EventStreamSignerTest {

    interface TestVector {
        SdkHttpFullRequest.Builder httpFullRequest();

        List<String> requestBody();

        AsyncRequestBody requestBodyPublisher();

        Flowable<Message> expectedMessagePublisher();
    }

    private static final List<Instant> SIGNING_INSTANTS = Stream.of(
            // Note: This first Instant is used for signing the request not an event
            OffsetDateTime.of(1981, 1, 16, 6, 30, 0, 0, ZoneOffset.UTC).toInstant(),
            OffsetDateTime.of(1981, 1, 16, 6, 30, 1, 0, ZoneOffset.UTC).toInstant(),
            OffsetDateTime.of(1981, 1, 16, 6, 30, 2, 0, ZoneOffset.UTC).toInstant(),
            OffsetDateTime.of(1981, 1, 16, 6, 30, 3, 0, ZoneOffset.UTC).toInstant(),
            OffsetDateTime.of(1981, 1, 16, 6, 30, 4, 0, ZoneOffset.UTC).toInstant()
    ).collect(Collectors.toList());

    private EventStreamAws4Signer signer = EventStreamAws4Signer.create();

    @Test
    public void testEventStreamSigning() {
        TestVector testVector = generateTestVector();
        SdkHttpFullRequest.Builder request = testVector.httpFullRequest();
        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        SdkHttpFullRequest signedRequest =
            SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingClock(), "us-east-1");

        AsyncRequestBody transformedPublisher =
            SignerTestUtils.signAsyncRequest(signer, signedRequest, testVector.requestBodyPublisher(),
                                             credentials, "demo", signingClock(), "us-east-1");

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
        TestVector testVector = generateTestVector();
        SdkHttpFullRequest.Builder request = testVector.httpFullRequest();
        AwsBasicCredentials credentials = AwsBasicCredentials.create("access", "secret");
        SdkHttpFullRequest signedRequest =
            SignerTestUtils.signRequest(signer, request.build(), credentials, "demo", signingClock(), "us-east-1");

        AsyncRequestBody transformedPublisher =
            SignerTestUtils.signAsyncRequest(signer, signedRequest, testVector.requestBodyPublisher(),
                                             credentials, "demo", signingClock(), "us-east-1");


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

    TestVector generateTestVector() {
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
                List<ByteBuffer> bodyBytes = requestBody.stream()
                        .map(s -> ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)))
                        .collect(Collectors.toList());

                Publisher<ByteBuffer> bodyPublisher = Flowable.fromIterable(bodyBytes);

                return AsyncRequestBody.fromPublisher(bodyPublisher);
            }

            @Override
            public Flowable<Message> expectedMessagePublisher() {
                Flowable<String> sigsHex = Flowable.just(
                        "7aabf85b765e6a4d0d500b6e968657b14726fa3e1eb7e839302728ffd77629a5",
                        "f72aa9642f571d24a6e1ae42f10f073ad9448d8a028b6bcd82da081335adda02",
                        "632af120435b57ec241d8bfbb12e496dfd5e2730a1a02ac0ab6eaa230ae02e9a",
                        "c6f679ddb3af68f5e82f0cf6761244cb2338cf11e7d01a24130aea1b7c17e53e");

                // The Last data frame is empty
                Flowable<String> payloads = Flowable.fromIterable(requestBody).concatWith(Flowable.just(""));

                return sigsHex.zipWith(payloads, new BiFunction<String, String, Message>() {
                            // The first Instant was used to sign the request
                            private int idx = 1;

                            @Override
                            public Message apply(String sig, String payload) throws Exception {
                                Map<String, HeaderValue> headers = new HashMap<>();
                                headers.put(EVENT_STREAM_DATE, HeaderValue.fromTimestamp(SIGNING_INSTANTS.get(idx++)));
                                headers.put(EVENT_STREAM_SIGNATURE,
                                        HeaderValue.fromByteArray(BinaryUtils.fromHex(sig)));
                                return new Message(headers, payload.getBytes(StandardCharsets.UTF_8));
                            }
                        }
                );
            }
        };
    }

    /**
     * @return A clock that returns the values from {@link #SIGNING_INSTANTS} in order.
     * @throws IllegalStateException When there are no more instants to return.
     */
    private static Clock signingClock() {
        return new Clock() {
            private AtomicInteger timeIndex = new AtomicInteger(0);

            @Override
            public Instant instant() {
                int idx;
                // Note: we use an atomic because Clock must be threadsafe,
                // though probably not necessary for our tests
                if ((idx = timeIndex.getAndIncrement()) >= SIGNING_INSTANTS.size()) {
                    throw new IllegalStateException("Clock ran out of Instants to return! " + idx);
                }
                return SIGNING_INSTANTS.get(idx);
            }

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
