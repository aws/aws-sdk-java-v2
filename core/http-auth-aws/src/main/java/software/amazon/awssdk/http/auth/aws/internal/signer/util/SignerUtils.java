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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerConstant.X_AMZ_DECODED_CONTENT_LENGTH;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.internal.DigestAlgorithm;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Utility methods to be used by various AWS Signer implementations. This class is protected and subject to change.
 */
@SdkInternalApi
public final class SignerUtils {

    private static final Logger LOG = Logger.loggerFor(SignerUtils.class);

    private static final FifoCache<SignerKey> SIGNER_CACHE =
        new FifoCache<>(300);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));

    private SignerUtils() {
    }

    /**
     * Returns a string representation of the given datetime in yyyyMMdd format. The date returned is in the UTC zone.
     * <p>
     * For example, given an Instant with millis-value of 1416863450581, this method returns "20141124"
     */
    public static String formatDate(Instant instant) {
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Returns a string representation of the given datetime in yyyyMMdd'T'HHmmss'Z' format. The date returned is in the UTC
     * zone.
     * <p>
     * For example, given an Instant with millis-value of 1416863450581, this method returns "20141124T211050Z"
     */
    public static String formatDateTime(Instant instant) {
        return TIME_FORMATTER.format(instant);
    }

    /**
     * Create a hash of the canonical request string
     * <p>
     * Step 2 of the AWS Signature version 4 calculation. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#create-canonical-request-hash.
     */
    public static String hashCanonicalRequest(String canonicalRequestString) {
        return BinaryUtils.toHex(
            hash(canonicalRequestString)
        );
    }

    /**
     * Get the signing key based on the given credentials and a credential-scope
     */
    public static byte[] deriveSigningKey(AwsCredentialsIdentity credentials, CredentialScope credentialScope) {
        String cacheKey = createSigningCacheKeyName(credentials, credentialScope.getRegion(), credentialScope.getService());
        SignerKey signerKey = SIGNER_CACHE.get(cacheKey);

        if (signerKey != null && signerKey.isValidForDate(credentialScope.getInstant())) {
            return signerKey.getSigningKey();
        }

        LOG.trace(() -> "Generating a new signing key as the signing key not available in the cache for the date: " +
                        credentialScope.getInstant().toEpochMilli());
        byte[] signingKey = newSigningKey(credentials,
                                          credentialScope.getDate(),
                                          credentialScope.getRegion(),
                                          credentialScope.getService());
        SIGNER_CACHE.add(cacheKey, new SignerKey(credentialScope.getInstant(), signingKey));
        return signingKey;
    }

    private static String createSigningCacheKeyName(AwsCredentialsIdentity credentials,
                                                    String regionName,
                                                    String serviceName) {
        return credentials.secretAccessKey() + "-" + regionName + "-" + serviceName;
    }

    private static byte[] newSigningKey(AwsCredentialsIdentity credentials,
                                        String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + credentials.secretAccessKey())
            .getBytes(StandardCharsets.UTF_8);
        byte[] kDate = sign(dateStamp, kSecret);
        byte[] kRegion = sign(regionName, kDate);
        byte[] kService = sign(serviceName, kRegion);
        return sign(SignerConstant.AWS4_TERMINATOR, kService);
    }

    /**
     * Sign given data using a key.
     */
    public static byte[] sign(String stringData, byte[] key) {
        try {
            byte[] data = stringData.getBytes(StandardCharsets.UTF_8);
            return sign(data, key, SigningAlgorithm.HMAC_SHA256);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: ", e);
        }
    }

    /**
     * Sign given data using a key and a specific algorithm
     */
    public static byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) {
        try {
            Mac mac = algorithm.getMac();
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: ", e);
        }
    }

    /**
     * Compute the signature of a string using a signing key.
     * <p>
     * Step 4 of the AWS Signature version 4 calculation. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#calculate-signature.
     */
    public static byte[] computeSignature(String stringToSign, byte[] signingKey) {
        return sign(stringToSign.getBytes(StandardCharsets.UTF_8), signingKey,
                    SigningAlgorithm.HMAC_SHA256);
    }

    /**
     * Add the host header based on parameters of a request
     */
    public static void addHostHeader(SdkHttpRequest.Builder requestBuilder) {
        // AWS4 requires that we sign the Host header, so we
        // have to have it in the request by the time we sign.

        // If the SdkHttpRequest has an associated Host header
        // already set, prefer to use that.

        if (requestBuilder.headers().get(SignerConstant.HOST) != null) {
            return;
        }

        String host = requestBuilder.host();
        if (!SdkHttpUtils.isUsingStandardPort(requestBuilder.protocol(), requestBuilder.port())) {
            StringBuilder hostHeaderBuilder = new StringBuilder(host);
            hostHeaderBuilder.append(":").append(requestBuilder.port());
            requestBuilder.putHeader(SignerConstant.HOST, hostHeaderBuilder.toString());
        } else {
            requestBuilder.putHeader(SignerConstant.HOST, host);
        }
    }

    /**
     * Add a date header using a datetime string
     */
    public static void addDateHeader(SdkHttpRequest.Builder requestBuilder, String dateTime) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_DATE, dateTime);
    }

    /**
     * Move `Content-Length` to `x-amz-decoded-content-length` if not already present. If `Content-Length` is not present, then
     * the payload is read in its entirety to calculate the length.
     */
    public static long moveContentLength(SdkHttpRequest.Builder request, InputStream payload) {
        Optional<String> decodedContentLength = request.firstMatchingHeader(X_AMZ_DECODED_CONTENT_LENGTH);
        if (!decodedContentLength.isPresent()) {
            // if the decoded length isn't present, content-length must be there
            String contentLength = request.firstMatchingHeader(Header.CONTENT_LENGTH).orElseGet(
                () -> String.valueOf(readAll(payload))
            );

            request.putHeader(X_AMZ_DECODED_CONTENT_LENGTH, contentLength)
                   .removeHeader(Header.CONTENT_LENGTH);
            return Long.parseLong(contentLength);
        }

        // decoded header is already there, so remove content-length just to be sure it's gone
        request.removeHeader(Header.CONTENT_LENGTH);
        return Long.parseLong(decodedContentLength.get());
    }

    private static MessageDigest getMessageDigestInstance() {
        return DigestAlgorithm.SHA256.getDigest();
    }

    public static InputStream getBinaryRequestPayloadStream(ContentStreamProvider streamProvider) {
        try {
            if (streamProvider == null) {
                return new ByteArrayInputStream(new byte[0]);
            }
            return streamProvider.newStream();
        } catch (Exception e) {
            throw new RuntimeException("Unable to read request payload to sign request: ", e);
        }
    }

    public static byte[] hash(InputStream input) {
        try {
            MessageDigest md = getMessageDigestInstance();
            byte[] buf = new byte[4096];
            int read = 0;
            while (read >= 0) {
                read = input.read(buf);
                md.update(buf, 0, read);
            }
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute hash while signing request: ", e);
        }
    }

    public static byte[] hash(ByteBuffer input) {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(input);
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute hash while signing request: ", e);
        }
    }

    public static byte[] hash(byte[] data) {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(data);
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("Unable to compute hash while signing request: ", e);
        }
    }

    public static byte[] hash(String text) {
        return hash(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Consume entire stream and return the number of bytes - the stream will NOT be reset upon completion, so if it needs to
     * be read again, the caller MUST reset the stream.
     */
    private static int readAll(InputStream inputStream) {
        try {
            byte[] buffer = new byte[4096];
            int read = 0;
            int offset = 0;
            while (read >= 0) {
                read = inputStream.read(buffer);
                if (read >= 0) {
                    offset += read;
                }
            }
            return offset;
        } catch (Exception e) {
            throw new RuntimeException("Could not finish reading stream: ", e);
        }
    }

    public static String getContentHash(SdkHttpRequest.Builder requestBuilder) {
        return requestBuilder.firstMatchingHeader(X_AMZ_CONTENT_SHA256).orElseThrow(
            () -> new IllegalArgumentException("Content hash must be present in the '" + X_AMZ_CONTENT_SHA256 + "' header!")
        );
    }
}
