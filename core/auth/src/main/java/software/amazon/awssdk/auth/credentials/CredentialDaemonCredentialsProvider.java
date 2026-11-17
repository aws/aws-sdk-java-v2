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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonClient;
import software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonRequest;
import software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonResponse;
import software.amazon.awssdk.auth.credentials.internal.daemon.CredentialDaemonUtils;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;
import software.amazon.awssdk.utils.cache.CachedSupplier;
import software.amazon.awssdk.utils.cache.NonBlocking;
import software.amazon.awssdk.utils.cache.RefreshResult;

/**
 * Loads short-lived credentials from a local credential daemon over a Unix domain socket. This is used to support the
 * {@code daemon_credential} setting in the shared config file.
 *
 * <p>
 * Each fetch opens a new connection to the daemon and sends one JSON-RPC 2.0 {@code credential/get} request. The socket is
 * looked up again on every fetch, in this order:
 * <ol>
 *     <li>The {@code AWS_CREDENTIAL_DAEMON_SOCKET} environment variable, when it holds an absolute path.</li>
 *     <li>{@code $HOME/.aws/credential-daemon.sock}, when anything is there: a socket, a symbolic link, or a link whose
 *     target is missing. {@code HOME} is read from the environment, and this location is skipped when {@code HOME} is
 *     unset, empty or relative.</li>
 * </ol>
 * The first location that applies is dialed, and no other location is tried if that dial fails. When no location applies,
 * or when the link's target is missing, the provider raises {@link CredentialDaemonException#NOT_RUNNING} without dialing.
 *
 * <p>
 * The daemon must accept the connection within the connect timeout (default 1 second), and the whole call must finish within
 * the call timeout (default 5 seconds).
 *
 * <p>
 * The daemon is reached over a Unix domain socket, so this provider needs Linux or macOS and a Java runtime that supports
 * Unix domain socket channels, which is Java 16 or later. The SDK supports Java 8, so the provider finds that support at
 * run time. On Java 8 to 15, on Android and on other operating systems such as Windows, every call fails with
 * {@link CredentialDaemonException#UNSUPPORTED_PLATFORM}.
 *
 * <p>
 * On such a platform, an application can still use the daemon's credentials through the AWS CLI. Give it a separate profile
 * that does not set {@code daemon_credential} and runs the CLI as a credential process, for example:
 * <pre>
 * [profile daemon-via-cli]
 * credential_process = aws configure export-credentials --profile my-daemon-profile --format process
 * </pre>
 * where {@code my-daemon-profile} is the profile that sets {@code daemon_credential}.
 *
 * <p>
 * Failures are raised as {@link CredentialDaemonException}, whose {@link CredentialDaemonException#errorCode()} identifies
 * the failure.
 *
 * <p>
 * This class can be created with {@link #builder()}.
 */
@SdkPublicApi
@ThreadSafe
public final class CredentialDaemonCredentialsProvider
    implements AwsCredentialsProvider,
               SdkAutoCloseable,
               ToCopyableBuilder<CredentialDaemonCredentialsProvider.Builder, CredentialDaemonCredentialsProvider> {
    private static final String CLASS_NAME = "CredentialDaemonCredentialsProvider";
    private static final String PROVIDER_NAME = BusinessMetricFeatureId.CREDENTIAL_DAEMON.value();
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration STALE_TIME = Duration.ofMinutes(1);
    private static final Duration PREFETCH_TIME = Duration.ofMinutes(5);

    private final String credentialName;
    private final String accountId;
    private final String roleName;
    private final Duration connectTimeout;
    private final Duration callTimeout;
    private final Boolean asyncCredentialUpdateEnabled;
    private final String sourceChain;
    private final CredentialDaemonRequest request;
    private final CredentialDaemonClient client;
    private final CachedSupplier<AwsCredentials> credentialCache;

    private final Clock clock;

    private CredentialDaemonCredentialsProvider(BuilderImpl builder) {
        this.credentialName = builder.credentialName;
        if (!CredentialDaemonUtils.isValidCredentialName(credentialName)) {
            throw CredentialDaemonUtils.error(CredentialDaemonException.INVALID_CONFIG,
                                               "The credential name is not valid. It must be 1 to 64 characters from A-Z, "
                                               + "a-z, 0-9, '.', '_' and '-', and must not start with '.'.",
                                               null);
        }
        this.accountId = builder.accountId;
        this.roleName = builder.roleName;
        Validate.isTrue((accountId == null) == (roleName == null), "accountId and roleName must be set together.");
        this.connectTimeout = Validate.isPositive(builder.connectTimeout, "connectTimeout");
        this.callTimeout = Validate.isPositive(builder.callTimeout, "callTimeout");
        this.asyncCredentialUpdateEnabled = builder.asyncCredentialUpdateEnabled;
        this.sourceChain = builder.sourceChain;
        String providerName = StringUtils.isEmpty(sourceChain) ? PROVIDER_NAME : sourceChain + "," + PROVIDER_NAME;
        this.request = new CredentialDaemonRequest(credentialName, accountId, roleName);
        this.client = CredentialDaemonClient.create(connectTimeout, callTimeout, providerName);
        this.clock = builder.clock;

        CachedSupplier.Builder<AwsCredentials> cacheBuilder = CachedSupplier.builder(this::refresh)
                                                                            .cachedValueName(toString())
                                                                            .clock(clock);
        if (Boolean.TRUE.equals(asyncCredentialUpdateEnabled)) {
            cacheBuilder.prefetchStrategy(new NonBlocking("sdk-credential-daemon-provider"));
        }
        this.credentialCache = cacheBuilder.build();
    }

    /**
     * Returns a new builder for a {@link CredentialDaemonCredentialsProvider}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return credentialCache.get();
    }

    private RefreshResult<AwsCredentials> refresh() {
        CredentialDaemonResponse fetched = client.fetch(request);
        Instant expiresAt = fetched.expiresAt();
        return RefreshResult.builder(fetched.credentials())
                            .staleTime(expiresAt.minus(STALE_TIME))
                            .prefetchTime(expiresAt.minus(PREFETCH_TIME))
                            .build();
    }

    @Override
    public void close() {
        credentialCache.close();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public String toString() {
        return ToString.builder(CLASS_NAME)
                       .add("credential", credentialName)
                       .build();
    }

    /**
     * A builder for a {@link CredentialDaemonCredentialsProvider}.
     */
    public interface Builder extends CopyableBuilder<Builder, CredentialDaemonCredentialsProvider> {
        /**
         * Sets the name of the credential to request from the daemon. It must be 1 to 64 characters from
         * {@code A-Z a-z 0-9 . _ -}, and must not start with {@code .}. This setting is required.
         */
        Builder credentialName(String credentialName);

        /**
         * Sets the account ID to request credentials for. It must be set together with {@link #roleName(String)}.
         */
        Builder accountId(String accountId);

        /**
         * Sets the role name to request credentials for. It must be set together with {@link #accountId(String)}.
         */
        Builder roleName(String roleName);

        /**
         * Sets how long to wait for the daemon to accept the connection.
         *
         * <p>
         * Default: 1 second.
         */
        Builder connectTimeout(Duration connectTimeout);

        /**
         * Sets how long the whole call to the daemon may take, from connecting to reading the response.
         *
         * <p>
         * Default: 5 seconds.
         */
        Builder callTimeout(Duration callTimeout);

        /**
         * Sets whether the provider fetches credentials in the background before they expire. If this is true, threads
         * are less likely to block when credentials are loaded, but additional resources are used to maintain the provider.
         *
         * <p>
         * Default: false.
         */
        Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled);

        /**
         * Sets an optional string denoting previous credentials providers that are chained with this one.
         *
         * <p>
         * <b>Note:</b> This method is primarily intended for use by AWS SDK internal components and should not be used
         * directly by external users.
         */
        Builder sourceChain(String sourceChain);

        /**
         * Builds the provider.
         *
         * @throws CredentialDaemonException with {@link CredentialDaemonException#INVALID_CONFIG} if the credential name
         * is not valid.
         */
        @Override
        CredentialDaemonCredentialsProvider build();
    }

    @SdkTestInternalApi
    static final class BuilderImpl implements Builder {
        private String credentialName;
        private String accountId;
        private String roleName;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration callTimeout = DEFAULT_CALL_TIMEOUT;
        private Boolean asyncCredentialUpdateEnabled = false;
        private String sourceChain;
        private Clock clock = Clock.systemUTC();

        private BuilderImpl() {
        }

        private BuilderImpl(CredentialDaemonCredentialsProvider provider) {
            this.credentialName = provider.credentialName;
            this.accountId = provider.accountId;
            this.roleName = provider.roleName;
            this.connectTimeout = provider.connectTimeout;
            this.callTimeout = provider.callTimeout;
            this.asyncCredentialUpdateEnabled = provider.asyncCredentialUpdateEnabled;
            this.sourceChain = provider.sourceChain;
            this.clock = provider.clock;
        }

        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public Builder credentialName(String credentialName) {
            this.credentialName = credentialName;
            return this;
        }

        @Override
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public Builder roleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        @Override
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public Builder callTimeout(Duration callTimeout) {
            this.callTimeout = callTimeout;
            return this;
        }

        @Override
        public Builder asyncCredentialUpdateEnabled(Boolean asyncCredentialUpdateEnabled) {
            this.asyncCredentialUpdateEnabled = asyncCredentialUpdateEnabled;
            return this;
        }

        @Override
        public Builder sourceChain(String sourceChain) {
            this.sourceChain = sourceChain;
            return this;
        }

        @Override
        public CredentialDaemonCredentialsProvider build() {
            return new CredentialDaemonCredentialsProvider(this);
        }
    }
}
