/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.putObjectChecksumEnabled;
import static software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator.validatePutObjectChecksum;

import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.checksums.ChecksumCalculatingInputStream;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingInputStream;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@SdkInternalApi
public final class SyncChecksumValidationInterceptor implements ExecutionInterceptor {

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {

        if (putObjectChecksumEnabled(context.request(), SYNC, executionAttributes, context.httpRequest())
            && context.requestBody().isPresent()) {
            SdkChecksum checksum = new Md5Checksum();

            ChecksumCalculatingInputStream is = new ChecksumCalculatingInputStream(context.requestBody()
                                                                                          .get()
                                                                                          .contentStreamProvider()
                                                                                          .newStream(),
                                                                                   checksum);
            executionAttributes.putAttribute(CHECKSUM, checksum);
            return Optional.of(RequestBody.fromContentProvider(() -> is,
                                                               context.requestBody().get().contentLength(),
                                                               context.requestBody().get().contentType()));
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
        if (putObjectChecksumEnabled(context.request(), SYNC, executionAttributes, context.httpResponse())) {
            validatePutObjectChecksum((PutObjectResponse) context.response(), executionAttributes);
        }
    }
}
