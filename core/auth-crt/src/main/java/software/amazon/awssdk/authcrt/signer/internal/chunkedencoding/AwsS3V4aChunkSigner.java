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

package software.amazon.awssdk.authcrt.signer.internal.chunkedencoding;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.internal.chunkedencoding.AwsChunkSigner;
import software.amazon.awssdk.authcrt.signer.internal.AwsCrt4aSigningAdapter;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;

/**
 * An implementation of AwsChunkSigner that can calculate a Sigv4a compatible chunk signature.
 */
@SdkInternalApi
public class AwsS3V4aChunkSigner implements AwsChunkSigner {

    private static final int SIGNATURE_LENGTH = 144;

    private final AwsCrt4aSigningAdapter aws4aSigner;
    private final AwsSigningConfig signingConfig;

    public AwsS3V4aChunkSigner(AwsCrt4aSigningAdapter aws4aSigner, AwsSigningConfig signingConfig) {
        this.aws4aSigner = aws4aSigner;
        this.signingConfig = signingConfig;
    }

    public String signChunk(byte[] chunkData, String previousSignature) {
        byte[] chunkSignature = aws4aSigner.signChunk(chunkData,
                                                      previousSignature.getBytes(StandardCharsets.UTF_8),
                                                      signingConfig);
        return new String(chunkSignature, StandardCharsets.UTF_8);
    }

    public static int getSignatureLength() {
        return SIGNATURE_LENGTH;
    }
}
