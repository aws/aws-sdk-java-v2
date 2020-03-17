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

package software.amazon.awssdk.auth.signer.internal;

import java.time.Clock;
import java.time.Instant;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;

public class SignerTestUtils {
    public static SdkHttpFullRequest signRequest(BaseAws4Signer signer,
                                                 SdkHttpFullRequest request,
                                                 AwsCredentials credentials,
                                                 String signingName,
                                                 Clock signingDateOverride,
                                                 String region) {

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
                                                        .awsCredentials(credentials)
                                                        .signingName(signingName)
                                                        .signingClockOverride(signingDateOverride)
                                                        .signingRegion(Region.of(region))
                                                        .build();

        return signer.sign(request, signerParams);
    }

    public static AsyncRequestBody signAsyncRequest(BaseAsyncAws4Signer signer,
                                                    SdkHttpFullRequest request,
                                                    AsyncRequestBody asyncRequestBody,
                                                    AwsCredentials credentials,
                                                    String signingName,
                                                    Clock signingDateOverride,
                                                    String region) {

        Aws4SignerParams signerParams = Aws4SignerParams.builder()
            .awsCredentials(credentials)
            .signingName(signingName)
            .signingClockOverride(signingDateOverride)
            .signingRegion(Region.of(region))
            .build();

        final Aws4SignerRequestParams requestParams = new Aws4SignerRequestParams(signerParams);

        return signer.signAsync(request, asyncRequestBody, requestParams, signerParams);
    }

    public static SdkHttpFullRequest presignRequest(BaseAws4Signer presigner,
                                                    SdkHttpFullRequest request,
                                                    AwsCredentials credentials,
                                                    Instant expiration,
                                                    String signingName,
                                                    Clock signingDateOverride,
                                                    String region) {
        Aws4PresignerParams signerParams = Aws4PresignerParams.builder()
                                                              .awsCredentials(credentials)
                                                              .expirationTime(expiration)
                                                              .signingName(signingName)
                                                              .signingClockOverride(signingDateOverride)
                                                              .signingRegion(Region.of(region))
                                                              .build();

        return presigner.presign(request, signerParams);
    }
}
