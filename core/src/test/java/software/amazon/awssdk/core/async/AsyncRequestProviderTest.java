/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.async.SimpleSubscriber;

public class AsyncRequestProviderTest {

    @Test
    public void canCreateAStringProvider() {
        AsyncRequestProvider provider = AsyncRequestProvider.fromString("Hello!");
        StringBuilder sb = new StringBuilder();
        boolean done = false;

        Subscriber<ByteBuffer> subscriber = spy(new SimpleSubscriber(buffer -> {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                sb.append(new String(bytes, StandardCharsets.UTF_8));
            }));

        provider.subscribe(subscriber);

        assertThat(sb.toString()).isEqualTo("Hello!");
        assertThat(provider.contentLength()).isEqualTo(6);
        verify(subscriber).onComplete();
        verify(subscriber, times(0)).onError(any(Throwable.class));
    }
}