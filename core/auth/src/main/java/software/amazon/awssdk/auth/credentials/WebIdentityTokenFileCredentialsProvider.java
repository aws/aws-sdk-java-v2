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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * An {@link IdentityProvider}{@code <}{@link AwsCredentialsIdentity}{@code >} implementation that loads credentials by
 * assuming a role from STS based on a web identity token loaded from a file path.
 *
 * <p>
 * This credentials provider requires a dependency on
 * <a href="https://mvnrepository.com/artifact/software.amazon.awssdk/sts">{@code sts}</a>.
 *
 * <p>
 * This credentials provider is most useful as a component of the {@link DefaultCredentialsProvider}, because it does not
 * require code changes when clients are using the default provider. If you have access to change the code that uses the SDK,
 * it's recommended to use {@link software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider}
 * instead of this credentials provider, because it allows more flexibility in specifying the
 * {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest}.
 *
 * <p>
 * This credentials provider will invoke
 * {@link software.amazon.awssdk.services.sts.StsClient#assumeRoleWithWebIdentity(
 * software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest)}. Some settings can be configured by
 * system properties or environment variables:
 * <ul>
 *     <li>The {@code aws.webIdentityTokenFile} system property or {@code AWS_WEB_IDENTITY_TOKEN_FILE} environment
 *     variable specifies a path to a file on disk that contains the
 *     {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#webIdentityToken()}. This file will
 *     be read each time the role needs to be refreshed.</li>
 *     <li>The {@code aws.roleArn} system property or {@code AWS_ROLE_ARN} environment variable specifies the
 *     {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#roleArn()}.</li>
 *     <li>The {@code aws.roleSessionName} system property or {@code AWS_ROLE_SESSION_NAME} environment variable specifies the
 *     {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#roleSessionName()}.</li>
 * </ul>
 *
 * <p>
 * This credential provider caches the credentials, and will only invoke STS periodically
 * to keep the credentials "fresh". As a result, it is recommended that you create a single credentials provider of this type
 * and reuse it throughout your application. You may notice small latency increases on requests that refresh the cached
 * credentials. To avoid this latency increase, you can enable async refreshing with
 * {@link Builder#asyncCredentialUpdateEnabled(Boolean)}.
 *
 * <p>
 * You must {@link #close()} this credential provider if you are done using it, because it creates resources that cannot be
 * garbage collected.
 *
 * <p>
 * This credentials provider is included in the {@link DefaultCredentialsProvider}.
 *
 * <p>
 * Create using {@link #create()} or {@link #builder()}:
 * {@snippet :
 * WebIdentityTokenFileCredentialsProvider credentialsProvider =
 *     WebIdentityTokenFileCredentialsProvider.create(); // @link substring="create" target="#create()"
 *
 * // or
 *
 * WebIdentityTokenFileCredentialsProvider credentialsProvider =
 *     WebIdentityTokenFileCredentialsProvider.builder() // @link substring="builder" target="#builder()"
 *                                            .asyncCredentialUpdateEnabled(false)
 *                                            .build();
 *
 * S3Client s3 = S3Client.builder()
 *                       .credentialsProvider(credentialsProvider)
 *                       .build();
 * }
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

    /**
     * Create a {@link WebIdentityTokenFileCredentialsProvider} with default configuration.
     * <p>
     * {@snippet :
     * WebIdentityTokenFileCredentialsProvider credentialsProvider = WebIdentityTokenFileCredentialsProvider.create();
     * }
     */
    public static WebIdentityTokenFileCredentialsProvider create() {
        return WebIdentityTokenFileCredentialsProvider.builder().build();
    }

    /**
     * Get a new builder for creating a {@link WebIdentityTokenFileCredentialsProvider}.
     * <p>
     * {@snippet :
     * WebIdentityTokenFileCredentialsProvider credentialsProvider =
     *     WebIdentityTokenFileCredentialsProvider.builder()
     *                                            .asyncCredentialUpdateEnabled(false)
     *                                            .build();
     * }
     */
    public static Builder builder() {
        return new BuilderImpl();
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
        return ToString.create("WebIdentityTokenCredentialsProvider");
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    /**
     * Release resources held by this credentials provider. This should be called when you're done using the credentials
     * provider, because it holds resources (e.g. an STS client) that must be released.
     */
    @Override
    public void close() {
        IoUtils.closeIfCloseable(credentialsProvider, null);
    }

    /**
     * See {@link WebIdentityTokenFileCredentialsProvider} for detailed documentation.
     */
    public interface Builder extends CopyableBuilder<Builder, WebIdentityTokenFileCredentialsProvider> {
        /**
         * Configure the path to a file containing the
         * {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#webIdentityToken()} that should be
         * used.
         *
         * <p>
         * If not specified, the {@code aws.webIdentityTokenFile} system property or {@code AWS_WEB_IDENTITY_TOKEN_FILE} environment
         * variable will be used. If these are also not set, credential resolution will fail. This file will be read each time
         * the role needs to be refreshed.
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .webIdentityTokenFile(Paths.get("/etc/web-identity-token.txt"))
         *                                        .build()
         * }
         */
        Builder webIdentityTokenFile(Path webIdentityTokenFile);

        /**
         * Configure the {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#roleArn()} that
         * should be assumed.
         *
         * <p>
         * If not specified, the {@code aws.roleArn} system property or {@code AWS_ROLE_ARN} environment variable will be used.
         * If these are also not set, credential resolution will fail.
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .roleArn("arn:aws:iam::012345678901:role/custom-role-to-assume")
         *                                        .build()
         * }
         */
        Builder roleArn(String roleArn);

        /**
         * Configure the {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#roleSessionName()} that
         * should be used.
         *
         * <p>
         * If not specified, the {@code aws.roleSessionName} system property or {@code AWS_ROLE_SESSION_NAME} environment
         * variable will be used. If these are also not set, credential resolution will fail.
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .roleSessionName("some-session-name")
         *                                        .build()
         * }
         */
        Builder roleSessionName(String roleSessionName);

        /**
         * Configure the {@link software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest#durationSeconds()} that
         * should be used.
         *
         * <p>
         * See the documentation linked above for the valid range of values for this setting. The minimum "resolution"
         * of this duration is seconds. Any values specified more precisely than to-the-second are rounded down (e.g. {@code
         * Duration.ofMillis(1999)} will be rounded down to {@code Duration.ofMillis(1000)}).
         *
         * <p>
         * This value should be greater than the {@link #prefetchTime(Duration)} ({@code roleSessionDuration > prefetchTime >
         * staleTime}).
         *
         * <p>
         * If not specified, the service default of {@code Duration.ofHours(1)} will be used. (55 minutes before the prefetch
         * duration, and 59 minutes before the default stale time).
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .roleSessionDuration(Duration.ofHours(1))
         *                                        .build()
         * }
         */
        Builder roleSessionDuration(Duration sessionDuration);

        /**
         * Configure the amount of time between when the credentials expire and when the credential provider starts to pre-fetch
         * updated credentials.
         *
         * <p>
         * When the pre-fetch threshold is encountered, the SDK will block a single calling thread to refresh the credentials.
         * Other threads will continue to use the existing credentials. This prevents all SDK caller's latency from increasing
         * when the credential gets close to expiration, but you may still see a single call with increased latency as that
         * thread refreshes the credentials. To avoid this single-thread latency increase, you can enable async refreshing with
         * {@link Builder#asyncCredentialUpdateEnabled(Boolean)}. When async refreshing is enabled, the pre-fetch threshold is
         * used to determine when the async refreshing thread should "run" to update the credentials.
         *
         * <p>
         * This value should be less than the {@link #roleSessionDuration(Duration)}, and greater than the
         * {@link #staleTime(Duration)} ({@code roleSessionDuration > prefetchTime > staleTime}).
         *
         * <p>
         * If not specified, {@code Duration.ofMinutes(5)} is used. (55 minutes after the default session duration, and 4
         * minutes before the default stale time).
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .prefetchTime(Duration.ofMinutes(5))
         *                                        .build()
         * }
         */
        Builder prefetchTime(Duration prefetchTime);

        /**
         * Configure the amount of time between when the credentials actually expire and when the credential provider treats
         * those credentials as expired.
         *
         * <p>
         * If the SDK treated the credentials as expired exactly when the service reported they will expire (a stale time of 0
         * seconds), SDK calls could fail close to that expiration time. As a result, the SDK treats credentials as expired
         * 1 minute before the service reported that those credentials will expire.
         *
         * <p>
         * The failures that could occur without this threshold are caused by two primary factors:
         * <ul>
         *     <li>Request latency: There is latency between when the credentials are loaded and when the service processes
         *     the request. The SDK has to sign the request, transmit to the service, and the service has to validate the
         *     signature.</li>
         *     <li>Clock skew: The client and service may not have the exact same measure of time, so an expiration time for
         *     the service may be off from the expiration time for the client.</li>
         * </ul>
         *
         * <p>
         * When the stale threshold is encountered, the SDK will block all calling threads until a successful refresh is achieved.
         * (Note: while all threads are blocked, only one thread will actually make the service call to refresh the
         * credentials). Because this increase in latency for all threads is undesirable, you should ensure that the
         * {@link #prefetchTime(Duration)} is greater than the {@code staleTime}. When configured correctly, the stale time is
         * only encountered when the prefetch calls did not succeed (e.g. due to an outage).
         *
         * <p>
         * This value should be less than the {@link #prefetchTime(Duration)} ({@code roleSessionDuration > prefetchTime >
         * staleTime}).
         *
         * <p>
         * If not specified, {@code Duration.ofMinutes(1)} is used. (4 minutes after the default
         * {@link #prefetchTime(Duration)}, and 59 minutes after the default session duration).
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .staleTime(Duration.ofMinutes(1))
         *                                        .build()
         * }
         */
        Builder staleTime(Duration staleTime);

        /**
         * Configure whether this provider should fetch credentials asynchronously in the background. If this is {@code true},
         * threads are less likely to block when credentials are loaded, but additional resources are used to maintain
         * the provider.
         *
         * <p>
         * If not specified, this is {@code false}.
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider.builder()
         *                                        .asyncCredentialUpdateEnabled(false)
         *                                        .build();
         * }
         */
        Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        /**
         * Build the {@link WebIdentityTokenFileCredentialsProvider}.
         *
         * <p>
         * {@snippet :
         * WebIdentityTokenFileCredentialsProvider credentialsProvider =
         *     WebIdentityTokenFileCredentialsProvider.builder()
         *                                            .asyncCredentialUpdateEnabled(false)
         *                                            .build();
         * }
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
