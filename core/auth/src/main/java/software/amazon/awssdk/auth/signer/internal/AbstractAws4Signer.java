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

package software.amazon.awssdk.auth.signer.internal;

import static software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.auth.signer.params.SignerChecksumParams;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Abstract base class for the AWS SigV4 signer implementations.
 * @param <T> Type of the signing params class that is used for signing the request
 * @param <U> Type of the signing params class that is used for pre signing the request
 */
@SdkInternalApi
public abstract class AbstractAws4Signer<T extends Aws4SignerParams, U extends Aws4PresignerParams>
    extends AbstractAwsSigner implements Presigner {

    public static final String EMPTY_STRING_SHA256_HEX = BinaryUtils.toHex(hash(""));

    private static final Logger LOG = Logger.loggerFor(Aws4Signer.class);
    private static final int SIGNER_CACHE_MAX_SIZE = 300;
    private static final FifoCache<SignerKey> SIGNER_CACHE =
        new FifoCache<>(SIGNER_CACHE_MAX_SIZE);
    private static final List<String> LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE =
        Arrays.asList("connection", "x-amzn-trace-id", "user-agent", "expect");

    protected SdkHttpFullRequest.Builder doSign(SdkHttpFullRequest request,
                                                Aws4SignerRequestParams requestParams,
                                                T signingParams) {
        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
        SdkChecksum sdkChecksum = createSdkChecksumFromParams(signingParams, request);
        String contentHash = calculateContentHash(mutableRequest, signingParams, sdkChecksum);
        return doSign(mutableRequest.build(), requestParams, signingParams,
                new ContentChecksum(contentHash, sdkChecksum));
    }

    protected SdkHttpFullRequest.Builder doSign(SdkHttpFullRequest request,
                                                Aws4SignerRequestParams requestParams,
                                                T signingParams,
                                                ContentChecksum contentChecksum) {

        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();

        AwsCredentials sanitizedCredentials = sanitizeCredentials(signingParams.awsCredentials());
        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            addSessionCredentials(mutableRequest, (AwsSessionCredentials) sanitizedCredentials);
        }

        addHostHeader(mutableRequest);
        addDateHeader(mutableRequest, requestParams.getFormattedRequestSigningDateTime());

        mutableRequest.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)
                      .filter(h -> h.equals("required"))
                      .ifPresent(h -> mutableRequest.putHeader(
                              SignerConstant.X_AMZ_CONTENT_SHA256, contentChecksum.contentHash()));

        putChecksumHeader(signingParams.checksumParams(), contentChecksum.contentFlexibleChecksum(),
                mutableRequest, contentChecksum.contentHash());

        CanonicalRequest canonicalRequest = createCanonicalRequest(request,
                                                                   mutableRequest,
                                                                   contentChecksum.contentHash(),
                                                                   signingParams.doubleUrlEncode(),
                                                                   signingParams.normalizePath());

        String canonicalRequestString = canonicalRequest.string();
        String stringToSign = createStringToSign(canonicalRequestString, requestParams);

        byte[] signingKey = deriveSigningKey(sanitizedCredentials, requestParams);

        byte[] signature = computeSignature(stringToSign, signingKey);

        mutableRequest.putHeader(SignerConstant.AUTHORIZATION,
                                 buildAuthorizationHeader(signature, sanitizedCredentials, requestParams, canonicalRequest));

        processRequestPayload(mutableRequest, signature, signingKey, requestParams, signingParams,
                contentChecksum.contentFlexibleChecksum());

        return mutableRequest;
    }

    protected SdkHttpFullRequest.Builder doPresign(SdkHttpFullRequest request,
                                                   Aws4SignerRequestParams requestParams,
                                                   U signingParams) {

        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();

        long expirationInSeconds = getSignatureDurationInSeconds(requestParams, signingParams);
        addHostHeader(mutableRequest);

        AwsCredentials sanitizedCredentials = sanitizeCredentials(signingParams.awsCredentials());
        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            // For SigV4 pre-signing URL, we need to add "X-Amz-Security-Token"
            // as a query string parameter, before constructing the canonical
            // request.
            mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_SECURITY_TOKEN,
                                                ((AwsSessionCredentials) sanitizedCredentials).sessionToken());
        }

        // Add the important parameters for v4 signing
        String contentSha256 = calculateContentHashPresign(mutableRequest, signingParams);

        CanonicalRequest canonicalRequest = createCanonicalRequest(request,
                                                                   mutableRequest,
                                                                   contentSha256,
                                                                   signingParams.doubleUrlEncode(),
                                                                   signingParams.normalizePath());

        addPreSignInformationToRequest(mutableRequest, canonicalRequest, sanitizedCredentials,
                                       requestParams, expirationInSeconds);

        String string = canonicalRequest.string();
        String stringToSign = createStringToSign(string, requestParams);

        byte[] signingKey = deriveSigningKey(sanitizedCredentials, requestParams);

        byte[] signature = computeSignature(stringToSign, signingKey);

        mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_SIGNATURE, BinaryUtils.toHex(signature));

        return mutableRequest;
    }

    @Override
    protected void addSessionCredentials(SdkHttpFullRequest.Builder mutableRequest,
                                         AwsSessionCredentials credentials) {
        mutableRequest.putHeader(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    /**
     * Calculate the hash of the request's payload. Subclass could override this
     * method to provide different values for "x-amz-content-sha256" header or
     * do any other necessary set-ups on the request headers. (e.g. aws-chunked
     * uses a pre-defined header value, and needs to change some headers
     * relating to content-encoding and content-length.)
     */
    protected String calculateContentHash(SdkHttpFullRequest.Builder mutableRequest, T signerParams) {
        return calculateContentHash(mutableRequest, signerParams, null);
    }


    /**
     * This method overloads calculateContentHash with contentFlexibleChecksum.
     * The contentFlexibleChecksum is computed at the same time while hash is calculated for Content.
     */
    protected String calculateContentHash(SdkHttpFullRequest.Builder mutableRequest, T signerParams,
                                          SdkChecksum contentFlexibleChecksum) {
        InputStream payloadStream = getBinaryRequestPayloadStream(mutableRequest.contentStreamProvider());
        return BinaryUtils.toHex(hash(payloadStream, contentFlexibleChecksum));
    }

    protected abstract void processRequestPayload(SdkHttpFullRequest.Builder mutableRequest,
                                                  byte[] signature,
                                                  byte[] signingKey,
                                                  Aws4SignerRequestParams signerRequestParams,
                                                  T signerParams);


    protected abstract void processRequestPayload(SdkHttpFullRequest.Builder mutableRequest,
                                                  byte[] signature,
                                                  byte[] signingKey,
                                                  Aws4SignerRequestParams signerRequestParams,
                                                  T signerParams,
                                                  SdkChecksum sdkChecksum);

    protected abstract String calculateContentHashPresign(SdkHttpFullRequest.Builder mutableRequest, U signerParams);

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    protected final byte[] deriveSigningKey(AwsCredentials credentials, Aws4SignerRequestParams signerRequestParams) {
        return deriveSigningKey(credentials,
                Instant.ofEpochMilli(signerRequestParams.getRequestSigningDateTimeMilli()),
                signerRequestParams.getRegionName(),
                signerRequestParams.getServiceSigningName());
    }

    protected final byte[] deriveSigningKey(AwsCredentials credentials, Instant signingInstant, String region, String service) {
        String cacheKey = createSigningCacheKeyName(credentials, region, service);
        SignerKey signerKey = SIGNER_CACHE.get(cacheKey);

        if (signerKey != null && signerKey.isValidForDate(signingInstant)) {
            return signerKey.getSigningKey();
        }

        LOG.trace(() -> "Generating a new signing key as the signing key not available in the cache for the date: " +
                signingInstant.toEpochMilli());
        byte[] signingKey = newSigningKey(credentials,
                Aws4SignerUtils.formatDateStamp(signingInstant),
                region,
                service);
        SIGNER_CACHE.add(cacheKey, new SignerKey(signingInstant, signingKey));
        return signingKey;
    }

    /**
     * Step 1 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-canonical-request.html to
     * generate the canonical request.
     */
    private CanonicalRequest createCanonicalRequest(SdkHttpFullRequest request,
                                                    SdkHttpFullRequest.Builder requestBuilder,
                                                    String contentSha256,
                                                    boolean doubleUrlEncode,
                                                    boolean normalizePath) {
        return new CanonicalRequest(request, requestBuilder, contentSha256, doubleUrlEncode, normalizePath);
    }

    /**
     * Step 2 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-string-to-sign.html.
     */
    private String createStringToSign(String canonicalRequest,
                                      Aws4SignerRequestParams requestParams) {

        LOG.debug(() -> "AWS4 Canonical Request: " + canonicalRequest);

        String requestHash = BinaryUtils.toHex(hash(canonicalRequest));

        String stringToSign = requestParams.getSigningAlgorithm() +
                              SignerConstant.LINE_SEPARATOR +
                              requestParams.getFormattedRequestSigningDateTime() +
                              SignerConstant.LINE_SEPARATOR +
                              requestParams.getScope() +
                              SignerConstant.LINE_SEPARATOR +
                              requestHash;

        LOG.debug(() -> "AWS4 String to sign: " + stringToSign);
        return stringToSign;
    }

    private String createSigningCacheKeyName(AwsCredentials credentials,
                                             String regionName,
                                             String serviceName) {
        return credentials.secretAccessKey() + "-" + regionName + "-" + serviceName;
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    private byte[] computeSignature(String stringToSign, byte[] signingKey) {
        return sign(stringToSign.getBytes(StandardCharsets.UTF_8), signingKey,
                    SigningAlgorithm.HmacSHA256);
    }

    /**
     * Creates the authorization header to be included in the request.
     */
    private String buildAuthorizationHeader(byte[] signature,
                                            AwsCredentials credentials,
                                            Aws4SignerRequestParams signerParams,
                                            CanonicalRequest canonicalRequest) {
        String accessKeyId = credentials.accessKeyId();
        String scope = signerParams.getScope();
        StringBuilder stringBuilder = canonicalRequest.signedHeaderStringBuilder();
        String signatureHex = BinaryUtils.toHex(signature);
        return SignerConstant.AWS4_SIGNING_ALGORITHM
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
     * Includes all the signing headers as request parameters for pre-signing.
     */
    private void addPreSignInformationToRequest(SdkHttpFullRequest.Builder mutableRequest,
                                                CanonicalRequest canonicalRequest,
                                                AwsCredentials sanitizedCredentials,
                                                Aws4SignerRequestParams signerParams,
                                                long expirationInSeconds) {

        String signingCredentials = sanitizedCredentials.accessKeyId() + "/" + signerParams.getScope();

        mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_ALGORITHM, SignerConstant.AWS4_SIGNING_ALGORITHM);
        mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_DATE, signerParams.getFormattedRequestSigningDateTime());
        mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_SIGNED_HEADER, canonicalRequest.signedHeaderString());
        mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_EXPIRES, Long.toString(expirationInSeconds));
        mutableRequest.putRawQueryParameter(SignerConstant.X_AMZ_CREDENTIAL, signingCredentials);
    }

    /**
     * Tests a char to see if is it whitespace.
     * This method considers the same characters to be white
     * space as the Pattern class does when matching \s
     *
     * @param ch the character to be tested
     * @return true if the character is white  space, false otherwise.
     */
    private static boolean isWhiteSpace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\u000b' || ch == '\r' || ch == '\f';
    }

    private void addHostHeader(SdkHttpFullRequest.Builder mutableRequest) {
        // AWS4 requires that we sign the Host header so we
        // have to have it in the request by the time we sign.

        StringBuilder hostHeaderBuilder = new StringBuilder(mutableRequest.host());
        if (!SdkHttpUtils.isUsingStandardPort(mutableRequest.protocol(), mutableRequest.port())) {
            hostHeaderBuilder.append(":").append(mutableRequest.port());
        }

        mutableRequest.putHeader(SignerConstant.HOST, hostHeaderBuilder.toString());
    }

    private void addDateHeader(SdkHttpFullRequest.Builder mutableRequest, String dateTime) {
        mutableRequest.putHeader(SignerConstant.X_AMZ_DATE, dateTime);
    }

    /**
     * Generates an expiration time for the presigned url. If user has specified
     * an expiration time, check if it is in the given limit.
     */
    private long getSignatureDurationInSeconds(Aws4SignerRequestParams requestParams,
                                               U signingParams) {

        long expirationInSeconds = signingParams.expirationTime()
                                                .map(t -> t.getEpochSecond() -
                                                          (requestParams.getRequestSigningDateTimeMilli() / 1000))
                                                .orElse(SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS);

        if (expirationInSeconds > SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw SdkClientException.builder()
                                    .message("Requests that are pre-signed by SigV4 algorithm are valid for at most 7" +
                                             " days. The expiration date set on the current request [" +
                                             Aws4SignerUtils.formatTimestamp(expirationInSeconds * 1000L) + "] +" +
                                            " has exceeded this limit.")
                                    .build();
        }
        return expirationInSeconds;
    }

    /**
     * Generates a new signing key from the given parameters and returns it.
     */
    private byte[] newSigningKey(AwsCredentials credentials,
                                 String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + credentials.secretAccessKey())
            .getBytes(StandardCharsets.UTF_8);
        byte[] kDate = sign(dateStamp, kSecret, SigningAlgorithm.HmacSHA256);
        byte[] kRegion = sign(regionName, kDate, SigningAlgorithm.HmacSHA256);
        byte[] kService = sign(serviceName, kRegion,
                               SigningAlgorithm.HmacSHA256);
        return sign(SignerConstant.AWS4_TERMINATOR, kService, SigningAlgorithm.HmacSHA256);
    }

    protected <B extends Aws4PresignerParams.Builder> B extractPresignerParams(B builder,
                                                                               ExecutionAttributes executionAttributes) {
        builder = extractSignerParams(builder, executionAttributes);
        builder.expirationTime(executionAttributes.getAttribute(AwsSignerExecutionAttribute.PRESIGNER_EXPIRATION));

        return builder;
    }

    protected <B extends Aws4SignerParams.Builder> B extractSignerParams(B paramsBuilder,
                                                                         ExecutionAttributes executionAttributes) {
        paramsBuilder.awsCredentials(executionAttributes.getAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS))
                     .signingName(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME))
                     .signingRegion(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_REGION))
                     .timeOffset(executionAttributes.getAttribute(AwsSignerExecutionAttribute.TIME_OFFSET))
                     .signingClockOverride(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNING_CLOCK));

        Boolean doubleUrlEncode = executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE);
        if (doubleUrlEncode != null) {
            paramsBuilder.doubleUrlEncode(doubleUrlEncode);
        }

        Boolean normalizePath =
            executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_NORMALIZE_PATH);
        if (normalizePath != null) {
            paramsBuilder.normalizePath(normalizePath);
        }
        ChecksumSpecs checksumSpecs = executionAttributes.getAttribute(RESOLVED_CHECKSUM_SPECS);
        if (checksumSpecs != null && checksumSpecs.algorithm() != null) {
            paramsBuilder.checksumParams(buildSignerChecksumParams(checksumSpecs));
        }
        return paramsBuilder;
    }

    private void putChecksumHeader(SignerChecksumParams checksumSigner, SdkChecksum sdkChecksum,
                                   SdkHttpFullRequest.Builder mutableRequest, String contentHashString) {

        if (checksumSigner != null && sdkChecksum != null && !UNSIGNED_PAYLOAD.equals(contentHashString)
            && !"STREAMING-UNSIGNED-PAYLOAD-TRAILER".equals(contentHashString)) {

            if (HttpChecksumUtils.isHttpChecksumPresent(mutableRequest.build(),
                                                        ChecksumSpecs.builder()
                                                                     .headerName(checksumSigner.checksumHeaderName()).build())) {
                LOG.debug(() -> "Checksum already added in header ");
                return;
            }
            String headerChecksum = checksumSigner.checksumHeaderName();
            if (StringUtils.isNotBlank(headerChecksum)) {
                mutableRequest.putHeader(headerChecksum,
                                         BinaryUtils.toBase64(sdkChecksum.getChecksumBytes()));
            }
        }
    }

    private SignerChecksumParams buildSignerChecksumParams(ChecksumSpecs checksumSpecs) {
        return SignerChecksumParams.builder().algorithm(checksumSpecs.algorithm())
                                   .isStreamingRequest(checksumSpecs.isRequestStreaming())
                                   .checksumHeaderName(checksumSpecs.headerName())
                                   .build();
    }

    private SdkChecksum createSdkChecksumFromParams(T signingParams, SdkHttpFullRequest request) {
        SignerChecksumParams signerChecksumParams = signingParams.checksumParams();
        boolean isValidChecksumHeader =
            signerChecksumParams != null && StringUtils.isNotBlank(signerChecksumParams.checksumHeaderName());

        if (isValidChecksumHeader
            && !HttpChecksumUtils.isHttpChecksumPresent(
            request,
            ChecksumSpecs.builder().headerName(signerChecksumParams.checksumHeaderName()).build())) {
            return SdkChecksum.forAlgorithm(signerChecksumParams.algorithm());
        }
        return null;
    }

    static final class CanonicalRequest {
        private final SdkHttpFullRequest request;
        private final SdkHttpFullRequest.Builder requestBuilder;
        private final String contentSha256;
        private final boolean doubleUrlEncode;
        private final boolean normalizePath;

        private String canonicalRequestString;
        private StringBuilder signedHeaderStringBuilder;
        private List<Pair<String, List<String>>> canonicalHeaders;
        private String signedHeaderString;

        CanonicalRequest(SdkHttpFullRequest request,
                         SdkHttpFullRequest.Builder requestBuilder,
                         String contentSha256,
                         boolean doubleUrlEncode,
                         boolean normalizePath) {
            this.request = request;
            this.requestBuilder = requestBuilder;
            this.contentSha256 = contentSha256;
            this.doubleUrlEncode = doubleUrlEncode;
            this.normalizePath = normalizePath;
        }

        public String string() {
            if (canonicalRequestString == null) {
                StringBuilder canonicalRequest = new StringBuilder(512);
                canonicalRequest.append(requestBuilder.method().toString())
                                .append(SignerConstant.LINE_SEPARATOR);
                addCanonicalizedResourcePath(canonicalRequest,
                                             request,
                                             doubleUrlEncode,
                                             normalizePath);
                canonicalRequest.append(SignerConstant.LINE_SEPARATOR);
                addCanonicalizedQueryString(canonicalRequest, requestBuilder);
                canonicalRequest.append(SignerConstant.LINE_SEPARATOR);
                addCanonicalizedHeaderString(canonicalRequest, canonicalHeaders());
                canonicalRequest.append(SignerConstant.LINE_SEPARATOR)
                                .append(signedHeaderStringBuilder())
                                .append(SignerConstant.LINE_SEPARATOR)
                                .append(contentSha256);
                this.canonicalRequestString = canonicalRequest.toString();
            }
            return canonicalRequestString;
        }

        private void addCanonicalizedResourcePath(StringBuilder result,
                                                  SdkHttpRequest request,
                                                  boolean urlEncode,
                                                  boolean normalizePath) {
            String path = normalizePath ? request.getUri().normalize().getRawPath()
                                        : request.encodedPath();

            if (StringUtils.isEmpty(path)) {
                result.append("/");
                return;
            }

            if (urlEncode) {
                path = SdkHttpUtils.urlEncodeIgnoreSlashes(path);
            }

            if (!path.startsWith("/")) {
                result.append("/");
            }
            result.append(path);

            // Normalization can leave a trailing slash at the end of the resource path,
            // even if the input path doesn't end with one. Example input: /foo/bar/.
            // Remove the trailing slash if the input path doesn't end with one.
            boolean trimTrailingSlash = normalizePath &&
                                        path.length() > 1 &&
                                        !request.encodedPath().endsWith("/") &&
                                        result.charAt(result.length() - 1) == '/';
            if (trimTrailingSlash) {
                result.setLength(result.length() - 1);
            }
        }

        /**
         * Examines the specified query string parameters and returns a
         * canonicalized form.
         * <p>
         * The canonicalized query string is formed by first sorting all the query
         * string parameters, then URI encoding both the key and value and then
         * joining them, in order, separating key value pairs with an '&amp;'.
         *
         * @return A canonicalized form for the specified query string parameters.
         */
        private void addCanonicalizedQueryString(StringBuilder result, SdkHttpRequest.Builder httpRequest) {

            SortedMap<String, List<String>> sorted = new TreeMap<>();

            /**
             * Signing protocol expects the param values also to be sorted after url
             * encoding in addition to sorted parameter names.
             */
            httpRequest.forEachRawQueryParameter((key, values) -> {
                if (StringUtils.isEmpty(key)) {
                    // Do not sign empty keys.
                    return;
                }

                String encodedParamName = SdkHttpUtils.urlEncode(key);

                List<String> encodedValues = new ArrayList<>(values.size());
                for (String value : values) {
                    String encodedValue = SdkHttpUtils.urlEncode(value);

                    // Null values should be treated as empty for the purposes of signing, not missing.
                    // For example "?foo=" instead of "?foo".
                    String signatureFormattedEncodedValue = encodedValue == null ? "" : encodedValue;

                    encodedValues.add(signatureFormattedEncodedValue);
                }
                Collections.sort(encodedValues);
                sorted.put(encodedParamName, encodedValues);
            });

            SdkHttpUtils.flattenQueryParameters(result, sorted);
        }

        public StringBuilder signedHeaderStringBuilder() {
            if (signedHeaderStringBuilder == null) {
                signedHeaderStringBuilder = new StringBuilder();
                addSignedHeaders(signedHeaderStringBuilder, canonicalHeaders());
            }
            return signedHeaderStringBuilder;
        }

        public String signedHeaderString() {
            if (signedHeaderString == null) {
                this.signedHeaderString = signedHeaderStringBuilder().toString();
            }
            return signedHeaderString;
        }

        private List<Pair<String, List<String>>> canonicalHeaders() {
            if (canonicalHeaders == null) {
                canonicalHeaders = canonicalizeSigningHeaders(requestBuilder);
            }
            return canonicalHeaders;
        }

        private void addCanonicalizedHeaderString(StringBuilder result, List<Pair<String, List<String>>> canonicalizedHeaders) {
            canonicalizedHeaders.forEach(header -> {
                result.append(header.left());
                result.append(":");
                for (String headerValue : header.right()) {
                    addAndTrim(result, headerValue);
                    result.append(",");
                }
                result.setLength(result.length() - 1);
                result.append("\n");
            });
        }

        private List<Pair<String, List<String>>> canonicalizeSigningHeaders(SdkHttpFullRequest.Builder headers) {
            List<Pair<String, List<String>>> result = new ArrayList<>(headers.numHeaders());

            headers.forEachHeader((key, value) -> {
                String lowerCaseHeader = lowerCase(key);
                if (!LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCaseHeader)) {
                    result.add(Pair.of(lowerCaseHeader, value));
                }
            });

            result.sort(Comparator.comparing(Pair::left));

            return result;
        }

        /**
         * "The addAndTrim function removes excess white space before and after values,
         * and converts sequential spaces to a single space."
         * <p>
         * https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
         * <p>
         * The collapse-whitespace logic is equivalent to:
         * <pre>
         *     value.replaceAll("\\s+", " ")
         * </pre>
         * but does not create a Pattern object that needs to compile the match
         * string; it also prevents us from having to make a Matcher object as well.
         */
        private void addAndTrim(StringBuilder result, String value) {
            int lengthBefore = result.length();
            boolean isStart = true;
            boolean previousIsWhiteSpace = false;

            for (int i = 0; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (isWhiteSpace(ch)) {
                    if (previousIsWhiteSpace || isStart) {
                        continue;
                    }
                    result.append(' ');
                    previousIsWhiteSpace = true;
                } else {
                    result.append(ch);
                    isStart = false;
                    previousIsWhiteSpace = false;
                }
            }

            if (lengthBefore == result.length()) {
                return;
            }

            int lastNonWhitespaceChar = result.length() - 1;
            while (isWhiteSpace(result.charAt(lastNonWhitespaceChar))) {
                --lastNonWhitespaceChar;
            }

            result.setLength(lastNonWhitespaceChar + 1);
        }

        private void addSignedHeaders(StringBuilder result, List<Pair<String, List<String>>> canonicalizedHeaders) {
            for (Pair<String, List<String>> header : canonicalizedHeaders) {
                result.append(header.left()).append(';');
            }

            if (!canonicalizedHeaders.isEmpty()) {
                result.setLength(result.length() - 1);
            }
        }
    }
}
