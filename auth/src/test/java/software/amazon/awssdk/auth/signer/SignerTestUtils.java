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

package software.amazon.awssdk.auth.signer;

import java.util.Date;
import software.amazon.awssdk.auth.AwsExecutionAttributes;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.internal.AwsPresignerParams;
import software.amazon.awssdk.auth.signer.internal.AwsSignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.core.signerspi.Presigner;
import software.amazon.awssdk.core.signerspi.Signer;
import software.amazon.awssdk.core.signerspi.SignerContext;

public class SignerTestUtils {
    public static SdkHttpFullRequest signRequest(Signer signer,
                                                 SdkHttpFullRequest request,
                                                 AwsCredentials credentials) {
        AwsSignerParams signerParams = new AwsSignerParams();
        signerParams.setAwsCredentials(credentials);
        return signer.sign(request, new SignerContext().putAttribute(AwsExecutionAttributes.AWS_SIGNER_PARAMS, signerParams));
    }

    public static SdkHttpFullRequest presignRequest(Presigner presigner,
                                                    SdkHttpFullRequest request,
                                                    AwsCredentials credentials,
                                                    Date expiration) {
        AwsPresignerParams signerParams = new AwsPresignerParams();
        signerParams.setAwsCredentials(credentials);
        signerParams.setExpirationDate(expiration);

        return presigner.presign(request, new SignerContext().putAttribute(AwsExecutionAttributes.AWS_SIGNER_PARAMS, signerParams));
    }
}
