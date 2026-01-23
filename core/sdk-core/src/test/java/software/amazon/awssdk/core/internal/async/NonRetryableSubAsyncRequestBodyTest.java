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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.exception.NonRetryableException;

class NonRetryableSubAsyncRequestBodyTest {

    private SubAsyncRequestBodyConfiguration configuration;
    private Consumer<Long> onNumBytesReceived;
    private Consumer<Long> onNumBytesConsumed;
    private NonRetryableSubAsyncRequestBody requestBody;

    @BeforeEach
    void setUp() {
        onNumBytesReceived = mock(Consumer.class);
        onNumBytesConsumed = mock(Consumer.class);
        
        configuration = SubAsyncRequestBodyConfiguration.builder()
                .contentLengthKnown(true)
                .maxLength(1024L)
                .partNumber(1)
                .onNumBytesReceived(onNumBytesReceived)
                .onNumBytesConsumed(onNumBytesConsumed)
                .sourceBodyName("test-body")
                .build();
        
        requestBody = new NonRetryableSubAsyncRequestBody(configuration);
    }

    @Test
    void getters_shouldReturnConfigurationValues() {
        assertThat(requestBody.maxLength()).isEqualTo(1024L);
        assertThat(requestBody.partNumber()).isEqualTo(1);
        assertThat(requestBody.body()).isEqualTo("test-body");
        assertThat(requestBody.contentLength()).isEqualTo(Optional.of(1024L));
        assertThat(requestBody.receivedBytesLength()).isEqualTo(0L);
    }

    @Test
    void constructor_withNullConfiguration_shouldThrowException() {
        assertThatThrownBy(() -> new NonRetryableSubAsyncRequestBody(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void contentLength_whenContentLengthUnknown_shouldReturnBufferedLength() {
        SubAsyncRequestBodyConfiguration unknownLengthConfig = SubAsyncRequestBodyConfiguration.builder()
                .contentLengthKnown(false)
                .maxLength(1024L)
                .partNumber(1)
                .onNumBytesReceived(onNumBytesReceived)
                .onNumBytesConsumed(onNumBytesConsumed)
                .sourceBodyName("test-body")
                .build();
        
        NonRetryableSubAsyncRequestBody unknownLengthBody = new NonRetryableSubAsyncRequestBody(unknownLengthConfig);
        
        assertThat(unknownLengthBody.contentLength()).isEqualTo(Optional.of(0L));
        
        // Send some data
        ByteBuffer data = ByteBuffer.wrap("test".getBytes());
        unknownLengthBody.send(data);
        
        assertThat(unknownLengthBody.contentLength()).isEqualTo(Optional.of(4L));
    }

    @Test
    void subscribe_shouldReceiveAllData() {
        byte[] part1 = RandomStringUtils.randomAscii(1024).getBytes(StandardCharsets.UTF_8);
        byte[] part2 = RandomStringUtils.randomAscii(512).getBytes(StandardCharsets.UTF_8);
        requestBody.send(ByteBuffer.wrap(part1));
        requestBody.send(ByteBuffer.wrap(part2));
        requestBody.complete();
        List<ByteBuffer> receivedBuffers = new ArrayList<>();
        Flowable.fromPublisher(requestBody).forEach(buffer -> receivedBuffers.add(buffer));

        verify(onNumBytesReceived).accept(1024L);
        verify(onNumBytesConsumed).accept(1024L);
        verify(onNumBytesReceived).accept(512L);
        verify(onNumBytesConsumed).accept(512L);
        assertThat(requestBody.receivedBytesLength()).isEqualTo(1536L);
        assertThat(receivedBuffers).containsExactly(ByteBuffer.wrap(part1), ByteBuffer.wrap(part2));
    }

    @Test
    void subscribe_secondTime_shouldSendError() {
        Subscriber<ByteBuffer> subscriber1 = mock(Subscriber.class);
        Subscriber<ByteBuffer> subscriber2 = mock(Subscriber.class);
        
        // First subscription
        requestBody.subscribe(subscriber1);
        
        // Second subscription should fail
        requestBody.subscribe(subscriber2);
        
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriber2).onSubscribe(subscriptionCaptor.capture());
        
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(subscriber2).onError(errorCaptor.capture());
        
        Throwable error = errorCaptor.getValue();
        assertThat(error).isInstanceOf(NonRetryableException.class);
        assertThat(error.getMessage()).contains("This could happen due to a retry attempt");
    }

    @Test
    void receivedBytesLength_shouldTrackSentData() {
        assertThat(requestBody.receivedBytesLength()).isEqualTo(0L);
        
        ByteBuffer data1 = ByteBuffer.wrap("hello".getBytes());
        requestBody.send(data1);
        assertThat(requestBody.receivedBytesLength()).isEqualTo(5L);
        
        ByteBuffer data2 = ByteBuffer.wrap(" world".getBytes());
        requestBody.send(data2);
        assertThat(requestBody.receivedBytesLength()).isEqualTo(11L);
    }
}
