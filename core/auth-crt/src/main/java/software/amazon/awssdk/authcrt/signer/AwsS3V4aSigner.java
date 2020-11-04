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

package software.amazon.awssdk.authcrt.signer;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.internal.AbstractAws4aSigner;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * s3-specific signer implementation that signs requests with the asymmetric AWS4 (aws4a) signing protocol.
 */
@SdkPublicApi
public class AwsS3V4aSigner extends AbstractAws4aSigner {

    private AwsS3V4aSigner() {
    }

    /**
     * Creates a new AwsS3V4aSigner instance
     * @return a new AwsS3V4aSigner instance
     */
    public static AwsS3V4aSigner create() {
        return new AwsS3V4aSigner();
    }

    @Override
    protected void fillInCrtSigningConfig(AwsSigningConfig signingConfig,
                                          SdkHttpFullRequest request,
                                          ExecutionAttributes executionAttributes) {
        super.fillInCrtSigningConfig(signingConfig, request, executionAttributes);

        /*
         * Always add x-amz-content-sha256 to s3 requests
         */
        signingConfig.setSignedBodyHeader(AwsSigningConfig.AwsSignedBodyHeaderType.X_AMZ_CONTENT_SHA256);

        /*
         * Sha256 the body if requested via execution attributes, otherwise use UNSIGNED_PAYLOAD
         * In CRT signing, Sha256 will be done by default unless an override value is specified.
         */
        Optional<Boolean> signPayload = Optional.ofNullable(executionAttributes.getAttribute(
                S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING));
        if (signPayload == null || !signPayload.isPresent() || signPayload.get() == false) {
            signingConfig.setSignedBodyValue(AwsSigningConfig.AwsSignedBodyValue.UNSIGNED_PAYLOAD);
        }
    }
}
