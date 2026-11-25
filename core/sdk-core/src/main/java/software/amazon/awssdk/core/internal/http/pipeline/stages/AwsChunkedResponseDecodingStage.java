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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedResponseDecodingUtils;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.io.AwsChunkedResponseDecoder;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;

/**
 * Decodes an aws-chunked response body and rewrites its framing headers. Runs after the read-only
 * {@code afterTransmission} interceptors and before any interceptor that can modify the response, so those only see the
 * decoded body and headers.
 */
@SdkInternalApi
public class AwsChunkedResponseDecodingStage
    implements RequestPipeline<Pair<SdkHttpFullRequest, SdkHttpFullResponse>, Pair<SdkHttpFullRequest, SdkHttpFullResponse>> {
    private static final Logger LOG = Logger.loggerFor(AwsChunkedResponseDecodingStage.class);

    @Override
    public Pair<SdkHttpFullRequest, SdkHttpFullResponse> execute(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                                                 RequestExecutionContext context) {
        ExecutionAttributes executionAttributes = context.executionAttributes();
        // Clear trailers from an earlier attempt, so they are never read for this response
        executionAttributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS, null);
        try {
            return decode(input, context);
        } catch (RuntimeException e) {
            // The body has not been handed to a later stage yet, so release the connection here
            input.right().content().ifPresent(AwsChunkedResponseDecodingStage::abortQuietly);
            throw e;
        }
    }

    private static Pair<SdkHttpFullRequest, SdkHttpFullResponse> decode(Pair<SdkHttpFullRequest, SdkHttpFullResponse> input,
                                                                        RequestExecutionContext context) {
        SdkHttpFullResponse response = input.right();
        ExecutionAttributes executionAttributes = context.executionAttributes();
        if (!AwsChunkedResponseDecodingUtils.shouldDecode(response, executionAttributes)) {
            return input;
        }

        long decodedContentLength = AwsChunkedResponseDecodingUtils.decodedContentLength(response);
        SdkHttpFullResponse decoded = AwsChunkedResponseDecodingUtils.rewriteHeaders(response, decodedContentLength);

        Optional<AbortableInputStream> body = response.content();
        if (body.isPresent()) {
            AwsChunkedResponseDecoder decoder =
                new AwsChunkedResponseDecoder(body.get(), decodedContentLength,
                                              AwsChunkedResponseDecodingUtils.declaredTrailers(response));
            executionAttributes.putAttribute(SdkInternalExecutionAttribute.AWS_CHUNKED_RESPONSE_TRAILERS, decoder::trailers);
            decoded = decoded.toBuilder().content(AbortableInputStream.create(decoder)).build();
        }

        SdkHttpFullResponse result = decoded;
        context.executionContext().interceptorContext(
            context.executionContext().interceptorContext().copy(b -> b.httpResponse(result)
                                                                       .responseBody(result.content().orElse(null))));
        return Pair.of(input.left(), result);
    }

    private static void abortQuietly(AbortableInputStream body) {
        try {
            body.abort();
        } catch (Throwable e) {
            LOG.debug(() -> "Failed to abort the response body after an aws-chunked decoding failure.", e);
        }
        IoUtils.closeQuietlyV2(body, LOG);
    }
}
