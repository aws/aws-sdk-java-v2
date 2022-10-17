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

package software.amazon.awssdk.auth.credentials;

import static software.amazon.awssdk.utils.StringUtils.trim;

import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityCredentialsUtils;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A credential provider that will read web identity token file path, aws role arn
 * and aws session name from system properties or environment variables for using
 * web identity token credentials with STS.
 * <p>
 *     Use of this credentials provider requires the 'sts' module to be on the classpath.
 * </p>
 * <p>
 * StsWebIdentityTokenFileCredentialsProvider in sts package can be used instead of this class if any one of following is required
 *<ul>
 *     <li>Pass a custom StsClient to the provider. </li>
 *     <li>Periodically update credentials </li>
 *</ul>
 * @see AwsCredentialsProvider
 */
@SdkPublicApi
public class WebIdentityTokenFileCredentialsProvider
    implements AwsCredentialsProvider, SdkAutoCloseable,
               ToCopyableBuilder<WebIdentityTokenFileCredentialsProvider.Builder, WebIdentityTokenFileCredentialsProvider> {

    private final AwsCredentialsProvider credentialsProvider;
    private final RuntimeException loadException;

    private final String roleArn;

    private final String roleSessionName;

    private final Path webIdentityTokenFile;

    private WebIdentityTokenFileCredentialsProvider(BuilderImpl builder) {
        AwsCredentialsProvider credentialsProvider = null;
        RuntimeException loadException = null;
        String roleArn = null;
        String roleSessionName = null;
        Path webIdentityTokenFile = null;

        try {
            webIdentityTokenFile =
                builder.webIdentityTokenFile != null ? builder.webIdentityTokenFile
                                                     : Paths.get(trim(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE
                                                                          .getStringValueOrThrow()));

            roleArn = builder.roleArn != null ? builder.roleArn
                                                   : trim(SdkSystemSetting.AWS_ROLE_ARN.getStringValueOrThrow());

            roleSessionName =
                builder.roleSessionName != null ? builder.roleSessionName
                                                : SdkSystemSetting.AWS_ROLE_SESSION_NAME.getStringValue().orElse(null);

            WebIdentityTokenCredentialProperties credentialProperties =
                WebIdentityTokenCredentialProperties.builder()
                                                    .roleArn(roleArn)
                                                    .roleSessionName(roleSessionName)
                                                    .webIdentityTokenFile(webIdentityTokenFile)
                                                    .build();

            credentialsProvider = WebIdentityCredentialsUtils.factory().create(credentialProperties);
        } catch (RuntimeException e) {
            // If we couldn't load the credentials provider for some reason, save an exception describing why. This exception
            // will only be raised on calls to getCredentials. We don't want to raise an exception here because it may be
            // expected (eg. in the default credential chain).
            loadException = e;
        }

        this.loadException = loadException;
        this.credentialsProvider = credentialsProvider;
        this.roleArn = roleArn;
        this.roleSessionName = roleSessionName;
        this.webIdentityTokenFile = webIdentityTokenFile;
    }

    public static WebIdentityTokenFileCredentialsProvider create() {

        return WebIdentityTokenFileCredentialsProvider.builder().build();
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

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public void close() {
        IoUtils.closeIfCloseable(credentialsProvider, null);
    }

    /**
     * A builder for creating a custom {@link WebIdentityTokenFileCredentialsProvider}.
     */
    public interface Builder extends CopyableBuilder<Builder, WebIdentityTokenFileCredentialsProvider> {

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
         * Create a {@link WebIdentityTokenFileCredentialsProvider} using the configuration applied to this builder.
         */
        WebIdentityTokenFileCredentialsProvider build();
    }

    static final class BuilderImpl implements Builder {
        private String roleArn;
        private String roleSessionName;
        private Path webIdentityTokenFile;

        BuilderImpl() {
        }

        private BuilderImpl(WebIdentityTokenFileCredentialsProvider provider) {
            this.roleArn = provider.roleArn;
            this.roleSessionName = provider.roleSessionName;
            this.webIdentityTokenFile = provider.webIdentityTokenFile;
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
        public WebIdentityTokenFileCredentialsProvider build() {
            return new WebIdentityTokenFileCredentialsProvider(this);
        }
    }
}
