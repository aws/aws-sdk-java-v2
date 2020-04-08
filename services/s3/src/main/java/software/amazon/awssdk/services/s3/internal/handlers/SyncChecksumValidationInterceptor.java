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

import static software.amazon.awssdk.core.ClientType.SYNC;
import static software.amazon.awssdk.services.s3.checksums.ChecksumConstant.CONTENT_LENGTH_HEADER;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.CHECKSUM;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerResponse;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.responseChecksumIsValid;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.shouldRecordChecksum;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.validatePutObjectChecksum;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.checksums.ChecksumCalculatingInputStream;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingInputStream;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SdkInternalApi
public final class SyncChecksumValidationInterceptor implements ExecutionInterceptor {
    private static ExecutionAttribute<Boolean> SYNC_RECORDING_CHECKSUM = new ExecutionAttribute<>("syncRecordingChecksum");

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {
        if (shouldRecordChecksum(context.request(), SYNC, executionAttributes, context.httpRequest())
            && context.requestBody().isPresent()) {
            SdkChecksum checksum = new Md5Checksum();
            executionAttributes.putAttribute(CHECKSUM, checksum);
            executionAttributes.putAttribute(SYNC_RECORDING_CHECKSUM, true);

            RequestBody requestBody = context.requestBody().get();

            ChecksumCalculatingStreamProvider streamProvider =
                new ChecksumCalculatingStreamProvider(requestBody.contentStreamProvider(), checksum);

            return Optional.of(RequestBody.fromContentProvider(streamProvider,
                                                               requestBody.contentLength(),
                                                               requestBody.contentType()));
        }

        return context.requestBody();
    }

    @Override
    public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                           ExecutionAttributes executionAttributes) {
        if (getObjectChecksumEnabledPerResponse(context.request(), context.httpResponse())
            && context.responseBody().isPresent()) {

            SdkChecksum checksum = new Md5Checksum();

            long contentLength = context.httpResponse()
                                        .firstMatchingHeader(CONTENT_LENGTH_HEADER)
                                        .map(Long::parseLong)
                                        .orElse(0L);

            if (contentLength > 0) {
                return Optional.of(new ChecksumValidatingInputStream(context.responseBody().get(), checksum, contentLength));
            }
        }

        return context.responseBody();
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {
        boolean recordingChecksum = Boolean.TRUE.equals(executionAttributes.getAttribute(SYNC_RECORDING_CHECKSUM));
        boolean responseChecksumIsValid = responseChecksumIsValid(context.httpResponse());

        if (recordingChecksum && responseChecksumIsValid) {
            validatePutObjectChecksum((PutObjectResponse) context.response(), executionAttributes);
        }
    }

    static final class ChecksumCalculatingStreamProvider implements ContentStreamProvider {
        private final SdkChecksum checksum;
        private InputStream currentStream;
        private final ContentStreamProvider underlyingInputStreamProvider;

        ChecksumCalculatingStreamProvider(ContentStreamProvider underlyingInputStreamProvider, SdkChecksum checksum) {
            this.underlyingInputStreamProvider = underlyingInputStreamProvider;
            this.checksum = checksum;
        }

        @Override
        public InputStream newStream() {
            closeCurrentStream();
            currentStream = invokeSafely(() -> new ChecksumCalculatingInputStream(underlyingInputStreamProvider.newStream(),
                                                                                  checksum));
            return currentStream;
        }

        private void closeCurrentStream() {
            checksum.reset();
            if (currentStream != null) {
                invokeSafely(currentStream::close);
                currentStream = null;
            }
        }
    }

}
