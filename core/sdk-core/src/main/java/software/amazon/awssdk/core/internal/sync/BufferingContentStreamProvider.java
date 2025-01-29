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

package software.amazon.awssdk.core.internal.sync;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.utils.IoUtils;

/**
 * {@code ContentStreamProvider} implementation that buffers the data stream data to memory as it's read. Once the underlying
 * stream is read fully, all subsequent calls to {@link #newStream()} will use the buffered data.
 */
@SdkInternalApi
@NotThreadSafe
public final class BufferingContentStreamProvider implements ContentStreamProvider {
    private final ContentStreamProvider delegate;
    private final Long expectedLength;
    private BufferStream bufferedStream;

    private byte[] bufferedStreamData;
    private int count;

    public BufferingContentStreamProvider(ContentStreamProvider delegate, Long expectedLength) {
        this.delegate = delegate;
        this.expectedLength = expectedLength;
    }

    @Override
    public InputStream newStream() {
        if (bufferedStreamData != null) {
            return new ByteArrayStream(bufferedStreamData, 0, this.count);
        }

        if (bufferedStream == null) {
            InputStream delegateStream = delegate.newStream();
            bufferedStream = new BufferStream(delegateStream);
            IoUtils.markStreamWithMaxReadLimit(bufferedStream, Integer.MAX_VALUE);
        }

        invokeSafely(bufferedStream::reset);
        return bufferedStream;
    }

    class ByteArrayStream extends ByteArrayInputStream {

        ByteArrayStream(byte[] buf, int offset, int length) {
            super(buf, offset, length);
        }

        @Override
        public void close() throws IOException {
            super.close();
            bufferedStream.close();
        }
    }

    class BufferStream extends BufferedInputStream {
        BufferStream(InputStream in) {
            super(in);
        }

        public byte[] getBuf() {
            return this.buf;
        }

        public int getCount() {
            return this.count;
        }

        @Override
        public void close() throws IOException {
            // We only want to close the underlying stream if we're confident all its data is buffered. In some cases, the
            // stream might be closed before we read everything, and we want to avoid closing in these cases if the request
            // body is being reused.
            if (!hasExpectedLength() || expectedLengthReached()) {
                saveBuffer();
                super.close();
            }
        }
    }

    private void saveBuffer() {
        if (bufferedStreamData == null) {
            this.bufferedStreamData = bufferedStream.getBuf();
            this.count = bufferedStream.getCount();
        }
    }

    private boolean expectedLengthReached() {
        return bufferedStream.getCount() >= expectedLength;
    }

    private boolean hasExpectedLength() {
        return this.expectedLength != null;
    }

}
