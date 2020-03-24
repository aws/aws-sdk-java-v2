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

package software.amazon.awssdk.services.s3.internal.handlers;

import static software.amazon.awssdk.core.ClientType.ASYNC;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CONTENT_LENGTH_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.CHECKSUM;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerResponse;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.responseChecksumIsValid;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.shouldRecordChecksum;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.validatePutObjectChecksum;

import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.checksums.ChecksumCalculatingAsyncRequestBody;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingPublisher;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SdkInternalApi
public final class AsyncChecksumValidationInterceptor implements ExecutionInterceptor {
    private static ExecutionAttribute<Boolean> ASYNC_RECORDING_CHECKSUM = new ExecutionAttribute<>("asyncRecordingChecksum");

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context,
                                                             ExecutionAttributes executionAttributes) {
        boolean shouldRecordChecksum = shouldRecordChecksum(context.request(), ASYNC, executionAttributes, context.httpRequest());

        if (shouldRecordChecksum && context.asyncRequestBody().isPresent()) {
            SdkChecksum checksum = new Md5Checksum();
            executionAttributes.putAttribute(ASYNC_RECORDING_CHECKSUM, true);
            executionAttributes.putAttribute(CHECKSUM, checksum);
            return Optional.of(new ChecksumCalculatingAsyncRequestBody(context.asyncRequestBody().get(), checksum));
        }

        return context.asyncRequestBody();
    }

    @Override
    public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                          ExecutionAttributes executionAttributes) {
        if (getObjectChecksumEnabledPerResponse(context.request(), context.httpResponse())
            && context.responsePublisher().isPresent()) {
            long contentLength = context.httpResponse()
                                        .firstMatchingHeader(CONTENT_LENGTH_HEADER)
                                        .map(Long::parseLong)
                                        .orElse(0L);

            SdkChecksum checksum = new Md5Checksum();
            executionAttributes.putAttribute(CHECKSUM, checksum);
            if (contentLength > 0) {
                return Optional.of(new ChecksumValidatingPublisher(context.responsePublisher().get(), checksum, contentLength));
            }
        }

        return context.responsePublisher();
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        boolean recordingChecksum = Boolean.TRUE.equals(executionAttributes.getAttribute(ASYNC_RECORDING_CHECKSUM));
        boolean responseChecksumIsValid = responseChecksumIsValid(context.httpResponse());

        if (recordingChecksum && responseChecksumIsValid) {
            validatePutObjectChecksum((PutObjectResponse) context.response(), executionAttributes);
        }
    }
}
