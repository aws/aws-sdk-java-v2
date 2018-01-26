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

package software.amazon.awssdk.core.auth;

import java.util.Date;

import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.AwsExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;

public class SignerTestUtils {
    public static SdkHttpFullRequest signRequest(Signer signer,
                                                 SdkHttpFullRequest request,
                                                 AwsCredentials credentials) {
        return signer.sign(InterceptorContext.builder().request(NoopTestRequest.builder().build()).httpRequest(request).build(),
                           new ExecutionAttributes().putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials));
    }

    public static SdkHttpFullRequest presignRequest(Presigner presigner,
                                                    SdkHttpFullRequest request,
                                                    AwsCredentials credentials,
                                                    Date expiration) {
        return presigner.presign(InterceptorContext.builder().request(NoopTestRequest.builder().build()).httpRequest(request).build(),
                                 new ExecutionAttributes().putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials),
                                 expiration);
    }
}
