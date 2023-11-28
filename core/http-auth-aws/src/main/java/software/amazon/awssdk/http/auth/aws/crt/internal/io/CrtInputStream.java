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

package software.amazon.awssdk.http.auth.aws.crt.internal.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.utils.FunctionalUtils;

@SdkInternalApi
public final class CrtInputStream implements HttpRequestBodyStream {
    private static final int READ_BUFFER_SIZE = 4096;
    private final ContentStreamProvider provider;
    private final int bufSize;
    private final byte[] readBuffer;
    private InputStream providerStream;

    public CrtInputStream(ContentStreamProvider provider) {
        this.provider = provider;
        this.bufSize = READ_BUFFER_SIZE;
        this.readBuffer = new byte[bufSize];
    }

    @Override
    public boolean sendRequestBody(ByteBuffer bodyBytesOut) {
        int read;

        if (providerStream == null) {
            FunctionalUtils.invokeSafely(this::createNewStream);
        }

        int toRead = Math.min(bufSize, bodyBytesOut.remaining());
        read = FunctionalUtils.invokeSafely(() -> providerStream.read(readBuffer, 0, toRead));

        if (read > 0) {
            bodyBytesOut.put(readBuffer, 0, read);
        } else {
            FunctionalUtils.invokeSafely(providerStream::close);
        }

        return read < 0;
    }

    @Override
    public boolean resetPosition() {
        if (provider == null) {
            throw new IllegalStateException("Cannot reset position while provider is null");
        }

        FunctionalUtils.invokeSafely(this::createNewStream);

        return true;
    }

    private void createNewStream() throws IOException {
        if (provider == null) {
            throw new IllegalStateException("Cannot create a new stream while provider is null");
        }
        if (providerStream != null) {
            providerStream.close();
        }
        providerStream = provider.newStream();
    }
}
