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

package software.amazon.awssdk.services.s3.internal.crt;


import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.DelegateCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Adapts an SDK {@link AwsCredentialsProvider} to CRT {@link CredentialsProvider}
 */
@SdkInternalApi
public final class CrtCredentialsProviderAdapter implements SdkAutoCloseable {
    private final IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private final CredentialsProvider crtCredentials;

    public CrtCredentialsProviderAdapter(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        this.crtCredentials = new DelegateCredentialsProvider.DelegateCredentialsProviderBuilder()
            .withHandler(() -> {

                if (credentialsProvider instanceof AnonymousCredentialsProvider) {
                    return Credentials.createAnonymousCredentials();
                }

                AwsCredentialsIdentity sdkCredentials =
                    CompletableFutureUtils.joinLikeSync(credentialsProvider.resolveIdentity());
                byte[] accessKey = sdkCredentials.accessKeyId().getBytes(StandardCharsets.UTF_8);
                byte[] secreteKey = sdkCredentials.secretAccessKey().getBytes(StandardCharsets.UTF_8);

                byte[] sessionTokens = null;
                if (sdkCredentials instanceof AwsSessionCredentialsIdentity) {
                    sessionTokens =
                        ((AwsSessionCredentialsIdentity) sdkCredentials).sessionToken().getBytes(StandardCharsets.UTF_8);
                }
                return new Credentials(accessKey, secreteKey, sessionTokens);

            }).build();
    }

    public CredentialsProvider crtCredentials() {
        return crtCredentials;
    }

    @Override
    public void close() {
        if (credentialsProvider instanceof SdkAutoCloseable) {
            ((SdkAutoCloseable) credentialsProvider).close();
        }
        crtCredentials.close();
    }
}
