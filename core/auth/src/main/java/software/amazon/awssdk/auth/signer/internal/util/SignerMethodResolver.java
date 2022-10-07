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

package software.amazon.awssdk.auth.signer.internal.util;

import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING;
import static software.amazon.awssdk.auth.signer.S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.internal.AbstractAwsS3V4Signer;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.signer.SigningMethod;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.signer.Signer;

@SdkInternalApi
public final class SignerMethodResolver {

    public static final String S3_SIGV4A_SIGNER_CLASS_PATH = "software.amazon.awssdk.authcrt.signer.internal"
                                                             + ".DefaultAwsCrtS3V4aSigner";

    private SignerMethodResolver() {
    }

    /**
     * The signing method can be Header-Auth, streaming-signing auth or Unsigned-payload.
     * For Aws4UnsignedPayloadSigner and ENABLE_PAYLOAD_SIGNING the protocol of request decides
     * whether the request will be Unsigned or Signed.
     *
     * @param signer              Signer Used.
     * @param executionAttributes Execution attributes.
     * @param credentials         Credentials configured for client.
     * @return SigningMethodUsed Enum based on various attributes.
     */
    public static SigningMethod resolveSigningMethodUsed(Signer signer, ExecutionAttributes executionAttributes,
                                                         AwsCredentials credentials) {

        SigningMethod signingMethod = SigningMethod.UNSIGNED_PAYLOAD;
        if (signer != null && !CredentialType.TOKEN.equals(signer.credentialType())) {
            if (isProtocolBasedStreamingSigningAuth(signer, executionAttributes)) {
                signingMethod = SigningMethod.PROTOCOL_STREAMING_SIGNING_AUTH;
            } else if (isProtocolBasedUnsigned(signer, executionAttributes)) {
                signingMethod = SigningMethod.PROTOCOL_BASED_UNSIGNED;
            } else if (isAnonymous(credentials) || signer instanceof NoOpSigner) {
                signingMethod = SigningMethod.UNSIGNED_PAYLOAD;
            } else {
                signingMethod = SigningMethod.HEADER_BASED_AUTH;
            }
        }
        return signingMethod;
    }

    private static boolean isProtocolBasedStreamingSigningAuth(Signer signer, ExecutionAttributes executionAttributes) {
        return (executionAttributes.getOptionalAttribute(ENABLE_PAYLOAD_SIGNING).orElse(false) &&
                executionAttributes.getOptionalAttribute(ENABLE_CHUNKED_ENCODING).orElse(false)) ||
               supportsPayloadSigning(signer)
               && executionAttributes.getOptionalAttribute(ENABLE_CHUNKED_ENCODING).orElse(false);
    }

    // S3 Signers like sigv4 abd sigv4a signers signs the payload with chucked encoding.
    private static boolean supportsPayloadSigning(Signer signer) {
        if (signer == null) {
            return false;
        }
        // auth-crt package is not a dependency of core package, thus we are directly checking the Canonical name.
        return signer instanceof AbstractAwsS3V4Signer ||
               S3_SIGV4A_SIGNER_CLASS_PATH.equals(signer.getClass().getCanonicalName());
    }

    private static boolean isProtocolBasedUnsigned(Signer signer, ExecutionAttributes executionAttributes) {

        return signer instanceof Aws4UnsignedPayloadSigner || signer instanceof AwsS3V4Signer
               || executionAttributes.getOptionalAttribute(ENABLE_PAYLOAD_SIGNING).orElse(false);
    }

    public static boolean isAnonymous(AwsCredentials credentials) {
        return credentials.secretAccessKey() == null && credentials.accessKeyId() == null;
    }

}
