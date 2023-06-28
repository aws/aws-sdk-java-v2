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

package software.amazon.awssdk.http.auth.internal.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.http.auth.internal.checksums.ChecksumSpecs;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.internal.io.SdkDigestInputStream;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class HttpChecksumUtils {

    public static final String X_AMZ_TRAILER = "x-amz-trailer";

    private HttpChecksumUtils() {
    }

    public static byte[] longToByte(Long input) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(input);
        return buffer.array();
    }

    public static boolean isHttpChecksumPresent(SdkHttpRequest sdkHttpRequest, ChecksumSpecs checksumSpec) {

        //check for the Direct header Or check if Trailer Header is present.
        return sdkHttpRequest.firstMatchingHeader(checksumSpec.headerName()).isPresent() ||
            isTrailerChecksumPresent(sdkHttpRequest, checksumSpec);
    }

    /**
     * Calculate the hash of the request's payload.
     */
    public static String calculateContentHash(ContentStreamProvider payload,
                                              SdkChecksum contentFlexibleChecksum) {
        InputStream payloadStream = getBinaryRequestPayloadStream(payload);
        return BinaryUtils.toHex(hash(payloadStream, contentFlexibleChecksum));
    }

    public static InputStream getBinaryRequestPayloadStream(ContentStreamProvider streamProvider) {
        try {
            if (streamProvider == null) {
                return new ByteArrayInputStream(new byte[0]);
            }
            return streamProvider.newStream();
        } catch (Exception e) {
            throw new RuntimeException("Unable to read request payload to sign request: " + e.getMessage());
        }
    }

    public static byte[] hash(InputStream input, SdkChecksum sdkChecksum) {
        try {
            MessageDigest md = getMessageDigestInstance();
            DigestInputStream digestInputStream = new SdkDigestInputStream(
                input, md, sdkChecksum);
            byte[] buffer = new byte[1024];
            while (digestInputStream.read(buffer) > -1) {
            }
            return digestInputStream.getMessageDigest().digest();
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage());
        }
    }


    public static byte[] hash(String text) {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(text.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage());
        }
    }

    public static SdkChecksum createSdkChecksumFromRequest(SdkHttpRequest request, String checksumHeaderName,
                                                           ChecksumAlgorithm checksumAlgorithm) {
        boolean isHeaderPresent = StringUtils.isNotBlank(checksumHeaderName);

        if (isHeaderPresent
            && !HttpChecksumUtils.isHttpChecksumPresent(
            request,
            ChecksumSpecs.builder().headerName(checksumHeaderName).build())) {
            return SdkChecksum.forAlgorithm(checksumAlgorithm);
        }
        return null;
    }

    private static MessageDigest getMessageDigestInstance() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to get SHA256 Function: " + e.getMessage());
        }
    }

    private static boolean isTrailerChecksumPresent(SdkHttpRequest sdkHttpRequest, ChecksumSpecs checksumSpec) {
        Optional<String> trailerBasedChecksum = sdkHttpRequest.firstMatchingHeader(X_AMZ_TRAILER);
        if (trailerBasedChecksum.isPresent()) {
            return trailerBasedChecksum.filter(checksum -> checksum.equalsIgnoreCase(checksumSpec.headerName())).isPresent();
        }
        return false;
    }
}
