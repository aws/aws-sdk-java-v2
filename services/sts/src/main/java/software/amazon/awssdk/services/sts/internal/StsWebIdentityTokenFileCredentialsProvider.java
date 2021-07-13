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

package software.amazon.awssdk.services.sts.internal;

import static software.amazon.awssdk.utils.StringUtils.trim;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.ToString;


/**
 * A credential provider that will read web identity token file path, aws role arn, aws session name
 * from system properties or environment variables  and StsClient for using
 * web identity token credentials with STS. If StsClient is not passed or null then it will use Default Sts client.
 */
@SdkPublicApi
public class StsWebIdentityTokenFileCredentialsProvider implements AwsCredentialsProvider {

    private final AwsCredentialsProvider credentialsProvider;
    private final RuntimeException loadException;

    private StsWebIdentityTokenFileCredentialsProvider(BuilderImpl builder) {
        AwsCredentialsProvider credentialsProviderLocal = null;
        RuntimeException loadExceptionLocal = null;

        try {
            Path webIdentityTokenFile =
                builder.webIdentityTokenFile != null ? builder.webIdentityTokenFile
                                                     : Paths.get(trim(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE
                                                                          .getStringValueOrThrow()));

            String roleArn = builder.roleArn != null ? builder.roleArn
                                                     : trim(SdkSystemSetting.AWS_ROLE_ARN.getStringValueOrThrow());

            Optional<String> roleSessionName =
                builder.roleSessionName != null ? Optional.of(builder.roleSessionName)
                                                : SdkSystemSetting.AWS_ROLE_SESSION_NAME.getStringValue();

            WebIdentityTokenCredentialProperties credentialProperties =
                WebIdentityTokenCredentialProperties.builder()
                                                    .roleArn(roleArn)
                                                    .roleSessionName(roleSessionName.orElse(null))
                                                    .webIdentityTokenFile(webIdentityTokenFile)
                                                    .build();

            credentialsProviderLocal = new StsWebIdentityCredentialsProviderFactory().create(credentialProperties,
                    builder.stsClient);
        } catch (RuntimeException e) {
            // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception
            // will only be raised on calls to getCredentials. We don't want to raise an exception here because it may be
            // expected (eg. in the default credential chain).
            loadExceptionLocal = e;
        }

        this.loadException = loadExceptionLocal;
        this.credentialsProvider = credentialsProviderLocal;
    }

    public static StsWebIdentityTokenFileCredentialsProvider create() {

        return StsWebIdentityTokenFileCredentialsProvider.builder().build();
    }

    public static StsWebIdentityTokenFileCredentialsProvider create(StsClient stsClient) {
        return StsWebIdentityTokenFileCredentialsProvider.builder().stsClient(stsClient).build();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        if (loadException != null) {
            throw loadException;
        }
        return credentialsProvider.resolveCredentials();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String toString() {
        return ToString.create("WebIdentityTokenCredentialsProvider");
    }

    /**
     * A builder for creating a custom {@link StsWebIdentityTokenFileCredentialsProvider}.
     */
    public interface Builder {

        /**
         * Define the role arn that should be used by this credentials provider.
         */
        Builder roleArn(String roleArn);

        /**
         * Define the role session name that should be used by this credentials provider.
         */
        Builder roleSessionName(String roleSessionName);

        /**
         * Define the absolute path to the web identity token file that should be used by this credentials provider.
         */
        Builder webIdentityTokenFile(Path webIdentityTokenFile);


        /**
         * Define the StsClient that is used for accessing AWS Security Token Service.
         */
        Builder stsClient(StsClient stsClient);

        /**
         * Create a {@link StsWebIdentityTokenFileCredentialsProvider} using the configuration applied to this builder.
         */
        StsWebIdentityTokenFileCredentialsProvider build();
    }

    static final class BuilderImpl implements Builder {
        private String roleArn;
        private String roleSessionName;
        private Path webIdentityTokenFile;
        private StsClient stsClient;

        BuilderImpl() {
        }

        @Override
        public Builder roleArn(String roleArn) {
            this.roleArn = roleArn;
            return this;
        }

        public void setRoleArn(String roleArn) {
            roleArn(roleArn);
        }

        @Override
        public Builder roleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
            return this;
        }

        public void setRoleSessionName(String roleSessionName) {
            roleSessionName(roleSessionName);
        }

        @Override
        public Builder webIdentityTokenFile(Path webIdentityTokenFile) {
            this.webIdentityTokenFile = webIdentityTokenFile;
            return this;
        }

        public void setWebIdentityTokenFile(Path webIdentityTokenFile) {
            webIdentityTokenFile(webIdentityTokenFile);
        }

        @Override
        public Builder stsClient(StsClient stsClient) {
            this.stsClient = stsClient;
            return this;
        }

        public void setStsClient(StsClient stsClient) {
            this.stsClient = stsClient;
        }

        @Override
        public StsWebIdentityTokenFileCredentialsProvider build() {
            return new StsWebIdentityTokenFileCredentialsProvider(this);
        }
    }
}
