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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpRequest;
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
public class HttpChecksumRequiredInterceptor implements ExecutionInterceptor {
    private static final ExecutionAttribute<String> CONTENT_MD5_VALUE = new ExecutionAttribute<>("ContentMd5");

    @Override
    public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
        boolean isHttpChecksumRequired = isHttpChecksumRequired(executionAttributes);
        boolean requestAlreadyHasMd5 = context.httpRequest().firstMatchingHeader(Header.CONTENT_MD5).isPresent();

        Optional<RequestBody> syncContent = context.requestBody();
        Optional<AsyncRequestBody> asyncContent = context.asyncRequestBody();

        if (!isHttpChecksumRequired || requestAlreadyHasMd5) {
            return;
        }

        if (asyncContent.isPresent()) {
            throw new IllegalArgumentException("This operation requires a content-MD5 checksum, but one cannot be calculated "
                                               + "for non-blocking content.");
        }

        syncContent.ifPresent(requestBody -> saveContentMd5(requestBody, executionAttributes));
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        String contentMd5 = executionAttributes.getAttribute(CONTENT_MD5_VALUE);
        if (contentMd5 != null) {
            return context.httpRequest().copy(r -> r.putHeader(Header.CONTENT_MD5, contentMd5));
        }
        return context.httpRequest();
    }

    private boolean isHttpChecksumRequired(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED) != null;
    }

    /**
     * Calculates the MD5 checksum of the provided request (and base64 encodes it), storing the result in
     * {@link #CONTENT_MD5_VALUE}.
     *
     * <p>Note: This assumes that the content stream provider can create multiple new streams. If it only supports one (e.g. with
     * an input stream that doesn't support mark/reset), we could consider buffering the content in memory here and updating the
     * request body to use that buffered content. We obviously don't want to do that for giant streams, so we haven't opted to do
     * that yet.
     */
    private void saveContentMd5(RequestBody requestBody, ExecutionAttributes executionAttributes) {
        try {
            String payloadMd5 = Md5Utils.md5AsBase64(requestBody.contentStreamProvider().newStream());
            executionAttributes.putAttribute(CONTENT_MD5_VALUE, payloadMd5);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
