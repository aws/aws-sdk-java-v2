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

import static software.amazon.awssdk.http.Header.CONTENT_MD5;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Md5Utils;

@SdkInternalApi
public class AddContentMd5HeaderInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<String> CONTENT_MD5_ATTRIBUTE = new ExecutionAttribute<>("contentMd5");

    // List of operations that should be ignored by this interceptor.
    // These are costly operations, so adding the md5 header will take a performance hit
    private static final List<Class> BLACKLIST_METHODS = Arrays.asList(PutObjectRequest.class, UploadPartRequest.class);

    @Override
    public Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                   ExecutionAttributes executionAttributes) {

        if (!BLACKLIST_METHODS.contains(context.request().getClass()) && context.requestBody().isPresent()
            && !context.httpRequest().firstMatchingHeader(CONTENT_MD5).isPresent()) {

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IoUtils.copy(context.requestBody().get().contentStreamProvider().newStream(), baos);
                executionAttributes.putAttribute(CONTENT_MD5_ATTRIBUTE, Md5Utils.md5AsBase64(baos.toByteArray()));
                return context.requestBody();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        return context.requestBody();
    }

    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                            ExecutionAttributes executionAttributes) {
        String contentMd5 = executionAttributes.getAttribute(CONTENT_MD5_ATTRIBUTE);

        if (contentMd5 != null) {
            return context.httpRequest().toBuilder().putHeader(CONTENT_MD5, contentMd5).build();
        }

        return context.httpRequest();
    }
}
