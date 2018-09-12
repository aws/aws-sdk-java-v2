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

package software.amazon.awssdk.services.s3.transform;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.runtime.transform.Unmarshaller;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;

@SdkProtectedApi
public final class GetBucketPolicyResponseUnmarshaller implements Unmarshaller<GetBucketPolicyResponse, SdkHttpFullResponse> {
    @Override
    public GetBucketPolicyResponse unmarshall(SdkHttpFullResponse response) throws Exception {
        GetBucketPolicyResponse.Builder builder = GetBucketPolicyResponse.builder();

        response.content().ifPresent(FunctionalUtils.safeConsumer(c -> builder.policy(IoUtils.toUtf8String(c))));

        return builder.build();
    }
}
