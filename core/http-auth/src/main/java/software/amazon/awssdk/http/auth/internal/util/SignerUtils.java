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

import static software.amazon.awssdk.http.auth.internal.util.HttpChecksumUtils.hash;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.internal.checksums.ContentChecksum;
import software.amazon.awssdk.http.auth.internal.checksums.SdkChecksum;
import software.amazon.awssdk.http.auth.spi.SignRequest;
import software.amazon.awssdk.http.auth.spi.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Utility methods to be used by various AWS Signer implementations.
 * This class is strictly internal and is subjected to change.
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
     * Returns a string representation of the given datetime in yyyyMMdd
     * format. The date returned is in the UTC zone.
     * <p>
     * For example, given an Instant with millis-value of 1416863450581, this
     * method returns "20141124"
     */
    public static String formatDate(Instant instant) {
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Returns a string representation of the given datetime in
     * yyyyMMdd'T'HHmmss'Z' format. The date returned is in the UTC zone.
     * <p>
     * For example, given an Instant with millis-value of 1416863450581, this
     * method returns "20141124T211050Z"
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
        byte[] kService = sign(serviceName, kRegion
        );
        return sign(SignerConstant.AWS4_TERMINATOR, kService);
    }

    private static byte[] sign(String stringData, byte[] key) {
        try {
            byte[] data = stringData.getBytes(StandardCharsets.UTF_8);
            return sign(data, key, SigningAlgorithm.HMAC_SHA256);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage());
        }
    }

    /**
     * Sign given data using a key and a specific algorithm
     */
    private static byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) {
        try {
            Mac mac = algorithm.getMac();
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage());
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
     * Validate that the {@link SignerProperty} is present in the {@link SignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and an exception is thrown otherwise.
     */
    public static <T> T validatedProperty(SignRequest<?, ?> request, SignerProperty<T> property) {
        return Validate.notNull(request.property(property), property.toString() + " must not be null!");
    }

    /**
     * Validate that the {@link SignerProperty} is present in the {@link SignRequest}.
     * <p>
     * The value, {@link T}, is return when present, and the default is returned otherwise.
     */
    public static <T> T validatedProperty(SignRequest<?, ?> request, SignerProperty<T> property, T defaultValue) {
        return Validate.getOrDefault(request.property(property), () -> defaultValue);
    }

    /**
     * Add the host header based on parameters of a request
     */
    public static void addHostHeader(SdkHttpRequest.Builder requestBuilder) {
        // AWS4 requires that we sign the Host header, so we
        // have to have it in the request by the time we sign.

        StringBuilder hostHeaderBuilder = new StringBuilder(requestBuilder.host());
        if (!SdkHttpUtils.isUsingStandardPort(requestBuilder.protocol(), requestBuilder.port())) {
            hostHeaderBuilder.append(":").append(requestBuilder.port());
        }

        requestBuilder.putHeader(SignerConstant.HOST, hostHeaderBuilder.toString());
    }

    /**
     * Add a date header using a datetime string
     */
    public static void addDateHeader(SdkHttpRequest.Builder requestBuilder, String dateTime) {
        requestBuilder.putHeader(SignerConstant.X_AMZ_DATE, dateTime);
    }

    public static void addSha256ContentHeader(SdkHttpRequest.Builder requestBuilder, ContentChecksum contentChecksum) {
        requestBuilder.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)
            .filter(h -> h.equals("required"))
            .ifPresent(h ->
                requestBuilder.putHeader(
                    SignerConstant.X_AMZ_CONTENT_SHA256, contentChecksum.contentHash()));
    }

    /**
     * Add a checksum header if the checksum is not null and the payload is signed
     */
    public static void addChecksumHeader(SdkHttpRequest.Builder requestBuilder, SdkChecksum sdkChecksum,
                                         String contentHashString,
                                         String checksumHeaderName) {

        if (sdkChecksum != null && !isUnsignedPayload(contentHashString)) {
            requestBuilder.putHeader(checksumHeaderName, BinaryUtils.toBase64(sdkChecksum.getChecksumBytes()));
        }
    }

    /**
     * Check if a payload is unsigned based on the content hash
     */
    private static boolean isUnsignedPayload(String contentHashString) {
        return "UNSIGNED_PAYLOAD".equals(contentHashString) || "STREAMING-UNSIGNED-PAYLOAD-TRAILER".equals(contentHashString);
    }
}
