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

import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC32C;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.CRC64NVME;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA1;
import static software.amazon.awssdk.checksums.DefaultChecksumAlgorithm.SHA256;
import static software.amazon.awssdk.core.HttpChecksumConstant.HEADER_FOR_TRAILER_REFERENCE;
import static software.amazon.awssdk.core.HttpChecksumConstant.HTTP_CHECKSUM_HEADER_PREFIX;
import static software.amazon.awssdk.core.HttpChecksumConstant.SIGNING_METHOD;
import static software.amazon.awssdk.core.HttpChecksumConstant.X_AMZ_TRAILER;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS;
import static software.amazon.awssdk.core.internal.util.HttpChecksumResolver.getResolvedChecksumSpecs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.signer.SigningMethod;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.ClassLoaderHelper;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public final class HttpChecksumUtils {
    private static final Logger log = Logger.loggerFor(HttpChecksumUtils.class);

    private static final int CHECKSUM_BUFFER_SIZE = 16 * 1024;

    private static final ImmutableMap<ChecksumAlgorithm, Algorithm> NEW_CHECKSUM_TO_LEGACY = ImmutableMap.of(
        SHA256, Algorithm.SHA256,
        SHA1, Algorithm.SHA1,
        CRC32, Algorithm.CRC32,
        CRC32C, Algorithm.CRC32C,
        CRC64NVME, Algorithm.CRC64NVME
    );

    private static final ImmutableMap<Algorithm, ChecksumAlgorithm> LEGACY_CHECKSUM_TO_NEW = ImmutableMap.of(
        Algorithm.SHA256, SHA256,
        Algorithm.SHA1, SHA1,
        Algorithm.CRC32, CRC32,
        Algorithm.CRC32C, CRC32C,
        Algorithm.CRC64NVME, CRC64NVME
    );

    private static Lazy<Boolean> isCrc64NvmeAvailable = new Lazy<>(() -> {
        try {
            ClassLoaderHelper.loadClass("software.amazon.awssdk.crt.checksums.CRC64NVME", false);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    });

    private HttpChecksumUtils() {
    }

    public static Algorithm toLegacyChecksumAlgorithm(ChecksumAlgorithm checksumAlgorithm) {
        return NEW_CHECKSUM_TO_LEGACY.get(checksumAlgorithm);
    }

    public static ChecksumAlgorithm toNewChecksumAlgorithm(Algorithm checksumAlgorithm) {
        return LEGACY_CHECKSUM_TO_NEW.get(checksumAlgorithm);
    }

    /**
     * @param algorithmName Checksum Algorithm Name
     * @return Http Checksum header for a given Algorithm.
     */
    public static String httpChecksumHeader(String algorithmName) {
        return HTTP_CHECKSUM_HEADER_PREFIX + "-" + StringUtils.lowerCase(algorithmName);
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

    public static boolean hasLegacyChecksumRequiredTrait(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED) != null;
    }

    /**
     * HTTP checksum calculation is needed if one of the following conditions is met:
     * 1. checksum is required per legacy httpRequired trait or the new HttpChecksum trait OR
     * 2. user has specified a checksum algorithm OR
     * 3. checksum is optional per new HttpChecksum trait AND RequestChecksumCalculation == when_supported
     *
     * HTTP checksum calculation is not needed if one of the following conditions is met:
     * 1. the operation does not have legacy httpRequired trait or the new HttpChecksum trait OR
     * 2. user has provided a checksum value (any header prefixed with "x-amz-checksum-") OR
     * 3. checksum is not required AND RequestChecksumCalculation == when_required
     */
    public static boolean isHttpChecksumCalculationNeeded(SdkHttpFullRequest.Builder request,
                                                          ExecutionAttributes executionAttributes) {

        ChecksumSpecs checksumSpecs = executionAttributes.getAttribute(RESOLVED_CHECKSUM_SPECS);
        boolean hasFlexibleChecksumTrait = checksumSpecs != null;

        if (!hasLegacyChecksumRequiredTrait(executionAttributes) && !hasFlexibleChecksumTrait) {
            return false;
        }

        if (requestAlreadyHasChecksum(request)) {
            return false;
        }

        boolean checksumAlgorithmSpecified =
            hasFlexibleChecksumTrait && checksumSpecs.algorithmV2() != null;

        if (checksumAlgorithmSpecified) {
            return true;
        }

        boolean isHttpChecksumRequired =
            hasLegacyChecksumRequiredTrait(executionAttributes) ||
            hasFlexibleChecksumTrait && checksumSpecs.isRequestChecksumRequired();

        if (isHttpChecksumRequired) {
            return true;
        }

        RequestChecksumCalculation requestChecksumCalculation =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION);

        return requestChecksumCalculation == RequestChecksumCalculation.WHEN_SUPPORTED;
    }

    private static boolean requestAlreadyHasChecksum(SdkHttpFullRequest.Builder request) {
        if (request.firstMatchingHeader(HEADER_FOR_TRAILER_REFERENCE).isPresent()) {
            return true;
        }

        return request.anyMatchingHeader(k -> k.startsWith(HTTP_CHECKSUM_HEADER_PREFIX));
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
     * Loops through the supported list of checksum for the operation, and gets the header value for the checksum header.
     * @param sdkHttpResponse response from service.
     * @param resolvedChecksumSpecs Resolved checksum specification for the operation.
     * @return Algorithm and its corresponding checksum value as sent by the service.
     */
    public static Pair<ChecksumAlgorithm, String> getAlgorithmChecksumValuePair(SdkHttpResponse sdkHttpResponse,
                                                                                ChecksumSpecs resolvedChecksumSpecs) {
        for (ChecksumAlgorithm checksumAlgorithm : resolvedChecksumSpecs.responseValidationAlgorithmsV2()) {
            Optional<String> firstMatchingHeader =
                sdkHttpResponse.firstMatchingHeader(httpChecksumHeader(checksumAlgorithm.algorithmId()));

            if (firstMatchingHeader.isPresent()) {
                if (checksumAlgorithm.equals(CRC64NVME) && !isCrc64NvmeAvailable.getValue()) {
                    log.debug(() -> "Skip CRC64NVME checksum validation because CRT is not on the classpath and CRC64NVME is "
                                    + "not available");
                    continue;
                }
                return Pair.of(checksumAlgorithm, firstMatchingHeader.get());
            }
        }
        return null;
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
