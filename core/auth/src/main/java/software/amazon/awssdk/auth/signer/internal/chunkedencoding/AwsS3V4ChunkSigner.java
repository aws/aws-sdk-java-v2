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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.internal.AbstractAws4Signer;
import software.amazon.awssdk.auth.signer.internal.SigningAlgorithm;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * An implementation of AwsChunkSigner that can calculate a Sigv4 compatible chunk
 * signature.
 */
@SdkInternalApi
public class AwsS3V4ChunkSigner implements AwsChunkSigner {

    private static final int SIGNATURE_LENGTH = 64;
    private static final String CHUNK_STRING_TO_SIGN_PREFIX = "AWS4-HMAC-SHA256-PAYLOAD";

    private final String dateTime;
    private final String keyPath;
    private final MessageDigest sha256;
    private final Mac hmacSha256;

    public AwsS3V4ChunkSigner(byte[] signingKey, String datetime, String keyPath) {
        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
            String signingAlgo = SigningAlgorithm.HmacSHA256.toString();
            this.hmacSha256 = Mac.getInstance(signingAlgo);
            hmacSha256.init(new SecretKeySpec(signingKey, signingAlgo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
        this.dateTime = datetime;
        this.keyPath = keyPath;
    }

    public String signChunk(byte[] chunkData, String previousSignature) {
        String chunkStringToSign =
            CHUNK_STRING_TO_SIGN_PREFIX + "\n" +
            dateTime + "\n" +
            keyPath + "\n" +
            previousSignature + "\n" +
            AbstractAws4Signer.EMPTY_STRING_SHA256_HEX + "\n" +
            BinaryUtils.toHex(sha256.digest(chunkData));
        try {
            byte[] bytes = hmacSha256.doFinal(chunkStringToSign.getBytes(StandardCharsets.UTF_8));
            return BinaryUtils.toHex(bytes);
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to calculate a request signature: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    public static int getSignatureLength() {
        return SIGNATURE_LENGTH;
    }
}
