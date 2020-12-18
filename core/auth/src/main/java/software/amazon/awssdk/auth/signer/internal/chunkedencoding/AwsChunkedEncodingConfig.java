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

package software.amazon.awssdk.auth.signer.internal.chunkedencoding;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class AwsChunkedEncodingConfig {

    private final int chunkSize;
    private final int bufferSize;

    private AwsChunkedEncodingConfig(BuilderImpl builder) {
        this.chunkSize = builder.chunkSize;
        this.bufferSize = builder.bufferSize;
    }

    public static AwsChunkedEncodingConfig create() {
        return builder().build();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public int chunkSize() {
        return chunkSize;
    }

    public int bufferSize() {
        return bufferSize;
    }

    public interface Builder {

        Builder chunkSize(int chunkSize);

        Builder bufferSize(int bufferSize);

        AwsChunkedEncodingConfig build();
    }

    private static final class BuilderImpl implements Builder {
        static final int DEFAULT_CHUNKED_ENCODING_ENABLED = 128 * 1024;
        static final int DEFAULT_PAYLOAD_SIGNING_ENABLED = 256 * 1024;

        private int chunkSize = DEFAULT_CHUNKED_ENCODING_ENABLED;
        private int bufferSize = DEFAULT_PAYLOAD_SIGNING_ENABLED;

        private BuilderImpl() {
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public void setChunkSize(int chunkSize) {
            chunkSize(chunkSize);
        }

        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public void setBufferSize(int bufferSize) {
            bufferSize(bufferSize);
        }

        @Override
        public AwsChunkedEncodingConfig build() {
            return new AwsChunkedEncodingConfig(this);
        }
    }
}
