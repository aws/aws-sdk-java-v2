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

import java.io.InputStream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.compression.Compressor;
import software.amazon.awssdk.core.internal.io.AwsCompressionInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.utils.IoUtils;

/**
 * {@link ContentStreamProvider} implementation for compression.
 */
@SdkInternalApi
public class CompressionContentStreamProvider implements ContentStreamProvider {
    private final ContentStreamProvider underlyingInputStreamProvider;
    private InputStream currentStream;
    private final Compressor compressor;

    public CompressionContentStreamProvider(ContentStreamProvider underlyingInputStreamProvider, Compressor compressor) {
        this.underlyingInputStreamProvider = underlyingInputStreamProvider;
        this.compressor = compressor;
    }

    @Override
    public InputStream newStream() {
        closeCurrentStream();
        currentStream = AwsCompressionInputStream.builder()
                                                 .inputStream(underlyingInputStreamProvider.newStream())
                                                 .compressor(compressor)
                                                 .build();
        return currentStream;
    }

    private void closeCurrentStream() {
        if (currentStream != null) {
            IoUtils.closeQuietly(currentStream, null);
            currentStream = null;
        }
    }
}
