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

package software.amazon.awssdk.core.internal.interceptor;

import static software.amazon.awssdk.core.HttpChecksumConstant.HTTP_CHECKSUM_VALUE;
import static software.amazon.awssdk.core.HttpChecksumConstant.SIGNING_METHOD;
import static software.amazon.awssdk.core.internal.util.HttpChecksumResolver.getResolvedChecksumSpecs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Implements the "HttpChecksum" C2J trait for a request.
 * HttpChecksum is added in the header only in following cases
 * <ol>
 *     <li>non streaming payload and Unsigned Payload </li>
 *     <li>non streaming payload and Header-based Signing auth</li>
 *     <li>streaming payload and Header-based Signing auth</li>
 * </ol>
 * This interceptor will inject the Http checksum only for case 1 as above i.e. for unsigned payloads.
 * For the other two cases the http checksum will be injected by the signers
 */
@SdkInternalApi
public class HttpChecksumInHeaderInterceptor implements ExecutionInterceptor {

    @Override
    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        ChecksumSpecs headerChecksumSpecs = HttpChecksumUtils.checksumSpecWithRequestAlgorithm(executionAttributes).orElse(null);

        if (shouldSkipHttpChecksumInHeader(context, executionAttributes, headerChecksumSpecs)) {
            return;
        }
        Optional<RequestBody> syncContent = context.requestBody();
        syncContent.ifPresent(
            requestBody -> saveContentChecksum(requestBody, executionAttributes, headerChecksumSpecs.algorithm()));
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        ChecksumSpecs checksumSpecs = getResolvedChecksumSpecs(executionAttributes);

        if (shouldSkipHttpChecksumInHeader(context, executionAttributes, checksumSpecs)) {
            return context.httpRequest();
        }

        String httpChecksumValue = executionAttributes.getAttribute(HTTP_CHECKSUM_VALUE);
        if (httpChecksumValue != null) {
            return context.httpRequest().copy(r -> r.putHeader(checksumSpecs.headerName(), httpChecksumValue));
        }
        return context.httpRequest();

    }

    /**
     * Calculates the checksumSpecs of the provided request (and base64 encodes it), storing the result in
     * executionAttribute "HttpChecksumValue".
     *
     * <p>Note: This assumes that the content stream provider can create multiple new streams. If it only supports one (e.g. with
     * an input stream that doesn't support mark/reset), we could consider buffering the content in memory here and updating the
     * request body to use that buffered content. We obviously don't want to do that for giant streams, so we haven't opted to do
     * that yet.
     */
    private static void saveContentChecksum(RequestBody requestBody, ExecutionAttributes executionAttributes,
                                            Algorithm algorithm) {
        try {
            String payloadChecksum = BinaryUtils.toBase64(HttpChecksumUtils.computeChecksum(
                requestBody.contentStreamProvider().newStream(), algorithm));
            executionAttributes.putAttribute(HTTP_CHECKSUM_VALUE, payloadChecksum);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean shouldSkipHttpChecksumInHeader(Context.AfterMarshalling context, ExecutionAttributes executionAttributes,
                                                   ChecksumSpecs headerChecksumSpecs) {
        return headerChecksumSpecs == null ||
               headerChecksumSpecs.algorithm() == null ||
               HttpChecksumUtils.isHttpChecksumPresent(context.httpRequest(), headerChecksumSpecs) ||
               !HttpChecksumUtils.isUnsignedPayload(
                   executionAttributes.getAttribute(SIGNING_METHOD),
                   context.httpRequest().protocol(),
                   context.requestBody().map(requestBody -> requestBody.contentStreamProvider() != null).orElse(false)) ||
               headerChecksumSpecs.isRequestStreaming();
    }
}