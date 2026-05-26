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

package software.amazon.awssdk.http.crt.internal.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.internal.CrtStreamHandler;
import software.amazon.awssdk.http.crt.internal.ResponseHandlerErrorNotifier;

/**
 * TCK black-box verification for {@link CrtRequestBodyPublisherSubscriber}.
 *
 * <p>Black-box is appropriate because the subscriber's behavior depends on its constructor-injected
 * collaborators ({@link CrtStreamHandler}, {@link CompletableFuture}, {@link SdkAsyncHttpResponseHandler}).
 * The test wires a real {@code CrtStreamHandler} backed by a mocked {@link HttpStreamBase} so no JNI
 * code is exercised.
 */
public class CrtRequestBodyPublisherSubscriberTckTest extends SubscriberBlackboxVerification<ByteBuffer> {

    public CrtRequestBodyPublisherSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber() {
        HttpStreamBase mockStream = mock(HttpStreamBase.class);
        when(mockStream.writeData(any(byte[].class), anyBoolean()))
            .thenReturn(CompletableFuture.completedFuture(null));
        // writeData(null, true) is the end-of-stream signal sent on onComplete; stub it too.
        when(mockStream.writeData(null, true))
            .thenReturn(CompletableFuture.completedFuture(null));

        CrtStreamHandler streamHandler = new CrtStreamHandler(CompletableFuture.completedFuture(mockStream));

        return new CrtRequestBodyPublisherSubscriber(
            streamHandler,
            new CompletableFuture<>(),
            new ResponseHandlerErrorNotifier(mock(SdkAsyncHttpResponseHandler.class)));
    }

    @Override
    public ByteBuffer createElement(int element) {
        return ByteBuffer.wrap(new byte[]{(byte) element});
    }
}
