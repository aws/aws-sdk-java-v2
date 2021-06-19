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

package software.amazon.awssdk.transfer.s3.internal;


import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.DelegateCredentialsProvider;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Adapts an SDK {@link AwsCredentialsProvider} to CRT {@link CredentialsProvider}
 */
@SdkInternalApi
public final class CrtCredentialsProviderAdapter implements SdkAutoCloseable {
    private final AwsCredentialsProvider credentialsProvider;

    public CrtCredentialsProviderAdapter(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public CredentialsProvider crtCredentials() {
        return new DelegateCredentialsProvider.DelegateCredentialsProviderBuilder()
            .withHandler(() -> {
                AwsCredentials sdkCredentials = credentialsProvider.resolveCredentials();
                byte[] accessKey = sdkCredentials.accessKeyId().getBytes(StandardCharsets.UTF_8);
                byte[] secreteKey = sdkCredentials.secretAccessKey().getBytes(StandardCharsets.UTF_8);

                // TODO: confirm with CRT if set empty means null. Currently setting null causes the crash
                byte[] sessionTokens = new byte[0];
                if (sdkCredentials instanceof AwsSessionCredentials) {
                    sessionTokens =
                        ((AwsSessionCredentials) sdkCredentials).sessionToken().getBytes(StandardCharsets.UTF_8);
                }

                return new Credentials(accessKey,
                                       secreteKey,
                                       sessionTokens);
            }).build();
    }

    @Override
    public void close() {
        if (credentialsProvider instanceof SdkAutoCloseable) {
            ((SdkAutoCloseable) credentialsProvider).close();
        }
    }
}
