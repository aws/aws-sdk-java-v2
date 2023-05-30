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
import static software.amazon.awssdk.http.auth.internal.util.SignerConstant.AWS4_SIGNING_ALGORITHM;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Utility methods to be used by various AWS Signer implementations.
 * This class is strictly internal and is subjected to change.
 */
@SdkInternalApi
public class SignerUtils {

    private static final Logger LOG = Logger.loggerFor(SignerUtils.class);

    private static final FifoCache<SignerKey> SIGNER_CACHE =
        new FifoCache<>(300);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
        .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));

    private static final List<String> LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE =
        Arrays.asList("connection", "x-amzn-trace-id", "user-agent", "expect");

    private SignerUtils() {
    }

    /**
     * Returns a string representation of the given date time in yyyyMMdd
     * format. The date returned is in the UTC zone.
     * <p>
     * For example, given a time "1416863450581", this method returns "20141124"
     */
    public static String formatDateStamp(long timeMilli) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timeMilli));
    }

    /**
     * Returns a string representation of the given date time in yyyyMMdd
     * format. The date returned is in the UTC zone.
     * <p>
     * For example, given an Instant with millis-value of 1416863450581, this
     * method returns "20141124"
     */
    public static String formatDateStamp(Instant instant) {
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Returns a string representation of the given timestamp in
     * yyyyMMdd'T'HHmmss'Z' format. The date returned is in the UTC zone.
     * <p>
     * For example, given a time "1416863450581", this method returns
     * "20141124T211050Z"
     */
    public static String formatTimestamp(long timeMilli) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timeMilli));
    }


    /**
     * Build the credential scope ("CredentialScope") string as documented in SigV4.
     */
    public static String buildScope(String dateStamp, String serviceName, String regionName) {
        return dateStamp + "/" + regionName + "/" + serviceName + "/" + SignerConstant.AWS4_TERMINATOR;
    }

    /**
     * Generate the authorization header string.
     * <p>
     * Step 5 (partial) of the AWS Signature version 4 calculation. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#add-signature-to-request
     */
    public static String buildAuthorizationHeader(byte[] signature,
                                                  AwsCredentialsIdentity credentials,
                                                  String scope,
                                                  CanonicalRequest canonicalRequest) {
        String accessKeyId = credentials.accessKeyId();
        StringBuilder stringBuilder = canonicalRequest.signedHeaderStringBuilder();
        String signatureHex = BinaryUtils.toHex(signature);
        return AWS4_SIGNING_ALGORITHM
            + " Credential="
            + accessKeyId
            + "/"
            + scope
            + ", SignedHeaders="
            + stringBuilder
            + ", Signature="
            + signatureHex;
    }

    /**
     * Create a canonical request object for a given request
     * <p>
     * Step 1 of the AWS Signature version 4 calculation. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#create-canonical-request.
     */
    public static CanonicalRequest createCanonicalRequest(SdkHttpRequest request,
                                                          SdkHttpRequest.Builder requestBuilder,
                                                          String contentSha256,
                                                          boolean doubleUrlEncode,
                                                          boolean normalizePath) {
        return new CanonicalRequest(request, requestBuilder, contentSha256,
            doubleUrlEncode, normalizePath, LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE);
    }

    /**
     * Create a hash of the canonical request string
     * <p>
     * Step 2 of the AWS Signature version 4 calculation. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#create-canonical-request-hash.
     */
    public static String hashCanonicalRequest(String canonicalRequestString) {
        return BinaryUtils.toHex(hash(canonicalRequestString));
    }

    /**
     * Build the sign-string ("string to sign").
     * <p>
     * Step 3 of the AWS Signature version 4 calculation. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#create-string-to-sign
     */
    public static String buildStringToSign(String canonicalRequest,
                                           String signingAlgorithm,
                                           String requestSigningDateTime,
                                           String scope) {

        LOG.debug(() -> "AWS4 Canonical Request: " + canonicalRequest);

        String stringToSign = signingAlgorithm +
            SignerConstant.LINE_SEPARATOR +
            requestSigningDateTime +
            SignerConstant.LINE_SEPARATOR +
            scope +
            SignerConstant.LINE_SEPARATOR +
            hashCanonicalRequest(canonicalRequest);

        LOG.debug(() -> "AWS4 String to sign: " + stringToSign);
        return stringToSign;
    }

    /**
     * Get the signing key based on the given credentials, datetime, region, and service
     */
    public static byte[] deriveSigningKey(AwsCredentialsIdentity credentials,
                                          long requestSigningDateTimeMilli,
                                          String regionName,
                                          String serviceSigningName) {
        return deriveSigningKey(credentials,
            Instant.ofEpochMilli(requestSigningDateTimeMilli),
            regionName,
            serviceSigningName);
    }

    /**
     * Get the signing key based on the given credentials, instant, region, and service
     */
    public static byte[] deriveSigningKey(AwsCredentialsIdentity credentials, Instant signingInstant, String region,
                                          String service) {
        String cacheKey = createSigningCacheKeyName(credentials, region, service);
        SignerKey signerKey = SIGNER_CACHE.get(cacheKey);

        if (signerKey != null && signerKey.isValidForDate(signingInstant)) {
            return signerKey.getSigningKey();
        }

        LOG.trace(() -> "Generating a new signing key as the signing key not available in the cache for the date: " +
            signingInstant.toEpochMilli());
        byte[] signingKey = newSigningKey(credentials,
            formatDateStamp(signingInstant),
            region,
            service);
        SIGNER_CACHE.add(cacheKey, new SignerKey(signingInstant, signingKey));
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
            return sign(data, key, SigningAlgorithm.HmacSHA256);
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
     * compute the signature of a string using a signing key.
     * <p>
     * Step 4 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html#calculate-signature.
     */
    public static byte[] computeSignature(String stringToSign, byte[] signingKey) {
        return sign(stringToSign.getBytes(StandardCharsets.UTF_8), signingKey,
            SigningAlgorithm.HmacSHA256);
    }
}
