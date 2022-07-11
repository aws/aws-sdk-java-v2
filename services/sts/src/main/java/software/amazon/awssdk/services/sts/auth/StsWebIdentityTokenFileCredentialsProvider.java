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

package software.amazon.awssdk.services.sts.auth;

import static software.amazon.awssdk.utils.StringUtils.trim;
import static software.amazon.awssdk.utils.Validate.notNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.internal.AssumeRoleWithWebIdentityRequestSupplier;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An implementation of {@link AwsCredentialsProvider} that periodically sends an {@link AssumeRoleWithWebIdentityRequest} to the
 * AWS Security Token Service to maintain short-lived sessions to use for authentication. These sessions are updated using a
 * single calling thread (by default) or asynchronously (if {@link Builder#asyncCredentialUpdateEnabled(Boolean)} is set).
 *
 * Unlike {@link StsAssumeRoleWithWebIdentityCredentialsProvider}, this reads the web identity information, including AWS role
 * ARN, AWS session name and the location of a web identity token file from system properties and environment variables. The
 * web identity token file is expected to contain the web identity token to use with each request.
 *
 * If the credentials are not successfully updated before expiration, calls to {@link #resolveCredentials()} will block until
 * they are updated successfully.
 *
 * Users of this provider must {@link #close()} it when they are finished using it.
 *
 * This is created using {@link #builder()}.
 */
@SdkPublicApi
public final class StsWebIdentityTokenFileCredentialsProvider
    extends StsCredentialsProvider
    implements ToCopyableBuilder<StsWebIdentityTokenFileCredentialsProvider.Builder, StsWebIdentityTokenFileCredentialsProvider> {

    private final AwsCredentialsProvider credentialsProvider;
    private final RuntimeException loadException;
    private final Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequest;

    private final Path webIdentityTokenFile;
    private final String roleArn;
    private final String roleSessionName;
    private final Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequestFromBuilder;

    private StsWebIdentityTokenFileCredentialsProvider(Builder builder) {
        super(builder, "sts-assume-role-with-web-identity-credentials-provider");
        Path webIdentityTokenFile =
            builder.webIdentityTokenFile != null ? builder.webIdentityTokenFile
                                                 : Paths.get(trim(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE
                                                                      .getStringValueOrThrow()));

        String roleArn = builder.roleArn != null ? builder.roleArn
                                                 : trim(SdkSystemSetting.AWS_ROLE_ARN.getStringValueOrThrow());

        String sessionName = builder.roleSessionName != null ? builder.roleSessionName :
                             SdkSystemSetting.AWS_ROLE_SESSION_NAME.getStringValue()
                                                                   .orElse("aws-sdk-java-" + System.currentTimeMillis());

        WebIdentityTokenCredentialProperties credentialProperties =
            WebIdentityTokenCredentialProperties.builder()
                                                .roleArn(roleArn)
                                                .roleSessionName(builder.roleSessionName)
                                                .webIdentityTokenFile(webIdentityTokenFile)
                                                .build();

        this.assumeRoleWithWebIdentityRequest = builder.assumeRoleWithWebIdentityRequestSupplier != null
                                                ? builder.assumeRoleWithWebIdentityRequestSupplier
                                                : () -> AssumeRoleWithWebIdentityRequest.builder()
                                                                                        .roleArn(credentialProperties.roleArn())
                                                                                        .roleSessionName(sessionName)
                                                                                        .build();

        AwsCredentialsProvider credentialsProviderLocal = null;
        RuntimeException loadExceptionLocal = null;
        try {
            AssumeRoleWithWebIdentityRequestSupplier supplier =
                AssumeRoleWithWebIdentityRequestSupplier.builder()
                                                        .assumeRoleWithWebIdentityRequest(assumeRoleWithWebIdentityRequest.get())
                                                        .webIdentityTokenFile(credentialProperties.webIdentityTokenFile())
                                                        .build();
            credentialsProviderLocal =
                StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
                                                               .stsClient(builder.stsClient)
                                                               .refreshRequest(supplier)
                                                               .build();
        } catch (RuntimeException e) {
            // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception
            // will only be raised on calls to getCredentials. We don't want to raise an exception here because it may be
            // expected (eg. in the default credential chain).
            loadExceptionLocal = e;
        }
        this.loadException = loadExceptionLocal;
        this.credentialsProvider = credentialsProviderLocal;

        this.webIdentityTokenFile = builder.webIdentityTokenFile;
        this.roleArn = builder.roleArn;
        this.roleSessionName = builder.roleSessionName;
        this.assumeRoleWithWebIdentityRequestFromBuilder = builder.assumeRoleWithWebIdentityRequestSupplier;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        if (loadException != null) {
            throw loadException;
        }
        return credentialsProvider.resolveCredentials();
    }

    @Override
    public String toString() {
        return ToString.builder("StsWebIdentityTokenFileCredentialsProvider")
                       .add("refreshRequest", assumeRoleWithWebIdentityRequest)
                       .build();
    }

    @Override
    protected Credentials getUpdatedCredentials(StsClient stsClient) {
        AssumeRoleWithWebIdentityRequest request = assumeRoleWithWebIdentityRequest.get();
        notNull(request, "AssumeRoleWithWebIdentityRequest can't be null");
        return stsClient.assumeRoleWithWebIdentity(request).credentials();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder extends BaseBuilder<Builder, StsWebIdentityTokenFileCredentialsProvider> {
        private String roleArn;
        private String roleSessionName;
        private Path webIdentityTokenFile;
        private Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequestSupplier;
        private StsClient stsClient;

        private Builder() {
            super(StsWebIdentityTokenFileCredentialsProvider::new);
        }

        private Builder(StsWebIdentityTokenFileCredentialsProvider provider) {
            super(StsWebIdentityTokenFileCredentialsProvider::new);
            this.roleArn = provider.roleArn;
            this.roleSessionName = provider.roleSessionName;
            this.webIdentityTokenFile = provider.webIdentityTokenFile;
            this.assumeRoleWithWebIdentityRequestSupplier = provider.assumeRoleWithWebIdentityRequestFromBuilder;
            this.stsClient = provider.stsClient;
        }

        /**
         * The Custom {@link StsClient} that will be used to fetch AWS service credentials.
         * <ul>
         *     <li>This SDK client must be closed by the caller when it is ready to be disposed.</li>
         *     <li>This SDK client's retry policy should handle IdpCommunicationErrorException </li>
         * </ul>
         * @param stsClient The STS client to use for communication with STS.
         *                  Make sure IdpCommunicationErrorException is retried in the retry policy for this client.
         *                  Make sure the custom STS client is closed when it is ready to be disposed.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        @Override
        public Builder stsClient(StsClient stsClient) {
            this.stsClient = stsClient;
            return super.stsClient(stsClient);
        }

        /**
         * <p>
         * The Amazon Resource Name (ARN) of the IAM role that is associated with the Sts.
         * If not provided this will be read from SdkSystemSetting.AWS_ROLE_ARN.
         * </p>
         *
         * @param roleArn The Amazon Resource Name (ARN) of the IAM role that is associated with the Sts cluster.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        /**
         * <p>
         * Sets Amazon Resource Name (ARN) of the IAM role that is associated with the Sts.
         * By default this will be read from SdkSystemSetting.AWS_ROLE_ARN.
         * </p>
         *
         * @param roleArn The Amazon Resource Name (ARN) of the IAM role that is associated with the Sts cluster.
         */
        public void setRoleArn(String roleArn) {
            roleArn(roleArn);
        }

        /**
         * <p>
         * Sets the role session name that should be used by this credentials provider.
         * By default this is read from SdkSystemSetting.AWS_ROLE_SESSION_NAME
         * </p>
         *
         * @param roleSessionName role session name that should be used by this credentials provider
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder roleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
            return this;
        }

        /**
         * <p>
         * Sets the role session name that should be used by this credentials provider.
         * By default this is read from SdkSystemSetting.AWS_ROLE_SESSION_NAME
         * </p>
         *
         * @param roleSessionName role session name that should be used by this credentials provider.
         */
        public void setRoleSessionName(String roleSessionName) {
            roleSessionName(roleSessionName);
        }

        /**
         * <p>
         * Sets the absolute path to the web identity token file that should be used by this credentials provider.
         * By default this will be read from SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.
         * </p>
         *
         * @param webIdentityTokenFile absolute path to the web identity token file that should be used by this credentials
         *                             provider.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder webIdentityTokenFile(Path webIdentityTokenFile) {
            this.webIdentityTokenFile = webIdentityTokenFile;
            return this;
        }

        public void setWebIdentityTokenFile(Path webIdentityTokenFile) {
            webIdentityTokenFile(webIdentityTokenFile);
        }


        /**
         * Configure the {@link AssumeRoleWithWebIdentityRequest} that should be periodically sent to the STS service to update
         * the session token when it gets close to expiring.
         *
         * @param assumeRoleWithWebIdentityRequest The request to send to STS whenever the assumed session expires.
         * @return This object for chained calls.
         */
        public Builder refreshRequest(AssumeRoleWithWebIdentityRequest assumeRoleWithWebIdentityRequest) {
            return refreshRequest(() -> assumeRoleWithWebIdentityRequest);
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleWithWebIdentityRequest)}, but takes a {@link Supplier} to supply the
         * request to STS.
         *
         * @param assumeRoleWithWebIdentityRequestSupplier A supplier
         * @return This object for chained calls.
         */
        public Builder refreshRequest(Supplier<AssumeRoleWithWebIdentityRequest> assumeRoleWithWebIdentityRequestSupplier) {
            this.assumeRoleWithWebIdentityRequestSupplier = assumeRoleWithWebIdentityRequestSupplier;
            return this;
        }

        /**
         * Similar to {@link #refreshRequest(AssumeRoleWithWebIdentityRequest)}, but takes a lambda to configure a new {@link
         * AssumeRoleWithWebIdentityRequest.Builder}. This removes the need to call {@link
         * AssumeRoleWithWebIdentityRequest#builder()} and {@link AssumeRoleWithWebIdentityRequest.Builder#build()}.
         */
        public Builder refreshRequest(Consumer<AssumeRoleWithWebIdentityRequest.Builder> assumeRoleWithWebIdentityRequest) {
            return refreshRequest(AssumeRoleWithWebIdentityRequest.builder().applyMutation(assumeRoleWithWebIdentityRequest)
                                                                  .build());
        }

        @Override
        public StsWebIdentityTokenFileCredentialsProvider build() {
            return new StsWebIdentityTokenFileCredentialsProvider(this);
        }

    }
}