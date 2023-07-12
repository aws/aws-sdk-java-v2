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

package software.amazon.awssdk.http.auth.aws.internal.chunkedencoding;

import static software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2.getCanonicalHeaders;
import static software.amazon.awssdk.http.auth.internal.util.CanonicalRequestV2.getCanonicalHeadersString;
import static software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils.hash;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.internal.util.CredentialScope;
import software.amazon.awssdk.http.auth.internal.util.SignerConstant;
import software.amazon.awssdk.http.auth.internal.util.SigningAlgorithm;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Pair;

/**
 * An implementation of AwsChunkSigner that can calculate a Sigv4 compatible chunk
 * signature.
 */
@SdkInternalApi
public class AwsS3V4ChunkSigner implements AwsChunkSigner {

    public static final int SIGNATURE_LENGTH = 64;
    public static final String EMPTY_STRING_SHA256_HEX = BinaryUtils.toHex(hash(""));
    private static final String CHUNK_STRING_TO_SIGN_PREFIX = "AWS4-HMAC-SHA256-PAYLOAD";
    private static final String TRAILING_HEADER_STRING_TO_SIGN_PREFIX = "AWS4-HMAC-SHA256-TRAILER";

    private final String dateTime;
    private final String keyPath;
    private final MessageDigest sha256;
    private final MessageDigest sha256ForTrailer;
    private final Mac hmacSha256;
    private final Mac trailerHmacSha256;

    public AwsS3V4ChunkSigner(byte[] signingKey, CredentialScope credentialScope) {
        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
            this.sha256ForTrailer = MessageDigest.getInstance("SHA-256");
            String signingAlgo = SigningAlgorithm.HMAC_SHA256.getAlgorithmName();
            this.hmacSha256 = Mac.getInstance(signingAlgo);
            hmacSha256.init(new SecretKeySpec(signingKey, signingAlgo));
            trailerHmacSha256 = Mac.getInstance(signingAlgo);
            trailerHmacSha256.init(new SecretKeySpec(signingKey, signingAlgo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        }
        this.dateTime = credentialScope.getDatetime();
        this.keyPath = credentialScope.scope();
    }

    public static int getSignatureLength() {
        return SIGNATURE_LENGTH;
    }

    @Override
    public String signChunk(byte[] chunkData, String previousSignature) {
        String chunkStringToSign =
            CHUNK_STRING_TO_SIGN_PREFIX + SignerConstant.LINE_SEPARATOR +
                dateTime + SignerConstant.LINE_SEPARATOR +
                keyPath + SignerConstant.LINE_SEPARATOR +
                previousSignature + SignerConstant.LINE_SEPARATOR +
                EMPTY_STRING_SHA256_HEX + SignerConstant.LINE_SEPARATOR +
                BinaryUtils.toHex(sha256.digest(chunkData));
        try {
            byte[] bytes = hmacSha256.doFinal(chunkStringToSign.getBytes(StandardCharsets.UTF_8));
            return BinaryUtils.toHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage());
        }
    }

    /**
     * Signed chunk must be of below format
     * signature = Hex(HMAC(K,
     * "AWS4-HMAC-SHA256-TRAILER"\n
     * DATE\n
     * KEYPATH\n
     * final_chunk_signature\n
     * Hex(SHA256(canonicalize(trailing-headers)))))
     *
     * @return Signed Checksum in above signature format.
     */
    public String signChecksumChunk(byte[] calculatedChecksum, String previousSignature, String checksumHeaderForTrailer) {


        List<Pair<String, List<String>>> canonicalHeaders = getCanonicalHeaders(
            Collections.singletonMap(checksumHeaderForTrailer,
                Collections.singletonList(BinaryUtils.toBase64(calculatedChecksum))));
        String canonicalizedHeaderString = getCanonicalHeadersString(canonicalHeaders);
        String chunkStringToSign =
            TRAILING_HEADER_STRING_TO_SIGN_PREFIX + SignerConstant.LINE_SEPARATOR +
                dateTime + SignerConstant.LINE_SEPARATOR +
                keyPath + SignerConstant.LINE_SEPARATOR +
                previousSignature + SignerConstant.LINE_SEPARATOR +
                BinaryUtils.toHex(sha256ForTrailer.digest(canonicalizedHeaderString.getBytes(StandardCharsets.UTF_8)));
        return BinaryUtils.toHex(trailerHmacSha256.doFinal(chunkStringToSign.getBytes(StandardCharsets.UTF_8)));
    }
}
