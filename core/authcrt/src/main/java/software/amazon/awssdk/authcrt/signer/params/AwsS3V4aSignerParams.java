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

package software.amazon.awssdk.authcrt.signer.params;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * S3-related parameters that are used during Sigv4a signing.
 */
@SdkPublicApi
public final class AwsS3V4aSignerParams extends Aws4aSignerParams {

    private final Boolean enablePayloadSigning;

    private AwsS3V4aSignerParams(BuilderImpl builder) {
        super(builder);
        this.enablePayloadSigning = builder.enablePayloadSigning;
    }

    public Boolean enablePayloadSigning() {
        return enablePayloadSigning;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends Aws4aSignerParams.Builder<Builder> {

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
        AwsS3V4aSignerParams build();
    }

    private static final class BuilderImpl extends Aws4aSignerParams.BuilderImpl<Builder> implements Builder {
        static final boolean DEFAULT_PAYLOAD_SIGNING_ENABLED = false;

        private Boolean enablePayloadSigning = DEFAULT_PAYLOAD_SIGNING_ENABLED;

        private BuilderImpl() {
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
        public AwsS3V4aSignerParams build() {
            return new AwsS3V4aSignerParams(this);
        }
    }
}
