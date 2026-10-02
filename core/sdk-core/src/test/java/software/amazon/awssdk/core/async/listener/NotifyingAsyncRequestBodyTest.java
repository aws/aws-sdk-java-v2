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

package software.amazon.awssdk.core.async.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBody.BodyType;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;

public class NotifyingAsyncRequestBodyTest {
    private static AsyncRequestBody mockRequestBody;
    private static AsyncRequestBodyListener mockListener;

    @BeforeEach
    public void setup() {
        mockRequestBody = mock(AsyncRequestBody.class);
        mockListener = mock(AsyncRequestBodyListener.class);
    }

    @Test
    public void contentLength_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.contentLength();
        verify(mockRequestBody).contentLength();
    }

    @Test
    public void contentType_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.contentType();
        verify(mockRequestBody).contentType();
    }

    @Test
    public void body_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.body();
        verify(mockRequestBody).body();
    }

    @Test
    public void body_wrappingFileRequestBody_returnsFile() throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path path = fs.getPath("./test");
        Files.write(path, "Hello world".getBytes());

        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(AsyncRequestBody.fromFile(path), mockListener);

        assertThat(notifying.body()).isEqualTo(BodyType.FILE.getName());
    }

    @Test
    public void body_wrappingBytesRequestBody_returnsBytes() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(AsyncRequestBody.fromString("Hello world"), mockListener);

        assertThat(notifying.body()).isEqualTo(BodyType.BYTES.getName());
    }

    @Test
    public void subscribe_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        notifying.subscribe(mock(Subscriber.class));
        verify(mockRequestBody).subscribe(any(Subscriber.class));
    }

    @Test
    public void split_configObject_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        AsyncRequestBodySplitConfiguration config = AsyncRequestBodySplitConfiguration.builder().build();
        notifying.split(config);
        verify(mockRequestBody).split(eq(config));
    }

    @Test
    public void split_consumer_delegatesCall() {
        AsyncRequestBodyListener.NotifyingAsyncRequestBody notifying =
            new AsyncRequestBodyListener.NotifyingAsyncRequestBody(mockRequestBody, mockListener);

        Consumer<AsyncRequestBodySplitConfiguration.Builder> consumer = c -> {};
        notifying.split(consumer);
        verify(mockRequestBody).split(eq(consumer));

    }
}
