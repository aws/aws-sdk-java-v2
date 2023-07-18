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
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityCredentialsUtils;
import software.amazon.awssdk.auth.credentials.internal.WebIdentityTokenCredentialProperties;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A credential provider that will read web identity token file path, aws role arn and aws session name from system properties or
 * environment variables for using web identity token credentials with STS.
 * <p>
 * Use of this credentials provider requires the 'sts' module to be on the classpath.
 * </p>
 * <p>
 * StsWebIdentityTokenFileCredentialsProvider in sts package can be used instead of this class if any one of following is
 * required
 * <ul>
 *     <li>Pass a custom StsClient to the provider. </li>
 *     <li>Periodically update credentials </li>
 * </ul>
 *
 * @see AwsCredentialsProvider
 */
@SdkPublicApi
public class WebIdentityTokenFileCredentialsProvider
    implements AwsCredentialsProvider, SdkAutoCloseable,
               ToCopyableBuilder<WebIdentityTokenFileCredentialsProvider.Builder, WebIdentityTokenFileCredentialsProvider> {
    private static final Logger log = Logger.loggerFor(WebIdentityTokenFileCredentialsProvider.class);

    private final AwsCredentialsProvider credentialsProvider;
    private final RuntimeException loadException;

    private final String roleArn;

    private final String roleSessionName;

    private final Path webIdentityTokenFile;

    private final Boolean asyncCredentialUpdateEnabled;

    private final Duration prefetchTime;

    private final Duration staleTime;

    private final Duration roleSessionDuration;

    private WebIdentityTokenFileCredentialsProvider(BuilderImpl builder) {
        AwsCredentialsProvider credentialsProvider = null;
        RuntimeException loadException = null;
        String roleArn = null;
        String roleSessionName = null;
        Path webIdentityTokenFile = null;
        Boolean asyncCredentialUpdateEnabled = null;
        Duration prefetchTime = null;
        Duration staleTime = null;
        Duration roleSessionDuration = null;

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

            asyncCredentialUpdateEnabled =
                builder.asyncCredentialUpdateEnabled != null ? builder.asyncCredentialUpdateEnabled : false;

            prefetchTime = builder.prefetchTime;
            staleTime = builder.staleTime;
            roleSessionDuration = builder.roleSessionDuration;

            WebIdentityTokenCredentialProperties credentialProperties =
                WebIdentityTokenCredentialProperties.builder()
                                                    .roleArn(roleArn)
                                                    .roleSessionName(roleSessionName)
                                                    .webIdentityTokenFile(webIdentityTokenFile)
                                                    .asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled)
                                                    .prefetchTime(prefetchTime)
                                                    .staleTime(staleTime)
                                                    .roleSessionDuration(roleSessionDuration)
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
        this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
        this.prefetchTime = prefetchTime;
        this.staleTime = staleTime;
        this.roleSessionDuration = roleSessionDuration;
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
         * Define whether the provider should fetch credentials asynchronously in the background.
         */
        Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        /**
         * Configure the amount of time, relative to STS token expiration, that the cached credentials are considered close to
         * stale and should be updated.
         *
         * <p>Prefetch updates will occur between the specified time and the stale time of the provider. Prefetch
         * updates may be asynchronous. See {@link #asyncCredentialUpdateEnabled}.
         *
         * <p>By default, this is 5 minutes.
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * Configure the amount of time, relative to STS token expiration, that the cached credentials are considered stale and
         * must be updated. All threads will block until the value is updated.
         *
         * <p>By default, this is 1 minute.
         */
        Builder staleTime(Duration staleTime);

        /**
         * @param sessionDuration
         * @return
         */
        Builder roleSessionDuration(Duration sessionDuration);

        /**
         * Create a {@link WebIdentityTokenFileCredentialsProvider} using the configuration applied to this builder.
         */
        WebIdentityTokenFileCredentialsProvider build();
    }

    static final class BuilderImpl implements Builder {
        private String roleArn;
        private String roleSessionName;
        private Path webIdentityTokenFile;
        private Boolean asyncCredentialUpdateEnabled;
        private Duration prefetchTime;
        private Duration staleTime;
        private Duration roleSessionDuration;

        BuilderImpl() {
        }

        private BuilderImpl(WebIdentityTokenFileCredentialsProvider provider) {
            this.roleArn = provider.roleArn;
            this.roleSessionName = provider.roleSessionName;
            this.webIdentityTokenFile = provider.webIdentityTokenFile;
            this.asyncCredentialUpdateEnabled = provider.asyncCredentialUpdateEnabled;
            this.prefetchTime = provider.prefetchTime;
            this.staleTime = provider.staleTime;
            this.roleSessionDuration = provider.roleSessionDuration;
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
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        public void setAsyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            asyncCredentialUpdateEnabled(asyncCredentialUpdateEnabled);
        }

        @Override
        public Builder prefetchTime(Duration prefetchTime) {
            this.prefetchTime = prefetchTime;
            return this;
        }

        public void setPrefetchTime(Duration prefetchTime) {
            prefetchTime(prefetchTime);
        }

        @Override
        public Builder staleTime(Duration staleTime) {
            this.staleTime = staleTime;
            return this;
        }

        public void setStaleTime(Duration staleTime) {
            staleTime(staleTime);
        }

        @Override
        public Builder roleSessionDuration(Duration sessionDuration) {
            this.roleSessionDuration = sessionDuration;
            return this;
        }

        public void setRoleSessionDuration(Duration roleSessionDuration) {
            roleSessionDuration(roleSessionDuration);
        }

        @Override
        public WebIdentityTokenFileCredentialsProvider build() {
            return new WebIdentityTokenFileCredentialsProvider(this);
        }
    }
}
