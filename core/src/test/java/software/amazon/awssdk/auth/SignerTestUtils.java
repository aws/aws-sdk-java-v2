/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import java.util.Date;
import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.handlers.AwsExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.InterceptorContext;

public class SignerTestUtils {
    public static SdkHttpFullRequest signRequest(Signer signer,
                                                 SdkHttpFullRequest request,
                                                 AwsCredentials credentials) {
        // FIXME(dongie)
        return null;
//        return signer.sign(InterceptorContext.builder().request(new SdkRequest() {}).httpRequest(request).build(),
//                           new ExecutionAttributes().putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials));
    }

    public static SdkHttpFullRequest presignRequest(Presigner presigner,
                                                    SdkHttpFullRequest request,
                                                    AwsCredentials credentials,
                                                    Date expiration) {
        // FIXME(dongie)
        return null;
//        return presigner.presign(InterceptorContext.builder().request(new SdkRequest() {}).httpRequest(request).build(),
//                                 new ExecutionAttributes().putAttribute(AwsExecutionAttributes.AWS_CREDENTIALS, credentials),
//                                 expiration);
    }
}
