/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import static software.amazon.awssdk.handlers.AwsExecutionAttributes.REQUEST_CONFIG;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.RequestClientOptions;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.auth.internal.Aws4SignerRequestParams;
import software.amazon.awssdk.event.ProgressInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.runtime.io.SdkDigestInputStream;
import software.amazon.awssdk.util.SdkHttpUtils;
import software.amazon.awssdk.utils.Base64Utils;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Abstract base class for AWS signing protocol implementations. Provides
 * utilities commonly needed by signing protocols such as computing
 * canonicalized host names, query string parameters, etc.
 * <p>
 * Not intended to be sub-classed by developers.
 */
public abstract class AbstractAwsSigner implements Signer {

    public static final String EMPTY_STRING_SHA256_HEX;
    private static final ThreadLocal<MessageDigest> SHA256_MESSAGE_DIGEST;

    static {
        SHA256_MESSAGE_DIGEST = new ThreadLocal<MessageDigest>() {
            @Override
            protected MessageDigest initialValue() {
                try {
                    return MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    throw new SdkClientException(
                            "Unable to get SHA256 Function"
                            + e.getMessage(), e);
                }
            }
        };
        EMPTY_STRING_SHA256_HEX = BinaryUtils.toHex(doHash(""));
    }

    private static byte[] doHash(String text) throws SdkClientException {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(text.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to compute hash while signing request: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Returns the re-usable thread local version of MessageDigest.
     */
    private static MessageDigest getMessageDigestInstance() {
        MessageDigest messageDigest = SHA256_MESSAGE_DIGEST.get();
        messageDigest.reset();
        return messageDigest;
    }

    /**
     * Computes an RFC 2104-compliant HMAC signature and returns the result as a
     * Base64 encoded string.
     */
    protected String signAndBase64Encode(String data, String key,
                                         SigningAlgorithm algorithm) throws SdkClientException {
        return signAndBase64Encode(data.getBytes(StandardCharsets.UTF_8), key, algorithm);
    }

    /**
     * Computes an RFC 2104-compliant HMAC signature for an array of bytes and
     * returns the result as a Base64 encoded string.
     */
    private String signAndBase64Encode(byte[] data, String key,
                                       SigningAlgorithm algorithm) throws SdkClientException {
        try {
            byte[] signature = sign(data, key.getBytes(StandardCharsets.UTF_8), algorithm);
            return Base64Utils.encodeAsString(signature);
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to calculate a request signature: "
                    + e.getMessage(), e);
        }
    }

    public byte[] signWithMac(String stringData, Mac mac) {
        try {
            return mac.doFinal(stringData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to calculate a request signature: "
                    + e.getMessage(), e);
        }
    }

    public byte[] sign(String stringData, byte[] key,
                       SigningAlgorithm algorithm) throws SdkClientException {
        try {
            byte[] data = stringData.getBytes(StandardCharsets.UTF_8);
            return sign(data, key, algorithm);
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to calculate a request signature: "
                    + e.getMessage(), e);
        }
    }

    protected byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) throws SdkClientException {
        try {
            Mac mac = algorithm.getMac();
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to calculate a request signature: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Hashes the string contents (assumed to be UTF-8) using the SHA-256
     * algorithm.
     *
     * @param text The string to hash.
     * @return The hashed bytes from the specified string.
     * @throws SdkClientException If the hash cannot be computed.
     */
    public byte[] hash(String text) throws SdkClientException {
        return AbstractAwsSigner.doHash(text);
    }

    protected byte[] hash(InputStream input) throws SdkClientException {
        try {
            MessageDigest md = getMessageDigestInstance();
            @SuppressWarnings("resource")
            DigestInputStream digestInputStream = new SdkDigestInputStream(
                    input, md);
            byte[] buffer = new byte[1024];
            while (digestInputStream.read(buffer) > -1) {
                ;
            }
            return digestInputStream.getMessageDigest().digest();
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to compute hash while signing request: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Hashes the binary data using the SHA-256 algorithm.
     *
     * @param data The binary data to hash.
     * @return The hashed bytes from the specified data.
     * @throws SdkClientException If the hash cannot be computed.
     */
    public byte[] hash(byte[] data) throws SdkClientException {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(data);
            return md.digest();
        } catch (Exception e) {
            throw new SdkClientException(
                    "Unable to compute hash while signing request: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Examines the specified query string parameters and returns a
     * canonicalized form.
     * <p>
     * The canonicalized query string is formed by first sorting all the query
     * string parameters, then URI encoding both the key and value and then
     * joining them, in order, separating key value pairs with an '&'.
     *
     * @param parameters The query string parameters to be canonicalized.
     * @return A canonicalized form for the specified query string parameters.
     */
    protected String getCanonicalizedQueryString(Map<String, List<String>> parameters) {

        final SortedMap<String, List<String>> sorted = new TreeMap<String, List<String>>();

        /**
         * Signing protocol expects the param values also to be sorted after url
         * encoding in addition to sorted parameter names.
         */
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            final String encodedParamName = SdkHttpUtils.urlEncode(
                    entry.getKey(), false);
            final List<String> paramValues = entry.getValue();
            final List<String> encodedValues = new ArrayList<String>(
                    paramValues.size());
            for (String value : paramValues) {
                encodedValues.add(SdkHttpUtils.urlEncode(value, false));
            }
            Collections.sort(encodedValues);
            sorted.put(encodedParamName, encodedValues);

        }

        final StringBuilder result = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            for (String value : entry.getValue()) {
                if (result.length() > 0) {
                    result.append("&");
                }
                result.append(entry.getKey())
                      .append("=")
                      .append(value);
            }
        }

        return result.toString();
    }

    protected static int getReadLimit(Aws4SignerRequestParams signerRequestParams) {
        return Optional.ofNullable(signerRequestParams.executionAttributes().getAttribute(REQUEST_CONFIG))
                       .map(RequestConfig::getRequestClientOptions)
                       .map(RequestClientOptions::getReadLimit)
                       .orElse(RequestClientOptions.DEFAULT_STREAM_BUFFER_SIZE);
    }

    protected InputStream getBinaryRequestPayloadStream(InputStream wrapped) {
        try {
            InputStream unwrapped = getContentUnwrapped(wrapped);
            if (unwrapped == null) {
                return new ByteArrayInputStream(new byte[0]);
            }
            if (!unwrapped.markSupported()) {
                throw new SdkClientException("Unable to read request payload to sign request.");
            }
            return unwrapped;
        } catch (AmazonClientException e) {
            throw e;
        } catch (Exception e) {
            throw new SdkClientException("Unable to read request payload to sign request: " + e.getMessage(), e);
        }
    }

    private InputStream getContentUnwrapped(InputStream is) {
        if (is == null) {
            return null;
        }
        // We want to disable the progress reporting when the stream is
        // consumed for signing purpose.
        while (is instanceof ProgressInputStream) {
            ProgressInputStream pris = (ProgressInputStream) is;
            is = pris.getWrappedInputStream();
        }
        return is;
    }

    protected String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            return "/";
        } else {
            String value = urlEncode ? SdkHttpUtils.urlEncode(resourcePath, true) : resourcePath;
            if (value.startsWith("/")) {
                return value;
            } else {
                return "/".concat(value);
            }
        }
    }

    protected String getCanonicalizedEndpoint(URI endpoint) {
        String endpointForStringToSign = StringUtils.lowerCase(endpoint.getHost());
        /*
         * Apache HttpClient will omit the port in the Host header for default
         * port values (i.e. 80 for HTTP and 443 for HTTPS) even if we
         * explicitly specify it, so we need to be careful that we use the same
         * value here when we calculate the string to sign and in the Host
         * header we send in the HTTP request.
         */
        if (SdkHttpUtils.isUsingNonDefaultPort(endpoint)) {
            endpointForStringToSign += ":" + endpoint.getPort();
        }

        return endpointForStringToSign;
    }

    /**
     * Loads the individual access key ID and secret key from the specified credentials, trimming any extra whitespace from the
     * credentials.
     *
     * <p>Returns either a {@link AwsSessionCredentials} or a {@link AwsCredentials} object, depending on the input type.
     *
     * @return A new credentials object with the sanitized credentials.
     */
    protected AwsCredentials sanitizeCredentials(AwsCredentials credentials) {
        String accessKeyId = StringUtils.trim(credentials.accessKeyId());
        String secretKey = StringUtils.trim(credentials.secretAccessKey());

        if (credentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;
            return new AwsSessionCredentials(accessKeyId,
                                             secretKey,
                                             StringUtils.trim(sessionCredentials.sessionToken()));
        }

        return new AwsCredentials(accessKeyId, secretKey);
    }

    /**
     * Returns the current time minus the given offset in seconds.
     * The intent is to adjust the current time in the running JVM to the
     * corresponding wall clock time at AWS for request signing purposes.
     *
     * @param offsetInSeconds offset in seconds
     */
    protected Date getSignatureDate(int offsetInSeconds) {
        return new Date(System.currentTimeMillis() - (1000L * offsetInSeconds));
    }

    /**
     * Adds session credentials to the request given.
     *
     * @param mutableRequest The request to add session credentials information to
     * @param credentials    The session credentials to add to the request
     */
    protected abstract void addSessionCredentials(SdkHttpFullRequest.Builder mutableRequest,
                                                  AwsSessionCredentials credentials);

}
