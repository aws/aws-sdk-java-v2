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

import static java.lang.Math.min;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.ContentStreamProvider;

@SdkInternalApi
final class CrtRequestInputStreamAdapter implements HttpRequestBodyStream {
    private static final int READ_BUFFER_SIZE = 16 * 1024;

    private final ContentStreamProvider provider;
    private volatile InputStream providerStream;
    private final byte[] readBuffer = new byte[READ_BUFFER_SIZE];

    CrtRequestInputStreamAdapter(ContentStreamProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        int read;

        try {
            if (providerStream == null) {
                createNewStream();
            }

            int toRead = min(READ_BUFFER_SIZE, bodyBytesOut.remaining());
            read = providerStream.read(readBuffer, 0, toRead);

            if (read > 0) {
                bodyBytesOut.put(readBuffer, 0, read);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return read < 0;
    }

    @Override
    public boolean resetPosition() {
        try {
            createNewStream();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return true;
    }

    private void createNewStream() throws IOException {
        if (providerStream != null) {
            providerStream.close();
        }
        providerStream = provider.newStream();
    }
}
