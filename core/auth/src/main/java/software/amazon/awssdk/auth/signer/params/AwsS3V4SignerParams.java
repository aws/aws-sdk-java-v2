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

package software.amazon.awssdk.auth.signer.params;

import software.amazon.awssdk.annotations.SdkPublicApi;

@SdkPublicApi
public final class AwsS3V4SignerParams extends Aws4SignerParams {

    private final Boolean enableChunkedEncoding;
    private final Boolean enablePayloadSigning;

    private AwsS3V4SignerParams(BuilderImpl builder) {
        super(builder);
        this.enableChunkedEncoding = builder.enableChunkedEncoding;
        this.enablePayloadSigning = builder.enablePayloadSigning;
    }

    public Boolean enableChunkedEncoding() {
        return enableChunkedEncoding;
    }

    public Boolean enablePayloadSigning() {
        return enablePayloadSigning;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends Aws4SignerParams.Builder<Builder> {

        /**
         * <p>
         * Configures the client to enable chunked encoding for all requests.
         * </p>
         * <p>
         * The default behavior is to enable chunked encoding automatically. Disable this flag will result in
         * disabling chunked encoding for all requests.
         * </p>
         * <p>
         * <b>Note:</b> Disabling this option has performance implications since the checksum for the
         * payload will have to be pre-calculated before sending the data. If your payload is large this
         * will affect the overall time required to upload an object. Using this option is recommended
         * only if your endpoint does not implement chunked uploading.
         * </p>
         *
         * @param enableChunkedEncoding True to enable chunked encoding and False to disable. Default value is True.
         */
        Builder enableChunkedEncoding(Boolean enableChunkedEncoding);

        /**
         * <p>
         * Configures the client to sign payloads in all situations.
         * </p>
         * <p>
         * Payload signing is optional when chunked encoding is not used and requests are made
         * against an HTTPS endpoint.  Under these conditions the client will by default
         * opt to not sign payloads to optimize performance.  If this flag is set to true the
         * client will instead always sign payloads.
         * </p>
         * <p>
         * <b>Note:</b> Payload signing can be expensive, particularly if transferring
         * large payloads in a single chunk.  Enabling this option will result in a performance
         * penalty.
         * </p>
         *
         * @param enablePayloadSigning True to explicitly enable payload signing in all situations. Default value is False.
         */
        Builder enablePayloadSigning(Boolean enablePayloadSigning);

        @Override
        AwsS3V4SignerParams build();
    }

    private static final class BuilderImpl extends Aws4SignerParams.BuilderImpl<Builder> implements Builder {
        static final boolean DEFAULT_CHUNKED_ENCODING_ENABLED = false;
        static final boolean DEFAULT_PAYLOAD_SIGNING_ENABLED = false;

        private Boolean enableChunkedEncoding = DEFAULT_CHUNKED_ENCODING_ENABLED;
        private Boolean enablePayloadSigning = DEFAULT_PAYLOAD_SIGNING_ENABLED;

        private BuilderImpl() {
        }

        @Override
        public Builder enableChunkedEncoding(Boolean enableChunkedEncoding) {
            this.enableChunkedEncoding = enableChunkedEncoding;
            return this;
        }

        public void setEnableChunkedEncoding(Boolean enableChunkedEncoding) {
            enableChunkedEncoding(enableChunkedEncoding);
        }

        @Override
        public Builder enablePayloadSigning(Boolean enablePayloadSigning) {
            this.enablePayloadSigning = enablePayloadSigning;
            return this;
        }

        public void setEnablePayloadSigning(Boolean enablePayloadSigning) {
            enablePayloadSigning(enablePayloadSigning);
        }

        @Override
        public AwsS3V4SignerParams build() {
            return new AwsS3V4SignerParams(this);
        }
    }
}
