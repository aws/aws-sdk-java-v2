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

package software.amazon.awssdk.core.internal.util;

import static software.amazon.awssdk.core.HttpChecksumConstant.SIGNING_METHOD;
import static software.amazon.awssdk.core.HttpChecksumConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.core.internal.util.HttpChecksumResolver.getResolvedChecksumSpecs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.HttpChecksumConstant;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.signer.SigningMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class HttpChecksumUtils {

    private static final int CHECKSUM_BUFFER_SIZE = 16 * 1024;

    private HttpChecksumUtils() {
    }

    /**
     * @param algorithmName Checksum Algorithm Name
     * @return Http Checksum header for a given Algorithm.
     */
    public static String httpChecksumHeader(String algorithmName) {
        return String.format("%s-%s", HttpChecksumConstant.HTTP_CHECKSUM_HEADER_PREFIX,
                             StringUtils.lowerCase(algorithmName));
    }

    /**
     * The header based Checksum is computed only if following criteria is met
     * - Flexible checksum is not already computed.
     * -
     * - HeaderChecksumSpecs are defined.
     * - Unsigned Payload request.
     */
    public static boolean isStreamingUnsignedPayload(SdkHttpRequest sdkHttpRequest,
                                                     ExecutionAttributes executionAttributes,
                                                     ChecksumSpecs headerChecksumSpecs,
                                                     boolean isContentStreaming) {
        SigningMethod signingMethodUsed = executionAttributes.getAttribute(SIGNING_METHOD);
        String protocol = sdkHttpRequest.protocol();
        if (isHeaderBasedSigningAuth(signingMethodUsed, protocol)) {
            return false;
        }
        return isUnsignedPayload(signingMethodUsed, protocol, isContentStreaming) && headerChecksumSpecs.isRequestStreaming();
    }

    public static boolean isHeaderBasedSigningAuth(SigningMethod signingMethodUsed, String protocol) {
        switch (signingMethodUsed) {
            case HEADER_BASED_AUTH: {
                return true;
            }
            case PROTOCOL_BASED_UNSIGNED: {
                return "http".equals(protocol);
            }
            default: {
                return false;
            }
        }
    }

    /**
     * @param signingMethod Signing Method.
     * @param protocol The http/https protocol.
     * @return true if Payload signing is resolved to Unsigned payload.
     */
    public static boolean isUnsignedPayload(SigningMethod signingMethod, String protocol, boolean isContentStreaming) {
        switch (signingMethod) {
            case UNSIGNED_PAYLOAD:
                return true;
            case PROTOCOL_STREAMING_SIGNING_AUTH:
                return "https".equals(protocol) || !isContentStreaming;
            case PROTOCOL_BASED_UNSIGNED:
                return "https".equals(protocol);
            default:
                return false;
        }
    }

    /**
     * Computes the Checksum  of the data in the given input stream and returns it as an array of bytes.
     *
     * @param is          InputStream for which checksum needs to be calculated.
     * @param algorithm algorithm that will be used to compute the checksum of input stream.
     * @return Calculated checksum in bytes.
     * @throws IOException I/O errors while reading.
     */
    public static byte[] computeChecksum(InputStream is, Algorithm algorithm) throws IOException {
        SdkChecksum sdkChecksum = SdkChecksum.forAlgorithm(algorithm);
        try (BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] buffer = new byte[CHECKSUM_BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                sdkChecksum.update(buffer, 0, bytesRead);
            }
            return sdkChecksum.getChecksumBytes();
        }
    }


    /**
     *
     * @param executionAttributes Execution attributes defined for the request.
     * @return Optional ChecksumSpec if checksum Algorithm exist for the checksumSpec
     */
    public static Optional<ChecksumSpecs> checksumSpecWithRequestAlgorithm(ExecutionAttributes executionAttributes) {
        ChecksumSpecs resolvedChecksumSpecs = getResolvedChecksumSpecs(executionAttributes);
        if (resolvedChecksumSpecs != null && resolvedChecksumSpecs.algorithm() != null) {
            return Optional.of(resolvedChecksumSpecs);
        }
        return Optional.empty();
    }

    /**
     * Checks if the request header is already updated with Calculated checksum.
     *
     * @param sdkHttpRequest      SdkHttpRequest
     * @return True if the flexible checksum header was already updated.
     */
    public static boolean isHttpChecksumPresent(SdkHttpRequest sdkHttpRequest, ChecksumSpecs checksumSpec) {

        //check for the Direct header Or check if Trailer Header is present.
        return sdkHttpRequest.firstMatchingHeader(checksumSpec.headerName()).isPresent() ||
               isTrailerChecksumPresent(sdkHttpRequest, checksumSpec);
    }

    public static boolean isMd5ChecksumRequired(ExecutionAttributes executionAttributes) {
        ChecksumSpecs resolvedChecksumSpecs = getResolvedChecksumSpecs(executionAttributes);
        if (resolvedChecksumSpecs == null) {
            return false;
        }
        return resolvedChecksumSpecs.algorithm() == null && resolvedChecksumSpecs.isRequestChecksumRequired();

    }

    private static boolean isTrailerChecksumPresent(SdkHttpRequest sdkHttpRequest, ChecksumSpecs checksumSpec) {
        Optional<String> trailerBasedChecksum = sdkHttpRequest.firstMatchingHeader(X_AMZ_TRAILER);
        if (trailerBasedChecksum.isPresent()) {
            return trailerBasedChecksum.filter(checksum -> checksum.equalsIgnoreCase(checksumSpec.headerName())).isPresent();
        }
        return false;
    }

    /**
     * The trailer based Checksum is computed only if following criteria is met
     * - Flexible checksum is not already computed.
     * - Streaming  Unsigned Payload defined.
     * - Unsigned Payload request.
     */
    public static boolean isTrailerBasedFlexibleChecksumComputed(SdkHttpRequest sdkHttpRequest,
                                                                 ExecutionAttributes executionAttributes,
                                                                 ChecksumSpecs checksumSpecs,
                                                                 boolean hasRequestBody,
                                                                 boolean isContentStreaming) {
        return hasRequestBody &&
               !HttpChecksumUtils.isHttpChecksumPresent(sdkHttpRequest, checksumSpecs) &&
               HttpChecksumUtils.isStreamingUnsignedPayload(sdkHttpRequest, executionAttributes,
                                                            checksumSpecs, isContentStreaming);
    }

    /**
     *
     * @param executionAttributes Execution attributes for the request.
     * @param httpRequest Http Request.
     * @param clientType Client Type for which the Trailer checksum is appended.
     * @param checksumSpecs Checksum specs for the request.
     * @param hasRequestBody Request body.
     * @return True if Trailer checksum needs to be calculated and appended.
     */
    public static boolean isTrailerBasedChecksumForClientType(
        ExecutionAttributes executionAttributes, SdkHttpRequest httpRequest,
        ClientType clientType, ChecksumSpecs checksumSpecs, boolean hasRequestBody, boolean isContentSteaming) {

        ClientType actualClientType = executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_TYPE);
        return actualClientType.equals(clientType) &&
               checksumSpecs != null &&
               HttpChecksumUtils.isTrailerBasedFlexibleChecksumComputed(
                   httpRequest, executionAttributes, checksumSpecs, hasRequestBody, isContentSteaming);
    }

    /**
     * Loops through the Supported list of checksum for the operation, and gets the Header value for the checksum header.
     * @param sdkHttpResponse response from service.
     * @param resolvedChecksumSpecs Resolved checksum specification for the operation.
     * @return Algorithm and its corresponding checksum value as sent by the service.
     */
    public static Pair<Algorithm, String> getAlgorithmChecksumValuePair(SdkHttpResponse sdkHttpResponse,
                                                                        ChecksumSpecs resolvedChecksumSpecs) {
        return resolvedChecksumSpecs.responseValidationAlgorithms().stream().map(
            algorithm -> {
                Optional<String> firstMatchingHeader = sdkHttpResponse.firstMatchingHeader(httpChecksumHeader(algorithm.name()));
                return firstMatchingHeader.map(s -> Pair.of(algorithm, s)).orElse(null);
            }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     *
     * @param resolvedChecksumSpecs Resolved checksum specification for the operation.
     * @return True is Response is to be validated for checksum checks.
     */
    public static boolean isHttpChecksumValidationEnabled(ChecksumSpecs resolvedChecksumSpecs) {
        return resolvedChecksumSpecs != null &&
               resolvedChecksumSpecs.isValidationEnabled() &&
               resolvedChecksumSpecs.responseValidationAlgorithms() != null;
    }


    public static byte[] longToByte(Long input) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(input);
        return buffer.array();
    }

}
