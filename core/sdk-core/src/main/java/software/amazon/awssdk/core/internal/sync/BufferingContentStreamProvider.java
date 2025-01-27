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
    private InputStream bufferedStream;

    private byte[] bufferedStreamData;
    private int count;

    public BufferingContentStreamProvider(ContentStreamProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public InputStream newStream() {
        if (bufferedStreamData != null) {
            return new ByteArrayInputStream(bufferedStreamData, 0, this.count);
        }

        if (bufferedStream == null) {
            InputStream delegateStream = delegate.newStream();
            bufferedStream = new BufferStream(delegateStream);
            IoUtils.markStreamWithMaxReadLimit(bufferedStream, Integer.MAX_VALUE);
        }

        invokeSafely(bufferedStream::reset);
        return bufferedStream;
    }

    private class BufferStream extends BufferedInputStream {
        BufferStream(InputStream in) {
            super(in);
        }

        @Override
        public synchronized int read() throws IOException {
            int read = super.read();
            if (read < 0) {
                saveBuffer();
            }
            return read;
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read < 0) {
                saveBuffer();
            }
            return read;
        }

        private void saveBuffer() {
            if (bufferedStreamData == null) {
                IoUtils.closeQuietlyV2(in, null);
                BufferingContentStreamProvider.this.bufferedStreamData = this.buf;
                BufferingContentStreamProvider.this.count = this.count;
            }
        }
    }

}
