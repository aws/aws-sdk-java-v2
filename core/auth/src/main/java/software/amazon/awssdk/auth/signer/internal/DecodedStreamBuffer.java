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

package software.amazon.awssdk.auth.signer.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
class DecodedStreamBuffer {
    private static final Logger log = Logger.loggerFor(DecodedStreamBuffer.class);

    private byte[] bufferArray;
    private int maxBufferSize;
    private int byteBuffered;
    private int pos = -1;
    private boolean bufferSizeOverflow;

    DecodedStreamBuffer(int maxBufferSize) {
        bufferArray = new byte[maxBufferSize];
        this.maxBufferSize = maxBufferSize;
    }

    public void buffer(byte read) {
        pos = -1;
        if (byteBuffered >= maxBufferSize) {
            log.debug(() -> "Buffer size " + maxBufferSize
                            + " has been exceeded and the input stream "
                            + "will not be repeatable. Freeing buffer memory");
            bufferSizeOverflow = true;
        } else {
            bufferArray[byteBuffered++] = read;
        }
    }

    public void buffer(byte[] src, int srcPos, int length) {
        pos = -1;
        if (byteBuffered + length > maxBufferSize) {
            log.debug(() -> "Buffer size " + maxBufferSize
                            + " has been exceeded and the input stream "
                            + "will not be repeatable. Freeing buffer memory");
            bufferSizeOverflow = true;
        } else {
            System.arraycopy(src, srcPos, bufferArray, byteBuffered, length);
            byteBuffered += length;
        }
    }

    public boolean hasNext() {
        return (pos != -1) && (pos < byteBuffered);
    }

    public byte next() {
        return bufferArray[pos++];
    }

    public void startReadBuffer() {
        if (bufferSizeOverflow) {
            throw SdkClientException.builder()
                                    .message("The input stream is not repeatable since the buffer size "
                                             + maxBufferSize + " has been exceeded.")
                                    .build();
        }
        pos = 0;
    }
}
