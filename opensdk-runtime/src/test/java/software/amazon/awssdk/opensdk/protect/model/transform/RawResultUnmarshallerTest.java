/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.protect.model.transform;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.opensdk.model.ResultContentConsumer;
import software.amazon.awssdk.runtime.transform.JsonUnmarshallerContext;

/**
 * @Tests for {@link RawResultUnmarshaller}
 */
public class RawResultUnmarshallerTest {
    @Test
    public void testStreamClosed() throws IOException {
        // No-op
        ResultContentConsumer consumer = (result, content) -> {};
        RawResultUnmarshaller unmarshaller = new RawResultUnmarshaller(consumer);

        JsonUnmarshallerContext ctx = mock(JsonUnmarshallerContext.class);
        HttpResponse response = mock(HttpResponse.class);
        InputStream is = mock(InputStream.class);

        when(ctx.getHttpResponse()).thenReturn(response);
        when(response.getContent()).thenReturn(is);

        unmarshaller.unmarshall(ctx);

        verify(is).close();
    }

    @Test(expected = RuntimeException.class)
    public void testConsumerThrowsStreamEmptyStreamClosed() throws IOException {
        // Throw from the consumer
        ResultContentConsumer consumer = (result, content) -> {
            throw new RuntimeException("BOOM");
        };

        RawResultUnmarshaller unmarshaller = new RawResultUnmarshaller(consumer);

        JsonUnmarshallerContext ctx = mock(JsonUnmarshallerContext.class);
        HttpResponse response = mock(HttpResponse.class);
        InputStream is = mock(InputStream.class);

        when(ctx.getHttpResponse()).thenReturn(response);
        when(response.getContent()).thenReturn(is);

        try {
            unmarshaller.unmarshall(ctx);
        } finally {
            verify(is).close();
        }
    }
}
