/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import static software.amazon.awssdk.core.util.DateUtils.numberOfDaysSinceEpoch;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Presigner;
import software.amazon.awssdk.core.util.StringUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;
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

    private static final Logger LOG = LoggerFactory.getLogger(Aws4Signer.class);
    private static final int SIGNER_CACHE_MAX_SIZE = 300;
    private static final FifoCache<SignerKey> SIGNER_CACHE =
        new FifoCache<>(SIGNER_CACHE_MAX_SIZE);
    private static final List<String> LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE = Arrays.asList("connection", "x-amzn-trace-id");

    protected SdkHttpFullRequest.Builder doSign(SdkHttpFullRequest request,
                                                Aws4SignerRequestParams requestParams,
                                                T signingParams) {

        final SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
        AwsCredentials sanitizedCredentials = sanitizeCredentials(signingParams.awsCredentials());
        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            addSessionCredentials(mutableRequest, (AwsSessionCredentials) sanitizedCredentials);
        }

        addHostHeader(mutableRequest);
        addDateHeader(mutableRequest, requestParams.getFormattedSigningDateTime());

        String contentSha256 = calculateContentHash(mutableRequest, signingParams);
        mutableRequest.firstMatchingHeader(SignerConstant.X_AMZ_CONTENT_SHA256)
                      .filter(h -> h.equals("required"))
                      .ifPresent(h -> mutableRequest.header(SignerConstant.X_AMZ_CONTENT_SHA256, contentSha256));

        final String canonicalRequest = createCanonicalRequest(mutableRequest, contentSha256, signingParams.doubleUrlEncode());

        final String stringToSign = createStringToSign(canonicalRequest, requestParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials, requestParams);

        final byte[] signature = computeSignature(stringToSign, signingKey);

        mutableRequest.header(SignerConstant.AUTHORIZATION,
                              buildAuthorizationHeader(signature, sanitizedCredentials, requestParams, mutableRequest));

        processRequestPayload(mutableRequest, signature, signingKey, requestParams, signingParams);

        return mutableRequest;
    }

    protected SdkHttpFullRequest.Builder doPresign(SdkHttpFullRequest request,
                                                   Aws4SignerRequestParams requestParams,
                                                   U signingParams) {

        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();

        long expirationInSeconds = generateExpirationTime(signingParams);
        addHostHeader(mutableRequest);

        AwsCredentials sanitizedCredentials = sanitizeCredentials(signingParams.awsCredentials());
        if (sanitizedCredentials instanceof AwsSessionCredentials) {
            // For SigV4 pre-signing URL, we need to add "X-Amz-Security-Token"
            // as a query string parameter, before constructing the canonical
            // request.
            mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_SECURITY_TOKEN,
                                             ((AwsSessionCredentials) sanitizedCredentials).sessionToken());
        }

        // Add the important parameters for v4 signing
        final String timeStamp = requestParams.getFormattedSigningDateTime();

        addPreSignInformationToRequest(mutableRequest, sanitizedCredentials, requestParams, timeStamp, expirationInSeconds);

        final String contentSha256 = calculateContentHashPresign(mutableRequest, signingParams);

        final String canonicalRequest = createCanonicalRequest(mutableRequest, contentSha256, signingParams.doubleUrlEncode());

        final String stringToSign = createStringToSign(canonicalRequest, requestParams);

        final byte[] signingKey = deriveSigningKey(sanitizedCredentials, requestParams);

        final byte[] signature = computeSignature(stringToSign, signingKey);

        mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_SIGNATURE, BinaryUtils.toHex(signature));

        return mutableRequest;
    }

    @Override
    protected void addSessionCredentials(SdkHttpFullRequest.Builder mutableRequest,
                                         AwsSessionCredentials credentials) {
        mutableRequest.header(SignerConstant.X_AMZ_SECURITY_TOKEN, credentials.sessionToken());
    }

    /**
     * Calculate the hash of the request's payload. Subclass could override this
     * method to provide different values for "x-amz-content-sha256" header or
     * do any other necessary set-ups on the request headers. (e.g. aws-chunked
     * uses a pre-defined header value, and needs to change some headers
     * relating to content-encoding and content-length.)
     */
    protected String calculateContentHash(SdkHttpFullRequest.Builder mutableRequest, T signerParams) {
        InputStream payloadStream = getBinaryRequestPayloadStream(mutableRequest.content());
        payloadStream.mark(getReadLimit());
        String contentSha256 = BinaryUtils.toHex(hash(payloadStream));
        try {
            payloadStream.reset();
        } catch (IOException e) {
            throw new SdkClientException("Unable to reset stream after calculating AWS4 signature", e);
        }
        return contentSha256;
    }

    protected abstract void processRequestPayload(SdkHttpFullRequest.Builder mutableRequest,
                                                  byte[] signature,
                                                  byte[] signingKey,
                                                  Aws4SignerRequestParams signerRequestParams,
                                                  T signerParams);

    protected abstract String calculateContentHashPresign(SdkHttpFullRequest.Builder mutableRequest, U signerParams);

    /**
     * Step 1 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-canonical-request.html to
     * generate the canonical request.
     */
    private String createCanonicalRequest(SdkHttpFullRequest.Builder request,
                                          String contentSha256,
                                          boolean doubleUrlEncode) {
        final String canonicalRequest = request.method().toString() +
                                        SignerConstant.LINE_SEPARATOR +
                                        // This would optionally double url-encode the resource path
                                        getCanonicalizedResourcePath(request.encodedPath(), doubleUrlEncode) +
                                        SignerConstant.LINE_SEPARATOR +
                                        getCanonicalizedQueryString(request.rawQueryParameters()) +
                                        SignerConstant.LINE_SEPARATOR +
                                        getCanonicalizedHeaderString(request.headers()) +
                                        SignerConstant.LINE_SEPARATOR +
                                        getSignedHeadersString(request.headers()) +
                                        SignerConstant.LINE_SEPARATOR +
                                        contentSha256;

        if (LOG.isDebugEnabled()) {
            LOG.debug("AWS4 Canonical Request: '\"" + canonicalRequest + "\"");
        }

        return canonicalRequest;
    }

    /**
     * Step 2 of the AWS Signature version 4 calculation. Refer to
     * http://docs.aws
     * .amazon.com/general/latest/gr/sigv4-create-string-to-sign.html.
     */
    private String createStringToSign(String canonicalRequest,
                                      Aws4SignerRequestParams requestParams) {

        final String stringToSign = requestParams.getSigningAlgorithm() +
                                    SignerConstant.LINE_SEPARATOR +
                                    requestParams.getFormattedSigningDateTime() +
                                    SignerConstant.LINE_SEPARATOR +
                                    requestParams.getScope() +
                                    SignerConstant.LINE_SEPARATOR +
                                    BinaryUtils.toHex(hash(canonicalRequest));

        if (LOG.isDebugEnabled()) {
            LOG.debug("AWS4 String to Sign: '\"" + stringToSign + "\"");
        }

        return stringToSign;
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    private byte[] deriveSigningKey(AwsCredentials credentials,
                                    Aws4SignerRequestParams signerRequestParams) {

        final String cacheKey = computeSigningCacheKeyName(credentials, signerRequestParams);
        final long daysSinceEpochSigningDate = numberOfDaysSinceEpoch(signerRequestParams.getSigningDateTimeMilli());

        SignerKey signerKey = SIGNER_CACHE.get(cacheKey);

        if (signerKey != null && daysSinceEpochSigningDate == signerKey.getNumberOfDaysSinceEpoch()) {
            return signerKey.getSigningKey();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generating a new signing key as the signing key not available in the cache for the date "
                      + TimeUnit.DAYS.toMillis(daysSinceEpochSigningDate));
        }
        byte[] signingKey = newSigningKey(credentials,
                                          signerRequestParams.getFormattedSigningDate(),
                                          signerRequestParams.getRegionName(),
                                          signerRequestParams.getServiceSigningName());
        SIGNER_CACHE.add(cacheKey, new SignerKey(daysSinceEpochSigningDate, signingKey));
        return signingKey;
    }

    /**
     * Computes the name to be used to reference the signing key in the cache.
     */
    private String computeSigningCacheKeyName(AwsCredentials credentials,
                                              Aws4SignerRequestParams signerRequestParams) {
        return credentials.secretAccessKey() + "-" + signerRequestParams.getRegionName() + "-" +
               signerRequestParams.getServiceSigningName();
    }

    /**
     * Step 3 of the AWS Signature version 4 calculation. It involves deriving
     * the signing key and computing the signature. Refer to
     * http://docs.aws.amazon
     * .com/general/latest/gr/sigv4-calculate-signature.html
     */
    private byte[] computeSignature(String stringToSign, byte[] signingKey) {
        return sign(stringToSign.getBytes(Charset.forName("UTF-8")), signingKey,
                    SigningAlgorithm.HmacSHA256);
    }

    /**
     * Creates the authorization header to be included in the request.
     */
    private String buildAuthorizationHeader(byte[] signature,
                                            AwsCredentials credentials,
                                            Aws4SignerRequestParams signerParams,
                                            SdkHttpFullRequest.Builder mutableRequest) {

        String signingCredentials = credentials.accessKeyId() + "/" + signerParams.getScope();
        String credential = "Credential=" + signingCredentials;
        String signerHeaders = "SignedHeaders=" +
                               getSignedHeadersString(mutableRequest.headers());
        String signatureHeader = "Signature=" + BinaryUtils.toHex(signature);

        return SignerConstant.AWS4_SIGNING_ALGORITHM + " " + credential + ", " + signerHeaders + ", " + signatureHeader;
    }

    /**
     * Includes all the signing headers as request parameters for pre-signing.
     */
    private void addPreSignInformationToRequest(SdkHttpFullRequest.Builder mutableRequest,
                                                AwsCredentials sanitizedCredentials,
                                                Aws4SignerRequestParams signerParams,
                                                String timeStamp,
                                                long expirationInSeconds) {

        String signingCredentials = sanitizedCredentials.accessKeyId() + "/" + signerParams.getScope();

        mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_ALGORITHM, SignerConstant.AWS4_SIGNING_ALGORITHM);
        mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_DATE, timeStamp);
        mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_SIGNED_HEADER,
                                         getSignedHeadersString(mutableRequest.headers()));
        mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_EXPIRES,
                                         Long.toString(expirationInSeconds));
        mutableRequest.rawQueryParameter(SignerConstant.X_AMZ_CREDENTIAL, signingCredentials);
    }


    private String getCanonicalizedHeaderString(Map<String, List<String>> headers) {
        final List<String> sortedHeaders = new ArrayList<>(headers.keySet());
        sortedHeaders.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (shouldExcludeHeaderFromSigning(header)) {
                continue;
            }
            String key = lowerCase(header);

            for (String headerValue : headers.get(header)) {
                StringUtils.appendCompactedString(buffer, key);
                buffer.append(":");
                if (headerValue != null) {
                    StringUtils.appendCompactedString(buffer, headerValue);
                }
                buffer.append("\n");
            }
        }

        return buffer.toString();
    }

    private String getSignedHeadersString(Map<String, List<String>> headers) {
        final List<String> sortedHeaders = new ArrayList<>(headers.keySet());
        sortedHeaders.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder buffer = new StringBuilder();
        for (String header : sortedHeaders) {
            if (shouldExcludeHeaderFromSigning(header)) {
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append(";");
            }
            buffer.append(lowerCase(header));
        }

        return buffer.toString();
    }

    private boolean shouldExcludeHeaderFromSigning(String header) {
        return LIST_OF_HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(lowerCase(header));
    }

    private void addHostHeader(SdkHttpFullRequest.Builder mutableRequest) {
        // AWS4 requires that we sign the Host header so we
        // have to have it in the request by the time we sign.

        final StringBuilder hostHeaderBuilder = new StringBuilder(mutableRequest.host());
        if (!SdkHttpUtils.isUsingStandardPort(mutableRequest.protocol(), mutableRequest.port())) {
            hostHeaderBuilder.append(":").append(mutableRequest.port());
        }

        mutableRequest.header(SignerConstant.HOST, hostHeaderBuilder.toString());
    }

    private void addDateHeader(SdkHttpFullRequest.Builder mutableRequest, String dateTime) {
        mutableRequest.header(SignerConstant.X_AMZ_DATE, dateTime);
    }

    /**
     * Generates an expiration time for the presigned url. If user has specified
     * an expiration time, check if it is in the given limit.
     */
    private long generateExpirationTime(U signingParams) {

        long expirationInSeconds = signingParams.expirationTime().map(Instant::getEpochSecond)
                                                 .orElse(SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS);

        if (expirationInSeconds > SignerConstant.PRESIGN_URL_MAX_EXPIRATION_SECONDS) {
            throw new SdkClientException(
                "Requests that are pre-signed by SigV4 algorithm are valid for at most 7 days. "
                + "The expiration date set on the current request ["
                + Aws4SignerUtils.formatTimestamp(expirationInSeconds * 1000L) + "] has exceeded this limit.");
        }
        return expirationInSeconds;
    }

    /**
     * Generates a new signing key from the given parameters and returns it.
     */
    private byte[] newSigningKey(AwsCredentials credentials,
                                 String dateStamp, String regionName, String serviceName) {
        byte[] kSecret = ("AWS4" + credentials.secretAccessKey())
            .getBytes(Charset.forName("UTF-8"));
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
                     .timeOffset(executionAttributes.getAttribute(AwsSignerExecutionAttribute.TIME_OFFSET));

        if (executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE) != null) {
            paramsBuilder.doubleUrlEncode(executionAttributes.getAttribute(AwsSignerExecutionAttribute.SIGNER_DOUBLE_URL_ENCODE));
        }

        return paramsBuilder;
    }
}
