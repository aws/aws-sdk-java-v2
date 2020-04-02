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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.io.SdkDigestInputStream;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Abstract base class for AWS signing protocol implementations. Provides
 * utilities commonly needed by signing protocols such as computing
 * canonicalized host names, query string parameters, etc.
 * <p>
 * Not intended to be sub-classed by developers.
 */
@SdkInternalApi
public abstract class AbstractAwsSigner implements Signer {

    private static final ThreadLocal<MessageDigest> SHA256_MESSAGE_DIGEST;

    static {
        SHA256_MESSAGE_DIGEST = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw SdkClientException.builder()
                                        .message("Unable to get SHA256 Function" + e.getMessage())
                                        .cause(e)
                                        .build();
            }
        });
    }

    private static byte[] doHash(String text) throws SdkClientException {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(text.getBytes(StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to compute hash while signing request: " + e.getMessage())
                                    .cause(e)
                                    .build();
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
            return BinaryUtils.toBase64(signature);
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to calculate a request signature: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    protected byte[] signWithMac(String stringData, Mac mac) {
        try {
            return mac.doFinal(stringData.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to calculate a request signature: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    protected byte[] sign(String stringData, byte[] key,
                       SigningAlgorithm algorithm) throws SdkClientException {
        try {
            byte[] data = stringData.getBytes(StandardCharsets.UTF_8);
            return sign(data, key, algorithm);
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to calculate a request signature: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    protected byte[] sign(byte[] data, byte[] key, SigningAlgorithm algorithm) throws SdkClientException {
        try {
            Mac mac = algorithm.getMac();
            mac.init(new SecretKeySpec(key, algorithm.toString()));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to calculate a request signature: " + e.getMessage())
                                    .cause(e)
                                    .build();
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
    static byte[] hash(String text) throws SdkClientException {
        return AbstractAwsSigner.doHash(text);
    }

    byte[] hash(InputStream input) throws SdkClientException {
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
            throw SdkClientException.builder()
                                    .message("Unable to compute hash while signing request: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    /**
     * Hashes the binary data using the SHA-256 algorithm.
     *
     * @param data The binary data to hash.
     * @return The hashed bytes from the specified data.
     * @throws SdkClientException If the hash cannot be computed.
     */
    byte[] hash(byte[] data) throws SdkClientException {
        try {
            MessageDigest md = getMessageDigestInstance();
            md.update(data);
            return md.digest();
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to compute hash while signing request: " + e.getMessage())
                                    .cause(e)
                                    .build();
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
     * @param parameters The query string parameters to be canonicalized.
     * @return A canonicalized form for the specified query string parameters.
     */
    protected String getCanonicalizedQueryString(Map<String, List<String>> parameters) {

        SortedMap<String, List<String>> sorted = new TreeMap<>();

        /**
         * Signing protocol expects the param values also to be sorted after url
         * encoding in addition to sorted parameter names.
         */
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String encodedParamName = SdkHttpUtils.urlEncode(entry.getKey());
            List<String> paramValues = entry.getValue();
            List<String> encodedValues = new ArrayList<>(paramValues.size());
            for (String value : paramValues) {
                String encodedValue = SdkHttpUtils.urlEncode(value);

                // Null values should be treated as empty for the purposes of signing, not missing.
                // For example "?foo=" instead of "?foo".
                String signatureFormattedEncodedValue = encodedValue == null ? "" : encodedValue;

                encodedValues.add(signatureFormattedEncodedValue);
            }
            Collections.sort(encodedValues);
            sorted.put(encodedParamName, encodedValues);

        }

        return SdkHttpUtils.flattenQueryParameters(sorted).orElse("");
    }

    protected InputStream getBinaryRequestPayloadStream(ContentStreamProvider streamProvider) {
        try {
            if (streamProvider == null) {
                return new ByteArrayInputStream(new byte[0]);
            }
            return streamProvider.newStream();
        } catch (SdkClientException e) {
            throw e;
        } catch (Exception e) {
            throw SdkClientException.builder()
                                    .message("Unable to read request payload to sign request: " + e.getMessage())
                                    .cause(e)
                                    .build();
        }
    }

    String getCanonicalizedResourcePath(String resourcePath, boolean urlEncode) {
        if (StringUtils.isEmpty(resourcePath)) {
            return "/";
        } else {
            String value = urlEncode ? SdkHttpUtils.urlEncodeIgnoreSlashes(resourcePath) : resourcePath;
            if (value.startsWith("/")) {
                return value;
            } else {
                return "/".concat(value);
            }
        }
    }

    protected String getCanonicalizedEndpoint(SdkHttpFullRequest request) {
        String endpointForStringToSign = StringUtils.lowerCase(request.host());

        // Omit the port from the endpoint if we're using the default port for the protocol. Some HTTP clients (ie. Apache) don't
        // allow you to specify it in the request, so we're standardizing around not including it. See SdkHttpRequest#port().
        if (!SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())) {
            endpointForStringToSign += ":" + request.port();
        }

        return endpointForStringToSign;
    }

    /**
     * Loads the individual access key ID and secret key from the specified credentials, trimming any extra whitespace from the
     * credentials.
     *
     * <p>Returns either a {@link AwsSessionCredentials} or a {@link AwsBasicCredentials} object, depending on the input type.
     *
     * @return A new credentials object with the sanitized credentials.
     */
    protected AwsCredentials sanitizeCredentials(AwsCredentials credentials) {
        String accessKeyId = StringUtils.trim(credentials.accessKeyId());
        String secretKey = StringUtils.trim(credentials.secretAccessKey());

        if (credentials instanceof AwsSessionCredentials) {
            AwsSessionCredentials sessionCredentials = (AwsSessionCredentials) credentials;
            return AwsSessionCredentials.create(accessKeyId,
                                                secretKey,
                                                StringUtils.trim(sessionCredentials.sessionToken()));
        }

        return AwsBasicCredentials.create(accessKeyId, secretKey);
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
