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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;

@SdkInternalApi
public class ResettableContentStreamProvider implements ContentStreamProvider {
    private final Supplier<InputStream> streamSupplier;
    private InputStream currentStream;

    public ResettableContentStreamProvider(Supplier<InputStream> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    @Override
    public InputStream newStream() {
        try {
            reset();
        } catch (IOException e) {
            throw new RuntimeException("Could not create new stream: ", e);
        }
        return currentStream;
    }

    private void reset() throws IOException {
        if (currentStream != null) {
            currentStream.reset();
        } else {
            currentStream = streamSupplier.get();
        }
    }
}
