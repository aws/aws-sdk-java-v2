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

package software.amazon.awssdk.core.internal.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Abortable;
import software.amazon.awssdk.utils.Validate;

/**
 * Sync aws-chunked response decoder. Strips framing and exposes trailers after EOF via {@link #trailers()}.
 */
@SdkInternalApi
public final class AwsChunkedResponseDecoder extends InputStream implements Abortable {

    private final InputStream in;
    private final AwsChunkedResponseParser parser;

    private boolean eof;

    // Skips the declared-trailer check; for tests
    AwsChunkedResponseDecoder(InputStream in, long expectedDecodedLength) {
        this(in, expectedDecodedLength, null);
    }

    /**
     * @param declaredTrailers see {@link AwsChunkedResponseParser}
     */
    public AwsChunkedResponseDecoder(InputStream in, long expectedDecodedLength, Set<String> declaredTrailers) {
        this.in = Validate.paramNotNull(in, "in");
        this.parser = new AwsChunkedResponseParser(expectedDecodedLength, declaredTrailers);
    }

    /**
     * @throws IllegalStateException if the stream has not been read to EOF successfully
     */
    public Map<String, String> trailers() {
        return parser.trailers();
    }

    @Override
    public int read() throws IOException {
        if (!advanceToData()) {
            return -1;
        }
        int b = in.read();
        if (b == -1) {
            parser.endOfInput(); // throws: input ended mid-chunk
        }
        parser.consumeData(1);
        return b;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        Validate.paramNotNull(buf, "buf");
        if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException("off=" + off + ", len=" + len + ", buffer length=" + buf.length);
        }
        if (len == 0) {
            return 0;
        }
        if (!advanceToData()) {
            return -1;
        }

        int toRead = (int) Math.min(len, parser.dataRemaining());
        int bytesRead = in.read(buf, off, toRead);
        if (bytesRead == -1) {
            parser.endOfInput(); // throws: input ended mid-chunk
        }
        parser.consumeData(bytesRead);
        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void abort() {
        if (in instanceof Abortable) {
            ((Abortable) in).abort();
        }
    }

    /**
     * Feeds framing bytes until data is available. Returns false at EOF.
     */
    private boolean advanceToData() throws IOException {
        if (eof) {
            return false;
        }
        while (parser.dataRemaining() == 0) {
            if (parser.isComplete()) {
                verifyEndOfInput();
                eof = true;
                return false;
            }
            int b = in.read();
            if (b == -1) {
                parser.endOfInput(); // throws: incomplete message
            }
            parser.consumeFramingByte(b);
        }
        return true;
    }

    /**
     * Ensures nothing follows the trailers. Relies on the HTTP client bounding the body, so this read doesn't block.
     */
    private void verifyEndOfInput() throws IOException {
        int b = in.read();
        if (b != -1) {
            parser.consumeFramingByte(b); // throws: data after terminal chunk
        }
        parser.endOfInput();
    }
}
