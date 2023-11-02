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

package software.amazon.awssdk.http.auth.aws.eventstream.internal.io;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

public class SigV4DataFramePublisherTest {

    private static final List<Instant> SIGNING_INSTANTS = Stream.of(
        // Note: This first Instant is used for signing the request not an event
        OffsetDateTime.of(1981, 1, 16, 6, 30, 0, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 1, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 2, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 3, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 4, 0, ZoneOffset.UTC).toInstant()
    ).collect(Collectors.toList());

    /**
     * @return A clock that returns the values from {@link #SIGNING_INSTANTS} in order.
     * @throws IllegalStateException When there are no more instants to return.
     */
    private static Clock signingClock() {
        return new Clock() {
            private final AtomicInteger timeIndex = new AtomicInteger(0);

            @Override
            public Instant instant() {
                int idx = timeIndex.getAndIncrement();
                // Note: we use an atomic because Clock must be threadsafe,
                // though probably not necessary for our tests
                if (idx >= SIGNING_INSTANTS.size()) {
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

    /**
     * Test that the sigv4 publisher adapts an existing publisher with sigv4 chunk-signing
     */
    @Test
    public void sigV4DataFramePublisher_shouldAdaptAndSignPublisher() {
        TestVector testVector = generateTestVector();
        Clock signingClock = signingClock();
        Instant initialInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("access", "secret");
        CredentialScope credentialScope = new CredentialScope("us-east-1", "demo", initialInstant);

        Publisher<ByteBuffer> sigV4Publisher = SigV4DataFramePublisher.builder()
                                                                      .publisher(testVector.payload())
                                                                      .credentials(credentials)
                                                                      .credentialScope(credentialScope)
                                                                      .signature(testVector.signature())
                                                                      .signingClock(signingClock)
                                                                      .build();

        TestSubscriber testSubscriber = TestSubscriber.create();

        Flowable.fromPublisher(sigV4Publisher)
                .flatMap(new Function<ByteBuffer, Publisher<?>>() {
                    final Queue<Message> messages = new LinkedList<>();
                    final MessageDecoder decoder = new MessageDecoder(message -> messages.offer(message));

                    @Override
                    public Publisher<?> apply(ByteBuffer byteBuffer) {
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
        testSubscriber.assertValueSequence(testVector.expectedPublisher().blockingIterable());
    }

    /**
     * Test that without demand from subscriber, trailing empty frame is not delivered
     */
    @Test
    public void testBackPressure() {
        TestVector testVector = generateTestVector();
        Clock signingClock = signingClock();
        Instant initialInstant = signingClock.instant();
        AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("access", "secret");
        CredentialScope credentialScope = new CredentialScope("us-east-1", "demo", initialInstant);

        Publisher<ByteBuffer> sigV4Publisher = SigV4DataFramePublisher.builder()
                                                                      .publisher(testVector.payload())
                                                                      .credentials(credentials)
                                                                      .credentialScope(credentialScope)
                                                                      .signature(testVector.signature())
                                                                      .signingClock(signingClock)
                                                                      .build();

        Subscriber<Object> subscriber = Mockito.spy(new Subscriber<Object>() {

            @Override
            public void onSubscribe(Subscription s) {
                // Only request the size of request body (which should NOT include the empty frame)
                s.request(testVector.content().size());
            }

            @Override
            public void onNext(Object o) {
            }

            @Override
            public void onError(Throwable t) {
                fail("onError should never been called");

            }

            @Override
            public void onComplete() {
                fail("onComplete should never been called");

            }
        });

        Flowable.fromPublisher(sigV4Publisher)
                .flatMap(new Function<ByteBuffer, Publisher<?>>() {
                    final Queue<Message> messages = new LinkedList<>();
                    final MessageDecoder decoder = new MessageDecoder(message -> messages.offer(message));

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
        verify(subscriber, times(testVector.content().size())).onNext(any());
        // subscriber is not terminated (no onError/onComplete) since trailing empty frame is not delivered yet
        verify(subscriber, never()).onError(any());
        verify(subscriber, never()).onComplete();
    }

    private TestVector generateTestVector() {
        return new TestVector() {
            final List<String> content = Lists.newArrayList("A", "B", "C");

            @Override
            public List<String> content() {
                return content;
            }

            @Override
            public String signature() {
                return "e1d8e8c8815e60969f2a34765c9a15945ffc0badbaa4b7e3b163ea19131e949b";
            }

            @Override
            public Publisher<ByteBuffer> payload() {
                List<ByteBuffer> bodyBytes = content.stream()
                                                    .map(s -> ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)))
                                                    .collect(Collectors.toList());

                return Flowable.fromIterable(bodyBytes);
            }

            @Override
            public Flowable<Message> expectedPublisher() {
                Flowable<String> sigsHex = Flowable.just(
                    "7aabf85b765e6a4d0d500b6e968657b14726fa3e1eb7e839302728ffd77629a5",
                    "f72aa9642f571d24a6e1ae42f10f073ad9448d8a028b6bcd82da081335adda02",
                    "632af120435b57ec241d8bfbb12e496dfd5e2730a1a02ac0ab6eaa230ae02e9a",
                    "c6f679ddb3af68f5e82f0cf6761244cb2338cf11e7d01a24130aea1b7c17e53e");

                // The Last data frame is empty
                Flowable<String> payloads = Flowable.fromIterable(content).concatWith(Flowable.just(""));

                return sigsHex.zipWith(payloads, new BiFunction<String, String, Message>() {
                                           // The first Instant was used to sign the request
                                           private int idx = 1;

                                           @Override
                                           public Message apply(String sig, String payload) {
                                               Map<String, HeaderValue> headers = new HashMap<>();
                                               headers.put(":date", HeaderValue.fromTimestamp(SIGNING_INSTANTS.get(idx)));
                                               headers.put(":chunk-signature",
                                                           HeaderValue.fromByteArray(BinaryUtils.fromHex(sig)));
                                               idx += 1;
                                               return new Message(headers, payload.getBytes(StandardCharsets.UTF_8));
                                           }
                                       }
                );
            }
        };
    }

    private interface TestVector {
        String signature();

        List<String> content();

        Publisher<ByteBuffer> payload();

        Flowable<Message> expectedPublisher();
    }
}
