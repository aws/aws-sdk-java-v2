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

import static software.amazon.awssdk.http.Header.CONTENT_MD5;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

@SdkProtectedApi
public class AddContentMd5HeaderInterceptor implements ExecutionInterceptor {

    // List of operations that should be ignored by this interceptor.
    // These are costly operations, so adding the md5 header will take a performance hit
    private static final List<Class> BLACKLIST_METHODS = Arrays.asList(PutObjectRequest.class, UploadPartRequest.class);

    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();

        if (!BLACKLIST_METHODS.contains(context.request().getClass()) && request.contentStreamProvider().isPresent()
            && !request.firstMatchingHeader(CONTENT_MD5).isPresent()) {

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IoUtils.copy(request.contentStreamProvider().get().newStream(), baos);
                return request.toBuilder()
                              .putHeader(CONTENT_MD5, Md5Utils.md5AsBase64(baos.toByteArray()))
                              .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return request;
    }
}
