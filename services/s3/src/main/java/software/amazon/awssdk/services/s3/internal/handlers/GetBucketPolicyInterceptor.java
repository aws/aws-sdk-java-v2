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

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.InputStream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * GetBucketPolicy returns just JSON so we wrap in XML so that it is unmarshalled correctly.
 */
@SdkInternalApi
public final class GetBucketPolicyInterceptor implements ExecutionInterceptor {

    @Override
    public Optional<InputStream> modifyHttpResponseContent(Context.ModifyHttpResponse context,
                                                           ExecutionAttributes executionAttributes) {
        if (context.request() instanceof GetBucketPolicyRequest) {

            String policy = context.responseBody() == null ? null : invokeSafely(
                () -> IoUtils.toUtf8String(context.responseBody().get()));
            // Wrap in CDATA to deal with any escaping issues
            String xml = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                       + "<Policy><![CDATA[%s]]></Policy>", policy);
            return Optional.of(AbortableInputStream.create(new StringInputStream(xml)));
        }

        return context.responseBody();
    }
}
