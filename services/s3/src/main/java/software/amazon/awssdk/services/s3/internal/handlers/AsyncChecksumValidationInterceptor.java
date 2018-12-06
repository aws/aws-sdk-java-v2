/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.checksums.ChecksumCalculatingAsyncRequestBody;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingPublisher;
import software.amazon.awssdk.services.s3.checksums.ChecksumsEnabledValidator;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.internal.Base16Lower;

@SdkInternalApi
public class AsyncChecksumValidationInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<SdkChecksum> CHECKSUM = new ExecutionAttribute("checksum");

    @Override
    public Optional<AsyncRequestBody> modifyAsyncHttpContent(Context.ModifyHttpRequest context,
                                                             ExecutionAttributes executionAttributes) {

        boolean checksumValidationEnabled = ChecksumsEnabledValidator.trailingChecksumsEnabled(ASYNC,
                                                                                               executionAttributes,
                                                                                               context.httpRequest().headers());

        if (context.request() instanceof PutObjectRequest && checksumValidationEnabled) {
            SdkChecksum checksum = new Md5Checksum();
            executionAttributes.putAttribute(CHECKSUM, checksum);
            return Optional.of(new ChecksumCalculatingAsyncRequestBody(context.asyncRequestBody().get(), checksum));
        }

        return context.asyncRequestBody();
    }

    @Override
    public Optional<Publisher<ByteBuffer>> modifyAsyncHttpResponseContent(Context.ModifyHttpResponse context,
                                                                ExecutionAttributes executionAttributes) {

        boolean checksumValidationEnabled = ChecksumsEnabledValidator.trailingChecksumsEnabled(ASYNC,
                                                                                               executionAttributes,
                                                                                               context.httpRequest().headers());

        if (context.request() instanceof GetObjectRequest && checksumValidationEnabled) {
            int contentLength = Integer.parseInt(context.httpResponse().firstMatchingHeader("Content-Length").orElse("0"));
            SdkChecksum checksum = new Md5Checksum();
            executionAttributes.putAttribute(CHECKSUM, checksum);
            return Optional.of(new ChecksumValidatingPublisher(context.responsePublisher().get(), checksum, contentLength));
        }

        return context.responsePublisher();
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {

        boolean checksumValidationEnabled = ChecksumsEnabledValidator.trailingChecksumsEnabled(ASYNC,
                                                                                               executionAttributes,
                                                                                               context.httpResponse().headers());

        if (context.response() instanceof PutObjectResponse && checksumValidationEnabled) {
            validatePutObjectChecksum(context.response(), executionAttributes);
        }
    }

    private void validatePutObjectChecksum(SdkResponse sdkResponse, ExecutionAttributes executionAttributes) {
        SdkChecksum checksum = executionAttributes.getAttribute(CHECKSUM);
        PutObjectResponse response = (PutObjectResponse) sdkResponse;

        if (response.eTag() != null) {
            String contentMd5 = BinaryUtils.toBase64(checksum.getChecksumBytes());
            byte[] digest = BinaryUtils.fromBase64(contentMd5);
            byte[] ssHash = Base16Lower.decode(response.eTag().replace("\"", ""));

            if (!Arrays.equals(digest, ssHash)) {
                throw SdkClientException.create("Data read has a different checksum than expected.");
            }
        }
    }
}
