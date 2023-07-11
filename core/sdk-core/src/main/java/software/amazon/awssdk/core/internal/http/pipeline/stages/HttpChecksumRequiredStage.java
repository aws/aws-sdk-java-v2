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

import java.io.IOException;
import java.io.UncheckedIOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.util.HttpChecksumUtils;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.Md5Utils;

/**
 * Implements the "httpChecksumRequired" C2J trait. Operations with that trait applied will automatically include a "Content-MD5"
 * header, containing a checksum of the payload.
 *
 * <p>This is NOT supported for asynchronous HTTP content, which is currently only used for streaming upload operations. If such
 * operations are added in the future, we'll have to find a way to support them in a non-blocking manner. That will likely require
 * interface changes of some sort, because it's not currently possible to do a non-blocking update to request headers.
 */
@SdkInternalApi
public class HttpChecksumRequiredStage implements MutableRequestToRequestPipeline {

    /**
     * Calculates the MD5 checksum of the provided request (and base64 encodes it), and adds the header to the request.
     *
     * <p>Note: This assumes that the content stream provider can create multiple new streams. If it only supports one (e.g. with
     * an input stream that doesn't support mark/reset), we could consider buffering the content in memory here and updating the
     * request body to use that buffered content. We obviously don't want to do that for giant streams, so we haven't opted to do
     * that yet.
     */
    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request, RequestExecutionContext context)
            throws Exception {
        boolean isHttpChecksumRequired = isHttpChecksumRequired(context.executionAttributes());
        boolean requestAlreadyHasMd5 = request.firstMatchingHeader(Header.CONTENT_MD5).isPresent();

        if (!isHttpChecksumRequired || requestAlreadyHasMd5) {
            return request;
        }

        if (context.requestProvider() != null) {
            throw new IllegalArgumentException("This operation requires a content-MD5 checksum, but one cannot be calculated "
                                               + "for non-blocking content.");
        }

        if (context.executionContext().interceptorContext().requestBody().isPresent()) {
            try {
                String payloadMd5 = Md5Utils.md5AsBase64(request.contentStreamProvider().newStream());
                return request.putHeader(Header.CONTENT_MD5, payloadMd5);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return request;
    }

    private boolean isHttpChecksumRequired(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED) != null
               || HttpChecksumUtils.isMd5ChecksumRequired(executionAttributes);
    }
}
