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

package software.amazon.awssdk.services.s3;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.checksums.SdkChecksum;

/**
 * Simple decoder for decoding an 'aws-chunked' body to get the raw content's
 * checksum.
 * <p>
 * See
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html">https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html</a>.
 */
public final class ChunkedDecoder {

    enum State {
        READING_META,
        READING_META_END,
        READING_CONTENT,
        READING_CONTENT_END,
        READING_TRAILER,
        ;
    }

    private State currentState = State.READING_META;

    private final ByteBuffer buffer = ByteBuffer.allocate(8 * 1024 * 1024); // 8 MiB
    private final SdkChecksum checksum;
    private ByteArrayOutputStream metaLine = new ByteArrayOutputStream();
    private ByteArrayOutputStream rawContent = new ByteArrayOutputStream();
    private int remainingContent = 0;

    public ChunkedDecoder(SdkChecksum checksum) {
        this.checksum = checksum;
    }

    public void update(byte[] data, int offset, int length) {
        buffer.put(data, offset, length);
        buffer.flip();
        processBuffer();
    }

    public byte[] checksumBytes() {
        return checksum.getChecksumBytes();
    }

    private void processBuffer() {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();

            switch (currentState) {
                case READING_META:
                    if (b == '\r') {
                        currentState = State.READING_META_END;
                        String meta = new String(metaLine.toByteArray(), StandardCharsets.UTF_8);
                        int semiIdx = meta.indexOf(';');
                        if (semiIdx > 0) {
                            meta = meta.substring(0, semiIdx);
                        }
                        remainingContent = Integer.parseInt(meta, 16);
                    } else {
                        metaLine.write(b);
                    }
                    break;
                case READING_META_END:
                    if (b != '\n') {
                        throw new RuntimeException("Expected newline");
                    }
                    // An empty chunk signifies the end of the content data. Only trailers if any, remain
                    if (remainingContent > 0) {
                        currentState = State.READING_CONTENT;
                    } else {
                        currentState = State.READING_TRAILER;
                    }
                    rawContent.reset();
                    break;
                case READING_CONTENT:
                    if (remainingContent > 0) {
                        checksum.update(b);
                        remainingContent -= 1;
                    } else if (b == '\r') {
                        currentState = State.READING_CONTENT_END;
                    } else {
                        throw new RuntimeException("Expected carriage return");
                    }
                    break;
                case READING_CONTENT_END:
                    if (b != '\n') {
                        throw new RuntimeException("Expected newline");
                    }
                    currentState = State.READING_META;
                    metaLine.reset();
                    break;
                case READING_TRAILER:
                    // don't need it
                    break;
            }
        }
        buffer.clear();
    }
}
