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

package software.amazon.awssdk.services.s3.handlers;

import static software.amazon.awssdk.core.ClientType.SYNC;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.checksums.Md5Checksum;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.checksums.ChecksumCalculatingInputStream;
import software.amazon.awssdk.services.s3.checksums.ChecksumValidatingInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.internal.Base16Lower;

@SdkInternalApi
public class SyncChecksumValidationInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<SdkChecksum> CHECKSUM = new ExecutionAttribute("checksum");

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {

        if (context.request() instanceof GetObjectRequest && checksumValidationEnabled(executionAttributes)) {
            return context.httpRequest().toBuilder().putHeader("x-amz-te", "append-md5").build();
        }

        return context.httpRequest();
    }

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {

        boolean checksumValidationEnabled = checksumValidationEnabled(executionAttributes);

        if (context.request() instanceof PutObjectRequest && checksumValidationEnabled) {
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

        if (context.request() instanceof GetObjectRequest && checksumValidationEnabled(executionAttributes)) {
            SdkHttpResponse originalResponse = context.httpResponse();
            SdkChecksum checksum = new Md5Checksum();

            int contentLength = Integer.valueOf(context.httpResponse().firstMatchingHeader("Content-Length").orElse("0"));

            if (contentLength > 0) {
                return Optional.of(new ChecksumValidatingInputStream(context.responseBody().get(), checksum, contentLength));
            }
        }

        return context.responseBody();
    }

    @Override
    public void afterUnmarshalling(Context.AfterUnmarshalling context, ExecutionAttributes executionAttributes) {

        if (context.response() instanceof PutObjectResponse && checksumValidationEnabled(executionAttributes)) {
            PutObjectResponse response = (PutObjectResponse) context.response();

            if (response.eTag() != null) {
                SdkChecksum checksum = executionAttributes.getAttribute(CHECKSUM);
                String contentMd5 = BinaryUtils.toBase64(checksum.getChecksumBytes());
                byte[] digest = BinaryUtils.fromBase64(contentMd5);
                byte[] ssHash = Base16Lower.decode(response.eTag().replace("\"", ""));

                if (!Arrays.equals(digest, ssHash)) {
                    throw SdkClientException.create(String.format("Data read has a different checksum than expected. " +
                                                                  "Was %d, but expected %d", digest, ssHash));
                }
            }
        }
    }

    private boolean checksumValidationEnabled(ExecutionAttributes executionAttributes) {

        ClientType clientType = executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_TYPE);

        if (SYNC.equals(clientType)) {
            S3Configuration serviceConfiguration =
                (S3Configuration) executionAttributes.getAttribute(AwsSignerExecutionAttribute.SERVICE_CONFIG);
            return serviceConfiguration == null || serviceConfiguration.checksumValidationEnabled();
        }

        return false;
    }
}
